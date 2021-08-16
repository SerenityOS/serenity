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

import com.sun.org.apache.xalan.internal.res.XSLMessages;
import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.dtm.DTMAxisTraverser;
import com.sun.org.apache.xml.internal.dtm.DTMIterator;
import com.sun.org.apache.xpath.internal.Expression;
import com.sun.org.apache.xpath.internal.ExpressionOwner;
import com.sun.org.apache.xpath.internal.XPathContext;
import com.sun.org.apache.xpath.internal.XPathVisitor;
import com.sun.org.apache.xpath.internal.compiler.Compiler;
import com.sun.org.apache.xpath.internal.res.XPATHErrorResources;
import java.util.List;

/**
 * Serves as common interface for axes Walkers, and stores common
 * state variables.
 *
 * @LastModified: Oct 2017
 */
public class AxesWalker extends PredicatedNodeTest
        implements Cloneable, PathComponent, ExpressionOwner
{
    static final long serialVersionUID = -2966031951306601247L;

  /**
   * Construct an AxesWalker using a LocPathIterator.
   *
   * @param locPathIterator non-null reference to the parent iterator.
   */
  public AxesWalker(LocPathIterator locPathIterator, int axis)
  {
    super( locPathIterator );
    m_axis = axis;
  }

  public final WalkingIterator wi()
  {
    return (WalkingIterator)m_lpi;
  }

  /**
   * Initialize an AxesWalker during the parse of the XPath expression.
   *
   * @param compiler The Compiler object that has information about this
   *                 walker in the op map.
   * @param opPos The op code position of this location step.
   * @param stepType  The type of location step.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public void init(Compiler compiler, int opPos, int stepType)
          throws javax.xml.transform.TransformerException
  {

    initPredicateInfo(compiler, opPos);

    // int testType = compiler.getOp(nodeTestOpPos);
  }

  /**
   * Get a cloned AxesWalker.
   *
   * @return A new AxesWalker that can be used without mutating this one.
   *
   * @throws CloneNotSupportedException
   */
  public Object clone() throws CloneNotSupportedException
  {
    // Do not access the location path itterator during this operation!

    AxesWalker clone = (AxesWalker) super.clone();

    //clone.setCurrentNode(clone.m_root);

    // clone.m_isFresh = true;

    return clone;
  }

  /**
   * Do a deep clone of this walker, including next and previous walkers.
   * If the this AxesWalker is on the clone list, don't clone but
   * return the already cloned version.
   *
   * @param cloneOwner non-null reference to the cloned location path
   *                   iterator to which this clone will be added.
   * @param cloneList non-null vector of sources in odd elements, and the
   *                  corresponding clones in even vectors.
   *
   * @return non-null clone, which may be a new clone, or may be a clone
   *         contained on the cloneList.
   */
  AxesWalker cloneDeep(WalkingIterator cloneOwner, List<AxesWalker> cloneList)
     throws CloneNotSupportedException
  {
    AxesWalker clone = findClone(this, cloneList);
    if(null != clone)
      return clone;
    clone = (AxesWalker)this.clone();
    clone.setLocPathIterator(cloneOwner);
    if(null != cloneList)
    {
      cloneList.add(this);
      cloneList.add(clone);
    }

    if(wi().m_lastUsedWalker == this)
      cloneOwner.m_lastUsedWalker = clone;

    if(null != m_nextWalker)
      clone.m_nextWalker = m_nextWalker.cloneDeep(cloneOwner, cloneList);

    // If you don't check for the cloneList here, you'll go into an
    // recursive infinate loop.
    if(null != cloneList)
    {
      if(null != m_prevWalker)
        clone.m_prevWalker = m_prevWalker.cloneDeep(cloneOwner, cloneList);
    }
    else
    {
      if(null != m_nextWalker)
        clone.m_nextWalker.m_prevWalker = clone;
    }
    return clone;
  }

  /**
   * Find a clone that corresponds to the key argument.
   *
   * @param key The original AxesWalker for which there may be a clone.
   * @param cloneList vector of sources in odd elements, and the
   *                  corresponding clones in even vectors, may be null.
   *
   * @return A clone that corresponds to the key, or null if key not found.
   */
  static AxesWalker findClone(AxesWalker key, List<AxesWalker> cloneList)
  {
    if(null != cloneList)
    {
      // First, look for clone on list.
      int n = cloneList.size();
      for (int i = 0; i < n; i+=2)
      {
        if(key == cloneList.get(i))
          return cloneList.get(i+1);
      }
    }
    return null;
  }

  /**
   * Detaches the walker from the set which it iterated over, releasing
   * any computational resources and placing the iterator in the INVALID
   * state.
   */
  public void detach()
  {
        m_currentNode = DTM.NULL;
        m_dtm = null;
        m_traverser = null;
        m_isFresh = true;
        m_root = DTM.NULL;
  }

  //=============== TreeWalker Implementation ===============

  /**
   * The root node of the TreeWalker, as specified in setRoot(int root).
   * Note that this may actually be below the current node.
   *
   * @return The context node of the step.
   */
  public int getRoot()
  {
    return m_root;
  }

  /**
   * Get the analysis bits for this walker, as defined in the WalkerFactory.
   * @return One of WalkerFactory#BIT_DESCENDANT, etc.
   */
  public int getAnalysisBits()
  {
        int axis = getAxis();
        int bit = WalkerFactory.getAnalysisBitFromAxes(axis);
        return bit;
  }

  /**
   * Set the root node of the TreeWalker.
   * (Not part of the DOM2 TreeWalker interface).
   *
   * @param root The context node of this step.
   */
  public void setRoot(int root)
  {
    // %OPT% Get this directly from the lpi.
    XPathContext xctxt = wi().getXPathContext();
    m_dtm = xctxt.getDTM(root);
    m_traverser = m_dtm.getAxisTraverser(m_axis);
    m_isFresh = true;
    m_foundLast = false;
    m_root = root;
    m_currentNode = root;

    if (DTM.NULL == root)
    {
      throw new RuntimeException(
        XSLMessages.createXPATHMessage(XPATHErrorResources.ER_SETTING_WALKER_ROOT_TO_NULL, null)); //"\n !!!! Error! Setting the root of a walker to null!!!");
    }

    resetProximityPositions();
  }

  /**
   * The node at which the TreeWalker is currently positioned.
   * <br> The value must not be null. Alterations to the DOM tree may cause
   * the current node to no longer be accepted by the TreeWalker's
   * associated filter. currentNode may also be explicitly set to any node,
   * whether or not it is within the subtree specified by the root node or
   * would be accepted by the filter and whatToShow flags. Further
   * traversal occurs relative to currentNode even if it is not part of the
   * current view by applying the filters in the requested direction (not
   * changing currentNode where no traversal is possible).
   *
   * @return The node at which the TreeWalker is currently positioned, only null
   * if setRoot has not yet been called.
   */
  public final int getCurrentNode()
  {
    return m_currentNode;
  }

  /**
   * Set the next walker in the location step chain.
   *
   *
   * @param walker Reference to AxesWalker derivative, or may be null.
   */
  public void setNextWalker(AxesWalker walker)
  {
    m_nextWalker = walker;
  }

  /**
   * Get the next walker in the location step chain.
   *
   *
   * @return Reference to AxesWalker derivative, or null.
   */
  public AxesWalker getNextWalker()
  {
    return m_nextWalker;
  }

  /**
   * Set or clear the previous walker reference in the location step chain.
   *
   *
   * @param walker Reference to previous walker reference in the location
   *               step chain, or null.
   */
  public void setPrevWalker(AxesWalker walker)
  {
    m_prevWalker = walker;
  }

  /**
   * Get the previous walker reference in the location step chain.
   *
   *
   * @return Reference to previous walker reference in the location
   *               step chain, or null.
   */
  public AxesWalker getPrevWalker()
  {
    return m_prevWalker;
  }

  /**
   * This is simply a way to bottle-neck the return of the next node, for
   * diagnostic purposes.
   *
   * @param n Node to return, or null.
   *
   * @return The argument.
   */
  private int returnNextNode(int n)
  {

    return n;
  }

  /**
   * Get the next node in document order on the axes.
   *
   * @return the next node in document order on the axes, or null.
   */
  protected int getNextNode()
  {
    if (m_foundLast)
      return DTM.NULL;

    if (m_isFresh)
    {
      m_currentNode = m_traverser.first(m_root);
      m_isFresh = false;
    }
    // I shouldn't have to do this the check for current node, I think.
    // numbering\numbering24.xsl fails if I don't do this.  I think
    // it occurs as the walkers are backing up. -sb
    else if(DTM.NULL != m_currentNode)
    {
      m_currentNode = m_traverser.next(m_root, m_currentNode);
    }

    if (DTM.NULL == m_currentNode)
      this.m_foundLast = true;

    return m_currentNode;
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
  public int nextNode()
  {
    int nextNode = DTM.NULL;
    AxesWalker walker = wi().getLastUsedWalker();

    while (true)
    {
      if (null == walker)
        break;

      nextNode = walker.getNextNode();

      if (DTM.NULL == nextNode)
      {

        walker = walker.m_prevWalker;
      }
      else
      {
        if (walker.acceptNode(nextNode) != DTMIterator.FILTER_ACCEPT)
        {
          continue;
        }

        if (null == walker.m_nextWalker)
        {
          wi().setLastUsedWalker(walker);

          // return walker.returnNextNode(nextNode);
          break;
        }
        else
        {
          AxesWalker prev = walker;

          walker = walker.m_nextWalker;

          walker.setRoot(nextNode);

          walker.m_prevWalker = prev;

          continue;
        }
      }  // if(null != nextNode)
    }  // while(null != walker)

    return nextNode;
  }

  //============= End TreeWalker Implementation =============

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

    int pos = getProximityPosition();

    AxesWalker walker;

    try
    {
      walker = (AxesWalker) clone();
    }
    catch (CloneNotSupportedException cnse)
    {
      return -1;
    }

    walker.setPredicateCount(m_predicateIndex);
    walker.setNextWalker(null);
    walker.setPrevWalker(null);

    WalkingIterator lpi = wi();
    AxesWalker savedWalker = lpi.getLastUsedWalker();

    try
    {
      lpi.setLastUsedWalker(walker);

      int next;

      while (DTM.NULL != (next = walker.nextNode()))
      {
        pos++;
      }

      // TODO: Should probably save this in the iterator.
    }
    finally
    {
      lpi.setLastUsedWalker(savedWalker);
    }

    // System.out.println("pos: "+pos);
    return pos;
  }

  //============= State Data =============

  /**
   * The DTM for the root.  This can not be used, or must be changed,
   * for the filter walker, or any walker that can have nodes
   * from multiple documents.
   * Never, ever, access this value without going through getDTM(int node).
   */
  private DTM m_dtm;

  /**
   * Set the DTM for this walker.
   *
   * @param dtm Non-null reference to a DTM.
   */
  public void setDefaultDTM(DTM dtm)
  {
    m_dtm = dtm;
  }

  /**
   * Get the DTM for this walker.
   *
   * @return Non-null reference to a DTM.
   */
  public DTM getDTM(int node)
  {
    //
    return wi().getXPathContext().getDTM(node);
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
    return m_axis;
  }

  /**
   * This will traverse the heararchy, calling the visitor for
   * each member.  If the called visitor method returns
   * false, the subtree should not be called.
   *
   * @param owner The owner of the visitor, where that path may be
   *              rewritten if needed.
   * @param visitor The visitor whose appropriate method will be called.
   */
  public void callVisitors(ExpressionOwner owner, XPathVisitor visitor)
  {
        if(visitor.visitStep(owner, this))
        {
                callPredicateVisitors(visitor);
                if(null != m_nextWalker)
                {
                        m_nextWalker.callVisitors(this, visitor);
                }
        }
  }

  /**
   * @see ExpressionOwner#getExpression()
   */
  public Expression getExpression()
  {
    return m_nextWalker;
  }

  /**
   * @see ExpressionOwner#setExpression(Expression)
   */
  public void setExpression(Expression exp)
  {
        exp.exprSetParent(this);
        m_nextWalker = (AxesWalker)exp;
  }

    /**
     * @see Expression#deepEquals(Expression)
     */
    public boolean deepEquals(Expression expr)
    {
      if (!super.deepEquals(expr))
                return false;

      AxesWalker walker = (AxesWalker)expr;
      if(this.m_axis != walker.m_axis)
        return false;

      return true;
    }

  /**
   *  The root node of the TreeWalker, as specified when it was created.
   */
  transient int m_root = DTM.NULL;

  /**
   *  The node at which the TreeWalker is currently positioned.
   */
  private transient int m_currentNode = DTM.NULL;

  /** True if an itteration has not begun.  */
  transient boolean m_isFresh;

  /** The next walker in the location step chain.
   *  @serial  */
  protected AxesWalker m_nextWalker;

  /** The previous walker in the location step chain, or null.
   *  @serial   */
  AxesWalker m_prevWalker;

  /** The traversal axis from where the nodes will be filtered. */
  protected int m_axis = -1;

  /** The DTM inner traversal class, that corresponds to the super axis. */
  protected DTMAxisTraverser m_traverser;
}
