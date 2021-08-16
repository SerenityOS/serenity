/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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

package com.sun.org.apache.xalan.internal.xsltc.trax;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.dom.DOMResult;

import com.sun.org.apache.xalan.internal.xsltc.StripFilter;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import com.sun.org.apache.xalan.internal.xsltc.dom.DOMWSFilter;
import com.sun.org.apache.xalan.internal.xsltc.dom.SAXImpl;
import com.sun.org.apache.xalan.internal.xsltc.dom.XSLTCDTMManager;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMWSFilter;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Implementation of a JAXP1.1 TransformerHandler
 * @author Morten Jorgensen
 */
public class TransformerHandlerImpl implements TransformerHandler, DeclHandler {

    private TransformerImpl  _transformer;
    private AbstractTranslet _translet = null;
    private String           _systemId;
    private SAXImpl          _dom = null;
    private ContentHandler   _handler = null;
    private LexicalHandler   _lexHandler = null;
    private DTDHandler       _dtdHandler = null;
    private DeclHandler      _declHandler = null;
    private Result           _result = null;
    private Locator          _locator = null;

    private boolean          _done = false; // Set in endDocument()

    /**
     * A flag indicating whether this transformer handler implements the
     * identity transform.
     */
    private boolean _isIdentity = false;

    /**
     * Cosntructor - pass in reference to a TransformerImpl object
     */
    public TransformerHandlerImpl(TransformerImpl transformer) {
        // Save the reference to the transformer
        _transformer = transformer;

        if (transformer.isIdentity()) {
            // Set initial handler to the empty handler
            _handler = new DefaultHandler();
            _isIdentity = true;
        }
        else {
            // Get a reference to the translet wrapped inside the transformer
            _translet = _transformer.getTranslet();
        }
    }

    /**
     * Implements javax.xml.transform.sax.TransformerHandler.getSystemId()
     * Get the base ID (URI or system ID) from where relative URLs will be
     * resolved.
     * @return The systemID that was set with setSystemId(String id)
     */
    @Override
    public String getSystemId() {
        return _systemId;
    }

    /**
     * Implements javax.xml.transform.sax.TransformerHandler.setSystemId()
     * Get the base ID (URI or system ID) from where relative URLs will be
     * resolved.
     * @param id Base URI for this stylesheet
     */
    @Override
    public void setSystemId(String id) {
        _systemId = id;
    }

    /**
     * Implements javax.xml.transform.sax.TransformerHandler.getTransformer()
     * Get the Transformer associated with this handler, which is needed in
     * order to set parameters and output properties.
     * @return The Transformer object
     */
    @Override
    public Transformer getTransformer() {
        return _transformer;
    }

