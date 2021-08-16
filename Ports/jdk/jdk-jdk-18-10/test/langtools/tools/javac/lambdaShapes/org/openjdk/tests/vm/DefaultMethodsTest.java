/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.tests.vm;

import org.openjdk.tests.separate.Compiler;
import org.openjdk.tests.separate.TestHarness;
import org.testng.annotations.Test;

import static org.openjdk.tests.separate.SourceModel.AbstractMethod;
import static org.openjdk.tests.separate.SourceModel.AccessFlag;
import static org.openjdk.tests.separate.SourceModel.Class;
import static org.openjdk.tests.separate.SourceModel.ConcreteMethod;
import static org.openjdk.tests.separate.SourceModel.DefaultMethod;
import static org.openjdk.tests.separate.SourceModel.Extends;
import static org.openjdk.tests.separate.SourceModel.Interface;
import static org.openjdk.tests.separate.SourceModel.MethodParameter;
import static org.openjdk.tests.separate.SourceModel.TypeParameter;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

@Test(groups = "vm")
public class DefaultMethodsTest extends TestHarness {
    public DefaultMethodsTest() {
        super(false, false);
    }

    /**
     * class C { public int m() { return 22; } }
     *
     * TEST: C c = new C(); c.m() == 22
     */
    public void testHarnessInvokeVirtual() {
        Class C = new Class("C", ConcreteMethod.std("22"));
        assertInvokeVirtualEquals(22, C);
    }

    /**
     * interface I { int m(); }
     * class C implements I { public int m() { return 33; } }
     *
     * TEST: I i = new C(); i.m() == 33;
     */
    public void testHarnessInvokeInterface() {
        Interface I = new Interface("I", AbstractMethod.std());
        Class C = new Class("C", I, ConcreteMethod.std("33"));
        assertInvokeInterfaceEquals(33, C, I);
    }

    /**
     * class C {}
     *
     * TEST: C c = new C(); c.m() throws NoSuchMethod
     */
    public void testHarnessThrows() {
        Class C = new Class("C");
        assertThrows(NoSuchMethodError.class, C);
    }

    /**
     * interface I { int m() default { return 44; } }
     * class C implements I {}
     *
     * TEST: C c = new C(); c.m() == 44;
     * TEST: I i = new C(); i.m() == 44;
     */
    public void testBasicDefault() {
        Interface I = new Interface("I", DefaultMethod.std("44"));
        Class C = new Class("C", I);

        assertInvokeVirtualEquals(44, C);
        assertInvokeInterfaceEquals(44, C, I);
    }

    /**
     * interface I { default int m() { return 44; } }
     * interface J extends I {}
     * interface K extends J {}
     * class C implements K {}
     *
     * TEST: C c = new C(); c.m() == 44;
     * TEST: I i = new C(); i.m() == 44;
     */
    public void testFarDefault() {
        Interface I = new Interface("I", DefaultMethod.std("44"));
        Interface J = new Interface("J", I);
        Interface K = new Interface("K", J);
        Class C = new Class("C", K);

        assertInvokeVirtualEquals(44, C);
        assertInvokeInterfaceEquals(44, C, K);
    }

    /**
     * interface I { int m(); }
     * interface J extends I { default int m() { return 44; } }
     * interface K extends J {}
     * class C implements K {}
     *
     * TEST: C c = new C(); c.m() == 44;
     * TEST: K k = new C(); k.m() == 44;
     */
    public void testOverrideAbstract() {
        Interface I = new Interface("I", AbstractMethod.std());
        Interface J = new Interface("J", I, DefaultMethod.std("44"));
        Interface K = new Interface("K", J);
        Class C = new Class("C", K);

        assertInvokeVirtualEquals(44, C);
        assertInvokeInterfaceEquals(44, C, K);
    }

    /**
     * interface I { int m() default { return 44; } }
     * class C implements I { public int m() { return 55; } }
     *
     * TEST: C c = new C(); c.m() == 55;
     * TEST: I i = new C(); i.m() == 55;
     */
    public void testExisting() {
        Interface I = new Interface("I", DefaultMethod.std("44"));
        Class C = new Class("C", I, ConcreteMethod.std("55"));

        assertInvokeVirtualEquals(55, C);
        assertInvokeInterfaceEquals(55, C, I);
    }

