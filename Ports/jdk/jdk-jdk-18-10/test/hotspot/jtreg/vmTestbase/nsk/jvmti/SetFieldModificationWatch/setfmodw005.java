/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.SetFieldModificationWatch;

import java.io.PrintStream;

public class setfmodw005 {
    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("setfmodw005");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load setfmodw005 library");
            System.err.println("java.library.path:" + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void getReady(Object o1, Object o2);
    native static void check(int ind);
    native static int getRes();

    static PrintStream out;
    static int result = 0;

    static long fld0;
    long fld1;
    static float fld2;
    float fld3;
    static double fld4;
    double fld5;
    static Object fld6;
    Object fld7;
    static Object copy6 = new Object();
    static Object copy7 = new Object();
    static boolean fld8;
    boolean fld9;
    static byte fld10;
    byte fld11;
    static short fld12;
    short fld13;
    static char fld14;
    char fld15;

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream ref) {
        out = ref;
        setfmodw005 t = new setfmodw005();

        getReady(copy6, copy7);

        t.meth();

        return (getRes() | result);
    }

    public void meth() {

        fld0 = 0x1234567890abcdefL;
        check(0);
        if (fld0 != 0x1234567890abcdefL) {
            out.print("fld0 value is corrupted: ");
            out.println("expected=12345678901L, actual=" + fld0);
            result = 2;
        }

        fld1 = 0xfedcba0987654321L;
        check(1);
        if (fld1 != 0xfedcba0987654321L) {
            out.print("fld1 value is corrupted: ");
            out.println("expected=22345678901L, actual=" + fld1);
            result = 2;
        }

        fld2 = 123.456F;
        check(2);
        if (fld2 != 123.456F) {
            out.print("fld2 value is corrupted: ");
            out.println("expected=123.456F, actual=" + fld2);
            result = 2;
        }

        fld3 = 654.321F;
        check(3);
        if (fld3 != 654.321F) {
            out.print("fld3 value is corrupted: ");
            out.println("expected=223.456F, actual=" + fld3);
            result = 2;
        }

        fld4 = 123456.654321;
        check(4);
        if (fld4 != 123456.654321) {
            out.print("fld4 value is corrupted: ");
            out.println("expected=123456.654321, actual=" + fld4);
            result = 2;
        }

        fld5 = 654321.123456;
        check(5);
        if (fld5 != 654321.123456) {
            out.print("fld5 value is corrupted: ");
            out.println("expected=223456.654321, actual=" + fld5);
            result = 2;
        }

        fld6 = copy6;
        check(6);
        if (fld6 != copy6) {
            out.print("fld6 value is corrupted: ");
            out.println("Object does not match original value");
            result = 2;
        }

        fld7 = copy7;
        check(7);
        if (fld7 != copy7) {
            out.print("fld7 value is corrupted: ");
            out.println("Object does not match original value");
            result = 2;
        }

        fld8 = true;
        check(8);
        if (fld8 != true) {
            out.print("fld8 value is corrupted: ");
            out.println("expected=true, actual=" + fld8);
            result = 2;
        }

        fld9 = false;
        check(9);
        if (fld9 != false) {
            out.print("fld9 value is corrupted: ");
            out.println("expected=false, actual=" + fld9);
            result = 2;
        }

        fld10 = 123;
        check(10);
        if (fld10 != 123) {
            out.print("fld10 value is corrupted: ");
            out.println("expected=123, actual=" + fld10);
            result = 2;
        }

        fld11 = -123;
        check(11);
        if (fld11 != -123) {
            out.print("fld11 value is corrupted: ");
            out.println("expected=-123, actual=" + fld11);
            result = 2;
        }

        fld12 = 12345;
        check(12);
        if (fld12 != 12345) {
            out.print("fld12 value is corrupted: ");
            out.println("expected=12345, actual=" + fld12);
            result = 2;
        }

        fld13 = -12345;
        check(13);
        if (fld13 != -12345) {
            out.print("fld13 value is corrupted: ");
            out.println("expected=-12345, actual=" + fld13);
            result = 2;
        }

        fld14 = '\uabcd';
        check(14);
        if (fld14 != '\uabcd') {
            out.print("fld14 value is corrupted: ");
            out.println("expected=\\uabcd, actual=\\u"
                + Integer.toHexString((int)fld14));
            result = 2;
        }

        fld15 = '\udcba';
        check(15);
        if (fld15 != '\udcba') {
            out.print("fld15 value is corrupted: ");
            out.println("expected=\\udcba, actual=\\u"
                + Integer.toHexString((int)fld15));
            result = 2;
        }
    }
}
