/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.bcel.internal.classfile;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import com.sun.org.apache.bcel.internal.Const;

/**
 * This class represents a stack map entry recording the types of
 * local variables and the the of stack items at a given byte code offset.
 * See CLDC specification 5.3.1.2
 *
 * @see     StackMap
 * @see     StackMapType
 */
public final class StackMapEntry implements Node, Cloneable
{

    private int frameType;
    private int byteCodeOffset;
    private StackMapType[] typesOfLocals;
    private StackMapType[] typesOfStackItems;
    private ConstantPool constantPool;


    /**
     * Construct object from input stream.
     *
     * @param input Input stream
     * @throws IOException
     */
    StackMapEntry(final DataInput input, final ConstantPool constantPool) throws IOException {
        this(input.readByte() & 0xFF, -1, null, null, constantPool);

        if (frameType >= Const.SAME_FRAME && frameType <= Const.SAME_FRAME_MAX) {
            byteCodeOffset = frameType - Const.SAME_FRAME;
        } else if (frameType >= Const.SAME_LOCALS_1_STACK_ITEM_FRAME &&
                   frameType <= Const.SAME_LOCALS_1_STACK_ITEM_FRAME_MAX) {
            byteCodeOffset = frameType - Const.SAME_LOCALS_1_STACK_ITEM_FRAME;
            typesOfStackItems = new StackMapType[1];
            typesOfStackItems[0] = new StackMapType(input, constantPool);
        } else if (frameType == Const.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) {
            byteCodeOffset = input.readShort();
            typesOfStackItems = new StackMapType[1];
            typesOfStackItems[0] = new StackMapType(input, constantPool);
        } else if (frameType >= Const.CHOP_FRAME && frameType <= Const.CHOP_FRAME_MAX) {
            byteCodeOffset = input.readShort();
        } else if (frameType == Const.SAME_FRAME_EXTENDED) {
            byteCodeOffset = input.readShort();
        } else if (frameType >= Const.APPEND_FRAME && frameType <= Const.APPEND_FRAME_MAX) {
            byteCodeOffset = input.readShort();
            final int number_of_locals = frameType - 251;
            typesOfLocals = new StackMapType[number_of_locals];
            for (int i = 0; i < number_of_locals; i++) {
                typesOfLocals[i] = new StackMapType(input, constantPool);
            }
        } else if (frameType == Const.FULL_FRAME) {
            byteCodeOffset = input.readShort();
            final int number_of_locals = input.readShort();
            typesOfLocals = new StackMapType[number_of_locals];
            for (int i = 0; i < number_of_locals; i++) {
                typesOfLocals[i] = new StackMapType(input, constantPool);
            }
            final int number_of_stack_items = input.readShort();
            typesOfStackItems = new StackMapType[number_of_stack_items];
            for (int i = 0; i < number_of_stack_items; i++) {
                typesOfStackItems[i] = new StackMapType(input, constantPool);
            }
        } else {
            /* Can't happen */
            throw new ClassFormatException ("Invalid frame type found while parsing stack map table: " + frameType);
        }
    }

    /**
     * DO NOT USE
     *
     * @param byteCodeOffset
     * @param numberOfLocals NOT USED
     * @param typesOfLocals array of {@link StackMapType}s of locals
     * @param numberOfStackItems NOT USED
     * @param typesOfStackItems array ot {@link StackMapType}s of stack items
     * @param constantPool the constant pool
     * @deprecated Since 6.0, use {@link #StackMapEntry(int, int, StackMapType[], StackMapType[], ConstantPool)}
     * instead
     */
    @java.lang.Deprecated
    public StackMapEntry(final int byteCodeOffset, final int numberOfLocals,
            final StackMapType[] typesOfLocals, final int numberOfStackItems,
            final StackMapType[] typesOfStackItems, final ConstantPool constantPool) {
        this.byteCodeOffset = byteCodeOffset;
        this.typesOfLocals = typesOfLocals != null ? typesOfLocals : new StackMapType[0];
        this.typesOfStackItems = typesOfStackItems != null ? typesOfStackItems : new StackMapType[0];
        this.constantPool = constantPool;
    }

