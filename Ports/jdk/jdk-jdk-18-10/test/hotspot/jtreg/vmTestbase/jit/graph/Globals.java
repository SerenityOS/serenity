/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jit.graph;

import jdk.test.lib.Utils;
import nsk.share.TestFailure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;

public final class Globals {


    public static int STATIC_LOOP = 0;
    public static int NUM_TEST_CLASSES = 7;
    public static long RANDOM_LOOP = 100;
    public static boolean VERBOSE = false;

    private static final Random indexGenerator = Utils.getRandomInstance();
    private static String[] ClassArray = null;
    private static Class[] ClassInstanceArray = null;
    private static int maxClassIndex = 0;

    private static String[] MethodName_Array = null;
    private static Method[] MethodInstance_Array = null;

    // Should be prime, so that odds of an incorrect verification reduced
    public static int[] MethodID_Array = null;

    public static synchronized void initialize(String testListPath) {
        File td = new File(testListPath);
        if (!td.exists()) {
            throw new Error("TESTBUG: File " + testListPath + " Not found");
        }

        if (!td.isFile()) {
            throw new Error("TESTBUG: " + testListPath + " Must be a File");
        }

        BufferedReader classList = null;
        try {
            try {
                classList = new BufferedReader(new FileReader(td));
            } catch (FileNotFoundException e) {
                throw new Error("TESTBUG: Error finding Classlist", e);
            }

            String line = null;
            try {
                line = classList.readLine();
            } catch (IOException e) {
                throw new Error("TESTBUG: Error reading Classlist", e);
            }

            try {
                // ClassArray.length;
                maxClassIndex = Math.abs(Integer.parseInt(line));
            } catch (NumberFormatException e) {
                throw new Error("TESTBUG: Error reading Classlist - first number must be number of methods defined", e);
            }

            ClassArray = new String[maxClassIndex];
            ClassInstanceArray = new Class[maxClassIndex];
            MethodName_Array = new String[maxClassIndex];
            MethodInstance_Array = new Method[maxClassIndex];
            MethodID_Array = new int[maxClassIndex];

            int i;
            for (i = 0; i < maxClassIndex; i++) {
                try {
                    line = classList.readLine();
                } catch (IOException e) {
                    throw new Error("TESTBUG: Error reading ClasslistFile: testListPath", e);
                }
                StringTokenizer lineTokens = new StringTokenizer(line, "\t ");
                if (lineTokens.countTokens() < 3) {
                    throw new Error("TESTBUG: ClasslistFile: unexpected line:" + line);
                } else {
                    ClassArray[i] = lineTokens.nextToken();
                    MethodName_Array[i] = lineTokens.nextToken();
                    MethodID_Array[i] = Integer.parseInt(lineTokens.nextToken());
                }
            }
            maxClassIndex = i;
        } finally {
            if (classList != null) {
                try {
                    classList.close();
                } catch (IOException e) {
                    throw new Error("can't close file", e);
                }
            }
        }

        if ((NUM_TEST_CLASSES < ClassArray.length) && (NUM_TEST_CLASSES > 0)) {
            maxClassIndex = NUM_TEST_CLASSES;
        } else {
            NUM_TEST_CLASSES = maxClassIndex;
        }
    }

    // does a binary search to find the index for the ID of a method
    private static int ID_BinSearch(int begin, int end, int ID) {
        if (end < begin) {
            return (-1);
        }

        int mid = (begin + end) / 2;
        int midvalue = MethodID_Array[mid];

        if (ID == midvalue) {
            return (mid);
        } else if (ID < midvalue) {
            return (ID_BinSearch(begin, mid - 1, ID));
        } else {
            return (ID_BinSearch(mid + 1, end, ID));
        }
    }


    // based off a static index, this function selects the method to be called
    public static MethodData returnNextStaticMethod(int Method_ID) {
        //int i = ID_BinSearch(0, MethodID_Array.length - 1, Method_ID);
        int i = ID_BinSearch(0, maxClassIndex - 1, Method_ID);

        return (nextStaticMethod((i == -1) ? 0 : i));
    }

    // this function randomly selects the next method to be called by the test class
    public static MethodData nextRandomMethod() {
        int i = indexGenerator.nextInt(maxClassIndex);
        return (nextStaticMethod(i));
    }

    private static MethodData nextStaticMethod(int i) {
        Class methodsClass = null;
        Method nextMethod = null;

        try {
            methodsClass = ClassInstanceArray[i];
            if (methodsClass == null) {
                methodsClass = Class.forName(ClassArray[i]);
                ClassInstanceArray[i] = methodsClass;
            }
            nextMethod = MethodInstance_Array[i];
            if (nextMethod == null) {
                nextMethod = methodsClass.getMethod(MethodName_Array[i],
                        Vector.class, Vector.class,  Long.class, Integer.class);
                // sum vector, ID vector, function depth, static function call depth
                MethodInstance_Array[i] = nextMethod;
            }
        } catch (ClassNotFoundException e) {
            throw new Error("TESTBUG Class: " + ClassArray[i] + " Not Found", e);
        } catch (NoSuchMethodException e) {
            throw new Error("TESTBUG Method: " + ClassArray[i] + "::" + MethodName_Array[i] + " Not Found", e);
        } catch (SecurityException e) {
            throw new Error("TESTBUG Security Exception Generated by " + ClassArray[i] + "::" + MethodName_Array[i], e);
        }
        return new MethodData(ClassArray[i], MethodName_Array[i], methodsClass, nextMethod, MethodID_Array[i]);
    }


    /* These two functions are used to verify that all function were called in the proper order */

    // called by "parent" function to add childs ID to vector
    public static void addFunctionIDToVector(int FunctionIndex, Vector IDVector) {
        IDVector.addElement(FunctionIndex);
    }

    // called by "child" to add Function Index to Vector
    public static void appendSumToSummationVector(int FunctionIndex, Vector SummationVector) {
        if (SummationVector.isEmpty()) {
            SummationVector.addElement((long) FunctionIndex);
        } else {
            SummationVector.addElement((Long) SummationVector.lastElement() + FunctionIndex);
        }
    }

    // This function calls a method based off of MethodData
    public static void callMethod(MethodData methodCallStr,
                                  Vector summation, Vector ID,
                                  Long numFcalls, Integer staticFcalls)
            throws InvocationTargetException {
        try {
            methodCallStr.nextMethod.invoke(methodCallStr.instance,
                    summation, ID, numFcalls, staticFcalls);
        } catch (IllegalAccessException e) {
            // should never happen with a valid testfile
            throw new TestFailure("Illegal Access Exception", e);
        }
    }
}
