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

import com.sun.org.apache.xml.internal.dtm.Axis;
import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.dtm.DTMAxisTraverser;
import com.sun.org.apache.xml.internal.dtm.DTMFilter;
import com.sun.org.apache.xml.internal.utils.QName;
import com.sun.org.apache.xpath.internal.Expression;
import com.sun.org.apache.xpath.internal.ExpressionOwner;
import com.sun.org.apache.xpath.internal.XPathContext;
import com.sun.org.apache.xpath.internal.XPathVisitor;
import com.sun.org.apache.xpath.internal.axes.SubContextList;
import com.sun.org.apache.xpath.internal.compiler.PsuedoNames;
import com.sun.org.apache.xpath.internal.objects.XObject;
import java.util.List;

/**
 * This class represents a single pattern match step.
 * @xsl.usage advanced
 * @LastModified: Oct 2017
 */
public class StepPattern extends NodeTest implements SubContextList, ExpressionOwner
{
    static final long serialVersionUID = 9071668960168152644L;

  /** The axis for this test. */
  protected int m_axis;

  /**
   * Construct a StepPattern that tests for namespaces and node names.
   *
   *
   * @param whatToShow Bit set defined mainly by {@link org.w3c.dom.traversal.NodeFilter}.
   * @param namespace The namespace to be tested.
   * @param name The local name to be tested.
   * @param axis The Axis for this test, one of of Axes.ANCESTORORSELF, etc.
   * @param axisForPredicate No longer used.
   */
  public StepPattern(int whatToShow, String namespace, String name, int axis,
                     int axisForPredicate)
  {

    super(whatToShow, namespace, name);

    m_axis = axis;
  }

  /**
   * Construct a StepPattern that doesn't test for node names.
   *
   *
   * @param whatToShow Bit set defined mainly by {@link org.w3c.dom.traversal.NodeFilter}.
   * @param axis The Axis for this test, one of of Axes.ANCESTORORSELF, etc.
   * @param axisForPredicate No longer used.
   */
  public StepPattern(int whatToShow, int axis, int axisForPredicate)
  {

    super(whatToShow);

    m_axis = axis;
  }

  /**
   * The target local name or psuedo name, for hash table lookup optimization.
   *  @serial
   */
  String m_targetString;  // only calculate on head

  /**
   * Calculate the local name or psuedo name of the node that this pattern will test,
   * for hash table lookup optimization.
   *
   * @see com.sun.org.apache.xpath.internal.compiler.PsuedoNames
   */
  public void calcTargetString()
  {

    int whatToShow = getWhatToShow();

    switch (whatToShow)
    {
    case DTMFilter.SHOW_COMMENT :
      m_targetString = PsuedoNames.PSEUDONAME_COMMENT;
      break;
    case DTMFilter.SHOW_TEXT :
    case DTMFilter.SHOW_CDATA_SECTION :
    case (DTMFilter.SHOW_TEXT | DTMFilter.SHOW_CDATA_SECTION) :
      m_targetString = PsuedoNames.PSEUDONAME_TEXT;
      break;
    case DTMFilter.SHOW_ALL :
      m_targetString = PsuedoNames.PSEUDONAME_ANY;
      break;
    case DTMFilter.SHOW_DOCUMENT :
    case DTMFilter.SHOW_DOCUMENT | DTMFilter.SHOW_DOCUMENT_FRAGMENT :
      m_targetString = PsuedoNames.PSEUDONAME_ROOT;
      break;
    case DTMFilter.SHOW_ELEMENT :
      if (WILD.equals(m_name))
        m_targetString = PsuedoNames.PSEUDONAME_ANY;
      else
        m_targetString = m_name;
      break;
    default :
      m_targetString = PsuedoNames.PSEUDONAME_ANY;
      break;
    }
  }

  /**
   * Get the local name or psuedo name of the node that this pattern will test,
   * for hash table lookup optimization.
   *
   *
   * @return local name or psuedo name of the node.
   * @see com.sun.org.apache.xpath.internal.compiler.PsuedoNames
   */
  public String getTargetString()
  {
    return m_targetString;
  }

