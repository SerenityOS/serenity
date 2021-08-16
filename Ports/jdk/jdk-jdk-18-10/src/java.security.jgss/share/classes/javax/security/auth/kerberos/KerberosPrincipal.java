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

package javax.security.auth.kerberos;

import java.io.*;
import sun.security.krb5.KrbException;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.Realm;
import sun.security.util.*;

/**
 * This class encapsulates a Kerberos principal.
 *
 * @author Mayank Upadhyay
 * @since 1.4
 */

public final class KerberosPrincipal
    implements java.security.Principal, java.io.Serializable {

    private static final long serialVersionUID = -7374788026156829911L;

    //name types

    /**
     * unknown name type.
     */

    public static final int KRB_NT_UNKNOWN =   0;

    /**
     * user principal name type.
     */

    public static final int KRB_NT_PRINCIPAL = 1;

    /**
     * service and other unique instance (krbtgt) name type.
     */
    public static final int KRB_NT_SRV_INST =  2;

    /**
     * service with host name as instance (telnet, rcommands) name type.
     */

    public static final int KRB_NT_SRV_HST =   3;

    /**
     * service with host as remaining components name type.
     */

    public static final int KRB_NT_SRV_XHST =  4;

    /**
     * unique ID name type.
     */

    public static final int KRB_NT_UID = 5;

    /**
     * Enterprise name (alias)
     *
     * @since 13
     */
    public static final int KRB_NT_ENTERPRISE = 10;

    private transient String fullName;

    private transient String realm;

    private transient int nameType;


    /**
     * Constructs a {@code KerberosPrincipal} from the provided string input.
     * The name type for this principal defaults to
     * {@link #KRB_NT_PRINCIPAL KRB_NT_PRINCIPAL}
     * This string is assumed to contain a name in the format
     * that is specified in Section 2.1.1. (Kerberos Principal Name Form) of
     * <a href=http://www.ietf.org/rfc/rfc1964.txt> RFC 1964 </a>
     * (for example, <i>duke@FOO.COM</i>, where <i>duke</i>
     * represents a principal, and <i>FOO.COM</i> represents a realm).
     *
     * <p>If the input name does not contain a realm, the default realm
     * is used. The default realm can be specified either in a Kerberos
     * configuration file or via the {@code java.security.krb5.realm}
     * system property. For more information, see the
     * {@extLink security_guide_jgss_tutorial Kerberos Requirements}.
     *
     * <p>Note that when this class or any other Kerberos-related class is
     * initially loaded and initialized, it may read and cache the default
     * realm from the Kerberos configuration file or via the
     * java.security.krb5.realm system property (the value will be empty if
     * no default realm is specified), such that any subsequent calls to set
     * or change the default realm by setting the java.security.krb5.realm
     * system property may be ignored.
     *
     * <p>Additionally, if a security manager is
     * installed, a {@link ServicePermission} must be granted and the service
     * principal of the permission must minimally be inside the
     * {@code KerberosPrincipal}'s realm. For example, if the result of
     * {@code new KerberosPrincipal("user")} is {@code user@EXAMPLE.COM},
     * then a {@code ServicePermission} with service principal
     * {@code host/www.example.com@EXAMPLE.COM} (and any action)
     * must be granted.
     *
     * @param name the principal name
     * @throws IllegalArgumentException if name is improperly
     * formatted, if name is null, or if name does not contain
     * the realm to use and the default realm is not specified
     * in either a Kerberos configuration file or via the
     * java.security.krb5.realm system property.
     * @throws SecurityException if a security manager is installed and
     * {@code name} does not contain the realm to use, and a proper
     * {@link ServicePermission} as described above is not granted.
     */
    public KerberosPrincipal(String name) {
        this(name, KRB_NT_PRINCIPAL);
    }

    /**
     * Constructs a {@code KerberosPrincipal} from the provided string and
     * name type input.  The string is assumed to contain a name in the
     * format that is specified in Section 2.1 (Mandatory Name Forms) of
     * <a href=http://www.ietf.org/rfc/rfc1964.txt>RFC 1964</a>.
     * Valid name types are specified in Section 6.2 (Principal Names) of
     * <a href=http://www.ietf.org/rfc/rfc4120.txt>RFC 4120</a>.
     * The input name must be consistent with the provided name type.
     * (for example, <i>duke@FOO.COM</i>, is a valid input string for the
     * name type, KRB_NT_PRINCIPAL where <i>duke</i>
     * represents a principal, and <i>FOO.COM</i> represents a realm).
     *
     * <p>If the input name does not contain a realm, the default realm
     * is used. The default realm can be specified either in a Kerberos
     * configuration file or via the {@code java.security.krb5.realm}
     * system property. For more information, see the
     * {@extLink security_guide_jgss_tutorial Kerberos Requirements}.
     *
     * <p>Note that when this class or any other Kerberos-related class is
     * initially loaded and initialized, it may read and cache the default
     * realm from the Kerberos configuration file or via the
     * java.security.krb5.realm system property (the value will be empty if
     * no default realm is specified), such that any subsequent calls to set
     * or change the default realm by setting the java.security.krb5.realm
     * system property may be ignored.
     *
     * <p>Additionally, if a security manager is
     * installed, a {@link ServicePermission} must be granted and the service
     * principal of the permission must minimally be inside the
     * {@code KerberosPrincipal}'s realm. For example, if the result of
     * {@code new KerberosPrincipal("user")} is {@code user@EXAMPLE.COM},
     * then a {@code ServicePermission} with service principal
     * {@code host/www.example.com@EXAMPLE.COM} (and any action)
     * must be granted.
     *
     * @param name the principal name
     * @param nameType the name type of the principal
     * @throws IllegalArgumentException if name is improperly
     * formatted, if name is null, if the nameType is not supported,
     * or if name does not contain the realm to use and the default
     * realm is not specified in either a Kerberos configuration
     * file or via the java.security.krb5.realm system property.
     * @throws SecurityException if a security manager is installed and
     * {@code name} does not contain the realm to use, and a proper
     * {@link ServicePermission} as described above is not granted.
     */

    public KerberosPrincipal(String name, int nameType) {

        PrincipalName krb5Principal = null;

        try {
            // Appends the default realm if it is missing
            krb5Principal  = new PrincipalName(name,nameType);
        } catch (KrbException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        if (krb5Principal.isRealmDeduced() && !Realm.AUTODEDUCEREALM) {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                try {
                    sm.checkPermission(new ServicePermission(
                            "@" + krb5Principal.getRealmAsString(), "-"));
                } catch (SecurityException se) {
                    // Swallow the actual exception to hide info
                    throw new SecurityException("Cannot read realm info");
                }
            }
        }
        this.nameType = nameType;
        fullName = krb5Principal.toString();
        realm = krb5Principal.getRealmString();
    }
    /**
     * Returns the realm component of this Kerberos principal.
     *
     * @return the realm component of this Kerberos principal.
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Returns a hash code for this {@code KerberosPrincipal}. The hash code
     * is defined to be the result of the following calculation:
     * <pre>{@code
     *  hashCode = getName().hashCode();
     * }</pre>
     *
     * @return a hash code for this {@code KerberosPrincipal}.
     */
    public int hashCode() {
        return getName().hashCode();
    }

    /**
     * Compares the specified object with this principal for equality.
     * Returns true if the given object is also a
     * {@code KerberosPrincipal} and the two
     * {@code KerberosPrincipal} instances are equivalent.
     * More formally two {@code KerberosPrincipal} instances are equal
     * if the values returned by {@code getName()} are equal.
     *
     * @param other the object to compare to
     * @return true if the object passed in represents the same principal
     * as this one, false otherwise.
     */
    public boolean equals(Object other) {

        if (other == this)
            return true;

        if (! (other instanceof KerberosPrincipal)) {
            return false;
        }
        String myFullName = getName();
        String otherFullName = ((KerberosPrincipal) other).getName();
        return myFullName.equals(otherFullName);
    }

    /**
     * Save the {@code KerberosPrincipal} object to a stream
     *
     * @param  oos the {@code ObjectOutputStream} to which data is written
     * @throws IOException if an I/O error occurs
     *
     * @serialData this {@code KerberosPrincipal} is serialized
     *          by writing out the PrincipalName and the
     *          Realm in their DER-encoded form as specified in Section 5.2.2 of
     *          <a href=http://www.ietf.org/rfc/rfc4120.txt> RFC4120</a>.
     */
    private void writeObject(ObjectOutputStream oos)
            throws IOException {

        PrincipalName krb5Principal;
        try {
            krb5Principal  = new PrincipalName(fullName, nameType);
            oos.writeObject(krb5Principal.asn1Encode());
            oos.writeObject(krb5Principal.getRealm().asn1Encode());
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Reads this object from a stream (i.e., deserializes it)
     *
     * @param  ois the {@code ObjectInputStream} from which data is read
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     */
    private void readObject(ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
        byte[] asn1EncPrincipal = (byte [])ois.readObject();
        byte[] encRealm = (byte [])ois.readObject();
        try {
           Realm realmObject = new Realm(new DerValue(encRealm));
           PrincipalName krb5Principal = new PrincipalName(
                   new DerValue(asn1EncPrincipal), realmObject);
           realm = realmObject.toString();
           fullName = krb5Principal.toString();
           nameType = krb5Principal.getNameType();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * The returned string corresponds to the single-string
     * representation of a Kerberos Principal name as specified in
     * Section 2.1 of <a href=http://www.ietf.org/rfc/rfc1964.txt>RFC 1964</a>.
     *
     * @return the principal name.
     */
    public String getName() {
        return fullName;
    }

    /**
     * Returns the name type of the {@code KerberosPrincipal}. Valid name types
     * are specified in Section 6.2 of
     * <a href=http://www.ietf.org/rfc/rfc4120.txt> RFC4120</a>.
     *
     * @return the name type.
     */
    public int getNameType() {
        return nameType;
    }

    /**
     * Returns an informative textual representation of this {@code KerberosPrincipal}.
     *
     * @return an informative textual representation of this {@code KerberosPrincipal}.
     */
    public String toString() {
        return getName();
    }
}