    /**
     * interface I { default int m() { return 99; } }
     * class B implements I {}
     * class C extends B {}
     *
     * TEST: C c = new C(); c.m() == 99;
     * TEST: I i = new C(); i.m() == 99;
     */
    public void testInherited() {
        Interface I = new Interface("I", DefaultMethod.std("99"));
        Class B = new Class("B", I);
        Class C = new Class("C", B);

        assertInvokeVirtualEquals(99, C);
        assertInvokeInterfaceEquals(99, C, I);
    }

    /**
     * interface I { default int m() { return 99; } }
     * class C { public int m() { return 11; } }
     * class D extends C implements I {}
     *
     * TEST: D d = new D(); d.m() == 11;
     * TEST: I i = new D(); i.m() == 11;
     */
    public void testExistingInherited() {
        Interface I = new Interface("I", DefaultMethod.std("99"));
        Class C = new Class("C", ConcreteMethod.std("11"));
        Class D = new Class("D", C, I);

        assertInvokeVirtualEquals(11, D);
        assertInvokeInterfaceEquals(11, D, I);
    }

    /**
     * interface I { default int m() { return 44; } }
     * class C implements I { public int m() { return 11; } }
     * class D extends C { public int m() { return 22; } }
     *
     * TEST: D d = new D(); d.m() == 22;
     * TEST: I i = new D(); i.m() == 22;
     */
    public void testExistingInheritedOverride() {
        Interface I = new Interface("I", DefaultMethod.std("99"));
        Class C = new Class("C", I, ConcreteMethod.std("11"));
        Class D = new Class("D", C, ConcreteMethod.std("22"));

        assertInvokeVirtualEquals(22, D);
        assertInvokeInterfaceEquals(22, D, I);
    }

    /**
     * interface I { default int m() { return 99; } }
     * interface J { defaultint m() { return 88; } }
     * class C implements I { public int m() { return 11; } }
     * class D extends C { public int m() { return 22; } }
     * class E extends D implements J {}
     *
     * TEST: E e = new E(); e.m() == 22;
     * TEST: J j = new E(); j.m() == 22;
     */
    public void testExistingInheritedPlusDefault() {
        Interface I = new Interface("I", DefaultMethod.std("99"));
        Interface J = new Interface("J", DefaultMethod.std("88"));
        Class C = new Class("C", I, ConcreteMethod.std("11"));
        Class D = new Class("D", C, ConcreteMethod.std("22"));
        Class E = new Class("E", D, J);

        assertInvokeVirtualEquals(22, E);
        assertInvokeInterfaceEquals(22, E, J);
    }

    /**
     * interface I { default int m() { return 99; } }
     * class B implements I {}
     * class C extends B { public int m() { return 77; } }
     *
     * TEST: C c = new C(); c.m() == 77;
     * TEST: I i = new C(); i.m() == 77;
     */
    public void testInheritedWithConcrete() {
        Interface I = new Interface("I", DefaultMethod.std("99"));
        Class B = new Class("B", I);
        Class C = new Class("C", B, ConcreteMethod.std("77"));

        assertInvokeVirtualEquals(77, C);
        assertInvokeInterfaceEquals(77, C, I);
    }

    /**
     * interface I { default int m() { return 99; } }
     * class B implements I {}
     * class C extends B implements I { public int m() { return 66; } }
     *
     * TEST: C c = new C(); c.m() == 66;
     * TEST: I i = new C(); i.m() == 66;
     */
    public void testInheritedWithConcreteAndImpl() {
        Interface I = new Interface("I", DefaultMethod.std("99"));
        Class B = new Class("B", I);
        Class C = new Class("C", B, I, ConcreteMethod.std("66"));

        assertInvokeVirtualEquals(66, C);
        assertInvokeInterfaceEquals(66, C, I);
    }

    /**
     * interface I { default int m() { return 99; } }
     * interface J { default int m() { return 88; } }
     * class C implements I, J {}
     *
     * TEST: C c = new C(); c.m() throws ICCE
     */
    public void testConflict() {
        Interface I = new Interface("I", DefaultMethod.std("99"));
        Interface J = new Interface("J", DefaultMethod.std("88"));
        Class C = new Class("C", I, J);

        assertThrows(IncompatibleClassChangeError.class, C);
    }

