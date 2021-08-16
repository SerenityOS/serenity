/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5067405
 * @summary Basic test for classes which implement Appendable.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

interface BasicRunnable extends Runnable {
    void init(Appendable a, String csq, String exp);
    Appendable reset(Appendable csq);
}

public class Basic {

    private static final String s = "Beware the Jabberwock, my son!";
    private static CharArrayWriter gw = new CharArrayWriter();
    private static ByteArrayOutputStream gos = new ByteArrayOutputStream();

    private static File newFile() {
        File f = null;
        try {
            f = File.createTempFile("append", ".txt");
            f.deleteOnExit();
        } catch (IOException x) {
            fail(x);
        }
        return f;
    }
    private static File gf = newFile();

    private static int fail = 0;
    private static int pass = 0;

    private static Throwable first;

    static void pass() {
        pass++;
    }

    static void fail(Throwable ex) {
        if (first == null)
            first = ex;
        System.err.println("FAILED: unexpected exception");
        fail++;
    }

    static void fail(String fs, Throwable ex) {
        String s = "'" + fs + "': " + ex.getClass().getName() + " not thrown";
        if (first == null)
            first = ex;
        System.err.println("FAILED: " + s);
        fail++;
    }

    static void fail(String fs, String exp, String got) {
        String s = "'" + fs + "': Expected '" + exp + "', got '" + got + "'";
        if (first == null)
            first = new RuntimeException(s);
        System.err.println("FAILED: " + s);
        fail++;
    }

    static void ck(String s, String exp, String got) {
        if (!exp.equals(got))
            fail(s, exp, got);
        else
            pass();
    }

    private static BasicRunnable testBufferedWriter =
        new BasicRunnable() {
            private String csn, exp;
            public void init(Appendable bw, String csn, String exp) {
                try {
                    ((BufferedWriter)bw).flush();
                } catch (IOException x) {
                    fail(x);
                }
                this.csn = csn;
                this.exp = exp;
            }
            public void run() {
                ck("BufferedWriter.append(" + csn + ")", exp, gw.toString());
            }
            public Appendable reset(Appendable bw) {
                gw.reset();
                return bw;
            }};

    private static BasicRunnable testCharArrayWriter =
        new BasicRunnable() {
            private String csn, exp;
            private CharArrayWriter cw;
            public void init(Appendable cw, String csn, String exp) {
                this.cw = (CharArrayWriter)cw;
                this.csn = csn;
                this.exp = exp;
            }
            public void run() {
                ck("CharArrayWriter.append(" + csn + ")", exp, cw.toString());
            }
            public Appendable reset(Appendable cw) {
                ((CharArrayWriter)cw).reset();
                return cw;
            }};

    private static BasicRunnable testFileWriter =
        new BasicRunnable() {
            private String csn, exp;
            public void init(Appendable fw, String csn, String exp) {
                try {
                    ((FileWriter)fw).flush();
                } catch (IOException x) {
                    fail(x);
                }
                this.csn = csn;
                this.exp = exp;
            }
            public void run() {
                StringBuilder sb = new StringBuilder();
                try {
                    BufferedReader in = new BufferedReader(new FileReader(gf));
                    String line;
                    while (true) {
                        if ((line = in.readLine()) == null)
                            break;
                        sb.append(line);
                    }
                } catch (IOException x) {
                    fail(x);
                }
                ck("FileWriter.append(" + csn + ")", exp, sb.toString());
            }
            public Appendable reset(Appendable fw) {
                try {
                    fw = new FileWriter(gf);
                } catch (IOException x) {
                    fail(x);
                }
                return fw;
            }};

    private static BasicRunnable testOutputStreamWriter =
        new BasicRunnable() {
            private String csn, exp;
            public void init(Appendable osw, String csn, String exp) {
                try {
                    ((OutputStreamWriter)osw).flush();
                } catch (IOException x) {
                    fail(x);
                }
                this.csn = csn;
                this.exp = exp;
            }
            public void run() {
                ck("OutputStreamWriter.append(" + csn + ")", exp, gos.toString());
            }
            public Appendable reset(Appendable osw) {
                gos.reset();
                return osw;
            }};

    private static BasicRunnable testPrintWriter =
        new BasicRunnable() {
            private String csn, exp;
            public void init(Appendable pw, String csn, String exp) {
                ((PrintWriter)pw).flush();
                this.csn = csn;
                this.exp = exp;
            }
            public void run() {
                ck("PrintWriter.append(" + csn + ")", exp, gw.toString());
            }
            public Appendable reset(Appendable pw) {
                gw.reset();
                return pw;
            }};

    private static BasicRunnable testStringWriter =
        new BasicRunnable() {
            private String csn, exp;
            private StringWriter sw;
            public void init(Appendable sw, String csn, String exp) {
                this.sw = (StringWriter)sw;
                this.csn = csn;
                this.exp = exp;
            }
            public void run() {
                ck("StringWriter.append(" + csn + ")", exp, sw.toString());
            }
            public Appendable reset(Appendable sw) {
                return new StringWriter();
            }};

