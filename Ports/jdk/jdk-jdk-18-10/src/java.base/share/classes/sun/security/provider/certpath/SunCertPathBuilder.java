/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.security.InvalidAlgorithmParameterException;
import java.security.PublicKey;
import java.security.cert.*;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.PKIXReason;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import javax.security.auth.x500.X500Principal;

import sun.security.provider.certpath.PKIX.BuilderParams;
import static sun.security.x509.PKIXExtensions.*;
import sun.security.util.Debug;

/**
 * This class builds certification paths in the forward direction.
 *
 * <p> If successful, it returns a certification path which has successfully
 * satisfied all the constraints and requirements specified in the
 * PKIXBuilderParameters object and has been validated according to the PKIX
 * path validation algorithm defined in RFC 5280.
 *
 * <p> This implementation uses a depth-first search approach to finding
 * certification paths. If it comes to a point in which it cannot find
 * any more certificates leading to the target OR the path length is too long
 * it backtracks to previous paths until the target has been found or
 * all possible paths have been exhausted.
 *
 * <p> This implementation is not thread-safe.
 *
 * @since       1.4
 * @author      Sean Mullan
 * @author      Yassir Elley
 */
public final class SunCertPathBuilder extends CertPathBuilderSpi {

    private static final Debug debug = Debug.getInstance("certpath");

    /*
     * private objects shared by methods
     */
    private BuilderParams buildParams;
    private CertificateFactory cf;
    private boolean pathCompleted = false;
    private PolicyNode policyTreeResult;
    private TrustAnchor trustAnchor;
    private PublicKey finalPublicKey;

