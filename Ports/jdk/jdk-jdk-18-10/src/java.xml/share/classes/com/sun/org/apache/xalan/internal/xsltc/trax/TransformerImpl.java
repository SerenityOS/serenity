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

import com.sun.org.apache.xalan.internal.utils.XMLSecurityManager;
import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.DOMCache;
import com.sun.org.apache.xalan.internal.xsltc.StripFilter;
import com.sun.org.apache.xalan.internal.xsltc.Translet;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import com.sun.org.apache.xalan.internal.xsltc.dom.DOMWSFilter;
import com.sun.org.apache.xalan.internal.xsltc.dom.SAXImpl;
import com.sun.org.apache.xalan.internal.xsltc.dom.XSLTCDTMManager;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xalan.internal.xsltc.runtime.output.TransletOutputHandlerFactory;
import com.sun.org.apache.xml.internal.dtm.DTMWSFilter;
import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import com.sun.org.apache.xml.internal.utils.SystemIDResolver;
import com.sun.org.apache.xml.internal.utils.XMLReaderManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownServiceException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogException;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogManager;
import javax.xml.catalog.CatalogResolver;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import jdk.xml.internal.JdkConstants;
import static jdk.xml.internal.JdkConstants.SP_XSLTC_IS_STANDALONE;
import jdk.xml.internal.JdkProperty;
import jdk.xml.internal.JdkXmlFeatures;
import jdk.xml.internal.JdkXmlUtils;
import jdk.xml.internal.JdkProperty.ImplPropMap;
import jdk.xml.internal.JdkProperty.State;
import jdk.xml.internal.SecuritySupport;
import jdk.xml.internal.TransformErrorListener;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

/**
 * @author Morten Jorgensen
 * @author G. Todd Miller
 * @author Santiago Pericas-Geertsen
 * @LastModified: June 2021
 */
