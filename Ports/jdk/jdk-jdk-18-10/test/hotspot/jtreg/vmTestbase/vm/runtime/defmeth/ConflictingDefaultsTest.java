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
 *      vm.runtime.defmeth.ConflictingDefaultsTest
 */
package vm.runtime.defmeth;

import java.util.Set;

import vm.runtime.defmeth.shared.DefMethTest;
import vm.runtime.defmeth.shared.annotation.NotApplicableFor;
import vm.runtime.defmeth.shared.builder.TestBuilder;
import vm.runtime.defmeth.shared.data.*;

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_SYNCHRONIZED;
import static vm.runtime.defmeth.shared.data.method.body.CallMethod.Invoke.*;
import static vm.runtime.defmeth.shared.data.method.body.CallMethod.IndexbyteOp.*;
import static vm.runtime.defmeth.shared.ExecutionMode.*;

/**
 * Tests on conflicting defaults.
 *
 * It is allowable to inherit a default through multiple paths (such as
 * through a diamond-shaped interface hierarchy), but the resolution procedure
 * is looking for a unique, most specific default-providing interface.
 *
 * If one default shadows  another (where a subinterface provides a different
 * default for an extension method declared in a superinterface), then the less
 * specific interface is pruned from consideration no matter where it appears
 * in the inheritance hierarchy.  If two or more extended interfaces provide
 * default implementations, and one is not a superinterface of the other, then
 * neither is used and a linkage exception is thrown indicating conflicting
 * default implementations.
 */
public class ConflictingDefaultsTest extends DefMethTest {
    public static void main(String[] args) {
        DefMethTest.runTest(ConflictingDefaultsTest.class,
                /* majorVer */ Set.of(MIN_MAJOR_VER, MAX_MAJOR_VER),
                /* flags    */ Set.of(0, ACC_SYNCHRONIZED),
                /* redefine */ Set.of(false, true),
                /* execMode */ Set.of(DIRECT, REFLECTION, INVOKE_EXACT, INVOKE_GENERIC, INVOKE_WITH_ARGS, INDY));
    }

    /*
     * Conflict
     *
     * interface I { int m() default { return 1; } }
     * interface J { int m() default { return 2; } }
     * class C implements I, J {}
     *
     * TEST: C c = new C(); c.m() ==> ICCE
     */
    public void testConflict(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J")
                .defaultMethod("m", "()I").returns(2).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I,J).build();

