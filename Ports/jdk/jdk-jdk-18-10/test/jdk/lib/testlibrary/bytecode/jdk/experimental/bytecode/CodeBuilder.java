/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package jdk.experimental.bytecode;

import jdk.experimental.bytecode.PoolHelper.StaticArgListBuilder;

import java.lang.invoke.MethodHandle;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Builder for class file code attributes. A code attribute is defined:
 * <pre>
 * {@code
 * Code_attribute {
 *    u2 attribute_name_index;
 *    u4 attribute_length;
 *    u2 max_stack;
 *    u2 max_locals;
 *    u4 code_length;
 *    u1 code[code_length];
 *    u2 exception_table_length;
 *    {   u2 start_pc;
 *        u2 end_pc;
 *        u2 handler_pc;
 *        u2 catch_type;
 *    } exception_table[exception_table_length];
 *    u2 attributes_count;
 *    attribute_info attributes[attributes_count];
 * } }
 * </pre>
 *
 * @param <S> the type of symbol representation
 * @param <T> the type of type descriptors representation
 * @param <E> the type of pool entries
 * @param <C> the type of this code builder
 */
public class CodeBuilder<S, T, E, C extends CodeBuilder<S, T, E, C>> extends AttributeBuilder<S, T, E, C> {

    protected GrowableByteBuffer code = new GrowableByteBuffer();
    GrowableByteBuffer catchers = new GrowableByteBuffer();
    GrowableByteBuffer stackmaps = new GrowableByteBuffer();
    MethodBuilder<S, T, E> methodBuilder;
    int ncatchers;
    int stacksize = -1;
    int localsize = -1;
    int nstackmaps = 0;

    public enum JumpMode {
        NARROW,
        WIDE;
    }

    CodeBuilder(MethodBuilder<S, T, E> methodBuilder) {
        super(methodBuilder.poolHelper, methodBuilder.typeHelper);
        this.methodBuilder = methodBuilder;
    }

    public C getstatic(S owner, CharSequence name, T type) {
        emitOp(Opcode.GETSTATIC, type);
        code.writeChar(poolHelper.putFieldRef(owner, name, type));
        return thisBuilder();
    }

    public C putstatic(S owner, CharSequence name, T type) {
        emitOp(Opcode.PUTSTATIC, type);
        code.writeChar(poolHelper.putFieldRef(owner, name, type));
        return thisBuilder();
    }

    public C getfield(S owner, CharSequence name, T type) {
        emitOp(Opcode.GETFIELD, type);
        code.writeChar(poolHelper.putFieldRef(owner, name, type));
        return thisBuilder();
    }

    public C vgetfield(S owner, CharSequence name, T type) {
        emitOp(Opcode.VGETFIELD, type);
        code.writeChar(poolHelper.putFieldRef(owner, name, type));
        return thisBuilder();
    }

    public C putfield(S owner, CharSequence name, T type) {
        emitOp(Opcode.PUTFIELD, type);
        code.writeChar(poolHelper.putFieldRef(owner, name, type));
        return thisBuilder();
    }

    public C invokevirtual(S owner, CharSequence name, T type, boolean isInterface) {
        emitOp(Opcode.INVOKEVIRTUAL, type);
        code.writeChar(poolHelper.putMethodRef(owner, name, type, isInterface));
        return thisBuilder();
    }

    public C invokespecial(S owner, CharSequence name, T type, boolean isInterface) {
        emitOp(Opcode.INVOKESPECIAL, type);
        code.writeChar(poolHelper.putMethodRef(owner, name, type, isInterface));
        return thisBuilder();
    }

    public C invokestatic(S owner, CharSequence name, T type, boolean isInterface) {
        emitOp(Opcode.INVOKESTATIC, type);
        code.writeChar(poolHelper.putMethodRef(owner, name, type, isInterface));
        return thisBuilder();
    }

    public C invokeinterface(S owner, CharSequence name, T type) {
        emitOp(Opcode.INVOKEINTERFACE, type);
        code.writeChar(poolHelper.putMethodRef(owner, name, type, true));
        int nargs = 1;
        Iterator<T> it = typeHelper.parameterTypes(type);
        while (it.hasNext()) {
            nargs += typeHelper.tag(it.next()).width;
        }
        code.writeByte(nargs);
        code.writeByte(0);
        return thisBuilder();
    }