    private static BasicRunnable testPrintStream =
        new BasicRunnable() {
            private String csn, exp;
            public void init(Appendable ps, String csn, String exp) {
                ((PrintStream)ps).flush();
                this.csn = csn;
                this.exp = exp;
            }
            public void run() {
                ck("PrintStream.append(" + csn + ")", exp, gos.toString());
            }
            public Appendable reset(Appendable ps) {
                gos.reset();
                return ps;
            }};

    private static BasicRunnable testCharBuffer =
        new BasicRunnable() {
            private String csn, exp;
            private CharBuffer cb;
            public void init(Appendable cb, String csn, String exp) {
                this.cb = (CharBuffer)cb;
                this.csn = csn;
                this.exp = exp;
            }
            public void run() {
                cb.limit(cb.position()).rewind();
                ck("CharBuffer.append(" + csn + ")", exp, cb.toString());
            }
            public Appendable reset(Appendable cb) {
                ((CharBuffer)cb).clear();
                return cb;
            }};

    private static BasicRunnable testStringBuffer =
        new BasicRunnable() {
            private String csn, exp;
            private StringBuffer sb;
            public void init(Appendable sb, String csn, String exp) {
                this.sb = (StringBuffer)sb;
                this.csn = csn;
                this.exp = exp;
            }
            public void run() {
                ck("StringBuffer.append(" + csn + ")", exp, sb.toString());
            }
            public Appendable reset(Appendable sb) {
                return new StringBuffer();
            }};

    private static BasicRunnable testStringBuilder =
        new BasicRunnable() {
            private String csn, exp;
            private StringBuilder sb;
            public void init(Appendable sb, String csn, String exp) {
                this.sb = (StringBuilder)sb;
                this.csn = csn;
                this.exp = exp;
            }
            public void run() {
                ck("StringBuilder.append(" + csn + ")", exp, sb.toString());
            }
            public Appendable reset(Appendable sb) {
                return new StringBuilder();
            }};

    private static void test(Appendable a, CharSequence csq, BasicRunnable thunk) {
        // appends that should always work
        int [][] sp = { { 0, 0 }, { 11, 11 }, { 11, 21 }, { 0, 7 },
                        { 0, s.length() }, { s.length(), s.length() },
        };
        for (int j = 0; j < sp.length; j++) {
            int start = sp[j][0];
            int end = sp[j][1];
            try {
                thunk.init(a.append(csq, start, end),
                           csq.getClass().getName(),
                           s.subSequence(start, end).toString());
                thunk.run();
                a = thunk.reset(a);
            } catch (IOException x) {
                fail(x);
            }
        }

        // appends that should always throw IndexOutOfBoundsException
        int [][] sf = { { -1, 0 }, { 0, -1 }, { 11, 10 },
                        { 0, s.length() + 1},
        };
        for (int j = 0; j < sf.length; j++) {
            int start = sf[j][0];
            int end = sf[j][1];
            try {
                a.append(csq, start, end);
                fail("start = " + start + ", end = " + end,
                     new IndexOutOfBoundsException());
                a = thunk.reset(a);
            } catch (IndexOutOfBoundsException x) {
                pass();
            } catch (IOException x) {
                fail(x);
            }
        }

        // appends of null
        int start = 1;
        int end = 2;
        try {
            thunk.init(a.append(null, start, end), "null",
                       "null".subSequence(start, end).toString());
            thunk.run();
            a = thunk.reset(a);
        } catch (IOException x) {
            fail(x);
        }
    }

    public static void main(String [] args) throws Exception {
        // CharSequences
        CharBuffer cb = CharBuffer.allocate(128).put(s);
        cb.limit(s.length()).rewind();
        CharBuffer dcb = ByteBuffer.allocateDirect(128).asCharBuffer().put(s);
        dcb.limit(s.length()).rewind();
        CharSequence [] ca = { s,
                               new StringBuffer(s),
                               new StringBuilder(s),
                               cb,
                               dcb,
        };

        // Appendables/Writers
        Object [][] wa = { { new CharArrayWriter(), testCharArrayWriter },
                           { new BufferedWriter(gw), testBufferedWriter },
                           // abstract, no implementing classes in jdk
                           // { new FilterWriter(), testFilterWriter },
                           { new FileWriter(gf), testFileWriter },
                           { new OutputStreamWriter(gos), testOutputStreamWriter },
                           // covered by previous two test cases
                           // { new PipedWriter(gw), testPipedWriter },
                           { new PrintWriter(gw), testPrintWriter },
                           { new StringWriter(), testStringWriter },
        };

        for (int i = 0; i < ca.length; i++) {
            CharSequence a = ca[i];
            for (int j = 0; j < wa.length; j++)
                test((Writer)wa[j][0], a, (BasicRunnable)wa[j][1]);

            // other Appendables
            test(new PrintStream(gos), a, testPrintStream);
            test(CharBuffer.allocate(128), a, testCharBuffer);
            test(ByteBuffer.allocateDirect(128).asCharBuffer(), a, testCharBuffer);
            test(new StringBuffer(), a, testStringBuffer);
            test(new StringBuilder(), a, testStringBuilder);
        }

        if (fail != 0)
            throw new RuntimeException((fail + pass) + " tests: "
                                       + fail + " failure(s), first", first);
        else
            System.out.println("all " + (fail + pass) + " tests passed");
    }
}
