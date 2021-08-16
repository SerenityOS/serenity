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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jdk.internal.org.objectweb.asm.AnnotationVisitor;
import jdk.internal.org.objectweb.asm.Attribute;
import jdk.internal.org.objectweb.asm.ConstantDynamic;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.TypePath;
import jdk.internal.org.objectweb.asm.TypeReference;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.analysis.Analyzer;
import jdk.internal.org.objectweb.asm.tree.analysis.AnalyzerException;
import jdk.internal.org.objectweb.asm.tree.analysis.BasicValue;
import jdk.internal.org.objectweb.asm.tree.analysis.BasicVerifier;

/**
 * A {@link MethodVisitor} that checks that its methods are properly used. More precisely this
 * method adapter checks each instruction individually, i.e., each visit method checks some
 * preconditions based <i>only</i> on its arguments - such as the fact that the given opcode is
 * correct for a given visit method. This adapter can also perform some basic data flow checks (more
 * precisely those that can be performed without the full class hierarchy - see {@link
 * jdk.internal.org.objectweb.asm.tree.analysis.BasicVerifier}). For instance in a method whose signature is
 * {@code void m ()}, the invalid instruction IRETURN, or the invalid sequence IADD L2I will be
 * detected if the data flow checks are enabled. These checks are enabled by using the {@link
 * #CheckMethodAdapter(int,String,String,MethodVisitor,Map)} constructor. They are not performed if
 * any other constructor is used.
 *
 * @author Eric Bruneton
 */
public class CheckMethodAdapter extends MethodVisitor {

    /** The 'generic' instruction visit methods (i.e. those that take an opcode argument). */
    private enum Method {
        VISIT_INSN,
        VISIT_INT_INSN,
        VISIT_VAR_INSN,
        VISIT_TYPE_INSN,
        VISIT_FIELD_INSN,
        VISIT_METHOD_INSN,
        VISIT_JUMP_INSN
    }

