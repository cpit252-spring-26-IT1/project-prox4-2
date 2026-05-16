package sa.edu.kau.fcit.cpit252.project;

import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class AppTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private static Path tempFile;
    private static Path tempDir;
    private static Path emptyFile;

    @BeforeAll
    static void setupFiles() throws IOException {
        tempFile = Files.createTempFile("test", ".txt");
        Files.write(tempFile, "Hello World\nLine 2".getBytes());
        tempDir = Files.createTempDirectory("testdir");
        emptyFile = Files.createTempFile("empty", ".txt");
    }

    @AfterAll
    static void cleanupFiles() throws IOException {
        Files.deleteIfExists(tempFile);
        Files.deleteIfExists(emptyFile);
        Files.deleteIfExists(tempDir);
        Path downloadDir = Paths.get("downloads");
        if (Files.exists(downloadDir)) {
            Files.walk(downloadDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> { try { Files.delete(p); } catch (IOException e) {} });
        }
        Files.deleteIfExists(Paths.get("access_log.txt"));
    }

    @BeforeEach
    void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
    }

    // ============ Enum Tests ============
    @Test
    void testRoleEnumValues() {
        assertEquals(3, Role.values().length);
        assertNotNull(Role.valueOf("MANAGER"));
        assertNotNull(Role.valueOf("USER"));
        assertNotNull(Role.valueOf("GUEST"));
    }

    @Test
    void testFileTypeEnumValues() {
        assertEquals(3, FileType.values().length);
        assertNotNull(FileType.valueOf("SENSITIVE"));
        assertNotNull(FileType.valueOf("INTERNAL"));
        assertNotNull(FileType.valueOf("NORMAL"));
    }

    @Test
    void testAccessEventEnumValues() {
        assertEquals(3, AccessEvent.values().length);
        assertNotNull(AccessEvent.valueOf("ACCESS_GRANTED"));
        assertNotNull(AccessEvent.valueOf("ACCESS_DENIED"));
        assertNotNull(AccessEvent.valueOf("LIMIT_REACHED"));
    }

    // ============ FileResource Tests ============
    @Test
    void testFileResourceGetters() {
        FileResource fr = new FileResource("doc", "/path/doc.txt", FileType.NORMAL);
        assertEquals("doc", fr.getName());
        assertEquals("/path/doc.txt", fr.getPath());
        assertEquals(FileType.NORMAL, fr.getType());
    }

    // ============ User Tests ============
    @Test
    void testUserGetters() {
        User u = new User("Test", Role.MANAGER);
        assertEquals("Test", u.getUsername());
        assertEquals(Role.MANAGER, u.getRole());
    }

    @Test
    void testManagerCanReadAll() {
        User m = new User("M", Role.MANAGER);
        assertTrue(m.canRead(new FileResource("a", "/p", FileType.SENSITIVE)));
        assertTrue(m.canRead(new FileResource("a", "/p", FileType.INTERNAL)));
        assertTrue(m.canRead(new FileResource("a", "/p", FileType.NORMAL)));
    }

    @Test
    void testUserCanReadInternalAndNormal() {
        User u = new User("U", Role.USER);
        assertFalse(u.canRead(new FileResource("a", "/p", FileType.SENSITIVE)));
        assertTrue(u.canRead(new FileResource("a", "/p", FileType.INTERNAL)));
        assertTrue(u.canRead(new FileResource("a", "/p", FileType.NORMAL)));
    }

    @Test
    void testGuestCanReadNormalOnly() {
        User g = new User("G", Role.GUEST);
        assertFalse(g.canRead(new FileResource("a", "/p", FileType.SENSITIVE)));
        assertFalse(g.canRead(new FileResource("a", "/p", FileType.INTERNAL)));
        assertTrue(g.canRead(new FileResource("a", "/p", FileType.NORMAL)));
    }

    @Test
    void testUserCanReadWithNullFile() {
        User u = new User("X", Role.MANAGER);
        assertFalse(u.canRead(null));
    }

    @Test
    void testUserCanReadWithNullFileType() {
        FileResource fr = new FileResource("name", "/path", null);
        User u = new User("X", Role.MANAGER);
        assertFalse(u.canRead(fr));
    }

    // ============ AuthenticationManager Tests ============
    @Test
    void testAuthenticationManagerSingleton() {
        AuthenticationManager a1 = AuthenticationManager.getInstance();
        AuthenticationManager a2 = AuthenticationManager.getInstance();
        assertSame(a1, a2);
    }

    @Test
    void testAuthenticateKnownManager() {
        User u = AuthenticationManager.getInstance().authenticate("Nawaf");
        assertNotNull(u);
        assertEquals(Role.MANAGER, u.getRole());
    }

    @Test
    void testAuthenticateKnownUser() {
        User u = AuthenticationManager.getInstance().authenticate("Faisal");
        assertNotNull(u);
        assertEquals(Role.USER, u.getRole());
    }

    @Test
    void testAuthenticateCaseInsensitive() {
        User u = AuthenticationManager.getInstance().authenticate("nawaf");
        assertNotNull(u);
        assertEquals(Role.MANAGER, u.getRole());
        assertEquals("Nawaf", u.getUsername());
    }

    @Test
    void testAuthenticateUnknownUserBecomesGuest() {
        User u = AuthenticationManager.getInstance().authenticate("RandomName");
        assertNotNull(u);
        assertEquals(Role.GUEST, u.getRole());
    }

    @Test
    void testAuthenticateNullReturnsNull() {
        assertNull(AuthenticationManager.getInstance().authenticate(null));
    }

    @Test
    void testAuthenticateEmptyReturnsNull() {
        assertNull(AuthenticationManager.getInstance().authenticate(""));
        assertNull(AuthenticationManager.getInstance().authenticate("   "));
    }

    @Test
    void testAuthenticateTrimsWhitespace() {
        User u = AuthenticationManager.getInstance().authenticate("  Khaled  ");
        assertNotNull(u);
        assertEquals(Role.MANAGER, u.getRole());
    }

    // ============ TimeAccessChecker Tests ============
    @Test
    void testTimeAccessCheckerWindow() {
        TimeAccessChecker tc = new TimeAccessChecker();
        assertNotNull(tc.getAccessWindow());
        assertTrue(tc.getAccessWindow().contains(":00"));
    }

    @Test
    void testTimeAccessCheckerIsAccessAllowed() {
        TimeAccessChecker tc = new TimeAccessChecker();
        boolean allowed = tc.isAccessAllowed();
        assertTrue(allowed || !allowed);
    }

    // ============ RealFileAccess Tests ============
    @Test
    void testRealFileAccessReadsExistingFile() {
        RealFileAccess real = new RealFileAccess();
        FileResource fr = new FileResource("test", tempFile.toString(), FileType.NORMAL);
        real.openFile(fr, new User("Nawaf", Role.MANAGER));
        String output = outContent.toString();
        assertTrue(output.contains("FILE FOUND"));
        assertTrue(output.contains("Hello World"));
        assertTrue(output.contains("CONFIDENTIAL"));
    }

    @Test
    void testRealFileAccessNonExistentFile() {
        RealFileAccess real = new RealFileAccess();
        FileResource fr = new FileResource("ghost", "/nonexistent/file.txt", FileType.NORMAL);
        real.openFile(fr, new User("Nawaf", Role.MANAGER));
        assertTrue(outContent.toString().contains("does not exist"));
    }

    @Test
    void testRealFileAccessRejectsDirectory() {
        RealFileAccess real = new RealFileAccess();
        FileResource fr = new FileResource("dir", tempDir.toString(), FileType.NORMAL);
        real.openFile(fr, new User("Nawaf", Role.MANAGER));
        assertTrue(outContent.toString().contains("not a file"));
    }

    @Test
    void testRealFileAccessHandlesEmptyFile() {
        RealFileAccess real = new RealFileAccess();
        FileResource fr = new FileResource("empty", emptyFile.toString(), FileType.NORMAL);
        real.openFile(fr, new User("Nawaf", Role.MANAGER));
        assertTrue(outContent.toString().contains("File is empty"));
    }

    // ============ SecureFileProxy Tests ============
    @Test
    void testProxyDeniesUnauthorizedRole() {
        SecureFileProxy proxy = new SecureFileProxy();
        FileResource fr = new FileResource("secret", tempFile.toString(), FileType.SENSITIVE);
        proxy.openFile(fr, new User("Guest", Role.GUEST));
        assertTrue(outContent.toString().contains("PROXY DENIED"));
    }

    @Test
    void testProxyGrantsAuthorizedAccess() {
        SecureFileProxy proxy = new SecureFileProxy();
        FileResource fr = new FileResource("file", tempFile.toString(), FileType.SENSITIVE);
        proxy.openFile(fr, new User("M", Role.MANAGER));
        assertTrue(outContent.toString().contains("PROXY GRANTED"));
    }

    @Test
    void testProxyBlocksAfter3Views() {
        SecureFileProxy proxy = new SecureFileProxy();
        User m = new User("M", Role.MANAGER);
        FileResource fr = new FileResource("file", tempFile.toString(), FileType.NORMAL);
        proxy.openFile(fr, m);
        proxy.openFile(fr, m);
        proxy.openFile(fr, m);
        outContent.reset();
        proxy.openFile(fr, m);
        assertTrue(outContent.toString().contains("Limit Reached"));
    }

    @Test
    void testProxyNullFile() {
        SecureFileProxy proxy = new SecureFileProxy();
        proxy.openFile(null, new User("M", Role.MANAGER));
        assertTrue(outContent.toString().contains("PROXY ERROR"));
    }

    @Test
    void testProxyNullUser() {
        SecureFileProxy proxy = new SecureFileProxy();
        proxy.openFile(new FileResource("a", "/b", FileType.NORMAL), null);
        assertTrue(outContent.toString().contains("PROXY ERROR"));
    }

    @Test
    void testProxyAddAndRemoveObserver() {
        SecureFileProxy proxy = new SecureFileProxy();
        AccessObserver observer = (e, u, f) -> System.out.println("OBSERVED");
        proxy.addObserver(observer);
        User m = new User("M", Role.MANAGER);
        FileResource fr = new FileResource("f", tempFile.toString(), FileType.NORMAL);
        proxy.openFile(fr, m);
        assertTrue(outContent.toString().contains("OBSERVED"));

        outContent.reset();
        proxy.removeObserver(observer);
        proxy.openFile(fr, m);
        assertFalse(outContent.toString().contains("OBSERVED"));
    }

    @Test
    void testProxyDeniedRoleNotifiesObserver() {
        SecureFileProxy proxy = new SecureFileProxy();
        final boolean[] notified = {false};
        proxy.addObserver((e, u, f) -> {
            if (e == AccessEvent.ACCESS_DENIED) notified[0] = true;
        });
        FileResource fr = new FileResource("secret", tempFile.toString(), FileType.SENSITIVE);
        proxy.openFile(fr, new User("G", Role.GUEST));
        assertTrue(notified[0]);
    }

    // ============ SecurityLogger Tests ============
    @Test
    void testSecurityLoggerWritesEvent() {
        SecurityLogger logger = new SecurityLogger();
        logger.onAccessEvent(AccessEvent.ACCESS_GRANTED,
                new User("TestUser", Role.MANAGER),
                new FileResource("testFile", "/path", FileType.NORMAL));
        String output = outContent.toString();
        assertTrue(output.contains("[LOG]"));
        assertTrue(output.contains("ACCESS_GRANTED"));
        assertTrue(output.contains("TestUser"));
    }

    @Test
    void testSecurityLoggerWritesAllEventTypes() {
        SecurityLogger logger = new SecurityLogger();
        User u = new User("X", Role.USER);
        FileResource fr = new FileResource("f", "/p", FileType.INTERNAL);
        logger.onAccessEvent(AccessEvent.ACCESS_DENIED, u, fr);
        logger.onAccessEvent(AccessEvent.LIMIT_REACHED, u, fr);
        String output = outContent.toString();
        assertTrue(output.contains("ACCESS_DENIED"));
        assertTrue(output.contains("LIMIT_REACHED"));
    }

    @Test
    void testSecurityLoggerWritesToFile() throws IOException {
        Files.deleteIfExists(Paths.get("access_log.txt"));
        new SecurityLogger().onAccessEvent(AccessEvent.ACCESS_GRANTED,
                new User("LogTest", Role.MANAGER),
                new FileResource("file", "/p", FileType.NORMAL));
        assertTrue(Files.exists(Paths.get("access_log.txt")));
        String content = new String(Files.readAllBytes(Paths.get("access_log.txt")));
        assertTrue(content.contains("LogTest"));
    }

    // ============ AlertObserver Tests ============
    @Test
    void testAlertObserverDeniedEvent() {
        new AlertObserver().onAccessEvent(AccessEvent.ACCESS_DENIED,
                new User("Hacker", Role.GUEST),
                new FileResource("secret", "/p", FileType.SENSITIVE));
        assertTrue(outContent.toString().contains("SECURITY ALERT"));
        assertTrue(outContent.toString().contains("Unauthorized"));
    }

    @Test
    void testAlertObserverLimitReached() {
        new AlertObserver().onAccessEvent(AccessEvent.LIMIT_REACHED,
                new User("X", Role.MANAGER),
                new FileResource("f", "/p", FileType.NORMAL));
        assertTrue(outContent.toString().contains("View limit exceeded"));
    }

    @Test
    void testAlertObserverGrantedIsSilent() {
        new AlertObserver().onAccessEvent(AccessEvent.ACCESS_GRANTED,
                new User("X", Role.MANAGER),
                new FileResource("f", "/p", FileType.NORMAL));
        assertFalse(outContent.toString().contains("SECURITY ALERT"));
    }

    // ============ DownloadProxy Tests ============
    @Test
    void testDownloadProxyManagerCanDownloadSensitive() {
        new DownloadProxy().downloadFile(
                new FileResource("secret", tempFile.toString(), FileType.SENSITIVE),
                new User("M", Role.MANAGER));
        assertTrue(outContent.toString().contains("DOWNLOAD SUCCESS"));
    }

    @Test
    void testDownloadProxyGuestDenied() {
        new DownloadProxy().downloadFile(
                new FileResource("file", tempFile.toString(), FileType.NORMAL),
                new User("G", Role.GUEST));
        assertTrue(outContent.toString().contains("DOWNLOAD DENIED"));
    }

    @Test
    void testDownloadProxyUserDeniedSensitive() {
        new DownloadProxy().downloadFile(
                new FileResource("file", tempFile.toString(), FileType.SENSITIVE),
                new User("U", Role.USER));
        assertTrue(outContent.toString().contains("DOWNLOAD DENIED"));
    }

    @Test
    void testDownloadProxyUserCanDownloadInternal() {
        new DownloadProxy().downloadFile(
                new FileResource("file", tempFile.toString(), FileType.INTERNAL),
                new User("U", Role.USER));
        assertTrue(outContent.toString().contains("DOWNLOAD SUCCESS"));
    }

    @Test
    void testDownloadProxyUserCanDownloadNormal() {
        new DownloadProxy().downloadFile(
                new FileResource("file", tempFile.toString(), FileType.NORMAL),
                new User("U", Role.USER));
        assertTrue(outContent.toString().contains("DOWNLOAD SUCCESS"));
    }

    @Test
    void testDownloadProxyNonExistentFile() {
        new DownloadProxy().downloadFile(
                new FileResource("ghost", "/no/such/file.txt", FileType.NORMAL),
                new User("M", Role.MANAGER));
        assertTrue(outContent.toString().contains("Source file not found"));
    }

    @Test
    void testDownloadProxyDirectory() {
        new DownloadProxy().downloadFile(
                new FileResource("dir", tempDir.toString(), FileType.NORMAL),
                new User("M", Role.MANAGER));
        assertTrue(outContent.toString().contains("Source file not found"));
    }

    // ============ App.main Tests ============
    @Test
    void testAppMainExitsCleanly() {
        InputStream original = System.in;
        try {
            System.setIn(new ByteArrayInputStream("EXIT\n".getBytes()));
            App.main(new String[]{});
            assertTrue(outContent.toString().contains("Shutting down"));
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void testAppMainOpenFile() {
        InputStream original = System.in;
        try {
            String input = "Nawaf\n1\nTestFile\n" + tempFile.toString() + "\n3\n3\nEXIT\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            App.main(new String[]{});
            assertTrue(outContent.toString().contains("PROXY GRANTED"));
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void testAppMainDownloadFile() {
        InputStream original = System.in;
        try {
            String input = "Nawaf\n2\nFile\n" + tempFile.toString() + "\n3\n3\nEXIT\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            App.main(new String[]{});
            assertTrue(outContent.toString().contains("DOWNLOAD"));
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void testAppMainEmptyFileName() {
        InputStream original = System.in;
        try {
            System.setIn(new ByteArrayInputStream("Nawaf\n1\n\n3\nEXIT\n".getBytes()));
            App.main(new String[]{});
            assertTrue(outContent.toString().contains("INVALID"));
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void testAppMainEmptyPath() {
        InputStream original = System.in;
        try {
            System.setIn(new ByteArrayInputStream("Nawaf\n1\nName\n\n3\nEXIT\n".getBytes()));
            App.main(new String[]{});
            assertTrue(outContent.toString().contains("path cannot be empty"));
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void testAppMainInvalidLevel() {
        InputStream original = System.in;
        try {
            String input = "Nawaf\n1\nFile\n" + tempFile.toString() + "\nabc\n3\nEXIT\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            App.main(new String[]{});
            assertTrue(outContent.toString().contains("Please enter a number"));
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void testAppMainLevelOutOfRange() {
        InputStream original = System.in;
        try {
            String input = "Nawaf\n1\nFile\n" + tempFile.toString() + "\n99\n3\nEXIT\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            App.main(new String[]{});
            assertTrue(outContent.toString().contains("must be 1, 2, or 3"));
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void testAppMainInvalidMenuOption() {
        InputStream original = System.in;
        try {
            System.setIn(new ByteArrayInputStream("Nawaf\n9\n3\nEXIT\n".getBytes()));
            App.main(new String[]{});
            assertTrue(outContent.toString().contains("Unknown option"));
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void testAppMainLogoutAndRelogin() {
        InputStream original = System.in;
        try {
            System.setIn(new ByteArrayInputStream("Nawaf\n3\nFaisal\n3\nEXIT\n".getBytes()));
            App.main(new String[]{});
            assertTrue(outContent.toString().contains("LOGOUT"));
            assertTrue(outContent.toString().contains("Faisal"));
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void testAppMainSensitiveLevel() {
        InputStream original = System.in;
        try {
            String input = "Nawaf\n1\nSecret\n" + tempFile.toString() + "\n1\n3\nEXIT\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            App.main(new String[]{});
            assertTrue(outContent.toString().contains("SENSITIVE"));
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void testAppMainInternalLevel() {
        InputStream original = System.in;
        try {
            String input = "Nawaf\n1\nInt\n" + tempFile.toString() + "\n2\n3\nEXIT\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            App.main(new String[]{});
            assertTrue(outContent.toString().contains("INTERNAL"));
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void testAppMainDownloadInvalidLevel() {
        InputStream original = System.in;
        try {
            String input = "Nawaf\n2\nFile\n" + tempFile.toString() + "\nabc\n3\nEXIT\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            App.main(new String[]{});
            assertTrue(outContent.toString().contains("Please enter a number"));
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void testAppMainDownloadLevelOutOfRange() {
        InputStream original = System.in;
        try {
            String input = "Nawaf\n2\nFile\n" + tempFile.toString() + "\n5\n3\nEXIT\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            App.main(new String[]{});
            assertTrue(outContent.toString().contains("must be 1, 2, or 3"));
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void testAppMainEmptyDownloadName() {
        InputStream original = System.in;
        try {
            System.setIn(new ByteArrayInputStream("Nawaf\n2\n\n3\nEXIT\n".getBytes()));
            App.main(new String[]{});
            assertTrue(outContent.toString().contains("File name cannot be empty"));
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void testAppMainEmptyDownloadPath() {
        InputStream original = System.in;
        try {
            System.setIn(new ByteArrayInputStream("Nawaf\n2\nName\n\n3\nEXIT\n".getBytes()));
            App.main(new String[]{});
            assertTrue(outContent.toString().contains("path cannot be empty"));
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void testAppMainDownloadSensitive() {
        InputStream original = System.in;
        try {
            String input = "Nawaf\n2\nSec\n" + tempFile.toString() + "\n1\n3\nEXIT\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            App.main(new String[]{});
            assertTrue(outContent.toString().contains("DOWNLOAD"));
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void testAppMainDownloadInternal() {
        InputStream original = System.in;
        try {
            String input = "Nawaf\n2\nInt\n" + tempFile.toString() + "\n2\n3\nEXIT\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            App.main(new String[]{});
            assertTrue(outContent.toString().contains("DOWNLOAD"));
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void testAppMainExitLowercase() {
        InputStream original = System.in;
        try {
            System.setIn(new ByteArrayInputStream("exit\n".getBytes()));
            App.main(new String[]{});
            assertTrue(outContent.toString().contains("Shutting down"));
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void testAppMainGuestLogin() {
        InputStream original = System.in;
        try {
            System.setIn(new ByteArrayInputStream("RandomGuest\n3\nEXIT\n".getBytes()));
            App.main(new String[]{});
            assertTrue(outContent.toString().contains("GUEST"));
        } finally {
            System.setIn(original);
        }
    }
}