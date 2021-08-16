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

/**
 * A very simple table that stores a list of strings, optimized
 * for small lists.
 * @xsl.usage internal
 */
public class StringVector implements java.io.Serializable
{
    static final long serialVersionUID = 4995234972032919748L;

  /** @serial Size of blocks to allocate           */
  protected int m_blocksize;

  /** @serial Array of strings this contains          */
  protected String m_map[];

  /** @serial Number of strings this contains          */
  protected int m_firstFree = 0;

  /** @serial Size of the array          */
  protected int m_mapSize;

  /**
   * Default constructor.  Note that the default
   * block size is very small, for small lists.
   */
  public StringVector()
  {

    m_blocksize = 8;
    m_mapSize = m_blocksize;
    m_map = new String[m_blocksize];
  }

  /**
   * Construct a StringVector, using the given block size.
   *
   * @param blocksize Size of the blocks to allocate
   */
  public StringVector(int blocksize)
  {

    m_blocksize = blocksize;
    m_mapSize = blocksize;
    m_map = new String[blocksize];
  }

  /**
   * Get the length of the list.
   *
   * @return Number of strings in the list
   */
  public int getLength()
  {
    return m_firstFree;
  }

  /**
   * Get the length of the list.
   *
   * @return Number of strings in the list
   */
  public final int size()
  {
    return m_firstFree;
  }

  /**
   * Append a string onto the vector.
   *
   * @param value Sting to add to the vector
   */
  public final void addElement(String value)
  {

    if ((m_firstFree + 1) >= m_mapSize)
    {
      m_mapSize += m_blocksize;

      String newMap[] = new String[m_mapSize];

      System.arraycopy(m_map, 0, newMap, 0, m_firstFree + 1);

      m_map = newMap;
    }

    m_map[m_firstFree] = value;

    m_firstFree++;
  }

  /**
   * Get the nth element.
   *
   * @param i Index of string to find
   *
   * @return String at given index
   */
  public final String elementAt(int i)
  {
    return m_map[i];
  }

  /**
   * Tell if the table contains the given string.
   *
   * @param s String to look for
   *
   * @return True if the string is in this table
   */
  public final boolean contains(String s)
  {

    if (null == s)
      return false;

    for (int i = 0; i < m_firstFree; i++)
    {
      if (m_map[i].equals(s))
        return true;
    }

    return false;
  }

  /**
   * Tell if the table contains the given string. Ignore case.
   *
   * @param s String to find
   *
   * @return True if the String is in this vector
   */
  public final boolean containsIgnoreCase(String s)
  {

    if (null == s)
      return false;

    for (int i = 0; i < m_firstFree; i++)
    {
      if (m_map[i].equalsIgnoreCase(s))
        return true;
    }

    return false;
  }

  /**
   * Tell if the table contains the given string.
   *
   * @param s String to push into the vector
   */
  public final void push(String s)
  {

    if ((m_firstFree + 1) >= m_mapSize)
    {
      m_mapSize += m_blocksize;

      String newMap[] = new String[m_mapSize];

      System.arraycopy(m_map, 0, newMap, 0, m_firstFree + 1);

      m_map = newMap;
    }

    m_map[m_firstFree] = s;

    m_firstFree++;
  }

  /**
   * Pop the tail of this vector.
   *
   * @return The String last added to this vector or null not found.
   * The string is removed from the vector.
   */
  public final String pop()
  {

    if (m_firstFree <= 0)
      return null;

    m_firstFree--;

    String s = m_map[m_firstFree];

    m_map[m_firstFree] = null;

    return s;
  }

  /**
   * Get the string at the tail of this vector without popping.
   *
   * @return The string at the tail of this vector.
   */
  public final String peek()
  {
    return (m_firstFree <= 0) ? null : m_map[m_firstFree - 1];
  }
}
