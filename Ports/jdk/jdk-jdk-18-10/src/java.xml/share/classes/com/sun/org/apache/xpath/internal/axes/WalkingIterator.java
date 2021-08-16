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

import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.utils.PrefixResolver;
import com.sun.org.apache.xml.internal.utils.QName;
import com.sun.org.apache.xpath.internal.Expression;
import com.sun.org.apache.xpath.internal.ExpressionOwner;
import com.sun.org.apache.xpath.internal.VariableStack;
import com.sun.org.apache.xpath.internal.XPathVisitor;
import com.sun.org.apache.xpath.internal.compiler.Compiler;
import com.sun.org.apache.xpath.internal.compiler.OpMap;
import java.util.List;

/**
 * Location path iterator that uses Walkers.
 *
 * @LastModified: Oct 2017
 */

public class WalkingIterator extends LocPathIterator implements ExpressionOwner
{
    static final long serialVersionUID = 9110225941815665906L;
  /**
   * Create a WalkingIterator iterator, including creation
   * of step walkers from the opcode list, and call back
   * into the Compiler to create predicate expressions.
   *
   * @param compiler The Compiler which is creating
   * this expression.
   * @param opPos The position of this iterator in the
   * opcode list from the compiler.
   * @param shouldLoadWalkers True if walkers should be
   * loaded, or false if this is a derived iterator and
   * it doesn't wish to load child walkers.
   *
   * @throws javax.xml.transform.TransformerException
   */
  WalkingIterator(
          Compiler compiler, int opPos, int analysis, boolean shouldLoadWalkers)
            throws javax.xml.transform.TransformerException
  {
    super(compiler, opPos, analysis, shouldLoadWalkers);

    int firstStepPos = OpMap.getFirstChildPos(opPos);

    if (shouldLoadWalkers)
    {
      m_firstWalker = WalkerFactory.loadWalkers(this, compiler, firstStepPos, 0);
      m_lastUsedWalker = m_firstWalker;
    }
  }

  /**
   * Create a WalkingIterator object.
   *
   * @param nscontext The namespace context for this iterator,
   * should be OK if null.
   */
  public WalkingIterator(PrefixResolver nscontext)
  {

    super(nscontext);
  }


  /**
   * Get the analysis bits for this walker, as defined in the WalkerFactory.
   * @return One of WalkerFactory#BIT_DESCENDANT, etc.
   */
  public int getAnalysisBits()
  {
    int bits = 0;
    if (null != m_firstWalker)
    {
      AxesWalker walker = m_firstWalker;

      while (null != walker)
      {
        int bit = walker.getAnalysisBits();
        bits |= bit;
        walker = walker.getNextWalker();
      }
    }
    return bits;
  }

  /**
   * Get a cloned WalkingIterator that holds the same
   * position as this iterator.
   *
   * @return A clone of this iterator that holds the same node position.
   *
   * @throws CloneNotSupportedException
   */
  public Object clone() throws CloneNotSupportedException
  {

    WalkingIterator clone = (WalkingIterator) super.clone();

    //    clone.m_varStackPos = this.m_varStackPos;
    //    clone.m_varStackContext = this.m_varStackContext;
    if (null != m_firstWalker)
    {
      clone.m_firstWalker = m_firstWalker.cloneDeep(clone, null);
    }

    return clone;
  }

  /**
   * Reset the iterator.
   */
  public void reset()
  {

    super.reset();

    if (null != m_firstWalker)
    {
      m_lastUsedWalker = m_firstWalker;

      m_firstWalker.setRoot(m_context);
    }

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

    if(null != m_firstWalker)
    {
      m_firstWalker.setRoot(context);
      m_lastUsedWalker = m_firstWalker;
    }
  }

