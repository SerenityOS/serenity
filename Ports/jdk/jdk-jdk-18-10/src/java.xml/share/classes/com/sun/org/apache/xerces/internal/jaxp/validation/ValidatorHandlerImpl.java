/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.jaxp.validation;

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.XMLEntityManager;
import com.sun.org.apache.xerces.internal.impl.XMLErrorReporter;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import com.sun.org.apache.xerces.internal.impl.validation.EntityState;
import com.sun.org.apache.xerces.internal.impl.validation.ValidationManager;
import com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaValidator;
import com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl;
import com.sun.org.apache.xerces.internal.util.AttributesProxy;
import com.sun.org.apache.xerces.internal.util.SAXLocatorWrapper;
import com.sun.org.apache.xerces.internal.util.SAXMessageFormatter;
import com.sun.org.apache.xerces.internal.util.Status;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.URI;
import com.sun.org.apache.xerces.internal.util.XMLAttributesImpl;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityManager;
import com.sun.org.apache.xerces.internal.utils.XMLSecurityPropertyManager;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xni.XMLDocumentHandler;
import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDocumentSource;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import com.sun.org.apache.xerces.internal.xs.AttributePSVI;
import com.sun.org.apache.xerces.internal.xs.ElementPSVI;
import com.sun.org.apache.xerces.internal.xs.ItemPSVI;
import com.sun.org.apache.xerces.internal.xs.PSVIProvider;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.TypeInfoProvider;
import javax.xml.validation.ValidatorHandler;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.JdkXmlUtils;
import org.w3c.dom.TypeInfo;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.Attributes2;
import org.xml.sax.ext.EntityResolver2;

/**
 * <p>Implementation of ValidatorHandler for W3C XML Schemas and
 * also a validator helper for <code>SAXSource</code>s.</p>
 *
 * @author Kohsuke Kawaguchi
 * @author Michael Glavassevich, IBM
 *
 * @LastModified: May 2021
 */
