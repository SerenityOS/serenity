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
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.FieldVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.ModuleVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.TypePath;

/**
 * A {@link ClassVisitor} for type remapping.
 *
 * @deprecated use {@link ClassRemapper} instead.
 * @author Eugene Kuleshov
 */
@Deprecated
public class RemappingClassAdapter extends ClassVisitor {

    protected final Remapper remapper;

    protected String className;

    public RemappingClassAdapter(final ClassVisitor classVisitor, final Remapper remapper) {
        this(Opcodes.ASM6, classVisitor, remapper);
    }

    protected RemappingClassAdapter(
            final int api, final ClassVisitor classVisitor, final Remapper remapper) {
        super(api, classVisitor);
        this.remapper = remapper;
    }

    @Override
    public void visit(
            final int version,
            final int access,
            final String name,
            final String signature,
            final String superName,
            final String[] interfaces) {
        this.className = name;
        super.visit(
                version,
                access,
                remapper.mapType(name),
                remapper.mapSignature(signature, false),
                remapper.mapType(superName),
                interfaces == null ? null : remapper.mapTypes(interfaces));
    }

    @Override
    public ModuleVisitor visitModule(final String name, final int flags, final String version) {
        throw new RuntimeException("RemappingClassAdapter is deprecated, use ClassRemapper instead");
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        AnnotationVisitor annotationVisitor =
                super.visitAnnotation(remapper.mapDesc(descriptor), visible);
        return annotationVisitor == null ? null : createRemappingAnnotationAdapter(annotationVisitor);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        AnnotationVisitor annotationVisitor =
                super.visitTypeAnnotation(typeRef, typePath, remapper.mapDesc(descriptor), visible);
        return annotationVisitor == null ? null : createRemappingAnnotationAdapter(annotationVisitor);
    }

    @Override
    public FieldVisitor visitField(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final Object value) {
        FieldVisitor fieldVisitor =
                super.visitField(
                        access,
                        remapper.mapFieldName(className, name, descriptor),
                        remapper.mapDesc(descriptor),
                        remapper.mapSignature(signature, true),
                        remapper.mapValue(value));
        return fieldVisitor == null ? null : createRemappingFieldAdapter(fieldVisitor);
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
        String newDescriptor = remapper.mapMethodDesc(descriptor);
        MethodVisitor methodVisitor =
                super.visitMethod(
                        access,
                        remapper.mapMethodName(className, name, descriptor),
                        newDescriptor,
                        remapper.mapSignature(signature, false),
                        exceptions == null ? null : remapper.mapTypes(exceptions));
        return methodVisitor == null
                ? null
                : createRemappingMethodAdapter(access, newDescriptor, methodVisitor);
    }

    @Override
    public void visitInnerClass(
            final String name, final String outerName, final String innerName, final int access) {
        super.visitInnerClass(
                remapper.mapType(name),
                outerName == null ? null : remapper.mapType(outerName),
                innerName,
                access);
    }

    @Override
    public void visitOuterClass(final String owner, final String name, final String descriptor) {
        super.visitOuterClass(
                remapper.mapType(owner),
                name == null ? null : remapper.mapMethodName(owner, name, descriptor),
                descriptor == null ? null : remapper.mapMethodDesc(descriptor));
    }

    protected FieldVisitor createRemappingFieldAdapter(final FieldVisitor fieldVisitor) {
        return new RemappingFieldAdapter(fieldVisitor, remapper);
    }

    protected MethodVisitor createRemappingMethodAdapter(
            final int access, final String newDescriptor, final MethodVisitor methodVisitior) {
        return new RemappingMethodAdapter(access, newDescriptor, methodVisitior, remapper);
    }

    protected AnnotationVisitor createRemappingAnnotationAdapter(final AnnotationVisitor av) {
        return new RemappingAnnotationAdapter(av, remapper);
    }
}
