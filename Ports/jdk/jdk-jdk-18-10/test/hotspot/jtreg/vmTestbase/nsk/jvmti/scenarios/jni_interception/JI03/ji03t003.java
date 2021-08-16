/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.scenarios.jni_interception.JI03;

import java.io.*;
import java.util.*;
import nsk.share.*;

/**
 * The test exercises the JVMTI target area "JNI Function Interception".
 * It implements the following scenario:<br>
 * <code>Check that functions in JNI function table can be redirected. Then
 * restore original function and verify that original function works.</code><br>
 *
 * The tested JVMTI functions Set/GetJNIFunctionTable are verified with
 * the JNI functions Throw, ThrowNew, ExceptionOccurred.
 */
public class ji03t003 {
    ji03t003Exc exc = new ji03t003Exc("Test exception");

    static {
        try {
            System.loadLibrary("ji03t003");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load \"ji03t003\" library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native int check();

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        return new ji03t003().runIt(args, out);
    }

    private int runIt(String args[], PrintStream out) {
        return check();
    }
}

/**
 * Tested Throwable class
 */
class ji03t003Exc extends RuntimeException {
    ji03t003Exc(String message) {
        super(message);
    }
}
