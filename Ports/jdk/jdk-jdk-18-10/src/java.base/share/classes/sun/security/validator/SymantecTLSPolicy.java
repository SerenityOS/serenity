/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package sun.security.validator;

import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import sun.security.util.Debug;
import sun.security.x509.X509CertImpl;

/**
 * This class checks if Symantec issued TLS Server certificates should be
 * restricted.
 */
final class SymantecTLSPolicy {

    private static final Debug debug = Debug.getInstance("certpath");

    // SHA-256 certificate fingerprints of distrusted roots
    private static final Set<String> FINGERPRINTS = Set.of(
        // cacerts alias: geotrustglobalca
        // DN: CN=GeoTrust Global CA, O=GeoTrust Inc., C=US
        "FF856A2D251DCD88D36656F450126798CFABAADE40799C722DE4D2B5DB36A73A",
        // cacerts alias: geotrustprimaryca
        // DN: CN=GeoTrust Primary Certification Authority,
        //     O=GeoTrust Inc., C=US
        "37D51006C512EAAB626421F1EC8C92013FC5F82AE98EE533EB4619B8DEB4D06C",
        // cacerts alias: geotrustprimarycag2
        // DN: CN=GeoTrust Primary Certification Authority - G2,
        //     OU=(c) 2007 GeoTrust Inc. - For authorized use only,
        //     O=GeoTrust Inc., C=US
        "5EDB7AC43B82A06A8761E8D7BE4979EBF2611F7DD79BF91C1C6B566A219ED766",
        // cacerts alias: geotrustprimarycag3
        // DN: CN=GeoTrust Primary Certification Authority - G3,
        //     OU=(c) 2008 GeoTrust Inc. - For authorized use only,
        //     O=GeoTrust Inc., C=US
        "B478B812250DF878635C2AA7EC7D155EAA625EE82916E2CD294361886CD1FBD4",
        // cacerts alias: geotrustuniversalca
        // DN: CN=GeoTrust Universal CA, O=GeoTrust Inc., C=US
        "A0459B9F63B22559F5FA5D4C6DB3F9F72FF19342033578F073BF1D1B46CBB912",
        // cacerts alias: thawteprimaryrootca
        // DN: CN=thawte Primary Root CA,
        //     OU="(c) 2006 thawte, Inc. - For authorized use only",
        //     OU=Certification Services Division, O="thawte, Inc.", C=US
        "8D722F81A9C113C0791DF136A2966DB26C950A971DB46B4199F4EA54B78BFB9F",
        // cacerts alias: thawteprimaryrootcag2
        // DN: CN=thawte Primary Root CA - G2,
        //     OU="(c) 2007 thawte, Inc. - For authorized use only",
        //     O="thawte, Inc.", C=US
        "A4310D50AF18A6447190372A86AFAF8B951FFB431D837F1E5688B45971ED1557",
        // cacerts alias: thawteprimaryrootcag3
        // DN: CN=thawte Primary Root CA - G3,
        //     OU="(c) 2008 thawte, Inc. - For authorized use only",
        //     OU=Certification Services Division, O="thawte, Inc.", C=US
        "4B03F45807AD70F21BFC2CAE71C9FDE4604C064CF5FFB686BAE5DBAAD7FDD34C",
        // cacerts alias: thawtepremiumserverca
        // DN: EMAILADDRESS=premium-server@thawte.com,
        //     CN=Thawte Premium Server CA, OU=Certification Services Division,
        //     O=Thawte Consulting cc, L=Cape Town, ST=Western Cape, C=ZA
        "3F9F27D583204B9E09C8A3D2066C4B57D3A2479C3693650880505698105DBCE9",
        // cacerts alias: verisignclass2g2ca
        // DN: OU=VeriSign Trust Network,
        //     OU="(c) 1998 VeriSign, Inc. - For authorized use only",
        //     OU=Class 2 Public Primary Certification Authority - G2,
        //     O="VeriSign, Inc.", C=US
        "3A43E220FE7F3EA9653D1E21742EAC2B75C20FD8980305BC502CAF8C2D9B41A1",
        // cacerts alias: verisignclass3ca
        // DN: OU=Class 3 Public Primary Certification Authority,
        //     O="VeriSign, Inc.", C=US
        "A4B6B3996FC2F306B3FD8681BD63413D8C5009CC4FA329C2CCF0E2FA1B140305",
        // cacerts alias: verisignclass3g2ca
        // DN: OU=VeriSign Trust Network,
        //     OU="(c) 1998 VeriSign, Inc. - For authorized use only",
        //     OU=Class 3 Public Primary Certification Authority - G2,
        //     O="VeriSign, Inc.", C=US
        "83CE3C1229688A593D485F81973C0F9195431EDA37CC5E36430E79C7A888638B",
        // cacerts alias: verisignclass3g3ca
        // DN: CN=VeriSign Class 3 Public Primary Certification Authority - G3,
        //     OU="(c) 1999 VeriSign, Inc. - For authorized use only",
        //     OU=VeriSign Trust Network, O="VeriSign, Inc.", C=US
        "EB04CF5EB1F39AFA762F2BB120F296CBA520C1B97DB1589565B81CB9A17B7244",
        // cacerts alias: verisignclass3g4ca
        // DN: CN=VeriSign Class 3 Public Primary Certification Authority - G4,
        //     OU="(c) 2007 VeriSign, Inc. - For authorized use only",
        //     OU=VeriSign Trust Network, O="VeriSign, Inc.", C=US
        "69DDD7EA90BB57C93E135DC85EA6FCD5480B603239BDC454FC758B2A26CF7F79",
        // cacerts alias: verisignclass3g5ca
        // DN: CN=VeriSign Class 3 Public Primary Certification Authority - G5,
        //     OU="(c) 2006 VeriSign, Inc. - For authorized use only",
        //     OU=VeriSign Trust Network, O="VeriSign, Inc.", C=US
        "9ACFAB7E43C8D880D06B262A94DEEEE4B4659989C3D0CAF19BAF6405E41AB7DF",
        // cacerts alias: verisignuniversalrootca
        // DN: CN=VeriSign Universal Root Certification Authority,
        //     OU="(c) 2008 VeriSign, Inc. - For authorized use only",
        //     OU=VeriSign Trust Network, O="VeriSign, Inc.", C=US
        "2399561127A57125DE8CEFEA610DDF2FA078B5C8067F4E828290BFB860E84B3C"
    );