  /**
   * Reference to nodetest and predicate for
   * parent or ancestor.
   * @serial
   */
  StepPattern m_relativePathPattern;

  /**
   * This function is used to fixup variables from QNames to stack frame
   * indexes at stylesheet build time.
   * @param vars List of QNames that correspond to variables.  This list
   * should be searched backwards for the first qualified name that
   * corresponds to the variable reference qname.  The position of the
   * QName in the vector from the start of the vector will be its position
   * in the stack frame (but variables above the globalsTop value will need
   * to be offset to the current stack frame).
   * @param globalsSize The number of variables in the global variable area.
   */
  public void fixupVariables(List<QName> vars, int globalsSize)
  {

    super.fixupVariables(vars, globalsSize);

    if (null != m_predicates)
    {
      for (int i = 0; i < m_predicates.length; i++)
      {
        m_predicates[i].fixupVariables(vars, globalsSize);
      }
    }

    if (null != m_relativePathPattern)
    {
      m_relativePathPattern.fixupVariables(vars, globalsSize);
    }
  }

  /**
   * Set the reference to nodetest and predicate for
   * parent or ancestor.
   *
   *
   * @param expr The relative pattern expression.
   */
  public void setRelativePathPattern(StepPattern expr)
  {

    m_relativePathPattern = expr;
    expr.exprSetParent(this);

    calcScore();
  }

  /**
   * Get the reference to nodetest and predicate for
   * parent or ancestor.
   *
   *
   * @return The relative pattern expression.
   */
  public StepPattern getRelativePathPattern()
  {
    return m_relativePathPattern;
  }

  //  /**
  //   * Set the list of predicate expressions for this pattern step.
  //   * @param predicates List of expression objects.
  //   */
  //  public void setPredicates(Expression[] predicates)
  //  {
  //    m_predicates = predicates;
  //  }

  /**
   * Set the list of predicate expressions for this pattern step.
   * @return List of expression objects.
   */
  public Expression[] getPredicates()
  {
    return m_predicates;
  }

  /**
   * The list of predicate expressions for this pattern step.
   *  @serial
   */
  Expression[] m_predicates;

  /**
   * Tell if this expression or it's subexpressions can traverse outside
   * the current subtree.
   *
   * NOTE: Ancestors tests with predicates are problematic, and will require
   * special treatment.
   *
   * @return true if traversal outside the context node's subtree can occur.
   */
  public boolean canTraverseOutsideSubtree()
  {

    int n = getPredicateCount();

    for (int i = 0; i < n; i++)
    {
      if (getPredicate(i).canTraverseOutsideSubtree())
        return true;
    }

    return false;
  }

  /**
   * Get a predicate expression.
   *
   *
   * @param i The index of the predicate.
   *
   * @return A predicate expression.
   */
  public Expression getPredicate(int i)
  {
    return m_predicates[i];
  }

  /**
   * Get the number of predicates for this match pattern step.
   *
   *
   * @return the number of predicates for this match pattern step.
   */
  public final int getPredicateCount()
  {
    return (null == m_predicates) ? 0 : m_predicates.length;
  }

  /**
   * Set the predicates for this match pattern step.
   *
   *
   * @param predicates An array of expressions that define predicates
   *                   for this step.
   */
  public void setPredicates(Expression[] predicates)
  {

    m_predicates = predicates;
    if(null != predicates)
    {
        for(int i = 0; i < predicates.length; i++)
        {
                predicates[i].exprSetParent(this);
        }
    }

    calcScore();
  }

  /**
   * Static calc of match score.
   */
  public void calcScore()
  {

    if ((getPredicateCount() > 0) || (null != m_relativePathPattern))
    {
      m_score = SCORE_OTHER;
    }
    else
      super.calcScore();

    if (null == m_targetString)
      calcTargetString();
  }

