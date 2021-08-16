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

/**
 * This is a default implementation of a table that manages mappings from
 * expanded names to expandedNameIDs.
 *
 * %OPT% The performance of the getExpandedTypeID() method is very important
 * to DTM building. To get the best performance out of this class, we implement
 * a simple hash algorithm directly into this class, instead of using the
 * inefficient java.util.Hashtable. The code for the get and put operations
 * are combined in getExpandedTypeID() method to share the same hash calculation
 * code. We only need to implement the rehash() interface which is used to
 * expand the hash table.
 */
public class ExpandedNameTable
{

  /** Array of extended types for this document   */
  private ExtendedType[] m_extendedTypes;

  /** The initial size of the m_extendedTypes array */
  private static int m_initialSize = 128;

  /** Next available extended type   */
  // %REVIEW% Since this is (should be) always equal
  // to the length of m_extendedTypes, do we need this?
  private int m_nextType;

  // These are all the types prerotated, for caller convenience.
  public static final int ELEMENT = ((int)DTM.ELEMENT_NODE) ;
  public static final int ATTRIBUTE = ((int)DTM.ATTRIBUTE_NODE) ;
  public static final int TEXT = ((int)DTM.TEXT_NODE) ;
  public static final int CDATA_SECTION = ((int)DTM.CDATA_SECTION_NODE) ;
  public static final int ENTITY_REFERENCE = ((int)DTM.ENTITY_REFERENCE_NODE) ;
  public static final int ENTITY = ((int)DTM.ENTITY_NODE) ;
  public static final int PROCESSING_INSTRUCTION = ((int)DTM.PROCESSING_INSTRUCTION_NODE) ;
  public static final int COMMENT = ((int)DTM.COMMENT_NODE) ;
  public static final int DOCUMENT = ((int)DTM.DOCUMENT_NODE) ;
  public static final int DOCUMENT_TYPE = ((int)DTM.DOCUMENT_TYPE_NODE) ;
  public static final int DOCUMENT_FRAGMENT =((int)DTM.DOCUMENT_FRAGMENT_NODE) ;
  public static final int NOTATION = ((int)DTM.NOTATION_NODE) ;
  public static final int NAMESPACE = ((int)DTM.NAMESPACE_NODE) ;

  /** Workspace for lookup. NOT THREAD SAFE!
   * */
  ExtendedType hashET = new ExtendedType(-1, "", "");

  /** The array to store the default extended types. */
  private static ExtendedType[] m_defaultExtendedTypes;

  /**
   * The default load factor of the Hashtable.
   * This is used to calcualte the threshold.
   */
  private static float m_loadFactor = 0.75f;

  /**
   * The initial capacity of the hash table. Use a bigger number
   * to avoid the cost of expanding the table.
   */
  private static int m_initialCapacity = 203;

  /**
   * The capacity of the hash table, i.e. the size of the
   * internal HashEntry array.
   */
  private int m_capacity;

  /**
   * The threshold of the hash table, which is equal to capacity * loadFactor.
   * If the number of entries in the hash table is bigger than the threshold,
   * the hash table needs to be expanded.
   */
  private int m_threshold;

  /**
   * The internal array to store the hash entries.
   * Each array member is a slot for a hash bucket.
   */
  private HashEntry[] m_table;

  /**
   * Init default values
   */
  static {
    m_defaultExtendedTypes = new ExtendedType[DTM.NTYPES];

    for (int i = 0; i < DTM.NTYPES; i++)
    {
      m_defaultExtendedTypes[i] = new ExtendedType(i, "", "");
    }
  }

  /**
   * Create an expanded name table.
   */
  public ExpandedNameTable()
  {
    m_capacity = m_initialCapacity;
    m_threshold = (int)(m_capacity * m_loadFactor);
    m_table = new HashEntry[m_capacity];

    initExtendedTypes();
  }


  /**
   *  Initialize the vector of extended types with the
   *  basic DOM node types.
   */
  private void initExtendedTypes()
  {
    m_extendedTypes = new ExtendedType[m_initialSize];
    for (int i = 0; i < DTM.NTYPES; i++) {
        m_extendedTypes[i] = m_defaultExtendedTypes[i];
        m_table[i] = new HashEntry(m_defaultExtendedTypes[i], i, i, null);
    }

    m_nextType = DTM.NTYPES;
  }

  /**
   * Given an expanded name represented by namespace, local name and node type,
   * return an ID.  If the expanded-name does not exist in the internal tables,
   * the entry will be created, and the ID will be returned.  Any additional
   * nodes that are created that have this expanded name will use this ID.
   *
   * @param namespace The namespace
   * @param localName The local name
   * @param type The node type
   *
   * @return the expanded-name id of the node.
   */
  public int getExpandedTypeID(String namespace, String localName, int type)
  {
    return getExpandedTypeID(namespace, localName, type, false);
  }

