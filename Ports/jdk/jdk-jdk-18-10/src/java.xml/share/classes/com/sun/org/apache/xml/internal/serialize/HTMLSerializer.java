/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
//  Fixed serializer to report IO exception directly, instead at
//  the end of document processing.
//  Reported by Patrick Higgins <phiggins@transzap.com>
// Aug 21, 2000:
//  Fixed bug in startDocument not calling prepare.
//  Reported by Mikael Staldal <d96-mst-ingen-reklam@d.kth.se>
// Aug 21, 2000:
//  Added ability to omit DOCTYPE declaration.
// Sep 1, 2000:
//   If no output format is provided the serializer now defaults
//   to ISO-8859-1 encoding. Reported by Mikael Staldal
//   <d96-mst@d.kth.se>


package com.sun.org.apache.xml.internal.serialize;

import com.sun.org.apache.xerces.internal.dom.DOMMessageFormatter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.AttributeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


/**
 * Implements an HTML/XHTML serializer supporting both DOM and SAX
 * pretty serializing. HTML/XHTML mode is determined in the
 * constructor.  For usage instructions see {@link Serializer}.
 * <p>
 * If an output stream is used, the encoding is taken from the
 * output format (defaults to <tt>UTF-8</tt>). If a writer is
 * used, make sure the writer uses the same encoding (if applies)
 * as specified in the output format.
 * <p>
 * The serializer supports both DOM and SAX. DOM serializing is done
 * by calling {@link #serialize} and SAX serializing is done by firing
 * SAX events and using the serializer as a document handler.
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
 * <p>
 * XHTML is slightly different than HTML:
 * <ul>
 * <li>Element/attribute names are lower case and case matters
 * <li>Attributes must specify value, even if empty string
 * <li>Empty elements must have '/' in empty tag
 * <li>Contents of SCRIPT and STYLE elements serialized as CDATA
 * </ul>
 *
 * @deprecated This class was deprecated in Xerces 2.6.2. It is
 * recommended that new applications use JAXP's Transformation API
 * for XML (TrAX) for serializing HTML. See the Xerces documentation
 * for more information.
 * @author <a href="mailto:arkin@intalio.com">Assaf Arkin</a>
 * @see Serializer
 */
