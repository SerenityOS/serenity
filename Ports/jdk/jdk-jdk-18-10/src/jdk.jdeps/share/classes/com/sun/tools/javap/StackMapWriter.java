/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.sun.tools.classfile.AccessFlags;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Descriptor;
import com.sun.tools.classfile.Descriptor.InvalidDescriptor;
import com.sun.tools.classfile.Instruction;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.StackMapTable_attribute;
import com.sun.tools.classfile.StackMapTable_attribute.*;

import static com.sun.tools.classfile.StackMapTable_attribute.verification_type_info.*;

/**
 * Annotate instructions with stack map.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class StackMapWriter extends InstructionDetailWriter {
    static StackMapWriter instance(Context context) {
        StackMapWriter instance = context.get(StackMapWriter.class);
        if (instance == null)
            instance = new StackMapWriter(context);
        return instance;
    }

    protected StackMapWriter(Context context) {
        super(context);
        context.put(StackMapWriter.class, this);
        classWriter = ClassWriter.instance(context);
    }

    public void reset(Code_attribute attr) {
        setStackMap((StackMapTable_attribute) attr.attributes.get(Attribute.StackMapTable));
    }

    void setStackMap(StackMapTable_attribute attr) {
        if (attr == null) {
            map = null;
            return;
        }

        Method m = classWriter.getMethod();
        Descriptor d = m.descriptor;
        String[] args;
        try {
            ConstantPool cp = classWriter.getClassFile().constant_pool;
            String argString = d.getParameterTypes(cp);
            args = argString.substring(1, argString.length() - 1).split("[, ]+");
        } catch (ConstantPoolException | InvalidDescriptor e) {
            return;
        }
        boolean isStatic = m.access_flags.is(AccessFlags.ACC_STATIC);

        verification_type_info[] initialLocals = new verification_type_info[(isStatic ? 0 : 1) + args.length];
        if (!isStatic)
            initialLocals[0] = new CustomVerificationTypeInfo("this");
        for (int i = 0; i < args.length; i++) {
            initialLocals[(isStatic ? 0 : 1) + i] =
                    new CustomVerificationTypeInfo(args[i].replace(".", "/"));
        }

        map = new HashMap<>();
        StackMapBuilder builder = new StackMapBuilder();

        // using -1 as the pc for the initial frame effectively compensates for
        // the difference in behavior for the first stack map frame (where the
        // pc offset is just offset_delta) compared to subsequent frames (where
        // the pc offset is always offset_delta+1).
        int pc = -1;

        map.put(pc, new StackMap(initialLocals, empty));

        for (int i = 0; i < attr.entries.length; i++)
            pc = attr.entries[i].accept(builder, pc);
    }

    public void writeInitialDetails() {
        writeDetails(-1);
    }

    public void writeDetails(Instruction instr) {
        writeDetails(instr.getPC());
    }

    private void writeDetails(int pc) {
        if (map == null)
            return;

        StackMap m = map.get(pc);
        if (m != null) {
            print("StackMap locals: ", m.locals);
            print("StackMap stack: ", m.stack);
        }

    }

    void print(String label, verification_type_info[] entries) {
        print(label);
        for (int i = 0; i < entries.length; i++) {
            print(" ");
            print(entries[i]);
        }
        println();
    }

    void print(verification_type_info entry) {
        if (entry == null) {
            print("ERROR");
            return;
        }

        switch (entry.tag) {
            case -1:
                print(((CustomVerificationTypeInfo) entry).text);
                break;

            case ITEM_Top:
                print("top");
                break;

            case ITEM_Integer:
                print("int");
                break;

            case ITEM_Float:
                print("float");
                break;

            case ITEM_Long:
                print("long");
                break;

            case ITEM_Double:
                print("double");
                break;

            case ITEM_Null:
                print("null");
                break;

            case ITEM_UninitializedThis:
                print("uninit_this");
                break;

            case ITEM_Object:
                try {
                    ConstantPool cp = classWriter.getClassFile().constant_pool;
                    ConstantPool.CONSTANT_Class_info class_info = cp.getClassInfo(((Object_variable_info) entry).cpool_index);
                    print(cp.getUTF8Value(class_info.name_index));
                } catch (ConstantPoolException e) {
                    print("??");
                }
                break;

            case ITEM_Uninitialized:
                print(((Uninitialized_variable_info) entry).offset);
                break;
        }

    }

    private Map<Integer, StackMap> map;
    private ClassWriter classWriter;

    class StackMapBuilder
            implements StackMapTable_attribute.stack_map_frame.Visitor<Integer, Integer> {

        public Integer visit_same_frame(same_frame frame, Integer pc) {
            int new_pc = pc + frame.getOffsetDelta() + 1;
            StackMap m = map.get(pc);
            assert (m != null);
            map.put(new_pc, m);
            return new_pc;
        }

        public Integer visit_same_locals_1_stack_item_frame(same_locals_1_stack_item_frame frame, Integer pc) {
            int new_pc = pc + frame.getOffsetDelta() + 1;
            StackMap prev = map.get(pc);
            assert (prev != null);
            StackMap m = new StackMap(prev.locals, frame.stack);
            map.put(new_pc, m);
            return new_pc;
        }

        public Integer visit_same_locals_1_stack_item_frame_extended(same_locals_1_stack_item_frame_extended frame, Integer pc) {
            int new_pc = pc + frame.getOffsetDelta() + 1;
            StackMap prev = map.get(pc);
            assert (prev != null);
            StackMap m = new StackMap(prev.locals, frame.stack);
            map.put(new_pc, m);
            return new_pc;
        }

        public Integer visit_chop_frame(chop_frame frame, Integer pc) {
            int new_pc = pc + frame.getOffsetDelta() + 1;
            StackMap prev = map.get(pc);
            assert (prev != null);
            int k = 251 - frame.frame_type;
            verification_type_info[] new_locals = Arrays.copyOf(prev.locals, prev.locals.length - k);
            StackMap m = new StackMap(new_locals, empty);
            map.put(new_pc, m);
            return new_pc;
        }

        public Integer visit_same_frame_extended(same_frame_extended frame, Integer pc) {
            int new_pc = pc + frame.getOffsetDelta();
            StackMap m = map.get(pc);
            assert (m != null);
            map.put(new_pc, m);
            return new_pc;
        }

        public Integer visit_append_frame(append_frame frame, Integer pc) {
            int new_pc = pc + frame.getOffsetDelta() + 1;
            StackMap prev = map.get(pc);
            assert (prev != null);
            verification_type_info[] new_locals = new verification_type_info[prev.locals.length + frame.locals.length];
            System.arraycopy(prev.locals, 0, new_locals, 0, prev.locals.length);
            System.arraycopy(frame.locals, 0, new_locals, prev.locals.length, frame.locals.length);
            StackMap m = new StackMap(new_locals, empty);
            map.put(new_pc, m);
            return new_pc;
        }

        public Integer visit_full_frame(full_frame frame, Integer pc) {
            int new_pc = pc + frame.getOffsetDelta() + 1;
            StackMap m = new StackMap(frame.locals, frame.stack);
            map.put(new_pc, m);
            return new_pc;
        }

    }

    static class StackMap {
        StackMap(verification_type_info[] locals, verification_type_info[] stack) {
            this.locals = locals;
            this.stack = stack;
        }

        private final verification_type_info[] locals;
        private final verification_type_info[] stack;
    }

    static class CustomVerificationTypeInfo extends verification_type_info {
        public CustomVerificationTypeInfo(String text) {
            super(-1);
            this.text = text;
        }
        private String text;
    }

    private final verification_type_info[] empty = { };
}
