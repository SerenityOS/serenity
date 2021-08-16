/*
 * Copyright (c) 2006, 2007, Oracle and/or its affiliates. All rights reserved.
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

/*
 * test
 * @bug 6377058
 * @summary SunJCE depends on sun.security.provider.SignatureImpl
 * behaviour, BC can't load into 1st slot.
 * @author Brad R. Wetmore
 */


import java.util.*;
import java.math.*;
import java.security.*;
import java.security.cert.*;
import javax.security.auth.x500.*;

public class MyX509CertImpl extends X509Certificate
        implements X509Extension {

    X509Certificate c;

    protected MyX509CertImpl(X509Certificate cert) {
        c = cert;
    }

    public void checkValidity() throws CertificateExpiredException,
            CertificateNotYetValidException {
        c.checkValidity();
    }


    public void checkValidity(Date date) throws CertificateExpiredException,
            CertificateNotYetValidException {
        c.checkValidity(date);
    }

    public int getVersion() {
        return c.getVersion();
    }

    public BigInteger getSerialNumber() {
        return c.getSerialNumber();
    }

    public Principal getIssuerDN() {
        return c.getIssuerDN();
    }

    public X500Principal getIssuerX500Principal() {
        return c.getIssuerX500Principal();
    }

    public Principal getSubjectDN() {
        return c.getSubjectDN();
    }

    public X500Principal getSubjectX500Principal() {
        return c.getSubjectX500Principal();
    }

    public Date getNotBefore() {
        return c.getNotBefore();
    }

    public Date getNotAfter() {
        return c.getNotAfter();
    }

    public byte[] getTBSCertificate()
        throws CertificateEncodingException {
        return c.getTBSCertificate();
    }

    public byte[] getSignature() {
        return c.getSignature();
    }

    public String getSigAlgName() {
        return c.getSigAlgName();
    }

    public String getSigAlgOID() {
        return c.getSigAlgOID();
    }

    public byte[] getSigAlgParams() {
        return c.getSigAlgParams();
    }

    public boolean[] getIssuerUniqueID() {
        return c.getIssuerUniqueID();
    }

    public boolean[] getSubjectUniqueID() {
        return c.getSubjectUniqueID();
    }

    public boolean[] getKeyUsage() {
        return c.getKeyUsage();
    }

    public List<String> getExtendedKeyUsage()
            throws CertificateParsingException {
        return c.getExtendedKeyUsage();
    }

    public int getBasicConstraints() {
        return c.getBasicConstraints();
    }

    public Collection<List<?>> getSubjectAlternativeNames()
        throws CertificateParsingException {
        return c.getSubjectAlternativeNames();
    }

    public Collection<List<?>> getIssuerAlternativeNames()
        throws CertificateParsingException {
        return c.getIssuerAlternativeNames();
    }

    /*
     * The following are from X509Extension
     */
    public boolean hasUnsupportedCriticalExtension() {
        return c.hasUnsupportedCriticalExtension();
    }

    public Set<String> getCriticalExtensionOIDs() {
        return c.getCriticalExtensionOIDs();
    }

    public Set<String> getNonCriticalExtensionOIDs() {
        return c.getNonCriticalExtensionOIDs();
    }

    public byte[] getExtensionValue(String oid) {
        return c.getExtensionValue(oid);
    }

    /*
     * The rest are from Certificate
     */
    public boolean equals(Object other) {
        return c.equals(other);
    }

    public int hashCode() {
        return c.hashCode();
    }

    public byte[] getEncoded()
            throws CertificateEncodingException {
        return c.getEncoded();
    }

    public void verify(PublicKey key)
            throws CertificateException, NoSuchAlgorithmException,
            InvalidKeyException, NoSuchProviderException,
            SignatureException {
        System.out.println("Trying a verify");
        try {
            c.verify(key);
        } catch (SignatureException e) {
            System.out.println("Rethrowing \"acceptable\" exception");
            throw new InvalidKeyException(
                "Rethrowing as a SignatureException", e);
        }
    }

    public void verify(PublicKey key, String sigProvider)
            throws CertificateException, NoSuchAlgorithmException,
            InvalidKeyException, NoSuchProviderException,
            SignatureException {

        System.out.println("Trying a verify");
        try {
            c.verify(key, sigProvider);
        } catch (SignatureException e) {
            System.out.println("Rethrowing \"acceptable\" exception");
            throw new InvalidKeyException(
                "Rethrowing as a SignatureException", e);
        }
    }

    public String toString() {
        return c.toString();
    }

    public PublicKey getPublicKey() {
        return c.getPublicKey();
    }
}
