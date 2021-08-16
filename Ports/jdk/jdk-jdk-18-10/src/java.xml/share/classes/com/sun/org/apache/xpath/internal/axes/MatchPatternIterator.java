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
import com.sun.org.apache.xml.internal.dtm.DTMAxisTraverser;
import com.sun.org.apache.xml.internal.dtm.DTMIterator;
import com.sun.org.apache.xpath.internal.XPathContext;
import com.sun.org.apache.xpath.internal.compiler.Compiler;
import com.sun.org.apache.xpath.internal.compiler.OpMap;
import com.sun.org.apache.xpath.internal.objects.XObject;
import com.sun.org.apache.xpath.internal.patterns.NodeTest;
import com.sun.org.apache.xpath.internal.patterns.StepPattern;

/**
 * This class treats a
 * <a href="http://www.w3.org/TR/xpath#location-paths">LocationPath</a> as a
 * filtered iteration over the tree, evaluating each node in a super axis
 * traversal against the LocationPath interpreted as a match pattern.  This
 * class is useful to find nodes in document order that are complex paths
 * whose steps probably criss-cross each other.
 *
 * @LastModified: Oct 2017
 */
public class MatchPatternIterator extends LocPathIterator
{
    static final long serialVersionUID = -5201153767396296474L;

  /** This is the select pattern, translated into a match pattern. */
  protected StepPattern m_pattern;

  /** The traversal axis from where the nodes will be filtered. */
  protected int m_superAxis = -1;

  /** The DTM inner traversal class, that corresponds to the super axis. */
  protected DTMAxisTraverser m_traverser;

  /** DEBUG flag for diagnostic dumps. */
  private static final boolean DEBUG = false;

//  protected int m_nsElemBase = DTM.NULL;

  /**
   * Create a LocPathIterator object, including creation
   * of step walkers from the opcode list, and call back
   * into the Compiler to create predicate expressions.
   *
   * @param compiler The Compiler which is creating
   * this expression.
   * @param opPos The position of this iterator in the
   * opcode list from the compiler.
   * @param analysis Analysis bits that give general information about the
   * LocationPath.
   *
   * @throws javax.xml.transform.TransformerException
   */
  MatchPatternIterator(Compiler compiler, int opPos, int analysis)
          throws javax.xml.transform.TransformerException
  {

    super(compiler, opPos, analysis, false);

    int firstStepPos = OpMap.getFirstChildPos(opPos);

    m_pattern = WalkerFactory.loadSteps(this, compiler, firstStepPos, 0);

    boolean fromRoot = false;
    boolean walkBack = false;
    boolean walkDescendants = false;
    boolean walkAttributes = false;

    if (0 != (analysis & (WalkerFactory.BIT_ROOT |
                          WalkerFactory.BIT_ANY_DESCENDANT_FROM_ROOT)))
      fromRoot = true;

    if (0 != (analysis
              & (WalkerFactory.BIT_ANCESTOR
                 | WalkerFactory.BIT_ANCESTOR_OR_SELF
                 | WalkerFactory.BIT_PRECEDING
                 | WalkerFactory.BIT_PRECEDING_SIBLING
                 | WalkerFactory.BIT_FOLLOWING
                 | WalkerFactory.BIT_FOLLOWING_SIBLING
                 | WalkerFactory.BIT_PARENT | WalkerFactory.BIT_FILTER)))
      walkBack = true;

    if (0 != (analysis
              & (WalkerFactory.BIT_DESCENDANT_OR_SELF
                 | WalkerFactory.BIT_DESCENDANT
                 | WalkerFactory.BIT_CHILD)))
      walkDescendants = true;

    if (0 != (analysis
              & (WalkerFactory.BIT_ATTRIBUTE | WalkerFactory.BIT_NAMESPACE)))
      walkAttributes = true;

    if(false || DEBUG)
    {
      System.out.print("analysis: "+Integer.toBinaryString(analysis));
      System.out.println(", "+WalkerFactory.getAnalysisString(analysis));
    }

    if(fromRoot || walkBack)
    {
      if(walkAttributes)
      {
        m_superAxis = Axis.ALL;
      }
      else
      {
        m_superAxis = Axis.DESCENDANTSFROMROOT;
      }
    }
    else if(walkDescendants)
    {
      if(walkAttributes)
      {
        m_superAxis = Axis.ALLFROMNODE;
      }
      else
      {
        m_superAxis = Axis.DESCENDANTORSELF;
      }
    }
    else
    {
      m_superAxis = Axis.ALL;
    }
    if(false || DEBUG)
    {
      System.out.println("axis: "+Axis.getNames(m_superAxis));
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
    m_traverser = m_cdtm.getAxisTraverser(m_superAxis);
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
      m_traverser = null;

      // Always call the superclass detach last!
      super.detach();
    }
  }

