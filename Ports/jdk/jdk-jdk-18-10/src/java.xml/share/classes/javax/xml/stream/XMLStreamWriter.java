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

/**
 * The XMLStreamWriter interface specifies how to write XML.  The XMLStreamWriter  does
 * not perform well formedness checking on its input.  However
 * the writeCharacters method is required to escape {@literal &, < and >}
 * For attribute values the writeAttribute method will escape the
 * above characters plus {@literal "} to ensure that all character content
 * and attribute values are well formed.
 *
 * Each NAMESPACE
 * and ATTRIBUTE must be individually written.
 *
 * <table class="striped">
 *     <caption>XML Namespaces, {@code javax.xml.stream.isRepairingNamespaces} and write method behaviour</caption>
 *     <thead>
 *         <tr style="border-bottom: 1px solid black">
 *             <th scope="col" rowspan="2">Method</th> <!-- method -->
 *             <th scope="col" colspan="2">{@code isRepairingNamespaces} == true</th>
 *             <th scope="col" colspan="2">{@code isRepairingNamespaces} == false</th>
 *         </tr>
 *         <tr>
 *             <!-- method -->
 *             <th scope="col">namespaceURI bound</th>
 *             <th scope="col">namespaceURI unbound</th>
 *             <th scope="col">namespaceURI bound</th>
 *             <th scope="col">namespaceURI unbound</th>
 *         </tr>
 *     </thead>
 *
 *     <tbody>
 *         <tr>
 *             <th scope="row">{@code writeAttribute(namespaceURI, localName, value)}</th>
 *             <!-- isRepairingNamespaces == true -->
 *             <td>
 *                 <!-- namespaceURI bound -->
 *                 prefix:localName="value"&nbsp;<sup>[1]</sup>
 *             </td>
 *             <td>
 *                 <!-- namespaceURI unbound -->
 *                 xmlns:{generated}="namespaceURI" {generated}:localName="value"
 *             </td>
 *             <!-- isRepairingNamespaces == false -->
 *             <td>
 *                 <!-- namespaceURI bound -->
 *                 prefix:localName="value"&nbsp;<sup>[1]</sup>
 *             </td>
 *             <td>
 *                 <!-- namespaceURI unbound -->
 *                 {@code XMLStreamException}
 *             </td>
 *         </tr>
 *
 *         <tr>
 *             <th scope="row">{@code writeAttribute(prefix, namespaceURI, localName, value)}</th>
 *             <!-- isRepairingNamespaces == true -->
 *             <td>
 *                 <!-- namespaceURI bound -->
 *                 bound to same prefix:<br>
 *                 prefix:localName="value"&nbsp;<sup>[1]</sup><br>
 *                 <br>
 *                 bound to different prefix:<br>
 *                 xmlns:{generated}="namespaceURI" {generated}:localName="value"
 *             </td>
 *             <td>
 *                 <!-- namespaceURI unbound -->
 *                 xmlns:prefix="namespaceURI" prefix:localName="value"&nbsp;<sup>[3]</sup>
 *             </td>
 *             <!-- isRepairingNamespaces == false -->
 *             <td>
 *                 <!-- namespaceURI bound -->
 *                 bound to same prefix:<br>
 *                 prefix:localName="value"&nbsp;<sup>[1][2]</sup><br>
 *                 <br>
 *                 bound to different prefix:<br>
 *                 {@code XMLStreamException}<sup>[2]</sup>
 *             </td>
 *             <td>
 *                 <!-- namespaceURI unbound -->
 *                 xmlns:prefix="namespaceURI" prefix:localName="value"&nbsp;<sup>[2][5]</sup>
 *             </td>
 *         </tr>
 *
 *         <tr>
 *             <th scope="row">{@code writeStartElement(namespaceURI, localName)}<br>
 *                 <br>
 *                 {@code writeEmptyElement(namespaceURI, localName)}</th>
 *             <!-- isRepairingNamespaces == true -->
 *             <td>
 *                 <!-- namespaceURI bound -->
 *                 {@code <prefix:localName>}&nbsp;<sup>[1]</sup>
 *             </td>
 *             <td>
 *                 <!-- namespaceURI unbound -->
 *                 {@code <{generated}:localName xmlns:{generated}="namespaceURI">}
 *             </td>
 *             <!-- isRepairingNamespaces == false -->
 *             <td>
 *                 <!-- namespaceURI bound -->
 *                 {@code prefix:localName>}&nbsp;<sup>[1]</sup>
 *             </td>
 *             <td>
 *                 <!-- namespaceURI unbound -->
 *                 {@code XMLStreamException}
 *             </td>
 *         </tr>
 *
 *         <tr>
 *             <th scope="row">{@code writeStartElement(prefix, localName, namespaceURI)}<br>
 *                 <br>
 *                 {@code writeEmptyElement(prefix, localName, namespaceURI)}</th>
 *             <!-- isRepairingNamespaces == true -->
 *             <td>
 *                 <!-- namespaceURI bound -->
 *                 bound to same prefix:<br>
 *                 {@code <prefix:localName>}&nbsp;<sup>[1]</sup><br>
 *                 <br>
 *                 bound to different prefix:<br>
 *                 {@code <{generated}:localName xmlns:{generated}="namespaceURI">}
 *             </td>
 *             <td>
 *                 <!-- namespaceURI unbound -->
 *                 {@code <prefix:localName xmlns:prefix="namespaceURI">}&nbsp;<sup>[4]</sup>
 *             </td>
 *             <!-- isRepairingNamespaces == false -->
 *             <td>
 *                 <!-- namespaceURI bound -->
 *                 bound to same prefix:<br>
 *                 {@code <prefix:localName>}&nbsp;<sup>[1]</sup><br>
 *                 <br>
 *                 bound to different prefix:<br>
 *                 {@code XMLStreamException}
 *             </td>
 *             <td>
 *                 <!-- namespaceURI unbound -->
 *                 {@code <prefix:localName>}&nbsp;
 *             </td>
 *         </tr>
 *     </tbody>
 * </table>

 * Notes:
 * <ul>
 *    <li>[1] if namespaceURI == default Namespace URI, then no prefix is written</li>
 *    <li>[2] if prefix == "" || null {@literal &&} namespaceURI == "", then
 *            no prefix or Namespace declaration is generated or written</li>
 *    <li>[3] if prefix == "" || null, then a prefix is randomly generated</li>
 *    <li>[4] if prefix == "" || null, then it is treated as the default Namespace and
 *            no prefix is generated or written, an xmlns declaration is generated
 *            and written if the namespaceURI is unbound</li>
 *    <li>[5] if prefix == "" || null, then it is treated as an invalid attempt to
 *            define the default Namespace and an XMLStreamException is thrown</li>
 * </ul>
 *
 * @version 1.0
 * @author Copyright (c) 2009 by Oracle Corporation. All Rights Reserved.
 * @see XMLOutputFactory
 * @see XMLStreamReader
 * @since 1.6
 */
