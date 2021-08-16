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

package com.sun.jndi.toolkit.url;

import javax.naming.*;
import javax.naming.spi.ResolveResult;
import javax.naming.spi.NamingManager;

import java.util.Hashtable;
import java.net.MalformedURLException;

/**
 * This abstract class is a generic URL context that accepts as the
 * name argument either a string URL or a Name whose first component
 * is a URL. It resolves the URL to a target context and then continues
 * the operation using the remaining name in the target context as if
 * the first component names a junction.
 *
 * A subclass must define getRootURLContext()
 * to process the URL into head/tail pieces. If it wants to control how
 * URL strings are parsed and compared for the rename() operation, then
 * it should override getNonRootURLSuffixes() and urlEquals().
 *
 * @author Scott Seligman
 * @author Rosanna Lee
 */
public abstract class GenericURLContext implements Context {
    protected Hashtable<String, Object> myEnv = null;

    @SuppressWarnings("unchecked") // Expect Hashtable<String, Object>
    public GenericURLContext(Hashtable<?,?> env) {
        // context that is not tied to any specific URL
        myEnv =
            (Hashtable<String, Object>)(env == null ? null : env.clone());
    }

    public void close() throws NamingException {
        myEnv = null;
    }

    public String getNameInNamespace() throws NamingException {
        return ""; // %%% check this out: A URL context's name is ""
    }

    /**
      * Resolves 'name' into a target context with remaining name.
      * For example, with a JNDI URL "jndi://dnsname/rest_name",
      * this method resolves "jndi://dnsname/" to a target context,
      * and returns the target context with "rest_name".
      * The definition of "root URL" and how much of the URL to
      * consume is implementation specific.
      * If rename() is supported for a particular URL scheme,
      * getRootURLContext(), getURLPrefix(), and getURLSuffix()
      * must be in sync wrt how URLs are parsed and returned.
      */
    protected abstract ResolveResult getRootURLContext(String url,
        Hashtable<?,?> env) throws NamingException;

    /**
      * Returns the suffix of the url. The result should be identical to
      * that of calling getRootURLContext().getRemainingName(), but
      * without the overhead of doing anything with the prefix like
      * creating a context.
      *<p>
      * This method returns a Name instead of a String because to give
      * the provider an opportunity to return a Name (for example,
      * for weakly separated naming systems like COS naming).
      *<p>
      * The default implementation uses skips 'prefix', calls
      * UrlUtil.decode() on it, and returns the result as a single component
      * CompositeName.
      * Subclass should override if this is not appropriate.
      * This method is used only by rename().
      * If rename() is supported for a particular URL scheme,
      * getRootURLContext(), getURLPrefix(), and getURLSuffix()
      * must be in sync wrt how URLs are parsed and returned.
      *<p>
      * For many URL schemes, this method is very similar to URL.getFile(),
      * except getFile() will return a leading slash in the
      * 2nd, 3rd, and 4th cases. For schemes like "ldap" and "iiop",
      * the leading slash must be skipped before the name is an acceptable
      * format for operation by the Context methods. For schemes that treat the
      * leading slash as significant (such as "file"),
      * the subclass must override getURLSuffix() to get the correct behavior.
      * Remember, the behavior must match getRootURLContext().
      *
      * <pre>{@code
      * URL                                     Suffix
      * foo://host:port                         <empty string>
      * foo://host:port/rest/of/name            rest/of/name
      * foo:///rest/of/name                     rest/of/name
      * foo:/rest/of/name                       rest/of/name
      * foo:rest/of/name                        rest/of/name
      * }</pre>
      */
    protected Name getURLSuffix(String prefix, String url) throws NamingException {
        String suffix = url.substring(prefix.length());
        if (suffix.length() == 0) {
            return new CompositeName();
        }

        if (suffix.charAt(0) == '/') {
            suffix = suffix.substring(1); // skip leading slash
        }

        try {
            return new CompositeName().add(UrlUtil.decode(suffix));
        } catch (MalformedURLException e) {
            throw new InvalidNameException(e.getMessage());
        }
    }

    /**
      * Finds the prefix of a URL.
      * Default implementation looks for slashes and then extracts
      * prefixes using String.substring().
      * Subclass should override if this is not appropriate.
      * This method is used only by rename().
      * If rename() is supported for a particular URL scheme,
      * getRootURLContext(), getURLPrefix(), and getURLSuffix()
      * must be in sync wrt how URLs are parsed and returned.
      *<p>
      * URL                                     Prefix
      * foo://host:port                         foo://host:port
      * foo://host:port/rest/of/name            foo://host:port
      * foo:///rest/of/name                     foo://
      * foo:/rest/of/name                       foo:
      * foo:rest/of/name                        foo:
      */
    protected String getURLPrefix(String url) throws NamingException {
        int start = url.indexOf(':');

        if (start < 0) {
            throw new OperationNotSupportedException("Invalid URL: " + url);
        }
        ++start; // skip ':'

        if (url.startsWith("//", start)) {
            start += 2;  // skip double slash

            // find last slash
            int posn = url.indexOf('/', start);
            if (posn >= 0) {
                start = posn;
            } else {
                start = url.length();  // rest of URL
            }
        }

        // else 0 or 1 initial slashes; start is unchanged
        return url.substring(0, start);
    }