    /**
     * Create an instance
     *
     * @param tag the frameType to use
     * @param byteCodeOffset
     * @param typesOfLocals array of {@link StackMapType}s of locals
     * @param typesOfStackItems array ot {@link StackMapType}s of stack items
     * @param constantPool the constant pool
     */
    public StackMapEntry(final int tag, final int byteCodeOffset,
            final StackMapType[] typesOfLocals,
            final StackMapType[] typesOfStackItems, final ConstantPool constantPool) {
        this.frameType = tag;
        this.byteCodeOffset = byteCodeOffset;
        this.typesOfLocals = typesOfLocals != null ? typesOfLocals : new StackMapType[0];
        this.typesOfStackItems = typesOfStackItems != null ? typesOfStackItems : new StackMapType[0];
        this.constantPool = constantPool;
    }


    /**
     * Dump stack map entry
     *
     * @param file Output file stream
     * @throws IOException
     */
    public void dump( final DataOutputStream file ) throws IOException {
        file.write(frameType);
        if (frameType >= Const.SAME_FRAME && frameType <= Const.SAME_FRAME_MAX) {
            // nothing to be done
        } else if (frameType >= Const.SAME_LOCALS_1_STACK_ITEM_FRAME &&
                   frameType <= Const.SAME_LOCALS_1_STACK_ITEM_FRAME_MAX) {
            typesOfStackItems[0].dump(file);
        } else if (frameType == Const.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) {
            file.writeShort(byteCodeOffset);
            typesOfStackItems[0].dump(file);
        } else if (frameType >= Const.CHOP_FRAME && frameType <= Const.CHOP_FRAME_MAX) {
            file.writeShort(byteCodeOffset);
        } else if (frameType == Const.SAME_FRAME_EXTENDED) {
            file.writeShort(byteCodeOffset);
        } else if (frameType >= Const.APPEND_FRAME && frameType <= Const.APPEND_FRAME_MAX) {
            file.writeShort(byteCodeOffset);
            for (final StackMapType type : typesOfLocals) {
                type.dump(file);
            }
        } else if (frameType == Const.FULL_FRAME) {
            file.writeShort(byteCodeOffset);
            file.writeShort(typesOfLocals.length);
            for (final StackMapType type : typesOfLocals) {
                type.dump(file);
            }
            file.writeShort(typesOfStackItems.length);
            for (final StackMapType type : typesOfStackItems) {
                type.dump(file);
            }
        } else {
            /* Can't happen */
            throw new ClassFormatException ("Invalid Stack map table tag: " + frameType);
        }
    }


