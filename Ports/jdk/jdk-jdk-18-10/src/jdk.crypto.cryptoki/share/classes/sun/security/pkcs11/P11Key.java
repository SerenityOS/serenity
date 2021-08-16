/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.pkcs11;

import java.io.*;
import java.lang.ref.*;
import java.math.BigInteger;
import java.util.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;

import javax.crypto.*;
import javax.crypto.interfaces.*;
import javax.crypto.spec.*;

import sun.security.rsa.RSAUtil.KeyType;
import sun.security.rsa.RSAPublicKeyImpl;
import sun.security.rsa.RSAPrivateCrtKeyImpl;

import sun.security.internal.interfaces.TlsMasterSecret;

import sun.security.pkcs11.wrapper.*;

import static sun.security.pkcs11.TemplateManager.O_GENERATE;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;

import sun.security.util.DerValue;
import sun.security.util.Length;
import sun.security.util.ECUtil;
import sun.security.jca.JCAUtil;

/**
 * Key implementation classes.
 *
 * In PKCS#11, the components of private and secret keys may or may not
 * be accessible. If they are, we use the algorithm specific key classes
 * (e.g. DSAPrivateKey) for compatibility with existing applications.
 * If the components are not accessible, we use a generic class that
 * only implements PrivateKey (or SecretKey). Whether the components of a
 * key are extractable is automatically determined when the key object is
 * created.
 *
 * @author  Andreas Sterbenz
 * @since   1.5
 */
abstract class P11Key implements Key, Length {

    private static final long serialVersionUID = -2575874101938349339L;

    private static final String PUBLIC = "public";
    private static final String PRIVATE = "private";
    private static final String SECRET = "secret";

    // type of key, one of (PUBLIC, PRIVATE, SECRET)
    final String type;

    // token instance
    final Token token;

    // algorithm name, returned by getAlgorithm(), etc.
    final String algorithm;

    // effective key length of the key, e.g. 56 for a DES key
    final int keyLength;

    // flags indicating whether the key is a token object, sensitive, extractable
    final boolean tokenObject, sensitive, extractable;

    private final NativeKeyHolder keyIDHolder;

    private static final boolean DISABLE_NATIVE_KEYS_EXTRACTION;

    /**
     * {@systemProperty sun.security.pkcs11.disableKeyExtraction} property
     * indicating whether or not cryptographic keys within tokens are
     * extracted to a Java byte array for memory management purposes.
     *
     * Key extraction affects NSS PKCS11 library only.
     *
     */
    static {
        PrivilegedAction<String> getKeyExtractionProp =
                () -> System.getProperty(
                        "sun.security.pkcs11.disableKeyExtraction", "false");
        @SuppressWarnings("removal")
        String disableKeyExtraction =
                AccessController.doPrivileged(getKeyExtractionProp);
        DISABLE_NATIVE_KEYS_EXTRACTION =
                "true".equalsIgnoreCase(disableKeyExtraction);
    }

    P11Key(String type, Session session, long keyID, String algorithm,
            int keyLength, CK_ATTRIBUTE[] attributes) {
        this.type = type;
        this.token = session.token;
        this.algorithm = algorithm;
        this.keyLength = keyLength;
        boolean tokenObject = false;
        boolean sensitive = false;
        boolean extractable = true;
        int n = (attributes == null) ? 0 : attributes.length;
        for (int i = 0; i < n; i++) {
            CK_ATTRIBUTE attr = attributes[i];
            if (attr.type == CKA_TOKEN) {
                tokenObject = attr.getBoolean();
            } else if (attr.type == CKA_SENSITIVE) {
                sensitive = attr.getBoolean();
            } else if (attr.type == CKA_EXTRACTABLE) {
                extractable = attr.getBoolean();
            }
        }
        this.tokenObject = tokenObject;
        this.sensitive = sensitive;
        this.extractable = extractable;
        char[] tokenLabel = this.token.tokenInfo.label;
        boolean isNSS = (tokenLabel[0] == 'N' && tokenLabel[1] == 'S'
                && tokenLabel[2] == 'S');
        boolean extractKeyInfo = (!DISABLE_NATIVE_KEYS_EXTRACTION && isNSS &&
                extractable && !tokenObject);
        this.keyIDHolder = new NativeKeyHolder(this, keyID, session,
                extractKeyInfo, tokenObject);
    }

    public long getKeyID() {
        return keyIDHolder.getKeyID();
    }

    public void releaseKeyID() {
        keyIDHolder.releaseKeyID();
    }

    // see JCA spec
    public final String getAlgorithm() {
        token.ensureValid();
        return algorithm;
    }

    // see JCA spec
    public final byte[] getEncoded() {
        byte[] b = getEncodedInternal();
        return (b == null) ? null : b.clone();
    }

    // Called by the NativeResourceCleaner at specified intervals
    // See NativeResourceCleaner for more information
    static boolean drainRefQueue() {
        boolean found = false;
        SessionKeyRef next;
        while ((next = (SessionKeyRef) SessionKeyRef.refQueue.poll()) != null) {
            found = true;
            next.dispose();
        }
        return found;
    }

