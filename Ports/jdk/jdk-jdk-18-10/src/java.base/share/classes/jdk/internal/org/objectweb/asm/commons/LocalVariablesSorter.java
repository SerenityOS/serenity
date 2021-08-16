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
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.TypePath;

/**
 * A {@link MethodVisitor} that renumbers local variables in their order of appearance. This adapter
 * allows one to easily add new local variables to a method. It may be used by inheriting from this
 * class, but the preferred way of using it is via delegation: the next visitor in the chain can
 * indeed add new locals when needed by calling {@link #newLocal} on this adapter (this requires a
 * reference back to this {@link LocalVariablesSorter}).
 *
 * @author Chris Nokleberg
 * @author Eugene Kuleshov
 * @author Eric Bruneton
 */
public class LocalVariablesSorter extends MethodVisitor {

    /** The type of the java.lang.Object class. */
    private static final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");

    /**
      * The mapping from old to new local variable indices. A local variable at index i of size 1 is
      * remapped to 'mapping[2*i]', while a local variable at index i of size 2 is remapped to
      * 'mapping[2*i+1]'.
      */
    private int[] remappedVariableIndices = new int[40];

    /**
      * The local variable types after remapping. The format of this array is the same as in {@link
      * MethodVisitor#visitFrame}, except that long and double types use two slots.
      */
    private Object[] remappedLocalTypes = new Object[20];

    /** The index of the first local variable, after formal parameters. */
    protected final int firstLocal;

    /** The index of the next local variable to be created by {@link #newLocal}. */
    protected int nextLocal;

    /**
      * Constructs a new {@link LocalVariablesSorter}. <i>Subclasses must not use this constructor</i>.
      * Instead, they must use the {@link #LocalVariablesSorter(int, int, String, MethodVisitor)}
      * version.
      *
      * @param access access flags of the adapted method.
      * @param descriptor the method's descriptor (see {@link Type}).
      * @param methodVisitor the method visitor to which this adapter delegates calls.
      * @throws IllegalStateException if a subclass calls this constructor.
      */
    public LocalVariablesSorter(
            final int access, final String descriptor, final MethodVisitor methodVisitor) {
        this(/* latest api = */ Opcodes.ASM8, access, descriptor, methodVisitor);
        if (getClass() != LocalVariablesSorter.class) {
            throw new IllegalStateException();
        }
    }

    /**
      * Constructs a new {@link LocalVariablesSorter}.
      *
      * @param api the ASM API version implemented by this visitor. Must be one of {@link
      *     Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}, {@link Opcodes#ASM7} or {@link
      *     Opcodes#ASM8}.
      * @param access access flags of the adapted method.
      * @param descriptor the method's descriptor (see {@link Type}).
      * @param methodVisitor the method visitor to which this adapter delegates calls.
      */
    protected LocalVariablesSorter(
            final int api, final int access, final String descriptor, final MethodVisitor methodVisitor) {
        super(api, methodVisitor);
        nextLocal = (Opcodes.ACC_STATIC & access) == 0 ? 1 : 0;
        for (Type argumentType : Type.getArgumentTypes(descriptor)) {
            nextLocal += argumentType.getSize();
        }
        firstLocal = nextLocal;
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        Type varType;
        switch (opcode) {
            case Opcodes.LLOAD:
            case Opcodes.LSTORE:
                varType = Type.LONG_TYPE;
                break;
            case Opcodes.DLOAD:
            case Opcodes.DSTORE:
                varType = Type.DOUBLE_TYPE;
                break;
            case Opcodes.FLOAD:
            case Opcodes.FSTORE:
                varType = Type.FLOAT_TYPE;
                break;
            case Opcodes.ILOAD:
            case Opcodes.ISTORE:
                varType = Type.INT_TYPE;
                break;
            case Opcodes.ALOAD:
            case Opcodes.ASTORE:
            case Opcodes.RET:
                varType = OBJECT_TYPE;
                break;
            default:
                throw new IllegalArgumentException("Invalid opcode " + opcode);
        }
        super.visitVarInsn(opcode, remap(var, varType));
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        super.visitIincInsn(remap(var, Type.INT_TYPE), increment);
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        super.visitMaxs(maxStack, nextLocal);
    }

    @Override
    public void visitLocalVariable(
            final String name,
            final String descriptor,
            final String signature,
            final Label start,
            final Label end,
            final int index) {
        int remappedIndex = remap(index, Type.getType(descriptor));
        super.visitLocalVariable(name, descriptor, signature, start, end, remappedIndex);
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
        Type type = Type.getType(descriptor);
        int[] remappedIndex = new int[index.length];
        for (int i = 0; i < remappedIndex.length; ++i) {
            remappedIndex[i] = remap(index[i], type);
        }
        return super.visitLocalVariableAnnotation(
                typeRef, typePath, start, end, remappedIndex, descriptor, visible);
    }

