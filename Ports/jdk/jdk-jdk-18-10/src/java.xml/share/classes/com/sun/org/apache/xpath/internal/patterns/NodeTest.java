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

package com.sun.org.apache.xpath.internal.patterns;

import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.dtm.DTMFilter;
import com.sun.org.apache.xml.internal.utils.QName;
import com.sun.org.apache.xpath.internal.Expression;
import com.sun.org.apache.xpath.internal.ExpressionOwner;
import com.sun.org.apache.xpath.internal.XPath;
import com.sun.org.apache.xpath.internal.XPathContext;
import com.sun.org.apache.xpath.internal.XPathVisitor;
import com.sun.org.apache.xpath.internal.objects.XNumber;
import com.sun.org.apache.xpath.internal.objects.XObject;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the basic node test class for both match patterns and location path
 * steps.
 * @xsl.usage advanced
 * @LastModified: Oct 2017
 */
public class NodeTest extends Expression
{
    static final long serialVersionUID = -5736721866747906182L;

  /**
   * The namespace or local name for node tests with a wildcard.
   *  @see <a href="http://www.w3.org/TR/xpath#NT-NameTest">the XPath NameTest production.</a>
   */
  public static final String WILD = "*";

  /**
   * The URL to pass to the Node#supports method, to see if the
   * DOM has already been stripped of whitespace nodes.
   */
  public static final String SUPPORTS_PRE_STRIPPING =
    "http://xml.apache.org/xpath/features/whitespace-pre-stripping";

  /**
   * This attribute determines which node types are accepted.
   * @serial
   */
  protected int m_whatToShow;

  /**
   * Special bitmap for match patterns starting with a function.
   * Make sure this does not conflict with {@link org.w3c.dom.traversal.NodeFilter}.
   */
  public static final int SHOW_BYFUNCTION = 0x00010000;

  /**
   * This attribute determines which node types are accepted.
   * These constants are defined in the {@link org.w3c.dom.traversal.NodeFilter}
   * interface.
   *
   * @return bitset mainly defined in {@link org.w3c.dom.traversal.NodeFilter}.
   */
  public int getWhatToShow()
  {
    return m_whatToShow;
  }

  /**
   * This attribute determines which node types are accepted.
   * These constants are defined in the {@link org.w3c.dom.traversal.NodeFilter}
   * interface.
   *
   * @param what bitset mainly defined in {@link org.w3c.dom.traversal.NodeFilter}.
   */
  public void setWhatToShow(int what)
  {
    m_whatToShow = what;
  }

  /**
   * The namespace to be tested for, which may be null.
   *  @serial
   */
  String m_namespace;

  /**
   * Return the namespace to be tested.
   *
   * @return The namespace to be tested for, or {@link #WILD}, or null.
   */
  public String getNamespace()
  {
    return m_namespace;
  }

  /**
   * Set the namespace to be tested.
   *
   * @param ns The namespace to be tested for, or {@link #WILD}, or null.
   */
  public void setNamespace(String ns)
  {
    m_namespace = ns;
  }

  /**
   * The local name to be tested for.
   *  @serial
   */
  protected String m_name;

  /**
   * Return the local name to be tested.
   *
   * @return the local name to be tested, or {@link #WILD}, or an empty string.
   */
  public String getLocalName()
  {
    return (null == m_name) ? "" : m_name;
  }

  /**
   * Set the local name to be tested.
   *
   * @param name the local name to be tested, or {@link #WILD}, or an empty string.
   */
  public void setLocalName(String name)
  {
    m_name = name;
  }

  /**
   * Statically calculated score for this test.  One of
   *  {@link #SCORE_NODETEST},
   *  {@link #SCORE_NONE},
   *  {@link #SCORE_NSWILD},
   *  {@link #SCORE_QNAME}, or
   *  {@link #SCORE_OTHER}.
   *  @serial
   */
  XNumber m_score;

  /**
   * The match score if the pattern consists of just a NodeTest.
   *  @see <a href="http://www.w3.org/TR/xslt#conflict">XSLT Specification - 5.5 Conflict Resolution for Template Rules</a>
   */
  public static final XNumber SCORE_NODETEST =
    new XNumber(XPath.MATCH_SCORE_NODETEST);

