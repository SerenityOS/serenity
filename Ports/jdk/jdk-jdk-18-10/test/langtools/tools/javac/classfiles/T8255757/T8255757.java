/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8255757
 * @summary Javac shouldn't emit duplicate pool entries on array::clone
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main T8255757
 */

import java.nio.file.Path;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPool.*;

import toolbox.JavacTask;
import toolbox.ToolBox;
import toolbox.TestRunner;

public class T8255757 extends TestRunner {
    ToolBox tb;

    T8255757() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String[] args) throws Exception {
        T8255757 t = new T8255757();
        t.runTests();
    }

    @Test
    public void testDuplicatePoolEntries() throws Exception {
        String code = """
                public class Test {
                    void test(Object[] o) {
                        o.clone();
                        o.clone();
                    }
                    void test2(Object[] o) {
                        o.clone();
                        o.clone();
                    }
                }""";
        Path curPath = Path.of(".");
        new JavacTask(tb)
                .sources(code)
                .outdir(curPath)
                .run();

        ClassFile cf = ClassFile.read(curPath.resolve("Test.class"));
        ConstantPool cp = cf.constant_pool;
        int num = 0;
        for (CPInfo cpInfo : cp.entries()) {
            if (cpInfo instanceof CONSTANT_Methodref_info) {
                int class_index = ((CONSTANT_Methodref_info) cpInfo).class_index;
                int name_and_type_index = ((CONSTANT_Methodref_info) cpInfo).name_and_type_index;
                int class_name_index = ((CONSTANT_Class_info)
                        cp.getClassInfo(class_index)).name_index;
                int method_name_index = ((CONSTANT_NameAndType_info)
                        cp.getNameAndTypeInfo(name_and_type_index)).name_index;
                int method_type_name_index = ((CONSTANT_NameAndType_info)
                        cp.getNameAndTypeInfo(name_and_type_index)).type_index;
                if ("[Ljava/lang/Object;".equals(cp.getUTF8Value(class_name_index)) &&
                        "clone".equals(cp.getUTF8Value(method_name_index)) &&
                        "()Ljava/lang/Object;".equals(cp.getUTF8Value(method_type_name_index))) {
                    ++num;
                }
            }
        }
        if (num != 1) {
            throw new AssertionError("The number of the pool entries on array::clone is not right. " +
                    "Expected number: 1, actual number: " + num);
        }
    }
}
