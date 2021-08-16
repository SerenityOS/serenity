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
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.util.Arrays;

import nsk.share.test.LazyIntArrayToString;
import vm.mlvm.meth.share.Argument;
import vm.mlvm.meth.share.MHUtils;
import vm.mlvm.share.Env;

public class MHPermuteTF extends MHBasicUnaryTF {

    private final int[] _reorderArray;
    private final MethodType _sourceMT;

    public MHPermuteTF(MHCall target, MethodType sourceMT, int[] reorderArray) {
        super(target);
        _reorderArray = reorderArray;
        _sourceMT = sourceMT;
    }

    public MHPermuteTF(MHCall target, int[] reorderArray) {
        this(target, getPermutedMT(target.getTargetMH().type(), reorderArray), reorderArray);
    }

    @Override
    protected void check(Argument[] targetArgs) throws IllegalArgumentException {
        super.check(targetArgs);

        if ( _sourceMT.parameterCount() < _reorderArray.length ) {
            throw new WrongMethodTypeException("reorderArray requires at least "
                                             + _reorderArray.length + " target arguments, but only "
                                             + _sourceMT.parameterCount() + " are given");
        }

        for ( int i = 0; i < _reorderArray.length; i++ ) {
            MHUtils.assertAssignableType("reorderArray element " + i,
                    targetArgs[i].getType(),
                    _sourceMT.parameterType(_reorderArray[i]));
        }
    }

    @Override
    protected MethodHandle computeInboundMH(MethodHandle targetMH) {
        MethodHandle r = MethodHandles.permuteArguments(targetMH, _sourceMT, _reorderArray);
        Env.traceDebug("permute: inType=%s; targetType=%s; reorder=%s",
                       r.type(), targetMH.type(), new LazyIntArrayToString(_reorderArray));
        return r;
    }

    @Override
    protected Argument[] computeInboundArgs(Argument[] targetArgs) {
        Argument[] resultArgs = new Argument[_sourceMT.parameterCount()];

        for ( int i = 0; i < targetArgs.length; i++ ) {
            resultArgs[_reorderArray[i]] = targetArgs[i];
        }

        for ( int i = 0; i < resultArgs.length; i++ ) {
            if ( resultArgs[i] == null ) {
                resultArgs[i] = new Argument(_sourceMT.parameterType(i), null);
            }
        }

        return resultArgs;
    }

    @Override
    protected String getName() {
        return "permuteArguments";
    }

    @Override
    protected String getDescription() {
        return "sourceMT=" + _sourceMT + "; reorder=" + Arrays.toString(_reorderArray);
    }

    public static MethodType getPermutedMT(MethodType targetMT, int[] reorderArray) {
        int srcParamCount = 0;
        for ( int t = 0; t < reorderArray.length; t++ )
            srcParamCount = Math.max(srcParamCount, reorderArray[t] + 1);

        Class<?>[] paramTypes = new Class<?>[srcParamCount];

        for ( int t = 0; t < reorderArray.length; t++ )
            paramTypes[reorderArray[t]] = targetMT.parameterType(t);

        for ( int s = 0; s < paramTypes.length; s++ )
            if ( paramTypes[s] == null )
                    throw new IllegalArgumentException("Type of parameter #" + s + " is not defined");

        return MethodType.methodType(targetMT.returnType(), paramTypes);
    }

    public static int[] getIdentityPermuteArray(int argCount) {
        int[] result = new int[argCount];
        for ( int i = 0; i < argCount; i++ )
            result[i] = i;
        return result;
    }

    public static int[] moveArgsInPermuteArray(int[] array, int oldPos, int count, int newPos) {
        if ( newPos == oldPos )
            return array;

        int[] result = new int[array.length];

        if ( newPos < oldPos ) {
            System.arraycopy(array, 0, result, 0, newPos);
            System.arraycopy(array, newPos, result, newPos + count, oldPos - newPos);
            System.arraycopy(array, oldPos + count, result, oldPos + count, array.length - oldPos - count);
        } else {
            System.arraycopy(array, 0, result, 0, oldPos);
            System.arraycopy(array, oldPos + count, result, oldPos, newPos - oldPos - count);
            System.arraycopy(array, newPos + count, result, newPos + count, array.length - newPos - count);
        }
        System.arraycopy(array, oldPos, result, newPos, count);

        return result;
    }
}
