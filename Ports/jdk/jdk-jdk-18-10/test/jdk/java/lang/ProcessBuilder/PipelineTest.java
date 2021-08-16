/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

/*
 * @test PipelineTest
 * @bug 8211844
 * @summary Tests for ProcessBuilder.startPipeline
 */

public class PipelineTest {

    private static void realMain(String[] args) throws Throwable {
        t1_simplePipeline();
        t2_translatePipeline();
        t3_redirectErrorStream();
        t4_failStartPipeline();
    }

    /**
     * Return a list of the varargs arguments.
     * @param args elements to include in the list
     * @param <T> the type of the elements
     * @return a {@code List<T>} of the arguments
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    static <T> List<T> asList(T... args) {
        return Arrays.asList(args);
    }

    /**
     * T1 - simple copy between two processes
     */
    static void t1_simplePipeline() {
        try {
            String s1 = "Now is the time to check!";
            verify(s1, s1,
                    asList(new ProcessBuilder("cat")));
            verify(s1, s1,
                    asList(new ProcessBuilder("cat"),
                            new ProcessBuilder("cat")));
            verify(s1, s1,
                    asList(new ProcessBuilder("cat"),
                            new ProcessBuilder("cat"),
                            new ProcessBuilder("cat")));
        } catch (Throwable t) {
            unexpected(t);
        }
    }

    /**
     * Pipeline that modifies the content.
     */
    static void t2_translatePipeline() {
        try {
            String s2 = "Now is the time to check!";
            String r2 = s2.replace('e', 'E').replace('o', 'O');
            verify(s2, r2,
                    asList(new ProcessBuilder("tr", "e", "E"),
                            new ProcessBuilder("tr", "o", "O")));
        } catch (Throwable t) {
            unexpected(t);
        }
    }

    /**
     * Test that redirectErrorStream sends standard error of the first process
     * to the standard output. The standard error of the first process should be empty.
     * The standard output of the 2nd should contain the error message including the bad file name.
     */
    static void t3_redirectErrorStream() {
        try {
            File p1err = new File("p1-test.err");
            File p2out = new File("p2-test.out");

            List<Process> processes = ProcessBuilder.startPipeline(
                    asList(new ProcessBuilder("cat", "NON-EXISTENT-FILE")
                                    .redirectErrorStream(true)
                                    .redirectError(p1err),
                            new ProcessBuilder("cat").redirectOutput(p2out)));
            waitForAll(processes);

            check("".equals(fileContents(p1err)), "The first process standard error should be empty");
            String p2contents = fileContents(p2out);
            check(p2contents.contains("NON-EXISTENT-FILE"),
                    "The error from the first process should be in the output of the second: " + p2contents);
        } catch (Throwable t) {
            unexpected(t);
        }
    }

    /**
     * Test that no processes are left after a failed startPipeline.
     * Test illegal combinations of redirects.
     */
    static void t4_failStartPipeline() {
        File p1err = new File("p1-test.err");
        File p2out = new File("p2-test.out");

        THROWS(IllegalArgumentException.class,
                () -> {
                    // Test that output redirect != PIPE throws IAE
                    List<Process> processes = ProcessBuilder.startPipeline(
                            asList(new ProcessBuilder("cat", "NON-EXISTENT-FILE1")
                                            .redirectOutput(p1err),
                                    new ProcessBuilder("cat")));
                },
                () -> {
                    // Test that input redirect != PIPE throws IAE
                    List<Process> processes = ProcessBuilder.startPipeline(
                            asList(new ProcessBuilder("cat", "NON-EXISTENT-FILE2"),
                                    new ProcessBuilder("cat").redirectInput(p2out)));
                }
        );

        THROWS(NullPointerException.class,
                () -> {
                    List<Process> processes = ProcessBuilder.startPipeline(
                            asList(new ProcessBuilder("cat", "a"), null));
                },
                () -> {
                    List<Process> processes = ProcessBuilder.startPipeline(
                            asList(null, new ProcessBuilder("cat", "b")));
                }
        );

        THROWS(IOException.class,
                () -> {
                    List<Process> processes = ProcessBuilder.startPipeline(
                            asList(new ProcessBuilder("cat", "c"),
                                    new ProcessBuilder("NON-EXISTENT-COMMAND")));
                });

        // Check no subprocess are left behind
        ProcessHandle.current().children().forEach(PipelineTest::print);
        ProcessHandle.current().children()
                .filter(p -> p.info().command().orElse("").contains("cat"))
                .forEach(p -> fail("process should have been destroyed: " + p));
    }

