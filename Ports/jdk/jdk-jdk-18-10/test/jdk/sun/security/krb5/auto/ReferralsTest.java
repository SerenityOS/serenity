/*
 * Copyright (c) 2019, 2020, Red Hat, Inc.
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
 * @test
 * @bug 8215032
 * @library /test/lib
 * @run main/othervm/timeout=120 -Dsun.security.krb5.debug=true ReferralsTest
 * @summary Test Kerberos cross-realm referrals (RFC 6806)
 */

import java.io.File;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSName;

import sun.security.jgss.GSSUtil;
import sun.security.krb5.Config;
import sun.security.krb5.PrincipalName;

public class ReferralsTest {
    private static final boolean DEBUG = true;
    private static final String krbConfigName = "krb5-localkdc.conf";
    private static final String krbConfigNameNoCanonicalize =
            "krb5-localkdc-nocanonicalize.conf";
    private static final String realmKDC1 = "RABBIT.HOLE";
    private static final String realmKDC2 = "DEV.RABBIT.HOLE";
    private static final char[] password = "123qwe@Z".toCharArray();

    // Names
    private static final String clientName = "test";
    private static final String userName = "user";
    private static final String serviceName = "http" +
            PrincipalName.NAME_COMPONENT_SEPARATOR_STR +
            "server.dev.rabbit.hole";
    private static final String backendServiceName = "cifs" +
            PrincipalName.NAME_COMPONENT_SEPARATOR_STR +
            "backend.rabbit.hole";

    // Alias
    private static final String clientAlias = clientName +
            PrincipalName.NAME_REALM_SEPARATOR_STR + realmKDC1;

    // Names + realms
    private static final String clientKDC1Name = clientAlias.replaceAll(
            PrincipalName.NAME_REALM_SEPARATOR_STR, "\\\\" +
            PrincipalName.NAME_REALM_SEPARATOR_STR) +
            PrincipalName.NAME_REALM_SEPARATOR_STR + realmKDC1;
    private static final String clientKDC2Name = clientName +
            PrincipalName.NAME_REALM_SEPARATOR_STR + realmKDC2;
    private static final String userKDC1Name = userName +
            PrincipalName.NAME_REALM_SEPARATOR_STR + realmKDC1;
    private static final String serviceKDC2Name = serviceName +
            PrincipalName.NAME_REALM_SEPARATOR_STR + realmKDC2;
    private static final String backendKDC1Name = backendServiceName +
            PrincipalName.NAME_REALM_SEPARATOR_STR + realmKDC1;
    private static final String krbtgtKDC1 =
            PrincipalName.TGS_DEFAULT_SRV_NAME +
            PrincipalName.NAME_COMPONENT_SEPARATOR_STR + realmKDC1;
    private static final String krbtgtKDC2 =
            PrincipalName.TGS_DEFAULT_SRV_NAME +
            PrincipalName.NAME_COMPONENT_SEPARATOR_STR + realmKDC2;
    private static final String krbtgtKDC1toKDC2 =
            PrincipalName.TGS_DEFAULT_SRV_NAME +
            PrincipalName.NAME_COMPONENT_SEPARATOR_STR + realmKDC2 +
            PrincipalName.NAME_REALM_SEPARATOR_STR + realmKDC1;
    private static final String krbtgtKDC2toKDC1 =
            PrincipalName.TGS_DEFAULT_SRV_NAME +
            PrincipalName.NAME_COMPONENT_SEPARATOR_STR + realmKDC1 +
            PrincipalName.NAME_REALM_SEPARATOR_STR + realmKDC2;

    public static void main(String[] args) throws Exception {
        try {
            initializeKDCs();
            testSubjectCredentials();
            testDelegation();
            testImpersonation();
            testDelegationWithReferrals();
            testNoCanonicalize();
        } finally {
            cleanup();
        }
    }