    /** The method to use to visit each instruction. Only generic methods are represented here. */
    private static final Method[] OPCODE_METHODS = {
        Method.VISIT_INSN, // NOP
        Method.VISIT_INSN, // ACONST_NULL
        Method.VISIT_INSN, // ICONST_M1
        Method.VISIT_INSN, // ICONST_0
        Method.VISIT_INSN, // ICONST_1
        Method.VISIT_INSN, // ICONST_2
        Method.VISIT_INSN, // ICONST_3
        Method.VISIT_INSN, // ICONST_4
        Method.VISIT_INSN, // ICONST_5
        Method.VISIT_INSN, // LCONST_0
        Method.VISIT_INSN, // LCONST_1
        Method.VISIT_INSN, // FCONST_0
        Method.VISIT_INSN, // FCONST_1
        Method.VISIT_INSN, // FCONST_2
        Method.VISIT_INSN, // DCONST_0
        Method.VISIT_INSN, // DCONST_1
        Method.VISIT_INT_INSN, // BIPUSH
        Method.VISIT_INT_INSN, // SIPUSH
        null, // LDC
        null, // LDC_W
        null, // LDC2_W
        Method.VISIT_VAR_INSN, // ILOAD
        Method.VISIT_VAR_INSN, // LLOAD
        Method.VISIT_VAR_INSN, // FLOAD
        Method.VISIT_VAR_INSN, // DLOAD
        Method.VISIT_VAR_INSN, // ALOAD
        null, // ILOAD_0
        null, // ILOAD_1
        null, // ILOAD_2
        null, // ILOAD_3
        null, // LLOAD_0
        null, // LLOAD_1
        null, // LLOAD_2
        null, // LLOAD_3
        null, // FLOAD_0
        null, // FLOAD_1
        null, // FLOAD_2
        null, // FLOAD_3
        null, // DLOAD_0
        null, // DLOAD_1
        null, // DLOAD_2
        null, // DLOAD_3
        null, // ALOAD_0
        null, // ALOAD_1
        null, // ALOAD_2
        null, // ALOAD_3
        Method.VISIT_INSN, // IALOAD
        Method.VISIT_INSN, // LALOAD
        Method.VISIT_INSN, // FALOAD
        Method.VISIT_INSN, // DALOAD
        Method.VISIT_INSN, // AALOAD
        Method.VISIT_INSN, // BALOAD
        Method.VISIT_INSN, // CALOAD
        Method.VISIT_INSN, // SALOAD
        Method.VISIT_VAR_INSN, // ISTORE
        Method.VISIT_VAR_INSN, // LSTORE
        Method.VISIT_VAR_INSN, // FSTORE
        Method.VISIT_VAR_INSN, // DSTORE
        Method.VISIT_VAR_INSN, // ASTORE
        null, // ISTORE_0
        null, // ISTORE_1
        null, // ISTORE_2
        null, // ISTORE_3
        null, // LSTORE_0
        null, // LSTORE_1
        null, // LSTORE_2
        null, // LSTORE_3
        null, // FSTORE_0
        null, // FSTORE_1
        null, // FSTORE_2
        null, // FSTORE_3
        null, // DSTORE_0
        null, // DSTORE_1
        null, // DSTORE_2
        null, // DSTORE_3
        null, // ASTORE_0
        null, // ASTORE_1
        null, // ASTORE_2
        null, // ASTORE_3
        Method.VISIT_INSN, // IASTORE
        Method.VISIT_INSN, // LASTORE
        Method.VISIT_INSN, // FASTORE
        Method.VISIT_INSN, // DASTORE
        Method.VISIT_INSN, // AASTORE
        Method.VISIT_INSN, // BASTORE
        Method.VISIT_INSN, // CASTORE
        Method.VISIT_INSN, // SASTORE
        Method.VISIT_INSN, // POP
        Method.VISIT_INSN, // POP2
        Method.VISIT_INSN, // DUP
        Method.VISIT_INSN, // DUP_X1
        Method.VISIT_INSN, // DUP_X2
        Method.VISIT_INSN, // DUP2
        Method.VISIT_INSN, // DUP2_X1
        Method.VISIT_INSN, // DUP2_X2
        Method.VISIT_INSN, // SWAP
        Method.VISIT_INSN, // IADD
        Method.VISIT_INSN, // LADD
        Method.VISIT_INSN, // FADD
        Method.VISIT_INSN, // DADD
        Method.VISIT_INSN, // ISUB
        Method.VISIT_INSN, // LSUB
        Method.VISIT_INSN, // FSUB
        Method.VISIT_INSN, // DSUB
        Method.VISIT_INSN, // IMUL
        Method.VISIT_INSN, // LMUL
        Method.VISIT_INSN, // FMUL
        Method.VISIT_INSN, // DMUL
        Method.VISIT_INSN, // IDIV
        Method.VISIT_INSN, // LDIV
        Method.VISIT_INSN, // FDIV
        Method.VISIT_INSN, // DDIV
        Method.VISIT_INSN, // IREM
        Method.VISIT_INSN, // LREM
        Method.VISIT_INSN, // FREM
        Method.VISIT_INSN, // DREM
        Method.VISIT_INSN, // INEG
        Method.VISIT_INSN, // LNEG
        Method.VISIT_INSN, // FNEG
        Method.VISIT_INSN, // DNEG
        Method.VISIT_INSN, // ISHL
        Method.VISIT_INSN, // LSHL
        Method.VISIT_INSN, // ISHR
        Method.VISIT_INSN, // LSHR
        Method.VISIT_INSN, // IUSHR
        Method.VISIT_INSN, // LUSHR
        Method.VISIT_INSN, // IAND
        Method.VISIT_INSN, // LAND
        Method.VISIT_INSN, // IOR
        Method.VISIT_INSN, // LOR
        Method.VISIT_INSN, // IXOR
        Method.VISIT_INSN, // LXOR
        null, // IINC
        Method.VISIT_INSN, // I2L
        Method.VISIT_INSN, // I2F
        Method.VISIT_INSN, // I2D
        Method.VISIT_INSN, // L2I
        Method.VISIT_INSN, // L2F
        Method.VISIT_INSN, // L2D
        Method.VISIT_INSN, // F2I
        Method.VISIT_INSN, // F2L
        Method.VISIT_INSN, // F2D
        Method.VISIT_INSN, // D2I
        Method.VISIT_INSN, // D2L
        Method.VISIT_INSN, // D2F
        Method.VISIT_INSN, // I2B
        Method.VISIT_INSN, // I2C
        Method.VISIT_INSN, // I2S
        Method.VISIT_INSN, // LCMP
        Method.VISIT_INSN, // FCMPL
        Method.VISIT_INSN, // FCMPG
        Method.VISIT_INSN, // DCMPL
        Method.VISIT_INSN, // DCMPG
        Method.VISIT_JUMP_INSN, // IFEQ
        Method.VISIT_JUMP_INSN, // IFNE
        Method.VISIT_JUMP_INSN, // IFLT
        Method.VISIT_JUMP_INSN, // IFGE
        Method.VISIT_JUMP_INSN, // IFGT
        Method.VISIT_JUMP_INSN, // IFLE
        Method.VISIT_JUMP_INSN, // IF_ICMPEQ
        Method.VISIT_JUMP_INSN, // IF_ICMPNE
        Method.VISIT_JUMP_INSN, // IF_ICMPLT
        Method.VISIT_JUMP_INSN, // IF_ICMPGE
        Method.VISIT_JUMP_INSN, // IF_ICMPGT
        Method.VISIT_JUMP_INSN, // IF_ICMPLE
        Method.VISIT_JUMP_INSN, // IF_ACMPEQ
        Method.VISIT_JUMP_INSN, // IF_ACMPNE
        Method.VISIT_JUMP_INSN, // GOTO
        Method.VISIT_JUMP_INSN, // JSR
        Method.VISIT_VAR_INSN, // RET
        null, // TABLESWITCH
        null, // LOOKUPSWITCH
        Method.VISIT_INSN, // IRETURN
        Method.VISIT_INSN, // LRETURN
        Method.VISIT_INSN, // FRETURN
        Method.VISIT_INSN, // DRETURN
        Method.VISIT_INSN, // ARETURN
        Method.VISIT_INSN, // RETURN
        Method.VISIT_FIELD_INSN, // GETSTATIC
        Method.VISIT_FIELD_INSN, // PUTSTATIC
        Method.VISIT_FIELD_INSN, // GETFIELD
        Method.VISIT_FIELD_INSN, // PUTFIELD
        Method.VISIT_METHOD_INSN, // INVOKEVIRTUAL
        Method.VISIT_METHOD_INSN, // INVOKESPECIAL
        Method.VISIT_METHOD_INSN, // INVOKESTATIC
        Method.VISIT_METHOD_INSN, // INVOKEINTERFACE
        null, // INVOKEDYNAMIC
        Method.VISIT_TYPE_INSN, // NEW
        Method.VISIT_INT_INSN, // NEWARRAY
        Method.VISIT_TYPE_INSN, // ANEWARRAY
        Method.VISIT_INSN, // ARRAYLENGTH
        Method.VISIT_INSN, // ATHROW
        Method.VISIT_TYPE_INSN, // CHECKCAST
        Method.VISIT_TYPE_INSN, // INSTANCEOF
        Method.VISIT_INSN, // MONITORENTER
        Method.VISIT_INSN, // MONITOREXIT
        null, // WIDE
        null, // MULTIANEWARRAY
        Method.VISIT_JUMP_INSN, // IFNULL
        Method.VISIT_JUMP_INSN // IFNONNULL
    };

    private static final String INVALID = "Invalid ";
    private static final String INVALID_DESCRIPTOR = "Invalid descriptor: ";
    private static final String INVALID_TYPE_REFERENCE = "Invalid type reference sort 0x";
    private static final String INVALID_LOCAL_VARIABLE_INDEX = "Invalid local variable index";
    private static final String MUST_NOT_BE_NULL_OR_EMPTY = " (must not be null or empty)";
    private static final String START_LABEL = "start label";
    private static final String END_LABEL = "end label";

    /** The class version number. */
    public int version;

    /** The access flags of the visited method. */
    private int access;

    /**
      * The number of method parameters that can have runtime visible annotations. 0 means that all the
      * parameters from the method descriptor can have annotations.
      */
    private int visibleAnnotableParameterCount;

    /**
      * The number of method parameters that can have runtime invisible annotations. 0 means that all
      * the parameters from the method descriptor can have annotations.
      */
    private int invisibleAnnotableParameterCount;

    /** Whether the {@link #visitCode} method has been called. */
    private boolean visitCodeCalled;

    /** Whether the {@link #visitMaxs} method has been called. */
    private boolean visitMaxCalled;

    /** Whether the {@link #visitEnd} method has been called. */
    private boolean visitEndCalled;

    /** The number of visited instructions so far. */
    private int insnCount;

