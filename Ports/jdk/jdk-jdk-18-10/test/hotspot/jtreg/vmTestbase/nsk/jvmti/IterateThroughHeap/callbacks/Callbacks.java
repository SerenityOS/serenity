/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase nsk/jvmti/IterateThroughHeap/callbacks.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * This test exercises JVMTI function IterateThroughHeap().
 * Test checks that for all test objects appropriate callback will be invoked with
 * correct arguments regardless to reachability of an object, it's visability
 * and regardless to whether or not field is static.
 * During this test no particular klass- or heap-filter passed into IterateThroughHeap.
 * Test itertaes over instances of four classes which differs only by visibility of
 * their members and tags these members.
 * Tag contains information about index in array with information about objects,
 * and index in array with fields info.
 * If field is non-primitive then it will be tag directly, but if field is primitive,
 * then it will be tagged undirectly - by tagging an object it belongs to or class in
 * case when field is static.
 * During the test IterateThtoughHeap invoked two times: find reachable objects
 * and then to find unreachable objects.
 * Test asserts that each of four available callbacks will be invoked appropriate number
 * of times and value passed to callback is correct.
 * Tags with format described above are used to distinguise between different
 * fields and to be able to compare it's value with appropriate expected value.
 * All errors associated with incorrect values passed into callback are reported
 * immediatly.
 * After each IterateThroughHeap call test verifies information collected during
 * heap iteration. For each field following amount of callback invocations are expected:
 *   - primitive field          1 invocation  (jvmtiPrimitiveFieldCallback);
 *   - primitive array          2 invocations (jvmtiArrayPrimitiveValueCallback,
 *                                             jvmtiHeapIterationCallback);
 *   - java.lang.String field   2 invocations (jvmtiStringPrimitiveValueCallback,
 *                                             jvmtiHeapIterationCallback);
 *   - other objetcts           1 invocation  (jvmtiHeapIterationCallback).
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:Callbacks=-waittime=5
 *      nsk.jvmti.IterateThroughHeap.callbacks.Callbacks
 */

package nsk.jvmti.IterateThroughHeap.callbacks;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class Callbacks extends DebugeeClass {

    static {
        loadLibrary("Callbacks");
    }

    public static void main(String args[]) {
        String[] argv = JVMTITest.commonInit(args);
        System.exit(new Callbacks().runTest(argv,System.out) + Consts.JCK_STATUS_BASE);
    }

    protected Log log = null;
    protected ArgumentHandler argHandler = null;
    protected int status = Consts.TEST_PASSED;

    static protected Object testObjects[];

    public int runTest(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        testObjects = new Object[]{new PublicPrimitives(),
                                   new ProtectedPrimitives(),
                                   new PrivatePrimitives(),
                                  new DefaultPrimitives()};
        log.display("Verifying reachable objects.");
        status = checkStatus(status);
        testObjects = null;
        log.display("Verifying unreachable objects.");
        status = checkStatus(status);
        return status;
    }

}


class Constants {
    public static final boolean BOOLEAN = false;
    public static final char CHAR = 'z';
    public static final byte BYTE = 0xB;
    public static final short SHORT = 0xB00;
    public static final int INT = 0xDEADBEEF;
    public static final long LONG = 0xDEADBEEFDEADL;
    public static final float FLOAT = 3.1416f;
    public static final double DOUBLE = 3.14159265;
    public static final String STRING = "I hope you'll find me in the heap!";
}

/**
 * Class, that contains all possible primitive fields
 * except java.lang.String and arrays.
 */
