/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4721003
 * @summary anonymous class in explicit constructor invocation can refer implicitly to encl
 *
 * @compile WhichImplicitThis9.java
 * @run main WhichImplicitThis9
 */

public class WhichImplicitThis9 {
    static int result;
    public synchronized static void main(String[] args) {
        result = 0;
        new WhichImplicitThis9(1);
        if (result != 13658) throw new Error("" + result);
    }
    WhichImplicitThis9(final int i) {
        class L {
            L() {
                result = result*10 + 1;
            }
            L(final int j) {
                this(new L() {
                        { result = result*10 + 2 + i; }
                });
                result = result*10 + 4 + i;
            }
            L(Object o) {
                result = result*10 + 6;
            }
        }
        new L(i) {
            {
                result = result*10 + 7 + i;
            }
        };
    }
}
