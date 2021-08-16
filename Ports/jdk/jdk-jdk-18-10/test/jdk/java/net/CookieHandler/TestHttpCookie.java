/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @summary Unit test for java.net.HttpCookie
 * @bug 6244040 6277796 6277801 6277808 6294071 6692802 6790677 6901170 8020758
 * @author Edward Wang
 */

import java.net.HttpCookie;
import java.util.List;

public class TestHttpCookie {
    private static int testCount = 0;

    private String cHeader = null;
    private List<HttpCookie> cookies = null;

    // test case here expressed as a string, which represents
    // the header string to be parsed into HttpCookie instance.
    // A TestHttpCookie instance will be created to hold such a HttpCookie
    // object, and TestHttpCookie class has utility methods to check equality
    // between HttpCookie's real property and expected property.
    static TestHttpCookie test(String cookieHeader) {
        testCount++;
        return new TestHttpCookie(cookieHeader);
    }

    TestHttpCookie(String cHeader) {
        this.cHeader = cHeader;

        try {
            List<HttpCookie> cookies = HttpCookie.parse(cHeader);
            this.cookies = cookies;
        } catch (IllegalArgumentException ignored) {
            cookies = null;
        }
    }

    // check name
    TestHttpCookie n(int index, String n) {
        HttpCookie cookie = cookies.get(index);
        if (cookie == null || !n.equalsIgnoreCase(cookie.getName())) {
            raiseError("name", cookie.getName(), n);
        }

        return this;
    }
    TestHttpCookie n(String n) { return n(0, n); }

    // check value
    TestHttpCookie v(int index, String v) {
        HttpCookie cookie = cookies.get(index);
        if (cookie == null || !v.equals(cookie.getValue())) {
            raiseError("value", cookie.getValue(), v);
        }

        return this;
    }
    TestHttpCookie v(String v) { return v(0, v); }

    // check version
    TestHttpCookie ver(int index, int ver) {
        HttpCookie cookie = cookies.get(index);
        if (cookie == null || (ver != cookie.getVersion())) {
            raiseError("version", Integer.toString(cookie.getVersion()), Integer.toString(ver));
        }

        return this;
    }
    TestHttpCookie ver(int ver) { return ver(0, ver); }

    // check path
    TestHttpCookie p(int index, String p) {
        HttpCookie cookie = cookies.get(index);
        if (cookie == null || !p.equals(cookie.getPath())) {
            raiseError("path", cookie.getPath(), p);
        }

        return this;
    }
    TestHttpCookie p(String p) { return p(0, p); }

    // check null-ability
    TestHttpCookie nil() {
        if (cookies != null) {
            raiseError("Check null-ability fail");
        }

        return this;
    }

    // check comment
    TestHttpCookie c(int index, String c) {
        HttpCookie cookie = cookies.get(index);
        if (cookie == null || !c.equals(cookie.getComment())) {
            raiseError("comment", cookie.getComment(), c);
        }

        return this;
    }
    TestHttpCookie c(String c) { return c(0, c); }

    // check comment url
    TestHttpCookie cu(int index, String cu) {
        HttpCookie cookie = cookies.get(index);
        if (cookie == null || !cu.equals(cookie.getCommentURL())) {
            raiseError("comment url", cookie.getCommentURL(), cu);
        }

        return this;
    }
    TestHttpCookie cu(String cu) { return cu(0, cu); }

    // check discard
    TestHttpCookie dsc(int index, boolean dsc) {
        HttpCookie cookie = cookies.get(index);
        if (cookie == null || (dsc != cookie.getDiscard())) {
            raiseError("discard", Boolean.toString(cookie.getDiscard()), Boolean.toString(dsc));
        }

        return this;
    }
    TestHttpCookie dsc(boolean dsc) { return dsc(0, dsc); }

    // check domain
    TestHttpCookie d(int index, String d) {
        HttpCookie cookie = cookies.get(index);
        if (cookie == null || !d.equalsIgnoreCase(cookie.getDomain())) {
            raiseError("domain", cookie.getDomain(), d);
        }

        return this;
    }
    TestHttpCookie d(String d) { return d(0, d); }

    // check max-age
    TestHttpCookie a(int index, long a) {
        HttpCookie cookie = cookies.get(index);
        if (cookie == null || (a != cookie.getMaxAge())) {
            raiseError("max-age", Long.toString(cookie.getMaxAge()), Long.toString(a));
        }

        return this;
    }
    TestHttpCookie a(long a) { return a(0, a); }

    // check port list
    TestHttpCookie port(int index, String p) {
        HttpCookie cookie = cookies.get(index);
        if (cookie == null || !p.equals(cookie.getPortlist())) {
            raiseError("portlist", cookie.getPortlist(), p);
        }

        return this;
    }
    TestHttpCookie port(String p) { return port(0, p); }

    // check http only
    TestHttpCookie httpOnly(int index, boolean b) {
        HttpCookie cookie = cookies.get(index);
        if (cookie == null || b != cookie.isHttpOnly()) {
            raiseError("HttpOnly", String.valueOf(cookie.isHttpOnly()), String.valueOf(b));
        }
        return this;
    }

