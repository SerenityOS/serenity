/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.CRLReason;
import java.security.cert.Extension;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import sun.security.action.GetIntegerAction;
import sun.security.util.Debug;
import sun.security.util.Event;
import sun.security.util.IOUtils;
import sun.security.validator.Validator;
import sun.security.x509.AccessDescription;
import sun.security.x509.AuthorityInfoAccessExtension;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNameInterface;
import sun.security.x509.PKIXExtensions;
import sun.security.x509.URIName;
import sun.security.x509.X509CertImpl;

/**
 * This is a class that checks the revocation status of a certificate(s) using
 * OCSP. It is not a PKIXCertPathChecker and therefore can be used outside of
 * the CertPathValidator framework. It is useful when you want to
 * just check the revocation status of a certificate, and you don't want to
 * incur the overhead of validating all of the certificates in the
 * associated certificate chain.
 *
 * @author Sean Mullan
 */
public final class OCSP {

    private static final Debug debug = Debug.getInstance("certpath");

    private static final int DEFAULT_CONNECT_TIMEOUT = 15000;

    /**
     * Integer value indicating the timeout length, in seconds, to be
     * used for the OCSP check. A timeout of zero is interpreted as
     * an infinite timeout.
     */
    private static final int CONNECT_TIMEOUT = initializeTimeout();

    /**
     * Initialize the timeout length by getting the OCSP timeout
     * system property. If the property has not been set, or if its
     * value is negative, set the timeout length to the default.
     */
    private static int initializeTimeout() {
        @SuppressWarnings("removal")
        Integer tmp = java.security.AccessController.doPrivileged(
                new GetIntegerAction("com.sun.security.ocsp.timeout"));
        if (tmp == null || tmp < 0) {
            return DEFAULT_CONNECT_TIMEOUT;
        }
        // Convert to milliseconds, as the system property will be
        // specified in seconds
        return tmp * 1000;
    }

    private OCSP() {}

    /**
     * Checks the revocation status of a list of certificates using OCSP.
     *
     * @param certIds the CertIds to be checked
     * @param responderURI the URI of the OCSP responder
     * @param issuerInfo the issuer's certificate and/or subject and public key
     * @param responderCert the OCSP responder's certificate
     * @param date the time the validity of the OCSP responder's certificate
     *    should be checked against. If null, the current time is used.
     * @param extensions zero or more OCSP extensions to be included in the
     *    request.  If no extensions are requested, an empty {@code List} must
     *    be used.  A {@code null} value is not allowed.
     * @return the OCSPResponse
     * @throws IOException if there is an exception connecting to or
     *    communicating with the OCSP responder
     * @throws CertPathValidatorException if an exception occurs while
     *    encoding the OCSP Request or validating the OCSP Response
     */
    static OCSPResponse check(List<CertId> certIds, URI responderURI,
                              OCSPResponse.IssuerInfo issuerInfo,
                              X509Certificate responderCert, Date date,
                              List<Extension> extensions, String variant)
        throws IOException, CertPathValidatorException
    {
        byte[] nonce = null;
        for (Extension ext : extensions) {
            if (ext.getId().equals(PKIXExtensions.OCSPNonce_Id.toString())) {
                nonce = ext.getValue();
            }
        }

        OCSPResponse ocspResponse = null;
        try {
            byte[] response = getOCSPBytes(certIds, responderURI, extensions);
            ocspResponse = new OCSPResponse(response);

            // verify the response
            ocspResponse.verify(certIds, issuerInfo, responderCert, date,
                    nonce, variant);
        } catch (IOException ioe) {
            throw new CertPathValidatorException(
                "Unable to determine revocation status due to network error",
                ioe, null, -1, BasicReason.UNDETERMINED_REVOCATION_STATUS);
        }

        return ocspResponse;
    }


