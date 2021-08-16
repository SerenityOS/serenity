/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @modules java.base/jdk.internal.org.objectweb.asm:+open java.base/jdk.internal.org.objectweb.asm.util:+open
 * @library /vmTestbase /test/lib
 *
 * @comment build retransform.jar in current dir
 * @run driver vm.runtime.defmeth.shared.BuildJar
 *
 * @run driver jdk.test.lib.FileInstaller . .
 * @run main/othervm/native
 *      -agentlib:redefineClasses
 *      -javaagent:retransform.jar
 *      vm.runtime.defmeth.MethodResolutionTest
 */
package vm.runtime.defmeth;

import java.util.Set;

import vm.runtime.defmeth.shared.data.*;
import vm.runtime.defmeth.shared.data.method.param.*;
import vm.runtime.defmeth.shared.DefMethTest;
import vm.runtime.defmeth.shared.builder.TestBuilder;

import static jdk.internal.org.objectweb.asm.Opcodes.*;
import static vm.runtime.defmeth.shared.ExecutionMode.*;

/**
 * Tests on method resolution in presence of default methods in the hierarchy.
 *
 * Because default methods reside in interfaces, and interfaces do not have
 * the constraint of being single-inheritance, it is possible to inherit
 * multiple conflicting default methods, or even inherit the same default method
 * from many different inheritance paths.
 *
 * There is an algorithm to select which method to use in the case that a
 * concrete class does not provide an implementation. Informally, the algorithm
 * works as follows:
 *
 * (1) If there is a adequate implementation in the class itself or in a
 *     superclass (not an interface), then that implementation should be used
 *     (i.e., class methods always "win").
 *
 * (2) Failing that, create the set of methods consisting of all methods in the
 *     type hierarchy which satisfy the slot to be filled, where in this case
 *     'satisfy' means that the methods have the same name, the same language-
 *     level representation of the parameters, and covariant return values. Both
 *     default methods and abstract methods will be part of this set.
 *
 * (3) Remove from this set, any method which has a "more specific" version
 *     anywhere in the hierarchy.  That is, if C implements I,J and I extends J,
 *     then if both I and J have a suitable methods, J's method is eliminated
 *     from the set since I is a subtype of J -- there exist a more specific
 *     method than J's method, so that is eliminated.
 *
 * (4) If the remaining set contains only a single entry, then that method is
 *     selected. Note that the method may be abstract, in which case an
 *     IncompatibleClassChangeError is thrown when/if the method is called. If there are
 *     multiple entries in the set, or no entries, then this also results in an
 *     IncompatibleClassChangeError when called.
 */
public class MethodResolutionTest extends DefMethTest {

    public static void main(String[] args) {
        DefMethTest.runTest(MethodResolutionTest.class,
                /* majorVer */ Set.of(MIN_MAJOR_VER, MAX_MAJOR_VER),
                /* flags    */ Set.of(0, ACC_SYNCHRONIZED),
                /* redefine */ Set.of(false, true),
                /* execMode */ Set.of(DIRECT, REFLECTION, INVOKE_EXACT, INVOKE_GENERIC, INVOKE_WITH_ARGS, INDY));
    }

    /*
     * Basic
     *
     * interface I { int m(); }
     * class C implements I { public int m() { return 1; } }
     *
     * TEST: C c = new C(); c.m() == 1;
     * TEST: I i = new C(); i.m() == 1;
     */
    public void testBasic(TestBuilder b) {
        Interface I =
            b.intf("I")
                .abstractMethod("m", "()I").build()
             .build();

        ConcreteClass C =
             b.clazz("C").implement(I)
                .concreteMethod("m", "()I").returns(1).build()
              .build();

        b.test()
                .callSite(I, C, "m", "()I")
                .returns(1)
            .done()
        .test()
                .callSite(C, C, "m", "()I")
                .returns(1)
            .done();
    }

    /*
     * Basic Default
     *
     * interface I { int m() default { return 1; } }
     * class C implements I {}
     *
     * TEST: C c = new C(); c.m() == 1;
     * TEST: I i = new C(); i.m() == 1;
     */
    public void testBasicDefault(TestBuilder b) {
        Interface I =
            b.intf("I")
                .defaultMethod("m", "()I").returns(1)
                .build()
             .build();

        ConcreteClass C =
             b.clazz("C").implement(I)
              .build();

        b.test()
                .callSite(I, C, "m", "()I")
                .returns(1)
            .done()
        .test().callSite(C, C, "m", "()I")
                .returns(1)
            .done();
    }

