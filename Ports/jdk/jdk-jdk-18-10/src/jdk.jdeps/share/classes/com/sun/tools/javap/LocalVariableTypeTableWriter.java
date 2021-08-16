/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Descriptor;
import com.sun.tools.classfile.Descriptor.InvalidDescriptor;
import com.sun.tools.classfile.Instruction;
import com.sun.tools.classfile.LocalVariableTypeTable_attribute;
import com.sun.tools.classfile.Signature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Annotate instructions with details about local variables.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class LocalVariableTypeTableWriter extends  InstructionDetailWriter {
    public enum NoteKind {
        START("start") {
            public boolean match(LocalVariableTypeTable_attribute.Entry entry, int pc) {
                return (pc == entry.start_pc);
            }
        },
        END("end") {
            public boolean match(LocalVariableTypeTable_attribute.Entry entry, int pc) {
                return (pc == entry.start_pc + entry.length);
            }
        };
        NoteKind(String text) {
            this.text = text;
        }
        public abstract boolean match(LocalVariableTypeTable_attribute.Entry entry, int pc);
        public final String text;
    }

    static LocalVariableTypeTableWriter instance(Context context) {
        LocalVariableTypeTableWriter instance = context.get(LocalVariableTypeTableWriter.class);
        if (instance == null)
            instance = new LocalVariableTypeTableWriter(context);
        return instance;
    }

    protected LocalVariableTypeTableWriter(Context context) {
        super(context);
        context.put(LocalVariableTypeTableWriter.class, this);
        classWriter = ClassWriter.instance(context);
    }

    public void reset(Code_attribute attr) {
        codeAttr = attr;
        pcMap = new HashMap<>();
        LocalVariableTypeTable_attribute lvt =
                (LocalVariableTypeTable_attribute) (attr.attributes.get(Attribute.LocalVariableTypeTable));
        if (lvt == null)
            return;

        for (int i = 0; i < lvt.local_variable_table.length; i++) {
            LocalVariableTypeTable_attribute.Entry entry = lvt.local_variable_table[i];
            put(entry.start_pc, entry);
            put(entry.start_pc + entry.length, entry);
        }
    }

    public void writeDetails(Instruction instr) {
        int pc = instr.getPC();
        writeLocalVariables(pc, NoteKind.END);
        writeLocalVariables(pc, NoteKind.START);
    }

    @Override
    public void flush() {
        int pc = codeAttr.code_length;
        writeLocalVariables(pc, NoteKind.END);
    }

    public void writeLocalVariables(int pc, NoteKind kind) {
        ConstantPool constant_pool = classWriter.getClassFile().constant_pool;
        String indent = space(2); // get from Options?
        List<LocalVariableTypeTable_attribute.Entry> entries = pcMap.get(pc);
        if (entries != null) {
            for (ListIterator<LocalVariableTypeTable_attribute.Entry> iter =
                    entries.listIterator(kind == NoteKind.END ? entries.size() : 0);
                    kind == NoteKind.END ? iter.hasPrevious() : iter.hasNext() ; ) {
                LocalVariableTypeTable_attribute.Entry entry =
                        kind == NoteKind.END ? iter.previous() : iter.next();
                if (kind.match(entry, pc)) {
                    print(indent);
                    print(kind.text);
                    print(" generic local ");
                    print(entry.index);
                    print(" // ");
                    Descriptor d = new Signature(entry.signature_index);
                    try {
                        print(d.getFieldType(constant_pool).toString().replace("/", "."));
                    } catch (InvalidDescriptor e) {
                        print(report(e));
                    } catch (ConstantPoolException e) {
                        print(report(e));
                    }
                    print(" ");
                    try {
                        print(constant_pool.getUTF8Value(entry.name_index));
                    } catch (ConstantPoolException e) {
                        print(report(e));
                    }
                    println();
                }
            }
        }
    }

    private void put(int pc, LocalVariableTypeTable_attribute.Entry entry) {
        List<LocalVariableTypeTable_attribute.Entry> list = pcMap.get(pc);
        if (list == null) {
            list = new ArrayList<>();
            pcMap.put(pc, list);
        }
        if (!list.contains(entry))
            list.add(entry);
    }

    private ClassWriter classWriter;
    private Code_attribute codeAttr;
    private Map<Integer, List<LocalVariableTypeTable_attribute.Entry>> pcMap;
}
