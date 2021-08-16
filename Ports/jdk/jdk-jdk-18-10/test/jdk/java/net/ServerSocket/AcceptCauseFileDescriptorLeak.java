/*
 * Copyright (c) 2006, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * author Edward Wang
 *
 * @test
 * @bug 6368984
 * @key intermittent
 * @summary Configuring unconnected Socket before passing to implAccept
 *          can cause fd leak.
 *          This test may fail intermittently if foreign processes will
 *          try to establish connection to the test server socket.
 * @requires (os.family != "windows")
 * @library /test/lib
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 *        AcceptCauseFileDescriptorLeak
 * @run main/othervm AcceptCauseFileDescriptorLeak root
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.OutputAnalyzer;

public class AcceptCauseFileDescriptorLeak {
    private static final int REPS = 2048;
    private static final int THRESHOLD = 1024;

    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            OutputAnalyzer analyzer = execCmd("ulimit -n -H");
            String output = analyzer.getOutput();
            if (output == null || output.length() == 0) {
                throw new RuntimeException("\"ulimit -n -H\" output nothing"
                        + " and its exit code is " + analyzer.getExitValue());
            } else {
                output = output.trim();
                // Set max open file descriptors to 1024
                // if it is unlimited or greater than 1024,
                // otherwise just do test directly
                if ("unlimited".equals(output)
                        || Integer.valueOf(output) > THRESHOLD) {
                    analyzer = execCmd("ulimit -n " + THRESHOLD + "; "
                            + composeJavaTestStr());
                    System.out.println("Output: ["
                            + analyzer.getOutput() + "]");
                    int rc = analyzer.getExitValue();
                    if (rc != 0) {
                        throw new RuntimeException(
                                "Unexpected exit code: " + rc);
                    }
                    return;
                }
            }
        }

        final ServerSocket ss = new ServerSocket() {
            public Socket accept() throws IOException {
                Socket s = new Socket() {
                };
                s.setSoTimeout(10000);
                implAccept(s);
                return s;
            }
        };
        ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        Thread t = new Thread(new Runnable() {
            public void run() {
                int repsCompleted = 0;
                try {
                    for (; repsCompleted < REPS; repsCompleted++) {
                        (new Socket(InetAddress.getLoopbackAddress(), ss.getLocalPort())).close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("Client iterations completed:" + repsCompleted);
                }
            }
        });
        t.start();
        int repsCompleted = 0;
        try {
            for (; repsCompleted < REPS; repsCompleted++) {
                ss.accept().close();
            }
        } finally {
            System.out.println("Server iterations completed:" + repsCompleted);
            ss.close();
        }
        t.join();
    }

    /**
     * Execute command with shell
     *
     * @param  command
     * @return OutputAnalyzer
     * @throws IOException
     */
    static OutputAnalyzer execCmd(String command) throws IOException {
        List<String> cmd = List.of("sh", "-c", command);
        System.out.println("Executing: " + cmd);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        return new OutputAnalyzer(pb.start());
    }

    static String composeJavaTestStr() {
        return JDKToolFinder.getTestJDKTool("java") + " "
                + AcceptCauseFileDescriptorLeak.class.getName();
    }
}

