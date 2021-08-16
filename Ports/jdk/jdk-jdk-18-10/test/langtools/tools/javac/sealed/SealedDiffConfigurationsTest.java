/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test 8247352
 * @summary test different configurations of sealed classes, same compilation unit, diff pkg or mdl, etc
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main SealedDiffConfigurationsTest
 */

import java.util.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

import com.sun.tools.classfile.*;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.util.Assert;
import toolbox.TestRunner;
import toolbox.ToolBox;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.OutputKind;

import static com.sun.tools.classfile.ConstantPool.*;

public class SealedDiffConfigurationsTest extends TestRunner {
    ToolBox tb;

    SealedDiffConfigurationsTest() {
        super(System.err);
        tb = new ToolBox();
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    public static void main(String... args) throws Exception {
        SealedDiffConfigurationsTest t = new SealedDiffConfigurationsTest();
        t.runTests();
    }

    Path[] findJavaFiles(Path... paths) throws IOException {
        return tb.findJavaFiles(paths);
    }

    @Test
    public void testSameCompilationUnitPos(Path base) throws Exception {
        Path src = base.resolve("src");
        Path test = src.resolve("Test");

        tb.writeJavaFiles(test,
                          "class Test {\n" +
                           "    sealed class Sealed permits Sub1, Sub2 {}\n" +
                           "    final class Sub1 extends Sealed {}\n" +
                           "    final class Sub2 extends Sealed {}\n" +
                           "}");

        Path out = base.resolve("out");

        Files.createDirectories(out);

        new JavacTask(tb)
                .outdir(out)
                .files(findJavaFiles(test))
                .run()
                .writeAll();

        checkSealedClassFile(out, "Test$Sealed.class", List.of("Test$Sub1", "Test$Sub2"));
        checkSubtypeClassFile(out, "Test$Sub1.class", "Test$Sealed", true);
        checkSubtypeClassFile(out, "Test$Sub2.class", "Test$Sealed", true);
    }

    @Test
    public void testSameCompilationUnitPos2(Path base) throws Exception {
        Path src = base.resolve("src");
        Path test = src.resolve("Test");

        tb.writeJavaFiles(test,
                "class Test {\n" +
                        "    sealed class Sealed {}\n" +
                        "    final class Sub1 extends Sealed {}\n" +
                        "    final class Sub2 extends Sealed {}\n" +
                        "}");

        Path out = base.resolve("out");

        Files.createDirectories(out);

        new JavacTask(tb)
                .outdir(out)
                .files(findJavaFiles(test))
                .run()
                .writeAll();

        checkSealedClassFile(out, "Test$Sealed.class", List.of("Test$Sub1", "Test$Sub2"));
        checkSubtypeClassFile(out, "Test$Sub1.class", "Test$Sealed", true);
        checkSubtypeClassFile(out, "Test$Sub2.class", "Test$Sealed", true);
    }

    private void checkSealedClassFile(Path out, String cfName, List<String> expectedSubTypeNames) throws ConstantPoolException, Exception {
        ClassFile sealedCF = ClassFile.read(out.resolve(cfName));
        Assert.check((sealedCF.access_flags.flags & Flags.FINAL) == 0, String.format("class at file %s must not be final", cfName));
        PermittedSubclasses_attribute permittedSubclasses = (PermittedSubclasses_attribute)sealedCF.attributes.get("PermittedSubclasses");
        Assert.check(permittedSubclasses.subtypes.length == expectedSubTypeNames.size());
        List<String> subtypeNames = new ArrayList<>();
        IntStream.of(permittedSubclasses.subtypes).forEach(i -> {
            try {
                subtypeNames.add(((CONSTANT_Class_info)sealedCF.constant_pool.get(i)).getName());
            } catch (ConstantPoolException ex) {
            }
        });
        subtypeNames.sort((s1, s2) -> s1.compareTo(s2));
        for (int i = 0; i < expectedSubTypeNames.size(); i++) {
            Assert.check(expectedSubTypeNames.get(0).equals(subtypeNames.get(0)));
        }
    }

    private void checkSubtypeClassFile(Path out, String cfName, String superClassName, boolean shouldBeFinal) throws Exception {
        ClassFile subCF1 = ClassFile.read(out.resolve(cfName));
        if (shouldBeFinal) {
            Assert.check((subCF1.access_flags.flags & Flags.FINAL) != 0, String.format("class at file %s must be final", cfName));
        }
        Assert.checkNull((PermittedSubclasses_attribute)subCF1.attributes.get("PermittedSubclasses"));
        Assert.check(((CONSTANT_Class_info)subCF1.constant_pool.get(subCF1.super_class)).getName().equals(superClassName));
    }

    @Test
    public void testSamePackagePos(Path base) throws Exception {
        Path src = base.resolve("src");
        Path pkg = src.resolve("pkg");
        Path sealed = pkg.resolve("Sealed");
        Path sub1 = pkg.resolve("Sub1");
        Path sub2 = pkg.resolve("Sub2");

        tb.writeJavaFiles(sealed,
                          "package pkg;\n" +
                          "\n" +
                          "sealed class Sealed permits Sub1, Sub2 {\n" +
                          "}");
        tb.writeJavaFiles(sub1,
                          "package pkg;\n" +
                          "\n" +
                          "final class Sub1 extends Sealed {\n" +
                          "}");
        tb.writeJavaFiles(sub2,
                          "package pkg;\n" +
                          "\n" +
                          "final class Sub2 extends Sealed {\n" +
                          "}");

        Path out = base.resolve("out");

        Files.createDirectories(out);

        new JavacTask(tb)
                .outdir(out)
                .files(findJavaFiles(pkg))
                .run()
                .writeAll();

        checkSealedClassFile(out.resolve("pkg"), "Sealed.class", List.of("pkg/Sub1", "pkg/Sub1"));
        checkSubtypeClassFile(out.resolve("pkg"), "Sub1.class", "pkg/Sealed", true);
        checkSubtypeClassFile(out.resolve("pkg"), "Sub2.class", "pkg/Sealed", true);
    }

    @Test
    public void testSameCompilationUnitNeg(Path base) throws Exception {
        Path src = base.resolve("src");
        Path test = src.resolve("Test");

        tb.writeJavaFiles(test,
                          "class Test {\n" +
                           "    sealed class Sealed permits Sub1 {}\n" +
                           "    final class Sub1 extends Sealed {}\n" +
                           "    class Sub2 extends Sealed {}\n" +
                           "}");

        List<String> error = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(test))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        List<String> expected = List.of(
                "Test.java:4:5: compiler.err.cant.inherit.from.sealed: Test.Sealed",
                "1 error");
        if (!error.containsAll(expected)) {
            throw new AssertionError("Expected output not found. Expected: " + expected);
        }
    }