    abstract byte[] getEncodedInternal();

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        // equals() should never throw exceptions
        if (token.isValid() == false) {
            return false;
        }
        if (obj instanceof Key == false) {
            return false;
        }
        String thisFormat = getFormat();
        if (thisFormat == null) {
            // no encoding, key only equal to itself
            // XXX getEncoded() for unextractable keys will change that
            return false;
        }
        Key other = (Key)obj;
        if (thisFormat.equals(other.getFormat()) == false) {
            return false;
        }
        byte[] thisEnc = this.getEncodedInternal();
        byte[] otherEnc;
        if (obj instanceof P11Key) {
            otherEnc = ((P11Key)other).getEncodedInternal();
        } else {
            otherEnc = other.getEncoded();
        }
        return MessageDigest.isEqual(thisEnc, otherEnc);
    }

    public int hashCode() {
        // hashCode() should never throw exceptions
        if (token.isValid() == false) {
            return 0;
        }
        byte[] b1 = getEncodedInternal();
        if (b1 == null) {
            return 0;
        }
        int r = b1.length;
        for (int i = 0; i < b1.length; i++) {
            r += (b1[i] & 0xff) * 37;
        }
        return r;
    }

    protected Object writeReplace() throws ObjectStreamException {
        KeyRep.Type type;
        String format = getFormat();
        if (isPrivate() && "PKCS#8".equals(format)) {
            type = KeyRep.Type.PRIVATE;
        } else if (isPublic() && "X.509".equals(format)) {
            type = KeyRep.Type.PUBLIC;
        } else if (isSecret() && "RAW".equals(format)) {
            type = KeyRep.Type.SECRET;
        } else {
            // XXX short term serialization for unextractable keys
            throw new NotSerializableException
                ("Cannot serialize sensitive and unextractable keys");
        }
        return new KeyRep(type, getAlgorithm(), format, getEncodedInternal());
    }

    public String toString() {
        token.ensureValid();
        String s1 = token.provider.getName() + " " + algorithm + " " + type
                + " key, " + keyLength + " bits";
        s1 += (tokenObject ? "token" : "session") + " object";
        if (isPublic()) {
            s1 += ")";
        } else {
            s1 += ", " + (sensitive ? "" : "not ") + "sensitive";
            s1 += ", " + (extractable ? "" : "un") + "extractable)";
        }
        return s1;
    }

    /**
     * Return bit length of the key.
     */
    @Override
    public int length() {
        return keyLength;
    }

    boolean isPublic() {
        return type == PUBLIC;
    }

    boolean isPrivate() {
        return type == PRIVATE;
    }

    boolean isSecret() {
        return type == SECRET;
    }

    void fetchAttributes(CK_ATTRIBUTE[] attributes) {
        Session tempSession = null;
        long keyID = this.getKeyID();
        try {
            tempSession = token.getOpSession();
            token.p11.C_GetAttributeValue(tempSession.id(), keyID,
                        attributes);
        } catch (PKCS11Exception e) {
            throw new ProviderException(e);
        } finally {
            this.releaseKeyID();
            token.releaseSession(tempSession);
        }
    }

    private static final CK_ATTRIBUTE[] A0 = new CK_ATTRIBUTE[0];

    private static CK_ATTRIBUTE[] getAttributes(Session session, long keyID,
            CK_ATTRIBUTE[] knownAttributes, CK_ATTRIBUTE[] desiredAttributes) {
        if (knownAttributes == null) {
            knownAttributes = A0;
        }
        for (int i = 0; i < desiredAttributes.length; i++) {
            // For each desired attribute, check to see if we have the value
            // available already. If everything is here, we save a native call.
            CK_ATTRIBUTE attr = desiredAttributes[i];
            for (CK_ATTRIBUTE known : knownAttributes) {
                if ((attr.type == known.type) && (known.pValue != null)) {
                    attr.pValue = known.pValue;
                    break; // break inner for loop
                }
            }
            if (attr.pValue == null) {
                // nothing found, need to call C_GetAttributeValue()
                for (int j = 0; j < i; j++) {
                    // clear values copied from knownAttributes
                    desiredAttributes[j].pValue = null;
                }
                try {
                    session.token.p11.C_GetAttributeValue
                            (session.id(), keyID, desiredAttributes);
                } catch (PKCS11Exception e) {
                    throw new ProviderException(e);
                }
                break; // break loop, goto return
            }
        }
        return desiredAttributes;
    }

    static SecretKey secretKey(Session session, long keyID, String algorithm,
            int keyLength, CK_ATTRIBUTE[] attributes) {
        attributes = getAttributes(session, keyID, attributes, new CK_ATTRIBUTE[] {
            new CK_ATTRIBUTE(CKA_TOKEN),
            new CK_ATTRIBUTE(CKA_SENSITIVE),
            new CK_ATTRIBUTE(CKA_EXTRACTABLE),
        });
        return new P11SecretKey(session, keyID, algorithm, keyLength,
                attributes);
    }

    static SecretKey masterSecretKey(Session session, long keyID, String algorithm,
            int keyLength, CK_ATTRIBUTE[] attributes, int major, int minor) {
        attributes = getAttributes(session, keyID, attributes, new CK_ATTRIBUTE[] {
            new CK_ATTRIBUTE(CKA_TOKEN),
            new CK_ATTRIBUTE(CKA_SENSITIVE),
            new CK_ATTRIBUTE(CKA_EXTRACTABLE),
        });
        return new P11TlsMasterSecretKey(
                session, keyID, algorithm, keyLength, attributes, major,
                minor);
    }

    // we assume that all components of public keys are always accessible
    static PublicKey publicKey(Session session, long keyID, String algorithm,
            int keyLength, CK_ATTRIBUTE[] attributes) {
        switch (algorithm) {
            case "RSA":
                return new P11RSAPublicKey(session, keyID, algorithm,
                        keyLength, attributes);
            case "DSA":
                return new P11DSAPublicKey(session, keyID, algorithm,
                        keyLength, attributes);
            case "DH":
                return new P11DHPublicKey(session, keyID, algorithm,
                        keyLength, attributes);
            case "EC":
                return new P11ECPublicKey(session, keyID, algorithm,
                        keyLength, attributes);
            default:
                throw new ProviderException
                    ("Unknown public key algorithm " + algorithm);
        }
    }

    static PrivateKey privateKey(Session session, long keyID, String algorithm,
            int keyLength, CK_ATTRIBUTE[] attributes) {
        attributes = getAttributes(session, keyID, attributes, new CK_ATTRIBUTE[] {
            new CK_ATTRIBUTE(CKA_TOKEN),
            new CK_ATTRIBUTE(CKA_SENSITIVE),
            new CK_ATTRIBUTE(CKA_EXTRACTABLE),
        });
        if (attributes[1].getBoolean() || (attributes[2].getBoolean() == false)) {
            return new P11PrivateKey
                (session, keyID, algorithm, keyLength, attributes);
        } else {
            switch (algorithm) {
                case "RSA":
                    // In order to decide if this is RSA CRT key, we first query
                    // and see if all extra CRT attributes are available.
                    CK_ATTRIBUTE[] attrs2 = new CK_ATTRIBUTE[] {
                        new CK_ATTRIBUTE(CKA_PUBLIC_EXPONENT),
                        new CK_ATTRIBUTE(CKA_PRIME_1),
                        new CK_ATTRIBUTE(CKA_PRIME_2),
                        new CK_ATTRIBUTE(CKA_EXPONENT_1),
                        new CK_ATTRIBUTE(CKA_EXPONENT_2),
                        new CK_ATTRIBUTE(CKA_COEFFICIENT),
                    };
                    boolean crtKey;
                    try {
                        session.token.p11.C_GetAttributeValue
                            (session.id(), keyID, attrs2);
                        crtKey = ((attrs2[0].pValue instanceof byte[]) &&
                                  (attrs2[1].pValue instanceof byte[]) &&
                                  (attrs2[2].pValue instanceof byte[]) &&
                                  (attrs2[3].pValue instanceof byte[]) &&
                                  (attrs2[4].pValue instanceof byte[]) &&
                                  (attrs2[5].pValue instanceof byte[])) ;
                    } catch (PKCS11Exception e) {
                        // ignore, assume not available
                        crtKey = false;
                    }
                    if (crtKey) {
                        return new P11RSAPrivateKey(session, keyID, algorithm,
                                keyLength, attributes, attrs2);
                    } else {
                        return new P11RSAPrivateNonCRTKey(session, keyID,
                                algorithm, keyLength, attributes);
                    }
                case "DSA":
                    return new P11DSAPrivateKey(session, keyID, algorithm,
                            keyLength, attributes);
                case "DH":
                    return new P11DHPrivateKey(session, keyID, algorithm,
                            keyLength, attributes);
                case "EC":
                    return new P11ECPrivateKey(session, keyID, algorithm,
                            keyLength, attributes);
                default:
                    throw new ProviderException
                            ("Unknown private key algorithm " + algorithm);
            }
        }
    }

    // class for sensitive and unextractable private keys
    private static final class P11PrivateKey extends P11Key
                                                implements PrivateKey {
        private static final long serialVersionUID = -2138581185214187615L;

        P11PrivateKey(Session session, long keyID, String algorithm,
                int keyLength, CK_ATTRIBUTE[] attributes) {
            super(PRIVATE, session, keyID, algorithm, keyLength, attributes);
        }
        // XXX temporary encoding for serialization purposes
        public String getFormat() {
            token.ensureValid();
            return null;
        }
        byte[] getEncodedInternal() {
            token.ensureValid();
            return null;
        }
    }

    private static class P11SecretKey extends P11Key implements SecretKey {
        private static final long serialVersionUID = -7828241727014329084L;
        private volatile byte[] encoded;
        P11SecretKey(Session session, long keyID, String algorithm,
                int keyLength, CK_ATTRIBUTE[] attributes) {
            super(SECRET, session, keyID, algorithm, keyLength, attributes);
        }
        public String getFormat() {
            token.ensureValid();
            if (sensitive || (extractable == false)) {
                return null;
            } else {
                return "RAW";
            }
        }
        byte[] getEncodedInternal() {
            token.ensureValid();
            if (getFormat() == null) {
                return null;
            }
            byte[] b = encoded;
            if (b == null) {
                synchronized (this) {
                    b = encoded;
                    if (b == null) {
                        Session tempSession = null;
                        long keyID = this.getKeyID();
                        try {
                            tempSession = token.getOpSession();
                            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
                                new CK_ATTRIBUTE(CKA_VALUE),
                            };
                            token.p11.C_GetAttributeValue
                                    (tempSession.id(), keyID, attributes);
                            b = attributes[0].getByteArray();
                        } catch (PKCS11Exception e) {
                            throw new ProviderException(e);
                        } finally {
                            this.releaseKeyID();
                            token.releaseSession(tempSession);
                        }
                        encoded = b;
                    }
                }
            }
            return b;
        }
    }

    @SuppressWarnings("deprecation")
    private static class P11TlsMasterSecretKey extends P11SecretKey
            implements TlsMasterSecret {
        private static final long serialVersionUID = -1318560923770573441L;

        private final int majorVersion, minorVersion;
        P11TlsMasterSecretKey(Session session, long keyID, String algorithm,
                int keyLength, CK_ATTRIBUTE[] attributes, int major, int minor) {
            super(session, keyID, algorithm, keyLength, attributes);
            this.majorVersion = major;
            this.minorVersion = minor;
        }
        public int getMajorVersion() {
            return majorVersion;
        }

        public int getMinorVersion() {
            return minorVersion;
        }
    }

    // RSA CRT private key
    private static final class P11RSAPrivateKey extends P11Key
                implements RSAPrivateCrtKey {
        private static final long serialVersionUID = 9215872438913515220L;

        private BigInteger n, e, d, p, q, pe, qe, coeff;
        private byte[] encoded;
        P11RSAPrivateKey(Session session, long keyID, String algorithm,
                int keyLength, CK_ATTRIBUTE[] attrs, CK_ATTRIBUTE[] crtAttrs) {
            super(PRIVATE, session, keyID, algorithm, keyLength, attrs);

            for (CK_ATTRIBUTE a : crtAttrs) {
                if (a.type == CKA_PUBLIC_EXPONENT) {
                    e = a.getBigInteger();
                } else if (a.type == CKA_PRIME_1) {
                    p = a.getBigInteger();
                } else if (a.type == CKA_PRIME_2) {
                    q = a.getBigInteger();
                } else if (a.type == CKA_EXPONENT_1) {
                    pe = a.getBigInteger();
                } else if (a.type == CKA_EXPONENT_2) {
                    qe = a.getBigInteger();
                } else if (a.type == CKA_COEFFICIENT) {
                    coeff = a.getBigInteger();
                }
            }
        }
        private synchronized void fetchValues() {
            token.ensureValid();
            if (n != null) {
                return;
            }
            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_MODULUS),
                new CK_ATTRIBUTE(CKA_PRIVATE_EXPONENT),
            };
            fetchAttributes(attributes);
            n = attributes[0].getBigInteger();
            d = attributes[1].getBigInteger();
        }

        public String getFormat() {
            token.ensureValid();
            return "PKCS#8";
        }
        synchronized byte[] getEncodedInternal() {
            token.ensureValid();
            if (encoded == null) {
                fetchValues();
                try {
                    Key newKey = RSAPrivateCrtKeyImpl.newKey
                        (KeyType.RSA, null, n, e, d, p, q, pe, qe, coeff);
                    encoded = newKey.getEncoded();
                } catch (GeneralSecurityException e) {
                    throw new ProviderException(e);
                }
            }
            return encoded;
        }
        public BigInteger getModulus() {
            fetchValues();
            return n;
        }
        public BigInteger getPublicExponent() {
            return e;
        }
        public BigInteger getPrivateExponent() {
            fetchValues();
            return d;
        }
        public BigInteger getPrimeP() {
            return p;
        }
        public BigInteger getPrimeQ() {
            return q;
        }
        public BigInteger getPrimeExponentP() {
            return pe;
        }
        public BigInteger getPrimeExponentQ() {
            return qe;
        }
        public BigInteger getCrtCoefficient() {
            return coeff;
        }
    }

    // RSA non-CRT private key
    private static final class P11RSAPrivateNonCRTKey extends P11Key
                implements RSAPrivateKey {
        private static final long serialVersionUID = 1137764983777411481L;

        private BigInteger n, d;
        private byte[] encoded;
        P11RSAPrivateNonCRTKey(Session session, long keyID, String algorithm,
                int keyLength, CK_ATTRIBUTE[] attributes) {
            super(PRIVATE, session, keyID, algorithm, keyLength, attributes);
        }
        private synchronized void fetchValues() {
            token.ensureValid();
            if (n != null) {
                return;
            }
            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_MODULUS),
                new CK_ATTRIBUTE(CKA_PRIVATE_EXPONENT),
            };
            fetchAttributes(attributes);
            n = attributes[0].getBigInteger();
            d = attributes[1].getBigInteger();
        }
        public String getFormat() {
            token.ensureValid();
            return "PKCS#8";
        }
        synchronized byte[] getEncodedInternal() {
            token.ensureValid();
            if (encoded == null) {
                fetchValues();
                try {
                    // XXX make constructor in SunRsaSign provider public
                    // and call it directly
                    KeyFactory factory = KeyFactory.getInstance
                        ("RSA", P11Util.getSunRsaSignProvider());
                    Key newKey = factory.translateKey(this);
                    encoded = newKey.getEncoded();
                } catch (GeneralSecurityException e) {
                    throw new ProviderException(e);
                }
            }
            return encoded;
        }
        public BigInteger getModulus() {
            fetchValues();
            return n;
        }
        public BigInteger getPrivateExponent() {
            fetchValues();
            return d;
        }
    }

    private static final class P11RSAPublicKey extends P11Key
                                                implements RSAPublicKey {
        private static final long serialVersionUID = -826726289023854455L;
        private BigInteger n, e;
        private byte[] encoded;
        P11RSAPublicKey(Session session, long keyID, String algorithm,
                int keyLength, CK_ATTRIBUTE[] attributes) {
            super(PUBLIC, session, keyID, algorithm, keyLength, attributes);
        }
        private synchronized void fetchValues() {
            token.ensureValid();
            if (n != null) {
                return;
            }
            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_MODULUS),
                new CK_ATTRIBUTE(CKA_PUBLIC_EXPONENT),
            };
            fetchAttributes(attributes);
            n = attributes[0].getBigInteger();
            e = attributes[1].getBigInteger();
        }
        public String getFormat() {
            token.ensureValid();
            return "X.509";
        }
        synchronized byte[] getEncodedInternal() {
            token.ensureValid();
            if (encoded == null) {
                fetchValues();
                try {
                    encoded = RSAPublicKeyImpl.newKey
                        (KeyType.RSA, null, n, e).getEncoded();
                } catch (InvalidKeyException e) {
                    throw new ProviderException(e);
                }
            }
            return encoded;
        }
        public BigInteger getModulus() {
            fetchValues();
            return n;
        }
        public BigInteger getPublicExponent() {
            fetchValues();
            return e;
        }
        public String toString() {
            fetchValues();
            return super.toString() +  "\n  modulus: " + n
                + "\n  public exponent: " + e;
        }
    }

    private static final class P11DSAPublicKey extends P11Key
                                                implements DSAPublicKey {
        private static final long serialVersionUID = 5989753793316396637L;

        private BigInteger y;
        private DSAParams params;
        private byte[] encoded;
        P11DSAPublicKey(Session session, long keyID, String algorithm,
                int keyLength, CK_ATTRIBUTE[] attributes) {
            super(PUBLIC, session, keyID, algorithm, keyLength, attributes);
        }
        private synchronized void fetchValues() {
            token.ensureValid();
            if (y != null) {
                return;
            }
            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_VALUE),
                new CK_ATTRIBUTE(CKA_PRIME),
                new CK_ATTRIBUTE(CKA_SUBPRIME),
                new CK_ATTRIBUTE(CKA_BASE),
            };
            fetchAttributes(attributes);
            y = attributes[0].getBigInteger();
            params = new DSAParameterSpec(
                attributes[1].getBigInteger(),
                attributes[2].getBigInteger(),
                attributes[3].getBigInteger()
            );
        }
        public String getFormat() {
            token.ensureValid();
            return "X.509";
        }
        synchronized byte[] getEncodedInternal() {
            token.ensureValid();
            if (encoded == null) {
                fetchValues();
                try {
                    Key key = new sun.security.provider.DSAPublicKey
                            (y, params.getP(), params.getQ(), params.getG());
                    encoded = key.getEncoded();
                } catch (InvalidKeyException e) {
                    throw new ProviderException(e);
                }
            }
            return encoded;
        }
        public BigInteger getY() {
            fetchValues();
            return y;
        }
        public DSAParams getParams() {
            fetchValues();
            return params;
        }
        public String toString() {
            fetchValues();
            return super.toString() +  "\n  y: " + y + "\n  p: " + params.getP()
                + "\n  q: " + params.getQ() + "\n  g: " + params.getG();
        }
    }

    private static final class P11DSAPrivateKey extends P11Key
                                                implements DSAPrivateKey {
        private static final long serialVersionUID = 3119629997181999389L;

        private BigInteger x;
        private DSAParams params;
        private byte[] encoded;
        P11DSAPrivateKey(Session session, long keyID, String algorithm,
                int keyLength, CK_ATTRIBUTE[] attributes) {
            super(PRIVATE, session, keyID, algorithm, keyLength, attributes);
        }
        private synchronized void fetchValues() {
            token.ensureValid();
            if (x != null) {
                return;
            }
            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_VALUE),
                new CK_ATTRIBUTE(CKA_PRIME),
                new CK_ATTRIBUTE(CKA_SUBPRIME),
                new CK_ATTRIBUTE(CKA_BASE),
            };
            fetchAttributes(attributes);
            x = attributes[0].getBigInteger();
            params = new DSAParameterSpec(
                attributes[1].getBigInteger(),
                attributes[2].getBigInteger(),
                attributes[3].getBigInteger()
            );
        }
        public String getFormat() {
            token.ensureValid();
            return "PKCS#8";
        }
        synchronized byte[] getEncodedInternal() {
            token.ensureValid();
            if (encoded == null) {
                fetchValues();
                Key key = new sun.security.provider.DSAPrivateKey
                        (x, params.getP(), params.getQ(), params.getG());
                encoded = key.getEncoded();
            }
            return encoded;
        }
        public BigInteger getX() {
            fetchValues();
            return x;
        }
        public DSAParams getParams() {
            fetchValues();
            return params;
        }
    }

    private static final class P11DHPrivateKey extends P11Key
                                                implements DHPrivateKey {
        private static final long serialVersionUID = -1698576167364928838L;

        private BigInteger x;
        private DHParameterSpec params;
        private byte[] encoded;
        P11DHPrivateKey(Session session, long keyID, String algorithm,
                int keyLength, CK_ATTRIBUTE[] attributes) {
            super(PRIVATE, session, keyID, algorithm, keyLength, attributes);
        }
        private synchronized void fetchValues() {
            token.ensureValid();
            if (x != null) {
                return;
            }
            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_VALUE),
                new CK_ATTRIBUTE(CKA_PRIME),
                new CK_ATTRIBUTE(CKA_BASE),
            };
            fetchAttributes(attributes);
            x = attributes[0].getBigInteger();
            params = new DHParameterSpec(
                attributes[1].getBigInteger(),
                attributes[2].getBigInteger()
            );
        }
        public String getFormat() {
            token.ensureValid();
            return "PKCS#8";
        }
        synchronized byte[] getEncodedInternal() {
            token.ensureValid();
            if (encoded == null) {
                fetchValues();
                try {
                    DHPrivateKeySpec spec = new DHPrivateKeySpec
                        (x, params.getP(), params.getG());
                    KeyFactory kf = KeyFactory.getInstance
                        ("DH", P11Util.getSunJceProvider());
                    Key key = kf.generatePrivate(spec);
                    encoded = key.getEncoded();
                } catch (GeneralSecurityException e) {
                    throw new ProviderException(e);
                }
            }
            return encoded;
        }
        public BigInteger getX() {
            fetchValues();
            return x;
        }
        public DHParameterSpec getParams() {
            fetchValues();
            return params;
        }
        public int hashCode() {
            if (!token.isValid()) {
                return 0;
            }
            fetchValues();
            return Objects.hash(x, params.getP(), params.getG());
        }
        public boolean equals(Object obj) {
            if (this == obj) return true;
            // equals() should never throw exceptions
            if (!token.isValid()) {
                return false;
            }
            if (!(obj instanceof DHPrivateKey)) {
                return false;
            }
            fetchValues();
            DHPrivateKey other = (DHPrivateKey) obj;
            DHParameterSpec otherParams = other.getParams();
            return ((this.x.compareTo(other.getX()) == 0) &&
                    (this.params.getP().compareTo(otherParams.getP()) == 0) &&
                    (this.params.getG().compareTo(otherParams.getG()) == 0));
        }
    }

    private static final class P11DHPublicKey extends P11Key
                                                implements DHPublicKey {
        static final long serialVersionUID = -598383872153843657L;

        private BigInteger y;
        private DHParameterSpec params;
        private byte[] encoded;
        P11DHPublicKey(Session session, long keyID, String algorithm,
                int keyLength, CK_ATTRIBUTE[] attributes) {
            super(PUBLIC, session, keyID, algorithm, keyLength, attributes);
        }
        private synchronized void fetchValues() {
            token.ensureValid();
            if (y != null) {
                return;
            }
            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_VALUE),
                new CK_ATTRIBUTE(CKA_PRIME),
                new CK_ATTRIBUTE(CKA_BASE),
            };
            fetchAttributes(attributes);
            y = attributes[0].getBigInteger();
            params = new DHParameterSpec(
                attributes[1].getBigInteger(),
                attributes[2].getBigInteger()
            );
        }
        public String getFormat() {
            token.ensureValid();
            return "X.509";
        }
        synchronized byte[] getEncodedInternal() {
            token.ensureValid();
            if (encoded == null) {
                fetchValues();
                try {
                    DHPublicKeySpec spec = new DHPublicKeySpec
                        (y, params.getP(), params.getG());
                    KeyFactory kf = KeyFactory.getInstance
                        ("DH", P11Util.getSunJceProvider());
                    Key key = kf.generatePublic(spec);
                    encoded = key.getEncoded();
                } catch (GeneralSecurityException e) {
                    throw new ProviderException(e);
                }
            }
            return encoded;
        }
        public BigInteger getY() {
            fetchValues();
            return y;
        }
        public DHParameterSpec getParams() {
            fetchValues();
            return params;
        }
        public String toString() {
            fetchValues();
            return super.toString() +  "\n  y: " + y + "\n  p: " + params.getP()
                + "\n  g: " + params.getG();
        }
        public int hashCode() {
            if (token.isValid() == false) {
                return 0;
            }
            fetchValues();
            return Objects.hash(y, params.getP(), params.getG());
        }
        public boolean equals(Object obj) {
            if (this == obj) return true;
            // equals() should never throw exceptions
            if (token.isValid() == false) {
                return false;
            }
            if (!(obj instanceof DHPublicKey)) {
                return false;
            }
            fetchValues();
            DHPublicKey other = (DHPublicKey) obj;
            DHParameterSpec otherParams = other.getParams();
            return ((this.y.compareTo(other.getY()) == 0) &&
                    (this.params.getP().compareTo(otherParams.getP()) == 0) &&
                    (this.params.getG().compareTo(otherParams.getG()) == 0));
        }
    }

    private static final class P11ECPrivateKey extends P11Key
                                                implements ECPrivateKey {
        private static final long serialVersionUID = -7786054399510515515L;

        private BigInteger s;
        private ECParameterSpec params;
        private byte[] encoded;
        P11ECPrivateKey(Session session, long keyID, String algorithm,
                int keyLength, CK_ATTRIBUTE[] attributes) {
            super(PRIVATE, session, keyID, algorithm, keyLength, attributes);
        }
        private synchronized void fetchValues() {
            token.ensureValid();
            if (s != null) {
                return;
            }
            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_VALUE),
                new CK_ATTRIBUTE(CKA_EC_PARAMS, params),
            };
            fetchAttributes(attributes);
            s = attributes[0].getBigInteger();
            try {
                params = P11ECKeyFactory.decodeParameters
                            (attributes[1].getByteArray());
            } catch (Exception e) {
                throw new RuntimeException("Could not parse key values", e);
            }
        }
        public String getFormat() {
            token.ensureValid();
            return "PKCS#8";
        }
        synchronized byte[] getEncodedInternal() {
            token.ensureValid();
            if (encoded == null) {
                fetchValues();
                try {
                    Key key = ECUtil.generateECPrivateKey(s, params);
                    encoded = key.getEncoded();
                } catch (InvalidKeySpecException e) {
                    throw new ProviderException(e);
                }
            }
            return encoded;
        }
        public BigInteger getS() {
            fetchValues();
            return s;
        }
        public ECParameterSpec getParams() {
            fetchValues();
            return params;
        }
    }

    private static final class P11ECPublicKey extends P11Key
                                                implements ECPublicKey {
        private static final long serialVersionUID = -6371481375154806089L;

        private ECPoint w;
        private ECParameterSpec params;
        private byte[] encoded;
        P11ECPublicKey(Session session, long keyID, String algorithm,
                int keyLength, CK_ATTRIBUTE[] attributes) {
            super(PUBLIC, session, keyID, algorithm, keyLength, attributes);
        }
        private synchronized void fetchValues() {
            token.ensureValid();
            if (w != null) {
                return;
            }
            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_EC_POINT),
                new CK_ATTRIBUTE(CKA_EC_PARAMS),
            };
            fetchAttributes(attributes);

            try {
                params = P11ECKeyFactory.decodeParameters
                            (attributes[1].getByteArray());
                byte[] ecKey = attributes[0].getByteArray();

                // Check whether the X9.63 encoding of an EC point is wrapped
                // in an ASN.1 OCTET STRING
                if (!token.config.getUseEcX963Encoding()) {
                    DerValue wECPoint = new DerValue(ecKey);

                    if (wECPoint.getTag() != DerValue.tag_OctetString) {
                        throw new IOException("Could not DER decode EC point." +
                            " Unexpected tag: " + wECPoint.getTag());
                    }
                    w = P11ECKeyFactory.decodePoint
                        (wECPoint.getDataBytes(), params.getCurve());

                } else {
                    w = P11ECKeyFactory.decodePoint(ecKey, params.getCurve());
                }

            } catch (Exception e) {
                throw new RuntimeException("Could not parse key values", e);
            }
        }
        public String getFormat() {
            token.ensureValid();
            return "X.509";
        }
        synchronized byte[] getEncodedInternal() {
            token.ensureValid();
            if (encoded == null) {
                fetchValues();
                try {
                    return ECUtil.x509EncodeECPublicKey(w, params);
                } catch (InvalidKeySpecException e) {
                    throw new ProviderException(e);
                }
            }
            return encoded;
        }
        public ECPoint getW() {
            fetchValues();
            return w;
        }
        public ECParameterSpec getParams() {
            fetchValues();
            return params;
        }
        public String toString() {
            fetchValues();
            return super.toString()
                + "\n  public x coord: " + w.getAffineX()
                + "\n  public y coord: " + w.getAffineY()
                + "\n  parameters: " + params;
        }
    }
}
final class NativeKeyHolder {