    /**
     * Send an OCSP request, then read and return the OCSP response bytes.
     *
     * @param certIds the CertIds to be checked
     * @param responderURI the URI of the OCSP responder
     * @param extensions zero or more OCSP extensions to be included in the
     *    request.  If no extensions are requested, an empty {@code List} must
     *    be used.  A {@code null} value is not allowed.
     *
     * @return the OCSP response bytes
     *
     * @throws IOException if there is an exception connecting to or
     *    communicating with the OCSP responder
     */
    public static byte[] getOCSPBytes(List<CertId> certIds, URI responderURI,
            List<Extension> extensions) throws IOException {
        OCSPRequest request = new OCSPRequest(certIds, extensions);
        byte[] bytes = request.encodeBytes();

        if (debug != null) {
            debug.println("connecting to OCSP service at: " + responderURI);
        }
        Event.report(Event.ReporterCategory.CRLCHECK, "event.ocsp.check",
                responderURI.toString());

        URL url;
        HttpURLConnection con = null;
        try {
            String encodedGetReq = responderURI.toString() + "/" +
                    URLEncoder.encode(Base64.getEncoder().encodeToString(bytes),
                            "UTF-8");

            if (encodedGetReq.length() <= 255) {
                url = new URL(encodedGetReq);
                con = (HttpURLConnection)url.openConnection();
                con.setDoOutput(true);
                con.setDoInput(true);
                con.setRequestMethod("GET");
            } else {
                url = responderURI.toURL();
                con = (HttpURLConnection)url.openConnection();
                con.setConnectTimeout(CONNECT_TIMEOUT);
                con.setReadTimeout(CONNECT_TIMEOUT);
                con.setDoOutput(true);
                con.setDoInput(true);
                con.setRequestMethod("POST");
                con.setRequestProperty
                    ("Content-type", "application/ocsp-request");
                con.setRequestProperty
                    ("Content-length", String.valueOf(bytes.length));
                OutputStream out = con.getOutputStream();
                out.write(bytes);
                out.flush();
            }

            // Check the response
            if (debug != null &&
                con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                debug.println("Received HTTP error: " + con.getResponseCode()
                    + " - " + con.getResponseMessage());
            }

            int contentLength = con.getContentLength();
            if (contentLength == -1) {
                contentLength = Integer.MAX_VALUE;
            }

            return IOUtils.readExactlyNBytes(con.getInputStream(),
                    contentLength);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    /**
     * Returns the URI of the OCSP Responder as specified in the
     * certificate's Authority Information Access extension, or null if
     * not specified.
     *
     * @param cert the certificate
     * @return the URI of the OCSP Responder, or null if not specified
     */
    // Called by com.sun.deploy.security.TrustDecider
    public static URI getResponderURI(X509Certificate cert) {
        try {
            return getResponderURI(X509CertImpl.toImpl(cert));
        } catch (CertificateException ce) {
            // treat this case as if the cert had no extension
            return null;
        }
    }

    static URI getResponderURI(X509CertImpl certImpl) {

        // Examine the certificate's AuthorityInfoAccess extension
        AuthorityInfoAccessExtension aia =
            certImpl.getAuthorityInfoAccessExtension();
        if (aia == null) {
            return null;
        }

        List<AccessDescription> descriptions = aia.getAccessDescriptions();
        for (AccessDescription description : descriptions) {
            if (description.getAccessMethod().equals(
                AccessDescription.Ad_OCSP_Id)) {

                GeneralName generalName = description.getAccessLocation();
                if (generalName.getType() == GeneralNameInterface.NAME_URI) {
                    URIName uri = (URIName) generalName.getName();
                    return uri.getURI();
                }
            }
        }
        return null;
    }

    /**
     * The Revocation Status of a certificate.
     */
    public static interface RevocationStatus {
        public enum CertStatus { GOOD, REVOKED, UNKNOWN };

        /**
         * Returns the revocation status.
         */
        CertStatus getCertStatus();
        /**
         * Returns the time when the certificate was revoked, or null
         * if it has not been revoked.
         */
        Date getRevocationTime();
        /**
         * Returns the reason the certificate was revoked, or null if it
         * has not been revoked.
         */
        CRLReason getRevocationReason();

        /**
         * Returns a Map of additional extensions.
         */
        Map<String, Extension> getSingleExtensions();
    }
}