  /**
   *  Returns the next node in the set and advances the position of the
   * iterator in the set. After a NodeIterator is created, the first call
   * to nextNode() returns the first node in the set.
   * @return  The next <code>Node</code> in the set being iterated over, or
   *   <code>null</code> if there are no more members in that set.
   */
  public int nextNode()
  {
        if(m_foundLast)
                return DTM.NULL;

    // If the variable stack position is not -1, we'll have to
    // set our position in the variable stack, so our variable access
    // will be correct.  Iterators that are at the top level of the
    // expression need to reset the variable stack, while iterators
    // in predicates do not need to, and should not, since their execution
    // may be much later than top-level iterators.
    // m_varStackPos is set in setRoot, which is called
    // from the execute method.
    if (-1 == m_stackFrame)
    {
      return returnNextNode(m_firstWalker.nextNode());
    }
    else
    {
      VariableStack vars = m_execContext.getVarStack();

      // These three statements need to be combined into one operation.
      int savedStart = vars.getStackFrame();

      vars.setStackFrame(m_stackFrame);

      int n = returnNextNode(m_firstWalker.nextNode());

      // These two statements need to be combined into one operation.
      vars.setStackFrame(savedStart);

      return n;
    }
  }


  /**
   * Get the head of the walker list.
   *
   * @return The head of the walker list, or null
   * if this iterator does not implement walkers.
   * @xsl.usage advanced
   */
  public final AxesWalker getFirstWalker()
  {
    return m_firstWalker;
  }

  /**
   * Set the head of the walker list.
   *
   * @param walker Should be a valid AxesWalker.
   * @xsl.usage advanced
   */
  public final void setFirstWalker(AxesWalker walker)
  {
    m_firstWalker = walker;
  }


  /**
   * Set the last used walker.
   *
   * @param walker The last used walker, or null.
   * @xsl.usage advanced
   */
  public final void setLastUsedWalker(AxesWalker walker)
  {
    m_lastUsedWalker = walker;
  }

  /**
   * Get the last used walker.
   *
   * @return The last used walker, or null.
   * @xsl.usage advanced
   */
  public final AxesWalker getLastUsedWalker()
  {
    return m_lastUsedWalker;
  }

  /**
   *  Detaches the iterator from the set which it iterated over, releasing
   * any computational resources and placing the iterator in the INVALID
   * state. After<code>detach</code> has been invoked, calls to
   * <code>nextNode</code> or<code>previousNode</code> will raise the
   * exception INVALID_STATE_ERR.
   */
  public void detach()
  {
    if(m_allowDetach)
    {
                AxesWalker walker = m_firstWalker;
            while (null != walker)
            {
              walker.detach();
              walker = walker.getNextWalker();
            }

            m_lastUsedWalker = null;

            // Always call the superclass detach last!
            super.detach();
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
    m_predicateIndex = -1;

    AxesWalker walker = m_firstWalker;

    while (null != walker)
    {
      walker.fixupVariables(vars, globalsSize);
      walker = walker.getNextWalker();
    }
  }

  /**
   * @see com.sun.org.apache.xpath.internal.XPathVisitable#callVisitors(ExpressionOwner, XPathVisitor)
   */
  public void callVisitors(ExpressionOwner owner, XPathVisitor visitor)
  {
                if(visitor.visitLocationPath(owner, this))
                {
                        if(null != m_firstWalker)
                        {
                                m_firstWalker.callVisitors(this, visitor);
                        }
                }
  }


  /** The last used step walker in the walker list.
   *  @serial */
  protected AxesWalker m_lastUsedWalker;

  /** The head of the step walker list.
   *  @serial */
  protected AxesWalker m_firstWalker;

  /**
   * @see ExpressionOwner#getExpression()
   */
  public Expression getExpression()
  {
    return m_firstWalker;
  }

  /**
   * @see ExpressionOwner#setExpression(Expression)
   */
  public void setExpression(Expression exp)
  {
        exp.exprSetParent(this);
        m_firstWalker = (AxesWalker)exp;
  }

    /**
     * @see Expression#deepEquals(Expression)
     */
    public boolean deepEquals(Expression expr)
    {
      if (!super.deepEquals(expr))
                return false;

      AxesWalker walker1 = m_firstWalker;
      AxesWalker walker2 = ((WalkingIterator)expr).m_firstWalker;
      while ((null != walker1) && (null != walker2))
      {
        if(!walker1.deepEquals(walker2))
                return false;
        walker1 = walker1.getNextWalker();
        walker2 = walker2.getNextWalker();
      }

      if((null != walker1) || (null != walker2))
        return false;

      return true;
    }

}