    /**
     * interface I { int m(); }
     * interface J { default int m() { return 88; } }
     * class C implements I, J {}
     *
     * TEST: C c = new C(); c.m() == 88
     */
    public void testAmbiguousReabstract() {
        Interface I = new Interface("I", AbstractMethod.std());
        Interface J = new Interface("J", DefaultMethod.std("88"));
        Class C = new Class("C", I, J);

        assertInvokeVirtualEquals(88, C);
    }

    /**
     * interface I { default int m() { return 99; } }
     * interface J extends I { }
     * interface K extends I { }
     * class C implements J, K {}
     *
     * TEST: C c = new C(); c.m() == 99
     * TEST: J j = new C(); j.m() == 99
     * TEST: K k = new C(); k.m() == 99
     * TEST: I i = new C(); i.m() == 99
     */
    public void testDiamond() {
        Interface I = new Interface("I", DefaultMethod.std("99"));
        Interface J = new Interface("J", I);
        Interface K = new Interface("K", I);
        Class C = new Class("C", J, K);

        assertInvokeVirtualEquals(99, C);
        assertInvokeInterfaceEquals(99, C, J);
        assertInvokeInterfaceEquals(99, C, K);
        assertInvokeInterfaceEquals(99, C, I);
    }

    /**
     * interface I { default int m() { return 99; } }
     * interface J extends I { }
     * interface K extends I { }
     * interface L extends I { }
     * interface M extends I { }
     * class C implements I, J, K, L, M {}
     *
     * TEST: C c = new C(); c.m() == 99
     * TEST: J j = new C(); j.m() == 99
     * TEST: K k = new C(); k.m() == 99
     * TEST: I i = new C(); i.m() == 99
     * TEST: L l = new C(); l.m() == 99
     * TEST: M m = new C(); m.m() == 99
     */
    public void testExpandedDiamond() {
        Interface I = new Interface("I", DefaultMethod.std("99"));
        Interface J = new Interface("J", I);
        Interface K = new Interface("K", I);
        Interface L = new Interface("L", I);
        Interface M = new Interface("M", L);
        Class C = new Class("C", I, J, K, L, M);

        assertInvokeVirtualEquals(99, C);
        assertInvokeInterfaceEquals(99, C, J);
        assertInvokeInterfaceEquals(99, C, K);
        assertInvokeInterfaceEquals(99, C, I);
        assertInvokeInterfaceEquals(99, C, L);
        assertInvokeInterfaceEquals(99, C, M);
    }

    /**
     * interface I { int m() default { return 99; } }
     * interface J extends I { int m(); }
     * class C implements J {}
     *
     * TEST: C c = new C(); c.m() throws AME
     */
    public void testReabstract() {
        Interface I = new Interface("I", DefaultMethod.std("99"));
        Interface J = new Interface("J", I, AbstractMethod.std());
        Class C = new Class("C", J);

        assertThrows(AbstractMethodError.class, C);
    }

    /**
     * interface I { default int m() { return 88; } }
     * interface J extends I { default int m() { return 99; } }
     * class C implements J {}
     *
     * TEST: C c = new C(); c.m() == 99;
     * TEST: J j = new C(); j.m() == 99;
     * TEST: I i = new C(); i.m() == 99;
     */
    public void testShadow() {
        Interface I = new Interface("I", DefaultMethod.std("88"));
        Interface J = new Interface("J", I, DefaultMethod.std("99"));
        Class C = new Class("C", J);

        assertInvokeVirtualEquals(99, C);
        assertInvokeInterfaceEquals(99, C, J);
        assertInvokeInterfaceEquals(99, C, I);
    }

    /**
     * interface I { default int m() { return 88; } }
     * interface J extends I { default int m() { return 99; } }
     * class C implements I, J {}
     *
     * TEST: C c = new C(); c.m() == 99;
     * TEST: J j = new C(); j.m() == 99;
     * TEST: I i = new C(); i.m() == 99;
     */
    public void testDisqualified() {
        Interface I = new Interface("I", DefaultMethod.std("88"));
        Interface J = new Interface("J", I, DefaultMethod.std("99"));
        Class C = new Class("C", I, J);

        assertInvokeVirtualEquals(99, C);
        assertInvokeInterfaceEquals(99, C, J);
        assertInvokeInterfaceEquals(99, C, I);
    }

