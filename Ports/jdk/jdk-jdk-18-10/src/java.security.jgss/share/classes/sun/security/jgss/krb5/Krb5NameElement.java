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
import sun.security.jgss.spi.*;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.Realm;
import sun.security.krb5.KrbException;

import javax.security.auth.kerberos.ServicePermission;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Provider;
import java.util.Locale;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Implements the GSSNameSpi for the krb5 mechanism.
 *
 * @author Mayank Upadhyay
 */
public class Krb5NameElement
    implements GSSNameSpi {

    private PrincipalName krb5PrincipalName;

    private String gssNameStr = null;
    private Oid gssNameType = null;

    private Krb5NameElement(PrincipalName principalName,
                            String gssNameStr,
                            Oid gssNameType) {
        this.krb5PrincipalName = principalName;
        this.gssNameStr = gssNameStr;
        this.gssNameType = gssNameType;
    }

    /**
     * Instantiates a new Krb5NameElement object. Internally it stores the
     * information provided by the input parameters so that they may later
     * be used for output when a printable representaion of this name is
     * needed in GSS-API format rather than in Kerberos format.
     *
     */
    static Krb5NameElement getInstance(String gssNameStr, Oid gssNameType)
        throws GSSException {

        /*
         * A null gssNameType implies that the mechanism default
         * Krb5MechFactory.NT_GSS_KRB5_PRINCIPAL be used.
         */
        if (gssNameType == null)
            gssNameType = Krb5MechFactory.NT_GSS_KRB5_PRINCIPAL;
        else
            if (!gssNameType.equals(GSSName.NT_USER_NAME) &&
                !gssNameType.equals(GSSName.NT_HOSTBASED_SERVICE) &&
                !gssNameType.equals(Krb5MechFactory.NT_GSS_KRB5_PRINCIPAL) &&
                !gssNameType.equals(GSSName.NT_EXPORT_NAME))
                throw new GSSException(GSSException.BAD_NAMETYPE, -1,
                                       gssNameType.toString()
                                       +" is an unsupported nametype");

        PrincipalName principalName;
        try {

            if (gssNameType.equals(GSSName.NT_EXPORT_NAME) ||
                gssNameType.equals(Krb5MechFactory.NT_GSS_KRB5_PRINCIPAL)) {
                principalName = new PrincipalName(gssNameStr,
                                  PrincipalName.KRB_NT_PRINCIPAL);
            } else {

                String[] components = getComponents(gssNameStr);

                /*
                 * We have forms of GSS name strings that can come in:
                 *
                 * 1. names of the form "foo" with just one
                 * component. (This might include a "@" but only in escaped
                 * form like "\@")
                 * 2. names of the form "foo@bar" with two components
                 *
                 * The nametypes that are accepted are NT_USER_NAME, and
                 * NT_HOSTBASED_SERVICE.
                 */

                if (gssNameType.equals(GSSName.NT_USER_NAME))
                    principalName = new PrincipalName(gssNameStr,
                                    PrincipalName.KRB_NT_PRINCIPAL);
                else {
                    String hostName = null;
                    String service = components[0];
                    if (components.length >= 2)
                        hostName = components[1];

                    String principal = getHostBasedInstance(service, hostName);
                    principalName = new PrincipalName(principal,
                            PrincipalName.KRB_NT_SRV_HST);
                }
            }

        } catch (KrbException e) {
            throw new GSSException(GSSException.BAD_NAME, -1, e.getMessage());
        }

        if (principalName.isRealmDeduced() && !Realm.AUTODEDUCEREALM) {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                try {
                    sm.checkPermission(new ServicePermission(
                            "@" + principalName.getRealmAsString(), "-"));
                } catch (SecurityException se) {
                    // Do not chain the actual exception to hide info
                    throw new GSSException(GSSException.FAILURE);
                }
            }
        }
        return new Krb5NameElement(principalName, gssNameStr, gssNameType);
    }

    public static Krb5NameElement getInstance(PrincipalName principalName) {
        return new Krb5NameElement(principalName,
                                   principalName.getName(),
                                   Krb5MechFactory.NT_GSS_KRB5_PRINCIPAL);
    }

    private static String[] getComponents(String gssNameStr)
        throws GSSException {

        String[] retVal;

        // XXX Perhaps provide this parsing code in PrincipalName

        // Look for @ as in service@host
        // Assumes host name will not have an escaped '@'
        int separatorPos = gssNameStr.lastIndexOf('@', gssNameStr.length());

        // Not really a separator if it is escaped. Then this is just part
        // of the principal name or service name
        if ((separatorPos > 0) &&
                (gssNameStr.charAt(separatorPos-1) == '\\')) {
            // Is the `\` character escaped itself?
            if ((separatorPos - 2 < 0) ||
                (gssNameStr.charAt(separatorPos-2) != '\\'))
                separatorPos = -1;
        }

        if (separatorPos > 0) {
            String serviceName = gssNameStr.substring(0, separatorPos);
            String hostName = gssNameStr.substring(separatorPos+1);
            retVal = new String[] { serviceName, hostName};
        } else {
            retVal = new String[] {gssNameStr};
        }

        return retVal;

    }

    private static String getHostBasedInstance(String serviceName,
                                               String hostName)
        throws GSSException {
            StringBuffer temp = new StringBuffer(serviceName);

            try {
                // A lack of "@" defaults to the service being on the local
                // host as per RFC 2743
                // XXX Move this part into JGSS framework
                if (hostName == null)
                    hostName = InetAddress.getLocalHost().getHostName();

            } catch (UnknownHostException e) {
                // use hostname as it is
            }
            hostName = hostName.toLowerCase(Locale.ENGLISH);

            temp = temp.append('/').append(hostName);
            return temp.toString();
    }

    public final PrincipalName getKrb5PrincipalName() {
        return krb5PrincipalName;
    }

    /**
     * Equal method for the GSSNameSpi objects.
     * If either name denotes an anonymous principal, the call should
     * return false.
     *
     * @param other to be compared with
     * @return true if they both refer to the same entity, else false
     * @exception GSSException with major codes of BAD_NAMETYPE,
     *  BAD_NAME, FAILURE
     */
    public boolean equals(GSSNameSpi other) throws GSSException {

        if (other == this)
            return true;

        if (other instanceof Krb5NameElement) {
                Krb5NameElement that = (Krb5NameElement) other;
                return (this.krb5PrincipalName.getName().equals(
                            that.krb5PrincipalName.getName()));
        }
        return false;
    }

    /**
     * Compares this <code>GSSNameSpi</code> object to another Object
     * that might be a <code>GSSNameSpi</code>. The behaviour is exactly
     * the same as in {@link #equals(GSSNameSpi) equals} except that
     * no GSSException is thrown; instead, false will be returned in the
     * situation where an error occurs.
     *
     * @param another the object to be compared to
     * @return true if they both refer to the same entity, else false
     * @see #equals(GSSNameSpi)
     */
    public boolean equals(Object another) {
        if (this == another) {
            return true;
        }

        try {
            if (another instanceof Krb5NameElement)
                 return equals((Krb5NameElement) another);
        } catch (GSSException e) {
            // ignore exception
        }
        return false;
    }

    /**
     * Returns a hashcode value for this GSSNameSpi.
     *
     * @return a hashCode value
     */
    public int hashCode() {
        return 37 * 17 + krb5PrincipalName.getName().hashCode();
    }


    /**
     * Returns the principal name in the form user@REALM or
     * host/service@REALM but with the following constraints that are
     * imposed by RFC 1964:
     * <pre>
     *  (1) all occurrences of the characters `@`,  `/`, and `\` within
     *   principal components or realm names shall be quoted with an
     *   immediately-preceding `\`.
     *
     *   (2) all occurrences of the null, backspace, tab, or newline
     *   characters within principal components or realm names will be
     *   represented, respectively, with `\0`, `\b`, `\t`, or `\n`.
     *
     *   (3) the `\` quoting character shall not be emitted within an
     *   exported name except to accommodate cases (1) and (2).
     * </pre>
     */
    public byte[] export() throws GSSException {
        // XXX Apply the above constraints.
        return krb5PrincipalName.getName().getBytes(UTF_8);
    }

    /**
     * Get the mechanism type that this NameElement corresponds to.
     *
     * @return the Oid of the mechanism type
     */
    public Oid getMechanism() {
        return (Krb5MechFactory.GSS_KRB5_MECH_OID);
    }

    /**
     * Returns a string representation for this name. The printed
     * name type can be obtained by calling getStringNameType().
     *
     * @return string form of this name
     * @see #getStringNameType()
     * @overrides Object#toString
     */
    public String toString() {
        return (gssNameStr);
        // For testing: return (super.toString());
    }

    /**
     * Returns the name type oid.
     */
    public Oid getGSSNameType() {
        return (gssNameType);
    }

    /**
     * Returns the oid describing the format of the printable name.
     *
     * @return the Oid for the format of the printed name
     */
    public Oid getStringNameType() {
        // XXX For NT_EXPORT_NAME return a different name type. Infact,
        // don't even store NT_EXPORT_NAME in the cons.
        return (gssNameType);
    }

    /**
     * Indicates if this name object represents an Anonymous name.
     */
    public boolean isAnonymousName() {
        return (gssNameType.equals(GSSName.NT_ANONYMOUS));
    }

    public Provider getProvider() {
        return Krb5MechFactory.PROVIDER;
    }

}
