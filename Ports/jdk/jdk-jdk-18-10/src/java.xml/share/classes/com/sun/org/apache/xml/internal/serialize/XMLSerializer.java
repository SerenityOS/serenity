/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

// Sep 14, 2000:
//  Fixed problem with namespace handling. Contributed by
//  David Blondeau <blondeau@intalio.com>
// Sep 14, 2000:
//  Fixed serializer to report IO exception directly, instead at
//  the end of document processing.
//  Reported by Patrick Higgins <phiggins@transzap.com>
// Aug 21, 2000:
//  Fixed bug in startDocument not calling prepare.
//  Reported by Mikael Staldal <d96-mst-ingen-reklam@d.kth.se>
// Aug 21, 2000:
//  Added ability to omit DOCTYPE declaration.

package com.sun.org.apache.xml.internal.serialize;

import com.sun.org.apache.xerces.internal.dom.DOMMessageFormatter;
import com.sun.org.apache.xerces.internal.util.NamespaceSupport;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMError;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeFilter;
import org.xml.sax.AttributeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Implements an XML serializer supporting both DOM and SAX pretty
 * serializing. For usage instructions see {@link Serializer}.
 * <p>
 * If an output stream is used, the encoding is taken from the
 * output format (defaults to <tt>UTF-8</tt>). If a writer is
 * used, make sure the writer uses the same encoding (if applies)
 * as specified in the output format.
 * <p>
 * The serializer supports both DOM and SAX. SAX serializing is done by firing
 * SAX events and using the serializer as a document handler. DOM serializing is done
 * by calling {@link #serialize(Document)} or by using DOM Level 3
 * {@link org.w3c.dom.ls.LSSerializer} and
 * serializing with {@link org.w3c.dom.ls.LSSerializer#write},
 * {@link org.w3c.dom.ls.LSSerializer#writeToString}.
 * <p>
 * If an I/O exception occurs while serializing, the serializer
 * will not throw an exception directly, but only throw it
 * at the end of serializing (either DOM or SAX's {@link
 * org.xml.sax.DocumentHandler#endDocument}.
 * <p>
 * For elements that are not specified as whitespace preserving,
 * the serializer will potentially break long text lines at space
 * boundaries, indent lines, and serialize elements on separate
 * lines. Line terminators will be regarded as spaces, and
 * spaces at beginning of line will be stripped.
 *
 * @author <a href="mailto:arkin@intalio.com">Assaf Arkin</a>
 * @author Rahul Srivastava
 * @author Elena Litani IBM
 * @see Serializer
 *
 * @deprecated As of JDK 9, Xerces 2.9.0, Xerces DOM L3 Serializer implementation
 * is replaced by that of Xalan. Main class
 * {@link com.sun.org.apache.xml.internal.serialize.DOMSerializerImpl} is replaced
 * by {@link com.sun.org.apache.xml.internal.serializer.dom3.LSSerializerImpl}.
 *
 * @LastModified: Oct 2017
 */
@Deprecated
public class XMLSerializer
extends BaseMarkupSerializer {

    //
    // constants
    //

    protected static final boolean DEBUG = false;

    //
    // data
    //

    //
    // DOM Level 3 implementation: variables intialized in DOMSerializerImpl
    //

    /** stores namespaces in scope */
    protected NamespaceSupport fNSBinder;

    /** stores all namespace bindings on the current element */
    protected NamespaceSupport fLocalNSBinder;

    /** symbol table for serialization */
    protected SymbolTable fSymbolTable;

    protected final static String PREFIX = "NS";

    /**
     * Controls whether namespace fixup should be performed during
     * the serialization.
     * NOTE: if this field is set to true the following
     * fields need to be initialized: fNSBinder, fLocalNSBinder, fSymbolTable,
     * XMLSymbols.EMPTY_STRING, fXmlSymbol, fXmlnsSymbol
     */
    protected boolean fNamespaces = false;

    /**
     * Controls whether namespace prefixes will be printed out during serialization
     */
    protected boolean fNamespacePrefixes = true;


    private boolean fPreserveSpace;


    /**
     * Constructs a new serializer. The serializer cannot be used without
     * calling {@link #setOutputCharStream} or {@link #setOutputByteStream}
     * first.
     */
    public XMLSerializer() {
        super( new OutputFormat( Method.XML, null, false ) );
    }


    /**
     * Constructs a new serializer. The serializer cannot be used without
     * calling {@link #setOutputCharStream} or {@link #setOutputByteStream}
     * first.
     */
    public XMLSerializer( OutputFormat format ) {
        super( format != null ? format : new OutputFormat( Method.XML, null, false ) );
        _format.setMethod( Method.XML );
    }


    /**
     * Constructs a new serializer that writes to the specified writer
     * using the specified output format. If <tt>format</tt> is null,
     * will use a default output format.
     *
     * @param writer The writer to use
     * @param format The output format to use, null for the default
     */
    public XMLSerializer( Writer writer, OutputFormat format ) {
        super( format != null ? format : new OutputFormat( Method.XML, null, false ) );
        _format.setMethod( Method.XML );
        setOutputCharStream( writer );
    }


    /**
     * Constructs a new serializer that writes to the specified output
     * stream using the specified output format. If <tt>format</tt>
     * is null, will use a default output format.
     *
     * @param output The output stream to use
     * @param format The output format to use, null for the default
     */
    public XMLSerializer( OutputStream output, OutputFormat format ) {
        super( format != null ? format : new OutputFormat( Method.XML, null, false ) );
        _format.setMethod( Method.XML );
        setOutputByteStream( output );
    }


    public void setOutputFormat( OutputFormat format ) {
        super.setOutputFormat( format != null ? format : new OutputFormat( Method.XML, null, false ) );
    }


    /**
     * This methods turns on namespace fixup algorithm during
     * DOM serialization.
     * @see org.w3c.dom.ls.LSSerializer
     *
     * @param namespaces
     */
    public void setNamespaces (boolean namespaces){
        fNamespaces = namespaces;
        if (fNSBinder == null) {
            fNSBinder = new NamespaceSupport();
            fLocalNSBinder = new NamespaceSupport();
            fSymbolTable = new SymbolTable();
        }
    }

    //-----------------------------------------//
    // SAX content handler serializing methods //
    //-----------------------------------------//


    public void startElement( String namespaceURI, String localName,
                              String rawName, Attributes attrs )
    throws SAXException
    {
        int          i;
        boolean      preserveSpace;
        ElementState state;
        String       name;
        String       value;

        if (DEBUG) {
            System.out.println("==>startElement("+namespaceURI+","+localName+
                               ","+rawName+")");
        }

        try {
            if (_printer == null) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "NoWriterSupplied", null);
                throw new IllegalStateException(msg);
            }

            state = getElementState();
            if (isDocumentState()) {
                // If this is the root element handle it differently.
                // If the first root element in the document, serialize
                // the document's DOCTYPE. Space preserving defaults
                // to that of the output format.
                if (! _started)
                    startDocument( ( localName == null || localName.length() == 0 ) ? rawName : localName );
            } else {
                // For any other element, if first in parent, then
                // close parent's opening tag and use the parnet's
                // space preserving.
                if (state.empty)
                    _printer.printText( '>' );
                // Must leave CData section first
                if (state.inCData) {
                    _printer.printText( "]]>" );
                    state.inCData = false;
                }
                // Indent this element on a new line if the first
                // content of the parent element or immediately
                // following an element or a comment
                if (_indenting && ! state.preserveSpace &&
                    ( state.empty || state.afterElement || state.afterComment))
                    _printer.breakLine();
            }
            preserveSpace = state.preserveSpace;

            //We remove the namespaces from the attributes list so that they will
            //be in _prefixes
            attrs = extractNamespaces(attrs);

            // Do not change the current element state yet.
            // This only happens in endElement().
            if (rawName == null || rawName.length() == 0) {
                if (localName == null) {
                    String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "NoName", null);
                    throw new SAXException(msg);
                }
                if (namespaceURI != null && ! namespaceURI.equals( "" )) {
                    String prefix;
                    prefix = getPrefix( namespaceURI );
                    if (prefix != null && prefix.length() > 0) {
                        rawName = prefix + ":" + localName;
                    }
                    else {
                        rawName = localName;
                    }
                }
                else {
                    rawName = localName;
                }
            }

            _printer.printText( '<' );
            _printer.printText( rawName );
            _printer.indent();

            // For each attribute print it's name and value as one part,
            // separated with a space so the element can be broken on
            // multiple lines.
            if (attrs != null) {
                for (i = 0 ; i < attrs.getLength() ; ++i) {
                    _printer.printSpace();

                    name = attrs.getQName( i );
                    if (name != null && name.length() == 0) {
                        String prefix;
                        String attrURI;

                        name = attrs.getLocalName( i );
                        attrURI = attrs.getURI( i );
                        if (( attrURI != null && attrURI.length() != 0 ) &&
                            ( namespaceURI == null || namespaceURI.length() == 0 ||
                              ! attrURI.equals( namespaceURI ) )) {
                            prefix = getPrefix( attrURI );
                            if (prefix != null && prefix.length() > 0)
                                name = prefix + ":" + name;
                        }
                    }

                    value = attrs.getValue( i );
                    if (value == null)
                        value = "";
                    _printer.printText( name );
                    _printer.printText( "=\"" );
                    printEscaped( value );
                    _printer.printText( '"' );

                    // If the attribute xml:space exists, determine whether
                    // to preserve spaces in this and child nodes based on
                    // its value.
                    if (name.equals( "xml:space" )) {
                        if (value.equals( "preserve" ))
                            preserveSpace = true;
                        else
                            preserveSpace = _format.getPreserveSpace();
                    }
                }
            }

            if (_prefixes != null) {
                for (Map.Entry<String, String> entry : _prefixes.entrySet()) {
                    _printer.printSpace();
                    value = entry.getKey();
                    name = entry.getValue();
                    if (name.length() == 0) {
                        _printer.printText( "xmlns=\"" );
                        printEscaped( value );
                        _printer.printText( '"' );
                    }
                    else {
                        _printer.printText( "xmlns:" );
                        _printer.printText( name );
                        _printer.printText( "=\"" );
                        printEscaped( value );
                        _printer.printText( '"' );
                    }
                }
            }

            // Now it's time to enter a new element state
            // with the tag name and space preserving.
            // We still do not change the curent element state.
            state = enterElementState( namespaceURI, localName, rawName, preserveSpace );
            name = ( localName == null || localName.length() == 0 ) ? rawName : namespaceURI + "^" + localName;
            state.doCData = _format.isCDataElement( name );
            state.unescaped = _format.isNonEscapingElement( name );
        } catch (IOException except) {
            throw new SAXException( except );
        }
    }


    public void endElement( String namespaceURI, String localName,
                            String rawName )
    throws SAXException
    {
        try {
            endElementIO( namespaceURI, localName, rawName );
        } catch (IOException except) {
            throw new SAXException( except );
        }
    }


    public void endElementIO( String namespaceURI, String localName,
                              String rawName )
    throws IOException
    {
        ElementState state;
        if (DEBUG) {
            System.out.println("==>endElement: " +rawName);
        }
        // Works much like content() with additions for closing
        // an element. Note the different checks for the closed
        // element's state and the parent element's state.
        _printer.unindent();
        state = getElementState();
        if (state.empty) {
            _printer.printText( "/>" );
        } else {
            // Must leave CData section first
            if (state.inCData)
                _printer.printText( "]]>" );
            // This element is not empty and that last content was
            // another element, so print a line break before that
            // last element and this element's closing tag.
            if (_indenting && ! state.preserveSpace && (state.afterElement || state.afterComment))
                _printer.breakLine();
            _printer.printText( "</" );
            _printer.printText( state.rawName );
            _printer.printText( '>' );
        }
        // Leave the element state and update that of the parent
        // (if we're not root) to not empty and after element.
        state = leaveElementState();
        state.afterElement = true;
        state.afterComment = false;
        state.empty = false;
        if (isDocumentState())
            _printer.flush();
    }


    //------------------------------------------//
    // SAX document handler serializing methods //
    //------------------------------------------//


    public void startElement( String tagName, AttributeList attrs )
    throws SAXException
    {
        int          i;
        boolean      preserveSpace;
        ElementState state;
        String       name;
        String       value;


        if (DEBUG) {
            System.out.println("==>startElement("+tagName+")");
        }

        try {
            if (_printer == null) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.SERIALIZER_DOMAIN, "NoWriterSupplied", null);
                throw new IllegalStateException(msg);
            }

            state = getElementState();
            if (isDocumentState()) {
                // If this is the root element handle it differently.
                // If the first root element in the document, serialize
                // the document's DOCTYPE. Space preserving defaults
                // to that of the output format.
                if (! _started)
                    startDocument( tagName );
            } else {
                // For any other element, if first in parent, then
                // close parent's opening tag and use the parnet's
                // space preserving.
                if (state.empty)
                    _printer.printText( '>' );
                // Must leave CData section first
                if (state.inCData) {
                    _printer.printText( "]]>" );
                    state.inCData = false;
                }
                // Indent this element on a new line if the first
                // content of the parent element or immediately
                // following an element.
                if (_indenting && ! state.preserveSpace &&
                    ( state.empty || state.afterElement || state.afterComment))
                    _printer.breakLine();
            }
            preserveSpace = state.preserveSpace;

            // Do not change the current element state yet.
            // This only happens in endElement().

            _printer.printText( '<' );
            _printer.printText( tagName );
            _printer.indent();

            // For each attribute print it's name and value as one part,
            // separated with a space so the element can be broken on
            // multiple lines.
            if (attrs != null) {
                for (i = 0 ; i < attrs.getLength() ; ++i) {
                    _printer.printSpace();
                    name = attrs.getName( i );
                    value = attrs.getValue( i );
                    if (value != null) {
                        _printer.printText( name );
                        _printer.printText( "=\"" );
                        printEscaped( value );
                        _printer.printText( '"' );
                    }

                    // If the attribute xml:space exists, determine whether
                    // to preserve spaces in this and child nodes based on
                    // its value.
                    if (name.equals( "xml:space" )) {
                        if (value.equals( "preserve" ))
                            preserveSpace = true;
                        else
                            preserveSpace = _format.getPreserveSpace();
                    }
                }
            }
            // Now it's time to enter a new element state
            // with the tag name and space preserving.
            // We still do not change the curent element state.
            state = enterElementState( null, null, tagName, preserveSpace );
            state.doCData = _format.isCDataElement( tagName );
            state.unescaped = _format.isNonEscapingElement( tagName );
        } catch (IOException except) {
            throw new SAXException( except );
        }

    }


    public void endElement( String tagName )
    throws SAXException
    {
        endElement( null, null, tagName );
    }



    //------------------------------------------//
    // Generic node serializing methods methods //
    //------------------------------------------//


    /**
     * Called to serialize the document's DOCTYPE by the root element.
     * The document type declaration must name the root element,
     * but the root element is only known when that element is serialized,
     * and not at the start of the document.
     * <p>
     * This method will check if it has not been called before ({@link #_started}),
     * will serialize the document type declaration, and will serialize all
     * pre-root comments and PIs that were accumulated in the document
     * (see {@link #serializePreRoot}). Pre-root will be serialized even if
     * this is not the first root element of the document.
     */
    protected void startDocument( String rootTagName )
    throws IOException
    {
        int    i;
        String dtd;

        dtd = _printer.leaveDTD();
        if (! _started) {

            if (! _format.getOmitXMLDeclaration()) {
                StringBuffer    buffer;

                // Serialize the document declaration appreaing at the head
                // of very XML document (unless asked not to).
                buffer = new StringBuffer( "<?xml version=\"" );
                if (_format.getVersion() != null)
                    buffer.append( _format.getVersion() );
                else
                    buffer.append( "1.0" );
                buffer.append( '"' );
                String format_encoding =  _format.getEncoding();
                if (format_encoding != null) {
                    buffer.append( " encoding=\"" );
                    buffer.append( format_encoding );
                    buffer.append( '"' );
                }
                if (_format.getStandalone() && _docTypeSystemId == null &&
                    _docTypePublicId == null)
                    buffer.append( " standalone=\"yes\"" );
                buffer.append( "?>" );
                _printer.printText( buffer );
                _printer.breakLine();
            }

            if (! _format.getOmitDocumentType()) {
                if (_docTypeSystemId != null) {
                    // System identifier must be specified to print DOCTYPE.
                    // If public identifier is specified print 'PUBLIC
                    // <public> <system>', if not, print 'SYSTEM <system>'.
                    _printer.printText( "<!DOCTYPE " );
                    _printer.printText( rootTagName );
                    if (_docTypePublicId != null) {
                        _printer.printText( " PUBLIC " );
                        printDoctypeURL( _docTypePublicId );
                        if (_indenting) {
                            _printer.breakLine();
                            for (i = 0 ; i < 18 + rootTagName.length() ; ++i)
                                _printer.printText( " " );
                        } else
                            _printer.printText( " " );
                        printDoctypeURL( _docTypeSystemId );
                    } else {
                        _printer.printText( " SYSTEM " );
                        printDoctypeURL( _docTypeSystemId );
                    }

                    // If we accumulated any DTD contents while printing.
                    // this would be the place to print it.
                    if (dtd != null && dtd.length() > 0) {
                        _printer.printText( " [" );
                        printText( dtd, true, true );
                        _printer.printText( ']' );
                    }

                    _printer.printText( ">" );
                    _printer.breakLine();
                } else if (dtd != null && dtd.length() > 0) {
                    _printer.printText( "<!DOCTYPE " );
                    _printer.printText( rootTagName );
                    _printer.printText( " [" );
                    printText( dtd, true, true );
                    _printer.printText( "]>" );
                    _printer.breakLine();
                }
            }
        }
        _started = true;
        // Always serialize these, even if not te first root element.
        serializePreRoot();
    }


    /**
     * Called to serialize a DOM element. Equivalent to calling {@link
     * #startElement}, {@link #endElement} and serializing everything
     * inbetween, but better optimized.
     */
    protected void serializeElement( Element elem )
    throws IOException
    {
        Attr         attr;
        NamedNodeMap attrMap;
        int          i;
        Node         child;
        ElementState state;
        String       name;
        String       value;
        String       tagName;

        String prefix, localUri;
        String uri;
        if (fNamespaces) {
            // local binder stores namespace declaration
            // that has been printed out during namespace fixup of
            // the current element
            fLocalNSBinder.reset();

            // add new namespace context
            fNSBinder.pushContext();
        }

        if (DEBUG) {
            System.out.println("==>startElement: " +elem.getNodeName() +" ns="+elem.getNamespaceURI());
        }
        tagName = elem.getTagName();
        state = getElementState();
        if (isDocumentState()) {
            // If this is the root element handle it differently.
            // If the first root element in the document, serialize
            // the document's DOCTYPE. Space preserving defaults
            // to that of the output format.

            if (! _started) {
                startDocument( tagName);
            }
        } else {
            // For any other element, if first in parent, then
            // close parent's opening tag and use the parent's
            // space preserving.
            if (state.empty)
                _printer.printText( '>' );
            // Must leave CData section first
            if (state.inCData) {
                _printer.printText( "]]>" );
                state.inCData = false;
            }
            // Indent this element on a new line if the first
            // content of the parent element or immediately
            // following an element.
            if (_indenting && ! state.preserveSpace &&
                ( state.empty || state.afterElement || state.afterComment))
                _printer.breakLine();
        }

        // Do not change the current element state yet.
        // This only happens in endElement().
        fPreserveSpace = state.preserveSpace;


        int length = 0;
        attrMap = null;
        // retrieve attributes
        if (elem.hasAttributes()) {
            attrMap = elem.getAttributes();
            length = attrMap.getLength();
        }

        if (!fNamespaces) { // no namespace fixup should be performed

            // serialize element name
            _printer.printText( '<' );
            _printer.printText( tagName );
            _printer.indent();

            // For each attribute print it's name and value as one part,
            // separated with a space so the element can be broken on
            // multiple lines.
            for ( i = 0 ; i < length ; ++i ) {
                attr = (Attr) attrMap.item( i );
                name = attr.getName();
                value = attr.getValue();
                if ( value == null )
                    value = "";
                printAttribute (name, value, attr.getSpecified(), attr);
            }
        } else { // do namespace fixup

            // REVISIT: some optimization could probably be done to avoid traversing
            //          attributes twice.
            //

            // ---------------------------------------
            // record all valid namespace declarations
            // before attempting to fix element's namespace
            // ---------------------------------------

            for (i = 0;i < length;i++) {

                attr = (Attr) attrMap.item( i );
                uri = attr.getNamespaceURI();
                // check if attribute is a namespace decl
                if (uri != null && uri.equals(NamespaceContext.XMLNS_URI)) {

                    value = attr.getNodeValue();
                    if (value == null) {
                        value=XMLSymbols.EMPTY_STRING;
                    }

                    if (value.equals(NamespaceContext.XMLNS_URI)) {
                        if (fDOMErrorHandler != null) {
                            String msg = DOMMessageFormatter.formatMessage(
                                DOMMessageFormatter.XML_DOMAIN,"CantBindXMLNS",null );
                            modifyDOMError(msg,  DOMError.SEVERITY_ERROR, null, attr);
                            boolean continueProcess = fDOMErrorHandler.handleError(fDOMError);
                            if (!continueProcess) {
                                // stop the namespace fixup and validation
                                throw new RuntimeException(
                                    DOMMessageFormatter.formatMessage(
                                    DOMMessageFormatter.SERIALIZER_DOMAIN,
                                    "SerializationStopped", null));
                            }
                        }
                    } else {
                        prefix = attr.getPrefix();
                        prefix = (prefix == null ||
                                  prefix.length() == 0) ? XMLSymbols.EMPTY_STRING :fSymbolTable.addSymbol(prefix);
                        String localpart = fSymbolTable.addSymbol( attr.getLocalName());
                        if (prefix == XMLSymbols.PREFIX_XMLNS) { //xmlns:prefix
                            value = fSymbolTable.addSymbol(value);
                            // record valid decl
                            if (value.length() != 0) {
                                fNSBinder.declarePrefix(localpart, value);
                            } else {
                                // REVISIT: issue error on invalid declarations
                                //          xmlns:foo = ""
                            }
                            continue;
                        }
                        // xmlns --- empty prefix is always bound ("" or some string)
                        value = fSymbolTable.addSymbol(value);
                        fNSBinder.declarePrefix(XMLSymbols.EMPTY_STRING, value);
                        continue;
                    }  // end-else: valid declaration
                } // end-if: namespace declaration
            }  // end-for

            //-----------------------
            // get element uri/prefix
            //-----------------------
            uri = elem.getNamespaceURI();
            prefix = elem.getPrefix();

            //----------------------
            // output element name
            //----------------------
            // REVISIT: this could be removed if we always convert empty string to null
            //          for the namespaces.
            if ((uri !=null && prefix !=null ) && uri.length() == 0 && prefix.length()!=0) {
                // uri is an empty string and element has some prefix
                // the namespace alg later will fix up the namespace attributes
                // remove element prefix
                prefix = null;
                _printer.printText( '<' );
                _printer.printText( elem.getLocalName() );
                _printer.indent();
            } else {
                _printer.printText( '<' );
                _printer.printText( tagName );
                _printer.indent();
            }


            // ---------------------------------------------------------
            // Fix up namespaces for element: per DOM L3
            // Need to consider the following cases:
            //
            // case 1: <foo:elem xmlns:ns1="myURI" xmlns="default"/>
            // Assume "foo", "ns1" are declared on the parent. We should not miss
            // redeclaration for both "ns1" and default namespace. To solve this
            // we add a local binder that stores declaration only for current element.
            // This way we avoid outputing duplicate declarations for the same element
            // as well as we are not omitting redeclarations.
            //
            // case 2: <elem xmlns="" xmlns="default"/>
            // We need to bind default namespace to empty string, to be able to
            // omit duplicate declarations for the same element
            //
            // case 3: <xsl:stylesheet xmlns:xsl="http://xsl">
            // We create another element body bound to the "http://xsl" namespace
            // as well as namespace attribute rebounding xsl to another namespace.
            // <xsl:body xmlns:xsl="http://another">
            // Need to make sure that the new namespace decl value is changed to
            // "http://xsl"
            //
            // ---------------------------------------------------------
            // check if prefix/namespace is correct for current element
            // ---------------------------------------------------------


            if (uri != null) {  // Element has a namespace
                uri = fSymbolTable.addSymbol(uri);
                prefix = (prefix == null ||
                          prefix.length() == 0) ? XMLSymbols.EMPTY_STRING :fSymbolTable.addSymbol(prefix);
                if (fNSBinder.getURI(prefix) == uri) {
                    // The xmlns:prefix=namespace or xmlns="default" was declared at parent.
                    // The binder always stores mapping of empty prefix to "".
                    // (NOTE: local binder does not store this kind of binding!)
                    // Thus the case where element was declared with uri="" (with or without a prefix)
                    // will be covered here.

                } else {
                    // the prefix is either undeclared
                    // or
                    // conflict: the prefix is bound to another URI
                    if (fNamespacePrefixes) {
                        printNamespaceAttr(prefix, uri);
                    }
                    fLocalNSBinder.declarePrefix(prefix, uri);
                    fNSBinder.declarePrefix(prefix, uri);
                }
            } else { // Element has no namespace
                if (elem.getLocalName() == null) {
                    //  DOM Level 1 node!
                    if (fDOMErrorHandler != null) {
                        String msg = DOMMessageFormatter.formatMessage(
                            DOMMessageFormatter.DOM_DOMAIN, "NullLocalElementName",
                            new Object[]{elem.getNodeName()});
                        modifyDOMError(msg,DOMError.SEVERITY_ERROR, null, elem);
                        boolean continueProcess = fDOMErrorHandler.handleError(fDOMError);
                        // REVISIT: should we terminate upon request?
                        if (!continueProcess) {
                           throw new RuntimeException(
                               DOMMessageFormatter.formatMessage(
                               DOMMessageFormatter.SERIALIZER_DOMAIN,
                               "SerializationStopped", null));
                        }
                    }
                } else { // uri=null and no colon (DOM L2 node)
                    uri = fNSBinder.getURI(XMLSymbols.EMPTY_STRING);

                    if (uri !=null && uri.length() > 0) {
                        // there is a default namespace decl that is bound to
                        // non-zero length uri, output xmlns=""
                        if (fNamespacePrefixes) {
                            printNamespaceAttr(XMLSymbols.EMPTY_STRING, XMLSymbols.EMPTY_STRING);
                        }
                        fLocalNSBinder.declarePrefix(XMLSymbols.EMPTY_STRING, XMLSymbols.EMPTY_STRING);
                        fNSBinder.declarePrefix(XMLSymbols.EMPTY_STRING, XMLSymbols.EMPTY_STRING);
                    }
                }
            }


            // -----------------------------------------
            // Fix up namespaces for attributes: per DOM L3
            // check if prefix/namespace is correct the attributes
            // -----------------------------------------

            for (i = 0; i < length; i++) {

                attr = (Attr) attrMap.item( i );
                value = attr.getValue();
                name = attr.getNodeName();

                uri = attr.getNamespaceURI();

                // Fix attribute that was declared with a prefix and namespace=""
                if (uri !=null && uri.length() == 0) {
                    uri=null;
                    // we must remove prefix for this attribute
                    name=attr.getLocalName();
                }

                if (DEBUG) {
                    System.out.println("==>process attribute: "+attr.getNodeName());
                }
                // make sure that value is never null.
                if (value == null) {
                    value=XMLSymbols.EMPTY_STRING;
                }

                if (uri != null) {  // attribute has namespace !=null
                    prefix = attr.getPrefix();
                    prefix = prefix == null ? XMLSymbols.EMPTY_STRING :fSymbolTable.addSymbol(prefix);
                    String localpart = fSymbolTable.addSymbol( attr.getLocalName());



                    // ---------------------------------------------------
                    // print namespace declarations namespace declarations
                    // ---------------------------------------------------
                    if (uri != null && uri.equals(NamespaceContext.XMLNS_URI)) {
                        // check if we need to output this declaration
                        prefix = attr.getPrefix();
                        prefix = (prefix == null ||
                                  prefix.length() == 0) ? XMLSymbols.EMPTY_STRING : fSymbolTable.addSymbol(prefix);
                        localpart = fSymbolTable.addSymbol( attr.getLocalName());
                        if (prefix == XMLSymbols.PREFIX_XMLNS) { //xmlns:prefix
                            localUri = fLocalNSBinder.getURI(localpart);  // local prefix mapping
                            value = fSymbolTable.addSymbol(value);
                            if (value.length() != 0 ) {
                                if (localUri == null) {
                                    // declaration was not printed while fixing element namespace binding

                                    // If the DOM Level 3 namespace-prefixes feature is set to false
                                    // do not print xmlns attributes
                                    if (fNamespacePrefixes) {
                                        printNamespaceAttr(localpart, value);
                                    }

                                    // case 4: <elem xmlns:xx="foo" xx:attr=""/>
                                    // where attribute is bound to "bar".
                                    // If the xmlns:xx is output here first, later we should not
                                    // redeclare "xx" prefix. Instead we would pick up different prefix
                                    // for the attribute.
                                    // final: <elem xmlns:xx="foo" NS1:attr="" xmlns:NS1="bar"/>
                                    fLocalNSBinder.declarePrefix(localpart, value);
                                }
                            } else {
                                // REVISIT: issue error on invalid declarations
                                //          xmlns:foo = ""
                            }
                            continue;
                        }
                        // xmlns --- empty prefix is always bound ("" or some string)
                        uri = fNSBinder.getURI(XMLSymbols.EMPTY_STRING);
                        localUri= fLocalNSBinder.getURI(XMLSymbols.EMPTY_STRING);
                        value = fSymbolTable.addSymbol(value);
                        if (localUri == null ) {
                            // declaration was not printed while fixing element namespace binding
                            if (fNamespacePrefixes) {
                                printNamespaceAttr(XMLSymbols.EMPTY_STRING, value);
                            }
                            // case 4 does not apply here since attributes can't use
                            // default namespace
                        }
                        continue;

                    }
                    uri = fSymbolTable.addSymbol(uri);

                    // find if for this prefix a URI was already declared
                    String declaredURI =  fNSBinder.getURI(prefix);

                    if (prefix == XMLSymbols.EMPTY_STRING || declaredURI != uri) {
                        // attribute has no prefix (default namespace decl does not apply to attributes)
                        // OR
                        // attribute prefix is not declared
                        // OR
                        // conflict: attr URI does not match the prefix in scope

                        name  = attr.getNodeName();
                        // Find if any prefix for attributes namespace URI is available
                        // in the scope
                        String declaredPrefix = fNSBinder.getPrefix(uri);

                        if (declaredPrefix !=null && declaredPrefix !=XMLSymbols.EMPTY_STRING) {
                            // use the prefix that was found
                            prefix = declaredPrefix;
                            name=prefix+":"+localpart;
                        } else {
                            if (DEBUG) {
                                System.out.println("==> cound not find prefix for the attribute: " +prefix);
                            }

                            if (prefix != XMLSymbols.EMPTY_STRING && fLocalNSBinder.getURI(prefix) == null) {
                                // the current prefix is not null and it has no in scope declaration

                                // use this prefix
                            } else {
                                // find a prefix following the pattern "NS" +index (starting at 1)
                                // make sure this prefix is not declared in the current scope.
                                int counter = 1;
                                prefix = fSymbolTable.addSymbol(PREFIX + counter++);
                                while (fLocalNSBinder.getURI(prefix)!=null) {
                                    prefix = fSymbolTable.addSymbol(PREFIX +counter++);
                                }
                                name=prefix+":"+localpart;
                            }
                            // add declaration for the new prefix
                            if (fNamespacePrefixes) {
                                printNamespaceAttr(prefix, uri);
                            }
                            value = fSymbolTable.addSymbol(value);
                            fLocalNSBinder.declarePrefix(prefix, value);
                            fNSBinder.declarePrefix(prefix, uri);
                        }

                        // change prefix for this attribute
                    }

                    printAttribute (name, (value==null)?XMLSymbols.EMPTY_STRING:value, attr.getSpecified(), attr);
                } else { // attribute uri == null
                    if (attr.getLocalName() == null) {
                        if (fDOMErrorHandler != null) {
                            String msg = DOMMessageFormatter.formatMessage(
                                DOMMessageFormatter.DOM_DOMAIN,
                                "NullLocalAttrName", new Object[]{attr.getNodeName()});
                            modifyDOMError(msg, DOMError.SEVERITY_ERROR, null, attr);
                            boolean continueProcess = fDOMErrorHandler.handleError(fDOMError);
                            if (!continueProcess) {
                                // stop the namespace fixup and validation
                                throw new RuntimeException(
                                   DOMMessageFormatter.formatMessage(
                                   DOMMessageFormatter.SERIALIZER_DOMAIN,
                                   "SerializationStopped", null));
                            }
                        }
                        printAttribute (name, value, attr.getSpecified(), attr);
                    } else { // uri=null and no colon

                        // no fix up is needed: default namespace decl does not
                        // apply to attributes
                        printAttribute (name, value, attr.getSpecified(), attr);
                    }
                }
            } // end loop for attributes

        }// end namespace fixup algorithm


        // If element has children, then serialize them, otherwise
        // serialize en empty tag.
        if (elem.hasChildNodes()) {
            // Enter an element state, and serialize the children
            // one by one. Finally, end the element.
            state = enterElementState( null, null, tagName, fPreserveSpace );
            state.doCData = _format.isCDataElement( tagName );
            state.unescaped = _format.isNonEscapingElement( tagName );
            child = elem.getFirstChild();
            while (child != null) {
                serializeNode( child );
                child = child.getNextSibling();
            }
            if (fNamespaces) {
                fNSBinder.popContext();
            }
            endElementIO( null, null, tagName );
        } else {
            if (DEBUG) {
                System.out.println("==>endElement: " +elem.getNodeName());
            }
            if (fNamespaces) {
                fNSBinder.popContext();
            }
            _printer.unindent();
            _printer.printText( "/>" );
            // After element but parent element is no longer empty.
            state.afterElement = true;
            state.afterComment = false;
            state.empty = false;
            if (isDocumentState())
                _printer.flush();
        }
    }



    /**
     * Serializes a namespace attribute with the given prefix and value for URI.
     * In case prefix is empty will serialize default namespace declaration.
     *
     * @param prefix
     * @param uri
     * @exception IOException
     */

    private void printNamespaceAttr(String prefix, String uri) throws IOException{
        _printer.printSpace();
        if (prefix == XMLSymbols.EMPTY_STRING) {
            if (DEBUG) {
                System.out.println("=>add xmlns=\""+uri+"\" declaration");
            }
            _printer.printText( XMLSymbols.PREFIX_XMLNS );
        } else {
            if (DEBUG) {
                System.out.println("=>add xmlns:"+prefix+"=\""+uri+"\" declaration");
            }
            _printer.printText( "xmlns:"+prefix );
        }
        _printer.printText( "=\"" );
        printEscaped( uri );
        _printer.printText( '"' );
    }



    /**
     * Prints attribute.
     * NOTE: xml:space attribute modifies output format
     *
     * @param name
     * @param value
     * @param isSpecified
     * @exception IOException
     */
    private void printAttribute (String name, String value, boolean isSpecified, Attr attr) throws IOException{

        if (isSpecified || (features & DOMSerializerImpl.DISCARDDEFAULT) == 0) {
            if (fDOMFilter !=null &&
                (fDOMFilter.getWhatToShow() & NodeFilter.SHOW_ATTRIBUTE)!= 0) {
                short code = fDOMFilter.acceptNode(attr);
                switch (code) {
                    case NodeFilter.FILTER_REJECT:
                    case NodeFilter.FILTER_SKIP: {
                        return;
                    }
                    default: {
                        // fall through
                    }
                }
            }
            _printer.printSpace();
            _printer.printText( name );
            _printer.printText( "=\"" );
            printEscaped( value );
            _printer.printText( '"' );
        }

        // If the attribute xml:space exists, determine whether
        // to preserve spaces in this and child nodes based on
        // its value.
        if (name.equals( "xml:space" )) {
            if (value.equals( "preserve" ))
                fPreserveSpace = true;
            else
                fPreserveSpace = _format.getPreserveSpace();
        }
    }

    protected String getEntityRef( int ch ) {
        // Encode special XML characters into the equivalent character references.
        // These five are defined by default for all XML documents.
        switch (ch) {
        case '<':
            return "lt";
        case '>':
            return "gt";
        case '"':
            return "quot";
        case '\'':
            return "apos";
        case '&':
            return "amp";
        }
        return null;
    }


    /** Retrieve and remove the namespaces declarations from the list of attributes.
     *
     */
    private Attributes extractNamespaces( Attributes attrs )
    throws SAXException
    {
        AttributesImpl attrsOnly;
        String         rawName;
        int            i;
        int            length;

        if (attrs == null) {
            return null;
        }
        length = attrs.getLength();
        attrsOnly = new AttributesImpl( attrs );

        for (i = length - 1 ; i >= 0 ; --i) {
            rawName = attrsOnly.getQName( i );

            //We have to exclude the namespaces declarations from the attributes
            //Append only when the feature http://xml.org/sax/features/namespace-prefixes"
            //is TRUE
            if (rawName.startsWith( "xmlns" )) {
                if (rawName.length() == 5) {
                    startPrefixMapping( "", attrs.getValue( i ) );
                    attrsOnly.removeAttribute( i );
                } else if (rawName.charAt(5) == ':') {
                    startPrefixMapping(rawName.substring(6), attrs.getValue(i));
                    attrsOnly.removeAttribute( i );
                }
            }
        }
        return attrsOnly;
    }

    //
    // Printing attribute value
    //
    protected void printEscaped(String source) throws IOException {
        int length = source.length();
        for (int i = 0; i < length; ++i) {
            int ch = source.charAt(i);
            if (!XMLChar.isValid(ch)) {
                if (++i < length) {
                    surrogates(ch, source.charAt(i), false);
                } else {
                    fatalError("The character '" + (char) ch + "' is an invalid XML character");
                }
                continue;
            }
            // escape NL, CR, TAB
            if (ch == '\n' || ch == '\r' || ch == '\t') {
                printHex(ch);
            } else if (ch == '<') {
                _printer.printText("&lt;");
            } else if (ch == '&') {
                _printer.printText("&amp;");
            } else if (ch == '"') {
                _printer.printText("&quot;");
            } else if ((ch >= ' ' && _encodingInfo.isPrintable((char) ch))) {
                _printer.printText((char) ch);
            } else {
                printHex(ch);
            }
        }
    }

    /** print text data */
    protected void printXMLChar( int ch) throws IOException {
        if (ch == '\r') {
                        printHex(ch);
        } else if ( ch == '<') {
            _printer.printText("&lt;");
        } else if (ch == '&') {
            _printer.printText("&amp;");
        } else if (ch == '>'){
                // character sequence "]]>" can't appear in content, therefore
                // we should escape '>'
                        _printer.printText("&gt;");
        } else if ( ch == '\n' ||  ch == '\t' ||
                    ( ch >= ' ' && _encodingInfo.isPrintable((char)ch))) {
            _printer.printText((char)ch);
        } else {
                        printHex(ch);
        }
    }

    protected void printText( String text, boolean preserveSpace, boolean unescaped )
    throws IOException {
        int index;
        char ch;
        int length = text.length();
        if ( preserveSpace ) {
            // Preserving spaces: the text must print exactly as it is,
            // without breaking when spaces appear in the text and without
            // consolidating spaces. If a line terminator is used, a line
            // break will occur.
            for ( index = 0 ; index < length ; ++index ) {
                ch = text.charAt( index );
                if (!XMLChar.isValid(ch)) {
                    // check if it is surrogate
                    if (++index <length) {
                        surrogates(ch, text.charAt(index), true);
                    } else {
                        fatalError("The character '"+ch+"' is an invalid XML character");
                    }
                    continue;
                }
                if ( unescaped ) {
                    _printer.printText( ch );
                } else {
                    printXMLChar( ch );
                }
            }
        } else {
            // Not preserving spaces: print one part at a time, and
            // use spaces between parts to break them into different
            // lines. Spaces at beginning of line will be stripped
            // by printing mechanism. Line terminator is treated
            // no different than other text part.
            for ( index = 0 ; index < length ; ++index ) {
                ch = text.charAt( index );
                if (!XMLChar.isValid(ch)) {
                    // check if it is surrogate
                    if (++index <length) {
                        surrogates(ch, text.charAt(index), true);
                    } else {
                        fatalError("The character '"+ch+"' is an invalid XML character");
                    }
                    continue;
                }

                if ( unescaped ) {
                    _printer.printText( ch );
                } else {
                    printXMLChar( ch );
                }
            }
        }
    }



    protected void printText( char[] chars, int start, int length,
                              boolean preserveSpace, boolean unescaped ) throws IOException {

        if ( preserveSpace ) {
            // Preserving spaces: the text must print exactly as it is,
            // without breaking when spaces appear in the text and without
            // consolidating spaces. If a line terminator is used, a line
            // break will occur.
            while ( length-- > 0 ) {
                char ch = chars[start++];
                if (!XMLChar.isValid(ch)) {
                    // check if it is surrogate
                    if ( length-- > 0 ) {
                        surrogates(ch, chars[start++], true);
                    } else {
                        fatalError("The character '"+ch+"' is an invalid XML character");
                    }
                    continue;
                }
                if ( unescaped )
                    _printer.printText( ch );
                else
                    printXMLChar( ch );
            }
        } else {
            // Not preserving spaces: print one part at a time, and
            // use spaces between parts to break them into different
            // lines. Spaces at beginning of line will be stripped
            // by printing mechanism. Line terminator is treated
            // no different than other text part.
            while ( length-- > 0 ) {
                char ch = chars[start++];
                if (!XMLChar.isValid(ch)) {
                    // check if it is surrogate
                    if ( length-- > 0 ) {
                        surrogates(ch, chars[start++], true);
                    } else {
                        fatalError("The character '"+ch+"' is an invalid XML character");
                    }
                    continue;
                }
                if ( unescaped )
                    _printer.printText( ch );
                else
                    printXMLChar( ch );
            }
        }
    }


   /**
    * DOM Level 3:
    * Check a node to determine if it contains unbound namespace prefixes.
    *
    * @param node The node to check for unbound namespace prefices
    */
        protected void checkUnboundNamespacePrefixedNode (Node node) throws IOException{

                if (fNamespaces) {

                        if (DEBUG) {
                            System.out.println("==>serializeNode("+node.getNodeName()+") [Entity Reference - Namespaces on]");
                                System.out.println("==>Declared Prefix Count: " + fNSBinder.getDeclaredPrefixCount());
                                System.out.println("==>Node Name: " + node.getNodeName());
                                System.out.println("==>First Child Node Name: " + node.getFirstChild().getNodeName());
                                System.out.println("==>First Child Node Prefix: " + node.getFirstChild().getPrefix());
                                System.out.println("==>First Child Node NamespaceURI: " + node.getFirstChild().getNamespaceURI());
                        }


                        Node child, next;
                for (child = node.getFirstChild(); child != null; child = next) {
                    next = child.getNextSibling();
                            if (DEBUG) {
                                System.out.println("==>serializeNode("+child.getNodeName()+") [Child Node]");
                                System.out.println("==>serializeNode("+child.getPrefix()+") [Child Node Prefix]");
                    }

                            //If a NamespaceURI is not declared for the current
                            //node's prefix, raise a fatal error.
                            String prefix = child.getPrefix();
                prefix = (prefix == null ||
                        prefix.length() == 0) ? XMLSymbols.EMPTY_STRING : fSymbolTable.addSymbol(prefix);
                            if (fNSBinder.getURI(prefix) == null && prefix != null) {
                                        fatalError("The replacement text of the entity node '"
                                                                + node.getNodeName()
                                                                + "' contains an element node '"
                                                                + child.getNodeName()
                                                                + "' with an undeclared prefix '"
                                                                + prefix + "'.");
                            }

                                if (child.getNodeType() == Node.ELEMENT_NODE) {

                                        NamedNodeMap attrs = child.getAttributes();

                                        for (int i = 0; i< attrs.getLength(); i++ ) {

                                            String attrPrefix = attrs.item(i).getPrefix();
                        attrPrefix = (attrPrefix == null ||
                                attrPrefix.length() == 0) ? XMLSymbols.EMPTY_STRING : fSymbolTable.addSymbol(attrPrefix);
                                            if (fNSBinder.getURI(attrPrefix) == null && attrPrefix != null) {
                                                        fatalError("The replacement text of the entity node '"
                                                                                + node.getNodeName()
                                                                                + "' contains an element node '"
                                                                                + child.getNodeName()
                                                                                + "' with an attribute '"
                                                                                + attrs.item(i).getNodeName()
                                                                                + "' an undeclared prefix '"
                                                                                + attrPrefix + "'.");
                                            }

                                        }

                                }

                                if (child.hasChildNodes()) {
                                        checkUnboundNamespacePrefixedNode(child);
                                }
                }
                }
        }

    public boolean reset() {
        super.reset();
        if (fNSBinder != null){
            fNSBinder.reset();
            // during serialization always have a mapping to empty string
            // so we assume there is a declaration.
            fNSBinder.declarePrefix(XMLSymbols.EMPTY_STRING, XMLSymbols.EMPTY_STRING);
        }
        return true;
    }

}
