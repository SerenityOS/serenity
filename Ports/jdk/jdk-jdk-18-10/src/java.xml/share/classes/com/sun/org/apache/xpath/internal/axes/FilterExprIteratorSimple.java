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

package com.sun.org.apache.xpath.internal.axes;

import com.sun.org.apache.xml.internal.dtm.Axis;
import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.utils.PrefixResolver;
import com.sun.org.apache.xml.internal.utils.QName;
import com.sun.org.apache.xpath.internal.Expression;
import com.sun.org.apache.xpath.internal.ExpressionOwner;
import com.sun.org.apache.xpath.internal.VariableStack;
import com.sun.org.apache.xpath.internal.XPathContext;
import com.sun.org.apache.xpath.internal.XPathVisitor;
import com.sun.org.apache.xpath.internal.objects.XNodeSet;
import java.util.List;

/**
 * Class to use for one-step iteration that doesn't have a predicate, and
 * doesn't need to set the context.
 *
 * @LastModified: Oct 2017
 */
public class FilterExprIteratorSimple extends LocPathIterator
{
    static final long serialVersionUID = -6978977187025375579L;
  /** The contained expression. Should be non-null.
   *  @serial   */
  private Expression m_expr;

  /** The result of executing m_expr.  Needs to be deep cloned on clone op.  */
  transient private XNodeSet m_exprObj;

  private boolean m_mustHardReset = false;
  private boolean m_canDetachNodeset = true;

  /**
   * Create a FilterExprIteratorSimple object.
   *
   */
  public FilterExprIteratorSimple()
  {
    super(null);
  }

  /**
   * Create a FilterExprIteratorSimple object.
   *
   */
  public FilterExprIteratorSimple(Expression expr)
  {
    super(null);
    m_expr = expr;
  }

  /**
   * Initialize the context values for this expression
   * after it is cloned.
   *
   * @param context The XPath runtime context for this
   * transformation.
   */
  public void setRoot(int context, Object environment)
  {
        super.setRoot(context, environment);
        m_exprObj = executeFilterExpr(context, m_execContext, getPrefixResolver(),
                          getIsTopLevel(), m_stackFrame, m_expr);
  }

  /**
   * Execute the expression.  Meant for reuse by other FilterExpr iterators
   * that are not derived from this object.
   */
  public static XNodeSet executeFilterExpr(int context, XPathContext xctxt,
                                                                                                PrefixResolver prefixResolver,
                                                                                                boolean isTopLevel,
                                                                                                int stackFrame,
                                                                                                Expression expr )
    throws com.sun.org.apache.xml.internal.utils.WrappedRuntimeException
  {
    PrefixResolver savedResolver = xctxt.getNamespaceContext();
    XNodeSet result = null;

    try
    {
      xctxt.pushCurrentNode(context);
      xctxt.setNamespaceContext(prefixResolver);

      // The setRoot operation can take place with a reset operation,
      // and so we may not be in the context of LocPathIterator#nextNode,
      // so we have to set up the variable context, execute the expression,
      // and then restore the variable context.

      if (isTopLevel)
      {
        // System.out.println("calling m_expr.execute(getXPathContext())");
        VariableStack vars = xctxt.getVarStack();

        // These three statements need to be combined into one operation.
        int savedStart = vars.getStackFrame();
        vars.setStackFrame(stackFrame);

        result = (com.sun.org.apache.xpath.internal.objects.XNodeSet) expr.execute(xctxt);
        result.setShouldCacheNodes(true);

        // These two statements need to be combined into one operation.
        vars.setStackFrame(savedStart);
      }
      else
          result = (com.sun.org.apache.xpath.internal.objects.XNodeSet) expr.execute(xctxt);

    }
    catch (javax.xml.transform.TransformerException se)
    {

      // TODO: Fix...
      throw new com.sun.org.apache.xml.internal.utils.WrappedRuntimeException(se);
    }
    finally
    {
      xctxt.popCurrentNode();
      xctxt.setNamespaceContext(savedResolver);
    }
    return result;
  }

