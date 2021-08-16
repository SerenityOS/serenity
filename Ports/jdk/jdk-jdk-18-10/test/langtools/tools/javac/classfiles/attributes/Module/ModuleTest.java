/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Module attribute tests
 * @bug 8080878 8161906 8162713 8170250
 * @modules java.compiler
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.classfile
 * @library /tools/lib ../lib /tools/javac/lib
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.ToolBox
 *      TestBase TestResult ModuleTestBase
 * @run main ModuleTest
 */

import java.nio.file.Path;

public class ModuleTest extends ModuleTestBase {

    public static void main(String[] args) throws Exception {
        new ModuleTest().run();
    }

    @Test
    public void testEmptyModule(Path base) throws Exception {
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("m")
                .write(base);
        compile(base);
        testModuleAttribute(base, moduleDescriptor);
    }

    @Test
    public void testOpenEmptyModule(Path base) throws Exception {
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("m", ModuleFlag.OPEN)
                .write(base);
        compile(base);
        testModuleAttribute(base, moduleDescriptor);
    }

    @Test
    public void testModuleName(Path base) throws Exception {
        testName("module.name", base.resolve("dot"));
        testName("module.exports.component.subcomponent.more.dots", base.resolve("dots"));
        testName("moduleName", base.resolve("noDots"));
    }

    private void testName(String name, Path path) throws Exception{
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor(name)
                .write(path);
        compile(path);
        testModuleAttribute(path, moduleDescriptor);
    }

    @Test
    public void testExports(Path base) throws Exception {
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("m")
                .exports("pack")
                .write(base);
        tb.writeJavaFiles(base, "package pack; public class C extends java.util.ArrayList{ }");
        compile(base);
        testModuleAttribute(base, moduleDescriptor);
    }

    @Test
    public void testSeveralExports(Path base) throws Exception {
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("m")
                .exports("pack")
                .exports("pack2")
                .exports("pack3")
                .exports("pack4")
                .exports("pack5")
                .write(base);
        tb.writeJavaFiles(base,
                "package pack; public class A { }",
                "package pack2; public class B { }",
                "package pack3; public class C { }",
                "package pack4; public class C { }",
                "package pack5; public class C { }");
        compile(base);
        testModuleAttribute(base, moduleDescriptor);
    }

    @Test
    public void testQualifiedExports(Path base) throws Exception {
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("m")
                .exportsTo("pack", "jdk.compiler")
                .write(base);
        tb.writeJavaFiles(base, "package pack; public class A { }");
        compile(base);
        testModuleAttribute(base, moduleDescriptor);
    }

    @Test
    public void testSeveralQualifiedExports(Path base) throws Exception {
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("m")
                .exportsTo("pack", "jdk.compiler, jdk.jdeps")
                .exportsTo("pack2", "jdk.jdeps")
                .exportsTo("pack3", "jdk.compiler")
                .exportsTo("pack4", "jdk.compiler, jdk.jdeps")
                .exportsTo("pack5", "jdk.compiler")
                .write(base);
        tb.writeJavaFiles(base,
                "package pack; public class A {}",
                "package pack2; public class B {}",
                "package pack3; public class C {}",
                "package pack4; public class C {}",
                "package pack5; public class C {}");
        compile(base);
        testModuleAttribute(base, moduleDescriptor);
    }

    @Test
    public void testOpens(Path base) throws Exception {
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("module.name")
                .opens("pack")
                .write(base);
        tb.writeJavaFiles(base, "package pack; public class C extends java.util.ArrayList{ }");
        compile(base);
        testModuleAttribute(base, moduleDescriptor);
    }

    @Test
    public void testQualifiedOpens(Path base) throws Exception {
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("m")
                .opensTo("pack", "jdk.compiler")
                .write(base);
        tb.writeJavaFiles(base, "package pack; public class A { }");
        compile(base);
        testModuleAttribute(base, moduleDescriptor);
    }

