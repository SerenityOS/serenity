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
import java.lang.invoke.MethodType;

import vm.mlvm.meth.share.Argument;
import vm.mlvm.meth.share.Arguments;
import vm.mlvm.meth.share.TestTypes;

public abstract class MHCastTF extends MHBasicUnaryTF {

    protected final Argument[] newArgs;
    protected final Argument newRetVal;
    protected final MethodType newMT;

    protected MHCastTF(MHCall target, Class<?> newRetType, Class<?>[] newArgTypes) {
        super(target);

        Argument[] targetArgs = target.getArgs();

        if ( newArgTypes.length != targetArgs.length )
            throw new IllegalArgumentException("newArgTypes length (" + newArgTypes.length + ") should be equal to argument count (" + targetArgs.length + ")");

        this.newArgs = new Argument[newArgTypes.length];
        for ( int i = 0; i < this.newArgs.length; i++ ) {
            if ( ! TestTypes.canConvertType(targetArgs[i].getType(), this.newArgs[i].getType(), false) )
                throw new IllegalArgumentException("Can't convert argument #" + i + " from [" + targetArgs[i].getType() + " to [" + this.newArgs[i].getType());

            this.newArgs[i] = convert(targetArgs[i], newArgTypes[i], false);
        }

        this.newRetVal = convert(target.getRetVal(), newRetType, true);

        this.newMT = MethodType.methodType(this.newRetVal.getType(), Arguments.typesArray(this.newArgs));
    }

    protected abstract Argument convert(Argument argument, Class<?> newClass, boolean isRetType);

    @Override
    protected abstract MethodHandle computeInboundMH(MethodHandle targetMH) throws NoSuchMethodException, IllegalAccessException;

    @Override
    protected Argument[] computeInboundArgs(Argument[] targetArgs) {
        return this.newArgs;
    }

    @Override
    protected Argument computeRetVal(Argument targetRetVal) {
        return this.newRetVal;
    }

    @Override
    protected String getDescription() {
        return "newRetVal=[" + this.newRetVal + "]; newArgs=[" + this.newArgs + "]";
    }

}
