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
package jdk.internal.org.objectweb.asm.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.signature.SignatureVisitor;

/**
 * A {@link SignatureVisitor} that builds the Java generic type declaration corresponding to the
 * signature it visits.
 *
 * @author Eugene Kuleshov
 * @author Eric Bruneton
 */
public final class TraceSignatureVisitor extends SignatureVisitor {

    private static final String COMMA_SEPARATOR = ", ";
    private static final String EXTENDS_SEPARATOR = " extends ";
    private static final String IMPLEMENTS_SEPARATOR = " implements ";

    private static final Map<Character, String> BASE_TYPES;

    static {
        HashMap<Character, String> baseTypes = new HashMap<>();
        baseTypes.put('Z', "boolean");
        baseTypes.put('B', "byte");
        baseTypes.put('C', "char");
        baseTypes.put('S', "short");
        baseTypes.put('I', "int");
        baseTypes.put('J', "long");
        baseTypes.put('F', "float");
        baseTypes.put('D', "double");
        baseTypes.put('V', "void");
        BASE_TYPES = Collections.unmodifiableMap(baseTypes);
    }

    /** Whether the visited signature is a class signature of a Java interface. */
    private final boolean isInterface;

    /** The Java generic type declaration corresponding to the visited signature. */
    private final StringBuilder declaration;

    /** The Java generic method return type declaration corresponding to the visited signature. */
    private StringBuilder returnType;

    /** The Java generic exception types declaration corresponding to the visited signature. */
    private StringBuilder exceptions;

    /** Whether {@link #visitFormalTypeParameter} has been called. */
    private boolean formalTypeParameterVisited;

    /** Whether {@link #visitInterfaceBound} has been called. */
    private boolean interfaceBoundVisited;

    /** Whether {@link #visitParameterType} has been called. */
    private boolean parameterTypeVisited;

    /** Whether {@link #visitInterface} has been called. */
    private boolean interfaceVisited;

    /**
      * The stack used to keep track of class types that have arguments. Each element of this stack is
      * a boolean encoded in one bit. The top of the stack is the least significant bit. Pushing false
      * = *2, pushing true = *2+1, popping = /2.
      */
    private int argumentStack;

    /**
      * The stack used to keep track of array class types. Each element of this stack is a boolean
      * encoded in one bit. The top of the stack is the lowest order bit. Pushing false = *2, pushing
      * true = *2+1, popping = /2.
      */
    private int arrayStack;

    /** The separator to append before the next visited class or inner class type. */
    private String separator = "";

    /**
      * Constructs a new {@link TraceSignatureVisitor}.
      *
      * @param accessFlags for class type signatures, the access flags of the class.
      */
    public TraceSignatureVisitor(final int accessFlags) {
        super(/* latest api = */ Opcodes.ASM8);
        this.isInterface = (accessFlags & Opcodes.ACC_INTERFACE) != 0;
        this.declaration = new StringBuilder();
    }

    private TraceSignatureVisitor(final StringBuilder stringBuilder) {
        super(/* latest api = */ Opcodes.ASM8);
        this.isInterface = false;
        this.declaration = stringBuilder;
    }

    @Override
    public void visitFormalTypeParameter(final String name) {
        declaration.append(formalTypeParameterVisited ? COMMA_SEPARATOR : "<").append(name);
        formalTypeParameterVisited = true;
        interfaceBoundVisited = false;
    }

    @Override
    public SignatureVisitor visitClassBound() {
        separator = EXTENDS_SEPARATOR;
        startType();
        return this;
    }

    @Override
    public SignatureVisitor visitInterfaceBound() {
        separator = interfaceBoundVisited ? COMMA_SEPARATOR : EXTENDS_SEPARATOR;
        interfaceBoundVisited = true;
        startType();
        return this;
    }

    @Override
    public SignatureVisitor visitSuperclass() {
        endFormals();
        separator = EXTENDS_SEPARATOR;
        startType();
        return this;
    }

    @Override
    public SignatureVisitor visitInterface() {
        if (interfaceVisited) {
            separator = COMMA_SEPARATOR;
        } else {
            separator = isInterface ? EXTENDS_SEPARATOR : IMPLEMENTS_SEPARATOR;
            interfaceVisited = true;
        }
        startType();
        return this;
    }