    TestHttpCookie httpOnly(boolean b) {
        return httpOnly(0, b);
    }

    // check equality
    static void eq(HttpCookie ck1, HttpCookie ck2, boolean same) {
        testCount++;
        if (ck1.equals(ck2) != same) {
            raiseError("Comparison inconsistent: " + ck1 + " " + ck2
                    + " should " + (same ? "equal" : "not equal"));
        }

        int h1 = ck1.hashCode();
        int h2 = ck2.hashCode();
        if ((h1 == h2) != same) {
            raiseError("Comparison inconsistent: hashCode for " + ck1 + " " + ck2
                    + " should " + (same ? "equal" : "not equal"));
        }
    }

    // check domainMatches()
    static void dm(String domain, String host, boolean matches) {
        testCount++;
        if (HttpCookie.domainMatches(domain, host) != matches) {
            raiseError("Host " + host + (matches?" should ":" should not ") +
                        "domain-match with domain " + domain);
        }
    }

    void raiseError(String attr, String realValue, String expectedValue) {
        StringBuilder sb = new StringBuilder();
        sb.append("Cookie ").append(attr).append(" is ").append(realValue).
                append(", should be ").append(expectedValue).
                append(" (").append(cHeader).append(")");
        throw new RuntimeException(sb.toString());
    }

    static void raiseError(String prompt) {
        throw new RuntimeException(prompt);
    }

    static void runTests() {
        rfc2965();
        netscape();
        misc();
    }

    static void rfc2965() {
        header("Test using rfc 2965 syntax");

        test("set-cookie2: Customer=\"WILE_E_COYOTE\"; Version=\"1\"; Path=\"/acme\"")
        .n("Customer").v("WILE_E_COYOTE").ver(1).p("/acme");

        // whitespace between attr and = sign
        test("set-cookie2: Customer = \"WILE_E_COYOTE\"; Version = \"1\"; Path = \"/acme\"")
        .n("Customer").v("WILE_E_COYOTE").ver(1).p("/acme");

        // $NAME is reserved; result should be null
        test("set-cookie2: $Customer = \"WILE_E_COYOTE\"; Version = \"1\"; Path = \"/acme\"")
        .nil();

        // a 'full' cookie
        test("set-cookie2: Customer=\"WILE_E_COYOTE\"" +
                ";Version=\"1\"" +
                ";Path=\"/acme\"" +
                ";Comment=\"this is a coyote\"" +
                ";CommentURL=\"http://www.coyote.org\"" +
                ";Discard" +
                ";Domain=\".coyote.org\"" +
                ";Max-Age=\"3600\"" +
                ";Port=\"80\"" +
                ";Secure")
        .n("Customer").v("WILE_E_COYOTE").ver(1).p("/acme")
        .c("this is a coyote").cu("http://www.coyote.org").dsc(true)
        .d(".coyote.org").a(3600).port("80");

        // a 'full' cookie, without leading set-cookie2 token
        test("Customer=\"WILE_E_COYOTE\"" +
                ";Version=\"1\"" +
                ";Path=\"/acme\"" +
                ";Comment=\"this is a coyote\"" +
                ";CommentURL=\"http://www.coyote.org\"" +
                ";Discard" +
                ";Domain=\".coyote.org\"" +
                ";Max-Age=\"3600\"" +
                ";Port=\"80\"" +
                ";Secure")
        .n("Customer").v("WILE_E_COYOTE").ver(1).p("/acme")
        .c("this is a coyote").cu("http://www.coyote.org").dsc(true)
        .d(".coyote.org").a(3600).port("80");

        // empty set-cookie string
        test("").nil();

        // NullPointerException expected
        try {
            test(null);
        } catch (NullPointerException ignored) {
            // no-op
        }

        // bug 6277796
        test("Set-Cookie2:Customer=\"dtftest\"; Discard; Secure; Domain=\".sun.com\"; Max-Age=\"100\"; Version=\"1\";  path=\"/www\"; Port=\"80\"")
        .n("Customer").v("dtftest").ver(1).d(".sun.com").p("/www").port("80").dsc(true).a(100);

        // bug 6277801
        test("Set-Cookie2:Customer=\"dtftest\"; Discard; Secure; Domain=\".sun.com\"; Max-Age=\"100\"; Version=\"1\";  path=\"/www\"; Port=\"80\"" +
                ";Domain=\".java.sun.com\"; Max-Age=\"200\"; path=\"/javadoc\"; Port=\"8080\"")
        .n("Customer").v("dtftest").ver(1).d(".sun.com").p("/www").port("80").dsc(true).a(100);

        // bug 6294071
        test("Set-Cookie2:Customer=\"dtftest\";Discard; Secure; Domain=\"sun.com\"; Max-Age=\"100\";Version=\"1\";  Path=\"/www\"; Port=\"80,8080\"")
        .n("Customer").v("dtftest").ver(1).d("sun.com").p("/www").port("80,8080").dsc(true).a(100);
        test("Set-Cookie2:Customer=\"developer\";Domain=\"sun.com\";Max-Age=\"100\";Path=\"/www\";Port=\"80,8080\";CommentURL=\"http://www.sun.com/java1,000,000.html\"")
        .n("Customer").v("developer").d("sun.com").p("/www").port("80,8080").a(100).cu("http://www.sun.com/java1,000,000.html");

        // a header string contains 2 cookies
        test("Set-Cookie2:C1=\"V1\";Domain=\".sun1.com\";path=\"/www1\";Max-Age=\"100\",C2=\"V2\";Domain=\".sun2.com\";path=\"/www2\";Max-Age=\"200\"")
        .n(0, "C1").v(0, "V1").p(0, "/www1").a(0, 100).d(0, ".sun1.com")
        .n(1, "C2").v(1, "V2").p(1, "/www2").a(1, 200).d(1, ".sun2.com");

        // Bug 6790677: Should ignore bogus attributes
        test("Set-Cookie2:C1=\"V1\";foobar").n(0, "C1").v(0, "V1");
    }