  /**
   * The match score if the pattern pattern has the form NCName:*.
   *  @see <a href="http://www.w3.org/TR/xslt#conflict">XSLT Specification - 5.5 Conflict Resolution for Template Rules</a>
   */
  public static final XNumber SCORE_NSWILD =
    new XNumber(XPath.MATCH_SCORE_NSWILD);

  /**
   * The match score if the pattern has the form
   * of a QName optionally preceded by an @ character.
   *  @see <a href="http://www.w3.org/TR/xslt#conflict">XSLT Specification - 5.5 Conflict Resolution for Template Rules</a>
   */
  public static final XNumber SCORE_QNAME =
    new XNumber(XPath.MATCH_SCORE_QNAME);

  /**
   * The match score if the pattern consists of something
   * other than just a NodeTest or just a qname.
   *  @see <a href="http://www.w3.org/TR/xslt#conflict">XSLT Specification - 5.5 Conflict Resolution for Template Rules</a>
   */
  public static final XNumber SCORE_OTHER =
    new XNumber(XPath.MATCH_SCORE_OTHER);

  /**
   * The match score if no match is made.
   *  @see <a href="http://www.w3.org/TR/xslt#conflict">XSLT Specification - 5.5 Conflict Resolution for Template Rules</a>
   */
  public static final XNumber SCORE_NONE =
    new XNumber(XPath.MATCH_SCORE_NONE);

  /**
   * Construct an NodeTest that tests for namespaces and node names.
   *
   *
   * @param whatToShow Bit set defined mainly by {@link org.w3c.dom.traversal.NodeFilter}.
   * @param namespace The namespace to be tested.
   * @param name The local name to be tested.
   */
  public NodeTest(int whatToShow, String namespace, String name)
  {
    initNodeTest(whatToShow, namespace, name);
  }

  /**
   * Construct an NodeTest that doesn't test for node names.
   *
   *
   * @param whatToShow Bit set defined mainly by {@link org.w3c.dom.traversal.NodeFilter}.
   */
  public NodeTest(int whatToShow)
  {
    initNodeTest(whatToShow);
  }

  /**
   * @see Expression#deepEquals(Expression)
   */
  public boolean deepEquals(Expression expr)
  {
        if(!isSameClass(expr))
                return false;

        NodeTest nt = (NodeTest)expr;

        if(null != nt.m_name)
        {
                if(null == m_name)
                        return false;
                else if(!nt.m_name.equals(m_name))
                        return false;
        }
        else if(null != m_name)
                return false;

        if(null != nt.m_namespace)
        {
                if(null == m_namespace)
                        return false;
                else if(!nt.m_namespace.equals(m_namespace))
                        return false;
        }
        else if(null != m_namespace)
                return false;

        if(m_whatToShow != nt.m_whatToShow)
                return false;

        if(m_isTotallyWild != nt.m_isTotallyWild)
                return false;

        return true;
  }

  /**
   * Null argument constructor.
   */
  public NodeTest(){}

  /**
   * Initialize this node test by setting the whatToShow property, and
   * calculating the score that this test will return if a test succeeds.
   *
   *
   * @param whatToShow Bit set defined mainly by {@link org.w3c.dom.traversal.NodeFilter}.
   */
  public void initNodeTest(int whatToShow)
  {

    m_whatToShow = whatToShow;

    calcScore();
  }

  /**
   * Initialize this node test by setting the whatToShow property and the
   * namespace and local name, and
   * calculating the score that this test will return if a test succeeds.
   *
   *
   * @param whatToShow Bit set defined mainly by {@link org.w3c.dom.traversal.NodeFilter}.
   * @param namespace The namespace to be tested.
   * @param name The local name to be tested.
   */
  public void initNodeTest(int whatToShow, String namespace, String name)
  {

    m_whatToShow = whatToShow;
    m_namespace = namespace;
    m_name = name;

    calcScore();
  }

  /**
   * True if this test has a null namespace and a local name of {@link #WILD}.
   *  @serial
   */
  private boolean m_isTotallyWild;

  /**
   * Get the static score for this node test.
   * @return Should be one of the SCORE_XXX constants.
   */
  public XNumber getStaticScore()
  {
    return m_score;
  }

  /**
   * Set the static score for this node test.
   * @param score Should be one of the SCORE_XXX constants.
   */
  public void setStaticScore(XNumber score)
  {
    m_score = score;
  }

