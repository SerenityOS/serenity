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
 * @bug 8025141
 * @summary Interfaces must not contain non-public fields, ensure $assertionsDisabled
 *          is not generated into an interface
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavaTask Assertions AssertionsTest
 * @run main AssertionsTest -da
 * @run main AssertionsTest -ea:test.Assertions Inner
 * @run main AssertionsTest -ea:test.Outer Outer
 * @run main AssertionsTest -ea:test.Another Another.Inner
 * @run main AssertionsTest -ea:test... Inner Outer Another.Inner
 */

import java.util.Arrays;

import toolbox.JavaTask;
import toolbox.Task;
import toolbox.ToolBox;

public class AssertionsTest {

    public static void main(String... args) throws Exception {
        String testClasses = System.getProperty("test.classes");
        ToolBox tb = new ToolBox();
        new JavaTask(tb).classpath(testClasses)
                         .vmOptions(args[0])
                         .className("test.Assertions")
                         .classArgs(Arrays.copyOfRange(args, 1, args.length))
                         .includeStandardOptions(false)
                         .run(Task.Expect.SUCCESS)
                         .writeAll();
    }

}
