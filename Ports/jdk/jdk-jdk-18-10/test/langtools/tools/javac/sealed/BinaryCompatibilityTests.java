/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test binary compatibility rules for sealed classes
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main BinaryCompatibilityTests
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
import toolbox.JavaTask;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.OutputKind;

import static com.sun.tools.classfile.ConstantPool.*;

public class BinaryCompatibilityTests extends TestRunner {
    ToolBox tb;

    BinaryCompatibilityTests() {
        super(System.err);
        tb = new ToolBox();
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[]{Paths.get(m.getName())});
    }

    public static void main(String... args) throws Exception {
        BinaryCompatibilityTests t = new BinaryCompatibilityTests();
        t.runTests();
    }

    Path[] findJavaFiles(Path... paths) throws IOException {
        return tb.findJavaFiles(paths);
    }

    @Test
    public void testCompatibilityAfterMakingSuperclassSealed(Path base) throws Exception {
        /* If a class that was not declared sealed is changed to be declared sealed, then an
         * IncompatibleClassChangeError is thrown if a binary of a pre-existing subclass of
         * this class is loaded that is not contained in its permits clause
         */
        testCompatibilityAfterModifyingSupertype(
                base,
                true,
                """
                package pkg;
                public class Super {
                    public static void main(String... args) {
                        pkg.Sub sub = new pkg.Sub();
                        System.out.println("done");
                    }
                }
                """,
                """
                package pkg;
                public sealed class Super permits Sub1 {
                    public static void main(String... args) {
                        pkg.Sub sub = new pkg.Sub();
                        System.out.println("done");
                    }
                }

                final class Sub1 extends Super {}
                """,
                """
                package pkg;
                class Sub extends Super {}
                """
        );
    }

    @Test
    public void testCompatibilityAfterMakingSuperInterfaceSealed(Path base) throws Exception {
        // test similar to testCompatibilityAfterMakingSuperclassSealed but with interfaces
        testCompatibilityAfterModifyingSupertype(
                base,
                true,
                """
                package pkg;
                public interface Super {
                    public static void main(String... args) {
                        pkg.Sub sub = new pkg.Sub();
                        System.out.println("done");
                    }
                }
                """,
                """
                package pkg;
                public sealed interface Super permits Sub1 {
                    public static void main(String... args) {
                        pkg.Sub sub = new pkg.Sub();
                        System.out.println("done");
                    }
                }

                final class Sub1 implements Super {}
                """,
                """
                package pkg;
                class Sub implements Super {}
                """
        );
    }

    /* 1- compiles the first version of the superclass source code along with the subclass source code
     * 2- executes the super class just to make sure that it works
     * 3- compiles the second version of the super class along with the class file of the subclass
     * 4- executes the super class and makes sure that the VM throws IncompatibleClassChangeError or not
     *    depending on the shouldFail argument
     */
    private void testCompatibilityAfterModifyingSupertype(
            Path base,
            boolean shouldFail,
            String superClassCode1,
            String superClassCode2,
            String... subClassesCode) throws Exception {
        Path src = base.resolve("src");
        Path pkg = src.resolve("pkg");
        Path superClass = pkg.resolve("Super");
        Path sub = pkg.resolve("Sub");

        tb.writeJavaFiles(superClass, superClassCode1);
        tb.writeJavaFiles(sub, subClassesCode);

        Path out = base.resolve("out");
        Files.createDirectories(out);

        new JavacTask(tb)
                .outdir(out)
                .files(findJavaFiles(pkg))
                .run();

        // let's execute to check that it's working
        String output = new JavaTask(tb)
                .classpath(out.toString())
                .classArgs("pkg.Super")
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.STDOUT);

        // let's first check that it runs wo issues
        if (!output.contains("done")) {
            throw new AssertionError("execution of Super didn't finish");
        }

        // now lets change the super class
        tb.writeJavaFiles(superClass, superClassCode2);

        new JavacTask(tb)
                .classpath(out)
                .outdir(out)
                .files(findJavaFiles(superClass))
                .run();

        if (shouldFail) {
            // let's now check that there is an IncompatibleClassChangeError
            output = new JavaTask(tb)
                    .classpath(out.toString())
                    .classArgs("pkg.Super")
                    .run(Task.Expect.FAIL)
                    .writeAll()
                    .getOutput(Task.OutputKind.STDERR);
            if (!output.startsWith("Exception in thread \"main\" java.lang.IncompatibleClassChangeError")) {
                throw new AssertionError("java.lang.IncompatibleClassChangeError expected");
            }
        } else {
            new JavaTask(tb)
                    .classpath(out.toString())
                    .classArgs("pkg.Super")
                    .run(Task.Expect.SUCCESS);
        }
    }

    @Test
    public void testRemoveSealedModifierToClass(Path base) throws Exception {
        /* Changing a class that is declared sealed to no longer be declared
         * sealed does not break compatibility with pre-existing binaries.
         */
        testCompatibilityAfterModifyingSupertype(
                base,
                false,
                """
                package pkg;
                public sealed class Super permits pkg.Sub {
                    public static void main(String... args) {
                        pkg.Sub sub = new pkg.Sub();
                        System.out.println("done");
                    }
                }
                """,
                """
                package pkg;
                public class Super {
                    public static void main(String... args) {
                        pkg.Sub sub = new pkg.Sub();
                        System.out.println("done");
                    }
                }
                """,
                """
                package pkg;
                final class Sub extends Super {}
                """
        );
    }

    @Test
    public void testRemoveSealedModifierToInterface(Path base) throws Exception {
        // same as testRemoveSealedModifierToClass but with an interface
        testCompatibilityAfterModifyingSupertype(
                base,
                false,
                """
                package pkg;
                public sealed interface Super permits pkg.Sub {
                    public static void main(String... args) {
                        pkg.Sub sub = new pkg.Sub();
                        System.out.println("done");
                    }
                }
                """,
                """
                package pkg;
                public interface Super {
                    public static void main(String... args) {
                        pkg.Sub sub = new pkg.Sub();
                        System.out.println("done");
                    }
                }
                """,
                """
                package pkg;
                final class Sub implements Super {}
                """
        );
    }

    @Test
    public void testAddNonSealedModifierToClass(Path base) throws Exception {
        /* Changing a class that is not declared non-sealed to be declared
         * non-sealed does not break compatibility with pre-existing binaries
         */
        testCompatibilityOKAfterSubclassChange(
                base,
                """
                package pkg;
                public sealed class Super permits pkg.Sub {
                    public static void main(String... args) {
                        pkg.Sub sub = new pkg.Sub();
                        System.out.println("done");
                    }
                }
                """,
                """
                package pkg;
                final class Sub extends Super {}
                """,
                """
                package pkg;
                non-sealed class Sub extends Super {}
                """
        );
    }

    @Test
    public void testAddNonSealedModifierToInterface(Path base) throws Exception {
        // same as `testAddNonSealedModifierToClass` but with interfaces
        testCompatibilityOKAfterSubclassChange(
                base,
                """
                package pkg;
                public sealed interface Super permits pkg.Sub {
                    public static void main(String... args) {
                        pkg.Sub sub = new pkg.Sub();
                        System.out.println("done");
                    }
                }
                """,
                """
                package pkg;
                final class Sub implements Super {}
                """,
                """
                package pkg;
                non-sealed class Sub implements Super {}
                """
        );
    }

    @Test
    public void testRemoveNonSealedModifier(Path base) throws Exception {
        /* Changing a class that is declared non-sealed to no longer be declared
         * non-sealed does not break compatibility with pre-existing binaries
         */
        testCompatibilityOKAfterSubclassChange(
                base,
                """
                package pkg;
                public sealed class Super permits pkg.Sub {
                    public static void main(String... args) {
                        pkg.Sub sub = new pkg.Sub();
                        System.out.println("done");
                    }
                }
                """,
                """
                package pkg;
                non-sealed class Sub extends Super {}
                """,
                """
                package pkg;
                final class Sub extends Super {}
                """
        );
    }

    @Test
    public void testRemoveNonSealedModifierFromInterface(Path base) throws Exception {
        // same as `testRemoveNonSealedModifier` but with interfaces
        testCompatibilityOKAfterSubclassChange(
                base,
                """
                package pkg;
                public sealed interface Super permits pkg.Sub {
                    public static void main(String... args) {
                        pkg.Sub sub = new pkg.Sub();
                        System.out.println("done");
                    }
                }
                """,
                """
                package pkg;
                non-sealed class Sub implements Super {}
                """,
                """
                package pkg;
                final class Sub implements Super {}
                """
        );
    }

    /* 1- compiles the the superclass source code along with the first version of the subclass source code
     * 2- executes the super class just to make sure that it works
     * 3- compiles the second version of the subclass along with the class file of the superclass
     * 4- executes the super class and makes sure that it executes successfully
     */
    private void testCompatibilityOKAfterSubclassChange(
            Path base,
            String superClassCode,
            String subClassCode1,
            String subClassCode2) throws Exception {
        Path src = base.resolve("src");
        Path pkg = src.resolve("pkg");
        Path superClass = pkg.resolve("Super");
        Path sub = pkg.resolve("Sub");

        // super class initially sealed
        tb.writeJavaFiles(superClass, superClassCode);

        tb.writeJavaFiles(sub, subClassCode1);

        Path out = base.resolve("out");

        Files.createDirectories(out);

        new JavacTask(tb)
                .outdir(out)
                .files(findJavaFiles(pkg))
                .run();

        // let's execute to check that it's working
        String output = new JavaTask(tb)
                .classpath(out.toString())
                .classArgs("pkg.Super")
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.STDOUT);

        // let's first check that it runs wo issues
        if (!output.contains("done")) {
            throw new AssertionError("execution of Super didn't finish");
        }

        // now lets remove the non-sealed modifier from class Sub
        tb.writeJavaFiles(sub, subClassCode2);

        new JavacTask(tb)
                .classpath(out)
                .outdir(out)
                .files(findJavaFiles(sub))
                .run();

        // should execute without issues
        output = new JavaTask(tb)
                .classpath(out.toString())
                .classArgs("pkg.Super")
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.STDOUT);
        // let's first check that it runs wo issues
        if (!output.contains("done")) {
            throw new AssertionError("execution of Super didn't finish");
        }
    }

    @Test
    public void testAfterChangingPermitsClause(Path base) throws Exception {
        /* If a class is removed from the set of permitted direct subclasses of
         * a sealed class then an IncompatibleClassChangeError is thrown if the
         * pre-existing binary of the removed class is loaded
         */
        testCompatibilityAfterModifyingSupertype(
                base,
                true,
                """
                package pkg;
                public sealed class Super permits pkg.Sub1, Sub2 {
                    public static void main(String... args) {
                        pkg.Sub1 sub = new pkg.Sub1();
                        System.out.println("done");
                    }
                }

                final class Sub2 extends Super {}
                """,
                """
                package pkg;
                public sealed class Super permits Sub2 {
                    public static void main(String... args) {
                        pkg.Sub1 sub = new pkg.Sub1();
                        System.out.println("done");
                    }
                }

                final class Sub2 extends Super {}
                """,
                """
                package pkg;
                final class Sub1 extends Super {}
                """
        );
    }

    @Test
    public void testAfterChangingPermitsClause2(Path base) throws Exception {
        /* If a class is removed from the set of permitted direct subclasses of
         * a sealed class then an IncompatibleClassChangeError is thrown if the
         * pre-existing binary of the removed class is loaded
         */
        testCompatibilityAfterModifyingSupertype(
                base,
                true,
                """
                package pkg;
                public sealed class Super permits pkg.Sub1, pkg.Sub2 {
                    public static void main(String... args) {
                        pkg.Sub1 sub1 = new pkg.Sub1();
                        pkg.Sub2 sub2 = new pkg.Sub2();
                        System.out.println("done");
                    }
                }
                """,
                """
                package pkg;
                public sealed class Super permits pkg.Sub1 {
                    public static void main(String... args) {
                        pkg.Sub1 sub1 = new pkg.Sub1();
                        pkg.Sub2 sub2 = new pkg.Sub2();
                        System.out.println("done");
                    }
                }
                """,
                """
                package pkg;
                final class Sub1 extends Super {}
                """,
                """
                package pkg;
                final class Sub2 extends Super {}
                """
        );
    }

    @Test
    public void testAfterChangingPermitsClause3(Path base) throws Exception {
        /* Changing the set of permitted direct subclasses of a sealed class will
         * not break compatibility with pre-existing binaries, provided that the
         * total set of permitted direct subclasses of the sealed class loses no
         * members
         */
        String superClassCode1 =
                """
                package pkg;
                public sealed class Super permits pkg.Sub1 {
                    public static void main(String... args) {
                        pkg.Sub1 sub1 = new pkg.Sub1();
                        System.out.println("done");
                    }
                }
                """;
        String subClass1Code =
                """
                package pkg;
                final class Sub1 extends Super {}
                """;

        String superClassCode2 =
                """
                package pkg;
                public sealed class Super permits pkg.Sub1, pkg.Sub2 {
                    public static void main(String... args) {
                        pkg.Sub1 sub1 = new pkg.Sub1();
                        pkg.Sub2 sub2 = new pkg.Sub2();
                        System.out.println("done");
                    }
                }
                """;
        String subClass2Code =
                """
                package pkg;
                final class Sub2 extends Super {}
                """;

        Path src = base.resolve("src");
        Path pkg = src.resolve("pkg");
        Path superClass = pkg.resolve("Super");
        Path sub1 = pkg.resolve("Sub1");

        tb.writeJavaFiles(superClass, superClassCode1);
        tb.writeJavaFiles(sub1, subClass1Code);

        Path out = base.resolve("out");
        Files.createDirectories(out);

        new JavacTask(tb)
                .outdir(out)
                .files(findJavaFiles(pkg))
                .run();

        // let's execute to check that it's working
        String output = new JavaTask(tb)
                .classpath(out.toString())
                .classArgs("pkg.Super")
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.STDOUT);

        // let's first check that it runs wo issues
        if (!output.contains("done")) {
            throw new AssertionError("execution of Super didn't finish");
        }

        // now lets change the super class
        tb.writeJavaFiles(superClass, superClassCode2);
        Path sub2 = pkg.resolve("Sub2");
        tb.writeJavaFiles(sub2, subClass2Code);

        new JavacTask(tb)
                .classpath(out)
                .outdir(out)
                .files(findJavaFiles(superClass)[0], findJavaFiles(sub2)[0])
                .run();

        new JavaTask(tb)
                .classpath(out.toString())
                .classArgs("pkg.Super")
                .run(Task.Expect.SUCCESS);
    }
}