class PublicPrimitives {
    public boolean booleanField = Constants.BOOLEAN;
    public char charField = Constants.CHAR;
    public byte byteField = Constants.BYTE;
    public short shortField = Constants.SHORT;
    public int intField = Constants.INT;
    public long longField = Constants.LONG;
    public float floatField = Constants.FLOAT;
    public double doubleField = Constants.DOUBLE;
    public String stringField = new String(Constants.STRING);
    public boolean[] booleanArray = new boolean[]{true,true,false,true,false};
    public char[] charArray = new char[]{Constants.CHAR,
                                         Constants.CHAR+1,
                                         Constants.CHAR+2,
                                         Constants.CHAR+3,
                                         Constants.CHAR+4};
    public byte[] byteArray = new byte[]{Constants.BYTE,
                                         Constants.BYTE+1,
                                         Constants.BYTE+2,
                                         Constants.BYTE+3,
                                         Constants.BYTE+4};
    public short[] shortArray = new short[]{Constants.SHORT,
                                            Constants.SHORT+1,
                                            Constants.SHORT+2,
                                            Constants.SHORT+3,
                                            Constants.SHORT+4};
    public int[] intArray = new int[]{Constants.INT,
                                      Constants.INT+1,
                                      Constants.INT+2,
                                      Constants.INT+3,
                                      Constants.INT+4};
    public long[] longArray = new long[]{Constants.LONG,
                                         Constants.LONG+1,
                                         Constants.LONG+2,
                                         Constants.LONG+3,
                                         Constants.LONG+4};
    public float[] floatArray = new float[]{Constants.FLOAT,
                                            Constants.FLOAT+1,
                                            Constants.FLOAT+2,
                                            Constants.FLOAT+3,
                                            Constants.FLOAT+4};
    public double[] doubleArray = new double[]{Constants.DOUBLE,
                                               Constants.DOUBLE+1,
                                               Constants.DOUBLE+2,
                                               Constants.DOUBLE+3,
                                               Constants.DOUBLE+4};
    public Object objectField = new Object();
    public Object[] objectArray = new Object[100];
    public int[][] multyArray = new int[10][10];

    public static boolean booleanStaticField = Constants.BOOLEAN;
    public static char charStaticField = Constants.CHAR;
    public static byte byteStaticField = Constants.BYTE;
    public static short shortStaticField = Constants.SHORT;
    public static int intStaticField = Constants.INT;
    public static long longStaticField = Constants.LONG;
    public static float floatStaticField = Constants.FLOAT;
    public static double doubleStaticField = Constants.DOUBLE;
    public static String stringStaticField = new String(Constants.STRING);
    public static boolean[] booleanStaticArray = new boolean[]{true,true,false,true,false};
    public static char[] charStaticArray = new char[]{Constants.CHAR,
                                                      Constants.CHAR+1,
                                                      Constants.CHAR+2,
                                                      Constants.CHAR+3,
                                                      Constants.CHAR+4};
    public static byte[] byteStaticArray = new byte[]{Constants.BYTE,
                                                      Constants.BYTE+1,
                                                      Constants.BYTE+2,
                                                      Constants.BYTE+3,
                                                      Constants.BYTE+4};
    public static short[] shortStaticArray = new short[]{Constants.SHORT,
                                                         Constants.SHORT+1,
                                                         Constants.SHORT+2,
                                                         Constants.SHORT+3,
                                                         Constants.SHORT+4};
    public static int[] intStaticArray = new int[]{Constants.INT,
                                                   Constants.INT+1,
                                                   Constants.INT+2,
                                                   Constants.INT+3,
                                                   Constants.INT+4};
    public static long[] longStaticArray = new long[]{Constants.LONG,
                                                      Constants.LONG+1,
                                                      Constants.LONG+2,
                                                      Constants.LONG+3,
                                                      Constants.LONG+4};
    public static float[] floatStaticArray = new float[]{Constants.FLOAT,
                                                         Constants.FLOAT+1,
                                                         Constants.FLOAT+2,
                                                         Constants.FLOAT+3,
                                                         Constants.FLOAT+4};
    public static double[] doubleStaticArray = new double[]{Constants.DOUBLE,
                                                            Constants.DOUBLE+1,
                                                            Constants.DOUBLE+2,
                                                            Constants.DOUBLE+3,
                                                            Constants.DOUBLE+4};
    public static Object objectStaticField = new Object();
    public static Object[] objectStaticArray = new Object[100];
    public static int[][] multyStaticArray = new int[10][10];

}

