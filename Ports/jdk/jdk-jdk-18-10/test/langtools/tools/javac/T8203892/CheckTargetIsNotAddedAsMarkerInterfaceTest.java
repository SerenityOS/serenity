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
 * @test 8203892
 * @summary Target interface added as marker interface in calls to altMetafactory
 * @library /tools/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main CheckTargetIsNotAddedAsMarkerInterfaceTest
 */

import java.io.File;
import java.nio.file.Paths;

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.BootstrapMethods_attribute;
import com.sun.tools.classfile.BootstrapMethods_attribute.BootstrapMethodSpecifier;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool.*;
import com.sun.tools.javac.util.Assert;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class CheckTargetIsNotAddedAsMarkerInterfaceTest {

    static final String testSource =
        "import java.util.*;\n" +
        "import java.util.function.*;\n" +
        "import java.io.*;\n" +

        "class Test {\n" +
        "    public static <T> Comparator<T> comparingInt(ToIntFunction<? super T> keyExtractor) {\n" +
        "        Objects.requireNonNull(keyExtractor);\n" +
        "        return (Comparator<T> & Serializable)\n" +
        "            (c1, c2) -> Integer.compare(keyExtractor.applyAsInt(c1), keyExtractor.applyAsInt(c2));\n" +
        "    }\n" +
        "}";

    public static void main(String[] args) throws Exception {
        new CheckTargetIsNotAddedAsMarkerInterfaceTest().run();
    }

    ToolBox tb = new ToolBox();

    void run() throws Exception {
        compileTestClass();
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"), "Test.class").toUri()));
    }

    void compileTestClass() throws Exception {
        new JavacTask(tb)
                .sources(testSource)
                .run();
    }

    void checkClassFile(final File cfile) throws Exception {
        ClassFile classFile = ClassFile.read(cfile);
        for (Attribute attr : classFile.attributes) {
            if (attr.getName(classFile.constant_pool).equals("BootstrapMethods")) {
                BootstrapMethods_attribute bsmAttr = (BootstrapMethods_attribute)attr;
                BootstrapMethodSpecifier bsmSpecifier = bsmAttr.bootstrap_method_specifiers[0];
                Assert.check(classFile.constant_pool.get(bsmSpecifier.bootstrap_arguments[0]) instanceof CONSTANT_MethodType_info);
                Assert.check(classFile.constant_pool.get(bsmSpecifier.bootstrap_arguments[1]) instanceof CONSTANT_MethodHandle_info);
                Assert.check(classFile.constant_pool.get(bsmSpecifier.bootstrap_arguments[2]) instanceof CONSTANT_MethodType_info);
                Assert.check(classFile.constant_pool.get(bsmSpecifier.bootstrap_arguments[3]) instanceof CONSTANT_Integer_info);
                Assert.check(classFile.constant_pool.get(bsmSpecifier.bootstrap_arguments[4]) instanceof CONSTANT_Integer_info);
                break;
            }
        }
    }
}
