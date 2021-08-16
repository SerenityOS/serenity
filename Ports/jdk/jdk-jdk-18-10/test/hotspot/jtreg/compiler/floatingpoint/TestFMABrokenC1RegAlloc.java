/*
 * Copyright (c) 2016, Red Hat, Inc. All rights reserved.
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
 * @bug 8171092
 * @summary C1's Math.fma() intrinsic doesn't correctly marks its inputs
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement TestFMABrokenC1RegAlloc
 *
 */

public class TestFMABrokenC1RegAlloc {

    double dummy0;
    double dummy1;
    double dummy2;
    double dummy3;
    double dummy4;
    double dummy5;
    double dummy6;
    double dummy7;
    double dummy8;
    double dummy9;
    double dummy10;
    double dummy11;
    double dummy12;
    double dummy13;
    double dummy14;
    double dummy15;
    double dummy16;
    double dummy17;
    double dummy18;
    double dummy19;
    double dummy20;
    double dummy21;
    double dummy22;
    double dummy23;
    double dummy24;
    double dummy25;
    double dummy26;
    double dummy27;
    double dummy28;
    double dummy29;
    double dummy30;
    double dummy31;
    double dummy32;
    double dummy33;
    double dummy34;
    double dummy35;
    double dummy36;
    double dummy37;
    double dummy38;
    double dummy39;

    double test(double a, double b, double c) {
        double dummy0 = this.dummy0;
        double dummy1 = this.dummy1;
        double dummy2 = this.dummy2;
        double dummy3 = this.dummy3;
        double dummy4 = this.dummy4;
        double dummy5 = this.dummy5;
        double dummy6 = this.dummy6;
        double dummy7 = this.dummy7;
        double dummy8 = this.dummy8;
        double dummy9 = this.dummy9;
        double dummy10 = this.dummy10;
        double dummy11 = this.dummy11;
        double dummy12 = this.dummy12;
        double dummy13 = this.dummy13;
        double dummy14 = this.dummy14;
        double dummy15 = this.dummy15;
        double dummy16 = this.dummy16;
        double dummy17 = this.dummy17;
        double dummy18 = this.dummy18;
        double dummy19 = this.dummy19;
        double dummy20 = this.dummy20;
        double dummy21 = this.dummy21;
        double dummy22 = this.dummy22;
        double dummy23 = this.dummy23;
        double dummy24 = this.dummy24;
        double dummy25 = this.dummy25;
        double dummy26 = this.dummy26;
        double dummy27 = this.dummy27;
        double dummy28 = this.dummy28;
        double dummy29 = this.dummy29;
        double dummy30 = this.dummy30;
        double dummy31 = this.dummy31;
        double dummy32 = this.dummy32;
        double dummy33 = this.dummy33;
        double dummy34 = this.dummy34;
        double dummy35 = this.dummy35;
        double dummy36 = this.dummy36;
        double dummy37 = this.dummy37;
        double dummy38 = this.dummy38;
        double dummy39 = this.dummy39;
        return Math.fma(a, b, c) +
            dummy0 +
            dummy1 +
            dummy2 +
            dummy3 +
            dummy4 +
            dummy5 +
            dummy6 +
            dummy7 +
            dummy8 +
            dummy9 +
            dummy10 +
            dummy11 +
            dummy12 +
            dummy13 +
            dummy14 +
            dummy15 +
            dummy16 +
            dummy17 +
            dummy18 +
            dummy19 +
            dummy20 +
            dummy21 +
            dummy22 +
            dummy23 +
            dummy24 +
            dummy25 +
            dummy26 +
            dummy27 +
            dummy28 +
            dummy29 +
            dummy30 +
            dummy31 +
            dummy32 +
            dummy33 +
            dummy34 +
            dummy35 +
            dummy36 +
            dummy37 +
            dummy38 +
            dummy39;
    }

    static public void main(String[] args) {
        TestFMABrokenC1RegAlloc t = new TestFMABrokenC1RegAlloc();
        for (int i = 0; i < 5000; i++) {
            if (t.test(5.0, 10.0, 7.0) != 57.0) {
                throw new RuntimeException("Failed");
            }
        }
    }
}
