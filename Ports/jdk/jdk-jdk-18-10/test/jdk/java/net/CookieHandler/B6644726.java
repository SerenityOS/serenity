/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6644726 6873543
 * @summary Cookie management issues
 */

import java.net.*;
import java.util.*;

public class B6644726 {
    public static void main(String[] args) throws Exception {
        testCookieStore();
    }

    private static void testCookieStore() throws Exception {
        CookieManager cm = new CookieManager();
        CookieStore cs = cm.getCookieStore();
        URI uri = new URI("http://www.s1.sun.com/dir/foo/doc.html");
        URI suri = new URI("https://www.s1.sun.com/dir/foo/index.html");
        cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        ArrayList<String> lst = new ArrayList<String>();
        // Let's test the default path
        lst.add("myCookie1=foo");
        // Then some alternate expires format
        lst.add("myCookie2=bar; path=/dir; expires=Tue, 19 Aug 2025 16:00:00 GMT");
        lst.add("myCookie3=test; path=/dir; expires=Tue Aug 19 2025 16:00:00 GMT-0100");
        // Then Netscape draft cookies and domains
        lst.add("myCookie4=test; domain=.sun.com; path=/dir/foo");
        HashMap<String, List<String>> map = new HashMap<String, List<String>>();
        map.put("Set-Cookie", lst);
        cm.put(uri, map);
        map.clear();
        lst.clear();
        // Test for secure tag
        lst.add("myCookie5=test; secure");
        // Test for passing cookies between http and https
        map.put("Set-Cookie", lst);
        cm.put(suri, map);

        List<HttpCookie> cookies = cs.getCookies();
        // There should be 5 cookies if all dates parsed correctly
        if (cookies.size() != 5) {
            fail("Should have 5 cookies. Got only "+ cookies.size() + ", expires probably didn't parse correctly");
        }
        // Check Path for first Cookie
        for (HttpCookie c : cookies) {
            if (c.getName().equals("myCookie1")) {
                if (!"/dir/foo/".equals(c.getPath())) {
                    fail("Default path for myCookie1 is " + c.getPath());
                }
            }
        }

        HashMap<String, List<String>> emptyMap = new HashMap<String, List<String>>();
        // We should get 1 Cookie: MyCookie4, because of the domain
        Map<String, List<String>>m = cm.get(new URI("http://www.s2.sun.com/dir/foo/doc2.html"),
                emptyMap);
        List<String> clst = m.get("Cookie");
        if (clst.size() != 1) {
            fail("We should have only 1 cookie, not " + clst.size());
        } else {
            if (!clst.get(0).startsWith("myCookie4")) {
                fail("The cookie should be myCookie4, not " + clst.get(0));
            }
        }
        // We should get 4 cookies for non secure URI, and 5 for the secure one
        m = cm.get(suri, emptyMap);
        clst = m.get("Cookie");
        if (clst.size() != 5) {
            fail("Cookies didn't cross from http to https. Got only " + clst.size());
        }

        m = cm.get(uri, emptyMap);
        clst = m.get("Cookie");
        if (clst.size() != 4) {
            fail("We should have gotten only 4 cookies over http (non secure), got " +
                    clst.size());
        }
        if (isIn(clst, "myCookie5=")) {
            // myCookie5 (the secure one) shouldn't be here
            fail("Got the secure cookie over a non secure link");
        }

        // Let's check that empty path is treated correctly
        uri = new URI("http://www.sun.com/");
        lst.clear();
        lst.add("myCookie6=foo");
        map.clear();
        map.put("Set-Cookie", lst);
        cm.put(uri, map);
        uri = new URI("http://www.sun.com");
        m = cm.get(uri, emptyMap);
        clst = m.get("Cookie");
        if (clst.size() != 1) {
            fail("Missing a cookie when using an empty path");
        }

        // And now, the other way around:

        uri = new URI("http://www.sun.com");
        lst.clear();
        lst.add("myCookie7=foo");
        map.clear();
        map.put("Set-Cookie", lst);
        cm.put(uri, map);
        uri = new URI("http://www.sun.com/");
        m = cm.get(uri, emptyMap);
        clst = m.get("Cookie");
        if (!isIn(clst, "myCookie7=")) {
            fail("Missing a cookie when using an empty path");
        }

        // Let's make sure the 'Port' optional attributes is enforced

        lst.clear();
        lst.add("myCookie8=porttest; port");
        lst.add("myCookie9=porttest; port=\"80,8000\"");
        lst.add("myCookie10=porttest; port=\"8000\"");
        map.clear();
        map.put("Set-Cookie", lst);
        uri = new URI("http://www.sun.com/");
        cm.put(uri, map);

        // myCookie10 should have been rejected
        cookies = cs.getCookies();
        for (HttpCookie c : cookies) {
            if (c.getName().equals("myCookie10")) {
                fail("A cookie with an invalid port list was accepted");
            }
        }

        uri = new URI("http://www.sun.com:80/");
        m = cm.get(uri, emptyMap);
        clst = m.get("Cookie");
        // We should find both myCookie8 and myCookie9 but not myCookie10
        if (!isIn(clst, "myCookie8=") || !isIn(clst, "myCookie9=")) {
            fail("Missing a cookie on port 80");
        }
        uri = new URI("http://www.sun.com:8000/");
        m = cm.get(uri, emptyMap);
        clst = m.get("Cookie");
        // We should find only myCookie9
        if (!isIn(clst, "myCookie9=")) {
            fail("Missing a cookie on port 80");
        }
        if (isIn(clst, "myCookie8=")) {
            fail("A cookie with an invalid port list was returned");
        }

        // Test httpOnly flag (CR# 6873543)
        lst.clear();
        map.clear();
        cm.getCookieStore().removeAll();
        lst.add("myCookie11=httpOnlyTest; httpOnly");
        map.put("Set-Cookie", lst);
        uri = new URI("http://www.sun.com/");
        cm.put(uri, map);
        m = cm.get(uri, emptyMap);
        clst = m.get("Cookie");
        // URI scheme was http: so we should get the cookie
        if (!isIn(clst, "myCookie11=")) {
            fail("Missing cookie with httpOnly flag");
        }
        uri = new URI("javascript://www.sun.com/");
        m = cm.get(uri, emptyMap);
        clst = m.get("Cookie");
        // URI scheme was neither http or https so we shouldn't get the cookie
        if (isIn(clst, "myCookie11=")) {
            fail("Should get the cookie with httpOnly when scheme is javascript:");
        }
    }

    private static boolean isIn(List<String> lst, String cookie) {
        if (lst == null || lst.isEmpty()) {
            return false;
        }
        for (String s : lst) {
            if (s.startsWith(cookie))
                return true;
        }
        return false;
    }

    private static void fail(String msg) throws Exception {
        throw new RuntimeException(msg);
    }
}