  /**
   * Static calc of match score.
   */
  protected void calcScore()
  {

    if ((m_namespace == null) && (m_name == null))
      m_score = SCORE_NODETEST;
    else if (((m_namespace == WILD) || (m_namespace == null))
             && (m_name == WILD))
      m_score = SCORE_NODETEST;
    else if ((m_namespace != WILD) && (m_name == WILD))
      m_score = SCORE_NSWILD;
    else
      m_score = SCORE_QNAME;

    m_isTotallyWild = (m_namespace == null && m_name == WILD);
  }

  /**
   * Get the score that this test will return if a test succeeds.
   *
   *
   * @return the score that this test will return if a test succeeds.
   */
  public double getDefaultScore()
  {
    return m_score.num();
  }

  /**
   * Tell what node type to test, if not DTMFilter.SHOW_ALL.
   *
   * @param whatToShow Bit set defined mainly by
   *        {@link com.sun.org.apache.xml.internal.dtm.DTMFilter}.
   * @return the node type for the whatToShow.  Since whatToShow can specify
   *         multiple types, it will return the first bit tested that is on,
   *         so the caller of this function should take care that this is
   *         the function they really want to call.  If none of the known bits
   *         are set, this function will return zero.
   */
  public static int getNodeTypeTest(int whatToShow)
  {
    // %REVIEW% Is there a better way?
    if (0 != (whatToShow & DTMFilter.SHOW_ELEMENT))
      return DTM.ELEMENT_NODE;

    if (0 != (whatToShow & DTMFilter.SHOW_ATTRIBUTE))
      return DTM.ATTRIBUTE_NODE;

    if (0 != (whatToShow & DTMFilter.SHOW_TEXT))
      return DTM.TEXT_NODE;

    if (0 != (whatToShow & DTMFilter.SHOW_DOCUMENT))
      return DTM.DOCUMENT_NODE;

    if (0 != (whatToShow & DTMFilter.SHOW_DOCUMENT_FRAGMENT))
      return DTM.DOCUMENT_FRAGMENT_NODE;

    if (0 != (whatToShow & DTMFilter.SHOW_NAMESPACE))
      return DTM.NAMESPACE_NODE;

    if (0 != (whatToShow & DTMFilter.SHOW_COMMENT))
      return DTM.COMMENT_NODE;

    if (0 != (whatToShow & DTMFilter.SHOW_PROCESSING_INSTRUCTION))
      return DTM.PROCESSING_INSTRUCTION_NODE;

    if (0 != (whatToShow & DTMFilter.SHOW_DOCUMENT_TYPE))
      return DTM.DOCUMENT_TYPE_NODE;

    if (0 != (whatToShow & DTMFilter.SHOW_ENTITY))
      return DTM.ENTITY_NODE;

    if (0 != (whatToShow & DTMFilter.SHOW_ENTITY_REFERENCE))
      return DTM.ENTITY_REFERENCE_NODE;

    if (0 != (whatToShow & DTMFilter.SHOW_NOTATION))
      return DTM.NOTATION_NODE;

    if (0 != (whatToShow & DTMFilter.SHOW_CDATA_SECTION))
      return DTM.CDATA_SECTION_NODE;


    return 0;
  }


