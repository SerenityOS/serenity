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
 * @summary test for com.sun.tools.javac.comp.Check::validateAnnotation, com.sun.tools.javac.code.SymbolMetadata::removeDeclarationMetadata and ::removeFromCompoundList
 * @bug 8241312 8246774
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.classfile
 * @run main ApplicableAnnotationsOnRecords
 */
import com.sun.tools.classfile.*;
import com.sun.tools.javac.util.Assert;
import java.lang.annotation.*;
import java.io.InputStream;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@interface FieldAnnotation {
}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@interface MethodAnnotation {
}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@interface ParameterAnnotation {
}

public record ApplicableAnnotationsOnRecords(@FieldAnnotation @MethodAnnotation @ParameterAnnotation String s, @FieldAnnotation @MethodAnnotation @ParameterAnnotation int i) {

    public static void main(String... args) throws Exception {
        try ( InputStream in = ApplicableAnnotationsOnRecords.class.getResourceAsStream("ApplicableAnnotationsOnRecords.class")) {
            ClassFile cf = ClassFile.read(in);
            Assert.check(cf.methods.length > 5);
            for (Method m : cf.methods) {
                String methodName = m.getName(cf.constant_pool);
                if (methodName.equals("toString") || methodName.equals("hashCode") || methodName.equals("equals") || methodName.equals("main")) {
                    // ignore
                } else if (methodName.equals("<init>")) {
                    var paAnnos = ((RuntimeVisibleParameterAnnotations_attribute) m.attributes.get(Attribute.RuntimeVisibleParameterAnnotations)).parameter_annotations;
                    Assert.check(paAnnos.length > 0);
                    for (var pa : paAnnos) {
                        Assert.check(pa.length == 1);
                        Assert.check(cf.constant_pool.getUTF8Value(pa[0].type_index).equals("LParameterAnnotation;"));
                    }
                } else {
                    var annos = ((RuntimeAnnotations_attribute) m.attributes.get(Attribute.RuntimeVisibleAnnotations)).annotations;
                    Assert.check(annos.length == 1);
                    Assert.check(cf.constant_pool.getUTF8Value(annos[0].type_index).equals("LMethodAnnotation;"));
                }
            }
            Assert.check(cf.fields.length > 0);
            for (Field field : cf.fields) {
                var annos = ((RuntimeAnnotations_attribute) field.attributes.get(Attribute.RuntimeVisibleAnnotations)).annotations;
                Assert.check(annos.length == 1);
                Assert.check(cf.constant_pool.getUTF8Value(annos[0].type_index).equals("LFieldAnnotation;"));
            }
        }
    }
}
