/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.cert.*;
import java.util.*;

import sun.security.action.GetBooleanAction;
import sun.security.provider.certpath.PKIX.BuilderParams;
import sun.security.util.Debug;
import sun.security.x509.GeneralNames;
import sun.security.x509.GeneralNameInterface;
import sun.security.x509.GeneralSubtrees;
import sun.security.x509.NameConstraintsExtension;
import sun.security.x509.SubjectAlternativeNameExtension;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;

/**
 * Abstract class representing a builder, which is able to retrieve
 * matching certificates and is able to verify a particular certificate.
 *
 * @since       1.4
 * @author      Sean Mullan
 * @author      Yassir Elley
 */

public abstract class Builder {

    private static final Debug debug = Debug.getInstance("certpath");
    private Set<String> matchingPolicies;
    final BuilderParams buildParams;
    final X509CertSelector targetCertConstraints;

    /**
     * Flag indicating whether support for the caIssuers field of the
     * Authority Information Access extension shall be enabled. Currently
     * disabled by default for compatibility reasons.
     */
    static final boolean USE_AIA = GetBooleanAction
            .privilegedGetProperty("com.sun.security.enableAIAcaIssuers");

    /**
     * Initialize the builder with the input parameters.
     *
     * @param buildParams the parameter set used to build a certification path
     */
    Builder(BuilderParams buildParams) {
        this.buildParams = buildParams;
        this.targetCertConstraints =
            (X509CertSelector)buildParams.targetCertConstraints();
    }

    /**
     * Retrieves certificates from the list of certStores using the buildParams
     * and the currentState as a filter
     *
     * @param currentState the current State
     * @param certStores list of CertStores
     */
    abstract Collection<X509Certificate> getMatchingCerts
        (State currentState, List<CertStore> certStores)
        throws CertStoreException, CertificateException, IOException;

    /**
     * Verifies the cert against the currentState, using the certPathList
     * generated thus far to help with loop detection
     *
     * @param cert the certificate to be verified
     * @param currentState the current state against which the cert is verified
     * @param certPathList the certPathList generated thus far
     */
    abstract void verifyCert(X509Certificate cert, State currentState,
                             List<X509Certificate> certPathList)
        throws GeneralSecurityException;

    /**
     * Verifies whether the input certificate completes the path.
     * When building in the forward direction, a trust anchor will
     * complete the path.
     *
     * @param cert the certificate to test
     * @return a boolean value indicating whether the cert completes the path.
     */
    abstract boolean isPathCompleted(X509Certificate cert);

    /**
     * Adds the certificate to the certPathList
     *
     * @param cert the certificate to be added
     * @param certPathList the certification path list
     */
    abstract void addCertToPath(X509Certificate cert,
                                LinkedList<X509Certificate> certPathList);

    /**
     * Removes final certificate from the certPathList
     *
     * @param certPathList the certification path list
     */
    abstract void removeFinalCertFromPath
        (LinkedList<X509Certificate> certPathList);

    /**
     * get distance of one GeneralName from another
     *
     * @param base GeneralName at base of subtree
     * @param test GeneralName to be tested against base
     * @param incomparable the value to return if the names are
     *  incomparable
     * @return distance of test name from base, where 0
     *         means exact match, 1 means test is an immediate
     *         child of base, 2 means test is a grandchild, etc.
     *         -1 means test is a parent of base, -2 means test
     *         is a grandparent, etc.
     */
    static int distance(GeneralNameInterface base,
                        GeneralNameInterface test, int incomparable)
    {
        switch (base.constrains(test)) {
        case GeneralNameInterface.NAME_DIFF_TYPE:
            if (debug != null) {
                debug.println("Builder.distance(): Names are different types");
            }
            return incomparable;
        case GeneralNameInterface.NAME_SAME_TYPE:
            if (debug != null) {
                debug.println("Builder.distance(): Names are same type but " +
                    "in different subtrees");
            }
            return incomparable;
        case GeneralNameInterface.NAME_MATCH:
            return 0;
        case GeneralNameInterface.NAME_WIDENS:
            break;
        case GeneralNameInterface.NAME_NARROWS:
            break;
        default: // should never occur
            return incomparable;
        }

        /* names are in same subtree */
        return test.subtreeDepth() - base.subtreeDepth();
    }

