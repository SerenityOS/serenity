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

import javax.naming.*;
import javax.naming.directory.*;

/**
 * Direct subclasses of AtomicDirContext must provide implementations for
 * the abstract a_ DirContext methods, and override the a_ Context methods
 * (which are no longer abstract because they have been overriden by
 * PartialCompositeDirContext with dummy implementations).
 *
 * If the subclass implements the notion of implicit nns,
 * it must override the a_*_nns DirContext and Context methods as well.
 *
 * @author Rosanna Lee
 *
 */

public abstract class AtomicDirContext extends ComponentDirContext {

    protected AtomicDirContext() {
        _contextType = _ATOMIC;
    }

// ------ Abstract methods whose implementations come from subclass

    protected abstract Attributes a_getAttributes(String name, String[] attrIds,
                                                    Continuation cont)
        throws NamingException;

    protected abstract void a_modifyAttributes(String name, int mod_op,
                                               Attributes attrs,
                                               Continuation cont)
        throws NamingException;

    protected abstract void a_modifyAttributes(String name,
                                               ModificationItem[] mods,
                                               Continuation cont)
        throws NamingException;

    protected abstract void a_bind(String name, Object obj,
                                   Attributes attrs,
                                   Continuation cont)
        throws NamingException;

    protected abstract void a_rebind(String name, Object obj,
                                     Attributes attrs,
                                     Continuation cont)
        throws NamingException;

    protected abstract DirContext a_createSubcontext(String name,
                                                    Attributes attrs,
                                                    Continuation cont)
        throws NamingException;

    protected abstract NamingEnumeration<SearchResult> a_search(
                                                  Attributes matchingAttributes,
                                                  String[] attributesToReturn,
                                                  Continuation cont)
        throws NamingException;

    protected abstract NamingEnumeration<SearchResult> a_search(
                                                  String name,
                                                  String filterExpr,
                                                  Object[] filterArgs,
                                                  SearchControls cons,
                                                  Continuation cont)
        throws NamingException;

    protected abstract NamingEnumeration<SearchResult> a_search(
                                                  String name,
                                                  String filter,
                                                  SearchControls cons,
                                                  Continuation cont)
        throws NamingException;

    protected abstract DirContext a_getSchema(Continuation cont)
        throws NamingException;

    protected abstract DirContext a_getSchemaClassDefinition(Continuation cont)
        throws NamingException;

// ------ Methods that need to be overridden by subclass

    //  default implementations of a_*_nns methods

    // The following methods are called when the DirContext methods
    // are invoked with a name that has a trailing slash.
    // For naming systems that support implicit nns,
    // the trailing slash signifies the implicit nns.
    // For such naming systems, override these a_*_nns methods.
    //
    // For naming systems that support junctions (explicit nns),
    // the trailing slash is meaningless because a junction does not
    // have an implicit nns.  The default implementation here
    // throws a NameNotFoundException for such names.
    // If a context wants to accept a trailing slash as having
    // the same meaning as the same name without a trailing slash,
    // then it should override these a_*_nns methods.

    protected Attributes a_getAttributes_nns(String name,
                                               String[] attrIds,
                                               Continuation cont)
        throws NamingException  {
            a_processJunction_nns(name, cont);
            return null;
        }

    protected void a_modifyAttributes_nns(String name, int mod_op,
                                          Attributes attrs,
                                          Continuation cont)
        throws NamingException {
            a_processJunction_nns(name, cont);
        }

    protected void a_modifyAttributes_nns(String name,
                                          ModificationItem[] mods,
                                          Continuation cont)
        throws NamingException {
            a_processJunction_nns(name, cont);
        }

    protected void a_bind_nns(String name, Object obj,
                              Attributes attrs,
                              Continuation cont)
        throws NamingException  {
            a_processJunction_nns(name, cont);
        }

    protected void a_rebind_nns(String name, Object obj,
                                Attributes attrs,
                                Continuation cont)
        throws NamingException  {
            a_processJunction_nns(name, cont);
        }

    protected DirContext a_createSubcontext_nns(String name,
                                               Attributes attrs,
                                               Continuation cont)
        throws NamingException  {
            a_processJunction_nns(name, cont);
            return null;
        }

    protected NamingEnumeration<SearchResult> a_search_nns(
                                             Attributes matchingAttributes,
                                             String[] attributesToReturn,
                                             Continuation cont)
        throws NamingException {
            a_processJunction_nns(cont);
            return null;
        }

    protected NamingEnumeration<SearchResult> a_search_nns(String name,
                                                           String filterExpr,
                                                           Object[] filterArgs,
                                                           SearchControls cons,
                                                           Continuation cont)
        throws NamingException {
            a_processJunction_nns(name, cont);
            return null;
        }

    protected NamingEnumeration<SearchResult> a_search_nns(String name,
                                                           String filter,
                                                           SearchControls cons,
                                                           Continuation cont)
        throws NamingException  {
            a_processJunction_nns(name, cont);
            return null;
        }

    protected DirContext a_getSchema_nns(Continuation cont) throws NamingException {
        a_processJunction_nns(cont);
        return null;
    }

    protected DirContext a_getSchemaDefinition_nns(Continuation cont)
        throws NamingException {
            a_processJunction_nns(cont);
            return null;
        }

// ------- implementations of c_ DirContext methods using corresponding
// ------- a_ and a_*_nns methods

    protected Attributes c_getAttributes(Name name, String[] attrIds,
                                           Continuation cont)
        throws NamingException  {
            if (resolve_to_penultimate_context(name, cont))
                return a_getAttributes(name.toString(), attrIds, cont);
            return null;
        }

