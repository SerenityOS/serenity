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
import java.util.Arrays;

import nsk.share.test.TestUtils;
import vm.mlvm.meth.share.Argument;
import vm.mlvm.meth.share.MHUtils;

public class MHFoldTF extends MHNaryTF {

    protected final MHCall _combiner, _target;

    public MHFoldTF(MHCall target, MHCall combiner) {
        _target = target;
        _combiner = combiner;
    }

    @Override
    protected void check() throws IllegalArgumentException {
        Argument[] targetArgs = _target.getArgs();
        Argument[] combinerArgs = _combiner.getArgs();

        MHUtils.assertAssignableType(
                "combiner result assignable to parameter 0",
                targetArgs[0].getType(),
                _combiner.getRetVal().getType());

        for ( int i = 0; i < combinerArgs.length; i++ ) {
            MHUtils.assertAssignableType(
                    "combiner parameter " + i + " assignable to target parameter " + (i + 1),
                    combinerArgs[i].getType(),
                    targetArgs[i + 1].getType());
        }
    }

    @Override
    protected Argument computeRetVal() {
        return _target.getRetVal();
    }

    @Override
    protected Argument[] computeInboundArgs() {
        return TestUtils.cdr(_target.getArgs());
    }

    @Override
    protected MethodHandle computeInboundMH() {
        return MethodHandles.foldArguments(_target.getTargetMH(), _combiner.getTargetMH());
    }

    @Override
    public MHCall[] getOutboundCalls() {
        return new MHCall[] { _target, _combiner };
    }

    @Override
    protected String getName() {
        return "foldArguments";
    }

    @Override
    protected String getDescription() {
        return "combiner=" + _combiner;
    }
}
