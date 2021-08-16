/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8144903 8171981 8191802 8191842
 * @summary Tests for determining the type from the expression
 * @build KullaTesting TestingInputStream
 * @run testng TypeNameTest
 */

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

@Test
public class TypeNameTest extends KullaTesting {


    private void assertType(String expr, String type) {
        assertType(expr, type, type);
    }

    private void assertType(String expr, String type, String inferType) {
        assertEquals(varKey(assertEval(expr)).typeName(), type);
        assertInferredType(expr, inferType);
    }

    public void testTypeInference() {
        assertEval("import java.util.List;");
        assertEval("import java.util.ArrayList;");
        assertEval("import java.util.Arrays;");

        assertType("new Object().getClass().getSuperclass() ", "Class<?>");
        assertType("new ArrayList().getClass().getSuperclass()", "Class<?>");
        assertType("new ArrayList().getClass()", "Class<? extends ArrayList>");
        assertType("ArrayList.class", "Class<ArrayList>");
        assertType("ArrayList.class.getSuperclass()", "Class<? super ArrayList>");

        assertEval("class D<T extends CharSequence> { D<? super T> getS() { return null; } }");
        assertEval("D<?> d = new D<String>();");
        assertType("d.getS()", "D<?>");
        assertType("null", "Object");
        assertType("Class.forName( \"java.util.ArrayList\" )", "Class<?>");
        assertType("new ArrayList<Boolean>() {}", "<anonymous class extending ArrayList<Boolean>>", "ArrayList<Boolean>");
        assertType("new ArrayList<String>().stream()", "java.util.stream.Stream<String>");
        assertType("Arrays.asList( 1, 2, 3)", "List<Integer>");
        assertType("new ArrayList().getClass().getClass()", "Class<? extends Class>");

        assertEval("interface A {}");
        assertEval("interface I {}");
        assertEval("interface J extends A, I {}");
        assertEval("interface K extends A, I {}");
        assertEval("class P<T extends A & I> {}");
        assertType("(P<?>) null", "P<?>");
    }

    public void testConditionals() {
        assertEval("import java.util.List;");
        assertEval("import java.util.ArrayList;");
        assertEval("import java.util.Arrays;");

        assertEval("CharSequence cs = \"hi\";");
        assertEval("String st = \"low\";");
        assertEval("boolean b;");
        assertType("b? cs : st", "CharSequence");

        assertEval("List<String> l1 = Arrays.asList(\"hi\");");
        assertEval("List<? extends String> l2 = Arrays.asList(\"po\");");
        assertType("b? l1.get(0) : l2.get(0)", "String");

        assertEval("class X {}");
        assertEval("class B extends X {}");
        assertEval("class C extends X {}");
        assertType("b? new B() : new C()", "X");
    }

