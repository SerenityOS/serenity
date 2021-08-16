/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.stream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

/**
 *  The XMLStreamReader interface allows forward, read-only access to XML.
 *  It is designed to be the lowest level and most efficient way to
 *  read XML data.
 *
 * <p>
 * The XMLStreamReader is designed to iterate over XML using
 * next() and hasNext().  The data can be accessed using methods such as getEventType(),
 * getNamespaceURI(), getLocalName() and getText();
 *
 * <p>
 * An XMLStreamReader instance is created with an initial event type START_DOCUMENT.
 * At any moment in time, it has a current event that the methods of the interface
 * access and may load the next event through the {@link #next() next()} method.
 * The current event type can be determined by {@link #getEventType getEventType()}, and
 * the next returned by the {@link #next() next()} method.
 *
 * <p>
 * Parsing events are defined as the XML Declaration, a DTD,
 * start tag, character data, white space, end tag, comment,
 * or processing instruction.  An attribute or namespace event may be encountered
 * at the root level of a document as the result of a query operation.
 *
 * <p>
 * For XML 1.0 compliance an XML processor must pass the
 * identifiers of declared unparsed entities, notation declarations and their
 * associated identifiers to the application.  This information is
 * provided through the property API on this interface.
 * The following two properties allow access to this information:
 * javax.xml.stream.notations and javax.xml.stream.entities.
 * When the current event is a DTD the following call will return a
 * list of Notations
 * {@code List l = (List) getProperty("javax.xml.stream.notations");}
 * The following call will return a list of entity declarations:
 * {@code List l = (List) getProperty("javax.xml.stream.entities");}
 * These properties can only be accessed during a DTD event and
 * are defined to return null if the information is not available.
 *
 * <p>
 * The following table describes which methods are valid in what state.
 * If a method is called in an invalid state the method will throw a
 * java.lang.IllegalStateException.
 *
 * <table class="striped">
 *   <caption>Valid methods for each state</caption>
 *   <thead>
 *     <tr>
 *       <th scope="col">Event Type</th>
 *       <th scope="col">Valid Methods</th>
 *     </tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <th scope="row"> All States  </th>
 *       <td> getProperty(), hasNext(), require(), close(),
 *            getNamespaceURI(), isStartElement(),
 *            isEndElement(), isCharacters(), isWhiteSpace(),
 *            getNamespaceContext(), getEventType(),getLocation(),
 *            hasText(), hasName()
 *       </td>
 *     </tr>
 *     <tr>
 *       <th scope="row"> START_ELEMENT  </th>
 *       <td> next(), getName(), getLocalName(), hasName(), getPrefix(),
 *            getAttributeXXX(), isAttributeSpecified(),
 *            getNamespaceXXX(),
 *            getElementText(), nextTag()
 *       </td>
 *     </tr>
 *     <tr>
 *       <th scope="row"> ATTRIBUTE  </th>
 *       <td> next(), nextTag()
 *            getAttributeXXX(), isAttributeSpecified(),
 *       </td>
 *     </tr>
 *     <tr>
 *       <th scope="row"> NAMESPACE  </th>
 *       <td> next(), nextTag()
 *            getNamespaceXXX()
 *       </td>
 *     </tr>
 *     <tr>
 *       <th scope="row"> END_ELEMENT  </th>
 *       <td> next(), getName(), getLocalName(), hasName(), getPrefix(),
 *            getNamespaceXXX(), nextTag()
 *      </td>
 *     </tr>
 *     <tr>
 *       <th scope="row"> CHARACTERS  </th>
 *       <td> next(), getTextXXX(), nextTag() </td>
 *     </tr>
 *     <tr>
 *       <th scope="row"> CDATA  </th>
 *       <td> next(), getTextXXX(), nextTag() </td>
 *     </tr>
 *     <tr>
 *       <th scope="row"> COMMENT  </th>
 *       <td> next(), getTextXXX(), nextTag() </td>
 *     </tr>
 *     <tr>
 *       <th scope="row"> SPACE  </th>
 *       <td> next(), getTextXXX(), nextTag() </td>
 *     </tr>
 *     <tr>
 *       <th scope="row"> START_DOCUMENT  </th>
 *       <td> next(), getEncoding(), getVersion(), isStandalone(), standaloneSet(),
 *            getCharacterEncodingScheme(), nextTag()</td>
 *     </tr>
 *     <tr>
 *       <th scope="row"> END_DOCUMENT  </th>
 *       <td> close()</td>
 *     </tr>
 *     <tr>
 *       <th scope="row"> PROCESSING_INSTRUCTION  </th>
 *       <td> next(), getPITarget(), getPIData(), nextTag() </td>
 *     </tr>
 *     <tr>
 *       <th scope="row"> ENTITY_REFERENCE  </th>
 *       <td> next(), getLocalName(), getText(), nextTag() </td>
 *     </tr>
 *     <tr>
 *       <th scope="row"> DTD  </th>
 *       <td> next(), getText(), nextTag() </td>
 *     </tr>
 *   </tbody>
 *  </table>
 *
 * @version 1.0
 * @author Copyright (c) 2009 by Oracle Corporation. All Rights Reserved.
 * @see javax.xml.stream.events.XMLEvent
 * @see XMLInputFactory
 * @see XMLStreamWriter
 * @since 1.6
 */
