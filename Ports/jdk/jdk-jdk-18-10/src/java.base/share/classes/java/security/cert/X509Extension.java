/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.security.cert;

import java.util.Set;

/**
 * Interface for an X.509 extension.
 *
 * <p>The extensions defined for X.509 v3
 * {@link X509Certificate Certificates} and v2
 * {@link X509CRL CRLs} (Certificate Revocation
 * Lists) provide methods
 * for associating additional attributes with users or public keys,
 * for managing the certification hierarchy, and for managing CRL
 * distribution. The X.509 extensions format also allows communities
 * to define private extensions to carry information unique to those
 * communities.
 *
 * <p>Each extension in a certificate/CRL may be designated as
 * critical or non-critical.  A certificate/CRL-using system (an application
 * validating a certificate/CRL) must reject the certificate/CRL if it
 * encounters a critical extension it does not recognize.  A non-critical
 * extension may be ignored if it is not recognized.
 * <p>
 * The ASN.1 definition for this is:
 * <pre>
 * Extensions  ::=  SEQUENCE SIZE (1..MAX) OF Extension
 *
 * Extension  ::=  SEQUENCE  {
 *     extnId        OBJECT IDENTIFIER,
 *     critical      BOOLEAN DEFAULT FALSE,
 *     extnValue     OCTET STRING
 *                   -- contains a DER encoding of a value
 *                   -- of the type registered for use with
 *                   -- the extnId object identifier value
 * }
 * </pre>
 * Since not all extensions are known, the {@code getExtensionValue}
 * method returns the DER-encoded OCTET STRING of the
 * extension value (i.e., the {@code extnValue}). This can then
 * be handled by a <em>Class</em> that understands the extension.
 *
 * @author Hemma Prafullchandra
 * @since 1.2
 */

public interface X509Extension {

    /**
     * Check if there is a critical extension that is not supported.
     *
     * @return {@code true} if a critical extension is found that is
     * not supported, otherwise {@code false}.
     */
    public boolean hasUnsupportedCriticalExtension();

    /**
     * Gets a Set of the OID strings for the extension(s) marked
     * CRITICAL in the certificate/CRL managed by the object
     * implementing this interface.
     *
     * Here is sample code to get a Set of critical extensions from an
     * X509Certificate and print the OIDs:
     * <pre>{@code
     * X509Certificate cert = null;
     * try (InputStream inStrm = new FileInputStream("DER-encoded-Cert")) {
     *     CertificateFactory cf = CertificateFactory.getInstance("X.509");
     *     cert = (X509Certificate)cf.generateCertificate(inStrm);
     * }
     *
     * Set<String> critSet = cert.getCriticalExtensionOIDs();
     * if (critSet != null && !critSet.isEmpty()) {
     *     System.out.println("Set of critical extensions:");
     *     for (String oid : critSet) {
     *         System.out.println(oid);
     *     }
     * }
     * }</pre>
     * @return a Set (or an empty Set if none are marked critical) of
     * the extension OID strings for extensions that are marked critical.
     * If there are no extensions present at all, then this method returns
     * null.
     */
    public Set<String> getCriticalExtensionOIDs();

    /**
     * Gets a Set of the OID strings for the extension(s) marked
     * NON-CRITICAL in the certificate/CRL managed by the object
     * implementing this interface.
     *
     * Here is sample code to get a Set of non-critical extensions from an
     * X509CRL revoked certificate entry and print the OIDs:
     * <pre>{@code
     * CertificateFactory cf = null;
     * X509CRL crl = null;
     * try (InputStream inStrm = new FileInputStream("DER-encoded-CRL")) {
     *     cf = CertificateFactory.getInstance("X.509");
     *     crl = (X509CRL)cf.generateCRL(inStrm);
     * }
     *
     * byte[] certData = <DER-encoded certificate data>
     * ByteArrayInputStream bais = new ByteArrayInputStream(certData);
     * X509Certificate cert = (X509Certificate)cf.generateCertificate(bais);
     * X509CRLEntry badCert =
     *              crl.getRevokedCertificate(cert.getSerialNumber());
     *
     * if (badCert != null) {
     *     Set<String> nonCritSet = badCert.getNonCriticalExtensionOIDs();
     *     if (nonCritSet != null)
     *         for (String oid : nonCritSet) {
     *             System.out.println(oid);
     *         }
     * }
     * }</pre>
     *
     * @return a Set (or an empty Set if none are marked non-critical) of
     * the extension OID strings for extensions that are marked non-critical.
     * If there are no extensions present at all, then this method returns
     * null.
     */
    public Set<String> getNonCriticalExtensionOIDs();

    /**
     * Gets the DER-encoded OCTET string for the extension value
     * (<em>extnValue</em>) identified by the passed-in {@code oid}
     * String.
     * The {@code oid} string is
     * represented by a set of nonnegative whole numbers separated
     * by periods.
     *
     * <p>For example:<br>
     * <table class="striped">
     * <caption style="display:none">Examples of OIDs and extension names</caption>
     * <thead>
     * <tr>
     * <th scope="col">OID <em>(Object Identifier)</em></th>
     * <th scope="col">Extension Name</th></tr>
     * </thead>
     * <tbody style="text-align:left">
     * <tr><th scope="row">2.5.29.14</th>
     * <td>SubjectKeyIdentifier</td></tr>
     * <tr><th scope="row">2.5.29.15</th>
     * <td>KeyUsage</td></tr>
     * <tr><th scope="row">2.5.29.16</th>
     * <td>PrivateKeyUsage</td></tr>
     * <tr><th scope="row">2.5.29.17</th>
     * <td>SubjectAlternativeName</td></tr>
     * <tr><th scope="row">2.5.29.18</th>
     * <td>IssuerAlternativeName</td></tr>
     * <tr><th scope="row">2.5.29.19</th>
     * <td>BasicConstraints</td></tr>
     * <tr><th scope="row">2.5.29.30</th>
     * <td>NameConstraints</td></tr>
     * <tr><th scope="row">2.5.29.33</th>
     * <td>PolicyMappings</td></tr>
     * <tr><th scope="row">2.5.29.35</th>
     * <td>AuthorityKeyIdentifier</td></tr>
     * <tr><th scope="row">2.5.29.36</th>
     * <td>PolicyConstraints</td></tr>
     * </tbody>
     * </table>
     *
     * @param oid the Object Identifier value for the extension.
     * @return the DER-encoded octet string of the extension value or
     * null if it is not present.
     */
    public byte[] getExtensionValue(String oid);
}
