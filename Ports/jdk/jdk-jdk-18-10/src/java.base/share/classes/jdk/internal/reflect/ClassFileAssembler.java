/*
 * Copyright (c) 2001, 2004, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.reflect;

class ClassFileAssembler implements ClassFileConstants {
    private ByteVector vec;
    private short cpIdx = 0;

    public ClassFileAssembler() {
        this(ByteVectorFactory.create());
    }

    public ClassFileAssembler(ByteVector vec) {
        this.vec = vec;
    }

    public ByteVector getData() {
        return vec;
    }

    /** Length in bytes */
    public short getLength() {
        return (short) vec.getLength();
    }

    public void emitMagicAndVersion() {
        emitInt(0xCAFEBABE);
        emitShort((short) 0);
        emitShort((short) 49);
    }

    public void emitInt(int val) {
        emitByte((byte) (val >> 24));
        emitByte((byte) ((val >> 16) & 0xFF));
        emitByte((byte) ((val >> 8) & 0xFF));
        emitByte((byte) (val & 0xFF));
    }

    public void emitShort(short val) {
        emitByte((byte) ((val >> 8) & 0xFF));
        emitByte((byte) (val & 0xFF));
    }

    // Support for labels; package-private
    void emitShort(short bci, short val) {
        vec.put(bci,     (byte) ((val >> 8) & 0xFF));
        vec.put(bci + 1, (byte) (val & 0xFF));
    }

    public void emitByte(byte val) {
        vec.add(val);
    }

    public void append(ClassFileAssembler asm) {
        append(asm.vec);
    }

    public void append(ByteVector vec) {
        for (int i = 0; i < vec.getLength(); i++) {
            emitByte(vec.get(i));
        }
    }

    /** Keeps track of the current (one-based) constant pool index;
        incremented after emitting one of the following constant pool
        entries. Can fetch the current constant pool index for use in
        later entries.  Index points at the last valid constant pool
        entry; initially invalid. It is illegal to fetch the constant
        pool index before emitting at least one constant pool entry. */
    public short cpi() {
        if (cpIdx == 0) {
            throw new RuntimeException("Illegal use of ClassFileAssembler");
        }
        return cpIdx;
    }

    public void emitConstantPoolUTF8(String str) {
        // NOTE: can not use str.getBytes("UTF-8") here because of
        // bootstrapping issues with the character set converters.
        byte[] bytes = UTF8.encode(str);
        emitByte(CONSTANT_Utf8);
        emitShort((short) bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            emitByte(bytes[i]);
        }
        cpIdx++;
    }

    public void emitConstantPoolClass(short index) {
        emitByte(CONSTANT_Class);
        emitShort(index);
        cpIdx++;
    }

    public void emitConstantPoolNameAndType(short nameIndex, short typeIndex) {
        emitByte(CONSTANT_NameAndType);
        emitShort(nameIndex);
        emitShort(typeIndex);
        cpIdx++;
    }

    public void emitConstantPoolFieldref
        (short classIndex, short nameAndTypeIndex)
    {
        emitByte(CONSTANT_Fieldref);
        emitShort(classIndex);
        emitShort(nameAndTypeIndex);
        cpIdx++;
    }

    public void emitConstantPoolMethodref
        (short classIndex, short nameAndTypeIndex)
    {
        emitByte(CONSTANT_Methodref);
        emitShort(classIndex);
        emitShort(nameAndTypeIndex);
        cpIdx++;
    }

    public void emitConstantPoolInterfaceMethodref
        (short classIndex, short nameAndTypeIndex)
    {
        emitByte(CONSTANT_InterfaceMethodref);
        emitShort(classIndex);
        emitShort(nameAndTypeIndex);
        cpIdx++;
    }

    public void emitConstantPoolString(short utf8Index) {
        emitByte(CONSTANT_String);
        emitShort(utf8Index);
        cpIdx++;
    }

    //----------------------------------------------------------------------
    // Opcodes. Keeps track of maximum stack and locals. Make a new
    // assembler for each piece of assembled code, then append the
    // result to the previous assembler's class file.
    //

    private int stack     = 0;
    private int maxStack  = 0;
    private int maxLocals = 0;

    private void incStack() {
        setStack(stack + 1);
    }

    private void decStack() {
        --stack;
    }

    public short getMaxStack() {
        return (short) maxStack;
    }

    public short getMaxLocals() {
        return (short) maxLocals;
    }

    /** It's necessary to be able to specify the number of arguments at
        the beginning of the method (which translates to the initial
        value of max locals) */
    public void setMaxLocals(int maxLocals) {
        this.maxLocals = maxLocals;
    }

    /** Needed to do flow control. Returns current stack depth. */
    public int getStack() {
        return stack;
    }

    /** Needed to do flow control. */
    public void setStack(int value) {
        stack = value;
        if (stack > maxStack) {
            maxStack = stack;
        }
    }

    ///////////////
    // Constants //
    ///////////////

    public void opc_aconst_null() {
        emitByte(opc_aconst_null);
        incStack();
    }

    public void opc_sipush(short constant) {
        emitByte(opc_sipush);
        emitShort(constant);
        incStack();
    }

    public void opc_ldc(byte cpIdx) {
        emitByte(opc_ldc);
        emitByte(cpIdx);
        incStack();
    }

    /////////////////////////////////////
    // Local variable loads and stores //
    /////////////////////////////////////

    public void opc_iload_0() {
        emitByte(opc_iload_0);
        if (maxLocals < 1) maxLocals = 1;
        incStack();
    }

    public void opc_iload_1() {
        emitByte(opc_iload_1);
        if (maxLocals < 2) maxLocals = 2;
        incStack();
    }

    public void opc_iload_2() {
        emitByte(opc_iload_2);
        if (maxLocals < 3) maxLocals = 3;
        incStack();
    }

    public void opc_iload_3() {
        emitByte(opc_iload_3);
        if (maxLocals < 4) maxLocals = 4;
        incStack();
    }

    public void opc_lload_0() {
        emitByte(opc_lload_0);
        if (maxLocals < 2) maxLocals = 2;
        incStack();
        incStack();
    }

    public void opc_lload_1() {
        emitByte(opc_lload_1);
        if (maxLocals < 3) maxLocals = 3;
        incStack();
        incStack();
    }

    public void opc_lload_2() {
        emitByte(opc_lload_2);
        if (maxLocals < 4) maxLocals = 4;
        incStack();
        incStack();
    }

    public void opc_lload_3() {
        emitByte(opc_lload_3);
        if (maxLocals < 5) maxLocals = 5;
        incStack();
        incStack();
    }

    public void opc_fload_0() {
        emitByte(opc_fload_0);
        if (maxLocals < 1) maxLocals = 1;
        incStack();
    }

    public void opc_fload_1() {
        emitByte(opc_fload_1);
        if (maxLocals < 2) maxLocals = 2;
        incStack();
    }

    public void opc_fload_2() {
        emitByte(opc_fload_2);
        if (maxLocals < 3) maxLocals = 3;
        incStack();
    }

    public void opc_fload_3() {
        emitByte(opc_fload_3);
        if (maxLocals < 4) maxLocals = 4;
        incStack();
    }

    public void opc_dload_0() {
        emitByte(opc_dload_0);
        if (maxLocals < 2) maxLocals = 2;
        incStack();
        incStack();
    }

    public void opc_dload_1() {
        emitByte(opc_dload_1);
        if (maxLocals < 3) maxLocals = 3;
        incStack();
        incStack();
    }

    public void opc_dload_2() {
        emitByte(opc_dload_2);
        if (maxLocals < 4) maxLocals = 4;
        incStack();
        incStack();
    }

    public void opc_dload_3() {
        emitByte(opc_dload_3);
        if (maxLocals < 5) maxLocals = 5;
        incStack();
        incStack();
    }

    public void opc_aload_0() {
        emitByte(opc_aload_0);
        if (maxLocals < 1) maxLocals = 1;
        incStack();
    }

    public void opc_aload_1() {
        emitByte(opc_aload_1);
        if (maxLocals < 2) maxLocals = 2;
        incStack();
    }

    public void opc_aload_2() {
        emitByte(opc_aload_2);
        if (maxLocals < 3) maxLocals = 3;
        incStack();
    }

    public void opc_aload_3() {
        emitByte(opc_aload_3);
        if (maxLocals < 4) maxLocals = 4;
        incStack();
    }

    public void opc_aaload() {
        emitByte(opc_aaload);
        decStack();
    }

    public void opc_astore_0() {
        emitByte(opc_astore_0);
        if (maxLocals < 1) maxLocals = 1;
        decStack();
    }

    public void opc_astore_1() {
        emitByte(opc_astore_1);
        if (maxLocals < 2) maxLocals = 2;
        decStack();
    }

    public void opc_astore_2() {
        emitByte(opc_astore_2);
        if (maxLocals < 3) maxLocals = 3;
        decStack();
    }

    public void opc_astore_3() {
        emitByte(opc_astore_3);
        if (maxLocals < 4) maxLocals = 4;
        decStack();
    }

    ////////////////////////
    // Stack manipulation //
    ////////////////////////

    public void opc_pop() {
        emitByte(opc_pop);
        decStack();
    }

    public void opc_dup() {
        emitByte(opc_dup);
        incStack();
    }

    public void opc_dup_x1() {
        emitByte(opc_dup_x1);
        incStack();
    }

    public void opc_swap() {
        emitByte(opc_swap);
    }

    ///////////////////////////////
    // Widening conversions only //
    ///////////////////////////////

    public void opc_i2l() {
        emitByte(opc_i2l);
    }

    public void opc_i2f() {
        emitByte(opc_i2f);
    }

    public void opc_i2d() {
        emitByte(opc_i2d);
    }

    public void opc_l2f() {
        emitByte(opc_l2f);
    }

    public void opc_l2d() {
        emitByte(opc_l2d);
    }

    public void opc_f2d() {
        emitByte(opc_f2d);
    }

    //////////////////
    // Control flow //
    //////////////////

    public void opc_ifeq(short bciOffset) {
        emitByte(opc_ifeq);
        emitShort(bciOffset);
        decStack();
    }

    /** Control flow with forward-reference BCI. Stack assumes
        straight-through control flow. */
    public void opc_ifeq(Label l) {
        short instrBCI = getLength();
        emitByte(opc_ifeq);
        l.add(this, instrBCI, getLength(), getStack() - 1);
        emitShort((short) -1); // Must be patched later
    }

    public void opc_if_icmpeq(short bciOffset) {
        emitByte(opc_if_icmpeq);
        emitShort(bciOffset);
        setStack(getStack() - 2);
    }

    /** Control flow with forward-reference BCI. Stack assumes straight
        control flow. */
    public void opc_if_icmpeq(Label l) {
        short instrBCI = getLength();
        emitByte(opc_if_icmpeq);
        l.add(this, instrBCI, getLength(), getStack() - 2);
        emitShort((short) -1); // Must be patched later
    }

    public void opc_goto(short bciOffset) {
        emitByte(opc_goto);
        emitShort(bciOffset);
    }

    /** Control flow with forward-reference BCI. Stack assumes straight
        control flow. */
    public void opc_goto(Label l) {
        short instrBCI = getLength();
        emitByte(opc_goto);
        l.add(this, instrBCI, getLength(), getStack());
        emitShort((short) -1); // Must be patched later
    }

    public void opc_ifnull(short bciOffset) {
        emitByte(opc_ifnull);
        emitShort(bciOffset);
        decStack();
    }

    /** Control flow with forward-reference BCI. Stack assumes straight
        control flow. */
    public void opc_ifnull(Label l) {
        short instrBCI = getLength();
        emitByte(opc_ifnull);
        l.add(this, instrBCI, getLength(), getStack() - 1);
        emitShort((short) -1); // Must be patched later
        decStack();
    }

    public void opc_ifnonnull(short bciOffset) {
        emitByte(opc_ifnonnull);
        emitShort(bciOffset);
        decStack();
    }

    /** Control flow with forward-reference BCI. Stack assumes straight
        control flow. */
    public void opc_ifnonnull(Label l) {
        short instrBCI = getLength();
        emitByte(opc_ifnonnull);
        l.add(this, instrBCI, getLength(), getStack() - 1);
        emitShort((short) -1); // Must be patched later
        decStack();
    }

    /////////////////////////
    // Return instructions //
    /////////////////////////

    public void opc_ireturn() {
        emitByte(opc_ireturn);
        setStack(0);
    }

    public void opc_lreturn() {
        emitByte(opc_lreturn);
        setStack(0);
    }

    public void opc_freturn() {
        emitByte(opc_freturn);
        setStack(0);
    }

    public void opc_dreturn() {
        emitByte(opc_dreturn);
        setStack(0);
    }

    public void opc_areturn() {
        emitByte(opc_areturn);
        setStack(0);
    }

    public void opc_return() {
        emitByte(opc_return);
        setStack(0);
    }

    //////////////////////
    // Field operations //
    //////////////////////

    public void opc_getstatic(short fieldIndex, int fieldSizeInStackSlots) {
        emitByte(opc_getstatic);
        emitShort(fieldIndex);
        setStack(getStack() + fieldSizeInStackSlots);
    }

    public void opc_putstatic(short fieldIndex, int fieldSizeInStackSlots) {
        emitByte(opc_putstatic);
        emitShort(fieldIndex);
        setStack(getStack() - fieldSizeInStackSlots);
    }

    public void opc_getfield(short fieldIndex, int fieldSizeInStackSlots) {
        emitByte(opc_getfield);
        emitShort(fieldIndex);
        setStack(getStack() + fieldSizeInStackSlots - 1);
    }

    public void opc_putfield(short fieldIndex, int fieldSizeInStackSlots) {
        emitByte(opc_putfield);
        emitShort(fieldIndex);
        setStack(getStack() - fieldSizeInStackSlots - 1);
    }

    ////////////////////////
    // Method invocations //
    ////////////////////////

    /** Long and double arguments and return types count as 2 arguments;
        other values count as 1. */
    public void opc_invokevirtual(short methodIndex,
                                  int numArgs,
                                  int numReturnValues)
    {
        emitByte(opc_invokevirtual);
        emitShort(methodIndex);
        setStack(getStack() - numArgs - 1 + numReturnValues);
    }

    /** Long and double arguments and return types count as 2 arguments;
        other values count as 1. */
    public void opc_invokespecial(short methodIndex,
                                  int numArgs,
                                  int numReturnValues)
    {
        emitByte(opc_invokespecial);
        emitShort(methodIndex);
        setStack(getStack() - numArgs - 1 + numReturnValues);
    }

    /** Long and double arguments and return types count as 2 arguments;
        other values count as 1. */
    public void opc_invokestatic(short methodIndex,
                                 int numArgs,
                                 int numReturnValues)
    {
        emitByte(opc_invokestatic);
        emitShort(methodIndex);
        setStack(getStack() - numArgs + numReturnValues);
    }

    /** Long and double arguments and return types count as 2 arguments;
        other values count as 1. */
    public void opc_invokeinterface(short methodIndex,
                                    int numArgs,
                                    byte count,
                                    int numReturnValues)
    {
        emitByte(opc_invokeinterface);
        emitShort(methodIndex);
        emitByte(count);
        emitByte((byte) 0);
        setStack(getStack() - numArgs - 1 + numReturnValues);
    }

    //////////////////
    // Array length //
    //////////////////

    public void opc_arraylength() {
        emitByte(opc_arraylength);
    }

    /////////
    // New //
    /////////

    public void opc_new(short classIndex) {
        emitByte(opc_new);
        emitShort(classIndex);
        incStack();
    }

    ////////////
    // Athrow //
    ////////////

    public void opc_athrow() {
        emitByte(opc_athrow);
        setStack(1);
    }

    //////////////////////////////
    // Checkcast and instanceof //
    //////////////////////////////

    /** Assumes the checkcast succeeds */
    public void opc_checkcast(short classIndex) {
        emitByte(opc_checkcast);
        emitShort(classIndex);
    }

    public void opc_instanceof(short classIndex) {
        emitByte(opc_instanceof);
        emitShort(classIndex);
    }
}