    private static final LocalDate DECEMBER_31_2019 =
        LocalDate.of(2019, Month.DECEMBER, 31);
    // SHA-256 certificate fingerprints of subCAs with later distrust dates
    private static final Map<String, LocalDate> EXEMPT_SUBCAS = Map.of(
        // Subject DN: C=US, O=Apple Inc., OU=Certification Authority,
        //             CN=Apple IST CA 2 - G1
        // Issuer DN: CN=GeoTrust Global CA, O=GeoTrust Inc., C=US
        "AC2B922ECFD5E01711772FEA8ED372DE9D1E2245FCE3F57A9CDBEC77296A424B",
        DECEMBER_31_2019,
        // Subject DN: C=US, O=Apple Inc., OU=Certification Authority,
        //             CN=Apple IST CA 8 - G1
        // Issuer DN: CN=GeoTrust Primary Certification Authority - G2,
        //            OU=(c) 2007 GeoTrust Inc. - For authorized use only,
        //            O=GeoTrust Inc., C=US
        "A4FE7C7F15155F3F0AEF7AAA83CF6E06DEB97CA3F909DF920AC1490882D488ED",
        DECEMBER_31_2019
    );

    // Any TLS Server certificate that is anchored by one of the Symantec
    // roots above and is issued after this date will be distrusted.
    private static final LocalDate APRIL_16_2019 =
        LocalDate.of(2019, Month.APRIL, 16);

    /**
     * This method assumes the eeCert is a TLS Server Cert and chains back to
     * the anchor.
     *
     * @param chain the end-entity's certificate chain. The end entity cert
     *              is at index 0, the trust anchor at index n-1.
     * @throws ValidatorException if the certificate is distrusted
     */
    static void checkDistrust(X509Certificate[] chain)
                              throws ValidatorException {
        X509Certificate anchor = chain[chain.length-1];
        String fp = fingerprint(anchor);
        if (fp == null) {
            throw new ValidatorException("Cannot generate fingerprint for "
                + "trust anchor of TLS server certificate");
        }
        if (FINGERPRINTS.contains(fp)) {
            Date notBefore = chain[0].getNotBefore();
            LocalDate ldNotBefore = LocalDate.ofInstant(notBefore.toInstant(),
                                                        ZoneOffset.UTC);
            // check if chain goes through one of the subCAs
            if (chain.length > 2) {
                X509Certificate subCA = chain[chain.length-2];
                fp = fingerprint(subCA);
                if (fp == null) {
                    throw new ValidatorException("Cannot generate fingerprint "
                        + "for intermediate CA of TLS server certificate");
                }
                LocalDate distrustDate = EXEMPT_SUBCAS.get(fp);
                if (distrustDate != null) {
                    // reject if certificate is issued after specified date
                    checkNotBefore(ldNotBefore, distrustDate, anchor);
                    return; // success
                }
            }
            // reject if certificate is issued after April 16, 2019
            checkNotBefore(ldNotBefore, APRIL_16_2019, anchor);
        }
    }

    private static String fingerprint(X509Certificate cert) {
        return X509CertImpl.getFingerprint("SHA-256", cert, debug);
    }

    private static void checkNotBefore(LocalDate notBeforeDate,
            LocalDate distrustDate, X509Certificate anchor)
            throws ValidatorException {
        if (notBeforeDate.isAfter(distrustDate)) {
            throw new ValidatorException
                ("TLS Server certificate issued after " + distrustDate +
                 " and anchored by a distrusted legacy Symantec root CA: "
                 + anchor.getSubjectX500Principal(),
                 ValidatorException.T_UNTRUSTED_CERT, anchor);
        }
    }

    private SymantecTLSPolicy() {}
}
