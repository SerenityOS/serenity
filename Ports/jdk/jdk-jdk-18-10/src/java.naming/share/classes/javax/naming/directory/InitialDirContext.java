/*
 * Copyright (c) 1999, 2009, Oracle and/or its affiliates. All rights reserved.
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


package javax.naming.directory;

import java.util.Hashtable;
import javax.naming.*;

/**
 * This class is the starting context for performing
 * directory operations. The documentation in the class description
 * of InitialContext (including those for synchronization) apply here.
 *
 *
 * @author Rosanna Lee
 * @author Scott Seligman
 *
 * @see javax.naming.InitialContext
 * @since 1.3
 */

public class InitialDirContext extends InitialContext implements DirContext {

    /**
     * Constructs an initial DirContext with the option of not
     * initializing it.  This may be used by a constructor in
     * a subclass when the value of the environment parameter
     * is not yet known at the time the {@code InitialDirContext}
     * constructor is called.  The subclass's constructor will
     * call this constructor, compute the value of the environment,
     * and then call {@code init()} before returning.
     *
     * @param lazy
     *          true means do not initialize the initial DirContext; false
     *          is equivalent to calling {@code new InitialDirContext()}
     * @throws  NamingException if a naming exception is encountered
     *
     * @see InitialContext#init(Hashtable)
     * @since 1.3
     */
    protected InitialDirContext(boolean lazy) throws NamingException {
        super(lazy);
    }

    /**
     * Constructs an initial DirContext.
     * No environment properties are supplied.
     * Equivalent to {@code new InitialDirContext(null)}.
     *
     * @throws  NamingException if a naming exception is encountered
     *
     * @see #InitialDirContext(Hashtable)
     */
    public InitialDirContext() throws NamingException {
        super();
    }

    /**
     * Constructs an initial DirContext using the supplied environment.
     * Environment properties are discussed in the
     * {@code javax.naming.InitialContext} class description.
     *
     * <p> If the {@code java.naming.provider.url} property of the supplied
     * environment consists of a URL (or a list of URLs) using the ldap
     * protocol the resulting {@link javax.naming.ldap.LdapContext} will use
     * an LDAP server resolved by the configured {@link
     * javax.naming.ldap.spi.LdapDnsProvider LdapDnsProviders}:
     * <ol>
     * <li>If this is the first {@code InitialDirContext} created with a
     *     {@code java.naming.provider.url} using the ldap protocol then the
     *     {@linkplain java.util.ServiceLoader ServiceLoader} mechanism is
     *     used to locate {@linkplain javax.naming.ldap.spi.LdapDnsProvider
     *     LdapDnsProvider} implementations using the system class loader.
     *     The order that providers are located is implementation specific
     *     and an implementation is free to cache the located providers.
     * <li>The {@code lookupEndpoints} method of each provider, if instantiated,
     *     is invoked once with a combination of each of the URLs in the the
     *     {@code java.naming.provider.url} property and the environment until
     *     a provider returns non-empty or all providers have been exhausted.
     *     If none of the
     *     {@linkplain javax.naming.ldap.spi.LdapDnsProvider LdapDnsProviders}
     *     return a non-empty
     *     {@linkplain javax.naming.ldap.spi.LdapDnsProviderResult result} then
     *     the implementation will make a best-effort attempt to determine an
     *     endpoint. A
     *     {@linkplain java.util.ServiceConfigurationError ServiceConfigurationError},
     *     {@code Error} or {@code RuntimeException} thrown when loading or
     *     calling an {@linkplain javax.naming.ldap.spi.LdapDnsProvider
     *     LdapDnsProvider}, if encountered, will be propagated to the calling
     *     thread.
     * </ol>
     *
     * <p> This constructor will not modify {@code environment}
     * or save a reference to it, but may save a clone.
     * Caller should not modify mutable keys and values in
     * {@code environment} after it has been passed to the constructor.
     *
     * @param environment
     *          environment used to create the initial DirContext.
     *          Null indicates an empty environment.
     *
     * @throws  NamingException if a naming exception is encountered
     */
    public InitialDirContext(Hashtable<?,?> environment)
        throws NamingException
    {
        super(environment);
    }

    private DirContext getURLOrDefaultInitDirCtx(String name)
            throws NamingException {
        Context answer = getURLOrDefaultInitCtx(name);
        if (!(answer instanceof DirContext)) {
            if (answer == null) {
                throw new NoInitialContextException();
            } else {
                throw new NotContextException(
                    "Not an instance of DirContext");
            }
        }
        return (DirContext)answer;
    }

    private DirContext getURLOrDefaultInitDirCtx(Name name)
            throws NamingException {
        Context answer = getURLOrDefaultInitCtx(name);
        if (!(answer instanceof DirContext)) {
            if (answer == null) {
                throw new NoInitialContextException();
            } else {
                throw new NotContextException(
                    "Not an instance of DirContext");
            }
        }
        return (DirContext)answer;
    }

// DirContext methods
// Most Javadoc is deferred to the DirContext interface.

    public Attributes getAttributes(String name)
            throws NamingException {
        return getAttributes(name, null);
    }

