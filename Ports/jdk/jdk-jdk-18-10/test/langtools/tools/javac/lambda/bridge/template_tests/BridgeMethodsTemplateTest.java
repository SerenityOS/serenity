/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;

import org.testng.annotations.Test;

/**
 * BridgeMethodsTemplateTest
 *
 * @author Brian Goetz
 */
@Test
public class BridgeMethodsTemplateTest extends BridgeMethodTestCase {

    /*
     * Cc1(A) -> Cc1(Ac0)
     *
     * 0*: Inherited from A
     * 1: Declared in C
     */
    public void test1() throws IOException, ReflectiveOperationException {
        compileSpec("Cc1(A)");
        assertLinkage("C", LINKAGE_ERROR, "C1");
        recompileSpec("Cc1(Ac0)", "A");
        assertLinkage("C", "A0", "C1");
    }

    /*
     * Cc1(I) -> Cc1(Id0)
     *
     * 0*: Inherited default from I
     * 1: Declared in C
     */
    public void test2() throws IOException, ReflectiveOperationException {
        compileSpec("Cc1(I)");
        assertLinkage("C", LINKAGE_ERROR, "C1");
        recompileSpec("Cc1(Id0)", "I");
        assertLinkage("C", "I0", "C1");
    }

    /*
     * C(Bc1(A)) -> C(Bc1(Ac0))
     *
     * 0*: Inherited from A
     * 1: Inherited from B
     */
    public void test3() throws IOException, ReflectiveOperationException {
        compileSpec("C(Bc1(A))");
        assertLinkage("C", LINKAGE_ERROR, "B1");
        recompileSpec("C(Bc1(Ac0))", "A");
        assertLinkage("C", "A0", "B1");
    }

    /*
     * C(B(Ac0)) -> C(Bc1(Ac0))
     *
     * 0: Inherited from B (through bridge)
     * 1: Inherited from B
     */
    public void test4() throws IOException, ReflectiveOperationException {
        compileSpec("C(B(Ac0))");
        assertLinkage("C", "A0", LINKAGE_ERROR);
        recompileSpec("C(Bc1(Ac0))", "B");
        assertLinkage("C", "B1", "B1");
    }

    /*
     * C(B(A)) -> C(Bc1(Ac0))
     *
     * 0: Inherited from B (through bridge)
     * 1: Inherited from B
     */
    public void test5() throws IOException, ReflectiveOperationException {
        compileSpec("C(B(A))");
        assertLinkage("C", LINKAGE_ERROR, LINKAGE_ERROR);
        recompileSpec("C(Bc1(Ac0))", "A", "B");
        assertLinkage("C", "B1", "B1");
    }

    /*
     * C(Ac1(I)) -> C(Ac1(Id0))
     *
     * 0*: Inherited default from I
     * 1: Inherited from A
     */
    public void test6() throws IOException, ReflectiveOperationException {
        compileSpec("C(Ac1(I))");
        assertLinkage("C", LINKAGE_ERROR, "A1");
        recompileSpec("C(Ac1(Id0))", "I");
        assertLinkage("C", "I0", "A1");
    }

    /*
     * C(A(Id0)) -> C(Ac1(Id0))
     *
     * 0: Inherited from A (through bridge)
     * 1: Inherited from A
     */
    public void test7() throws IOException, ReflectiveOperationException {
        compileSpec("C(A(Id0))");
        assertLinkage("C", "I0", LINKAGE_ERROR);
        recompileSpec("C(Ac1(Id0))", "A");
        assertLinkage("C", "A1", "A1");
    }

    /*
     * C(A(I)) -> C(Ac1(Id0))
     *
     * 0*: Inherited from A (through bridge)
     * 1*: Inherited from A
     */
    public void test8() throws IOException, ReflectiveOperationException {
        compileSpec("C(A(I))");
        assertLinkage("C", LINKAGE_ERROR, LINKAGE_ERROR);
        recompileSpec("C(Ac1(Id0))", "A", "I");
        assertLinkage("C", "A1", "A1");
    }

    /*
     * C(Id1(J)) -> C(Id1(Jd0))
     *
     * 0*: Inherited default from J
     * 1: Inherited default from I
     */
    public void test9() throws IOException, ReflectiveOperationException {
        compileSpec("C(Id1(J))");
        assertLinkage("C", LINKAGE_ERROR, "I1");
        recompileSpec("C(Id1(Jd0))", "J");
        assertLinkage("C", "J0", "I1");
    }