    static void verify(String input, String expected, List<ProcessBuilder> builders) throws IOException {
        File infile = new File("test.in");
        File outfile = new File("test.out");
        setFileContents(infile, input);
        for (int i = 0; i < builders.size(); i++) {
            ProcessBuilder b = builders.get(i);
            if (i == 0) {
                b.redirectInput(infile);
            }
            if (i == builders.size() - 1) {
                b.redirectOutput(outfile);
            }
        }
        List<Process> processes = ProcessBuilder.startPipeline(builders);
        verifyProcesses(processes);
        waitForAll(processes);
        String result = fileContents(outfile);
        check(result.equals(expected),
                "result not as expected: " + result + ", expected: " + expected);
    }

    /**
     * Wait for each of the processes to be done.
     *
     * @param processes the list  of processes to check
     */
    static void waitForAll(List<Process> processes) {
        processes.forEach(p -> {
            try {
                int status = p.waitFor();
            } catch (InterruptedException ie) {
                unexpected(ie);
            }
        });
    }

    static void print(ProcessBuilder pb) {
        if (pb != null) {
            System.out.printf(" pb: %s%n", pb);
            System.out.printf("    cmd: %s%n", pb.command());
        }
    }

    static void print(ProcessHandle p) {
        System.out.printf("process: pid: %d, info: %s%n",
                p.pid(), p.info());
    }

    // Check various aspects of the processes
    static void verifyProcesses(List<Process> processes) {
        for (int i = 0; i < processes.size(); i++) {
            Process p = processes.get(i);

            if (i != 0) {
                verifyNullStream(p.getOutputStream(), "getOutputStream");
            }
            if (i <= processes.size() - 1) {
                verifyNullStream(p.getInputStream(), "getInputStream");
            }
            if (i == processes.size() - 1) {
                verifyNullStream(p.getErrorStream(), "getErrorStream");
            }
        }
    }

    static void verifyNullStream(OutputStream s, String msg) {
        try {
            s.write(0xff);
            fail("Stream should have been a NullStream: " + msg);
        } catch (IOException ie) {
            // expected
        }
    }

    static void verifyNullStream(InputStream s, String msg) {
        try {
            int len = s.read();
            check(len == -1, "Stream should have been a NullStream: " + msg);
        } catch (IOException ie) {
            // expected
        }
    }

    static void setFileContents(File file, String contents) {
        try {
            Writer w = new FileWriter(file);
            w.write(contents);
            w.close();
        } catch (Throwable t) { unexpected(t); }
    }

    static String fileContents(File file) {
        try {
            Reader r = new FileReader(file);
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int n;
            while ((n = r.read(buffer)) != -1)
                sb.append(buffer,0,n);
            r.close();
            return new String(sb);
        } catch (Throwable t) { unexpected(t); return ""; }
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; new Exception("Stack trace").printStackTrace(System.out);}
    static void fail(String msg) {
        System.out.println(msg); failed++;
        new Exception("Stack trace: " + msg).printStackTrace(System.out);
    }
    static void unexpected(Throwable t) {failed++; t.printStackTrace(System.out);}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void check(boolean cond, String m) {if (cond) pass(); else fail(m);}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(">'" + x + "'<" + " not equal to " + "'" + y + "'");
    }

    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");
    }
    interface Fun {void f() throws Throwable;}
    static void THROWS(Class<? extends Throwable> k, Fun... fs) {
        for (Fun f : fs)
            try { f.f(); fail("Expected " + k.getName() + " not thrown"); }
            catch (Throwable t) {
                if (k.isAssignableFrom(t.getClass())) pass();
                else unexpected(t);}
    }

}
