/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.parsers;

import com.sun.org.apache.xerces.internal.dom.AttrImpl;
import com.sun.org.apache.xerces.internal.dom.CoreDocumentImpl;
import com.sun.org.apache.xerces.internal.dom.DOMErrorImpl;
import com.sun.org.apache.xerces.internal.dom.DOMMessageFormatter;
import com.sun.org.apache.xerces.internal.dom.DeferredDocumentImpl;
import com.sun.org.apache.xerces.internal.dom.DocumentImpl;
import com.sun.org.apache.xerces.internal.dom.DocumentTypeImpl;
import com.sun.org.apache.xerces.internal.dom.ElementDefinitionImpl;
import com.sun.org.apache.xerces.internal.dom.ElementImpl;
import com.sun.org.apache.xerces.internal.dom.ElementNSImpl;
import com.sun.org.apache.xerces.internal.dom.EntityImpl;
import com.sun.org.apache.xerces.internal.dom.EntityReferenceImpl;
import com.sun.org.apache.xerces.internal.dom.NodeImpl;
import com.sun.org.apache.xerces.internal.dom.NotationImpl;
import com.sun.org.apache.xerces.internal.dom.PSVIAttrNSImpl;
import com.sun.org.apache.xerces.internal.dom.PSVIDocumentImpl;
import com.sun.org.apache.xerces.internal.dom.PSVIElementNSImpl;
import com.sun.org.apache.xerces.internal.dom.TextImpl;
import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import com.sun.org.apache.xerces.internal.util.DOMErrorHandlerWrapper;
import com.sun.org.apache.xerces.internal.utils.ObjectFactory;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParserConfiguration;
import com.sun.org.apache.xerces.internal.xs.AttributePSVI;
import com.sun.org.apache.xerces.internal.xs.ElementPSVI;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import java.util.Locale;
import java.util.Stack;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMError;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.ls.LSParserFilter;
import org.w3c.dom.traversal.NodeFilter;
import org.xml.sax.SAXException;

/**
 * This is the base class of all DOM parsers. It implements the XNI
 * callback methods to create the DOM tree. After a successful parse of
 * an XML document, the DOM Document object can be queried using the
 * <code>getDocument</code> method. The actual pipeline is defined in
 * parser configuration.
 *
 * @author Arnaud Le Hors, IBM
 * @author Andy Clark, IBM
 * @author Elena Litani, IBM
 *
 * @LastModified: Jan 2019
 */
public class AbstractDOMParser extends AbstractXMLDocumentParser {

    //
    // Constants
    //

    // feature ids

    /** Feature id: namespace. */
    protected static final String NAMESPACES =
    Constants.SAX_FEATURE_PREFIX+Constants.NAMESPACES_FEATURE;

    /** Feature id: create entity ref nodes. */
    protected static final String CREATE_ENTITY_REF_NODES =
    Constants.XERCES_FEATURE_PREFIX + Constants.CREATE_ENTITY_REF_NODES_FEATURE;

    /** Feature id: include comments. */
    protected static final String INCLUDE_COMMENTS_FEATURE =
    Constants.XERCES_FEATURE_PREFIX + Constants.INCLUDE_COMMENTS_FEATURE;

    /** Feature id: create cdata nodes. */
    protected static final String CREATE_CDATA_NODES_FEATURE =
    Constants.XERCES_FEATURE_PREFIX + Constants.CREATE_CDATA_NODES_FEATURE;

    /** Feature id: include ignorable whitespace. */
    protected static final String INCLUDE_IGNORABLE_WHITESPACE =
    Constants.XERCES_FEATURE_PREFIX + Constants.INCLUDE_IGNORABLE_WHITESPACE;

    /** Feature id: defer node expansion. */
    protected static final String DEFER_NODE_EXPANSION =
    Constants.XERCES_FEATURE_PREFIX + Constants.DEFER_NODE_EXPANSION_FEATURE;


    /** Recognized features. */
    private static final String[] RECOGNIZED_FEATURES = {
        NAMESPACES,
        CREATE_ENTITY_REF_NODES,
        INCLUDE_COMMENTS_FEATURE,
        CREATE_CDATA_NODES_FEATURE,
        INCLUDE_IGNORABLE_WHITESPACE,
        DEFER_NODE_EXPANSION
    };

    // property ids

    /** Property id: document class name. */
    protected static final String DOCUMENT_CLASS_NAME =
    Constants.XERCES_PROPERTY_PREFIX + Constants.DOCUMENT_CLASS_NAME_PROPERTY;

    protected static final String  CURRENT_ELEMENT_NODE=
    Constants.XERCES_PROPERTY_PREFIX + Constants.CURRENT_ELEMENT_NODE_PROPERTY;

    // protected static final String GRAMMAR_POOL =
    // Constants.XERCES_PROPERTY_PREFIX + Constants.XMLGRAMMAR_POOL_PROPERTY;

    /** Recognized properties. */
    private static final String[] RECOGNIZED_PROPERTIES = {
        DOCUMENT_CLASS_NAME,
        CURRENT_ELEMENT_NODE,
    };

    // other

    /** Default document class name. */
    protected static final String DEFAULT_DOCUMENT_CLASS_NAME =
    "com.sun.org.apache.xerces.internal.dom.DocumentImpl";

    protected static final String CORE_DOCUMENT_CLASS_NAME =
    "com.sun.org.apache.xerces.internal.dom.CoreDocumentImpl";

    protected static final String PSVI_DOCUMENT_CLASS_NAME =
    "com.sun.org.apache.xerces.internal.dom.PSVIDocumentImpl";

    /**
     * If the user stops the process, this exception will be thrown.
     */
    static final class Abort extends RuntimeException {
        private static final long serialVersionUID = 1687848994976808490L;
        static final Abort INSTANCE = new Abort();
        private Abort() {}
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    // debugging

    private static final boolean DEBUG_EVENTS = false;
    private static final boolean DEBUG_BASEURI = false;

    //
    // Data
    //

    /** DOM L3 error handler */
    protected DOMErrorHandlerWrapper fErrorHandler = null;

    /** True if inside DTD. */
    protected boolean fInDTD;

    // features

    /** Create entity reference nodes. */
    protected boolean fCreateEntityRefNodes;

    /** Include ignorable whitespace. */
    protected boolean fIncludeIgnorableWhitespace;

    /** Include Comments. */
    protected boolean fIncludeComments;

    /** Create cdata nodes. */
    protected boolean fCreateCDATANodes;

    // dom information

    /** The document. */
    protected Document fDocument;

    /** The default Xerces document implementation, if used. */
    protected CoreDocumentImpl fDocumentImpl;

    /** Whether to store PSVI information in DOM tree. */
    protected boolean fStorePSVI;

    /** The document class name to use. */
    protected String  fDocumentClassName;

    /** The document type node. */
    protected DocumentType fDocumentType;

    /** Current node. */
    protected Node fCurrentNode;
    protected CDATASection fCurrentCDATASection;
    protected EntityImpl fCurrentEntityDecl;
    protected int fDeferredEntityDecl;

    /** Character buffer */
    protected final StringBuilder fStringBuilder = new StringBuilder (50);

    // internal subset

    /** Internal subset buffer. */
    protected StringBuilder fInternalSubset;

    // deferred expansion data

    protected boolean              fDeferNodeExpansion;
    protected boolean              fNamespaceAware;
    protected DeferredDocumentImpl fDeferredDocumentImpl;
    protected int                  fDocumentIndex;
    protected int                  fDocumentTypeIndex;
    protected int                  fCurrentNodeIndex;
    protected int                  fCurrentCDATASectionIndex;

    // state

    /** True if inside DTD external subset. */
    protected boolean fInDTDExternalSubset;

    /** Root element node. */
    protected Node fRoot;

    /** True if inside CDATA section. */
    protected boolean fInCDATASection;

    /** True if saw the first chunk of characters*/
    protected boolean fFirstChunk = false;


    /** LSParserFilter: specifies that element with given QNAME and all its children
     * must be rejected */
    protected boolean fFilterReject = false;

    // data

    /** Base uri stack*/
    protected final Stack<String> fBaseURIStack = new Stack<>();

    /** LSParserFilter: tracks the element depth within a rejected subtree. */
    protected int fRejectedElementDepth = 0;

    /** LSParserFilter: store depth of skipped elements */
    protected Stack<Boolean> fSkippedElemStack = null;

    /** LSParserFilter: true if inside entity reference */
    protected boolean fInEntityRef = false;

    /** Attribute QName. */
    private final QName fAttrQName = new QName();

    /** Document locator. */
    private XMLLocator fLocator;

    // handlers

    protected LSParserFilter fDOMFilter = null;

    //
    // Constructors
    //

    /** Default constructor. */
    protected AbstractDOMParser (XMLParserConfiguration config) {

        super (config);


        // add recognized features
        fConfiguration.addRecognizedFeatures (RECOGNIZED_FEATURES);

        // set default values
        fConfiguration.setFeature (CREATE_ENTITY_REF_NODES, true);
        fConfiguration.setFeature (INCLUDE_IGNORABLE_WHITESPACE, true);
        fConfiguration.setFeature (DEFER_NODE_EXPANSION, true);
        fConfiguration.setFeature (INCLUDE_COMMENTS_FEATURE, true);
        fConfiguration.setFeature (CREATE_CDATA_NODES_FEATURE, true);

        // add recognized properties
        fConfiguration.addRecognizedProperties (RECOGNIZED_PROPERTIES);

        // set default values
        fConfiguration.setProperty (DOCUMENT_CLASS_NAME,
        DEFAULT_DOCUMENT_CLASS_NAME);

    } // <init>(XMLParserConfiguration)

    /**
     * This method retrieves the name of current document class.
     */
    protected String getDocumentClassName () {
        return fDocumentClassName;
    }

    /**
     * This method allows the programmer to decide which document
     * factory to use when constructing the DOM tree. However, doing
     * so will lose the functionality of the default factory. Also,
     * a document class other than the default will lose the ability
     * to defer node expansion on the DOM tree produced.
     *
     * @param documentClassName The fully qualified class name of the
     *                      document factory to use when constructing
     *                      the DOM tree.
     *
     * @see #getDocumentClassName
     * @see #DEFAULT_DOCUMENT_CLASS_NAME
     */
    protected void setDocumentClassName (String documentClassName) {

        // normalize class name
        if (documentClassName == null) {
            documentClassName = DEFAULT_DOCUMENT_CLASS_NAME;
        }

        if (!documentClassName.equals(DEFAULT_DOCUMENT_CLASS_NAME) &&
            !documentClassName.equals(PSVI_DOCUMENT_CLASS_NAME)) {
            // verify that this class exists and is of the right type
            try {
                Class<?> _class = ObjectFactory.findProviderClass (documentClassName, true);
                //if (!_class.isAssignableFrom(Document.class)) {
                if (!Document.class.isAssignableFrom (_class)) {
                    throw new IllegalArgumentException (
                        DOMMessageFormatter.formatMessage(
                        DOMMessageFormatter.DOM_DOMAIN,
                        "InvalidDocumentClassName", new Object [] {documentClassName}));
                }
            }
            catch (ClassNotFoundException e) {
                throw new IllegalArgumentException (
                    DOMMessageFormatter.formatMessage(
                    DOMMessageFormatter.DOM_DOMAIN,
                    "MissingDocumentClassName", new Object [] {documentClassName}));
            }
        }

        // set document class name
        fDocumentClassName = documentClassName;
        if (!documentClassName.equals (DEFAULT_DOCUMENT_CLASS_NAME)) {
            fDeferNodeExpansion = false;
        }

    } // setDocumentClassName(String)

