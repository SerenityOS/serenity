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

/* ============================================================================== */
/* Quote from JVMTI specification v.1.1.102:
 *
 *     Since native methods cannot be directly instrumented (they have no
 *     bytecodes), they must be wrapped with a non-native method which can be
 *     instrumented. For example, if we had:
 *             native boolean foo(int x);
 *
 *     We could transform the class file (with the ClassFileLoadHook event) so
 *     that this becomes:
 *         boolean foo(int x) {
 *             ... record entry to foo ...
 *             return wrapped_foo(x);
 *         }
 *
 *         native boolean wrapped_foo(int x);
 *
 *     Where foo becomes a wrapper for the actual native method with the appended
 *     prefix "wrapped_".
 *
 *     The wrapper will allow data to be collected on the native method call, but
 *     now the problem becomes linking up the wrapped method with the native
 *     implementation. That is, the method wrapped_foo needs to be resolved to the
 *     native implementation of foo, which might be:
 *
 *         Java_somePackage_someClass_foo(JNIEnv* env, jint x)
 *
 *     This function allows the prefix to be specified and the proper resolution
 *     to occur. Specifically, when the standard resolution fails, the resolution
 *     is retried taking the prefix into consideration. There are two ways that
 *     resolution occurs, explicit resolution with the JNI function
 *     RegisterNatives and the normal automatic resolution. For RegisterNatives,
 *     the VM will attempt this association:
 *
 *         method(foo) -> nativeImplementation(foo)
 *
 *     When this fails, the resolution will be retried with the specified prefix
 *     prepended to the method name, yielding the correct resolution:
 *
 *         method(wrapped_foo) -> nativeImplementation(foo)
 *
 *     For automatic resolution, the VM will attempt:
 *
 *         method(wrapped_foo) -> nativeImplementation(wrapped_foo)
 *
 *     When this fails, the resolution will be retried with the specified prefix
 *     deleted from the implementation name, yielding the correct resolution:
 *
 *         method(wrapped_foo) -> nativeImplementation(foo)
 *
 *     Note that since the prefix is only used when standard resolution fails,
 *     native methods can be wrapped selectively.
 */
/* ============================================================================== */


/* ======================================================= */
// Auxiliary classes for explicit method resolution
/* ======================================================= */
class ExplicitResolution1 {
    static public int foo() { return wrapped_foo(); }
    native static public int wrapped_foo();
}

/* ======================================================= */
class ExplicitResolution2 {
    native static public int wrapped_foo();
}

/* ======================================================= */
// Auxiliary classes for automatic resolution
/* ======================================================= */
class AutomaticResolution1 {
    static public int foo() { return wrapped_foo(); }
    native static public int wrapped_foo();
}

/* ======================================================= */
class AutomaticResolution2 {
    static public int foo() { return wrapped_foo(); }
    native static public int wrapped_foo();
}

/* ======================================================= */
class AutomaticResolution3 {
    static public int foo() { return wrapped_foo(); }
    native static public int wrapped_foo();
}

/* ======================================================= */
public class SetNativeMethodPrefix001 {
    PrintStream out = System.out;

    static public final String prefix = "wrapped_";

    /* ============================================================ */
    //
    //  For RegisterNatives, the VM will attempt this association:
    //
    //      method(foo) -> nativeImplementation(foo)
    //
    //  When this fails, the resolution will be retried with the specified
    //  prefix prepended to the method name, yielding the correct resolution:
    //
    //      method(wrapped_foo) -> nativeImplementation(foo)
    //
    //  The prefix is only used when standard resolution fails.
    //
    /* ============================================================ */
    public boolean checkExplicitResolution1(boolean isPrefixSet) {

        // Initiate class A loading and initialization
        // See CR 6493522 for details
        new ExplicitResolution1();

        // Bind ExplicitResolution1.foo() to a native function.
        // If the prefix isn't set, this code should fail since ExplicitResolution1.foo() isn't native.
        if (Binder.registerMethod(ExplicitResolution1.class, "foo", "()I", Binder.FUNCTION_FOO) != isPrefixSet)
        {
            out.println("ERROR: unexpected RegisterNatives() behavior.");
            return false;
        }

        if (isPrefixSet) {
            if (ExplicitResolution1.foo() != Binder.FOO_RETURN) {
                out.println("ERROR: native method wrapped_foo() wasn't correctly bound.");
                return false;
            }
        }

        // Bind ExplicitResolution1.wrapped_foo() to a native fucntion.
        // This code should succeed since ExplicitResolution1.wrapped_foo() is native.
        if (!Binder.registerMethod(ExplicitResolution1.class, "wrapped_foo", "()I", Binder.FUNCTION_WRAPPED_FOO))
        {
            out.println("ERROR: RegisterNative() failed for native method ExplicitResolution1.wrapped_foo().");
            return false;
        }

        if (ExplicitResolution1.foo() != Binder.WRAPPED_FOO_RETURN) {
            out.println("ERROR: native method wrapped_foo() wasn't correctly bound.");
            return false;
        }
        return true;
    }

