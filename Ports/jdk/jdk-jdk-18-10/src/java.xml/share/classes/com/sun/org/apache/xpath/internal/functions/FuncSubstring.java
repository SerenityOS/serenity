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

package com.sun.org.apache.xpath.internal.functions;

import com.sun.org.apache.xalan.internal.res.XSLMessages;
import com.sun.org.apache.xml.internal.utils.XMLString;
import com.sun.org.apache.xpath.internal.XPathContext;
import com.sun.org.apache.xpath.internal.objects.XObject;
import com.sun.org.apache.xpath.internal.objects.XString;
import com.sun.org.apache.xpath.internal.res.XPATHErrorResources;

/**
 * Execute the Substring() function.
 * @xsl.usage advanced
 */
public class FuncSubstring extends Function3Args
{
    static final long serialVersionUID = -5996676095024715502L;

  /**
   * Execute the function.  The function must return
   * a valid object.
   * @param xctxt The current execution context.
   * @return A valid XObject.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public XObject execute(XPathContext xctxt) throws javax.xml.transform.TransformerException
  {

    XMLString s1 = m_arg0.execute(xctxt).xstr();
    double start = m_arg1.execute(xctxt).num();
    int lenOfS1 = s1.length();
    XMLString substr;

    if (lenOfS1 <= 0)
      return XString.EMPTYSTRING;
    else
    {
      int startIndex;

      if (Double.isNaN(start))
      {

        // Double.MIN_VALUE doesn't work with math below
        // so just use a big number and hope I never get caught.
        start = -1000000;
        startIndex = 0;
      }
      else
      {
        start = Math.round(start);
        startIndex = (start > 0) ? (int) start - 1 : 0;
      }

      if (null != m_arg2)
      {
        double len = m_arg2.num(xctxt);
        int end = (int) (Math.round(len) + start) - 1;

        // Normalize end index.
        if (end < 0)
          end = 0;
        else if (end > lenOfS1)
          end = lenOfS1;

        if (startIndex > lenOfS1)
          startIndex = lenOfS1;

        substr = s1.substring(startIndex, end);
      }
      else
      {
        if (startIndex > lenOfS1)
          startIndex = lenOfS1;
        substr = s1.substring(startIndex);
      }
    }

    return (XString)substr; // cast semi-safe
  }

  /**
   * Check that the number of arguments passed to this function is correct.
   *
   *
   * @param argNum The number of arguments that is being passed to the function.
   *
   * @throws WrongNumberArgsException
   */
  public void checkNumberArgs(int argNum) throws WrongNumberArgsException
  {
    if (argNum < 2)
      reportWrongNumberArgs();
  }

  /**
   * Constructs and throws a WrongNumberArgException with the appropriate
   * message for this function object.
   *
   * @throws WrongNumberArgsException
   */
  protected void reportWrongNumberArgs() throws WrongNumberArgsException {
      throw new WrongNumberArgsException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_TWO_OR_THREE, null)); //"2 or 3");
  }
}
