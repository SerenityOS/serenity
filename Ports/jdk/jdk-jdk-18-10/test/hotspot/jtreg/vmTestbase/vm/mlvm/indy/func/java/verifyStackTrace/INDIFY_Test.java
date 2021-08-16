/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase vm/mlvm/indy/func/java/verifyStackTrace.
 * VM Testbase keywords: [feature_mlvm]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test verifies that a stack trace is readable and contains a correct stack in a bootstrap and a target methods
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.indy.func.java.verifyStackTrace.INDIFY_Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm vm.mlvm.indy.func.java.verifyStackTrace.INDIFY_Test
 */

package vm.mlvm.indy.func.java.verifyStackTrace;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

import vm.mlvm.share.MlvmTest;

public class INDIFY_Test extends MlvmTest {

    private static final String METHOD_NAME = "runFunky";
    private static final int MAX_FRAME = 10;

    public INDIFY_Test() {}

    private static MethodType MT_bootstrap() {
        return MethodType.methodType(CallSite.class, Lookup.class, String.class, MethodType.class);
    }

    private static MethodHandle MH_bootstrap() throws NoSuchMethodException, IllegalAccessException {
        return MethodHandles.lookup().findStatic(
                INDIFY_Test.class,
                "bootstrap",
                MT_bootstrap());
    }

    public static CallSite bootstrap(Lookup c, String name, MethodType mt) throws Throwable {
        getLog().trace(0, "Lookup " + c + "; method name = " + name + "; method type = " + mt);

        boolean found = false;
        StackTraceElement trace[] = new Throwable().getStackTrace();
        for ( int i = 1; i < MAX_FRAME; i++ ) {
            StackTraceElement stackFrame = trace[i];
            getLog().trace(0, "Caller " + i + " stack frame: " + stackFrame);
            if ( stackFrame.getMethodName().equals(METHOD_NAME) ) {
                getLog().trace(0, "Required stack frame found");
                found = true;
                break;
            }
        }

        if ( ! found )
            throw new RuntimeException("Can't find caller method name (" + METHOD_NAME + ") in a bootstrap method stack");

        return new ConstantCallSite(MethodHandles.lookup().findStatic(INDIFY_Test.class, "target", mt));
    }

    public static Throwable target(Object o, String s, int i) {
        getLog().trace(0, "Target called! Object = " + o + "; string = " + s + "; int = " + i);
        return new Throwable();
    }

    private static MethodHandle INDY_call;
    private static MethodHandle INDY_call() throws Throwable {
        if (INDY_call != null)
            return INDY_call;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Throwable.class, Object.class, String.class, int.class));

        return cs.dynamicInvoker();
    }

    public boolean runFunky() throws Throwable {
        // Throwable t = (Throwable) InvokeDynamic.greet(new Object(), "world", 123);
        Object o = new Object();
        String s = "world";
        int i = 123;
        Throwable t = (Throwable) INDY_call().invokeExact(o, s, i);

        StackTraceElement stackFrame = t.getStackTrace()[1];
        getLog().trace(0, "Stack trace element from target call: " + stackFrame);
        if ( ! stackFrame.getMethodName().equals(METHOD_NAME) )
            throw new Exception("Wrong method name in a bootstrap method: " + stackFrame);

        return true;
    }

    public boolean run() throws Throwable { return runFunky(); }

    public static void main(String[] args) { MlvmTest.launch(args); }
}