    private static long nativeKeyWrapperKeyID = 0;
    private static CK_MECHANISM nativeKeyWrapperMechanism = null;
    private static long nativeKeyWrapperRefCount = 0;
    private static Session nativeKeyWrapperSession = null;

    private final P11Key p11Key;
    private final byte[] nativeKeyInfo;
    private boolean wrapperKeyUsed;

    // destroyed and recreated when refCount toggles to 1
    private long keyID;

    // phantom reference notification clean up for session keys
    private SessionKeyRef ref;

    private int refCount;

    private static void createNativeKeyWrapper(Token token)
            throws PKCS11Exception {
        assert(nativeKeyWrapperKeyID == 0);
        assert(nativeKeyWrapperRefCount == 0);
        assert(nativeKeyWrapperSession == null);
        // Create a global wrapping/unwrapping key
        CK_ATTRIBUTE[] wrappingAttributes = token.getAttributes(O_GENERATE,
                        CKO_SECRET_KEY, CKK_AES, new CK_ATTRIBUTE[] {
                                new CK_ATTRIBUTE(CKA_CLASS, CKO_SECRET_KEY),
                                new CK_ATTRIBUTE(CKA_VALUE_LEN, 256 >> 3)});
        Session s = null;
        try {
            s = token.getObjSession();
            nativeKeyWrapperKeyID = token.p11.C_GenerateKey(
                    s.id(), new CK_MECHANISM(CKM_AES_KEY_GEN),
                    wrappingAttributes);
            nativeKeyWrapperSession = s;
            nativeKeyWrapperSession.addObject();
            byte[] iv = new byte[16];
            JCAUtil.getSecureRandom().nextBytes(iv);
            nativeKeyWrapperMechanism = new CK_MECHANISM(CKM_AES_CBC_PAD, iv);
        } catch (PKCS11Exception e) {
            // best effort
        } finally {
            token.releaseSession(s);
        }
    }

