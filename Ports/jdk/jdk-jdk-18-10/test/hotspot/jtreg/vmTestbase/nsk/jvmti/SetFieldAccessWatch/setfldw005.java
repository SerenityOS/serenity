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

package nsk.jvmti.SetFieldAccessWatch;

import java.io.PrintStream;

public class setfldw005 {

    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("setfldw005");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load setfldw005 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void getReady();
    native static void check(int ind);
    native static int getRes();

    static PrintStream out;
    static int result = 0;

    static long fld0 = 12345678901L;
    long fld1 = 22345678901L;
    static float fld2 = 123.456F;
    float fld3 = 223.456F;
    static double fld4 = 123456.654321;
    double fld5 = 223456.654321;
    static Object fld6 = new Object();
    Object fld7 = new Object();
    static Object copy6;
    static Object copy7;
    static boolean fld8 = true;
    boolean fld9 = false;
    static byte fld10 = 123;
    byte fld11 = -123;
    static short fld12 = 12345;
    short fld13 = -12345;
    static char fld14 = '\uabcd';
    char fld15 = '\udcba';

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream ref) {
        out = ref;
        setfldw005 t = new setfldw005();
        copy6 = fld6;
        copy7 = t.fld7;

        getReady();

        t.meth();

        return (getRes() | result);
    }

    public void meth() {
        long loc_l;
        float loc_f;
        double loc_d;
        Object loc_o;
        boolean loc_z;
        byte loc_b;
        short loc_s;
        char loc_c;

        loc_l = fld0;
        check(0);
        if (loc_l != 12345678901L) {
            out.print("fld0 value is corrupted: ");
            out.println("expected=12345678901L, actual=" + loc_l);
            result = 2;
        }

        loc_l = fld1;
        check(1);
        if (loc_l != 22345678901L) {
            out.print("fld1 value is corrupted: ");
            out.println("expected=22345678901L, actual=" + loc_l);
            result = 2;
        }

        loc_f = fld2;
        check(2);
        if (loc_f != 123.456F) {
            out.print("fld2 value is corrupted: ");
            out.println("expected=123.456F, actual=" + loc_f);
            result = 2;
        }

        loc_f = fld3;
        check(3);
        if (loc_f != 223.456F) {
            out.print("fld3 value is corrupted: ");
            out.println("expected=223.456F, actual=" + loc_f);
            result = 2;
        }

        loc_d = fld4;
        check(4);
        if (loc_d != 123456.654321) {
            out.print("fld4 value is corrupted: ");
            out.println("expected=123456.654321, actual=" + loc_d);
            result = 2;
        }

        loc_d = fld5;
        check(5);
        if (loc_d != 223456.654321) {
            out.print("fld5 value is corrupted: ");
            out.println("expected=223456.654321, actual=" + loc_d);
            result = 2;
        }

        loc_o = fld6;
        check(6);
        if (loc_o != copy6) {
            out.print("fld6 value is corrupted: ");
            out.println("Object does not match saved copy");
            result = 2;
        }

        loc_o = fld7;
        check(7);
        if (loc_o != copy7) {
            out.print("fld7 value is corrupted: ");
            out.println("Object does not match saved copy");
            result = 2;
        }

        loc_z = fld8;
        check(8);
        if (loc_z != true) {
            out.print("fld8 value is corrupted: ");
            out.println("expected=true, actual=" + loc_z);
            result = 2;
        }

        loc_z = fld9;
        check(9);
        if (loc_z != false) {
            out.print("fld9 value is corrupted: ");
            out.println("expected=false, actual=" + loc_z);
            result = 2;
        }

        loc_b = fld10;
        check(10);
        if (loc_b != 123) {
            out.print("fld10 value is corrupted: ");
            out.println("expected=123, actual=" + loc_b);
            result = 2;
        }

        loc_b = fld11;
        check(11);
        if (loc_b != -123) {
            out.print("fld11 value is corrupted: ");
            out.println("expected=-123, actual=" + loc_b);
            result = 2;
        }

        loc_s = fld12;
        check(12);
        if (loc_s != 12345) {
            out.print("fld12 value is corrupted: ");
            out.println("expected=12345, actual=" + loc_s);
            result = 2;
        }

        loc_s = fld13;
        check(13);
        if (loc_s != -12345) {
            out.print("fld13 value is corrupted: ");
            out.println("expected=-12345, actual=" + loc_s);
            result = 2;
        }

        loc_c = fld14;
        check(14);
        if (loc_c != '\uabcd') {
            out.print("fld14 value is corrupted: ");
            out.println("expected=\\uabcd, actual=\\u"
                + Integer.toHexString((int)loc_c));
            result = 2;
        }

        loc_c = fld15;
        check(15);
        if (loc_c != '\udcba') {
            out.print("fld15 value is corrupted: ");
            out.println("expected=\\udcba, actual=\\u"
                + Integer.toHexString((int)loc_c));
            result = 2;
        }
    }
}
