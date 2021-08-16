/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.dtm.DTMFilter;
import com.sun.org.apache.xml.internal.dtm.DTMIterator;
import com.sun.org.apache.xpath.internal.Expression;
import com.sun.org.apache.xpath.internal.XPathContext;
import com.sun.org.apache.xpath.internal.compiler.Compiler;
import com.sun.org.apache.xpath.internal.compiler.OpMap;

/**
 * This class implements a general iterator for
 * those LocationSteps with only one step, and perhaps a predicate.
 * @see com.sun.org.apache.xpath.internal.axes#LocPathIterator
 * @xsl.usage advanced
 */
public class OneStepIterator extends ChildTestIterator
{
    static final long serialVersionUID = 4623710779664998283L;
  /** The traversal axis from where the nodes will be filtered. */
  protected int m_axis = -1;

  /** The DTM inner traversal class, that corresponds to the super axis. */
  protected DTMAxisIterator m_iterator;

  /**
   * Create a OneStepIterator object.
   *
   * @param compiler A reference to the Compiler that contains the op map.
   * @param opPos The position within the op map, which contains the
   * location path expression for this itterator.
   *
   * @throws javax.xml.transform.TransformerException
   */
  OneStepIterator(Compiler compiler, int opPos, int analysis)
          throws javax.xml.transform.TransformerException
  {
    super(compiler, opPos, analysis);
    int firstStepPos = OpMap.getFirstChildPos(opPos);

    m_axis = WalkerFactory.getAxisFromStep(compiler, firstStepPos);

  }


  /**
   * Create a OneStepIterator object.
   *
   * @param iterator The DTM iterator which this iterator will use.
   * @param axis One of Axis.Child, etc., or -1 if the axis is unknown.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public OneStepIterator(DTMAxisIterator iterator, int axis)
          throws javax.xml.transform.TransformerException
  {
    super(null);

    m_iterator = iterator;
    m_axis = axis;
    int whatToShow = DTMFilter.SHOW_ALL;
    initNodeTest(whatToShow);
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
    if(m_axis > -1)
      m_iterator = m_cdtm.getAxisIterator(m_axis);
    m_iterator.setStartNode(m_context);
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
      if(m_axis > -1)
        m_iterator = null;

      // Always call the superclass detach last!
      super.detach();
    }
  }

  /**
   * Get the next node via getFirstAttribute && getNextAttribute.
   */
  protected int getNextNode()
  {
    return m_lastFetched = m_iterator.next();
  }

  /**
   * Get a cloned iterator.
   *
   * @return A new iterator that can be used without mutating this one.
   *
   * @throws CloneNotSupportedException
   */
  public Object clone() throws CloneNotSupportedException
  {
    // Do not access the location path itterator during this operation!

    OneStepIterator clone = (OneStepIterator) super.clone();

    if(m_iterator != null)
    {
      clone.m_iterator = m_iterator.cloneIterator();
    }
    return clone;
  }

  /**
   *  Get a cloned Iterator that is reset to the beginning
   *  of the query.
   *
   *  @return A cloned NodeIterator set of the start of the query.
   *
   *  @throws CloneNotSupportedException
   */
  public DTMIterator cloneWithReset() throws CloneNotSupportedException
  {

    OneStepIterator clone = (OneStepIterator) super.cloneWithReset();
    clone.m_iterator = m_iterator;

    return clone;
  }



  /**
   * Tells if this is a reverse axes.  Overrides AxesWalker#isReverseAxes.
   *
   * @return true for this class.
   */
  public boolean isReverseAxes()
  {
    return m_iterator.isReverse();
  }

  /**
   * Get the current sub-context position.  In order to do the
   * reverse axes count, for the moment this re-searches the axes
   * up to the predicate.  An optimization on this is to cache
   * the nodes searched, but, for the moment, this case is probably
   * rare enough that the added complexity isn't worth it.
   *
   * @param predicateIndex The predicate index of the proximity position.
   *
   * @return The pridicate index, or -1.
   */
  protected int getProximityPosition(int predicateIndex)
  {
    if(!isReverseAxes())
      return super.getProximityPosition(predicateIndex);

    // A negative predicate index seems to occur with
    // (preceding-sibling::*|following-sibling::*)/ancestor::*[position()]/*[position()]
    // -sb
    if(predicateIndex < 0)
      return -1;

    if (m_proximityPositions[predicateIndex] <= 0)
    {
      XPathContext xctxt = getXPathContext();
      try
      {
        OneStepIterator clone = (OneStepIterator) this.clone();

        int root = getRoot();
        xctxt.pushCurrentNode(root);
        clone.setRoot(root, xctxt);

        // clone.setPredicateCount(predicateIndex);
        clone.m_predCount = predicateIndex;

        // Count 'em all
        int count = 1;
        int next;

        while (DTM.NULL != (next = clone.nextNode()))
        {
          count++;
        }

        m_proximityPositions[predicateIndex] += count;
      }
      catch (CloneNotSupportedException cnse)
      {

        // can't happen
      }
      finally
      {
        xctxt.popCurrentNode();
      }
    }

    return m_proximityPositions[predicateIndex];
  }

  /**
   *  The number of nodes in the list. The range of valid child node indices
   * is 0 to <code>length-1</code> inclusive.
   *
   * @return The number of nodes in the list, always greater or equal to zero.
   */
  public int getLength()
  {
    if(!isReverseAxes())
      return super.getLength();

    // Tell if this is being called from within a predicate.
    boolean isPredicateTest = (this == m_execContext.getSubContextList());

    // And get how many total predicates are part of this step.
    int predCount = getPredicateCount();

    // If we have already calculated the length, and the current predicate
    // is the first predicate, then return the length.  We don't cache
    // the anything but the length of the list to the first predicate.
    if (-1 != m_length && isPredicateTest && m_predicateIndex < 1)
       return m_length;

    int count = 0;

    XPathContext xctxt = getXPathContext();
    try
    {
      OneStepIterator clone = (OneStepIterator) this.cloneWithReset();

      int root = getRoot();
      xctxt.pushCurrentNode(root);
      clone.setRoot(root, xctxt);

      clone.m_predCount = m_predicateIndex;

      int next;

      while (DTM.NULL != (next = clone.nextNode()))
      {
        count++;
      }
    }
    catch (CloneNotSupportedException cnse)
    {
       // can't happen
    }
    finally
    {
      xctxt.popCurrentNode();
    }
    if (isPredicateTest && m_predicateIndex < 1)
      m_length = count;

    return count;
  }

  /**
   * Count backwards one proximity position.
   *
   * @param i The predicate index.
   */
  protected void countProximityPosition(int i)
  {
    if(!isReverseAxes())
      super.countProximityPosition(i);
    else if (i < m_proximityPositions.length)
      m_proximityPositions[i]--;
  }

  /**
   * Reset the iterator.
   */
  public void reset()
  {

    super.reset();
    if(null != m_iterator)
      m_iterator.reset();
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
   * @see Expression#deepEquals(Expression)
   */
  public boolean deepEquals(Expression expr)
  {
        if(!super.deepEquals(expr))
                return false;

        if(m_axis != ((OneStepIterator)expr).m_axis)
                return false;

        return true;
  }


}