  /**
   * Execute this pattern step, including predicates.
   *
   *
   * @param xctxt XPath runtime context.
   * @param currentNode The current node context.
   *
   * @return {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_NODETEST},
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_NONE},
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_NSWILD},
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_QNAME}, or
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_OTHER}.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public XObject execute(XPathContext xctxt, int currentNode)
          throws javax.xml.transform.TransformerException
  {

    DTM dtm = xctxt.getDTM(currentNode);

    if (dtm != null)
    {
      int expType = dtm.getExpandedTypeID(currentNode);

      return execute(xctxt, currentNode, dtm, expType);
    }

    return NodeTest.SCORE_NONE;
  }

  /**
   * Execute this pattern step, including predicates.
   *
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
   * Execute an expression in the XPath runtime context, and return the
   * result of the expression.
   *
   *
   * @param xctxt The XPath runtime context.
   * @param currentNode The currentNode.
   * @param dtm The DTM of the current node.
   * @param expType The expanded type ID of the current node.
   *
   * @return The result of the expression in the form of a <code>XObject</code>.
   *
   * @throws javax.xml.transform.TransformerException if a runtime exception
   *         occurs.
   */
  public XObject execute(
          XPathContext xctxt, int currentNode, DTM dtm, int expType)
            throws javax.xml.transform.TransformerException
  {

    if (m_whatToShow == NodeTest.SHOW_BYFUNCTION)
    {
      if (null != m_relativePathPattern)
      {
        return m_relativePathPattern.execute(xctxt);
      }
      else
        return NodeTest.SCORE_NONE;
    }

    XObject score;

    score = super.execute(xctxt, currentNode, dtm, expType);

    if (score == NodeTest.SCORE_NONE)
      return NodeTest.SCORE_NONE;

    if (getPredicateCount() != 0)
    {
      if (!executePredicates(xctxt, dtm, currentNode))
        return NodeTest.SCORE_NONE;
    }

    if (null != m_relativePathPattern)
      return m_relativePathPattern.executeRelativePathPattern(xctxt, dtm,
              currentNode);

    return score;
  }

  /**
   * New Method to check whether the current node satisfies a position predicate
   *
   * @param xctxt The XPath runtime context.
   * @param predPos Which predicate we're evaluating of foo[1][2][3].
   * @param dtm The DTM of the current node.
   * @param context The currentNode.
   * @param pos The position being requested, i.e. the value returned by
   *            m_predicates[predPos].execute(xctxt).
   *
   * @return true of the position of the context matches pos, false otherwise.
   */
  private final boolean checkProximityPosition(XPathContext xctxt,
          int predPos, DTM dtm, int context, int pos)
  {

    try
    {
      DTMAxisTraverser traverser =
        dtm.getAxisTraverser(Axis.PRECEDINGSIBLING);

      for (int child = traverser.first(context); DTM.NULL != child;
              child = traverser.next(context, child))
      {
        try
        {
          xctxt.pushCurrentNode(child);

          if (NodeTest.SCORE_NONE != super.execute(xctxt, child))
          {
            boolean pass = true;

            try
            {
              xctxt.pushSubContextList(this);

              for (int i = 0; i < predPos; i++)
              {
                xctxt.pushPredicatePos(i);
                try
                {
                  XObject pred = m_predicates[i].execute(xctxt);

                  try
                  {
                    if (XObject.CLASS_NUMBER == pred.getType())
                    {
                      throw new Error("Why: Should never have been called");
                    }
                    else if (!pred.boolWithSideEffects())
                    {
                      pass = false;

                      break;
                    }
                  }
                  finally
                  {
                    pred.detach();
                  }
                }
                finally
                {
                  xctxt.popPredicatePos();
                }
              }
            }
            finally
            {
              xctxt.popSubContextList();
            }

            if (pass)
              pos--;

            if (pos < 1)
              return false;
          }
        }
        finally
        {
          xctxt.popCurrentNode();
        }
      }
    }
    catch (javax.xml.transform.TransformerException se)
    {

      // TODO: should keep throw sax exception...
      throw new java.lang.RuntimeException(se.getMessage());
    }

    return (pos == 1);
  }

