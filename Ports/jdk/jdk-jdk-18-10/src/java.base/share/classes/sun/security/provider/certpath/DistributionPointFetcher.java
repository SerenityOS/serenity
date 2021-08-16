/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.net.URI;
import java.security.*;
import java.security.cert.*;
import javax.security.auth.x500.X500Principal;
import java.util.*;

import sun.security.util.Debug;
import sun.security.util.Event;
import sun.security.validator.Validator;
import static sun.security.x509.PKIXExtensions.*;
import sun.security.x509.*;

/**
 * Class to obtain CRLs via the CRLDistributionPoints extension.
 * Note that the functionality of this class must be explicitly enabled
 * via a system property, see the USE_CRLDP variable below.
 *
 * This class uses the URICertStore class to fetch CRLs. The URICertStore
 * class also implements CRL caching: see the class description for more
 * information.
 *
 * @author Andreas Sterbenz
 * @author Sean Mullan
 * @since 1.4.2
 */
public class DistributionPointFetcher {

    private static final Debug debug = Debug.getInstance("certpath");

    private static final boolean[] ALL_REASONS =
        {true, true, true, true, true, true, true, true, true};

    /**
     * Private instantiation only.
     */
    private DistributionPointFetcher() {}

    /**
     * Return the X509CRLs matching this selector. The selector must be
     * an X509CRLSelector with certificateChecking set.
     */
    public static Collection<X509CRL> getCRLs(X509CRLSelector selector,
                                              boolean signFlag,
                                              PublicKey prevKey,
                                              X509Certificate prevCert,
                                              String provider,
                                              List<CertStore> certStores,
                                              boolean[] reasonsMask,
                                              Set<TrustAnchor> trustAnchors,
                                              Date validity,
                                              String variant,
                                              TrustAnchor anchor)
        throws CertStoreException
    {
        X509Certificate cert = selector.getCertificateChecking();
        if (cert == null) {
            return Collections.emptySet();
        }
        try {
            X509CertImpl certImpl = X509CertImpl.toImpl(cert);
            if (debug != null) {
                debug.println("DistributionPointFetcher.getCRLs: Checking "
                        + "CRLDPs for " + certImpl.getSubjectX500Principal());
            }
            CRLDistributionPointsExtension ext =
                certImpl.getCRLDistributionPointsExtension();
            if (ext == null) {
                if (debug != null) {
                    debug.println("No CRLDP ext");
                }
                return Collections.emptySet();
            }
            List<DistributionPoint> points =
                    ext.get(CRLDistributionPointsExtension.POINTS);
            Set<X509CRL> results = new HashSet<>();
            for (Iterator<DistributionPoint> t = points.iterator();
                 t.hasNext() && !Arrays.equals(reasonsMask, ALL_REASONS); ) {
                DistributionPoint point = t.next();
                Collection<X509CRL> crls = getCRLs(selector, certImpl,
                    point, reasonsMask, signFlag, prevKey, prevCert, provider,
                    certStores, trustAnchors, validity, variant, anchor);
                results.addAll(crls);
            }
            if (debug != null) {
                debug.println("Returning " + results.size() + " CRLs");
            }
            return results;
        } catch (CertificateException | IOException e) {
            return Collections.emptySet();
        }
    }