    public void testJEP286NonDenotable() {
        assertEval("import java.util.List;");
        assertEval("import java.util.Arrays;");
        assertEval("import java.util.Iterator;");

        assertEval("List<? extends String> extString() { return Arrays.asList( \"hi\", \"low\" ); }");
        assertEval("List<? super String> supString() { return Arrays.asList( \"hi\", \"low\" ); }");
        assertEval("List<?> unbString() { return Arrays.asList( \"hi\", \"low\" ); }");
        assertEval("List<? extends String>[] extStringArr() {" +
                " @SuppressWarnings(\"unchecked\") " +
                "List<? extends String>[] a = new List[1]; a[0] = Arrays.asList(\"hi\"); return a; }");
        assertEval("List<? super String>[] supStringArr() {" +
                " @SuppressWarnings(\"unchecked\") " +
                "List<? super String>[] a = new List[1]; a[0] = Arrays.asList(\"hi\"); return a; }");
        assertEval("List<?>[] unbStringArr() {" +
                " @SuppressWarnings(\"unchecked\") " +
                "List<?>[] a = new List[1]; a[0] = Arrays.asList(\"hi\"); return a; }");
        assertEval("Iterable<? extends List<? extends String>> extStringIter() {" +
                "return Arrays.asList( Arrays.asList( \"hi\" ) ); }");
        assertEval("Iterable<? extends List<? super String>> supStringIter() {" +
                "return Arrays.asList( Arrays.asList( \"hi\" ) ); }");
        assertEval("Iterable<? extends List<?>> unbStringIter() {" +
                "return Arrays.asList( Arrays.asList( \"hi\" ) ); }");
        assertType("extString()", "List<? extends String>");
        assertType("extString().get(0)", "String");
        assertType("supString()", "List<? super String>");
        assertType("supString().get(0)", "Object");
        assertType("unbString()", "List<?>");
        assertType("unbString().get(0)", "Object");
        assertType("supStringArr()", "List<? super String>[]");
        assertType("supStringArr()[0]", "List<? super String>");
        assertType("supStringArr()[0].get(0)", "Object");
        assertType("unbStringArr()", "List<?>[]");
        assertType("unbStringArr()[0]", "List<?>");
        assertType("unbStringArr()[0].get(0)", "Object");
        assertType("extStringIter()", "Iterable<? extends List<? extends String>>");
        assertType("extStringIter().iterator()", "Iterator<? extends List<? extends String>>");
        assertType("extStringIter().iterator().next()", "List<? extends String>");
        assertType("extStringIter().iterator().next().get(0)", "String");
        assertType("supStringIter()", "Iterable<? extends List<? super String>>");
        assertType("supStringIter().iterator()", "Iterator<? extends List<? super String>>");
        assertType("supStringIter().iterator().next()", "List<? super String>");
        assertType("supStringIter().iterator().next().get(0)", "Object");
        assertType("unbStringIter()", "Iterable<? extends List<?>>");
        assertType("unbStringIter().iterator()", "Iterator<? extends List<?>>");
        assertType("unbStringIter().iterator().next()", "List<?>");
        assertType("unbStringIter().iterator().next().get(0)", "Object");
    }

    public void testJEP286NonDenotable2() {
        assertEval("import java.util.List;");
        assertEval("import java.util.Arrays;");
        assertEval("import java.lang.reflect.Array;");

        assertEval("<Z extends Comparable<Z>> List<? extends Z> extFbound() {" +
                "return Arrays.asList( (Z)null ); }");
        assertEval("<Z extends Comparable<Z>> List<? super Z> supFbound() {" +
                "return Arrays.asList( (Z)null ); }");
        assertEval("<Z extends Comparable<Z>> List<? extends Z>[] extFboundArr() {" +
                "@SuppressWarnings(\"unchecked\")" +
                "List<? extends Z>[] a = new List[1]; a[0] = Arrays.asList( (Z)null ); return a; }");
        assertEval("<Z extends Comparable<Z>> List<? super Z>[] supFboundArr() {" +
                "@SuppressWarnings(\"unchecked\")" +
                "List<? super Z>[] a = new List[1]; a[0] = Arrays.asList( (Z)null ); return a; }");
        assertEval("<Z extends Comparable<Z>> Iterable<? extends List<? extends Z>> extFboundIter() {" +
                "return Arrays.asList( Arrays.asList( (Z)null ) ); }");
        assertEval("<Z extends Comparable<Z>> Iterable<? extends List<? super Z>> supFboundIter() {" +
                "return Arrays.asList( Arrays.asList( (Z)null ) ); }");
        assertEval("<Z> List<Z> listOf(Z z) { return Arrays.asList( z ); }");
        assertEval("<Z> Z[] arrayOf(Z z) {" +
                "@SuppressWarnings(\"unchecked\")" +
                "final Z[] a = (Z[]) Array.newInstance(z.getClass(), 1); a[0] = z; return a; }");
        assertType("extFbound()", "List<? extends Comparable<?>>");
        assertType("extFbound().get(0)", "Comparable<?>");
        assertType("supFbound()", "List<?>");
        assertType("supFbound().get(0)", "Object");
        assertType("extFboundArr()", "List<? extends Comparable<?>>[]");
        assertType("extFboundArr()[0]", "List<? extends Comparable<?>>");
        assertType("extFboundArr()[0].get(0)", "Comparable<?>");
        assertType("supFboundArr()", "List<?>[]");
        assertType("supFboundArr()[0]", "List<?>");
        assertType("supFboundArr()[0].get(0)", "Object");
        assertType("extFboundIter()", "Iterable<? extends List<? extends Comparable<?>>>");
        assertType("extFboundIter().iterator()", "java.util.Iterator<? extends List<? extends Comparable<?>>>");
        assertType("extFboundIter().iterator().next()", "List<? extends Comparable<?>>");
        assertType("extFboundIter().iterator().next().get(0)", "Comparable<?>");
        assertType("supFboundIter()", "Iterable<? extends List<?>>");
        assertType("supFboundIter().iterator()", "java.util.Iterator<? extends List<?>>");
        assertType("supFboundIter().iterator().next()", "List<?>");
        assertType("supFboundIter().iterator().next().get(0)", "Object");
        assertType("listOf(23)", "List<Integer>");
        assertType("listOf(true)", "List<Boolean>");
        assertType("listOf(true).get(0)", "Boolean");
        assertType("arrayOf(99)", "Integer[]");
        assertType("arrayOf(99)[0]", "Integer");

        assertEval("<Z> Z choose(Z z1, Z z2) { return z1; }");
        assertType("choose(1, 1L);",
                  "Number&Comparable<? extends Number&Comparable<?>&java.lang.constant.Constable&java.lang.constant.ConstantDesc>&java.lang.constant.Constable&java.lang.constant.ConstantDesc",
                "Number");
    }

