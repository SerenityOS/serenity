/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertSelector;
import java.security.cert.X509Certificate;
import java.security.cert.X509CertSelector;
import java.util.*;
import javax.security.auth.x500.X500Principal;
import javax.xml.crypto.*;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dom.*;
import javax.xml.crypto.dsig.keyinfo.*;

import org.jcp.xml.dsig.internal.dom.DOMRetrievalMethod;

/**
 * A <code>KeySelector</code> that returns {@link PublicKey}s. If the
 * selector is created as trusted, it only returns public keys of trusted
 * {@link X509Certificate}s stored in a {@link KeyStore}. Otherwise, it
 * returns trusted or untrusted public keys (it doesn't care as long
 * as it finds one).
 *
 * <p>This <code>KeySelector</code> uses the specified <code>KeyStore</code>
 * to find a trusted <code>X509Certificate</code> that matches information
 * specified in the {@link KeyInfo} passed to the {@link #select} method.
 * The public key from the first match is returned. If no match,
 * <code>null</code> is returned. See the <code>select</code> method for more
 * information.
 *
 * @author Sean Mullan
 */
class X509KeySelector extends KeySelector {

    private KeyStore ks;
    private boolean trusted = true;

    /**
     * Creates a trusted <code>X509KeySelector</code>.
     *
     * @param keyStore the keystore
     * @throws KeyStoreException if the keystore has not been initialized
     * @throws NullPointerException if <code>keyStore</code> is
     *    <code>null</code>
     */
    X509KeySelector(KeyStore keyStore) throws KeyStoreException {
        this(keyStore, true);
    }

    X509KeySelector(KeyStore keyStore, boolean trusted)
        throws KeyStoreException {
        if (keyStore == null) {
            throw new NullPointerException("keyStore is null");
        }
        this.trusted = trusted;
        this.ks = keyStore;
        // test to see if KeyStore has been initialized
        this.ks.size();
    }