    /*
     * C(I(Jd0)) -> C(Id1(Jd0))
     *
     * 0: Inherited default from I (through bridge)
     * 1: Inherited default from I
     */
    public void test10() throws IOException, ReflectiveOperationException {
        compileSpec("C(I(Jd0))");
        assertLinkage("C", "J0", LINKAGE_ERROR);
        recompileSpec("C(Id1(Jd0))", "I");
        assertLinkage("C", "I1", "I1");
    }

    /*
     * C(I(J)) -> C(Id1(Jd0))
     *
     * 0: Inherited default from I (through bridge)
     * 1: Inherited default from I
     */
    public void test11() throws IOException, ReflectiveOperationException {
        compileSpec("C(I(J))");
        assertLinkage("C", LINKAGE_ERROR, LINKAGE_ERROR);
        recompileSpec("C(Id1(Jd0))", "I", "J");
        assertLinkage("C", "I1", "I1");
    }

    /*
     * Cc2(B(Ac0)) -> Cc2(Bc1(Ac0))
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited from B
     * 2: Declared in C
     */
    public void test12() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(B(Ac0))");
        assertLinkage("C", "C2", LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(Bc1(Ac0))", "B");
        assertLinkage("C", "C2", "B1", "C2");
    }

    /*
     * Cc2(B(Aa0)) -> Cc2(Bc1(Aa0))
     *
     * 0: Bridge in C (through bridge)
     * 1*: Inherited from B
     * 2: Declared in C
     */
    public void test13() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(B(Aa0))");
        assertLinkage("C", "C2", LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(Bc1(Aa0))", "B");
        assertLinkage("C", "C2", "B1", "C2");
    }

