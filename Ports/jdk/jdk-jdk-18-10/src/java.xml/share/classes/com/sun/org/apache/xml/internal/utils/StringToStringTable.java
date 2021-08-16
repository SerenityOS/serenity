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
 * A very simple lookup table that stores a list of strings, the even
 * number strings being keys, and the odd number strings being values.
 * @xsl.usage internal
 */
public class StringToStringTable
{

  /** Size of blocks to allocate          */
  private int m_blocksize;

  /** Array of strings this contains          */
  private String m_map[];

  /** Number of strings this contains           */
  private int m_firstFree = 0;

  /** Size of this table           */
  private int m_mapSize;

  /**
   * Default constructor.  Note that the default
   * block size is very small, for small lists.
   */
  public StringToStringTable()
  {

    m_blocksize = 16;
    m_mapSize = m_blocksize;
    m_map = new String[m_blocksize];
  }

  /**
   * Construct a StringToStringTable, using the given block size.
   *
   * @param blocksize Size of blocks to allocate
   */
  public StringToStringTable(int blocksize)
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
  public final int getLength()
  {
    return m_firstFree;
  }

  /**
   * Append a string onto the vector.
   * The strings go to the even locations in the array
   * and the values in the odd.
   *
   * @param key String to add to the list
   * @param value Value of the string
   */
  public final void put(String key, String value)
  {

    if ((m_firstFree + 2) >= m_mapSize)
    {
      m_mapSize += m_blocksize;

      String newMap[] = new String[m_mapSize];

      System.arraycopy(m_map, 0, newMap, 0, m_firstFree + 1);

      m_map = newMap;
    }

    m_map[m_firstFree] = key;

    m_firstFree++;

    m_map[m_firstFree] = value;

    m_firstFree++;
  }

  /**
   * Tell if the table contains the given string.
   *
   * @param key String to look up
   *
   * @return return the value of the string or null if not found.
   */
  public final String get(String key)
  {

    for (int i = 0; i < m_firstFree; i += 2)
    {
      if (m_map[i].equals(key))
        return m_map[i + 1];
    }

    return null;
  }

  /**
   * Remove the given string and its value from this table.
   *
   * @param key String to remove from the table
   */
  public final void remove(String key)
  {

    for (int i = 0; i < m_firstFree; i += 2)
    {
      if (m_map[i].equals(key))
      {
        if ((i + 2) < m_firstFree)
          System.arraycopy(m_map, i + 2, m_map, i, m_firstFree - (i + 2));

        m_firstFree -= 2;
        m_map[m_firstFree] = null;
        m_map[m_firstFree + 1] = null;

        break;
      }
    }
  }

  /**
   * Tell if the table contains the given string. Ignore case
   *
   * @param key String to look up
   *
   * @return The value of the string or null if not found
   */
  public final String getIgnoreCase(String key)
  {

    if (null == key)
      return null;

    for (int i = 0; i < m_firstFree; i += 2)
    {
      if (m_map[i].equalsIgnoreCase(key))
        return m_map[i + 1];
    }

    return null;
  }

  /**
   * Tell if the table contains the given string in the value.
   *
   * @param val Value of the string to look up
   *
   * @return the string associated with the given value or null if not found
   */
  public final String getByValue(String val)
  {

    for (int i = 1; i < m_firstFree; i += 2)
    {
      if (m_map[i].equals(val))
        return m_map[i - 1];
    }

    return null;
  }

  /**
   * Get the nth element.
   *
   * @param i index of the string to look up.
   *
   * @return The string at the given index.
   */
  public final String elementAt(int i)
  {
    return m_map[i];
  }

  /**
   * Tell if the table contains the given string.
   *
   * @param key String to look up
   *
   * @return True if the given string is in this table
   */
  public final boolean contains(String key)
  {

    for (int i = 0; i < m_firstFree; i += 2)
    {
      if (m_map[i].equals(key))
        return true;
    }

    return false;
  }

  /**
   * Tell if the table contains the given string.
   *
   * @param val value to look up
   *
   * @return True if the given value is in the table.
   */
  public final boolean containsValue(String val)
  {

    for (int i = 1; i < m_firstFree; i += 2)
    {
      if (m_map[i].equals(val))
        return true;
    }

    return false;
  }
}
