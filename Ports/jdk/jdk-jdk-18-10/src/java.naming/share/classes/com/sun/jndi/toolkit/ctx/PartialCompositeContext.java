/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.toolkit.ctx;

import java.util.Hashtable;
import java.util.Enumeration;

import javax.naming.*;
import javax.naming.spi.Resolver;
import javax.naming.spi.ResolveResult;
import javax.naming.spi.NamingManager;

/**
  * PartialCompositeContext implements Context operations on
  * composite names using implementations of the p_ interfaces
  * defined by its subclasses.
  *
  * The main purpose provided by this class is that it deals with
  * partial resolutions and continuations, so that callers of the
  * Context operation don't have to.
  *
  * Types of clients that will be direct subclasses of
  * PartialCompositeContext may be service providers that implement
  * one of the JNDI protocols, but which do not deal with
  * continuations.  Usually, service providers will be using
  * one of the subclasses of PartialCompositeContext.
  *
  * @author Rosanna Lee
  */


public abstract class PartialCompositeContext implements Context, Resolver {
    protected static final int _PARTIAL = 1;
    protected static final int _COMPONENT = 2;
    protected static final int _ATOMIC = 3;

    protected int _contextType = _PARTIAL;

    static final CompositeName _EMPTY_NAME = new CompositeName();
    static CompositeName _NNS_NAME;

    static {
        try {
            _NNS_NAME = new CompositeName("/");
        } catch (InvalidNameException e) {
            // Should never happen
        }
    }

    protected PartialCompositeContext() {
    }

// ------ Abstract methods whose implementations come from subclasses

    /* Equivalent to method in  Resolver interface */
    protected abstract ResolveResult p_resolveToClass(Name name,
        Class<?> contextType, Continuation cont) throws NamingException;

    /* Equivalent to methods in Context interface */
    protected abstract Object p_lookup(Name name, Continuation cont)
        throws NamingException;
    protected abstract Object p_lookupLink(Name name, Continuation cont)
        throws NamingException;
    protected abstract NamingEnumeration<NameClassPair> p_list(Name name,
        Continuation cont) throws NamingException;
    protected abstract NamingEnumeration<Binding> p_listBindings(Name name,
        Continuation cont) throws NamingException;
    protected abstract void p_bind(Name name, Object obj, Continuation cont)
        throws NamingException;
    protected abstract void p_rebind(Name name, Object obj, Continuation cont)
        throws NamingException;
    protected abstract void p_unbind(Name name, Continuation cont)
        throws NamingException;
    protected abstract void p_destroySubcontext(Name name, Continuation cont)
        throws NamingException;
    protected abstract Context p_createSubcontext(Name name, Continuation cont)
        throws NamingException;
    protected abstract void p_rename(Name oldname, Name newname,
                                     Continuation cont)
        throws NamingException;
    protected abstract NameParser p_getNameParser(Name name, Continuation cont)
        throws NamingException;

// ------ should be overridden by subclass;
// ------ not abstract only for backward compatibility

    /**
     * A cheap way of getting the environment.
     * Default implementation is NOT cheap because it simply calls
     * getEnvironment(), which most implementations clone before returning.
     * Subclass should ALWAYS override this with the cheapest possible way.
     * The toolkit knows to clone when necessary.
     * @return The possibly null environment of the context.
     */
    protected Hashtable<?,?> p_getEnvironment() throws NamingException {
        return getEnvironment();
    }


// ------ implementations of methods in Resolver and Context
// ------ using corresponding p_ methods provided by subclass

    /* implementations for method in Resolver interface using p_ method */

    public ResolveResult resolveToClass(String name,
                                        Class<? extends Context> contextType)
        throws NamingException
    {
        return resolveToClass(new CompositeName(name), contextType);
    }

    public ResolveResult resolveToClass(Name name,
                                        Class<? extends Context> contextType)
        throws NamingException
    {
        PartialCompositeContext ctx = this;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);
        ResolveResult answer;
        Name nm = name;

