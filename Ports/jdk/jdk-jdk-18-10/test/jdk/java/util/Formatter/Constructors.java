/*
 * Copyright (c) 2004, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4981811 4984465 5064492 6240171 7000511
 * @summary Unit test for all constructors introduced by the formatter feature
 */

import java.io.*;
import java.util.*;
import java.nio.charset.Charset;

public class Constructors {

    private static int fail = 0;
    private static int pass = 0;

    private static Throwable first;

    static void pass() {
        pass++;
    }

    static void fail(String fs) {
        String s = "'" + fs + "': exception not thrown";
        if (first == null)
            first = new RuntimeException(s);
        System.err.println("FAILED: " + s);
        fail++;
    }

    static void fail(String fs, Throwable ex) {
        String s = "'" + fs + "': " + ex.getClass().getName() + " thrown";
        if (first == null)
            first = ex;
        System.err.println("FAILED: " + s);
        fail++;
    }

    static void locale(Formatter f) {
        locale(f, Locale.getDefault(Locale.Category.FORMAT));
    }

    static void locale(Formatter f, Locale l) {
        try {
            if ((l != null && !l.equals(f.locale()))
                || (l == null && f.locale() != null))
                    throw new RuntimeException(f.locale() + " != " + l);
            pass();
        } catch (RuntimeException x) {
            fail(x.getMessage());
        }
    }

    static void out(Formatter f, Class c) {
        try {
            Appendable a = f.out();
            if (!c.isInstance(a))
                throw new RuntimeException(a.getClass().getName()
                                           + " != " + c.getName());
            pass();
        } catch (RuntimeException x) {
            fail(x.getMessage());
        }
    }

