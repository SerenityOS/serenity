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

import java.io.IOException;
import org.ietf.jgss.*;
import sun.security.jgss.GSSCaller;
import sun.security.jgss.spi.*;
import sun.security.krb5.*;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.AccessController;
import java.security.AccessControlContext;
import javax.security.auth.DestroyFailedException;

/**
 * Implements the krb5 acceptor credential element.
 *
 * @author Mayank Upadhyay
 * @since 1.4
 */
public class Krb5AcceptCredential
    implements Krb5CredElement {

    private final Krb5NameElement name;
    private final ServiceCreds screds;

    private Krb5AcceptCredential(Krb5NameElement name, ServiceCreds creds) {
        /*
         * Initialize this instance with the data from the acquired
         * KerberosKey. This class needs to be a KerberosKey too
         * hence we can't just store a reference.
         */

        this.name = name;
        this.screds = creds;
    }

    @SuppressWarnings("removal")
    static Krb5AcceptCredential getInstance(final GSSCaller caller, Krb5NameElement name)
        throws GSSException {

        final String serverPrinc = (name == null? null:
            name.getKrb5PrincipalName().getName());
        final AccessControlContext acc = AccessController.getContext();

        ServiceCreds creds = null;
        try {
            creds = AccessController.doPrivileged(
                        new PrivilegedExceptionAction<ServiceCreds>() {
                public ServiceCreds run() throws Exception {
                    return Krb5Util.getServiceCreds(
                        caller == GSSCaller.CALLER_UNKNOWN ? GSSCaller.CALLER_ACCEPT: caller,
                        serverPrinc, acc);
                }});
        } catch (PrivilegedActionException e) {
            GSSException ge =
                new GSSException(GSSException.NO_CRED, -1,
                    "Attempt to obtain new ACCEPT credentials failed!");
            ge.initCause(e.getException());
            throw ge;
        }

        if (creds == null)
            throw new GSSException(GSSException.NO_CRED, -1,
                                   "Failed to find any Kerberos credentials");

        if (name == null) {
            String fullName = creds.getName();
            if (fullName != null) {
                name = Krb5NameElement.getInstance(fullName,
                                       Krb5MechFactory.NT_GSS_KRB5_PRINCIPAL);
            }
        }

        return new Krb5AcceptCredential(name, creds);
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
        return 0;
    }

    /**
     * Returns the accept lifetime remaining.
     *
     * @return the accept lifetime remaining in seconds
     * @exception GSSException may be thrown
     */
    public int getAcceptLifetime() throws GSSException {
        return GSSCredential.INDEFINITE_LIFETIME;
    }

    public boolean isInitiatorCredential() throws GSSException {
        return false;
    }

    public boolean isAcceptorCredential() throws GSSException {
        return true;
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

    public EncryptionKey[] getKrb5EncryptionKeys(PrincipalName princ) {
        return screds.getEKeys(princ);
    }

    /**
     * Called to invalidate this credential element.
     */
    public void dispose() throws GSSException {
        try {
            destroy();
        } catch (DestroyFailedException e) {
            GSSException gssException =
                new GSSException(GSSException.FAILURE, -1,
                 "Could not destroy credentials - " + e.getMessage());
            gssException.initCause(e);
        }
    }

    /**
     * Destroys the locally cached EncryptionKey value and then calls
     * destroy in the base class.
     */
    public void destroy() throws DestroyFailedException {
        screds.destroy();
    }

    /**
     * Impersonation is only available on the initiator side. The
     * service must starts as an initiator to get an initial TGT to complete
     * the S4U2self protocol.
     */
    @Override
    public GSSCredentialSpi impersonate(GSSNameSpi name) throws GSSException {
        Credentials cred = screds.getInitCred();
        if (cred != null) {
            return Krb5InitCredential.getInstance(this.name, cred)
                    .impersonate(name);
        } else {
            throw new GSSException(GSSException.FAILURE, -1,
                "Only an initiate credentials can impersonate");
        }
    }
}
