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
 * @summary
 * @library /tools/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main NoLocalsMustBeReservedForDCEedVarsTest
 */

import java.io.File;
import java.nio.file.Paths;

import com.sun.tools.classfile.*;
import com.sun.tools.javac.util.Assert;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class NoLocalsMustBeReservedForDCEedVarsTest {

    static final String source =
            "class Test {\n" +
            "    static String foo() {\n" +
            "        final String hello = \"Hello!\";\n" +
            "        return hello;\n" +
            "    }\n" +
            "} ";

    public static void main(String[] args) throws Exception {
        new NoLocalsMustBeReservedForDCEedVarsTest().run();
    }

    void run() throws Exception {
        ToolBox tb = new ToolBox();
        new JavacTask(tb)
                .sources(source)
                .run();

        File cfile = new File(Paths.get(System.getProperty("user.dir"), "Test.class").toUri());
        ClassFile classFile = ClassFile.read(cfile);
        for (Method method: classFile.methods) {
            if (method.getName(classFile.constant_pool).equals("foo")) {
                Code_attribute codeAttr = (Code_attribute)method.attributes.get("Code");
                Assert.check(codeAttr.max_locals == 0, "max locals found " + codeAttr.max_locals);
            }
        }
    }
}
