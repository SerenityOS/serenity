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

package com.sun.org.apache.xml.internal.dtm;

/**
 * A class that implements traverses DTMAxisTraverser interface can traverse
 * a set of nodes, usually as defined by an XPath axis.  It is different from
 * an iterator, because it does not need to hold state, and, in fact, must not
 * hold any iteration-based state.  It is meant to be implemented as an inner
 * class of a DTM, and returned by the getAxisTraverser(final int axis)
 * function.
 *
 * <p>A DTMAxisTraverser can probably not traverse a reverse axis in
 * document order.</p>
 *
 * <p>Typical usage:</p>
 * <pre><code>
 * for(int nodeHandle=myTraverser.first(myContext);
 *     nodeHandle!=DTM.NULL;
 *     nodeHandle=myTraverser.next(myContext,nodeHandle))
 * { ... processing for node indicated by nodeHandle goes here ... }
 * </code></pre>
 *
 * @author Scott Boag
 */
public abstract class DTMAxisTraverser
{

  /**
   * By the nature of the stateless traversal, the context node can not be
   * returned or the iteration will go into an infinate loop.  So to traverse
   * an axis, the first function must be used to get the first node.
   *
   * <p>This method needs to be overloaded only by those axis that process
   * the self node. <\p>
   *
   * @param context The context node of this traversal. This is the point
   * that the traversal starts from.
   * @return the first node in the traversal.
   */
  public int first(int context)
  {
    return next(context, context);
  }

  /**
   * By the nature of the stateless traversal, the context node can not be
   * returned or the iteration will go into an infinate loop.  So to traverse
   * an axis, the first function must be used to get the first node.
   *
   * <p>This method needs to be overloaded only by those axis that process
   * the self node. <\p>
   *
   * @param context The context node of this traversal. This is the point
   * of origin for the traversal -- its "root node" or starting point.
   * @param extendedTypeID The extended type ID that must match.
   *
   * @return the first node in the traversal.
   */
  public int first(int context, int extendedTypeID)
  {
    return next(context, context, extendedTypeID);
  }

  /**
   * Traverse to the next node after the current node.
   *
   * @param context The context node of this traversal. This is the point
   * of origin for the traversal -- its "root node" or starting point.
   * @param current The current node of the traversal. This is the last known
   * location in the traversal, typically the node-handle returned by the
   * previous traversal step. For the first traversal step, context
   * should be set equal to current. Note that in order to test whether
   * context is in the set, you must use the first() method instead.
   *
   * @return the next node in the iteration, or DTM.NULL.
   * @see #first(int)
   */
  public abstract int next(int context, int current);

  /**
   * Traverse to the next node after the current node that is matched
   * by the extended type ID.
   *
   * @param context The context node of this traversal. This is the point
   * of origin for the traversal -- its "root node" or starting point.
   * @param current The current node of the traversal. This is the last known
   * location in the traversal, typically the node-handle returned by the
   * previous traversal step. For the first traversal step, context
   * should be set equal to current. Note that in order to test whether
   * context is in the set, you must use the first() method instead.
   * @param extendedTypeID The extended type ID that must match.
   *
   * @return the next node in the iteration, or DTM.NULL.
   * @see #first(int,int)
   */
  public abstract int next(int context, int current, int extendedTypeID);
}