    /**
     * get hop distance of one GeneralName from another in links where
     * the names need not have an ancestor/descendant relationship.
     * For example, the hop distance from ou=D,ou=C,o=B,c=US to
     * ou=F,ou=E,ou=C,o=B,c=US is 3: D->C, C->E, E->F.  The hop distance
     * from ou=C,o=B,c=US to ou=D,ou=C,o=B,c=US is -1: C->D
     *
     * @param base GeneralName
     * @param test GeneralName to be tested against base
     * @param incomparable the value to return if the names are
     *  incomparable
     * @return distance of test name from base measured in hops in the
     *         namespace hierarchy, where 0 means exact match.  Result
     *         is positive if path is some number of up hops followed by
     *         some number of down hops; result is negative if path is
     *         some number of down hops.
     */
    static int hops(GeneralNameInterface base, GeneralNameInterface test,
                    int incomparable)
    {
        int baseRtest = base.constrains(test);
        switch (baseRtest) {
        case GeneralNameInterface.NAME_DIFF_TYPE:
            if (debug != null) {
                debug.println("Builder.hops(): Names are different types");
            }
            return incomparable;
        case GeneralNameInterface.NAME_SAME_TYPE:
            /* base and test are in different subtrees */
            break;
        case GeneralNameInterface.NAME_MATCH:
            /* base matches test */
            return 0;
        case GeneralNameInterface.NAME_WIDENS:
            /* base is ancestor of test */
            return (test.subtreeDepth()-base.subtreeDepth());
        case GeneralNameInterface.NAME_NARROWS:
            /* base is descendant of test */
            return (test.subtreeDepth()-base.subtreeDepth());
        default: // should never occur
            return incomparable;
        }

        /* names are in different subtrees */
        if (base.getType() != GeneralNameInterface.NAME_DIRECTORY) {
            if (debug != null) {
                debug.println("Builder.hops(): hopDistance not implemented " +
                    "for this name type");
            }
            return incomparable;
        }
        X500Name baseName = (X500Name)base;
        X500Name testName = (X500Name)test;
        X500Name commonName = baseName.commonAncestor(testName);
        if (commonName == null) {
            if (debug != null) {
                debug.println("Builder.hops(): Names are in different " +
                    "namespaces");
            }
            return incomparable;
        } else {
            int commonDistance = commonName.subtreeDepth();
            int baseDistance = baseName.subtreeDepth();
            int testDistance = testName.subtreeDepth();
            return (baseDistance + testDistance - (2 * commonDistance));
        }
    }

    /**
     * Determine how close a given certificate gets you toward
     * a given target.
     *
     * @param constraints Current NameConstraints; if null,
     *        then caller must verify NameConstraints
     *        independently, realizing that this certificate
     *        may not actually lead to the target at all.
     * @param cert Candidate certificate for chain
     * @param target GeneralNameInterface name of target
     * @return distance from this certificate to target:
     * <ul>
     * <li>-1 means certificate could be CA for target, but
     *     there are no NameConstraints limiting how close
     * <li> 0 means certificate subject or subjectAltName
     *      matches target
     * <li> 1 means certificate is permitted to be CA for
     *      target.
     * <li> 2 means certificate is permitted to be CA for
     *      parent of target.
     * <li>&gt;0 in general, means certificate is permitted
     *     to be a CA for this distance higher in the naming
     *     hierarchy than the target, plus 1.
     * </ul>
     * <p>Note that the subject and/or subjectAltName of the
     * candidate cert does not have to be an ancestor of the
     * target in order to be a CA that can issue a certificate to
     * the target. In these cases, the target distance is calculated
     * by inspecting the NameConstraints extension in the candidate
     * certificate. For example, suppose the target is an X.500 DN with
     * a value of "CN=mullan,OU=ireland,O=sun,C=us" and the
     * NameConstraints extension in the candidate certificate
     * includes a permitted component of "O=sun,C=us", which implies
     * that the candidate certificate is allowed to issue certs in
     * the "O=sun,C=us" namespace. The target distance is 3
     * ((distance of permitted NC from target) + 1).
     * The (+1) is added to distinguish the result from the case
     * which returns (0).
     * @throws IOException if certificate does not get closer
     */
    static int targetDistance(NameConstraintsExtension constraints,
                              X509Certificate cert, GeneralNameInterface target)
            throws IOException
    {
        /* ensure that certificate satisfies existing name constraints */
        if (constraints != null && !constraints.verify(cert)) {
            throw new IOException("certificate does not satisfy existing name "
                + "constraints");
        }

        X509CertImpl certImpl;
        try {
            certImpl = X509CertImpl.toImpl(cert);
        } catch (CertificateException e) {
            throw new IOException("Invalid certificate", e);
        }
        /* see if certificate subject matches target */
        X500Name subject = X500Name.asX500Name(certImpl.getSubjectX500Principal());
        if (subject.equals(target)) {
            /* match! */
            return 0;
        }

        SubjectAlternativeNameExtension altNameExt =
            certImpl.getSubjectAlternativeNameExtension();
        if (altNameExt != null) {
            GeneralNames altNames = altNameExt.get(
                    SubjectAlternativeNameExtension.SUBJECT_NAME);
            /* see if any alternative name matches target */
            if (altNames != null) {
                for (int j = 0, n = altNames.size(); j < n; j++) {
                    GeneralNameInterface altName = altNames.get(j).getName();
                    if (altName.equals(target)) {
                        return 0;
                    }
                }
            }
        }


        /* no exact match; see if certificate can get us to target */

        /* first, get NameConstraints out of certificate */
        NameConstraintsExtension ncExt = certImpl.getNameConstraintsExtension();
        if (ncExt == null) {
            return -1;
        }

        /* merge certificate's NameConstraints with current NameConstraints */
        if (constraints != null) {
            constraints.merge(ncExt);
        } else {
            // Make sure we do a clone here, because we're probably
            // going to modify this object later and we don't want to
            // be sharing it with a Certificate object!
            constraints = (NameConstraintsExtension) ncExt.clone();
        }

        if (debug != null) {
            debug.println("Builder.targetDistance() merged constraints: "
                + String.valueOf(constraints));
        }
        /* reduce permitted by excluded */
        GeneralSubtrees permitted =
                constraints.get(NameConstraintsExtension.PERMITTED_SUBTREES);
        GeneralSubtrees excluded =
                constraints.get(NameConstraintsExtension.EXCLUDED_SUBTREES);
        if (permitted != null) {
            permitted.reduce(excluded);
        }
        if (debug != null) {
            debug.println("Builder.targetDistance() reduced constraints: "
                + permitted);
        }
        /* see if new merged constraints allow target */
        if (!constraints.verify(target)) {
            throw new IOException("New certificate not allowed to sign "
                + "certificate for target");
        }
        /* find distance to target, if any, in permitted */
        if (permitted == null) {
            /* certificate is unconstrained; could sign for anything */
            return -1;
        }
        for (int i = 0, n = permitted.size(); i < n; i++) {
            GeneralNameInterface perName = permitted.get(i).getName().getName();
            int distance = distance(perName, target, -1);
            if (distance >= 0) {
                return (distance + 1);
            }
        }
        /* no matching type in permitted; cert holder could certify target */
        return -1;
    }