    /**
     * @return String representation.
     */
    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder(64);
        buf.append("(");
        if (frameType >= Const.SAME_FRAME && frameType <= Const.SAME_FRAME_MAX) {
            buf.append("SAME");
        } else if (frameType >= Const.SAME_LOCALS_1_STACK_ITEM_FRAME &&
                  frameType <= Const.SAME_LOCALS_1_STACK_ITEM_FRAME_MAX) {
            buf.append("SAME_LOCALS_1_STACK");
        } else if (frameType == Const.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) {
            buf.append("SAME_LOCALS_1_STACK_EXTENDED");
        } else if (frameType >= Const.CHOP_FRAME && frameType <= Const.CHOP_FRAME_MAX) {
            buf.append("CHOP ").append(String.valueOf(251-frameType));
        } else if (frameType == Const.SAME_FRAME_EXTENDED) {
            buf.append("SAME_EXTENDED");
        } else if (frameType >= Const.APPEND_FRAME && frameType <= Const.APPEND_FRAME_MAX) {
            buf.append("APPEND ").append(String.valueOf(frameType-251));
        } else if (frameType == Const.FULL_FRAME) {
            buf.append("FULL");
        } else {
            buf.append("UNKNOWN (").append(frameType).append(")");
        }
        buf.append(", offset delta=").append(byteCodeOffset);
        if (typesOfLocals.length > 0) {
            buf.append(", locals={");
            for (int i = 0; i < typesOfLocals.length; i++) {
                buf.append(typesOfLocals[i]);
                if (i < typesOfLocals.length - 1) {
                    buf.append(", ");
                }
            }
            buf.append("}");
        }
        if (typesOfStackItems.length > 0) {
            buf.append(", stack items={");
            for (int i = 0; i < typesOfStackItems.length; i++) {
                buf.append(typesOfStackItems[i]);
                if (i < typesOfStackItems.length - 1) {
                    buf.append(", ");
                }
            }
            buf.append("}");
        }
        buf.append(")");
        return buf.toString();
    }


    /**
     * Calculate stack map entry size
     *
     */
    int getMapEntrySize() {
        if (frameType >= Const.SAME_FRAME && frameType <= Const.SAME_FRAME_MAX) {
            return 1;
        } else if (frameType >= Const.SAME_LOCALS_1_STACK_ITEM_FRAME &&
                   frameType <= Const.SAME_LOCALS_1_STACK_ITEM_FRAME_MAX) {
            return 1 + (typesOfStackItems[0].hasIndex() ? 3 : 1);
        } else if (frameType == Const.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) {
            return 3 + (typesOfStackItems[0].hasIndex() ? 3 : 1);
        } else if (frameType >= Const.CHOP_FRAME && frameType <= Const.CHOP_FRAME_MAX) {
            return 3;
        } else if (frameType == Const.SAME_FRAME_EXTENDED) {
            return 3;
        } else if (frameType >= Const.APPEND_FRAME && frameType <= Const.APPEND_FRAME_MAX) {
            int len = 3;
            for (final StackMapType types_of_local : typesOfLocals) {
                len += types_of_local.hasIndex() ? 3 : 1;
            }
            return len;
        } else if (frameType == Const.FULL_FRAME) {
            int len = 7;
            for (final StackMapType types_of_local : typesOfLocals) {
                len += types_of_local.hasIndex() ? 3 : 1;
            }
            for (final StackMapType types_of_stack_item : typesOfStackItems) {
                len += types_of_stack_item.hasIndex() ? 3 : 1;
            }
            return len;
        } else {
            throw new IllegalStateException("Invalid StackMap frameType: " + frameType);
        }
    }


    public void setFrameType( final int f ) {
        if (f >= Const.SAME_FRAME && f <= Const.SAME_FRAME_MAX) {
            byteCodeOffset = f - Const.SAME_FRAME;
        } else if (f >= Const.SAME_LOCALS_1_STACK_ITEM_FRAME &&
                   f <= Const.SAME_LOCALS_1_STACK_ITEM_FRAME_MAX) {
            byteCodeOffset = f - Const.SAME_LOCALS_1_STACK_ITEM_FRAME;
        } else if (f == Const.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) { // CHECKSTYLE IGNORE EmptyBlock
        } else if (f >= Const.CHOP_FRAME && f <= Const.CHOP_FRAME_MAX) { // CHECKSTYLE IGNORE EmptyBlock
        } else if (f == Const.SAME_FRAME_EXTENDED) { // CHECKSTYLE IGNORE EmptyBlock
        } else if (f >= Const.APPEND_FRAME && f <= Const.APPEND_FRAME_MAX) { // CHECKSTYLE IGNORE EmptyBlock
        } else if (f == Const.FULL_FRAME) { // CHECKSTYLE IGNORE EmptyBlock
        } else {
            throw new IllegalArgumentException("Invalid StackMap frameType");
        }
        frameType = f;
    }


    public int getFrameType() {
        return frameType;
    }


    public void setByteCodeOffset( final int new_offset ) {
        if (new_offset < 0 || new_offset > 32767) {
            throw new IllegalArgumentException("Invalid StackMap offset: " + new_offset);
        }

        if (frameType >= Const.SAME_FRAME &&
            frameType <= Const.SAME_FRAME_MAX) {
            if (new_offset > Const.SAME_FRAME_MAX) {
                frameType = Const.SAME_FRAME_EXTENDED;
            } else {
                frameType = new_offset;
            }
        } else if (frameType >= Const.SAME_LOCALS_1_STACK_ITEM_FRAME &&
                   frameType <= Const.SAME_LOCALS_1_STACK_ITEM_FRAME_MAX) {
            if (new_offset > Const.SAME_FRAME_MAX) {
                frameType = Const.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED;
            } else {
                frameType = Const.SAME_LOCALS_1_STACK_ITEM_FRAME + new_offset;
            }
        } else if (frameType == Const.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) { // CHECKSTYLE IGNORE EmptyBlock
        } else if (frameType >= Const.CHOP_FRAME &&
                   frameType <= Const.CHOP_FRAME_MAX) { // CHECKSTYLE IGNORE EmptyBlock
        } else if (frameType == Const.SAME_FRAME_EXTENDED) { // CHECKSTYLE IGNORE EmptyBlock
        } else if (frameType >= Const.APPEND_FRAME &&
                   frameType <= Const.APPEND_FRAME_MAX) { // CHECKSTYLE IGNORE EmptyBlock
        } else if (frameType == Const.FULL_FRAME) { // CHECKSTYLE IGNORE EmptyBlock
        } else {
            throw new IllegalStateException("Invalid StackMap frameType: " + frameType);
        }
        byteCodeOffset = new_offset;
    }


    /**
     * Update the distance (as an offset delta) from this StackMap
     * entry to the next.  Note that this might cause the the
     * frame type to change.  Note also that delta may be negative.
     *
     * @param delta offset delta
     */
    public void updateByteCodeOffset(final int delta) {
        setByteCodeOffset(byteCodeOffset + delta);
    }


    public int getByteCodeOffset() {
        return byteCodeOffset;
    }


    /**
     *
     * @deprecated since 6.0
     */
    @java.lang.Deprecated
    public void setNumberOfLocals( final int n ) { // TODO unused
    }


    public int getNumberOfLocals() {
        return typesOfLocals.length;
    }


    public void setTypesOfLocals( final StackMapType[] types ) {
        typesOfLocals = types != null ? types : new StackMapType[0];
    }


    public StackMapType[] getTypesOfLocals() {
        return typesOfLocals;
    }


    /**
     *
     * @deprecated since 6.0
     */
    @java.lang.Deprecated
    public void setNumberOfStackItems( final int n ) { // TODO unused
    }


    public int getNumberOfStackItems() {
        return typesOfStackItems.length;
    }


    public void setTypesOfStackItems( final StackMapType[] types ) {
        typesOfStackItems = types != null ? types : new StackMapType[0];
    }


    public StackMapType[] getTypesOfStackItems() {
        return typesOfStackItems;
    }


    /**
     * @return deep copy of this object
     */
    public StackMapEntry copy() {
        StackMapEntry e;
        try {
            e = (StackMapEntry) clone();
        } catch (final CloneNotSupportedException ex) {
            throw new Error("Clone Not Supported");
        }

        e.typesOfLocals = new StackMapType[typesOfLocals.length];
        for (int i = 0; i < typesOfLocals.length; i++) {
            e.typesOfLocals[i] = typesOfLocals[i].copy();
        }
        e.typesOfStackItems = new StackMapType[typesOfStackItems.length];
        for (int i = 0; i < typesOfStackItems.length; i++) {
            e.typesOfStackItems[i] = typesOfStackItems[i].copy();
        }
        return e;
    }


    /**
     * Called by objects that are traversing the nodes of the tree implicitely
     * defined by the contents of a Java class. I.e., the hierarchy of methods,
     * fields, attributes, etc. spawns a tree of objects.
     *
     * @param v Visitor object
     */
    @Override
    public void accept( final Visitor v ) {
        v.visitStackMapEntry(this);
    }


    /**
     * @return Constant pool used by this object.
     */
    public ConstantPool getConstantPool() {
        return constantPool;
    }


    /**
     * @param constantPool Constant pool to be used for this object.
     */
    public void setConstantPool( final ConstantPool constantPool ) {
        this.constantPool = constantPool;
    }
}
