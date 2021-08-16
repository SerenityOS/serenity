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

import nsk.share.test.TestUtils;
import vm.mlvm.meth.share.Argument;
import vm.mlvm.meth.share.Arguments;
import vm.mlvm.meth.share.MHUtils;

public class MHInsertTF extends MHInsertOrDropTF {

    protected final boolean _canDropProtected;

    public MHInsertTF(MHCall target, int pos, Argument[] values, boolean canDropProtected) {
        super(target, pos, values);
        _canDropProtected = canDropProtected;
    }

    @Override
    protected void check(Argument[] targetArgs) throws IllegalArgumentException {
        super.check(targetArgs);

        for (int i = this.pos; i < Math.min(targetArgs.length, this.pos + this.values.length); i++) {
            if ( ! _canDropProtected && targetArgs[i].isPreserved() ) {
                throw new WrongMethodTypeException("Dropping a protected argument #" + i
                                                 + ": " + targetArgs[i]);

            }

            MHUtils.assertAssignableType("argument " + i, targetArgs[i].getType(), this.values[i - this.pos].getType());
        }
    }

    @Override
    protected Argument[] computeInboundArgs(Argument[] targetArgs) {
        return TestUtils.concatArrays(
                Arrays.copyOfRange(targetArgs, 0, this.pos),
                Arrays.copyOfRange(targetArgs, this.pos + this.values.length, targetArgs.length));
    }

    @Override
    protected MethodHandle computeInboundMH(MethodHandle targetMH) {
        return MethodHandles.insertArguments(targetMH, this.pos, Arguments.valuesArray(this.values));
    }

    @Override
    protected String getName() {
        return "insertArguments";
    }

}