    private static void deleteNativeKeyWrapper() {
        Token token = nativeKeyWrapperSession.token;
        if (token.isValid()) {
            Session s = null;
            try {
                s = token.getOpSession();
                token.p11.C_DestroyObject(s.id(), nativeKeyWrapperKeyID);
                nativeKeyWrapperSession.removeObject();
            } catch (PKCS11Exception e) {
                // best effort
            } finally {
                token.releaseSession(s);
            }
        }
        nativeKeyWrapperKeyID = 0;
        nativeKeyWrapperMechanism = null;
        nativeKeyWrapperSession = null;
    }

    static void decWrapperKeyRef() {
        synchronized(NativeKeyHolder.class) {
            assert(nativeKeyWrapperKeyID != 0);
            assert(nativeKeyWrapperRefCount > 0);
            nativeKeyWrapperRefCount--;
            if (nativeKeyWrapperRefCount == 0) {
                deleteNativeKeyWrapper();
            }
        }
    }

    NativeKeyHolder(P11Key p11Key, long keyID, Session keySession,
            boolean extractKeyInfo, boolean isTokenObject) {
        this.p11Key = p11Key;
        this.keyID = keyID;
        this.refCount = -1;
        byte[] ki = null;
        if (isTokenObject) {
            this.ref = null;
        } else {
            // Try extracting key info, if any error, disable it
            Token token = p11Key.token;
            if (extractKeyInfo) {
                try {
                    if (p11Key.sensitive) {
                        // p11Key native key information has to be wrapped
                        synchronized(NativeKeyHolder.class) {
                            if (nativeKeyWrapperKeyID == 0) {
                                createNativeKeyWrapper(token);
                            }
                            // If a wrapper-key was successfully created or
                            // already exists, increment its reference
                            // counter to keep it alive while native key
                            // information is being held.
                            if (nativeKeyWrapperKeyID != 0) {
                                nativeKeyWrapperRefCount++;
                                wrapperKeyUsed = true;
                            }
                        }
                    }
                    Session opSession = null;
                    try {
                        opSession = token.getOpSession();
                        ki = p11Key.token.p11.getNativeKeyInfo(opSession.id(),
                                keyID, nativeKeyWrapperKeyID,
                                nativeKeyWrapperMechanism);
                    } catch (PKCS11Exception e) {
                        // best effort
                    } finally {
                        token.releaseSession(opSession);
                    }
                } catch (PKCS11Exception e) {
                    // best effort
                }
            }
            this.ref = new SessionKeyRef(p11Key, keyID, wrapperKeyUsed,
                    keySession);
        }

        this.nativeKeyInfo = ((ki == null || ki.length == 0)? null : ki);
    }

