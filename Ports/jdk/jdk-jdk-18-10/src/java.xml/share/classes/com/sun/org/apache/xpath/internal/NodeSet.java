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

package com.sun.org.apache.xpath.internal;

import com.sun.org.apache.xalan.internal.res.XSLMessages;
import com.sun.org.apache.xml.internal.utils.DOM2Helper;
import com.sun.org.apache.xpath.internal.axes.ContextNodeList;
import com.sun.org.apache.xpath.internal.res.XPATHErrorResources;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;


/**
 * <p>The NodeSet class can act as either a NodeVector,
 * NodeList, or NodeIterator.  However, in order for it to
 * act as a NodeVector or NodeList, it's required that
 * setShouldCacheNodes(true) be called before the first
 * nextNode() is called, in order that nodes can be added
 * as they are fetched.  Derived classes that implement iterators
 * must override runTo(int index), in order that they may
 * run the iteration to the given index. </p>
 *
 * <p>Note that we directly implement the DOM's NodeIterator
 * interface. We do not emulate all the behavior of the
 * standard NodeIterator. In particular, we do not guarantee
 * to present a "live view" of the document ... but in XSLT,
 * the source document should never be mutated, so this should
 * never be an issue.</p>
 *
 * <p>Thought: Should NodeSet really implement NodeList and NodeIterator,
 * or should there be specific subclasses of it which do so? The
 * advantage of doing it all here is that all NodeSets will respond
 * to the same calls; the disadvantage is that some of them may return
 * less-than-enlightening results when you do so.</p>
 * @xsl.usage advanced
 * @LastModified: Nov 2017
 */