    protected void c_modifyAttributes(Name name, int mod_op,
                                      Attributes attrs, Continuation cont)
        throws NamingException {
            if (resolve_to_penultimate_context(name, cont))
                a_modifyAttributes(name.toString(), mod_op, attrs, cont);
        }

    protected void c_modifyAttributes(Name name, ModificationItem[] mods,
                                      Continuation cont)
        throws NamingException {
            if (resolve_to_penultimate_context(name, cont))
                a_modifyAttributes(name.toString(), mods, cont);
        }

    protected void c_bind(Name name, Object obj,
                          Attributes attrs, Continuation cont)
        throws NamingException  {
            if (resolve_to_penultimate_context(name, cont))
                a_bind(name.toString(), obj, attrs, cont);
        }

    protected void c_rebind(Name name, Object obj,
                            Attributes attrs, Continuation cont)
        throws NamingException  {
            if (resolve_to_penultimate_context(name, cont))
                a_rebind(name.toString(), obj, attrs, cont);
        }

    protected DirContext c_createSubcontext(Name name,
                                           Attributes attrs,
                                           Continuation cont)
        throws NamingException  {
            if (resolve_to_penultimate_context(name, cont))
                return a_createSubcontext(name.toString(),
                                          attrs, cont);
            return null;
        }

    protected NamingEnumeration<SearchResult> c_search(Name name,
                                         Attributes matchingAttributes,
                                         String[] attributesToReturn,
                                         Continuation cont)
        throws NamingException  {
            if (resolve_to_context(name, cont))
                return a_search(matchingAttributes, attributesToReturn, cont);
            return null;
        }

    protected NamingEnumeration<SearchResult> c_search(Name name,
                                                       String filter,
                                                       SearchControls cons,
                                                       Continuation cont)
        throws NamingException {
            if (resolve_to_penultimate_context(name, cont))
                return a_search(name.toString(), filter, cons, cont);
            return null;
        }

    protected NamingEnumeration<SearchResult> c_search(Name name,
                                                       String filterExpr,
                                                       Object[] filterArgs,
                                                       SearchControls cons,
                                                       Continuation cont)
        throws NamingException  {
            if (resolve_to_penultimate_context(name, cont))
                return a_search(name.toString(), filterExpr, filterArgs, cons, cont);
            return null;
        }

    protected DirContext c_getSchema(Name name, Continuation cont)
        throws NamingException  {
            if (resolve_to_context(name, cont))
                return a_getSchema(cont);
            return null;
        }

    protected DirContext c_getSchemaClassDefinition(Name name, Continuation cont)
        throws NamingException  {
            if (resolve_to_context(name, cont))
                return a_getSchemaClassDefinition(cont);
            return null;
        }

    /* equivalent to methods in DirContext interface for nns */

    protected Attributes c_getAttributes_nns(Name name, String[] attrIds,
                                           Continuation cont)
        throws NamingException  {
            if (resolve_to_penultimate_context_nns(name, cont))
                return a_getAttributes_nns(name.toString(), attrIds, cont);
            return null;
        }

    protected void c_modifyAttributes_nns(Name name, int mod_op,
                                          Attributes attrs, Continuation cont)
        throws NamingException {
            if (resolve_to_penultimate_context_nns(name, cont))
                a_modifyAttributes_nns(name.toString(), mod_op, attrs, cont);
        }

    protected void c_modifyAttributes_nns(Name name, ModificationItem[] mods,
                                      Continuation cont)
        throws NamingException {
            if (resolve_to_penultimate_context_nns(name, cont))
                a_modifyAttributes_nns(name.toString(), mods, cont);
        }

    protected void c_bind_nns(Name name, Object obj,
                              Attributes attrs, Continuation cont)
        throws NamingException  {
            if (resolve_to_penultimate_context_nns(name, cont))
                a_bind_nns(name.toString(), obj, attrs, cont);
        }

    protected void c_rebind_nns(Name name, Object obj,
                                Attributes attrs, Continuation cont)
        throws NamingException  {
            if (resolve_to_penultimate_context_nns(name, cont))
                a_rebind_nns(name.toString(), obj, attrs, cont);
        }

    protected DirContext c_createSubcontext_nns(Name name,
                                               Attributes attrs,
                                               Continuation cont)
        throws NamingException  {
            if (resolve_to_penultimate_context_nns(name, cont))
                return a_createSubcontext_nns(name.toString(), attrs, cont);
            return null;
        }

    protected NamingEnumeration<SearchResult> c_search_nns(
                                         Name name,
                                         Attributes matchingAttributes,
                                         String[] attributesToReturn,
                                         Continuation cont)
        throws NamingException  {
            resolve_to_nns_and_continue(name, cont);
            return null;
        }

    protected NamingEnumeration<SearchResult> c_search_nns(Name name,
                                                           String filter,
                                                           SearchControls cons,
                                                           Continuation cont)
        throws NamingException {
            if (resolve_to_penultimate_context_nns(name, cont))
                return a_search_nns(name.toString(), filter, cons, cont);
            return null;
        }

    protected NamingEnumeration<SearchResult> c_search_nns(Name name,
                                                           String filterExpr,
                                                           Object[] filterArgs,
                                                           SearchControls cons,
                                                           Continuation cont)
        throws NamingException  {
            if (resolve_to_penultimate_context_nns(name, cont))
                return a_search_nns(name.toString(), filterExpr, filterArgs,
                                    cons, cont);
            return null;
        }

    protected DirContext c_getSchema_nns(Name name, Continuation cont)
        throws NamingException  {
            resolve_to_nns_and_continue(name, cont);
            return null;
        }

    protected DirContext c_getSchemaClassDefinition_nns(Name name, Continuation cont)
        throws NamingException  {
            resolve_to_nns_and_continue(name, cont);
            return null;
        }
}