class ProtectedPrimitives {
    protected boolean booleanField = Constants.BOOLEAN;
    protected char charField = Constants.CHAR;
    protected byte byteField = Constants.BYTE;
    protected short shortField = Constants.SHORT;
    protected int intField = Constants.INT;
    protected long longField = Constants.LONG;
    protected float floatField = Constants.FLOAT;
    protected double doubleField = Constants.DOUBLE;
    protected String stringField = new String(Constants.STRING);
    protected boolean[] booleanArray = new boolean[]{true,true,false,true,false};
    protected char[] charArray = new char[]{Constants.CHAR,
                                         Constants.CHAR+1,
                                         Constants.CHAR+2,
                                         Constants.CHAR+3,
                                         Constants.CHAR+4};
    protected byte[] byteArray = new byte[]{Constants.BYTE,
                                         Constants.BYTE+1,
                                         Constants.BYTE+2,
                                         Constants.BYTE+3,
                                         Constants.BYTE+4};
    protected short[] shortArray = new short[]{Constants.SHORT,
                                            Constants.SHORT+1,
                                            Constants.SHORT+2,
                                            Constants.SHORT+3,
                                            Constants.SHORT+4};
    protected int[] intArray = new int[]{Constants.INT,
                                      Constants.INT+1,
                                      Constants.INT+2,
                                      Constants.INT+3,
                                      Constants.INT+4};
    protected long[] longArray = new long[]{Constants.LONG,
                                         Constants.LONG+1,
                                         Constants.LONG+2,
                                         Constants.LONG+3,
                                         Constants.LONG+4};
    protected float[] floatArray = new float[]{Constants.FLOAT,
                                            Constants.FLOAT+1,
                                            Constants.FLOAT+2,
                                            Constants.FLOAT+3,
                                            Constants.FLOAT+4};
    protected double[] doubleArray = new double[]{Constants.DOUBLE,
                                               Constants.DOUBLE+1,
                                               Constants.DOUBLE+2,
                                               Constants.DOUBLE+3,
                                               Constants.DOUBLE+4};
    protected Object objectField = new Object();
    protected Object[] objectArray = new Object[100];
    protected int[][] multyArray = new int[10][10];

    protected static boolean booleanStaticField = Constants.BOOLEAN;
    protected static char charStaticField = Constants.CHAR;
    protected static byte byteStaticField = Constants.BYTE;
    protected static short shortStaticField = Constants.SHORT;
    protected static int intStaticField = Constants.INT;
    protected static long longStaticField = Constants.LONG;
    protected static float floatStaticField = Constants.FLOAT;
    protected static double doubleStaticField = Constants.DOUBLE;
    protected static String stringStaticField = new String(Constants.STRING);
    protected static boolean[] booleanStaticArray = new boolean[]{true,true,false,true,false};
    protected static char[] charStaticArray = new char[]{Constants.CHAR,
                                                      Constants.CHAR+1,
                                                      Constants.CHAR+2,
                                                      Constants.CHAR+3,
                                                      Constants.CHAR+4};
    protected static byte[] byteStaticArray = new byte[]{Constants.BYTE,
                                                      Constants.BYTE+1,
                                                      Constants.BYTE+2,
                                                      Constants.BYTE+3,
                                                      Constants.BYTE+4};
    protected static short[] shortStaticArray = new short[]{Constants.SHORT,
                                                         Constants.SHORT+1,
                                                         Constants.SHORT+2,
                                                         Constants.SHORT+3,
                                                         Constants.SHORT+4};
    protected static int[] intStaticArray = new int[]{Constants.INT,
                                                   Constants.INT+1,
                                                   Constants.INT+2,
                                                   Constants.INT+3,
                                                   Constants.INT+4};
    protected static long[] longStaticArray = new long[]{Constants.LONG,
                                                      Constants.LONG+1,
                                                      Constants.LONG+2,
                                                      Constants.LONG+3,
                                                      Constants.LONG+4};
    protected static float[] floatStaticArray = new float[]{Constants.FLOAT,
                                                         Constants.FLOAT+1,
                                                         Constants.FLOAT+2,
                                                         Constants.FLOAT+3,
                                                         Constants.FLOAT+4};
    protected static double[] doubleStaticArray = new double[]{Constants.DOUBLE,
                                                            Constants.DOUBLE+1,
                                                            Constants.DOUBLE+2,
                                                            Constants.DOUBLE+3,
                                                            Constants.DOUBLE+4};
    protected static Object objectStaticField = new Object();
    protected static Object[] objectStaticArray = new Object[100];
    protected static int[][] multyStaticArray = new int[10][10];
}