public interface XMLStreamWriter {

  /**
   * Writes a start tag to the output.  All writeStartElement methods
   * open a new scope in the internal namespace context.  Writing the
   * corresponding EndElement causes the scope to be closed.
   * @param localName local name of the tag, may not be null
   * @throws XMLStreamException if an error occurs
   */
  public void writeStartElement(String localName)
    throws XMLStreamException;

  /**
   * Writes a start tag to the output
   * @param namespaceURI the namespaceURI of the prefix to use, may not be null
   * @param localName local name of the tag, may not be null
   * @throws XMLStreamException if the namespace URI has not been bound to a prefix and
   * javax.xml.stream.isRepairingNamespaces has not been set to true
   */
  public void writeStartElement(String namespaceURI, String localName)
    throws XMLStreamException;

  /**
   * Writes a start tag to the output
   * @param localName local name of the tag, may not be null
   * @param prefix the prefix of the tag, may not be null
   * @param namespaceURI the uri to bind the prefix to, may not be null
   * @throws XMLStreamException if an error occurs
   */
  public void writeStartElement(String prefix,
                                String localName,
                                String namespaceURI)
    throws XMLStreamException;

  /**
   * Writes an empty element tag to the output
   * @param namespaceURI the uri to bind the tag to, may not be null
   * @param localName local name of the tag, may not be null
   * @throws XMLStreamException if the namespace URI has not been bound to a prefix and
   * javax.xml.stream.isRepairingNamespaces has not been set to true
   */
  public void writeEmptyElement(String namespaceURI, String localName)
    throws XMLStreamException;

  /**
   * Writes an empty element tag to the output
   * @param prefix the prefix of the tag, may not be null
   * @param localName local name of the tag, may not be null
   * @param namespaceURI the uri to bind the tag to, may not be null
   * @throws XMLStreamException if an error occurs
   */
  public void writeEmptyElement(String prefix, String localName, String namespaceURI)
    throws XMLStreamException;

