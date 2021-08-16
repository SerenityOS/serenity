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
 * @bug 8067914
 * @summary Redundant type cast nodes in AST (follow up from JDK-8043741)
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox
 * @run compile -XD-printsource T8067914.java
 * @run main NukeExtraCast
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import toolbox.ToolBox;

public class NukeExtraCast {

    public static void main(String[] args) throws Exception {
        ToolBox tb = new ToolBox();
        Path path1 = Paths.get(ToolBox.testClasses, "E.java");
        List<String> file1 = tb.readAllLines(path1);

        Path path2 = Paths.get(ToolBox.testSrc, "E.out");
        List<String> file2 = tb.readAllLines(path2);
        tb.checkEqual(file1, file2);
    }

}
