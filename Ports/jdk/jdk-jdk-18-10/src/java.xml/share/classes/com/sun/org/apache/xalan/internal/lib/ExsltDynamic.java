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

package com.sun.org.apache.xalan.internal.lib;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;

import com.sun.org.apache.xalan.internal.extensions.ExpressionContext;
import com.sun.org.apache.xalan.internal.res.XSLMessages;
import com.sun.org.apache.xalan.internal.res.XSLTErrorResources;
import com.sun.org.apache.xpath.internal.NodeSet;
import com.sun.org.apache.xpath.internal.NodeSetDTM;
import com.sun.org.apache.xpath.internal.XPath;
import com.sun.org.apache.xpath.internal.XPathContext;
import com.sun.org.apache.xpath.internal.objects.XBoolean;
import com.sun.org.apache.xpath.internal.objects.XNodeSet;
import com.sun.org.apache.xpath.internal.objects.XNumber;
import com.sun.org.apache.xpath.internal.objects.XObject;
import jdk.xml.internal.JdkXmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.xml.sax.SAXNotSupportedException;

/**
 * This class contains EXSLT dynamic extension functions.
 *
 * It is accessed by specifying a namespace URI as follows:
 * <pre>
 *    xmlns:dyn="http://exslt.org/dynamic"
 * </pre>
 * The documentation for each function has been copied from the relevant
 * EXSLT Implementer page.
 *
 * @see <a href="http://www.exslt.org/">EXSLT</a>

 * @xsl.usage general
 */
public class ExsltDynamic extends ExsltBase
{

   public static final String EXSL_URI = "http://exslt.org/common";

  /**
   * The dyn:max function calculates the maximum value for the nodes passed as
   * the first argument, where the value of each node is calculated dynamically
   * using an XPath expression passed as a string as the second argument.
   * <p>
   * The expressions are evaluated relative to the nodes passed as the first argument.
   * In other words, the value for each node is calculated by evaluating the XPath
   * expression with all context information being the same as that for the call to
   * the dyn:max function itself, except for the following:
   * <p>
   * <ul>
   *  <li>the context node is the node whose value is being calculated.</li>
   *  <li>the context position is the position of the node within the node set passed as
   *   the first argument to the dyn:max function, arranged in document order.</li>
   *  <li>the context size is the number of nodes passed as the first argument to the
   *   dyn:max function.</li>
   * </ul>
   * <p>
   * The dyn:max function returns the maximum of these values, calculated in exactly
   * the same way as for math:max.
   * <p>
   * If the expression string passed as the second argument is an invalid XPath
   * expression (including an empty string), this function returns NaN.
   * <p>
   * This function must take a second argument. To calculate the maximum of a set of
   * nodes based on their string values, you should use the math:max function.
   *
   * @param myContext The ExpressionContext passed by the extension processor
   * @param nl The node set
   * @param expr The expression string
   *
   * @return The maximum evaluation value
   */
  public static double max(ExpressionContext myContext, NodeList nl, String expr)
    throws SAXNotSupportedException
  {

    XPathContext xctxt = null;
    if (myContext instanceof XPathContext.XPathExpressionContext)
      xctxt = ((XPathContext.XPathExpressionContext) myContext).getXPathContext();
    else
      throw new SAXNotSupportedException(XSLMessages.createMessage(XSLTErrorResources.ER_INVALID_CONTEXT_PASSED, new Object[]{myContext }));

    if (expr == null || expr.length() == 0)
      return Double.NaN;

    NodeSetDTM contextNodes = new NodeSetDTM(nl, xctxt);
    xctxt.pushContextNodeList(contextNodes);

    double maxValue = - Double.MAX_VALUE;
    for (int i = 0; i < contextNodes.getLength(); i++)
    {
      int contextNode = contextNodes.item(i);
      xctxt.pushCurrentNode(contextNode);

      double result = 0;
      try
      {
        XPath dynamicXPath = new XPath(expr, xctxt.getSAXLocator(),
                                       xctxt.getNamespaceContext(),
                                       XPath.SELECT);
        result = dynamicXPath.execute(xctxt, contextNode, xctxt.getNamespaceContext()).num();
      }
      catch (TransformerException e)
      {
        xctxt.popCurrentNode();
        xctxt.popContextNodeList();
        return Double.NaN;
      }

      xctxt.popCurrentNode();

      if (result > maxValue)
          maxValue = result;
    }

    xctxt.popContextNodeList();
    return maxValue;

  }

