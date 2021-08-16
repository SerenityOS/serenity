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

package com.sun.org.apache.xalan.internal.lib;

import com.sun.org.apache.xml.internal.dtm.ref.DTMNodeProxy;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The base class for some EXSLT extension classes.
 * It contains common utility methods to be used by the sub-classes.
 */
public abstract class ExsltBase
{
  /**
   * Return the string value of a Node
   *
   * @param n The Node.
   * @return The string value of the Node
   */
  protected static String toString(Node n)
  {
    if (n instanceof DTMNodeProxy)
         return ((DTMNodeProxy)n).getStringValue();
    else
    {
      String value = n.getNodeValue();
      if (value == null)
      {
        NodeList nodelist = n.getChildNodes();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < nodelist.getLength(); i++)
        {
          Node childNode = nodelist.item(i);
          buf.append(toString(childNode));
        }
        return buf.toString();
      }
      else
        return value;
    }
  }

  /**
   * Convert the string value of a Node to a number.
   * Return NaN if the string is not a valid number.
   *
   * @param n The Node.
   * @return The number value of the Node
   */
  protected static double toNumber(Node n)
  {
    double d = 0.0;
    String str = toString(n);
    try
    {
      d = Double.valueOf(str).doubleValue();
    }
    catch (NumberFormatException e)
    {
      d= Double.NaN;
    }
    return d;
  }
}
