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

import javax.xml.transform.SourceLocator;

/**
 * <code>NodeLocator</code> maintains information on an XML source
 * node.
 *
 * @author <a href="mailto:ovidiu@cup.hp.com">Ovidiu Predescu</a>
 * @since May 23, 2001
 */
public class NodeLocator implements SourceLocator
{
  protected String m_publicId;
  protected String m_systemId;
  protected int m_lineNumber;
  protected int m_columnNumber;

  /**
   * Creates a new <code>NodeLocator</code> instance.
   *
   * @param publicId a <code>String</code> value
   * @param systemId a <code>String</code> value
   * @param lineNumber an <code>int</code> value
   * @param columnNumber an <code>int</code> value
   */
  public NodeLocator(String publicId, String systemId,
                     int lineNumber, int columnNumber)
  {
    this.m_publicId = publicId;
    this.m_systemId = systemId;
    this.m_lineNumber = lineNumber;
    this.m_columnNumber = columnNumber;
  }

  /**
   * <code>getPublicId</code> returns the public ID of the node.
   *
   * @return a <code>String</code> value
   */
  public String getPublicId()
  {
    return m_publicId;
  }

  /**
   * <code>getSystemId</code> returns the system ID of the node.
   *
   * @return a <code>String</code> value
   */
  public String getSystemId()
  {
    return m_systemId;
  }

  /**
   * <code>getLineNumber</code> returns the line number of the node.
   *
   * @return an <code>int</code> value
   */
  public int getLineNumber()
  {
    return m_lineNumber;
  }

  /**
   * <code>getColumnNumber</code> returns the column number of the
   * node.
   *
   * @return an <code>int</code> value
   */
  public int getColumnNumber()
  {
    return m_columnNumber;
  }

  /**
   * <code>toString</code> returns a string representation of this
   * NodeLocator instance.
   *
   * @return a <code>String</code> value
   */
  public String toString()
  {
    return "file '" + m_systemId
      + "', line #" + m_lineNumber
      + ", column #" + m_columnNumber;
  }
}