  /**
   * The dyn:min function calculates the minimum value for the nodes passed as the
   * first argument, where the value of each node is calculated dynamically using
   * an XPath expression passed as a string as the second argument.
   * <p>
   * The expressions are evaluated relative to the nodes passed as the first argument.
   * In other words, the value for each node is calculated by evaluating the XPath
   * expression with all context information being the same as that for the call to
   * the dyn:min function itself, except for the following:
   * <p>
   * <ul>
   *  <li>the context node is the node whose value is being calculated.</li>
   *  <li>the context position is the position of the node within the node set passed
   *    as the first argument to the dyn:min function, arranged in document order.</li>
   *  <li>the context size is the number of nodes passed as the first argument to the
   *    dyn:min function.</li>
   * </ul>
   * <p>
   * The dyn:min function returns the minimum of these values, calculated in exactly
   * the same way as for math:min.
   * <p>
   * If the expression string passed as the second argument is an invalid XPath expression
   * (including an empty string), this function returns NaN.
   * <p>
   * This function must take a second argument. To calculate the minimum of a set of
   * nodes based on their string values, you should use the math:min function.
   *
   * @param myContext The ExpressionContext passed by the extension processor
   * @param nl The node set
   * @param expr The expression string
   *
   * @return The minimum evaluation value
   */
  public static double min(ExpressionContext myContext, NodeList nl, String expr)
    throws SAXNotSupportedException
  {

    XPathContext xctxt = null;
    if (myContext instanceof XPathContext.XPathExpressionContext)
      xctxt = ((XPathContext.XPathExpressionContext) myContext).getXPathContext();
    else
      throw new SAXNotSupportedException(XSLMessages.createMessage(XSLTErrorResources.ER_INVALID_CONTEXT_PASSED, new Object[]{myContext }));

    if (expr == null || expr.length() == 0)
      return Double.NaN;

    NodeSetDTM contextNodes = new NodeSetDTM(nl, xctxt);
    xctxt.pushContextNodeList(contextNodes);

    double minValue = Double.MAX_VALUE;
    for (int i = 0; i < nl.getLength(); i++)
    {
      int contextNode = contextNodes.item(i);
      xctxt.pushCurrentNode(contextNode);

      double result = 0;
      try
      {
        XPath dynamicXPath = new XPath(expr, xctxt.getSAXLocator(),
                                       xctxt.getNamespaceContext(),
                                       XPath.SELECT);
        result = dynamicXPath.execute(xctxt, contextNode, xctxt.getNamespaceContext()).num();
      }
      catch (TransformerException e)
      {
        xctxt.popCurrentNode();
        xctxt.popContextNodeList();
        return Double.NaN;
      }

      xctxt.popCurrentNode();

      if (result < minValue)
          minValue = result;
    }

    xctxt.popContextNodeList();
    return minValue;

  }

