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

package javax.naming.spi;

import java.util.Hashtable;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.CompositeName;
import javax.naming.NamingException;
import javax.naming.CannotProceedException;
import javax.naming.OperationNotSupportedException;
import javax.naming.Context;

import javax.naming.directory.DirContext;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.directory.ModificationItem;

/**
  * This class is the continuation context for invoking DirContext methods.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  * @since 1.3
  */

class ContinuationDirContext extends ContinuationContext implements DirContext {

    ContinuationDirContext(CannotProceedException cpe, Hashtable<?,?> env) {
        super(cpe, env);
    }

    protected DirContextNamePair getTargetContext(Name name)
            throws NamingException {

        if (cpe.getResolvedObj() == null)
            throw (NamingException)cpe.fillInStackTrace();

        Context ctx = NamingManager.getContext(cpe.getResolvedObj(),
                                               cpe.getAltName(),
                                               cpe.getAltNameCtx(),
                                               env);
        if (ctx == null)
            throw (NamingException)cpe.fillInStackTrace();

        if (ctx instanceof DirContext)
            return new DirContextNamePair((DirContext)ctx, name);

        if (ctx instanceof Resolver) {
            Resolver res = (Resolver)ctx;
            ResolveResult rr = res.resolveToClass(name, DirContext.class);

            // Reached a DirContext; return result.
            DirContext dctx = (DirContext)rr.getResolvedObj();
            return (new DirContextNamePair(dctx, rr.getRemainingName()));
        }

        // Resolve all the way using lookup().  This may allow the operation
        // to succeed if it doesn't require the penultimate context.
        Object ultimate = ctx.lookup(name);
        if (ultimate instanceof DirContext) {
            return (new DirContextNamePair((DirContext)ultimate,
                                          new CompositeName()));
        }

        throw (NamingException)cpe.fillInStackTrace();
    }

    protected DirContextStringPair getTargetContext(String name)
            throws NamingException {

        if (cpe.getResolvedObj() == null)
            throw (NamingException)cpe.fillInStackTrace();

        Context ctx = NamingManager.getContext(cpe.getResolvedObj(),
                                               cpe.getAltName(),
                                               cpe.getAltNameCtx(),
                                               env);

        if (ctx instanceof DirContext)
            return new DirContextStringPair((DirContext)ctx, name);

        if (ctx instanceof Resolver) {
            Resolver res = (Resolver)ctx;
            ResolveResult rr = res.resolveToClass(name, DirContext.class);

            // Reached a DirContext; return result.
            DirContext dctx = (DirContext)rr.getResolvedObj();
            Name tmp = rr.getRemainingName();
            String remains = (tmp != null) ? tmp.toString() : "";
            return (new DirContextStringPair(dctx, remains));
        }

        // Resolve all the way using lookup().  This may allow the operation
        // to succeed if it doesn't require the penultimate context.
        Object ultimate = ctx.lookup(name);
        if (ultimate instanceof DirContext) {
            return (new DirContextStringPair((DirContext)ultimate, ""));
        }

        throw (NamingException)cpe.fillInStackTrace();
    }

    public Attributes getAttributes(String name) throws NamingException {
        DirContextStringPair res = getTargetContext(name);
        return res.getDirContext().getAttributes(res.getString());
    }

    public Attributes getAttributes(String name, String[] attrIds)
        throws NamingException {
            DirContextStringPair res = getTargetContext(name);
            return res.getDirContext().getAttributes(res.getString(), attrIds);
        }

    public Attributes getAttributes(Name name) throws NamingException {
        DirContextNamePair res = getTargetContext(name);
        return res.getDirContext().getAttributes(res.getName());
    }

    public Attributes getAttributes(Name name, String[] attrIds)
        throws NamingException {
            DirContextNamePair res = getTargetContext(name);
            return res.getDirContext().getAttributes(res.getName(), attrIds);
        }

    public void modifyAttributes(Name name, int mod_op, Attributes attrs)
        throws NamingException  {
            DirContextNamePair res = getTargetContext(name);
            res.getDirContext().modifyAttributes(res.getName(), mod_op, attrs);
        }
    public void modifyAttributes(String name, int mod_op, Attributes attrs)
        throws NamingException  {
            DirContextStringPair res = getTargetContext(name);
            res.getDirContext().modifyAttributes(res.getString(), mod_op, attrs);
        }

    public void modifyAttributes(Name name, ModificationItem[] mods)
        throws NamingException  {
            DirContextNamePair res = getTargetContext(name);
            res.getDirContext().modifyAttributes(res.getName(), mods);
        }
    public void modifyAttributes(String name, ModificationItem[] mods)
        throws NamingException  {
            DirContextStringPair res = getTargetContext(name);
            res.getDirContext().modifyAttributes(res.getString(), mods);
        }

