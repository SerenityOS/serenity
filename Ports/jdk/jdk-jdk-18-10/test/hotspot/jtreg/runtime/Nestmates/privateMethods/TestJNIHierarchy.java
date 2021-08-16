/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @compile ../NestmatesJNI.java
 * @run main/othervm/native  TestJNIHierarchy
 * @run main/othervm/native -Xcheck:jni TestJNIHierarchy
 */

public class TestJNIHierarchy {

    // Unlike reflection, the calling context is not relevant to JNI
    // calls, but we keep the same structure as the reflection tests.

    static final String METHOD = "priv_invoke";

    static class NestedA extends ExternalSuper {
        static final String ID =  "NestedA::priv_invoke";
        private String priv_invoke() {
            return ID;
        }
        static void checkA(NestedA a) {
            String res = NestmatesJNI.callStringVoid(a,
                                                     NestedA.class.getName(),
                                                     METHOD,
                                                     true);
            verifyEquals(res, NestedA.ID);
            res = NestmatesJNI.callStringVoid(a,
                                              NestedA.class.getName(),
                                              METHOD,
                                              false);
            verifyEquals(res, NestedA.ID);
        }
    }

    static class NestedB extends NestedA {
        static final String ID =  "NestedB::priv_invoke";
        private String priv_invoke() {
            return ID;
        }
        static void checkA(NestedA a) {
            String res = NestmatesJNI.callStringVoid(a,
                                                     NestedA.class.getName(),
                                                     METHOD,
                                                     true);
            verifyEquals(res, NestedA.ID);

            res = NestmatesJNI.callStringVoid(a,
                                              NestedA.class.getName(),
                                              METHOD,
                                              false);
            verifyEquals(res, NestedA.ID);
        }
    }

    static class NestedC extends NestedB {
        static final String ID =  "NestedC::priv_invoke";
        private String priv_invoke() {
            return ID;
        }
        static void checkA(NestedA a) {
            String res = NestmatesJNI.callStringVoid(a,
                                                     NestedA.class.getName(),
                                                     METHOD,
                                                     true);
            verifyEquals(res, NestedA.ID);
        }
    }

    static void checkA(NestedA a) {
        String res = NestmatesJNI.callStringVoid(a,
                                                 NestedA.class.getName(),
                                                 METHOD,
                                                 true);
        verifyEquals(res, NestedA.ID);

        res = NestmatesJNI.callStringVoid(a,
                                          NestedA.class.getName(),
                                          METHOD,
                                          false);
        verifyEquals(res, NestedA.ID);
    }

    static void checkB(NestedB b) {
        String res = NestmatesJNI.callStringVoid(b,
                                                 NestedB.class.getName(),
                                                 METHOD,
                                                 true);
        verifyEquals(res, NestedB.ID);

        res = NestmatesJNI.callStringVoid(b,
                                          NestedB.class.getName(),
                                          METHOD,
                                          false);
        verifyEquals(res, NestedB.ID);

    }

    static void checkC(NestedC c) {
        String res = NestmatesJNI.callStringVoid(c,
                                                 NestedC.class.getName(),
                                                 METHOD,
                                                 true);
        verifyEquals(res, NestedC.ID);

        res = NestmatesJNI.callStringVoid(c,
                                          NestedC.class.getName(),
                                          METHOD,
                                          false);
        verifyEquals(res, NestedC.ID);
    }


    // Access to private members of classes outside the nest is
    // not permitted in general, but JNI ignores all access checks.

    static void checkExternalSuper(ExternalSuper s) {
        String res = NestmatesJNI.callStringVoid(s,
                                                 ExternalSuper.class.getName(),
                                                 METHOD,
                                                 true);
        verifyEquals(res, ExternalSuper.ID);

        res = NestmatesJNI.callStringVoid(s,
                                          ExternalSuper.class.getName(),
                                          METHOD,
                                          false);
        verifyEquals(res, ExternalSuper.ID);
    }

    static void checkExternalSub(ExternalSub s) {
        String res = NestmatesJNI.callStringVoid(s,
                                                 ExternalSub.class.getName(),
                                                 METHOD,
                                                 true);
        verifyEquals(res, ExternalSub.ID);

        res = NestmatesJNI.callStringVoid(s,
                                          ExternalSub.class.getName(),
                                          METHOD,
                                          false);
        verifyEquals(res, ExternalSub.ID);
    }

    static void verifyEquals(String actual, String expected) {
        if (!actual.equals(expected)) {
            throw new Error("Expected " + expected + " but got " + actual);
        }
        System.out.println("Check passed for " + expected);
    }

    public static void main(String[] args) {
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

class ExternalSuper {
    static final String ID =  "ExternalSuper::priv_invoke";
    private String priv_invoke() {
        return ID;
    }
}


class ExternalSub extends TestJNIHierarchy.NestedC {
    static final String ID =  "ExternalSub::priv_invoke";
    private String priv_invoke() {
        return ID;
    }
}
