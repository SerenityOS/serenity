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

/*
 * @test
 * @bug 8046171
 * @summary Test access to private methods between nestmates where there
 *          is an inheritance hierarchy and we invoke private methods that
 *          exist in specific classes in the hierarchy.
 * @compile TestInvokeHierarchy.java
 * @compile ExternalSuper.jcod ExternalSub.jcod
 * @run main TestInvokeHierarchy
 */

public class TestInvokeHierarchy {

    static class NestedA extends ExternalSuper {
        static final String ID =  "NestedA::priv_invoke";
        private String priv_invoke() {
            return ID;
        }
        static void checkA(NestedA a) throws Throwable {
            verifyEquals(a.priv_invoke(), NestedA.ID);
        }
    }

    static class NestedB extends NestedA {
        static final String ID =  "NestedB::priv_invoke";
        private String priv_invoke() {
            return ID;
        }
        static void checkA(NestedA a) throws Throwable {
            verifyEquals(a.priv_invoke(), NestedA.ID);
        }
    }

    static class NestedC extends NestedB {
        static final String ID =  "NestedC::priv_invoke";
        private String priv_invoke() {
            return ID;
        }
        static void checkA(NestedA a) throws Throwable {
            verifyEquals(a.priv_invoke(), NestedA.ID);
        }
    }

    static void checkA(NestedA a) throws Throwable {
        verifyEquals(a.priv_invoke(), NestedA.ID);
    }

    static void checkB(NestedB b) throws Throwable {
        verifyEquals(b.priv_invoke(), NestedB.ID);
    }

    static void checkC(NestedC c) throws Throwable {
        verifyEquals(c.priv_invoke(), NestedC.ID);
    }


    // Access to private members of classes outside the nest is
    // not permitted. These tests should throw IllegalAccessError
    // at runtime. To allow them to compile the classes below are
    // defined with public members. We then replace those class files
    // with jcod variants that make the member private again.

    static void checkExternalSuper(ExternalSuper s) throws Throwable {
        try {
            String str = s.priv_invoke_s();
            throw new Error("Unexpected access to ExternalSuper.priv_invoke_s");
        }
        catch (IllegalAccessError iae) {
            System.out.println("Got expected exception accessing ExternalSuper.priv_invoke_s:" + iae);
        }
    }

    static void checkExternalSub(ExternalSub s) throws Throwable {
        try {
            String str = s.priv_invoke();
            throw new Error("Unexpected access to ExternalSub.priv_invoke");
        }
        catch (IllegalAccessError iae) {
            System.out.println("Got expected exception accessing ExternalSub.priv_invoke:" + iae);
        }
    }

    static void verifyEquals(String actual, String expected) {
        if (!actual.equals(expected)) {
            throw new Error("Expected " + expected + " but got " + actual);
        }
        System.out.println("Check passed for " + expected);
    }

    public static void main(String[] args) throws Throwable {
        NestedA a = new NestedA();
        NestedB b = new NestedB();
        NestedC c = new NestedC();
        ExternalSub sub = new ExternalSub();
        ExternalSuper sup = new ExternalSuper();

        checkExternalSuper(sup);
        checkExternalSuper(a);
        checkExternalSuper(b);
        checkExternalSuper(c);
        checkExternalSuper(sub);

        checkA(a);
        checkA(b);
        checkA(c);
        checkA(sub);

        NestedA.checkA(a);
        NestedA.checkA(b);
        NestedA.checkA(c);
        NestedA.checkA(sub);

        NestedB.checkA(a);
        NestedB.checkA(b);
        NestedB.checkA(c);
        NestedB.checkA(sub);

        NestedC.checkA(a);
        NestedC.checkA(b);
        NestedC.checkA(c);
        NestedC.checkA(sub);

        checkB(b);
        checkB(c);
        checkB(sub);

        checkC(c);
        checkC(sub);

        checkExternalSub(sub);
    }
}

// Classes that are not part of the nest.
// Being non-public allows us to declare them in this file.
// The priv_invoke* member is public to allow this file to compile, but
// the jcod files change it back to private.

class ExternalSuper {
    static final String ID =  "ExternalSuper::priv_invoke_s";
    // Can't call this priv_invoke as subclasses make it less accessible
    // which is not allowed.
    public String priv_invoke_s() {
        return ID;
    }
}


class ExternalSub extends TestInvokeHierarchy.NestedC {
    static final String ID =  "ExternalSub::priv_invoke";
    public String priv_invoke() {
        return ID;
    }
}