    public C invokedynamic(CharSequence invokedName, T invokedType, S bsmClass, CharSequence bsmName, T bsmType, Consumer<StaticArgListBuilder<S, T, E>> staticArgs) {
        emitOp(Opcode.INVOKEDYNAMIC, invokedType);
        code.writeChar(poolHelper.putInvokeDynamic(invokedName, invokedType, bsmClass, bsmName, bsmType, staticArgs));
        code.writeChar(0); //padding
        return thisBuilder();
    }

    public C new_(S clazz) {
        emitOp(Opcode.NEW, clazz);
        code.writeChar(poolHelper.putClass(clazz));
        return thisBuilder();
    }

    public C vnew_(S clazz, CharSequence name, T desc) {
        emitOp(Opcode.VNEW, clazz);
        code.writeChar(poolHelper.putMethodRef(clazz, name, desc, false));
        return thisBuilder();
    }

    public C vnewarray(S array) {
        emitOp(Opcode.VNEWARRAY, array);
        code.writeChar(poolHelper.putClass(array));
        return thisBuilder();
    }

    public C newarray(TypeTag tag) {
        emitOp(Opcode.NEWARRAY, tag);
        int newarraycode = tag.newarraycode;
        if (newarraycode == -1) {
            throw new IllegalStateException("Bad tag " + tag);
        }
        code.writeByte(newarraycode);
        return thisBuilder();
    }

    public C anewarray(S array) {
        emitOp(Opcode.ANEWARRAY, array);
        code.writeChar(poolHelper.putClass(array));
        return thisBuilder();
    }

    public C checkcast(S target) {
        emitOp(Opcode.CHECKCAST);
        code.writeChar(poolHelper.putClass(target));
        return thisBuilder();
    }

    public C instanceof_(S target) {
        emitOp(Opcode.INSTANCEOF);
        code.writeChar(poolHelper.putClass(target));
        return thisBuilder();
    }

    public C multianewarray(S array, byte dims) {
        emitOp(Opcode.MULTIANEWARRAY, new Object[]{array, dims});
        code.writeChar(poolHelper.putClass(array)).writeByte(dims);
        return thisBuilder();
    }

    public C multivnewarray(S array, byte dims) {
        emitOp(Opcode.MULTIVNEWARRAY, new Object[]{array, dims});
        code.writeChar(poolHelper.putClass(array)).writeByte(dims);
        return thisBuilder();
    }

    public C vbox(S target) {
        emitOp(Opcode.VBOX, target);
        code.writeChar(poolHelper.putClass(target));
        return thisBuilder();
    }

    public C vunbox(S target) {
        emitOp(Opcode.VUNBOX, target);
        code.writeChar(poolHelper.putClass(target));
        return thisBuilder();
    }

    public C ldc(int i) {
        return ldc(pool -> pool.putInt(i), false);
    }

    public C ldc(long l) {
        return ldc(pool -> pool.putLong(l), true);
    }

    public C ldc(float f) {
        return ldc(pool -> pool.putFloat(f), false);
    }

    public C ldc(double d) {
        return ldc(pool -> pool.putDouble(d), true);
    }

    public C ldc(String s) {
        return ldc(pool -> pool.putString(s), false);
    }

    public C ldc(CharSequence constName, T constType, S bsmClass, CharSequence bsmName, T bsmType, Consumer<StaticArgListBuilder<S, T, E>> staticArgs) {
        boolean fat = typeHelper.tag(constType).width() == 2;
        return ldc(pool -> pool.putDynamicConstant(constName, constType, bsmClass, bsmName, bsmType, staticArgs), fat);
    }

    public <Z> C ldc(Z z, BiFunction<PoolHelper<S, T, E>, Z, Integer> poolFunc) {
        return ldc(pool -> poolFunc.apply(pool, z), false);
    }

    protected C ldc(ToIntFunction<PoolHelper<S, T, E>> indexFunc, boolean fat) {
        // @@@ This should probably be abstract
        int index = indexFunc.applyAsInt(poolHelper);
        return ldc(index, null, fat);
    }

