/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 */

/*
  @test
  @bug 4490864
  @summary Verify reflective static field accesses (sanity check)
  @author Kenneth Russell
*/

import java.lang.reflect.*;

public class StaticFieldTest {
    private static byte   byteField;
    private static short  shortField;
    private static char   charField;
    private static int    intField;
    private static long   longField;
    private static float  floatField;
    private static double doubleField;
    private static String stringField;

    private static Field getAccessibleField(String name) throws NoSuchFieldException {
        Field f = StaticFieldTest.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    public static void main(String[] args) throws Exception {
        Field byteField   = getAccessibleField("byteField");
        Field shortField  = getAccessibleField("shortField");
        Field charField   = getAccessibleField("charField");
        Field intField    = getAccessibleField("intField");
        Field longField   = getAccessibleField("longField");
        Field floatField  = getAccessibleField("floatField");
        Field doubleField = getAccessibleField("doubleField");
        Field stringField = getAccessibleField("stringField");

        byteField.setByte    (null, (byte) 77);
        shortField.setShort  (null, (short) 77);
        charField.setChar    (null, (char) 77);
        intField.setInt      (null, (int) 77);
        longField.setLong    (null, (long) 77);
        floatField.setFloat  (null, (float) 77);
        doubleField.setDouble(null, (double) 77);
        String myString = "Hello, world";
        stringField.set      (null, myString);

        if (byteField.getByte(null)     != 77) throw new RuntimeException("Test failed");
        if (shortField.getShort(null)   != 77) throw new RuntimeException("Test failed");
        if (charField.getChar(null)     != 77) throw new RuntimeException("Test failed");
        if (intField.getInt(null)       != 77) throw new RuntimeException("Test failed");
        if (longField.getLong(null)     != 77) throw new RuntimeException("Test failed");
        if (floatField.getFloat(null)   != 77) throw new RuntimeException("Test failed");
        if (doubleField.getDouble(null) != 77) throw new RuntimeException("Test failed");
        if (stringField.get(null)       != myString) throw new RuntimeException("Test failed");

        // Test passed.
    }
}
