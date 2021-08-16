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
 * @summary converted from VM Testbase vm/mlvm/indy/func/java/thisAsArgument.
 * VM Testbase keywords: [feature_mlvm]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test is written for a CR 6927831:
 *     InvokeDynamic throws NoClassDefFoundError in the following test:
 *     package test;
 *     import java.dyn.InvokeDynamic;
 *     import java.dyn.InvokeDynamicBootstrapError;
 *     public class Self {
 *       public static void main(String[] args) {
 *             try {
 *                 InvokeDynamic.<void>greet(new Self());
 *             } catch ( InvokeDynamicBootstrapError e ) {
 *                 System.out.println("TEST PASSED");
 *             } catch ( Throwable t ) {
 *                 System.err.println("Oops!");
 *                 t.printStackTrace();
 *             }
 *         }
 *     }
 *     ...when it is launched with -classpath:
 *     $ java -classpath bin test.Self
 *     Oops!
 *     java.lang.NoClassDefFoundError: test/Self
 *         at test.Self.main(Self.java:10)
 *     If we replace -classpath with -Xbootclasspath:
 *     $ java -Xbootclasspath/a:bin test.Self
 *     TEST PASSED
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.indy.func.java.thisAsArgument.INDIFY_Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm vm.mlvm.indy.func.java.thisAsArgument.INDIFY_Test
 */

package vm.mlvm.indy.func.java.thisAsArgument;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

import vm.mlvm.share.MlvmTest;

public class INDIFY_Test extends MlvmTest {

    public static void main(String[] args) { MlvmTest.launch(args); }

    // Indify-specific bootstrap trampoline
    private static MethodType MT_bootstrap() {
        return MethodType.methodType(Object.class, Object.class, Object.class, Object.class);
    }
    private static MethodHandle MH_bootstrap() throws NoSuchMethodException, IllegalAccessException {
        return MethodHandles.lookup().findStatic(
                INDIFY_Test.class,
                "bootstrap",
                MT_bootstrap());
    }

    public static Object bootstrap(Object lookup, Object name, Object type) throws Throwable {
        getLog().trace(0, "bootstrap(" +
                Arrays.asList(lookup.getClass(), lookup,
                        name.getClass(), name,
                        type.getClass(), type) + ") called");

        return new Object();
    }

    public static void target(INDIFY_Test arg) {
        getLog().trace(0, "target called: arg=" + arg);
        new Throwable("Stack trace").printStackTrace(getLog().getOutStream());
    }

    // Indify-specific invokedynamic call substitution
    private static MethodHandle INDY_call;
    private static MethodHandle INDY_call() throws Throwable {
        if (INDY_call != null)
            return INDY_call;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "target",
                MethodType.methodType(void.class, INDIFY_Test.class));

        return cs.dynamicInvoker();
    }

    @Override
    public boolean run() throws Throwable {
        try {
            // Substitution for:
            // InvokeDynamic.target(new INDIFY_Test());
            INDIFY_Test test = new INDIFY_Test();
            INDY_call().invokeExact(test);
            getLog().complain("Target method should not be called");
            return false;
        } catch ( BootstrapMethodError e ) {
            getLog().trace(0, "Caught exception as expected:");
            e.printStackTrace(getLog().getOutStream());
            return true;
        } catch ( Throwable t ) {
            getLog().complain("Wrong exception caught!");
            t.printStackTrace(getLog().getOutStream());
            return false;
        }
    }
}