public interface XMLStreamReader extends XMLStreamConstants {
  /**
   * Get the value of a feature/property from the underlying implementation
   * @param name The name of the property, may not be null
   * @return The value of the property
   * @throws IllegalArgumentException if name is null
   */
  public Object getProperty(java.lang.String name) throws java.lang.IllegalArgumentException;

  /**
   * Get next parsing event - a processor may return all contiguous
   * character data in a single chunk, or it may split it into several chunks.
   * If the property javax.xml.stream.isCoalescing is set to true
   * element content must be coalesced and only one CHARACTERS event
   * must be returned for contiguous element content or
   * CDATA Sections.
   *
   * By default entity references must be
   * expanded and reported transparently to the application.
   * An exception will be thrown if an entity reference cannot be expanded.
   * If element content is empty (i.e. content is "") then no CHARACTERS event will be reported.
   *
   * <p>Given the following XML:<br>
   * {@code <foo><!--description-->content text<![CDATA[<greeting>Hello>/greeting>]]>other content>/foo>}<br>
   * The behavior of calling next() when being on foo will be:<br>
   * 1- the comment (COMMENT)<br>
   * 2- then the characters section (CHARACTERS)<br>
   * 3- then the CDATA section (another CHARACTERS)<br>
   * 4- then the next characters section (another CHARACTERS)<br>
   * 5- then the END_ELEMENT<br>
   *
   * <p><b>NOTE:</b> empty element (such as {@code <tag/>}) will be reported
   *  with  two separate events: START_ELEMENT, END_ELEMENT - This preserves
   *   parsing equivalency of empty element to {@code <tag></tag>}.
   *
   * @see javax.xml.stream.events.XMLEvent
   * @return the integer code corresponding to the current parse event
   * @throws java.util.NoSuchElementException if this is called when hasNext() returns false
   * @throws XMLStreamException  if there is an error processing the underlying XML source
   */
  public int next() throws XMLStreamException;

  /**
   * Test if the current event is of the given type and if the namespace and name match the current
   * namespace and name of the current event.  If the namespaceURI is null it is not checked for equality,
   * if the localName is null it is not checked for equality.
   * @param type the event type
   * @param namespaceURI the uri of the event, may be null
   * @param localName the localName of the event, may be null
   * @throws XMLStreamException if the required values are not matched.
   */
  public void require(int type, String namespaceURI, String localName) throws XMLStreamException;

