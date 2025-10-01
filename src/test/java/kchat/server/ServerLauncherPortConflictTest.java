package kchat.server;

import kchat.ServerLauncher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.*;

public class ServerLauncherPortConflictTest {

    private ServerSocket occupyingSocket;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;

    @BeforeEach
    void setUp() throws Exception {
        occupyingSocket = new ServerSocket(0);
        occupyingSocket.setReuseAddress(true);
        originalOut = System.out;
        originalErr = System.err;
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (occupyingSocket != null && !occupyingSocket.isClosed()) {
            occupyingSocket.close();
        }
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void testServerLauncherDeclinesWhenPortInUse() {
        int portInUse = occupyingSocket.getLocalPort();
        ServerLauncher.main(new String[]{String.valueOf(portInUse)});
        String combined = outContent.toString() + errContent.toString();
        assertTrue(combined.contains("Aborting new server start"), () -> "Expected abort message not found. Output was:\n" + combined);
        assertTrue(combined.contains("port " + portInUse), () -> "Expected port number mention not found. Output was:\n" + combined);
        assertFalse(occupyingSocket.isClosed());
    }
}
