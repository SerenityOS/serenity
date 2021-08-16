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
package com.sun.jndi.toolkit.url;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.spi.ResolveResult;
import javax.naming.spi.DirectoryManager;

import java.util.Hashtable;

/**
 * This abstract class is a generic URL DirContext that accepts as the
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

public abstract class GenericURLDirContext extends GenericURLContext
implements DirContext {

    protected GenericURLDirContext(Hashtable<?,?> env) {
        super(env);
    }

    /**
     * Gets the context in which to continue the operation. This method
     * is called when this context is asked to process a multicomponent
     * Name in which the first component is a URL.
     * Treat the first component like a junction: resolve it and then use
     * DirectoryManager.getContinuationDirContext() to get the target context in
     * which to operate on the remainder of the name (n.getSuffix(1)).
     * Do this in case intermediate contexts are not DirContext.
     */
    protected DirContext getContinuationDirContext(Name n) throws NamingException {
        Object obj = lookup(n.get(0));
        CannotProceedException cpe = new CannotProceedException();
        cpe.setResolvedObj(obj);
        cpe.setEnvironment(myEnv);
        return DirectoryManager.getContinuationDirContext(cpe);
    }


    public Attributes getAttributes(String name) throws NamingException {
        ResolveResult res = getRootURLContext(name, myEnv);
        DirContext ctx = (DirContext)res.getResolvedObj();
        try {
            return ctx.getAttributes(res.getRemainingName());
        } finally {
            ctx.close();
        }
    }

    public Attributes getAttributes(Name name) throws NamingException  {
        if (name.size() == 1) {
            return getAttributes(name.get(0));
        } else {
            DirContext ctx = getContinuationDirContext(name);
            try {
                return ctx.getAttributes(name.getSuffix(1));
            } finally {
                ctx.close();
            }
        }
    }

    public Attributes getAttributes(String name, String[] attrIds)
        throws NamingException {
            ResolveResult res = getRootURLContext(name, myEnv);
            DirContext ctx = (DirContext)res.getResolvedObj();
            try {
                return ctx.getAttributes(res.getRemainingName(), attrIds);
            } finally {
                ctx.close();
            }
    }

    public Attributes getAttributes(Name name, String[] attrIds)
        throws NamingException {
            if (name.size() == 1) {
                return getAttributes(name.get(0), attrIds);
            } else {
                DirContext ctx = getContinuationDirContext(name);
                try {
                    return ctx.getAttributes(name.getSuffix(1), attrIds);
                } finally {
                    ctx.close();
                }
            }
    }

    public void modifyAttributes(String name, int mod_op, Attributes attrs)
        throws NamingException {
            ResolveResult res = getRootURLContext(name, myEnv);
            DirContext ctx = (DirContext)res.getResolvedObj();
            try {
                ctx.modifyAttributes(res.getRemainingName(), mod_op, attrs);
            } finally {
                ctx.close();
            }
    }

    public void modifyAttributes(Name name, int mod_op, Attributes attrs)
        throws NamingException {
            if (name.size() == 1) {
                modifyAttributes(name.get(0), mod_op, attrs);
            } else {
                DirContext ctx = getContinuationDirContext(name);
                try {
                    ctx.modifyAttributes(name.getSuffix(1), mod_op, attrs);
                } finally {
                    ctx.close();
                }
            }
    }

    public void modifyAttributes(String name, ModificationItem[] mods)
        throws NamingException {
            ResolveResult res = getRootURLContext(name, myEnv);
            DirContext ctx = (DirContext)res.getResolvedObj();
            try {
                ctx.modifyAttributes(res.getRemainingName(), mods);
            } finally {
                ctx.close();
            }
    }

    public void modifyAttributes(Name name, ModificationItem[] mods)
        throws NamingException  {
            if (name.size() == 1) {
                modifyAttributes(name.get(0), mods);
            } else {
                DirContext ctx = getContinuationDirContext(name);
                try {
                    ctx.modifyAttributes(name.getSuffix(1), mods);
                } finally {
                    ctx.close();
                }
            }
    }

    public void bind(String name, Object obj, Attributes attrs)
        throws NamingException {
            ResolveResult res = getRootURLContext(name, myEnv);
            DirContext ctx = (DirContext)res.getResolvedObj();
            try {
                ctx.bind(res.getRemainingName(), obj, attrs);
            } finally {
                ctx.close();
            }
    }

    public void bind(Name name, Object obj, Attributes attrs)
        throws NamingException {
            if (name.size() == 1) {
                bind(name.get(0), obj, attrs);
            } else {
                DirContext ctx = getContinuationDirContext(name);
                try {
                    ctx.bind(name.getSuffix(1), obj, attrs);
                } finally {
                    ctx.close();
                }
            }
    }

    public void rebind(String name, Object obj, Attributes attrs)
        throws NamingException {
            ResolveResult res = getRootURLContext(name, myEnv);
            DirContext ctx = (DirContext)res.getResolvedObj();
            try {
                ctx.rebind(res.getRemainingName(), obj, attrs);
            } finally {
                ctx.close();
            }
    }

    public void rebind(Name name, Object obj, Attributes attrs)
        throws NamingException {
            if (name.size() == 1) {
                rebind(name.get(0), obj, attrs);
            } else {
                DirContext ctx = getContinuationDirContext(name);
                try {
                    ctx.rebind(name.getSuffix(1), obj, attrs);
                } finally {
                    ctx.close();
                }
            }
    }

    public DirContext createSubcontext(String name, Attributes attrs)
        throws NamingException {
            ResolveResult res = getRootURLContext(name, myEnv);
            DirContext ctx = (DirContext)res.getResolvedObj();
            try {
                return ctx.createSubcontext(res.getRemainingName(), attrs);
            } finally {
                ctx.close();
            }
    }

    public DirContext createSubcontext(Name name, Attributes attrs)
        throws NamingException {
            if (name.size() == 1) {
                return createSubcontext(name.get(0), attrs);
            } else {
                DirContext ctx = getContinuationDirContext(name);
                try {
                    return ctx.createSubcontext(name.getSuffix(1), attrs);
                } finally {
                    ctx.close();
                }
            }
    }

    public DirContext getSchema(String name) throws NamingException {
        ResolveResult res = getRootURLContext(name, myEnv);
        DirContext ctx = (DirContext)res.getResolvedObj();
        return ctx.getSchema(res.getRemainingName());
    }

    public DirContext getSchema(Name name) throws NamingException {
        if (name.size() == 1) {
            return getSchema(name.get(0));
        } else {
            DirContext ctx = getContinuationDirContext(name);
            try {
                return ctx.getSchema(name.getSuffix(1));
            } finally {
                ctx.close();
            }
        }
    }

    public DirContext getSchemaClassDefinition(String name)
        throws NamingException {
            ResolveResult res = getRootURLContext(name, myEnv);
            DirContext ctx = (DirContext)res.getResolvedObj();
            try {
                return ctx.getSchemaClassDefinition(res.getRemainingName());
            } finally {
                ctx.close();
            }
    }

    public DirContext getSchemaClassDefinition(Name name)
        throws NamingException {
            if (name.size() == 1) {
                return getSchemaClassDefinition(name.get(0));
            } else {
                DirContext ctx = getContinuationDirContext(name);
                try {
                    return ctx.getSchemaClassDefinition(name.getSuffix(1));
                } finally {
                    ctx.close();
                }
            }
    }

    public NamingEnumeration<SearchResult> search(String name,
        Attributes matchingAttributes)
        throws NamingException {
            ResolveResult res = getRootURLContext(name, myEnv);
            DirContext ctx = (DirContext)res.getResolvedObj();
            try {
                return ctx.search(res.getRemainingName(), matchingAttributes);
            } finally {
                ctx.close();
            }
    }

    public NamingEnumeration<SearchResult> search(Name name,
        Attributes matchingAttributes)
        throws NamingException {
            if (name.size() == 1) {
                return search(name.get(0), matchingAttributes);
            } else {
                DirContext ctx = getContinuationDirContext(name);
                try {
                    return ctx.search(name.getSuffix(1), matchingAttributes);
                } finally {
                    ctx.close();
                }
            }
    }

    public NamingEnumeration<SearchResult> search(String name,
        Attributes matchingAttributes,
        String[] attributesToReturn)
        throws NamingException {
            ResolveResult res = getRootURLContext(name, myEnv);
            DirContext ctx = (DirContext)res.getResolvedObj();
            try {
                return ctx.search(res.getRemainingName(),
                    matchingAttributes, attributesToReturn);
            } finally {
                ctx.close();
            }
    }

    public NamingEnumeration<SearchResult> search(Name name,
        Attributes matchingAttributes,
        String[] attributesToReturn)
        throws NamingException {
            if (name.size() == 1) {
                return search(name.get(0), matchingAttributes,
                    attributesToReturn);
            } else {
                DirContext ctx = getContinuationDirContext(name);
                try {
                    return ctx.search(name.getSuffix(1),
                        matchingAttributes, attributesToReturn);
                } finally {
                    ctx.close();
                }
            }
    }

    public NamingEnumeration<SearchResult> search(String name,
        String filter,
        SearchControls cons)
        throws NamingException {
            ResolveResult res = getRootURLContext(name, myEnv);
            DirContext ctx = (DirContext)res.getResolvedObj();
            try {
                return ctx.search(res.getRemainingName(), filter, cons);
            } finally {
                ctx.close();
            }
    }

    public NamingEnumeration<SearchResult> search(Name name,
        String filter,
        SearchControls cons)
        throws NamingException {
            if (name.size() == 1) {
                return search(name.get(0), filter, cons);
            } else {
                DirContext ctx = getContinuationDirContext(name);
                try {
                    return ctx.search(name.getSuffix(1), filter, cons);
                } finally {
                    ctx.close();
                }
            }
    }

    public NamingEnumeration<SearchResult> search(String name,
        String filterExpr,
        Object[] filterArgs,
        SearchControls cons)
        throws NamingException {
            ResolveResult res = getRootURLContext(name, myEnv);
            DirContext ctx = (DirContext)res.getResolvedObj();
            try {
                return
                    ctx.search(res.getRemainingName(), filterExpr, filterArgs, cons);
            } finally {
                ctx.close();
            }
    }

    public NamingEnumeration<SearchResult> search(Name name,
        String filterExpr,
        Object[] filterArgs,
        SearchControls cons)
        throws NamingException {
            if (name.size() == 1) {
                return search(name.get(0), filterExpr, filterArgs, cons);
            } else {
                DirContext ctx = getContinuationDirContext(name);
                try {
                return ctx.search(name.getSuffix(1), filterExpr, filterArgs, cons);
                } finally {
                    ctx.close();
                }
            }
    }
}
