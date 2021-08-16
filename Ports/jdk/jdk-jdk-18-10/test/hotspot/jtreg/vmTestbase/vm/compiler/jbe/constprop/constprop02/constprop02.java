/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase runtime/jbe/constprop/constprop02.
 * VM Testbase keywords: [quick, runtime]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.constprop.constprop02.constprop02
 */

package vm.compiler.jbe.constprop.constprop02;

// constprop02.java

/* Tests constant propagation by substitute integer, long, and double
   by constant values.
 */

public class constprop02 {
    public static void main(String args[]) {
        boolean bPass = true;
        constprop02 cpr = new constprop02();

        bPass &= cpr.verify(cpr.testDiv_un_opt());
        bPass &= cpr.verify(cpr.testDiv_hand_opt());
        System.out.println("------------------------");
        if (!bPass) {
            throw new Error("Test constprop02 Failed.");
        }
        System.out.println("Test constprop02 Passed.");
    }

    String testDiv_un_opt() {
        int ia, ib;
        long lc, ld;
        double de, df;

        System.out.println("testDiv_un_opt:");
        ia = ib = 513;         if ( 1 != ia / ib) return "case 1 failed";
        ia = -ia;              if (-1 != ia / ib) return "case 2 failed";
        ia = ib = 1073741824;  if ( 1 != ia / ib) return "case 3 failed";
        ia = -ia;              if (-1 != ia / ib) return "case 4 failed";
        lc = ld = 8L;          if ( 1 != lc / ld) return "case 5 failed";
        lc = -lc;              if (-1 != lc / ld) return "case 6 failed";
        lc = ld = 1073741824L; if ( 1 != lc / ld) return "case 7 failed";
        lc = -lc;              if (-1 != lc / ld) return "case 8 failed";
        ib = 0;
        try {
            ia = ia / ib;
            return "case 9 failed";
        } catch (java.lang.Exception x) {/* good */}
        ld = 0;
        try {
            lc = lc / ld;
            return "case 10 failed";
        } catch (java.lang.Exception x) {/* good */}
        try {
            lc = lc % ld;
            return "case 11 failed";
        } catch (java.lang.Exception x) {/* good */}
        de = df = 16385.0;
        if (1.0 != de / df) return "case 12 failed";
        de = -de;
        if (-1.0 != de / df) return "case 13 failed";
        df = 0.0;
        try {
            de = de / df;
        } catch (java.lang.Exception x) {
            return "case 14 failed";
        }
        try {
            de = de % df;
            de = 5.66666666666 % df;
        } catch (java.lang.Exception x) {
            return "case 15 failed";
        }
        return null;
    }

    String testDiv_hand_opt() {
        int ia, ib;
        long lc, ld;
        double de, df;

        System.out.println("testDiv_hand_opt:");
        if ( 1 != 513 / 513) return "case 1 failed";
        if (-1 != -513 / 513) return "case 2 failed";
        if ( 1 != 1073741824 / 1073741824) return "case 3 failed";
        if (-1 != 1073741824 / -1073741824) return "case 4 failed";
        if ( 1 != 8L / 8L) return "case 5 failed";
        if (-1 != -8L / 8L) return "case 6 failed";
        if ( 1 != 1073741824L / 1073741824L) return "case 7 failed";
        if (-1 != 1073741824L / -1073741824L) return "case 8 failed";
        ib = 0;
        try {
            ia = -1073741824 / ib;
            return "case 9 failed";
        } catch (java.lang.Exception x) {/* good */}
        ld = 0L;
        try {
            lc = -1073741824L / ld;
            return "case 10 failed";
        } catch (java.lang.Exception x) {/* good */}
        try {
            lc = -1073741824L % ld;
            return "case 11 failed";
        } catch (java.lang.Exception x) {/* good */}
        if (1.0 != 16385.0 / 16385.0) return "case 12 failed";
        if (-1.0 != -16385.0 / 16385.0) return "case 13 failed";
        df = 0.0;
        try {
            de = -1073741824L / df;
        } catch (java.lang.Exception x) {
            return "case 14 failed";
        }
        try {
            de = -1073741824L % 0.0;
            de = 5.66666666666 % df;
        } catch (java.lang.Exception x) {
            return "cnase 15 failed";
        }
        return null;
    }

    boolean verify(String str) {
        boolean st = true;

        if (null == str || str.equals(""))
            System.out.println("OK");
        else {
            st = false;
            System.out.println("** "+str+" **");
        }
        return st;
    }
}