public class NodeSet
        implements NodeList, NodeIterator, Cloneable, ContextNodeList
{

  /**
   * Create an empty nodelist.
   */
  public NodeSet()
  {
    m_blocksize = 32;
    m_mapSize = 0;
  }

  /**
   * Create an empty, using the given block size.
   *
   * @param blocksize Size of blocks to allocate
   */
  public NodeSet(int blocksize)
  {
    m_blocksize = blocksize;
    m_mapSize = 0;
  }

  /**
   * Create a NodeSet, and copy the members of the
   * given nodelist into it.
   *
   * @param nodelist List of Nodes to be made members of the new set.
   */
  public NodeSet(NodeList nodelist)
  {

    this(32);

    addNodes(nodelist);
  }

  /**
   * Create a NodeSet, and copy the members of the
   * given NodeSet into it.
   *
   * @param nodelist Set of Nodes to be made members of the new set.
   */
  public NodeSet(NodeSet nodelist)
  {

    this(32);

    addNodes((NodeIterator) nodelist);
  }

  /**
   * Create a NodeSet, and copy the members of the
   * given NodeIterator into it.
   *
   * @param ni Iterator which yields Nodes to be made members of the new set.
   */
  public NodeSet(NodeIterator ni)
  {

    this(32);

    addNodes(ni);
  }

  /**
   * Create a NodeSet which contains the given Node.
   *
   * @param node Single node to be added to the new set.
   */
  public NodeSet(Node node)
  {

    this(32);

    addNode(node);
  }

  /**
   * @return The root node of the Iterator, as specified when it was created.
   * For non-Iterator NodeSets, this will be null.
   */
  public Node getRoot()
  {
    return null;
  }

  /**
   * Get a cloned Iterator, and reset its state to the beginning of the
   * iteration.
   *
   * @return a new NodeSet of the same type, having the same state...
   * except that the reset() operation has been called.
   *
   * @throws CloneNotSupportedException if this subclass of NodeSet
   * does not support the clone() operation.
   */
  public NodeIterator cloneWithReset() throws CloneNotSupportedException
  {

    NodeSet clone = (NodeSet) clone();

    clone.reset();

    return clone;
  }

  /**
   * Reset the iterator. May have no effect on non-iterator Nodesets.
   */
  public void reset()
  {
    m_next = 0;
  }

  /**
   *  This attribute determines which node types are presented via the
   * iterator. The available set of constants is defined in the
   * <code>NodeFilter</code> interface. For NodeSets, the mask has been
   * hardcoded to show all nodes except EntityReference nodes, which have
   * no equivalent in the XPath data model.
   *
   * @return integer used as a bit-array, containing flags defined in
   * the DOM's NodeFilter class. The value will be
   * <code>SHOW_ALL & ~SHOW_ENTITY_REFERENCE</code>, meaning that
   * only entity references are suppressed.
   */
  public int getWhatToShow()
  {
    return NodeFilter.SHOW_ALL & ~NodeFilter.SHOW_ENTITY_REFERENCE;
  }

  /**
   * The filter object used to screen nodes. Filters are applied to
   * further reduce (and restructure) the NodeIterator's view of the
   * document. In our case, we will be using hardcoded filters built
   * into our iterators... but getFilter() is part of the DOM's
   * NodeIterator interface, so we have to support it.
   *
   * @return null, which is slightly misleading. True, there is no
   * user-written filter object, but in fact we are doing some very
   * sophisticated custom filtering. A DOM purist might suggest
   * returning a placeholder object just to indicate that this is
   * not going to return all nodes selected by whatToShow.
   */
  public NodeFilter getFilter()
  {
    return null;
  }

  /**
   *  The value of this flag determines whether the children of entity
   * reference nodes are visible to the iterator. If false, they will be
   * skipped over.
   * <br> To produce a view of the document that has entity references
   * expanded and does not expose the entity reference node itself, use the
   * whatToShow flags to hide the entity reference node and set
   * expandEntityReferences to true when creating the iterator. To produce
   * a view of the document that has entity reference nodes but no entity
   * expansion, use the whatToShow flags to show the entity reference node
   * and set expandEntityReferences to false.
   *
   * @return true for all iterators based on NodeSet, meaning that the
   * contents of EntityRefrence nodes may be returned (though whatToShow
   * says that the EntityReferences themselves are not shown.)
   */
  public boolean getExpandEntityReferences()
  {
    return true;
  }

  /**
   *  Returns the next node in the set and advances the position of the
   * iterator in the set. After a NodeIterator is created, the first call
   * to nextNode() returns the first node in the set.
   * @return  The next <code>Node</code> in the set being iterated over, or
   *   <code>null</code> if there are no more members in that set.
   * @throws DOMException
   *    INVALID_STATE_ERR: Raised if this method is called after the
   *   <code>detach</code> method was invoked.
   */
  public Node nextNode() throws DOMException
  {

    if ((m_next) < this.size())
    {
      Node next = this.elementAt(m_next);

      m_next++;

      return next;
    }
    else
      return null;
  }

  /**
   *  Returns the previous node in the set and moves the position of the
   * iterator backwards in the set.
   * @return  The previous <code>Node</code> in the set being iterated over,
   *   or<code>null</code> if there are no more members in that set.
   * @throws DOMException
   *    INVALID_STATE_ERR: Raised if this method is called after the
   *   <code>detach</code> method was invoked.
   * @throws RuntimeException thrown if this NodeSet is not of
   * a cached type, and hence doesn't know what the previous node was.
   */
  public Node previousNode() throws DOMException
  {

    if (!m_cacheNodes)
      throw new RuntimeException(
        XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_CANNOT_ITERATE, null)); //"This NodeSet can not iterate to a previous node!");

    if ((m_next - 1) > 0)
    {
      m_next--;

      return this.elementAt(m_next);
    }
    else
      return null;
  }

  /**
   * Detaches the iterator from the set which it iterated over, releasing
   * any computational resources and placing the iterator in the INVALID
   * state. After<code>detach</code> has been invoked, calls to
   * <code>nextNode</code> or<code>previousNode</code> will raise the
   * exception INVALID_STATE_ERR.
   * <p>
   * This operation is a no-op in NodeSet, and will not cause
   * INVALID_STATE_ERR to be raised by later operations.
   * </p>
   */
  public void detach(){}

  /**
   * Tells if this NodeSet is "fresh", in other words, if
   * the first nextNode() that is called will return the
   * first node in the set.
   *
   * @return true if nextNode() would return the first node in the set,
   * false if it would return a later one.
   */
  public boolean isFresh()
  {
    return (m_next == 0);
  }

  /**
   * If an index is requested, NodeSet will call this method
   * to run the iterator to the index.  By default this sets
   * m_next to the index.  If the index argument is -1, this
   * signals that the iterator should be run to the end.
   *
   * @param index Position to advance (or retreat) to, with
   * 0 requesting the reset ("fresh") position and -1 (or indeed
   * any out-of-bounds value) requesting the final position.
   * @throws RuntimeException thrown if this NodeSet is not
   * one of the types which supports indexing/counting.
   */
  public void runTo(int index)
  {

    if (!m_cacheNodes)
      throw new RuntimeException(
        XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_CANNOT_INDEX, null)); //"This NodeSet can not do indexing or counting functions!");

    if ((index >= 0) && (m_next < m_firstFree))
      m_next = index;
    else
      m_next = m_firstFree - 1;
  }

  /**
   * Returns the <code>index</code>th item in the collection. If
   * <code>index</code> is greater than or equal to the number of nodes in
   * the list, this returns <code>null</code>.
   *
   * TODO: What happens if index is out of range?
   *
   * @param index Index into the collection.
   * @return The node at the <code>index</code>th position in the
   *   <code>NodeList</code>, or <code>null</code> if that is not a valid
   *   index.
   */
  public Node item(int index)
  {

    runTo(index);

    return this.elementAt(index);
  }

  /**
   * The number of nodes in the list. The range of valid child node indices is
   * 0 to <code>length-1</code> inclusive. Note that this operation requires
   * finding all the matching nodes, which may defeat attempts to defer
   * that work.
   *
   * @return integer indicating how many nodes are represented by this list.
   */
  public int getLength()
  {

    runTo(-1);

    return this.size();
  }

  /**
   * Add a node to the NodeSet. Not all types of NodeSets support this
   * operation
   *
   * @param n Node to be added
   * @throws RuntimeException thrown if this NodeSet is not of
   * a mutable type.
   */
  public void addNode(Node n)
  {

    if (!m_mutable)
      throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null)); //"This NodeSet is not mutable!");

    this.addElement(n);
  }

  /**
   * Insert a node at a given position.
   *
   * @param n Node to be added
   * @param pos Offset at which the node is to be inserted,
   * with 0 being the first position.
   * @throws RuntimeException thrown if this NodeSet is not of
   * a mutable type.
   */
  public void insertNode(Node n, int pos)
  {

    if (!m_mutable)
      throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null)); //"This NodeSet is not mutable!");

    insertElementAt(n, pos);
  }

  /**
   * Remove a node.
   *
   * @param n Node to be added
   * @throws RuntimeException thrown if this NodeSet is not of
   * a mutable type.
   */
  public void removeNode(Node n)
  {

    if (!m_mutable)
      throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null)); //"This NodeSet is not mutable!");

    this.removeElement(n);
  }

  /**
   * Copy NodeList members into this nodelist, adding in
   * document order.  If a node is null, don't add it.
   *
   * @param nodelist List of nodes which should now be referenced by
   * this NodeSet.
   * @throws RuntimeException thrown if this NodeSet is not of
   * a mutable type.
   */
  public void addNodes(NodeList nodelist)
  {

    if (!m_mutable)
      throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null)); //"This NodeSet is not mutable!");

    if (null != nodelist)  // defensive to fix a bug that Sanjiva reported.
    {
      int nChildren = nodelist.getLength();

      for (int i = 0; i < nChildren; i++)
      {
        Node obj = nodelist.item(i);

        if (null != obj)
        {
          addElement(obj);
        }
      }
    }

    // checkDups();
  }

  /**
   * <p>Copy NodeList members into this nodelist, adding in
   * document order.  Only genuine node references will be copied;
   * nulls appearing in the source NodeSet will
   * not be added to this one. </p>
   *
   * <p> In case you're wondering why this function is needed: NodeSet
   * implements both NodeIterator and NodeList. If this method isn't
   * provided, Java can't decide which of those to use when addNodes()
   * is invoked. Providing the more-explicit match avoids that
   * ambiguity.)</p>
   *
   * @param ns NodeSet whose members should be merged into this NodeSet.
   * @throws RuntimeException thrown if this NodeSet is not of
   * a mutable type.
   */
  public void addNodes(NodeSet ns)
  {

    if (!m_mutable)
      throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null)); //"This NodeSet is not mutable!");

    addNodes((NodeIterator) ns);
  }

  /**
   * Copy NodeList members into this nodelist, adding in
   * document order.  Null references are not added.
   *
   * @param iterator NodeIterator which yields the nodes to be added.
   * @throws RuntimeException thrown if this NodeSet is not of
   * a mutable type.
   */
  public void addNodes(NodeIterator iterator)
  {

    if (!m_mutable)
      throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null)); //"This NodeSet is not mutable!");

    if (null != iterator)  // defensive to fix a bug that Sanjiva reported.
    {
      Node obj;

      while (null != (obj = iterator.nextNode()))
      {
        addElement(obj);
      }
    }

    // checkDups();
  }

  /**
   * Copy NodeList members into this nodelist, adding in
   * document order.  If a node is null, don't add it.
   *
   * @param nodelist List of nodes to be added
   * @param support The XPath runtime context.
   * @throws RuntimeException thrown if this NodeSet is not of
   * a mutable type.
   */
  public void addNodesInDocOrder(NodeList nodelist, XPathContext support)
  {

    if (!m_mutable)
      throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null)); //"This NodeSet is not mutable!");

    int nChildren = nodelist.getLength();

    for (int i = 0; i < nChildren; i++)
    {
      Node node = nodelist.item(i);

      if (null != node)
      {
        addNodeInDocOrder(node, support);
      }
    }
  }

  /**
   * Copy NodeList members into this nodelist, adding in
   * document order.  If a node is null, don't add it.
   *
   * @param iterator NodeIterator which yields the nodes to be added.
   * @param support The XPath runtime context.
   * @throws RuntimeException thrown if this NodeSet is not of
   * a mutable type.
   */
  public void addNodesInDocOrder(NodeIterator iterator, XPathContext support)
  {

    if (!m_mutable)
      throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null)); //"This NodeSet is not mutable!");

    Node node;

    while (null != (node = iterator.nextNode()))
    {
      addNodeInDocOrder(node, support);
    }
  }

  /**
   * Add the node list to this node set in document order.
   *
   * @param start index.
   * @param end index.
   * @param testIndex index.
   * @param nodelist The nodelist to add.
   * @param support The XPath runtime context.
   *
   * @return false always.
   * @throws RuntimeException thrown if this NodeSet is not of
   * a mutable type.
   */
  private boolean addNodesInDocOrder(int start, int end, int testIndex,
                                     NodeList nodelist, XPathContext support)
  {

    if (!m_mutable)
      throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null)); //"This NodeSet is not mutable!");

    boolean foundit = false;
    int i;
    Node node = nodelist.item(testIndex);

    for (i = end; i >= start; i--)
    {
      Node child = elementAt(i);

      if (child == node)
      {
        i = -2;  // Duplicate, suppress insert

        break;
      }

      if (!DOM2Helper.isNodeAfter(node, child))
      {
        insertElementAt(node, i + 1);

        testIndex--;

        if (testIndex > 0)
        {
          boolean foundPrev = addNodesInDocOrder(0, i, testIndex, nodelist,
                                                 support);

          if (!foundPrev)
          {
            addNodesInDocOrder(i, size() - 1, testIndex, nodelist, support);
          }
        }

        break;
      }
    }

    if (i == -1)
    {
      insertElementAt(node, 0);
    }

    return foundit;
  }

  /**
   * Add the node into a vector of nodes where it should occur in
   * document order.
   * @param node The node to be added.
   * @param test true if we should test for doc order
   * @param support The XPath runtime context.
   * @return insertIndex.
   * @throws RuntimeException thrown if this NodeSet is not of
   * a mutable type.
   */
  public int addNodeInDocOrder(Node node, boolean test, XPathContext support)
  {

    if (!m_mutable)
      throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null)); //XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null)); //"This NodeSet is not mutable!");

    int insertIndex = -1;

    if (test)
    {

      // This needs to do a binary search, but a binary search
      // is somewhat tough because the sequence test involves
      // two nodes.
      int size = size(), i;

      for (i = size - 1; i >= 0; i--)
      {
        Node child = elementAt(i);

        if (child == node)
        {
          i = -2;  // Duplicate, suppress insert

          break;
        }

        if (!DOM2Helper.isNodeAfter(node, child))
        {
          break;
        }
      }

      if (i != -2)
      {
        insertIndex = i + 1;

        insertElementAt(node, insertIndex);
      }
    }
    else
    {
      insertIndex = this.size();

      boolean foundit = false;

      for (int i = 0; i < insertIndex; i++)
      {
        if (this.item(i).equals(node))
        {
          foundit = true;

          break;
        }
      }

      if (!foundit)
        addElement(node);
    }

    // checkDups();
    return insertIndex;
  }  // end addNodeInDocOrder(Vector v, Object obj)

  /**
   * Add the node into a vector of nodes where it should occur in
   * document order.
   * @param node The node to be added.
   * @param support The XPath runtime context.
   *
   * @return The index where it was inserted.
   * @throws RuntimeException thrown if this NodeSet is not of
   * a mutable type.
   */
  public int addNodeInDocOrder(Node node, XPathContext support)
  {

    if (!m_mutable)
      throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null)); //"This NodeSet is not mutable!");

    return addNodeInDocOrder(node, true, support);
  }  // end addNodeInDocOrder(Vector v, Object obj)


  /** If this node is being used as an iterator, the next index that nextNode()
   *  will return.  */
  transient protected int m_next = 0;

  /**
   * Get the current position, which is one less than
   * the next nextNode() call will retrieve.  i.e. if
   * you call getCurrentPos() and the return is 0, the next
   * fetch will take place at index 1.
   *
   * @return The the current position index.
   */
  public int getCurrentPos()
  {
    return m_next;
  }

  /**
   * Set the current position in the node set.
   * @param i Must be a valid index.
   * @throws RuntimeException thrown if this NodeSet is not of
   * a cached type, and thus doesn't permit indexed access.
   */
  public void setCurrentPos(int i)
  {

    if (!m_cacheNodes)
      throw new RuntimeException(
        XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_CANNOT_INDEX, null)); //"This NodeSet can not do indexing or counting functions!");

    m_next = i;
  }

  /**
   * Return the last fetched node.  Needed to support the UnionPathIterator.
   *
   * @return the last fetched node.
   * @throws RuntimeException thrown if this NodeSet is not of
   * a cached type, and thus doesn't permit indexed access.
   */
  public Node getCurrentNode()
  {

    if (!m_cacheNodes)
      throw new RuntimeException(
        XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_CANNOT_INDEX, null)); //"This NodeSet can not do indexing or counting functions!");

    int saved = m_next;
    Node n = (m_next < m_firstFree) ? elementAt(m_next) : null;
    m_next = saved; // HACK: I think this is a bit of a hack.  -sb
    return n;
  }

  /** True if this list can be mutated.  */
  transient protected boolean m_mutable = true;

  /** True if this list is cached.
   *  @serial  */
  transient protected boolean m_cacheNodes = true;

  /**
   * Get whether or not this is a cached node set.
   *
   *
   * @return True if this list is cached.
   */
  public boolean getShouldCacheNodes()
  {
    return m_cacheNodes;
  }

  /**
   * If setShouldCacheNodes(true) is called, then nodes will
   * be cached.  They are not cached by default. This switch must
   * be set before the first call to nextNode is made, to ensure
   * that all nodes are cached.
   *
   * @param b true if this node set should be cached.
   * @throws RuntimeException thrown if an attempt is made to
   * request caching after we've already begun stepping through the
   * nodes in this set.
  */
  public void setShouldCacheNodes(boolean b)
  {

    if (!isFresh())
      throw new RuntimeException(
        XSLMessages.createXPATHMessage(XPATHErrorResources.ER_CANNOT_CALL_SETSHOULDCACHENODE, null)); //"Can not call setShouldCacheNodes after nextNode has been called!");

    m_cacheNodes = b;
    m_mutable = true;
  }


  transient private int m_last = 0;

  public int getLast()
  {
    return m_last;
  }

  public void setLast(int last)
  {
    m_last = last;
  }

  /** Size of blocks to allocate.
   *  @serial          */
  private int m_blocksize;

  /** Array of nodes this points to.
   *  @serial          */
  Node m_map[];

  /** Number of nodes in this NodeVector.
   *  @serial          */
  protected int m_firstFree = 0;

  /** Size of the array this points to.
   *  @serial           */
  private int m_mapSize;  // lazy initialization

  /**
   * Get a cloned LocPathIterator.
   *
   * @return A clone of this
   *
   * @throws CloneNotSupportedException
   */
  public Object clone() throws CloneNotSupportedException
  {

    NodeSet clone = (NodeSet) super.clone();

    if ((null != this.m_map) && (this.m_map == clone.m_map))
    {
      clone.m_map = new Node[this.m_map.length];

      System.arraycopy(this.m_map, 0, clone.m_map, 0, this.m_map.length);
    }

    return clone;
  }

  /**
   * Get the length of the list.
   *
   * @return Number of nodes in this NodeVector
   */
  public int size()
  {
    return m_firstFree;
  }

  /**
   * Append a Node onto the vector.
   *
   * @param value Node to add to the vector
   */
  public void addElement(Node value)
  {
    if (!m_mutable)
      throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null)); //"This NodeSet is not mutable!");

    if ((m_firstFree + 1) >= m_mapSize)
    {
      if (null == m_map)
      {
        m_map = new Node[m_blocksize];
        m_mapSize = m_blocksize;
      }
      else
      {
        m_mapSize += m_blocksize;

        Node newMap[] = new Node[m_mapSize];

        System.arraycopy(m_map, 0, newMap, 0, m_firstFree + 1);

        m_map = newMap;
      }
    }

    m_map[m_firstFree] = value;

    m_firstFree++;
  }

  /**
   * Append a Node onto the vector.
   *
   * @param value Node to add to the vector
   */
  public final void push(Node value)
  {

    int ff = m_firstFree;

    if ((ff + 1) >= m_mapSize)
    {
      if (null == m_map)
      {
        m_map = new Node[m_blocksize];
        m_mapSize = m_blocksize;
      }
      else
      {
        m_mapSize += m_blocksize;

        Node newMap[] = new Node[m_mapSize];

        System.arraycopy(m_map, 0, newMap, 0, ff + 1);

        m_map = newMap;
      }
    }

    m_map[ff] = value;

    ff++;

    m_firstFree = ff;
  }

  /**
   * Pop a node from the tail of the vector and return the result.
   *
   * @return the node at the tail of the vector
   */
  public final Node pop()
  {

    m_firstFree--;

    Node n = m_map[m_firstFree];

    m_map[m_firstFree] = null;

    return n;
  }

  /**
   * Pop a node from the tail of the vector and return the
   * top of the stack after the pop.
   *
   * @return The top of the stack after it's been popped
   */
  public final Node popAndTop()
  {

    m_firstFree--;

    m_map[m_firstFree] = null;

    return (m_firstFree == 0) ? null : m_map[m_firstFree - 1];
  }

  /**
   * Pop a node from the tail of the vector.
   */
  public final void popQuick()
  {

    m_firstFree--;

    m_map[m_firstFree] = null;
  }

  /**
   * Return the node at the top of the stack without popping the stack.
   * Special purpose method for TransformerImpl, pushElemTemplateElement.
   * Performance critical.
   *
   * @return Node at the top of the stack or null if stack is empty.
   */
  public final Node peepOrNull()
  {
    return ((null != m_map) && (m_firstFree > 0))
           ? m_map[m_firstFree - 1] : null;
  }

  /**
   * Push a pair of nodes into the stack.
   * Special purpose method for TransformerImpl, pushElemTemplateElement.
   * Performance critical.
   *
   * @param v1 First node to add to vector
   * @param v2 Second node to add to vector
   */
  public final void pushPair(Node v1, Node v2)
  {

    if (null == m_map)
    {
      m_map = new Node[m_blocksize];
      m_mapSize = m_blocksize;
    }
    else
    {
      if ((m_firstFree + 2) >= m_mapSize)
      {
        m_mapSize += m_blocksize;

        Node newMap[] = new Node[m_mapSize];

        System.arraycopy(m_map, 0, newMap, 0, m_firstFree);

        m_map = newMap;
      }
    }

    m_map[m_firstFree] = v1;
    m_map[m_firstFree + 1] = v2;
    m_firstFree += 2;
  }

  /**
   * Pop a pair of nodes from the tail of the stack.
   * Special purpose method for TransformerImpl, pushElemTemplateElement.
   * Performance critical.
   */
  public final void popPair()
  {

    m_firstFree -= 2;
    m_map[m_firstFree] = null;
    m_map[m_firstFree + 1] = null;
  }

  /**
   * Set the tail of the stack to the given node.
   * Special purpose method for TransformerImpl, pushElemTemplateElement.
   * Performance critical.
   *
   * @param n Node to set at the tail of vector
   */
  public final void setTail(Node n)
  {
    m_map[m_firstFree - 1] = n;
  }

  /**
   * Set the given node one position from the tail.
   * Special purpose method for TransformerImpl, pushElemTemplateElement.
   * Performance critical.
   *
   * @param n Node to set
   */
  public final void setTailSub1(Node n)
  {
    m_map[m_firstFree - 2] = n;
  }

  /**
   * Return the node at the tail of the vector without popping
   * Special purpose method for TransformerImpl, pushElemTemplateElement.
   * Performance critical.
   *
   * @return Node at the tail of the vector
   */
  public final Node peepTail()
  {
    return m_map[m_firstFree - 1];
  }

  /**
   * Return the node one position from the tail without popping.
   * Special purpose method for TransformerImpl, pushElemTemplateElement.
   * Performance critical.
   *
   * @return Node one away from the tail
   */
  public final Node peepTailSub1()
  {
    return m_map[m_firstFree - 2];
  }

  /**
   * Inserts the specified node in this vector at the specified index.
   * Each component in this vector with an index greater or equal to
   * the specified index is shifted upward to have an index one greater
   * than the value it had previously.
   *
   * @param value Node to insert
   * @param at Position where to insert
   */
  public void insertElementAt(Node value, int at)
  {
    if (!m_mutable)
      throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null)); //"This NodeSet is not mutable!");

    if (null == m_map)
    {
      m_map = new Node[m_blocksize];
      m_mapSize = m_blocksize;
    }
    else if ((m_firstFree + 1) >= m_mapSize)
    {
      m_mapSize += m_blocksize;

      Node newMap[] = new Node[m_mapSize];

      System.arraycopy(m_map, 0, newMap, 0, m_firstFree + 1);

      m_map = newMap;
    }

    if (at <= (m_firstFree - 1))
    {
      System.arraycopy(m_map, at, m_map, at + 1, m_firstFree - at);
    }

    m_map[at] = value;

    m_firstFree++;
  }

  /**
   * Append the nodes to the list.
   *
   * @param nodes NodeVector to append to this list
   */
  public void appendNodes(NodeSet nodes)
  {

    int nNodes = nodes.size();

    if (null == m_map)
    {
      m_mapSize = nNodes + m_blocksize;
      m_map = new Node[m_mapSize];
    }
    else if ((m_firstFree + nNodes) >= m_mapSize)
    {
      m_mapSize += (nNodes + m_blocksize);

      Node newMap[] = new Node[m_mapSize];

      System.arraycopy(m_map, 0, newMap, 0, m_firstFree + nNodes);

      m_map = newMap;
    }

    System.arraycopy(nodes.m_map, 0, m_map, m_firstFree, nNodes);

    m_firstFree += nNodes;
  }

  /**
   * Inserts the specified node in this vector at the specified index.
   * Each component in this vector with an index greater or equal to
   * the specified index is shifted upward to have an index one greater
   * than the value it had previously.
   */
  public void removeAllElements()
  {

    if (null == m_map)
      return;

    for (int i = 0; i < m_firstFree; i++)
    {
      m_map[i] = null;
    }

    m_firstFree = 0;
  }

  /**
   * Removes the first occurrence of the argument from this vector.
   * If the object is found in this vector, each component in the vector
   * with an index greater or equal to the object's index is shifted
   * downward to have an index one smaller than the value it had
   * previously.
   *
   * @param s Node to remove from the list
   *
   * @return True if the node was successfully removed
   */
  public boolean removeElement(Node s)
  {
    if (!m_mutable)
      throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null)); //"This NodeSet is not mutable!");

    if (null == m_map)
      return false;

    for (int i = 0; i < m_firstFree; i++)
    {
      Node node = m_map[i];

      if ((null != node) && node.equals(s))
      {
        if (i < m_firstFree - 1)
          System.arraycopy(m_map, i + 1, m_map, i, m_firstFree - i - 1);

        m_firstFree--;
        m_map[m_firstFree] = null;

        return true;
      }
    }

    return false;
  }

  /**
   * Deletes the component at the specified index. Each component in
   * this vector with an index greater or equal to the specified
   * index is shifted downward to have an index one smaller than
   * the value it had previously.
   *
   * @param i Index of node to remove
   */
  public void removeElementAt(int i)
  {

    if (null == m_map)
      return;

    if (i >= m_firstFree)
      throw new ArrayIndexOutOfBoundsException(i + " >= " + m_firstFree);
    else if (i < 0)
      throw new ArrayIndexOutOfBoundsException(i);

    if (i < m_firstFree - 1)
      System.arraycopy(m_map, i + 1, m_map, i, m_firstFree - i - 1);

    m_firstFree--;
    m_map[m_firstFree] = null;
  }

  /**
   * Sets the component at the specified index of this vector to be the
   * specified object. The previous component at that position is discarded.
   *
   * The index must be a value greater than or equal to 0 and less
   * than the current size of the vector.
   *
   * @param node Node to set
   * @param index Index of where to set the node
   */
  public void setElementAt(Node node, int index)
  {
    if (!m_mutable)
      throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_NODESET_NOT_MUTABLE, null)); //"This NodeSet is not mutable!");

    if (null == m_map)
    {
      m_map = new Node[m_blocksize];
      m_mapSize = m_blocksize;
    }

    m_map[index] = node;
  }

  /**
   * Get the nth element.
   *
   * @param i Index of node to get
   *
   * @return Node at specified index
   */
  public Node elementAt(int i)
  {

    if (null == m_map)
      return null;

    return m_map[i];
  }

  /**
   * Tell if the table contains the given node.
   *
   * @param s Node to look for
   *
   * @return True if the given node was found.
   */
  public boolean contains(Node s)
  {
    runTo(-1);

    if (null == m_map)
      return false;

    for (int i = 0; i < m_firstFree; i++)
    {
      Node node = m_map[i];

      if ((null != node) && node.equals(s))
        return true;
    }

    return false;
  }

  /**
   * Searches for the first occurence of the given argument,
   * beginning the search at index, and testing for equality
   * using the equals method.
   *
   * @param elem Node to look for
   * @param index Index of where to start the search
   * @return the index of the first occurrence of the object
   * argument in this vector at position index or later in the
   * vector; returns -1 if the object is not found.
   */
  public int indexOf(Node elem, int index)
  {
    runTo(-1);

    if (null == m_map)
      return -1;

    for (int i = index; i < m_firstFree; i++)
    {
      Node node = m_map[i];

      if ((null != node) && node.equals(elem))
        return i;
    }

    return -1;
  }

  /**
   * Searches for the first occurence of the given argument,
   * beginning the search at index, and testing for equality
   * using the equals method.
   *
   * @param elem Node to look for
   * @return the index of the first occurrence of the object
   * argument in this vector at position index or later in the
   * vector; returns -1 if the object is not found.
   */
  public int indexOf(Node elem)
  {
    runTo(-1);

    if (null == m_map)
      return -1;

    for (int i = 0; i < m_firstFree; i++)
    {
      Node node = m_map[i];

      if ((null != node) && node.equals(elem))
        return i;
    }

    return -1;
  }

}
