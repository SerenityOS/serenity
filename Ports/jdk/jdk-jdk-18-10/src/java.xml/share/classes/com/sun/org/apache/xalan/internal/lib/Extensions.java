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

import java.util.StringTokenizer;

import com.sun.org.apache.xalan.internal.extensions.ExpressionContext;
import com.sun.org.apache.xpath.internal.NodeSet;
import com.sun.org.apache.xpath.internal.objects.XBoolean;
import com.sun.org.apache.xpath.internal.objects.XNumber;
import com.sun.org.apache.xpath.internal.objects.XObject;
import jdk.xml.internal.JdkXmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.NodeIterator;

import org.xml.sax.SAXNotSupportedException;

/**
 * This class contains many of the Xalan-supplied extensions.
 * It is accessed by specifying a namespace URI as follows:
 * <pre>
 *    xmlns:xalan="http://xml.apache.org/xalan"
 * </pre>
 * @xsl.usage general
 */
public class Extensions
{
  /**
   * Constructor Extensions
   *
   */
  private Extensions(){}  // Make sure class cannot be instantiated

  /**
   * This method is an extension that implements as a Xalan extension
   * the node-set function also found in xt and saxon.
   * If the argument is a Result Tree Fragment, then <code>nodeset</code>
   * returns a node-set consisting of a single root node as described in
   * section 11.1 of the XSLT 1.0 Recommendation.  If the argument is a
   * node-set, <code>nodeset</code> returns a node-set.  If the argument
   * is a string, number, or boolean, then <code>nodeset</code> returns
   * a node-set consisting of a single root node with a single text node
   * child that is the result of calling the XPath string() function on the
   * passed parameter.  If the argument is anything else, then a node-set
   * is returned consisting of a single root node with a single text node
   * child that is the result of calling the java <code>toString()</code>
   * method on the passed argument.
   * Most of the
   * actual work here is done in <code>MethodResolver</code> and
   * <code>XRTreeFrag</code>.
   * @param myProcessor Context passed by the extension processor
   * @param rtf Argument in the stylesheet to the nodeset extension function
   *
   * NEEDSDOC ($objectName$) @return
   */
  public static NodeSet nodeset(ExpressionContext myProcessor, Object rtf)
  {

    String textNodeValue;

    if (rtf instanceof NodeIterator)
    {
      return new NodeSet((NodeIterator) rtf);
    }
    else
    {
      if (rtf instanceof String)
      {
        textNodeValue = (String) rtf;
      }
      else if (rtf instanceof Boolean)
      {
        textNodeValue = new XBoolean(((Boolean) rtf).booleanValue()).str();
      }
      else if (rtf instanceof Double)
      {
        textNodeValue = new XNumber(((Double) rtf).doubleValue()).str();
      }
      else
      {
        textNodeValue = rtf.toString();
      }

      // This no longer will work right since the DTM.
      // Document myDoc = myProcessor.getContextNode().getOwnerDocument();
      Document myDoc = JdkXmlUtils.getDOMDocument();

        Text textNode = myDoc.createTextNode(textNodeValue);
        DocumentFragment docFrag = myDoc.createDocumentFragment();

        docFrag.appendChild(textNode);

      return new NodeSet(docFrag);
    }
  }

  /**
   * Returns the intersection of two node-sets.
   *
   * @param nl1 NodeList for first node-set
   * @param nl2 NodeList for second node-set
   * @return a NodeList containing the nodes in nl1 that are also in nl2
   *
   * Note: The usage of this extension function in the xalan namespace
   * is deprecated. Please use the same function in the EXSLT sets extension
   * (http://exslt.org/sets).
   */
  public static NodeList intersection(NodeList nl1, NodeList nl2)
  {
    return ExsltSets.intersection(nl1, nl2);
  }

  /**
   * Returns the difference between two node-sets.
   *
   * @param nl1 NodeList for first node-set
   * @param nl2 NodeList for second node-set
   * @return a NodeList containing the nodes in nl1 that are not in nl2
   *
   * Note: The usage of this extension function in the xalan namespace
   * is deprecated. Please use the same function in the EXSLT sets extension
   * (http://exslt.org/sets).
   */
  public static NodeList difference(NodeList nl1, NodeList nl2)
  {
    return ExsltSets.difference(nl1, nl2);
  }