    @Test
    public void testSeveralOpens(Path base) throws Exception {
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("module.m1.name")
                .opensTo("pack", "jdk.compiler, jdk.jdeps")
                .opensTo("pack2", "jdk.jdeps")
                .opensTo("pack3", "jdk.compiler")
                .opensTo("pack4", "jdk.compiler, jdk.jdeps")
                .opensTo("pack5", "jdk.compiler")
                .opens("pack6")
                .write(base);
        tb.writeJavaFiles(base,
                "package pack; public class A {}",
                "package pack2; public class B {}",
                "package pack3; public class C {}",
                "package pack4; public class C {}",
                "package pack5; public class C {}",
                "package pack6; public class C {}");
        compile(base);
        testModuleAttribute(base, moduleDescriptor);
    }

    @Test
    public void testRequires(Path base) throws Exception {
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("m")
                .requires("jdk.compiler")
                .write(base);
        compile(base);
        testModuleAttribute(base, moduleDescriptor);
    }

    @Test
    public void testRequiresTransitive(Path base) throws Exception {
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("m")
                .requires("jdk.jdeps", RequiresFlag.TRANSITIVE)
                .write(base);
        compile(base);
        testModuleAttribute(base, moduleDescriptor);
    }

    @Test
    public void testRequiresStatic(Path base) throws Exception {
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("m")
                .requires("jdk.jdeps", RequiresFlag.STATIC)
                .write(base);
        compile(base);
        testModuleAttribute(base, moduleDescriptor);
    }

    @Test
    public void testSeveralRequires(Path base) throws Exception {
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("m1x")
                .requires("jdk.jdeps", RequiresFlag.TRANSITIVE)
                .requires("jdk.compiler")
                .requires("m2x", RequiresFlag.STATIC)
                .requires("m3x")
                .requires("m4x", RequiresFlag.TRANSITIVE)
                .requires("m5x", RequiresFlag.STATIC, RequiresFlag.TRANSITIVE)
                .write(base.resolve("m1x"));
        tb.writeJavaFiles(base.resolve("m2x"), "module m2x { }");
        tb.writeJavaFiles(base.resolve("m3x"), "module m3x { }");
        tb.writeJavaFiles(base.resolve("m4x"), "module m4x { }");
        tb.writeJavaFiles(base.resolve("m5x"), "module m5x { }");
        compile(base, "--module-source-path", base.toString(),
                "-d", base.toString());
        testModuleAttribute(base.resolve("m1x"), moduleDescriptor);
    }

    @Test
    public void testProvides(Path base) throws Exception {
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("m")
                .provides("java.util.Collection", "pack2.D")
                .write(base);
        tb.writeJavaFiles(base, "package pack2; public class D extends java.util.ArrayList{ }");
        compile(base);
        testModuleAttribute(base, moduleDescriptor);
    }

    @Test
    public void testSeveralProvides(Path base) throws Exception {
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("m")
                .provides("java.util.Collection", "pack2.D")
                .provides("java.util.List", "pack2.D")
                .requires("jdk.compiler")
                .provides("javax.tools.FileObject", "pack2.E")
                .provides("com.sun.tools.javac.Main", "pack2.C")
                .write(base);
        tb.writeJavaFiles(base, "package pack2; public class D extends java.util.ArrayList{ }",
                "package pack2; public class C extends com.sun.tools.javac.Main{ }",
                "package pack2; public class E extends javax.tools.SimpleJavaFileObject{ public E(){ super(null,null); } }");
        compile(base);
        testModuleAttribute(base, moduleDescriptor);
    }

    @Test
    public void testUses(Path base) throws Exception {
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("m")
                .uses("java.util.List")
                .write(base);
        compile(base);
        testModuleAttribute(base, moduleDescriptor);
    }

    @Test
    public void testSeveralUses(Path base) throws Exception {
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("m")
                .uses("java.util.List")
                .uses("java.util.Collection") // from java.base
                .requires("jdk.compiler")
                .uses("javax.tools.JavaCompiler") // from java.compiler
                .uses("com.sun.tools.javac.Main") // from jdk.compiler
                .write(base);
        compile(base);
        testModuleAttribute(base, moduleDescriptor);
    }

