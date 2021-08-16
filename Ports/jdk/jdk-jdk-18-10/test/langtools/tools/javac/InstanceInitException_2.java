/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4054256
 * @summary Verify that exceptions in instance initializer must be declared in all constructors.
 * @author William Maddox
 *
 * @run compile/fail InstanceInitException_2.java
 */

public class InstanceInitException_2 {

    int x = 1;

    class Exn1 extends Exception {}
    class Exn2 extends Exception {}

    class Inner {

        Inner() throws Exn1 {}

        Inner(int x) throws Exn1, Exn2 {}

        {
            if (x > 0) throw new Exn1();
            if (x > 0) throw new Exn2();  // error -- not declared in Inner()
        }

    }

}
