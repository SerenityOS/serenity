/*
 * Copyright (c) 2001, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Unit test for java.net.URL (Based on the URI tests that is authored by Mark Reinhold)
 * @bug 4496251
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.MalformedURLException;


public class Test {

    static PrintStream out = System.out;
    static int testCount = 0;

    // Properties that we check
    static final int PARSEFAIL   = 1 << 0;
    static final int PROTOCOL    = 1 << 1;
    static final int USERINFO    = 1 << 2;
    static final int HOST        = 1 << 3;
    static final int PORT        = 1 << 4;
    static final int PATH        = 1 << 5;
    static final int QUERY       = 1 << 6;
    static final int REF         = 1 << 7;

    String input;
    URL url = null;
    URL originalURL;
    URL base = null;                    // Base for resolution/relativization
    String op = null;                   // Op performed if url != originalURL
    int checked = 0;                    // Mask for checked properties
    int failed = 0;                     // Mask for failed properties
    Exception exc = null;

    private Test(String s) {
        testCount++;
        input = s;
        try {
            url = new URL(s);
        } catch (MalformedURLException x) {
            exc = x;
        }
        originalURL = url;
    }

    static Test test(String s) {
        return new Test(s);
    }

    private Test(String s, boolean xxx) {
        testCount++;
        try {
            url = new URL(s);
        } catch (Exception x) {
            exc = x;
        }
        if (url != null)
            input = url.toString();
        originalURL = url;
    }

    static Test test(URL base, String spec) {
        return new Test(base, spec);
    }
    private Test(URL base, String spec) {
        testCount++;
        try {
            url = new URL(base, spec);
        } catch (Exception x) {
            exc = x;
        }
        if (url != null)
            input = url.toString();
        originalURL = url;
    }

   static Test test(String protocol, String host, int port, String file) {
        return new Test(protocol, host, port, file);
    }
    private Test(String protocol, String host, int port, String file) {
        testCount++;
        try {
            url = new URL(protocol, host, port, file);
        } catch (Exception x) {
            exc = x;
        }
        if (url != null)
            input = url.toString();
        originalURL = url;
    }

    boolean parsed() {
        return url != null;
    }

    boolean resolved() {
        return base != null;
    }

    URL url() {
        return url;
    }

    // Operations on Test instances
    //
    // These are short so as to make test cases compact.
    //
    //    s      Scheme
    //    u      User info
    //    h      Host
    //    n      port Number
    //    p      Path
    //    q      Query
    //    f      Fragment
    //
    //    rslv   Resolve against given base
    //    rtvz   Relativize
    //    psa    Parse server Authority
    //    norm   Normalize
    //
    //    x      Check that parse failed as expected
    //    z      End -- ensure that unchecked components are null

    private boolean check1(int prop) {
        checked |= prop;
        if (!parsed()) {
            failed |= prop;
            return false;
        }
        return true;
    }

    private void check2(String s, String ans, int prop) {
        if (s == null && ans == null)
            return;
        if ((s == null) || !s.equals(ans))
            failed |= prop;
    }

    Test s(String s) {
        if (check1(PROTOCOL)) check2(url.getProtocol(), s, PROTOCOL);
        return this;
    }

    Test u(String s) {
        if (check1(USERINFO)) check2(url.getUserInfo(), s, USERINFO);
        return this;
    }

    Test h(String s) {
        if (check1(HOST)) check2(url.getHost(), s, HOST);
        return this;
    }

    Test n(int n) {
        checked |= PORT;
        if (!parsed() || (url.getPort() != n))
            failed |= PORT;
        return this;
    }

    Test p(String s) {
        if (check1(PATH)) check2(url.getPath(), s, PATH);
        return this;
    }

    Test q(String s) {
        if (check1(QUERY)) check2(url.getQuery(), s, QUERY);
        return this;
    }

    Test f(String s) {
        if (check1(REF)) check2(url.getRef(), s, REF);
        return this;
    }

    Test x() {
        checked |= PARSEFAIL;
        if (parsed())
            failed |= PARSEFAIL;
        return this;
    }

    private void checkEmpty(String s, int prop) {
        if (((checked & prop) == 0) && (s != null))
            failed |= prop;
    }

    // Check that unchecked component properties are not defined,
    // and report any failures
    Test z() {
        if (!parsed()) {
            report();
            return this;
        }
        checkEmpty(url.getProtocol(), PROTOCOL);
        checkEmpty(url.getUserInfo(), USERINFO);
        checkEmpty(url.getHost(), HOST);
        if (((checked & PORT) == 0) && (url.getPort() != -1)) failed |= PORT;
        checkEmpty(url.getPath(), PATH);
        checkEmpty(url.getQuery(), QUERY);
        checkEmpty(url.getRef(), REF);
        report();
        return this;
    }


    // Summarization and reporting

    static void header(String s) {
        out.println();
        out.println();
        out.println("-- " + s + " --");
    }

    static void show(String prefix, MalformedURLException x) {
        out.println(prefix + ": " + x.getMessage());
    }

    private void summarize() {
        out.println();
        StringBuffer sb = new StringBuffer();
        if (input.length() == 0)
            sb.append("\"\"");
        else
            sb.append(input);
        if (base != null) {
            sb.append(" ");
            sb.append(base);
        }
        if (!parsed()) {
            String s = (((checked & PARSEFAIL) != 0)
                        ? "Correct exception" : "UNEXPECTED EXCEPTION");
            if (exc instanceof MalformedURLException)
                show(s, (MalformedURLException)exc);
            else {
                out.println(sb.toString());
                out.print(s + ": ");
                exc.printStackTrace(out);
            }
        } else {
            if (url != originalURL) {
                sb.append(" ");
                sb.append(op);
                sb.append(" --> ");
                sb.append(url);
            }
            out.println(sb.toString());
        }
    }

    static void show(String n, String v) {
        out.println("  " + n + "          = ".substring(n.length()) + v);
    }

    public static void show(URL u) {
        show("scheme", u.getProtocol());
        show("authority", u.getAuthority());
        show("userInfo", u.getUserInfo());
        show("host", u.getHost());
        show("port", "" + u.getPort());
        show("path", u.getPath());
        show("query", u.getQuery());
        show("ref", u.getRef());
    }

    private void report() {
        summarize();
        if (failed == 0) return;
        StringBuffer sb = new StringBuffer();
        sb.append("FAIL:");
        if ((failed & PARSEFAIL) != 0) sb.append(" parsefail");
        if ((failed & PROTOCOL) != 0) sb.append(" scheme");
        if ((failed & USERINFO) != 0) sb.append(" userinfo");
        if ((failed & HOST) != 0) sb.append(" host");
        if ((failed & PORT) != 0) sb.append(" port");
        if ((failed & PATH) != 0) sb.append(" path");
        if ((failed & QUERY) != 0) sb.append(" query");
        if ((failed & REF) != 0) sb.append(" fragment");
        out.println(sb.toString());
        if (url != null) show(url);
        throw new RuntimeException("Test failed");
    }

    private static boolean hasFtp() {
        try {
            return new java.net.URL("ftp://") != null;
        } catch (java.net.MalformedURLException x) {
            System.out.println("FTP not supported by this runtime.");
            return false;
        }
    }

    // -- Tests --

    static void rfc2396() {


        header("RFC2396: Basic examples");

        if (hasFtp())
            test("ftp://ftp.is.co.za/rfc/rfc1808.txt")
                .s("ftp").h("ftp.is.co.za").p("/rfc/rfc1808.txt").z();

        test("http://www.math.uio.no/faq/compression-faq/part1.html")
            .s("http").h("www.math.uio.no").p("/faq/compression-faq/part1.html").z();

        test("http://www.w3.org/Addressing/")
            .s("http").h("www.w3.org").p("/Addressing/").z();

        if (hasFtp())
            test("ftp://ds.internic.net/rfc/")
                .s("ftp").h("ds.internic.net").p("/rfc/").z();

        test("http://www.ics.uci.edu/pub/ietf/url/historical.html#WARNING")
            .s("http").h("www.ics.uci.edu").p("/pub/ietf/url/historical.html")
            .f("WARNING").z();

        test("http://www.ics.uci.edu/pub/ietf/url/#Related")
            .s("http").h("www.ics.uci.edu").p("/pub/ietf/url/")
            .f("Related").z();

        test("file:/home/someone/dir1/dir2/file").s("file").h("").p("/home/someone/dir1/dir2/file").z();

        header("RFC2396: Normal relative-URL examples (appendix C)");

        URL base = (test("http://a/b/c/d;p?q")
                    .s("http").h("a").p("/b/c/d;p").q("q").z().url());

        // g:h       g:h
        // test(base, "http:h").s("g").p("h").z();

        // g         http://a/b/c/g
        test(base, "g").s("http").h("a").p("/b/c/g").z();

        // ./g       http://a/b/c/g
        test(base, "./g").s("http").h("a").p("/b/c/g").z();

        // g/        http://a/b/c/g/
        test(base, "g/").s("http").h("a").p("/b/c/g/").z();

        // /g        http://a/g
        test(base, "/g").s("http").h("a").p("/g").z();

        // //g       http://g
        test(base,"//g").s("http").h("g").p("").z();

        // ?y        http://a/b/c/?y
        test(base, "?y").s("http").h("a").p("/b/c/").q("y").z();

        // g?y       http://a/b/c/g?y
        test(base, "g?y").s("http").h("a").p("/b/c/g").q("y").z();

        // #s        (current document)#s
        // DEVIATION: Lone fragment parses as relative URL with empty path,
        // and resolves without removing the last segment of the base path.
        // test(base,"#s").s("http").h("a").p("/b/c/d;p").f("s").z();
        test(base,"#s").s("http").h("a").p("/b/c/d;p").q("q").f("s").z();

        // g#s       http://a/b/c/g#s
        test(base, "g#s").s("http").h("a").p("/b/c/g").f("s").z();

        // g?y#s     http://a/b/c/g?y#s
        test(base,"g?y#s").s("http").h("a").p("/b/c/g").q("y").f("s").z();

        // ;x        http://a/b/c/;x
        test(base,";x").s("http").h("a").p("/b/c/;x").z();

        // g;x       http://a/b/c/g;x
        test(base,"g;x").s("http").h("a").p("/b/c/g;x").z();

        // g;x?y#s   http://a/b/c/g;x?y#s
        test(base,"g;x?y#s").s("http").h("a").p("/b/c/g;x").q("y").f("s").z();

        // .         http://a/b/c/
        test(base,".").s("http").h("a").p("/b/c/").z();

        // ./        http://a/b/c/
        test(base,"./").s("http").h("a").p("/b/c/").z();

        // ..        http://a/b/
        test(base,"..").s("http").h("a").p("/b/").z();

        // ../       http://a/b/
        test(base,"../").s("http").h("a").p("/b/").z();

        // ../g      http://a/b/g
        test(base,"../g").s("http").h("a").p("/b/g").z();

        // ../..     http://a/
        test(base,"../..").s("http").h("a").p("/").z();

        // ../../    http://a/
        test(base,"../../").s("http").h("a").p("/").z();

        // ../../g   http://a/g
        test(base,"../../g").s("http").h("a").p("/g").z();


        // http://u@s1/p1 http://s2/p2
        test(test("http://u:p@s1/p1").url(),"http://s2/p2")
             .s("http").h("s2").u(null).p("/p2").z();

        header("RFC2396: Abnormal relative-URL examples (appendix C)");

        // ../../../g    =  http://a/../g
        test(base,"../../../g").s("http").h("a").p("/../g").z();

        // ../../../../g =  http://a/../../g
        test(base, "../../../../g").s("http").h("a").p("/../../g").z();


        // /./g          =  http://a/./g
        test(base,"/./g").s("http").h("a").p("/./g").z();

        // /../g         =  http://a/../g
        test(base,"/../g").s("http").h("a").p("/../g").z();

        // g.            =  http://a/b/c/g.
        test(base,"g.").s("http").h("a").p("/b/c/g.").z();

        // .g            =  http://a/b/c/.g
        test(base,".g").s("http").h("a").p("/b/c/.g").z();

        // g..           =  http://a/b/c/g..
        test(base,"g..").s("http").h("a").p("/b/c/g..").z();

        // ..g           =  http://a/b/c/..g
        test(base,"..g").s("http").h("a").p("/b/c/..g").z();

        // ./../g        =  http://a/b/g
        test(base,"./../g").s("http").h("a").p("/b/g").z();

        // ./g/.         =  http://a/b/c/g/
        test(base,"./g/.").s("http").h("a").p("/b/c/g/").z();

        // g/./h         =  http://a/b/c/g/h
        test(base,"g/./h").s("http").h("a").p("/b/c/g/h").z();

        // g/../h        =  http://a/b/c/h
        test(base,"g/../h").s("http").h("a").p("/b/c/h").z();

        // g;x=1/./y     =  http://a/b/c/g;x=1/y
        test(base,"g;x=1/./y").s("http").h("a").p("/b/c/g;x=1/y").z();

        // g;x=1/../y    =  http://a/b/c/y
        test(base,"g;x=1/../y").s("http").h("a").p("/b/c/y").z();

        // g?y/./x       =  http://a/b/c/g?y/./x
        test(base,"g?y/./x").s("http").h("a").p("/b/c/g").q("y/./x").z();

        // g?y/../x      =  http://a/b/c/g?y/../x
        test(base,"g?y/../x").s("http").h("a").p("/b/c/g").q("y/../x").z();

        // g#s/./x       =  http://a/b/c/g#s/./x
        test(base,"g#s/./x").s("http").h("a").p("/b/c/g").f("s/./x").z();

        // g#s/../x      =  http://a/b/c/g#s/../x
        test(base,"g#s/../x").s("http").h("a").p("/b/c/g").f("s/../x").z();

        // http:g        =  http:g
        // test(base,"http:g").s("http").p("g").z();

    }


    static void ip() {

        header("IP addresses");

        test("http://1.2.3.4:5")
            .s("http").h("1.2.3.4").n(5).p("").z();

        // From RFC2732

        test("http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]:80/index.html")
            .s("http").h("[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]")
            .n(80).p("/index.html").z();

        test("http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210%12]:80/index.html")
            .s("http").h("[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210%12]")
            .n(80).p("/index.html").z();

        test("http://[1080:0:0:0:8:800:200C:417A]/index.html")
            .s("http").h("[1080:0:0:0:8:800:200C:417A]").p("/index.html").z();

        test("http://[1080:0:0:0:8:800:200C:417A%1]/index.html")
            .s("http").h("[1080:0:0:0:8:800:200C:417A%1]").p("/index.html").z();

        test("http://[3ffe:2a00:100:7031::1]")
            .s("http").h("[3ffe:2a00:100:7031::1]").p("").z();

        test("http://[1080::8:800:200C:417A]/foo")
            .s("http").h("[1080::8:800:200C:417A]").p("/foo").z();

        test("http://[::192.9.5.5]/ipng")
            .s("http").h("[::192.9.5.5]").p("/ipng").z();

        test("http://[::192.9.5.5%interface]/ipng")
            .s("http").h("[::192.9.5.5%interface]").p("/ipng").z();

        test("http://[::FFFF:129.144.52.38]:80/index.html")
            .s("http").h("[::FFFF:129.144.52.38]").n(80).p("/index.html").z();

        test("http://[2010:836B:4179::836B:4179]")
            .s("http").h("[2010:836B:4179::836B:4179]").p("").z();

        // From RFC2373

        test("http://[FF01::101]")
            .s("http").h("[FF01::101]").p("").z();

        test("http://[::1]")
            .s("http").h("[::1]").p("").z();

        test("http://[::]")
            .s("http").h("[::]").p("").z();

        test("http://[::%hme0]")
            .s("http").h("[::%hme0]").p("").z();

        test("http://[0:0:0:0:0:0:13.1.68.3]")
            .s("http").h("[0:0:0:0:0:0:13.1.68.3]").p("").z();

        test("http://[0:0:0:0:0:FFFF:129.144.52.38]")
            .s("http").h("[0:0:0:0:0:FFFF:129.144.52.38]").p("").z();

        test("http://[0:0:0:0:0:FFFF:129.144.52.38%33]")
            .s("http").h("[0:0:0:0:0:FFFF:129.144.52.38%33]").p("").z();

        test("http://[::13.1.68.3]")
            .s("http").h("[::13.1.68.3]").p("").z();

        test("http://[::13.1.68.3]:")
            .s("http").h("[::13.1.68.3]").p("").z();
        // Error cases

        test("http://[ff01:234/foo").x().z();
        test("http://[ff01:234:zzz]/foo").x().z();
        test("http://[foo]").x().z();
        test("http://[]").x().z();
        test("http://[129.33.44.55]").x().z();
        test("http://[ff:ee:dd::cc:bb::aa:9:8]").x().z();
        test("http://[1:2:3:4:5:6:7:8%]").x().z();
        test("http://[1:2:3:4:5:6:7:8%!/]").x().z();
        test("http://[1:2:3:4:5:6:7:8:9]").x().z();
        test("http://[::1.2.3.300]").x().z();
        test("http://[1.2.3.4:5]").x().z();

        // Optional IPv6 brackets in constructors
        test("http", "1:2:3:4:5:6:7:8", -1, "")
            .s("http").h("[1:2:3:4:5:6:7:8]").p("").z();
        test("http", "1:2:3:4:5:6:7:8%hme0", -1, "")
            .s("http").h("[1:2:3:4:5:6:7:8%hme0]").p("").z();
        test("http", "[1:2:3:4:5:6:7:8]", -1, "")
            .s("http").h("[1:2:3:4:5:6:7:8]").p("").z();

    }

    static void serial() throws IOException, MalformedURLException {

        header("Serialization");

        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(bo);
        URL u = new URL("http://java.sun.com/jdk/1.4?release#beta");
        oo.writeObject(u);
        oo.close();

        ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
        ObjectInputStream oi = new ObjectInputStream(bi);
        try {
            Object o = oi.readObject();
            u.equals(o);
        } catch (ClassNotFoundException x) {
            x.printStackTrace();
            throw new RuntimeException(x.toString());
        }

    }

    static void tests() throws IOException, MalformedURLException {
        rfc2396();
        ip();
        serial();
    }


    // -- Command-line invocation --

    static void usage() {
        out.println("Usage:");
        out.println("  java Test               --  Runs all tests in this file");
        out.println("  java Test <url>         --  Parses url, shows components");
        out.println("  java Test <base> <url>  --  Parses url and base, then resolves");
        out.println("                              url against base");
    }

    public static void main(String[] args) throws Exception {
        switch (args.length) {

        case 0:
            tests();
            out.println();
            out.println("Test cases: " + testCount);
            break;

        case 1:
            if (args[0].equals("-help")) {
                usage();
                break;
            }
            // clargs(null, args[0]);
            break;

        case 2:
            // clargs(args[0], args[1]);
            break;

        default:
            usage();
            break;

        }
    }

}
