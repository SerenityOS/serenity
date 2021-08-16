/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test 8216261
 * @summary Javap ignores default modifier on interfaces
 * @library /tools/lib
 * @modules jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavapTask
 * @run main JavapNotPrintingDefaultModifierTest
 */

import java.nio.file.*;
import java.util.*;

import toolbox.JavapTask;
import toolbox.TestRunner;
import toolbox.ToolBox;
import toolbox.Task;

public class JavapNotPrintingDefaultModifierTest extends TestRunner {
    ToolBox tb = new ToolBox();

    interface SimpleInterface {
        default void defaultMethod() {}
        void foo();
    }

    private static final List<String> expectedOutput = List.of(
            "Compiled from \"JavapNotPrintingDefaultModifierTest.java\"",
            "interface JavapNotPrintingDefaultModifierTest$SimpleInterface {",
            "  public default void defaultMethod();",
            "  public abstract void foo();",
            "}");

    JavapNotPrintingDefaultModifierTest() throws Exception {
        super(System.err);
    }

    public static void main(String... args) throws Exception {
        JavapNotPrintingDefaultModifierTest tester = new JavapNotPrintingDefaultModifierTest();
        tester.runTests();
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    @Test
    public void testMain(Path base) throws Exception {
        Path testClassesPath = Paths.get(System.getProperty("test.classes"));
        List<String> output = new JavapTask(tb)
                .options("-p", testClassesPath.resolve(this.getClass().getSimpleName() + "$SimpleInterface.class").toString())
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        System.out.println(output);
        if (!output.equals(expectedOutput)) {
            throw new AssertionError(String.format("unexpected output:\n %s", output));
        }
    }
}
