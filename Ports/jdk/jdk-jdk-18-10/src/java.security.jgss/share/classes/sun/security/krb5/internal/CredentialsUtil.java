/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 *
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5.internal;

import sun.security.krb5.*;
import sun.security.util.DerValue;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is a utility that contains much of the TGS-Exchange
 * protocol. It is used by ../Credentials.java for service ticket
 * acquisition in both the normal and the x-realm case.
 */
public class CredentialsUtil {

    private static boolean DEBUG = sun.security.krb5.internal.Krb5.DEBUG;

    private static enum S4U2Type {
        NONE, SELF, PROXY
    }

    /**
     * Used by a middle server to acquire credentials on behalf of a
     * client to itself using the S4U2self extension.
     * @param client the client to impersonate
     * @param ccreds the TGT of the middle service
     * @return the new creds (cname=client, sname=middle)
     */
    public static Credentials acquireS4U2selfCreds(PrincipalName client,
            Credentials ccreds) throws KrbException, IOException {
        if (!ccreds.isForwardable()) {
            throw new KrbException("S4U2self needs a FORWARDABLE ticket");
        }
        PrincipalName sname = ccreds.getClient();
        String uRealm = client.getRealmString();
        String localRealm = ccreds.getClient().getRealmString();
        if (!uRealm.equals(localRealm)) {
            // Referrals will be required because the middle service
            // and the client impersonated are on different realms.
            if (Config.DISABLE_REFERRALS) {
                throw new KrbException("Cross-realm S4U2Self request not" +
                        " possible when referrals are disabled.");
            }
            if (ccreds.getClientAlias() != null) {
                // If the name was canonicalized, the user pick
                // has preference. This gives the possibility of
                // using FQDNs that KDCs may use to return referrals.
                // I.e.: a SVC/host.realm-2.com@REALM-1.COM name
                // may be used by REALM-1.COM KDC to return a
                // referral to REALM-2.COM.
                sname = ccreds.getClientAlias();
            }
            sname = new PrincipalName(sname.getNameType(),
                    sname.getNameStrings(), new Realm(uRealm));
        }
        Credentials creds = serviceCreds(
                KDCOptions.with(KDCOptions.FORWARDABLE),
                ccreds, ccreds.getClient(), sname, null,
                new PAData[] {
                        new PAData(Krb5.PA_FOR_USER,
                                new PAForUserEnc(client,
                                        ccreds.getSessionKey()).asn1Encode()),
                        new PAData(Krb5.PA_PAC_OPTIONS,
                                new PaPacOptions()
                                        .setResourceBasedConstrainedDelegation(true)
                                        .setClaims(true)
                                        .asn1Encode())
                        }, S4U2Type.SELF);
        if (!creds.getClient().equals(client)) {
            throw new KrbException("S4U2self request not honored by KDC");
        }
        if (!creds.isForwardable()) {
            throw new KrbException("S4U2self ticket must be FORWARDABLE");
        }
        return creds;
    }

    /**
     * Used by a middle server to acquire a service ticket to a backend
     * server using the S4U2proxy extension.
     * @param backend the name of the backend service
     * @param second the client's service ticket to the middle server
     * @param ccreds the TGT of the middle server
     * @return the creds (cname=client, sname=backend)
     */
    public static Credentials acquireS4U2proxyCreds(
                String backend, Ticket second,
                PrincipalName client, Credentials ccreds)
            throws KrbException, IOException {
        PrincipalName backendPrincipal = new PrincipalName(backend);
        String backendRealm = backendPrincipal.getRealmString();
        String localRealm = ccreds.getClient().getRealmString();
        if (!backendRealm.equals(localRealm)) {
            // The middle service and the backend service are on
            // different realms, so referrals will be required.
            if (Config.DISABLE_REFERRALS) {
                throw new KrbException("Cross-realm S4U2Proxy request not" +
                        " possible when referrals are disabled.");
            }
            backendPrincipal = new PrincipalName(
                    backendPrincipal.getNameType(),
                    backendPrincipal.getNameStrings(),
                    new Realm(localRealm));
        }
        Credentials creds = serviceCreds(KDCOptions.with(
                KDCOptions.CNAME_IN_ADDL_TKT, KDCOptions.FORWARDABLE),
                ccreds, ccreds.getClient(), backendPrincipal,
                new Ticket[] {second}, new PAData[] {
                        new PAData(Krb5.PA_PAC_OPTIONS,
                                new PaPacOptions()
                                        .setResourceBasedConstrainedDelegation(true)
                                        .setClaims(true)
                                        .asn1Encode())
                        }, S4U2Type.PROXY);
        if (!creds.getClient().equals(client)) {
            throw new KrbException("S4U2proxy request not honored by KDC");
        }
        return creds;
    }

