/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xpath.internal;

import javax.xml.transform.TransformerException;

import com.sun.org.apache.xml.internal.utils.PrefixResolver;
import com.sun.org.apache.xml.internal.utils.PrefixResolverDefault;
import com.sun.org.apache.xpath.internal.objects.XObject;
import jdk.xml.internal.JdkConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;

/**
 * The methods in this class are convenience methods into the
 * low-level XPath API.
 * These functions tend to be a little slow, since a number of objects must be
 * created for each evaluation.  A faster way is to precompile the
 * XPaths using the low-level API, and then just use the XPaths
 * over and over.
 *
 * NOTE: In particular, each call to this method will create a new
 * XPathContext, a new DTMManager... and thus a new DTM. That's very
 * safe, since it guarantees that you're always processing against a
 * fully up-to-date view of your document. But it's also portentially
 * very expensive, since you're rebuilding the DTM every time. You should
 * consider using an instance of CachedXPathAPI rather than these static
 * methods.
 *
 * @see <a href="http://www.w3.org/TR/xpath">XPath Specification</a>
 * @LastModified: May 2021
 * */
public class XPathAPI
{

  /**
   * Use an XPath string to select a single node. XPath namespace
   * prefixes are resolved from the context node, which may not
   * be what you want (see the next method).
   *
   * @param contextNode The node to start searching from.
   * @param str A valid XPath string.
   * @return The first node found that matches the XPath, or null.
   *
   * @throws TransformerException
   */
  public static Node selectSingleNode(Node contextNode, String str)
          throws TransformerException
  {
    return selectSingleNode(contextNode, str, contextNode);
  }

  /**
   * Use an XPath string to select a single node.
   * XPath namespace prefixes are resolved from the namespaceNode.
   *
   * @param contextNode The node to start searching from.
   * @param str A valid XPath string.
   * @param namespaceNode The node from which prefixes in the XPath will be resolved to namespaces.
   * @return The first node found that matches the XPath, or null.
   *
   * @throws TransformerException
   */
  public static Node selectSingleNode(
          Node contextNode, String str, Node namespaceNode)
            throws TransformerException
  {

    // Have the XObject return its result as a NodeSetDTM.
    NodeIterator nl = selectNodeIterator(contextNode, str, namespaceNode);

    // Return the first node, or null
    return nl.nextNode();
  }

  /**
   *  Use an XPath string to select a nodelist.
   *  XPath namespace prefixes are resolved from the contextNode.
   *
   *  @param contextNode The node to start searching from.
   *  @param str A valid XPath string.
   *  @return A NodeIterator, should never be null.
   *
   * @throws TransformerException
   */
  public static NodeIterator selectNodeIterator(Node contextNode, String str)
          throws TransformerException
  {
    return selectNodeIterator(contextNode, str, contextNode);
  }

  /**
   *  Use an XPath string to select a nodelist.
   *  XPath namespace prefixes are resolved from the namespaceNode.
   *
   *  @param contextNode The node to start searching from.
   *  @param str A valid XPath string.
   *  @param namespaceNode The node from which prefixes in the XPath will be resolved to namespaces.
   *  @return A NodeIterator, should never be null.
   *
   * @throws TransformerException
   */
  public static NodeIterator selectNodeIterator(
          Node contextNode, String str, Node namespaceNode)
            throws TransformerException
  {

    // Execute the XPath, and have it return the result
    XObject list = eval(contextNode, str, namespaceNode);

    // Have the XObject return its result as a NodeSetDTM.
    return list.nodeset();
  }

  /**
   *  Use an XPath string to select a nodelist.
   *  XPath namespace prefixes are resolved from the contextNode.
   *
   *  @param contextNode The node to start searching from.
   *  @param str A valid XPath string.
   *  @return A NodeIterator, should never be null.
   *
   * @throws TransformerException
   */
  public static NodeList selectNodeList(Node contextNode, String str)
          throws TransformerException
  {
    return selectNodeList(contextNode, str, contextNode);
  }

  /**
   *  Use an XPath string to select a nodelist.
   *  XPath namespace prefixes are resolved from the namespaceNode.
   *
   *  @param contextNode The node to start searching from.
   *  @param str A valid XPath string.
   *  @param namespaceNode The node from which prefixes in the XPath will be resolved to namespaces.
   *  @return A NodeIterator, should never be null.
   *
   * @throws TransformerException
   */
  public static NodeList selectNodeList(
          Node contextNode, String str, Node namespaceNode)
            throws TransformerException
  {

    // Execute the XPath, and have it return the result
    XObject list = eval(contextNode, str, namespaceNode);

    // Return a NodeList.
    return list.nodelist();
  }

