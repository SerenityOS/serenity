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
import javax.naming.spi.ResolveResult;

/**
  * Provides implementation of p_* operations using
  * c_* operations provided by subclasses.
  *
  * Clients: deal only with names for its own naming service.  Must
  * provide implementations for c_* methods, and for p_parseComponent()
  * and the c_*_nns methods if the defaults are not appropriate.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  */

public abstract class ComponentContext extends PartialCompositeContext {
    private static int debug = 0;

    protected ComponentContext() {
        _contextType = _COMPONENT;
    }

// ------ Abstract methods whose implementation are provided by subclass

    /* Equivalent methods in Context interface */
    protected abstract Object c_lookup(Name name, Continuation cont)
        throws NamingException;
    protected abstract Object c_lookupLink(Name name, Continuation cont)
        throws NamingException;

    protected abstract NamingEnumeration<NameClassPair> c_list(Name name,
        Continuation cont) throws NamingException;
    protected abstract NamingEnumeration<Binding> c_listBindings(Name name,
        Continuation cont) throws NamingException;
    protected abstract void c_bind(Name name, Object obj, Continuation cont)
        throws NamingException;
    protected abstract void c_rebind(Name name, Object obj, Continuation cont)
        throws NamingException;
    protected abstract void c_unbind(Name name, Continuation cont)
        throws NamingException;
    protected abstract void c_destroySubcontext(Name name, Continuation cont)
        throws NamingException;
    protected abstract Context c_createSubcontext(Name name,
        Continuation cont) throws NamingException;
    protected abstract void c_rename(Name oldname, Name newname,
        Continuation cont) throws NamingException;
    protected abstract NameParser c_getNameParser(Name name, Continuation cont)
        throws NamingException;

// ------ Methods that may need to be overridden by subclass

    /* Parsing method */
    /**
      * Determines which of the first components of 'name' belong
      * to this naming system.
      * If no components belong to this naming system, return
      * the empty name (new CompositeName()) as the head,
      * and the entire name as the tail.
      *
      * The default implementation supports strong separation.
      * If the name is empty or if the first component is empty,
      * head is the empty name and tail is the entire name.
      * (This means that this context does not have any name to work with).
      * Otherwise, it returns the first component as head, and the rest of
      * the components as tail.
      *
      * Subclass should override this method according its own policies.
      *
      * For example, a weakly separated system with dynamic boundary
      * determination would simply return as head 'name'.
      * A weakly separated with static boundary
      * determination would select the components in the front of 'name'
      * that conform to some syntax rules.  (e.g. in X.500 syntax, perhaps
      * select front components that have a equal sign).
      * If none conforms, return an empty name.
      */
    protected HeadTail p_parseComponent(Name name, Continuation cont)
        throws NamingException {
        int separator;
        // if no name to parse, or if we're already at boundary
        if (name.isEmpty() ||  name.get(0).isEmpty()) {
            separator = 0;
        } else {
            separator = 1;
        }
        Name head, tail;

        if (name instanceof CompositeName) {
            head = name.getPrefix(separator);
            tail = name.getSuffix(separator);
        } else {
            // treat like compound name
            head = new CompositeName().add(name.toString());
            tail = null;
        }

        if (debug > 2) {
            System.err.println("ORIG: " + name);
            System.err.println("PREFIX: " + name);
            System.err.println("SUFFIX: " + null);
        }
        return new HeadTail(head, tail);
    }


    /* Resolution method for supporting federation */

