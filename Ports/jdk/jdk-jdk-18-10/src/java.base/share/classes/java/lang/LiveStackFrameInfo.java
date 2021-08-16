/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package java.lang;

final class LiveStackFrameInfo extends StackFrameInfo implements LiveStackFrame {
    private static Object[] EMPTY_ARRAY = new Object[0];

    // These flags must match the values maintained in the VM
    private static final int MODE_INTERPRETED = 0x01;
    private static final int MODE_COMPILED    = 0x02;

    LiveStackFrameInfo(StackWalker walker) {
        super(walker);
    }

    // These fields are initialized by the VM if ExtendedOption.LOCALS_AND_OPERANDS is set
    private Object[] monitors = EMPTY_ARRAY;
    private Object[] locals = EMPTY_ARRAY;
    private Object[] operands = EMPTY_ARRAY;
    private int mode = 0;

    @Override
    public Object[] getMonitors() {
        return monitors;
    }

    @Override
    public Object[] getLocals() {
        return locals;
    }

    @Override
    public Object[] getStack() {
        return operands;
    }

    @Override
    public String toString() {
        StringBuilder retVal = new StringBuilder(super.toString());
        if (mode != 0) {
            retVal.append("(");
            if ((mode & MODE_INTERPRETED) == MODE_INTERPRETED) {
                retVal.append(" interpreted ");
            }
            if ((mode & MODE_COMPILED) == MODE_COMPILED) {
                retVal.append(" compiled ");
            }
            retVal.append(")");
        }
        return retVal.toString();
    }

    /*
     * Convert primitive value to {@code PrimitiveSlot} object to represent
     * a local variable or an element on the operand stack of primitive type.
     */

    static PrimitiveSlot asPrimitive(int value) {
        return new PrimitiveSlot32(value);
    }

    static PrimitiveSlot asPrimitive(long value) {
        return new PrimitiveSlot64(value);
    }

    private static class PrimitiveSlot32 extends PrimitiveSlot {
        final int value;
        PrimitiveSlot32(int value) {
            this.value = value;
        }

        @Override
        public int size() {
            return 4;
        }

        @Override
        public int intValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    private static class PrimitiveSlot64 extends PrimitiveSlot {
        final long value;
        PrimitiveSlot64(long value) {
            this.value = value;
        }

        @Override
        public int size() {
            return 8;
        }

        @Override
        public long longValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}
