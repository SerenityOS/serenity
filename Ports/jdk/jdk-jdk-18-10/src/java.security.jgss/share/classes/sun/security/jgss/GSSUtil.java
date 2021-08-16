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

package sun.security.jgss;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.kerberos.KerberosKey;
import org.ietf.jgss.*;
import sun.security.jgss.spi.GSSNameSpi;
import sun.security.jgss.spi.GSSCredentialSpi;
import sun.security.action.GetPropertyAction;
import sun.security.jgss.krb5.Krb5NameElement;
import sun.security.jgss.spnego.SpNegoCredElement;
import java.util.Set;
import java.util.HashSet;
import java.util.Vector;
import java.util.Iterator;
import java.security.AccessController;
import java.security.AccessControlContext;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import sun.security.action.GetBooleanAction;
import sun.security.util.ConsoleCallbackHandler;

/**
 * The GSSUtilImplementation that knows how to work with the internals of
 * the GSS-API.
 */
public class GSSUtil {

    public static final Oid GSS_KRB5_MECH_OID =
                GSSUtil.createOid("1.2.840.113554.1.2.2");
    public static final Oid GSS_KRB5_MECH_OID2 =
                GSSUtil.createOid("1.3.5.1.5.2");
    public static final Oid GSS_KRB5_MECH_OID_MS =
                GSSUtil.createOid("1.2.840.48018.1.2.2");

    public static final Oid GSS_SPNEGO_MECH_OID =
                GSSUtil.createOid("1.3.6.1.5.5.2");

    public static final Oid NT_GSS_KRB5_PRINCIPAL =
                GSSUtil.createOid("1.2.840.113554.1.2.2.1");

    static final boolean DEBUG =
            GetBooleanAction.privilegedGetProperty("sun.security.jgss.debug");

    static void debug(String message) {
        if (DEBUG) {
            assert(message != null);
            System.out.println(message);
        }
    }

    // NOTE: this method is only for creating Oid objects with
    // known to be valid <code>oidStr</code> given it ignores
    // the GSSException
    public static Oid createOid(String oidStr) {
        try {
            return new Oid(oidStr);
        } catch (GSSException e) {
            debug("Ignored invalid OID: " + oidStr);
            return null;
        }
    }

    public static boolean isSpNegoMech(Oid oid) {
        return (GSS_SPNEGO_MECH_OID.equals(oid));
    }

    public static boolean isKerberosMech(Oid oid) {
        return (GSS_KRB5_MECH_OID.equals(oid) ||
                GSS_KRB5_MECH_OID2.equals(oid) ||
                GSS_KRB5_MECH_OID_MS.equals(oid));

    }

    public static String getMechStr(Oid oid) {
        if (isSpNegoMech(oid)) {
            return "SPNEGO";
        } else if (isKerberosMech(oid)) {
            return "Kerberos V5";
        } else {
            return oid.toString();
        }
    }

    /**
     * Note: The current impl only works with Sun's impl of
     * GSSName and GSSCredential since it depends on package
     * private APIs.
     */
    public static Subject getSubject(GSSName name,
                                     GSSCredential creds) {

        HashSet<Object> privCredentials = null;
        HashSet<Object> pubCredentials = new HashSet<Object>(); // empty Set

        Set<GSSCredentialSpi> gssCredentials = null;

        Set<KerberosPrincipal> krb5Principals =
                                new HashSet<KerberosPrincipal>();

        if (name instanceof GSSNameImpl) {
            try {
                GSSNameSpi ne = ((GSSNameImpl) name).getElement
                    (GSS_KRB5_MECH_OID);
                String krbName = ne.toString();
                if (ne instanceof Krb5NameElement) {
                    krbName =
                        ((Krb5NameElement) ne).getKrb5PrincipalName().getName();
                }
                KerberosPrincipal krbPrinc = new KerberosPrincipal(krbName);
                krb5Principals.add(krbPrinc);
            } catch (GSSException ge) {
                debug("Skipped name " + name + " due to " + ge);
            }
        }

        if (creds instanceof GSSCredentialImpl) {
            gssCredentials = ((GSSCredentialImpl) creds).getElements();
            privCredentials = new HashSet<Object>(gssCredentials.size());
            populateCredentials(privCredentials, gssCredentials);
        } else {
            privCredentials = new HashSet<Object>(); // empty Set
        }
        debug("Created Subject with the following");
        debug("principals=" + krb5Principals);
        debug("public creds=" + pubCredentials);
        debug("private creds=" + privCredentials);

        return new Subject(false, krb5Principals, pubCredentials,
                           privCredentials);

    }

