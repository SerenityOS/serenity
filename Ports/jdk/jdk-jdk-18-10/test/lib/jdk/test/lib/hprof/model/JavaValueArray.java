/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun.
 */

package jdk.test.lib.hprof.model;

import jdk.test.lib.hprof.parser.ReadBuffer;
import java.io.IOException;
import java.util.Objects;

/**
 * An array of values, that is, an array of ints, boolean, floats or the like.
 *
 * @author      Bill Foote
 */
public class JavaValueArray extends JavaLazyReadObject
                /*imports*/ implements ArrayTypeCodes {

    private static String arrayTypeName(byte sig) {
        switch (sig) {
            case 'B':
                return "byte[]";
            case 'Z':
                return "boolean[]";
            case 'C':
                return "char[]";
            case 'S':
                return "short[]";
            case 'I':
                return "int[]";
            case 'F':
                return "float[]";
            case 'J':
                return "long[]";
            case 'D':
                return "double[]";
            default:
                throw new RuntimeException("invalid array element sig: " + sig);
        }
    }

    private static int elementSize(byte type) {
        switch (type) {
            case T_BYTE:
            case T_BOOLEAN:
                return 1;
            case T_CHAR:
            case T_SHORT:
                return 2;
            case T_INT:
            case T_FLOAT:
                return 4;
            case T_LONG:
            case T_DOUBLE:
                return 8;
            default:
                throw new RuntimeException("invalid array element type: " + type);
        }
    }

    /*
     * Java primitive array record (HPROF_GC_PRIM_ARRAY_DUMP) looks
     * as below:
     *
     *    object ID
     *    stack trace serial number (int)
     *    number of elements (int)
     *    element type (byte)
     *    array data
     */
    @Override
    protected final long readValueLength() throws IOException {
        long offset = getOffset() + idSize() + 4;
        // length of the array in elements
        long len = buf().getInt(offset);
        // byte length of array
        return len * elementSize(getElementType());
    }

    private long dataStartOffset() {
        return getOffset() + idSize() + 4 + 4 + 1;
    }


    @Override
    protected final JavaThing[] readValue() throws IOException {
        int len = getLength();
        long offset = dataStartOffset();

        JavaThing[] res = new JavaThing[len];
        synchronized (buf()) {
            switch (getElementType()) {
                case 'Z': {
                              for (int i = 0; i < len; i++) {
                                  res[i] = new JavaBoolean(booleanAt(offset));
                                  offset += 1;
                              }
                              return res;
                }
                case 'B': {
                              for (int i = 0; i < len; i++) {
                                  res[i] = new JavaByte(byteAt(offset));
                                  offset += 1;
                              }
                              return res;
                }
                case 'C': {
                              for (int i = 0; i < len; i++) {
                                  res[i] = new JavaChar(charAt(offset));
                                  offset += 2;
                              }
                              return res;
                }
                case 'S': {
                              for (int i = 0; i < len; i++) {
                                  res[i] = new JavaShort(shortAt(offset));
                                  offset += 2;
                              }
                              return res;
                }
                case 'I': {
                              for (int i = 0; i < len; i++) {
                                  res[i] = new JavaInt(intAt(offset));
                                  offset += 4;
                              }
                              return res;
                }
                case 'J': {
                              for (int i = 0; i < len; i++) {
                                  res[i] = new JavaLong(longAt(offset));
                                  offset += 8;
                              }
                              return res;
                }
                case 'F': {
                              for (int i = 0; i < len; i++) {
                                  res[i] = new JavaFloat(floatAt(offset));
                                  offset += 4;
                              }
                              return res;
                }
                case 'D': {
                              for (int i = 0; i < len; i++) {
                                  res[i] = new JavaDouble(doubleAt(offset));
                                  offset += 8;
                              }
                              return res;
                }
                default: {
                             throw new RuntimeException("unknown primitive type?");
                }
            }
        }
    }

    // JavaClass set only after resolve.
    private JavaClass clazz;

    // This field contains elementSignature byte and
    // divider to be used to calculate length. Note that
    // length of content byte[] is not same as array length.
    // Actual array length is (byte[].length / divider)
    private int data;

    // First 8 bits of data is used for element signature
    private static final int SIGNATURE_MASK = 0x0FF;

    // Next 8 bits of data is used for length divider
    private static final int LENGTH_DIVIDER_MASK = 0x0FF00;

    // Number of bits to shift to get length divider
    private static final int LENGTH_DIVIDER_SHIFT = 8;

    public JavaValueArray(byte elementSignature, long offset) {
        super(offset);
        this.data = (elementSignature & SIGNATURE_MASK);
    }

    public JavaClass getClazz() {
        return clazz;
    }

    public void visitReferencedObjects(JavaHeapObjectVisitor v) {
        super.visitReferencedObjects(v);
    }

    public void resolve(Snapshot snapshot) {
        if (clazz instanceof JavaClass) {
            return;
        }
        byte elementSig = getElementType();
        clazz = snapshot.findClass(arrayTypeName(elementSig));
        if (clazz == null) {
            clazz = snapshot.getArrayClass("" + ((char) elementSig));
        }
        getClazz().addInstance(this);
        super.resolve(snapshot);
    }

    public int getLength() {
        int divider = (data & LENGTH_DIVIDER_MASK) >>> LENGTH_DIVIDER_SHIFT;
        if (divider == 0) {
            byte elementSignature = getElementType();
            switch (elementSignature) {
            case 'B':
            case 'Z':
                divider = 1;
                break;
            case 'C':
            case 'S':
                divider = 2;
                break;
            case 'I':
            case 'F':
                divider = 4;
                break;
            case 'J':
            case 'D':
                divider = 8;
                break;
            default:
                throw new RuntimeException("unknown primitive type: " +
                                elementSignature);
            }
            data |= (divider << LENGTH_DIVIDER_SHIFT);
        }
        return (int)(getValueLength() / divider);
    }

    public JavaThing[] getElements() {
        return getValue();
    }

    public byte getElementType() {
        return (byte) (data & SIGNATURE_MASK);
    }

    private void checkIndex(int index) {
        Objects.checkIndex(index, getLength());
    }

    private void requireType(char type) {
        if (getElementType() != type) {
            throw new RuntimeException("not of type : " + type);
        }
    }

    public String valueString() {
        return valueString(true);
    }

    public String valueString(boolean bigLimit) {
        // Char arrays deserve special treatment
        StringBuilder result;
        JavaThing[] things = getValue();
        byte elementSignature = getElementType();
        if (elementSignature == 'C')  {
            result = new StringBuilder();
            for (int i = 0; i < things.length; i++) {
                result.append(things[i]);
            }
        } else {
            int limit = 8;
            if (bigLimit) {
                limit = 1000;
            }
            result = new StringBuilder("{");
            for (int i = 0; i < things.length; i++) {
                if (i > 0) {
                    result.append(", ");
                }
                if (i >= limit) {
                    result.append("... ");
                    break;
                }
                switch (elementSignature) {
                    case 'Z': {
                        boolean val = ((JavaBoolean)things[i]).value;
                        if (val) {
                            result.append("true");
                        } else {
                            result.append("false");
                        }
                        break;
                    }
                    case 'B': {
                        byte val = ((JavaByte)things[i]).value;
                        result.append("0x").append(Integer.toString(val, 16));
                        break;
                    }
                    case 'S': {
                        short val = ((JavaShort)things[i]).value;
                        result.append(val);
                        break;
                    }
                    case 'I': {
                        int val = ((JavaInt)things[i]).value;
                        result.append(val);
                        break;
                    }
                    case 'J': {         // long
                        long val = ((JavaLong)things[i]).value;
                        result.append(val);
                        break;
                    }
                    case 'F': {
                        float val = ((JavaFloat)things[i]).value;
                        result.append(val);
                        break;
                    }
                    case 'D': {         // double
                        double val = ((JavaDouble)things[i]).value;
                        result.append(val);
                        break;
                    }
                    default: {
                        throw new RuntimeException("unknown primitive type?");
                    }
                }
            }
            result.append('}');
        }
        return result.toString();
    }
}
