/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.jfr.internal;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import jdk.internal.org.objectweb.asm.AnnotationVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.commons.GeneratorAdapter;
import jdk.internal.org.objectweb.asm.commons.Method;
import jdk.jfr.AnnotationElement;
import jdk.jfr.Event;
import jdk.jfr.ValueDescriptor;


// Helper class for building dynamic events
public final class EventClassBuilder {

    private static final Type TYPE_EVENT = Type.getType(Event.class);
    private static final Type TYPE_IOBE = Type.getType(IndexOutOfBoundsException.class);
    private static final Method DEFAULT_CONSTRUCTOR = Method.getMethod("void <init> ()");
    private static final Method SET_METHOD = Method.getMethod("void set (int, java.lang.Object)");
    private static final AtomicLong idCounter = new AtomicLong();
    private final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    private final String fullClassName;
    private final Type type;
    private final List<ValueDescriptor> fields;
    private final List<AnnotationElement> annotationElements;

    public EventClassBuilder(List<AnnotationElement> annotationElements, List<ValueDescriptor> fields) {
        this.fullClassName = "jdk.jfr.DynamicEvent" + idCounter.incrementAndGet();
        this.type = Type.getType("L" + fullClassName.replace(".", "/") + ";");
        this.fields = fields;
        this.annotationElements = annotationElements;
    }

    public Class<? extends Event> build() {
        buildClassInfo();
        buildConstructor();
        buildFields();
        buildSetMethod();
        endClass();
        byte[] bytes = classWriter.toByteArray();
        ASMToolkit.logASM(fullClassName, bytes);
        return SecuritySupport.defineClass(Event.class, bytes).asSubclass(Event.class);
    }

    private void endClass() {
        classWriter.visitEnd();
    }

    private void buildSetMethod() {
        GeneratorAdapter ga = new GeneratorAdapter(Opcodes.ACC_PUBLIC, SET_METHOD, null, null, classWriter);
        int index = 0;
        for (ValueDescriptor v : fields) {
            ga.loadArg(0);
            ga.visitLdcInsn(index);
            Label notEqual = new Label();
            ga.ifICmp(GeneratorAdapter.NE, notEqual);
            ga.loadThis();
            ga.loadArg(1);
            Type fieldType = ASMToolkit.toType(v);
            ga.unbox(ASMToolkit.toType(v));
            ga.putField(type, v.getName(), fieldType);
            ga.visitInsn(Opcodes.RETURN);
            ga.visitLabel(notEqual);
            index++;
        }
        ga.throwException(TYPE_IOBE, "Index must between 0 and " + fields.size());
        ga.endMethod();
    }

    private void buildConstructor() {
        MethodVisitor mv = classWriter.visitMethod(Opcodes.ACC_PUBLIC, DEFAULT_CONSTRUCTOR.getName(), DEFAULT_CONSTRUCTOR.getDescriptor(), null, null);
        mv.visitIntInsn(Opcodes.ALOAD, 0);
        ASMToolkit.invokeSpecial(mv, TYPE_EVENT.getInternalName(), DEFAULT_CONSTRUCTOR);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
    }

    private void buildClassInfo() {
        String internalSuperName = ASMToolkit.getInternalName(Event.class.getName());
        String internalClassName = type.getInternalName();
        classWriter.visit(52, Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_SUPER, internalClassName, null, internalSuperName, null);

        for (AnnotationElement a : annotationElements) {
            String descriptor = ASMToolkit.getDescriptor(a.getTypeName());
            AnnotationVisitor av = classWriter.visitAnnotation(descriptor, true);
            for (ValueDescriptor v : a.getValueDescriptors()) {
                Object value = a.getValue(v.getName());
                String name = v.getName();
                if (v.isArray()) {
                    AnnotationVisitor arrayVisitor = av.visitArray(name);
                    Object[] array = (Object[]) value;
                    for (int i = 0; i < array.length; i++) {
                        arrayVisitor.visit(null, array[i]);
                    }
                    arrayVisitor.visitEnd();
                } else {
                    av.visit(name, value);
                }
            }
            av.visitEnd();
        }
    }

    private void buildFields() {
        for (ValueDescriptor v : fields) {
            String internal = ASMToolkit.getDescriptor(v.getTypeName());
            classWriter.visitField(Opcodes.ACC_PRIVATE, v.getName(), internal, null, null);
            // No need to store annotations on field since they will be replaced anyway.
        }
    }
}
