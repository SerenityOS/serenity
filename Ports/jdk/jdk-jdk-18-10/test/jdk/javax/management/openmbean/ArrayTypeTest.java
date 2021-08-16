/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5045358
 * @summary Test that Open MBeans support arrays of primitive types.
 * @author Luis-Miguel Alventosa
 *
 * @run clean ArrayTypeTest
 * @run build ArrayTypeTest
 * @run main ArrayTypeTest
 */

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.management.ObjectName;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.SimpleType;

public class ArrayTypeTest {

    private static final String toStringResult[] = {
        "javax.management.openmbean.ArrayType(name=[[Ljava.lang.String;,dimension=2,elementType=javax.management.openmbean.SimpleType(name=java.lang.String),primitiveArray=false)",
        "javax.management.openmbean.ArrayType(name=[I,dimension=1,elementType=javax.management.openmbean.SimpleType(name=java.lang.Integer),primitiveArray=true)",
        "javax.management.openmbean.ArrayType(name=[Ljava.lang.Integer;,dimension=1,elementType=javax.management.openmbean.SimpleType(name=java.lang.Integer),primitiveArray=false)",
        "javax.management.openmbean.ArrayType(name=[[[[I,dimension=4,elementType=javax.management.openmbean.SimpleType(name=java.lang.Integer),primitiveArray=true)",
        "javax.management.openmbean.ArrayType(name=[[[[Ljava.lang.Integer;,dimension=4,elementType=javax.management.openmbean.SimpleType(name=java.lang.Integer),primitiveArray=false)",
        "javax.management.openmbean.ArrayType(name=[Ljava.lang.String;,dimension=1,elementType=javax.management.openmbean.SimpleType(name=java.lang.String),primitiveArray=false)",
        "OpenDataException",
        "javax.management.openmbean.ArrayType(name=[Ljava.lang.Integer;,dimension=1,elementType=javax.management.openmbean.SimpleType(name=java.lang.Integer),primitiveArray=false)",
        "javax.management.openmbean.ArrayType(name=[[Ljava.lang.Integer;,dimension=2,elementType=javax.management.openmbean.SimpleType(name=java.lang.Integer),primitiveArray=false)",
        "javax.management.openmbean.ArrayType(name=[[I,dimension=2,elementType=javax.management.openmbean.SimpleType(name=java.lang.Integer),primitiveArray=true)",
        "javax.management.openmbean.ArrayType(name=[[[I,dimension=3,elementType=javax.management.openmbean.SimpleType(name=java.lang.Integer),primitiveArray=true)",
        "javax.management.openmbean.ArrayType(name=[F,dimension=1,elementType=javax.management.openmbean.SimpleType(name=java.lang.Float),primitiveArray=true)",
        "javax.management.openmbean.ArrayType(name=[[F,dimension=2,elementType=javax.management.openmbean.SimpleType(name=java.lang.Float),primitiveArray=true)",
        "javax.management.openmbean.ArrayType(name=[Ljavax.management.ObjectName;,dimension=1,elementType=javax.management.openmbean.SimpleType(name=javax.management.ObjectName),primitiveArray=false)",
        "javax.management.openmbean.ArrayType(name=[[Ljavax.management.ObjectName;,dimension=2,elementType=javax.management.openmbean.SimpleType(name=javax.management.ObjectName),primitiveArray=false)",
        "javax.management.openmbean.ArrayType(name=[[[Ljava.lang.String;,dimension=3,elementType=javax.management.openmbean.SimpleType(name=java.lang.String),primitiveArray=false)",
        "javax.management.openmbean.ArrayType(name=[[[Ljava.lang.String;,dimension=3,elementType=javax.management.openmbean.SimpleType(name=java.lang.String),primitiveArray=false)",
        "javax.management.openmbean.ArrayType(name=[I,dimension=1,elementType=javax.management.openmbean.SimpleType(name=java.lang.Integer),primitiveArray=true)",
        "javax.management.openmbean.ArrayType(name=[[Z,dimension=2,elementType=javax.management.openmbean.SimpleType(name=java.lang.Boolean),primitiveArray=true)",
        "javax.management.openmbean.ArrayType(name=[Ljava.lang.Long;,dimension=1,elementType=javax.management.openmbean.SimpleType(name=java.lang.Long),primitiveArray=false)",
        "javax.management.openmbean.ArrayType(name=[Ljava.lang.Double;,dimension=1,elementType=javax.management.openmbean.SimpleType(name=java.lang.Double),primitiveArray=false)",
    };

    private static int checkResult(int i, ArrayType a) {
        if (a.toString().equals(toStringResult[i])) {
            System.out.println("Test passed!");
            return 0;
        } else {
            System.out.println("Test failed!");
            return 1;
        }
    }