    /**
     * Download CRLs from the given distribution point, verify and return them.
     * See the top of the class for current limitations.
     *
     * @throws CertStoreException if there is an error retrieving the CRLs
     *         from one of the GeneralNames and no other CRLs are retrieved from
     *         the other GeneralNames. If more than one GeneralName throws an
     *         exception then the one from the last GeneralName is thrown.
     */
    private static Collection<X509CRL> getCRLs(X509CRLSelector selector,
        X509CertImpl certImpl, DistributionPoint point, boolean[] reasonsMask,
        boolean signFlag, PublicKey prevKey, X509Certificate prevCert,
        String provider, List<CertStore> certStores,
        Set<TrustAnchor> trustAnchors, Date validity, String variant,
        TrustAnchor anchor)
            throws CertStoreException {

        // check for full name
        GeneralNames fullName = point.getFullName();
        if (fullName == null) {
            // check for relative name
            RDN relativeName = point.getRelativeName();
            if (relativeName == null) {
                return Collections.emptySet();
            }
            try {
                GeneralNames crlIssuers = point.getCRLIssuer();
                if (crlIssuers == null) {
                    fullName = getFullNames
                        ((X500Name) certImpl.getIssuerDN(), relativeName);
                } else {
                    // should only be one CRL Issuer
                    if (crlIssuers.size() != 1) {
                        return Collections.emptySet();
                    } else {
                        fullName = getFullNames
                            ((X500Name) crlIssuers.get(0).getName(), relativeName);
                    }
                }
            } catch (IOException ioe) {
                return Collections.emptySet();
            }
        }
        Collection<X509CRL> possibleCRLs = new ArrayList<>();
        CertStoreException savedCSE = null;
        for (Iterator<GeneralName> t = fullName.iterator(); t.hasNext(); ) {
            try {
                GeneralName name = t.next();
                if (name.getType() == GeneralNameInterface.NAME_DIRECTORY) {
                    X500Name x500Name = (X500Name) name.getName();
                    possibleCRLs.addAll(
                        getCRLs(x500Name, certImpl.getIssuerX500Principal(),
                                certStores));
                } else if (name.getType() == GeneralNameInterface.NAME_URI) {
                    URIName uriName = (URIName)name.getName();
                    X509CRL crl = getCRL(uriName);
                    if (crl != null) {
                        possibleCRLs.add(crl);
                    }
                }
            } catch (CertStoreException cse) {
                savedCSE = cse;
            }
        }
        // only throw CertStoreException if no CRLs are retrieved
        if (possibleCRLs.isEmpty() && savedCSE != null) {
            throw savedCSE;
        }

        Collection<X509CRL> crls = new ArrayList<>(2);
        for (X509CRL crl : possibleCRLs) {
            try {
                // make sure issuer is not set
                // we check the issuer in verifyCRLs method
                selector.setIssuerNames(null);
                if (selector.match(crl) && verifyCRL(certImpl, point, crl,
                        reasonsMask, signFlag, prevKey, prevCert, provider,
                        trustAnchors, certStores, validity, variant, anchor)) {
                    crls.add(crl);
                }
            } catch (IOException | CRLException e) {
                // don't add the CRL
                if (debug != null) {
                    debug.println("Exception verifying CRL: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return crls;
    }

    /**
     * Download CRL from given URI.
     */
    private static X509CRL getCRL(URIName name) throws CertStoreException {
        URI uri = name.getURI();
        if (debug != null) {
            debug.println("Trying to fetch CRL from DP " + uri);
        }

        Event.report(Event.ReporterCategory.CRLCHECK, "event.crl.check", uri.toString());
        CertStore ucs = null;
        try {
            ucs = URICertStore.getInstance(new URICertStoreParameters(uri));
        } catch (InvalidAlgorithmParameterException |
                 NoSuchAlgorithmException e) {
            if (debug != null) {
                debug.println("Can't create URICertStore: " + e.getMessage());
            }
            return null;
        }

        Collection<? extends CRL> crls = ucs.getCRLs(null);
        if (crls.isEmpty()) {
            return null;
        } else {
            return (X509CRL) crls.iterator().next();
        }
    }

    /**
     * Fetch CRLs from certStores.
     *
     * @throws CertStoreException if there is an error retrieving the CRLs from
     *         one of the CertStores and no other CRLs are retrieved from
     *         the other CertStores. If more than one CertStore throws an
     *         exception then the one from the last CertStore is thrown.
     */
    private static Collection<X509CRL> getCRLs(X500Name name,
                                               X500Principal certIssuer,
                                               List<CertStore> certStores)
        throws CertStoreException
    {
        if (debug != null) {
            debug.println("Trying to fetch CRL from DP " + name);
        }
        X509CRLSelector xcs = new X509CRLSelector();
        xcs.addIssuer(name.asX500Principal());
        xcs.addIssuer(certIssuer);
        Collection<X509CRL> crls = new ArrayList<>();
        CertStoreException savedCSE = null;
        for (CertStore store : certStores) {
            try {
                for (CRL crl : store.getCRLs(xcs)) {
                    crls.add((X509CRL)crl);
                }
            } catch (CertStoreException cse) {
                if (debug != null) {
                    debug.println("Exception while retrieving " +
                        "CRLs: " + cse);
                    cse.printStackTrace();
                }
                savedCSE = new PKIX.CertStoreTypeException(store.getType(),cse);
            }
        }
        // only throw CertStoreException if no CRLs are retrieved
        if (crls.isEmpty() && savedCSE != null) {
            throw savedCSE;
        } else {
            return crls;
        }
    }

    /**
     * Verifies a CRL for the given certificate's Distribution Point to
     * ensure it is appropriate for checking the revocation status.
     *
     * @param certImpl the certificate whose revocation status is being checked
     * @param point one of the distribution points of the certificate
     * @param crl the CRL
     * @param reasonsMask the interim reasons mask
     * @param signFlag true if prevKey can be used to verify the CRL
     * @param prevKey the public key that verifies the certificate's signature
     * @param prevCert the certificate whose public key verifies
     *        {@code certImpl}'s signature
     * @param provider the Signature provider to use
     * @param trustAnchors a {@code Set} of {@code TrustAnchor}s
     * @param certStores a {@code List} of {@code CertStore}s to be used in
     *        finding certificates and CRLs
     * @param validity the time for which the validity of the CRL issuer's
     *        certification path should be determined
     * @return true if ok, false if not
     */
    static boolean verifyCRL(X509CertImpl certImpl, DistributionPoint point,
        X509CRL crl, boolean[] reasonsMask, boolean signFlag,
        PublicKey prevKey, X509Certificate prevCert, String provider,
        Set<TrustAnchor> trustAnchors, List<CertStore> certStores,
        Date validity, String variant, TrustAnchor anchor)
        throws CRLException, IOException {

        if (debug != null) {
            debug.println("DistributionPointFetcher.verifyCRL: " +
                "checking revocation status for" +
                "\n  SN: " + Debug.toHexString(certImpl.getSerialNumber()) +
                "\n  Subject: " + certImpl.getSubjectX500Principal() +
                "\n  Issuer: " + certImpl.getIssuerX500Principal());
        }

        boolean indirectCRL = false;
        X509CRLImpl crlImpl = X509CRLImpl.toImpl(crl);
        IssuingDistributionPointExtension idpExt =
            crlImpl.getIssuingDistributionPointExtension();
        X500Name certIssuer = (X500Name) certImpl.getIssuerDN();
        X500Name crlIssuer = (X500Name) crlImpl.getIssuerDN();

        // if crlIssuer is set, verify that it matches the issuer of the
        // CRL and the CRL contains an IDP extension with the indirectCRL
        // boolean asserted. Otherwise, verify that the CRL issuer matches the
        // certificate issuer.
        GeneralNames pointCrlIssuers = point.getCRLIssuer();
        X500Name pointCrlIssuer = null;
        if (pointCrlIssuers != null) {
            if (idpExt == null ||
                ((Boolean) idpExt.get
                    (IssuingDistributionPointExtension.INDIRECT_CRL)).equals
                        (Boolean.FALSE)) {
                return false;
            }
            boolean match = false;
            for (Iterator<GeneralName> t = pointCrlIssuers.iterator();
                 !match && t.hasNext(); ) {
                GeneralNameInterface name = t.next().getName();
                if (crlIssuer.equals(name) == true) {
                    pointCrlIssuer = (X500Name) name;
                    match = true;
                }
            }
            if (match == false) {
                return false;
            }

            // we accept the case that a CRL issuer provide status
            // information for itself.
            if (issues(certImpl, crlImpl, provider)) {
                // reset the public key used to verify the CRL's signature
                prevKey = certImpl.getPublicKey();
            } else {
                indirectCRL = true;
            }
        } else if (crlIssuer.equals(certIssuer) == false) {
            if (debug != null) {
                debug.println("crl issuer does not equal cert issuer.\n" +
                              "crl issuer: " + crlIssuer + "\n" +
                              "cert issuer: " + certIssuer);
            }
            return false;
        } else {
            // in case of self-issued indirect CRL issuer.
            KeyIdentifier certAKID = certImpl.getAuthKeyId();
            KeyIdentifier crlAKID = crlImpl.getAuthKeyId();

            if (certAKID == null || crlAKID == null) {
                // cannot recognize indirect CRL without AKID

                // we accept the case that a CRL issuer provide status
                // information for itself.
                if (issues(certImpl, crlImpl, provider)) {
                    // reset the public key used to verify the CRL's signature
                    prevKey = certImpl.getPublicKey();
                }
            } else if (!certAKID.equals(crlAKID)) {
                // we accept the case that a CRL issuer provide status
                // information for itself.
                if (issues(certImpl, crlImpl, provider)) {
                    // reset the public key used to verify the CRL's signature
                    prevKey = certImpl.getPublicKey();
                } else {
                    indirectCRL = true;
                }
            }
        }

        if (!indirectCRL && !signFlag) {
            // cert's key cannot be used to verify the CRL
            return false;
        }

        if (idpExt != null) {
            DistributionPointName idpPoint = (DistributionPointName)
                idpExt.get(IssuingDistributionPointExtension.POINT);
            if (idpPoint != null) {
                GeneralNames idpNames = idpPoint.getFullName();
                if (idpNames == null) {
                    RDN relativeName = idpPoint.getRelativeName();
                    if (relativeName == null) {
                        if (debug != null) {
                           debug.println("IDP must be relative or full DN");
                        }
                        return false;
                    }
                    if (debug != null) {
                        debug.println("IDP relativeName:" + relativeName);
                    }
                    idpNames = getFullNames(crlIssuer, relativeName);
                }
                // if the DP name is present in the IDP CRL extension and the
                // DP field is present in the DP, then verify that one of the
                // names in the IDP matches one of the names in the DP
                if (point.getFullName() != null ||
                    point.getRelativeName() != null) {
                    GeneralNames pointNames = point.getFullName();
                    if (pointNames == null) {
                        RDN relativeName = point.getRelativeName();
                        if (relativeName == null) {
                            if (debug != null) {
                                debug.println("DP must be relative or full DN");
                            }
                            return false;
                        }
                        if (debug != null) {
                            debug.println("DP relativeName:" + relativeName);
                        }
                        if (indirectCRL) {
                            if (pointCrlIssuers.size() != 1) {
                                // RFC 5280: there must be only 1 CRL issuer
                                // name when relativeName is present
                                if (debug != null) {
                                    debug.println("must only be one CRL " +
                                        "issuer when relative name present");
                                }
                                return false;
                            }
                            pointNames = getFullNames
                                (pointCrlIssuer, relativeName);
                        } else {
                            pointNames = getFullNames(certIssuer, relativeName);
                        }
                    }
                    boolean match = false;
                    for (Iterator<GeneralName> i = idpNames.iterator();
                         !match && i.hasNext(); ) {
                        GeneralNameInterface idpName = i.next().getName();
                        if (debug != null) {
                            debug.println("idpName: " + idpName);
                        }
                        for (Iterator<GeneralName> p = pointNames.iterator();
                             !match && p.hasNext(); ) {
                            GeneralNameInterface pointName = p.next().getName();
                            if (debug != null) {
                                debug.println("pointName: " + pointName);
                            }
                            match = idpName.equals(pointName);
                        }
                    }
                    if (!match) {
                        if (debug != null) {
                            debug.println("IDP name does not match DP name");
                        }
                        return false;
                    }
                // if the DP name is present in the IDP CRL extension and the
                // DP field is absent from the DP, then verify that one of the
                // names in the IDP matches one of the names in the crlIssuer
                // field of the DP
                } else {
                    // verify that one of the names in the IDP matches one of
                    // the names in the cRLIssuer of the cert's DP
                    boolean match = false;
                    for (Iterator<GeneralName> t = pointCrlIssuers.iterator();
                         !match && t.hasNext(); ) {
                        GeneralNameInterface crlIssuerName = t.next().getName();
                        for (Iterator<GeneralName> i = idpNames.iterator();
                             !match && i.hasNext(); ) {
                            GeneralNameInterface idpName = i.next().getName();
                            match = crlIssuerName.equals(idpName);
                        }
                    }
                    if (!match) {
                        return false;
                    }
                }
            }

            // if the onlyContainsUserCerts boolean is asserted, verify that the
            // cert is not a CA cert
            Boolean b = (Boolean)
                idpExt.get(IssuingDistributionPointExtension.ONLY_USER_CERTS);
            if (b.equals(Boolean.TRUE) && certImpl.getBasicConstraints() != -1) {
                if (debug != null) {
                    debug.println("cert must be a EE cert");
                }
                return false;
            }

            // if the onlyContainsCACerts boolean is asserted, verify that the
            // cert is a CA cert
            b = (Boolean)
                idpExt.get(IssuingDistributionPointExtension.ONLY_CA_CERTS);
            if (b.equals(Boolean.TRUE) && certImpl.getBasicConstraints() == -1) {
                if (debug != null) {
                    debug.println("cert must be a CA cert");
                }
                return false;
            }

            // verify that the onlyContainsAttributeCerts boolean is not
            // asserted
            b = (Boolean) idpExt.get
                (IssuingDistributionPointExtension.ONLY_ATTRIBUTE_CERTS);
            if (b.equals(Boolean.TRUE)) {
                if (debug != null) {
                    debug.println("cert must not be an AA cert");
                }
                return false;
            }
        }

        // compute interim reasons mask
        boolean[] interimReasonsMask = new boolean[9];
        ReasonFlags reasons = null;
        if (idpExt != null) {
            reasons = (ReasonFlags)
                idpExt.get(IssuingDistributionPointExtension.REASONS);
        }

        boolean[] pointReasonFlags = point.getReasonFlags();
        if (reasons != null) {
            if (pointReasonFlags != null) {
                // set interim reasons mask to the intersection of
                // reasons in the DP and onlySomeReasons in the IDP
                boolean[] idpReasonFlags = reasons.getFlags();
                for (int i = 0; i < interimReasonsMask.length; i++) {
                    interimReasonsMask[i] =
                        (i < idpReasonFlags.length && idpReasonFlags[i]) &&
                        (i < pointReasonFlags.length && pointReasonFlags[i]);
                }
            } else {
                // set interim reasons mask to the value of
                // onlySomeReasons in the IDP (and clone it since we may
                // modify it)
                interimReasonsMask = reasons.getFlags().clone();
            }
        } else if (idpExt == null || reasons == null) {
            if (pointReasonFlags != null) {
                // set interim reasons mask to the value of DP reasons
                interimReasonsMask = pointReasonFlags.clone();
            } else {
                // set interim reasons mask to the special value all-reasons
                Arrays.fill(interimReasonsMask, true);
            }
        }

        // verify that interim reasons mask includes one or more reasons
        // not included in the reasons mask
        boolean oneOrMore = false;
        for (int i = 0; i < interimReasonsMask.length && !oneOrMore; i++) {
            if (interimReasonsMask[i] &&
                    !(i < reasonsMask.length && reasonsMask[i]))
            {
                oneOrMore = true;
            }
        }
        if (!oneOrMore) {
            return false;
        }

        // Obtain and validate the certification path for the complete
        // CRL issuer (if indirect CRL). If a key usage extension is present
        // in the CRL issuer's certificate, verify that the cRLSign bit is set.
        if (indirectCRL) {
            X509CertSelector certSel = new X509CertSelector();
            certSel.setSubject(crlIssuer.asX500Principal());
            boolean[] crlSign = {false,false,false,false,false,false,true};
            certSel.setKeyUsage(crlSign);

            // Currently by default, forward builder does not enable
            // subject/authority key identifier identifying for target
            // certificate, instead, it only compares the CRL issuer and
            // the target certificate subject. If the certificate of the
            // delegated CRL issuer is a self-issued certificate, the
            // builder is unable to find the proper CRL issuer by issuer
            // name only, there is a potential dead loop on finding the
            // proper issuer. It is of great help to narrow the target
            // scope down to aware of authority key identifiers in the
            // selector, for the purposes of breaking the dead loop.
            AuthorityKeyIdentifierExtension akidext =
                                            crlImpl.getAuthKeyIdExtension();
            if (akidext != null) {
                byte[] kid = akidext.getEncodedKeyIdentifier();
                if (kid != null) {
                    certSel.setSubjectKeyIdentifier(kid);
                }

                SerialNumber asn = (SerialNumber)akidext.get(
                        AuthorityKeyIdentifierExtension.SERIAL_NUMBER);
                if (asn != null) {
                    certSel.setSerialNumber(asn.getNumber());
                }
                // the subject criterion will be set by builder automatically.
            }

            // By now, we have validated the previous certificate, so we can
            // trust it during the validation of the CRL issuer.
            // In addition to the performance improvement, another benefit is to
            // break the dead loop while looking for the issuer back and forth
            // between the delegated self-issued certificate and its issuer.
            Set<TrustAnchor> newTrustAnchors = new HashSet<>(trustAnchors);

            if (prevKey != null) {
                // Add the previous certificate as a trust anchor.
                // If prevCert is not null, we want to construct a TrustAnchor
                // using the cert object because when the certpath for the CRL
                // is built later, the CertSelector will make comparisons with
                // the TrustAnchor's trustedCert member rather than its pubKey.
                TrustAnchor temporary;
                if (prevCert != null) {
                    temporary = new TrustAnchor(prevCert, null);
                } else {
                    X500Principal principal = certImpl.getIssuerX500Principal();
                    temporary = new TrustAnchor(principal, prevKey, null);
                }
                newTrustAnchors.add(temporary);
            }

            PKIXBuilderParameters params = null;
            try {
                params = new PKIXBuilderParameters(newTrustAnchors, certSel);
            } catch (InvalidAlgorithmParameterException iape) {
                throw new CRLException(iape);
            }
            params.setCertStores(certStores);
            params.setSigProvider(provider);
            params.setDate(validity);
            try {
                CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");
                PKIXCertPathBuilderResult result =
                    (PKIXCertPathBuilderResult) builder.build(params);
                prevKey = result.getPublicKey();
            } catch (GeneralSecurityException e) {
                throw new CRLException(e);
            }
        }

        // check the crl signature algorithm
        try {
            AlgorithmChecker.check(prevKey, crl, variant, anchor);
        } catch (CertPathValidatorException cpve) {
            if (debug != null) {
                debug.println("CRL signature algorithm check failed: " + cpve);
            }
            return false;
        }

        // validate the signature on the CRL
        try {
            crl.verify(prevKey, provider);
        } catch (GeneralSecurityException e) {
            if (debug != null) {
                debug.println("CRL signature failed to verify");
            }
            return false;
        }

        // reject CRL if any unresolved critical extensions remain in the CRL.
        Set<String> unresCritExts = crl.getCriticalExtensionOIDs();
        // remove any that we have processed
        if (unresCritExts != null) {
            unresCritExts.remove(IssuingDistributionPoint_Id.toString());
            if (!unresCritExts.isEmpty()) {
                if (debug != null) {
                    debug.println("Unrecognized critical extension(s) in CRL: "
                        + unresCritExts);
                    for (String ext : unresCritExts) {
                        debug.println(ext);
                    }
                }
                return false;
            }
        }

        // update reasonsMask
        for (int i = 0; i < reasonsMask.length; i++) {
            reasonsMask[i] = reasonsMask[i] ||
                    (i < interimReasonsMask.length && interimReasonsMask[i]);
        }

        return true;
    }

    /**
     * Append relative name to the issuer name and return a new
     * GeneralNames object.
     */
    private static GeneralNames getFullNames(X500Name issuer, RDN rdn)
        throws IOException
    {
        List<RDN> rdns = new ArrayList<>(issuer.rdns());
        rdns.add(rdn);
        X500Name fullName = new X500Name(rdns.toArray(new RDN[0]));
        GeneralNames fullNames = new GeneralNames();
        fullNames.add(new GeneralName(fullName));
        return fullNames;
    }

    /**
     * Verifies whether a CRL is issued by a certain certificate
     *
     * @param cert the certificate
     * @param crl the CRL to be verified
     * @param provider the name of the signature provider
     */
    private static boolean issues(X509CertImpl cert, X509CRLImpl crl,
                                  String provider) throws IOException
    {
        boolean matched = false;

        AdaptableX509CertSelector issuerSelector =
                                    new AdaptableX509CertSelector();

        // check certificate's key usage
        boolean[] usages = cert.getKeyUsage();
        if (usages != null) {
            usages[6] = true;       // cRLSign
            issuerSelector.setKeyUsage(usages);
        }

        // check certificate's subject
        X500Principal crlIssuer = crl.getIssuerX500Principal();
        issuerSelector.setSubject(crlIssuer);

        /*
         * Facilitate certification path construction with authority
         * key identifier and subject key identifier.
         *
         * In practice, conforming CAs MUST use the key identifier method,
         * and MUST include authority key identifier extension in all CRLs
         * issued. [section 5.2.1, RFC 5280]
         */
        AuthorityKeyIdentifierExtension crlAKID = crl.getAuthKeyIdExtension();
        issuerSelector.setSkiAndSerialNumber(crlAKID);

        matched = issuerSelector.match(cert);

        // if AKID is unreliable, verify the CRL signature with the cert
        if (matched && (crlAKID == null ||
                cert.getAuthorityKeyIdentifierExtension() == null)) {
            try {
                crl.verify(cert.getPublicKey(), provider);
                matched = true;
            } catch (GeneralSecurityException e) {
                matched = false;
            }
        }

        return matched;
    }
}
