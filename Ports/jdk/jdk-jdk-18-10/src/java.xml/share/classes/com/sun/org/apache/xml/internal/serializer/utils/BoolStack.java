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

package com.sun.org.apache.xml.internal.serializer.utils;


/**
 * Simple stack for boolean values.
 *
 * This class is a copy of the one in com.sun.org.apache.xml.internal.utils.
 * It exists to cut the serializers dependancy on that package.
 * A minor changes from that package are:
 * doesn't implement Clonable
 *
 * This class is not a public API, it is only public because it is
 * used in com.sun.org.apache.xml.internal.serializer.
 *
 * @xsl.usage internal
 */
public final class BoolStack
{

  /** Array of boolean values          */
  private boolean m_values[];

  /** Array size allocated           */
  private int m_allocatedSize;

  /** Index into the array of booleans          */
  private int m_index;

  /**
   * Default constructor.  Note that the default
   * block size is very small, for small lists.
   */
  public BoolStack()
  {
    this(32);
  }

  /**
   * Construct a IntVector, using the given block size.
   *
   * @param size array size to allocate
   */
  public BoolStack(int size)
  {

    m_allocatedSize = size;
    m_values = new boolean[size];
    m_index = -1;
  }

  /**
   * Get the length of the list.
   *
   * @return Current length of the list
   */
  public final int size()
  {
    return m_index + 1;
  }

  /**
   * Clears the stack.
   *
   */
  public final void clear()
  {
    m_index = -1;
  }

  /**
   * Pushes an item onto the top of this stack.
   *
   *
   * @param val the boolean to be pushed onto this stack.
   * @return  the <code>item</code> argument.
   */
  public final boolean push(boolean val)
  {

    if (m_index == m_allocatedSize - 1)
      grow();

    return (m_values[++m_index] = val);
  }

  /**
   * Removes the object at the top of this stack and returns that
   * object as the value of this function.
   *
   * @return     The object at the top of this stack.
   * @throws  EmptyStackException  if this stack is empty.
   */
  public final boolean pop()
  {
    return m_values[m_index--];
  }

  /**
   * Removes the object at the top of this stack and returns the
   * next object at the top as the value of this function.
   *
   *
   * @return Next object to the top or false if none there
   */
  public final boolean popAndTop()
  {

    m_index--;

    return (m_index >= 0) ? m_values[m_index] : false;
  }

  /**
   * Set the item at the top of this stack
   *
   *
   * @param b Object to set at the top of this stack
   */
  public final void setTop(boolean b)
  {
    m_values[m_index] = b;
  }

  /**
   * Looks at the object at the top of this stack without removing it
   * from the stack.
   *
   * @return     the object at the top of this stack.
   * @throws  EmptyStackException  if this stack is empty.
   */
  public final boolean peek()
  {
    return m_values[m_index];
  }

  /**
   * Looks at the object at the top of this stack without removing it
   * from the stack.  If the stack is empty, it returns false.
   *
   * @return     the object at the top of this stack.
   */
  public final boolean peekOrFalse()
  {
    return (m_index > -1) ? m_values[m_index] : false;
  }

  /**
   * Looks at the object at the top of this stack without removing it
   * from the stack.  If the stack is empty, it returns true.
   *
   * @return     the object at the top of this stack.
   */
  public final boolean peekOrTrue()
  {
    return (m_index > -1) ? m_values[m_index] : true;
  }

  /**
   * Tests if this stack is empty.
   *
   * @return  <code>true</code> if this stack is empty;
   *          <code>false</code> otherwise.
   */
  public boolean isEmpty()
  {
    return (m_index == -1);
  }

  /**
   * Grows the size of the stack
   *
   */
  private void grow()
  {

    m_allocatedSize *= 2;

    boolean newVector[] = new boolean[m_allocatedSize];

    System.arraycopy(m_values, 0, newVector, 0, m_index + 1);

    m_values = newVector;
  }
}
