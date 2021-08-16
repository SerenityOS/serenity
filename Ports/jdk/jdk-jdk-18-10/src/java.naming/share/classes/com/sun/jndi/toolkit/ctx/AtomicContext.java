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

/**
  * Clients: deal only with names for its own naming service
  * and deals with single contexts that can be built up into
  * hierarchical naming systems.
  * Direct subclasses of AtomicContext must provide implementations for
  * the abstract a_ Context methods, and c_parseComponent().
  *
  * If the subclass implements the notion of implicit nns,
  * it must override the a_*_nns Context methods as well.
  *
  * @author Rosanna Lee
  *
  */

public abstract class AtomicContext extends ComponentContext {
    private static int debug = 0;

    protected AtomicContext () {
        _contextType = _ATOMIC;
    }

// ------ Abstract methods whose implementation are provided by subclasses


    /* Equivalent to Context methods */
    protected abstract Object a_lookup(String name, Continuation cont)
        throws NamingException;
    protected abstract Object a_lookupLink(String name, Continuation cont)
        throws NamingException;

    protected abstract NamingEnumeration<NameClassPair> a_list(
        Continuation cont) throws NamingException;
    protected abstract NamingEnumeration<Binding> a_listBindings(
        Continuation cont) throws NamingException;
    protected abstract void a_bind(String name, Object obj, Continuation cont)
        throws NamingException;
    protected abstract void a_rebind(String name, Object obj, Continuation cont)
        throws NamingException;
    protected abstract void a_unbind(String name, Continuation cont)
        throws NamingException;
    protected abstract void a_destroySubcontext(String name, Continuation cont)
        throws NamingException;
    protected abstract Context a_createSubcontext(String name,
        Continuation cont) throws NamingException;
    protected abstract void a_rename(String oldname, Name newname,
        Continuation cont) throws NamingException;
    protected abstract NameParser a_getNameParser(Continuation cont)
        throws NamingException;

    /* Parsing */
    /**
     * Parse 'inputName' into two parts:
     * head: the first component in this name
     * tail: the rest of the unused name.
     *
     * Subclasses should provide an implementation for this method
     * which parses inputName using its own name syntax.
     */
    protected abstract StringHeadTail c_parseComponent(String inputName,
        Continuation cont) throws NamingException;


// ------ Methods that need to be overridden by subclass

    /* Resolution method for supporting federation */
    /**
      * Resolves the nns for 'name' when the named context is acting
      * as an intermediate context.
      *
      * For a system that supports junctions, this would be equivalent to
      *         a_lookup(name, cont);
      * because for junctions, an intermediate slash simply signifies
      * a syntactic separator.
      *
      * For a system that supports implicit nns, this would be equivalent to
      *         a_lookup_nns(name, cont);
      * because for implicit nns, a slash always signifies the implicit nns,
      * regardless of whether it is intermediate or trailing.
      *
      * By default this method supports junctions, and also allows for an
      * implicit nns to be dynamically determined through the use of the
      * "nns" reference (see a_processJunction_nns()).
      * Contexts that implement implicit nns directly should provide an
      * appropriate override.
      */
    protected Object a_resolveIntermediate_nns(String name, Continuation cont)
        throws NamingException {
            try {
                final Object obj = a_lookup(name, cont);

                // Do not append "" to Continuation 'cont' even if set
                // because the intention is to ignore the nns

                //
                if (obj != null && getClass().isInstance(obj)) {
                    // If "obj" is in the same type as this object, it must
                    // not be a junction. Continue the lookup with "/".

                    cont.setContinueNNS(obj, name, this);
                    return null;

                } else if (obj != null && !(obj instanceof Context)) {
                    // obj is not even a context, so try to find its nns
                    // dynamically by constructing a Reference containing obj.
                    RefAddr addr = new RefAddr("nns") {
                        public Object getContent() {
                            return obj;
                        }
                        private static final long serialVersionUID =
                            -3399518522645918499L;
                    };
                    Reference ref = new Reference("java.lang.Object", addr);

                    // Resolved name has trailing slash to indicate nns
                    CompositeName resName = new CompositeName();
                    resName.add(name);
                    resName.add(""); // add trailing slash

                    // Set continuation leave it to
                    // PartialCompositeContext.getPCContext() to throw CPE.
                    // Do not use setContinueNNS() because we've already
                    // consumed "/" (i.e., moved it to resName).

                    cont.setContinue(ref, resName, this);
                    return null;

                } else {
                    return obj;
                }

            } catch (NamingException e) {
                e.appendRemainingComponent(""); // add nns back
                throw e;
            }
        }

