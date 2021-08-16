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

package com.sun.jndi.ldap;

import javax.naming.*;
import javax.naming.directory.*;
import java.util.Hashtable;
import com.sun.jndi.toolkit.dir.HierMemDirCtx;

/**
 * This is the class used to implement LDAP's GetSchema call.
 *
 * It subclasses HierMemDirContext for most of the functionality. It
 * overrides functions that cause the schema definitions to change.
 * In such a case, it write the schema to the LdapServer and (assuming
 * there are no errors), calls it's superclass's equivalent function.
 * Thus, the schema tree and the LDAP server's schema attributes are
 * always in sync.
 */

final class LdapSchemaCtx extends HierMemDirCtx {

    private static final boolean debug = false;

    private static final int LEAF = 0;  // schema object (e.g. attribute type defn)
    private static final int SCHEMA_ROOT = 1;   // schema tree root
    static final int OBJECTCLASS_ROOT = 2;   // root of object class subtree
    static final int ATTRIBUTE_ROOT = 3;     // root of attribute type subtree
    static final int SYNTAX_ROOT = 4;        // root of syntax subtree
    static final int MATCHRULE_ROOT = 5;     // root of matching rule subtree
    static final int OBJECTCLASS = 6;   // an object class definition
    static final int ATTRIBUTE = 7;     // an attribute type definition
    static final int SYNTAX = 8;        // a syntax definition
    static final int MATCHRULE = 9;     // a matching rule definition

    private SchemaInfo info= null;
    private boolean setupMode = true;

    private int objectType;

    static DirContext createSchemaTree(Hashtable<String,Object> env,
            String subschemasubentry, LdapCtx schemaEntry,
            Attributes schemaAttrs, boolean netscapeBug)
        throws NamingException {
            try {
                LdapSchemaParser parser = new LdapSchemaParser(netscapeBug);

                SchemaInfo allinfo = new SchemaInfo(subschemasubentry,
                    schemaEntry, parser);

                LdapSchemaCtx root = new LdapSchemaCtx(SCHEMA_ROOT, env, allinfo);
                LdapSchemaParser.LDAP2JNDISchema(schemaAttrs, root);
                return root;
            } catch (NamingException e) {
                schemaEntry.close(); // cleanup
                throw e;
            }
    }

    // Called by createNewCtx
    private LdapSchemaCtx(int objectType, Hashtable<String,Object> environment,
                          SchemaInfo info) {
        super(environment, LdapClient.caseIgnore);

        this.objectType = objectType;
        this.info = info;
    }

    // override HierMemDirCtx.close to prevent premature GC of shared data
    public void close() throws NamingException {
        info.close();
    }

    // override to ignore obj and use attrs
    // treat same as createSubcontext
    public final void bind(Name name, Object obj, Attributes attrs)
        throws NamingException {
        if (!setupMode) {
            if (obj != null) {
                throw new IllegalArgumentException("obj must be null");
            }

            // Update server
            addServerSchema(attrs);
        }

        // Update in-memory copy
        LdapSchemaCtx newEntry =
            (LdapSchemaCtx)super.doCreateSubcontext(name, attrs);
    }

    protected final void doBind(Name name, Object obj, Attributes attrs,
        boolean useFactory) throws NamingException {
        if (!setupMode) {
            throw new SchemaViolationException(
                "Cannot bind arbitrary object; use createSubcontext()");
        } else {
            super.doBind(name, obj, attrs, false); // always ignore factories
        }
    }

    // override to use bind() instead
    public final void rebind(Name name, Object obj, Attributes attrs)
        throws NamingException {
        try {
            doLookup(name, false);
            throw new SchemaViolationException(
                "Cannot replace existing schema object");
        } catch (NameNotFoundException e) {
            bind(name, obj, attrs);
        }
    }

    protected final void doRebind(Name name, Object obj, Attributes attrs,
        boolean useFactory) throws NamingException {
        if (!setupMode) {
            throw new SchemaViolationException(
                "Cannot bind arbitrary object; use createSubcontext()");
        } else {
            super.doRebind(name, obj, attrs, false); // always ignore factories
        }
    }

