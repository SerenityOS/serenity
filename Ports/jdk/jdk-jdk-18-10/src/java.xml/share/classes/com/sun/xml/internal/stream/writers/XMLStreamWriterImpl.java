/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.xml.internal.stream.writers;

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.PropertyManager;
import com.sun.org.apache.xerces.internal.util.NamespaceSupport;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.xml.internal.stream.util.ReadOnlyIterator;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamResult;
import jdk.xml.internal.SecuritySupport;

/**
 * This class implements a StAX XMLStreamWriter. It extends
 * <code>AbstractMap</code> in order to support a getter for
 * implementation-specific properties. For example, you can get
 * the underlying <code>OutputStream</code> by casting an instance
 * of this class to <code>Map</code> and calling
 * <code>getProperty(OUTPUTSTREAM_PROPERTY)</code>.
 *
 * @author Neeraj Bajaj
 * @author K.Venugopal
 * @author Santiago Pericas-Geertsen
 * @author Sunitha Reddy
 */
public final class XMLStreamWriterImpl extends AbstractMap<Object, Object>
        implements XMLStreamWriterBase {

    public static final String START_COMMENT = "<!--";
    public static final String END_COMMENT = "-->";
    public static final String DEFAULT_ENCODING = " encoding=\"utf-8\"";
    public static final String DEFAULT_XMLDECL = "<?xml version=\"1.0\" ?>";
    public static final String DEFAULT_XML_VERSION = "1.0";
    public static final char CLOSE_START_TAG = '>';
    public static final char OPEN_START_TAG = '<';
    public static final String OPEN_END_TAG = "</";
    public static final char CLOSE_END_TAG = '>';
    public static final String START_CDATA = "<![CDATA[";
    public static final String END_CDATA = "]]>";
    public static final String CLOSE_EMPTY_ELEMENT = "/>";
    public static final String SPACE = " ";
    public static final String UTF_8 = "UTF-8";

    public static final String OUTPUTSTREAM_PROPERTY = "sjsxp-outputstream";

    /**
     * This flag can be used to turn escaping off for content. It does
     * not apply to attribute content.
     */
    boolean fEscapeCharacters = true;

    /**
     * Flag for the value of repairNamespace property
     */
    private boolean fIsRepairingNamespace = false;

    /**
     * Underlying Writer to which characters are written.
     */
    private Writer fWriter;

    /**
     * Underlying OutputStream to which <code>fWriter</code>
     * writes to. May be null if unknown.
     */
    private OutputStream fOutputStream = null;

    /**
     * Collects attributes when the writer is in reparing mode.
     */
    private List<Attribute> fAttributeCache;

    /**
     * Collects namespace declarations when the writer is in reparing mode.
     */
    private List<QName> fNamespaceDecls;

    /**
     * Namespace context encapsulating user specified context
     * and context built by the writer
     */
    private NamespaceContextImpl fNamespaceContext = null;

    private NamespaceSupport fInternalNamespaceContext = null;

    private Random fPrefixGen = null;

    /**
     * Reference to PropertyManager
     */
    private PropertyManager fPropertyManager = null;

    /**
     * Flag to track if start tag is opened
     */
    private boolean fStartTagOpened = false;

    /**
     * Boolean flag  to indicate, if instance can be reused
     */
    private boolean fReuse;

    private SymbolTable fSymbolTable = new SymbolTable();

    private ElementStack fElementStack = new ElementStack(); //Change this .-Venu

    final private String DEFAULT_PREFIX = fSymbolTable.addSymbol("");

    private final ReadOnlyIterator<String> fReadOnlyIterator = new ReadOnlyIterator<>();

    /**
     * In some cases, this charset encoder is used to determine if a char is
     * encodable by underlying writer. For example, an 8-bit char from the
     * extended ASCII set is not encodable by 7-bit ASCII encoder. Unencodable
     * chars are escaped using XML numeric entities.
     */
    private CharsetEncoder fEncoder = null;

     /**
     * This is used to hold the namespace for attributes which happen to have
     * the same uri as the default namespace; It's added to avoid changing the
     * current impl. which has many redundant code for the repair mode
     */
    Map<String, String> fAttrNamespace = null;

    /**
     * Creates a new instance of XMLStreamWriterImpl. Uses platform's default
     * encoding.
     *
     * @param outputStream Underlying stream to write the bytes to
     * @param props        Properties used by this writer
     */
    public XMLStreamWriterImpl(OutputStream outputStream, PropertyManager props)
        throws IOException {

        // cannot call this(outputStream, null, props); for constructor,
        // OutputStreamWriter charsetName cannot be null

        // use default encoding
        this(new OutputStreamWriter(outputStream), props);
    }

    /**
     * Creates a new instance of XMLStreamWriterImpl.
     *
     * @param outputStream Underlying stream to write the bytes
     * @param encoding     Encoding used to convert chars into bytes
     * @param props        Properties used by this writer
     */
    public XMLStreamWriterImpl(OutputStream outputStream, String encoding,
        PropertyManager props) throws java.io.IOException {
        this(new StreamResult(outputStream), encoding, props);
    }

    /**
     * Creates a new instance of XMLStreamWriterImpl using a Writer.
     *
     * @param writer  Underlying writer to which chars are written
     * @param props   Properties used by this writer
     */
    public XMLStreamWriterImpl(Writer writer, PropertyManager props)
        throws java.io.IOException {
        this(new StreamResult(writer), null, props);
    }

    /**
     * Creates a new instance of XMLStreamWriterImpl using a StreamResult.
     * A StreamResult encasupates an OutputStream, a Writer or a SystemId.
     *
     * @param writer  Underlying writer to which chars are written
     * @param props   Properties used by this writer
     */
    public XMLStreamWriterImpl(StreamResult sr, String encoding,
        PropertyManager props) throws java.io.IOException {
        setOutput(sr, encoding);
        fPropertyManager = props;
        init();
    }

    /**
     * Initialize an instance of this XMLStreamWriter. Allocate new instances
     * for all the data structures. Set internal flags based on property values.
     */
    private void init() {
        fReuse = false;
        fNamespaceDecls = new ArrayList<>();
        fPrefixGen = new Random();
        fAttributeCache = new ArrayList<>();
        fInternalNamespaceContext = new NamespaceSupport();
        fInternalNamespaceContext.reset();
        fNamespaceContext = new NamespaceContextImpl();
        fNamespaceContext.internalContext = fInternalNamespaceContext;

        // Set internal state based on property values
        Boolean ob = (Boolean) fPropertyManager.getProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES);
        fIsRepairingNamespace = ob;
        ob = (Boolean) fPropertyManager.getProperty(Constants.ESCAPE_CHARACTERS);
        setEscapeCharacters(ob);
    }

    /**
     * Reset this instance so that it can be re-used. Do not read properties
     * again. The method <code>setOutput(StreamResult, encoding)</code> must
     * be called after this one.
     */
    public void reset() {
        reset(false);
    }

    /**
     * Reset this instance so that it can be re-used. Clears but does not
     * re-allocate internal data structures.
     *
     * @param resetProperties Indicates if properties should be read again
     */
    void reset(boolean resetProperties) {
        if (!fReuse) {
            throw new java.lang.IllegalStateException(
                "close() Must be called before calling reset()");
        }

        fReuse = false;
        fNamespaceDecls.clear();
        fAttributeCache.clear();

        // reset Element/NamespaceContext stacks
        fElementStack.clear();
        fInternalNamespaceContext.reset();

        fStartTagOpened = false;
        fNamespaceContext.userContext = null;

        if (resetProperties) {
            Boolean ob = (Boolean) fPropertyManager.getProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES);
            fIsRepairingNamespace = ob;
            ob = (Boolean) fPropertyManager.getProperty(Constants.ESCAPE_CHARACTERS);
            setEscapeCharacters(ob);
        }
    }

    /**
     * Use a StreamResult to initialize the output for this XMLStreamWriter. Check
     * for OutputStream, Writer and then systemId, in that order.
     *
     * @param sr        StreamResult encapsulating output information
     * @param encoding  Encoding to be used except when a Writer is available
     */
    public void setOutput(StreamResult sr, String encoding)
        throws IOException {

        if (sr.getOutputStream() != null) {
            setOutputUsingStream(sr.getOutputStream(), encoding);
        }
        else if (sr.getWriter() != null) {
            setOutputUsingWriter(sr.getWriter());
        }
        else if (sr.getSystemId() != null) {
            setOutputUsingStream(new FileOutputStream(sr.getSystemId()),
                encoding);
        }
    }

     private void setOutputUsingWriter(Writer writer)
        throws IOException
     {
         fWriter = writer;

         if (writer instanceof OutputStreamWriter) {
             String charset = ((OutputStreamWriter) writer).getEncoding();
             if (charset != null && !charset.equalsIgnoreCase("utf-8")) {
                 fEncoder = Charset.forName(charset).newEncoder();
             }
         }
     }

    /**
     * Utility method to create a writer when passed an OutputStream. Make
     * sure to wrap an <code>OutputStreamWriter</code> using an
     * <code>XMLWriter</code> for performance reasons.
     *
     * @param os        Underlying OutputStream
     * @param encoding  Encoding used to convert chars into bytes
     */
    private void setOutputUsingStream(OutputStream os, String encoding)
        throws IOException {
        fOutputStream = os;

        if (encoding != null) {
            if (encoding.equalsIgnoreCase("utf-8")) {
                fWriter = new UTF8OutputStreamWriter(os);
            }
            else {
                fWriter = new XMLWriter(new OutputStreamWriter(os, encoding));
                fEncoder = Charset.forName(encoding).newEncoder();
            }
        } else {
            encoding = SecuritySupport.getSystemProperty("file.encoding");
            if (encoding != null && encoding.equalsIgnoreCase("utf-8")) {
                fWriter = new UTF8OutputStreamWriter(os);
            } else {
                fWriter = new XMLWriter(new OutputStreamWriter(os));
            }
        }
    }

    /** Can this instance be reused
     *
     * @return boolean boolean value to indicate if this instance can be reused or not
     */
    public boolean canReuse() {
        return fReuse;
    }

    public void setEscapeCharacters(boolean escape) {
        fEscapeCharacters = escape;
    }

    public boolean getEscapeCharacters() {
        return fEscapeCharacters;
    }

    /**
     * Close this XMLStreamWriter by closing underlying writer.
     */
    @Override
    public void close() throws XMLStreamException {
        if (fWriter != null) {
            try {
                //fWriter.close();
                fWriter.flush();
            } catch (IOException e) {
                throw new XMLStreamException(e);
            }
        }
        fWriter = null;
        fOutputStream = null;
        fNamespaceDecls.clear();
        fAttributeCache.clear();
        fElementStack.clear();
        fInternalNamespaceContext.reset();
        fReuse = true;
        fStartTagOpened = false;
        fNamespaceContext.userContext = null;
    }

    /**
     * Flush this XMLStreamWriter by flushin underlying writer.
     */
    @Override
    public void flush() throws XMLStreamException {
        try {
            fWriter.flush();
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    /**
     * Return <code>NamespaceContext</code> being used by the writer.
     *
     * @return NamespaceContext
     */
    @Override
    public NamespaceContext getNamespaceContext() {
        return fNamespaceContext;
    }

    /**
     * Return a prefix associated with specified uri, or null if the
     * uri is unknown.
     *
     * @param  uri The namespace uri
     * @throws XMLStreamException if uri specified is "" or null
     */
    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return fNamespaceContext.getPrefix(uri);
    }

    /**
     * Returns value associated with the specified property name.
     *
     * @param  str Property name
     * @throws IllegalArgumentException if the specified property is not supported
     * @return value associated with the specified property.
     */
    @Override
    public Object getProperty(String str)
        throws IllegalArgumentException {
        if (str == null) {
            throw new NullPointerException();
        }

        if (!fPropertyManager.containsProperty(str)) {
            throw new IllegalArgumentException("Property '" + str +
                "' is not supported");
        }

        return fPropertyManager.getProperty(str);
    }

    /**
     * Set the specified URI as default namespace in the current namespace context.
     *
     * @param uri Namespace URI
     */
    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        if (uri != null) {
            uri = fSymbolTable.addSymbol(uri);
        }

        if (fIsRepairingNamespace) {
            if (isDefaultNamespace(uri)) {
                return;
            }

            QName qname = new QName();
            qname.setValues(DEFAULT_PREFIX, "xmlns", null, uri);
            fNamespaceDecls.add(qname);
        } else {
            fInternalNamespaceContext.declarePrefix(DEFAULT_PREFIX, uri);
        }
    }

    /**
     * Sets the current <code>NamespaceContext</code> for prefix and uri bindings.
     * This context becomes the root namespace context for writing and
     * will replace the current root namespace context. Subsequent calls
     * to setPrefix and setDefaultNamespace will bind namespaces using
     * the context passed to the method as the root context for resolving
     * namespaces. This method may only be called once at the start of the
     * document. It does not cause the namespaces to be declared. If a
     * namespace URI to prefix mapping is found in the namespace context
     * it is treated as declared and the prefix may be used by the
     * <code>XMLStreamWriter</code>.
     *
     * @param namespaceContext the namespace context to use for this writer, may not be null
     * @throws XMLStreamException
     */
    @Override
    public void setNamespaceContext(NamespaceContext namespaceContext)
        throws XMLStreamException {
        fNamespaceContext.userContext = namespaceContext;
    }

    /**
     * Sets the prefix the uri is bound to. This prefix is bound in the scope of
     * the current START_ELEMENT / END_ELEMENT pair. If this method is called before
     * a START_ELEMENT has been written the prefix is bound in the root scope.
     *
     * @param prefix
     * @param uri
     * @throws XMLStreamException
     */
    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {

        if (prefix == null) {
            throw new XMLStreamException("Prefix cannot be null");
        }

        if (uri == null) {
            throw new XMLStreamException("URI cannot be null");
        }

        prefix = fSymbolTable.addSymbol(prefix);
        uri = fSymbolTable.addSymbol(uri);

        if (fIsRepairingNamespace) {
            String tmpURI = fInternalNamespaceContext.getURI(prefix);

            if ((tmpURI != null) && (tmpURI == uri)) {
                return;
            }

            if(checkUserNamespaceContext(prefix,uri))
                return;
            QName qname = new QName();
            qname.setValues(prefix,XMLConstants.XMLNS_ATTRIBUTE, null,uri);
            fNamespaceDecls.add(qname);

            return;
        }

        fInternalNamespaceContext.declarePrefix(prefix, uri);
    }

    @Override
    public void writeAttribute(String localName, String value)
        throws XMLStreamException {
        try {
            if (!fStartTagOpened) {
                throw new XMLStreamException(
                    "Attribute not associated with any element");
            }

            if (fIsRepairingNamespace) {
                Attribute attr = new Attribute(value); // Revisit:Dont create new one's. Reuse.-Venu
                attr.setValues(null, localName, null, null);
                fAttributeCache.add(attr);

                return;
            }

            fWriter.write(" ");
            fWriter.write(localName);
            fWriter.write("=\"");
            writeXMLContent(
                    value,
                    true,   // true = escapeChars
                    true);  // true = escapeDoubleQuotes
            fWriter.write("\"");
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    public void writeAttribute(String namespaceURI, String localName,
        String value) throws XMLStreamException {
        try {
            if (!fStartTagOpened) {
                throw new XMLStreamException(
                    "Attribute not associated with any element");
            }

            if (namespaceURI == null) {
                throw new XMLStreamException("NamespaceURI cannot be null");
            }

            namespaceURI = fSymbolTable.addSymbol(namespaceURI);

            String prefix = fInternalNamespaceContext.getPrefix(namespaceURI);

            if (!fIsRepairingNamespace) {
                if (prefix == null) {
                    throw new XMLStreamException("Prefix cannot be null");
                }

                writeAttributeWithPrefix(prefix, localName, value);
            } else {
                Attribute attr = new Attribute(value);
                attr.setValues(null, localName, null, namespaceURI);
                fAttributeCache.add(attr);
            }
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    private void writeAttributeWithPrefix(String prefix, String localName,
        String value) throws IOException {
        fWriter.write(SPACE);

        if ((prefix != null) && (!prefix.equals(XMLConstants.DEFAULT_NS_PREFIX))) {
            fWriter.write(prefix);
            fWriter.write(":");
        }

        fWriter.write(localName);
        fWriter.write("=\"");
        writeXMLContent(value,
                true,   // true = escapeChars
                true);  // true = escapeDoubleQuotes
        fWriter.write("\"");
    }

    @Override
    public void writeAttribute(String prefix, String namespaceURI,
        String localName, String value) throws XMLStreamException {
        try {
            if (!fStartTagOpened) {
                throw new XMLStreamException(
                    "Attribute not associated with any element");
            }

            if (namespaceURI == null) {
                throw new XMLStreamException("NamespaceURI cannot be null");
            }

            if (localName == null) {
                throw new XMLStreamException("Local name cannot be null");
            }

            if (!fIsRepairingNamespace) {
                if (prefix == null || prefix.isEmpty()){
                    if (!namespaceURI.isEmpty()) {
                        throw new XMLStreamException("prefix cannot be null or empty");
                    } else {
                        writeAttributeWithPrefix(null, localName, value);
                        return;
                    }
                }

                if (!prefix.equals(XMLConstants.XML_NS_PREFIX) ||
                        !namespaceURI.equals(XMLConstants.XML_NS_URI)) {

                    prefix = fSymbolTable.addSymbol(prefix);
                    namespaceURI = fSymbolTable.addSymbol(namespaceURI);

                    if (fInternalNamespaceContext.containsPrefixInCurrentContext(prefix)){

                        String tmpURI = fInternalNamespaceContext.getURI(prefix);

                        if (tmpURI != null && tmpURI != namespaceURI){
                            throw new XMLStreamException("Prefix "+prefix+" is " +
                                    "already bound to "+tmpURI+
                                    ". Trying to rebind it to "+namespaceURI+" is an error.");
                        }
                    }
                    fInternalNamespaceContext.declarePrefix(prefix, namespaceURI);
                }
                writeAttributeWithPrefix(prefix, localName, value);
            } else {
                if (prefix != null) {
                    prefix = fSymbolTable.addSymbol(prefix);
                }

                namespaceURI = fSymbolTable.addSymbol(namespaceURI);

                Attribute attr = new Attribute(value);
                attr.setValues(prefix, localName, null, namespaceURI);
                fAttributeCache.add(attr);
            }
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    public void writeCData(String cdata) throws XMLStreamException {
        try {
            if (cdata == null) {
                throw new XMLStreamException("cdata cannot be null");
            }

            if (fStartTagOpened) {
                closeStartTag();
            }

            fWriter.write(START_CDATA);
            fWriter.write(cdata);
            fWriter.write(END_CDATA);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    public void writeCharacters(String data) throws XMLStreamException {
        try {
            if (fStartTagOpened) {
                closeStartTag();
            }

            writeXMLContent(data);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    public void writeCharacters(char[] data, int start, int len)
        throws XMLStreamException {
        try {
            if (fStartTagOpened) {
                closeStartTag();
            }

            writeXMLContent(data, start, len, fEscapeCharacters);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    public void writeComment(String comment) throws XMLStreamException {
        try {
            if (fStartTagOpened) {
                closeStartTag();
            }

            fWriter.write(START_COMMENT);

            if (comment != null) {
                fWriter.write(comment);
            }

            fWriter.write(END_COMMENT);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    public void writeDTD(String dtd) throws XMLStreamException {
        try {
            if (fStartTagOpened) {
                closeStartTag();
            }

            fWriter.write(dtd);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    /*
     * Write default Namespace.
     *
     * If namespaceURI == null,
     * then it is assumed to be equivilent to {@link XMLConstants.NULL_NS_URI},
     * i.e. there is no Namespace.
     *
     * @param namespaceURI NamespaceURI to declare.
     *
     * @throws XMLStreamException
     *
     * @see <a href="http://www.w3.org/TR/REC-xml-names/#defaulting">
     *   Namespaces in XML, 5.2 Namespace Defaulting</a>
     */
    @Override
    public void writeDefaultNamespace(String namespaceURI)
        throws XMLStreamException {

        // normalize namespaceURI
        String namespaceURINormalized;
        if (namespaceURI == null) {
            namespaceURINormalized = ""; // XMLConstants.NULL_NS_URI
        } else {
            namespaceURINormalized = namespaceURI;
        }

        try {
            if (!fStartTagOpened) {
                throw new IllegalStateException(
                    "Namespace Attribute not associated with any element");
            }

            if (fIsRepairingNamespace) {
                QName qname = new QName();
                qname.setValues(XMLConstants.DEFAULT_NS_PREFIX,
                    XMLConstants.XMLNS_ATTRIBUTE, null, namespaceURINormalized);
                fNamespaceDecls.add(qname);

                return;
            }

            namespaceURINormalized = fSymbolTable.addSymbol(namespaceURINormalized);

            if (fInternalNamespaceContext.containsPrefixInCurrentContext("")){

                String tmp = fInternalNamespaceContext.getURI("");

                if (tmp != null && !tmp.equals(namespaceURINormalized)) {
                        throw new XMLStreamException(
                                "xmlns has been already bound to " +tmp +
                                ". Rebinding it to "+ namespaceURINormalized +
                                " is an error");
                    }
            }
            fInternalNamespaceContext.declarePrefix("", namespaceURINormalized);

            // use common namespace code with a prefix == null for xmlns="..."
            writenamespace(null, namespaceURINormalized);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        try {
            if (fStartTagOpened) {
                closeStartTag();
            }

            openStartTag();
            fElementStack.push(null, localName, null, null, true);
            fInternalNamespaceContext.pushContext();

            if (!fIsRepairingNamespace) {
                fWriter.write(localName);
            }
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName)
        throws XMLStreamException {
        if (namespaceURI == null) {
            throw new XMLStreamException("NamespaceURI cannot be null");
        }

        namespaceURI = fSymbolTable.addSymbol(namespaceURI);

        String prefix = fNamespaceContext.getPrefix(namespaceURI);
        writeEmptyElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement(String prefix, String localName,
        String namespaceURI) throws XMLStreamException {
        try {
            if (localName == null) {
                throw new XMLStreamException("Local Name cannot be null");
            }

            if (namespaceURI == null) {
                throw new XMLStreamException("NamespaceURI cannot be null");
            }

            if (prefix != null) {
                prefix = fSymbolTable.addSymbol(prefix);
            }

            namespaceURI = fSymbolTable.addSymbol(namespaceURI);

            if (fStartTagOpened) {
                closeStartTag();
            }

            openStartTag();

            fElementStack.push(prefix, localName, null, namespaceURI, true);
            fInternalNamespaceContext.pushContext();

            if (!fIsRepairingNamespace) {
                if (prefix == null) {
                    throw new XMLStreamException("NamespaceURI " +
                        namespaceURI + " has not been bound to any prefix");
                }
            } else {
                return;
            }

            if ((prefix != null) && (!prefix.equals(XMLConstants.DEFAULT_NS_PREFIX))) {
                fWriter.write(prefix);
                fWriter.write(":");
            }

            fWriter.write(localName);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        try {
            if (fStartTagOpened) {
                closeStartTag();
            }

            while (!fElementStack.empty()) {
                ElementState elem = fElementStack.pop();
                fInternalNamespaceContext.popContext();

                if (elem.isEmpty) {
                    //fWriter.write(CLOSE_EMPTY_ELEMENT);
                } else {
                    fWriter.write(OPEN_END_TAG);

                    if ((elem.prefix != null) && !(elem.prefix).isEmpty()) {
                        fWriter.write(elem.prefix);
                        fWriter.write(":");
                    }

                    fWriter.write(elem.localpart);
                    fWriter.write(CLOSE_END_TAG);
                }
            }
        } catch (IOException e) {
            throw new XMLStreamException(e);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new XMLStreamException("No more elements to write");
        }
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        try {
            if (fStartTagOpened) {
                closeStartTag();
            }

            ElementState currentElement = fElementStack.pop();

            if (currentElement == null) {
                throw new XMLStreamException("No element was found to write");
            }

            if (currentElement.isEmpty) {
                //fWriter.write(CLOSE_EMPTY_ELEMENT);
                return;
            }

            fWriter.write(OPEN_END_TAG);

            if ((currentElement.prefix != null) &&
                    !(currentElement.prefix).isEmpty()) {
                fWriter.write(currentElement.prefix);
                fWriter.write(":");
            }

            fWriter.write(currentElement.localpart);
            fWriter.write(CLOSE_END_TAG);
            fInternalNamespaceContext.popContext();
        } catch (IOException e) {
            throw new XMLStreamException(e);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new XMLStreamException(
                    "No element was found to write: "
                    + e.toString(), e);
        }
    }

    @Override
    public void writeEntityRef(String refName) throws XMLStreamException {
        try {
            if (fStartTagOpened) {
                closeStartTag();
            }

            fWriter.write('&');
            fWriter.write(refName);
            fWriter.write(';');
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    /**
     * Write a Namespace declaration.
     *
     * If namespaceURI == null,
     * then it is assumed to be equivilent to {@link XMLConstants.NULL_NS_URI},
     * i.e. there is no Namespace.
     *
     * @param prefix Prefix to bind.
     * @param namespaceURI NamespaceURI to declare.
     *
     * @throws XMLStreamException
     *
     * @see <a href="http://www.w3.org/TR/REC-xml-names/#defaulting">
     *   Namespaces in XML, 5.2 Namespace Defaulting</a>
     */
    @Override
    public void writeNamespace(String prefix, String namespaceURI)
        throws XMLStreamException {

        // normalize namespaceURI
        String namespaceURINormalized;
        if (namespaceURI == null) {
            namespaceURINormalized = ""; // XMLConstants.NULL_NS_URI
        } else {
            namespaceURINormalized = namespaceURI;
        }

        try {
            QName qname;

            if (!fStartTagOpened) {
                throw new IllegalStateException(
                        "Invalid state: start tag is not opened at writeNamespace("
                        + prefix
                        + ", "
                        + namespaceURINormalized
                        + ")");
            }

            // is this the default Namespace?
            if (prefix == null
                    || prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)
                    || prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
                writeDefaultNamespace(namespaceURINormalized);
                return;
            }

            if (prefix.equals(XMLConstants.XML_NS_PREFIX) && namespaceURINormalized.equals(XMLConstants.XML_NS_URI))
                return;

            prefix = fSymbolTable.addSymbol(prefix);
            namespaceURINormalized = fSymbolTable.addSymbol(namespaceURINormalized);

            if (fIsRepairingNamespace) {
                String tmpURI = fInternalNamespaceContext.getURI(prefix);

                if ((tmpURI != null) && (tmpURI.equals(namespaceURINormalized))) {
                    return;
                }

                qname = new QName();
                qname.setValues(prefix, XMLConstants.XMLNS_ATTRIBUTE, null,
                    namespaceURINormalized);
                fNamespaceDecls.add(qname);

                return;
            }


            if (fInternalNamespaceContext.containsPrefixInCurrentContext(prefix)){

                String tmp = fInternalNamespaceContext.getURI(prefix);

                if (tmp != null && !tmp.equals(namespaceURINormalized)) {

                       throw new XMLStreamException("prefix "+prefix+
                            " has been already bound to " +tmp +
                            ". Rebinding it to "+ namespaceURINormalized+
                            " is an error");
                }
            }

            fInternalNamespaceContext.declarePrefix(prefix, namespaceURINormalized);
            writenamespace(prefix, namespaceURINormalized);

        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    private void writenamespace(String prefix, String namespaceURI)
        throws IOException {
        fWriter.write(" xmlns");

        if ((prefix != null) && (!prefix.equals(XMLConstants.DEFAULT_NS_PREFIX))) {
            fWriter.write(":");
            fWriter.write(prefix);
        }

        fWriter.write("=\"");
        writeXMLContent(
                namespaceURI,
                true,   // true = escapeChars
                true);  // true = escapeDoubleQuotes
        fWriter.write("\"");
    }

    @Override
    public void writeProcessingInstruction(String target)
        throws XMLStreamException {
        try {
            if (fStartTagOpened) {
                closeStartTag();
            }

            if (target != null) {
                fWriter.write("<?");
                fWriter.write(target);
                fWriter.write("?>");

                return;
            }
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }

        throw new XMLStreamException("PI target cannot be null");
    }

    /**
     * @param target
     * @param data
     * @throws XMLStreamException
     */
    @Override
    public void writeProcessingInstruction(String target, String data)
        throws XMLStreamException {
        try {
            if (fStartTagOpened) {
                closeStartTag();
            }

            if ((target == null) || (data == null)) {
                throw new XMLStreamException("PI target cannot be null");
            }

            fWriter.write("<?");
            fWriter.write(target);
            fWriter.write(SPACE);
            fWriter.write(data);
            fWriter.write("?>");
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }

    /**
     * Writes the XML declaration.
     *
     * @throws XMLStreamException in case of an IOException
     */
    @Override
    public void writeStartDocument() throws XMLStreamException {
        writeStartDocument(null, null, false, false);
    }

    /**
     * Writes the XML declaration.
     *
     * @param version the specified version
     * @throws XMLStreamException in case of an IOException
     */
    @Override
    public void writeStartDocument(String version) throws XMLStreamException {
        writeStartDocument(null, version, false, false);
    }

    /**
     * Writes the XML declaration.
     *
     * @param encoding the specified encoding
     * @param version the specified version
     * @throws XMLStreamException in case of an IOException
     */
    @Override
    public void writeStartDocument(String encoding, String version)
        throws XMLStreamException {
        writeStartDocument(encoding, version, false, false);
    }

    public void writeStartDocument(String encoding, String version,
            boolean standalone, boolean standaloneSet)
        throws XMLStreamException {

        try {
            if ((encoding == null || encoding.length() == 0)
                    && (version == null || version.length() == 0)
                    && (!standaloneSet)) {
                fWriter.write(DEFAULT_XMLDECL);
                return;
            }

            // Verify the encoding before writing anything
            if (encoding != null && !encoding.isEmpty()) {
                verifyEncoding(encoding);
            }

            fWriter.write("<?xml version=\"");

            if ((version == null) || version.isEmpty()) {
                fWriter.write(DEFAULT_XML_VERSION);
            } else {
                fWriter.write(version);
            }

            if (encoding != null && !encoding.isEmpty()) {
                fWriter.write("\" encoding=\"");
                fWriter.write(encoding);
            }

            if (standaloneSet) {
                fWriter.write("\" standalone=\"");
                if (standalone) {
                    fWriter.write("yes");
                } else {
                    fWriter.write("no");
                }
            }

            fWriter.write("\"?>");
        } catch (IOException ex) {
            throw new XMLStreamException(ex);
        }
    }

    /**
     * Verifies that the encoding is consistent between the underlying encoding
     * and that specified.
     *
     * @param encoding the specified encoding
     * @throws XMLStreamException if they do not match
     */
    private void verifyEncoding(String encoding) throws XMLStreamException {

        String streamEncoding = null;
        if (fWriter instanceof OutputStreamWriter) {
            streamEncoding = ((OutputStreamWriter) fWriter).getEncoding();
        }
        else if (fWriter instanceof UTF8OutputStreamWriter) {
            streamEncoding = ((UTF8OutputStreamWriter) fWriter).getEncoding();
        }
        else if (fWriter instanceof XMLWriter) {
            streamEncoding = ((OutputStreamWriter) ((XMLWriter)fWriter).getWriter()).getEncoding();
        }

        if (streamEncoding != null && !streamEncoding.equalsIgnoreCase(encoding)) {
            // If the equality check failed, check for charset encoding aliases
            boolean foundAlias = false;
            Set<String> aliases = Charset.forName(encoding).aliases();
            for (Iterator<String> it = aliases.iterator(); !foundAlias && it.hasNext(); ) {
                if (streamEncoding.equalsIgnoreCase(it.next())) {
                    foundAlias = true;
                }
            }
            // If no alias matches the encoding name, then report error
            if (!foundAlias) {
                throw new XMLStreamException("Underlying stream encoding '"
                        + streamEncoding
                        + "' and input paramter for writeStartDocument() method '"
                        + encoding + "' do not match.");
            }
        }
    }

    /**
     * @param localName
     * @throws XMLStreamException
     */
    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        try {
            if (localName == null) {
                throw new XMLStreamException("Local Name cannot be null");
            }

            if (fStartTagOpened) {
                closeStartTag();
            }

            openStartTag();
            fElementStack.push(null, localName, null, null, false);
            fInternalNamespaceContext.pushContext();

            if (fIsRepairingNamespace) {
                return;
            }

            fWriter.write(localName);
        } catch (IOException ex) {
            throw new XMLStreamException(ex);
        }
    }

    /**
     * @param namespaceURI
     * @param localName
     * @throws XMLStreamException
     */
    @Override
    public void writeStartElement(String namespaceURI, String localName)
        throws XMLStreamException {
        if (localName == null) {
            throw new XMLStreamException("Local Name cannot be null");
        }

        if (namespaceURI == null) {
            throw new XMLStreamException("NamespaceURI cannot be null");
        }

        namespaceURI = fSymbolTable.addSymbol(namespaceURI);

        String prefix = null;

        if (!fIsRepairingNamespace) {
            prefix = fNamespaceContext.getPrefix(namespaceURI);

            if (prefix != null) {
                prefix = fSymbolTable.addSymbol(prefix);
            }
        }

        writeStartElement(prefix, localName, namespaceURI);
    }

    /**
     * @param prefix
     * @param localName
     * @param namespaceURI
     * @throws XMLStreamException
     */
    @Override
    public void writeStartElement(String prefix, String localName,
        String namespaceURI) throws XMLStreamException {
        try {
            if (localName == null) {
                throw new XMLStreamException("Local Name cannot be null");
            }

            if (namespaceURI == null) {
                throw new XMLStreamException("NamespaceURI cannot be null");
            }

            if (!fIsRepairingNamespace) {
                if (prefix == null) {
                    throw new XMLStreamException("Prefix cannot be null");
                }
            }

            if (fStartTagOpened) {
                closeStartTag();
            }

            openStartTag();
            namespaceURI = fSymbolTable.addSymbol(namespaceURI);

            if (prefix != null) {
                prefix = fSymbolTable.addSymbol(prefix);
            }

            fElementStack.push(prefix, localName, null, namespaceURI, false);
            fInternalNamespaceContext.pushContext();

            String tmpPrefix = fNamespaceContext.getPrefix(namespaceURI);


            if ((prefix != null) &&
                    ((tmpPrefix == null) || !prefix.equals(tmpPrefix))) {
                fInternalNamespaceContext.declarePrefix(prefix, namespaceURI);

            }

            if (fIsRepairingNamespace) {
                if ((prefix == null) ||
                        ((tmpPrefix != null) && prefix.equals(tmpPrefix))) {
                    return;
                }

                QName qname = new QName();
                qname.setValues(prefix, XMLConstants.XMLNS_ATTRIBUTE, null,
                    namespaceURI);
                fNamespaceDecls.add(qname);

                return;
            }

            if ((prefix != null) && (prefix != XMLConstants.DEFAULT_NS_PREFIX)) {
                fWriter.write(prefix);
                fWriter.write(":");
            }

            fWriter.write(localName);

        } catch (IOException ex) {
            throw new XMLStreamException(ex);
        }
    }

    /**
     * Writes character reference in hex format.
     */
    private void writeCharRef(int codePoint) throws IOException {
        fWriter.write( "&#x" );
        fWriter.write( Integer.toHexString(codePoint) );
        fWriter.write( ';' );
    }

    /**
     * Writes XML content to underlying writer. Escapes characters unless
     * escaping character feature is turned off.
     */
    private void writeXMLContent(char[] content, int start, int length,
        boolean escapeChars) throws IOException {
        if (!escapeChars) {
            fWriter.write(content, start, length);

            return;
        }

        // Index of the next char to be written
        int startWritePos = start;

        final int end = start + length;

        for (int index = start; index < end; index++) {
            char ch = content[index];

            if (fEncoder != null && !fEncoder.canEncode(ch)){
                fWriter.write(content, startWritePos, index - startWritePos );

                // Check if current and next characters forms a surrogate pair
                // and escape it to avoid generation of invalid xml content
                if ( index != end - 1 && Character.isSurrogatePair(ch, content[index+1])) {
                    writeCharRef(Character.toCodePoint(ch, content[index+1]));
                    index++;
                } else {
                    writeCharRef(ch);
                }
                startWritePos = index + 1;
                continue;
            }

            switch (ch) {
            case '<':
                fWriter.write(content, startWritePos, index - startWritePos);
                fWriter.write("&lt;");
                startWritePos = index + 1;

                break;

            case '&':
                fWriter.write(content, startWritePos, index - startWritePos);
                fWriter.write("&amp;");
                startWritePos = index + 1;

                break;

            case '>':
                fWriter.write(content, startWritePos, index - startWritePos);
                fWriter.write("&gt;");
                startWritePos = index + 1;

                break;
            }
        }

        // Write any pending data
        fWriter.write(content, startWritePos, end - startWritePos);
    }

    private void writeXMLContent(String content) throws IOException {
        if ((content != null) && (content.length() > 0)) {
            writeXMLContent(content,
                    fEscapeCharacters,  // boolean = escapeChars
                    false);             // false = escapeDoubleQuotes
        }
    }

    /**
     * Writes XML content to underlying writer. Escapes characters unless
     * escaping character feature is turned off.
     */
    private void writeXMLContent(
            String content,
            boolean escapeChars,
            boolean escapeDoubleQuotes)
        throws IOException {

        if (!escapeChars) {
            fWriter.write(content);

            return;
        }

        // Index of the next char to be written
        int startWritePos = 0;

        final int end = content.length();

        for (int index = 0; index < end; index++) {
            char ch = content.charAt(index);

            if (fEncoder != null && !fEncoder.canEncode(ch)){
                fWriter.write(content, startWritePos, index - startWritePos );

                // Check if current and next characters forms a surrogate pair
                // and escape it to avoid generation of invalid xml content
                if ( index != end - 1 && Character.isSurrogatePair(ch, content.charAt(index+1))) {
                    writeCharRef(Character.toCodePoint(ch, content.charAt(index+1)));
                    index++;
                } else {
                    writeCharRef(ch);
                }

                startWritePos = index + 1;
                continue;
            }

            switch (ch) {
            case '<':
                fWriter.write(content, startWritePos, index - startWritePos);
                fWriter.write("&lt;");
                startWritePos = index + 1;

                break;

            case '&':
                fWriter.write(content, startWritePos, index - startWritePos);
                fWriter.write("&amp;");
                startWritePos = index + 1;

                break;

            case '>':
                fWriter.write(content, startWritePos, index - startWritePos);
                fWriter.write("&gt;");
                startWritePos = index + 1;

                break;

            case '"':
                fWriter.write(content, startWritePos, index - startWritePos);
                if (escapeDoubleQuotes) {
                    fWriter.write("&quot;");
                } else {
                    fWriter.write('"');
                }
                startWritePos = index + 1;

                break;
            }
        }

        // Write any pending data
        fWriter.write(content, startWritePos, end - startWritePos);
    }

    /**
     * marks close of start tag and writes the same into the writer.
     */
    private void closeStartTag() throws XMLStreamException {
        try {
            ElementState currentElement = fElementStack.peek();

            if (fIsRepairingNamespace) {
                repair();
                correctPrefix(currentElement, XMLStreamConstants.START_ELEMENT);

                if ((currentElement.prefix != null) &&
                        (currentElement.prefix != XMLConstants.DEFAULT_NS_PREFIX)) {
                    fWriter.write(currentElement.prefix);
                    fWriter.write(":");
                }

                fWriter.write(currentElement.localpart);

                int len = fNamespaceDecls.size();
                QName qname;

                for (int i = 0; i < len; i++) {
                    qname = fNamespaceDecls.get(i);

                    if (qname != null) {
                        if (fInternalNamespaceContext.declarePrefix(qname.prefix,
                            qname.uri)) {
                            writenamespace(qname.prefix, qname.uri);
                        }
                    }
                }

                fNamespaceDecls.clear();

                Attribute attr;

                for (int j = 0; j < fAttributeCache.size(); j++) {
                    attr = fAttributeCache.get(j);

                    if ((attr.prefix != null) && (attr.uri != null)) {
                        if (!attr.prefix.isEmpty() && !attr.uri.isEmpty() ) {
                            String tmp = fInternalNamespaceContext.getPrefix(attr.uri);

                            if ((tmp == null) || (!tmp.equals(attr.prefix))) {
                                tmp = getAttrPrefix(attr.uri);
                                if (tmp == null) {
                                    if (fInternalNamespaceContext.declarePrefix(attr.prefix,
                                        attr.uri)) {
                                        writenamespace(attr.prefix, attr.uri);
                                    }
                                } else {
                                    writenamespace(attr.prefix, attr.uri);
                                }
                            }
                        }
                    }

                    writeAttributeWithPrefix(attr.prefix, attr.localpart,
                        attr.value);
                }
                fAttrNamespace = null;
                fAttributeCache.clear();
            }

            if (currentElement.isEmpty) {
                fElementStack.pop();
                fInternalNamespaceContext.popContext();
                fWriter.write(CLOSE_EMPTY_ELEMENT);
            } else {
                fWriter.write(CLOSE_START_TAG);
            }

            fStartTagOpened = false;
        } catch (IOException ex) {
            fStartTagOpened = false;
            throw new XMLStreamException(ex);
        }
    }

    /**
     * marks open of start tag and writes the same into the writer.
     */
    private void openStartTag() throws IOException {
        fStartTagOpened = true;
        fWriter.write(OPEN_START_TAG);
    }

    /**
     *
     * @param uri
     * @return
     */
    private void correctPrefix(QName attr, int type) {
        String tmpPrefix;
        String prefix;
        String uri;
        prefix = attr.prefix;
        uri = attr.uri;
        boolean isSpecialCaseURI = false;

        if (prefix == null || prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
            if (uri == null) {
                return;
            }

            if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix) && uri.equals(XMLConstants.DEFAULT_NS_PREFIX))
                return;

            uri = fSymbolTable.addSymbol(uri);

            QName decl;

            for (int i = 0; i < fNamespaceDecls.size(); i++) {
                decl = fNamespaceDecls.get(i);

                if ((decl != null) && (decl.uri.equals(attr.uri))) {
                    attr.prefix = decl.prefix;

                    return;
                }
            }

            tmpPrefix = fNamespaceContext.getPrefix(uri);

            if (XMLConstants.DEFAULT_NS_PREFIX.equals(tmpPrefix)) {
                if (type == XMLStreamConstants.START_ELEMENT) {
                    return;
                }
                else if (type == XMLStreamConstants.ATTRIBUTE) {
                    //the uri happens to be the same as that of the default namespace
                    tmpPrefix = getAttrPrefix(uri);
                    isSpecialCaseURI = true;
                }
            }

            if (tmpPrefix == null) {
                StringBuilder genPrefix = new StringBuilder("zdef");

                for (int i = 0; i < 1; i++) {
                    genPrefix.append(fPrefixGen.nextInt());
                }

                prefix = genPrefix.toString();
                prefix = fSymbolTable.addSymbol(prefix);
            } else {
                prefix = fSymbolTable.addSymbol(tmpPrefix);
            }

            if (tmpPrefix == null) {
                if (isSpecialCaseURI) {
                    addAttrNamespace(prefix, uri);
                } else {
                    QName qname = new QName();
                    qname.setValues(prefix, XMLConstants.XMLNS_ATTRIBUTE, null, uri);
                    fNamespaceDecls.add(qname);
                    fInternalNamespaceContext.declarePrefix(fSymbolTable.addSymbol(
                        prefix), uri);
                }
            }
        }

        attr.prefix = prefix;
    }

    /**
     * return the prefix if the attribute has an uri the same as that of the default namespace
     */
    private String getAttrPrefix(String uri) {
        if (fAttrNamespace != null) {
            return fAttrNamespace.get(uri);
        }
        return null;
    }
    private void addAttrNamespace(String prefix, String uri) {
        if (fAttrNamespace == null) {
            fAttrNamespace = new HashMap<>();
        }
        fAttrNamespace.put(prefix, uri);
    }
    /**
     * @param uri
     * @return
     */
    private boolean isDefaultNamespace(String uri) {
        String defaultNamespace = fInternalNamespaceContext.getURI(DEFAULT_PREFIX);
        return Objects.equals(uri, defaultNamespace);
    }

    /**
     * @param prefix
     * @param uri
     * @return
     */
    private boolean checkUserNamespaceContext(String prefix, String uri) {
        if (fNamespaceContext.userContext != null) {
            String tmpURI = fNamespaceContext.userContext.getNamespaceURI(prefix);

            if ((tmpURI != null) && tmpURI.equals(uri)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Correct's namespaces  as per requirements of isReparisingNamespace property.
     */
    protected void repair() {
        Attribute attr;
        Attribute attr2;
        ElementState currentElement = fElementStack.peek();
        removeDuplicateDecls();

        for(int i=0 ; i< fAttributeCache.size();i++){
            attr = fAttributeCache.get(i);
            if((attr.prefix != null && !attr.prefix.isEmpty()) || (attr.uri != null && !attr.uri.isEmpty())) {
                correctPrefix(currentElement,attr);
            }
        }

        if (!isDeclared(currentElement)) {
            if ((currentElement.prefix != null) &&
                    (currentElement.uri != null)) {
                if ((!currentElement.prefix.isEmpty()) && (!currentElement.uri.isEmpty())) {
                    fNamespaceDecls.add(currentElement);
                }
            }
        }

        for(int i=0 ; i< fAttributeCache.size();i++){
            attr = fAttributeCache.get(i);
            for(int j=i+1;j<fAttributeCache.size();j++){
                attr2 = fAttributeCache.get(j);
                if(!"".equals(attr.prefix)&& !"".equals(attr2.prefix)){
                    correctPrefix(attr,attr2);
                }
            }
        }

        repairNamespaceDecl(currentElement);

        int i;

        for (i = 0; i < fAttributeCache.size(); i++) {
            attr = fAttributeCache.get(i);
            /* If 'attr' is an attribute and it is in no namespace(which means that prefix="", uri=""), attr's
               namespace should not be redinded. See [http://www.w3.org/TR/REC-xml-names/#defaulting].
             */
            if (attr.prefix != null && attr.prefix.isEmpty() && attr.uri != null && attr.uri.isEmpty()){
                repairNamespaceDecl(attr);
            }
        }

        QName qname = null;

        for (i = 0; i < fNamespaceDecls.size(); i++) {
            qname = fNamespaceDecls.get(i);

            if (qname != null) {
                fInternalNamespaceContext.declarePrefix(qname.prefix, qname.uri);
            }
        }

        for (i = 0; i < fAttributeCache.size(); i++) {
            attr = fAttributeCache.get(i);
            correctPrefix(attr, XMLStreamConstants.ATTRIBUTE);
        }
    }

    /*
     *If element and/or attribute names in the same start or empty-element tag
     *are bound to different namespace URIs and are using the same prefix then
     *the element or the first occurring attribute retains the original prefix
     *and the following attributes have their prefixes replaced with a new prefix
     *that is bound to the namespace URIs of those attributes.
     */
    void correctPrefix(QName attr1, QName attr2) {
        String tmpPrefix;
        QName decl;

        checkForNull(attr1);
        checkForNull(attr2);

        if(attr1.prefix.equals(attr2.prefix) && !(attr1.uri.equals(attr2.uri))){

            tmpPrefix = fNamespaceContext.getPrefix(attr2.uri);

            if (tmpPrefix != null) {
                attr2.prefix = fSymbolTable.addSymbol(tmpPrefix);
            } else {
                for (int n=0; n<fNamespaceDecls.size(); n++) {
                    decl = fNamespaceDecls.get(n);
                    if(decl != null && (decl.uri.equals(attr2.uri))){
                        attr2.prefix = decl.prefix;

                        return;
                    }
                }

                //No namespace mapping found , so declare prefix.
                StringBuilder genPrefix = new StringBuilder("zdef");

                for (int k = 0; k < 1; k++) {
                    genPrefix.append(fPrefixGen.nextInt());
                }

                tmpPrefix = genPrefix.toString();
                tmpPrefix = fSymbolTable.addSymbol(tmpPrefix);
                attr2.prefix = tmpPrefix;

                QName qname = new QName();
                qname.setValues(tmpPrefix, XMLConstants.XMLNS_ATTRIBUTE, null,
                    attr2.uri);
                fNamespaceDecls.add(qname);
            }
        }
    }

    void checkForNull(QName attr) {
        if (attr.prefix == null) attr.prefix = XMLConstants.DEFAULT_NS_PREFIX;
        if (attr.uri == null) attr.uri = XMLConstants.DEFAULT_NS_PREFIX;
    }

    void removeDuplicateDecls(){
        QName decl1,decl2;
        for(int i =0; i<fNamespaceDecls.size(); i++) {
            decl1 = fNamespaceDecls.get(i);
            if(decl1!=null) {
                for(int j=i+1;j<fNamespaceDecls.size();j++){
                    decl2 = fNamespaceDecls.get(j);
                    // QName.equals relies on identity equality, so we can't use it,
                    // because prefixes aren't interned
                    if(decl2!=null && decl1.prefix.equals(decl2.prefix) && decl1.uri.equals(decl2.uri))
                        fNamespaceDecls.remove(j);
                }
            }
        }
    }

    /*
     *If an element or attribute name is bound to a prefix and there is a namespace
     *declaration that binds that prefix to a different URI then that namespace declaration
     *is either removed if the correct mapping is inherited from the parent context of that element,
     *or changed to the namespace URI of the element or attribute using that prefix.
     *
     */
    void repairNamespaceDecl(QName attr) {
        QName decl;
        String tmpURI;

        //check for null prefix.
        for (int j = 0; j < fNamespaceDecls.size(); j++) {
            decl = fNamespaceDecls.get(j);

            if (decl != null) {
                if ((attr.prefix != null) &&
                        (attr.prefix.equals(decl.prefix) &&
                        !(attr.uri.equals(decl.uri)))) {
                    tmpURI = fNamespaceContext.getNamespaceURI(attr.prefix);

                    //see if you need to add to symbole table.
                    if (tmpURI != null) {
                        if (tmpURI.equals(attr.uri)) {
                            fNamespaceDecls.set(j, null);
                        } else {
                            decl.uri = attr.uri;
                        }
                    }
                }
            }
        }
    }

    boolean isDeclared(QName attr) {
        QName decl;

        for (int n = 0; n < fNamespaceDecls.size(); n++) {
            decl = fNamespaceDecls.get(n);

            if ((attr.prefix != null) &&
                    ((attr.prefix.equals(decl.prefix)) && (decl.uri.equals(attr.uri)))) {
                return true;
            }
        }

        if (attr.uri != null) {
            if (fNamespaceContext.getPrefix(attr.uri) != null) {
                return true;
            }
        }

        return false;
    }

    /*
     * Start of Internal classes.
     *
     */
    protected class ElementStack {
        /** The stack data. */
        protected ElementState[] fElements;

        /** The size of the stack. */
        protected short fDepth;

        /** Default constructor. */
        public ElementStack() {
            fElements = new ElementState[10];

            for (int i = 0; i < fElements.length; i++) {
                fElements[i] = new ElementState();
            }
        }

        /**
         * Pushes an element on the stack.
         * <p>
         * <strong>Note:</strong> The QName values are copied into the
         * stack. In other words, the caller does <em>not</em> orphan
         * the element to the stack. Also, the QName object returned
         * is <em>not</em> orphaned to the caller. It should be
         * considered read-only.
         *
         * @param element The element to push onto the stack.
         *
         * @return Returns the actual QName object that stores the
         */
        public ElementState push(ElementState element) {
            if (fDepth == fElements.length) {
                ElementState[] array = new ElementState[fElements.length * 2];
                System.arraycopy(fElements, 0, array, 0, fDepth);
                fElements = array;

                for (int i = fDepth; i < fElements.length; i++) {
                    fElements[i] = new ElementState();
                }
            }

            fElements[fDepth].setValues(element);

            return fElements[fDepth++];
        }

        /**
         *
         * @param prefix
         * @param localpart
         * @param rawname
         * @param uri
         * @param isEmpty
         * @return
         */
        public ElementState push(String prefix, String localpart,
            String rawname, String uri, boolean isEmpty) {
            if (fDepth == fElements.length) {
                ElementState[] array = new ElementState[fElements.length * 2];
                System.arraycopy(fElements, 0, array, 0, fDepth);
                fElements = array;

                for (int i = fDepth; i < fElements.length; i++) {
                    fElements[i] = new ElementState();
                }
            }

            fElements[fDepth].setValues(prefix, localpart, rawname, uri, isEmpty);

            return fElements[fDepth++];
        }

        /**
         * Pops an element off of the stack by setting the values of
         * the specified QName.
         * <p>
         * <strong>Note:</strong> The object returned is <em>not</em>
         * orphaned to the caller. Therefore, the caller should consider
         * the object to be read-only.
         */
        public ElementState pop() {
            return fElements[--fDepth];
        }

        /** Clears the stack without throwing away existing QName objects. */
        public void clear() {
            fDepth = 0;
        }

        /**
         * This function is as a result of optimization done for endElement --
         * we dont need to set the value for every end element we encouter.
         * For Well formedness checks we can have the same QName object that was pushed.
         * the values will be set only if application need to know about the endElement
         */
        public ElementState peek() {
            return fElements[fDepth - 1];
        }

        /**
         *
         * @return
         */
        public boolean empty() {
            return (fDepth > 0) ? false : true;
        }
    }

    /**
     * Maintains element state . localName for now.
     */
    class ElementState extends QName {
        public boolean isEmpty = false;

        public ElementState() {}

        public ElementState(String prefix, String localpart, String rawname,
            String uri) {
            super(prefix, localpart, rawname, uri);
        }

        public void setValues(String prefix, String localpart, String rawname,
            String uri, boolean isEmpty) {
            super.setValues(prefix, localpart, rawname, uri);
            this.isEmpty = isEmpty;
        }
    }

    /**
     * Attributes
     */
    class Attribute extends QName {
        String value;

        Attribute(String value) {
            super();
            this.value = value;
        }
    }

    /**
     * Implementation of NamespaceContext .
     *
     */
    class NamespaceContextImpl implements NamespaceContext {
        //root namespace context set by user.
        NamespaceContext userContext = null;

        //context built by the writer.
        NamespaceSupport internalContext = null;

        public String getNamespaceURI(String prefix) {
            String uri = null;

            if (prefix != null) {
                prefix = fSymbolTable.addSymbol(prefix);
            }

            if (internalContext != null) {
                uri = internalContext.getURI(prefix);

                if (uri != null) {
                    return uri;
                }
            }

            if (userContext != null) {
                uri = userContext.getNamespaceURI(prefix);

                return uri;
            }

            return null;
        }

        public String getPrefix(String uri) {
            String prefix = null;

            if (uri != null) {
                uri = fSymbolTable.addSymbol(uri);
            }

            if (internalContext != null) {
                prefix = internalContext.getPrefix(uri);

                if (prefix != null) {
                    return prefix;
                }
            }

            if (userContext != null) {
                return userContext.getPrefix(uri);
            }

            return null;
        }

        //Cleanup note: leaving these warnings to a xerces.internal.util cleanup
        public Iterator<String> getPrefixes(String uri) {
            List<String> prefixes = null;
            Iterator<String> itr = null;

            if (uri != null) {
                uri = fSymbolTable.addSymbol(uri);
            }

            if (userContext != null) {
                itr = userContext.getPrefixes(uri);
            }

            if (internalContext != null) {
                prefixes = internalContext.getPrefixes(uri);
            }

            if ((prefixes == null) && (itr != null)) {
                return itr;
            } else if ((prefixes != null) && (itr == null)) {
                return new ReadOnlyIterator<>(prefixes.iterator());
            } else if ((prefixes != null) && (itr != null)) {
                String ob = null;

                while (itr.hasNext()) {
                    ob = itr.next();

                    if (ob != null) {
                        ob = fSymbolTable.addSymbol(ob);
                    }

                    if (!prefixes.contains(ob)) {
                        prefixes.add(ob);
                    }
                }

                return new ReadOnlyIterator<>(prefixes.iterator());
            }

            return fReadOnlyIterator;
        }
    }

    // -- Map Interface --------------------------------------------------

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        return key.equals(OUTPUTSTREAM_PROPERTY);
    }

    /**
     * Returns the value associated to an implementation-specific
     * property.
     */
    @Override
    public Object get(Object key) {
        if (key.equals(OUTPUTSTREAM_PROPERTY)) {
            return fOutputStream;
        }
        return null;
    }

    @Override
    public Set<Entry<Object,Object>> entrySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * Overrides the method defined in AbstractMap which is
     * not completely implemented. Calling toString() in
     * AbstractMap would cause an unsupported exection to
     * be thrown.
     */
    @Override
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    /**
     * Overrides the method defined in AbstractMap
     * This is required by the toString() method
     */
    @Override
    public int hashCode() {
        return fElementStack.hashCode();
    }
    /**
     * Overrides the method defined in AbstractMap
     * This is required to satisfy the contract for hashCode.
     */
    @Override
    public boolean equals(Object obj) {
        return (this == obj);
    }
}