  /**
   * Get the next node via getNextXXX.  Bottlenecked for derived class override.
   * @return The next node on the axis, or DTM.NULL.
   */
  protected int getNextNode()
  {
    m_lastFetched = (DTM.NULL == m_lastFetched)
                     ? m_traverser.first(m_context)
                     : m_traverser.next(m_context, m_lastFetched);
    return m_lastFetched;
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

    int next;

    com.sun.org.apache.xpath.internal.VariableStack vars;
    int savedStart;
    if (-1 != m_stackFrame)
    {
      vars = m_execContext.getVarStack();

      // These three statements need to be combined into one operation.
      savedStart = vars.getStackFrame();

      vars.setStackFrame(m_stackFrame);
    }
    else
    {
      // Yuck.  Just to shut up the compiler!
      vars = null;
      savedStart = 0;
    }

    try
    {
      if(DEBUG)
        System.out.println("m_pattern"+m_pattern.toString());

      do
      {
        next = getNextNode();

        if (DTM.NULL != next)
        {
          if(DTMIterator.FILTER_ACCEPT == acceptNode(next, m_execContext))
            break;
          else
            continue;
        }
        else
          break;
      }
      while (next != DTM.NULL);

      if (DTM.NULL != next)
      {
        if(DEBUG)
        {
          System.out.println("next: "+next);
          System.out.println("name: "+m_cdtm.getNodeName(next));
        }
        incrementCurrentPos();

        return next;
      }
      else
      {
        m_foundLast = true;

        return DTM.NULL;
      }
    }
    finally
    {
      if (-1 != m_stackFrame)
      {
        // These two statements need to be combined into one operation.
        vars.setStackFrame(savedStart);
      }
    }

  }

  /**
   *  Test whether a specified node is visible in the logical view of a
   * TreeWalker or NodeIterator. This function will be called by the
   * implementation of TreeWalker and NodeIterator; it is not intended to
   * be called directly from user code.
   * @param n  The node to check to see if it passes the filter or not.
   * @return  a constant to determine whether the node is accepted,
   *   rejected, or skipped, as defined  above .
   */
  public short acceptNode(int n, XPathContext xctxt)
  {

    try
    {
      xctxt.pushCurrentNode(n);
      xctxt.pushIteratorRoot(m_context);
      if(DEBUG)
      {
        System.out.println("traverser: "+m_traverser);
        System.out.print("node: "+n);
        System.out.println(", "+m_cdtm.getNodeName(n));
        // if(m_cdtm.getNodeName(n).equals("near-east"))
        System.out.println("pattern: "+m_pattern.toString());
        NodeTest.debugWhatToShow(m_pattern.getWhatToShow());
      }

      XObject score = m_pattern.execute(xctxt);

      if(DEBUG)
      {
        // System.out.println("analysis: "+Integer.toBinaryString(m_analysis));
        System.out.println("score: "+score);
        System.out.println("skip: "+(score == NodeTest.SCORE_NONE));
      }

      // System.out.println("\n::acceptNode - score: "+score.num()+"::");
      return (score == NodeTest.SCORE_NONE) ? DTMIterator.FILTER_SKIP
                    : DTMIterator.FILTER_ACCEPT;
    }
    catch (javax.xml.transform.TransformerException se)
    {

      // TODO: Fix this.
      throw new RuntimeException(se.getMessage());
    }
    finally
    {
      xctxt.popCurrentNode();
      xctxt.popIteratorRoot();
    }

  }

}
