/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.mscapi;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.KeyRep;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;

import sun.security.rsa.RSAUtil.KeyType;
import sun.security.rsa.RSAPublicKeyImpl;
import sun.security.util.ECKeySizeParameterSpec;

/**
 * The handle for an RSA public key using the Microsoft Crypto API.
 *
 * @since 1.6
 */
public abstract class CPublicKey extends CKey implements PublicKey {

    private static final long serialVersionUID = -2289561342425825391L;

    protected byte[] encoding = null;

    public static class CECPublicKey extends CPublicKey implements ECPublicKey {

        private ECPoint w = null;
        private static final long serialVersionUID = 12L;

        CECPublicKey(NativeHandles handles, int keyLength) {
            super("EC", handles, keyLength);
        }

        @Override
        public ECPoint getW() {
            if (w == null) {
                // See CKey::generateECBlob.
                try {
                    byte[] blob = getPublicKeyBlob(
                            handles.hCryptProv, handles.hCryptKey);
                    int len = blob[8] & 0xff;
                    byte[] x = Arrays.copyOfRange(blob, 8, 8 + len);
                    byte[] y = Arrays.copyOfRange(blob, 8 + len, 8 + len + len);
                    w = new ECPoint(new BigInteger(1, x), new BigInteger(1, y));
                } catch (KeyException e) {
                    throw new ProviderException(e);
                }
            }
            return w;
        }

        @Override
        public byte[] getEncoded() {
            if (encoding == null) {
                try {
                    encoding = KeyFactory.getInstance("EC").generatePublic(
                                new ECPublicKeySpec(getW(), getParams()))
                            .getEncoded();
                } catch (Exception e) {
                    // ignore
                }
            }
            return encoding;
        }

        @Override
        public ECParameterSpec getParams() {
            try {
                AlgorithmParameters ap = AlgorithmParameters.getInstance("EC");
                ap.init(new ECKeySizeParameterSpec(keyLength));
                return ap.getParameterSpec(ECParameterSpec.class);
            } catch (Exception e) {
                throw new ProviderException(e);
            }
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(algorithm + "PublicKey [size=").append(keyLength)
                    .append("]\n  ECPoint: ").append(getW())
                    .append("\n  params: ").append(getParams());
            return sb.toString();
        }
    }

    public static class CRSAPublicKey extends CPublicKey implements RSAPublicKey {

        private BigInteger modulus = null;
        private BigInteger exponent = null;
        private static final long serialVersionUID = 12L;

        CRSAPublicKey(NativeHandles handles, int keyLength) {
            super("RSA", handles, keyLength);
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(algorithm + "PublicKey [size=").append(keyLength)
                    .append(" bits, type=");
            if (handles.hCryptKey != 0) {
                sb.append(getKeyType(handles.hCryptKey))
                        .append(", container=").append(getContainerName(handles.hCryptProv));
            } else {
                sb.append("CNG");
            }
            sb.append("]\n  modulus: ").append(getModulus())
                    .append("\n  public exponent: ").append(getPublicExponent());
            return sb.toString();
        }

        @Override
        public BigInteger getPublicExponent() {
            if (exponent == null) {
                try {
                    byte[] publicKeyBlob = getPublicKeyBlob(
                            handles.hCryptProv, handles.hCryptKey);
                    exponent = new BigInteger(1, getExponent(publicKeyBlob));
                } catch (KeyException e) {
                    throw new ProviderException(e);
                }
            }
            return exponent;
        }

        @Override
        public BigInteger getModulus() {
            if (modulus == null) {
                try {
                    byte[] publicKeyBlob = getPublicKeyBlob(
                            handles.hCryptProv, handles.hCryptKey);
                    modulus = new BigInteger(1, getModulus(publicKeyBlob));
                } catch (KeyException e) {
                    throw new ProviderException(e);
                }
            }
            return modulus;
        }

        @Override
        public byte[] getEncoded() {
            if (encoding == null) {
                try {
                    encoding = RSAPublicKeyImpl.newKey(KeyType.RSA, null,
                            getModulus(), getPublicExponent()).getEncoded();
                } catch (KeyException e) {
                    // ignore
                }
            }
            return encoding;
        }

        private native byte[] getExponent(byte[] keyBlob) throws KeyException;

        private native byte[] getModulus(byte[] keyBlob) throws KeyException;
    }

    // Called by native code inside security.cpp
    static CPublicKey of(
            String alg, long hCryptProv, long hCryptKey, int keyLength) {
        return of(alg, new NativeHandles(hCryptProv, hCryptKey), keyLength);
    }

    public static CPublicKey of(
            String alg, NativeHandles handles, int keyLength) {
        switch (alg) {
            case "RSA":
                return new CRSAPublicKey(handles, keyLength);
            case "EC":
                return new CECPublicKey(handles, keyLength);
            default:
                throw new AssertionError("Unsupported algorithm: " + alg);
        }
    }

    protected CPublicKey(
            String alg, NativeHandles handles, int keyLength) {
        super(alg, handles, keyLength);
    }

    @Override
    public String getFormat() {
        return "X.509";
    }

    protected Object writeReplace() throws java.io.ObjectStreamException {
        return new KeyRep(KeyRep.Type.PUBLIC,
                        getAlgorithm(),
                        getFormat(),
                        getEncoded());
    }

    // Returns the CAPI or CNG representation of the key.
    native byte[] getPublicKeyBlob(long hCryptProv, long hCryptKey)
            throws KeyException;
}
