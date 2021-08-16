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
/*
 * $Id: UnImplNode.java,v
 */

package com.sun.org.apache.xml.internal.utils;

import com.sun.org.apache.xml.internal.res.XMLErrorResources;
import com.sun.org.apache.xml.internal.res.XMLMessages;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

import org.w3c.dom.UserDataHandler;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.TypeInfo;
/**
 * <meta name="usage" content="internal"/>
 * To be subclassed by classes that wish to fake being nodes.
 */
public class UnImplNode implements Node, Element, NodeList, Document
{

  /**
   * Constructor UnImplNode
   *
   */
  public UnImplNode(){}

  /**
   * Throw an error.
   *
   * @param msg Message Key for the error
   */
  public void error(String msg)
  {

    System.out.println("DOM ERROR! class: " + this.getClass().getName());

    throw new RuntimeException(XMLMessages.createXMLMessage(msg, null));
  }

  /**
   * Throw an error.
   *
   * @param msg Message Key for the error
   * @param args Array of arguments to be used in the error message
   */
  public void error(String msg, Object[] args)
  {

    System.out.println("DOM ERROR! class: " + this.getClass().getName());

    throw new RuntimeException(XMLMessages.createXMLMessage(msg, args));  //"UnImplNode error: "+msg);
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @param newChild New node to append to the list of this node's children
   *
   * @return null
   *
   * @throws DOMException
   */
  public Node appendChild(Node newChild) throws DOMException
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"appendChild not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @return false
   */
  public boolean hasChildNodes()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"hasChildNodes not supported!");

    return false;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @return 0
   */
  public short getNodeType()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getNodeType not supported!");

    return 0;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @return null
   */
  public Node getParentNode()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getParentNode not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @return null
   */
  public NodeList getChildNodes()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getChildNodes not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @return null
   */
  public Node getFirstChild()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getFirstChild not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @return null
   */
  public Node getLastChild()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getLastChild not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @return null
   */
  public Node getNextSibling()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getNextSibling not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.NodeList
   *
   * @return 0
   */
  public int getLength()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getLength not supported!");