    /**
     * Determines whether two URLs are the same.
     * Default implementation uses String.equals().
     * Subclass should override if this is not appropriate.
     * This method is used by rename().
     */
    protected boolean urlEquals(String url1, String url2) {
        return url1.equals(url2);
    }

    /**
     * Gets the context in which to continue the operation. This method
     * is called when this context is asked to process a multicomponent
     * Name in which the first component is a URL.
     * Treat the first component like a junction: resolve it and then use
     * NamingManager.getContinuationContext() to get the target context in
     * which to operate on the remainder of the name (n.getSuffix(1)).
     */
    protected Context getContinuationContext(Name n) throws NamingException {
        Object obj = lookup(n.get(0));
        CannotProceedException cpe = new CannotProceedException();
        cpe.setResolvedObj(obj);
        cpe.setEnvironment(myEnv);
        return NamingManager.getContinuationContext(cpe);
    }

    public Object lookup(String name) throws NamingException {
        ResolveResult res = getRootURLContext(name, myEnv);
        Context ctx = (Context)res.getResolvedObj();
        try {
            return ctx.lookup(res.getRemainingName());
        } finally {
            ctx.close();
        }
    }

    public Object lookup(Name name) throws NamingException {
        if (name.size() == 1) {
            return lookup(name.get(0));
        } else {
            Context ctx = getContinuationContext(name);
            try {
                return ctx.lookup(name.getSuffix(1));
            } finally {
                ctx.close();
            }
        }
    }

    public void bind(String name, Object obj) throws NamingException {
        ResolveResult res = getRootURLContext(name, myEnv);
        Context ctx = (Context)res.getResolvedObj();
        try {
            ctx.bind(res.getRemainingName(), obj);
        } finally {
            ctx.close();
        }
    }

    public void bind(Name name, Object obj) throws NamingException {
        if (name.size() == 1) {
            bind(name.get(0), obj);
        } else {
            Context ctx = getContinuationContext(name);
            try {
                ctx.bind(name.getSuffix(1), obj);
            } finally {
                ctx.close();
            }
        }
    }

    public void rebind(String name, Object obj) throws NamingException {
        ResolveResult res = getRootURLContext(name, myEnv);
        Context ctx = (Context)res.getResolvedObj();
        try {
            ctx.rebind(res.getRemainingName(), obj);
        } finally {
            ctx.close();
        }
    }

    public void rebind(Name name, Object obj) throws NamingException {
        if (name.size() == 1) {
            rebind(name.get(0), obj);
        } else {
            Context ctx = getContinuationContext(name);
            try {
                ctx.rebind(name.getSuffix(1), obj);
            } finally {
                ctx.close();
            }
        }
    }

    public void unbind(String name) throws NamingException {
        ResolveResult res = getRootURLContext(name, myEnv);
        Context ctx = (Context)res.getResolvedObj();
        try {
            ctx.unbind(res.getRemainingName());
        } finally {
            ctx.close();
        }
    }

    public void unbind(Name name) throws NamingException {
        if (name.size() == 1) {
            unbind(name.get(0));
        } else {
            Context ctx = getContinuationContext(name);
            try {
                ctx.unbind(name.getSuffix(1));
            } finally {
                ctx.close();
            }
        }
    }

    public void rename(String oldName, String newName) throws NamingException {
        String oldPrefix = getURLPrefix(oldName);
        String newPrefix = getURLPrefix(newName);
        if (!urlEquals(oldPrefix, newPrefix)) {
            throw new OperationNotSupportedException(
                "Renaming using different URL prefixes not supported : " +
                oldName + " " + newName);
        }

        ResolveResult res = getRootURLContext(oldName, myEnv);
        Context ctx = (Context)res.getResolvedObj();
        try {
            ctx.rename(res.getRemainingName(), getURLSuffix(newPrefix, newName));
        } finally {
            ctx.close();
        }
    }

    public void rename(Name name, Name newName) throws NamingException {
        if (name.size() == 1) {
            if (newName.size() != 1) {
                throw new OperationNotSupportedException(
            "Renaming to a Name with more components not supported: " + newName);
            }
            rename(name.get(0), newName.get(0));
        } else {
            // > 1 component with 1st one being URL
            // URLs must be identical; cannot deal with diff URLs
            if (!urlEquals(name.get(0), newName.get(0))) {
                throw new OperationNotSupportedException(
                    "Renaming using different URLs as first components not supported: " +
                    name + " " + newName);
            }

            Context ctx = getContinuationContext(name);
            try {
                ctx.rename(name.getSuffix(1), newName.getSuffix(1));
            } finally {
                ctx.close();
            }
        }
    }

