/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Unit test for java.lang.Runtime.Version
 * @bug 8072379 8144062 8161236 8160956 8194879
 */

import java.lang.Runtime.Version;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.System.out;

public class Basic {

    private static final Class<? extends Throwable> IAE
        = IllegalArgumentException.class;
    private static final Class<? extends Throwable> NPE
        = NullPointerException.class;
    private static final Class<? extends Throwable> NFE
        = NumberFormatException.class;

    private static final BigInteger TOO_BIG
        = (BigInteger.valueOf(Integer.MAX_VALUE)).add(BigInteger.ONE);
    private static final String TOO_BIG_STR = TOO_BIG.toString();

    public static void main(String ... args) {

        //// Tests for parse(), feature(), interim(), update(), patch(),
        //// pre(), build(), optional(), version(), and toString()
        //   v                          f     i  u  p pre bld opt

        // $VNUM
        test("9",                       9,    0, 0, 0, "", 0, "");
        test("9.1",                     9,    1, 0, 0, "", 0, "");
        test("9.0.1",                   9,    0, 1, 0, "", 0, "");
        test("9.0.0.1",                 9,    0, 0, 1, "", 0, "");
        test("9.0.0.0.1",               9,    0, 0, 0, "", 0, "");
        test("404.1.2",                 404,  1, 2, 0, "", 0, "");
        test("9.1.2.3",                 9,    1, 2, 3, "", 0, "");
        test("1000.0.0.0.0.0.99999999", 1000, 0, 0, 0, "", 0, "");

        tryCatch(null,    NPE);
        tryCatch("",      IAE);
        tryCatch("foo",   IAE);
        tryCatch("7a",    IAE);
        tryCatch("0",     IAE);
        tryCatch("09",    IAE);
        tryCatch("9.0",   IAE);
        tryCatch("9.0.",  IAE);
        tryCatch("1.9,1", IAE);
        tryCatch(TOO_BIG_STR, NFE);

        // $PRE
        test("9-ea",       9, 0, 0, 0, "ea",       0, "");
        test("9-internal", 9, 0, 0, 0, "internal", 0, "");
        test("9-0",        9, 0, 0, 0, "0",        0, "");
        test("9.2.7-8",    9, 2, 7, 0, "8",        0, "");
        test("1-ALL",      1, 0, 0, 0, "ALL",      0, "");
        test("2.3.4.5-1a", 2, 3, 4, 5, "1a",       0, "");
        test("1-" + TOO_BIG_STR, 1, 0, 0, 0, TOO_BIG_STR, 0, "");

        tryCatch("9:-ea",     IAE);
        tryCatch("3.14159-",  IAE);
        tryCatch("3.14159-%", IAE);

        // $BUILD
        test("9+0",            9, 0,  0,  0, "",      0,       "");
        test("3.14+9999900",   3, 14, 0,  0, "",      9999900, "");
        test("9-pre+105",      9, 0,  0,  0, "pre",   105,     "");
        test("6.0.42-8beta+4", 6, 0,  42, 0, "8beta", 4,       "");

        tryCatch("9+",     IAE);
        tryCatch("7+a",    IAE);
        tryCatch("9+00",   IAE);
        tryCatch("4.2+01", IAE);
        tryCatch("4.2+1a", IAE);
        tryCatch("1+" + TOO_BIG_STR, NFE);

        // $OPT
        test("9+-foo",          9,   0, 0, 0, "",       0,  "foo");
        test("9-pre-opt",       9,   0, 0, 0, "pre",    0,  "opt");
        test("42+---bar",       42,  0, 0, 0, "",       0,  "--bar");
        test("2.91+-8061493-",  2,  91, 0, 0, "",       0,  "8061493-");
        test("24+-foo.bar",     24,  0, 0, 0, "",       0,  "foo.bar");
        test("9-ribbit+17-...", 9,   0, 0, 0, "ribbit", 17, "...");
        test("7+1-" + TOO_BIG_STR, 7,0, 0, 0, "",       1,  TOO_BIG_STR);

        tryCatch("9-pre+-opt", IAE);
        tryCatch("1.4142+-",   IAE);
        tryCatch("2.9979+-%",  IAE);
        tryCatch("10--ea",     IAE);

        //// Test for Runtime.version()
        testVersion();

        //// Test for equals{IgnoreOptional}?(), hashCode(),
        //// compareTo{IgnoreOptional}?()
        // compare: after "<" == -1, equal == 0, before ">" == 1
        //      v0            v1                  eq     eqNO  cmp  cmpNO
        testEHC("9",          "9",                true,  true,   0,    0);

        testEHC("8",          "9",                false, false, -1,   -1);
        testEHC("9",          "10",               false, false, -1,   -1);
        testEHC("9",          "8",                false, false,  1,    1);

        testEHC("10.512.1",   "10.512.2",         false, false, -1,   -1);
        testEHC("10.512.0.1", "10.512.0.2",       false, false, -1,   -1);
        testEHC("10.512.0.0.1", "10.512.0.0.2",   false, false, -1,   -1);
        testEHC("512.10.1",   "512.11.1",         false, false, -1,   -1);

        // $OPT comparison
        testEHC("9",          "9+-oink",          false, true,  -1,    0);
        testEHC("9+-ribbit",  "9+-moo",           false, true,   1,    0);
        testEHC("9-quack+3-ribbit",
                              "9-quack+3-moo",    false, true,   1,    0);
        testEHC("9.1+7",      "9.1+7-moo-baa-la", false, true,  -1,    0);

        // numeric vs. non-numeric $PRE
        testEHC("9.1.1.2-2a", "9.1.1.2-12",       false, false,  1,    1);
        testEHC("9.1.1.2-12", "9.1.1.2-4",        false, false,  1,    1);

        testEHC("27.16",      "27.16+120",        false, false, -1,   -1);
        testEHC("10",         "10-ea",            false, false,  1,    1);
        testEHC("10.1+1",     "10.1-ea+1",        false, false,  1,    1);
        testEHC("10.0.1+22",  "10.0.1+21",        false, false,  1,    1);

        // numeric vs. non-numeric $PRE
        testEHC("9.1.1.2-12", "9.1.1.2-a2",       false, false, -1,   -1);
        testEHC("9.1.1.2-1",  "9.1.1.2-4",        false, false, -1,   -1);

        testEHC("9-internal", "9",                false, false, -1,   -1);
        testEHC("9-ea+120",   "9+120",            false, false, -1,   -1);
        testEHC("9-ea+120",   "9+120",            false, false, -1,   -1);
        testEHC("9+101",      "9",                false, false,  1,    1);
        testEHC("9+101",      "9+102",            false, false, -1,   -1);
        testEHC("1.9-ea",     "9-ea",             false, false, -1,   -1);

        if (fail != 0)
            throw new RuntimeException((fail + pass) + " tests: "
                                       + fail + " failure(s), first", first);
        else
            out.println("all " + (fail + pass) + " tests passed");

    }

