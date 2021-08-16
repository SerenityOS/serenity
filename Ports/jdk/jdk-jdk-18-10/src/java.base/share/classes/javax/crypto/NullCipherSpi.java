/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package javax.crypto;

import java.security.*;
import java.security.spec.*;

/**
 * This class provides a delegate for the identity cipher - one that does not
 * transform the plain text.
 *
 * @author  Li Gong
 * @see NullCipher
 *
 * @since 1.4
 */

final class NullCipherSpi extends CipherSpi {

    /*
     * Do not let anybody instantiate this directly (protected).
     */
    protected NullCipherSpi() {}

    public void engineSetMode(String mode) {}

    public void engineSetPadding(String padding) {}

    protected int engineGetBlockSize() {
        return 1;
    }

    protected int engineGetOutputSize(int inputLen) {
        return inputLen;
    }

    protected byte[] engineGetIV() {
        byte[] x = new byte[8];
        return x;
    }

    protected AlgorithmParameters engineGetParameters() {
        return null;
    }

    protected void engineInit(int mode, Key key, SecureRandom random) {}

    protected void engineInit(int mode, Key key,
                              AlgorithmParameterSpec params,
                              SecureRandom random) {}

    protected void engineInit(int mode, Key key,
                              AlgorithmParameters params,
                              SecureRandom random) {}

    protected byte[] engineUpdate(byte[] input, int inputOffset,
                                  int inputLen) {
        if (input == null) return null;
        byte[] x = new byte[inputLen];
        System.arraycopy(input, inputOffset, x, 0, inputLen);
        return x;
    }

    protected int engineUpdate(byte[] input, int inputOffset,
                               int inputLen, byte[] output,
                               int outputOffset) {
        if (input == null) return 0;
        System.arraycopy(input, inputOffset, output, outputOffset, inputLen);
        return inputLen;
    }

    protected byte[] engineDoFinal(byte[] input, int inputOffset,
                                   int inputLen)
    {
        return engineUpdate(input, inputOffset, inputLen);
    }

    protected int engineDoFinal(byte[] input, int inputOffset,
                                int inputLen, byte[] output,
                                int outputOffset)
    {
        return engineUpdate(input, inputOffset, inputLen,
                            output, outputOffset);
    }

    protected int engineGetKeySize(Key key)
    {
        return 0;
    }
}
