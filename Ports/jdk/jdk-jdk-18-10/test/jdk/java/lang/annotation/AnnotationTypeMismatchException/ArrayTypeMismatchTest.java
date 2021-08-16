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
 * @bug 8266766
 * @summary An array property of a type that is no longer of a type that is a legal member of an
 *          annotation should throw an AnnotationTypeMismatchException.
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @run main ArrayTypeMismatchTest
 */

import jdk.internal.org.objectweb.asm.AnnotationVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationTypeMismatchException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;

public class ArrayTypeMismatchTest {

    public static void main(String[] args) throws Exception {
        /*
         * This test creates an annotation where the annotation member's type is an array with
         * a component type that cannot be legally used for an annotation member. This can happen
         * if a class is recompiled independencly of the annotation type and linked at runtime
         * in this new version. For a test, a class is created as:
         *
         * package sample;
         * @Carrier(value = { @NoAnnotation })
         * class Host { }
         *
         * where NoAnnotation is defined as a regular interface and not as an annotation type.
         * The classes are created by using ASM to emulate this state.
         */
        ByteArrayClassLoader cl = new ByteArrayClassLoader(NoAnnotation.class.getClassLoader());
        cl.init(annotationType(), carrierType());
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> host = (Class<? extends Annotation>) cl.loadClass("sample.Host");
        Annotation sample = cl.loadClass("sample.Carrier").getAnnotation(host);
        try {
            Object value = host.getMethod("value").invoke(sample);
            throw new IllegalStateException("Found value: " + value);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof AnnotationTypeMismatchException) {
                AnnotationTypeMismatchException e = ((AnnotationTypeMismatchException) cause);
                if (!e.element().getName().equals("value")) {
                    throw new IllegalStateException("Unexpected element: " + e.element());
                } else if (!e.foundType().equals("Array with component tag: @")) {
                    throw new IllegalStateException("Unexpected type: " + e.foundType());
                }
            } else {
                throw new IllegalStateException(cause);
            }
        }
    }

    private static byte[] carrierType() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, 0, "sample/Carrier", null, Type.getInternalName(Object.class), null);
        AnnotationVisitor v = writer.visitAnnotation("Lsample/Host;", true);
        AnnotationVisitor a = v.visitArray("value");
        a.visitAnnotation(null, Type.getDescriptor(NoAnnotation.class)).visitEnd();
        a.visitEnd();
        v.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] annotationType() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE | Opcodes.ACC_ANNOTATION,
                "sample/Host",
                null,
                Type.getInternalName(Object.class),
                new String[]{Type.getInternalName(Annotation.class)});
        AnnotationVisitor a = writer.visitAnnotation(Type.getDescriptor(Retention.class), true);
        a.visitEnum("value", Type.getDescriptor(RetentionPolicy.class), RetentionPolicy.RUNTIME.name());
        writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                "value",
                Type.getMethodDescriptor(Type.getType(NoAnnotation[].class)),
                null,
                null).visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    public interface NoAnnotation { }

    public static class ByteArrayClassLoader extends ClassLoader {

        public ByteArrayClassLoader(ClassLoader parent) {
            super(parent);
        }

        void init(byte[] annotationType, byte[] carrierType) {
            defineClass("sample.Host", annotationType, 0, annotationType.length);
            defineClass("sample.Carrier", carrierType, 0, carrierType.length);
        }
    }
}