    @Test
    public void testComplex(Path base) throws Exception {
        Path m1 = base.resolve("m1x");
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("m1x")
                .exports("pack1")
                .opens("pack3")
                .exportsTo("packTo1", "m2x")
                .opensTo("packTo1", "m2x") // the same as exportsTo
                .opensTo("packTo3", "m3x")
                .requires("jdk.compiler")
                .requires("m2x", RequiresFlag.TRANSITIVE)
                .requires("m3x", RequiresFlag.STATIC)
                .requires("m4x", RequiresFlag.TRANSITIVE, RequiresFlag.STATIC)
                .provides("java.util.List", "pack1.C", "pack2.D")
                .uses("java.util.List")
                .uses("java.nio.file.Path")
                .requires("jdk.jdeps", RequiresFlag.STATIC, RequiresFlag.TRANSITIVE)
                .requires("m5x", RequiresFlag.STATIC)
                .requires("m6x", RequiresFlag.TRANSITIVE)
                .requires("java.compiler")
                .opensTo("packTo4", "java.compiler")
                .exportsTo("packTo2", "java.compiler")
                .opens("pack2") // same as exports
                .opens("pack4")
                .exports("pack2")
                .write(m1);
        tb.writeJavaFiles(m1, "package pack1; public class C extends java.util.ArrayList{ }",
                "package pack2; public class D extends java.util.ArrayList{ }",
                "package pack3; public class D extends java.util.ArrayList{ }",
                "package pack4; public class D extends java.util.ArrayList{ }");
        tb.writeJavaFiles(m1,
                "package packTo1; public class T1 {}",
                "package packTo2; public class T2 {}",
                "package packTo3; public class T3 {}",
                "package packTo4; public class T4 {}");
        tb.writeJavaFiles(base.resolve("m2x"), "module m2x { }");
        tb.writeJavaFiles(base.resolve("m3x"), "module m3x { }");
        tb.writeJavaFiles(base.resolve("m4x"), "module m4x { }");
        tb.writeJavaFiles(base.resolve("m5x"), "module m5x { }");
        tb.writeJavaFiles(base.resolve("m6x"), "module m6x { }");
        compile(base, "--module-source-path", base.toString(),
                "-d", base.toString());
        testModuleAttribute(m1, moduleDescriptor);
    }

    @Test
    public void testOpenComplexModule(Path base) throws Exception {
        Path m1 = base.resolve("m1x");
        ModuleDescriptor moduleDescriptor = new ModuleDescriptor("m1x", ModuleFlag.OPEN)
                .exports("pack1")
                .exportsTo("packTo1", "m2x")
                .requires("jdk.compiler")
                .requires("m2x", RequiresFlag.TRANSITIVE)
                .requires("m3x", RequiresFlag.STATIC)
                .requires("m4x", RequiresFlag.TRANSITIVE, RequiresFlag.STATIC)
                .provides("java.util.List", "pack1.C", "pack2.D")
                .uses("java.util.List")
                .uses("java.nio.file.Path")
                .requires("jdk.jdeps", RequiresFlag.STATIC, RequiresFlag.TRANSITIVE)
                .requires("m5x", RequiresFlag.STATIC)
                .requires("m6x", RequiresFlag.TRANSITIVE)
                .requires("java.compiler")
                .exportsTo("packTo2", "java.compiler")
                .exports("pack2")
                .write(m1);
        tb.writeJavaFiles(m1, "package pack1; public class C extends java.util.ArrayList{ }",
                "package pack2; public class D extends java.util.ArrayList{ }",
                "package pack3; public class D extends java.util.ArrayList{ }",
                "package pack4; public class D extends java.util.ArrayList{ }");
        tb.writeJavaFiles(m1,
                "package packTo1; public class T1 {}",
                "package packTo2; public class T2 {}",
                "package packTo3; public class T3 {}",
                "package packTo4; public class T4 {}");
        tb.writeJavaFiles(base.resolve("m2x"), "module m2x { }");
        tb.writeJavaFiles(base.resolve("m3x"), "module m3x { }");
        tb.writeJavaFiles(base.resolve("m4x"), "module m4x { }");
        tb.writeJavaFiles(base.resolve("m5x"), "module m5x { }");
        tb.writeJavaFiles(base.resolve("m6x"), "module m6x { }");
        compile(base, "--module-source-path", base.toString(),
                "-d", base.toString());
        testModuleAttribute(m1, moduleDescriptor);
    }
}