  /**
   * Reads the content of a text-only element, an exception is thrown if this is
   * not a text-only element.
   * Regardless of value of javax.xml.stream.isCoalescing this method always returns coalesced content.
   * <br> Precondition: the current event is START_ELEMENT.
   * <br> Postcondition: the current event is the corresponding END_ELEMENT.
   *
   * <br>The method does the following (implementations are free to optimized
   * but must do equivalent processing):
   * <pre>
   * if(getEventType() != XMLStreamConstants.START_ELEMENT) {
   *     throw new XMLStreamException(
   *     "parser must be on START_ELEMENT to read next text", getLocation());
   * }
   *
   * int eventType = next();
   * StringBuffer content = new StringBuffer();
   * while(eventType != XMLStreamConstants.END_ELEMENT) {
   *     if(eventType == XMLStreamConstants.CHARACTERS
   *        || eventType == XMLStreamConstants.CDATA
   *        || eventType == XMLStreamConstants.SPACE
   *        || eventType == XMLStreamConstants.ENTITY_REFERENCE) {
   *           buf.append(getText());
   *     } else if(eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
   *               || eventType == XMLStreamConstants.COMMENT) {
   *         // skipping
   *     } else if(eventType == XMLStreamConstants.END_DOCUMENT) {
   *         throw new XMLStreamException(
   *         "unexpected end of document when reading element text content", this);
   *     } else if(eventType == XMLStreamConstants.START_ELEMENT) {
   *         throw new XMLStreamException(
   *         "element text content may not contain START_ELEMENT", getLocation());
   *     } else {
   *         throw new XMLStreamException(
   *         "Unexpected event type "+eventType, getLocation());
   *     }
   *     eventType = next();
   * }
   * return buf.toString();
   * </pre>
   *
   * @return the content of a text-only element
   * @throws XMLStreamException if the current event is not a START_ELEMENT
   * or if a non text element is encountered
   */
  public String getElementText() throws XMLStreamException;

  /**
   * Skips any white space (isWhiteSpace() returns true), COMMENT,
   * or PROCESSING_INSTRUCTION,
   * until a START_ELEMENT or END_ELEMENT is reached.
   * If other than white space characters, COMMENT, PROCESSING_INSTRUCTION, START_ELEMENT, END_ELEMENT
   * are encountered, an exception is thrown. This method should
   * be used when processing element-only content seperated by white space.
   *
   * <br> Precondition: none
   * <br> Postcondition: the current event is START_ELEMENT or END_ELEMENT
   * and cursor may have moved over any whitespace event.
   *
   * <br>Essentially it does the following (implementations are free to optimized
   * but must do equivalent processing):
   * <pre> {@code
   * int eventType = next();
   * while((eventType == XMLStreamConstants.CHARACTERS && isWhiteSpace()) // skip whitespace
   * || (eventType == XMLStreamConstants.CDATA && isWhiteSpace())
   * // skip whitespace
   * || eventType == XMLStreamConstants.SPACE
   * || eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
   * || eventType == XMLStreamConstants.COMMENT
   * ) {
   *     eventType = next();
   * }
   * if (eventType != XMLStreamConstants.START_ELEMENT && eventType != XMLStreamConstants.END_ELEMENT) {
   *     throw new String XMLStreamException("expected start or end tag", getLocation());
   * }
   * return eventType; }
   * </pre>
   *
   * @return the event type of the element read (START_ELEMENT or END_ELEMENT)
   * @throws XMLStreamException if the current event is not white space, PROCESSING_INSTRUCTION,
   * START_ELEMENT or END_ELEMENT
   * @throws java.util.NoSuchElementException if this is called when hasNext() returns false
   */
  public int nextTag() throws XMLStreamException;

  /**
   * Returns true if there are more parsing events and false
   * if there are no more events.  This method will return
   * false if the current state of the XMLStreamReader is
   * END_DOCUMENT
   * @return true if there are more events, false otherwise
   * @throws XMLStreamException if there is a fatal error detecting the next state
   */
  public boolean hasNext() throws XMLStreamException;

  /**
   * Frees any resources associated with this Reader. This method does not close the
   * underlying input source.
   * @throws XMLStreamException if there are errors freeing associated resources
   */
  public void close() throws XMLStreamException;

