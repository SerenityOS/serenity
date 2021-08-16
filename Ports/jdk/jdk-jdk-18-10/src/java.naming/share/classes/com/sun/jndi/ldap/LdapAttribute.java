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

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import javax.naming.*;
import javax.naming.directory.*;

/**
  * This subclass is used by LDAP to implement the schema calls.
  * Basically, it keeps track of which context it is an attribute of
  * so it can get the schema for that context.
  *
  * @author Jon Ruiz
  */
final class LdapAttribute extends BasicAttribute {

    static final long serialVersionUID = -4288716561020779584L;

    private transient DirContext baseCtx = null;
    private Name rdn = new CompositeName();

    // these two are used to reconstruct the baseCtx if this attribute has
    // been serialized (
    private String baseCtxURL;
    private Hashtable<String, ? super String> baseCtxEnv;

    @SuppressWarnings("unchecked") // clone()
    public Object clone() {
        LdapAttribute attr = new LdapAttribute(this.attrID, baseCtx, rdn);
        attr.values = (Vector<Object>)values.clone();
        return attr;
    }

    /**
      * Adds a new value to this attribute.
      *
      * @param attrVal The value to be added. If null, a null value is added to
      *                the attribute.
      * @return true Always returns true.
      */
    public boolean add(Object attrVal) {
        // LDAP attributes don't contain duplicate values so there's no need
        // to check if the value already exists before adding it.
        values.addElement(attrVal);
        return true;
    }

    /**
      * Constructs a new instance of an attribute.
      *
      * @param id The attribute's id. It cannot be null.
      */
    LdapAttribute(String id) {
        super(id);
    }

    /**
      * Constructs a new instance of an attribute.
      *
      * @param id The attribute's id. It cannot be null.
      * @param baseCtx  the baseCtx object of this attribute
      * @param rdn      the RDN of the entry (relative to baseCtx)
      */
    private LdapAttribute(String id, DirContext baseCtx, Name rdn) {
        super(id);
        this.baseCtx = baseCtx;
        this.rdn = rdn;
    }

     /**
      * Sets the baseCtx and rdn used to find the attribute's schema
      * Used by LdapCtx.setParents().
      */
    void setParent(DirContext baseCtx, Name rdn) {
        this.baseCtx = baseCtx;
        this.rdn = rdn;
    }

    /**
     * returns the ctx this attribute came from. This call allows
     * LDAPAttribute to be serializable. 'baseCtx' is transient so if
     * it is null, the `baseCtxURL` is used to reconstruct the context
     * to which calls are made.
     */
    private DirContext getBaseCtx() throws NamingException {
        if(baseCtx == null) {
            if (baseCtxEnv == null) {
                baseCtxEnv = new Hashtable<String, String>(3);
            }
            baseCtxEnv.put(Context.INITIAL_CONTEXT_FACTORY,
                             "com.sun.jndi.ldap.LdapCtxFactory");
            baseCtxEnv.put(Context.PROVIDER_URL,baseCtxURL);
            baseCtx = (new InitialDirContext(baseCtxEnv));
        }
        return baseCtx;
    }

    /**
     * This is called when the object is serialized. It is
     * overridden so that the appropriate class variables can be set
     * to re-construct the baseCtx when deserialized. Setting these
     * variables is costly, so it is only done if the object
     * is actually serialized.
     */
    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException {

        // setup internal state
        this.setBaseCtxInfo();

        // let the ObjectOutputStream do the real work of serialization
        out.defaultWriteObject();
    }

    /**
     * sets the information needed to reconstruct the baseCtx if
     * we are serialized. This must be called _before_ the object is
     * serialized!!!
     */
    @SuppressWarnings("unchecked") // clone()
    private void setBaseCtxInfo() {
        Hashtable<String, Object> realEnv = null;
        Hashtable<String, Object> secureEnv = null;

        if (baseCtx != null) {
            realEnv = ((LdapCtx)baseCtx).envprops;
            this.baseCtxURL = ((LdapCtx)baseCtx).getURL();
        }

        if(realEnv != null && realEnv.size() > 0 ) {
            // remove any security credentials - otherwise the serialized form
            // would store them in the clear
            for (String key : realEnv.keySet()){
                if (key.indexOf("security") != -1 ) {

                    //if we need to remove props, we must do it to a clone
                    //of the environment. cloning is expensive, so we only do
                    //it if we have to.
                    if(secureEnv == null) {
                        secureEnv = (Hashtable<String, Object>)realEnv.clone();
                    }
                    secureEnv.remove(key);
                }
            }
        }

        // set baseCtxEnv depending on whether we removed props or not
        this.baseCtxEnv = (secureEnv == null ? realEnv : secureEnv);
    }

    /**
      * Retrieves the syntax definition associated with this attribute.
      * @return This attribute's syntax definition.
      */
    public DirContext getAttributeSyntaxDefinition() throws NamingException {
        // get the syntax id from the attribute def
        DirContext schema = getBaseCtx().getSchema(rdn);
        DirContext attrDef = (DirContext)schema.lookup(
            LdapSchemaParser.ATTRIBUTE_DEFINITION_NAME + "/" + getID());

        Attribute syntaxAttr = attrDef.getAttributes("").get("SYNTAX");

        if(syntaxAttr == null || syntaxAttr.size() == 0) {
            throw new NameNotFoundException(
                getID() + " does not have a syntax associated with it");
        }

        String syntaxName = (String)syntaxAttr.get();

        // look in the schema tree for the syntax definition
        return (DirContext)schema.lookup(
            LdapSchemaParser.SYNTAX_DEFINITION_NAME + "/" + syntaxName);
    }

    /**
      * Retrieves this attribute's schema definition.
      *
      * @return This attribute's schema definition.
      */
    public DirContext getAttributeDefinition() throws NamingException {
        DirContext schema = getBaseCtx().getSchema(rdn);

        return (DirContext)schema.lookup(
            LdapSchemaParser.ATTRIBUTE_DEFINITION_NAME + "/" + getID());
    }
}
