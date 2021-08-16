/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * This class represents an XPath boolean object, and is capable of
 * converting the boolean to other types, such as a string.
 * @xsl.usage advanced
 */
public class XBoolean extends XObject
{
    static final long serialVersionUID = -2964933058866100881L;

  /**
   * A true boolean object so we don't have to keep creating them.
   * @xsl.usage internal
   */
  public static final XBoolean S_TRUE = new XBooleanStatic(true);

  /**
   * A true boolean object so we don't have to keep creating them.
   * @xsl.usage internal
   */
  public static final XBoolean S_FALSE = new XBooleanStatic(false);

  /** Value of the object.
   *  @serial         */
  private final boolean m_val;

  /**
   * Construct a XBoolean object.
   *
   * @param b Value of the boolean object
   */
  public XBoolean(boolean b)
  {

    super();

    m_val = b;
  }

  /**
   * Construct a XBoolean object.
   *
   * @param b Value of the boolean object
   */
  public XBoolean(Boolean b)
  {

    super();

    m_val = b.booleanValue();
    setObject(b);
  }


  /**
   * Tell that this is a CLASS_BOOLEAN.
   *
   * @return type of CLASS_BOOLEAN
   */
  public int getType()
  {
    return CLASS_BOOLEAN;
  }

  /**
   * Given a request type, return the equivalent string.
   * For diagnostic purposes.
   *
   * @return type string "#BOOLEAN"
   */
  public String getTypeString()
  {
    return "#BOOLEAN";
  }

  /**
   * Cast result object to a number.
   *
   * @return numeric value of the object value
   */
  public double num()
  {
    return m_val ? 1.0 : 0.0;
  }

  /**
   * Cast result object to a boolean.
   *
   * @return The object value as a boolean
   */
  public boolean bool()
  {
    return m_val;
  }

  /**
   * Cast result object to a string.
   *
   * @return The object's value as a string
   */
  public String str()
  {
    return m_val ? "true" : "false";
  }

  /**
   * Return a java object that's closest to the representation
   * that should be handed to an extension.
   *
   * @return The object's value as a java object
   */
  public Object object()
  {
    if(null == m_obj)
      setObject(m_val);
    return m_obj;
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

    // In order to handle the 'all' semantics of
    // nodeset comparisons, we always call the
    // nodeset function.
    if (obj2.getType() == XObject.CLASS_NODESET)
      return obj2.equals(this);

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