  /**
   * Return the uri for the given prefix.
   * The uri returned depends on the current state of the processor.
   *
   * <p><strong>NOTE:</strong>The 'xml' prefix is bound as defined in
   * <a href="http://www.w3.org/TR/REC-xml-names/#ns-using">Namespaces in XML</a>
   * specification to "http://www.w3.org/XML/1998/namespace".
   *
   * <p><strong>NOTE:</strong> The 'xmlns' prefix must be resolved to following namespace
   * <a href="http://www.w3.org/2000/xmlns/">http://www.w3.org/2000/xmlns/</a>
   * @param prefix The prefix to lookup, may not be null
   * @return the uri bound to the given prefix or null if it is not bound
   * @throws IllegalArgumentException if the prefix is null
   */
  public String getNamespaceURI(String prefix);

  /**
   * Returns true if the cursor points to a start tag (otherwise false)
   * @return true if the cursor points to a start tag, false otherwise
   */
  public boolean isStartElement();

  /**
   * Returns true if the cursor points to an end tag (otherwise false)
   * @return true if the cursor points to an end tag, false otherwise
   */
  public boolean isEndElement();

  /**
   * Returns true if the cursor points to a character data event
   * @return true if the cursor points to character data, false otherwise
   */
  public boolean isCharacters();

  /**
   * Returns true if the cursor points to a character data event
   * that consists of all whitespace
   * @return true if the cursor points to all whitespace, false otherwise
   */
  public boolean isWhiteSpace();


  /**
   * Returns the normalized attribute value of the
   * attribute with the namespace and localName
   * If the namespaceURI is null the namespace
   * is not checked for equality
   * @param namespaceURI the namespace of the attribute
   * @param localName the local name of the attribute, cannot be null
   * @return returns the value of the attribute , returns null if not found
   * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
   */
  public String getAttributeValue(String namespaceURI,
                                  String localName);

  /**
   * Returns the count of attributes on this START_ELEMENT,
   * this method is only valid on a START_ELEMENT or ATTRIBUTE.  This
   * count excludes namespace definitions.  Attribute indices are
   * zero-based.
   * @return returns the number of attributes
   * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
   */
  public int getAttributeCount();

  /** Returns the qname of the attribute at the provided index
   *
   * @param index the position of the attribute
   * @return the QName of the attribute
   * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
   */
  public QName getAttributeName(int index);

  /**
   * Returns the namespace of the attribute at the provided
   * index
   * @param index the position of the attribute
   * @return the namespace URI (can be null)
   * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
   */
  public String getAttributeNamespace(int index);

  /**
   * Returns the localName of the attribute at the provided
   * index
   * @param index the position of the attribute
   * @return the localName of the attribute
   * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
   */
  public String getAttributeLocalName(int index);

  /**
   * Returns the prefix of this attribute at the
   * provided index
   * @param index the position of the attribute
   * @return the prefix of the attribute
   * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
   */
  public String getAttributePrefix(int index);

  /**
   * Returns the XML type of the attribute at the provided
   * index
   * @param index the position of the attribute
   * @return the XML type of the attribute
   * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
   */
  public String getAttributeType(int index);

  /**
   * Returns the value of the attribute at the
   * index
   * @param index the position of the attribute
   * @return the attribute value
   * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
   */
  public String getAttributeValue(int index);

  /**
   * Returns a boolean which indicates if this
   * attribute was created by default
   * @param index the position of the attribute
   * @return true if this is a default attribute
   * @throws IllegalStateException if this is not a START_ELEMENT or ATTRIBUTE
   */
  public boolean isAttributeSpecified(int index);

  /**
   * Returns the count of namespaces declared on this START_ELEMENT or END_ELEMENT,
   * this method is only valid on a START_ELEMENT, END_ELEMENT or NAMESPACE. On
   * an END_ELEMENT the count is of the namespaces that are about to go
   * out of scope.  This is the equivalent of the information reported
   * by SAX callback for an end element event.
   * @return returns the number of namespace declarations on this specific element
   * @throws IllegalStateException if this is not a START_ELEMENT, END_ELEMENT or NAMESPACE
   */
  public int getNamespaceCount();

  /**
   * Returns the prefix for the namespace declared at the
   * index.  Returns null if this is the default namespace
   * declaration
   *
   * @param index the position of the namespace declaration
   * @return returns the namespace prefix
   * @throws IllegalStateException if this is not a START_ELEMENT, END_ELEMENT or NAMESPACE
   */
  public String getNamespacePrefix(int index);

