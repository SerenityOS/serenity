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
import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.utils.XMLString;
import com.sun.org.apache.xpath.internal.XPathContext;
import com.sun.org.apache.xpath.internal.objects.XString;
import com.sun.org.apache.xpath.internal.res.XPATHErrorResources;

/**
 * Base class for functions that accept one argument that can be defaulted if
 * not specified.
 * @xsl.usage advanced
 */
public class FunctionDef1Arg extends FunctionOneArg
{
    static final long serialVersionUID = 2325189412814149264L;

  /**
   * Execute the first argument expression that is expected to return a
   * nodeset.  If the argument is null, then return the current context node.
   *
   * @param xctxt Runtime XPath context.
   *
   * @return The first node of the executed nodeset, or the current context
   *         node if the first argument is null.
   *
   * @throws javax.xml.transform.TransformerException if an error occurs while
   *                                   executing the argument expression.
   */
  protected int getArg0AsNode(XPathContext xctxt)
          throws javax.xml.transform.TransformerException
  {

    return (null == m_arg0)
           ? xctxt.getCurrentNode() : m_arg0.asNode(xctxt);
  }

  /**
   * Tell if the expression is a nodeset expression.
   * @return true if the expression can be represented as a nodeset.
   */
  public boolean Arg0IsNodesetExpr()
  {
    return (null == m_arg0) ? true : m_arg0.isNodesetExpr();
  }

  /**
   * Execute the first argument expression that is expected to return a
   * string.  If the argument is null, then get the string value from the
   * current context node.
   *
   * @param xctxt Runtime XPath context.
   *
   * @return The string value of the first argument, or the string value of the
   *         current context node if the first argument is null.
   *
   * @throws javax.xml.transform.TransformerException if an error occurs while
   *                                   executing the argument expression.
   */
  protected XMLString getArg0AsString(XPathContext xctxt)
          throws javax.xml.transform.TransformerException
  {
    if(null == m_arg0)
    {
      int currentNode = xctxt.getCurrentNode();
      if(DTM.NULL == currentNode)
        return XString.EMPTYSTRING;
      else
      {
        DTM dtm = xctxt.getDTM(currentNode);
        return dtm.getStringValue(currentNode);
      }

    }
    else
      return m_arg0.execute(xctxt).xstr();
  }

  /**
   * Execute the first argument expression that is expected to return a
   * number.  If the argument is null, then get the number value from the
   * current context node.
   *
   * @param xctxt Runtime XPath context.
   *
   * @return The number value of the first argument, or the number value of the
   *         current context node if the first argument is null.
   *
   * @throws javax.xml.transform.TransformerException if an error occurs while
   *                                   executing the argument expression.
   */
  protected double getArg0AsNumber(XPathContext xctxt)
          throws javax.xml.transform.TransformerException
  {

    if(null == m_arg0)
    {
      int currentNode = xctxt.getCurrentNode();
      if(DTM.NULL == currentNode)
        return 0;
      else
      {
        DTM dtm = xctxt.getDTM(currentNode);
        XMLString str = dtm.getStringValue(currentNode);
        return str.toDouble();
      }

    }
    else
      return m_arg0.execute(xctxt).num();
  }

  /**
   * Check that the number of arguments passed to this function is correct.
   *
   * @param argNum The number of arguments that is being passed to the function.
   *
   * @throws WrongNumberArgsException if the number of arguments is not 0 or 1.
   */
  public void checkNumberArgs(int argNum) throws WrongNumberArgsException
  {
    if (argNum > 1)
      reportWrongNumberArgs();
  }

  /**
   * Constructs and throws a WrongNumberArgException with the appropriate
   * message for this function object.
   *
   * @throws WrongNumberArgsException
   */
  protected void reportWrongNumberArgs() throws WrongNumberArgsException {
      throw new WrongNumberArgsException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_ZERO_OR_ONE, null)); //"0 or 1");
  }

  /**
   * Tell if this expression or it's subexpressions can traverse outside
   * the current subtree.
   *
   * @return true if traversal outside the context node's subtree can occur.
   */
  public boolean canTraverseOutsideSubtree()
  {
    return (null == m_arg0) ? false : super.canTraverseOutsideSubtree();
  }
}
