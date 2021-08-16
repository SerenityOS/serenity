/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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

// 0 means no enclosing class
// - means anonymous enclosing class

class Test {
    void m1(int m1_arg) {
        String x = "Test; 0; 0";
        String y = "Test; 0; 0";
        String z = "Test; 0; 0";
        Object o = new Object() {
            public boolean equals(Object other) {
                String p = "-; Test; 0; 0";
                String q = "-; Test; 0; 0";
                String r = "-; Test; 0; 0";
                return (this == other);
            }
        };
    }

    String s = "Test; 0; 0";

    boolean b = new Object() {
            public boolean equals(Object other) {
                String p = "-; Test; 0; 0";
                String q = "-; Test; 0; 0";
                String r = "-; Test; 0; 0";
                return (this == other);
            }

    }.equals(null);

    class Test2 {
        String s = "Test.Test2; Test; 0; 0";
    }
}
