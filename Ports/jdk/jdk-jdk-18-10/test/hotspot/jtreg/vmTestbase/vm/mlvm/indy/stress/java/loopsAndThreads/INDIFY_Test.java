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

// generated from vm/mlvm/indy/stress/java/loopsAndThreads/INDIFY_Test.jmpp

package vm.mlvm.indy.stress.java.loopsAndThreads;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.atomic.AtomicLong;

import vm.mlvm.share.MlvmTest;
import vm.mlvm.share.Env;
import vm.mlvm.share.MultiThreadedTest;
import nsk.share.test.Stresser;
import nsk.share.Failure;

public class INDIFY_Test extends MultiThreadedTest {

    private AtomicLong _counter = new AtomicLong();
    private AtomicLong _expectedTargetCalls = new AtomicLong();

    public INDIFY_Test() {
        super();
    }

    private static MethodType MT_bootstrap() {
        return MethodType.methodType(Object.class, Object.class, Object.class, Object.class);
    }

    private static MethodHandle MH_bootstrap() throws Exception {
        return MethodHandles.lookup().findStatic(
                INDIFY_Test.class,
                "bootstrap",
                MT_bootstrap());
    }

    public static Object bootstrap(Object c, Object name, Object mt) throws Throwable {
        Env.traceVerbose("bootstrap: Class " + c + "; method name = " + name + "; method type = " + mt);
        CallSite cs = new ConstantCallSite(
                MethodHandles.lookup().findVirtual(
                INDIFY_Test.class,
                "target",
                MethodType.methodType(Object.class, String.class, int.class)));
        return cs;
    }

    public Object target(String s, int i) {
        Env.traceDebug("target called");
        _counter.incrementAndGet();
        return null;
    }

