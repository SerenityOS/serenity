/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4131169 4168988
   @summary Basic File constructor tests
 */


import java.io.*;
import java.util.ArrayList;


public class Cons {

    private static boolean debug = false;
    private static boolean old = false;
    private static boolean win32 = (File.separatorChar == '\\');


    private static String cvt(String s) {
        if (s == null) return s;
        if (win32) return s.replace('/', '\\');
        return s;
    }

    private static String[] slashPerms(String s) {
        if (!win32) return new String[] { s };
        if (s == null) return new String[] { s };
        int i = s.indexOf('/');
        if (i < 0) return new String[] { s };
        ArrayList a = new ArrayList();
        String p1 = s.substring(0, i);
        String[] p2 = slashPerms(s.substring(i + 1));
        for (int j = 0; j < p2.length; j++)
            a.add(p1 + '/' + p2[j]);
        for (int j = 0; j < p2.length; j++)
            a.add(p1 + '\\' + p2[j]);
        return (String[])(a.toArray(new String[a.size()]));
    }


    static class F extends File {
        String exp;

        public F(String path) {
            super(path);
            this.exp = cons(path);
        }

        public F(String parent, String child) {
            super(parent, child);
            this.exp = cons(parent, child);
        }

        public F(F parent, String child) {
            super(parent, child);
            if (parent == null) this.exp = cons((String)null, child);
            else this.exp = cons(parent, child);
        }

    }


    private static String nos(String s) {
        if (s == null) return "null";
        else return "\"" + s + "\"";
    }

    private static void ok(String ans, String exp) {
        System.err.println(nos(ans) + " <== " + exp);
    }

    private static void err(String ans, String exp, String got) throws Exception {
        System.err.println(nos(ans) + " <-- " + exp + " ==> " + nos(got));
        if (!debug) {
            throw new Exception("Mismatch: " + exp + " ==> " + nos(got)
                                + ", should be " + nos(ans));
        }
    }

    private static void ck(String ans, String exp, String got)
        throws Exception
    {
        if ((got == ans) || ((got != null) && got.equals(ans))) ok(ans, exp);
        else err(ans, exp, got);
    }

    private static String cons(String arg) {
        return "new File(" + nos(arg) + ")";
    }

    private static String cons(String arg1, String arg2) {
        return "new File(" + nos(arg1) + ", " + nos(arg2) + ")";
    }

    private static String cons(F arg1, String arg2) {
        return "new File(" + arg1.exp + ", " + nos(arg2) + ")";
    }

    private static String op(String exp, String opname) {
        return exp + "." + opname + "()";
    }

    private static void ckpnp(F f,
                              String parent, String name, String path)
        throws Exception
    {
        ck(cvt(path), op(f.exp, "getPath"), f.getPath());
        ck(cvt(parent), op(f.exp, "getParent"), f.getParent());
        ck(cvt(name), op(f.exp, "getName"), f.getName());
    }

    private static void ck1(String arg,
                            String parent, String name, String path)
        throws Exception
    {
        String[] parg = slashPerms(arg);
        for (int i = 0; i < parg.length; i++)
            ckpnp(new F(parg[i]), parent, name, path);
    }

    private static void ck2(String arg1, String arg2,
                            String parent, String name, String path)
        throws Exception
    {
        String[] parg1 = slashPerms(arg1);
        String[] parg2 = slashPerms(arg2);
        for (int i = 0; i < parg1.length; i++)
            for (int j = 0; j < parg2.length; j++)
                ckpnp(new F(parg1[i], parg2[j]), parent, name, path);
    }

    private static void ck2f(String arg1, String arg2,
                             String parent, String name, String path)
        throws Exception
    {
        String[] parg1 = slashPerms(arg1);
        String[] parg2 = slashPerms(arg2);
        for (int i = 0; i < parg1.length; i++)
            for (int j = 0; j < parg2.length; j++) {
                F p = (parg1[i] == null) ? null : new F(parg1[i]);
                ckpnp(new F(p, parg2[j]), parent, name, path);
            }
    }


