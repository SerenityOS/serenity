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

package com.sun.org.apache.xml.internal.utils;

import java.io.Serializable;

import com.sun.org.apache.xml.internal.dtm.DTM;

/**
 * A very simple table that stores a list of Nodes.
 * @xsl.usage internal
 */
public class NodeVector implements Serializable, Cloneable
{
    static final long serialVersionUID = -713473092200731870L;

  /**
   * Size of blocks to allocate.
   *  @serial
   */
  private int m_blocksize;

  /**
   * Array of nodes this points to.
   *  @serial
   */
  private int m_map[];

  /**
   * Number of nodes in this NodeVector.
   *  @serial
   */
  protected int m_firstFree = 0;

  /**
   * Size of the array this points to.
   *  @serial
   */
  private int m_mapSize;  // lazy initialization

  /**
   * Default constructor.
   */
  public NodeVector()
  {
    m_blocksize = 32;
    m_mapSize = 0;
  }

  /**
   * Construct a NodeVector, using the given block size.
   *
   * @param blocksize Size of blocks to allocate
   */
  public NodeVector(int blocksize)
  {
    m_blocksize = blocksize;
    m_mapSize = 0;
  }

  /**
   * Get a cloned LocPathIterator.
   *
   * @return A clone of this
   *
   * @throws CloneNotSupportedException
   */
  public Object clone() throws CloneNotSupportedException
  {

    NodeVector clone = (NodeVector) super.clone();

    if ((null != this.m_map) && (this.m_map == clone.m_map))
    {
      clone.m_map = new int[this.m_map.length];

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
  public void addElement(int value)
  {

    if ((m_firstFree + 1) >= m_mapSize)
    {
      if (null == m_map)
      {
        m_map = new int[m_blocksize];
        m_mapSize = m_blocksize;
      }
      else
      {
        m_mapSize += m_blocksize;

        int newMap[] = new int[m_mapSize];

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
  public final void push(int value)
  {

    int ff = m_firstFree;

    if ((ff + 1) >= m_mapSize)
    {
      if (null == m_map)
      {
        m_map = new int[m_blocksize];
        m_mapSize = m_blocksize;
      }
      else
      {
        m_mapSize += m_blocksize;

        int newMap[] = new int[m_mapSize];

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
  public final int pop()
  {

    m_firstFree--;

    int n = m_map[m_firstFree];

    m_map[m_firstFree] = DTM.NULL;

    return n;
  }

  /**
   * Pop a node from the tail of the vector and return the
   * top of the stack after the pop.
   *
   * @return The top of the stack after it's been popped
   */
  public final int popAndTop()
  {

    m_firstFree--;

    m_map[m_firstFree] = DTM.NULL;

    return (m_firstFree == 0) ? DTM.NULL : m_map[m_firstFree - 1];
  }

  /**
   * Pop a node from the tail of the vector.
   */
  public final void popQuick()
  {

    m_firstFree--;

    m_map[m_firstFree] = DTM.NULL;
  }

  /**
   * Return the node at the top of the stack without popping the stack.
   * Special purpose method for TransformerImpl, pushElemTemplateElement.
   * Performance critical.
   *
   * @return Node at the top of the stack or null if stack is empty.
   */
  public final int peepOrNull()
  {
    return ((null != m_map) && (m_firstFree > 0))
           ? m_map[m_firstFree - 1] : DTM.NULL;
  }

  /**
   * Push a pair of nodes into the stack.
   * Special purpose method for TransformerImpl, pushElemTemplateElement.
   * Performance critical.
   *
   * @param v1 First node to add to vector
   * @param v2 Second node to add to vector
   */
  public final void pushPair(int v1, int v2)
  {

    if (null == m_map)
    {
      m_map = new int[m_blocksize];
      m_mapSize = m_blocksize;
    }
    else
    {
      if ((m_firstFree + 2) >= m_mapSize)
      {
        m_mapSize += m_blocksize;

        int newMap[] = new int[m_mapSize];

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
    m_map[m_firstFree] = DTM.NULL;
    m_map[m_firstFree + 1] = DTM.NULL;
  }

  /**
   * Set the tail of the stack to the given node.
   * Special purpose method for TransformerImpl, pushElemTemplateElement.
   * Performance critical.
   *
   * @param n Node to set at the tail of vector
   */
  public final void setTail(int n)
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
  public final void setTailSub1(int n)
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
  public final int peepTail()
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
  public final int peepTailSub1()
  {
    return m_map[m_firstFree - 2];
  }

  /**
   * Insert a node in order in the list.
   *
   * @param value Node to insert
   */
  public void insertInOrder(int value)
  {

    for (int i = 0; i < m_firstFree; i++)
    {
      if (value < m_map[i])
      {
        insertElementAt(value, i);

        return;
      }
    }

    addElement(value);
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
  public void insertElementAt(int value, int at)
  {

    if (null == m_map)
    {
      m_map = new int[m_blocksize];
      m_mapSize = m_blocksize;
    }
    else if ((m_firstFree + 1) >= m_mapSize)
    {
      m_mapSize += m_blocksize;

      int newMap[] = new int[m_mapSize];

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
  public void appendNodes(NodeVector nodes)
  {

    int nNodes = nodes.size();

    if (null == m_map)
    {
      m_mapSize = nNodes + m_blocksize;
      m_map = new int[m_mapSize];
    }
    else if ((m_firstFree + nNodes) >= m_mapSize)
    {
      m_mapSize += (nNodes + m_blocksize);

      int newMap[] = new int[m_mapSize];

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
      m_map[i] = DTM.NULL;
    }

    m_firstFree = 0;
  }

  /**
   * Set the length to zero, but don't clear the array.
   */
  public void RemoveAllNoClear()
  {

    if (null == m_map)
      return;

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
  public boolean removeElement(int s)
  {

    if (null == m_map)
      return false;

    for (int i = 0; i < m_firstFree; i++)
    {
      int node = m_map[i];

      if (node == s)
      {
        if (i > m_firstFree)
          System.arraycopy(m_map, i + 1, m_map, i - 1, m_firstFree - i);
        else
          m_map[i] = DTM.NULL;

        m_firstFree--;

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

    if (i > m_firstFree)
      System.arraycopy(m_map, i + 1, m_map, i - 1, m_firstFree - i);
    else
      m_map[i] = DTM.NULL;
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
  public void setElementAt(int node, int index)
  {

    if (null == m_map)
    {
      m_map = new int[m_blocksize];
      m_mapSize = m_blocksize;
    }

    if(index == -1)
        addElement(node);

    m_map[index] = node;
  }

  /**
   * Get the nth element.
   *
   * @param i Index of node to get
   *
   * @return Node at specified index
   */
  public int elementAt(int i)
  {

    if (null == m_map)
      return DTM.NULL;

    return m_map[i];
  }

  /**
   * Tell if the table contains the given node.
   *
   * @param s Node to look for
   *
   * @return True if the given node was found.
   */
  public boolean contains(int s)
  {

    if (null == m_map)
      return false;

    for (int i = 0; i < m_firstFree; i++)
    {
      int node = m_map[i];

      if (node == s)
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
  public int indexOf(int elem, int index)
  {

    if (null == m_map)
      return -1;

    for (int i = index; i < m_firstFree; i++)
    {
      int node = m_map[i];

      if (node == elem)
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
  public int indexOf(int elem)
  {

    if (null == m_map)
      return -1;

    for (int i = 0; i < m_firstFree; i++)
    {
      int node = m_map[i];

      if (node == elem)
        return i;
    }

    return -1;
  }

  /**
   * Sort an array using a quicksort algorithm.
   *
   * @param a The array to be sorted.
   * @param lo0  The low index.
   * @param hi0  The high index.
   *
   * @throws Exception
   */
  public void sort(int a[], int lo0, int hi0) throws Exception
  {

    int lo = lo0;
    int hi = hi0;

    // pause(lo, hi);
    if (lo >= hi)
    {
      return;
    }
    else if (lo == hi - 1)
    {

      /*
       *  sort a two element list by swapping if necessary
       */
      if (a[lo] > a[hi])
      {
        int T = a[lo];

        a[lo] = a[hi];
        a[hi] = T;
      }

      return;
    }

    /*
     *  Pick a pivot and move it out of the way
     */
    int mid = (lo + hi) >>> 1;
    int pivot = a[mid];

    a[mid] = a[hi];
    a[hi] = pivot;

    while (lo < hi)
    {

      /*
       *  Search forward from a[lo] until an element is found that
       *  is greater than the pivot or lo >= hi
       */
      while (a[lo] <= pivot && lo < hi)
      {
        lo++;
      }

      /*
       *  Search backward from a[hi] until element is found that
       *  is less than the pivot, or lo >= hi
       */
      while (pivot <= a[hi] && lo < hi)
      {
        hi--;
      }

      /*
       *  Swap elements a[lo] and a[hi]
       */
      if (lo < hi)
      {
        int T = a[lo];

        a[lo] = a[hi];
        a[hi] = T;

        // pause();
      }

      // if (stopRequested) {
      //    return;
      // }
    }

    /*
     *  Put the median in the "center" of the list
     */
    a[hi0] = a[hi];
    a[hi] = pivot;

    /*
     *  Recursive calls, elements a[lo0] to a[lo-1] are less than or
     *  equal to pivot, elements a[hi+1] to a[hi0] are greater than
     *  pivot.
     */
    sort(a, lo0, lo - 1);
    sort(a, hi + 1, hi0);
  }

  /**
   * Sort an array using a quicksort algorithm.
   *
   * @throws Exception
   */
  public void sort() throws Exception
  {
    sort(m_map, 0, m_firstFree - 1);
  }
}