    private static int checkGetters(ArrayType a,
                                    String className,
                                    String description,
                                    String typeName,
                                    boolean isArray,
                                    boolean isPrimitiveArray,
                                    int dimension) {
        int error = 0;
        if (a.getClassName().equals(className)) {
            System.out.println("\tArrayType.getClassName() OK!");
        } else {
            System.out.println("\tArrayType.getClassName() KO!");
            System.out.println("\t\t---> expecting " + className);
            error++;
        }
        if (a.getDescription().equals(description)) {
            System.out.println("\tArrayType.getDescription() OK!");
        } else {
            System.out.println("\tArrayType.getDescription() KO!");
            System.out.println("\t\t---> expecting " + description);
            error++;
        }
        if (a.getTypeName().equals(typeName)) {
            System.out.println("\tArrayType.getTypeName() OK!");
        } else {
            System.out.println("\tArrayType.getTypeName() KO!");
            System.out.println("\t\t---> expecting " + typeName);
            error++;
        }
        if (a.isArray() == isArray) {
            System.out.println("\tArrayType.isArray() OK!");
        } else {
            System.out.println("\tArrayType.isArray() KO!");
            System.out.println("\t\t---> expecting " + isArray);
            error++;
        }
        if (a.isPrimitiveArray() == isPrimitiveArray) {
            System.out.println("\tArrayType.isPrimitiveArray() OK!");
        } else {
            System.out.println("\tArrayType.isPrimitiveArray() KO!");
            System.out.println("\t\t---> expecting " + isPrimitiveArray);
            error++;
        }
        if (a.getDimension() == dimension) {
            System.out.println("\tArrayType.getDimension() OK!");
        } else {
            System.out.println("\tArrayType.getDimension() KO!");
            System.out.println("\t\t---> expecting " + dimension);
            error++;
        }

        if (error > 0) {
            System.out.println("Test failed!");
            return 1;
        } else {
            System.out.println("Test passed!");
            return 0;
        }
    }

    private static void printArrayType(ArrayType a) {
        System.out.println("\tArrayType.getClassName() = " + a.getClassName());
        System.out.println("\tArrayType.getDescription() = " + a.getDescription());
        System.out.println("\tArrayType.getTypeName() = " + a.getTypeName());
        System.out.println("\tArrayType.isArray() = " + a.isArray());
        System.out.println("\tArrayType.isPrimitiveArray() = " + a.isPrimitiveArray());
        System.out.println("\tArrayType.getDimension() = " + a.getDimension());
    }