    return 0;
  }  // getLength():int

  /**
   * Unimplemented. See org.w3c.dom.NodeList
   *
   * @param index index of a child of this node in its list of children
   *
   * @return null
   */
  public Node item(int index)
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"item not supported!");

    return null;
  }  // item(int):Node

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @return null
   */
  public Document getOwnerDocument()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getOwnerDocument not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @return null
   */
  public String getTagName()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getTagName not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @return null
   */
  public String getNodeName()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getNodeName not supported!");

    return null;
  }

  /** Unimplemented. See org.w3c.dom.Node */
  public void normalize()
  {
    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"normalize not supported!");
  }

  /**
   * Unimplemented. See org.w3c.dom.Element
   *
   * @param name Name of the element
   *
   * @return null
   */
  public NodeList getElementsByTagName(String name)
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getElementsByTagName not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Element
   *
   * @param oldAttr Attribute to be removed from this node's list of attributes
   *
   * @return null
   *
   * @throws DOMException
   */
  public Attr removeAttributeNode(Attr oldAttr) throws DOMException
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"removeAttributeNode not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Element
   *
   * @param newAttr Attribute node to be added to this node's list of attributes
   *
   * @return null
   *
   * @throws DOMException
   */
  public Attr setAttributeNode(Attr newAttr) throws DOMException
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"setAttributeNode not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Element
   *
   *
   * @param name Name of an attribute
   *
   * @return false
   */
  public boolean hasAttribute(String name)
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"hasAttribute not supported!");

    return false;
  }

  /**
   * Unimplemented. See org.w3c.dom.Element
   *
   *
   * @param name
   * @param x
   *
   * @return false
   */
  public boolean hasAttributeNS(String name, String x)
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"hasAttributeNS not supported!");

    return false;
  }

  /**
   * Unimplemented. See org.w3c.dom.Element
   *
   *
   * @param name Attribute node name
   *
   * @return null
   */
  public Attr getAttributeNode(String name)
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getAttributeNode not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Element
   *
   * @param name Attribute node name to remove from list of attributes
   *
   * @throws DOMException
   */
  public void removeAttribute(String name) throws DOMException
  {
    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"removeAttribute not supported!");
  }

  /**
   * Unimplemented. See org.w3c.dom.Element
   *
   * @param name Name of attribute to set
   * @param value Value of attribute
   *
   * @throws DOMException
   */
  public void setAttribute(String name, String value) throws DOMException
  {
    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"setAttribute not supported!");
  }

  /**
   * Unimplemented. See org.w3c.dom.Element
   *
   * @param name Name of attribute to get
   *
   * @return null
   */
  public String getAttribute(String name)
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getAttribute not supported!");

    return null;
  }

  /**
   * Unimplemented. Introduced in DOM Level 2.
   *
   * @return false
   */
  public boolean hasAttributes()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"hasAttributes not supported!");

    return false;
  }

  /**
   * Unimplemented. See org.w3c.dom.Element
   *
   * @param namespaceURI Namespace URI of the element
   * @param localName Local part of qualified name of the element
   *
   * @return null
   */
  public NodeList getElementsByTagNameNS(String namespaceURI,
                                         String localName)
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getElementsByTagNameNS not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Element
   *
   * @param newAttr Attribute to set
   *
   * @return null
   *
   * @throws DOMException
   */
  public Attr setAttributeNodeNS(Attr newAttr) throws DOMException
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"setAttributeNodeNS not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Element
   *
   * @param namespaceURI Namespace URI of attribute node to get
   * @param localName Local part of qualified name of attribute node to get
   *
   * @return null
   */
  public Attr getAttributeNodeNS(String namespaceURI, String localName)
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getAttributeNodeNS not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Element
   *
   * @param namespaceURI Namespace URI of attribute node to remove
   * @param localName Local part of qualified name of attribute node to remove
   *
   * @throws DOMException
   */
  public void removeAttributeNS(String namespaceURI, String localName)
          throws DOMException
  {
    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"removeAttributeNS not supported!");
  }

  /**
   * Unimplemented. See org.w3c.dom.Element
   *
   * @param namespaceURI Namespace URI of attribute node to set
   * @param qualifiedName qualified name of attribute
   * @param value value of attribute
   *
   * @throws DOMException
   */
  public void setAttributeNS(
          String namespaceURI, String qualifiedName, String value)
            throws DOMException
  {
    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"setAttributeNS not supported!");
  }

  /**
   * Unimplemented. See org.w3c.dom.Element
   *
   * @param namespaceURI Namespace URI of attribute node to get
   * @param localName Local part of qualified name of attribute node to get
   *
   * @return null
   */
  public String getAttributeNS(String namespaceURI, String localName)
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getAttributeNS not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @return null
   */
  public Node getPreviousSibling()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getPreviousSibling not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @param deep Flag indicating whether to clone deep (clone member variables)
   *
   * @return null
   */
  public Node cloneNode(boolean deep)
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"cloneNode not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @return null
   *
   * @throws DOMException
   */
  public String getNodeValue() throws DOMException
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getNodeValue not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @param nodeValue Value to set this node to
   *
   * @throws DOMException
   */
  public void setNodeValue(String nodeValue) throws DOMException
  {
    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"setNodeValue not supported!");
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   *
   * NEEDSDOC @param value
   * @return value Node value
   *
   * @throws DOMException
   */

  // public String getValue ()
  // {
  //  error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED); //"getValue not supported!");
  //  return null;
  // }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @param value Value to set this node to
   *
   * @throws DOMException
   */
  public void setValue(String value) throws DOMException
  {
    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"setValue not supported!");
  }

  /**
   *  Returns the name of this attribute.
   *
   * @return the name of this attribute.
   */

  // public String getName()
  // {
  //  return this.getNodeName();
  // }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @return null
   */
  public Element getOwnerElement()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getOwnerElement not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @return False
   */
  public boolean getSpecified()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"setValue not supported!");

    return false;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @return null
   */
  public NamedNodeMap getAttributes()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getAttributes not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @param newChild New child node to insert
   * @param refChild Insert in front of this child
   *
   * @return null
   *
   * @throws DOMException
   */
  public Node insertBefore(Node newChild, Node refChild) throws DOMException
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"insertBefore not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @param newChild Replace existing child with this one
   * @param oldChild Existing child to be replaced
   *
   * @return null
   *
   * @throws DOMException
   */
  public Node replaceChild(Node newChild, Node oldChild) throws DOMException
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"replaceChild not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @param oldChild Child to be removed
   *
   * @return null
   *
   * @throws DOMException
   */
  public Node removeChild(Node oldChild) throws DOMException
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"replaceChild not supported!");

    return null;
  }

  /**
   * Tests whether the DOM implementation implements a specific feature and
   * that feature is supported by this node.
   * @param feature The name of the feature to test. This is the same name
   *   which can be passed to the method <code>hasFeature</code> on
   *   <code>DOMImplementation</code>.
   * @param version This is the version number of the feature to test. In
   *   Level 2, version 1, this is the string "2.0". If the version is not
   *   specified, supporting any version of the feature will cause the
   *   method to return <code>true</code>.
   *
   * @return Returns <code>false</code>
   * @since DOM Level 2
   */
  public boolean isSupported(String feature, String version)
  {
    return false;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @return null
   */
  public String getNamespaceURI()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getNamespaceURI not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @return null
   */
  public String getPrefix()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getPrefix not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @param prefix Prefix to set for this node
   *
   * @throws DOMException
   */
  public void setPrefix(String prefix) throws DOMException
  {
    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"setPrefix not supported!");
  }

  /**
   * Unimplemented. See org.w3c.dom.Node
   *
   * @return null
   */
  public String getLocalName()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);  //"getLocalName not supported!");

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Document
   *
   * @return null
   */
  public DocumentType getDoctype()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Document
   *
   * @return null
   */
  public DOMImplementation getImplementation()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Document
   *
   * @return null
   */
  public Element getDocumentElement()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Document
   *
   * @param tagName Element tag name
   *
   * @return null
   *
   * @throws DOMException
   */
  public Element createElement(String tagName) throws DOMException
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Document
   *
   * @return null
   */
  public DocumentFragment createDocumentFragment()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Document
   *
   * @param data Data for text node
   *
   * @return null
   */
  public Text createTextNode(String data)
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Document
   *
   * @param data Data for comment
   *
   * @return null
   */
  public Comment createComment(String data)
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Document
   *
   * @param data Data for CDATA section
   *
   * @return null
   *
   * @throws DOMException
   */
  public CDATASection createCDATASection(String data) throws DOMException
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Document
   *
   * @param target Target for Processing instruction
   * @param data Data for Processing instruction
   *
   * @return null
   *
   * @throws DOMException
   */
  public ProcessingInstruction createProcessingInstruction(
          String target, String data) throws DOMException
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Document
   *
   * @param name Attribute name
   *
   * @return null
   *
   * @throws DOMException
   */
  public Attr createAttribute(String name) throws DOMException
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Document
   *
   * @param name Entity Reference name
   *
   * @return null
   *
   * @throws DOMException
   */
  public EntityReference createEntityReference(String name)
          throws DOMException
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Document
   *
   * @param importedNode The node to import.
   * @param deep If <code>true</code>, recursively import the subtree under
   *   the specified node; if <code>false</code>, import only the node
   *   itself, as explained above. This has no effect on <code>Attr</code>
   *   , <code>EntityReference</code>, and <code>Notation</code> nodes.
   *
   * @return null
   *
   * @throws DOMException
   */
  public Node importNode(Node importedNode, boolean deep) throws DOMException
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Document
   *
   * @param namespaceURI Namespace URI for the element
   * @param qualifiedName Qualified name of the element
   *
   * @return null
   *
   * @throws DOMException
   */
  public Element createElementNS(String namespaceURI, String qualifiedName)
          throws DOMException
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Document
   *
   * @param namespaceURI Namespace URI of the attribute
   * @param qualifiedName Qualified name of the attribute
   *
   * @return null
   *
   * @throws DOMException
   */
  public Attr createAttributeNS(String namespaceURI, String qualifiedName)
          throws DOMException
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * Unimplemented. See org.w3c.dom.Document
   *
   * @param elementId ID of the element to get
   *
   * @return null
   */
  public Element getElementById(String elementId)
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * Set Node data
   *
   *
   * @param data data to set for this node
   *
   * @throws DOMException
   */
  public void setData(String data) throws DOMException
  {
    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);
  }

  /**
   * Unimplemented.
   *
   * @param offset Start offset of substring to extract.
   * @param count The length of the substring to extract.
   *
   * @return null
   *
   * @throws DOMException
   */
  public String substringData(int offset, int count) throws DOMException
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * Unimplemented.
   *
   * @param arg String data to append
   *
   * @throws DOMException
   */
  public void appendData(String arg) throws DOMException
  {
    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);
  }

  /**
   * Unimplemented.
   *
   * @param offset Start offset of substring to insert.
   * @param arg The (sub)string to insert.
   *
   * @throws DOMException
   */
  public void insertData(int offset, String arg) throws DOMException
  {
    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);
  }

  /**
   * Unimplemented.
   *
   * @param offset Start offset of substring to delete.
   * @param count The length of the substring to delete.
   *
   * @throws DOMException
   */
  public void deleteData(int offset, int count) throws DOMException
  {
    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);
  }

  /**
   * Unimplemented.
   *
   * @param offset Start offset of substring to replace.
   * @param count The length of the substring to replace.
   * @param arg substring to replace with
   *
   * @throws DOMException
   */
  public void replaceData(int offset, int count, String arg)
          throws DOMException
  {
    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);
  }

  /**
   * Unimplemented.
   *
   * @param offset Offset into text to split
   *
   * @return null, unimplemented
   *
   * @throws DOMException
   */
  public Text splitText(int offset) throws DOMException
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * NEEDSDOC Method adoptNode
   *
   *
   * NEEDSDOC @param source
   *
   * NEEDSDOC (adoptNode) @return
   *
   * @throws DOMException
   */
  public Node adoptNode(Node source) throws DOMException
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * <p>EXPERIMENTAL! Based on the <a
   * href='http://www.w3.org/TR/2001/WD-DOM-Level-3-Core-20010605'>Document
   * Object Model (DOM) Level 3 Core Working Draft of 5 June 2001.</a>.
   * <p>
   * An attribute specifying, as part of the XML declaration, the encoding
   * of this document. This is <code>null</code> when unspecified.
   * @since DOM Level 3
   *
   * NEEDSDOC ($objectName$) @return
   */
  public String getInputEncoding()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * <p>EXPERIMENTAL! Based on the <a
   * href='http://www.w3.org/TR/2001/WD-DOM-Level-3-Core-20010605'>Document
   * Object Model (DOM) Level 3 Core Working Draft of 5 June 2001.</a>.
   * <p>
   * An attribute specifying, as part of the XML declaration, the encoding
   * of this document. This is <code>null</code> when unspecified.
   * @since DOM Level 3
   *
   * NEEDSDOC @param encoding
   */
  public void setInputEncoding(String encoding)
  {
    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);
  }

  /**
   * <p>EXPERIMENTAL! Based on the <a
   * href='http://www.w3.org/TR/2001/WD-DOM-Level-3-Core-20010605'>Document
   * Object Model (DOM) Level 3 Core Working Draft of 5 June 2001.</a>.
   * <p>
   * An attribute specifying, as part of the XML declaration, whether this
   * document is standalone.
   * @since DOM Level 3
   *
   * NEEDSDOC ($objectName$) @return
   */
  public boolean getStandalone()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return false;
  }

  /**
   * <p>EXPERIMENTAL! Based on the <a
   * href='http://www.w3.org/TR/2001/WD-DOM-Level-3-Core-20010605'>Document
   * Object Model (DOM) Level 3 Core Working Draft of 5 June 2001.</a>.
   * <p>
   * An attribute specifying, as part of the XML declaration, whether this
   * document is standalone.
   * @since DOM Level 3
   *
   * NEEDSDOC @param standalone
   */
  public void setStandalone(boolean standalone)
  {
    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);
  }

  /**
   * <p>EXPERIMENTAL! Based on the <a
   * href='http://www.w3.org/TR/2001/WD-DOM-Level-3-Core-20010605'>Document
   * Object Model (DOM) Level 3 Core Working Draft of 5 June 2001.</a>.
   * <p>
   * An attribute specifying whether errors checking is enforced or not.
   * When set to <code>false</code>, the implementation is free to not
   * test every possible error case normally defined on DOM operations,
   * and not raise any <code>DOMException</code>. In case of error, the
   * behavior is undefined. This attribute is <code>true</code> by
   * defaults.
   * @since DOM Level 3
   *
   * NEEDSDOC ($objectName$) @return
   */
  public boolean getStrictErrorChecking()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return false;
  }

  /**
   * <p>EXPERIMENTAL! Based on the <a
   * href='http://www.w3.org/TR/2001/WD-DOM-Level-3-Core-20010605'>Document
   * Object Model (DOM) Level 3 Core Working Draft of 5 June 2001.</a>.
   * <p>
   * An attribute specifying whether errors checking is enforced or not.
   * When set to <code>false</code>, the implementation is free to not
   * test every possible error case normally defined on DOM operations,
   * and not raise any <code>DOMException</code>. In case of error, the
   * behavior is undefined. This attribute is <code>true</code> by
   * defaults.
   * @since DOM Level 3
   *
   * NEEDSDOC @param strictErrorChecking
   */
  public void setStrictErrorChecking(boolean strictErrorChecking)
  {
    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);
  }

  /**
   * <p>EXPERIMENTAL! Based on the <a
   * href='http://www.w3.org/TR/2001/WD-DOM-Level-3-Core-20010605'>Document
   * Object Model (DOM) Level 3 Core Working Draft of 5 June 2001.</a>.
   * <p>
   * An attribute specifying, as part of the XML declaration, the version
   * number of this document. This is <code>null</code> when unspecified.
   * @since DOM Level 3
   *
   * NEEDSDOC ($objectName$) @return
   */
  public String getVersion()
  {

    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);

    return null;
  }

  /**
   * <p>EXPERIMENTAL! Based on the <a
   * href='http://www.w3.org/TR/2001/WD-DOM-Level-3-Core-20010605'>Document
   * Object Model (DOM) Level 3 Core Working Draft of 5 June 2001.</a>.
   * <p>
   * An attribute specifying, as part of the XML declaration, the version
   * number of this document. This is <code>null</code> when unspecified.
   * @since DOM Level 3
   *
   * NEEDSDOC @param version
   */
  public void setVersion(String version)
  {
    error(XMLErrorResources.ER_FUNCTION_NOT_SUPPORTED);
  }



