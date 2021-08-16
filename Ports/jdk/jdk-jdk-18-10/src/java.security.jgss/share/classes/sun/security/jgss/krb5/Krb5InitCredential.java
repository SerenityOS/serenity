/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.ietf.jgss.*;
import sun.security.jgss.GSSCaller;
import sun.security.jgss.spi.*;
import sun.security.krb5.*;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.kerberos.KerberosPrincipal;
import java.net.InetAddress;
import java.io.IOException;
import java.util.Date;
import java.security.AccessController;
import java.security.AccessControlContext;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

/**
 * Implements the krb5 initiator credential element.
 *
 * @author Mayank Upadhyay
 * @author Ram Marti
 * @since 1.4
 */

public class Krb5InitCredential
    extends KerberosTicket
    implements Krb5CredElement {

    private static final long serialVersionUID = 7723415700837898232L;

    @SuppressWarnings("serial") // Not statically typed as Serializable
    private Krb5NameElement name;
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private Credentials krb5Credentials;
    public KerberosTicket proxyTicket;

    private Krb5InitCredential(Krb5NameElement name,
                               byte[] asn1Encoding,
                               KerberosPrincipal client,
                               KerberosPrincipal clientAlias,
                               KerberosPrincipal server,
                               KerberosPrincipal serverAlias,
                               byte[] sessionKey,
                               int keyType,
                               boolean[] flags,
                               Date authTime,
                               Date startTime,
                               Date endTime,
                               Date renewTill,
                               InetAddress[] clientAddresses)
                               throws GSSException {
        super(asn1Encoding,
              client,
              server,
              sessionKey,
              keyType,
              flags,
              authTime,
              startTime,
              endTime,
              renewTill,
              clientAddresses);
        KerberosSecrets.getJavaxSecurityAuthKerberosAccess()
                .kerberosTicketSetClientAlias(this, clientAlias);
        KerberosSecrets.getJavaxSecurityAuthKerberosAccess()
                .kerberosTicketSetServerAlias(this, serverAlias);
        this.name = name;

        try {
            // Cache this for later use by the sun.security.krb5 package.
            krb5Credentials = new Credentials(asn1Encoding,
                                              client.getName(),
                                              (clientAlias != null ?
                                                      clientAlias.getName() : null),
                                              server.getName(),
                                              (serverAlias != null ?
                                                      serverAlias.getName() : null),
                                              sessionKey,
                                              keyType,
                                              flags,
                                              authTime,
                                              startTime,
                                              endTime,
                                              renewTill,
                                              clientAddresses);
        } catch (KrbException e) {
            throw new GSSException(GSSException.NO_CRED, -1,
                                   e.getMessage());
        } catch (IOException e) {
            throw new GSSException(GSSException.NO_CRED, -1,
                                   e.getMessage());
        }

    }

    private Krb5InitCredential(Krb5NameElement name,
                               Credentials delegatedCred,
                               byte[] asn1Encoding,
                               KerberosPrincipal client,
                               KerberosPrincipal clientAlias,
                               KerberosPrincipal server,
                               KerberosPrincipal serverAlias,
                               byte[] sessionKey,
                               int keyType,
                               boolean[] flags,
                               Date authTime,
                               Date startTime,
                               Date endTime,
                               Date renewTill,
                               InetAddress[] clientAddresses)
                               throws GSSException {
        super(asn1Encoding,
              client,
              server,
              sessionKey,
              keyType,
              flags,
              authTime,
              startTime,
              endTime,
              renewTill,
              clientAddresses);
        KerberosSecrets.getJavaxSecurityAuthKerberosAccess()
                .kerberosTicketSetClientAlias(this, clientAlias);
        KerberosSecrets.getJavaxSecurityAuthKerberosAccess()
                .kerberosTicketSetServerAlias(this, serverAlias);
        this.name = name;
        // A delegated cred does not have all fields set. So do not try to
        // creat new Credentials out of the delegatedCred.
        this.krb5Credentials = delegatedCred;
    }

    static Krb5InitCredential getInstance(GSSCaller caller, Krb5NameElement name,
                                   int initLifetime)
        throws GSSException {

        KerberosTicket tgt = getTgt(caller, name, initLifetime);
        if (tgt == null)
            throw new GSSException(GSSException.NO_CRED, -1,
                                   "Failed to find any Kerberos tgt");

        if (name == null) {
            String fullName = tgt.getClient().getName();
            name = Krb5NameElement.getInstance(fullName,
                                       Krb5MechFactory.NT_GSS_KRB5_PRINCIPAL);
        }

        KerberosPrincipal clientAlias = KerberosSecrets
                .getJavaxSecurityAuthKerberosAccess()
                .kerberosTicketGetClientAlias(tgt);
        KerberosPrincipal serverAlias = KerberosSecrets
                .getJavaxSecurityAuthKerberosAccess()
                .kerberosTicketGetServerAlias(tgt);
        Krb5InitCredential result = new Krb5InitCredential(name,
                                      tgt.getEncoded(),
                                      tgt.getClient(),
                                      clientAlias,
                                      tgt.getServer(),
                                      serverAlias,
                                      tgt.getSessionKey().getEncoded(),
                                      tgt.getSessionKeyType(),
                                      tgt.getFlags(),
                                      tgt.getAuthTime(),
                                      tgt.getStartTime(),
                                      tgt.getEndTime(),
                                      tgt.getRenewTill(),
                                      tgt.getClientAddresses());
        result.proxyTicket = KerberosSecrets.getJavaxSecurityAuthKerberosAccess().
            kerberosTicketGetProxy(tgt);
        return result;
    }

    static Krb5InitCredential getInstance(Krb5NameElement name,
                                   Credentials delegatedCred)
        throws GSSException {

        EncryptionKey sessionKey = delegatedCred.getSessionKey();

        /*
         * all of the following data is optional in a KRB-CRED
         * messages. This check for each field.
         */

        PrincipalName cPrinc = delegatedCred.getClient();
        PrincipalName cAPrinc = delegatedCred.getClientAlias();
        PrincipalName sPrinc = delegatedCred.getServer();
        PrincipalName sAPrinc = delegatedCred.getServerAlias();

        KerberosPrincipal client = null;
        KerberosPrincipal clientAlias = null;
        KerberosPrincipal server = null;
        KerberosPrincipal serverAlias = null;

        Krb5NameElement credName = null;

        if (cPrinc != null) {
            String fullName = cPrinc.getName();
            credName = Krb5NameElement.getInstance(fullName,
                               Krb5MechFactory.NT_GSS_KRB5_PRINCIPAL);
            client =  new KerberosPrincipal(fullName);
        }

        if (cAPrinc != null) {
            clientAlias = new KerberosPrincipal(cAPrinc.getName());
        }

        // XXX Compare name to credName

        if (sPrinc != null) {
            server =
                new KerberosPrincipal(sPrinc.getName(),
                                        KerberosPrincipal.KRB_NT_SRV_INST);
        }

        if (sAPrinc != null) {
            serverAlias = new KerberosPrincipal(sAPrinc.getName());
        }

        return new Krb5InitCredential(credName,
                                      delegatedCred,
                                      delegatedCred.getEncoded(),
                                      client,
                                      clientAlias,
                                      server,
                                      serverAlias,
                                      sessionKey.getBytes(),
                                      sessionKey.getEType(),
                                      delegatedCred.getFlags(),
                                      delegatedCred.getAuthTime(),
                                      delegatedCred.getStartTime(),
                                      delegatedCred.getEndTime(),
                                      delegatedCred.getRenewTill(),
                                      delegatedCred.getClientAddresses());
    }

    /**
     * Returns the principal name for this credential. The name
     * is in mechanism specific format.
     *
     * @return GSSNameSpi representing principal name of this credential
     * @exception GSSException may be thrown
     */
    public final GSSNameSpi getName() throws GSSException {
        return name;
    }

    /**
     * Returns the init lifetime remaining.
     *
     * @return the init lifetime remaining in seconds
     * @exception GSSException may be thrown
     */
    public int getInitLifetime() throws GSSException {
        Date d = getEndTime();
        if (d == null) {
            return 0;
        }
        long retVal = d.getTime() - System.currentTimeMillis();
        return (int)(retVal/1000);
    }

    /**
     * Returns the accept lifetime remaining.
     *
     * @return the accept lifetime remaining in seconds
     * @exception GSSException may be thrown
     */
    public int getAcceptLifetime() throws GSSException {
        return 0;
    }

    public boolean isInitiatorCredential() throws GSSException {
        return true;
    }

    public boolean isAcceptorCredential() throws GSSException {
        return false;
    }

    /**
     * Returns the oid representing the underlying credential
     * mechanism oid.
     *
     * @return the Oid for this credential mechanism
     * @exception GSSException may be thrown
     */
    public final Oid getMechanism() {
        return Krb5MechFactory.GSS_KRB5_MECH_OID;
    }

    public final java.security.Provider getProvider() {
        return Krb5MechFactory.PROVIDER;
    }


    /**
     * Returns a sun.security.krb5.Credentials instance so that it maybe
     * used in that package for th Kerberos protocol.
     */
    Credentials getKrb5Credentials() {
        return krb5Credentials;
    }

    /*
     * XXX Call to this.refresh() should refresh the locally cached copy
     * of krb5Credentials also.
     */

    /**
     * Called to invalidate this credential element.
     */
    public void dispose() throws GSSException {
        try {
            destroy();
        } catch (javax.security.auth.DestroyFailedException e) {
            GSSException gssException =
                new GSSException(GSSException.FAILURE, -1,
                 "Could not destroy credentials - " + e.getMessage());
            gssException.initCause(e);
        }
    }

    // XXX call to this.destroy() should destroy the locally cached copy
    // of krb5Credentials and then call super.destroy().

    @SuppressWarnings("removal")
    private static KerberosTicket getTgt(GSSCaller caller, Krb5NameElement name,
                                                 int initLifetime)
        throws GSSException {

        final String clientPrincipal;

        /*
         * Find the TGT for the realm that the client is in. If the client
         * name is not available, then use the default realm.
         */
        if (name != null) {
            clientPrincipal = (name.getKrb5PrincipalName()).getName();
        } else {
            clientPrincipal = null;
        }

        final AccessControlContext acc = AccessController.getContext();

        try {
            final GSSCaller realCaller = (caller == GSSCaller.CALLER_UNKNOWN)
                                   ? GSSCaller.CALLER_INITIATE
                                   : caller;
            return AccessController.doPrivileged(
                new PrivilegedExceptionAction<KerberosTicket>() {
                public KerberosTicket run() throws Exception {
                    // It's OK to use null as serverPrincipal. TGT is almost
                    // the first ticket for a principal and we use list.
                    return Krb5Util.getInitialTicket(
                        realCaller,
                        clientPrincipal, acc);
                        }});
        } catch (PrivilegedActionException e) {
            GSSException ge =
                new GSSException(GSSException.NO_CRED, -1,
                    "Attempt to obtain new INITIATE credentials failed!" +
                    " (" + e.getMessage() + ")");
            ge.initCause(e.getException());
            throw ge;
        }
    }

    @Override
    public GSSCredentialSpi impersonate(GSSNameSpi name) throws GSSException {
        try {
            Krb5NameElement kname = (Krb5NameElement)name;
            Credentials newCred = Credentials.acquireS4U2selfCreds(
                    kname.getKrb5PrincipalName(), krb5Credentials);
            return new Krb5ProxyCredential(this, kname, newCred.getTicket());
        } catch (IOException | KrbException ke) {
            GSSException ge =
                new GSSException(GSSException.FAILURE, -1,
                    "Attempt to obtain S4U2self credentials failed!");
            ge.initCause(ke);
            throw ge;
        }
    }
}