    /*
     * Far Default
     *
     * interface I { int m() default { return 1; } }
     * interface J extends I {}
     * interface K extends J {}
     * class C implements K {}
     *
     * TEST: [I|J|K|C] i = new C(); i.m() == 1;
     */
    public void testFarDefault(TestBuilder b) {
        Interface I =
            b.intf("I")
                .defaultMethod("m", "()I").returns(1)
                .build()
             .build();

        Interface J = b.intf("J").extend(I).build();
        Interface K = b.intf("K").extend(J).build();

        ConcreteClass C =
             b.clazz("C").implement(K)
              .build();

        b.test()
                .callSite(I, C, "m", "()I")
                .returns(1)
            .done()
        .test().callSite(J, C, "m", "()I")
                .returns(1)
            .done()
        .test().callSite(K, C, "m", "()I")
                .returns(1)
            .done()
        .test().callSite(C, C, "m", "()I")
                .returns(1)
            .done();
    }

    /*
     * Override Abstract
     *
     * interface I { int m(); }
     * interface J extends I { int m() default { return 1; } }
     * interface K extends J {}
     * class C implements K {}
     *
     * TEST: C c = new C(); c.m() == 1;
     * TEST: K k = new C(); k.m() == 1;
     */
    public void testOverrideAbstract(TestBuilder b) {
        Interface I = b.intf("I")
                .abstractMethod("m", "()I").build()
            .build();

        Interface J = b.intf("J").extend(I)
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface K = b.intf("K").extend(J).build();

        ConcreteClass C = b.clazz("C").implement(K).build();

        b.test()
                .callSite(I, C, "m", "()I")
                .returns(1)
            .done()
        .test()
                .callSite(J, C, "m", "()I")
                .returns(1)
            .done()
        .test()
                .callSite(K, C, "m", "()I")
                .returns(1)
            .done()
        .test()
                .callSite(C, C, "m", "()I")
                .returns(1)
            .done();
    }

    /*
     * Default vs Concrete
     *
     * interface I { int m() default { return 1; } }
     * class C implements I { public int m() { return 2; } }
     *
     * TEST: [C|I] c = new C(); c.m() == 2;
     */
    public void testDefaultVsConcrete(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I)
                .concreteMethod("m", "()I").returns(2).build()
            .build();

