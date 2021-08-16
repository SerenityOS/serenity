/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8025091 8198890
 * @summary Verify the presence visible members in the case of
 *          member hiding and overridding.
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.* toolbox.ToolBox builder.ClassBuilder
 * @run main TestVisibleMembers
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import builder.AbstractBuilder;
import builder.AbstractBuilder.Comment.Kind;
import builder.ClassBuilder;
import builder.ClassBuilder.*;

import toolbox.ToolBox;
import builder.ClassBuilder;

import javadoc.tester.JavadocTester;

public class TestVisibleMembers extends JavadocTester {

    final ToolBox tb;
    public static void main(String... args) throws Exception {
        TestVisibleMembers tester = new TestVisibleMembers();
        tester.runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    TestVisibleMembers() {
        tb = new ToolBox();
    }

    @Test
    public void testChronoDiamondLeafDetail(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        emitChronoDiamondLeaf(srcDir);

        Path outDir = base.resolve("out");
        javadoc("-d", outDir.toString(),
                "-html5",
                "--override-methods=detail",
                "-sourcepath", srcDir.toString(),
                "p");
        checkExit(Exit.OK);

        checkOrder("p/C.html", "METHOD SUMMARY",
                "boolean", "equals", "java.lang.Object", "Method equals in p.C",
                "C", "with", "java.lang.Object", "obj",  "Method with in p.C",
                "C", "with", "java.lang.Object", "obj", "long", "lvalue", "Method with in p.C",
                "METHOD DETAIL");
        checkOutput("p/C.html", false, "BImpl");

        checkOrder("p/E.html", "METHOD SUMMARY",
                "boolean", "equals", "java.lang.Object", "Method equals in p.E",
                "C", "with", "java.lang.Object", "Method with in p.E",
                "C", "with", "java.lang.Object", "obj", "long", "lvalue", "Method with in p.E",
                "METHOD DETAIL");
        checkOutput("p/E.html", false, "EImpl");
    }

    @Test
    public void testChronoDiamondLeafSummary(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        emitChronoDiamondLeaf(srcDir);

        Path outDir = base.resolve("out-member");
        javadoc("-d", outDir.toString(),
                "-html5",
                "--override-methods=summary",
                "-sourcepath", srcDir.toString(),
                "p");
        checkExit(Exit.OK);

        checkOrder("p/C.html", "METHOD SUMMARY",
                "boolean", "equals", "java.lang.Object", "Method equals in p.C",
                "C", "with", "java.lang.Object", "obj", "Method with in p.C",
                "C", "with", "java.lang.Object", "obj", "long", "lvalue", "Method with in p.C",
                "METHOD DETAIL");
        checkOutput("p/C.html", false, "BImpl");

        checkOrder("p/E.html", "METHOD SUMMARY",
                "boolean", "equals", "java.lang.Object", "Method equals in p.E",
                "C", "with", "java.lang.Object", "Method with in p.E",
                "C", "with", "java.lang.Object", "long", "lvalue", "Method with in p.E",
                "METHOD DETAIL");
        checkOutput("p/E.html", false, "EImpl");
    }

    // see j.t.TemporalAdjuster
    void emitChronoDiamondLeaf(Path srcDir) throws Exception {

        // Interface A
        MethodBuilder mbWith1 = MethodBuilder
                .parse("default Object with(Object obj) {return null;}");
        MethodBuilder mbWith2 = MethodBuilder
                .parse("default Object with(Object obj, long lvalue) {return null;}");

        new ClassBuilder(tb, "p.A")
                .setModifiers("public", "interface")
                .addMembers(mbWith1, mbWith2)
                .write(srcDir);

        // Interface B
        mbWith1.setComments("{@inheritDoc}", "@param obj an object",
                "@return something");

        mbWith2.setComments("{@inheritDoc}", "@param obj an object",
                "@param lvalue an lvalue", "@return something");

        new ClassBuilder(tb, "p.B")
                .setModifiers( "public", "interface")
                .setExtends("A")
                .addMembers(mbWith1, mbWith2)
                .write(srcDir);

        // Class BImpl
        MethodBuilder mb31 = MethodBuilder.parse("C with(Object obj) {return null;}");
        MethodBuilder mb32 = MethodBuilder.parse("C with(Object obj, Long lobj) {return null;}");
        new ClassBuilder(tb, "p.BImpl<C extends B>")
                .setModifiers( "abstract", "class")
                .addImplements("B")
                .addMembers(mb31, mb32)
                .write(srcDir);

        // Class C
        new ClassBuilder(tb, "p.C")
                .setModifiers("public", "class")
                .setExtends("BImpl")
                .addMembers(mbWith1.setReturn("C")
                        .setModifiers("public")
                        .setComments(AbstractBuilder.Comment.Kind.AUTO))
                .addMembers(mbWith2.setReturn("C")
                        .setModifiers("public")
                        .setComments(AbstractBuilder.Comment.Kind.AUTO))
                .addMembers(MethodBuilder.parse("public boolean equals(Object obj) { return false;}"))
                .write(srcDir);

        // Class EImpl
        MethodBuilder mb41 = MethodBuilder.parse("C with(Object obj) {return null;}")
                .setComments(Kind.NO_API_COMMENT);
        MethodBuilder mb42 = MethodBuilder.parse("C with(Object obj, Long lobj) {return null;}");
        new ClassBuilder(tb, "p.EImpl<C extends B>")
                .setModifiers( "abstract", "class")
                .addImplements("B")
                .addMembers(mb41, mb42)
                .write(srcDir);

        // Class E
        MethodBuilder mb51 = MethodBuilder.parse("public C with(Object obj) {return null;}");
        MethodBuilder mb52 = MethodBuilder.parse("public C with(Object obj, long lvalue) {return null;}");
        MethodBuilder mb53 = MethodBuilder.parse("public boolean equals(Object obj) { return false;}");
        new ClassBuilder(tb, "p.E")
                .setModifiers("public", "class")
                .setExtends("EImpl")
                .addMembers(mb51, mb52, mb53)
                .write(srcDir);
    }

    @Test
    public void testNestedInterfaceDetail(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        emitNestedInterface(srcDir);

        Path outDir = base.resolve("out");
        javadoc("-d", outDir.toString(),
                "-html5",
                "--override-methods=detail",
                "-sourcepath", srcDir.toString(),
                "p");
        checkExit(Exit.OK);

        checkOutput("p/TA.html", false, "getTA");

        checkOrder("p/Bar.html",
                "doSomething()",
                "getTA()");
    }

    @Test
    public void testNestedInterfaceSummary(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        emitNestedInterface(srcDir);

        Path outDir = base.resolve("out");
        javadoc("-d", outDir.toString(),
                "-html5",
                "--override-methods=summary",
                "-sourcepath", srcDir.toString(),
                "p");
        checkExit(Exit.OK);

        checkOutput("p/TA.html", false, "getTA");

        checkOrder("p/Bar.html",
                "doSomething()",
                "getTA()");

        checkOrder("p/Foo.html",
                "Methods declared in",
                "Bar.html",
                "getTA");
    }

    // See jx.s.TransferHandler
    void emitNestedInterface(Path srcDir) throws Exception {

        ClassBuilder innerI = new ClassBuilder(tb, "HasTA")
                .setModifiers("interface");
        MethodBuilder interfaceMethod = MethodBuilder.parse("public TA getTa();")
                .setComments(Kind.NO_API_COMMENT);
        innerI.addMembers(interfaceMethod);

        new ClassBuilder(tb, "p.TA")
                .setModifiers("public", "class")
                .addImplements("java.io.Serializable")
                .addNestedClasses(innerI)
                .write(srcDir);

        new ClassBuilder(tb, "p.Foo")
                .setModifiers("public", "class")
                .setExtends("Bar")
                .write(srcDir);

        new ClassBuilder(tb, "p.Bar")
                .setModifiers("public", "abstract", "class")
                .addImplements("TA.HasTA")
                .addMembers(
                    MethodBuilder.parse("public void doSomething(){}"),
                    MethodBuilder.parse("public TA getTA(){return null;}")
                ).write(srcDir);
    }

    @Test
    public void testStreamsMissingLinksDetail(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        emitStreamsMissingLinks(srcDir);

        Path outDir = base.resolve("out");
        javadoc("-d", outDir.toString(),
                "-html5",
                "--override-methods=detail",
                "--no-platform-links",
                "-sourcepath", srcDir.toString(),
                "p");
        checkExit(Exit.OK);

        checkOrder("p/C.html",
                "METHOD DETAIL",
                "public", "void", "method",
                "See Also:",
                "sub()",
                "sub1()");

        checkOrder("p/ILong.html",
                "METHOD DETAIL",
                "default", "void", "forEach", "java.util.function.Consumer",
                "java.lang.Long", "action",
                "Do you see me", "#forEach(java.util.function.LongConsumer)",
                "forEach(LongConsumer)",
                "END OF CLASS DATA");

        checkOrder("p/IImpl.html",
                "METHOD DETAIL",
                "Method sub in p.IImpl",
                "Specified by:", "I.html", "II.html",
                "END OF CLASS DATA");
    }

    @Test
    public void testStreamsMissingLinksSummary(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        emitStreamsMissingLinks(srcDir);

        Path outDir = base.resolve("out");
        javadoc("-d", outDir.toString(),
                "-html5",
                "--override-methods=summary",
                "--no-platform-links",
                "-sourcepath", srcDir.toString(),
                "p");
        checkExit(Exit.OK);

        checkOrder("p/C.html",
                "METHOD DETAIL",
                "public", "void", "method", "See Also:", "sub()", "sub1()",
                "public", "void", "m", "Method in C. See", "I.length()"
                );

        checkOrder("p/ILong.html",
                "METHOD DETAIL",
                "default", "void", "forEach", "java.util.function.Consumer",
                "java.lang.Long", "action",
                "Do you see me", "QLong.html#forEach(Q)",
                "QLong.forEach(LongConsumer)",
                "END OF CLASS DATA");

        checkOrder("p/IImpl.html",
                "METHOD DETAIL",
                "Method sub in p.IImpl",
                "Specified by:", "I.html",
                "Specified by:", "II.html",
                "END OF CLASS DATA");
    }

    // see j.u.Spliterator
    void emitStreamsMissingLinks(Path srcDir) throws Exception {
        new ClassBuilder(tb, "p.I")
                .setModifiers("public", "interface")
                .addMembers(
                        MethodBuilder.parse("public I sub();"),
                        MethodBuilder.parse("public I sub1();"),
                        MethodBuilder.parse("public int length();")
                ).write(srcDir);

        new ClassBuilder(tb, "p.A")
                .setModifiers("abstract", "class")
                .addImplements("I")
                .addMembers(
                        MethodBuilder.parse("public I sub() {}"),
                        MethodBuilder.parse("public I sub1() {}"),
                        MethodBuilder.parse("public int length(){return 0;}")
                                .setComments(Kind.NO_API_COMMENT),
                        MethodBuilder.parse("public void m(){}")
                        .setComments("Method in C. See {@link #length()}.")
                ).write(srcDir);

        new ClassBuilder(tb, "p.C")
                .setModifiers("public", "class")
                .setExtends("A").addImplements("I")
                .addMembers(
                        MethodBuilder.parse("public I sub() {return null;}"),
                        MethodBuilder.parse("public I sub1() {return null;}")
                                .setComments(Kind.INHERIT_DOC),
                        MethodBuilder.parse(" public void method() {}")
                                .setComments("A method ", "@see #sub", "@see #sub1"),
                        MethodBuilder.parse("public int length(){return 1;}")
                                .setComments(Kind.NO_API_COMMENT)
                ).write(srcDir);

        new ClassBuilder(tb, "p.II")
                .setModifiers("public", "interface")
                .setExtends("I")
                .addMembers(
                        MethodBuilder.parse("default public I sub() {return null;}")
                            .setComments(Kind.NO_API_COMMENT)
                ).write(srcDir);

        new ClassBuilder(tb, "p.IImpl")
                .setModifiers("public", "class")
                .addImplements("II")
                .addMembers(
                    MethodBuilder.parse("public I sub() {return null;}")
                ).write(srcDir);

        new ClassBuilder(tb, "p.QLong<P, Q, R>")
                .setModifiers("public interface")
                .addMembers(
                        MethodBuilder.parse("default void forEach(Q action) {}")
                ).write(srcDir);

        new ClassBuilder(tb, "p.ILong")
                .addImports("java.util.function.*")
                .setModifiers("public", "interface")
                .setExtends("QLong<Long, LongConsumer, Object>")
                .addMembers(
                        MethodBuilder.parse("default void forEach(LongConsumer action) {}")
                            .setComments(Kind.NO_API_COMMENT),
                        MethodBuilder.parse("default void forEach(Consumer<Long> action) {}")
                            .setComments("Do you see me {@link #forEach(LongConsumer)} ?")
                ).write(srcDir);
    }

    @Test
    public void testVisibleMemberTableDetail(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        emitVisibleMemberTable(srcDir);

        Path outDir = base.resolve("out");
        javadoc("-d", outDir.toString(),
                "-html5",
                "--override-methods=detail",
                "-sourcepath", srcDir.toString(),
                "p");
        checkExit(Exit.OK);

        checkOrder("p/C.html",
                "METHOD DETAIL",
                "public", "void", "m", "Method m in p.B",
                "public", "void", "n", "Method n in p.A",
                "public", "void", "o", "Description copied from class:", ">A<", "Method o in p.A",
                "public", "void", "p", "Method p in p.B",
                "END OF CLASS DATA");

        checkOutput("p/C.html", false,
                "Overrides",
                "Methods declared in class p");

        checkOrder("p/D.html",
                "METHOD SUMMARY",
                "void", "m", "Method m in p.D",
                "void", "n", "Method n in p.D",
                "void", "o", "Method o in p.D",
                "void", "p", "Method p in p.D",
                "CONSTRUCTOR DETAIL");

        checkOutput("p/D.html", false,
                "Description copied from class:",
                "Overrides",
                "Methods declared in class p");

        checkOrder("p/E.html",
                "METHOD SUMMARY",
                "void", "m", "Method m in p.B",
                "void", "n", "Method n in p.A",
                "void", "o", "Method o in p.A",
                "void", "p", "Method p in p.B",
                "CONSTRUCTOR DETAIL");

        checkOutput("p/E.html", false,
                "Description copied from class:",
                "Overrides",
                "Methods declared in class p");
    }

    @Test
    public void testVisibleMemberTableSummary(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        emitVisibleMemberTable(srcDir);

        Path outDir = base.resolve("out");
        javadoc("-d", outDir.toString(),
                "-html5",
                "--override-methods=summary",
                "-sourcepath", srcDir.toString(),
                "p");
        checkExit(Exit.OK);

        checkOrder("p/C.html",
                "METHOD SUMMARY",
                "void", "m", "Method m in p.B",
                "void", "n", "Method n in p.A",
                "void", "o", "Method o in p.A",
                "void", "p", "Method p in p.B",
                "CONSTRUCTOR DETAIL");

        checkOrder("p/C.html",
                "METHOD DETAIL",
                "public", "void", "m", "Method m in p.B",
                "public", "void", "n", "Method n in p.A",
                "public", "void", "o", "Description copied from class:", ">A<", "Method o in p.A",
                "public", "void", "p", "Method p in p.B",
                "END OF CLASS DATA");

        checkOutput("p/C.html", false,
                "Overrides",
                "Methods declared in class p");

        checkOrder("p/D.html",
                "METHOD SUMMARY",
                "void", "m", "Method m in p.D",
                "void", "n", "Method n in p.D",
                "void", "o", "Method o in p.D",
                "void", "p", "Method p in p.D",
                "CONSTRUCTOR DETAIL");

        checkOutput("p/D.html", false,
                "Description copied from class:",
                "Overrides",
                "Methods declared in class p");

        checkOrder("p/E.html",
                "METHOD SUMMARY",
                "void", "m", "Method m in p.B",
                "void", "n", "Method n in p.A",
                "void", "o", "Method o in p.A",
                "void", "p", "Method p in p.B",
                "CONSTRUCTOR DETAIL");

        checkOutput("p/E.html", false,
                "Description copied from class:",
                "Overrides",
                "Methods declared in class p");

    }

    // emit a matrix of method variants
    void emitVisibleMemberTable(Path srcDir) throws Exception {
        new ClassBuilder(tb, "p.A")
                .setModifiers("public", "class")
                .addMembers(
                        MethodBuilder.parse("public void m() {}"),
                        MethodBuilder.parse("public void n() {}"),
                        MethodBuilder.parse("public void o() {}")
                ).write(srcDir);

        new ClassBuilder(tb, "p.B")
                .setModifiers("class")
                .setExtends("A")
                .addMembers(
                        MethodBuilder.parse("public void m() {}"),
                        MethodBuilder.parse("public void n() {}")
                                .setComments(Kind.INHERIT_DOC),
                        MethodBuilder.parse("public void o() {}")
                        .setComments(Kind.NO_API_COMMENT),
                        MethodBuilder.parse("public void p() {}")
                ).write(srcDir);

        new ClassBuilder(tb, "p.C")
                .setModifiers("public", "class")
                .setExtends("B")
                .addMembers(
                        MethodBuilder.parse("public void m() {}")
                                .setComments(Kind.NO_API_COMMENT),
                        MethodBuilder.parse("public void n() {}")
                                .setComments(Kind.NO_API_COMMENT),
                        MethodBuilder.parse("public void o() {}")
                                .setComments(Kind.NO_API_COMMENT),
                        MethodBuilder.parse("public void p() {}")
                                .setComments(Kind.NO_API_COMMENT)
                ).write(srcDir);

        new ClassBuilder(tb, "p.D")
                .setModifiers("public", "class")
                .setExtends("B")
                .addMembers(
                        MethodBuilder.parse("public void m() {}"),
                        MethodBuilder.parse("public void n() {}"),
                        MethodBuilder.parse("public void o() {}"),
                        MethodBuilder.parse("public void p() {}")
                ).write(srcDir);

        new ClassBuilder(tb, "p.E")
                .setModifiers("public", "class")
                .setExtends("B")
                .addMembers(
                        MethodBuilder.parse("public void m() {}")
                                .setComments(Kind.INHERIT_DOC),
                        MethodBuilder.parse("public void n() {}")
                                .setComments(Kind.INHERIT_DOC),
                        MethodBuilder.parse("public void o() {}")
                                .setComments(Kind.INHERIT_DOC),
                        MethodBuilder.parse("public void p() {}")
                                .setComments(Kind.INHERIT_DOC)
                ).write(srcDir);
    }

    @Test
    public void testHiddenMembersDetail(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        emitHiddenMembers(srcDir);

        Path outDir = base.resolve("out");
        javadoc("-d", outDir.toString(),
                "-html5",
                "--override-methods=detail",
                "-sourcepath", srcDir.toString(),
                "p");
        checkExit(Exit.OK);

        checkOrder("p/C1.html",
                "FIELD SUMMARY",
                "Fields inherited from interface", "I1", "field2",
                "Fields inherited from interface", "I2", "field2",
                "Fields inherited from interface", "I3", "field",
                "METHOD SUMMARY",
                "Methods inherited from interface", "I1", "method2",
                "Methods inherited from interface", "I2", "method2",
                "Methods inherited from interface", "I3", "method",
                "CONSTRUCTOR DETAIL");

        checkOrder("p/C2.html",
                "FIELD SUMMARY",
                "int", "field", "Field field in p.C2",
                "Fields inherited from interface", "I1", "field2",
                "Fields inherited from interface", "I2", "field2",
                "METHOD SUMMARY",
                "void", "method", "Method method in p.C2",
                "void", "method2", "Method method2 in p.C2");

    }

    @Test
    public void testHiddenMembersSummary(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        emitHiddenMembers(srcDir);

        Path outDir = base.resolve("out");
        javadoc("-d", outDir.toString(),
                "-html5",
                "--override-methods=summary",
                "-sourcepath", srcDir.toString(),
                "p");
        checkExit(Exit.OK);

        checkOrder("p/C1.html",
                "Field Summary",
                "Fields declared in interface", "I1", "field2",
                "Fields declared in interface", "I2", "field2",
                "Fields declared in interface", "I3", "field",
                "Method Summary",
                "Methods declared in interface", "I1", "method2",
                "Methods declared in interface", "I2", "method2",
                "Methods declared in interface", "I3", "method",
                "Constructor Detail");

        checkOrder("p/C2.html",
                "Field Summary",
                "int", "field", "Field field in p.C2",
                "Fields declared in interface", "I1", "field2",
                "Fields declared in interface", "I2", "field2",
                "Method Summary",
                "void", "method", "Method method in p.C2",
                "void", "method2", "Method method2 in p.C2");

    }

    void emitHiddenMembers(Path srcDir) throws Exception {
        new ClassBuilder(tb, "p.I1")
                .setModifiers("public", "interface")
                .addMembers(
                        FieldBuilder.parse("public static int field = 3;"),
                        FieldBuilder.parse("public static int field2 = 3;"),
                        MethodBuilder.parse("public void method();"),
                        MethodBuilder.parse("public void method2();"),
                        MethodBuilder.parse("public static void staticMethod() {}")
                ).write(srcDir);

        new ClassBuilder(tb, "p.I2")
                .setModifiers("public", "interface")
                .addMembers(
                        FieldBuilder.parse("public static int field = 3;"),
                        FieldBuilder.parse("public static int field2 = 3;"),
                        MethodBuilder.parse("public void method();"),
                        MethodBuilder.parse("public void method2();"),
                        MethodBuilder.parse("public static void staticMethod() {}")
                ).write(srcDir);

        new ClassBuilder(tb, "p.I3")
                .setExtends("I1, I2")
                .setModifiers("public", "interface")
                .addMembers(
                        FieldBuilder.parse("public static int field = 3;"),
                        MethodBuilder.parse("public void method();"),
                        MethodBuilder.parse("public static void staticMethod() {}")
                ).write(srcDir);

        new ClassBuilder(tb, "p.C1")
                .setModifiers("public", "abstract", "class")
                .addImplements("I3")
                .write(srcDir);

        new ClassBuilder(tb, "p.C2")
                .setExtends("C1")
                .setModifiers("public", "abstract", "class")
                .addMembers(
                        FieldBuilder.parse("public int field;"),
                        MethodBuilder.parse("public void method(){}"),
                        MethodBuilder.parse("public void method2(){}")
                ).write(srcDir);
    }
}
