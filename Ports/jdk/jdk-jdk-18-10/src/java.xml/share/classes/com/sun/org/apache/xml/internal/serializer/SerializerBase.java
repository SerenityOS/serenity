/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xml.internal.serializer;

import com.sun.org.apache.xml.internal.serializer.utils.MsgKey;
import com.sun.org.apache.xml.internal.serializer.utils.Utils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.Transformer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.Locator2;

/**
 * This class acts as a base class for the XML "serializers"
 * and the stream serializers.
 * It contains a number of common fields and methods.
 *
 * @xsl.usage internal
 * @LastModified: May 2021
 */
public abstract class SerializerBase
    implements SerializationHandler, SerializerConstants
{
    protected ErrorListener m_errListener;

    /**
     * To fire off the end element trace event
     * @param name Name of element
     */
    protected void fireEndElem(String name)
        throws org.xml.sax.SAXException
    {
        if (m_tracer != null) {
            flushMyWriter();
            m_tracer.fireGenerateEvent(SerializerTrace.EVENTTYPE_ENDELEMENT,name, (Attributes)null);
        }
    }

    /**
     * Report the characters trace event
     * @param chars  content of characters
     * @param start  starting index of characters to output
     * @param length  number of characters to output
     */
    protected void fireCharEvent(char[] chars, int start, int length)
        throws org.xml.sax.SAXException
    {
        if (m_tracer != null) {
            flushMyWriter();
            m_tracer.fireGenerateEvent(SerializerTrace.EVENTTYPE_CHARACTERS, chars, start,length);
        }
    }

    /**
     * true if we still need to call startDocumentInternal()
     */
    protected boolean m_needToCallStartDocument = true;

    /** True if a trailing "]]>" still needs to be written to be
     * written out. Used to merge adjacent CDATA sections
     */
    protected boolean m_cdataTagOpen = false;

    /**
     * All the attributes of the current element, collected from
     * startPrefixMapping() calls, or addAddtribute() calls, or
     * from the SAX attributes in a startElement() call.
     */
    protected AttributesImplSerializer m_attributes = new AttributesImplSerializer();

    /**
     * Tells if we're in an EntityRef event, true if it's greater than 0. Use
     * integer type to handle nested entity reference, increase m_inEntityRef in
     * startEntity, decrease m_inEntityRef in endEntity.
     */
    protected int m_inEntityRef = 0;

    /** This flag is set while receiving events from the external DTD */
    protected boolean m_inExternalDTD = false;

    /**
     * The System ID for the doc type.
     */
    protected String m_doctypeSystem;

    /**
     * The public ID for the doc type.
     */
    protected String m_doctypePublic;

    /**
     * Flag to tell that we need to add the doctype decl, which we can't do
     * until the first element is encountered.
     */
    boolean m_needToOutputDocTypeDecl = true;

    /**
     * Tells if we should write the XML declaration.
     */
    protected boolean m_shouldNotWriteXMLHeader = false;

    /**
     * The standalone value for the doctype.
     */
    private String m_standalone;

    /**
     * True if standalone was specified.
     */
    protected boolean m_standaloneWasSpecified = false;

    /**
     * Determine if the output is a standalone.
     */
    protected boolean m_isStandalone = false;

    /**
     * Flag to tell if indenting (pretty-printing) is on.
     */
    protected boolean m_doIndent = false;

    /**
     * Amount to indent.
     */
    protected int m_indentAmount = 4;

    /**
     * Tells the XML version, for writing out to the XML decl.
     */
    protected String m_version = null;

    /**
     * The mediatype.  Not used right now.
     */
    protected String m_mediatype;

    /**
     * The transformer that was around when this output handler was created (if
     * any).
     */
    private Transformer m_transformer;

    /**
     * Namespace support, that keeps track of currently defined
     * prefix/uri mappings. As processed elements come and go, so do
     * the associated mappings for that element.
     */
    protected NamespaceMappings m_prefixMap;

    /**
     * Handle for firing generate events.  This interface may be implemented
     * by the referenced transformer object.
     */
    protected SerializerTrace m_tracer;

    protected SourceLocator m_sourceLocator;

    /**
     * The writer to send output to. This field is only used in the ToStream
     * serializers, but exists here just so that the fireStartDoc() and
     * other fire... methods can flush this writer when tracing.
     */
    protected java.io.Writer m_writer = null;

    /**
     * A reference to "stack frame" corresponding to
     * the current element. Such a frame is pushed at a startElement()
     * and popped at an endElement(). This frame contains information about
     * the element, such as its namespace URI.
     */
    protected ElemContext m_elemContext = new ElemContext();

    /**
     * A utility buffer for converting Strings passed to
     * character() methods to character arrays.
     * Reusing this buffer means not creating a new character array
     * everytime and it runs faster.
     */
    protected char[] m_charsBuff = new char[60];

    /**
     * A utility buffer for converting Strings passed to
     * attribute methods to character arrays.
     * Reusing this buffer means not creating a new character array
     * everytime and it runs faster.
     */
    protected char[] m_attrBuff = new char[30];

    private Locator m_locator = null;

    protected boolean m_needToCallSetDocumentInfo = true;

    /**
     * Receive notification of a comment.
     *
     * @see ExtendedLexicalHandler#comment(String)
     */
    public void comment(String data) throws SAXException {
        final int length = data.length();
        if (length > m_charsBuff.length) {
            m_charsBuff = new char[length * 2 + 1];
        }
        data.getChars(0, length, m_charsBuff, 0);
        comment(m_charsBuff, 0, length);
    }

    /**
     * If at runtime, when the qname of the attribute is
     * known, another prefix is specified for the attribute, then we can
     * patch or hack the name with this method. For
     * a qname of the form "ns?:otherprefix:name", this function patches the
     * qname by simply ignoring "otherprefix".
     * TODO: This method is a HACK! We do not have access to the
     * XML file, it sometimes generates a NS prefix of the form "ns?" for
     * an attribute.
     */
    protected String patchName(String qname) {
        final int lastColon = qname.lastIndexOf(':');

        if (lastColon > 0) {
            final int firstColon = qname.indexOf(':');
            final String prefix = qname.substring(0, firstColon);
            final String localName = qname.substring(lastColon + 1);

            // If uri is "" then ignore prefix
            final String uri = m_prefixMap.lookupNamespace(prefix);
            if (uri != null && uri.length() == 0) {
                return localName;
            } else if (firstColon != lastColon) {
                return prefix + ':' + localName;
            }
        }
        return qname;
    }

    /**
     * Returns the local name of a qualified name. If the name has no prefix,
     * then it works as the identity (SAX2).
     * @param qname the qualified name
     * @return the name, but excluding any prefix and colon.
     */
    protected static String getLocalName(String qname) {
        final int col = qname.lastIndexOf(':');
        return (col > 0) ? qname.substring(col + 1) : qname;
    }

    /**
     * Receive an object for locating the origin of SAX document events.
     *
     * @param locator An object that can return the location of any SAX document
     * event.
     *
     * Receive an object for locating the origin of SAX document events.
     *
     * <p>SAX parsers are strongly encouraged (though not absolutely
     * required) to supply a locator: if it does so, it must supply
     * the locator to the application by invoking this method before
     * invoking any of the other methods in the DocumentHandler
     * interface.</p>
     *
     * <p>The locator allows the application to determine the end
     * position of any document-related event, even if the parser is
     * not reporting an error.  Typically, the application will
     * use this information for reporting its own errors (such as
     * character content that does not match an application's
     * business rules).  The information returned by the locator
     * is probably not sufficient for use with a search engine.</p>
     *
     * <p>Note that the locator will return correct information only
     * during the invocation of the events in this interface.  The
     * application should not attempt to use it at any other time.</p>
     */
    public void setDocumentLocator(Locator locator) {
        m_locator = locator;
    }

    /**
     * Adds the given attribute to the set of collected attributes , but only if
     * there is a currently open element.
     *
     * An element is currently open if a startElement() notification has
     * occured but the start of the element has not yet been written to the
     * output.  In the stream case this means that we have not yet been forced
     * to close the elements opening tag by another notification, such as a
     * character notification.
     *
     * @param uri the URI of the attribute
     * @param localName the local name of the attribute
     * @param rawName    the qualified name of the attribute
     * @param type the type of the attribute (probably CDATA)
     * @param value the value of the attribute
     * @param XSLAttribute true if this attribute is coming from an xsl:attriute element
     * @see ExtendedContentHandler#addAttribute(String, String, String, String, String)
     */
    public void addAttribute(String uri, String localName, String rawName,
                             String type, String value, boolean XSLAttribute)
        throws SAXException
    {
        if (m_elemContext.m_startTagOpen) {
            addAttributeAlways(uri, localName, rawName, type, value, XSLAttribute);
        }
    }

    /**
     * Adds the given attribute to the set of attributes, even if there is
     * no currently open element. This is useful if a SAX startPrefixMapping()
     * should need to add an attribute before the element name is seen.
     *
     * @param uri the URI of the attribute
     * @param localName the local name of the attribute
     * @param rawName   the qualified name of the attribute
     * @param type the type of the attribute (probably CDATA)
     * @param value the value of the attribute
     * @param XSLAttribute true if this attribute is coming from an xsl:attribute element
     * @return true if the attribute was added,
     * false if an existing value was replaced.
     */
    public boolean addAttributeAlways(String uri, String localName, String rawName,
                                      String type, String value, boolean XSLAttribute)
    {
        boolean was_added;
        int index;

        if (localName == null || uri == null || uri.length() == 0)
            index = m_attributes.getIndex(rawName);
        else {
            index = m_attributes.getIndex(uri,localName);
        }
        if (index >= 0) {
            /* We've seen the attribute before.
             * We may have a null uri or localName, but all
             * we really want to re-set is the value anyway.
             */
            m_attributes.setValue(index,value);
            was_added = false;
        } else {
            // the attribute doesn't exist yet, create it
            m_attributes.addAttribute(uri, localName, rawName, type, value);
            was_added = true;
        }
        return was_added;
    }

    /**
     *  Adds  the given attribute to the set of collected attributes,
     * but only if there is a currently open element.
     *
     * @param name the attribute's qualified name
     * @param value the value of the attribute
     */
    public void addAttribute(String name, final String value) {
        if (m_elemContext.m_startTagOpen) {
            final String patchedName = patchName(name);
            final String localName = getLocalName(patchedName);
            final String uri = getNamespaceURI(patchedName, false);

            addAttributeAlways(uri,localName, patchedName, "CDATA", value, false);
        }
    }

    /**
     * Adds the given xsl:attribute to the set of collected attributes,
     * but only if there is a currently open element.
     *
     * @param name the attribute's qualified name (prefix:localName)
     * @param value the value of the attribute
     * @param uri the URI that the prefix of the name points to
     */
    public void addXSLAttribute(String name, final String value, final String uri) {
        if (m_elemContext.m_startTagOpen) {
            final String patchedName = patchName(name);
            final String localName = getLocalName(patchedName);

            addAttributeAlways(uri,localName, patchedName, "CDATA", value, true);
        }
    }

    /**
     * Add the given attributes to the currently collected ones. These
     * attributes are always added, regardless of whether on not an element
     * is currently open.
     * @param atts List of attributes to add to this list
     */
    public void addAttributes(Attributes atts) throws SAXException {
        int nAtts = atts.getLength();
        for (int i = 0; i < nAtts; i++) {
            String uri = atts.getURI(i);

            if (null == uri)
                uri = "";

            addAttributeAlways(
                uri,
                atts.getLocalName(i),
                atts.getQName(i),
                atts.getType(i),
                atts.getValue(i),
                false);
        }
    }

    /**
     * Return a {@link ContentHandler} interface into this serializer.
     * If the serializer does not support the {@link ContentHandler}
     * interface, it should return null.
     *
     * @return A {@link ContentHandler} interface into this serializer,
     *  or null if the serializer is not SAX 2 capable
     * @throws IOException An I/O exception occured
     */
    public ContentHandler asContentHandler() throws IOException {
        return this;
    }

    /**
     * Report the end of an entity.
     *
     * @param name The name of the entity that is ending.
     * @throws org.xml.sax.SAXException The application may raise an exception.
     * @see #startEntity
     */
    public void endEntity(String name) throws org.xml.sax.SAXException {
        if (name.equals("[dtd]"))
            m_inExternalDTD = false;

        if (!m_inExternalDTD)
            m_inEntityRef--;

        if (m_tracer != null)
            this.fireEndEntity(name);
    }

    /**
     * This method checks if current node is in entity reference.
     *
     * @return True if current node is in entity reference.
     */
    protected boolean isInEntityRef() {
        return m_inEntityRef > 0;
    }

    /**
     * Flush and close the underlying java.io.Writer. This method applies to
     * ToStream serializers, not ToSAXHandler serializers.
     * @see ToStream
     */
    public void close() {
        // do nothing (base behavior)
    }

    /**
     * Initialize global variables
     */
    protected void initCDATA() {
        // CDATA stack
        // _cdataStack = new Stack();
        // _cdataStack.push(new Integer(-1)); // push dummy value
    }

    /**
     * Returns the character encoding to be used in the output document.
     * @return the character encoding to be used in the output document.
     */
    public String getEncoding() {
        return getOutputProperty(OutputKeys.ENCODING);
    }

   /**
     * Sets the character encoding coming from the xsl:output encoding stylesheet attribute.
     * @param m_encoding the character encoding
     */
    public void setEncoding(String encoding) {
        setOutputProperty(OutputKeys.ENCODING,encoding);
    }

    /**
     * Sets the value coming from the xsl:output omit-xml-declaration stylesheet attribute
     * @param b true if the XML declaration is to be omitted from the output
     * document.
     */
    public void setOmitXMLDeclaration(boolean b) {
        String val = b ? "yes":"no";
        setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,val);
    }

    /**
     * @return true if the XML declaration is to be omitted from the output
     * document.
     */
    public boolean getOmitXMLDeclaration() {
        return m_shouldNotWriteXMLHeader;
    }

    /**
     * Returns the previously set value of the value to be used as the public
     * identifier in the document type declaration (DTD).
     *
     *@return the public identifier to be used in the DOCTYPE declaration in the
     * output document.
     */
    public String getDoctypePublic()
    {
        return m_doctypePublic;
    }

    /** Set the value coming from the xsl:output doctype-public stylesheet attribute.
      * @param doctypePublic the public identifier to be used in the DOCTYPE
      * declaration in the output document.
      */
    public void setDoctypePublic(String doctypePublic)
    {
        setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctypePublic);
    }


    /**
     * Returns the previously set value of the value to be used
     * as the system identifier in the document type declaration (DTD).
         * @return the system identifier to be used in the DOCTYPE declaration in
         * the output document.
     *
     */
    public String getDoctypeSystem()
    {
        return m_doctypeSystem;
    }

    /** Set the value coming from the xsl:output doctype-system stylesheet attribute.
      * @param doctypeSystem the system identifier to be used in the DOCTYPE
      * declaration in the output document.
      */
    public void setDoctypeSystem(String doctypeSystem)
    {
        setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctypeSystem);
    }

    /** Set the value coming from the xsl:output doctype-public and doctype-system stylesheet properties
     * @param doctypeSystem the system identifier to be used in the DOCTYPE
     * declaration in the output document.
     * @param doctypePublic the public identifier to be used in the DOCTYPE
     * declaration in the output document.
     */
    public void setDoctype(String doctypeSystem, String doctypePublic)
    {
        setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctypeSystem);
        setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctypePublic);
    }

    /**
     * Sets the value coming from the xsl:output standalone stylesheet attribute.
     * @param standalone a value of "yes" indicates that the
     * <code>standalone</code> delaration is to be included in the output
     * document. This method remembers if the value was explicitly set using
     * this method, verses if the value is the default value.
     */
    public void setStandalone(String standalone) {
        setOutputProperty(OutputKeys.STANDALONE, standalone);
    }

    /**
     * Sets the XSL standalone attribute, but does not remember if this is a
     * default or explicite setting.
     * @param standalone "yes" | "no"
     */
    protected void setStandaloneInternal(String standalone) {
        if ("yes".equals(standalone))
            m_standalone = "yes";
        else
            m_standalone = "no";

    }

    /**
     * Gets the XSL standalone attribute
     * @return a value of "yes" if the <code>standalone</code> delaration is to
     * be included in the output document.
     *  @see XSLOutputAttributes#getStandalone()
     */
    public String getStandalone() {
        return m_standalone;
    }

    /**
     * @return true if the output document should be indented to visually
     * indicate its structure.
     */
    public boolean getIndent() {
        return m_doIndent;
    }
    /**
     * Gets the mediatype the media-type or MIME type associated with the output
     * document.
     * @return the mediatype the media-type or MIME type associated with the
     * output document.
     */
    public String getMediaType() {
        return m_mediatype;
    }

    /**
     * Gets the version of the output format.
     * @return the version of the output format.
     */
    public String getVersion() {
        return m_version;
    }

    /**
     * Sets the value coming from the xsl:output version attribute.
     * @param version the version of the output format.
     * @see SerializationHandler#setVersion(String)
     */
    public void setVersion(String version) {
        setOutputProperty(OutputKeys.VERSION, version);
    }

    /**
     * Sets the value coming from the xsl:output media-type stylesheet attribute.
     * @param mediaType the non-null media-type or MIME type associated with the
     * output document.
     * @see javax.xml.transform.OutputKeys#MEDIA_TYPE
     * @see SerializationHandler#setMediaType(String)
     */
    public void setMediaType(String mediaType) {
        setOutputProperty(OutputKeys.MEDIA_TYPE,mediaType);
    }

    /**
     * @return the number of spaces to indent for each indentation level.
     */
    public int getIndentAmount() {
        return m_indentAmount;
    }

    /**
     * Sets the indentation amount.
     * @param m_indentAmount The m_indentAmount to set
     */
    public void setIndentAmount(int m_indentAmount) {
        this.m_indentAmount = m_indentAmount;
    }

    /**
     * Sets the value coming from the xsl:output indent stylesheet
     * attribute.
     * @param doIndent true if the output document should be indented to
     * visually indicate its structure.
     * @see XSLOutputAttributes#setIndent(boolean)
     */
    public void setIndent(boolean doIndent) {
        String val = doIndent ? "yes":"no";
        setOutputProperty(OutputKeys.INDENT,val);
    }

    /**
     * Sets the implementation specific property "isStandalone".
     * @param isStandalone the value of the property
     * @see JdkConstants.SP_XSLTC_IS_Standalone
     */
    public void setIsStandalone(boolean isStandalone) {
       m_isStandalone = isStandalone;
    }

    /**
     * This method is used when a prefix/uri namespace mapping
     * is indicated after the element was started with a
     * startElement() and before and endElement().
     * startPrefixMapping(prefix,uri) would be used before the
     * startElement() call.
     * @param uri the URI of the namespace
     * @param prefix the prefix associated with the given URI.
     *
     * @see ExtendedContentHandler#namespaceAfterStartElement(String, String)
     */
    public void namespaceAfterStartElement(String uri, String prefix)
        throws SAXException
    {
        // default behavior is to do nothing
    }

    /**
     * Return a {@link DOMSerializer} interface into this serializer. If the
     * serializer does not support the {@link DOMSerializer} interface, it should
     * return null.
     *
     * @return A {@link DOMSerializer} interface into this serializer,  or null
     * if the serializer is not DOM capable
     * @throws IOException An I/O exception occured
     * @see Serializer#asDOMSerializer()
     */
    public DOMSerializer asDOMSerializer() throws IOException {
        return this;
    }

    /**
     * Tell if two strings are equal, without worry if the first string is null.
     *
     * @param p String reference, which may be null.
     * @param t String reference, which may be null.
     *
     * @return true if strings are equal.
     */
    private static final boolean subPartMatch(String p, String t) {
        return (p == t) || ((null != p) && (p.equals(t)));
    }

    /**
     * Returns the local name of a qualified name.
     * If the name has no prefix,
     * then it works as the identity (SAX2).
     *
     * @param qname a qualified name
     * @return returns the prefix of the qualified name,
     * or null if there is no prefix.
     */
    protected static final String getPrefixPart(String qname) {
        final int col = qname.indexOf(':');
        return (col > 0) ? qname.substring(0, col) : null;
        //return (col > 0) ? qname.substring(0,col) : "";
    }

    /**
     * Some users of the serializer may need the current namespace mappings
     * @return the current namespace mappings (prefix/uri)
     * @see ExtendedContentHandler#getNamespaceMappings()
     */
    public NamespaceMappings getNamespaceMappings() {
        return m_prefixMap;
    }

    /**
     * Returns the prefix currently pointing to the given URI (if any).
     * @param namespaceURI the uri of the namespace in question
     * @return a prefix pointing to the given URI (if any).
     * @see ExtendedContentHandler#getPrefix(String)
     */
    public String getPrefix(String namespaceURI) {
        String prefix = m_prefixMap.lookupPrefix(namespaceURI);
        return prefix;
    }

    /**
     * Returns the URI of an element or attribute. Note that default namespaces
     * do not apply directly to attributes.
     * @param qname a qualified name
     * @param isElement true if the qualified name is the name of
     * an element.
     * @return returns the namespace URI associated with the qualified name.
     */
    public String getNamespaceURI(String qname, boolean isElement) {
        String uri = EMPTYSTRING;
        int col = qname.lastIndexOf(':');
        final String prefix = (col > 0) ? qname.substring(0, col) : EMPTYSTRING;

        if (!EMPTYSTRING.equals(prefix) || isElement) {
            if (m_prefixMap != null) {
                uri = m_prefixMap.lookupNamespace(prefix);
                if (uri == null && !prefix.equals(XMLNS_PREFIX)) {
                    throw new RuntimeException(
                        Utils.messages.createMessage(
                            MsgKey.ER_NAMESPACE_PREFIX,
                            new Object[] { qname.substring(0, col) }  ));
                }
            }
        }
        return uri;
    }

    /**
     * Returns the URI of prefix (if any)
     *
         * @param prefix the prefix whose URI is searched for
     * @return the namespace URI currently associated with the
     * prefix, null if the prefix is undefined.
     */
    public String getNamespaceURIFromPrefix(String prefix) {
        String uri = null;
        if (m_prefixMap != null)
            uri = m_prefixMap.lookupNamespace(prefix);
        return uri;
    }

    /**
     * Entity reference event.
     *
     * @param name Name of entity
     *
     * @throws org.xml.sax.SAXException
     */
    public void entityReference(String name) throws org.xml.sax.SAXException {
        flushPending();

        startEntity(name);
        endEntity(name);

        if (m_tracer != null)
            fireEntityReference(name);
    }

    /**
     * Sets the transformer associated with this serializer
     * @param t the transformer associated with this serializer.
     * @see SerializationHandler#setTransformer(Transformer)
     */
    public void setTransformer(Transformer t) {
        m_transformer = t;

        // If this transformer object implements the SerializerTrace interface
        // then assign m_tracer to the transformer object so it can be used
        // to fire trace events.
        if ((m_transformer instanceof SerializerTrace) &&
            (((SerializerTrace) m_transformer).hasTraceListeners())) {
           m_tracer = (SerializerTrace) m_transformer;
        } else {
           m_tracer = null;
        }
    }

    /**
     * Gets the transformer associated with this serializer
     * @return returns the transformer associated with this serializer.
     * @see SerializationHandler#getTransformer()
     */
    public Transformer getTransformer() {
        return m_transformer;
    }

    /**
     * This method gets the nodes value as a String and uses that String as if
     * it were an input character notification.
     * @param node the Node to serialize
     * @throws org.xml.sax.SAXException
     */
    public void characters(org.w3c.dom.Node node)
        throws org.xml.sax.SAXException
    {
        flushPending();
        String data = node.getNodeValue();
        if (data != null) {
            final int length = data.length();
            if (length > m_charsBuff.length) {
                m_charsBuff = new char[length * 2 + 1];
            }
            data.getChars(0, length, m_charsBuff, 0);
            characters(m_charsBuff, 0, length);
        }
    }


    /**
     * @see org.xml.sax.ErrorHandler#error(SAXParseException)
     */
    public void error(SAXParseException exc) throws SAXException {
    }

    /**
     * @see org.xml.sax.ErrorHandler#fatalError(SAXParseException)
     */
    public void fatalError(SAXParseException exc) throws SAXException {
        m_elemContext.m_startTagOpen = false;
    }

    /**
     * @see org.xml.sax.ErrorHandler#warning(SAXParseException)
     */
    public void warning(SAXParseException exc) throws SAXException {
    }

    /**
     * To fire off start entity trace event
     * @param name Name of entity
     */
    protected void fireStartEntity(String name)
        throws org.xml.sax.SAXException
    {
        if (m_tracer != null)
        {
            flushMyWriter();
            m_tracer.fireGenerateEvent(SerializerTrace.EVENTTYPE_ENTITYREF, name);
        }
    }

    /**
     * This method is only used internally when flushing the writer from the
     * various fire...() trace events.  Due to the writer being wrapped with
     * SerializerTraceWriter it may cause the flush of these trace events:
     * EVENTTYPE_OUTPUT_PSEUDO_CHARACTERS
     * EVENTTYPE_OUTPUT_CHARACTERS
     * which trace the output written to the output stream.
     *
     */
    private void flushMyWriter() {
        if (m_writer != null) {
            try {
                m_writer.flush();
            } catch(IOException ioe) {
            }
        }
    }

    /**
     * Report the CDATA trace event
     * @param chars  content of CDATA
     * @param start  starting index of characters to output
     * @param length  number of characters to output
     */
    protected void fireCDATAEvent(char[] chars, int start, int length)
        throws org.xml.sax.SAXException
    {
        if (m_tracer != null) {
            flushMyWriter();
            m_tracer.fireGenerateEvent(SerializerTrace.EVENTTYPE_CDATA, chars, start,length);
        }
    }

    /**
     * Report the comment trace event
     * @param chars  content of comment
     * @param start  starting index of comment to output
     * @param length  number of characters to output
     */
    protected void fireCommentEvent(char[] chars, int start, int length)
        throws org.xml.sax.SAXException
    {
        if (m_tracer != null) {
            flushMyWriter();
            m_tracer.fireGenerateEvent(SerializerTrace.EVENTTYPE_COMMENT, new String(chars, start, length));
        }
    }


    /**
     * To fire off end entity trace event
     * @param name Name of entity
     */
    public void fireEndEntity(String name)
        throws org.xml.sax.SAXException
    {
        if (m_tracer != null)
            flushMyWriter();
        // we do not need to handle this.
    }

    /**
     * To fire off start document trace  event
     */
     protected void fireStartDoc()
        throws org.xml.sax.SAXException
    {
        if (m_tracer != null)
        {
            flushMyWriter();
            m_tracer.fireGenerateEvent(SerializerTrace.EVENTTYPE_STARTDOCUMENT);
        }
    }


    /**
     * To fire off end document trace event
     */
    protected void fireEndDoc()
        throws org.xml.sax.SAXException
    {
        if (m_tracer != null)
        {
            flushMyWriter();
            m_tracer.fireGenerateEvent(SerializerTrace.EVENTTYPE_ENDDOCUMENT);
        }
    }

    /**
     * Report the start element trace event. This trace method needs to be
     * called just before the attributes are cleared.
     *
     * @param elemName the qualified name of the element
     *
     */
    protected void fireStartElem(String elemName)
        throws org.xml.sax.SAXException
    {
        if (m_tracer != null)
        {
            flushMyWriter();
            m_tracer.fireGenerateEvent(SerializerTrace.EVENTTYPE_STARTELEMENT,
                elemName, m_attributes);
        }
    }


    /**
     * To fire off the end element event
     * @param name Name of element
     */