    protected final C ldc(int index, T type, boolean fat) {
        if (fat) {
            emitOp(Opcode.LDC2_W, type);
            code.writeChar(index);
        } else if (index > 63) {
            emitOp(Opcode.LDC_W, type);
            code.writeChar(index);
        } else {
            emitOp(Opcode.LDC, type);
            code.writeByte(index);
        }
        return thisBuilder();
    }

    //other non-CP dependent opcodes
    public C areturn() {
        return emitOp(Opcode.ARETURN);
    }

    public C ireturn() {
        return emitOp(Opcode.IRETURN);
    }

    public C freturn() {
        return emitOp(Opcode.FRETURN);
    }

    public C lreturn() {
        return emitOp(Opcode.LRETURN);
    }

    public C dreturn() {
        return emitOp(Opcode.DRETURN);
    }

    public C return_() {
        return emitOp(Opcode.RETURN);
    }

    public C vreturn() {
        return emitOp(Opcode.VRETURN);
    }

    protected C emitWideIfNeeded(Opcode opcode, int n) {
        boolean wide = n > Byte.MAX_VALUE;
        if (wide) {
            wide();
        }
        emitOp(opcode, n);
        if (wide) {
            code.writeChar(n);
        } else {
            code.writeByte(n);
        }
        return thisBuilder();
    }

    protected C emitWideIfNeeded(Opcode opcode, int n, int v) {
        boolean wide = n > Byte.MAX_VALUE || v > Byte.MAX_VALUE;
        if (wide) {
            wide();
        }
        emitOp(opcode, n);
        if (wide) {
            code.writeChar(n).writeChar(v);
        } else {
            code.writeByte(n).writeByte(v);
        }
        return thisBuilder();
    }

    public TypedBuilder typed(TypeTag typeTag) {
        return typed(typeTag, _unused -> new TypedBuilder());
    }

    protected <TB extends TypedBuilder> TB typed(TypeTag typeTag, Function<TypeTag, TB> typedBuilderFunc) {
        emitOp(Opcode.TYPED);
        code.writeChar(poolHelper.putType(typeHelper.fromTag(typeTag)));
        return typedBuilderFunc.apply(typeTag);
    }

    public class TypedBuilder {
        public C aload_0() {
            return CodeBuilder.this.aload_0();
        }

        public C aload_1() {
            return CodeBuilder.this.aload_1();
        }

        public C aload_2() {
            return CodeBuilder.this.aload_2();
        }

        public C aload_3() {
            return CodeBuilder.this.aload_3();
        }

        public C aload(int n) {
            return CodeBuilder.this.aload(n);
        }

        public C astore_0() {
            return CodeBuilder.this.astore_0();
        }

        public C astore_1() {
            return CodeBuilder.this.astore_1();
        }

        public C astore_2() {
            return CodeBuilder.this.astore_2();
        }

        public C astore_3() {
            return CodeBuilder.this.astore_3();
        }

        public C astore(int n) {
            return CodeBuilder.this.astore(n);
        }

        public C aaload() {
            return CodeBuilder.this.aaload();
        }

        public C aastore() {
            return CodeBuilder.this.aastore();
        }

        public C areturn() {
            return CodeBuilder.this.areturn();
        }

        public C anewarray(S s) {
            return CodeBuilder.this.anewarray(s);
        }

        public C aconst_null() {
            return CodeBuilder.this.aconst_null();
        }

        public C if_acmpeq(short target) {
            return CodeBuilder.this.if_acmpeq(target);
        }

        public C if_acmpne(short target) {
            return CodeBuilder.this.if_acmpeq(target);
        }
    }

    public C vload(int i) {
        return emitWideIfNeeded(Opcode.VLOAD, i);
    }

    public C aload(int i) {
        return emitWideIfNeeded(Opcode.ALOAD, i);
    }

    public C iload(int i) {
        return emitWideIfNeeded(Opcode.ILOAD, i);
    }

    public C fload(int i) {
        return emitWideIfNeeded(Opcode.FLOAD, i);
    }

    public C lload(int i) {
        return emitWideIfNeeded(Opcode.LLOAD, i);
    }

    public C dload(int i) {
        return emitWideIfNeeded(Opcode.DLOAD, i);
    }

    public C aload_0() {
        return emitOp(Opcode.ALOAD_0);
    }

    public C iload_0() {
        return emitOp(Opcode.ILOAD_0);
    }

