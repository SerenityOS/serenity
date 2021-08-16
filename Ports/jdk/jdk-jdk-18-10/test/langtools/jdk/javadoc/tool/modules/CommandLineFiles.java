/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8176539
 * @summary Test use case when all java files are listed
 * @modules
 *      jdk.javadoc/jdk.javadoc.internal.api
 *      jdk.javadoc/jdk.javadoc.internal.tool
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.TestRunner
 * @run main CommandLineFiles
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import toolbox.*;

public class CommandLineFiles extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new CommandLineFiles().runTests();
    }

    @Test
    public void testExplicitJavaFiles(Path base) throws Exception {
        Path src = Paths.get(base.toString(), "src");
        Path mpath = Paths.get(src. toString(), "m");

        tb.writeJavaFiles(mpath,
                "module m { exports p; }",
                "package p; public class C { }");

        List<String> cmdList = new ArrayList<>();
        cmdList.add("--source-path");
        cmdList.add(mpath.toString());
        Arrays.asList(tb.findJavaFiles(src)).stream()
                .map(Path::toString)
                .collect(Collectors.toCollection(() -> cmdList));
        execTask(cmdList.toArray(new String[cmdList.size()]));
        assertMessageNotPresent("warning:");
    }

}
