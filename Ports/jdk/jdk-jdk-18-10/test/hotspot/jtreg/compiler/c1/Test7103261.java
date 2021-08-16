/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7103261
 * @summary crash with jittester on sparc
 *
 * @run main compiler.c1.Test7103261
 */

package compiler.c1;

// exercise implicit null checking in the compiler for various field types
public class Test7103261 {
    static Test7103261 null_value;
    static Test7103261 nonnull_value = new Test7103261();
    static Test7103261 nonnull_value2 = new Test7103261();

    long l;
    int i;
    float f;
    double d;
    byte b;
    char c;
    short s;
    boolean z;
    Object o;

    public static void main(String[] args) {
        constantStore();
        valueTest(false);
        valueTest(true);
    }
    static void constantStore() {
        for (int field = 0; field < 9; field++) {
            try {
                Test7103261 o = nonnull_value;
                for (int i = 0; i < 100000; i++) {
                    switch (field) {
                    case 0: o.l = 0; break;
                    case 1: o.i = 0; break;
                    case 2: o.f = 0; break;
                    case 3: o.d = 0; break;
                    case 4: o.b = 0; break;
                    case 5: o.c = 0; break;
                    case 6: o.s = 0; break;
                    case 7: o.z = false; break;
                    case 8: o.o = null; break;
                    default: throw new InternalError();
                    }
                    if (i == 90000) {
                        // hide nullness from optimizer
                        o = null_value;
                    }
                }
            } catch (NullPointerException npe) {
            }
        }
    }
    static void valueTest(boolean store) {
        for (int field = 0; field < 9; field++) {
            try {
                Test7103261 o  = nonnull_value;
                Test7103261 o2 = nonnull_value2;
                for (int i = 0; i < 100000; i++) {
                    switch (field) {
                    case 0: o.l = o2.l; break;
                    case 1: o.i = o2.i; break;
                    case 2: o.f = o2.f; break;
                    case 3: o.d = o2.d; break;
                    case 4: o.b = o2.b; break;
                    case 5: o.c = o2.c; break;
                    case 6: o.s = o2.s; break;
                    case 7: o.z = o2.z; break;
                    case 8: o.o = o2.o; break;
                    default: throw new InternalError();
                    }
                    if (i == 90000) {
                        // hide nullness from optimizer
                        if (store)
                            o = null_value;
                        else
                            o2 = null_value;
                    }
                }
            } catch (NullPointerException npe) {
            }
        }
    }
}
