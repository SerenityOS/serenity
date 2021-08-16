/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8210009
 * @summary Source Launcher classloader should support getResource and getResourceAsStream
 * @modules jdk.compiler jdk.jdeps
 * @library /tools/lib
 * @build toolbox.JavaTask toolbox.ToolBox
 * @run main GetResourceTest
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import toolbox.JavaTask;
import toolbox.Task;
import toolbox.ToolBox;

/*
 * The body of this test is in ${test.src}/src/CLTest.java,
 * which is executed in single-file source-launcher mode,
 * in order to test the classloader used to launch such programs.
 */
public class GetResourceTest {
    public static void main(String... args) throws Exception {
        GetResourceTest t = new GetResourceTest();
        t.run();
    }

    void run() throws Exception {
        ToolBox tb = new ToolBox();
        Path file = Paths.get(tb.testSrc).resolve("src").resolve("CLTest.java");
        new JavaTask(tb)
            .vmOptions("--add-modules", "jdk.jdeps",
                       "--add-exports", "jdk.jdeps/com.sun.tools.classfile=ALL-UNNAMED")
            .className(file.toString()) // implies source file mode
            .run(Task.Expect.SUCCESS)
            .writeAll();
    }
}
