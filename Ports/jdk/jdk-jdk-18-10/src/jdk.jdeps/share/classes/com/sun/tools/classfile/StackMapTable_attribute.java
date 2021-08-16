/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.classfile;

import java.io.IOException;

/**
 * See JVMS, section 4.8.4.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class StackMapTable_attribute extends Attribute {
    static class InvalidStackMap extends AttributeException {
        private static final long serialVersionUID = -5659038410855089780L;
        InvalidStackMap(String msg) {
            super(msg);
        }
    }

    StackMapTable_attribute(ClassReader cr, int name_index, int length)
            throws IOException, InvalidStackMap {
        super(name_index, length);
        number_of_entries = cr.readUnsignedShort();
        entries = new stack_map_frame[number_of_entries];
        for (int i = 0; i < number_of_entries; i++)
            entries[i] = stack_map_frame.read(cr);
    }

    public StackMapTable_attribute(ConstantPool constant_pool, stack_map_frame[] entries)
            throws ConstantPoolException {
        this(constant_pool.getUTF8Index(Attribute.StackMapTable), entries);
    }

    public StackMapTable_attribute(int name_index, stack_map_frame[] entries) {
        super(name_index, length(entries));
        this.number_of_entries = entries.length;
        this.entries = entries;
    }

    public <R, D> R accept(Visitor<R, D> visitor, D data) {
        return visitor.visitStackMapTable(this, data);
    }

    static int length(stack_map_frame[] entries) {
        int n = 2;
        for (stack_map_frame entry: entries)
            n += entry.length();
        return n;
    }

    public final int number_of_entries;
    public final stack_map_frame entries[];

    public static abstract class stack_map_frame {
        static stack_map_frame read(ClassReader cr)
                throws IOException, InvalidStackMap {
            int frame_type = cr.readUnsignedByte();
            if (frame_type <= 63)
                return new same_frame(frame_type);
            else if (frame_type <= 127)
                return new same_locals_1_stack_item_frame(frame_type, cr);
            else if (frame_type <= 246)
                throw new Error("unknown frame_type " + frame_type);
            else if (frame_type == 247)
                return new same_locals_1_stack_item_frame_extended(frame_type, cr);
            else if (frame_type <= 250)
                return new chop_frame(frame_type, cr);
            else if (frame_type == 251)
                return new same_frame_extended(frame_type, cr);
            else if (frame_type <= 254)
                return new append_frame(frame_type, cr);
            else
                return new full_frame(frame_type, cr);
        }

        protected stack_map_frame(int frame_type) {
            this.frame_type = frame_type;
        }

        public int length() {
            return 1;
        }

        public abstract int getOffsetDelta();

        public abstract <R,D> R accept(Visitor<R,D> visitor, D data);

        public final int frame_type;

        public static interface Visitor<R,P> {
            R visit_same_frame(same_frame frame, P p);
            R visit_same_locals_1_stack_item_frame(same_locals_1_stack_item_frame frame, P p);
            R visit_same_locals_1_stack_item_frame_extended(same_locals_1_stack_item_frame_extended frame, P p);
            R visit_chop_frame(chop_frame frame, P p);
            R visit_same_frame_extended(same_frame_extended frame, P p);
            R visit_append_frame(append_frame frame, P p);
            R visit_full_frame(full_frame frame, P p);
        }
    }

    public static class same_frame extends stack_map_frame {
        same_frame(int frame_type) {
            super(frame_type);
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visit_same_frame(this, data);
        }

        public int getOffsetDelta() {
            return frame_type;
        }
    }

    public static class same_locals_1_stack_item_frame extends stack_map_frame {
        same_locals_1_stack_item_frame(int frame_type, ClassReader cr)
                throws IOException, InvalidStackMap {
            super(frame_type);
            stack = new verification_type_info[1];
            stack[0] = verification_type_info.read(cr);
        }

        @Override
        public int length() {
            return super.length() + stack[0].length();
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visit_same_locals_1_stack_item_frame(this, data);
        }

        public int getOffsetDelta() {
            return frame_type - 64;
        }

        public final verification_type_info[] stack;
    }

    public static class same_locals_1_stack_item_frame_extended extends stack_map_frame {
        same_locals_1_stack_item_frame_extended(int frame_type, ClassReader cr)
                throws IOException, InvalidStackMap {
            super(frame_type);
            offset_delta = cr.readUnsignedShort();
            stack = new verification_type_info[1];
            stack[0] = verification_type_info.read(cr);
        }

        @Override
        public int length() {
            return super.length() + 2 + stack[0].length();
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visit_same_locals_1_stack_item_frame_extended(this, data);
        }

        public int getOffsetDelta() {
            return offset_delta;
        }

        public final int offset_delta;
        public final verification_type_info[] stack;
    }

    public static class chop_frame extends stack_map_frame {
        chop_frame(int frame_type, ClassReader cr) throws IOException {
            super(frame_type);
            offset_delta = cr.readUnsignedShort();
        }

        @Override
        public int length() {
            return super.length() + 2;
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visit_chop_frame(this, data);
        }

        public int getOffsetDelta() {
            return offset_delta;
        }

        public final int offset_delta;
    }

    public static class same_frame_extended extends stack_map_frame {
        same_frame_extended(int frame_type, ClassReader cr) throws IOException {
            super(frame_type);
            offset_delta = cr.readUnsignedShort();
        }

        @Override
        public int length() {
            return super.length() + 2;
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visit_same_frame_extended(this, data);
        }

        public int getOffsetDelta() {
            return offset_delta;
        }

        public final int offset_delta;
    }

    public static class append_frame extends stack_map_frame {
        append_frame(int frame_type, ClassReader cr)
                throws IOException, InvalidStackMap {
            super(frame_type);
            offset_delta = cr.readUnsignedShort();
            locals = new verification_type_info[frame_type - 251];
            for (int i = 0; i < locals.length; i++)
                locals[i] = verification_type_info.read(cr);
        }

        @Override
        public int length() {
            int n = super.length() + 2;
            for (verification_type_info local: locals)
                n += local.length();
            return n;
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visit_append_frame(this, data);
        }

        public int getOffsetDelta() {
            return offset_delta;
        }

        public final int offset_delta;
        public final verification_type_info[] locals;
    }

    public static class full_frame extends stack_map_frame {
        full_frame(int frame_type, ClassReader cr)
                throws IOException, InvalidStackMap {
            super(frame_type);
            offset_delta = cr.readUnsignedShort();
            number_of_locals = cr.readUnsignedShort();
            locals = new verification_type_info[number_of_locals];
            for (int i = 0; i < locals.length; i++)
                locals[i] = verification_type_info.read(cr);
            number_of_stack_items = cr.readUnsignedShort();
            stack = new verification_type_info[number_of_stack_items];
            for (int i = 0; i < stack.length; i++)
                stack[i] = verification_type_info.read(cr);
        }

        @Override
        public int length() {
            int n = super.length() + 2;
            for (verification_type_info local: locals)
                n += local.length();
            n += 2;
            for (verification_type_info item: stack)
                n += item.length();
            return n;
        }

        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visit_full_frame(this, data);
        }

        public int getOffsetDelta() {
            return offset_delta;
        }

        public final int offset_delta;
        public final int number_of_locals;
        public final verification_type_info[] locals;
        public final int number_of_stack_items;
        public final verification_type_info[] stack;
    }

    public static class verification_type_info {
        public static final int ITEM_Top = 0;
        public static final int ITEM_Integer = 1;
        public static final int ITEM_Float = 2;
        public static final int ITEM_Long = 4;
        public static final int ITEM_Double = 3;
        public static final int ITEM_Null = 5;
        public static final int ITEM_UninitializedThis = 6;
        public static final int ITEM_Object = 7;
        public static final int ITEM_Uninitialized = 8;

        static verification_type_info read(ClassReader cr)
                throws IOException, InvalidStackMap {
            int tag = cr.readUnsignedByte();
            switch (tag) {
            case ITEM_Top:
            case ITEM_Integer:
            case ITEM_Float:
            case ITEM_Long:
            case ITEM_Double:
            case ITEM_Null:
            case ITEM_UninitializedThis:
                return new verification_type_info(tag);

            case ITEM_Object:
                return new Object_variable_info(cr);

            case ITEM_Uninitialized:
                return new Uninitialized_variable_info(cr);

            default:
                throw new InvalidStackMap("unrecognized verification_type_info tag");
            }
        }

        protected verification_type_info(int tag) {
            this.tag = tag;
        }

        public int length() {
            return 1;
        }

        public final int tag;
    }

    public static class Object_variable_info extends verification_type_info {
        Object_variable_info(ClassReader cr) throws IOException {
            super(ITEM_Object);
            cpool_index = cr.readUnsignedShort();
        }

        @Override
        public int length() {
            return super.length() + 2;
        }

        public final int cpool_index;
    }

    public static class Uninitialized_variable_info extends verification_type_info {
        Uninitialized_variable_info(ClassReader cr) throws IOException {
            super(ITEM_Uninitialized);
            offset = cr.readUnsignedShort();
        }

        @Override
        public int length() {
            return super.length() + 2;
        }

        public final int offset;

    }
}
