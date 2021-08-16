/*
 * Copyright (c) 1999, 2011, Oracle and/or its affiliates. All rights reserved.
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

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.spi.DirectoryManager;

/*
 * Inherit from AtomicContext so that subclasses of PartialCompositeDirContext
 * can get the ns methods defined in subclasses of PartialCompositeContext.
 *
 * Direct subclasses of DirContext should provide implementations for
 * the p_ abstract DirContext methods and override the p_ Context methods
 * (not abstract anymore because they are overridden by ComponentContext
 * (the superclass of AtomicContext)).
 *
 * @author Rosanna Lee
 */

public abstract class PartialCompositeDirContext
        extends AtomicContext implements DirContext {

    protected PartialCompositeDirContext() {
        _contextType = _PARTIAL;
    }

// ------ Abstract methods whose implementation come from subclasses

     /* Equivalent to DirContext methods */
     protected abstract Attributes p_getAttributes(Name name, String[] attrIds,
                                                     Continuation cont)
         throws NamingException;

     protected abstract void p_modifyAttributes(Name name, int mod_op,
                                                Attributes attrs,
                                                Continuation cont)
         throws NamingException;

     protected abstract void p_modifyAttributes(Name name,
                                                ModificationItem[] mods,
                                                Continuation cont)
         throws NamingException;

     protected abstract void p_bind(Name name, Object obj,
                                    Attributes attrs,
                                    Continuation cont)
         throws NamingException;

     protected abstract void p_rebind(Name name, Object obj,
                                      Attributes attrs,
                                      Continuation cont)
         throws NamingException;

     protected abstract DirContext p_createSubcontext(Name name,
                                                     Attributes attrs,
                                                     Continuation cont)
         throws NamingException;

     protected abstract NamingEnumeration<SearchResult> p_search(
                            Name name,
                            Attributes matchingAttributes,
                            String[] attributesToReturn,
                            Continuation cont)
         throws NamingException;

     protected abstract NamingEnumeration<SearchResult> p_search(
                            Name name,
                            String filter,
                            SearchControls cons,
                            Continuation cont)
         throws NamingException;

     protected abstract NamingEnumeration<SearchResult> p_search(
                            Name name,
                            String filterExpr,
                            Object[] filterArgs,
                            SearchControls cons,
                            Continuation cont)
         throws NamingException;

     protected abstract DirContext p_getSchema(Name name, Continuation cont)
         throws NamingException;

     protected abstract DirContext p_getSchemaClassDefinition(Name name,
                                                             Continuation cont)
         throws NamingException;

// ------ implementation for DirContext methods using
// ------ corresponding p_ methods

    public Attributes getAttributes(String name)
            throws NamingException {
        return getAttributes(name, null);
    }

    public Attributes getAttributes(Name name)
            throws NamingException {
        return getAttributes(name, null);
    }

    public Attributes getAttributes(String name, String[] attrIds)
            throws NamingException {
        return getAttributes(new CompositeName(name), attrIds);
    }

    public Attributes getAttributes(Name name, String[] attrIds)
            throws NamingException {
        PartialCompositeDirContext ctx = this;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);
        Attributes answer;
        Name nm = name;

        try {
            answer = ctx.p_getAttributes(nm, attrIds, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCDirContext(cont);
                answer = ctx.p_getAttributes(nm, attrIds, cont);
            }
        } catch (CannotProceedException e) {
            DirContext cctx = DirectoryManager.getContinuationDirContext(e);
            answer = cctx.getAttributes(e.getRemainingName(), attrIds);
        }
        return answer;
    }

    public void modifyAttributes(String name, int mod_op, Attributes attrs)
            throws NamingException {
        modifyAttributes(new CompositeName(name), mod_op, attrs);
    }

    public void modifyAttributes(Name name, int mod_op, Attributes attrs)
            throws NamingException {
        PartialCompositeDirContext ctx = this;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);
        Name nm = name;

        try {
            ctx.p_modifyAttributes(nm, mod_op, attrs, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCDirContext(cont);
                ctx.p_modifyAttributes(nm, mod_op, attrs, cont);
            }
        } catch (CannotProceedException e) {
            DirContext cctx = DirectoryManager.getContinuationDirContext(e);
            cctx.modifyAttributes(e.getRemainingName(), mod_op, attrs);
        }
    }

    public void modifyAttributes(String name, ModificationItem[] mods)
            throws NamingException {
        modifyAttributes(new CompositeName(name), mods);
    }

    public void modifyAttributes(Name name, ModificationItem[] mods)
            throws NamingException {
        PartialCompositeDirContext ctx = this;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);
        Name nm = name;

        try {
            ctx.p_modifyAttributes(nm, mods, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCDirContext(cont);
                ctx.p_modifyAttributes(nm, mods, cont);
            }
        } catch (CannotProceedException e) {
            DirContext cctx = DirectoryManager.getContinuationDirContext(e);
            cctx.modifyAttributes(e.getRemainingName(), mods);
        }
    }

    public void bind(String name, Object obj, Attributes attrs)
            throws NamingException {
        bind(new CompositeName(name), obj, attrs);
    }

    public void bind(Name name, Object obj, Attributes attrs)
            throws NamingException {
        PartialCompositeDirContext ctx = this;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);
        Name nm = name;

        try {
            ctx.p_bind(nm, obj, attrs, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCDirContext(cont);
                ctx.p_bind(nm, obj, attrs, cont);
            }
        } catch (CannotProceedException e) {
            DirContext cctx = DirectoryManager.getContinuationDirContext(e);
            cctx.bind(e.getRemainingName(), obj, attrs);
        }
    }

    public void rebind(String name, Object obj, Attributes attrs)
            throws NamingException {
        rebind(new CompositeName(name), obj, attrs);
    }

    public void rebind(Name name, Object obj, Attributes attrs)
            throws NamingException {
        PartialCompositeDirContext ctx = this;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);
        Name nm = name;

        try {
            ctx.p_rebind(nm, obj, attrs, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCDirContext(cont);
                ctx.p_rebind(nm, obj, attrs, cont);
            }
        } catch (CannotProceedException e) {
            DirContext cctx = DirectoryManager.getContinuationDirContext(e);
            cctx.rebind(e.getRemainingName(), obj, attrs);
        }
    }

    public DirContext createSubcontext(String name, Attributes attrs)
            throws NamingException {
        return createSubcontext(new CompositeName(name), attrs);
    }

    public DirContext createSubcontext(Name name, Attributes attrs)
            throws NamingException {
        PartialCompositeDirContext ctx = this;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);
        DirContext answer;
        Name nm = name;

        try {
            answer = ctx.p_createSubcontext(nm, attrs, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCDirContext(cont);
                answer = ctx.p_createSubcontext(nm, attrs, cont);
            }
        } catch (CannotProceedException e) {
            DirContext cctx = DirectoryManager.getContinuationDirContext(e);
            answer = cctx.createSubcontext(e.getRemainingName(), attrs);
        }
        return answer;
    }

    public NamingEnumeration<SearchResult>
        search(String name, Attributes matchingAttributes)
        throws NamingException
    {
        return search(name, matchingAttributes, null);
    }

    public NamingEnumeration<SearchResult>
        search(Name name, Attributes matchingAttributes)
        throws NamingException
    {
        return search(name, matchingAttributes, null);
    }

    public NamingEnumeration<SearchResult>
        search(String name,
               Attributes matchingAttributes,
               String[] attributesToReturn)
        throws NamingException
    {
        return search(new CompositeName(name),
                      matchingAttributes, attributesToReturn);
    }

    public NamingEnumeration<SearchResult>
        search(Name name,
               Attributes matchingAttributes,
               String[] attributesToReturn)
        throws NamingException
    {

        PartialCompositeDirContext ctx = this;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);
        NamingEnumeration<SearchResult> answer;
        Name nm = name;

        try {
            answer = ctx.p_search(nm, matchingAttributes,
                                  attributesToReturn, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCDirContext(cont);
                answer = ctx.p_search(nm, matchingAttributes,
                                      attributesToReturn, cont);
            }
        } catch (CannotProceedException e) {
            DirContext cctx = DirectoryManager.getContinuationDirContext(e);
            answer = cctx.search(e.getRemainingName(), matchingAttributes,
                                 attributesToReturn);
        }
        return answer;
    }

    public NamingEnumeration<SearchResult>
        search(String name,
               String filter,
               SearchControls cons)
        throws NamingException
    {
        return search(new CompositeName(name), filter, cons);
    }

    public NamingEnumeration<SearchResult>
        search(Name name,
               String filter,
               SearchControls cons)
        throws NamingException
    {

        PartialCompositeDirContext ctx = this;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);
        NamingEnumeration<SearchResult> answer;
        Name nm = name;

        try {
            answer = ctx.p_search(nm, filter, cons, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCDirContext(cont);
                answer = ctx.p_search(nm, filter, cons, cont);
            }
        } catch (CannotProceedException e) {
            DirContext cctx = DirectoryManager.getContinuationDirContext(e);
            answer = cctx.search(e.getRemainingName(), filter, cons);
        }
        return answer;
    }

    public NamingEnumeration<SearchResult>
        search(String name,
               String filterExpr,
               Object[] filterArgs,
               SearchControls cons)
        throws NamingException
    {
        return search(new CompositeName(name), filterExpr, filterArgs, cons);
    }

    public NamingEnumeration<SearchResult>
        search(Name name,
               String filterExpr,
               Object[] filterArgs,
               SearchControls cons)
        throws NamingException
    {

        PartialCompositeDirContext ctx = this;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);
        NamingEnumeration<SearchResult> answer;
        Name nm = name;

        try {
            answer = ctx.p_search(nm, filterExpr, filterArgs, cons, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCDirContext(cont);
                answer = ctx.p_search(nm, filterExpr, filterArgs, cons, cont);
            }
        } catch (CannotProceedException e) {
            DirContext cctx = DirectoryManager.getContinuationDirContext(e);
            answer = cctx.search(e.getRemainingName(), filterExpr, filterArgs,
                                 cons);
        }
        return answer;
    }

    public DirContext getSchema(String name) throws NamingException {
        return getSchema(new CompositeName(name));
    }

    public DirContext getSchema(Name name) throws NamingException {
        PartialCompositeDirContext ctx = this;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);
        DirContext answer;
        Name nm = name;

        try {
            answer = ctx.p_getSchema(nm, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCDirContext(cont);
                answer = ctx.p_getSchema(nm, cont);
            }
        } catch (CannotProceedException e) {
            DirContext cctx = DirectoryManager.getContinuationDirContext(e);
            answer = cctx.getSchema(e.getRemainingName());
        }
        return answer;
    }

    public DirContext getSchemaClassDefinition(String name)
            throws NamingException {
        return getSchemaClassDefinition(new CompositeName(name));
    }

    public DirContext getSchemaClassDefinition(Name name)
            throws NamingException {
        PartialCompositeDirContext ctx = this;
        Hashtable<?,?> env = p_getEnvironment();
        Continuation cont = new Continuation(name, env);
        DirContext answer;
        Name nm = name;

        try {
            answer = ctx.p_getSchemaClassDefinition(nm, cont);
            while (cont.isContinue()) {
                nm = cont.getRemainingName();
                ctx = getPCDirContext(cont);
                answer = ctx.p_getSchemaClassDefinition(nm, cont);
            }
        } catch (CannotProceedException e) {
            DirContext cctx = DirectoryManager.getContinuationDirContext(e);
            answer = cctx.getSchemaClassDefinition(e.getRemainingName());
        }
        return answer;
    }