  /**
   * Writes an empty element tag to the output
   * @param localName local name of the tag, may not be null
   * @throws XMLStreamException if an error occurs
   */
  public void writeEmptyElement(String localName)
    throws XMLStreamException;

  /**
   * Writes string data to the output without checking for well formedness.
   * The data is opaque to the XMLStreamWriter, i.e. the characters are written
   * blindly to the underlying output.  If the method cannot be supported
   * in the currrent writing context the implementation may throw a
   * UnsupportedOperationException.  For example note that any
   * namespace declarations, end tags, etc. will be ignored and could
   * interfere with proper maintanence of the writers internal state.
   *
   * @param data the data to write
   */
  //  public void writeRaw(String data) throws XMLStreamException;

  /**
   * Writes an end tag to the output relying on the internal
   * state of the writer to determine the prefix and local name
   * of the event.
   * @throws XMLStreamException if an error occurs
   */
  public void writeEndElement()
    throws XMLStreamException;

  /**
   * Closes any start tags and writes corresponding end tags.
   * @throws XMLStreamException if an error occurs
   */
  public void writeEndDocument()
    throws XMLStreamException;

  /**
   * Close this writer and free any resources associated with the
   * writer.  This must not close the underlying output stream.
   * @throws XMLStreamException if an error occurs
   */
  public void close()
    throws XMLStreamException;

  /**
   * Write any cached data to the underlying output mechanism.
   * @throws XMLStreamException if an error occurs
   */
  public void flush()
    throws XMLStreamException;

  /**
   * Writes an attribute to the output stream without
   * a prefix.
   * @param localName the local name of the attribute
   * @param value the value of the attribute
   * @throws IllegalStateException if the current state does not allow Attribute writing
   * @throws XMLStreamException if an error occurs
   */
  public void writeAttribute(String localName, String value)
    throws XMLStreamException;

  /**
   * Writes an attribute to the output stream
   * @param prefix the prefix for this attribute
   * @param namespaceURI the uri of the prefix for this attribute
   * @param localName the local name of the attribute
   * @param value the value of the attribute
   * @throws IllegalStateException if the current state does not allow Attribute writing
   * @throws XMLStreamException if the namespace URI has not been bound to a prefix and
   * javax.xml.stream.isRepairingNamespaces has not been set to true
   */

  public void writeAttribute(String prefix,
                             String namespaceURI,
                             String localName,
                             String value)
    throws XMLStreamException;

  /**
   * Writes an attribute to the output stream
   * @param namespaceURI the uri of the prefix for this attribute
   * @param localName the local name of the attribute
   * @param value the value of the attribute
   * @throws IllegalStateException if the current state does not allow Attribute writing
   * @throws XMLStreamException if the namespace URI has not been bound to a prefix and
   * javax.xml.stream.isRepairingNamespaces has not been set to true
   */
  public void writeAttribute(String namespaceURI,
                             String localName,
                             String value)
    throws XMLStreamException;

  /**
   * Writes a namespace to the output stream
   * If the prefix argument to this method is the empty string,
   * "xmlns", or null this method will delegate to writeDefaultNamespace
   *
   * @param prefix the prefix to bind this namespace to
   * @param namespaceURI the uri to bind the prefix to
   * @throws IllegalStateException if the current state does not allow Namespace writing
   * @throws XMLStreamException if an error occurs
   */
  public void writeNamespace(String prefix, String namespaceURI)
    throws XMLStreamException;

  /**
   * Writes the default namespace to the stream
   * @param namespaceURI the uri to bind the default namespace to
   * @throws IllegalStateException if the current state does not allow Namespace writing
   * @throws XMLStreamException if an error occurs
   */
  public void writeDefaultNamespace(String namespaceURI)
    throws XMLStreamException;

  /**
   * Writes an xml comment with the data enclosed
   * @param data the data contained in the comment, may be null
   * @throws XMLStreamException if an error occurs
   */
  public void writeComment(String data)
    throws XMLStreamException;

  /**
   * Writes a processing instruction
   * @param target the target of the processing instruction, may not be null
   * @throws XMLStreamException if an error occurs
   */
  public void writeProcessingInstruction(String target)
    throws XMLStreamException;

  /**
   * Writes a processing instruction
   * @param target the target of the processing instruction, may not be null
   * @param data the data contained in the processing instruction, may not be null
   * @throws XMLStreamException if an error occurs
   */
  public void writeProcessingInstruction(String target,
                                         String data)
    throws XMLStreamException;