//    protected void fireEndElem(String name)
//        throws org.xml.sax.SAXException
//    {
//        if (m_tracer != null)
//            m_tracer.fireGenerateEvent(SerializerTrace.EVENTTYPE_ENDELEMENT,name, (Attributes)null);
//    }


    /**
     * To fire off the PI trace event
     * @param name Name of PI
     */
    protected void fireEscapingEvent(String name, String data)
        throws org.xml.sax.SAXException
    {

        if (m_tracer != null)
        {
            flushMyWriter();
            m_tracer.fireGenerateEvent(SerializerTrace.EVENTTYPE_PI,name, data);
        }
    }


    /**
     * To fire off the entity reference trace event
     * @param name Name of entity reference
     */
    protected void fireEntityReference(String name)
        throws org.xml.sax.SAXException
    {
        if (m_tracer != null)
        {
            flushMyWriter();
            m_tracer.fireGenerateEvent(SerializerTrace.EVENTTYPE_ENTITYREF,name, (Attributes)null);
        }
    }

    /**
     * Receive notification of the beginning of a document.
     * This method is never a self generated call,
     * but only called externally.
     *
     * <p>The SAX parser will invoke this method only once, before any
     * other methods in this interface or in DTDHandler (except for
     * setDocumentLocator).</p>
     *
     * @throws org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     *
     * @throws org.xml.sax.SAXException
     */
    public void startDocument() throws org.xml.sax.SAXException
    {

        // if we do get called with startDocument(), handle it right away
        startDocumentInternal();
        m_needToCallStartDocument = false;
        return;
    }

    /**
     * This method handles what needs to be done at a startDocument() call,
     * whether from an external caller, or internally called in the
     * serializer.  For historical reasons the serializer is flexible to
     * startDocument() not always being called.
     * Even if no external call is
     * made into startDocument() this method will always be called as a self
     * generated internal startDocument, it handles what needs to be done at a
     * startDocument() call.
     *
     * This method exists just to make sure that startDocument() is only ever
     * called from an external caller, which in principle is just a matter of
     * style.
     *
     * @throws SAXException
     */
    protected void startDocumentInternal() throws org.xml.sax.SAXException
    {
        if (m_tracer != null)
            this.fireStartDoc();

    }

    /* This method extracts version and encoding information from SAX events.
     */
    protected void setDocumentInfo() {
        if (m_locator == null)
                return;
        try{
            String strVersion = ((Locator2)m_locator).getXMLVersion();
            if (strVersion != null)
                setVersion(strVersion);
            /*String strEncoding = ((Locator2)m_locator).getEncoding();
            if (strEncoding != null)
                setEncoding(strEncoding); */

        }catch(ClassCastException e){}
    }

    /**
     * This method is used to set the source locator, which might be used to
     * generated an error message.
     * @param locator the source locator
     *
     * @see ExtendedContentHandler#setSourceLocator(javax.xml.transform.SourceLocator)
     */
    public void setSourceLocator(SourceLocator locator) {
        m_sourceLocator = locator;
    }

    /**
     * Used only by TransformerSnapshotImpl to restore the serialization
     * to a previous state.
     *
     * @param mappings NamespaceMappings
     */
    public void setNamespaceMappings(NamespaceMappings mappings) {
        m_prefixMap = mappings;
    }

    public boolean reset() {
        resetSerializerBase();
        return true;
    }

    /**
     * Reset all of the fields owned by SerializerBase
     *
     */
    private void resetSerializerBase() {
        this.m_attributes.clear();
        this.m_StringOfCDATASections = null;
        this.m_elemContext = new ElemContext();
        this.m_doctypePublic = null;
        this.m_doctypeSystem = null;
        this.m_doIndent = false;
        this.m_indentAmount = 4;
        this.m_inEntityRef = 0;
        this.m_inExternalDTD = false;
        this.m_mediatype = null;
        this.m_needToCallStartDocument = true;
        this.m_needToOutputDocTypeDecl = false;
        if (this.m_prefixMap != null)
            this.m_prefixMap.reset();
        this.m_shouldNotWriteXMLHeader = false;
        this.m_sourceLocator = null;
        this.m_standalone = null;
        this.m_standaloneWasSpecified = false;
        this.m_tracer = null;
        this.m_transformer = null;
        this.m_version = null;
        // don't set writer to null, so that it might be re-used
        //this.m_writer = null;
    }

    /**
     * Returns true if the serializer is used for temporary output rather than
     * final output.
     *
     * This concept is made clear in the XSLT 2.0 draft.
     */
    final boolean inTemporaryOutputState() {
        /* This is a hack. We should really be letting the serializer know
         * that it is in temporary output state with an explicit call, but
         * from a pragmatic point of view (for now anyways) having no output
         * encoding at all, not even the default UTF-8 indicates that the
         * serializer is being used for temporary RTF.
         */
        return (getEncoding() == null);

    }

    /**
     * This method adds an attribute the the current element,
     * but should not be used for an xsl:attribute child.
     * @see ExtendedContentHandler#addAttribute(java.lang.String, java.lang.String,
     *          java.lang.String, java.lang.String, java.lang.String)
     */
    public void addAttribute(String uri, String localName, String rawName,
                             String type, String value) throws SAXException
    {
        if (m_elemContext.m_startTagOpen) {
            addAttributeAlways(uri, localName, rawName, type, value, false);
        }
    }

    /**
     * @see org.xml.sax.DTDHandler#notationDecl(java.lang.String,
     *          java.lang.String, java.lang.String)
     */
    public void notationDecl(String arg0, String arg1, String arg2)
        throws SAXException
    {
        // This method just provides a definition to satisfy the interface
        // A particular sub-class of SerializerBase provides the implementation
        // (if desired)
    }

    /**
     * @see org.xml.sax.DTDHandler#unparsedEntityDecl(java.lang.String,
     *          java.lang.String, java.lang.String, java.lang.String)
     */
    public void unparsedEntityDecl(String arg0, String arg1, String arg2,
                                   String arg3) throws SAXException {
        // This method just provides a definition to satisfy the interface
        // A particular sub-class of SerializerBase provides the implementation
        // (if desired)
    }

    /**
     * If set to false the serializer does not expand DTD entities,
     * but leaves them as is, the default value is true.
     */
    public void setDTDEntityExpansion(boolean expand) {
        // This method just provides a definition to satisfy the interface
        // A particular sub-class of SerializerBase provides the implementation (if desired)
    }


    /**
     * The CDATA section names stored in a whitespace separateed list with
     * each element being a word of the form "{uri}localName" This list
     * comes from the cdata-section-elements attribute.
     *
     * This field replaces m_cdataSectionElements Vector.
     */
    protected String m_StringOfCDATASections = null;

    boolean m_docIsEmpty = true;
    void initCdataElems(String s)
    {
        if (s != null)
        {
            int max = s.length();

            // true if we are in the middle of a pair of curly braces that delimit a URI
            boolean inCurly = false;

            // true if we found a URI but haven't yet processed the local name
            boolean foundURI = false;

            StringBuilder buf = new StringBuilder();
            String uri = null;
            String localName = null;

            // parse through string, breaking on whitespaces.  I do this instead
            // of a tokenizer so I can track whitespace inside of curly brackets,
            // which theoretically shouldn't happen if they contain legal URLs.
            for (int i = 0; i < max; i++)
            {
                char c = s.charAt(i);

                if (Character.isWhitespace(c))
                {
                    if (!inCurly)
                    {
                        if (buf.length() > 0)
                        {
                            localName = buf.toString();
                            if (!foundURI)
                                uri = "";
                            addCDATAElement(uri,localName);
                            buf.setLength(0);
                            foundURI = false;
                        }
                        continue;
                    }
                    else
                        buf.append(c); // add whitespace to the URI
                }
                else if ('{' == c) // starting a URI
                    inCurly = true;
                else if ('}' == c)
                {
                    // we just ended a URI
                    foundURI = true;
                    uri = buf.toString();
                    buf.setLength(0);
                    inCurly = false;
                }
                else
                {
                    // append non-whitespace, non-curly to current URI or localName being gathered.
                    buf.append(c);
                }

            }

            if (buf.length() > 0)
            {
                // We have one last localName to process.
                localName = buf.toString();
                if (!foundURI)
                    uri = "";
                addCDATAElement(uri,localName);
            }
        }
    }

    protected java.util.HashMap<String, HashMap<String, String>> m_CdataElems = null;
    private void addCDATAElement(String uri, String localName)
    {
        if (m_CdataElems == null) {
            m_CdataElems = new java.util.HashMap<>();
        }

        HashMap<String,String> h = m_CdataElems.get(localName);
        if (h == null) {
            h = new HashMap<>();
            m_CdataElems.put(localName,h);
        }
        h.put(uri,uri);

    }


    /**
     * Return true if nothing has been sent to this result tree yet.
     * <p>
     * This is not a public API.
     *
     * @xsl.usage internal
     */
    public boolean documentIsEmpty() {
        // If we haven't called startDocument() yet, then this document is empty
        return m_docIsEmpty && (m_elemContext.m_currentElemDepth == 0);
    }

    /**
     * Return true if the current element in m_elemContext
     * is a CDATA section.
     * CDATA sections are specified in the <xsl:output> attribute
     * cdata-section-names or in the JAXP equivalent property.
     * In any case the format of the value of such a property is:
     * <pre>
     * "{uri1}localName1 {uri2}localName2 . . . "
     * </pre>
     *
     * <p>
     * This method is not a public API, but is only used internally by the serializer.
     */
    protected boolean isCdataSection() {
        boolean b = false;

        if (null != m_StringOfCDATASections) {
            if (m_elemContext.m_elementLocalName == null) {
                String localName =  getLocalName(m_elemContext.m_elementName);
                m_elemContext.m_elementLocalName = localName;
            }

            if ( m_elemContext.m_elementURI == null) {

                m_elemContext.m_elementURI = getElementURI();
            }
            else if ( m_elemContext.m_elementURI.length() == 0) {
                if ( m_elemContext.m_elementName == null) {
                    m_elemContext.m_elementName = m_elemContext.m_elementLocalName;
                    // leave URI as "", meaning in no namespace
                }
                else if (m_elemContext.m_elementLocalName.length() < m_elemContext.m_elementName.length()){
                    // We were told the URI was "", yet the name has a prefix since the name is longer than the localname.
                    // So we will fix that incorrect information here.
                    m_elemContext.m_elementURI = getElementURI();
                }
            }

            HashMap<String, String> h = null;
            if (m_CdataElems != null) {
                h = m_CdataElems.get(m_elemContext.m_elementLocalName);
            }
            if (h != null) {
                Object obj = h.get(m_elemContext.m_elementURI);
                if (obj != null)
                    b = true;
            }

        }
        return b;
    }

    /**
     * Before this call m_elementContext.m_elementURI is null,
     * which means it is not yet known. After this call it
     * is non-null, but possibly "" meaning that it is in the
     * default namespace.
     *
     * @return The URI of the element, never null, but possibly "".
     */
    private String getElementURI() {
        String uri = null;
        // At this point in processing we have received all the
        // namespace mappings
        // As we still don't know the elements namespace,
        // we now figure it out.

        String prefix = getPrefixPart(m_elemContext.m_elementName);

        if (prefix == null) {
            // no prefix so lookup the URI of the default namespace
            uri = m_prefixMap.lookupNamespace("");
        } else {
            uri = m_prefixMap.lookupNamespace(prefix);
        }
        if (uri == null) {
            // We didn't find the namespace for the
            // prefix ... ouch, that shouldn't happen.
            // This is a hack, we really don't know
            // the namespace
            uri = EMPTYSTRING;
        }

        return uri;
    }


    /**
     * Get the value of an output property,
     * the explicit value, if any, otherwise the
     * default value, if any, otherwise null.
     */
    public String getOutputProperty(String name) {
        String val = getOutputPropertyNonDefault(name);
        // If no explicit value, try to get the default value
        if (val == null)
            val = getOutputPropertyDefault(name);
        return val;

    }
    /**
     * Get the value of an output property,
     * not the default value. If there is a default
     * value, but no non-default value this method
     * will return null.
     * <p>
     *
     */
    public String getOutputPropertyNonDefault(String name) {
        return getProp(name,false);
    }

    /**
     * Return a {@link DOM3Serializer} interface into this serializer. If the
     * serializer does not support the {@link DOM3Serializer} interface, it should
     * return null.
     *
     * @return A {@link DOM3Serializer} interface into this serializer,  or null
     * if the serializer is not DOM capable
     * @throws IOException An I/O exception occured
     * @see org.apache.xml.serializer.Serializer#asDOM3Serializer()
     */
    public Object asDOM3Serializer() throws IOException
    {
        return new com.sun.org.apache.xml.internal.serializer.dom3.DOM3SerializerImpl(this);
    }

    /**
     * Get the default value of an xsl:output property,
     * which would be null only if no default value exists
     * for the property.
     */
    public String getOutputPropertyDefault(String name) {
        return getProp(name, true);
    }

    /**
     * Set the value for the output property, typically from
     * an xsl:output element, but this does not change what
     * the default value is.
     */
    public void setOutputProperty(String name, String val) {
        setProp(name,val,false);
    }

    /**
     * Set the default value for an output property, but this does
     * not impact any explicitly set value.
     */
    public void setOutputPropertyDefault(String name, String val) {
        setProp(name,val,true);

    }

    /**
     * A mapping of keys to explicitly set values, for example if
     * and <xsl:output/> has an "encoding" attribute, this
     * map will have what that attribute maps to.
     */
    private HashMap<String, String> m_OutputProps;

    /**
     * A mapping of keys to default values, for example if
     * the default value of the encoding is "UTF-8" then this
     * map will have that "encoding" maps to "UTF-8".
     */
    private HashMap<String, String> m_OutputPropsDefault;

    Set<String> getOutputPropDefaultKeys() {
        return m_OutputPropsDefault.keySet();
    }

    Set<String> getOutputPropKeys() {
        return m_OutputProps.keySet();
    }

    private String getProp(String name, boolean defaultVal) {
        if (m_OutputProps == null) {
            m_OutputProps = new HashMap<>();
            m_OutputPropsDefault = new HashMap<>();
        }

        String val;
        if (defaultVal)
            val = m_OutputPropsDefault.get(name);
        else
            val = m_OutputProps.get(name);

        return val;
    }

    /**
     *
     * @param name The name of the property, e.g. "{http://myprop}indent-tabs" or "indent".
     * @param val The value of the property, e.g. "4"
     * @param defaultVal true if this is a default value being set for the property as
     * opposed to a user define on, set say explicitly in the stylesheet or via JAXP
     */
    void setProp(String name, String val, boolean defaultVal) {
        if (m_OutputProps == null) {
            m_OutputProps = new HashMap<>();
            m_OutputPropsDefault = new HashMap<>();
        }

        if (defaultVal)
            m_OutputPropsDefault.put(name,val);
        else {
            if (OutputKeys.CDATA_SECTION_ELEMENTS.equals(name) && val != null) {
                initCdataElems(val);
                String oldVal = m_OutputProps.get(name);
                String newVal;
                if (oldVal == null)
                    newVal = oldVal + ' ' + val;
                else
                    newVal = val;
                m_OutputProps.put(name,newVal);
            }
            else {
                m_OutputProps.put(name,val);
            }
        }
    }

    /**
     * Get the first char of the local name
     * @param name Either a local name, or a local name
     * preceeded by a uri enclosed in curly braces.
     */
    static char getFirstCharLocName(String name) {
        final char first;
        int i = name.indexOf('}');
        if (i < 0)
            first = name.charAt(0);
        else
            first = name.charAt(i+1);
        return first;
    }
}