  /**
   * Get the proximity position index of the current node based on this
   * node test.
   *
   *
   * @param xctxt XPath runtime context.
   * @param predPos Which predicate we're evaluating of foo[1][2][3].
   * @param findLast If true, don't terminate when the context node is found.
   *
   * @return the proximity position index of the current node based on the
   *         node test.
   */
  private final int getProximityPosition(XPathContext xctxt, int predPos,
                    boolean findLast)
  {

    int pos = 0;
    int context = xctxt.getCurrentNode();
    DTM dtm = xctxt.getDTM(context);
    int parent = dtm.getParent(context);

    try
    {
      DTMAxisTraverser traverser = dtm.getAxisTraverser(Axis.CHILD);

      for (int child = traverser.first(parent); DTM.NULL != child;
              child = traverser.next(parent, child))
      {
        try
        {
          xctxt.pushCurrentNode(child);

          if (NodeTest.SCORE_NONE != super.execute(xctxt, child))
          {
            boolean pass = true;

            try
            {
              xctxt.pushSubContextList(this);

              for (int i = 0; i < predPos; i++)
              {
                xctxt.pushPredicatePos(i);
                try
                {
                  XObject pred = m_predicates[i].execute(xctxt);

                  try
                  {
                    if (XObject.CLASS_NUMBER == pred.getType())
                    {
                      if ((pos + 1) != (int) pred.numWithSideEffects())
                      {
                        pass = false;

                        break;
                      }
                    }
                    else if (!pred.boolWithSideEffects())
                    {
                      pass = false;

                      break;
                    }
                  }
                  finally
                  {
                    pred.detach();
                  }
                }
                finally
                {
                  xctxt.popPredicatePos();
                }
              }
            }
            finally
            {
              xctxt.popSubContextList();
            }

            if (pass)
              pos++;

            if (!findLast && child == context)
            {
              return pos;
            }
          }
        }
        finally
        {
          xctxt.popCurrentNode();
        }
      }
    }
    catch (javax.xml.transform.TransformerException se)
    {

      // TODO: should keep throw sax exception...
      throw new java.lang.RuntimeException(se.getMessage());
    }

    return pos;
  }

  /**
   * Get the proximity position index of the current node based on this
   * node test.
   *
   *
   * @param xctxt XPath runtime context.
   *
   * @return the proximity position index of the current node based on the
   *         node test.
   */
  public int getProximityPosition(XPathContext xctxt)
  {
    return getProximityPosition(xctxt, xctxt.getPredicatePos(), false);
  }

  /**
   * Get the count of the nodes that match the test, which is the proximity
   * position of the last node that can pass this test in the sub context
   * selection.  In XSLT 1-based indexing, this count is the index of the last
   * node.
   *
   *
   * @param xctxt XPath runtime context.
   *
   * @return the count of the nodes that match the test.
   */
  public int getLastPos(XPathContext xctxt)
  {
    return getProximityPosition(xctxt, xctxt.getPredicatePos(), true);
  }

  /**
   * Execute the match pattern step relative to another step.
   *
   *
   * @param xctxt The XPath runtime context.
   * @param dtm The DTM of the current node.
   * @param currentNode The current node context.
   *
   * @return {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_NODETEST},
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_NONE},
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_NSWILD},
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_QNAME}, or
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_OTHER}.
   *
   * @throws javax.xml.transform.TransformerException
   */
  protected final XObject executeRelativePathPattern(
          XPathContext xctxt, DTM dtm, int currentNode)
            throws javax.xml.transform.TransformerException
  {

    XObject score = NodeTest.SCORE_NONE;
    int context = currentNode;
    DTMAxisTraverser traverser;

    traverser = dtm.getAxisTraverser(m_axis);

    for (int relative = traverser.first(context); DTM.NULL != relative;
            relative = traverser.next(context, relative))
    {
      try
      {
        xctxt.pushCurrentNode(relative);

        score = execute(xctxt);

        if (score != NodeTest.SCORE_NONE)
          break;
      }
      finally
      {
        xctxt.popCurrentNode();
      }
    }

    return score;
  }

