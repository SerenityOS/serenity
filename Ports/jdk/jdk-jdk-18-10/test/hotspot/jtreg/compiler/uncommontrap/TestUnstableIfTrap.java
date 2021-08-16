/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8030976 8059226
 * @requires !vm.graal.enabled
 * @library /test/lib /
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbatch -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -XX:+LogCompilation
 *                   -XX:CompileCommand=compileonly,UnstableIfExecutable.test
 *                   -XX:LogFile=always_taken_not_fired.xml
 *                   compiler.uncommontrap.TestUnstableIfTrap ALWAYS_TAKEN false
 * @run main/othervm -Xbatch -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -XX:+LogCompilation
 *                   -XX:CompileCommand=compileonly,UnstableIfExecutable.test
 *                   -XX:LogFile=always_taken_fired.xml
 *                   compiler.uncommontrap.TestUnstableIfTrap ALWAYS_TAKEN true
 * @run main/othervm -Xbatch -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -XX:+LogCompilation
 *                   -XX:CompileCommand=compileonly,UnstableIfExecutable.test
 *                   -XX:LogFile=never_taken_not_fired.xml
 *                   compiler.uncommontrap.TestUnstableIfTrap NEVER_TAKEN false
 * @run main/othervm -Xbatch -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI -XX:+LogCompilation
 *                   -XX:CompileCommand=compileonly,UnstableIfExecutable.test
 *                   -XX:LogFile=never_taken_fired.xml
 *                   compiler.uncommontrap.TestUnstableIfTrap NEVER_TAKEN true
 * @run driver compiler.testlibrary.uncommontrap.Verifier always_taken_not_fired.xml
 *                                                        always_taken_fired.xml
 *                                                        never_taken_not_fired.xml
 *                                                        never_taken_fired.xml
 */

package compiler.uncommontrap;

import compiler.testlibrary.uncommontrap.Verifier;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.test.lib.ByteCodeLoader;
import jdk.test.lib.Platform;
import sun.hotspot.WhiteBox;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_STATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_VOLATILE;
import static jdk.internal.org.objectweb.asm.Opcodes.GETSTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.GOTO;
import static jdk.internal.org.objectweb.asm.Opcodes.IADD;
import static jdk.internal.org.objectweb.asm.Opcodes.ICONST_1;
import static jdk.internal.org.objectweb.asm.Opcodes.IFEQ;
import static jdk.internal.org.objectweb.asm.Opcodes.ILOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.ISUB;
import static jdk.internal.org.objectweb.asm.Opcodes.RETURN;

public class TestUnstableIfTrap {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();
    private static final String CLASS_NAME = "UnstableIfExecutable";
    private static final String METHOD_NAME = "test";
    private static final String FIELD_NAME = "field";
    private static final int ITERATIONS = 1_000_000;
    // There is no dependency on particular class file version, so it could be
    // set to any version (if you're updating this test for Java 42).
    private static final int CLASS_FILE_VERSION = 49;
    private static final int MAX_TIER = 4;
    // This test aimed to verify that uncommon trap with reason "unstable_if"
    // is emitted when method that contain control-flow divergence such that
    // one of two branches is never taken (and other one is taken always).
    // C2 will made a decision whether or not the branch was ever taken
    // depending on method's profile.
    // If profile was collected for a few method's invocations, then C2 will not
    // trust in branches' probabilities and the tested trap won't be emitted.
    // In fact, a method has to be invoked at least 40 time at the day when this
    // comment was written (see Parse::dynamic_branch_prediction for an actual
    // value). It would be to implementation dependent to use "40" as
    // a threshold value in the test, so in order to improve test's robustness
    // the threshold value is 1000: if the tested method was compiled by C2
    // before it was invoked 1000 times, then we won't verify that trap was
    // emitted and fired.
    private static final int MIN_INVOCATIONS_BEFORE_C2_COMPILATION = 1000;
    /**
     * Description of test case parameters and uncommon trap that will
     * be emitted during tested method compilation.
     */
    private static enum TestCaseName {
        ALWAYS_TAKEN(false, "taken always"),
        NEVER_TAKEN(true, "taken never");
        TestCaseName(boolean predicate, String comment) {
            this.predicate = predicate;
            this.comment = comment;
        }

        public final boolean predicate;
        public final String name = "unstable_if";
        public final String comment;
    }

    public static void main(String args[]) {
        if (args.length != 2) {
            throw new Error("Expected two arguments: test case name and a "
                    + "boolean determining if uncommon trap should be fired.");
        }
        test(TestCaseName.valueOf(args[0]), Boolean.valueOf(args[1]));
    }

