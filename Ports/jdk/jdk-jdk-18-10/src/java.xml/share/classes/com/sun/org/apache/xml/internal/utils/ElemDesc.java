/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * $Id: ElemDesc.java,v 1.2.4.1 2005/09/15 08:15:44 suresh_emailid Exp $
 */
package com.sun.org.apache.xml.internal.utils;

import java.util.HashMap;
import java.util.Map;


/**
 * This class is in support of SerializerToHTML, and acts as a sort
 * of element representative for HTML elements.
 * @xsl.usage internal
 */
class ElemDesc
{

  /** Table of attributes for the element */
  Map<String, Integer> m_attrs = null;

  /** Element's flags, describing the role this element plays during
   * formatting of the document. This is used as a bitvector; more than one flag
   * may be set at a time, bitwise-ORed together. Mnemonic and bits
   * have been assigned to the flag values. NOTE: Some bits are
   * currently assigned multiple mnemonics; it is the caller's
   * responsibility to disambiguate these if necessary. */
  int m_flags;

  /** Defines mnemonic and bit-value for the EMPTY flag */
  static final int EMPTY = (1 << 1);

  /** Defines mnemonic and bit-value for the FLOW flag  */
  static final int FLOW = (1 << 2);

  /** Defines mnemonic and bit-value for the BLOCK flag          */
  static final int BLOCK = (1 << 3);

  /** Defines mnemonic and bit-value for the BLOCKFORM  flag         */
  static final int BLOCKFORM = (1 << 4);

  /** Defines mnemonic and bit-value for the BLOCKFORMFIELDSET flag          */
  static final int BLOCKFORMFIELDSET = (1 << 5);

  /** Defines mnemonic and bit-value for the CDATA flag         */
  static final int CDATA = (1 << 6);

  /** Defines mnemonic and bit-value for the PCDATA flag          */
  static final int PCDATA = (1 << 7);

  /** Defines mnemonic and bit-value for the RAW flag         */
  static final int RAW = (1 << 8);

  /** Defines mnemonic and bit-value for the INLINE flag          */
  static final int INLINE = (1 << 9);

  /** Defines mnemonic and bit-value for the INLINEA flag          */
  static final int INLINEA = (1 << 10);

  /** Defines mnemonic and bit-value for the INLINELABEL flag          */
  static final int INLINELABEL = (1 << 11);

  /** Defines mnemonic and bit-value for the FONTSTYLE flag          */
  static final int FONTSTYLE = (1 << 12);

  /** Defines mnemonic and bit-value for the PHRASE flag          */
  static final int PHRASE = (1 << 13);

  /** Defines mnemonic and bit-value for the FORMCTRL flag         */
  static final int FORMCTRL = (1 << 14);

  /** Defines mnemonic and bit-value for the SPECIAL flag         */
  static final int SPECIAL = (1 << 15);

  /** Defines mnemonic and bit-value for the ASPECIAL flag         */
  static final int ASPECIAL = (1 << 16);

  /** Defines mnemonic and bit-value for the HEADMISC flag         */
  static final int HEADMISC = (1 << 17);

  /** Defines mnemonic and bit-value for the HEAD flag         */
  static final int HEAD = (1 << 18);

  /** Defines mnemonic and bit-value for the LIST flag         */
  static final int LIST = (1 << 19);

  /** Defines mnemonic and bit-value for the PREFORMATTED flag         */
  static final int PREFORMATTED = (1 << 20);

  /** Defines mnemonic and bit-value for the WHITESPACESENSITIVE flag         */
  static final int WHITESPACESENSITIVE = (1 << 21);

  /** Defines mnemonic and bit-value for the ATTRURL flag         */
  static final int ATTRURL = (1 << 1);

  /** Defines mnemonic and bit-value for the ATTREMPTY flag         */
  static final int ATTREMPTY = (1 << 2);

  /**
   * Construct an ElementDescription with an initial set of flags.
   *
   * @param flags Element flags
   * @see m_flags
   */
  ElemDesc(int flags)
  {
    m_flags = flags;
  }

  /**
   * "is (this element described by these flags)".
   *
   * This might more properly be called areFlagsSet(). It accepts an
   * integer (being used as a bitvector) and checks whether all the
   * corresponding bits are set in our internal flags. Note that this
   * test is performed as a bitwise AND, not an equality test, so a
   * 0 bit in the input means "don't test", not "must be set false".
   *
   * @param flags Vector of flags to compare against this element's flags
   *
   * @return true if the flags set in the parameter are also set in the
   * element's stored flags.
   *
   * @see m_flags
   * @see isAttrFlagSet
   */
  boolean is(int flags)
  {
    // int which = (m_flags & flags);
    return (m_flags & flags) != 0;
  }

  /**
   * Set a new attribute for this element
   *
   *
   * @param name Attribute name
   * @param flags Attibute flags
   */
  void setAttr(String name, int flags)
  {

    if (null == m_attrs)
      m_attrs = new HashMap<>();

    m_attrs.put(name, flags);
  }

  /**
   * Find out if a flag is set in a given attribute of this element
   *
   *
   * @param name Attribute name
   * @param flags Flag to check
   *
   * @return True if the flag is set in the attribute. Returns false
   * if the attribute is not found
   * @see m_flags
   */
  boolean isAttrFlagSet(String name, int flags)
  {

    if (null != m_attrs)
    {
      Integer _flags = m_attrs.get(name);

      if (null != _flags)
      {
        return (_flags & flags) != 0;
      }
    }

    return false;
  }
}