    static void netscape() {
        header("Test using netscape cookie syntax");

        test("set-cookie: CUSTOMER=WILE_E_COYOTE; path=/; expires=Wednesday, 09-Nov-99 23:12:40 GMT")
        .n("CUSTOMER").v("WILE_E_COYOTE").p("/").ver(0);

        // a Netscape cookie, without set-cookie leading token
        test("CUSTOMER=WILE_E_COYOTE; path=/; expires=Wednesday, 09-Nov-99 23:12:40 GMT")
        .n("CUSTOMER").v("WILE_E_COYOTE").p("/").ver(0);

        // a 'google' cookie
        test("Set-Cookie: PREF=ID=1eda537de48ac25d:CR=1:TM=1112868587:LM=1112868587:S=t3FPA-mT9lTR3bxU;" +
             "expires=Sun, 17-Jan-2038 19:14:07 GMT; path=/; domain=.google.com")
        .n("PREF").v("ID=1eda537de48ac25d:CR=1:TM=1112868587:LM=1112868587:S=t3FPA-mT9lTR3bxU")
        .p("/").d(".google.com").ver(0);

        // bug 6277796
        test("set-cookie: CUSTOMER=WILE_E_COYOTE; path=/; expires=Wednesday, 09-Nov-99 23:12:40 GMT; Secure")
        .n("CUSTOMER").v("WILE_E_COYOTE").p("/").ver(0);

        // bug 6277801
        test("set-cookie: CUSTOMER=WILE_E_COYOTE; path=/; expires=Wednesday, 09-Nov-99 23:12:40 GMT; path=\"/acme\"")
        .n("CUSTOMER").v("WILE_E_COYOTE").p("/").ver(0);

        // bug 6901170
        test("set-cookie: CUSTOMER=WILE_E_COYOTE; version='1'").ver(1);
    }

    static void misc() {
        header("Test equals()");

        // test equals()
        HttpCookie c1 = new HttpCookie("Customer", "WILE_E_COYOTE");
        c1.setDomain(".coyote.org");
        c1.setPath("/acme");
        HttpCookie c2 = (HttpCookie)c1.clone();
        eq(c1, c2, true);

        // test equals() when domain and path are null
        c1 = new HttpCookie("Customer", "WILE_E_COYOTE");
        c2 = new HttpCookie("CUSTOMER", "WILE_E_COYOTE");
        eq(c1, c2, true);

        // path is case-sensitive
        c1 = new HttpCookie("Customer", "WILE_E_COYOTE");
        c2 = new HttpCookie("CUSTOMER", "WILE_E_COYOTE");
        c1.setPath("/acme");
        c2.setPath("/ACME");
        eq(c1, c2, false);

        header("Test domainMatches()");
        dm(".foo.com",      "y.x.foo.com",      false);
        dm(".foo.com",      "x.foo.com",        true);
        dm(".com",          "whatever.com",     false);
        dm(".com.",         "whatever.com",     false);
        dm(".ajax.com",     "ajax.com",         true);
        dm(".local",        "example.local",    true);
        dm("example.local", "example",          true);

        // bug 6277808
        testCount++;
        try {
            c1 = new HttpCookie("", "whatever");
        } catch (IllegalArgumentException ignored) {
            // expected exception; no-op
        }

        // CR 6692802: HttpOnly flag
        test("set-cookie: CUSTOMER=WILE_E_COYOTE;HttpOnly").httpOnly(true);
        test("set-cookie: CUSTOMER=WILE_E_COYOTE").httpOnly(false);

        // space disallowed in name (both Netscape and RFC2965)
        test("set-cookie: CUST OMER=WILE_E_COYOTE").nil();
    }

    static void header(String prompt) {
        System.out.println("== " + prompt + " ==");
    }

    public static void main(String[] args) {
        runTests();

        System.out.println("Succeeded in running " + testCount + " tests.");
    }
}
