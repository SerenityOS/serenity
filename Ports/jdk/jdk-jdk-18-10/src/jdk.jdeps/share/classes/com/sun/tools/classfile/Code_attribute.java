/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * See JVMS, section 4.8.3.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Code_attribute extends Attribute {
    public static class InvalidIndex extends AttributeException {
        private static final long serialVersionUID = -8904527774589382802L;
        InvalidIndex(int index) {
            this.index = index;
        }

        @Override
        public String getMessage() {
            // i18n
            return "invalid index " + index + " in Code attribute";
        }

        public final int index;
    }

    Code_attribute(ClassReader cr, int name_index, int length)
            throws IOException, ConstantPoolException {
        super(name_index, length);
        max_stack = cr.readUnsignedShort();
        max_locals = cr.readUnsignedShort();
        code_length = cr.readInt();
        code = new byte[code_length];
        cr.readFully(code);
        exception_table_length = cr.readUnsignedShort();
        exception_table = new Exception_data[exception_table_length];
        for (int i = 0; i < exception_table_length; i++)
            exception_table[i] = new Exception_data(cr);
        attributes = new Attributes(cr);
    }

    public int getByte(int offset) throws InvalidIndex {
        if (offset < 0 || offset >= code.length)
            throw new InvalidIndex(offset);
        return code[offset];
    }

    public int getUnsignedByte(int offset) throws InvalidIndex {
        if (offset < 0 || offset >= code.length)
            throw new InvalidIndex(offset);
        return code[offset] & 0xff;
    }

    public int getShort(int offset) throws InvalidIndex {
        if (offset < 0 || offset + 1 >= code.length)
            throw new InvalidIndex(offset);
        return (code[offset] << 8) | (code[offset + 1] & 0xFF);
    }

    public int getUnsignedShort(int offset) throws InvalidIndex {
        if (offset < 0 || offset + 1 >= code.length)
            throw new InvalidIndex(offset);
        return ((code[offset] << 8) | (code[offset + 1] & 0xFF)) & 0xFFFF;
    }

    public int getInt(int offset) throws InvalidIndex {
        if (offset < 0 || offset + 3 >= code.length)
            throw new InvalidIndex(offset);
        return (getShort(offset) << 16) | (getShort(offset + 2) & 0xFFFF);
    }

    public <R, D> R accept(Visitor<R, D> visitor, D data) {
        return visitor.visitCode(this, data);
    }

    public Iterable<Instruction> getInstructions() {
        return () -> new Iterator<Instruction>() {

            public boolean hasNext() {
                return (next != null);
            }

            public Instruction next() {
                if (next == null)
                    throw new NoSuchElementException();

                current = next;
                pc += current.length();
                next = (pc < code.length ? new Instruction(code, pc) : null);
                return current;
            }

            public void remove() {
                throw new UnsupportedOperationException("Not supported.");
            }

            Instruction current = null;
            int pc = 0;
            Instruction next = (pc < code.length ? new Instruction(code, pc) : null);

        };
    }

    public final int max_stack;
    public final int max_locals;
    public final int code_length;
    public final byte[] code;
    public final int exception_table_length;
    public final Exception_data[] exception_table;
    public final Attributes attributes;

    public static class Exception_data {
        Exception_data(ClassReader cr) throws IOException {
            start_pc = cr.readUnsignedShort();
            end_pc = cr.readUnsignedShort();
            handler_pc = cr.readUnsignedShort();
            catch_type = cr.readUnsignedShort();
        }

        public final int start_pc;
        public final int end_pc;
        public final int handler_pc;
        public final int catch_type;
    }
}
