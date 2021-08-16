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

import com.sun.org.apache.xalan.internal.res.XSLMessages;
import com.sun.org.apache.xml.internal.utils.FastStringBuffer;
import com.sun.org.apache.xpath.internal.res.XPATHErrorResources;


/**
 * This class will wrap a FastStringBuffer and allow for
 */
public class XStringForChars extends XString
{
    static final long serialVersionUID = -2235248887220850467L;
  /** The start position in the fsb. */
  int m_start;

  /** The length of the string. */
  int m_length;

  protected String m_strCache = null;

  /**
   * Construct a XNodeSet object.
   *
   * @param val FastStringBuffer object this will wrap, must be non-null.
   * @param start The start position in the array.
   * @param length The number of characters to read from the array.
   */
  public XStringForChars(char[] val, int start, int length)
  {
    super(val);
    m_start = start;
    m_length = length;
    if(null == val)
      throw new IllegalArgumentException(
                          XSLMessages.createXPATHMessage(XPATHErrorResources.ER_FASTSTRINGBUFFER_CANNOT_BE_NULL, null)); //"The FastStringBuffer argument can not be null!!");
  }


  /**
   * Construct a XNodeSet object.
   *
   * @param val String object this will wrap.
   */
  private XStringForChars(String val)
  {
    super(val);
    throw new IllegalArgumentException(
                      XSLMessages.createXPATHMessage(XPATHErrorResources.ER_XSTRINGFORCHARS_CANNOT_TAKE_STRING, null)); //"XStringForChars can not take a string for an argument!");
  }

  /**
   * Cast result object to a string.
   *
   * @return The string this wraps or the empty string if null
   */
  public FastStringBuffer fsb()
  {
    throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_FSB_NOT_SUPPORTED_XSTRINGFORCHARS, null)); //"fsb() not supported for XStringForChars!");
  }

  /**
   * Cast result object to a string.
   *
   * @return The string this wraps or the empty string if null
   */
  public void appendToFsb(com.sun.org.apache.xml.internal.utils.FastStringBuffer fsb)
  {
    fsb.append((char[])m_obj, m_start, m_length);
  }


  /**
   * Tell if this object contains a java String object.
   *
   * @return true if this XMLString can return a string without creating one.
   */
  public boolean hasString()
  {
    return (null != m_strCache);
  }


  /**
   * Cast result object to a string.
   *
   * @return The string this wraps or the empty string if null
   */
  public String str()
  {
    if(null == m_strCache)
      m_strCache = new String((char[])m_obj, m_start, m_length);

    return m_strCache;
  }


  /**
   * Since this object is incomplete without the length and the offset, we
   * have to convert to a string when this function is called.
   *
   * @return The java String representation of this object.
   */
  public Object object()
  {
    return str();
  }

  /**
   * Directly call the
   * characters method on the passed ContentHandler for the
   * string-value. Multiple calls to the
   * ContentHandler's characters methods may well occur for a single call to
   * this method.
   *
   * @param ch A non-null reference to a ContentHandler.
   *
   * @throws org.xml.sax.SAXException
   */
  public void dispatchCharactersEvents(org.xml.sax.ContentHandler ch)
      throws org.xml.sax.SAXException
  {
    ch.characters((char[])m_obj, m_start, m_length);
  }

  /**
   * Directly call the
   * comment method on the passed LexicalHandler for the
   * string-value.
   *
   * @param lh A non-null reference to a LexicalHandler.
   *
   * @throws org.xml.sax.SAXException
   */
  public void dispatchAsComment(org.xml.sax.ext.LexicalHandler lh)
      throws org.xml.sax.SAXException
  {
    lh.comment((char[])m_obj, m_start, m_length);
  }

  /**
   * Returns the length of this string.
   *
   * @return  the length of the sequence of characters represented by this
   *          object.
   */
  public int length()
  {
    return m_length;
  }

  /**
   * Returns the character at the specified index. An index ranges
   * from <code>0</code> to <code>length() - 1</code>. The first character
   * of the sequence is at index <code>0</code>, the next at index
   * <code>1</code>, and so on, as for array indexing.
   *
   * @param      index   the index of the character.
   * @return     the character at the specified index of this string.
   *             The first character is at index <code>0</code>.
   * @exception  IndexOutOfBoundsException  if the <code>index</code>
   *             argument is negative or not less than the length of this
   *             string.
   */
  public char charAt(int index)
  {
    return ((char[])m_obj)[index+m_start];
  }

  /**
   * Copies characters from this string into the destination character
   * array.
   *
   * @param      srcBegin   index of the first character in the string
   *                        to copy.
   * @param      srcEnd     index after the last character in the string
   *                        to copy.
   * @param      dst        the destination array.
   * @param      dstBegin   the start offset in the destination array.
   * @exception IndexOutOfBoundsException If any of the following
   *            is true:
   *            <ul><li><code>srcBegin</code> is negative.
   *            <li><code>srcBegin</code> is greater than <code>srcEnd</code>
   *            <li><code>srcEnd</code> is greater than the length of this
   *                string
   *            <li><code>dstBegin</code> is negative
   *            <li><code>dstBegin+(srcEnd-srcBegin)</code> is larger than
   *                <code>dst.length</code></ul>
   * @exception NullPointerException if <code>dst</code> is <code>null</code>
   */
  public void getChars(int srcBegin, int srcEnd, char dst[], int dstBegin)
  {
    System.arraycopy((char[])m_obj, m_start+srcBegin, dst, dstBegin, srcEnd);
  }

}
