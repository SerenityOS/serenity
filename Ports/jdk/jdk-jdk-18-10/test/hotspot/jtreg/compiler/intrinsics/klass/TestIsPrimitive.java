/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8150669
 * @bug 8233019
 * @summary C1 intrinsic for Class.isPrimitive
 * @modules java.base/jdk.internal.misc
 *
 * @run main/othervm -ea -Diters=200   -Xint
 *      compiler.intrinsics.klass.TestIsPrimitive
 * @run main/othervm -ea -XX:-UseSharedSpaces -Diters=30000 -XX:TieredStopAtLevel=1
 *      compiler.intrinsics.klass.TestIsPrimitive
 * @run main/othervm -ea -Diters=30000 -XX:TieredStopAtLevel=4
 *      compiler.intrinsics.klass.TestIsPrimitive
 */

package compiler.intrinsics.klass;

import java.util.concurrent.Callable;

public class TestIsPrimitive {
    static final int ITERS = Integer.getInteger("iters", 1);

    public static void main(String... args) throws Exception {
        testOK(true,  InlineConstants::testBoolean);
        testOK(true,  InlineConstants::testByte);
        testOK(true,  InlineConstants::testShort);
        testOK(true,  InlineConstants::testChar);
        testOK(true,  InlineConstants::testInt);
        testOK(true,  InlineConstants::testFloat);
        testOK(true,  InlineConstants::testLong);
        testOK(true,  InlineConstants::testDouble);
        testOK(false, InlineConstants::testObject);
        testOK(false, InlineConstants::testArray);
        testOK(false, InlineConstants::testBooleanArray);

        testOK(true,  StaticConstants::testBoolean);
        testOK(true,  StaticConstants::testByte);
        testOK(true,  StaticConstants::testShort);
        testOK(true,  StaticConstants::testChar);
        testOK(true,  StaticConstants::testInt);
        testOK(true,  StaticConstants::testFloat);
        testOK(true,  StaticConstants::testLong);
        testOK(true,  StaticConstants::testDouble);
        testOK(false, StaticConstants::testObject);
        testOK(false, StaticConstants::testArray);
        testOK(false, StaticConstants::testBooleanArray);
        testNPE(      StaticConstants::testNull);

        testOK(true,  NoConstants::testBoolean);
        testOK(true,  NoConstants::testByte);
        testOK(true,  NoConstants::testShort);
        testOK(true,  NoConstants::testChar);
        testOK(true,  NoConstants::testInt);
        testOK(true,  NoConstants::testFloat);
        testOK(true,  NoConstants::testLong);
        testOK(true,  NoConstants::testDouble);
        testOK(false, NoConstants::testObject);
        testOK(false, NoConstants::testArray);
        testOK(false, NoConstants::testBooleanArray);
        testNPE(      NoConstants::testNull);
    }

    public static void testOK(boolean expected, Callable<Object> test) throws Exception {
        for (int c = 0; c < ITERS; c++) {
            Object res = test.call();
            if (!res.equals(expected)) {
                throw new IllegalStateException("Wrong result: expected = " + expected + ", but got " + res);
            }
        }
    }

    static volatile Object sink;

    public static void testNPE(Callable<Object> test) throws Exception {
        for (int c = 0; c < ITERS; c++) {
            try {
               sink = test.call();
               throw new IllegalStateException("Expected NPE");
            } catch (NullPointerException iae) {
               // expected
            }
        }
    }

    static volatile Class<?> classBoolean = boolean.class;
    static volatile Class<?> classByte    = byte.class;
    static volatile Class<?> classShort   = short.class;
    static volatile Class<?> classChar    = char.class;
    static volatile Class<?> classInt     = int.class;
    static volatile Class<?> classFloat   = float.class;
    static volatile Class<?> classLong    = long.class;
    static volatile Class<?> classDouble  = double.class;
    static volatile Class<?> classObject  = Object.class;
    static volatile Class<?> classArray   = Object[].class;
    static volatile Class<?> classNull    = null;
    static volatile Class<?> classBooleanArray = boolean[].class;

    static final Class<?> staticClassBoolean = boolean.class;
    static final Class<?> staticClassByte    = byte.class;
    static final Class<?> staticClassShort   = short.class;
    static final Class<?> staticClassChar    = char.class;
    static final Class<?> staticClassInt     = int.class;
    static final Class<?> staticClassFloat   = float.class;
    static final Class<?> staticClassLong    = long.class;
    static final Class<?> staticClassDouble  = double.class;
    static final Class<?> staticClassObject  = Object.class;
    static final Class<?> staticClassArray   = Object[].class;
    static final Class<?> staticClassNull    = null;
    static final Class<?> staticClassBooleanArray = boolean[].class;

    static class InlineConstants {
        static boolean testBoolean() { return boolean.class.isPrimitive();  }
        static boolean testByte()    { return byte.class.isPrimitive();     }
        static boolean testShort()   { return short.class.isPrimitive();    }
        static boolean testChar()    { return char.class.isPrimitive();     }
        static boolean testInt()     { return int.class.isPrimitive();      }
        static boolean testFloat()   { return float.class.isPrimitive();    }
        static boolean testLong()    { return long.class.isPrimitive();     }
        static boolean testDouble()  { return double.class.isPrimitive();   }
        static boolean testObject()  { return Object.class.isPrimitive();   }
        static boolean testArray()   { return Object[].class.isPrimitive(); }
        static boolean testBooleanArray() { return boolean[].class.isPrimitive(); }
    }

    static class StaticConstants {
        static boolean testBoolean() { return staticClassBoolean.isPrimitive(); }
        static boolean testByte()    { return staticClassByte.isPrimitive();    }
        static boolean testShort()   { return staticClassShort.isPrimitive();   }
        static boolean testChar()    { return staticClassChar.isPrimitive();    }
        static boolean testInt()     { return staticClassInt.isPrimitive();     }
        static boolean testFloat()   { return staticClassFloat.isPrimitive();   }
        static boolean testLong()    { return staticClassLong.isPrimitive();    }
        static boolean testDouble()  { return staticClassDouble.isPrimitive();  }
        static boolean testObject()  { return staticClassObject.isPrimitive();  }
        static boolean testArray()   { return staticClassArray.isPrimitive();   }
        static boolean testNull()    { return staticClassNull.isPrimitive();    }
        static boolean testBooleanArray() { return staticClassBooleanArray.isPrimitive(); }
    }

    static class NoConstants {
        static boolean testBoolean() { return classBoolean.isPrimitive(); }
        static boolean testByte()    { return classByte.isPrimitive();    }
        static boolean testShort()   { return classShort.isPrimitive();   }
        static boolean testChar()    { return classChar.isPrimitive();    }
        static boolean testInt()     { return classInt.isPrimitive();     }
        static boolean testFloat()   { return classFloat.isPrimitive();   }
        static boolean testLong()    { return classLong.isPrimitive();    }
        static boolean testDouble()  { return classDouble.isPrimitive();  }
        static boolean testObject()  { return classObject.isPrimitive();  }
        static boolean testArray()   { return classArray.isPrimitive();   }
        static boolean testNull()    { return classNull.isPrimitive();    }
        static boolean testBooleanArray() { return classBooleanArray.isPrimitive();    }
    }


}

