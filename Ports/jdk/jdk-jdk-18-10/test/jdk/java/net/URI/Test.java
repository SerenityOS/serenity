/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Unit test for java.net.URI
 * @bug 4464135 4505046 4503239 4438319 4991359 4866303 7023363 7041800
 *      7171415 6933879
 * @author Mark Reinhold
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.MalformedURLException;


public class Test {

    static PrintStream out = System.out;
    static int testCount = 0;

    // Properties that we check
    static final int PARSEFAIL   = 1 << 0;
    static final int SCHEME      = 1 << 1;
    static final int SSP         = 1 << 2;
    static final int SSP_D       = 1 << 3;      // Decoded form
    static final int OPAQUEPART  = 1 << 4;      // SSP, and URI is opaque
    static final int USERINFO    = 1 << 5;
    static final int USERINFO_D  = 1 << 6;      // Decoded form
    static final int HOST        = 1 << 7;
    static final int PORT        = 1 << 8;
    static final int REGISTRY    = 1 << 9;
    static final int REGISTRY_D  = 1 << 10;     // Decoded form
    static final int PATH        = 1 << 11;
    static final int PATH_D      = 1 << 12;     // Decoded form
    static final int QUERY       = 1 << 13;
    static final int QUERY_D     = 1 << 14;     // Decoded form
    static final int FRAGMENT    = 1 << 15;
    static final int FRAGMENT_D  = 1 << 16;     // Decoded form
    static final int TOASCII     = 1 << 17;
    static final int IDENT_STR   = 1 << 18;     // Identities
    static final int IDENT_URI1  = 1 << 19;
    static final int IDENT_URI3  = 1 << 20;
    static final int IDENT_URI5  = 1 << 21;
    static final int IDENT_URI7  = 1 << 22;
    static final int TOSTRING    = 1 << 23;

    String input;
    URI uri = null;
    URI originalURI;
    URI base = null;                    // Base for resolution/relativization
    String op = null;                   // Op performed if uri != originalURI
    int checked = 0;                    // Mask for checked properties
    int failed = 0;                     // Mask for failed properties
    Exception exc = null;

    private Test(String s) {
        testCount++;
        input = s;
        try {
            uri = new URI(s);
        } catch (URISyntaxException x) {
            exc = x;
        }
        originalURI = uri;
    }

    static Test test(String s) {
        return new Test(s);
    }

    private Test(String s, String u, String h, int n,
                 String p, String q, String f)
    {
        testCount++;
        try {
            uri = new URI(s, u, h, n, p, q, f);
        } catch (URISyntaxException x) {
            exc = x;
            input = x.getInput();
        }
        if (uri != null)
            input = uri.toString();
        originalURI = uri;
    }

    static Test test(String s, String u, String h, int n,
                     String p, String q, String f) {
        return new Test(s, u, h, n, p, q, f);
    }

    private Test(String s, String a,
                 String p, String q, String f)
    {
        testCount++;
        try {
            uri = new URI(s, a, p, q, f);
        } catch (URISyntaxException x) {
            exc = x;
            input = x.getInput();
        }
        if (uri != null)
            input = uri.toString();
        originalURI = uri;
    }

    static Test test(String s, String a,
                     String p, String q, String f) {
        return new Test(s, a, p, q, f);
    }

    private Test(String s, String h, String p, String f) {
        testCount++;
        try {
            uri = new URI(s, h, p, f);
        } catch (URISyntaxException x) {
            exc = x;
            input = x.getInput();
        }
        if (uri != null)
            input = uri.toString();
        originalURI = uri;
    }

    static Test test(String s, String h, String p, String f) {
        return new Test(s, h, p, f);
    }

    private Test(String s, String ssp, String f) {
        testCount++;
        try {
            uri = new URI(s, ssp, f);
        } catch (URISyntaxException x) {
            exc = x;
            input = x.getInput();
        }
        if (uri != null)
            input = uri.toString();
        originalURI = uri;
    }

    static Test test(String s, String ssp, String f) {
        return new Test(s, ssp, f);
    }

    private Test(String s, boolean xxx) {
        testCount++;
        try {
            uri = URI.create(s);
        } catch (IllegalArgumentException x) {
            exc = x;
        }
        if (uri != null)
            input = uri.toString();
        originalURI = uri;
    }

    static Test testCreate(String s) {
        return new Test(s, false);
    }

    boolean parsed() {
        return uri != null;
    }

    boolean resolved() {
        return base != null;
    }

    URI uri() {
        return uri;
    }