    //
    // Public methods
    //

    /** Returns the DOM document object. */
    public Document getDocument () {
        return fDocument;
    } // getDocument():Document

    /**
     * Drops all references to the last DOM which was built by this parser.
     */
    public final void dropDocumentReferences() {
        fDocument = null;
        fDocumentImpl = null;
        fDeferredDocumentImpl = null;
        fDocumentType = null;
        fCurrentNode = null;
        fCurrentCDATASection = null;
        fCurrentEntityDecl = null;
        fRoot = null;
    } // dropDocumentReferences()

    //
    // XMLDocumentParser methods
    //

    /**
     * Resets the parser state.
     *
     * @throws SAXException Thrown on initialization error.
     */
    public void reset () throws XNIException {
        super.reset ();


        // get feature state
        fCreateEntityRefNodes =
        fConfiguration.getFeature (CREATE_ENTITY_REF_NODES);

        fIncludeIgnorableWhitespace =
        fConfiguration.getFeature (INCLUDE_IGNORABLE_WHITESPACE);

        fDeferNodeExpansion =
        fConfiguration.getFeature (DEFER_NODE_EXPANSION);

        fNamespaceAware = fConfiguration.getFeature (NAMESPACES);

        fIncludeComments = fConfiguration.getFeature (INCLUDE_COMMENTS_FEATURE);

        fCreateCDATANodes = fConfiguration.getFeature (CREATE_CDATA_NODES_FEATURE);

        // get property
        setDocumentClassName ((String)
        fConfiguration.getProperty (DOCUMENT_CLASS_NAME));

        // reset dom information
        fDocument = null;
        fDocumentImpl = null;
        fStorePSVI = false;
        fDocumentType = null;
        fDocumentTypeIndex = -1;
        fDeferredDocumentImpl = null;
        fCurrentNode = null;

        // reset string buffer
        fStringBuilder.setLength (0);

        // reset state information
        fRoot = null;
        fInDTD = false;
        fInDTDExternalSubset = false;
        fInCDATASection = false;
        fFirstChunk = false;
        fCurrentCDATASection = null;
        fCurrentCDATASectionIndex = -1;

        fBaseURIStack.removeAllElements ();


    } // reset()

    /**
     * Set the locale to use for messages.
     *
     * @param locale The locale object to use for localization of messages.
     *
     */
    public void setLocale (Locale locale) {
        fConfiguration.setLocale (locale);

    } // setLocale(Locale)

    //
    // XMLDocumentHandler methods
    //

    /**
     * This method notifies the start of a general entity.
     * <p>
     * <strong>Note:</strong> This method is not called for entity references
     * appearing as part of attribute values.
     *
     * @param name     The name of the general entity.
     * @param identifier The resource identifier.
     * @param encoding The auto-detected IANA encoding name of the entity
     *                 stream. This value will be null in those situations
     *                 where the entity encoding is not auto-detected (e.g.
     *                 internal entities or a document entity that is
     *                 parsed from a java.io.Reader).
     * @param augs     Additional information that may include infoset augmentations
     *
     * @exception XNIException Thrown by handler to signal an error.
     */
    public void startGeneralEntity (String name,
    XMLResourceIdentifier identifier,
    String encoding, Augmentations augs)
    throws XNIException {
        if (DEBUG_EVENTS) {
            System.out.println ("==>startGeneralEntity ("+name+")");
            if (DEBUG_BASEURI) {
                System.out.println ("   expandedSystemId( **baseURI): " +
                        identifier == null ? null : identifier.getExpandedSystemId());
                System.out.println ("   baseURI:" +
                        identifier == null ? null : identifier.getBaseSystemId());
            }
        }

        // Always create entity reference nodes to be able to recreate
        // entity as a part of doctype
        if (!fDeferNodeExpansion) {
            if (fFilterReject) {
                return;
            }
            setCharacterData (true);
            EntityReference er = fDocument.createEntityReference (name);
            if (fDocumentImpl != null) {
                // REVISIT: baseURI/actualEncoding
                //         remove dependency on our implementation when DOM L3 is REC
                //

                EntityReferenceImpl erImpl =(EntityReferenceImpl)er;

                // set base uri
                erImpl.setBaseURI (identifier == null ? null : identifier.getExpandedSystemId());
                if (fDocumentType != null) {
                    // set actual encoding
                    NamedNodeMap entities = fDocumentType.getEntities ();
                    fCurrentEntityDecl = (EntityImpl) entities.getNamedItem (name);
                    if (fCurrentEntityDecl != null) {
                        fCurrentEntityDecl.setInputEncoding (encoding);
                    }

                }
                // we don't need synchronization now, because entity ref will be
                // expanded anyway. Synch only needed when user creates entityRef node
                erImpl.needsSyncChildren (false);
            }
            fInEntityRef = true;
            fCurrentNode.appendChild (er);

            if (!fCreateEntityRefNodes) {
                fCurrentNode = er;
            } else {
                ((NodeImpl)er).setReadOnly (true, true);
            }
        }
        else {

            int er = fDeferredDocumentImpl.createDeferredEntityReference (name,
                    identifier == null ? null : identifier.getExpandedSystemId ());
            if (fDocumentTypeIndex != -1) {
                // find corresponding Entity decl
                int node = fDeferredDocumentImpl.getLastChild (fDocumentTypeIndex, false);
                while (node != -1) {
                    short nodeType = fDeferredDocumentImpl.getNodeType (node, false);
                    if (nodeType == Node.ENTITY_NODE) {
                        String nodeName =
                        fDeferredDocumentImpl.getNodeName (node, false);
                        if (nodeName.equals (name)) {
                            fDeferredEntityDecl = node;
                            fDeferredDocumentImpl.setInputEncoding (node, encoding);
                            break;
                        }
                    }
                    node = fDeferredDocumentImpl.getRealPrevSibling (node, false);
                }
            }
            fDeferredDocumentImpl.appendChild (fCurrentNodeIndex, er);

            if (!fCreateEntityRefNodes) {
                fCurrentNodeIndex = er;
            }
        }

    } // startGeneralEntity(String,XMLResourceIdentifier, Augmentations)

    /**
     * Notifies of the presence of a TextDecl line in an entity. If present,
     * this method will be called immediately following the startEntity call.
     * <p>
     * <strong>Note:</strong> This method will never be called for the
     * document entity; it is only called for external general entities
     * referenced in document content.
     * <p>
     * <strong>Note:</strong> This method is not called for entity references
     * appearing as part of attribute values.
     *
     * @param version  The XML version, or null if not specified.
     * @param encoding The IANA encoding name of the entity.
     * @param augs       Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void textDecl (String version, String encoding, Augmentations augs) throws XNIException {
        if (fInDTD){
            return;
        }
        if (!fDeferNodeExpansion) {
            if (fCurrentEntityDecl != null && !fFilterReject) {
                fCurrentEntityDecl.setXmlEncoding (encoding);
                if (version != null)
                    fCurrentEntityDecl.setXmlVersion (version);
            }
        }
        else {
            if (fDeferredEntityDecl !=-1) {
                fDeferredDocumentImpl.setEntityInfo (fDeferredEntityDecl, version, encoding);
            }
        }
    } // textDecl(String,String)

    /**
     * A comment.
     *
     * @param text The text in the comment.
     * @param augs       Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by application to signal an error.
     */
    @SuppressWarnings("fallthrough") // by design at case LSParserFilter.FILTER_SKIP
    public void comment (XMLString text, Augmentations augs) throws XNIException {
        if (fInDTD) {
            if (fInternalSubset != null && !fInDTDExternalSubset) {
                fInternalSubset.append ("<!--");
                if (text.length > 0) {
                    fInternalSubset.append (text.ch, text.offset, text.length);
                }
                fInternalSubset.append ("-->");
            }
            return;
        }
        if (!fIncludeComments || fFilterReject) {
            return;
        }
        if (!fDeferNodeExpansion) {
            Comment comment = fDocument.createComment (text.toString ());

            setCharacterData (false);
            fCurrentNode.appendChild (comment);
            if (fDOMFilter !=null && !fInEntityRef &&
            (fDOMFilter.getWhatToShow () & NodeFilter.SHOW_COMMENT)!= 0) {
                short code = fDOMFilter.acceptNode (comment);
                switch (code) {
                    case LSParserFilter.FILTER_INTERRUPT:{
                        throw Abort.INSTANCE;
                    }
                    case LSParserFilter.FILTER_REJECT:{
                        // REVISIT: the constant FILTER_REJECT should be changed when new
                        // DOM LS specs gets published

                        // fall through to SKIP since comment has no children.
                    }
                    case LSParserFilter.FILTER_SKIP: {
                        // REVISIT: the constant FILTER_SKIP should be changed when new
                        // DOM LS specs gets published
                        fCurrentNode.removeChild (comment);
                        // make sure we don't loose chars if next event is characters()
                        fFirstChunk = true;
                        return;
                    }

                    default: {
                        // accept node
                    }
                }
            }

        }
        else {
            int comment =
            fDeferredDocumentImpl.createDeferredComment (text.toString ());
            fDeferredDocumentImpl.appendChild (fCurrentNodeIndex, comment);
        }

    } // comment(XMLString)

