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
import com.sun.org.apache.xml.internal.utils.XMLCharacterRecognizer;
import com.sun.org.apache.xml.internal.utils.XMLString;
import com.sun.org.apache.xml.internal.utils.XMLStringFactory;
import com.sun.org.apache.xpath.internal.res.XPATHErrorResources;

/**
 * This class will wrap a FastStringBuffer and allow for
 */
public class XStringForFSB extends XString
{
    static final long serialVersionUID = -1533039186550674548L;

  /** The start position in the fsb. */
  int m_start;

  /** The length of the string. */
  int m_length;

  /** If the str() function is called, the string will be cached here. */
  protected String m_strCache = null;

  /** cached hash code */
  protected int m_hash = 0;

  /**
   * Construct a XNodeSet object.
   *
   * @param val FastStringBuffer object this will wrap, must be non-null.
   * @param start The start position in the array.
   * @param length The number of characters to read from the array.
   */
  public XStringForFSB(FastStringBuffer val, int start, int length)
  {

    super(val);

    m_start = start;
    m_length = length;

    if (null == val)
      throw new IllegalArgumentException(
        XSLMessages.createXPATHMessage(XPATHErrorResources.ER_FASTSTRINGBUFFER_CANNOT_BE_NULL, null));
  }

  /**
   * Construct a XNodeSet object.
   *
   * @param val String object this will wrap.
   */
  private XStringForFSB(String val)
  {

    super(val);

    throw new IllegalArgumentException(
      XSLMessages.createXPATHMessage(XPATHErrorResources.ER_FSB_CANNOT_TAKE_STRING, null)); // "XStringForFSB can not take a string for an argument!");
  }

  /**
   * Cast result object to a string.
   *
   * @return The string this wraps or the empty string if null
   */
  public FastStringBuffer fsb()
  {
    return ((FastStringBuffer) m_obj);
  }

