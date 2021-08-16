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

/**
 * A representation of a namespace.  One of these will
 * be pushed on the namespace stack for each
 * element.
 * @xsl.usage advanced
 */
public class NameSpace implements Serializable
{
    static final long serialVersionUID = 1471232939184881839L;

  /** Next NameSpace element on the stack.
   *  @serial             */
  public NameSpace m_next = null;

  /** Prefix of this NameSpace element.
   *  @serial          */
  public String m_prefix;

  /** Namespace URI of this NameSpace element.
   *  @serial           */
  public String m_uri;  // if null, then Element namespace is empty.

  /**
   * Construct a namespace for placement on the
   * result tree namespace stack.
   *
   * @param prefix Prefix of this element
   * @param uri URI of  this element
   */
  public NameSpace(String prefix, String uri)
  {
    m_prefix = prefix;
    m_uri = uri;
  }
}