    /**
     * A processing instruction. Processing instructions consist of a
     * target name and, optionally, text data. The data is only meaningful
     * to the application.
     * <p>
     * Typically, a processing instruction's data will contain a series
     * of pseudo-attributes. These pseudo-attributes follow the form of
     * element attributes but are <strong>not</strong> parsed or presented
     * to the application as anything other than text. The application is
     * responsible for parsing the data.
     *
     * @param target The target.
     * @param data   The data or null if none specified.
     * @param augs       Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    @SuppressWarnings("fallthrough") // by design at case LSParserFilter.FILTER_REJECT
    public void processingInstruction (String target, XMLString data, Augmentations augs)
    throws XNIException {

        if (fInDTD) {
            if (fInternalSubset != null && !fInDTDExternalSubset) {
                fInternalSubset.append ("<?");
                fInternalSubset.append (target);
                if (data.length > 0) {
                    fInternalSubset.append (' ').append (data.ch, data.offset, data.length);
                }
                fInternalSubset.append ("?>");
            }
            return;
        }

        if (DEBUG_EVENTS) {
            System.out.println ("==>processingInstruction ("+target+")");
        }
        if (!fDeferNodeExpansion) {
            if (fFilterReject) {
                return;
            }
            ProcessingInstruction pi =
            fDocument.createProcessingInstruction (target, data.toString ());


            setCharacterData (false);
            fCurrentNode.appendChild (pi);
            if (fDOMFilter !=null && !fInEntityRef &&
            (fDOMFilter.getWhatToShow () & NodeFilter.SHOW_PROCESSING_INSTRUCTION)!= 0) {
                short code = fDOMFilter.acceptNode (pi);
                switch (code) {
                    case LSParserFilter.FILTER_INTERRUPT:{
                        throw Abort.INSTANCE;
                    }
                    case LSParserFilter.FILTER_REJECT:{
                        // fall through to SKIP since PI has no children.
                    }
                    case LSParserFilter.FILTER_SKIP: {
                        fCurrentNode.removeChild (pi);
                        // fFirstChunk must be set to true so that data
                        // won't be lost in the case where the child before PI is
                        // a text node and the next event is characters.
                        fFirstChunk = true;
                        return;
                    }
                    default: {
                    }
                }
            }
        }
        else {
            int pi = fDeferredDocumentImpl.
            createDeferredProcessingInstruction (target, data.toString ());
            fDeferredDocumentImpl.appendChild (fCurrentNodeIndex, pi);
        }

    } // processingInstruction(String,XMLString)

    /**
     * The start of the document.
     *
     * @param locator The system identifier of the entity if the entity
     *                 is external, null otherwise.
     * @param encoding The auto-detected IANA encoding name of the entity
     *                 stream. This value will be null in those situations
     *                 where the entity encoding is not auto-detected (e.g.
     *                 internal entities or a document entity that is
     *                 parsed from a java.io.Reader).
     * @param namespaceContext
     *                 The namespace context in effect at the
     *                 start of this document.
     *                 This object represents the current context.
     *                 Implementors of this class are responsible
     *                 for copying the namespace bindings from the
     *                 the current context (and its parent contexts)
     *                 if that information is important.
     * @param augs     Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startDocument (XMLLocator locator, String encoding,
    NamespaceContext namespaceContext, Augmentations augs)
    throws XNIException {

        fLocator = locator;
        if (!fDeferNodeExpansion) {
            if (fDocumentClassName.equals (DEFAULT_DOCUMENT_CLASS_NAME)) {
                fDocument = new DocumentImpl ();
                fDocumentImpl = (CoreDocumentImpl)fDocument;
                // REVISIT: when DOM Level 3 is REC rely on Document.support
                //          instead of specific class
                // set DOM error checking off
                fDocumentImpl.setStrictErrorChecking (false);
                // set actual encoding
                fDocumentImpl.setInputEncoding (encoding);
                // set documentURI
                fDocumentImpl.setDocumentURI (locator.getExpandedSystemId ());
            }
            else if (fDocumentClassName.equals (PSVI_DOCUMENT_CLASS_NAME)) {
                fDocument = new PSVIDocumentImpl();
                fDocumentImpl = (CoreDocumentImpl)fDocument;
                fStorePSVI = true;
                // REVISIT: when DOM Level 3 is REC rely on Document.support
                //          instead of specific class
                // set DOM error checking off
                fDocumentImpl.setStrictErrorChecking (false);
                // set actual encoding
                fDocumentImpl.setInputEncoding (encoding);
                // set documentURI
                fDocumentImpl.setDocumentURI (locator.getExpandedSystemId ());
            }
            else {
                // use specified document class
                try {
                    Class<?> documentClass = ObjectFactory.findProviderClass (fDocumentClassName, true);
                    fDocument = (Document)documentClass.getConstructor().newInstance();

                    // if subclass of our own class that's cool too
                    Class<?> defaultDocClass =
                    ObjectFactory.findProviderClass (CORE_DOCUMENT_CLASS_NAME, true);
                    if (defaultDocClass.isAssignableFrom (documentClass)) {
                        fDocumentImpl = (CoreDocumentImpl)fDocument;

                        Class<?> psviDocClass = ObjectFactory.findProviderClass (PSVI_DOCUMENT_CLASS_NAME, true);
                        if (psviDocClass.isAssignableFrom (documentClass)) {
                            fStorePSVI = true;
                        }

                        // REVISIT: when DOM Level 3 is REC rely on
                        //          Document.support instead of specific class
                        // set DOM error checking off
                        fDocumentImpl.setStrictErrorChecking (false);
                        // set actual encoding
                        fDocumentImpl.setInputEncoding (encoding);
                        // set documentURI
                        if (locator != null) {
                            fDocumentImpl.setDocumentURI (locator.getExpandedSystemId ());
                        }
                    }
                }
                catch (ClassNotFoundException e) {
                    // won't happen we already checked that earlier
                }
                catch (Exception e) {
                    throw new RuntimeException (
                        DOMMessageFormatter.formatMessage(
                        DOMMessageFormatter.DOM_DOMAIN,
                        "CannotCreateDocumentClass",
                        new Object [] {fDocumentClassName}));
                }
            }
            fCurrentNode = fDocument;
        }
        else {
            fDeferredDocumentImpl = new DeferredDocumentImpl (fNamespaceAware);
            fDocument = fDeferredDocumentImpl;
            fDocumentIndex = fDeferredDocumentImpl.createDeferredDocument ();
            // REVISIT: strict error checking is not implemented in deferred dom.
            //          Document.support instead of specific class

            // set actual encoding
            fDeferredDocumentImpl.setInputEncoding (encoding);
            // set documentURI
            fDeferredDocumentImpl.setDocumentURI (locator.getExpandedSystemId ());
            fCurrentNodeIndex = fDocumentIndex;

        }

    } // startDocument(String,String)

    /**
     * Notifies of the presence of an XMLDecl line in the document. If
     * present, this method will be called immediately following the
     * startDocument call.
     *
     * @param version    The XML version.
     * @param encoding   The IANA encoding name of the document, or null if
     *                   not specified.
     * @param standalone The standalone value, or null if not specified.
     * @param augs       Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void xmlDecl (String version, String encoding, String standalone,
    Augmentations augs)
    throws XNIException {
        if (!fDeferNodeExpansion) {
            // REVISIT: when DOM Level 3 is REC rely on Document.support
            //          instead of specific class
            if (fDocumentImpl != null) {
                if (version != null)
                    fDocumentImpl.setXmlVersion (version);
                fDocumentImpl.setXmlEncoding (encoding);
                fDocumentImpl.setXmlStandalone ("yes".equals (standalone));
            }
        }
        else {
            if (version != null)
                fDeferredDocumentImpl.setXmlVersion (version);
            fDeferredDocumentImpl.setXmlEncoding (encoding);
            fDeferredDocumentImpl.setXmlStandalone ("yes".equals (standalone));
        }
    } // xmlDecl(String,String,String)

    /**
     * Notifies of the presence of the DOCTYPE line in the document.
     *
     * @param rootElement The name of the root element.
     * @param publicId    The public identifier if an external DTD or null
     *                    if the external DTD is specified using SYSTEM.
     * @param systemId    The system identifier if an external DTD, null
     *                    otherwise.
     * @param augs     Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void doctypeDecl (String rootElement,
    String publicId, String systemId, Augmentations augs)
    throws XNIException {

        if (!fDeferNodeExpansion) {
            if (fDocumentImpl != null) {
                fDocumentType = fDocumentImpl.createDocumentType (
                rootElement, publicId, systemId);
                fCurrentNode.appendChild (fDocumentType);
            }
        }
        else {
            fDocumentTypeIndex = fDeferredDocumentImpl.
            createDeferredDocumentType (rootElement, publicId, systemId);
            fDeferredDocumentImpl.appendChild (fCurrentNodeIndex, fDocumentTypeIndex);
        }

    } // doctypeDecl(String,String,String)

    /**
     * The start of an element. If the document specifies the start element
     * by using an empty tag, then the startElement method will immediately
     * be followed by the endElement method, with no intervening methods.
     *
     * @param element    The name of the element.
     * @param attributes The element attributes.
     * @param augs     Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startElement (QName element, XMLAttributes attributes, Augmentations augs)
    throws XNIException {
        if (DEBUG_EVENTS) {
            System.out.println ("==>startElement ("+element.rawname+")");
        }
        if (!fDeferNodeExpansion) {
            if (fFilterReject) {
                ++fRejectedElementDepth;
                return;
            }
            Element el = createElementNode (element);
            int attrCount = attributes.getLength ();
            boolean seenSchemaDefault = false;
            for (int i = 0; i < attrCount; i++) {
                attributes.getName (i, fAttrQName);
                Attr attr = createAttrNode (fAttrQName);

                String attrValue = attributes.getValue (i);

                AttributePSVI attrPSVI =(AttributePSVI) attributes.getAugmentations (i).getItem (Constants.ATTRIBUTE_PSVI);
                if (fStorePSVI && attrPSVI != null){
                    ((PSVIAttrNSImpl) attr).setPSVI (attrPSVI);
                }

                attr.setValue (attrValue);
                boolean specified = attributes.isSpecified(i);
                // Take special care of schema defaulted attributes. Calling the
                // non-namespace aware setAttributeNode() method could overwrite
                // another attribute with the same local name.
                if (!specified && (seenSchemaDefault || (fAttrQName.uri != null &&
                    fAttrQName.uri != NamespaceContext.XMLNS_URI && fAttrQName.prefix == null))) {
                    el.setAttributeNodeNS(attr);
                    seenSchemaDefault = true;
                }
                else {
                    el.setAttributeNode(attr);
                }
                // NOTE: The specified value MUST be set after you set
                //       the node value because that turns the "specified"
                //       flag to "true" which may overwrite a "false"
                //       value from the attribute list. -Ac
                if (fDocumentImpl != null) {
                    AttrImpl attrImpl = (AttrImpl) attr;
                    Object type = null;
                    boolean id = false;

                    // REVISIT: currently it is possible that someone turns off
                    // namespaces and turns on xml schema validation
                    // To avoid classcast exception in AttrImpl check for namespaces
                    // however the correct solution should probably disallow setting
                    // namespaces to false when schema processing is turned on.
                    if (attrPSVI != null && fNamespaceAware) {
                        // XML Schema
                        type = attrPSVI.getMemberTypeDefinition ();
                        if (type == null) {
                            type = attrPSVI.getTypeDefinition ();
                            if (type != null) {
                                id = ((XSSimpleType) type).isIDType ();
                                attrImpl.setType (type);
                            }
                        }
                        else {
                            id = ((XSSimpleType) type).isIDType ();
                            attrImpl.setType (type);
                        }
                    }
                    else {
                        // DTD
                        boolean isDeclared = Boolean.TRUE.equals (attributes.getAugmentations (i).getItem (Constants.ATTRIBUTE_DECLARED));
                        // For DOM Level 3 TypeInfo, the type name must
                        // be null if this attribute has not been declared
                        // in the DTD.
                        if (isDeclared) {
                            type = attributes.getType (i);
                            id = "ID".equals (type);
                        }
                        attrImpl.setType (type);
                    }

                    if (id) {
                        ((ElementImpl) el).setIdAttributeNode (attr, true);
                    }

                    attrImpl.setSpecified (specified);
                    // REVISIT: Handle entities in attribute value.
                }
            }
            setCharacterData (false);

            if (augs != null) {
                ElementPSVI elementPSVI = (ElementPSVI)augs.getItem (Constants.ELEMENT_PSVI);
                if (elementPSVI != null && fNamespaceAware) {
                    XSTypeDefinition type = elementPSVI.getMemberTypeDefinition ();
                    if (type == null) {
                        type = elementPSVI.getTypeDefinition ();
                    }
                    ((ElementNSImpl)el).setType (type);
                }
            }


            // filter nodes
            if (fDOMFilter != null && !fInEntityRef) {
                if (fRoot == null) {
                    // fill value of the root element
                    fRoot = el;
                } else {
                    short code = fDOMFilter.startElement(el);
                    switch (code) {
                        case LSParserFilter.FILTER_INTERRUPT :
                            {
                                throw Abort.INSTANCE;
                            }
                        case LSParserFilter.FILTER_REJECT :
                            {
                                fFilterReject = true;
                                fRejectedElementDepth = 0;
                                return;
                            }
                        case LSParserFilter.FILTER_SKIP :
                            {
                                // make sure that if any char data is available
                                // the fFirstChunk is true, so that if the next event
                                // is characters(), and the last node is text, we will copy
                                // the value already in the text node to fStringBuffer
                                // (not to lose it).
                                fFirstChunk = true;
                                fSkippedElemStack.push(Boolean.TRUE);
                                return;
                            }
                        default :
                            {
                                if (!fSkippedElemStack.isEmpty()) {
                                    fSkippedElemStack.push(Boolean.FALSE);
                                }
                            }
                    }
                }
            }
            fCurrentNode.appendChild (el);
            fCurrentNode = el;
        }
        else {
            int el = fDeferredDocumentImpl.createDeferredElement (fNamespaceAware ?
                    element.uri : null, element.rawname);
            Object type = null;
            int attrCount = attributes.getLength ();
            // Need to loop in reverse order so that the attributes
            // are processed in document order when the DOM is expanded.
            for (int i = attrCount - 1; i >= 0; --i) {

                // set type information
                AttributePSVI attrPSVI = (AttributePSVI)attributes.getAugmentations (i).getItem (Constants.ATTRIBUTE_PSVI);
                boolean id = false;

                // REVISIT: currently it is possible that someone turns off
                // namespaces and turns on xml schema validation
                // To avoid classcast exception in AttrImpl check for namespaces
                // however the correct solution should probably disallow setting
                // namespaces to false when schema processing is turned on.
                if (attrPSVI != null && fNamespaceAware) {
                    // XML Schema
                    type = attrPSVI.getMemberTypeDefinition ();
                    if (type == null) {
                        type = attrPSVI.getTypeDefinition ();
                        if (type != null){
                            id = ((XSSimpleType) type).isIDType ();
                        }
                    }
                    else {
                        id = ((XSSimpleType) type).isIDType ();
                    }
                }
                else {
                    // DTD
                    boolean isDeclared = Boolean.TRUE.equals (attributes.getAugmentations (i).getItem (Constants.ATTRIBUTE_DECLARED));
                    // For DOM Level 3 TypeInfo, the type name must
                    // be null if this attribute has not been declared
                    // in the DTD.
                    if (isDeclared) {
                        type = attributes.getType (i);
                        id = "ID".equals (type);
                    }
                }

                // create attribute
                fDeferredDocumentImpl.setDeferredAttribute (
                el,
                attributes.getQName (i),
                attributes.getURI (i),
                attributes.getValue (i),
                attributes.isSpecified (i),
                id,
                type);
            }

            fDeferredDocumentImpl.appendChild (fCurrentNodeIndex, el);
            fCurrentNodeIndex = el;
        }
    } // startElement(QName,XMLAttributes)


    /**
     * An empty element.
     *
     * @param element    The name of the element.
     * @param attributes The element attributes.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void emptyElement (QName element, XMLAttributes attributes, Augmentations augs)
    throws XNIException {

        startElement (element, attributes, augs);
        endElement (element, augs);

    } // emptyElement(QName,XMLAttributes)

    /**
     * Character content.
     *
     * @param text The content.
     * @param augs     Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void characters (XMLString text, Augmentations augs) throws XNIException {

        if (DEBUG_EVENTS) {
            System.out.println ("==>characters(): "+text.toString ());
        }

        if (!fDeferNodeExpansion) {

            if (fFilterReject) {
                return;
            }
            if (fInCDATASection && fCreateCDATANodes) {
                if (fCurrentCDATASection == null) {
                    fCurrentCDATASection =
                    fDocument.createCDATASection (text.toString ());
                    fCurrentNode.appendChild (fCurrentCDATASection);
                    fCurrentNode = fCurrentCDATASection;
                }
                else {
                    fCurrentCDATASection.appendData (text.toString ());
                }
            }
            else if (!fInDTD) {
                // if type is union (XML Schema) it is possible that we receive
                // character call with empty data
                if (text.length == 0) {
                    return;
                }

                Node child = fCurrentNode.getLastChild ();
                if (child != null && child.getNodeType () == Node.TEXT_NODE) {
                    // collect all the data into the string buffer.
                    if (fFirstChunk) {
                        if (fDocumentImpl != null) {
                            fStringBuilder.append (((TextImpl)child).removeData ());
                        } else {
                            fStringBuilder.append (((Text)child).getData ());
                            ((Text)child).setNodeValue (null);
                        }
                        fFirstChunk = false;
                    }
                    if (text.length > 0) {
                        fStringBuilder.append (text.ch, text.offset, text.length);
                    }
                }
                else {
                    fFirstChunk = true;
                    Text textNode = fDocument.createTextNode (text.toString());
                    fCurrentNode.appendChild (textNode);
                }

            }
        }
        else {
            // The Text and CDATASection normalization is taken care of within
            // the DOM in the deferred case.
            if (fInCDATASection && fCreateCDATANodes) {
                if (fCurrentCDATASectionIndex == -1) {
                    int cs = fDeferredDocumentImpl.
                    createDeferredCDATASection (text.toString ());

                    fDeferredDocumentImpl.appendChild (fCurrentNodeIndex, cs);
                    fCurrentCDATASectionIndex = cs;
                    fCurrentNodeIndex = cs;
                }
                else {
                    int txt = fDeferredDocumentImpl.
                    createDeferredTextNode (text.toString (), false);
                    fDeferredDocumentImpl.appendChild (fCurrentNodeIndex, txt);
                }
            } else if (!fInDTD) {
                // if type is union (XML Schema) it is possible that we receive
                // character call with empty data
                if (text.length == 0) {
                    return;
                }

                String value = text.toString ();
                int txt = fDeferredDocumentImpl.
                createDeferredTextNode (value, false);
                fDeferredDocumentImpl.appendChild (fCurrentNodeIndex, txt);

            }
        }
    } // characters(XMLString)

    /**
     * Ignorable whitespace. For this method to be called, the document
     * source must have some way of determining that the text containing
     * only whitespace characters should be considered ignorable. For
     * example, the validator can determine if a length of whitespace
     * characters in the document are ignorable based on the element
     * content model.
     *
     * @param text The ignorable whitespace.
     * @param augs     Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void ignorableWhitespace (XMLString text, Augmentations augs) throws XNIException {

        if (!fIncludeIgnorableWhitespace || fFilterReject) {
            return;
        }
        if (!fDeferNodeExpansion) {
            Node child = fCurrentNode.getLastChild ();
            if (child != null && child.getNodeType () == Node.TEXT_NODE) {
                Text textNode = (Text)child;
                textNode.appendData (text.toString ());
            }
            else {
                Text textNode = fDocument.createTextNode (text.toString ());
                if (fDocumentImpl != null) {
                    TextImpl textNodeImpl = (TextImpl)textNode;
                    textNodeImpl.setIgnorableWhitespace (true);
                }
                fCurrentNode.appendChild (textNode);
            }
        }
        else {
            // The Text normalization is taken care of within the DOM in the
            // deferred case.
            int txt = fDeferredDocumentImpl.
            createDeferredTextNode (text.toString (), true);
            fDeferredDocumentImpl.appendChild (fCurrentNodeIndex, txt);
        }

    } // ignorableWhitespace(XMLString)

    /**
     * The end of an element.
     *
     * @param element The name of the element.
     * @param augs     Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endElement (QName element, Augmentations augs) throws XNIException {
        if (DEBUG_EVENTS) {
            System.out.println ("==>endElement ("+element.rawname+")");
        }
        if (!fDeferNodeExpansion) {

            // REVISIT: Should this happen after we call the filter?
            if (augs != null && fDocumentImpl != null && (fNamespaceAware || fStorePSVI)) {
                ElementPSVI elementPSVI = (ElementPSVI) augs.getItem(Constants.ELEMENT_PSVI);
                if (elementPSVI != null) {
                    // Updating TypeInfo. If the declared type is a union the
                    // [member type definition] will only be available at the
                    // end of an element.
                    if (fNamespaceAware) {
                        XSTypeDefinition type = elementPSVI.getMemberTypeDefinition();
                        if (type == null) {
                            type = elementPSVI.getTypeDefinition();
                        }
                        ((ElementNSImpl)fCurrentNode).setType(type);
                    }
                    if (fStorePSVI) {
                        ((PSVIElementNSImpl)fCurrentNode).setPSVI (elementPSVI);
                    }
                }
            }

            if (fDOMFilter != null) {
                if (fFilterReject) {
                    if (fRejectedElementDepth-- == 0) {
                        fFilterReject = false;
                    }
                    return;
                }
                if (!fSkippedElemStack.isEmpty()) {
                    if (fSkippedElemStack.pop() == Boolean.TRUE) {
                        return;
                    }
                }
                setCharacterData (false);
                if ((fCurrentNode != fRoot) && !fInEntityRef && (fDOMFilter.getWhatToShow () & NodeFilter.SHOW_ELEMENT)!=0) {
                    short code = fDOMFilter.acceptNode (fCurrentNode);
                    switch (code) {
                        case LSParserFilter.FILTER_INTERRUPT:{
                            throw Abort.INSTANCE;
                        }
                        case LSParserFilter.FILTER_REJECT:{
                            Node parent = fCurrentNode.getParentNode ();
                            parent.removeChild (fCurrentNode);
                            fCurrentNode = parent;
                            return;
                        }
                        case LSParserFilter.FILTER_SKIP: {
                            // make sure that if any char data is available
                            // the fFirstChunk is true, so that if the next event
                            // is characters(), and the last node is text, we will copy
                            // the value already in the text node to fStringBuffer
                            // (not to lose it).
                            fFirstChunk = true;

                            // replace children
                            Node parent = fCurrentNode.getParentNode ();
                            NodeList ls = fCurrentNode.getChildNodes ();
                            int length = ls.getLength ();

                            for (int i=0;i<length;i++) {
                                parent.appendChild (ls.item (0));
                            }
                            parent.removeChild (fCurrentNode);
                            fCurrentNode = parent;

                            return;
                        }

                        default: { }
                    }
                }
                fCurrentNode = fCurrentNode.getParentNode ();

            } // end-if DOMFilter
            else {
                setCharacterData (false);
                fCurrentNode = fCurrentNode.getParentNode ();
            }

        }
        else {
            if (augs != null) {
                ElementPSVI elementPSVI = (ElementPSVI) augs.getItem(Constants.ELEMENT_PSVI);
                if (elementPSVI != null) {
                    // Setting TypeInfo. If the declared type is a union the
                    // [member type definition] will only be available at the
                    // end of an element.
                    XSTypeDefinition type = elementPSVI.getMemberTypeDefinition();
                    if (type == null) {
                        type = elementPSVI.getTypeDefinition();
                    }
                    fDeferredDocumentImpl.setTypeInfo(fCurrentNodeIndex, type);
                }
            }
            fCurrentNodeIndex =
                fDeferredDocumentImpl.getParentNode (fCurrentNodeIndex, false);
        }


    } // endElement(QName)


    /**
     * The start of a CDATA section.
     * @param augs     Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startCDATA (Augmentations augs) throws XNIException {

        fInCDATASection = true;
        if (!fDeferNodeExpansion) {
            if (fFilterReject) {
                return;
            }
            if (fCreateCDATANodes) {
                setCharacterData (false);
            }
        }
    } // startCDATA()

    /**
     * The end of a CDATA section.
     * @param augs     Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    @SuppressWarnings("fallthrough") // by design at case LSParserFilter.FILTER_REJECT
    public void endCDATA (Augmentations augs) throws XNIException {

        fInCDATASection = false;
        if (!fDeferNodeExpansion) {

            if (fFilterReject) {
                return;
            }

            if (fCurrentCDATASection !=null) {

                if (fDOMFilter !=null && !fInEntityRef &&
                (fDOMFilter.getWhatToShow () & NodeFilter.SHOW_CDATA_SECTION)!= 0) {
                    short code = fDOMFilter.acceptNode (fCurrentCDATASection);
                    switch (code) {
                        case LSParserFilter.FILTER_INTERRUPT:{
                            throw Abort.INSTANCE;
                        }
                        case LSParserFilter.FILTER_REJECT:{
                            // fall through to SKIP since CDATA section has no children.
                        }
                        case LSParserFilter.FILTER_SKIP: {
                            Node parent = fCurrentNode.getParentNode ();
                            parent.removeChild (fCurrentCDATASection);
                            fCurrentNode = parent;
                            return;
                        }

                        default: {
                            // accept node
                        }
                    }
                }

                fCurrentNode = fCurrentNode.getParentNode ();
                fCurrentCDATASection = null;
            }
        }
        else {
            if (fCurrentCDATASectionIndex !=-1) {
                fCurrentNodeIndex =
                fDeferredDocumentImpl.getParentNode (fCurrentNodeIndex, false);
                fCurrentCDATASectionIndex = -1;
            }
        }

    } // endCDATA()

    /**
     * The end of the document.
     * @param augs     Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endDocument (Augmentations augs) throws XNIException {

        if (!fDeferNodeExpansion) {
            // REVISIT: when DOM Level 3 is REC rely on Document.support
            //          instead of specific class
            // set the actual encoding and set DOM error checking back on
            if (fDocumentImpl != null) {
                if (fLocator != null) {
                    if (fLocator.getEncoding() != null)
                        fDocumentImpl.setInputEncoding (fLocator.getEncoding());
                }
                fDocumentImpl.setStrictErrorChecking (true);
            }
            fCurrentNode = null;
        }
        else {
            // set the actual encoding
            if (fLocator != null) {
                if (fLocator.getEncoding() != null)
                    fDeferredDocumentImpl.setInputEncoding (fLocator.getEncoding());
            }
            fCurrentNodeIndex = -1;
        }

    } // endDocument()

    /**
     * This method notifies the end of a general entity.
     * <p>
     * <strong>Note:</strong> This method is not called for entity references
     * appearing as part of attribute values.
     *
     * @param name   The name of the entity.
     * @param augs   Additional information that may include infoset augmentations
     *
     * @exception XNIException
     *                   Thrown by handler to signal an error.
     */
    public void endGeneralEntity (String name, Augmentations augs) throws XNIException {
        if (DEBUG_EVENTS) {
            System.out.println ("==>endGeneralEntity: ("+name+")");
        }
        if (!fDeferNodeExpansion) {

            if (fFilterReject) {
                return;
            }
            setCharacterData (true);

            if (fDocumentType != null) {
                // get current entity declaration
                NamedNodeMap entities = fDocumentType.getEntities ();
                fCurrentEntityDecl = (EntityImpl) entities.getNamedItem (name);
                if (fCurrentEntityDecl != null) {
                    if (fCurrentEntityDecl != null && fCurrentEntityDecl.getFirstChild () == null) {
                        fCurrentEntityDecl.setReadOnly (false, true);
                        Node child = fCurrentNode.getFirstChild ();
                        while (child != null) {
                            Node copy = child.cloneNode (true);
                            fCurrentEntityDecl.appendChild (copy);
                            child = child.getNextSibling ();
                        }
                        fCurrentEntityDecl.setReadOnly (true, true);

                        //entities.setNamedItem(fCurrentEntityDecl);
                    }
                    fCurrentEntityDecl = null;
                }

            }
            fInEntityRef = false;
            boolean removeEntityRef = false;
            if (fCreateEntityRefNodes) {
                if (fDocumentImpl != null) {
                    // Make entity ref node read only
                    ((NodeImpl)fCurrentNode).setReadOnly (true, true);
                }

                if (fDOMFilter !=null &&
                (fDOMFilter.getWhatToShow () & NodeFilter.SHOW_ENTITY_REFERENCE)!= 0) {
                    short code = fDOMFilter.acceptNode (fCurrentNode);
                    switch (code) {
                        case LSParserFilter.FILTER_INTERRUPT:{
                            throw Abort.INSTANCE;
                        }
                        case LSParserFilter.FILTER_REJECT:{
                            Node parent = fCurrentNode.getParentNode ();
                            parent.removeChild (fCurrentNode);
                            fCurrentNode = parent;
                            return;

                        }
                        case LSParserFilter.FILTER_SKIP: {
                            // make sure we don't loose chars if next event is characters()
                            fFirstChunk = true;
                            removeEntityRef = true;
                            break;
                        }

                        default: {
                            fCurrentNode = fCurrentNode.getParentNode ();
                        }
                    }
                } else {
                    fCurrentNode = fCurrentNode.getParentNode ();
                }
            }

            if (!fCreateEntityRefNodes || removeEntityRef) {
                // move entity reference children to the list of
                // siblings of its parent and remove entity reference
                NodeList children = fCurrentNode.getChildNodes ();
                Node parent = fCurrentNode.getParentNode ();
                int length = children.getLength ();
                if (length > 0) {

                    // get previous sibling of the entity reference
                    Node node = fCurrentNode.getPreviousSibling ();
                    // normalize text nodes
                    Node child = children.item (0);
                    if (node != null && node.getNodeType () == Node.TEXT_NODE &&
                    child.getNodeType () == Node.TEXT_NODE) {
                        ((Text)node).appendData (child.getNodeValue ());
                        fCurrentNode.removeChild (child);

                    } else {
                        node = parent.insertBefore (child, fCurrentNode);
                        handleBaseURI (node);
                    }

                    for (int i=1;i <length;i++) {
                        node = parent.insertBefore (children.item (0), fCurrentNode);
                        handleBaseURI (node);
                    }
                } // length > 0
                parent.removeChild (fCurrentNode);
                fCurrentNode = parent;
            }
        }
        else {

            if (fDocumentTypeIndex != -1) {
                // find corresponding Entity decl
                int node = fDeferredDocumentImpl.getLastChild (fDocumentTypeIndex, false);
                while (node != -1) {
                    short nodeType = fDeferredDocumentImpl.getNodeType (node, false);
                    if (nodeType == Node.ENTITY_NODE) {
                        String nodeName =
                        fDeferredDocumentImpl.getNodeName (node, false);
                        if (nodeName.equals (name)) {
                            fDeferredEntityDecl = node;
                            break;
                        }
                    }
                    node = fDeferredDocumentImpl.getRealPrevSibling (node, false);
                }
            }

            if (fDeferredEntityDecl != -1 &&
            fDeferredDocumentImpl.getLastChild (fDeferredEntityDecl, false) == -1) {
                // entity definition exists and it does not have any children
                int prevIndex = -1;
                int childIndex = fDeferredDocumentImpl.getLastChild (fCurrentNodeIndex, false);
                while (childIndex != -1) {
                    int cloneIndex = fDeferredDocumentImpl.cloneNode (childIndex, true);
                    fDeferredDocumentImpl.insertBefore (fDeferredEntityDecl, cloneIndex, prevIndex);
                    prevIndex = cloneIndex;
                    childIndex = fDeferredDocumentImpl.getRealPrevSibling (childIndex, false);
                }
            }
            if (fCreateEntityRefNodes) {
                fCurrentNodeIndex =
                fDeferredDocumentImpl.getParentNode (fCurrentNodeIndex,
                false);
            } else { //!fCreateEntityRefNodes
                // move children of entity ref before the entity ref.
                // remove entity ref.

                // holds a child of entity ref
                int childIndex = fDeferredDocumentImpl.getLastChild (fCurrentNodeIndex, false);
                int parentIndex =
                fDeferredDocumentImpl.getParentNode (fCurrentNodeIndex,
                false);

                int prevIndex = fCurrentNodeIndex;
                int lastChild = childIndex;
                int sibling = -1;
                while (childIndex != -1) {
                    handleBaseURI (childIndex);
                    sibling = fDeferredDocumentImpl.getRealPrevSibling (childIndex, false);
                    fDeferredDocumentImpl.insertBefore (parentIndex, childIndex, prevIndex);
                    prevIndex = childIndex;
                    childIndex = sibling;
                }
                if(lastChild != -1)
                    fDeferredDocumentImpl.setAsLastChild (parentIndex, lastChild);
                else{
                    sibling = fDeferredDocumentImpl.getRealPrevSibling (prevIndex, false);
                    fDeferredDocumentImpl.setAsLastChild (parentIndex, sibling);
                }
                fCurrentNodeIndex = parentIndex;
            }
            fDeferredEntityDecl = -1;
        }


    } // endGeneralEntity(String, Augmentations)