    /**
      * Resolves the nns for 'name' when the named context is acting
      * as an intermediate context.
      *
      * For a system that supports only junctions, this would be
      * equivalent to
      *         c_lookup(name, cont);
      * because for junctions, an intermediate slash simply signifies
      * a syntactic separator.
      *
      * For a system that supports only implicit nns, this would be
      * equivalent to
      *         c_lookup_nns(name, cont);
      * because for implicit nns, a slash always signifies the implicit nns,
      * regardless of whether it is intermediate or trailing.
      *
      * By default this method supports junctions, and also allows for an
      * implicit nns to be dynamically determined through the use of the
      * "nns" reference (see c_processJunction_nns()).
      * Contexts that implement implicit nns directly should provide an
      * appropriate override.
      *
      * A junction, by definition, is a binding of a name in one
      * namespace to an object in another.  The default implementation
      * of this method detects the crossover into another namespace
      * using the following heuristic:  there is a junction when "name"
      * resolves to a context that is not an instance of
      * this.getClass().  Contexts supporting junctions for which this
      * heuristic is inappropriate should override this method.
      */
    protected Object c_resolveIntermediate_nns(Name name, Continuation cont)
        throws NamingException {
            try {
                final Object obj = c_lookup(name, cont);

                // Do not append "" to Continuation 'cont' even if set
                // because the intention is to ignore the nns

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
                            -8831204798861786362L;
                    };
                    Reference ref = new Reference("java.lang.Object", addr);

                    // Resolved name has trailing slash to indicate nns
                    CompositeName resName = (CompositeName)name.clone();
                    resName.add(""); // add trailing slash

                    // Set continuation leave it to
                    // PartialCompositeContext.getPCContext() to throw CPE.
                    // Do not use setContinueNNS() because we've already
                    // consumed "/" (i.e., moved it to resName).

                    cont.setContinue(ref, resName, this);
                    return null;
                } else {
                    // Consume "/" and continue
                    return obj;
                }

            } catch (NamingException e) {
                e.appendRemainingComponent(""); // add nns back
                throw e;
            }
        }

    /* Equivalent of Context Methods for supporting nns */

    // The following methods are called when the Context methods
    // are invoked with a name that has a trailing slash.
    // For naming systems that support implicit nns,
    // the trailing slash signifies the implicit nns.
    // For such naming systems, override these c_*_nns methods.
    //
    // For naming systems that do not support implicit nns, the
    // default implementations here throw an exception.  See
    // c_processJunction_nns() for details.

    protected Object c_lookup_nns(Name name, Continuation cont)
        throws NamingException {
            c_processJunction_nns(name, cont);
            return null;
        }

    protected Object c_lookupLink_nns(Name name, Continuation cont)
        throws NamingException {
            c_processJunction_nns(name, cont);
            return null;
        }

    protected NamingEnumeration<NameClassPair> c_list_nns(Name name,
        Continuation cont) throws NamingException {
            c_processJunction_nns(name, cont);
            return null;
        }

    protected NamingEnumeration<Binding> c_listBindings_nns(Name name,
        Continuation cont) throws NamingException {
            c_processJunction_nns(name, cont);
            return null;
        }

    protected void c_bind_nns(Name name, Object obj, Continuation cont)
        throws NamingException {
            c_processJunction_nns(name, cont);
        }

    protected void c_rebind_nns(Name name, Object obj, Continuation cont)
        throws NamingException {
            c_processJunction_nns(name, cont);
        }

    protected void c_unbind_nns(Name name, Continuation cont)
        throws NamingException {
            c_processJunction_nns(name, cont);
        }

    protected Context c_createSubcontext_nns(Name name,
        Continuation cont) throws NamingException {
            c_processJunction_nns(name, cont);
            return null;
        }

    protected void c_destroySubcontext_nns(Name name, Continuation cont)
        throws NamingException {
            c_processJunction_nns(name, cont);
        }


    protected void c_rename_nns(Name oldname, Name newname, Continuation cont)
        throws NamingException {
            c_processJunction_nns(oldname, cont);
        }

    protected NameParser c_getNameParser_nns(Name name, Continuation cont)
        throws NamingException {
            c_processJunction_nns(name, cont);
            return null;
        }

