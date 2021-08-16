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


// Aug 21, 2000:
//  Added ability to omit DOCTYPE declaration.
//  Reported by Lars Martin <lars@smb-tec.com>
// Aug 25, 2000:
//  Added ability to omit comments.
//  Contributed by Anupam Bagchi <abagchi@jtcsv.com>


package com.sun.org.apache.xml.internal.serialize;


import java.io.UnsupportedEncodingException;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;


/**
 * Specifies an output format to control the serializer. Based on the
 * XSLT specification for output format, plus additional parameters.
 * Used to select the suitable serializer and determine how the
 * document should be formatted on output.
 * <p>
 * The two interesting constructors are:
 * <ul>
 * <li>{@link #OutputFormat(String,String,boolean)} creates a format
 *  for the specified method (XML, HTML, Text, etc), encoding and indentation
 * <li>{@link #OutputFormat(Document,String,boolean)} creates a format
 *  compatible with the document type (XML, HTML, Text, etc), encoding and
 *  indentation
 * </ul>
 *
 *
 * @author <a href="mailto:arkin@intalio.com">Assaf Arkin</a>
 *         <a href="mailto:visco@intalio.com">Keith Visco</a>
 * @see Serializer
 * @see Method
 * @see LineSeparator
 *
 * @deprecated As of JDK 9, Xerces 2.9.0, Xerces DOM L3 Serializer implementation
 * is replaced by that of Xalan. Main class
 * {@link com.sun.org.apache.xml.internal.serialize.DOMSerializerImpl} is replaced
 * by {@link com.sun.org.apache.xml.internal.serializer.dom3.LSSerializerImpl}.
 */
@Deprecated
public class OutputFormat
{


    public static class DTD
    {

        /**
         * Public identifier for HTML 4.01 (Strict) document type.
         */
        public static final String HTMLPublicId = "-//W3C//DTD HTML 4.01//EN";

        /**
         * System identifier for HTML 4.01 (Strict) document type.
         */
        public static final String HTMLSystemId =
            "http://www.w3.org/TR/html4/strict.dtd";

        /**
         * Public identifier for XHTML 1.0 (Strict) document type.
         */
        public static final String XHTMLPublicId =
            "-//W3C//DTD XHTML 1.0 Strict//EN";

        /**
         * System identifier for XHTML 1.0 (Strict) document type.
         */
        public static final String XHTMLSystemId =
            "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd";

    }


    public static class Defaults
    {

        /**
         * If indentation is turned on, the default identation
         * level is 4.
         *
         * @see #setIndenting(boolean)
         */
        public static final int Indent = 4;

        /**
         * The default encoding for Web documents it UTF-8.
         *
         * @see #getEncoding()
         */
        public static final String Encoding = "UTF-8";

        /**
         * The default line width at which to break long lines
         * when identing. This is set to 72.
         */
        public static final int LineWidth = 72;

    }


    /**
     * Holds the output method specified for this document,
     * or null if no method was specified.
     */
    private String _method;


    /**
     * Specifies the version of the output method.
     */
    private String _version;


    /**
     * The indentation level, or zero if no indentation
     * was requested.
     */
    private int _indent = 0;


    /**
     * The encoding to use, if an input stream is used.
     * The default is always UTF-8.
     */
    private String _encoding = Defaults.Encoding;

    /**
     * The EncodingInfo instance for _encoding.
     */
    private EncodingInfo _encodingInfo = null;

    // whether java names for encodings are permitted
    private boolean _allowJavaNames = false;

    /**
     * The specified media type or null.
     */
    private String _mediaType;


    /**
     * The specified document type system identifier, or null.
     */
    private String _doctypeSystem;


    /**
     * The specified document type public identifier, or null.
     */
    private String _doctypePublic;


    /**
     * Ture if the XML declaration should be ommited;
     */
    private boolean _omitXmlDeclaration = false;


    /**
     * Ture if the DOCTYPE declaration should be ommited;
     */
    private boolean _omitDoctype = false;


    /**
     * Ture if comments should be ommited;
     */
    private boolean _omitComments = false;


    /**
     * Ture if the comments should be ommited;
     */
    private boolean _stripComments = false;


    /**
     * True if the document type should be marked as standalone.
     */
    private boolean _standalone = false;


    /**
     * List of element tag names whose text node children must
     * be output as CDATA.
     */
    private String[] _cdataElements;


    /**
     * List of element tag names whose text node children must
     * be output unescaped.
     */
    private String[] _nonEscapingElements;