    /* Equivalent of Context Methods for supporting nns */

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


    protected Object a_lookup_nns(String name, Continuation cont)
        throws NamingException {
            a_processJunction_nns(name, cont);
            return null;
        }

    protected Object a_lookupLink_nns(String name, Continuation cont)
        throws NamingException {
            a_processJunction_nns(name, cont);
            return null;
        }

    protected NamingEnumeration<NameClassPair> a_list_nns(Continuation cont)
        throws NamingException {
            a_processJunction_nns(cont);
            return null;
        }
    protected NamingEnumeration<Binding> a_listBindings_nns(Continuation cont)
        throws NamingException {
            a_processJunction_nns(cont);
            return null;
        }

    protected void a_bind_nns(String name, Object obj, Continuation cont)
        throws NamingException {
            a_processJunction_nns(name, cont);
        }

    protected void a_rebind_nns(String name, Object obj, Continuation cont)
        throws NamingException {
            a_processJunction_nns(name, cont);
        }

    protected void a_unbind_nns(String name, Continuation cont)
        throws NamingException {
            a_processJunction_nns(name, cont);
        }

    protected Context a_createSubcontext_nns(String name, Continuation cont)
        throws NamingException {
            a_processJunction_nns(name, cont);
            return null;
        }

    protected void a_destroySubcontext_nns(String name, Continuation cont)
        throws NamingException {
            a_processJunction_nns(name, cont);
        }

    protected void a_rename_nns(String oldname, Name newname, Continuation cont)
        throws NamingException {
            a_processJunction_nns(oldname, cont);
        }

    protected NameParser a_getNameParser_nns(Continuation cont)
        throws NamingException {
            a_processJunction_nns(cont);
            return null;
        }



    protected boolean isEmpty(String name) {
        return name == null || name.isEmpty();
    }

// ------ implementations of c_  and c_*_nns methods using
// ------ the corresponding a_ and a_*_nns methods

    /* Equivalent to methods in  Context interface */

    protected Object c_lookup(Name name, Continuation cont)
        throws NamingException {
            Object ret = null;
            if (resolve_to_penultimate_context(name, cont)) {
                ret = a_lookup(name.toString(), cont);
                if (ret != null && ret instanceof LinkRef) {
                    cont.setContinue(ret, name, this);
                    ret = null;
                }
            }
            return ret;
        }

    protected Object c_lookupLink(Name name, Continuation cont)
        throws NamingException {
            if (resolve_to_penultimate_context(name, cont)) {
                return a_lookupLink(name.toString(), cont);
            }
            return null;
        }

    protected NamingEnumeration<NameClassPair> c_list(Name name,
        Continuation cont) throws NamingException {
            if (resolve_to_context(name, cont)) {
                return a_list(cont);
            }
            return null;
        }

    protected NamingEnumeration<Binding> c_listBindings(Name name,
        Continuation cont) throws NamingException {
            if (resolve_to_context(name, cont)) {
                return a_listBindings(cont);
            }
            return null;
        }

    protected void c_bind(Name name, Object obj, Continuation cont)
        throws NamingException {
            if (resolve_to_penultimate_context(name, cont))
                a_bind(name.toString(), obj, cont);
        }

    protected void c_rebind(Name name, Object obj, Continuation cont)
        throws NamingException {
            if (resolve_to_penultimate_context(name, cont))
                a_rebind(name.toString(), obj, cont);
        }

    protected void c_unbind(Name name, Continuation cont)
        throws NamingException {
            if (resolve_to_penultimate_context(name, cont))
                a_unbind(name.toString(), cont);
        }