  /**
   * Returns the uri for the namespace declared at the
   * index.
   *
   * @param index the position of the namespace declaration
   * @return returns the namespace uri
   * @throws IllegalStateException if this is not a START_ELEMENT, END_ELEMENT or NAMESPACE
   */
  public String getNamespaceURI(int index);

  /**
   * Returns a read only namespace context for the current
   * position.  The context is transient and only valid until
   * a call to next() changes the state of the reader.
   * @return return a namespace context
   */
  public NamespaceContext getNamespaceContext();

  /**
   * Returns a reader that points to the current start element
   * and all of its contents.  Throws an XMLStreamException if the
   * cursor does not point to a START_ELEMENT.<p>
   * The sub stream is read from it MUST be read before the parent stream is
   * moved on, if not any call on the sub stream will cause an XMLStreamException to be
   * thrown.   The parent stream will always return the same result from next()
   * whatever is done to the sub stream.
   * @return an XMLStreamReader which points to the next element
   */
  //  public XMLStreamReader subReader() throws XMLStreamException;

  /**
   * Allows the implementation to reset and reuse any underlying tables
   */
  //  public void recycle() throws XMLStreamException;

  /**
   * Returns an integer code that indicates the type of the event the cursor is
   * pointing to. The initial event type is {@link #START_DOCUMENT}.
   *
   * @return the type of the current event
   */
  public int getEventType();

  /**
   * Returns the current value of the parse event as a string,
   * this returns the string value of a CHARACTERS event,
   * returns the value of a COMMENT, the replacement value
   * for an ENTITY_REFERENCE, the string value of a CDATA section,
   * the string value for a SPACE event,
   * or the String value of the internal subset of the DTD.
   * If an ENTITY_REFERENCE has been resolved, any character data
   * will be reported as CHARACTERS events.
   * @return the current text or null
   * @throws java.lang.IllegalStateException if this state is not
   * a valid text state.
   */
  public String getText();

  /**
   * Returns an array which contains the characters from this event.
   * This array should be treated as read-only and transient. I.e. the array will
   * contain the text characters until the XMLStreamReader moves on to the next event.
   * Attempts to hold onto the character array beyond that time or modify the
   * contents of the array are breaches of the contract for this interface.
   * @return the current text or an empty array
   * @throws java.lang.IllegalStateException if this state is not
   * a valid text state.
   */
  public char[] getTextCharacters();