class PrivatePrimitives {
    private boolean booleanField = Constants.BOOLEAN;
    private char charField = Constants.CHAR;
    private byte byteField = Constants.BYTE;
    private short shortField = Constants.SHORT;
    private int intField = Constants.INT;
    private long longField = Constants.LONG;
    private float floatField = Constants.FLOAT;
    private double doubleField = Constants.DOUBLE;
    private String stringField = new String(Constants.STRING);
    private boolean[] booleanArray = new boolean[]{true,true,false,true,false};
    private char[] charArray = new char[]{Constants.CHAR,
                                         Constants.CHAR+1,
                                         Constants.CHAR+2,
                                         Constants.CHAR+3,
                                         Constants.CHAR+4};
    private byte[] byteArray = new byte[]{Constants.BYTE,
                                         Constants.BYTE+1,
                                         Constants.BYTE+2,
                                         Constants.BYTE+3,
                                         Constants.BYTE+4};
    private short[] shortArray = new short[]{Constants.SHORT,
                                            Constants.SHORT+1,
                                            Constants.SHORT+2,
                                            Constants.SHORT+3,
                                            Constants.SHORT+4};
    private int[] intArray = new int[]{Constants.INT,
                                      Constants.INT+1,
                                      Constants.INT+2,
                                      Constants.INT+3,
                                      Constants.INT+4};
    private long[] longArray = new long[]{Constants.LONG,
                                         Constants.LONG+1,
                                         Constants.LONG+2,
                                         Constants.LONG+3,
                                         Constants.LONG+4};
    private float[] floatArray = new float[]{Constants.FLOAT,
                                            Constants.FLOAT+1,
                                            Constants.FLOAT+2,
                                            Constants.FLOAT+3,
                                            Constants.FLOAT+4};
    private double[] doubleArray = new double[]{Constants.DOUBLE,
                                               Constants.DOUBLE+1,
                                               Constants.DOUBLE+2,
                                               Constants.DOUBLE+3,
                                               Constants.DOUBLE+4};
    private Object objectField = new Object();
    private Object[] objectArray = new Object[100];
    private int[][] multyArray = new int[10][10];

    private static boolean booleanStaticField = Constants.BOOLEAN;
    private static char charStaticField = Constants.CHAR;
    private static byte byteStaticField = Constants.BYTE;
    private static short shortStaticField = Constants.SHORT;
    private static int intStaticField = Constants.INT;
    private static long longStaticField = Constants.LONG;
    private static float floatStaticField = Constants.FLOAT;
    private static double doubleStaticField = Constants.DOUBLE;
    private static String stringStaticField = new String(Constants.STRING);
    private static boolean[] booleanStaticArray = new boolean[]{true,true,false,true,false};
    private static char[] charStaticArray = new char[]{Constants.CHAR,
                                                      Constants.CHAR+1,
                                                      Constants.CHAR+2,
                                                      Constants.CHAR+3,
                                                      Constants.CHAR+4};
    private static byte[] byteStaticArray = new byte[]{Constants.BYTE,
                                                      Constants.BYTE+1,
                                                      Constants.BYTE+2,
                                                      Constants.BYTE+3,
                                                      Constants.BYTE+4};
    private static short[] shortStaticArray = new short[]{Constants.SHORT,
                                                         Constants.SHORT+1,
                                                         Constants.SHORT+2,
                                                         Constants.SHORT+3,
                                                         Constants.SHORT+4};
    private static int[] intStaticArray = new int[]{Constants.INT,
                                                   Constants.INT+1,
                                                   Constants.INT+2,
                                                   Constants.INT+3,
                                                   Constants.INT+4};
    private static long[] longStaticArray = new long[]{Constants.LONG,
                                                      Constants.LONG+1,
                                                      Constants.LONG+2,
                                                      Constants.LONG+3,
                                                      Constants.LONG+4};
    private static float[] floatStaticArray = new float[]{Constants.FLOAT,
                                                         Constants.FLOAT+1,
                                                         Constants.FLOAT+2,
                                                         Constants.FLOAT+3,
                                                         Constants.FLOAT+4};
    private static double[] doubleStaticArray = new double[]{Constants.DOUBLE,
                                                            Constants.DOUBLE+1,
                                                            Constants.DOUBLE+2,
                                                            Constants.DOUBLE+3,
                                                            Constants.DOUBLE+4};
    private static Object objectStaticField = new Object();
    private static Object[] objectStaticArray = new Object[100];
    private static int[][] multyStaticArray = new int[10][10];
}

