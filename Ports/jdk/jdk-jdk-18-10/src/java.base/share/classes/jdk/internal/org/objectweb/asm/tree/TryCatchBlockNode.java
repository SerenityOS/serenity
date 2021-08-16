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
import jdk.internal.org.objectweb.asm.MethodVisitor;

/**
 * A node that represents a try catch block.
 *
 * @author Eric Bruneton
 */
public class TryCatchBlockNode {

    /** The beginning of the exception handler's scope (inclusive). */
    public LabelNode start;

    /** The end of the exception handler's scope (exclusive). */
    public LabelNode end;

    /** The beginning of the exception handler's code. */
    public LabelNode handler;

    /**
      * The internal name of the type of exceptions handled by the handler. May be {@literal null} to
      * catch any exceptions (for "finally" blocks).
      */
    public String type;

    /** The runtime visible type annotations on the exception handler type. May be {@literal null}. */
    public List<TypeAnnotationNode> visibleTypeAnnotations;

    /**
      * The runtime invisible type annotations on the exception handler type. May be {@literal null}.
      */
    public List<TypeAnnotationNode> invisibleTypeAnnotations;

    /**
      * Constructs a new {@link TryCatchBlockNode}.
      *
      * @param start the beginning of the exception handler's scope (inclusive).
      * @param end the end of the exception handler's scope (exclusive).
      * @param handler the beginning of the exception handler's code.
      * @param type the internal name of the type of exceptions handled by the handler, or {@literal
      *     null} to catch any exceptions (for "finally" blocks).
      */
    public TryCatchBlockNode(
            final LabelNode start, final LabelNode end, final LabelNode handler, final String type) {
        this.start = start;
        this.end = end;
        this.handler = handler;
        this.type = type;
    }

    /**
      * Updates the index of this try catch block in the method's list of try catch block nodes. This
      * index maybe stored in the 'target' field of the type annotations of this block.
      *
      * @param index the new index of this try catch block in the method's list of try catch block
      *     nodes.
      */
    public void updateIndex(final int index) {
        int newTypeRef = 0x42000000 | (index << 8);
        if (visibleTypeAnnotations != null) {
            for (int i = 0, n = visibleTypeAnnotations.size(); i < n; ++i) {
                visibleTypeAnnotations.get(i).typeRef = newTypeRef;
            }
        }
        if (invisibleTypeAnnotations != null) {
            for (int i = 0, n = invisibleTypeAnnotations.size(); i < n; ++i) {
                invisibleTypeAnnotations.get(i).typeRef = newTypeRef;
            }
        }
    }

    /**
      * Makes the given visitor visit this try catch block.
      *
      * @param methodVisitor a method visitor.
      */
    public void accept(final MethodVisitor methodVisitor) {
        methodVisitor.visitTryCatchBlock(
                start.getLabel(), end.getLabel(), handler == null ? null : handler.getLabel(), type);
        if (visibleTypeAnnotations != null) {
            for (int i = 0, n = visibleTypeAnnotations.size(); i < n; ++i) {
                TypeAnnotationNode typeAnnotation = visibleTypeAnnotations.get(i);
                typeAnnotation.accept(
                        methodVisitor.visitTryCatchAnnotation(
                                typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, true));
            }
        }
        if (invisibleTypeAnnotations != null) {
            for (int i = 0, n = invisibleTypeAnnotations.size(); i < n; ++i) {
                TypeAnnotationNode typeAnnotation = invisibleTypeAnnotations.get(i);
                typeAnnotation.accept(
                        methodVisitor.visitTryCatchAnnotation(
                                typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, false));
            }
        }
    }
}
