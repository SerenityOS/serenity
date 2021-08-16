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

/* @test
 * @bug 8149170
 * @summary Test native functions return booleans as 0/1 but differently than java functions
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @compile BoolConstructor.java
 * @run main/native NativeSmallIntCallsTest
 */

// This test shows that returns from native calls for boolean truncate to JNI_TRUE if value != 0
// and JNI_FALSE if value returned is 0.

import jdk.test.lib.Asserts;

public class NativeSmallIntCallsTest {
    static native boolean nativeCastToBoolCallTrue();
    static native boolean nativeCastToBoolCallFalse();
    static native boolean nativeCallBoolConstructor(int visible, boolean expected);
    static {
        System.loadLibrary("NativeSmallIntCalls");
    }

    public static void main(java.lang.String[] unused) throws Exception {
        // Call through jni
        // JNI makes all results != 0 true for compatibility
        boolean nativeTrueVal = nativeCastToBoolCallTrue();
        Asserts.assertTrue(nativeTrueVal, "trueval is false!");

        boolean nativeFalseVal = nativeCastToBoolCallFalse();
        Asserts.assertTrue(nativeFalseVal, "falseval is false in native!");

        // Call a constructor or method that passes jboolean values into Java from native
        nativeCallBoolConstructor(1, true);
        nativeCallBoolConstructor(0x10, true);
        nativeCallBoolConstructor(0x100, false);
        nativeCallBoolConstructor(0x1000, false);
    }
}
