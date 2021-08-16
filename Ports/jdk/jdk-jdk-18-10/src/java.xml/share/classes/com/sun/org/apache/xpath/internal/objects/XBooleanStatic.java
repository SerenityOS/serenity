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

package com.sun.org.apache.xpath.internal.objects;

/**
 * This class doesn't have any XPathContext, so override
 * whatever to ensure it works OK.
 * @xsl.usage internal
 */
public class XBooleanStatic extends XBoolean
{
    static final long serialVersionUID = -8064147275772687409L;

  /** The value of the object.
   *  @serial          */
  private final boolean m_val;

  /**
   * Construct a XBooleanStatic object.
   *
   * @param b The value of the object
   */
  public XBooleanStatic(boolean b)
  {

    super(b);

    m_val = b;
  }

  /**
   * Tell if two objects are functionally equal.
   *
   * @param obj2 Object to compare to this
   *
   * @return True if the two objects are equal
   *
   * @throws javax.xml.transform.TransformerException
   */
  public boolean equals(XObject obj2)
  {
    try
    {
      return m_val == obj2.bool();
    }
    catch(javax.xml.transform.TransformerException te)
    {
      throw new com.sun.org.apache.xml.internal.utils.WrappedRuntimeException(te);
    }
  }
}