    @Test
    public void testSameCompilationUnitNeg2(Path base) throws Exception {
        Path src = base.resolve("src");
        Path test = src.resolve("Test");

        tb.writeJavaFiles(test,
                "class Test {\n" +
                        "    sealed class Sealed permits Sub1 {}\n" +
                        "    class Sub1 extends Sealed {}\n" +
                        "}");

        List<String> error = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(test))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        List<String> expected = List.of(
                "Test.java:3:5: compiler.err.non.sealed.sealed.or.final.expected",
                "1 error");
        if (!error.containsAll(expected)) {
            throw new AssertionError("Expected output not found. Expected: " + expected);
        }
    }

    @Test
    public void testSamePackageNeg(Path base) throws Exception {
        Path src = base.resolve("src");
        Path pkg = src.resolve("pkg");
        Path sealed = pkg.resolve("Sealed");
        Path sub1 = pkg.resolve("Sub1");
        Path sub2 = pkg.resolve("Sub2");

        tb.writeJavaFiles(sealed,
                          "package pkg;\n" +
                          "\n" +
                          "sealed class Sealed permits Sub1 {\n" +
                          "}");
        tb.writeJavaFiles(sub1,
                          "package pkg;\n" +
                          "\n" +
                          "final class Sub1 extends Sealed {\n" +
                          "}");
        tb.writeJavaFiles(sub2,
                          "package pkg;\n" +
                          "\n" +
                          "class Sub2 extends Sealed {\n" +
                          "}");

        List<String> error = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(pkg))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        List<String> expected = List.of(
                "Sub2.java:3:1: compiler.err.cant.inherit.from.sealed: pkg.Sealed",
                "1 error"
                );
        if (!error.containsAll(expected)) {
            throw new AssertionError("Expected output not found. Expected: " + expected);
        }
    }

    @Test
    public void testSamePackageNeg2(Path base) throws Exception {
        Path src = base.resolve("src");
        Path pkg = src.resolve("pkg");
        Path sealed = pkg.resolve("Sealed");
        Path sub1 = pkg.resolve("Sub1");

        tb.writeJavaFiles(sealed,
                "package pkg;\n" +
                        "\n" +
                        "final class Sealed {\n" +
                        "}");
        tb.writeJavaFiles(sub1,
                "package pkg;\n" +
                        "\n" +
                        "class Sub1 extends Sealed {\n" +
                        "}");

        List<String> error = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(pkg))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        List<String> expected = List.of(
                "Sub1.java:3:20: compiler.err.cant.inherit.from.final: pkg.Sealed",
                "1 error");
        if (!error.containsAll(expected)) {
            throw new AssertionError("Expected output not found. Expected: " + expected);
        }
    }

    @Test
    public void testSamePackageNeg3(Path base) throws Exception {
        Path src = base.resolve("src");
        Path pkg = src.resolve("pkg");
        Path sealed = pkg.resolve("Sealed");
        Path sub1 = pkg.resolve("Sub1");

        tb.writeJavaFiles(sealed,
                "package pkg;\n" +
                        "\n" +
                        "sealed class Sealed permits Sub1{\n" +
                        "}");
        tb.writeJavaFiles(sub1,
                "package pkg;\n" +
                        "\n" +
                        "class Sub1 extends Sealed {\n" +
                        "}");

        List<String> error = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(pkg))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        List<String> expected = List.of(
                "Sub1.java:3:1: compiler.err.non.sealed.sealed.or.final.expected",
                "1 error");
        if (!error.containsAll(expected)) {
            throw new AssertionError("Expected output not found. Expected: " + expected);
        }
    }

    @Test
    public void testDiffPackageNeg(Path base) throws Exception {
        Path src = base.resolve("src");
        Path pkg1 = src.resolve("pkg1");
        Path pkg2 = src.resolve("pkg2");
        Path sealed = pkg1.resolve("Sealed");
        Path sub1 = pkg2.resolve("Sub1");
        Path sub2 = pkg2.resolve("Sub2");

        tb.writeJavaFiles(sealed,
                "package pkg1;\n" +
                        "import pkg2.*;\n" +
                        "public sealed class Sealed permits pkg2.Sub1 {\n" +
                        "}");
        tb.writeJavaFiles(sub1,
                "package pkg2;\n" +
                        "import pkg1.*;\n" +
                        "public final class Sub1 extends pkg1.Sealed {\n" +
                        "}");

        List<String> error = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(pkg1, pkg2))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        List<String> expected = List.of(
                "Sealed.java:3:40: compiler.err.class.in.unnamed.module.cant.extend.sealed.in.diff.package: pkg1.Sealed",
                "1 error");
        if (!error.containsAll(expected)) {
            throw new AssertionError("Expected output not found. Expected: " + expected);
        }
    }

    @Test
    public void testDiffPackageNeg2(Path base) throws Exception {
        // test that the compiler rejects a subtype that is not accessible to the sealed class
        Path src = base.resolve("src");
        Path pkg1 = src.resolve("pkg1");
        Path pkg2 = src.resolve("pkg2");
        Path sealed = pkg1.resolve("Sealed");
        Path sub1 = pkg2.resolve("Sub1");
        Path sub2 = pkg2.resolve("Sub2");

        tb.writeJavaFiles(sealed,
                "package pkg1;\n" +
                        "import pkg2.*;\n" +
                        "public sealed class Sealed permits pkg2.Sub1 {\n" +
                        "}");
        tb.writeJavaFiles(sub1,
                "package pkg2;\n" +
                        "import pkg1.*;\n" +
                        "final class Sub1 extends pkg1.Sealed {\n" +
                        "}");

        List<String> error = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(pkg1, pkg2))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        List<String> expected = List.of(
                "Sealed.java:3:40: compiler.err.not.def.public.cant.access: pkg2.Sub1, pkg2",
                "Sub1.java:3:7: compiler.err.cant.inherit.from.sealed: pkg1.Sealed",
                "2 errors");
        if (!error.containsAll(expected)) {
            throw new AssertionError("Expected output not found. Expected: " + expected);
        }
    }

    @Test
    public void testDiffPackageNeg3(Path base) throws Exception {
        Path src = base.resolve("src");
        Path pkg1 = src.resolve("pkg1");
        Path pkg2 = src.resolve("pkg2");
        Path sealed = pkg1.resolve("Sealed");
        Path sub1 = pkg2.resolve("Sub1");
        Path sub2 = pkg2.resolve("Sub2");

        tb.writeJavaFiles(sealed,
                "package pkg1;\n" +
                        "import pkg2.*;\n" +
                        "public sealed class Sealed permits pkg2.Sub1 {\n" +
                        "}");
        tb.writeJavaFiles(sub1,
                "package pkg2;\n" +
                        "import pkg1.*;\n" +
                        "public final class Sub1 extends pkg1.Sealed {\n" +
                        "}");

        Path out = base.resolve("out");

        Files.createDirectories(out);

        List<String> error = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(pkg1, pkg2))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        List<String> expected = List.of(
                "Sealed.java:3:40: compiler.err.class.in.unnamed.module.cant.extend.sealed.in.diff.package: pkg1.Sealed",
                "1 error");
        if (!error.containsAll(expected)) {
            throw new AssertionError("Expected output not found. Found: " + error);
        }
    }

    @Test
    public void testSameModuleSamePkgPos(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("mSealed");
        tb.writeJavaFiles(src_m1,
                "module mSealed {}",
                "package pkg; public sealed class Sealed permits pkg.Sub{}",
                "package pkg; public final class Sub extends pkg.Sealed{}");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @Test
    public void testSameModuleDiffPkgPos(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("mSealed");
        tb.writeJavaFiles(src_m1,
                "module mSealed {}",
                "package pkg1; public sealed class Sealed permits pkg2.Sub{}",
                "package pkg2; public final class Sub extends pkg1.Sealed{}");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @Test
    public void testSameModuleSamePkgNeg1(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("mSealed");
        // subclass doesn't extend super class
        tb.writeJavaFiles(src_m1,
                "module mSealed {}",
                "package pkg; public sealed class Sealed permits pkg.Sub {}",
                "package pkg; public final class Sub {}");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> error = new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path",
                        src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        List<String> expected = List.of(
                "Sealed.java:1:52: compiler.err.invalid.permits.clause: (compiler.misc.doesnt.extend.sealed: pkg.Sub)",
                "1 error");
        if (!error.containsAll(expected)) {
            throw new AssertionError("Expected output not found. Found: " + error);
        }
    }

    @Test
    public void testSameModuleSamePkgNeg2(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("mSealed");
        // subclass doesn't extend super class
        tb.writeJavaFiles(src_m1,
                "module mSealed {}",
                "package pkg; public sealed interface Sealed permits pkg.Sub1, pkg.Sub2 {}",
                "package pkg; public sealed class Sub1 implements Sealed permits Sub2 {}",
                "package pkg; public final class Sub2 extends Sub1 {}");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> error = new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path",
                        src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        List<String> expected = List.of(
                "Sealed.java:1:66: compiler.err.invalid.permits.clause: (compiler.misc.doesnt.extend.sealed: pkg.Sub2)",
                "1 error");
        if (!error.containsAll(expected)) {
            throw new AssertionError("Expected output not found. Found: " + error);
        }
    }

    @Test
    public void testDifferentModuleNeg(Path base) throws Exception {
        // check that a subclass in one module can't extend a sealed class in another module
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("mSealed");
        tb.writeJavaFiles(src_m1,
                "module mSealed { exports a; }",
                "package a; public sealed class Base permits b.Impl {}"
        );

        Path src_m2 = src.resolve("mSub");
        tb.writeJavaFiles(src_m2,
                "module mSub { exports b; requires mSealed; }",
                "package b; public final class Impl extends a.Base {}"
        );

        Path classes = base.resolve("classes");
        tb.createDirectories(classes);


        List<String> error =
            new JavacTask(tb)
                .options("-XDrawDiagnostics",
                        "--module-source-path", src.toString(),
                        "--add-reads", "mSealed=mSub")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        List<String> expected = List.of(
            "Base.java:1:46: compiler.err.class.in.module.cant.extend.sealed.in.diff.module: a.Base, mSealed",
            "1 error"
        );
        if (!error.containsAll(expected)) {
            throw new AssertionError("Expected output not found. Found: " + error);
        }
    }

    @Test
    public void testSeparateCompilation(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m = src.resolve("m");
        tb.writeJavaFiles(src_m,
                "module m {}",
                "package pkg.a; public sealed interface Sealed permits pkg.b.Sub {}",
                "package pkg.b; public final class Sub implements pkg.a.Sealed {}");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path",
                        src.toString())
                .outdir(classes)
                .files(findJavaFiles(src_m))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path",
                        src.toString(), "-doe")
                .outdir(classes)
                .files(findJavaFiles(src_m.resolve("pkg").resolve("a")))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path",
                        src.toString(), "-doe")
                .outdir(classes)
                .files(findJavaFiles(src_m.resolve("pkg").resolve("b")))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        tb.cleanDirectory(classes);

        //implicit compilations:
        new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path",
                        src.toString(), "-doe")
                .outdir(classes)
                .files(findJavaFiles(src_m.resolve("pkg").resolve("a")))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        tb.cleanDirectory(classes);

        new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path",
                        src.toString(), "-doe")
                .outdir(classes)
                .files(findJavaFiles(src_m.resolve("pkg").resolve("b")))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);
    }
}
