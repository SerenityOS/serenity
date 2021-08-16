/*
 * Copyright (c) 2006, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import java.io.*;
//TOPLEVEL_SCOPE:List, Test2, Test; java.io.*, java.lang.*
class Test {
    void m1(int m1_arg) {
        String x = "x, m1_arg, super, this; List, Test2, Test; java.io.*, java.lang.*";
        String y = "y, x, m1_arg, super, this; List, Test2, Test; java.io.*, java.lang.*";
        String z = "z, y, x, m1_arg, super, this; List, Test2, Test; java.io.*, java.lang.*";
        Object o = new Object() {
            public boolean equals(Object other) {
                String p = "p, other, super, this; -, o, z, y, x, m1_arg, super, this; List, Test2, Test; java.io.*, java.lang.*";
                String q = "q, p, other, super, this; -, o, z, y, x, m1_arg, super, this; List, Test2, Test; java.io.*, java.lang.*";
                String r = "r, q, p, other, super, this; -, o, z, y, x, m1_arg, super, this; List, Test2, Test; java.io.*, java.lang.*";
                return (this == other);
            }
        };
    }

    String s = "super, this; List, Test2, Test; java.io.*, java.lang.*";

    boolean b = new Object() {
            public boolean equals(Object other) {
                String p = "p, other, super, this; -, super, this; List, Test2, Test; java.io.*, java.lang.*";
                String q = "q, p, other, super, this; -, super, this; List, Test2, Test; java.io.*, java.lang.*";
                String r = "r, q, p, other, super, this; -, super, this; List, Test2, Test; java.io.*, java.lang.*";
                return (this == other);
            }

    }.equals(null);
}

class Test2 { }