    public void bind(Name name, Object obj, Attributes attrs)
        throws NamingException  {
            DirContextNamePair res = getTargetContext(name);
            res.getDirContext().bind(res.getName(), obj, attrs);
        }
    public void bind(String name, Object obj, Attributes attrs)
        throws NamingException  {
            DirContextStringPair res = getTargetContext(name);
            res.getDirContext().bind(res.getString(), obj, attrs);
        }

    public void rebind(Name name, Object obj, Attributes attrs)
                throws NamingException {
            DirContextNamePair res = getTargetContext(name);
            res.getDirContext().rebind(res.getName(), obj, attrs);
        }
    public void rebind(String name, Object obj, Attributes attrs)
                throws NamingException {
            DirContextStringPair res = getTargetContext(name);
            res.getDirContext().rebind(res.getString(), obj, attrs);
        }

    public DirContext createSubcontext(Name name, Attributes attrs)
                throws NamingException  {
            DirContextNamePair res = getTargetContext(name);
            return res.getDirContext().createSubcontext(res.getName(), attrs);
        }

    public DirContext createSubcontext(String name, Attributes attrs)
                throws NamingException  {
            DirContextStringPair res = getTargetContext(name);
            return
                res.getDirContext().createSubcontext(res.getString(), attrs);
        }

    public NamingEnumeration<SearchResult> search(Name name,
                                    Attributes matchingAttributes,
                                    String[] attributesToReturn)
        throws NamingException  {
            DirContextNamePair res = getTargetContext(name);
            return res.getDirContext().search(res.getName(), matchingAttributes,
                                             attributesToReturn);
        }

    public NamingEnumeration<SearchResult> search(String name,
                                    Attributes matchingAttributes,
                                    String[] attributesToReturn)
        throws NamingException  {
            DirContextStringPair res = getTargetContext(name);
            return res.getDirContext().search(res.getString(),
                                             matchingAttributes,
                                             attributesToReturn);
        }

    public NamingEnumeration<SearchResult> search(Name name,
                                    Attributes matchingAttributes)
        throws NamingException  {
            DirContextNamePair res = getTargetContext(name);
            return res.getDirContext().search(res.getName(), matchingAttributes);
        }
    public NamingEnumeration<SearchResult> search(String name,
                                    Attributes matchingAttributes)
        throws NamingException  {
            DirContextStringPair res = getTargetContext(name);
            return res.getDirContext().search(res.getString(),
                                             matchingAttributes);
        }

    public NamingEnumeration<SearchResult> search(Name name,
                                    String filter,
                                    SearchControls cons)
        throws NamingException {
            DirContextNamePair res = getTargetContext(name);
            return res.getDirContext().search(res.getName(), filter, cons);
        }

    public NamingEnumeration<SearchResult> search(String name,
                                    String filter,
                                    SearchControls cons)
        throws NamingException {
            DirContextStringPair res = getTargetContext(name);
            return res.getDirContext().search(res.getString(), filter, cons);
        }

    public NamingEnumeration<SearchResult> search(Name name,
                                    String filterExpr,
                                    Object[] args,
                                    SearchControls cons)
        throws NamingException {
            DirContextNamePair res = getTargetContext(name);
            return res.getDirContext().search(res.getName(), filterExpr, args,
                                             cons);
        }

    public NamingEnumeration<SearchResult> search(String name,
                                    String filterExpr,
                                    Object[] args,
                                    SearchControls cons)
        throws NamingException {
            DirContextStringPair res = getTargetContext(name);
            return res.getDirContext().search(res.getString(), filterExpr, args,
                                             cons);
        }

    public DirContext getSchema(String name) throws NamingException {
        DirContextStringPair res = getTargetContext(name);
        return res.getDirContext().getSchema(res.getString());
    }

    public DirContext getSchema(Name name) throws NamingException  {
        DirContextNamePair res = getTargetContext(name);
        return res.getDirContext().getSchema(res.getName());
    }

    public DirContext getSchemaClassDefinition(String name)
            throws NamingException  {
        DirContextStringPair res = getTargetContext(name);
        return res.getDirContext().getSchemaClassDefinition(res.getString());
    }

    public DirContext getSchemaClassDefinition(Name name)
            throws NamingException  {
        DirContextNamePair res = getTargetContext(name);
        return res.getDirContext().getSchemaClassDefinition(res.getName());
    }
}

class DirContextNamePair {
        DirContext ctx;
        Name name;

        DirContextNamePair(DirContext ctx, Name name) {
            this.ctx = ctx;
            this.name = name;
        }

        DirContext getDirContext() {
            return ctx;
        }

        Name getName() {
            return name;
        }
}

class DirContextStringPair {
        DirContext ctx;
        String str;

        DirContextStringPair(DirContext ctx, String str) {
            this.ctx = ctx;
            this.str = str;
        }

        DirContext getDirContext() {
            return ctx;
        }

        String getString() {
            return str;
        }
}