    public static void main(String [] args) {
        // Formatter()
        try (Formatter f = new Formatter()) {
            pass();
            out(f, StringBuilder.class);
            locale(f);
        } catch (Exception x) {
            fail("new Formatter()", x);
        }

        // Formatter(Appendable a)
        try (Formatter f = new Formatter((Appendable) null)) {
            pass();
            out(f, StringBuilder.class);
            locale(f);
        } catch (Exception x) {
            fail("new Formatter((Appendable)null)", x);
        }

        // Formatter(Locale l)
        try (Formatter f = new Formatter((Locale) null)) {
            pass();
            out(f, StringBuilder.class);
            locale(f, null);
        } catch (Exception x) {
            fail("new Formatter((Locale)null)", x);
        }

        // Formatter(Appendable a, Locale l)
        try (Formatter f = new Formatter((Appendable) null, (Locale) null)) {
            pass();
            out(f, StringBuilder.class);
            locale(f, null);
        } catch (Exception x) {
            fail("new Formatter((Appendable) null, (Locale) null)", x);
        }

        // Formatter(String fileName)
        try (Formatter f = new Formatter("foo")) {
            pass();
            out(f, BufferedWriter.class);
            locale(f);
        } catch (Exception x) {
            fail("new Formatter(\"foo\")", x);
        }

        try {
            new Formatter((String)null);
            fail("new Formatter((String)null)");
        } catch (NullPointerException x) {
            pass();
        } catch (Exception x) {
            fail("new Formatter((String)null)", x);
        }

        // Formatter(String fileName, String csn)
        try (Formatter f = new Formatter("foo", "UTF-8")) {
            pass();
            out(f, BufferedWriter.class);
            locale(f);
        } catch (Exception x) {
            fail("new Formatter(\"foo\", \"UTF-8\")", x);
        }

        try {
            new Formatter("foo", "bar");
            fail("new Formatter(\"foo\", \"bar\")");
        } catch (UnsupportedEncodingException x) {
            pass();
        } catch (Exception x) {
            fail("new Formatter(\"foo\", \"bar\")", x);
        }

        try {
            new Formatter(".", "bar");
            fail("new Formatter(\".\", \"bar\")");
        } catch (FileNotFoundException|UnsupportedEncodingException x) {
            pass();
        } catch (Exception x) {
            fail("new Formatter(\".\", \"bar\")", x);
        }

        // Formatter(String fileName, String csn, Locale l)
        try (Formatter f = new Formatter("foo", "ISO-8859-1", Locale.GERMANY)) {
            pass();
            out(f, BufferedWriter.class);
            locale(f, Locale.GERMANY);
        } catch (Exception x) {
            fail("new Formatter(\"foo\", \"ISO-8859-1\", Locale.GERMANY)", x);
        }

        try (Formatter f = new Formatter("foo", "ISO-8859-1", null)) {
            pass();
            locale(f, null);
            out(f, BufferedWriter.class);
        } catch (Exception x) {
            fail("new Formatter(\"foo\", \"ISO-8859-1\", null)", x);
        }

        // Formatter(File)
        try (Formatter f = new Formatter(new File("foo"))) {
            pass();
            locale(f);
            out(f, BufferedWriter.class);
        } catch (Exception x) {
            fail("new Formatter(new File(\"foo\")", x);
        }

        try {
            new Formatter((File)null);
            fail("new Formatter((File)null)");
        } catch (NullPointerException x) {
            pass();
        } catch (Exception x) {
            fail("new Formatter((File)null)", x);
        }

        // Formatter(PrintStream ps)
        try {
            // ambiguity detected at compile-time
            Formatter f = new Formatter(System.out);
            pass();
            out(f, PrintStream.class);
            locale(f);
        } catch (Exception x) {
            fail("new Formatter(System.out)", x);
        }

        try {
            new Formatter((PrintStream) null);
            fail("new Formatter((PrintStream) null)");
        } catch (NullPointerException x) {
            pass();
        } catch (Exception x) {
            fail("new Formatter((PrintStream) null)", x);
        }

        try (Formatter f = new Formatter(new PrintStream("foo"))) {
            pass();
            locale(f);
            out(f, PrintStream.class);
        } catch (FileNotFoundException x) {
            fail("new Formatter(new PrintStream(\"foo\")", x);
        } catch (Exception x) {
            fail("new Formatter(new PrintStream(\"foo\")", x);
        }

        try (Formatter f = new Formatter(new PrintStream("foo"),
                                         Locale.JAPANESE)) {
            pass();
            locale(f, Locale.JAPANESE);
            out(f, PrintStream.class);
        } catch (FileNotFoundException x) {
            fail("new Formatter(new PrintStream(\"foo\")", x);
        } catch (Exception x) {
            fail("new Formatter(new PrintStream(\"foo\")", x);
        }

        try (PrintStream ps = new PrintStream("foo")) {
            // The cast here is necessary to avoid an ambiguity error
            // between Formatter(Appendable a, Locale l)
            // and Formatter(OutputStream os, String csn)
            new Formatter(ps, (String)null);
            fail("new Formatter(new PrintStream(\"foo\"), (String)null)");
        } catch (FileNotFoundException x) {
            fail("new Formatter(new PrintStream(\"foo\"), (String)null)", x);
        } catch (NullPointerException x) {
            pass();
        } catch (UnsupportedEncodingException x) {
            fail("new Formatter(new PrintStream(\"foo\"), (String)null)", x);
        } catch (Exception x) {
            fail("new Formatter(new PrintStream(\"foo\"), (String)null)", x);
        }

        // The cast here is necessary to avoid an ambiguity error
        // between Formatter(Appendable a, Locale l)
        // and  Formatter(OutputStream os, String csn)
        try (Formatter f = new Formatter(new PrintStream("foo"),
                                         (Locale)null)) {
            pass();
            locale(f, null);
            out(f, PrintStream.class);
        } catch (FileNotFoundException x) {
            fail("new Formatter(new PrintStream(\"foo\"), (Locale)null)", x);
        } catch (Exception x) {
            fail("new Formatter(new PrintStream(\"foo\"), (Locale)null)", x);
        }

        try (Formatter f = new Formatter(new PrintStream("foo"),
                                         Locale.KOREAN)) {
            pass();
            locale(f, Locale.KOREAN);
            out(f, PrintStream.class);
        } catch (FileNotFoundException x) {
            fail("new Formatter(new PrintStream(\"foo\"), Locale.KOREAN)", x);
        } catch (Exception x) {
            fail("new Formatter(new PrintStream(\"foo\"), Locale.KOREAN)", x);
        }

        try (Formatter f = new Formatter(new PrintStream("foo"),
                                         "UTF-16BE", null)) {
            pass();
            locale(f, null);
            out(f, BufferedWriter.class);
        } catch (FileNotFoundException x) {
            fail("new Formatter(new PrintStream(\"foo\"), \"UTF-16BE\", null");
        } catch (UnsupportedEncodingException x) {
            fail("new Formatter(new PrintStream(\"foo\"), \"UTF-16BE\", null");
        } catch (Exception x) {
            fail("new Formatter(new PrintStream(\"foo\"), \"UTF-16BE\", null");
        }

        try (Formatter f = new Formatter(new PrintStream("foo"),
                                         "UTF-16BE", Locale.ITALIAN)) {
            pass();
            locale(f, Locale.ITALIAN);
            out(f, BufferedWriter.class);
        } catch (FileNotFoundException x) {
            fail("new Formatter(new PrintStream(\"foo\"), \"UTF-16BE\", Locale.ITALIAN");
        } catch (UnsupportedEncodingException x) {
            fail("new Formatter(new PrintStream(\"foo\"), \"UTF-16BE\", Locale.ITALIAN");
        } catch (Exception x) {
            fail("new Formatter(new PrintStream(\"foo\"), \"UTF-16BE\", Locale.ITALIAN");
        }

        String csn = Charset.defaultCharset().newEncoder().canEncode('\u00a3') ?
            "ASCII" : "ISO-8859-1";
        try {
            ByteArrayOutputStream bs[] = { new ByteArrayOutputStream(),
                                           new ByteArrayOutputStream(),
                                           new ByteArrayOutputStream()
            };
            new Formatter((Appendable)  new PrintStream(bs[0], true, csn)).format("\u00a3");
            new Formatter((OutputStream)new PrintStream(bs[1], true, csn)).format("\u00a3");
            new Formatter(              new PrintStream(bs[2], true, csn)).format("\u00a3");
            if (Arrays.equals(bs[0].toByteArray(), bs[1].toByteArray())) {
                fail("arrays shouldn't match: " + bs[0].toByteArray());
            } else {
                pass();
            }
            if (! Arrays.equals(bs[0].toByteArray(), bs[2].toByteArray())) {
                fail("arrays should match: " + bs[0].toByteArray() + " != "
                     + bs[2].toByteArray());
            } else {
                pass();
            }
        } catch (UnsupportedEncodingException x) {
            fail("new PrintStream(newByteArrayOutputStream, true, " + csn + ")", x);
        }

        // Formatter(OutputStream os)
        try {
            new Formatter((OutputStream) null);
            fail("new Formatter((OutputStream) null)");
        } catch (NullPointerException x) {
            pass();
        } catch (Exception x) {
            fail("new Formatter((OutputStream) null)", x);
        }

        try (Formatter f = new Formatter((OutputStream) new PrintStream("foo"))) {
            pass();
            locale(f);
            out(f, BufferedWriter.class);
        } catch (Exception x) {
            fail("new Formatter((OutputStream) new PrintStream(\"foo\")", x);
        }

        // Formatter(OutputStream os, String csn)
        try {
            new Formatter((OutputStream) null, "ISO-8859-1");
            fail("new Formatter((OutputStream) null, \"ISO-8859-1\")");
        } catch (NullPointerException x) {
            pass();
        } catch (Exception x) {
            fail("new Formatter((OutputStream) null, \"ISO-8859-1\")", x);
        }

        try (PrintStream ps = new PrintStream("foo")) {
            new Formatter((OutputStream) ps, null);
            fail("new Formatter((OutputStream) new PrintStream(\"foo\"), null");
        } catch (NullPointerException x) {
            pass();
        } catch (Exception x) {
            fail("new Formatter((OutputStream) new PrintStream(\"foo\"), null",
                 x);
        }

        try (PrintStream ps = new PrintStream("foo")) {
            new Formatter(ps, "bar");
            fail("new Formatter(new PrintStream(\"foo\"), \"bar\")");
        } catch (UnsupportedEncodingException x) {
            pass();
        } catch (Exception x) {
            fail("new Formatter(new PrintStream(\"foo\"), \"bar\")", x);
        }

        try (Formatter f = new Formatter(new PrintStream("foo"), "UTF-8")) {
            pass();
            locale(f);
            out(f, BufferedWriter.class);
        } catch (Exception x) {
            fail("new Formatter(new PrintStream(\"foo\"), \"UTF-8\")", x);
        }

        // Formatter(OutputStream os, String csn, Locale l)
        try {
            new Formatter((OutputStream) null, "ISO-8859-1", Locale.UK);
            fail("new Formatter((OutputStream) null, \"ISO-8859-1\", Locale.UK)");
        } catch (NullPointerException x) {
            pass();
        } catch (Exception x) {
            fail("new Formatter((OutputStream) null, \"ISO-8859-1\", Locale.UK)",
                 x);
        }

        try (PrintStream ps = new PrintStream("foo")) {
            new Formatter(ps, (String)null, Locale.UK);
            fail("new Formatter(new PrintStream(\"foo\"), (String)null, Locale.UK)");
        } catch (NullPointerException x) {
            pass();
        } catch (Exception x) {
            fail("new Formatter(new PrintStream(\"foo\"), (String)null, Locale.UK)",
                 x);
        }

        // Formatter(OutputStream os, Charset charset, Locale l)
        try (PrintStream ps = new PrintStream("foo")) {
            new Formatter(ps, (Charset)null, Locale.UK);
            fail("new Formatter(new PrintStream(\"foo\"), (Charset)null, Locale.UK)");
        } catch (NullPointerException x) {
            pass();
        } catch (Exception x) {
            fail("new Formatter(new PrintStream(\"foo\"), (Charset)null, Locale.UK)",
                 x);
        }

        try (PrintStream ps = new PrintStream("foo")) {
            new Formatter(ps, "bar", Locale.UK);
            fail("new Formatter(new PrintStream(\"foo\"), \"bar\", Locale.UK)");
        } catch (UnsupportedEncodingException x) {
            pass();
        } catch (Exception x) {
            fail("new Formatter(new PrintStream(\"foo\"), \"bar\", Locale.UK)",
                 x);
        }

        try (Formatter f = new Formatter(new PrintStream("foo"), "UTF-8", Locale.UK)) {
            pass();
            out(f, BufferedWriter.class);
            locale(f, Locale.UK);
        } catch (Exception x) {
            fail("new Formatter(new PrintStream(\"foo\"), \"UTF-8\"), Locale.UK",
                 x);
        }

        // PrintStream(String fileName)
        try (PrintStream ps = new PrintStream("foo")) {
            pass();
        } catch (Exception x) {
            fail("new PrintStream(\"foo\")", x);
        }

        // PrintStream(String fileName, String csn)
        try {
            new PrintStream("foo", (String)null);
            fail("new PrintStream(\"foo\", (String)null)");
        } catch (NullPointerException x) {
            pass();
        } catch (Exception x) {
            fail("new PrintStream(\"foo\", (String)null)", x);
        }

        // PrintStream(String fileName, Charset charset)
        try {
            new PrintStream("foo", (Charset)null);
            fail("new PrintStream(\"foo\", (Charset)null)");
        } catch (NullPointerException x) {
            pass();
        } catch (Exception x) {
            fail("new PrintStream(\"foo\", (Charset)null)", x);
        }

        // PrintStream(File file)
        try (PrintStream ps = new PrintStream(new File("foo"))) {
            pass();
        } catch (Exception x) {
            fail("new PrintStream(new File(\"foo\"))", x);
        }

        // PrintStream(File file, String csn)
        try {
            new PrintStream(new File("foo"), (String)null);
            fail("new PrintStream(new File(\"foo\"), (String)null)");
        } catch (NullPointerException x) {
            pass();
        } catch (Exception x) {
            fail("new PrintStream(new File(\"foo\"), (String)null)", x);
        }

        // PrintStream(File file, Charset charset)
        try {
            new PrintStream(new File("foo"), (Charset)null);
            fail("new PrintStream(new File(\"foo\"), (Charset)null)");
        } catch (NullPointerException x) {
            pass();
        } catch (Exception x) {
            fail("new PrintStream(new File(\"foo\"), (Charset)null)", x);
        }

        // PrintWriter(String fileName)
        try (PrintWriter pw = new PrintWriter("foo")) {
            pass();
        } catch (Exception x) {
            fail("new PrintWriter(\"foo\")", x);
        }

        // PrintWriter(String fileName, String csn)
        try {
            new PrintWriter("foo", (String)null);
            fail("new PrintWriter(\"foo\"), (String)null");
        } catch (NullPointerException x) {
            pass();
        } catch (Exception x) {
            fail("new PrintWriter(\"foo\"), (String)null", x);
        }

        // PrintWriter(String fileName, Charset charset)
        try {
            new PrintWriter("foo", (Charset)null);
            fail("new PrintWriter(\"foo\"), (Charset)null");
        } catch (NullPointerException x) {
            pass();
        } catch (Exception x) {
            fail("new PrintWriter(\"foo\"), (Charset)null", x);
        }

        // PrintWriter(File file)
        try (PrintWriter pw = new PrintWriter(new File("foo"))) {
            pass();
        } catch (Exception x) {
            fail("new PrintWriter(new File(\"foo\"))", x);
        }

        // PrintWriter(File file, String csn)
        try {
            new PrintWriter(new File("foo"), (String)null);
            fail("new PrintWriter(new File(\"foo\")), (String)null");
        } catch (NullPointerException x) {
            pass();
        } catch (Exception x) {
            fail("new PrintWriter(new File(\"foo\")), (String)null", x);
        }

        // PrintWriter(File file, Charset charset)
        try {
            new PrintWriter(new File("foo"), (Charset)null);
            fail("new PrintWriter(new File(\"foo\")), (Charset)null");
        } catch (NullPointerException x) {
            pass();
        } catch (Exception x) {
            fail("new PrintWriter(new File(\"foo\")), (Charset)null", x);
        }

        if (fail != 0)
            throw new RuntimeException((fail + pass) + " tests: "
                                       + fail + " failure(s), first", first);
        else
            System.out.println("all " + (fail + pass) + " tests passed");
    }
}