  /**
   * Execute the predicates on this step to determine if the current node
   * should be filtered or accepted.
   *
   * @param xctxt The XPath runtime context.
   * @param dtm The DTM of the current node.
   * @param currentNode The current node context.
   *
   * @return true if the node should be accepted, false otherwise.
   *
   * @throws javax.xml.transform.TransformerException
   */
  protected final boolean executePredicates(
          XPathContext xctxt, DTM dtm, int currentNode)
            throws javax.xml.transform.TransformerException
  {

    boolean result = true;
    boolean positionAlreadySeen = false;
    int n = getPredicateCount();

    try
    {
      xctxt.pushSubContextList(this);

      for (int i = 0; i < n; i++)
      {
        xctxt.pushPredicatePos(i);

        try
        {
          XObject pred = m_predicates[i].execute(xctxt);

          try
          {
            if (XObject.CLASS_NUMBER == pred.getType())
            {
              int pos = (int) pred.num();

              if (positionAlreadySeen)
              {
                result = (pos == 1);

                break;
              }
              else
              {
                positionAlreadySeen = true;

                if (!checkProximityPosition(xctxt, i, dtm, currentNode, pos))
                {
                  result = false;

                  break;
                }
              }

            }
            else if (!pred.boolWithSideEffects())
            {
              result = false;

              break;
            }
          }
          finally
          {
            pred.detach();
          }
        }
        finally
        {
          xctxt.popPredicatePos();
        }
      }
    }
    finally
    {
      xctxt.popSubContextList();
    }

    return result;
  }

  /**
   * Get the string represenentation of this step for diagnostic purposes.
   *
   *
   * @return A string representation of this step, built by reverse-engineering
   * the contained info.
   */
  public String toString()
  {

    StringBuffer buf = new StringBuffer();

    for (StepPattern pat = this; pat != null; pat = pat.m_relativePathPattern)
    {
      if (pat != this)
        buf.append("/");

      buf.append(Axis.getNames(pat.m_axis));
      buf.append("::");

      if (0x000005000 == pat.m_whatToShow)
      {
        buf.append("doc()");
      }
      else if (DTMFilter.SHOW_BYFUNCTION == pat.m_whatToShow)
      {
        buf.append("function()");
      }
      else if (DTMFilter.SHOW_ALL == pat.m_whatToShow)
      {
        buf.append("node()");
      }
      else if (DTMFilter.SHOW_TEXT == pat.m_whatToShow)
      {
        buf.append("text()");
      }
      else if (DTMFilter.SHOW_PROCESSING_INSTRUCTION == pat.m_whatToShow)
      {
        buf.append("processing-instruction(");

        if (null != pat.m_name)
        {
          buf.append(pat.m_name);
        }

        buf.append(")");
      }
      else if (DTMFilter.SHOW_COMMENT == pat.m_whatToShow)
      {
        buf.append("comment()");
      }
      else if (null != pat.m_name)
      {
        if (DTMFilter.SHOW_ATTRIBUTE == pat.m_whatToShow)
        {
          buf.append("@");
        }

        if (null != pat.m_namespace)
        {
          buf.append("{");
          buf.append(pat.m_namespace);
          buf.append("}");
        }

        buf.append(pat.m_name);
      }
      else if (DTMFilter.SHOW_ATTRIBUTE == pat.m_whatToShow)
      {
        buf.append("@");
      }
      else if ((DTMFilter.SHOW_DOCUMENT | DTMFilter.SHOW_DOCUMENT_FRAGMENT)
               == pat.m_whatToShow)
      {
        buf.append("doc-root()");
      }
      else
      {
        buf.append('?').append(Integer.toHexString(pat.m_whatToShow));
      }

      if (null != pat.m_predicates)
      {
        for (int i = 0; i < pat.m_predicates.length; i++)
        {
          buf.append("[");
          buf.append(pat.m_predicates[i]);
          buf.append("]");
        }
      }
    }

    return buf.toString();
  }

  /** Set to true to send diagnostics about pattern matches to the consol. */
  private static final boolean DEBUG_MATCHES = false;

