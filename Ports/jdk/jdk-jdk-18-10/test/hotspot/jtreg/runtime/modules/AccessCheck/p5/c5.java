/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package p5;

import java.lang.Module;
import p2.c2;

public class c5 {
    public void method5(p2.c2 param) {
        // The invokedynamic opcode that gets generated for the '+' string
        // concatenation operator throws an IllegalAccessError when trying to
        // access 'param'.
        System.out.println("In c5's method5 with param = " + param);
    }

    public void methodAddReadEdge(Module m) {
        // Add a read edge from p5/c5's module (first_mod) to second_mod
        c5.class.getModule().addReads(m);
    }
}