  /**
   * Do a diagnostics dump of a whatToShow bit set.
   *
   *
   * @param whatToShow Bit set defined mainly by
   *        {@link com.sun.org.apache.xml.internal.dtm.DTMFilter}.
   */
  public static void debugWhatToShow(int whatToShow)
  {

    List<String> v = new ArrayList<>();

    if (0 != (whatToShow & DTMFilter.SHOW_ATTRIBUTE))
      v.add("SHOW_ATTRIBUTE");

    if (0 != (whatToShow & DTMFilter.SHOW_NAMESPACE))
      v.add("SHOW_NAMESPACE");

    if (0 != (whatToShow & DTMFilter.SHOW_CDATA_SECTION))
      v.add("SHOW_CDATA_SECTION");

    if (0 != (whatToShow & DTMFilter.SHOW_COMMENT))
      v.add("SHOW_COMMENT");

    if (0 != (whatToShow & DTMFilter.SHOW_DOCUMENT))
      v.add("SHOW_DOCUMENT");

    if (0 != (whatToShow & DTMFilter.SHOW_DOCUMENT_FRAGMENT))
      v.add("SHOW_DOCUMENT_FRAGMENT");

    if (0 != (whatToShow & DTMFilter.SHOW_DOCUMENT_TYPE))
      v.add("SHOW_DOCUMENT_TYPE");

    if (0 != (whatToShow & DTMFilter.SHOW_ELEMENT))
      v.add("SHOW_ELEMENT");

    if (0 != (whatToShow & DTMFilter.SHOW_ENTITY))
      v.add("SHOW_ENTITY");

    if (0 != (whatToShow & DTMFilter.SHOW_ENTITY_REFERENCE))
      v.add("SHOW_ENTITY_REFERENCE");

    if (0 != (whatToShow & DTMFilter.SHOW_NOTATION))
      v.add("SHOW_NOTATION");

    if (0 != (whatToShow & DTMFilter.SHOW_PROCESSING_INSTRUCTION))
      v.add("SHOW_PROCESSING_INSTRUCTION");

    if (0 != (whatToShow & DTMFilter.SHOW_TEXT))
      v.add("SHOW_TEXT");

    int n = v.size();

    for (int i = 0; i < n; i++)
    {
      if (i > 0)
        System.out.print(" | ");

      System.out.print(v.get(i));
    }

    if (0 == n)
      System.out.print("empty whatToShow: " + whatToShow);

    System.out.println();
  }

  /**
   * Two names are equal if they and either both are null or
   * the name t is wild and the name p is non-null, or the two
   * strings are equal.
   *
   * @param p part string from the node.
   * @param t target string, which may be {@link #WILD}.
   *
   * @return true if the strings match according to the rules of this method.
   */
  private static final boolean subPartMatch(String p, String t)
  {

    // boolean b = (p == t) || ((null != p) && ((t == WILD) || p.equals(t)));
    // System.out.println("subPartMatch - p: "+p+", t: "+t+", result: "+b);
    return (p == t) || ((null != p) && ((t == WILD) || p.equals(t)));
  }

  /**
   * This is temporary to patch over Xerces issue with representing DOM
   * namespaces as "".
   *
   * @param p part string from the node, which may represent the null namespace
   *        as null or as "".
   * @param t target string, which may be {@link #WILD}.
   *
   * @return true if the strings match according to the rules of this method.
   */
  private static final boolean subPartMatchNS(String p, String t)
  {

    return (p == t)
           || ((null != p)
               && ((p.length() > 0)
                   ? ((t == WILD) || p.equals(t)) : null == t));
  }

