/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8003639
 * @summary convert lambda testng tests to jtreg and add them
 * @run testng LambdaTranslationTest2
 */

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * LambdaTranslationTest2 -- end-to-end smoke tests for lambda evaluation
 */

@Test
public class LambdaTranslationTest2 {

    final String dummy = "dummy";

    public void testLambdas() {
        TPredicate<String> isEmpty = s -> s.isEmpty();
        assertTrue(isEmpty.test(""));
        assertTrue(!isEmpty.test("foo"));

        TPredicate<Object> oIsEmpty = s -> ((String) s).isEmpty();
        assertTrue(oIsEmpty.test(""));
        assertTrue(!oIsEmpty.test("foo"));

        TPredicate<Object> alwaysTrue = o -> true;
        assertTrue(alwaysTrue.test(""));
        assertTrue(alwaysTrue.test(null));

        TPredicate<Object> alwaysFalse = o -> false;
        assertTrue(!alwaysFalse.test(""));
        assertTrue(!alwaysFalse.test(null));

        // tests local capture
        String foo = "foo";
        TPredicate<String> equalsFoo = s -> s.equals(foo);
        assertTrue(!equalsFoo.test(""));
        assertTrue(equalsFoo.test("foo"));

        // tests instance capture
        TPredicate<String> equalsDummy = s -> s.equals(dummy);
        assertTrue(!equalsDummy.test(""));
        assertTrue(equalsDummy.test("dummy"));

        TMapper<Object, Object> ident = s -> s;

        assertEquals("blarf", ident.map("blarf"));
        assertEquals("wooga", ident.map("wooga"));
        assertTrue("wooga" == ident.map("wooga"));

        // constant capture
        TMapper<Object, Object> prefixer = s -> "p" + s;
        assertEquals("pblarf", prefixer.map("blarf"));
        assertEquals("pwooga", prefixer.map("wooga"));

        // instance capture
        TMapper<Object, Object> prefixer2 = s -> dummy + s;
        assertEquals("dummyblarf", prefixer2.map("blarf"));
        assertEquals("dummywooga", prefixer2.map("wooga"));
    }

    interface Factory<T> {
        T make();
    }

    interface StringFactory extends Factory<String> { }

    interface StringFactory2 extends Factory<String> {
        String make();
    }

    public void testBridges() {
        Factory<String> of = () -> "y";
        Factory<?> ef = () -> "z";

        assertEquals("y", of.make());
        assertEquals("y", ((Factory<?>) of).make());
        assertEquals("y", ((Factory) of).make());

        assertEquals("z", ef.make());
        assertEquals("z", ((Factory) ef).make());
    }

    public void testBridgesImplicitSpecialization() {
        StringFactory sf = () -> "x";

        assertEquals("x", sf.make());
        assertEquals("x", ((Factory<String>) sf).make());
        assertEquals("x", ((Factory<?>) sf).make());
        assertEquals("x", ((Factory) sf).make());
    }

    public void testBridgesExplicitSpecialization() {
        StringFactory2 sf = () -> "x";

        assertEquals("x", sf.make());
        assertEquals("x", ((Factory<String>) sf).make());
        assertEquals("x", ((Factory<?>) sf).make());
        assertEquals("x", ((Factory) sf).make());
    }

    public void testSuperCapture() {
        class A {
            String make() { return "x"; }
        }

        class B extends A {
            void testSuperCapture() {
                StringFactory sf = () -> super.make();
                assertEquals("x", sf.make());
            }
        }

        new B().testSuperCapture();
    }

    interface WidenD {
        public String m(float a0, double a1);
    }

    interface WidenS {
        public String m(byte a0, short a1);
    }

    interface WidenI {
        public String m(byte a0, short a1, char a2, int a3);
    }

    interface WidenL {
        public String m(byte a0, short a1, char a2, int a3, long a4);
    }

    interface Box {
        public String m(byte a0, short a1, char a2, int a3, long a4, boolean a5, float a6, double a7);
    }

    static String pb(Byte a0, Short a1, Character a2, Integer a3, Long a4, Boolean a5, Float a6, Double a7) {
        return String.format("b%d s%d c%c i%d j%d z%b f%f d%f", a0, a1, a2, a3, a4, a5, a6, a7);
    }

    static String pwI1(int a0, int a1, int a2, int a3) {
        return String.format("b%d s%d c%d i%d", a0, a1, a2, a3);
    }

    static String pwI2(Integer a0, Integer a1, Integer a2, Integer a3) {
        return String.format("b%d s%d c%d i%d", a0, a1, a2, a3);
    }

    static String pwL1(long a0, long a1, long a2, long a3, long a4) {
        return String.format("b%d s%d c%d i%d j%d", a0, a1, a2, a3, a4);
    }

    static String pwL2(Long a0, Long a1, Long a2, Long a3, Long a4) {
        return String.format("b%d s%d c%d i%d j%d", a0, a1, a2, a3, a4);
    }

    static String pwS1(short a0, short a1) {
        return String.format("b%d s%d", a0, a1);
    }

    static String pwS2(Short a0, Short a1) {
        return String.format("b%d s%d", a0, a1);
    }

