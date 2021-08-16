/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.runtime;

import java.nio.charset.StandardCharsets;
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

public class PerfDataEntry extends VMObject {
    private static JIntField  entryLengthField;
    private static JIntField  nameOffsetField;
    private static JIntField  vectorLengthField;
    private static JByteField dataTypeField;
    private static JByteField flagsField;
    private static JByteField dataUnitsField;
    private static JByteField dataVariabilityField;
    private static JIntField  dataOffsetField;

    static {
        VM.registerVMInitializedObserver(new Observer() {
                public void update(Observable o, Object data) {
                    initialize(VM.getVM().getTypeDataBase());
                }
            });
    }

    private static synchronized void initialize(TypeDataBase db) {
        Type type = db.lookupType("PerfDataEntry");
        entryLengthField = type.getJIntField("entry_length");
        nameOffsetField = type.getJIntField("name_offset");
        vectorLengthField = type.getJIntField("vector_length");
        dataTypeField = type.getJByteField("data_type");
        flagsField = type.getJByteField("flags");
        dataUnitsField = type.getJByteField("data_units");
        dataVariabilityField = type.getJByteField("data_variability");
        dataOffsetField = type.getJIntField("data_offset");
    }

    public PerfDataEntry(Address addr) {
        super(addr);
    }

    // Accessors

    public int entryLength() {
        return (int) entryLengthField.getValue(addr);
    }

    public int nameOffset() {
        return (int) nameOffsetField.getValue(addr);
    }

    public int vectorLength() {
        return (int) vectorLengthField.getValue(addr);
    }

    // returns one of the constants in BasicType class
    public int dataType() {
        char ch = (char) (byte) dataTypeField.getValue(addr);
        return BasicType.charToType(ch);
    }

    public byte flags() {
        return (byte) flagsField.getValue(addr);
    }

    public boolean supported() {
        return (flags() & 0x1) != 0;
    }

    private static class PerfDataUnits {
        public static int U_None;
        public static int U_Bytes;
        public static int U_Ticks;
        public static int U_Events;
        public static int U_String;
        public static int U_Hertz;

        static {
            VM.registerVMInitializedObserver(new Observer() {
                public void update(Observable o, Object data) {
                    initialize(VM.getVM().getTypeDataBase());
                }
            });
        }
        private static synchronized void initialize(TypeDataBase db) {
            U_None = db.lookupIntConstant("PerfData::U_None");
            U_Bytes = db.lookupIntConstant("PerfData::U_Bytes");
            U_Ticks = db.lookupIntConstant("PerfData::U_Ticks");
            U_Events = db.lookupIntConstant("PerfData::U_Events");
            U_String = db.lookupIntConstant("PerfData::U_String");
            U_Hertz = db.lookupIntConstant("PerfData::U_Hertz");
        }
    }

    // returns one of the constants in PerfDataUnits
    public int dataUnits() {
        return (int) dataUnitsField.getValue(addr);
    }

    // returns one of the constants in PerfDataVariability
    public int dataVariability() {
        return (int) dataVariabilityField.getValue(addr);
    }

    public int dataOffset() {
        return (int) dataOffsetField.getValue(addr);
    }

    public String name() {
        int off = nameOffset();
        return CStringUtilities.getString(addr.addOffsetTo(off));
    }

