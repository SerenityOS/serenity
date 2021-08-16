/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.Arrays;
import java.util.Random;

import jdk.test.lib.RandomFactory;

import static java.lang.String.format;

/*
 * @test
 * @bug 8191706
 * @summary tests whether java.io.Reader.transferTo conforms to its
 *          contract defined source the javadoc
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main TransferTo
 * @key randomness
 * @author Patrick Reinhart
 */
public class TransferTo {

    private static Random generator = RandomFactory.getRandom();

    public static void main(String[] args) throws IOException {
        ifOutIsNullThenNpeIsThrown();
        ifExceptionInInputNeitherStreamIsClosed();
        ifExceptionInOutputNeitherStreamIsClosed();
        onReturnNeitherStreamIsClosed();
        onReturnInputIsAtEnd();
        contents();
    }

    private static void ifOutIsNullThenNpeIsThrown() throws IOException {
        try (Reader in = input()) {
            assertThrowsNPE(() -> in.transferTo(null), "out");
        }

        try (Reader in = input((char) 1)) {
            assertThrowsNPE(() -> in.transferTo(null), "out");
        }

        try (Reader in = input((char) 1, (char) 2)) {
            assertThrowsNPE(() -> in.transferTo(null), "out");
        }

        Reader in = null;
        try {
            Reader fin = in = new ThrowingReader();
            // null check should precede everything else:
            // Reader shouldn't be touched if Writer is null
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
        transferToThenCheckIfAnyClosed(input(0, new char[]{1, 2, 3}), output());
        transferToThenCheckIfAnyClosed(input(1, new char[]{1, 2, 3}), output());
        transferToThenCheckIfAnyClosed(input(2, new char[]{1, 2, 3}), output());
    }

    private static void ifExceptionInOutputNeitherStreamIsClosed()
            throws IOException {
        transferToThenCheckIfAnyClosed(input(new char[]{1, 2, 3}), output(0));
        transferToThenCheckIfAnyClosed(input(new char[]{1, 2, 3}), output(1));
        transferToThenCheckIfAnyClosed(input(new char[]{1, 2, 3}), output(2));
    }

    private static void transferToThenCheckIfAnyClosed(Reader input,
                                                       Writer output)
            throws IOException {
        try (CloseLoggingReader in = new CloseLoggingReader(input);
             CloseLoggingWriter out =
                     new CloseLoggingWriter(output)) {
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
        try (CloseLoggingReader in =
                     new CloseLoggingReader(input(new char[]{1, 2, 3}));
             CloseLoggingWriter out =
                     new CloseLoggingWriter(output())) {

            in.transferTo(out);

            if (in.wasClosed() || out.wasClosed()) {
                throw new AssertionError();
            }
        }
    }

    private static void onReturnInputIsAtEnd() throws IOException {
        try (Reader in = input(new char[]{1, 2, 3});
             Writer out = output()) {

            in.transferTo(out);

            if (in.read() != -1) {
                throw new AssertionError();
            }
        }
    }

    private static void contents() throws IOException {
        checkTransferredContents(new char[0]);
        checkTransferredContents(createRandomChars(1024, 4096));
        // to span through several batches
        checkTransferredContents(createRandomChars(16384, 16384));
    }

    private static void checkTransferredContents(char[] chars)
            throws IOException {
        try (Reader in = input(chars);
             StringWriter out = new StringWriter()) {
            in.transferTo(out);

            char[] outChars = out.toString().toCharArray();
            if (!Arrays.equals(chars, outChars)) {
                throw new AssertionError(
                        format("chars.length=%s, outChars.length=%s",
                                chars.length, outChars.length));
            }
        }
    }

    private static char[] createRandomChars(int min, int maxRandomAdditive) {
        char[] chars = new char[min + generator.nextInt(maxRandomAdditive)];
        for (int index=0; index<chars.length; index++) {
            chars[index] = (char)generator.nextInt();
        }
        return chars;
    }

    private static Writer output() {
        return output(-1);
    }

    private static Writer output(int exceptionPosition) {
        return new Writer() {

            int pos;

            @Override
            public void write(int b) throws IOException {
                if (pos++ == exceptionPosition)
                    throw new IOException();
            }

            @Override
            public void write(char[] chars, int off, int len) throws IOException {
                for (int i=0; i<len; i++) {
                    write(chars[off + i]);
                }
            }

            @Override
            public Writer append(CharSequence csq, int start, int end) throws IOException {
                for (int i = start; i < end; i++) {
                    write(csq.charAt(i));
                }
                return this;
            }

            @Override
            public void flush() throws IOException {
            }

            @Override
            public void close() throws IOException {
            }
        };
    }

    private static Reader input(char... chars) {
        return input(-1, chars);
    }

    private static Reader input(int exceptionPosition, char... chars) {
        return new Reader() {

            int pos;

            @Override
            public int read() throws IOException {
                if (pos == exceptionPosition) {
                    throw new IOException();
                }

                if (pos >= chars.length)
                    return -1;
                return chars[pos++];
            }

            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                int c = read();
                if (c == -1) {
                    return -1;
                }
                cbuf[off] = (char)c;

                int i = 1;
                for (; i < len ; i++) {
                    c = read();
                    if (c == -1) {
                        break;
                    }
                    cbuf[off + i] = (char)c;
                }
                return i;
            }

            @Override
            public void close() throws IOException {
            }
        };
    }

    private static class ThrowingReader extends Reader {

        boolean closed;

        @Override
        public int read(char[] b, int off, int len) throws IOException {
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
        public int read() throws IOException {
            throw new IOException();
        }
    }

    private static class CloseLoggingReader extends FilterReader {

        boolean closed;

        CloseLoggingReader(Reader in) {
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

    private static class CloseLoggingWriter extends FilterWriter {

        boolean closed;

        CloseLoggingWriter(Writer out) {
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
