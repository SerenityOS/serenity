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

package nsk.jvmti.NativeMethodBind;

import java.io.*;

import nsk.share.*;
import nsk.share.jvmti.*;

/**
 * This test exercises the JVMTI event <code>NativeMethodBind</code>.
 * <br>It verifies that binding native method can be redirected during
 * the event callback.<p>
 * The test works as follows. An agent part enables the NativeMethodBind
 * generation. Then the java part invokes the native method
 * <code>nativeMethod()</code> which leads to the NativeMethodBind generation.
 * In NativeMethodBind callback incoming address of the nativeMethod() is
 * changed to the address of another native method
 * <code>redirNativeMethod()</code>.
 * Both functions nativeMethod() and redirNativeMethod() count their calls.<br>
 * In accordance with the spec, the nativeMethod() should not be invoked
 * and the redirNativeMethod() should be invoked once.
 */
public class nativemethbind004 {
    static {
        try {
            System.loadLibrary("nativemethbind004");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load \"nativemethbind004\" library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native void nativeMethod();
    native int check();

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // produce JCK-like exit status
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new nativemethbind004().runThis(argv, out);
    }

    private int runThis(String argv[], PrintStream out) {
        // invoke native method to be redirected
        nativeMethod();

        return check();
    }
}
