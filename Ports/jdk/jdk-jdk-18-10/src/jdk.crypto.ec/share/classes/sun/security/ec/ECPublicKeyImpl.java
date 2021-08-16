/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ec;

import java.io.IOException;

import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;

import sun.security.util.ECParameters;
import sun.security.util.ECUtil;

import sun.security.x509.*;

/**
 * Key implementation for EC public keys.
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 */
public final class ECPublicKeyImpl extends X509Key implements ECPublicKey {

    private static final long serialVersionUID = -2462037275160462289L;

    private ECPoint w;
    private ECParameterSpec params;

    /**
     * Construct a key from its components. Used by the
     * ECKeyFactory.
     */
    @SuppressWarnings("deprecation")
    ECPublicKeyImpl(ECPoint w, ECParameterSpec params)
            throws InvalidKeyException {
        this.w = w;
        this.params = params;
        // generate the encoding
        algid = new AlgorithmId
            (AlgorithmId.EC_oid, ECParameters.getAlgorithmParameters(params));
        key = ECUtil.encodePoint(w, params.getCurve());
    }

    /**
     * Construct a key from its encoding.
     */
    ECPublicKeyImpl(byte[] encoded) throws InvalidKeyException {
        decode(encoded);
    }

    // see JCA doc
    public String getAlgorithm() {
        return "EC";
    }

    // see JCA doc
    public ECPoint getW() {
        return w;
    }

    // see JCA doc
    public ECParameterSpec getParams() {
        return params;
    }

    // Internal API to get the encoded point. Currently used by SunPKCS11.
    // This may change/go away depending on what we do with the public API.
    @SuppressWarnings("deprecation")
    public byte[] getEncodedPublicValue() {
        return key.clone();
    }

    /**
     * Parse the key. Called by X509Key.
     */
    @SuppressWarnings("deprecation")
    protected void parseKeyBits() throws InvalidKeyException {
        AlgorithmParameters algParams = this.algid.getParameters();
        if (algParams == null) {
            throw new InvalidKeyException("EC domain parameters must be " +
                "encoded in the algorithm identifier");
        }

        try {
            params = algParams.getParameterSpec(ECParameterSpec.class);
            w = ECUtil.decodePoint(key, params.getCurve());
        } catch (IOException e) {
            throw new InvalidKeyException("Invalid EC key", e);
        } catch (InvalidParameterSpecException e) {
            throw new InvalidKeyException("Invalid EC key", e);
        }
    }

    // return a string representation of this key for debugging
    public String toString() {
        return "Sun EC public key, " + params.getCurve().getField().getFieldSize()
            + " bits\n  public x coord: " + w.getAffineX()
            + "\n  public y coord: " + w.getAffineY()
            + "\n  parameters: " + params;
    }

    protected Object writeReplace() throws java.io.ObjectStreamException {
        return new KeyRep(KeyRep.Type.PUBLIC,
                        getAlgorithm(),
                        getFormat(),
                        getEncoded());
    }
}
