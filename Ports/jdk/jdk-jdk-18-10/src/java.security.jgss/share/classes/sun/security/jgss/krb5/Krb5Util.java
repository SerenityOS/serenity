/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.jgss.krb5;

import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KeyTab;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.security.AccessControlContext;

import sun.security.action.GetBooleanAction;
import sun.security.jgss.GSSUtil;
import sun.security.jgss.GSSCaller;

import sun.security.krb5.Credentials;
import sun.security.krb5.EncryptionKey;
import sun.security.krb5.KrbException;
import java.io.IOException;
import sun.security.krb5.KerberosSecrets;
import sun.security.krb5.PrincipalName;

/**
 * Utilities for obtaining and converting Kerberos tickets.
 */
public class Krb5Util {

    static final boolean DEBUG = GetBooleanAction
            .privilegedGetProperty("sun.security.krb5.debug");

    /**
     * Default constructor
     */
    private Krb5Util() {  // Cannot create one of these
    }

    /**
     * Retrieves the ticket corresponding to the client/server principal
     * pair from the Subject in the specified AccessControlContext.
     */
    static KerberosTicket getServiceTicket(GSSCaller caller,
        String clientPrincipal, String serverPrincipal,
        @SuppressWarnings("removal") AccessControlContext acc) throws LoginException {

        // Try to get ticket from acc's Subject
        @SuppressWarnings("removal")
        Subject accSubj = Subject.getSubject(acc);
        KerberosTicket ticket =
            SubjectComber.find(accSubj, serverPrincipal, clientPrincipal,
                  KerberosTicket.class);

        return ticket;
    }

    /**
     * Retrieves the initial TGT corresponding to the client principal
     * from the Subject in the specified AccessControlContext.
     * If the ticket can not be found in the Subject, and if
     * useSubjectCredsOnly is false, then obtain ticket from
     * a LoginContext.
     */
    static KerberosTicket getInitialTicket(GSSCaller caller,
            String clientPrincipal,
            @SuppressWarnings("removal") AccessControlContext acc) throws LoginException {

        // Try to get ticket from acc's Subject
        @SuppressWarnings("removal")
        Subject accSubj = Subject.getSubject(acc);
        KerberosTicket ticket =
                SubjectComber.find(accSubj, null, clientPrincipal,
                        KerberosTicket.class);

        // Try to get ticket from Subject obtained from GSSUtil
        if (ticket == null && !GSSUtil.useSubjectCredsOnly(caller)) {
            Subject subject = GSSUtil.login(caller, GSSUtil.GSS_KRB5_MECH_OID);
            ticket = SubjectComber.find(subject,
                    null, clientPrincipal, KerberosTicket.class);
        }
        return ticket;
    }

    /**
     * Retrieves the ServiceCreds for the specified server principal from
     * the Subject in the specified AccessControlContext. If not found, and if
     * useSubjectCredsOnly is false, then obtain from a LoginContext.
     *
     * NOTE: This method is also used by JSSE Kerberos Cipher Suites
     */
    public static ServiceCreds getServiceCreds(GSSCaller caller,
        String serverPrincipal, @SuppressWarnings("removal") AccessControlContext acc)
                throws LoginException {

        @SuppressWarnings("removal")
        Subject accSubj = Subject.getSubject(acc);
        ServiceCreds sc = null;
        if (accSubj != null) {
            sc = ServiceCreds.getInstance(accSubj, serverPrincipal);
        }
        if (sc == null && !GSSUtil.useSubjectCredsOnly(caller)) {
            Subject subject = GSSUtil.login(caller, GSSUtil.GSS_KRB5_MECH_OID);
            sc = ServiceCreds.getInstance(subject, serverPrincipal);
        }
        return sc;
    }

    public static KerberosTicket credsToTicket(Credentials serviceCreds) {
        EncryptionKey sessionKey =  serviceCreds.getSessionKey();
        KerberosTicket kt = new KerberosTicket(
            serviceCreds.getEncoded(),
            new KerberosPrincipal(serviceCreds.getClient().getName()),
            new KerberosPrincipal(serviceCreds.getServer().getName(),
                                KerberosPrincipal.KRB_NT_SRV_INST),
            sessionKey.getBytes(),
            sessionKey.getEType(),
            serviceCreds.getFlags(),
            serviceCreds.getAuthTime(),
            serviceCreds.getStartTime(),
            serviceCreds.getEndTime(),
            serviceCreds.getRenewTill(),
            serviceCreds.getClientAddresses());
        PrincipalName clientAlias = serviceCreds.getClientAlias();
        PrincipalName serverAlias = serviceCreds.getServerAlias();
        if (clientAlias != null) {
            KerberosSecrets.getJavaxSecurityAuthKerberosAccess()
                    .kerberosTicketSetClientAlias(kt, new KerberosPrincipal(
                            clientAlias.getName(), clientAlias.getNameType()));
        }
        if (serverAlias != null) {
            KerberosSecrets.getJavaxSecurityAuthKerberosAccess()
                    .kerberosTicketSetServerAlias(kt, new KerberosPrincipal(
                            serverAlias.getName(), serverAlias.getNameType()));
        }
        return kt;
    };

    public static Credentials ticketToCreds(KerberosTicket kerbTicket)
            throws KrbException, IOException {
        KerberosPrincipal clientAlias = KerberosSecrets
                .getJavaxSecurityAuthKerberosAccess()
                .kerberosTicketGetClientAlias(kerbTicket);
        KerberosPrincipal serverAlias = KerberosSecrets
                .getJavaxSecurityAuthKerberosAccess()
                .kerberosTicketGetServerAlias(kerbTicket);
        return new Credentials(
            kerbTicket.getEncoded(),
            kerbTicket.getClient().getName(),
            (clientAlias != null ? clientAlias.getName() : null),
            kerbTicket.getServer().getName(),
            (serverAlias != null ? serverAlias.getName() : null),
            kerbTicket.getSessionKey().getEncoded(),
            kerbTicket.getSessionKeyType(),
            kerbTicket.getFlags(),
            kerbTicket.getAuthTime(),
            kerbTicket.getStartTime(),
            kerbTicket.getEndTime(),
            kerbTicket.getRenewTill(),
            kerbTicket.getClientAddresses());
    }

    /**
     * A helper method to get a sun..KeyTab from a javax..KeyTab
     * @param ktab the javax..KeyTab object
     * @return the sun..KeyTab object
     */
    public static sun.security.krb5.internal.ktab.KeyTab
            snapshotFromJavaxKeyTab(KeyTab ktab) {
        return KerberosSecrets.getJavaxSecurityAuthKerberosAccess()
                .keyTabTakeSnapshot(ktab);
    }

    /**
     * A helper method to get EncryptionKeys from a javax..KeyTab
     * @param ktab the javax..KeyTab object
     * @param cname the PrincipalName
     * @return the EKeys, never null, might be empty
     */
    public static EncryptionKey[] keysFromJavaxKeyTab(
            KeyTab ktab, PrincipalName cname) {
        return snapshotFromJavaxKeyTab(ktab).readServiceKeys(cname);
    }
}
