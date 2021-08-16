/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 */
package org.jcp.xml.dsig.internal.dom;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.ECField;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dom.DOMCryptoContext;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.keyinfo.KeyValue;

import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * DOM-based implementation of KeyValue.
 *
 */
public abstract class DOMKeyValue<K extends PublicKey> extends DOMStructure implements KeyValue {

    private static final String XMLDSIG_11_XMLNS
        = "http://www.w3.org/2009/xmldsig11#";
    private final K publicKey;

    public DOMKeyValue(K key) throws KeyException {
        if (key == null) {
            throw new NullPointerException("key cannot be null");
        }
        this.publicKey = key;
    }

    /**
     * Creates a {@code DOMKeyValue} from an element.
     *
     * @param kvtElem a KeyValue child element
     */
    public DOMKeyValue(Element kvtElem) throws MarshalException {
        this.publicKey = unmarshalKeyValue(kvtElem);
    }

    static KeyValue unmarshal(Element kvElem) throws MarshalException {
        Element kvtElem = DOMUtils.getFirstChildElement(kvElem);
        if (kvtElem == null) {
            throw new MarshalException("KeyValue must contain at least one type");
        }

        String namespace = kvtElem.getNamespaceURI();
        if ("DSAKeyValue".equals(kvtElem.getLocalName()) && XMLSignature.XMLNS.equals(namespace)) {
            return new DSA(kvtElem);
        } else if ("RSAKeyValue".equals(kvtElem.getLocalName()) && XMLSignature.XMLNS.equals(namespace)) {
            return new RSA(kvtElem);
        } else if ("ECKeyValue".equals(kvtElem.getLocalName()) && XMLDSIG_11_XMLNS.equals(namespace)) {
            return new EC(kvtElem);
        } else {
            return new Unknown(kvtElem);
        }
    }

    public PublicKey getPublicKey() throws KeyException {
        if (publicKey == null) {
            throw new KeyException("can't convert KeyValue to PublicKey");
        } else {
            return publicKey;
        }
    }

    @Override
    public void marshal(Node parent, String dsPrefix, DOMCryptoContext context)
        throws MarshalException
    {
        Document ownerDoc = DOMUtils.getOwnerDocument(parent);

        // create KeyValue element
        Element kvElem = DOMUtils.createElement(ownerDoc, "KeyValue",
                                                XMLSignature.XMLNS, dsPrefix);
        marshalPublicKey(kvElem, ownerDoc, dsPrefix, context);

        parent.appendChild(kvElem);
    }

    abstract void marshalPublicKey(Node parent, Document doc, String dsPrefix,
        DOMCryptoContext context) throws MarshalException;

    abstract K unmarshalKeyValue(Element kvtElem)
        throws MarshalException;

    private static PublicKey generatePublicKey(KeyFactory kf, KeySpec keyspec) {
        try {
            return kf.generatePublic(keyspec);
        } catch (InvalidKeySpecException e) {
            //@@@ should dump exception to LOG
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof KeyValue)) {
            return false;
        }
        try {
            KeyValue kv = (KeyValue)obj;
            if (publicKey == null ) {
                if (kv.getPublicKey() != null) {
                    return false;
                }
            } else if (!publicKey.equals(kv.getPublicKey())) {
                return false;
            }
        } catch (KeyException ke) {
            // no practical way to determine if the keys are equal
            return false;
        }

