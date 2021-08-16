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
 * @test CompilerQueueTest
 * @bug 8054889
 * @key intermittent
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @summary Test of diagnostic command Compiler.queue
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run testng/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -Xmixed -XX:+WhiteBoxAPI CompilerQueueTest
 * @run testng/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -Xmixed -XX:-TieredCompilation -XX:+WhiteBoxAPI CompilerQueueTest
 * @run testng/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -Xint -XX:+WhiteBoxAPI CompilerQueueTest
 */

import compiler.testlibrary.CompilerUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;
import org.testng.annotations.Test;
import org.testng.Assert;
import sun.hotspot.WhiteBox;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Iterator;

public class CompilerQueueTest {

    /**
     * This test calls Jcmd (diagnostic command tool) Compiler.queue and
     * then parses the output, making sure that the output look ok.
     *
     *
     * Output example:
     *
     * Current compiles:
     * C1 CompilerThread14 267       3       java.net.URLStreamHandler::parseURL (1166 bytes)
     * C1 CompilerThread13 760       3       javax.management.StandardMBean::getDescription (11 bytes)
     * C1 CompilerThread12 757  s    3       com.sun.jmx.mbeanserver.DefaultMXBeanMappingFactory::getMapping (27 bytes)
     * C1 CompilerThread11 756  s!   3       com.sun.jmx.mbeanserver.DefaultMXBeanMappingFactory::mappingForType (110 bytes)
     * C1 CompilerThread10 761       3       java.lang.StringLatin1::indexOf (121 bytes)
     * C2 CompilerThread7 769       4       CompilerQueueTest::testcaseMethod4 (1 bytes)
     *
     * C1 compile queue:
     * 762       3       java.lang.invoke.MethodType::basicType (8 bytes)
     * 763       3       java.util.ArrayList::rangeCheck (22 bytes)
     * 764       3       java.util.ArrayList::elementData (7 bytes)
     * 765       3       jdk.internal.org.objectweb.asm.MethodVisitor::<init> (35 bytes)
     * 766       1       CompilerQueueTest::testcaseMethod1 (1 bytes)
     * 767       2       CompilerQueueTest::testcaseMethod2 (1 bytes)
     * 768       3       CompilerQueueTest::testcaseMethod3 (1 bytes)
     * 770       3       java.util.Properties::getProperty (46 bytes)
     *
     * C2 compile queue:
     * Empty
     *
     **/

    protected static final WhiteBox WB = WhiteBox.getWhiteBox();

    public void run(CommandExecutor executor) {

        TestCase[] testcases = {
                new TestCase(1, "testcaseMethod1"),
                new TestCase(2, "testcaseMethod2"),
                new TestCase(3, "testcaseMethod3"),
                new TestCase(4, "testcaseMethod4"),
        };

        // Lock compilation makes all compiles stay in queue or compile thread before completion
        WB.lockCompilation();

        // Enqueue one test method for each available level
        int[] complevels = CompilerUtils.getAvailableCompilationLevels();
        for (int level : complevels) {
            TestCase testcase = testcases[level - 1];

            boolean added = WB.enqueueMethodForCompilation(testcase.method, testcase.level);
            // Set results to false for those methods we must to find
            // We will also assert if we find any test method we don't expect
            Assert.assertEquals(added, WB.isMethodQueuedForCompilation(testcase.method));
            testcase.check = false;
        }

        // Get output from dcmd (diagnostic command)
        OutputAnalyzer output = executor.execute("Compiler.queue");
        Iterator<String> lines = output.asLines().iterator();

        // Loop over output set result for all found methods
        while (lines.hasNext()) {
            String str = lines.next();
            // Fast check for common part of method name
            if (str.contains("testcaseMethod")) {
                for (TestCase testcase : testcases) {
                    if (str.contains(testcase.methodName)) {
                        Assert.assertFalse(testcase.check, "Must not be found or already found.");
                        testcase.check = true;
                    }
                }
            }
        }

        for (TestCase testcase : testcases) {
            if (!testcase.check) {
                // If this method wasn't found it must have been removed by policy,
                // verify that it is now removed from the queue
                Assert.assertFalse(WB.isMethodQueuedForCompilation(testcase.method), "Must be found or not in queue");
            }
            // Otherwise all good.
        }

        // Enable compilations again
        WB.unlockCompilation();
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
            this.method = getMethod(CompilerQueueTest.class, methodName);
            this.level = level;
            this.methodName = methodName;
            this.check = true;
        }
    }

}
