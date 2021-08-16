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

/* Direct subclasses of ComponentDirContext must provide implementations for
 * the abstract c_ DirContext methods, and override the c_ Context methods
 * (which are no longer abstract because they have been overriden by
 * AtomicContext, the direct superclass of PartialDSCompositeContext).
 *
 * If the subclass is supports implicit nns, it must override all the
 * c_*_nns methods corresponding to those in DirContext and Context.
 * See ComponentContext for details.
 *
 * @author Rosanna Lee
 */

public abstract class ComponentDirContext extends PartialCompositeDirContext {

    protected ComponentDirContext () {
        _contextType = _COMPONENT;
    }

// ------ Abstract methods whose implementations are provided by subclass

    /* Equivalent to methods in DirContext */
    protected abstract Attributes c_getAttributes(Name name,
                                                 String[] attrIds,
                                                 Continuation cont)
        throws NamingException;

    protected abstract void c_modifyAttributes(Name name, int mod_op,
                                            Attributes attrs,
                                            Continuation cont)
            throws NamingException;

    protected abstract void c_modifyAttributes(Name name,
                                            ModificationItem[] mods,
                                            Continuation cont)
        throws NamingException;

    protected abstract void c_bind(Name name, Object obj,
                                   Attributes attrs,
                                   Continuation cont)
        throws NamingException;

    protected abstract void c_rebind(Name name, Object obj,
                                     Attributes attrs,
                                     Continuation cont)
        throws NamingException;

    protected abstract DirContext c_createSubcontext(Name name,
                                                    Attributes attrs,
                                                    Continuation cont)
        throws NamingException;

    protected abstract NamingEnumeration<SearchResult> c_search(
                            Name name,
                            Attributes matchingAttributes,
                            String[] attributesToReturn,
                            Continuation cont)
        throws NamingException;

    protected abstract NamingEnumeration<SearchResult> c_search(
                            Name name,
                            String filter,
                            SearchControls cons,
                            Continuation cont)
        throws NamingException;

    protected abstract NamingEnumeration<SearchResult> c_search(
                            Name name,
                            String filterExpr,
                            Object[] filterArgs,
                            SearchControls cons,
                            Continuation cont)
        throws NamingException;

    protected abstract DirContext c_getSchema(Name name, Continuation cont)
        throws NamingException;

    protected abstract DirContext c_getSchemaClassDefinition(Name name,
                                                            Continuation cont)
        throws NamingException;

// ------- default implementations of c_*_nns methods from DirContext

    // The following methods are called when the DirContext methods
    // are invoked with a name that has a trailing slash.
    // For naming systems that support implicit nns,
    // the trailing slash signifies the implicit nns.
    // For such naming systems, override these c_*_nns methods.
    //
    // For naming systems that support junctions (explicit nns),
    // the trailing slash is meaningless because a junction does not
    // have an implicit nns.  The default implementation here
    // throws a NameNotFoundException for such names.
    // If a context wants to accept a trailing slash as having
    // the same meaning as the same name without a trailing slash,
    // then it should override these c_*_nns methods.

    // See ComponentContext for details.

    protected Attributes c_getAttributes_nns(Name name,
                                            String[] attrIds,
                                            Continuation cont)
        throws NamingException {
            c_processJunction_nns(name, cont);
            return null;
        }

    protected void c_modifyAttributes_nns(Name name,
                                       int mod_op,
                                       Attributes attrs,
                                       Continuation cont)
        throws NamingException {
            c_processJunction_nns(name, cont);
        }

    protected void c_modifyAttributes_nns(Name name,
                                       ModificationItem[] mods,
                                       Continuation cont)
        throws NamingException {
            c_processJunction_nns(name, cont);
        }

    protected void c_bind_nns(Name name,
                              Object obj,
                              Attributes attrs,
                              Continuation cont)
        throws NamingException  {
            c_processJunction_nns(name, cont);
        }

    protected void c_rebind_nns(Name name,
                                Object obj,
                                Attributes attrs,
                                Continuation cont)
        throws NamingException  {
            c_processJunction_nns(name, cont);
        }

    protected DirContext c_createSubcontext_nns(Name name,
                                               Attributes attrs,
                                               Continuation cont)
        throws NamingException  {
            c_processJunction_nns(name, cont);
            return null;
        }

    protected NamingEnumeration<SearchResult> c_search_nns(
                        Name name,
                        Attributes matchingAttributes,
                        String[] attributesToReturn,
                        Continuation cont)
        throws NamingException {
            c_processJunction_nns(name, cont);
            return null;
        }

    protected NamingEnumeration<SearchResult> c_search_nns(
                        Name name,
                        String filter,
                        SearchControls cons,
                        Continuation cont)
        throws NamingException  {
            c_processJunction_nns(name, cont);
            return null;
        }

    protected NamingEnumeration<SearchResult> c_search_nns(
                        Name name,
                        String filterExpr,
                        Object[] filterArgs,
                        SearchControls cons,
                        Continuation cont)
        throws NamingException  {
            c_processJunction_nns(name, cont);
            return null;
        }

    protected DirContext c_getSchema_nns(Name name, Continuation cont)
        throws NamingException {
            c_processJunction_nns(name, cont);
            return null;
        }