    /**
     * The selected line separator.
     */
    private String _lineSeparator = LineSeparator.Web;


    /**
     * The line width at which to wrap long lines when indenting.
     */
    private int _lineWidth = Defaults.LineWidth;


    /**
     * True if spaces should be preserved in elements that do not
     * specify otherwise, or specify the default behavior.
     */
    private boolean _preserve = false;
        /** If true, an empty string valued attribute is output as "". If false and
         * and we are using the HTMLSerializer, then only the attribute name is
         * serialized. Defaults to false for backwards compatibility.
         */
        private boolean _preserveEmptyAttributes = false;

    /**
     * Constructs a new output format with the default values.
     */
    public OutputFormat()
    {
    }


    /**
     * Constructs a new output format with the default values for
     * the specified method and encoding. If <tt>indent</tt>
     * is true, the document will be pretty printed with the default
     * indentation level and default line wrapping.
     *
     * @param method The specified output method
     * @param encoding The specified encoding
     * @param indenting True for pretty printing
     * @see #setEncoding
     * @see #setIndenting
     * @see #setMethod
     */
    public OutputFormat( String method, String encoding, boolean indenting )
    {
        setMethod( method );
        setEncoding( encoding );
        setIndenting( indenting );
    }

    /**
     * Returns the method specified for this output format.
     * Typically the method will be <tt>xml</tt>, <tt>html</tt>
     * or <tt>text</tt>, but it might be other values.
     * If no method was specified, null will be returned
     * and the most suitable method will be determined for
     * the document by calling {@link #whichMethod}.
     *
     * @return The specified output method, or null
     */
    public String getMethod()
    {
        return _method;
    }


    /**
     * Sets the method for this output format.
     *
     * @see #getMethod
     * @param method The output method, or null
     */
    public void setMethod( String method )
    {
        _method = method;
    }


    /**
     * Returns the version for this output method.
     * If no version was specified, will return null
     * and the default version number will be used.
     * If the serializerr does not support that particular
     * version, it should default to a supported version.
     *
     * @return The specified method version, or null
     */
    public String getVersion()
    {
        return _version;
    }


    /**
     * Sets the version for this output method.
     * For XML the value would be "1.0", for HTML
     * it would be "4.0".
     *
     * @see #getVersion
     * @param version The output method version, or null
     */
    public void setVersion( String version )
    {
        _version = version;
    }


    /**
     * Returns the indentation specified. If no indentation
     * was specified, zero is returned and the document
     * should not be indented.
     *
     * @return The indentation or zero
     * @see #setIndenting
     */
    public int getIndent()
    {
        return _indent;
    }


    /**
     * Returns true if indentation was specified.
     */
    public boolean getIndenting()
    {
        return ( _indent > 0 );
    }


    /**
     * Sets the indentation. The document will not be
     * indented if the indentation is set to zero.
     * Calling {@link #setIndenting} will reset this
     * value to zero (off) or the default (on).
     *
     * @param indent The indentation, or zero
     */
    public void setIndent( int indent )
    {
        if ( indent < 0 )
            _indent = 0;
        else
            _indent = indent;
    }


    /**
     * Sets the indentation on and off. When set on, the default
     * indentation level and default line wrapping is used
     * (see {@link Defaults#Indent} and {@link Defaults#LineWidth}).
     * To specify a different indentation level or line wrapping,
     * use {@link #setIndent} and {@link #setLineWidth}.
     *
     * @param on True if indentation should be on
     */
    public void setIndenting( boolean on )
    {
        if ( on ) {
            _indent = Defaults.Indent;
            _lineWidth = Defaults.LineWidth;
        } else {
            _indent = 0;
            _lineWidth = 0;
        }
    }


    /**
     * Returns the specified encoding. If no encoding was
     * specified, the default is always "UTF-8".
     *
     * @return The encoding
     */
    public String getEncoding()
    {
        return _encoding;
    }


    /**
     * Sets the encoding for this output method. If no
     * encoding was specified, the default is always "UTF-8".
     * Make sure the encoding is compatible with the one
     * used by the {@link java.io.Writer}.
     *
     * @see #getEncoding
     * @param encoding The encoding, or null
     */
    public void setEncoding( String encoding )
    {
        _encoding = encoding;
        _encodingInfo = null;
    }

    /**
     * Sets the encoding for this output method with an <code>EncodingInfo</code>
     * instance.
     */
    public void setEncoding(EncodingInfo encInfo) {
        _encoding = encInfo.getIANAName();
        _encodingInfo = encInfo;
    }

