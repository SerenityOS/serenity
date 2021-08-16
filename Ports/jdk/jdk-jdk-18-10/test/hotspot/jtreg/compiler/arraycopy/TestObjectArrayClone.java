/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8155643 8268125 8270461 8270098
 * @summary Test Object.clone() intrinsic.
 * @modules java.base/java.lang:+open
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:-ReduceInitialCardMarks
 *                   -XX:CompileCommand=compileonly,compiler.arraycopy.TestObjectArrayClone::testClone*
 *                   -XX:CompileCommand=compileonly,jdk.internal.reflect.GeneratedMethodAccessor*::invoke
 *                   compiler.arraycopy.TestObjectArrayClone
 * @run main/othervm -XX:CompileCommand=compileonly,compiler.arraycopy.TestObjectArrayClone::testClone*
 *                   -XX:CompileCommand=compileonly,jdk.internal.reflect.GeneratedMethodAccessor*::invoke
 *                   compiler.arraycopy.TestObjectArrayClone
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:-UseCompressedClassPointers -Xmx128m
 *                   -XX:CompileCommand=compileonly,compiler.arraycopy.TestObjectArrayClone::testClone*
 *                   -XX:CompileCommand=compileonly,jdk.internal.reflect.GeneratedMethodAccessor*::invoke
 *                   compiler.arraycopy.TestObjectArrayClone
 * @run main/othervm -Xbatch -XX:-UseTypeProfile
 *                   -XX:CompileCommand=compileonly,compiler.arraycopy.TestObjectArrayClone::testClone*
 *                   -XX:CompileCommand=compileonly,jdk.internal.reflect.GeneratedMethodAccessor*::invoke
 *                   compiler.arraycopy.TestObjectArrayClone
 */

package compiler.arraycopy;

