/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xalan.internal.xsltc.trax;

import com.sun.org.apache.xalan.internal.xsltc.compiler.CompilerException;
import com.sun.org.apache.xalan.internal.xsltc.compiler.Parser;
import com.sun.org.apache.xalan.internal.xsltc.compiler.SourceLoader;
import com.sun.org.apache.xalan.internal.xsltc.compiler.Stylesheet;
import com.sun.org.apache.xalan.internal.xsltc.compiler.SyntaxTreeNode;
import com.sun.org.apache.xalan.internal.xsltc.compiler.XSLTC;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import java.util.ArrayList;
import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.TemplatesHandler;
import jdk.xml.internal.JdkConstants;
import jdk.xml.internal.JdkXmlFeatures;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Implementation of a JAXP1.1 TemplatesHandler
 * @author Morten Jorgensen
 * @author Santiago Pericas-Geertsen
 * @LastModified: May 2021
 */
public class TemplatesHandlerImpl
    implements ContentHandler, TemplatesHandler, SourceLoader
{
    /**
     * System ID for this stylesheet.
     */
    private String _systemId;

    /**
     * Number of spaces to add for output indentation.
     */
    private int _indentNumber;

    /**
     * This URIResolver is passed to all Transformers.
     */
    private URIResolver _uriResolver = null;

    /**
     * A reference to the transformer factory that this templates
     * object belongs to.
     */
    private TransformerFactoryImpl _tfactory = null;

    /**
     * A reference to XSLTC's parser object.
     */
    private Parser _parser = null;

    /**
     * The created Templates object.
     */
    private TemplatesImpl _templates = null;

    // Catalog features
    CatalogFeatures _catalogFeatures;

    // Catalog is enabled by default
    boolean _useCatalog = true;

    /**
     * Default constructor
     */
    protected TemplatesHandlerImpl(int indentNumber, TransformerFactoryImpl tfactory,
            boolean hasUserErrListener)
    {
        _indentNumber = indentNumber;
        _tfactory = tfactory;

        // Instantiate XSLTC and get reference to parser object
        XSLTC xsltc = new XSLTC(tfactory.getJdkXmlFeatures(), hasUserErrListener);
        if (tfactory.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING))
            xsltc.setSecureProcessing(true);

        xsltc.setProperty(XMLConstants.ACCESS_EXTERNAL_STYLESHEET,
                (String)tfactory.getAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET));
        xsltc.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD,
                (String)tfactory.getAttribute(XMLConstants.ACCESS_EXTERNAL_DTD));
        xsltc.setProperty(JdkConstants.SECURITY_MANAGER,
                tfactory.getAttribute(JdkConstants.SECURITY_MANAGER));


        if ("true".equals(tfactory.getAttribute(TransformerFactoryImpl.ENABLE_INLINING)))
            xsltc.setTemplateInlining(true);
        else
            xsltc.setTemplateInlining(false);

        _useCatalog = tfactory.getFeature(XMLConstants.USE_CATALOG);
        _catalogFeatures = (CatalogFeatures)tfactory.getAttribute(JdkXmlFeatures.CATALOG_FEATURES);
        xsltc.setProperty(JdkXmlFeatures.CATALOG_FEATURES, _catalogFeatures);

        _parser = xsltc.getParser();
    }

    /**
     * Implements javax.xml.transform.sax.TemplatesHandler.getSystemId()
     * Get the base ID (URI or system ID) from where relative URLs will be
     * resolved.
     * @return The systemID that was set with setSystemId(String id)
     */
    public String getSystemId() {
        return _systemId;
    }

    /**
     * Implements javax.xml.transform.sax.TemplatesHandler.setSystemId()
     * Get the base ID (URI or system ID) from where relative URLs will be
     * resolved.
     * @param id Base URI for this stylesheet
     */
    public void setSystemId(String id) {
        _systemId = id;
    }

    /**
     * Store URIResolver needed for Transformers.
     */
    public void setURIResolver(URIResolver resolver) {
        _uriResolver = resolver;
    }

    /**
     * Implements javax.xml.transform.sax.TemplatesHandler.getTemplates()
     * When a TemplatesHandler object is used as a ContentHandler or
     * DocumentHandler for the parsing of transformation instructions, it
     * creates a Templates object, which the caller can get once the SAX
     * events have been completed.
     * @return The Templates object that was created during the SAX event
     *         process, or null if no Templates object has been created.
     */
    public Templates getTemplates() {
        return _templates;
    }

    /**
     * This method implements XSLTC's SourceLoader interface. It is used to
     * glue a TrAX URIResolver to the XSLTC compiler's Input and Import classes.
     *
     * @param href The URI of the document to load
     * @param context The URI of the currently loaded document
     * @param xsltc The compiler that resuests the document
     * @return An InputSource with the loaded document
     */
    public InputSource loadSource(String href, String context, XSLTC xsltc) {
        try {
            // A _uriResolver must be set if this method is called
            final Source source = _uriResolver.resolve(href, context);
            if (source != null) {
                return Util.getInputSource(xsltc, source);
            }
        }
        catch (TransformerException e) {
            // Falls through
        }
        return null;
    }

    // -- ContentHandler --------------------------------------------------

    /**
     * Re-initialize parser and forward SAX2 event.
     */
    public void startDocument() {
        XSLTC xsltc = _parser.getXSLTC();
        xsltc.init();   // calls _parser.init()
        xsltc.setOutputType(XSLTC.BYTEARRAY_OUTPUT);
        _parser.startDocument();
    }

    /**
     * Just forward SAX2 event to parser object.
     */
    public void endDocument() throws SAXException {
        _parser.endDocument();

        // create the templates
        try {
            XSLTC xsltc = _parser.getXSLTC();

            // Set the translet class name if not already set
            String transletName;
            if (_systemId != null) {
                transletName = Util.baseName(_systemId);
            }
            else {
                transletName = (String)_tfactory.getAttribute("translet-name");
            }
            xsltc.setClassName(transletName);

            // Get java-legal class name from XSLTC module
            transletName = xsltc.getClassName();

            Stylesheet stylesheet = null;
            SyntaxTreeNode root = _parser.getDocumentRoot();

            // Compile the translet - this is where the work is done!
            if (!_parser.errorsFound() && root != null) {
                // Create a Stylesheet element from the root node
                stylesheet = _parser.makeStylesheet(root);
                stylesheet.setSystemId(_systemId);
                stylesheet.setParentStylesheet(null);

                if (xsltc.getTemplateInlining())
                   stylesheet.setTemplateInlining(true);
                else
                   stylesheet.setTemplateInlining(false);

                // Set a document loader (for xsl:include/import) if defined
                if (_uriResolver != null || (_useCatalog &&
                        _catalogFeatures.get(CatalogFeatures.Feature.FILES) != null)) {
                    stylesheet.setSourceLoader(this);
                }

                _parser.setCurrentStylesheet(stylesheet);

                // Set it as top-level in the XSLTC object
                xsltc.setStylesheet(stylesheet);

                // Create AST under the Stylesheet element
                _parser.createAST(stylesheet);
            }

            // Generate the bytecodes and output the translet class(es)
            if (!_parser.errorsFound() && stylesheet != null) {
                stylesheet.setMultiDocument(xsltc.isMultiDocument());
                stylesheet.setHasIdCall(xsltc.hasIdCall());

                // Class synchronization is needed for BCEL
                synchronized (xsltc.getClass()) {
                    stylesheet.translate();
                }
            }

            if (!_parser.errorsFound()) {
                // Check that the transformation went well before returning
                final byte[][] bytecodes = xsltc.getBytecodes();
                if (bytecodes != null) {
                    _templates =
                    new TemplatesImpl(xsltc.getBytecodes(), transletName,
                        _parser.getOutputProperties(), _indentNumber, _tfactory);

                    // Set URIResolver on templates object
                    if (_uriResolver != null) {
                        _templates.setURIResolver(_uriResolver);
                    }
                }
            }
            else {
                StringBuilder errorMessage = new StringBuilder();
                ArrayList<ErrorMsg> errors = _parser.getErrors();
                final int count = errors.size();
                for (int i = 0; i < count; i++) {
                    if (errorMessage.length() > 0)
                        errorMessage.append('\n');
                    errorMessage.append(errors.get(i).toString());
                }
                throw new SAXException(ErrorMsg.JAXP_COMPILE_ERR, new TransformerException(errorMessage.toString()));
            }
        }
        catch (CompilerException e) {
            throw new SAXException(ErrorMsg.JAXP_COMPILE_ERR, e);
        }
    }

    /**
     * Just forward SAX2 event to parser object.
     */
    public void startPrefixMapping(String prefix, String uri) {
        _parser.startPrefixMapping(prefix, uri);
    }

    /**
     * Just forward SAX2 event to parser object.
     */
    public void endPrefixMapping(String prefix) {
        _parser.endPrefixMapping(prefix);
    }

    /**
     * Just forward SAX2 event to parser object.
     */
    public void startElement(String uri, String localname, String qname,
        Attributes attributes) throws SAXException
    {
        _parser.startElement(uri, localname, qname, attributes);
    }

    /**
     * Just forward SAX2 event to parser object.
     */
    public void endElement(String uri, String localname, String qname) {
        _parser.endElement(uri, localname, qname);
    }

    /**
     * Just forward SAX2 event to parser object.
     */
    public void characters(char[] ch, int start, int length) {
        _parser.characters(ch, start, length);
    }

    /**
     * Just forward SAX2 event to parser object.
     */
    public void processingInstruction(String name, String value) {
        _parser.processingInstruction(name, value);
    }

    /**
     * Just forward SAX2 event to parser object.
     */
    public void ignorableWhitespace(char[] ch, int start, int length) {
        _parser.ignorableWhitespace(ch, start, length);
    }

    /**
     * Just forward SAX2 event to parser object.
     */
    public void skippedEntity(String name) {
        _parser.skippedEntity(name);
    }

    /**
     * Set internal system Id and forward SAX2 event to parser object.
     */
    public void setDocumentLocator(Locator locator) {
        setSystemId(locator.getSystemId());
        _parser.setDocumentLocator(locator);
    }
}