    /**
     * Acquires credentials for a specified service using initial
     * credential. When the service has a different realm from the initial
     * credential, we do cross-realm authentication - first, we use the
     * current credential to get a cross-realm credential from the local KDC,
     * then use that cross-realm credential to request service credential
     * from the foreign KDC.
     *
     * @param service the name of service principal
     * @param ccreds client's initial credential
     */
    public static Credentials acquireServiceCreds(
                String service, Credentials ccreds)
            throws KrbException, IOException {
        PrincipalName sname = new PrincipalName(service,
                PrincipalName.KRB_NT_UNKNOWN);
        return serviceCreds(sname, ccreds);
    }

    /**
     * Gets a TGT to another realm
     * @param localRealm this realm
     * @param serviceRealm the other realm, cannot equals to localRealm
     * @param ccreds TGT in this realm
     * @param okAsDelegate an [out] argument to receive the okAsDelegate
     * property. True only if all realms allow delegation.
     * @return the TGT for the other realm, null if cannot find a path
     * @throws KrbException if something goes wrong
     */
    private static Credentials getTGTforRealm(String localRealm,
            String serviceRealm, Credentials ccreds, boolean[] okAsDelegate)
            throws KrbException {

        // Get a list of realms to traverse
        String[] realms = Realm.getRealmsList(localRealm, serviceRealm);

        int i = 0, k = 0;
        Credentials cTgt = null, newTgt = null, theTgt = null;
        PrincipalName tempService = null;
        String newTgtRealm = null;

        okAsDelegate[0] = true;
        for (cTgt = ccreds, i = 0; i < realms.length;) {
            tempService = PrincipalName.tgsService(serviceRealm, realms[i]);

            if (DEBUG) {
                System.out.println(
                        ">>> Credentials acquireServiceCreds: main loop: ["
                        + i +"] tempService=" + tempService);
            }

            try {
                newTgt = serviceCreds(tempService, cTgt);
            } catch (Exception exc) {
                newTgt = null;
            }

            if (newTgt == null) {
                if (DEBUG) {
                    System.out.println(">>> Credentials acquireServiceCreds: "
                            + "no tgt; searching thru capath");
                }

                /*
                 * No tgt found. Let's go thru the realms list one by one.
                 */
                for (newTgt = null, k = i+1;
                        newTgt == null && k < realms.length; k++) {
                    tempService = PrincipalName.tgsService(realms[k], realms[i]);
                    if (DEBUG) {
                        System.out.println(
                                ">>> Credentials acquireServiceCreds: "
                                + "inner loop: [" + k
                                + "] tempService=" + tempService);
                    }
                    try {
                        newTgt = serviceCreds(tempService, cTgt);
                    } catch (Exception exc) {
                        newTgt = null;
                    }
                }
            } // Ends 'if (newTgt == null)'

            if (newTgt == null) {
                if (DEBUG) {
                    System.out.println(">>> Credentials acquireServiceCreds: "
                            + "no tgt; cannot get creds");
                }
                break;
            }

            /*
             * We have a tgt. It may or may not be for the target.
             * If it's for the target realm, we're done looking for a tgt.
             */
            newTgtRealm = newTgt.getServer().getInstanceComponent();
            if (okAsDelegate[0] && !newTgt.checkDelegate()) {
                if (DEBUG) {
                    System.out.println(">>> Credentials acquireServiceCreds: " +
                            "global OK-AS-DELEGATE turned off at " +
                            newTgt.getServer());
                }
                okAsDelegate[0] = false;
            }

            if (DEBUG) {
                System.out.println(">>> Credentials acquireServiceCreds: "
                        + "got tgt");
            }

            if (newTgtRealm.equals(serviceRealm)) {
                /* We got the right tgt */
                theTgt = newTgt;
                break;
            }

            /*
             * The new tgt is not for the target realm.
             * See if the realm of the new tgt is in the list of realms
             * and continue looking from there.
             */
            for (k = i+1; k < realms.length; k++) {
                if (newTgtRealm.equals(realms[k])) {
                    break;
                }
            }

            if (k < realms.length) {
                /*
                 * (re)set the counter so we start looking
                 * from the realm we just obtained a tgt for.
                 */
                i = k;
                cTgt = newTgt;

                if (DEBUG) {
                    System.out.println(">>> Credentials acquireServiceCreds: "
                            + "continuing with main loop counter reset to " + i);
                }
                continue;
            }
            else {
                /*
                 * The new tgt's realm is not in the hierarchy of realms.
                 * It's probably not safe to get a tgt from
                 * a tgs that is outside the known list of realms.
                 * Give up now.
                 */
                break;
            }
        } // Ends outermost/main 'for' loop

        return theTgt;
    }

