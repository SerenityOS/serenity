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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.WrongMethodTypeException;

import vm.mlvm.meth.share.Argument;
import vm.mlvm.meth.share.MHUtils;

public class MHFilterRetValTF extends MHNaryTF {

    protected final MHCall target, filter;

    public MHFilterRetValTF(MHCall target, MHCall filter) {
        this.target = target;
        this.filter = filter;
    }

    @Override
    protected void check() throws IllegalArgumentException {
        if ( this.filter.getArgs().length != 1 )
            throw new WrongMethodTypeException("Filter should have exactly one argument, but has: " + this.filter.getArgs());

        MHUtils.assertAssignableType("target return type to filter parameter",
                this.filter.getArgs()[0].getType(), this.target.getRetVal().getType());
    }

    @Override
    protected Argument computeRetVal() {
        return this.filter.getRetVal();
    }

    @Override
    protected Argument[] computeInboundArgs() {
        return this.target.getArgs();
    }

    @Override
    protected MethodHandle computeInboundMH() {
        return MethodHandles.filterReturnValue(target.getTargetMH(), filter.getTargetMH());
    }

    @Override
    public MHCall[] getOutboundCalls() {
        return new MHCall[] { target, filter };
    }

    @Override
    protected String getName() {
        return "filterReturnValue";
    }

    @Override
    protected String getDescription() {
        return "filter=" + this.filter;
    }
}
