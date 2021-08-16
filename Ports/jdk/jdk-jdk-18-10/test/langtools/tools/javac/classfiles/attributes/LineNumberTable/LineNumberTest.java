/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Tests a line number table attribute for language constructions in different containers.
 * @bug 8040131
 * @library /tools/lib /tools/javac/lib ../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox InMemoryFileManager TestBase
 * @build LineNumberTestBase Container TestCase
 * @run main LineNumberTest
 */
public class LineNumberTest extends LineNumberTestBase {
    public static void main(String[] args) throws Exception {
        new LineNumberTest().test();
    }

    public void test() throws Exception {
        int failed = 0;
        for (TestData testData : TestData.values()) {
            echo("[Executing test]: " + testData);
            try {
                test(testData.container);
            } catch (Exception e) {
                echo("[Test failed]: " + testData);
                e.printStackTrace();
                failed++;
                continue;
            }
            echo("[Test passed]: " + testData);
        }
        if (failed > 0)
            throw new RuntimeException(String.format("Failed tests %d of %d%n", failed, TestData.values().length));
    }

    enum TestData {
        SimpleMethod(new MainContainer()),
        LocalClassContainer(new MainContainer()
                .insert(new LocalClassContainer())),
        LambdaContainer(new MainContainer()
                .insert(new LambdaContainer())),
        LambdaInLambdaContainer(new MainContainer()
                .insert(new LambdaContainer())
                .insert(new LambdaContainer())),
        LambdaInLocalClassContainerTest(new MainContainer()
                .insert(new LocalClassContainer())
                .insert(new LambdaContainer())),
        LocalClassInLambdaContainer(new MainContainer()
                .insert(new LambdaContainer())
                .insert(new LocalClassContainer())),
        LocalInLocalContainer(new MainContainer()
                .insert(new LocalClassContainer())
                .insert(new LocalClassContainer()));
        Container container;

        TestData(Container container) {
            this.container = container;
        }
    }
}
