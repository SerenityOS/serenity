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

package com.sun.org.apache.xml.internal.dtm.ref;

import com.sun.org.apache.xml.internal.dtm.DTM;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * DTMNamedNodeMap is a quickie (as opposed to quick) implementation of the DOM's
 * NamedNodeMap interface, intended to support DTMProxy's getAttributes()
 * call.
 * <p>
 * ***** Note: this does _not_ current attempt to cache any of the data;
 * if you ask for attribute 27 and then 28, you'll have to rescan the first
 * 27. It should probably at least keep track of the last one retrieved,
 * and possibly buffer the whole array.
 * <p>
 * ***** Also note that there's no fastpath for the by-name query; we search
 * linearly until we find it or fail to find it. Again, that could be
 * optimized at some cost in object creation/storage.
 * @xsl.usage internal
 */
public class DTMNamedNodeMap implements NamedNodeMap
{

  /** The DTM for this node. */
  DTM dtm;

  /** The DTM element handle. */
  int element;

  /** The number of nodes in this map. */
  short m_count = -1;

  /**
   * Create a getAttributes NamedNodeMap for a given DTM element node
   *
   * @param dtm The DTM Reference, must be non-null.
   * @param element The DTM element handle.
   */
  public DTMNamedNodeMap(DTM dtm, int element)
  {
    this.dtm = dtm;
    this.element = element;
  }

  /**
   * Return the number of Attributes on this Element
   *
   * @return The number of nodes in this map.
   */
  public int getLength()
  {

    if (m_count == -1)
    {
      short count = 0;

      for (int n = dtm.getFirstAttribute(element); n != -1;
              n = dtm.getNextAttribute(n))
      {
        ++count;
      }

      m_count = count;
    }

    return (int) m_count;
  }

  /**
   * Retrieves a node specified by name.
   * @param name The <code>nodeName</code> of a node to retrieve.
   * @return A <code>Node</code> (of any type) with the specified
   *   <code>nodeName</code>, or <code>null</code> if it does not identify
   *   any node in this map.
   */
  public Node getNamedItem(String name)
  {

    for (int n = dtm.getFirstAttribute(element); n != DTM.NULL;
            n = dtm.getNextAttribute(n))
    {
      if (dtm.getNodeName(n).equals(name))
        return dtm.getNode(n);
    }

    return null;
  }

  /**
   * Returns the <code>index</code>th item in the map. If <code>index</code>
   * is greater than or equal to the number of nodes in this map, this
   * returns <code>null</code>.
   * @param i The index of the requested item.
   * @return The node at the <code>index</code>th position in the map, or
   *   <code>null</code> if that is not a valid index.
   */
  public Node item(int i)
  {

    int count = 0;

    for (int n = dtm.getFirstAttribute(element); n != -1;
            n = dtm.getNextAttribute(n))
    {
      if (count == i)
        return dtm.getNode(n);
      else
        ++count;
    }

    return null;
  }

  /**
   * Adds a node using its <code>nodeName</code> attribute. If a node with
   * that name is already present in this map, it is replaced by the new
   * one.
   * <br>As the <code>nodeName</code> attribute is used to derive the name
   * which the node must be stored under, multiple nodes of certain types
   * (those that have a "special" string value) cannot be stored as the
   * names would clash. This is seen as preferable to allowing nodes to be
   * aliased.
   * @param newNode node to store in this map. The node will later be
   *   accessible using the value of its <code>nodeName</code> attribute.
   *
   * @return If the new <code>Node</code> replaces an existing node the
   *   replaced <code>Node</code> is returned, otherwise <code>null</code>
   *   is returned.
   * @exception DOMException
   *   WRONG_DOCUMENT_ERR: Raised if <code>arg</code> was created from a
   *   different document than the one that created this map.
   *   <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this map is readonly.
   *   <br>INUSE_ATTRIBUTE_ERR: Raised if <code>arg</code> is an
   *   <code>Attr</code> that is already an attribute of another
   *   <code>Element</code> object. The DOM user must explicitly clone
   *   <code>Attr</code> nodes to re-use them in other elements.
   */
  public Node setNamedItem(Node newNode)
  {
    throw new DTMException(DTMException.NO_MODIFICATION_ALLOWED_ERR);
  }