  /**
   * Given an expanded name represented by namespace, local name and node type,
   * return an ID.  If the expanded-name does not exist in the internal tables,
   * the entry will be created, and the ID will be returned.  Any additional
   * nodes that are created that have this expanded name will use this ID.
   * <p>
   * If searchOnly is true, we will return -1 if the name is not found in the
   * table, otherwise the name is added to the table and the expanded name id
   * of the new entry is returned.
   *
   * @param namespace The namespace
   * @param localName The local name
   * @param type The node type
   * @param searchOnly If it is true, we will only search for the expanded name.
   * -1 is return is the name is not found.
   *
   * @return the expanded-name id of the node.
   */
  public int getExpandedTypeID(String namespace, String localName, int type, boolean searchOnly)
  {
    if (null == namespace)
      namespace = "";
    if (null == localName)
      localName = "";

    // Calculate the hash code
    int hash = type + namespace.hashCode() + localName.hashCode();

    // Redefine the hashET object to represent the new expanded name.
    hashET.redefine(type, namespace, localName, hash);

    // Calculate the index into the HashEntry table.
    int index = hash % m_capacity;
    if (index < 0)
      index = -index;

    // Look up the expanded name in the hash table. Return the id if
    // the expanded name is already in the hash table.
    for (HashEntry e = m_table[index]; e != null; e = e.next)
    {
      if (e.hash == hash && e.key.equals(hashET))
        return e.value;
    }

    if (searchOnly)
    {
      return DTM.NULL;
    }

    // Expand the internal HashEntry array if necessary.
    if (m_nextType > m_threshold) {
      rehash();
      index = hash % m_capacity;
      if (index < 0)
        index = -index;
    }

    // Create a new ExtendedType object
    ExtendedType newET = new ExtendedType(type, namespace, localName, hash);

    // Expand the m_extendedTypes array if necessary.
    if (m_extendedTypes.length == m_nextType) {
        ExtendedType[] newArray = new ExtendedType[m_extendedTypes.length * 2];
        System.arraycopy(m_extendedTypes, 0, newArray, 0,
                         m_extendedTypes.length);
        m_extendedTypes = newArray;
    }

    m_extendedTypes[m_nextType] = newET;

    // Create a new hash entry for the new ExtendedType and put it into
    // the table.
    HashEntry entry = new HashEntry(newET, m_nextType, hash, m_table[index]);
    m_table[index] = entry;

    return m_nextType++;
  }

  /**
   * Increases the capacity of and internally reorganizes the hashtable,
   * in order to accommodate and access its entries more efficiently.
   * This method is called when the number of keys in the hashtable exceeds
   * this hashtable's capacity and load factor.
   */
  private void rehash()
  {
    int oldCapacity = m_capacity;
    HashEntry[] oldTable = m_table;

    int newCapacity = 2 * oldCapacity + 1;
    m_capacity = newCapacity;
    m_threshold = (int)(newCapacity * m_loadFactor);

    m_table = new HashEntry[newCapacity];
    for (int i = oldCapacity-1; i >=0 ; i--)
    {
      for (HashEntry old = oldTable[i]; old != null; )
      {
        HashEntry e = old;
        old = old.next;

        int newIndex = e.hash % newCapacity;
        if (newIndex < 0)
          newIndex = -newIndex;

        e.next = m_table[newIndex];
        m_table[newIndex] = e;
      }
    }
  }

  /**
   * Given a type, return an expanded name ID.Any additional nodes that are
   * created that have this expanded name will use this ID.
   *
   * @return the expanded-name id of the node.
   */
  public int getExpandedTypeID(int type)
  {
    return type;
  }

  /**
   * Given an expanded-name ID, return the local name part.
   *
   * @param ExpandedNameID an ID that represents an expanded-name.
   * @return String Local name of this node, or null if the node has no name.
   */
  public String getLocalName(int ExpandedNameID)
  {
    return m_extendedTypes[ExpandedNameID].getLocalName();
  }

  /**
   * Given an expanded-name ID, return the local name ID.
   *
   * @param ExpandedNameID an ID that represents an expanded-name.
   * @return The id of this local name.
   */
  public final int getLocalNameID(int ExpandedNameID)
  {
    // ExtendedType etype = m_extendedTypes[ExpandedNameID];
    if (m_extendedTypes[ExpandedNameID].getLocalName().equals(""))
      return 0;
    else
    return ExpandedNameID;
  }


  /**
   * Given an expanded-name ID, return the namespace URI part.
   *
   * @param ExpandedNameID an ID that represents an expanded-name.
   * @return String URI value of this node's namespace, or null if no
   * namespace was resolved.
   */
  public String getNamespace(int ExpandedNameID)
  {
    String namespace = m_extendedTypes[ExpandedNameID].getNamespace();
    return (namespace.equals("") ? null : namespace);
  }

  /**
   * Given an expanded-name ID, return the namespace URI ID.
   *
   * @param ExpandedNameID an ID that represents an expanded-name.
   * @return The id of this namespace.
   */
  public final int getNamespaceID(int ExpandedNameID)
  {
    //ExtendedType etype = m_extendedTypes[ExpandedNameID];
    if (m_extendedTypes[ExpandedNameID].getNamespace().equals(""))
      return 0;
    else
    return ExpandedNameID;
  }

  /**
   * Given an expanded-name ID, return the local name ID.
   *
   * @param ExpandedNameID an ID that represents an expanded-name.
   * @return The id of this local name.
   */
  public final short getType(int ExpandedNameID)
  {
    //ExtendedType etype = m_extendedTypes[ExpandedNameID];
    return (short)m_extendedTypes[ExpandedNameID].getNodeType();
  }

  /**
   * Return the size of the ExpandedNameTable
   *
   * @return The size of the ExpandedNameTable
   */
  public int getSize()
  {
    return m_nextType;
  }

  /**
   * Return the array of extended types
   *
   * @return The array of extended types
   */
  public ExtendedType[] getExtendedTypes()
  {
    return m_extendedTypes;
  }

  /**
   * Inner class which represents a hash table entry.
   * The field next points to the next entry which is hashed into
   * the same bucket in the case of "hash collision".
   */
  private static final class HashEntry
  {
    ExtendedType key;
    int value;
    int hash;
    HashEntry next;

    protected HashEntry(ExtendedType key, int value, int hash, HashEntry next)
    {
      this.key = key;
      this.value = value;
      this.hash = hash;
      this.next = next;
    }
  }

}