    protected final void doUnbind(Name name) throws NamingException {
        if (!setupMode) {
            // Update server
            try {
                // Lookup entry from memory
                LdapSchemaCtx target = (LdapSchemaCtx)doLookup(name, false);

                deleteServerSchema(target.attrs);
            } catch (NameNotFoundException e) {
                return;
            }
        }
        // Update in-memory copy
        super.doUnbind(name);
    }

    protected final void doRename(Name oldname, Name newname)
        throws NamingException {
        if (!setupMode) {
            throw new SchemaViolationException("Cannot rename a schema object");
        } else {
            super.doRename(oldname, newname);
        }
    }

    protected final void doDestroySubcontext(Name name) throws NamingException {
        if (!setupMode) {
            // Update server
            try {
                // Lookup entry from memory
                LdapSchemaCtx target = (LdapSchemaCtx)doLookup(name, false);

                deleteServerSchema(target.attrs);
            } catch (NameNotFoundException e) {
                return;
            }
        }

        // Update in-memory copy
        super.doDestroySubcontext(name);
     }

    // Called to create oc, attr, syntax or matching rule roots and leaf entries
    final LdapSchemaCtx setup(int objectType, String name, Attributes attrs)
        throws NamingException{
            try {
                setupMode = true;
                LdapSchemaCtx answer =
                    (LdapSchemaCtx) super.doCreateSubcontext(
                        new CompositeName(name), attrs);

                answer.objectType = objectType;
                answer.setupMode = false;
                return answer;
            } finally {
                setupMode = false;
            }
    }

    protected final DirContext doCreateSubcontext(Name name, Attributes attrs)
        throws NamingException {

        if (attrs == null || attrs.size() == 0) {
            throw new SchemaViolationException(
                "Must supply attributes describing schema");
        }

        if (!setupMode) {
            // Update server
            addServerSchema(attrs);
        }

        // Update in-memory copy
        LdapSchemaCtx newEntry =
            (LdapSchemaCtx) super.doCreateSubcontext(name, attrs);
        return newEntry;
    }

    private static final Attributes deepClone(Attributes orig)
        throws NamingException {
        BasicAttributes copy = new BasicAttributes(true);
        NamingEnumeration<? extends Attribute> attrs = orig.getAll();
        while (attrs.hasMore()) {
            copy.put((Attribute)attrs.next().clone());
        }
        return copy;
    }

    protected final void doModifyAttributes(ModificationItem[] mods)
        throws NamingException {
        if (setupMode) {
            super.doModifyAttributes(mods);
        } else {
            Attributes copy = deepClone(attrs);

            // Apply modifications to copy
            applyMods(mods, copy);

            // Update server copy
            modifyServerSchema(attrs, copy);

            // Update in-memory copy
            attrs = copy;
        }
    }

    // we override this so the superclass creates the right kind of contexts
    // Default is to create LEAF objects; caller will change after creation
    // if necessary
    protected final HierMemDirCtx createNewCtx() {
        LdapSchemaCtx ctx = new LdapSchemaCtx(LEAF, myEnv, info);
        return ctx;
    }


    private final void addServerSchema(Attributes attrs)
        throws NamingException {
        Attribute schemaAttr;

        switch (objectType) {
        case OBJECTCLASS_ROOT:
            schemaAttr = info.parser.stringifyObjDesc(attrs);
            break;

        case ATTRIBUTE_ROOT:
            schemaAttr = info.parser.stringifyAttrDesc(attrs);
            break;

        case SYNTAX_ROOT:
            schemaAttr = info.parser.stringifySyntaxDesc(attrs);
            break;

        case MATCHRULE_ROOT:
            schemaAttr = info.parser.stringifyMatchRuleDesc(attrs);
            break;

        case SCHEMA_ROOT:
            throw new SchemaViolationException(
                "Cannot create new entry under schema root");

        default:
            throw new SchemaViolationException(
                "Cannot create child of schema object");
        }

        Attributes holder = new BasicAttributes(true);
        holder.put(schemaAttr);
        //System.err.println((String)schemaAttr.get());

        info.modifyAttributes(myEnv, DirContext.ADD_ATTRIBUTE, holder);

    }

