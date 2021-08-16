/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.security.auth;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.text.MessageFormat;
import java.security.AccessController;
import java.security.AccessControlContext;
import java.security.DomainCombiner;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.security.ProtectionDomain;
import sun.security.util.ResourcesMgr;

/**
 * <p> A {@code Subject} represents a grouping of related information
 * for a single entity, such as a person.
 * Such information includes the Subject's identities as well as
 * its security-related attributes
 * (passwords and cryptographic keys, for example).
 *
 * <p> Subjects may potentially have multiple identities.
 * Each identity is represented as a {@code Principal}
 * within the {@code Subject}.  Principals simply bind names to a
 * {@code Subject}.  For example, a {@code Subject} that happens
 * to be a person, Alice, might have two Principals:
 * one which binds "Alice Bar", the name on her driver license,
 * to the {@code Subject}, and another which binds,
 * "999-99-9999", the number on her student identification card,
 * to the {@code Subject}.  Both Principals refer to the same
 * {@code Subject} even though each has a different name.
 *
 * <p> A {@code Subject} may also own security-related attributes,
 * which are referred to as credentials.
 * Sensitive credentials that require special protection, such as
 * private cryptographic keys, are stored within a private credential
 * {@code Set}.  Credentials intended to be shared, such as
 * public key certificates or Kerberos server tickets are stored
 * within a public credential {@code Set}.  Different permissions
 * are required to access and modify the different credential Sets.
 *
 * <p> To retrieve all the Principals associated with a {@code Subject},
 * invoke the {@code getPrincipals} method.  To retrieve
 * all the public or private credentials belonging to a {@code Subject},
 * invoke the {@code getPublicCredentials} method or
 * {@code getPrivateCredentials} method, respectively.
 * To modify the returned {@code Set} of Principals and credentials,
 * use the methods defined in the {@code Set} class.
 * For example:
 * <pre>
 *      Subject subject;
 *      Principal principal;
 *      Object credential;
 *
 *      // add a Principal and credential to the Subject
 *      subject.getPrincipals().add(principal);
 *      subject.getPublicCredentials().add(credential);
 * </pre>
 *
 * <p> This {@code Subject} class implements {@code Serializable}.
 * While the Principals associated with the {@code Subject} are serialized,
 * the credentials associated with the {@code Subject} are not.
 * Note that the {@code java.security.Principal} class
 * does not implement {@code Serializable}.  Therefore all concrete
 * {@code Principal} implementations associated with Subjects
 * must implement {@code Serializable}.
 *
 * @since 1.4
 * @see java.security.Principal
 * @see java.security.DomainCombiner
 */
public final class Subject implements java.io.Serializable {

    @java.io.Serial
    private static final long serialVersionUID = -8308522755600156056L;

    /**
     * A {@code Set} that provides a view of all of this
     * Subject's Principals
     *
     * @serial Each element in this set is a
     *          {@code java.security.Principal}.
     *          The set is a {@code Subject.SecureSet}.
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    Set<Principal> principals;

    /**
     * Sets that provide a view of all of this
     * Subject's Credentials
     */
    transient Set<Object> pubCredentials;
    transient Set<Object> privCredentials;

    /**
     * Whether this Subject is read-only
     *
     * @serial
     */
    private volatile boolean readOnly;

    private static final int PRINCIPAL_SET = 1;
    private static final int PUB_CREDENTIAL_SET = 2;
    private static final int PRIV_CREDENTIAL_SET = 3;

    private static final ProtectionDomain[] NULL_PD_ARRAY
        = new ProtectionDomain[0];

    /**
     * Create an instance of a {@code Subject}
     * with an empty {@code Set} of Principals and empty
     * Sets of public and private credentials.
     *
     * <p> The newly constructed Sets check whether this {@code Subject}
     * has been set read-only before permitting subsequent modifications.
     * The newly created Sets also prevent illegal modifications
     * by ensuring that callers have sufficient permissions.  These Sets
     * also prohibit null elements, and attempts to add or query a null
     * element will result in a {@code NullPointerException}.
     *
     * <p> To modify the Principals Set, the caller must have
     * {@code AuthPermission("modifyPrincipals")}.
     * To modify the public credential Set, the caller must have
     * {@code AuthPermission("modifyPublicCredentials")}.
     * To modify the private credential Set, the caller must have
     * {@code AuthPermission("modifyPrivateCredentials")}.
     */
    public Subject() {

        this.principals = Collections.synchronizedSet
                        (new SecureSet<>(this, PRINCIPAL_SET));
        this.pubCredentials = Collections.synchronizedSet
                        (new SecureSet<>(this, PUB_CREDENTIAL_SET));
        this.privCredentials = Collections.synchronizedSet
                        (new SecureSet<>(this, PRIV_CREDENTIAL_SET));
    }

    /**
     * Create an instance of a {@code Subject} with
     * Principals and credentials.
     *
     * <p> The Principals and credentials from the specified Sets
     * are copied into newly constructed Sets.
     * These newly created Sets check whether this {@code Subject}
     * has been set read-only before permitting subsequent modifications.
     * The newly created Sets also prevent illegal modifications
     * by ensuring that callers have sufficient permissions.  These Sets
     * also prohibit null elements, and attempts to add or query a null
     * element will result in a {@code NullPointerException}.
     *
     * <p> To modify the Principals Set, the caller must have
     * {@code AuthPermission("modifyPrincipals")}.
     * To modify the public credential Set, the caller must have
     * {@code AuthPermission("modifyPublicCredentials")}.
     * To modify the private credential Set, the caller must have
     * {@code AuthPermission("modifyPrivateCredentials")}.
     *
     * @param readOnly true if the {@code Subject} is to be read-only,
     *          and false otherwise.
     *
     * @param principals the {@code Set} of Principals
     *          to be associated with this {@code Subject}.
     *
     * @param pubCredentials the {@code Set} of public credentials
     *          to be associated with this {@code Subject}.
     *
     * @param privCredentials the {@code Set} of private credentials
     *          to be associated with this {@code Subject}.
     *
     * @throws NullPointerException if the specified
     *          {@code principals}, {@code pubCredentials},
     *          or {@code privCredentials} are {@code null},
     *          or a null value exists within any of these three
     *          Sets.
     */
    public Subject(boolean readOnly, Set<? extends Principal> principals,
                   Set<?> pubCredentials, Set<?> privCredentials) {
        LinkedList<Principal> principalList
                = collectionNullClean(principals);
        LinkedList<Object> pubCredsList
                = collectionNullClean(pubCredentials);
        LinkedList<Object> privCredsList
                = collectionNullClean(privCredentials);

        this.principals = Collections.synchronizedSet(
                new SecureSet<>(this, PRINCIPAL_SET, principalList));
        this.pubCredentials = Collections.synchronizedSet(
                new SecureSet<>(this, PUB_CREDENTIAL_SET, pubCredsList));
        this.privCredentials = Collections.synchronizedSet(
                new SecureSet<>(this, PRIV_CREDENTIAL_SET, privCredsList));
        this.readOnly = readOnly;
    }