    /**
     * This method can be used as an optimization to filter out
     * certificates that do not have policies which are valid.
     * It returns the set of policies (String OIDs) that should exist in
     * the certificate policies extension of the certificate that is
     * needed by the builder. The logic applied is as follows:
     * <p>
     *   1) If some initial policies have been set *and* policy mappings are
     *   inhibited, then acceptable certificates are those that include
     *   the ANY_POLICY OID or with policies that intersect with the
     *   initial policies.
     *   2) If no initial policies have been set *or* policy mappings are
     *   not inhibited then we don't have much to work with. All we know is
     *   that a certificate must have *some* policy because if it didn't
     *   have any policy then the policy tree would become null (and validation
     *   would fail).
     *
     * @return the Set of policies any of which must exist in a
     * cert's certificate policies extension in order for a cert to be selected.
     */
    Set<String> getMatchingPolicies() {
        if (matchingPolicies != null) {
            Set<String> initialPolicies = buildParams.initialPolicies();
            if ((!initialPolicies.isEmpty()) &&
                (!initialPolicies.contains(PolicyChecker.ANY_POLICY)) &&
                (buildParams.policyMappingInhibited()))
            {
                matchingPolicies = new HashSet<>(initialPolicies);
                matchingPolicies.add(PolicyChecker.ANY_POLICY);
            } else {
                // we just return an empty set to make sure that there is
                // at least a certificate policies extension in the cert
                matchingPolicies = Collections.<String>emptySet();
            }
        }
        return matchingPolicies;
    }

    /**
     * Search the specified CertStores and add all certificates matching
     * selector to resultCerts. Self-signed certs are not useful here
     * and therefore ignored.
     *
     * If the targetCert criterion of the selector is set, only that cert
     * is examined and the CertStores are not searched.
     *
     * If checkAll is true, all CertStores are searched for matching certs.
     * If false, the method returns as soon as the first CertStore returns
     * a matching cert(s).
     *
     * Returns true iff resultCerts changed (a cert was added to the collection)
     */
    boolean addMatchingCerts(X509CertSelector selector,
                             Collection<CertStore> certStores,
                             Collection<X509Certificate> resultCerts,
                             boolean checkAll)
    {
        X509Certificate targetCert = selector.getCertificate();
        if (targetCert != null) {
            // no need to search CertStores
            if (selector.match(targetCert) && !X509CertImpl.isSelfSigned
                (targetCert, buildParams.sigProvider())) {
                if (debug != null) {
                    debug.println("Builder.addMatchingCerts: " +
                        "adding target cert" +
                        "\n  SN: " + Debug.toHexString(
                                            targetCert.getSerialNumber()) +
                        "\n  Subject: " + targetCert.getSubjectX500Principal() +
                        "\n  Issuer: " + targetCert.getIssuerX500Principal());
                }
                return resultCerts.add(targetCert);
            }
            return false;
        }
        boolean add = false;
        for (CertStore store : certStores) {
            try {
                Collection<? extends Certificate> certs =
                                        store.getCertificates(selector);
                for (Certificate cert : certs) {
                    if (!X509CertImpl.isSelfSigned
                        ((X509Certificate)cert, buildParams.sigProvider())) {
                        if (resultCerts.add((X509Certificate)cert)) {
                            add = true;
                        }
                    }
                }
                if (!checkAll && add) {
                    return true;
                }
            } catch (CertStoreException cse) {
                // if getCertificates throws a CertStoreException, we ignore
                // it and move on to the next CertStore
                if (debug != null) {
                    debug.println("Builder.addMatchingCerts, non-fatal " +
                        "exception retrieving certs: " + cse);
                    cse.printStackTrace();
                }
            }
        }
        return add;
    }
}
