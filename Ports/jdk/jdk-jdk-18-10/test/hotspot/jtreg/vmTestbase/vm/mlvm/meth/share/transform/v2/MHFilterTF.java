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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import nsk.share.test.TestUtils;

import vm.mlvm.meth.share.Argument;
import vm.mlvm.meth.share.MHUtils;

public class MHFilterTF extends MHNaryTF {

    protected final MHCall _target, _filters[];
    protected final int _pos;

    public MHFilterTF(MHCall target, int pos, MHCall[] filters) {
        _target = target;
        _pos = pos;
        _filters = filters;
    }

    @Override
    protected void check() throws IllegalArgumentException {
        Argument[] targetArgs = _target.getArgs();
        for ( int i = 0; i < _filters.length; i++ ) {
            MHCall f = _filters[i];
            if ( f == null )
                continue;

            int p = i + _pos;

            if ( f.getArgs().length != 1 )
                throw new WrongMethodTypeException("Filter " + i + " should have exactly one argument, but has: " + f.getArgs());

            MHUtils.assertAssignableType("filter return type to target parameter " + i,
                    targetArgs[p].getType(),
                    f.getRetVal().getType());
        }
    }

    @Override
    protected Argument computeRetVal() {
        return _target.getRetVal();
    }

    @Override
    protected Argument[] computeInboundArgs() {
        Argument[] result = _target.getArgs().clone();

        for ( int i = 0; i < _filters.length; i++ ) {
            MHCall f = _filters[i];
            if ( f != null )
                result[i + _pos] = f.getArgs()[0];
        }

        return result;
    }

    @Override
    protected MethodHandle computeInboundMH() {
        MethodHandle[] filterMHs = new MethodHandle[_filters.length];
        for ( int i = 0; i < _filters.length; i++ ) {
            MHCall f = _filters[i];
            if ( f != null )
                filterMHs[i] = f.getTargetMH();
        }
        return MethodHandles.filterArguments(_target.getTargetMH(), _pos, filterMHs);
    }

    @Override
    public MHCall[] getOutboundCalls() {
        Set<MHCall> calls = new HashSet<MHCall>();
        calls.add(_target);
        calls.addAll(Arrays.asList(_filters));
        calls.remove(null);
        return calls.toArray(new MHCall[0]);
    }

    @Override
    protected String getName() {
        return "filterArguments";
    }

    @Override
    protected String getDescription() {
        return "pos=" + _pos + "; filters=" + Arrays.toString(_filters);
    }
}