    /**
     * interface I<T> { default int m(T t) { return 99; } }
     * Class C implements I<String> { public int m(String s) { return 88; } }
     *
     * TEST: C c = new C(); c.m("string") == 88;
     * TEST: I i = new C(); i.m("string") == 88;
     */
    public void testSelfFill() {
        // This test ensures that a concrete method overrides a default method
        // that matches at the language-level, but has a different method
        // signature due to erasure.

        DefaultMethod dm = new DefaultMethod(
            "int", "m", "return 99;", new MethodParameter("T", "t"));
        ConcreteMethod cm = new ConcreteMethod(
            "int", "m", "return 88;", AccessFlag.PUBLIC,
            new MethodParameter("String", "s"));

        Interface I = new Interface("I", new TypeParameter("T"), dm);
        Class C = new Class("C", I.with("String"), cm);

        AbstractMethod pm = new AbstractMethod(
            "int", "m", new MethodParameter("T", "t"));

        assertInvokeVirtualEquals(88, C, cm, "-1", "\"string\"");
        assertInvokeInterfaceEquals(99, C, I.with("String"), pm, "\"string\"");

        C.setFullCompilation(true); // Force full bridge generation
        assertInvokeInterfaceEquals(88, C, I.with("String"), pm, "\"string\"");
    }

    /**
     * interface I { default int m() { return 99; } }
     * class C implements I {}
     *
     * TEST: C.class.getMethod("m").invoke(new C()) == 99
     */
    public void testReflectCall() {
        Interface I = new Interface("I", DefaultMethod.std("99"));
        //workaround accessibility issue when loading C with DirectedClassLoader
        I.addAccessFlag(AccessFlag.PUBLIC);
        Class C = new Class("C", I);

        Compiler.Flags[] flags = this.verbose ?
            new Compiler.Flags[] { Compiler.Flags.VERBOSE } :
            new Compiler.Flags[] {};
        Compiler compiler = new Compiler(flags);
        java.lang.Class<?> cls = null;
        try {
            cls = compiler.compileAndLoad(C);
        } catch (ClassNotFoundException e) {
            fail("Could not load class");
        }

        java.lang.reflect.Method method = null;
        try {
            method = cls.getMethod(stdMethodName);
        } catch (NoSuchMethodException e) {
            fail("Could not find method in class");
        }
        assertNotNull(method);

        Object c = null;
        try {
            c = cls.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            fail("Could not create instance of class");
        }
        assertNotNull(c);

        Integer res = null;
        try {
            res = (Integer)method.invoke(c);
        } catch (IllegalAccessException |
                 java.lang.reflect.InvocationTargetException e) {
            fail("Could not invoke default instance method");
        }
        assertNotNull(res);

        assertEquals(res.intValue(), 99);

        compiler.cleanup();
    }

    /**
     * interface I<T,V,W> { default int m(T t, V v, W w) { return 99; } }
     * interface J<T,V> extends I<String,T,V> { int m(T t, V v, String w); } }
     * interface K<T> extends J<String,T> { int m(T t, String v, String w); } }
     * class C implements K<String> {
     *     public int m(String t, String v, String w) { return 88; }
     * }
     *
     * TEST: I<String,String,String> i = new C(); i.m("A","B","C") == 88;
     * TEST: J<String,String> j = new C(); j.m("A","B","C") == 88;
     * TEST: K<String> k = new C(); k.m("A","B","C") == 88;
     */
    public void testBridges() {
        DefaultMethod dm = new DefaultMethod("int", stdMethodName, "return 99;",
            new MethodParameter("T", "t"), new MethodParameter("V", "v"),
            new MethodParameter("W", "w"));

        AbstractMethod pm0 = new AbstractMethod("int", stdMethodName,
            new MethodParameter("T", "t"), new MethodParameter("V", "v"),
            new MethodParameter("W", "w"));

        AbstractMethod pm1 = new AbstractMethod("int", stdMethodName,
            new MethodParameter("T", "t"), new MethodParameter("V", "v"),
            new MethodParameter("String", "w"));

        AbstractMethod pm2 = new AbstractMethod("int", stdMethodName,
            new MethodParameter("T", "t"), new MethodParameter("String", "v"),
            new MethodParameter("String", "w"));

        ConcreteMethod cm = new ConcreteMethod("int",stdMethodName,"return 88;",
            AccessFlag.PUBLIC,
            new MethodParameter("String", "t"),
            new MethodParameter("String", "v"),
            new MethodParameter("String", "w"));

        Interface I = new Interface("I", new TypeParameter("T"),
            new TypeParameter("V"), new TypeParameter("W"), dm);
        Interface J = new Interface("J",
            new TypeParameter("T"), new TypeParameter("V"),
            I.with("String", "T", "V"), pm1);
        Interface K = new Interface("K", new TypeParameter("T"),
            J.with("String", "T"), pm2);
        Class C = new Class("C", K.with("String"), cm);

        // First, without compiler bridges
        String[] args = new String[] { "\"A\"", "\"B\"", "\"C\"" };
        assertInvokeInterfaceEquals(99, C, I.with("String", "String", "String"), pm0, args);
        assertInvokeInterfaceThrows(AbstractMethodError.class, C, J.with("String", "String"), pm1, args);
        assertInvokeInterfaceThrows(AbstractMethodError.class, C, K.with("String"), pm2, args);

        // Then with compiler bridges
        C.setFullCompilation(true);
        assertInvokeInterfaceEquals(88, C, I.with("String", "String", "String"), pm0, args);
        assertInvokeInterfaceEquals(88, C, J.with("String", "String"), pm1, args);
        assertInvokeInterfaceEquals(88, C, K.with("String"), pm2, args);
    }