@Deprecated
public class HTMLSerializer
    extends BaseMarkupSerializer
{


    /**
     * True if serializing in XHTML format.
     */
    private boolean _xhtml;


    public static final String XHTMLNamespace = "http://www.w3.org/1999/xhtml";

    // for users to override XHTMLNamespace if need be.
    private String fUserXHTMLNamespace = null;


    /**
     * Constructs a new HTML/XHTML serializer depending on the value of
     * <tt>xhtml</tt>. The serializer cannot be used without calling
     * {@link #setOutputCharStream} or {@link #setOutputByteStream} first.
     *
     * @param xhtml True if XHTML serializing
     */
    protected HTMLSerializer( boolean xhtml, OutputFormat format )
    {
        super( format );
        _xhtml = xhtml;
    }


    /**
     * Constructs a new serializer. The serializer cannot be used without
     * calling {@link #setOutputCharStream} or {@link #setOutputByteStream}
     * first.
     */
    public HTMLSerializer()
    {
        this( false, new OutputFormat( Method.HTML, "ISO-8859-1", false ) );
    }


    /**
     * Constructs a new serializer. The serializer cannot be used without
     * calling {@link #setOutputCharStream} or {@link #setOutputByteStream}
     * first.
     */
    public HTMLSerializer( OutputFormat format )
    {
        this( false, format != null ? format : new OutputFormat( Method.HTML, "ISO-8859-1", false ) );
    }



    /**
     * Constructs a new serializer that writes to the specified writer
     * using the specified output format. If <tt>format</tt> is null,
     * will use a default output format.
     *
     * @param writer The writer to use
     * @param format The output format to use, null for the default
     */
    public HTMLSerializer( Writer writer, OutputFormat format )
    {
        this( false, format != null ? format : new OutputFormat( Method.HTML, "ISO-8859-1", false ) );
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
    public HTMLSerializer( OutputStream output, OutputFormat format )
    {
        this( false, format != null ? format : new OutputFormat( Method.HTML, "ISO-8859-1", false ) );
        setOutputByteStream( output );
    }


    public void setOutputFormat( OutputFormat format )
    {
        super.setOutputFormat( format != null ? format : new OutputFormat( Method.HTML, "ISO-8859-1", false ) );
    }

    // Set  value for alternate XHTML namespace.
    public void setXHTMLNamespace(String newNamespace) {
        fUserXHTMLNamespace = newNamespace;
    } // setXHTMLNamespace(String)

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
        String       htmlName;
        boolean      addNSAttr = false;

        try {
            if ( _printer == null )
                throw new IllegalStateException(
                                    DOMMessageFormatter.formatMessage(
                                    DOMMessageFormatter.SERIALIZER_DOMAIN,
                    "NoWriterSupplied", null));

            state = getElementState();
            if ( isDocumentState() ) {
                // If this is the root element handle it differently.
                // If the first root element in the document, serialize
                // the document's DOCTYPE. Space preserving defaults
                // to that of the output format.
                if ( ! _started )
                    startDocument( (localName == null || localName.length() == 0)
                        ? rawName : localName );
            } else {
                // For any other element, if first in parent, then
                // close parent's opening tag and use the parnet's
                // space preserving.
                if ( state.empty )
                    _printer.printText( '>' );
                // Indent this element on a new line if the first
                // content of the parent element or immediately
                // following an element.
                if ( _indenting && ! state.preserveSpace &&
                     ( state.empty || state.afterElement ) )
                    _printer.breakLine();
            }
            preserveSpace = state.preserveSpace;

            // Do not change the current element state yet.
            // This only happens in endElement().

            // As per SAX2, the namespace URI is an empty string if the element has no
            // namespace URI, or namespaces is turned off. The check against null protects
            // against broken SAX implementations, so I've left it there. - mrglavas
            boolean hasNamespaceURI = (namespaceURI != null && namespaceURI.length() != 0);

            // SAX2: rawName (QName) could be empty string if
            // namespace-prefixes property is false.
            if ( rawName == null || rawName.length() == 0) {
                rawName = localName;
                if ( hasNamespaceURI ) {
                    String prefix;
                    prefix = getPrefix( namespaceURI );
                    if ( prefix != null && prefix.length() != 0 )
                        rawName = prefix + ":" + localName;
                }
                addNSAttr = true;
            }
            if ( !hasNamespaceURI )
                htmlName = rawName;
            else {
                if ( namespaceURI.equals( XHTMLNamespace ) ||
                        (fUserXHTMLNamespace != null && fUserXHTMLNamespace.equals(namespaceURI)) )
                    htmlName = localName;
                else
                    htmlName = null;
            }

            // XHTML: element names are lower case, DOM will be different
            _printer.printText( '<' );
            if ( _xhtml )
                _printer.printText( rawName.toLowerCase(Locale.ENGLISH) );
            else
                _printer.printText( rawName );
            _printer.indent();

            // For each attribute serialize it's name and value as one part,
            // separated with a space so the element can be broken on
            // multiple lines.
            if ( attrs != null ) {
                for ( i = 0 ; i < attrs.getLength() ; ++i ) {
                    _printer.printSpace();
                    name = attrs.getQName( i ).toLowerCase(Locale.ENGLISH);
                    value = attrs.getValue( i );
                    if ( _xhtml || hasNamespaceURI ) {
                        // XHTML: print empty string for null values.
                        if ( value == null ) {
                            _printer.printText( name );
                            _printer.printText( "=\"\"" );
                        } else {
                            _printer.printText( name );
                            _printer.printText( "=\"" );
                            printEscaped( value );
                            _printer.printText( '"' );
                        }
                    } else {
                        // HTML: Empty values print as attribute name, no value.
                        // HTML: URI attributes will print unescaped
                        if ( value == null ) {
                            value = "";
                        }
                        if ( !_format.getPreserveEmptyAttributes() && value.length() == 0 )
                            _printer.printText( name );
                        else if ( HTMLdtd.isURI( rawName, name ) ) {
                            _printer.printText( name );
                            _printer.printText( "=\"" );
                            _printer.printText( escapeURI( value ) );
                            _printer.printText( '"' );
                        } else if ( HTMLdtd.isBoolean( rawName, name ) )
                            _printer.printText( name );
                        else {
                            _printer.printText( name );
                            _printer.printText( "=\"" );
                            printEscaped( value );
                            _printer.printText( '"' );
                        }
                    }
                }
            }
            if ( htmlName != null && HTMLdtd.isPreserveSpace( htmlName ) )
                preserveSpace = true;

            if ( addNSAttr ) {
                for (Map.Entry<String, String> entry : _prefixes.entrySet()) {
                    _printer.printSpace();
                    value = entry.getKey(); //The prefixes map uses the URI value as key.
                    name = entry.getValue(); //and prefix name as value
                    if ( name.length() == 0 ) {
                        _printer.printText( "xmlns=\"" );
                        printEscaped( value );
                        _printer.printText( '"' );
                    } else {
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

            // Prevents line breaks inside A/TD

            if ( htmlName != null && ( htmlName.equalsIgnoreCase( "A" ) ||
                                       htmlName.equalsIgnoreCase( "TD" ) ) ) {
                state.empty = false;
                _printer.printText( '>' );
            }

            // Handle SCRIPT and STYLE specifically by changing the
            // state of the current element to CDATA (XHTML) or
            // unescaped (HTML).
            if ( htmlName != null && ( rawName.equalsIgnoreCase( "SCRIPT" ) ||
                                       rawName.equalsIgnoreCase( "STYLE" ) ) ) {
                if ( _xhtml ) {
                    // XHTML: Print contents as CDATA section
                    state.doCData = true;
                } else {
                    // HTML: Print contents unescaped
                    state.unescaped = true;
                }
            }
        } catch ( IOException except ) {
            throw new SAXException( except );
        }
    }


    public void endElement( String namespaceURI, String localName,
                            String rawName )
        throws SAXException
    {
        try {
            endElementIO( namespaceURI, localName, rawName );
        } catch ( IOException except ) {
            throw new SAXException( except );
        }
    }


    public void endElementIO( String namespaceURI, String localName,
                              String rawName )
        throws IOException
    {
        ElementState state;
        String       htmlName;

        // Works much like content() with additions for closing
        // an element. Note the different checks for the closed
        // element's state and the parent element's state.
        _printer.unindent();
        state = getElementState();

        if ( state.namespaceURI == null || state.namespaceURI.length() == 0 )
            htmlName = state.rawName;
        else {
            if ( state.namespaceURI.equals( XHTMLNamespace ) ||
                        (fUserXHTMLNamespace != null && fUserXHTMLNamespace.equals(state.namespaceURI)) )
                htmlName = state.localName;
            else
                htmlName = null;
        }

        if ( _xhtml) {
            if ( state.empty ) {
                _printer.printText( " />" );
            } else {
                // Must leave CData section first
                if ( state.inCData )
                    _printer.printText( "]]>" );
                // XHTML: element names are lower case, DOM will be different
                _printer.printText( "</" );
                _printer.printText( state.rawName.toLowerCase(Locale.ENGLISH) );
                _printer.printText( '>' );
            }
        } else {
            if ( state.empty )
                _printer.printText( '>' );
            // This element is not empty and that last content was
            // another element, so print a line break before that
            // last element and this element's closing tag.
            // [keith] Provided this is not an anchor.
            // HTML: some elements do not print closing tag (e.g. LI)
            if ( htmlName == null || ! HTMLdtd.isOnlyOpening( htmlName ) ) {
                if ( _indenting && ! state.preserveSpace && state.afterElement )
                    _printer.breakLine();
                // Must leave CData section first (Illegal in HTML, but still)
                if ( state.inCData )
                    _printer.printText( "]]>" );
                _printer.printText( "</" );
                _printer.printText( state.rawName );
                _printer.printText( '>' );
            }
        }
        // Leave the element state and update that of the parent
        // (if we're not root) to not empty and after element.
        state = leaveElementState();
        // Temporary hack to prevent line breaks inside A/TD
        if ( htmlName == null || ( ! htmlName.equalsIgnoreCase( "A" ) &&
                                   ! htmlName.equalsIgnoreCase( "TD" ) ) )

            state.afterElement = true;
        state.empty = false;
        if ( isDocumentState() )
            _printer.flush();
    }


    //------------------------------------------//
    // SAX document handler serializing methods //
    //------------------------------------------//


    public void characters( char[] chars, int start, int length )
        throws SAXException
    {
        ElementState state;

        try {
            // HTML: no CDATA section
            state = content();
            state.doCData = false;
            super.characters( chars, start, length );
        } catch ( IOException except ) {
            throw new SAXException( except );
        }
    }


    public void startElement( String tagName, AttributeList attrs )
        throws SAXException
    {
        int          i;
        boolean      preserveSpace;
        ElementState state;
        String       name;
        String       value;

        try {
            if ( _printer == null )
                throw new IllegalStateException(
                                    DOMMessageFormatter.formatMessage(
                                    DOMMessageFormatter.SERIALIZER_DOMAIN,
                    "NoWriterSupplied", null));


            state = getElementState();
            if ( isDocumentState() ) {
                // If this is the root element handle it differently.
                // If the first root element in the document, serialize
                // the document's DOCTYPE. Space preserving defaults
                // to that of the output format.
                if ( ! _started )
                    startDocument( tagName );
            } else {
                // For any other element, if first in parent, then
                // close parent's opening tag and use the parnet's
                // space preserving.
                if ( state.empty )
                    _printer.printText( '>' );
                // Indent this element on a new line if the first
                // content of the parent element or immediately
                // following an element.
                if ( _indenting && ! state.preserveSpace &&
                     ( state.empty || state.afterElement ) )
                    _printer.breakLine();
            }
            preserveSpace = state.preserveSpace;

            // Do not change the current element state yet.
            // This only happens in endElement().

            // XHTML: element names are lower case, DOM will be different
            _printer.printText( '<' );
            if ( _xhtml )
                _printer.printText( tagName.toLowerCase(Locale.ENGLISH) );
            else
                _printer.printText( tagName );
            _printer.indent();

            // For each attribute serialize it's name and value as one part,
            // separated with a space so the element can be broken on
            // multiple lines.
            if ( attrs != null ) {
                for ( i = 0 ; i < attrs.getLength() ; ++i ) {
                    _printer.printSpace();
                    name = attrs.getName( i ).toLowerCase(Locale.ENGLISH);
                    value = attrs.getValue( i );
                    if ( _xhtml ) {
                        // XHTML: print empty string for null values.
                        if ( value == null ) {
                            _printer.printText( name );
                            _printer.printText( "=\"\"" );
                        } else {
                            _printer.printText( name );
                            _printer.printText( "=\"" );
                            printEscaped( value );
                            _printer.printText( '"' );
                        }
                    } else {
                        // HTML: Empty values print as attribute name, no value.
                        // HTML: URI attributes will print unescaped
                        if ( value == null ) {
                            value = "";
                        }
                        if ( !_format.getPreserveEmptyAttributes() && value.length() == 0 )
                            _printer.printText( name );
                        else if ( HTMLdtd.isURI( tagName, name ) ) {
                            _printer.printText( name );
                            _printer.printText( "=\"" );
                            _printer.printText( escapeURI( value ) );
                            _printer.printText( '"' );
                        } else if ( HTMLdtd.isBoolean( tagName, name ) )
                            _printer.printText( name );
                        else {
                            _printer.printText( name );
                            _printer.printText( "=\"" );
                            printEscaped( value );
                            _printer.printText( '"' );
                        }
                    }
                }
            }
            if ( HTMLdtd.isPreserveSpace( tagName ) )
                preserveSpace = true;

            // Now it's time to enter a new element state
            // with the tag name and space preserving.
            // We still do not change the curent element state.
            state = enterElementState( null, null, tagName, preserveSpace );

            // Prevents line breaks inside A/TD
            if ( tagName.equalsIgnoreCase( "A" ) || tagName.equalsIgnoreCase( "TD" ) ) {
                state.empty = false;
                _printer.printText( '>' );
            }

            // Handle SCRIPT and STYLE specifically by changing the
            // state of the current element to CDATA (XHTML) or
            // unescaped (HTML).
            if ( tagName.equalsIgnoreCase( "SCRIPT" ) ||
                 tagName.equalsIgnoreCase( "STYLE" ) ) {
                if ( _xhtml ) {
                    // XHTML: Print contents as CDATA section
                    state.doCData = true;
                } else {
                    // HTML: Print contents unescaped
                    state.unescaped = true;
                }
            }
        } catch ( IOException except ) {
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
        StringBuffer buffer;

        // Not supported in HTML/XHTML, but we still have to switch
        // out of DTD mode.
        _printer.leaveDTD();
        if ( ! _started ) {
            // If the public and system identifiers were not specified
            // in the output format, use the appropriate ones for HTML
            // or XHTML.
            if ( _docTypePublicId == null && _docTypeSystemId == null ) {
                if ( _xhtml ) {
                    _docTypePublicId = HTMLdtd.XHTMLPublicId;
                    _docTypeSystemId = HTMLdtd.XHTMLSystemId;
                } else {
                    _docTypePublicId = HTMLdtd.HTMLPublicId;
                    _docTypeSystemId = HTMLdtd.HTMLSystemId;
                }
            }

            if ( ! _format.getOmitDocumentType() ) {
                // XHTML: If public identifier and system identifier
                //  specified, print them, else print just system identifier
                // HTML: If public identifier specified, print it with
                //  system identifier, if specified.
                // XHTML requires that all element names are lower case, so the
                // root on the DOCTYPE must be 'html'. - mrglavas
                if ( _docTypePublicId != null && ( ! _xhtml || _docTypeSystemId != null )  ) {
                    if (_xhtml) {
                        _printer.printText( "<!DOCTYPE html PUBLIC " );
                    }
                    else {
                        _printer.printText( "<!DOCTYPE HTML PUBLIC " );
                    }
                    printDoctypeURL( _docTypePublicId );
                    if ( _docTypeSystemId != null ) {
                        if ( _indenting ) {
                            _printer.breakLine();
                            _printer.printText( "                      " );
                        } else
                        _printer.printText( ' ' );
                        printDoctypeURL( _docTypeSystemId );
                    }
                    _printer.printText( '>' );
                    _printer.breakLine();
                } else if ( _docTypeSystemId != null ) {
                    if (_xhtml) {
                        _printer.printText( "<!DOCTYPE html SYSTEM " );
                    }
                    else {
                        _printer.printText( "<!DOCTYPE HTML SYSTEM " );
                    }
                    printDoctypeURL( _docTypeSystemId );
                    _printer.printText( '>' );
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
        boolean      preserveSpace;
        String       name;
        String       value;
        String       tagName;

        tagName = elem.getTagName();
        state = getElementState();
        if ( isDocumentState() ) {
            // If this is the root element handle it differently.
            // If the first root element in the document, serialize
            // the document's DOCTYPE. Space preserving defaults
            // to that of the output format.
            if ( ! _started )
                startDocument( tagName );
        } else {
            // For any other element, if first in parent, then
            // close parent's opening tag and use the parnet's
            // space preserving.
            if ( state.empty )
                _printer.printText( '>' );
            // Indent this element on a new line if the first
            // content of the parent element or immediately
            // following an element.
            if ( _indenting && ! state.preserveSpace &&
                 ( state.empty || state.afterElement ) )
                _printer.breakLine();
        }
        preserveSpace = state.preserveSpace;

        // Do not change the current element state yet.
        // This only happens in endElement().

        // XHTML: element names are lower case, DOM will be different
        _printer.printText( '<' );
        if ( _xhtml )
            _printer.printText( tagName.toLowerCase(Locale.ENGLISH) );
        else
            _printer.printText( tagName );
        _printer.indent();

        // Lookup the element's attribute, but only print specified
        // attributes. (Unspecified attributes are derived from the DTD.
        // For each attribute print it's name and value as one part,
        // separated with a space so the element can be broken on
        // multiple lines.
        attrMap = elem.getAttributes();
        if ( attrMap != null ) {
            for ( i = 0 ; i < attrMap.getLength() ; ++i ) {
                attr = (Attr) attrMap.item( i );
                name = attr.getName().toLowerCase(Locale.ENGLISH);
                value = attr.getValue();
                if ( attr.getSpecified() ) {
                    _printer.printSpace();
                    if ( _xhtml ) {
                        // XHTML: print empty string for null values.
                        if ( value == null ) {
                            _printer.printText( name );
                            _printer.printText( "=\"\"" );
                        } else {
                            _printer.printText( name );
                            _printer.printText( "=\"" );
                            printEscaped( value );
                            _printer.printText( '"' );
                        }
                    } else {
                        // HTML: Empty values print as attribute name, no value.
                        // HTML: URI attributes will print unescaped
                        if ( value == null ) {
                            value = "";
                        }
                        if ( !_format.getPreserveEmptyAttributes() && value.length() == 0 )
                            _printer.printText( name );
                        else if ( HTMLdtd.isURI( tagName, name ) ) {
                            _printer.printText( name );
                            _printer.printText( "=\"" );
                            _printer.printText( escapeURI( value ) );
                            _printer.printText( '"' );
                        } else if ( HTMLdtd.isBoolean( tagName, name ) )
                            _printer.printText( name );
                        else {
                            _printer.printText( name );
                            _printer.printText( "=\"" );
                            printEscaped( value );
                            _printer.printText( '"' );
                        }
                    }
                }
            }
        }
        if ( HTMLdtd.isPreserveSpace( tagName ) )
            preserveSpace = true;

        // If element has children, or if element is not an empty tag,
        // serialize an opening tag.
        if ( elem.hasChildNodes() || ! HTMLdtd.isEmptyTag( tagName ) ) {
            // Enter an element state, and serialize the children
            // one by one. Finally, end the element.
            state = enterElementState( null, null, tagName, preserveSpace );

            // Prevents line breaks inside A/TD
            if ( tagName.equalsIgnoreCase( "A" ) || tagName.equalsIgnoreCase( "TD" ) ) {
                state.empty = false;
                _printer.printText( '>' );
            }

            // Handle SCRIPT and STYLE specifically by changing the
            // state of the current element to CDATA (XHTML) or
            // unescaped (HTML).
            if ( tagName.equalsIgnoreCase( "SCRIPT" ) ||
                 tagName.equalsIgnoreCase( "STYLE" ) ) {
                if ( _xhtml ) {
                    // XHTML: Print contents as CDATA section
                    state.doCData = true;
                } else {
                    // HTML: Print contents unescaped
                    state.unescaped = true;
                }
            }
            child = elem.getFirstChild();
            while ( child != null ) {
                serializeNode( child );
                child = child.getNextSibling();
            }
            endElementIO( null, null, tagName );
        } else {
            _printer.unindent();
            // XHTML: Close empty tag with ' />' so it's XML and HTML compatible.
            // HTML: Empty tags are defined as such in DTD no in document.
            if ( _xhtml )
                _printer.printText( " />" );
            else
                _printer.printText( '>' );
            // After element but parent element is no longer empty.
            state.afterElement = true;
            state.empty = false;
            if ( isDocumentState() )
                _printer.flush();
        }
    }



    protected void characters( String text )
        throws IOException
    {
        ElementState state;

        // HTML: no CDATA section
        state = content();
        super.characters( text );
    }


    protected String getEntityRef( int ch )
    {
        return HTMLdtd.fromChar( ch );
    }


    protected String escapeURI( String uri )
    {
        int index;

        // XXX  Apparently Netscape doesn't like if we escape the URI
        //      using %nn, so we leave it as is, just remove any quotes.
        index = uri.indexOf( "\"" );
        if ( index >= 0 )
            return uri.substring( 0, index );
        else
            return uri;
    }


}
