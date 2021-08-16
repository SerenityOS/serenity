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
 *      vm.runtime.defmeth.SuperCallTest
 */
package vm.runtime.defmeth;

import java.util.Set;

import vm.runtime.defmeth.shared.DefMethTest;
import vm.runtime.defmeth.shared.annotation.NotApplicableFor;
import vm.runtime.defmeth.shared.data.*;
import vm.runtime.defmeth.shared.builder.TestBuilder;

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_SYNCHRONIZED;
import static vm.runtime.defmeth.shared.ExecutionMode.*;

/*
 * Tests on invoke-super-default.
 *
 * Invoke-super-default is used by a subclass to defer to a default method
 * implementation or to disambiguate between conflicting inherited default
 * methods.
 *
 * Invoke-super-default appears in the source code as
 * "<interface-name>.super.<method-name>(<args>)". It is compiled into an
 * invokespecial instruction whose target is <interface-name>.<method-name>,
 * where the interface is a direct supertype of the current class (the current class
 * must directly implement <interface-name>).
 *
 * Invokespecial on any superinterface method will run interface method
 * resolution, and then the selected method will be set to the resolved method.
 * super defaults no longer check for lack of shadowing, other languages
 * want this capability.
 */
public class SuperCallTest extends DefMethTest {

    public static void main(String[] args) {
        DefMethTest.runTest(SuperCallTest.class,
                /* majorVer */ Set.of(MAX_MAJOR_VER),
                /* flags    */ Set.of(0, ACC_SYNCHRONIZED),
                /* redefine */ Set.of(false, true),
                /* execMode */ Set.of(DIRECT, REFLECTION, INVOKE_EXACT, INVOKE_GENERIC, INVOKE_WITH_ARGS, INDY));
    }

    @Override
    protected void configure() {
        // Since invoke-super-default relies on new semantics of invokespecial,
        // the tests are applicable only to class files of 52 version.
        if (factory.getVer() < 52) {
            getLog().warn("WARN: SuperCallTest is applicable only for class files w/ version >=52.");
            getLog().warn("WARN: Overriding \"-ver " + factory.getVer() + "\" w/ \"-ver 52\".");

            factory.setVer(52);
        }
    }

    /*
     * Basic case
     *
     * interface I { int m() default { return 1; } }
     * interface J extends I { int m() default { return I.super.m(); } }
     * class C implements J {}
     *
     * TEST: C c = new C(); c.m() == 88;
     * TEST: I i = new C(); i.m() == 88;
     */
    public void testSuperBasic1(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J").extend(I)
                .defaultMethod("m", "()I").callSuper(I, "m", "()I").build()
            .build();

        ConcreteClass C = b.clazz("C").implement(J).build();

        b.test().callSite(I, C, "m", "()I").returns(1).done()
         .test().callSite(J, C, "m", "()I").returns(1).done()
         .test().callSite(C, C, "m", "()I").returns(1).done();
    }

    /*
     * Super Conflict Resolution
     *
     * interface K { int m() default { return 1; } }
     * interface L { int m() default { return 2; } }
     * interface I extends K,L { int m() default { K.super.m(); } }
     * class C implements I {}
     * class D implements K,L {}
     *
     * TEST: K k = new C(); k.m() == 1
     * TEST: L l = new C(); l.m() == 1
     * TEST: I i = new C(); i.m() == 1
     * TEST: C c = new C(); c.m() == 1
     *
     * TEST: K k = new D(); k.m() == 1
     * TEST: L l = new D(); l.m() == 1
     * TEST: D d = new D(); d.m() == 1
     */
    public void testSuperConflictResolution(TestBuilder b) {
        Interface K = b.intf("K")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface L = b.intf("L")
                .defaultMethod("m", "()I").returns(2).build()
            .build();

        Interface I = b.intf("I").extend(K, L)
                .defaultMethod("m", "()I").callSuper(K, "m", "()I").build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I).build();

        ConcreteClass D = b.clazz("D").implement(K,L)
                .concreteMethod("m", "()I").callSuper(K, "m", "()I").build()
            .build();


