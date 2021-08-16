/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6855215
 * @summary Calculation error (NaN) after about 1500 calculations
 *
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:UseSSE=0 compiler.c1.Test6855215
 */

package compiler.c1;

public class Test6855215 {
    private double m;
    private double b;

    public static double log10(double x) {
        return Math.log(x) / Math.log(10);
    }

    void calcMapping(double xmin, double xmax, double ymin, double ymax) {
        m = (ymax - ymin) / (log10(xmax) - log10(xmin));
        b = (log10(xmin) * ymax - log10(xmax) * ymin);
    }

    public static void main(String[] args) {
        Test6855215 c = new Test6855215();
        for (int i = 0; i < 30000; i++) {
            c.calcMapping(91, 121, 177, 34);
            if (c.m != c.m) {
                throw new InternalError();
            }
        }
    }
}