    private static void initializeKDCs() throws Exception {
        KDC kdc1 = KDC.create(realmKDC1, "localhost", 0, true);
        kdc1.addPrincipalRandKey(krbtgtKDC1);
        kdc1.addPrincipal(krbtgtKDC2toKDC1, password);
        kdc1.addPrincipal(krbtgtKDC2, password);
        kdc1.addPrincipal(userKDC1Name, password);
        kdc1.addPrincipal(backendServiceName, password);

        KDC kdc2 = KDC.create(realmKDC2, "localhost", 0, true);
        kdc2.addPrincipalRandKey(krbtgtKDC2);
        kdc2.addPrincipal(clientKDC2Name, password);
        kdc2.addPrincipal(serviceName, password);
        kdc2.addPrincipal(krbtgtKDC1, password);
        kdc2.addPrincipal(krbtgtKDC1toKDC2, password);

        kdc1.registerAlias(clientAlias, kdc2);
        kdc1.registerAlias(serviceName, kdc2);
        kdc2.registerAlias(clientAlias, clientKDC2Name);
        kdc2.registerAlias(backendServiceName, kdc1);

        kdc1.setOption(KDC.Option.ALLOW_S4U2SELF, Arrays.asList(
                new String[]{serviceName + "@" + realmKDC2}));
        Map<String,List<String>> mapKDC1 = new HashMap<>();
        mapKDC1.put(serviceName + "@" + realmKDC2, Arrays.asList(
                new String[]{backendKDC1Name}));
        kdc1.setOption(KDC.Option.ALLOW_S4U2PROXY, mapKDC1);

        Map<String,List<String>> mapKDC2 = new HashMap<>();
        mapKDC2.put(serviceName + "@" + realmKDC2, Arrays.asList(
                new String[]{serviceName + "@" + realmKDC2,
                        krbtgtKDC2toKDC1}));
        kdc2.setOption(KDC.Option.ALLOW_S4U2PROXY, mapKDC2);

        KDC.saveConfig(krbConfigName, kdc1, kdc2,
                "forwardable=true", "canonicalize=true");
        KDC.saveConfig(krbConfigNameNoCanonicalize, kdc1, kdc2,
                "forwardable=true");
        System.setProperty("java.security.krb5.conf", krbConfigName);
    }

    private static void cleanup() {
        String[] configFiles = new String[]{krbConfigName,
                krbConfigNameNoCanonicalize};
        for (String configFile : configFiles) {
            File f = new File(configFile);
            if (f.exists()) {
                f.delete();
            }
        }
    }

    /*
     * The client subject (whose principal is
     * test@RABBIT.HOLE@RABBIT.HOLE) will obtain a TGT after
     * realm referral and name canonicalization (TGT cname
     * will be test@DEV.RABBIT.HOLE). With this TGT, the client will request
     * a TGS for service http/server.dev.rabbit.hole@RABBIT.HOLE. After
     * realm referral, a http/server.dev.rabbit.hole@DEV.RABBIT.HOLE TGS
     * will be obtained.
     *
     * Assert that we get the proper TGT and TGS tickets, and that they are
     * associated to the client subject.
     *
     * Assert that if we request a TGS for the same service again (based on the
     * original service name), we don't get a new one but the previous,
     * already in the subject credentials.
     */
    private static void testSubjectCredentials() throws Exception {
        Subject clientSubject = new Subject();
        Context clientContext = Context.fromUserPass(clientSubject,
                clientKDC1Name, password, false);

        Set<Principal> clientPrincipals = clientSubject.getPrincipals();
        if (clientPrincipals.size() != 1) {
            throw new Exception("Only one client subject principal expected");
        }
        Principal clientPrincipal = clientPrincipals.iterator().next();
        if (DEBUG) {
            System.out.println("Client subject principal: " +
                    clientPrincipal.getName());
        }
        if (!clientPrincipal.getName().equals(clientKDC1Name)) {
            throw new Exception("Unexpected client subject principal.");
        }

        clientContext.startAsClient(serviceName, GSSUtil.GSS_KRB5_MECH_OID);
        clientContext.take(new byte[0]);
        Set<KerberosTicket> clientTickets =
                clientSubject.getPrivateCredentials(KerberosTicket.class);
        boolean tgtFound = false;
        boolean tgsFound = false;
        for (KerberosTicket clientTicket : clientTickets) {
            String cname = clientTicket.getClient().getName();
            String sname = clientTicket.getServer().getName();
            if (cname.equals(clientKDC2Name)) {
                if (sname.equals(krbtgtKDC2 +
                        PrincipalName.NAME_REALM_SEPARATOR_STR + realmKDC2)) {
                    tgtFound = true;
                } else if (sname.equals(serviceKDC2Name)) {
                    tgsFound = true;
                }
            }
            if (DEBUG) {
                System.out.println("Client subject KerberosTicket:");
                System.out.println(clientTicket);
            }
        }
        if (!tgtFound || !tgsFound) {
            throw new Exception("client subject tickets (TGT/TGS) not found.");
        }
        int numOfTickets = clientTickets.size();
        clientContext.startAsClient(serviceName, GSSUtil.GSS_KRB5_MECH_OID);
        clientContext.take(new byte[0]);
        clientContext.status();
        int newNumOfTickets =
                clientSubject.getPrivateCredentials(KerberosTicket.class).size();
        if (DEBUG) {
            System.out.println("client subject number of tickets: " +
                    numOfTickets);
            System.out.println("client subject new number of tickets: " +
                    newNumOfTickets);
        }
        if (numOfTickets != newNumOfTickets) {
            throw new Exception("Useless client subject TGS request because" +
                    " TGS was not found in private credentials.");
        }
    }

