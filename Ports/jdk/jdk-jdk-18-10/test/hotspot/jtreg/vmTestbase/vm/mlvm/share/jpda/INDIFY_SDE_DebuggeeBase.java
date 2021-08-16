/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.share.jpda;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

import vm.mlvm.share.Env;
import vm.mlvm.share.Stratum;

@Stratum(stratumName="Logo", stratumSourceFileName="INDIFY_SDE_DebuggeeBase.logo")
public class INDIFY_SDE_DebuggeeBase extends Debuggee {

    private static MethodType MT_bootstrap() {
        return MethodType.methodType(Object.class, Object.class, Object.class, Object.class);
    }

    private static MethodHandle MH_bootstrap() throws NoSuchMethodException, IllegalAccessException {
        return MethodHandles.lookup().findStatic(
                INDIFY_SDE_DebuggeeBase.class, "bootstrap", MT_bootstrap());
    }

    private static MethodType MT_target() {
        return MethodType.methodType(void.class, String.class);
    }

    private static MethodHandle INDY_call;

    private static MethodHandle INDY_call() throws Throwable {
        if (INDY_call != null)
            return INDY_call;

        return ((CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(), "hello", MT_target())).dynamicInvoker();
    }

    private static Object bootstrap(Object l, Object n, Object t) throws Throwable {
Stratum_Logo_30_BOOTSTRAP:
        Env.traceNormal("BSM called: " + Arrays.asList(new Object[] { l, n, t }));
        getDebuggeeInstance().hangUpIfNeeded("bootstrap");
        return new ConstantCallSite(MethodHandles.lookup().findStatic(
                INDIFY_SDE_DebuggeeBase.class, "target", MT_target()));
    }

    public static void indyWrapper(String s) throws Throwable {
Stratum_Logo_20_INDY:
        INDY_call().invokeExact(s);
    }

    public static void target(String s) throws Throwable {
        Debuggee d;
Stratum_Logo_40_TARGET:
        d = getDebuggeeInstance();
        if ( d.isWarmingUp() )
            Env.traceDebug("Target called. Argument: [" + s + "]");
        else
            Env.traceNormal("Target called. Argument: [" + s + "]");
        d.hangUpIfNeeded("target");
    }

    public static void stop() throws Throwable {
Stratum_Logo_50_END:
        getDebuggeeInstance().hangUpIfNeeded("stop");
    }

    @Override
    protected void warmUp() throws Throwable {
        indyWrapper("warming up");
    }

    @Override
    public boolean runDebuggee() throws Throwable {
Stratum_Logo_10_BEGIN:
        indyWrapper("hello from main!");
        stop();
        return true;
    }
}