    /**
     * Create an instance of <code>SunCertPathBuilder</code>.
     *
     * @throws CertPathBuilderException if an error occurs
     */
    public SunCertPathBuilder() throws CertPathBuilderException {
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new CertPathBuilderException(e);
        }
    }

    @Override
    public CertPathChecker engineGetRevocationChecker() {
        return new RevocationChecker();
    }

    /**
     * Attempts to build a certification path using the Sun build
     * algorithm from a trusted anchor(s) to a target subject, which must both
     * be specified in the input parameter set. This method will
     * attempt to build in the forward direction: from the target to the CA.
     *
     * <p>The certification path that is constructed is validated
     * according to the PKIX specification.
     *
     * @param params the parameter set for building a path. Must be an instance
     *  of <code>PKIXBuilderParameters</code>.
     * @return a certification path builder result.
     * @exception CertPathBuilderException Exception thrown if builder is
     *  unable to build a complete certification path from the trusted anchor(s)
     *  to the target subject.
     * @throws InvalidAlgorithmParameterException if the given parameters are
     *  inappropriate for this certification path builder.
     */
    @Override
    public CertPathBuilderResult engineBuild(CertPathParameters params)
        throws CertPathBuilderException, InvalidAlgorithmParameterException {

        if (debug != null) {
            debug.println("SunCertPathBuilder.engineBuild(" + params + ")");
        }

        buildParams = PKIX.checkBuilderParams(params);
        return build();
    }

    private PKIXCertPathBuilderResult build() throws CertPathBuilderException {
        List<List<Vertex>> adjList = new ArrayList<>();
        PKIXCertPathBuilderResult result = buildCertPath(false, adjList);
        if (result == null) {
            if (debug != null) {
                debug.println("SunCertPathBuilder.engineBuild: 2nd pass; " +
                              "try building again searching all certstores");
            }
            // try again
            adjList.clear();
            result = buildCertPath(true, adjList);
            if (result == null) {
                throw new SunCertPathBuilderException("unable to find valid "
                    + "certification path to requested target",
                    new AdjacencyList(adjList));
            }
        }
        return result;
    }

    private PKIXCertPathBuilderResult buildCertPath(boolean searchAllCertStores,
                                                    List<List<Vertex>> adjList)
        throws CertPathBuilderException
    {
        // Init shared variables and build certification path
        pathCompleted = false;
        trustAnchor = null;
        finalPublicKey = null;
        policyTreeResult = null;
        LinkedList<X509Certificate> certPathList = new LinkedList<>();
        try {
            buildForward(adjList, certPathList, searchAllCertStores);
        } catch (GeneralSecurityException | IOException e) {
            if (debug != null) {
                debug.println("SunCertPathBuilder.engineBuild() exception in "
                    + "build");
                e.printStackTrace();
            }
            throw new SunCertPathBuilderException("unable to find valid "
                + "certification path to requested target", e,
                new AdjacencyList(adjList));
        }

        // construct SunCertPathBuilderResult
        try {
            if (pathCompleted) {
                if (debug != null)
                    debug.println("SunCertPathBuilder.engineBuild() "
                                  + "pathCompleted");

                // we must return a certpath which has the target
                // as the first cert in the certpath - i.e. reverse
                // the certPathList
                Collections.reverse(certPathList);

                return new SunCertPathBuilderResult(
                    cf.generateCertPath(certPathList), trustAnchor,
                    policyTreeResult, finalPublicKey,
                    new AdjacencyList(adjList));
            }
        } catch (CertificateException e) {
            if (debug != null) {
                debug.println("SunCertPathBuilder.engineBuild() exception "
                              + "in wrap-up");
                e.printStackTrace();
            }
            throw new SunCertPathBuilderException("unable to find valid "
                + "certification path to requested target", e,
                new AdjacencyList(adjList));
        }

        return null;
    }

    /*
     * Private build forward method.
     */
    private void buildForward(List<List<Vertex>> adjacencyList,
                              LinkedList<X509Certificate> certPathList,
                              boolean searchAllCertStores)
        throws GeneralSecurityException, IOException
    {
        if (debug != null) {
            debug.println("SunCertPathBuilder.buildForward()...");
        }

        /* Initialize current state */
        ForwardState currentState = new ForwardState();
        currentState.initState(buildParams.certPathCheckers());

        /* Initialize adjacency list */
        adjacencyList.clear();
        adjacencyList.add(new LinkedList<Vertex>());

        currentState.untrustedChecker = new UntrustedChecker();

        depthFirstSearchForward(buildParams.targetSubject(), currentState,
                                new ForwardBuilder(buildParams,
                                                   searchAllCertStores),
                                adjacencyList, certPathList);
    }

    /*
     * This method performs a depth first search for a certification
     * path while building forward which meets the requirements set in
     * the parameters object.
     * It uses an adjacency list to store all certificates which were
     * tried (i.e. at one time added to the path - they may not end up in
     * the final path if backtracking occurs). This information can
     * be used later to debug or demo the build.
     *
     * See "Data Structure and Algorithms, by Aho, Hopcroft, and Ullman"
     * for an explanation of the DFS algorithm.
     *
     * @param dN the distinguished name being currently searched for certs
     * @param currentState the current PKIX validation state
     */
    private void depthFirstSearchForward(X500Principal dN,
                                         ForwardState currentState,
                                         ForwardBuilder builder,
                                         List<List<Vertex>> adjList,
                                         LinkedList<X509Certificate> cpList)
        throws GeneralSecurityException, IOException
    {
        if (debug != null) {
            debug.println("SunCertPathBuilder.depthFirstSearchForward(" + dN
                          + ", " + currentState.toString() + ")");
        }

        /*
         * Find all the certificates issued to dN which
         * satisfy the PKIX certification path constraints.
         */
        Collection<X509Certificate> certs =
            builder.getMatchingCerts(currentState, buildParams.certStores());
        List<Vertex> vertices = addVertices(certs, adjList);
        if (debug != null) {
            debug.println("SunCertPathBuilder.depthFirstSearchForward(): "
                          + "certs.size=" + vertices.size());
        }

        /*
         * For each cert in the collection, verify anything
         * that hasn't been checked yet (signature, revocation, etc)
         * and check for loops. Call depthFirstSearchForward()
         * recursively for each good cert.
         */

               vertices:
        for (Vertex vertex : vertices) {
            /**
             * Restore state to currentState each time through the loop.
             * This is important because some of the user-defined
             * checkers modify the state, which MUST be restored if
             * the cert eventually fails to lead to the target and
             * the next matching cert is tried.
             */
            ForwardState nextState = (ForwardState) currentState.clone();
            X509Certificate cert = vertex.getCertificate();

            try {
                builder.verifyCert(cert, nextState, cpList);
            } catch (GeneralSecurityException gse) {
                if (debug != null) {
                    debug.println("SunCertPathBuilder.depthFirstSearchForward()"
                                  + ": validation failed: " + gse);
                    gse.printStackTrace();
                }
                vertex.setThrowable(gse);
                continue;
            }

            /*
             * Certificate is good.
             * If cert completes the path,
             *    process userCheckers that don't support forward checking
             *    and process policies over whole path
             *    and backtrack appropriately if there is a failure
             * else if cert does not complete the path,
             *    add it to the path
             */
            if (builder.isPathCompleted(cert)) {

                if (debug != null)
                    debug.println("SunCertPathBuilder.depthFirstSearchForward()"
                                  + ": commencing final verification");

                List<X509Certificate> appendedCerts = new ArrayList<>(cpList);

                /*
                 * if the trust anchor selected is specified as a trusted
                 * public key rather than a trusted cert, then verify this
                 * cert (which is signed by the trusted public key), but
                 * don't add it yet to the cpList
                 */
                if (builder.trustAnchor.getTrustedCert() == null) {
                    appendedCerts.add(0, cert);
                }

                Set<String> initExpPolSet =
                    Collections.singleton(PolicyChecker.ANY_POLICY);

                PolicyNodeImpl rootNode = new PolicyNodeImpl(null,
                    PolicyChecker.ANY_POLICY, null, false, initExpPolSet, false);

                List<PKIXCertPathChecker> checkers = new ArrayList<>();
                PolicyChecker policyChecker
                    = new PolicyChecker(buildParams.initialPolicies(),
                                        appendedCerts.size(),
                                        buildParams.explicitPolicyRequired(),
                                        buildParams.policyMappingInhibited(),
                                        buildParams.anyPolicyInhibited(),
                                        buildParams.policyQualifiersRejected(),
                                        rootNode);
                checkers.add(policyChecker);

                // add the algorithm checker
                checkers.add(new AlgorithmChecker(builder.trustAnchor,
                        buildParams.date(), buildParams.variant()));

                BasicChecker basicChecker = null;
                if (nextState.keyParamsNeeded()) {
                    PublicKey rootKey = cert.getPublicKey();
                    if (builder.trustAnchor.getTrustedCert() == null) {
                        rootKey = builder.trustAnchor.getCAPublicKey();
                        if (debug != null)
                            debug.println(
                                "SunCertPathBuilder.depthFirstSearchForward " +
                                "using buildParams public key: " +
                                rootKey.toString());
                    }
                    TrustAnchor anchor = new TrustAnchor
                        (cert.getSubjectX500Principal(), rootKey, null);

                    // add the basic checker
                    basicChecker = new BasicChecker(anchor, buildParams.date(),
                                                    buildParams.sigProvider(),
                                                    true);
                    checkers.add(basicChecker);
                }

                buildParams.setCertPath(cf.generateCertPath(appendedCerts));

                boolean revCheckerAdded = false;
                List<PKIXCertPathChecker> ckrs = buildParams.certPathCheckers();
                for (PKIXCertPathChecker ckr : ckrs) {
                    if (ckr instanceof PKIXRevocationChecker) {
                        if (revCheckerAdded) {
                            throw new CertPathValidatorException(
                                "Only one PKIXRevocationChecker can be specified");
                        }
                        revCheckerAdded = true;
                        // if it's our own, initialize it
                        if (ckr instanceof RevocationChecker) {
                            ((RevocationChecker)ckr).init(builder.trustAnchor,
                                                          buildParams);
                        }
                    }
                }
                // only add a RevocationChecker if revocation is enabled and
                // a PKIXRevocationChecker has not already been added
                if (buildParams.revocationEnabled() && !revCheckerAdded) {
                    checkers.add(new RevocationChecker(builder.trustAnchor,
                                                       buildParams));
                }

                checkers.addAll(ckrs);

                // Why we don't need BasicChecker and RevocationChecker
                // if nextState.keyParamsNeeded() is false?

                for (int i = 0; i < appendedCerts.size(); i++) {
                    X509Certificate currCert = appendedCerts.get(i);
                    if (debug != null)
                        debug.println("current subject = "
                                      + currCert.getSubjectX500Principal());
                    Set<String> unresCritExts =
                        currCert.getCriticalExtensionOIDs();
                    if (unresCritExts == null) {
                        unresCritExts = Collections.<String>emptySet();
                    }

                    for (PKIXCertPathChecker currChecker : checkers) {
                        if (!currChecker.isForwardCheckingSupported()) {
                            if (i == 0) {
                                currChecker.init(false);

                                // The user specified
                                // AlgorithmChecker may not be
                                // able to set the trust anchor until now.
                                if (currChecker instanceof AlgorithmChecker) {
                                    ((AlgorithmChecker)currChecker).
                                        trySetTrustAnchor(builder.trustAnchor);
                                }
                            }

                            try {
                                currChecker.check(currCert, unresCritExts);
                            } catch (CertPathValidatorException cpve) {
                                if (debug != null)
                                    debug.println
                                    ("SunCertPathBuilder.depthFirstSearchForward(): " +
                                    "final verification failed: " + cpve);
                                // If the target cert itself is revoked, we
                                // cannot trust it. We can bail out here.
                                if (buildParams.targetCertConstraints().match(currCert)
                                        && cpve.getReason() == BasicReason.REVOKED) {
                                    throw cpve;
                                }
                                vertex.setThrowable(cpve);
                                continue vertices;
                            }
                        }
                    }

                    /*
                     * Remove extensions from user checkers that support
                     * forward checking. After this step, we will have
                     * removed all extensions that all user checkers
                     * are capable of processing.
                     */
                    for (PKIXCertPathChecker checker :
                         buildParams.certPathCheckers())
                    {
                        if (checker.isForwardCheckingSupported()) {
                            Set<String> suppExts =
                                checker.getSupportedExtensions();
                            if (suppExts != null) {
                                unresCritExts.removeAll(suppExts);
                            }
                        }
                    }

                    if (!unresCritExts.isEmpty()) {
                        unresCritExts.remove(BasicConstraints_Id.toString());
                        unresCritExts.remove(NameConstraints_Id.toString());
                        unresCritExts.remove(CertificatePolicies_Id.toString());
                        unresCritExts.remove(PolicyMappings_Id.toString());
                        unresCritExts.remove(PolicyConstraints_Id.toString());
                        unresCritExts.remove(InhibitAnyPolicy_Id.toString());
                        unresCritExts.remove(
                            SubjectAlternativeName_Id.toString());
                        unresCritExts.remove(KeyUsage_Id.toString());
                        unresCritExts.remove(ExtendedKeyUsage_Id.toString());

                        if (!unresCritExts.isEmpty()) {
                            throw new CertPathValidatorException
                                ("unrecognized critical extension(s)", null,
                                 null, -1, PKIXReason.UNRECOGNIZED_CRIT_EXT);
                        }
                    }
                }
                if (debug != null)
                    debug.println("SunCertPathBuilder.depthFirstSearchForward()"
                        + ": final verification succeeded - path completed!");
                pathCompleted = true;

                /*
                 * if the user specified a trusted public key rather than
                 * trusted certs, then add this cert (which is signed by
                 * the trusted public key) to the cpList
                 */
                if (builder.trustAnchor.getTrustedCert() == null)
                    builder.addCertToPath(cert, cpList);
                // Save the trust anchor
                this.trustAnchor = builder.trustAnchor;

                /*
                 * Extract and save the final target public key
                 */
                if (basicChecker != null) {
                    finalPublicKey = basicChecker.getPublicKey();
                } else {
                    Certificate finalCert;
                    if (cpList.isEmpty()) {
                        finalCert = builder.trustAnchor.getTrustedCert();
                    } else {
                        finalCert = cpList.getLast();
                    }
                    finalPublicKey = finalCert.getPublicKey();
                }

                policyTreeResult = policyChecker.getPolicyTree();
                return;
            } else {
                builder.addCertToPath(cert, cpList);
            }

            /* Update the PKIX state */
            nextState.updateState(cert);

            /*
             * Append an entry for cert in adjacency list and
             * set index for current vertex.
             */
            adjList.add(new LinkedList<Vertex>());
            vertex.setIndex(adjList.size() - 1);

            /* recursively search for matching certs at next dN */
            depthFirstSearchForward(cert.getIssuerX500Principal(), nextState,
                                    builder, adjList, cpList);

            /*
             * If path has been completed, return ASAP!
             */
            if (pathCompleted) {
                return;
            } else {
                /*
                 * If we get here, it means we have searched all possible
                 * certs issued by the dN w/o finding any matching certs.
                 * This means we have to backtrack to the previous cert in
                 * the path and try some other paths.
                 */
                if (debug != null)
                    debug.println("SunCertPathBuilder.depthFirstSearchForward()"
                                  + ": backtracking");
                builder.removeFinalCertFromPath(cpList);
            }
        }
    }

    /*
     * Adds a collection of matching certificates to the
     * adjacency list.
     */
    private static List<Vertex> addVertices(Collection<X509Certificate> certs,
                                            List<List<Vertex>> adjList)
    {
        List<Vertex> l = adjList.get(adjList.size() - 1);

        for (X509Certificate cert : certs) {
            Vertex v = new Vertex(cert);
            l.add(v);
        }

        return l;
    }

    /**
     * Returns true if trust anchor certificate matches specified
     * certificate constraints.
     */
    private static boolean anchorIsTarget(TrustAnchor anchor,
                                          CertSelector sel)
    {
        X509Certificate anchorCert = anchor.getTrustedCert();
        if (anchorCert != null) {
            return sel.match(anchorCert);
        }
        return false;
    }
}
