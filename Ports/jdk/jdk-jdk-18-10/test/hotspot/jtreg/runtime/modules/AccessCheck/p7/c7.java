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

package p7;

import java.lang.Module;
import p2.c2;

public class c7 {
    public void method7(p2.c2 param, Module c2Mod) {
        try {
            // The invokedynamic opcode that gets generated for the '+' string
            // concatenation operator throws an IllegalAccessError when trying
            // to access 'param'.
            System.out.println("In c7's method7 with param = " + param);
            throw new java.lang.RuntimeException("c7 failed to throw expected IllegalAccessError");
        } catch (IllegalAccessError e) {
        }
        methodAddReadEdge(c2Mod);

        // This invokedynamic (for the string concat) should succeed because of
        // the added read edge.  The fact that the invokedynamic executed before
        // the read edge was added threw an IllegalAccessError exception should
        // not affect this one.
        try {
            System.out.println("In c7's method7 with param = " + param);
        } catch (IllegalAccessError e) {
            throw new java.lang.RuntimeException("Unexpected IllegalAccessError: " + e.getMessage());
        }
    }

    public void methodAddReadEdge(Module m) {
        // Add a read edge from p7/c7's module (third_mod) to module m.
        c7.class.getModule().addReads(m);
    }
}

