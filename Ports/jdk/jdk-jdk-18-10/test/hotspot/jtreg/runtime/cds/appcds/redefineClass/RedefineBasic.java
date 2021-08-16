/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import sun.hotspot.WhiteBox;

// package access top-level class to avoid problem with RedefineClassHelper
// and nested types.
class RedefineBasic_B {
    public static void okToCallBeforeRedefine() {
        System.out.println("okToCallBeforeRedefine");
    }
    public static void okToCallAfterRedefine() {
        throw new RuntimeException("okToCallAfterRedefine is called before redefinition, test failed");
    }
}

public class RedefineBasic {

    public static String newB =
        " class RedefineBasic_B { " +
        " public static void okToCallBeforeRedefine() { " +
        "    throw new RuntimeException(\"newB: okToCallBeforeRedefine is " +
        "    called after redefinition, test failed\"); }" +
        " public static void okToCallAfterRedefine() { " +
        "     System.out.println(\"newB: okToCallAfterRedefine\"); } " +
        " } ";



    static class SubclassOfB extends RedefineBasic_B {
        public static void testAfterRedefine() {
            RedefineBasic_B.okToCallAfterRedefine();
        }
    }

    class Subclass2OfB extends RedefineBasic_B {
        public void testAfterRedefine() {
            super.okToCallAfterRedefine();
        }
    }

    // verify that a given class is shared, report error if necessary
    public static void
    verifyClassIsShared(WhiteBox wb, Class c) throws Exception {
        if (!wb.isSharedClass(c)) {
            throw new RuntimeException(
            "This class should be shared but isn't: " + c.getName());
        } else {
            System.out.println("The class is shared as expected: " +
                c.getName());
        }
    }

    public static void main(String[] args) throws Exception {
        WhiteBox wb = WhiteBox.getWhiteBox();

        verifyClassIsShared(wb, RedefineBasic.class);
        verifyClassIsShared(wb, RedefineBasic_B.class);
        verifyClassIsShared(wb, SubclassOfB.class);
        verifyClassIsShared(wb, Subclass2OfB.class);

        // (1) Test case: verify that original B works as expected
        // and that redefined B is shared and works as expected,
        // with new behavior
        RedefineBasic_B.okToCallBeforeRedefine();
        RedefineClassHelper.redefineClass(RedefineBasic_B.class, newB);
        verifyClassIsShared(wb, RedefineBasic_B.class);
        RedefineBasic_B.okToCallAfterRedefine();

        // Static subclass of the super:
        // 1. Make sure it is still shared
        // 2. and it calls the correct super (the redefined one)
        verifyClassIsShared(wb, SubclassOfB.class);
        SubclassOfB.testAfterRedefine();

        // Same as above, but for non-static class
        verifyClassIsShared(wb, Subclass2OfB.class);
        RedefineBasic thisTest = new RedefineBasic();
        thisTest.testSubclass2OfB();
    }

    public void testSubclass2OfB() {
        Subclass2OfB sub = new Subclass2OfB();
        sub.testAfterRedefine();
    }
}
