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
}
