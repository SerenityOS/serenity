/*
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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
package sun.security.provider.certpath;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;

import javax.security.auth.x500.X500Principal;

import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.Cache;
import sun.security.x509.X509CertImpl;
import sun.security.provider.X509Factory;

/**
 * This class represents an X.509 Certificate Pair object, which is primarily
 * used to hold a pair of cross certificates issued between Certification
 * Authorities. The ASN.1 structure is listed below. The forward certificate
 * of the CertificatePair contains a certificate issued to this CA by another
 * CA. The reverse certificate of the CertificatePair contains a certificate
 * issued by this CA to another CA. When both the forward and the reverse
 * certificates are present in the CertificatePair, the issuer name in one
 * certificate shall match the subject name in the other and vice versa, and
 * the subject public key in one certificate shall be capable of verifying the
 * digital signature on the other certificate and vice versa.  If a subject
 * public key in one certificate does not contain required key algorithm
 * parameters, then the signature check involving that key is not done.<p>
 *
 * The ASN.1 syntax for this object is:
 * <pre>
 * CertificatePair      ::=     SEQUENCE {
 *      forward [0]     Certificate OPTIONAL,
 *      reverse [1]     Certificate OPTIONAL
 *                      -- at least one of the pair shall be present -- }
 * </pre><p>
 *
 * This structure uses EXPLICIT tagging. References: Annex A of
 * X.509(2000), X.509(1997).
 *
 * @author      Sean Mullan
 * @since       1.4
 */

public class X509CertificatePair {

    /* ASN.1 explicit tags */
    private static final byte TAG_FORWARD = 0;
    private static final byte TAG_REVERSE = 1;

    private X509Certificate forward;
    private X509Certificate reverse;
    private byte[] encoded;

    private static final Cache<Object, X509CertificatePair> cache
        = Cache.newSoftMemoryCache(750);

    /**
     * Creates an empty instance of X509CertificatePair.
     */
    public X509CertificatePair() {}

    /**
     * Creates an instance of X509CertificatePair. At least one of
     * the pair must be non-null.
     *
     * @param forward The forward component of the certificate pair
     *          which represents a certificate issued to this CA by other CAs.
     * @param reverse The reverse component of the certificate pair
     *          which represents a certificate issued by this CA to other CAs.
     * @throws CertificateException If an exception occurs.
     */
    public X509CertificatePair(X509Certificate forward, X509Certificate reverse)
                throws CertificateException {
        if (forward == null && reverse == null) {
            throw new CertificateException("at least one of certificate pair "
                + "must be non-null");
        }

        this.forward = forward;
        this.reverse = reverse;

        checkPair();
    }

    /**
     * Create a new X509CertificatePair from its encoding.
     *
     * For internal use only, external code should use generateCertificatePair.
     */
    private X509CertificatePair(byte[] encoded) throws CertificateException {
        try {
            parse(new DerValue(encoded));
            this.encoded = encoded;
        } catch (IOException ex) {
            throw new CertificateException(ex.toString());
        }
        checkPair();
    }

    /**
     * Clear the cache for debugging.
     */
    public static synchronized void clearCache() {
        cache.clear();
    }

    /**
     * Create a X509CertificatePair from its encoding. Uses cache lookup
     * if possible.
     */
    public static synchronized X509CertificatePair generateCertificatePair
            (byte[] encoded) throws CertificateException {
        Object key = new Cache.EqualByteArray(encoded);
        X509CertificatePair pair = cache.get(key);
        if (pair != null) {
            return pair;
        }
        pair = new X509CertificatePair(encoded);
        key = new Cache.EqualByteArray(pair.encoded);
        cache.put(key, pair);
        return pair;
    }

    /**
     * Sets the forward component of the certificate pair.
     */
    public void setForward(X509Certificate cert) throws CertificateException {
        checkPair();
        forward = cert;
    }

    /**
     * Sets the reverse component of the certificate pair.
     */
    public void setReverse(X509Certificate cert) throws CertificateException {
        checkPair();
        reverse = cert;
    }

    /**
     * Returns the forward component of the certificate pair.
     *
     * @return The forward certificate, or null if not set.
     */
    public X509Certificate getForward() {
        return forward;
    }

    /**
     * Returns the reverse component of the certificate pair.
     *
     * @return The reverse certificate, or null if not set.
     */
    public X509Certificate getReverse() {
        return reverse;
    }