   /*
    * This method does the real job to request the service credential.
    */
    private static Credentials serviceCreds(
            PrincipalName service, Credentials ccreds)
            throws KrbException, IOException {
        return serviceCreds(new KDCOptions(), ccreds,
                ccreds.getClient(), service, null, null,
                S4U2Type.NONE);
    }

    /*
     * Obtains credentials for a service (TGS).
     * Cross-realm referrals are handled if enabled. A fallback scheme
     * without cross-realm referrals supports is used in case of server
     * error to maintain backward compatibility.
     */
    private static Credentials serviceCreds(
            KDCOptions options, Credentials asCreds,
            PrincipalName cname, PrincipalName sname,
            Ticket[] additionalTickets, PAData[] extraPAs,
            S4U2Type s4u2Type)
            throws KrbException, IOException {
        if (!Config.DISABLE_REFERRALS) {
            try {
                return serviceCredsReferrals(options, asCreds, cname, sname,
                        s4u2Type, additionalTickets, extraPAs);
            } catch (KrbException e) {
                // Server may raise an error if CANONICALIZE is true.
                // Try CANONICALIZE false.
            }
        }
        return serviceCredsSingle(options, asCreds, cname,
                asCreds.getClientAlias(), sname, sname, s4u2Type,
                additionalTickets, extraPAs);
    }