    protected void c_destroySubcontext(Name name, Continuation cont)
        throws NamingException {
            if (resolve_to_penultimate_context(name, cont))
                a_destroySubcontext(name.toString(), cont);
        }

    protected Context c_createSubcontext(Name name,
        Continuation cont) throws NamingException {
            if (resolve_to_penultimate_context(name, cont))
                return a_createSubcontext(name.toString(), cont);
            else
                return null;
        }

    protected void c_rename(Name oldname, Name newname,
        Continuation cont) throws NamingException {
            if (resolve_to_penultimate_context(oldname, cont))
                 a_rename(oldname.toString(), newname, cont);
        }

    protected NameParser c_getNameParser(Name name,
        Continuation cont) throws NamingException {
            if (resolve_to_context(name, cont))
                return a_getNameParser(cont);
            return null;
        }

    /* The following are overridden only for AtomicContexts.
     * AtomicContext is used by PartialCompositeDirContext and ComponentDirContext
     * in the inheritance tree to make use of methods in
     * PartialCompositeContext and ComponentContext. We only want to use the
     * atomic forms when we're actually an atomic context.
     */

    /* From ComponentContext */

    protected Object c_resolveIntermediate_nns(Name name, Continuation cont)
        throws NamingException {
            if (_contextType == _ATOMIC) {
                Object ret = null;
                if (resolve_to_penultimate_context_nns(name, cont)) {
                    ret = a_resolveIntermediate_nns(name.toString(), cont);
                    if (ret != null && ret instanceof LinkRef) {
                        cont.setContinue(ret, name, this);
                        ret = null;
                    }
                }
                return ret;
            } else {
                // use ComponentContext
                return super.c_resolveIntermediate_nns(name, cont);
            }
        }

    /* Equivalent to methods in Context interface for nns */

    protected Object c_lookup_nns(Name name, Continuation cont)
        throws NamingException {
            if (_contextType == _ATOMIC) {
                Object ret = null;
                if (resolve_to_penultimate_context_nns(name, cont)) {
                    ret = a_lookup_nns(name.toString(), cont);
                    if (ret != null && ret instanceof LinkRef) {
                        cont.setContinue(ret, name, this);
                        ret = null;
                    }
                }
                return ret;
            } else {
                return super.c_lookup_nns(name, cont);
            }
        }

    protected Object c_lookupLink_nns(Name name, Continuation cont)
        throws NamingException {
            if (_contextType == _ATOMIC) {
                // %%% check logic
                resolve_to_nns_and_continue(name, cont);
                return null;
            } else {
                // use ComponentContext
                return super.c_lookupLink_nns(name, cont);
            }
        }

    protected NamingEnumeration<NameClassPair> c_list_nns(Name name,
        Continuation cont) throws NamingException {
            if (_contextType == _ATOMIC) {
                resolve_to_nns_and_continue(name, cont);
                return null;
            } else {
                // use ComponentContext
                return super.c_list_nns(name, cont);
            }
        }

    protected NamingEnumeration<Binding> c_listBindings_nns(Name name,
        Continuation cont) throws NamingException {
            if (_contextType == _ATOMIC) {
                resolve_to_nns_and_continue(name, cont);
                return null;
            } else {
                // use ComponentContext
                return super.c_listBindings_nns(name, cont);
            }
        }

    protected void c_bind_nns(Name name, Object obj, Continuation cont)
        throws NamingException {
            if (_contextType == _ATOMIC) {
                if (resolve_to_penultimate_context_nns(name, cont))
                    a_bind_nns(name.toString(), obj, cont);
            } else {
                // use ComponentContext
                super.c_bind_nns(name, obj, cont);
            }
        }

    protected void c_rebind_nns(Name name, Object obj, Continuation cont)
        throws NamingException {
            if (_contextType == _ATOMIC) {
                if (resolve_to_penultimate_context_nns(name, cont))
                    a_rebind_nns(name.toString(), obj, cont);
            } else {
                // use ComponentContext
                super.c_rebind_nns(name, obj, cont);
            }
        }

