/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.ldap;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.spi.*;
import javax.naming.ldap.*;

import java.util.Hashtable;
import java.util.StringTokenizer;
import com.sun.jndi.toolkit.dir.SearchFilter;

/**
 * A context for handling referrals.
 *
 * @author Vincent Ryan
 */
final class LdapReferralContext implements DirContext, LdapContext {

    private DirContext refCtx = null;
    private Name urlName = null;   // override the supplied name
    private String urlAttrs = null;  // override attributes
    private String urlScope = null;  // override scope
    private String urlFilter = null; // override filter

    private LdapReferralException refEx = null;
    private boolean skipThisReferral = false;
    private int hopCount = 1;
    private NamingException previousEx = null;

    @SuppressWarnings("unchecked") // clone()
    LdapReferralContext(LdapReferralException ex,
        Hashtable<?,?> env,
        Control[] connCtls,
        Control[] reqCtls,
        String nextName,
        boolean skipThisReferral,
        int handleReferrals) throws NamingException {

        refEx = ex;

        if (this.skipThisReferral = skipThisReferral) {
            return; // don't create a DirContext for this referral
        }

        String referral;

        // Make copies of environment and connect controls for our own use.
        if (env != null) {
            env = (Hashtable<?,?>) env.clone();
            // Remove old connect controls from environment, unless we have new
            // ones that will override them anyway.
            if (connCtls == null) {
                env.remove(LdapCtx.BIND_CONTROLS);
            }
        } else if (connCtls != null) {
            env = new Hashtable<String, Control[]>(5);
        }
        if (connCtls != null) {
            Control[] copiedCtls = new Control[connCtls.length];
            System.arraycopy(connCtls, 0, copiedCtls, 0, connCtls.length);
            // Add copied controls to environment, replacing any old ones.
            ((Hashtable<? super String, ? super Control[]>)env)
                    .put(LdapCtx.BIND_CONTROLS, copiedCtls);
        }

        while (true) {
            try {
                referral = refEx.getNextReferral();
                if (referral == null) {
                    if (previousEx != null) {
                        throw (NamingException)(previousEx.fillInStackTrace());
                    } else {
                        throw new NamingException(
                            "Illegal encoding: referral is empty");
                    }
                }

            } catch (LdapReferralException e) {

                if (handleReferrals == LdapClient.LDAP_REF_THROW) {
                    throw e;
                } else {
                    refEx = e;
                    continue;
                }
            }

            // Create a Reference containing the referral URL.
            Reference ref = new Reference("javax.naming.directory.DirContext",
                                          new StringRefAddr("URL", referral));

            Object obj;
            try {
                obj = NamingManager.getObjectInstance(ref, null, null, env);

            } catch (NamingException e) {

                if (handleReferrals == LdapClient.LDAP_REF_THROW) {
                    throw e;
                }

                // mask the exception and save it for later
                previousEx = e;

                // follow another referral
                continue;

            } catch (Exception e) {
                NamingException e2 =
                    new NamingException(
                        "problem generating object using object factory");
                e2.setRootCause(e);
                throw e2;
            }
            if (obj instanceof DirContext) {
                refCtx = (DirContext)obj;
                if (refCtx instanceof LdapContext && reqCtls != null) {
                    ((LdapContext)refCtx).setRequestControls(reqCtls);
                }
                initDefaults(referral, nextName);

                break;
            } else {
                NamingException ne = new NotContextException(
                    "Cannot create context for: " + referral);
                ne.setRemainingName((new CompositeName()).add(nextName));
                throw ne;
            }
        }
    }

    private void initDefaults(String referral, String nextName)
        throws NamingException {
        String urlString;
        try {
            // parse URL
            LdapURL url = new LdapURL(referral);
            urlString = url.getDN();
            urlAttrs = url.getAttributes();
            urlScope = url.getScope();
            urlFilter = url.getFilter();

        } catch (NamingException e) {
            // Not an LDAP URL; use original URL
            urlString = referral;
            urlAttrs = urlScope = urlFilter = null;
        }

        // reuse original name if URL DN is absent
        if (urlString == null) {
            urlString = nextName;
        } else {
            // concatenate with remaining name if URL DN is present
            urlString = "";
        }

        if (urlString == null) {
            urlName = null;
        } else {
            urlName = urlString.isEmpty() ? new CompositeName() :
                new CompositeName().add(urlString);
        }
    }


