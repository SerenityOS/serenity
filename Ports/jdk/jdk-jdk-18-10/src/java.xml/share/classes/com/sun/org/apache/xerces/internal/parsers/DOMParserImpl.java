/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.org.apache.xerces.internal.dom.DOMErrorImpl;
import com.sun.org.apache.xerces.internal.dom.DOMMessageFormatter;
import com.sun.org.apache.xerces.internal.dom.DOMStringListImpl;
import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.util.DOMEntityResolverWrapper;
import com.sun.org.apache.xerces.internal.util.DOMErrorHandlerWrapper;
import com.sun.org.apache.xerces.internal.util.DOMUtil;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xni.XMLDTDContentModelHandler;
import com.sun.org.apache.xerces.internal.xni.XMLDTDHandler;
import com.sun.org.apache.xerces.internal.xni.XMLDocumentHandler;
import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDTDContentModelSource;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDTDSource;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDocumentSource;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParserConfiguration;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.StringTokenizer;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMStringList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSException;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSParserFilter;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;


/**
 * This is Xerces DOM Builder class. It uses the abstract DOM
 * parser with a document scanner, a dtd scanner, and a validator, as
 * well as a grammar pool.
 *
 * @author Pavani Mukthipudi, Sun Microsystems Inc.
 * @author Elena Litani, IBM
 * @author Rahul Srivastava, Sun Microsystems Inc.
 * @LastModified: Oct 2017
 */


