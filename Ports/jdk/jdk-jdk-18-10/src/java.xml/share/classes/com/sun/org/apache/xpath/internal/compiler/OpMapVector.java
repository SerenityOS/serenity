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

package com.sun.org.apache.xpath.internal.compiler;

/**
 *
 * Like IntVector, but used only for the OpMap array.  Length of array
 * is kept in the m_lengthPos position of the array.  Only the required methods
 * are in included here.
 * @xsl.usage internal
 */
public class OpMapVector {

 /** Size of blocks to allocate          */
  protected int m_blocksize;

  /** Array of ints          */
  protected int m_map[]; // IntStack is trying to see this directly

  /** Position where size of array is kept          */
  protected int m_lengthPos = 0;

  /** Size of array          */
  protected int m_mapSize;

    /**
   * Construct a OpMapVector, using the given block size.
   *
   * @param blocksize Size of block to allocate
   */
  public OpMapVector(int blocksize, int increaseSize, int lengthPos)
  {

    m_blocksize = increaseSize;
    m_mapSize = blocksize;
    m_lengthPos = lengthPos;
    m_map = new int[blocksize];
  }

  /**
   * Get the nth element.
   *
   * @param i index of object to get
   *
   * @return object at given index
   */
  public final int elementAt(int i)
  {
    return m_map[i];
  }

    /**
   * Sets the component at the specified index of this vector to be the
   * specified object. The previous component at that position is discarded.
   *
   * The index must be a value greater than or equal to 0 and less
   * than the current size of the vector.
   *
   * @param value object to set
   * @param index Index of where to set the object
   */
  public final void setElementAt(int value, int index)
  {
    if (index >= m_mapSize)
    {
      int oldSize = m_mapSize;

      m_mapSize += m_blocksize;

      int newMap[] = new int[m_mapSize];

      System.arraycopy(m_map, 0, newMap, 0, oldSize);

      m_map = newMap;
    }

    m_map[index] = value;
  }


  /*
   * Reset the array to the supplied size.  No checking is done.
   *
   * @param size The size to trim to.
   */
  public final void setToSize(int size) {

    int newMap[] = new int[size];

    System.arraycopy(m_map, 0, newMap, 0, m_map[m_lengthPos]);

    m_mapSize = size;
    m_map = newMap;

  }

}
