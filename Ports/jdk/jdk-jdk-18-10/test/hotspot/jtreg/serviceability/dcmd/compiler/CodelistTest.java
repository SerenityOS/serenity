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
 * @test CodelistTest
 * @bug 8054889
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run testng/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:-UseCodeCacheFlushing -Xmixed -XX:-BackgroundCompilation CodelistTest
 * @run testng/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:-UseCodeCacheFlushing -Xint CodelistTest
 * @summary Test of diagnostic command Compiler.codelist
 *
 * Flag comment:
 * -XX:-UseCodeCacheFlushing - to prevent methods from being removed from the code cache before we have checked the results
 *
 * This test should never run in the same VM as other tests - the code cache may get huge which will
 * create an enormous amount of output to parse. Same for -Xcomp.
 */

import compiler.testlibrary.CompilerUtils;
import compiler.whitebox.CompilerWhiteBoxTest;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;
import org.testng.annotations.Test;
import org.testng.Assert;
import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;
import java.util.Iterator;

public class CodelistTest {

    /**
     * This test calls Jcmd (diagnostic command tool) Compiler.codelist and then parses the output,
     * making sure that the first methods in the list is valid by reflection.
     *
     * Output example:
     *
     * 6 0 java.lang.System.arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V [0x00007f7b49200910, 0x00007f7b49200aa0 - 0x00007f7b49200d30]
     * 2 3 java.lang.String.indexOf(II)I [0x00007f7b49200d90, 0x00007f7b49200f60 - 0x00007f7b49201490]
     * 7 3 java.lang.Math.min(II)I [0x00007f7b4922f010, 0x00007f7b4922f180 - 0x00007f7b4922f338]
     * 8 3 java.lang.String.equals(Ljava/lang/Object;)Z [0x00007f7b4922fb10, 0x00007f7b4922fd40 - 0x00007f7b49230698]
     * 9 3 java.lang.AbstractStringBuilder.ensureCapacityInternal(I)V [0x00007f7b49232010, 0x00007f7b492321a0 - 0x00007f7b49232510]
     * 10 1 java.lang.Object.<init>()V [0x00007f7b49233e90, 0x00007f7b49233fe0 - 0x00007f7b49234118]
     *
     */

    protected static final WhiteBox WB = WhiteBox.getWhiteBox();

    public void run(CommandExecutor executor) {

        TestCase[] testcases = {
                new TestCase(CompilerWhiteBoxTest.COMP_LEVEL_SIMPLE, "testcaseMethod1"),
                new TestCase(CompilerWhiteBoxTest.COMP_LEVEL_LIMITED_PROFILE, "testcaseMethod2"),
                new TestCase(CompilerWhiteBoxTest.COMP_LEVEL_FULL_PROFILE, "testcaseMethod3"),
                new TestCase(CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION, "testcaseMethod4"),
        };

        String directive = "{ match: \"CodelistTest.testcaseMethod*\", " +
                "BackgroundCompilation: false }";
        Assert.assertTrue(
                WB.addCompilerDirective(directive) == 1,
                "Must succeed");

        try {
            // Enqueue one test method for each available level
            int[] complevels = CompilerUtils.getAvailableCompilationLevels();
            for (int level : complevels) {
                // Only test comp level 1 and 4 - level 1, 2 and 3 may interfere with each other
                if (level == 1 || level == 4) {
                    TestCase testcase = testcases[level - 1];
                    WB.enqueueMethodForCompilation(testcase.method, testcase.level);
                    // Set results to false for those methods we must to find
                    // We will also assert if we find any test method we don't expect
                    testcase.check = false;
                }
            }
        } finally {
            WB.removeCompilerDirective(1);
        }

        // Get output from dcmd (diagnostic command)
        OutputAnalyzer output = executor.execute("Compiler.codelist");
        Iterator<String> lines = output.asLines().iterator();

        // Loop over output set result for all found methods
        while (lines.hasNext()) {
            String line = lines.next();

            // Fast check for common part of method name
            if (line.contains("CodelistTest.testcaseMethod")) {
                String[] parts = line.split(" ");
                int compileID = Integer.parseInt(parts[0]);
                Assert.assertTrue(compileID > 0, "CompileID must be positive");

                int compileLevel = Integer.parseInt(parts[1]);
                Assert.assertTrue(compileLevel >= -1, "CompileLevel must be at least -1 (Any)");
                Assert.assertTrue(compileLevel <= 4,  "CompileLevel must be at most 4 (C2)");

                int codeState = Integer.parseInt(parts[2]);
                Assert.assertTrue(codeState >= 0, "CodeState must be at least 0 (In Use)");
                Assert.assertTrue(codeState <= 4, "CodeState must be at most 4 (Unloaded)");

                String str = parts[3];
                for (TestCase testcase : testcases) {
                    if (str.contains(testcase.methodName)) {
                        Assert.assertFalse(testcase.check, "Must not be found or already found.");
                        Assert.assertTrue(testcase.level == compileLevel, "Must have correct level");
                        testcase.check = true;
                    }
                }
            }
        }

        // Check all testcases that was run
        for (TestCase testcase : testcases) {
            Assert.assertTrue(testcase.check, "Missing testcase " + testcase.methodName);
        }
    }

    @Test
    public void jmx() {
        run(new JMXExecutor());
    }

    public void testcaseMethod1() {
    }

    public void testcaseMethod2() {
    }

    public void testcaseMethod3() {
    }

    public void testcaseMethod4() {
    }

    public static Method getMethod(Class klass, String name, Class<?>... parameterTypes) {
        try {
            return klass.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException("exception on getting method Helper." + name, e);
        }
    }

    class TestCase {
        Method method;
        int level;
        String methodName;
        Boolean check;

        public TestCase(int level, String methodName) {
            this.method = getMethod(CodelistTest.class, methodName);
            this.level = level;
            this.methodName = methodName;
            this.check = true;
        }
    }
}