    private static void test(TestCaseName testCase, boolean shouldBeFired) {
        Method testMethod;
        Label unstableIfLocation = new Label();
        boolean shouldBeEmitted;
        boolean compiledToEarly = false;

        try {
            Class testClass = ByteCodeLoader.load(CLASS_NAME,
                    generateTest(unstableIfLocation));
            testMethod = testClass.getDeclaredMethod(METHOD_NAME,
                    boolean.class);
            for (int i = 0; i < ITERATIONS; i++) {
                testMethod.invoke(null, testCase.predicate);
                if (i < MIN_INVOCATIONS_BEFORE_C2_COMPILATION
                        && isMethodCompiledByC2(testMethod)) {
                    compiledToEarly = true;
                    // There is no sense in further invocations: we already
                    // decided to avoid verification.
                    break;
                }
            }
            // We're checking that trap should be emitted (i.e. it was compiled
            // by C2) before the trap is fired, because otherwise the nmethod
            // will be deoptimized and isMethodCompiledByC2 will return false.
            shouldBeEmitted = isMethodCompiledByC2(testMethod)
                    && !compiledToEarly;
            if (shouldBeFired) {
                testMethod.invoke(null, !testCase.predicate);
            }
        } catch (ReflectiveOperationException e) {
            throw new Error("Test case should be generated, loaded and executed"
                    + " without any issues.", e);
        }

        shouldBeFired &= shouldBeEmitted;

        Properties properties = new Properties();
        properties.setProperty(Verifier.VERIFICATION_SHOULD_BE_SKIPPED,
                Boolean.toString(compiledToEarly));
        properties.setProperty(Verifier.UNCOMMON_TRAP_SHOULD_EMITTED,
                Boolean.toString(shouldBeEmitted));
        properties.setProperty(Verifier.UNCOMMON_TRAP_SHOULD_FIRED,
                Boolean.toString(shouldBeFired));
        properties.setProperty(Verifier.UNCOMMON_TRAP_NAME, testCase.name);
        properties.setProperty(Verifier.UNCOMMON_TRAP_COMMENT,
                testCase.comment);
        properties.setProperty(Verifier.UNCOMMON_TRAP_BCI,
                Integer.toString(unstableIfLocation.getOffset()));

        properties.list(System.out);

        File f = new File(WB.getStringVMFlag("LogFile") +
                Verifier.PROPERTIES_FILE_SUFFIX);
        try (FileWriter wr = new FileWriter(f)) {
            properties.store(wr, "");
        } catch (IOException e) {
            throw new Error("Unable to store test properties.", e);
        }
    }

    private static boolean isMethodCompiledByC2(Method m) {
        boolean isTiered = WB.getBooleanVMFlag("TieredCompilation");
        boolean isMethodCompiled = WB.isMethodCompiled(m);
        boolean isMethodCompiledAtMaxTier
                = WB.getMethodCompilationLevel(m) == MAX_TIER;

        return Platform.isServer() && !Platform.isEmulatedClient() && isMethodCompiled
                && (!isTiered || isMethodCompiledAtMaxTier);
    }

    /**
     * Generates class with name {@code CLASS_NAME}, which will contain a
     * static method {@code METHOD_NAME}:
     *
     * <pre>{@code
     * public abstract class UnstableIfExecutable {
     *   private static int field = 0;
     *
     *   public static void test(boolean alwaysTrue) {
     *     if (alwaysTrue) {
     *       field++;
     *     } else {
     *       field--;
     *     }
     *   }
     * }
     * }</pre>
     *
     * @return generated bytecode.
     */
    private static byte[] generateTest(Label unstableIfLocation) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        cw.visit(CLASS_FILE_VERSION, ACC_PUBLIC | ACC_ABSTRACT, CLASS_NAME,
                null, "java/lang/Object", null);

        cw.visitField(ACC_PUBLIC | ACC_STATIC | ACC_VOLATILE, FIELD_NAME,
                "I", null, Integer.valueOf(0));

        generateTestMethod(cw, unstableIfLocation);

        return cw.toByteArray();
    }

    private static void generateTestMethod(ClassVisitor cv,
            Label unstableIfLocation) {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC | ACC_STATIC, METHOD_NAME,
                "(Z)V", null, null);
        mv.visitCode();

        Label end = new Label();
        Label falseBranch = new Label();

        // push "field" field's value and 1 to stack
        mv.visitFieldInsn(GETSTATIC, CLASS_NAME, FIELD_NAME, "I");
        mv.visitInsn(ICONST_1);
        // load argument's value
        mv.visitVarInsn(ILOAD, 0); // alwaysTrue
        // here is our unstable if
        mv.visitLabel(unstableIfLocation);
        mv.visitJumpInsn(IFEQ, falseBranch);
        // increment on "true"
        mv.visitInsn(IADD);
        mv.visitJumpInsn(GOTO, end);
        // decrement on "false"
        mv.visitLabel(falseBranch);
        mv.visitInsn(ISUB);
        mv.visitLabel(end);
        // bye bye
        mv.visitInsn(RETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}

