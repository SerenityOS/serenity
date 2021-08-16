/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7199742
 * @summary A lot of C2 OSR compilations of the same method's bci
 *
 * @run main/othervm -Xmx128m -Xbatch compiler.c2.Test7199742
 */

package compiler.c2;

public class Test7199742 {
    private static final int ITERS = 10000000;

    public static void main(String args[]) {
        Test7199742 t = new Test7199742();
        for (int i = 0; i < 10; i++) {
            test(t, 7);
        }
    }

    static Test7199742 test(Test7199742 t, int m) {
        int i = -(ITERS / 2);
        if (i == 0) return null;
        Test7199742 v = null;
        while (i < ITERS) {
            if ((i & m) == 0) {
                v = t;
            }
            i++;
        }
        return v;
    }
}