  /**
   * Returns node-set containing distinct string values.
   *
   * @param nl NodeList for node-set
   * @return a NodeList with nodes from nl containing distinct string values.
   * In other words, if more than one node in nl contains the same string value,
   * only include the first such node found.
   *
   * Note: The usage of this extension function in the xalan namespace
   * is deprecated. Please use the same function in the EXSLT sets extension
   * (http://exslt.org/sets).
   */
  public static NodeList distinct(NodeList nl)
  {
    return ExsltSets.distinct(nl);
  }

  /**
   * Returns true if both node-sets contain the same set of nodes.
   *
   * @param nl1 NodeList for first node-set
   * @param nl2 NodeList for second node-set
   * @return true if nl1 and nl2 contain exactly the same set of nodes.
   */
  public static boolean hasSameNodes(NodeList nl1, NodeList nl2)
  {

    NodeSet ns1 = new NodeSet(nl1);
    NodeSet ns2 = new NodeSet(nl2);

    if (ns1.getLength() != ns2.getLength())
      return false;

    for (int i = 0; i < ns1.getLength(); i++)
    {
      Node n = ns1.elementAt(i);

      if (!ns2.contains(n))
        return false;
    }

    return true;
  }

  /**
   * Returns the result of evaluating the argument as a string containing
   * an XPath expression.  Used where the XPath expression is not known until
   * run-time.  The expression is evaluated as if the run-time value of the
   * argument appeared in place of the evaluate function call at compile time.
   *
   * @param myContext an <code>ExpressionContext</code> passed in by the
   *                  extension mechanism.  This must be an XPathContext.
   * @param xpathExpr The XPath expression to be evaluated.
   * @return the XObject resulting from evaluating the XPath
   *
   * @throws SAXNotSupportedException
   *
   * Note: The usage of this extension function in the xalan namespace
   * is deprecated. Please use the same function in the EXSLT dynamic extension
   * (http://exslt.org/dynamic).
   */
  public static XObject evaluate(ExpressionContext myContext, String xpathExpr)
         throws SAXNotSupportedException
  {
    return ExsltDynamic.evaluate(myContext, xpathExpr);
  }

  /**
   * Returns a NodeSet containing one text node for each token in the first argument.
   * Delimiters are specified in the second argument.
   * Tokens are determined by a call to <code>StringTokenizer</code>.
   * If the first argument is an empty string or contains only delimiters, the result
   * will be an empty NodeSet.
   *
   * Contributed to XalanJ1 by <a href="mailto:benoit.cerrina@writeme.com">Benoit Cerrina</a>.
   *
   * @param toTokenize The string to be split into text tokens.
   * @param delims The delimiters to use.
   * @return a NodeSet as described above.
   */
  public static NodeList tokenize(String toTokenize, String delims)
  {

    Document doc = JdkXmlUtils.getDOMDocument();

    StringTokenizer lTokenizer = new StringTokenizer(toTokenize, delims);
    NodeSet resultSet = new NodeSet();

    synchronized (doc)
    {
      while (lTokenizer.hasMoreTokens())
      {
        resultSet.addNode(doc.createTextNode(lTokenizer.nextToken()));
      }
    }

    return resultSet;
  }

  /**
   * Returns a NodeSet containing one text node for each token in the first argument.
   * Delimiters are whitespace.  That is, the delimiters that are used are tab (&#x09),
   * linefeed (&#x0A), return (&#x0D), and space (&#x20).
   * Tokens are determined by a call to <code>StringTokenizer</code>.
   * If the first argument is an empty string or contains only delimiters, the result
   * will be an empty NodeSet.
   *
   * Contributed to XalanJ1 by <a href="mailto:benoit.cerrina@writeme.com">Benoit Cerrina</a>.
   *
   * @param toTokenize The string to be split into text tokens.
   * @return a NodeSet as described above.
   */
  public static NodeList tokenize(String toTokenize)
  {
    return tokenize(toTokenize, " \t\n\r");
  }
}
