/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @summary Proof of concept test for RedefineClassHelper
 * @modules java.base/jdk.internal.misc
 * @modules java.compiler
 *          java.instrument
 * @run main RedefineClassHelper
 * @run main/othervm -javaagent:redefineagent.jar RedefineClassTest
 */

import static jdk.test.lib.Asserts.*;

/*
 * Proof of concept test for the test utility class RedefineClassHelper
 */

// package access top-level class to avoid problem with RedefineClassHelper
// and nested types.
class RedefineClassTest_A {
    public int Method() {
        return 1;
    }
}

public class RedefineClassTest {

    public static String newClass = "class RedefineClassTest_A { public int Method() { return 2; } }";
    public static void main(String[] args) throws Exception {
        RedefineClassTest_A a = new RedefineClassTest_A();
        assertTrue(a.Method() == 1);
        RedefineClassHelper.redefineClass(RedefineClassTest_A.class, newClass);
        assertTrue(a.Method() == 2);
    }
}