    /**
     * interface J { default int m() { return 88; } }
     * interface I extends J { default int m() { return J.super.m(); } }
     * class C implements I {}
     *
     * TEST: C c = new C(); c.m() == 88;
     * TEST: I i = new C(); i.m() == 88;
     */
    public void testSuperBasic() {
        Interface J = new Interface("J", DefaultMethod.std("88"));
        Interface I = new Interface("I", J, new DefaultMethod(
            "int", stdMethodName, "return J.super.m();"));
        I.addCompilationDependency(J.findMethod(stdMethodName));
        Class C = new Class("C", I);

        assertInvokeVirtualEquals(88, C);
        assertInvokeInterfaceEquals(88, C, I);
    }

    /**
     * interface K { int m() default { return 99; } }
     * interface L { int m() default { return 101; } }
     * interface J extends K, L {}
     * interface I extends J, K { int m() default { J.super.m(); } }
     * class C implements I {}
     *
     * TEST: C c = new C(); c.m() throws ICCE
     * TODO: add case for K k = new C(); k.m() throws ICCE
     */
    public void testSuperConflict() {
        Interface K = new Interface("K", DefaultMethod.std("99"));
        Interface L = new Interface("L", DefaultMethod.std("101"));
        Interface J = new Interface("J", K, L);
        Interface I = new Interface("I", J, K, new DefaultMethod(
            "int", stdMethodName, "return J.super.m();"));
        Interface Jstub = new Interface("J", DefaultMethod.std("-1"));
        I.addCompilationDependency(Jstub);
        I.addCompilationDependency(Jstub.findMethod(stdMethodName));
        Class C = new Class("C", I);

        assertThrows(IncompatibleClassChangeError.class, C);
    }

    /**
     * interface I { default int m() { return 99; } }
     * interface J extends I { default int m() { return 55; } }
     * class C implements I, J { public int m() { return I.super.m(); } }
     *
     * TEST: C c = new C(); c.m() == 99
     * TODO: add case for J j = new C(); j.m() == ???
     */
    public void testSuperDisqual() {
        Interface I = new Interface("I", DefaultMethod.std("99"));
        Interface J = new Interface("J", I, DefaultMethod.std("55"));
        Class C = new Class("C", I, J,
            new ConcreteMethod("int", stdMethodName, "return I.super.m();",
                AccessFlag.PUBLIC));
        C.addCompilationDependency(I.findMethod(stdMethodName));

        assertInvokeVirtualEquals(99, C);
    }

    /**
     * interface J { int m(); }
     * interface I extends J { default int m() { return J.super.m(); } }
     * class C implements I {}
     *
     * TEST: C c = new C(); c.m() throws AME
     * TODO: add case for I i = new C(); i.m() throws AME
     */
    public void testSuperNull() {
        Interface J = new Interface("J", AbstractMethod.std());
        Interface I = new Interface("I", J, new DefaultMethod(
            "int", stdMethodName, "return J.super.m();"));
        Interface Jstub = new Interface("J", DefaultMethod.std("99"));
        I.addCompilationDependency(Jstub);
        I.addCompilationDependency(Jstub.findMethod(stdMethodName));
        Class C = new Class("C", I);

        assertThrows(AbstractMethodError.class, C);
    }

