/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8155608
 * @summary Verifies that string intrinsics throw array out of bounds exceptions.
 * @library /compiler/patches /test/lib
 * @build java.base/java.lang.Helper
 * @run main/othervm -Xbatch -XX:CompileThreshold=100 compiler.intrinsics.string.TestStringIntrinsicRangeChecks
 */
package compiler.intrinsics.string;

import java.lang.Helper;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TestStringIntrinsicRangeChecks {
    // Prepare test arrays
    private static int SIZE = 16;
    private static byte[] byteArray = new byte[SIZE];
    private static char[] charArray = new char[SIZE];

    public static void check(Method m, boolean shouldThrow, Object... args) throws Exception {
        // Prepare error message
        String message = m.getName() + "(";
        for (int i = 0; i < args.length; ++i) {
            message += args[i];
            message += (i+1 < args.length) ? ", " : ")";
        }

        try {
            m.invoke(null, args);
        } catch (InvocationTargetException e) {
            // Get actual exception
            Throwable t = e.getTargetException();
            if (!shouldThrow) {
                throw new RuntimeException("Unexpected exception thrown for " + message, e);
            }
            if (t instanceof StringIndexOutOfBoundsException ||
                t instanceof ArrayIndexOutOfBoundsException) {
                // Expected exception. Make sure that the exception was not thrown in UTF16.putChar/getChar
                // because the corresponding intrinsics are unchecked and the Java code should do all the checks.
                StackTraceElement[] stack = t.getStackTrace();
                if (stack.length != 0) {
                    String methodName = stack[0].getMethodName();
                    if (methodName.equals("putChar") || methodName.equals("getChar")) {
                        throw new RuntimeException("Exception thrown in " + methodName + " for " + message, t);
                    }
                }
            }
            return;
        }
        if (shouldThrow) {
            throw new RuntimeException("No exception thrown for " + message);
        }
    }

    public static void main(String[] args) throws Exception {
        // Get intrinsified String API methods
        Method compressByte = Helper.class.getMethod("compressByte", byte[].class, int.class, int.class, int.class, int.class);
        Method compressChar = Helper.class.getMethod("compressChar", char[].class, int.class, int.class, int.class, int.class);
        Method inflateByte  = Helper.class.getMethod("inflateByte",  byte[].class, int.class, int.class, int.class, int.class);
        Method inflateChar  = Helper.class.getMethod("inflateChar",  byte[].class, int.class, int.class, int.class, int.class);
        Method toBytes      = Helper.class.getMethod("toBytes",      char[].class, int.class, int.class);
        Method getChars     = Helper.class.getMethod("getChars",     byte[].class, int.class, int.class, int.class, int.class);

        // Check different combinations of arguments (source/destination offset and length)
        for (int srcOff = 0; srcOff < SIZE; ++srcOff) {
            for (int dstOff = 0; dstOff < SIZE; ++dstOff) {
                for (int len = 0; len < SIZE; ++len) {
                    // Check for potential overlows in source or destination array
                    boolean srcOverflow  = (srcOff + len) > SIZE;
                    boolean srcOverflowB = (2*srcOff + 2*len) > SIZE;
                    boolean dstOverflow  = (dstOff + len) > SIZE;
                    boolean dstOverflowB = (2*dstOff + 2*len) > SIZE;
                    boolean getCharsOver = (srcOff < len) && ((2*(len-1) >= SIZE) || ((dstOff + len - srcOff) > SIZE));
                    // Check if an exception is thrown and bail out if result is inconsistent with above
                    // assumptions (for example, an exception was not thrown although an overflow happened).
                    check(compressByte, srcOverflowB || dstOverflow,  byteArray, srcOff, SIZE, dstOff, len);
                    check(compressChar, srcOverflow  || dstOverflow,  charArray, srcOff, SIZE, dstOff, len);
                    check(inflateByte,  srcOverflow  || dstOverflowB, byteArray, srcOff, SIZE, dstOff, len);
                    check(inflateChar,  srcOverflow  || dstOverflow,  byteArray, srcOff, SIZE, dstOff, len);
                    check(toBytes,      srcOverflow,                  charArray, srcOff, len);
                    check(getChars,     getCharsOver,                 byteArray, srcOff, len, SIZE, dstOff);
                }
            }
        }
    }
}