class DefaultPrimitives {
    boolean booleanField = Constants.BOOLEAN;
    char charField = Constants.CHAR;
    byte byteField = Constants.BYTE;
    short shortField = Constants.SHORT;
    int intField = Constants.INT;
    long longField = Constants.LONG;
    float floatField = Constants.FLOAT;
    double doubleField = Constants.DOUBLE;
    String stringField = new String(Constants.STRING);
    boolean[] booleanArray = new boolean[]{true,true,false,true,false};
    char[] charArray = new char[]{Constants.CHAR,
                                         Constants.CHAR+1,
                                         Constants.CHAR+2,
                                         Constants.CHAR+3,
                                         Constants.CHAR+4};
    byte[] byteArray = new byte[]{Constants.BYTE,
                                         Constants.BYTE+1,
                                         Constants.BYTE+2,
                                         Constants.BYTE+3,
                                         Constants.BYTE+4};
    short[] shortArray = new short[]{Constants.SHORT,
                                            Constants.SHORT+1,
                                            Constants.SHORT+2,
                                            Constants.SHORT+3,
                                            Constants.SHORT+4};
    int[] intArray = new int[]{Constants.INT,
                                      Constants.INT+1,
                                      Constants.INT+2,
                                      Constants.INT+3,
                                      Constants.INT+4};
    long[] longArray = new long[]{Constants.LONG,
                                         Constants.LONG+1,
                                         Constants.LONG+2,
                                         Constants.LONG+3,
                                         Constants.LONG+4};
    float[] floatArray = new float[]{Constants.FLOAT,
                                            Constants.FLOAT+1,
                                            Constants.FLOAT+2,
                                            Constants.FLOAT+3,
                                            Constants.FLOAT+4};
    double[] doubleArray = new double[]{Constants.DOUBLE,
                                               Constants.DOUBLE+1,
                                               Constants.DOUBLE+2,
                                               Constants.DOUBLE+3,
                                               Constants.DOUBLE+4};
    Object objectField = new Object();
    Object[] objectArray = new Object[100];
    int[][] multyArray = new int[10][10];

    static boolean booleanStaticField = Constants.BOOLEAN;
    static char charStaticField = Constants.CHAR;
    static byte byteStaticField = Constants.BYTE;
    static short shortStaticField = Constants.SHORT;
    static int intStaticField = Constants.INT;
    static long longStaticField = Constants.LONG;
    static float floatStaticField = Constants.FLOAT;
    static double doubleStaticField = Constants.DOUBLE;
    static String stringStaticField = new String(Constants.STRING);
    static boolean[] booleanStaticArray = new boolean[]{true,true,false,true,false};
    static char[] charStaticArray = new char[]{Constants.CHAR,
                                                      Constants.CHAR+1,
                                                      Constants.CHAR+2,
                                                      Constants.CHAR+3,
                                                      Constants.CHAR+4};
    static byte[] byteStaticArray = new byte[]{Constants.BYTE,
                                                      Constants.BYTE+1,
                                                      Constants.BYTE+2,
                                                      Constants.BYTE+3,
                                                      Constants.BYTE+4};
    static short[] shortStaticArray = new short[]{Constants.SHORT,
                                                         Constants.SHORT+1,
                                                         Constants.SHORT+2,
                                                         Constants.SHORT+3,
                                                         Constants.SHORT+4};
    static int[] intStaticArray = new int[]{Constants.INT,
                                                   Constants.INT+1,
                                                   Constants.INT+2,
                                                   Constants.INT+3,
                                                   Constants.INT+4};
    static long[] longStaticArray = new long[]{Constants.LONG,
                                                      Constants.LONG+1,
                                                      Constants.LONG+2,
                                                      Constants.LONG+3,
                                                      Constants.LONG+4};
    static float[] floatStaticArray = new float[]{Constants.FLOAT,
                                                         Constants.FLOAT+1,
                                                         Constants.FLOAT+2,
                                                         Constants.FLOAT+3,
                                                         Constants.FLOAT+4};
    static double[] doubleStaticArray = new double[]{Constants.DOUBLE,
                                                            Constants.DOUBLE+1,
                                                            Constants.DOUBLE+2,
                                                            Constants.DOUBLE+3,
                                                            Constants.DOUBLE+4};
    static Object objectStaticField = new Object();
    static Object[] objectStaticArray = new Object[100];
    static int[][] multyStaticArray = new int[10][10];
}
