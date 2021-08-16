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

import java.util.List;
import jdk.internal.org.objectweb.asm.AnnotationVisitor;
import jdk.internal.org.objectweb.asm.Attribute;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.FieldVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.ModuleVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.RecordComponentVisitor;
import jdk.internal.org.objectweb.asm.TypePath;

/**
 * A {@link ClassVisitor} that remaps types with a {@link Remapper}.
 *
 * <p><i>This visitor has several limitations</i>. A non-exhaustive list is the following:
 *
 * <ul>
 *   <li>it cannot remap type names in dynamically computed strings (remapping of type names in
 *       static values is supported).
 *   <li>it cannot remap values derived from type names at compile time, such as
 *       <ul>
 *         <li>type name hashcodes used by some Java compilers to implement the string switch
 *             statement.
 *         <li>some compound strings used by some Java compilers to implement lambda
 *             deserialization.
 *       </ul>
 * </ul>
 *
 * @author Eugene Kuleshov
 */
public class ClassRemapper extends ClassVisitor {

    /** The remapper used to remap the types in the visited class. */
    protected final Remapper remapper;

    /** The internal name of the visited class. */
    protected String className;

    /**
      * Constructs a new {@link ClassRemapper}. <i>Subclasses must not use this constructor</i>.
      * Instead, they must use the {@link #ClassRemapper(int,ClassVisitor,Remapper)} version.
      *
      * @param classVisitor the class visitor this remapper must deleted to.
      * @param remapper the remapper to use to remap the types in the visited class.
      */
    public ClassRemapper(final ClassVisitor classVisitor, final Remapper remapper) {
        this(/* latest api = */ Opcodes.ASM8, classVisitor, remapper);
    }

    /**
      * Constructs a new {@link ClassRemapper}.
      *
      * @param api the ASM API version supported by this remapper. Must be one of {@link
      *     jdk.internal.org.objectweb.asm.Opcodes#ASM4}, {@link jdk.internal.org.objectweb.asm.Opcodes#ASM5}, {@link
      *     jdk.internal.org.objectweb.asm.Opcodes#ASM6}, {@link jdk.internal.org.objectweb.asm.Opcodes#ASM7} or {@link
      *     jdk.internal.org.objectweb.asm.Opcodes#ASM8}.
      * @param classVisitor the class visitor this remapper must deleted to.
      * @param remapper the remapper to use to remap the types in the visited class.
      */
    protected ClassRemapper(final int api, final ClassVisitor classVisitor, final Remapper remapper) {
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
        ModuleVisitor moduleVisitor = super.visitModule(remapper.mapModuleName(name), flags, version);
        return moduleVisitor == null ? null : createModuleRemapper(moduleVisitor);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        AnnotationVisitor annotationVisitor =
                super.visitAnnotation(remapper.mapDesc(descriptor), visible);
        return annotationVisitor == null ? null : createAnnotationRemapper(annotationVisitor);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        AnnotationVisitor annotationVisitor =
                super.visitTypeAnnotation(typeRef, typePath, remapper.mapDesc(descriptor), visible);
        return annotationVisitor == null ? null : createAnnotationRemapper(annotationVisitor);
    }

    @Override
    public void visitAttribute(final Attribute attribute) {
        if (attribute instanceof ModuleHashesAttribute) {
            ModuleHashesAttribute moduleHashesAttribute = (ModuleHashesAttribute) attribute;
            List<String> modules = moduleHashesAttribute.modules;
            for (int i = 0; i < modules.size(); ++i) {
                modules.set(i, remapper.mapModuleName(modules.get(i)));
            }
        }
        super.visitAttribute(attribute);
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(
            final String name, final String descriptor, final String signature) {
        RecordComponentVisitor recordComponentVisitor =
                super.visitRecordComponent(
                        remapper.mapRecordComponentName(className, name, descriptor),
                        remapper.mapDesc(descriptor),
                        remapper.mapSignature(signature, true));
        return recordComponentVisitor == null
                ? null
                : createRecordComponentRemapper(recordComponentVisitor);
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
                        (value == null) ? null : remapper.mapValue(value));
        return fieldVisitor == null ? null : createFieldRemapper(fieldVisitor);
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
        String remappedDescriptor = remapper.mapMethodDesc(descriptor);
        MethodVisitor methodVisitor =
                super.visitMethod(
                        access,
                        remapper.mapMethodName(className, name, descriptor),
                        remappedDescriptor,
                        remapper.mapSignature(signature, false),
                        exceptions == null ? null : remapper.mapTypes(exceptions));
        return methodVisitor == null ? null : createMethodRemapper(methodVisitor);
    }

