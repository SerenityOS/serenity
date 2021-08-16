/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8155808
 * @run main SkipTest
 * @summary verify skip method of Process Input Stream
 */

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SkipTest {
    private static final String javaExe =
            System.getProperty("java.home") +
                    File.separator + "bin" + File.separator + "java";

    private static final String classpath =
            System.getProperty("java.class.path");

    static final int BLOCK_SIZE = 10000;


    public static void main(String[] args) throws Throwable {

        // Start a Process to generate the test data to stdout and stderr
        ProcessBuilder pb = new ProcessBuilder(javaExe, "-classpath", classpath, "SkipTest$GenerateData");
        System.out.printf("cmd: %s%n", pb.command());
        Process process = pb.start();

        /*
         * Verify the data expected is received mixing reads and skip.
         */
        try (InputStream in = process.getInputStream()) {

            // Note: Process.getInputStream() actually returns a BufferedInputStream whose
            // skip() method works perfectly, partially obscuring the bug. Only when the
            // BufferedInputStream's buffer is drained, and it passes the skip() call to
            // the underlying native stream, does the problem occur.

            long n = in.skip(-1);
            if (n != 0) {
                throw new AssertionError("skip(-1) should return 0");
            }
            n = in.skip(0);
            if (n != 0) {
                throw new AssertionError("skip(0) should return 0");
            }

            // Now iterate all the data blocks
            int header;
            for (int expectedHeader = 'A'; (header = in.read()) != -1; expectedHeader++) {
                // The header byte should be simple 'A' to 'Z'.
                // When the bug hits, we will get lowercase letters instead.
                if (header != expectedHeader) {
                    throw new AssertionError("header char wrong, expected: " +
                            expectedHeader + ", actual: " + header);
                }

                // Handle the data bytes.
                // If the correct number of bytes are not skipped,
                // then subsequent reads become out-of-sync;
                int remaining = BLOCK_SIZE;
                do {
                    remaining -= in.skip(remaining);
                } while (remaining != 0);
            }
            n = in.skip(1);
            if (n != 0) {
                throw new AssertionError("skip(1) at eof should return 0");
            }
        }

        /**
         * Do the same for the standard error stream.
         */
        try (InputStream in = process.getErrorStream()) {
            long n = in.skip(-1);
            if (n != 0) {
                throw new AssertionError("skip(-1) should return 0");
            }
            n = in.skip(0);
            if (n != 0) {
                throw new AssertionError("skip(0) should return 0");
            }

            // Now iterate all the data blocks
            int header;
            for (int expectedHeader = 'A'; (header = in.read()) != -1; expectedHeader++) {
                // The header byte should be simple 'A' to 'Z'.
                // When the bug hits, we will get lowercase letters instead.
                if (header != expectedHeader) {
                    throw new AssertionError("header char wrong, expected: " +
                            expectedHeader + ", actual: " + header);
                }

                // Handle the data bytes.
                // If the correct number of bytes are not skipped,
                // then subsequent reads become out-of-sync;
                int remaining = BLOCK_SIZE;
                do {
                    remaining -= in.skip(remaining);
                } while (remaining != 0);
            }
            n = in.skip(1);
            if (n != 0) {
                throw new AssertionError("skip(1) at eof should return 0");
            }
        }
    }

    /**
     * Alternate main to generate the test data to standard output
     * and standard error.
     */
    static class GenerateData {

        public static void main(String[] args) {
            // Generate test data containing a series of data blocks of length BLOCK_SIZE,
            // each with a one-byte header. For example's sake, the "header" is a capital letter,
            // and the "data" is the lowercase version of that letter repeated BLOCK_SIZE times:
            try (OutputStream out = new BufferedOutputStream(System.out)) {
                for (int header = 'A'; header <= 'Z'; header++) {
                    out.write(header);

                    int data = Character.toLowerCase(header);
                    for (int i = 0; i < BLOCK_SIZE; i++) {
                        out.write(data);
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.exit(1);
            }
            // Generate the same data to the error output
            try (OutputStream err = new BufferedOutputStream(System.err)) {
                for (int header = 'A'; header <= 'Z'; header++) {
                    err.write(header);

                    int data = Character.toLowerCase(header);
                    for (int i = 0; i < BLOCK_SIZE; i++) {
                        err.write(data);
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.exit(1);
            }
        }
    }
}
