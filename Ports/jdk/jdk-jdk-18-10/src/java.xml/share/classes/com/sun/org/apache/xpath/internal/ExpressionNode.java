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

package com.sun.org.apache.xpath.internal;

import javax.xml.transform.SourceLocator;

/**
 * A class that implements this interface can construct expressions,
 * give information about child and parent expressions,
 * and give the originating source information.  A class that implements
 * this interface does not lay any claim to being directly executable.
 *
 * <p>Note: This interface should not be considered stable.  Only exprSetParent
 * and exprGetParent can be counted on to work reliably.  Work in progress.</p>
 */
public interface ExpressionNode extends SourceLocator
{
  /** This pair of methods are used to inform the node of its
    parent. */
  public void exprSetParent(ExpressionNode n);
  public ExpressionNode exprGetParent();

  /** This method tells the node to add its argument to the node's
    list of children.  */
  public void exprAddChild(ExpressionNode n, int i);

  /** This method returns a child node.  The children are numbered
     from zero, left to right. */
  public ExpressionNode exprGetChild(int i);

  /** Return the number of children the node has. */
  public int exprGetNumChildren();
}