final class ValidatorHandlerImpl extends ValidatorHandler implements
    DTDHandler, EntityState, PSVIProvider, ValidatorHelper, XMLDocumentHandler {

    // feature identifiers

    /** Feature identifier: namespace prefixes. */
    private static final String NAMESPACE_PREFIXES =
        Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACE_PREFIXES_FEATURE;

    /** Feature identifier: string interning. */
    protected static final String STRING_INTERNING =
        Constants.SAX_FEATURE_PREFIX + Constants.STRING_INTERNING_FEATURE;

    // property identifiers

    /** Property identifier: error reporter. */
    private static final String ERROR_REPORTER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_REPORTER_PROPERTY;

    /** Property identifier: namespace context. */
    private static final String NAMESPACE_CONTEXT =
        Constants.XERCES_PROPERTY_PREFIX + Constants.NAMESPACE_CONTEXT_PROPERTY;

    /** Property identifier: XML Schema validator. */
    private static final String SCHEMA_VALIDATOR =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SCHEMA_VALIDATOR_PROPERTY;

    /** Property identifier: security manager. */
    private static final String SECURITY_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SECURITY_MANAGER_PROPERTY;

    /** Property identifier: symbol table. */
    private static final String SYMBOL_TABLE =
        Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY;

    /** Property identifier: validation manager. */
    private static final String VALIDATION_MANAGER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.VALIDATION_MANAGER_PROPERTY;

    /** Property identifier: Security property manager. */
    private static final String XML_SECURITY_PROPERTY_MANAGER =
            JdkConstants.XML_SECURITY_PROPERTY_MANAGER;

    //
    // Data
    //

    /** Error reporter. */
    private XMLErrorReporter fErrorReporter;

    /** The namespace context of this document: stores namespaces in scope */
    private NamespaceContext fNamespaceContext;

    /** Schema validator. **/
    private XMLSchemaValidator fSchemaValidator;

    /** Symbol table **/
    private SymbolTable fSymbolTable;

    /** Validation manager. */
    private ValidationManager fValidationManager;

    /** Component manager. **/
    private XMLSchemaValidatorComponentManager fComponentManager;

    /** XML Locator wrapper for SAX. **/
    private final SAXLocatorWrapper fSAXLocatorWrapper = new SAXLocatorWrapper();

    /** Flag used to track whether the namespace context needs to be pushed. */
    private boolean fNeedPushNSContext = true;

    /** Map for tracking unparsed entities. */
    private Map<String, String> fUnparsedEntities = null;

    /** Flag used to track whether XML names and Namespace URIs have been internalized. */
    private boolean fStringsInternalized = false;

    /** Fields for start element, end element and characters. */
    private final QName fElementQName = new QName();
    private final QName fAttributeQName = new QName();
    private final XMLAttributesImpl fAttributes = new XMLAttributesImpl();
    private final AttributesProxy fAttrAdapter = new AttributesProxy(fAttributes);
    private final XMLString fTempString = new XMLString();

    //
    // User Objects
    //

    private ContentHandler fContentHandler = null;

    /*
     * Constructors
     */

    public ValidatorHandlerImpl(XSGrammarPoolContainer grammarContainer) {
        this(new XMLSchemaValidatorComponentManager(grammarContainer));
        fComponentManager.addRecognizedFeatures(new String [] {NAMESPACE_PREFIXES});
        fComponentManager.setFeature(NAMESPACE_PREFIXES, false);
        setErrorHandler(null);
        setResourceResolver(null);
    }

    public ValidatorHandlerImpl(XMLSchemaValidatorComponentManager componentManager) {
        fComponentManager = componentManager;
        fErrorReporter = (XMLErrorReporter) fComponentManager.getProperty(ERROR_REPORTER);
        fNamespaceContext = (NamespaceContext) fComponentManager.getProperty(NAMESPACE_CONTEXT);
        fSchemaValidator = (XMLSchemaValidator) fComponentManager.getProperty(SCHEMA_VALIDATOR);
        fSymbolTable = (SymbolTable) fComponentManager.getProperty(SYMBOL_TABLE);
        fValidationManager = (ValidationManager) fComponentManager.getProperty(VALIDATION_MANAGER);
    }

    /*
     * ValidatorHandler methods
     */

    public void setContentHandler(ContentHandler receiver) {
        fContentHandler = receiver;
    }

    public ContentHandler getContentHandler() {
        return fContentHandler;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        fComponentManager.setErrorHandler(errorHandler);
    }

    public ErrorHandler getErrorHandler() {
        return fComponentManager.getErrorHandler();
    }

    public void setResourceResolver(LSResourceResolver resourceResolver) {
        fComponentManager.setResourceResolver(resourceResolver);
    }

    public LSResourceResolver getResourceResolver() {
        return fComponentManager.getResourceResolver();
    }

    public TypeInfoProvider getTypeInfoProvider() {
        return fTypeInfoProvider;
    }

    public boolean getFeature(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException();
        }
        try {
            return fComponentManager.getFeature(name);
        }
        catch (XMLConfigurationException e) {
            final String identifier = e.getIdentifier();
            final String key = e.getType() == Status.NOT_RECOGNIZED ?
                    "feature-not-recognized" : "feature-not-supported";
            throw new SAXNotRecognizedException(
                    SAXMessageFormatter.formatMessage(fComponentManager.getLocale(),
                    key, new Object [] {identifier}));
        }
    }

    public void setFeature(String name, boolean value)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException();
        }
        try {
            fComponentManager.setFeature(name, value);
        }
        catch (XMLConfigurationException e) {
            final String identifier = e.getIdentifier();
            final String key;
            if (e.getType() == Status.NOT_ALLOWED) {
                //for now, the identifier can only be (XMLConstants.FEATURE_SECURE_PROCESSING)
                throw new SAXNotSupportedException(
                    SAXMessageFormatter.formatMessage(fComponentManager.getLocale(),
                    "jaxp-secureprocessing-feature", null));
            } else if (e.getType() == Status.NOT_RECOGNIZED) {
                key = "feature-not-recognized";
            } else {
                key = "feature-not-supported";
            }
            throw new SAXNotRecognizedException(
                    SAXMessageFormatter.formatMessage(fComponentManager.getLocale(),
                    key, new Object [] {identifier}));
        }
    }

    public Object getProperty(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException();
        }
        try {
            return fComponentManager.getProperty(name);
        }
        catch (XMLConfigurationException e) {
            final String identifier = e.getIdentifier();
            final String key = e.getType() == Status.NOT_RECOGNIZED ?
                    "property-not-recognized" : "property-not-supported";
            throw new SAXNotRecognizedException(
                    SAXMessageFormatter.formatMessage(fComponentManager.getLocale(),
                    key, new Object [] {identifier}));
        }
    }

    public void setProperty(String name, Object object)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException();
        }
        try {
            fComponentManager.setProperty(name, object);
        }
        catch (XMLConfigurationException e) {
            final String identifier = e.getIdentifier();
            final String key = e.getType() == Status.NOT_RECOGNIZED ?
                    "property-not-recognized" : "property-not-supported";
            throw new SAXNotRecognizedException(
                    SAXMessageFormatter.formatMessage(fComponentManager.getLocale(),
                    key, new Object [] {identifier}));
        }
    }

    /*
     * EntityState methods
     */

    public boolean isEntityDeclared(String name) {
        return false;
    }

    public boolean isEntityUnparsed(String name) {
        if (fUnparsedEntities != null) {
            return fUnparsedEntities.containsKey(name);
        }
        return false;
    }

    /*
     * XMLDocumentHandler methods
     */

    public void startDocument(XMLLocator locator, String encoding,
            NamespaceContext namespaceContext, Augmentations augs)
            throws XNIException {
        if (fContentHandler != null) {
            try {
                fContentHandler.startDocument();
            }
            catch (SAXException e) {
                throw new XNIException(e);
            }
        }
    }

    public void xmlDecl(String version, String encoding, String standalone,
            Augmentations augs) throws XNIException {}

    public void doctypeDecl(String rootElement, String publicId,
            String systemId, Augmentations augs) throws XNIException {}

    public void comment(XMLString text, Augmentations augs) throws XNIException {}

    public void processingInstruction(String target, XMLString data,
            Augmentations augs) throws XNIException {
        if (fContentHandler != null) {
            try {
                fContentHandler.processingInstruction(target, data.toString());
            }
            catch (SAXException e) {
                throw new XNIException(e);
            }
        }
    }

    public void startElement(QName element, XMLAttributes attributes,
            Augmentations augs) throws XNIException {
        if (fContentHandler != null) {
            try {
                fTypeInfoProvider.beginStartElement(augs, attributes);
                fContentHandler.startElement((element.uri != null) ? element.uri : XMLSymbols.EMPTY_STRING,
                        element.localpart, element.rawname, fAttrAdapter);
            }
            catch (SAXException e) {
                throw new XNIException(e);
            }
            finally {
                fTypeInfoProvider.finishStartElement();
            }
        }
    }

    public void emptyElement(QName element, XMLAttributes attributes,
            Augmentations augs) throws XNIException {
        /** Split empty element event. **/
        startElement(element, attributes, augs);
        endElement(element, augs);
    }

    public void startGeneralEntity(String name,
            XMLResourceIdentifier identifier, String encoding,
            Augmentations augs) throws XNIException {}

    public void textDecl(String version, String encoding, Augmentations augs)
            throws XNIException {}

    public void endGeneralEntity(String name, Augmentations augs)
            throws XNIException {}

    public void characters(XMLString text, Augmentations augs)
            throws XNIException {
        if (fContentHandler != null) {
            // if the type is union it is possible that we receive
            // a character call with empty data
            if (text.length == 0) {
                return;
            }
            try {
                fContentHandler.characters(text.ch, text.offset, text.length);
            }
            catch (SAXException e) {
                throw new XNIException(e);
            }
        }
    }

    public void ignorableWhitespace(XMLString text, Augmentations augs)
            throws XNIException {
        if (fContentHandler != null) {
            try {
                fContentHandler.ignorableWhitespace(text.ch, text.offset, text.length);
            }
            catch (SAXException e) {
                throw new XNIException(e);
            }
        }
    }

    public void endElement(QName element, Augmentations augs)
            throws XNIException {
        if (fContentHandler != null) {
            try {
                fTypeInfoProvider.beginEndElement(augs);
                fContentHandler.endElement((element.uri != null) ? element.uri : XMLSymbols.EMPTY_STRING,
                        element.localpart, element.rawname);
            }
            catch (SAXException e) {
                throw new XNIException(e);
            }
            finally {
                fTypeInfoProvider.finishEndElement();
            }
        }
    }

    public void startCDATA(Augmentations augs) throws XNIException {}

    public void endCDATA(Augmentations augs) throws XNIException {}

    public void endDocument(Augmentations augs) throws XNIException {
        if (fContentHandler != null) {
            try {
                fContentHandler.endDocument();
            }
            catch (SAXException e) {
                throw new XNIException(e);
            }
        }
    }

    // NO-OP
    public void setDocumentSource(XMLDocumentSource source) {}

    public XMLDocumentSource getDocumentSource() {
        return fSchemaValidator;
    }

    /*
     * ContentHandler methods
     */

    public void setDocumentLocator(Locator locator) {
        fSAXLocatorWrapper.setLocator(locator);
        if (fContentHandler != null) {
            fContentHandler.setDocumentLocator(locator);
        }
    }

    public void startDocument() throws SAXException {
        fComponentManager.reset();
        fSchemaValidator.setDocumentHandler(this);
        fValidationManager.setEntityState(this);
        fTypeInfoProvider.finishStartElement(); // cleans up TypeInfoProvider
        fNeedPushNSContext = true;
        if (fUnparsedEntities != null && !fUnparsedEntities.isEmpty()) {
            // should only clear this if the last document contained unparsed entities
            fUnparsedEntities.clear();
        }
        fErrorReporter.setDocumentLocator(fSAXLocatorWrapper);
        try {
            fSchemaValidator.startDocument(fSAXLocatorWrapper, fSAXLocatorWrapper.getEncoding(), fNamespaceContext, null);
        }
        catch (XMLParseException e) {
            throw Util.toSAXParseException(e);
        }
        catch (XNIException e) {
            throw Util.toSAXException(e);
        }
    }

    public void endDocument() throws SAXException {
        fSAXLocatorWrapper.setLocator(null);
        try {
            fSchemaValidator.endDocument(null);
        }
        catch (XMLParseException e) {
            throw Util.toSAXParseException(e);
        }
        catch (XNIException e) {
            throw Util.toSAXException(e);
        }
    }

    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
        String prefixSymbol;
        String uriSymbol;
        if (!fStringsInternalized) {
            prefixSymbol = (prefix != null) ? fSymbolTable.addSymbol(prefix) : XMLSymbols.EMPTY_STRING;
            uriSymbol = (uri != null && uri.length() > 0) ? fSymbolTable.addSymbol(uri) : null;
        }
        else {
            prefixSymbol = (prefix != null) ? prefix : XMLSymbols.EMPTY_STRING;
            uriSymbol = (uri != null && uri.length() > 0) ? uri : null;
        }
        if (fNeedPushNSContext) {
            fNeedPushNSContext = false;
            fNamespaceContext.pushContext();
        }
        fNamespaceContext.declarePrefix(prefixSymbol, uriSymbol);
        if (fContentHandler != null) {
            fContentHandler.startPrefixMapping(prefix, uri);
        }
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        if (fContentHandler != null) {
            fContentHandler.endPrefixMapping(prefix);
        }
    }

    public void startElement(String uri, String localName, String qName,
            Attributes atts) throws SAXException {
        if (fNeedPushNSContext) {
            fNamespaceContext.pushContext();
        }
        fNeedPushNSContext = true;

        // Fill element QName
        fillQName(fElementQName, uri, localName, qName);

        // Fill XMLAttributes
        if (atts instanceof Attributes2) {
            fillXMLAttributes2((Attributes2) atts);
        }
        else {
            fillXMLAttributes(atts);
        }

        try {
            fSchemaValidator.startElement(fElementQName, fAttributes, null);
        }
        catch (XMLParseException e) {
            throw Util.toSAXParseException(e);
        }
        catch (XNIException e) {
            throw Util.toSAXException(e);
        }
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        fillQName(fElementQName, uri, localName, qName);
        try {
            fSchemaValidator.endElement(fElementQName, null);
        }
        catch (XMLParseException e) {
            throw Util.toSAXParseException(e);
        }
        catch (XNIException e) {
            throw Util.toSAXException(e);
        }
        finally {
            fNamespaceContext.popContext();
        }
    }

    public void characters(char[] ch, int start, int length)
            throws SAXException {
        try {
            fTempString.setValues(ch, start, length);
            fSchemaValidator.characters(fTempString, null);
        }
        catch (XMLParseException e) {
            throw Util.toSAXParseException(e);
        }
        catch (XNIException e) {
            throw Util.toSAXException(e);
        }
    }

    public void ignorableWhitespace(char[] ch, int start, int length)
            throws SAXException {
        try {
            fTempString.setValues(ch, start, length);
            fSchemaValidator.ignorableWhitespace(fTempString, null);
        }
        catch (XMLParseException e) {
            throw Util.toSAXParseException(e);
        }
        catch (XNIException e) {
            throw Util.toSAXException(e);
        }
    }

    public void processingInstruction(String target, String data)
            throws SAXException {
        /**
         * Processing instructions do not participate in schema validation,
         * so just forward the event to the application's content
         * handler.
         */
        if (fContentHandler != null) {
            fContentHandler.processingInstruction(target, data);
        }
    }

    public void skippedEntity(String name) throws SAXException {
        // there seems to be no corresponding method on XMLDocumentFilter.
        // just pass it down to the output, if any.
        if (fContentHandler != null) {
            fContentHandler.skippedEntity(name);
        }
    }

    /*
     * DTDHandler methods
     */

    public void notationDecl(String name, String publicId,
            String systemId) throws SAXException {}

    public void unparsedEntityDecl(String name, String publicId,
            String systemId, String notationName) throws SAXException {
        if (fUnparsedEntities == null) {
            fUnparsedEntities = new HashMap<>();
        }
        fUnparsedEntities.put(name, name);
    }

    /*
     * ValidatorHelper methods
     */

    public void validate(Source source, Result result)
        throws SAXException, IOException {
        if (result instanceof SAXResult || result == null) {
            final SAXSource saxSource = (SAXSource) source;
            final SAXResult saxResult = (SAXResult) result;

            if (result != null) {
                setContentHandler(saxResult.getHandler());
            }

            try {
                XMLReader reader = saxSource.getXMLReader();
                if( reader==null ) {
                    // create one now
                    reader = JdkXmlUtils.getXMLReader(fComponentManager.getFeature(JdkConstants.OVERRIDE_PARSER),
                            fComponentManager.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING));

                    try {
                        // If this is a Xerces SAX parser, set the security manager if there is one
                        if (reader instanceof com.sun.org.apache.xerces.internal.parsers.SAXParser) {
                           XMLSecurityManager securityManager =
                                   (XMLSecurityManager) fComponentManager.getProperty(SECURITY_MANAGER);
                           if (securityManager != null) {
                               try {
                                   reader.setProperty(SECURITY_MANAGER, securityManager);
                               }
                               // Ignore the exception if the security manager cannot be set.
                               catch (SAXException exc) {}
                           }
                           try {
                               XMLSecurityPropertyManager spm = (XMLSecurityPropertyManager)
                                       fComponentManager.getProperty(XML_SECURITY_PROPERTY_MANAGER);
                               reader.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD,
                                       spm.getValue(XMLSecurityPropertyManager.Property.ACCESS_EXTERNAL_DTD));
                           } catch (SAXException exc) {
                               XMLSecurityManager.printWarning(reader.getClass().getName(),
                                       XMLConstants.ACCESS_EXTERNAL_DTD, exc);
                           }
                        }

                        // Passing on the CatalogFeatures settings from a configuration object to the reader
                        JdkXmlUtils.catalogFeaturesConfig2Reader(fComponentManager, reader);
                    } catch( Exception e ) {
                        // this is impossible, but better safe than sorry
                        throw new FactoryConfigurationError(e);
                    }
                }

                // If XML names and Namespace URIs are already internalized we
                // can avoid running them through the SymbolTable.
                try {
                    fStringsInternalized = reader.getFeature(STRING_INTERNING);
                }
                catch (SAXException exc) {
                    // The feature isn't recognized or getting it is not supported.
                    // In either case, assume that strings are not internalized.
                    fStringsInternalized = false;
                }

                ErrorHandler errorHandler = fComponentManager.getErrorHandler();
                reader.setErrorHandler(errorHandler != null ? errorHandler : DraconianErrorHandler.getInstance());
                reader.setEntityResolver(fResolutionForwarder);
                fResolutionForwarder.setEntityResolver(fComponentManager.getResourceResolver());
                reader.setContentHandler(this);
                reader.setDTDHandler(this);

                InputSource is = saxSource.getInputSource();
                reader.parse(is);
            }
            finally {
                // release the reference to user's handler ASAP
                setContentHandler(null);
            }
            return;
        }
        throw new IllegalArgumentException(JAXPValidationMessageFormatter.formatMessage(fComponentManager.getLocale(),
                "SourceResultMismatch",
                new Object [] {source.getClass().getName(), result.getClass().getName()}));
    }

    /*
     * PSVIProvider methods
     */

    public ElementPSVI getElementPSVI() {
        return fTypeInfoProvider.getElementPSVI();
    }

    public AttributePSVI getAttributePSVI(int index) {
        return fTypeInfoProvider.getAttributePSVI(index);
    }

    public AttributePSVI getAttributePSVIByName(String uri, String localname) {
        return fTypeInfoProvider.getAttributePSVIByName(uri, localname);
    }

    //
    //
    // helper methods
    //
    //

    /** Fills in a QName object. */
    private void fillQName(QName toFill, String uri, String localpart, String raw) {
        if (!fStringsInternalized) {
            uri = (uri != null && uri.length() > 0) ? fSymbolTable.addSymbol(uri) : null;
            localpart = (localpart != null) ? fSymbolTable.addSymbol(localpart) : XMLSymbols.EMPTY_STRING;
            raw = (raw != null) ? fSymbolTable.addSymbol(raw) : XMLSymbols.EMPTY_STRING;
        }
        else {
            if (uri != null && uri.length() == 0) {
                uri = null;
            }
            if (localpart == null) {
                localpart = XMLSymbols.EMPTY_STRING;
            }
            if (raw == null) {
                raw = XMLSymbols.EMPTY_STRING;
            }
        }
        String prefix = XMLSymbols.EMPTY_STRING;
        int prefixIdx = raw.indexOf(':');
        if (prefixIdx != -1) {
            prefix = fSymbolTable.addSymbol(raw.substring(0, prefixIdx));
        }
        toFill.setValues(prefix, localpart, raw, uri);
    }

    /** Fills in the XMLAttributes object. */
    private void fillXMLAttributes(Attributes att) {
        fAttributes.removeAllAttributes();
        final int len = att.getLength();
        for (int i = 0; i < len; ++i) {
            fillXMLAttribute(att, i);
            fAttributes.setSpecified(i, true);
        }
    }

    /** Fills in the XMLAttributes object. */
    private void fillXMLAttributes2(Attributes2 att) {
        fAttributes.removeAllAttributes();
        final int len = att.getLength();
        for (int i = 0; i < len; ++i) {
            fillXMLAttribute(att, i);
            fAttributes.setSpecified(i, att.isSpecified(i));
            if (att.isDeclared(i)) {
                fAttributes.getAugmentations(i).putItem(Constants.ATTRIBUTE_DECLARED, Boolean.TRUE);
            }
        }
    }

    /** Adds an attribute to the XMLAttributes object. */
    private void fillXMLAttribute(Attributes att, int index) {
        fillQName(fAttributeQName, att.getURI(index), att.getLocalName(index), att.getQName(index));
        String type = att.getType(index);
        fAttributes.addAttributeNS(fAttributeQName, (type != null) ? type : XMLSymbols.fCDATASymbol, att.getValue(index));
    }

    /**
     * {@link TypeInfoProvider} implementation.
     *
     * REVISIT: I'm not sure if this code should belong here.
     */
    private final XMLSchemaTypeInfoProvider fTypeInfoProvider = new XMLSchemaTypeInfoProvider();
    private class XMLSchemaTypeInfoProvider extends TypeInfoProvider {

        /** Element augmentations: contains ElementPSVI. **/
        private Augmentations fElementAugs;

        /** Attributes: augmentations for each attribute contain AttributePSVI. **/
        private XMLAttributes fAttributes;

        /** In start element. **/
        private boolean fInStartElement = false;

        /** In end element. **/
        private boolean fInEndElement = false;

        /** Initializes the TypeInfoProvider with type information for the current element. **/
        void beginStartElement(Augmentations elementAugs, XMLAttributes attributes) {
            fInStartElement = true;
            fElementAugs = elementAugs;
            fAttributes = attributes;
        }

        /** Cleanup at the end of start element. **/
        void finishStartElement() {
            fInStartElement = false;
            fElementAugs = null;
            fAttributes = null;
        }

        /** Initializes the TypeInfoProvider with type information for the current element. **/
        void beginEndElement(Augmentations elementAugs) {
            fInEndElement = true;
            fElementAugs = elementAugs;
        }

        /** Cleanup at the end of end element. **/
        void finishEndElement() {
            fInEndElement = false;
            fElementAugs = null;
        }

        /**
         * Throws a {@link IllegalStateException} if we are not in
         * the startElement callback. the JAXP API requires this
         * for most of the public methods.
         */
        private void checkState(boolean forElementInfo) {
            if (! (fInStartElement || (fInEndElement && forElementInfo))) {
                throw new IllegalStateException(JAXPValidationMessageFormatter.formatMessage(fComponentManager.getLocale(),
                        "TypeInfoProviderIllegalState", null));
            }
        }

        public TypeInfo getAttributeTypeInfo(int index) {
            checkState(false);
            return getAttributeType(index);
        }

        private TypeInfo getAttributeType( int index ) {
            checkState(false);
            if( index<0 || fAttributes.getLength()<=index )
                throw new IndexOutOfBoundsException(Integer.toString(index));
            Augmentations augs = fAttributes.getAugmentations(index);
            if (augs == null) return null;
            AttributePSVI psvi = (AttributePSVI)augs.getItem(Constants.ATTRIBUTE_PSVI);
            return getTypeInfoFromPSVI(psvi);
        }

        public TypeInfo getAttributeTypeInfo(String attributeUri, String attributeLocalName) {
            checkState(false);
            return getAttributeTypeInfo(fAttributes.getIndex(attributeUri,attributeLocalName));
        }

        public TypeInfo getAttributeTypeInfo(String attributeQName) {
            checkState(false);
            return getAttributeTypeInfo(fAttributes.getIndex(attributeQName));
        }

        public TypeInfo getElementTypeInfo() {
            checkState(true);
            if (fElementAugs == null) return null;
            ElementPSVI psvi = (ElementPSVI)fElementAugs.getItem(Constants.ELEMENT_PSVI);
            return getTypeInfoFromPSVI(psvi);
        }

        private TypeInfo getTypeInfoFromPSVI( ItemPSVI psvi ) {
            if(psvi==null)  return null;

            // TODO: make sure if this is correct.
            // TODO: since the number of types in a schema is quite limited,
            // TypeInfoImpl should be pooled. Even better, it should be a part
            // of the element decl.
            if( psvi.getValidity()== ElementPSVI.VALIDITY_VALID ) {
                XSTypeDefinition t = psvi.getMemberTypeDefinition();
                if (t != null) {
                    return (t instanceof TypeInfo) ? (TypeInfo) t : null;
                }
            }

            XSTypeDefinition t = psvi.getTypeDefinition();
            // TODO: can t be null?
            if (t != null) {
                return (t instanceof TypeInfo) ? (TypeInfo) t : null;
            }
            return null;
        }

        public boolean isIdAttribute(int index) {
            checkState(false);
            XSSimpleType type = (XSSimpleType)getAttributeType(index);
            if(type==null)  return false;
            return type.isIDType();
        }

        public boolean isSpecified(int index) {
            checkState(false);
            return fAttributes.isSpecified(index);
        }

        /*
         * Other methods
         */

        // PSVIProvider support
        ElementPSVI getElementPSVI() {
            return (fElementAugs != null) ? (ElementPSVI) fElementAugs.getItem(Constants.ELEMENT_PSVI) : null;
        }

        AttributePSVI getAttributePSVI(int index) {
            if (fAttributes != null) {
                Augmentations augs = fAttributes.getAugmentations(index);
                if (augs != null) {
                    return (AttributePSVI) augs.getItem(Constants.ATTRIBUTE_PSVI);
                }
            }
            return null;
        }

        AttributePSVI getAttributePSVIByName(String uri, String localname) {
            if (fAttributes != null) {
                Augmentations augs = fAttributes.getAugmentations(uri, localname);
                if (augs != null) {
                    return (AttributePSVI) augs.getItem(Constants.ATTRIBUTE_PSVI);
                }
            }
            return null;
        }
    }

    /** SAX adapter for an LSResourceResolver. */
    private final ResolutionForwarder fResolutionForwarder = new ResolutionForwarder(null);
    static final class ResolutionForwarder
        implements EntityResolver2 {

        //
        // Data
        //

        /** XML 1.0 type constant according to DOM L3 LS REC spec "http://www.w3.org/TR/2004/REC-DOM-Level-3-LS-20040407/" */
        private static final String XML_TYPE = "http://www.w3.org/TR/REC-xml";

        /** The DOM entity resolver. */
        protected LSResourceResolver fEntityResolver;

        //
        // Constructors
        //

        /** Default constructor. */
        public ResolutionForwarder() {}

        /** Wraps the specified DOM entity resolver. */
        public ResolutionForwarder(LSResourceResolver entityResolver) {
            setEntityResolver(entityResolver);
        }

        //
        // Public methods
        //

        /** Sets the DOM entity resolver. */
        public void setEntityResolver(LSResourceResolver entityResolver) {
            fEntityResolver = entityResolver;
        } // setEntityResolver(LSResourceResolver)

        /** Returns the DOM entity resolver. */
        public LSResourceResolver getEntityResolver() {
            return fEntityResolver;
        } // getEntityResolver():LSResourceResolver

        /**
         * Always returns <code>null</code>. An LSResourceResolver has no corresponding method.
         */
        public InputSource getExternalSubset(String name, String baseURI)
                throws SAXException, IOException {
            return null;
        }

        /**
         * Resolves the given resource and adapts the <code>LSInput</code>
         * returned into an <code>InputSource</code>.
         */
        public InputSource resolveEntity(String name, String publicId,
                String baseURI, String systemId) throws SAXException, IOException {
            if (fEntityResolver != null) {
                LSInput lsInput = fEntityResolver.resolveResource(XML_TYPE, null, publicId, systemId, baseURI);
                if (lsInput != null) {
                    final String pubId = lsInput.getPublicId();
                    final String sysId = lsInput.getSystemId();
                    final String baseSystemId = lsInput.getBaseURI();
                    final Reader charStream = lsInput.getCharacterStream();
                    final InputStream byteStream = lsInput.getByteStream();
                    final String data = lsInput.getStringData();
                    final String encoding = lsInput.getEncoding();

                    /**
                     * An LSParser looks at inputs specified in LSInput in
                     * the following order: characterStream, byteStream,
                     * stringData, systemId, publicId. For consistency
                     * with the DOM Level 3 Load and Save Recommendation
                     * use the same lookup order here.
                     */
                    InputSource inputSource = new InputSource();
                    inputSource.setPublicId(pubId);
                    inputSource.setSystemId((baseSystemId != null) ? resolveSystemId(sysId, baseSystemId) : sysId);

                    if (charStream != null) {
                        inputSource.setCharacterStream(charStream);
                    }
                    else if (byteStream != null) {
                        inputSource.setByteStream(byteStream);
                    }
                    else if (data != null && data.length() != 0) {
                        inputSource.setCharacterStream(new StringReader(data));
                    }
                    inputSource.setEncoding(encoding);
                    return inputSource;
                }
            }
            return null;
        }

        /** Delegates to EntityResolver2.resolveEntity(String, String, String, String). */
        public InputSource resolveEntity(String publicId, String systemId)
                throws SAXException, IOException {
            return resolveEntity(null, publicId, null, systemId);
        }

        /** Resolves a system identifier against a base URI. */
        private String resolveSystemId(String systemId, String baseURI) {
            try {
                return XMLEntityManager.expandSystemId(systemId, baseURI, false);
            }
            // In the event that resolution failed against the
            // base URI, just return the system id as is. There's not
            // much else we can do.
            catch (URI.MalformedURIException ex) {
                return systemId;
            }
        }
    }
}