public final class TransformerImpl extends Transformer
    implements DOMCache
{

    private final static String LEXICAL_HANDLER_PROPERTY =
        "http://xml.org/sax/properties/lexical-handler";

    /**
     * Namespace prefixes feature for {@link XMLReader}.
     */
    private static final String NAMESPACE_PREFIXES_FEATURE =
        "http://xml.org/sax/features/namespace-prefixes";

    /**
     * A reference to the translet or null if the identity transform.
     */
    private AbstractTranslet _translet = null;

    /**
     * The output method of this transformation.
     */
    private String _method = null;

    /**
     * The output encoding of this transformation.
     */
    private String _encoding = null;

    /**
     * The systemId set in input source.
     */
    private String _sourceSystemId = null;

    /**
     * Default error listener
     */
    private final ErrorListener _defaultListener = new TransformErrorListener();

    /**
     * An error listener for runtime errors.
     */
    private ErrorListener _errorListener = _defaultListener;

    /**
     * A reference to a URI resolver for calls to document().
     */
    private URIResolver _uriResolver = null;

    /**
     * Output properties of this transformer instance.
     */
    private Properties _properties, _propertiesClone;

    /**
     * A reference to an output handler factory.
     */
    private TransletOutputHandlerFactory _tohFactory = null;

    /**
     * A reference to a internal DOM representation of the input.
     */
    private DOM _dom = null;

    /**
     * Number of indent spaces to add when indentation is on.
     */
    private int _indentNumber = -1;

    /**
     * A reference to the transformer factory that this templates
     * object belongs to.
     */
    private TransformerFactoryImpl _tfactory = null;

    /**
     * A reference to the output stream, if we create one in our code.
     */
    private OutputStream _ostream = null;

    /**
     * A reference to the XSLTCDTMManager which is used to build the DOM/DTM
     * for this transformer.
     */
    private XSLTCDTMManager _dtmManager = null;

    /**
     * A reference to an object that creates and caches XMLReader objects.
     */
    private XMLReaderManager _readerManager;

    /**
     * A flag indicating whether we use incremental building of the DTM.
     */
    //private boolean _isIncremental = false;

    /**
     * A flag indicating whether this transformer implements the identity
     * transform.
     */
    private boolean _isIdentity = false;

    /**
     * State of the secure processing feature.
     */
    private boolean _isSecureProcessing = false;

    /**
     * Indicates whether 3rd party parser may be used to override the system-default
     */
    private boolean _overrideDefaultParser;

     /**
     * protocols allowed for external DTD references in source file and/or stylesheet.
     */
    private String _accessExternalDTD = JdkConstants.EXTERNAL_ACCESS_DEFAULT;

    private XMLSecurityManager _securityManager;
    /**
     * A map to store parameters for the identity transform. These
     * are not needed during the transformation, but we must keep track of
     * them to be fully complaint with the JAXP API.
     */
    private Map<String, Object> _parameters = null;

    // Catalog features
    CatalogFeatures _catalogFeatures;
    CatalogResolver _catalogUriResolver;

    // Catalog is enabled by default
    boolean _useCatalog = true;

    int _cdataChunkSize = JdkConstants.CDATA_CHUNK_SIZE_DEFAULT;

    // OutputProperty/Impl-specific property: xsltcIsStandalone
    JdkProperty<String> _xsltcIsStandalone;

    /**
     * This class wraps an ErrorListener into a MessageHandler in order to
     * capture messages reported via xsl:message.
     */
    static class MessageHandler
           extends com.sun.org.apache.xalan.internal.xsltc.runtime.MessageHandler
    {
        private ErrorListener _errorListener;

        public MessageHandler(ErrorListener errorListener) {
            _errorListener = errorListener;
        }

        @Override
        public void displayMessage(String msg) {
            if(_errorListener == null) {
                System.err.println(msg);
            }
            else {
                try {
                    _errorListener.warning(new TransformerException(msg));
                }
                catch (TransformerException e) {
                    // ignored
                }
            }
        }

        public ErrorListener getErrorListener() {
            return _errorListener;
        }
    }

    protected TransformerImpl(Properties outputProperties, int indentNumber,
        TransformerFactoryImpl tfactory)
    {
        this(null, outputProperties, indentNumber, tfactory);
        _isIdentity = true;
        // _properties.put(OutputKeys.METHOD, "xml");
    }

    protected TransformerImpl(Translet translet, Properties outputProperties,
        int indentNumber, TransformerFactoryImpl tfactory)
    {
        _translet = (AbstractTranslet) translet;
        if (_translet != null) {
            _translet.setMessageHandler(new MessageHandler(_errorListener));
        }
        _properties = createOutputProperties(outputProperties);
        String isStandalone = SecuritySupport.getJAXPSystemProperty(
                String.class, SP_XSLTC_IS_STANDALONE, "no");
        _xsltcIsStandalone = new JdkProperty<>(ImplPropMap.XSLTCISSTANDALONE,
                isStandalone, State.DEFAULT);
        _propertiesClone = (Properties) _properties.clone();
        _indentNumber = indentNumber;
        _tfactory = tfactory;
        _overrideDefaultParser = _tfactory.overrideDefaultParser();
        _accessExternalDTD = (String)_tfactory.getAttribute(XMLConstants.ACCESS_EXTERNAL_DTD);
        _securityManager = (XMLSecurityManager)_tfactory.getAttribute(JdkConstants.SECURITY_MANAGER);
        _readerManager = XMLReaderManager.getInstance(_overrideDefaultParser);
        _readerManager.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, _accessExternalDTD);
        _readerManager.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, _isSecureProcessing);
        _readerManager.setProperty(JdkConstants.SECURITY_MANAGER, _securityManager);
        _cdataChunkSize = JdkXmlUtils.getValue(_tfactory.getAttribute(JdkConstants.CDATA_CHUNK_SIZE),
                JdkConstants.CDATA_CHUNK_SIZE_DEFAULT);
        _readerManager.setProperty(JdkConstants.CDATA_CHUNK_SIZE, _cdataChunkSize);

        _useCatalog = _tfactory.getFeature(XMLConstants.USE_CATALOG);
        if (_useCatalog) {
            _catalogFeatures = (CatalogFeatures)_tfactory.getAttribute(JdkXmlFeatures.CATALOG_FEATURES);
            String catalogFiles = _catalogFeatures.get(CatalogFeatures.Feature.DEFER);
            if (catalogFiles != null) {
                _readerManager.setFeature(XMLConstants.USE_CATALOG, _useCatalog);
                _readerManager.setProperty(JdkXmlFeatures.CATALOG_FEATURES, _catalogFeatures);
            }
        }
        //_isIncremental = tfactory._incremental;
    }

    /**
     * Return the state of the secure processing feature.
     */
    public boolean isSecureProcessing() {
        return _isSecureProcessing;
    }

    /**
     * Set the state of the secure processing feature.
     */
    public void setSecureProcessing(boolean flag) {
        _isSecureProcessing = flag;
        _readerManager.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, _isSecureProcessing);
    }
    /**
     * Return the state of the services mechanism feature.
     */
    public boolean overrideDefaultParser() {
        return _overrideDefaultParser;
    }

    /**
     * Set the state of the services mechanism feature.
     */
    public void setOverrideDefaultParser(boolean flag) {
        _overrideDefaultParser = flag;
    }

    /**
     * Returns the translet wrapped inside this Transformer or
     * null if this is the identity transform.
     */
    protected AbstractTranslet getTranslet() {
        return _translet;
    }

    public boolean isIdentity() {
        return _isIdentity;
    }

    /**
     * Implements JAXP's Transformer.transform()
     *
     * @param source Contains the input XML document
     * @param result Will contain the output from the transformation
     * @throws TransformerException
     */
    @Override
    public void transform(Source source, Result result)
        throws TransformerException
    {
        if (!_isIdentity) {
            if (_translet == null) {
                ErrorMsg err = new ErrorMsg(ErrorMsg.JAXP_NO_TRANSLET_ERR);
                throw new TransformerException(err.toString());
            }
            // Pass output properties to the translet
            transferOutputProperties(_translet);
        }

        final SerializationHandler toHandler = getOutputHandler(result);
        if (toHandler == null) {
            ErrorMsg err = new ErrorMsg(ErrorMsg.JAXP_NO_HANDLER_ERR);
            throw new TransformerException(err.toString());
        }

        if (!_isIdentity && (_uriResolver != null || (_tfactory.getFeature(XMLConstants.USE_CATALOG)
                    && _tfactory.getAttribute(JdkXmlUtils.CATALOG_FILES) != null))) {
            _translet.setDOMCache(this);
        }

        // Pass output properties to handler if identity
        if (_isIdentity) {
            transferOutputProperties(toHandler);
        }

        transform(source, toHandler, _encoding);
        try{
            if (result instanceof DOMResult) {
                ((DOMResult)result).setNode(_tohFactory.getNode());
            } else if (result instanceof StAXResult) {
                  if (((StAXResult) result).getXMLEventWriter() != null)
                {
                    (_tohFactory.getXMLEventWriter()).flush();
                }
                else if (((StAXResult) result).getXMLStreamWriter() != null) {
                    (_tohFactory.getXMLStreamWriter()).flush();
                    //result = new StAXResult(_tohFactory.getXMLStreamWriter());
                }
            }
        } catch (Exception e) {
            System.out.println("Result writing error");
        }
    }

    /**
     * Create an output handler for the transformation output based on
     * the type and contents of the TrAX Result object passed to the
     * transform() method.
     */
    public SerializationHandler getOutputHandler(Result result)
        throws TransformerException
    {
        // Get output method using get() to ignore defaults
        _method = (String) _properties.get(OutputKeys.METHOD);

        // Get encoding using getProperty() to use defaults
        _encoding = _properties.getProperty(OutputKeys.ENCODING);

        _tohFactory = TransletOutputHandlerFactory
                .newInstance(_overrideDefaultParser, _errorListener);
        _tohFactory.setEncoding(_encoding);
        if (_method != null) {
            _tohFactory.setOutputMethod(_method);
        }

        // Set indentation number in the factory
        if (_indentNumber >= 0) {
            _tohFactory.setIndentNumber(_indentNumber);
        }

        // Return the content handler for this Result object
        try {
            // Result object could be SAXResult, DOMResult, or StreamResult
            if (result instanceof SAXResult) {
                final SAXResult target = (SAXResult)result;
                final ContentHandler handler = target.getHandler();

                _tohFactory.setHandler(handler);

                /**
                 * Fix for bug 24414
                 * If the lexicalHandler is set then we need to get that
                 * for obtaining the lexical information
                 */
                LexicalHandler lexicalHandler = target.getLexicalHandler();

                if (lexicalHandler != null ) {
                    _tohFactory.setLexicalHandler(lexicalHandler);
                }

                _tohFactory.setOutputType(TransletOutputHandlerFactory.SAX);
                return _tohFactory.getSerializationHandler();
            }
            else if (result instanceof StAXResult) {
                if (((StAXResult) result).getXMLEventWriter() != null)
                    _tohFactory.setXMLEventWriter(((StAXResult) result).getXMLEventWriter());
                else if (((StAXResult) result).getXMLStreamWriter() != null)
                    _tohFactory.setXMLStreamWriter(((StAXResult) result).getXMLStreamWriter());
                _tohFactory.setOutputType(TransletOutputHandlerFactory.STAX);
                return _tohFactory.getSerializationHandler();
            }
            else if (result instanceof DOMResult) {
                _tohFactory.setNode(((DOMResult) result).getNode());
                _tohFactory.setNextSibling(((DOMResult) result).getNextSibling());
                _tohFactory.setOutputType(TransletOutputHandlerFactory.DOM);
                return _tohFactory.getSerializationHandler();
            }
            else if (result instanceof StreamResult) {
                // Get StreamResult
                final StreamResult target = (StreamResult) result;

                // StreamResult may have been created with a java.io.File,
                // java.io.Writer, java.io.OutputStream or just a String
                // systemId.

                _tohFactory.setOutputType(TransletOutputHandlerFactory.STREAM);

                // try to get a Writer from Result object
                final Writer writer = target.getWriter();
                if (writer != null) {
                    _tohFactory.setWriter(writer);
                    return _tohFactory.getSerializationHandler();
                }

                // or try to get an OutputStream from Result object
                final OutputStream ostream = target.getOutputStream();
                if (ostream != null) {
                    _tohFactory.setOutputStream(ostream);
                    return _tohFactory.getSerializationHandler();
                }

                // or try to get just a systemId string from Result object
                String systemId = result.getSystemId();
                if (systemId == null) {
                    ErrorMsg err = new ErrorMsg(ErrorMsg.JAXP_NO_RESULT_ERR);
                    throw new TransformerException(err.toString());
                }

                // System Id may be in one of several forms, (1) a uri
                // that starts with 'file:', (2) uri that starts with 'http:'
                // or (3) just a filename on the local system.
                URL url;
                if (systemId.startsWith("file:")) {
                    try{
                        Path p = Paths.get(new URI(systemId));
                        _ostream = new FileOutputStream(p.toFile());
                        _tohFactory.setOutputStream(_ostream);
                        return _tohFactory.getSerializationHandler();
                    }
                    catch (Exception e) {
                        throw new TransformerException(e);
                    }
                }
                else if (systemId.startsWith("http:")) {
                    url = new URL(systemId);
                    final URLConnection connection = url.openConnection();
                    _tohFactory.setOutputStream(_ostream = connection.getOutputStream());
                    return _tohFactory.getSerializationHandler();
                }
                else {
                    // system id is just a filename
                    _tohFactory.setOutputStream(
                        _ostream = new FileOutputStream(new File(systemId)));
                    return _tohFactory.getSerializationHandler();
                }
            }
        }
        // If we cannot write to the location specified by the SystemId
        catch (UnknownServiceException e) {
            throw new TransformerException(e);
        }
        catch (ParserConfigurationException e) {
            throw new TransformerException(e);
        }
        // If we cannot create the file specified by the SystemId
        catch (IOException e) {
            throw new TransformerException(e);
        }
        return null;
    }

    /**
     * Set the internal DOM that will be used for the next transformation
     */
    protected void setDOM(DOM dom) {
        _dom = dom;
    }

    /**
     * Builds an internal DOM from a TrAX Source object
     */
    private DOM getDOM(Source source) throws TransformerException {
        try {
            DOM dom;

            if (source != null) {
                DTMWSFilter wsfilter;
                if (_translet != null && _translet instanceof StripFilter) {
                    wsfilter = new DOMWSFilter(_translet);
                 } else {
                    wsfilter = null;
                 }

                 boolean hasIdCall = (_translet != null) ? _translet.hasIdCall()
                                                         : false;

                 if (_dtmManager == null) {
                     _dtmManager =
                         _tfactory.createNewDTMManagerInstance();
                     _dtmManager.setOverrideDefaultParser(_overrideDefaultParser);
                 }
                 dom = (DOM)_dtmManager.getDTM(source, false, wsfilter, true,
                                              false, false, 0, hasIdCall);
            } else if (_dom != null) {
                 dom = _dom;
                 _dom = null;  // use only once, so reset to 'null'
            } else {
                 return null;
            }

            if (!_isIdentity) {
                // Give the translet the opportunity to make a prepass of
                // the document, in case it can extract useful information early
                _translet.prepassDocument(dom);
            }

            return dom;

        }
        catch (Exception e) {
            if (_errorListener != null) {
                postErrorToListener(e.getMessage());
            }
            throw new TransformerException(e);
        }
    }

    /**
     * Returns the {@link com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl}
     * object that create this <code>Transformer</code>.
     */
    protected TransformerFactoryImpl getTransformerFactory() {
        return _tfactory;
    }

    /**
     * Returns the {@link com.sun.org.apache.xalan.internal.xsltc.runtime.output.TransletOutputHandlerFactory}
     * object that create the <code>TransletOutputHandler</code>.
     */
    protected TransletOutputHandlerFactory getTransletOutputHandlerFactory() {
        return _tohFactory;
    }

    private void transformIdentity(Source source, SerializationHandler handler)
        throws Exception
    {
        // Get systemId from source
        if (source != null) {
            _sourceSystemId = source.getSystemId();
        }

        if (source instanceof StreamSource) {
            final StreamSource stream = (StreamSource) source;
            final InputStream streamInput = stream.getInputStream();
            final Reader streamReader = stream.getReader();
            final XMLReader reader = _readerManager.getXMLReader();

            try {
                // Hook up reader and output handler
                try {
                    reader.setProperty(LEXICAL_HANDLER_PROPERTY, handler);
                    reader.setFeature(NAMESPACE_PREFIXES_FEATURE, true);
                } catch (SAXException e) {
                    // Falls through
                }
                reader.setContentHandler(handler);

                // Create input source from source
                InputSource input;
                if (streamInput != null) {
                    input = new InputSource(streamInput);
                    input.setSystemId(_sourceSystemId);
                }
                else if (streamReader != null) {
                    input = new InputSource(streamReader);
                    input.setSystemId(_sourceSystemId);
                }
                else if (_sourceSystemId != null) {
                    input = new InputSource(_sourceSystemId);
                }
                else {
                    ErrorMsg err = new ErrorMsg(ErrorMsg.JAXP_NO_SOURCE_ERR);
                    throw new TransformerException(err.toString());
                }

                // Start pushing SAX events
                reader.parse(input);
            } finally {
                _readerManager.releaseXMLReader(reader);
            }
        } else if (source instanceof SAXSource) {
            final SAXSource sax = (SAXSource) source;
            XMLReader reader = sax.getXMLReader();
            final InputSource input = sax.getInputSource();
            boolean userReader = true;

            try {
                // Create a reader if not set by user
                if (reader == null) {
                    reader = _readerManager.getXMLReader();
                    userReader = false;
                }

                // Hook up reader and output handler
                try {
                    reader.setProperty(LEXICAL_HANDLER_PROPERTY, handler);
                    reader.setFeature(NAMESPACE_PREFIXES_FEATURE, true);
                } catch (SAXException e) {
                    // Falls through
                }
                reader.setContentHandler(handler);

                // Start pushing SAX events
                reader.parse(input);
            } finally {
                if (!userReader) {
                    _readerManager.releaseXMLReader(reader);
                }
            }
        } else if (source instanceof StAXSource) {
            final StAXSource staxSource = (StAXSource)source;
            StAXEvent2SAX staxevent2sax;
            StAXStream2SAX staxStream2SAX;
            if (staxSource.getXMLEventReader() != null) {
                final XMLEventReader xmlEventReader = staxSource.getXMLEventReader();
                staxevent2sax = new StAXEvent2SAX(xmlEventReader);
                staxevent2sax.setContentHandler(handler);
                staxevent2sax.parse();
                handler.flushPending();
            } else if (staxSource.getXMLStreamReader() != null) {
                final XMLStreamReader xmlStreamReader = staxSource.getXMLStreamReader();
                staxStream2SAX = new StAXStream2SAX(xmlStreamReader);
                staxStream2SAX.setContentHandler(handler);
                staxStream2SAX.parse();
                handler.flushPending();
            }
        } else if (source instanceof DOMSource) {
            final DOMSource domsrc = (DOMSource) source;
            new DOM2TO(domsrc.getNode(), handler).parse();
        } else if (source instanceof XSLTCSource) {
            final DOM dom = ((XSLTCSource) source).getDOM(null, _translet);
            ((SAXImpl)dom).copy(handler);
        } else {
            ErrorMsg err = new ErrorMsg(ErrorMsg.JAXP_NO_SOURCE_ERR);
            throw new TransformerException(err.toString());
        }
    }

    /**
     * Internal transformation method - uses the internal APIs of XSLTC
     */
    private void transform(Source source, SerializationHandler handler,
        String encoding) throws TransformerException
    {
        try {
            /*
             * According to JAXP1.2, new SAXSource()/StreamSource()
             * should create an empty input tree, with a default root node.
             * new DOMSource()creates an empty document using DocumentBuilder.
             * newDocument(); Use DocumentBuilder.newDocument() for all 3
             * situations, since there is no clear spec. how to create
             * an empty tree when both SAXSource() and StreamSource() are used.
             */
            if ((source instanceof StreamSource && source.getSystemId()==null
                && ((StreamSource)source).getInputStream()==null &&
                ((StreamSource)source).getReader()==null)||
                (source instanceof SAXSource &&
                ((SAXSource)source).getInputSource()==null &&
                ((SAXSource)source).getXMLReader()==null )||
                (source instanceof DOMSource &&
                ((DOMSource)source).getNode()==null)){

                boolean supportCatalog = true;

                DocumentBuilderFactory builderF = JdkXmlUtils.getDOMFactory(_overrideDefaultParser);
                try {
                    builderF.setFeature(XMLConstants.USE_CATALOG, _useCatalog);
                } catch (ParserConfigurationException e) {
                    supportCatalog = false;
                }

                if (supportCatalog && _useCatalog) {
                    CatalogFeatures cf = (CatalogFeatures)_tfactory.getAttribute(JdkXmlFeatures.CATALOG_FEATURES);
                    if (cf != null) {
                        for (CatalogFeatures.Feature f : CatalogFeatures.Feature.values()) {
                            builderF.setAttribute(f.getPropertyName(), cf.get(f));
                        }
                    }
                }

                DocumentBuilder builder = builderF.newDocumentBuilder();
                String systemID = source.getSystemId();
                source = new DOMSource(builder.newDocument());

                // Copy system ID from original, empty Source to new
                if (systemID != null) {
                  source.setSystemId(systemID);
                }
            }
            if (_isIdentity) {
                transformIdentity(source, handler);
            } else {
                _translet.transform(getDOM(source), handler);
            }
        } catch (TransletException e) {
            if (_errorListener != null) postErrorToListener(e.getMessage());
            throw new TransformerException(e);
        } catch (RuntimeException e) {
            if (_errorListener != null) postErrorToListener(e.getMessage());
            throw new TransformerException(e);
        } catch (Exception e) {
            if (_errorListener != null) postErrorToListener(e.getMessage());
            throw new TransformerException(e);
        } finally {
            _dtmManager = null;
        }

        // If we create an output stream for the Result, we need to close it after the transformation.
        if (_ostream != null) {
            try {
                _ostream.close();
            }
            catch (IOException e) {}
            _ostream = null;
        }
    }

    /**
     * Implements JAXP's Transformer.getErrorListener()
     * Get the error event handler in effect for the transformation.
     *
     * @return The error event handler currently in effect
     */
    @Override
    public ErrorListener getErrorListener() {
        return _errorListener;
    }

    /**
     * Implements JAXP's Transformer.setErrorListener()
     * Set the error event listener in effect for the transformation.
     * Register a message handler in the translet in order to forward
     * xsl:messages to error listener.
     *
     * @param listener The error event listener to use
     * @throws IllegalArgumentException
     */
    @Override
    public void setErrorListener(ErrorListener listener)
        throws IllegalArgumentException {
        if (listener == null) {
            ErrorMsg err = new ErrorMsg(ErrorMsg.ERROR_LISTENER_NULL_ERR,
                                        "Transformer");
            throw new IllegalArgumentException(err.toString());
        }
        _errorListener = listener;

        // Register a message handler to report xsl:messages
        if (_translet != null)
            _translet.setMessageHandler(new MessageHandler(_errorListener));
    }

    /**
     * Inform TrAX error listener of an error
     */
    private void postErrorToListener(String message) {
        try {
            _errorListener.error(new TransformerException(message));
        }
        catch (TransformerException e) {
            // ignored - transformation cannot be continued
        }
    }

    /**
     * Inform TrAX error listener of a warning
     */
    private void postWarningToListener(String message) {
        try {
            _errorListener.warning(new TransformerException(message));
        }
        catch (TransformerException e) {
            // ignored - transformation cannot be continued
        }
    }

    /**
     * Implements JAXP's Transformer.getOutputProperties().
     * Returns a copy of the output properties for the transformation. This is
     * a set of layered properties. The first layer contains properties set by
     * calls to setOutputProperty() and setOutputProperties() on this class,
     * and the output settings defined in the stylesheet's <xsl:output>
     * element makes up the second level, while the default XSLT output
     * settings are returned on the third level.
     *
     * @return Properties in effect for this Transformer
     */
    @Override
    public Properties getOutputProperties() {
        return (Properties) _properties.clone();
    }

    /**
     * Implements JAXP's Transformer.getOutputProperty().
     * Get an output property that is in effect for the transformation. The
     * property specified may be a property that was set with setOutputProperty,
     * or it may be a property specified in the stylesheet.
     *
     * @param name A non-null string that contains the name of the property
     * @throws IllegalArgumentException if the property name is not known
     */
    @Override
    public String getOutputProperty(String name)
        throws IllegalArgumentException
    {
        if (ImplPropMap.XSLTCISSTANDALONE.is(name)) {
            return _xsltcIsStandalone.getValue();
        }
        if (!validOutputProperty(name)) {
            ErrorMsg err = new ErrorMsg(ErrorMsg.JAXP_UNKNOWN_PROP_ERR, name);
            throw new IllegalArgumentException(err.toString());
        }
        return _properties.getProperty(name);
    }

    /**
     * Implements JAXP's Transformer.setOutputProperties().
     * Set the output properties for the transformation. These properties
     * will override properties set in the Templates with xsl:output.
     * Unrecognised properties will be quitely ignored.
     *
     * @param properties The properties to use for the Transformer
     * @throws IllegalArgumentException Never, errors are ignored
     */
    @Override
    public void setOutputProperties(Properties properties)
        throws IllegalArgumentException
    {
        if (properties != null) {
            final Enumeration<?> names = properties.propertyNames();

            while (names.hasMoreElements()) {
                final String name = (String) names.nextElement();

                // Ignore lower layer properties
                if (isDefaultProperty(name, properties)) continue;

                if (validOutputProperty(name)) {
                    _properties.setProperty(name, properties.getProperty(name));
                }
                else {
                    ErrorMsg err = new ErrorMsg(ErrorMsg.JAXP_UNKNOWN_PROP_ERR, name);
                    throw new IllegalArgumentException(err.toString());
                }
            }
        }
        else {
            _properties = (Properties)_propertiesClone.clone();
        }
    }

    /**
     * Implements JAXP's Transformer.setOutputProperty().
     * Get an output property that is in effect for the transformation. The
     * property specified may be a property that was set with
     * setOutputProperty(), or it may be a property specified in the stylesheet.
     *
     * @param name The name of the property to set
     * @param value The value to assign to the property
     * @throws IllegalArgumentException Never, errors are ignored
     */
    @Override
    public void setOutputProperty(String name, String value)
        throws IllegalArgumentException
    {
        if (!validOutputProperty(name)) {
            ErrorMsg err = new ErrorMsg(ErrorMsg.JAXP_UNKNOWN_PROP_ERR, name);
            throw new IllegalArgumentException(err.toString());
        }

        if (ImplPropMap.XSLTCISSTANDALONE.is(name)) {
            _xsltcIsStandalone.setValue(name, value, State.APIPROPERTY);
        } else {
            _properties.setProperty(name, value);
        }
    }

    /**
     * Internal method to pass any properties to the translet prior to
     * initiating the transformation
     */
    private void transferOutputProperties(AbstractTranslet translet)
    {
        // Return right now if no properties are set
        if (_properties == null) return;

        // Get a list of all the defined properties
        Enumeration<?> names = _properties.propertyNames();
        while (names.hasMoreElements()) {
            // Note the use of get() instead of getProperty()
            String name  = (String) names.nextElement();
            String value = (String) _properties.get(name);

            // Ignore default properties
            if (value == null) continue;

            // Pass property value to translet - override previous setting
            if (name.equals(OutputKeys.ENCODING)) {
                translet._encoding = value;
            }
            else if (name.equals(OutputKeys.METHOD)) {
                translet._method = value;
            }
            else if (name.equals(OutputKeys.DOCTYPE_PUBLIC)) {
                translet._doctypePublic = value;
            }
            else if (name.equals(OutputKeys.DOCTYPE_SYSTEM)) {
                translet._doctypeSystem = value;
            }
            else if (name.equals(OutputKeys.MEDIA_TYPE)) {
                translet._mediaType = value;
            }
            else if (name.equals(OutputKeys.STANDALONE)) {
                translet._standalone = value;
            }
            else if (name.equals(OutputKeys.VERSION)) {
                translet._version = value;
            }
            else if (name.equals(OutputKeys.OMIT_XML_DECLARATION)) {
                translet._omitHeader =
                    (value != null && value.toLowerCase().equals("yes"));
            }
            else if (name.equals(OutputKeys.INDENT)) {
                translet._indent =
                    (value != null && value.toLowerCase().equals("yes"));
            }
            else if (name.equals(OutputPropertiesFactory.S_BUILTIN_OLD_EXTENSIONS_UNIVERSAL +"indent-amount")) {
                 if (value != null) {
                     translet._indentamount = Integer.parseInt(value);
                 }
            }
            else if (name.equals(OutputPropertiesFactory.S_BUILTIN_EXTENSIONS_UNIVERSAL +"indent-amount")) {
                 if (value != null) {
                     translet._indentamount = Integer.parseInt(value);
                 }
            }
            else if (name.equals(OutputKeys.CDATA_SECTION_ELEMENTS)) {
                if (value != null) {
                    translet._cdata = null; // clear previous setting
                    StringTokenizer e = new StringTokenizer(value);
                    while (e.hasMoreTokens()) {
                        translet.addCdataElement(e.nextToken());
                    }
                }
            }
        }
        if (_xsltcIsStandalone.getValue().equals("yes")) {
            translet._isStandalone = true;
        }
    }

    /**
     * This method is used to pass any properties to the output handler
     * when running the identity transform.
     */
    public void transferOutputProperties(SerializationHandler handler)
    {
        // Return right now if no properties are set
        if (_properties == null) return;

        String doctypePublic = null;
        String doctypeSystem = null;

        // Get a list of all the defined properties
        Enumeration<?> names = _properties.propertyNames();
        while (names.hasMoreElements()) {
            // Note the use of get() instead of getProperty()
            String name  = (String) names.nextElement();
            String value = (String) _properties.get(name);

            // Ignore default properties
            if (value == null) continue;

            // Pass property value to translet - override previous setting
            if (name.equals(OutputKeys.DOCTYPE_PUBLIC)) {
                doctypePublic = value;
            }
            else if (name.equals(OutputKeys.DOCTYPE_SYSTEM)) {
                doctypeSystem = value;
            }
            else if (name.equals(OutputKeys.MEDIA_TYPE)) {
                handler.setMediaType(value);
            }
            else if (name.equals(OutputKeys.STANDALONE)) {
                handler.setStandalone(value);
            }
            else if (name.equals(OutputKeys.VERSION)) {
                handler.setVersion(value);
            }
            else if (name.equals(OutputKeys.OMIT_XML_DECLARATION)) {
                handler.setOmitXMLDeclaration(
                    value != null && value.toLowerCase().equals("yes"));
            }
            else if (name.equals(OutputKeys.INDENT)) {
                handler.setIndent(
                    value != null && value.toLowerCase().equals("yes"));
            }
            else if (name.equals(OutputPropertiesFactory.S_BUILTIN_OLD_EXTENSIONS_UNIVERSAL +"indent-amount")) {
                if (value != null) {
                    handler.setIndentAmount(Integer.parseInt(value));
                }
            }
            else if (name.equals(OutputPropertiesFactory.S_BUILTIN_EXTENSIONS_UNIVERSAL +"indent-amount")) {
                if (value != null) {
                    handler.setIndentAmount(Integer.parseInt(value));
                }
            }
            else if (name.equals(OutputKeys.CDATA_SECTION_ELEMENTS)) {
                if (value != null) {
                    StringTokenizer e = new StringTokenizer(value);
                    List<String> uriAndLocalNames = null;
                    while (e.hasMoreTokens()) {
                        final String token = e.nextToken();

                        // look for the last colon, as the String may be
                        // something like "http://abc.com:local"
                        int lastcolon = token.lastIndexOf(':');
                        String uri;
                        String localName;
                        if (lastcolon > 0) {
                            uri = token.substring(0, lastcolon);
                            localName = token.substring(lastcolon+1);
                        } else {
                            // no colon at all, lets hope this is the
                            // local name itself then
                            uri = null;
                            localName = token;
                        }

                        if (uriAndLocalNames == null) {
                            uriAndLocalNames = new ArrayList<>();
                        }
                        // add the uri/localName as a pair, in that order
                        uriAndLocalNames.add(uri);
                        uriAndLocalNames.add(localName);
                    }
                    handler.setCdataSectionElements(uriAndLocalNames);
                }
            }
        }

        // Call setDoctype() if needed
        if (doctypePublic != null || doctypeSystem != null) {
            handler.setDoctype(doctypeSystem, doctypePublic);
        }

        if (_xsltcIsStandalone.getValue().equals("yes")) {
            handler.setIsStandalone(true);
        }
    }

    /**
     * Internal method to create the initial set of properties. There
     * are two layers of properties: the default layer and the base layer.
     * The latter contains properties defined in the stylesheet or by
     * the user using this API.
     */
    private Properties createOutputProperties(Properties outputProperties) {
        final Properties defaults = new Properties();
        setDefaults(defaults, "xml");

        // Copy propeties set in stylesheet to base
        final Properties base = new Properties(defaults);
        if (outputProperties != null) {
            final Enumeration<?> names = outputProperties.propertyNames();
            while (names.hasMoreElements()) {
                final String name = (String) names.nextElement();
                base.setProperty(name, outputProperties.getProperty(name));
            }
        }
        else {
            base.setProperty(OutputKeys.ENCODING, _translet._encoding);
            if (_translet._method != null)
                base.setProperty(OutputKeys.METHOD, _translet._method);
        }

        // Update defaults based on output method
        final String method = base.getProperty(OutputKeys.METHOD);
        if (method != null) {
            if (method.equals("html")) {
                setDefaults(defaults,"html");
            }
            else if (method.equals("text")) {
                setDefaults(defaults,"text");
            }
        }

        return base;
    }

        /**
         * Internal method to get the default properties from the
         * serializer factory and set them on the property object.
         * @param props a java.util.Property object on which the properties are set.
         * @param method The output method type, one of "xml", "text", "html" ...
         */
        private void setDefaults(Properties props, String method)
        {
                final Properties method_props =
                        OutputPropertiesFactory.getDefaultMethodProperties(method);
                {
                    final Enumeration<?> names = method_props.propertyNames();
                    while (names.hasMoreElements())
                    {
                        final String name = (String)names.nextElement();
                        props.setProperty(name, method_props.getProperty(name));
                    }
                }
        }
    /**
     * Verifies if a given output property name is a property defined in
     * the JAXP 1.1 / TrAX spec
     */
    private boolean validOutputProperty(String name) {
        return (name.equals(OutputKeys.ENCODING) ||
                name.equals(OutputKeys.METHOD) ||
                name.equals(OutputKeys.INDENT) ||
                name.equals(OutputKeys.DOCTYPE_PUBLIC) ||
                name.equals(OutputKeys.DOCTYPE_SYSTEM) ||
                name.equals(OutputKeys.CDATA_SECTION_ELEMENTS) ||
                name.equals(OutputKeys.MEDIA_TYPE) ||
                name.equals(OutputKeys.OMIT_XML_DECLARATION)   ||
                name.equals(OutputKeys.STANDALONE) ||
                name.equals(OutputKeys.VERSION) ||
                ImplPropMap.XSLTCISSTANDALONE.is(name) ||
                name.charAt(0) == '{');
    }

    /**
     * Checks if a given output property is default (2nd layer only)
     */
    private boolean isDefaultProperty(String name, Properties properties) {
        return (properties.get(name) == null);
    }

    /**
     * Implements JAXP's Transformer.setParameter()
     * Add a parameter for the transformation. The parameter is simply passed
     * on to the translet - no validation is performed - so any unused
     * parameters are quitely ignored by the translet.
     *
     * @param name The name of the parameter
     * @param value The value to assign to the parameter
     */
    @Override
    public void setParameter(String name, Object value) {

        if (value == null) {
            ErrorMsg err = new ErrorMsg(ErrorMsg.JAXP_INVALID_SET_PARAM_VALUE, name);
            throw new IllegalArgumentException(err.toString());
        }

        if (_isIdentity) {
            if (_parameters == null) {
                _parameters = new HashMap<>();
            }
            _parameters.put(name, value);
        }
        else {
            _translet.addParameter(name, value);
        }
    }

    /**
     * Implements JAXP's Transformer.clearParameters()
     * Clear all parameters set with setParameter. Clears the translet's
     * parameter stack.
     */
    @Override
    public void clearParameters() {
        if (_isIdentity && _parameters != null) {
            _parameters.clear();
        }
        else {
            _translet.clearParameters();
        }
    }

    /**
     * Implements JAXP's Transformer.getParameter()
     * Returns the value of a given parameter. Note that the translet will not
     * keep values for parameters that were not defined in the stylesheet.
     *
     * @param name The name of the parameter
     * @return An object that contains the value assigned to the parameter
     */
    @Override
    public final Object getParameter(String name) {
        if (_isIdentity) {
            return (_parameters != null) ? _parameters.get(name) : null;
        }
        else {
            return _translet.getParameter(name);
        }
    }

    /**
     * Implements JAXP's Transformer.getURIResolver()
     * Set the object currently used to resolve URIs used in document().
     *
     * @return  The URLResolver object currently in use
     */
    @Override
    public URIResolver getURIResolver() {
        return _uriResolver;
    }

    /**
     * Implements JAXP's Transformer.setURIResolver()
     * Set an object that will be used to resolve URIs used in document().
     *
     * @param resolver The URIResolver to use in document()
     */
    @Override
    public void setURIResolver(URIResolver resolver) {
        _uriResolver = resolver;
    }

    /**
     * This class should only be used as a DOMCache for the translet if the
     * URIResolver has been set.
     *
     * The method implements XSLTC's DOMCache interface, which is used to
     * plug in an external document loader into a translet. This method acts
     * as an adapter between TrAX's URIResolver interface and XSLTC's
     * DOMCache interface. This approach is simple, but removes the
     * possibility of using external document caches with XSLTC.
     *
     * @param baseURI The base URI used by the document call.
     * @param href The href argument passed to the document function.
     * @param translet A reference to the translet requesting the document
     */
    @Override
    public DOM retrieveDocument(String baseURI, String href, Translet translet) {
        try {
            // Argument to document function was: document('');
            if (href.length() == 0) {
                href = baseURI;
            }

            /*
             *  Fix for bug 24188
             *  Incase the _uriResolver.resolve(href,base) is null
             *  try to still  retrieve the document before returning null
             *  and throwing the FileNotFoundException in
             *  com.sun.org.apache.xalan.internal.xsltc.dom.LoadDocument
             *
             */
            Source resolvedSource = null;
            if (_uriResolver != null) {
                resolvedSource = _uriResolver.resolve(href, baseURI);
            }

            if (resolvedSource == null && _useCatalog &&
                    _catalogFeatures.get(CatalogFeatures.Feature.FILES) != null)  {
                if (_catalogUriResolver == null) {
                    _catalogUriResolver = CatalogManager.catalogResolver(_catalogFeatures);
                }
                resolvedSource = _catalogUriResolver.resolve(href, baseURI);
            }

            if (resolvedSource == null)  {
                StreamSource streamSource = new StreamSource(
                     SystemIDResolver.getAbsoluteURI(href, baseURI));
                return getDOM(streamSource) ;
            }

            return getDOM(resolvedSource);
        }
        catch (TransformerException | CatalogException e) {
            if (_errorListener != null)
                postErrorToListener("File not found: " + e.getMessage());
            return(null);
        }
    }

    /**
     * This method resets  the Transformer to its original configuration
     * Transformer code is reset to the same state it was when it was
     * created
     * @since 1.5
     */
    @Override
    public void reset() {

        _method = null;
        _encoding = null;
        _sourceSystemId = null;
        _errorListener = _defaultListener;
        _uriResolver = null;
        _dom = null;
        _parameters = null;
        _indentNumber = -1;
        setOutputProperties (null);
        _tohFactory = null;
        _ostream = null;

    }
}