    public boolean booleanValue() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(vectorLength() == 0 &&
                        dataType() == BasicType.getTBoolean(), "not a boolean");
        }
        return addr.getJBooleanAt(dataOffset());
    }

    public char charValue() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(vectorLength() == 0 &&
                        dataType() == BasicType.getTChar(), "not a char");
        }
        return addr.getJCharAt(dataOffset());
    }

    public byte byteValue() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(vectorLength() == 0 &&
                        dataType() == BasicType.getTByte(), "not a byte");
        }
        return addr.getJByteAt(dataOffset());

    }

    public short shortValue() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(vectorLength() == 0 &&
                        dataType() == BasicType.getTShort(), "not a short");
        }
        return addr.getJShortAt(dataOffset());
    }

    public int intValue() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(vectorLength() == 0 &&
                        dataType() == BasicType.getTInt(), "not an int");
        }
        return addr.getJIntAt(dataOffset());
    }

    public long longValue() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(vectorLength() == 0 &&
                        dataType() == BasicType.getTLong(), "not a long");
        }
        return addr.getJLongAt(dataOffset());
    }

    public float floatValue() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(vectorLength() == 0 &&
                        dataType() == BasicType.getTFloat(), "not a float");
        }
        return addr.getJFloatAt(dataOffset());
    }

    public double doubleValue() {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(vectorLength() == 0 &&
                        dataType() == BasicType.getTDouble(), "not a double");
        }
        return addr.getJDoubleAt(dataOffset());
    }

    public boolean[] booleanArrayValue() {
        int len = vectorLength();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(len > 0 &&
                        dataType() == BasicType.getTBoolean(), "not a boolean vector");
        }
        boolean[] res = new boolean[len];
        final int off = dataOffset();
        final long size =  getHeap().getBooleanSize();
        for (int i = 0; i < len; i++) {
            res[i] = addr.getJBooleanAt(off + i * size);
        }
        return res;
    }

    public char[] charArrayValue() {
        int len = vectorLength();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(len > 0 &&
                        dataType() == BasicType.getTChar(), "not a char vector");
        }
        char[] res = new char[len];
        final int off = dataOffset();
        final long size = getHeap().getCharSize();
        for (int i = 0; i < len; i++) {
            res[i] = addr.getJCharAt(off + i * size);
        }
        return res;
    }

    public byte[] byteArrayValue() {
        int len = vectorLength();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(len > 0 &&
                        dataType() == BasicType.getTByte(), "not a byte vector");
        }
        byte[] res = new byte[len];
        final int off = dataOffset();
        final long size = getHeap().getByteSize();
        for (int i = 0; i < len; i++) {
            res[i] = addr.getJByteAt(off + i * size);
        }
        return res;
    }

    public short[] shortArrayValue() {
        int len = vectorLength();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(len > 0 &&
                        dataType() == BasicType.getTShort(), "not a short vector");
        }
        short[] res = new short[len];
        final int off = dataOffset();
        final long size = getHeap().getShortSize();
        for (int i = 0; i < len; i++) {
            res[i] = addr.getJShortAt(off + i * size);
        }
        return res;
    }

    public int[] intArrayValue() {
        int len = vectorLength();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(len > 0 &&
                        dataType() == BasicType.getTInt(), "not an int vector");
        }
        int[] res = new int[len];
        final int off = dataOffset();
        final long size = getHeap().getIntSize();
        for (int i = 0; i < len; i++) {
            res[i] = addr.getJIntAt(off + i * size);
        }
        return res;
    }

    public long[] longArrayValue() {
        int len = vectorLength();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(len > 0 &&
                        dataType() == BasicType.getTLong(), "not a long vector");
        }
        long[] res = new long[len];
        final int off = dataOffset();
        final long size = getHeap().getLongSize();
        for (int i = 0; i < len; i++) {
            res[i] = addr.getJLongAt(off + i * size);
        }
        return res;
    }

    public float[] floatArrayValue() {
        int len = vectorLength();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(len > 0 &&
                        dataType() == BasicType.getTFloat(), "not a float vector");
        }
        float[] res = new float[len];
        final int off = dataOffset();
        final long size = getHeap().getFloatSize();
        for (int i = 0; i < len; i++) {
            res[i] = addr.getJFloatAt(off + i * size);
        }
        return res;
    }

    public double[] doubleArrayValue() {
        int len = vectorLength();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(len > 0 &&
                        dataType() == BasicType.getTDouble(), "not a double vector");
        }
        double[] res = new double[len];
        final int off = dataOffset();
        final long size = getHeap().getDoubleSize();
        for (int i = 0; i < len; i++) {
            res[i] = addr.getJDoubleAt(off + i * size);
        }
        return res;
    }

    // value as String
    public String valueAsString() {
        int dataType = dataType();
        int len = vectorLength();
        String str = null;
        if (len == 0) { // scalar
            if (dataType == BasicType.getTBoolean()) {
                str = Boolean.toString(booleanValue());
            } else if (dataType == BasicType.getTChar()) {
                str = "'" + Character.toString(charValue()) + "'";
            } else if (dataType == BasicType.getTByte()) {
                str = Byte.toString(byteValue());
            } else if (dataType == BasicType.getTShort()) {
                str = Short.toString(shortValue());
            } else if (dataType ==  BasicType.getTInt()) {
                str = Integer.toString(intValue());
            } else if (dataType == BasicType.getTLong()) {
                str = Long.toString(longValue());
            } else if (dataType == BasicType.getTFloat()) {
                str = Float.toString(floatValue());
            } else if (dataType == BasicType.getTDouble()) {
                str = Double.toString(doubleValue());
            } else {
                str = "<unknown scalar value>";
            }
        } else { // vector
            if (dataType == BasicType.getTBoolean()) {
                boolean[] res = booleanArrayValue();
                StringBuilder buf = new StringBuilder();
                buf.append('[');
                for (int i = 0; i < res.length; i++) {
                    buf.append(res[i]);
                    buf.append(", ");
                }
                buf.append(']');
                str = buf.toString();
            } else if (dataType == BasicType.getTChar()) {
                // char[] is returned as a String
                str = new String(charArrayValue());
            } else if (dataType == BasicType.getTByte()) {
                // byte[] is returned as a String
                str = CStringUtilities.getString(addr.addOffsetTo(dataOffset()),
                                                 StandardCharsets.US_ASCII);
            } else if (dataType == BasicType.getTShort()) {
                short[] res = shortArrayValue();
                StringBuilder buf = new StringBuilder();
                buf.append('[');
                for (int i = 0; i < res.length; i++) {
                    buf.append(res[i]);
                    buf.append(", ");
                }
                buf.append(']');
                str = buf.toString();
            } else if (dataType ==  BasicType.getTInt()) {
                int[] res = intArrayValue();
                StringBuilder buf = new StringBuilder();
                buf.append('[');
                for (int i = 0; i < res.length; i++) {
                    buf.append(res[i]);
                    buf.append(", ");
                }
                buf.append(']');
                str = buf.toString();
            } else if (dataType == BasicType.getTLong()) {
                long[] res = longArrayValue();
                StringBuilder buf = new StringBuilder();
                buf.append('[');
                for (int i = 0; i < res.length; i++) {
                    buf.append(res[i]);
                    buf.append(", ");
                }
                buf.append(']');
                str = buf.toString();
            } else if (dataType == BasicType.getTFloat()) {
                float[] res = floatArrayValue();
                StringBuilder buf = new StringBuilder();
                buf.append('[');
                for (int i = 0; i < res.length; i++) {
                    buf.append(res[i]);
                    buf.append(", ");
                }
                buf.append(']');
                str = buf.toString();
            } else if (dataType == BasicType.getTDouble()) {
                double[] res = doubleArrayValue();
                StringBuilder buf = new StringBuilder();
                buf.append('[');
                for (int i = 0; i < res.length; i++) {
                    buf.append(res[i]);
                    buf.append(", ");
                }
                buf.append(']');
                str = buf.toString();
            } else {
                str = "<unknown vector value>";
            }
        }

        // add units
        int dataUnitsValue = dataUnits();

        if (dataUnitsValue == PerfDataUnits.U_Bytes) {
            str += " byte(s)";
        } else if (dataUnitsValue == PerfDataUnits.U_Ticks) {
            str += " tick(s)";
        } else if (dataUnitsValue == PerfDataUnits.U_Events) {
            str += " event(s)";
        } else if (dataUnitsValue == PerfDataUnits.U_Hertz) {
            str += " Hz";
        }

        return str;
    }

    // -- Internals only below this point
    private ObjectHeap getHeap() {
        return VM.getVM().getObjectHeap();
    }
}
