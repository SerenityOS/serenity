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
import com.sun.org.apache.xpath.internal.XPathContext;

/**
 * Walker for a reverse axes.
 * @see <a href="http://www.w3.org/TR/xpath#predicates">XPath 2.4 Predicates</a>
 */
public class ReverseAxesWalker extends AxesWalker
{
    static final long serialVersionUID = 2847007647832768941L;

  /**
   * Construct an AxesWalker using a LocPathIterator.
   *
   * @param locPathIterator The location path iterator that 'owns' this walker.
   */
  ReverseAxesWalker(LocPathIterator locPathIterator, int axis)
  {
    super(locPathIterator, axis);
  }

  /**
   * Set the root node of the TreeWalker.
   * (Not part of the DOM2 TreeWalker interface).
   *
   * @param root The context node of this step.
   */
  public void setRoot(int root)
  {
    super.setRoot(root);
    m_iterator = getDTM(root).getAxisIterator(m_axis);
    m_iterator.setStartNode(root);
  }

  /**
   * Detaches the walker from the set which it iterated over, releasing
   * any computational resources and placing the iterator in the INVALID
   * state.
   */
  public void detach()
  {
    m_iterator = null;
    super.detach();
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

    int next = m_iterator.next();

    if (m_isFresh)
      m_isFresh = false;

    if (DTM.NULL == next)
      this.m_foundLast = true;

    return next;
  }


  /**
   * Tells if this is a reverse axes.  Overrides AxesWalker#isReverseAxes.
   *
   * @return true for this class.
   */
  public boolean isReverseAxes()
  {
    return true;
  }

//  /**
//   *  Set the root node of the TreeWalker.
//   *
//   * @param root The context node of this step.
//   */
//  public void setRoot(int root)
//  {
//    super.setRoot(root);
//  }

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
    // A negative predicate index seems to occur with
    // (preceding-sibling::*|following-sibling::*)/ancestor::*[position()]/*[position()]
    // -sb
    if(predicateIndex < 0)
      return -1;

    int count = m_proximityPositions[predicateIndex];

    if (count <= 0)
    {
      AxesWalker savedWalker = wi().getLastUsedWalker();

      try
      {
        ReverseAxesWalker clone = (ReverseAxesWalker) this.clone();

        clone.setRoot(this.getRoot());

        clone.setPredicateCount(predicateIndex);

        clone.setPrevWalker(null);
        clone.setNextWalker(null);
        wi().setLastUsedWalker(clone);

        // Count 'em all
        count++;
        int next;

        while (DTM.NULL != (next = clone.nextNode()))
        {
          count++;
        }

        m_proximityPositions[predicateIndex] = count;
      }
      catch (CloneNotSupportedException cnse)
      {

        // can't happen
      }
      finally
      {
        wi().setLastUsedWalker(savedWalker);
      }
    }

    return count;
  }

  /**
   * Count backwards one proximity position.
   *
   * @param i The predicate index.
   */
  protected void countProximityPosition(int i)
  {
    if (i < m_proximityPositions.length)
      m_proximityPositions[i]--;
  }

  /**
   * Get the number of nodes in this node list.  The function is probably ill
   * named?
   *
   *
   * @param xctxt The XPath runtime context.
   *
   * @return the number of nodes in this node list.
   */
  public int getLastPos(XPathContext xctxt)
  {

    int count = 0;
    AxesWalker savedWalker = wi().getLastUsedWalker();

    try
    {
      ReverseAxesWalker clone = (ReverseAxesWalker) this.clone();

      clone.setRoot(this.getRoot());

      clone.setPredicateCount(this.getPredicateCount() - 1);

      clone.setPrevWalker(null);
      clone.setNextWalker(null);
      wi().setLastUsedWalker(clone);

      // Count 'em all
      // count = 1;
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
      wi().setLastUsedWalker(savedWalker);
    }

    return count;
  }

  /**
   * Returns true if all the nodes in the iteration well be returned in document
   * order.
   * Warning: This can only be called after setRoot has been called!
   *
   * @return false.
   */
  public boolean isDocOrdered()
  {
    return false;  // I think.
  }

  /** The DTM inner traversal class, that corresponds to the super axis. */
  protected DTMAxisIterator m_iterator;
}
