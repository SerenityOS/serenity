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

/**
 * @test
 * @bug 6942326
 * @summary x86 code in string_indexof() could read beyond reserved heap space
 *
 * @run main/othervm/timeout=300 -Xmx128m -Xbatch -XX:+IgnoreUnrecognizedVMOptions
 *      -XX:CompileCommand=exclude,compiler.codegen.Test6942326::main
 *      -XX:CompileCommand=exclude,compiler.codegen.Test6942326::test_varsub_indexof
 *      -XX:CompileCommand=exclude,compiler.codegen.Test6942326::test_varstr_indexof
 *      -XX:CompileCommand=exclude,compiler.codegen.Test6942326::test_missub_indexof
 *      -XX:CompileCommand=exclude,compiler.codegen.Test6942326::test_consub_indexof
 *      -XX:CompileCommand=exclude,compiler.codegen.Test6942326::test_conmis_indexof
 *      -XX:CompileCommand=exclude,compiler.codegen.Test6942326::test_subcon
 *      compiler.codegen.Test6942326
 */

package compiler.codegen;

public class Test6942326 {

    static String[] strings = new String[1024];
    private static final int ITERATIONS = 100000;

    public static void main(String[] args) {

        long start_total = System.currentTimeMillis();

        // search variable size substring in string (33 chars).
        String a = " 1111111111111xx1111111111111xx11y"; // +1 to execute a.substring(1) first
        String b =  "1111111111111xx1111111111111xx11y";
        test_varsub_indexof(a, b);

        // search variable size substring in string (32 chars).
        a = " 1111111111111xx1111111111111xx1y";
        b =  "1111111111111xx1111111111111xx1y";
        test_varsub_indexof(a, b);

        // search variable size substring in string (17 chars).
        a = " 1111111111111xx1y";
        b =  "1111111111111xx1y";
        test_varsub_indexof(a, b);

        // search variable size substring in string (16 chars).
        a = " 111111111111xx1y";
        b =  "111111111111xx1y";
        test_varsub_indexof(a, b);

        // search variable size substring in string (8 chars).
        a = " 1111xx1y";
        b =  "1111xx1y";
        test_varsub_indexof(a, b);

        // search variable size substring in string (7 chars).
        a = " 111xx1y";
        b =  "111xx1y";
        test_varsub_indexof(a, b);



        // search substring (17 chars) in variable size string.
        a =                 "1111111111111xx1x";
        b = " 1111111111111xx1111111111111xx1x"; // +1 to execute b.substring(1) first
        test_varstr_indexof(a, b);

        // search substring (16 chars) in variable size string.
        a =                  "111111111111xx1x";
        b = " 1111111111111xx1111111111111xx1x";
        test_varstr_indexof(a, b);

        // search substring (9 chars) in variable size string.
        a =                         "11111xx1x";
        b = " 1111111111111xx1111111111111xx1x";
        test_varstr_indexof(a, b);

        // search substring (8 chars) in variable size string.
        a =                          "1111xx1x";
        b = " 1111111111111xx1111111111111xx1x";
        test_varstr_indexof(a, b);

        // search substring (4 chars) in variable size string.
        a =                              "xx1x";
        b = " 1111111111111xx1111111111111xx1x";
        test_varstr_indexof(a, b);

        // search substring (3 chars) in variable size string.
        a =                               "x1x";
        b = " 1111111111111xx1111111111111xx1x";
        test_varstr_indexof(a, b);

        // search substring (2 chars) in variable size string.
        a =                                "1y";
        b = " 1111111111111xx1111111111111xx1y";
        test_varstr_indexof(a, b);



        // search non matching variable size substring in string (33 chars).
        a = " 1111111111111xx1111111111111xx11z"; // +1 to execute a.substring(1) first
        b =  "1111111111111xx1111111111111xx11y";
        test_missub_indexof(a, b);

        // search non matching variable size substring in string (32 chars).
        a = " 1111111111111xx1111111111111xx1z";
        b =  "1111111111111xx1111111111111xx1y";
        test_missub_indexof(a, b);

        // search non matching variable size substring in string (17 chars).
        a = " 1111111111111xx1z";
        b =  "1111111111111xx1y";
        test_missub_indexof(a, b);

        // search non matching variable size substring in string (16 chars).
        a = " 111111111111xx1z";
        b =  "111111111111xx1y";
        test_missub_indexof(a, b);

        // search non matching variable size substring in string (8 chars).
        a = " 1111xx1z";
        b =  "1111xx1y";
        test_missub_indexof(a, b);

        // search non matching variable size substring in string (7 chars).
        a = " 111xx1z";
        b =  "111xx1y";
        test_missub_indexof(a, b);



        // Testing constant substring search in variable size string.

        // search constant substring (17 chars).
        b = " 1111111111111xx1111111111111xx1x"; // +1 to execute b.substring(1) first
        TestCon tc = new TestCon17();
        test_consub_indexof(tc, b);

        // search constant substring (16 chars).
        b = " 1111111111111xx1111111111111xx1x";
        tc = new TestCon16();
        test_consub_indexof(tc, b);

        // search constant substring (9 chars).
        b = " 1111111111111xx1111111111111xx1x";
        tc = new TestCon9();
        test_consub_indexof(tc, b);

        // search constant substring (8 chars).
        b = " 1111111111111xx1111111111111xx1x";
        tc = new TestCon8();
        test_consub_indexof(tc, b);

        // search constant substring (4 chars).
        b = " 1111111111111xx1111111111111xx1x";
        tc = new TestCon4();
        test_consub_indexof(tc, b);

        // search constant substring (3 chars).
        b = " 1111111111111xx1111111111111xx1x";
        tc = new TestCon3();
        test_consub_indexof(tc, b);

        // search constant substring (2 chars).
        b = " 1111111111111xx1111111111111xx1y";
        tc = new TestCon2();
        test_consub_indexof(tc, b);

        // search constant substring (1 chars).
        b = " 1111111111111xx1111111111111xx1y";
        tc = new TestCon1();
        test_consub_indexof(tc, b);


        // search non matching constant substring (17 chars).
        b = " 1111111111111xx1111111111111xx1z"; // +1 to execute b.substring(1) first
        tc = new TestCon17();
        test_conmis_indexof(tc, b);

        // search non matching constant substring (16 chars).
        b = " 1111111111111xx1111111111111xx1z";
        tc = new TestCon16();
        test_conmis_indexof(tc, b);

        // search non matching constant substring (9 chars).
        b = " 1111111111111xx1111111111111xx1z";
        tc = new TestCon9();
        test_conmis_indexof(tc, b);

        // search non matching constant substring (8 chars).
        b = " 1111111111111xx1111111111111xx1z";
        tc = new TestCon8();
        test_conmis_indexof(tc, b);

        // search non matching constant substring (4 chars).
        b = " 1111111111111xx1111111111111xx1z";
        tc = new TestCon4();
        test_conmis_indexof(tc, b);

        // search non matching constant substring (3 chars).
        b = " 1111111111111xx1111111111111xx1z";
        tc = new TestCon3();
        test_conmis_indexof(tc, b);

        // search non matching constant substring (2 chars).
        b = " 1111111111111xx1111111111111xx1z";
        tc = new TestCon2();
        test_conmis_indexof(tc, b);

        // search non matching constant substring (1 chars).
        b = " 1111111111111xx1111111111111xx1z";
        tc = new TestCon1();
        test_conmis_indexof(tc, b);

        long end_total = System.currentTimeMillis();
        System.out.println("End run time: " + (end_total - start_total));

    }