  /**
   * The dyn:sum function calculates the sum for the nodes passed as the first argument,
   * where the value of each node is calculated dynamically using an XPath expression
   * passed as a string as the second argument.
   * <p>
   * The expressions are evaluated relative to the nodes passed as the first argument.
   * In other words, the value for each node is calculated by evaluating the XPath
   * expression with all context information being the same as that for the call to
   * the dyn:sum function itself, except for the following:
   * <p>
   * <ul>
   *  <li>the context node is the node whose value is being calculated.</li>
   *  <li>the context position is the position of the node within the node set passed as
   *    the first argument to the dyn:sum function, arranged in document order.</li>
   *  <li>the context size is the number of nodes passed as the first argument to the
   *    dyn:sum function.</li>
   * </ul>
   * <p>
   * The dyn:sum function returns the sumimum of these values, calculated in exactly
   * the same way as for sum.
   * <p>
   * If the expression string passed as the second argument is an invalid XPath
   * expression (including an empty string), this function returns NaN.
   * <p>
   * This function must take a second argument. To calculate the sumimum of a set of
   * nodes based on their string values, you should use the sum function.
   *
   * @param myContext The ExpressionContext passed by the extension processor
   * @param nl The node set
   * @param expr The expression string
   *
   * @return The sum of the evaluation value on each node
   */
  public static double sum(ExpressionContext myContext, NodeList nl, String expr)
    throws SAXNotSupportedException
  {
    XPathContext xctxt = null;
    if (myContext instanceof XPathContext.XPathExpressionContext)
      xctxt = ((XPathContext.XPathExpressionContext) myContext).getXPathContext();
    else
      throw new SAXNotSupportedException(XSLMessages.createMessage(XSLTErrorResources.ER_INVALID_CONTEXT_PASSED, new Object[]{myContext }));

    if (expr == null || expr.length() == 0)
      return Double.NaN;

    NodeSetDTM contextNodes = new NodeSetDTM(nl, xctxt);
    xctxt.pushContextNodeList(contextNodes);

    double sum = 0;
    for (int i = 0; i < nl.getLength(); i++)
    {
      int contextNode = contextNodes.item(i);
      xctxt.pushCurrentNode(contextNode);

      double result = 0;
      try
      {
        XPath dynamicXPath = new XPath(expr, xctxt.getSAXLocator(),
                                       xctxt.getNamespaceContext(),
                                       XPath.SELECT);
        result = dynamicXPath.execute(xctxt, contextNode, xctxt.getNamespaceContext()).num();
      }
      catch (TransformerException e)
      {
        xctxt.popCurrentNode();
        xctxt.popContextNodeList();
        return Double.NaN;
      }

      xctxt.popCurrentNode();

      sum = sum + result;

    }

    xctxt.popContextNodeList();
    return sum;
  }

