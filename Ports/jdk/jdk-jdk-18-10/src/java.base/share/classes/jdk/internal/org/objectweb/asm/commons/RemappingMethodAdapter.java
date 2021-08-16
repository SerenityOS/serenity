/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package jdk.internal.org.objectweb.asm.commons;

import jdk.internal.org.objectweb.asm.AnnotationVisitor;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.TypePath;

/**
 * A {@link LocalVariablesSorter} for type mapping.
 *
 * @deprecated use {@link MethodRemapper} instead.
 * @author Eugene Kuleshov
 */
@Deprecated
public class RemappingMethodAdapter extends LocalVariablesSorter {

    protected final Remapper remapper;

    public RemappingMethodAdapter(
            final int access,
            final String descriptor,
            final MethodVisitor methodVisitor,
            final Remapper remapper) {
        this(Opcodes.ASM6, access, descriptor, methodVisitor, remapper);
    }

    protected RemappingMethodAdapter(
            final int api,
            final int access,
            final String descriptor,
            final MethodVisitor methodVisitor,
            final Remapper remapper) {
        super(api, access, descriptor, methodVisitor);
        this.remapper = remapper;
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        AnnotationVisitor annotationVisitor = super.visitAnnotationDefault();
        return annotationVisitor == null
                ? annotationVisitor
                : new RemappingAnnotationAdapter(annotationVisitor, remapper);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        AnnotationVisitor annotationVisitor =
                super.visitAnnotation(remapper.mapDesc(descriptor), visible);
        return annotationVisitor == null
                ? annotationVisitor
                : new RemappingAnnotationAdapter(annotationVisitor, remapper);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        AnnotationVisitor annotationVisitor =
                super.visitTypeAnnotation(typeRef, typePath, remapper.mapDesc(descriptor), visible);
        return annotationVisitor == null
                ? annotationVisitor
                : new RemappingAnnotationAdapter(annotationVisitor, remapper);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(
            final int parameter, final String descriptor, final boolean visible) {
        AnnotationVisitor annotationVisitor =
                super.visitParameterAnnotation(parameter, remapper.mapDesc(descriptor), visible);
        return annotationVisitor == null
                ? annotationVisitor
                : new RemappingAnnotationAdapter(annotationVisitor, remapper);
    }

    @Override
    public void visitFrame(
            final int type,
            final int numLocal,
            final Object[] local,
            final int numStack,
            final Object[] stack) {
        super.visitFrame(
                type, numLocal, remapEntries(numLocal, local), numStack, remapEntries(numStack, stack));
    }

    private Object[] remapEntries(final int numTypes, final Object[] entries) {
        if (entries == null) {
            return entries;
        }
        Object[] remappedEntries = null;
        for (int i = 0; i < numTypes; ++i) {
            if (entries[i] instanceof String) {
                if (remappedEntries == null) {
                    remappedEntries = new Object[numTypes];
                    System.arraycopy(entries, 0, remappedEntries, 0, numTypes);
                }
                remappedEntries[i] = remapper.mapType((String) entries[i]);
            }
        }
        return remappedEntries == null ? entries : remappedEntries;
    }

    @Override
    public void visitFieldInsn(
            final int opcode, final String owner, final String name, final String descriptor) {
        super.visitFieldInsn(
                opcode,
                remapper.mapType(owner),
                remapper.mapFieldName(owner, name, descriptor),
                remapper.mapDesc(descriptor));
    }

    @Deprecated
    @Override
    public void visitMethodInsn(
            final int opcode, final String owner, final String name, final String descriptor) {
        if (api >= Opcodes.ASM5) {
            super.visitMethodInsn(opcode, owner, name, descriptor);
            return;
        }
        doVisitMethodInsn(opcode, owner, name, descriptor, opcode == Opcodes.INVOKEINTERFACE);
    }

    @Override
    public void visitMethodInsn(
            final int opcode,
            final String owner,
            final String name,
            final String descriptor,
            final boolean isInterface) {
        if (api < Opcodes.ASM5) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            return;
        }
        doVisitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    private void doVisitMethodInsn(
            final int opcode,
            final String owner,
            final String name,
            final String descriptor,
            final boolean isInterface) {
        // Calling super.visitMethodInsn requires to call the correct version
        // depending on this.api (otherwise infinite loops can occur). To
        // simplify and to make it easier to automatically remove the backward
        // compatibility code, we inline the code of the overridden method here.
        // IMPORTANT: THIS ASSUMES THAT visitMethodInsn IS NOT OVERRIDDEN IN
        // LocalVariableSorter.
        if (mv != null) {
            mv.visitMethodInsn(
                    opcode,
                    remapper.mapType(owner),
                    remapper.mapMethodName(owner, name, descriptor),
                    remapper.mapMethodDesc(descriptor),
                    isInterface);
        }
    }

    @Override
    public void visitInvokeDynamicInsn(
            final String name,
            final String descriptor,
            final Handle bootstrapMethodHandle,
            final Object... bootstrapMethodArguments) {
        for (int i = 0; i < bootstrapMethodArguments.length; i++) {
            bootstrapMethodArguments[i] = remapper.mapValue(bootstrapMethodArguments[i]);
        }
        super.visitInvokeDynamicInsn(
                remapper.mapInvokeDynamicMethodName(name, descriptor),
                remapper.mapMethodDesc(descriptor),
                (Handle) remapper.mapValue(bootstrapMethodHandle),
                bootstrapMethodArguments);
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        super.visitTypeInsn(opcode, remapper.mapType(type));
    }

    @Override
    public void visitLdcInsn(final Object value) {
        super.visitLdcInsn(remapper.mapValue(value));
    }

    @Override
    public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
        super.visitMultiANewArrayInsn(remapper.mapDesc(descriptor), numDimensions);
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(
            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        AnnotationVisitor annotationVisitor =
                super.visitInsnAnnotation(typeRef, typePath, remapper.mapDesc(descriptor), visible);
        return annotationVisitor == null
                ? annotationVisitor
                : new RemappingAnnotationAdapter(annotationVisitor, remapper);
    }

    @Override
    public void visitTryCatchBlock(
            final Label start, final Label end, final Label handler, final String type) {
        super.visitTryCatchBlock(start, end, handler, type == null ? null : remapper.mapType(type));
    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(
            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        AnnotationVisitor annotationVisitor =
                super.visitTryCatchAnnotation(typeRef, typePath, remapper.mapDesc(descriptor), visible);
        return annotationVisitor == null
                ? annotationVisitor
                : new RemappingAnnotationAdapter(annotationVisitor, remapper);
    }

    @Override
    public void visitLocalVariable(
            final String name,
            final String descriptor,
            final String signature,
            final Label start,
            final Label end,
            final int index) {
        super.visitLocalVariable(
                name,
                remapper.mapDesc(descriptor),
                remapper.mapSignature(signature, true),
                start,
                end,
                index);
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(
            final int typeRef,
            final TypePath typePath,
            final Label[] start,
            final Label[] end,
            final int[] index,
            final String descriptor,
            final boolean visible) {
        AnnotationVisitor annotationVisitor =
                super.visitLocalVariableAnnotation(
                        typeRef, typePath, start, end, index, remapper.mapDesc(descriptor), visible);
        return annotationVisitor == null
                ? annotationVisitor
                : new RemappingAnnotationAdapter(annotationVisitor, remapper);
    }
}