    public C fload_0() {
        return emitOp(Opcode.FLOAD_0);
    }

    public C lload_0() {
        return emitOp(Opcode.LLOAD_0);
    }

    public C dload_0() {
        return emitOp(Opcode.DLOAD_0);
    }

    public C aload_1() {
        return emitOp(Opcode.ALOAD_1);
    }

    public C iload_1() {
        return emitOp(Opcode.ILOAD_1);
    }

    public C fload_1() {
        return emitOp(Opcode.FLOAD_1);
    }

    public C lload_1() {
        return emitOp(Opcode.LLOAD_1);
    }

    public C dload_1() {
        return emitOp(Opcode.DLOAD_1);
    }

    public C aload_2() {
        return emitOp(Opcode.ALOAD_2);
    }

    public C iload_2() {
        return emitOp(Opcode.ILOAD_2);
    }

    public C fload_2() {
        return emitOp(Opcode.FLOAD_2);
    }

    public C lload_2() {
        return emitOp(Opcode.LLOAD_2);
    }

    public C dload_2() {
        return emitOp(Opcode.DLOAD_2);
    }

    public C aload_3() {
        return emitOp(Opcode.ALOAD_3);
    }

    public C iload_3() {
        return emitOp(Opcode.ILOAD_3);
    }

    public C fload_3() {
        return emitOp(Opcode.FLOAD_3);
    }

    public C lload_3() {
        return emitOp(Opcode.LLOAD_3);
    }

    public C dload_3() {
        return emitOp(Opcode.DLOAD_3);
    }

    public C vstore(int i) {
        return emitWideIfNeeded(Opcode.VSTORE, i);
    }

    public C astore(int i) {
        return emitWideIfNeeded(Opcode.ASTORE, i);
    }

    public C istore(int i) {
        return emitWideIfNeeded(Opcode.ISTORE, i);
    }

    public C fstore(int i) {
        return emitWideIfNeeded(Opcode.FSTORE, i);
    }

    public C lstore(int i) {
        return emitWideIfNeeded(Opcode.LSTORE, i);
    }

    public C dstore(int i) {
        return emitWideIfNeeded(Opcode.DSTORE, i);
    }

    public C astore_0() {
        return emitOp(Opcode.ASTORE_0);
    }

    public C istore_0() {
        return emitOp(Opcode.ISTORE_0);
    }

    public C fstore_0() {
        return emitOp(Opcode.FSTORE_0);
    }

    public C lstore_0() {
        return emitOp(Opcode.LSTORE_0);
    }

    public C dstore_0() {
        return emitOp(Opcode.DSTORE_0);
    }

    public C astore_1() {
        return emitOp(Opcode.ASTORE_1);
    }

    public C istore_1() {
        return emitOp(Opcode.ISTORE_1);
    }

    public C fstore_1() {
        return emitOp(Opcode.FSTORE_1);
    }

    public C lstore_1() {
        return emitOp(Opcode.LSTORE_1);
    }

    public C dstore_1() {
        return emitOp(Opcode.DSTORE_1);
    }

    public C astore_2() {
        return emitOp(Opcode.ASTORE_2);
    }

    public C istore_2() {
        return emitOp(Opcode.ISTORE_2);
    }

    public C fstore_2() {
        return emitOp(Opcode.FSTORE_2);
    }

    public C lstore_2() {
        return emitOp(Opcode.LSTORE_2);
    }

    public C dstore_2() {
        return emitOp(Opcode.DSTORE_2);
    }

    public C astore_3() {
        return emitOp(Opcode.ASTORE_3);
    }

    public C istore_3() {
        return emitOp(Opcode.ISTORE_3);
    }

    public C fstore_3() {
        return emitOp(Opcode.FSTORE_3);
    }

    public C lstore_3() {
        return emitOp(Opcode.LSTORE_3);
    }

    public C dstore_3() {
        return emitOp(Opcode.DSTORE_3);
    }

    //...

    public C iaload() {
        return emitOp(Opcode.IALOAD);
    }

    public C laload() {
        return emitOp(Opcode.LALOAD);
    }

    public C faload() {
        return emitOp(Opcode.FALOAD);
    }

    public C daload() {
        return emitOp(Opcode.DALOAD);
    }

    public C vaload() {
        return emitOp(Opcode.VALOAD);
    }