    static String pwD1(double a0, double a1) {
        return String.format("f%f d%f", a0, a1);
    }

    static String pwD2(Double a0, Double a1) {
        return String.format("f%f d%f", a0, a1);
    }

    public void testPrimitiveWidening() {
        WidenS ws1 = LambdaTranslationTest2::pwS1;
        assertEquals("b1 s2", ws1.m((byte) 1, (short) 2));

        WidenD wd1 = LambdaTranslationTest2::pwD1;
        assertEquals("f1.000000 d2.000000", wd1.m(1.0f, 2.0));

        WidenI wi1 = LambdaTranslationTest2::pwI1;
        assertEquals("b1 s2 c3 i4", wi1.m((byte) 1, (short) 2, (char) 3, 4));

        WidenL wl1 = LambdaTranslationTest2::pwL1;
        assertEquals("b1 s2 c3 i4 j5", wl1.m((byte) 1, (short) 2, (char) 3, 4, 5L));

        // @@@ TODO: clarify spec on widen+box conversion
    }

    interface Unbox {
        public String m(Byte a0, Short a1, Character a2, Integer a3, Long a4, Boolean a5, Float a6, Double a7);
    }

    static String pu(byte a0, short a1, char a2, int a3, long a4, boolean a5, float a6, double a7) {
        return String.format("b%d s%d c%c i%d j%d z%b f%f d%f", a0, a1, a2, a3, a4, a5, a6, a7);
    }

    public void testUnboxing() {
        Unbox u = LambdaTranslationTest2::pu;
        assertEquals("b1 s2 cA i4 j5 ztrue f6.000000 d7.000000", u.m((byte)1, (short) 2, 'A', 4, 5L, true, 6.0f, 7.0));
    }

    public void testBoxing() {
        Box b = LambdaTranslationTest2::pb;
        assertEquals("b1 s2 cA i4 j5 ztrue f6.000000 d7.000000", b.m((byte) 1, (short) 2, 'A', 4, 5L, true, 6.0f, 7.0));
    }

    static boolean cc(Object o) {
        return ((String) o).equals("foo");
    }

    public void testArgCastingAdaptation() {
        TPredicate<String> p = LambdaTranslationTest2::cc;
        assertTrue(p.test("foo"));
        assertTrue(!p.test("bar"));
    }

    interface SonOfPredicate<T> extends TPredicate<T> { }

    public void testExtendsSAM() {
        SonOfPredicate<String> p = s -> s.isEmpty();
        assertTrue(p.test(""));
        assertTrue(!p.test("foo"));
    }

    public void testConstructorRef() {
        Factory<List<String>> lf = ArrayList<String>::new;
        List<String> list = lf.make();
        assertTrue(list instanceof ArrayList);
        assertTrue(list != lf.make());
        list.add("a");
        assertEquals("[a]", list.toString());
    }

    private static String privateMethod() {
        return "private";
    }

    public void testPrivateMethodRef() {
        Factory<String> sf = LambdaTranslationTest2::privateMethod;
        assertEquals("private", sf.make());
    }

    private interface PrivateIntf {
        String make();
    }

    public void testPrivateIntf() {
        PrivateIntf p = () -> "foo";
        assertEquals("foo", p.make());
    }

    interface Op<T> {
        public T op(T a, T b);
    }

    public void testBoxToObject() {
        Op<Integer> maxer = Math::max;
        for (int i=-100000; i < 100000; i += 100)
            for (int j=-100000; j < 100000; j += 99) {
                assertEquals((int) maxer.op(i,j), Math.max(i,j));
            }
    }

    protected static String protectedMethod() {
        return "protected";
    }

    public void testProtectedMethodRef() {
        Factory<String> sf = LambdaTranslationTest2::protectedMethod;
        assertEquals("protected", sf.make());
    }

    class Inner1 {
        String m1() {
            return "Inner1.m1()";
        }

        class Inner2 {
            public String m1() {
                return "Inner1.Inner2.m1()";
            }

            protected String m2() {
                return "Inner1.Inner2.m2()";
            }

            String m3() {
                return "Inner1.Inner2.m3()";
            }

            class Inner3<T> {
                T t = null;
                Inner3(T t) {
                    this.t = t;
                }
                T m1() {
                    return t;
                }
            }
        }
    }

    public void testInnerClassMethodRef() {
        Factory<String> fs = new Inner1()::m1;
        assertEquals("Inner1.m1()", fs.make());

        fs = new Inner1().new Inner2()::m1;
        assertEquals("Inner1.Inner2.m1()", fs.make());

        fs = new Inner1().new Inner2()::m2;
        assertEquals("Inner1.Inner2.m2()", fs.make());

        fs = new Inner1().new Inner2()::m3;
        assertEquals("Inner1.Inner2.m3()", fs.make());

        fs = new Inner1().new Inner2().new Inner3<String>("Inner1.Inner2.Inner3")::m1;
        assertEquals("Inner1.Inner2.Inner3", fs.make());

        Factory<Integer> fsi = new Inner1().new Inner2().new Inner3<Integer>(100)::m1;
        assertEquals(100, (int)fsi.make());
    }
}
