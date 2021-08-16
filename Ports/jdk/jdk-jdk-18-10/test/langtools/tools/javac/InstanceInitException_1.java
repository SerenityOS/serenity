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
 * @summary Verify that instance initializers can throw checked exceptions.
 * @author William Maddox
 *
 * @run compile InstanceInitException_1.java
 */

public class InstanceInitException_1 {

    int i = 1;

    InstanceInitException_1() throws Throwable {}

    {
        if (i > 0) throw new Throwable();
    }

    class Inner1 {

        Inner1() throws Throwable {}

        {
            if (i > 0) throw new Throwable();
        }

    }

    class Exn1 extends Throwable {}
    class Exn2 extends Exception {}

    class Inner2 {

        Inner2() throws Exn1, Exn2 {}

        {
            if (i > 0) throw new Exn1();
            if (i > 0) throw new Exn2();
        }

    }

    void test() throws Throwable {

        new Object() {
            {
                if (i > 0) throw new Throwable();
            }
        };

    }

}
