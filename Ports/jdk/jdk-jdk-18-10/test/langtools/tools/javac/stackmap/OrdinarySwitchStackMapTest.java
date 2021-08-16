/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8262891
 * @summary Verify StackMapTable is sensible for simple ordinary switches
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavapTask
 * @run compile OrdinarySwitchStackMapTest.java
 * @run main OrdinarySwitchStackMapTest
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import toolbox.JavapTask;
import toolbox.Task;
import toolbox.ToolBox;

public class OrdinarySwitchStackMapTest {

    class Test {
        void method0(int i) throws Exception {
            switch (i) {
                case 0: System.err.println(0); break;
                case 1: System.err.println(1); break;
                default: System.err.println(2); break;
            }
        }
    }

    public static void main(String args[]) throws Exception {
        ToolBox tb = new ToolBox();
        Path pathToClass = Paths.get(ToolBox.testClasses, "OrdinarySwitchStackMapTest$Test.class");
        String javapOut = new JavapTask(tb)
                .options("-v")
                .classes(pathToClass.toString())
                .run()
                .getOutput(Task.OutputKind.DIRECT);

        if (!javapOut.contains("StackMapTable: number_of_entries = 4"))
            throw new AssertionError("The number of entries of the stack map "
                    + "table should be equal to 4");
    }

}