    /**
     * Record baseURI information for the Element (by adding xml:base attribute)
     * or for the ProcessingInstruction (by setting a baseURI field)
     * Non deferred DOM.
     *
     * @param node
     */
    protected final void handleBaseURI (Node node){
        if (fDocumentImpl != null) {
            // REVISIT: remove dependency on our implementation when
            //          DOM L3 becomes REC

            String baseURI = null;
            short nodeType = node.getNodeType ();

            if (nodeType == Node.ELEMENT_NODE) {
                // if an element already has xml:base attribute
                // do nothing
                if (fNamespaceAware) {
                    if (((Element)node).getAttributeNodeNS ("http://www.w3.org/XML/1998/namespace","base")!=null) {
                        return;
                    }
                } else if (((Element)node).getAttributeNode ("xml:base") != null) {
                    return;
                }
                // retrive the baseURI from the entity reference
                baseURI = ((EntityReferenceImpl)fCurrentNode).getBaseURI ();
                if (baseURI !=null && !baseURI.equals (fDocumentImpl.getDocumentURI ())) {
                    if (fNamespaceAware) {
                        ((Element)node).setAttributeNS ("http://www.w3.org/XML/1998/namespace", "xml:base", baseURI);
                    } else {
                        ((Element)node).setAttribute ("xml:base", baseURI);
                    }
                }
            }
            else if (nodeType == Node.PROCESSING_INSTRUCTION_NODE) {

                baseURI = ((EntityReferenceImpl)fCurrentNode).getBaseURI ();
                if (baseURI !=null && fErrorHandler != null) {
                    DOMErrorImpl error = new DOMErrorImpl ();
                    error.fType = "pi-base-uri-not-preserved";
                    error.fRelatedData = baseURI;
                    error.fSeverity = DOMError.SEVERITY_WARNING;
                    fErrorHandler.getErrorHandler ().handleError (error);
                }
            }
        }
    }

