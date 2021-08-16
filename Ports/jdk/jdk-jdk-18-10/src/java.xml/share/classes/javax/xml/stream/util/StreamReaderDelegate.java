/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.stream.util;

import javax.xml.namespace.QName;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 * This is the base class for deriving an XMLStreamReader filter
 *
 * This class is designed to sit between an XMLStreamReader and an
 * application's XMLStreamReader.   By default each method
 * does nothing but call the corresponding method on the
 * parent interface.
 *
 * @version 1.0
 * @author Copyright (c) 2009 by Oracle Corporation. All Rights Reserved.
 * @see javax.xml.stream.XMLStreamReader
 * @see EventReaderDelegate
 * @since 1.6
 */

public class StreamReaderDelegate implements XMLStreamReader {
  private XMLStreamReader reader;

  /**
   * Construct an empty filter with no parent.
   */
  public StreamReaderDelegate(){}

  /**
   * Construct an filter with the specified parent.
   * @param reader the parent
   */
  public StreamReaderDelegate(XMLStreamReader reader) {
    this.reader = reader;
  }

  /**
   * Set the parent of this instance.
   * @param reader the new parent
   */
  public void setParent(XMLStreamReader reader) {
    this.reader = reader;
  }

  /**
   * Get the parent of this instance.
   * @return the parent or null if none is set
   */
  public XMLStreamReader getParent() {
    return reader;
  }

  public int next()
    throws XMLStreamException
  {
    return reader.next();
  }

  public int nextTag()
    throws XMLStreamException
  {
    return reader.nextTag();
  }

  public String getElementText()
    throws XMLStreamException
  {
    return reader.getElementText();
  }

  public void require(int type, String namespaceURI, String localName)
    throws XMLStreamException
  {
    reader.require(type,namespaceURI,localName);
  }

  public boolean hasNext()
    throws XMLStreamException
  {
    return reader.hasNext();
  }

  public void close()
    throws XMLStreamException
  {
    reader.close();
  }

  public String getNamespaceURI(String prefix)
  {
    return reader.getNamespaceURI(prefix);
  }

  public NamespaceContext getNamespaceContext() {
    return reader.getNamespaceContext();
  }

  public boolean isStartElement() {
    return reader.isStartElement();
  }

  public boolean isEndElement() {
    return reader.isEndElement();
  }

  public boolean isCharacters() {
    return reader.isCharacters();
  }

  public boolean isWhiteSpace() {
    return reader.isWhiteSpace();
  }

  public String getAttributeValue(String namespaceUri,
                                  String localName)
  {
    return reader.getAttributeValue(namespaceUri,localName);
  }

  public int getAttributeCount() {
    return reader.getAttributeCount();
  }

  public QName getAttributeName(int index) {
    return reader.getAttributeName(index);
  }

  public String getAttributePrefix(int index) {
    return reader.getAttributePrefix(index);
  }

  public String getAttributeNamespace(int index) {
    return reader.getAttributeNamespace(index);
  }

  public String getAttributeLocalName(int index) {
    return reader.getAttributeLocalName(index);
  }

  public String getAttributeType(int index) {
    return reader.getAttributeType(index);
  }

  public String getAttributeValue(int index) {
    return reader.getAttributeValue(index);
  }

  public boolean isAttributeSpecified(int index) {
    return reader.isAttributeSpecified(index);
  }

  public int getNamespaceCount() {
    return reader.getNamespaceCount();
  }

  public String getNamespacePrefix(int index) {
    return reader.getNamespacePrefix(index);
  }

  public String getNamespaceURI(int index) {
    return reader.getNamespaceURI(index);
  }

  public int getEventType() {
    return reader.getEventType();
  }

  public String getText() {
    return reader.getText();
  }

  public int getTextCharacters(int sourceStart,
                               char[] target,
                               int targetStart,
                               int length)
    throws XMLStreamException {
    return reader.getTextCharacters(sourceStart,
                                    target,
                                    targetStart,
                                    length);
  }


  public char[] getTextCharacters() {
    return reader.getTextCharacters();
  }

  public int getTextStart() {
    return reader.getTextStart();
  }

  public int getTextLength() {
    return reader.getTextLength();
  }

  public String getEncoding() {
    return reader.getEncoding();
  }

  public boolean hasText() {
    return reader.hasText();
  }

  public Location getLocation() {
    return reader.getLocation();
  }

  public QName getName() {
    return reader.getName();
  }

  public String getLocalName() {
    return reader.getLocalName();
  }

  public boolean hasName() {
    return reader.hasName();
  }

  public String getNamespaceURI() {
    return reader.getNamespaceURI();
  }

  public String getPrefix() {
    return reader.getPrefix();
  }

  public String getVersion() {
    return reader.getVersion();
  }

  public boolean isStandalone() {
    return reader.isStandalone();
  }

  public boolean standaloneSet() {
    return reader.standaloneSet();
  }

  public String getCharacterEncodingScheme() {
    return reader.getCharacterEncodingScheme();
  }

  public String getPITarget() {
    return reader.getPITarget();
  }

  public String getPIData() {
    return reader.getPIData();
  }

  public Object getProperty(String name) {
    return reader.getProperty(name);
  }
}
