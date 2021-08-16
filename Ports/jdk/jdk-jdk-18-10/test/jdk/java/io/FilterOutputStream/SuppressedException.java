/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/*
 * @test
 * @bug 8042377
 * @summary Ensure suppressed exceptions are properly handled in close()
 */
public class SuppressedException {
    private static final String CLOSE_MESSAGE = "Close exception";
    private static final String FLUSH_MESSAGE = "Flush exception";
    private static final String SAME_MESSAGE = "Same exception";

    public static void main(String[] args) throws java.io.IOException {
        SuppressedException test = new SuppressedException();
        test.test();
    }

    private FilterOutputStream createOutputStream(OutputStream out,
        boolean isBuffered) {
        return isBuffered ? new BufferedOutputStream(out) :
            new FilterOutputStream(out);
    }

    private void test() {
        int failures = 0;
        FilterOutputStream buf;

        boolean[] isBuffered = new boolean[] {false, true};
        for (boolean buffered : isBuffered) {
            System.err.println("\n>>> Buffered: " + buffered + " <<<");
            System.err.flush();

            try {
                buf = createOutputStream(new OutputStreamFailsWithException(),
                        buffered);
                buf.close();
                System.err.println("\nNo IOException thrown for same exception");
                failures++;
            } catch (IOException expected) {
                if (!expected.getMessage().equals(SAME_MESSAGE)) {
                    System.err.println("\nIOException with unexpected message thrown");
                    expected.printStackTrace();
                    failures++;
                }
            } catch (IllegalArgumentException unexpected) {
                System.err.println("\nUnexpected IllegalArgumentException thrown");
                unexpected.printStackTrace();
                failures++;
            }

            try {
                buf = createOutputStream(
                        new OutputStreamFailsWithException(false, false),
                        buffered);
                buf.close();
            } catch (IOException e) {
                System.err.println("\nUnexpected IOException thrown");
                e.printStackTrace();
                failures++;
            }

            try {
                buf = createOutputStream(
                        new OutputStreamFailsWithException(true, false),
                        buffered);
                buf.close();
            } catch (IOException e) {
                if (!e.getMessage().equals(CLOSE_MESSAGE)) {
                    System.err.println("\nIOException with unexpected message thrown");
                    e.printStackTrace();
                    failures++;
                }
            }

            try {
                buf = createOutputStream(
                        new OutputStreamFailsWithException(false, true),
                        buffered);
                buf.close();
            } catch (IOException e) {
                if (!e.getMessage().equals(FLUSH_MESSAGE)) {
                    System.err.println("\nIOException with unexpected message thrown");
                    e.printStackTrace();
                    failures++;
                }
            }

            try {
                buf = createOutputStream(
                        new OutputStreamFailsWithException(true, true),
                        buffered);
                buf.close();
            } catch (IOException e) {
                if (!e.getMessage().equals(CLOSE_MESSAGE)) {
                    System.err.println("\nIOException with unexpected message thrown");
                    e.printStackTrace();
                    failures++;
                }

                Throwable[] suppressed = e.getSuppressed();
                if (suppressed == null) {
                    System.err.println("\nExpected suppressed exception not present");
                    e.printStackTrace();
                    failures++;
                } else if (suppressed.length != 1) {
                    System.err.println("\nUnexpected number of suppressed exceptions");
                    e.printStackTrace();
                    failures++;
                } else if (!(suppressed[0] instanceof IOException)) {
                    System.err.println("\nSuppressed exception is not an IOException");
                    e.printStackTrace();
                    failures++;
                } else if (!suppressed[0].getMessage().equals(FLUSH_MESSAGE)) {
                    System.err.println("\nIOException with unexpected message thrown");
                    e.printStackTrace();
                    failures++;
                }
            }
        }

        if (failures > 0) {
            throw new RuntimeException("Test failed with " + failures + " errors");
        } else {
            System.out.println("Test succeeded.");
        }
    }

    class OutputStreamFailsWithException extends OutputStream {
        private final IOException sameException = new IOException(SAME_MESSAGE);

        private final Boolean throwSeparateCloseException;
        private final Boolean throwSeparateFlushException;

        OutputStreamFailsWithException() {
            throwSeparateCloseException = null;
            throwSeparateFlushException = null;
        }

        OutputStreamFailsWithException(boolean throwCloseException,
                boolean throwFlushException) {
            throwSeparateCloseException = throwCloseException;
            throwSeparateFlushException = throwFlushException;
        }

        @Override
        public void write(int i) throws IOException {
            throw new UnsupportedOperationException("");
        }

        @Override
        public void flush() throws IOException {
            System.out.println("flush()");
            if (throwSeparateFlushException == null) {
                throw sameException;
            } else if (throwSeparateFlushException) {
                throw new IOException(FLUSH_MESSAGE);
            }
        }

        @Override
        public void close() throws IOException {
            System.out.println("close()");
            if (throwSeparateCloseException == null) {
                throw sameException;
            } else if (throwSeparateCloseException) {
                throw new IOException(CLOSE_MESSAGE);
            }
        }
    }
}