    // Operations on Test instances
    //
    // These are short so as to make test cases compact.
    //
    //    s      Scheme
    //    sp     Scheme-specific part
    //    spd    Scheme-specific part, decoded
    //    o      Opaque part (isOpaque() && ssp matches)
    //    g      reGistry (authority matches, and host is not defined)
    //    gd     reGistry, decoded
    //    u      User info
    //    ud     User info, decoded
    //    h      Host
    //    n      port Number
    //    p      Path
    //    pd     Path, decoded
    //    q      Query
    //    qd     Query, decoded
    //    f      Fragment
    //    fd     Fragment, decoded
    //
    //    rslv   Resolve against given base
    //    rtvz   Relativize
    //    psa    Parse server Authority
    //    norm   Normalize
    //    ta     ASCII form
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
        if ((s == null) || !s.equals(ans))
            failed |= prop;
    }

    Test s(String s) {
        if (check1(SCHEME)) check2(uri.getScheme(), s, SCHEME);
        return this;
    }

    Test u(String s) {
        if (check1(USERINFO)) check2(uri.getRawUserInfo(), s, USERINFO);
        return this;
    }

    Test ud(String s) {
        if (check1(USERINFO_D)) {
            check2(uri.getUserInfo(), s, USERINFO_D);
        }
        return this;
    }

    Test h(String s) {
        if (check1(HOST)) check2(uri.getHost(), s, HOST);
        return this;
    }

    Test g(String s) {
        if (check1(REGISTRY)) {
            if (uri.getHost() != null)
                failed |= REGISTRY;
            else
                check2(uri.getRawAuthority(), s, REGISTRY);
        }
        return this;
    }

    Test gd(String s) {
        if (check1(REGISTRY_D)) {
            if (uri.getHost() != null)
                failed |= REGISTRY_D;
            else
                check2(uri.getAuthority(), s, REGISTRY_D);
        }
        return this;
    }

    Test n(int n) {
        checked |= PORT;
        if (!parsed() || (uri.getPort() != n))
            failed |= PORT;
        return this;
    }

    Test p(String s) {
        if (check1(PATH)) check2(uri.getRawPath(), s, PATH);
        return this;
    }

    Test pd(String s) {
        if (check1(PATH_D)) check2(uri.getPath(), s, PATH_D);
        return this;
    }

    Test o(String s) {
        if (check1(OPAQUEPART)) {
            if (!uri.isOpaque())
                failed |= OPAQUEPART;
            else
                check2(uri.getSchemeSpecificPart(), s, OPAQUEPART);
        }
        return this;
    }

    Test sp(String s) {
        if (check1(SSP)) check2(uri.getRawSchemeSpecificPart(), s, SSP);
        return this;
    }

    Test spd(String s) {
        if (check1(SSP_D)) check2(uri.getSchemeSpecificPart(), s, SSP_D);
        return this;
    }

    Test q(String s) {
        if (check1(QUERY)) check2(uri.getRawQuery(), s, QUERY);
        return this;
    }

    Test qd(String s) {
        if (check1(QUERY_D)) check2(uri.getQuery(), s, QUERY_D);
        return this;
    }

    Test f(String s) {
        if (check1(FRAGMENT)) check2(uri.getRawFragment(), s, FRAGMENT);
        return this;
    }

    Test fd(String s) {
        if (check1(FRAGMENT_D)) check2(uri.getFragment(), s, FRAGMENT_D);
        return this;
    }

    Test ta(String s) {
        if (check1(TOASCII))
            check2(uri.toASCIIString(), s, TOASCII);
        return this;
    }

    Test ts(String s) {
        if (check1(TOSTRING))
            check2(uri.toString(), s, TOSTRING);
        return this;
    }

    Test x() {
        checked |= PARSEFAIL;
        if (parsed())
            failed |= PARSEFAIL;
        return this;
    }

    Test rslv(URI base) {
        if (!parsed())
            return this;
        this.base = base;
        op = "rslv";
        URI u = uri;
        uri = null;
        try {
            this.uri = base.resolve(u);
        } catch (IllegalArgumentException x) {
            exc = x;
        }
        checked = 0;
        failed = 0;
        return this;
    }

    Test norm() {
        if (!parsed())
            return this;
        op = "norm";
        uri = uri.normalize();
        return this;
    }

    Test rtvz(URI base) {
        if (!parsed())
            return this;
        this.base = base;
        op = "rtvz";
        uri = base.relativize(uri);
        checked = 0;
        failed = 0;
        return this;
    }

    Test psa() {
        try {
            uri.parseServerAuthority();
        } catch (URISyntaxException x) {
            exc = x;
            uri = null;
        }
        checked = 0;
        failed = 0;
        return this;
    }

    private void checkEmpty(String s, int prop) {
        if (((checked & prop) == 0) && (s != null))
            failed |= prop;
    }

    // Check identity for the seven-argument URI constructor
    //
    void checkURI7() {
        // Only works on hierarchical URIs
        if (uri.isOpaque())
            return;
        // Only works with server-based authorities
        if ((uri.getAuthority() == null)
            != ((uri.getUserInfo() == null) && (uri.getHost() == null)))
            return;
        // Not true if non-US-ASCII chars are encoded unnecessarily
        if (uri.getPath().indexOf('\u20AC') >= 0)
            return;
        try {
            URI u2 = new URI(uri.getScheme(), uri.getUserInfo(),
                             uri.getHost(), uri.getPort(), uri.getPath(),
                             uri.getQuery(), uri.getFragment());
            if (!uri.equals(u2))
                failed |= IDENT_URI7;
        } catch (URISyntaxException x) {
            failed |= IDENT_URI7;
        }
    }

    // Check identity for the five-argument URI constructor
    //
    void checkURI5() {
        // Only works on hierarchical URIs
        if (uri.isOpaque())
            return;
        try {
            URI u2 = new URI(uri.getScheme(), uri.getAuthority(),
                             uri.getPath(), uri.getQuery(), uri.getFragment());
            if (!uri.equals(u2))
                failed |= IDENT_URI5;
        } catch (URISyntaxException x) {
            failed |= IDENT_URI5;
        }
    }

    // Check identity for the three-argument URI constructor
    //
    void checkURI3() {
        try {
            URI u2 = new URI(uri.getScheme(),
                             uri.getSchemeSpecificPart(),
                             uri.getFragment());
            if (!uri.equals(u2))
                failed |= IDENT_URI3;
        } catch (URISyntaxException x) {
            failed |= IDENT_URI3;
        }
    }

    // Check all identities mentioned in the URI class specification
    //
    void checkIdentities() {
        if (input != null) {
            if (!uri.toString().equals(input))
                failed |= IDENT_STR;
        }
        try {
            if (!(new URI(uri.toString())).equals(uri))
                failed |= IDENT_URI1;
        } catch (URISyntaxException x) {
            failed |= IDENT_URI1;
        }

        // Remaining identities fail if "//" given but authority is undefined
        if ((uri.getAuthority() == null)
            && (uri.getSchemeSpecificPart() != null)
            && (uri.getSchemeSpecificPart().startsWith("///")
                || uri.getSchemeSpecificPart().startsWith("//?")
                || uri.getSchemeSpecificPart().equals("//")))
            return;

        // Remaining identities fail if ":" given but port is undefined
        if ((uri.getHost() != null)
            && (uri.getAuthority() != null)
            && (uri.getAuthority().equals(uri.getHost() + ":")))
            return;

        // Remaining identities fail if non-US-ASCII chars are encoded
        // unnecessarily
        if ((uri.getPath() != null) && uri.getPath().indexOf('\u20AC') >= 0)
            return;

        checkURI3();
        checkURI5();
        checkURI7();
    }

    // Check identities, check that unchecked component properties are not
    // defined, and report any failures
    //
    Test z() {
        if (!parsed()) {
            report();
            return this;
        }

        if (op == null)
            checkIdentities();

        // Check that unchecked components are undefined
        checkEmpty(uri.getScheme(), SCHEME);
        checkEmpty(uri.getUserInfo(), USERINFO);
        checkEmpty(uri.getHost(), HOST);
        if (((checked & PORT) == 0) && (uri.getPort() != -1)) failed |= PORT;
        checkEmpty(uri.getPath(), PATH);
        checkEmpty(uri.getQuery(), QUERY);
        checkEmpty(uri.getFragment(), FRAGMENT);

        // Report failures
        report();
        return this;
    }


    // Summarization and reporting

    static void header(String s) {
        out.println();
        out.println();
        out.println("-- " + s + " --");
    }

    static void show(String prefix, URISyntaxException x) {
        out.println(uquote(x.getInput()));
        if (x.getIndex() >= 0) {
            for (int i = 0; i < x.getIndex(); i++) {
                if (x.getInput().charAt(i) >= '\u0080')
                    out.print("      ");        // Skip over \u1234
                else
                    out.print(" ");
            }
            out.println("^");
        }
        out.println(prefix + ": " + x.getReason());
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
            if (exc instanceof URISyntaxException)
                show(s, (URISyntaxException)exc);
            else {
                out.println(uquote(sb.toString()));
                out.print(s + ": ");
                exc.printStackTrace(out);
            }
        } else {
            if (uri != originalURI) {
                sb.append(" ");
                sb.append(op);
                sb.append(" --> ");
                sb.append(uri);
            }
            out.println(uquote(sb.toString()));
        }
    }

    public static String uquote(String str) {
        if (str == null)
            return str;
        StringBuffer sb = new StringBuffer();
        int n = str.length();
        for (int i = 0; i < n; i++) {
            char c = str.charAt(i);
            if ((c >= ' ') && (c < 0x7f)) {
                sb.append(c);
                continue;
            }
            sb.append("\\u");
            String s = Integer.toHexString(c).toUpperCase();
            while (s.length() < 4)
                s = "0" + s;
            sb.append(s);
        }
        return sb.toString();
    }

    static void show(String n, String v) {
        out.println("  " + n
                    + "          = ".substring(n.length())
                    + uquote(v));
    }

    static void show(String n, String v, String vd) {
        if ((v == null) || v.equals(vd))
            show(n, v);
        else {
            out.println("  " + n
                        + "          = ".substring(n.length())
                        + uquote(v)
                        + " = " + uquote(vd));
        }
    }

    public static void show(URI u) {
        show("opaque", "" + u.isOpaque());
        show("scheme", u.getScheme());
        show("ssp", u.getRawSchemeSpecificPart(), u.getSchemeSpecificPart());
        show("authority", u.getRawAuthority(), u.getAuthority());
        show("userinfo", u.getRawUserInfo(), u.getUserInfo());
        show("host", u.getHost());
        show("port", "" + u.getPort());
        show("path", u.getRawPath(), u.getPath());
        show("query", u.getRawQuery(), u.getQuery());
        show("fragment", u.getRawFragment(), u.getFragment());
        if (!u.toString().equals(u.toASCIIString()))
            show("toascii", u.toASCIIString());
    }

    private void report() {
        summarize();
        if (failed == 0) return;
        StringBuffer sb = new StringBuffer();
        sb.append("FAIL:");
        if ((failed & PARSEFAIL) != 0) sb.append(" parsefail");
        if ((failed & SCHEME) != 0) sb.append(" scheme");
        if ((failed & SSP) != 0) sb.append(" ssp");
        if ((failed & OPAQUEPART) != 0) sb.append(" opaquepart");
        if ((failed & USERINFO) != 0) sb.append(" userinfo");
        if ((failed & USERINFO_D) != 0) sb.append(" userinfod");
        if ((failed & HOST) != 0) sb.append(" host");
        if ((failed & PORT) != 0) sb.append(" port");
        if ((failed & REGISTRY) != 0) sb.append(" registry");
        if ((failed & PATH) != 0) sb.append(" path");
        if ((failed & PATH_D) != 0) sb.append(" pathd");
        if ((failed & QUERY) != 0) sb.append(" query");
        if ((failed & QUERY_D) != 0) sb.append(" queryd");
        if ((failed & FRAGMENT) != 0) sb.append(" fragment");
        if ((failed & FRAGMENT_D) != 0) sb.append(" fragmentd");
        if ((failed & TOASCII) != 0) sb.append(" toascii");
        if ((failed & IDENT_STR) != 0) sb.append(" ident-str");
        if ((failed & IDENT_URI1) != 0) sb.append(" ident-uri1");
        if ((failed & IDENT_URI3) != 0) sb.append(" ident-uri3");
        if ((failed & IDENT_URI5) != 0) sb.append(" ident-uri5");
        if ((failed & IDENT_URI7) != 0) sb.append(" ident-uri7");
        if ((failed & TOSTRING) != 0) sb.append(" tostring");
        out.println(sb.toString());
        if (uri != null) show(uri);
        throw new RuntimeException("Test failed");
    }



    // -- Tests --

    static void rfc2396() {


        header("RFC2396: Basic examples");

        test("ftp://ftp.is.co.za/rfc/rfc1808.txt")
            .s("ftp").h("ftp.is.co.za").p("/rfc/rfc1808.txt").z();

        test("http://www.math.uio.no/faq/compression-faq/part1.html")
            .s("http").h("www.math.uio.no").p("/faq/compression-faq/part1.html").z();

        test("mailto:mduerst@ifi.unizh.ch")
            .s("mailto").o("mduerst@ifi.unizh.ch").z();

        test("news:comp.infosystems.www.servers.unix")
            .s("news").o("comp.infosystems.www.servers.unix").z();

        test("telnet://melvyl.ucop.edu/")
            .s("telnet").h("melvyl.ucop.edu").p("/").z();

        test("http://www.w3.org/Addressing/")
            .s("http").h("www.w3.org").p("/Addressing/").z();

        test("ftp://ds.internic.net/rfc/")
            .s("ftp").h("ds.internic.net").p("/rfc/").z();

        test("http://www.ics.uci.edu/pub/ietf/uri/historical.html#WARNING")
            .s("http").h("www.ics.uci.edu").p("/pub/ietf/uri/historical.html")
            .f("WARNING").z();

        test("http://www.ics.uci.edu/pub/ietf/uri/#Related")
            .s("http").h("www.ics.uci.edu").p("/pub/ietf/uri/")
            .f("Related").z();


        header("RFC2396: Normal relative-URI examples (appendix C)");

        URI base = (test("http://a/b/c/d;p?q")
                    .s("http").h("a").p("/b/c/d;p").q("q").z().uri());

        // g:h       g:h
        test("g:h")
            .s("g").o("h").z()
            .rslv(base).s("g").o("h").z();

        // g         http://a/b/c/g
        test("g")
            .p("g").z()
            .rslv(base).s("http").h("a").p("/b/c/g").z();

        // ./g       http://a/b/c/g
        test("./g")
            .p("./g").z()
            .rslv(base).s("http").h("a").p("/b/c/g").z();

        // g/        http://a/b/c/g/
        test("g/")
            .p("g/").z()
            .rslv(base).s("http").h("a").p("/b/c/g/").z();

        // /g        http://a/g
        test("/g")
            .p("/g").z()
            .rslv(base).s("http").h("a").p("/g").z();

        // //g       http://g
        test("//g")
            .h("g").p("").z()
            .rslv(base).s("http").h("g").p("").z();

        // ?y        http://a/b/c/?y
        test("?y")
            .p("").q("y").z()
            .rslv(base).s("http").h("a").p("/b/c/").q("y").z();

        // g?y       http://a/b/c/g?y
        test("g?y")
            .p("g").q("y").z()
            .rslv(base).s("http").h("a").p("/b/c/g").q("y").z();

        // #s        (current document)#s
        // DEVIATION: Lone fragment parses as relative URI with empty path
        test("#s")
            .p("").f("s").z()
            .rslv(base).s("http").h("a").p("/b/c/d;p").f("s").q("q").z();

        // g#s       http://a/b/c/g#s
        test("g#s")
            .p("g").f("s").z()
            .rslv(base).s("http").h("a").p("/b/c/g").f("s").z();

        // g?y#s     http://a/b/c/g?y#s
        test("g?y#s")
            .p("g").q("y").f("s").z()
            .rslv(base).s("http").h("a").p("/b/c/g").q("y").f("s").z();

        // ;x        http://a/b/c/;x
        test(";x")
            .p(";x").z()
            .rslv(base).s("http").h("a").p("/b/c/;x").z();

        // g;x       http://a/b/c/g;x
        test("g;x")
            .p("g;x").z()
            .rslv(base).s("http").h("a").p("/b/c/g;x").z();

        // g;x?y#s   http://a/b/c/g;x?y#s
        test("g;x?y#s")
            .p("g;x").q("y").f("s").z()
            .rslv(base).s("http").h("a").p("/b/c/g;x").q("y").f("s").z();

        // .         http://a/b/c/
        test(".")
            .p(".").z()
            .rslv(base).s("http").h("a").p("/b/c/").z();

        // ./        http://a/b/c/
        test("./")
            .p("./").z()
            .rslv(base).s("http").h("a").p("/b/c/").z();

        // ..        http://a/b/
        test("..")
            .p("..").z()
            .rslv(base).s("http").h("a").p("/b/").z();

        // ../       http://a/b/
        test("../")
            .p("../").z()
            .rslv(base).s("http").h("a").p("/b/").z();

        // ../g      http://a/b/g
        test("../g")
            .p("../g").z()
            .rslv(base).s("http").h("a").p("/b/g").z();

        // ../..     http://a/
        test("../..")
            .p("../..").z()
            .rslv(base).s("http").h("a").p("/").z();

        // ../../    http://a/
        test("../../")
            .p("../../").z()
            .rslv(base).s("http").h("a").p("/").z();

        // ../../g   http://a/g
        test("../../g")
            .p("../../g").z()
            .rslv(base).s("http").h("a").p("/g").z();


        header("RFC2396: Abnormal relative-URI examples (appendix C)");

        // ../../../g    =  http://a/../g
        test("../../../g")
            .p("../../../g").z()
            .rslv(base).s("http").h("a").p("/../g").z();

        // ../../../../g =  http://a/../../g
        test("../../../../g")
            .p("../../../../g").z()
            .rslv(base).s("http").h("a").p("/../../g").z();


        // /./g          =  http://a/./g
        test("/./g")
            .p("/./g").z()
            .rslv(base).s("http").h("a").p("/./g").z();

        // /../g         =  http://a/../g
        test("/../g")
            .p("/../g").z()
            .rslv(base).s("http").h("a").p("/../g").z();

        // g.            =  http://a/b/c/g.
        test("g.")
            .p("g.").z()
            .rslv(base).s("http").h("a").p("/b/c/g.").z();

        // .g            =  http://a/b/c/.g
        test(".g")
            .p(".g").z()
            .rslv(base).s("http").h("a").p("/b/c/.g").z();

        // g..           =  http://a/b/c/g..
        test("g..")
            .p("g..").z()
            .rslv(base).s("http").h("a").p("/b/c/g..").z();

        // ..g           =  http://a/b/c/..g
        test("..g")
            .p("..g").z()
            .rslv(base).s("http").h("a").p("/b/c/..g").z();

        // ./../g        =  http://a/b/g
        test("./../g")
            .p("./../g").z()
            .rslv(base).s("http").h("a").p("/b/g").z();

        // ./g/.         =  http://a/b/c/g/
        test("./g/.")
            .p("./g/.").z()
            .rslv(base).s("http").h("a").p("/b/c/g/").z();

        // g/./h         =  http://a/b/c/g/h
        test("g/./h")
            .p("g/./h").z()
            .rslv(base).s("http").h("a").p("/b/c/g/h").z();

        // g/../h        =  http://a/b/c/h
        test("g/../h")
            .p("g/../h").z()
            .rslv(base).s("http").h("a").p("/b/c/h").z();

        // g;x=1/./y     =  http://a/b/c/g;x=1/y
        test("g;x=1/./y")
            .p("g;x=1/./y").z()
            .rslv(base).s("http").h("a").p("/b/c/g;x=1/y").z();

        // g;x=1/../y    =  http://a/b/c/y
        test("g;x=1/../y")
            .p("g;x=1/../y").z()
            .rslv(base).s("http").h("a").p("/b/c/y").z();

        // g?y/./x       =  http://a/b/c/g?y/./x
        test("g?y/./x")
            .p("g").q("y/./x").z()
            .rslv(base).s("http").h("a").p("/b/c/g").q("y/./x").z();

        // g?y/../x      =  http://a/b/c/g?y/../x
        test("g?y/../x")
            .p("g").q("y/../x").z()
            .rslv(base).s("http").h("a").p("/b/c/g").q("y/../x").z();

        // g#s/./x       =  http://a/b/c/g#s/./x
        test("g#s/./x")
            .p("g").f("s/./x").z()
            .rslv(base).s("http").h("a").p("/b/c/g").f("s/./x").z();

        // g#s/../x      =  http://a/b/c/g#s/../x
        test("g#s/../x")
            .p("g").f("s/../x").z()
            .rslv(base).s("http").h("a").p("/b/c/g").f("s/../x").z();

        // http:g        =  http:g
        test("http:g")
            .s("http").o("g").z()
            .rslv(base).s("http").o("g").z();

    }


    static void ip() {

        header("IP addresses");

        test("http://1.2.3.4:5")
            .s("http").h("1.2.3.4").n(5).p("").z();

        // From RFC2732

        test("http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]:80/index.html")
            .s("http").h("[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]")
            .n(80).p("/index.html").z();

        test("http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:10%12]:80/index.html")
            .s("http").h("[FEDC:BA98:7654:3210:FEDC:BA98:7654:10%12]")
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

        test("http://[0:0:0:0:0:ffff:1.2.3.4]")
            .s("http").h("[0:0:0:0:0:ffff:1.2.3.4]").p("").z();

        test("http://[::13.1.68.3]")
            .s("http").h("[::13.1.68.3]").p("").z();

        // Optional IPv6 brackets in constructors

        test("s", null, "1:2:3:4:5:6:7:8", -1, null, null, null)
            .s("s").h("[1:2:3:4:5:6:7:8]").p("").z();

        test("s", null, "[1:2:3:4:5:6:7:8]", -1, null, null, null)
            .s("s").h("[1:2:3:4:5:6:7:8]").p("").z();

        test("s", null, "[1:2:3:4:5:6:7:8]", -1, null, null, null)
            .s("s").h("[1:2:3:4:5:6:7:8]").p("").z();

        test("s", "1:2:3:4:5:6:7:8", null, null)
            .s("s").h("[1:2:3:4:5:6:7:8]").p("").z();

        test("s", "1:2:3:4:5:6:7:8%hme0", null, null)
            .s("s").h("[1:2:3:4:5:6:7:8%hme0]").p("").z();

        test("s", "1:2:3:4:5:6:7:8%1", null, null)
            .s("s").h("[1:2:3:4:5:6:7:8%1]").p("").z();

        test("s", "[1:2:3:4:5:6:7:8]", null, null)
            .s("s").h("[1:2:3:4:5:6:7:8]").p("").z();

        test("s", "[1:2:3:4:5:6:7:8]", null, null, null)
            .s("s").h("[1:2:3:4:5:6:7:8]").p("").z();

        test("s", "1:2:3:4:5:6:7:8", null, null, null)
            .s("s").g("1:2:3:4:5:6:7:8").p("").z();

        // Error cases

        test("http://[ff01:234/foo").x().z();
        test("http://[ff01:234:zzz]/foo").x().z();
        test("http://[foo]").x().z();
        test("http://[]").x().z();
        test("http://[129.33.44.55]").x().z();
        test("http://[ff:ee:dd:cc:bb::aa:9:8]").x().z();
        test("http://[fffff::1]").x().z();
        test("http://[ff::ee::8]").x().z();
        test("http://[1:2:3:4::5:6:7:8]").x().z();
        test("http://[1:2]").x().z();
        test("http://[1:2:3:4:5:6:7:8:9]").x().z();
        test("http://[1:2:3:4:5:6:7:8%]").x().z();
        test("http://[1:2:3:4:5:6:7:8%!/]").x().z();
        test("http://[::1.2.3.300]").x().z();
        test("http://1.2.3").psa().x().z();
        test("http://1.2.3.300").psa().x().z();
        test("http://1.2.3.4.5").psa().x().z();
        test("http://[1.2.3.4:5]").x().z();
        test("http://1:2:3:4:5:6:7:8").psa().x().z();
        test("http://[1.2.3.4]/").x().z();
        test("http://[1.2.3.4/").x().z();
        test("http://[foo]/").x().z();
        test("http://[foo/").x().z();
        test("s", "[foo]", "/", null, null).x().z();
        test("s", "[foo", "/", null, null).x().z();
        test("s", "[::foo", "/", null, null).x().z();

        // Test hostnames that might initially look like IPv4 addresses

        test("s://1.2.3.com").psa().s("s").h("1.2.3.com").p("").z();
        test("s://1.2.3.4me.com").psa().s("s").h("1.2.3.4me.com").p("").z();

        test("s://7up.com").psa().s("s").h("7up.com").p("").z();
        test("s://7up.com/p").psa().s("s").h("7up.com").p("/p").z();
        test("s://7up").psa().s("s").h("7up").p("").z();
        test("s://7up/p").psa().s("s").h("7up").p("/p").z();
        test("s://7up.").psa().s("s").h("7up.").p("").z();
        test("s://7up./p").psa().s("s").h("7up.").p("/p").z();
    }


    static void misc() throws URISyntaxException {

        URI base = new URI("s://h/a/b");
        URI rbase = new URI("a/b/c/d");


        header("Corner cases");

        // The empty URI parses as a relative URI with an empty path
        test("").p("").z()
            .rslv(base).s("s").h("h").p("/a/").z();

        // Resolving solo queries and fragments
        test("#f").p("").f("f").z()
            .rslv(base).s("s").h("h").p("/a/b").f("f").z();
        test("?q").p("").q("q").z()
            .rslv(base).s("s").h("h").p("/a/").q("q").z();

        // Fragment is not part of ssp
        test("p#f").p("p").f("f").sp("p").z();
        test("s:p#f").s("s").o("p").f("f").z();
        test("p#f")
            .rslv(base).s("s").h("h").p("/a/p").f("f").sp("//h/a/p").z();
        test("").p("").sp("").z();



        header("Emptiness");

        // Components that may be empty
        test("///p").p("/p").z();                 // Authority (w/ path)
        test("//@h/p").u("").h("h").p("/p").z();  // User info
        test("//h:/p").h("h").p("/p").z();        // Port
        test("//h").h("h").p("").z();             // Path
        test("//h?q").h("h").p("").q("q").z();    // Path (w/query)
        test("//?q").p("").q("q").z();            // Authority (w/query)
        test("//#f").p("").f("f").z();            // Authority (w/fragment)
        test("p?#").p("p").q("").f("").z();       // Query & fragment

        // Components that may not be empty
        test(":").x().z();              // Scheme
        test("x:").x().z();             // Hier/opaque
        test("//").x().z();             // Authority (w/o path)


        header("Resolution, normalization, and relativization");

        // Resolving relative paths
        test("../e/f").p("../e/f").z()
            .rslv(rbase).p("a/b/e/f").z();
        test("../../../../d").p("../../../../d").z()
            .rslv(rbase).p("../d").z();
        test("../../../d:e").p("../../../d:e").z()
            .rslv(rbase).p("./d:e").z();
        test("../../../d:e/f").p("../../../d:e/f").z()
            .rslv(rbase).p("./d:e/f").z();

        // Normalization
        test("a/./c/../d/f").p("a/./c/../d/f").z()
            .norm().p("a/d/f").z();
        test("http://a/./b/c/../d?q#f")
            .s("http").h("a").p("/./b/c/../d").q("q").f("f").z()
            .norm().s("http").h("a").p("/b/d").q("q").f("f").z();
        test("a/../b").p("a/../b").z().
            norm().p("b");
        test("a/../b:c").p("a/../b:c").z()
            .norm().p("./b:c").z();

        // Normalization of already normalized URI should yield the
        // same URI
        URI u1 = URI.create("s://h/../p");
        URI u2 = u1.normalize();
        eq(u1, u2);
        eqeq(u1, u2);

        // Relativization
        test("/a/b").p("/a/b").z()
            .rtvz(new URI("/a")).p("b").z();
        test("/a/b").p("/a/b").z()
            .rtvz(new URI("/a/")).p("b").z();
        test("a/b").p("a/b").z()
            .rtvz(new URI("a")).p("b").z();
        test("/a/b").p("/a/b").z()
            .rtvz(new URI("/a/b")).p("").z();   // Result is empty path
        test("a/../b:c/d").p("a/../b:c/d").z()
            .rtvz(new URI("./b:c/")).p("d").z();

        test("http://a/b/d/e?q#f")
            .s("http").h("a").p("/b/d/e").q("q").f("f").z()
            .rtvz(new URI("http://a/b/?r#g"))
            .p("d/e").q("q").f("f").z();

        // parseServerAuthority
        test("/a/b").psa().p("/a/b").z();
        test("s://u@h:1/p")
            .psa().s("s").u("u").h("h").n(1).p("/p").z();
        test("s://u@h:-foo/p").s("s").g("u@h:-foo").p("/p").z()
            .psa().x().z();
        test("s://h:999999999999999999999999").psa().x().z();
        test("s://:/b").psa().x().z();


        header("Constructors and factories");

        test("s", null, null, -1, "p", null, null).x().z();
        test(null, null, null, -1, null, null, null).p("").z();
        test(null, null, null, -1, "p", null, null).p("p").z();
        test(null, null, "foo%20bar", -1, null, null, null).x().z();
        test(null, null, "foo", -100, null, null, null).x().z();
        test("s", null, null, -1, "", null, null).x().z();
        test("s", null, null, -1, "/p", null, null).s("s").p("/p").z();
        test("s", "u", "h", 10, "/p", "q", "f")
            .s("s").u("u").h("h").n(10).p("/p").q("q").f("f").z();
        test("s", "a:b", "/p", "q", "f")
            .s("s").g("a:b").p("/p").q("q").f("f").z();
        test("s", "h", "/p", "f")
            .s("s").h("h").p("/p").f("f").z();
        test("s", "p", "f").s("s").o("p").f("f").z();
        test("s", "/p", "f").s("s").p("/p").f("f").z();
        testCreate("s://u@h/p?q#f")
            .s("s").u("u").h("h").p("/p").q("q").f("f").z();
    }

    static void npes() throws URISyntaxException {

        header("NullPointerException");

        URI base = URI.create("mailto:root@foobar.com");

        out.println();

        try {
            base.resolve((URI)null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException x) {
            out.println("resolve((URI)null) -->");
            out.println("Correct exception: " + x);
        }

        out.println();

        try {
            base.resolve((String)null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException x) {
            out.println("resolve((String)null) -->");
            out.println("Correct exception: " + x);
        }

        out.println();

        try {
            base.relativize((URI)null);
            throw new RuntimeException("NullPointerException not thrown");
        } catch (NullPointerException x) {
            out.println("relativize((String)null) -->");
            out.println("Correct exception: " + x);
        }

        testCount += 3;
    }


    static void chars() throws URISyntaxException {

        header("Escapes and non-US-ASCII characters");

        URI uri;

        // Escape pairs
        test("%0a%0A%0f%0F%01%09zz")
            .p("%0a%0A%0f%0F%01%09zz").z();
        test("foo%1").x().z();
        test("foo%z").x().z();
        test("foo%9z").x().z();

        // Escapes not permitted in scheme, host
        test("s%20t://a").x().z();
        test("//a%20b").g("a%20b").p("").z();         // Parses as registry

        // Escapes permitted in opaque part, userInfo, registry, path,
        // query, and fragment
        test("//u%20v@a").u("u%20v").h("a").p("").z();
        test("/p%20q").p("/p%20q").z();
        test("/p?q%20").p("/p").q("q%20").z();
        test("/p#%20f").p("/p").f("%20f").z();

        // Non-US-ASCII chars
        test("s\u00a7t://a").x().z();
        test("//\u00a7/b").g("\u00a7").p("/b").z();     // Parses as registry
        test("//u\u00a7v@a").u("u\u00a7v").h("a").p("").z();
        test("/p\u00a7q").p("/p\u00a7q").z();
        test("/p?q\u00a7").p("/p").q("q\u00a7").z();
        test("/p#\u00a7f").p("/p").f("\u00a7f").z();

        // 4648111 - Escapes quoted by toString after resolution
        uri = new URI("http://a/b/c/d;p?q");
        test("/p%20p")
            .rslv(uri).s("http").h("a").p("/p%20p").ts("http://a/p%20p").z();

        // 4464135: Forbid unwise characters throughout opaque part
        test("foo:x{bar").x().z();
        test("foo:{bar").x().z();

        // 4438319: Single-argument constructor requires quotation,
        //          preserves escapes
        test("//u%01@h/a/b/%02/c?q%03#f%04")
            .u("u%01").ud("u\1")
            .h("h")
            .p("/a/b/%02/c").pd("/a/b/\2/c")
            .q("q%03").qd("q\3")
            .f("f%04").fd("f\4")
            .z();
        test("/a/b c").x().z();

        // 4438319: Multi-argument constructors quote illegal chars and
        //          preserve legal non-ASCII chars
        // \uA001-\uA009 are visible characters, \u2000 is a space character
        test(null, "u\uA001\1", "h", -1,
             "/p% \uA002\2\u2000",
             "q% \uA003\3\u2000",
             "f% \uA004\4\u2000")
            .u("u\uA001%01").h("h")
            .p("/p%25%20\uA002%02%E2%80%80").pd("/p% \uA002\2\u2000")
            .q("q%25%20\uA003%03%E2%80%80").qd("q% \uA003\3\u2000")
            .f("f%25%20\uA004%04%E2%80%80").fd("f% \uA004\4\u2000").z();
        test(null, "g\uA001\1",
             "/p% \uA002\2\u2000",
             "q% \uA003\3\u2000",
             "f% \uA004\4\u2000")
            .g("g\uA001%01")
            .p("/p%25%20\uA002%02%E2%80%80").pd("/p% \uA002\2\u2000")
            .q("q%25%20\uA003%03%E2%80%80").qd("q% \uA003\3\u2000")
            .f("f%25%20\uA004%04%E2%80%80").fd("f% \uA004\4\u2000").z();
        test(null, null, "/p% \uA002\2\u2000", "f% \uA004\4\u2000")
            .p("/p%25%20\uA002%02%E2%80%80").pd("/p% \uA002\2\u2000")
            .f("f%25%20\uA004%04%E2%80%80").fd("f% \uA004\4\u2000").z();
        test(null, "/sp% \uA001\1\u2000", "f% \uA004\4\u2000")
            .sp("/sp%25%20\uA001%01%E2%80%80").spd("/sp% \uA001\1\u2000")
            .p("/sp%25%20\uA001%01%E2%80%80").pd("/sp% \uA001\1\u2000")
            .f("f%25%20\uA004%04%E2%80%80").fd("f% \uA004\4\u2000").z();

        // 4438319: Non-raw accessors decode all escaped octets
        test("/%25%20%E2%82%AC%E2%80%80")
            .p("/%25%20%E2%82%AC%E2%80%80").pd("/% \u20Ac\u2000").z();

        // 4438319: toASCIIString
        test("/\uCAFE\uBABE")
            .p("/\uCAFE\uBABE").ta("/%EC%AB%BE%EB%AA%BE").z();

        // 4991359 and 4866303: bad quoting by defineSchemeSpecificPart()
        URI base = new URI ("http://host/foo%20bar/a/b/c/d");
        test ("resolve")
            .rslv(base).spd("//host/foo bar/a/b/c/resolve")
            .sp("//host/foo%20bar/a/b/c/resolve").s("http")
            .pd("/foo bar/a/b/c/resolve").h("host")
            .p("/foo%20bar/a/b/c/resolve").z();

        // 6773270: java.net.URI fails to escape u0000
        test("s", "a", "/\u0000", null)
            .s("s").p("/%00").h("a")
            .ta("s://a/%00").z();
    }


    static void eq0(URI u, URI v) throws URISyntaxException {
        testCount++;
        if (!u.equals(v))
            throw new RuntimeException("Not equal: " + u + " " + v);
        int uh = u.hashCode();
        int vh = v.hashCode();
        if (uh != vh)
            throw new RuntimeException("Hash codes not equal: "
                                       + u + " " + Integer.toHexString(uh) + " "
                                       + v + " " + Integer.toHexString(vh));
        out.println();
        out.println(u + " == " + v
                    + "  [" + Integer.toHexString(uh) + "]");
    }

    static void cmp0(URI u, URI v, boolean same)
        throws URISyntaxException
    {
        int c = u.compareTo(v);
        if ((c == 0) != same)
            throw new RuntimeException("Comparison inconsistent: " + u + " " + v
                                       + " " + c);
    }

    static void eq(URI u, URI v) throws URISyntaxException {
        eq0(u, v);
        cmp0(u, v, true);
    }

    static void eq(String expected, String actual) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected != null && expected.equals(actual)) {
            return;
        }
        throw new AssertionError(String.format(
                "Strings are not equal: '%s', '%s'", expected, actual));
    }

    static void eqeq(URI u, URI v) {
        testCount++;
        if (u != v)
            throw new RuntimeException("Not ==: " + u + " " + v);
    }

    static void ne0(URI u, URI v) throws URISyntaxException {
        testCount++;
        if (u.equals(v))
            throw new RuntimeException("Equal: " + u + " " + v);
        out.println();
        out.println(u + " != " + v
                    + "  [" + Integer.toHexString(u.hashCode())
                    + " " + Integer.toHexString(v.hashCode())
                    + "]");
    }

    static void ne(URI u, URI v) throws URISyntaxException {
        ne0(u, v);
        cmp0(u, v, false);
    }

    static void lt(URI u, URI v) throws URISyntaxException {
        ne0(u, v);
        int c = u.compareTo(v);
        if (c >= 0) {
            show(u);
            show(v);
            throw new RuntimeException("Not less than: " + u + " " + v
                                       + " " + c);
        }
        out.println(u + " < " + v);
    }

    static void lt(String s, String t) throws URISyntaxException {
        lt(new URI(s), new URI(t));
    }

    static void gt0(URI u, URI v) throws URISyntaxException {
        ne0(u, v);
        int c = u.compareTo(v);
        if (c <= 0) {
            show(u);
            show(v);
            throw new RuntimeException("Not greater than: " + u + " " + v
                    + " " + c);
        }
        out.println(u + " < " + v);
    }

    static void gt(URI u, URI v) throws URISyntaxException {
        lt(v, u);
    }

    static void eqHashComp() throws URISyntaxException {

        header("Equality, hashing, and comparison");

        URI o = new URI("mailto:foo@bar.com");
        URI r = new URI("reg://some%20registry/b/c/d?q#f");
        URI s = new URI("http://jag:cafebabe@java.sun.com:94/b/c/d?q#f");
        URI t = new URI("http://example.com/%5bsegment%5d");
        eq(o, o);
        lt(o, r);
        lt(s, o);
        lt(s, r);

        eq(o, new URI("MaILto:foo@bar.com"));
        gt(o, new URI("mailto:foo@bar.COM"));
        eq(r, new URI("rEg://some%20registry/b/c/d?q#f"));
        gt(r, new URI("reg://Some%20Registry/b/c/d?q#f"));
        gt(r, new URI("reg://some%20registry/b/c/D?q#f"));
        eq(s, new URI("hTtP://jag:cafebabe@Java.Sun.COM:94/b/c/d?q#f"));
        gt(s, new URI("http://jag:CafeBabe@java.sun.com:94/b/c/d?q#f"));
        lt(s, new URI("http://jag:cafebabe@java.sun.com:94/b/c/d?r#f"));
        lt(s, new URI("http://jag:cafebabe@java.sun.com:94/b/c/d?q#g"));
        cmp0(t, new URI("http://example.com/%5Bsegment%5D"), true);
        gt0(t, new URI("http://example.com/%5BSegment%5D"));
        lt(new URI("http://example.com/%5Asegment%5D"), new URI("http://example.com/%5Bsegment%5D"));
        eq(new URI("http://host/a%00bcd"), new URI("http://host/a%00bcd"));
        ne(new URI("http://host/a%00bcd"), new URI("http://host/aZ00bcd"));
        eq0(new URI("http://host/abc%e2def%C3ghi"),
            new URI("http://host/abc%E2def%c3ghi"));

        lt("p", "s:p");
        lt("s:p", "T:p");
        lt("S:p", "t:p");
        lt("s:/p", "s:p");
        lt("s:p", "s:q");
        lt("s:p#f", "s:p#g");
        lt("s://u@h:1", "s://v@h:1");
        lt("s://u@h:1", "s://u@i:1");
        lt("s://u@h:1", "s://v@h:2");
        lt("s://a%20b", "s://a%20c");
        lt("s://a%20b", "s://aab");
        lt("s://AA", "s://A_");
        lt("s:/p", "s:/q");
        lt("s:/p?q", "s:/p?r");
        lt("s:/p#f", "s:/p#g");

        lt("s://h", "s://h/p");
        lt("s://h/p", "s://h/p?q");

    }


    static void serial(URI u) throws IOException, URISyntaxException {

        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(bo);

        oo.writeObject(u);
        oo.close();

        ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
        ObjectInputStream oi = new ObjectInputStream(bi);
        try {
            Object o = oi.readObject();
            eq(u, (URI)o);
        } catch (ClassNotFoundException x) {
            x.printStackTrace();
            throw new RuntimeException(x.toString());
        }

        testCount++;
    }

    static void serial() throws IOException, URISyntaxException {
        header("Serialization");

        serial(URI.create("http://java.sun.com/jdk/1.4?release#beta"));
        serial(URI.create("s://h/p").resolve("/long%20path/"));
    }


    static void urls() throws URISyntaxException {

        header("URLs");

        URI uri;
        URL url;
        boolean caught = false;

        out.println();
        uri = new URI("http://a/p?q#f");
        try {
            url = uri.toURL();
        } catch (MalformedURLException x) {
            throw new RuntimeException(x.toString());
        }
        if (!url.toString().equals("http://a/p?q#f"))
            throw new RuntimeException("Incorrect URL: " + url);
        out.println(uri + " url --> " + url);

        out.println();
        uri = new URI("a/b");
        try {
            out.println(uri + " url --> ");
            url = uri.toURL();
        } catch (IllegalArgumentException x) {
            caught = true;
            out.println("Correct exception: " + x);
        } catch (MalformedURLException x) {
            caught = true;
            throw new RuntimeException("Incorrect exception: " + x);
        }
        if (!caught)
            throw new RuntimeException("Incorrect URL: " + url);

        out.println();
        uri = new URI("foo://bar/baz");
        caught = false;
        try {
            out.println(uri + " url --> ");
            url = uri.toURL();
        } catch (MalformedURLException x) {
            caught = true;
            out.println("Correct exception: " + x);
        } catch (IllegalArgumentException x) {
            caught = true;
            throw new RuntimeException("Incorrect exception: " + x);
        }
        if (!caught)
            throw new RuntimeException("Incorrect URL: " + url);

        testCount += 3;
    }


    static void tests() throws IOException, URISyntaxException {
        rfc2396();
        ip();
        misc();
        chars();
        eqHashComp();
        serial();
        urls();
        npes();
        bugs();
    }


    // -- Command-line invocation --

    static void usage() {
        out.println("Usage:");
        out.println("  java Test               --  Runs all tests in this file");
        out.println("  java Test <uri>         --  Parses uri, shows components");
        out.println("  java Test <base> <uri>  --  Parses uri and base, then resolves");
        out.println("                              uri against base");
    }

    static void clargs(String base, String uri) {
        URI b = null, u;
        try {
            if (base != null) {
                b = new URI(base);
                out.println(base);
                show(b);
            }
            u = new URI(uri);
            out.println(uri);
            show(u);
            if (base != null) {
                URI r = b.resolve(u);
                out.println(r);
                show(r);
            }
        } catch (URISyntaxException x) {
            show("ERROR", x);
            x.printStackTrace(out);
        }
    }


    // miscellaneous bugs/rfes that don't fit in with the test framework

    static void bugs() {
        b6339649();
        b6933879();
        b8037396();
    }

    // 6339649 - include detail message from nested exception
    private static void b6339649() {
        try {
            URI uri = URI.create("http://nowhere.net/should not be permitted");
        } catch (IllegalArgumentException e) {
            if ("".equals(e.getMessage()) || e.getMessage() == null) {
                throw new RuntimeException ("No detail message");
            }
        }
    }

    // 6933879 - check that "." and "_" characters are allowed in IPv6 scope_id.
    private static void b6933879() {
        final String HOST = "fe80::c00:16fe:cebe:3214%eth1.12_55";
        URI uri;
        try {
            uri = new URI("http", null, HOST, 10, "/", null, null);
        } catch (URISyntaxException ex) {
            throw new AssertionError("Should not happen", ex);
        }
        eq("[" + HOST + "]", uri.getHost());
    }

    private static void b8037396() {

        // primary checks:

        URI u;
        try {
            u = new URI("http", "example.org", "/[a b]", "[a b]", "[a b]");
        } catch (URISyntaxException e) {
            throw new AssertionError("shouldn't ever happen", e);
        }
        eq("/[a b]", u.getPath());
        eq("[a b]", u.getQuery());
        eq("[a b]", u.getFragment());

        // additional checks:
        //  *   '%' symbols are still decoded outside square brackets
        //  *   the getRawXXX() functionality left intact

        try {
            u = new URI("http", "example.org", "/a b[c d]", "a b[c d]", "a b[c d]");
        } catch (URISyntaxException e) {
            throw new AssertionError("shouldn't ever happen", e);
        }

        eq("/a b[c d]", u.getPath());
        eq("a b[c d]", u.getQuery());
        eq("a b[c d]", u.getFragment());

        eq("/a%20b%5Bc%20d%5D", u.getRawPath());
        eq("a%20b[c%20d]", u.getRawQuery());
        eq("a%20b[c%20d]", u.getRawFragment());
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
            clargs(null, args[0]);
            break;

        case 2:
            clargs(args[0], args[1]);
            break;

        default:
            usage();
            break;

        }
    }

}
