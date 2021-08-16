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
/*
 * $Id: DTMNodeProxy.java,v
 */

package com.sun.org.apache.xml.internal.dtm.ref;

import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.dtm.DTMDOMException;
import com.sun.org.apache.xpath.internal.NodeSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
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
import org.w3c.dom.TypeInfo;
import org.w3c.dom.UserDataHandler;

/**
 * <code>DTMNodeProxy</code> presents a DOM Node API front-end to the DTM model.
 * <p>
 * It does _not_ attempt to address the "node identity" question; no effort
 * is made to prevent the creation of multiple proxies referring to a single
 * DTM node. Users can create a mechanism for managing this, or relinquish the
 * use of "==" and use the .sameNodeAs() mechanism, which is under
 * consideration for future versions of the DOM.
 * <p>
 * DTMNodeProxy may be subclassed further to present specific DOM node types.
 *
 * @see org.w3c.dom
 * @LastModified: Nov 2017
 */
public class DTMNodeProxy
  implements Node, Document, Text, Element, Attr,
                   ProcessingInstruction, Comment, DocumentFragment
{

  /** The DTM for this node. */
  public DTM dtm;

  /** The DTM node handle. */
  int node;

  /** The return value as Empty String. */
  private static final String EMPTYSTRING = "";

  /** The DOMImplementation object */
  static final DOMImplementation implementation=new DTMNodeProxyImplementation();

  /**
   * Create a DTMNodeProxy Node representing a specific Node in a DTM
   *
   * @param dtm The DTM Reference, must be non-null.
   * @param node The DTM node handle.
   */
  public DTMNodeProxy(DTM dtm, int node)
  {
    this.dtm = dtm;
    this.node = node;
  }

  /**
   * NON-DOM: Return the DTM model
   *
   * @return The DTM that this proxy is a representative for.
   */
  public final DTM getDTM()
  {
    return dtm;
  }

  /**
   * NON-DOM: Return the DTM node number
   *
   * @return The DTM node handle.
   */
  public final int getDTMNodeNumber()
  {
    return node;
  }

  /**
   * Test for equality based on node number.
   *
   * @param node A DTM node proxy reference.
   *
   * @return true if the given node has the same handle as this node.
   */
  public final boolean equals(Node node)
  {

    try
    {
      DTMNodeProxy dtmp = (DTMNodeProxy) node;

      // return (dtmp.node == this.node);
      // Patch attributed to Gary L Peskin <garyp@firstech.com>
      return (dtmp.node == this.node) && (dtmp.dtm == this.dtm);
    }
    catch (ClassCastException cce)
    {
      return false;
    }
  }

  /**
   * Test for equality based on node number.
   *
   * @param node A DTM node proxy reference.
   *
   * @return true if the given node has the same handle as this node.
   */
  @Override
  public final boolean equals(Object node)
  {
      // DTMNodeProxy dtmp = (DTMNodeProxy)node;
      // return (dtmp.node == this.node);
      // Patch attributed to Gary L Peskin <garyp@firstech.com>
      return node instanceof Node && equals((Node) node);
  }

  @Override
  public int hashCode() {
      int hash = 7;
      hash = 29 * hash + Objects.hashCode(this.dtm);
      hash = 29 * hash + this.node;
      return hash;
  }

  /**
   * FUTURE DOM: Test node identity, in lieu of Node==Node
   *
   * @param other
   *
   * @return true if the given node has the same handle as this node.
   */
  public final boolean sameNodeAs(Node other)
  {

    if (!(other instanceof DTMNodeProxy))
      return false;

    DTMNodeProxy that = (DTMNodeProxy) other;

    return this.dtm == that.dtm && this.node == that.node;
  }

  /**
   *
   *
   * @see org.w3c.dom.Node
   */
  @Override
  public final String getNodeName()
  {
    return dtm.getNodeName(node);
  }

  /**
   * A PI's "target" states what processor channel the PI's data
   * should be directed to. It is defined differently in HTML and XML.
   * <p>
   * In XML, a PI's "target" is the first (whitespace-delimited) token
   * following the "<?" token that begins the PI.
   * <p>
   * In HTML, target is always null.
   * <p>
   * Note that getNodeName is aliased to getTarget.
   *
   *
   */
  @Override
  public final String getTarget()
  {
    return dtm.getNodeName(node);
  }  // getTarget():String

  /**
   *
   *
   * @see org.w3c.dom.Node as of DOM Level 2
   */
  @Override
  public final String getLocalName()
  {
    return dtm.getLocalName(node);
  }

  /**
   * @return The prefix for this node.
   * @see org.w3c.dom.Node as of DOM Level 2
   */
  @Override
  public final String getPrefix()
  {
    return dtm.getPrefix(node);
  }

  /**
   *
   * @param prefix
   *
   * @throws DOMException
   * @see org.w3c.dom.Node as of DOM Level 2 -- DTMNodeProxy is read-only
   */
  @Override
  public final void setPrefix(String prefix) throws DOMException
  {
    throw new DTMDOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR);
  }

  /**
   *
   *
   * @see org.w3c.dom.Node as of DOM Level 2
   */
  @Override
  public final String getNamespaceURI()
  {
    return dtm.getNamespaceURI(node);
  }

  /** Ask whether we support a given DOM feature.
   * In fact, we do not _fully_ support any DOM feature -- we're a
   * read-only subset -- so arguably we should always return false.
   * Or we could say that we support DOM Core Level 2 but all nodes
   * are read-only. Unclear which answer is least misleading.
   *
   * NON-DOM method. This was present in early drafts of DOM Level 2,
   * but was renamed isSupported. It's present here only because it's
   * cheap, harmless, and might help some poor fool who is still trying
   * to use an early Working Draft of the DOM.
   *
   * @param feature
   * @param version
   *
   * @return false
   */
  public final boolean supports(String feature, String version)
  {
    return implementation.hasFeature(feature,version);
    //throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /** Ask whether we support a given DOM feature.
   * In fact, we do not _fully_ support any DOM feature -- we're a
   * read-only subset -- so arguably we should always return false.
   *
   * @param feature
   * @param version
   *
   * @return false
   * @see org.w3c.dom.Node as of DOM Level 2
   */
  @Override
  public final boolean isSupported(String feature, String version)
  {
    return implementation.hasFeature(feature,version);
    // throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   *
   *
   * @throws DOMException
   * @see org.w3c.dom.Node
   */
  @Override
  public final String getNodeValue() throws DOMException
  {
    return dtm.getNodeValue(node);
  }

  /**
   * @return The string value of the node
   *
   * @throws DOMException
   */
  public final String getStringValue() throws DOMException
  {
        return dtm.getStringValue(node).toString();
  }

  /**
   *
   * @param nodeValue
   *
   * @throws DOMException
   * @see org.w3c.dom.Node -- DTMNodeProxy is read-only
   */
  @Override
  public final void setNodeValue(String nodeValue) throws DOMException
  {
    throw new DTMDOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR);
  }

  /**
   *
   *
   * @see org.w3c.dom.Node
   */
  @Override
  public final short getNodeType()
  {
    return dtm.getNodeType(node);
  }

  /**
   *
   *
   * @see org.w3c.dom.Node
   */
  @Override
  public final Node getParentNode()
  {

    if (getNodeType() == Node.ATTRIBUTE_NODE)
      return null;

    int newnode = dtm.getParent(node);

    return (newnode == DTM.NULL) ? null : dtm.getNode(newnode);
  }

  /**
   *
   *
   * @see org.w3c.dom.Node
   */
  public final Node getOwnerNode()
  {

    int newnode = dtm.getParent(node);

    return (newnode == DTM.NULL) ? null : dtm.getNode(newnode);
  }

  /**
   *
   *
   * @see org.w3c.dom.Node
   */
  @Override
  public final NodeList getChildNodes()
  {

    // Annoyingly, AxisIterators do not currently implement DTMIterator, so
    // we can't just wap DTMNodeList around an Axis.CHILD iterator.
    // Instead, we've created a special-case operating mode for that object.
    return new DTMChildIterNodeList(dtm,node);

    // throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   *
   * @see org.w3c.dom.Node
   */
  @Override
  public final Node getFirstChild()
  {

    int newnode = dtm.getFirstChild(node);

    return (newnode == DTM.NULL) ? null : dtm.getNode(newnode);
  }

  /**
   *
   *
   * @see org.w3c.dom.Node
   */
  @Override
  public final Node getLastChild()
  {

    int newnode = dtm.getLastChild(node);

    return (newnode == DTM.NULL) ? null : dtm.getNode(newnode);
  }

  /**
   *
   *
   * @see org.w3c.dom.Node
   */
  @Override
  public final Node getPreviousSibling()
  {

    int newnode = dtm.getPreviousSibling(node);

    return (newnode == DTM.NULL) ? null : dtm.getNode(newnode);
  }

  /**
   *
   *
   * @see org.w3c.dom.Node
   */
  @Override
  public final Node getNextSibling()
  {

    // Attr's Next is defined at DTM level, but not at DOM level.
    if (dtm.getNodeType(node) == Node.ATTRIBUTE_NODE)
      return null;

    int newnode = dtm.getNextSibling(node);

    return (newnode == DTM.NULL) ? null : dtm.getNode(newnode);
  }

  // DTMNamedNodeMap m_attrs;

  /**
   *
   *
   * @see org.w3c.dom.Node
   */
  @Override
  public final NamedNodeMap getAttributes()
  {

    return new DTMNamedNodeMap(dtm, node);
  }

  /**
   * Method hasAttribute
   *
   *
   * @param name
   *
   */
  @Override
  public boolean hasAttribute(String name)
  {
    return DTM.NULL != dtm.getAttributeNode(node,null,name);
  }

  /**
   * Method hasAttributeNS
   *
   *
   * @param namespaceURI
   * @param localName
   *
   *
   */
  @Override
  public boolean hasAttributeNS(String namespaceURI, String localName)
  {
    return DTM.NULL != dtm.getAttributeNode(node,namespaceURI,localName);
  }

  /**
   *
   *
   * @see org.w3c.dom.Node
   */
  @Override
  public final Document getOwnerDocument()
  {
        // Note that this uses the DOM-compatable version of the call
        return (Document)(dtm.getNode(dtm.getOwnerDocument(node)));
  }

  /**
   *
   * @param newChild
   * @param refChild
   *
   *
   *
   * @throws DOMException
   * @see org.w3c.dom.Node -- DTMNodeProxy is read-only
   */
  @Override
  public final Node insertBefore(Node newChild, Node refChild)
    throws DOMException
  {
    throw new DTMDOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR);
  }

  /**
   *
   * @param newChild
   * @param oldChild
   *
   *
   *
   * @throws DOMException
   * @see org.w3c.dom.Node -- DTMNodeProxy is read-only
   */
  @Override
  public final Node replaceChild(Node newChild, Node oldChild)
    throws DOMException
  {
    throw new DTMDOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR);
  }

  /**
   *
   * @param oldChild
   *
   *
   *
   * @throws DOMException
   * @see org.w3c.dom.Node -- DTMNodeProxy is read-only
   */
  @Override
  public final Node removeChild(Node oldChild) throws DOMException
  {
    throw new DTMDOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR);
  }

  /**
   *
   * @param newChild
   *
   *
   *
   * @throws DOMException
   * @see org.w3c.dom.Node -- DTMNodeProxy is read-only
   */
  @Override
  public final Node appendChild(Node newChild) throws DOMException
  {
    throw new DTMDOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR);
  }

  /**
   *
   *
   * @see org.w3c.dom.Node
   */
  @Override
  public final boolean hasChildNodes()
  {
    return (DTM.NULL != dtm.getFirstChild(node));
  }

  /**
   *
   * @param deep
   *
   *
   * @see org.w3c.dom.Node -- DTMNodeProxy is read-only
   */
  @Override
  public final Node cloneNode(boolean deep)
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   *
   * @see org.w3c.dom.Document
   */
  @Override
  public final DocumentType getDoctype()
  {
    return null;
  }

  /**
   *
   *
   * @see org.w3c.dom.Document
   */
  @Override
  public final DOMImplementation getImplementation()
  {
    return implementation;
  }

  /** This is a bit of a problem in DTM, since a DTM may be a Document
   * Fragment and hence not have a clear-cut Document Element. We can
   * make it work in the well-formed cases but would that be confusing for others?
   *
   *
   * @see org.w3c.dom.Document
   */
  @Override
  public final Element getDocumentElement()
  {
                int dochandle=dtm.getDocument();
                int elementhandle=DTM.NULL;
                for(int kidhandle=dtm.getFirstChild(dochandle);
                                kidhandle!=DTM.NULL;
                                kidhandle=dtm.getNextSibling(kidhandle))
                {
                        switch(dtm.getNodeType(kidhandle))
                        {
                        case Node.ELEMENT_NODE:
                                if(elementhandle!=DTM.NULL)
                                {
                                        elementhandle=DTM.NULL; // More than one; ill-formed.
                                        kidhandle=dtm.getLastChild(dochandle); // End loop
                                }
                                else
                                        elementhandle=kidhandle;
                                break;

                        // These are harmless; document is still wellformed
                        case Node.COMMENT_NODE:
                        case Node.PROCESSING_INSTRUCTION_NODE:
                        case Node.DOCUMENT_TYPE_NODE:
                                break;

                        default:
                                elementhandle=DTM.NULL; // ill-formed
                                kidhandle=dtm.getLastChild(dochandle); // End loop
                                break;
                        }
                }
                if(elementhandle==DTM.NULL)
                        throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
                else
                        return (Element)(dtm.getNode(elementhandle));
  }

  /**
   *
   * @param tagName
   *
   *
   *
   * @throws DOMException
   * @see org.w3c.dom.Document
   */
  @Override
  public final Element createElement(String tagName) throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   *
   * @see org.w3c.dom.Document
   */
  @Override
  public final DocumentFragment createDocumentFragment()
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   * @param data
   *
   *
   * @see org.w3c.dom.Document
   */
  @Override
  public final Text createTextNode(String data)
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   * @param data
   *
   *
   * @see org.w3c.dom.Document
   */
  @Override
  public final Comment createComment(String data)
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   * @param data
   *
   *
   *
   * @throws DOMException
   * @see org.w3c.dom.Document
   */
  @Override
  public final CDATASection createCDATASection(String data)
    throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   * @param target
   * @param data
   *
   *
   *
   * @throws DOMException
   * @see org.w3c.dom.Document
   */
  @Override
  public final ProcessingInstruction createProcessingInstruction(
                                String target, String data) throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   * @param name
   *
   *
   *
   * @throws DOMException
   * @see org.w3c.dom.Document
   */
  @Override
  public final Attr createAttribute(String name) throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   * @param name
   *
   *
   *
   * @throws DOMException
   * @see org.w3c.dom.Document
   */
  @Override
  public final EntityReference createEntityReference(String name)
    throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }
 /**
   *
   * @param tagname
   *
   *
   * @see org.w3c.dom.Document
   */
  @Override
  public final NodeList getElementsByTagName(String tagname)
  {
       List<Node> listVector = new ArrayList<>();
       Node retNode = dtm.getNode(node);
       if (retNode != null)
       {
         boolean isTagNameWildCard = "*".equals(tagname);
         if (DTM.ELEMENT_NODE == retNode.getNodeType())
         {
           NodeList nodeList = retNode.getChildNodes();
           for (int i = 0; i < nodeList.getLength(); i++)
           {
             traverseChildren(listVector, nodeList.item(i), tagname,
                              isTagNameWildCard);
           }
         } else if (DTM.DOCUMENT_NODE == retNode.getNodeType()) {
           traverseChildren(listVector, dtm.getNode(node), tagname,
                            isTagNameWildCard);
         }
       }
       int size = listVector.size();
       NodeSet nodeSet = new NodeSet(size);
       for (int i = 0; i < size; i++)
       {
         nodeSet.addNode(listVector.get(i));
       }
       return (NodeList) nodeSet;
  }

  /**
   *
   * @param listVector
   * @param tempNode
   * @param tagname
   * @param isTagNameWildCard
   *
   *
   * Private method to be used for recursive iterations to obtain elements by tag name.
   */
  private final void traverseChildren(List<Node> listVector, Node tempNode,
          String tagname, boolean isTagNameWildCard) {
    if (tempNode == null)
    {
      return;
    }
    else
    {
      if (tempNode.getNodeType() == DTM.ELEMENT_NODE
            && (isTagNameWildCard || tempNode.getNodeName().equals(tagname)))
      {
        listVector.add(tempNode);
      }
      if(tempNode.hasChildNodes())
      {
        NodeList nodeList = tempNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++)
        {
          traverseChildren(listVector, nodeList.item(i), tagname,
                           isTagNameWildCard);
        }
      }
    }
  }



  /**
   *
   * @param importedNode
   * @param deep
   *
   *
   *
   * @throws DOMException
   * @see org.w3c.dom.Document as of DOM Level 2 -- DTMNodeProxy is read-only
   */
  @Override
  public final Node importNode(Node importedNode, boolean deep)
    throws DOMException
  {
    throw new DTMDOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR);
  }

  /**
   *
   * @param namespaceURI
   * @param qualifiedName
   *
   *
   *
   * @throws DOMException
   * @see org.w3c.dom.Document as of DOM Level 2
   */
  @Override
  public final Element createElementNS(
                 String namespaceURI, String qualifiedName) throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   * @param namespaceURI
   * @param qualifiedName
   *
   *
   *
   * @throws DOMException
   * @see org.w3c.dom.Document as of DOM Level 2
   */
  @Override
  public final Attr createAttributeNS(
                  String namespaceURI, String qualifiedName) throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

   /**
   *
   * @param namespaceURI
   * @param localName
   *
   *
   * @see org.w3c.dom.Document as of DOM Level 2
   */
  @Override
  public final NodeList getElementsByTagNameNS(String namespaceURI,
                                               String localName)
  {
    List<Node> listVector = new ArrayList<>();
    Node retNode = dtm.getNode(node);
    if (retNode != null)
    {
      boolean isNamespaceURIWildCard = "*".equals(namespaceURI);
      boolean isLocalNameWildCard    = "*".equals(localName);
      if (DTM.ELEMENT_NODE == retNode.getNodeType())
      {
        NodeList nodeList = retNode.getChildNodes();
        for(int i = 0; i < nodeList.getLength(); i++)
        {
          traverseChildren(listVector, nodeList.item(i), namespaceURI, localName, isNamespaceURIWildCard, isLocalNameWildCard);
        }
      }
      else if(DTM.DOCUMENT_NODE == retNode.getNodeType())
      {
        traverseChildren(listVector, dtm.getNode(node), namespaceURI, localName, isNamespaceURIWildCard, isLocalNameWildCard);
      }
    }
    int size = listVector.size();
    NodeSet nodeSet = new NodeSet(size);
    for (int i = 0; i < size; i++)
    {
      nodeSet.addNode(listVector.get(i));
    }
    return (NodeList) nodeSet;
  }
  /**
   *
   * @param listVector
   * @param tempNode
   * @param namespaceURI
   * @param localname
   * @param isNamespaceURIWildCard
   * @param isLocalNameWildCard
   *
   * Private method to be used for recursive iterations to obtain elements by tag name
   * and namespaceURI.
   */
  private final void traverseChildren(List<Node> listVector, Node tempNode,
          String namespaceURI, String localname, boolean isNamespaceURIWildCard,
          boolean isLocalNameWildCard)
   {
    if (tempNode == null)
    {
      return;
    }
    else
    {
      if (tempNode.getNodeType() == DTM.ELEMENT_NODE
              && (isLocalNameWildCard
                      || tempNode.getLocalName().equals(localname)))
      {
        String nsURI = tempNode.getNamespaceURI();
        if ((namespaceURI == null && nsURI == null)
               || isNamespaceURIWildCard
               || (namespaceURI != null && namespaceURI.equals(nsURI)))
        {
          listVector.add(tempNode);
        }
      }
      if(tempNode.hasChildNodes())
      {
        NodeList nl = tempNode.getChildNodes();
        for(int i = 0; i < nl.getLength(); i++)
        {
          traverseChildren(listVector, nl.item(i), namespaceURI, localname,
                           isNamespaceURIWildCard, isLocalNameWildCard);
        }
      }
    }
  }
  /**
   *
   * @param elementId
   *
   *
   * @see org.w3c.dom.Document as of DOM Level 2
   */
  @Override
  public final Element getElementById(String elementId)
  {
       return (Element) dtm.getNode(dtm.getElementById(elementId));
  }

  /**
   *
   * @param offset
   *
   *
   *
   * @throws DOMException
   * @see org.w3c.dom.Text
   */
  @Override
  public final Text splitText(int offset) throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   *
   *
   * @throws DOMException
   * @see org.w3c.dom.CharacterData
   */
  @Override
  public final String getData() throws DOMException
  {
    return dtm.getNodeValue(node);
  }

  /**
   *
   * @param data
   *
   * @throws DOMException
   * @see org.w3c.dom.CharacterData
   */
  @Override
  public final void setData(String data) throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   *
   * @see org.w3c.dom.CharacterData
   */
  @Override
  public final int getLength()
  {
    // %OPT% This should do something smarter?
    return dtm.getNodeValue(node).length();
  }

  /**
   *
   * @param offset
   * @param count
   *
   *
   *
   * @throws DOMException
   * @see org.w3c.dom.CharacterData
   */
  @Override
  public final String substringData(int offset, int count) throws DOMException
  {
    return getData().substring(offset,offset+count);
  }

  /**
   *
   * @param arg
   *
   * @throws DOMException
   * @see org.w3c.dom.CharacterData
   */
  @Override
  public final void appendData(String arg) throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   * @param offset
   * @param arg
   *
   * @throws DOMException
   * @see org.w3c.dom.CharacterData
   */
  @Override
  public final void insertData(int offset, String arg) throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   * @param offset
   * @param count
   *
   * @throws DOMException
   * @see org.w3c.dom.CharacterData
   */
  @Override
  public final void deleteData(int offset, int count) throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   * @param offset
   * @param count
   * @param arg
   *
   * @throws DOMException
   * @see org.w3c.dom.CharacterData
   */
  @Override
  public final void replaceData(int offset, int count, String arg)
    throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   *
   * @see org.w3c.dom.Element
   */
  @Override
  public final String getTagName()
  {
    return dtm.getNodeName(node);
  }

  /**
   *
   * @param name
   *
   *
   * @see org.w3c.dom.Element
   */
  @Override
  public final String getAttribute(String name)
  {
    DTMNamedNodeMap  map = new DTMNamedNodeMap(dtm, node);
    Node n = map.getNamedItem(name);
    return (null == n) ? EMPTYSTRING : n.getNodeValue();
  }

  /**
   *
   * @param name
   * @param value
   *
   * @throws DOMException
   * @see org.w3c.dom.Element
   */
  @Override
  public final void setAttribute(String name, String value)
    throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   * @param name
   *
   * @throws DOMException
   * @see org.w3c.dom.Element
   */
  @Override
  public final void removeAttribute(String name) throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   * @param name
   *
   *
   * @see org.w3c.dom.Element
   */
  @Override
  public final Attr getAttributeNode(String name)
  {
    DTMNamedNodeMap  map = new DTMNamedNodeMap(dtm, node);
    return (Attr)map.getNamedItem(name);
  }

  /**
   *
   * @param newAttr
   *
   *
   *
   * @throws DOMException
   * @see org.w3c.dom.Element
   */
  @Override
  public final Attr setAttributeNode(Attr newAttr) throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   * @param oldAttr
   *
   *
   *
   * @throws DOMException
   * @see org.w3c.dom.Element
   */
  @Override
  public final Attr removeAttributeNode(Attr oldAttr) throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   * Introduced in DOM Level 2.
   *
   *
   */
  @Override
  public boolean hasAttributes()
  {
    return DTM.NULL != dtm.getFirstAttribute(node);
  }

  /** @see org.w3c.dom.Element */
  @Override
  public final void normalize()
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   * @param namespaceURI
   * @param localName
   *
   *
   * @see org.w3c.dom.Element
   */
  @Override
  public final String getAttributeNS(String namespaceURI, String localName)
  {
    Node retNode = null;
    int n = dtm.getAttributeNode(node,namespaceURI,localName);
    if(n != DTM.NULL)
            retNode = dtm.getNode(n);
    return (null == retNode) ? EMPTYSTRING : retNode.getNodeValue();
  }

  /**
   *
   * @param namespaceURI
   * @param qualifiedName
   * @param value
   *
   * @throws DOMException
   * @see org.w3c.dom.Element
   */
  @Override
  public final void setAttributeNS(
                                   String namespaceURI, String qualifiedName, String value)
    throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   * @param namespaceURI
   * @param localName
   *
   * @throws DOMException
   * @see org.w3c.dom.Element
   */
  @Override
  public final void removeAttributeNS(String namespaceURI, String localName)
    throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   * @param namespaceURI
   * @param localName
   *
   *
   * @see org.w3c.dom.Element
   */
  @Override
  public final Attr getAttributeNodeNS(String namespaceURI, String localName)
  {
       Attr retAttr = null;
       int n = dtm.getAttributeNode(node,namespaceURI,localName);
       if(n != DTM.NULL)
               retAttr = (Attr) dtm.getNode(n);
       return retAttr;

  }

  /**
   *
   * @param newAttr
   *
   *
   *
   * @throws DOMException
   * @see org.w3c.dom.Element
   */
  @Override
  public final Attr setAttributeNodeNS(Attr newAttr) throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   *
   *
   * @see org.w3c.dom.Attr
   */
  @Override
  public final String getName()
  {
    return dtm.getNodeName(node);
  }

  /**
   *
   *
   * @see org.w3c.dom.Attr
   */
  @Override
  public final boolean getSpecified()
  {
    // We really don't know which attributes might have come from the
    // source document versus from the DTD. Treat them all as having
    // been provided by the user.
    // %REVIEW% if/when we become aware of DTDs/schemae.
    return true;
  }

  /**
   *
   *
   * @see org.w3c.dom.Attr
   */
  @Override
  public final String getValue()
  {
    return dtm.getNodeValue(node);
  }

  /**
   *
   * @param value
   * @see org.w3c.dom.Attr
   */
  @Override
  public final void setValue(String value)
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }

  /**
   * Get the owner element of an attribute.
   *
   *
   * @see org.w3c.dom.Attr as of DOM Level 2
   */
  @Override
  public final Element getOwnerElement()
  {
    if (getNodeType() != Node.ATTRIBUTE_NODE)
      return null;
    // In XPath and DTM data models, unlike DOM, an Attr's parent is its
    // owner element.
    int newnode = dtm.getParent(node);
    return (newnode == DTM.NULL) ? null : (Element)(dtm.getNode(newnode));
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
  @Override
  public Node adoptNode(Node source) throws DOMException
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
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
  @Override
  public String getInputEncoding()
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
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
  public void setEncoding(String encoding)
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
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
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
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
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
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
  @Override
  public boolean getStrictErrorChecking()
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
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
  @Override
  public void setStrictErrorChecking(boolean strictErrorChecking)
  {
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
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
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
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
    throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
  }


  /** Inner class to support getDOMImplementation.
   */
  static class DTMNodeProxyImplementation implements DOMImplementation
  {
    @Override
    public DocumentType createDocumentType(String qualifiedName,String publicId, String systemId)
    {
      throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
    }
    @Override
    public Document createDocument(String namespaceURI,String qualfiedName,DocumentType doctype)
    {
      // Could create a DTM... but why, when it'd have to be permanantly empty?
      throw new DTMDOMException(DOMException.NOT_SUPPORTED_ERR);
    }
    /** Ask whether we support a given DOM feature.
     *
     * In fact, we do not _fully_ support any DOM feature -- we're a
     * read-only subset -- so arguably we should always return false.
     * On the other hand, it may be more practically useful to return
     * true and simply treat the whole DOM as read-only, failing on the
     * methods we can't support. I'm not sure which would be more useful
     * to the caller.
     */
    @Override
    public boolean hasFeature(String feature,String version)
    {
      if( ("CORE".equals(feature.toUpperCase()) || "XML".equals(feature.toUpperCase()))
                                        &&
          ("1.0".equals(version) || "2.0".equals(version)))
        return true;
      return false;
    }

    /**
     *  This method returns a specialized object which implements the
     * specialized APIs of the specified feature and version. The
     * specialized object may also be obtained by using binding-specific
     * casting methods but is not necessarily expected to, as discussed in Mixed DOM implementations
.
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
    @Override
    public Object getFeature(String feature, String version) {
        // we don't have any alternate node, either this node does the job
        // or we don't have anything that does
        //return hasFeature(feature, version) ? this : null;
        return null; //PENDING
    }

  }


//RAMESH : Pending proper implementation of DOM Level 3

    @Override
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
    @Override
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
    @Override
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
     * @param arg The node to compare equality with.
     * @param deep If <code>true</code>, recursively compare the subtrees; if
     *   <code>false</code>, compare only the nodes themselves (and its
     *   attributes, if it is an <code>Element</code>).
     * @return If the nodes, and possibly subtrees are equal,
     *   <code>true</code> otherwise <code>false</code>.
     * @since DOM Level 3
     */
    @Override
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
     * DOM Level 3
     * Look up the namespace URI associated to the given prefix, starting from this node.
     * Use lookupNamespaceURI(null) to lookup the default namespace
     *
     * @param namespaceURI
     * @return th URI for the namespace
     * @since DOM Level 3
     */
    @Override
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
     *  DOM Level 3
     *  This method checks if the specified <code>namespaceURI</code> is the
     *  default namespace or not.
     *  @param namespaceURI The namespace URI to look for.
     *  @return  <code>true</code> if the specified <code>namespaceURI</code>
     *   is the default namespace, <code>false</code> otherwise.
     * @since DOM Level 3
     */
    @Override
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
     * DOM Level 3
     * Look up the prefix associated to the given namespace URI, starting from this node.
     *
     * @param namespaceURI
     * @return the prefix for the namespace
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
    public String getTextContent() throws DOMException {
        return dtm.getStringValue(node).toString();
    }

     /**
     * Compares a node with this node with regard to their position in the
     * document.
     * @param other The node to compare against this node.
     * @return Returns how the given node is positioned relatively to this
     *   node.
     * @since DOM Level 3
     */
    @Override
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
    @Override
    public String getBaseURI() {
        return null;
    }

    /**
     * DOM Level 3
     * Renaming node
     */
    @Override
    public Node renameNode(Node n,
                           String namespaceURI,
                           String name)
                           throws DOMException{
        return n;
    }


    /**
     *  DOM Level 3
     *  Normalize document.
     */
    @Override
    public void normalizeDocument(){

    }
    /**
     * The configuration used when <code>Document.normalizeDocument</code> is
     * invoked.
     * @since DOM Level 3
     */
    @Override
    public DOMConfiguration getDomConfig(){
       return null;
    }


    /** DOM Level 3 feature: documentURI */
    protected String fDocumentURI;

    /**
     * DOM Level 3
     */
    @Override
    public void setDocumentURI(String documentURI){
        fDocumentURI= documentURI;
    }

        /**
     * DOM Level 3
     * The location of the document or <code>null</code> if undefined.
     * <br>Beware that when the <code>Document</code> supports the feature
     * "HTML" , the href attribute of the HTML BASE element takes precedence
     * over this attribute.
     * @since DOM Level 3
     */
    @Override
    public String getDocumentURI(){
        return fDocumentURI;
    }

        /**DOM Level 3 feature: Document actualEncoding */
    protected String actualEncoding;

     /**
     * DOM Level 3
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
     * DOM Level 3
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
    * DOM Level 3
    */
    @Override
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
     * DOM Level 3
     * Returns all text of <code>Text</code> nodes logically-adjacent text
     * nodes to this node, concatenated in document order.
     * @since DOM Level 3
     */
    @Override
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
    * DOM Level 3
     * Returns whether this text node contains whitespace in element content,
     * often abusively called "ignorable whitespace".
     */
    @Override
    public boolean isElementContentWhitespace(){
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
    @Override
    public void setIdAttribute(String name, boolean makeId) {
        //PENDING
    }


    /**
     * DOM Level 3: register the given attribute node as an ID attribute
     */
    @Override
    public void setIdAttributeNode(Attr at, boolean makeId) {
        //PENDING
    }

    /**
     * DOM Level 3: register the given attribute node as an ID attribute
     */
    @Override
    public void setIdAttributeNS(String namespaceURI, String localName,
                                    boolean makeId) {
        //PENDING
    }
         /**
         * Method getSchemaTypeInfo.
         * @return TypeInfo
         */
    @Override
    public TypeInfo getSchemaTypeInfo(){
      return null; //PENDING
    }

    @Override
    public boolean isId() {
        return false; //PENDING
    }


    private String xmlEncoding;
    @Override
    public String getXmlEncoding( ) {
        return xmlEncoding;
    }
    public void setXmlEncoding( String xmlEncoding ) {
        this.xmlEncoding = xmlEncoding;
    }

    private boolean xmlStandalone;
    @Override
    public boolean getXmlStandalone() {
        return xmlStandalone;
    }

    @Override
    public void setXmlStandalone(boolean xmlStandalone) throws DOMException {
        this.xmlStandalone = xmlStandalone;
    }

    private String xmlVersion;
    @Override
    public String getXmlVersion() {
        return xmlVersion;
    }

    @Override
    public void setXmlVersion(String xmlVersion) throws DOMException {
        this.xmlVersion = xmlVersion;
    }

}