    /**
     * Finds a key from the keystore satisfying the specified constraints.
     *
     * <p>This method compares data contained in {@link KeyInfo} entries
     * with information stored in the <code>KeyStore</code>. The implementation
     * iterates over the KeyInfo types and returns the first {@link PublicKey}
     * of an X509Certificate in the keystore that is compatible with the
     * specified AlgorithmMethod according to the following rules for each
     * keyinfo type:
     *
     * X509Data X509Certificate: if it contains a <code>KeyUsage</code>
     *   extension that asserts the <code>digitalSignature</code> bit and
     *   matches an <code>X509Certificate</code> in the <code>KeyStore</code>.
     * X509Data X509IssuerSerial: if the serial number and issuer DN match an
     *    <code>X509Certificate</code> in the <code>KeyStore</code>.
     * X509Data X509SubjectName: if the subject DN matches an
     *    <code>X509Certificate</code> in the <code>KeyStore</code>.
     * X509Data X509SKI: if the subject key identifier matches an
     *    <code>X509Certificate</code> in the <code>KeyStore</code>.
     * KeyName: if the keyname matches an alias in the <code>KeyStore</code>.
     * RetrievalMethod: supports rawX509Certificate and X509Data types. If
     *    rawX509Certificate type, it must match an <code>X509Certificate</code>
     *    in the <code>KeyStore</code>.
     *
     * @param keyInfo a <code>KeyInfo</code> (may be <code>null</code>)
     * @param purpose the key's purpose
     * @param method the algorithm method that this key is to be used for.
     *    Only keys that are compatible with the algorithm and meet the
     *    constraints of the specified algorithm should be returned.
     * @param an <code>XMLCryptoContext</code> that may contain additional
     *    useful information for finding an appropriate key
     * @return a key selector result
     * @throws KeySelectorException if an exceptional condition occurs while
     *    attempting to find a key. Note that an inability to find a key is not
     *    considered an exception (<code>null</code> should be
     *    returned in that case). However, an error condition (ex: network
     *    communications failure) that prevented the <code>KeySelector</code>
     *    from finding a potential key should be considered an exception.
     * @throws ClassCastException if the data type of <code>method</code>
     *    is not supported by this key selector
     */
    public KeySelectorResult select(KeyInfo keyInfo,
        KeySelector.Purpose purpose, AlgorithmMethod method,
        XMLCryptoContext context) throws KeySelectorException {

        SignatureMethod sm = (SignatureMethod) method;

        try {
            // return null if keyinfo is null or keystore is empty
            if (keyInfo == null || ks.size() == 0) {
                return new SimpleKeySelectorResult(null);
            }

            // Iterate through KeyInfo types
            for (XMLStructure kiType : keyInfo.getContent()) {
                // check X509Data
                if (kiType instanceof X509Data) {
                    X509Data xd = (X509Data) kiType;
                    KeySelectorResult ksr = x509DataSelect(xd, sm);
                    if (ksr != null) {
                        return ksr;
                    }
                // check KeyName
                } else if (kiType instanceof KeyName) {
                    KeyName kn = (KeyName) kiType;
                    Certificate cert = ks.getCertificate(kn.getName());
                    if (cert != null && algEquals(sm.getAlgorithm(),
                        cert.getPublicKey().getAlgorithm())) {
                        return new SimpleKeySelectorResult(cert.getPublicKey());
                    }
                // check RetrievalMethod
                } else if (kiType instanceof RetrievalMethod) {
                    RetrievalMethod rm = (RetrievalMethod) kiType;
                    try {
                        KeySelectorResult ksr = null;
                        if (rm.getType().equals
                            (X509Data.RAW_X509_CERTIFICATE_TYPE)) {
                            OctetStreamData data = (OctetStreamData)
                                rm.dereference(context);
                            CertificateFactory cf =
                                CertificateFactory.getInstance("X.509");
                            X509Certificate cert = (X509Certificate)
                                cf.generateCertificate(data.getOctetStream());
                            ksr = certSelect(cert, sm);
                        } else if (rm.getType().equals(X509Data.TYPE)) {
                            X509Data xd = (X509Data) ((DOMRetrievalMethod) rm).
                                dereferenceAsXMLStructure(context);
                            ksr = x509DataSelect(xd, sm);
                        } else {
                            // skip; keyinfo type is not supported
                            continue;
                        }
                        if (ksr != null) {
                            return ksr;
                        }
                    } catch (Exception e) {
                        throw new KeySelectorException(e);
                    }
                }
            }
        } catch (KeyStoreException kse) {
            // throw exception if keystore is uninitialized
            throw new KeySelectorException(kse);
        }

        // return null since no match could be found
        return new SimpleKeySelectorResult(null);
    }