    /**
     * interface J<T> { default int m(T t) { return 88; } }
     * interface I extends J<String> {
     *     int m(String s) default { return J.super.m(); }
     * }
     * class C implements I {}
     *
     * TEST: I i = new C(); i.m("") == 88;
     */
    public void testSuperGeneric() {
        Interface J = new Interface("J", new TypeParameter("T"),
            new DefaultMethod("int", stdMethodName, "return 88;",
                new MethodParameter("T", "t")));
        Interface I = new Interface("I", J.with("String"),
            new DefaultMethod("int", stdMethodName, "return J.super.m(s);",
                new MethodParameter("String", "s")));
        I.addCompilationDependency(J.findMethod(stdMethodName));
        Class C = new Class("C", I);

        AbstractMethod pm = new AbstractMethod("int", stdMethodName,
            new MethodParameter("String", "s"));

        assertInvokeInterfaceEquals(88, C, new Extends(I), pm, "\"\"");
    }

    /**
     * interface I<T> { int m(T t) default { return 44; } }
     * interface J extends I<String> { int m(String s) default { return 55; } }
     * class C implements I<String>, J {
     *     public int m(String s) { return I.super.m(s); }
     * }
     *
     * TEST: C c = new C(); c.m("string") == 44
     */
    public void testSuperGenericDisqual() {
        MethodParameter t = new MethodParameter("T", "t");
        MethodParameter s = new MethodParameter("String", "s");

        Interface I = new Interface("I", new TypeParameter("T"),
            new DefaultMethod("int", stdMethodName, "return 44;", t));
        Interface J = new Interface("J", I.with("String"),
            new DefaultMethod("int", stdMethodName, "return 55;", s));
        Class C = new Class("C", I.with("String"), J,
            new ConcreteMethod("int", stdMethodName,
                "return I.super.m(s);", AccessFlag.PUBLIC, s));
        C.addCompilationDependency(I.findMethod(stdMethodName));

        assertInvokeVirtualEquals(44, C,
            new ConcreteMethod(
                "int", stdMethodName, "return -1;", AccessFlag.PUBLIC, s),
            "-1", "\"string\"");
    }

    /**
     * interface I { default Integer m() { return new Integer(88); } }
     * class C { Number m() { return new Integer(99); } }
     * class D extends C implements I {}
     * class S { Object foo() { return (new D()).m(); } // link sig: ()LInteger;
     * TEST: S s = new S(); s.foo() == new Integer(99)
     */
    public void testCovarBridge() {
        Interface I = new Interface("I", new DefaultMethod(
            "Integer", "m", "return new Integer(88);"));
        Class C = new Class("C", new ConcreteMethod(
            "Number", "m", "return new Integer(99);", AccessFlag.PUBLIC));
        Class D = new Class("D", I, C);

        ConcreteMethod DstubMethod = new ConcreteMethod(
            "Integer", "m", "return null;", AccessFlag.PUBLIC);
        Class Dstub = new Class("D", DstubMethod);

        ConcreteMethod toCall = new ConcreteMethod(
            "Object", "foo", "return (new D()).m();", AccessFlag.PUBLIC);
        Class S = new Class("S", D, toCall);
        S.addCompilationDependency(Dstub);
        S.addCompilationDependency(DstubMethod);

        // NEGATIVE test for separate compilation -- dispatches to I, not C
        assertInvokeVirtualEquals(88, S, toCall, "null");
    }

    /**
     * interface I { default Integer m() { return new Integer(88); } }
     * class C { int m() { return 99; } }
     * class D extends C implements I {}
     * class S { Object foo() { return (new D()).m(); } // link sig: ()LInteger;
     * TEST: S s = new S(); s.foo() == new Integer(88)
     */
    public void testNoCovarNoBridge() {
        Interface I = new Interface("I", new DefaultMethod(
            "Integer", "m", "return new Integer(88);"));
        Class C = new Class("C", new ConcreteMethod(
            "int", "m", "return 99;", AccessFlag.PUBLIC));
        Class D = new Class("D", I, C);

        ConcreteMethod DstubMethod = new ConcreteMethod(
            "Integer", "m", "return null;", AccessFlag.PUBLIC);
        Class Dstub = new Class("D", DstubMethod);

        ConcreteMethod toCall = new ConcreteMethod(
            "Object", "foo", "return (new D()).m();", AccessFlag.PUBLIC);
        Class S = new Class("S", D, toCall);
        S.addCompilationDependency(Dstub);
        S.addCompilationDependency(DstubMethod);

        assertInvokeVirtualEquals(88, S, toCall, "null");
    }