    /**
     *
     * Record baseURI information for the Element (by adding xml:base attribute)
     * or for the ProcessingInstruction (by setting a baseURI field)
     * Deferred DOM.
     *
     * @param node
     */
    protected final void handleBaseURI (int node){
        short nodeType = fDeferredDocumentImpl.getNodeType (node, false);

        if (nodeType == Node.ELEMENT_NODE) {
            String baseURI = fDeferredDocumentImpl.getNodeValueString (fCurrentNodeIndex, false);
            if (baseURI == null) {
                baseURI = fDeferredDocumentImpl.getDeferredEntityBaseURI (fDeferredEntityDecl);
            }
            if (baseURI !=null && !baseURI.equals (fDeferredDocumentImpl.getDocumentURI ())) {
                fDeferredDocumentImpl.setDeferredAttribute (node,
                "xml:base",
                "http://www.w3.org/XML/1998/namespace",
                baseURI,
                true,
                false,
                null);
            }
        }
        else if (nodeType == Node.PROCESSING_INSTRUCTION_NODE) {


            // retrieve baseURI from the entity reference
            String baseURI = fDeferredDocumentImpl.getNodeValueString (fCurrentNodeIndex, false);

            if (baseURI == null) {
                // try baseURI of the entity declaration
                baseURI = fDeferredDocumentImpl.getDeferredEntityBaseURI (fDeferredEntityDecl);
            }

            if (baseURI != null && fErrorHandler != null) {
                DOMErrorImpl error = new DOMErrorImpl ();
                error.fType = "pi-base-uri-not-preserved";
                error.fRelatedData = baseURI;
                error.fSeverity = DOMError.SEVERITY_WARNING;
                fErrorHandler.getErrorHandler ().handleError (error);
            }
        }
    }


