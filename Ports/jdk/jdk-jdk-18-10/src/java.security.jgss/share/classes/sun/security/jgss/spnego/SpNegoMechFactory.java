/*
 * Copyright (c) 2005, 2009, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.jgss.spnego;

import org.ietf.jgss.*;
import sun.security.jgss.*;
import sun.security.jgss.spi.*;
import sun.security.jgss.krb5.Krb5MechFactory;
import sun.security.jgss.krb5.Krb5InitCredential;
import sun.security.jgss.krb5.Krb5AcceptCredential;
import sun.security.jgss.krb5.Krb5NameElement;
import java.security.Provider;
import java.util.Vector;

/**
 * SpNego Mechanism plug in for JGSS
 * This is the properties object required by the JGSS framework.
 * All mechanism specific information is defined here.
 *
 * @author Seema Malkani
 * @since 1.6
 */

public final class SpNegoMechFactory implements MechanismFactory {

    static final Provider PROVIDER =
        new sun.security.jgss.SunProvider();

    static final Oid GSS_SPNEGO_MECH_OID =
        GSSUtil.createOid("1.3.6.1.5.5.2");

    private static Oid[] nameTypes =
        new Oid[] { GSSName.NT_USER_NAME,
                        GSSName.NT_HOSTBASED_SERVICE,
                        GSSName.NT_EXPORT_NAME};

    // The default underlying mech of SPNEGO, must not be SPNEGO itself.
    private static final Oid DEFAULT_SPNEGO_MECH_OID =
            ProviderList.DEFAULT_MECH_OID.equals(GSS_SPNEGO_MECH_OID)?
                GSSUtil.GSS_KRB5_MECH_OID:
                ProviderList.DEFAULT_MECH_OID;

    // Use an instance of a GSSManager whose provider list
    // does not include native provider
    final GSSManagerImpl manager;
    final Oid[] availableMechs;

    private static SpNegoCredElement getCredFromSubject(GSSNameSpi name,
                                                        boolean initiate)
        throws GSSException {
        Vector<SpNegoCredElement> creds =
            GSSUtil.searchSubject(name, GSS_SPNEGO_MECH_OID,
                initiate, SpNegoCredElement.class);

        SpNegoCredElement result = ((creds == null || creds.isEmpty()) ?
                                    null : creds.firstElement());

        // Force permission check before returning the cred to caller
        if (result != null) {
            GSSCredentialSpi cred = result.getInternalCred();
            if (GSSUtil.isKerberosMech(cred.getMechanism())) {
                if (initiate) {
                    Krb5InitCredential krbCred = (Krb5InitCredential) cred;
                    Krb5MechFactory.checkInitCredPermission
                        ((Krb5NameElement) krbCred.getName());
                } else {
                    Krb5AcceptCredential krbCred = (Krb5AcceptCredential) cred;
                    Krb5MechFactory.checkAcceptCredPermission
                        ((Krb5NameElement) krbCred.getName(), name);
                }
            }
        }
        return result;
    }

    public SpNegoMechFactory() {
        this(GSSCaller.CALLER_UNKNOWN);
    }

    public SpNegoMechFactory(GSSCaller caller) {
        manager = new GSSManagerImpl(caller, false);
        Oid[] mechs = manager.getMechs();
        availableMechs = new Oid[mechs.length-1];
        for (int i = 0, j = 0; i < mechs.length; i++) {
            // Skip SpNego mechanism
            if (!mechs[i].equals(GSS_SPNEGO_MECH_OID)) {
                availableMechs[j++] = mechs[i];
            }
        }
        // Move the preferred mech to first place
        for (int i=0; i<availableMechs.length; i++) {
            if (availableMechs[i].equals(DEFAULT_SPNEGO_MECH_OID)) {
                if (i != 0) {
                    availableMechs[i] = availableMechs[0];
                    availableMechs[0] = DEFAULT_SPNEGO_MECH_OID;
                }
                break;
            }
        }
    }

    public GSSNameSpi getNameElement(String nameStr, Oid nameType)
            throws GSSException {
        return manager.getNameElement(
                nameStr, nameType, DEFAULT_SPNEGO_MECH_OID);
    }

    public GSSNameSpi getNameElement(byte[] name, Oid nameType)
            throws GSSException {
        return manager.getNameElement(name, nameType, DEFAULT_SPNEGO_MECH_OID);
    }

    public GSSCredentialSpi getCredentialElement(GSSNameSpi name,
           int initLifetime, int acceptLifetime,
           int usage) throws GSSException {

        SpNegoCredElement credElement = getCredFromSubject
            (name, (usage != GSSCredential.ACCEPT_ONLY));

        if (credElement == null) {
            // get CredElement for the default Mechanism
            credElement = new SpNegoCredElement
                (manager.getCredentialElement(name, initLifetime,
                acceptLifetime, null, usage));
        }
        return credElement;
    }

    public GSSContextSpi getMechanismContext(GSSNameSpi peer,
                             GSSCredentialSpi myInitiatorCred, int lifetime)
        throws GSSException {
        // get SpNego mechanism context
        if (myInitiatorCred == null) {
            myInitiatorCred = getCredFromSubject(null, true);
        } else if (!(myInitiatorCred instanceof SpNegoCredElement)) {
            // convert to SpNegoCredElement
            SpNegoCredElement cred = new SpNegoCredElement(myInitiatorCred);
            return new SpNegoContext(this, peer, cred, lifetime);
        }
        return new SpNegoContext(this, peer, myInitiatorCred, lifetime);
    }

    public GSSContextSpi getMechanismContext(GSSCredentialSpi myAcceptorCred)
        throws GSSException {
        // get SpNego mechanism context
        if (myAcceptorCred == null) {
            myAcceptorCred = getCredFromSubject(null, false);
        } else if (!(myAcceptorCred instanceof SpNegoCredElement)) {
            // convert to SpNegoCredElement
            SpNegoCredElement cred = new SpNegoCredElement(myAcceptorCred);
            return new SpNegoContext(this, cred);
        }
        return new SpNegoContext(this, myAcceptorCred);
    }

    public GSSContextSpi getMechanismContext(byte[] exportedContext)
        throws GSSException {
        // get SpNego mechanism context
        return new SpNegoContext(this, exportedContext);
    }

    public final Oid getMechanismOid() {
        return GSS_SPNEGO_MECH_OID;
    }

    public Provider getProvider() {
        return PROVIDER;
    }

    public Oid[] getNameTypes() {
        // nameTypes is cloned in GSSManager.getNamesForMech
        return nameTypes;
    }
}