        b.test().callSite(C, C, "m","()I")
                .throws_(IncompatibleClassChangeError.class)
            .done();
    }

    /*
     * Maximally-specific Default (0.6.3 spec change)
     *
     * interface I { int m(); }
     * interface J { int m() default { return 2; } }
     * class C implements I, J {}
     *
     * TEST: C c = new C(); c.m() return 2
     */
    public void testMaximallySpecificDefault(TestBuilder b) {
        Interface I = b.intf("I")
                .abstractMethod("m", "()I").build()
            .build();

        Interface J = b.intf("J")
                .defaultMethod("m", "()I").returns(2).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I,J).build();

        b.test().callSite(C, C, "m","()I")
                .returns(2)
            .done();
    }

    /*
     * Reabstract
     *
     * interface I { int m() default { return 1; } }
     * interface J extends I { int m(); }
     * class C implements J {}
     *
     * TEST: C c = new C(); c.m() ==> AME
     */
    public void testReabstract(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J").extend(I)
                .abstractMethod("m", "()I").build()
            .build();

        ConcreteClass C = b.clazz("C").implement(J).build();

        b.test().callSite(C, C, "m","()I")
                .throws_(AbstractMethodError.class)
            .done();
    }

    /*
     * Reabstract2
     *
     * interface I { int m() default { return 1; } }
     * interface J extends I { int m(); }
     * class C implements J {}
     * class D extends C { callSuper C.m}
     *
     * TEST: C c = new C(); c.m() ==> AME
     * TEST: J j = new C(); j.m() ==> AME
     * TEST: I i = new C(); i.m() ==> AME
     * TEST: D d = new D(); d.m() ==> callSuper C.m ==> AME
     */
    public void testReabstract2(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J").extend(I)
                .abstractMethod("m", "()I").build()
            .build();

        ConcreteClass C = b.clazz("C").implement(J).build();
        ConcreteClass D = b.clazz("D").extend(C)
                .concreteMethod("m", "()I").callSuper(C, "m", "()I").build()
            .build();

        b.test().callSite(C, C, "m","()I")
                .throws_(AbstractMethodError.class)
            .done()
         .test().callSite(J, C, "m","()I")
                .throws_(AbstractMethodError.class)
            .done()
         .test().callSite(I, C, "m","()I")
                .throws_(AbstractMethodError.class)
            .done()
         .test().callSite(D, D, "m","()I")
                .throws_(AbstractMethodError.class)
            .done();
    }

    /*
     * ReabstractConflictingDefaults
     *
     * interface I { int m() default { return 1; } }
     * interface J { int m() default { return 2; } }
     * interface K extends I,J { int m(); }
     * class A implements I,J {}
     * class C extends A implements K {}
     *
     * TEST: A c = new C(); c.m() ==> AME
     */
    public void testReabstractConflictingDefaults(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J")
                .defaultMethod("m", "()I").returns(2).build()
            .build();

        Interface K = b.intf("K").extend(I,J)
                .abstractMethod("m", "()I").build()
            .build();

        ConcreteClass A = b.clazz("A").implement(I,J).build();
        ConcreteClass C = b.clazz("C").extend(A).implement(K).build();

        b.test().callSite(A, C, "m","()I")
                .throws_(AbstractMethodError.class)
            .done();
    }


    /*
     * ReabstractConflictingDefaultsInvokeInterface
     *
     * interface I { int m() default { return 1; } }
     * interface J { int m() default { return 2; } }
     * interface K extends I,J { int m(); }
     * interface L extends K { }
     * class A implements I,J {}
     * class C extends A implements K {}
     * class D extends C implements L {}
     *
     * TEST: I i = new A(); i.m() ==> ICCE
     * TEST: K k = new C(); k.m() ==> AME
     * TEST: L l = new D(); l.m() ==> AME
     */
    public void testReabstractConflictingDefaultsInvokeInterface(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J")
                .defaultMethod("m", "()I").returns(2).build()
            .build();

        Interface K = b.intf("K").extend(I,J)
                .abstractMethod("m", "()I").build()
            .build();

        Interface L = b.intf("L").extend(K)
            .build();

        ConcreteClass A = b.clazz("A").implement(I,J).build();
        ConcreteClass C = b.clazz("C").extend(A).implement(K).build();
        ConcreteClass D = b.clazz("D").extend(C).implement(L).build();

        b.test().callSite(I, A, "m","()I")
                .throws_(IncompatibleClassChangeError.class)
            .done()
         .test().callSite(K, C, "m","()I")
                .throws_(AbstractMethodError.class)
            .done()
         .test().callSite(L, D, "m","()I")
                .throws_(AbstractMethodError.class)
            .done();
    }

    /*
     * ReabstractConflictingDefaultsSuper
     *
     * interface I { int m() default { return 1; } }
     * interface J { int m() default { return 2; } }
     * interface K extends I,J { int m(); }
     * interface L extends K { }
     * class A implements I,J {}
     * class C extends A implements K {}
     * class D extends C implements L {int m() {callSuper A.m }
     *
     * TEST: I i = new A(); i.m() ==> ICCE
     * TEST: K k = new C(); CallSuper k.m() ==> AME
     * TEST: L l = new D(); l.m() ==> AME
     */
    public void testReabstractConflictingDefaultsSuper(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J")
                .defaultMethod("m", "()I").returns(2).build()
            .build();

        Interface K = b.intf("K").extend(I,J)
                .abstractMethod("m", "()I").build()
            .build();

        Interface L = b.intf("L").extend(K)
            .build();

        ConcreteClass A = b.clazz("A").implement(I,J).build();
        ConcreteClass C = b.clazz("C").extend(A).implement(K).build();
        ConcreteClass D = b.clazz("D").extend(C).implement(L)
                .concreteMethod("m", "()I").callSuper(A, "m", "()I").build()
            .build();

        b.test().callSite(I, A, "m","()I")
                .throws_(IncompatibleClassChangeError.class)
            .done()
         .test().callSite(L, D, "m","()I")
                .throws_(AbstractMethodError.class)
            .done();
    }

    /*
     * testReabstractResolveMethod00705m2
     *
     * Test case for JDK-8027804: JCK resolveMethod test fails expecting AME
     *
     * This test is an extension of the JCK test resolveMethod00705m2
     * with additional invoke* bytecodes specified for testing purposes.
     *
     * interface I { int m() default { return 1; } }
     * interface J implements I { int m(); }
     * class A implements J,I {}
     * class C extends A {}
     *
     * TEST: A a = new C(); a.m() ==> AME (invokevirtual)
     *       C c = new C(); c.m() ==> AME (invokevirtual)
     *       J j = new C(); j.m() ==> AME (invokeinterface)
     *       I i = new C(); i.m() ==> AME (invokeinterface)
     *       c.test_Cmethod_ISMR(); c.super.m() ==> AME (invokespecial MR)
     *       a.test_Amethod_ISIMR(); j.super.m() ==> AME (invokespecial IMR)
     *
     *       For ver > 49, error will be AME
     *       For ver = 49, error will be VE
     */

    @NotApplicableFor(modes = { REDEFINITION }) // Can't redefine a class that gets error during loading
    public void testReabstractResolveMethod00705m2(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J").extend(I)
            .abstractMethod("m", "()I").build()
            .build();

        ConcreteClass A = b.clazz("A").implement(J,I)
                .concreteMethod("test_Amethod_ISIMR", "()V")
                    .invoke(SPECIAL, b.clazzByName("J"), b.clazzByName("A"),
                         "m", "()I", INTERFACEMETHODREF)
                .build()
            .build();

        ConcreteClass C = b.clazz("C").extend(A)
                .concreteMethod("test_Cmethod_ISMR", "()V")
                    .invoke(SPECIAL, b.clazzByName("C"), b.clazzByName("C"),
                         "m", "()I", CALLSITE)
                .build()
            .build();

        Class expectedError1, expectedError2;
        if (factory.getVer() >=52) {
            expectedError1 = expectedError2 = AbstractMethodError.class;
        } else {
            expectedError1 = expectedError2 = VerifyError.class;
        }

         b.test().callSite(A, C, "m", "()I")
                 .throws_(expectedError2)
             .done()
          .test().callSite(C, C, "m", "()I")
                 .throws_(expectedError2)
             .done()
          .test().callSite(J, C, "m", "()I")
                 .throws_(expectedError1)
             .done()
          .test().callSite(I, C, "m", "()I")
                 .throws_(expectedError1)
             .done()
          .test().callSite(C, C, "test_Cmethod_ISMR", "()V")
                 .throws_(expectedError2)
             .done()
          .test().callSite(A, C, "test_Amethod_ISIMR", "()V")
                 .throws_(expectedError2)
             .done();
    }

    /*
     * Shadow
     *
     * interface I { int m() default { return 1; } }
     * interface J extends I { int m() default { return 2; } }
     * class C implements J {}
     *
     * TEST: [I|J|C] c = new C(); c.m() == 2;
     */
    public void testShadow(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J").extend(I)
                .defaultMethod("m", "()I").returns(2).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(J).build();

        b.test().callSite(I, C, "m","()I")
                .returns(2)
            .done()
        .test()
                .callSite(J, C, "m","()I")
                .returns(2)
            .done()
        .test()
                .callSite(C, C, "m","()I")
                .returns(2)
            .done();
    }

    /*
     * Disqualified
     *
     * interface I { int m() default { return 1; } }
     * interface J extends I { int m() default { return 2; } }
     * class C implements I, J {}
     *
     * TEST: [I|J|C] c = new C(); c.m() == 2;
     */
    public void testDisqualified(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J").extend(I)
                .defaultMethod("m", "()I").returns(2).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I,J).build();

        b.test()
                .callSite(I, C, "m","()I")
                .returns(2)
            .done()
        .test()
                .callSite(J, C, "m","()I")
                .returns(2)
            .done()
        .test()
                .callSite(C, C, "m","()I")
                .returns(2)
            .done();
    }

    /*
     * Mixed arity
     *
     * interface I { int m() default { return 1; } }
     * interface J { int m(int i) default { return 2; } }
     * class C implements I, J {}
     *
     * TEST: I i = new C(); i.m() == 1; i.m(0) ==> NSME
     * TEST: J j = new C(); j.m() ==> NSME; j.m(0) == 2
     * TEST: C c = new C(); c.m() == 1; c.m(0) == 2
     */
    public void testMixedArity1(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J")
                .defaultMethod("m", "(I)I").returns(2).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I,J).build();

        // I i = new C(); ...
        b.test()
                .callSite(I, C, "m","()I")
                .returns(1)
            .done()
        .test()
                .callSite(I, C, "m","(I)I")
                .params(0)
                .throws_(NoSuchMethodError.class)
            .done()

        // J j = new C(); ...
        .test()
                .callSite(J, C, "m","()I")
                .throws_(NoSuchMethodError.class)
            .done()
        .test()
                .callSite(J, C, "m","(I)I")
                .params(0)
                .returns(2)
            .done()

        // C c = new C(); ...
        .test()
                .callSite(C, C, "m","()I")
                .returns(1)
            .done()
        .test()
                .callSite(C, C, "m","(I)I")
                .params(0)
                .returns(2)
            .done();
    }

    /*
     * Mixed arity
     *
     * interface I { int m() default { return 1; } }
     * interface J { int m() default { return 2; } }
     * class C implements I, J { int m(int i) { return 3; }}
     *
     * TEST: I i = new C(); i.m() ==> ICCE
     * TEST: J j = new C(); j.m() ==> ICCE
     * TEST: C c = new C(); c.m() ==> ICCE; c.m(0) == 3
     */
    public void testMixedArity2(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J")
                .defaultMethod("m", "()I").returns(2).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I,J)
                .concreteMethod("m", "(I)I").returns(3).build()
            .build();

        // I i = new C(); ...
        b.test()
                .callSite(I, C, "m","()I")
                .throws_(IncompatibleClassChangeError.class)
            .done()

        // J j = new C(); ...
        .test()
                .callSite(J, C, "m","()I")
                .throws_(IncompatibleClassChangeError.class)
            .done()

        // C c = new C(); ...
        .test()
                .callSite(C, C, "m","()I")
                .throws_(IncompatibleClassChangeError.class)
            .done()
        .test()
                .callSite(C, C, "m","(I)I")
                .params(0)
                .returns(3)
            .done();
    }
}