    public C aaload() {
        return emitOp(Opcode.AALOAD);
    }

    public C baload() {
        return emitOp(Opcode.BALOAD);
    }

    public C caload() {
        return emitOp(Opcode.CALOAD);
    }

    public C saload() {
        return emitOp(Opcode.SALOAD);
    }

    public C iastore() {
        return emitOp(Opcode.IASTORE);
    }

    public C lastore() {
        return emitOp(Opcode.LASTORE);
    }

    public C fastore() {
        return emitOp(Opcode.FASTORE);
    }

    public C dastore() {
        return emitOp(Opcode.DASTORE);
    }

    public C vastore() {
        return emitOp(Opcode.VASTORE);
    }

    public C aastore() {
        return emitOp(Opcode.AASTORE);
    }

    public C bastore() {
        return emitOp(Opcode.BASTORE);
    }

    public C castore() {
        return emitOp(Opcode.CASTORE);
    }

    public C sastore() {
        return emitOp(Opcode.SASTORE);
    }

    public C nop() {
        return emitOp(Opcode.NOP);
    }

    public C aconst_null() {
        return emitOp(Opcode.ACONST_NULL);
    }

    public C iconst_0() {
        return emitOp(Opcode.ICONST_0);
    }

    public C iconst_1() {
        return emitOp(Opcode.ICONST_1);
    }

    public C iconst_2() {
        return emitOp(Opcode.ICONST_2);
    }

    public C iconst_3() {
        return emitOp(Opcode.ICONST_3);
    }

    public C iconst_4() {
        return emitOp(Opcode.ICONST_4);
    }

    public C iconst_5() {
        return emitOp(Opcode.ICONST_5);
    }

    public C iconst_m1() {
        return emitOp(Opcode.ICONST_M1);
    }

    public C lconst_0() {
        return emitOp(Opcode.LCONST_0);
    }

    public C lconst_1() {
        return emitOp(Opcode.LCONST_1);
    }

    public C fconst_0() {
        return emitOp(Opcode.FCONST_0);
    }

    public C fconst_1() {
        return emitOp(Opcode.FCONST_1);
    }

    public C fconst_2() {
        return emitOp(Opcode.FCONST_2);
    }

    public C dconst_0() {
        return emitOp(Opcode.DCONST_0);
    }

    public C dconst_1() {
        return emitOp(Opcode.DCONST_1);
    }

    public C sipush(int s) {
        emitOp(Opcode.SIPUSH);
        code.writeChar(s);
        return thisBuilder();
    }

    public C bipush(int b) {
        emitOp(Opcode.BIPUSH);
        code.writeByte(b);
        return thisBuilder();
    }

    public C pop() {
        return emitOp(Opcode.POP);
    }

    public C pop2() {
        return emitOp(Opcode.POP2);
    }

    public C dup() {
        return emitOp(Opcode.DUP);
    }

    public C dup_x1() {
        return emitOp(Opcode.DUP_X1);
    }

    public C dup_x2() {
        return emitOp(Opcode.DUP_X2);
    }

    public C dup2() {
        return emitOp(Opcode.DUP2);
    }

    public C dup2_x1() {
        return emitOp(Opcode.DUP2_X1);
    }

    public C dup2_x2() {
        return emitOp(Opcode.DUP2_X2);
    }

    public C swap() {
        return emitOp(Opcode.SWAP);
    }

    public C iadd() {
        return emitOp(Opcode.IADD);
    }

    public C ladd() {
        return emitOp(Opcode.LADD);
    }

    public C fadd() {
        return emitOp(Opcode.FADD);
    }

    public C dadd() {
        return emitOp(Opcode.DADD);
    }

    public C isub() {
        return emitOp(Opcode.ISUB);
    }

    public C lsub() {
        return emitOp(Opcode.LSUB);
    }

    public C fsub() {
        return emitOp(Opcode.FSUB);
    }

    public C dsub() {
        return emitOp(Opcode.DSUB);
    }

    public C imul() {
        return emitOp(Opcode.IMUL);
    }

    public C lmul() {
        return emitOp(Opcode.LMUL);
    }

    public C fmul() {
        return emitOp(Opcode.FMUL);
    }

    public C dmul() {
        return emitOp(Opcode.DMUL);
    }

