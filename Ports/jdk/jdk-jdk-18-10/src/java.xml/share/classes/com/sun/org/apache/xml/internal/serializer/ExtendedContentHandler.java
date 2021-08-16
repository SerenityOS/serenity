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

package com.sun.org.apache.xml.internal.serializer;

import javax.xml.transform.SourceLocator;

import org.xml.sax.SAXException;

/**
 * This interface describes extensions to the SAX ContentHandler interface.
 * It is intended to be used by a serializer. The methods on this interface will
 * implement SAX- like behavior. This allows the gradual collection of
 * information rather than having it all up front. For example the call
 * <pre>
 * startElement(namespaceURI,localName,qName,atts)
 * </pre>
 * could be replaced with the calls
 * <pre>
 * startElement(namespaceURI,localName,qName)
 * addAttributes(atts)
 * </pre>
 * If there are no attributes the second call can be dropped. If attributes are
 * to be added one at a time with calls to
 * <pre>
 * addAttribute(namespaceURI, localName, qName, type, value)
 * </pre>
 * @xsl.usage internal
 */
abstract interface ExtendedContentHandler extends org.xml.sax.ContentHandler
{
    /**
     * Add at attribute to the current element
     * @param uri the namespace URI of the attribute name
     * @param localName the local name of the attribute (without prefix)
     * @param rawName the qualified name of the attribute
     * @param type the attribute type typically character data (CDATA)
     * @param value the value of the attribute
     * @param XSLAttribute true if the added attribute is coming from an xsl:attribute element
     * @throws SAXException
     */
    public void addAttribute(
        String uri,
        String localName,
        String rawName,
        String type,
        String value,
        boolean XSLAttribute)
        throws SAXException;
    /**
     * Add attributes to the current element
     * @param atts the attributes to add.
     * @throws SAXException
     */
    public void addAttributes(org.xml.sax.Attributes atts)
        throws org.xml.sax.SAXException;
    /**
     * Add an attribute to the current element. The namespace URI of the
     * attribute will be calculated from the prefix of qName. The local name
     * will be derived from qName and the type will be assumed to be "CDATA".
     * @param qName
     * @param value
     */
    public void addAttribute(String qName, String value);

    /**
     * This method is used to notify of a character event, but passing the data
     * as a character String rather than the standard character array.
     * @param chars the character data
     * @throws SAXException
     */
    public void characters(String chars) throws SAXException;

    /**
     * This method is used to notify of a character event, but passing the data
     * as a DOM Node rather than the standard character array.
     * @param node a DOM Node containing text.
     * @throws SAXException
     */
    public void characters(org.w3c.dom.Node node) throws org.xml.sax.SAXException;
    /**
     * This method is used to notify that an element has ended. Unlike the
     * standard SAX method
     * <pre>
     * endElement(namespaceURI,localName,qName)
     * </pre>
     * only the last parameter is passed. If needed the serializer can derive
     * the localName from the qualified name and derive the namespaceURI from
     * its implementation.
     * @param elemName the fully qualified element name.
     * @throws SAXException
     */
    public void endElement(String elemName) throws SAXException;

    /**
     * This method is used to notify that an element is starting.
     * This method is just like the standard SAX method
     * <pre>
     * startElement(uri,localName,qname,atts)
     * </pre>
     * but without the attributes.
     * @param uri the namespace URI of the element
     * @param localName the local name (without prefix) of the element
     * @param qName the qualified name of the element
     *
     * @throws SAXException
     */
    public void startElement(String uri, String localName, String qName)
        throws org.xml.sax.SAXException;

    /**
     * This method is used to notify of the start of an element
     * @param qName the fully qualified name of the element
     * @throws SAXException
     */
    public void startElement(String qName) throws SAXException;
    /**
     * This method is used to notify that a prefix mapping is to start, but
     * after an element is started. The SAX method call
     * <pre>
     * startPrefixMapping(prefix,uri)
     * </pre>
     * is used just before an element starts and applies to the element to come,
     * not to the current element.  This method applies to the current element.
     * For example one could make the calls in this order:
     * <pre>
     * startElement("prfx8:elem9")
     * namespaceAfterStartElement("http://namespace8","prfx8")
     * </pre>
     *
     * @param uri the namespace URI being declared
     * @param prefix the prefix that maps to the given namespace
     * @throws SAXException
     */
    public void namespaceAfterStartElement(String uri, String prefix)
        throws SAXException;

