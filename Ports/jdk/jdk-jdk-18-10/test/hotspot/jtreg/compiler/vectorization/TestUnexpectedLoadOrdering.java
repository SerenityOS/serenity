/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
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
 * @bug 8201367
 * @summary RPO walk of counted loop block doesn't properly order loads
 * @requires vm.flavor == "server"
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseCountedLoopSafepoints TestUnexpectedLoadOrdering
 *
 */

public class TestUnexpectedLoadOrdering {

    public static void main(String[] args) {
        double[] array1 = new double[1000];
        double[] array2 = new double[1000];
        for (int i = 0; i < 20_000; i++) {
            test(array1, array2);
        }
    }

    private static double test(double[] array1, double[] array2) {
        double res = 0;
        for (int i = 0; i < array1.length; i++) {
            array2[i] = i;
            res += array1[i];
        }
        return res;
    }
}