  /**
   * Writes a CData section
   * @param data the data contained in the CData Section, may not be null
   * @throws XMLStreamException if an error occurs
   */
  public void writeCData(String data)
    throws XMLStreamException;

  /**
   * Write a DTD section.  This string represents the entire doctypedecl production
   * from the XML 1.0 specification.
   *
   * @param dtd the DTD to be written
   * @throws XMLStreamException if an error occurs
   */
  public void writeDTD(String dtd)
    throws XMLStreamException;

  /**
   * Writes an entity reference
   * @param name the name of the entity
   * @throws XMLStreamException if an error occurs
   */
  public void writeEntityRef(String name)
    throws XMLStreamException;

  /**
   * Write the XML Declaration. Defaults the XML version to 1.0, and the encoding to utf-8
   * @throws XMLStreamException if an error occurs
   */
  public void writeStartDocument()
    throws XMLStreamException;

  /**
   * Write the XML Declaration. Defaults the XML version to 1.0
   * @param version version of the xml document
   * @throws XMLStreamException if an error occurs
   */
  public void writeStartDocument(String version)
    throws XMLStreamException;

  /**
   * Write the XML Declaration.  Note that the encoding parameter does
   * not set the actual encoding of the underlying output.  That must
   * be set when the instance of the XMLStreamWriter is created using the
   * XMLOutputFactory
   * @param encoding encoding of the xml declaration
   * @param version version of the xml document
   * @throws XMLStreamException If given encoding does not match encoding
   * of the underlying stream
   */
  public void writeStartDocument(String encoding,
                                 String version)
    throws XMLStreamException;

  /**
   * Write text to the output
   * @param text the value to write
   * @throws XMLStreamException if an error occurs
   */
  public void writeCharacters(String text)
    throws XMLStreamException;

  /**
   * Write text to the output
   * @param text the value to write
   * @param start the starting position in the array
   * @param len the number of characters to write
   * @throws XMLStreamException if an error occurs
   */
  public void writeCharacters(char[] text, int start, int len)
    throws XMLStreamException;

  /**
   * Gets the prefix the uri is bound to.
   * @param uri the uri the prefix is bound to
   * @return the prefix or null
   * @throws XMLStreamException if an error occurs
   */
  public String getPrefix(String uri)
    throws XMLStreamException;

  /**
   * Sets the prefix the uri is bound to.  This prefix is bound
   * in the scope of the current START_ELEMENT / END_ELEMENT pair.
   * If this method is called before a START_ELEMENT has been written
   * the prefix is bound in the root scope.
   * @param prefix the prefix to bind to the uri, may not be null
   * @param uri the uri to bind to the prefix, may be null
   * @throws XMLStreamException if an error occurs
   */
  public void setPrefix(String prefix, String uri)
    throws XMLStreamException;


  /**
   * Binds a URI to the default namespace
   * This URI is bound
   * in the scope of the current START_ELEMENT / END_ELEMENT pair.
   * If this method is called before a START_ELEMENT has been written
   * the uri is bound in the root scope.
   * @param uri the uri to bind to the default namespace, may be null
   * @throws XMLStreamException if an error occurs
   */
  public void setDefaultNamespace(String uri)
    throws XMLStreamException;

  /**
   * Sets the current namespace context for prefix and uri bindings.
   * This context becomes the root namespace context for writing and
   * will replace the current root namespace context.  Subsequent calls
   * to setPrefix and setDefaultNamespace will bind namespaces using
   * the context passed to the method as the root context for resolving
   * namespaces.  This method may only be called once at the start of
   * the document.  It does not cause the namespaces to be declared.
   * If a namespace URI to prefix mapping is found in the namespace
   * context it is treated as declared and the prefix may be used
   * by the StreamWriter.
   * @param context the namespace context to use for this writer, may not be null
   * @throws XMLStreamException if an error occurs
   */
  public void setNamespaceContext(NamespaceContext context)
    throws XMLStreamException;

  /**
   * Returns the current namespace context.
   * @return the current NamespaceContext
   */
  public NamespaceContext getNamespaceContext();

  /**
   * Get the value of a feature/property from the underlying implementation
   * @param name The name of the property, may not be null
   * @return The value of the property
   * @throws IllegalArgumentException if the property is not supported
   * @throws NullPointerException if the name is null
   */
  public Object getProperty(java.lang.String name) throws IllegalArgumentException;

}