  /**
   * Gets the the text associated with a CHARACTERS, SPACE or CDATA event.
   * Text starting a "sourceStart" is copied into "target" starting at "targetStart".
   * Up to "length" characters are copied.  The number of characters actually copied is returned.
   *
   * The "sourceStart" argument must be greater or equal to 0 and less than or equal to
   * the number of characters associated with the event.  Usually, one requests text starting at a "sourceStart" of 0.
   * If the number of characters actually copied is less than the "length", then there is no more text.
   * Otherwise, subsequent calls need to be made until all text has been retrieved. For example:
   *
   * <pre>{@code
   * int length = 1024;
   * char[] myBuffer = new char[ length ];
   *
   * for ( int sourceStart = 0 ; ; sourceStart += length )
   * {
   *    int nCopied = stream.getTextCharacters( sourceStart, myBuffer, 0, length );
   *
   *   if (nCopied < length)
   *       break;
   * }
   * } </pre>
   * XMLStreamException may be thrown if there are any XML errors in the underlying source.
   * The "targetStart" argument must be greater than or equal to 0 and less than the length of "target",
   * Length must be greater than 0 and "targetStart + length" must be less than or equal to length of "target".
   *
   * @param sourceStart the index of the first character in the source array to copy
   * @param target the destination array
   * @param targetStart the start offset in the target array
   * @param length the number of characters to copy
   * @return the number of characters actually copied
   * @throws XMLStreamException if the underlying XML source is not well-formed
   * @throws IndexOutOfBoundsException if targetStart {@literal <} 0 or {@literal >} than the length of target
   * @throws IndexOutOfBoundsException if length {@literal <} 0 or targetStart + length {@literal >} length of target
   * @throws UnsupportedOperationException if this method is not supported
   * @throws NullPointerException is if target is null
   */
   public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length)
     throws XMLStreamException;

  /**
   * Gets the text associated with a CHARACTERS, SPACE or CDATA event.  Allows the underlying
   * implementation to return the text as a stream of characters.  The reference to the
   * Reader returned by this method is only valid until next() is called.
   *
   * All characters must have been checked for well-formedness.
   *
   * <p> This method is optional and will throw UnsupportedOperationException if it is not supported.
   * @throws UnsupportedOperationException if this method is not supported
   * @throws IllegalStateException if this is not a valid text state
   */
  //public Reader getTextStream();

  /**
   * Returns the offset into the text character array where the first
   * character (of this text event) is stored.
   *
   * @return the starting position of the text in the character array
   * @throws java.lang.IllegalStateException if this state is not
   * a valid text state.
   */
  public int getTextStart();

  /**
   * Returns the length of the sequence of characters for this
   * Text event within the text character array.
   *
   * @return the length of the text
   * @throws java.lang.IllegalStateException if this state is not
   * a valid text state.
   */
  public int getTextLength();

  /**
   * Return input encoding if known or null if unknown.
   * @return the encoding of this instance or null
   */
  public String getEncoding();

  /**
   * Return a boolean indicating whether the current event has text.
   * The following events have text:
   * CHARACTERS,DTD ,ENTITY_REFERENCE, COMMENT, SPACE
   *
   * @return true if the event has text, false otherwise
   */
  public boolean hasText();

  /**
   * Return the current location of the processor.
   * If the Location is unknown the processor should return
   * an implementation of Location that returns -1 for the
   * location and null for the publicId and systemId.
   * The location information is only valid until next() is
   * called.
   * @return the location of the cursor
   */
  public Location getLocation();

  /**
   * Returns a QName for the current START_ELEMENT or END_ELEMENT event
   * @return the QName for the current START_ELEMENT or END_ELEMENT event
   * @throws IllegalStateException if this is not a START_ELEMENT or
   * END_ELEMENT
   */
  public QName getName();

  /**
   * Returns the (local) name of the current event.
   * For START_ELEMENT or END_ELEMENT returns the (local) name of the current element.
   * For ENTITY_REFERENCE it returns entity name.
   * The current event must be START_ELEMENT or END_ELEMENT,
   * or ENTITY_REFERENCE
   * @return the localName
   * @throws IllegalStateException if this not a START_ELEMENT,
   * END_ELEMENT or ENTITY_REFERENCE
   */
  public String getLocalName();

  /**
   * returns a boolean indicating whether the current event has a name
   * (is a START_ELEMENT or END_ELEMENT).
   *
   * @return true if the event has a name, false otherwise
   */
  public boolean hasName();

  /**
   * If the current event is a START_ELEMENT or END_ELEMENT  this method
   * returns the URI of the prefix or the default namespace.
   * Returns null if the event does not have a prefix.
   * @return the URI bound to this elements prefix, the default namespace, or null
   */
  public String getNamespaceURI();

  /**
   * Returns the prefix of the current event or null if the event does not have a prefix
   * @return the prefix or null
   */
  public String getPrefix();

  /**
   * Get the xml version declared on the xml declaration
   * Returns null if none was declared
   * @return the XML version or null
   */
  public String getVersion();

  /**
   * Get the standalone declaration from the xml declaration
   * @return true if this is standalone, or false otherwise
   */
  public boolean isStandalone();

  /**
   * Checks if standalone was set in the document
   * @return true if standalone was set in the document, or false otherwise
   */
  public boolean standaloneSet();

  /**
   * Returns the character encoding declared on the xml declaration
   * Returns null if none was declared
   * @return the encoding declared in the document or null
   */
  public String getCharacterEncodingScheme();

  /**
   * Get the target of a processing instruction
   * @return the target or null
   */
  public String getPITarget();

  /**
   * Get the data section of a processing instruction
   * @return the data or null
   */
  public String getPIData();
}