    long getKeyID() throws ProviderException {
        if (this.nativeKeyInfo != null) {
            synchronized(this.nativeKeyInfo) {
                if (this.refCount == -1) {
                    this.refCount = 0;
                }
                int cnt = (this.refCount)++;
                if (keyID == 0) {
                    if (cnt != 0) {
                        throw new RuntimeException(
                                "Error: null keyID with non-zero refCount " + cnt);
                    }
                    Token token = p11Key.token;
                    // Create keyID using nativeKeyInfo
                    Session session = null;
                    try {
                        session = token.getObjSession();
                        this.keyID = token.p11.createNativeKey(session.id(),
                                nativeKeyInfo, nativeKeyWrapperKeyID,
                                nativeKeyWrapperMechanism);
                        this.ref.registerNativeKey(this.keyID, session);
                    } catch (PKCS11Exception e) {
                        this.refCount--;
                        throw new ProviderException("Error recreating native key", e);
                    } finally {
                        token.releaseSession(session);
                    }
                } else {
                    if (cnt < 0) {
                        throw new RuntimeException("ERROR: negative refCount");
                    }
                }
            }
        }
        return keyID;
    }

    void releaseKeyID() {
        if (this.nativeKeyInfo != null) {
            synchronized(this.nativeKeyInfo) {
                if (this.refCount == -1) {
                    throw new RuntimeException("Error: miss match getKeyID call");
                }
                int cnt = --(this.refCount);
                if (cnt == 0) {
                    // destroy
                    if (this.keyID == 0) {
                        throw new RuntimeException("ERROR: null keyID can't be destroyed");
                    }

                    // destroy
                    this.keyID = 0;
                    this.ref.removeNativeKey();
                } else {
                    if (cnt < 0) {
                        // should never happen as we start count at 1 and pair get/release calls
                        throw new RuntimeException("wrong refCount value: " + cnt);
                    }
                }
            }
        }
    }
}