// ------ internal method used by PartialCompositeDirContext

    /**
     * Retrieves a PartialCompositeDirContext for the resolved object in
     * cont.  Throws CannotProceedException if not successful.
     */
    protected static PartialCompositeDirContext getPCDirContext(Continuation cont)
            throws NamingException {

        PartialCompositeContext pctx =
            PartialCompositeContext.getPCContext(cont);

        if (!(pctx instanceof PartialCompositeDirContext)) {
            throw cont.fillInException(
                    new NotContextException(
                            "Resolved object is not a DirContext."));
        }

        return (PartialCompositeDirContext)pctx;
    }


//------ Compensation for inheriting from AtomicContext

    /*
     * Dummy implementations defined here so that direct subclasses
     * of PartialCompositeDirContext or ComponentDirContext do not
     * have to provide dummy implementations for these.
     * Override these for subclasses of AtomicDirContext.
     */

    protected StringHeadTail c_parseComponent(String inputName,
        Continuation cont) throws NamingException {
            OperationNotSupportedException e = new
                OperationNotSupportedException();
            throw cont.fillInException(e);
        }

    protected Object a_lookup(String name, Continuation cont)
        throws NamingException {
            OperationNotSupportedException e = new
                OperationNotSupportedException();
            throw cont.fillInException(e);
        }

    protected Object a_lookupLink(String name, Continuation cont)
        throws NamingException {
            OperationNotSupportedException e = new
                OperationNotSupportedException();
            throw cont.fillInException(e);
        }

    protected NamingEnumeration<NameClassPair> a_list(
        Continuation cont) throws NamingException {
            OperationNotSupportedException e = new
                OperationNotSupportedException();
            throw cont.fillInException(e);
        }

    protected NamingEnumeration<Binding> a_listBindings(
        Continuation cont) throws NamingException {
            OperationNotSupportedException e = new
                OperationNotSupportedException();
            throw cont.fillInException(e);
        }

    protected void a_bind(String name, Object obj, Continuation cont)
        throws NamingException {
            OperationNotSupportedException e = new
                OperationNotSupportedException();
            throw cont.fillInException(e);
        }

    protected void a_rebind(String name, Object obj, Continuation cont)
        throws NamingException {
            OperationNotSupportedException e = new
                OperationNotSupportedException();
            throw cont.fillInException(e);
        }

    protected void a_unbind(String name, Continuation cont)
        throws NamingException {
            OperationNotSupportedException e = new
                OperationNotSupportedException();
            throw cont.fillInException(e);
        }

    protected void a_destroySubcontext(String name, Continuation cont)
        throws NamingException {
            OperationNotSupportedException e = new
                OperationNotSupportedException();
            throw cont.fillInException(e);
        }

    protected Context a_createSubcontext(String name, Continuation cont)
        throws NamingException {
            OperationNotSupportedException e = new
                OperationNotSupportedException();
            throw cont.fillInException(e);
        }

    protected void a_rename(String oldname, Name newname,
        Continuation cont) throws NamingException {
            OperationNotSupportedException e = new
                OperationNotSupportedException();
            throw cont.fillInException(e);
        }

    protected NameParser a_getNameParser(Continuation cont)
        throws NamingException {
            OperationNotSupportedException e = new
                OperationNotSupportedException();
            throw cont.fillInException(e);
        }
}