// ------ internal method used by ComponentContext

    /**
     * Locates the nns using the default policy.  This policy fully
     * handles junctions, but otherwise throws an exception when an
     * attempt is made to resolve an implicit nns.
     *
     * The default policy is as follows:  If there is a junction in
     * the namespace, then resolve to the junction and continue the
     * operation there (thus deferring to that context to find its own
     * nns).  Otherwise, resolve as far as possible and then throw
     * CannotProceedException with the resolved object being a reference:
     * the address type is "nns", and the address contents is this
     * context.
     *
     * For example, when c_bind_nns(name, obj, ...) is invoked, the
     * caller is attempting to bind the object "obj" to the nns of
     * "name".  If "name" is a junction, it names an object in another
     * naming system that (presumably) has an nns.  c_bind_nns() will
     * first resolve "name" to a context and then attempt to continue
     * the bind operation there, (thus binding to the nns of the
     * context named by "name").  If "name" is empty then throw an
     * exception, since this context does not by default support an
     * implicit nns.
     *
     * To implement a context that does support an implicit nns, it is
     * necessary to override this default policy.  This is done by
     * overriding the c_*_nns() methods (which each call this method
     * by default).
     */
    protected void c_processJunction_nns(Name name, Continuation cont)
            throws NamingException
    {
        if (name.isEmpty()) {
            // Construct a new Reference that contains this context.
            RefAddr addr = new RefAddr("nns") {
                public Object getContent() {
                    return ComponentContext.this;
                }
                private static final long serialVersionUID =
                    -1389472957988053402L;
            };
            Reference ref = new Reference("java.lang.Object", addr);

            // Set continuation leave it to PartialCompositeContext.getPCContext()
            // to throw the exception.
            // Do not use setContinueNNS() because we've are
            // setting relativeResolvedName to "/".
            cont.setContinue(ref, _NNS_NAME, this);
            return;
        }

        try {
            // lookup name to continue operation in nns
            Object target = c_lookup(name, cont);
            if (cont.isContinue())
                cont.appendRemainingComponent("");
            else {
                cont.setContinueNNS(target, name, this);
            }
        } catch (NamingException e) {
            e.appendRemainingComponent(""); // add nns back
            throw e;
        }
    }

    protected static final byte USE_CONTINUATION = 1;
    protected static final byte TERMINAL_COMPONENT = 2;
    protected static final byte TERMINAL_NNS_COMPONENT = 3;

    /**
      * Determine whether 'name' is a terminal component in
      * this naming system.
      * If so, return status indicating so, so that caller
      * can perform context operation on this name.
      *
      * If not, then the first component(s) of 'name' names
      * an intermediate context.  In that case, resolve these components
      * and set Continuation to be the object named.
      *
      * see test cases at bottom of file.
      */

    protected HeadTail p_resolveIntermediate(Name name, Continuation cont)
        throws NamingException {
        int ret = USE_CONTINUATION;
        cont.setSuccess();      // initialize
        HeadTail p = p_parseComponent(name, cont);
        Name tail = p.getTail();
        Name head = p.getHead();

        if (tail == null || tail.isEmpty()) {
//System.out.println("terminal : " + head);
            ret = TERMINAL_COMPONENT;
        } else if (!tail.get(0).isEmpty()) {
            // tail does not begin with "/"
/*
            if (head.isEmpty()) {
                // Context could not find name that it can use
                // illegal syntax error or name not found
//System.out.println("nnf exception : " + head);
                NamingException e = new NameNotFoundException();
                cont.setError(this, name);
                throw cont.fillInException(e);
            } else  {
*/
                // head is being used as intermediate context,
                // resolve head and set Continuation with tail
                try {
                    Object obj = c_resolveIntermediate_nns(head, cont);
//System.out.println("resInter : " + head + "=" + obj);
                    if (obj != null)
                        cont.setContinue(obj, head, this, tail);
                    else if (cont.isContinue()) {
                        checkAndAdjustRemainingName(cont.getRemainingName());
                        cont.appendRemainingName(tail);
                    }
                } catch (NamingException e) {
                    checkAndAdjustRemainingName(e.getRemainingName());
                    e.appendRemainingName(tail);
                    throw e;
                }
/*
            }
*/
        } else {
            // tail begins with "/"
            if (tail.size() == 1) {
                ret = TERMINAL_NNS_COMPONENT;
//System.out.println("terminal_nns : " + head);
            } else if (head.isEmpty() || isAllEmpty(tail)) {
                // resolve nns of head and continue with tail.getSuffix(1)
                Name newTail = tail.getSuffix(1);
                try {
                    Object obj = c_lookup_nns(head, cont);
//System.out.println("lookup_nns : " + head + "=" + obj);
                    if (obj != null)
                        cont.setContinue(obj, head, this, newTail);
                    else if (cont.isContinue()) {
                        cont.appendRemainingName(newTail);
//                      Name rname = cont.getRemainingName();
//System.out.println("cont.rname" + rname);
                    }
                } catch (NamingException e) {
                    e.appendRemainingName(newTail);
                    throw e;
                }
            } else {
                // head is being used as intermediate context
                // resolve and set continuation to tail
                try {
                    Object obj = c_resolveIntermediate_nns(head, cont);
//System.out.println("resInter2 : " + head + "=" + obj);
                    if (obj != null)
                        cont.setContinue(obj, head, this, tail);
                    else if (cont.isContinue()) {
                        checkAndAdjustRemainingName(cont.getRemainingName());
                        cont.appendRemainingName(tail);
                    }
                } catch (NamingException e) {
                    checkAndAdjustRemainingName(e.getRemainingName());
                    e.appendRemainingName(tail);
                    throw e;
                }
            }
        }

        p.setStatus(ret);
        return p;
    }

    // When c_resolveIntermediate_nns() or c_lookup_nns() sets up
    // its continuation, to indicate "nns", it appends an empty
    // component to the remaining name (e.g. "eng/"). If last
    // component of remaining name is empty; delete empty component
    // before appending tail so that composition of the names work
    // correctly. For example, when merging "eng/" and "c.b.a", we want
    // the result to be "eng/c.b.a" because the trailing slash in eng
    // is extraneous.  When merging "" and "c.b.a", we want the result
    // to be "/c.b.a" and so must keep the trailing slash (empty name).
    void checkAndAdjustRemainingName(Name rname) throws InvalidNameException {
        int count;
        if (rname != null && (count=rname.size()) > 1 &&
            rname.get(count-1).isEmpty()) {
            rname.remove(count-1);
        }
    }

    // Returns true if n contains only empty components
    protected boolean isAllEmpty(Name n) {
        int count = n.size();
        for (int i =0; i < count; i++ ) {
            if (!n.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }



// ------ implementations of p_ Resolver and Context methods using
// ------ corresponding c_ and c_*_nns methods


    /* implementation for Resolver method */

    protected ResolveResult p_resolveToClass(Name name,
                                             Class<?> contextType,
                                             Continuation cont)
            throws NamingException {

        if (contextType.isInstance(this)) {
            cont.setSuccess();
            return (new ResolveResult(this, name));
        }

        ResolveResult ret = null;
        HeadTail res = p_resolveIntermediate(name, cont);
        switch (res.getStatus()) {
        case TERMINAL_NNS_COMPONENT:
            Object obj = p_lookup(name, cont);
            if (!cont.isContinue() && contextType.isInstance(obj)) {
                ret = new ResolveResult(obj, _EMPTY_NAME);
            }
            break;

        case TERMINAL_COMPONENT:
            cont.setSuccess();  // no contextType found; return null
            break;

        default:
            /* USE_CONTINUATION */
            /* pcont already set or exception thrown */
            break;
        }
        return ret;
    }

    /* implementations of p_ Context methods */

    protected Object p_lookup(Name name, Continuation cont) throws NamingException {
        Object ret = null;
        HeadTail res = p_resolveIntermediate(name, cont);
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                ret = c_lookup_nns(res.getHead(), cont);
                if (ret instanceof LinkRef) {
                    cont.setContinue(ret, res.getHead(), this);
                    ret = null;
                }
                break;

            case TERMINAL_COMPONENT:
                ret = c_lookup(res.getHead(), cont);
                if (ret instanceof LinkRef) {
                    cont.setContinue(ret, res.getHead(), this);
                    ret = null;
                }
                break;

            default:
                /* USE_CONTINUATION */
                /* pcont already set or exception thrown */
                break;
        }
        return ret;
    }

    protected NamingEnumeration<NameClassPair> p_list(Name name, Continuation cont)
        throws NamingException {
        NamingEnumeration<NameClassPair> ret = null;
        HeadTail res = p_resolveIntermediate(name, cont);
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                if (debug > 0)
                    System.out.println("c_list_nns(" + res.getHead() + ")");
                ret = c_list_nns(res.getHead(), cont);
                break;

            case TERMINAL_COMPONENT:
                if (debug > 0)
                    System.out.println("c_list(" + res.getHead() + ")");
                ret = c_list(res.getHead(), cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
        }
        return ret;
    }

    protected NamingEnumeration<Binding> p_listBindings(Name name, Continuation cont) throws
        NamingException {
        NamingEnumeration<Binding> ret = null;
        HeadTail res = p_resolveIntermediate(name, cont);
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                ret = c_listBindings_nns(res.getHead(), cont);
                break;

            case TERMINAL_COMPONENT:
                ret = c_listBindings(res.getHead(), cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
        }
        return ret;
    }

    protected void p_bind(Name name, Object obj, Continuation cont) throws
        NamingException {
        HeadTail res = p_resolveIntermediate(name, cont);
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                c_bind_nns(res.getHead(), obj, cont);
                break;

            case TERMINAL_COMPONENT:
                c_bind(res.getHead(), obj, cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
        }
    }

    protected void p_rebind(Name name, Object obj, Continuation cont) throws
        NamingException {
        HeadTail res = p_resolveIntermediate(name, cont);
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                c_rebind_nns(res.getHead(), obj, cont);
                break;

            case TERMINAL_COMPONENT:
                c_rebind(res.getHead(), obj, cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
        }
    }

    protected void p_unbind(Name name, Continuation cont) throws
        NamingException {
        HeadTail res = p_resolveIntermediate(name, cont);
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                c_unbind_nns(res.getHead(), cont);
                break;

            case TERMINAL_COMPONENT:
                c_unbind(res.getHead(), cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
        }
    }

    protected void p_destroySubcontext(Name name, Continuation cont) throws
        NamingException {
        HeadTail res = p_resolveIntermediate(name, cont);
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                c_destroySubcontext_nns(res.getHead(), cont);
                break;

            case TERMINAL_COMPONENT:
                c_destroySubcontext(res.getHead(), cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
        }
    }

    protected Context p_createSubcontext(Name name, Continuation cont) throws
        NamingException {
            Context ret = null;
        HeadTail res = p_resolveIntermediate(name, cont);
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                ret = c_createSubcontext_nns(res.getHead(), cont);
                break;

            case TERMINAL_COMPONENT:
                ret = c_createSubcontext(res.getHead(), cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
        }
        return ret;
    }

    protected void p_rename(Name oldName, Name newName, Continuation cont) throws
        NamingException {
        HeadTail res = p_resolveIntermediate(oldName, cont);
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                c_rename_nns(res.getHead(), newName, cont);
                break;

            case TERMINAL_COMPONENT:
                c_rename(res.getHead(), newName, cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
        }
    }

    protected NameParser p_getNameParser(Name name, Continuation cont) throws
        NamingException {
        NameParser ret = null;
        HeadTail res = p_resolveIntermediate(name, cont);
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                ret = c_getNameParser_nns(res.getHead(), cont);
                break;

            case TERMINAL_COMPONENT:
                ret = c_getNameParser(res.getHead(), cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
        }
        return ret;
    }

    protected Object p_lookupLink(Name name, Continuation cont)
        throws NamingException {
        Object ret = null;
        HeadTail res = p_resolveIntermediate(name, cont);
        switch (res.getStatus()) {
            case TERMINAL_NNS_COMPONENT:
                ret = c_lookupLink_nns(res.getHead(), cont);
                break;

            case TERMINAL_COMPONENT:
                ret = c_lookupLink(res.getHead(), cont);
                break;

            default:
                /* USE_CONTINUATION */
                /* cont already set or exception thrown */
                break;
        }
        return ret;
    }
}

/*
 *      How p_resolveIntermediate() should behave for various test cases

a.b/x   {a.b, x}
        c_resolveIntermediate_nns(a.b)
        continue(x)
        {x,}
        terminal(x)

a.b/    {a.b, ""}
        terminal_nns(a.b);

a.b//
        {a.b, ("", "")}
        c_lookup_nns(a.b)
        continue({""})
        {,""}
        terminal_nns({})

/x      {{}, {"", x}}
        c_lookup_nns({})
        continue(x)
        {x,}
        terminal(x)

//y     {{}, {"", "", y}}
        c_lookup_nns({})
        continue({"", y})
        {{}, {"", y}}
        c_lookup_nns({})
        continue(y)
        {y,}
        terminal(y)

a.b//y  {a.b, {"", y}}
        c_resolveIntermediate_nns(a.b)
        continue({"", y})
        {{}, {"",y}}
        c_lookup_nns({});
        continue(y)
        {y,}
        terminal(y);
 *
 */
