/*
 * Copyright (c) 1996, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.security;

import java.io.Serializable;
import java.util.*;

/**
 * <p>This class represents identities: real-world objects such as people,
 * companies or organizations whose identities can be authenticated using
 * their public keys. Identities may also be more abstract (or concrete)
 * constructs, such as daemon threads or smart cards.
 *
 * <p>All Identity objects have a name and a public key. Names are
 * immutable. Identities may also be scoped. That is, if an Identity is
 * specified to have a particular scope, then the name and public
 * key of the Identity are unique within that scope.
 *
 * <p>An Identity also has a set of certificates (all certifying its own
 * public key). The Principal names specified in these certificates need
 * not be the same, only the key.
 *
 * <p>An Identity can be subclassed, to include postal and email addresses,
 * telephone numbers, images of faces and logos, and so on.
 *
 * @see IdentityScope
 * @see Signer
 * @see Principal
 *
 * @author Benjamin Renaud
 * @since 1.1
 * @deprecated This class is deprecated and subject to removal in a future
 *     version of Java SE. It has been replaced by
 *     {@code java.security.KeyStore}, the {@code java.security.cert} package,
 *     and {@code java.security.Principal}.
 */
@Deprecated(since="1.2", forRemoval=true)
@SuppressWarnings("removal")
public abstract class Identity implements Principal, Serializable {

    /** use serialVersionUID from JDK 1.1.x for interoperability */
    @java.io.Serial
    private static final long serialVersionUID = 3609922007826600659L;

    /**
     * The name for this identity.
     *
     * @serial
     */
    private String name;

    /**
     * The public key for this identity.
     *
     * @serial
     */
    private PublicKey publicKey;

    /**
     * Generic, descriptive information about the identity.
     *
     * @serial
     */
    String info = "No further information available.";

    /**
     * The scope of the identity.
     *
     * @serial
     */
    IdentityScope scope;

    /**
     * The certificates for this identity.
     *
     * @serial
     */
    Vector<Certificate> certificates;

    /**
     * Constructor for serialization only.
     */
    protected Identity() {
        this("restoring...");
    }

    /**
     * Constructs an identity with the specified name and scope.
     *
     * @param name the identity name.
     * @param scope the scope of the identity.
     *
     * @throws    KeyManagementException if there is already an identity
     * with the same name in the scope.
     */
    public Identity(String name, IdentityScope scope) throws
    KeyManagementException {
        this(name);
        if (scope != null) {
            scope.addIdentity(this);
        }
        this.scope = scope;
    }

    /**
     * Constructs an identity with the specified name and no scope.
     *
     * @param name the identity name.
     */
    public Identity(String name) {
        this.name = name;
    }

    /**
     * Returns this identity's name.
     *
     * @return the name of this identity.
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns this identity's scope.
     *
     * @return the scope of this identity.
     */
    public final IdentityScope getScope() {
        return scope;
    }

    /**
     * Returns this identity's public key.
     *
     * @return the public key for this identity.
     *
     * @see #setPublicKey
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Sets this identity's public key. The old key and all of this
     * identity's certificates are removed by this operation.
     *
     * <p>First, if there is a security manager, its {@code checkSecurityAccess}
     * method is called with {@code "setIdentityPublicKey"}
     * as its argument to see if it's ok to set the public key.
     *
     * @param key the public key for this identity.
     *
     * @throws    KeyManagementException if another identity in the
     * identity's scope has the same public key, or if another exception occurs.
     *
     * @throws     SecurityException  if a security manager exists and its
     * {@code checkSecurityAccess} method doesn't allow
     * setting the public key.
     *
     * @see #getPublicKey
     * @see SecurityManager#checkSecurityAccess
     */
    /* Should we throw an exception if this is already set? */
    public void setPublicKey(PublicKey key) throws KeyManagementException {

        check("setIdentityPublicKey");
        this.publicKey = key;
        certificates = new Vector<>();
    }