    @Override
    public SignatureVisitor visitParameterType() {
        endFormals();
        if (parameterTypeVisited) {
            declaration.append(COMMA_SEPARATOR);
        } else {
            declaration.append('(');
            parameterTypeVisited = true;
        }
        startType();
        return this;
    }

    @Override
    public SignatureVisitor visitReturnType() {
        endFormals();
        if (parameterTypeVisited) {
            parameterTypeVisited = false;
        } else {
            declaration.append('(');
        }
        declaration.append(')');
        returnType = new StringBuilder();
        return new TraceSignatureVisitor(returnType);
    }

    @Override
    public SignatureVisitor visitExceptionType() {
        if (exceptions == null) {
            exceptions = new StringBuilder();
        } else {
            exceptions.append(COMMA_SEPARATOR);
        }
        return new TraceSignatureVisitor(exceptions);
    }

    @Override
    public void visitBaseType(final char descriptor) {
        String baseType = BASE_TYPES.get(descriptor);
        if (baseType == null) {
            throw new IllegalArgumentException();
        }
        declaration.append(baseType);
        endType();
    }

    @Override
    public void visitTypeVariable(final String name) {
        declaration.append(separator).append(name);
        separator = "";
        endType();
    }

    @Override
    public SignatureVisitor visitArrayType() {
        startType();
        arrayStack |= 1;
        return this;
    }

    @Override
    public void visitClassType(final String name) {
        if ("java/lang/Object".equals(name)) {
            // 'Map<java.lang.Object,java.util.List>' or 'public abstract V get(Object key);' should have
            // Object 'but java.lang.String extends java.lang.Object' is unnecessary.
            boolean needObjectClass = argumentStack % 2 != 0 || parameterTypeVisited;
            if (needObjectClass) {
                declaration.append(separator).append(name.replace('/', '.'));
            }
        } else {
            declaration.append(separator).append(name.replace('/', '.'));
        }
        separator = "";
        argumentStack *= 2;
    }

    @Override
    public void visitInnerClassType(final String name) {
        if (argumentStack % 2 != 0) {
            declaration.append('>');
        }
        argumentStack /= 2;
        declaration.append('.');
        declaration.append(separator).append(name.replace('/', '.'));
        separator = "";
        argumentStack *= 2;
    }

    @Override
    public void visitTypeArgument() {
        if (argumentStack % 2 == 0) {
            ++argumentStack;
            declaration.append('<');
        } else {
            declaration.append(COMMA_SEPARATOR);
        }
        declaration.append('?');
    }

    @Override
    public SignatureVisitor visitTypeArgument(final char tag) {
        if (argumentStack % 2 == 0) {
            ++argumentStack;
            declaration.append('<');
        } else {
            declaration.append(COMMA_SEPARATOR);
        }

        if (tag == EXTENDS) {
            declaration.append("? extends ");
        } else if (tag == SUPER) {
            declaration.append("? super ");
        }

        startType();
        return this;
    }

    @Override
    public void visitEnd() {
        if (argumentStack % 2 != 0) {
            declaration.append('>');
        }
        argumentStack /= 2;
        endType();
    }

    // -----------------------------------------------------------------------------------------------

    /**
      * Returns the Java generic type declaration corresponding to the visited signature.
      *
      * @return the Java generic type declaration corresponding to the visited signature.
      */
    public String getDeclaration() {
        return declaration.toString();
    }

    /**
      * Returns the Java generic method return type declaration corresponding to the visited signature.
      *
      * @return the Java generic method return type declaration corresponding to the visited signature.
      */
    public String getReturnType() {
        return returnType == null ? null : returnType.toString();
    }

    /**
      * Returns the Java generic exception types declaration corresponding to the visited signature.
      *
      * @return the Java generic exception types declaration corresponding to the visited signature.
      */
    public String getExceptions() {
        return exceptions == null ? null : exceptions.toString();
    }

    // -----------------------------------------------------------------------------------------------

    private void endFormals() {
        if (formalTypeParameterVisited) {
            declaration.append('>');
            formalTypeParameterVisited = false;
        }
    }

    private void startType() {
        arrayStack *= 2;
    }

    private void endType() {
        if (arrayStack % 2 == 0) {
            arrayStack /= 2;
        } else {
            while (arrayStack % 2 != 0) {
                arrayStack /= 2;
                declaration.append("[]");
            }
        }
    }
}