    private static void test(String s, Integer feature, Integer interim,
                             Integer update, Integer patch,
                             String pre, Integer build, String opt)
    {
        Version v = testParse(s);

        testStr(v.toString(), s);

        testInt(v.feature(), feature);
        testInt(v.major(), feature);
        testInt(v.interim(), interim);
        testInt(v.minor(), interim);
        testInt(v.update(), update);
        testInt(v.security(), update);
        testInt(v.patch(), patch);
        testStr((v.pre().isPresent() ? v.pre().get() : ""), pre);
        testInt((v.build().isPresent() ? v.build().get() : 0), build);
        testStr((v.optional().isPresent() ? v.optional().get() : ""), opt);

        testVersion(v.version(), s);
    }

    private static Version testParse(String s) {
        Version v = Version.parse(s);
        pass();
        return v;
    }

    private static void testInt(int got, int exp) {
        if (got != exp) {
            fail("testInt()", Integer.toString(exp), Integer.toString(got));
        } else {
            pass();
        }
     }

    private static void testStr(String got, String exp) {
        if (!got.equals(exp)) {
            fail("testStr()", exp, got);
        } else {
            pass();
        }
    }

    private static void tryCatch(String s, Class<? extends Throwable> ex) {
        Throwable t = null;
        try {
            Version.parse(s);
        } catch (Throwable x) {
            if (ex.isAssignableFrom(x.getClass())) {
                t = x;
            } else
                x.printStackTrace();
        }
        if ((t == null) && (ex != null))
            fail(s, ex);
        else
            pass();
    }

