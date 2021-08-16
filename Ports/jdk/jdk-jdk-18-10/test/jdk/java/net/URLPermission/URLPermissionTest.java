/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.net.URLPermission;
import java.io.*;

/**
 * @test
 * @bug 8010464 8027570 8027687 8029354 8114860 8071660 8161291
 */

public class URLPermissionTest {

    // super class for all test types
    abstract static class Test {
        boolean expected;
        abstract boolean execute();
    };

    // Instantiation: should succeed
    static class CreateTest extends Test {
        String arg;
        CreateTest(String arg) {
            this.arg = arg;
        }

        @Override
        boolean execute() {
            try {
                URLPermission p = new URLPermission(arg);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    };

    static CreateTest createtest(String arg) {
        return new CreateTest(arg);
    }

    // Should throw an IAE on construction

    static class ExTest extends Test {
        String arg;
        ExTest(String arg) {
            this.arg = arg;
        }

        @Override
        boolean execute() {
            try {
                URLPermission p = new URLPermission(arg);
                return false;
            } catch (IllegalArgumentException e) {
                return true;
            }
        }
    };

    static ExTest extest(String arg) {
        return new ExTest(arg);
    }

    // Tests URL part of implies() method. This is the main test.
    static class URLImpliesTest extends Test {
        String arg1, arg2;

        URLImpliesTest(String arg1, String arg2, boolean expected) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.expected = expected;
        }

          boolean execute() {
            URLPermission p1 = new URLPermission (arg1, "GET:*");
            URLPermission p2 = new URLPermission (arg2, "GET:*");
            boolean result = p1.implies(p2);
            if (result != expected) {
                System.out.println("p1 = " + p1);
                System.out.println("p2 = " + p2);
            }
            return result == expected;
        }
    };

    static URLImpliesTest imtest(String arg1, String arg2, boolean expected) {
        return new URLImpliesTest(arg1, arg2, expected);
    }

    static class ActionImpliesTest extends Test {
        String arg1, arg2;
        String url1 = "http://www.foo.com/-";
        String url2 = "http://www.foo.com/a/b";

        ActionImpliesTest(String arg1, String arg2, boolean expected) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.expected = expected;
        }

        ActionImpliesTest(String ur11, String url2, String arg1, String arg2,
            boolean expected) {
            this.url1 = ur11;
            this.url2 = url2;
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.expected = expected;
        }

        @Override
          boolean execute() {
            URLPermission p1 = new URLPermission(url1, arg1);
            URLPermission p2 = new URLPermission(url2, arg2);
            boolean result = p1.implies(p2);

            return result == expected;
        }
    }

    static ActionsStringTest actionstest(String arg, String expectedActions) {
        return new ActionsStringTest(arg, expectedActions);
    }

    static class ActionsStringTest extends Test {

        String expectedActions;
        String arg;

        public ActionsStringTest(String arg, String expectedActions) {
            this.arg = arg;
            this.expectedActions = expectedActions;
        }

        @Override
        boolean execute() {
            String url = "http://www.foo.com/";
            URLPermission urlp = new URLPermission(url, arg);
            return (expectedActions.equals(urlp.getActions()));
        }
    }

    static ActionImpliesTest actest(String arg1, String arg2, boolean expected) {
        return new ActionImpliesTest(arg1, arg2, expected);
    }

    static ActionImpliesTest actest(String url1, String url2, String arg1,
        String arg2, boolean expected) {
        return new ActionImpliesTest(url1, url2, arg1, arg2, expected);
    }

    static class HashCodeTest extends Test {
        String arg1, arg2;
        int hash;

        HashCodeTest(String arg1, String arg2, int hash) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.hash = hash;
        }