        b.test()
                .callSite(I, C, "m", "()I")
                .returns(2)
            .done()
        .test()
                .callSite(C, C, "m", "()I")
                .returns(2)
            .done();
    }

    /*
     * InheritedDefault
     *
     * interface I { int m() default { return 1; } }
     * class B implements I {}
     * class C extends B {}
     *
     * TEST: [I|B|C] v = new C(); v.m() == 1;
     */
    public void testInheritedDefault(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        ConcreteClass B = b.clazz("B").implement(I).build();
        ConcreteClass C = b.clazz("C").extend(B).build();

        b.test()
                .callSite(I, C, "m","()I")
                .returns(1)
            .done()
        .test()
                .callSite(B, C, "m","()I")
                .returns(1)
            .done()
        .test()
                .callSite(C, C, "m","()I")
                .returns(1)
            .done();
    }

    /*
     * ExistingInherited
     *
     * interface I { int m() default { return 1; } }
     * class B { public int m() { return 2; } }
     * class C extends B implements I {}
     *
     * TEST: [I|B|C] v = new C(); v.m() == 2;
     */
    public void testExistingInherited(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        ConcreteClass B = b.clazz("B")
                .concreteMethod("m", "()I").returns(2).build()
                .build();

        ConcreteClass C = b.clazz("C").extend(B).implement(I).build();

        b.test()
                .callSite(I, C, "m","()I")
                .returns(2)
            .done()
        .test()
                .callSite(B, C, "m","()I")
                .returns(2)
            .done()
        .test()
                .callSite(C, C, "m","()I")
                .returns(2)
            .done();
    }

    /*
     * ExistingInheritedOverride
     *
     * interface I { int m() default { return 1; } }
     * class B implements I { public int m() { return 2; } }
     * class C extends B { public int m() { return 3; } }
     *
     * TEST: [I|B|D] v = new C(); v.m() == 3;
     */
    public void testExistingInheritedOverride(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        ConcreteClass B = b.clazz("B").implement(I)
                .concreteMethod("m", "()I").returns(2).build()
                .build();

        ConcreteClass C = b.clazz("C").extend(B)
                .concreteMethod("m", "()I").returns(3).build()
                .build();

        b.test()
                .callSite(I, C, "m","()I")
                .returns(3)
            .done()
        .test()
                .callSite(B, C, "m","()I")
                .returns(3)
            .done()
        .test()
                .callSite(C, C, "m","()I")
                .returns(3)
            .done();
    }

    /*
     * ExistingInheritedPlusDefault
     *
     * interface I { int m() default { return 11; } }
     * interface J { int m() default { return 12; } }
     * class C implements I { public int m() { return 21; } }
     * class D extends C { public int m() { return 22; } }
     * class E extends D implements J {}
     *
     * TEST: [I|J|C|D|J] v = new E(); v.m() == 22;
     */
    public void testExistingInheritedPlusDefault(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(11).build()
            .build();

        Interface J = b.intf("J")
                .defaultMethod("m", "()I").returns(12).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I)
                .concreteMethod("m","()I").returns(21).build()
            .build();

        ConcreteClass D = b.clazz("D").extend(C)
                .concreteMethod("m", "()I").returns(22).build()
            .build();

        ConcreteClass E = b.clazz("E").extend(D).implement(J)
                .build();

        b.test()
                .callSite(I, E, "m","()I")
                .returns(22)
            .done()
        .test()
                .callSite(J, E, "m","()I")
                .returns(22)
            .done()
        .test()
                .callSite(C, E, "m","()I")
                .returns(22)
            .done()
        .test()
                .callSite(D, E, "m","()I")
                .returns(22)
            .done()
        .test()
                .callSite(E, E, "m","()I")
                .returns(22)
            .done();
    }

    /*
     * InheritedWithConcrete
     *
     * interface I { int m() default { return 1; } }
     * class B implements I {}
     * class C extends B { public int m() { return 2; } }
     *
     * TEST: [I|B|C] v = new C(); v.m() == 2;
     */
    public void testInheritedWithConcrete(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        ConcreteClass B = b.clazz("B").implement(I).build();

        ConcreteClass C = b.clazz("C").extend(B)
                .concreteMethod("m", "()I").returns(2).build()
                .build();

        b.test()
                .callSite(I, C, "m","()I")
                .returns(2)
            .done()
        .test()
                .callSite(B, C, "m","()I")
                .returns(2)
            .done()
        .test()
                .callSite(C, C, "m","()I")
                .returns(2)
            .done();
    }

    /*
     * InheritedWithConcreteAndImpl
     *
     * interface I { int m() default { return 1; } }
     * class B implements I {}
     * class C extends B implements I { public int m() { return 2; } }
     *
     * TEST: [I|B|C] v = new C(); v.m() == 2;
     */
    public void testInheritedWithConcreteAndImpl(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        ConcreteClass B = b.clazz("B").implement(I).build();

        ConcreteClass C = b.clazz("C").extend(B)
                .concreteMethod("m", "()I").returns(2).build()
            .build();

        b.test()
                .callSite(I, C, "m","()I")
                .returns(2)
            .done()
        .test()
                .callSite(B, C, "m","()I")
                .returns(2)
            .done()
        .test()
                .callSite(C, C, "m","()I")
                .returns(2)
            .done();
    }

    /*
     * Diamond
     *
     * interface I { int m() default { return 1; } }
     * interface J extends I {}
     * interface K extends I {}
     * class C implements J, K {}
     *
     * TEST: [I|J|K|C] c = new C(); c.m() == 99
     */
    public void testDiamond(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J").extend(I).build();
        Interface K = b.intf("K").extend(I).build();

        ConcreteClass C = b.clazz("C").implement(J,K)
            .build();

        b.test()
                .callSite(I, C, "m","()I")
                .returns(1)
            .done()
        .test()
                .callSite(J, C, "m","()I")
                .returns(1)
            .done()
        .test()
                .callSite(K, C, "m","()I")
                .returns(1)
            .done()
        .test()
                .callSite(C, C, "m","()I")
                .returns(1)
            .done();
    }

    /*
     * ExpandedDiamond
     *
     * interface I { int m() default { return 1; } }
     * interface J extends I {}
     * interface K extends I {}
     * interface L extends I {}
     * interface M extends I {}
     * class C implements J, K, L, M {}
     *
     * TEST: [I|J|K|L|M|C] c = new C(); c.m() == 1
     */
    public void testExpandedDiamond(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J").extend(I).build();
        Interface K = b.intf("K").extend(I).build();
        Interface L = b.intf("L").extend(I).build();
        Interface M = b.intf("M").extend(I).build();

        ConcreteClass C = b.clazz("C").implement(J,K,L,M)
            .build();

        b.test()
                .callSite(I, C, "m","()I")
                .returns(1)
            .done()
        .test()
                .callSite(J, C, "m","()I")
                .returns(1)
            .done()
        .test()
                .callSite(K, C, "m","()I")
                .returns(1)
            .done()
        .test()
                .callSite(L, C, "m","()I")
                .returns(1)
            .done()
        .test()
                .callSite(M, C, "m","()I")
                .returns(1)
            .done()
        .test()
                .callSite(C, C, "m","()I")
                .returns(1)
            .done();
    }

    /*
     * SelfFill w/ explicit bridge
     *
     * interface I<T> { int m(T t) default { return 1; } }
     * class C implements I<C> {
     *   public int m(C s) { return 2; }
     *   public int m(Object o) { ... }
     * }
     *
     * TEST: I i = new C(); i.m((Object)null) == 2;
     * TEST: C c = new C(); c.m((Object)null) == 2;
     * TEST: C c = new C(); c.m((C)null) == 2;
     */
    public void testSelfFillWithExplicitBridge(TestBuilder b) {
        /* interface I<T> { ... */
        Interface I = b.intf("I").sig("<T:Ljava/lang/Object;>Ljava/lang/Object;")
                    /* default int m(T t) { return 1; } */
                    .defaultMethod("m", "(Ljava/lang/Object;)I")
                        .sig("(TT;)I")
                        .returns(1)
                        .build()
                .build();

        /* class C implements I<C> { ... */
        ConcreteClass C = b.clazz("C").implement(I)
                .sig("Ljava/lang/Object;LI<LC;>;")

                /* public int m(I i) { return 2; } */
                .concreteMethod("m","(LC;)I").returns(2).build()

                /* bridge method for m(LI;)I */
                .concreteMethod("m","(Ljava/lang/Object;)I")
                    .flags(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC)
                    .returns(2)
                .build()
            .build();

        // I i = new C(); ...
        b.test()
                .callSite(I, C, "m", "(Ljava/lang/Object;)I")
                .params(new NullParam())
                .returns(2)
            .done()
        // C c = new C(); ...
        .test()
                .callSite(C, C, "m", "(Ljava/lang/Object;)I")
                .params(new NullParam())
                .returns(2)
            .done()
        .test()
                .callSite(C, C, "m", "(LC;)I")
                .params(new NullParam())
                .returns(2)
            .done();
    }

    /*
     * interface I { int m() default { return 1; } }
     * class C implements I { int m(int i) { return 2; } }
     *
     * TEST: C c = new C(); c.m(0) == 2;
     * TEST: I i = new C(); i.m() == 1;
     */
    public void testMixedArity() {
        TestBuilder b = factory.getBuilder();

        Interface I =
            b.intf("I")
                .defaultMethod("m", "()I").returns(1)
                .build()
             .build();

        ConcreteClass C =
             b.clazz("C").implement(I)
                .concreteMethod("m", "(I)I").returns(2)
                .build()
              .build();

        b.test().callSite(I, C, "m", "()I")
                .returns(1)
            .build();
        b.test().callSite(C, C, "m", "(I)I").params(ICONST_0)
                .returns(2)
            .build();
    }

    /*
     * interface I { int m() default { return 1; } }
     * interface J { int m(int i) default { return 2; } }
     * class C implements I, J {}
     *
     * TEST: I i = new C(); i.m() == 1;     i.m(0) ==> NSME
     * TEST: J j = new C(); j.m() ==> NSME; j.m(0) == 2
     * TEST: C c = new C(); c.m() == 1;     c.m(0) == 2
     */
    public void testConflictingDefaultMixedArity1(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1)
                .build()
            .build();

        Interface J = b.intf("J")
                .defaultMethod("m", "(I)I").returns(2)
                .build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I,J).build();


        // I i = new C(); ...
        b.test().callSite(I, C, "m", "()I")
                .returns(1)
            .build();
        b.test().callSite(I, C, "m", "(I)I").params(ICONST_0)
                .throws_(NoSuchMethodError.class)
            .build();

        // J j = new C(); ...
        b.test().callSite(J, C, "m", "()I")
                .throws_(NoSuchMethodError.class)
            .build();
        b.test().callSite(J, C, "m", "(I)I").params(ICONST_0)
                .returns(2)
            .build();

        // C c = new C(); ...
        b.test().callSite(C, C, "m", "()I")
                .returns(1)
            .build();
        b.test().callSite(C, C, "m", "(I)I").params(ICONST_0)
                .returns(2)
            .build();
    }

    /*
     * interface I { int m() default { return 1; } }
     * interface J { int m() default { return 2; } }
     * class C implements I, J {
     *   int m(int i) { return 3; }
     * }
     *
     * TEST: I i = new C(); i.m(0) ==> ICCE
     * TEST: J j = new C(); j.m(0) ==> ICCE
     * TEST: C c = new C(); c.m() ==> ICCE; c.m(0) == 3
     */
    public void testConflictingDefaultMixedArity2(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1)
                .build()
            .build();

        Interface J = b.intf("J")
                .defaultMethod("m", "()I").returns(2)
                .build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I, J)
                .concreteMethod("m", "(I)I").returns(3)
                .build()
            .build();

        // I i = new C(); ...
        b.test().callSite(I, C, "m", "()I")
                .throws_(IncompatibleClassChangeError.class)
            .build();
        b.test().callSite(I, C, "m", "(I)I").params(ICONST_0)
                .throws_(NoSuchMethodError.class)
            .build();

        // J j = new C(); ...
        b.test().callSite(J, C, "m", "()I")
                .throws_(IncompatibleClassChangeError.class)
            .build();
        b.test().callSite(J, C, "m", "(I)I").params(ICONST_0)
                .throws_(NoSuchMethodError.class)
            .build();

        // C c = new C(); ...
        b.test().callSite(C, C, "m", "()I")
                .throws_(IncompatibleClassChangeError.class)
            .build();
        b.test().callSite(C, C, "m", "(I)I").params(ICONST_0)
                .returns(3)
            .build();
    }

    /* In package1:
     * package p1;
     * interface I {
     *     default int m() { return 10; };
     * }
     * public interface J extends I {};
     *
     * In package2:
     * class A implements p1.J {}
     * A myA = new A;
     * myA.m();  // should return 10 except for reflect mode,
     *           // throw IllegalAccessException with reflect mode
     */

    public void testMethodResolvedInDifferentPackage(TestBuilder b) {
        Interface I = b.intf("p1.I").flags(~ACC_PUBLIC & ACC_PUBLIC) // make it package private
                .defaultMethod("m", "()I").returns(10)
                .build()
            .build();

        Interface J = b.intf("p1.J").extend(I)
            .build();

        ConcreteClass myA = b.clazz("p2.A").implement(J)
            .build();
        if (!factory.getExecutionMode().equals("REFLECTION")) {
            b.test()
                .callSite(myA, myA, "m", "()I")
                .returns(10)
                .done();
        } else {
            // -mode reflect will fail with IAE as expected
            b.test()
                .callSite(myA, myA, "m", "()I")
                .throws_(IllegalAccessException.class)
                .done();
        }
    }

    /* In package p1:
     * package p1;
     * interface I {
     *   public default int m() { return 12; };
     * }
     *
     * public class A implements I {}
     *
     * In package p2:
     * package p2;
     * public interface J { int m(); }
     *
     * public class B extends p1.A implements J {
     *   public int m() { return 13; }
     * }
     *
     * Then:
     *   A myA = new B;
     *   myA.m();  // should return 13, not throw IllegalAccessError
     */

    public void testMethodResolvedInLocalFirst(TestBuilder b) {
        Interface I = b.intf("p1.I")
                .defaultMethod("m", "()I").returns(12)
                .build()
            .build();

        ConcreteClass myA = b.clazz("p1.A").implement(I)
            .build();

        Interface J = b.intf("p2.J").abstractMethod("m", "()I")
                .build()
            .build();

        ConcreteClass myB = b.clazz("p2.B").extend(myA).implement(J)
                 .concreteMethod("m", "()I").returns(13)
                 .build()
            .build();

        b.test()
                .callSite(myB, myB, "m", "()I")
                .returns(13)
                .done();
    }
}
