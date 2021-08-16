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

import vm.mlvm.meth.share.Argument;
import vm.mlvm.meth.share.Arguments;
import vm.mlvm.meth.share.SimpleOpMethodHandles;
import vm.mlvm.share.Env;

public class MHThrowCatchTFPair extends MHTFPair {

    private final String id;
    private final Argument testArg;
    private final Object testValue2;
    private final boolean testEq;
    private final Throwable _exception;

    private class ThrowTF extends MHDropTF {

        public ThrowTF(MHCall outboundCall) {
            super(outboundCall, 0, new Argument[] { testArg });
        }

        @Override
        protected MethodHandle computeInboundMH(MethodHandle targetMH) {
            try {
                Argument testArg = MHThrowCatchTFPair.this.testArg;

                MethodType targetType = targetMH.type().insertParameterTypes(0, testArg.getType());

                MHMacroTF mTF = new MHMacroTF("throwCatch throw part");
                mTF.addOutboundCall(getTargetCall());
/*
                MHCall testCall =
                    mTF.addTransformation(new MHInsertTF(
                           mTF.addTransformation(new MHExplicitCastTF(
                               mTF.addTransformation(new MHEqualityTestTF(testArg)),
                               boolean.class, new Class<?>[] { testArg.getType(), testArg.getType() })),
                    0, new Argument[] { testArg }, false));
*/
                MethodHandle testMH = MethodHandles.insertArguments(
                        MethodHandles.explicitCastArguments(
                                SimpleOpMethodHandles.eqMH(),
                                MethodType.methodType(boolean.class, testArg.getType(), testArg.getType())),
                        0, testArg.getValue());

                MethodHandle normalBranchMH = MethodHandles.dropArguments(targetMH, 0, testArg.getType());

                MethodHandle throwingBranchMH =
                    MethodHandles.dropArguments(
                            MethodHandles.insertArguments(
                                    MethodHandles.throwException(targetType.returnType(), _exception.getClass()),
                            0, _exception),
                    0, targetType.parameterArray());

                MethodHandle thenMH, elseMH;
                if ( MHThrowCatchTFPair.this.testEq ) {
                    thenMH = throwingBranchMH;
                    elseMH = normalBranchMH;
                } else {
                    testMH = MethodHandles.filterReturnValue(testMH, SimpleOpMethodHandles.notMH());
                    elseMH = throwingBranchMH;
                    thenMH = normalBranchMH;
                }

                Env.traceDebug("ThrowCatchTFPair: targetMH=%s; testMH=%s; thenMH=%s; elseMH=%s",
                               targetMH.type(), testMH.type(), thenMH.type(), elseMH.type());

                return MethodHandles.guardWithTest(testMH, thenMH, elseMH);
            } catch ( Throwable t ) {
                throw (IllegalArgumentException) (new IllegalArgumentException("Can't create throw/catch TF")).initCause(t);
            }
        }
    }

    private class CatchTF extends MHInsertTF {

        public CatchTF(MHCall target, int argIdx) {
            super(target, argIdx, new Argument[] { testArg }, true);
        }

        @Override
        protected MethodHandle computeInboundMH(MethodHandle targetMH) {
            try {
                MethodHandle catchTargetMH = MethodHandles.insertArguments(targetMH, this.pos, MHThrowCatchTFPair.this.testArg.getValue());
                MethodHandle catchHandlerMH = MethodHandles.dropArguments(
                        MethodHandles.insertArguments(targetMH, this.pos, MHThrowCatchTFPair.this.testValue2),
                        0, _exception.getClass());

                return MethodHandles.catchException(catchTargetMH, _exception.getClass(), catchHandlerMH);
            } catch ( Throwable t ) {
                IllegalArgumentException e = new IllegalArgumentException("Can't create a transformation");
                e.initCause(t);
                throw e;
            }
        }
    }
    public MHThrowCatchTFPair(MHCall outboundTarget, Argument testArg, Object testValue2, boolean testEq, Throwable exc) {
        super(outboundTarget);
        this.id = "ThrowCatch_" + hashCode();

        this.testArg = testArg.clone();
        this.testArg.setPreserved(true);
        this.testArg.setTag(this.id);

        this.testValue2 = testValue2;
        this.testEq = testEq;
        _exception = exc;
    }

    @Override
    public MHTF getOutboundTF() {
        return new ThrowTF(this.outboundTarget);
    }

    @Override
    public MHTF getInboundTF(MHCall inboundTarget) {
        int[] tagged = Arguments.findTag(inboundTarget.getArgs(), this.id);

        if ( tagged.length != 1 ) {
            throw new IllegalArgumentException("Can't find exactly one argument tagged " + this.id
                              + " from inner transformation (found indexes: " + tagged + ")");
        }

        return new CatchTF(inboundTarget, tagged[0]);
    }

}