    public static long test_init(String a, String b) {
        for (int i = 0; i < 512; i++) {
            strings[i * 2] = new String(b.toCharArray());
            strings[i * 2 + 1] = new String(a.toCharArray());
        }
        System.out.print(a.length() + " " + b.length() + " ");
        return System.currentTimeMillis();
    }

    public static void test_end(String a, String b, int v, int expected, long start) {
        long end = System.currentTimeMillis();
        int res = (v/ITERATIONS);
        System.out.print(" " + res);
        System.out.println(" time:" + (end - start));
        if (res != expected) {
            System.out.println("wrong indexOf result: " + res + ", expected " + expected);
            System.out.println("\"" + b + "\".indexOf(\"" + a + "\")");
            System.exit(97);
        }
    }

    public static int test_subvar() {
        int s = 0;
        int v = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            v += strings[s].indexOf(strings[s + 1]);
            s += 2;
            if (s >= strings.length) s = 0;
        }
        return v;
    }

    public static void test_varsub_indexof(String a, String b) {
        System.out.println("Start search variable size substring in string (" + b.length() + " chars)");
        long start_it = System.currentTimeMillis();
        int limit = 1; // last a.length() == 1
        while (a.length() > limit) {
            a = a.substring(1);
            long start = test_init(a, b);
            int v = test_subvar();
            test_end(a, b, v, (b.length() - a.length()), start);
        }
        long end_it = System.currentTimeMillis();
        System.out.println("End search variable size substring in string (" + b.length() + " chars), time: " + (end_it - start_it));
    }

    public static void test_varstr_indexof(String a, String b) {
        System.out.println("Start search substring (" + a.length() + " chars) in variable size string");
        long start_it = System.currentTimeMillis();
        int limit = a.length();
        while (b.length() > limit) {
            b = b.substring(1);
            long start = test_init(a, b);
            int v = test_subvar();
            test_end(a, b, v, (b.length() - a.length()), start);
        }
        long end_it = System.currentTimeMillis();
        System.out.println("End search substring (" + a.length() + " chars) in variable size string, time: " + (end_it - start_it));
    }

    public static void test_missub_indexof(String a, String b) {
        System.out.println("Start search non matching variable size substring in string (" + b.length() + " chars)");
        long start_it = System.currentTimeMillis();
        int limit = 1; // last a.length() == 1
        while (a.length() > limit) {
            a = a.substring(1);
            long start = test_init(a, b);
            int v = test_subvar();
            test_end(a, b, v, (-1), start);
        }
        long end_it = System.currentTimeMillis();
        System.out.println("End search non matching variable size substring in string (" + b.length() + " chars), time: " + (end_it - start_it));
    }



    public static void test_consub_indexof(TestCon tc, String b) {
        System.out.println("Start search constant substring (" + tc.constr().length() + " chars)");
        long start_it = System.currentTimeMillis();
        int limit = tc.constr().length();
        while (b.length() > limit) {
            b = b.substring(1);
            long start = test_init(tc.constr(), b);
            int v = test_subcon(tc);
            test_end(tc.constr(), b, v, (b.length() - tc.constr().length()), start);
        }
        long end_it = System.currentTimeMillis();
        System.out.println("End search constant substring (" + tc.constr().length() + " chars), time: " + (end_it - start_it));
    }

    public static void test_conmis_indexof(TestCon tc, String b) {
        System.out.println("Start search non matching constant substring (" + tc.constr().length() + " chars)");
        long start_it = System.currentTimeMillis();
        int limit = tc.constr().length();
        while (b.length() > limit) {
            b = b.substring(1);
            long start = test_init(tc.constr(), b);
            int v = test_subcon(tc);
            test_end(tc.constr(), b, v, (-1), start);
        }
        long end_it = System.currentTimeMillis();
        System.out.println("End search non matching constant substring (" + tc.constr().length() + " chars), time: " + (end_it - start_it));
    }

    public static int test_subcon(TestCon tc) {
        int s = 0;
        int v = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            v += tc.indexOf(strings[s]);
            s += 2;
            if (s >= strings.length) s = 0;
        }
        return v;
    }

    private interface TestCon {
        public String constr();
        public int indexOf(String str);
    }

    // search constant substring (17 chars).
    private final static class TestCon17 implements TestCon {
        private static final String constr = "1111111111111xx1x";
        public String constr() { return constr; }
        public int indexOf(String str) { return str.indexOf(constr); }
    }

    // search constant substring (16 chars).
    private final static class TestCon16 implements TestCon {
        private static final String constr = "111111111111xx1x";
        public String constr() { return constr; }
        public int indexOf(String str) { return str.indexOf(constr); }
    }

    // search constant substring (9 chars).
    private final static class TestCon9 implements TestCon {
        private static final String constr = "11111xx1x";
        public String constr() { return constr; }
        public int indexOf(String str) { return str.indexOf(constr); }
    }

    // search constant substring (8 chars).
    private final static class TestCon8 implements TestCon {
        private static final String constr = "1111xx1x";
        public String constr() { return constr; }
        public int indexOf(String str) { return str.indexOf(constr); }
    }

    // search constant substring (4 chars).
    private final static class TestCon4 implements TestCon {
        private static final String constr = "xx1x";
        public String constr() { return constr; }
        public int indexOf(String str) { return str.indexOf(constr); }
    }

    // search constant substring (3 chars).
    private final static class TestCon3 implements TestCon {
        private static final String constr = "x1x";
        public String constr() { return constr; }
        public int indexOf(String str) { return str.indexOf(constr); }
    }

    // search constant substring (2 chars).
    private final static class TestCon2 implements TestCon {
        private static final String constr = "1y";
        public String constr() { return constr; }
        public int indexOf(String str) { return str.indexOf(constr); }
    }


    // search constant substring (1 chars).
    private final static class TestCon1 implements TestCon {
        private static final String constr = "y";
        public String constr() { return constr; }
        public int indexOf(String str) { return str.indexOf(constr); }
    }
}
