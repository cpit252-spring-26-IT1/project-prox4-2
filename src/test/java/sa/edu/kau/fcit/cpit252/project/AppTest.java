package sa.edu.kau.fcit.cpit252.project;

import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

import sa.edu.kau.fcit.cpit252.project.auth.*;
import sa.edu.kau.fcit.cpit252.project.database.DatabaseManager;
import sa.edu.kau.fcit.cpit252.project.files.*;
import sa.edu.kau.fcit.cpit252.project.model.*;
import sa.edu.kau.fcit.cpit252.project.observer.*;
import sa.edu.kau.fcit.cpit252.project.proxy.*;
import sa.edu.kau.fcit.cpit252.project.ui.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full coverage suite for the ProX4 Secure File System.
 *
 * <p>The integration tests for {@link App} run first (low @Order values) so that
 * the OWNER account is created during the first-run flow before any other test
 * touches the singletons. Pure unit tests run afterwards (high @Order values).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AppTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final InputStream originalIn = System.in;

    private static Path tempFile;
    private static Path tempDir;
    private static Path emptyFile;
    private static final String T() { return tempFile.toString(); }

    // â”€â”€ Lifecycle â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @BeforeAll
    static void setupFiles() throws IOException {
        // Clean any persisted state from previous runs so the first-run flow triggers.
        Files.deleteIfExists(Paths.get("users.dat"));
        Files.deleteIfExists(Paths.get("access_log.txt"));

        tempFile = Files.createTempFile("prox4test", ".txt");
        Files.write(tempFile, "Hello World\nSecond Line".getBytes());
        tempDir = Files.createTempDirectory("prox4dir");
        emptyFile = Files.createTempFile("prox4empty", ".txt");
    }

    @AfterAll
    static void cleanupFiles() throws IOException {
        Files.deleteIfExists(tempFile);
        Files.deleteIfExists(emptyFile);
        Files.deleteIfExists(tempDir);
        deleteDir(Paths.get("downloads"));
        deleteDir(Paths.get("workspace"));
        Files.deleteIfExists(Paths.get("access_log.txt"));
    }

    private static void deleteDir(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir).sorted(Comparator.reverseOrder())
                    .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        }
    }

    @BeforeEach
    void redirectOut() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setIn(originalIn);
        outContent.reset();
    }

    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Feeds {@code input} to System.in, resets App's static Scanner, then runs main. */
    private void runApp(String input) {
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        try {
            Field sc = App.class.getDeclaredField("sc");
            sc.setAccessible(true);
            sc.set(null, new Scanner(System.in));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        App.main(new String[]{});
    }

    private String out() { return outContent.toString(); }

    private AuthenticationManager auth() { return AuthenticationManager.getInstance(); }

    private UserAccount owner() { return auth().getAccounts().get("owner"); }

    /** Ensures an account exists (creating it via OWNER if missing). */
    private UserAccount ensureUser(String name, String pass, Role role) {
        UserAccount existing = auth().getAccounts().get(name);
        if (existing != null) return existing;
        auth().addUser(name, pass, role, owner());
        return auth().getAccounts().get(name);
    }

    // ===========================================================
    //  APP INTEGRATION TESTS (ordered first)
    // ===========================================================

    @Test @Order(1)
    void appFirstRunSetupCreatesOwner() {
        // username: empty (retry) then "owner"
        // password: empty/empty (retry), mismatch a/b (retry), then pass/pass
        // then login menu -> exit
        runApp("\nowner\n\n\na\nb\npass\npass\n3\n");
        String o = out();
        assertTrue(o.contains("FIRST RUN"));
        assertTrue(o.contains("Username cannot be empty"));
        assertTrue(o.contains("Passwords do not match"));
        assertTrue(o.contains("Password cannot be empty"));
        assertTrue(o.contains("OWNER account created"));
        assertFalse(auth().isFirstRun());
        assertNotNull(owner());
        assertEquals(Role.OWNER, owner().getRole());
    }

    @Test @Order(2)
    void appOwnerBrowseEmptyAndSelectEmpty() {
        // browse (empty registry) then open (selectFile -> null) then logout/exit
        runApp("1\nowner\npass\n1\n2\n11\n3\n");
        String o = out();
        assertTrue(o.contains("OWNER DASHBOARD"));
        assertTrue(o.contains("No files available"));
        assertTrue(o.contains("LOGOUT"));
        assertTrue(o.contains("Goodbye"));
    }

    @Test @Order(3)
    void appOwnerRegisterBrowseOpenMoveLockDelete() {
        StringBuilder in = new StringBuilder();
        in.append("1\nowner\npass\n");
        in.append("4\nsdoc\n").append(T()).append("\n1\n");   // SENSITIVE
        in.append("4\nidoc\n").append(T()).append("\n2\n");   // INTERNAL
        in.append("4\ndoc1\n").append(T()).append("\n3\n");   // NORMAL
        in.append("4\ndoc2\n").append(T()).append("\nx\n");   // invalid type
        in.append("4\n\n");                                    // empty name
        in.append("4\ndoc3\n\n");                              // empty path
        in.append("4\nghost\nZ:\\no\\such.txt\n3\n");         // bad-path NORMAL
        in.append("1\n");                                      // browse non-empty
        in.append("2\nNOPE\n");                                // open not found
        in.append("2\ndoc1\n");                                // open found
        in.append("3\ndoc1\n");                                // move success
        in.append("3\nghost\n");                               // move source missing
        in.append("6\ndoc1\n1\n");                             // lock
        in.append("1\n");                                      // browse shows [LOCKED]
        in.append("6\ndoc1\n2\n");                             // unlock
        in.append("6\ndoc1\nx\n");                             // lock invalid subchoice
        in.append("5\ndoc1\n");                                // delete
        in.append("11\n3\n");
        runApp(in.toString());
        String o = out();
        assertTrue(o.contains("File registered: sdoc"));
        assertTrue(o.contains("Unknown type"));
        assertTrue(o.contains("Name cannot be empty"));
        assertTrue(o.contains("Path cannot be empty"));
        assertTrue(o.contains("File Registry"));
        assertTrue(o.contains("File not found in registry"));
        assertTrue(o.contains("PROXY GRANTED"));
        assertTrue(o.contains("MOVE SUCCESS"));
        assertTrue(o.contains("Source file not found"));
        assertTrue(o.contains("[LOCKED]") && o.contains("now locked"));
        assertTrue(o.contains("now unlocked"));
        assertTrue(o.contains("File removed: doc1"));
    }

    @Test @Order(4)
    void appOwnerManageUsers() {
        StringBuilder in = new StringBuilder();
        in.append("1\nowner\npass\n");
        in.append("7\n1\nmgr\nmgrpass\nmgrpass\n1\n");   // add MANAGER
        in.append("7\n1\nusr\nusrpass\nusrpass\n2\n");   // add USER
        in.append("7\n1\ngst\ngstpass\ngstpass\n3\n");   // add GUEST
        in.append("7\n1\nbadrole\np\np\n9\n");            // invalid role
        in.append("7\n1\n\n");                            // empty username
        in.append("7\n1\nmm\np\npX\n");                   // password mismatch
        in.append("7\n1\nee\n\n");                        // empty password
        in.append("7\n5\n");                             // list users
        in.append("7\n1\ntmp\ntmppass\ntmppass\n2\n");   // add USER tmp
        in.append("7\n3\ntmp\n");                        // promote tmp -> MANAGER
        in.append("7\n4\ntmp\n");                        // demote tmp -> USER
        in.append("7\n3\n\n");                            // promote empty username
        in.append("7\n2\ntmp\n");                        // remove tmp
        in.append("7\n9\n");                             // invalid submenu
        in.append("11\n3\n");
        runApp(in.toString());
        String o = out();
        assertTrue(o.contains("User 'mgr' created with role: MANAGER"));
        assertTrue(o.contains("User 'usr' created with role: USER"));
        assertTrue(o.contains("User 'gst' created with role: GUEST"));
        assertTrue(o.contains("Unknown role"));
        assertTrue(o.contains("Passwords do not match"));
        assertTrue(o.contains("Registered Users"));
        assertTrue(o.contains("promoted from USER to MANAGER"));
        assertTrue(o.contains("demoted from MANAGER to USER"));
        assertTrue(o.contains("User 'tmp' deleted"));
        assertTrue(o.contains("Unknown option"));
        assertEquals(Role.MANAGER, auth().getAccounts().get("mgr").getRole());
        assertEquals(Role.USER, auth().getAccounts().get("usr").getRole());
    }

    @Test @Order(5)
    void appOwnerAuditAndEmptyPromotion() {
        runApp("1\nowner\npass\n8\n9\n8\n10\n11\n3\n");
        String o = out();
        assertTrue(o.contains("Audit Log"));
        assertTrue(o.contains("Audit log cleared"));
        assertTrue(o.contains("Audit log is empty"));
        assertTrue(o.contains("No pending promotion requests"));
    }

    @Test @Order(6)
    void appOwnerApprovePromotion() {
        UserAccount papp = ensureUser("papp", "pp", Role.USER);
        PromotionManager.getInstance().requestPromotion(papp);
        runApp("1\nowner\npass\n10\nA\n1\n11\n3\n");
        String o = out();
        assertTrue(o.contains("Pending Promotion Requests"));
        assertTrue(o.contains("APPROVED"));
        assertEquals(Role.MANAGER, auth().getAccounts().get("papp").getRole());
    }

    @Test @Order(7)
    void appOwnerRejectAndBadNumberPromotion() {
        UserAccount prej = ensureUser("prej", "pp", Role.USER);
        PromotionManager.getInstance().requestPromotion(prej);
        runApp("1\nowner\npass\n10\nR\n1\n11\n3\n");
        assertTrue(out().contains("REJECTED"));

        UserAccount pbad = ensureUser("pbad", "pp", Role.USER);
        PromotionManager.getInstance().requestPromotion(pbad);
        runApp("1\nowner\npass\n10\nA\nabc\n11\n3\n");
        assertTrue(out().contains("Invalid number"));
    }

    @Test @Order(8)
    void appOwnerInvalidOption() {
        runApp("1\nowner\npass\nZZ\n11\n3\n");
        assertTrue(out().contains("Unknown option"));
    }

    @Test @Order(9)
    void appManagerSession() {
        StringBuilder in = new StringBuilder();
        in.append("1\nmgr\nmgrpass\n");
        in.append("1\n");                                      // browse
        in.append("4\nmdoc\n").append(T()).append("\n3\n");   // register NORMAL
        in.append("2\nmdoc\n");                                // open
        in.append("3\nmdoc\n");                                // move
        in.append("6\nmdoc\n1\n");                             // lock
        in.append("6\nmdoc\n2\n");                             // unlock
        in.append("4\nmsens\n").append(T()).append("\n1\n");  // register SENSITIVE
        in.append("5\nmsens\n");                               // delete SENSITIVE -> proxy denied
        in.append("5\nmdoc\n");                                // delete NORMAL -> ok
        in.append("7\nmusr\nmp\nmp\n");                        // add user (forced USER)
        in.append("8\n");                                      // audit log
        in.append("ZZ\n");                                     // invalid
        in.append("9\n3\n");                                   // logout + exit
        runApp(in.toString());
        String o = out();
        assertTrue(o.contains("USER MENU [MANAGER]"));
        assertFalse(o.contains("OWNER DASHBOARD"));
        assertTrue(o.contains("PROXY DENIED") || o.contains("cannot perform"));
        assertTrue(o.contains("MANAGER can only create USER"));
        assertTrue(o.contains("Unknown option"));
        assertTrue(o.contains("LOGOUT"));
    }

    @Test @Order(10)
    void appUserSession() {
        FileRegistry.getInstance().register(new FileResource("shared", T(), FileType.NORMAL));
        StringBuilder in = new StringBuilder();
        in.append("1\nusr\nusrpass\n");
        in.append("1\n");                 // browse
        in.append("2\nshared\n");         // open
        in.append("3\nshared\n");         // move
        in.append("4\n");                 // request promotion
        in.append("ZZ\n");                // invalid
        in.append("5\n3\n");              // logout + exit
        runApp(in.toString());
        String o = out();
        assertTrue(o.contains("USER MENU [USER]"));
        assertTrue(o.contains("Promotion request submitted") || o.contains("pending promotion request"));
        assertTrue(o.contains("Unknown option"));
        assertTrue(o.contains("LOGOUT"));
    }

    @Test @Order(11)
    void appGuestSession() {
        runApp("2\n1\n2\nZZ\n3\n3\n");
        String o = out();
        assertTrue(o.contains("Limited access granted"));
        assertTrue(o.contains("USER MENU [GUEST]"));
        assertTrue(o.contains("Promotion request submitted"));
        assertTrue(o.contains("Unknown option"));
        assertTrue(o.contains("LOGOUT"));
    }

    @Test @Order(12)
    void appLoginWrongPassword() {
        runApp("1\nowner\nWRONG\n3\n");
        assertTrue(out().contains("Wrong password"));
    }

    @Test @Order(13)
    void appLoginAccountNotFound() {
        runApp("1\nnobody\nx\n3\n");
        assertTrue(out().contains("Account not found"));
    }

    @Test @Order(14)
    void appLoginInvalidMenuChoice() {
        runApp("9\n3\n");
        assertTrue(out().contains("Please select 1, 2, or 3"));
    }

    @Test @Order(15)
    void appSessionAndLoginLoopBreakOnEndOfInput() {
        // Login as owner, then input ends mid-session -> sessionLoop break, then loginLoop break.
        runApp("1\nowner\npass\n");
        assertTrue(out().contains("OWNER DASHBOARD"));
    }

    // ===========================================================
    //  UNIT TESTS
    // ===========================================================

    // ---- Enums ----
    @Test @Order(100)
    void enumsHaveExpectedValues() {
        assertEquals(4, Role.values().length);
        assertEquals(Role.OWNER, Role.valueOf("OWNER"));
        assertEquals(3, FileType.values().length);
        assertEquals(FileType.NORMAL, FileType.valueOf("NORMAL"));
        assertEquals(6, Operation.values().length);
        assertEquals(Operation.REGISTER, Operation.valueOf("REGISTER"));
        assertEquals(15, AccessEvent.values().length);
        assertEquals(AccessEvent.ACCESS_GRANTED, AccessEvent.valueOf("ACCESS_GRANTED"));
    }

    // ---- Colors / Banner ----
    @Test @Order(101)
    void colorsWrapText() {
        assertTrue(Colors.red("x").contains("x"));
        assertTrue(Colors.green("x").contains("x"));
        assertTrue(Colors.yellow("x").contains("x"));
        assertTrue(Colors.cyan("x").contains("x"));
        assertTrue(Colors.white("x").contains("x"));
        assertTrue(Colors.bold("x").contains("x"));
        assertTrue(Colors.purple("x").contains(Colors.PURPLE));
        assertNotNull(Colors.RESET);
    }

    @Test @Order(102)
    void bannerPrints() {
        Banner.printBanner();
        Banner.printFirstRunBanner();
        Banner.printSection("HELLO");
        assertTrue(out().contains("ProX4"));
        assertTrue(out().contains("HELLO"));
    }

    // ---- FileResource ----
    @Test @Order(103)
    void fileResourceGettersAndLock() {
        FileResource fr = new FileResource("doc", "/p/doc.txt", FileType.NORMAL);
        assertEquals("doc", fr.getName());
        assertEquals("/p/doc.txt", fr.getPath());
        assertEquals(FileType.NORMAL, fr.getType());
        assertFalse(fr.isLocked());
        fr.setLocked(true);
        assertTrue(fr.isLocked());
    }

    // ---- User (model) ----
    @Test @Order(104)
    void userCanReadRules() {
        assertEquals("M", new User("M", Role.MANAGER).getUsername());
        User m = new User("M", Role.MANAGER);
        assertTrue(m.canRead(new FileResource("a", "/p", FileType.SENSITIVE)));
        assertTrue(m.canRead(new FileResource("a", "/p", FileType.INTERNAL)));
        assertTrue(m.canRead(new FileResource("a", "/p", FileType.NORMAL)));

        User u = new User("U", Role.USER);
        assertFalse(u.canRead(new FileResource("a", "/p", FileType.SENSITIVE)));
        assertTrue(u.canRead(new FileResource("a", "/p", FileType.INTERNAL)));

        User g = new User("G", Role.GUEST);
        assertFalse(g.canRead(new FileResource("a", "/p", FileType.INTERNAL)));
        assertTrue(g.canRead(new FileResource("a", "/p", FileType.NORMAL)));
        assertEquals(Role.GUEST, g.getRole());

        assertFalse(m.canRead(null));
        assertFalse(m.canRead(new FileResource("a", "/p", null)));
    }

    // ---- PasswordManager / CryptoManager ----
    @Test @Order(105)
    void passwordHashAndVerify() {
        String h = PasswordManager.hash("secret");
        assertEquals(h, PasswordManager.hash("secret"));
        assertTrue(PasswordManager.verify("secret", h));
        assertFalse(PasswordManager.verify("nope", h));
    }

    @Test @Order(106)
    void cryptoRoundTripAndFailure() {
        String enc = CryptoManager.encrypt("payload");
        assertEquals("payload", CryptoManager.decrypt(enc));
        assertThrows(RuntimeException.class, () -> CryptoManager.decrypt("!!not-valid!!"));
    }

    // ---- UserAccount ----
    @Test @Order(107)
    void userAccountLockoutLifecycle() {
        UserAccount a = new UserAccount("acc", "pw", Role.USER);
        assertEquals("acc", a.getUsername());
        assertEquals(Role.USER, a.getRole());
        assertNotNull(a.getHashedPassword());
        assertFalse(a.isLocked());
        assertFalse(a.isMustChangePassword());
        assertNull(a.getLockoutUntil());

        a.setRole(Role.MANAGER);
        assertEquals(Role.MANAGER, a.getRole());
        a.setMustChangePassword(true);
        assertTrue(a.isMustChangePassword());
        a.setHashedPassword("abc");
        assertEquals("abc", a.getHashedPassword());

        a.incrementFailedAttempts();
        a.incrementFailedAttempts();
        a.incrementFailedAttempts();
        assertEquals(3, a.getFailedAttempts());
        assertTrue(a.isLocked());
        assertNotNull(a.getLockoutUntil());
        a.resetFailedAttempts();
        assertEquals(0, a.getFailedAttempts());
        assertFalse(a.isLocked());

        UserAccount ownerAcc = new UserAccount("o", "p", Role.OWNER);
        ownerAcc.incrementFailedAttempts();
        ownerAcc.incrementFailedAttempts();
        ownerAcc.incrementFailedAttempts();
        assertFalse(ownerAcc.isLocked()); // OWNER never locks
    }

    // ---- AuthenticationManager (singleton already populated) ----
    @Test @Order(108)
    void authSingletonAndLoginFlows() {
        assertSame(AuthenticationManager.getInstance(), AuthenticationManager.getInstance());

        // success
        assertNotNull(auth().login("owner", "pass"));
        // account not found
        assertNull(auth().login("ghost-user", "x"));
    }

    @Test @Order(109)
    void authLockoutAfterThreeFailures() {
        ensureUser("locktest", "right", Role.USER);
        assertNull(auth().login("locktest", "wrong")); // 1
        assertNull(auth().login("locktest", "wrong")); // 2
        assertNull(auth().login("locktest", "wrong")); // 3 -> locked
        assertTrue(out().contains("ACCOUNT LOCKED") || out().contains("Locked"));
        assertNull(auth().login("locktest", "right"));  // locked branch
        assertTrue(out().contains("Account is locked"));
    }

    @Test @Order(110)
    void authAddUserValidation() {
        // duplicate
        assertFalse(auth().addUser("owner", "x", Role.USER, owner()));
        // MANAGER may only add USER
        UserAccount mgr = ensureUser("mgr", "mgrpass", Role.MANAGER);
        assertFalse(auth().addUser("newmgr", "x", Role.MANAGER, mgr));
        assertTrue(out().contains("MANAGER can only add USER"));
        // success
        assertTrue(auth().addUser("addok", "x", Role.USER, owner()));
    }

    @Test @Order(111)
    void authRemoveUserValidation() {
        assertFalse(auth().removeUser("does-not-exist", owner()));
        assertFalse(auth().removeUser("owner", owner())); // cannot delete OWNER
        UserAccount mgr = ensureUser("mgr", "mgrpass", Role.MANAGER);
        ensureUser("rmtarget", "x", Role.USER);
        assertFalse(auth().removeUser("rmtarget", mgr)); // MANAGER cannot delete
        assertTrue(auth().removeUser("rmtarget", owner())); // OWNER ok
    }

    @Test @Order(112)
    void authPromoteValidation() {
        ensureUser("promoU", "x", Role.USER);
        // not owner
        UserAccount mgr = ensureUser("mgr", "mgrpass", Role.MANAGER);
        assertFalse(auth().promoteUser("promoU", mgr));
        // target not found
        assertFalse(auth().promoteUser("ghost", owner()));
        // promote OWNER
        assertFalse(auth().promoteUser("owner", owner()));
        // success USER -> MANAGER
        assertTrue(auth().promoteUser("promoU", owner()));
        // already MANAGER
        assertFalse(auth().promoteUser("promoU", owner()));
        // GUEST -> USER
        ensureUser("promoG", "x", Role.GUEST);
        assertTrue(auth().promoteUser("promoG", owner()));
        assertEquals(Role.USER, auth().getAccounts().get("promoG").getRole());
    }

    @Test @Order(113)
    void authDemoteValidation() {
        // not found
        assertFalse(auth().demoteUser("ghost", owner()));
        // not owner
        UserAccount mgr = ensureUser("mgr", "mgrpass", Role.MANAGER);
        assertFalse(auth().demoteUser("mgr", mgr));
        // demote OWNER
        assertFalse(auth().demoteUser("owner", owner()));
        // MANAGER -> USER
        ensureUser("demM", "x", Role.MANAGER);
        assertTrue(auth().demoteUser("demM", owner()));
        assertEquals(Role.USER, auth().getAccounts().get("demM").getRole());
        // USER -> GUEST
        assertTrue(auth().demoteUser("demM", owner()));
        assertEquals(Role.GUEST, auth().getAccounts().get("demM").getRole());
        // already GUEST
        assertFalse(auth().demoteUser("demM", owner()));
    }

    // ---- FileRegistry ----
    @Test @Order(114)
    void fileRegistryOperationsAndVisibility() {
        FileRegistry reg = FileRegistry.getInstance();
        reg.register(new FileResource("rs", "/p", FileType.SENSITIVE));
        reg.register(new FileResource("ri", "/p", FileType.INTERNAL));
        reg.register(new FileResource("rn", "/p", FileType.NORMAL));

        assertNotNull(reg.getFile("rs"));
        assertNull(reg.getFile("no-such"));
        assertFalse(reg.isEmpty());
        assertNotNull(reg.getAll());

        Map<String, FileResource> ownerView = reg.getAllVisibleTo(Role.OWNER);
        assertTrue(ownerView.containsKey("rs"));
        Map<String, FileResource> userView = reg.getAllVisibleTo(Role.USER);
        assertFalse(userView.containsKey("rs"));
        assertTrue(userView.containsKey("ri"));
        Map<String, FileResource> guestView = reg.getAllVisibleTo(Role.GUEST);
        assertFalse(guestView.containsKey("ri"));
        assertTrue(guestView.containsKey("rn"));

        reg.delete("rn");
        assertNull(reg.getFile("rn"));
    }

    // ---- FileLockManager ----
    @Test @Order(115)
    void fileLockManagerRules() {
        FileResource f = new FileResource("lf", "/p", FileType.NORMAL);
        UserAccount user = new UserAccount("u", "p", Role.USER);
        UserAccount mgr = new UserAccount("m", "p", Role.MANAGER);

        assertFalse(FileLockManager.lockFile(f, user));   // denied
        assertTrue(FileLockManager.lockFile(f, mgr));      // ok
        assertFalse(FileLockManager.lockFile(f, mgr));     // already locked
        assertFalse(FileLockManager.unlockFile(f, user));  // denied
        assertTrue(FileLockManager.unlockFile(f, mgr));    // ok
        assertFalse(FileLockManager.unlockFile(f, mgr));   // not locked
    }

    // ---- TimeAccessChecker ----
    @Test @Order(116)
    void timeAccessChecker() {
        TimeAccessChecker tc = new TimeAccessChecker();
        assertTrue(tc.isAccessAllowed());
        assertTrue(tc.getAccessWindow().contains(":00"));
    }

    // ---- RealFileAccess ----
    @Test @Order(117)
    void realFileAccessVariants() {
        UserAccount u = new UserAccount("Nawaf", "p", Role.MANAGER);
        RealFileAccess real = new RealFileAccess();

        real.openFile(new FileResource("t", T(), FileType.NORMAL), u);
        assertTrue(out().contains("FILE FOUND"));
        assertTrue(out().contains("Hello World"));
        outContent.reset();

        real.openFile(new FileResource("g", "/no/such/file.txt", FileType.NORMAL), u);
        assertTrue(out().contains("does not exist"));
        outContent.reset();

        real.openFile(new FileResource("d", tempDir.toString(), FileType.NORMAL), u);
        assertTrue(out().contains("not a file"));
        outContent.reset();

        real.openFile(new FileResource("e", emptyFile.toString(), FileType.NORMAL), u);
        assertTrue(out().contains("File is empty"));
    }

    // ---- SecureFileProxy ----
    @Test @Order(118)
    void proxyNullArgsAndLegacy() {
        SecureFileProxy proxy = new SecureFileProxy();
        proxy.execute(Operation.OPEN, null, new UserAccount("m", "p", Role.MANAGER));
        assertTrue(out().contains("PROXY ERROR"));
        outContent.reset();
        proxy.execute(Operation.OPEN, new FileResource("a", "/b", FileType.NORMAL), null);
        assertTrue(out().contains("PROXY ERROR"));
        outContent.reset();
        proxy.openFile(new FileResource("a", "/b", FileType.NORMAL), new UserAccount("m", "p", Role.MANAGER));
        assertTrue(out().contains("Use execute"));
    }

    @Test @Order(119)
    void proxyPermissionAndViewLimit() {
        SecureFileProxy proxy = new SecureFileProxy();
        UserAccount guest = new UserAccount("g", "p", Role.GUEST);
        UserAccount mgr = new UserAccount("m", "p", Role.MANAGER);
        UserAccount usr = new UserAccount("u", "p", Role.USER);

        // permission denied (guest on SENSITIVE)
        proxy.execute(Operation.OPEN, new FileResource("s", T(), FileType.SENSITIVE), guest);
        assertTrue(out().contains("PROXY DENIED"));
        outContent.reset();

        // granted manager sensitive
        proxy.execute(Operation.OPEN, new FileResource("s2", T(), FileType.SENSITIVE), mgr);
        assertTrue(out().contains("PROXY GRANTED"));
        outContent.reset();

        // view limit for USER
        FileResource n = new FileResource("nn", T(), FileType.NORMAL);
        proxy.execute(Operation.OPEN, n, usr);
        proxy.execute(Operation.OPEN, n, usr);
        proxy.execute(Operation.OPEN, n, usr);
        outContent.reset();
        proxy.execute(Operation.OPEN, n, usr);
        assertTrue(out().contains("Limit Reached"));
    }

    @Test @Order(120)
    void proxyLockBypassAndOps() {
        SecureFileProxy proxy = new SecureFileProxy();
        UserAccount mgr = new UserAccount("m", "p", Role.MANAGER);
        UserAccount ownerAcc = new UserAccount("o", "p", Role.OWNER);

        FileResource locked = new FileResource("lk", T(), FileType.NORMAL);
        locked.setLocked(true);
        proxy.execute(Operation.OPEN, locked, mgr);
        assertTrue(out().contains("File is locked"));
        outContent.reset();

        // OWNER bypasses lock & permission & view-limit
        proxy.execute(Operation.OPEN, locked, ownerAcc);
        assertTrue(out().contains("PROXY GRANTED"));
        outContent.reset();

        // DELETE rules
        proxy.execute(Operation.DELETE, new FileResource("dn", T(), FileType.NORMAL), mgr);
        assertTrue(out().contains("removed from registry"));
        outContent.reset();
        proxy.execute(Operation.DELETE, new FileResource("ds", T(), FileType.SENSITIVE), mgr);
        assertTrue(out().contains("PROXY DENIED"));
        outContent.reset();
        proxy.execute(Operation.DELETE, new FileResource("ds2", T(), FileType.SENSITIVE), ownerAcc);
        assertTrue(out().contains("removed from registry"));
        outContent.reset();

        // LOCK / UNLOCK / REGISTER / MOVE
        FileResource lf = new FileResource("opf", T(), FileType.NORMAL);
        proxy.execute(Operation.LOCK, lf, mgr);
        assertTrue(lf.isLocked());
        assertTrue(out().contains("now locked"));
        outContent.reset();
        // UNLOCK must be on an unlocked file (the lock-check denies non-owners on locked files)
        FileResource uf = new FileResource("opf2", T(), FileType.NORMAL);
        proxy.execute(Operation.UNLOCK, uf, mgr);
        assertTrue(out().contains("now unlocked"));
        outContent.reset();
        proxy.execute(Operation.REGISTER, new FileResource("rg", T(), FileType.NORMAL), mgr);
        assertTrue(out().contains("Operation noted"));
        outContent.reset();
        proxy.execute(Operation.MOVE, new FileResource("mv", T(), FileType.NORMAL), mgr);
        assertTrue(out().contains("MOVE SUCCESS"));
        outContent.reset();
        proxy.execute(Operation.MOVE, new FileResource("mvx", "Z:\\no\\such.txt", FileType.NORMAL), mgr);
        assertTrue(out().contains("Source file not found"));
    }

    @Test @Order(121)
    void proxyObservers() {
        SecureFileProxy proxy = new SecureFileProxy();
        final boolean[] notified = {false};
        AccessObserver obs = (e, u, f) -> { if (e == AccessEvent.ACCESS_GRANTED) notified[0] = true; };
        proxy.addObserver(obs);
        proxy.execute(Operation.OPEN, new FileResource("o", T(), FileType.NORMAL),
                new UserAccount("m", "p", Role.MANAGER));
        assertTrue(notified[0]);
        notified[0] = false;
        proxy.removeObserver(obs);
        proxy.execute(Operation.OPEN, new FileResource("o", T(), FileType.NORMAL),
                new UserAccount("m", "p", Role.MANAGER));
        assertFalse(notified[0]);
    }

    // ---- DownloadProxy ----
    @Test @Order(122)
    void downloadProxyRules() {
        DownloadProxy dp = new DownloadProxy();
        dp.downloadFile(new FileResource("s", T(), FileType.SENSITIVE), new UserAccount("m", "p", Role.MANAGER));
        assertTrue(out().contains("DOWNLOAD SUCCESS"));
        outContent.reset();
        dp.downloadFile(new FileResource("n", T(), FileType.NORMAL), new UserAccount("g", "p", Role.GUEST));
        assertTrue(out().contains("DOWNLOAD DENIED"));
        outContent.reset();
        dp.downloadFile(new FileResource("s", T(), FileType.SENSITIVE), new UserAccount("u", "p", Role.USER));
        assertTrue(out().contains("DOWNLOAD DENIED"));
        outContent.reset();
        dp.downloadFile(new FileResource("i", T(), FileType.INTERNAL), new UserAccount("u", "p", Role.USER));
        assertTrue(out().contains("DOWNLOAD SUCCESS"));
        outContent.reset();
        dp.downloadFile(new FileResource("n", T(), FileType.NORMAL), new UserAccount("u", "p", Role.USER));
        assertTrue(out().contains("DOWNLOAD SUCCESS"));
        outContent.reset();
        dp.downloadFile(new FileResource("g", "/no/such.txt", FileType.NORMAL), new UserAccount("m", "p", Role.MANAGER));
        assertTrue(out().contains("Source file not found"));
    }

    // ---- AlertObserver ----
    @Test @Order(123)
    void alertObserverEvents() {
        AlertObserver a = new AlertObserver();
        UserAccount u = new UserAccount("X", "p", Role.MANAGER);
        FileResource f = new FileResource("f", "/p", FileType.NORMAL);
        a.onAccessEvent(AccessEvent.ACCESS_DENIED, u, f);
        a.onAccessEvent(AccessEvent.LIMIT_REACHED, u, f);
        a.onAccessEvent(AccessEvent.ACCOUNT_LOCKED, u, f);
        a.onAccessEvent(AccessEvent.USER_CREATED, u, f);
        a.onAccessEvent(AccessEvent.USER_PROMOTED, u, f);
        a.onAccessEvent(AccessEvent.USER_DEMOTED, u, f);
        a.onAccessEvent(AccessEvent.FILE_LOCKED, u, f);
        a.onAccessEvent(AccessEvent.FILE_UNLOCKED, u, f);
        a.onAccessEvent(AccessEvent.ACCESS_GRANTED, u, f); // default (silent)
        a.onAccessEvent(AccessEvent.ACCESS_DENIED, null, null); // null branch
        String o = out();
        assertTrue(o.contains("SECURITY ALERT"));
        assertTrue(o.contains("View limit exceeded"));
        assertTrue(o.contains("AUDIT"));
        assertTrue(o.contains("SYSTEM"));
    }

    // ---- SecurityLogger ----
    @Test @Order(124)
    void securityLoggerWritesConsoleAndFile() throws IOException {
        Files.deleteIfExists(Paths.get("access_log.txt"));
        SecurityLogger logger = new SecurityLogger();
        logger.onAccessEvent(AccessEvent.ACCESS_GRANTED,
                new UserAccount("LogTest", "p", Role.MANAGER),
                new FileResource("file", "/p", FileType.NORMAL));
        logger.onAccessEvent(AccessEvent.ACCESS_DENIED, null, null); // SYSTEM/N/A branch
        assertTrue(out().contains("[LOG]"));
        assertTrue(Files.exists(Paths.get("access_log.txt")));
        String content = new String(Files.readAllBytes(Paths.get("access_log.txt")));
        assertTrue(content.contains("LogTest"));
        assertTrue(content.contains("SYSTEM"));
    }

    // ---- AuditLogViewer ----
    @Test @Order(125)
    void auditLogViewerStates() throws IOException {
        // missing
        Files.deleteIfExists(Paths.get("access_log.txt"));
        AuditLogViewer.viewLog(10);
        assertTrue(out().contains("No audit log found"));
        outContent.reset();

        // empty file
        new PrintWriter("access_log.txt").close();
        AuditLogViewer.viewLog(10);
        assertTrue(out().contains("Audit log is empty"));
        outContent.reset();

        // with categorized lines
        try (PrintWriter pw = new PrintWriter(new FileWriter("access_log.txt"))) {
            pw.println("EVENT ACCESS_DENIED here");
            pw.println("EVENT ACCESS_GRANTED here");
            pw.println("EVENT PASSWORD_CHANGED here");
        }
        AuditLogViewer.viewLog(10);
        String o = out();
        assertTrue(o.contains("Audit Log"));
        outContent.reset();

        AuditLogViewer.clearLog();
        assertTrue(out().contains("Audit log cleared"));
    }

    // ---- MenuRenderer ----
    @Test @Order(126)
    void menuRendererAllRoles() {
        MenuRenderer.showMenu(Role.OWNER);
        MenuRenderer.showMenu(Role.MANAGER);
        MenuRenderer.showMenu(Role.USER);
        MenuRenderer.showMenu(Role.GUEST);
        String o = out();
        assertTrue(o.contains("Manage Users"));
        assertTrue(o.contains("Request Promotion"));
    }

    // ---- OwnerDashboard ----
    @Test @Order(127)
    void ownerDashboardWithAndWithoutLog() throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter("access_log.txt"))) {
            pw.println("EVENT ACCESS_DENIED a");
            pw.println("EVENT LOGIN_FAILED b");
        }
        OwnerDashboard.show(auth());
        assertTrue(out().contains("OWNER DASHBOARD"));
        assertTrue(out().contains("Security Violations"));
        outContent.reset();

        Files.deleteIfExists(Paths.get("access_log.txt"));
        OwnerDashboard.show(auth());
        assertTrue(out().contains("OWNER DASHBOARD"));
    }

    // ---- PromotionRequest ----
    @Test @Order(128)
    void promotionRequestNextRole() {
        PromotionRequest g = new PromotionRequest("g", Role.GUEST);
        assertEquals(Role.USER, g.getRequestedRole());
        PromotionRequest u = new PromotionRequest("u", Role.USER);
        assertEquals(Role.MANAGER, u.getRequestedRole());
        PromotionRequest m = new PromotionRequest("m", Role.MANAGER);
        assertEquals(Role.MANAGER, m.getRequestedRole()); // default branch
        assertEquals("u", u.getRequesterUsername());
        assertEquals(Role.USER, u.getCurrentRole());
        assertNotNull(u.getRequestTime());
        assertTrue(u.isPending());
        u.setPending(false);
        assertFalse(u.isPending());
    }

    // ---- PromotionManager ----
    @Test @Order(129)
    void promotionManagerFlows() {
        PromotionManager pm = PromotionManager.getInstance();
        assertSame(pm, PromotionManager.getInstance());

        // manager/owner cannot be promoted
        pm.requestPromotion(new UserAccount("mm", "p", Role.MANAGER));
        assertTrue(out().contains("cannot be promoted"));
        outContent.reset();

        UserAccount cand = new UserAccount("candUser", "p", Role.USER);
        pm.requestPromotion(cand);
        assertTrue(out().contains("Promotion request submitted"));
        outContent.reset();
        // duplicate
        pm.requestPromotion(cand);
        assertTrue(out().contains("already have a pending"));
        outContent.reset();

        pm.showPendingRequests();
        assertTrue(out().contains("Pending Promotion Requests"));
        outContent.reset();

        // invalid range approve/reject
        pm.approveRequest(999, owner(), auth());
        assertTrue(out().contains("out of range"));
        outContent.reset();
        pm.rejectRequest(999);
        assertTrue(out().contains("out of range"));
    }

    // ---- UserManager direct branches (OWNER-only guards + empty input) ----
    @Test @Order(130)
    void userManagerGuardAndEmptyBranches() {
        UserAccount mgr = ensureUser("mgr", "mgrpass", Role.MANAGER);
        UserManager umMgr = new UserManager(auth(), new Scanner(new ByteArrayInputStream("".getBytes())));
        umMgr.removeUser(mgr);
        umMgr.promoteUser(mgr);
        umMgr.demoteUser(mgr);
        String o = out();
        assertTrue(o.contains("Only OWNER can remove"));
        assertTrue(o.contains("Only OWNER can promote"));
        assertTrue(o.contains("Only OWNER can demote"));
        outContent.reset();

        UserManager umEmpty = new UserManager(auth(), new Scanner(new ByteArrayInputStream("\n\n\n".getBytes())));
        umEmpty.removeUser(owner());
        umEmpty.promoteUser(owner());
        umEmpty.demoteUser(owner());
        assertTrue(out().contains("Username cannot be empty"));
    }

    // ---- SecureFileProxy permission matrix (branch coverage) ----
    @Test @Order(131)
    void proxyPermissionMatrix() {
        SecureFileProxy proxy = new SecureFileProxy();
        UserAccount usr = new UserAccount("u", "p", Role.USER);
        UserAccount guest = new UserAccount("g", "p", Role.GUEST);
        UserAccount mgr = new UserAccount("m", "p", Role.MANAGER);

        proxy.execute(Operation.OPEN, new FileResource("i1", T(), FileType.INTERNAL), usr);   // granted
        assertTrue(out().contains("PROXY GRANTED")); outContent.reset();
        proxy.execute(Operation.OPEN, new FileResource("i2", T(), FileType.INTERNAL), guest); // denied
        assertTrue(out().contains("PROXY DENIED")); outContent.reset();
        proxy.execute(Operation.OPEN, new FileResource("s1", T(), FileType.SENSITIVE), usr);  // denied
        assertTrue(out().contains("PROXY DENIED")); outContent.reset();
        proxy.execute(Operation.MOVE, new FileResource("n1", T(), FileType.NORMAL), guest);   // denied
        assertTrue(out().contains("PROXY DENIED")); outContent.reset();

        proxy.execute(Operation.DELETE, new FileResource("n2", T(), FileType.NORMAL), usr);   // USER delete -> denied
        assertTrue(out().contains("PROXY DENIED")); outContent.reset();
        proxy.execute(Operation.DELETE, new FileResource("i3", T(), FileType.INTERNAL), mgr); // MANAGER delete non-NORMAL -> denied
        assertTrue(out().contains("PROXY DENIED")); outContent.reset();
        proxy.execute(Operation.LOCK, new FileResource("n3", T(), FileType.NORMAL), usr);     // USER lock -> denied
        assertTrue(out().contains("PROXY DENIED")); outContent.reset();
        proxy.execute(Operation.UNLOCK, new FileResource("n4", T(), FileType.NORMAL), guest); // GUEST unlock -> denied
        assertTrue(out().contains("PROXY DENIED")); outContent.reset();
        proxy.execute(Operation.REGISTER, new FileResource("n5", T(), FileType.NORMAL), usr); // USER register -> denied
        assertTrue(out().contains("PROXY DENIED"));
    }

    // ---- Utility-class default constructors (line coverage) ----
    @Test @Order(132)
    void utilityClassConstructors() {
        assertNotNull(new App());
        assertNotNull(new Banner());
        assertNotNull(new Colors());
        assertNotNull(new MenuRenderer());
        assertNotNull(new FileLockManager());
        assertNotNull(new AuditLogViewer());
        assertNotNull(new OwnerDashboard());
        assertNotNull(new DatabaseManager());
        assertNotNull(new PasswordManager());
        assertNotNull(new CryptoManager());
    }

    // ---- AuditLogViewer categorization + SecurityLogger/AuditLogViewer error paths ----
    @Test @Order(133)
    void auditAndLoggerErrorPaths() throws IOException {
        // Cover every color-categorization sub-branch.
        try (PrintWriter pw = new PrintWriter(new FileWriter("access_log.txt"))) {
            pw.println("evt DENIED a");
            pw.println("evt LOCKED a");
            pw.println("evt FAILED a");
            pw.println("evt GRANTED a");
            pw.println("evt SUCCESS a");
            pw.println("evt CREATED a");
            pw.println("evt NEUTRAL a");
        }
        AuditLogViewer.viewLog(50);
        assertTrue(out().contains("Audit Log"));
        outContent.reset();

        // Turn access_log.txt into a DIRECTORY so all file operations throw -> catch branches.
        Files.deleteIfExists(Paths.get("access_log.txt"));
        Files.createDirectory(Paths.get("access_log.txt"));
        try {
            new SecurityLogger().onAccessEvent(AccessEvent.ACCESS_GRANTED,
                    new UserAccount("E", "p", Role.MANAGER),
                    new FileResource("f", "/p", FileType.NORMAL));
            assertTrue(out().contains("LOG ERROR"));
            outContent.reset();

            AuditLogViewer.viewLog(10);
            assertTrue(out().contains("Could not read log"));
            outContent.reset();

            AuditLogViewer.clearLog();
            assertTrue(out().contains("Could not clear log"));
        } finally {
            Files.deleteIfExists(Paths.get("access_log.txt"));
        }
    }

    // ---- DownloadProxy extra branches + IOException catch ----
    @Test @Order(134)
    void downloadProxyBranchesAndError() throws IOException {
        DownloadProxy dp = new DownloadProxy();
        dp.downloadFile(new FileResource("dn", T(), FileType.NORMAL), new UserAccount("m", "p", Role.MANAGER));
        assertTrue(out().contains("DOWNLOAD SUCCESS")); outContent.reset();
        dp.downloadFile(new FileResource("ds", T(), FileType.SENSITIVE), new UserAccount("g", "p", Role.GUEST));
        assertTrue(out().contains("DOWNLOAD DENIED")); outContent.reset();
        dp.downloadFile(new FileResource("di", T(), FileType.INTERNAL), new UserAccount("g", "p", Role.GUEST));
        assertTrue(out().contains("DOWNLOAD DENIED")); outContent.reset();

        // Make "downloads" a regular file so the copy fails -> IOException catch.
        deleteDir(Paths.get("downloads"));
        Files.write(Paths.get("downloads"), "x".getBytes());
        try {
            dp.downloadFile(new FileResource("derr", T(), FileType.NORMAL), new UserAccount("m", "p", Role.MANAGER));
            assertTrue(out().contains("DOWNLOAD ERROR"));
        } finally {
            Files.deleteIfExists(Paths.get("downloads"));
        }
    }

    // ---- DatabaseManager (run last; restores users.dat) ----
    @Test @Order(200)
    void databaseManagerRoundTripAndErrors() throws IOException {
        byte[] backup = Files.readAllBytes(Paths.get("users.dat"));
        try {
            assertFalse(DatabaseManager.isFirstRun());

            Map<String, UserAccount> map = new HashMap<>();
            map.put("z", new UserAccount("z", "pw", Role.USER));
            DatabaseManager.save(map);
            Map<String, UserAccount> loaded = DatabaseManager.load();
            assertNotNull(loaded);
            assertTrue(loaded.containsKey("z"));

            // corrupted content -> load returns empty map (catch branch)
            Files.write(Paths.get("users.dat"), "garbage-not-encrypted".getBytes());
            Map<String, UserAccount> bad = DatabaseManager.load();
            assertNotNull(bad);
            assertTrue(bad.isEmpty());

            // missing file -> null
            Files.deleteIfExists(Paths.get("users.dat"));
            assertNull(DatabaseManager.load());
            assertTrue(DatabaseManager.isFirstRun());
        } finally {
            Files.write(Paths.get("users.dat"), backup);
        }
    }
}
