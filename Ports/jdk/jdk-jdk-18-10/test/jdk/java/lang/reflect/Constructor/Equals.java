/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7073296
 * @summary Generic framework to test Constructor.equals.
 *
 * @compile Equals.java
 * @run main Equals
 */

import java.lang.reflect.*;

public class Equals {
    public Equals(){}
    public Equals(Object o) {/* only for testing*/}
    public Equals(int i) {/* only for testing */}

    public Equals m() { return this; }

    public static void main(String [] args) {
        Equals e = new Equals();
        e.equalConstructors();
    }

    public void equalConstructors() {
        Constructor<?>[] constructors = Equals.class.getDeclaredConstructors();
        for(Constructor<?> ctor1 : constructors) {
            for(Constructor<?> ctor2 : constructors) {
                boolean expected = (ctor1 == ctor2);
                if (ctor1.equals(ctor2) != expected)
                    throw new RuntimeException("Constructors '" +
                        ctor1 + "'("+ System.identityHashCode(ctor1) + ") " +
                        (expected ? "!=" : "==") + " '" +
                        ctor2 + "'("+ System.identityHashCode(ctor2) + ")");
            }
        }
    }
}
