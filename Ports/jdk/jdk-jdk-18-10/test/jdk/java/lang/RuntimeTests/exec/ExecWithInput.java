/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4763384
 * @summary Ensure that piped input always works with exec'd processes
 */

import java.io.*;


/**
 * This class demonstrates a regression in java1.4.1 in the handling of the
 * Process OutputStream (exec'd process stdin).  The subprocess completes 100%
 * of the time in 1.4, but about only about 50% of the time under 1.4.1.  Issue
 * exists for client JVM, Linux Redhat 6.2 not sure about other variants of
 * Linux or other OSes, or server JVM.
 */

public class ExecWithInput {

    private static final int N = 200;

    static int go(int i) throws Exception {
        /*
         * Execute /bin/cat supplying two lines of input. cat should
         * read the input lines and copy them to stdout. On completion,
         * p.waitFor should return and the exit status is printed and this
         * program exits. Under 1.4.1, cat sometimes gets stuck on a pipe
         * read and never terminates.
         */
        Process p = Runtime.getRuntime().exec(UnixCommands.cat());

        String input = i + ": line 1\n" + i + ": line 2\n";
        StringBufferInputStream in = new StringBufferInputStream(input);
        // create threads to handle I/O streams
        IO ioIn = new IO("stdin", in, p.getOutputStream());
        IO ioOut = new IO("stdout", p.getInputStream(), System.out);
        IO ioErr = new IO("stderr", p.getErrorStream(), System.err);

        // wait for process to exit
        return p.waitFor();
    }

    public static void main(String[] args) throws Exception {
        if (! UnixCommands.isLinux) {
            System.out.println("For Linux only");
            return;
        }
        UnixCommands.ensureCommandsAvailable("cat");

        for (int i = 0; i < N; i++)
            go(i);
    }

    /**
     * Handle IO. Thread is started in constructor.
     */
    static class IO extends Thread {

        private InputStream in;
        private OutputStream out;

        IO(String name, InputStream in, OutputStream out)
        {
            this.in = in;
            this.out = out;
            setName(name);
            start();
        }

        public void run() {
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    out.flush();
                }
                /*
                while ((c = in.read()) != -1) {
                    out.write(c);
                    if (c == '\n')
                        out.flush();
                }
                out.flush();
                */
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (!System.out.equals(out) && !System.err.equals(out)) {
                    // Note: in order to get an exec'd java process to
                    // see EOF on input, it is necessary to close stdin
                    if (out != null) {
                        try { out.close(); } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

}