    /**
     * Populates the set credentials with elements from gssCredentials. At
     * the same time, it converts any subclasses of KerberosTicket
     * into KerberosTicket instances and any subclasses of KerberosKey into
     * KerberosKey instances. (It is not desirable to expose the customer
     * to sun.security.jgss.krb5.Krb5InitCredential which extends
     * KerberosTicket and sun.security.jgss.krb5.Kbr5AcceptCredential which
     * extends KerberosKey.)
     */
    private static void populateCredentials(Set<Object> credentials,
                                            Set<?> gssCredentials) {

        Object cred;

        Iterator<?> elements = gssCredentials.iterator();
        while (elements.hasNext()) {

            cred = elements.next();

            // Retrieve the internal cred out of SpNegoCredElement
            if (cred instanceof SpNegoCredElement) {
                cred = ((SpNegoCredElement) cred).getInternalCred();
            }

            if (cred instanceof KerberosTicket) {
                if (!cred.getClass().getName().equals
                    ("javax.security.auth.kerberos.KerberosTicket")) {
                    KerberosTicket tempTkt = (KerberosTicket) cred;
                    cred = new KerberosTicket(tempTkt.getEncoded(),
                                              tempTkt.getClient(),
                                              tempTkt.getServer(),
                                              tempTkt.getSessionKey().getEncoded(),
                                              tempTkt.getSessionKeyType(),
                                              tempTkt.getFlags(),
                                              tempTkt.getAuthTime(),
                                              tempTkt.getStartTime(),
                                              tempTkt.getEndTime(),
                                              tempTkt.getRenewTill(),
                                              tempTkt.getClientAddresses());
                }
                credentials.add(cred);
            } else if (cred instanceof KerberosKey) {
                if (!cred.getClass().getName().equals
                    ("javax.security.auth.kerberos.KerberosKey")) {
                    KerberosKey tempKey = (KerberosKey) cred;
                    cred = new KerberosKey(tempKey.getPrincipal(),
                                           tempKey.getEncoded(),
                                           tempKey.getKeyType(),
                                           tempKey.getVersionNumber());
                }
                credentials.add(cred);
            } else {
                // Ignore non-KerberosTicket and non-KerberosKey elements
                debug("Skipped cred element: " + cred);
            }
        }
    }

    /**
     * Authenticate using the login module from the specified
     * configuration entry.
     *
     * @param caller the caller of JAAS Login
     * @param mech the mech to be used
     * @return the authenticated subject
     */
    public static Subject login(GSSCaller caller, Oid mech) throws LoginException {

        CallbackHandler cb = null;
        if (caller instanceof HttpCaller) {
            cb = new sun.net.www.protocol.http.spnego.NegotiateCallbackHandler(
                    ((HttpCaller)caller).info());
        } else {
            String defaultHandler = java.security.Security
                    .getProperty("auth.login.defaultCallbackHandler");
            // get the default callback handler
            if ((defaultHandler != null) && (defaultHandler.length() != 0)) {
                cb = null;
            } else {
                cb = new ConsoleCallbackHandler();
            }
        }

        // New instance of LoginConfigImpl must be created for each login,
        // since the entry name is not passed as the first argument, but
        // generated with caller and mech inside LoginConfigImpl
        LoginContext lc = new LoginContext("", null, cb,
                new LoginConfigImpl(caller, mech));
        lc.login();
        return lc.getSubject();
    }

