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
import sun.security.jgss.GSSUtil;
import sun.security.jgss.GSSCaller;
import sun.security.jgss.spi.*;
import javax.security.auth.kerberos.ServicePermission;
import java.security.Provider;
import java.util.Vector;

/**
 * Krb5 Mechanism plug in for JGSS
 * This is the properties object required by the JGSS framework.
 * All mechanism specific information is defined here.
 *
 * @author Mayank Upadhyay
 */

public final class Krb5MechFactory implements MechanismFactory {

    private static final boolean DEBUG = Krb5Util.DEBUG;

    static final Provider PROVIDER =
        new sun.security.jgss.SunProvider();

    static final Oid GSS_KRB5_MECH_OID =
        createOid("1.2.840.113554.1.2.2");

    static final Oid NT_GSS_KRB5_PRINCIPAL =
        createOid("1.2.840.113554.1.2.2.1");

    private static Oid[] nameTypes =
        new Oid[] { GSSName.NT_USER_NAME,
                        GSSName.NT_HOSTBASED_SERVICE,
                        GSSName.NT_EXPORT_NAME,
                        NT_GSS_KRB5_PRINCIPAL};

    private final GSSCaller caller;

    private static Krb5CredElement getCredFromSubject(GSSNameSpi name,
                                                      boolean initiate)
        throws GSSException {
        Vector<Krb5CredElement> creds =
            GSSUtil.searchSubject(name, GSS_KRB5_MECH_OID, initiate,
                                  (initiate ?
                                   Krb5InitCredential.class :
                                   Krb5AcceptCredential.class));

        Krb5CredElement result = ((creds == null || creds.isEmpty()) ?
                                  null : creds.firstElement());

        // Force permission check before returning the cred to caller
        if (result != null) {
            if (initiate) {
                checkInitCredPermission((Krb5NameElement) result.getName());
            } else {
                checkAcceptCredPermission
                    ((Krb5NameElement) result.getName(), name);
            }
        }
        return result;
    }

    public Krb5MechFactory() {
        this(GSSCaller.CALLER_UNKNOWN);
    }

    public Krb5MechFactory(GSSCaller caller) {
        this.caller = caller;
    }

    public GSSNameSpi getNameElement(String nameStr, Oid nameType)
        throws GSSException {
        return Krb5NameElement.getInstance(nameStr, nameType);
    }

    public GSSNameSpi getNameElement(byte[] name, Oid nameType)
        throws GSSException {
        // At this point, even an exported name is stripped down to safe
        // bytes only
        // XXX Use encoding here
        return Krb5NameElement.getInstance(new String(name), nameType);
    }

    public GSSCredentialSpi getCredentialElement(GSSNameSpi name,
           int initLifetime, int acceptLifetime,
           int usage) throws GSSException {

        if (name != null && !(name instanceof Krb5NameElement)) {
            name = Krb5NameElement.getInstance(name.toString(),
                                       name.getStringNameType());
        }

        Krb5CredElement credElement = getCredFromSubject
            (name, (usage != GSSCredential.ACCEPT_ONLY));

        if (credElement == null) {
            if (usage == GSSCredential.INITIATE_ONLY ||
                usage == GSSCredential.INITIATE_AND_ACCEPT) {
                credElement = Krb5InitCredential.getInstance
                    (caller, (Krb5NameElement) name, initLifetime);
                credElement = Krb5ProxyCredential.tryImpersonation(
                        caller, (Krb5InitCredential)credElement);
                checkInitCredPermission
                    ((Krb5NameElement) credElement.getName());
            } else if (usage == GSSCredential.ACCEPT_ONLY) {
                credElement =
                    Krb5AcceptCredential.getInstance(caller,
                                                     (Krb5NameElement) name);
                checkAcceptCredPermission
                    ((Krb5NameElement) credElement.getName(), name);
            } else
                throw new GSSException(GSSException.FAILURE, -1,
                                       "Unknown usage mode requested");
        }
        return credElement;
    }

    public static void checkInitCredPermission(Krb5NameElement name) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            String realm = (name.getKrb5PrincipalName()).getRealmAsString();
            String tgsPrincipal =
                new String("krbtgt/" + realm + '@' + realm);
            ServicePermission perm =
                new ServicePermission(tgsPrincipal, "initiate");
            try {
                sm.checkPermission(perm);
            } catch (SecurityException e) {
                if (DEBUG) {
                    System.out.println("Permission to initiate" +
                        "kerberos init credential" + e.getMessage());
                }
                throw e;
            }
        }
    }

    public static void checkAcceptCredPermission(Krb5NameElement name,
                                           GSSNameSpi originalName) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null && name != null) {
            ServicePermission perm = new ServicePermission
                (name.getKrb5PrincipalName().getName(), "accept");
            try {
                sm.checkPermission(perm);
            } catch (SecurityException e) {
                if (originalName == null) {
                    // Don't disclose the name of the principal
                    e = new SecurityException("No permission to acquire "
                                      + "Kerberos accept credential");
                    // Don't call e.initCause() with caught exception
                }
                throw e;
            }
        }
    }

    public GSSContextSpi getMechanismContext(GSSNameSpi peer,
                             GSSCredentialSpi myInitiatorCred, int lifetime)
        throws GSSException {
        if (peer != null && !(peer instanceof Krb5NameElement)) {
            peer = Krb5NameElement.getInstance(peer.toString(),
                                       peer.getStringNameType());
        }
        // XXX Convert myInitiatorCred to Krb5CredElement
        if (myInitiatorCred == null) {
            myInitiatorCred = getCredentialElement(null, lifetime, 0,
                GSSCredential.INITIATE_ONLY);
        }
        return new Krb5Context(caller, (Krb5NameElement)peer,
                               (Krb5CredElement)myInitiatorCred, lifetime);
    }

    public GSSContextSpi getMechanismContext(GSSCredentialSpi myAcceptorCred)
        throws GSSException {
        // XXX Convert myAcceptorCred to Krb5CredElement
        if (myAcceptorCred == null) {
            myAcceptorCred = getCredentialElement(null, 0,
                GSSCredential.INDEFINITE_LIFETIME, GSSCredential.ACCEPT_ONLY);
        }
        return new Krb5Context(caller, (Krb5CredElement)myAcceptorCred);
    }

    public GSSContextSpi getMechanismContext(byte[] exportedContext)
        throws GSSException {
        return new Krb5Context(caller, exportedContext);
    }


    public final Oid getMechanismOid() {
        return GSS_KRB5_MECH_OID;
    }

    public Provider getProvider() {
        return PROVIDER;
    }

    public Oid[] getNameTypes() {
        // nameTypes is cloned in GSSManager.getNamesForMech
        return nameTypes;
    }

    private static Oid createOid(String oidStr) {
        Oid retVal = null;
        try {
            retVal = new Oid(oidStr);
        } catch (GSSException e) {
            // Should not happen!
        }
        return retVal;
    }
}