    /**
     * Searches the specified keystore for a certificate that matches the
     * criteria specified in the CertSelector.
     *
     * @return a KeySelectorResult containing the cert's public key if there
     *   is a match; otherwise null
     */
    private KeySelectorResult keyStoreSelect(CertSelector cs)
        throws KeyStoreException {
        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate cert = ks.getCertificate(alias);
            if (cert != null && cs.match(cert)) {
                return new SimpleKeySelectorResult(cert.getPublicKey());
            }
        }
        return null;
    }

    /**
     * Searches the specified keystore for a certificate that matches the
     * specified X509Certificate and contains a public key that is compatible
     * with the specified SignatureMethod.
     *
     * @return a KeySelectorResult containing the cert's public key if there
     *   is a match; otherwise null
     */
    private KeySelectorResult certSelect(X509Certificate xcert,
        SignatureMethod sm) throws KeyStoreException {
        // skip non-signer certs
        boolean[] keyUsage = xcert.getKeyUsage();
        if (keyUsage != null && keyUsage[0] == false) {
            return null;
        }
        String alias = ks.getCertificateAlias(xcert);
        if (alias != null) {
            PublicKey pk = ks.getCertificate(alias).getPublicKey();
            // make sure algorithm is compatible with method
            if (algEquals(sm.getAlgorithm(), pk.getAlgorithm())) {
                return new SimpleKeySelectorResult(pk);
            }
        }
        return null;
    }

    /**
     * Returns an OID of a public-key algorithm compatible with the specified
     * signature algorithm URI.
     */
    private String getPKAlgorithmOID(String algURI) {
        if (algURI.equalsIgnoreCase(SignatureMethod.DSA_SHA1)) {
            return "1.2.840.10040.4.1";
        } else if (algURI.equalsIgnoreCase(SignatureMethod.RSA_SHA1)) {
            return "1.2.840.113549.1.1";
        } else {
            return null;
        }
    }

    /**
     * A simple KeySelectorResult containing a public key.
     */
    private static class SimpleKeySelectorResult implements KeySelectorResult {
        private final Key key;
        SimpleKeySelectorResult(Key key) { this.key = key; }
        public Key getKey() { return key; }
    }

    /**
     * Checks if a JCA/JCE public key algorithm name is compatible with
     * the specified signature algorithm URI.
     */
    //@@@FIXME: this should also work for key types other than DSA/RSA
    private boolean algEquals(String algURI, String algName) {
        if (algName.equalsIgnoreCase("DSA") &&
            algURI.equalsIgnoreCase(SignatureMethod.DSA_SHA1)) {
            return true;
        } else if (algName.equalsIgnoreCase("RSA") &&
            algURI.equalsIgnoreCase(SignatureMethod.RSA_SHA1)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Searches the specified keystore for a certificate that matches an
     * entry of the specified X509Data and contains a public key that is
     * compatible with the specified SignatureMethod.
     *
     * @return a KeySelectorResult containing the cert's public key if there
     *   is a match; otherwise null
     */
    private KeySelectorResult x509DataSelect(X509Data xd, SignatureMethod sm)
        throws KeyStoreException, KeySelectorException {

        // convert signature algorithm to compatible public-key alg OID
        String algOID = getPKAlgorithmOID(sm.getAlgorithm());
        X509CertSelector subjectcs = new X509CertSelector();
        try {
            subjectcs.setSubjectPublicKeyAlgID(algOID);
        } catch (IOException ioe) {
            throw new KeySelectorException(ioe);
        }
        Collection<X509Certificate> certs = new ArrayList<>();

        for (Object o : xd.getContent()) {
            // check X509IssuerSerial
            if (o instanceof X509IssuerSerial) {
                X509IssuerSerial xis = (X509IssuerSerial) o;
                try {
                    subjectcs.setSerialNumber(xis.getSerialNumber());
                    String issuer = new X500Principal(xis.getIssuerName()).getName();
                    // strip off newline
                    if (issuer.endsWith("\n")) {
                        issuer = new String
                            (issuer.toCharArray(), 0, issuer.length()-1);
                    }
                    subjectcs.setIssuer(issuer);
                } catch (IOException ioe) {
                    throw new KeySelectorException(ioe);
                }
            // check X509SubjectName
            } else if (o instanceof String) {
                String sn = (String) o;
                try {
                    String subject = new X500Principal(sn).getName();
                    // strip off newline
                    if (subject.endsWith("\n")) {
                        subject = new String
                            (subject.toCharArray(), 0, subject.length()-1);
                    }
                    subjectcs.setSubject(subject);
                } catch (IOException ioe) {
                    throw new KeySelectorException(ioe);
                }
            // check X509SKI
            } else if (o instanceof byte[]) {
                byte[] ski = (byte[]) o;
                // DER-encode ski - required by X509CertSelector
                byte[] encodedSki = new byte[ski.length+2];
                encodedSki[0] = 0x04; // OCTET STRING tag value
                encodedSki[1] = (byte) ski.length; // length
                System.arraycopy(ski, 0, encodedSki, 2, ski.length);
                subjectcs.setSubjectKeyIdentifier(encodedSki);
            } else if (o instanceof X509Certificate) {
                certs.add((X509Certificate)o);
            // check X509CRL
            // not supported: should use CertPath API
            } else {
                // skip all other entries
                continue;
            }
        }
        KeySelectorResult ksr = keyStoreSelect(subjectcs);
        if (ksr != null) {
            return ksr;
        }
        if (!certs.isEmpty() && !trusted) {
            // try to find public key in certs in X509Data
            for (X509Certificate cert : certs) {
                if (subjectcs.match(cert)) {
                    return new SimpleKeySelectorResult(cert.getPublicKey());
                }
            }
        }
        return null;
    }
}