  /**
   * The dyn:map function evaluates the expression passed as the second argument for
   * each of the nodes passed as the first argument, and returns a node set of those values.
   * <p>
   * The expressions are evaluated relative to the nodes passed as the first argument.
   * In other words, the value for each node is calculated by evaluating the XPath
   * expression with all context information being the same as that for the call to
   * the dyn:map function itself, except for the following:
   * <p>
   * <ul>
   *  <li>The context node is the node whose value is being calculated.</li>
   *  <li>the context position is the position of the node within the node set passed
   *    as the first argument to the dyn:map function, arranged in document order.</li>
   *  <li>the context size is the number of nodes passed as the first argument to the
   *    dyn:map function.</li>
   * </ul>
   * <p>
   * If the expression string passed as the second argument is an invalid XPath
   * expression (including an empty string), this function returns an empty node set.
   * <p>
   * If the XPath expression evaluates as a node set, the dyn:map function returns
   * the union of the node sets returned by evaluating the expression for each of the
   * nodes in the first argument. Note that this may mean that the node set resulting
   * from the call to the dyn:map function contains a different number of nodes from
   * the number in the node set passed as the first argument to the function.
   * <p>
   * If the XPath expression evaluates as a number, the dyn:map function returns a
   * node set containing one exsl:number element (namespace http://exslt.org/common)
   * for each node in the node set passed as the first argument to the dyn:map function,
   * in document order. The string value of each exsl:number element is the same as
   * the result of converting the number resulting from evaluating the expression to
   * a string as with the number function, with the exception that Infinity results
   * in an exsl:number holding the highest number the implementation can store, and
   * -Infinity results in an exsl:number holding the lowest number the implementation
   * can store.
   * <p>
   * If the XPath expression evaluates as a boolean, the dyn:map function returns a
   * node set containing one exsl:boolean element (namespace http://exslt.org/common)
   * for each node in the node set passed as the first argument to the dyn:map function,
   * in document order. The string value of each exsl:boolean element is 'true' if the
   * expression evaluates as true for the node, and '' if the expression evaluates as
   * false.
   * <p>
   * Otherwise, the dyn:map function returns a node set containing one exsl:string
   * element (namespace http://exslt.org/common) for each node in the node set passed
   * as the first argument to the dyn:map function, in document order. The string
   * value of each exsl:string element is the same as the result of converting the
   * result of evaluating the expression for the relevant node to a string as with
   * the string function.
   *
   * @param myContext The ExpressionContext passed by the extension processor
   * @param nl The node set
   * @param expr The expression string
   *
   * @return The node set after evaluation
   */
  public static NodeList map(ExpressionContext myContext, NodeList nl, String expr)
    throws SAXNotSupportedException
  {
    XPathContext xctxt = null;
    Document lDoc = null;

    if (myContext instanceof XPathContext.XPathExpressionContext)
      xctxt = ((XPathContext.XPathExpressionContext) myContext).getXPathContext();
    else
      throw new SAXNotSupportedException(XSLMessages.createMessage(XSLTErrorResources.ER_INVALID_CONTEXT_PASSED, new Object[]{myContext }));

    if (expr == null || expr.length() == 0)
      return new NodeSet();

    NodeSetDTM contextNodes = new NodeSetDTM(nl, xctxt);
    xctxt.pushContextNodeList(contextNodes);

    NodeSet resultSet = new NodeSet();
    resultSet.setShouldCacheNodes(true);

    for (int i = 0; i < nl.getLength(); i++)
    {
      int contextNode = contextNodes.item(i);
      xctxt.pushCurrentNode(contextNode);

      XObject object = null;
      try
      {
        XPath dynamicXPath = new XPath(expr, xctxt.getSAXLocator(),
                                       xctxt.getNamespaceContext(),
                                       XPath.SELECT);
        object = dynamicXPath.execute(xctxt, contextNode, xctxt.getNamespaceContext());

        if (object instanceof XNodeSet)
        {
          NodeList nodelist = null;
          nodelist = ((XNodeSet)object).nodelist();

          for (int k = 0; k < nodelist.getLength(); k++)
          {
            Node n = nodelist.item(k);
            if (!resultSet.contains(n))
              resultSet.addNode(n);
          }
        }
        else
        {
          if (lDoc == null)
          {
            lDoc = JdkXmlUtils.getDOMDocument();
          }

          Element element = null;
          if (object instanceof XNumber)
            element = lDoc.createElementNS(EXSL_URI, "exsl:number");
          else if (object instanceof XBoolean)
            element = lDoc.createElementNS(EXSL_URI, "exsl:boolean");
          else
            element = lDoc.createElementNS(EXSL_URI, "exsl:string");

          Text textNode = lDoc.createTextNode(object.str());
          element.appendChild(textNode);
          resultSet.addNode(element);
        }
      }
      catch (Exception e)
      {
        xctxt.popCurrentNode();
        xctxt.popContextNodeList();
        return new NodeSet();
      }

      xctxt.popCurrentNode();

    }

    xctxt.popContextNodeList();
    return resultSet;
  }

  /**
   * The dyn:evaluate function evaluates a string as an XPath expression and returns
   * the resulting value, which might be a boolean, number, string, node set, result
   * tree fragment or external object. The sole argument is the string to be evaluated.
   * <p>
   * If the expression string passed as the second argument is an invalid XPath
   * expression (including an empty string), this function returns an empty node set.
   * <p>
   * You should only use this function if the expression must be constructed dynamically,
   * otherwise it is much more efficient to use the expression literally.
   *
   * @param myContext The ExpressionContext passed by the extension processor
   * @param xpathExpr The XPath expression string
   *
   * @return The evaluation result
   */
  public static XObject evaluate(ExpressionContext myContext, String xpathExpr)
    throws SAXNotSupportedException
  {
    if (myContext instanceof XPathContext.XPathExpressionContext)
    {
      XPathContext xctxt = null;
      try
      {
        xctxt = ((XPathContext.XPathExpressionContext) myContext).getXPathContext();
        XPath dynamicXPath = new XPath(xpathExpr, xctxt.getSAXLocator(),
                                       xctxt.getNamespaceContext(),
                                       XPath.SELECT);

        return dynamicXPath.execute(xctxt, myContext.getContextNode(),
                                    xctxt.getNamespaceContext());
      }
      catch (TransformerException e)
      {
        return new XNodeSet(xctxt.getDTMManager());
      }
    }
    else
      throw new SAXNotSupportedException(XSLMessages.createMessage(XSLTErrorResources.ER_INVALID_CONTEXT_PASSED, new Object[]{myContext })); //"Invalid context passed to evaluate "
  }