    //
    // XMLDTDHandler methods
    //

    /**
     * The start of the DTD.
     *
     * @param locator  The document locator, or null if the document
     *                 location cannot be reported during the parsing of
     *                 the document DTD. However, it is <em>strongly</em>
     *                 recommended that a locator be supplied that can
     *                 at least report the base system identifier of the
     *                 DTD.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startDTD (XMLLocator locator, Augmentations augs) throws XNIException {
        if (DEBUG_EVENTS) {
            System.out.println ("==>startDTD");
            if (DEBUG_BASEURI) {
                System.out.println ("   expandedSystemId: "+locator.getExpandedSystemId ());
                System.out.println ("   baseURI:"+ locator.getBaseSystemId ());
            }
        }

        fInDTD = true;
        if (locator != null) {
            fBaseURIStack.push (locator.getBaseSystemId ());
        }
        if (fDeferNodeExpansion || fDocumentImpl != null) {
            fInternalSubset = new StringBuilder (1024);
        }
    } // startDTD(XMLLocator)


    /**
     * The end of the DTD.
     *
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endDTD (Augmentations augs) throws XNIException {
        if (DEBUG_EVENTS) {
            System.out.println ("==>endDTD()");
        }
        fInDTD = false;
        if (!fBaseURIStack.isEmpty ()) {
            fBaseURIStack.pop ();
        }
        String internalSubset = fInternalSubset != null && fInternalSubset.length () > 0
        ? fInternalSubset.toString () : null;
        if (fDeferNodeExpansion) {
            if (internalSubset != null) {
                fDeferredDocumentImpl.setInternalSubset (fDocumentTypeIndex, internalSubset);
            }
        }
        else if (fDocumentImpl != null) {
            if (internalSubset != null) {
                ((DocumentTypeImpl)fDocumentType).setInternalSubset (internalSubset);
            }
        }
    } // endDTD()

    /**
     * The start of a conditional section.
     *
     * @param type The type of the conditional section. This value will
     *             either be CONDITIONAL_INCLUDE or CONDITIONAL_IGNORE.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     *
     * @see #CONDITIONAL_INCLUDE
     * @see #CONDITIONAL_IGNORE
     */
    public void startConditional (short type, Augmentations augs) throws XNIException  {
    } // startConditional(short)

