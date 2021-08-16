/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary sourcefile attribute test for module-info.
 * @bug 8080878
 * @library /tools/lib /tools/javac/lib ../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask InMemoryFileManager TestBase SourceFileTestBase
 * @run main ModuleInfoTest
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class ModuleInfoTest extends SourceFileTestBase {
    public static void main(String[] args) throws Exception {
        Path outdir = Paths.get(".");
        ToolBox tb = new ToolBox();
        final Path moduleInfo = Paths.get("module-info.java");
        tb.writeFile(moduleInfo, "module m1{}");
        new JavacTask(tb)
                .files(moduleInfo)
                .outdir(outdir)
                .run(Task.Expect.SUCCESS)
                .writeAll();

        new ModuleInfoTest().test(outdir.resolve("module-info.class"), "module-info.java");
    }
}