  /**
   * The dyn:closure function creates a node set resulting from transitive closure of
   * evaluating the expression passed as the second argument on each of the nodes passed
   * as the first argument, then on the node set resulting from that and so on until no
   * more nodes are found. For example:
   * <pre>
   *  dyn:closure(., '*')
   * </pre>
   * returns all the descendant elements of the node (its element children, their
   * children, their children's children and so on).
   * <p>
   * The expression is thus evaluated several times, each with a different node set
   * acting as the context of the expression. The first time the expression is
   * evaluated, the context node set is the first argument passed to the dyn:closure
   * function. In other words, the node set for each node is calculated by evaluating
   * the XPath expression with all context information being the same as that for
   * the call to the dyn:closure function itself, except for the following:
   * <p>
   * <ul>
   *  <li>the context node is the node whose value is being calculated.</li>
   *  <li>the context position is the position of the node within the node set passed
   *    as the first argument to the dyn:closure function, arranged in document order.</li>
   *  <li>the context size is the number of nodes passed as the first argument to the
   *    dyn:closure function.</li>
   *  <li>the current node is the node whose value is being calculated.</li>
   * </ul>
   * <p>
   * The result for a particular iteration is the union of the node sets resulting
   * from evaluting the expression for each of the nodes in the source node set for
   * that iteration. This result is then used as the source node set for the next
   * iteration, and so on. The result of the function as a whole is the union of
   * the node sets generated by each iteration.
   * <p>
   * If the expression string passed as the second argument is an invalid XPath
   * expression (including an empty string) or an expression that does not return a
   * node set, this function returns an empty node set.
   *
   * @param myContext The ExpressionContext passed by the extension processor
   * @param nl The node set
   * @param expr The expression string
   *
   * @return The node set after evaluation
   */
  public static NodeList closure(ExpressionContext myContext, NodeList nl, String expr)
    throws SAXNotSupportedException
  {
    XPathContext xctxt = null;
    if (myContext instanceof XPathContext.XPathExpressionContext)
      xctxt = ((XPathContext.XPathExpressionContext) myContext).getXPathContext();
    else
      throw new SAXNotSupportedException(XSLMessages.createMessage(XSLTErrorResources.ER_INVALID_CONTEXT_PASSED, new Object[]{myContext }));

    if (expr == null || expr.length() == 0)
      return new NodeSet();

    NodeSet closureSet = new NodeSet();
    closureSet.setShouldCacheNodes(true);

    NodeList iterationList = nl;
    do
    {

      NodeSet iterationSet = new NodeSet();

      NodeSetDTM contextNodes = new NodeSetDTM(iterationList, xctxt);
      xctxt.pushContextNodeList(contextNodes);

      for (int i = 0; i < iterationList.getLength(); i++)
      {
        int contextNode = contextNodes.item(i);
        xctxt.pushCurrentNode(contextNode);

        XObject object = null;
        try
        {
          XPath dynamicXPath = new XPath(expr, xctxt.getSAXLocator(),
                                         xctxt.getNamespaceContext(),
                                         XPath.SELECT);
          object = dynamicXPath.execute(xctxt, contextNode, xctxt.getNamespaceContext());

          if (object instanceof XNodeSet)
          {
            NodeList nodelist = null;
            nodelist = ((XNodeSet)object).nodelist();

            for (int k = 0; k < nodelist.getLength(); k++)
            {
              Node n = nodelist.item(k);
              if (!iterationSet.contains(n))
                iterationSet.addNode(n);
            }
          }
          else
          {
            xctxt.popCurrentNode();
            xctxt.popContextNodeList();
            return new NodeSet();
          }
        }
        catch (TransformerException e)
        {
          xctxt.popCurrentNode();
          xctxt.popContextNodeList();
          return new NodeSet();
        }

        xctxt.popCurrentNode();

      }

      xctxt.popContextNodeList();

      iterationList = iterationSet;

      for (int i = 0; i < iterationList.getLength(); i++)
      {
        Node n = iterationList.item(i);
        if (!closureSet.contains(n))
          closureSet.addNode(n);
      }

    } while(iterationList.getLength() > 0);

    return closureSet;

  }

}