    /**
      * When we delete an entry, we use the original to make sure that
      * any formatting inconsistencies are eliminated.
      * This is because we're just deleting a value from an attribute
      * on the server and there might not be any checks for extra spaces
      * or parens.
      */
    private final void deleteServerSchema(Attributes origAttrs)
        throws NamingException {

        Attribute origAttrVal;

        switch (objectType) {
        case OBJECTCLASS_ROOT:
            origAttrVal = info.parser.stringifyObjDesc(origAttrs);
            break;

        case ATTRIBUTE_ROOT:
            origAttrVal = info.parser.stringifyAttrDesc(origAttrs);
            break;

        case SYNTAX_ROOT:
            origAttrVal = info.parser.stringifySyntaxDesc(origAttrs);
            break;

        case MATCHRULE_ROOT:
            origAttrVal = info.parser.stringifyMatchRuleDesc(origAttrs);
            break;

        case SCHEMA_ROOT:
            throw new SchemaViolationException(
                "Cannot delete schema root");

        default:
            throw new SchemaViolationException(
                "Cannot delete child of schema object");
        }

        ModificationItem[] mods = new ModificationItem[1];
        mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, origAttrVal);

        info.modifyAttributes(myEnv, mods);
    }

    /**
      * When we modify an entry, we use the original attribute value
      * in the schema to make sure that any formatting inconsistencies
      * are eliminated. A modification is done by deleting the original
      * value and adding a new value with the modification.
      */
    private final void modifyServerSchema(Attributes origAttrs,
        Attributes newAttrs) throws NamingException {

        Attribute newAttrVal;
        Attribute origAttrVal;

        switch (objectType) {
        case OBJECTCLASS:
            origAttrVal = info.parser.stringifyObjDesc(origAttrs);
            newAttrVal = info.parser.stringifyObjDesc(newAttrs);
            break;

        case ATTRIBUTE:
            origAttrVal = info.parser.stringifyAttrDesc(origAttrs);
            newAttrVal = info.parser.stringifyAttrDesc(newAttrs);
            break;

        case SYNTAX:
            origAttrVal = info.parser.stringifySyntaxDesc(origAttrs);
            newAttrVal = info.parser.stringifySyntaxDesc(newAttrs);
            break;

        case MATCHRULE:
            origAttrVal = info.parser.stringifyMatchRuleDesc(origAttrs);
            newAttrVal = info.parser.stringifyMatchRuleDesc(newAttrs);
            break;

        default:
            throw new SchemaViolationException(
                "Cannot modify schema root");
        }

        ModificationItem[] mods = new ModificationItem[2];
        mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, origAttrVal);
        mods[1] = new ModificationItem(DirContext.ADD_ATTRIBUTE, newAttrVal);

        info.modifyAttributes(myEnv, mods);
    }

    private static final class SchemaInfo {
        private LdapCtx schemaEntry;
        private String schemaEntryName;
        LdapSchemaParser parser;
        private String host;
        private int port;
        private boolean hasLdapsScheme;

        SchemaInfo(String schemaEntryName, LdapCtx schemaEntry,
            LdapSchemaParser parser) {
            this.schemaEntryName = schemaEntryName;
            this.schemaEntry = schemaEntry;
            this.parser = parser;
            this.port = schemaEntry.port_number;
            this.host = schemaEntry.hostname;
            this.hasLdapsScheme = schemaEntry.hasLdapsScheme;
        }

        synchronized void close() throws NamingException {
            if (schemaEntry != null) {
                schemaEntry.close();
                schemaEntry = null;
            }
        }

        private LdapCtx reopenEntry(Hashtable<?,?> env) throws NamingException {
            // Use subschemasubentry name as DN
            return new LdapCtx(schemaEntryName, host, port,
                                env, hasLdapsScheme);
        }

        synchronized void modifyAttributes(Hashtable<?,?> env,
                                           ModificationItem[] mods)
            throws NamingException {
            if (schemaEntry == null) {
                schemaEntry = reopenEntry(env);
            }
            schemaEntry.modifyAttributes("", mods);
        }

        synchronized void modifyAttributes(Hashtable<?,?> env, int mod,
            Attributes attrs) throws NamingException {
            if (schemaEntry == null) {
                schemaEntry = reopenEntry(env);
            }
            schemaEntry.modifyAttributes("", mod, attrs);
        }
    }
}
