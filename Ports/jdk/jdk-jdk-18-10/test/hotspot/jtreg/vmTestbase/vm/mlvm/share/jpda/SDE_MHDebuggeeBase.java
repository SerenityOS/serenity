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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import vm.mlvm.meth.share.Argument;
import vm.mlvm.meth.share.MHTransformationGen;
import vm.mlvm.meth.share.RandomArgumentsGen;
import vm.mlvm.meth.share.transform.v2.MHMacroTF;
import vm.mlvm.share.Env;
import vm.mlvm.share.Stratum;

@Stratum(stratumName="Logo", stratumSourceFileName="SDE_MHDebuggeeBase.logo")
public class SDE_MHDebuggeeBase extends Debuggee {

    private static final int ARG_NUM = 0;

    private MethodHandle _mh;
    public int _mhInvoked;
    public int _mhTargetInvoked;
    public int _plainInvoked;
    public int _plainTargetInvoked;

    public Object plainTarget(int i, String s, Float f) throws Throwable {
Stratum_Logo_50_PLAIN:
        _plainTargetInvoked++;
        hangUpIfNeeded("plainTarget");
        return null;
    }

    private static Float f = 1.0F;

    public Object warmupPlain() throws Throwable {
        return plainTarget(1, "abc", f);
    }

    public Object invokePlain() throws Throwable {
        Object o;
Stratum_Logo_40_INVOKE_PLAIN:
        o = plainTarget(1, "abc", f);
        _plainInvoked++;
        return o;
    }

    public Object mhTarget(int i, String s, Float f) throws Throwable {
Stratum_Logo_30_MH:
        hangUpIfNeeded("mhTarget");
        _mhTargetInvoked++;
        return null;
    }

    public Object warmupMH() throws Throwable {
        return _mh.invokeExact();
    }

    public Object invokeMH() throws Throwable {
        Object o;
Stratum_Logo_20_INVOKE_MH:
        o = _mh.invokeExact();
        _mhInvoked++;
        return o;
    }

    public void stop() throws InterruptedException {
Stratum_Logo_60_END:
        hangUpIfNeeded("stop");
    }

    @Override
    public void startUp() throws Throwable {
        final MethodHandle mh = MethodHandles.lookup().findVirtual(
                SDE_MHDebuggeeBase.class,
                "mhTarget",
                MethodType.methodType(Object.class, int.class, String.class,
                        Float.class));

        MHMacroTF sequence = MHTransformationGen.createSequence(
                new Argument(Object.class, null), this, mh,
                RandomArgumentsGen.createRandomArgs(true, mh.type()));

        MHTransformationGen.transformToMatchArgsNum(sequence, ARG_NUM, ARG_NUM);

        _mh = sequence.computeInboundCall().getTargetMH();
    }

    @Override
    public void warmUp() throws Throwable {
        warmupMH();
        warmupPlain();
    }

    @Override
    public boolean runDebuggee() throws Throwable {
Stratum_Logo_10_BEGIN:
        invokeMH();
        invokePlain();
        stop();

        Env.traceNormal("MH target invoked = " + _mhTargetInvoked + "\n"
                      + "MH invoked = " + _mhInvoked + "\n"
                      + "Plain target invoked = " + _plainTargetInvoked + "\n"
                      + "Plain invoked = " + _plainInvoked);

        long targetInvocationCount = getWarmupsCount();
        return _mhInvoked == 1 && _plainInvoked == 1
                && _mhTargetInvoked == targetInvocationCount
                && _plainTargetInvoked == targetInvocationCount;
    }
}
