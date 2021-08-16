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

import java.util.ArrayList;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.signature.SignatureVisitor;

/**
 * A {@link SignatureVisitor} that remaps types with a {@link Remapper}.
 *
 * @author Eugene Kuleshov
 */
public class SignatureRemapper extends SignatureVisitor {

    private final SignatureVisitor signatureVisitor;

    private final Remapper remapper;

    private ArrayList<String> classNames = new ArrayList<>();

    /**
      * Constructs a new {@link SignatureRemapper}. <i>Subclasses must not use this constructor</i>.
      * Instead, they must use the {@link #SignatureRemapper(int,SignatureVisitor,Remapper)} version.
      *
      * @param signatureVisitor the signature visitor this remapper must deleted to.
      * @param remapper the remapper to use to remap the types in the visited signature.
      */
    public SignatureRemapper(final SignatureVisitor signatureVisitor, final Remapper remapper) {
        this(/* latest api = */ Opcodes.ASM8, signatureVisitor, remapper);
    }

    /**
      * Constructs a new {@link SignatureRemapper}.
      *
      * @param api the ASM API version supported by this remapper. Must be one of {@link
      *     jdk.internal.org.objectweb.asm.Opcodes#ASM4}, {@link jdk.internal.org.objectweb.asm.Opcodes#ASM5},{@link
      *     jdk.internal.org.objectweb.asm.Opcodes#ASM6}, {@link jdk.internal.org.objectweb.asm.Opcodes#ASM7} or {@link
      *     jdk.internal.org.objectweb.asm.Opcodes#ASM8}.
      * @param signatureVisitor the signature visitor this remapper must deleted to.
      * @param remapper the remapper to use to remap the types in the visited signature.
      */
    protected SignatureRemapper(
            final int api, final SignatureVisitor signatureVisitor, final Remapper remapper) {
        super(api);
        this.signatureVisitor = signatureVisitor;
        this.remapper = remapper;
    }

    @Override
    public void visitClassType(final String name) {
        classNames.add(name);
        signatureVisitor.visitClassType(remapper.mapType(name));
    }

    @Override
    public void visitInnerClassType(final String name) {
        String outerClassName = classNames.remove(classNames.size() - 1);
        String className = outerClassName + '$' + name;
        classNames.add(className);
        String remappedOuter = remapper.mapType(outerClassName) + '$';
        String remappedName = remapper.mapType(className);
        int index =
                remappedName.startsWith(remappedOuter)
                        ? remappedOuter.length()
                        : remappedName.lastIndexOf('$') + 1;
        signatureVisitor.visitInnerClassType(remappedName.substring(index));
    }

    @Override
    public void visitFormalTypeParameter(final String name) {
        signatureVisitor.visitFormalTypeParameter(name);
    }

    @Override
    public void visitTypeVariable(final String name) {
        signatureVisitor.visitTypeVariable(name);
    }

    @Override
    public SignatureVisitor visitArrayType() {
        signatureVisitor.visitArrayType();
        return this;
    }

    @Override
    public void visitBaseType(final char descriptor) {
        signatureVisitor.visitBaseType(descriptor);
    }

    @Override
    public SignatureVisitor visitClassBound() {
        signatureVisitor.visitClassBound();
        return this;
    }

    @Override
    public SignatureVisitor visitExceptionType() {
        signatureVisitor.visitExceptionType();
        return this;
    }

    @Override
    public SignatureVisitor visitInterface() {
        signatureVisitor.visitInterface();
        return this;
    }

    @Override
    public SignatureVisitor visitInterfaceBound() {
        signatureVisitor.visitInterfaceBound();
        return this;
    }

    @Override
    public SignatureVisitor visitParameterType() {
        signatureVisitor.visitParameterType();
        return this;
    }

    @Override
    public SignatureVisitor visitReturnType() {
        signatureVisitor.visitReturnType();
        return this;
    }

    @Override
    public SignatureVisitor visitSuperclass() {
        signatureVisitor.visitSuperclass();
        return this;
    }

    @Override
    public void visitTypeArgument() {
        signatureVisitor.visitTypeArgument();
    }

    @Override
    public SignatureVisitor visitTypeArgument(final char wildcard) {
        signatureVisitor.visitTypeArgument(wildcard);
        return this;
    }

    @Override
    public void visitEnd() {
        signatureVisitor.visitEnd();
        classNames.remove(classNames.size() - 1);
    }
}
