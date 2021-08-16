/*
 * Copyright (c) 1997, 1998, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4030421 4095716
 * @summary Verify that compiler can resolve local class names.
 * @author William Maddox (maddox)
 *
 * @clean LocalClasses_1a LocalClasses_1b
 * @compile LocalClasses_1.java
 */

class LocalClasses_1a {
    class Inner {
        void f() {
            class Local { }
            new Local();
        }
    }
}

class LocalClasses_1b {
    void f() {
        class Inner { }
        new Object() {
            {
                new Inner();
            }
        };
    }
}

class LocalClasses_1c {
    Object f() {
        class Local { }
        new Object() {
            int x;
            Local y;
            Local g() {
                return new Local();
            }
        };
        return new Local();
    }
}

class LocalClasses_1d {
    void f() {
        class Local { }
        class Inner {
            Local z;
        }
    }
}