    public static void main(String[] args) throws Exception {

        System.out.println("\nTest that Open MBeans support arrays of primitive types.");

        int index = 0;
        int error = 0;

        //
        // Constructor tests
        //
        System.out.println("\n>>> Constructor tests");

        System.out.println("\nArrayType<String[][]> a1 = new ArrayType<String[][]>(2, SimpleType.STRING)");
        ArrayType<String[][]> a1 = new ArrayType<String[][]>(2, SimpleType.STRING);
        printArrayType(a1);
        error += checkResult(index++, a1);

        System.out.println("\nArrayType<int[]> a2 = new ArrayType<int[]>(SimpleType.INTEGER, true)");
        ArrayType<int[]> a2 = new ArrayType<int[]>(SimpleType.INTEGER, true);
        printArrayType(a2);
        error += checkResult(index++, a2);

        System.out.println("\nArrayType<Integer[]> a3 = new ArrayType<Integer[]>(SimpleType.INTEGER, false)");
        ArrayType<Integer[]> a3 = new ArrayType<Integer[]>(SimpleType.INTEGER, false);
        printArrayType(a3);
        error += checkResult(index++, a3);

        System.out.println("\nArrayType<int[][][][]> a4 = new ArrayType<int[][][][]>(3, a2)");
        ArrayType<int[][][][]> a4 = new ArrayType<int[][][][]>(3, a2);
        printArrayType(a4);
        error += checkResult(index++, a4);

        System.out.println("\nArrayType<Integer[][][][]> a5 = new ArrayType<Integer[][][][]>(3, a3)");
        ArrayType<Integer[][][][]> a5 = new ArrayType<Integer[][][][]>(3, a3);
        printArrayType(a5);
        error += checkResult(index++, a5);

        System.out.println("\nArrayType<String[]> a6 = new ArrayType<String[]>(SimpleType.STRING, false)");
        ArrayType<String[]> a6 = new ArrayType<String[]>(SimpleType.STRING, false);
        printArrayType(a6);
        error += checkResult(index++, a6);

        System.out.println("\nArrayType<String[]> a7 = new ArrayType<String[]>(SimpleType.STRING, true)");
        index++; // skip this dummy entry in the toStringResult array
        try {
            ArrayType<String[]> a7 =
                new ArrayType<String[]>(SimpleType.STRING, true);
            System.out.println("\tDid not get expected OpenDataException!");
            System.out.println("Test failed!");
            error++;
        } catch (OpenDataException e) {
            System.out.println("\tGot expected OpenDataException: " + e);
            System.out.println("Test passed!");
        }

        //
        // Factory tests
        //
        System.out.println("\n>>> Factory tests");

        System.out.println("\nArrayType<Integer[]> a8 = ArrayType.getArrayType(SimpleType.INTEGER)");
        ArrayType<Integer[]> a8 = ArrayType.getArrayType(SimpleType.INTEGER);
        printArrayType(a8);
        error += checkResult(index++, a8);

        System.out.println("\nArrayType<Integer[][]> a9 = ArrayType.getArrayType(a8)");
        ArrayType<Integer[][]> a9 = ArrayType.getArrayType(a8);
        printArrayType(a9);
        error += checkResult(index++, a9);

        System.out.println("\nArrayType<int[][]> a10 = ArrayType.getPrimitiveArrayType(int[][].class)");
        ArrayType<int[][]> a10 = ArrayType.getPrimitiveArrayType(int[][].class);
        printArrayType(a10);
        error += checkResult(index++, a10);

        System.out.println("\nArrayType<int[][][]> a11 = ArrayType.getArrayType(a10)");
        ArrayType<int[][][]> a11 = ArrayType.getArrayType(a10);
        printArrayType(a11);
        error += checkResult(index++, a11);

        System.out.println("\nArrayType<float[]> a12 = ArrayType.getPrimitiveArrayType(float[].class)");
        ArrayType<float[]> a12 = ArrayType.getPrimitiveArrayType(float[].class);
        printArrayType(a12);
        error += checkResult(index++, a12);

        System.out.println("\nArrayType<float[][]> a13 = ArrayType.getArrayType(a12)");
        ArrayType<float[][]> a13 = ArrayType.getArrayType(a12);
        printArrayType(a13);
        error += checkResult(index++, a13);

        System.out.println("\nArrayType<ObjectName[]> a14 = ArrayType.getArrayType(SimpleType.OBJECTNAME)");
        ArrayType<ObjectName[]> a14 = ArrayType.getArrayType(SimpleType.OBJECTNAME);
        printArrayType(a14);
        error += checkResult(index++, a14);

        System.out.println("\nArrayType<ObjectName[][]> a15 = ArrayType.getArrayType(a14)");
        ArrayType<ObjectName[][]> a15 = ArrayType.getArrayType(a14);
        printArrayType(a15);
        error += checkResult(index++, a15);

        System.out.println("\nArrayType<String[][][]> a16 = new ArrayType<String[][][]>(3, SimpleType.STRING)");
        ArrayType<String[][][]> a16 = new ArrayType<String[][][]>(3, SimpleType.STRING);
        printArrayType(a16);
        error += checkResult(index++, a16);

        System.out.println("\nArrayType<String[]> a17 = new ArrayType<String[]>(1, SimpleType.STRING)");
        System.out.println("ArrayType<String[][]> a18 = new ArrayType<String[][]>(1, a17)");
        System.out.println("ArrayType<String[][][]> a19 = new ArrayType<String[][][]>(1, a18)");
        ArrayType<String[]> a17 = new ArrayType<String[]>(1, SimpleType.STRING);
        ArrayType<String[][]> a18 = new ArrayType<String[][]>(1, a17);
        ArrayType<String[][][]> a19 = new ArrayType<String[][][]>(1, a18);
        printArrayType(a19);
        error += checkResult(index++, a19);

        //
        // Serialization tests
        //
        System.out.println("\n>>> Serialization tests\n");

        ArrayType<int[]> i1 = ArrayType.getPrimitiveArrayType(int[].class);
        ArrayType<int[]> i2 = null;

        ArrayType<boolean[][]> b1 = ArrayType.getPrimitiveArrayType(boolean[][].class);
        ArrayType<boolean[][]> b2 = null;

        ArrayType<Long[]> l1 = ArrayType.getArrayType(SimpleType.LONG);
        ArrayType<Long[]> l2 = null;

        ArrayType<Double[]> d1 = ArrayType.getArrayType(SimpleType.DOUBLE);
        ArrayType<Double[]> d2 = null;

        // serialize the objects
        try {
            FileOutputStream fo = new FileOutputStream("serial.tmp");
            ObjectOutputStream so = new ObjectOutputStream(fo);
            System.out.println("Serialize ArrayType<int[]> i1 = ArrayType.getPrimitiveArrayType(int[].class)");
            so.writeObject(i1);
            System.out.println("Serialize ArrayType<boolean[][]> b1 = ArrayType.getPrimitiveArrayType(boolean[][].class)");
            so.writeObject(b1);
            System.out.println("Serialize ArrayType<Long[]> l1 = ArrayType.getArrayType(SimpleType.LONG)");
            so.writeObject(l1);
            System.out.println("Serialize ArrayType<Double[]> d1 = ArrayType.getArrayType(SimpleType.DOUBLE)");
            so.writeObject(d1);
            so.flush();
        } catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }

