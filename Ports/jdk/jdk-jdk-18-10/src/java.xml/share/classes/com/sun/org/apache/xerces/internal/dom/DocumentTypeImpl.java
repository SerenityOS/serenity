/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.dom;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import org.w3c.dom.DOMException;
import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

/**
 * This class represents a Document Type <em>declaraction</em> in
 * the document itself, <em>not</em> a Document Type Definition (DTD).
 * An XML document may (or may not) have such a reference.
 * <P>
 * DocumentType is an Extended DOM feature, used in XML documents but
 * not in HTML.
 * <P>
 * Note that Entities and Notations are no longer children of the
 * DocumentType, but are parentless nodes hung only in their
 * appropriate NamedNodeMaps.
 * <P>
 * This area is UNDERSPECIFIED IN REC-DOM-Level-1-19981001
 * Most notably, absolutely no provision was made for storing
 * and using Element and Attribute information. Nor was the linkage
 * between Entities and Entity References nailed down solidly.
 *
 * @xerces.internal
 *
 * @author Arnaud  Le Hors, IBM
 * @author Joe Kesselman, IBM
 * @author Andy Clark, IBM
 * @since  PR-DOM-Level-1-19980818.
 * @LastModified: Apr 2019
 */
public class DocumentTypeImpl
    extends ParentNode
    implements DocumentType {

    //
    // Constants
    //

    /** Serialization version. */
    static final long serialVersionUID = 7751299192316526485L;

    //
    // Data
    //

    /** Document type name. */
    protected String name;

    /** Entities. */
    protected NamedNodeMapImpl entities;

    /** Notations. */
    protected NamedNodeMapImpl notations;

    // NON-DOM

    /** Elements. */
    protected NamedNodeMapImpl elements;

    // DOM2: support public ID.
    protected String publicID;

    // DOM2: support system ID.
    protected String systemID;

    // DOM2: support internal subset.
    protected String internalSubset;

    /** The following are required for compareDocumentPosition
    */
    // Doctype number.   Doc types which have no owner may be assigned
    // a number, on demand, for ordering purposes for compareDocumentPosition
    private int doctypeNumber=0;

    private Map<String, UserDataRecord> userData =  null;


    /**
     * @serialField name String document type name
     * @serialField entities NamedNodeMapImpl entities
     * @serialField notations NamedNodeMapImpl notations
     * @serialField elements NamedNodeMapImpl elements
     * @serialField publicID String support public ID
     * @serialField systemID String support system ID
     * @serialField internalSubset String support internal subset
     * @serialField doctypeNumber int Doctype number
     * @serialField userData Hashtable user data
     */
    private static final ObjectStreamField[] serialPersistentFields =
        new ObjectStreamField[] {
            new ObjectStreamField("name", String.class),
            new ObjectStreamField("entities", NamedNodeMapImpl.class),
            new ObjectStreamField("notations", NamedNodeMapImpl.class),
            new ObjectStreamField("elements", NamedNodeMapImpl.class),
            new ObjectStreamField("publicID", String.class),
            new ObjectStreamField("systemID", String.class),
            new ObjectStreamField("internalSubset", String.class),
            new ObjectStreamField("doctypeNumber", int.class),
            new ObjectStreamField("userData", Hashtable.class),
        };

    //
    // Constructors
    //

    /** Factory method for creating a document type node. */
    public DocumentTypeImpl(CoreDocumentImpl ownerDocument, String name) {
        super(ownerDocument);

        this.name = name;
        // DOM
        entities  = new NamedNodeMapImpl(this);
        notations = new NamedNodeMapImpl(this);

        // NON-DOM
        elements = new NamedNodeMapImpl(this);

    } // <init>(CoreDocumentImpl,String)

    /** Factory method for creating a document type node. */
    public DocumentTypeImpl(CoreDocumentImpl ownerDocument,
                            String qualifiedName,
                            String publicID, String systemID) {
        this(ownerDocument, qualifiedName);
        this.publicID = publicID;
        this.systemID = systemID;

    } // <init>(CoreDocumentImpl,String)

    //
    // DOM2: methods.
    //

    /**
     * Introduced in DOM Level 2. <p>
     *
     * Return the public identifier of this Document type.
     * @since WD-DOM-Level-2-19990923
     */
    public String getPublicId() {
        if (needsSyncData()) {
            synchronizeData();
        }
        return publicID;
    }
    /**
     * Introduced in DOM Level 2. <p>
     *
     * Return the system identifier of this Document type.
     * @since WD-DOM-Level-2-19990923
     */
    public String getSystemId() {
        if (needsSyncData()) {
            synchronizeData();
        }
        return systemID;
    }

    /**
     * NON-DOM. <p>
     *
     * Set the internalSubset given as a string.
     */
    public void setInternalSubset(String internalSubset) {
        if (needsSyncData()) {
            synchronizeData();
        }
        this.internalSubset = internalSubset;
    }

    /**
     * Introduced in DOM Level 2. <p>
     *
     * Return the internalSubset given as a string.
     * @since WD-DOM-Level-2-19990923
     */
    public String getInternalSubset() {
        if (needsSyncData()) {
            synchronizeData();
        }
        return internalSubset;
    }

    //
    // Node methods
    //

    /**
     * A short integer indicating what type of node this is. The named
     * constants for this value are defined in the org.w3c.dom.Node interface.
     */
    public short getNodeType() {
        return Node.DOCUMENT_TYPE_NODE;
    }

    /**
     * Returns the document type name
     */
    public String getNodeName() {
        if (needsSyncData()) {
            synchronizeData();
        }
        return name;
    }

    /** Clones the node. */
    public Node cloneNode(boolean deep) {

        DocumentTypeImpl newnode = (DocumentTypeImpl)super.cloneNode(deep);
        // NamedNodeMaps must be cloned explicitly, to avoid sharing them.
        newnode.entities  = entities.cloneMap(newnode);
        newnode.notations = notations.cloneMap(newnode);
        newnode.elements  = elements.cloneMap(newnode);

        return newnode;

    } // cloneNode(boolean):Node

    /*
     * Get Node text content
     * @since DOM Level 3
     */
    public String getTextContent() throws DOMException {
        return null;
    }

    /*
     * Set Node text content
     * @since DOM Level 3
     */
    public void setTextContent(String textContent)
        throws DOMException {
        // no-op
    }

        /**
          * DOM Level 3 WD- Experimental.
          * Override inherited behavior from ParentNodeImpl to support deep equal.
          */
    public boolean isEqualNode(Node arg) {

        if (!super.isEqualNode(arg)) {
            return false;
        }

        if (needsSyncData()) {
            synchronizeData();
        }
        DocumentTypeImpl argDocType = (DocumentTypeImpl) arg;

        //test if the following string attributes are equal: publicId,
        //systemId, internalSubset.
        if ((getPublicId() == null && argDocType.getPublicId() != null)
            || (getPublicId() != null && argDocType.getPublicId() == null)
            || (getSystemId() == null && argDocType.getSystemId() != null)
            || (getSystemId() != null && argDocType.getSystemId() == null)
            || (getInternalSubset() == null
                && argDocType.getInternalSubset() != null)
            || (getInternalSubset() != null
                && argDocType.getInternalSubset() == null)) {
            return false;
        }

        if (getPublicId() != null) {
            if (!getPublicId().equals(argDocType.getPublicId())) {
                return false;
            }
        }

        if (getSystemId() != null) {
            if (!getSystemId().equals(argDocType.getSystemId())) {
                return false;
            }
        }

        if (getInternalSubset() != null) {
            if (!getInternalSubset().equals(argDocType.getInternalSubset())) {
                return false;
            }
        }

        //test if NamedNodeMaps entities and notations are equal
        NamedNodeMapImpl argEntities = argDocType.entities;

        if ((entities == null && argEntities != null)
            || (entities != null && argEntities == null))
            return false;

        if (entities != null && argEntities != null) {
            if (entities.getLength() != argEntities.getLength())
                return false;

            for (int index = 0; entities.item(index) != null; index++) {
                Node entNode1 = entities.item(index);
                Node entNode2 =
                    argEntities.getNamedItem(entNode1.getNodeName());

                if (!((NodeImpl) entNode1).isEqualNode(entNode2))
                    return false;
            }
        }

        NamedNodeMapImpl argNotations = argDocType.notations;

        if ((notations == null && argNotations != null)
            || (notations != null && argNotations == null))
            return false;

        if (notations != null && argNotations != null) {
            if (notations.getLength() != argNotations.getLength())
                return false;

            for (int index = 0; notations.item(index) != null; index++) {
                Node noteNode1 = notations.item(index);
                Node noteNode2 =
                    argNotations.getNamedItem(noteNode1.getNodeName());

                if (!((NodeImpl) noteNode1).isEqualNode(noteNode2))
                    return false;
            }
        }

        return true;
    } //end isEqualNode


    /**
     * NON-DOM
     * set the ownerDocument of this node and its children
     */
    protected void setOwnerDocument(CoreDocumentImpl doc) {
        super.setOwnerDocument(doc);
        entities.setOwnerDocument(doc);
        notations.setOwnerDocument(doc);
        elements.setOwnerDocument(doc);
    }

    /** NON-DOM
        Get the number associated with this doctype.
    */
    protected int getNodeNumber() {
         // If the doctype has a document owner, get the node number
         // relative to the owner doc
         if (getOwnerDocument()!=null)
            return super.getNodeNumber();

         // The doctype is disconnected and not associated with any document.
         // Assign the doctype a number relative to the implementation.
         if (doctypeNumber==0) {

            CoreDOMImplementationImpl cd = (CoreDOMImplementationImpl)CoreDOMImplementationImpl.getDOMImplementation();
            doctypeNumber = cd.assignDocTypeNumber();
         }
         return doctypeNumber;
    }

    //
    // DocumentType methods
    //

    /**
     * Name of this document type. If we loaded from a DTD, this should
     * be the name immediately following the DOCTYPE keyword.
     */
    public String getName() {

        if (needsSyncData()) {
            synchronizeData();
        }
        return name;

    } // getName():String

    /**
     * Access the collection of general Entities, both external and
     * internal, defined in the DTD. For example, in:
     * <p>
     * <pre>
     *   &lt;!doctype example SYSTEM "ex.dtd" [
     *     &lt;!ENTITY foo "foo"&gt;
     *     &lt;!ENTITY bar "bar"&gt;
     *     &lt;!ENTITY % baz "baz"&gt;
     *     ]&gt;
     * </pre>
     * <p>
     * The Entities map includes foo and bar, but not baz. It is promised that
     * only Nodes which are Entities will exist in this NamedNodeMap.
     * <p>
     * For HTML, this will always be null.
     * <p>
     * Note that "built in" entities such as &amp; and &lt; should be
     * converted to their actual characters before being placed in the DOM's
     * contained text, and should be converted back when the DOM is rendered
     * as XML or HTML, and hence DO NOT appear here.
     */
    public NamedNodeMap getEntities() {
        if (needsSyncChildren()) {
            synchronizeChildren();
            }
        return entities;
    }

    /**
     * Access the collection of Notations defined in the DTD.  A
     * notation declares, by name, the format of an XML unparsed entity
     * or is used to formally declare a Processing Instruction target.
     */
    public NamedNodeMap getNotations() {
        if (needsSyncChildren()) {
            synchronizeChildren();
            }
        return notations;
    }

    //
    // Public methods
    //

    /**
     * NON-DOM: Subclassed to flip the entities' and notations' readonly switch
     * as well.
     * @see NodeImpl#setReadOnly
     */
    public void setReadOnly(boolean readOnly, boolean deep) {

        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        super.setReadOnly(readOnly, deep);

        // set read-only property
        elements.setReadOnly(readOnly, true);
        entities.setReadOnly(readOnly, true);
        notations.setReadOnly(readOnly, true);

    } // setReadOnly(boolean,boolean)

    /**
     * NON-DOM: Access the collection of ElementDefinitions.
     * @see ElementDefinitionImpl
     */
    public NamedNodeMap getElements() {
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        return elements;
    }

    public Object setUserData(String key,
    Object data, UserDataHandler handler) {
        if(userData == null)
            userData = new HashMap<>();
        if (data == null) {
            if (userData != null) {
                UserDataRecord udr = userData.remove(key);
                if (udr != null) {
                    return udr.fData;
                }
            }
            return null;
        }
        else {
            UserDataRecord udr = userData.put(key, new UserDataRecord(data, handler));
            if (udr != null) {
                return udr.fData;
            }
        }
        return null;
    }

    public Object getUserData(String key) {
        if (userData == null) {
            return null;
        }
        UserDataRecord udr = userData.get(key);
        if (udr != null) {
            return udr.fData;
        }
        return null;
    }

    @Override
    protected Map<String, UserDataRecord> getUserDataRecord(){
        return userData;
    }

    /**
     * @serialData Serialized fields. Convert Map to Hashtable for backward
     * compatibility.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Convert the HashMap to Hashtable
        Hashtable<String, UserDataRecord> ud = (userData == null)? null : new Hashtable<>(userData);

        // Write serialized fields
        ObjectOutputStream.PutField pf = out.putFields();
        pf.put("name", name);
        pf.put("entities", entities);
        pf.put("notations", notations);
        pf.put("elements", elements);
        pf.put("publicID", publicID);
        pf.put("systemID", systemID);
        pf.put("internalSubset", internalSubset);
        pf.put("doctypeNumber", doctypeNumber);
        pf.put("userData", ud);
        out.writeFields();
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in)
                        throws IOException, ClassNotFoundException {
        // We have to read serialized fields first.
        ObjectInputStream.GetField gf = in.readFields();
        name = (String)gf.get("name", null);
        entities = (NamedNodeMapImpl)gf.get("entities", null);
        notations = (NamedNodeMapImpl)gf.get("notations", null);
        elements = (NamedNodeMapImpl)gf.get("elements", null);
        publicID = (String)gf.get("publicID", null);
        systemID = (String)gf.get("systemID", null);
        internalSubset = (String)gf.get("internalSubset", null);
        doctypeNumber = gf.get("doctypeNumber", 0);

        Hashtable<String, UserDataRecord> ud =
                (Hashtable<String, UserDataRecord>)gf.get("userData", null);

        //convert the Hashtable back to HashMap
        if (ud != null) userData = new HashMap<>(ud);
    }
} // class DocumentTypeImpl