  /**
   * Tell what the test score is for the given node.
   *
   *
   * @param xctxt XPath runtime context.
   * @param context The node being tested.
   *
   * @return {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_NODETEST},
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_NONE},
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_NSWILD},
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_QNAME}, or
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_OTHER}.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public XObject execute(XPathContext xctxt, int context)
          throws javax.xml.transform.TransformerException
  {

    DTM dtm = xctxt.getDTM(context);
    short nodeType = dtm.getNodeType(context);

    if (m_whatToShow == DTMFilter.SHOW_ALL)
      return m_score;

    int nodeBit = (m_whatToShow & (0x00000001 << (nodeType - 1)));

    switch (nodeBit)
    {
    case DTMFilter.SHOW_DOCUMENT_FRAGMENT :
    case DTMFilter.SHOW_DOCUMENT :
      return SCORE_OTHER;
    case DTMFilter.SHOW_COMMENT :
      return m_score;
    case DTMFilter.SHOW_CDATA_SECTION :
    case DTMFilter.SHOW_TEXT :

      // was:
      // return (!xctxt.getDOMHelper().shouldStripSourceNode(context))
      //       ? m_score : SCORE_NONE;
      return m_score;
    case DTMFilter.SHOW_PROCESSING_INSTRUCTION :
      return subPartMatch(dtm.getNodeName(context), m_name)
             ? m_score : SCORE_NONE;

    // From the draft: "Two expanded names are equal if they
    // have the same local part, and either both have no URI or
    // both have the same URI."
    // "A node test * is true for any node of the principal node type.
    // For example, child::* will select all element children of the
    // context node, and attribute::* will select all attributes of
    // the context node."
    // "A node test can have the form NCName:*. In this case, the prefix
    // is expanded in the same way as with a QName using the context
    // namespace declarations. The node test will be true for any node
    // of the principal type whose expanded name has the URI to which
    // the prefix expands, regardless of the local part of the name."
    case DTMFilter.SHOW_NAMESPACE :
    {
      String ns = dtm.getLocalName(context);

      return (subPartMatch(ns, m_name)) ? m_score : SCORE_NONE;
    }
    case DTMFilter.SHOW_ATTRIBUTE :
    case DTMFilter.SHOW_ELEMENT :
    {
      return (m_isTotallyWild || (subPartMatchNS(dtm.getNamespaceURI(context), m_namespace) && subPartMatch(dtm.getLocalName(context), m_name)))
             ? m_score : SCORE_NONE;
    }
    default :
      return SCORE_NONE;
    }  // end switch(testType)
  }

  /**
   * Tell what the test score is for the given node.
   *
   *
   * @param xctxt XPath runtime context.
   * @param context The node being tested.
   *
   * @return {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_NODETEST},
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_NONE},
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_NSWILD},
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_QNAME}, or
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_OTHER}.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public XObject execute(XPathContext xctxt, int context,
                         DTM dtm, int expType)
          throws javax.xml.transform.TransformerException
  {

    if (m_whatToShow == DTMFilter.SHOW_ALL)
      return m_score;

    int nodeBit = (m_whatToShow & (0x00000001
                   << ((dtm.getNodeType(context)) - 1)));

    switch (nodeBit)
    {
    case DTMFilter.SHOW_DOCUMENT_FRAGMENT :
    case DTMFilter.SHOW_DOCUMENT :
      return SCORE_OTHER;
    case DTMFilter.SHOW_COMMENT :
      return m_score;
    case DTMFilter.SHOW_CDATA_SECTION :
    case DTMFilter.SHOW_TEXT :

      // was:
      // return (!xctxt.getDOMHelper().shouldStripSourceNode(context))
      //       ? m_score : SCORE_NONE;
      return m_score;
    case DTMFilter.SHOW_PROCESSING_INSTRUCTION :
      return subPartMatch(dtm.getNodeName(context), m_name)
             ? m_score : SCORE_NONE;

    // From the draft: "Two expanded names are equal if they
    // have the same local part, and either both have no URI or
    // both have the same URI."
    // "A node test * is true for any node of the principal node type.
    // For example, child::* will select all element children of the
    // context node, and attribute::* will select all attributes of
    // the context node."
    // "A node test can have the form NCName:*. In this case, the prefix
    // is expanded in the same way as with a QName using the context
    // namespace declarations. The node test will be true for any node
    // of the principal type whose expanded name has the URI to which
    // the prefix expands, regardless of the local part of the name."
    case DTMFilter.SHOW_NAMESPACE :
    {
      String ns = dtm.getLocalName(context);

      return (subPartMatch(ns, m_name)) ? m_score : SCORE_NONE;
    }
    case DTMFilter.SHOW_ATTRIBUTE :
    case DTMFilter.SHOW_ELEMENT :
    {
      return (m_isTotallyWild || (subPartMatchNS(dtm.getNamespaceURI(context), m_namespace) && subPartMatch(dtm.getLocalName(context), m_name)))
             ? m_score : SCORE_NONE;
    }
    default :
      return SCORE_NONE;
    }  // end switch(testType)
  }

  /**
   * Test the current node to see if it matches the given node test.
   *
   * @param xctxt XPath runtime context.
   *
   * @return {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_NODETEST},
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_NONE},
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_NSWILD},
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_QNAME}, or
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_OTHER}.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public XObject execute(XPathContext xctxt)
          throws javax.xml.transform.TransformerException
  {
    return execute(xctxt, xctxt.getCurrentNode());
  }

  /**
   * Node tests by themselves do not need to fix up variables.
   */
  public void fixupVariables(List<QName> vars, int globalsSize)
  {
    // no-op
  }

  /**
   * @see com.sun.org.apache.xpath.internal.XPathVisitable#callVisitors(ExpressionOwner, XPathVisitor)
   */
  public void callVisitors(ExpressionOwner owner, XPathVisitor visitor)
  {
        assertion(false, "callVisitors should not be called for this object!!!");
  }

}