  /**
   *  Returns the next node in the set and advances the position of the
   * iterator in the set. After a NodeIterator is created, the first call
   * to nextNode() returns the first node in the set.
   *
   * @return  The next <code>Node</code> in the set being iterated over, or
   *   <code>null</code> if there are no more members in that set.
   */
  public int nextNode()
  {
        if(m_foundLast)
                return DTM.NULL;

    int next;

    if (null != m_exprObj)
    {
      m_lastFetched = next = m_exprObj.nextNode();
    }
    else
      m_lastFetched = next = DTM.NULL;

    // m_lastFetched = next;
    if (DTM.NULL != next)
    {
      m_pos++;
      return next;
    }
    else
    {
      m_foundLast = true;

      return DTM.NULL;
    }
  }

  /**
   * Detaches the walker from the set which it iterated over, releasing
   * any computational resources and placing the iterator in the INVALID
   * state.
   */
  public void detach()
  {
    if(m_allowDetach)
    {
                super.detach();
                m_exprObj.detach();
                m_exprObj = null;
    }
  }

  /**
   * This function is used to fixup variables from QNames to stack frame
   * indexes at stylesheet build time.
   * @param vars List of QNames that correspond to variables.  This list
   * should be searched backwards for the first qualified name that
   * corresponds to the variable reference qname.  The position of the
   * QName in the vector from the start of the vector will be its position
   * in the stack frame (but variables above the globalsTop value will need
   * to be offset to the current stack frame).
   */
  public void fixupVariables(List<QName> vars, int globalsSize)
  {
    super.fixupVariables(vars, globalsSize);
    m_expr.fixupVariables(vars, globalsSize);
  }

  /**
   * Get the inner contained expression of this filter.
   */
  public Expression getInnerExpression()
  {
    return m_expr;
  }

  /**
   * Set the inner contained expression of this filter.
   */
  public void setInnerExpression(Expression expr)
  {
    expr.exprSetParent(this);
    m_expr = expr;
  }

  /**
   * Get the analysis bits for this walker, as defined in the WalkerFactory.
   * @return One of WalkerFactory#BIT_DESCENDANT, etc.
   */
  public int getAnalysisBits()
  {
    if (null != m_expr && m_expr instanceof PathComponent)
    {
      return ((PathComponent) m_expr).getAnalysisBits();
    }
    return WalkerFactory.BIT_FILTER;
  }

  /**
   * Returns true if all the nodes in the iteration well be returned in document
   * order.
   * Warning: This can only be called after setRoot has been called!
   *
   * @return true as a default.
   */
  public boolean isDocOrdered()
  {
    return m_exprObj.isDocOrdered();
  }

  class filterExprOwner implements ExpressionOwner
  {
    /**
    * @see ExpressionOwner#getExpression()
    */
    public Expression getExpression()
    {
      return m_expr;
    }

    /**
     * @see ExpressionOwner#setExpression(Expression)
     */
    public void setExpression(Expression exp)
    {
      exp.exprSetParent(FilterExprIteratorSimple.this);
      m_expr = exp;
    }

  }

  /**
   * This will traverse the heararchy, calling the visitor for
   * each member.  If the called visitor method returns
   * false, the subtree should not be called.
   *
   * @param visitor The visitor whose appropriate method will be called.
   */
  public void callPredicateVisitors(XPathVisitor visitor)
  {
    m_expr.callVisitors(new filterExprOwner(), visitor);

    super.callPredicateVisitors(visitor);
  }

  /**
   * @see Expression#deepEquals(Expression)
   */
  public boolean deepEquals(Expression expr)
  {
    if (!super.deepEquals(expr))
      return false;

    FilterExprIteratorSimple fet = (FilterExprIteratorSimple) expr;
    if (!m_expr.deepEquals(fet.m_expr))
      return false;

    return true;
  }

  /**
   * Returns the axis being iterated, if it is known.
   *
   * @return Axis.CHILD, etc., or -1 if the axis is not known or is of multiple
   * types.
   */
  public int getAxis()
  {
        if(null != m_exprObj)
        return m_exprObj.getAxis();
    else
        return Axis.FILTEREDLIST;
  }


}