    /**
     * interface J { int m(); }
     * interface I extends J { default int m() { return 99; } }
     * class B implements J {}
     * class C extends B implements I {}
     * TEST: C c = new C(); c.m() == 99
     *
     * The point of this test is that B does not get default method analysis,
     * and C does not generate any new miranda methods in the vtable.
     * It verifies that default method analysis occurs when mirandas have been
     * inherited and the supertypes don't have any overpass methods.
     */
    public void testNoNewMiranda() {
        Interface J = new Interface("J", AbstractMethod.std());
        Interface I = new Interface("I", J, DefaultMethod.std("99"));
        Class B = new Class("B", J);
        Class C = new Class("C", B, I);
        assertInvokeVirtualEquals(99, C);
    }

    /**
     * interface I<T,V,W> { int m(T t, V v, W w); }
     * interface J<T,V> implements I<T,V,String> { int m(T t, V v, String w); }
     * interface K<T> implements J<T,String> {
     *     int m(T t, String v, String w); { return 99; } }
     * class C implements K<String> {
     *     public int m(Object t, Object v, String w) { return 77; }
     * }
     * TEST C = new C(); ((I)c).m(Object,Object,Object) == 99
     * TEST C = new C(); ((J)c).m(Object,Object,String) == 77
     * TEST C = new C(); ((K)c).m(Object,String,String) == 99
     *
     * Test that a erased-signature-matching method does not implement
     * non-language-level matching methods
     */
    public void testNonConcreteFill() {
        AbstractMethod ipm = new AbstractMethod("int", "m",
            new MethodParameter("T", "t"),
            new MethodParameter("V", "s"),
            new MethodParameter("W", "w"));
        Interface I = new Interface("I",
            new TypeParameter("T"),
            new TypeParameter("V"),
            new TypeParameter("W"), ipm);

        AbstractMethod jpm = new AbstractMethod("int", "m",
            new MethodParameter("T", "t"),
            new MethodParameter("V", "s"),
            new MethodParameter("String", "w"));
        Interface J = new Interface("J",
            new TypeParameter("T"),
            new TypeParameter("V"),
            I.with("T", "V", "String"), jpm);

        AbstractMethod kpm = new AbstractMethod("int", "m",
            new MethodParameter("T", "t"),
            new MethodParameter("String", "s"),
            new MethodParameter("String", "w"));
        DefaultMethod kdm = new DefaultMethod("int", "m", "return 99;",
                                              new MethodParameter("T", "t"),
                                              new MethodParameter("String", "v"),
                                              new MethodParameter("String", "w"));
        Interface K = new Interface("K",
            new TypeParameter("T"),
            J.with("T", "String"),
            kdm);

        Class C = new Class("C",
            K.with("String"),
            new ConcreteMethod("int", "m", "return 77;",
                AccessFlag.PUBLIC,
                new MethodParameter("Object", "t"),
                new MethodParameter("Object", "v"),
                new MethodParameter("String", "w")));

        // First, without compiler bridges
        String a = "\"\"";
        assertInvokeInterfaceEquals(99, C, K.with("String"), kpm, a, a, a);
        assertInvokeInterfaceEquals(77, C, J.with("String", "String"), jpm, a, a, a);
        assertInvokeInterfaceThrows(AbstractMethodError.class, C, I.with("String", "String", "String"), ipm, a, a, a);

        // Now, with bridges
        J.setFullCompilation(true);
        K.setFullCompilation(true);
        assertInvokeInterfaceEquals(99, C, K.with("String"), kpm, a, a, a);
        assertInvokeInterfaceEquals(77, C, J.with("String", "String"), jpm, a, a, a);
        assertInvokeInterfaceEquals(99, C, I.with("String", "String", "String"), ipm, a, a, a);
    }

    public void testStrictfpDefault() {
        try {
            java.lang.Class.forName("org.openjdk.tests.vm.StrictfpDefault");
        } catch (Exception e) {
            fail("Could not load class", e);
        }
    }
}

interface StrictfpDefault {
    default strictfp void m() {}
}