    /**
     * Returns an <code>EncodingInfo<code> instance for the encoding.
     *
     * @see #setEncoding
     */
    public EncodingInfo getEncodingInfo() throws UnsupportedEncodingException {
        if (_encodingInfo == null)
            _encodingInfo = Encodings.getEncodingInfo(_encoding, _allowJavaNames);
        return _encodingInfo;
    }

    /**
     * Sets whether java encoding names are permitted
     */
    public void setAllowJavaNames (boolean allow) {
        _allowJavaNames = allow;
    }

    /**
     * Returns whether java encoding names are permitted
     */
    public boolean setAllowJavaNames () {
        return _allowJavaNames;
    }

    /**
     * Returns the specified media type, or null.
     * To determine the media type based on the
     * document type, use {@link #whichMediaType}.
     *
     * @return The specified media type, or null
     */
    public String getMediaType()
    {
        return _mediaType;
    }


    /**
     * Sets the media type.
     *
     * @see #getMediaType
     * @param mediaType The specified media type
     */
    public void setMediaType( String mediaType )
    {
        _mediaType = mediaType;
    }


    /**
     * Sets the document type public and system identifiers.
     * Required only if the DOM Document or SAX events do not
     * specify the document type, and one must be present in
     * the serialized document. Any document type specified
     * by the DOM Document or SAX events will override these
     * values.
     *
     * @param publicId The public identifier, or null
     * @param systemId The system identifier, or null
     */
    public void setDoctype( String publicId, String systemId )
    {
        _doctypePublic = publicId;
        _doctypeSystem = systemId;
    }


    /**
     * Returns the specified document type public identifier,
     * or null.
     */
    public String getDoctypePublic()
    {
        return _doctypePublic;
    }


    /**
     * Returns the specified document type system identifier,
     * or null.
     */
    public String getDoctypeSystem()
    {
        return _doctypeSystem;
    }


    /**
     * Returns true if comments should be ommited.
     * The default is false.
     */
    public boolean getOmitComments()
    {
        return _omitComments;
    }


    /**
     * Sets comment omitting on and off.
     *
     * @param omit True if comments should be ommited
     */
    public void setOmitComments( boolean omit )
    {
        _omitComments = omit;
    }


    /**
     * Returns true if the DOCTYPE declaration should
     * be ommited. The default is false.
     */
    public boolean getOmitDocumentType()
    {
        return _omitDoctype;
    }


    /**
     * Sets DOCTYPE declaration omitting on and off.
     *
     * @param omit True if DOCTYPE declaration should be ommited
     */
    public void setOmitDocumentType( boolean omit )
    {
        _omitDoctype = omit;
    }


    /**
     * Returns true if the XML document declaration should
     * be ommited. The default is false.
     */
    public boolean getOmitXMLDeclaration()
    {
        return _omitXmlDeclaration;
    }


    /**
     * Sets XML declaration omitting on and off.
     *
     * @param omit True if XML declaration should be ommited
     */
    public void setOmitXMLDeclaration( boolean omit )
    {
        _omitXmlDeclaration = omit;
    }


    /**
     * Returns true if the document type is standalone.
     * The default is false.
     */
    public boolean getStandalone()
    {
        return _standalone;
    }


    /**
     * Sets document DTD standalone. The public and system
     * identifiers must be null for the document to be
     * serialized as standalone.
     *
     * @param standalone True if document DTD is standalone
     */
    public void setStandalone( boolean standalone )
    {
        _standalone = standalone;
    }


    /**
     * Returns a list of all the elements whose text node children
     * should be output as CDATA, or null if no such elements were
     * specified.
     */
    public String[] getCDataElements()
    {
        return _cdataElements;
    }


    /**
     * Returns true if the text node children of the given elements
     * should be output as CDATA.
     *
     * @param tagName The element's tag name
     * @return True if should serialize as CDATA
     */
    public boolean isCDataElement( String tagName )
    {
        int i;

        if ( _cdataElements == null )
            return false;
        for ( i = 0 ; i < _cdataElements.length ; ++i )
            if ( _cdataElements[ i ].equals( tagName ) )
                return true;
        return false;
    }


    /**
     * Sets the list of elements for which text node children
     * should be output as CDATA.
     *
     * @param cdataElements List of CDATA element tag names
     */
    public void setCDataElements( String[] cdataElements )
    {
        _cdataElements = cdataElements;
    }


    /**
     * Returns a list of all the elements whose text node children
     * should be output unescaped (no character references), or null
     * if no such elements were specified.
     */
    public String[] getNonEscapingElements()
    {
        return _nonEscapingElements;
    }


