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
package jdk.internal.org.objectweb.asm.signature;

import jdk.internal.org.objectweb.asm.Opcodes;

/**
 * A visitor to visit a generic signature. The methods of this interface must be called in one of
 * the three following orders (the last one is the only valid order for a {@link SignatureVisitor}
 * that is returned by a method of this interface):
 *
 * <ul>
 *   <li><i>ClassSignature</i> = ( {@code visitFormalTypeParameter} {@code visitClassBound}? {@code
 *       visitInterfaceBound}* )* ({@code visitSuperclass} {@code visitInterface}* )
 *   <li><i>MethodSignature</i> = ( {@code visitFormalTypeParameter} {@code visitClassBound}? {@code
 *       visitInterfaceBound}* )* ({@code visitParameterType}* {@code visitReturnType} {@code
 *       visitExceptionType}* )
 *   <li><i>TypeSignature</i> = {@code visitBaseType} | {@code visitTypeVariable} | {@code
 *       visitArrayType} | ( {@code visitClassType} {@code visitTypeArgument}* ( {@code
 *       visitInnerClassType} {@code visitTypeArgument}* )* {@code visitEnd} ) )
 * </ul>
 *
 * @author Thomas Hallgren
 * @author Eric Bruneton
 */
public abstract class SignatureVisitor {

    /** Wildcard for an "extends" type argument. */
    public static final char EXTENDS = '+';

    /** Wildcard for a "super" type argument. */
    public static final char SUPER = '-';

    /** Wildcard for a normal type argument. */
    public static final char INSTANCEOF = '=';

    /**
      * The ASM API version implemented by this visitor. The value of this field must be one of {@link
      * Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6} or {@link Opcodes#ASM7}.
      */
    protected final int api;

    /**
      * Constructs a new {@link SignatureVisitor}.
      *
      * @param api the ASM API version implemented by this visitor. Must be one of {@link
      *     Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6} or {@link Opcodes#ASM7}.
      */
    @SuppressWarnings("deprecation")
    public SignatureVisitor(final int api) {
        if (api != Opcodes.ASM8
                && api != Opcodes.ASM7
                && api != Opcodes.ASM6
                && api != Opcodes.ASM5
                && api != Opcodes.ASM4
                && api != Opcodes.ASM9_EXPERIMENTAL) {
            throw new IllegalArgumentException("Unsupported api " + api);
        }
        this.api = api;
    }

    /**
      * Visits a formal type parameter.
      *
      * @param name the name of the formal parameter.
      */
    public void visitFormalTypeParameter(final String name) {}

    /**
      * Visits the class bound of the last visited formal type parameter.
      *
      * @return a non null visitor to visit the signature of the class bound.
      */
    public SignatureVisitor visitClassBound() {
        return this;
    }

    /**
      * Visits an interface bound of the last visited formal type parameter.
      *
      * @return a non null visitor to visit the signature of the interface bound.
      */
    public SignatureVisitor visitInterfaceBound() {
        return this;
    }

    /**
      * Visits the type of the super class.
      *
      * @return a non null visitor to visit the signature of the super class type.
      */
    public SignatureVisitor visitSuperclass() {
        return this;
    }

    /**
      * Visits the type of an interface implemented by the class.
      *
      * @return a non null visitor to visit the signature of the interface type.
      */
    public SignatureVisitor visitInterface() {
        return this;
    }

    /**
      * Visits the type of a method parameter.
      *
      * @return a non null visitor to visit the signature of the parameter type.
      */
    public SignatureVisitor visitParameterType() {
        return this;
    }

    /**
      * Visits the return type of the method.
      *
      * @return a non null visitor to visit the signature of the return type.
      */
    public SignatureVisitor visitReturnType() {
        return this;
    }

    /**
      * Visits the type of a method exception.
      *
      * @return a non null visitor to visit the signature of the exception type.
      */
    public SignatureVisitor visitExceptionType() {
        return this;
    }

    /**
      * Visits a signature corresponding to a primitive type.
      *
      * @param descriptor the descriptor of the primitive type, or 'V' for {@code void} .
      */
    public void visitBaseType(final char descriptor) {}

    /**
      * Visits a signature corresponding to a type variable.
      *
      * @param name the name of the type variable.
      */
    public void visitTypeVariable(final String name) {}

    /**
      * Visits a signature corresponding to an array type.
      *
      * @return a non null visitor to visit the signature of the array element type.
      */
    public SignatureVisitor visitArrayType() {
        return this;
    }

    /**
      * Starts the visit of a signature corresponding to a class or interface type.
      *
      * @param name the internal name of the class or interface.
      */
    public void visitClassType(final String name) {}

    /**
      * Visits an inner class.
      *
      * @param name the local name of the inner class in its enclosing class.
      */
    public void visitInnerClassType(final String name) {}

    /** Visits an unbounded type argument of the last visited class or inner class type. */
    public void visitTypeArgument() {}

    /**
      * Visits a type argument of the last visited class or inner class type.
      *
      * @param wildcard '+', '-' or '='.
      * @return a non null visitor to visit the signature of the type argument.
      */
    public SignatureVisitor visitTypeArgument(final char wildcard) {
        return this;
    }

    /** Ends the visit of a signature corresponding to a class or interface type. */
    public void visitEnd() {}
}
