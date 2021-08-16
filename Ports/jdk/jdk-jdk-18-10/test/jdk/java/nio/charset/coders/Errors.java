/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Check that error cases are replaced correctly in String/ISR/OSW
 * @bug 4457851 7096080
 *
 * @build Errors Util
 * @run main Errors
 */

import java.io.*;
import java.nio.*;


public class Errors {

    static PrintStream log = System.err;
    static int failures = 0;

    static final byte Q = (byte)'?';
    static final byte X = (byte)'x';
    static final byte Y = (byte)'y';
    static final byte Z = (byte)'z';

    static abstract class Test {

        protected final String csn;
        protected final String what;

        Test(String csn, String what) {
            this.csn = csn;
            this.what = what;
        }

        abstract byte[] enc(String s) throws IOException;

        Test test(String s, byte[] ref) {
            log.print("  " + Util.toString(s.toCharArray()));
            byte[] ba = null;
            try {
                ba = enc(s);
            } catch (IOException x) {
                log.println(" -e-> ERROR: " + x.getClass().getName());
                failures++;
                return this;
            }
            log.println(" -e-> " + Util.toString(ba));
            int i = Util.cmp(ba, ref);
            if (i >= 0) {
                log.println("    ERROR: Mismatch at index " + i
                            + ", expected: " + Util.toString(ref));
                failures++;
            }
            return this;
        }

        abstract String dec(byte[] ba) throws IOException;

        Test test(byte[] ba, String ref) {
            log.print("  " + Util.toString(ba));
            String s = null;
            try {
                s = dec(ba);
            } catch (IOException x) {
                log.println(" -d-> ERROR: " + x.getClass().getName());
                failures++;
                return this;
            }
            log.println(" -d-> " + Util.toString(s.toCharArray()));
            char[] ca = s.toCharArray();
            char[] refa = ref.toCharArray();
            int i = Util.cmp(ca, refa);
            if (i >= 0) {
                log.println("    ERROR: Mismatch at index " + i
                            + ", expected: " + Util.toString(refa));
                failures++;
            }
            return this;
        }

        Test run() {
            log.println(csn + ", " + what);

            test("xyzzy", new byte[] { X, Y, Z, Z, Y });

            // Malformed surrogates
            test("\uD800x", new byte[] { Q, X });
            test("\uDC00x", new byte[] { Q, X });
            test("\uD800\uDB00x", new byte[] { Q, Q, X });

            return this;
        }

    }

    static class TestStream extends Test {

        TestStream(String csn) {
            super(csn, "I/O streams");
        }

        byte[] enc(String s) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Writer wr = new OutputStreamWriter(bos, csn);
            wr.write(s);
            wr.close();
            return bos.toByteArray();
        }

        String dec(byte[] ba) throws IOException {
            ByteArrayInputStream bis = new ByteArrayInputStream(ba);
            Reader rd = new InputStreamReader(bis, csn);
            char[] ca = new char[1024];
            int n = rd.read(ca);
            String s = new String(ca, 0, n);
            rd.close();
            return s;
        }

    }

    static class TestString extends Test {

        TestString(String csn) {
            super(csn, "strings");
        }

        byte[] enc(String s) throws IOException {
            return s.getBytes(csn);
        }

        String dec(byte[] ba) throws IOException {
            return new String(ba, 0, ba.length, csn);
        }

    }

    static void test_US_ASCII(Test t) {
        t.run();
        t.test("\u0080", new byte[] { Q });
        t.test("\u0100", new byte[] { Q });
        t.test("\uD800\uDC00", new byte[] { Q });
        t.test("\uF000", new byte[] { Q });
        t.test("\uFFFE", new byte[] { Q });
        t.test("\uFFFF", new byte[] { Q });
        t.test(new byte[] { X, (byte)0x7f, Y }, "x\u007Fy");
        t.test(new byte[] { X, (byte)0x80, Y }, "x\uFFFDy");
        t.test(new byte[] { (byte)0xf0, (byte)0xf0 }, "\uFFFD\uFFFD");
    }

    static void test_ISO_8859_1(Test t) {
        t.run();
        t.test("\u0080", new byte[] { (byte)0x80 });
        t.test("\u0100", new byte[] { Q });
        t.test("\uD800\uDC00x", new byte[] { Q, X });
        t.test("\uF000", new byte[] { Q });
        t.test("\uFFFE", new byte[] { Q });
        t.test("\uFFFF", new byte[] { Q });
        t.test(new byte[] { X, (byte)0x7f, Y }, "x\u007Fy");
        t.test(new byte[] { X, (byte)0x80, Y }, "x\u0080y");
        t.test(new byte[] { (byte)0xf0, (byte)0xf0 }, "\u00F0\u00F0");
    }

    static void test_UTF_8(Test t) {
        t.run();
        t.test("\u0080", new byte[] { (byte)0xC2, (byte)0x80 });
        t.test("\u0100", new byte[] { (byte)0xC4, (byte)0x80 });
        t.test("\uD800\uDC00",
               new byte[] { (byte)0xF0, (byte)0x90, (byte)0x80, (byte)0x80 });
        t.test("\uF000", new byte[] { (byte)0xEF, (byte)0x80, (byte)0x80 });
        t.test("\uFFFE", new byte[] { (byte)0xEF, (byte)0xBF, (byte)0xBE });
        t.test("\uFFFF", new byte[] { (byte)0xEF, (byte)0xBF, (byte)0xBF });
        t.test(new byte[] { X, (byte)0x7f, Y }, "x\u007Fy");
        t.test(new byte[] { X, (byte)0x80, Y }, "x\uFFFDy");
    }

    public static void main(String[] args) throws Exception {
        test_US_ASCII(new TestString("US-ASCII"));
        test_US_ASCII(new TestStream("US-ASCII"));

        test_ISO_8859_1(new TestString("ISO-8859-1"));
        test_ISO_8859_1(new TestStream("ISO-8859-1"));

        test_ISO_8859_1(new TestString("ISO-8859-15"));
        test_ISO_8859_1(new TestStream("ISO-8859-15"));

        test_UTF_8(new TestString("UTF-8"));
        test_UTF_8(new TestStream("UTF-8"));

        if (failures > 0) {
            log.println();
            throw new Exception("Tests failed: " + failures);
        }

    }

}