    /*
     * Obtains credentials for a service (TGS).
     * May handle and follow cross-realm referrals as defined by RFC 6806.
     */
    private static Credentials serviceCredsReferrals(
            KDCOptions options, Credentials asCreds,
            PrincipalName cname, PrincipalName sname,
            S4U2Type s4u2Type, Ticket[] additionalTickets,
            PAData[] extraPAs)
                    throws KrbException, IOException {
        options = new KDCOptions(options.toBooleanArray());
        options.set(KDCOptions.CANONICALIZE, true);
        PrincipalName cSname = sname;
        PrincipalName refSname = sname; // May change with referrals
        Credentials creds = null;
        boolean isReferral = false;
        List<String> referrals = new LinkedList<>();
        PrincipalName clientAlias = asCreds.getClientAlias();
        while (referrals.size() <= Config.MAX_REFERRALS) {
            ReferralsCache.ReferralCacheEntry ref =
                    ReferralsCache.get(cname, sname, refSname.getRealmString());
            String toRealm = null;
            if (ref == null) {
                creds = serviceCredsSingle(options, asCreds, cname,
                        clientAlias, refSname, cSname, s4u2Type,
                        additionalTickets, extraPAs);
                PrincipalName server = creds.getServer();
                if (!refSname.equals(server)) {
                    String[] serverNameStrings = server.getNameStrings();
                    if (serverNameStrings.length == 2 &&
                        serverNameStrings[0].equals(
                                PrincipalName.TGS_DEFAULT_SRV_NAME) &&
                        !refSname.getRealmAsString().equals(
                                serverNameStrings[1])) {
                        // Server Name (sname) has the following format:
                        //      krbtgt/TO-REALM.COM@FROM-REALM.COM
                        if (s4u2Type == S4U2Type.NONE) {
                            // Do not store S4U2Self or S4U2Proxy referral
                            // TGTs in the cache. Caching such tickets is not
                            // defined in MS-SFU and may cause unexpected
                            // results when using them in a different context.
                            ReferralsCache.put(cname, sname,
                                    server.getRealmString(),
                                    serverNameStrings[1], creds);
                        }
                        toRealm = serverNameStrings[1];
                        isReferral = true;
                    }
                }
            } else {
                creds = ref.getCreds();
                toRealm = ref.getToRealm();
                isReferral = true;
            }
            if (isReferral) {
                if (s4u2Type == S4U2Type.PROXY) {
                    Credentials[] credsInOut =
                            new Credentials[] {creds, null};
                    toRealm = handleS4U2ProxyReferral(asCreds,
                            credsInOut, sname);
                    creds = credsInOut[0];
                    if (additionalTickets == null ||
                            additionalTickets.length == 0 ||
                            credsInOut[1] == null) {
                        throw new KrbException("Additional tickets expected" +
                                " for S4U2Proxy.");
                    }
                    additionalTickets[0] = credsInOut[1].getTicket();
                } else if (s4u2Type == S4U2Type.SELF) {
                    handleS4U2SelfReferral(extraPAs, asCreds, creds);
                }
                if (referrals.contains(toRealm)) {
                    // Referrals loop detected
                    return null;
                }
                asCreds = creds;
                refSname = new PrincipalName(refSname.getNameString(),
                        refSname.getNameType(), toRealm);
                referrals.add(toRealm);
                isReferral = false;
                continue;
            }
            break;
        }
        return creds;
    }

    /*
     * Obtains credentials for a service (TGS).
     * If the service realm is different than the one in the TGT, a new TGT for
     * the service realm is obtained first (see getTGTforRealm call). This is
     * not expected when following cross-realm referrals because the referral
     * TGT realm matches the service realm.
     */
    private static Credentials serviceCredsSingle(
            KDCOptions options, Credentials asCreds,
            PrincipalName cname, PrincipalName clientAlias,
            PrincipalName refSname, PrincipalName sname,
            S4U2Type s4u2Type, Ticket[] additionalTickets,
            PAData[] extraPAs)
                    throws KrbException, IOException {
        Credentials theCreds = null;
        boolean[] okAsDelegate = new boolean[]{true};
        String[] serverAsCredsNames = asCreds.getServer().getNameStrings();
        String tgtRealm = serverAsCredsNames[1];
        String serviceRealm = refSname.getRealmString();
        if (!serviceRealm.equals(tgtRealm)) {
            // This is a cross-realm service request
            if (DEBUG) {
                System.out.println(">>> serviceCredsSingle:" +
                        " cross-realm authentication");
                System.out.println(">>> serviceCredsSingle:" +
                        " obtaining credentials from " + tgtRealm +
                        " to " + serviceRealm);
            }
            Credentials newTgt = getTGTforRealm(tgtRealm, serviceRealm,
                    asCreds, okAsDelegate);
            if (newTgt == null) {
                throw new KrbApErrException(Krb5.KRB_AP_ERR_GEN_CRED,
                        "No service creds");
            }
            if (DEBUG) {
                System.out.println(">>> Cross-realm TGT Credentials" +
                        " serviceCredsSingle: ");
                Credentials.printDebug(newTgt);
            }
            if (s4u2Type == S4U2Type.SELF) {
                handleS4U2SelfReferral(extraPAs, asCreds, newTgt);
            }
            asCreds = newTgt;
            cname = asCreds.getClient();
        } else if (DEBUG) {
            System.out.println(">>> Credentials serviceCredsSingle:" +
                    " same realm");
        }
        KrbTgsReq req = new KrbTgsReq(options, asCreds, cname, clientAlias,
                refSname, sname, additionalTickets, extraPAs);
        theCreds = req.sendAndGetCreds();
        if (theCreds != null) {
            if (DEBUG) {
                System.out.println(">>> TGS credentials serviceCredsSingle:");
                Credentials.printDebug(theCreds);
            }
            if (!okAsDelegate[0]) {
                theCreds.resetDelegate();
            }
        }
        return theCreds;
    }

