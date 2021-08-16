/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

package sun.jvm.hotspot.utilities;

/** Derived from the example in Section 15.3 of CLR. */

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class IntervalTree extends RBTree {
  private Comparator<Object> endpointComparator;

  /** This constructor takes only one comparator: one which operates
      upon the endpoints of the Intervals this tree will store. It
      constructs an internal "interval comparator" out of this one. */
  public IntervalTree(Comparator<Object> endpointComparator) {
    super(new IntervalComparator(endpointComparator));
    this.endpointComparator = endpointComparator;
  }

  public void insert(Interval interval, Object data) {
    IntervalNode node = new IntervalNode(interval, endpointComparator, data);
    insertNode(node);
  }

  /** Returns a List&lt;IntervalNode&gt; indicating which nodes'
      intervals were intersected by the given query interval. It is
      guaranteed that these nodes will be returned sorted by
      increasing low endpoint. */
  public List<IntervalNode> findAllNodesIntersecting(Interval interval) {
    List<IntervalNode> retList = new ArrayList<>();
    searchForIntersectingNodesFrom((IntervalNode) getRoot(), interval, retList);
    return retList;
  }

  public void print() {
    printOn(System.out);
  }

  public void printOn(PrintStream tty) {
    printFromNode(getRoot(), tty, 0);
  }

  //----------------------------------------------------------------------
  // Overridden internal functionality

  protected Object getNodeValue(RBNode node) {
    return ((IntervalNode) node).getInterval();
  }

  protected void verify() {
    super.verify();
    verifyFromNode(getRoot());
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void verifyFromNode(RBNode node) {
    if (node == null) {
      return;
    }

    // We already know that the red/black structure has been verified.
    // What we need to find out is whether this node has been updated
    // properly -- i.e., whether its notion of the maximum endpoint is
    // correct.
    IntervalNode intNode = (IntervalNode) node;
    if (!intNode.getMaxEndpoint().equals(intNode.computeMaxEndpoint())) {
      if (DEBUGGING && VERBOSE) {
        print();
      }
      throw new RuntimeException("Node's max endpoint was not updated properly");
    }
    if (!intNode.getMinEndpoint().equals(intNode.computeMinEndpoint())) {
      if (DEBUGGING && VERBOSE) {
        print();
      }
      throw new RuntimeException("Node's min endpoint was not updated properly");
    }

    verifyFromNode(node.getLeft());
    verifyFromNode(node.getRight());
  }

  static class IntervalComparator implements Comparator<Object> {
    private Comparator<Object> endpointComparator;

    public IntervalComparator(Comparator<Object> endpointComparator) {
      this.endpointComparator = endpointComparator;
    }

    public int compare(Object o1, Object o2) {
      Interval i1 = (Interval) o1;
      Interval i2 = (Interval) o2;
      return endpointComparator.compare(i1.getLowEndpoint(), i2.getLowEndpoint());
    }
  }

  private void searchForIntersectingNodesFrom(IntervalNode node,
                                              Interval interval,
                                              List<IntervalNode> resultList) {
    if (node == null) {
      return;
    }

    // Inorder traversal (very important to guarantee sorted order)

    // Check to see whether we have to traverse the left subtree
    IntervalNode left = (IntervalNode) node.getLeft();
    if ((left != null) &&
        (endpointComparator.compare(left.getMaxEndpoint(),
                                    interval.getLowEndpoint()) > 0)) {
      searchForIntersectingNodesFrom(left, interval, resultList);
    }

    // Check for intersection with current node
    if (node.getInterval().overlaps(interval, endpointComparator)) {
      resultList.add(node);
    }

    // Check to see whether we have to traverse the left subtree
    IntervalNode right = (IntervalNode) node.getRight();
    if ((right != null) &&
        (endpointComparator.compare(interval.getHighEndpoint(),
                                    right.getMinEndpoint()) > 0)) {
      searchForIntersectingNodesFrom(right, interval, resultList);
    }
  }

  /** Debugging */
  private void printFromNode(RBNode node, PrintStream tty, int indentDepth) {
    for (int i = 0; i < indentDepth; i++) {
      tty.print(" ");
    }

    tty.print("-");
    if (node == null) {
      tty.println();
      return;
    }

    tty.println(" " + node +
                " (min " + ((IntervalNode) node).getMinEndpoint() +
                ", max " + ((IntervalNode) node).getMaxEndpoint() + ")" +
                ((node.getColor() == RBColor.RED) ? " (red)" : " (black)"));
    if (node.getLeft()  != null) printFromNode(node.getLeft(),  tty, indentDepth + 2);
    if (node.getRight() != null) printFromNode(node.getRight(), tty, indentDepth + 2);
  }
}