  /**
   * Removes a node specified by name. When this map contains the attributes
   * attached to an element, if the removed attribute is known to have a
   * default value, an attribute immediately appears containing the
   * default value as well as the corresponding namespace URI, local name,
   * and prefix when applicable.
   * @param name The <code>nodeName</code> of the node to remove.
   *
   * @return The node removed from this map if a node with such a name
   *   exists.
   * @exception DOMException
   *   NOT_FOUND_ERR: Raised if there is no node named <code>name</code> in
   *   this map.
   *   <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this map is readonly.
   */
  public Node removeNamedItem(String name)
  {
    throw new DTMException(DTMException.NO_MODIFICATION_ALLOWED_ERR);
  }

  /**
   * Retrieves a node specified by local name and namespace URI. HTML-only
   * DOM implementations do not need to implement this method.
   * @param namespaceURI The namespace URI of the node to retrieve.
   * @param localName The local name of the node to retrieve.
   *
   * @return A <code>Node</code> (of any type) with the specified local
   *   name and namespace URI, or <code>null</code> if they do not
   *   identify any node in this map.
   * @since DOM Level 2
   */
  public Node getNamedItemNS(String namespaceURI, String localName)
  {
       Node retNode = null;
       for (int n = dtm.getFirstAttribute(element); n != DTM.NULL;
                       n = dtm.getNextAttribute(n))
       {
         if (localName.equals(dtm.getLocalName(n)))
         {
           String nsURI = dtm.getNamespaceURI(n);
           if ((namespaceURI == null && nsURI == null)
                  || (namespaceURI != null && namespaceURI.equals(nsURI)))
           {
             retNode = dtm.getNode(n);
             break;
           }
         }
       }
       return retNode;
  }

  /**
   * Adds a node using its <code>namespaceURI</code> and
   * <code>localName</code>. If a node with that namespace URI and that
   * local name is already present in this map, it is replaced by the new
   * one.
   * <br>HTML-only DOM implementations do not need to implement this method.
   * @param arg A node to store in this map. The node will later be
   *   accessible using the value of its <code>namespaceURI</code> and
   *   <code>localName</code> attributes.
   *
   * @return If the new <code>Node</code> replaces an existing node the
   *   replaced <code>Node</code> is returned, otherwise <code>null</code>
   *   is returned.
   * @exception DOMException
   *   WRONG_DOCUMENT_ERR: Raised if <code>arg</code> was created from a
   *   different document than the one that created this map.
   *   <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this map is readonly.
   *   <br>INUSE_ATTRIBUTE_ERR: Raised if <code>arg</code> is an
   *   <code>Attr</code> that is already an attribute of another
   *   <code>Element</code> object. The DOM user must explicitly clone
   *   <code>Attr</code> nodes to re-use them in other elements.
   * @since DOM Level 2
   */
  public Node setNamedItemNS(Node arg) throws DOMException
  {
    throw new DTMException(DTMException.NO_MODIFICATION_ALLOWED_ERR);
  }

  /**
   * Removes a node specified by local name and namespace URI. A removed
   * attribute may be known to have a default value when this map contains
   * the attributes attached to an element, as returned by the attributes
   * attribute of the <code>Node</code> interface. If so, an attribute
   * immediately appears containing the default value as well as the
   * corresponding namespace URI, local name, and prefix when applicable.
   * <br>HTML-only DOM implementations do not need to implement this method.
   *
   * @param namespaceURI The namespace URI of the node to remove.
   * @param localName The local name of the node to remove.
   *
   * @return The node removed from this map if a node with such a local
   *   name and namespace URI exists.
   * @exception DOMException
   *   NOT_FOUND_ERR: Raised if there is no node with the specified
   *   <code>namespaceURI</code> and <code>localName</code> in this map.
   *   <br>NO_MODIFICATION_ALLOWED_ERR: Raised if this map is readonly.
   * @since DOM Level 2
   */
  public Node removeNamedItemNS(String namespaceURI, String localName)
          throws DOMException
  {
    throw new DTMException(DTMException.NO_MODIFICATION_ALLOWED_ERR);
  }

  /**
   * Simple implementation of DOMException.
   * @xsl.usage internal
   */
  public class DTMException extends org.w3c.dom.DOMException
  {
          static final long serialVersionUID = -8290238117162437678L;
    /**
     * Constructs a DOM/DTM exception.
     *
     * @param code
     * @param message
     */
    public DTMException(short code, String message)
    {
      super(code, message);
    }

    /**
     * Constructor DTMException
     *
     *
     * @param code
     */
    public DTMException(short code)
    {
      super(code, "");
    }
  }
}