/*
 * NOTE: Must use PhantomReference here and not WeakReference
 * otherwise the key maybe cleared before other objects which
 * still use these keys during finalization such as SSLSocket.
 */
final class SessionKeyRef extends PhantomReference<P11Key> {
    static ReferenceQueue<P11Key> refQueue = new ReferenceQueue<>();
    private static Set<SessionKeyRef> refSet =
        Collections.synchronizedSet(new HashSet<>());

    // handle to the native key and the session it is generated under
    private long keyID;
    private Session session;
    private boolean wrapperKeyUsed;

    SessionKeyRef(P11Key p11Key, long keyID, boolean wrapperKeyUsed,
            Session session) {
        super(p11Key, refQueue);
        if (session == null) {
            throw new ProviderException
                    ("key must be associated with a session");
        }
        registerNativeKey(keyID, session);
        this.wrapperKeyUsed = wrapperKeyUsed;

        refSet.add(this);
    }

    void registerNativeKey(long newKeyID, Session newSession) {
        assert(newKeyID != 0);
        assert(newSession != null);
        updateNativeKey(newKeyID, newSession);
    }

    void removeNativeKey() {
        assert(session != null);
        updateNativeKey(0, null);
    }

    private void updateNativeKey(long newKeyID, Session newSession) {
        if (newKeyID == 0) {
            assert(newSession == null);
            Token token = session.token;
            // If the token is still valid, try to remove the key object
            if (token.isValid()) {
                Session s = null;
                try {
                    s = token.getOpSession();
                    token.p11.C_DestroyObject(s.id(), this.keyID);
                } catch (PKCS11Exception e) {
                    // best effort
                } finally {
                    token.releaseSession(s);
                }
            }
            session.removeObject();
        } else {
            newSession.addObject();
        }
        keyID = newKeyID;
        session = newSession;
    }

    // Called when the GC disposes a p11Key
    void dispose() {
        if (wrapperKeyUsed) {
            // Wrapper-key no longer needed for
            // p11Key native key information
            NativeKeyHolder.decWrapperKeyRef();
        }
        if (keyID != 0) {
            removeNativeKey();
        }
        refSet.remove(this);
        this.clear();
    }
}