  /**
   * Cast result object to a string.
   *
   * @return The string this wraps or the empty string if null
   */
  public void appendToFsb(com.sun.org.apache.xml.internal.utils.FastStringBuffer fsb)
  {
    // %OPT% !!! FSB has to be updated to take partial fsb's for append.
    fsb.append(str());
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

//  /** NEEDSDOC Field strCount */
//  public static int strCount = 0;
//
//  /** NEEDSDOC Field xtable */
//  static java.util.Hashtable xtable = new java.util.Hashtable();

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
   * Cast result object to a string.
   *
   * @return The string this wraps or the empty string if null
   */
  public String str()
  {

    if (null == m_strCache)
    {
      m_strCache = fsb().getString(m_start, m_length);

//      strCount++;
//
//      RuntimeException e = new RuntimeException("Bad!  Bad!");
//      java.io.CharArrayWriter writer = new java.io.CharArrayWriter();
//      java.io.PrintWriter pw = new java.io.PrintWriter(writer);
//
//      e.printStackTrace(pw);
//
//      String str = writer.toString();
//
//      str = str.substring(0, 600);
//
//      if (null == xtable.get(str))
//      {
//        xtable.put(str, str);
//        System.out.println(str);
//      }
//      System.out.println("strCount: " + strCount);

//      throw e;
//      e.printStackTrace();
      // System.exit(-1);
    }

    return m_strCache;
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
    fsb().sendSAXcharacters(ch, m_start, m_length);
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
    fsb().sendSAXComment(lh, m_start, m_length);
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
    return fsb().charAt(m_start + index);
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

    // %OPT% Need to call this on FSB when it is implemented.
    // %UNTESTED% (I don't think anyone calls this yet?)
    int n = srcEnd - srcBegin;

    if (n > m_length)
      n = m_length;

    if (n > (dst.length - dstBegin))
      n = (dst.length - dstBegin);

    int end = srcBegin + m_start + n;
    int d = dstBegin;
    FastStringBuffer fsb = fsb();

    for (int i = srcBegin + m_start; i < end; i++)
    {
      dst[d++] = fsb.charAt(i);
    }
  }

  /**
   * Compares this string to the specified object.
   * The result is <code>true</code> if and only if the argument is not
   * <code>null</code> and is a <code>String</code> object that represents
   * the same sequence of characters as this object.
   *
   * @param   obj2       the object to compare this <code>String</code>
   *                     against.
   *
   * @return  <code>true</code> if the <code>String </code>are equal;
   *          <code>false</code> otherwise.
   * @see     java.lang.String#compareTo(java.lang.String)
   * @see     java.lang.String#equalsIgnoreCase(java.lang.String)
   */
  public boolean equals(XMLString obj2)
  {

    if (this == obj2)
    {
      return true;
    }

    int n = m_length;

    if (n == obj2.length())
    {
      FastStringBuffer fsb = fsb();
      int i = m_start;
      int j = 0;

      while (n-- != 0)
      {
        if (fsb.charAt(i) != obj2.charAt(j))
        {
          return false;
        }

        i++;
        j++;
      }

      return true;
    }

    return false;
  }

  /**
   * Tell if two objects are functionally equal.
   *
   * @param obj2 Object to compare this to
   *
   * @return true if the two objects are equal
   *
   * @throws javax.xml.transform.TransformerException
   */
  public boolean equals(XObject obj2)
  {

    if (this == obj2)
    {
      return true;
    }
    if(obj2.getType() == XObject.CLASS_NUMBER)
        return obj2.equals(this);

    String str = obj2.str();
    int n = m_length;

    if (n == str.length())
    {
      FastStringBuffer fsb = fsb();
      int i = m_start;
      int j = 0;

      while (n-- != 0)
      {
        if (fsb.charAt(i) != str.charAt(j))
        {
          return false;
        }

        i++;
        j++;
      }

      return true;
    }

    return false;
  }

  /**
   * Tell if two objects are functionally equal.
   *
   * @param anotherString Object to compare this to
   *
   * @return true if the two objects are equal
   *
   * @throws javax.xml.transform.TransformerException
   */
  public boolean equals(String anotherString)
  {

    int n = m_length;

    if (n == anotherString.length())
    {
      FastStringBuffer fsb = fsb();
      int i = m_start;
      int j = 0;

      while (n-- != 0)
      {
        if (fsb.charAt(i) != anotherString.charAt(j))
        {
          return false;
        }

        i++;
        j++;
      }

      return true;
    }

    return false;
  }

  /**
   * Compares this string to the specified object.
   * The result is <code>true</code> if and only if the argument is not
   * <code>null</code> and is a <code>String</code> object that represents
   * the same sequence of characters as this object.
   *
   * @param   obj2       the object to compare this <code>String</code>
   *                     against.
   *
   * @return  <code>true</code> if the <code>String </code>are equal;
   *          <code>false</code> otherwise.
   * @see     java.lang.String#compareTo(java.lang.String)
   * @see     java.lang.String#equalsIgnoreCase(java.lang.String)
   */
  public boolean equals(Object obj2)
  {

    if (null == obj2)
      return false;

    if(obj2 instanceof XNumber)
        return obj2.equals(this);

      // In order to handle the 'all' semantics of
      // nodeset comparisons, we always call the
      // nodeset function.
    else if (obj2 instanceof XNodeSet)
      return obj2.equals(this);
    else if (obj2 instanceof XStringForFSB)
      return equals((XMLString) obj2);
    else
      return equals(obj2.toString());
  }

  /**
   * Compares this <code>String</code> to another <code>String</code>,
   * ignoring case considerations.  Two strings are considered equal
   * ignoring case if they are of the same length, and corresponding
   * characters in the two strings are equal ignoring case.
   *
   * @param   anotherString   the <code>String</code> to compare this
   *                          <code>String</code> against.
   * @return  <code>true</code> if the argument is not <code>null</code>
   *          and the <code>String</code>s are equal,
   *          ignoring case; <code>false</code> otherwise.
   * @see     #equals(Object)
   * @see     java.lang.Character#toLowerCase(char)
   * @see java.lang.Character#toUpperCase(char)
   */
  public boolean equalsIgnoreCase(String anotherString)
  {
    return (m_length == anotherString.length())
           ? str().equalsIgnoreCase(anotherString) : false;
  }

  /**
   * Compares two strings lexicographically.
   *
   * @param   xstr   the <code>String</code> to be compared.
   *
   * @return  the value <code>0</code> if the argument string is equal to
   *          this string; a value less than <code>0</code> if this string
   *          is lexicographically less than the string argument; and a
   *          value greater than <code>0</code> if this string is
   *          lexicographically greater than the string argument.
   * @exception java.lang.NullPointerException if <code>anotherString</code>
   *          is <code>null</code>.
   */
  public int compareTo(XMLString xstr)
  {

    int len1 = m_length;
    int len2 = xstr.length();
    int n = Math.min(len1, len2);
    FastStringBuffer fsb = fsb();
    int i = m_start;
    int j = 0;

    while (n-- != 0)
    {
      char c1 = fsb.charAt(i);
      char c2 = xstr.charAt(j);

      if (c1 != c2)
      {
        return c1 - c2;
      }

      i++;
      j++;
    }

    return len1 - len2;
  }

  /**
   * Compares two strings lexicographically, ignoring case considerations.
   * This method returns an integer whose sign is that of
   * <code>this.toUpperCase().toLowerCase().compareTo(
   * str.toUpperCase().toLowerCase())</code>.
   * <p>
   * Note that this method does <em>not</em> take locale into account,
   * and will result in an unsatisfactory ordering for certain locales.
   * The java.text package provides <em>collators</em> to allow
   * locale-sensitive ordering.
   *
   * @param   xstr   the <code>String</code> to be compared.
   *
   * @return  a negative integer, zero, or a positive integer as the
   *          the specified String is greater than, equal to, or less
   *          than this String, ignoring case considerations.
   * @see     java.text.Collator#compare(String, String)
   * @since   1.2
   */
  public int compareToIgnoreCase(XMLString xstr)
  {

    int len1 = m_length;
    int len2 = xstr.length();
    int n = Math.min(len1, len2);
    FastStringBuffer fsb = fsb();
    int i = m_start;
    int j = 0;

    while (n-- != 0)
    {
      char c1 = Character.toLowerCase(fsb.charAt(i));
      char c2 = Character.toLowerCase(xstr.charAt(j));

      if (c1 != c2)
      {
        return c1 - c2;
      }

      i++;
      j++;
    }

    return len1 - len2;
  }

  /**
   * Returns a hashcode for this string. The hashcode for a
   * <code>String</code> object is computed as
   * <blockquote><pre>
   * s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
   * </pre></blockquote>
   * using <code>int</code> arithmetic, where <code>s[i]</code> is the
   * <i>i</i>th character of the string, <code>n</code> is the length of
   * the string, and <code>^</code> indicates exponentiation.
   * (The hash value of the empty string is zero.)
   *
   * @return  a hash code value for this object.
   */
  public int hashCode()
  {
    // Commenting this out because in JDK1.1.8 and VJ++
    // we don't match XMLStrings. Defaulting to the super
    // causes us to create a string, but at this point
    // this only seems to get called in key processing.
    // Maybe we can live with it?

/*
    int h = m_hash;

    if (h == 0)
    {
      int off = m_start;
      int len = m_length;
      FastStringBuffer fsb = fsb();

      for (int i = 0; i < len; i++)
      {
        h = 31 * h + fsb.charAt(off);

        off++;
      }

      m_hash = h;
    }
    */

    return super.hashCode(); // h;
  }

  /**
   * Tests if this string starts with the specified prefix beginning
   * a specified index.
   *
   * @param   prefix    the prefix.
   * @param   toffset   where to begin looking in the string.
   * @return  <code>true</code> if the character sequence represented by the
   *          argument is a prefix of the substring of this object starting
   *          at index <code>toffset</code>; <code>false</code> otherwise.
   *          The result is <code>false</code> if <code>toffset</code> is
   *          negative or greater than the length of this
   *          <code>String</code> object; otherwise the result is the same
   *          as the result of the expression
   *          <pre>
   *          this.subString(toffset).startsWith(prefix)
   *          </pre>
   * @exception java.lang.NullPointerException if <code>prefix</code> is
   *          <code>null</code>.
   */
  public boolean startsWith(XMLString prefix, int toffset)
  {

    FastStringBuffer fsb = fsb();
    int to = m_start + toffset;
    int tlim = m_start + m_length;
    int po = 0;
    int pc = prefix.length();

    // Note: toffset might be near -1>>>1.
    if ((toffset < 0) || (toffset > m_length - pc))
    {
      return false;
    }

    while (--pc >= 0)
    {
      if (fsb.charAt(to) != prefix.charAt(po))
      {
        return false;
      }

      to++;
      po++;
    }

    return true;
  }

  /**
   * Tests if this string starts with the specified prefix.
   *
   * @param   prefix   the prefix.
   * @return  <code>true</code> if the character sequence represented by the
   *          argument is a prefix of the character sequence represented by
   *          this string; <code>false</code> otherwise.
   *          Note also that <code>true</code> will be returned if the
   *          argument is an empty string or is equal to this
   *          <code>String</code> object as determined by the
   *          {@link #equals(Object)} method.
   * @exception java.lang.NullPointerException if <code>prefix</code> is
   *          <code>null</code>.
   * @since   JDK1. 0
   */
  public boolean startsWith(XMLString prefix)
  {
    return startsWith(prefix, 0);
  }

  /**
   * Returns the index within this string of the first occurrence of the
   * specified character. If a character with value <code>ch</code> occurs
   * in the character sequence represented by this <code>String</code>
   * object, then the index of the first such occurrence is returned --
   * that is, the smallest value <i>k</i> such that:
   * <blockquote><pre>
   * this.charAt(<i>k</i>) == ch
   * </pre></blockquote>
   * is <code>true</code>. If no such character occurs in this string,
   * then <code>-1</code> is returned.
   *
   * @param   ch   a character.
   * @return  the index of the first occurrence of the character in the
   *          character sequence represented by this object, or
   *          <code>-1</code> if the character does not occur.
   */
  public int indexOf(int ch)
  {
    return indexOf(ch, 0);
  }

  /**
   * Returns the index within this string of the first occurrence of the
   * specified character, starting the search at the specified index.
   * <p>
   * If a character with value <code>ch</code> occurs in the character
   * sequence represented by this <code>String</code> object at an index
   * no smaller than <code>fromIndex</code>, then the index of the first
   * such occurrence is returned--that is, the smallest value <i>k</i>
   * such that:
   * <blockquote><pre>
   * (this.charAt(<i>k</i>) == ch) && (<i>k</i> >= fromIndex)
   * </pre></blockquote>
   * is true. If no such character occurs in this string at or after
   * position <code>fromIndex</code>, then <code>-1</code> is returned.
   * <p>
   * There is no restriction on the value of <code>fromIndex</code>. If it
   * is negative, it has the same effect as if it were zero: this entire
   * string may be searched. If it is greater than the length of this
   * string, it has the same effect as if it were equal to the length of
   * this string: <code>-1</code> is returned.
   *
   * @param   ch          a character.
   * @param   fromIndex   the index to start the search from.
   * @return  the index of the first occurrence of the character in the
   *          character sequence represented by this object that is greater
   *          than or equal to <code>fromIndex</code>, or <code>-1</code>
   *          if the character does not occur.
   */
  public int indexOf(int ch, int fromIndex)
  {

    int max = m_start + m_length;
    FastStringBuffer fsb = fsb();

    if (fromIndex < 0)
    {
      fromIndex = 0;
    }
    else if (fromIndex >= m_length)
    {

      // Note: fromIndex might be near -1>>>1.
      return -1;
    }

    for (int i = m_start + fromIndex; i < max; i++)
    {
      if (fsb.charAt(i) == ch)
      {
        return i - m_start;
      }
    }

    return -1;
  }

  /**
   * Returns a new string that is a substring of this string. The
   * substring begins with the character at the specified index and
   * extends to the end of this string. <p>
   * Examples:
   * <blockquote><pre>
   * "unhappy".substring(2) returns "happy"
   * "Harbison".substring(3) returns "bison"
   * "emptiness".substring(9) returns "" (an empty string)
   * </pre></blockquote>
   *
   * @param      beginIndex   the beginning index, inclusive.
   * @return     the specified substring.
   * @exception  IndexOutOfBoundsException  if
   *             <code>beginIndex</code> is negative or larger than the
   *             length of this <code>String</code> object.
   */
  public XMLString substring(int beginIndex)
  {

    int len = m_length - beginIndex;

    if (len <= 0)
      return XString.EMPTYSTRING;
    else
    {
      int start = m_start + beginIndex;

      return new XStringForFSB(fsb(), start, len);
    }
  }

  /**
   * Returns a new string that is a substring of this string. The
   * substring begins at the specified <code>beginIndex</code> and
   * extends to the character at index <code>endIndex - 1</code>.
   * Thus the length of the substring is <code>endIndex-beginIndex</code>.
   *
   * @param      beginIndex   the beginning index, inclusive.
   * @param      endIndex     the ending index, exclusive.
   * @return     the specified substring.
   * @exception  IndexOutOfBoundsException  if the
   *             <code>beginIndex</code> is negative, or
   *             <code>endIndex</code> is larger than the length of
   *             this <code>String</code> object, or
   *             <code>beginIndex</code> is larger than
   *             <code>endIndex</code>.
   */
  public XMLString substring(int beginIndex, int endIndex)
  {

    int len = endIndex - beginIndex;

    if (len > m_length)
      len = m_length;

    if (len <= 0)
      return XString.EMPTYSTRING;
    else
    {
      int start = m_start + beginIndex;

      return new XStringForFSB(fsb(), start, len);
    }
  }

  /**
   * Concatenates the specified string to the end of this string.
   *
   * @param   str   the <code>String</code> that is concatenated to the end
   *                of this <code>String</code>.
   * @return  a string that represents the concatenation of this object's
   *          characters followed by the string argument's characters.
   * @exception java.lang.NullPointerException if <code>str</code> is
   *          <code>null</code>.
   */
  public XMLString concat(String str)
  {

    // %OPT% Make an FSB here?
    return new XString(str().concat(str));
  }

  /**
   * Removes white space from both ends of this string.
   *
   * @return  this string, with white space removed from the front and end.
   */
  public XMLString trim()
  {
    return fixWhiteSpace(true, true, false);
  }

  /**
   * Returns whether the specified <var>ch</var> conforms to the XML 1.0 definition
   * of whitespace.  Refer to <A href="http://www.w3.org/TR/1998/REC-xml-19980210#NT-S">
   * the definition of <CODE>S</CODE></A> for details.
   * @param   ch      Character to check as XML whitespace.
   * @return          =true if <var>ch</var> is XML whitespace; otherwise =false.
   */
  private static boolean isSpace(char ch)
  {
    return XMLCharacterRecognizer.isWhiteSpace(ch);  // Take the easy way out for now.
  }

  /**
   * Conditionally trim all leading and trailing whitespace in the specified String.
   * All strings of white space are
   * replaced by a single space character (#x20), except spaces after punctuation which
   * receive double spaces if doublePunctuationSpaces is true.
   * This function may be useful to a formatter, but to get first class
   * results, the formatter should probably do it's own white space handling
   * based on the semantics of the formatting object.
   *
   * @param   trimHead    Trim leading whitespace?
   * @param   trimTail    Trim trailing whitespace?
   * @param   doublePunctuationSpaces    Use double spaces for punctuation?
   * @return              The trimmed string.
   */
  public XMLString fixWhiteSpace(boolean trimHead, boolean trimTail,
                                 boolean doublePunctuationSpaces)
  {

    int end = m_length + m_start;
    char[] buf = new char[m_length];
    FastStringBuffer fsb = fsb();
    boolean edit = false;

    /* replace S to ' '. and ' '+ -> single ' '. */
    int d = 0;
    boolean pres = false;

    for (int s = m_start; s < end; s++)
    {
      char c = fsb.charAt(s);

      if (isSpace(c))
      {
        if (!pres)
        {
          if (' ' != c)
          {
            edit = true;
          }

          buf[d++] = ' ';

          if (doublePunctuationSpaces && (d != 0))
          {
            char prevChar = buf[d - 1];

            if (!((prevChar == '.') || (prevChar == '!')
                  || (prevChar == '?')))
            {
              pres = true;
            }
          }
          else
          {
            pres = true;
          }
        }
        else
        {
          edit = true;
          pres = true;
        }
      }
      else
      {
        buf[d++] = c;
        pres = false;
      }
    }

    if (trimTail && 1 <= d && ' ' == buf[d - 1])
    {
      edit = true;

      d--;
    }

    int start = 0;

    if (trimHead && 0 < d && ' ' == buf[0])
    {
      edit = true;

      start++;
    }

    XMLStringFactory xsf = XMLStringFactoryImpl.getFactory();

    return edit ? xsf.newstr(buf, start, d - start) : this;
  }

  /**
   * Convert a string to a double -- Allowed input is in fixed
   * notation ddd.fff.
   *
   * %OPT% CHECK PERFORMANCE against generating a Java String and
   * converting it to double. The advantage of running in native
   * machine code -- perhaps even microcode, on some systems -- may
   * more than make up for the cost of allocating and discarding the
   * additional object. We need to benchmark this.
   *
   * %OPT% More importantly, we need to decide whether we _care_ about
   * the performance of this operation. Does XString.toDouble constitute
   * any measurable percentage of our typical runtime? I suspect not!
   *
   * @return A double value representation of the string, or return Double.NaN
   * if the string can not be converted.  */
  public double toDouble()
  {
    if(m_length == 0)
      return Double.NaN;
    int i;
    char c;
    String valueString = fsb().getString(m_start,m_length);

    // The following are permitted in the Double.valueOf, but not by the XPath spec:
    // - a plus sign
    // - The use of e or E to indicate exponents
    // - trailing f, F, d, or D
    // See function comments; not sure if this is slower than actually doing the
    // conversion ourselves (as was before).

    for (i=0;i<m_length;i++)
      if (!XMLCharacterRecognizer.isWhiteSpace(valueString.charAt(i)))
        break;
    if (i == m_length) return Double.NaN;
    if (valueString.charAt(i) == '-')
      i++;
    for (;i<m_length;i++) {
      c = valueString.charAt(i);
      if (c != '.' && (c < '0' || c > '9'))
        break;
    }
    for (;i<m_length;i++)
      if (!XMLCharacterRecognizer.isWhiteSpace(valueString.charAt(i)))
        break;
    if (i != m_length)
      return Double.NaN;

    try {
      return Double.parseDouble(valueString);
    } catch (NumberFormatException nfe) {
      // This should catch double periods, empty strings.
      return Double.NaN;
    }
  }
}
