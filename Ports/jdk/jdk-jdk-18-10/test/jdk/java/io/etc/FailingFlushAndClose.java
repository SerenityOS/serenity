/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 7015589 8054565
 * @summary Test that buffering streams are considered closed even when the
 *    close or flush from the underlying stream fails.
 */

public class FailingFlushAndClose {

    static int failed;

    static void fail(String msg) {
        System.err.println("FAIL: " + msg);
        failed++;
    }

    static void failWithIOE(String msg) throws IOException {
        fail(msg);
        throw new IOException(msg);
    }

    static class FailingCloseInputStream extends InputStream {
        boolean closed;
        @Override
        public int read()throws IOException {
            if (closed)
                failWithIOE("input stream is closed");
            return 1;
        }
        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                throw new IOException("close failed");
            }
        }
    }

    static class FailingCloseOutputStream extends OutputStream {
        boolean closed;
        @Override
        public void write(int b) throws IOException {
            if (closed)
                failWithIOE("output stream is closed");
        }
        @Override
        public void flush() throws IOException {
            if (closed)
                failWithIOE("output stream is closed");
        }
        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                throw new IOException("close failed");
            }
        }
    }

    static class FailingFlushOutputStream extends OutputStream {
        boolean closed;
        @Override
        public void write(int b) throws IOException {
            if (closed)
                failWithIOE("output stream is closed");
        }
        @Override
        public void flush() throws IOException {
            if (closed) {
                failWithIOE("output stream is closed");
            } else {
                throw new IOException("flush failed");
            }
        }
        @Override
        public void close() throws IOException {
            closed = true;
        }
    }

    static class FailingCloseReader extends Reader {
        boolean closed;
        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            if (closed)
                failWithIOE("reader is closed");
            return 1;
        }
        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                throw new IOException("close failed");
            }
        }
    }

    static class FailingCloseWriter extends Writer {
        boolean closed;
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            if (closed)
                failWithIOE("writer is closed");
        }
        @Override
        public void flush() throws IOException {
            if (closed)
                failWithIOE("writer is closed");
        }
        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                throw new IOException("close failed");
            }
        }
    }

    static class FailingFlushWriter extends Writer {
        boolean closed;
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            if (closed)
                failWithIOE("writer is closed");
        }
        @Override
        public void flush() throws IOException {
            if (closed) {
                failWithIOE("writer is closed");
            } else {
                throw new IOException("flush failed");
            }
        }
        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                throw new IOException("close failed");
            }
        }
    }

    static InputStream testFailingClose(InputStream in) throws IOException {
        System.out.println(in.getClass());
        in.read(new byte[100]);
        try {
            in.close();
            fail("close did not fail");
        } catch (IOException expected) { }
        try {
            in.read(new byte[100]);
            fail("read did not fail");
        } catch (IOException expected) { }
        return in;
    }

    static OutputStream testFailingClose(OutputStream out) throws IOException {
        System.out.println(out.getClass());
        out.write(1);
        try {
            out.close();
            fail("close did not fail");
        } catch (IOException expected) { }
        try {
            out.write(1);
            if (!(out instanceof BufferedOutputStream))
                fail("write did not fail");
        } catch (IOException expected) { }
        return out;
    }

    static OutputStream testFailingFlush(OutputStream out) throws IOException {
        System.out.println(out.getClass());
        out.write(1);
        try {
            out.flush();
            fail("flush did not fail");
        } catch (IOException expected) { }
        if (out instanceof BufferedOutputStream) {
            out.write(1);
            try {
                out.close();
                fail("close did not fail");
            } catch (IOException expected) { }
        }
        return out;
    }

    static void closeAgain(InputStream in) throws IOException {
        // assert the given stream should already be closed.
        try {
            in.close();
        } catch (IOException expected) {
            fail("unexpected IOException from subsequent close");
        }
    }
    static void closeAgain(OutputStream out) throws IOException {
        // assert the given stream should already be closed.
        try {
            out.close();
        } catch (IOException expected) {
            fail("unexpected IOException from subsequent close");
        }
    }

    static Reader testFailingClose(Reader r) throws IOException {
        System.out.println(r.getClass());
        r.read(new char[100]);
        try {
            r.close();
            fail("close did not fail");
        } catch (IOException expected) { }
        try {
            r.read(new char[100]);
            fail("read did not fail");
        } catch (IOException expected) { }
        return r;
    }

    static Writer testFailingClose(Writer w) throws IOException {
        System.out.println(w.getClass());
        w.write("message");
        try {
            w.close();
            fail("close did not fail");
        } catch (IOException expected) { }
        try {
            w.write("another message");
            fail("write did not fail");
        } catch (IOException expected) { }
        return w;
    }

    static Writer testFailingFlush(Writer w) throws IOException {
        System.out.println(w.getClass());
        w.write("message");
        try {
            w.flush();
            fail("flush did not fail");
        } catch (IOException expected) { }
        if (w instanceof BufferedWriter) {
            // assume this message will be buffered
            w.write("another message");
            try {
                w.close();
                fail("close did not fail");
            } catch (IOException expected) { }
        }
        return w;
    }

    static Reader closeAgain(Reader r) throws IOException {
        // assert the given stream should already be closed.
        try {
            r.close();
        } catch (IOException expected) {
            fail("unexpected IOException from subsequent close");
        }
        return r;
    }
    static Writer closeAgain(Writer w) throws IOException {
        // assert the given stream should already be closed.
        try {
            w.close();
        } catch (IOException expected) {
            fail("unexpected IOException from subsequent close");
        }
        return w;
    }

    public static void main(String[] args) throws IOException {

        closeAgain(testFailingClose(new BufferedInputStream(new FailingCloseInputStream())));
        closeAgain(testFailingClose(new BufferedOutputStream(new FailingCloseOutputStream())));

        closeAgain(testFailingClose(new BufferedReader(new FailingCloseReader())));
        closeAgain(testFailingClose(new BufferedWriter(new FailingCloseWriter())));

        closeAgain(testFailingFlush(new BufferedOutputStream(new FailingFlushOutputStream())));
        closeAgain(testFailingFlush(new BufferedWriter(new FailingFlushWriter())));

        if (failed > 0)
            throw new RuntimeException(failed + " test(s) failed - see log for details");
    }
}