    /** The index of the instruction designated by each visited label. */
    private final Map<Label, Integer> labelInsnIndices;

    /** The labels referenced by the visited method. */
    private Set<Label> referencedLabels;

    /** The index of the instruction corresponding to the last visited stack map frame. */
    private int lastFrameInsnIndex = -1;

    /** The number of visited frames in expanded form. */
    private int numExpandedFrames;

    /** The number of visited frames in compressed form. */
    private int numCompressedFrames;

    /**
      * The exception handler ranges. Each pair of list element contains the start and end labels of an
      * exception handler block.
      */
    private List<Label> handlers;

    /**
      * Constructs a new {@link CheckMethodAdapter} object. This method adapter will not perform any
      * data flow check (see {@link #CheckMethodAdapter(int,String,String,MethodVisitor,Map)}).
      * <i>Subclasses must not use this constructor</i>. Instead, they must use the {@link
      * #CheckMethodAdapter(int, MethodVisitor, Map)} version.
      *
      * @param methodvisitor the method visitor to which this adapter must delegate calls.
      */
    public CheckMethodAdapter(final MethodVisitor methodvisitor) {
        this(methodvisitor, new HashMap<Label, Integer>());
    }

    /**
      * Constructs a new {@link CheckMethodAdapter} object. This method adapter will not perform any
      * data flow check (see {@link #CheckMethodAdapter(int,String,String,MethodVisitor,Map)}).
      * <i>Subclasses must not use this constructor</i>. Instead, they must use the {@link
      * #CheckMethodAdapter(int, MethodVisitor, Map)} version.
      *
      * @param methodVisitor the method visitor to which this adapter must delegate calls.
      * @param labelInsnIndices the index of the instruction designated by each visited label so far
      *     (in other methods). This map is updated with the labels from the visited method.
      * @throws IllegalStateException If a subclass calls this constructor.
      */
    public CheckMethodAdapter(
            final MethodVisitor methodVisitor, final Map<Label, Integer> labelInsnIndices) {
        this(/* latest api = */ Opcodes.ASM8, methodVisitor, labelInsnIndices);
        if (getClass() != CheckMethodAdapter.class) {
            throw new IllegalStateException();
        }
    }

    /**
      * Constructs a new {@link CheckMethodAdapter} object. This method adapter will not perform any
      * data flow check (see {@link #CheckMethodAdapter(int,String,String,MethodVisitor,Map)}).
      *
      * @param api the ASM API version implemented by this CheckMethodAdapter. Must be one of {@link
      *     Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}, {@link Opcodes#ASM7} or {@link
      *     Opcodes#ASM8}.
      * @param methodVisitor the method visitor to which this adapter must delegate calls.
      * @param labelInsnIndices the index of the instruction designated by each visited label so far
      *     (in other methods). This map is updated with the labels from the visited method.
      */
    protected CheckMethodAdapter(
            final int api,
            final MethodVisitor methodVisitor,
            final Map<Label, Integer> labelInsnIndices) {
        super(api, methodVisitor);
        this.labelInsnIndices = labelInsnIndices;
        this.referencedLabels = new HashSet<>();
        this.handlers = new ArrayList<>();
    }

    /**
      * Constructs a new {@link CheckMethodAdapter} object. This method adapter will perform basic data
      * flow checks. For instance in a method whose signature is {@code void m ()}, the invalid
      * instruction IRETURN, or the invalid sequence IADD L2I will be detected. <i>Subclasses must not
      * use this constructor</i>. Instead, they must use the {@link
      * #CheckMethodAdapter(int,int,String,String,MethodVisitor,Map)} version.
      *
      * @param access the method's access flags.
      * @param name the method's name.
      * @param descriptor the method's descriptor (see {@link Type}).
      * @param methodVisitor the method visitor to which this adapter must delegate calls.
      * @param labelInsnIndices the index of the instruction designated by each visited label so far
      *     (in other methods). This map is updated with the labels from the visited method.
      */
    public CheckMethodAdapter(
            final int access,
            final String name,
            final String descriptor,
            final MethodVisitor methodVisitor,
            final Map<Label, Integer> labelInsnIndices) {
        this(
                /* latest api = */ Opcodes.ASM8, access, name, descriptor, methodVisitor, labelInsnIndices);
        if (getClass() != CheckMethodAdapter.class) {
            throw new IllegalStateException();
        }
    }

