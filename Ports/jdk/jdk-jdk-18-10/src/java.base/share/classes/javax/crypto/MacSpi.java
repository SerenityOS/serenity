/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;

/**
 * This class defines the <i>Service Provider Interface</i> (<b>SPI</b>)
 * for the <code>Mac</code> class.
 * All the abstract methods in this class must be implemented by each
 * cryptographic service provider who wishes to supply the implementation
 * of a particular MAC algorithm.
 *
 * <p> Implementations are free to implement the Cloneable interface.
 *
 * @author Jan Luehe
 *
 * @since 1.4
 */

public abstract class MacSpi {

    /**
     * Constructor for subclasses to call.
     */
    public MacSpi() {}

    /**
     * Returns the length of the MAC in bytes.
     *
     * @return the MAC length in bytes.
     */
    protected abstract int engineGetMacLength();

    /**
     * Initializes the MAC with the given (secret) key and algorithm
     * parameters.
     *
     * @param key the (secret) key.
     * @param params the algorithm parameters.
     *
     * @exception InvalidKeyException if the given key is inappropriate for
     * initializing this MAC.
     * @exception InvalidAlgorithmParameterException if the given algorithm
     * parameters are inappropriate for this MAC.
     */
    protected abstract void engineInit(Key key,
                                       AlgorithmParameterSpec params)
        throws InvalidKeyException, InvalidAlgorithmParameterException ;

    /**
     * Processes the given byte.
     *
     * @param input the input byte to be processed.
     */
    protected abstract void engineUpdate(byte input);

    /**
     * Processes the first <code>len</code> bytes in <code>input</code>,
     * starting at <code>offset</code> inclusive.
     *
     * @param input the input buffer.
     * @param offset the offset in <code>input</code> where the input starts.
     * @param len the number of bytes to process.
     */
    protected abstract void engineUpdate(byte[] input, int offset, int len);

    /**
     * Processes <code>input.remaining()</code> bytes in the ByteBuffer
     * <code>input</code>, starting at <code>input.position()</code>.
     * Upon return, the buffer's position will be equal to its limit;
     * its limit will not have changed.
     *
     * <p>Subclasses should consider overriding this method if they can
     * process ByteBuffers more efficiently than byte arrays.
     *
     * @param input the ByteBuffer
     * @since 1.5
     */
    protected void engineUpdate(ByteBuffer input) {
        if (input.hasRemaining() == false) {
            return;
        }
        if (input.hasArray()) {
            byte[] b = input.array();
            int ofs = input.arrayOffset();
            int pos = input.position();
            int lim = input.limit();
            engineUpdate(b, ofs + pos, lim - pos);
            input.position(lim);
        } else {
            int len = input.remaining();
            byte[] b = new byte[CipherSpi.getTempArraySize(len)];
            while (len > 0) {
                int chunk = Math.min(len, b.length);
                input.get(b, 0, chunk);
                engineUpdate(b, 0, chunk);
                len -= chunk;
            }
        }
    }

    /**
     * Completes the MAC computation and resets the MAC for further use,
     * maintaining the secret key that the MAC was initialized with.
     *
     * @return the MAC result.
     */
    protected abstract byte[] engineDoFinal();

    /**
     * Resets the MAC for further use, maintaining the secret key that the
     * MAC was initialized with.
     */
    protected abstract void engineReset();

    /**
     * Returns a clone if the implementation is cloneable.
     *
     * @return a clone if the implementation is cloneable.
     *
     * @exception CloneNotSupportedException if this is called
     * on an implementation that does not support <code>Cloneable</code>.
     */
    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        } else {
            throw new CloneNotSupportedException();
        }
    }
}