    protected DirContext c_getSchemaClassDefinition_nns(Name name, Continuation cont)
        throws NamingException {
            c_processJunction_nns(name, cont);
            return null;
        }

// ------- implementations of p_ DirContext methods using corresponding
// ------- DirContext c_ and c_*_nns methods

    /* Implements for abstract methods declared in PartialCompositeDirContext */
    protected Attributes p_getAttributes(Name name,
                                        String[] attrIds,
                                        Continuation cont)
        throws NamingException  {
        HeadTail res = p_resolveIntermediate(name, cont);
        Attributes answer = null;
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                answer = c_getAttributes_nns(res.getHead(), attrIds, cont);
                break;

            case TERMINAL_COMPONENT:
                answer = c_getAttributes(res.getHead(), attrIds, cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
            }
        return answer;
    }

    protected void p_modifyAttributes(Name name, int mod_op,
                                   Attributes attrs,
                                   Continuation cont)
        throws NamingException {
        HeadTail res = p_resolveIntermediate(name, cont);
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                c_modifyAttributes_nns(res.getHead(), mod_op, attrs, cont);
                break;

            case TERMINAL_COMPONENT:
                c_modifyAttributes(res.getHead(), mod_op, attrs, cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
            }
    }
    protected void p_modifyAttributes(Name name,
                                   ModificationItem[] mods,
                                   Continuation cont)
        throws NamingException {
        HeadTail res = p_resolveIntermediate(name, cont);
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                c_modifyAttributes_nns(res.getHead(), mods, cont);
                break;

            case TERMINAL_COMPONENT:
                c_modifyAttributes(res.getHead(), mods, cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
            }
    }

    protected void p_bind(Name name,
                          Object obj,
                          Attributes attrs,
                          Continuation cont)
        throws NamingException {
        HeadTail res = p_resolveIntermediate(name, cont);
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                c_bind_nns(res.getHead(), obj, attrs, cont);
                break;

            case TERMINAL_COMPONENT:
                c_bind(res.getHead(), obj, attrs, cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
            }
    }

    protected void p_rebind(Name name, Object obj,
                            Attributes attrs, Continuation cont)
        throws NamingException {
        HeadTail res = p_resolveIntermediate(name, cont);
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                c_rebind_nns(res.getHead(), obj, attrs, cont);
                break;

            case TERMINAL_COMPONENT:
                c_rebind(res.getHead(), obj, attrs, cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
            }
    }

    protected DirContext p_createSubcontext(Name name,
                                           Attributes attrs,
                                           Continuation cont)
        throws NamingException {
        HeadTail res = p_resolveIntermediate(name, cont);
        DirContext answer = null;
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                answer = c_createSubcontext_nns(res.getHead(), attrs, cont);
                break;

            case TERMINAL_COMPONENT:
                answer = c_createSubcontext(res.getHead(), attrs, cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
            }
        return answer;
    }

    protected NamingEnumeration<SearchResult> p_search(
                    Name name,
                    Attributes matchingAttributes,
                    String[] attributesToReturn,
                    Continuation cont)
        throws NamingException {
        HeadTail res = p_resolveIntermediate(name, cont);
        NamingEnumeration<SearchResult> answer = null;
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                answer = c_search_nns(res.getHead(), matchingAttributes,
                                      attributesToReturn, cont);
                break;

            case TERMINAL_COMPONENT:
                answer = c_search(res.getHead(), matchingAttributes,
                                  attributesToReturn, cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
            }
        return answer;
    }

    protected NamingEnumeration<SearchResult> p_search(Name name,
                                                       String filter,
                                                       SearchControls cons,
                                                       Continuation cont)
        throws NamingException {
        HeadTail res = p_resolveIntermediate(name, cont);
        NamingEnumeration<SearchResult> answer = null;
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                answer = c_search_nns(res.getHead(), filter, cons, cont);
                break;

            case TERMINAL_COMPONENT:
                answer = c_search(res.getHead(), filter, cons, cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
            }
        return answer;
    }

    protected NamingEnumeration<SearchResult> p_search(Name name,
                                                       String filterExpr,
                                                       Object[] filterArgs,
                                                       SearchControls cons,
                                                       Continuation cont)
            throws NamingException {
        HeadTail res = p_resolveIntermediate(name, cont);
        NamingEnumeration<SearchResult> answer = null;
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                answer = c_search_nns(res.getHead(),
                                      filterExpr, filterArgs, cons, cont);
                break;

            case TERMINAL_COMPONENT:
                answer = c_search(res.getHead(), filterExpr, filterArgs, cons, cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
            }
        return answer;
    }

    protected DirContext p_getSchema(Name name, Continuation cont)
        throws NamingException  {
            DirContext answer = null;
            HeadTail res = p_resolveIntermediate(name, cont);
            switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                answer = c_getSchema_nns(res.getHead(), cont);
                break;

            case TERMINAL_COMPONENT:
                answer = c_getSchema(res.getHead(), cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
            }
            return answer;
        }

    protected DirContext p_getSchemaClassDefinition(Name name, Continuation cont)
        throws NamingException  {
            DirContext answer = null;
            HeadTail res = p_resolveIntermediate(name, cont);
            switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                answer = c_getSchemaClassDefinition_nns(res.getHead(), cont);
                break;

            case TERMINAL_COMPONENT:
                answer = c_getSchemaClassDefinition(res.getHead(), cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
            }
            return answer;
        }
}
