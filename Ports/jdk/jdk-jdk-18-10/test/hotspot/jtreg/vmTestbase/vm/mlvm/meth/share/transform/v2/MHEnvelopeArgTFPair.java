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

import vm.mlvm.meth.share.Argument;
import vm.mlvm.meth.share.Arguments;

public abstract class MHEnvelopeArgTFPair extends MHTFPair {

    private final String _tag;
    private final int _argNum;
    protected final Argument _envelopeArg;
    protected final Argument _envelopeLocatorArg;
    private final Argument _componentArg;

    public MHEnvelopeArgTFPair(MHCall outboundTarget, String tag, int argNum, Argument envelope, Argument envelopeLocator) {
        super(outboundTarget);

        _tag = tag;
        _argNum = argNum;

        envelopeLocator.setPreserved(true);
        envelopeLocator.setTag(tag + "_Locator");
        _envelopeLocatorArg = envelopeLocator;

        Argument arg = outboundTarget.getArgs()[argNum];
        _componentArg = arg;

        envelope.setTag(tag + "_Envelope");
        envelope.setPreserved(true);
        _envelopeArg = envelope;
    }

    @Override
    public MHTF getOutboundTF() {
        try {
            MHMacroTF mTF = new MHMacroTF("envelope arg outbound");
            mTF.addOutboundCall(outboundTarget);

            Argument[] outArgs = outboundTarget.getArgs();

            mTF.addTransformation(new MHPermuteTF(
                    mTF.addTransformation(new MHFoldTF(
                            mTF.addTransformation(new MHPermuteTF(outboundTarget,
                                    MHPermuteTF.moveArgsInPermuteArray(MHPermuteTF.getIdentityPermuteArray(outArgs.length), 0, 1, _argNum)
                            )),
                            mTF.addTransformation(computeGetTF(_envelopeArg, _envelopeLocatorArg))
                    )),
                    MHPermuteTF.moveArgsInPermuteArray(MHPermuteTF.getIdentityPermuteArray(outArgs.length + 1), _argNum, 2, 0)
            ));

            return mTF;
        } catch ( Exception e ) {
            throw (IllegalArgumentException) (new IllegalArgumentException("Exception when creating TF")).initCause(e);
        }
    }

    protected abstract MHTF computeGetTF(Argument envelopeArg2, Argument envelopeLocatorArg2);

    @Override
    public MHTF getInboundTF(MHCall target) {
        try {
            Argument[] outArgs = target.getArgs();

            int[] arrayArgIdxs = Arguments.findTag(outArgs, _tag + "_Envelope");
            if ( arrayArgIdxs.length != 1 )
                throw new IllegalArgumentException("There should be only one argument tagged [" + _tag + "_Envelope], but there are " + arrayArgIdxs);
            int arrayArgIdx = arrayArgIdxs[0];

            int[] idxArgIdxs = Arguments.findTag(outArgs, _tag + "_Locator");
            if ( idxArgIdxs.length != 1 )
                throw new IllegalArgumentException("There should be only one argument tagged [" + _tag + "_Locator], but there are " + idxArgIdxs);
            int idxArgIdx = idxArgIdxs[0];

            MHMacroTF mTF = new MHMacroTF("envelope arg inbound");
            mTF.addOutboundCall(target);

            int[] innerPermuteArray = MHPermuteTF.getIdentityPermuteArray(outArgs.length);

            if ( arrayArgIdx < idxArgIdx )
                innerPermuteArray = MHPermuteTF.moveArgsInPermuteArray(MHPermuteTF.moveArgsInPermuteArray(innerPermuteArray, 0, 1, arrayArgIdx), 0, 1, idxArgIdx);
            else
                innerPermuteArray = MHPermuteTF.moveArgsInPermuteArray(MHPermuteTF.moveArgsInPermuteArray(innerPermuteArray, 0, 1, idxArgIdx), 0, 1, arrayArgIdx);

            mTF.addTransformation(new MHPermuteTF(
                    mTF.addTransformation(new MHInsertTF(
                            mTF.addTransformation(new MHFoldTF(
                                    mTF.addTransformation(new MHPermuteTF(target, innerPermuteArray)),
                                    mTF.addTransformation(computeSetTF(_envelopeArg, _envelopeLocatorArg, _componentArg))
                            )),
                            0, new Argument[] { _envelopeArg, _envelopeLocatorArg }, true
                    )),
                    MHPermuteTF.moveArgsInPermuteArray(MHPermuteTF.getIdentityPermuteArray(outArgs.length), arrayArgIdx, 1, 0)
            ));

            return mTF;
        } catch ( Exception e ) {
            throw (IllegalArgumentException) (new IllegalArgumentException("Exception when creating TF")).initCause(e);
        }
    }

    protected abstract MHTF computeSetTF(Argument envelopeArg2, Argument envelopeLocatorArg2, Argument componentArg2);
}
