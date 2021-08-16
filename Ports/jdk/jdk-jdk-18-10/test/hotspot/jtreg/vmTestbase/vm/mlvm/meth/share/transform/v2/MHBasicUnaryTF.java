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


package vm.mlvm.meth.share.transform.v2;

import java.lang.invoke.MethodHandle;

import vm.mlvm.meth.share.Argument;

public abstract class MHBasicUnaryTF extends MHUnaryTF {

    protected MHBasicUnaryTF(final MHCall target) {
        super(target);
    }

    // Syntax sugar

    @Override
    protected void check() throws IllegalArgumentException {
        check(getTargetCall().getArgs());
    }

    @Override
    protected Argument computeRetVal() {
        return computeRetVal(getTargetCall().getRetVal());
    }

    @Override
    protected Argument[] computeInboundArgs() {
        return computeInboundArgs(getTargetCall().getArgs());
    }

    @Override
    protected MethodHandle computeInboundMH() throws NoSuchMethodException, IllegalAccessException {
        return computeInboundMH(getTargetCall().getTargetMH());
    }

    protected void check(Argument[] targetArgs) throws IllegalArgumentException {
    }

    protected Argument computeRetVal(Argument targetRetVal) {
        return targetRetVal;
    }

    protected Argument[] computeInboundArgs(Argument[] targetArgs) {
        return targetArgs;
    }

    protected MethodHandle computeInboundMH(MethodHandle targetMH) throws NoSuchMethodException, IllegalAccessException {
        return targetMH;
    }
}