        b.test().callSite(K, C, "m", "()I").returns(1).done()
         .test().callSite(L, C, "m", "()I").returns(1).done()
         .test().callSite(I, C, "m", "()I").returns(1).done()
         .test().callSite(C, C, "m", "()I").returns(1).done()

         .test().callSite(K, D, "m", "()I").returns(1).done()
         .test().callSite(L, D, "m", "()I").returns(1).done()
         .test().callSite(D, D, "m", "()I").returns(1).done();
    }

    /*
     * Super call of conflicting default method from different method name
     *
     * interface K {
     *     default public int m(int) { return 1; }
     * }
     * interface L {
     *     default public int m(int) { return 2; }
     * }
     * interface I extends K, L {
     *     default public int k() { return K.super.m((int)0); }
     *     default public int l() { return L.super.m((int)0); }
     * }
     * class C implements I {}
     * class D implements K, L {
     *     public int k()  { return K.super.m((int)0); }
     *     public int l()  { return L.super.m((int)0); }
     * }
     *
     * TEST: K o = new C(); o.m(I)I throws ICCE
     * TEST: L o = new C(); o.m(I)I throws ICCE
     * TEST: C o = new C(); o.m(I)I throws ICCE
     * TEST: I o = new C(); o.k()I == 1
     * TEST: C o = new C(); o.k()I == 1
     * TEST: I o = new C(); o.l()I == 2
     * TEST: C o = new C(); o.l()I == 2
     * TEST: K o = new D(); o.m(I)I throws ICCE
     * TEST: L o = new D(); o.m(I)I throws ICCE
     * TEST: D o = new D(); o.m(I)I throws ICCE
     * TEST: D o = new D(); o.k()I == 1
     * TEST: D o = new D(); o.l()I == 2
     */
    public void testSuperConflictDiffMethod(TestBuilder b) {
        Interface K = b.intf("K")
                .defaultMethod("m", "(I)I").returns(1).build()
            .build();

        Interface L = b.intf("L")
                .defaultMethod("m", "(I)I").returns(2).build()
            .build();

        Interface I = b.intf("I").extend(K, L)
                .defaultMethod("k", "()I").callSuper(K, "m", "(I)I").build()
                .defaultMethod("l", "()I").callSuper(L, "m", "(I)I").build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I).build();

        ConcreteClass D = b.clazz("D").implement(K,L)
                .concreteMethod("k", "()I").callSuper(K, "m", "(I)I").build()
                .concreteMethod("l", "()I").callSuper(L, "m", "(I)I").build()
            .build();

        b.test().callSite(K, C, "m", "(I)I").throws_(IncompatibleClassChangeError.class).done()
         .test().callSite(L, C, "m", "(I)I").throws_(IncompatibleClassChangeError.class).done()
         .test().callSite(C, C, "m", "(I)I").throws_(IncompatibleClassChangeError.class).done()

         .test().callSite(I, C, "k", "()I").returns(1).done()
         .test().callSite(C, C, "k", "()I").returns(1).done()
         .test().callSite(I, C, "l", "()I").returns(2).done()
         .test().callSite(C, C, "l", "()I").returns(2).done()

         .test().callSite(K, D, "m", "(I)I").throws_(IncompatibleClassChangeError.class).done()
         .test().callSite(L, D, "m", "(I)I").throws_(IncompatibleClassChangeError.class).done()
         .test().callSite(D, D, "m", "(I)I").throws_(IncompatibleClassChangeError.class).done()

         .test().callSite(D, D, "k", "()I").returns(1).done()
         .test().callSite(D, D, "l", "()I").returns(2).done();
    }

    /*
     * SuperConflict
     *
     * interface K { int m() default { return 1; } }
     * interface L { int m() default { return 2; } }
     * interface J extends K, L {}
     * interface I extends J, K { int m() default { J.super.m(); } }
     * class C implements I {}
     *
     * TEST: K k = new C(); k.m() ==> ICCE
     * TEST: L l = new C(); l.m() ==> ICCE
     * TEST: J j = new C(); j.m() ==> ICCE
     * TEST: I i = new C(); i.m() ==> ICCE
     * TEST: C c = new C(); c.m() ==> ICCE
     */
    public void testSuperConflict(TestBuilder b) {
        Interface K = b.intf("K")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface L = b.intf("L")
                .defaultMethod("m", "()I").returns(2).build()
            .build();

        Interface J = b.intf("J").extend(K, L).build();

        Interface I = b.intf("I").extend(K, J)
                .defaultMethod("m", "()I").callSuper(J, "m", "()I").build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I).build();

        b.test().callSite(K, C, "m", "()I").throws_(IncompatibleClassChangeError.class).done()
         .test().callSite(L, C, "m", "()I").throws_(IncompatibleClassChangeError.class).done()
         .test().callSite(J, C, "m", "()I").throws_(IncompatibleClassChangeError.class).done()
         .test().callSite(I, C, "m", "()I").throws_(IncompatibleClassChangeError.class).done()
         .test().callSite(C, C, "m", "()I").throws_(IncompatibleClassChangeError.class).done();
    }

    /*
     * SuperDisqual
     *
     * interface I { int m() default { return 1; } }
     * interface J { int m() default { return 2; } }
     * class C implements I, J { public int m() { return I.super.m(); } }
     *
     * TEST: C c = new C(); c.m() ==> 1
     * TEST: J j = new C(); j.m() ==> 1
     */
    public void testSuperDisqual(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J")
                .defaultMethod("m", "()I").returns(2).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I, J)
                .concreteMethod("m", "()I").callSuper(I, "m", "()I").build()
            .build();

        b.test().callSite(I, C, "m", "()I").returns(1).done()
         .test().callSite(J, C, "m", "()I").returns(1).done()
         .test().callSite(C, C, "m", "()I").returns(1).done();
    }

    /*
     * SuperNull
     *
     * interface I { int m(); }
     * interface J extends I { int m() default { return I.super.m(); } }
     * interface K extends I { int m() default { return I.super.n(); } }
     * class C implements J {}
     * class D implements K {}
     *
     * TEST: I i = new C(); i.m() ==> AME
     * TEST: J j = new C(); j.m() ==> AME
     * TEST: C c = new C(); c.m() ==> AME
     * TEST: K k = new D(); k.m() ==> NSME
     */
    public void testSuperNull(TestBuilder b) {
        Interface I = b.intf("I")
                .abstractMethod("m", "()I").build()
            .build();

        Interface J = b.intf("J").extend(I)
                .defaultMethod("m", "()I").callSuper(I, "m", "()I").build()
            .build();

        Interface K = b.intf("K").extend(I)
                .defaultMethod("m", "()I").callSuper(I, "n", "()I").build()
            .build();

        ConcreteClass C = b.clazz("C").implement(J).build();
        ConcreteClass D = b.clazz("D").implement(K).build();

        b.test().callSite(I, C, "m", "()I").throws_(AbstractMethodError.class).done()
         .test().callSite(J, C, "m", "()I").throws_(AbstractMethodError.class).done()
         .test().callSite(C, C, "m", "()I").throws_(AbstractMethodError.class).done()
         .test().callSite(K, D, "m", "()I").throws_(NoSuchMethodError.class).done();
    }

    /*
     * SuperGeneric
     *
     * interface J<T> { int m(T t) default { return 1; } }
     * interface I extends J<String> { int m(String s) default { return J.super.m(); } }
     * class C implements I {}
     *
     * TEST: I i = new C(); i.m(new Object()) == 1;
     * TESTL J j = new C(); j.m(new Object()) == 1;
     * TEST: J j = new C(); j.m("") == 1;
     * TEST: C c = new C(); c.m(new Object()) == 1;
     * TEST: C c = new C(); c.m("") == 1;
     */
    public void testSuperGeneric(TestBuilder b) {
        // interface I<T> {
        //     default int m(T t) { return 1; }
        // }
        Interface I = b.intf("I")
                .sig("<T:Ljava/lang/Object;>Ljava/lang/Object;")
                .defaultMethod("m", "(Ljava/lang/Object;)I").sig("(TT;)I").returns(1).build()
            .build();

        // interface J extends I<String> {
        //     default int m(String s) { return I.super.m(); }
        // }
        Interface J = b.intf("J").extend(I)
                .sig("Ljava/lang/Object;LI<Ljava/lang/String;>;")
                .defaultMethod("m", "(Ljava/lang/String;)I").callSuper(I, "m", "(Ljava/lang/Object;)I").build()
            .build();

        ConcreteClass C = b.clazz("C").implement(J).build();

        b.test().callSite(I, C, "m", "(Ljava/lang/Object;)I").returns(1).done()

         .test().callSite(J, C, "m", "(Ljava/lang/Object;)I").returns(1).done()
         .test().callSite(J, C, "m", "(Ljava/lang/String;)I").returns(1).done()

         .test().callSite(C, C, "m", "(Ljava/lang/Object;)I").returns(1).done()
         .test().callSite(C, C, "m", "(Ljava/lang/String;)I").returns(1).done();
    }

    /*
     * SuperGenericDisqual
     *
     * interface I<T> { int m(T t) default { return 1; } }
     * interface J extends I<String> { int m(String s) default { return 2; } }
     * class C implements I<String>, J { public int m(String s) { return I.super.m(s); } }
     *
     * TEST: C c = new C(); c.m("string") == 1
     */
    public void testSuperGenericDisqual(TestBuilder b) {
        Interface I = b.intf("I").sig("<T:Ljava/lang/Object;>Ljava/lang/Object;")
                .defaultMethod("m", "(Ljava/lang/Object;)I").sig("(TT;)I").returns(1).build()
            .build();

        Interface J = b.intf("J").extend(I)
                .sig("Ljava/lang/Object;LJ<Ljava/lang/String;>;")
                .defaultMethod("m", "(Ljava/lang/String;)I").returns(2).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I,J)
                .sig("Ljava/lang/Object;LI;LJ<Ljava/lang/String;>;")
                .concreteMethod("m", "(Ljava/lang/String;)I").callSuper(I, "m", "(Ljava/lang/Object;)I").build()
                .build();

        b.test().callSite(I, C, "m", "(Ljava/lang/Object;)I").returns(1).done()

         .test().callSite(J, C, "m", "(Ljava/lang/Object;)I").returns(1).done()
         .test().callSite(J, C, "m", "(Ljava/lang/String;)I").returns(1).done()

         .test().callSite(C, C, "m", "(Ljava/lang/Object;)I").returns(1).done()
         .test().callSite(C, C, "m", "(Ljava/lang/String;)I").returns(1).done();
    }

    /*
     * Super-call of non-default method
     *
     * class C { int m() { return 1; } }
     * class D extends C { int m() { return C.super.m(); } }
     *
     * TEST: C d = new D(); d.m() == 1
     * TEST: D d = new D(); d.m() == 1
     */
    public void testSuperNonDefault(TestBuilder b) {
        ConcreteClass C = b.clazz("C")
                .concreteMethod("m", "()I").returns(1).build()
            .build();

        ConcreteClass D = b.clazz("D").extend(C)
                .concreteMethod("m", "()I").callSuper(C, "m", "()I").build()
            .build();

        b.test().callSite(C, D, "m", "()I").returns(1).done()
         .test().callSite(D, D, "m", "()I").returns(1).done();
    }

    /*
     * Super-call of non-default method
     *
     * interface I { int m(); }
     * class C { int m() { return 1; } }
     * class D extends C implements I { int m() { return I.super.m(); } }
     * class E extends C implements I { int m() { return C.super.m(); } }
     *
     * TEST: I d = new D(); d.m() ==> AME
     * TEST: C d = new D(); d.m() ==> AME
     * TEST: D d = new D(); d.m() ==> AME
     * TEST: I e = new E(); e.m() == 1
     * TEST: C e = new E(); e.m() == 1
     * TEST: E e = new E(); e.m() == 1
     */
    public void testSuperNonDefault1(TestBuilder b) {
        Interface I = b.intf("I")
                .abstractMethod("m", "()I").build()
            .build();

        ConcreteClass C = b.clazz("C")
                .concreteMethod("m", "()I").returns(1).build()
            .build();

        ConcreteClass D = b.clazz("D").extend(C).implement(I)
                .concreteMethod("m", "()I").callSuper(I, "m", "()I").build()
            .build();

        ConcreteClass E = b.clazz("E").extend(C).implement(I)
                .concreteMethod("m", "()I").callSuper(C, "m", "()I").build()
            .build();

        b.test().callSite(I, D, "m", "()I").throws_(AbstractMethodError.class).done()
         .test().callSite(C, D, "m", "()I").throws_(AbstractMethodError.class).done()
         .test().callSite(D, D, "m", "()I").throws_(AbstractMethodError.class).done()

         .test().callSite(I, E, "m", "()I").returns(1).done()
         .test().callSite(C, E, "m", "()I").returns(1).done()
         .test().callSite(E, E, "m", "()I").returns(1).done();
    }

    /*
     * Super-call of non-default method
     *
     * interface I { int m() {return 1;} }
     * class C { int m() { return 2; } }
     * class D extends C implements I { int m() { return I.super.m(); } }
     * class E extends C implements I { int m() { return C.super.m(); } }
     *
     * TEST: I d = new D(); d.m() == 1
     * TEST: C d = new D(); d.m() == 1
     * TEST: D d = new D(); d.m() == 1
     *
     * TEST: I e = new E(); e.m() == 2
     * TEST: C e = new E(); e.m() == 2
     * TEST: E e = new E(); e.m() == 2
     */
    public void testSuperNonDefault2(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        ConcreteClass C = b.clazz("C")
                .concreteMethod("m", "()I").returns(2).build()
            .build();

        ConcreteClass D = b.clazz("D").extend(C).implement(I)
                .concreteMethod("m", "()I").callSuper(I, "m", "()I").build()
            .build();

        ConcreteClass E = b.clazz("E").extend(C).implement(I)
                .concreteMethod("m", "()I").callSuper(C, "m", "()I").build()
            .build();

        b.test().callSite(I, D, "m", "()I").returns(1).done()
         .test().callSite(C, D, "m", "()I").returns(1).done()
         .test().callSite(D, D, "m", "()I").returns(1).done()

         .test().callSite(I, E, "m", "()I").returns(2).done()
         .test().callSite(C, E, "m", "()I").returns(2).done()
         .test().callSite(E, E, "m", "()I").returns(2).done();
    }

    /*
     * Disambig
     *
     * interface I { int m() default { return 1; } }
     * interface J { int m() default { return 2; } }
     * class C implements I, J { int q() { return I.super.m(); }
     *                           int r() { return J.super.m(); } }
     *
     * TEST: C c = new C(); c.m() == ICCE;
     * TEST: C c = new C(); c.q() == 1;
     * TEST: C c = new C(); c.r() == 2;
     */
    public void testDisambig(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J")
                .defaultMethod("m", "()I").returns(2).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I,J)
                .concreteMethod("q", "()I").callSuper(I, "m", "()I").build()
                .concreteMethod("r", "()I").callSuper(J, "m", "()I").build()
            .build();

        b.test().callSite(C, C, "q", "()I").returns(1).done()
         .test().callSite(C, C, "r", "()I").returns(2).done()
         .test().callSite(C, C, "m", "()I").throws_(IncompatibleClassChangeError.class).done();
    }

    /*
     * Disambig2
     *
     * interface I { int m() default { return 1; } }
     * interface J { int m() default { return 2; } }
     * interface K extends I
     * interface L extends J
     * class C implements K, L { int q() { return K.super.m(); }
     *                           int r() { return L.super.m(); } }
     *
     * TEST: C c = new C(); c.m() == ICCE;
     * TEST: C c = new C(); c.q() == 1;
     * TEST: C c = new C(); c.r() == 2;
     */
    public void testDisambig2(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J")
                .defaultMethod("m", "()I").returns(2).build()
            .build();

        Interface K = b.intf("K").extend(I).build();

        Interface L = b.intf("L").extend(J).build();

        ConcreteClass C = b.clazz("C").implement(K,L)
                .concreteMethod("q", "()I").callSuper(K, "m", "()I").build()
                .concreteMethod("r", "()I").callSuper(L, "m", "()I").build()
            .build();

        b.test().callSite(C, C, "q", "()I").returns(1).done()
         .test().callSite(C, C, "r", "()I").returns(2).done()
         .test().callSite(C, C, "m", "()I").throws_(IncompatibleClassChangeError.class).done();
    }

    /*
     * testResolvedShadowed
     *
     * interface I { int m() default { return 1; } }
     * interface K extends I { int m() default { return 2; } }
     * interface J extends I { }
     * class C implements J,K { int q { J.super.m(); } }
     *
     * TEST: C c = new C(); c.m() == 2
     * TEST: C c = new C(); c.q() == 1
     */
    public void testResolvedShadowed(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface K = b.intf("K").extend(I)
                .defaultMethod("m", "()I").returns(2).build()
            .build();

        Interface J = b.intf("J").extend(I)
            .build();

        ConcreteClass C = b.clazz("C").implement(J,K)
                .concreteMethod("q", "()I").callSuper(J, "m", "()I").build()
            .build();

        b.test().callSite(C, C, "m", "()I").returns(2).done()
         .test().callSite(C, C, "q", "()I").returns(1).done();
    }

    /*
     * testResolvedButSuperClass
     *
     * interface I { int m() default { return 1; } }
     * interface J { }
     * class A { public int m() { return 2; } }
     * class C implements J extends A { int q { J.super.m(); } }
     *
     * TEST: C c = new C(); c.q() == 1
     * TEST: C c = new C(); c.m() == 2
     */
    public void testResolvedButSuperClass(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J").extend(I)
            .build();

        ConcreteClass A = b.clazz("A")
             .concreteMethod("m", "()I").returns(2).build()
            .build();

        ConcreteClass C = b.clazz("C").implement(J).extend(A)
                .concreteMethod("q", "()I").callSuper(J, "m", "()I").build()
            .build();

        b.test().callSite(C, C, "q", "()I").returns(1).done()
         .test().callSite(C, C, "m", "()I").returns(2).done();
    }

    /*
     * testResolved1Caller2NotShadowed
     *
     * interface I { int m() default { return 1; } }
     * interface J extends I { }
     * interface L { int m() default { return 2; } }
     * interface K extends I, L { }
     * class C implements J,K { int q { J.super.m(); } }
     *
     * TEST: C c = new C(); c.m() == ICCE
     * TEST: C c = new C(); c.q() == 1
     */
    public void testResolved1Caller2NotShadowed(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J").extend(I).build();

        Interface L = b.intf("L")
                .defaultMethod("m", "()I").returns(2).build()
            .build();

        Interface K = b.intf("K").extend(I,L)
            .build();

        ConcreteClass C = b.clazz("C").implement(J,K)
                .concreteMethod("q", "()I").callSuper(J, "m", "()I").build()
            .build();

        b.test().callSite(C, C, "m", "()I").throws_(IncompatibleClassChangeError.class).done()
         .test().callSite(C, C, "q", "()I").returns(1).done();
    }

    /*
     * Test validity of invokespecial on indirect superinterface's method,
     * this test should receive a verification error.
     *
     * JVMS-4.9.2 Structural Constraints
     * Each invokespecial instruction must name an instance initialization
     * method (2.9), or must reference a method in the current class or interface,
     * a method in a superclass of the current class or interface, or a method
     * in a direct superinterface of the current class or interface
     *
     * Note: Normally javac would reject this test case complaining that,
     * InDirectSuper.java:5: error: not an enclosing class: I
     * interface K extends J { default public void m() { I.super.m(); } }
     *                                             ^
     * However, the below test case allows us to check for this structural
     * constraint on invokespecial in the JVM.
     *
     * interface I { int m() default { return 1; } }
     * interface J extends I { }
     * interface K extends J { int m() default { return I.super.m(); } }
     * class C implements K {}
     *
     * TEST: K k = new C(); k.m() == VerifyError
     */
    @NotApplicableFor(modes = { REDEFINITION }) // Can't redefine a class that gets VerifyError
    public void testSuperInvalidIndirectInterfaceMethodInvokeSpecial(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("m", "()I").returns(1).build()
            .build();

        Interface J = b.intf("J").extend(I).build();

        Interface K = b.intf("K").extend(J)
                .defaultMethod("m", "()I").callSuper(I, "m", "()I").build()
            .build();

        ConcreteClass C = b.clazz("C").implement(K).build();

        b.test().callSite(K, C, "m", "()I").throws_(VerifyError.class).done();
    }
}
