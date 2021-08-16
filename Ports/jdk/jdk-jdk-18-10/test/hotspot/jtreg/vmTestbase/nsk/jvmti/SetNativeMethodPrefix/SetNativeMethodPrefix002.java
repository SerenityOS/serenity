/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jvmti.SetNativeMethodPrefix;

import java.io.*;
import nsk.share.Consts;
import java.lang.reflect.*;


public class SetNativeMethodPrefix002 {
    static {
        System.loadLibrary("SetNativeMethodPrefix002Main");
    }

    PrintStream out = System.out;

    // Should be automatically bound to the native method foo() from SetNativeMethodPrefix002 library
    native public int wc_wb_wa_foo();
    public int foo() { return wc_wb_wa_foo(); }

    // Should be automatically bound to the native method foo1() from SetNativeMethodPrefix002Main library
    native public int wc_wb_wa_foo1();
    public int foo1() { return wc_wb_wa_foo1(); }

    // Should be manually bound to the native method wrapped_foo() from SetNativeMethodPrefix001 library
    native public int wc_wb_wa_foo2();
    public int foo2() { return wc_wb_wa_foo2(); }

    /* ============================================================ */
    public boolean runIt(String [] args, PrintStream _out) {
        if (_out != null) {
            out = _out;
        }

        try {
            // Check automatic resolution
            foo();
            foo1();

            // Check explicit binding

            // wc_wb_wa_foo2 shouldn't be bound at this moment
            try {
                foo2();
                out.println("ERROR: wc_wb_wa_foo2 is bound.");
                return false;
            } catch (UnsatisfiedLinkError e) {
                // wc_wb_wa_foo2 hasn't been bound so far.
            }

            if (Binder.registerMethod(SetNativeMethodPrefix002.class, "wc_wb_wa_foo2", "()I", Binder.FUNCTION_FOO)) {
                foo2();
            } else {
                out.println("ERROR: Binding process failed for a method wc_wb_wa_foo2");
                return false;
            }
        } catch (UnsatisfiedLinkError e) {
            out.println(e.getClass().getName()+": "+e.getMessage());
            return false;
        }

        return true;
    }

    /* ============================================================ */
    public static int run(String argv[], PrintStream out) {
        if ((new SetNativeMethodPrefix002()).runIt(argv, out)) {
            out.println("TEST PASSED");
            return Consts.TEST_PASSED;
        } else {
            out.println("TEST FAILED");
            return Consts.TEST_FAILED;
        }
    }

    /* */
    /* ============================================================ */
    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }
}