    private static void testVersion() {
        Version current = Runtime.version();
        String javaVer = System.getProperty("java.runtime.version");

        // java.runtime.version == $VNUM(\-$PRE)?(\+$BUILD)?(-$OPT)?
        String [] jv  = javaVer.split("\\+");
        String [] ver = jv[0].split("-");
        List<Integer> javaVerVNum
            = Arrays.stream(ver[0].split("\\."))
            .map(Integer::parseInt)
            .collect(Collectors.toList());
        if (!javaVerVNum.equals(current.version())) {
            fail("Runtime.version()", javaVerVNum.toString(),
                 current.version().toString());
        } else {
            pass();
        }

        Optional<String> javaVerPre
            = (ver.length == 2)
            ? Optional.ofNullable(ver[1])
            : Optional.empty();
        if (!javaVerPre.equals(current.pre())) {
            fail("testCurrent() pre()", javaVerPre.toString(),
                 current.pre().toString());
        } else {
            pass();
        }

        testEHC(current.toString(), javaVer, true, true, 0, 0);
    }

    private static void testVersion(List<Integer> vnum, String s) {
        List<Integer> svnum = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            Character c = s.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            } else {
                svnum.add(Integer.parseInt(sb.toString()));
                sb = new StringBuilder();
                if (c == '+' || c == '-') {
                    break;
                }
            }
        }
        if (sb.length() > 0) {
            svnum.add(Integer.parseInt(sb.toString()));
        }

        if (!svnum.equals(vnum)) {
            fail("testVersion() equals()", svnum.toString(), vnum.toString());
        } else {
            pass();
        }
    }

    private static void testEHC(String s0, String s1, boolean eq, boolean eqNO,
                                int cmp, int cmpNO)
    {
        Version v0 = Version.parse(s0);
        Version v1 = Version.parse(s1);

        testEquals(v0, v1, eq);
        testEqualsNO(v0, v1, eqNO);

        testHashCode(v0, v1, eq);

        testCompare(v0, v1, cmp);
        testCompareNO(v0, v1, cmpNO);
    }

    private static void testEqualsNO(Version v0, Version v1, boolean eq) {
        if (eq == v0.equalsIgnoreOptional(v1)) {
            pass();
        } else {
            fail("equalsIgnoreOptional() " + Boolean.toString(eq),
                 v0.toString(), v1.toString());
        }
    }

    private static void testEquals(Version v0, Version v1, boolean eq) {
        if (eq == v0.equals(v1)) {
            pass();
        } else {
            fail("equals() " + Boolean.toString(eq),
                 v0.toString(), v1.toString());
        }
    }

    private static void testHashCode(Version v0, Version v1, boolean eq) {
        int h0 = v0.hashCode();
        int h1 = v1.hashCode();
        if (eq) {
            testInt(h0, h1);
        } else if (h0 == h1) {
            fail(String.format("hashCode() %s", h0),
                 Integer.toString(h0),
                 Integer.toString(h1));
        } else { // !eq && (h0 != h1)
            pass();
        }
    }

    private static void testCompareNO(Version v0, Version v1, int compare) {
        int cmp = v0.compareToIgnoreOptional(v1);
        checkCompare(v0, v1, compare, cmp);
    }

    private static void testCompare(Version v0, Version v1, int compare) {
        int cmp = v0.compareTo(v1);
        checkCompare(v0, v1, compare, cmp);
    }

    private static void checkCompare(Version v0, Version v1,
                                     int expected, int actual)
    {
        if (Integer.signum(expected) == Integer.signum(actual)) {
            pass();
        } else {
            fail(String.format("compare() (actual = %s) (expected = %s)",
                               actual, expected),
                 v0.toString(), v1.toString());
        }
    }

    private static int fail = 0;
    private static int pass = 0;

    private static Throwable first;

    static void pass() {
        pass++;
    }

    static void fail(String fs, Class ex) {
        String s = "'" + fs + "'";
        if (ex != null)
            s += ": " + ex.getName() + " not thrown";
        if (first == null)
            setFirst(s);
        System.err.println("FAILED: " + s);
        fail++;
    }

    static void fail(String t, String exp, String got) {
        String s = t + ": Expected '" + exp + "', got '" + got + "'";
        if (first == null)
            setFirst(s);
        System.err.println("FAILED: " + s);
        fail++;
     }

    private static void setFirst(String s) {
        try {
            throw new RuntimeException(s);
        } catch (RuntimeException x) {
            first = x;
        }
    }

}
