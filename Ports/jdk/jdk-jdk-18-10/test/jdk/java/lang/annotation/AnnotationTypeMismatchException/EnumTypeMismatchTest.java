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
 * @bug 8228988 8266598
 * @summary An enumeration-typed property of an annotation that is represented as an
 *          incompatible property of another type should yield an AnnotationTypeMismatchException.
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @run main EnumTypeMismatchTest
 */

import jdk.internal.org.objectweb.asm.AnnotationVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;

import java.lang.annotation.AnnotationTypeMismatchException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class EnumTypeMismatchTest {

    public static void main(String[] args) throws Exception {
        /*
         * @AnAnnotation(value = @AnAnnotation) // would now be: value = AnEnum.VALUE
         * class Carrier { }
         */
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, 0, "sample/Carrier", null, Type.getInternalName(Object.class), null);
        AnnotationVisitor v = writer.visitAnnotation(Type.getDescriptor(AnAnnotation.class), true);
        v.visitAnnotation("value", Type.getDescriptor(AnAnnotation.class)).visitEnd();
        writer.visitEnd();
        byte[] b = writer.toByteArray();
        ByteArrayClassLoader cl = new ByteArrayClassLoader(EnumTypeMismatchTest.class.getClassLoader());
        cl.init(b);
        AnAnnotation sample = cl.loadClass("sample.Carrier").getAnnotation(AnAnnotation.class);
        try {
            AnEnum value = sample.value();
            throw new IllegalStateException("Found value: " + value);
        } catch (AnnotationTypeMismatchException e) {
            if (!e.element().getName().equals("value")) {
                throw new IllegalStateException("Unexpected element: " + e.element());
            } else if (!e.foundType().equals("@" + AnAnnotation.class.getName() + "(" + AnEnum.VALUE.name() + ")")) {
                throw new IllegalStateException("Unexpected type: " + e.foundType());
            }
        }
    }

    public enum AnEnum {
        VALUE
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface AnAnnotation {
        AnEnum value() default AnEnum.VALUE;
    }

    public static class ByteArrayClassLoader extends ClassLoader {

        public ByteArrayClassLoader(ClassLoader parent) {
            super(parent);
        }

        void init(byte[] b) {
            defineClass("sample.Carrier", b, 0, b.length);
        }
    }
}