    public void close() throws NamingException {
        if (refCtx != null) {
            refCtx.close();
            refCtx = null;
        }
        refEx = null;
    }

    void setHopCount(int hopCount) {
        this.hopCount = hopCount;
        if ((refCtx != null) && (refCtx instanceof LdapCtx)) {
            ((LdapCtx)refCtx).setHopCount(hopCount);
        }
    }

    public Object lookup(String name) throws NamingException {
        return lookup(toName(name));
    }

    public Object lookup(Name name) throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        return refCtx.lookup(overrideName(name));
    }

    public void bind(String name, Object obj) throws NamingException {
        bind(toName(name), obj);
    }

    public void bind(Name name, Object obj) throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        refCtx.bind(overrideName(name), obj);
    }

    public void rebind(String name, Object obj) throws NamingException {
        rebind(toName(name), obj);
    }

    public void rebind(Name name, Object obj) throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        refCtx.rebind(overrideName(name), obj);
    }

    public void unbind(String name) throws NamingException {
        unbind(toName(name));
    }

    public void unbind(Name name) throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        refCtx.unbind(overrideName(name));
    }

    public void rename(String oldName, String newName) throws NamingException {
        rename(toName(oldName), toName(newName));
    }

    public void rename(Name oldName, Name newName) throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        refCtx.rename(overrideName(oldName), toName(refEx.getNewRdn()));
    }

    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        return list(toName(name));
    }

    @SuppressWarnings("unchecked")
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }
        try {
            NamingEnumeration<NameClassPair> ne = null;

            if (urlScope != null && urlScope.equals("base")) {
                SearchControls cons = new SearchControls();
                cons.setReturningObjFlag(true);
                cons.setSearchScope(SearchControls.OBJECT_SCOPE);

                ne = (NamingEnumeration)
                        refCtx.search(overrideName(name), "(objectclass=*)", cons);

            } else {
                ne = refCtx.list(overrideName(name));
            }

            refEx.setNameResolved(true);

            // append (referrals from) the exception that generated this
            // context to the new search results, so that referral processing
            // can continue
            ((ReferralEnumeration)ne).appendUnprocessedReferrals(refEx);

            return (ne);

        } catch (LdapReferralException e) {

            // append (referrals from) the exception that generated this
            // context to the new exception, so that referral processing
            // can continue

            e.appendUnprocessedReferrals(refEx);
            throw (NamingException)(e.fillInStackTrace());

        } catch (NamingException e) {

            // record the exception if there are no remaining referrals
            if ((refEx != null) && (! refEx.hasMoreReferrals())) {
                refEx.setNamingException(e);
            }
            if ((refEx != null) &&
                (refEx.hasMoreReferrals() ||
                 refEx.hasMoreReferralExceptions())) {
                throw (NamingException)
                    ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
            } else {
                throw e;
            }
        }
    }

    public NamingEnumeration<Binding> listBindings(String name) throws
            NamingException {
        return listBindings(toName(name));
    }

    @SuppressWarnings("unchecked")
    public NamingEnumeration<Binding> listBindings(Name name) throws
            NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        try {
            NamingEnumeration<Binding> be = null;

            if (urlScope != null && urlScope.equals("base")) {
                SearchControls cons = new SearchControls();
                cons.setReturningObjFlag(true);
                cons.setSearchScope(SearchControls.OBJECT_SCOPE);

                be = (NamingEnumeration)refCtx.search(overrideName(name),
                        "(objectclass=*)", cons);

            } else {
                be = refCtx.listBindings(overrideName(name));
            }

            refEx.setNameResolved(true);

            // append (referrals from) the exception that generated this
            // context to the new search results, so that referral processing
            // can continue
            ((ReferralEnumeration<Binding>)be).appendUnprocessedReferrals(refEx);

            return (be);

        } catch (LdapReferralException e) {

            // append (referrals from) the exception that generated this
            // context to the new exception, so that referral processing
            // can continue

            e.appendUnprocessedReferrals(refEx);
            throw (NamingException)(e.fillInStackTrace());

        } catch (NamingException e) {

            // record the exception if there are no remaining referrals
            if ((refEx != null) && (! refEx.hasMoreReferrals())) {
                refEx.setNamingException(e);
            }
            if ((refEx != null) &&
                (refEx.hasMoreReferrals() ||
                 refEx.hasMoreReferralExceptions())) {
                throw (NamingException)
                    ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
            } else {
                throw e;
            }
        }
    }

    public void destroySubcontext(String name) throws NamingException {
        destroySubcontext(toName(name));
    }

    public void destroySubcontext(Name name) throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        refCtx.destroySubcontext(overrideName(name));
    }

    public Context createSubcontext(String name) throws NamingException {
        return createSubcontext(toName(name));
    }

    public Context createSubcontext(Name name) throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        return refCtx.createSubcontext(overrideName(name));
    }

    public Object lookupLink(String name) throws NamingException {
        return lookupLink(toName(name));
    }

    public Object lookupLink(Name name) throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        return refCtx.lookupLink(overrideName(name));
    }

    public NameParser getNameParser(String name) throws NamingException {
        return getNameParser(toName(name));
    }

    public NameParser getNameParser(Name name) throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        return refCtx.getNameParser(overrideName(name));
    }

    public String composeName(String name, String prefix)
            throws NamingException {
                return composeName(toName(name), toName(prefix)).toString();
    }

    public Name composeName(Name name, Name prefix) throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }
        return refCtx.composeName(name, prefix);
    }

    public Object addToEnvironment(String propName, Object propVal)
            throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        return refCtx.addToEnvironment(propName, propVal);
    }

    public Object removeFromEnvironment(String propName)
            throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        return refCtx.removeFromEnvironment(propName);
    }

    public Hashtable<?,?> getEnvironment() throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        return refCtx.getEnvironment();
    }

    public Attributes getAttributes(String name) throws NamingException {
        return getAttributes(toName(name));
    }

    public Attributes getAttributes(Name name) throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        return refCtx.getAttributes(overrideName(name));
    }

    public Attributes getAttributes(String name, String[] attrIds)
            throws NamingException {
        return getAttributes(toName(name), attrIds);
    }

    public Attributes getAttributes(Name name, String[] attrIds)
            throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        return refCtx.getAttributes(overrideName(name), attrIds);
    }

    public void modifyAttributes(String name, int mod_op, Attributes attrs)
            throws NamingException {
        modifyAttributes(toName(name), mod_op, attrs);
    }

    public void modifyAttributes(Name name, int mod_op, Attributes attrs)
            throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        refCtx.modifyAttributes(overrideName(name), mod_op, attrs);
    }

    public void modifyAttributes(String name, ModificationItem[] mods)
            throws NamingException {
        modifyAttributes(toName(name), mods);
    }

    public void modifyAttributes(Name name, ModificationItem[] mods)
            throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        refCtx.modifyAttributes(overrideName(name), mods);
    }

    public void bind(String name, Object obj, Attributes attrs)
            throws NamingException {
        bind(toName(name), obj, attrs);
    }

    public void bind(Name name, Object obj, Attributes attrs)
            throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        refCtx.bind(overrideName(name), obj, attrs);
    }

    public void rebind(String name, Object obj, Attributes attrs)
            throws NamingException {
        rebind(toName(name), obj, attrs);
    }

    public void rebind(Name name, Object obj, Attributes attrs)
            throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        refCtx.rebind(overrideName(name), obj, attrs);
    }

    public DirContext createSubcontext(String name, Attributes attrs)
            throws NamingException {
        return createSubcontext(toName(name), attrs);
    }

    public DirContext createSubcontext(Name name, Attributes attrs)
            throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        return refCtx.createSubcontext(overrideName(name), attrs);
    }

    public DirContext getSchema(String name) throws NamingException {
        return getSchema(toName(name));
    }

    public DirContext getSchema(Name name) throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        return refCtx.getSchema(overrideName(name));
    }

    public DirContext getSchemaClassDefinition(String name)
            throws NamingException {
        return getSchemaClassDefinition(toName(name));
    }

    public DirContext getSchemaClassDefinition(Name name)
            throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

      return refCtx.getSchemaClassDefinition(overrideName(name));
    }

    public NamingEnumeration<SearchResult> search(String name,
                                                  Attributes matchingAttributes)
            throws NamingException {
        return search(toName(name), SearchFilter.format(matchingAttributes),
            new SearchControls());
    }

    public NamingEnumeration<SearchResult> search(Name name,
                                                  Attributes matchingAttributes)
            throws NamingException {
        return search(name, SearchFilter.format(matchingAttributes),
            new SearchControls());
    }

    public NamingEnumeration<SearchResult> search(String name,
                                                  Attributes matchingAttributes,
                                                  String[] attributesToReturn)
            throws NamingException {
        SearchControls cons = new SearchControls();
        cons.setReturningAttributes(attributesToReturn);

        return search(toName(name), SearchFilter.format(matchingAttributes),
            cons);
    }

    public NamingEnumeration<SearchResult> search(Name name,
                                                  Attributes matchingAttributes,
                                                  String[] attributesToReturn)
            throws NamingException {
        SearchControls cons = new SearchControls();
        cons.setReturningAttributes(attributesToReturn);

        return search(name, SearchFilter.format(matchingAttributes), cons);
    }

    public NamingEnumeration<SearchResult> search(String name,
                                                  String filter,
                                                  SearchControls cons)
            throws NamingException {
        return search(toName(name), filter, cons);
    }

    public NamingEnumeration<SearchResult> search(Name name,
                                                  String filter,
        SearchControls cons) throws NamingException {

        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        try {
            NamingEnumeration<SearchResult> se =
                    refCtx.search(overrideName(name),
                                  overrideFilter(filter),
                                  overrideAttributesAndScope(cons));

            refEx.setNameResolved(true);

            // append (referrals from) the exception that generated this
            // context to the new search results, so that referral processing
            // can continue
            ((ReferralEnumeration)se).appendUnprocessedReferrals(refEx);

            return (se);

        } catch (LdapReferralException e) {

            // %%% setNameResolved(true);

            // append (referrals from) the exception that generated this
            // context to the new exception, so that referral processing
            // can continue

            e.appendUnprocessedReferrals(refEx);
            throw (NamingException)(e.fillInStackTrace());

        } catch (NamingException e) {

            // record the exception if there are no remaining referrals
            if ((refEx != null) && (! refEx.hasMoreReferrals())) {
                refEx.setNamingException(e);
            }
            if ((refEx != null) &&
                (refEx.hasMoreReferrals() ||
                 refEx.hasMoreReferralExceptions())) {
                throw (NamingException)
                    ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
            } else {
                throw e;
            }
        }
    }

    public NamingEnumeration<SearchResult> search(String name,
                                                  String filterExpr,
                                                  Object[] filterArgs,
                                                  SearchControls cons)
            throws NamingException {
        return search(toName(name), filterExpr, filterArgs, cons);
    }

    public NamingEnumeration<SearchResult> search(Name name,
        String filterExpr,
        Object[] filterArgs,
        SearchControls cons) throws NamingException {

        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        try {
            NamingEnumeration<SearchResult> se;

            if (urlFilter != null) {
                se = refCtx.search(overrideName(name), urlFilter,
                overrideAttributesAndScope(cons));
            } else {
                se = refCtx.search(overrideName(name), filterExpr,
                filterArgs, overrideAttributesAndScope(cons));
            }

            refEx.setNameResolved(true);

            // append (referrals from) the exception that generated this
            // context to the new search results, so that referral processing
            // can continue
            ((ReferralEnumeration)se).appendUnprocessedReferrals(refEx);

            return (se);

        } catch (LdapReferralException e) {

            // append (referrals from) the exception that generated this
            // context to the new exception, so that referral processing
            // can continue

            e.appendUnprocessedReferrals(refEx);
            throw (NamingException)(e.fillInStackTrace());

        } catch (NamingException e) {

            // record the exception if there are no remaining referrals
            if ((refEx != null) && (! refEx.hasMoreReferrals())) {
                refEx.setNamingException(e);
            }
            if ((refEx != null) &&
                (refEx.hasMoreReferrals() ||
                 refEx.hasMoreReferralExceptions())) {
                throw (NamingException)
                    ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
            } else {
                throw e;
            }
        }
    }

    public String getNameInNamespace() throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }
        return urlName != null && !urlName.isEmpty() ? urlName.get(0) : "";
    }

    // ---------------------- LdapContext ---------------------

    public ExtendedResponse extendedOperation(ExtendedRequest request)
        throws NamingException {

        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        if (!(refCtx instanceof LdapContext)) {
            throw new NotContextException(
                "Referral context not an instance of LdapContext");
        }

        return ((LdapContext)refCtx).extendedOperation(request);
    }

    public LdapContext newInstance(Control[] requestControls)
        throws NamingException {

        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        if (!(refCtx instanceof LdapContext)) {
            throw new NotContextException(
                "Referral context not an instance of LdapContext");
        }

        return ((LdapContext)refCtx).newInstance(requestControls);
    }

    public void reconnect(Control[] connCtls) throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        if (!(refCtx instanceof LdapContext)) {
            throw new NotContextException(
                "Referral context not an instance of LdapContext");
        }

        ((LdapContext)refCtx).reconnect(connCtls);
    }

    public Control[] getConnectControls() throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        if (!(refCtx instanceof LdapContext)) {
            throw new NotContextException(
                "Referral context not an instance of LdapContext");
        }

        return ((LdapContext)refCtx).getConnectControls();
    }

    public void setRequestControls(Control[] requestControls)
        throws NamingException {

        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        if (!(refCtx instanceof LdapContext)) {
            throw new NotContextException(
                "Referral context not an instance of LdapContext");
        }

        ((LdapContext)refCtx).setRequestControls(requestControls);
    }

    public Control[] getRequestControls() throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        if (!(refCtx instanceof LdapContext)) {
            throw new NotContextException(
                "Referral context not an instance of LdapContext");
        }
        return ((LdapContext)refCtx).getRequestControls();
    }

    public Control[] getResponseControls() throws NamingException {
        if (skipThisReferral) {
            throw (NamingException)
                ((refEx.appendUnprocessedReferrals(null)).fillInStackTrace());
        }

        if (!(refCtx instanceof LdapContext)) {
            throw new NotContextException(
                "Referral context not an instance of LdapContext");
        }
        return ((LdapContext)refCtx).getResponseControls();
    }

    // ---------------------- Private methods  ---------------------
    private Name toName(String name) throws InvalidNameException {
        return name.isEmpty() ? new CompositeName() :
            new CompositeName().add(name);
    }

    /*
     * Use the DN component from the LDAP URL (if present) to override the
     * supplied DN.
     */
    private Name overrideName(Name name) throws InvalidNameException {
        return (urlName == null ? name : urlName);
    }

    /*
     * Use the attributes and scope components from the LDAP URL (if present)
     * to override the corresponding components supplied in SearchControls.
     */
    private SearchControls overrideAttributesAndScope(SearchControls cons) {
        SearchControls urlCons;

        if ((urlScope != null) || (urlAttrs != null)) {
            urlCons = new SearchControls(cons.getSearchScope(),
                                        cons.getCountLimit(),
                                        cons.getTimeLimit(),
                                        cons.getReturningAttributes(),
                                        cons.getReturningObjFlag(),
                                        cons.getDerefLinkFlag());

            if (urlScope != null) {
                if (urlScope.equals("base")) {
                    urlCons.setSearchScope(SearchControls.OBJECT_SCOPE);
                } else if (urlScope.equals("one")) {
                    urlCons.setSearchScope(SearchControls.ONELEVEL_SCOPE);
                } else if (urlScope.equals("sub")) {
                    urlCons.setSearchScope(SearchControls.SUBTREE_SCOPE);
                }
            }

            if (urlAttrs != null) {
                StringTokenizer tokens = new StringTokenizer(urlAttrs, ",");
                int count = tokens.countTokens();
                String[] attrs = new String[count];
                for (int i = 0; i < count; i ++) {
                    attrs[i] = tokens.nextToken();
                }
                urlCons.setReturningAttributes(attrs);
            }

            return urlCons;

        } else {
            return cons;
        }
    }

    /*
     * Use the filter component from the LDAP URL (if present) to override the
     * supplied filter.
     */
    private String overrideFilter(String filter) {
        return (urlFilter == null ? filter : urlFilter);
    }

}
