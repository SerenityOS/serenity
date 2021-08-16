/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4663146
 * @summary Arguments match no method error
 * @comment converted from test/jdk/com/sun/jdi/EvalArgs.sh
 *
 * @library /test/lib
 * @build EvalArgs
 * @run main/othervm EvalArgs
 */

import lib.jdb.JdbCommand;
import lib.jdb.JdbTest;

/*
 * The bug is that, for example, if a String is passed
 * as an arg to a func where an Object is expected,
 * "Arguments match no method" error occurs. jdb doesn't notice that this is
 * legal because String is an instance of Object.
 */

class EvalArgsTarg {

    static jj1 myjj1;
    static jj2 myjj2;
    static oranges myoranges;
    static boolean jjboolean = true;
    static byte    jjbyte = 1;
    static char    jjchar = 'a';
    static double  jjdouble = 2.2;
    static float   jjfloat = 3.1f;
    static int     jjint = 4;
    static long    jjlong = 5;
    static short   jjshort = 6;
    static int[]   jjintArray = {7, 8};
    static float[] jjfloatArray = {9.1f, 10.2f};


    public static void main(String args[]) {
        myjj1 = new jj1();
        myjj2 = new jj2();
        myoranges = new oranges();

        // prove that these work
        System.out.println( ffjj1(myjj1));
        System.out.println( ffjj1(myjj2));

        System.out.println("EvalArgsTarg.ffoverload(EvalArgsTarg.jjboolean) = " +
                            EvalArgsTarg.ffoverload(EvalArgsTarg.jjboolean));
        System.out.println("EvalArgsTarg.ffoverload(EvalArgsTarg.jjbyte) = " +
                            EvalArgsTarg.ffoverload(EvalArgsTarg.jjbyte));
        System.out.println("EvalArgsTarg.ffoverload(EvalArgsTarg.jjchar) = " +
                            EvalArgsTarg.ffoverload(EvalArgsTarg.jjchar));
        System.out.println("EvalArgsTarg.ffoverload(EvalArgsTarg.jjdouble) = " +
                            EvalArgsTarg.ffoverload(EvalArgsTarg.jjdouble));

        //This doesn't even compile
        //System.out.println( "ffintArray(jjfloatArray) = " + ffintArray(jjfloatArray));
        gus();
    }

    static void gus() {
        int x = 0;             // @1 breakpoint
    }

    public static String ffjj1(jj1 arg) {
        return arg.me;
    }

    public static String ffjj2(jj2 arg) {
        return arg.me;
    }

    static String ffboolean(boolean p1) {
        return "ffbool: p1 = " + p1;
    }

    static String ffbyte(byte p1) {
        return "ffbyte: p1 = " + p1;
    }

    static String ffchar(char p1) {
        return "ffchar: p1 = " + p1;
    }

    static String ffdouble(double p1) {
        return "ffdouble: p1 = " + p1;
    }

    static String fffloat(float p1) {
        return "fffloat: p1 = " + p1;
    }

    static String ffint(int p1) {
        return "ffint: p1 = " + p1;
    }

    static String fflong(long p1) {
        return "fflong: p1 = " + p1;
    }

    static String ffshort(short p1) {
        return "ffshort: p1 = " + p1;
    }

    static String ffintArray(int[] p1) {
        return "ffintArray: p1 = " + p1;
    }

    // Overloaded funcs
    public static String ffoverload(jj1 arg) {
        return arg.me;
    }

    static String ffoverload(boolean p1) {
        return "ffoverload: boolean p1 = " + p1;
    }
/***
    static String ffoverload(byte p1) {
        return "ffoverload: byte p1 = " + p1;
    }
***/
    static String ffoverload(char p1) {
        return "ffoverload: char p1 = " + p1;
    }

    static String ffoverload(double p1) {
        return "ffoverload: double p1 = " + p1;
    }

    static String ffoverload(float p1) {
        return "ffoverload: float p1 = " + p1;
    }
/***
    static String ffoverload(int p1) {
        return "ffoverload: int p1 = " + p1;
    }
***/
    static String ffoverload(long p1) {
        return "ffoverload: long p1 = " + p1;
    }

    static String ffoverload(short p1) {
        return "ffoverload: short p1 = " + p1;
    }

    static String ffoverload(int[] p1) {
        return "ffoverload: int array p1 = " + p1;
    }

    static class jj1 {
        String me;
        jj1() {
            me = "jj1name";
        }
        public String toString() {
            return me;
        }

    }

    static class jj2 extends jj1 {
        jj2() {
            super();
            me = "jj2name";
        }
    }

    static class oranges {
        oranges() {
        }
    }
}

