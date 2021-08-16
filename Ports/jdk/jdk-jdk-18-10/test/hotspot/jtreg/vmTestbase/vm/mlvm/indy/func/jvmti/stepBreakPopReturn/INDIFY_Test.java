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
 * @summary converted from VM Testbase vm/mlvm/indy/func/jvmti/stepBreakPopReturn.
 * VM Testbase keywords: [feature_mlvm, jvmti, noJFR]
 * VM Testbase readme:
 * DESCRIPTION
 *     Test calls a boostrap and a target methods via InvokeDynamic call, verifying that the
 *     following JVMTI events are firing:
 *     - MethodEntry
 *     - SingleStep
 *     - Breakpoint
 *     Also it calls JVMTI function PopFrame() from the bootstrap method
 *     and ForceEarlyReturn() function from the target method
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.indy.func.jvmti.stepBreakPopReturn.INDIFY_Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm/native
 *      -agentlib:stepBreakPopReturn=verbose=
 *      vm.mlvm.indy.func.jvmti.stepBreakPopReturn.INDIFY_Test
 */

package vm.mlvm.indy.func.jvmti.stepBreakPopReturn;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

import vm.mlvm.share.MlvmTest;

public class INDIFY_Test extends MlvmTest {

    public static native void setDebuggeeMethodName(String name);
    public static native void setDebuggeeClassName(String name);
    public static native boolean checkStatus();

    static {
        System.loadLibrary("stepBreakPopReturn");
    }

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
        int i = 0; // For single step
        getLog().trace(i, "Lookup " + c + "; method name = " + name + "; method type = " + mt);
        CallSite cs = new ConstantCallSite(MethodHandles.lookup().findStatic(
                INDIFY_Test.class,
                "target",
                MethodType.methodType(int.class, Object.class, String.class, int.class)));
        return cs;
    }

    public static int target(Object o, String s, int i) {
        int j = 0; // For single step event
        getLog().trace(0, "Target called! Object = " + o + "; string = " + s + "; int = " + i);
        return i;
    }

    private static MethodHandle INDY_call1;
    private static MethodHandle INDY_call1() throws Throwable {
        if (INDY_call1 != null)
            return INDY_call1;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(int.class, Object.class, String.class, int.class));

        return cs.dynamicInvoker();
    }

    private static MethodHandle INDY_call2;
    private static MethodHandle INDY_call2() throws Throwable {
        if (INDY_call2 != null)
            return INDY_call2;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(int.class, Object.class, String.class, int.class));

        return cs.dynamicInvoker();
    }

    public boolean run() throws Throwable {
        setDebuggeeClassName("L" + INDIFY_Test.class.getName().replace('.', '/') + ";");
        setDebuggeeMethodName("target");

        Object o = new Object();
        String s = "heaven";
        int i = 789;

        // When agent is loaded and correctly working, every call to target() should return 0
        // (without agent target() returns 3rd argument)
        // so the sum variable should be 0 at every moment
        getLog().trace(0, "Call site 1");
        int sum = (int) INDY_call1().invokeExact(o, s, i);
        for ( i = 0; i < 5; i++ ) {
            getLog().trace(0, "Call site 2, sum=" + sum);
            sum += (int) INDY_call2().invokeExact(o, s, i + 789);
        }
        getLog().trace(0, "Direct call, sum=" + sum);
        sum += target(new Object(), "hell", 123);

        getLog().trace(0, "Done, sum=" + sum + " (should be 0)");

        return checkStatus() && sum == 0;
    }

    public static void main(String[] args) { MlvmTest.launch(args); }
}
