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
import java.util.Enumeration;
import java.util.Properties;

/**
 * <p>This class represents a scope for identities. It is an Identity
 * itself, and therefore has a name and can have a scope. It can also
 * optionally have a public key and associated certificates.
 *
 * <p>An IdentityScope can contain Identity objects of all kinds, including
 * Signers. All types of Identity objects can be retrieved, added, and
 * removed using the same methods. Note that it is possible, and in fact
 * expected, that different types of identity scopes will
 * apply different policies for their various operations on the
 * various types of Identities.
 *
 * <p>There is a one-to-one mapping between keys and identities, and
 * there can only be one copy of one key per scope. For example, suppose
 * <b>Acme Software, Inc</b> is a software publisher known to a user.
 * Suppose it is an Identity, that is, it has a public key, and a set of
 * associated certificates. It is named in the scope using the name
 * "Acme Software". No other named Identity in the scope has the same
 * public  key. Of course, none has the same name as well.
 *
 * @see Identity
 * @see Signer
 * @see Principal
 * @see Key
 *
 * @author Benjamin Renaud
 * @since 1.1
 *
 * @deprecated This class is deprecated and subject to removal in a future
 *     version of Java SE. It has been replaced by
 *     {@code java.security.KeyStore}, the {@code java.security.cert} package,
 *     and {@code java.security.Principal}.
 */
@Deprecated(since="1.2", forRemoval=true)
@SuppressWarnings("removal")
public abstract
class IdentityScope extends Identity {

    @java.io.Serial
    private static final long serialVersionUID = -2337346281189773310L;

    /* The system's scope */
    private static IdentityScope scope;

    // initialize the system scope
    private static void initializeSystemScope() {

        String classname = AccessController.doPrivileged(
                                new PrivilegedAction<>() {
            public String run() {
                return Security.getProperty("system.scope");
            }
        });

        if (classname == null) {
            return;

        } else {

            try {
                Class.forName(classname);
            } catch (ClassNotFoundException e) {
                System.err.println("unable to establish a system scope from " +
                             classname);
                e.printStackTrace();
            }
        }
    }

    /**
     * This constructor is used for serialization only and should not
     * be used by subclasses.
     */
    protected IdentityScope() {
        this("restoring...");
    }

    /**
     * Constructs a new identity scope with the specified name.
     *
     * @param name the scope name.
     */
    public IdentityScope(String name) {
        super(name);
    }

    /**
     * Constructs a new identity scope with the specified name and scope.
     *
     * @param name the scope name.
     * @param scope the scope for the new identity scope.
     *
     * @throws    KeyManagementException if there is already an identity
     * with the same name in the scope.
     */
    public IdentityScope(String name, IdentityScope scope)
    throws KeyManagementException {
        super(name, scope);
    }

    /**
     * Returns the system's identity scope.
     *
     * @return the system's identity scope, or {@code null} if none has been
     *         set.
     *
     * @see #setSystemScope
     */
    public static IdentityScope getSystemScope() {
        if (scope == null) {
            initializeSystemScope();
        }
        return scope;
    }


    /**
     * Sets the system's identity scope.
     *
     * <p>First, if there is a security manager, its
     * {@code checkSecurityAccess}
     * method is called with {@code "setSystemScope"}
     * as its argument to see if it's ok to set the identity scope.
     *
     * @param scope the scope to set.
     *
     * @throws     SecurityException  if a security manager exists and its
     * {@code checkSecurityAccess} method doesn't allow
     * setting the identity scope.
     *
     * @see #getSystemScope
     * @see SecurityManager#checkSecurityAccess
     */
    protected static void setSystemScope(IdentityScope scope) {
        check("setSystemScope");
        IdentityScope.scope = scope;
    }

    /**
     * Returns the number of identities within this identity scope.
     *
     * @return the number of identities within this identity scope.
     */
    public abstract int size();

    /**
     * Returns the identity in this scope with the specified name (if any).
     *
     * @param name the name of the identity to be retrieved.
     *
     * @return the identity named {@code name}, or null if there are
     * no identities named {@code name} in this scope.
     */
    public abstract Identity getIdentity(String name);

    /**
     * Retrieves the identity whose name is the same as that of the
     * specified principal. (Note: Identity implements Principal.)
     *
     * @param principal the principal corresponding to the identity
     * to be retrieved.
     *
     * @return the identity whose name is the same as that of the
     * principal, or null if there are no identities of the same name
     * in this scope.
     */
    public Identity getIdentity(Principal principal) {
        return getIdentity(principal.getName());
    }

    /**
     * Retrieves the identity with the specified public key.
     *
     * @param key the public key for the identity to be returned.
     *
     * @return the identity with the given key, or null if there are
     * no identities in this scope with that key.
     */
    public abstract Identity getIdentity(PublicKey key);

    /**
     * Adds an identity to this identity scope.
     *
     * @param identity the identity to be added.
     *
     * @throws    KeyManagementException if the identity is not
     * valid, a name conflict occurs, another identity has the same
     * public key as the identity being added, or another exception
     * occurs. */
    public abstract void addIdentity(Identity identity)
    throws KeyManagementException;

    /**
     * Removes an identity from this identity scope.
     *
     * @param identity the identity to be removed.
     *
     * @throws    KeyManagementException if the identity is missing,
     * or another exception occurs.
     */
    public abstract void removeIdentity(Identity identity)
    throws KeyManagementException;

    /**
     * Returns an enumeration of all identities in this identity scope.
     *
     * @return an enumeration of all identities in this identity scope.
     */
    public abstract Enumeration<Identity> identities();

    /**
     * Returns a string representation of this identity scope, including
     * its name, its scope name, and the number of identities in this
     * identity scope.
     *
     * @return a string representation of this identity scope.
     */
    public String toString() {
        return super.toString() + "[" + size() + "]";
    }

    private static void check(String directive) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkSecurityAccess(directive);
        }
    }

}