    /**
     * Set this {@code Subject} to be read-only.
     *
     * <p> Modifications (additions and removals) to this Subject's
     * {@code Principal} {@code Set} and
     * credential Sets will be disallowed.
     * The {@code destroy} operation on this Subject's credentials will
     * still be permitted.
     *
     * <p> Subsequent attempts to modify the Subject's {@code Principal}
     * and credential Sets will result in an
     * {@code IllegalStateException} being thrown.
     * Also, once a {@code Subject} is read-only,
     * it can not be reset to being writable again.
     *
     * @throws SecurityException if a security manager is installed and the
     *         caller does not have an
     *         {@link AuthPermission#AuthPermission(String)
     *         AuthPermission("setReadOnly")} permission to set this
     *         {@code Subject} to be read-only.
     */
    public void setReadOnly() {
        @SuppressWarnings("removal")
        java.lang.SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(AuthPermissionHolder.SET_READ_ONLY_PERMISSION);
        }

        this.readOnly = true;
    }

    /**
     * Query whether this {@code Subject} is read-only.
     *
     * @return true if this {@code Subject} is read-only, false otherwise.
     */
    public boolean isReadOnly() {
        return this.readOnly;
    }

    /**
     * Get the {@code Subject} associated with the provided
     * {@code AccessControlContext}.
     *
     * <p> The {@code AccessControlContext} may contain many
     * Subjects (from nested {@code doAs} calls).
     * In this situation, the most recent {@code Subject} associated
     * with the {@code AccessControlContext} is returned.
     *
     * @param  acc the {@code AccessControlContext} from which to retrieve
     *          the {@code Subject}.
     *
     * @return  the {@code Subject} associated with the provided
     *          {@code AccessControlContext}, or {@code null}
     *          if no {@code Subject} is associated
     *          with the provided {@code AccessControlContext}.
     *
     * @throws SecurityException if a security manager is installed and the
     *          caller does not have an
     *          {@link AuthPermission#AuthPermission(String)
     *          AuthPermission("getSubject")} permission to get the
     *          {@code Subject}.
     *
     * @throws NullPointerException if the provided
     *          {@code AccessControlContext} is {@code null}.
     *
     * @deprecated This method depends on {@link AccessControlContext}
     *       which, in conjunction with
     *       {@linkplain SecurityManager the Security Manager}, is deprecated
     *       and subject to removal in a future release. However, obtaining a
     *       Subject is useful independent of the Security Manager, so a
     *       replacement for this method may be added in a future release.
     */
    @SuppressWarnings("removal")
    @Deprecated(since="17", forRemoval=true)
    public static Subject getSubject(final AccessControlContext acc) {

        java.lang.SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(AuthPermissionHolder.GET_SUBJECT_PERMISSION);
        }

        Objects.requireNonNull(acc, ResourcesMgr.getString
                ("invalid.null.AccessControlContext.provided"));

        // return the Subject from the DomainCombiner of the provided context
        return AccessController.doPrivileged
            (new java.security.PrivilegedAction<>() {
            public Subject run() {
                DomainCombiner dc = acc.getDomainCombiner();
                if (!(dc instanceof SubjectDomainCombiner)) {
                    return null;
                }
                SubjectDomainCombiner sdc = (SubjectDomainCombiner)dc;
                return sdc.getSubject();
            }
        });
    }

    /**
     * Perform work as a particular {@code Subject}.
     *
     * <p> This method first retrieves the current Thread's
     * {@code AccessControlContext} via
     * {@code AccessController.getContext},
     * and then instantiates a new {@code AccessControlContext}
     * using the retrieved context along with a new
     * {@code SubjectDomainCombiner} (constructed using
     * the provided {@code Subject}).
     * Finally, this method invokes {@code AccessController.doPrivileged},
     * passing it the provided {@code PrivilegedAction},
     * as well as the newly constructed {@code AccessControlContext}.
     *
     * @param subject the {@code Subject} that the specified
     *                  {@code action} will run as.  This parameter
     *                  may be {@code null}.
     *
     * @param <T> the type of the value returned by the PrivilegedAction's
     *                  {@code run} method.
     *
     * @param action the code to be run as the specified
     *                  {@code Subject}.
     *
     * @return the value returned by the PrivilegedAction's
     *                  {@code run} method.
     *
     * @throws NullPointerException if the {@code PrivilegedAction}
     *                  is {@code null}.
     *
     * @throws SecurityException if a security manager is installed and the
     *                  caller does not have an
     *                  {@link AuthPermission#AuthPermission(String)
     *                  AuthPermission("doAs")} permission to invoke this
     *                  method.
     */
    @SuppressWarnings("removal")
    public static <T> T doAs(final Subject subject,
                        final java.security.PrivilegedAction<T> action) {

        java.lang.SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(AuthPermissionHolder.DO_AS_PERMISSION);
        }

        Objects.requireNonNull(action,
                ResourcesMgr.getString("invalid.null.action.provided"));

        // set up the new Subject-based AccessControlContext
        // for doPrivileged
        final AccessControlContext currentAcc = AccessController.getContext();

        // call doPrivileged and push this new context on the stack
        return java.security.AccessController.doPrivileged
                                        (action,
                                        createContext(subject, currentAcc));
    }

    /**
     * Perform work as a particular {@code Subject}.
     *
     * <p> This method first retrieves the current Thread's
     * {@code AccessControlContext} via
     * {@code AccessController.getContext},
     * and then instantiates a new {@code AccessControlContext}
     * using the retrieved context along with a new
     * {@code SubjectDomainCombiner} (constructed using
     * the provided {@code Subject}).
     * Finally, this method invokes {@code AccessController.doPrivileged},
     * passing it the provided {@code PrivilegedExceptionAction},
     * as well as the newly constructed {@code AccessControlContext}.
     *
     * @param subject the {@code Subject} that the specified
     *                  {@code action} will run as.  This parameter
     *                  may be {@code null}.
     *
     * @param <T> the type of the value returned by the
     *                  PrivilegedExceptionAction's {@code run} method.
     *
     * @param action the code to be run as the specified
     *                  {@code Subject}.
     *
     * @return the value returned by the
     *                  PrivilegedExceptionAction's {@code run} method.
     *
     * @throws PrivilegedActionException if the
     *                  {@code PrivilegedExceptionAction.run}
     *                  method throws a checked exception.
     *
     * @throws NullPointerException if the specified
     *                  {@code PrivilegedExceptionAction} is
     *                  {@code null}.
     *
     * @throws SecurityException if a security manager is installed and the
     *                  caller does not have an
     *                  {@link AuthPermission#AuthPermission(String)
     *                  AuthPermission("doAs")} permission to invoke this
     *                  method.
     */
    @SuppressWarnings("removal")
    public static <T> T doAs(final Subject subject,
                        final java.security.PrivilegedExceptionAction<T> action)
                        throws java.security.PrivilegedActionException {

        java.lang.SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(AuthPermissionHolder.DO_AS_PERMISSION);
        }

        Objects.requireNonNull(action,
                ResourcesMgr.getString("invalid.null.action.provided"));

        // set up the new Subject-based AccessControlContext for doPrivileged
        final AccessControlContext currentAcc = AccessController.getContext();

        // call doPrivileged and push this new context on the stack
        return java.security.AccessController.doPrivileged
                                        (action,
                                        createContext(subject, currentAcc));
    }

    /**
     * Perform privileged work as a particular {@code Subject}.
     *
     * <p> This method behaves exactly as {@code Subject.doAs},
     * except that instead of retrieving the current Thread's
     * {@code AccessControlContext}, it uses the provided
     * {@code AccessControlContext}.  If the provided
     * {@code AccessControlContext} is {@code null},
     * this method instantiates a new {@code AccessControlContext}
     * with an empty collection of ProtectionDomains.
     *
     * @param subject the {@code Subject} that the specified
     *                  {@code action} will run as.  This parameter
     *                  may be {@code null}.
     *
     * @param <T> the type of the value returned by the PrivilegedAction's
     *                  {@code run} method.
     *
     * @param action the code to be run as the specified
     *                  {@code Subject}.
     *
     * @param acc the {@code AccessControlContext} to be tied to the
     *                  specified <i>subject</i> and <i>action</i>.
     *
     * @return the value returned by the PrivilegedAction's
     *                  {@code run} method.
     *
     * @throws NullPointerException if the {@code PrivilegedAction}
     *                  is {@code null}.
     *
     * @throws SecurityException if a security manager is installed and the
     *                  caller does not have a
     *                  {@link AuthPermission#AuthPermission(String)
     *                  AuthPermission("doAsPrivileged")} permission to invoke
     *                  this method.
     *
     * @deprecated This method is only useful in conjunction with
     *       {@linkplain SecurityManager the Security Manager}, which is
     *       deprecated and subject to removal in a future release.
     *       Consequently, this method is also deprecated and subject to
     *       removal. There is no replacement for the Security Manager or this
     *       method.
     */
    @SuppressWarnings("removal")
    @Deprecated(since="17", forRemoval=true)
    public static <T> T doAsPrivileged(final Subject subject,
                        final java.security.PrivilegedAction<T> action,
                        final java.security.AccessControlContext acc) {

        java.lang.SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(AuthPermissionHolder.DO_AS_PRIVILEGED_PERMISSION);
        }

        Objects.requireNonNull(action,
                ResourcesMgr.getString("invalid.null.action.provided"));

        // set up the new Subject-based AccessControlContext
        // for doPrivileged
        final AccessControlContext callerAcc =
                (acc == null ?
                new AccessControlContext(NULL_PD_ARRAY) :
                acc);

        // call doPrivileged and push this new context on the stack
        return java.security.AccessController.doPrivileged
                                        (action,
                                        createContext(subject, callerAcc));
    }

    /**
     * Perform privileged work as a particular {@code Subject}.
     *
     * <p> This method behaves exactly as {@code Subject.doAs},
     * except that instead of retrieving the current Thread's
     * {@code AccessControlContext}, it uses the provided
     * {@code AccessControlContext}.  If the provided
     * {@code AccessControlContext} is {@code null},
     * this method instantiates a new {@code AccessControlContext}
     * with an empty collection of ProtectionDomains.
     *
     * @param subject the {@code Subject} that the specified
     *                  {@code action} will run as.  This parameter
     *                  may be {@code null}.
     *
     * @param <T> the type of the value returned by the
     *                  PrivilegedExceptionAction's {@code run} method.
     *
     * @param action the code to be run as the specified
     *                  {@code Subject}.
     *
     * @param acc the {@code AccessControlContext} to be tied to the
     *                  specified <i>subject</i> and <i>action</i>.
     *
     * @return the value returned by the
     *                  PrivilegedExceptionAction's {@code run} method.
     *
     * @throws PrivilegedActionException if the
     *                  {@code PrivilegedExceptionAction.run}
     *                  method throws a checked exception.
     *
     * @throws NullPointerException if the specified
     *                  {@code PrivilegedExceptionAction} is
     *                  {@code null}.
     *
     * @throws SecurityException if a security manager is installed and the
     *                  caller does not have a
     *                  {@link AuthPermission#AuthPermission(String)
     *                  AuthPermission("doAsPrivileged")} permission to invoke
     *                  this method.
     *
     * @deprecated This method is only useful in conjunction with
     *       {@linkplain SecurityManager the Security Manager}, which is
     *       deprecated and subject to removal in a future release.
     *       Consequently, this method is also deprecated and subject to
     *       removal. There is no replacement for the Security Manager or this
     *       method.
     */
    @SuppressWarnings("removal")
    @Deprecated(since="17", forRemoval=true)
    public static <T> T doAsPrivileged(final Subject subject,
                        final java.security.PrivilegedExceptionAction<T> action,
                        final java.security.AccessControlContext acc)
                        throws java.security.PrivilegedActionException {

        java.lang.SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(AuthPermissionHolder.DO_AS_PRIVILEGED_PERMISSION);
        }

        Objects.requireNonNull(action,
                ResourcesMgr.getString("invalid.null.action.provided"));

        // set up the new Subject-based AccessControlContext for doPrivileged
        final AccessControlContext callerAcc =
                (acc == null ?
                new AccessControlContext(NULL_PD_ARRAY) :
                acc);

        // call doPrivileged and push this new context on the stack
        return java.security.AccessController.doPrivileged
                                        (action,
                                        createContext(subject, callerAcc));
    }

    @SuppressWarnings("removal")
    private static AccessControlContext createContext(final Subject subject,
                                        final AccessControlContext acc) {


        return java.security.AccessController.doPrivileged
            (new java.security.PrivilegedAction<>() {
            public AccessControlContext run() {
                if (subject == null) {
                    return new AccessControlContext(acc, null);
                } else {
                    return new AccessControlContext
                                        (acc,
                                        new SubjectDomainCombiner(subject));
            }
            }
        });
    }

    /**
     * Return the {@code Set} of Principals associated with this
     * {@code Subject}.  Each {@code Principal} represents
     * an identity for this {@code Subject}.
     *
     * <p> The returned {@code Set} is backed by this Subject's
     * internal {@code Principal} {@code Set}.  Any modification
     * to the returned {@code Set} affects the internal
     * {@code Principal} {@code Set} as well.
     *
     * <p> If a security manager is installed, the caller must have a
     * {@link AuthPermission#AuthPermission(String)
     * AuthPermission("modifyPrincipals")} permission to modify
     * the returned set, or a {@code SecurityException} will be thrown.
     *
     * @return  the {@code Set} of Principals associated with this
     *          {@code Subject}.
     */
    public Set<Principal> getPrincipals() {

        // always return an empty Set instead of null
        // so LoginModules can add to the Set if necessary
        return principals;
    }

    /**
     * Return a {@code Set} of Principals associated with this
     * {@code Subject} that are instances or subclasses of the specified
     * {@code Class}.
     *
     * <p> The returned {@code Set} is not backed by this Subject's
     * internal {@code Principal} {@code Set}.  A new
     * {@code Set} is created and returned for each method invocation.
     * Modifications to the returned {@code Set}
     * will not affect the internal {@code Principal} {@code Set}.
     *
     * @param <T> the type of the class modeled by {@code c}
     *
     * @param c the returned {@code Set} of Principals will all be
     *          instances of this class.
     *
     * @return a {@code Set} of Principals that are instances of the
     *          specified {@code Class}.
     *
     * @throws NullPointerException if the specified {@code Class}
     *          is {@code null}.
     */
    public <T extends Principal> Set<T> getPrincipals(Class<T> c) {

        Objects.requireNonNull(c,
                ResourcesMgr.getString("invalid.null.Class.provided"));

        // always return an empty Set instead of null
        // so LoginModules can add to the Set if necessary
        return new ClassSet<T>(PRINCIPAL_SET, c);
    }

    /**
     * Return the {@code Set} of public credentials held by this
     * {@code Subject}.
     *
     * <p> The returned {@code Set} is backed by this Subject's
     * internal public Credential {@code Set}.  Any modification
     * to the returned {@code Set} affects the internal public
     * Credential {@code Set} as well.
     *
     * <p> If a security manager is installed, the caller must have a
     * {@link AuthPermission#AuthPermission(String)
     * AuthPermission("modifyPublicCredentials")} permission to modify
     * the returned set, or a {@code SecurityException} will be thrown.
     *
     * @return  a {@code Set} of public credentials held by this
     *          {@code Subject}.
     */
    public Set<Object> getPublicCredentials() {

        // always return an empty Set instead of null
        // so LoginModules can add to the Set if necessary
        return pubCredentials;
    }

    /**
     * Return the {@code Set} of private credentials held by this
     * {@code Subject}.
     *
     * <p> The returned {@code Set} is backed by this Subject's
     * internal private Credential {@code Set}.  Any modification
     * to the returned {@code Set} affects the internal private
     * Credential {@code Set} as well.
     *
     * <p> If a security manager is installed, the caller must have a
     * {@link AuthPermission#AuthPermission(String)
     * AuthPermission("modifyPrivateCredentials")} permission to modify
     * the returned set, or a {@code SecurityException} will be thrown.
     *
     * <p> While iterating through the {@code Set},
     * a {@code SecurityException} is thrown if a security manager is installed
     * and the caller does not have a {@link PrivateCredentialPermission}
     * to access a particular Credential.  The {@code Iterator}
     * is nevertheless advanced to the next element in the {@code Set}.
     *
     * @return  a {@code Set} of private credentials held by this
     *          {@code Subject}.
     */
    public Set<Object> getPrivateCredentials() {

        // XXX
        // we do not need a security check for
        // AuthPermission(getPrivateCredentials)
        // because we already restrict access to private credentials
        // via the PrivateCredentialPermission.  all the extra AuthPermission
        // would do is protect the set operations themselves
        // (like size()), which don't seem security-sensitive.

        // always return an empty Set instead of null
        // so LoginModules can add to the Set if necessary
        return privCredentials;
    }

    /**
     * Return a {@code Set} of public credentials associated with this
     * {@code Subject} that are instances or subclasses of the specified
     * {@code Class}.
     *
     * <p> The returned {@code Set} is not backed by this Subject's
     * internal public Credential {@code Set}.  A new
     * {@code Set} is created and returned for each method invocation.
     * Modifications to the returned {@code Set}
     * will not affect the internal public Credential {@code Set}.
     *
     * @param <T> the type of the class modeled by {@code c}
     *
     * @param c the returned {@code Set} of public credentials will all be
     *          instances of this class.
     *
     * @return a {@code Set} of public credentials that are instances
     *          of the  specified {@code Class}.
     *
     * @throws NullPointerException if the specified {@code Class}
     *          is {@code null}.
     */
    public <T> Set<T> getPublicCredentials(Class<T> c) {

        Objects.requireNonNull(c,
                ResourcesMgr.getString("invalid.null.Class.provided"));

        // always return an empty Set instead of null
        // so LoginModules can add to the Set if necessary
        return new ClassSet<T>(PUB_CREDENTIAL_SET, c);
    }

    /**
     * Return a {@code Set} of private credentials associated with this
     * {@code Subject} that are instances or subclasses of the specified
     * {@code Class}.
     *
     * <p> If a security manager is installed, the caller must have a
     * {@link PrivateCredentialPermission} to access all of the requested
     * Credentials, or a {@code SecurityException} will be thrown.
     *
     * <p> The returned {@code Set} is not backed by this Subject's
     * internal private Credential {@code Set}.  A new
     * {@code Set} is created and returned for each method invocation.
     * Modifications to the returned {@code Set}
     * will not affect the internal private Credential {@code Set}.
     *
     * @param <T> the type of the class modeled by {@code c}
     *
     * @param c the returned {@code Set} of private credentials will all be
     *          instances of this class.
     *
     * @return a {@code Set} of private credentials that are instances
     *          of the  specified {@code Class}.
     *
     * @throws NullPointerException if the specified {@code Class}
     *          is {@code null}.
     */
    public <T> Set<T> getPrivateCredentials(Class<T> c) {

        // XXX
        // we do not need a security check for
        // AuthPermission(getPrivateCredentials)
        // because we already restrict access to private credentials
        // via the PrivateCredentialPermission.  all the extra AuthPermission
        // would do is protect the set operations themselves
        // (like size()), which don't seem security-sensitive.

        Objects.requireNonNull(c,
                ResourcesMgr.getString("invalid.null.Class.provided"));

        // always return an empty Set instead of null
        // so LoginModules can add to the Set if necessary
        return new ClassSet<T>(PRIV_CREDENTIAL_SET, c);
    }

    /**
     * Compares the specified Object with this {@code Subject}
     * for equality.  Returns true if the given object is also a Subject
     * and the two {@code Subject} instances are equivalent.
     * More formally, two {@code Subject} instances are
     * equal if their {@code Principal} and {@code Credential}
     * Sets are equal.
     *
     * @param o Object to be compared for equality with this
     *          {@code Subject}.
     *
     * @return true if the specified Object is equal to this
     *          {@code Subject}.
     *
     * @throws SecurityException if a security manager is installed and the
     *         caller does not have a {@link PrivateCredentialPermission}
     *         permission to access the private credentials for this
     *         {@code Subject} or the provided {@code Subject}.
     */
    @Override
    public boolean equals(Object o) {

        if (o == null) {
            return false;
        }

        if (this == o) {
            return true;
        }

        if (o instanceof Subject) {

            final Subject that = (Subject)o;

            // check the principal and credential sets
            Set<Principal> thatPrincipals;
            synchronized(that.principals) {
                // avoid deadlock from dual locks
                thatPrincipals = new HashSet<>(that.principals);
            }
            if (!principals.equals(thatPrincipals)) {
                return false;
            }

            Set<Object> thatPubCredentials;
            synchronized(that.pubCredentials) {
                // avoid deadlock from dual locks
                thatPubCredentials = new HashSet<>(that.pubCredentials);
            }
            if (!pubCredentials.equals(thatPubCredentials)) {
                return false;
            }

            Set<Object> thatPrivCredentials;
            synchronized(that.privCredentials) {
                // avoid deadlock from dual locks
                thatPrivCredentials = new HashSet<>(that.privCredentials);
            }
            if (!privCredentials.equals(thatPrivCredentials)) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Return the String representation of this {@code Subject}.
     *
     * @return the String representation of this {@code Subject}.
     */
    @Override
    public String toString() {
        return toString(true);
    }

    /**
     * package private convenience method to print out the Subject
     * without firing off a security check when trying to access
     * the Private Credentials
     */
    String toString(boolean includePrivateCredentials) {

        String s = ResourcesMgr.getString("Subject.");
        String suffix = "";

        synchronized(principals) {
            Iterator<Principal> pI = principals.iterator();
            while (pI.hasNext()) {
                Principal p = pI.next();
                suffix = suffix + ResourcesMgr.getString(".Principal.") +
                        p.toString() + ResourcesMgr.getString("NEWLINE");
            }
        }

        synchronized(pubCredentials) {
            Iterator<Object> pI = pubCredentials.iterator();
            while (pI.hasNext()) {
                Object o = pI.next();
                suffix = suffix +
                        ResourcesMgr.getString(".Public.Credential.") +
                        o.toString() + ResourcesMgr.getString("NEWLINE");
            }
        }

        if (includePrivateCredentials) {
            synchronized(privCredentials) {
                Iterator<Object> pI = privCredentials.iterator();
                while (pI.hasNext()) {
                    try {
                        Object o = pI.next();
                        suffix += ResourcesMgr.getString
                                        (".Private.Credential.") +
                                        o.toString() +
                                        ResourcesMgr.getString("NEWLINE");
                    } catch (SecurityException se) {
                        suffix += ResourcesMgr.getString
                                (".Private.Credential.inaccessible.");
                        break;
                    }
                }
            }
        }
        return s + suffix;
    }

    /**
     * Returns a hashcode for this {@code Subject}.
     *
     * @return a hashcode for this {@code Subject}.
     *
     * @throws SecurityException if a security manager is installed and the
     *         caller does not have a {@link PrivateCredentialPermission}
     *         permission to access this Subject's private credentials.
     */
    @Override
    public int hashCode() {

        /**
         * The hashcode is derived exclusive or-ing the
         * hashcodes of this Subject's Principals and credentials.
         *
         * If a particular credential was destroyed
         * ({@code credential.hashCode()} throws an
         * {@code IllegalStateException}),
         * the hashcode for that credential is derived via:
         * {@code credential.getClass().toString().hashCode()}.
         */

        int hashCode = 0;

        synchronized(principals) {
            Iterator<Principal> pIterator = principals.iterator();
            while (pIterator.hasNext()) {
                Principal p = pIterator.next();
                hashCode ^= p.hashCode();
            }
        }

        synchronized(pubCredentials) {
            Iterator<Object> pubCIterator = pubCredentials.iterator();
            while (pubCIterator.hasNext()) {
                hashCode ^= getCredHashCode(pubCIterator.next());
            }
        }
        return hashCode;
    }

    /**
     * get a credential's hashcode
     */
    private int getCredHashCode(Object o) {
        try {
            return o.hashCode();
        } catch (IllegalStateException ise) {
            return o.getClass().toString().hashCode();
        }
    }

    /**
     * Writes this object out to a stream (i.e., serializes it).
     *
     * @param  oos the {@code ObjectOutputStream} to which data is written
     * @throws IOException if an I/O error occurs
     */
    @java.io.Serial
    private void writeObject(java.io.ObjectOutputStream oos)
                throws java.io.IOException {
        synchronized(principals) {
            oos.defaultWriteObject();
        }
    }

    /**
     * Reads this object from a stream (i.e., deserializes it)
     *
     * @param  s the {@code ObjectInputStream} from which data is read
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     */
    @SuppressWarnings("unchecked")
    @java.io.Serial
    private void readObject(java.io.ObjectInputStream s)
                throws java.io.IOException, ClassNotFoundException {

        ObjectInputStream.GetField gf = s.readFields();

        readOnly = gf.get("readOnly", false);

        Set<Principal> inputPrincs = (Set<Principal>)gf.get("principals", null);

        Objects.requireNonNull(inputPrincs,
                ResourcesMgr.getString("invalid.null.input.s."));

        // Rewrap the principals into a SecureSet
        try {
            LinkedList<Principal> principalList = collectionNullClean(inputPrincs);
            principals = Collections.synchronizedSet(new SecureSet<>
                                (this, PRINCIPAL_SET, principalList));
        } catch (NullPointerException npe) {
            // Sometimes people deserialize the principals set only.
            // Subject is not accessible, so just don't fail.
            principals = Collections.synchronizedSet
                        (new SecureSet<>(this, PRINCIPAL_SET));
        }

        // The Credential {@code Set} is not serialized, but we do not
        // want the default deserialization routine to set it to null.
        this.pubCredentials = Collections.synchronizedSet
                        (new SecureSet<>(this, PUB_CREDENTIAL_SET));
        this.privCredentials = Collections.synchronizedSet
                        (new SecureSet<>(this, PRIV_CREDENTIAL_SET));
    }

    /**
     * Tests for null-clean collections (both non-null reference and
     * no null elements)
     *
     * @param coll A {@code Collection} to be tested for null references
     *
     * @throws NullPointerException if the specified collection is either
     *            {@code null} or contains a {@code null} element
     */
    private static <E> LinkedList<E> collectionNullClean(
            Collection<? extends E> coll) {

        Objects.requireNonNull(coll,
                ResourcesMgr.getString("invalid.null.input.s."));

        LinkedList<E> output = new LinkedList<>();
        for (E e : coll) {
            output.add(Objects.requireNonNull(e,
                    ResourcesMgr.getString("invalid.null.input.s.")));
        }
        return output;
    }

    /**
     * Prevent modifications unless caller has permission.
     *
     * @serial include
     */
    private static class SecureSet<E>
        implements Set<E>, java.io.Serializable {

        @java.io.Serial
        private static final long serialVersionUID = 7911754171111800359L;

        /**
         * @serialField this$0 Subject The outer Subject instance.
         * @serialField elements LinkedList The elements in this set.
         */
        @java.io.Serial
        private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("this$0", Subject.class),
            new ObjectStreamField("elements", LinkedList.class),
            new ObjectStreamField("which", int.class)
        };

        Subject subject;
        LinkedList<E> elements;

        /**
         * @serial An integer identifying the type of objects contained
         *      in this set.  If {@code which == 1},
         *      this is a Principal set and all the elements are
         *      of type {@code java.security.Principal}.
         *      If {@code which == 2}, this is a public credential
         *      set and all the elements are of type {@code Object}.
         *      If {@code which == 3}, this is a private credential
         *      set and all the elements are of type {@code Object}.
         */
        private int which;

        SecureSet(Subject subject, int which) {
            this.subject = subject;
            this.which = which;
            this.elements = new LinkedList<E>();
        }

        SecureSet(Subject subject, int which, LinkedList<E> list) {
            this.subject = subject;
            this.which = which;
            this.elements = list;
        }

        public int size() {
            return elements.size();
        }

        public Iterator<E> iterator() {
            final LinkedList<E> list = elements;
            return new Iterator<E>() {
                ListIterator<E> i = list.listIterator(0);

                public boolean hasNext() {return i.hasNext();}

                public E next() {
                    if (which != Subject.PRIV_CREDENTIAL_SET) {
                        return i.next();
                    }

                    @SuppressWarnings("removal")
                    SecurityManager sm = System.getSecurityManager();
                    if (sm != null) {
                        try {
                            sm.checkPermission(new PrivateCredentialPermission
                                (list.get(i.nextIndex()).getClass().getName(),
                                subject.getPrincipals()));
                        } catch (SecurityException se) {
                            i.next();
                            throw (se);
                        }
                    }
                    return i.next();
                }

                public void remove() {

                    if (subject.isReadOnly()) {
                        throw new IllegalStateException(ResourcesMgr.getString
                                ("Subject.is.read.only"));
                    }

                    @SuppressWarnings("removal")
                    java.lang.SecurityManager sm = System.getSecurityManager();
                    if (sm != null) {
                        switch (which) {
                        case Subject.PRINCIPAL_SET:
                            sm.checkPermission(AuthPermissionHolder.MODIFY_PRINCIPALS_PERMISSION);
                            break;
                        case Subject.PUB_CREDENTIAL_SET:
                            sm.checkPermission(AuthPermissionHolder.MODIFY_PUBLIC_CREDENTIALS_PERMISSION);
                            break;
                        default:
                            sm.checkPermission(AuthPermissionHolder.MODIFY_PRIVATE_CREDENTIALS_PERMISSION);
                            break;
                        }
                    }
                    i.remove();
                }
            };
        }

        public boolean add(E o) {

            Objects.requireNonNull(o,
                    ResourcesMgr.getString("invalid.null.input.s."));

            if (subject.isReadOnly()) {
                throw new IllegalStateException
                        (ResourcesMgr.getString("Subject.is.read.only"));
            }

            @SuppressWarnings("removal")
            java.lang.SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                switch (which) {
                case Subject.PRINCIPAL_SET:
                    sm.checkPermission(AuthPermissionHolder.MODIFY_PRINCIPALS_PERMISSION);
                    break;
                case Subject.PUB_CREDENTIAL_SET:
                    sm.checkPermission(AuthPermissionHolder.MODIFY_PUBLIC_CREDENTIALS_PERMISSION);
                    break;
                default:
                    sm.checkPermission(AuthPermissionHolder.MODIFY_PRIVATE_CREDENTIALS_PERMISSION);
                    break;
                }
            }

            switch (which) {
            case Subject.PRINCIPAL_SET:
                if (!(o instanceof Principal)) {
                    throw new SecurityException(ResourcesMgr.getString
                        ("attempting.to.add.an.object.which.is.not.an.instance.of.java.security.Principal.to.a.Subject.s.Principal.Set"));
                }
                break;
            default:
                // ok to add Objects of any kind to credential sets
                break;
            }

            // check for duplicates
            if (!elements.contains(o))
                return elements.add(o);
            else {
                return false;
        }
        }

        @SuppressWarnings("removal")
        public boolean remove(Object o) {

            Objects.requireNonNull(o,
                    ResourcesMgr.getString("invalid.null.input.s."));

            final Iterator<E> e = iterator();
            while (e.hasNext()) {
                E next;
                if (which != Subject.PRIV_CREDENTIAL_SET) {
                    next = e.next();
                } else {
                    next = java.security.AccessController.doPrivileged
                        (new java.security.PrivilegedAction<E>() {
                        public E run() {
                            return e.next();
                        }
                    });
                }

                if (next.equals(o)) {
                    e.remove();
                    return true;
                }
            }
            return false;
        }

        @SuppressWarnings("removal")
        public boolean contains(Object o) {

            Objects.requireNonNull(o,
                    ResourcesMgr.getString("invalid.null.input.s."));

            final Iterator<E> e = iterator();
            while (e.hasNext()) {
                E next;
                if (which != Subject.PRIV_CREDENTIAL_SET) {
                    next = e.next();
                } else {

                    // For private credentials:
                    // If the caller does not have read permission for
                    // for o.getClass(), we throw a SecurityException.
                    // Otherwise we check the private cred set to see whether
                    // it contains the Object

                    SecurityManager sm = System.getSecurityManager();
                    if (sm != null) {
                        sm.checkPermission(new PrivateCredentialPermission
                                                (o.getClass().getName(),
                                                subject.getPrincipals()));
                    }
                    next = java.security.AccessController.doPrivileged
                        (new java.security.PrivilegedAction<E>() {
                        public E run() {
                            return e.next();
                        }
                    });
                }

                if (next.equals(o)) {
                    return true;
                }
            }
            return false;
        }

        public boolean addAll(Collection<? extends E> c) {
            boolean result = false;

            c = collectionNullClean(c);

            for (E item : c) {
                result |= this.add(item);
            }

            return result;
        }

        @SuppressWarnings("removal")
        public boolean removeAll(Collection<?> c) {
            c = collectionNullClean(c);

            boolean modified = false;
            final Iterator<E> e = iterator();
            while (e.hasNext()) {
                E next;
                if (which != Subject.PRIV_CREDENTIAL_SET) {
                    next = e.next();
                } else {
                    next = java.security.AccessController.doPrivileged
                        (new java.security.PrivilegedAction<E>() {
                        public E run() {
                            return e.next();
                        }
                    });
                }

                Iterator<?> ce = c.iterator();
                while (ce.hasNext()) {
                    if (next.equals(ce.next())) {
                            e.remove();
                            modified = true;
                            break;
                        }
                }
            }
            return modified;
        }

        public boolean containsAll(Collection<?> c) {
            c = collectionNullClean(c);

            for (Object item : c) {
                if (this.contains(item) == false) {
                    return false;
                }
            }

            return true;
        }

        @SuppressWarnings("removal")
        public boolean retainAll(Collection<?> c) {
            c = collectionNullClean(c);

            boolean modified = false;
            final Iterator<E> e = iterator();
            while (e.hasNext()) {
                E next;
                if (which != Subject.PRIV_CREDENTIAL_SET) {
                    next = e.next();
                } else {
                    next = java.security.AccessController.doPrivileged
                        (new java.security.PrivilegedAction<E>() {
                        public E run() {
                            return e.next();
                        }
                    });
                }

                if (c.contains(next) == false) {
                    e.remove();
                    modified = true;
                }
            }

            return modified;
        }

        @SuppressWarnings("removal")
        public void clear() {
            final Iterator<E> e = iterator();
            while (e.hasNext()) {
                E next;
                if (which != Subject.PRIV_CREDENTIAL_SET) {
                    next = e.next();
                } else {
                    next = java.security.AccessController.doPrivileged
                        (new java.security.PrivilegedAction<E>() {
                        public E run() {
                            return e.next();
                        }
                    });
                }
                e.remove();
            }
        }

        public boolean isEmpty() {
            return elements.isEmpty();
        }

        public Object[] toArray() {
            final Iterator<E> e = iterator();
            while (e.hasNext()) {
                // The next() method performs a security manager check
                // on each element in the SecureSet.  If we make it all
                // the way through we should be able to simply return
                // element's toArray results.  Otherwise we'll let
                // the SecurityException pass up the call stack.
                e.next();
            }

            return elements.toArray();
        }

        public <T> T[] toArray(T[] a) {
            final Iterator<E> e = iterator();
            while (e.hasNext()) {
                // The next() method performs a security manager check
                // on each element in the SecureSet.  If we make it all
                // the way through we should be able to simply return
                // element's toArray results.  Otherwise we'll let
                // the SecurityException pass up the call stack.
                e.next();
            }

            return elements.toArray(a);
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }

            if (!(o instanceof Set)) {
                return false;
            }

            Collection<?> c = (Collection<?>) o;
            if (c.size() != size()) {
                return false;
            }

            try {
                return containsAll(c);
            } catch (ClassCastException unused)   {
                return false;
            } catch (NullPointerException unused) {
                return false;
            }
        }

        public int hashCode() {
            int h = 0;
            Iterator<E> i = iterator();
            while (i.hasNext()) {
                E obj = i.next();
                if (obj != null) {
                    h += obj.hashCode();
                }
            }
            return h;
        }

        /**
         * Writes this object out to a stream (i.e., serializes it).
         *
         * @serialData If this is a private credential set,
         *      a security check is performed to ensure that
         *      the caller has permission to access each credential
         *      in the set.  If the security check passes,
         *      the set is serialized.
         *
         * @param  oos the {@code ObjectOutputStream} to which data is written
         * @throws IOException if an I/O error occurs
         */
        @java.io.Serial
        private void writeObject(java.io.ObjectOutputStream oos)
                throws java.io.IOException {

            if (which == Subject.PRIV_CREDENTIAL_SET) {
                // check permissions before serializing
                Iterator<E> i = iterator();
                while (i.hasNext()) {
                    i.next();
                }
            }
            ObjectOutputStream.PutField fields = oos.putFields();
            fields.put("this$0", subject);
            fields.put("elements", elements);
            fields.put("which", which);
            oos.writeFields();
        }

        /**
         * Restores the state of this object from the stream.
         *
         * @param  ois the {@code ObjectInputStream} from which data is read
         * @throws IOException if an I/O error occurs
         * @throws ClassNotFoundException if a serialized class cannot be loaded
         */
        @SuppressWarnings("unchecked")
        @java.io.Serial
        private void readObject(ObjectInputStream ois)
            throws IOException, ClassNotFoundException
        {
            ObjectInputStream.GetField fields = ois.readFields();
            subject = (Subject) fields.get("this$0", null);
            which = fields.get("which", 0);

            LinkedList<E> tmp = (LinkedList<E>) fields.get("elements", null);

            elements = Subject.collectionNullClean(tmp);
        }

    }

    /**
     * This class implements a {@code Set} which returns only
     * members that are an instance of a specified Class.
     */
    private class ClassSet<T> extends AbstractSet<T> {

        private int which;
        private Class<T> c;
        private Set<T> set;

        ClassSet(int which, Class<T> c) {
            this.which = which;
            this.c = c;
            set = new HashSet<T>();

            switch (which) {
            case Subject.PRINCIPAL_SET:
                synchronized(principals) { populateSet(); }
                break;
            case Subject.PUB_CREDENTIAL_SET:
                synchronized(pubCredentials) { populateSet(); }
                break;
            default:
                synchronized(privCredentials) { populateSet(); }
                break;
            }
        }

        @SuppressWarnings({"removal","unchecked"})     /*To suppress warning from line 1374*/
        private void populateSet() {
            final Iterator<?> iterator;
            switch(which) {
            case Subject.PRINCIPAL_SET:
                iterator = Subject.this.principals.iterator();
                break;
            case Subject.PUB_CREDENTIAL_SET:
                iterator = Subject.this.pubCredentials.iterator();
                break;
            default:
                iterator = Subject.this.privCredentials.iterator();
                break;
            }

            // Check whether the caller has permisson to get
            // credentials of Class c

            while (iterator.hasNext()) {
                Object next;
                if (which == Subject.PRIV_CREDENTIAL_SET) {
                    next = java.security.AccessController.doPrivileged
                        (new java.security.PrivilegedAction<>() {
                        public Object run() {
                            return iterator.next();
                        }
                    });
                } else {
                    next = iterator.next();
                }
                if (c.isAssignableFrom(next.getClass())) {
                    if (which != Subject.PRIV_CREDENTIAL_SET) {
                        set.add((T)next);
                    } else {
                        // Check permission for private creds
                        SecurityManager sm = System.getSecurityManager();
                        if (sm != null) {
                            sm.checkPermission(new PrivateCredentialPermission
                                                (next.getClass().getName(),
                                                Subject.this.getPrincipals()));
                        }
                        set.add((T)next);
                    }
                }
            }
        }

        @Override
        public int size() {
            return set.size();
        }

        @Override
        public Iterator<T> iterator() {
            return set.iterator();
        }

        @Override
        public boolean add(T o) {

            if (!c.isAssignableFrom(o.getClass())) {
                MessageFormat form = new MessageFormat(ResourcesMgr.getString
                        ("attempting.to.add.an.object.which.is.not.an.instance.of.class"));
                Object[] source = {c.toString()};
                throw new SecurityException(form.format(source));
            }

            return set.add(o);
        }
    }

    static final class AuthPermissionHolder {
        static final AuthPermission DO_AS_PERMISSION =
            new AuthPermission("doAs");

        static final AuthPermission DO_AS_PRIVILEGED_PERMISSION =
            new AuthPermission("doAsPrivileged");

        static final AuthPermission SET_READ_ONLY_PERMISSION =
            new AuthPermission("setReadOnly");

        static final AuthPermission GET_SUBJECT_PERMISSION =
            new AuthPermission("getSubject");

        static final AuthPermission MODIFY_PRINCIPALS_PERMISSION =
            new AuthPermission("modifyPrincipals");

        static final AuthPermission MODIFY_PUBLIC_CREDENTIALS_PERMISSION =
            new AuthPermission("modifyPublicCredentials");

        static final AuthPermission MODIFY_PRIVATE_CREDENTIALS_PERMISSION =
            new AuthPermission("modifyPrivateCredentials");
    }
}