    /**
     * This method is used to notify that a prefix maping is to start, which can
     * be for the current element, or for the one to come.
     * @param prefix the prefix that maps to the given URI
     * @param uri the namespace URI of the given prefix
     * @param shouldFlush if true this call is like the SAX
     * startPrefixMapping(prefix,uri) call and the mapping applies to the
     * element to come.  If false the mapping applies to the current element.
     * @return boolean false if the prefix mapping was already in effect (in
     * other words we are just re-declaring), true if this is a new, never
     * before seen mapping for the element.
     * @throws SAXException
     */
    public boolean startPrefixMapping(
        String prefix,
        String uri,
        boolean shouldFlush)
        throws SAXException;
    /**
     * Notify of an entity reference.
     * @param entityName the name of the entity
     * @throws SAXException
     */
    public void entityReference(String entityName) throws SAXException;

    /**
     * This method returns an object that has the current namespace mappings in
     * effect.
     *
     * @return NamespaceMappings an object that has the current namespace
     * mappings in effect.
     */
    public NamespaceMappings getNamespaceMappings();
    /**
     * This method returns the prefix that currently maps to the given namespace
     * URI.
     * @param uri the namespace URI
     * @return String the prefix that currently maps to the given URI.
     */
    public String getPrefix(String uri);
    /**
     * This method gets the prefix associated with a current element or
     * attribute name.
     * @param name the qualified name of an element, or attribute
     * @param isElement true if it is an element name, false if it is an
     * atttribute name
     * @return String the namespace URI associated with the element or
     * attribute.
     */
    public String getNamespaceURI(String name, boolean isElement);
    /**
     * This method returns the namespace URI currently associated with the
     * prefix.
     * @param prefix a prefix of an element or attribute.
     * @return String the namespace URI currently associated with the prefix.
     */
    public String getNamespaceURIFromPrefix(String prefix);

    /**
     * This method is used to set the source locator, which might be used to
     * generated an error message.
     * @param locator the source locator
     */
    public void setSourceLocator(SourceLocator locator);

    // Bit constants for addUniqueAttribute().

    // The attribute value contains no bad characters. A "bad" character is one which
    // is greater than 126 or it is one of '<', '>', '&' or '"'.
    public static final int NO_BAD_CHARS = 0x1;

    // An HTML empty attribute (e.g. <OPTION selected>).
    public static final int HTML_ATTREMPTY = 0x2;

    // An HTML URL attribute
    public static final int HTML_ATTRURL = 0x4;

    /**
     * Add a unique attribute to the current element.
     * The attribute is guaranteed to be unique here. The serializer can write
     * it out immediately without saving it in a table first. The integer
     * flag contains information about the attribute, which helps the serializer
     * to decide whether a particular processing is needed.
     *
     * @param qName the fully qualified attribute name.
     * @param value the attribute value
     * @param flags a bitwise flag
     */
    public void addUniqueAttribute(String qName, String value, int flags)
        throws SAXException;

    /**
     * Add an attribute from an xsl:attribute element.
     * @param qName the qualified attribute name (prefix:localName)
     * @param value the attributes value
     * @param uri the uri that the prefix of the qName is mapped to.
     */
    public void addXSLAttribute(String qName, final String value, final String uri);

    /**
     * Add at attribute to the current element, not from an xsl:attribute
     * element.
     * @param uri the namespace URI of the attribute name
     * @param localName the local name of the attribute (without prefix)
     * @param rawName the qualified name of the attribute
     * @param type the attribute type typically character data (CDATA)
     * @param value the value of the attribute
     * @throws SAXException
     */
    public void addAttribute(
        String uri,
        String localName,
        String rawName,
        String type,
        String value)
        throws SAXException;
}
