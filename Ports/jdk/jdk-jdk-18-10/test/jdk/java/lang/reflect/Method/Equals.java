/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4913433
 * @summary Generic framework to test Method.equals.
 *
 * @clean A
 * @compile Equals.java
 * @run main Equals
 */

import java.lang.reflect.*;

class A {
    public Object m() { return this; }
}

public class Equals extends A {
    public Equals m() { return this; }

    public static void main(String [] args) {
        Equals e = new Equals();
        e.returnType();
    }

    private void returnType() {
        Class c = this.getClass();
        Method [] ma = c.getMethods();
        Method m0 = null, m1 = null;

        for (int i = 0; i < ma.length; i++) {
            if (ma[i].getName().equals("m")) {
                if (m0 == null) {
                    m0 = ma[i];
                    continue;
                } else {
                    m1 = ma[i];
                    break;
                }
            }
        }

        if (m0 == null || m1 == null)
            throw new RuntimeException("Can't find bridge methods");

        if (m0.equals(m1))
            throw new RuntimeException("Return types not compared");
        System.out.println("\"" + m0 + "\" and \"" + m1 + "\" are different");
    }
}