    public C idiv() {
        return emitOp(Opcode.IDIV);
    }

    public C ldiv() {
        return emitOp(Opcode.LDIV);
    }

    public C fdiv() {
        return emitOp(Opcode.FDIV);
    }

    public C ddiv() {
        return emitOp(Opcode.DDIV);
    }

    public C irem() {
        return emitOp(Opcode.IREM);
    }

    public C lrem() {
        return emitOp(Opcode.LREM);
    }

    public C frem() {
        return emitOp(Opcode.FREM);
    }

    public C drem() {
        return emitOp(Opcode.DREM);
    }

    public C ineg() {
        return emitOp(Opcode.INEG);
    }

    public C lneg() {
        return emitOp(Opcode.LNEG);
    }

    public C fneg() {
        return emitOp(Opcode.FNEG);
    }

    public C dneg() {
        return emitOp(Opcode.DNEG);
    }

    public C ishl() {
        return emitOp(Opcode.ISHL);
    }

    public C lshl() {
        return emitOp(Opcode.LSHL);
    }

    public C ishr() {
        return emitOp(Opcode.ISHR);
    }

    public C lshr() {
        return emitOp(Opcode.LSHR);
    }

    public C iushr() {
        return emitOp(Opcode.IUSHR);
    }

    public C lushr() {
        return emitOp(Opcode.LUSHR);
    }

    public C iand() {
        return emitOp(Opcode.IAND);
    }

    public C land() {
        return emitOp(Opcode.LAND);
    }

    public C ior() {
        return emitOp(Opcode.IOR);
    }

    public C lor() {
        return emitOp(Opcode.LOR);
    }

    public C ixor() {
        return emitOp(Opcode.IXOR);
    }

    public C lxor() {
        return emitOp(Opcode.LXOR);
    }

    public C iinc(int index, int val) {
        return emitWideIfNeeded(Opcode.IINC, index, val);
    }

    public C i2l() {
        return emitOp(Opcode.I2L);
    }

    public C i2f() {
        return emitOp(Opcode.I2F);
    }

    public C i2d() {
        return emitOp(Opcode.I2D);
    }

    public C l2i() {
        return emitOp(Opcode.L2I);
    }

    public C l2f() {
        return emitOp(Opcode.L2F);
    }

    public C l2d() {
        return emitOp(Opcode.L2D);
    }

    public C f2i() {
        return emitOp(Opcode.F2I);
    }

    public C f2l() {
        return emitOp(Opcode.F2L);
    }

    public C f2d() {
        return emitOp(Opcode.F2D);
    }

    public C d2i() {
        return emitOp(Opcode.D2I);
    }

    public C d2l() {
        return emitOp(Opcode.D2L);
    }

    public C d2f() {
        return emitOp(Opcode.D2F);
    }

    public C i2b() {
        return emitOp(Opcode.I2B);
    }

    public C i2c() {
        return emitOp(Opcode.I2C);
    }

    public C i2s() {
        return emitOp(Opcode.I2S);
    }

    public C lcmp() {
        return emitOp(Opcode.LCMP);
    }

    public C fcmpl() {
        return emitOp(Opcode.FCMPL);
    }

    public C fcmpg() {
        return emitOp(Opcode.FCMPG);
    }

    public C dcmpl() {
        return emitOp(Opcode.DCMPL);
    }

    public C dcmpg() {
        return emitOp(Opcode.DCMPG);
    }

    public C ifeq(short target) {
        return emitNarrowJumpOp(Opcode.IFEQ, target);
    }

    public C ifne(short target) {
        return emitNarrowJumpOp(Opcode.IFNE, target);
    }

    public C iflt(short target) {
        return emitNarrowJumpOp(Opcode.IFLT, target);
    }

    public C ifge(short target) {
        return emitNarrowJumpOp(Opcode.IFGE, target);
    }

    public C ifgt(short target) {
        return emitNarrowJumpOp(Opcode.IFGT, target);
    }

    public C ifle(short target) {
        return emitNarrowJumpOp(Opcode.IFLE, target);
    }

    public C if_icmpeq(short target) {
        return emitNarrowJumpOp(Opcode.IF_ICMPEQ, target);
    }

    public C if_icmpne(short target) {
        return emitNarrowJumpOp(Opcode.IF_ICMPNE, target);
    }