    @Override
    public void visitFrame(
            final int type,
            final int numLocal,
            final Object[] local,
            final int numStack,
            final Object[] stack) {
        if (type != Opcodes.F_NEW) { // Uncompressed frame.
            throw new IllegalArgumentException(
                    "LocalVariablesSorter only accepts expanded frames (see ClassReader.EXPAND_FRAMES)");
        }

        // Create a copy of remappedLocals.
        Object[] oldRemappedLocals = new Object[remappedLocalTypes.length];
        System.arraycopy(remappedLocalTypes, 0, oldRemappedLocals, 0, oldRemappedLocals.length);

        updateNewLocals(remappedLocalTypes);

        // Copy the types from 'local' to 'remappedLocals'. 'remappedLocals' already contains the
        // variables added with 'newLocal'.
        int oldVar = 0; // Old local variable index.
        for (int i = 0; i < numLocal; ++i) {
            Object localType = local[i];
            if (localType != Opcodes.TOP) {
                Type varType = OBJECT_TYPE;
                if (localType == Opcodes.INTEGER) {
                    varType = Type.INT_TYPE;
                } else if (localType == Opcodes.FLOAT) {
                    varType = Type.FLOAT_TYPE;
                } else if (localType == Opcodes.LONG) {
                    varType = Type.LONG_TYPE;
                } else if (localType == Opcodes.DOUBLE) {
                    varType = Type.DOUBLE_TYPE;
                } else if (localType instanceof String) {
                    varType = Type.getObjectType((String) localType);
                }
                setFrameLocal(remap(oldVar, varType), localType);
            }
            oldVar += localType == Opcodes.LONG || localType == Opcodes.DOUBLE ? 2 : 1;
        }

        // Remove TOP after long and double types as well as trailing TOPs.
        oldVar = 0;
        int newVar = 0;
        int remappedNumLocal = 0;
        while (oldVar < remappedLocalTypes.length) {
            Object localType = remappedLocalTypes[oldVar];
            oldVar += localType == Opcodes.LONG || localType == Opcodes.DOUBLE ? 2 : 1;
            if (localType != null && localType != Opcodes.TOP) {
                remappedLocalTypes[newVar++] = localType;
                remappedNumLocal = newVar;
            } else {
                remappedLocalTypes[newVar++] = Opcodes.TOP;
            }
        }

        // Visit the remapped frame.
        super.visitFrame(type, remappedNumLocal, remappedLocalTypes, numStack, stack);

        // Restore the original value of 'remappedLocals'.
        remappedLocalTypes = oldRemappedLocals;
    }

    // -----------------------------------------------------------------------------------------------

    /**
      * Constructs a new local variable of the given type.
      *
      * @param type the type of the local variable to be created.
      * @return the identifier of the newly created local variable.
      */
    public int newLocal(final Type type) {
        Object localType;
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                localType = Opcodes.INTEGER;
                break;
            case Type.FLOAT:
                localType = Opcodes.FLOAT;
                break;
            case Type.LONG:
                localType = Opcodes.LONG;
                break;
            case Type.DOUBLE:
                localType = Opcodes.DOUBLE;
                break;
            case Type.ARRAY:
                localType = type.getDescriptor();
                break;
            case Type.OBJECT:
                localType = type.getInternalName();
                break;
            default:
                throw new AssertionError();
        }
        int local = newLocalMapping(type);
        setLocalType(local, type);
        setFrameLocal(local, localType);
        return local;
    }

    /**
      * Notifies subclasses that a new stack map frame is being visited. The array argument contains
      * the stack map frame types corresponding to the local variables added with {@link #newLocal}.
      * This method can update these types in place for the stack map frame being visited. The default
      * implementation of this method does nothing, i.e. a local variable added with {@link #newLocal}
      * will have the same type in all stack map frames. But this behavior is not always the desired
      * one, for instance if a local variable is added in the middle of a try/catch block: the frame
      * for the exception handler should have a TOP type for this new local.
      *
      * @param newLocals the stack map frame types corresponding to the local variables added with
      *     {@link #newLocal} (and null for the others). The format of this array is the same as in
      *     {@link MethodVisitor#visitFrame}, except that long and double types use two slots. The
      *     types for the current stack map frame must be updated in place in this array.
      */
    protected void updateNewLocals(final Object[] newLocals) {
        // The default implementation does nothing.
    }

    /**
      * Notifies subclasses that a local variable has been added or remapped. The default
      * implementation of this method does nothing.
      *
      * @param local a local variable identifier, as returned by {@link #newLocal}.
      * @param type the type of the value being stored in the local variable.
      */
    protected void setLocalType(final int local, final Type type) {
        // The default implementation does nothing.
    }

    private void setFrameLocal(final int local, final Object type) {
        int numLocals = remappedLocalTypes.length;
        if (local >= numLocals) {
            Object[] newRemappedLocalTypes = new Object[Math.max(2 * numLocals, local + 1)];
            System.arraycopy(remappedLocalTypes, 0, newRemappedLocalTypes, 0, numLocals);
            remappedLocalTypes = newRemappedLocalTypes;
        }
        remappedLocalTypes[local] = type;
    }

    private int remap(final int var, final Type type) {
        if (var + type.getSize() <= firstLocal) {
            return var;
        }
        int key = 2 * var + type.getSize() - 1;
        int size = remappedVariableIndices.length;
        if (key >= size) {
            int[] newRemappedVariableIndices = new int[Math.max(2 * size, key + 1)];
            System.arraycopy(remappedVariableIndices, 0, newRemappedVariableIndices, 0, size);
            remappedVariableIndices = newRemappedVariableIndices;
        }
        int value = remappedVariableIndices[key];
        if (value == 0) {
            value = newLocalMapping(type);
            setLocalType(value, type);
            remappedVariableIndices[key] = value + 1;
        } else {
            value--;
        }
        return value;
    }

    protected int newLocalMapping(final Type type) {
        int local = nextLocal;
        nextLocal += type.getSize();
        return local;
    }
}