    /**
     * Returns true if the text node children of the given elements
     * should be output unescaped.
     *
     * @param tagName The element's tag name
     * @return True if should serialize unescaped
     */
    public boolean isNonEscapingElement( String tagName )
    {
        int i;

        if ( _nonEscapingElements == null ) {
            return false;
        }
        for ( i = 0 ; i < _nonEscapingElements.length ; ++i )
            if ( _nonEscapingElements[ i ].equals( tagName ) )
                return true;
        return false;
    }


    /**
     * Sets the list of elements for which text node children
     * should be output unescaped (no character references).
     *
     * @param nonEscapingElements List of unescaped element tag names
     */
    public void setNonEscapingElements( String[] nonEscapingElements )
    {
        _nonEscapingElements = nonEscapingElements;
    }



    /**
     * Returns a specific line separator to use. The default is the
     * Web line separator (<tt>\n</tt>). A string is returned to
     * support double codes (CR + LF).
     *
     * @return The specified line separator
     */
    public String getLineSeparator()
    {
        return _lineSeparator;
    }


    /**
     * Sets the line separator. The default is the Web line separator
     * (<tt>\n</tt>). The machine's line separator can be obtained
     * from the system property <tt>line.separator</tt>, but is only
     * useful if the document is edited on machines of the same type.
     * For general documents, use the Web line separator.
     *
     * @param lineSeparator The specified line separator
     */
    public void setLineSeparator( String lineSeparator )
    {
        if ( lineSeparator == null )
            _lineSeparator =  LineSeparator.Web;
        else
            _lineSeparator = lineSeparator;
    }


    /**
     * Returns true if the default behavior for this format is to
     * preserve spaces. All elements that do not specify otherwise
     * or specify the default behavior will be formatted based on
     * this rule. All elements that specify space preserving will
     * always preserve space.
     */
    public boolean getPreserveSpace()
    {
        return _preserve;
    }


    /**
     * Sets space preserving as the default behavior. The default is
     * space stripping and all elements that do not specify otherwise
     * or use the default value will not preserve spaces.
     *
     * @param preserve True if spaces should be preserved
     */
    public void setPreserveSpace( boolean preserve )
    {
        _preserve = preserve;
    }


    /**
     * Return the selected line width for breaking up long lines.
     * When indenting, and only when indenting, long lines will be
     * broken at space boundaries based on this line width.
     * No line wrapping occurs if this value is zero.
     */
    public int getLineWidth()
    {
        return _lineWidth;
    }


    /**
     * Sets the line width. If zero then no line wrapping will
     * occur. Calling {@link #setIndenting} will reset this
     * value to zero (off) or the default (on).
     *
     * @param lineWidth The line width to use, zero for default
     * @see #getLineWidth
     * @see #setIndenting
     */
    public void setLineWidth( int lineWidth )
    {
        if ( lineWidth <= 0 )
            _lineWidth = 0;
        else
            _lineWidth = lineWidth;
    }
        /**
         * Returns the preserveEmptyAttribute flag. If flag is false, then'
         * attributes with empty string values are output as the attribute
         * name only (in HTML mode).
         * @return preserve the preserve flag
         */     public boolean getPreserveEmptyAttributes () {          return _preserveEmptyAttributes;        }       /**
         * Sets the preserveEmptyAttribute flag. If flag is false, then'
         * attributes with empty string values are output as the attribute
         * name only (in HTML mode).
         * @param preserve the preserve flag
         */     public void setPreserveEmptyAttributes (boolean preserve) {             _preserveEmptyAttributes = preserve;    }

    /**
     * Returns the last printable character based on the selected
     * encoding. Control characters and non-printable characters
     * are always printed as character references.
     */
    public char getLastPrintable()
    {
        if ( getEncoding() != null &&
             ( getEncoding().equalsIgnoreCase( "ASCII" ) ) )
            return 0xFF;
        else
            return 0xFFFF;
    }


    /**
     * Returns the suitable media format for a document
     * output with the specified method.
     */
    public static String whichMediaType( String method )
    {
        if ( method.equalsIgnoreCase( Method.XML ) )
            return "text/xml";
        if ( method.equalsIgnoreCase( Method.HTML ) )
            return "text/html";
        if ( method.equalsIgnoreCase( Method.XHTML ) )
            return "text/html";
        if ( method.equalsIgnoreCase( Method.TEXT ) )
            return "text/plain";
        if ( method.equalsIgnoreCase( Method.FOP ) )
            return "application/pdf";
        return null;
    }


}