    public NamingEnumeration<NameClassPair> list(String name)   throws NamingException {
        ResolveResult res = getRootURLContext(name, myEnv);
        Context ctx = (Context)res.getResolvedObj();
        try {
            return ctx.list(res.getRemainingName());
        } finally {
            ctx.close();
        }
    }

    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        if (name.size() == 1) {
            return list(name.get(0));
        } else {
            Context ctx = getContinuationContext(name);
            try {
                return ctx.list(name.getSuffix(1));
            } finally {
                ctx.close();
            }
        }
    }

    public NamingEnumeration<Binding> listBindings(String name)
        throws NamingException {
        ResolveResult res = getRootURLContext(name, myEnv);
        Context ctx = (Context)res.getResolvedObj();
        try {
            return ctx.listBindings(res.getRemainingName());
        } finally {
            ctx.close();
        }
    }

    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        if (name.size() == 1) {
            return listBindings(name.get(0));
        } else {
            Context ctx = getContinuationContext(name);
            try {
                return ctx.listBindings(name.getSuffix(1));
            } finally {
                ctx.close();
            }
        }
    }

    public void destroySubcontext(String name) throws NamingException {
        ResolveResult res = getRootURLContext(name, myEnv);
        Context ctx = (Context)res.getResolvedObj();
        try {
            ctx.destroySubcontext(res.getRemainingName());
        } finally {
            ctx.close();
        }
    }

    public void destroySubcontext(Name name) throws NamingException {
        if (name.size() == 1) {
            destroySubcontext(name.get(0));
        } else {
            Context ctx = getContinuationContext(name);
            try {
                ctx.destroySubcontext(name.getSuffix(1));
            } finally {
                ctx.close();
            }
        }
    }

    public Context createSubcontext(String name) throws NamingException {
        ResolveResult res = getRootURLContext(name, myEnv);
        Context ctx = (Context)res.getResolvedObj();
        try {
            return ctx.createSubcontext(res.getRemainingName());
        } finally {
            ctx.close();
        }
    }

    public Context createSubcontext(Name name) throws NamingException {
        if (name.size() == 1) {
            return createSubcontext(name.get(0));
        } else {
            Context ctx = getContinuationContext(name);
            try {
                return ctx.createSubcontext(name.getSuffix(1));
            } finally {
                ctx.close();
            }
        }
    }

    public Object lookupLink(String name) throws NamingException {
        ResolveResult res = getRootURLContext(name, myEnv);
        Context ctx = (Context)res.getResolvedObj();
        try {
            return ctx.lookupLink(res.getRemainingName());
        } finally {
            ctx.close();
        }
    }

    public Object lookupLink(Name name) throws NamingException {
        if (name.size() == 1) {
            return lookupLink(name.get(0));
        } else {
            Context ctx = getContinuationContext(name);
            try {
                return ctx.lookupLink(name.getSuffix(1));
            } finally {
                ctx.close();
            }
        }
    }

    public NameParser getNameParser(String name) throws NamingException {
        ResolveResult res = getRootURLContext(name, myEnv);
        Context ctx = (Context)res.getResolvedObj();
        try {
            return ctx.getNameParser(res.getRemainingName());
        } finally {
            ctx.close();
        }
    }

    public NameParser getNameParser(Name name) throws NamingException {
        if (name.size() == 1) {
            return getNameParser(name.get(0));
        } else {
            Context ctx = getContinuationContext(name);
            try {
                return ctx.getNameParser(name.getSuffix(1));
            } finally {
                ctx.close();
            }
        }
    }

    public String composeName(String name, String prefix)
        throws NamingException {
            if (prefix.isEmpty()) {
                return name;
            } else if (name.isEmpty()) {
                return prefix;
            } else {
                return (prefix + "/" + name);
            }
    }

    public Name composeName(Name name, Name prefix) throws NamingException {
        Name result = (Name)prefix.clone();
        result.addAll(name);
        return result;
    }

    public Object removeFromEnvironment(String propName)
        throws NamingException {
            if (myEnv == null) {
                return null;
            }
            return myEnv.remove(propName);
    }

    public Object addToEnvironment(String propName, Object propVal)
        throws NamingException {
            if (myEnv == null) {
                myEnv = new Hashtable<String, Object>(11, 0.75f);
            }
            return myEnv.put(propName, propVal);
    }

    @SuppressWarnings("unchecked") // clone()
    public Hashtable<String, Object> getEnvironment() throws NamingException {
        if (myEnv == null) {
            return new Hashtable<>(5, 0.75f);
        } else {
            return (Hashtable<String, Object>)myEnv.clone();
        }
    }

/*
// To test, declare getURLPrefix and getURLSuffix static.

    public static void main(String[] args) throws Exception {
        String[] tests = {"file://host:port",
                          "file:///rest/of/name",
                          "file://host:port/rest/of/name",
                          "file:/rest/of/name",
                          "file:rest/of/name"};
        for (int i = 0; i < tests.length; i++) {
            String pre = getURLPrefix(tests[i]);
            System.out.println(pre);
            System.out.println(getURLSuffix(pre, tests[i]));
        }
    }
*/
}