    public C if_icmplt(short target) {
        return emitNarrowJumpOp(Opcode.IF_ICMPLT, target);
    }

    public C if_icmpge(short target) {
        return emitNarrowJumpOp(Opcode.IF_ICMPGE, target);
    }

    public C if_icmpgt(short target) {
        return emitNarrowJumpOp(Opcode.IF_ICMPGT, target);
    }

    public C if_icmple(short target) {
        return emitNarrowJumpOp(Opcode.IF_ICMPLE, target);
    }

    public C if_acmpeq(short target) {
        return emitNarrowJumpOp(Opcode.IF_ACMPEQ, target);
    }

    public C if_acmpne(short target) {
        return emitNarrowJumpOp(Opcode.IF_ACMPNE, target);
    }

    public C goto_(short target) {
        return emitNarrowJumpOp(Opcode.GOTO_, target);
    }

    public C jsr(short target) {
        return emitNarrowJumpOp(Opcode.JSR, target);
    }

    public C ret(int index) {
        return emitWideIfNeeded(Opcode.RET, index);
    }

    public C tableswitch(int low, int high, int defaultTarget, int... targets) {
        if (high - low + 1 != targets.length) throw new IllegalStateException("Bad targets length");
        emitOp(Opcode.TABLESWITCH);
        //padding
        int start = code.offset;
        if ((start % 4) != 0) {
            //add padding
            for (int i = 0; i < 4 - (start % 4); i++) {
                code.writeByte(0);
            }
        }
        code.writeInt(defaultTarget)
                .writeInt(low)
                .writeInt(high);
        for (int target : targets) {
            code.writeInt(target);
        }
        return thisBuilder();
    }

    public C lookupswitch(int defaultTarget, int... npairs) {
        if (npairs.length % 2 != 0) throw new IllegalStateException("Bad npairs length");
        emitOp(Opcode.LOOKUPSWITCH);
        //padding
        int start = code.offset;
        for (int i = 0; i < (4 - (start % 4)); i++) {
            code.writeByte(0);
        }
        code.writeInt(defaultTarget)
                .writeInt(npairs.length / 2);
        for (int i = 0; i < npairs.length; i += 2) {
            code.writeInt(npairs[i]);
            code.writeInt(npairs[i + 1]);
        }
        return thisBuilder();
    }

    public C arraylength() {
        return emitOp(Opcode.ARRAYLENGTH);
    }

    public C athrow() {
        return emitOp(Opcode.ATHROW);
    }

    public C monitorenter() {
        return emitOp(Opcode.MONITORENTER);
    }

    public C monitorexit() {
        return emitOp(Opcode.MONITOREXIT);
    }

    public C wide() {
        return emitOp(Opcode.WIDE);
    }

    public C if_null(short offset) {
        return emitNarrowJumpOp(Opcode.IF_NULL, offset);
    }

    public C if_nonnull(short offset) {
        return emitNarrowJumpOp(Opcode.IF_NONNULL, offset);
    }

    public C goto_w(int target) {
        return emitWideJumpOp(Opcode.GOTO_W, target);
    }

    public C jsr_w(int target) {
        return emitWideJumpOp(Opcode.JSR_W, target);
    }

    public C withCatch(S type, int start, int end, int offset) {
        catchers.writeChar(start);
        catchers.writeChar(end);
        catchers.writeChar(offset);
        catchers.writeChar(type != null ? poolHelper.putClass(type) : 0);
        ncatchers++;
        return thisBuilder();
    }

    public C withLocalSize(int localsize) {
        this.localsize = localsize;
        return thisBuilder();
    }

    public C withStackSize(int stacksize) {
        this.stacksize = stacksize;
        return thisBuilder();
    }

    protected int localsize() {
        return localsize;
    }

    void build(GrowableByteBuffer buf) {
        buf.writeChar(stacksize); //max stack size
        buf.writeChar(localsize()); //max locals
        buf.writeInt(code.offset);
        buf.writeBytes(code);
        buf.writeChar(ncatchers);
        buf.writeBytes(catchers);
        buf.writeChar(nattrs); //attributes
        buf.writeBytes(attributes);
    }

    byte[] build() {
        GrowableByteBuffer buf = new GrowableByteBuffer();
        build(buf);
        return buf.bytes();
    }