  /**
   * Get the match score of the given node.
   *
   * @param xctxt The XPath runtime context.
   * @param context The node to be tested.
   *
   * @return {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_NODETEST},
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_NONE},
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_NSWILD},
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_QNAME}, or
   *         {@link com.sun.org.apache.xpath.internal.patterns.NodeTest#SCORE_OTHER}.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public double getMatchScore(XPathContext xctxt, int context)
          throws javax.xml.transform.TransformerException
  {

    xctxt.pushCurrentNode(context);
    xctxt.pushCurrentExpressionNode(context);

    try
    {
      XObject score = execute(xctxt);

      return score.num();
    }
    finally
    {
      xctxt.popCurrentNode();
      xctxt.popCurrentExpressionNode();
    }

    // return XPath.MATCH_SCORE_NONE;
  }

  /**
   * Set the axis that this step should follow.
   *
   *
   * @param axis The Axis for this test, one of of Axes.ANCESTORORSELF, etc.
   */
  public void setAxis(int axis)
  {
    m_axis = axis;
  }

  /**
   * Get the axis that this step follows.
   *
   *
   * @return The Axis for this test, one of of Axes.ANCESTORORSELF, etc.
   */
  public int getAxis()
  {
    return m_axis;
  }

  class PredOwner implements ExpressionOwner
  {
        int m_index;

        PredOwner(int index)
        {
                m_index = index;
        }

    /**
     * @see ExpressionOwner#getExpression()
     */
    public Expression getExpression()
    {
      return m_predicates[m_index];
    }


    /**
     * @see ExpressionOwner#setExpression(Expression)
     */
    public void setExpression(Expression exp)
    {
        exp.exprSetParent(StepPattern.this);
        m_predicates[m_index] = exp;
    }
  }

  /**
   * @see com.sun.org.apache.xpath.internal.XPathVisitable#callVisitors(ExpressionOwner, XPathVisitor)
   */
  public void callVisitors(ExpressionOwner owner, XPathVisitor visitor)
  {
                if(visitor.visitMatchPattern(owner, this))
                {
                        callSubtreeVisitors(visitor);
                }
  }

  /**
   * Call the visitors on the subtree.  Factored out from callVisitors
   * so it may be called by derived classes.
   */
  protected void callSubtreeVisitors(XPathVisitor visitor)
  {
    if (null != m_predicates)
    {
      int n = m_predicates.length;
      for (int i = 0; i < n; i++)
      {
        ExpressionOwner predOwner = new PredOwner(i);
        if (visitor.visitPredicate(predOwner, m_predicates[i]))
        {
          m_predicates[i].callVisitors(predOwner, visitor);
        }
      }
    }
    if (null != m_relativePathPattern)
    {
      m_relativePathPattern.callVisitors(this, visitor);
    }
  }


  /**
   * @see ExpressionOwner#getExpression()
   */
  public Expression getExpression()
  {
    return m_relativePathPattern;
  }

  /**
   * @see ExpressionOwner#setExpression(Expression)
   */
  public void setExpression(Expression exp)
  {
    exp.exprSetParent(this);
        m_relativePathPattern = (StepPattern)exp;
  }

  /**
   * @see Expression#deepEquals(Expression)
   */
  public boolean deepEquals(Expression expr)
  {
        if(!super.deepEquals(expr))
                return false;

        StepPattern sp = (StepPattern)expr;

    if (null != m_predicates)
    {
        int n = m_predicates.length;
        if ((null == sp.m_predicates) || (sp.m_predicates.length != n))
              return false;
        for (int i = 0; i < n; i++)
        {
          if (!m_predicates[i].deepEquals(sp.m_predicates[i]))
                return false;
        }
    }
    else if (null != sp.m_predicates)
        return false;

        if(null != m_relativePathPattern)
        {
                if(!m_relativePathPattern.deepEquals(sp.m_relativePathPattern))
                        return false;
        }
        else if(sp.m_relativePathPattern != null)
                return false;

        return true;
  }


}