public class EvalArgs extends JdbTest {
    public static void main(String argv[]) {
        new EvalArgs().run();
    }

    private EvalArgs() {
        super(DEBUGGEE_CLASS);
    }

    private static final String DEBUGGEE_CLASS = EvalArgsTarg.class.getName();

    @Override
    protected void runCases() {
        setBreakpointsFromTestSource("EvalArgs.java", 1);
        // Run to breakpoint #1
        jdb.command(JdbCommand.run());

        final String argsMatchNoMethod = "Arguments match no method";
        // verify that it works ok when arg types are the same as
        // the param types
        evalShouldNotContain("EvalArgsTarg.ffboolean(EvalArgsTarg.jjboolean)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.ffbyte(EvalArgsTarg.jjbyte)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.ffchar(EvalArgsTarg.jjchar)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.ffdouble(EvalArgsTarg.jjdouble)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.fffloat(EvalArgsTarg.jjfloat)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.ffint(EvalArgsTarg.jjint)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.fflong(EvalArgsTarg.jjlong)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.ffshort(EvalArgsTarg.jjshort)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.ffintArray(EvalArgsTarg.jjintArray)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.ffjj1(EvalArgsTarg.myjj1)", argsMatchNoMethod);

        // Provide a visual break in the output
        jdb.command(JdbCommand.print("1"));

        // Verify mixing primitive types works ok
        // These should work even though the arg types are
        // not the same because there is only one
        // method with each name.
        evalShouldNotContain("EvalArgsTarg.ffbyte(EvalArgsTarg.jjint)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.ffchar(EvalArgsTarg.jjdouble)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.ffdouble(EvalArgsTarg.jjfloat)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.fffloat(EvalArgsTarg.jjshort)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.ffint(EvalArgsTarg.jjlong)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.fflong(EvalArgsTarg.jjchar)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.ffshort(EvalArgsTarg.jjbyte)", argsMatchNoMethod);

        jdb.command(JdbCommand.print("1"));

        //  Verify that passing a subclass object works
        evalShouldNotContain("EvalArgsTarg.ffjj1(EvalArgsTarg.myjj2)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.myjj1.toString().equals(\"jj1name\")", argsMatchNoMethod);

        jdb.command(JdbCommand.print("1"));

        // Overloaded methods.  These should pass
        // because there is an exact  match.
        evalShouldNotContain("EvalArgsTarg.ffoverload(EvalArgsTarg.jjboolean)", argsMatchNoMethod);

        evalShouldNotContain("EvalArgsTarg.ffoverload(EvalArgsTarg.jjchar)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.ffoverload(EvalArgsTarg.jjdouble)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.ffoverload(EvalArgsTarg.jjfloat)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.ffoverload(EvalArgsTarg.jjlong)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.ffoverload(EvalArgsTarg.jjshort)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.ffoverload(EvalArgsTarg.jjintArray)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.ffoverload(EvalArgsTarg.myjj1)", argsMatchNoMethod);
        evalShouldNotContain("EvalArgsTarg.ffoverload(EvalArgsTarg.myjj2)", argsMatchNoMethod);

        jdb.command(JdbCommand.print("1"));
        jdb.command(JdbCommand.print("\"These should fail with msg Arguments match multiple methods\""));

        // These overload calls should fail because there
        // isn't an exact match and jdb isn't smart  enough
        // to figure out which of several possibilities
        // should be called
        final String argsMatchMultipleMethods = "Arguments match multiple methods";
        evalShouldContain("EvalArgsTarg.ffoverload(EvalArgsTarg.jjbyte)", argsMatchMultipleMethods);

        evalShouldContain("EvalArgsTarg.ffoverload(EvalArgsTarg.jjint)", argsMatchMultipleMethods);

        jdb.command(JdbCommand.print("1"));
        jdb.command(JdbCommand.print("\"These should fail with InvalidTypeExceptions\""));

        final String invalidTypeException = "InvalidTypeException";
        evalShouldContain("EvalArgsTarg.ffboolean(EvalArgsTarg.jjbyte)", invalidTypeException);
        evalShouldContain("EvalArgsTarg.ffintArray(EvalArgsTarg.jjint)", invalidTypeException);
        evalShouldContain("EvalArgsTarg.ffintArray(EvalArgsTarg.jjfloatArray)", invalidTypeException);
        evalShouldContain("EvalArgsTarg.ffjj2(EvalArgsTarg.myjj1)", invalidTypeException);
        evalShouldContain("EvalArgsTarg.ffjj2(EvalArgsTarg.myoranges)", invalidTypeException);

        jdb.contToExit(1);
    }

}