    /**
     * Determines if the application doesn't mind if the mechanism obtains
     * the required credentials from outside of the current Subject. Our
     * Kerberos v5 mechanism would do a JAAS login on behalf of the
     * application if this were the case.
     *
     * The application indicates this by explicitly setting the system
     * property javax.security.auth.useSubjectCredsOnly to false.
     */
    public static boolean useSubjectCredsOnly(GSSCaller caller) {

        String propValue = GetPropertyAction
                .privilegedGetProperty("javax.security.auth.useSubjectCredsOnly");

        // Invalid values should be ignored and the default assumed.
        if (caller instanceof HttpCaller) {
            // Default for HTTP/SPNEGO is false.
            return "true".equalsIgnoreCase(propValue);
        } else {
            // Default for JGSS is true.
            return !("false".equalsIgnoreCase(propValue));
        }
    }

    /**
     * Determines the SPNEGO interoperability mode with Microsoft;
     * by default it is set to true.
     *
     * To disable it, the application indicates this by explicitly setting
     * the system property sun.security.spnego.interop to false.
     */
    public static boolean useMSInterop() {
        /*
         * Don't use GetBooleanAction because the default value in the JRE
         * (when this is unset) has to treated as true.
         */
        String propValue = GetPropertyAction
                .privilegedGetProperty("sun.security.spnego.msinterop", "true");
        /*
         * This property has to be explicitly set to "false". Invalid
         * values should be ignored and the default "true" assumed.
         */
        return (!propValue.equalsIgnoreCase("false"));
    }

    /**
     * Searches the private credentials of current Subject with the
     * specified criteria and returns the matching GSSCredentialSpi
     * object out of Sun's impl of GSSCredential. Returns null if
     * no Subject present or a Vector which contains 0 or more
     * matching GSSCredentialSpi objects.
     */
    public static <T extends GSSCredentialSpi> Vector<T>
            searchSubject(final GSSNameSpi name,
                          final Oid mech,
                          final boolean initiate,
                          final Class<? extends T> credCls) {
        debug("Search Subject for " + getMechStr(mech) +
              (initiate? " INIT" : " ACCEPT") + " cred (" +
              (name == null? "<<DEF>>" : name.toString()) + ", " +
              credCls.getName() + ")");
        @SuppressWarnings("removal")
        final AccessControlContext acc = AccessController.getContext();
        try {
            @SuppressWarnings("removal")
            Vector<T> creds =
                AccessController.doPrivileged
                (new PrivilegedExceptionAction<Vector<T>>() {
                    public Vector<T> run() throws Exception {
                        Subject accSubj = Subject.getSubject(acc);
                        Vector<T> result = null;
                        if (accSubj != null) {
                            result = new Vector<T>();
                            Iterator<GSSCredentialImpl> iterator =
                                accSubj.getPrivateCredentials
                                (GSSCredentialImpl.class).iterator();
                            while (iterator.hasNext()) {
                                GSSCredentialImpl cred = iterator.next();
                                debug("...Found cred" + cred);
                                try {
                                    GSSCredentialSpi ce =
                                        cred.getElement(mech, initiate);
                                    debug("......Found element: " + ce);
                                    if (ce.getClass().equals(credCls) &&
                                        (name == null ||
                                         name.equals((Object) ce.getName()))) {
                                        result.add(credCls.cast(ce));
                                    } else {
                                        debug("......Discard element");
                                    }
                                } catch (GSSException ge) {
                                    debug("...Discard cred (" + ge + ")");
                                }
                            }
                        } else debug("No Subject");
                        return result;
                    }
                });
            return creds;
        } catch (PrivilegedActionException pae) {
            debug("Unexpected exception when searching Subject:");
            if (DEBUG) pae.printStackTrace();
            return null;
        }
    }
}
