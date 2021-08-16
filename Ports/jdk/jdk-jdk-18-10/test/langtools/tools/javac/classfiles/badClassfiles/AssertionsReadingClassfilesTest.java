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

/* @test
 * @bug 8230776 8231311 8230964 8230919 8230963
 * @summary Javac throws AssertionError on fussed class files
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.TestRunner
 * @compile Test1.jcod
 * @compile Test2.jcod
 * @compile Test3.jcod
 * @compile Test4.jcod
 * @compile Test5.jcod
 * @run main AssertionsReadingClassfilesTest
 */

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;
import toolbox.TestRunner;

public class AssertionsReadingClassfilesTest extends TestRunner {

    static final String SubSrc = "class Sub extends Test#ID {}";
    static final String expectedOuputBadDescriptor = "Sub.java:1:19: compiler.err.cant.access: Test#ID, " +
            "(compiler.misc.bad.class.file.header: Test#ID.class, (compiler.misc.method.descriptor.invalid: <init>))";

    private final ToolBox tb = new ToolBox();

    public AssertionsReadingClassfilesTest() {
        super(System.err);
    }

    public static void main(String... args) throws Exception {
        new AssertionsReadingClassfilesTest().runTests();
    }

    @Test
    public void testBadDescriptor() {
        testHelper(SubSrc.replaceAll("#ID", "1"), expectedOuputBadDescriptor.replaceAll("#ID", "1"));
        testHelper(SubSrc.replaceAll("#ID", "2"), expectedOuputBadDescriptor.replaceAll("#ID", "2"));
    }

    private void testHelper(String src, String expectedMsg) {
        String javacOut = new JavacTask(tb)
                .sources(src)
                .classpath(System.getProperty("test.classes"))
                .options("-XDrawDiagnostics")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        if (!javacOut.contains(expectedMsg)) {
            throw new AssertionError("test failed, unexpected output");
        }
    }


    @Test
    public void testBadSuperClass() {
        String expectedOutput =
                "Sub.java:1:19: compiler.err.cant.access: Test3, (compiler.misc.bad.class.file.header: Test3.class, " +
                "(compiler.misc.unexpected.const.pool.tag.at: 10, 11))";
        testHelper(SubSrc.replaceAll("#ID", "3"), expectedOutput);
    }

    @Test
    public void testModuleInfoExpected() {
        String expectedOutput =
                "Sub.java:1:19: compiler.err.cant.access: Test4, (compiler.misc.bad.class.file.header: Test4.class, " +
                "(compiler.misc.module.info.definition.expected))";
        testHelper(SubSrc.replaceAll("#ID", "4"), expectedOutput);
    }

    @Test
    public void testUnexpectedCPEntry() {
        String expectedOutput =
                "Sub.java:1:19: compiler.err.cant.access: Test5, (compiler.misc.bad.class.file.header: Test5.class, " +
                        "(compiler.misc.unexpected.const.pool.tag.at: 12, 88))";
        testHelper(SubSrc.replaceAll("#ID", "5"), expectedOutput);
    }
}
