/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @summary Testing external editor.
 * @bug 8143955 8080843 8163816 8143006 8169828 8171130 8162989 8210808
 * @modules jdk.jshell/jdk.internal.jshell.tool
 * @build ReplToolTesting CustomEditor EditorTestBase
 * @run testng ExternalEditorTest
 * @key intermittent
 */

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class ExternalEditorTest extends EditorTestBase {

    private static Path executionScript;
    private static ServerSocket listener;

    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    @Override
    public void writeSource(String s) {
        try {
            outputStream.writeInt(CustomEditor.SOURCE_CODE);
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String getSource() {
        return readString(CustomEditor.GET_SOURCE_CODE);
    }

    private void sendCode(int code) {
        try {
            outputStream.writeInt(code);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void accept() {
        sendCode(CustomEditor.ACCEPT_CODE);
    }

    @Override
    public void exit() {
        sendCode(CustomEditor.EXIT_CODE);
        inputStream = null;
        outputStream = null;
    }

    @Override
    public void cancel() {
        sendCode(CustomEditor.CANCEL_CODE);
    }

    protected String getFilename() {
        return readString(CustomEditor.GET_FILENAME);
    }

    private String readString(int code) {
        try {
            outputStream.writeInt(code);
            int length = inputStream.readInt();
            byte[] bytes = new byte[length];
            inputStream.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void testEditor(boolean defaultStartup, String[] args, ReplTest... tests) {
        ReplTest[] t = new ReplTest[tests.length + 1];
        t[0] = a -> assertCommandCheckOutput(a, "/set editor " + executionScript,
                assertStartsWith("|  Editor set to: " + executionScript));
        System.arraycopy(tests, 0, t, 1, tests.length);
        super.testEditor(defaultStartup, args, t);
    }

    @Test
    public void testStatementSemicolonAddition() {
        testEditor(
                a -> assertCommand(a, "if (true) {}", ""),
                a -> assertCommand(a, "if (true) {} else {}", ""),
                a -> assertCommand(a, "Object o", "o ==> null"),
                a -> assertCommand(a, "if (true) o = new Object() { int x; }", ""),
                a -> assertCommand(a, "if (true) o = new Object() { int y; }", ""),
                a -> assertCommand(a, "System.err.flush()", ""), // test still ; for expression statement
                a -> assertEditOutput(a, "/ed", "", () -> {
                    assertEquals(getSource(),
                            "if (true) {}\n" +
                            "if (true) {} else {}\n" +
                            "Object o;\n" +
                            "if (true) o = new Object() { int x; };\n" +
                            "if (true) o = new Object() { int y; };\n" +
                            "System.err.flush();\n");
                    exit();
                })
        );
    }

    @Test
    public void testTempFileDeleted() {
        String[] fna = new String[1];
        testEditor(
                a -> assertVariable(a, "int", "a", "0", "0"),
                a -> assertEditOutput(a, "/ed 1", "a ==> 10", () -> {
                    fna[0] = getFilename();
                    assertTrue(Files.exists(Paths.get(fna[0])), "Test set-up failed: " + fna[0]);
                    writeSource("\n\n\nint a = 10;\n\n\n");
                    exit();
                }),
               a -> assertCommand(a, "if (true) {} else {}", "")
        );
        assertFalse(Files.exists(Paths.get(fna[0])), "File not deleted: " + fna[0]);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    @BeforeClass
    public static void setUpExternalEditorTest() throws IOException {
        listener = new ServerSocket(0);
        listener.setSoTimeout(30000);
        int localPort = listener.getLocalPort();

        executionScript = Paths.get(isWindows() ? "editor.bat" : "editor.sh").toAbsolutePath();
        Path java = Paths.get(System.getProperty("java.home")).resolve("bin").resolve("java");
        try (BufferedWriter writer = Files.newBufferedWriter(executionScript)) {
            if(!isWindows()) {
                writer.append(java.toString()).append(" ")
                        .append(" -cp ").append(System.getProperty("java.class.path"))
                        .append(" CustomEditor ").append(Integer.toString(localPort)).append(" $@");
                executionScript.toFile().setExecutable(true);
            } else {
                writer.append(java.toString()).append(" ")
                        .append(" -cp ").append(System.getProperty("java.class.path"))
                        .append(" CustomEditor ").append(Integer.toString(localPort)).append(" %*");
            }
        }
    }

    private Future<?> task;
    @Override
    void assertEdit(boolean after, String cmd,
                           Consumer<String> checkInput, Consumer<String> checkOutput, Action action) {
        if (!after) {
            setCommandInput(cmd + "\n");
            task = getExecutor().submit(() -> {
                try (Socket socket = listener.accept()) {
                    inputStream = new DataInputStream(socket.getInputStream());
                    outputStream = new DataOutputStream(socket.getOutputStream());
                    checkInput.accept(getSource());
                    action.accept();
                } catch (SocketTimeoutException e) {
                    fail("Socket timeout exception.\n Output: " + getCommandOutput() +
                            "\n, error: " + getCommandErrorOutput());
                } catch (Throwable e) {
                    shutdownEditor();
                    if (e instanceof AssertionError) {
                        throw (AssertionError) e;
                    }
                    throw new RuntimeException(e);
                }
            });
        } else {
            try {
                task.get();
                checkOutput.accept(getCommandOutput());
            } catch (ExecutionException e) {
                if (e.getCause() instanceof AssertionError) {
                    throw (AssertionError) e.getCause();
                }
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void shutdownEditor() {
        if (outputStream != null) {
            exit();
        }
    }

    @Test
    public void setUnknownEditor() {
        test(
                a -> assertCommand(a, "/set editor UNKNOWN", "|  Editor set to: UNKNOWN"),
                a -> assertCommand(a, "int a;", null),
                a -> assertCommandOutputStartsWith(a, "/ed 1",
                        "|  Edit Error:")
        );
    }

    @Test(enabled = false) // TODO 8159229
    public void testRemoveTempFile() {
        test(new String[]{"--no-startup"},
                a -> assertCommandCheckOutput(a, "/set editor " + executionScript,
                        assertStartsWith("|  Editor set to: " + executionScript)),
                a -> assertVariable(a, "int", "a", "0", "0"),
                a -> assertEditOutput(a, "/ed 1", assertStartsWith("|  Edit Error: Failure in read edit file:"), () -> {
                    sendCode(CustomEditor.REMOVE_CODE);
                    exit();
                }),
                a -> assertCommandCheckOutput(a, "/vars", assertVariables())
        );
    }

    @AfterClass
    public static void shutdown() throws IOException {
        executorShutdown();
        if (listener != null) {
            listener.close();
        }
    }
}