import java.lang.invoke.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class Payload implements Cloneable {
    boolean b;
    int i;
    char c;
    String str;
    short s;
    int i2;

    public Payload(boolean b, int i, char c, String str, short s, int i2) {
        super();
        this.b = b;
        this.i = i;
        this.c = c;
        this.str = str;
        this.s = s;
        this.i2 = i2;
    }

    public Payload clonep() {
        try {
            return (Payload) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}

class Payload2 implements Cloneable {
    boolean b;
    int i;
    char c;
    String str;
    short s;
    int i2;
    boolean b2;
    int i3;
    char c2;
    String str2;
    short s2;
    int i4;

    public Payload2(boolean b, int i, char c, String str, short s, int i2, boolean b2, int i3, char c2, String str2,
            short s2, int i4) {
        super();
        this.b = b;
        this.i = i;
        this.c = c;
        this.str = str;
        this.s = s;
        this.i2 = i2;
        this.b2 = b2;
        this.i3 = i3;
        this.c2 = c2;
        this.str2 = str2;
        this.s2 = s2;
        this.i4 = i4;
    }

    public Payload2 clonep() {
        try {
            return (Payload2) super.clone();
        } catch(CloneNotSupportedException e) {
            return null;
        }
    }
}

public class TestObjectArrayClone {

    public static String[] escape_arr;

    public static String str1 = new String("1");
    public static String str2 = new String("2");
    public static String str3 = new String("3");
    public static String str4 = new String("4");
    public static String str5 = new String("5");

    public static String[] testCloneObjectArray(String[] arr) {
        return arr.clone();
    }

    public static String[] testCloneObjectArrayCopy(String[] arr) {
        String[] arr2 = new String[arr.length];
        System.arraycopy(arr, 0, arr2, 0, arr.length);
        return arr2;
    }

    public static String[] testCloneShortObjectArray() {
        String[] arr = new String[5];
        arr[0] = str1;
        arr[1] = str2;
        arr[2] = str3;
        arr[3] = str4;
        arr[4] = str5;
        escape_arr = arr;
        return arr.clone();
    }

    public static String[] testCloneShortObjectArray2(Method clone) throws Exception {
        String[] arr = new String[5];
        arr[0] = str1;
        arr[1] = str2;
        arr[2] = str3;
        arr[3] = str4;
        arr[4] = str5;
        escape_arr = arr;
        return (String[]) testCloneObject(clone, arr);
    }

    public static String[] testCloneShortObjectArrayCopy() {
        String[] arr = new String[5];
        arr[0] = str1;
        arr[1] = str2;
        arr[2] = str3;
        arr[3] = str4;
        arr[4] = str5;
        escape_arr = arr;
        String[] arr2 = new String[arr.length];
        System.arraycopy(arr, 0, arr2, 0, arr.length);
        return arr2;
    }

    public static int[] testClonePrimitiveArray(int[] arr) {
        return arr.clone();
    }

    public static Object testCloneOop(Payload p) {
      return p.clonep();
    }

    public static Object testCloneOop2(Payload2 p) {
        return p.clonep();
    }

    public static Object testCloneObject(Method clone, Object obj) throws Exception {
        return clone.invoke(obj);
    }

    public static void main(String[] args) throws Exception {
        Method clone = Object.class.getDeclaredMethod("clone");
        clone.setAccessible(true);

        String[] arr1 = new String[42];
        for (int j = 0; j < arr1.length; j++) {
            arr1[j] = new String(Integer.toString(j));
        }

        for (int i = 0; i < 50_000; i++) {
            String[] arr2 = testCloneObjectArray(arr1);
            verifyStr(arr1, arr2);
            String[] arr3 = testCloneObjectArray(arr1);
            verifyStr(arr1, arr3);
            String[] arr4 = testCloneObjectArray(arr1);
            verifyStr(arr1, arr4);
            verifyStr(arr1, arr3);
            verifyStr(arr1, arr2);
        }

        for (int i = 0; i < 50_000; i++) {
            for (int j = 0; j < arr1.length; j++) {
                arr1[j] = new String(Integer.toString(j));
            }
            String[] arr2 = (String[]) testCloneObject(clone, arr1);
            verifyStr(arr1, arr2);
            String[] arr3 = (String[]) testCloneObject(clone, arr1);
            verifyStr(arr1, arr3);
            String[] arr4 = (String[]) testCloneObject(clone, arr1);
            verifyStr(arr1, arr4);
            verifyStr(arr1, arr3);
            verifyStr(arr1, arr2);
        }

        for (int i = 0; i < 50_000; i++) {
            String[] value = testCloneShortObjectArray();
            verifyStr(value, escape_arr);
            String[] value2 = testCloneShortObjectArray();
            verifyStr(value2, escape_arr);
            String[] value3 = testCloneShortObjectArray();
            verifyStr(value3, escape_arr);
            String[] value4 = testCloneShortObjectArray2(clone);
            verifyStr(value4, escape_arr);
            verifyStr(value, value4);
            verifyStr(value, value3);
            verifyStr(value, value2);
        }

        for (int i = 0; i < 50_000; i++) {
            String[] arr2 = testCloneObjectArrayCopy(arr1);
            verifyStr(arr1, arr2);
            String[] arr3 = testCloneObjectArrayCopy(arr1);
            verifyStr(arr1, arr3);
            String[] arr4 = testCloneObjectArrayCopy(arr1);
            verifyStr(arr1, arr4);
            verifyStr(arr1, arr3);
            verifyStr(arr1, arr2);
        }

        for (int i = 0; i < 50_000; i++) {
            String[] value = testCloneShortObjectArrayCopy();
            verifyStr(value, escape_arr);
            String[] value2 = testCloneShortObjectArrayCopy();
            verifyStr(value2, escape_arr);
            String[] value3 = testCloneShortObjectArrayCopy();
            verifyStr(value3, escape_arr);
            verifyStr(value, value3);
            verifyStr(value, value2);
        }

        int[] arr2 = new int[42];
        for (int i = 0; i < arr2.length; i++) {
            arr2[i] = i;
        }
        for (int i = 0; i < 50_000; i++) {
            int[] res1 = testClonePrimitiveArray(arr2);
            int[] res2 = (int[])testCloneObject(clone, arr2);
            for (int j = 0; j < arr2.length; j++) {
                if (res1[j] != j) {
                    throw new RuntimeException("Unexpected result: " + res1[j] + " != " + j);
                }
                if (res2[j] != j) {
                    throw new RuntimeException("Unexpected result: " + res2[j] + " != " + j);
                }
            }
        }

        Payload ref = new Payload(false, -1, 'c', str1, (short) 5, -1);
        for (int i = 0; i < 50_000; i++) {
            Payload p1 = (Payload) testCloneOop(ref);
            verifyPayload(ref, p1);
            Payload p2 = (Payload) testCloneOop(ref);
            verifyPayload(ref, p2);
            Payload p3 = (Payload) testCloneOop(ref);
            verifyPayload(ref, p3);
            verifyPayload(p2, p3);
            verifyPayload(p1, p3);
        }

        for (int i = 0; i < 50_000; i++) {
            Payload p1 = (Payload) testCloneObject(clone, ref);
            verifyPayload(ref, p1);
            Payload p2 = (Payload) testCloneObject(clone, ref);
            verifyPayload(ref, p2);
            Payload p3 = (Payload) testCloneObject(clone, ref);
            verifyPayload(ref, p3);
            verifyPayload(p2, p3);
            verifyPayload(p1, p3);
        }

        Payload2 ref2 = new Payload2(false, -1, 'c', str1, (short) 5, -1, false, 0, 'k', str2, (short)-1, 0);
        for (int i = 0; i < 50_000; i++) {
            Payload2 p1 = (Payload2) testCloneOop2(ref2);
            verifyPayload2(ref2, p1);
            Payload2 p2 = (Payload2) testCloneOop2(ref2);
            verifyPayload2(ref2, p2);
            Payload2 p3 = (Payload2) testCloneOop2(ref2);
            verifyPayload2(ref2, p3);
            verifyPayload2(p2, p3);
            verifyPayload2(p1, p3);
        }

        for (int i = 0; i < 50_000; i++) {
            Payload2 p1 = (Payload2) testCloneObject(clone, ref2);
            verifyPayload2(ref2, p1);
            Payload2 p2 = (Payload2) testCloneObject(clone, ref2);
            verifyPayload2(ref2, p2);
            Payload2 p3 = (Payload2) testCloneObject(clone, ref2);
            verifyPayload2(ref2, p3);
            verifyPayload2(p2, p3);
            verifyPayload2(p1, p3);
        }
    }

    public static void verifyPayload(Payload p1, Payload p2) {
        if  (p1.b != p2.b) {
            throw new RuntimeException("b is wrong");
        }
        if  (p1.c != p2.c) {
            throw new RuntimeException("c is wrong");
        }
        if  (p1.i != p2.i) {
            throw new RuntimeException("i is wrong");
        }
        if  (p1.s != p2.s) {
            throw new RuntimeException("s is wrong");
        }
        if  (p1.i2 != p2.i2) {
            throw new RuntimeException("i2 is wrong");
        }
        if  (p1.str != p2.str) {
            throw new RuntimeException("str is wrong");
        }
        if  (!p1.str.equals(p2.str)) {
            throw new RuntimeException("str content is wrong");
        }
    }

    public static void verifyPayload2(Payload2 p1, Payload2 p2) {
        if  (p1.b != p2.b) {
            throw new RuntimeException("b is wrong");
        }
        if  (p1.c != p2.c) {
            throw new RuntimeException("c is wrong");
        }
        if  (p1.i != p2.i) {
            throw new RuntimeException("i is wrong");
        }
        if  (p1.s != p2.s) {
            throw new RuntimeException("s is wrong");
        }
        if  (p1.i2 != p2.i2) {
            throw new RuntimeException("i2 is wrong");
        }
        if  (p1.str != p2.str) {
            throw new RuntimeException("str is wrong");
        }
        if  (!p1.str.equals(p2.str)) {
            throw new RuntimeException("str content is wrong");
        }
        if  (p1.b2 != p2.b2) {
            throw new RuntimeException("b is wrong");
        }
        if  (p1.c2 != p2.c2) {
            throw new RuntimeException("c is wrong");
        }
        if  (p1.i3 != p2.i3) {
            throw new RuntimeException("i is wrong");
        }
        if  (p1.s2 != p2.s2) {
            throw new RuntimeException("s is wrong");
        }
        if  (p1.i4 != p2.i4) {
            throw new RuntimeException("i2 is wrong");
        }
        if  (p1.str2 != p2.str2) {
            throw new RuntimeException("str is wrong");
        }
        if  (!p1.str2.equals(p2.str2)) {
            throw new RuntimeException("str content is wrong");
        }
    }

    public static void verifyStr(String[] arr1, String[] arr2) {
        if (arr1 == arr2) {
            throw new RuntimeException("Must not be the same");
        }
        if (arr1.length != arr2.length) {
            throw new RuntimeException("Must have the same length");
        }
        for (int i = 0; i < arr1.length; i++) {
            if (arr1[i] != arr2[i]) {
                throw new RuntimeException("Fail cloned element not the same: " + i);
            }
            if (!arr1[i].equals(arr2[i])) {
                throw new RuntimeException("Fail cloned element content not the same");
            }
        }
    }
}

