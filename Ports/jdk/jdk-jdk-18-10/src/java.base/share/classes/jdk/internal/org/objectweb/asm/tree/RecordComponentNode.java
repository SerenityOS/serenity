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
package jdk.internal.org.objectweb.asm.tree;

import java.util.List;
import jdk.internal.org.objectweb.asm.AnnotationVisitor;
import jdk.internal.org.objectweb.asm.Attribute;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.RecordComponentVisitor;
import jdk.internal.org.objectweb.asm.TypePath;

/**
 * A node that represents a record component.
 *
 * @author Remi Forax
 */
public class RecordComponentNode extends RecordComponentVisitor {

    /** The record component name. */
    public String name;

    /** The record component descriptor (see {@link jdk.internal.org.objectweb.asm.Type}). */
    public String descriptor;

    /** The record component signature. May be {@literal null}. */
    public String signature;

    /** The runtime visible annotations of this record component. May be {@literal null}. */
    public List<AnnotationNode> visibleAnnotations;

    /** The runtime invisible annotations of this record component. May be {@literal null}. */
    public List<AnnotationNode> invisibleAnnotations;

    /** The runtime visible type annotations of this record component. May be {@literal null}. */
    public List<TypeAnnotationNode> visibleTypeAnnotations;

    /** The runtime invisible type annotations of this record component. May be {@literal null}. */
    public List<TypeAnnotationNode> invisibleTypeAnnotations;

    /** The non standard attributes of this record component. * May be {@literal null}. */
    public List<Attribute> attrs;

    /**
      * Constructs a new {@link RecordComponentNode}. <i>Subclasses must not use this constructor</i>.
      * Instead, they must use the {@link #RecordComponentNode(int, String, String, String)} version.
      *
      * @param name the record component name.
      * @param descriptor the record component descriptor (see {@link jdk.internal.org.objectweb.asm.Type}).
      * @param signature the record component signature.
      * @throws IllegalStateException If a subclass calls this constructor.
      */
    public RecordComponentNode(final String name, final String descriptor, final String signature) {
        this(/* latest api = */ Opcodes.ASM8, name, descriptor, signature);
        if (getClass() != RecordComponentNode.class) {
            throw new IllegalStateException();
        }
    }

    /**
      * Constructs a new {@link RecordComponentNode}.
      *
      * @param api the ASM API version implemented by this visitor. Must be {@link Opcodes#ASM8}.
      * @param name the record component name.
      * @param descriptor the record component descriptor (see {@link jdk.internal.org.objectweb.asm.Type}).
      * @param signature the record component signature.
      */
    public RecordComponentNode(
            final int api, final String name, final String descriptor, final String signature) {
        super(api);
        this.name = name;
        this.descriptor = descriptor;
        this.signature = signature;
    }

    // -----------------------------------------------------------------------------------------------
    // Implementation of the FieldVisitor abstract class
    // -----------------------------------------------------------------------------------------------

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        AnnotationNode annotation = new AnnotationNode(descriptor);
        if (visible) {
            visibleAnnotations = Util.add(visibleAnnotations, annotation);
        } else {
            invisibleAnnotations = Util.add(invisibleAnnotations, annotation);
        }
        return annotation;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        TypeAnnotationNode typeAnnotation = new TypeAnnotationNode(typeRef, typePath, descriptor);
        if (visible) {
            visibleTypeAnnotations = Util.add(visibleTypeAnnotations, typeAnnotation);
        } else {
            invisibleTypeAnnotations = Util.add(invisibleTypeAnnotations, typeAnnotation);
        }
        return typeAnnotation;
    }

    @Override
    public void visitAttribute(final Attribute attribute) {
        attrs = Util.add(attrs, attribute);
    }

    @Override
    public void visitEnd() {
        // Nothing to do.
    }

    // -----------------------------------------------------------------------------------------------
    // Accept methods
    // -----------------------------------------------------------------------------------------------

    /**
      * Checks that this record component node is compatible with the given ASM API version. This
      * method checks that this node, and all its children recursively, do not contain elements that
      * were introduced in more recent versions of the ASM API than the given version.
      *
      * @param api an ASM API version. Must be {@link Opcodes#ASM8}.
      */
    public void check(final int api) {
        if (api < Opcodes.ASM8) {
            throw new UnsupportedClassVersionException();
        }
    }

    /**
      * Makes the given class visitor visit this record component.
      *
      * @param classVisitor a class visitor.
      */
    public void accept(final ClassVisitor classVisitor) {
        RecordComponentVisitor recordComponentVisitor =
                classVisitor.visitRecordComponent(name, descriptor, signature);
        if (recordComponentVisitor == null) {
            return;
        }
        // Visit the annotations.
        if (visibleAnnotations != null) {
            for (int i = 0, n = visibleAnnotations.size(); i < n; ++i) {
                AnnotationNode annotation = visibleAnnotations.get(i);
                annotation.accept(recordComponentVisitor.visitAnnotation(annotation.desc, true));
            }
        }
        if (invisibleAnnotations != null) {
            for (int i = 0, n = invisibleAnnotations.size(); i < n; ++i) {
                AnnotationNode annotation = invisibleAnnotations.get(i);
                annotation.accept(recordComponentVisitor.visitAnnotation(annotation.desc, false));
            }
        }
        if (visibleTypeAnnotations != null) {
            for (int i = 0, n = visibleTypeAnnotations.size(); i < n; ++i) {
                TypeAnnotationNode typeAnnotation = visibleTypeAnnotations.get(i);
                typeAnnotation.accept(
                        recordComponentVisitor.visitTypeAnnotation(
                                typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, true));
            }
        }
        if (invisibleTypeAnnotations != null) {
            for (int i = 0, n = invisibleTypeAnnotations.size(); i < n; ++i) {
                TypeAnnotationNode typeAnnotation = invisibleTypeAnnotations.get(i);
                typeAnnotation.accept(
                        recordComponentVisitor.visitTypeAnnotation(
                                typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, false));
            }
        }
        // Visit the non standard attributes.
        if (attrs != null) {
            for (int i = 0, n = attrs.size(); i < n; ++i) {
                recordComponentVisitor.visitAttribute(attrs.get(i));
            }
        }
        recordComponentVisitor.visitEnd();
    }
}
