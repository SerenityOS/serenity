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
 * Specifies values related to XPath Axes.
 * <p>The ancestor, descendant, following, preceding and self axes partition a
 * document (ignoring attribute and namespace nodes): they do not overlap
 * and together they contain all the nodes in the document.</p>
 *
 */
public final class Axis
{

  /**
   * The ancestor axis contains the ancestors of the context node;
   *  the ancestors of the context node consist of the parent of context
   *  node and the parent's parent and so on; thus, the ancestor axis will
   *  always include the root node, unless the context node is the root node.
   */
  public static final int ANCESTOR = 0;

  /**
   * the ancestor-or-self axis contains the context node and the ancestors of
   *  the context node; thus, the ancestor axis will always include the
   *  root node.
   */
  public static final int ANCESTORORSELF = 1;

  /**
   * the attribute axis contains the attributes of the context node; the axis
   *  will be empty unless the context node is an element.
   */
  public static final int ATTRIBUTE = 2;

  /** The child axis contains the children of the context node. */
  public static final int CHILD = 3;

  /**
   * The descendant axis contains the descendants of the context node;
   *  a descendant is a child or a child of a child and so on; thus the
   *  descendant axis never contains attribute or namespace nodes.
   */
  public static final int DESCENDANT = 4;

  /**
   * The descendant-or-self axis contains the context node and the
   *  descendants of the context node.
   */
  public static final int DESCENDANTORSELF = 5;

  /**
   * the following axis contains all nodes in the same document as the
   *  context node that are after the context node in document order, excluding
   *  any descendants and excluding attribute nodes and namespace nodes.
   */
  public static final int FOLLOWING = 6;

  /**
   * The following-sibling axis contains all the following siblings of the
   *  context node; if the context node is an attribute node or namespace node,
   *  the following-sibling axis is empty.
   */
  public static final int FOLLOWINGSIBLING = 7;

  /**
   * The namespace axis contains the namespace nodes of the context node; the
   *  axis will be empty unless the context node is an element.
   */
  public static final int NAMESPACEDECLS = 8;

  /**
   * The namespace axis contains the namespace nodes of the context node; the
   *  axis will be empty unless the context node is an element.
   */
  public static final int NAMESPACE = 9;

  /**
   * The parent axis contains the parent of the context node,
   *  if there is one.
   */
  public static final int PARENT = 10;

  /**
   * The preceding axis contains all nodes in the same document as the context
   *  node that are before the context node in document order, excluding any
   *  ancestors and excluding attribute nodes and namespace nodes
   */
  public static final int PRECEDING = 11;

  /**
   * The preceding-sibling axis contains all the preceding siblings of the
   *  context node; if the context node is an attribute node or namespace node,
   *  the preceding-sibling axis is empty.
   */
  public static final int PRECEDINGSIBLING = 12;

  /** The self axis contains just the context node itself. */
  public static final int SELF = 13;

  /**
   * A non-xpath axis, traversing the subtree including the subtree
   *  root, descendants, attributes, and namespace node decls.
   */
  public static final int ALLFROMNODE = 14;

  /**
   * A non-xpath axis, traversing the the preceding and the ancestor nodes,
   * needed for inverseing select patterns to match patterns.
   */
  public static final int PRECEDINGANDANCESTOR = 15;

  // ===========================================
  // All axis past this are absolute.

  /**
   * A non-xpath axis, returns all nodes in the tree from and including the
   * root.
   */
  public static final int ALL = 16;

  /**
   * A non-xpath axis, returns all nodes that aren't namespaces or attributes,
   * from and including the root.
   */
  public static final int DESCENDANTSFROMROOT = 17;

  /**
   * A non-xpath axis, returns all nodes that aren't namespaces or attributes,
   * from and including the root.
   */
  public static final int DESCENDANTSORSELFFROMROOT = 18;

  /**
   * A non-xpath axis, returns root only.
   */
  public static final int ROOT = 19;

  /**
   * A non-xpath axis, for functions.
   */
  public static final int FILTEREDLIST = 20;

  /**
   * A table to identify whether an axis is a reverse axis;
   */
  private static final boolean[] isReverse = {
      true,  // ancestor
      true,  // ancestor-or-self
      false, // attribute
      false, // child
      false, // descendant
      false, // descendant-or-self
      false, // following
      false, // following-sibling
      false, // namespace
      false, // namespace-declarations
      false, // parent (one node, has no order)
      true,  // preceding
      true,  // preceding-sibling
      false  // self (one node, has no order)
  };

    /** The names of the axes for diagnostic purposes. */
    private static final String[] names =
    {
      "ancestor",  // 0
      "ancestor-or-self",  // 1
      "attribute",  // 2
      "child",  // 3
      "descendant",  // 4
      "descendant-or-self",  // 5
      "following",  // 6
      "following-sibling",  // 7
      "namespace-decls",  // 8
      "namespace",  // 9
      "parent",  // 10
      "preceding",  // 11
      "preceding-sibling",  // 12
      "self",  // 13
      "all-from-node",  // 14
      "preceding-and-ancestor",  // 15
      "all",  // 16
      "descendants-from-root",  // 17
      "descendants-or-self-from-root",  // 18
      "root",  // 19
      "filtered-list"  // 20
    };

  public static boolean isReverse(int axis){
      return isReverse[axis];
  }

    public static String getNames(int index){
        return names[index];
    }

    public static int getNamesLength(){
        return names.length;
    }

}
