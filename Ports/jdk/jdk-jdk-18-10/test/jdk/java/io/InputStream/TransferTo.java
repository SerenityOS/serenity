/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Random;

import static java.lang.String.format;

/*
 * @test
 * @bug 8066867
 * @summary tests whether java.io.InputStream.transferTo conforms to its
 *          contract defined in the javadoc
 * @key randomness
 */
public class TransferTo {

    public static void main(String[] args) throws IOException {
        ifOutIsNullThenNpeIsThrown();
        ifExceptionInInputNeitherStreamIsClosed();
        ifExceptionInOutputNeitherStreamIsClosed();
        onReturnNeitherStreamIsClosed();
        onReturnInputIsAtEnd();
        contents();
    }

    private static void ifOutIsNullThenNpeIsThrown() throws IOException {
        try (InputStream in = input()) {
            assertThrowsNPE(() -> in.transferTo(null), "out");
        }

        try (InputStream in = input((byte) 1)) {
            assertThrowsNPE(() -> in.transferTo(null), "out");
        }

        try (InputStream in = input((byte) 1, (byte) 2)) {
            assertThrowsNPE(() -> in.transferTo(null), "out");
        }

        InputStream in = null;
        try {
            InputStream fin = in = new ThrowingInputStream();
            // null check should precede everything else:
            // InputStream shouldn't be touched if OutputStream is null
            assertThrowsNPE(() -> fin.transferTo(null), "out");
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException ignored) { }
        }
    }

    private static void ifExceptionInInputNeitherStreamIsClosed()
            throws IOException {
        transferToThenCheckIfAnyClosed(input(0, new byte[]{1, 2, 3}), output());
        transferToThenCheckIfAnyClosed(input(1, new byte[]{1, 2, 3}), output());
        transferToThenCheckIfAnyClosed(input(2, new byte[]{1, 2, 3}), output());
    }

    private static void ifExceptionInOutputNeitherStreamIsClosed()
            throws IOException {
        transferToThenCheckIfAnyClosed(input(new byte[]{1, 2, 3}), output(0));
        transferToThenCheckIfAnyClosed(input(new byte[]{1, 2, 3}), output(1));
        transferToThenCheckIfAnyClosed(input(new byte[]{1, 2, 3}), output(2));
    }

    private static void transferToThenCheckIfAnyClosed(InputStream input,
                                                       OutputStream output)
            throws IOException {
        try (CloseLoggingInputStream in = new CloseLoggingInputStream(input);
             CloseLoggingOutputStream out =
                     new CloseLoggingOutputStream(output)) {
            boolean thrown = false;
            try {
                in.transferTo(out);
            } catch (IOException ignored) {
                thrown = true;
            }
            if (!thrown)
                throw new AssertionError();

            if (in.wasClosed() || out.wasClosed()) {
                throw new AssertionError();
            }
        }
    }

    private static void onReturnNeitherStreamIsClosed()
            throws IOException {
        try (CloseLoggingInputStream in =
                     new CloseLoggingInputStream(input(new byte[]{1, 2, 3}));
             CloseLoggingOutputStream out =
                     new CloseLoggingOutputStream(output())) {

            in.transferTo(out);

            if (in.wasClosed() || out.wasClosed()) {
                throw new AssertionError();
            }
        }
    }

    private static void onReturnInputIsAtEnd() throws IOException {
        try (InputStream in = input(new byte[]{1, 2, 3});
             OutputStream out = output()) {

            in.transferTo(out);

            if (in.read() != -1) {
                throw new AssertionError();
            }
        }
    }

    private static void contents() throws IOException {
        checkTransferredContents(new byte[0]);
        checkTransferredContents(createRandomBytes(1024, 4096));
        // to span through several batches
        checkTransferredContents(createRandomBytes(16384, 16384));
    }

    private static void checkTransferredContents(byte[] bytes)
            throws IOException {
        try (InputStream in = input(bytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);

            byte[] outBytes = out.toByteArray();
            if (!Arrays.equals(bytes, outBytes)) {
                throw new AssertionError(
                        format("bytes.length=%s, outBytes.length=%s",
                                bytes.length, outBytes.length));
            }
        }
    }

    private static byte[] createRandomBytes(int min, int maxRandomAdditive) {
        Random rnd = new Random();
        byte[] bytes = new byte[min + rnd.nextInt(maxRandomAdditive)];
        rnd.nextBytes(bytes);
        return bytes;
    }

    private static OutputStream output() {
        return output(-1);
    }

    private static OutputStream output(int exceptionPosition) {
        return new OutputStream() {

            int pos;

            @Override
            public void write(int b) throws IOException {
                if (pos++ == exceptionPosition)
                    throw new IOException();
            }
        };
    }

    private static InputStream input(byte... bytes) {
        return input(-1, bytes);
    }

    private static InputStream input(int exceptionPosition, byte... bytes) {
        return new InputStream() {

            int pos;

            @Override
            public int read() throws IOException {
                if (pos == exceptionPosition) {
                    // because of the pesky IOException swallowing in
                    // java.io.InputStream.read(byte[], int, int)
                    // pos++;
                    throw new IOException();
                }

                if (pos >= bytes.length)
                    return -1;
                return bytes[pos++] & 0xff;
            }
        };
    }

    private static class ThrowingInputStream extends InputStream {

        boolean closed;

        @Override
        public int read(byte[] b) throws IOException {
            throw new IOException();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            throw new IOException();
        }

        @Override
        public long skip(long n) throws IOException {
            throw new IOException();
        }

        @Override
        public int available() throws IOException {
            throw new IOException();
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                throw new IOException();
            }
        }

        @Override
        public void reset() throws IOException {
            throw new IOException();
        }

        @Override
        public int read() throws IOException {
            throw new IOException();
        }
    }

    private static class CloseLoggingInputStream extends FilterInputStream {

        boolean closed;

        CloseLoggingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        boolean wasClosed() {
            return closed;
        }
    }

    private static class CloseLoggingOutputStream extends FilterOutputStream {

        boolean closed;

        CloseLoggingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        boolean wasClosed() {
            return closed;
        }
    }

    public interface Thrower {
        public void run() throws Throwable;
    }

    public static void assertThrowsNPE(Thrower thrower, String message) {
        assertThrows(thrower, NullPointerException.class, message);
    }

    public static <T extends Throwable> void assertThrows(Thrower thrower,
                                                          Class<T> throwable,
                                                          String message) {
        Throwable thrown;
        try {
            thrower.run();
            thrown = null;
        } catch (Throwable caught) {
            thrown = caught;
        }

        if (!throwable.isInstance(thrown)) {
            String caught = thrown == null ?
                    "nothing" : thrown.getClass().getCanonicalName();
            throw new AssertionError(
                    format("Expected to catch %s, but caught %s",
                            throwable, caught), thrown);
        }

        if (thrown != null && !message.equals(thrown.getMessage())) {
            throw new AssertionError(
                    format("Expected exception message to be '%s', but it's '%s'",
                            message, thrown.getMessage()));
        }
    }
}
