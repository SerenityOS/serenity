/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4110560 4785453
 * @summary portability : javac.properties
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main NewLineTest
 */

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

//original test: test/tools/javac/newlines/Newlines.sh
/*
 * Checks that the usage message, contained in the properties in the
 * resource file javac.properties, is correctly rendered, including
 * embedded newlines in the resource strings. For more context,
 * see JDK-4110560.
 */
public class NewLineTest {

    public static void main(String args[]) throws Exception {
        ToolBox tb = new ToolBox();
        File javacOutput = new File("output.txt");
        new JavacTask(tb, Task.Mode.EXEC)
                .redirect(Task.OutputKind.STDOUT, javacOutput.getPath())
                .options("-J-Dline.separator='@'")
                .run(Task.Expect.FAIL);

        List<String> lines = Files.readAllLines(javacOutput.toPath(),
                Charset.defaultCharset());
        if (lines.size() != 1) {
            throw new AssertionError("The compiler output should have one line only");
        }
    }

}
