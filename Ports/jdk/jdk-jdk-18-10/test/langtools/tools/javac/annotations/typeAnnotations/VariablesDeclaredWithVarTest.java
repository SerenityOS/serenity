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
 * @bug 8261205
 * @summary check that potentially applicable type annotations are skip if the variable or parameter was declared with var
 * @library /tools/lib
 * @modules
 *      jdk.jdeps/com.sun.tools.classfile
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main VariablesDeclaredWithVarTest
 */

import java.util.List;
import java.util.ArrayList;

import java.io.File;
import java.nio.file.Paths;

import java.lang.annotation.*;
import java.util.Arrays;

import com.sun.tools.classfile.*;
import com.sun.tools.javac.util.Assert;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class VariablesDeclaredWithVarTest {
    ToolBox tb = new ToolBox();

    final String src =
            """
            import java.util.function.*;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            @Target({ElementType.TYPE_USE, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
            @interface A {}

            class Test {
                void kaa() {
                    @A var c = g(1, 1L);
                }

                <X> X g(X a, X b) {
                    return a;
                }

                void foo() {
                    bar((@A var s) -> s);
                }

                void bar(Function<String, String> f) {}
            }
            """;

    public static void main(String... args) throws Exception {
        new VariablesDeclaredWithVarTest().run();
    }

    void run() throws Exception {
        compileTestClass();
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"),
                "Test.class").toUri()), 0);
    }

    void compileTestClass() throws Exception {
        new JavacTask(tb)
                .sources(src)
                .run();
    }

    void checkClassFile(final File cfile, int... taPositions) throws Exception {
        ClassFile classFile = ClassFile.read(cfile);
        List<TypeAnnotation> annos = new ArrayList<>();
        for (Method method : classFile.methods) {
            findAnnotations(classFile, method, annos);
            String methodName = method.getName(classFile.constant_pool);
            Assert.check(annos.size() == 0, "there shouldn't be any type annotations in any method, found " + annos.size() +
                    " type annotations at method " + methodName);
        }
    }

    void findAnnotations(ClassFile cf, Method m, List<TypeAnnotation> annos) {
        findAnnotations(cf, m, Attribute.RuntimeVisibleTypeAnnotations, annos);
        findAnnotations(cf, m, Attribute.RuntimeInvisibleTypeAnnotations, annos);
    }

    void findAnnotations(ClassFile cf, Method m, String name, List<TypeAnnotation> annos) {
        int index = m.attributes.getIndex(cf.constant_pool, name);
        if (index != -1) {
            Attribute attr = m.attributes.get(index);
            assert attr instanceof RuntimeTypeAnnotations_attribute;
            RuntimeTypeAnnotations_attribute tAttr = (RuntimeTypeAnnotations_attribute)attr;
            annos.addAll(Arrays.asList(tAttr.annotations));
        }

        int cindex = m.attributes.getIndex(cf.constant_pool, Attribute.Code);
        if (cindex != -1) {
            Attribute cattr = m.attributes.get(cindex);
            assert cattr instanceof Code_attribute;
            Code_attribute cAttr = (Code_attribute)cattr;
            index = cAttr.attributes.getIndex(cf.constant_pool, name);
            if (index != -1) {
                Attribute attr = cAttr.attributes.get(index);
                assert attr instanceof RuntimeTypeAnnotations_attribute;
                RuntimeTypeAnnotations_attribute tAttr = (RuntimeTypeAnnotations_attribute)attr;
                annos.addAll(Arrays.asList(tAttr.annotations));
            }
        }
    }
}