    public Attributes getAttributes(String name, String[] attrIds)
            throws NamingException {
        return getURLOrDefaultInitDirCtx(name).getAttributes(name, attrIds);
    }

    public Attributes getAttributes(Name name)
            throws NamingException {
        return getAttributes(name, null);
    }

    public Attributes getAttributes(Name name, String[] attrIds)
            throws NamingException {
        return getURLOrDefaultInitDirCtx(name).getAttributes(name, attrIds);
    }

    public void modifyAttributes(String name, int mod_op, Attributes attrs)
            throws NamingException {
        getURLOrDefaultInitDirCtx(name).modifyAttributes(name, mod_op, attrs);
    }

    public void modifyAttributes(Name name, int mod_op, Attributes attrs)
            throws NamingException  {
        getURLOrDefaultInitDirCtx(name).modifyAttributes(name, mod_op, attrs);
    }

    public void modifyAttributes(String name, ModificationItem[] mods)
            throws NamingException  {
        getURLOrDefaultInitDirCtx(name).modifyAttributes(name, mods);
    }

    public void modifyAttributes(Name name, ModificationItem[] mods)
            throws NamingException  {
        getURLOrDefaultInitDirCtx(name).modifyAttributes(name, mods);
    }

    public void bind(String name, Object obj, Attributes attrs)
            throws NamingException  {
        getURLOrDefaultInitDirCtx(name).bind(name, obj, attrs);
    }

    public void bind(Name name, Object obj, Attributes attrs)
            throws NamingException  {
        getURLOrDefaultInitDirCtx(name).bind(name, obj, attrs);
    }

    public void rebind(String name, Object obj, Attributes attrs)
            throws NamingException  {
        getURLOrDefaultInitDirCtx(name).rebind(name, obj, attrs);
    }

    public void rebind(Name name, Object obj, Attributes attrs)
            throws NamingException  {
        getURLOrDefaultInitDirCtx(name).rebind(name, obj, attrs);
    }

    public DirContext createSubcontext(String name, Attributes attrs)
            throws NamingException  {
        return getURLOrDefaultInitDirCtx(name).createSubcontext(name, attrs);
    }

    public DirContext createSubcontext(Name name, Attributes attrs)
            throws NamingException  {
        return getURLOrDefaultInitDirCtx(name).createSubcontext(name, attrs);
    }

    public DirContext getSchema(String name) throws NamingException {
        return getURLOrDefaultInitDirCtx(name).getSchema(name);
    }

    public DirContext getSchema(Name name) throws NamingException {
        return getURLOrDefaultInitDirCtx(name).getSchema(name);
    }

    public DirContext getSchemaClassDefinition(String name)
            throws NamingException {
        return getURLOrDefaultInitDirCtx(name).getSchemaClassDefinition(name);
    }

    public DirContext getSchemaClassDefinition(Name name)
            throws NamingException {
        return getURLOrDefaultInitDirCtx(name).getSchemaClassDefinition(name);
    }

// -------------------- search operations

    public NamingEnumeration<SearchResult>
        search(String name, Attributes matchingAttributes)
        throws NamingException
    {
        return getURLOrDefaultInitDirCtx(name).search(name, matchingAttributes);
    }

    public NamingEnumeration<SearchResult>
        search(Name name, Attributes matchingAttributes)
        throws NamingException
    {
        return getURLOrDefaultInitDirCtx(name).search(name, matchingAttributes);
    }

    public NamingEnumeration<SearchResult>
        search(String name,
               Attributes matchingAttributes,
               String[] attributesToReturn)
        throws NamingException
    {
        return getURLOrDefaultInitDirCtx(name).search(name,
                                                      matchingAttributes,
                                                      attributesToReturn);
    }

    public NamingEnumeration<SearchResult>
        search(Name name,
               Attributes matchingAttributes,
               String[] attributesToReturn)
        throws NamingException
    {
        return getURLOrDefaultInitDirCtx(name).search(name,
                                            matchingAttributes,
                                            attributesToReturn);
    }

    public NamingEnumeration<SearchResult>
        search(String name,
               String filter,
               SearchControls cons)
        throws NamingException
    {
        return getURLOrDefaultInitDirCtx(name).search(name, filter, cons);
    }

    public NamingEnumeration<SearchResult>
        search(Name name,
               String filter,
               SearchControls cons)
        throws NamingException
    {
        return getURLOrDefaultInitDirCtx(name).search(name, filter, cons);
    }

    public NamingEnumeration<SearchResult>
        search(String name,
               String filterExpr,
               Object[] filterArgs,
               SearchControls cons)
        throws NamingException
    {
        return getURLOrDefaultInitDirCtx(name).search(name, filterExpr,
                                                      filterArgs, cons);
    }

    public NamingEnumeration<SearchResult>
        search(Name name,
               String filterExpr,
               Object[] filterArgs,
               SearchControls cons)
        throws NamingException
    {
        return getURLOrDefaultInitDirCtx(name).search(name, filterExpr,
                                                      filterArgs, cons);
    }
}
