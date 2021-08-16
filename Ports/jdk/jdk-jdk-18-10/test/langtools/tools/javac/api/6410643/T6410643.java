/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6410643
 * @summary JSR 199: The method JavaCompilerTool.run fails to handle null arguments
 * @author  Peter von der Ah\u00e9
 * @library ../lib
 * @modules java.compiler
 *          jdk.compiler
 * @build ToolTester
 * @run main T6410643
 */

import java.io.IOException;
import javax.tools.JavaFileObject;
import static java.util.Collections.singleton;

public class T6410643 extends ToolTester {

    void testGetTask(Iterable<String> options,
                     Iterable<String> classes,
                     Iterable<? extends JavaFileObject> compilationUnits) {
        try {
            task = tool.getTask(null, null, null, options, classes, compilationUnits);
            throw new AssertionError("Error expected");
        } catch (NullPointerException e) {
            System.err.println("Expected error occurred: " + e);
        }
    }

    void test(String... args) {
        task = tool.getTask(null, null, null, null, null, null);
        try {
            task.call();
            throw new AssertionError("Error expected");
        } catch (IllegalStateException e) {
            System.err.println("Expected error occurred: " + e);
        }

        Iterable<String>         s = singleton(null);
        Iterable<JavaFileObject> f = singleton(null);
        //    case (null, null, null) is tested above
        testGetTask(null, null, f);
        testGetTask(null, s,    null);
        testGetTask(null, s,    f);
        testGetTask(s,    null, null);
        testGetTask(s,    null, f);
        testGetTask(s,    s,     null);
        testGetTask(s,    s,    f);
        System.err.println("Test result: PASSED");
    }

    public static void main(String... args) throws IOException {
        try (T6410643 t = new T6410643()) {
            t.test(args);
        }
    }
}
