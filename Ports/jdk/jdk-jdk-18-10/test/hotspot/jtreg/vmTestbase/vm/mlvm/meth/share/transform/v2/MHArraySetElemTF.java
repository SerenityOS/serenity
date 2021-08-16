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
import java.lang.reflect.Array;

import vm.mlvm.meth.share.Argument;
import vm.mlvm.meth.share.MHUtils;

public class MHArraySetElemTF extends MHNullaryTF {

    private final Argument arrayArg;
    private final Argument idxArg;
    private final int idx;
    private final Argument newValueArg;

    public MHArraySetElemTF(Argument array, Argument idxArg, Argument newValue) {
        this.arrayArg = array;
        this.idxArg = idxArg;
        this.idx = (Integer) idxArg.getValue();
        this.newValueArg = newValue;
    }

    @Override
    protected void check() throws IllegalArgumentException {
        if ( ! this.arrayArg.getType().isArray() )
            throw new IllegalArgumentException("Argument " + this.arrayArg + " should be an array!");

        if ( ! this.idxArg.getType().equals(int.class) )
            throw new IllegalArgumentException("Argument " + this.idxArg + " should be of type int!");

        if ( this.idx < 0 || this.idx >= Array.getLength(this.arrayArg.getValue()) )
            throw new IllegalArgumentException("Index " + this.idx + " is out of bounds for array " + this.arrayArg);

        MHUtils.assertAssignableType("Can't assign new value to array", this.arrayArg.getType().getComponentType(), this.newValueArg.getType());
    }

    @Override
    protected Argument computeRetVal() {
        return new Argument(void.class, null);
    }

    @Override
    protected Argument[] computeInboundArgs() {
        return new Argument[] { this.arrayArg, this.idxArg, this.newValueArg };
    }

    @Override
    protected MethodHandle computeInboundMH() {
        return MethodHandles.arrayElementSetter(this.arrayArg.getType());
    }

    @Override
    protected String getName() {
        return "arrayElementSetter";
    }

    @Override
    protected String getDescription() {
        return "array=" + this.arrayArg + "; idx=" + this.idxArg + "; newValue=" + this.newValueArg;
    }

}