    /**
     * Specifies a general information string for this identity.
     *
     * <p>First, if there is a security manager, its {@code checkSecurityAccess}
     * method is called with {@code "setIdentityInfo"}
     * as its argument to see if it's ok to specify the information string.
     *
     * @param info the information string.
     *
     * @throws     SecurityException  if a security manager exists and its
     * {@code checkSecurityAccess} method doesn't allow
     * setting the information string.
     *
     * @see #getInfo
     * @see SecurityManager#checkSecurityAccess
     */
    public void setInfo(String info) {
        check("setIdentityInfo");
        this.info = info;
    }

    /**
     * Returns general information previously specified for this identity.
     *
     * @return general information about this identity.
     *
     * @see #setInfo
     */
    public String getInfo() {
        return info;
    }

    /**
     * Adds a certificate for this identity. If the identity has a public
     * key, the public key in the certificate must be the same, and if
     * the identity does not have a public key, the identity's
     * public key is set to be that specified in the certificate.
     *
     * <p>First, if there is a security manager, its {@code checkSecurityAccess}
     * method is called with {@code "addIdentityCertificate"}
     * as its argument to see if it's ok to add a certificate.
     *
     * @param certificate the certificate to be added.
     *
     * @throws    KeyManagementException if the certificate is not valid,
     * if the public key in the certificate being added conflicts with
     * this identity's public key, or if another exception occurs.
     *
     * @throws     SecurityException  if a security manager exists and its
     * {@code checkSecurityAccess} method doesn't allow
     * adding a certificate.
     *
     * @see SecurityManager#checkSecurityAccess
     */
    public void addCertificate(Certificate certificate)
    throws KeyManagementException {

        check("addIdentityCertificate");

        if (certificates == null) {
            certificates = new Vector<>();
        }
        if (publicKey != null) {
            if (!keyEquals(publicKey, certificate.getPublicKey())) {
                throw new KeyManagementException(
                    "public key different from cert public key");
            }
        } else {
            publicKey = certificate.getPublicKey();
        }
        certificates.addElement(certificate);
    }

    private boolean keyEquals(PublicKey aKey, PublicKey anotherKey) {
        String aKeyFormat = aKey.getFormat();
        String anotherKeyFormat = anotherKey.getFormat();
        if ((aKeyFormat == null) ^ (anotherKeyFormat == null))
            return false;
        if (aKeyFormat != null && anotherKeyFormat != null)
            if (!aKeyFormat.equalsIgnoreCase(anotherKeyFormat))
                return false;
        return java.util.Arrays.equals(aKey.getEncoded(),
                                     anotherKey.getEncoded());
    }


    /**
     * Removes a certificate from this identity.
     *
     * <p>First, if there is a security manager, its {@code checkSecurityAccess}
     * method is called with {@code "removeIdentityCertificate"}
     * as its argument to see if it's ok to remove a certificate.
     *
     * @param certificate the certificate to be removed.
     *
     * @throws    KeyManagementException if the certificate is
     * missing, or if another exception occurs.
     *
     * @throws     SecurityException  if a security manager exists and its
     * {@code checkSecurityAccess} method doesn't allow
     * removing a certificate.
     *
     * @see SecurityManager#checkSecurityAccess
     */
    public void removeCertificate(Certificate certificate)
    throws KeyManagementException {
        check("removeIdentityCertificate");
        if (certificates != null) {
            certificates.removeElement(certificate);
        }
    }

    /**
     * Returns a copy of all the certificates for this identity.
     *
     * @return a copy of all the certificates for this identity.
     */
    public Certificate[] certificates() {
        if (certificates == null) {
            return new Certificate[0];
        }
        int len = certificates.size();
        Certificate[] certs = new Certificate[len];
        certificates.copyInto(certs);
        return certs;
    }

