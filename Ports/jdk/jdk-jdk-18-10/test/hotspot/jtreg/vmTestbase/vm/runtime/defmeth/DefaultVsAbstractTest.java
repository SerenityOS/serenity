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
 *      vm.runtime.defmeth.DefaultVsAbstractTest
 */
package vm.runtime.defmeth;

import java.util.Set;

import vm.runtime.defmeth.shared.DefMethTest;
import vm.runtime.defmeth.shared.data.*;
import vm.runtime.defmeth.shared.builder.TestBuilder;

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_SYNCHRONIZED;
import static vm.runtime.defmeth.shared.ExecutionMode.*;

/**
 * Tests on interaction of default methods with abstract methods
 *
 * The rule: "the superclass always wins."
 *
 * In searching the superclass hierarchy, a declaration in a superclass is
 * preferred to a default in an interface. This preference includes abstract
 * methods in superclasses as well; the defaults are only considered when
 * the entire implementation hierarchy is silent on the status of the method
 * in question.
 */
public class DefaultVsAbstractTest extends DefMethTest {

    public static void main(String[] args) {
        DefMethTest.runTest(DefaultVsAbstractTest.class,
                /* majorVer */ Set.of(MIN_MAJOR_VER, MAX_MAJOR_VER),
                /* flags    */ Set.of(0, ACC_SYNCHRONIZED),
                /* redefine */ Set.of(false, true),
                /* execMode */ Set.of(DIRECT, REFLECTION, INVOKE_EXACT, INVOKE_GENERIC, INVOKE_WITH_ARGS, INDY));
    }

    /*
     * interface I          { public int m() default { return 1; } }
     * class C implements I { public abstract int m(); }
     *
     * TEST: new C() throws InstantiationError
     */
    public void test0(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I)
                .abstractMethod("m", "()I").build()
            .build();