    /*
     * Cc2(Bc1(A)) -> Cc2(Bc1(Ac0))
     *
     * 0*: Inherited from A
     * 1: Declared in C (through bridge)
     * 2: Declared in C
     */
    public void test14() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(Bc1(A))");
        assertLinkage("C", LINKAGE_ERROR, "C2", "C2");
        recompileSpec("Cc2(Bc1(Ac0))", "A");
        assertLinkage("C", "A0", "C2", "C2");
    }

    /*
     * Cc2(Ba1(A)) -> Cc2(Ba1(Ac0))
     *
     * 0*: Inherited from A
     * 1: Declared in C (through bridge)
     * 2: Declared in C
     */
    public void test15() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(Ba1(A))");
        assertLinkage("C", LINKAGE_ERROR, "C2", "C2");
        recompileSpec("Cc2(Ba1(Ac0))", "A");
        assertLinkage("C", "A0", "C2", "C2");
    }

    /*
     * Cc2(B(A)) -> Cc2(Bc1(Ac0))
     *
     * 0*: Inherited from B (through bridge)
     * 1*: Inherited from B
     * 2: Declared in C
     */
    public void test16() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(B(A))");
        assertLinkage("C", LINKAGE_ERROR, LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(Bc1(Ac0))", "B", "A");
        assertLinkage("C", "B1", "B1", "C2");
    }

    /*
     * Cc2(A(Id0)) -> Cc2(Ac1(Id0))
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited from A
     * 2: Declared in C
     */
    public void test17() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(A(Id0))");
        assertLinkage("C", "C2", LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(Ac1(Id0))", "A");
        assertLinkage("C", "C2", "A1", "C2");
    }

    /*
     * Cc2(A(Ia0)) -> Cc2(Ac1(Ia0))
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited from A
     * 2: Declared in C
     */
    public void test18() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(A(Ia0))");
        assertLinkage("C", "C2", LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(Ac1(Ia0))", "A");
        assertLinkage("C", "C2", "A1", "C2");
    }

    /*
     * Cc2(Ac1(I)) -> Cc2(Ac1(Id0))
     *
     * 0*: Inherited from I
     * 1: Declared in C (through bridge)
     * 2: Declared in C
     */
    public void test19() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(Ac1(I))");
        assertLinkage("C", LINKAGE_ERROR, "C2", "C2");
        recompileSpec("Cc2(Ac1(Id0))", "I");
        assertLinkage("C", "I0", "C2", "C2");
    }

    /*
     * Cc2(Aa1(I)) -> Cc2(Aa1(Id0))
     *
     * 0*: Inherited from I
     * 1: Declared in C (through bridge)
     * 2: Declared in C
     */
    public void test20() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(Aa1(I))");
        assertLinkage("C", LINKAGE_ERROR, "C2", "C2");
        recompileSpec("Cc2(Aa1(Id0))", "I");
        assertLinkage("C", "I0", "C2", "C2");
    }

    /*
     * Cc2(A(I)) -> Cc2(Ac1(Id0))
     *
     * 0*: Inherited from A (through bridge)
     * 1*: Inherited from A
     * 2: Declared in C
     */
    public void test21() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(A(I))");
        assertLinkage("C", LINKAGE_ERROR, LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(Ac1(Id0))", "A", "I");
        assertLinkage("C", "A1", "A1", "C2");
    }

    /*
     * Cc2(J(Id0)) -> Cc2(Jd1(Id0))
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited default from J
     * 2: Declared in C
     */
    public void test22() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(J(Id0))");
        assertLinkage("C", "C2", LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(Jd1(Id0))", "J");
        assertLinkage("C", "C2", "J1", "C2");
    }

    /*
     * Cc2(J(Ia0)) -> Cc2(Jd1(Ia0))
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited default from J
     * 2: Declared in C
     */
    public void test23() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(J(Ia0))");
        assertLinkage("C", "C2", LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(Jd1(Ia0))", "J");
        assertLinkage("C", "C2", "J1", "C2");
    }

    /*
     * Cc2(Jd1(I)) -> Cc2(Jd1(Id0))
     *
     * 0*: Inherited default from I
     * 1: Declared in C (through bridge)
     * 2: Declared in C
     */
    public void test24() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(Jd1(I))");
        assertLinkage("C", LINKAGE_ERROR, "C2", "C2");
        recompileSpec("Cc2(Jd1(Id0))", "I");
        assertLinkage("C", "I0", "C2", "C2");
    }

    /*
     * Cc2(Ja1(I)) -> Cc2(Ja1(Id0))
     *
     * 0*: Inherited default from I
     * 1: Declared in C (through bridge)
     * 2: Declared in C
     */
    public void test25() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(Ja1(I))");
        assertLinkage("C", LINKAGE_ERROR, "C2", "C2");
        recompileSpec("Cc2(Ja1(Id0))", "I");
        assertLinkage("C", "I0", "C2", "C2");
    }

    /*
     * Cc2(J(I)) -> Cc2(Jd1(Id0))
     *
     * 0*: Inherited default from J (through bridge)
     * 1*: Inherited default from J
     * 2: Declared in C
     */
    public void test26() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(J(I))");
        assertLinkage("C", LINKAGE_ERROR, LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(Jd1(Id0))", "J", "I");
        assertLinkage("C", "J1", "J1", "C2");
    }

    /*
     * C(Ac1, I) -> C(Ac1, Id0)
     *
     * 0*: Inherited default from I
     * 1: Inherited from A
     */
    public void test27() throws IOException, ReflectiveOperationException {
        compileSpec("C(Ac1,I)");
        assertLinkage("C", LINKAGE_ERROR, "A1");
        recompileSpec("C(Ac1,Id0)", "I");
        assertLinkage("C", "I0", "A1");
    }

    /*
     * C(A, Id0) -> C(Ac1, Id0)
     *
     * 0*: Inherited default from I
     * 1: Inherited from A
     */
    public void test28() throws IOException, ReflectiveOperationException {
        compileSpec("C(A,Id0)");
        assertLinkage("C", "I0", LINKAGE_ERROR);
        recompileSpec("C(Ac1,Id0)", "A");
        assertLinkage("C", "I0", "A1");
    }

    /*
     * C(A, I) -> C(Ac1, Id0)
     *
     * 0*: Inherited default from I
     * 1: Inherited from A
     */
    public void test29() throws IOException, ReflectiveOperationException {
        compileSpec("C(A,I)");
        assertLinkage("C", LINKAGE_ERROR, LINKAGE_ERROR);
        recompileSpec("C(Ac1,Id0)", "A", "I");
        assertLinkage("C", "I0", "A1");
    }

    /*
     * Cc2(Ac1, I) -> Cc2(Ac1, Id0)
     *
     * 0*: Inherited default from I
     * 1: Declared in C (through bridge)
     * 2: Declared in C
     */
    public void test30() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(Ac1,I)");
        assertLinkage("C", LINKAGE_ERROR, "C2", "C2");
        recompileSpec("Cc2(Ac1,Id0)", "I");
        assertLinkage("C", "I0", "C2", "C2");
    }

    /*
     * Cc2(Aa1, I) -> Cc2(Aa1, Id0)
     *
     * 0*: Inherited default from I
     * 1: Declared in C (through bridge)
     * 2: Declared in C
     */
    public void test31() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(Aa1,I)");
        assertLinkage("C", LINKAGE_ERROR, "C2", "C2");
        recompileSpec("Cc2(Aa1,Id0)", "I");
        assertLinkage("C", "I0", "C2", "C2");
    }

    /*
     * Cc2(A, Id0) -> Cc2(Ac1, Id0)
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited from A
     * 2: Declared in C
     */
    public void test32() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(A,Id0)");
        assertLinkage("C", "C2", LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(Ac1,Id0)", "A");
        assertLinkage("C", "C2", "A1", "C2");
    }

    /*
     * Cc2(A, Ia0) -> Cc2(Ac1, Ia0)
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited from A
     * 2: Declared in C
     */
    public void test33() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(A,Ia0)");
        assertLinkage("C", "C2", LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(Ac1,Ia0)", "A");
        assertLinkage("C", "C2", "A1", "C2");
    }

    /*
     * Cc2(A, I) -> Cc2(Ac1, Id0)
     *
     * 0*: Inherited from A
     * 1*: Inherited default from I
     * 2: Declared in C
     */
    public void test34() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(A,I)");
        assertLinkage("C", LINKAGE_ERROR, LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(Ac1,Id0)", "A", "I");
        assertLinkage("C", "I0", "A1", "C2");
    }

    /*
     * Cc2(Id0, J) -> Cc2(Id0, Jd1)
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited default from J
     * 2: Declared in C
     */
    public void test35() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(Id0,J)");
        assertLinkage("C", "C2", LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(Id0,Jd1)", "J");
        assertLinkage("C", "C2", "J1", "C2");
    }

    /*
     * Cc2(Ia0, J) -> Cc2(Ia0, Jd1)
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited default from J
     * 2: Declared in C
     */
    public void test36() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(Ia0,J)");
        assertLinkage("C", "C2", LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(Ia0,Jd1)", "J");
        assertLinkage("C", "C2", "J1", "C2");
    }

    /*
     * Cc2(I, J) -> Cc2(Id0, Jd1)
     *
     * 0*: Inherited default from I
     * 1*: Inherited default from J
     * 2: Declared in C
     */
    public void test37() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(I,J)");
        assertLinkage("C", LINKAGE_ERROR, LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(Id0,Jd1)", "I", "J");
        assertLinkage("C", "I0", "J1", "C2");
    }

    /*
     * C(A(Id0), J(Id0)) -> C(Ac1(Id0), J(Id0))
     *
     * 0: Inherited default from I
     * 0*: Inherited from A (through bridge)
     * 1*: Inherited from A
     */
    public void test38() throws IOException, ReflectiveOperationException {
        compileSpec("C(A(Id0),J(Id0))");
        assertLinkage("C", "I0", LINKAGE_ERROR);
        recompileSpec("C(Ac1(Id0),J(Id0))", "A");
        assertLinkage("C", "A1", "A1");
    }

    /*
     * C(A(Id0), J(Id0)) -> C(A(Id0), Jd1(Id0))
     *
     * 0: Inherited default from I
     * 0: Inherited default from J (through bridge)
     * 1*: Inherited default from J
     */
    public void test39() throws IOException, ReflectiveOperationException {
        compileSpec("C(A(Id0),J(Id0))");
        assertLinkage("C", "I0", LINKAGE_ERROR);
        recompileSpec("C(A(Id0),Jd1(Id0))", "J");
        assertLinkage("C", "J1", "J1");
    }

    /*
     * C(A(Id0), J(Id0)) -> C(Ac2(Id0), Jd1(Id0))
     *
     * 0: Inherited default from I
     * 0*: Inherited from A (new bridge in A beats new bridge in J)
     * 1*: Inherited default from J
     * 2: Inherited from A
     */
    public void test40() throws IOException, ReflectiveOperationException {
        compileSpec("C(A(Id0),J(Id0))");
        assertLinkage("C", "I0", LINKAGE_ERROR);
        recompileSpec("C(Ac2(Id0),Jd1(Id0))", "A", "J");
        assertLinkage("C", "A2", "J1", "A2");
    }

    /*
     * C(J(Id0), K(Id0)) -> C(Jd1(Id0), K(Id0))
     *
     * 0: Inherited from I
     * 0*: Inherited default from J (through bridge)
     * 1: Inherited default from J
     */
    public void test41() throws IOException, ReflectiveOperationException {
        compileSpec("C(J(Id0),K(Id0))");
        assertLinkage("C", "I0", LINKAGE_ERROR);
        recompileSpec("C(Jd1(Id0),K(Id0))", "J");
        assertLinkage("C", "J1", "J1");
    }

    /*
     * C(Ac2(Id0), J(Id0)) -> C(Ac2(Id0), Jd1(Id0))
     *
     * 0: Inherited from A (bridge in A beats new bridge in J)
     * 1*: Inherited default from J
     * 2: Inherited from A
     */
    public void test42() throws IOException, ReflectiveOperationException {
        compileSpec("C(Ac2(Id0),J(Id0))");
        assertLinkage("C", "A2", LINKAGE_ERROR, "A2");
        recompileSpec("C(Ac2(Id0),Jd1(Id0))", "J");
        assertLinkage("C", "A2", "J1", "A2");
    }

    /*
     * C(Ac2(Ia0), J(Ia0)) -> C(Ac2(Ia0), Jd1(Ia0))
     *
     * 0: Inherited from A (bridge in A beats new bridge in J)
     * 1*: Inherited default from J
     * 2: Inherited from A
     */
    public void test43() throws IOException, ReflectiveOperationException {
        compileSpec("C(Ac2(Ia0),J(Ia0))");
        assertLinkage("C", "A2", LINKAGE_ERROR, "A2");
        recompileSpec("C(Ac2(Ia0),Jd1(Ia0))", "J");
        assertLinkage("C", "A2", "J1", "A2");
    }

    /*
     * C(A(Id0), Jd1(Id0)) -> C(Ac2(Id0), Jd1(Id0))
     *
     * 0: Inherited from J
     * 0*: Inherited from A (new bridge in A beats bridge in J)
     * 1: Inherited default from J
     * 2*: Inherited from A
     */
    public void test44() throws IOException, ReflectiveOperationException {
        compileSpec("C(A(Id0),Jd1(Id0))");
        assertLinkage("C", "J1", "J1", LINKAGE_ERROR);
        recompileSpec("C(Ac2(Id0),Jd1(Id0))", "A");
        assertLinkage("C", "A2", "J1", "A2");
    }

    /*
     * C(A(Ia0), Jd1(Ia0)) -> C(Ac2(Ia0), Jd1(Ia0))
     *
     * 0: Inherited from J
     * 0*: Inherited from A (new bridge in A beats bridge in J)
     * 1: Inherited default from J
     * 2*: Inherited from A
     */
    public void test45() throws IOException, ReflectiveOperationException {
        compileSpec("C(A(Ia0),Jd1(Ia0))");
        assertLinkage("C", "J1", "J1", LINKAGE_ERROR);
        recompileSpec("C(Ac2(Ia0),Jd1(Ia0))", "A");
        assertLinkage("C", "A2", "J1", "A2");
    }

    /*
     * Cc2(A(Id0), J(Id0)) -> Cc2(Ac1(Id0), J(Id0))
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited from A
     * 2: Declared in C
     */
    public void test46() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(A(Id0),J(Id0))");
        assertLinkage("C", "C2", LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(Ac1(Id0),J(Id0))", "A");
        assertLinkage("C", "C2", "A1", "C2");
    }

    /*
     * Cc2(A(Ia0), J(Ia0)) -> Cc2(Ac1(Ia0), J(Ia0))
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited from A
     * 2: Declared in C
     */
    public void test47() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(A(Ia0),J(Ia0))");
        assertLinkage("C", "C2", LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(Ac1(Ia0),J(Ia0))", "A");
        assertLinkage("C", "C2", "A1", "C2");
    }

    /*
     * Cc2(A(Id0), J(Id0)) -> Cc2(A(Id0), Jd1(Id0))
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited default from J
     * 2: Declared in C
     */
    public void test48() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(A(Id0),J(Id0))");
        assertLinkage("C", "C2", LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(A(Id0),Jd1(Id0))", "J");
        assertLinkage("C", "C2", "J1", "C2");
    }

    /*
     * Cc2(A(Ia0), J(Ia0)) -> Cc2(A(Ia0), Jd1(Ia0))
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited default from J
     * 2: Declared in C
     */
    public void test49() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(A(Ia0),J(Ia0))");
        assertLinkage("C", "C2", LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(A(Ia0),Jd1(Ia0))", "J");
        assertLinkage("C", "C2", "J1", "C2");
    }


    /*
     * Cc3(A(Id0), J(Id0)) -> Cc3(Ac1(Id0), Jd2(Id0))
     *
     * 0: Bridge in C
     * 1*: Inherited from A
     * 2*: Inherited default from J
     * 3: Declared in C
     */
    public void test50() throws IOException, ReflectiveOperationException {
        compileSpec("Cc3(A(Id0),J(Id0))");
        assertLinkage("C", "C3", LINKAGE_ERROR, LINKAGE_ERROR, "C3");
        recompileSpec("Cc3(Ac1(Id0),Jd2(Id0))", "A", "J");
        assertLinkage("C", "C3", "A1", "J2", "C3");
    }

    /*
     * Cc3(A(Ia0), J(Ia0)) -> Cc3(Ac1(Ia0), Jd2(Ia0))
     *
     * 0: Bridge in C
     * 1*: Inherited from A
     * 2*: Inherited default from J
     * 3: Declared in C
     */
    public void test51() throws IOException, ReflectiveOperationException {
        compileSpec("Cc3(A(Ia0),J(Ia0))");
        assertLinkage("C", "C3", LINKAGE_ERROR, LINKAGE_ERROR, "C3");
        recompileSpec("Cc3(Ac1(Ia0),Jd2(Ia0))", "A", "J");
        assertLinkage("C", "C3", "A1", "J2", "C3");
    }

    /*
     * Cc2(J(Id0), K(Id0)) -> Cc2(Jd1(Id0), K(Id0))
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited default from J
     * 2: Declared in C
     */
    public void test52() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(J(Id0),K(Id0))");
        assertLinkage("C", "C2", LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(Jd1(Id0),K(Id0))", "J");
        assertLinkage("C", "C2", "J1", "C2");
    }

    /*
     * Cc2(J(Ia0), K(Ia0)) -> Cc2(Jd1(Ia0), K(Ia0))
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited default from J
     * 2: Declared in C
     */
    public void test53() throws IOException, ReflectiveOperationException {
        compileSpec("Cc2(J(Ia0),K(Ia0))");
        assertLinkage("C", "C2", LINKAGE_ERROR, "C2");
        recompileSpec("Cc2(Jd1(Ia0),K(Ia0))", "J");
        assertLinkage("C", "C2", "J1", "C2");
    }

    /*
     * Cc3(J(Id0), K(Id0)) -> Cc3(Jd1(Id0), Kd2(Id0))
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited default from J
     * 2*: Inherited default from K
     * 3: Declared in C
     */
    public void test54() throws IOException, ReflectiveOperationException {
        compileSpec("Cc3(J(Id0),K(Id0))");
        assertLinkage("C", "C3", LINKAGE_ERROR, LINKAGE_ERROR, "C3");
        recompileSpec("Cc3(Jd1(Id0),Kd2(Id0))", "J", "K");
        assertLinkage("C", "C3", "J1", "K2", "C3");
    }

    /*
     * Cc3(J(Ia0), K(Ia0)) -> Cc3(Jd1(Ia0), Kd2(Ia0))
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited default from J
     * 2*: Inherited default from K
     * 3: Declared in C
     */
    public void test55() throws IOException, ReflectiveOperationException {
        compileSpec("Cc3(J(Ia0),K(Ia0))");
        assertLinkage("C", "C3", LINKAGE_ERROR, LINKAGE_ERROR, "C3");
        recompileSpec("Cc3(Jd1(Ia0),Kd2(Ia0))", "J", "K");
        assertLinkage("C", "C3", "J1", "K2", "C3");
    }

    /*
     * Cc3(Ac1(Id0), J(Id0)) -> Cc3(Ac1(Id0), Jd2(Id0))
     *
     * 0: Declared in C (through bridge)
     * 1: Declared in C (through bridge)
     * 2*: Inherited default from J
     * 3: Declared in C
     */
    public void test56() throws IOException, ReflectiveOperationException {
        compileSpec("Cc3(Ac1(Id0),J(Id0))");
        assertLinkage("C", "C3", "C3", LINKAGE_ERROR, "C3");
        recompileSpec("Cc3(Ac1(Id0),Jd2(Id0))", "J");
        assertLinkage("C", "C3", "C3", "J2", "C3");
    }

    /*
     * Cc3(Ac1(Ia0), J(Ia0)) -> Cc3(Ac1(Ia0), Jd2(Ia0))
     *
     * 0: Declared in C (through bridge)
     * 1: Declared in C (through bridge)
     * 2*: Inherited default from J
     * 3: Declared in C
     */
    public void test57() throws IOException, ReflectiveOperationException {
        compileSpec("Cc3(Ac1(Ia0),J(Ia0))");
        assertLinkage("C", "C3", "C3", LINKAGE_ERROR, "C3");
        recompileSpec("Cc3(Ac1(Ia0),Jd2(Ia0))", "J");
        assertLinkage("C", "C3", "C3", "J2", "C3");
    }

    /*
     * Cc3(Aa1(Id0), J(Id0)) -> Cc3(Aa1(Id0), Jd2(Id0))
     *
     * 0: Declared in C (through bridge)
     * 1: Declared in C (through bridge)
     * 2*: Inherited default from J
     * 3: Declared in C
     */
    public void test58() throws IOException, ReflectiveOperationException {
        compileSpec("Cc3(Aa1(Id0),J(Id0))");
        assertLinkage("C", "C3", "C3", LINKAGE_ERROR, "C3");
        recompileSpec("Cc3(Aa1(Id0),Jd2(Id0))", "J");
        assertLinkage("C", "C3", "C3", "J2", "C3");
    }

    /*
     * Cc3(Aa1(Ia0), J(Ia0)) -> Cc3(Aa1(Ia0), Jd2(Ia0))
     *
     * 0: Declared in C (through bridge)
     * 1: Declared in C (through bridge)
     * 2*: Inherited default from J
     * 3: Declared in C
     */
    public void test59() throws IOException, ReflectiveOperationException {
        compileSpec("Cc3(Aa1(Ia0),J(Ia0))");
        assertLinkage("C", "C3", "C3", LINKAGE_ERROR, "C3");
        recompileSpec("Cc3(Aa1(Ia0),Jd2(Ia0))", "J");
        assertLinkage("C", "C3", "C3", "J2", "C3");
    }

    /*
     * Cc3(A(Id0), Jd2(Id0)) -> Cc3(Ac1(Id0), Jd2(Id0))
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited from A
     * 2: Declared in C (through bridge)
     * 3: Declared in C
     */
    public void test60() throws IOException, ReflectiveOperationException {
        compileSpec("Cc3(A(Id0),Jd2(Id0))");
        assertLinkage("C", "C3", LINKAGE_ERROR, "C3", "C3");
        recompileSpec("Cc3(Ac1(Id0),Jd2(Id0))", "A");
        assertLinkage("C", "C3", "A1", "C3", "C3");
    }

    /*
     * Cc3(A(Im0), Jd2(Ia0)) -> Cc3(Ac1(Im0), Jd2(Ia0))
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited from A
     * 2: Declared in C (through bridge)
     * 3: Declared in C
     */
    public void test61() throws IOException, ReflectiveOperationException {
        compileSpec("Cc3(A(Ia0),Jd2(Ia0))");
        assertLinkage("C", "C3", LINKAGE_ERROR, "C3", "C3");
        recompileSpec("Cc3(Ac1(Ia0),Jd2(Ia0))", "A");
        assertLinkage("C", "C3", "A1", "C3", "C3");
    }

    /*
     * Cc3(A(Im0), Ja2(Id0)) -> Cc3(Ac1(Id0), Ja2(Id0))
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited from A
     * 2: Declared in C (through bridge)
     * 3: Declared in C
     */
    public void test62() throws IOException, ReflectiveOperationException {
        compileSpec("Cc3(A(Id0),Ja2(Id0))");
        assertLinkage("C", "C3", LINKAGE_ERROR, "C3", "C3");
        recompileSpec("Cc3(Ac1(Id0),Ja2(Id0))", "A");
        assertLinkage("C", "C3", "A1", "C3", "C3");
    }

    /*
     * Cc3(A(Im0), Ja2(Ia0)) -> Cc3(Ac1(Ia0), Ja2(Ia0))
     *
     * 0: Declared in C (through bridge)
     * 1*: Inherited from A
     * 2: Declared in C (through bridge)
     * 3: Declared in C
     */
    public void test63() throws IOException, ReflectiveOperationException {
        compileSpec("Cc3(A(Ia0),Ja2(Ia0))");
        assertLinkage("C", "C3", LINKAGE_ERROR, "C3", "C3");
        recompileSpec("Cc3(Ac1(Ia0),Ja2(Ia0))", "A");
        assertLinkage("C", "C3", "A1", "C3", "C3");
    }

    /*
     * Cc3(Jd1(Id0), K(Id0)) -> Cc3(Jd1(Id0), Kd2(Id0))
     *
     * 0: Declared in C (through bridge)
     * 1: Declared in C (through bridge)
     * 2*: Inherited default from K
     * 3: Declared in C
     */
    public void test64() throws IOException, ReflectiveOperationException {
        compileSpec("Cc3(Jd1(Id0),K(Id0))");
        assertLinkage("C", "C3", "C3", LINKAGE_ERROR, "C3");
        recompileSpec("Cc3(Jd1(Id0),Kd2(Id0))", "K");
        assertLinkage("C", "C3", "C3", "K2", "C3");
    }

    /*
     * Cc3(Jd1(Ia0), K(Ia0)) -> Cc3(Jd1(Ia0), Kd2(Ia0))
     *
     * 0: Declared in C (through bridge)
     * 1: Declared in C (through bridge)
     * 2*: Inherited default from K
     * 3: Declared in C
     */
    public void test65() throws IOException, ReflectiveOperationException {
        compileSpec("Cc3(Jd1(Ia0),K(Ia0))");
        assertLinkage("C", "C3", "C3", LINKAGE_ERROR, "C3");
        recompileSpec("Cc3(Jd1(Ia0),Kd2(Ia0))", "K");
        assertLinkage("C", "C3", "C3", "K2", "C3");
    }

    /*
     * Cc3(Ja1(Id0), K(Id0)) -> Cc3(Ja1(Id0), Kd2(Id0))
     *
     * 0: Declared in C (through bridge)
     * 1: Declared in C (through bridge)
     * 2*: Inherited default from K
     * 3: Declared in C
     */
    public void test66() throws IOException, ReflectiveOperationException {
        compileSpec("Cc3(Jd1(Id0),K(Id0))");
        assertLinkage("C", "C3", "C3", LINKAGE_ERROR, "C3");
        recompileSpec("Cc3(Jd1(Id0),Kd2(Id0))", "K");
        assertLinkage("C", "C3", "C3", "K2", "C3");
    }

    /*
     * Cc3(Ja1(Ia0), K(Ia0)) -> Cc3(Ja1(Ia0), Kd2(Ia0))
     *
     * 0: Declared in C (through bridge)
     * 1: Declared in C (through bridge)
     * 2*: Inherited default from K
     * 3: Declared in C
     */
    public void test67() throws IOException, ReflectiveOperationException {
        compileSpec("Cc3(Jd1(Ia0),K(Ia0))");
        assertLinkage("C", "C3", "C3", LINKAGE_ERROR, "C3");
        recompileSpec("Cc3(Jd1(Ia0),Kd2(Ia0))", "K");
        assertLinkage("C", "C3", "C3", "K2", "C3");
    }

    // Dan's set A
    public void testA1() throws IOException, ReflectiveOperationException {
        compileSpec("C(Id0)");
        assertLinkage("C", "I0");
    }

    public void testA2() throws IOException, ReflectiveOperationException {
        compileSpec("C(A(Id0))");
        assertLinkage("C", "I0");
    }

    public void testA3() throws IOException, ReflectiveOperationException {
        compileSpec("C(A(Id0),J)");
        assertLinkage("C", "I0");
    }

    public void testA4() throws IOException, ReflectiveOperationException {
        compileSpec("D(C(Id0),Jd0(Id0))");
        assertLinkage("D", "J0");
        assertLinkage("C", "I0");
    }

    public void testA5() throws IOException, ReflectiveOperationException {
        compileSpec("C(A(Id0),Jd0)",
                    "compiler.err.types.incompatible");
    }

    public void testA6() throws IOException, ReflectiveOperationException {
        compileSpec("C(A(Ia0,Jd0))",
                    "compiler.err.does.not.override.abstract",
                    "compiler.err.types.incompatible");
    }

    public void testA7() throws IOException, ReflectiveOperationException {
        compileSpec("C(A(Id0,Jd0))",
                    "compiler.err.types.incompatible");
    }

    public void testA8() throws IOException, ReflectiveOperationException {
        compileSpec("C(A(Ia0),J)", "compiler.err.does.not.override.abstract");
    }

    public void testA9() throws IOException, ReflectiveOperationException {
        compileSpec("C(Ac0(Id0))");
        assertLinkage("C", "A0");
    }

    public void testA10() throws IOException, ReflectiveOperationException {
        compileSpec("C(Aa0,I)", "compiler.err.does.not.override.abstract");
    }

    public void testA11() throws IOException, ReflectiveOperationException {
        compileSpec("C(Ac0,Id0)");
        assertLinkage("C", "A0");
    }

    // Dan's set B

    /* B1 can't be done, needs a second concrete class D
    public void testB1() throws IOException, ReflectiveOperationException {
        compileSpec("Cc1(Dc0)");
        assertLinkage("C", "C1", "C1");
        assertLinkage("D", "A0", LINKAGE_ERROR);
    }
    */

    public void testB2() throws IOException, ReflectiveOperationException {
        compileSpec("Cc1(Ac0)");
        assertLinkage("C", "C1", "C1");
    }

    //??? B3 seems to suggest that we should create an abstract class
    //public void testB3() throws IOException, ReflectiveOperationException {
    //    compileSpec("Ba1(Cc0)");
    //    assertLinkage("B", "C0", "A1");
    //}

    // B4 needs too many classes

    public void testB5() throws IOException, ReflectiveOperationException {
        compileSpec("Cc1(Aa1(Id0))");
        assertLinkage("C", "C1", "C1");
    }

    public void testB6() throws IOException, ReflectiveOperationException {
        compileSpec("C(Ac1(Id0))");
        assertLinkage("C", "A1", "A1");
    }

    public void testB7() throws IOException, ReflectiveOperationException {
        compileSpec("Cc1(Id0)");
        assertLinkage("C", "C1", "C1");
    }

    public void testB8() throws IOException, ReflectiveOperationException {
        compileSpec("C(Jd1(Id0))");
        assertLinkage("C", "J1", "J1");
    }

    // B9 needs too many classes

    // The rest of Dan's tests need generics
}