    /**
     * PA-FOR-USER may need to be regenerated if credentials
     * change. This may happen when obtaining a TGT for a
     * different realm or when using a referral TGT.
     */
    private static void handleS4U2SelfReferral(PAData[] pas,
            Credentials oldCeds, Credentials newCreds)
                    throws Asn1Exception, KrbException, IOException {
        if (DEBUG) {
            System.out.println(">>> Handling S4U2Self referral");
        }
        for (int i = 0; i < pas.length; i++) {
            PAData pa = pas[i];
            if (pa.getType() == Krb5.PA_FOR_USER) {
                PAForUserEnc paForUser = new PAForUserEnc(
                        new DerValue(pa.getValue()),
                        oldCeds.getSessionKey());
                pas[i] = new PAData(Krb5.PA_FOR_USER,
                        new PAForUserEnc(paForUser.getName(),
                                newCreds.getSessionKey()).asn1Encode());
                break;
            }
        }
    }

    /**
     * This method is called after receiving the first realm referral for
     * a S4U2Proxy request. The credentials and tickets needed for the
     * final S4U2Proxy request (in the referrals chain) are returned.
     *
     * Referrals are handled as described by MS-SFU (section 3.1.5.2.2
     * Receives Referral).
     *
     * @param asCreds middle service credentials used for the first S4U2Proxy
     *        request
     * @param credsInOut (in/out parameter):
     *         * input: first S4U2Proxy referral TGT received, null
     *         * output: referral TGT for final S4U2Proxy service request,
     *                   client referral TGT for final S4U2Proxy service request
     *                   (to be sent as additional-ticket)
     * @param sname the backend service name
     * @param additionalTickets (out parameter): the additional ticket for the
     *        last S4U2Proxy request is returned
     * @return the backend realm for the last S4U2Proxy request
     */
    private static String handleS4U2ProxyReferral(Credentials asCreds,
            Credentials[] credsInOut, PrincipalName sname)
                    throws KrbException, IOException {
        if (DEBUG) {
            System.out.println(">>> Handling S4U2Proxy referral");
        }
        Credentials refTGT = null;
        // Get a credential for the middle service to the backend so we know
        // the backend realm, as described in MS-SFU (section 3.1.5.2.2).
        Credentials middleSvcCredsInBackendRealm =
                serviceCreds(sname, asCreds);
        String backendRealm =
                middleSvcCredsInBackendRealm.getServer().getRealmString();
        String toRealm = credsInOut[0].getServer().getNameStrings()[1];
        if (!toRealm.equals(backendRealm)) {
            // More than 1 hop. Follow the referrals chain and obtain a
            // TGT for the backend realm.
            refTGT = getTGTforRealm(toRealm, backendRealm, credsInOut[0],
                    new boolean[1]);
        } else {
            // There was only 1 hop. The referral TGT received is already
            // for the backend realm.
            refTGT = credsInOut[0];
        }
        credsInOut[0] = getTGTforRealm(asCreds.getClient().getRealmString(),
                backendRealm, asCreds, new boolean[1]);
        credsInOut[1] = refTGT;
        return backendRealm;
    }
}