    protected void c_unbind_nns(Name name, Continuation cont)
        throws NamingException {
            if (_contextType == _ATOMIC) {
                if (resolve_to_penultimate_context_nns(name, cont))
                    a_unbind_nns(name.toString(), cont);
            } else {
                // use ComponentContext
                super.c_unbind_nns(name, cont);
            }
        }

    protected Context c_createSubcontext_nns(Name name,
        Continuation cont) throws NamingException {
            if (_contextType == _ATOMIC) {
                if (resolve_to_penultimate_context_nns(name, cont))
                    return a_createSubcontext_nns(name.toString(), cont);
                else
                    return null;
            } else {
                // use ComponentContext
                return super.c_createSubcontext_nns(name, cont);
            }
        }

    protected void c_destroySubcontext_nns(Name name, Continuation cont)
        throws NamingException {
            if (_contextType == _ATOMIC) {
                if (resolve_to_penultimate_context_nns(name, cont))
                    a_destroySubcontext_nns(name.toString(), cont);
            } else {
                // use ComponentContext
                super.c_destroySubcontext_nns(name, cont);
            }
        }

    protected void c_rename_nns(Name oldname, Name newname, Continuation cont)
        throws NamingException {
            if (_contextType == _ATOMIC) {
                if (resolve_to_penultimate_context_nns(oldname, cont))
                    a_rename_nns(oldname.toString(), newname, cont);
            } else {
                // use ComponentContext
                super.c_rename_nns(oldname, newname, cont);
            }
        }

    protected NameParser c_getNameParser_nns(Name name, Continuation cont)
        throws NamingException {
            if (_contextType == _ATOMIC) {
                resolve_to_nns_and_continue(name, cont);
                return null;
            } else {
                // use ComponentContext
                return super.c_getNameParser_nns(name, cont);
            }
        }

// --------------    internal methods used by this class

    /* Handles nns for junctions */
    /**
      * This function is used when implementing a naming system that
      * supports junctions.  For example, when the a_bind_nns(name, newobj)
      * method is invoked, that means the caller is attempting to bind the
      * object 'newobj' to the nns of 'name'.  For context that supports
      * junctions, 'name' names a junction and is pointing to the root
      * of another naming system, which in turn might have an nns.
      * This means that a_bind_nns() should first resolve 'name' and attempt to
      * continue the operation in the context named by 'name'.  (i.e. bind
      * to the nns of the context named by 'name').
      * If name is already empty, then throw NameNotFoundException because
      * this context by default does not have any nns.
      */
    protected void a_processJunction_nns(String name, Continuation cont)
        throws NamingException {
            if (name.isEmpty()) {
                NameNotFoundException e = new NameNotFoundException();
                cont.setErrorNNS(this, name);
                throw cont.fillInException(e);
            }
            try {
                // lookup name to continue operation in nns
                Object target = a_lookup(name, cont);
                if (cont.isContinue())
                    cont.appendRemainingComponent("");  // add nns back
                else {
                    cont.setContinueNNS(target, name, this);
                }
            } catch (NamingException e) {
                e.appendRemainingComponent(""); // add nns back
                throw e;
            }
        }

    /**
      * This function is used when implementing a naming system that
      * supports junctions.  For example, when the a_list_nns(newobj)
      * method is invoked, that means the caller is attempting to list the
      * the nns context of this context.  For a context that supports
      * junctions, it by default does not have any nns.  Consequently,
      * a NameNotFoundException is thrown.
      */
    protected void a_processJunction_nns(Continuation cont) throws NamingException {

        // Construct a new Reference that contains this context.
        RefAddr addr = new RefAddr("nns") {
            public Object getContent() {
                return AtomicContext.this;
            }
            private static final long serialVersionUID = 3449785852664978312L;
        };
        Reference ref = new Reference("java.lang.Object", addr);

        // Set continuation leave it to PartialCompositeContext.getPCContext()
        // to throw the exception.
        // Do not use setContinueNNS() because we've are
        // setting relativeResolvedName to "/".
        cont.setContinue(ref, _NNS_NAME, this);
    }

    /* *********** core resolution routines ******************* */

