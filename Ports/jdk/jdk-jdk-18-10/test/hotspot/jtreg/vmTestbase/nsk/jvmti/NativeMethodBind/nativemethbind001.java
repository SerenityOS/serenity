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
 * <br>It verifies that the event will be properly sent:
 * <li>for the native method called for the first time
 * <li>when the JNI RegisterNatives() is called.<p>
 * The test works as follows. The java part invokes the native method
 * <code>nativeMethod()</code> twice. At the first time that method
 * registers another native method <code>anotherNativeMethod()</code> for
 * the dummy class <code>TestedClass</code>. Registration is made through
 * the JNI RegisterNatives() call. Being invoked at the second time, the
 * nativeMethod() just returns.<br>
 * In accordance with the spec, it is expected that the NativeMethodBind
 * will be generated only one time for the nativeMethod(), and only one
 * time for the anotherNativeMethod().
 */
public class nativemethbind001 {
    static {
        try {
            System.loadLibrary("nativemethbind001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load \"nativemethbind001\" library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native void nativeMethod(boolean registerNative);

    native int check();

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // produce JCK-like exit status
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new nativemethbind001().runThis(argv, out);
    }

    private int runThis(String argv[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(out, argHandler);

        log.display("\nCalling native methods ...\n");

        // dummy method used to provoke the NativeMethodBind event
        nativeMethod(true);

        // call one more time to provoke the wrong NativeMethodBind
        // event
        nativeMethod(false);

        return check();
    }

   /**
    * Dummy class used only to register native method
    * <code>anotherNativeMethod</code> with it
    */
    class TestedClass {
        native void anotherNativeMethod();
    }
}