        try {
            answer = ctx.p_resolveToClass(nm, contextType, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCContext(cont);
                answer = ctx.p_resolveToClass(nm, contextType, cont);
            }
        } catch (CannotProceedException e) {
            Context cctx = NamingManager.getContinuationContext(e);
            if (!(cctx instanceof Resolver)) {
                throw e;
            }
            answer = ((Resolver)cctx).resolveToClass(e.getRemainingName(),
                                                     contextType);
        }
        return answer;
    }

    /* implementations for methods in Context interface using p_ methods */

    public Object lookup(String name) throws NamingException {
        return lookup(new CompositeName(name));
    }

    public Object lookup(Name name) throws NamingException {
        PartialCompositeContext ctx = this;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);
        Object answer;
        Name nm = name;

        try {
            answer = ctx.p_lookup(nm, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCContext(cont);
                answer = ctx.p_lookup(nm, cont);
            }
        } catch (CannotProceedException e) {
            Context cctx = NamingManager.getContinuationContext(e);
            answer = cctx.lookup(e.getRemainingName());
        }
        return answer;
    }

    public void bind(String name, Object newObj) throws NamingException {
        bind(new CompositeName(name), newObj);
    }

    public void bind(Name name, Object newObj) throws NamingException {
        PartialCompositeContext ctx = this;
        Name nm = name;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);

        try {
            ctx.p_bind(nm, newObj, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCContext(cont);
                ctx.p_bind(nm, newObj, cont);
            }
        } catch (CannotProceedException e) {
            Context cctx = NamingManager.getContinuationContext(e);
            cctx.bind(e.getRemainingName(), newObj);
        }
    }

    public void rebind(String name, Object newObj) throws NamingException {
        rebind(new CompositeName(name), newObj);
    }
    public void rebind(Name name, Object newObj) throws NamingException {
        PartialCompositeContext ctx = this;
        Name nm = name;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);

        try {
            ctx.p_rebind(nm, newObj, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCContext(cont);
                ctx.p_rebind(nm, newObj, cont);
            }
        } catch (CannotProceedException e) {
            Context cctx = NamingManager.getContinuationContext(e);
            cctx.rebind(e.getRemainingName(), newObj);
        }
    }

    public void unbind(String name) throws NamingException {
        unbind(new CompositeName(name));
    }
    public void unbind(Name name) throws NamingException {
        PartialCompositeContext ctx = this;
        Name nm = name;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);

        try {
            ctx.p_unbind(nm, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCContext(cont);
                ctx.p_unbind(nm, cont);
            }
        } catch (CannotProceedException e) {
            Context cctx = NamingManager.getContinuationContext(e);
            cctx.unbind(e.getRemainingName());
        }
    }

    public void rename(String oldName, String newName) throws NamingException {
        rename(new CompositeName(oldName), new CompositeName(newName));
    }
    public void rename(Name oldName, Name newName)
        throws NamingException
    {
        PartialCompositeContext ctx = this;
        Name nm = oldName;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(oldName, env);

        try {
            ctx.p_rename(nm, newName, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCContext(cont);
                ctx.p_rename(nm, newName, cont);
            }
        } catch (CannotProceedException e) {
            Context cctx = NamingManager.getContinuationContext(e);
            if (e.getRemainingNewName() != null) {
                // %%% e.getRemainingNewName() should never be null
                newName = e.getRemainingNewName();
            }
            cctx.rename(e.getRemainingName(), newName);
        }
    }

    public NamingEnumeration<NameClassPair> list(String name)
        throws NamingException
    {
        return list(new CompositeName(name));
    }

    public NamingEnumeration<NameClassPair> list(Name name)
        throws NamingException
    {
        PartialCompositeContext ctx = this;
        Name nm = name;
        NamingEnumeration<NameClassPair> answer;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);

        try {
            answer = ctx.p_list(nm, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCContext(cont);
                answer = ctx.p_list(nm, cont);
            }
        } catch (CannotProceedException e) {
            Context cctx = NamingManager.getContinuationContext(e);
            answer = cctx.list(e.getRemainingName());
        }
        return answer;
    }

    public NamingEnumeration<Binding> listBindings(String name)
        throws NamingException
    {
        return listBindings(new CompositeName(name));
    }

    public NamingEnumeration<Binding> listBindings(Name name)
        throws NamingException
    {
        PartialCompositeContext ctx = this;
        Name nm = name;
        NamingEnumeration<Binding> answer;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);

        try {
            answer = ctx.p_listBindings(nm, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCContext(cont);
                answer = ctx.p_listBindings(nm, cont);
            }
        } catch (CannotProceedException e) {
            Context cctx = NamingManager.getContinuationContext(e);
            answer = cctx.listBindings(e.getRemainingName());
        }
        return answer;
    }

    public void destroySubcontext(String name) throws NamingException {
        destroySubcontext(new CompositeName(name));
    }

    public void destroySubcontext(Name name) throws NamingException {
        PartialCompositeContext ctx = this;
        Name nm = name;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);

        try {
            ctx.p_destroySubcontext(nm, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCContext(cont);
                ctx.p_destroySubcontext(nm, cont);
            }
        } catch (CannotProceedException e) {
            Context cctx = NamingManager.getContinuationContext(e);
            cctx.destroySubcontext(e.getRemainingName());
        }
    }

    public Context createSubcontext(String name) throws NamingException {
        return createSubcontext(new CompositeName(name));
    }

    public Context createSubcontext(Name name) throws NamingException {
        PartialCompositeContext ctx = this;
        Name nm = name;
        Context answer;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);

        try {
            answer = ctx.p_createSubcontext(nm, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCContext(cont);
                answer = ctx.p_createSubcontext(nm, cont);
            }
        } catch (CannotProceedException e) {
            Context cctx = NamingManager.getContinuationContext(e);
            answer = cctx.createSubcontext(e.getRemainingName());
        }
        return answer;
    }

    public Object lookupLink(String name) throws NamingException {
        return lookupLink(new CompositeName(name));
    }

    public Object lookupLink(Name name) throws NamingException {
        PartialCompositeContext ctx = this;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);
        Object answer;
        Name nm = name;

        try {
            answer = ctx.p_lookupLink(nm, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCContext(cont);
                answer = ctx.p_lookupLink(nm, cont);
            }
        } catch (CannotProceedException e) {
            Context cctx = NamingManager.getContinuationContext(e);
            answer = cctx.lookupLink(e.getRemainingName());
        }
        return answer;
    }

    public NameParser getNameParser(String name) throws NamingException {
        return getNameParser(new CompositeName(name));
    }

    public NameParser getNameParser(Name name) throws NamingException {
        PartialCompositeContext ctx = this;
        Name nm = name;
        NameParser answer;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);

        try {
            answer = ctx.p_getNameParser(nm, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCContext(cont);
                answer = ctx.p_getNameParser(nm, cont);
            }
        } catch (CannotProceedException e) {
            Context cctx = NamingManager.getContinuationContext(e);
            answer = cctx.getNameParser(e.getRemainingName());
        }
        return answer;
    }

    public String composeName(String name, String prefix)
            throws NamingException {
        Name fullName = composeName(new CompositeName(name),
                                    new CompositeName(prefix));
        return fullName.toString();
    }

    /**
     * This default implementation simply concatenates the two names.
     * There's one twist when the "java.naming.provider.compose.elideEmpty"
     * environment setting is set to "true":  if each name contains a
     * nonempty component, and if 'prefix' ends with an empty component or
     * 'name' starts with one, then one empty component is dropped.
     * For example:
     * <pre>{@code
     *                            elideEmpty=false     elideEmpty=true
     * {"a"} + {"b"}          =>  {"a", "b"}           {"a", "b"}
     * {"a"} + {""}           =>  {"a", ""}            {"a", ""}
     * {"a"} + {"", "b"}      =>  {"a", "", "b"}       {"a", "b"}
     * {"a", ""} + {"b", ""}  =>  {"a", "", "b", ""}   {"a", "b", ""}
     * {"a", ""} + {"", "b"}  =>  {"a", "", "", "b"}   {"a", "", "b"}
     * }</pre>
     */
    public Name composeName(Name name, Name prefix) throws NamingException {
        Name res = (Name)prefix.clone();
        if (name == null) {
            return res;
        }
        res.addAll(name);

        String elide = (String)
            p_getEnvironment().get("java.naming.provider.compose.elideEmpty");
        if (elide == null || !elide.equalsIgnoreCase("true")) {
            return res;
        }

        int len = prefix.size();

        if (!allEmpty(prefix) && !allEmpty(name)) {
            if (res.get(len - 1).isEmpty()) {
                res.remove(len - 1);
            } else if (res.get(len).isEmpty()) {
                res.remove(len);
            }
        }
        return res;
    }


// ------ internal methods used by PartialCompositeContext

    /**
     * Tests whether a name contains a nonempty component.
     */
    protected static boolean allEmpty(Name name) {
        Enumeration<String> enum_ = name.getAll();
        while (enum_.hasMoreElements()) {
            if (!enum_.nextElement().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Retrieves a PartialCompositeContext for the resolved object in
     * cont.  Throws CannotProceedException if not successful.
     */
    protected static PartialCompositeContext getPCContext(Continuation cont)
            throws NamingException {

        Object obj = cont.getResolvedObj();
        PartialCompositeContext pctx = null;

        if (obj instanceof PartialCompositeContext) {
            // Just cast if octx already is PartialCompositeContext
            // %%% ignoring environment for now
            return (PartialCompositeContext)obj;
        } else {
            throw cont.fillInException(new CannotProceedException());
        }
    }
};