    protected C emitNarrowJumpOp(Opcode opcode, short target) {
        emitOp(opcode);
        emitOffset(code, JumpMode.NARROW, target);
        return thisBuilder();
    }

    protected C emitWideJumpOp(Opcode opcode, int target) {
        emitOp(opcode);
        emitOffset(code, JumpMode.WIDE, target);
        return thisBuilder();
    }

    protected C emitOp(Opcode opcode) {
        return emitOp(opcode, null);
    }

    protected C emitOp(Opcode opcode, Object optPoolValue) {
        code.writeByte(opcode.code);
        return thisBuilder();
    }

    protected void emitOffset(GrowableByteBuffer buf, JumpMode jumpMode, int offset) {
        if (jumpMode == JumpMode.NARROW) {
            buf.writeChar((short) offset);
        } else {
            buf.writeInt(offset);
        }
    }

    int offset() {
        return code.offset;
    }

    /*** stackmap support ***/

    /**
     * The tags and constants used in compressed stackmap.
     */
    static final int SAME_FRAME_SIZE = 64;
    static final int SAME_LOCALS_1_STACK_ITEM_EXTENDED = 247;
    static final int SAME_FRAME_EXTENDED = 251;
    static final int FULL_FRAME = 255;
    static final int MAX_LOCAL_LENGTH_DIFF = 4;

    @SuppressWarnings("unchecked")
    private void writeStackMapType(T t) {
        if (t == null) {
            stackmaps.writeByte(0);
        } else {
            switch (typeHelper.tag(t)) {
                case B:
                case C:
                case S:
                case I:
                case Z:
                    stackmaps.writeByte(1);
                    break;
                case F:
                    stackmaps.writeByte(2);
                    break;
                case D:
                    stackmaps.writeByte(3);
                    break;
                case J:
                    stackmaps.writeByte(4);
                    break;
                case A:
                    if (t == typeHelper.nullType()) {
                        stackmaps.writeByte(5); //null
                    } else {
                        //TODO: uninit this, top?
                        stackmaps.writeByte(7);
                        stackmaps.writeChar(poolHelper.putClass(typeHelper.symbol(t)));
                    }
                    break;
                default:
                    throw new IllegalStateException("Bad type");
            }
        }
    }

    public void sameFrame(int offsetDelta) {
        int frameType = (offsetDelta < SAME_FRAME_SIZE) ?
                offsetDelta : SAME_FRAME_EXTENDED;
        stackmaps.writeByte(frameType);
        if (frameType == SAME_FRAME_EXTENDED) {
            stackmaps.writeChar(offsetDelta);
        }
    }

    public void sameLocals1StackItemFrame(int offsetDelta, T stackItem) {
        int frameType = (offsetDelta < SAME_FRAME_SIZE) ?
                (SAME_FRAME_SIZE + offsetDelta) : SAME_LOCALS_1_STACK_ITEM_EXTENDED;
        stackmaps.writeByte(frameType);
        if (frameType == SAME_LOCALS_1_STACK_ITEM_EXTENDED) {
            stackmaps.writeChar(offsetDelta);
        }
        writeStackMapType(stackItem);
    }

    public void appendFrame(int offsetDelta, int prevLocalsSize, List<T> locals) {
        int frameType = SAME_FRAME_EXTENDED + (locals.size() - prevLocalsSize);
        stackmaps.writeByte(frameType);
        stackmaps.writeChar(offsetDelta);
        for (int i = prevLocalsSize; i < locals.size(); i++) {
            writeStackMapType(locals.get(i));
        }
    }

    public void chopFrame(int offsetDelta, int droppedVars) {
        int frameType = SAME_FRAME_EXTENDED - droppedVars;
        stackmaps.writeByte(frameType);
        stackmaps.writeChar(offsetDelta);
    }

    public void fullFrame(int offsetDelta, List<T> locals, List<T> stackItems) {
        stackmaps.writeByte(FULL_FRAME);
        stackmaps.writeChar(offsetDelta);
        stackmaps.writeChar(locals.size());
        for (T local : locals) {
            writeStackMapType(local);
        }

        stackmaps.writeChar(stackItems.size());
        for (T stackType : stackItems) {
            writeStackMapType(stackType);
        }
    }
}