    /**
     * Tests for equality between the specified object and this identity.
     * This first tests to see if the entities actually refer to the same
     * object, in which case it returns true. Next, it checks to see if
     * the entities have the same name and the same scope. If they do,
     * the method returns true. Otherwise, it calls
     * {@link #identityEquals(Identity) identityEquals}, which subclasses should
     * override.
     *
     * @param identity the object to test for equality with this identity.
     *
     * @return true if the objects are considered equal, false otherwise.
     *
     * @see #identityEquals
     */
    public final boolean equals(Object identity) {
        if (identity == this) {
            return true;
        }

        return identity instanceof Identity other
                && (this.fullName().equals(other.fullName()) || identityEquals(other));
    }

    /**
     * Tests for equality between the specified identity and this identity.
     * This method should be overridden by subclasses to test for equality.
     * The default behavior is to return true if the names and public keys
     * are equal.
     *
     * @param identity the identity to test for equality with this identity.
     *
     * @return true if the identities are considered equal, false
     * otherwise.
     *
     * @see #equals
     */
    protected boolean identityEquals(Identity identity) {
        if (!name.equalsIgnoreCase(identity.name))
            return false;

        if ((publicKey == null) ^ (identity.publicKey == null))
            return false;

        if (publicKey != null && identity.publicKey != null)
            if (!publicKey.equals(identity.publicKey))
                return false;

        return true;

    }

    /**
     * Returns a parsable name for identity: identityName.scopeName
     */
    String fullName() {
        String parsable = name;
        if (scope != null) {
            parsable += "." + scope.getName();
        }
        return parsable;
    }

    /**
     * Returns a short string describing this identity, telling its
     * name and its scope (if any).
     *
     * <p>First, if there is a security manager, its {@code checkSecurityAccess}
     * method is called with {@code "printIdentity"}
     * as its argument to see if it's ok to return the string.
     *
     * @return information about this identity, such as its name and the
     * name of its scope (if any).
     *
     * @throws     SecurityException  if a security manager exists and its
     * {@code checkSecurityAccess} method doesn't allow
     * returning a string describing this identity.
     *
     * @see SecurityManager#checkSecurityAccess
     */
    public String toString() {
        check("printIdentity");
        String printable = name;
        if (scope != null) {
            printable += "[" + scope.getName() + "]";
        }
        return printable;
    }

    /**
     * Returns a string representation of this identity, with
     * optionally more details than that provided by the
     * {@code toString} method without any arguments.
     *
     * <p>First, if there is a security manager, its {@code checkSecurityAccess}
     * method is called with {@code "printIdentity"}
     * as its argument to see if it's ok to return the string.
     *
     * @param detailed whether or not to provide detailed information.
     *
     * @return information about this identity. If {@code detailed}
     * is true, then this method returns more information than that
     * provided by the {@code toString} method without any arguments.
     *
     * @throws     SecurityException  if a security manager exists and its
     * {@code checkSecurityAccess} method doesn't allow
     * returning a string describing this identity.
     *
     * @see #toString
     * @see SecurityManager#checkSecurityAccess
     */
    public String toString(boolean detailed) {
        String out = toString();
        if (detailed) {
            out += "\n";
            out += printKeys();
            out += "\n" + printCertificates();
            if (info != null) {
                out += "\n\t" + info;
            } else {
                out += "\n\tno additional information available.";
            }
        }
        return out;
    }

    String printKeys() {
        String key = "";
        if (publicKey != null) {
            key = "\tpublic key initialized";
        } else {
            key = "\tno public key";
        }
        return key;
    }

    String printCertificates() {
        String out = "";
        if (certificates == null) {
            return "\tno certificates";
        } else {
            out += "\tcertificates: \n";

            int i = 1;
            for (Certificate cert : certificates) {
                out += "\tcertificate " + i++ +
                    "\tfor  : " + cert.getPrincipal() + "\n";
                out += "\t\t\tfrom : " +
                    cert.getGuarantor() + "\n";
            }
        }
        return out;
    }

    /**
     * Returns a hashcode for this identity.
     *
     * @return a hashcode for this identity.
     */
    public int hashCode() {
        return name.hashCode();
    }

    private static void check(String directive) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkSecurityAccess(directive);
        }
    }
}