    /** Resolve to context named by 'name'.
      * Returns true if at named context (i.e. 'name' is empty name).
      * Returns false otherwise, and sets Continuation on parts of 'name'
      * not yet resolved.
      */
    protected boolean resolve_to_context(Name name, Continuation cont)
    throws NamingException {
        String target = name.toString();


        StringHeadTail ht = c_parseComponent(target, cont);
        String tail = ht.getTail();
        String head = ht.getHead();

        if (debug > 0)
            System.out.println("RESOLVE TO CONTEXT(" + target + ") = {" +
                               head + ", " + tail + "}");

        if (head == null) {
            // something is wrong; no name at all
            InvalidNameException e = new InvalidNameException();
            throw cont.fillInException(e);
        }
        if (!isEmpty(head)) {
            // if there is head is a non-empty name
            // this means more resolution to be done
            try {
                Object headCtx = a_lookup(head, cont);
//              System.out.println("answer " + headCtx);
                if (headCtx != null)
                    cont.setContinue(headCtx, head, this, (tail == null ? "" : tail));
                else if (cont.isContinue())
                    cont.appendRemainingComponent(tail);
            } catch (NamingException e) {
                e.appendRemainingComponent(tail);
                throw e;
            }
        } else {
            cont.setSuccess();  // clear
            return true;
        }
        return false;
    }

    /**
      * Resolves to penultimate context named by 'name'.
      * Returns true if penultimate context has been reached (i.e. name
      * only has one atomic component left).
      * Returns false otherwise, and sets Continuation to parts of name
      * not yet resolved.
      */
    protected boolean resolve_to_penultimate_context(Name name, Continuation cont)
    throws NamingException {
        String target = name.toString();

        if (debug > 0)
            System.out.println("RESOLVE TO PENULTIMATE" + target);

        StringHeadTail ht = c_parseComponent(target, cont);
        String tail = ht.getTail();
        String head = ht.getHead();
        if (head == null) {
            // something is wrong; no name at all
            InvalidNameException e = new InvalidNameException();
            throw cont.fillInException(e);
        }

        if (!isEmpty(tail)) {
            // more components; hence not at penultimate context yet
            try {
                Object headCtx = a_lookup(head, cont);
                if (headCtx != null)
                    cont.setContinue(headCtx, head, this, tail);
                else if (cont.isContinue())
                    cont.appendRemainingComponent(tail);
            } catch (NamingException e) {
                e.appendRemainingComponent(tail);
                throw e;
            }
        } else {
            // already at penultimate context
            cont.setSuccess();  // clear
            return true;
        }
        return false;
    }

    /**
      * This function is similar to resolve_to_penultimate_context()
      * except it should only be called by the nns() functions.
      * This function fixes any exception or continuations so that
      * it will have the proper nns name.
      */
    protected boolean resolve_to_penultimate_context_nns(Name name,
                                                         Continuation cont)
        throws NamingException {
            try {
        if (debug > 0)
            System.out.println("RESOLVE TO PENULTIMATE NNS" + name.toString());
                boolean answer = resolve_to_penultimate_context(name, cont);

                // resolve_to_penultimate_context() only calls a_lookup().
                // Any continuation it sets is lacking the nns, so
                // we need to add it back
                if (cont.isContinue())
                    cont.appendRemainingComponent("");

                return answer;
            } catch (NamingException e) {
                // resolve_to_penultimate_context() only calls a_lookup().
                // Any exceptions it throws is lacking the nns, so
                // we need to add it back.
                e.appendRemainingComponent("");
                throw e;
            }
        }

    /**
      * Resolves to nns associated with 'name' and set Continuation
      * to the result.
      */
    protected void resolve_to_nns_and_continue(Name name, Continuation cont)
        throws NamingException {
        if (debug > 0)
            System.out.println("RESOLVE TO NNS AND CONTINUE" + name.toString());

        if (resolve_to_penultimate_context_nns(name, cont)) {
            Object nns = a_lookup_nns(name.toString(), cont);
            if (nns != null)
                cont.setContinue(nns, name, this);
        }
    }
}
