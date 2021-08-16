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

import java.util.EmptyStackException;

/**
 * Implement a stack of simple integers.
 *
 * %OPT%
 * This is currently based on ObjectVector, which permits fast acess but pays a
 * heavy recopying penalty if/when its size is increased. If we expect deep
 * stacks, we should consider a version based on ChunkedObjectVector.
 * @xsl.usage internal
 */
public class ObjectStack extends ObjectVector
{

  /**
   * Default constructor.  Note that the default
   * block size is very small, for small lists.
   */
  public ObjectStack()
  {
    super();
  }

  /**
   * Construct a ObjectVector, using the given block size.
   *
   * @param blocksize Size of block to allocate
   */
  public ObjectStack(int blocksize)
  {
    super(blocksize);
  }

  /**
   * Copy constructor for ObjectStack
   *
   * @param v ObjectStack to copy
   */
  public ObjectStack (ObjectStack v)
  {
        super(v);
  }

  /**
   * Pushes an item onto the top of this stack.
   *
   * @param   i   the int to be pushed onto this stack.
   * @return  the <code>item</code> argument.
   */
  public Object push(Object i)
  {

    if ((m_firstFree + 1) >= m_mapSize)
    {
      m_mapSize += m_blocksize;

      Object newMap[] = new Object[m_mapSize];

      System.arraycopy(m_map, 0, newMap, 0, m_firstFree + 1);

      m_map = newMap;
    }

    m_map[m_firstFree] = i;

    m_firstFree++;

    return i;
  }

  /**
   * Removes the object at the top of this stack and returns that
   * object as the value of this function.
   *
   * @return     The object at the top of this stack.
   */
  public Object pop()
  {
    Object val = m_map[--m_firstFree];
    m_map[m_firstFree] = null;

    return val;
  }

  /**
   * Quickly pops a number of items from the stack.
   */

  public void quickPop(int n)
  {
    m_firstFree -= n;
  }

  /**
   * Looks at the object at the top of this stack without removing it
   * from the stack.
   *
   * @return     the object at the top of this stack.
   * @throws  EmptyStackException  if this stack is empty.
   */
  public Object peek()
  {
    try {
      return m_map[m_firstFree - 1];
    }
    catch (ArrayIndexOutOfBoundsException e)
    {
      throw new EmptyStackException();
    }
  }

  /**
   * Looks at the object at the position the stack counting down n items.
   *
   * @param n The number of items down, indexed from zero.
   * @return     the object at n items down.
   * @throws  EmptyStackException  if this stack is empty.
   */
  public Object peek(int n)
  {
    try {
      return m_map[m_firstFree-(1+n)];
    }
    catch (ArrayIndexOutOfBoundsException e)
    {
      throw new EmptyStackException();
    }
  }

  /**
   * Sets an object at a the top of the statck
   *
   *
   * @param val object to set at the top
   * @throws  EmptyStackException  if this stack is empty.
   */
  public void setTop(Object val)
  {
    try {
      m_map[m_firstFree - 1] = val;
    }
    catch (ArrayIndexOutOfBoundsException e)
    {
      throw new EmptyStackException();
    }
  }

  /**
   * Tests if this stack is empty.
   *
   * @return  <code>true</code> if this stack is empty;
   *          <code>false</code> otherwise.
   * @since   JDK1.0
   */
  public boolean empty()
  {
    return m_firstFree == 0;
  }

  /**
   * Returns where an object is on this stack.
   *
   * @param   o   the desired object.
   * @return  the distance from the top of the stack where the object is]
   *          located; the return value <code>-1</code> indicates that the
   *          object is not on the stack.
   * @since   JDK1.0
   */
  public int search(Object o)
  {

    int i = lastIndexOf(o);

    if (i >= 0)
    {
      return size() - i;
    }

    return -1;
  }

  /**
   * Returns clone of current ObjectStack
   *
   * @return clone of current ObjectStack
   */
  public Object clone()
    throws CloneNotSupportedException
  {
        return (ObjectStack) super.clone();
  }

}