public class DOMParserImpl
extends AbstractDOMParser implements LSParser, DOMConfiguration {



    // SAX & Xerces feature ids

    /** Feature identifier: namespaces. */
    protected static final String NAMESPACES =
    Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACES_FEATURE;

    /** Feature id: validation. */
    protected static final String VALIDATION_FEATURE =
    Constants.SAX_FEATURE_PREFIX+Constants.VALIDATION_FEATURE;

    /** XML Schema validation */
    protected static final String XMLSCHEMA =
    Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_VALIDATION_FEATURE;

    /** XML Schema full checking */
    protected static final String XMLSCHEMA_FULL_CHECKING =
    Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_FULL_CHECKING;

    /** Dynamic validation */
    protected static final String DYNAMIC_VALIDATION =
    Constants.XERCES_FEATURE_PREFIX + Constants.DYNAMIC_VALIDATION_FEATURE;

    /** Feature identifier: expose schema normalized value */
    protected static final String NORMALIZE_DATA =
    Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_NORMALIZED_VALUE;

    /** Feature identifier: disallow docType Decls. */
    protected static final String DISALLOW_DOCTYPE_DECL_FEATURE =
        Constants.XERCES_FEATURE_PREFIX + Constants.DISALLOW_DOCTYPE_DECL_FEATURE;

    /** Feature identifier: namespace growth */
    protected static final String NAMESPACE_GROWTH =
        Constants.XERCES_FEATURE_PREFIX + Constants.NAMESPACE_GROWTH_FEATURE;

    /** Feature identifier: tolerate duplicates */
    protected static final String TOLERATE_DUPLICATES =
        Constants.XERCES_FEATURE_PREFIX + Constants.TOLERATE_DUPLICATES_FEATURE;

    // internal properties
    protected static final String SYMBOL_TABLE =
    Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY;

    protected static final String PSVI_AUGMENT =
    Constants.XERCES_FEATURE_PREFIX +Constants.SCHEMA_AUGMENT_PSVI;


    //
    // Data
    //

    /** Include namespace declaration attributes in the document. **/
    protected boolean fNamespaceDeclarations = true;

    // REVISIT: this value should be null by default and should be set during creation of
    //          LSParser
    protected String fSchemaType = null;

    protected boolean fBusy = false;

    private boolean abortNow = false;

    private Thread currentThread;

    protected final static boolean DEBUG = false;

    private String fSchemaLocation = null;
        private DOMStringList fRecognizedParameters;

    private AbortHandler abortHandler = null;

    //
    // Constructors
    //

    /**
     * Constructs a DOM Builder using the standard parser configuration.
     */
    public DOMParserImpl (XMLParserConfiguration config, String schemaType) {
        this (config);
        if (schemaType != null) {
            if (schemaType.equals (Constants.NS_DTD)) {
                //Schema validation is false by default and hence there is no
                //need to set it to false here.  Also, schema validation is
                //not a recognized feature for DTDConfiguration's and so
                //setting this feature here would result in a Configuration
                //Exception.
                fConfiguration.setProperty (
                Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_LANGUAGE,
                Constants.NS_DTD);
                fSchemaType = Constants.NS_DTD;
            }
            else if (schemaType.equals (Constants.NS_XMLSCHEMA)) {
                // XML Schem validation
                fConfiguration.setProperty (
                Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_LANGUAGE,
                Constants.NS_XMLSCHEMA);
            }
        }

    }

    /**
     * Constructs a DOM Builder using the specified parser configuration.
     */
    public DOMParserImpl (XMLParserConfiguration config) {
        super (config);

        // add recognized features
        final String[] domRecognizedFeatures = {
            Constants.DOM_CANONICAL_FORM,
            Constants.DOM_CDATA_SECTIONS,
            Constants.DOM_CHARSET_OVERRIDES_XML_ENCODING,
            Constants.DOM_INFOSET,
            Constants.DOM_NAMESPACE_DECLARATIONS,
            Constants.DOM_SPLIT_CDATA,
            Constants.DOM_SUPPORTED_MEDIATYPES_ONLY,
            Constants.DOM_CERTIFIED,
            Constants.DOM_WELLFORMED,
            Constants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS,
        };

        fConfiguration.addRecognizedFeatures (domRecognizedFeatures);

        // turn off deferred DOM
        fConfiguration.setFeature (DEFER_NODE_EXPANSION, false);

        // Set values so that the value of the
        // infoset parameter is true (its default value).
        //
        // true: namespace-declarations, well-formed,
        // element-content-whitespace, comments, namespaces
        //
        // false: validate-if-schema, entities,
        // datatype-normalization, cdata-sections

        fConfiguration.setFeature(Constants.DOM_NAMESPACE_DECLARATIONS, true);
        fConfiguration.setFeature(Constants.DOM_WELLFORMED, true);
        fConfiguration.setFeature(INCLUDE_COMMENTS_FEATURE, true);
        fConfiguration.setFeature(INCLUDE_IGNORABLE_WHITESPACE, true);
        fConfiguration.setFeature(NAMESPACES, true);

        fConfiguration.setFeature(DYNAMIC_VALIDATION, false);
        fConfiguration.setFeature(CREATE_ENTITY_REF_NODES, false);
        fConfiguration.setFeature(CREATE_CDATA_NODES_FEATURE, false);

        // set other default values
        fConfiguration.setFeature (Constants.DOM_CANONICAL_FORM, false);
        fConfiguration.setFeature (Constants.DOM_CHARSET_OVERRIDES_XML_ENCODING, true);
        fConfiguration.setFeature (Constants.DOM_SPLIT_CDATA, true);
        fConfiguration.setFeature (Constants.DOM_SUPPORTED_MEDIATYPES_ONLY, false);
        fConfiguration.setFeature (Constants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS, true);

        // REVISIT: by default Xerces assumes that input is certified.
        //          default is different from the one specified in the DOM spec
        fConfiguration.setFeature (Constants.DOM_CERTIFIED, true);

        // Xerces datatype-normalization feature is on by default
        // This is a recognized feature only for XML Schemas. If the
        // configuration doesn't support this feature, ignore it.
        try {
            fConfiguration.setFeature ( NORMALIZE_DATA, false );
        }
        catch (XMLConfigurationException exc) {}

    } // <init>(XMLParserConfiguration)

    /**
     * Constructs a DOM Builder using the specified symbol table.
     */
    public DOMParserImpl (SymbolTable symbolTable) {
        this (new XIncludeAwareParserConfiguration());
        fConfiguration.setProperty (
        Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY,
        symbolTable);
    } // <init>(SymbolTable)


    /**
     * Constructs a DOM Builder using the specified symbol table and
     * grammar pool.
     */
    public DOMParserImpl (SymbolTable symbolTable, XMLGrammarPool grammarPool) {
        this (new XIncludeAwareParserConfiguration());
        fConfiguration.setProperty (
        Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY,
        symbolTable);
        fConfiguration.setProperty (
        Constants.XERCES_PROPERTY_PREFIX
        + Constants.XMLGRAMMAR_POOL_PROPERTY,
        grammarPool);
    }

    /**
     * Resets the parser state.
     *
     * @throws SAXException Thrown on initialization error.
     */
    public void reset () {
        super.reset();

        // get state of namespace-declarations parameter.
        fNamespaceDeclarations =
            fConfiguration.getFeature(Constants.DOM_NAMESPACE_DECLARATIONS);

        // DOM Filter
        if (fSkippedElemStack != null) {
            fSkippedElemStack.removeAllElements();
        }

        fRejectedElementDepth = 0;
        fFilterReject = false;
        fSchemaType = null;

    } // reset()

    //
    // DOMParser methods
    //

    public DOMConfiguration getDomConfig (){
        return this;
    }


    /**
     *  When the application provides a filter, the parser will call out to
     * the filter at the completion of the construction of each
     * <code>Element</code> node. The filter implementation can choose to
     * remove the element from the document being constructed (unless the
     * element is the document element) or to terminate the parse early. If
     * the document is being validated when it's loaded the validation
     * happens before the filter is called.
     */
    public LSParserFilter getFilter () {
        return fDOMFilter;
    }

    /**
     *  When the application provides a filter, the parser will call out to
     * the filter at the completion of the construction of each
     * <code>Element</code> node. The filter implementation can choose to
     * remove the element from the document being constructed (unless the
     * element is the document element) or to terminate the parse early. If
     * the document is being validated when it's loaded the validation
     * happens before the filter is called.
     */
    public void setFilter (LSParserFilter filter) {
        fDOMFilter = filter;
        if (fSkippedElemStack == null) {
            fSkippedElemStack = new Stack<>();
        }
    }

    /**
     * Set parameters and properties
     */
    public void setParameter (String name, Object value) throws DOMException {
        // set features

        if(value instanceof Boolean){
            boolean state = ((Boolean)value).booleanValue ();
            try {
                if (name.equalsIgnoreCase (Constants.DOM_COMMENTS)) {
                    fConfiguration.setFeature (INCLUDE_COMMENTS_FEATURE, state);
                }
                else if (name.equalsIgnoreCase (Constants.DOM_DATATYPE_NORMALIZATION)) {
                    fConfiguration.setFeature (NORMALIZE_DATA, state);
                }
                else if (name.equalsIgnoreCase (Constants.DOM_ENTITIES)) {
                    fConfiguration.setFeature (CREATE_ENTITY_REF_NODES, state);
                }
                else if (name.equalsIgnoreCase (Constants.DOM_DISALLOW_DOCTYPE)) {
                    fConfiguration.setFeature (DISALLOW_DOCTYPE_DECL_FEATURE, state);
                }
                else if (name.equalsIgnoreCase (Constants.DOM_SUPPORTED_MEDIATYPES_ONLY)
                || name.equalsIgnoreCase(Constants.DOM_NORMALIZE_CHARACTERS)
                || name.equalsIgnoreCase (Constants.DOM_CHECK_CHAR_NORMALIZATION)
                || name.equalsIgnoreCase (Constants.DOM_CANONICAL_FORM)) {
                    if (state) { // true is not supported
                        String msg =
                        DOMMessageFormatter.formatMessage (
                        DOMMessageFormatter.DOM_DOMAIN,
                        "FEATURE_NOT_SUPPORTED",
                        new Object[] { name });
                        throw new DOMException (DOMException.NOT_SUPPORTED_ERR, msg);
                    }
                    // setting those features to false is no-op
                }
                else if (name.equalsIgnoreCase (Constants.DOM_NAMESPACES)) {
                    fConfiguration.setFeature (NAMESPACES, state);
                }
                else if (name.equalsIgnoreCase (Constants.DOM_INFOSET)) {
                    // Setting false has no effect.
                    if (state) {
                        // true: namespaces, namespace-declarations,
                        // comments, element-content-whitespace
                        fConfiguration.setFeature(NAMESPACES, true);
                        fConfiguration.setFeature(Constants.DOM_NAMESPACE_DECLARATIONS, true);
                        fConfiguration.setFeature(INCLUDE_COMMENTS_FEATURE, true);
                        fConfiguration.setFeature(INCLUDE_IGNORABLE_WHITESPACE, true);

                        // false: validate-if-schema, entities,
                        // datatype-normalization, cdata-sections
                        fConfiguration.setFeature(DYNAMIC_VALIDATION, false);
                        fConfiguration.setFeature(CREATE_ENTITY_REF_NODES, false);
                        fConfiguration.setFeature(NORMALIZE_DATA, false);
                        fConfiguration.setFeature(CREATE_CDATA_NODES_FEATURE, false);
                    }
                }
                else if (name.equalsIgnoreCase(Constants.DOM_CDATA_SECTIONS)) {
                    fConfiguration.setFeature(CREATE_CDATA_NODES_FEATURE, state);
                }
                else if (name.equalsIgnoreCase (Constants.DOM_NAMESPACE_DECLARATIONS)) {
                    fConfiguration.setFeature(Constants.DOM_NAMESPACE_DECLARATIONS, state);
                }
                else if (name.equalsIgnoreCase (Constants.DOM_WELLFORMED)
                || name.equalsIgnoreCase (Constants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS)) {
                    if (!state) { // false is not supported
                        String msg =
                        DOMMessageFormatter.formatMessage (
                        DOMMessageFormatter.DOM_DOMAIN,
                        "FEATURE_NOT_SUPPORTED",
                        new Object[] { name });
                        throw new DOMException (DOMException.NOT_SUPPORTED_ERR, msg);
                    }
                    // setting these features to true is no-op
                    // REVISIT: implement "namespace-declaration" feature
                }
                else if (name.equalsIgnoreCase (Constants.DOM_VALIDATE)) {
                    fConfiguration.setFeature (VALIDATION_FEATURE, state);
                    if (fSchemaType != Constants.NS_DTD) {
                        fConfiguration.setFeature (XMLSCHEMA, state);
                        fConfiguration.setFeature (XMLSCHEMA_FULL_CHECKING, state);
                    }
                    if (state){
                        fConfiguration.setFeature (DYNAMIC_VALIDATION, false);
                    }
                }
                else if (name.equalsIgnoreCase (Constants.DOM_VALIDATE_IF_SCHEMA)) {
                    fConfiguration.setFeature (DYNAMIC_VALIDATION, state);
                    // Note: validation and dynamic validation are mutually exclusive
                    if (state){
                        fConfiguration.setFeature (VALIDATION_FEATURE, false);
                    }
                }
                else if (name.equalsIgnoreCase (Constants.DOM_ELEMENT_CONTENT_WHITESPACE)) {
                    fConfiguration.setFeature (INCLUDE_IGNORABLE_WHITESPACE, state);
                }
                else if (name.equalsIgnoreCase (Constants.DOM_PSVI)){
                    //XSModel - turn on PSVI augmentation
                    fConfiguration.setFeature (PSVI_AUGMENT, true);
                    fConfiguration.setProperty (DOCUMENT_CLASS_NAME,
                    "com.sun.org.apache.xerces.internal.dom.PSVIDocumentImpl");
                }
                else {
                    // Constants.DOM_CHARSET_OVERRIDES_XML_ENCODING feature,
                    // Constants.DOM_SPLIT_CDATA feature,
                    // or any Xerces feature
                    String normalizedName;
                    if (name.equals(NAMESPACE_GROWTH)) {
                        normalizedName = NAMESPACE_GROWTH;
                    }
                    else if (name.equals(TOLERATE_DUPLICATES)) {
                        normalizedName = TOLERATE_DUPLICATES;
                    }
                    else {
                        normalizedName = name.toLowerCase(Locale.ENGLISH);
                    }
                    fConfiguration.setFeature (normalizedName, state);
                }

            }
            catch (XMLConfigurationException e) {
                String msg =
                DOMMessageFormatter.formatMessage (
                DOMMessageFormatter.DOM_DOMAIN,
                "FEATURE_NOT_FOUND",
                new Object[] { name });
                throw new DOMException (DOMException.NOT_FOUND_ERR, msg);
            }
        }
        else { // set properties
            if (name.equalsIgnoreCase (Constants.DOM_ERROR_HANDLER)) {
                if (value instanceof DOMErrorHandler || value == null) {
                    try {
                        fErrorHandler = new DOMErrorHandlerWrapper ((DOMErrorHandler) value);
                        fConfiguration.setProperty (ERROR_HANDLER, fErrorHandler);
                    }
                    catch (XMLConfigurationException e) {}
                }
                else {
                    // REVISIT: type mismatch
                    String msg =
                    DOMMessageFormatter.formatMessage (
                    DOMMessageFormatter.DOM_DOMAIN,
                    "TYPE_MISMATCH_ERR",
                    new Object[] { name });
                    throw new DOMException (DOMException.TYPE_MISMATCH_ERR, msg);
                }

            }
            else if (name.equalsIgnoreCase (Constants.DOM_RESOURCE_RESOLVER)) {
                if (value instanceof LSResourceResolver || value == null) {
                    try {
                        fConfiguration.setProperty (ENTITY_RESOLVER, new DOMEntityResolverWrapper ((LSResourceResolver) value));
                    }
                    catch (XMLConfigurationException e) {}
                }
                else {
                    // REVISIT: type mismatch
                    String msg =
                    DOMMessageFormatter.formatMessage (
                    DOMMessageFormatter.DOM_DOMAIN,
                    "TYPE_MISMATCH_ERR",
                    new Object[] { name });
                    throw new DOMException (DOMException.TYPE_MISMATCH_ERR, msg);
                }

            }
            else if (name.equalsIgnoreCase (Constants.DOM_SCHEMA_LOCATION)) {
                if (value instanceof String || value == null) {
                    try {
                        if (value == null) {
                            fSchemaLocation = null;
                            fConfiguration.setProperty (
                                Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_SOURCE,
                                null);
                        }
                        else {
                            fSchemaLocation = (String)value;
                            // map DOM schema-location to JAXP schemaSource property
                            // tokenize location string
                            StringTokenizer t = new StringTokenizer (fSchemaLocation, " \n\t\r");
                            if (t.hasMoreTokens()) {
                                List<String> locations = new ArrayList<>();
                                locations.add (t.nextToken());
                                while (t.hasMoreTokens()) {
                                    locations.add (t.nextToken());
                                }
                                fConfiguration.setProperty (
                                Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_SOURCE,
                                locations.toArray());
                            }
                            else {
                                fConfiguration.setProperty (
                                Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_SOURCE,
                                value);
                            }
                        }
                    }
                    catch (XMLConfigurationException e) {}
                }
                else {
                    // REVISIT: type mismatch
                    String msg =
                    DOMMessageFormatter.formatMessage (
                    DOMMessageFormatter.DOM_DOMAIN,
                    "TYPE_MISMATCH_ERR",
                    new Object[] { name });
                    throw new DOMException (DOMException.TYPE_MISMATCH_ERR, msg);
                }

            }
            else if (name.equalsIgnoreCase (Constants.DOM_SCHEMA_TYPE)) {
                if (value instanceof String || value == null) {
                    try {
                        if (value == null) {
                            // turn off schema features
                            fConfiguration.setFeature (XMLSCHEMA, false);
                            fConfiguration.setFeature (XMLSCHEMA_FULL_CHECKING, false);
                            // map to JAXP schemaLanguage
                            fConfiguration.setProperty ( Constants.JAXP_PROPERTY_PREFIX
                            + Constants.SCHEMA_LANGUAGE,
                            null);
                            fSchemaType = null;
                        }
                        else if (value.equals (Constants.NS_XMLSCHEMA)) {
                            // turn on schema features
                            fConfiguration.setFeature (XMLSCHEMA, true);
                            fConfiguration.setFeature (XMLSCHEMA_FULL_CHECKING, true);
                            // map to JAXP schemaLanguage
                            fConfiguration.setProperty ( Constants.JAXP_PROPERTY_PREFIX
                            + Constants.SCHEMA_LANGUAGE,
                            Constants.NS_XMLSCHEMA);
                            fSchemaType = Constants.NS_XMLSCHEMA;
                        }
                        else if (value.equals (Constants.NS_DTD)) {
                            // turn off schema features
                            fConfiguration.setFeature (XMLSCHEMA, false);
                            fConfiguration.setFeature (XMLSCHEMA_FULL_CHECKING, false);
                            // map to JAXP schemaLanguage
                            fConfiguration.setProperty ( Constants.JAXP_PROPERTY_PREFIX
                            + Constants.SCHEMA_LANGUAGE,
                            Constants.NS_DTD);
                            fSchemaType = Constants.NS_DTD;
                        }
                    }
                    catch (XMLConfigurationException e) {}
                }
                else {
                    String msg =
                    DOMMessageFormatter.formatMessage (
                    DOMMessageFormatter.DOM_DOMAIN,
                    "TYPE_MISMATCH_ERR",
                    new Object[] { name });
                    throw new DOMException (DOMException.TYPE_MISMATCH_ERR, msg);
                }

            }
            else if (name.equalsIgnoreCase (DOCUMENT_CLASS_NAME)) {
                fConfiguration.setProperty (DOCUMENT_CLASS_NAME, value);
            }
            else {
                // Try to set the property.
                String normalizedName = name.toLowerCase(Locale.ENGLISH);
                try {
                    fConfiguration.setProperty(normalizedName, value);
                    return;
                }
                catch (XMLConfigurationException e) {}

                // If this is a boolean parameter a type mismatch should be thrown.
                try {
                    if (name.equals(NAMESPACE_GROWTH)) {
                        normalizedName = NAMESPACE_GROWTH;
                    }
                    else if (name.equals(TOLERATE_DUPLICATES)) {
                        normalizedName = TOLERATE_DUPLICATES;
                    }
                    fConfiguration.getFeature(normalizedName);
                    throw newTypeMismatchError(name);

                }
                catch (XMLConfigurationException e) {}

                // Parameter is not recognized
                throw newFeatureNotFoundError(name);
            }
        }
    }

    /**
     * Look up the value of a feature or a property.
     */
    public Object getParameter (String name) throws DOMException {
        if (name.equalsIgnoreCase (Constants.DOM_COMMENTS)) {
            return (fConfiguration.getFeature (INCLUDE_COMMENTS_FEATURE))
            ? Boolean.TRUE
            : Boolean.FALSE;
        }
        else if (name.equalsIgnoreCase (Constants.DOM_DATATYPE_NORMALIZATION)) {
            return (fConfiguration.getFeature (NORMALIZE_DATA))
            ? Boolean.TRUE
            : Boolean.FALSE;
        }
        else if (name.equalsIgnoreCase (Constants.DOM_ENTITIES)) {
            return (fConfiguration.getFeature (CREATE_ENTITY_REF_NODES))
            ? Boolean.TRUE
            : Boolean.FALSE;
        }
        else if (name.equalsIgnoreCase (Constants.DOM_NAMESPACES)) {
            return (fConfiguration.getFeature (NAMESPACES))
            ? Boolean.TRUE
            : Boolean.FALSE;
        }
        else if (name.equalsIgnoreCase (Constants.DOM_VALIDATE)) {
            return (fConfiguration.getFeature (VALIDATION_FEATURE))
            ? Boolean.TRUE
            : Boolean.FALSE;
        }
        else if (name.equalsIgnoreCase (Constants.DOM_VALIDATE_IF_SCHEMA)) {
            return (fConfiguration.getFeature (DYNAMIC_VALIDATION))
            ? Boolean.TRUE
            : Boolean.FALSE;
        }
        else if (name.equalsIgnoreCase (Constants.DOM_ELEMENT_CONTENT_WHITESPACE)) {
            return (fConfiguration.getFeature (INCLUDE_IGNORABLE_WHITESPACE))
            ? Boolean.TRUE
            : Boolean.FALSE;
        }
        else if (name.equalsIgnoreCase (Constants.DOM_DISALLOW_DOCTYPE)) {
            return (fConfiguration.getFeature (DISALLOW_DOCTYPE_DECL_FEATURE))
            ? Boolean.TRUE
            : Boolean.FALSE;
        }
        else if (name.equalsIgnoreCase (Constants.DOM_INFOSET)) {
            // REVISIT: This is somewhat expensive to compute
            // but it's possible that the user has a reference
            // to the configuration and is changing the values
            // of these features directly on it.
            boolean infoset = fConfiguration.getFeature(NAMESPACES) &&
                fConfiguration.getFeature(Constants.DOM_NAMESPACE_DECLARATIONS) &&
                fConfiguration.getFeature(INCLUDE_COMMENTS_FEATURE) &&
                fConfiguration.getFeature(INCLUDE_IGNORABLE_WHITESPACE) &&
                !fConfiguration.getFeature(DYNAMIC_VALIDATION) &&
                !fConfiguration.getFeature(CREATE_ENTITY_REF_NODES) &&
                !fConfiguration.getFeature(NORMALIZE_DATA) &&
                !fConfiguration.getFeature(CREATE_CDATA_NODES_FEATURE);
            return (infoset) ? Boolean.TRUE : Boolean.FALSE;
        }
        else if (name.equalsIgnoreCase(Constants.DOM_CDATA_SECTIONS)) {
            return (fConfiguration.getFeature(CREATE_CDATA_NODES_FEATURE))
                ? Boolean.TRUE : Boolean.FALSE;
        }
        else if (name.equalsIgnoreCase(Constants.DOM_CHECK_CHAR_NORMALIZATION ) ||
                 name.equalsIgnoreCase(Constants.DOM_NORMALIZE_CHARACTERS)){
            return Boolean.FALSE;
        }
        else if (name.equalsIgnoreCase(Constants.DOM_NAMESPACE_DECLARATIONS)
        || name.equalsIgnoreCase (Constants.DOM_WELLFORMED)
        || name.equalsIgnoreCase (Constants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS)
        || name.equalsIgnoreCase (Constants.DOM_CANONICAL_FORM)
        || name.equalsIgnoreCase (Constants.DOM_SUPPORTED_MEDIATYPES_ONLY)
        || name.equalsIgnoreCase (Constants.DOM_SPLIT_CDATA)
        || name.equalsIgnoreCase (Constants.DOM_CHARSET_OVERRIDES_XML_ENCODING)) {
            return (fConfiguration.getFeature (name.toLowerCase(Locale.ENGLISH)))
            ? Boolean.TRUE
            : Boolean.FALSE;
        }
        else if (name.equalsIgnoreCase (Constants.DOM_ERROR_HANDLER)) {
            if (fErrorHandler != null) {
                return fErrorHandler.getErrorHandler ();
            }
            return null;
        }
        else if (name.equalsIgnoreCase (Constants.DOM_RESOURCE_RESOLVER)) {
            try {
                XMLEntityResolver entityResolver =
                (XMLEntityResolver) fConfiguration.getProperty (ENTITY_RESOLVER);
                if (entityResolver != null
                && entityResolver instanceof DOMEntityResolverWrapper) {
                    return ((DOMEntityResolverWrapper) entityResolver).getEntityResolver ();
                }
                return null;
            }
            catch (XMLConfigurationException e) {}
        }
        else if (name.equalsIgnoreCase (Constants.DOM_SCHEMA_TYPE)) {
            return fConfiguration.getProperty (
            Constants.JAXP_PROPERTY_PREFIX + Constants.SCHEMA_LANGUAGE);
        }
        else if (name.equalsIgnoreCase (Constants.DOM_SCHEMA_LOCATION)) {
            return fSchemaLocation;
        }
        else if (name.equalsIgnoreCase (SYMBOL_TABLE)){
            return fConfiguration.getProperty (SYMBOL_TABLE);
        }
        else if (name.equalsIgnoreCase (DOCUMENT_CLASS_NAME)) {
            return fConfiguration.getProperty (DOCUMENT_CLASS_NAME);
        }
        else {
            // This could be a recognized feature or property.
            String normalizedName;

            if (name.equals(NAMESPACE_GROWTH)) {
                normalizedName = NAMESPACE_GROWTH;
            }
            else if (name.equals(TOLERATE_DUPLICATES)) {
                normalizedName = TOLERATE_DUPLICATES;
            }
            else {
                normalizedName = name.toLowerCase(Locale.ENGLISH);
            }
            try {
                return fConfiguration.getFeature(normalizedName)
                    ? Boolean.TRUE : Boolean.FALSE;
            }
            catch (XMLConfigurationException e) {}

            // This isn't a feature; perhaps it's a property
            try {
                return fConfiguration.getProperty(normalizedName);
            }
            catch (XMLConfigurationException e) {}

            throw newFeatureNotFoundError(name);
        }
        return null;
    }

    public boolean canSetParameter (String name, Object value) {
        if (value == null){
                return true;
        }

        if(value instanceof Boolean){
            boolean state = ((Boolean)value).booleanValue ();
            if ( name.equalsIgnoreCase (Constants.DOM_SUPPORTED_MEDIATYPES_ONLY)
            || name.equalsIgnoreCase(Constants.DOM_NORMALIZE_CHARACTERS)
            || name.equalsIgnoreCase(Constants.DOM_CHECK_CHAR_NORMALIZATION )
            || name.equalsIgnoreCase (Constants.DOM_CANONICAL_FORM) ) {
                // true is not supported
                return (state) ? false : true;
            }
            else if (name.equalsIgnoreCase (Constants.DOM_WELLFORMED)
            || name.equalsIgnoreCase (Constants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS)) {
                // false is not supported
                return (state) ? true : false;
            }
            else if (name.equalsIgnoreCase (Constants.DOM_CDATA_SECTIONS)
            || name.equalsIgnoreCase (Constants.DOM_CHARSET_OVERRIDES_XML_ENCODING)
            || name.equalsIgnoreCase (Constants.DOM_COMMENTS)
            || name.equalsIgnoreCase (Constants.DOM_DATATYPE_NORMALIZATION)
            || name.equalsIgnoreCase (Constants.DOM_DISALLOW_DOCTYPE)
            || name.equalsIgnoreCase (Constants.DOM_ENTITIES)
            || name.equalsIgnoreCase (Constants.DOM_INFOSET)
            || name.equalsIgnoreCase (Constants.DOM_NAMESPACES)
            || name.equalsIgnoreCase (Constants.DOM_NAMESPACE_DECLARATIONS)
            || name.equalsIgnoreCase (Constants.DOM_VALIDATE)
            || name.equalsIgnoreCase (Constants.DOM_VALIDATE_IF_SCHEMA)
            || name.equalsIgnoreCase (Constants.DOM_ELEMENT_CONTENT_WHITESPACE)
            || name.equalsIgnoreCase (Constants.DOM_XMLDECL)) {
                return true;
            }

            // Recognize Xerces features.
            try {
                String normalizedName;
                if (name.equalsIgnoreCase(NAMESPACE_GROWTH)) {
                    normalizedName = NAMESPACE_GROWTH;
                }
                else if (name.equalsIgnoreCase(TOLERATE_DUPLICATES)) {
                    normalizedName = TOLERATE_DUPLICATES;
                }
                else {
                    normalizedName = name.toLowerCase(Locale.ENGLISH);
                }
                fConfiguration.getFeature(normalizedName);
                return true;
            }
            catch (XMLConfigurationException e) {
                return false;
            }
        }
        else { // check properties
            if (name.equalsIgnoreCase (Constants.DOM_ERROR_HANDLER)) {
                if (value instanceof DOMErrorHandler || value == null) {
                    return true;
                }
                return false;
            }
            else if (name.equalsIgnoreCase (Constants.DOM_RESOURCE_RESOLVER)) {
                if (value instanceof LSResourceResolver || value == null) {
                    return true;
                }
                return false;
            }
            else if (name.equalsIgnoreCase (Constants.DOM_SCHEMA_TYPE)) {
                if ((value instanceof String
                && (value.equals (Constants.NS_XMLSCHEMA)
                || value.equals (Constants.NS_DTD))) || value == null) {
                    return true;
                }
                return false;
            }
            else if (name.equalsIgnoreCase (Constants.DOM_SCHEMA_LOCATION)) {
                if (value instanceof String || value == null)
                    return true;
                return false;
            }
            else if (name.equalsIgnoreCase (DOCUMENT_CLASS_NAME)){
                return true;
            }
            return false;
        }
    }

    /**
     *  DOM Level 3 CR - Experimental.
     *
     *  The list of the parameters supported by this
     * <code>DOMConfiguration</code> object and for which at least one value
     * can be set by the application. Note that this list can also contain
     * parameter names defined outside this specification.
     */
    public DOMStringList getParameterNames () {
        if (fRecognizedParameters == null){
            List<String> parameters = new ArrayList<>();

            // REVISIT: add Xerces recognized properties/features
            parameters.add(Constants.DOM_NAMESPACES);
            parameters.add(Constants.DOM_CDATA_SECTIONS);
            parameters.add(Constants.DOM_CANONICAL_FORM);
            parameters.add(Constants.DOM_NAMESPACE_DECLARATIONS);
            parameters.add(Constants.DOM_SPLIT_CDATA);

            parameters.add(Constants.DOM_ENTITIES);
            parameters.add(Constants.DOM_VALIDATE_IF_SCHEMA);
            parameters.add(Constants.DOM_VALIDATE);
            parameters.add(Constants.DOM_DATATYPE_NORMALIZATION);

            parameters.add(Constants.DOM_CHARSET_OVERRIDES_XML_ENCODING);
            parameters.add(Constants.DOM_CHECK_CHAR_NORMALIZATION);
            parameters.add(Constants.DOM_SUPPORTED_MEDIATYPES_ONLY);
            parameters.add(Constants.DOM_IGNORE_UNKNOWN_CHARACTER_DENORMALIZATIONS);

            parameters.add(Constants.DOM_NORMALIZE_CHARACTERS);
            parameters.add(Constants.DOM_WELLFORMED);
            parameters.add(Constants.DOM_INFOSET);
            parameters.add(Constants.DOM_DISALLOW_DOCTYPE);
            parameters.add(Constants.DOM_ELEMENT_CONTENT_WHITESPACE);
            parameters.add(Constants.DOM_COMMENTS);

            parameters.add(Constants.DOM_ERROR_HANDLER);
            parameters.add(Constants.DOM_RESOURCE_RESOLVER);
            parameters.add(Constants.DOM_SCHEMA_LOCATION);
            parameters.add(Constants.DOM_SCHEMA_TYPE);

            fRecognizedParameters = new DOMStringListImpl(parameters);

        }

        return fRecognizedParameters;
    }

    /**
     * Parse an XML document from a location identified by an URI reference.
     * If the URI contains a fragment identifier (see section 4.1 in ), the
     * behavior is not defined by this specification.
     *
     */
    public Document parseURI (String uri) throws LSException {

        //If DOMParser insstance is already busy parsing another document when this
        // method is called, then raise INVALID_STATE_ERR according to DOM L3 LS spec
        if ( fBusy ) {
            String msg = DOMMessageFormatter.formatMessage (
            DOMMessageFormatter.DOM_DOMAIN,
            "INVALID_STATE_ERR",null);
            throw new DOMException ( DOMException.INVALID_STATE_ERR,msg);
        }

        XMLInputSource source = new XMLInputSource (null, uri, null, false);
        try {
            currentThread = Thread.currentThread();
                        fBusy = true;
            parse (source);
            fBusy = false;
            if (abortNow && currentThread.isInterrupted()) {
                //reset interrupt state
                abortNow = false;
                Thread.interrupted();
            }
        } catch (Exception e){
            fBusy = false;
            if (abortNow && currentThread.isInterrupted()) {
                Thread.interrupted();
            }
            if (abortNow) {
                abortNow = false;
                restoreHandlers();
                return null;
            }
            // Consume this exception if the user
            // issued an interrupt or an abort.
            if (e != Abort.INSTANCE) {
                if (!(e instanceof XMLParseException) && fErrorHandler != null) {
                    DOMErrorImpl error = new DOMErrorImpl ();
                    error.fException = e;
                    error.fMessage = e.getMessage ();
                    error.fSeverity = DOMError.SEVERITY_FATAL_ERROR;
                    fErrorHandler.getErrorHandler ().handleError (error);
                }
                if (DEBUG) {
                    e.printStackTrace ();
                }
                throw (LSException) DOMUtil.createLSException(LSException.PARSE_ERR, e).fillInStackTrace();
            }
        }
        Document doc = getDocument();
        dropDocumentReferences();
        return doc;
    }

    /**
     * Parse an XML document from a resource identified by an
     * <code>LSInput</code>.
     *
     */
    public Document parse (LSInput is) throws LSException {

        // need to wrap the LSInput with an XMLInputSource
        XMLInputSource xmlInputSource = dom2xmlInputSource (is);
        if ( fBusy ) {
            String msg = DOMMessageFormatter.formatMessage (
            DOMMessageFormatter.DOM_DOMAIN,
            "INVALID_STATE_ERR",null);
            throw new DOMException ( DOMException.INVALID_STATE_ERR,msg);
        }

        try {
            currentThread = Thread.currentThread();
                        fBusy = true;
            parse (xmlInputSource);
            fBusy = false;
            if (abortNow && currentThread.isInterrupted()) {
                //reset interrupt state
                abortNow = false;
                Thread.interrupted();
            }
        } catch (Exception e) {
            fBusy = false;
            if (abortNow && currentThread.isInterrupted()) {
                Thread.interrupted();
            }
            if (abortNow) {
                abortNow = false;
                restoreHandlers();
                return null;
            }
            // Consume this exception if the user
            // issued an interrupt or an abort.
            if (e != Abort.INSTANCE) {
                if (!(e instanceof XMLParseException) && fErrorHandler != null) {
                   DOMErrorImpl error = new DOMErrorImpl ();
                   error.fException = e;
                   error.fMessage = e.getMessage ();
                   error.fSeverity = DOMError.SEVERITY_FATAL_ERROR;
                   fErrorHandler.getErrorHandler().handleError (error);
                }
                if (DEBUG) {
                   e.printStackTrace ();
                }
                throw (LSException) DOMUtil.createLSException(LSException.PARSE_ERR, e).fillInStackTrace();
            }
        }
        Document doc = getDocument();
        dropDocumentReferences();
        return doc;
    }


    private void restoreHandlers() {
        fConfiguration.setDocumentHandler(this);
        fConfiguration.setDTDHandler(this);
        fConfiguration.setDTDContentModelHandler(this);
    }

    /**
     *  Parse an XML document or fragment from a resource identified by an
     * <code>LSInput</code> and insert the content into an existing
     * document at the position epcified with the <code>contextNode</code>
     * and <code>action</code> arguments. When parsing the input stream the
     * context node is used for resolving unbound namespace prefixes.
     *
     * @param is  The <code>LSInput</code> from which the source
     *   document is to be read.
     * @param cnode  The <code>Node</code> that is used as the context for
     *   the data that is being parsed.
     * @param action This parameter describes which action should be taken
     *   between the new set of node being inserted and the existing
     *   children of the context node. The set of possible actions is
     *   defined above.
     * @exception DOMException
     *   HIERARCHY_REQUEST_ERR: Thrown if this action results in an invalid
     *   hierarchy (i.e. a Document with more than one document element).
     */
    public Node parseWithContext (LSInput is, Node cnode,
    short action) throws DOMException, LSException {
        // REVISIT: need to implement.
        throw new DOMException (DOMException.NOT_SUPPORTED_ERR, "Not supported");
    }


    /**
     * NON-DOM: convert LSInput to XNIInputSource
     *
     * @param is
     * @return
     */
    XMLInputSource dom2xmlInputSource (LSInput is) {
        // need to wrap the LSInput with an XMLInputSource
        XMLInputSource xis = null;
        // check whether there is a Reader
        // according to DOM, we need to treat such reader as "UTF-16".
        if (is.getCharacterStream () != null) {
            xis = new XMLInputSource (is.getPublicId (), is.getSystemId (),
            is.getBaseURI (), is.getCharacterStream (),
            "UTF-16");
        }
        // check whether there is an InputStream
        else if (is.getByteStream () != null) {
            xis = new XMLInputSource (is.getPublicId (), is.getSystemId (),
            is.getBaseURI (), is.getByteStream (),
            is.getEncoding ());
        }
        // if there is a string data, use a StringReader
        // according to DOM, we need to treat such data as "UTF-16".
        else if (is.getStringData () != null && is.getStringData().length() > 0) {
            xis = new XMLInputSource (is.getPublicId (), is.getSystemId (),
            is.getBaseURI (), new StringReader (is.getStringData ()),
            "UTF-16");
        }
        // otherwise, just use the public/system/base Ids
        else if ((is.getSystemId() != null && is.getSystemId().length() > 0) ||
            (is.getPublicId() != null && is.getPublicId().length() > 0)) {
            xis = new XMLInputSource (is.getPublicId (), is.getSystemId (),
            is.getBaseURI(), false);
        }
        else {
            // all inputs are null
            if (fErrorHandler != null) {
                DOMErrorImpl error = new DOMErrorImpl();
                error.fType = "no-input-specified";
                error.fMessage = "no-input-specified";
                error.fSeverity = DOMError.SEVERITY_FATAL_ERROR;
                fErrorHandler.getErrorHandler().handleError(error);
            }
            throw new LSException(LSException.PARSE_ERR, "no-input-specified");
        }
        return xis;
    }

    /**
     * @see org.w3c.dom.ls.LSParser#getAsync()
     */
    public boolean getAsync () {
        return false;
    }

    /**
     * @see org.w3c.dom.ls.LSParser#getBusy()
     */
    public boolean getBusy () {
        return fBusy;
    }

    /**
     * @see org.w3c.dom.ls.LSParser#abort()
     */
    public void abort () {
        // If parse operation is in progress then reset it
        if ( fBusy ) {
            fBusy = false;
            if(currentThread != null) {
                abortNow = true;
                if (abortHandler == null) {
                    abortHandler = new AbortHandler();
                }
                fConfiguration.setDocumentHandler(abortHandler);
                fConfiguration.setDTDHandler(abortHandler);
                fConfiguration.setDTDContentModelHandler(abortHandler);

                if(currentThread == Thread.currentThread())
                    throw Abort.INSTANCE;

                currentThread.interrupt();
            }
        }
        return; // If not busy then this is noop
    }

    /**
     * The start of an element. If the document specifies the start element
     * by using an empty tag, then the startElement method will immediately
     * be followed by the endElement method, with no intervening methods.
     * Overriding the parent to handle DOM_NAMESPACE_DECLARATIONS=false.
     *
     * @param element    The name of the element.
     * @param attributes The element attributes.
     * @param augs     Additional information that may include infoset augmentations
     *
     * @throws XNIException Thrown by handler to signal an error.
     */
    public void startElement (QName element, XMLAttributes attributes, Augmentations augs) {
        // namespace declarations parameter has no effect if namespaces is false.
        if (!fNamespaceDeclarations && fNamespaceAware) {
            int len = attributes.getLength();
            for (int i = len - 1; i >= 0; --i) {
                if (XMLSymbols.PREFIX_XMLNS == attributes.getPrefix(i) ||
                    XMLSymbols.PREFIX_XMLNS == attributes.getQName(i)) {
                    attributes.removeAttributeAt(i);
                }
            }
        }
        super.startElement(element, attributes, augs);
    }

    private class AbortHandler implements XMLDocumentHandler, XMLDTDHandler, XMLDTDContentModelHandler  {

        private XMLDocumentSource documentSource;
        private XMLDTDContentModelSource dtdContentSource;
        private XMLDTDSource dtdSource;

        public void startDocument(XMLLocator locator, String encoding, NamespaceContext namespaceContext, Augmentations augs) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void xmlDecl(String version, String encoding, String standalone, Augmentations augs) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void doctypeDecl(String rootElement, String publicId, String systemId, Augmentations augs) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void comment(XMLString text, Augmentations augs) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void processingInstruction(String target, XMLString data, Augmentations augs) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void startGeneralEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void textDecl(String version, String encoding, Augmentations augs) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void endGeneralEntity(String name, Augmentations augs) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void characters(XMLString text, Augmentations augs) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void endElement(QName element, Augmentations augs) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void startCDATA(Augmentations augs) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void endCDATA(Augmentations augs) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void endDocument(Augmentations augs) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void setDocumentSource(XMLDocumentSource source) {
            documentSource = source;
        }

        public XMLDocumentSource getDocumentSource() {
            return documentSource;
        }

        public void startDTD(XMLLocator locator, Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void startParameterEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void endParameterEntity(String name, Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void startExternalSubset(XMLResourceIdentifier identifier, Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void endExternalSubset(Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void elementDecl(String name, String contentModel, Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void startAttlist(String elementName, Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void attributeDecl(String elementName, String attributeName, String type, String[] enumeration, String defaultType, XMLString defaultValue, XMLString nonNormalizedDefaultValue, Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void endAttlist(Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void internalEntityDecl(String name, XMLString text, XMLString nonNormalizedText, Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void externalEntityDecl(String name, XMLResourceIdentifier identifier, Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void unparsedEntityDecl(String name, XMLResourceIdentifier identifier, String notation, Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void notationDecl(String name, XMLResourceIdentifier identifier, Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void startConditional(short type, Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void ignoredCharacters(XMLString text, Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void endConditional(Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void endDTD(Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void setDTDSource(XMLDTDSource source) {
            dtdSource = source;
        }

        public XMLDTDSource getDTDSource() {
            return dtdSource;
        }

        public void startContentModel(String elementName, Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void any(Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void empty(Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void startGroup(Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void pcdata(Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void element(String elementName, Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void separator(short separator, Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void occurrence(short occurrence, Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void endGroup(Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void endContentModel(Augmentations augmentations) throws XNIException {
            throw Abort.INSTANCE;
        }

        public void setDTDContentModelSource(XMLDTDContentModelSource source) {
            dtdContentSource = source;
        }

        public XMLDTDContentModelSource getDTDContentModelSource() {
            return dtdContentSource;
        }

    }

    private static DOMException newFeatureNotFoundError(String name) {
        String msg =
            DOMMessageFormatter.formatMessage (
                    DOMMessageFormatter.DOM_DOMAIN,
                    "FEATURE_NOT_FOUND",
                    new Object[] { name });
        return new DOMException (DOMException.NOT_FOUND_ERR, msg);
    }

    private static DOMException newTypeMismatchError(String name) {
        String msg =
            DOMMessageFormatter.formatMessage (
                    DOMMessageFormatter.DOM_DOMAIN,
                    "TYPE_MISMATCH_ERR",
                    new Object[] { name });
        return new DOMException (DOMException.TYPE_MISMATCH_ERR, msg);
    }

} // class DOMParserImpl