    /* ============================================================ */
    public boolean checkExplicitResolution2() {
        // Initiate class ExplicitResolution2 loading and initialization
        // See CR 6493522 for details
        new ExplicitResolution2();

        // This binding should fail whether the prefix is set or not since:
        //    - there's no Java_nsk_jvmti_SetNativeMethodPrefix_ExplicitResolution2_wrapped_1foo defined
        //    - class ExplicitResolution2 doesn't have java method foo()
        if (Binder.registerMethod(ExplicitResolution2.class, "foo", "()I", Binder.FUNCTION_WRAPPED_FOO)) {
            out.println("ERROR: unexpected RegisterNatives() behavior.");
            return false;
        }

        return true;
    }

    /* ============================================================ */
    public boolean checkExplicitResolution (boolean isMultiplePrefixes) {
        // Setting method prefix
        out.println("\tSetting prefix: "+prefix);
        if (isMultiplePrefixes) {
            if (!Binder.setMultiplePrefixes(prefix)) { return false; }
        } else {
            if (!Binder.setMethodPrefix(prefix)) { return false; }
        }

        // Check the behavior
        out.println("\t\tChecking resolution for ExplicitResolution1");
        if (!checkExplicitResolution1(true)) { return false; }
        out.println("\t\tChecking resolution for ExplicitResolution2");
        if (!checkExplicitResolution2()) { return false; }

        // Resetting method prefix
        out.println("\tResetting prefix");
        if (isMultiplePrefixes) {
            if (!Binder.setMultiplePrefixes(null)) { return false; }
        } else {
            if (!Binder.setMethodPrefix(null)) { return false; }
        }

        // Check the behavior
        out.println("\t\tChecking resolution for ExplicitResolution1");
        if (!checkExplicitResolution1(false)) { return false; }
        out.println("\t\tChecking resolution for ExplicitResolution2");
        if (!checkExplicitResolution2()) { return false; }

        return true;
    }

    /* ============================================================ */
    //  For automatic resolution, the VM will attempt:
    //
    //      method(wrapped_foo) -> nativeImplementation(wrapped_foo)
    //
    //  When this fails, the resolution will be retried with the specified
    //  prefix deleted from the implementation name, yielding the correct
    //  resolution:
    //
    //      method(wrapped_foo) -> nativeImplementation(foo)
    //
    //  The prefix is only used when standard resolution fails.
    /* ============================================================ */
    public boolean checkAutomaticResolution1() {
        if (AutomaticResolution1.foo() != Binder.WRAPPED_FOO_RETURN) {
            out.println("ERROR: native method AutomaticResolution1.wrapped_foo() wasn't correctly bound.");
            return false;
        }

        return true;
    }

    /* ============================================================ */
    public boolean checkAutomaticResolution2 (boolean isPrefixSet) {
        if (isPrefixSet) {
            if (AutomaticResolution2.foo() != Binder.FOO_RETURN) {
                out.println("ERROR: native method AutomaticResolution2.wrapped_foo() wasn't correctly bound.");
                return false;
            }
        } else {
            try {
                AutomaticResolution3.foo();
                out.println("ERROR: native method AutomaticResolution3.wrapped_foo() was bound.");
                return false;
            } catch (UnsatisfiedLinkError e) {
                out.println(String.format("The method isn't bound: %s %s\n", e.getClass().getName(), e.getMessage()));
                return true;
            }
        }

        return true;
    }

    /* ============================================================ */
    public boolean checkAutomaticResolution (boolean isMultiplePrefixes) {
        // Setting method prefix
        out.println("\tSetting prefix: "+prefix);
        if (isMultiplePrefixes) {
            if (!Binder.setMultiplePrefixes(prefix)) { return false; }
        } else {
            if (!Binder.setMethodPrefix(prefix)) { return false; }
        }

        // Check the behavior
        out.println("\t\tChecking resolution for AutomaticResolution1");
        if (!checkAutomaticResolution1()) { return false; }
        out.println("\t\tChecking resolution for AutomaticResolution2");
        if (!checkAutomaticResolution2(true)) { return false; }

        // Resetting method prefix
        out.println("\tResetting prefix");
        if (isMultiplePrefixes) {
            if (!Binder.setMultiplePrefixes(null)) { return false; }
        } else {
            if (!Binder.setMethodPrefix(null)) { return false; }
        }

        // Check the behavior
        out.println("\t\tChecking resolution for AutomaticResolution1");
        if (!checkAutomaticResolution1()) { return false; }
        out.println("\t\tChecking resolution for AutomaticResolution2");
        if (!checkAutomaticResolution2(false)) { return false; }

        return true;
    }

    /* ============================================================ */
    public boolean runIt(String [] args, PrintStream _out) {
        if (_out != null) {
            out = _out;
        }

        out.println("\nAutomatic resolution; SetMethodPrefix is used");
        if (!checkAutomaticResolution(true)) { return false; }

        out.println("\nAutomatic resolution; SetMultiplePrefixes is used");
        if (!checkAutomaticResolution(false)) { return false; }

        out.println("\nExplicit resolution; SetMethodPrefix is used");
        if (!checkExplicitResolution(true)) { return false; }

        out.println("\nExplicit resolution; SetMultiplePrefixes is used");
        if (!checkExplicitResolution(false)) { return false; }


        return true;
    }

    /* ============================================================ */
    public static int run(String argv[], PrintStream out) {
        if ((new SetNativeMethodPrefix001()).runIt(argv, out)) {
            out.println("TEST PASSED");
            return Consts.TEST_PASSED;
        } else {
            out.println("TEST FAILED");
            return Consts.TEST_FAILED;
        }
    }

    /* ============================================================ */
    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }
}