    /**
      * Constructs a new {@link CheckMethodAdapter} object. This method adapter will perform basic data
      * flow checks. For instance in a method whose signature is {@code void m ()}, the invalid
      * instruction IRETURN, or the invalid sequence IADD L2I will be detected.
      *
      * @param api the ASM API version implemented by this CheckMethodAdapter. Must be one of {@link
      *     Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6}, {@link Opcodes#ASM7} or {@link
      *     Opcodes#ASM8}.
      * @param access the method's access flags.
      * @param name the method's name.
      * @param descriptor the method's descriptor (see {@link Type}).
      * @param methodVisitor the method visitor to which this adapter must delegate calls.
      * @param labelInsnIndices the index of the instruction designated by each visited label so far
      *     (in other methods). This map is updated with the labels from the visited method.
      */
    protected CheckMethodAdapter(
            final int api,
            final int access,
            final String name,
            final String descriptor,
            final MethodVisitor methodVisitor,
            final Map<Label, Integer> labelInsnIndices) {
        this(
                api,
                new MethodNode(api, access, name, descriptor, null, null) {
                    @Override
                    public void visitEnd() {
                        Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicVerifier());
                        try {
                            analyzer.analyze("dummy", this);
                        } catch (IndexOutOfBoundsException e) {
                            if (maxLocals == 0 && maxStack == 0) {
                                throw new IllegalArgumentException(
                                        "Data flow checking option requires valid, non zero maxLocals and maxStack.",
                                        e);
                            }
                            throwError(analyzer, e);
                        } catch (AnalyzerException e) {
                            throwError(analyzer, e);
                        }
                        if (methodVisitor != null) {
                            accept(methodVisitor);
                        }
                    }

                    private void throwError(final Analyzer<BasicValue> analyzer, final Exception e) {
                        StringWriter stringWriter = new StringWriter();
                        PrintWriter printWriter = new PrintWriter(stringWriter, true);
                        CheckClassAdapter.printAnalyzerResult(this, analyzer, printWriter);
                        printWriter.close();
                        throw new IllegalArgumentException(e.getMessage() + ' ' + stringWriter.toString(), e);
                    }
                },
                labelInsnIndices);
        this.access = access;
    }

    @Override
    public void visitParameter(final String name, final int access) {
        if (name != null) {
            checkUnqualifiedName(version, name, "name");
        }
        CheckClassAdapter.checkAccess(
                access, Opcodes.ACC_FINAL + Opcodes.ACC_MANDATED + Opcodes.ACC_SYNTHETIC);
        super.visitParameter(name, access);
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        checkVisitEndNotCalled();
        checkDescriptor(version, descriptor, false);
        return new CheckAnnotationAdapter(super.visitAnnotation(descriptor, visible));
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        checkVisitEndNotCalled();
        int sort = new TypeReference(typeRef).getSort();
        if (sort != TypeReference.METHOD_TYPE_PARAMETER
                && sort != TypeReference.METHOD_TYPE_PARAMETER_BOUND
                && sort != TypeReference.METHOD_RETURN
                && sort != TypeReference.METHOD_RECEIVER
                && sort != TypeReference.METHOD_FORMAL_PARAMETER
                && sort != TypeReference.THROWS) {
            throw new IllegalArgumentException(INVALID_TYPE_REFERENCE + Integer.toHexString(sort));
        }
        CheckClassAdapter.checkTypeRef(typeRef);
        CheckMethodAdapter.checkDescriptor(version, descriptor, false);
        return new CheckAnnotationAdapter(
                super.visitTypeAnnotation(typeRef, typePath, descriptor, visible));
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        checkVisitEndNotCalled();
        return new CheckAnnotationAdapter(super.visitAnnotationDefault(), false);
    }

    @Override
    public void visitAnnotableParameterCount(final int parameterCount, final boolean visible) {
        checkVisitEndNotCalled();
        if (visible) {
            visibleAnnotableParameterCount = parameterCount;
        } else {
            invisibleAnnotableParameterCount = parameterCount;
        }
        super.visitAnnotableParameterCount(parameterCount, visible);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(
            final int parameter, final String descriptor, final boolean visible) {
        checkVisitEndNotCalled();
        if ((visible
                        && visibleAnnotableParameterCount > 0
                        && parameter >= visibleAnnotableParameterCount)
                || (!visible
                        && invisibleAnnotableParameterCount > 0
                        && parameter >= invisibleAnnotableParameterCount)) {
            throw new IllegalArgumentException("Invalid parameter index");
        }
        checkDescriptor(version, descriptor, false);
        return new CheckAnnotationAdapter(
                super.visitParameterAnnotation(parameter, descriptor, visible));
    }

    @Override
    public void visitAttribute(final Attribute attribute) {
        checkVisitEndNotCalled();
        if (attribute == null) {
            throw new IllegalArgumentException("Invalid attribute (must not be null)");
        }
        super.visitAttribute(attribute);
    }

    @Override
    public void visitCode() {
        if ((access & Opcodes.ACC_ABSTRACT) != 0) {
            throw new UnsupportedOperationException("Abstract methods cannot have code");
        }
        visitCodeCalled = true;
        super.visitCode();
    }

    @Override
    public void visitFrame(
            final int type,
            final int numLocal,
            final Object[] local,
            final int numStack,
            final Object[] stack) {
        if (insnCount == lastFrameInsnIndex) {
            throw new IllegalStateException("At most one frame can be visited at a given code location.");
        }
        lastFrameInsnIndex = insnCount;
        int maxNumLocal;
        int maxNumStack;
        switch (type) {
            case Opcodes.F_NEW:
            case Opcodes.F_FULL:
                maxNumLocal = Integer.MAX_VALUE;
                maxNumStack = Integer.MAX_VALUE;
                break;

            case Opcodes.F_SAME:
                maxNumLocal = 0;
                maxNumStack = 0;
                break;

            case Opcodes.F_SAME1:
                maxNumLocal = 0;
                maxNumStack = 1;
                break;

            case Opcodes.F_APPEND:
            case Opcodes.F_CHOP:
                maxNumLocal = 3;
                maxNumStack = 0;
                break;

            default:
                throw new IllegalArgumentException("Invalid frame type " + type);
        }

        if (numLocal > maxNumLocal) {
            throw new IllegalArgumentException(
                    "Invalid numLocal=" + numLocal + " for frame type " + type);
        }
        if (numStack > maxNumStack) {
            throw new IllegalArgumentException(
                    "Invalid numStack=" + numStack + " for frame type " + type);
        }

        if (type != Opcodes.F_CHOP) {
            if (numLocal > 0 && (local == null || local.length < numLocal)) {
                throw new IllegalArgumentException("Array local[] is shorter than numLocal");
            }
            for (int i = 0; i < numLocal; ++i) {
                checkFrameValue(local[i]);
            }
        }
        if (numStack > 0 && (stack == null || stack.length < numStack)) {
            throw new IllegalArgumentException("Array stack[] is shorter than numStack");
        }
        for (int i = 0; i < numStack; ++i) {
            checkFrameValue(stack[i]);
        }
        if (type == Opcodes.F_NEW) {
            ++numExpandedFrames;
        } else {
            ++numCompressedFrames;
        }
        if (numExpandedFrames > 0 && numCompressedFrames > 0) {
            throw new IllegalArgumentException("Expanded and compressed frames must not be mixed.");
        }
        super.visitFrame(type, numLocal, local, numStack, stack);
    }

    @Override
    public void visitInsn(final int opcode) {
        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        checkOpcodeMethod(opcode, Method.VISIT_INSN);
        super.visitInsn(opcode);
        ++insnCount;
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        checkOpcodeMethod(opcode, Method.VISIT_INT_INSN);
        switch (opcode) {
            case Opcodes.BIPUSH:
                checkSignedByte(operand, "Invalid operand");
                break;
            case Opcodes.SIPUSH:
                checkSignedShort(operand, "Invalid operand");
                break;
            case Opcodes.NEWARRAY:
                if (operand < Opcodes.T_BOOLEAN || operand > Opcodes.T_LONG) {
                    throw new IllegalArgumentException(
                            "Invalid operand (must be an array type code T_...): " + operand);
                }
                break;
            default:
                throw new AssertionError();
        }
        super.visitIntInsn(opcode, operand);
        ++insnCount;
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        checkOpcodeMethod(opcode, Method.VISIT_VAR_INSN);
        checkUnsignedShort(var, INVALID_LOCAL_VARIABLE_INDEX);
        super.visitVarInsn(opcode, var);
        ++insnCount;
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        checkOpcodeMethod(opcode, Method.VISIT_TYPE_INSN);
        checkInternalName(version, type, "type");
        if (opcode == Opcodes.NEW && type.charAt(0) == '[') {
            throw new IllegalArgumentException("NEW cannot be used to create arrays: " + type);
        }
        super.visitTypeInsn(opcode, type);
        ++insnCount;
    }

    @Override
    public void visitFieldInsn(
            final int opcode, final String owner, final String name, final String descriptor) {
        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        checkOpcodeMethod(opcode, Method.VISIT_FIELD_INSN);
        checkInternalName(version, owner, "owner");
        checkUnqualifiedName(version, name, "name");
        checkDescriptor(version, descriptor, false);
        super.visitFieldInsn(opcode, owner, name, descriptor);
        ++insnCount;
    }

    @Override
    public void visitMethodInsn(
            final int opcodeAndSource,
            final String owner,
            final String name,
            final String descriptor,
            final boolean isInterface) {
        if (api < Opcodes.ASM5 && (opcodeAndSource & Opcodes.SOURCE_DEPRECATED) == 0) {
            // Redirect the call to the deprecated version of this method.
            super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface);
            return;
        }
        int opcode = opcodeAndSource & ~Opcodes.SOURCE_MASK;

        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        checkOpcodeMethod(opcode, Method.VISIT_METHOD_INSN);
        if (opcode != Opcodes.INVOKESPECIAL || !"<init>".equals(name)) {
            checkMethodIdentifier(version, name, "name");
        }
        checkInternalName(version, owner, "owner");
        checkMethodDescriptor(version, descriptor);
        if (opcode == Opcodes.INVOKEVIRTUAL && isInterface) {
            throw new IllegalArgumentException("INVOKEVIRTUAL can't be used with interfaces");
        }
        if (opcode == Opcodes.INVOKEINTERFACE && !isInterface) {
            throw new IllegalArgumentException("INVOKEINTERFACE can't be used with classes");
        }
        if (opcode == Opcodes.INVOKESPECIAL && isInterface && (version & 0xFFFF) < Opcodes.V1_8) {
            throw new IllegalArgumentException(
                    "INVOKESPECIAL can't be used with interfaces prior to Java 8");
        }
        super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface);
        ++insnCount;
    }

    @Override
    public void visitInvokeDynamicInsn(
            final String name,
            final String descriptor,
            final Handle bootstrapMethodHandle,
            final Object... bootstrapMethodArguments) {
        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        checkMethodIdentifier(version, name, "name");
        checkMethodDescriptor(version, descriptor);
        if (bootstrapMethodHandle.getTag() != Opcodes.H_INVOKESTATIC
                && bootstrapMethodHandle.getTag() != Opcodes.H_NEWINVOKESPECIAL) {
            throw new IllegalArgumentException("invalid handle tag " + bootstrapMethodHandle.getTag());
        }
        for (Object bootstrapMethodArgument : bootstrapMethodArguments) {
            checkLdcConstant(bootstrapMethodArgument);
        }
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        ++insnCount;
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        checkOpcodeMethod(opcode, Method.VISIT_JUMP_INSN);
        checkLabel(label, false, "label");
        super.visitJumpInsn(opcode, label);
        referencedLabels.add(label);
        ++insnCount;
    }

    @Override
    public void visitLabel(final Label label) {
        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        checkLabel(label, false, "label");
        if (labelInsnIndices.get(label) != null) {
            throw new IllegalArgumentException("Already visited label");
        }
        labelInsnIndices.put(label, insnCount);
        super.visitLabel(label);
    }

    @Override
    public void visitLdcInsn(final Object value) {
        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        checkLdcConstant(value);
        super.visitLdcInsn(value);
        ++insnCount;
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        checkUnsignedShort(var, INVALID_LOCAL_VARIABLE_INDEX);
        checkSignedShort(increment, "Invalid increment");
        super.visitIincInsn(var, increment);
        ++insnCount;
    }

    @Override
    public void visitTableSwitchInsn(
            final int min, final int max, final Label dflt, final Label... labels) {
        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        if (max < min) {
            throw new IllegalArgumentException(
                    "Max = " + max + " must be greater than or equal to min = " + min);
        }
        checkLabel(dflt, false, "default label");
        if (labels == null || labels.length != max - min + 1) {
            throw new IllegalArgumentException("There must be max - min + 1 labels");
        }
        for (int i = 0; i < labels.length; ++i) {
            checkLabel(labels[i], false, "label at index " + i);
        }
        super.visitTableSwitchInsn(min, max, dflt, labels);
        Collections.addAll(referencedLabels, labels);
        ++insnCount;
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
        checkVisitMaxsNotCalled();
        checkVisitCodeCalled();
        checkLabel(dflt, false, "default label");
        if (keys == null || labels == null || keys.length != labels.length) {
            throw new IllegalArgumentException("There must be the same number of keys and labels");
        }
        for (int i = 0; i < labels.length; ++i) {
            checkLabel(labels[i], false, "label at index " + i);
        }
        super.visitLookupSwitchInsn(dflt, keys, labels);
        referencedLabels.add(dflt);
        Collections.addAll(referencedLabels, labels);
        ++insnCount;
    }

    @Override
    public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        checkDescriptor(version, descriptor, false);
        if (descriptor.charAt(0) != '[') {
            throw new IllegalArgumentException(
                    "Invalid descriptor (must be an array type descriptor): " + descriptor);
        }
        if (numDimensions < 1) {
            throw new IllegalArgumentException(
                    "Invalid dimensions (must be greater than 0): " + numDimensions);
        }
        if (numDimensions > descriptor.lastIndexOf('[') + 1) {
            throw new IllegalArgumentException(
                    "Invalid dimensions (must not be greater than numDimensions(descriptor)): "
                            + numDimensions);
        }
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
        ++insnCount;
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(
            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        int sort = new TypeReference(typeRef).getSort();
        if (sort != TypeReference.INSTANCEOF
                && sort != TypeReference.NEW
                && sort != TypeReference.CONSTRUCTOR_REFERENCE
                && sort != TypeReference.METHOD_REFERENCE
                && sort != TypeReference.CAST
                && sort != TypeReference.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT
                && sort != TypeReference.METHOD_INVOCATION_TYPE_ARGUMENT
                && sort != TypeReference.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT
                && sort != TypeReference.METHOD_REFERENCE_TYPE_ARGUMENT) {
            throw new IllegalArgumentException(INVALID_TYPE_REFERENCE + Integer.toHexString(sort));
        }
        CheckClassAdapter.checkTypeRef(typeRef);
        CheckMethodAdapter.checkDescriptor(version, descriptor, false);
        return new CheckAnnotationAdapter(
                super.visitInsnAnnotation(typeRef, typePath, descriptor, visible));
    }

    @Override
    public void visitTryCatchBlock(
            final Label start, final Label end, final Label handler, final String type) {
        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        checkLabel(start, false, START_LABEL);
        checkLabel(end, false, END_LABEL);
        checkLabel(handler, false, "handler label");
        if (labelInsnIndices.get(start) != null
                || labelInsnIndices.get(end) != null
                || labelInsnIndices.get(handler) != null) {
            throw new IllegalStateException("Try catch blocks must be visited before their labels");
        }
        if (type != null) {
            checkInternalName(version, type, "type");
        }
        super.visitTryCatchBlock(start, end, handler, type);
        handlers.add(start);
        handlers.add(end);
    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(
            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        int sort = new TypeReference(typeRef).getSort();
        if (sort != TypeReference.EXCEPTION_PARAMETER) {
            throw new IllegalArgumentException(INVALID_TYPE_REFERENCE + Integer.toHexString(sort));
        }
        CheckClassAdapter.checkTypeRef(typeRef);
        CheckMethodAdapter.checkDescriptor(version, descriptor, false);
        return new CheckAnnotationAdapter(
                super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible));
    }

    @Override
    public void visitLocalVariable(
            final String name,
            final String descriptor,
            final String signature,
            final Label start,
            final Label end,
            final int index) {
        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        checkUnqualifiedName(version, name, "name");
        checkDescriptor(version, descriptor, false);
        if (signature != null) {
            CheckClassAdapter.checkFieldSignature(signature);
        }
        checkLabel(start, true, START_LABEL);
        checkLabel(end, true, END_LABEL);
        checkUnsignedShort(index, INVALID_LOCAL_VARIABLE_INDEX);
        int startInsnIndex = labelInsnIndices.get(start).intValue();
        int endInsnIndex = labelInsnIndices.get(end).intValue();
        if (endInsnIndex < startInsnIndex) {
            throw new IllegalArgumentException(
                    "Invalid start and end labels (end must be greater than start)");
        }
        super.visitLocalVariable(name, descriptor, signature, start, end, index);
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
        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        int sort = new TypeReference(typeRef).getSort();
        if (sort != TypeReference.LOCAL_VARIABLE && sort != TypeReference.RESOURCE_VARIABLE) {
            throw new IllegalArgumentException(INVALID_TYPE_REFERENCE + Integer.toHexString(sort));
        }
        CheckClassAdapter.checkTypeRef(typeRef);
        checkDescriptor(version, descriptor, false);
        if (start == null
                || end == null
                || index == null
                || end.length != start.length
                || index.length != start.length) {
            throw new IllegalArgumentException(
                    "Invalid start, end and index arrays (must be non null and of identical length");
        }
        for (int i = 0; i < start.length; ++i) {
            checkLabel(start[i], true, START_LABEL);
            checkLabel(end[i], true, END_LABEL);
            checkUnsignedShort(index[i], INVALID_LOCAL_VARIABLE_INDEX);
            int startInsnIndex = labelInsnIndices.get(start[i]).intValue();
            int endInsnIndex = labelInsnIndices.get(end[i]).intValue();
            if (endInsnIndex < startInsnIndex) {
                throw new IllegalArgumentException(
                        "Invalid start and end labels (end must be greater than start)");
            }
        }
        return super.visitLocalVariableAnnotation(
                typeRef, typePath, start, end, index, descriptor, visible);
    }

    @Override
    public void visitLineNumber(final int line, final Label start) {
        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        checkUnsignedShort(line, "Invalid line number");
        checkLabel(start, true, START_LABEL);
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
        checkVisitCodeCalled();
        checkVisitMaxsNotCalled();
        visitMaxCalled = true;
        for (Label l : referencedLabels) {
            if (labelInsnIndices.get(l) == null) {
                throw new IllegalStateException("Undefined label used");
            }
        }
        for (int i = 0; i < handlers.size(); i += 2) {
            Integer startInsnIndex = labelInsnIndices.get(handlers.get(i));
            Integer endInsnIndex = labelInsnIndices.get(handlers.get(i + 1));
            if (startInsnIndex == null || endInsnIndex == null) {
                throw new IllegalStateException("Undefined try catch block labels");
            }
            if (endInsnIndex.intValue() <= startInsnIndex.intValue()) {
                throw new IllegalStateException("Emty try catch block handler range");
            }
        }
        checkUnsignedShort(maxStack, "Invalid max stack");
        checkUnsignedShort(maxLocals, "Invalid max locals");
        super.visitMaxs(maxStack, maxLocals);
    }

    @Override
    public void visitEnd() {
        checkVisitEndNotCalled();
        visitEndCalled = true;
        super.visitEnd();
    }

    // -----------------------------------------------------------------------------------------------
    // Utility methods
    // -----------------------------------------------------------------------------------------------

    /** Checks that the {@link #visitCode} method has been called. */
    private void checkVisitCodeCalled() {
        if (!visitCodeCalled) {
            throw new IllegalStateException(
                    "Cannot visit instructions before visitCode has been called.");
        }
    }

    /** Checks that the {@link #visitMaxs} method has not been called. */
    private void checkVisitMaxsNotCalled() {
        if (visitMaxCalled) {
            throw new IllegalStateException("Cannot visit instructions after visitMaxs has been called.");
        }
    }

    /** Checks that the {@link #visitEnd} method has not been called. */
    private void checkVisitEndNotCalled() {
        if (visitEndCalled) {
            throw new IllegalStateException("Cannot visit elements after visitEnd has been called.");
        }
    }

    /**
      * Checks a stack frame value.
      *
      * @param value the value to be checked.
      */
    private void checkFrameValue(final Object value) {
        if (value == Opcodes.TOP
                || value == Opcodes.INTEGER
                || value == Opcodes.FLOAT
                || value == Opcodes.LONG
                || value == Opcodes.DOUBLE
                || value == Opcodes.NULL
                || value == Opcodes.UNINITIALIZED_THIS) {
            return;
        }
        if (value instanceof String) {
            checkInternalName(version, (String) value, "Invalid stack frame value");
        } else if (value instanceof Label) {
            referencedLabels.add((Label) value);
        } else {
            throw new IllegalArgumentException("Invalid stack frame value: " + value);
        }
    }

    /**
      * Checks that the method to visit the given opcode is equal to the given method.
      *
      * @param opcode the opcode to be checked.
      * @param method the expected visit method.
      */
    private static void checkOpcodeMethod(final int opcode, final Method method) {
        if (opcode < Opcodes.NOP || opcode > Opcodes.IFNONNULL || OPCODE_METHODS[opcode] != method) {
            throw new IllegalArgumentException("Invalid opcode: " + opcode);
        }
    }

    /**
      * Checks that the given value is a signed byte.
      *
      * @param value the value to be checked.
      * @param message the message to use in case of error.
      */
    private static void checkSignedByte(final int value, final String message) {
        if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
            throw new IllegalArgumentException(message + " (must be a signed byte): " + value);
        }
    }

    /**
      * Checks that the given value is a signed short.
      *
      * @param value the value to be checked.
      * @param message the message to use in case of error.
      */
    private static void checkSignedShort(final int value, final String message) {
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw new IllegalArgumentException(message + " (must be a signed short): " + value);
        }
    }

    /**
      * Checks that the given value is an unsigned short.
      *
      * @param value the value to be checked.
      * @param message the message to use in case of error.
      */
    private static void checkUnsignedShort(final int value, final String message) {
        if (value < 0 || value > 65535) {
            throw new IllegalArgumentException(message + " (must be an unsigned short): " + value);
        }
    }

    /**
      * Checks that the given value is an {@link Integer}, {@link Float}, {@link Long}, {@link Double}
      * or {@link String} value.
      *
      * @param value the value to be checked.
      */
    static void checkConstant(final Object value) {
        if (!(value instanceof Integer)
                && !(value instanceof Float)
                && !(value instanceof Long)
                && !(value instanceof Double)
                && !(value instanceof String)) {
            throw new IllegalArgumentException("Invalid constant: " + value);
        }
    }

    /**
      * Checks that the given value is a valid operand for the LDC instruction.
      *
      * @param value the value to be checked.
      */
    private void checkLdcConstant(final Object value) {
        if (value instanceof Type) {
            int sort = ((Type) value).getSort();
            if (sort != Type.OBJECT && sort != Type.ARRAY && sort != Type.METHOD) {
                throw new IllegalArgumentException("Illegal LDC constant value");
            }
            if (sort != Type.METHOD && (version & 0xFFFF) < Opcodes.V1_5) {
                throw new IllegalArgumentException("ldc of a constant class requires at least version 1.5");
            }
            if (sort == Type.METHOD && (version & 0xFFFF) < Opcodes.V1_7) {
                throw new IllegalArgumentException("ldc of a method type requires at least version 1.7");
            }
        } else if (value instanceof Handle) {
            if ((version & 0xFFFF) < Opcodes.V1_7) {
                throw new IllegalArgumentException("ldc of a Handle requires at least version 1.7");
            }
            Handle handle = (Handle) value;
            int tag = handle.getTag();
            if (tag < Opcodes.H_GETFIELD || tag > Opcodes.H_INVOKEINTERFACE) {
                throw new IllegalArgumentException("invalid handle tag " + tag);
            }
            checkInternalName(this.version, handle.getOwner(), "handle owner");
            if (tag <= Opcodes.H_PUTSTATIC) {
                checkDescriptor(this.version, handle.getDesc(), false);
            } else {
                checkMethodDescriptor(this.version, handle.getDesc());
            }
            String handleName = handle.getName();
            if (!("<init>".equals(handleName) && tag == Opcodes.H_NEWINVOKESPECIAL)) {
                checkMethodIdentifier(this.version, handleName, "handle name");
            }
        } else if (value instanceof ConstantDynamic) {
            if ((version & 0xFFFF) < Opcodes.V11) {
                throw new IllegalArgumentException("ldc of a ConstantDynamic requires at least version 11");
            }
            ConstantDynamic constantDynamic = (ConstantDynamic) value;
            checkMethodIdentifier(this.version, constantDynamic.getName(), "constant dynamic name");
            checkDescriptor(this.version, constantDynamic.getDescriptor(), false);
            checkLdcConstant(constantDynamic.getBootstrapMethod());
            int bootstrapMethodArgumentCount = constantDynamic.getBootstrapMethodArgumentCount();
            for (int i = 0; i < bootstrapMethodArgumentCount; ++i) {
                checkLdcConstant(constantDynamic.getBootstrapMethodArgument(i));
            }
        } else {
            checkConstant(value);
        }
    }

    /**
      * Checks that the given string is a valid unqualified name.
      *
      * @param version the class version.
      * @param name the string to be checked.
      * @param message the message to use in case of error.
      */
    static void checkUnqualifiedName(final int version, final String name, final String message) {
        checkIdentifier(version, name, 0, -1, message);
    }

    /**
      * Checks that the given substring is a valid Java identifier.
      *
      * @param version the class version.
      * @param name the string to be checked.
      * @param startPos the index of the first character of the identifier (inclusive).
      * @param endPos the index of the last character of the identifier (exclusive). -1 is equivalent
      *     to {@code name.length()} if name is not {@literal null}.
      * @param message the message to use in case of error.
      */
    static void checkIdentifier(
            final int version,
            final String name,
            final int startPos,
            final int endPos,
            final String message) {
        if (name == null || (endPos == -1 ? name.length() <= startPos : endPos <= startPos)) {
            throw new IllegalArgumentException(INVALID + message + MUST_NOT_BE_NULL_OR_EMPTY);
        }
        int max = endPos == -1 ? name.length() : endPos;
        if ((version & 0xFFFF) >= Opcodes.V1_5) {
            for (int i = startPos; i < max; i = name.offsetByCodePoints(i, 1)) {
                if (".;[/".indexOf(name.codePointAt(i)) != -1) {
                    throw new IllegalArgumentException(
                            INVALID + message + " (must not contain . ; [ or /): " + name);
                }
            }
            return;
        }
        for (int i = startPos; i < max; i = name.offsetByCodePoints(i, 1)) {
            if (i == startPos
                    ? !Character.isJavaIdentifierStart(name.codePointAt(i))
                    : !Character.isJavaIdentifierPart(name.codePointAt(i))) {
                throw new IllegalArgumentException(
                        INVALID + message + " (must be a valid Java identifier): " + name);
            }
        }
    }

    /**
      * Checks that the given string is a valid Java identifier.
      *
      * @param version the class version.
      * @param name the string to be checked.
      * @param message the message to use in case of error.
      */
    static void checkMethodIdentifier(final int version, final String name, final String message) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException(INVALID + message + MUST_NOT_BE_NULL_OR_EMPTY);
        }
        if ((version & 0xFFFF) >= Opcodes.V1_5) {
            for (int i = 0; i < name.length(); i = name.offsetByCodePoints(i, 1)) {
                if (".;[/<>".indexOf(name.codePointAt(i)) != -1) {
                    throw new IllegalArgumentException(
                            INVALID + message + " (must be a valid unqualified name): " + name);
                }
            }
            return;
        }
        for (int i = 0; i < name.length(); i = name.offsetByCodePoints(i, 1)) {
            if (i == 0
                    ? !Character.isJavaIdentifierStart(name.codePointAt(i))
                    : !Character.isJavaIdentifierPart(name.codePointAt(i))) {
                throw new IllegalArgumentException(
                        INVALID
                                + message
                                + " (must be a '<init>', '<clinit>' or a valid Java identifier): "
                                + name);
            }
        }
    }

    /**
      * Checks that the given string is a valid internal class name or array type descriptor.
      *
      * @param version the class version.
      * @param name the string to be checked.
      * @param message the message to use in case of error.
      */
    static void checkInternalName(final int version, final String name, final String message) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException(INVALID + message + MUST_NOT_BE_NULL_OR_EMPTY);
        }
        if (name.charAt(0) == '[') {
            checkDescriptor(version, name, false);
        } else {
            checkInternalClassName(version, name, message);
        }
    }

    /**
      * Checks that the given string is a valid internal class name.
      *
      * @param version the class version.
      * @param name the string to be checked.
      * @param message the message to use in case of error.
      */
    private static void checkInternalClassName(
            final int version, final String name, final String message) {
        try {
            int startIndex = 0;
            int slashIndex;
            while ((slashIndex = name.indexOf('/', startIndex + 1)) != -1) {
                checkIdentifier(version, name, startIndex, slashIndex, null);
                startIndex = slashIndex + 1;
            }
            checkIdentifier(version, name, startIndex, name.length(), null);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    INVALID + message + " (must be an internal class name): " + name, e);
        }
    }

    /**
      * Checks that the given string is a valid type descriptor.
      *
      * @param version the class version.
      * @param descriptor the string to be checked.
      * @param canBeVoid {@literal true} if {@code V} can be considered valid.
      */
    static void checkDescriptor(final int version, final String descriptor, final boolean canBeVoid) {
        int endPos = checkDescriptor(version, descriptor, 0, canBeVoid);
        if (endPos != descriptor.length()) {
            throw new IllegalArgumentException(INVALID_DESCRIPTOR + descriptor);
        }
    }

    /**
      * Checks that a the given substring is a valid type descriptor.
      *
      * @param version the class version.
      * @param descriptor the string to be checked.
      * @param startPos the index of the first character of the type descriptor (inclusive).
      * @param canBeVoid whether {@code V} can be considered valid.
      * @return the index of the last character of the type descriptor, plus one.
      */
    private static int checkDescriptor(
            final int version, final String descriptor, final int startPos, final boolean canBeVoid) {
        if (descriptor == null || startPos >= descriptor.length()) {
            throw new IllegalArgumentException("Invalid type descriptor (must not be null or empty)");
        }
        switch (descriptor.charAt(startPos)) {
            case 'V':
                if (canBeVoid) {
                    return startPos + 1;
                } else {
                    throw new IllegalArgumentException(INVALID_DESCRIPTOR + descriptor);
                }
            case 'Z':
            case 'C':
            case 'B':
            case 'S':
            case 'I':
            case 'F':
            case 'J':
            case 'D':
                return startPos + 1;
            case '[':
                int pos = startPos + 1;
                while (pos < descriptor.length() && descriptor.charAt(pos) == '[') {
                    ++pos;
                }
                if (pos < descriptor.length()) {
                    return checkDescriptor(version, descriptor, pos, false);
                } else {
                    throw new IllegalArgumentException(INVALID_DESCRIPTOR + descriptor);
                }
            case 'L':
                int endPos = descriptor.indexOf(';', startPos);
                if (startPos == -1 || endPos - startPos < 2) {
                    throw new IllegalArgumentException(INVALID_DESCRIPTOR + descriptor);
                }
                try {
                    checkInternalClassName(version, descriptor.substring(startPos + 1, endPos), null);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(INVALID_DESCRIPTOR + descriptor, e);
                }
                return endPos + 1;
            default:
                throw new IllegalArgumentException(INVALID_DESCRIPTOR + descriptor);
        }
    }

    /**
      * Checks that the given string is a valid method descriptor.
      *
      * @param version the class version.
      * @param descriptor the string to be checked.
      */
    static void checkMethodDescriptor(final int version, final String descriptor) {
        if (descriptor == null || descriptor.length() == 0) {
            throw new IllegalArgumentException("Invalid method descriptor (must not be null or empty)");
        }
        if (descriptor.charAt(0) != '(' || descriptor.length() < 3) {
            throw new IllegalArgumentException(INVALID_DESCRIPTOR + descriptor);
        }
        int pos = 1;
        if (descriptor.charAt(pos) != ')') {
            do {
                if (descriptor.charAt(pos) == 'V') {
                    throw new IllegalArgumentException(INVALID_DESCRIPTOR + descriptor);
                }
                pos = checkDescriptor(version, descriptor, pos, false);
            } while (pos < descriptor.length() && descriptor.charAt(pos) != ')');
        }
        pos = checkDescriptor(version, descriptor, pos + 1, true);
        if (pos != descriptor.length()) {
            throw new IllegalArgumentException(INVALID_DESCRIPTOR + descriptor);
        }
    }

    /**
      * Checks that the given label is not null. This method can also check that the label has been
      * visited.
      *
      * @param label the label to be checked.
      * @param checkVisited whether to check that the label has been visited.
      * @param message the message to use in case of error.
      */
    private void checkLabel(final Label label, final boolean checkVisited, final String message) {
        if (label == null) {
            throw new IllegalArgumentException(INVALID + message + " (must not be null)");
        }
        if (checkVisited && labelInsnIndices.get(label) == null) {
            throw new IllegalArgumentException(INVALID + message + " (must be visited first)");
        }
    }
}