        return true;
    }

    public static BigInteger decode(Element elem) throws MarshalException {
        try {
            String base64str = elem.getFirstChild().getNodeValue();
            return new BigInteger(1, XMLUtils.decode(base64str));
        } catch (Exception ex) {
            throw new MarshalException(ex);
        }
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (publicKey != null) {
            result = 31 * result + publicKey.hashCode();
        }

        return result;
    }

    static final class RSA extends DOMKeyValue<RSAPublicKey> {
        // RSAKeyValue CryptoBinaries
        private DOMCryptoBinary modulus, exponent;
        private KeyFactory rsakf;

        RSA(RSAPublicKey key) throws KeyException {
            super(key);
            RSAPublicKey rkey = key;
            exponent = new DOMCryptoBinary(rkey.getPublicExponent());
            modulus = new DOMCryptoBinary(rkey.getModulus());
        }

        RSA(Element elem) throws MarshalException {
            super(elem);
        }

        void marshalPublicKey(Node parent, Document doc, String dsPrefix,
            DOMCryptoContext context) throws MarshalException {
            Element rsaElem = DOMUtils.createElement(doc, "RSAKeyValue",
                                                     XMLSignature.XMLNS,
                                                     dsPrefix);
            Element modulusElem = DOMUtils.createElement(doc, "Modulus",
                                                         XMLSignature.XMLNS,
                                                         dsPrefix);
            Element exponentElem = DOMUtils.createElement(doc, "Exponent",
                                                          XMLSignature.XMLNS,
                                                          dsPrefix);
            modulus.marshal(modulusElem, dsPrefix, context);
            exponent.marshal(exponentElem, dsPrefix, context);
            rsaElem.appendChild(modulusElem);
            rsaElem.appendChild(exponentElem);
            parent.appendChild(rsaElem);
        }

        @Override
        RSAPublicKey unmarshalKeyValue(Element kvtElem)
            throws MarshalException
        {
            if (rsakf == null) {
                try {
                    rsakf = KeyFactory.getInstance("RSA");
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException
                        ("unable to create RSA KeyFactory: " + e.getMessage());
                }
            }
            Element modulusElem = DOMUtils.getFirstChildElement(kvtElem,
                                                                "Modulus",
                                                                XMLSignature.XMLNS);
            BigInteger modulus = decode(modulusElem);
            Element exponentElem = DOMUtils.getNextSiblingElement(modulusElem,
                                                                  "Exponent",
                                                                  XMLSignature.XMLNS);
            BigInteger exponent = decode(exponentElem);
            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            return (RSAPublicKey) generatePublicKey(rsakf, spec);
        }
    }

    static final class DSA extends DOMKeyValue<DSAPublicKey> {
        // DSAKeyValue CryptoBinaries
        private DOMCryptoBinary p, q, g, y; //, seed, pgen;
        private KeyFactory dsakf;

        DSA(DSAPublicKey key) throws KeyException {
            super(key);
            DSAPublicKey dkey = key;
            DSAParams params = dkey.getParams();
            p = new DOMCryptoBinary(params.getP());
            q = new DOMCryptoBinary(params.getQ());
            g = new DOMCryptoBinary(params.getG());
            y = new DOMCryptoBinary(dkey.getY());
        }

        DSA(Element elem) throws MarshalException {
            super(elem);
        }

        @Override
        void marshalPublicKey(Node parent, Document doc, String dsPrefix,
                              DOMCryptoContext context)
            throws MarshalException
        {
            Element dsaElem = DOMUtils.createElement(doc, "DSAKeyValue",
                                                     XMLSignature.XMLNS,
                                                     dsPrefix);
            // parameters J, Seed & PgenCounter are not included
            Element pElem = DOMUtils.createElement(doc, "P", XMLSignature.XMLNS,
                                                   dsPrefix);
            Element qElem = DOMUtils.createElement(doc, "Q", XMLSignature.XMLNS,
                                                   dsPrefix);
            Element gElem = DOMUtils.createElement(doc, "G", XMLSignature.XMLNS,
                                                   dsPrefix);
            Element yElem = DOMUtils.createElement(doc, "Y", XMLSignature.XMLNS,
                                                   dsPrefix);
            p.marshal(pElem, dsPrefix, context);
            q.marshal(qElem, dsPrefix, context);
            g.marshal(gElem, dsPrefix, context);
            y.marshal(yElem, dsPrefix, context);
            dsaElem.appendChild(pElem);
            dsaElem.appendChild(qElem);
            dsaElem.appendChild(gElem);
            dsaElem.appendChild(yElem);
            parent.appendChild(dsaElem);
        }

        @Override
        DSAPublicKey unmarshalKeyValue(Element kvtElem)
            throws MarshalException
        {
            if (dsakf == null) {
                try {
                    dsakf = KeyFactory.getInstance("DSA");
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException
                        ("unable to create DSA KeyFactory: " + e.getMessage());
                }
            }
            Element curElem = DOMUtils.getFirstChildElement(kvtElem);
            if (curElem == null) {
                throw new MarshalException("KeyValue must contain at least one type");
            }
            // check for P and Q
            BigInteger p = null;
            BigInteger q = null;
            if ("P".equals(curElem.getLocalName()) && XMLSignature.XMLNS.equals(curElem.getNamespaceURI())) {
                p = decode(curElem);
                curElem = DOMUtils.getNextSiblingElement(curElem, "Q", XMLSignature.XMLNS);
                q = decode(curElem);
                curElem = DOMUtils.getNextSiblingElement(curElem);
            }
            BigInteger g = null;
            if (curElem != null
                && "G".equals(curElem.getLocalName()) && XMLSignature.XMLNS.equals(curElem.getNamespaceURI())) {
                g = decode(curElem);
                curElem = DOMUtils.getNextSiblingElement(curElem, "Y", XMLSignature.XMLNS);
            }
            BigInteger y = null;
            if (curElem != null) {
                y = decode(curElem);
                curElem = DOMUtils.getNextSiblingElement(curElem);
            }
            //if (curElem != null && "J".equals(curElem.getLocalName())) {
                //j = new DOMCryptoBinary(curElem.getFirstChild());
                // curElem = DOMUtils.getNextSiblingElement(curElem);
            //}
            //@@@ do we care about j, pgenCounter or seed?
            DSAPublicKeySpec spec = new DSAPublicKeySpec(y, p, q, g);
            return (DSAPublicKey) generatePublicKey(dsakf, spec);
        }
    }

    static final class EC extends DOMKeyValue<ECPublicKey> {
     // ECKeyValue CryptoBinaries
        private byte[] ecPublicKey;
        private KeyFactory eckf;
        private ECParameterSpec ecParams;

        /* Supported curve, secp256r1 */
        private static final Curve SECP256R1 = initializeCurve(
            "secp256r1 [NIST P-256, X9.62 prime256v1]",
            "1.2.840.10045.3.1.7",
            "FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF",
            "FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFC",
            "5AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B",
            "6B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296",
            "4FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5",
            "FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551",
            1
        );

        /* Supported curve secp384r1 */
        private static final Curve SECP384R1 = initializeCurve(
            "secp384r1 [NIST P-384]",
            "1.3.132.0.34",
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFF0000000000000000FFFFFFFF",
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFF0000000000000000FFFFFFFC",
            "B3312FA7E23EE7E4988E056BE3F82D19181D9C6EFE8141120314088F5013875AC656398D8A2ED19D2A85C8EDD3EC2AEF",
            "AA87CA22BE8B05378EB1C71EF320AD746E1D3B628BA79B9859F741E082542A385502F25DBF55296C3A545E3872760AB7",
            "3617DE4A96262C6F5D9E98BF9292DC29F8F41DBD289A147CE9DA3113B5F0B8C00A60B1CE1D7E819D7A431D7C90EA0E5F",
            "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7634D81F4372DDF581A0DB248B0A77AECEC196ACCC52973",
            1
        );

        /* Supported curve secp521r1 */
        private static final Curve SECP521R1 = initializeCurve(
            "secp521r1 [NIST P-521]",
            "1.3.132.0.35",
            "01FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",
            "01FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC",
            "0051953EB9618E1C9A1F929A21A0B68540EEA2DA725B99B315F3B8B489918EF109E156193951EC7E937B1652C0BD3BB1BF073573DF883D2C34F1EF451FD46B503F00",
            "00C6858E06B70404E9CD9E3ECB662395B4429C648139053FB521F828AF606B4D3DBAA14B5E77EFE75928FE1DC127A2FFA8DE3348B3C1856A429BF97E7E31C2E5BD66",
            "011839296A789A3BC0045C8A5FB42C7D1BD998F54449579B446817AFBD17273E662C97EE72995EF42640C550B9013FAD0761353C7086A272C24088BE94769FD16650",
            "01FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFA51868783BF2F966B7FCC0148F709A5D03BB5C9B8899C47AEBB6FB71E91386409",
            1
        );

        private static Curve initializeCurve(String name, String oid,
                String sfield, String a, String b,
                String x, String y, String n, int h) {
            BigInteger p = bigInt(sfield);
            ECField field = new ECFieldFp(p);
            EllipticCurve curve = new EllipticCurve(field, bigInt(a),
                                                    bigInt(b));
            ECPoint g = new ECPoint(bigInt(x), bigInt(y));
            return new Curve(name, oid, curve, g, bigInt(n), h);
        }

        EC(ECPublicKey ecKey) throws KeyException {
            super(ecKey);
            ECPoint ecPoint = ecKey.getW();
            ecParams = ecKey.getParams();
            ecPublicKey = encodePoint(ecPoint, ecParams.getCurve());
        }

        EC(Element dmElem) throws MarshalException {
            super(dmElem);
        }

        private static ECPoint decodePoint(byte[] data, EllipticCurve curve)
                throws IOException {
            if (data.length == 0 || data[0] != 4) {
                throw new IOException("Only uncompressed point format " +
                                      "supported");
            }
            // Per ANSI X9.62, an encoded point is a 1 byte type followed by
            // ceiling(LOG base 2 field-size / 8) bytes of x and the same of y.
            int n = (data.length - 1) / 2;
            if (n != (curve.getField().getFieldSize() + 7) >> 3) {
                throw new IOException("Point does not match field size");
            }

            byte[] xb = Arrays.copyOfRange(data, 1, 1 + n);
            byte[] yb = Arrays.copyOfRange(data, n + 1, n + 1 + n);

            return new ECPoint(new BigInteger(1, xb), new BigInteger(1, yb));
        }

        private static byte[] encodePoint(ECPoint point, EllipticCurve curve) {
            // get field size in bytes (rounding up)
            int n = (curve.getField().getFieldSize() + 7) >> 3;
            byte[] xb = trimZeroes(point.getAffineX().toByteArray());
            byte[] yb = trimZeroes(point.getAffineY().toByteArray());
            if (xb.length > n || yb.length > n) {
                throw new RuntimeException("Point coordinates do not " +
                                           "match field size");
            }
            byte[] b = new byte[1 + (n << 1)];
            b[0] = 4; // uncompressed
            System.arraycopy(xb, 0, b, n - xb.length + 1, xb.length);
            System.arraycopy(yb, 0, b, b.length - yb.length, yb.length);
            return b;
        }

        private static byte[] trimZeroes(byte[] b) {
            int i = 0;
            while (i < b.length - 1 && b[i] == 0) {
                i++;
            }
            if (i == 0) {
                return b;
            }
            return Arrays.copyOfRange(b, i, b.length);
        }

        private static String getCurveOid(ECParameterSpec params) {
            // Check that the params represent one of the supported
            // curves. If there is a match, return the object identifier
            // of the curve.
            Curve match;
            if (matchCurve(params, SECP256R1)) {
                match = SECP256R1;
            } else if (matchCurve(params, SECP384R1)) {
                match = SECP384R1;
            } else if (matchCurve(params, SECP521R1)) {
                match = SECP521R1;
            } else {
                return null;
            }
            return match.getObjectId();
        }

        private static boolean matchCurve(ECParameterSpec params, Curve curve) {
            int fieldSize = params.getCurve().getField().getFieldSize();
            return curve.getCurve().getField().getFieldSize() == fieldSize
                && curve.getCurve().equals(params.getCurve())
                && curve.getGenerator().equals(params.getGenerator())
                && curve.getOrder().equals(params.getOrder())
                && curve.getCofactor() == params.getCofactor();
        }

        @Override
        void marshalPublicKey(Node parent, Document doc, String dsPrefix,
                              DOMCryptoContext context)
            throws MarshalException
        {
            String prefix = DOMUtils.getNSPrefix(context, XMLDSIG_11_XMLNS);
            Element ecKeyValueElem = DOMUtils.createElement(doc, "ECKeyValue",
                                                            XMLDSIG_11_XMLNS,
                                                            prefix);
            Element namedCurveElem = DOMUtils.createElement(doc, "NamedCurve",
                                                            XMLDSIG_11_XMLNS,
                                                            prefix);
            Element publicKeyElem = DOMUtils.createElement(doc, "PublicKey",
                                                           XMLDSIG_11_XMLNS,
                                                           prefix);
            String oid = getCurveOid(ecParams);
            if (oid == null) {
                throw new MarshalException("Invalid ECParameterSpec");
            }
            DOMUtils.setAttribute(namedCurveElem, "URI", "urn:oid:" + oid);
            String qname = (prefix == null || prefix.length() == 0)
                       ? "xmlns" : "xmlns:" + prefix;
            namedCurveElem.setAttributeNS("http://www.w3.org/2000/xmlns/",
                                          qname, XMLDSIG_11_XMLNS);
            ecKeyValueElem.appendChild(namedCurveElem);
            String encoded = XMLUtils.encodeToString(ecPublicKey);
            publicKeyElem.appendChild
                (DOMUtils.getOwnerDocument(publicKeyElem).createTextNode(encoded));
            ecKeyValueElem.appendChild(publicKeyElem);
            parent.appendChild(ecKeyValueElem);
        }

        @Override
        ECPublicKey unmarshalKeyValue(Element kvtElem)
            throws MarshalException
        {
            if (eckf == null) {
                try {
                    eckf = KeyFactory.getInstance("EC");
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException
                        ("unable to create EC KeyFactory: " + e.getMessage());
                }
            }
            ECParameterSpec ecParams = null;
            Element curElem = DOMUtils.getFirstChildElement(kvtElem);
            if (curElem == null) {
                throw new MarshalException("KeyValue must contain at least one type");
            }

            if ("ECParameters".equals(curElem.getLocalName())
                && XMLDSIG_11_XMLNS.equals(curElem.getNamespaceURI())) {
                throw new UnsupportedOperationException
                    ("ECParameters not supported");
            } else if ("NamedCurve".equals(curElem.getLocalName())
                && XMLDSIG_11_XMLNS.equals(curElem.getNamespaceURI())) {
                String uri = DOMUtils.getAttributeValue(curElem, "URI");
                // strip off "urn:oid"
                if (uri.startsWith("urn:oid:")) {
                    String oid = uri.substring("urn:oid:".length());
                    ecParams = getECParameterSpec(oid);
                    if (ecParams == null) {
                        throw new MarshalException("Invalid curve OID");
                    }
                } else {
                    throw new MarshalException("Invalid NamedCurve URI");
                }
            } else {
                throw new MarshalException("Invalid ECKeyValue");
            }
            curElem = DOMUtils.getNextSiblingElement(curElem, "PublicKey", XMLDSIG_11_XMLNS);
            ECPoint ecPoint = null;

            try {
                String content = XMLUtils.getFullTextChildrenFromNode(curElem);
                ecPoint = decodePoint(XMLUtils.decode(content),
                                      ecParams.getCurve());
            } catch (IOException ioe) {
                throw new MarshalException("Invalid EC Point", ioe);
            }

            ECPublicKeySpec spec = new ECPublicKeySpec(ecPoint, ecParams);
            return (ECPublicKey) generatePublicKey(eckf, spec);
        }

        private static ECParameterSpec getECParameterSpec(String oid) {
            if (oid.equals(SECP256R1.getObjectId())) {
                return SECP256R1;
            } else if (oid.equals(SECP384R1.getObjectId())) {
                return SECP384R1;
            } else if (oid.equals(SECP521R1.getObjectId())) {
                return SECP521R1;
            } else {
                return null;
            }
        }

        static final class Curve extends ECParameterSpec {
            private final String name;
            private final String oid;

            Curve(String name, String oid, EllipticCurve curve,
                  ECPoint g, BigInteger n, int h) {
                super(curve, g, n, h);
                this.name = name;
                this.oid = oid;
            }

            private String getName() {
                return name;
            }

            private String getObjectId() {
                return oid;
            }
        }
    }

    private static BigInteger bigInt(String s) {
        return new BigInteger(s, 16);
    }

    static final class Unknown extends DOMKeyValue<PublicKey> {
        private javax.xml.crypto.dom.DOMStructure externalPublicKey;
        Unknown(Element elem) throws MarshalException {
            super(elem);
        }

        @Override
        PublicKey unmarshalKeyValue(Element kvElem) throws MarshalException {
            externalPublicKey = new javax.xml.crypto.dom.DOMStructure(kvElem);
            return null;
        }

        @Override
        void marshalPublicKey(Node parent, Document doc, String dsPrefix,
                              DOMCryptoContext context)
            throws MarshalException
        {
            parent.appendChild(externalPublicKey.getNode());
        }
    }
}
