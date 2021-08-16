/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8162817 8168921
 * @summary Test of toString on normal annotations
 */

// See also the sibling compile-time test
// test/langtools/tools/javac/processing/model/element/AnnotationToStringTest.java

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * The expected string values are stored in @ExpectedString
 * annotations. The essence of the test is comparing the toString()
 * result of annotations to the corresponding ExpectedString.value().
 */

public class AnnotationToStringTest {
    public static void main(String... args) throws Exception {
        int failures = 0;

        failures += check(PrimHost.class.getAnnotation(ExpectedString.class).value(),
                          PrimHost.class.getAnnotation(MostlyPrimitive.class).toString());
        failures += classyTest();
        failures += arrayAnnotationTest();

        if (failures > 0)
            throw new RuntimeException(failures + " failures");
    }

    private static int check(String expected, String actual) {
        if (!expected.equals(actual)) {
            System.err.printf("ERROR: Expected ''%s'';%ngot             ''%s''.\n",
                              expected, actual);
            return 1;
        } else
            return 0;
    }

    @ExpectedString(
        "@MostlyPrimitive(c0='a', "+
        "c1='\\'', " +
        "b0=(byte)0x01, " +
        "i0=1, " +
        "i1=2, " +
        "f0=1.0f, " +
        "f1=0.0f/0.0f, " +
        "d0=0.0, " +
        "d1=1.0/0.0, " +
        "l0=5L, " +
        "l1=9223372036854775807L, " +
        "l2=-9223372036854775808L, " +
        "l3=-2147483648L, " +
        "s0=\"Hello world.\", " +
        "s1=\"a\\\"b\", " +
        "class0=Obj[].class, " +
        "classArray={Obj[].class})")
    @MostlyPrimitive(
        c0='a',
        c1='\'',
        b0=1,
        i0=1,
        i1=2,
        f0=1.0f,
        f1=Float.NaN,
        d0=0.0,
        d1=2.0/0.0,
        l0=5,
        l1=Long.MAX_VALUE,
        l2=Long.MIN_VALUE,
        l3=Integer.MIN_VALUE,
        s0="Hello world.",
        s1="a\"b",
        class0=Obj[].class,
        classArray={Obj[].class}
    )
    static class PrimHost{}

    private static int classyTest() {
        int failures = 0;
        for (Field f : AnnotationHost.class.getFields()) {
            Annotation a = f.getAnnotation(Classy.class);
            System.out.println(a);
            failures += check(f.getAnnotation(ExpectedString.class).value(),
                              a.toString());
        }
        return failures;
    }

    static class AnnotationHost {
        @ExpectedString(
       "@Classy(Obj.class)")
        @Classy(Obj.class)
        public int f0;

        @ExpectedString(
       "@Classy(Obj[].class)")
        @Classy(Obj[].class)
        public int f1;

        @ExpectedString(
       "@Classy(Obj[][].class)")
        @Classy(Obj[][].class)
        public int f2;

        @ExpectedString(
       "@Classy(Obj[][][].class)")
        @Classy(Obj[][][].class)
        public int f3;

        @ExpectedString(
       "@Classy(int.class)")
        @Classy(int.class)
        public int f4;

        @ExpectedString(
       "@Classy(int[][][].class)")
        @Classy(int[][][].class)
        public int f5;
    }

    /**
     * Each field should have two annotations, the first being
     * @ExpectedString and the second the annotation under test.
     */
    private static int arrayAnnotationTest() {
        int failures = 0;
        for (Field f : ArrayAnnotationHost.class.getFields()) {
            Annotation[] annotations = f.getAnnotations();
            System.out.println(annotations[1]);
            failures += check(((ExpectedString)annotations[0]).value(),
                              annotations[1].toString());
        }
        return failures;
    }

    static class ArrayAnnotationHost {
        @ExpectedString(
       "@BooleanArray({true, false, true})")
        @BooleanArray({true, false, true})
        public boolean[]   f0;

        @ExpectedString(
       "@FloatArray({3.0f, 4.0f, 0.0f/0.0f, -1.0f/0.0f, 1.0f/0.0f})")
        @FloatArray({3.0f, 4.0f, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY})
        public float[]     f1;

        @ExpectedString(
       "@DoubleArray({1.0, 2.0, 0.0/0.0, 1.0/0.0, -1.0/0.0})")
        @DoubleArray({1.0, 2.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,})
        public double[]    f2;

        @ExpectedString(
       "@ByteArray({(byte)0x0a, (byte)0x0b, (byte)0x0c})")
        @ByteArray({10, 11, 12})
        public byte[]      f3;

        @ExpectedString(
       "@ShortArray({0, 4, 5})")
        @ShortArray({0, 4, 5})
        public short[]     f4;

        @ExpectedString(
       "@CharArray({'a', 'b', 'c', '\\''})")
        @CharArray({'a', 'b', 'c', '\''})
        public char[]      f5;

        @ExpectedString(
       "@IntArray({1})")
        @IntArray({1})
        public int[]       f6;

        @ExpectedString(
       "@LongArray({-9223372036854775808L, -2147483649L, -2147483648L," +
                " -2147483647L, 2147483648L, 9223372036854775807L})")
        @LongArray({Long.MIN_VALUE, Integer.MIN_VALUE-1L, Integer.MIN_VALUE,
                -Integer.MAX_VALUE, Integer.MAX_VALUE+1L, Long.MAX_VALUE})
        public long[]      f7;

        @ExpectedString(
       "@StringArray({\"A\", \"B\", \"C\", \"\\\"Quote\\\"\"})")
        @StringArray({"A", "B", "C", "\"Quote\""})
        public String[]    f8;

        @ExpectedString(
       "@ClassArray({int.class, Obj[].class})")
        @ClassArray({int.class, Obj[].class})
        public Class<?>[]  f9;

        @ExpectedString(
       "@EnumArray({SOURCE})")
        @EnumArray({RetentionPolicy.SOURCE})
        public RetentionPolicy[]  f10;
    }
}

// ------------ Supporting types ------------

class Obj {}

@Retention(RetentionPolicy.RUNTIME)
@interface ExpectedString {
    String value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface Classy {
    Class<?> value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface BooleanArray {
    boolean[] value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface FloatArray {
    float[] value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface DoubleArray {
    double[] value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface ByteArray {
    byte[] value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface ShortArray {
    short[] value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface CharArray {
    char[] value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface IntArray {
    int[] value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface LongArray {
    long[] value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface ClassArray {
    Class<?>[] value() default {int.class, Obj[].class};
}

@Retention(RetentionPolicy.RUNTIME)
@interface StringArray {
    String[] value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface EnumArray {
    RetentionPolicy[] value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface MostlyPrimitive {
    char   c0();
    char   c1();
    byte   b0();
    int    i0();
    int    i1();
    float  f0();
    float  f1();
    double d0();
    double d1();
    long   l0();
    long   l1();
    long   l2();
    long   l3();
    String s0();
    String s1();
    Class<?> class0();
    Class<?>[] classArray();
}
