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
 * The default implementation of XMLStringFactory.
 * This implementation creates XMLStringDefault objects.
 */
public class XMLStringFactoryDefault extends XMLStringFactory
{
  // A constant representing the empty String
  private static final XMLStringDefault EMPTY_STR = new XMLStringDefault("");

  /**
   * Create a new XMLString from a Java string.
   *
   *
   * @param string Java String reference, which must be non-null.
   *
   * @return An XMLString object that wraps the String reference.
   */
  public XMLString newstr(String string)
  {
    return new XMLStringDefault(string);
  }

  /**
   * Create a XMLString from a FastStringBuffer.
   *
   *
   * @param fsb FastStringBuffer reference, which must be non-null.
   * @param start The start position in the array.
   * @param length The number of characters to read from the array.
   *
   * @return An XMLString object that wraps the FastStringBuffer reference.
   */
  public XMLString newstr(FastStringBuffer fsb, int start, int length)
  {
    return new XMLStringDefault(fsb.getString(start, length));
  }

  /**
   * Create a XMLString from a FastStringBuffer.
   *
   *
   * @param string FastStringBuffer reference, which must be non-null.
   * @param start The start position in the array.
   * @param length The number of characters to read from the array.
   *
   * @return An XMLString object that wraps the FastStringBuffer reference.
   */
  public XMLString newstr(char[] string, int start, int length)
  {
    return new XMLStringDefault(new String(string, start, length));
  }

  /**
   * Get a cheap representation of an empty string.
   *
   * @return An non-null reference to an XMLString that represents "".
   */
  public XMLString emptystr()
  {
    return EMPTY_STR;
  }
}