    private static MethodHandle INDY_call0;
    private static MethodHandle INDY_call0 () throws Throwable {
        if (INDY_call0 != null)
            return INDY_call0;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call1;
    private static MethodHandle INDY_call1 () throws Throwable {
        if (INDY_call1 != null)
            return INDY_call1;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call2;
    private static MethodHandle INDY_call2 () throws Throwable {
        if (INDY_call2 != null)
            return INDY_call2;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call3;
    private static MethodHandle INDY_call3 () throws Throwable {
        if (INDY_call3 != null)
            return INDY_call3;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call4;
    private static MethodHandle INDY_call4 () throws Throwable {
        if (INDY_call4 != null)
            return INDY_call4;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call5;
    private static MethodHandle INDY_call5 () throws Throwable {
        if (INDY_call5 != null)
            return INDY_call5;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call6;
    private static MethodHandle INDY_call6 () throws Throwable {
        if (INDY_call6 != null)
            return INDY_call6;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call7;
    private static MethodHandle INDY_call7 () throws Throwable {
        if (INDY_call7 != null)
            return INDY_call7;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call8;
    private static MethodHandle INDY_call8 () throws Throwable {
        if (INDY_call8 != null)
            return INDY_call8;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call9;
    private static MethodHandle INDY_call9 () throws Throwable {
        if (INDY_call9 != null)
            return INDY_call9;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call10;
    private static MethodHandle INDY_call10 () throws Throwable {
        if (INDY_call10 != null)
            return INDY_call10;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call11;
    private static MethodHandle INDY_call11 () throws Throwable {
        if (INDY_call11 != null)
            return INDY_call11;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call12;
    private static MethodHandle INDY_call12 () throws Throwable {
        if (INDY_call12 != null)
            return INDY_call12;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call13;
    private static MethodHandle INDY_call13 () throws Throwable {
        if (INDY_call13 != null)
            return INDY_call13;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call14;
    private static MethodHandle INDY_call14 () throws Throwable {
        if (INDY_call14 != null)
            return INDY_call14;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call15;
    private static MethodHandle INDY_call15 () throws Throwable {
        if (INDY_call15 != null)
            return INDY_call15;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call16;
    private static MethodHandle INDY_call16 () throws Throwable {
        if (INDY_call16 != null)
            return INDY_call16;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call17;
    private static MethodHandle INDY_call17 () throws Throwable {
        if (INDY_call17 != null)
            return INDY_call17;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call18;
    private static MethodHandle INDY_call18 () throws Throwable {
        if (INDY_call18 != null)
            return INDY_call18;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call19;
    private static MethodHandle INDY_call19 () throws Throwable {
        if (INDY_call19 != null)
            return INDY_call19;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call20;
    private static MethodHandle INDY_call20 () throws Throwable {
        if (INDY_call20 != null)
            return INDY_call20;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call21;
    private static MethodHandle INDY_call21 () throws Throwable {
        if (INDY_call21 != null)
            return INDY_call21;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call22;
    private static MethodHandle INDY_call22 () throws Throwable {
        if (INDY_call22 != null)
            return INDY_call22;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call23;
    private static MethodHandle INDY_call23 () throws Throwable {
        if (INDY_call23 != null)
            return INDY_call23;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call24;
    private static MethodHandle INDY_call24 () throws Throwable {
        if (INDY_call24 != null)
            return INDY_call24;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call25;
    private static MethodHandle INDY_call25 () throws Throwable {
        if (INDY_call25 != null)
            return INDY_call25;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call26;
    private static MethodHandle INDY_call26 () throws Throwable {
        if (INDY_call26 != null)
            return INDY_call26;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call27;
    private static MethodHandle INDY_call27 () throws Throwable {
        if (INDY_call27 != null)
            return INDY_call27;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call28;
    private static MethodHandle INDY_call28 () throws Throwable {
        if (INDY_call28 != null)
            return INDY_call28;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call29;
    private static MethodHandle INDY_call29 () throws Throwable {
        if (INDY_call29 != null)
            return INDY_call29;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call30;
    private static MethodHandle INDY_call30 () throws Throwable {
        if (INDY_call30 != null)
            return INDY_call30;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call31;
    private static MethodHandle INDY_call31 () throws Throwable {
        if (INDY_call31 != null)
            return INDY_call31;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call32;
    private static MethodHandle INDY_call32 () throws Throwable {
        if (INDY_call32 != null)
            return INDY_call32;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call33;
    private static MethodHandle INDY_call33 () throws Throwable {
        if (INDY_call33 != null)
            return INDY_call33;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call34;
    private static MethodHandle INDY_call34 () throws Throwable {
        if (INDY_call34 != null)
            return INDY_call34;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call35;
    private static MethodHandle INDY_call35 () throws Throwable {
        if (INDY_call35 != null)
            return INDY_call35;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call36;
    private static MethodHandle INDY_call36 () throws Throwable {
        if (INDY_call36 != null)
            return INDY_call36;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call37;
    private static MethodHandle INDY_call37 () throws Throwable {
        if (INDY_call37 != null)
            return INDY_call37;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call38;
    private static MethodHandle INDY_call38 () throws Throwable {
        if (INDY_call38 != null)
            return INDY_call38;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call39;
    private static MethodHandle INDY_call39 () throws Throwable {
        if (INDY_call39 != null)
            return INDY_call39;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call40;
    private static MethodHandle INDY_call40 () throws Throwable {
        if (INDY_call40 != null)
            return INDY_call40;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call41;
    private static MethodHandle INDY_call41 () throws Throwable {
        if (INDY_call41 != null)
            return INDY_call41;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call42;
    private static MethodHandle INDY_call42 () throws Throwable {
        if (INDY_call42 != null)
            return INDY_call42;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call43;
    private static MethodHandle INDY_call43 () throws Throwable {
        if (INDY_call43 != null)
            return INDY_call43;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call44;
    private static MethodHandle INDY_call44 () throws Throwable {
        if (INDY_call44 != null)
            return INDY_call44;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call45;
    private static MethodHandle INDY_call45 () throws Throwable {
        if (INDY_call45 != null)
            return INDY_call45;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call46;
    private static MethodHandle INDY_call46 () throws Throwable {
        if (INDY_call46 != null)
            return INDY_call46;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call47;
    private static MethodHandle INDY_call47 () throws Throwable {
        if (INDY_call47 != null)
            return INDY_call47;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call48;
    private static MethodHandle INDY_call48 () throws Throwable {
        if (INDY_call48 != null)
            return INDY_call48;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call49;
    private static MethodHandle INDY_call49 () throws Throwable {
        if (INDY_call49 != null)
            return INDY_call49;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call50;
    private static MethodHandle INDY_call50 () throws Throwable {
        if (INDY_call50 != null)
            return INDY_call50;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call51;
    private static MethodHandle INDY_call51 () throws Throwable {
        if (INDY_call51 != null)
            return INDY_call51;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call52;
    private static MethodHandle INDY_call52 () throws Throwable {
        if (INDY_call52 != null)
            return INDY_call52;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call53;
    private static MethodHandle INDY_call53 () throws Throwable {
        if (INDY_call53 != null)
            return INDY_call53;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call54;
    private static MethodHandle INDY_call54 () throws Throwable {
        if (INDY_call54 != null)
            return INDY_call54;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call55;
    private static MethodHandle INDY_call55 () throws Throwable {
        if (INDY_call55 != null)
            return INDY_call55;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call56;
    private static MethodHandle INDY_call56 () throws Throwable {
        if (INDY_call56 != null)
            return INDY_call56;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call57;
    private static MethodHandle INDY_call57 () throws Throwable {
        if (INDY_call57 != null)
            return INDY_call57;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call58;
    private static MethodHandle INDY_call58 () throws Throwable {
        if (INDY_call58 != null)
            return INDY_call58;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call59;
    private static MethodHandle INDY_call59 () throws Throwable {
        if (INDY_call59 != null)
            return INDY_call59;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call60;
    private static MethodHandle INDY_call60 () throws Throwable {
        if (INDY_call60 != null)
            return INDY_call60;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call61;
    private static MethodHandle INDY_call61 () throws Throwable {
        if (INDY_call61 != null)
            return INDY_call61;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call62;
    private static MethodHandle INDY_call62 () throws Throwable {
        if (INDY_call62 != null)
            return INDY_call62;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call63;
    private static MethodHandle INDY_call63 () throws Throwable {
        if (INDY_call63 != null)
            return INDY_call63;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call64;
    private static MethodHandle INDY_call64 () throws Throwable {
        if (INDY_call64 != null)
            return INDY_call64;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call65;
    private static MethodHandle INDY_call65 () throws Throwable {
        if (INDY_call65 != null)
            return INDY_call65;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call66;
    private static MethodHandle INDY_call66 () throws Throwable {
        if (INDY_call66 != null)
            return INDY_call66;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call67;
    private static MethodHandle INDY_call67 () throws Throwable {
        if (INDY_call67 != null)
            return INDY_call67;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call68;
    private static MethodHandle INDY_call68 () throws Throwable {
        if (INDY_call68 != null)
            return INDY_call68;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call69;
    private static MethodHandle INDY_call69 () throws Throwable {
        if (INDY_call69 != null)
            return INDY_call69;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call70;
    private static MethodHandle INDY_call70 () throws Throwable {
        if (INDY_call70 != null)
            return INDY_call70;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call71;
    private static MethodHandle INDY_call71 () throws Throwable {
        if (INDY_call71 != null)
            return INDY_call71;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call72;
    private static MethodHandle INDY_call72 () throws Throwable {
        if (INDY_call72 != null)
            return INDY_call72;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call73;
    private static MethodHandle INDY_call73 () throws Throwable {
        if (INDY_call73 != null)
            return INDY_call73;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call74;
    private static MethodHandle INDY_call74 () throws Throwable {
        if (INDY_call74 != null)
            return INDY_call74;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call75;
    private static MethodHandle INDY_call75 () throws Throwable {
        if (INDY_call75 != null)
            return INDY_call75;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call76;
    private static MethodHandle INDY_call76 () throws Throwable {
        if (INDY_call76 != null)
            return INDY_call76;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call77;
    private static MethodHandle INDY_call77 () throws Throwable {
        if (INDY_call77 != null)
            return INDY_call77;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call78;
    private static MethodHandle INDY_call78 () throws Throwable {
        if (INDY_call78 != null)
            return INDY_call78;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call79;
    private static MethodHandle INDY_call79 () throws Throwable {
        if (INDY_call79 != null)
            return INDY_call79;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call80;
    private static MethodHandle INDY_call80 () throws Throwable {
        if (INDY_call80 != null)
            return INDY_call80;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call81;
    private static MethodHandle INDY_call81 () throws Throwable {
        if (INDY_call81 != null)
            return INDY_call81;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call82;
    private static MethodHandle INDY_call82 () throws Throwable {
        if (INDY_call82 != null)
            return INDY_call82;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call83;
    private static MethodHandle INDY_call83 () throws Throwable {
        if (INDY_call83 != null)
            return INDY_call83;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call84;
    private static MethodHandle INDY_call84 () throws Throwable {
        if (INDY_call84 != null)
            return INDY_call84;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call85;
    private static MethodHandle INDY_call85 () throws Throwable {
        if (INDY_call85 != null)
            return INDY_call85;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call86;
    private static MethodHandle INDY_call86 () throws Throwable {
        if (INDY_call86 != null)
            return INDY_call86;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call87;
    private static MethodHandle INDY_call87 () throws Throwable {
        if (INDY_call87 != null)
            return INDY_call87;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call88;
    private static MethodHandle INDY_call88 () throws Throwable {
        if (INDY_call88 != null)
            return INDY_call88;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call89;
    private static MethodHandle INDY_call89 () throws Throwable {
        if (INDY_call89 != null)
            return INDY_call89;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call90;
    private static MethodHandle INDY_call90 () throws Throwable {
        if (INDY_call90 != null)
            return INDY_call90;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call91;
    private static MethodHandle INDY_call91 () throws Throwable {
        if (INDY_call91 != null)
            return INDY_call91;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call92;
    private static MethodHandle INDY_call92 () throws Throwable {
        if (INDY_call92 != null)
            return INDY_call92;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call93;
    private static MethodHandle INDY_call93 () throws Throwable {
        if (INDY_call93 != null)
            return INDY_call93;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call94;
    private static MethodHandle INDY_call94 () throws Throwable {
        if (INDY_call94 != null)
            return INDY_call94;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call95;
    private static MethodHandle INDY_call95 () throws Throwable {
        if (INDY_call95 != null)
            return INDY_call95;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call96;
    private static MethodHandle INDY_call96 () throws Throwable {
        if (INDY_call96 != null)
            return INDY_call96;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call97;
    private static MethodHandle INDY_call97 () throws Throwable {
        if (INDY_call97 != null)
            return INDY_call97;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call98;
    private static MethodHandle INDY_call98 () throws Throwable {
        if (INDY_call98 != null)
            return INDY_call98;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call99;
    private static MethodHandle INDY_call99 () throws Throwable {
        if (INDY_call99 != null)
            return INDY_call99;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call100;
    private static MethodHandle INDY_call100 () throws Throwable {
        if (INDY_call100 != null)
            return INDY_call100;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call101;
    private static MethodHandle INDY_call101 () throws Throwable {
        if (INDY_call101 != null)
            return INDY_call101;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call102;
    private static MethodHandle INDY_call102 () throws Throwable {
        if (INDY_call102 != null)
            return INDY_call102;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call103;
    private static MethodHandle INDY_call103 () throws Throwable {
        if (INDY_call103 != null)
            return INDY_call103;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call104;
    private static MethodHandle INDY_call104 () throws Throwable {
        if (INDY_call104 != null)
            return INDY_call104;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call105;
    private static MethodHandle INDY_call105 () throws Throwable {
        if (INDY_call105 != null)
            return INDY_call105;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call106;
    private static MethodHandle INDY_call106 () throws Throwable {
        if (INDY_call106 != null)
            return INDY_call106;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call107;
    private static MethodHandle INDY_call107 () throws Throwable {
        if (INDY_call107 != null)
            return INDY_call107;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call108;
    private static MethodHandle INDY_call108 () throws Throwable {
        if (INDY_call108 != null)
            return INDY_call108;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call109;
    private static MethodHandle INDY_call109 () throws Throwable {
        if (INDY_call109 != null)
            return INDY_call109;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call110;
    private static MethodHandle INDY_call110 () throws Throwable {
        if (INDY_call110 != null)
            return INDY_call110;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call111;
    private static MethodHandle INDY_call111 () throws Throwable {
        if (INDY_call111 != null)
            return INDY_call111;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call112;
    private static MethodHandle INDY_call112 () throws Throwable {
        if (INDY_call112 != null)
            return INDY_call112;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call113;
    private static MethodHandle INDY_call113 () throws Throwable {
        if (INDY_call113 != null)
            return INDY_call113;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call114;
    private static MethodHandle INDY_call114 () throws Throwable {
        if (INDY_call114 != null)
            return INDY_call114;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call115;
    private static MethodHandle INDY_call115 () throws Throwable {
        if (INDY_call115 != null)
            return INDY_call115;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call116;
    private static MethodHandle INDY_call116 () throws Throwable {
        if (INDY_call116 != null)
            return INDY_call116;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call117;
    private static MethodHandle INDY_call117 () throws Throwable {
        if (INDY_call117 != null)
            return INDY_call117;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call118;
    private static MethodHandle INDY_call118 () throws Throwable {
        if (INDY_call118 != null)
            return INDY_call118;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call119;
    private static MethodHandle INDY_call119 () throws Throwable {
        if (INDY_call119 != null)
            return INDY_call119;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call120;
    private static MethodHandle INDY_call120 () throws Throwable {
        if (INDY_call120 != null)
            return INDY_call120;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call121;
    private static MethodHandle INDY_call121 () throws Throwable {
        if (INDY_call121 != null)
            return INDY_call121;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call122;
    private static MethodHandle INDY_call122 () throws Throwable {
        if (INDY_call122 != null)
            return INDY_call122;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call123;
    private static MethodHandle INDY_call123 () throws Throwable {
        if (INDY_call123 != null)
            return INDY_call123;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call124;
    private static MethodHandle INDY_call124 () throws Throwable {
        if (INDY_call124 != null)
            return INDY_call124;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call125;
    private static MethodHandle INDY_call125 () throws Throwable {
        if (INDY_call125 != null)
            return INDY_call125;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call126;
    private static MethodHandle INDY_call126 () throws Throwable {
        if (INDY_call126 != null)
            return INDY_call126;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call127;
    private static MethodHandle INDY_call127 () throws Throwable {
        if (INDY_call127 != null)
            return INDY_call127;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call128;
    private static MethodHandle INDY_call128 () throws Throwable {
        if (INDY_call128 != null)
            return INDY_call128;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call129;
    private static MethodHandle INDY_call129 () throws Throwable {
        if (INDY_call129 != null)
            return INDY_call129;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call130;
    private static MethodHandle INDY_call130 () throws Throwable {
        if (INDY_call130 != null)
            return INDY_call130;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call131;
    private static MethodHandle INDY_call131 () throws Throwable {
        if (INDY_call131 != null)
            return INDY_call131;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call132;
    private static MethodHandle INDY_call132 () throws Throwable {
        if (INDY_call132 != null)
            return INDY_call132;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call133;
    private static MethodHandle INDY_call133 () throws Throwable {
        if (INDY_call133 != null)
            return INDY_call133;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call134;
    private static MethodHandle INDY_call134 () throws Throwable {
        if (INDY_call134 != null)
            return INDY_call134;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call135;
    private static MethodHandle INDY_call135 () throws Throwable {
        if (INDY_call135 != null)
            return INDY_call135;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call136;
    private static MethodHandle INDY_call136 () throws Throwable {
        if (INDY_call136 != null)
            return INDY_call136;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call137;
    private static MethodHandle INDY_call137 () throws Throwable {
        if (INDY_call137 != null)
            return INDY_call137;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call138;
    private static MethodHandle INDY_call138 () throws Throwable {
        if (INDY_call138 != null)
            return INDY_call138;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call139;
    private static MethodHandle INDY_call139 () throws Throwable {
        if (INDY_call139 != null)
            return INDY_call139;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call140;
    private static MethodHandle INDY_call140 () throws Throwable {
        if (INDY_call140 != null)
            return INDY_call140;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call141;
    private static MethodHandle INDY_call141 () throws Throwable {
        if (INDY_call141 != null)
            return INDY_call141;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call142;
    private static MethodHandle INDY_call142 () throws Throwable {
        if (INDY_call142 != null)
            return INDY_call142;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call143;
    private static MethodHandle INDY_call143 () throws Throwable {
        if (INDY_call143 != null)
            return INDY_call143;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call144;
    private static MethodHandle INDY_call144 () throws Throwable {
        if (INDY_call144 != null)
            return INDY_call144;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call145;
    private static MethodHandle INDY_call145 () throws Throwable {
        if (INDY_call145 != null)
            return INDY_call145;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call146;
    private static MethodHandle INDY_call146 () throws Throwable {
        if (INDY_call146 != null)
            return INDY_call146;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call147;
    private static MethodHandle INDY_call147 () throws Throwable {
        if (INDY_call147 != null)
            return INDY_call147;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call148;
    private static MethodHandle INDY_call148 () throws Throwable {
        if (INDY_call148 != null)
            return INDY_call148;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call149;
    private static MethodHandle INDY_call149 () throws Throwable {
        if (INDY_call149 != null)
            return INDY_call149;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call150;
    private static MethodHandle INDY_call150 () throws Throwable {
        if (INDY_call150 != null)
            return INDY_call150;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call151;
    private static MethodHandle INDY_call151 () throws Throwable {
        if (INDY_call151 != null)
            return INDY_call151;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call152;
    private static MethodHandle INDY_call152 () throws Throwable {
        if (INDY_call152 != null)
            return INDY_call152;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call153;
    private static MethodHandle INDY_call153 () throws Throwable {
        if (INDY_call153 != null)
            return INDY_call153;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call154;
    private static MethodHandle INDY_call154 () throws Throwable {
        if (INDY_call154 != null)
            return INDY_call154;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call155;
    private static MethodHandle INDY_call155 () throws Throwable {
        if (INDY_call155 != null)
            return INDY_call155;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call156;
    private static MethodHandle INDY_call156 () throws Throwable {
        if (INDY_call156 != null)
            return INDY_call156;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call157;
    private static MethodHandle INDY_call157 () throws Throwable {
        if (INDY_call157 != null)
            return INDY_call157;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call158;
    private static MethodHandle INDY_call158 () throws Throwable {
        if (INDY_call158 != null)
            return INDY_call158;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call159;
    private static MethodHandle INDY_call159 () throws Throwable {
        if (INDY_call159 != null)
            return INDY_call159;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call160;
    private static MethodHandle INDY_call160 () throws Throwable {
        if (INDY_call160 != null)
            return INDY_call160;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call161;
    private static MethodHandle INDY_call161 () throws Throwable {
        if (INDY_call161 != null)
            return INDY_call161;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call162;
    private static MethodHandle INDY_call162 () throws Throwable {
        if (INDY_call162 != null)
            return INDY_call162;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call163;
    private static MethodHandle INDY_call163 () throws Throwable {
        if (INDY_call163 != null)
            return INDY_call163;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call164;
    private static MethodHandle INDY_call164 () throws Throwable {
        if (INDY_call164 != null)
            return INDY_call164;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call165;
    private static MethodHandle INDY_call165 () throws Throwable {
        if (INDY_call165 != null)
            return INDY_call165;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call166;
    private static MethodHandle INDY_call166 () throws Throwable {
        if (INDY_call166 != null)
            return INDY_call166;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call167;
    private static MethodHandle INDY_call167 () throws Throwable {
        if (INDY_call167 != null)
            return INDY_call167;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call168;
    private static MethodHandle INDY_call168 () throws Throwable {
        if (INDY_call168 != null)
            return INDY_call168;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call169;
    private static MethodHandle INDY_call169 () throws Throwable {
        if (INDY_call169 != null)
            return INDY_call169;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call170;
    private static MethodHandle INDY_call170 () throws Throwable {
        if (INDY_call170 != null)
            return INDY_call170;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call171;
    private static MethodHandle INDY_call171 () throws Throwable {
        if (INDY_call171 != null)
            return INDY_call171;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call172;
    private static MethodHandle INDY_call172 () throws Throwable {
        if (INDY_call172 != null)
            return INDY_call172;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call173;
    private static MethodHandle INDY_call173 () throws Throwable {
        if (INDY_call173 != null)
            return INDY_call173;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call174;
    private static MethodHandle INDY_call174 () throws Throwable {
        if (INDY_call174 != null)
            return INDY_call174;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call175;
    private static MethodHandle INDY_call175 () throws Throwable {
        if (INDY_call175 != null)
            return INDY_call175;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call176;
    private static MethodHandle INDY_call176 () throws Throwable {
        if (INDY_call176 != null)
            return INDY_call176;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call177;
    private static MethodHandle INDY_call177 () throws Throwable {
        if (INDY_call177 != null)
            return INDY_call177;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call178;
    private static MethodHandle INDY_call178 () throws Throwable {
        if (INDY_call178 != null)
            return INDY_call178;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call179;
    private static MethodHandle INDY_call179 () throws Throwable {
        if (INDY_call179 != null)
            return INDY_call179;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call180;
    private static MethodHandle INDY_call180 () throws Throwable {
        if (INDY_call180 != null)
            return INDY_call180;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call181;
    private static MethodHandle INDY_call181 () throws Throwable {
        if (INDY_call181 != null)
            return INDY_call181;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call182;
    private static MethodHandle INDY_call182 () throws Throwable {
        if (INDY_call182 != null)
            return INDY_call182;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call183;
    private static MethodHandle INDY_call183 () throws Throwable {
        if (INDY_call183 != null)
            return INDY_call183;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call184;
    private static MethodHandle INDY_call184 () throws Throwable {
        if (INDY_call184 != null)
            return INDY_call184;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call185;
    private static MethodHandle INDY_call185 () throws Throwable {
        if (INDY_call185 != null)
            return INDY_call185;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call186;
    private static MethodHandle INDY_call186 () throws Throwable {
        if (INDY_call186 != null)
            return INDY_call186;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call187;
    private static MethodHandle INDY_call187 () throws Throwable {
        if (INDY_call187 != null)
            return INDY_call187;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call188;
    private static MethodHandle INDY_call188 () throws Throwable {
        if (INDY_call188 != null)
            return INDY_call188;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call189;
    private static MethodHandle INDY_call189 () throws Throwable {
        if (INDY_call189 != null)
            return INDY_call189;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call190;
    private static MethodHandle INDY_call190 () throws Throwable {
        if (INDY_call190 != null)
            return INDY_call190;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call191;
    private static MethodHandle INDY_call191 () throws Throwable {
        if (INDY_call191 != null)
            return INDY_call191;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call192;
    private static MethodHandle INDY_call192 () throws Throwable {
        if (INDY_call192 != null)
            return INDY_call192;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call193;
    private static MethodHandle INDY_call193 () throws Throwable {
        if (INDY_call193 != null)
            return INDY_call193;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call194;
    private static MethodHandle INDY_call194 () throws Throwable {
        if (INDY_call194 != null)
            return INDY_call194;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call195;
    private static MethodHandle INDY_call195 () throws Throwable {
        if (INDY_call195 != null)
            return INDY_call195;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call196;
    private static MethodHandle INDY_call196 () throws Throwable {
        if (INDY_call196 != null)
            return INDY_call196;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call197;
    private static MethodHandle INDY_call197 () throws Throwable {
        if (INDY_call197 != null)
            return INDY_call197;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call198;
    private static MethodHandle INDY_call198 () throws Throwable {
        if (INDY_call198 != null)
            return INDY_call198;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call199;
    private static MethodHandle INDY_call199 () throws Throwable {
        if (INDY_call199 != null)
            return INDY_call199;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call200;
    private static MethodHandle INDY_call200 () throws Throwable {
        if (INDY_call200 != null)
            return INDY_call200;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call201;
    private static MethodHandle INDY_call201 () throws Throwable {
        if (INDY_call201 != null)
            return INDY_call201;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call202;
    private static MethodHandle INDY_call202 () throws Throwable {
        if (INDY_call202 != null)
            return INDY_call202;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call203;
    private static MethodHandle INDY_call203 () throws Throwable {
        if (INDY_call203 != null)
            return INDY_call203;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call204;
    private static MethodHandle INDY_call204 () throws Throwable {
        if (INDY_call204 != null)
            return INDY_call204;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call205;
    private static MethodHandle INDY_call205 () throws Throwable {
        if (INDY_call205 != null)
            return INDY_call205;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call206;
    private static MethodHandle INDY_call206 () throws Throwable {
        if (INDY_call206 != null)
            return INDY_call206;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call207;
    private static MethodHandle INDY_call207 () throws Throwable {
        if (INDY_call207 != null)
            return INDY_call207;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call208;
    private static MethodHandle INDY_call208 () throws Throwable {
        if (INDY_call208 != null)
            return INDY_call208;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call209;
    private static MethodHandle INDY_call209 () throws Throwable {
        if (INDY_call209 != null)
            return INDY_call209;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call210;
    private static MethodHandle INDY_call210 () throws Throwable {
        if (INDY_call210 != null)
            return INDY_call210;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call211;
    private static MethodHandle INDY_call211 () throws Throwable {
        if (INDY_call211 != null)
            return INDY_call211;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call212;
    private static MethodHandle INDY_call212 () throws Throwable {
        if (INDY_call212 != null)
            return INDY_call212;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call213;
    private static MethodHandle INDY_call213 () throws Throwable {
        if (INDY_call213 != null)
            return INDY_call213;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call214;
    private static MethodHandle INDY_call214 () throws Throwable {
        if (INDY_call214 != null)
            return INDY_call214;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call215;
    private static MethodHandle INDY_call215 () throws Throwable {
        if (INDY_call215 != null)
            return INDY_call215;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call216;
    private static MethodHandle INDY_call216 () throws Throwable {
        if (INDY_call216 != null)
            return INDY_call216;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call217;
    private static MethodHandle INDY_call217 () throws Throwable {
        if (INDY_call217 != null)
            return INDY_call217;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call218;
    private static MethodHandle INDY_call218 () throws Throwable {
        if (INDY_call218 != null)
            return INDY_call218;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call219;
    private static MethodHandle INDY_call219 () throws Throwable {
        if (INDY_call219 != null)
            return INDY_call219;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call220;
    private static MethodHandle INDY_call220 () throws Throwable {
        if (INDY_call220 != null)
            return INDY_call220;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call221;
    private static MethodHandle INDY_call221 () throws Throwable {
        if (INDY_call221 != null)
            return INDY_call221;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call222;
    private static MethodHandle INDY_call222 () throws Throwable {
        if (INDY_call222 != null)
            return INDY_call222;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call223;
    private static MethodHandle INDY_call223 () throws Throwable {
        if (INDY_call223 != null)
            return INDY_call223;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call224;
    private static MethodHandle INDY_call224 () throws Throwable {
        if (INDY_call224 != null)
            return INDY_call224;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call225;
    private static MethodHandle INDY_call225 () throws Throwable {
        if (INDY_call225 != null)
            return INDY_call225;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call226;
    private static MethodHandle INDY_call226 () throws Throwable {
        if (INDY_call226 != null)
            return INDY_call226;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call227;
    private static MethodHandle INDY_call227 () throws Throwable {
        if (INDY_call227 != null)
            return INDY_call227;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call228;
    private static MethodHandle INDY_call228 () throws Throwable {
        if (INDY_call228 != null)
            return INDY_call228;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call229;
    private static MethodHandle INDY_call229 () throws Throwable {
        if (INDY_call229 != null)
            return INDY_call229;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call230;
    private static MethodHandle INDY_call230 () throws Throwable {
        if (INDY_call230 != null)
            return INDY_call230;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call231;
    private static MethodHandle INDY_call231 () throws Throwable {
        if (INDY_call231 != null)
            return INDY_call231;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call232;
    private static MethodHandle INDY_call232 () throws Throwable {
        if (INDY_call232 != null)
            return INDY_call232;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call233;
    private static MethodHandle INDY_call233 () throws Throwable {
        if (INDY_call233 != null)
            return INDY_call233;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call234;
    private static MethodHandle INDY_call234 () throws Throwable {
        if (INDY_call234 != null)
            return INDY_call234;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call235;
    private static MethodHandle INDY_call235 () throws Throwable {
        if (INDY_call235 != null)
            return INDY_call235;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call236;
    private static MethodHandle INDY_call236 () throws Throwable {
        if (INDY_call236 != null)
            return INDY_call236;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call237;
    private static MethodHandle INDY_call237 () throws Throwable {
        if (INDY_call237 != null)
            return INDY_call237;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call238;
    private static MethodHandle INDY_call238 () throws Throwable {
        if (INDY_call238 != null)
            return INDY_call238;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call239;
    private static MethodHandle INDY_call239 () throws Throwable {
        if (INDY_call239 != null)
            return INDY_call239;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call240;
    private static MethodHandle INDY_call240 () throws Throwable {
        if (INDY_call240 != null)
            return INDY_call240;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call241;
    private static MethodHandle INDY_call241 () throws Throwable {
        if (INDY_call241 != null)
            return INDY_call241;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call242;
    private static MethodHandle INDY_call242 () throws Throwable {
        if (INDY_call242 != null)
            return INDY_call242;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call243;
    private static MethodHandle INDY_call243 () throws Throwable {
        if (INDY_call243 != null)
            return INDY_call243;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call244;
    private static MethodHandle INDY_call244 () throws Throwable {
        if (INDY_call244 != null)
            return INDY_call244;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call245;
    private static MethodHandle INDY_call245 () throws Throwable {
        if (INDY_call245 != null)
            return INDY_call245;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call246;
    private static MethodHandle INDY_call246 () throws Throwable {
        if (INDY_call246 != null)
            return INDY_call246;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call247;
    private static MethodHandle INDY_call247 () throws Throwable {
        if (INDY_call247 != null)
            return INDY_call247;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call248;
    private static MethodHandle INDY_call248 () throws Throwable {
        if (INDY_call248 != null)
            return INDY_call248;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call249;
    private static MethodHandle INDY_call249 () throws Throwable {
        if (INDY_call249 != null)
            return INDY_call249;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call250;
    private static MethodHandle INDY_call250 () throws Throwable {
        if (INDY_call250 != null)
            return INDY_call250;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call251;
    private static MethodHandle INDY_call251 () throws Throwable {
        if (INDY_call251 != null)
            return INDY_call251;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call252;
    private static MethodHandle INDY_call252 () throws Throwable {
        if (INDY_call252 != null)
            return INDY_call252;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call253;
    private static MethodHandle INDY_call253 () throws Throwable {
        if (INDY_call253 != null)
            return INDY_call253;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call254;
    private static MethodHandle INDY_call254 () throws Throwable {
        if (INDY_call254 != null)
            return INDY_call254;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call255;
    private static MethodHandle INDY_call255 () throws Throwable {
        if (INDY_call255 != null)
            return INDY_call255;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call256;
    private static MethodHandle INDY_call256 () throws Throwable {
        if (INDY_call256 != null)
            return INDY_call256;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call257;
    private static MethodHandle INDY_call257 () throws Throwable {
        if (INDY_call257 != null)
            return INDY_call257;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call258;
    private static MethodHandle INDY_call258 () throws Throwable {
        if (INDY_call258 != null)
            return INDY_call258;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call259;
    private static MethodHandle INDY_call259 () throws Throwable {
        if (INDY_call259 != null)
            return INDY_call259;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call260;
    private static MethodHandle INDY_call260 () throws Throwable {
        if (INDY_call260 != null)
            return INDY_call260;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call261;
    private static MethodHandle INDY_call261 () throws Throwable {
        if (INDY_call261 != null)
            return INDY_call261;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call262;
    private static MethodHandle INDY_call262 () throws Throwable {
        if (INDY_call262 != null)
            return INDY_call262;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call263;
    private static MethodHandle INDY_call263 () throws Throwable {
        if (INDY_call263 != null)
            return INDY_call263;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call264;
    private static MethodHandle INDY_call264 () throws Throwable {
        if (INDY_call264 != null)
            return INDY_call264;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call265;
    private static MethodHandle INDY_call265 () throws Throwable {
        if (INDY_call265 != null)
            return INDY_call265;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call266;
    private static MethodHandle INDY_call266 () throws Throwable {
        if (INDY_call266 != null)
            return INDY_call266;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call267;
    private static MethodHandle INDY_call267 () throws Throwable {
        if (INDY_call267 != null)
            return INDY_call267;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call268;
    private static MethodHandle INDY_call268 () throws Throwable {
        if (INDY_call268 != null)
            return INDY_call268;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call269;
    private static MethodHandle INDY_call269 () throws Throwable {
        if (INDY_call269 != null)
            return INDY_call269;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call270;
    private static MethodHandle INDY_call270 () throws Throwable {
        if (INDY_call270 != null)
            return INDY_call270;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call271;
    private static MethodHandle INDY_call271 () throws Throwable {
        if (INDY_call271 != null)
            return INDY_call271;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call272;
    private static MethodHandle INDY_call272 () throws Throwable {
        if (INDY_call272 != null)
            return INDY_call272;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call273;
    private static MethodHandle INDY_call273 () throws Throwable {
        if (INDY_call273 != null)
            return INDY_call273;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call274;
    private static MethodHandle INDY_call274 () throws Throwable {
        if (INDY_call274 != null)
            return INDY_call274;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call275;
    private static MethodHandle INDY_call275 () throws Throwable {
        if (INDY_call275 != null)
            return INDY_call275;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call276;
    private static MethodHandle INDY_call276 () throws Throwable {
        if (INDY_call276 != null)
            return INDY_call276;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call277;
    private static MethodHandle INDY_call277 () throws Throwable {
        if (INDY_call277 != null)
            return INDY_call277;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call278;
    private static MethodHandle INDY_call278 () throws Throwable {
        if (INDY_call278 != null)
            return INDY_call278;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call279;
    private static MethodHandle INDY_call279 () throws Throwable {
        if (INDY_call279 != null)
            return INDY_call279;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call280;
    private static MethodHandle INDY_call280 () throws Throwable {
        if (INDY_call280 != null)
            return INDY_call280;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call281;
    private static MethodHandle INDY_call281 () throws Throwable {
        if (INDY_call281 != null)
            return INDY_call281;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call282;
    private static MethodHandle INDY_call282 () throws Throwable {
        if (INDY_call282 != null)
            return INDY_call282;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call283;
    private static MethodHandle INDY_call283 () throws Throwable {
        if (INDY_call283 != null)
            return INDY_call283;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call284;
    private static MethodHandle INDY_call284 () throws Throwable {
        if (INDY_call284 != null)
            return INDY_call284;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call285;
    private static MethodHandle INDY_call285 () throws Throwable {
        if (INDY_call285 != null)
            return INDY_call285;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call286;
    private static MethodHandle INDY_call286 () throws Throwable {
        if (INDY_call286 != null)
            return INDY_call286;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call287;
    private static MethodHandle INDY_call287 () throws Throwable {
        if (INDY_call287 != null)
            return INDY_call287;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call288;
    private static MethodHandle INDY_call288 () throws Throwable {
        if (INDY_call288 != null)
            return INDY_call288;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call289;
    private static MethodHandle INDY_call289 () throws Throwable {
        if (INDY_call289 != null)
            return INDY_call289;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call290;
    private static MethodHandle INDY_call290 () throws Throwable {
        if (INDY_call290 != null)
            return INDY_call290;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call291;
    private static MethodHandle INDY_call291 () throws Throwable {
        if (INDY_call291 != null)
            return INDY_call291;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call292;
    private static MethodHandle INDY_call292 () throws Throwable {
        if (INDY_call292 != null)
            return INDY_call292;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call293;
    private static MethodHandle INDY_call293 () throws Throwable {
        if (INDY_call293 != null)
            return INDY_call293;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call294;
    private static MethodHandle INDY_call294 () throws Throwable {
        if (INDY_call294 != null)
            return INDY_call294;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call295;
    private static MethodHandle INDY_call295 () throws Throwable {
        if (INDY_call295 != null)
            return INDY_call295;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call296;
    private static MethodHandle INDY_call296 () throws Throwable {
        if (INDY_call296 != null)
            return INDY_call296;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call297;
    private static MethodHandle INDY_call297 () throws Throwable {
        if (INDY_call297 != null)
            return INDY_call297;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call298;
    private static MethodHandle INDY_call298 () throws Throwable {
        if (INDY_call298 != null)
            return INDY_call298;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call299;
    private static MethodHandle INDY_call299 () throws Throwable {
        if (INDY_call299 != null)
            return INDY_call299;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call300;
    private static MethodHandle INDY_call300 () throws Throwable {
        if (INDY_call300 != null)
            return INDY_call300;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call301;
    private static MethodHandle INDY_call301 () throws Throwable {
        if (INDY_call301 != null)
            return INDY_call301;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call302;
    private static MethodHandle INDY_call302 () throws Throwable {
        if (INDY_call302 != null)
            return INDY_call302;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call303;
    private static MethodHandle INDY_call303 () throws Throwable {
        if (INDY_call303 != null)
            return INDY_call303;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call304;
    private static MethodHandle INDY_call304 () throws Throwable {
        if (INDY_call304 != null)
            return INDY_call304;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call305;
    private static MethodHandle INDY_call305 () throws Throwable {
        if (INDY_call305 != null)
            return INDY_call305;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call306;
    private static MethodHandle INDY_call306 () throws Throwable {
        if (INDY_call306 != null)
            return INDY_call306;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call307;
    private static MethodHandle INDY_call307 () throws Throwable {
        if (INDY_call307 != null)
            return INDY_call307;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call308;
    private static MethodHandle INDY_call308 () throws Throwable {
        if (INDY_call308 != null)
            return INDY_call308;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call309;
    private static MethodHandle INDY_call309 () throws Throwable {
        if (INDY_call309 != null)
            return INDY_call309;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call310;
    private static MethodHandle INDY_call310 () throws Throwable {
        if (INDY_call310 != null)
            return INDY_call310;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call311;
    private static MethodHandle INDY_call311 () throws Throwable {
        if (INDY_call311 != null)
            return INDY_call311;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call312;
    private static MethodHandle INDY_call312 () throws Throwable {
        if (INDY_call312 != null)
            return INDY_call312;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call313;
    private static MethodHandle INDY_call313 () throws Throwable {
        if (INDY_call313 != null)
            return INDY_call313;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call314;
    private static MethodHandle INDY_call314 () throws Throwable {
        if (INDY_call314 != null)
            return INDY_call314;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call315;
    private static MethodHandle INDY_call315 () throws Throwable {
        if (INDY_call315 != null)
            return INDY_call315;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call316;
    private static MethodHandle INDY_call316 () throws Throwable {
        if (INDY_call316 != null)
            return INDY_call316;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call317;
    private static MethodHandle INDY_call317 () throws Throwable {
        if (INDY_call317 != null)
            return INDY_call317;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call318;
    private static MethodHandle INDY_call318 () throws Throwable {
        if (INDY_call318 != null)
            return INDY_call318;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call319;
    private static MethodHandle INDY_call319 () throws Throwable {
        if (INDY_call319 != null)
            return INDY_call319;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call320;
    private static MethodHandle INDY_call320 () throws Throwable {
        if (INDY_call320 != null)
            return INDY_call320;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call321;
    private static MethodHandle INDY_call321 () throws Throwable {
        if (INDY_call321 != null)
            return INDY_call321;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call322;
    private static MethodHandle INDY_call322 () throws Throwable {
        if (INDY_call322 != null)
            return INDY_call322;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call323;
    private static MethodHandle INDY_call323 () throws Throwable {
        if (INDY_call323 != null)
            return INDY_call323;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call324;
    private static MethodHandle INDY_call324 () throws Throwable {
        if (INDY_call324 != null)
            return INDY_call324;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call325;
    private static MethodHandle INDY_call325 () throws Throwable {
        if (INDY_call325 != null)
            return INDY_call325;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call326;
    private static MethodHandle INDY_call326 () throws Throwable {
        if (INDY_call326 != null)
            return INDY_call326;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call327;
    private static MethodHandle INDY_call327 () throws Throwable {
        if (INDY_call327 != null)
            return INDY_call327;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call328;
    private static MethodHandle INDY_call328 () throws Throwable {
        if (INDY_call328 != null)
            return INDY_call328;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call329;
    private static MethodHandle INDY_call329 () throws Throwable {
        if (INDY_call329 != null)
            return INDY_call329;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call330;
    private static MethodHandle INDY_call330 () throws Throwable {
        if (INDY_call330 != null)
            return INDY_call330;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call331;
    private static MethodHandle INDY_call331 () throws Throwable {
        if (INDY_call331 != null)
            return INDY_call331;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call332;
    private static MethodHandle INDY_call332 () throws Throwable {
        if (INDY_call332 != null)
            return INDY_call332;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call333;
    private static MethodHandle INDY_call333 () throws Throwable {
        if (INDY_call333 != null)
            return INDY_call333;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call334;
    private static MethodHandle INDY_call334 () throws Throwable {
        if (INDY_call334 != null)
            return INDY_call334;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call335;
    private static MethodHandle INDY_call335 () throws Throwable {
        if (INDY_call335 != null)
            return INDY_call335;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call336;
    private static MethodHandle INDY_call336 () throws Throwable {
        if (INDY_call336 != null)
            return INDY_call336;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call337;
    private static MethodHandle INDY_call337 () throws Throwable {
        if (INDY_call337 != null)
            return INDY_call337;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call338;
    private static MethodHandle INDY_call338 () throws Throwable {
        if (INDY_call338 != null)
            return INDY_call338;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call339;
    private static MethodHandle INDY_call339 () throws Throwable {
        if (INDY_call339 != null)
            return INDY_call339;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call340;
    private static MethodHandle INDY_call340 () throws Throwable {
        if (INDY_call340 != null)
            return INDY_call340;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call341;
    private static MethodHandle INDY_call341 () throws Throwable {
        if (INDY_call341 != null)
            return INDY_call341;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call342;
    private static MethodHandle INDY_call342 () throws Throwable {
        if (INDY_call342 != null)
            return INDY_call342;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call343;
    private static MethodHandle INDY_call343 () throws Throwable {
        if (INDY_call343 != null)
            return INDY_call343;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call344;
    private static MethodHandle INDY_call344 () throws Throwable {
        if (INDY_call344 != null)
            return INDY_call344;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call345;
    private static MethodHandle INDY_call345 () throws Throwable {
        if (INDY_call345 != null)
            return INDY_call345;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call346;
    private static MethodHandle INDY_call346 () throws Throwable {
        if (INDY_call346 != null)
            return INDY_call346;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call347;
    private static MethodHandle INDY_call347 () throws Throwable {
        if (INDY_call347 != null)
            return INDY_call347;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call348;
    private static MethodHandle INDY_call348 () throws Throwable {
        if (INDY_call348 != null)
            return INDY_call348;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call349;
    private static MethodHandle INDY_call349 () throws Throwable {
        if (INDY_call349 != null)
            return INDY_call349;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call350;
    private static MethodHandle INDY_call350 () throws Throwable {
        if (INDY_call350 != null)
            return INDY_call350;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call351;
    private static MethodHandle INDY_call351 () throws Throwable {
        if (INDY_call351 != null)
            return INDY_call351;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call352;
    private static MethodHandle INDY_call352 () throws Throwable {
        if (INDY_call352 != null)
            return INDY_call352;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call353;
    private static MethodHandle INDY_call353 () throws Throwable {
        if (INDY_call353 != null)
            return INDY_call353;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call354;
    private static MethodHandle INDY_call354 () throws Throwable {
        if (INDY_call354 != null)
            return INDY_call354;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call355;
    private static MethodHandle INDY_call355 () throws Throwable {
        if (INDY_call355 != null)
            return INDY_call355;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call356;
    private static MethodHandle INDY_call356 () throws Throwable {
        if (INDY_call356 != null)
            return INDY_call356;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call357;
    private static MethodHandle INDY_call357 () throws Throwable {
        if (INDY_call357 != null)
            return INDY_call357;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call358;
    private static MethodHandle INDY_call358 () throws Throwable {
        if (INDY_call358 != null)
            return INDY_call358;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call359;
    private static MethodHandle INDY_call359 () throws Throwable {
        if (INDY_call359 != null)
            return INDY_call359;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call360;
    private static MethodHandle INDY_call360 () throws Throwable {
        if (INDY_call360 != null)
            return INDY_call360;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call361;
    private static MethodHandle INDY_call361 () throws Throwable {
        if (INDY_call361 != null)
            return INDY_call361;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call362;
    private static MethodHandle INDY_call362 () throws Throwable {
        if (INDY_call362 != null)
            return INDY_call362;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call363;
    private static MethodHandle INDY_call363 () throws Throwable {
        if (INDY_call363 != null)
            return INDY_call363;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call364;
    private static MethodHandle INDY_call364 () throws Throwable {
        if (INDY_call364 != null)
            return INDY_call364;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call365;
    private static MethodHandle INDY_call365 () throws Throwable {
        if (INDY_call365 != null)
            return INDY_call365;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call366;
    private static MethodHandle INDY_call366 () throws Throwable {
        if (INDY_call366 != null)
            return INDY_call366;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call367;
    private static MethodHandle INDY_call367 () throws Throwable {
        if (INDY_call367 != null)
            return INDY_call367;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call368;
    private static MethodHandle INDY_call368 () throws Throwable {
        if (INDY_call368 != null)
            return INDY_call368;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call369;
    private static MethodHandle INDY_call369 () throws Throwable {
        if (INDY_call369 != null)
            return INDY_call369;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call370;
    private static MethodHandle INDY_call370 () throws Throwable {
        if (INDY_call370 != null)
            return INDY_call370;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call371;
    private static MethodHandle INDY_call371 () throws Throwable {
        if (INDY_call371 != null)
            return INDY_call371;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call372;
    private static MethodHandle INDY_call372 () throws Throwable {
        if (INDY_call372 != null)
            return INDY_call372;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call373;
    private static MethodHandle INDY_call373 () throws Throwable {
        if (INDY_call373 != null)
            return INDY_call373;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call374;
    private static MethodHandle INDY_call374 () throws Throwable {
        if (INDY_call374 != null)
            return INDY_call374;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call375;
    private static MethodHandle INDY_call375 () throws Throwable {
        if (INDY_call375 != null)
            return INDY_call375;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call376;
    private static MethodHandle INDY_call376 () throws Throwable {
        if (INDY_call376 != null)
            return INDY_call376;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call377;
    private static MethodHandle INDY_call377 () throws Throwable {
        if (INDY_call377 != null)
            return INDY_call377;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call378;
    private static MethodHandle INDY_call378 () throws Throwable {
        if (INDY_call378 != null)
            return INDY_call378;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call379;
    private static MethodHandle INDY_call379 () throws Throwable {
        if (INDY_call379 != null)
            return INDY_call379;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call380;
    private static MethodHandle INDY_call380 () throws Throwable {
        if (INDY_call380 != null)
            return INDY_call380;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call381;
    private static MethodHandle INDY_call381 () throws Throwable {
        if (INDY_call381 != null)
            return INDY_call381;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call382;
    private static MethodHandle INDY_call382 () throws Throwable {
        if (INDY_call382 != null)
            return INDY_call382;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call383;
    private static MethodHandle INDY_call383 () throws Throwable {
        if (INDY_call383 != null)
            return INDY_call383;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call384;
    private static MethodHandle INDY_call384 () throws Throwable {
        if (INDY_call384 != null)
            return INDY_call384;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call385;
    private static MethodHandle INDY_call385 () throws Throwable {
        if (INDY_call385 != null)
            return INDY_call385;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call386;
    private static MethodHandle INDY_call386 () throws Throwable {
        if (INDY_call386 != null)
            return INDY_call386;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call387;
    private static MethodHandle INDY_call387 () throws Throwable {
        if (INDY_call387 != null)
            return INDY_call387;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call388;
    private static MethodHandle INDY_call388 () throws Throwable {
        if (INDY_call388 != null)
            return INDY_call388;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call389;
    private static MethodHandle INDY_call389 () throws Throwable {
        if (INDY_call389 != null)
            return INDY_call389;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call390;
    private static MethodHandle INDY_call390 () throws Throwable {
        if (INDY_call390 != null)
            return INDY_call390;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call391;
    private static MethodHandle INDY_call391 () throws Throwable {
        if (INDY_call391 != null)
            return INDY_call391;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call392;
    private static MethodHandle INDY_call392 () throws Throwable {
        if (INDY_call392 != null)
            return INDY_call392;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call393;
    private static MethodHandle INDY_call393 () throws Throwable {
        if (INDY_call393 != null)
            return INDY_call393;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call394;
    private static MethodHandle INDY_call394 () throws Throwable {
        if (INDY_call394 != null)
            return INDY_call394;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call395;
    private static MethodHandle INDY_call395 () throws Throwable {
        if (INDY_call395 != null)
            return INDY_call395;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call396;
    private static MethodHandle INDY_call396 () throws Throwable {
        if (INDY_call396 != null)
            return INDY_call396;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call397;
    private static MethodHandle INDY_call397 () throws Throwable {
        if (INDY_call397 != null)
            return INDY_call397;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call398;
    private static MethodHandle INDY_call398 () throws Throwable {
        if (INDY_call398 != null)
            return INDY_call398;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call399;
    private static MethodHandle INDY_call399 () throws Throwable {
        if (INDY_call399 != null)
            return INDY_call399;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call400;
    private static MethodHandle INDY_call400 () throws Throwable {
        if (INDY_call400 != null)
            return INDY_call400;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call401;
    private static MethodHandle INDY_call401 () throws Throwable {
        if (INDY_call401 != null)
            return INDY_call401;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call402;
    private static MethodHandle INDY_call402 () throws Throwable {
        if (INDY_call402 != null)
            return INDY_call402;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call403;
    private static MethodHandle INDY_call403 () throws Throwable {
        if (INDY_call403 != null)
            return INDY_call403;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call404;
    private static MethodHandle INDY_call404 () throws Throwable {
        if (INDY_call404 != null)
            return INDY_call404;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call405;
    private static MethodHandle INDY_call405 () throws Throwable {
        if (INDY_call405 != null)
            return INDY_call405;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call406;
    private static MethodHandle INDY_call406 () throws Throwable {
        if (INDY_call406 != null)
            return INDY_call406;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call407;
    private static MethodHandle INDY_call407 () throws Throwable {
        if (INDY_call407 != null)
            return INDY_call407;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call408;
    private static MethodHandle INDY_call408 () throws Throwable {
        if (INDY_call408 != null)
            return INDY_call408;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call409;
    private static MethodHandle INDY_call409 () throws Throwable {
        if (INDY_call409 != null)
            return INDY_call409;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call410;
    private static MethodHandle INDY_call410 () throws Throwable {
        if (INDY_call410 != null)
            return INDY_call410;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call411;
    private static MethodHandle INDY_call411 () throws Throwable {
        if (INDY_call411 != null)
            return INDY_call411;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call412;
    private static MethodHandle INDY_call412 () throws Throwable {
        if (INDY_call412 != null)
            return INDY_call412;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call413;
    private static MethodHandle INDY_call413 () throws Throwable {
        if (INDY_call413 != null)
            return INDY_call413;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call414;
    private static MethodHandle INDY_call414 () throws Throwable {
        if (INDY_call414 != null)
            return INDY_call414;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call415;
    private static MethodHandle INDY_call415 () throws Throwable {
        if (INDY_call415 != null)
            return INDY_call415;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call416;
    private static MethodHandle INDY_call416 () throws Throwable {
        if (INDY_call416 != null)
            return INDY_call416;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call417;
    private static MethodHandle INDY_call417 () throws Throwable {
        if (INDY_call417 != null)
            return INDY_call417;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call418;
    private static MethodHandle INDY_call418 () throws Throwable {
        if (INDY_call418 != null)
            return INDY_call418;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call419;
    private static MethodHandle INDY_call419 () throws Throwable {
        if (INDY_call419 != null)
            return INDY_call419;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call420;
    private static MethodHandle INDY_call420 () throws Throwable {
        if (INDY_call420 != null)
            return INDY_call420;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call421;
    private static MethodHandle INDY_call421 () throws Throwable {
        if (INDY_call421 != null)
            return INDY_call421;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call422;
    private static MethodHandle INDY_call422 () throws Throwable {
        if (INDY_call422 != null)
            return INDY_call422;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call423;
    private static MethodHandle INDY_call423 () throws Throwable {
        if (INDY_call423 != null)
            return INDY_call423;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call424;
    private static MethodHandle INDY_call424 () throws Throwable {
        if (INDY_call424 != null)
            return INDY_call424;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call425;
    private static MethodHandle INDY_call425 () throws Throwable {
        if (INDY_call425 != null)
            return INDY_call425;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call426;
    private static MethodHandle INDY_call426 () throws Throwable {
        if (INDY_call426 != null)
            return INDY_call426;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call427;
    private static MethodHandle INDY_call427 () throws Throwable {
        if (INDY_call427 != null)
            return INDY_call427;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call428;
    private static MethodHandle INDY_call428 () throws Throwable {
        if (INDY_call428 != null)
            return INDY_call428;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call429;
    private static MethodHandle INDY_call429 () throws Throwable {
        if (INDY_call429 != null)
            return INDY_call429;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call430;
    private static MethodHandle INDY_call430 () throws Throwable {
        if (INDY_call430 != null)
            return INDY_call430;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call431;
    private static MethodHandle INDY_call431 () throws Throwable {
        if (INDY_call431 != null)
            return INDY_call431;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call432;
    private static MethodHandle INDY_call432 () throws Throwable {
        if (INDY_call432 != null)
            return INDY_call432;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call433;
    private static MethodHandle INDY_call433 () throws Throwable {
        if (INDY_call433 != null)
            return INDY_call433;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call434;
    private static MethodHandle INDY_call434 () throws Throwable {
        if (INDY_call434 != null)
            return INDY_call434;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call435;
    private static MethodHandle INDY_call435 () throws Throwable {
        if (INDY_call435 != null)
            return INDY_call435;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call436;
    private static MethodHandle INDY_call436 () throws Throwable {
        if (INDY_call436 != null)
            return INDY_call436;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call437;
    private static MethodHandle INDY_call437 () throws Throwable {
        if (INDY_call437 != null)
            return INDY_call437;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call438;
    private static MethodHandle INDY_call438 () throws Throwable {
        if (INDY_call438 != null)
            return INDY_call438;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call439;
    private static MethodHandle INDY_call439 () throws Throwable {
        if (INDY_call439 != null)
            return INDY_call439;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call440;
    private static MethodHandle INDY_call440 () throws Throwable {
        if (INDY_call440 != null)
            return INDY_call440;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call441;
    private static MethodHandle INDY_call441 () throws Throwable {
        if (INDY_call441 != null)
            return INDY_call441;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call442;
    private static MethodHandle INDY_call442 () throws Throwable {
        if (INDY_call442 != null)
            return INDY_call442;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call443;
    private static MethodHandle INDY_call443 () throws Throwable {
        if (INDY_call443 != null)
            return INDY_call443;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call444;
    private static MethodHandle INDY_call444 () throws Throwable {
        if (INDY_call444 != null)
            return INDY_call444;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call445;
    private static MethodHandle INDY_call445 () throws Throwable {
        if (INDY_call445 != null)
            return INDY_call445;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call446;
    private static MethodHandle INDY_call446 () throws Throwable {
        if (INDY_call446 != null)
            return INDY_call446;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call447;
    private static MethodHandle INDY_call447 () throws Throwable {
        if (INDY_call447 != null)
            return INDY_call447;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call448;
    private static MethodHandle INDY_call448 () throws Throwable {
        if (INDY_call448 != null)
            return INDY_call448;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call449;
    private static MethodHandle INDY_call449 () throws Throwable {
        if (INDY_call449 != null)
            return INDY_call449;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call450;
    private static MethodHandle INDY_call450 () throws Throwable {
        if (INDY_call450 != null)
            return INDY_call450;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call451;
    private static MethodHandle INDY_call451 () throws Throwable {
        if (INDY_call451 != null)
            return INDY_call451;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call452;
    private static MethodHandle INDY_call452 () throws Throwable {
        if (INDY_call452 != null)
            return INDY_call452;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call453;
    private static MethodHandle INDY_call453 () throws Throwable {
        if (INDY_call453 != null)
            return INDY_call453;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call454;
    private static MethodHandle INDY_call454 () throws Throwable {
        if (INDY_call454 != null)
            return INDY_call454;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call455;
    private static MethodHandle INDY_call455 () throws Throwable {
        if (INDY_call455 != null)
            return INDY_call455;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call456;
    private static MethodHandle INDY_call456 () throws Throwable {
        if (INDY_call456 != null)
            return INDY_call456;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call457;
    private static MethodHandle INDY_call457 () throws Throwable {
        if (INDY_call457 != null)
            return INDY_call457;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call458;
    private static MethodHandle INDY_call458 () throws Throwable {
        if (INDY_call458 != null)
            return INDY_call458;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call459;
    private static MethodHandle INDY_call459 () throws Throwable {
        if (INDY_call459 != null)
            return INDY_call459;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call460;
    private static MethodHandle INDY_call460 () throws Throwable {
        if (INDY_call460 != null)
            return INDY_call460;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call461;
    private static MethodHandle INDY_call461 () throws Throwable {
        if (INDY_call461 != null)
            return INDY_call461;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call462;
    private static MethodHandle INDY_call462 () throws Throwable {
        if (INDY_call462 != null)
            return INDY_call462;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call463;
    private static MethodHandle INDY_call463 () throws Throwable {
        if (INDY_call463 != null)
            return INDY_call463;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call464;
    private static MethodHandle INDY_call464 () throws Throwable {
        if (INDY_call464 != null)
            return INDY_call464;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call465;
    private static MethodHandle INDY_call465 () throws Throwable {
        if (INDY_call465 != null)
            return INDY_call465;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call466;
    private static MethodHandle INDY_call466 () throws Throwable {
        if (INDY_call466 != null)
            return INDY_call466;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call467;
    private static MethodHandle INDY_call467 () throws Throwable {
        if (INDY_call467 != null)
            return INDY_call467;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call468;
    private static MethodHandle INDY_call468 () throws Throwable {
        if (INDY_call468 != null)
            return INDY_call468;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call469;
    private static MethodHandle INDY_call469 () throws Throwable {
        if (INDY_call469 != null)
            return INDY_call469;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call470;
    private static MethodHandle INDY_call470 () throws Throwable {
        if (INDY_call470 != null)
            return INDY_call470;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call471;
    private static MethodHandle INDY_call471 () throws Throwable {
        if (INDY_call471 != null)
            return INDY_call471;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call472;
    private static MethodHandle INDY_call472 () throws Throwable {
        if (INDY_call472 != null)
            return INDY_call472;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call473;
    private static MethodHandle INDY_call473 () throws Throwable {
        if (INDY_call473 != null)
            return INDY_call473;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call474;
    private static MethodHandle INDY_call474 () throws Throwable {
        if (INDY_call474 != null)
            return INDY_call474;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call475;
    private static MethodHandle INDY_call475 () throws Throwable {
        if (INDY_call475 != null)
            return INDY_call475;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call476;
    private static MethodHandle INDY_call476 () throws Throwable {
        if (INDY_call476 != null)
            return INDY_call476;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call477;
    private static MethodHandle INDY_call477 () throws Throwable {
        if (INDY_call477 != null)
            return INDY_call477;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call478;
    private static MethodHandle INDY_call478 () throws Throwable {
        if (INDY_call478 != null)
            return INDY_call478;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call479;
    private static MethodHandle INDY_call479 () throws Throwable {
        if (INDY_call479 != null)
            return INDY_call479;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call480;
    private static MethodHandle INDY_call480 () throws Throwable {
        if (INDY_call480 != null)
            return INDY_call480;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call481;
    private static MethodHandle INDY_call481 () throws Throwable {
        if (INDY_call481 != null)
            return INDY_call481;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call482;
    private static MethodHandle INDY_call482 () throws Throwable {
        if (INDY_call482 != null)
            return INDY_call482;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call483;
    private static MethodHandle INDY_call483 () throws Throwable {
        if (INDY_call483 != null)
            return INDY_call483;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call484;
    private static MethodHandle INDY_call484 () throws Throwable {
        if (INDY_call484 != null)
            return INDY_call484;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call485;
    private static MethodHandle INDY_call485 () throws Throwable {
        if (INDY_call485 != null)
            return INDY_call485;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call486;
    private static MethodHandle INDY_call486 () throws Throwable {
        if (INDY_call486 != null)
            return INDY_call486;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call487;
    private static MethodHandle INDY_call487 () throws Throwable {
        if (INDY_call487 != null)
            return INDY_call487;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call488;
    private static MethodHandle INDY_call488 () throws Throwable {
        if (INDY_call488 != null)
            return INDY_call488;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call489;
    private static MethodHandle INDY_call489 () throws Throwable {
        if (INDY_call489 != null)
            return INDY_call489;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call490;
    private static MethodHandle INDY_call490 () throws Throwable {
        if (INDY_call490 != null)
            return INDY_call490;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call491;
    private static MethodHandle INDY_call491 () throws Throwable {
        if (INDY_call491 != null)
            return INDY_call491;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call492;
    private static MethodHandle INDY_call492 () throws Throwable {
        if (INDY_call492 != null)
            return INDY_call492;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call493;
    private static MethodHandle INDY_call493 () throws Throwable {
        if (INDY_call493 != null)
            return INDY_call493;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call494;
    private static MethodHandle INDY_call494 () throws Throwable {
        if (INDY_call494 != null)
            return INDY_call494;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call495;
    private static MethodHandle INDY_call495 () throws Throwable {
        if (INDY_call495 != null)
            return INDY_call495;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call496;
    private static MethodHandle INDY_call496 () throws Throwable {
        if (INDY_call496 != null)
            return INDY_call496;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call497;
    private static MethodHandle INDY_call497 () throws Throwable {
        if (INDY_call497 != null)
            return INDY_call497;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call498;
    private static MethodHandle INDY_call498 () throws Throwable {
        if (INDY_call498 != null)
            return INDY_call498;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call499;
    private static MethodHandle INDY_call499 () throws Throwable {
        if (INDY_call499 != null)
            return INDY_call499;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call500;
    private static MethodHandle INDY_call500 () throws Throwable {
        if (INDY_call500 != null)
            return INDY_call500;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call501;
    private static MethodHandle INDY_call501 () throws Throwable {
        if (INDY_call501 != null)
            return INDY_call501;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call502;
    private static MethodHandle INDY_call502 () throws Throwable {
        if (INDY_call502 != null)
            return INDY_call502;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call503;
    private static MethodHandle INDY_call503 () throws Throwable {
        if (INDY_call503 != null)
            return INDY_call503;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call504;
    private static MethodHandle INDY_call504 () throws Throwable {
        if (INDY_call504 != null)
            return INDY_call504;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call505;
    private static MethodHandle INDY_call505 () throws Throwable {
        if (INDY_call505 != null)
            return INDY_call505;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call506;
    private static MethodHandle INDY_call506 () throws Throwable {
        if (INDY_call506 != null)
            return INDY_call506;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call507;
    private static MethodHandle INDY_call507 () throws Throwable {
        if (INDY_call507 != null)
            return INDY_call507;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call508;
    private static MethodHandle INDY_call508 () throws Throwable {
        if (INDY_call508 != null)
            return INDY_call508;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call509;
    private static MethodHandle INDY_call509 () throws Throwable {
        if (INDY_call509 != null)
            return INDY_call509;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call510;
    private static MethodHandle INDY_call510 () throws Throwable {
        if (INDY_call510 != null)
            return INDY_call510;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call511;
    private static MethodHandle INDY_call511 () throws Throwable {
        if (INDY_call511 != null)
            return INDY_call511;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call512;
    private static MethodHandle INDY_call512 () throws Throwable {
        if (INDY_call512 != null)
            return INDY_call512;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call513;
    private static MethodHandle INDY_call513 () throws Throwable {
        if (INDY_call513 != null)
            return INDY_call513;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call514;
    private static MethodHandle INDY_call514 () throws Throwable {
        if (INDY_call514 != null)
            return INDY_call514;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call515;
    private static MethodHandle INDY_call515 () throws Throwable {
        if (INDY_call515 != null)
            return INDY_call515;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call516;
    private static MethodHandle INDY_call516 () throws Throwable {
        if (INDY_call516 != null)
            return INDY_call516;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call517;
    private static MethodHandle INDY_call517 () throws Throwable {
        if (INDY_call517 != null)
            return INDY_call517;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call518;
    private static MethodHandle INDY_call518 () throws Throwable {
        if (INDY_call518 != null)
            return INDY_call518;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call519;
    private static MethodHandle INDY_call519 () throws Throwable {
        if (INDY_call519 != null)
            return INDY_call519;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call520;
    private static MethodHandle INDY_call520 () throws Throwable {
        if (INDY_call520 != null)
            return INDY_call520;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call521;
    private static MethodHandle INDY_call521 () throws Throwable {
        if (INDY_call521 != null)
            return INDY_call521;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call522;
    private static MethodHandle INDY_call522 () throws Throwable {
        if (INDY_call522 != null)
            return INDY_call522;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call523;
    private static MethodHandle INDY_call523 () throws Throwable {
        if (INDY_call523 != null)
            return INDY_call523;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call524;
    private static MethodHandle INDY_call524 () throws Throwable {
        if (INDY_call524 != null)
            return INDY_call524;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call525;
    private static MethodHandle INDY_call525 () throws Throwable {
        if (INDY_call525 != null)
            return INDY_call525;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call526;
    private static MethodHandle INDY_call526 () throws Throwable {
        if (INDY_call526 != null)
            return INDY_call526;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call527;
    private static MethodHandle INDY_call527 () throws Throwable {
        if (INDY_call527 != null)
            return INDY_call527;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call528;
    private static MethodHandle INDY_call528 () throws Throwable {
        if (INDY_call528 != null)
            return INDY_call528;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call529;
    private static MethodHandle INDY_call529 () throws Throwable {
        if (INDY_call529 != null)
            return INDY_call529;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call530;
    private static MethodHandle INDY_call530 () throws Throwable {
        if (INDY_call530 != null)
            return INDY_call530;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call531;
    private static MethodHandle INDY_call531 () throws Throwable {
        if (INDY_call531 != null)
            return INDY_call531;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call532;
    private static MethodHandle INDY_call532 () throws Throwable {
        if (INDY_call532 != null)
            return INDY_call532;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call533;
    private static MethodHandle INDY_call533 () throws Throwable {
        if (INDY_call533 != null)
            return INDY_call533;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call534;
    private static MethodHandle INDY_call534 () throws Throwable {
        if (INDY_call534 != null)
            return INDY_call534;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call535;
    private static MethodHandle INDY_call535 () throws Throwable {
        if (INDY_call535 != null)
            return INDY_call535;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call536;
    private static MethodHandle INDY_call536 () throws Throwable {
        if (INDY_call536 != null)
            return INDY_call536;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call537;
    private static MethodHandle INDY_call537 () throws Throwable {
        if (INDY_call537 != null)
            return INDY_call537;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call538;
    private static MethodHandle INDY_call538 () throws Throwable {
        if (INDY_call538 != null)
            return INDY_call538;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call539;
    private static MethodHandle INDY_call539 () throws Throwable {
        if (INDY_call539 != null)
            return INDY_call539;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call540;
    private static MethodHandle INDY_call540 () throws Throwable {
        if (INDY_call540 != null)
            return INDY_call540;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call541;
    private static MethodHandle INDY_call541 () throws Throwable {
        if (INDY_call541 != null)
            return INDY_call541;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call542;
    private static MethodHandle INDY_call542 () throws Throwable {
        if (INDY_call542 != null)
            return INDY_call542;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call543;
    private static MethodHandle INDY_call543 () throws Throwable {
        if (INDY_call543 != null)
            return INDY_call543;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call544;
    private static MethodHandle INDY_call544 () throws Throwable {
        if (INDY_call544 != null)
            return INDY_call544;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call545;
    private static MethodHandle INDY_call545 () throws Throwable {
        if (INDY_call545 != null)
            return INDY_call545;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call546;
    private static MethodHandle INDY_call546 () throws Throwable {
        if (INDY_call546 != null)
            return INDY_call546;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call547;
    private static MethodHandle INDY_call547 () throws Throwable {
        if (INDY_call547 != null)
            return INDY_call547;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call548;
    private static MethodHandle INDY_call548 () throws Throwable {
        if (INDY_call548 != null)
            return INDY_call548;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call549;
    private static MethodHandle INDY_call549 () throws Throwable {
        if (INDY_call549 != null)
            return INDY_call549;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call550;
    private static MethodHandle INDY_call550 () throws Throwable {
        if (INDY_call550 != null)
            return INDY_call550;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call551;
    private static MethodHandle INDY_call551 () throws Throwable {
        if (INDY_call551 != null)
            return INDY_call551;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call552;
    private static MethodHandle INDY_call552 () throws Throwable {
        if (INDY_call552 != null)
            return INDY_call552;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call553;
    private static MethodHandle INDY_call553 () throws Throwable {
        if (INDY_call553 != null)
            return INDY_call553;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call554;
    private static MethodHandle INDY_call554 () throws Throwable {
        if (INDY_call554 != null)
            return INDY_call554;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call555;
    private static MethodHandle INDY_call555 () throws Throwable {
        if (INDY_call555 != null)
            return INDY_call555;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call556;
    private static MethodHandle INDY_call556 () throws Throwable {
        if (INDY_call556 != null)
            return INDY_call556;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call557;
    private static MethodHandle INDY_call557 () throws Throwable {
        if (INDY_call557 != null)
            return INDY_call557;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call558;
    private static MethodHandle INDY_call558 () throws Throwable {
        if (INDY_call558 != null)
            return INDY_call558;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call559;
    private static MethodHandle INDY_call559 () throws Throwable {
        if (INDY_call559 != null)
            return INDY_call559;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call560;
    private static MethodHandle INDY_call560 () throws Throwable {
        if (INDY_call560 != null)
            return INDY_call560;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call561;
    private static MethodHandle INDY_call561 () throws Throwable {
        if (INDY_call561 != null)
            return INDY_call561;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call562;
    private static MethodHandle INDY_call562 () throws Throwable {
        if (INDY_call562 != null)
            return INDY_call562;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call563;
    private static MethodHandle INDY_call563 () throws Throwable {
        if (INDY_call563 != null)
            return INDY_call563;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call564;
    private static MethodHandle INDY_call564 () throws Throwable {
        if (INDY_call564 != null)
            return INDY_call564;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call565;
    private static MethodHandle INDY_call565 () throws Throwable {
        if (INDY_call565 != null)
            return INDY_call565;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call566;
    private static MethodHandle INDY_call566 () throws Throwable {
        if (INDY_call566 != null)
            return INDY_call566;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call567;
    private static MethodHandle INDY_call567 () throws Throwable {
        if (INDY_call567 != null)
            return INDY_call567;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call568;
    private static MethodHandle INDY_call568 () throws Throwable {
        if (INDY_call568 != null)
            return INDY_call568;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call569;
    private static MethodHandle INDY_call569 () throws Throwable {
        if (INDY_call569 != null)
            return INDY_call569;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call570;
    private static MethodHandle INDY_call570 () throws Throwable {
        if (INDY_call570 != null)
            return INDY_call570;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call571;
    private static MethodHandle INDY_call571 () throws Throwable {
        if (INDY_call571 != null)
            return INDY_call571;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call572;
    private static MethodHandle INDY_call572 () throws Throwable {
        if (INDY_call572 != null)
            return INDY_call572;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call573;
    private static MethodHandle INDY_call573 () throws Throwable {
        if (INDY_call573 != null)
            return INDY_call573;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call574;
    private static MethodHandle INDY_call574 () throws Throwable {
        if (INDY_call574 != null)
            return INDY_call574;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call575;
    private static MethodHandle INDY_call575 () throws Throwable {
        if (INDY_call575 != null)
            return INDY_call575;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call576;
    private static MethodHandle INDY_call576 () throws Throwable {
        if (INDY_call576 != null)
            return INDY_call576;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call577;
    private static MethodHandle INDY_call577 () throws Throwable {
        if (INDY_call577 != null)
            return INDY_call577;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call578;
    private static MethodHandle INDY_call578 () throws Throwable {
        if (INDY_call578 != null)
            return INDY_call578;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call579;
    private static MethodHandle INDY_call579 () throws Throwable {
        if (INDY_call579 != null)
            return INDY_call579;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call580;
    private static MethodHandle INDY_call580 () throws Throwable {
        if (INDY_call580 != null)
            return INDY_call580;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call581;
    private static MethodHandle INDY_call581 () throws Throwable {
        if (INDY_call581 != null)
            return INDY_call581;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call582;
    private static MethodHandle INDY_call582 () throws Throwable {
        if (INDY_call582 != null)
            return INDY_call582;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call583;
    private static MethodHandle INDY_call583 () throws Throwable {
        if (INDY_call583 != null)
            return INDY_call583;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call584;
    private static MethodHandle INDY_call584 () throws Throwable {
        if (INDY_call584 != null)
            return INDY_call584;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call585;
    private static MethodHandle INDY_call585 () throws Throwable {
        if (INDY_call585 != null)
            return INDY_call585;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call586;
    private static MethodHandle INDY_call586 () throws Throwable {
        if (INDY_call586 != null)
            return INDY_call586;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call587;
    private static MethodHandle INDY_call587 () throws Throwable {
        if (INDY_call587 != null)
            return INDY_call587;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call588;
    private static MethodHandle INDY_call588 () throws Throwable {
        if (INDY_call588 != null)
            return INDY_call588;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call589;
    private static MethodHandle INDY_call589 () throws Throwable {
        if (INDY_call589 != null)
            return INDY_call589;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call590;
    private static MethodHandle INDY_call590 () throws Throwable {
        if (INDY_call590 != null)
            return INDY_call590;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call591;
    private static MethodHandle INDY_call591 () throws Throwable {
        if (INDY_call591 != null)
            return INDY_call591;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call592;
    private static MethodHandle INDY_call592 () throws Throwable {
        if (INDY_call592 != null)
            return INDY_call592;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call593;
    private static MethodHandle INDY_call593 () throws Throwable {
        if (INDY_call593 != null)
            return INDY_call593;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call594;
    private static MethodHandle INDY_call594 () throws Throwable {
        if (INDY_call594 != null)
            return INDY_call594;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call595;
    private static MethodHandle INDY_call595 () throws Throwable {
        if (INDY_call595 != null)
            return INDY_call595;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call596;
    private static MethodHandle INDY_call596 () throws Throwable {
        if (INDY_call596 != null)
            return INDY_call596;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call597;
    private static MethodHandle INDY_call597 () throws Throwable {
        if (INDY_call597 != null)
            return INDY_call597;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call598;
    private static MethodHandle INDY_call598 () throws Throwable {
        if (INDY_call598 != null)
            return INDY_call598;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call599;
    private static MethodHandle INDY_call599 () throws Throwable {
        if (INDY_call599 != null)
            return INDY_call599;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call600;
    private static MethodHandle INDY_call600 () throws Throwable {
        if (INDY_call600 != null)
            return INDY_call600;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call601;
    private static MethodHandle INDY_call601 () throws Throwable {
        if (INDY_call601 != null)
            return INDY_call601;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call602;
    private static MethodHandle INDY_call602 () throws Throwable {
        if (INDY_call602 != null)
            return INDY_call602;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call603;
    private static MethodHandle INDY_call603 () throws Throwable {
        if (INDY_call603 != null)
            return INDY_call603;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call604;
    private static MethodHandle INDY_call604 () throws Throwable {
        if (INDY_call604 != null)
            return INDY_call604;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call605;
    private static MethodHandle INDY_call605 () throws Throwable {
        if (INDY_call605 != null)
            return INDY_call605;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call606;
    private static MethodHandle INDY_call606 () throws Throwable {
        if (INDY_call606 != null)
            return INDY_call606;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call607;
    private static MethodHandle INDY_call607 () throws Throwable {
        if (INDY_call607 != null)
            return INDY_call607;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call608;
    private static MethodHandle INDY_call608 () throws Throwable {
        if (INDY_call608 != null)
            return INDY_call608;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call609;
    private static MethodHandle INDY_call609 () throws Throwable {
        if (INDY_call609 != null)
            return INDY_call609;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call610;
    private static MethodHandle INDY_call610 () throws Throwable {
        if (INDY_call610 != null)
            return INDY_call610;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call611;
    private static MethodHandle INDY_call611 () throws Throwable {
        if (INDY_call611 != null)
            return INDY_call611;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call612;
    private static MethodHandle INDY_call612 () throws Throwable {
        if (INDY_call612 != null)
            return INDY_call612;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call613;
    private static MethodHandle INDY_call613 () throws Throwable {
        if (INDY_call613 != null)
            return INDY_call613;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call614;
    private static MethodHandle INDY_call614 () throws Throwable {
        if (INDY_call614 != null)
            return INDY_call614;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call615;
    private static MethodHandle INDY_call615 () throws Throwable {
        if (INDY_call615 != null)
            return INDY_call615;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call616;
    private static MethodHandle INDY_call616 () throws Throwable {
        if (INDY_call616 != null)
            return INDY_call616;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call617;
    private static MethodHandle INDY_call617 () throws Throwable {
        if (INDY_call617 != null)
            return INDY_call617;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call618;
    private static MethodHandle INDY_call618 () throws Throwable {
        if (INDY_call618 != null)
            return INDY_call618;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call619;
    private static MethodHandle INDY_call619 () throws Throwable {
        if (INDY_call619 != null)
            return INDY_call619;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call620;
    private static MethodHandle INDY_call620 () throws Throwable {
        if (INDY_call620 != null)
            return INDY_call620;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call621;
    private static MethodHandle INDY_call621 () throws Throwable {
        if (INDY_call621 != null)
            return INDY_call621;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call622;
    private static MethodHandle INDY_call622 () throws Throwable {
        if (INDY_call622 != null)
            return INDY_call622;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call623;
    private static MethodHandle INDY_call623 () throws Throwable {
        if (INDY_call623 != null)
            return INDY_call623;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call624;
    private static MethodHandle INDY_call624 () throws Throwable {
        if (INDY_call624 != null)
            return INDY_call624;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call625;
    private static MethodHandle INDY_call625 () throws Throwable {
        if (INDY_call625 != null)
            return INDY_call625;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call626;
    private static MethodHandle INDY_call626 () throws Throwable {
        if (INDY_call626 != null)
            return INDY_call626;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call627;
    private static MethodHandle INDY_call627 () throws Throwable {
        if (INDY_call627 != null)
            return INDY_call627;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call628;
    private static MethodHandle INDY_call628 () throws Throwable {
        if (INDY_call628 != null)
            return INDY_call628;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call629;
    private static MethodHandle INDY_call629 () throws Throwable {
        if (INDY_call629 != null)
            return INDY_call629;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call630;
    private static MethodHandle INDY_call630 () throws Throwable {
        if (INDY_call630 != null)
            return INDY_call630;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call631;
    private static MethodHandle INDY_call631 () throws Throwable {
        if (INDY_call631 != null)
            return INDY_call631;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call632;
    private static MethodHandle INDY_call632 () throws Throwable {
        if (INDY_call632 != null)
            return INDY_call632;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call633;
    private static MethodHandle INDY_call633 () throws Throwable {
        if (INDY_call633 != null)
            return INDY_call633;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call634;
    private static MethodHandle INDY_call634 () throws Throwable {
        if (INDY_call634 != null)
            return INDY_call634;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call635;
    private static MethodHandle INDY_call635 () throws Throwable {
        if (INDY_call635 != null)
            return INDY_call635;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call636;
    private static MethodHandle INDY_call636 () throws Throwable {
        if (INDY_call636 != null)
            return INDY_call636;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call637;
    private static MethodHandle INDY_call637 () throws Throwable {
        if (INDY_call637 != null)
            return INDY_call637;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call638;
    private static MethodHandle INDY_call638 () throws Throwable {
        if (INDY_call638 != null)
            return INDY_call638;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call639;
    private static MethodHandle INDY_call639 () throws Throwable {
        if (INDY_call639 != null)
            return INDY_call639;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call640;
    private static MethodHandle INDY_call640 () throws Throwable {
        if (INDY_call640 != null)
            return INDY_call640;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call641;
    private static MethodHandle INDY_call641 () throws Throwable {
        if (INDY_call641 != null)
            return INDY_call641;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call642;
    private static MethodHandle INDY_call642 () throws Throwable {
        if (INDY_call642 != null)
            return INDY_call642;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call643;
    private static MethodHandle INDY_call643 () throws Throwable {
        if (INDY_call643 != null)
            return INDY_call643;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call644;
    private static MethodHandle INDY_call644 () throws Throwable {
        if (INDY_call644 != null)
            return INDY_call644;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call645;
    private static MethodHandle INDY_call645 () throws Throwable {
        if (INDY_call645 != null)
            return INDY_call645;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call646;
    private static MethodHandle INDY_call646 () throws Throwable {
        if (INDY_call646 != null)
            return INDY_call646;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call647;
    private static MethodHandle INDY_call647 () throws Throwable {
        if (INDY_call647 != null)
            return INDY_call647;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call648;
    private static MethodHandle INDY_call648 () throws Throwable {
        if (INDY_call648 != null)
            return INDY_call648;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call649;
    private static MethodHandle INDY_call649 () throws Throwable {
        if (INDY_call649 != null)
            return INDY_call649;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call650;
    private static MethodHandle INDY_call650 () throws Throwable {
        if (INDY_call650 != null)
            return INDY_call650;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call651;
    private static MethodHandle INDY_call651 () throws Throwable {
        if (INDY_call651 != null)
            return INDY_call651;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call652;
    private static MethodHandle INDY_call652 () throws Throwable {
        if (INDY_call652 != null)
            return INDY_call652;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call653;
    private static MethodHandle INDY_call653 () throws Throwable {
        if (INDY_call653 != null)
            return INDY_call653;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call654;
    private static MethodHandle INDY_call654 () throws Throwable {
        if (INDY_call654 != null)
            return INDY_call654;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call655;
    private static MethodHandle INDY_call655 () throws Throwable {
        if (INDY_call655 != null)
            return INDY_call655;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call656;
    private static MethodHandle INDY_call656 () throws Throwable {
        if (INDY_call656 != null)
            return INDY_call656;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call657;
    private static MethodHandle INDY_call657 () throws Throwable {
        if (INDY_call657 != null)
            return INDY_call657;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call658;
    private static MethodHandle INDY_call658 () throws Throwable {
        if (INDY_call658 != null)
            return INDY_call658;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call659;
    private static MethodHandle INDY_call659 () throws Throwable {
        if (INDY_call659 != null)
            return INDY_call659;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call660;
    private static MethodHandle INDY_call660 () throws Throwable {
        if (INDY_call660 != null)
            return INDY_call660;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call661;
    private static MethodHandle INDY_call661 () throws Throwable {
        if (INDY_call661 != null)
            return INDY_call661;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call662;
    private static MethodHandle INDY_call662 () throws Throwable {
        if (INDY_call662 != null)
            return INDY_call662;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call663;
    private static MethodHandle INDY_call663 () throws Throwable {
        if (INDY_call663 != null)
            return INDY_call663;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call664;
    private static MethodHandle INDY_call664 () throws Throwable {
        if (INDY_call664 != null)
            return INDY_call664;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call665;
    private static MethodHandle INDY_call665 () throws Throwable {
        if (INDY_call665 != null)
            return INDY_call665;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call666;
    private static MethodHandle INDY_call666 () throws Throwable {
        if (INDY_call666 != null)
            return INDY_call666;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call667;
    private static MethodHandle INDY_call667 () throws Throwable {
        if (INDY_call667 != null)
            return INDY_call667;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call668;
    private static MethodHandle INDY_call668 () throws Throwable {
        if (INDY_call668 != null)
            return INDY_call668;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call669;
    private static MethodHandle INDY_call669 () throws Throwable {
        if (INDY_call669 != null)
            return INDY_call669;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call670;
    private static MethodHandle INDY_call670 () throws Throwable {
        if (INDY_call670 != null)
            return INDY_call670;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call671;
    private static MethodHandle INDY_call671 () throws Throwable {
        if (INDY_call671 != null)
            return INDY_call671;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call672;
    private static MethodHandle INDY_call672 () throws Throwable {
        if (INDY_call672 != null)
            return INDY_call672;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call673;
    private static MethodHandle INDY_call673 () throws Throwable {
        if (INDY_call673 != null)
            return INDY_call673;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call674;
    private static MethodHandle INDY_call674 () throws Throwable {
        if (INDY_call674 != null)
            return INDY_call674;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call675;
    private static MethodHandle INDY_call675 () throws Throwable {
        if (INDY_call675 != null)
            return INDY_call675;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call676;
    private static MethodHandle INDY_call676 () throws Throwable {
        if (INDY_call676 != null)
            return INDY_call676;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call677;
    private static MethodHandle INDY_call677 () throws Throwable {
        if (INDY_call677 != null)
            return INDY_call677;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call678;
    private static MethodHandle INDY_call678 () throws Throwable {
        if (INDY_call678 != null)
            return INDY_call678;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call679;
    private static MethodHandle INDY_call679 () throws Throwable {
        if (INDY_call679 != null)
            return INDY_call679;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call680;
    private static MethodHandle INDY_call680 () throws Throwable {
        if (INDY_call680 != null)
            return INDY_call680;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call681;
    private static MethodHandle INDY_call681 () throws Throwable {
        if (INDY_call681 != null)
            return INDY_call681;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call682;
    private static MethodHandle INDY_call682 () throws Throwable {
        if (INDY_call682 != null)
            return INDY_call682;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call683;
    private static MethodHandle INDY_call683 () throws Throwable {
        if (INDY_call683 != null)
            return INDY_call683;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call684;
    private static MethodHandle INDY_call684 () throws Throwable {
        if (INDY_call684 != null)
            return INDY_call684;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call685;
    private static MethodHandle INDY_call685 () throws Throwable {
        if (INDY_call685 != null)
            return INDY_call685;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call686;
    private static MethodHandle INDY_call686 () throws Throwable {
        if (INDY_call686 != null)
            return INDY_call686;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call687;
    private static MethodHandle INDY_call687 () throws Throwable {
        if (INDY_call687 != null)
            return INDY_call687;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call688;
    private static MethodHandle INDY_call688 () throws Throwable {
        if (INDY_call688 != null)
            return INDY_call688;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call689;
    private static MethodHandle INDY_call689 () throws Throwable {
        if (INDY_call689 != null)
            return INDY_call689;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call690;
    private static MethodHandle INDY_call690 () throws Throwable {
        if (INDY_call690 != null)
            return INDY_call690;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call691;
    private static MethodHandle INDY_call691 () throws Throwable {
        if (INDY_call691 != null)
            return INDY_call691;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call692;
    private static MethodHandle INDY_call692 () throws Throwable {
        if (INDY_call692 != null)
            return INDY_call692;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call693;
    private static MethodHandle INDY_call693 () throws Throwable {
        if (INDY_call693 != null)
            return INDY_call693;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call694;
    private static MethodHandle INDY_call694 () throws Throwable {
        if (INDY_call694 != null)
            return INDY_call694;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call695;
    private static MethodHandle INDY_call695 () throws Throwable {
        if (INDY_call695 != null)
            return INDY_call695;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call696;
    private static MethodHandle INDY_call696 () throws Throwable {
        if (INDY_call696 != null)
            return INDY_call696;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call697;
    private static MethodHandle INDY_call697 () throws Throwable {
        if (INDY_call697 != null)
            return INDY_call697;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call698;
    private static MethodHandle INDY_call698 () throws Throwable {
        if (INDY_call698 != null)
            return INDY_call698;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call699;
    private static MethodHandle INDY_call699 () throws Throwable {
        if (INDY_call699 != null)
            return INDY_call699;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call700;
    private static MethodHandle INDY_call700 () throws Throwable {
        if (INDY_call700 != null)
            return INDY_call700;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call701;
    private static MethodHandle INDY_call701 () throws Throwable {
        if (INDY_call701 != null)
            return INDY_call701;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call702;
    private static MethodHandle INDY_call702 () throws Throwable {
        if (INDY_call702 != null)
            return INDY_call702;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call703;
    private static MethodHandle INDY_call703 () throws Throwable {
        if (INDY_call703 != null)
            return INDY_call703;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call704;
    private static MethodHandle INDY_call704 () throws Throwable {
        if (INDY_call704 != null)
            return INDY_call704;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call705;
    private static MethodHandle INDY_call705 () throws Throwable {
        if (INDY_call705 != null)
            return INDY_call705;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call706;
    private static MethodHandle INDY_call706 () throws Throwable {
        if (INDY_call706 != null)
            return INDY_call706;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call707;
    private static MethodHandle INDY_call707 () throws Throwable {
        if (INDY_call707 != null)
            return INDY_call707;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call708;
    private static MethodHandle INDY_call708 () throws Throwable {
        if (INDY_call708 != null)
            return INDY_call708;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call709;
    private static MethodHandle INDY_call709 () throws Throwable {
        if (INDY_call709 != null)
            return INDY_call709;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call710;
    private static MethodHandle INDY_call710 () throws Throwable {
        if (INDY_call710 != null)
            return INDY_call710;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call711;
    private static MethodHandle INDY_call711 () throws Throwable {
        if (INDY_call711 != null)
            return INDY_call711;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call712;
    private static MethodHandle INDY_call712 () throws Throwable {
        if (INDY_call712 != null)
            return INDY_call712;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call713;
    private static MethodHandle INDY_call713 () throws Throwable {
        if (INDY_call713 != null)
            return INDY_call713;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call714;
    private static MethodHandle INDY_call714 () throws Throwable {
        if (INDY_call714 != null)
            return INDY_call714;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call715;
    private static MethodHandle INDY_call715 () throws Throwable {
        if (INDY_call715 != null)
            return INDY_call715;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call716;
    private static MethodHandle INDY_call716 () throws Throwable {
        if (INDY_call716 != null)
            return INDY_call716;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call717;
    private static MethodHandle INDY_call717 () throws Throwable {
        if (INDY_call717 != null)
            return INDY_call717;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call718;
    private static MethodHandle INDY_call718 () throws Throwable {
        if (INDY_call718 != null)
            return INDY_call718;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call719;
    private static MethodHandle INDY_call719 () throws Throwable {
        if (INDY_call719 != null)
            return INDY_call719;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call720;
    private static MethodHandle INDY_call720 () throws Throwable {
        if (INDY_call720 != null)
            return INDY_call720;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call721;
    private static MethodHandle INDY_call721 () throws Throwable {
        if (INDY_call721 != null)
            return INDY_call721;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call722;
    private static MethodHandle INDY_call722 () throws Throwable {
        if (INDY_call722 != null)
            return INDY_call722;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call723;
    private static MethodHandle INDY_call723 () throws Throwable {
        if (INDY_call723 != null)
            return INDY_call723;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call724;
    private static MethodHandle INDY_call724 () throws Throwable {
        if (INDY_call724 != null)
            return INDY_call724;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call725;
    private static MethodHandle INDY_call725 () throws Throwable {
        if (INDY_call725 != null)
            return INDY_call725;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call726;
    private static MethodHandle INDY_call726 () throws Throwable {
        if (INDY_call726 != null)
            return INDY_call726;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call727;
    private static MethodHandle INDY_call727 () throws Throwable {
        if (INDY_call727 != null)
            return INDY_call727;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call728;
    private static MethodHandle INDY_call728 () throws Throwable {
        if (INDY_call728 != null)
            return INDY_call728;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call729;
    private static MethodHandle INDY_call729 () throws Throwable {
        if (INDY_call729 != null)
            return INDY_call729;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call730;
    private static MethodHandle INDY_call730 () throws Throwable {
        if (INDY_call730 != null)
            return INDY_call730;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call731;
    private static MethodHandle INDY_call731 () throws Throwable {
        if (INDY_call731 != null)
            return INDY_call731;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call732;
    private static MethodHandle INDY_call732 () throws Throwable {
        if (INDY_call732 != null)
            return INDY_call732;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call733;
    private static MethodHandle INDY_call733 () throws Throwable {
        if (INDY_call733 != null)
            return INDY_call733;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call734;
    private static MethodHandle INDY_call734 () throws Throwable {
        if (INDY_call734 != null)
            return INDY_call734;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call735;
    private static MethodHandle INDY_call735 () throws Throwable {
        if (INDY_call735 != null)
            return INDY_call735;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call736;
    private static MethodHandle INDY_call736 () throws Throwable {
        if (INDY_call736 != null)
            return INDY_call736;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call737;
    private static MethodHandle INDY_call737 () throws Throwable {
        if (INDY_call737 != null)
            return INDY_call737;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call738;
    private static MethodHandle INDY_call738 () throws Throwable {
        if (INDY_call738 != null)
            return INDY_call738;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call739;
    private static MethodHandle INDY_call739 () throws Throwable {
        if (INDY_call739 != null)
            return INDY_call739;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call740;
    private static MethodHandle INDY_call740 () throws Throwable {
        if (INDY_call740 != null)
            return INDY_call740;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call741;
    private static MethodHandle INDY_call741 () throws Throwable {
        if (INDY_call741 != null)
            return INDY_call741;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call742;
    private static MethodHandle INDY_call742 () throws Throwable {
        if (INDY_call742 != null)
            return INDY_call742;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call743;
    private static MethodHandle INDY_call743 () throws Throwable {
        if (INDY_call743 != null)
            return INDY_call743;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call744;
    private static MethodHandle INDY_call744 () throws Throwable {
        if (INDY_call744 != null)
            return INDY_call744;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call745;
    private static MethodHandle INDY_call745 () throws Throwable {
        if (INDY_call745 != null)
            return INDY_call745;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call746;
    private static MethodHandle INDY_call746 () throws Throwable {
        if (INDY_call746 != null)
            return INDY_call746;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call747;
    private static MethodHandle INDY_call747 () throws Throwable {
        if (INDY_call747 != null)
            return INDY_call747;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call748;
    private static MethodHandle INDY_call748 () throws Throwable {
        if (INDY_call748 != null)
            return INDY_call748;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call749;
    private static MethodHandle INDY_call749 () throws Throwable {
        if (INDY_call749 != null)
            return INDY_call749;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call750;
    private static MethodHandle INDY_call750 () throws Throwable {
        if (INDY_call750 != null)
            return INDY_call750;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call751;
    private static MethodHandle INDY_call751 () throws Throwable {
        if (INDY_call751 != null)
            return INDY_call751;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call752;
    private static MethodHandle INDY_call752 () throws Throwable {
        if (INDY_call752 != null)
            return INDY_call752;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call753;
    private static MethodHandle INDY_call753 () throws Throwable {
        if (INDY_call753 != null)
            return INDY_call753;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call754;
    private static MethodHandle INDY_call754 () throws Throwable {
        if (INDY_call754 != null)
            return INDY_call754;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call755;
    private static MethodHandle INDY_call755 () throws Throwable {
        if (INDY_call755 != null)
            return INDY_call755;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call756;
    private static MethodHandle INDY_call756 () throws Throwable {
        if (INDY_call756 != null)
            return INDY_call756;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call757;
    private static MethodHandle INDY_call757 () throws Throwable {
        if (INDY_call757 != null)
            return INDY_call757;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call758;
    private static MethodHandle INDY_call758 () throws Throwable {
        if (INDY_call758 != null)
            return INDY_call758;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call759;
    private static MethodHandle INDY_call759 () throws Throwable {
        if (INDY_call759 != null)
            return INDY_call759;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call760;
    private static MethodHandle INDY_call760 () throws Throwable {
        if (INDY_call760 != null)
            return INDY_call760;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call761;
    private static MethodHandle INDY_call761 () throws Throwable {
        if (INDY_call761 != null)
            return INDY_call761;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call762;
    private static MethodHandle INDY_call762 () throws Throwable {
        if (INDY_call762 != null)
            return INDY_call762;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call763;
    private static MethodHandle INDY_call763 () throws Throwable {
        if (INDY_call763 != null)
            return INDY_call763;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call764;
    private static MethodHandle INDY_call764 () throws Throwable {
        if (INDY_call764 != null)
            return INDY_call764;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call765;
    private static MethodHandle INDY_call765 () throws Throwable {
        if (INDY_call765 != null)
            return INDY_call765;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call766;
    private static MethodHandle INDY_call766 () throws Throwable {
        if (INDY_call766 != null)
            return INDY_call766;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call767;
    private static MethodHandle INDY_call767 () throws Throwable {
        if (INDY_call767 != null)
            return INDY_call767;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call768;
    private static MethodHandle INDY_call768 () throws Throwable {
        if (INDY_call768 != null)
            return INDY_call768;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call769;
    private static MethodHandle INDY_call769 () throws Throwable {
        if (INDY_call769 != null)
            return INDY_call769;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call770;
    private static MethodHandle INDY_call770 () throws Throwable {
        if (INDY_call770 != null)
            return INDY_call770;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call771;
    private static MethodHandle INDY_call771 () throws Throwable {
        if (INDY_call771 != null)
            return INDY_call771;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call772;
    private static MethodHandle INDY_call772 () throws Throwable {
        if (INDY_call772 != null)
            return INDY_call772;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call773;
    private static MethodHandle INDY_call773 () throws Throwable {
        if (INDY_call773 != null)
            return INDY_call773;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call774;
    private static MethodHandle INDY_call774 () throws Throwable {
        if (INDY_call774 != null)
            return INDY_call774;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call775;
    private static MethodHandle INDY_call775 () throws Throwable {
        if (INDY_call775 != null)
            return INDY_call775;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call776;
    private static MethodHandle INDY_call776 () throws Throwable {
        if (INDY_call776 != null)
            return INDY_call776;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call777;
    private static MethodHandle INDY_call777 () throws Throwable {
        if (INDY_call777 != null)
            return INDY_call777;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call778;
    private static MethodHandle INDY_call778 () throws Throwable {
        if (INDY_call778 != null)
            return INDY_call778;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call779;
    private static MethodHandle INDY_call779 () throws Throwable {
        if (INDY_call779 != null)
            return INDY_call779;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call780;
    private static MethodHandle INDY_call780 () throws Throwable {
        if (INDY_call780 != null)
            return INDY_call780;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call781;
    private static MethodHandle INDY_call781 () throws Throwable {
        if (INDY_call781 != null)
            return INDY_call781;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call782;
    private static MethodHandle INDY_call782 () throws Throwable {
        if (INDY_call782 != null)
            return INDY_call782;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call783;
    private static MethodHandle INDY_call783 () throws Throwable {
        if (INDY_call783 != null)
            return INDY_call783;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call784;
    private static MethodHandle INDY_call784 () throws Throwable {
        if (INDY_call784 != null)
            return INDY_call784;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call785;
    private static MethodHandle INDY_call785 () throws Throwable {
        if (INDY_call785 != null)
            return INDY_call785;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call786;
    private static MethodHandle INDY_call786 () throws Throwable {
        if (INDY_call786 != null)
            return INDY_call786;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call787;
    private static MethodHandle INDY_call787 () throws Throwable {
        if (INDY_call787 != null)
            return INDY_call787;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call788;
    private static MethodHandle INDY_call788 () throws Throwable {
        if (INDY_call788 != null)
            return INDY_call788;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call789;
    private static MethodHandle INDY_call789 () throws Throwable {
        if (INDY_call789 != null)
            return INDY_call789;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call790;
    private static MethodHandle INDY_call790 () throws Throwable {
        if (INDY_call790 != null)
            return INDY_call790;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call791;
    private static MethodHandle INDY_call791 () throws Throwable {
        if (INDY_call791 != null)
            return INDY_call791;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call792;
    private static MethodHandle INDY_call792 () throws Throwable {
        if (INDY_call792 != null)
            return INDY_call792;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call793;
    private static MethodHandle INDY_call793 () throws Throwable {
        if (INDY_call793 != null)
            return INDY_call793;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call794;
    private static MethodHandle INDY_call794 () throws Throwable {
        if (INDY_call794 != null)
            return INDY_call794;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call795;
    private static MethodHandle INDY_call795 () throws Throwable {
        if (INDY_call795 != null)
            return INDY_call795;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call796;
    private static MethodHandle INDY_call796 () throws Throwable {
        if (INDY_call796 != null)
            return INDY_call796;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call797;
    private static MethodHandle INDY_call797 () throws Throwable {
        if (INDY_call797 != null)
            return INDY_call797;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call798;
    private static MethodHandle INDY_call798 () throws Throwable {
        if (INDY_call798 != null)
            return INDY_call798;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call799;
    private static MethodHandle INDY_call799 () throws Throwable {
        if (INDY_call799 != null)
            return INDY_call799;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call800;
    private static MethodHandle INDY_call800 () throws Throwable {
        if (INDY_call800 != null)
            return INDY_call800;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call801;
    private static MethodHandle INDY_call801 () throws Throwable {
        if (INDY_call801 != null)
            return INDY_call801;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call802;
    private static MethodHandle INDY_call802 () throws Throwable {
        if (INDY_call802 != null)
            return INDY_call802;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call803;
    private static MethodHandle INDY_call803 () throws Throwable {
        if (INDY_call803 != null)
            return INDY_call803;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call804;
    private static MethodHandle INDY_call804 () throws Throwable {
        if (INDY_call804 != null)
            return INDY_call804;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call805;
    private static MethodHandle INDY_call805 () throws Throwable {
        if (INDY_call805 != null)
            return INDY_call805;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call806;
    private static MethodHandle INDY_call806 () throws Throwable {
        if (INDY_call806 != null)
            return INDY_call806;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call807;
    private static MethodHandle INDY_call807 () throws Throwable {
        if (INDY_call807 != null)
            return INDY_call807;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call808;
    private static MethodHandle INDY_call808 () throws Throwable {
        if (INDY_call808 != null)
            return INDY_call808;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call809;
    private static MethodHandle INDY_call809 () throws Throwable {
        if (INDY_call809 != null)
            return INDY_call809;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call810;
    private static MethodHandle INDY_call810 () throws Throwable {
        if (INDY_call810 != null)
            return INDY_call810;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call811;
    private static MethodHandle INDY_call811 () throws Throwable {
        if (INDY_call811 != null)
            return INDY_call811;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call812;
    private static MethodHandle INDY_call812 () throws Throwable {
        if (INDY_call812 != null)
            return INDY_call812;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call813;
    private static MethodHandle INDY_call813 () throws Throwable {
        if (INDY_call813 != null)
            return INDY_call813;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call814;
    private static MethodHandle INDY_call814 () throws Throwable {
        if (INDY_call814 != null)
            return INDY_call814;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call815;
    private static MethodHandle INDY_call815 () throws Throwable {
        if (INDY_call815 != null)
            return INDY_call815;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call816;
    private static MethodHandle INDY_call816 () throws Throwable {
        if (INDY_call816 != null)
            return INDY_call816;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call817;
    private static MethodHandle INDY_call817 () throws Throwable {
        if (INDY_call817 != null)
            return INDY_call817;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call818;
    private static MethodHandle INDY_call818 () throws Throwable {
        if (INDY_call818 != null)
            return INDY_call818;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call819;
    private static MethodHandle INDY_call819 () throws Throwable {
        if (INDY_call819 != null)
            return INDY_call819;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call820;
    private static MethodHandle INDY_call820 () throws Throwable {
        if (INDY_call820 != null)
            return INDY_call820;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call821;
    private static MethodHandle INDY_call821 () throws Throwable {
        if (INDY_call821 != null)
            return INDY_call821;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call822;
    private static MethodHandle INDY_call822 () throws Throwable {
        if (INDY_call822 != null)
            return INDY_call822;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call823;
    private static MethodHandle INDY_call823 () throws Throwable {
        if (INDY_call823 != null)
            return INDY_call823;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call824;
    private static MethodHandle INDY_call824 () throws Throwable {
        if (INDY_call824 != null)
            return INDY_call824;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call825;
    private static MethodHandle INDY_call825 () throws Throwable {
        if (INDY_call825 != null)
            return INDY_call825;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call826;
    private static MethodHandle INDY_call826 () throws Throwable {
        if (INDY_call826 != null)
            return INDY_call826;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call827;
    private static MethodHandle INDY_call827 () throws Throwable {
        if (INDY_call827 != null)
            return INDY_call827;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call828;
    private static MethodHandle INDY_call828 () throws Throwable {
        if (INDY_call828 != null)
            return INDY_call828;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call829;
    private static MethodHandle INDY_call829 () throws Throwable {
        if (INDY_call829 != null)
            return INDY_call829;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call830;
    private static MethodHandle INDY_call830 () throws Throwable {
        if (INDY_call830 != null)
            return INDY_call830;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call831;
    private static MethodHandle INDY_call831 () throws Throwable {
        if (INDY_call831 != null)
            return INDY_call831;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call832;
    private static MethodHandle INDY_call832 () throws Throwable {
        if (INDY_call832 != null)
            return INDY_call832;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call833;
    private static MethodHandle INDY_call833 () throws Throwable {
        if (INDY_call833 != null)
            return INDY_call833;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call834;
    private static MethodHandle INDY_call834 () throws Throwable {
        if (INDY_call834 != null)
            return INDY_call834;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call835;
    private static MethodHandle INDY_call835 () throws Throwable {
        if (INDY_call835 != null)
            return INDY_call835;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call836;
    private static MethodHandle INDY_call836 () throws Throwable {
        if (INDY_call836 != null)
            return INDY_call836;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call837;
    private static MethodHandle INDY_call837 () throws Throwable {
        if (INDY_call837 != null)
            return INDY_call837;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call838;
    private static MethodHandle INDY_call838 () throws Throwable {
        if (INDY_call838 != null)
            return INDY_call838;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call839;
    private static MethodHandle INDY_call839 () throws Throwable {
        if (INDY_call839 != null)
            return INDY_call839;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call840;
    private static MethodHandle INDY_call840 () throws Throwable {
        if (INDY_call840 != null)
            return INDY_call840;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call841;
    private static MethodHandle INDY_call841 () throws Throwable {
        if (INDY_call841 != null)
            return INDY_call841;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call842;
    private static MethodHandle INDY_call842 () throws Throwable {
        if (INDY_call842 != null)
            return INDY_call842;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call843;
    private static MethodHandle INDY_call843 () throws Throwable {
        if (INDY_call843 != null)
            return INDY_call843;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call844;
    private static MethodHandle INDY_call844 () throws Throwable {
        if (INDY_call844 != null)
            return INDY_call844;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call845;
    private static MethodHandle INDY_call845 () throws Throwable {
        if (INDY_call845 != null)
            return INDY_call845;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call846;
    private static MethodHandle INDY_call846 () throws Throwable {
        if (INDY_call846 != null)
            return INDY_call846;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call847;
    private static MethodHandle INDY_call847 () throws Throwable {
        if (INDY_call847 != null)
            return INDY_call847;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call848;
    private static MethodHandle INDY_call848 () throws Throwable {
        if (INDY_call848 != null)
            return INDY_call848;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call849;
    private static MethodHandle INDY_call849 () throws Throwable {
        if (INDY_call849 != null)
            return INDY_call849;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call850;
    private static MethodHandle INDY_call850 () throws Throwable {
        if (INDY_call850 != null)
            return INDY_call850;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call851;
    private static MethodHandle INDY_call851 () throws Throwable {
        if (INDY_call851 != null)
            return INDY_call851;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call852;
    private static MethodHandle INDY_call852 () throws Throwable {
        if (INDY_call852 != null)
            return INDY_call852;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call853;
    private static MethodHandle INDY_call853 () throws Throwable {
        if (INDY_call853 != null)
            return INDY_call853;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call854;
    private static MethodHandle INDY_call854 () throws Throwable {
        if (INDY_call854 != null)
            return INDY_call854;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call855;
    private static MethodHandle INDY_call855 () throws Throwable {
        if (INDY_call855 != null)
            return INDY_call855;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call856;
    private static MethodHandle INDY_call856 () throws Throwable {
        if (INDY_call856 != null)
            return INDY_call856;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call857;
    private static MethodHandle INDY_call857 () throws Throwable {
        if (INDY_call857 != null)
            return INDY_call857;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call858;
    private static MethodHandle INDY_call858 () throws Throwable {
        if (INDY_call858 != null)
            return INDY_call858;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call859;
    private static MethodHandle INDY_call859 () throws Throwable {
        if (INDY_call859 != null)
            return INDY_call859;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call860;
    private static MethodHandle INDY_call860 () throws Throwable {
        if (INDY_call860 != null)
            return INDY_call860;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call861;
    private static MethodHandle INDY_call861 () throws Throwable {
        if (INDY_call861 != null)
            return INDY_call861;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call862;
    private static MethodHandle INDY_call862 () throws Throwable {
        if (INDY_call862 != null)
            return INDY_call862;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call863;
    private static MethodHandle INDY_call863 () throws Throwable {
        if (INDY_call863 != null)
            return INDY_call863;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call864;
    private static MethodHandle INDY_call864 () throws Throwable {
        if (INDY_call864 != null)
            return INDY_call864;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call865;
    private static MethodHandle INDY_call865 () throws Throwable {
        if (INDY_call865 != null)
            return INDY_call865;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call866;
    private static MethodHandle INDY_call866 () throws Throwable {
        if (INDY_call866 != null)
            return INDY_call866;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call867;
    private static MethodHandle INDY_call867 () throws Throwable {
        if (INDY_call867 != null)
            return INDY_call867;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call868;
    private static MethodHandle INDY_call868 () throws Throwable {
        if (INDY_call868 != null)
            return INDY_call868;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call869;
    private static MethodHandle INDY_call869 () throws Throwable {
        if (INDY_call869 != null)
            return INDY_call869;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call870;
    private static MethodHandle INDY_call870 () throws Throwable {
        if (INDY_call870 != null)
            return INDY_call870;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call871;
    private static MethodHandle INDY_call871 () throws Throwable {
        if (INDY_call871 != null)
            return INDY_call871;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call872;
    private static MethodHandle INDY_call872 () throws Throwable {
        if (INDY_call872 != null)
            return INDY_call872;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call873;
    private static MethodHandle INDY_call873 () throws Throwable {
        if (INDY_call873 != null)
            return INDY_call873;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call874;
    private static MethodHandle INDY_call874 () throws Throwable {
        if (INDY_call874 != null)
            return INDY_call874;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call875;
    private static MethodHandle INDY_call875 () throws Throwable {
        if (INDY_call875 != null)
            return INDY_call875;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call876;
    private static MethodHandle INDY_call876 () throws Throwable {
        if (INDY_call876 != null)
            return INDY_call876;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call877;
    private static MethodHandle INDY_call877 () throws Throwable {
        if (INDY_call877 != null)
            return INDY_call877;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call878;
    private static MethodHandle INDY_call878 () throws Throwable {
        if (INDY_call878 != null)
            return INDY_call878;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call879;
    private static MethodHandle INDY_call879 () throws Throwable {
        if (INDY_call879 != null)
            return INDY_call879;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call880;
    private static MethodHandle INDY_call880 () throws Throwable {
        if (INDY_call880 != null)
            return INDY_call880;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call881;
    private static MethodHandle INDY_call881 () throws Throwable {
        if (INDY_call881 != null)
            return INDY_call881;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call882;
    private static MethodHandle INDY_call882 () throws Throwable {
        if (INDY_call882 != null)
            return INDY_call882;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call883;
    private static MethodHandle INDY_call883 () throws Throwable {
        if (INDY_call883 != null)
            return INDY_call883;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call884;
    private static MethodHandle INDY_call884 () throws Throwable {
        if (INDY_call884 != null)
            return INDY_call884;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call885;
    private static MethodHandle INDY_call885 () throws Throwable {
        if (INDY_call885 != null)
            return INDY_call885;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call886;
    private static MethodHandle INDY_call886 () throws Throwable {
        if (INDY_call886 != null)
            return INDY_call886;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call887;
    private static MethodHandle INDY_call887 () throws Throwable {
        if (INDY_call887 != null)
            return INDY_call887;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call888;
    private static MethodHandle INDY_call888 () throws Throwable {
        if (INDY_call888 != null)
            return INDY_call888;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call889;
    private static MethodHandle INDY_call889 () throws Throwable {
        if (INDY_call889 != null)
            return INDY_call889;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call890;
    private static MethodHandle INDY_call890 () throws Throwable {
        if (INDY_call890 != null)
            return INDY_call890;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call891;
    private static MethodHandle INDY_call891 () throws Throwable {
        if (INDY_call891 != null)
            return INDY_call891;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call892;
    private static MethodHandle INDY_call892 () throws Throwable {
        if (INDY_call892 != null)
            return INDY_call892;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call893;
    private static MethodHandle INDY_call893 () throws Throwable {
        if (INDY_call893 != null)
            return INDY_call893;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call894;
    private static MethodHandle INDY_call894 () throws Throwable {
        if (INDY_call894 != null)
            return INDY_call894;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call895;
    private static MethodHandle INDY_call895 () throws Throwable {
        if (INDY_call895 != null)
            return INDY_call895;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call896;
    private static MethodHandle INDY_call896 () throws Throwable {
        if (INDY_call896 != null)
            return INDY_call896;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call897;
    private static MethodHandle INDY_call897 () throws Throwable {
        if (INDY_call897 != null)
            return INDY_call897;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call898;
    private static MethodHandle INDY_call898 () throws Throwable {
        if (INDY_call898 != null)
            return INDY_call898;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call899;
    private static MethodHandle INDY_call899 () throws Throwable {
        if (INDY_call899 != null)
            return INDY_call899;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call900;
    private static MethodHandle INDY_call900 () throws Throwable {
        if (INDY_call900 != null)
            return INDY_call900;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call901;
    private static MethodHandle INDY_call901 () throws Throwable {
        if (INDY_call901 != null)
            return INDY_call901;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call902;
    private static MethodHandle INDY_call902 () throws Throwable {
        if (INDY_call902 != null)
            return INDY_call902;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call903;
    private static MethodHandle INDY_call903 () throws Throwable {
        if (INDY_call903 != null)
            return INDY_call903;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call904;
    private static MethodHandle INDY_call904 () throws Throwable {
        if (INDY_call904 != null)
            return INDY_call904;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call905;
    private static MethodHandle INDY_call905 () throws Throwable {
        if (INDY_call905 != null)
            return INDY_call905;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call906;
    private static MethodHandle INDY_call906 () throws Throwable {
        if (INDY_call906 != null)
            return INDY_call906;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call907;
    private static MethodHandle INDY_call907 () throws Throwable {
        if (INDY_call907 != null)
            return INDY_call907;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call908;
    private static MethodHandle INDY_call908 () throws Throwable {
        if (INDY_call908 != null)
            return INDY_call908;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call909;
    private static MethodHandle INDY_call909 () throws Throwable {
        if (INDY_call909 != null)
            return INDY_call909;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call910;
    private static MethodHandle INDY_call910 () throws Throwable {
        if (INDY_call910 != null)
            return INDY_call910;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call911;
    private static MethodHandle INDY_call911 () throws Throwable {
        if (INDY_call911 != null)
            return INDY_call911;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call912;
    private static MethodHandle INDY_call912 () throws Throwable {
        if (INDY_call912 != null)
            return INDY_call912;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call913;
    private static MethodHandle INDY_call913 () throws Throwable {
        if (INDY_call913 != null)
            return INDY_call913;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call914;
    private static MethodHandle INDY_call914 () throws Throwable {
        if (INDY_call914 != null)
            return INDY_call914;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call915;
    private static MethodHandle INDY_call915 () throws Throwable {
        if (INDY_call915 != null)
            return INDY_call915;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call916;
    private static MethodHandle INDY_call916 () throws Throwable {
        if (INDY_call916 != null)
            return INDY_call916;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call917;
    private static MethodHandle INDY_call917 () throws Throwable {
        if (INDY_call917 != null)
            return INDY_call917;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call918;
    private static MethodHandle INDY_call918 () throws Throwable {
        if (INDY_call918 != null)
            return INDY_call918;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call919;
    private static MethodHandle INDY_call919 () throws Throwable {
        if (INDY_call919 != null)
            return INDY_call919;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call920;
    private static MethodHandle INDY_call920 () throws Throwable {
        if (INDY_call920 != null)
            return INDY_call920;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call921;
    private static MethodHandle INDY_call921 () throws Throwable {
        if (INDY_call921 != null)
            return INDY_call921;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call922;
    private static MethodHandle INDY_call922 () throws Throwable {
        if (INDY_call922 != null)
            return INDY_call922;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call923;
    private static MethodHandle INDY_call923 () throws Throwable {
        if (INDY_call923 != null)
            return INDY_call923;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call924;
    private static MethodHandle INDY_call924 () throws Throwable {
        if (INDY_call924 != null)
            return INDY_call924;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call925;
    private static MethodHandle INDY_call925 () throws Throwable {
        if (INDY_call925 != null)
            return INDY_call925;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call926;
    private static MethodHandle INDY_call926 () throws Throwable {
        if (INDY_call926 != null)
            return INDY_call926;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call927;
    private static MethodHandle INDY_call927 () throws Throwable {
        if (INDY_call927 != null)
            return INDY_call927;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call928;
    private static MethodHandle INDY_call928 () throws Throwable {
        if (INDY_call928 != null)
            return INDY_call928;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call929;
    private static MethodHandle INDY_call929 () throws Throwable {
        if (INDY_call929 != null)
            return INDY_call929;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call930;
    private static MethodHandle INDY_call930 () throws Throwable {
        if (INDY_call930 != null)
            return INDY_call930;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call931;
    private static MethodHandle INDY_call931 () throws Throwable {
        if (INDY_call931 != null)
            return INDY_call931;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call932;
    private static MethodHandle INDY_call932 () throws Throwable {
        if (INDY_call932 != null)
            return INDY_call932;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call933;
    private static MethodHandle INDY_call933 () throws Throwable {
        if (INDY_call933 != null)
            return INDY_call933;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call934;
    private static MethodHandle INDY_call934 () throws Throwable {
        if (INDY_call934 != null)
            return INDY_call934;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call935;
    private static MethodHandle INDY_call935 () throws Throwable {
        if (INDY_call935 != null)
            return INDY_call935;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call936;
    private static MethodHandle INDY_call936 () throws Throwable {
        if (INDY_call936 != null)
            return INDY_call936;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call937;
    private static MethodHandle INDY_call937 () throws Throwable {
        if (INDY_call937 != null)
            return INDY_call937;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call938;
    private static MethodHandle INDY_call938 () throws Throwable {
        if (INDY_call938 != null)
            return INDY_call938;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call939;
    private static MethodHandle INDY_call939 () throws Throwable {
        if (INDY_call939 != null)
            return INDY_call939;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call940;
    private static MethodHandle INDY_call940 () throws Throwable {
        if (INDY_call940 != null)
            return INDY_call940;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call941;
    private static MethodHandle INDY_call941 () throws Throwable {
        if (INDY_call941 != null)
            return INDY_call941;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call942;
    private static MethodHandle INDY_call942 () throws Throwable {
        if (INDY_call942 != null)
            return INDY_call942;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call943;
    private static MethodHandle INDY_call943 () throws Throwable {
        if (INDY_call943 != null)
            return INDY_call943;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call944;
    private static MethodHandle INDY_call944 () throws Throwable {
        if (INDY_call944 != null)
            return INDY_call944;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call945;
    private static MethodHandle INDY_call945 () throws Throwable {
        if (INDY_call945 != null)
            return INDY_call945;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call946;
    private static MethodHandle INDY_call946 () throws Throwable {
        if (INDY_call946 != null)
            return INDY_call946;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call947;
    private static MethodHandle INDY_call947 () throws Throwable {
        if (INDY_call947 != null)
            return INDY_call947;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call948;
    private static MethodHandle INDY_call948 () throws Throwable {
        if (INDY_call948 != null)
            return INDY_call948;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call949;
    private static MethodHandle INDY_call949 () throws Throwable {
        if (INDY_call949 != null)
            return INDY_call949;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call950;
    private static MethodHandle INDY_call950 () throws Throwable {
        if (INDY_call950 != null)
            return INDY_call950;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call951;
    private static MethodHandle INDY_call951 () throws Throwable {
        if (INDY_call951 != null)
            return INDY_call951;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call952;
    private static MethodHandle INDY_call952 () throws Throwable {
        if (INDY_call952 != null)
            return INDY_call952;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call953;
    private static MethodHandle INDY_call953 () throws Throwable {
        if (INDY_call953 != null)
            return INDY_call953;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call954;
    private static MethodHandle INDY_call954 () throws Throwable {
        if (INDY_call954 != null)
            return INDY_call954;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call955;
    private static MethodHandle INDY_call955 () throws Throwable {
        if (INDY_call955 != null)
            return INDY_call955;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call956;
    private static MethodHandle INDY_call956 () throws Throwable {
        if (INDY_call956 != null)
            return INDY_call956;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call957;
    private static MethodHandle INDY_call957 () throws Throwable {
        if (INDY_call957 != null)
            return INDY_call957;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call958;
    private static MethodHandle INDY_call958 () throws Throwable {
        if (INDY_call958 != null)
            return INDY_call958;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call959;
    private static MethodHandle INDY_call959 () throws Throwable {
        if (INDY_call959 != null)
            return INDY_call959;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call960;
    private static MethodHandle INDY_call960 () throws Throwable {
        if (INDY_call960 != null)
            return INDY_call960;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call961;
    private static MethodHandle INDY_call961 () throws Throwable {
        if (INDY_call961 != null)
            return INDY_call961;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call962;
    private static MethodHandle INDY_call962 () throws Throwable {
        if (INDY_call962 != null)
            return INDY_call962;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call963;
    private static MethodHandle INDY_call963 () throws Throwable {
        if (INDY_call963 != null)
            return INDY_call963;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call964;
    private static MethodHandle INDY_call964 () throws Throwable {
        if (INDY_call964 != null)
            return INDY_call964;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call965;
    private static MethodHandle INDY_call965 () throws Throwable {
        if (INDY_call965 != null)
            return INDY_call965;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call966;
    private static MethodHandle INDY_call966 () throws Throwable {
        if (INDY_call966 != null)
            return INDY_call966;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call967;
    private static MethodHandle INDY_call967 () throws Throwable {
        if (INDY_call967 != null)
            return INDY_call967;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call968;
    private static MethodHandle INDY_call968 () throws Throwable {
        if (INDY_call968 != null)
            return INDY_call968;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call969;
    private static MethodHandle INDY_call969 () throws Throwable {
        if (INDY_call969 != null)
            return INDY_call969;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call970;
    private static MethodHandle INDY_call970 () throws Throwable {
        if (INDY_call970 != null)
            return INDY_call970;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call971;
    private static MethodHandle INDY_call971 () throws Throwable {
        if (INDY_call971 != null)
            return INDY_call971;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call972;
    private static MethodHandle INDY_call972 () throws Throwable {
        if (INDY_call972 != null)
            return INDY_call972;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call973;
    private static MethodHandle INDY_call973 () throws Throwable {
        if (INDY_call973 != null)
            return INDY_call973;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call974;
    private static MethodHandle INDY_call974 () throws Throwable {
        if (INDY_call974 != null)
            return INDY_call974;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call975;
    private static MethodHandle INDY_call975 () throws Throwable {
        if (INDY_call975 != null)
            return INDY_call975;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call976;
    private static MethodHandle INDY_call976 () throws Throwable {
        if (INDY_call976 != null)
            return INDY_call976;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call977;
    private static MethodHandle INDY_call977 () throws Throwable {
        if (INDY_call977 != null)
            return INDY_call977;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call978;
    private static MethodHandle INDY_call978 () throws Throwable {
        if (INDY_call978 != null)
            return INDY_call978;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call979;
    private static MethodHandle INDY_call979 () throws Throwable {
        if (INDY_call979 != null)
            return INDY_call979;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call980;
    private static MethodHandle INDY_call980 () throws Throwable {
        if (INDY_call980 != null)
            return INDY_call980;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call981;
    private static MethodHandle INDY_call981 () throws Throwable {
        if (INDY_call981 != null)
            return INDY_call981;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call982;
    private static MethodHandle INDY_call982 () throws Throwable {
        if (INDY_call982 != null)
            return INDY_call982;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call983;
    private static MethodHandle INDY_call983 () throws Throwable {
        if (INDY_call983 != null)
            return INDY_call983;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call984;
    private static MethodHandle INDY_call984 () throws Throwable {
        if (INDY_call984 != null)
            return INDY_call984;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call985;
    private static MethodHandle INDY_call985 () throws Throwable {
        if (INDY_call985 != null)
            return INDY_call985;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call986;
    private static MethodHandle INDY_call986 () throws Throwable {
        if (INDY_call986 != null)
            return INDY_call986;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call987;
    private static MethodHandle INDY_call987 () throws Throwable {
        if (INDY_call987 != null)
            return INDY_call987;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call988;
    private static MethodHandle INDY_call988 () throws Throwable {
        if (INDY_call988 != null)
            return INDY_call988;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call989;
    private static MethodHandle INDY_call989 () throws Throwable {
        if (INDY_call989 != null)
            return INDY_call989;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call990;
    private static MethodHandle INDY_call990 () throws Throwable {
        if (INDY_call990 != null)
            return INDY_call990;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call991;
    private static MethodHandle INDY_call991 () throws Throwable {
        if (INDY_call991 != null)
            return INDY_call991;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call992;
    private static MethodHandle INDY_call992 () throws Throwable {
        if (INDY_call992 != null)
            return INDY_call992;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call993;
    private static MethodHandle INDY_call993 () throws Throwable {
        if (INDY_call993 != null)
            return INDY_call993;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call994;
    private static MethodHandle INDY_call994 () throws Throwable {
        if (INDY_call994 != null)
            return INDY_call994;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call995;
    private static MethodHandle INDY_call995 () throws Throwable {
        if (INDY_call995 != null)
            return INDY_call995;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call996;
    private static MethodHandle INDY_call996 () throws Throwable {
        if (INDY_call996 != null)
            return INDY_call996;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call997;
    private static MethodHandle INDY_call997 () throws Throwable {
        if (INDY_call997 != null)
            return INDY_call997;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call998;
    private static MethodHandle INDY_call998 () throws Throwable {
        if (INDY_call998 != null)
            return INDY_call998;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }
    private static MethodHandle INDY_call999;
    private static MethodHandle INDY_call999 () throws Throwable {
        if (INDY_call999 != null)
            return INDY_call999;

        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(
                MethodHandles.lookup(),
                "greet",
                MethodType.methodType(Object.class, INDIFY_Test.class, String.class, int.class));

        return cs.dynamicInvoker();
    }

    public boolean runThread(int threadNum) throws Throwable {
        final INDIFY_Test x = this;
        final String s = "todo el mundo";
        final int i = 123;

        Stresser stresser = createStresser();

        stresser.start(1);
        while ( stresser.continueExecution() ) {
            stresser.iteration();

            long e;
            do {
                e = _expectedTargetCalls.get();
            } while ( ! _expectedTargetCalls.compareAndSet(e, e + 1000) );

            Object o0 = (Object) INDY_call0 ().invokeExact(x, s, i);
            Object o1 = (Object) INDY_call1 ().invokeExact(x, s, i);
            Object o2 = (Object) INDY_call2 ().invokeExact(x, s, i);
            Object o3 = (Object) INDY_call3 ().invokeExact(x, s, i);
            Object o4 = (Object) INDY_call4 ().invokeExact(x, s, i);
            Object o5 = (Object) INDY_call5 ().invokeExact(x, s, i);
            Object o6 = (Object) INDY_call6 ().invokeExact(x, s, i);
            Object o7 = (Object) INDY_call7 ().invokeExact(x, s, i);
            Object o8 = (Object) INDY_call8 ().invokeExact(x, s, i);
            Object o9 = (Object) INDY_call9 ().invokeExact(x, s, i);
            Object o10 = (Object) INDY_call10 ().invokeExact(x, s, i);
            Object o11 = (Object) INDY_call11 ().invokeExact(x, s, i);
            Object o12 = (Object) INDY_call12 ().invokeExact(x, s, i);
            Object o13 = (Object) INDY_call13 ().invokeExact(x, s, i);
            Object o14 = (Object) INDY_call14 ().invokeExact(x, s, i);
            Object o15 = (Object) INDY_call15 ().invokeExact(x, s, i);
            Object o16 = (Object) INDY_call16 ().invokeExact(x, s, i);
            Object o17 = (Object) INDY_call17 ().invokeExact(x, s, i);
            Object o18 = (Object) INDY_call18 ().invokeExact(x, s, i);
            Object o19 = (Object) INDY_call19 ().invokeExact(x, s, i);
            Object o20 = (Object) INDY_call20 ().invokeExact(x, s, i);
            Object o21 = (Object) INDY_call21 ().invokeExact(x, s, i);
            Object o22 = (Object) INDY_call22 ().invokeExact(x, s, i);
            Object o23 = (Object) INDY_call23 ().invokeExact(x, s, i);
            Object o24 = (Object) INDY_call24 ().invokeExact(x, s, i);
            Object o25 = (Object) INDY_call25 ().invokeExact(x, s, i);
            Object o26 = (Object) INDY_call26 ().invokeExact(x, s, i);
            Object o27 = (Object) INDY_call27 ().invokeExact(x, s, i);
            Object o28 = (Object) INDY_call28 ().invokeExact(x, s, i);
            Object o29 = (Object) INDY_call29 ().invokeExact(x, s, i);
            Object o30 = (Object) INDY_call30 ().invokeExact(x, s, i);
            Object o31 = (Object) INDY_call31 ().invokeExact(x, s, i);
            Object o32 = (Object) INDY_call32 ().invokeExact(x, s, i);
            Object o33 = (Object) INDY_call33 ().invokeExact(x, s, i);
            Object o34 = (Object) INDY_call34 ().invokeExact(x, s, i);
            Object o35 = (Object) INDY_call35 ().invokeExact(x, s, i);
            Object o36 = (Object) INDY_call36 ().invokeExact(x, s, i);
            Object o37 = (Object) INDY_call37 ().invokeExact(x, s, i);
            Object o38 = (Object) INDY_call38 ().invokeExact(x, s, i);
            Object o39 = (Object) INDY_call39 ().invokeExact(x, s, i);
            Object o40 = (Object) INDY_call40 ().invokeExact(x, s, i);
            Object o41 = (Object) INDY_call41 ().invokeExact(x, s, i);
            Object o42 = (Object) INDY_call42 ().invokeExact(x, s, i);
            Object o43 = (Object) INDY_call43 ().invokeExact(x, s, i);
            Object o44 = (Object) INDY_call44 ().invokeExact(x, s, i);
            Object o45 = (Object) INDY_call45 ().invokeExact(x, s, i);
            Object o46 = (Object) INDY_call46 ().invokeExact(x, s, i);
            Object o47 = (Object) INDY_call47 ().invokeExact(x, s, i);
            Object o48 = (Object) INDY_call48 ().invokeExact(x, s, i);
            Object o49 = (Object) INDY_call49 ().invokeExact(x, s, i);
            Object o50 = (Object) INDY_call50 ().invokeExact(x, s, i);
            Object o51 = (Object) INDY_call51 ().invokeExact(x, s, i);
            Object o52 = (Object) INDY_call52 ().invokeExact(x, s, i);
            Object o53 = (Object) INDY_call53 ().invokeExact(x, s, i);
            Object o54 = (Object) INDY_call54 ().invokeExact(x, s, i);
            Object o55 = (Object) INDY_call55 ().invokeExact(x, s, i);
            Object o56 = (Object) INDY_call56 ().invokeExact(x, s, i);
            Object o57 = (Object) INDY_call57 ().invokeExact(x, s, i);
            Object o58 = (Object) INDY_call58 ().invokeExact(x, s, i);
            Object o59 = (Object) INDY_call59 ().invokeExact(x, s, i);
            Object o60 = (Object) INDY_call60 ().invokeExact(x, s, i);
            Object o61 = (Object) INDY_call61 ().invokeExact(x, s, i);
            Object o62 = (Object) INDY_call62 ().invokeExact(x, s, i);
            Object o63 = (Object) INDY_call63 ().invokeExact(x, s, i);
            Object o64 = (Object) INDY_call64 ().invokeExact(x, s, i);
            Object o65 = (Object) INDY_call65 ().invokeExact(x, s, i);
            Object o66 = (Object) INDY_call66 ().invokeExact(x, s, i);
            Object o67 = (Object) INDY_call67 ().invokeExact(x, s, i);
            Object o68 = (Object) INDY_call68 ().invokeExact(x, s, i);
            Object o69 = (Object) INDY_call69 ().invokeExact(x, s, i);
            Object o70 = (Object) INDY_call70 ().invokeExact(x, s, i);
            Object o71 = (Object) INDY_call71 ().invokeExact(x, s, i);
            Object o72 = (Object) INDY_call72 ().invokeExact(x, s, i);
            Object o73 = (Object) INDY_call73 ().invokeExact(x, s, i);
            Object o74 = (Object) INDY_call74 ().invokeExact(x, s, i);
            Object o75 = (Object) INDY_call75 ().invokeExact(x, s, i);
            Object o76 = (Object) INDY_call76 ().invokeExact(x, s, i);
            Object o77 = (Object) INDY_call77 ().invokeExact(x, s, i);
            Object o78 = (Object) INDY_call78 ().invokeExact(x, s, i);
            Object o79 = (Object) INDY_call79 ().invokeExact(x, s, i);
            Object o80 = (Object) INDY_call80 ().invokeExact(x, s, i);
            Object o81 = (Object) INDY_call81 ().invokeExact(x, s, i);
            Object o82 = (Object) INDY_call82 ().invokeExact(x, s, i);
            Object o83 = (Object) INDY_call83 ().invokeExact(x, s, i);
            Object o84 = (Object) INDY_call84 ().invokeExact(x, s, i);
            Object o85 = (Object) INDY_call85 ().invokeExact(x, s, i);
            Object o86 = (Object) INDY_call86 ().invokeExact(x, s, i);
            Object o87 = (Object) INDY_call87 ().invokeExact(x, s, i);
            Object o88 = (Object) INDY_call88 ().invokeExact(x, s, i);
            Object o89 = (Object) INDY_call89 ().invokeExact(x, s, i);
            Object o90 = (Object) INDY_call90 ().invokeExact(x, s, i);
            Object o91 = (Object) INDY_call91 ().invokeExact(x, s, i);
            Object o92 = (Object) INDY_call92 ().invokeExact(x, s, i);
            Object o93 = (Object) INDY_call93 ().invokeExact(x, s, i);
            Object o94 = (Object) INDY_call94 ().invokeExact(x, s, i);
            Object o95 = (Object) INDY_call95 ().invokeExact(x, s, i);
            Object o96 = (Object) INDY_call96 ().invokeExact(x, s, i);
            Object o97 = (Object) INDY_call97 ().invokeExact(x, s, i);
            Object o98 = (Object) INDY_call98 ().invokeExact(x, s, i);
            Object o99 = (Object) INDY_call99 ().invokeExact(x, s, i);
            Object o100 = (Object) INDY_call100 ().invokeExact(x, s, i);
            Object o101 = (Object) INDY_call101 ().invokeExact(x, s, i);
            Object o102 = (Object) INDY_call102 ().invokeExact(x, s, i);
            Object o103 = (Object) INDY_call103 ().invokeExact(x, s, i);
            Object o104 = (Object) INDY_call104 ().invokeExact(x, s, i);
            Object o105 = (Object) INDY_call105 ().invokeExact(x, s, i);
            Object o106 = (Object) INDY_call106 ().invokeExact(x, s, i);
            Object o107 = (Object) INDY_call107 ().invokeExact(x, s, i);
            Object o108 = (Object) INDY_call108 ().invokeExact(x, s, i);
            Object o109 = (Object) INDY_call109 ().invokeExact(x, s, i);
            Object o110 = (Object) INDY_call110 ().invokeExact(x, s, i);
            Object o111 = (Object) INDY_call111 ().invokeExact(x, s, i);
            Object o112 = (Object) INDY_call112 ().invokeExact(x, s, i);
            Object o113 = (Object) INDY_call113 ().invokeExact(x, s, i);
            Object o114 = (Object) INDY_call114 ().invokeExact(x, s, i);
            Object o115 = (Object) INDY_call115 ().invokeExact(x, s, i);
            Object o116 = (Object) INDY_call116 ().invokeExact(x, s, i);
            Object o117 = (Object) INDY_call117 ().invokeExact(x, s, i);
            Object o118 = (Object) INDY_call118 ().invokeExact(x, s, i);
            Object o119 = (Object) INDY_call119 ().invokeExact(x, s, i);
            Object o120 = (Object) INDY_call120 ().invokeExact(x, s, i);
            Object o121 = (Object) INDY_call121 ().invokeExact(x, s, i);
            Object o122 = (Object) INDY_call122 ().invokeExact(x, s, i);
            Object o123 = (Object) INDY_call123 ().invokeExact(x, s, i);
            Object o124 = (Object) INDY_call124 ().invokeExact(x, s, i);
            Object o125 = (Object) INDY_call125 ().invokeExact(x, s, i);
            Object o126 = (Object) INDY_call126 ().invokeExact(x, s, i);
            Object o127 = (Object) INDY_call127 ().invokeExact(x, s, i);
            Object o128 = (Object) INDY_call128 ().invokeExact(x, s, i);
            Object o129 = (Object) INDY_call129 ().invokeExact(x, s, i);
            Object o130 = (Object) INDY_call130 ().invokeExact(x, s, i);
            Object o131 = (Object) INDY_call131 ().invokeExact(x, s, i);
            Object o132 = (Object) INDY_call132 ().invokeExact(x, s, i);
            Object o133 = (Object) INDY_call133 ().invokeExact(x, s, i);
            Object o134 = (Object) INDY_call134 ().invokeExact(x, s, i);
            Object o135 = (Object) INDY_call135 ().invokeExact(x, s, i);
            Object o136 = (Object) INDY_call136 ().invokeExact(x, s, i);
            Object o137 = (Object) INDY_call137 ().invokeExact(x, s, i);
            Object o138 = (Object) INDY_call138 ().invokeExact(x, s, i);
            Object o139 = (Object) INDY_call139 ().invokeExact(x, s, i);
            Object o140 = (Object) INDY_call140 ().invokeExact(x, s, i);
            Object o141 = (Object) INDY_call141 ().invokeExact(x, s, i);
            Object o142 = (Object) INDY_call142 ().invokeExact(x, s, i);
            Object o143 = (Object) INDY_call143 ().invokeExact(x, s, i);
            Object o144 = (Object) INDY_call144 ().invokeExact(x, s, i);
            Object o145 = (Object) INDY_call145 ().invokeExact(x, s, i);
            Object o146 = (Object) INDY_call146 ().invokeExact(x, s, i);
            Object o147 = (Object) INDY_call147 ().invokeExact(x, s, i);
            Object o148 = (Object) INDY_call148 ().invokeExact(x, s, i);
            Object o149 = (Object) INDY_call149 ().invokeExact(x, s, i);
            Object o150 = (Object) INDY_call150 ().invokeExact(x, s, i);
            Object o151 = (Object) INDY_call151 ().invokeExact(x, s, i);
            Object o152 = (Object) INDY_call152 ().invokeExact(x, s, i);
            Object o153 = (Object) INDY_call153 ().invokeExact(x, s, i);
            Object o154 = (Object) INDY_call154 ().invokeExact(x, s, i);
            Object o155 = (Object) INDY_call155 ().invokeExact(x, s, i);
            Object o156 = (Object) INDY_call156 ().invokeExact(x, s, i);
            Object o157 = (Object) INDY_call157 ().invokeExact(x, s, i);
            Object o158 = (Object) INDY_call158 ().invokeExact(x, s, i);
            Object o159 = (Object) INDY_call159 ().invokeExact(x, s, i);
            Object o160 = (Object) INDY_call160 ().invokeExact(x, s, i);
            Object o161 = (Object) INDY_call161 ().invokeExact(x, s, i);
            Object o162 = (Object) INDY_call162 ().invokeExact(x, s, i);
            Object o163 = (Object) INDY_call163 ().invokeExact(x, s, i);
            Object o164 = (Object) INDY_call164 ().invokeExact(x, s, i);
            Object o165 = (Object) INDY_call165 ().invokeExact(x, s, i);
            Object o166 = (Object) INDY_call166 ().invokeExact(x, s, i);
            Object o167 = (Object) INDY_call167 ().invokeExact(x, s, i);
            Object o168 = (Object) INDY_call168 ().invokeExact(x, s, i);
            Object o169 = (Object) INDY_call169 ().invokeExact(x, s, i);
            Object o170 = (Object) INDY_call170 ().invokeExact(x, s, i);
            Object o171 = (Object) INDY_call171 ().invokeExact(x, s, i);
            Object o172 = (Object) INDY_call172 ().invokeExact(x, s, i);
            Object o173 = (Object) INDY_call173 ().invokeExact(x, s, i);
            Object o174 = (Object) INDY_call174 ().invokeExact(x, s, i);
            Object o175 = (Object) INDY_call175 ().invokeExact(x, s, i);
            Object o176 = (Object) INDY_call176 ().invokeExact(x, s, i);
            Object o177 = (Object) INDY_call177 ().invokeExact(x, s, i);
            Object o178 = (Object) INDY_call178 ().invokeExact(x, s, i);
            Object o179 = (Object) INDY_call179 ().invokeExact(x, s, i);
            Object o180 = (Object) INDY_call180 ().invokeExact(x, s, i);
            Object o181 = (Object) INDY_call181 ().invokeExact(x, s, i);
            Object o182 = (Object) INDY_call182 ().invokeExact(x, s, i);
            Object o183 = (Object) INDY_call183 ().invokeExact(x, s, i);
            Object o184 = (Object) INDY_call184 ().invokeExact(x, s, i);
            Object o185 = (Object) INDY_call185 ().invokeExact(x, s, i);
            Object o186 = (Object) INDY_call186 ().invokeExact(x, s, i);
            Object o187 = (Object) INDY_call187 ().invokeExact(x, s, i);
            Object o188 = (Object) INDY_call188 ().invokeExact(x, s, i);
            Object o189 = (Object) INDY_call189 ().invokeExact(x, s, i);
            Object o190 = (Object) INDY_call190 ().invokeExact(x, s, i);
            Object o191 = (Object) INDY_call191 ().invokeExact(x, s, i);
            Object o192 = (Object) INDY_call192 ().invokeExact(x, s, i);
            Object o193 = (Object) INDY_call193 ().invokeExact(x, s, i);
            Object o194 = (Object) INDY_call194 ().invokeExact(x, s, i);
            Object o195 = (Object) INDY_call195 ().invokeExact(x, s, i);
            Object o196 = (Object) INDY_call196 ().invokeExact(x, s, i);
            Object o197 = (Object) INDY_call197 ().invokeExact(x, s, i);
            Object o198 = (Object) INDY_call198 ().invokeExact(x, s, i);
            Object o199 = (Object) INDY_call199 ().invokeExact(x, s, i);
            Object o200 = (Object) INDY_call200 ().invokeExact(x, s, i);
            Object o201 = (Object) INDY_call201 ().invokeExact(x, s, i);
            Object o202 = (Object) INDY_call202 ().invokeExact(x, s, i);
            Object o203 = (Object) INDY_call203 ().invokeExact(x, s, i);
            Object o204 = (Object) INDY_call204 ().invokeExact(x, s, i);
            Object o205 = (Object) INDY_call205 ().invokeExact(x, s, i);
            Object o206 = (Object) INDY_call206 ().invokeExact(x, s, i);
            Object o207 = (Object) INDY_call207 ().invokeExact(x, s, i);
            Object o208 = (Object) INDY_call208 ().invokeExact(x, s, i);
            Object o209 = (Object) INDY_call209 ().invokeExact(x, s, i);
            Object o210 = (Object) INDY_call210 ().invokeExact(x, s, i);
            Object o211 = (Object) INDY_call211 ().invokeExact(x, s, i);
            Object o212 = (Object) INDY_call212 ().invokeExact(x, s, i);
            Object o213 = (Object) INDY_call213 ().invokeExact(x, s, i);
            Object o214 = (Object) INDY_call214 ().invokeExact(x, s, i);
            Object o215 = (Object) INDY_call215 ().invokeExact(x, s, i);
            Object o216 = (Object) INDY_call216 ().invokeExact(x, s, i);
            Object o217 = (Object) INDY_call217 ().invokeExact(x, s, i);
            Object o218 = (Object) INDY_call218 ().invokeExact(x, s, i);
            Object o219 = (Object) INDY_call219 ().invokeExact(x, s, i);
            Object o220 = (Object) INDY_call220 ().invokeExact(x, s, i);
            Object o221 = (Object) INDY_call221 ().invokeExact(x, s, i);
            Object o222 = (Object) INDY_call222 ().invokeExact(x, s, i);
            Object o223 = (Object) INDY_call223 ().invokeExact(x, s, i);
            Object o224 = (Object) INDY_call224 ().invokeExact(x, s, i);
            Object o225 = (Object) INDY_call225 ().invokeExact(x, s, i);
            Object o226 = (Object) INDY_call226 ().invokeExact(x, s, i);
            Object o227 = (Object) INDY_call227 ().invokeExact(x, s, i);
            Object o228 = (Object) INDY_call228 ().invokeExact(x, s, i);
            Object o229 = (Object) INDY_call229 ().invokeExact(x, s, i);
            Object o230 = (Object) INDY_call230 ().invokeExact(x, s, i);
            Object o231 = (Object) INDY_call231 ().invokeExact(x, s, i);
            Object o232 = (Object) INDY_call232 ().invokeExact(x, s, i);
            Object o233 = (Object) INDY_call233 ().invokeExact(x, s, i);
            Object o234 = (Object) INDY_call234 ().invokeExact(x, s, i);
            Object o235 = (Object) INDY_call235 ().invokeExact(x, s, i);
            Object o236 = (Object) INDY_call236 ().invokeExact(x, s, i);
            Object o237 = (Object) INDY_call237 ().invokeExact(x, s, i);
            Object o238 = (Object) INDY_call238 ().invokeExact(x, s, i);
            Object o239 = (Object) INDY_call239 ().invokeExact(x, s, i);
            Object o240 = (Object) INDY_call240 ().invokeExact(x, s, i);
            Object o241 = (Object) INDY_call241 ().invokeExact(x, s, i);
            Object o242 = (Object) INDY_call242 ().invokeExact(x, s, i);
            Object o243 = (Object) INDY_call243 ().invokeExact(x, s, i);
            Object o244 = (Object) INDY_call244 ().invokeExact(x, s, i);
            Object o245 = (Object) INDY_call245 ().invokeExact(x, s, i);
            Object o246 = (Object) INDY_call246 ().invokeExact(x, s, i);
            Object o247 = (Object) INDY_call247 ().invokeExact(x, s, i);
            Object o248 = (Object) INDY_call248 ().invokeExact(x, s, i);
            Object o249 = (Object) INDY_call249 ().invokeExact(x, s, i);
            Object o250 = (Object) INDY_call250 ().invokeExact(x, s, i);
            Object o251 = (Object) INDY_call251 ().invokeExact(x, s, i);
            Object o252 = (Object) INDY_call252 ().invokeExact(x, s, i);
            Object o253 = (Object) INDY_call253 ().invokeExact(x, s, i);
            Object o254 = (Object) INDY_call254 ().invokeExact(x, s, i);
            Object o255 = (Object) INDY_call255 ().invokeExact(x, s, i);
            Object o256 = (Object) INDY_call256 ().invokeExact(x, s, i);
            Object o257 = (Object) INDY_call257 ().invokeExact(x, s, i);
            Object o258 = (Object) INDY_call258 ().invokeExact(x, s, i);
            Object o259 = (Object) INDY_call259 ().invokeExact(x, s, i);
            Object o260 = (Object) INDY_call260 ().invokeExact(x, s, i);
            Object o261 = (Object) INDY_call261 ().invokeExact(x, s, i);
            Object o262 = (Object) INDY_call262 ().invokeExact(x, s, i);
            Object o263 = (Object) INDY_call263 ().invokeExact(x, s, i);
            Object o264 = (Object) INDY_call264 ().invokeExact(x, s, i);
            Object o265 = (Object) INDY_call265 ().invokeExact(x, s, i);
            Object o266 = (Object) INDY_call266 ().invokeExact(x, s, i);
            Object o267 = (Object) INDY_call267 ().invokeExact(x, s, i);
            Object o268 = (Object) INDY_call268 ().invokeExact(x, s, i);
            Object o269 = (Object) INDY_call269 ().invokeExact(x, s, i);
            Object o270 = (Object) INDY_call270 ().invokeExact(x, s, i);
            Object o271 = (Object) INDY_call271 ().invokeExact(x, s, i);
            Object o272 = (Object) INDY_call272 ().invokeExact(x, s, i);
            Object o273 = (Object) INDY_call273 ().invokeExact(x, s, i);
            Object o274 = (Object) INDY_call274 ().invokeExact(x, s, i);
            Object o275 = (Object) INDY_call275 ().invokeExact(x, s, i);
            Object o276 = (Object) INDY_call276 ().invokeExact(x, s, i);
            Object o277 = (Object) INDY_call277 ().invokeExact(x, s, i);
            Object o278 = (Object) INDY_call278 ().invokeExact(x, s, i);
            Object o279 = (Object) INDY_call279 ().invokeExact(x, s, i);
            Object o280 = (Object) INDY_call280 ().invokeExact(x, s, i);
            Object o281 = (Object) INDY_call281 ().invokeExact(x, s, i);
            Object o282 = (Object) INDY_call282 ().invokeExact(x, s, i);
            Object o283 = (Object) INDY_call283 ().invokeExact(x, s, i);
            Object o284 = (Object) INDY_call284 ().invokeExact(x, s, i);
            Object o285 = (Object) INDY_call285 ().invokeExact(x, s, i);
            Object o286 = (Object) INDY_call286 ().invokeExact(x, s, i);
            Object o287 = (Object) INDY_call287 ().invokeExact(x, s, i);
            Object o288 = (Object) INDY_call288 ().invokeExact(x, s, i);
            Object o289 = (Object) INDY_call289 ().invokeExact(x, s, i);
            Object o290 = (Object) INDY_call290 ().invokeExact(x, s, i);
            Object o291 = (Object) INDY_call291 ().invokeExact(x, s, i);
            Object o292 = (Object) INDY_call292 ().invokeExact(x, s, i);
            Object o293 = (Object) INDY_call293 ().invokeExact(x, s, i);
            Object o294 = (Object) INDY_call294 ().invokeExact(x, s, i);
            Object o295 = (Object) INDY_call295 ().invokeExact(x, s, i);
            Object o296 = (Object) INDY_call296 ().invokeExact(x, s, i);
            Object o297 = (Object) INDY_call297 ().invokeExact(x, s, i);
            Object o298 = (Object) INDY_call298 ().invokeExact(x, s, i);
            Object o299 = (Object) INDY_call299 ().invokeExact(x, s, i);
            Object o300 = (Object) INDY_call300 ().invokeExact(x, s, i);
            Object o301 = (Object) INDY_call301 ().invokeExact(x, s, i);
            Object o302 = (Object) INDY_call302 ().invokeExact(x, s, i);
            Object o303 = (Object) INDY_call303 ().invokeExact(x, s, i);
            Object o304 = (Object) INDY_call304 ().invokeExact(x, s, i);
            Object o305 = (Object) INDY_call305 ().invokeExact(x, s, i);
            Object o306 = (Object) INDY_call306 ().invokeExact(x, s, i);
            Object o307 = (Object) INDY_call307 ().invokeExact(x, s, i);
            Object o308 = (Object) INDY_call308 ().invokeExact(x, s, i);
            Object o309 = (Object) INDY_call309 ().invokeExact(x, s, i);
            Object o310 = (Object) INDY_call310 ().invokeExact(x, s, i);
            Object o311 = (Object) INDY_call311 ().invokeExact(x, s, i);
            Object o312 = (Object) INDY_call312 ().invokeExact(x, s, i);
            Object o313 = (Object) INDY_call313 ().invokeExact(x, s, i);
            Object o314 = (Object) INDY_call314 ().invokeExact(x, s, i);
            Object o315 = (Object) INDY_call315 ().invokeExact(x, s, i);
            Object o316 = (Object) INDY_call316 ().invokeExact(x, s, i);
            Object o317 = (Object) INDY_call317 ().invokeExact(x, s, i);
            Object o318 = (Object) INDY_call318 ().invokeExact(x, s, i);
            Object o319 = (Object) INDY_call319 ().invokeExact(x, s, i);
            Object o320 = (Object) INDY_call320 ().invokeExact(x, s, i);
            Object o321 = (Object) INDY_call321 ().invokeExact(x, s, i);
            Object o322 = (Object) INDY_call322 ().invokeExact(x, s, i);
            Object o323 = (Object) INDY_call323 ().invokeExact(x, s, i);
            Object o324 = (Object) INDY_call324 ().invokeExact(x, s, i);
            Object o325 = (Object) INDY_call325 ().invokeExact(x, s, i);
            Object o326 = (Object) INDY_call326 ().invokeExact(x, s, i);
            Object o327 = (Object) INDY_call327 ().invokeExact(x, s, i);
            Object o328 = (Object) INDY_call328 ().invokeExact(x, s, i);
            Object o329 = (Object) INDY_call329 ().invokeExact(x, s, i);
            Object o330 = (Object) INDY_call330 ().invokeExact(x, s, i);
            Object o331 = (Object) INDY_call331 ().invokeExact(x, s, i);
            Object o332 = (Object) INDY_call332 ().invokeExact(x, s, i);
            Object o333 = (Object) INDY_call333 ().invokeExact(x, s, i);
            Object o334 = (Object) INDY_call334 ().invokeExact(x, s, i);
            Object o335 = (Object) INDY_call335 ().invokeExact(x, s, i);
            Object o336 = (Object) INDY_call336 ().invokeExact(x, s, i);
            Object o337 = (Object) INDY_call337 ().invokeExact(x, s, i);
            Object o338 = (Object) INDY_call338 ().invokeExact(x, s, i);
            Object o339 = (Object) INDY_call339 ().invokeExact(x, s, i);
            Object o340 = (Object) INDY_call340 ().invokeExact(x, s, i);
            Object o341 = (Object) INDY_call341 ().invokeExact(x, s, i);
            Object o342 = (Object) INDY_call342 ().invokeExact(x, s, i);
            Object o343 = (Object) INDY_call343 ().invokeExact(x, s, i);
            Object o344 = (Object) INDY_call344 ().invokeExact(x, s, i);
            Object o345 = (Object) INDY_call345 ().invokeExact(x, s, i);
            Object o346 = (Object) INDY_call346 ().invokeExact(x, s, i);
            Object o347 = (Object) INDY_call347 ().invokeExact(x, s, i);
            Object o348 = (Object) INDY_call348 ().invokeExact(x, s, i);
            Object o349 = (Object) INDY_call349 ().invokeExact(x, s, i);
            Object o350 = (Object) INDY_call350 ().invokeExact(x, s, i);
            Object o351 = (Object) INDY_call351 ().invokeExact(x, s, i);
            Object o352 = (Object) INDY_call352 ().invokeExact(x, s, i);
            Object o353 = (Object) INDY_call353 ().invokeExact(x, s, i);
            Object o354 = (Object) INDY_call354 ().invokeExact(x, s, i);
            Object o355 = (Object) INDY_call355 ().invokeExact(x, s, i);
            Object o356 = (Object) INDY_call356 ().invokeExact(x, s, i);
            Object o357 = (Object) INDY_call357 ().invokeExact(x, s, i);
            Object o358 = (Object) INDY_call358 ().invokeExact(x, s, i);
            Object o359 = (Object) INDY_call359 ().invokeExact(x, s, i);
            Object o360 = (Object) INDY_call360 ().invokeExact(x, s, i);
            Object o361 = (Object) INDY_call361 ().invokeExact(x, s, i);
            Object o362 = (Object) INDY_call362 ().invokeExact(x, s, i);
            Object o363 = (Object) INDY_call363 ().invokeExact(x, s, i);
            Object o364 = (Object) INDY_call364 ().invokeExact(x, s, i);
            Object o365 = (Object) INDY_call365 ().invokeExact(x, s, i);
            Object o366 = (Object) INDY_call366 ().invokeExact(x, s, i);
            Object o367 = (Object) INDY_call367 ().invokeExact(x, s, i);
            Object o368 = (Object) INDY_call368 ().invokeExact(x, s, i);
            Object o369 = (Object) INDY_call369 ().invokeExact(x, s, i);
            Object o370 = (Object) INDY_call370 ().invokeExact(x, s, i);
            Object o371 = (Object) INDY_call371 ().invokeExact(x, s, i);
            Object o372 = (Object) INDY_call372 ().invokeExact(x, s, i);
            Object o373 = (Object) INDY_call373 ().invokeExact(x, s, i);
            Object o374 = (Object) INDY_call374 ().invokeExact(x, s, i);
            Object o375 = (Object) INDY_call375 ().invokeExact(x, s, i);
            Object o376 = (Object) INDY_call376 ().invokeExact(x, s, i);
            Object o377 = (Object) INDY_call377 ().invokeExact(x, s, i);
            Object o378 = (Object) INDY_call378 ().invokeExact(x, s, i);
            Object o379 = (Object) INDY_call379 ().invokeExact(x, s, i);
            Object o380 = (Object) INDY_call380 ().invokeExact(x, s, i);
            Object o381 = (Object) INDY_call381 ().invokeExact(x, s, i);
            Object o382 = (Object) INDY_call382 ().invokeExact(x, s, i);
            Object o383 = (Object) INDY_call383 ().invokeExact(x, s, i);
            Object o384 = (Object) INDY_call384 ().invokeExact(x, s, i);
            Object o385 = (Object) INDY_call385 ().invokeExact(x, s, i);
            Object o386 = (Object) INDY_call386 ().invokeExact(x, s, i);
            Object o387 = (Object) INDY_call387 ().invokeExact(x, s, i);
            Object o388 = (Object) INDY_call388 ().invokeExact(x, s, i);
            Object o389 = (Object) INDY_call389 ().invokeExact(x, s, i);
            Object o390 = (Object) INDY_call390 ().invokeExact(x, s, i);
            Object o391 = (Object) INDY_call391 ().invokeExact(x, s, i);
            Object o392 = (Object) INDY_call392 ().invokeExact(x, s, i);
            Object o393 = (Object) INDY_call393 ().invokeExact(x, s, i);
            Object o394 = (Object) INDY_call394 ().invokeExact(x, s, i);
            Object o395 = (Object) INDY_call395 ().invokeExact(x, s, i);
            Object o396 = (Object) INDY_call396 ().invokeExact(x, s, i);
            Object o397 = (Object) INDY_call397 ().invokeExact(x, s, i);
            Object o398 = (Object) INDY_call398 ().invokeExact(x, s, i);
            Object o399 = (Object) INDY_call399 ().invokeExact(x, s, i);
            Object o400 = (Object) INDY_call400 ().invokeExact(x, s, i);
            Object o401 = (Object) INDY_call401 ().invokeExact(x, s, i);
            Object o402 = (Object) INDY_call402 ().invokeExact(x, s, i);
            Object o403 = (Object) INDY_call403 ().invokeExact(x, s, i);
            Object o404 = (Object) INDY_call404 ().invokeExact(x, s, i);
            Object o405 = (Object) INDY_call405 ().invokeExact(x, s, i);
            Object o406 = (Object) INDY_call406 ().invokeExact(x, s, i);
            Object o407 = (Object) INDY_call407 ().invokeExact(x, s, i);
            Object o408 = (Object) INDY_call408 ().invokeExact(x, s, i);
            Object o409 = (Object) INDY_call409 ().invokeExact(x, s, i);
            Object o410 = (Object) INDY_call410 ().invokeExact(x, s, i);
            Object o411 = (Object) INDY_call411 ().invokeExact(x, s, i);
            Object o412 = (Object) INDY_call412 ().invokeExact(x, s, i);
            Object o413 = (Object) INDY_call413 ().invokeExact(x, s, i);
            Object o414 = (Object) INDY_call414 ().invokeExact(x, s, i);
            Object o415 = (Object) INDY_call415 ().invokeExact(x, s, i);
            Object o416 = (Object) INDY_call416 ().invokeExact(x, s, i);
            Object o417 = (Object) INDY_call417 ().invokeExact(x, s, i);
            Object o418 = (Object) INDY_call418 ().invokeExact(x, s, i);
            Object o419 = (Object) INDY_call419 ().invokeExact(x, s, i);
            Object o420 = (Object) INDY_call420 ().invokeExact(x, s, i);
            Object o421 = (Object) INDY_call421 ().invokeExact(x, s, i);
            Object o422 = (Object) INDY_call422 ().invokeExact(x, s, i);
            Object o423 = (Object) INDY_call423 ().invokeExact(x, s, i);
            Object o424 = (Object) INDY_call424 ().invokeExact(x, s, i);
            Object o425 = (Object) INDY_call425 ().invokeExact(x, s, i);
            Object o426 = (Object) INDY_call426 ().invokeExact(x, s, i);
            Object o427 = (Object) INDY_call427 ().invokeExact(x, s, i);
            Object o428 = (Object) INDY_call428 ().invokeExact(x, s, i);
            Object o429 = (Object) INDY_call429 ().invokeExact(x, s, i);
            Object o430 = (Object) INDY_call430 ().invokeExact(x, s, i);
            Object o431 = (Object) INDY_call431 ().invokeExact(x, s, i);
            Object o432 = (Object) INDY_call432 ().invokeExact(x, s, i);
            Object o433 = (Object) INDY_call433 ().invokeExact(x, s, i);
            Object o434 = (Object) INDY_call434 ().invokeExact(x, s, i);
            Object o435 = (Object) INDY_call435 ().invokeExact(x, s, i);
            Object o436 = (Object) INDY_call436 ().invokeExact(x, s, i);
            Object o437 = (Object) INDY_call437 ().invokeExact(x, s, i);
            Object o438 = (Object) INDY_call438 ().invokeExact(x, s, i);
            Object o439 = (Object) INDY_call439 ().invokeExact(x, s, i);
            Object o440 = (Object) INDY_call440 ().invokeExact(x, s, i);
            Object o441 = (Object) INDY_call441 ().invokeExact(x, s, i);
            Object o442 = (Object) INDY_call442 ().invokeExact(x, s, i);
            Object o443 = (Object) INDY_call443 ().invokeExact(x, s, i);
            Object o444 = (Object) INDY_call444 ().invokeExact(x, s, i);
            Object o445 = (Object) INDY_call445 ().invokeExact(x, s, i);
            Object o446 = (Object) INDY_call446 ().invokeExact(x, s, i);
            Object o447 = (Object) INDY_call447 ().invokeExact(x, s, i);
            Object o448 = (Object) INDY_call448 ().invokeExact(x, s, i);
            Object o449 = (Object) INDY_call449 ().invokeExact(x, s, i);
            Object o450 = (Object) INDY_call450 ().invokeExact(x, s, i);
            Object o451 = (Object) INDY_call451 ().invokeExact(x, s, i);
            Object o452 = (Object) INDY_call452 ().invokeExact(x, s, i);
            Object o453 = (Object) INDY_call453 ().invokeExact(x, s, i);
            Object o454 = (Object) INDY_call454 ().invokeExact(x, s, i);
            Object o455 = (Object) INDY_call455 ().invokeExact(x, s, i);
            Object o456 = (Object) INDY_call456 ().invokeExact(x, s, i);
            Object o457 = (Object) INDY_call457 ().invokeExact(x, s, i);
            Object o458 = (Object) INDY_call458 ().invokeExact(x, s, i);
            Object o459 = (Object) INDY_call459 ().invokeExact(x, s, i);
            Object o460 = (Object) INDY_call460 ().invokeExact(x, s, i);
            Object o461 = (Object) INDY_call461 ().invokeExact(x, s, i);
            Object o462 = (Object) INDY_call462 ().invokeExact(x, s, i);
            Object o463 = (Object) INDY_call463 ().invokeExact(x, s, i);
            Object o464 = (Object) INDY_call464 ().invokeExact(x, s, i);
            Object o465 = (Object) INDY_call465 ().invokeExact(x, s, i);
            Object o466 = (Object) INDY_call466 ().invokeExact(x, s, i);
            Object o467 = (Object) INDY_call467 ().invokeExact(x, s, i);
            Object o468 = (Object) INDY_call468 ().invokeExact(x, s, i);
            Object o469 = (Object) INDY_call469 ().invokeExact(x, s, i);
            Object o470 = (Object) INDY_call470 ().invokeExact(x, s, i);
            Object o471 = (Object) INDY_call471 ().invokeExact(x, s, i);
            Object o472 = (Object) INDY_call472 ().invokeExact(x, s, i);
            Object o473 = (Object) INDY_call473 ().invokeExact(x, s, i);
            Object o474 = (Object) INDY_call474 ().invokeExact(x, s, i);
            Object o475 = (Object) INDY_call475 ().invokeExact(x, s, i);
            Object o476 = (Object) INDY_call476 ().invokeExact(x, s, i);
            Object o477 = (Object) INDY_call477 ().invokeExact(x, s, i);
            Object o478 = (Object) INDY_call478 ().invokeExact(x, s, i);
            Object o479 = (Object) INDY_call479 ().invokeExact(x, s, i);
            Object o480 = (Object) INDY_call480 ().invokeExact(x, s, i);
            Object o481 = (Object) INDY_call481 ().invokeExact(x, s, i);
            Object o482 = (Object) INDY_call482 ().invokeExact(x, s, i);
            Object o483 = (Object) INDY_call483 ().invokeExact(x, s, i);
            Object o484 = (Object) INDY_call484 ().invokeExact(x, s, i);
            Object o485 = (Object) INDY_call485 ().invokeExact(x, s, i);
            Object o486 = (Object) INDY_call486 ().invokeExact(x, s, i);
            Object o487 = (Object) INDY_call487 ().invokeExact(x, s, i);
            Object o488 = (Object) INDY_call488 ().invokeExact(x, s, i);
            Object o489 = (Object) INDY_call489 ().invokeExact(x, s, i);
            Object o490 = (Object) INDY_call490 ().invokeExact(x, s, i);
            Object o491 = (Object) INDY_call491 ().invokeExact(x, s, i);
            Object o492 = (Object) INDY_call492 ().invokeExact(x, s, i);
            Object o493 = (Object) INDY_call493 ().invokeExact(x, s, i);
            Object o494 = (Object) INDY_call494 ().invokeExact(x, s, i);
            Object o495 = (Object) INDY_call495 ().invokeExact(x, s, i);
            Object o496 = (Object) INDY_call496 ().invokeExact(x, s, i);
            Object o497 = (Object) INDY_call497 ().invokeExact(x, s, i);
            Object o498 = (Object) INDY_call498 ().invokeExact(x, s, i);
            Object o499 = (Object) INDY_call499 ().invokeExact(x, s, i);
            Object o500 = (Object) INDY_call500 ().invokeExact(x, s, i);
            Object o501 = (Object) INDY_call501 ().invokeExact(x, s, i);
            Object o502 = (Object) INDY_call502 ().invokeExact(x, s, i);
            Object o503 = (Object) INDY_call503 ().invokeExact(x, s, i);
            Object o504 = (Object) INDY_call504 ().invokeExact(x, s, i);
            Object o505 = (Object) INDY_call505 ().invokeExact(x, s, i);
            Object o506 = (Object) INDY_call506 ().invokeExact(x, s, i);
            Object o507 = (Object) INDY_call507 ().invokeExact(x, s, i);
            Object o508 = (Object) INDY_call508 ().invokeExact(x, s, i);
            Object o509 = (Object) INDY_call509 ().invokeExact(x, s, i);
            Object o510 = (Object) INDY_call510 ().invokeExact(x, s, i);
            Object o511 = (Object) INDY_call511 ().invokeExact(x, s, i);
            Object o512 = (Object) INDY_call512 ().invokeExact(x, s, i);
            Object o513 = (Object) INDY_call513 ().invokeExact(x, s, i);
            Object o514 = (Object) INDY_call514 ().invokeExact(x, s, i);
            Object o515 = (Object) INDY_call515 ().invokeExact(x, s, i);
            Object o516 = (Object) INDY_call516 ().invokeExact(x, s, i);
            Object o517 = (Object) INDY_call517 ().invokeExact(x, s, i);
            Object o518 = (Object) INDY_call518 ().invokeExact(x, s, i);
            Object o519 = (Object) INDY_call519 ().invokeExact(x, s, i);
            Object o520 = (Object) INDY_call520 ().invokeExact(x, s, i);
            Object o521 = (Object) INDY_call521 ().invokeExact(x, s, i);
            Object o522 = (Object) INDY_call522 ().invokeExact(x, s, i);
            Object o523 = (Object) INDY_call523 ().invokeExact(x, s, i);
            Object o524 = (Object) INDY_call524 ().invokeExact(x, s, i);
            Object o525 = (Object) INDY_call525 ().invokeExact(x, s, i);
            Object o526 = (Object) INDY_call526 ().invokeExact(x, s, i);
            Object o527 = (Object) INDY_call527 ().invokeExact(x, s, i);
            Object o528 = (Object) INDY_call528 ().invokeExact(x, s, i);
            Object o529 = (Object) INDY_call529 ().invokeExact(x, s, i);
            Object o530 = (Object) INDY_call530 ().invokeExact(x, s, i);
            Object o531 = (Object) INDY_call531 ().invokeExact(x, s, i);
            Object o532 = (Object) INDY_call532 ().invokeExact(x, s, i);
            Object o533 = (Object) INDY_call533 ().invokeExact(x, s, i);
            Object o534 = (Object) INDY_call534 ().invokeExact(x, s, i);
            Object o535 = (Object) INDY_call535 ().invokeExact(x, s, i);
            Object o536 = (Object) INDY_call536 ().invokeExact(x, s, i);
            Object o537 = (Object) INDY_call537 ().invokeExact(x, s, i);
            Object o538 = (Object) INDY_call538 ().invokeExact(x, s, i);
            Object o539 = (Object) INDY_call539 ().invokeExact(x, s, i);
            Object o540 = (Object) INDY_call540 ().invokeExact(x, s, i);
            Object o541 = (Object) INDY_call541 ().invokeExact(x, s, i);
            Object o542 = (Object) INDY_call542 ().invokeExact(x, s, i);
            Object o543 = (Object) INDY_call543 ().invokeExact(x, s, i);
            Object o544 = (Object) INDY_call544 ().invokeExact(x, s, i);
            Object o545 = (Object) INDY_call545 ().invokeExact(x, s, i);
            Object o546 = (Object) INDY_call546 ().invokeExact(x, s, i);
            Object o547 = (Object) INDY_call547 ().invokeExact(x, s, i);
            Object o548 = (Object) INDY_call548 ().invokeExact(x, s, i);
            Object o549 = (Object) INDY_call549 ().invokeExact(x, s, i);
            Object o550 = (Object) INDY_call550 ().invokeExact(x, s, i);
            Object o551 = (Object) INDY_call551 ().invokeExact(x, s, i);
            Object o552 = (Object) INDY_call552 ().invokeExact(x, s, i);
            Object o553 = (Object) INDY_call553 ().invokeExact(x, s, i);
            Object o554 = (Object) INDY_call554 ().invokeExact(x, s, i);
            Object o555 = (Object) INDY_call555 ().invokeExact(x, s, i);
            Object o556 = (Object) INDY_call556 ().invokeExact(x, s, i);
            Object o557 = (Object) INDY_call557 ().invokeExact(x, s, i);
            Object o558 = (Object) INDY_call558 ().invokeExact(x, s, i);
            Object o559 = (Object) INDY_call559 ().invokeExact(x, s, i);
            Object o560 = (Object) INDY_call560 ().invokeExact(x, s, i);
            Object o561 = (Object) INDY_call561 ().invokeExact(x, s, i);
            Object o562 = (Object) INDY_call562 ().invokeExact(x, s, i);
            Object o563 = (Object) INDY_call563 ().invokeExact(x, s, i);
            Object o564 = (Object) INDY_call564 ().invokeExact(x, s, i);
            Object o565 = (Object) INDY_call565 ().invokeExact(x, s, i);
            Object o566 = (Object) INDY_call566 ().invokeExact(x, s, i);
            Object o567 = (Object) INDY_call567 ().invokeExact(x, s, i);
            Object o568 = (Object) INDY_call568 ().invokeExact(x, s, i);
            Object o569 = (Object) INDY_call569 ().invokeExact(x, s, i);
            Object o570 = (Object) INDY_call570 ().invokeExact(x, s, i);
            Object o571 = (Object) INDY_call571 ().invokeExact(x, s, i);
            Object o572 = (Object) INDY_call572 ().invokeExact(x, s, i);
            Object o573 = (Object) INDY_call573 ().invokeExact(x, s, i);
            Object o574 = (Object) INDY_call574 ().invokeExact(x, s, i);
            Object o575 = (Object) INDY_call575 ().invokeExact(x, s, i);
            Object o576 = (Object) INDY_call576 ().invokeExact(x, s, i);
            Object o577 = (Object) INDY_call577 ().invokeExact(x, s, i);
            Object o578 = (Object) INDY_call578 ().invokeExact(x, s, i);
            Object o579 = (Object) INDY_call579 ().invokeExact(x, s, i);
            Object o580 = (Object) INDY_call580 ().invokeExact(x, s, i);
            Object o581 = (Object) INDY_call581 ().invokeExact(x, s, i);
            Object o582 = (Object) INDY_call582 ().invokeExact(x, s, i);
            Object o583 = (Object) INDY_call583 ().invokeExact(x, s, i);
            Object o584 = (Object) INDY_call584 ().invokeExact(x, s, i);
            Object o585 = (Object) INDY_call585 ().invokeExact(x, s, i);
            Object o586 = (Object) INDY_call586 ().invokeExact(x, s, i);
            Object o587 = (Object) INDY_call587 ().invokeExact(x, s, i);
            Object o588 = (Object) INDY_call588 ().invokeExact(x, s, i);
            Object o589 = (Object) INDY_call589 ().invokeExact(x, s, i);
            Object o590 = (Object) INDY_call590 ().invokeExact(x, s, i);
            Object o591 = (Object) INDY_call591 ().invokeExact(x, s, i);
            Object o592 = (Object) INDY_call592 ().invokeExact(x, s, i);
            Object o593 = (Object) INDY_call593 ().invokeExact(x, s, i);
            Object o594 = (Object) INDY_call594 ().invokeExact(x, s, i);
            Object o595 = (Object) INDY_call595 ().invokeExact(x, s, i);
            Object o596 = (Object) INDY_call596 ().invokeExact(x, s, i);
            Object o597 = (Object) INDY_call597 ().invokeExact(x, s, i);
            Object o598 = (Object) INDY_call598 ().invokeExact(x, s, i);
            Object o599 = (Object) INDY_call599 ().invokeExact(x, s, i);
            Object o600 = (Object) INDY_call600 ().invokeExact(x, s, i);
            Object o601 = (Object) INDY_call601 ().invokeExact(x, s, i);
            Object o602 = (Object) INDY_call602 ().invokeExact(x, s, i);
            Object o603 = (Object) INDY_call603 ().invokeExact(x, s, i);
            Object o604 = (Object) INDY_call604 ().invokeExact(x, s, i);
            Object o605 = (Object) INDY_call605 ().invokeExact(x, s, i);
            Object o606 = (Object) INDY_call606 ().invokeExact(x, s, i);
            Object o607 = (Object) INDY_call607 ().invokeExact(x, s, i);
            Object o608 = (Object) INDY_call608 ().invokeExact(x, s, i);
            Object o609 = (Object) INDY_call609 ().invokeExact(x, s, i);
            Object o610 = (Object) INDY_call610 ().invokeExact(x, s, i);
            Object o611 = (Object) INDY_call611 ().invokeExact(x, s, i);
            Object o612 = (Object) INDY_call612 ().invokeExact(x, s, i);
            Object o613 = (Object) INDY_call613 ().invokeExact(x, s, i);
            Object o614 = (Object) INDY_call614 ().invokeExact(x, s, i);
            Object o615 = (Object) INDY_call615 ().invokeExact(x, s, i);
            Object o616 = (Object) INDY_call616 ().invokeExact(x, s, i);
            Object o617 = (Object) INDY_call617 ().invokeExact(x, s, i);
            Object o618 = (Object) INDY_call618 ().invokeExact(x, s, i);
            Object o619 = (Object) INDY_call619 ().invokeExact(x, s, i);
            Object o620 = (Object) INDY_call620 ().invokeExact(x, s, i);
            Object o621 = (Object) INDY_call621 ().invokeExact(x, s, i);
            Object o622 = (Object) INDY_call622 ().invokeExact(x, s, i);
            Object o623 = (Object) INDY_call623 ().invokeExact(x, s, i);
            Object o624 = (Object) INDY_call624 ().invokeExact(x, s, i);
            Object o625 = (Object) INDY_call625 ().invokeExact(x, s, i);
            Object o626 = (Object) INDY_call626 ().invokeExact(x, s, i);
            Object o627 = (Object) INDY_call627 ().invokeExact(x, s, i);
            Object o628 = (Object) INDY_call628 ().invokeExact(x, s, i);
            Object o629 = (Object) INDY_call629 ().invokeExact(x, s, i);
            Object o630 = (Object) INDY_call630 ().invokeExact(x, s, i);
            Object o631 = (Object) INDY_call631 ().invokeExact(x, s, i);
            Object o632 = (Object) INDY_call632 ().invokeExact(x, s, i);
            Object o633 = (Object) INDY_call633 ().invokeExact(x, s, i);
            Object o634 = (Object) INDY_call634 ().invokeExact(x, s, i);
            Object o635 = (Object) INDY_call635 ().invokeExact(x, s, i);
            Object o636 = (Object) INDY_call636 ().invokeExact(x, s, i);
            Object o637 = (Object) INDY_call637 ().invokeExact(x, s, i);
            Object o638 = (Object) INDY_call638 ().invokeExact(x, s, i);
            Object o639 = (Object) INDY_call639 ().invokeExact(x, s, i);
            Object o640 = (Object) INDY_call640 ().invokeExact(x, s, i);
            Object o641 = (Object) INDY_call641 ().invokeExact(x, s, i);
            Object o642 = (Object) INDY_call642 ().invokeExact(x, s, i);
            Object o643 = (Object) INDY_call643 ().invokeExact(x, s, i);
            Object o644 = (Object) INDY_call644 ().invokeExact(x, s, i);
            Object o645 = (Object) INDY_call645 ().invokeExact(x, s, i);
            Object o646 = (Object) INDY_call646 ().invokeExact(x, s, i);
            Object o647 = (Object) INDY_call647 ().invokeExact(x, s, i);
            Object o648 = (Object) INDY_call648 ().invokeExact(x, s, i);
            Object o649 = (Object) INDY_call649 ().invokeExact(x, s, i);
            Object o650 = (Object) INDY_call650 ().invokeExact(x, s, i);
            Object o651 = (Object) INDY_call651 ().invokeExact(x, s, i);
            Object o652 = (Object) INDY_call652 ().invokeExact(x, s, i);
            Object o653 = (Object) INDY_call653 ().invokeExact(x, s, i);
            Object o654 = (Object) INDY_call654 ().invokeExact(x, s, i);
            Object o655 = (Object) INDY_call655 ().invokeExact(x, s, i);
            Object o656 = (Object) INDY_call656 ().invokeExact(x, s, i);
            Object o657 = (Object) INDY_call657 ().invokeExact(x, s, i);
            Object o658 = (Object) INDY_call658 ().invokeExact(x, s, i);
            Object o659 = (Object) INDY_call659 ().invokeExact(x, s, i);
            Object o660 = (Object) INDY_call660 ().invokeExact(x, s, i);
            Object o661 = (Object) INDY_call661 ().invokeExact(x, s, i);
            Object o662 = (Object) INDY_call662 ().invokeExact(x, s, i);
            Object o663 = (Object) INDY_call663 ().invokeExact(x, s, i);
            Object o664 = (Object) INDY_call664 ().invokeExact(x, s, i);
            Object o665 = (Object) INDY_call665 ().invokeExact(x, s, i);
            Object o666 = (Object) INDY_call666 ().invokeExact(x, s, i);
            Object o667 = (Object) INDY_call667 ().invokeExact(x, s, i);
            Object o668 = (Object) INDY_call668 ().invokeExact(x, s, i);
            Object o669 = (Object) INDY_call669 ().invokeExact(x, s, i);
            Object o670 = (Object) INDY_call670 ().invokeExact(x, s, i);
            Object o671 = (Object) INDY_call671 ().invokeExact(x, s, i);
            Object o672 = (Object) INDY_call672 ().invokeExact(x, s, i);
            Object o673 = (Object) INDY_call673 ().invokeExact(x, s, i);
            Object o674 = (Object) INDY_call674 ().invokeExact(x, s, i);
            Object o675 = (Object) INDY_call675 ().invokeExact(x, s, i);
            Object o676 = (Object) INDY_call676 ().invokeExact(x, s, i);
            Object o677 = (Object) INDY_call677 ().invokeExact(x, s, i);
            Object o678 = (Object) INDY_call678 ().invokeExact(x, s, i);
            Object o679 = (Object) INDY_call679 ().invokeExact(x, s, i);
            Object o680 = (Object) INDY_call680 ().invokeExact(x, s, i);
            Object o681 = (Object) INDY_call681 ().invokeExact(x, s, i);
            Object o682 = (Object) INDY_call682 ().invokeExact(x, s, i);
            Object o683 = (Object) INDY_call683 ().invokeExact(x, s, i);
            Object o684 = (Object) INDY_call684 ().invokeExact(x, s, i);
            Object o685 = (Object) INDY_call685 ().invokeExact(x, s, i);
            Object o686 = (Object) INDY_call686 ().invokeExact(x, s, i);
            Object o687 = (Object) INDY_call687 ().invokeExact(x, s, i);
            Object o688 = (Object) INDY_call688 ().invokeExact(x, s, i);
            Object o689 = (Object) INDY_call689 ().invokeExact(x, s, i);
            Object o690 = (Object) INDY_call690 ().invokeExact(x, s, i);
            Object o691 = (Object) INDY_call691 ().invokeExact(x, s, i);
            Object o692 = (Object) INDY_call692 ().invokeExact(x, s, i);
            Object o693 = (Object) INDY_call693 ().invokeExact(x, s, i);
            Object o694 = (Object) INDY_call694 ().invokeExact(x, s, i);
            Object o695 = (Object) INDY_call695 ().invokeExact(x, s, i);
            Object o696 = (Object) INDY_call696 ().invokeExact(x, s, i);
            Object o697 = (Object) INDY_call697 ().invokeExact(x, s, i);
            Object o698 = (Object) INDY_call698 ().invokeExact(x, s, i);
            Object o699 = (Object) INDY_call699 ().invokeExact(x, s, i);
            Object o700 = (Object) INDY_call700 ().invokeExact(x, s, i);
            Object o701 = (Object) INDY_call701 ().invokeExact(x, s, i);
            Object o702 = (Object) INDY_call702 ().invokeExact(x, s, i);
            Object o703 = (Object) INDY_call703 ().invokeExact(x, s, i);
            Object o704 = (Object) INDY_call704 ().invokeExact(x, s, i);
            Object o705 = (Object) INDY_call705 ().invokeExact(x, s, i);
            Object o706 = (Object) INDY_call706 ().invokeExact(x, s, i);
            Object o707 = (Object) INDY_call707 ().invokeExact(x, s, i);
            Object o708 = (Object) INDY_call708 ().invokeExact(x, s, i);
            Object o709 = (Object) INDY_call709 ().invokeExact(x, s, i);
            Object o710 = (Object) INDY_call710 ().invokeExact(x, s, i);
            Object o711 = (Object) INDY_call711 ().invokeExact(x, s, i);
            Object o712 = (Object) INDY_call712 ().invokeExact(x, s, i);
            Object o713 = (Object) INDY_call713 ().invokeExact(x, s, i);
            Object o714 = (Object) INDY_call714 ().invokeExact(x, s, i);
            Object o715 = (Object) INDY_call715 ().invokeExact(x, s, i);
            Object o716 = (Object) INDY_call716 ().invokeExact(x, s, i);
            Object o717 = (Object) INDY_call717 ().invokeExact(x, s, i);
            Object o718 = (Object) INDY_call718 ().invokeExact(x, s, i);
            Object o719 = (Object) INDY_call719 ().invokeExact(x, s, i);
            Object o720 = (Object) INDY_call720 ().invokeExact(x, s, i);
            Object o721 = (Object) INDY_call721 ().invokeExact(x, s, i);
            Object o722 = (Object) INDY_call722 ().invokeExact(x, s, i);
            Object o723 = (Object) INDY_call723 ().invokeExact(x, s, i);
            Object o724 = (Object) INDY_call724 ().invokeExact(x, s, i);
            Object o725 = (Object) INDY_call725 ().invokeExact(x, s, i);
            Object o726 = (Object) INDY_call726 ().invokeExact(x, s, i);
            Object o727 = (Object) INDY_call727 ().invokeExact(x, s, i);
            Object o728 = (Object) INDY_call728 ().invokeExact(x, s, i);
            Object o729 = (Object) INDY_call729 ().invokeExact(x, s, i);
            Object o730 = (Object) INDY_call730 ().invokeExact(x, s, i);
            Object o731 = (Object) INDY_call731 ().invokeExact(x, s, i);
            Object o732 = (Object) INDY_call732 ().invokeExact(x, s, i);
            Object o733 = (Object) INDY_call733 ().invokeExact(x, s, i);
            Object o734 = (Object) INDY_call734 ().invokeExact(x, s, i);
            Object o735 = (Object) INDY_call735 ().invokeExact(x, s, i);
            Object o736 = (Object) INDY_call736 ().invokeExact(x, s, i);
            Object o737 = (Object) INDY_call737 ().invokeExact(x, s, i);
            Object o738 = (Object) INDY_call738 ().invokeExact(x, s, i);
            Object o739 = (Object) INDY_call739 ().invokeExact(x, s, i);
            Object o740 = (Object) INDY_call740 ().invokeExact(x, s, i);
            Object o741 = (Object) INDY_call741 ().invokeExact(x, s, i);
            Object o742 = (Object) INDY_call742 ().invokeExact(x, s, i);
            Object o743 = (Object) INDY_call743 ().invokeExact(x, s, i);
            Object o744 = (Object) INDY_call744 ().invokeExact(x, s, i);
            Object o745 = (Object) INDY_call745 ().invokeExact(x, s, i);
            Object o746 = (Object) INDY_call746 ().invokeExact(x, s, i);
            Object o747 = (Object) INDY_call747 ().invokeExact(x, s, i);
            Object o748 = (Object) INDY_call748 ().invokeExact(x, s, i);
            Object o749 = (Object) INDY_call749 ().invokeExact(x, s, i);
            Object o750 = (Object) INDY_call750 ().invokeExact(x, s, i);
            Object o751 = (Object) INDY_call751 ().invokeExact(x, s, i);
            Object o752 = (Object) INDY_call752 ().invokeExact(x, s, i);
            Object o753 = (Object) INDY_call753 ().invokeExact(x, s, i);
            Object o754 = (Object) INDY_call754 ().invokeExact(x, s, i);
            Object o755 = (Object) INDY_call755 ().invokeExact(x, s, i);
            Object o756 = (Object) INDY_call756 ().invokeExact(x, s, i);
            Object o757 = (Object) INDY_call757 ().invokeExact(x, s, i);
            Object o758 = (Object) INDY_call758 ().invokeExact(x, s, i);
            Object o759 = (Object) INDY_call759 ().invokeExact(x, s, i);
            Object o760 = (Object) INDY_call760 ().invokeExact(x, s, i);
            Object o761 = (Object) INDY_call761 ().invokeExact(x, s, i);
            Object o762 = (Object) INDY_call762 ().invokeExact(x, s, i);
            Object o763 = (Object) INDY_call763 ().invokeExact(x, s, i);
            Object o764 = (Object) INDY_call764 ().invokeExact(x, s, i);
            Object o765 = (Object) INDY_call765 ().invokeExact(x, s, i);
            Object o766 = (Object) INDY_call766 ().invokeExact(x, s, i);
            Object o767 = (Object) INDY_call767 ().invokeExact(x, s, i);
            Object o768 = (Object) INDY_call768 ().invokeExact(x, s, i);
            Object o769 = (Object) INDY_call769 ().invokeExact(x, s, i);
            Object o770 = (Object) INDY_call770 ().invokeExact(x, s, i);
            Object o771 = (Object) INDY_call771 ().invokeExact(x, s, i);
            Object o772 = (Object) INDY_call772 ().invokeExact(x, s, i);
            Object o773 = (Object) INDY_call773 ().invokeExact(x, s, i);
            Object o774 = (Object) INDY_call774 ().invokeExact(x, s, i);
            Object o775 = (Object) INDY_call775 ().invokeExact(x, s, i);
            Object o776 = (Object) INDY_call776 ().invokeExact(x, s, i);
            Object o777 = (Object) INDY_call777 ().invokeExact(x, s, i);
            Object o778 = (Object) INDY_call778 ().invokeExact(x, s, i);
            Object o779 = (Object) INDY_call779 ().invokeExact(x, s, i);
            Object o780 = (Object) INDY_call780 ().invokeExact(x, s, i);
            Object o781 = (Object) INDY_call781 ().invokeExact(x, s, i);
            Object o782 = (Object) INDY_call782 ().invokeExact(x, s, i);
            Object o783 = (Object) INDY_call783 ().invokeExact(x, s, i);
            Object o784 = (Object) INDY_call784 ().invokeExact(x, s, i);
            Object o785 = (Object) INDY_call785 ().invokeExact(x, s, i);
            Object o786 = (Object) INDY_call786 ().invokeExact(x, s, i);
            Object o787 = (Object) INDY_call787 ().invokeExact(x, s, i);
            Object o788 = (Object) INDY_call788 ().invokeExact(x, s, i);
            Object o789 = (Object) INDY_call789 ().invokeExact(x, s, i);
            Object o790 = (Object) INDY_call790 ().invokeExact(x, s, i);
            Object o791 = (Object) INDY_call791 ().invokeExact(x, s, i);
            Object o792 = (Object) INDY_call792 ().invokeExact(x, s, i);
            Object o793 = (Object) INDY_call793 ().invokeExact(x, s, i);
            Object o794 = (Object) INDY_call794 ().invokeExact(x, s, i);
            Object o795 = (Object) INDY_call795 ().invokeExact(x, s, i);
            Object o796 = (Object) INDY_call796 ().invokeExact(x, s, i);
            Object o797 = (Object) INDY_call797 ().invokeExact(x, s, i);
            Object o798 = (Object) INDY_call798 ().invokeExact(x, s, i);
            Object o799 = (Object) INDY_call799 ().invokeExact(x, s, i);
            Object o800 = (Object) INDY_call800 ().invokeExact(x, s, i);
            Object o801 = (Object) INDY_call801 ().invokeExact(x, s, i);
            Object o802 = (Object) INDY_call802 ().invokeExact(x, s, i);
            Object o803 = (Object) INDY_call803 ().invokeExact(x, s, i);
            Object o804 = (Object) INDY_call804 ().invokeExact(x, s, i);
            Object o805 = (Object) INDY_call805 ().invokeExact(x, s, i);
            Object o806 = (Object) INDY_call806 ().invokeExact(x, s, i);
            Object o807 = (Object) INDY_call807 ().invokeExact(x, s, i);
            Object o808 = (Object) INDY_call808 ().invokeExact(x, s, i);
            Object o809 = (Object) INDY_call809 ().invokeExact(x, s, i);
            Object o810 = (Object) INDY_call810 ().invokeExact(x, s, i);
            Object o811 = (Object) INDY_call811 ().invokeExact(x, s, i);
            Object o812 = (Object) INDY_call812 ().invokeExact(x, s, i);
            Object o813 = (Object) INDY_call813 ().invokeExact(x, s, i);
            Object o814 = (Object) INDY_call814 ().invokeExact(x, s, i);
            Object o815 = (Object) INDY_call815 ().invokeExact(x, s, i);
            Object o816 = (Object) INDY_call816 ().invokeExact(x, s, i);
            Object o817 = (Object) INDY_call817 ().invokeExact(x, s, i);
            Object o818 = (Object) INDY_call818 ().invokeExact(x, s, i);
            Object o819 = (Object) INDY_call819 ().invokeExact(x, s, i);
            Object o820 = (Object) INDY_call820 ().invokeExact(x, s, i);
            Object o821 = (Object) INDY_call821 ().invokeExact(x, s, i);
            Object o822 = (Object) INDY_call822 ().invokeExact(x, s, i);
            Object o823 = (Object) INDY_call823 ().invokeExact(x, s, i);
            Object o824 = (Object) INDY_call824 ().invokeExact(x, s, i);
            Object o825 = (Object) INDY_call825 ().invokeExact(x, s, i);
            Object o826 = (Object) INDY_call826 ().invokeExact(x, s, i);
            Object o827 = (Object) INDY_call827 ().invokeExact(x, s, i);
            Object o828 = (Object) INDY_call828 ().invokeExact(x, s, i);
            Object o829 = (Object) INDY_call829 ().invokeExact(x, s, i);
            Object o830 = (Object) INDY_call830 ().invokeExact(x, s, i);
            Object o831 = (Object) INDY_call831 ().invokeExact(x, s, i);
            Object o832 = (Object) INDY_call832 ().invokeExact(x, s, i);
            Object o833 = (Object) INDY_call833 ().invokeExact(x, s, i);
            Object o834 = (Object) INDY_call834 ().invokeExact(x, s, i);
            Object o835 = (Object) INDY_call835 ().invokeExact(x, s, i);
            Object o836 = (Object) INDY_call836 ().invokeExact(x, s, i);
            Object o837 = (Object) INDY_call837 ().invokeExact(x, s, i);
            Object o838 = (Object) INDY_call838 ().invokeExact(x, s, i);
            Object o839 = (Object) INDY_call839 ().invokeExact(x, s, i);
            Object o840 = (Object) INDY_call840 ().invokeExact(x, s, i);
            Object o841 = (Object) INDY_call841 ().invokeExact(x, s, i);
            Object o842 = (Object) INDY_call842 ().invokeExact(x, s, i);
            Object o843 = (Object) INDY_call843 ().invokeExact(x, s, i);
            Object o844 = (Object) INDY_call844 ().invokeExact(x, s, i);
            Object o845 = (Object) INDY_call845 ().invokeExact(x, s, i);
            Object o846 = (Object) INDY_call846 ().invokeExact(x, s, i);
            Object o847 = (Object) INDY_call847 ().invokeExact(x, s, i);
            Object o848 = (Object) INDY_call848 ().invokeExact(x, s, i);
            Object o849 = (Object) INDY_call849 ().invokeExact(x, s, i);
            Object o850 = (Object) INDY_call850 ().invokeExact(x, s, i);
            Object o851 = (Object) INDY_call851 ().invokeExact(x, s, i);
            Object o852 = (Object) INDY_call852 ().invokeExact(x, s, i);
            Object o853 = (Object) INDY_call853 ().invokeExact(x, s, i);
            Object o854 = (Object) INDY_call854 ().invokeExact(x, s, i);
            Object o855 = (Object) INDY_call855 ().invokeExact(x, s, i);
            Object o856 = (Object) INDY_call856 ().invokeExact(x, s, i);
            Object o857 = (Object) INDY_call857 ().invokeExact(x, s, i);
            Object o858 = (Object) INDY_call858 ().invokeExact(x, s, i);
            Object o859 = (Object) INDY_call859 ().invokeExact(x, s, i);
            Object o860 = (Object) INDY_call860 ().invokeExact(x, s, i);
            Object o861 = (Object) INDY_call861 ().invokeExact(x, s, i);
            Object o862 = (Object) INDY_call862 ().invokeExact(x, s, i);
            Object o863 = (Object) INDY_call863 ().invokeExact(x, s, i);
            Object o864 = (Object) INDY_call864 ().invokeExact(x, s, i);
            Object o865 = (Object) INDY_call865 ().invokeExact(x, s, i);
            Object o866 = (Object) INDY_call866 ().invokeExact(x, s, i);
            Object o867 = (Object) INDY_call867 ().invokeExact(x, s, i);
            Object o868 = (Object) INDY_call868 ().invokeExact(x, s, i);
            Object o869 = (Object) INDY_call869 ().invokeExact(x, s, i);
            Object o870 = (Object) INDY_call870 ().invokeExact(x, s, i);
            Object o871 = (Object) INDY_call871 ().invokeExact(x, s, i);
            Object o872 = (Object) INDY_call872 ().invokeExact(x, s, i);
            Object o873 = (Object) INDY_call873 ().invokeExact(x, s, i);
            Object o874 = (Object) INDY_call874 ().invokeExact(x, s, i);
            Object o875 = (Object) INDY_call875 ().invokeExact(x, s, i);
            Object o876 = (Object) INDY_call876 ().invokeExact(x, s, i);
            Object o877 = (Object) INDY_call877 ().invokeExact(x, s, i);
            Object o878 = (Object) INDY_call878 ().invokeExact(x, s, i);
            Object o879 = (Object) INDY_call879 ().invokeExact(x, s, i);
            Object o880 = (Object) INDY_call880 ().invokeExact(x, s, i);
            Object o881 = (Object) INDY_call881 ().invokeExact(x, s, i);
            Object o882 = (Object) INDY_call882 ().invokeExact(x, s, i);
            Object o883 = (Object) INDY_call883 ().invokeExact(x, s, i);
            Object o884 = (Object) INDY_call884 ().invokeExact(x, s, i);
            Object o885 = (Object) INDY_call885 ().invokeExact(x, s, i);
            Object o886 = (Object) INDY_call886 ().invokeExact(x, s, i);
            Object o887 = (Object) INDY_call887 ().invokeExact(x, s, i);
            Object o888 = (Object) INDY_call888 ().invokeExact(x, s, i);
            Object o889 = (Object) INDY_call889 ().invokeExact(x, s, i);
            Object o890 = (Object) INDY_call890 ().invokeExact(x, s, i);
            Object o891 = (Object) INDY_call891 ().invokeExact(x, s, i);
            Object o892 = (Object) INDY_call892 ().invokeExact(x, s, i);
            Object o893 = (Object) INDY_call893 ().invokeExact(x, s, i);
            Object o894 = (Object) INDY_call894 ().invokeExact(x, s, i);
            Object o895 = (Object) INDY_call895 ().invokeExact(x, s, i);
            Object o896 = (Object) INDY_call896 ().invokeExact(x, s, i);
            Object o897 = (Object) INDY_call897 ().invokeExact(x, s, i);
            Object o898 = (Object) INDY_call898 ().invokeExact(x, s, i);
            Object o899 = (Object) INDY_call899 ().invokeExact(x, s, i);
            Object o900 = (Object) INDY_call900 ().invokeExact(x, s, i);
            Object o901 = (Object) INDY_call901 ().invokeExact(x, s, i);
            Object o902 = (Object) INDY_call902 ().invokeExact(x, s, i);
            Object o903 = (Object) INDY_call903 ().invokeExact(x, s, i);
            Object o904 = (Object) INDY_call904 ().invokeExact(x, s, i);
            Object o905 = (Object) INDY_call905 ().invokeExact(x, s, i);
            Object o906 = (Object) INDY_call906 ().invokeExact(x, s, i);
            Object o907 = (Object) INDY_call907 ().invokeExact(x, s, i);
            Object o908 = (Object) INDY_call908 ().invokeExact(x, s, i);
            Object o909 = (Object) INDY_call909 ().invokeExact(x, s, i);
            Object o910 = (Object) INDY_call910 ().invokeExact(x, s, i);
            Object o911 = (Object) INDY_call911 ().invokeExact(x, s, i);
            Object o912 = (Object) INDY_call912 ().invokeExact(x, s, i);
            Object o913 = (Object) INDY_call913 ().invokeExact(x, s, i);
            Object o914 = (Object) INDY_call914 ().invokeExact(x, s, i);
            Object o915 = (Object) INDY_call915 ().invokeExact(x, s, i);
            Object o916 = (Object) INDY_call916 ().invokeExact(x, s, i);
            Object o917 = (Object) INDY_call917 ().invokeExact(x, s, i);
            Object o918 = (Object) INDY_call918 ().invokeExact(x, s, i);
            Object o919 = (Object) INDY_call919 ().invokeExact(x, s, i);
            Object o920 = (Object) INDY_call920 ().invokeExact(x, s, i);
            Object o921 = (Object) INDY_call921 ().invokeExact(x, s, i);
            Object o922 = (Object) INDY_call922 ().invokeExact(x, s, i);
            Object o923 = (Object) INDY_call923 ().invokeExact(x, s, i);
            Object o924 = (Object) INDY_call924 ().invokeExact(x, s, i);
            Object o925 = (Object) INDY_call925 ().invokeExact(x, s, i);
            Object o926 = (Object) INDY_call926 ().invokeExact(x, s, i);
            Object o927 = (Object) INDY_call927 ().invokeExact(x, s, i);
            Object o928 = (Object) INDY_call928 ().invokeExact(x, s, i);
            Object o929 = (Object) INDY_call929 ().invokeExact(x, s, i);
            Object o930 = (Object) INDY_call930 ().invokeExact(x, s, i);
            Object o931 = (Object) INDY_call931 ().invokeExact(x, s, i);
            Object o932 = (Object) INDY_call932 ().invokeExact(x, s, i);
            Object o933 = (Object) INDY_call933 ().invokeExact(x, s, i);
            Object o934 = (Object) INDY_call934 ().invokeExact(x, s, i);
            Object o935 = (Object) INDY_call935 ().invokeExact(x, s, i);
            Object o936 = (Object) INDY_call936 ().invokeExact(x, s, i);
            Object o937 = (Object) INDY_call937 ().invokeExact(x, s, i);
            Object o938 = (Object) INDY_call938 ().invokeExact(x, s, i);
            Object o939 = (Object) INDY_call939 ().invokeExact(x, s, i);
            Object o940 = (Object) INDY_call940 ().invokeExact(x, s, i);
            Object o941 = (Object) INDY_call941 ().invokeExact(x, s, i);
            Object o942 = (Object) INDY_call942 ().invokeExact(x, s, i);
            Object o943 = (Object) INDY_call943 ().invokeExact(x, s, i);
            Object o944 = (Object) INDY_call944 ().invokeExact(x, s, i);
            Object o945 = (Object) INDY_call945 ().invokeExact(x, s, i);
            Object o946 = (Object) INDY_call946 ().invokeExact(x, s, i);
            Object o947 = (Object) INDY_call947 ().invokeExact(x, s, i);
            Object o948 = (Object) INDY_call948 ().invokeExact(x, s, i);
            Object o949 = (Object) INDY_call949 ().invokeExact(x, s, i);
            Object o950 = (Object) INDY_call950 ().invokeExact(x, s, i);
            Object o951 = (Object) INDY_call951 ().invokeExact(x, s, i);
            Object o952 = (Object) INDY_call952 ().invokeExact(x, s, i);
            Object o953 = (Object) INDY_call953 ().invokeExact(x, s, i);
            Object o954 = (Object) INDY_call954 ().invokeExact(x, s, i);
            Object o955 = (Object) INDY_call955 ().invokeExact(x, s, i);
            Object o956 = (Object) INDY_call956 ().invokeExact(x, s, i);
            Object o957 = (Object) INDY_call957 ().invokeExact(x, s, i);
            Object o958 = (Object) INDY_call958 ().invokeExact(x, s, i);
            Object o959 = (Object) INDY_call959 ().invokeExact(x, s, i);
            Object o960 = (Object) INDY_call960 ().invokeExact(x, s, i);
            Object o961 = (Object) INDY_call961 ().invokeExact(x, s, i);
            Object o962 = (Object) INDY_call962 ().invokeExact(x, s, i);
            Object o963 = (Object) INDY_call963 ().invokeExact(x, s, i);
            Object o964 = (Object) INDY_call964 ().invokeExact(x, s, i);
            Object o965 = (Object) INDY_call965 ().invokeExact(x, s, i);
            Object o966 = (Object) INDY_call966 ().invokeExact(x, s, i);
            Object o967 = (Object) INDY_call967 ().invokeExact(x, s, i);
            Object o968 = (Object) INDY_call968 ().invokeExact(x, s, i);
            Object o969 = (Object) INDY_call969 ().invokeExact(x, s, i);
            Object o970 = (Object) INDY_call970 ().invokeExact(x, s, i);
            Object o971 = (Object) INDY_call971 ().invokeExact(x, s, i);
            Object o972 = (Object) INDY_call972 ().invokeExact(x, s, i);
            Object o973 = (Object) INDY_call973 ().invokeExact(x, s, i);
            Object o974 = (Object) INDY_call974 ().invokeExact(x, s, i);
            Object o975 = (Object) INDY_call975 ().invokeExact(x, s, i);
            Object o976 = (Object) INDY_call976 ().invokeExact(x, s, i);
            Object o977 = (Object) INDY_call977 ().invokeExact(x, s, i);
            Object o978 = (Object) INDY_call978 ().invokeExact(x, s, i);
            Object o979 = (Object) INDY_call979 ().invokeExact(x, s, i);
            Object o980 = (Object) INDY_call980 ().invokeExact(x, s, i);
            Object o981 = (Object) INDY_call981 ().invokeExact(x, s, i);
            Object o982 = (Object) INDY_call982 ().invokeExact(x, s, i);
            Object o983 = (Object) INDY_call983 ().invokeExact(x, s, i);
            Object o984 = (Object) INDY_call984 ().invokeExact(x, s, i);
            Object o985 = (Object) INDY_call985 ().invokeExact(x, s, i);
            Object o986 = (Object) INDY_call986 ().invokeExact(x, s, i);
            Object o987 = (Object) INDY_call987 ().invokeExact(x, s, i);
            Object o988 = (Object) INDY_call988 ().invokeExact(x, s, i);
            Object o989 = (Object) INDY_call989 ().invokeExact(x, s, i);
            Object o990 = (Object) INDY_call990 ().invokeExact(x, s, i);
            Object o991 = (Object) INDY_call991 ().invokeExact(x, s, i);
            Object o992 = (Object) INDY_call992 ().invokeExact(x, s, i);
            Object o993 = (Object) INDY_call993 ().invokeExact(x, s, i);
            Object o994 = (Object) INDY_call994 ().invokeExact(x, s, i);
            Object o995 = (Object) INDY_call995 ().invokeExact(x, s, i);
            Object o996 = (Object) INDY_call996 ().invokeExact(x, s, i);
            Object o997 = (Object) INDY_call997 ().invokeExact(x, s, i);
            Object o998 = (Object) INDY_call998 ().invokeExact(x, s, i);
            Object o999 = (Object) INDY_call999 ().invokeExact(x, s, i);
        }

        return true;
    }

    protected void finalizeTest(boolean ok) throws Throwable {
        String msg = "Target was called " + _counter.get() + " times of " + _expectedTargetCalls.get();
        if ( ok && _counter.get() != _expectedTargetCalls.get() )
            throw new Failure(msg);
        else
            Env.display(msg);
    }

    public static void main(String[] args) { MlvmTest.launch(args); }
}
