/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.util.URI;
import com.sun.org.apache.xerces.internal.util.XML11Char;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xerces.internal.utils.ObjectFactory;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import jdk.xml.internal.SecuritySupport;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Notation;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

/**
 * The Document interface represents the entire HTML or XML document.
 * Conceptually, it is the root of the document tree, and provides the
 * primary access to the document's data.
 * <P>
 * Since elements, text nodes, comments, processing instructions,
 * etc. cannot exist outside the context of a Document, the Document
 * interface also contains the factory methods needed to create these
 * objects. The Node objects created have a ownerDocument attribute
 * which associates them with the Document within whose context they
 * were created.
 * <p>
 * The CoreDocumentImpl class only implements the DOM Core. Additional modules
 * are supported by the more complete DocumentImpl subclass.
 * <p>
 * <b>Note:</b> When any node in the document is serialized, the
 * entire document is serialized along with it.
 *
 * @xerces.internal
 *
 * @author Arnaud  Le Hors, IBM
 * @author Joe Kesselman, IBM
 * @author Andy Clark, IBM
 * @author Ralf Pfeiffer, IBM
 * @since  PR-DOM-Level-1-19980818.
 * @LastModified: Sept 2019
 */
public class CoreDocumentImpl
        extends ParentNode implements Document {

    /**
     * TODO:: 1. Change XML11Char method names similar to XMLChar. That will
     * prevent lot of dirty version checking code.
     *
     * 2. IMO during cloneNode qname/isXMLName check should not be made.
     */
    //
    // Constants
    //

    /** Serialization version. */
    static final long serialVersionUID = 0;

    //
    // Data
    //

    // document information

    /** Document type. */
    protected DocumentTypeImpl docType;

    /** Document element. */
    protected ElementImpl docElement;

    /** NodeListCache free list */
    transient NodeListCache fFreeNLCache;

    /**Experimental DOM Level 3 feature: Document encoding */
    protected String encoding;

    /**Experimental DOM Level 3 feature: Document actualEncoding */
    protected String actualEncoding;

    /**Experimental DOM Level 3 feature: Document version */
    protected String version;

    /**Experimental DOM Level 3 feature: Document standalone */
    protected boolean standalone;

    /**Experimental DOM Level 3 feature: documentURI */
    protected String fDocumentURI;

    //Revisit :: change to a better data structure.
    /** Table for user data attached to this document nodes. */
    private Map<Node, Map<String, UserDataRecord>> nodeUserData;

    /** Identifiers. */
    protected Map<String, Node> identifiers;

    // DOM Level 3: normalizeDocument
    transient DOMNormalizer domNormalizer = null;
    transient DOMConfigurationImpl fConfiguration = null;

    // support of XPath API
    transient Object fXPathEvaluator = null;

    /** Table for quick check of child insertion. */
    private final static int[] kidOK;

    /**
     * Number of alterations made to this document since its creation.
     * Serves as a "dirty bit" so that live objects such as NodeList can
     * recognize when an alteration has been made and discard its cached
     * state information.
     * <p>
     * Any method that alters the tree structure MUST cause or be
     * accompanied by a call to changed(), to inform it that any outstanding
     * NodeLists may have to be updated.
     * <p>
     * (Required because NodeList is simultaneously "live" and integer-
     * indexed -- a bad decision in the DOM's design.)
     * <p>
     * Note that changes which do not affect the tree's structure -- changing
     * the node's name, for example -- do _not_ have to call changed().
     * <p>
     * Alternative implementation would be to use a cryptographic
     * Digest value rather than a count. This would have the advantage that
     * "harmless" changes (those producing equal() trees) would not force
     * NodeList to resynchronize. Disadvantage is that it's slightly more prone
     * to "false negatives", though that's the difference between "wildly
     * unlikely" and "absurdly unlikely". IF we start maintaining digests,
     * we should consider taking advantage of them.
     *
     * Note: This used to be done a node basis, so that we knew what
     * subtree changed. But since only DeepNodeList really use this today,
     * the gain appears to be really small compared to the cost of having
     * an int on every (parent) node plus having to walk up the tree all the
     * way to the root to mark the branch as changed everytime a node is
     * changed.
     * So we now have a single counter global to the document. It means that
     * some objects may flush their cache more often than necessary, but this
     * makes nodes smaller and only the document needs to be marked as changed.
     */
    protected int changes = 0;

    // experimental

    /** Allow grammar access. */
    protected boolean allowGrammarAccess;

    /** Bypass error checking. */
    protected boolean errorChecking = true;
    /** Ancestor checking */
    protected boolean ancestorChecking = true;

    //Did version change at any point when the document was created ?
    //this field helps us to optimize when normalizingDocument.
    protected boolean xmlVersionChanged = false ;

    /** The following are required for compareDocumentPosition
     */
    // Document number.   Documents are ordered across the implementation using
    // positive integer values.  Documents are assigned numbers on demand.
    private int documentNumber=0;
    // Node counter and table.  Used to assign numbers to nodes for this
    // document.  Node number values are negative integers.  Nodes are
    // assigned numbers on demand.
    private int nodeCounter = 0;
    private Map<Node, Integer> nodeTable;
    private boolean xml11Version = false; //by default 1.0
    //
    // Static initialization
    //

    static {

        kidOK = new int[13];

        kidOK[DOCUMENT_NODE] =
        1 << ELEMENT_NODE | 1 << PROCESSING_INSTRUCTION_NODE |
        1 << COMMENT_NODE | 1 << DOCUMENT_TYPE_NODE;

        kidOK[DOCUMENT_FRAGMENT_NODE] =
        kidOK[ENTITY_NODE] =
        kidOK[ENTITY_REFERENCE_NODE] =
        kidOK[ELEMENT_NODE] =
        1 << ELEMENT_NODE | 1 << PROCESSING_INSTRUCTION_NODE |
        1 << COMMENT_NODE | 1 << TEXT_NODE |
        1 << CDATA_SECTION_NODE | 1 << ENTITY_REFERENCE_NODE ;


        kidOK[ATTRIBUTE_NODE] =
        1 << TEXT_NODE | 1 << ENTITY_REFERENCE_NODE;

        kidOK[DOCUMENT_TYPE_NODE] =
        kidOK[PROCESSING_INSTRUCTION_NODE] =
        kidOK[COMMENT_NODE] =
        kidOK[TEXT_NODE] =
        kidOK[CDATA_SECTION_NODE] =
        kidOK[NOTATION_NODE] =
        0;

    } // static

    /**
     * @serialField docType DocumentTypeImpl document type
     * @serialField docElement ElementImpl document element
     * @serialField fFreeNLCache NodeListCache NodeListCache free list
     * @serialField encoding String Document encoding
     * @serialField actualEncoding String Document actualEncoding
     * @serialField version String Document version
     * @serialField standalone boolean Document standalone
     * @serialField fDocumentURI String Document URI
     * @serialField userData Hashtable user data attached to the nodes. Note that
     * it was original called "userData". It has been changed to nodeUserData to
     * avoid confusion with those that are actually values of the map.
     * @serialField identifiers Hashtable identifiers
     * @serialField changes int flag indicates whether the node has changed
     * @serialField allowGrammarAccess boolean Allow grammar access
     * @serialField errorChecking boolean Bypass error checking
     * @serialField ancestorChecking boolean Ancestor checking
     * @serialField xmlVersionChanged boolean Indicate whether the version has changed
     * @serialField documentNumber int Document number
     * @serialField nodeCounter int Node counter
     * @serialField nodeTable Hashtable Node table
     * @serialField xml11Version boolean XML version
     */
    private static final ObjectStreamField[] serialPersistentFields =
        new ObjectStreamField[] {
            new ObjectStreamField("docType", DocumentTypeImpl.class),
            new ObjectStreamField("docElement", ElementImpl.class),
            new ObjectStreamField("fFreeNLCache", NodeListCache.class),
            new ObjectStreamField("encoding", String.class),
            new ObjectStreamField("actualEncoding", String.class),
            new ObjectStreamField("version", String.class),
            new ObjectStreamField("standalone", boolean.class),
            new ObjectStreamField("fDocumentURI", String.class),
            new ObjectStreamField("userData", Hashtable.class),
            new ObjectStreamField("identifiers", Hashtable.class),
            new ObjectStreamField("changes", int.class),
            new ObjectStreamField("allowGrammarAccess", boolean.class),
            new ObjectStreamField("errorChecking", boolean.class),
            new ObjectStreamField("ancestorChecking", boolean.class),
            new ObjectStreamField("xmlVersionChanged", boolean.class),
            new ObjectStreamField("documentNumber", int.class),
            new ObjectStreamField("nodeCounter", int.class),
            new ObjectStreamField("nodeTable", Hashtable.class),
            new ObjectStreamField("xml11Version", boolean.class),
        };

    //
    // Constructors
    //

    /**
     * NON-DOM: Actually creating a Document is outside the DOM's spec,
     * since it has to operate in terms of a particular implementation.
     */
    public CoreDocumentImpl() {
        this(false);
    }

    /** Constructor. */
    public CoreDocumentImpl(boolean grammarAccess) {
        super(null);
        ownerDocument = this;
        allowGrammarAccess = grammarAccess;
        String systemProp = SecuritySupport.getSystemProperty(Constants.SUN_DOM_PROPERTY_PREFIX+Constants.SUN_DOM_ANCESTOR_CHECCK);
        if (systemProp != null) {
            if (systemProp.equalsIgnoreCase("false")) {
                ancestorChecking = false;
            }
        }
    }

    /**
     * For DOM2 support.
     * The createDocument factory method is in DOMImplementation.
     */
    public CoreDocumentImpl(DocumentType doctype) {
        this(doctype, false);
    }

    /** For DOM2 support. */
    public CoreDocumentImpl(DocumentType doctype, boolean grammarAccess) {
        this(grammarAccess);
        if (doctype != null) {
            DocumentTypeImpl doctypeImpl;
            try {
                doctypeImpl = (DocumentTypeImpl) doctype;
            } catch (ClassCastException e) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null);
                throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, msg);
            }
            doctypeImpl.ownerDocument = this;
            appendChild(doctype);
        }
    }

    //
    // Node methods
    //

    // even though ownerDocument refers to this in this implementation
    // the DOM Level 2 spec says it must be null, so make it appear so
    final public Document getOwnerDocument() {
        return null;
    }

    /** Returns the node type. */
    public short getNodeType() {
        return Node.DOCUMENT_NODE;
    }

    /** Returns the node name. */
    public String getNodeName() {
        return "#document";
    }

    /**
     * Deep-clone a document, including fixing ownerDoc for the cloned
     * children. Note that this requires bypassing the WRONG_DOCUMENT_ERR
     * protection. I've chosen to implement it by calling importNode
     * which is DOM Level 2.
     *
     * @return org.w3c.dom.Node
     * @param deep boolean, iff true replicate children
     */
    public Node cloneNode(boolean deep) {

        CoreDocumentImpl newdoc = new CoreDocumentImpl();
        callUserDataHandlers(this, newdoc, UserDataHandler.NODE_CLONED);
        cloneNode(newdoc, deep);

        return newdoc;

    } // cloneNode(boolean):Node


    /**
     * internal method to share code with subclass
     **/
    protected void cloneNode(CoreDocumentImpl newdoc, boolean deep) {

        // clone the children by importing them
        if (needsSyncChildren()) {
            synchronizeChildren();
        }

        if (deep) {
            Map<Node, String> reversedIdentifiers = null;

            if (identifiers != null) {
                // Build a reverse mapping from element to identifier.
                reversedIdentifiers = new HashMap<>(identifiers.size());
                for (String elementId : identifiers.keySet()) {
                    reversedIdentifiers.put(identifiers.get(elementId), elementId);
                }
            }

            // Copy children into new document.
            for (ChildNode kid = firstChild; kid != null;
                    kid = kid.nextSibling) {
                newdoc.appendChild(newdoc.importNode(kid, true, true,
                        reversedIdentifiers));
            }
        }

        // experimental
        newdoc.allowGrammarAccess = allowGrammarAccess;
        newdoc.errorChecking = errorChecking;

    } // cloneNode(CoreDocumentImpl,boolean):void

    /**
     * Since a Document may contain at most one top-level Element child,
     * and at most one DocumentType declaraction, we need to subclass our
     * add-children methods to implement this constraint.
     * Since appendChild() is implemented as insertBefore(,null),
     * altering the latter fixes both.
     * <p>
     * While I'm doing so, I've taken advantage of the opportunity to
     * cache documentElement and docType so we don't have to
     * search for them.
     *
     * REVISIT: According to the spec it is not allowed to alter neither the
     * document element nor the document type in any way
     */
    public Node insertBefore(Node newChild, Node refChild)
            throws DOMException {

        // Only one such child permitted
        int type = newChild.getNodeType();
        if (errorChecking) {
            if((type == Node.ELEMENT_NODE && docElement != null) ||
            (type == Node.DOCUMENT_TYPE_NODE && docType != null)) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "HIERARCHY_REQUEST_ERR", null);
                throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, msg);
            }
        }
        // Adopt orphan doctypes
        if (newChild.getOwnerDocument() == null &&
        newChild instanceof DocumentTypeImpl) {
            ((DocumentTypeImpl) newChild).ownerDocument = this;
        }
        super.insertBefore(newChild,refChild);

        // If insert succeeded, cache the kid appropriately
        if (type == Node.ELEMENT_NODE) {
            docElement = (ElementImpl)newChild;
        }
        else if (type == Node.DOCUMENT_TYPE_NODE) {
            docType = (DocumentTypeImpl)newChild;
        }

        return newChild;

    } // insertBefore(Node,Node):Node

    /**
     * Since insertBefore caches the docElement (and, currently, docType),
     * removeChild has to know how to undo the cache
     *
     * REVISIT: According to the spec it is not allowed to alter neither the
     * document element nor the document type in any way
     */
    public Node removeChild(Node oldChild) throws DOMException {

        super.removeChild(oldChild);

        // If remove succeeded, un-cache the kid appropriately
        int type = oldChild.getNodeType();
        if(type == Node.ELEMENT_NODE) {
            docElement = null;
        }
        else if (type == Node.DOCUMENT_TYPE_NODE) {
            docType = null;
        }

        return oldChild;

    }   // removeChild(Node):Node

    /**
     * Since we cache the docElement (and, currently, docType),
     * replaceChild has to update the cache
     *
     * REVISIT: According to the spec it is not allowed to alter neither the
     * document element nor the document type in any way
     */
    public Node replaceChild(Node newChild, Node oldChild)
            throws DOMException {

        // Adopt orphan doctypes
        if (newChild.getOwnerDocument() == null &&
        newChild instanceof DocumentTypeImpl) {
            ((DocumentTypeImpl) newChild).ownerDocument = this;
        }

        if (errorChecking &&((docType != null &&
            oldChild.getNodeType() != Node.DOCUMENT_TYPE_NODE &&
            newChild.getNodeType() == Node.DOCUMENT_TYPE_NODE)
            || (docElement != null &&
            oldChild.getNodeType() != Node.ELEMENT_NODE &&
            newChild.getNodeType() == Node.ELEMENT_NODE))) {

            throw new DOMException(
                    DOMException.HIERARCHY_REQUEST_ERR,
                    DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "HIERARCHY_REQUEST_ERR", null));
        }
        super.replaceChild(newChild, oldChild);

        int type = oldChild.getNodeType();
        if(type == Node.ELEMENT_NODE) {
            docElement = (ElementImpl)newChild;
        }
        else if (type == Node.DOCUMENT_TYPE_NODE) {
            docType = (DocumentTypeImpl)newChild;
        }
        return oldChild;
    }   // replaceChild(Node,Node):Node

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
     * @since DOM Level 3
     */
    public Object getFeature(String feature, String version) {
        return super.getFeature(feature, version);
    }

    //
    // Document methods
    //

    // factory methods

    /**
     * Factory method; creates an Attribute having this Document as its
     * OwnerDoc.
     *
     * @param name The name of the attribute. Note that the attribute's value is
     * _not_ established at the factory; remember to set it!
     *
     * @throws DOMException(INVALID_NAME_ERR)
     * if the attribute name is not acceptable.
     */
    public Attr createAttribute(String name)
            throws DOMException {

        if (errorChecking && !isXMLName(name,xml11Version)) {
            String msg =
                DOMMessageFormatter.formatMessage(
                            DOMMessageFormatter.DOM_DOMAIN,
                            "INVALID_CHARACTER_ERR",
                            null);
            throw new DOMException(DOMException.INVALID_CHARACTER_ERR, msg);
        }
        return new AttrImpl(this, name);

    } // createAttribute(String):Attr

    /**
     * Factory method; creates a CDATASection having this Document as
     * its OwnerDoc.
     *
     * @param data The initial contents of the CDATA
     *
     * @throws DOMException(NOT_SUPPORTED_ERR) for HTML documents. (HTML
     * not yet implemented.)
     */
    public CDATASection createCDATASection(String data)
            throws DOMException {
        return new CDATASectionImpl(this, data);
    }

    /**
     * Factory method; creates a Comment having this Document as its
     * OwnerDoc.
     *
     * @param data The initial contents of the Comment. */
    public Comment createComment(String data) {
        return new CommentImpl(this, data);
    }

    /**
     * Factory method; creates a DocumentFragment having this Document
     * as its OwnerDoc.
     */
    public DocumentFragment createDocumentFragment() {
        return new DocumentFragmentImpl(this);
    }

    /**
     * Factory method; creates an Element having this Document
     * as its OwnerDoc.
     *
     * @param tagName The name of the element type to instantiate. For
     * XML, this is case-sensitive. For HTML, the tagName parameter may
     * be provided in any case, but it must be mapped to the canonical
     * uppercase form by the DOM implementation.
     *
     * @throws DOMException(INVALID_NAME_ERR) if the tag name is not
     * acceptable.
     */
    public Element createElement(String tagName)
            throws DOMException {

        if (errorChecking && !isXMLName(tagName,xml11Version)) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
            throw new DOMException(DOMException.INVALID_CHARACTER_ERR, msg);
        }
        return new ElementImpl(this, tagName);

    } // createElement(String):Element

    /**
     * Factory method; creates an EntityReference having this Document
     * as its OwnerDoc.
     *
     * @param name The name of the Entity we wish to refer to
     *
     * @throws DOMException(NOT_SUPPORTED_ERR) for HTML documents, where
     * nonstandard entities are not permitted. (HTML not yet
     * implemented.)
     */
    public EntityReference createEntityReference(String name)
            throws DOMException {

        if (errorChecking && !isXMLName(name,xml11Version)) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
            throw new DOMException(DOMException.INVALID_CHARACTER_ERR, msg);
        }
        return new EntityReferenceImpl(this, name);

    } // createEntityReference(String):EntityReference

    /**
     * Factory method; creates a ProcessingInstruction having this Document
     * as its OwnerDoc.
     *
     * @param target The target "processor channel"
     * @param data Parameter string to be passed to the target.
     *
     * @throws DOMException(INVALID_NAME_ERR) if the target name is not
     * acceptable.
     *
     * @throws DOMException(NOT_SUPPORTED_ERR) for HTML documents. (HTML
     * not yet implemented.)
     */
    public ProcessingInstruction createProcessingInstruction(String target,
            String data)
            throws DOMException {

        if (errorChecking && !isXMLName(target,xml11Version)) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
            throw new DOMException(DOMException.INVALID_CHARACTER_ERR, msg);
        }
        return new ProcessingInstructionImpl(this, target, data);

    } // createProcessingInstruction(String,String):ProcessingInstruction

    /**
     * Factory method; creates a Text node having this Document as its
     * OwnerDoc.
     *
     * @param data The initial contents of the Text.
     */
    public Text createTextNode(String data) {
        return new TextImpl(this, data);
    }

    // other document methods

    /**
     * For XML, this provides access to the Document Type Definition.
     * For HTML documents, and XML documents which don't specify a DTD,
     * it will be null.
     */
    public DocumentType getDoctype() {
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        return docType;
    }

    /**
     * Convenience method, allowing direct access to the child node
     * which is considered the root of the actual document content. For
     * HTML, where it is legal to have more than one Element at the top
     * level of the document, we pick the one with the tagName
     * "HTML". For XML there should be only one top-level
     *
     * (HTML not yet supported.)
     */
    public Element getDocumentElement() {
        if (needsSyncChildren()) {
            synchronizeChildren();
        }
        return docElement;
    }

    /**
     * Return a <em>live</em> collection of all descendent Elements (not just
     * immediate children) having the specified tag name.
     *
     * @param tagname The type of Element we want to gather. "*" will be
     * taken as a wildcard, meaning "all elements in the document."
     *
     * @see DeepNodeListImpl
     */
    public NodeList getElementsByTagName(String tagname) {
        return new DeepNodeListImpl(this,tagname);
    }

    /**
     * Retrieve information describing the abilities of this particular
     * DOM implementation. Intended to support applications that may be
     * using DOMs retrieved from several different sources, potentially
     * with different underlying representations.
     */
    public DOMImplementation getImplementation() {
        // Currently implemented as a singleton, since it's hardcoded
        // information anyway.
        return CoreDOMImplementationImpl.getDOMImplementation();
    }

    //
    // Public methods
    //

    // properties

    /**
     * Sets whether the DOM implementation performs error checking
     * upon operations. Turning off error checking only affects
     * the following DOM checks:
     * <ul>
     * <li>Checking strings to make sure that all characters are
     *     legal XML characters
     * <li>Hierarchy checking such as allowed children, checks for
     *     cycles, etc.
     * </ul>
     * <p>
     * Turning off error checking does <em>not</em> turn off the
     * following checks:
     * <ul>
     * <li>Read only checks
     * <li>Checks related to DOM events
     * </ul>
     */

    public void setErrorChecking(boolean check) {
        errorChecking = check;
    }

    /*
     * DOM Level 3 WD - Experimental.
     */
    public void setStrictErrorChecking(boolean check) {
        errorChecking = check;
    }

    /**
     * Returns true if the DOM implementation performs error checking.
     */
    public boolean getErrorChecking() {
        return errorChecking;
    }

    /*
     * DOM Level 3 WD - Experimental.
     */
    public boolean getStrictErrorChecking() {
        return errorChecking;
    }

    /**
     * DOM Level 3 CR - Experimental. (Was getActualEncoding)
     *
     * An attribute specifying the encoding used for this document
     * at the time of the parsing. This is <code>null</code> when
     * it is not known, such as when the <code>Document</code> was
     * created in memory.
     * @since DOM Level 3
     */
    public String getInputEncoding() {
        return actualEncoding;
    }

    /**
     * DOM Internal
     * (Was a DOM L3 Core WD public interface method setActualEncoding )
     *
     * An attribute specifying the actual encoding of this document. This is
     * <code>null</code> otherwise.
     * <br> This attribute represents the property [character encoding scheme]
     * defined in .
     */
    public void setInputEncoding(String value) {
        actualEncoding = value;
    }

    /**
     * DOM Internal
     * (Was a DOM L3 Core WD public interface method setXMLEncoding )
     *
     * An attribute specifying, as part of the XML declaration,
     * the encoding of this document. This is null when unspecified.
     */
    public void setXmlEncoding(String value) {
        encoding = value;
    }

    /**
     * @deprecated This method is internal and only exists for
     * compatibility with older applications. New applications
     * should never call this method.
     */
    @Deprecated
    public void setEncoding(String value) {
        setXmlEncoding(value);
    }

    /**
     * DOM Level 3 WD - Experimental.
     * The encoding of this document (part of XML Declaration)
     */
    public String getXmlEncoding() {
        return encoding;
    }

    /**
     * @deprecated This method is internal and only exists for
     * compatibility with older applications. New applications
     * should never call this method.
     */
    @Deprecated
    public String getEncoding() {
        return getXmlEncoding();
    }

    /**
     * DOM Level 3 CR - Experimental.
     * version - An attribute specifying, as part of the XML declaration,
     * the version number of this document.
     */
    public void setXmlVersion(String value) {
        if (value == null) {
            return;
        }
        if(value.equals("1.0") || value.equals("1.1")){
            //we need to change the flag value only --
            // when the version set is different than already set.
            if(!getXmlVersion().equals(value)){
                xmlVersionChanged = true ;
                //change the normalization value back to false
                isNormalized(false);
                version = value;
            }
        }
        else{
            //NOT_SUPPORTED_ERR: Raised if the vesion is set to a value that is not supported by
            //this document
            //we dont support any other XML version
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
            throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);

        }
        if((getXmlVersion()).equals("1.1")){
            xml11Version = true;
        }
        else{
            xml11Version = false;
        }
    }

    /**
     * @deprecated This method is internal and only exists for
     * compatibility with older applications. New applications
     * should never call this method.
     */
    @Deprecated
    public void setVersion(String value) {
        setXmlVersion(value);
    }

    /**
     * DOM Level 3 WD - Experimental.
     * The version of this document (part of XML Declaration)
     */

    public String getXmlVersion() {
        return (version == null)?"1.0":version;
    }

    /**
     * @deprecated This method is internal and only exists for
     * compatibility with older applications. New applications
     * should never call this method.
     */
    @Deprecated
    public String getVersion() {
        return getXmlVersion();
    }

    /**
     * DOM Level 3 CR - Experimental.
     *
     * Xmlstandalone - An attribute specifying, as part of the XML declaration,
     * whether this document is standalone
     * @exception DOMException
     *    NOT_SUPPORTED_ERR: Raised if this document does not support the
     *   "XML" feature.
     * @since DOM Level 3
     */
    public void setXmlStandalone(boolean value)
            throws DOMException {
        standalone = value;
    }

    /**
     * @deprecated This method is internal and only exists for
     * compatibility with older applications. New applications
     * should never call this method.
     */
    @Deprecated
    public void setStandalone(boolean value) {
        setXmlStandalone(value);
    }

    /**
     * DOM Level 3 WD - Experimental.
     * standalone that specifies whether this document is standalone
     * (part of XML Declaration)
     */
    public boolean getXmlStandalone() {
        return standalone;
    }

    /**
     * @deprecated This method is internal and only exists for
     * compatibility with older applications. New applications
     * should never call this method.
     */
    @Deprecated
    public boolean getStandalone() {
        return getXmlStandalone();
    }

    /**
     * DOM Level 3 WD - Experimental.
     * The location of the document or <code>null</code> if undefined.
     * <br>Beware that when the <code>Document</code> supports the feature
     * "HTML" , the href attribute of the HTML BASE element takes precedence
     * over this attribute.
     * @since DOM Level 3
     */
    public String getDocumentURI(){
        return fDocumentURI;
    }


    /**
     * DOM Level 3 WD - Experimental.
     * Renaming node
     */
    public Node renameNode(Node n,String namespaceURI,String name)
    throws DOMException{

        if (errorChecking && n.getOwnerDocument() != this && n != this) {
            String msg = DOMMessageFormatter.formatMessage(
                    DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null);
            throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, msg);
        }
        switch (n.getNodeType()) {
            case ELEMENT_NODE: {
                ElementImpl el = (ElementImpl) n;
                if (el instanceof ElementNSImpl) {
                    ((ElementNSImpl) el).rename(namespaceURI, name);

                    // fire user data NODE_RENAMED event
                    callUserDataHandlers(el, null, UserDataHandler.NODE_RENAMED);
                }
                else {
                    if (namespaceURI == null) {
                        if (errorChecking) {
                            int colon1 = name.indexOf(':');
                            if(colon1 != -1){
                                String msg =
                                    DOMMessageFormatter.formatMessage(
                                                DOMMessageFormatter.DOM_DOMAIN,
                                                "NAMESPACE_ERR",
                                                null);
                                throw new DOMException(DOMException.NAMESPACE_ERR, msg);
                            }
                            if (!isXMLName(name,xml11Version)) {
                                String msg = DOMMessageFormatter.formatMessage(
                                        DOMMessageFormatter.DOM_DOMAIN,
                                        "INVALID_CHARACTER_ERR", null);
                                throw new DOMException(DOMException.INVALID_CHARACTER_ERR,
                                        msg);
                            }
                        }
                        el.rename(name);

                        // fire user data NODE_RENAMED event
                        callUserDataHandlers(el, null,
                                UserDataHandler.NODE_RENAMED);
                    }
                    else {
                        // we need to create a new object
                        ElementNSImpl nel =
                            new ElementNSImpl(this, namespaceURI, name);

                        // register event listeners on new node
                        copyEventListeners(el, nel);

                        // remove user data from old node
                        Map<String, UserDataRecord> data = removeUserDataTable(el);

                        // remove old node from parent if any
                        Node parent = el.getParentNode();
                        Node nextSib = el.getNextSibling();
                        if (parent != null) {
                            parent.removeChild(el);
                        }
                        // move children to new node
                        Node child = el.getFirstChild();
                        while (child != null) {
                            el.removeChild(child);
                            nel.appendChild(child);
                            child = el.getFirstChild();
                        }
                        // move specified attributes to new node
                        nel.moveSpecifiedAttributes(el);

                        // attach user data to new node
                        setUserDataTable(nel, data);

                        // and fire user data NODE_RENAMED event
                        callUserDataHandlers(el, nel,
                                UserDataHandler.NODE_RENAMED);

                        // insert new node where old one was
                        if (parent != null) {
                            parent.insertBefore(nel, nextSib);
                        }
                        el = nel;
                    }
                }
                // fire ElementNameChanged event
                renamedElement((Element) n, el);
                return el;
            }
            case ATTRIBUTE_NODE: {
                AttrImpl at = (AttrImpl) n;

                // dettach attr from element
                Element el = at.getOwnerElement();
                if (el != null) {
                    el.removeAttributeNode(at);
                }
                if (n instanceof AttrNSImpl) {
                    ((AttrNSImpl) at).rename(namespaceURI, name);
                    // reattach attr to element
                    if (el != null) {
                        el.setAttributeNodeNS(at);
                    }

                    // fire user data NODE_RENAMED event
                    callUserDataHandlers(at, null, UserDataHandler.NODE_RENAMED);
                }
                else {
                    if (namespaceURI == null) {
                        at.rename(name);
                        // reattach attr to element
                        if (el != null) {
                            el.setAttributeNode(at);
                        }

                        // fire user data NODE_RENAMED event
                        callUserDataHandlers(at, null, UserDataHandler.NODE_RENAMED);
                    }
                    else {
                        // we need to create a new object
                        AttrNSImpl nat = new AttrNSImpl(this, namespaceURI, name);

                        // register event listeners on new node
                        copyEventListeners(at, nat);

                        // remove user data from old node
                        Map<String, UserDataRecord> data = removeUserDataTable(at);

                        // move children to new node
                        Node child = at.getFirstChild();
                        while (child != null) {
                            at.removeChild(child);
                            nat.appendChild(child);
                            child = at.getFirstChild();
                        }

                        // attach user data to new node
                        setUserDataTable(nat, data);

                        // and fire user data NODE_RENAMED event
                        callUserDataHandlers(at, nat, UserDataHandler.NODE_RENAMED);

                        // reattach attr to element
                        if (el != null) {
                            el.setAttributeNode(nat);
                        }
                        at = nat;
                    }
                }
                // fire AttributeNameChanged event
                renamedAttrNode((Attr) n, at);

                return at;
            }
            default: {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
                throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);
            }
        }

    }


    /**
     *  DOM Level 3 WD - Experimental
     *  Normalize document.
     */
    public void normalizeDocument(){
        // No need to normalize if already normalized.
        if (isNormalized() && !isNormalizeDocRequired()) {
            return;
        }
        if (needsSyncChildren()) {
            synchronizeChildren();
        }

        if (domNormalizer == null) {
            domNormalizer = new DOMNormalizer();
        }

        if (fConfiguration == null) {
            fConfiguration =  new DOMConfigurationImpl();
        }
        else {
            fConfiguration.reset();
        }

        domNormalizer.normalizeDocument(this, fConfiguration);
        isNormalized(true);
        //set the XMLversion changed value to false -- once we have finished
        //doing normalization
        xmlVersionChanged = false ;
    }


    /**
     * DOM Level 3 CR - Experimental
     *
     *  The configuration used when <code>Document.normalizeDocument</code> is
     * invoked.
     * @since DOM Level 3
     */
    public DOMConfiguration getDomConfig(){
        if (fConfiguration == null) {
            fConfiguration = new DOMConfigurationImpl();
        }
        return fConfiguration;
    }


    /**
     * Returns the absolute base URI of this node or null if the implementation
     * wasn't able to obtain an absolute URI. Note: If the URI is malformed, a
     * null is returned.
     *
     * @return The absolute base URI of this node or null.
     * @since DOM Level 3
     */
    public String getBaseURI() {
        if (fDocumentURI != null && fDocumentURI.length() != 0 ) {// attribute value is always empty string
            try {
                return new URI(fDocumentURI).toString();
            }
            catch (com.sun.org.apache.xerces.internal.util.URI.MalformedURIException e){
                // REVISIT: what should happen in this case?
                return null;
            }
        }
        return fDocumentURI;
    }

    /**
     * DOM Level 3 WD - Experimental.
     */
    public void setDocumentURI(String documentURI){
        fDocumentURI = documentURI;
    }


    //
    // DOM L3 LS
    //
    /**
     * DOM Level 3 WD - Experimental.
     * Indicates whether the method load should be synchronous or
     * asynchronous. When the async attribute is set to <code>true</code>
     * the load method returns control to the caller before the document has
     * completed loading. The default value of this property is
     * <code>false</code>.
     * <br>Setting the value of this attribute might throw NOT_SUPPORTED_ERR
     * if the implementation doesn't support the mode the attribute is being
     * set to. Should the DOM spec define the default value of this
     * property? What if implementing both async and sync IO is impractical
     * in some systems?  2001-09-14. default is <code>false</code> but we
     * need to check with Mozilla and IE.
     */
    public boolean getAsync() {
        return false;
    }

    /**
     * DOM Level 3 WD - Experimental.
     * Indicates whether the method load should be synchronous or
     * asynchronous. When the async attribute is set to <code>true</code>
     * the load method returns control to the caller before the document has
     * completed loading. The default value of this property is
     * <code>false</code>.
     * <br>Setting the value of this attribute might throw NOT_SUPPORTED_ERR
     * if the implementation doesn't support the mode the attribute is being
     * set to. Should the DOM spec define the default value of this
     * property? What if implementing both async and sync IO is impractical
     * in some systems?  2001-09-14. default is <code>false</code> but we
     * need to check with Mozilla and IE.
     */
    public void setAsync(boolean async) {
        if (async) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
            throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);
        }
    }
    /**
     * DOM Level 3 WD - Experimental.
     * If the document is currently being loaded as a result of the method
     * <code>load</code> being invoked the loading and parsing is
     * immediately aborted. The possibly partial result of parsing the
     * document is discarded and the document is cleared.
     */
    public void abort() {
    }

    /**
     * DOM Level 3 WD - Experimental.
     *
     * Replaces the content of the document with the result of parsing the
     * given URI. Invoking this method will either block the caller or
     * return to the caller immediately depending on the value of the async
     * attribute. Once the document is fully loaded a "load" event (as
     * defined in [<a href='http://www.w3.org/TR/2003/WD-DOM-Level-3-Events-20030331'>DOM Level 3 Events</a>]
     * , except that the <code>Event.targetNode</code> will be the document,
     * not an element) will be dispatched on the document. If an error
     * occurs, an implementation dependent "error" event will be dispatched
     * on the document. If this method is called on a document that is
     * currently loading, the current load is interrupted and the new URI
     * load is initiated.
     * <br> When invoking this method the parameters used in the
     * <code>DOMParser</code> interface are assumed to have their default
     * values with the exception that the parameters <code>"entities"</code>
     * , <code>"normalize-characters"</code>,
     * <code>"check-character-normalization"</code> are set to
     * <code>"false"</code>.
     * <br> The result of a call to this method is the same the result of a
     * call to <code>DOMParser.parseWithContext</code> with an input stream
     * referencing the URI that was passed to this call, the document as the
     * context node, and the action <code>ACTION_REPLACE_CHILDREN</code>.
     * @param uri The URI reference for the XML file to be loaded. If this is
     *  a relative URI, the base URI used by the implementation is
     *  implementation dependent.
     * @return If async is set to <code>true</code> <code>load</code> returns
     *   <code>true</code> if the document load was successfully initiated.
     *   If an error occurred when initiating the document load,
     *   <code>load</code> returns <code>false</code>.If async is set to
     *   <code>false</code> <code>load</code> returns <code>true</code> if
     *   the document was successfully loaded and parsed. If an error
     *   occurred when either loading or parsing the URI, <code>load</code>
     *   returns <code>false</code>.
     */
    public boolean load(String uri) {
        return false;
    }

    /**
     * DOM Level 3 WD - Experimental.
     * Replace the content of the document with the result of parsing the
     * input string, this method is always synchronous.
     * @param source A string containing an XML document.
     * @return <code>true</code> if parsing the input string succeeded
     *   without errors, otherwise <code>false</code>.
     */
    public boolean loadXML(String source) {
        return false;
    }

    /**
     * DOM Level 3 WD - Experimental.
     * Save the document or the given node and all its descendants to a string
     * (i.e. serialize the document or node).
     * <br>The parameters used in the <code>LSSerializer</code> interface are
     * assumed to have their default values when invoking this method.
     * <br> The result of a call to this method is the same the result of a
     * call to <code>LSSerializer.writeToString</code> with the document as
     * the node to write.
     * @param node Specifies what to serialize, if this parameter is
     *   <code>null</code> the whole document is serialized, if it's
     *   non-null the given node is serialized.
     * @return The serialized document or <code>null</code> in case an error
     *   occurred.
     * @exception DOMException
     *   WRONG_DOCUMENT_ERR: Raised if the node passed in as the node
     *   parameter is from an other document.
     */
    public String saveXML(Node node)
            throws DOMException {
        if (errorChecking && node != null
                && this != node.getOwnerDocument()) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "WRONG_DOCUMENT_ERR", null);
            throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, msg);
        }
        DOMImplementationLS domImplLS = (DOMImplementationLS) DOMImplementationImpl.getDOMImplementation();
        LSSerializer xmlWriter = domImplLS.createLSSerializer();
        if (node == null) {
            node = this;
        }
        return xmlWriter.writeToString(node);
    }

    /**
     * Sets whether the DOM implementation generates mutation events upon
     * operations.
     */
    void setMutationEvents(boolean set) {
        // does nothing by default - overidden in subclass
    }

    /**
     * Returns true if the DOM implementation generates mutation events.
     */
    boolean getMutationEvents() {
        // does nothing by default - overriden in subclass
        return false;
    }

    // non-DOM factory methods
    /**
     * NON-DOM Factory method; creates a DocumentType having this Document as
     * its OwnerDoc. (REC-DOM-Level-1-19981001 left the process of building DTD
     * information unspecified.)
     *
     * @param name The name of the Entity we wish to provide a value for.
     *
     * @throws DOMException(NOT_SUPPORTED_ERR) for HTML documents, where DTDs
     * are not permitted. (HTML not yet implemented.)
     */
    public DocumentType createDocumentType(String qualifiedName,
            String publicID,
            String systemID)
            throws DOMException {

        return new DocumentTypeImpl(this, qualifiedName, publicID, systemID);

    } // createDocumentType(String):DocumentType

    /**
     * NON-DOM Factory method; creates an Entity having this Document as its
     * OwnerDoc. (REC-DOM-Level-1-19981001 left the process of building DTD
     * information unspecified.)
     *
     * @param name The name of the Entity we wish to provide a value for.
     *
     * @throws DOMException(NOT_SUPPORTED_ERR) for HTML documents, where
     * nonstandard entities are not permitted. (HTML not yet implemented.)
     */
    public Entity createEntity(String name)
            throws DOMException {

        if (errorChecking && !isXMLName(name, xml11Version)) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
            throw new DOMException(DOMException.INVALID_CHARACTER_ERR, msg);
        }
        return new EntityImpl(this, name);

    } // createEntity(String):Entity

    /**
     * NON-DOM Factory method; creates a Notation having this Document as its
     * OwnerDoc. (REC-DOM-Level-1-19981001 left the process of building DTD
     * information unspecified.)
     *
     * @param name The name of the Notation we wish to describe
     *
     * @throws DOMException(NOT_SUPPORTED_ERR) for HTML documents, where
     * notations are not permitted. (HTML not yet implemented.)
     */
    public Notation createNotation(String name)
            throws DOMException {

        if (errorChecking && !isXMLName(name, xml11Version)) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
            throw new DOMException(DOMException.INVALID_CHARACTER_ERR, msg);
        }
        return new NotationImpl(this, name);

    } // createNotation(String):Notation

    /**
     * NON-DOM Factory method: creates an element definition. Element
     * definitions hold default attribute values.
     */
    public ElementDefinitionImpl createElementDefinition(String name)
            throws DOMException {

        if (errorChecking && !isXMLName(name, xml11Version)) {
            String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "INVALID_CHARACTER_ERR", null);
            throw new DOMException(DOMException.INVALID_CHARACTER_ERR, msg);
        }
        return new ElementDefinitionImpl(this, name);

    } // createElementDefinition(String):ElementDefinitionImpl

    // other non-DOM methods
    /**
     * NON-DOM: Get the number associated with this document. Used to order
     * documents in the implementation.
     */
    protected int getNodeNumber() {
        if (documentNumber == 0) {

            CoreDOMImplementationImpl cd = (CoreDOMImplementationImpl) CoreDOMImplementationImpl.getDOMImplementation();
            documentNumber = cd.assignDocumentNumber();
        }
        return documentNumber;
    }

    /**
     * NON-DOM: Get a number associated with a node created with respect to this
     * document. Needed for compareDocumentPosition when nodes are disconnected.
     * This is only used on demand.
     */
    protected int getNodeNumber(Node node) {

        // Check if the node is already in the hash
        // If so, retrieve the node number
        // If not, assign a number to the node
        // Node numbers are negative, from -1 to -n
        int num;
        if (nodeTable == null) {
            nodeTable = new HashMap<>();
            num = --nodeCounter;
            nodeTable.put(node, num);
        } else {
            Integer n = nodeTable.get(node);
            if (n == null) {
                num = --nodeCounter;
                nodeTable.put(node, num);
            } else {
                num = n.intValue();
            }
        }
        return num;
    }

    /**
     * Copies a node from another document to this document. The new nodes are
     * created using this document's factory methods and are populated with the
     * data from the source's accessor methods defined by the DOM interfaces.
     * Its behavior is otherwise similar to that of cloneNode.
     * <p>
     * According to the DOM specifications, document nodes cannot be imported
     * and a NOT_SUPPORTED_ERR exception is thrown if attempted.
     */
    public Node importNode(Node source, boolean deep)
            throws DOMException {
        return importNode(source, deep, false, null);
    } // importNode(Node,boolean):Node

    /**
     * Overloaded implementation of DOM's importNode method. This method
     * provides the core functionality for the public importNode and cloneNode
     * methods.
     *
     * The reversedIdentifiers parameter is provided for cloneNode to preserve
     * the document's identifiers. The Map has Elements as the keys and
     * their identifiers as the values. When an element is being imported, a
     * check is done for an associated identifier. If one exists, the identifier
     * is registered with the new, imported element. If reversedIdentifiers is
     * null, the parameter is not applied.
     */
    private Node importNode(Node source, boolean deep, boolean cloningDoc,
            Map<Node, String> reversedIdentifiers)
            throws DOMException {
        Node newnode = null;
        Map<String, UserDataRecord> userData = null;

        // Sigh. This doesn't work; too many nodes have private data that
        // would have to be manually tweaked. May be able to add local
        // shortcuts to each nodetype. Consider ?????
        // if(source instanceof NodeImpl &&
        //  !(source instanceof DocumentImpl))
        // {
        //  // Can't clone DocumentImpl since it invokes us...
        //  newnode=(NodeImpl)source.cloneNode(false);
        //  newnode.ownerDocument=this;
        // }
        // else
        if (source instanceof NodeImpl) {
            userData = ((NodeImpl) source).getUserDataRecord();
        }
        int type = source.getNodeType();
        switch (type) {
            case ELEMENT_NODE: {
                Element newElement;
                boolean domLevel20 = source.getOwnerDocument().getImplementation().hasFeature("XML", "2.0");
                // Create element according to namespace support/qualification.
                if(domLevel20 == false || source.getLocalName() == null)
                    newElement = createElement(source.getNodeName());
                else
                    newElement = createElementNS(source.getNamespaceURI(),
                            source.getNodeName());

                // Copy element's attributes, if any.
                NamedNodeMap sourceAttrs = source.getAttributes();
                if (sourceAttrs != null) {
                    int length = sourceAttrs.getLength();
                    for (int index = 0; index < length; index++) {
                        Attr attr = (Attr)sourceAttrs.item(index);

                        // NOTE: this methods is used for both importingNode
                        // and cloning the document node. In case of the
                        // clonning default attributes should be copied.
                        // But for importNode defaults should be ignored.
                        if (attr.getSpecified() || cloningDoc) {
                            Attr newAttr = (Attr)importNode(attr, true, cloningDoc,
                                    reversedIdentifiers);

                            // Attach attribute according to namespace
                            // support/qualification.
                            if (domLevel20 == false ||
                            attr.getLocalName() == null)
                                newElement.setAttributeNode(newAttr);
                            else
                                newElement.setAttributeNodeNS(newAttr);
                            }
                        }
                    }

                // Register element identifier.
                if (reversedIdentifiers != null) {
                    // Does element have an associated identifier?
                    String elementId = reversedIdentifiers.get(source);
                    if (elementId != null) {
                        if (identifiers == null) {
                            identifiers = new HashMap<>();
                        }

                        identifiers.put(elementId, newElement);
                    }
                }

                newnode = newElement;
                break;
            }

            case ATTRIBUTE_NODE: {

                if( source.getOwnerDocument().getImplementation().hasFeature("XML", "2.0") ){
                    if (source.getLocalName() == null) {
                        newnode = createAttribute(source.getNodeName());
                    } else {
                        newnode = createAttributeNS(source.getNamespaceURI(),
                                source.getNodeName());
                    }
                }
                else {
                    newnode = createAttribute(source.getNodeName());
                }
                // if source is an AttrImpl from this very same implementation
                // avoid creating the child nodes if possible
                if (source instanceof AttrImpl) {
                    AttrImpl attr = (AttrImpl) source;
                    if (attr.hasStringValue()) {
                        AttrImpl newattr = (AttrImpl) newnode;
                        newattr.setValue(attr.getValue());
                        deep = false;
                    }
                    else {
                        deep = true;
                    }
                }
                else {
                    // According to the DOM spec the kids carry the value.
                    // However, there are non compliant implementations out
                    // there that fail to do so. To avoid ending up with no
                    // value at all, in this case we simply copy the text value
                    // directly.
                    if (source.getFirstChild() == null) {
                        newnode.setNodeValue(source.getNodeValue());
                        deep = false;
                    } else {
                        deep = true;
                    }
                }
                break;
            }

            case TEXT_NODE: {
                newnode = createTextNode(source.getNodeValue());
                break;
            }

            case CDATA_SECTION_NODE: {
                newnode = createCDATASection(source.getNodeValue());
                break;
            }

            case ENTITY_REFERENCE_NODE: {
                newnode = createEntityReference(source.getNodeName());
                // the subtree is created according to this doc by the method
                // above, so avoid carrying over original subtree
                deep = false;
                break;
            }

            case ENTITY_NODE: {
                Entity srcentity = (Entity)source;
                EntityImpl newentity =
                (EntityImpl)createEntity(source.getNodeName());
                newentity.setPublicId(srcentity.getPublicId());
                newentity.setSystemId(srcentity.getSystemId());
                newentity.setNotationName(srcentity.getNotationName());
                // Kids carry additional value,
                // allow deep import temporarily
                newentity.isReadOnly(false);
                newnode = newentity;
                break;
            }

            case PROCESSING_INSTRUCTION_NODE: {
                newnode = createProcessingInstruction(source.getNodeName(),
                        source.getNodeValue());
                break;
            }

            case COMMENT_NODE: {
                newnode = createComment(source.getNodeValue());
                break;
            }

            case DOCUMENT_TYPE_NODE: {
                // unless this is used as part of cloning a Document
                // forbid it for the sake of being compliant to the DOM spec
                if (!cloningDoc) {
                    String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
                    throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);
                }
                DocumentType srcdoctype = (DocumentType)source;
                DocumentTypeImpl newdoctype = (DocumentTypeImpl)
                createDocumentType(srcdoctype.getNodeName(),
                        srcdoctype.getPublicId(),
                        srcdoctype.getSystemId());
                // Values are on NamedNodeMaps
                NamedNodeMap smap = srcdoctype.getEntities();
                NamedNodeMap tmap = newdoctype.getEntities();
                if(smap != null) {
                    for(int i = 0; i < smap.getLength(); i++) {
                        tmap.setNamedItem(importNode(smap.item(i), true, true,
                                reversedIdentifiers));
                    }
                }
                smap = srcdoctype.getNotations();
                tmap = newdoctype.getNotations();
                if (smap != null) {
                    for(int i = 0; i < smap.getLength(); i++) {
                        tmap.setNamedItem(importNode(smap.item(i), true, true,
                                reversedIdentifiers));
                    }
                }

                // NOTE: At this time, the DOM definition of DocumentType
                // doesn't cover Elements and their Attributes. domimpl's
                // extentions in that area will not be preserved, even if
                // copying from domimpl to domimpl. We could special-case
                // that here. Arguably we should. Consider. ?????
                newnode = newdoctype;
                break;
            }

            case DOCUMENT_FRAGMENT_NODE: {
                newnode = createDocumentFragment();
                // No name, kids carry value
                break;
            }

            case NOTATION_NODE: {
                Notation srcnotation = (Notation)source;
                NotationImpl newnotation =
                (NotationImpl)createNotation(source.getNodeName());
                newnotation.setPublicId(srcnotation.getPublicId());
                newnotation.setSystemId(srcnotation.getSystemId());
                // Kids carry additional value
                newnode = newnotation;
                // No name, no value
                break;
            }
            case DOCUMENT_NODE : // Can't import document nodes
            default: {           // Unknown node type
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
                throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);
            }
        }

                if(userData != null)
                        callUserDataHandlers(source, newnode, UserDataHandler.NODE_IMPORTED,userData);

        // If deep, replicate and attach the kids.
        if (deep) {
            for (Node srckid = source.getFirstChild();
                    srckid != null;
                    srckid = srckid.getNextSibling()) {
                newnode.appendChild(importNode(srckid, true, cloningDoc,
                        reversedIdentifiers));
            }
        }
        if (newnode.getNodeType() == Node.ENTITY_NODE) {
            ((NodeImpl)newnode).setReadOnly(true, true);
        }
        return newnode;

    } // importNode(Node,boolean,boolean,Map):Node

    /**
     * DOM Level 3 WD - Experimental
     * Change the node's ownerDocument, and its subtree, to this Document
     *
     * @param source The node to adopt.
     * @see #importNode
     **/
    public Node adoptNode(Node source) {
        NodeImpl node;
        Map<String, UserDataRecord> userData;
        try {
            node = (NodeImpl) source;
        } catch (ClassCastException e) {
            // source node comes from a different DOMImplementation
            return null;
        }

        // Return null if the source is null

        if (source == null ) {
            return null;
        } else if (source.getOwnerDocument() != null) {

            DOMImplementation thisImpl = this.getImplementation();
            DOMImplementation otherImpl = source.getOwnerDocument().getImplementation();

            // when the source node comes from a different implementation.
            if (thisImpl != otherImpl) {

                // Adopting from a DefferedDOM to DOM
                if (thisImpl instanceof com.sun.org.apache.xerces.internal.dom.DOMImplementationImpl &&
                        otherImpl instanceof com.sun.org.apache.xerces.internal.dom.DeferredDOMImplementationImpl) {
                    // traverse the DOM and expand deffered nodes and then allow adoption
                    undeferChildren (node);
                } else if ( thisImpl instanceof com.sun.org.apache.xerces.internal.dom.DeferredDOMImplementationImpl
                        && otherImpl instanceof com.sun.org.apache.xerces.internal.dom.DOMImplementationImpl) {
                    // Adopting from a DOM into a DefferedDOM, this should be okay
                } else {
                    // Adopting between two dissimilar DOM's is not allowed
                    return null;
                }
            }
            // Adopting from a deferred DOM into another deferred DOM
            else if (otherImpl instanceof DeferredDOMImplementationImpl) {
                // traverse the DOM and expand deferred nodes and then allow adoption
                undeferChildren (node);
            }
        }

        switch (node.getNodeType()) {
            case ATTRIBUTE_NODE: {
                AttrImpl attr = (AttrImpl) node;
                // remove node from wherever it is
                if( attr.getOwnerElement() != null){
                    //1. owner element attribute is set to null
                    attr.getOwnerElement().removeAttributeNode(attr);
                }
                //2. specified flag is set to true
                attr.isSpecified(true);
                userData = node.getUserDataRecord();

                //3. change ownership
                attr.setOwnerDocument(this);
                if (userData != null) {
                    setUserDataTable(node, userData);
                }
                break;
            }
            //entity, notation nodes are read only nodes.. so they can't be adopted.
            //runtime will fall through to NOTATION_NODE
            case ENTITY_NODE:
            case NOTATION_NODE:{
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null);
                throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, msg);

            }
            //document, documentype nodes can't be adopted.
            //runtime will fall through to DocumentTypeNode
            case DOCUMENT_NODE:
            case DOCUMENT_TYPE_NODE: {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
                throw new DOMException(DOMException.NOT_SUPPORTED_ERR, msg);
            }
            case ENTITY_REFERENCE_NODE: {
                userData = node.getUserDataRecord();
                // remove node from wherever it is
                Node parent = node.getParentNode();
                if (parent != null) {
                    parent.removeChild(source);
                }
                // discard its replacement value
                Node child;
                while ((child = node.getFirstChild()) != null) {
                    node.removeChild(child);
                }
                // change ownership
                node.setOwnerDocument(this);
                if (userData != null) {
                    setUserDataTable(node, userData);
                }
                // set its new replacement value if any
                if (docType == null) {
                    break;
                }
                NamedNodeMap entities = docType.getEntities();
                Node entityNode = entities.getNamedItem(node.getNodeName());
                if (entityNode == null) {
                    break;
                }
                for (child = entityNode.getFirstChild();
                        child != null; child = child.getNextSibling()) {
                    Node childClone = child.cloneNode(true);
                    node.appendChild(childClone);
                }
                break;
            }
            case ELEMENT_NODE: {
                userData = node.getUserDataRecord();
                // remove node from wherever it is
                Node parent = node.getParentNode();
                if (parent != null) {
                    parent.removeChild(source);
                }
                // change ownership
                node.setOwnerDocument(this);
                if (userData != null) {
                    setUserDataTable(node, userData);
                }
                // reconcile default attributes
                ((ElementImpl)node).reconcileDefaultAttributes();
                break;
            }
            default: {
                userData = node.getUserDataRecord();
                // remove node from wherever it is
                Node parent = node.getParentNode();
                if (parent != null) {
                    parent.removeChild(source);
                }
                // change ownership
                node.setOwnerDocument(this);
                if (userData != null) {
                    setUserDataTable(node, userData);
                }
            }
        }

                //DOM L3 Core CR
        //http://www.w3.org/TR/2003/CR-DOM-Level-3-Core-20031107/core.html#UserDataHandler-ADOPTED
        if (userData != null) {
            callUserDataHandlers(source, null, UserDataHandler.NODE_ADOPTED, userData);
        }

        return node;
    }

    /**
     * Traverses the DOM Tree and expands deferred nodes and their
     * children.
     *
     */
    protected void undeferChildren(Node node) {

        Node top = node;

        while (null != node) {

            if (((NodeImpl)node).needsSyncData()) {
                ((NodeImpl)node).synchronizeData();
            }

            NamedNodeMap attributes = node.getAttributes();
            if (attributes != null) {
                int length = attributes.getLength();
                for (int i = 0; i < length; ++i) {
                    undeferChildren(attributes.item(i));
                }
            }

            Node nextNode = null;
            nextNode = node.getFirstChild();

            while (null == nextNode) {

                if (top.equals(node))
                    break;

                nextNode = node.getNextSibling();

                if (null == nextNode) {
                    node = node.getParentNode();

                    if ((null == node) || (top.equals(node))) {
                        nextNode = null;
                        break;
                    }
                }
            }

            node = nextNode;
        }
    }

    // identifier maintenence
    /**
     * Introduced in DOM Level 2
     * Returns the Element whose ID is given by elementId. If no such element
     * exists, returns null. Behavior is not defined if more than one element
     * has this ID.
     * <p>
     * Note: The DOM implementation must have information that says which
     * attributes are of type ID. Attributes with the name "ID" are not of type
     * ID unless so defined. Implementations that do not know whether
     * attributes are of type ID or not are expected to return null.
     * @see #getIdentifier
     */
    public Element getElementById(String elementId) {
        return getIdentifier(elementId);
    }

    /**
     * Remove all identifiers from the ID table
     */
    protected final void clearIdentifiers(){
        if (identifiers != null){
            identifiers.clear();
        }
    }

    /**
     * Registers an identifier name with a specified element node.
     * If the identifier is already registered, the new element
     * node replaces the previous node. If the specified element
     * node is null, removeIdentifier() is called.
     *
     * @see #getIdentifier
     * @see #removeIdentifier
     */
    public void putIdentifier(String idName, Element element) {

        if (element == null) {
            removeIdentifier(idName);
            return;
        }

        if (needsSyncData()) {
            synchronizeData();
        }

        if (identifiers == null) {
            identifiers = new HashMap<>();
        }

        identifiers.put(idName, element);

    } // putIdentifier(String,Element)

    /**
     * Returns a previously registered element with the specified
     * identifier name, or null if no element is registered.
     *
     * @see #putIdentifier
     * @see #removeIdentifier
     */
    public Element getIdentifier(String idName) {

        if (needsSyncData()) {
            synchronizeData();
        }

        if (identifiers == null) {
            return null;
        }
        Element elem = (Element) identifiers.get(idName);
        if (elem != null) {
            // check that the element is in the tree
            Node parent = elem.getParentNode();
            while (parent != null) {
                if (parent == this) {
                    return elem;
                }
                parent = parent.getParentNode();
            }
        }
        return null;
    } // getIdentifier(String):Element

    /**
     * Removes a previously registered element with the specified
     * identifier name.
     *
     * @see #putIdentifier
     * @see #getIdentifier
     */
    public void removeIdentifier(String idName) {

        if (needsSyncData()) {
            synchronizeData();
        }

        if (identifiers == null) {
            return;
        }

        identifiers.remove(idName);

    } // removeIdentifier(String)

    //
    // DOM2: Namespace methods
    //
    /**
     * Introduced in DOM Level 2. <p>
     * Creates an element of the given qualified name and namespace URI.
     * If the given namespaceURI is null or an empty string and the
     * qualifiedName has a prefix that is "xml", the created element
     * is bound to the predefined namespace
     * "http://www.w3.org/XML/1998/namespace" [Namespaces].
     * @param namespaceURI The namespace URI of the element to
     *                     create.
     * @param qualifiedName The qualified name of the element type to
     *                      instantiate.
     * @return Element A new Element object with the following attributes:
     * @throws DOMException INVALID_CHARACTER_ERR: Raised if the specified
     * name contains an invalid character.
     * @throws DOMException NAMESPACE_ERR: Raised if the qualifiedName has a
     *                      prefix that is "xml" and the namespaceURI is
     *                      neither null nor an empty string nor
     *                      "http://www.w3.org/XML/1998/namespace", or
     *                      if the qualifiedName has a prefix different
     *                      from "xml" and the namespaceURI is null or an
     *                      empty string.
     * @since WD-DOM-Level-2-19990923
     */
    public Element createElementNS(String namespaceURI, String qualifiedName)
            throws DOMException {
        return new ElementNSImpl(this, namespaceURI, qualifiedName);
    }

    /**
     * NON-DOM: a factory method used by the Xerces DOM parser
     * to create an element.
     *
     * @param namespaceURI The namespace URI of the element to
     *                     create.
     * @param qualifiedName The qualified name of the element type to
     *                      instantiate.
     * @param localpart  The local name of the attribute to instantiate.
     *
     * @return Element A new Element object with the following attributes:
     * @exception DOMException INVALID_CHARACTER_ERR: Raised if the specified
     *                   name contains an invalid character.
     */
    public Element createElementNS(String namespaceURI, String qualifiedName,
            String localpart)
            throws DOMException {
        return new ElementNSImpl(this, namespaceURI, qualifiedName, localpart);
    }

    /**
     * Introduced in DOM Level 2. <p>
     * Creates an attribute of the given qualified name and namespace URI.
     * If the given namespaceURI is null or an empty string and the
     * qualifiedName has a prefix that is "xml", the created element
     * is bound to the predefined namespace
     * "http://www.w3.org/XML/1998/namespace" [Namespaces].
     *
     * @param namespaceURI  The namespace URI of the attribute to
     *                      create. When it is null or an empty string,
     *                      this method behaves like createAttribute.
     * @param qualifiedName The qualified name of the attribute to
     *                      instantiate.
     * @return Attr         A new Attr object.
     * @throws DOMException INVALID_CHARACTER_ERR: Raised if the specified
     * name contains an invalid character.
     * @since WD-DOM-Level-2-19990923
     */
    public Attr createAttributeNS(String namespaceURI, String qualifiedName)
            throws DOMException {
        return new AttrNSImpl(this, namespaceURI, qualifiedName);
    }

    /**
     * NON-DOM: a factory method used by the Xerces DOM parser
     * to create an element.
     *
     * @param namespaceURI  The namespace URI of the attribute to
     *                      create. When it is null or an empty string,
     *                      this method behaves like createAttribute.
     * @param qualifiedName The qualified name of the attribute to
     *                      instantiate.
     * @param localpart     The local name of the attribute to instantiate.
     *
     * @return Attr         A new Attr object.
     * @throws DOMException INVALID_CHARACTER_ERR: Raised if the specified
     * name contains an invalid character.
     */
    public Attr createAttributeNS(String namespaceURI, String qualifiedName,
            String localpart)
            throws DOMException {
        return new AttrNSImpl(this, namespaceURI, qualifiedName, localpart);
    }

    /**
     * Introduced in DOM Level 2. <p>
     * Returns a NodeList of all the Elements with a given local name and
     * namespace URI in the order in which they would be encountered in a
     * preorder traversal of the Document tree.
     * @param namespaceURI  The namespace URI of the elements to match
     *                      on. The special value "*" matches all
     *                      namespaces. When it is null or an empty
     *                      string, this method behaves like
     *                      getElementsByTagName.
     * @param localName     The local name of the elements to match on.
     *                      The special value "*" matches all local names.
     * @return NodeList     A new NodeList object containing all the matched
     *                      Elements.
     * @since WD-DOM-Level-2-19990923
     */
    public NodeList getElementsByTagNameNS(String namespaceURI,
            String localName) {
        return new DeepNodeListImpl(this, namespaceURI, localName);
    }

    //
    // Object methods
    //

    /** Clone. */
    public Object clone() throws CloneNotSupportedException {
        CoreDocumentImpl newdoc = (CoreDocumentImpl) super.clone();
        newdoc.docType = null;
        newdoc.docElement = null;
        return newdoc;
    }

    //
    // Public static methods
    //

    /**
     * Check the string against XML's definition of acceptable names for
     * elements and attributes and so on using the XMLCharacterProperties
     * utility class
     */

    public static final boolean isXMLName(String s, boolean xml11Version) {

        if (s == null) {
            return false;
        }
        if(!xml11Version)
            return XMLChar.isValidName(s);
        else
            return XML11Char.isXML11ValidName(s);

    } // isXMLName(String):boolean

    /**
     * Checks if the given qualified name is legal with respect
     * to the version of XML to which this document must conform.
     *
     * @param prefix prefix of qualified name
     * @param local local part of qualified name
     */
    public static final boolean isValidQName(String prefix, String local, boolean xml11Version) {

        // check that both prefix and local part match NCName
        if (local == null) return false;
        boolean validNCName = false;

        if (!xml11Version) {
            validNCName = (prefix == null || XMLChar.isValidNCName(prefix))
                    && XMLChar.isValidNCName(local);
        }
        else {
            validNCName = (prefix == null || XML11Char.isXML11ValidNCName(prefix))
                    && XML11Char.isXML11ValidNCName(local);
        }

        return validNCName;
    }
    //
    // Protected methods
    //

    /**
     * Uses the kidOK lookup table to check whether the proposed
     * tree structure is legal.
     */
    protected boolean isKidOK(Node parent, Node child) {
        if (allowGrammarAccess &&
        parent.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
            return child.getNodeType() == Node.ELEMENT_NODE;
        }
        return 0 != (kidOK[parent.getNodeType()] & 1 << child.getNodeType());
    }

    /**
     * Denotes that this node has changed.
     */
    protected void changed() {
        changes++;
    }

    /**
     * Returns the number of changes to this node.
     */
    protected int changes() {
        return changes;
    }

    //  NodeListCache pool

    /**
     * Returns a NodeListCache for the given node.
     */
    NodeListCache getNodeListCache(ParentNode owner) {
        if (fFreeNLCache == null) {
            return new NodeListCache(owner);
        }
        NodeListCache c = fFreeNLCache;
        fFreeNLCache = fFreeNLCache.next;
        c.fChild = null;
        c.fChildIndex = -1;
        c.fLength = -1;
        // revoke previous ownership
        if (c.fOwner != null) {
            c.fOwner.fNodeListCache = null;
        }
        c.fOwner = owner;
        // c.next = null; not necessary, except for confused people...
        return c;
    }

    /**
     * Puts the given NodeListCache in the free list.
     * Note: The owner node can keep using it until we reuse it
     */
    void freeNodeListCache(NodeListCache c) {
        c.next = fFreeNLCache;
        fFreeNLCache = c;
    }



    /**
     * Associate an object to a key on this node. The object can later be
     * retrieved from this node by calling <code>getUserData</code> with the
     * same key.
     * @param n The node to associate the object to.
     * @param key The key to associate the object to.
     * @param data The object to associate to the given key, or
     *   <code>null</code> to remove any existing association to that key.
     * @param handler The handler to associate to that key, or
     *   <code>null</code>.
     * @return Returns the <code>DOMObject</code> previously associated to
     *   the given key on this node, or <code>null</code> if there was none.
     * @since DOM Level 3
     *
     * REVISIT: we could use a free list of UserDataRecord here
     */
    public Object setUserData(Node n, String key,
            Object data, UserDataHandler handler) {
        if (data == null) {
            if (nodeUserData != null) {
                Map<String, UserDataRecord> t = nodeUserData.get(n);
                if (t != null) {
                    UserDataRecord r = t.remove(key);
                    if (r != null) {
                        return r.fData;
                    }
                }
            }
            return null;
        } else {
            Map<String, UserDataRecord> t;
            if (nodeUserData == null) {
                nodeUserData = new HashMap<>();
                t = new HashMap<>();
                nodeUserData.put(n, t);
            } else {
                t = nodeUserData.get(n);
                if (t == null) {
                    t = new HashMap<>();
                    nodeUserData.put(n, t);
                }
            }
            UserDataRecord r = t.put(key, new UserDataRecord(data, handler));
            if (r != null) {
                return r.fData;
            }
            return null;
        }
    }


    /**
     * Retrieves the object associated to a key on a this node. The object
     * must first have been set to this node by calling
     * <code>setUserData</code> with the same key.
     * @param n The node the object is associated to.
     * @param key The key the object is associated to.
     * @return Returns the <code>DOMObject</code> associated to the given key
     *   on this node, or <code>null</code> if there was none.
     * @since DOM Level 3
     */
    public Object getUserData(Node n, String key) {
        if (nodeUserData == null) {
            return null;
        }
        Map<String, UserDataRecord> t = nodeUserData.get(n);
        if (t == null) {
            return null;
        }
        UserDataRecord r = t.get(key);
        if (r != null) {
            return r.fData;
        }
        return null;
    }

    protected Map<String, UserDataRecord> getUserDataRecord(Node n) {
        if (nodeUserData == null) {
            return null;
        }
        Map<String, UserDataRecord> t = nodeUserData.get(n);
        if (t == null) {
            return null;
        }
        return t;
    }

    /**
     * Remove user data table for the given node.
     * @param n The node this operation applies to.
     * @return The removed table.
     */
    Map<String, UserDataRecord> removeUserDataTable(Node n) {
        if (nodeUserData == null) {
            return null;
        }
        return nodeUserData.get(n);
    }

    /**
     * Set user data table for the given node.
     * @param n The node this operation applies to.
     * @param data The user data table.
     */
    void setUserDataTable(Node n, Map<String, UserDataRecord> data) {
        if (nodeUserData == null) {
            nodeUserData = new HashMap<>();
        }

        if (data != null) {
            nodeUserData.put(n, data);
        }
    }

    /**
     * Call user data handlers when a node is deleted (finalized)
     * @param n The node this operation applies to.
     * @param c The copy node or null.
     * @param operation The operation - import, clone, or delete.
     */
    void callUserDataHandlers(Node n, Node c, short operation) {
        if (nodeUserData == null) {
            return;
        }

        if (n instanceof NodeImpl) {
            Map<String, UserDataRecord> t = ((NodeImpl) n).getUserDataRecord();
            if (t == null || t.isEmpty()) {
                return;
            }
            callUserDataHandlers(n, c, operation, t);
        }
    }

    /**
     * Call user data handlers when a node is deleted (finalized)
     * @param n The node this operation applies to.
     * @param c The copy node or null.
     * @param operation The operation - import, clone, or delete.
     * @param handlers Data associated with n.
     */
    void callUserDataHandlers(Node n, Node c, short operation, Map<String, UserDataRecord> userData) {
        if (userData == null || userData.isEmpty()) {
            return;
        }

        userData.keySet().stream().forEach((key) -> {
            UserDataRecord r = userData.get(key);
            if (r.fHandler != null) {
                r.fHandler.handle(operation, key, r.fData, n, c);
            }
        });
    }

    /**
     * Call user data handlers to let them know the nodes they are related to
     * are being deleted. The alternative would be to do that on Node but
     * because the nodes are used as the keys we have a reference to them that
     * prevents them from being gc'ed until the document is. At the same time,
     * doing it here has the advantage of avoiding a finalize() method on Node,
     * which would affect all nodes and not just the ones that have a user
     * data.
     */
    // Temporarily comment out this method, because
    // 1. It seems that finalizers are not guaranteed to be called, so the
    //    functionality is not implemented.
    // 2. It affects the performance greatly in multi-thread environment.
    // -SG
    /*public void finalize() {
     if (userData == null) {
     return;
     }
     Enumeration nodes = userData.keys();
     while (nodes.hasMoreElements()) {
     Object node = nodes.nextElement();
     Hashtable t = (Hashtable) userData.get(node);
     if (t != null && !t.isEmpty()) {
     Enumeration keys = t.keys();
     while (keys.hasMoreElements()) {
     String key = (String) keys.nextElement();
     UserDataRecord r = (UserDataRecord) t.get(key);
     if (r.fHandler != null) {
     r.fHandler.handle(UserDataHandler.NODE_DELETED,
     key, r.fData, null, null);
     }
     }
     }
     }
     }*/

    protected final void checkNamespaceWF( String qname, int colon1,
            int colon2) {

        if (!errorChecking) {
            return;
        }
        // it is an error for NCName to have more than one ':'
        // check if it is valid QName [Namespace in XML production 6]
        // :camera , nikon:camera:minolta, camera:
        if (colon1 == 0 || colon1 == qname.length() - 1 || colon2 != colon1) {
            String msg =
            DOMMessageFormatter.formatMessage(
                            DOMMessageFormatter.DOM_DOMAIN,
                            "NAMESPACE_ERR",
                            null);
            throw new DOMException(DOMException.NAMESPACE_ERR, msg);
        }
    }
    protected final void checkDOMNSErr(String prefix,
            String namespace) {
        if (errorChecking) {
            if (namespace == null) {
                String msg =
                DOMMessageFormatter.formatMessage(
                                DOMMessageFormatter.DOM_DOMAIN,
                                "NAMESPACE_ERR",
                                null);
                throw new DOMException(DOMException.NAMESPACE_ERR, msg);
            }
            else if (prefix.equals("xml")
                    && !namespace.equals(NamespaceContext.XML_URI)) {
                String msg =
                DOMMessageFormatter.formatMessage(
                                DOMMessageFormatter.DOM_DOMAIN,
                                "NAMESPACE_ERR",
                                null);
                throw new DOMException(DOMException.NAMESPACE_ERR, msg);
            }
            else if (
            prefix.equals("xmlns")
                    && !namespace.equals(NamespaceContext.XMLNS_URI)
                    || (!prefix.equals("xmlns")
                    && namespace.equals(NamespaceContext.XMLNS_URI))) {
                String msg =
                DOMMessageFormatter.formatMessage(
                                DOMMessageFormatter.DOM_DOMAIN,
                                "NAMESPACE_ERR",
                                null);
                throw new DOMException(DOMException.NAMESPACE_ERR, msg);
            }
        }
    }

    /**
     * Checks if the given qualified name is legal with respect
     * to the version of XML to which this document must conform.
     *
     * @param prefix prefix of qualified name
     * @param local local part of qualified name
     */
    protected final void checkQName(String prefix, String local) {
        if (!errorChecking) {
            return;
        }

        // check that both prefix and local part match NCName
        boolean validNCName = false;
        if (!xml11Version) {
            validNCName = (prefix == null || XMLChar.isValidNCName(prefix))
                    && XMLChar.isValidNCName(local);
        }
        else {
            validNCName = (prefix == null || XML11Char.isXML11ValidNCName(prefix))
                    && XML11Char.isXML11ValidNCName(local);
        }

        if (!validNCName) {
            // REVISIT: add qname parameter to the message
            String msg =
            DOMMessageFormatter.formatMessage(
                            DOMMessageFormatter.DOM_DOMAIN,
                            "INVALID_CHARACTER_ERR",
                            null);
            throw new DOMException(DOMException.INVALID_CHARACTER_ERR, msg);
        }
    }

    /**
     * We could have more xml versions in future , but for now we could
     * do with this to handle XML 1.0 and 1.1
     */
    boolean isXML11Version(){
        return xml11Version;
    }

    boolean isNormalizeDocRequired(){
        // REVISIT: Implement to optimize when normalization
        // is required
        return true;
    }

    //we should be checking the (elements, attribute, entity etc.) names only when
    //version of the document is changed.
    boolean isXMLVersionChanged(){
        return xmlVersionChanged ;
    }
    /**
     * NON-DOM: kept for backward compatibility
     * Store user data related to a given node
     * This is a place where we could use weak references! Indeed, the node
     * here won't be GC'ed as long as some user data is attached to it, since
     * the userData table will have a reference to the node.
     */
    protected void setUserData(NodeImpl n, Object data) {
        setUserData(n, "XERCES1DOMUSERDATA", data, null);
    }

    /**
     * NON-DOM: kept for backward compatibility
     * Retreive user data related to a given node
     */
    protected Object getUserData(NodeImpl n) {
        return getUserData(n, "XERCES1DOMUSERDATA");
    }


    // Event related methods overidden in subclass

    protected void addEventListener(NodeImpl node, String type,
            EventListener listener,
            boolean useCapture) {
        // does nothing by default - overidden in subclass
    }

    protected void removeEventListener(NodeImpl node, String type,
            EventListener listener,
            boolean useCapture) {
        // does nothing by default - overidden in subclass
    }

    protected void copyEventListeners(NodeImpl src, NodeImpl tgt) {
        // does nothing by default - overidden in subclass
    }

    protected boolean dispatchEvent(NodeImpl node, Event event) {
        // does nothing by default - overidden in subclass
        return false;
    }

    // Notification methods overidden in subclasses

    /**
     * A method to be called when some text was changed in a text node,
     * so that live objects can be notified.
     */
    void replacedText(NodeImpl node) {
    }

    /**
     * A method to be called when some text was deleted from a text node,
     * so that live objects can be notified.
     */
    void deletedText(NodeImpl node, int offset, int count) {
    }

    /**
     * A method to be called when some text was inserted into a text node,
     * so that live objects can be notified.
     */
    void insertedText(NodeImpl node, int offset, int count) {
    }

    /**
     * A method to be called when a character data node is about to be modified
     */
    void modifyingCharacterData(NodeImpl node, boolean replace) {
    }

    /**
     * A method to be called when a character data node has been modified
     */
    void modifiedCharacterData(NodeImpl node, String oldvalue, String value, boolean replace) {
    }

    /**
     * A method to be called when a node is about to be inserted in the tree.
     */
    void insertingNode(NodeImpl node, boolean replace) {
    }

    /**
     * A method to be called when a node has been inserted in the tree.
     */
    void insertedNode(NodeImpl node, NodeImpl newInternal, boolean replace) {
    }

    /**
     * A method to be called when a node is about to be removed from the tree.
     */
    void removingNode(NodeImpl node, NodeImpl oldChild, boolean replace) {
    }

    /**
     * A method to be called when a node has been removed from the tree.
     */
    void removedNode(NodeImpl node, boolean replace) {
    }

    /**
     * A method to be called when a node is about to be replaced in the tree.
     */
    void replacingNode(NodeImpl node) {
    }

    /**
     * A method to be called when a node has been replaced in the tree.
     */
    void replacedNode(NodeImpl node) {
    }

    /**
     * A method to be called when a character data node is about to be replaced
     */
    void replacingData(NodeImpl node) {
    }

    /**
     *  method to be called when a character data node has been replaced.
     */
    void replacedCharacterData(NodeImpl node, String oldvalue, String value) {
    }


    /**
     * A method to be called when an attribute value has been modified
     */
    void modifiedAttrValue(AttrImpl attr, String oldvalue) {
    }

    /**
     * A method to be called when an attribute node has been set
     */
    void setAttrNode(AttrImpl attr, AttrImpl previous) {
    }

    /**
     * A method to be called when an attribute node has been removed
     */
    void removedAttrNode(AttrImpl attr, NodeImpl oldOwner, String name) {
    }

    /**
     * A method to be called when an attribute node has been renamed
     */
    void renamedAttrNode(Attr oldAt, Attr newAt) {
    }

    /**
     * A method to be called when an element has been renamed
     */
    void renamedElement(Element oldEl, Element newEl) {
    }

    /**
     * @serialData Serialized fields. Convert Maps to Hashtables for backward
     * compatibility.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Convert Maps to Hashtables
        Hashtable<Node, Hashtable<String, UserDataRecord>> nud = null;
        if (nodeUserData != null) {
            nud = new Hashtable<>();
            for (Map.Entry<Node, Map<String, UserDataRecord>> e : nodeUserData.entrySet()) {
                //e.getValue() will not be null since an entry is always put with a non-null value
                nud.put(e.getKey(), new Hashtable<>(e.getValue()));
            }
        }

        Hashtable<String, Node> ids = (identifiers == null)? null : new Hashtable<>(identifiers);
        Hashtable<Node, Integer> nt = (nodeTable == null)? null : new Hashtable<>(nodeTable);

        // Write serialized fields
        ObjectOutputStream.PutField pf = out.putFields();
        pf.put("docType", docType);
        pf.put("docElement", docElement);
        pf.put("fFreeNLCache", fFreeNLCache);
        pf.put("encoding", encoding);
        pf.put("actualEncoding", actualEncoding);
        pf.put("version", version);
        pf.put("standalone", standalone);
        pf.put("fDocumentURI", fDocumentURI);

        //userData is the original name. It has been changed to nodeUserData, refer to the corrsponding @serialField
        pf.put("userData", nud);
        pf.put("identifiers", ids);
        pf.put("changes", changes);
        pf.put("allowGrammarAccess", allowGrammarAccess);
        pf.put("errorChecking", errorChecking);
        pf.put("ancestorChecking", ancestorChecking);
        pf.put("xmlVersionChanged", xmlVersionChanged);
        pf.put("documentNumber", documentNumber);
        pf.put("nodeCounter", nodeCounter);
        pf.put("nodeTable", nt);
        pf.put("xml11Version", xml11Version);
        out.writeFields();
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in)
                        throws IOException, ClassNotFoundException {
        // We have to read serialized fields first.
        ObjectInputStream.GetField gf = in.readFields();
        docType = (DocumentTypeImpl)gf.get("docType", null);
        docElement = (ElementImpl)gf.get("docElement", null);
        fFreeNLCache = (NodeListCache)gf.get("fFreeNLCache", null);
        encoding = (String)gf.get("encoding", null);
        actualEncoding = (String)gf.get("actualEncoding", null);
        version = (String)gf.get("version", null);
        standalone = gf.get("standalone", false);
        fDocumentURI = (String)gf.get("fDocumentURI", null);

        //userData is the original name. It has been changed to nodeUserData, refer to the corrsponding @serialField
        Hashtable<Node, Hashtable<String, UserDataRecord>> nud =
                (Hashtable<Node, Hashtable<String, UserDataRecord>>)gf.get("userData", null);

        Hashtable<String, Node> ids = (Hashtable<String, Node>)gf.get("identifiers", null);

        changes = gf.get("changes", 0);
        allowGrammarAccess = gf.get("allowGrammarAccess", false);
        errorChecking = gf.get("errorChecking", true);
        ancestorChecking = gf.get("ancestorChecking", true);
        xmlVersionChanged = gf.get("xmlVersionChanged", false);
        documentNumber = gf.get("documentNumber", 0);
        nodeCounter = gf.get("nodeCounter", 0);

        Hashtable<Node, Integer> nt = (Hashtable<Node, Integer>)gf.get("nodeTable", null);

        xml11Version = gf.get("xml11Version", false);

        //convert Hashtables back to HashMaps
        if (nud != null) {
            nodeUserData = new HashMap<>();
            for (Map.Entry<Node, Hashtable<String, UserDataRecord>> e : nud.entrySet()) {
                nodeUserData.put(e.getKey(), new HashMap<>(e.getValue()));
            }
        }

        if (ids != null) identifiers = new HashMap<>(ids);
        if (nt != null) nodeTable = new HashMap<>(nt);
    }
} // class CoreDocumentImpl
