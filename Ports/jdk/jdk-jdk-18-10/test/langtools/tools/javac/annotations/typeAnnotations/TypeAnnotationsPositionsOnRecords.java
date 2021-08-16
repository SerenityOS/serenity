/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8246774
 * @summary Verify location of type annotations on records
 * @library /tools/lib
 * @modules
 *      jdk.jdeps/com.sun.tools.classfile
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main TypeAnnotationsPositionsOnRecords
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

public class TypeAnnotationsPositionsOnRecords {

    final String src =
            """
            import java.lang.annotation.*;

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ ElementType.TYPE_USE })
            @interface Nullable {}

            record Record1(@Nullable String t) {}

            record Record2(@Nullable String t) {
                public Record2 {}
            }

            record Record3(@Nullable String t1, @Nullable String t2) {}

            record Record4(@Nullable String t1, @Nullable String t2) {
                public Record4 {}
            }

            record Record5(String t1, @Nullable String t2) {}

            record Record6(String t1, @Nullable String t2) {
                public Record6 {}
            }
            """;

    public static void main(String... args) throws Exception {
        new TypeAnnotationsPositionsOnRecords().run();
    }

    ToolBox tb = new ToolBox();

    void run() throws Exception {
        compileTestClass();
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"),
                "Record1.class").toUri()), 0);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"),
                "Record2.class").toUri()), 0);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"),
                "Record3.class").toUri()), 0, 1);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"),
                "Record4.class").toUri()), 0, 1);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"),
                "Record5.class").toUri()), 1);
        checkClassFile(new File(Paths.get(System.getProperty("user.dir"),
                "Record6.class").toUri()), 1);
    }

    void compileTestClass() throws Exception {
        new JavacTask(tb)
                .sources(src)
                .run();
    }

    void checkClassFile(final File cfile, int... taPositions) throws Exception {
        ClassFile classFile = ClassFile.read(cfile);
        int accessorPos = 0;
        int checkedAccessors = 0;
        for (Method method : classFile.methods) {
            String methodName = method.getName(classFile.constant_pool);
            if (methodName.equals("toString") || methodName.equals("hashCode") || methodName.equals("equals")) {
                // ignore
                continue;
            }
            if (methodName.equals("<init>")) {
                checkConstructor(classFile, method, taPositions);
            } else {
                for (int taPos : taPositions) {
                    if (taPos == accessorPos) {
                        checkAccessor(classFile, method);
                        checkedAccessors++;
                    }
                }
                accessorPos++;
            }
        }
        checkFields(classFile, taPositions);
        Assert.check(checkedAccessors == taPositions.length);
    }

    /*
     * there can be several parameters annotated we have to check that the ones annotated are the
     * expected ones
     */
    void checkConstructor(ClassFile classFile, Method method, int... positions) throws Exception {
        List<TypeAnnotation> annos = new ArrayList<>();
        findAnnotations(classFile, method, annos);
        Assert.check(annos.size() == positions.length);
        int i = 0;
        for (int pos : positions) {
            TypeAnnotation ta = annos.get(i);
            Assert.check(ta.position.type.toString().equals("METHOD_FORMAL_PARAMETER"));
            Assert.check(ta.position.parameter_index == pos);
            i++;
        }
    }

    /*
     * this case is simpler as there can only be one annotation at the accessor and it has to be applied
     * at the return type
     */
    void checkAccessor(ClassFile classFile, Method method) {
        List<TypeAnnotation> annos = new ArrayList<>();
        findAnnotations(classFile, method, annos);
        Assert.check(annos.size() == 1);
        TypeAnnotation ta = annos.get(0);
        Assert.check(ta.position.type.toString().equals("METHOD_RETURN"));
    }

    /*
     * here we have to check that only the fields for which its position matches with the one of the
     * original annotated record component are annotated
     */
    void checkFields(ClassFile classFile, int... positions) {
        if (positions != null && positions.length > 0) {
            int fieldPos = 0;
            int annotationPos = 0;
            int currentAnnoPosition = positions[annotationPos];
            int annotatedFields = 0;
            for (Field field : classFile.fields) {
                List<TypeAnnotation> annos = new ArrayList<>();
                findAnnotations(classFile, field, annos);
                if (fieldPos != currentAnnoPosition) {
                    Assert.check(annos.size() == 0);
                } else {
                    Assert.check(annos.size() == 1);
                    TypeAnnotation ta = annos.get(0);
                    Assert.check(ta.position.type.toString().equals("FIELD"));
                    annotationPos++;
                    currentAnnoPosition = annotationPos < positions.length ? positions[annotationPos] : -1;
                    annotatedFields++;
                }
                fieldPos++;
            }
            Assert.check(annotatedFields == positions.length);
        }
    }

    // utility methods
    void findAnnotations(ClassFile cf, Method m, List<TypeAnnotation> annos) {
        findAnnotations(cf, m, Attribute.RuntimeVisibleTypeAnnotations, annos);
        findAnnotations(cf, m, Attribute.RuntimeInvisibleTypeAnnotations, annos);
    }

    void findAnnotations(ClassFile cf, Field m, List<TypeAnnotation> annos) {
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

    void findAnnotations(ClassFile cf, Field m, String name, List<TypeAnnotation> annos) {
        int index = m.attributes.getIndex(cf.constant_pool, name);
        if (index != -1) {
            Attribute attr = m.attributes.get(index);
            assert attr instanceof RuntimeTypeAnnotations_attribute;
            RuntimeTypeAnnotations_attribute tAttr = (RuntimeTypeAnnotations_attribute)attr;
            annos.addAll(Arrays.asList(tAttr.annotations));
        }
    }
}