        @Override
        boolean execute() {
            URLPermission p = new URLPermission(arg1, arg2);
            int h = p.hashCode();
            return h == hash;
        }
    }

    static HashCodeTest hashtest(String arg1, String arg2, int expected) {
        return new HashCodeTest(arg1, arg2, expected);
    }

    static class URLEqualityTest extends Test {
        String arg1, arg2;

        URLEqualityTest(String arg1, String arg2, boolean expected) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.expected = expected;
        }

        @Override
          boolean execute() {
            URLPermission p1 = new URLPermission(arg1);
            URLPermission p2 = new URLPermission(arg2);
            boolean result = p1.equals(p2);

            return result == expected;
        }
    }

    static URLEqualityTest eqtest(String arg1, String arg2, boolean expected) {
        return new URLEqualityTest(arg1, arg2, expected);
    }

    static Test[] pathImplies = {
        // single
        imtest("http://www.foo.com/", "http://www.foo.com/", true),
        imtest("http://www.bar.com/", "http://www.foo.com/", false),
        imtest("http://www.foo.com/a/b", "http://www.foo.com/", false),
        imtest("http://www.foo.com/a/b", "http://www.foo.com/a/b/c", false),
        // wildcard
        imtest("http://www.foo.com/a/b/*", "http://www.foo.com/a/b/c", true),
        imtest("http://www.foo.com/a/b/*", "http://www.foo.com/a/b/*", true),
        imtest("http://www.foo.com/a/b/*", "http://www.foo.com/a/b/c#frag", true),
        imtest("http://www.foo.com/a/b/*", "http://www.foo.com/a/b/c#frag?foo=foo", true),
        imtest("http://www.foo.com/a/b/*", "http://www.foo.com/b/b/c", false),
        imtest("http://www.foo.com/a/b/*", "http://www.foo.com/a/b/c.html", true),
        imtest("http://www.foo.com/a/b/*", "http://www.foo.com/a/b/c.html", true),
        imtest("http://www.foo.com/a/b/*", "https://www.foo.com/a/b/c", false),
        // recursive
        imtest("http://www.foo.com/a/b/-", "http://www.foo.com/a/b/-", true),
        imtest("http://www.foo.com/a/b/-", "http://www.foo.com/a/b/c", true),
        imtest("http://www.foo.com/a/b/-", "http://www.foo.com/a/b/c#frag", true),
        imtest("http://www.foo.com/a/b/-", "http://www.foo.com/a/b/c#frag?foo=foo", true),
        imtest("http://www.foo.com/a/b/-", "http://www.foo.com/b/b/c", false),
        imtest("http://www.foo.com/a/b/-", "http://www.foo.com/a/b/c.html", true),
        imtest("http://www.foo.com/a/b/-", "http://www.foo.com/a/b/c.html", true),
        imtest("http://www.foo.com/a/b/-", "http://www.foo.com/a/b/c/d/e.html", true),
        imtest("https://www.foo.com/a/b/-", "http://www.foo.com/a/b/c/d/e.html", false),
        imtest("http://www.foo.com/a/b/-", "http://www.foo.com/a/b/c/d/e#frag", true),
        imtest("http://www.foo.com/a/b/-", "https://www.foo.com/a/b/c", false),
        // special cases
        imtest("http:*", "https://www.foo.com/a/b/c", false),
        imtest("http:*", "http://www.foo.com/a/b/c", true),
        imtest("http:*", "http://foo/bar", true),
        imtest("http://WWW.foO.cOM/a/b/*", "http://wwW.foo.com/a/b/c", true),
        imtest("http://wWw.fOo.cOm/a/b/*", "http://Www.foo.com/a/b/*", true),
        imtest("http://www.FOO.com/", "http://www.foo.COM/", true),
        imtest("http://66ww-w.F-O012O.com/", "http://66ww-w.f-o012o.COM/",true),
        imtest("http://xn--ire-9la.com/", "http://xn--ire-9la.COM/", true),
        imtest("http://x/", "http://X/", true),
        imtest("http://x/", "http://x/", true),
        imtest("http://X/", "http://X/", true),
        imtest("http://foo/bar", "https://foo/bar", false),
        imtest("http://www.foo.com/*", "http://www.foo.com/#foo", true),
        imtest("http://www.foo.com/a/*#foo", "http://www.foo.com/a/b#foo", true),
        imtest("http://www.foo.com/a/-", "http://www.foo.com/a/b#foo", true),
        imtest("http://www.foo.com/?q1=1&q2=2#foo", "http://www.foo.com/?q1=1&q2=2#bar", true),
        imtest("http://www.foo.com/", "http://www.foo.com/?q1=1&q2=2#bar", true),
        imtest("http://www.foo.com/", "http://www.foo.com?q1=1&q2=2#bar", false),
        imtest("http://www.foo.com", "http://www.foo.com?q1=1&q2=2#bar", true)
    };

    // new functionality

    static Test[] exceptionTests = {
        extest("http://1.2.3.4.5/a/b/c"),
        extest("http://www.*.com"),
        extest("http://[foo.com]:99"),
        extest("http://[fec0::X]:99"),
        extest("http:\\www.foo.com"),
        extest("http://w_09ww.foo.com"),
        extest("http://w&09ww.foo.com/p"),
        extest("http://www+foo.com"),
        extest("http:")
    };

    static Test[] hashTests = {
        hashtest("http://www.foo.com/path", "GET:X-Foo", 388644203),
        hashtest("http:*", "*:*", 3255810)
    };

    static Test[] pathImplies2 = {
        imtest("http://[FE80::]:99", "http://[fe80:0::]:99", true),

        // hostnames
        imtest("http://*.foo.com/a/b/-", "http://www.foo.com/a/b/c/d", true),
        imtest("http://*.foo.com/a/b/-", "http://www.bar.com/a/b/c/d", false),
        imtest("http://*.foo.com/a/b/-", "http://www.biz.bar.foo.com/a/b/c/d", true),
        imtest("http://*.foo.com/a/b/-", "http://www.biz.bar.foo.como/a/b/c/d", false),
        imtest("http://*/a/b/-", "http://www.biz.bar.foo.fuzz/a/b/c/d", true),
        imtest("http://*/a/b/-", "http://*/a/b/c/d", true),
        imtest("http://*.foo.com/a/b/-", "http://*/a/b/c/d", false),
        imtest("http:*", "http://*/a/b/c/d", true),

        // literal IPv4 addresses
        imtest("http://1.2.3.4/a/b/-", "http://www.biz.bar.foo.com/a/b/c/d", false),
        imtest("http://1.2.3.4/a/b/-", "http://1.2.3.4/a/b/c/d", true),
        imtest("http://1.2.3.4/a/b/-", "http://1.2.88.4/a/b/c/d", false),
        imtest("http:*", "http://1.2.88.4/a/b/c/d", true),

        // literal IPv6 addresses
        imtest("http://[fe80::]/a/b/-", "http://[fe80::0]/a/b/c", true),
        imtest("http://[fe80::]/a/b/-", "http://[fe80::3]/a/b/c", false),
        imtest("http://[1:2:3:4:5:6:7:8]/a/b/-","http://[1:002:03:4:0005:6:07:8]/a/b/c", true),
        imtest("http://[1:2:3:4:5:6:7:8]/a/b/-","http://[1:002:03:4:0033:6:07:8]/a/b/c", false),
        imtest("http://[1::2]/a/b/-", "http://[1:0:0:0::2]/a/b/c", true),
        imtest("http://[1::2]/a/b/-", "http://[1:0:0:0::3]/a/b/c", false),
        imtest("http://[FE80::]:99", "http://[fe80:0::]:99", true),
        imtest("http:*", "http://[fe80:0::]:99", true),

        // portranges
        imtest("http://*.foo.com:1-2/a/b/-", "http://www.foo.com:1/a/b/c/d", true),
        imtest("http://*.foo.com:1-2/a/b/-", "http://www.foo.com:3/a/b/c/d", false),
        imtest("http://*.foo.com:3-/a/b/-", "http://www.foo.com:1/a/b/c/d", false),
        imtest("http://*.foo.com:3-/a/b/-", "http://www.foo.com:4-5/a/b/c/d", true),
        imtest("http://*.foo.com:3-/a/b/-", "http://www.foo.com:3-3/a/b/c/d", true),
        imtest("http://*.foo.com:3-99/a/b/-", "http://www.foo.com:55-100/a/b/c/d", false),
        imtest("http://*.foo.com:-44/a/b/-", "http://www.foo.com:1/a/b/c/d", true),
        imtest("http://*.foo.com:-44/a/b/-", "http://www.foo.com:1-10/a/b/c/d", true),
        imtest("http://*.foo.com:-44/a/b/-", "http://www.foo.com:44/a/b/c/d", true),
        imtest("http://*.foo.com:-44/a/b/-", "http://www.foo.com:45/a/b/c/d", false),
        imtest("http://www.foo.com:70-90/a/b", "http://www.foo.com/a/b", true),
        imtest("https://www.foo.com/a/b", "https://www.foo.com:80/a/b", false),
        imtest("https://www.foo.com:70-90/a/b", "https://www.foo.com/a/b", false),
        imtest("https://www.foo.com/a/b", "https://www.foo.com:443/a/b", true),
        imtest("https://www.foo.com:200-500/a/b", "https://www.foo.com/a/b", true),
        imtest("http://www.foo.com:*/a/b", "http://www.foo.com:1-12345/a/b", true),
        imtest("http://host/a/b", "http://HOST/a/b", true),

        // misc
        imtest("https:*", "http://www.foo.com", false),
        imtest("https:*", "http:*", false)
    };

    static final String FOO_URL = "http://www.foo.com/";
    static final String BAR_URL = "http://www.bar.com/";

    static Test[] actionImplies = {
        actest("GET", "GET", true),
        actest("GET", "POST", false),
        actest("GET:", "PUT", false),
        actest("GET:", "GET", true),
        actest("GET,POST", "GET", true),
        actest("GET,POST:", "GET", true),
        actest("GET:X-Foo", "GET:x-foo", true),
        actest("GET:X-Foo,X-bar", "GET:x-foo", true),
        actest("GET:X-Foo", "GET:x-boo", false),
        actest("GET:X-Foo,X-Bar", "GET:x-bar,x-foo", true),
        actest("GET:X-Bar,X-Foo,X-Bar,Y-Foo", "GET:x-bar,x-foo", true),
        actest("GET:*", "GET:x-bar,x-foo", true),
        actest("*:*", "GET:x-bar,x-foo", true),
        actest("", "GET:x-bar,x-foo", false),
        actest("GET:x-bar,x-foo", "", true),
        actest("", "", true),
        actest("GET,DELETE", "GET,DELETE:x-foo", false),
        actest(FOO_URL, BAR_URL, "", "GET:x-bar,x-foo", false),
        actest(FOO_URL, BAR_URL, "GET:x-bar,x-foo", "", false),
        actest(FOO_URL, BAR_URL, "", "", false)
    };

    static Test[] actionsStringTest = {
        actionstest("", ":"),
        actionstest(":", ":"),
        actionstest(":X-Bar", ":X-Bar"),
        actionstest("GET", "GET:"),
        actionstest("get", "GET:"),
        actionstest("GET,POST", "GET,POST:"),
        actionstest("GET,post", "GET,POST:"),
        actionstest("get,post", "GET,POST:"),
        actionstest("get,post,DELETE", "DELETE,GET,POST:"),
        actionstest("GET,POST:", "GET,POST:"),
        actionstest("GET:X-Foo,X-bar", "GET:X-Bar,X-Foo"),
        actionstest("GET,POST,DELETE:X-Bar,X-Foo,X-Bar,Y-Foo", "DELETE,GET,POST:X-Bar,X-Bar,X-Foo,Y-Foo")
    };

    static Test[] equalityTests = {
        eqtest("http://www.foo.com", "http://www.FOO.CoM", true),
        eqtest("http://[fe80:0:0::]:1-2", "HTTP://[FE80::]:1-2", true),
        eqtest("HTTP://1.2.3.5/A/B/C", "http://1.2.3.5/A/b/C", false),
        eqtest("HTTP://1.2.3.5/A/B/C", "HTTP://1.2.3.5/A/b/C", false),
        eqtest("http:*", "http:*", true),
        eqtest("http://www.foo.com/a/b", "https://www.foo.com/a/b", false),
        eqtest("http://w.foo.com", "http://w.foo.com/", false),
        eqtest("http://*.foo.com", "http://*.foo.com", true),
        eqtest("http://www.foo.com/a/b", "http://www.foo.com:80/a/b", true),
        eqtest("http://www.foo.com/a/b", "http://www.foo.com:82/a/b", false),
        eqtest("https://www.foo.com/a/b", "https://www.foo.com:443/a/b", true),
        eqtest("https://www.foo.com/a/b", "https://www.foo.com:444/a/b", false),
        eqtest("http://michael@foo.com/bar","http://michael@foo.com/bar", true),
        eqtest("http://Michael@foo.com/bar","http://michael@goo.com/bar",false),
        eqtest("http://michael@foo.com/bar","http://george@foo.com/bar", true),
        eqtest("http://@foo.com/bar","http://foo.com/bar", true)
    };

    static Test[] createTests = {
        createtest("http://user@foo.com/a/b/c"),
        createtest("http://user:pass@foo.com/a/b/c"),
        createtest("http://user:@foo.com/a/b/c")
    };

    static boolean failed = false;

    public static void main(String args[]) throws Exception {
        for (int i=0; i<pathImplies.length ; i++) {
            URLImpliesTest test = (URLImpliesTest)pathImplies[i];
            Exception caught = null;
            boolean result = false;
            try {
                result = test.execute();
            } catch (Exception e) {
                caught = e;
                e.printStackTrace();
            }
            if (!result) {
                failed = true;
                System.out.printf("path test %d failed: %s : %s\n", i, test.arg1,
                        test.arg2);
            } else {
                System.out.println ("path test " + i + " OK");
            }

        }

        // new tests for functionality added in revision of API

        for (int i=0; i<pathImplies2.length ; i++) {
            URLImpliesTest test = (URLImpliesTest)pathImplies2[i];
            Exception caught = null;
            boolean result = false;
            try {
                result = test.execute();
            } catch (Exception e) {
                caught = e;
                e.printStackTrace();
            }
            if (!result) {
                failed = true;
                System.out.printf("path2 test %d failed: %s : %s\n", i, test.arg1,
                        test.arg2);
            } else {
                System.out.println ("path2 test " + i + " OK");
            }

        }

        for (int i=0; i<equalityTests.length ; i++) {
            URLEqualityTest test = (URLEqualityTest)equalityTests[i];
            Exception caught = null;
            boolean result = false;
            try {
                result = test.execute();
            } catch (Exception e) {
                caught = e;
                e.printStackTrace();
            }
            if (!result) {
                failed = true;
                System.out.printf("equality test %d failed: %s : %s\n", i, test.arg1,
                        test.arg2);
            } else {
                System.out.println ("equality test " + i + " OK");
            }

        }

        for (int i=0; i<hashTests.length; i++) {
            HashCodeTest test = (HashCodeTest)hashTests[i];
            boolean result = test.execute();
            if (!result) {
                System.out.printf ("test failed: %s %s %d\n", test.arg1, test.arg2, test.hash);
                failed = true;
            } else {
                System.out.println ("hash test " + i + " OK");
            }
        }

        for (int i=0; i<exceptionTests.length; i++) {
            ExTest test = (ExTest)exceptionTests[i];
            boolean result = test.execute();
            if (!result) {
                System.out.println ("test failed: " + test.arg);
                failed = true;
            } else {
                System.out.println ("exception test " + i + " OK");
            }
        }

        for (int i=0; i<createTests.length; i++) {
            CreateTest test = (CreateTest)createTests[i];
            boolean result = test.execute();
            if (!result) {
                System.out.println ("test failed: " + test.arg);
                failed = true;
            } else {
                System.out.println ("create test " + i + " OK");
            }
        }

        for (int i=0; i<actionImplies.length ; i++) {
            ActionImpliesTest test = (ActionImpliesTest)actionImplies[i];
            Exception caught = null;
            boolean result = false;
            try {
                result = test.execute();
            } catch (Exception e) {
                caught = e;
                e.printStackTrace();
            }
            if (!result) {
                failed = true;
                System.out.println ("test failed: " + test.arg1 + ": " +
                        test.arg2 + " Exception: " + caught);
            }
            System.out.println ("action test " + i + " OK");
        }

        for (int i = 0; i < actionsStringTest.length; i++) {
            ActionsStringTest test = (ActionsStringTest) actionsStringTest[i];
            Exception caught = null;
            boolean result = false;
            try {
                result = test.execute();
            } catch (Exception e) {
                caught = e;
            }
            if (!result) {
                failed = true;
                System.out.println("test failed: " + test.arg + ": "
                        + test.expectedActions + " Exception: " + caught);
            }
            System.out.println("Actions String test " + i + " OK");
        }

        serializationTest("http://www.foo.com/-", "GET,DELETE:*");
        serializationTest("https://www.foo.com/-", "POST:X-Foo");
        serializationTest("https:*", "*:*");
        serializationTest("http://www.foo.com/a/b/s/", "POST:X-Foo");
        serializationTest("http://www.foo.com/a/b/s/*", "POST:X-Foo");

        if (failed) {
            throw new RuntimeException("some tests failed");
        }

    }

    static void serializationTest(String name, String actions)
        throws Exception {

        URLPermission out = new URLPermission(name, actions);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(baos);
        o.writeObject(out);
        ByteArrayInputStream bain = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream i = new ObjectInputStream(bain);
        URLPermission in = (URLPermission)i.readObject();
        if (!in.equals(out)) {
            System.out.println ("FAIL");
            System.out.println ("in = " + in);
            System.out.println ("out = " + out);
            failed = true;
        }
    }
}