    /*
     * The server (http/server.dev.rabbit.hole@DEV.RABBIT.HOLE)
     * will authenticate on itself on behalf of the client
     * (test@DEV.RABBIT.HOLE). Cross-realm referrals will occur
     * when requesting different TGTs and TGSs (including the
     * request for delegated credentials).
     */
    private static void testDelegation() throws Exception {
        Context c = Context.fromUserPass(clientKDC2Name,
                password, false);
        c.startAsClient(serviceName, GSSUtil.GSS_KRB5_MECH_OID);
        Context s = Context.fromUserPass(serviceKDC2Name,
                password, true);
        s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);
        Context.handshake(c, s);
        Context delegatedContext = s.delegated();
        delegatedContext.startAsClient(serviceName, GSSUtil.GSS_KRB5_MECH_OID);
        delegatedContext.x().requestMutualAuth(false);
        Context s2 = Context.fromUserPass(serviceKDC2Name,
                password, true);
        s2.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);

        // Test authentication
        Context.handshake(delegatedContext, s2);
        if (!delegatedContext.x().isEstablished() || !s2.x().isEstablished()) {
            throw new Exception("Delegated authentication failed");
        }

        // Test identities
        GSSName contextInitiatorName = delegatedContext.x().getSrcName();
        GSSName contextAcceptorName = delegatedContext.x().getTargName();
        if (DEBUG) {
            System.out.println("Context initiator: " + contextInitiatorName);
            System.out.println("Context acceptor: " + contextAcceptorName);
        }
        if (!contextInitiatorName.toString().equals(clientKDC2Name) ||
                !contextAcceptorName.toString().equals(serviceName)) {
            throw new Exception("Unexpected initiator or acceptor names");
        }
    }

    /*
     * The server (http/server.dev.rabbit.hole@DEV.RABBIT.HOLE)
     * will get a TGS ticket for itself on behalf of the client
     * (user@RABBIT.HOLE). Cross-realm referrals will be handled
     * in S4U2Self requests because the user and the server are
     * on different realms.
     */
    private static void testImpersonation() throws Exception {
        Context s = Context.fromUserPass(serviceKDC2Name, password, true);
        s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);
        GSSName impName = s.impersonate(userKDC1Name).cred().getName();
        if (DEBUG) {
            System.out.println("Impersonated name: " + impName);
        }
        if (!impName.toString().equals(userKDC1Name)) {
            throw new Exception("Unexpected impersonated name");
        }
    }

    /*
     * The server (http/server.dev.rabbit.hole@DEV.RABBIT.HOLE)
     * will use delegated credentials (user@RABBIT.HOLE) to
     * authenticate in the backend (cifs/backend.rabbit.hole@RABBIT.HOLE).
     * Cross-realm referrals will be handled in S4U2Proxy requests
     * because the server and the backend are on different realms.
     */
    private static void testDelegationWithReferrals() throws Exception {
        Context c = Context.fromUserPass(userKDC1Name, password, false);
        c.startAsClient(serviceName, GSSUtil.GSS_KRB5_MECH_OID);
        Context s = Context.fromUserPass(serviceKDC2Name, password, true);
        s.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);
        Context.handshake(c, s);
        Context delegatedContext = s.delegated();
        delegatedContext.startAsClient(backendServiceName,
                GSSUtil.GSS_KRB5_MECH_OID);
        delegatedContext.x().requestMutualAuth(false);
        Context b = Context.fromUserPass(backendKDC1Name, password, true);
        b.startAsServer(GSSUtil.GSS_KRB5_MECH_OID);

        // Test authentication
        Context.handshake(delegatedContext, b);
        if (!delegatedContext.x().isEstablished() || !b.x().isEstablished()) {
            throw new Exception("Delegated authentication failed");
        }

        // Test identities
        GSSName contextInitiatorName = delegatedContext.x().getSrcName();
        GSSName contextAcceptorName = delegatedContext.x().getTargName();
        if (DEBUG) {
            System.out.println("Context initiator: " + contextInitiatorName);
            System.out.println("Context acceptor: " + contextAcceptorName);
        }
        if (!contextInitiatorName.toString().equals(userKDC1Name) ||
                !contextAcceptorName.toString().equals(backendServiceName)) {
            throw new Exception("Unexpected initiator or acceptor names");
        }
    }

    /*
     * The client tries to get a TGT (AS protocol) as in testSubjectCredentials
     * but without the canonicalize setting in krb5.conf. The KDC
     * must not return a referral but a failure because the client
     * is not in the local database.
     */
    private static void testNoCanonicalize() throws Exception {
        System.setProperty("java.security.krb5.conf",
                krbConfigNameNoCanonicalize);
        Config.refresh();
        try {
            Context.fromUserPass(new Subject(),
                    clientKDC1Name, password, false);
            throw new Exception("should not succeed");
        } catch (LoginException e) {
            // expected
        }
    }
}