    static void testBoth() throws Exception {

        /* Null/empty constructor cases */
        ck1("", null, "", "");
        ck2(null, "", null, "", "");
        ck2("", "", null, "", "/"); /* ugh */
        ck2f("", "", null, "", "/");
        if (!old) {
            /* throws NullPointerException */
            ck2f(null, "", null, "", "");
        }

        /* Separators-in-pathnames cases */
        ck1("/", null, "", "/");
        ck2f("/", "", null, "", "/");

        /* One-arg constructor cases */
        ck1("foo", null, "foo", "foo");
        ck1("/foo", "/", "foo", "/foo");
        ck1("/foo/bar", "/foo", "bar", "/foo/bar");
        ck1("foo/bar", "foo", "bar", "foo/bar");
        if (!old) {
            ck1("foo/", null, "foo", "foo");
            ck1("/foo/", "/", "foo", "/foo");
            ck1("/foo//", "/", "foo", "/foo");
            ck1("/foo///", "/", "foo", "/foo");
            ck1("/foo//bar", "/foo", "bar", "/foo/bar");
            ck1("/foo/bar//", "/foo", "bar", "/foo/bar");
            ck1("foo//bar", "foo", "bar", "foo/bar");
            ck1("foo/bar/", "foo", "bar", "foo/bar");
            ck1("foo/bar//", "foo", "bar", "foo/bar");
        }

        /* Two-arg constructor cases, string parent */
        ck2("foo", "bar", "foo", "bar", "foo/bar");
        ck2("foo/", "bar", "foo", "bar", "foo/bar");
        ck2("/foo", "bar", "/foo", "bar", "/foo/bar");
        ck2("/foo/", "bar", "/foo", "bar", "/foo/bar");
        if (!old) {
            ck2("foo//", "bar", "foo", "bar", "foo/bar");
            ck2("foo", "bar/", "foo", "bar", "foo/bar");
            ck2("foo", "bar//", "foo", "bar", "foo/bar");
            ck2("/foo//", "bar", "/foo", "bar", "/foo/bar");
            ck2("/foo", "bar/", "/foo", "bar", "/foo/bar");
            ck2("/foo", "bar//", "/foo", "bar", "/foo/bar");
            ck2("foo", "/bar", "foo", "bar", "foo/bar");
            ck2("foo", "//bar", "foo", "bar", "foo/bar");
            ck2("/", "bar", "/", "bar", "/bar");
            ck2("/", "/bar", "/", "bar", "/bar");
        }

        /* Two-arg constructor cases, File parent */
        ck2f("foo", "bar", "foo", "bar", "foo/bar");
        ck2f("foo/", "bar", "foo", "bar", "foo/bar");
        ck2f("/foo", "bar", "/foo", "bar", "/foo/bar");
        ck2f("/foo/", "bar", "/foo", "bar", "/foo/bar");
        if (!old) {
            ck2f("foo//", "bar", "foo", "bar", "foo/bar");
            ck2f("foo", "bar/", "foo", "bar", "foo/bar");
            ck2f("foo", "bar//", "foo", "bar", "foo/bar");
            ck2f("/foo//", "bar", "/foo", "bar", "/foo/bar");
            ck2f("/foo", "bar/", "/foo", "bar", "/foo/bar");
            ck2f("/foo", "bar//", "/foo", "bar", "/foo/bar");
            ck2f("foo", "/bar", "foo", "bar", "foo/bar");
            ck2f("foo", "//bar", "foo", "bar", "foo/bar");
        }
    }


    static void testUnix() throws Exception {

        /* Separators-in-pathnames cases */
        if (!old) {
            ck1("//", null, "", "/");
        }

        /* One-arg constructor cases */
        if (!old) {
            ck1("//foo", "/", "foo", "/foo");
            ck1("///foo", "/", "foo", "/foo");
            ck1("//foo/bar", "/foo", "bar", "/foo/bar");
        }

        /* Two-arg constructors cases, string parent */
        if (!old) {
            ck2("//foo", "bar", "/foo", "bar", "/foo/bar");
        }

        /* Two-arg constructor cases, File parent */
        if (!old) {
            ck2f("//foo", "bar", "/foo", "bar", "/foo/bar");
        }

        File f = new File("/foo");
        if (! f.isAbsolute()) throw new Exception(f + " should be absolute");

        f = new File("foo");
        if (f.isAbsolute()) throw new Exception(f + " should not be absolute");
    }


    static void testWin32() throws Exception {

        if (!old) {
            /* Separators-in-pathnames cases */
            ck1("//", null, "", "//");

            /* One-arg constructor cases */
            ck1("//foo", "//", "foo", "//foo");
            ck1("///foo", "//", "foo", "//foo");
            ck1("//foo/bar", "//foo", "bar", "//foo/bar");

            ck1("z:", null, "", "z:");
            ck1("z:/", null, "", "z:/");
            ck1("z://", null, "", "z:/");
            ck1("z:/foo", "z:/", "foo", "z:/foo");
            ck1("z:/foo/", "z:/", "foo", "z:/foo");
            ck1("/z:/foo", "z:/", "foo", "z:/foo");
            ck1("//z:/foo", "z:/", "foo", "z:/foo");
            ck1("z:/foo/bar", "z:/foo", "bar", "z:/foo/bar");
            ck1("z:foo", "z:", "foo", "z:foo");

            /* Two-arg constructors cases, string parent */
            ck2("z:", "/", null, "", "z:/");    /* ## ? */
            ck2("z:", "/foo", "z:/", "foo", "z:/foo");
            ck2("z:/", "foo", "z:/", "foo", "z:/foo");
            ck2("z://", "foo", "z:/", "foo", "z:/foo");
            ck2("z:/", "/foo", "z:/", "foo", "z:/foo");
            ck2("z:/", "//foo", "z:/", "foo", "z:/foo");
            ck2("z:/", "foo/", "z:/", "foo", "z:/foo");
            ck2("//foo", "bar", "//foo", "bar", "//foo/bar");

            /* Two-arg constructor cases, File parent */
            ck2f("//foo", "bar", "//foo", "bar", "//foo/bar");

        }
    }


    public static void main(String[] args) throws Exception {
        old = new File("foo/").getPath().equals("foo/");
        if (old) System.err.println("** Testing old java.io.File class");
        testBoth();
        if (win32) testWin32();
        else testUnix();
    }

}