        // deserialize the objects
        try {
            FileInputStream fi = new FileInputStream("serial.tmp");
            ObjectInputStream si = new ObjectInputStream(fi);
            System.out.println("Deserialize ArrayType<int[]> i1 = ArrayType.getPrimitiveArrayType(int[].class)");
            i2 = (ArrayType<int[]>) si.readObject();
            System.out.println("Deserialize ArrayType<boolean[][]> b1 = ArrayType.getPrimitiveArrayType(boolean[][].class)");
            b2 = (ArrayType<boolean[][]>) si.readObject();
            System.out.println("Deserialize ArrayType<Long[]> l1 = ArrayType.getArrayType(SimpleType.LONG)");
            l2 = (ArrayType<Long[]>) si.readObject();
            System.out.println("Deserialize ArrayType<Double[]> d1 = ArrayType.getArrayType(SimpleType.DOUBLE)");
            d2 = (ArrayType<Double[]>) si.readObject();
        } catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }

        if (i1.toString().equals(toStringResult[index++]) &&
            i1.toString().equals(i2.toString())) {
            System.out.println("Test passed for ArrayType<int[]> i1 = ArrayType.getPrimitiveArrayType(int[].class)");
        } else {
            System.out.println("Test failed for ArrayType<int[]> i1 = ArrayType.getPrimitiveArrayType(int[].class)");
            error++;
        }
        if (b1.toString().equals(toStringResult[index++]) &&
            b1.toString().equals(b2.toString())) {
            System.out.println("Test passed for ArrayType<boolean[][]> b1 = ArrayType.getPrimitiveArrayType(boolean[][].class)");
        } else {
            System.out.println("Test failed for ArrayType<boolean[][]> b1 = ArrayType.getPrimitiveArrayType(boolean[][].class)");
            error++;
        }
        if (l1.toString().equals(toStringResult[index++]) &&
            l1.toString().equals(l2.toString())) {
            System.out.println("Test passed for ArrayType<Long[]> l1 = ArrayType.getArrayType(SimpleType.LONG)");
        } else {
            System.out.println("Test failed for ArrayType<Long[]> l1 = ArrayType.getArrayType(SimpleType.LONG)");
            error++;
        }
        if (d1.toString().equals(toStringResult[index++]) &&
            d1.toString().equals(d2.toString())) {
            System.out.println("Test passed for ArrayType<Double[]> d1 = ArrayType.getArrayType(SimpleType.DOUBLE)");
        } else {
            System.out.println("Test failed for ArrayType<Double[]> d1 = ArrayType.getArrayType(SimpleType.DOUBLE)");
            error++;
        }

        //
        // Test getters
        //
        System.out.println("\n>>> Getter tests");

        System.out.println("\nArrayType<Integer[][]> g1 = new ArrayType<Integer[][]>(2, SimpleType.INTEGER)");
        ArrayType<Integer[][]> g1 = new ArrayType<Integer[][]>(2, SimpleType.INTEGER);
        printArrayType(g1);
        error += checkGetters(g1,
                              "[[Ljava.lang.Integer;",
                              "2-dimension array of java.lang.Integer",
                              "[[Ljava.lang.Integer;",
                              true,
                              false,
                              2);

        System.out.println("\nArrayType<int[][]> g2 = ArrayType.getPrimitiveArrayType(int[][].class)");
        ArrayType<int[][]> g2 = ArrayType.getPrimitiveArrayType(int[][].class);
        printArrayType(g2);
        error += checkGetters(g2,
                              "[[I",
                              "2-dimension array of int",
                              "[[I",
                              true,
                              true,
                              2);

        if (error > 0) {
            final String msg = "\nTest FAILED! Got " + error + " error(s)";
            System.out.println(msg);
            throw new IllegalArgumentException(msg);
        } else {
            System.out.println("\nTest PASSED!");
        }
    }
}