    /**
     * Return the DER encoded form of the certificate pair.
     *
     * @return The encoded form of the certificate pair.
     * @throws CerticateEncodingException If an encoding exception occurs.
     */
    public byte[] getEncoded() throws CertificateEncodingException {
        try {
            if (encoded == null) {
                DerOutputStream tmp = new DerOutputStream();
                emit(tmp);
                encoded = tmp.toByteArray();
            }
        } catch (IOException ex) {
            throw new CertificateEncodingException(ex.toString());
        }
        return encoded;
    }

    /**
     * Return a printable representation of the certificate pair.
     *
     * @return A String describing the contents of the pair.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("X.509 Certificate Pair: [\n");
        if (forward != null)
            sb.append("  Forward: ").append(forward).append("\n");
        if (reverse != null)
            sb.append("  Reverse: ").append(reverse).append("\n");
        sb.append("]");
        return sb.toString();
    }

    /* Parse the encoded bytes */
    private void parse(DerValue val)
        throws IOException, CertificateException
    {
        if (val.tag != DerValue.tag_Sequence) {
            throw new IOException
                ("Sequence tag missing for X509CertificatePair");
        }

        while (val.data != null && val.data.available() != 0) {
            DerValue opt = val.data.getDerValue();
            short tag = (byte) (opt.tag & 0x01f);
            switch (tag) {
                case TAG_FORWARD:
                    if (opt.isContextSpecific() && opt.isConstructed()) {
                        if (forward != null) {
                            throw new IOException("Duplicate forward "
                                + "certificate in X509CertificatePair");
                        }
                        opt = opt.data.getDerValue();
                        forward = X509Factory.intern
                                        (new X509CertImpl(opt.toByteArray()));
                    }
                    break;
                case TAG_REVERSE:
                    if (opt.isContextSpecific() && opt.isConstructed()) {
                        if (reverse != null) {
                            throw new IOException("Duplicate reverse "
                                + "certificate in X509CertificatePair");
                        }
                        opt = opt.data.getDerValue();
                        reverse = X509Factory.intern
                                        (new X509CertImpl(opt.toByteArray()));
                    }
                    break;
                default:
                    throw new IOException("Invalid encoding of "
                        + "X509CertificatePair");
            }
        }
        if (forward == null && reverse == null) {
            throw new CertificateException("at least one of certificate pair "
                + "must be non-null");
        }
    }

    /* Translate to encoded bytes */
    private void emit(DerOutputStream out)
        throws IOException, CertificateEncodingException
    {
        DerOutputStream tagged = new DerOutputStream();

        if (forward != null) {
            DerOutputStream tmp = new DerOutputStream();
            tmp.putDerValue(new DerValue(forward.getEncoded()));
            tagged.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                         true, TAG_FORWARD), tmp);
        }

        if (reverse != null) {
            DerOutputStream tmp = new DerOutputStream();
            tmp.putDerValue(new DerValue(reverse.getEncoded()));
            tagged.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                         true, TAG_REVERSE), tmp);
        }

        out.write(DerValue.tag_Sequence, tagged);
    }

    /*
     * Check for a valid certificate pair
     */
    private void checkPair() throws CertificateException {

        /* if either of pair is missing, return w/o error */
        if (forward == null || reverse == null) {
            return;
        }
        /*
         * If both elements of the pair are present, check that they
         * are a valid pair.
         */
        X500Principal fwSubject = forward.getSubjectX500Principal();
        X500Principal fwIssuer = forward.getIssuerX500Principal();
        X500Principal rvSubject = reverse.getSubjectX500Principal();
        X500Principal rvIssuer = reverse.getIssuerX500Principal();
        if (!fwIssuer.equals(rvSubject) || !rvIssuer.equals(fwSubject)) {
            throw new CertificateException("subject and issuer names in "
                + "forward and reverse certificates do not match");
        }

        /* check signatures unless key parameters are missing */
        try {
            PublicKey pk = reverse.getPublicKey();
            if (!(pk instanceof DSAPublicKey) ||
                        ((DSAPublicKey)pk).getParams() != null) {
                forward.verify(pk);
            }
            pk = forward.getPublicKey();
            if (!(pk instanceof DSAPublicKey) ||
                        ((DSAPublicKey)pk).getParams() != null) {
                reverse.verify(pk);
            }
        } catch (GeneralSecurityException e) {
            throw new CertificateException("invalid signature: "
                + e.getMessage());
        }
    }
}