    /**
     * Implements javax.xml.transform.sax.TransformerHandler.setResult()
     * Enables the user of the TransformerHandler to set the to set the Result
     * for the transformation.
     * @param result A Result instance, should not be null
     * @throws IllegalArgumentException if result is invalid for some reason
     */
    @Override
    public void setResult(Result result) throws IllegalArgumentException {
        _result = result;

    if (null == result) {
       ErrorMsg err = new ErrorMsg(ErrorMsg.ER_RESULT_NULL);
       throw new IllegalArgumentException(err.toString()); //"result should not be null");
    }

        if (_isIdentity) {
            try {
                // Connect this object with output system directly
                SerializationHandler outputHandler =
                    _transformer.getOutputHandler(result);
                _transformer.transferOutputProperties(outputHandler);

                _handler = outputHandler;
                _lexHandler = outputHandler;
            }
            catch (TransformerException e) {
                _result = null;
            }
        }
        else if (_done) {
            // Run the transformation now, if not already done
            try {
                _transformer.setDOM(_dom);
                _transformer.transform(null, _result);
            }
            catch (TransformerException e) {
                // What the hell are we supposed to do with this???
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    /**
     * Implements org.xml.sax.ContentHandler.characters()
     * Receive notification of character data.
     */
    @Override
    public void characters(char[] ch, int start, int length)
        throws SAXException
    {
        _handler.characters(ch, start, length);
    }

    /**
     * Implements org.xml.sax.ContentHandler.startDocument()
     * Receive notification of the beginning of a document.
     */
    @Override
    public void startDocument() throws SAXException {
        // Make sure setResult() was called before the first SAX event
        if (_result == null) {
            ErrorMsg err = new ErrorMsg(ErrorMsg.JAXP_SET_RESULT_ERR);
            throw new SAXException(err.toString());
        }

        if (!_isIdentity) {
            boolean hasIdCall = (_translet != null) ? _translet.hasIdCall() : false;
            XSLTCDTMManager dtmManager = null;

            // Create an internal DOM (not W3C) and get SAX2 input handler
            try {
                dtmManager = _transformer.getTransformerFactory()
                                         .createNewDTMManagerInstance();
            } catch (Exception e) {
                throw new SAXException(e);
            }

            DTMWSFilter wsFilter;
            if (_translet != null && _translet instanceof StripFilter) {
                wsFilter = new DOMWSFilter(_translet);
            } else {
                wsFilter = null;
            }

            // Construct the DTM using the SAX events that come through
            _dom = (SAXImpl)dtmManager.getDTM(null, false, wsFilter, true,
                                              false, hasIdCall);

            _handler = _dom.getBuilder();
            _lexHandler = (LexicalHandler) _handler;
            _dtdHandler = (DTDHandler) _handler;
            _declHandler = (DeclHandler) _handler;


            // Set document URI
            _dom.setDocumentURI(_systemId);

            if (_locator != null) {
                _handler.setDocumentLocator(_locator);
            }
        }

        // Proxy call
        _handler.startDocument();
    }

    /**
     * Implements org.xml.sax.ContentHandler.endDocument()
     * Receive notification of the end of a document.
     */
    @Override
    public void endDocument() throws SAXException {
        // Signal to the DOMBuilder that the document is complete
        _handler.endDocument();

        if (!_isIdentity) {
            // Run the transformation now if we have a reference to a Result object
            if (_result != null) {
                try {
                    _transformer.setDOM(_dom);
                    _transformer.transform(null, _result);
                }
                catch (TransformerException e) {
                    throw new SAXException(e);
                }
            }
            // Signal that the internal DOM is built (see 'setResult()').
            _done = true;

            // Set this DOM as the transformer's DOM
            _transformer.setDOM(_dom);
        }
        if (_isIdentity && _result instanceof DOMResult) {
            ((DOMResult)_result).setNode(_transformer.getTransletOutputHandlerFactory().getNode());
        }
    }

    /**
     * Implements org.xml.sax.ContentHandler.startElement()
     * Receive notification of the beginning of an element.
     */
    @Override
    public void startElement(String uri, String localName,
                             String qname, Attributes attributes)
        throws SAXException
    {
        _handler.startElement(uri, localName, qname, attributes);
    }

    /**
     * Implements org.xml.sax.ContentHandler.endElement()
     * Receive notification of the end of an element.
     */
    @Override
    public void endElement(String namespaceURI, String localName, String qname)
        throws SAXException
    {
        _handler.endElement(namespaceURI, localName, qname);
    }

    /**
     * Implements org.xml.sax.ContentHandler.processingInstruction()
     * Receive notification of a processing instruction.
     */
    @Override
    public void processingInstruction(String target, String data)
        throws SAXException
    {
        _handler.processingInstruction(target, data);
    }

    /**
     * Implements org.xml.sax.ext.LexicalHandler.startCDATA()
     */
    @Override
    public void startCDATA() throws SAXException {
        if (_lexHandler != null) {
            _lexHandler.startCDATA();
        }
    }

    /**
     * Implements org.xml.sax.ext.LexicalHandler.endCDATA()
     */
    @Override
    public void endCDATA() throws SAXException {
        if (_lexHandler != null) {
            _lexHandler.endCDATA();
        }
    }

    /**
     * Implements org.xml.sax.ext.LexicalHandler.comment()
     * Receieve notification of a comment
     */
    @Override
    public void comment(char[] ch, int start, int length)
        throws SAXException
    {
        if (_lexHandler != null) {
            _lexHandler.comment(ch, start, length);
        }
    }

    /**
     * Implements org.xml.sax.ContentHandler.ignorableWhitespace()
     * Receive notification of ignorable whitespace in element
     * content. Similar to characters(char[], int, int).
     */
    @Override
    public void ignorableWhitespace(char[] ch, int start, int length)
        throws SAXException
    {
        _handler.ignorableWhitespace(ch, start, length);
    }

    /**
     * Implements org.xml.sax.ContentHandler.setDocumentLocator()
     * Receive an object for locating the origin of SAX document events.
     */
    @Override
    public void setDocumentLocator(Locator locator) {
        _locator = locator;

        if (_handler != null) {
            _handler.setDocumentLocator(locator);
        }
    }

    /**
     * Implements org.xml.sax.ContentHandler.skippedEntity()
     * Receive notification of a skipped entity.
     */
    @Override
    public void skippedEntity(String name) throws SAXException {
        _handler.skippedEntity(name);
    }

    /**
     * Implements org.xml.sax.ContentHandler.startPrefixMapping()
     * Begin the scope of a prefix-URI Namespace mapping.
     */
    @Override
    public void startPrefixMapping(String prefix, String uri)
        throws SAXException {
        _handler.startPrefixMapping(prefix, uri);
    }

    /**
     * Implements org.xml.sax.ContentHandler.endPrefixMapping()
     * End the scope of a prefix-URI Namespace mapping.
     */
    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        _handler.endPrefixMapping(prefix);
    }

    /**
     * Implements org.xml.sax.ext.LexicalHandler.startDTD()
     */
    @Override
    public void startDTD(String name, String publicId, String systemId)
        throws SAXException
    {
        if (_lexHandler != null) {
            _lexHandler.startDTD(name, publicId, systemId);
        }
    }

    /**
     * Implements org.xml.sax.ext.LexicalHandler.endDTD()
     */
    @Override
    public void endDTD() throws SAXException {
        if (_lexHandler != null) {
            _lexHandler.endDTD();
        }
    }

    /**
     * Implements org.xml.sax.ext.LexicalHandler.startEntity()
     */
    @Override
    public void startEntity(String name) throws SAXException {
        if (_lexHandler != null) {
            _lexHandler.startEntity(name);
        }
    }

    /**
     * Implements org.xml.sax.ext.LexicalHandler.endEntity()
     */
    @Override
    public void endEntity(String name) throws SAXException {
        if (_lexHandler != null) {
            _lexHandler.endEntity(name);
        }
    }

    /**
     * Implements org.xml.sax.DTDHandler.unparsedEntityDecl()
     */
    @Override
    public void unparsedEntityDecl(String name, String publicId,
        String systemId, String notationName) throws SAXException
    {
        if (_dtdHandler != null) {
            _dtdHandler.unparsedEntityDecl(name, publicId, systemId,
                                           notationName);
        }
    }

    /**
     * Implements org.xml.sax.DTDHandler.notationDecl()
     */
    @Override
    public void notationDecl(String name, String publicId, String systemId)
        throws SAXException
    {
        if (_dtdHandler != null) {
            _dtdHandler.notationDecl(name, publicId, systemId);
        }
    }

    /**
     * Implements org.xml.sax.ext.DeclHandler.attributeDecl()
     */
    @Override
    public void attributeDecl(String eName, String aName, String type,
        String valueDefault, String value) throws SAXException
    {
        if (_declHandler != null) {
            _declHandler.attributeDecl(eName, aName, type, valueDefault, value);
        }
    }

    /**
     * Implements org.xml.sax.ext.DeclHandler.elementDecl()
     */
    @Override
    public void elementDecl(String name, String model)
        throws SAXException
    {
        if (_declHandler != null) {
            _declHandler.elementDecl(name, model);
        }
    }

    /**
     * Implements org.xml.sax.ext.DeclHandler.externalEntityDecl()
     */
    @Override
    public void externalEntityDecl(String name, String publicId, String systemId)
        throws SAXException
    {
        if (_declHandler != null) {
            _declHandler.externalEntityDecl(name, publicId, systemId);
        }
    }

    /**
     * Implements org.xml.sax.ext.DeclHandler.externalEntityDecl()
     */
    @Override
    public void internalEntityDecl(String name, String value)
        throws SAXException
    {
        if (_declHandler != null) {
            _declHandler.internalEntityDecl(name, value);
        }
    }


   /** Implementation of the reset() method
    *
    */
   public void reset() {
       _systemId = null;
       _dom = null;
       _handler = null;
       _lexHandler = null;
       _dtdHandler = null;
       _declHandler = null;
       _result = null;
       _locator = null;
   }
}
