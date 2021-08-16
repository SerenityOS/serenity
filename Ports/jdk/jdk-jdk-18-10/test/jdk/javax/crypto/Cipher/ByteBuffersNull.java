/*
 * Copyright (c) 2004, 2007, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 5000980
 * @summary Check NullPointerException for cipherSpi.engineUpdate(x, null)
 */

import javax.crypto.CipherSpi;
import java.security.InvalidKeyException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.Key;
import java.security.Security;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.NoSuchPaddingException;
import java.security.GeneralSecurityException;
import java.nio.ByteBuffer;

public class ByteBuffersNull {

    static final int bufSize = 1024;

    public void testCase010() throws Exception {
        CipherSpiImpl c = new CipherSpiImpl();
        BufferDescr[] inBuffers = getByteBuffersForTest(bufSize);
        int failureCount = 0;

        for (int i = 0; i < inBuffers.length; i++) {
            String key = inBuffers[i].descr;
            ByteBuffer bb = inBuffers[i].buf;

            try {

                c.engineUpdate(bb, null);

                throw new Exception("No Exception?!");
            }  catch (NullPointerException npe) {
                // Expected behaviour - pass
                System.out.println("OK: " + npe);
            }
        }
    }

    // Creates a ByteBuffer with a desired properties
    ByteBuffer getByteBuffer(int capacity, int position,
            boolean limitAt0) {
        ByteBuffer bb = ByteBuffer.allocate(capacity);

        bb.position(position);

        if (limitAt0)
            bb.limit(0);

        return bb;
    }

    BufferDescr[] getByteBuffersForTest(int defaultSize) {
        int caseNum = 4;

        BufferDescr[] buffers = new BufferDescr[caseNum];

        // ByteBuffer with capacity 0
        buffers[0]= new BufferDescr("ByteBuffer with capacity == 0",
            getByteBuffer(0,0,false));

        // ByteBuffer with some space but limit = 0
        buffers[1] = new BufferDescr(
            "ByteBuffer with some capacity but limit == 0",
            getByteBuffer(defaultSize, 0, true));

        // ByteBuffer with some remaining data (limit = capacity)
        buffers[2] = new BufferDescr("ByteBuffer with some data",
            getByteBuffer(defaultSize,0,false));

        // ByteBuffer with some data but position is at the limit
        buffers[3] = new BufferDescr(
            "ByteBuffer with data but position at the limit",
            getByteBuffer(defaultSize, defaultSize,false));

        return buffers;
    }

    public static void main(String argv[]) throws Exception {
        ByteBuffersNull test = new ByteBuffersNull();
        test.testCase010();
    }

    class BufferDescr {
        BufferDescr(String d, ByteBuffer b) {
            descr = d;
            buf = b;
        }

        public String descr;
        public ByteBuffer buf;
    }

    public class CipherSpiImpl extends CipherSpi {

        public CipherSpiImpl() {
            super();
        }

        public void engineSetMode(String mode)
            throws NoSuchAlgorithmException { }

        public void engineSetPadding(String padding)
            throws NoSuchPaddingException { }

        public int engineGetBlockSize() {
            return 0;
        }

        public int engineGetOutputSize(int inputLen) {
            return 0;
        }

        public byte[] engineGetIV() {
            return null;
        }

        public AlgorithmParameters engineGetParameters() {
            return null;
        }

        public void engineInit(int opmode, Key key, SecureRandom random)
            throws InvalidKeyException { }

        public void engineInit(int opmode, Key key,
                               AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException { }

        public void engineInit(int opmode, Key key, AlgorithmParameters params,
                               SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException { }

        public byte[] engineUpdate(byte[] input, int offset, int len) {
            return null;
        }

        public int engineUpdate(byte[] input, int inputOffset, int inputLen,
                                byte[] output, int outputOffset)
            throws ShortBufferException {
            return 0;
        }

        public byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen)
            throws IllegalBlockSizeException, BadPaddingException {
            return null;
        }

        public int engineDoFinal(byte[] input, int inputOffset, int inputLen,
                                 byte[] output, int outputOffset)
            throws ShortBufferException, IllegalBlockSizeException,
            BadPaddingException {
            return 0;
        }

        public byte[] engineWrap(Key key)
            throws IllegalBlockSizeException, InvalidKeyException {
            return super.engineWrap(key);
        }

        public Key engineUnwrap(byte[] wKey, String wKeyAlgorithm,
                                int wKeyType) throws InvalidKeyException,
            NoSuchAlgorithmException {
            return super.engineUnwrap(wKey, wKeyAlgorithm, wKeyType);
        }

        public int engineGetKeySize(Key key) throws InvalidKeyException {
            return super.engineGetKeySize(key);
        }

        public int engineDoFinal(ByteBuffer input, ByteBuffer output)
            throws ShortBufferException, IllegalBlockSizeException,
            BadPaddingException {
            return super.engineDoFinal(input, output);
        }

        public int engineUpdate(ByteBuffer input, ByteBuffer output)
            throws ShortBufferException {
            return super.engineUpdate(input, output);
        }
    }
}