//RAMESH : Pending proper implementation of DOM Level 3

    public Object setUserData(String key,
                              Object data,
                              UserDataHandler handler) {
        return getOwnerDocument().setUserData( key, data, handler);
    }

    /**
     * Retrieves the object associated to a key on a this node. The object
     * must first have been set to this node by calling
     * <code>setUserData</code> with the same key.
     * @param key The key the object is associated to.
     * @return Returns the <code>DOMObject</code> associated to the given key
     *   on this node, or <code>null</code> if there was none.
     * @since DOM Level 3
     */
    public Object getUserData(String key) {
        return getOwnerDocument().getUserData( key);
    }

      /**
     *  This method returns a specialized object which implements the
     * specialized APIs of the specified feature and version. The
     * specialized object may also be obtained by using binding-specific
     * casting methods but is not necessarily expected to, as discussed in Mixed DOM implementations.
     * @param feature The name of the feature requested (case-insensitive).
     * @param version  This is the version number of the feature to test. If
     *   the version is <code>null</code> or the empty string, supporting
     *   any version of the feature will cause the method to return an
     *   object that supports at least one version of the feature.
     * @return  Returns an object which implements the specialized APIs of
     *   the specified feature and version, if any, or <code>null</code> if
     *   there is no object which implements interfaces associated with that
     *   feature. If the <code>DOMObject</code> returned by this method
     *   implements the <code>Node</code> interface, it must delegate to the
     *   primary core <code>Node</code> and not return results inconsistent
     *   with the primary core <code>Node</code> such as attributes,
     *   childNodes, etc.
     * @since DOM Level 3
     */
    public Object getFeature(String feature, String version) {
        // we don't have any alternate node, either this node does the job
        // or we don't have anything that does
        return isSupported(feature, version) ? this : null;
    }

    /**
     * Tests whether two nodes are equal.
     * <br>This method tests for equality of nodes, not sameness (i.e.,
     * whether the two nodes are references to the same object) which can be
     * tested with <code>Node.isSameNode</code>. All nodes that are the same
     * will also be equal, though the reverse may not be true.
     * <br>Two nodes are equal if and only if the following conditions are
     * satisfied: The two nodes are of the same type.The following string
     * attributes are equal: <code>nodeName</code>, <code>localName</code>,
     * <code>namespaceURI</code>, <code>prefix</code>, <code>nodeValue</code>
     * , <code>baseURI</code>. This is: they are both <code>null</code>, or
     * they have the same length and are character for character identical.
     * The <code>attributes</code> <code>NamedNodeMaps</code> are equal.
     * This is: they are both <code>null</code>, or they have the same
     * length and for each node that exists in one map there is a node that
     * exists in the other map and is equal, although not necessarily at the
     * same index.The <code>childNodes</code> <code>NodeLists</code> are
     * equal. This is: they are both <code>null</code>, or they have the
     * same length and contain equal nodes at the same index. This is true
     * for <code>Attr</code> nodes as for any other type of node. Note that
     * normalization can affect equality; to avoid this, nodes should be
     * normalized before being compared.
     * <br>For two <code>DocumentType</code> nodes to be equal, the following
     * conditions must also be satisfied: The following string attributes
     * are equal: <code>publicId</code>, <code>systemId</code>,
     * <code>internalSubset</code>.The <code>entities</code>
     * <code>NamedNodeMaps</code> are equal.The <code>notations</code>
     * <code>NamedNodeMaps</code> are equal.
     * <br>On the other hand, the following do not affect equality: the
     * <code>ownerDocument</code> attribute, the <code>specified</code>
     * attribute for <code>Attr</code> nodes, the
     * <code>isWhitespaceInElementContent</code> attribute for
     * <code>Text</code> nodes, as well as any user data or event listeners
     * registered on the nodes.
     *
     * @param arg The node to compare equality with.
     * @return If the nodes, and possibly subtrees are equal,
     *   <code>true</code> otherwise <code>false</code>.
     * @since DOM Level 3
     */
    public boolean isEqualNode(Node arg) {
        if (arg == this) {
            return true;
        }
        if (arg.getNodeType() != getNodeType()) {
            return false;
        }
        // in theory nodeName can't be null but better be careful
        // who knows what other implementations may be doing?...
        if (getNodeName() == null) {
            if (arg.getNodeName() != null) {
                return false;
            }
        }
        else if (!getNodeName().equals(arg.getNodeName())) {
            return false;
        }

        if (getLocalName() == null) {
            if (arg.getLocalName() != null) {
                return false;
            }
        }
        else if (!getLocalName().equals(arg.getLocalName())) {
            return false;
        }

        if (getNamespaceURI() == null) {
            if (arg.getNamespaceURI() != null) {
                return false;
            }
        }
        else if (!getNamespaceURI().equals(arg.getNamespaceURI())) {
            return false;
        }

        if (getPrefix() == null) {
            if (arg.getPrefix() != null) {
                return false;
            }
        }
        else if (!getPrefix().equals(arg.getPrefix())) {
            return false;
        }

        if (getNodeValue() == null) {
            if (arg.getNodeValue() != null) {
                return false;
            }
        }
        else if (!getNodeValue().equals(arg.getNodeValue())) {
            return false;
        }
    /*
        if (getBaseURI() == null) {
            if (((NodeImpl) arg).getBaseURI() != null) {
                return false;
            }
        }
        else if (!getBaseURI().equals(((NodeImpl) arg).getBaseURI())) {
            return false;
        }
*/

             return true;
    }

      /**
     * DOM Level 3 - Experimental:
     * Look up the namespace URI associated to the given prefix, starting from this node.
     * Use lookupNamespaceURI(null) to lookup the default namespace
     *
     * @param namespaceURI
     * @return th URI for the namespace
     * @since DOM Level 3
     */
    public String lookupNamespaceURI(String specifiedPrefix) {
        short type = this.getNodeType();
        switch (type) {
        case Node.ELEMENT_NODE : {

                String namespace = this.getNamespaceURI();
                String prefix = this.getPrefix();
                if (namespace !=null) {
                    // REVISIT: is it possible that prefix is empty string?
                    if (specifiedPrefix== null && prefix==specifiedPrefix) {
                        // looking for default namespace
                        return namespace;
                    } else if (prefix != null && prefix.equals(specifiedPrefix)) {
                        // non default namespace
                        return namespace;
                    }
                }
                if (this.hasAttributes()) {
                    NamedNodeMap map = this.getAttributes();
                    int length = map.getLength();
                    for (int i=0;i<length;i++) {
                        Node attr = map.item(i);
                        String attrPrefix = attr.getPrefix();
                        String value = attr.getNodeValue();
                        namespace = attr.getNamespaceURI();
                        if (namespace !=null && namespace.equals("http://www.w3.org/2000/xmlns/")) {
                            // at this point we are dealing with DOM Level 2 nodes only
                            if (specifiedPrefix == null &&
                                attr.getNodeName().equals("xmlns")) {
                                // default namespace
                                return value;
                            } else if (attrPrefix !=null &&
                                       attrPrefix.equals("xmlns") &&
                                       attr.getLocalName().equals(specifiedPrefix)) {
                 // non default namespace
                                return value;
                            }
                        }
                    }
                }
                /*
                NodeImpl ancestor = (NodeImpl)getElementAncestor(this);
                if (ancestor != null) {
                    return ancestor.lookupNamespaceURI(specifiedPrefix);
                }
                */

                return null;


            }
/*
        case Node.DOCUMENT_NODE : {
                return((NodeImpl)((Document)this).getDocumentElement()).lookupNamespaceURI(specifiedPrefix) ;
            }
*/
        case Node.ENTITY_NODE :
        case Node.NOTATION_NODE:
        case Node.DOCUMENT_FRAGMENT_NODE:
        case Node.DOCUMENT_TYPE_NODE:
            // type is unknown
            return null;
        case Node.ATTRIBUTE_NODE:{
                if (this.getOwnerElement().getNodeType() == Node.ELEMENT_NODE) {
                    return getOwnerElement().lookupNamespaceURI(specifiedPrefix);

                }
                return null;
            }
        default:{
           /*
                NodeImpl ancestor = (NodeImpl)getElementAncestor(this);
                if (ancestor != null) {
                    return ancestor.lookupNamespaceURI(specifiedPrefix);
                }
             */
                return null;
            }

        }
    }


    /**
     *  DOM Level 3: Experimental
     *  This method checks if the specified <code>namespaceURI</code> is the
     *  default namespace or not.
     *  @param namespaceURI The namespace URI to look for.
     *  @return  <code>true</code> if the specified <code>namespaceURI</code>
     *   is the default namespace, <code>false</code> otherwise.
     * @since DOM Level 3
     */
    public boolean isDefaultNamespace(String namespaceURI){
       /*
        // REVISIT: remove casts when DOM L3 becomes REC.
        short type = this.getNodeType();
        switch (type) {
        case Node.ELEMENT_NODE: {
            String namespace = this.getNamespaceURI();
            String prefix = this.getPrefix();

            // REVISIT: is it possible that prefix is empty string?
            if (prefix == null || prefix.length() == 0) {
                if (namespaceURI == null) {
                    return (namespace == namespaceURI);
                }
                return namespaceURI.equals(namespace);
            }
            if (this.hasAttributes()) {
                ElementImpl elem = (ElementImpl)this;
                NodeImpl attr = (NodeImpl)elem.getAttributeNodeNS("http://www.w3.org/2000/xmlns/", "xmlns");
                if (attr != null) {
                    String value = attr.getNodeValue();
                    if (namespaceURI == null) {
                        return (namespace == value);
                    }
                    return namespaceURI.equals(value);
                }
            }

            NodeImpl ancestor = (NodeImpl)getElementAncestor(this);
            if (ancestor != null) {
                return ancestor.isDefaultNamespace(namespaceURI);
            }
            return false;
        }
        case Node.DOCUMENT_NODE:{
                return((NodeImpl)((Document)this).getDocumentElement()).isDefaultNamespace(namespaceURI);
            }

        case Node.ENTITY_NODE :
          case Node.NOTATION_NODE:
        case Node.DOCUMENT_FRAGMENT_NODE:
        case Node.DOCUMENT_TYPE_NODE:
            // type is unknown
            return false;
        case Node.ATTRIBUTE_NODE:{
                if (this.ownerNode.getNodeType() == Node.ELEMENT_NODE) {
                    return ownerNode.isDefaultNamespace(namespaceURI);

                }
                return false;
            }
        default:{
                NodeImpl ancestor = (NodeImpl)getElementAncestor(this);
                if (ancestor != null) {
                    return ancestor.isDefaultNamespace(namespaceURI);
                }
                return false;
            }

        }
*/
        return false;


    }

      /**
     *
     * DOM Level 3 - Experimental:
     * Look up the prefix associated to the given namespace URI, starting from this node.
     *
     * @param namespaceURI
     * @return the prefix for the namespace
     */
    public String lookupPrefix(String namespaceURI){

        // REVISIT: When Namespaces 1.1 comes out this may not be true
        // Prefix can't be bound to null namespace
        if (namespaceURI == null) {
            return null;
        }

        short type = this.getNodeType();

        switch (type) {
/*
        case Node.ELEMENT_NODE: {

                String namespace = this.getNamespaceURI(); // to flip out children
                return lookupNamespacePrefix(namespaceURI, (ElementImpl)this);
            }

        case Node.DOCUMENT_NODE:{
                return((NodeImpl)((Document)this).getDocumentElement()).lookupPrefix(namespaceURI);
            }
*/
        case Node.ENTITY_NODE :
        case Node.NOTATION_NODE:
        case Node.DOCUMENT_FRAGMENT_NODE:
        case Node.DOCUMENT_TYPE_NODE:
            // type is unknown
            return null;
        case Node.ATTRIBUTE_NODE:{
                if (this.getOwnerElement().getNodeType() == Node.ELEMENT_NODE) {
                    return getOwnerElement().lookupPrefix(namespaceURI);

                }
                return null;
            }
        default:{
/*
                NodeImpl ancestor = (NodeImpl)getElementAncestor(this);
                if (ancestor != null) {
                    return ancestor.lookupPrefix(namespaceURI);
                }
*/
                return null;
            }
         }
    }

     /**
     * Returns whether this node is the same node as the given one.
     * <br>This method provides a way to determine whether two
     * <code>Node</code> references returned by the implementation reference
     * the same object. When two <code>Node</code> references are references
     * to the same object, even if through a proxy, the references may be
     * used completely interchangably, such that all attributes have the
     * same values and calling the same DOM method on either reference
     * always has exactly the same effect.
     * @param other The node to test against.
     * @return Returns <code>true</code> if the nodes are the same,
     *   <code>false</code> otherwise.
     * @since DOM Level 3
     */
    public boolean isSameNode(Node other) {
        // we do not use any wrapper so the answer is obvious
        return this == other;
    }

      /**
     * This attribute returns the text content of this node and its
     * descendants. When it is defined to be null, setting it has no effect.
     * When set, any possible children this node may have are removed and
     * replaced by a single <code>Text</code> node containing the string
     * this attribute is set to. On getting, no serialization is performed,
     * the returned string does not contain any markup. No whitespace
     * normalization is performed, the returned string does not contain the
     * element content whitespaces . Similarly, on setting, no parsing is
     * performed either, the input string is taken as pure textual content.
     * <br>The string returned is made of the text content of this node
     * depending on its type, as defined below:
     * <table border='1'>
     * <tr>
     * <th>Node type</th>
     * <th>Content</th>
     * </tr>
     * <tr>
     * <td valign='top' rowspan='1' colspan='1'>
     * ELEMENT_NODE, ENTITY_NODE, ENTITY_REFERENCE_NODE,
     * DOCUMENT_FRAGMENT_NODE</td>
     * <td valign='top' rowspan='1' colspan='1'>concatenation of the <code>textContent</code>
     * attribute value of every child node, excluding COMMENT_NODE and
     * PROCESSING_INSTRUCTION_NODE nodes</td>
     * </tr>
     * <tr>
     * <td valign='top' rowspan='1' colspan='1'>ATTRIBUTE_NODE, TEXT_NODE,
     * CDATA_SECTION_NODE, COMMENT_NODE, PROCESSING_INSTRUCTION_NODE</td>
     * <td valign='top' rowspan='1' colspan='1'>
     * <code>nodeValue</code></td>
     * </tr>
     * <tr>
     * <td valign='top' rowspan='1' colspan='1'>DOCUMENT_NODE, DOCUMENT_TYPE_NODE, NOTATION_NODE</td>
     * <td valign='top' rowspan='1' colspan='1'>
     * null</td>
     * </tr>
     * </table>
     * @exception DOMException
     *   NO_MODIFICATION_ALLOWED_ERR: Raised when the node is readonly.
     * @exception DOMException
     *   DOMSTRING_SIZE_ERR: Raised when it would return more characters than
     *   fit in a <code>DOMString</code> variable on the implementation
     *   platform.
       * @since DOM Level 3
     */
    public void setTextContent(String textContent)
        throws DOMException {
        setNodeValue(textContent);
    }
    /**
     * This attribute returns the text content of this node and its
     * descendants. When it is defined to be null, setting it has no effect.
     * When set, any possible children this node may have are removed and
     * replaced by a single <code>Text</code> node containing the string
     * this attribute is set to. On getting, no serialization is performed,
     * the returned string does not contain any markup. No whitespace
     * normalization is performed, the returned string does not contain the
     * element content whitespaces . Similarly, on setting, no parsing is
     * performed either, the input string is taken as pure textual content.
     * <br>The string returned is made of the text content of this node
     * depending on its type, as defined below:
     * <table border='1'>
     * <tr>
     * <th>Node type</th>
     * <th>Content</th>
     * </tr>
     * <tr>
     * <td valign='top' rowspan='1' colspan='1'>
     * ELEMENT_NODE, ENTITY_NODE, ENTITY_REFERENCE_NODE,
     * DOCUMENT_FRAGMENT_NODE</td>
     * <td valign='top' rowspan='1' colspan='1'>concatenation of the <code>textContent</code>
     * attribute value of every child node, excluding COMMENT_NODE and
     * PROCESSING_INSTRUCTION_NODE nodes</td>
     * </tr>
     * <tr>
     * <td valign='top' rowspan='1' colspan='1'>ATTRIBUTE_NODE, TEXT_NODE,
     * CDATA_SECTION_NODE, COMMENT_NODE, PROCESSING_INSTRUCTION_NODE</td>
     * <td valign='top' rowspan='1' colspan='1'>
     * <code>nodeValue</code></td>
     * </tr>
     * <tr>
     * <td valign='top' rowspan='1' colspan='1'>DOCUMENT_NODE, DOCUMENT_TYPE_NODE, NOTATION_NODE</td>
     * <td valign='top' rowspan='1' colspan='1'>
     * null</td>
     * </tr>
     * </table>
     * @exception DOMException
     *   NO_MODIFICATION_ALLOWED_ERR: Raised when the node is readonly.
     * @exception DOMException
     *   DOMSTRING_SIZE_ERR: Raised when it would return more characters than
     *   fit in a <code>DOMString</code> variable on the implementation
     *   platform.
     * @since DOM Level 3
     */
    public String getTextContent() throws DOMException {
        return getNodeValue();  // overriden in some subclasses
    }

     /**
     * Compares a node with this node with regard to their position in the
     * document.
     * @param other The node to compare against this node.
     * @return Returns how the given node is positioned relatively to this
     *   node.
     * @since DOM Level 3
     */
    public short compareDocumentPosition(Node other) throws DOMException {
        return 0;
    }

     /**
     * The absolute base URI of this node or <code>null</code> if undefined.
     * This value is computed according to . However, when the
     * <code>Document</code> supports the feature "HTML" , the base URI is
     * computed using first the value of the href attribute of the HTML BASE
     * element if any, and the value of the <code>documentURI</code>
     * attribute from the <code>Document</code> interface otherwise.
     * <br> When the node is an <code>Element</code>, a <code>Document</code>
     * or a a <code>ProcessingInstruction</code>, this attribute represents
     * the properties [base URI] defined in . When the node is a
     * <code>Notation</code>, an <code>Entity</code>, or an
     * <code>EntityReference</code>, this attribute represents the
     * properties [declaration base URI] in the . How will this be affected
     * by resolution of relative namespace URIs issue?It's not.Should this
     * only be on Document, Element, ProcessingInstruction, Entity, and
     * Notation nodes, according to the infoset? If not, what is it equal to
     * on other nodes? Null? An empty string? I think it should be the
     * parent's.No.Should this be read-only and computed or and actual
     * read-write attribute?Read-only and computed (F2F 19 Jun 2000 and
     * teleconference 30 May 2001).If the base HTML element is not yet
     * attached to a document, does the insert change the Document.baseURI?
     * Yes. (F2F 26 Sep 2001)
     * @since DOM Level 3
     */
    public String getBaseURI() {
        return null;
    }

        /**
     * DOM Level 3 WD - Experimental.
     * Renaming node
     */
    public Node renameNode(Node n,
                           String namespaceURI,
                           String name)
                           throws DOMException{
        return n;
    }


    /**
     *  DOM Level 3 WD - Experimental
     *  Normalize document.
     */
    public void normalizeDocument(){

    }
    /**
     *  The configuration used when <code>Document.normalizeDocument</code> is
     * invoked.
     * @since DOM Level 3
     */
    public DOMConfiguration getDomConfig(){
       return null;
    }


    /**Experimental DOM Level 3 feature: documentURI */
    protected String fDocumentURI;

    /**
     * DOM Level 3 WD - Experimental.
     */
    public void setDocumentURI(String documentURI){

        fDocumentURI= documentURI;
    }

        /**
     * DOM Level 3 WD - Experimental.
     * The location of the document or <code>null</code> if undefined.
     * <br>Beware that when the <code>Document</code> supports the feature
     * "HTML" , the href attribute of the HTML BASE element takes precedence
     * over this attribute.
     * @since DOM Level 3
     */
    public String getDocumentURI(){
        return fDocumentURI;
    }

        /**Experimental DOM Level 3 feature: Document actualEncoding */
    protected String actualEncoding;

     /**
     * DOM Level 3 WD - Experimental.
     * An attribute specifying the actual encoding of this document. This is
     * <code>null</code> otherwise.
     * <br> This attribute represents the property [character encoding scheme]
     * defined in .
     * @since DOM Level 3
     */
    public String getActualEncoding() {
        return actualEncoding;
    }

    /**
     * DOM Level 3 WD - Experimental.
     * An attribute specifying the actual encoding of this document. This is
     * <code>null</code> otherwise.
     * <br> This attribute represents the property [character encoding scheme]
     * defined in .
     * @since DOM Level 3
     */
    public void setActualEncoding(String value) {
        actualEncoding = value;
    }

     /**
    * DOM Level 3 WD - Experimental.
    */
    public Text replaceWholeText(String content)
                                 throws DOMException{
/*

        if (needsSyncData()) {
            synchronizeData();
        }

        // make sure we can make the replacement
        if (!canModify(nextSibling)) {
            throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR,
                DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NO_MODIFICATION_ALLOWED_ERR", null));
        }

        Node parent = this.getParentNode();
        if (content == null || content.length() == 0) {
            // remove current node
            if (parent !=null) { // check if node in the tree
                parent.removeChild(this);
                return null;
            }
        }
        Text currentNode = null;
        if (isReadOnly()){
            Text newNode = this.ownerDocument().createTextNode(content);
            if (parent !=null) { // check if node in the tree
                parent.insertBefore(newNode, this);
                parent.removeChild(this);
                currentNode = newNode;
            } else {
                return newNode;
            }
        }  else {
            this.setData(content);
            currentNode = this;
        }
        Node sibling =  currentNode.getNextSibling();
        while ( sibling !=null) {
            parent.removeChild(sibling);
            sibling = currentNode.getNextSibling();
        }

        return currentNode;
*/
        return null; //Pending
    }

     /**
     * DOM Level 3 WD - Experimental.
     * Returns all text of <code>Text</code> nodes logically-adjacent text
     * nodes to this node, concatenated in document order.
     * @since DOM Level 3
     */
    public String getWholeText(){

/*
        if (needsSyncData()) {
            synchronizeData();
        }
        if (nextSibling == null) {
            return data;
        }
        StringBuffer buffer = new StringBuffer();
        if (data != null && data.length() != 0) {
            buffer.append(data);
        }
        getWholeText(nextSibling, buffer);
        return buffer.toString();
*/
        return null; // PENDING

    }

      /**
    * DOM Level 3 WD - Experimental.
     * Returns whether this text node contains whitespace in element content,
     * often abusively called "ignorable whitespace".
     */
    public boolean isWhitespaceInElementContent(){
        return false;
    }




     /**
     * NON-DOM: set the type of this attribute to be ID type.
     *
     * @param id
     */
    public void setIdAttribute(boolean id){
        //PENDING
    }

     /**
     * DOM Level 3: register the given attribute node as an ID attribute
     */
    public void setIdAttribute(String name, boolean makeId) {
        //PENDING
    }


    /**
     * DOM Level 3: register the given attribute node as an ID attribute
     */
    public void setIdAttributeNode(Attr at, boolean makeId) {
        //PENDING
    }

    /**
     * DOM Level 3: register the given attribute node as an ID attribute
     */
    public void setIdAttributeNS(String namespaceURI, String localName,
                                    boolean makeId) {
        //PENDING
    }
         /**
         * Method getSchemaTypeInfo.
         * @return TypeInfo
         */
    public TypeInfo getSchemaTypeInfo(){
      return null; //PENDING
    }

    public boolean isId() {
        return false; //PENDING
    }

    private String xmlEncoding;
    public String getXmlEncoding ( ) {
        return xmlEncoding;
    }
    public void setXmlEncoding ( String xmlEncoding ) {
        this.xmlEncoding = xmlEncoding;
    }

    private boolean xmlStandalone;
    public boolean getXmlStandalone() {
        return xmlStandalone;
    }

    public void setXmlStandalone(boolean xmlStandalone) throws DOMException {
        this.xmlStandalone = xmlStandalone;
    }

    private String xmlVersion;
    public String getXmlVersion() {
        return xmlVersion;
    }

    public void setXmlVersion(String xmlVersion) throws DOMException {
        this.xmlVersion = xmlVersion;
    }


}
