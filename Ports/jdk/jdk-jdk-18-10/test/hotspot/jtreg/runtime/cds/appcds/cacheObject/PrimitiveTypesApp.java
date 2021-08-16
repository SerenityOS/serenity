/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Field;
import sun.hotspot.WhiteBox;

//
// Test primitive type class mirror objects are cached when open archive heap
// objects are mapped.
//
public class PrimitiveTypesApp {
    public static void main(String[] args) {
        WhiteBox wb = WhiteBox.getWhiteBox();
        if (!wb.areOpenArchiveHeapObjectsMapped()) {
            System.out.println("Archived open_archive_heap objects are not mapped.");
            System.out.println("This may happen during normal operation. Test Skipped.");
            return;
        }

        FieldsTest ft = new FieldsTest();
        ft.testBoolean(wb);
        ft.testByte(wb);
        ft.testChar(wb);
        ft.testInt(wb);
        ft.testShort(wb);
        ft.testLong(wb);
        ft.testFloat(wb);
        ft.testDouble(wb);
    }
}

class FieldsTest {
    public boolean f_boolean;
    public byte f_byte;
    public char f_char;
    public int f_int;
    public short f_short;
    public long f_long;
    public float f_float;
    public double f_double;

    FieldsTest() {
        f_byte = 1;
        f_boolean = false;
        f_char = 'a';
        f_int = 1;
        f_short = 100;
        f_long = 2018L;
        f_float = 1.0f;
        f_double = 2.5;
    }

    void testBoolean(WhiteBox wb) {
        try {
            Field f = this.getClass().getDeclaredField("f_boolean");
            f.setBoolean(this, true);
            if (!f_boolean) {
                throw new RuntimeException("FAILED. Field f_boolean has unexpected value: " + f_boolean);
            }
            checkPrimitiveType(wb, f, Boolean.TYPE);
        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }

    void testByte(WhiteBox wb) {
        try {
            Field f = this.getClass().getDeclaredField("f_byte");
            f.setByte(this, (byte)9);
            if (f_byte != (byte)9) {
                throw new RuntimeException("FAILED. Field f_byte has unexpected value: " + f_byte);
            }
            checkPrimitiveType(wb, f, Byte.TYPE);
        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }

    void testChar(WhiteBox wb) {
        try {
            Field f = this.getClass().getDeclaredField("f_char");
            f.setChar(this, 'b');
            if (f_char != 'b') {
                throw new RuntimeException("FAILED. Field f_char has unexpected value: " + f_char);
            }
            checkPrimitiveType(wb, f, Character.TYPE);
        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }

    void testInt(WhiteBox wb) {
        try {
            Field f = this.getClass().getDeclaredField("f_int");
            f.setInt(this, 9999);
            if (f_int != 9999) {
                throw new RuntimeException("FAILED. Field f_int has unexpected value: " + f_int);
            }
            checkPrimitiveType(wb, f, Integer.TYPE);
        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }

    void testShort(WhiteBox wb) {
        try {
            Field f = this.getClass().getDeclaredField("f_short");
            f.setShort(this, (short)99);
            if (f_short != 99) {
                throw new RuntimeException("FAILED. Field f_short has unexpected value: " + f_short);
            }
            checkPrimitiveType(wb, f, Short.TYPE);
        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }

    void testLong(WhiteBox wb) {
        try {
            Field f = this.getClass().getDeclaredField("f_long");
            f.setLong(this, 99L);
            if (f_long != 99L) {
                throw new RuntimeException("FAILED. Field f_long has unexpected value: " + f_long);
            }
            checkPrimitiveType(wb, f, Long.TYPE);
        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }

    void testFloat(WhiteBox wb) {
        try {
            Field f = this.getClass().getDeclaredField("f_float");
            f.setFloat(this, 9.9f);
            if (f_float != 9.9f) {
                throw new RuntimeException("FAILED. Field f_float has unexpected value: " + f_float);
            }
            checkPrimitiveType(wb, f, Float.TYPE);
        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }

    void testDouble(WhiteBox wb) {
        try {
            Field f = this.getClass().getDeclaredField("f_double");
            f.setDouble(this, 9.9);
            if (f_double != 9.9) {
                throw new RuntimeException("FAILED. Field f_double has unexpected value: " + f_double);
            }
            checkPrimitiveType(wb, f, Double.TYPE);
        } catch (NoSuchFieldException nsfe) {
            throw new RuntimeException(nsfe);
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }

    void checkPrimitiveType(WhiteBox wb, Field f, Class t) {
        Class c = f.getType();
        if (!(c.isPrimitive() && c == t)) {
            throw new RuntimeException("FAILED. " + c + " is not primitive type " + t);
        }
        if (wb.isShared(c)) {
            System.out.println(c + " is cached, expected");
        } else {
            throw new RuntimeException("FAILED. " + c + " is not cached.");
        }
    }
}