    @Override
    public void visitInnerClass(
            final String name, final String outerName, final String innerName, final int access) {
        super.visitInnerClass(
                remapper.mapType(name),
                outerName == null ? null : remapper.mapType(outerName),
                innerName == null ? null : remapper.mapInnerClassName(name, outerName, innerName),
                access);
    }

    @Override
    public void visitOuterClass(final String owner, final String name, final String descriptor) {
        super.visitOuterClass(
                remapper.mapType(owner),
                name == null ? null : remapper.mapMethodName(owner, name, descriptor),
                descriptor == null ? null : remapper.mapMethodDesc(descriptor));
    }

    @Override
    public void visitNestHost(final String nestHost) {
        super.visitNestHost(remapper.mapType(nestHost));
    }

    @Override
    public void visitNestMember(final String nestMember) {
        super.visitNestMember(remapper.mapType(nestMember));
    }

    /**
      * <b>Experimental, use at your own risk.</b>.
      *
      * @param permittedSubclass the internal name of a permitted subclass.
      * @deprecated this API is experimental.
      */
    @Override
    @Deprecated
    public void visitPermittedSubclassExperimental(final String permittedSubclass) {
        super.visitPermittedSubclassExperimental(remapper.mapType(permittedSubclass));
    }

    /**
      * Constructs a new remapper for fields. The default implementation of this method returns a new
      * {@link FieldRemapper}.
      *
      * @param fieldVisitor the FieldVisitor the remapper must delegate to.
      * @return the newly created remapper.
      */
    protected FieldVisitor createFieldRemapper(final FieldVisitor fieldVisitor) {
        return new FieldRemapper(api, fieldVisitor, remapper);
    }

    /**
      * Constructs a new remapper for methods. The default implementation of this method returns a new
      * {@link MethodRemapper}.
      *
      * @param methodVisitor the MethodVisitor the remapper must delegate to.
      * @return the newly created remapper.
      */
    protected MethodVisitor createMethodRemapper(final MethodVisitor methodVisitor) {
        return new MethodRemapper(api, methodVisitor, remapper);
    }

    /**
      * Constructs a new remapper for annotations. The default implementation of this method returns a
      * new {@link AnnotationRemapper}.
      *
      * @param annotationVisitor the AnnotationVisitor the remapper must delegate to.
      * @return the newly created remapper.
      */
    protected AnnotationVisitor createAnnotationRemapper(final AnnotationVisitor annotationVisitor) {
        return new AnnotationRemapper(api, annotationVisitor, remapper);
    }

    /**
      * Constructs a new remapper for modules. The default implementation of this method returns a new
      * {@link ModuleRemapper}.
      *
      * @param moduleVisitor the ModuleVisitor the remapper must delegate to.
      * @return the newly created remapper.
      */
    protected ModuleVisitor createModuleRemapper(final ModuleVisitor moduleVisitor) {
        return new ModuleRemapper(api, moduleVisitor, remapper);
    }

    /**
      * Constructs a new remapper for record components. The default implementation of this method
      * returns a new {@link RecordComponentRemapper}.
      *
      * @param recordComponentVisitor the RecordComponentVisitor the remapper must delegate to.
      * @return the newly created remapper.
      */
    protected RecordComponentVisitor createRecordComponentRemapper(
            final RecordComponentVisitor recordComponentVisitor) {
        return new RecordComponentRemapper(api, recordComponentVisitor, remapper);
    }
}