    public void testVariableTypeName() {
        assertType("\"x\"", "String");

        assertType("java.util.regex.Pattern.compile(\"x\")", "java.util.regex.Pattern");
        assertEval("import java.util.regex.*;");
        assertType("java.util.regex.Pattern.compile(\"x\")", "Pattern");

        assertType("new java.util.ArrayList()", "java.util.ArrayList");
        assertEval("import java.util.ArrayList;");
        assertType("new java.util.ArrayList()", "ArrayList");

        assertType("java.util.Locale.Category.FORMAT", "java.util.Locale.Category");
        assertEval("import static java.util.Locale.Category;");
        assertType("java.util.Locale.Category.FORMAT", "Category");
    }

    public void testReplNestedClassName() {
        assertEval("class D { static class E {} }");
        assertType("new D.E();", "D.E");
    }

    public void testAnonymousClassName() {
        assertEval("class C {}");
        assertType("new C();", "C");
        assertType("new C() { int x; };", "<anonymous class extending C>", "C");
    }

    public void testCapturedTypeName() {
        assertType("\"\".getClass();", "Class<? extends String>");
        assertType("\"\".getClass().getEnumConstants();", "String[]");
    }

    public void testJavaLang() {
        assertType("\"\";", "String");
    }

    public void testNotOverEagerPackageEating() {
        assertType("\"\".getClass().getDeclaredMethod(\"hashCode\");", "java.lang.reflect.Method");
    }

    public void testBounds() {
        assertEval("java.util.List<? extends String> list1 = java.util.Arrays.asList(\"\");");
        assertType("list1.iterator().next()", "String");
        assertEval("java.util.List<? super String> list2 = java.util.Arrays.asList(\"\");");
        assertType("list2.iterator().next()", "Object");
        assertEval("java.util.List<?> list3 = java.util.Arrays.asList(\"\");");
        assertType("list3.iterator().next()", "Object");
        assertEval("class Test1<X extends CharSequence> { public X get() { return null; } }");
        assertEval("Test1<?> test1 = new Test1<>();");
        assertType("test1.get()", "CharSequence");
        assertEval("class Test2<X extends Number & CharSequence> { public X get() { return null; } }");
        assertEval("Test2<?> test2 = new Test2<>();");
        assertType("test2.get()", "Number&CharSequence", "Number");
        assertEval("class Test3<T> { T[][] get() { return null; } }");
        assertEval("Test3<? extends String> test3 = new Test3<>();");
        assertType("test3.get()", "String[][]");
    }
}