        b.test()
            .callSite(I, C, "m", "()I")
            .throws_(InstantiationError.class)
            .done();
    }

    /*
     * interface I          {
     *     public int m() default { return 1; }
     * }
     * class C implements I {
     *     public abstract int m();
     * }
     * class D extends C {}
     *
     * TEST: I i = new D(); i.m() ==> AME
     * TEST: C c = new D(); c.m() ==> AME
     * TEST: D d = new D(); d.m() ==> AME
     */
    public void test1(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I)
                .abstractMethod("m", "()I").build()
            .build();

        ConcreteClass D = b.clazz("D").extend(C).build();

        b.test()
            .callSite(I, D, "m",  "()I")
            .throws_(AbstractMethodError.class)
            .done()
        .test()
            .callSite(C, D, "m", "()I")
            .throws_(AbstractMethodError.class)
            .done()
        .test()
            .callSite(D, D, "m", "()I")
            .throws_(AbstractMethodError.class)
            .done();
    }

    /*
     * interface I {
     *     default  public int m() { return 1; }
     * }
     * class C     {
     *     abstract public int m();
     * }
     * class D extends C implements I {}
     *
     * TEST: I o = new D(); o.m()I throws AME
     * TEST: C o = new D(); o.m()I throws AME
     * TEST: D o = new D(); o.m()I throws AME
     */
    public void test2(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        ConcreteClass C = b.clazz("C")
                .abstractMethod("m", "()I").build()
            .build();

        ConcreteClass D = b.clazz("D").extend(C).implement(I).build();

        b.test()
            .callSite(I, D, "m", "()I")
            .throws_(AbstractMethodError.class)
            .done()
        .test()
            .callSite(C, D, "m", "()I")
            .throws_(AbstractMethodError.class)
            .done()
        .test()
            .callSite(D, D, "m", "()I")
            .throws_(AbstractMethodError.class)
            .done();
    }

    /*
     * interface I {
     *     default public int m() { return 1; }
     * }
     * class C {
     *     abstract public int m();
     * }
     * class D extends C implements I {
     *     public int m()  { return 2; }
     * }
     *
     * TEST: I o = new D(); o.m()I == 2
     * TEST: C o = new D(); o.m()I == 2
     * TEST: D o = new D(); o.m()I == 2
     */
    public void test3(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        ConcreteClass C = b.clazz("C")
                .abstractMethod("m", "()I").build()
            .build();

        ConcreteClass D = b.clazz("D").extend(C).implement(I)
                .concreteMethod("m", "()I").returns(2)
                .build()
            .build();

        b.test() // I i = new D(); ...
            .callSite(I, D, "m", "()I").returns(2)
            .done()
        .test()  // C c = new D(); ...
            .callSite(C, D, "m", "()I").returns(2)
            .done()
        .test()  // D d = new C(); ...
            .callSite(D, D, "m", "()I").returns(2)
            .done();
    }

    /*
     * interface I {
     *     default public int m() { return 1; }
     * }
     * class E {
     *     abstract public int m();
     * }
     * class D extends E {}
     * class C extends D implements I {}
     *
     * TEST: I o = new C(); o.m()I throws AME
     * TEST: E o = new C(); o.m()I throws AME
     * TEST: D o = new C(); o.m()I throws AME
     * TEST: C o = new C(); o.m()I throws AME
     */
    public void test4(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
                .build();

        ConcreteClass E = b.clazz("E")
                .abstractMethod("m", "()I").build()
                .build();

        ConcreteClass D = b.clazz("D").extend(E).build();

        ConcreteClass C = b.clazz("C").extend(D).implement(I).build();

        b.test() // I i = new C(); ...
            .callSite(I, C, "m", "()I")
            .throws_(AbstractMethodError.class)
            .done()
        .test() // E e = new C(); ...
            .callSite(E, C, "m", "()I")
            .throws_(AbstractMethodError.class)
            .done()
        .test() // D d = new C(); ...
            .callSite(D, C, "m", "()I")
            .throws_(AbstractMethodError.class)
            .done()
        .test() // C c = new C(); ...
            .callSite(C, C, "m", "()I")
            .throws_(AbstractMethodError.class)
            .done();
    }

    /*
     * interface I {
     *     default public int m() { return 1; }
     * }
     * class E {
     *     abstract public int m();
     * }
     * class D extends E {
     *     public int m()  { return 2; }
     * }
     * class C extends D implements I {}
     *
     * TEST: I o = new C(); o.m()I == 2
     * TEST: I o = new C(); o.m()I == 2
     * TEST: I o = new C(); o.m()I == 2
     * TEST: I o = new C(); o.m()I == 2
     */
    public void test5(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        ConcreteClass E = b.clazz("E")
                .abstractMethod("m", "()I").build()
            .build();

        ConcreteClass D = b.clazz("D").extend(E)
                .concreteMethod("m", "()I").returns(2).build()
            .build();

        ConcreteClass C = b.clazz("C").extend(D).implement(I).build();

        b.test() // I i = new C(); ...
            .callSite(I, C, "m", "()I")
            .returns(2)
            .done()
        .test() // E e = new C(); ...
            .callSite(I, C, "m", "()I")
            .returns(2)
            .done()
        .test() // D d = new C(); ...
            .callSite(I, C, "m", "()I")
            .returns(2)
            .done()
        .test() // C c = new C(); ...
            .callSite(I, C, "m", "()I")
            .returns(2)
            .done();
    }

    /*
     * interface I {
     *     default public int m() { return 1; }
     * }
     * interface J {
     *     default public int m() { return 2; }
     * }
     * class E {
     *     abstract public int m();
     * }
     * class D extends E {
     *     public int m()  { return 3; }
     * }
     * class C extends D implements I, J {}
     *
     * TEST: I o = new C(); o.m()I == 3
     * TEST: J o = new C(); o.m()I == 3
     * TEST: E o = new C(); o.m()I == 3
     * TEST: D o = new C(); o.m()I == 3
     * TEST: J o = new C(); o.m()I == 3
     */
    public void test6(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J")
                .defaultMethod("m", "()I").returns(2).build()
            .build();

        ConcreteClass E = b.clazz("E")
                .abstractMethod("m", "()I").build()
            .build();

        ConcreteClass D = b.clazz("D").extend(E)
                .concreteMethod("m", "()I").returns(3).build()
            .build();

        ConcreteClass C = b.clazz("C").extend(D).implement(I, J).build();


        b.test() // I i = new C(); ...
            .callSite(I, C, "m", "()I").returns(3)
            .done()
        .test() // J j = new C(); ...
            .callSite(J, C, "m", "()I").returns(3)
            .done()
        .test()  // E e = new C(); ...
            .callSite(E, C, "m", "()I").returns(3)
            .done()
        .test()  // D d = new C(); ...
            .callSite(D, C, "m", "()I").returns(3)
            .done()
        .test() // C c = new C(); ...
            .callSite(J, C, "m", "()I").returns(3)
            .done();
    }

    /*
     * interface I {
     *     abstract public int m();
     * }
     *
     * interface J {
     *     default public int m() { return 1; }
     * }
     *
     * class A implements I;
     *
     * class B extends A implements J;
     *
     * TEST: A o = new B(); o.m()I
     *                returns 1 for REFLECTION and INVOKE_WITH_ARGS
     *                ICCE for other modes
     */
    public void testInvokeInterfaceClassDefaultMethod(TestBuilder b) {
        Interface I = b.intf("I")
            .abstractMethod("m", "()I").build()
            .build();

        Interface J = b.intf("J")
            .extend(I)
            .defaultMethod("m", "()I").returns(1).build()
            .build();

        ConcreteClass A = b.clazz("A").implement(I).build();

        ConcreteClass B = b.clazz("B").extend(A).implement(J).build();

        String exeMode = factory.getExecutionMode();

        // the test passes in the reflection mode because there's no way to
        // express invokeinterface on a class using Reflection API
        // In the test generator, vm.runtime.defmeth.shared.executor.ReflectionTest,
        // the invokeinterface is switched to invokevirtual.
        //
        // the test passes in the INVOKE_WITH_ARGS mode due to the fix for
        // JDK-8032010 to conform with the removal of the following check
        // during method resolution in JVMS-5.4.3.3 Method Resolution
        // "If method lookup succeeds and the method is abstract, but C is not
        // abstract, method resolution throws an AbstractMethodError."
        if (exeMode.equals("REFLECTION") ||
            exeMode.equals("INVOKE_WITH_ARGS")) {
            b.test().interfaceCallSite(A, B, "m", "()I")
             .returns(1).done();
        } else {
            // ICCE in other modes due to
            // JVMS-5.4.3.4. Interface Method Resolution
            //   When resolving an interface method reference:
            //     If C is not an interface, interface method resolution throws an IncompatibleClassChangeError.
            b.test().interfaceCallSite(A, B, "m", "()I")
             .throws_(IncompatibleClassChangeError.class).done();
        }
    }

    /*
     * interface I {
     *     abstract public int m();
     * }
     *
     * interface J {
     *     abstract public int m();
     * }
     *
     * class A implements I;
     *
     * class B extends A implements J;
     *
     * TEST: A o = new B(); o.m()I throws ICCE
     */
    public void testInvokeInterfaceClassAbstractMethod(TestBuilder b) {
        Interface I = b.intf("I")
            .abstractMethod("m", "()I").build()
            .build();

        Interface J = b.intf("J")
            .abstractMethod("m", "()I").build()
            .build();

        ConcreteClass A = b.clazz("A").implement(I).build();

        ConcreteClass B = b.clazz("B").extend(A).implement(J).build();

        // JVMS-5.4.3.4. Interface Method Resolution
        //   When resolving an interface method reference:
        //     If C is not an interface, interface method resolution throws an IncompatibleClassChangeError.
        b.test().interfaceCallSite(A, B, "m", "()I")
         .throws_(IncompatibleClassChangeError.class).done();
    }

    /*
     * interface I {
     *     public int m() default { return 1; }
     * }
     *
     * interface J {
     *     public int m() default { return 1; }
     * }
     *
     * class A implements I;
     *
     * class B extends A implements J;
     *
     * TEST: A o = new B(); o.m()I throws ICCE
     */
    public void testInvokeInterfaceMultipleDefinedClassDefaultMethod(TestBuilder b) {
        Interface I = b.intf("I")
            .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J")
            .defaultMethod("m", "()I").returns(1).build()
            .build();

        ConcreteClass A = b.clazz("A").implement(I).build();

        ConcreteClass B = b.clazz("B").extend(A).implement(J).build();

        // JVMS-5.4.3.4. Interface Method Resolution
        //   When resolving an interface method reference:
        //     If C is not an interface, interface method resolution throws an IncompatibleClassChangeError.
        b.test().interfaceCallSite(A, B, "m", "()I")
         .throws_(IncompatibleClassChangeError.class).done();
    }

}