    /**
     * The end of a conditional section.
     *
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endConditional (Augmentations augs) throws XNIException {
    } // endConditional()


    /**
     * The start of the DTD external subset.
     *
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startExternalSubset (XMLResourceIdentifier identifier,
    Augmentations augs) throws XNIException {
        if (DEBUG_EVENTS) {
            System.out.println ("==>startExternalSubset");
            if (DEBUG_BASEURI) {
                System.out.println ("   expandedSystemId: "+identifier.getExpandedSystemId ());
                System.out.println ("   baseURI:"+ identifier.getBaseSystemId ());
            }
        }
        fBaseURIStack.push (identifier.getBaseSystemId ());
        fInDTDExternalSubset = true;
    } // startExternalSubset(Augmentations)

    /**
     * The end of the DTD external subset.
     *
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endExternalSubset (Augmentations augs) throws XNIException {
        fInDTDExternalSubset = false;
        fBaseURIStack.pop ();
    } // endExternalSubset(Augmentations)

    /**
     * An internal entity declaration.
     *
     * @param name The name of the entity. Parameter entity names start with
     *             '%', whereas the name of a general entity is just the
     *             entity name.
     * @param text The value of the entity.
     * @param nonNormalizedText The non-normalized value of the entity. This
     *             value contains the same sequence of characters that was in
     *             the internal entity declaration, without any entity
     *             references expanded.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void internalEntityDecl (String name, XMLString text,
    XMLString nonNormalizedText,
    Augmentations augs) throws XNIException {

        if (DEBUG_EVENTS) {
            System.out.println ("==>internalEntityDecl: "+name);
            if (DEBUG_BASEURI) {
                System.out.println ("   baseURI:"+ fBaseURIStack.peek ());
            }
        }
        // internal subset string
        if (fInternalSubset != null && !fInDTDExternalSubset) {
            fInternalSubset.append ("<!ENTITY ");
            if (name.startsWith ("%")) {
                fInternalSubset.append ("% ");
                fInternalSubset.append (name.substring (1));
            }
            else {
                fInternalSubset.append (name);
            }
            fInternalSubset.append (' ');
            String value = nonNormalizedText.toString ();
            boolean singleQuote = value.indexOf ('\'') == -1;
            fInternalSubset.append (singleQuote ? '\'' : '"');
            fInternalSubset.append (value);
            fInternalSubset.append (singleQuote ? '\'' : '"');
            fInternalSubset.append (">\n");
        }

        // NOTE: We only know how to create these nodes for the Xerces
        //       DOM implementation because DOM Level 2 does not specify
        //       that functionality. -Ac

        // create full node
        // don't add parameter entities!
        if(name.startsWith ("%"))
            return;
        if (fDocumentType != null) {
            NamedNodeMap entities = fDocumentType.getEntities ();
            EntityImpl entity = (EntityImpl)entities.getNamedItem (name);
            if (entity == null) {
                entity = (EntityImpl)fDocumentImpl.createEntity (name);
                entity.setBaseURI (fBaseURIStack.peek ());
                entities.setNamedItem (entity);
            }
        }

        // create deferred node
        if (fDocumentTypeIndex != -1) {
            boolean found = false;
            int node = fDeferredDocumentImpl.getLastChild (fDocumentTypeIndex, false);
            while (node != -1) {
                short nodeType = fDeferredDocumentImpl.getNodeType (node, false);
                if (nodeType == Node.ENTITY_NODE) {
                    String nodeName = fDeferredDocumentImpl.getNodeName (node, false);
                    if (nodeName.equals (name)) {
                        found = true;
                        break;
                    }
                }
                node = fDeferredDocumentImpl.getRealPrevSibling (node, false);
            }
            if (!found) {
                int entityIndex =
                fDeferredDocumentImpl.createDeferredEntity (name, null, null, null, fBaseURIStack.peek ());
                fDeferredDocumentImpl.appendChild (fDocumentTypeIndex, entityIndex);
            }
        }

    } // internalEntityDecl(String,XMLString,XMLString)

    /**
     * An external entity declaration.
     *
     * @param name     The name of the entity. Parameter entity names start
     *                 with '%', whereas the name of a general entity is just
     *                 the entity name.
     * @param identifier    An object containing all location information
     *                      pertinent to this notation.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void externalEntityDecl (String name, XMLResourceIdentifier identifier,
    Augmentations augs) throws XNIException {


        if (DEBUG_EVENTS) {
            System.out.println ("==>externalEntityDecl: "+name);
            if (DEBUG_BASEURI) {
                System.out.println ("   expandedSystemId:"+ identifier.getExpandedSystemId ());
                System.out.println ("   baseURI:"+ identifier.getBaseSystemId ());
            }
        }
        // internal subset string
        String publicId = identifier.getPublicId ();
        String literalSystemId = identifier.getLiteralSystemId ();
        if (fInternalSubset != null && !fInDTDExternalSubset) {
            fInternalSubset.append ("<!ENTITY ");
            if (name.startsWith ("%")) {
                fInternalSubset.append ("% ");
                fInternalSubset.append (name.substring (1));
            }
            else {
                fInternalSubset.append (name);
            }
            fInternalSubset.append (' ');
            if (publicId != null) {
                fInternalSubset.append ("PUBLIC '");
                fInternalSubset.append (publicId);
                fInternalSubset.append ("' '");
            }
            else {
                fInternalSubset.append ("SYSTEM '");
            }
            fInternalSubset.append (literalSystemId);
            fInternalSubset.append ("'>\n");
        }

        // NOTE: We only know how to create these nodes for the Xerces
        //       DOM implementation because DOM Level 2 does not specify
        //       that functionality. -Ac

        // create full node
        // don't add parameter entities!
        if(name.startsWith ("%"))
            return;
        if (fDocumentType != null) {
            NamedNodeMap entities = fDocumentType.getEntities ();
            EntityImpl entity = (EntityImpl)entities.getNamedItem (name);
            if (entity == null) {
                entity = (EntityImpl)fDocumentImpl.createEntity (name);
                entity.setPublicId (publicId);
                entity.setSystemId (literalSystemId);
                entity.setBaseURI (identifier.getBaseSystemId ());
                entities.setNamedItem (entity);
            }
        }

        // create deferred node
        if (fDocumentTypeIndex != -1) {
            boolean found = false;
            int nodeIndex = fDeferredDocumentImpl.getLastChild (fDocumentTypeIndex, false);
            while (nodeIndex != -1) {
                short nodeType = fDeferredDocumentImpl.getNodeType (nodeIndex, false);
                if (nodeType == Node.ENTITY_NODE) {
                    String nodeName = fDeferredDocumentImpl.getNodeName (nodeIndex, false);
                    if (nodeName.equals (name)) {
                        found = true;
                        break;
                    }
                }
                nodeIndex = fDeferredDocumentImpl.getRealPrevSibling (nodeIndex, false);
            }
            if (!found) {
                int entityIndex = fDeferredDocumentImpl.createDeferredEntity (
                name, publicId, literalSystemId, null, identifier.getBaseSystemId ());
                fDeferredDocumentImpl.appendChild (fDocumentTypeIndex, entityIndex);
            }
        }

    } // externalEntityDecl(String,XMLResourceIdentifier, Augmentations)


    /**
     * This method notifies of the start of a parameter entity. The parameter
     * entity name start with a '%' character.
     *
     * @param name     The name of the parameter entity.
     * @param identifier The resource identifier.
     * @param encoding The auto-detected IANA encoding name of the entity
     *                 stream. This value will be null in those situations
     *                 where the entity encoding is not auto-detected (e.g.
     *                 internal parameter entities).
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startParameterEntity (String name,
    XMLResourceIdentifier identifier,
    String encoding,
    Augmentations augs) throws XNIException {
        if (DEBUG_EVENTS) {
            System.out.println ("==>startParameterEntity: "+name);
            if (DEBUG_BASEURI) {
                System.out.println ("   expandedSystemId: "+identifier.getExpandedSystemId ());
                System.out.println ("   baseURI:"+ identifier.getBaseSystemId ());
            }
        }
        if (augs != null && fInternalSubset != null &&
            !fInDTDExternalSubset &&
            Boolean.TRUE.equals(augs.getItem(Constants.ENTITY_SKIPPED))) {
            fInternalSubset.append(name).append(";\n");
        }
        fBaseURIStack.push (identifier.getExpandedSystemId ());
    }


    /**
     * This method notifies the end of a parameter entity. Parameter entity
     * names begin with a '%' character.
     *
     * @param name The name of the parameter entity.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endParameterEntity (String name, Augmentations augs) throws XNIException {

        if (DEBUG_EVENTS) {
            System.out.println ("==>endParameterEntity: "+name);
        }
        fBaseURIStack.pop ();
    }

    /**
     * An unparsed entity declaration.
     *
     * @param name     The name of the entity.
     * @param identifier    An object containing all location information
     *                      pertinent to this entity.
     * @param notation The name of the notation.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void unparsedEntityDecl (String name, XMLResourceIdentifier identifier,
    String notation, Augmentations augs)
    throws XNIException {

        if (DEBUG_EVENTS) {
            System.out.println ("==>unparsedEntityDecl: "+name);
            if (DEBUG_BASEURI) {
                System.out.println ("   expandedSystemId:"+ identifier.getExpandedSystemId ());
                System.out.println ("   baseURI:"+ identifier.getBaseSystemId ());
            }
        }
        // internal subset string
        String publicId = identifier.getPublicId ();
        String literalSystemId = identifier.getLiteralSystemId ();
        if (fInternalSubset != null && !fInDTDExternalSubset) {
            fInternalSubset.append ("<!ENTITY ");
            fInternalSubset.append (name);
            fInternalSubset.append (' ');
            if (publicId != null) {
                fInternalSubset.append ("PUBLIC '");
                fInternalSubset.append (publicId);
                if (literalSystemId != null) {
                    fInternalSubset.append ("' '");
                    fInternalSubset.append (literalSystemId);
                }
            }
            else {
                fInternalSubset.append ("SYSTEM '");
                fInternalSubset.append (literalSystemId);
            }
            fInternalSubset.append ("' NDATA ");
            fInternalSubset.append (notation);
            fInternalSubset.append (">\n");
        }

        // NOTE: We only know how to create these nodes for the Xerces
        //       DOM implementation because DOM Level 2 does not specify
        //       that functionality. -Ac

        // create full node
        if (fDocumentType != null) {
            NamedNodeMap entities = fDocumentType.getEntities ();
            EntityImpl entity = (EntityImpl)entities.getNamedItem (name);
            if (entity == null) {
                entity = (EntityImpl)fDocumentImpl.createEntity (name);
                entity.setPublicId (publicId);
                entity.setSystemId (literalSystemId);
                entity.setNotationName (notation);
                entity.setBaseURI (identifier.getBaseSystemId ());
                entities.setNamedItem (entity);
            }
        }

        // create deferred node
        if (fDocumentTypeIndex != -1) {
            boolean found = false;
            int nodeIndex = fDeferredDocumentImpl.getLastChild (fDocumentTypeIndex, false);
            while (nodeIndex != -1) {
                short nodeType = fDeferredDocumentImpl.getNodeType (nodeIndex, false);
                if (nodeType == Node.ENTITY_NODE) {
                    String nodeName = fDeferredDocumentImpl.getNodeName (nodeIndex, false);
                    if (nodeName.equals (name)) {
                        found = true;
                        break;
                    }
                }
                nodeIndex = fDeferredDocumentImpl.getRealPrevSibling (nodeIndex, false);
            }
            if (!found) {
                int entityIndex = fDeferredDocumentImpl.createDeferredEntity (
                name, publicId, literalSystemId, notation, identifier.getBaseSystemId ());
                fDeferredDocumentImpl.appendChild (fDocumentTypeIndex, entityIndex);
            }
        }

    } // unparsedEntityDecl(String,XMLResourceIdentifier, String, Augmentations)

    /**
     * A notation declaration
     *
     * @param name     The name of the notation.
     * @param identifier    An object containing all location information
     *                      pertinent to this notation.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void notationDecl (String name, XMLResourceIdentifier identifier,
    Augmentations augs) throws XNIException {

        // internal subset string
        String publicId = identifier.getPublicId ();
        String literalSystemId = identifier.getLiteralSystemId ();
        if (fInternalSubset != null && !fInDTDExternalSubset) {
            fInternalSubset.append ("<!NOTATION ");
            fInternalSubset.append (name);
            if (publicId != null) {
                fInternalSubset.append (" PUBLIC '");
                fInternalSubset.append (publicId);
                if (literalSystemId != null) {
                    fInternalSubset.append ("' '");
                    fInternalSubset.append (literalSystemId);
                }
            }
            else {
                fInternalSubset.append (" SYSTEM '");
                fInternalSubset.append (literalSystemId);
            }
            fInternalSubset.append ("'>\n");
        }

        // NOTE: We only know how to create these nodes for the Xerces
        //       DOM implementation because DOM Level 2 does not specify
        //       that functionality. -Ac

        // create full node
        if (fDocumentImpl !=null && fDocumentType != null) {
            NamedNodeMap notations = fDocumentType.getNotations ();
            if (notations.getNamedItem (name) == null) {
                NotationImpl notation = (NotationImpl)fDocumentImpl.createNotation (name);
                notation.setPublicId (publicId);
                notation.setSystemId (literalSystemId);
                notation.setBaseURI (identifier.getBaseSystemId ());
                notations.setNamedItem (notation);
            }
        }

        // create deferred node
        if (fDocumentTypeIndex != -1) {
            boolean found = false;
            int nodeIndex = fDeferredDocumentImpl.getLastChild (fDocumentTypeIndex, false);
            while (nodeIndex != -1) {
                short nodeType = fDeferredDocumentImpl.getNodeType (nodeIndex, false);
                if (nodeType == Node.NOTATION_NODE) {
                    String nodeName = fDeferredDocumentImpl.getNodeName (nodeIndex, false);
                    if (nodeName.equals (name)) {
                        found = true;
                        break;
                    }
                }
                nodeIndex = fDeferredDocumentImpl.getPrevSibling (nodeIndex, false);
            }
            if (!found) {
                int notationIndex = fDeferredDocumentImpl.createDeferredNotation (
                name, publicId, literalSystemId, identifier.getBaseSystemId ());
                fDeferredDocumentImpl.appendChild (fDocumentTypeIndex, notationIndex);
            }
        }

    } // notationDecl(String,XMLResourceIdentifier, Augmentations)

    /**
     * Characters within an IGNORE conditional section.
     *
     * @param text The ignored text.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void ignoredCharacters (XMLString text, Augmentations augs) throws XNIException {
    } // ignoredCharacters(XMLString, Augmentations)


    /**
     * An element declaration.
     *
     * @param name         The name of the element.
     * @param contentModel The element content model.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void elementDecl (String name, String contentModel, Augmentations augs)
    throws XNIException {

        // internal subset string
        if (fInternalSubset != null && !fInDTDExternalSubset) {
            fInternalSubset.append ("<!ELEMENT ");
            fInternalSubset.append (name);
            fInternalSubset.append (' ');
            fInternalSubset.append (contentModel);
            fInternalSubset.append (">\n");
        }

    } // elementDecl(String,String)

    /**
     * An attribute declaration.
     *
     * @param elementName   The name of the element that this attribute
     *                      is associated with.
     * @param attributeName The name of the attribute.
     * @param type          The attribute type. This value will be one of
     *                      the following: "CDATA", "ENTITY", "ENTITIES",
     *                      "ENUMERATION", "ID", "IDREF", "IDREFS",
     *                      "NMTOKEN", "NMTOKENS", or "NOTATION".
     * @param enumeration   If the type has the value "ENUMERATION" or
     *                      "NOTATION", this array holds the allowed attribute
     *                      values; otherwise, this array is null.
     * @param defaultType   The attribute default type. This value will be
     *                      one of the following: "#FIXED", "#IMPLIED",
     *                      "#REQUIRED", or null.
     * @param defaultValue  The attribute default value, or null if no
     *                      default value is specified.
     * @param nonNormalizedDefaultValue  The attribute default value with no normalization
     *                      performed, or null if no default value is specified.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void attributeDecl (String elementName, String attributeName,
    String type, String[] enumeration,
    String defaultType, XMLString defaultValue,
    XMLString nonNormalizedDefaultValue, Augmentations augs) throws XNIException {

        // internal subset string
        if (fInternalSubset != null && !fInDTDExternalSubset) {
            fInternalSubset.append ("<!ATTLIST ");
            fInternalSubset.append (elementName);
            fInternalSubset.append (' ');
            fInternalSubset.append (attributeName);
            fInternalSubset.append (' ');
            if (type.equals ("ENUMERATION")) {
                fInternalSubset.append ('(');
                for (int i = 0; i < enumeration.length; i++) {
                    if (i > 0) {
                        fInternalSubset.append ('|');
                    }
                    fInternalSubset.append (enumeration[i]);
                }
                fInternalSubset.append (')');
            }
            else {
                fInternalSubset.append (type);
            }
            if (defaultType != null) {
                fInternalSubset.append (' ');
                fInternalSubset.append (defaultType);
            }
            if (defaultValue != null) {
                fInternalSubset.append (" '");
                for (int i = 0; i < defaultValue.length; i++) {
                    char c = defaultValue.ch[defaultValue.offset + i];
                    if (c == '\'') {
                        fInternalSubset.append ("&apos;");
                    }
                    else {
                        fInternalSubset.append (c);
                    }
                }
                fInternalSubset.append ('\'');
            }
            fInternalSubset.append (">\n");
        }
        // REVISIT: This code applies to the support of domx/grammar-access
        // feature in Xerces 1

        // deferred expansion
        if (fDeferredDocumentImpl != null) {

            // get the default value
            if (defaultValue != null) {

                // get element definition
                int elementDefIndex  = fDeferredDocumentImpl.lookupElementDefinition (elementName);

                // create element definition if not already there
                if (elementDefIndex == -1) {
                    elementDefIndex = fDeferredDocumentImpl.createDeferredElementDefinition (elementName);
                    fDeferredDocumentImpl.appendChild (fDocumentTypeIndex, elementDefIndex);
                }
                // add default attribute
                boolean nsEnabled = fNamespaceAware;
                String namespaceURI = null;
                if (nsEnabled) {
                    // DOM Level 2 wants all namespace declaration attributes
                    // to be bound to "http://www.w3.org/2000/xmlns/"
                    // So as long as the XML parser doesn't do it, it needs to
                    // done here.
                    if (attributeName.startsWith("xmlns:") ||
                        attributeName.equals("xmlns")) {
                        namespaceURI = NamespaceContext.XMLNS_URI;
                    }
                    else if (attributeName.startsWith("xml:")) {
                        namespaceURI = NamespaceContext.XML_URI;
                    }
                }
                int attrIndex = fDeferredDocumentImpl.createDeferredAttribute (
                        attributeName, namespaceURI, defaultValue.toString(), false);
                if ("ID".equals (type)) {
                    fDeferredDocumentImpl.setIdAttribute (attrIndex);
                }
                // REVISIT: set ID type correctly
                fDeferredDocumentImpl.appendChild (elementDefIndex, attrIndex);
            }

        } // if deferred

        // full expansion
        else if (fDocumentImpl != null) {

            // get the default value
            if (defaultValue != null) {

                // get element definition node
                NamedNodeMap elements = ((DocumentTypeImpl)fDocumentType).getElements ();
                ElementDefinitionImpl elementDef = (ElementDefinitionImpl)elements.getNamedItem (elementName);
                if (elementDef == null) {
                    elementDef = fDocumentImpl.createElementDefinition (elementName);
                    ((DocumentTypeImpl)fDocumentType).getElements ().setNamedItem (elementDef);
                }

                // REVISIT: Check for uniqueness of element name? -Ac

                // create attribute and set properties
                boolean nsEnabled = fNamespaceAware;
                AttrImpl attr;
                if (nsEnabled) {
                    String namespaceURI = null;
                    // DOM Level 2 wants all namespace declaration attributes
                    // to be bound to "http://www.w3.org/2000/xmlns/"
                    // So as long as the XML parser doesn't do it, it needs to
                    // done here.
                    if (attributeName.startsWith("xmlns:") ||
                        attributeName.equals("xmlns")) {
                        namespaceURI = NamespaceContext.XMLNS_URI;
                    }
                    else if (attributeName.startsWith("xml:")) {
                        namespaceURI = NamespaceContext.XML_URI;
                    }
                    attr = (AttrImpl)fDocumentImpl.createAttributeNS (namespaceURI,
                    attributeName);
                }
                else {
                    attr = (AttrImpl)fDocumentImpl.createAttribute (attributeName);
                }
                attr.setValue (defaultValue.toString ());
                attr.setSpecified (false);
                attr.setIdAttribute ("ID".equals (type));

                // add default attribute to element definition
                if (nsEnabled){
                    elementDef.getAttributes ().setNamedItemNS (attr);
                }
                else {
                    elementDef.getAttributes ().setNamedItem (attr);
                }
            }

        } // if NOT defer-node-expansion

    } // attributeDecl(String,String,String,String[],String,XMLString, XMLString, Augmentations)


    /**
     * The start of an attribute list.
     *
     * @param elementName The name of the element that this attribute
     *                    list is associated with.
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startAttlist (String elementName, Augmentations augs) throws XNIException {
    } // startAttlist(String)


    /**
     * The end of an attribute list.
     *
     * @param augs Additional information that may include infoset
     *                      augmentations.
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void endAttlist (Augmentations augs) throws XNIException {
    } // endAttlist()


    // method to create an element node.
    // subclasses can override this method to create element nodes in other ways.
    protected Element createElementNode (QName element) {
        Element el = null;

        if (fNamespaceAware) {
            // if we are using xerces DOM implementation, call our
            // own constructor to reuse the strings we have here.
            if (fDocumentImpl != null) {
                el = fDocumentImpl.createElementNS (element.uri, element.rawname,
                element.localpart);
            }
            else {
                el = fDocument.createElementNS (element.uri, element.rawname);
            }
        }
        else {
            el = fDocument.createElement (element.rawname);
        }

        return el;
    }

    // method to create an attribute node.
    // subclasses can override this method to create attribute nodes in other ways.
    protected Attr createAttrNode (QName attrQName) {
        Attr attr = null;

        if (fNamespaceAware) {
            if (fDocumentImpl != null) {
                // if we are using xerces DOM implementation, call our
                // own constructor to reuse the strings we have here.
                attr = fDocumentImpl.createAttributeNS (attrQName.uri,
                attrQName.rawname,
                attrQName.localpart);
            }
            else {
                attr = fDocument.createAttributeNS (attrQName.uri,
                attrQName.rawname);
            }
        }
        else {
            attr = fDocument.createAttribute (attrQName.rawname);
        }

        return attr;
    }

    /*
     * When the first characters() call is received, the data is stored in
     * a new Text node. If right after the first characters() we receive another chunk of data,
     * the data from the Text node, following the new characters are appended
     * to the fStringBuffer and the text node data is set to empty.
     *
     * This function is called when the state is changed and the
     * data must be appended to the current node.
     *
     * Note: if DOMFilter is set, you must make sure that if Node is skipped,
     * or removed fFistChunk must be set to true, otherwise some data can be lost.
     *
     */
    @SuppressWarnings("fallthrough") // by design at case LSParserFilter.FILTER_REJECT
    protected void  setCharacterData (boolean sawChars){

        // handle character data
        fFirstChunk = sawChars;


        // if we have data in the buffer we must have created
        // a text node already.

        Node child = fCurrentNode.getLastChild ();
        if (child != null) {
            if (fStringBuilder.length () > 0) {
                // REVISIT: should this check be performed?
                if (child.getNodeType () == Node.TEXT_NODE) {
                    if (fDocumentImpl != null) {
                        ((TextImpl)child).replaceData (fStringBuilder.toString ());
                    }
                    else {
                        ((Text)child).setData (fStringBuilder.toString ());
                    }
                }
                // reset string buffer
                fStringBuilder.setLength (0);
            }

            if (fDOMFilter !=null && !fInEntityRef) {
                if ( (child.getNodeType () == Node.TEXT_NODE ) &&
                ((fDOMFilter.getWhatToShow () & NodeFilter.SHOW_TEXT)!= 0) ) {
                    short code = fDOMFilter.acceptNode (child);
                    switch (code) {
                        case LSParserFilter.FILTER_INTERRUPT:{
                            throw Abort.INSTANCE;
                        }
                        case LSParserFilter.FILTER_REJECT:{
                            // fall through to SKIP since Comment has no children.
                        }
                        case LSParserFilter.FILTER_SKIP: {
                            fCurrentNode.removeChild (child);
                            return;
                        }
                        default: {
                            // accept node -- do nothing
                        }
                    }
                }
            }   // end-if fDOMFilter !=null

        } // end-if child !=null
    }


    /**
     * @see org.w3c.dom.ls.LSParser#abort()
     */
    public void abort () {
        throw Abort.INSTANCE;
    }


} // class AbstractDOMParser