  /**
   *  Evaluate XPath string to an XObject.  Using this method,
   *  XPath namespace prefixes will be resolved from the namespaceNode.
   *  @param contextNode The node to start searching from.
   *  @param str A valid XPath string.
   *  @return An XObject, which can be used to obtain a string, number, nodelist, etc, should never be null.
   *  @see com.sun.org.apache.xpath.internal.objects.XObject
   *  @see com.sun.org.apache.xpath.internal.objects.XNull
   *  @see com.sun.org.apache.xpath.internal.objects.XBoolean
   *  @see com.sun.org.apache.xpath.internal.objects.XNumber
   *  @see com.sun.org.apache.xpath.internal.objects.XString
   *  @see com.sun.org.apache.xpath.internal.objects.XRTreeFrag
   *
   * @throws TransformerException
   */
  public static XObject eval(Node contextNode, String str)
          throws TransformerException
  {
    return eval(contextNode, str, contextNode);
  }

  /**
   *  Evaluate XPath string to an XObject.
   *  XPath namespace prefixes are resolved from the namespaceNode.
   *  The implementation of this is a little slow, since it creates
   *  a number of objects each time it is called.  This could be optimized
   *  to keep the same objects around, but then thread-safety issues would arise.
   *
   *  @param contextNode The node to start searching from.
   *  @param str A valid XPath string.
   *  @param namespaceNode The node from which prefixes in the XPath will be resolved to namespaces.
   *  @return An XObject, which can be used to obtain a string, number, nodelist, etc, should never be null.
   *  @see com.sun.org.apache.xpath.internal.objects.XObject
   *  @see com.sun.org.apache.xpath.internal.objects.XNull
   *  @see com.sun.org.apache.xpath.internal.objects.XBoolean
   *  @see com.sun.org.apache.xpath.internal.objects.XNumber
   *  @see com.sun.org.apache.xpath.internal.objects.XString
   *  @see com.sun.org.apache.xpath.internal.objects.XRTreeFrag
   *
   * @throws TransformerException
   */
  public static XObject eval(Node contextNode, String str, Node namespaceNode)
          throws TransformerException
  {

    // Since we don't have a XML Parser involved here, install some default support
    // for things like namespaces, etc.
    // (Changed from: XPathContext xpathSupport = new XPathContext();
    //    because XPathContext is weak in a number of areas... perhaps
    //    XPathContext should be done away with.)
    XPathContext xpathSupport = new XPathContext(JdkConstants.OVERRIDE_PARSER_DEFAULT);

    // Create an object to resolve namespace prefixes.
    // XPath namespaces are resolved from the input context node's document element
    // if it is a root node, or else the current context node (for lack of a better
    // resolution space, given the simplicity of this sample code).
    PrefixResolverDefault prefixResolver = new PrefixResolverDefault(
      (namespaceNode.getNodeType() == Node.DOCUMENT_NODE)
      ? ((Document) namespaceNode).getDocumentElement() : namespaceNode);

    // Create the XPath object.
    XPath xpath = new XPath(str, null, prefixResolver, XPath.SELECT, null);

    // Execute the XPath, and have it return the result
    // return xpath.execute(xpathSupport, contextNode, prefixResolver);
    int ctxtNode = xpathSupport.getDTMHandleFromNode(contextNode);

    return xpath.execute(xpathSupport, ctxtNode, prefixResolver);
  }

  /**
   *   Evaluate XPath string to an XObject.
   *   XPath namespace prefixes are resolved from the namespaceNode.
   *   The implementation of this is a little slow, since it creates
   *   a number of objects each time it is called.  This could be optimized
   *   to keep the same objects around, but then thread-safety issues would arise.
   *
   *   @param contextNode The node to start searching from.
   *   @param str A valid XPath string.
   *   @param prefixResolver Will be called if the parser encounters namespace
   *                         prefixes, to resolve the prefixes to URLs.
   *   @return An XObject, which can be used to obtain a string, number, nodelist, etc, should never be null.
   *   @see com.sun.org.apache.xpath.internal.objects.XObject
   *   @see com.sun.org.apache.xpath.internal.objects.XNull
   *   @see com.sun.org.apache.xpath.internal.objects.XBoolean
   *   @see com.sun.org.apache.xpath.internal.objects.XNumber
   *   @see com.sun.org.apache.xpath.internal.objects.XString
   *   @see com.sun.org.apache.xpath.internal.objects.XRTreeFrag
   *
   * @throws TransformerException
   */
  public static XObject eval(
          Node contextNode, String str, PrefixResolver prefixResolver)
            throws TransformerException
  {

    // Since we don't have a XML Parser involved here, install some default support
    // for things like namespaces, etc.
    // (Changed from: XPathContext xpathSupport = new XPathContext();
    //    because XPathContext is weak in a number of areas... perhaps
    //    XPathContext should be done away with.)
    // Create the XPath object.
    XPath xpath = new XPath(str, null, prefixResolver, XPath.SELECT, null);

    // Execute the XPath, and have it return the result
    XPathContext xpathSupport = new XPathContext(JdkConstants.OVERRIDE_PARSER_DEFAULT);
    int ctxtNode = xpathSupport.getDTMHandleFromNode(contextNode);

    return xpath.execute(xpathSupport, ctxtNode, prefixResolver);
  }
}
