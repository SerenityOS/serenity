/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.org.apache.xml.internal.dtm.DTMIterator;
import com.sun.org.apache.xml.internal.utils.QName;
import com.sun.org.apache.xpath.internal.Expression;
import com.sun.org.apache.xpath.internal.ExpressionOwner;
import com.sun.org.apache.xpath.internal.XPathContext;
import com.sun.org.apache.xpath.internal.XPathVisitor;
import com.sun.org.apache.xpath.internal.compiler.Compiler;
import com.sun.org.apache.xpath.internal.compiler.OpCodes;
import com.sun.org.apache.xpath.internal.objects.XNodeSet;
import java.util.List;

/**
 * Walker for the OP_VARIABLE, or OP_EXTFUNCTION, or OP_FUNCTION, or OP_GROUP,
 * op codes.
 * @see <a href="http://www.w3.org/TR/xpath#NT-FilterExpr">XPath FilterExpr descriptions</a>
 * @LastModified: May 2019
 */
public class FilterExprWalker extends AxesWalker
{
    static final long serialVersionUID = 5457182471424488375L;

  /**
   * Construct a FilterExprWalker using a LocPathIterator.
   *
   * @param locPathIterator non-null reference to the parent iterator.
   */
  public FilterExprWalker(WalkingIterator locPathIterator)
  {
    super(locPathIterator, Axis.FILTEREDLIST);
  }

  /**
   * Init a FilterExprWalker.
   *
   * @param compiler non-null reference to the Compiler that is constructing.
   * @param opPos positive opcode position for this step.
   * @param stepType The type of step.
   *
   * @throws javax.xml.transform.TransformerException
   */
  @SuppressWarnings("fallthrough")
  public void init(Compiler compiler, int opPos, int stepType)
          throws javax.xml.transform.TransformerException
  {

    super.init(compiler, opPos, stepType);

    // Smooth over an anomily in the opcode map...
    switch (stepType)
    {
    case OpCodes.OP_FUNCTION :
    case OpCodes.OP_EXTFUNCTION :
        m_mustHardReset = true;
    case OpCodes.OP_GROUP :
    case OpCodes.OP_VARIABLE :
      m_expr = compiler.compileExpression(opPos);
      m_expr.exprSetParent(this);
      //if((OpCodes.OP_FUNCTION == stepType) && (m_expr instanceof com.sun.org.apache.xalan.internal.templates.FuncKey))
      if(m_expr instanceof com.sun.org.apache.xpath.internal.operations.Variable)
      {
        // hack/temp workaround
        m_canDetachNodeset = false;
      }
      break;
    default :
      m_expr = compiler.compileExpression(opPos + 2);
      m_expr.exprSetParent(this);
    }
//    if(m_expr instanceof WalkingIterator)
//    {
//      WalkingIterator wi = (WalkingIterator)m_expr;
//      if(wi.getFirstWalker() instanceof FilterExprWalker)
//      {
//              FilterExprWalker fw = (FilterExprWalker)wi.getFirstWalker();
//              if(null == fw.getNextWalker())
//              {
//                      m_expr = fw.m_expr;
//                      m_expr.exprSetParent(this);
//              }
//      }
//
//    }
  }

  /**
   * Detaches the walker from the set which it iterated over, releasing
   * any computational resources and placing the iterator in the INVALID
   * state.
   */
  public void detach()
  {
        super.detach();
        if (m_canDetachNodeset)
        {
          m_exprObj.detach();
        }
        m_exprObj = null;
  }

  /**
   *  Set the root node of the TreeWalker.
   *
   * @param root non-null reference to the root, or starting point of
   *        the query.
   */
  public void setRoot(int root)
  {

    super.setRoot(root);

        m_exprObj = FilterExprIteratorSimple.executeFilterExpr(root,
                          m_lpi.getXPathContext(), m_lpi.getPrefixResolver(),
                          m_lpi.getIsTopLevel(), m_lpi.m_stackFrame, m_expr);

  }

  /**
   * Get a cloned FilterExprWalker.
   *
   * @return A new FilterExprWalker that can be used without mutating this one.
   *
   * @throws CloneNotSupportedException
   */
  public Object clone() throws CloneNotSupportedException
  {

    FilterExprWalker clone = (FilterExprWalker) super.clone();

    // clone.m_expr = (Expression)((Expression)m_expr).clone();
    if (null != m_exprObj)
      clone.m_exprObj = (XNodeSet) m_exprObj.clone();

    return clone;
  }

  /**
   * This method needs to override AxesWalker.acceptNode because FilterExprWalkers
   * don't need to, and shouldn't, do a node test.
   * @param n  The node to check to see if it passes the filter or not.
   * @return  a constant to determine whether the node is accepted,
   *   rejected, or skipped, as defined  above .
   */
  public short acceptNode(int n)
  {

    try
    {
      if (getPredicateCount() > 0)
      {
        countProximityPosition(0);

        if (!executePredicates(n, m_lpi.getXPathContext()))
          return DTMIterator.FILTER_SKIP;
      }

      return DTMIterator.FILTER_ACCEPT;
    }
    catch (javax.xml.transform.TransformerException se)
    {
      throw new RuntimeException(se.getMessage());
    }
  }

  /**
   *  Moves the <code>TreeWalker</code> to the next visible node in document
   * order relative to the current node, and returns the new node. If the
   * current node has no next node,  or if the search for nextNode attempts
   * to step upward from the TreeWalker's root node, returns
   * <code>null</code> , and retains the current node.
   * @return  The new node, or <code>null</code> if the current node has no
   *   next node  in the TreeWalker's logical view.
   */
  public int getNextNode()
  {

    if (null != m_exprObj)
    {
       int next = m_exprObj.nextNode();
       return next;
    }
    else
      return DTM.NULL;
  }

  /**
   * Get the index of the last node that can be itterated to.
   *
   *
   * @param xctxt XPath runtime context.
   *
   * @return the index of the last node that can be itterated to.
   */
  public int getLastPos(XPathContext xctxt)
  {
    return m_exprObj.getLength();
  }

  /** The contained expression. Should be non-null.
   *  @serial   */
  private Expression m_expr;

  /** The result of executing m_expr.  Needs to be deep cloned on clone op.  */
  transient private XNodeSet m_exprObj;

  private boolean m_mustHardReset = false;
  private boolean m_canDetachNodeset = true;

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

  /**
   * Returns the axis being iterated, if it is known.
   *
   * @return Axis.CHILD, etc., or -1 if the axis is not known or is of multiple
   * types.
   */
  public int getAxis()
  {
    return m_exprObj.getAxis();
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
        exp.exprSetParent(FilterExprWalker.this);
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

      FilterExprWalker walker = (FilterExprWalker)expr;
      if(!m_expr.deepEquals(walker.m_expr))
        return false;

      return true;
    }



}
