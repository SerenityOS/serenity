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

/** <P> This class implements a Red-Black tree as described in Cormen,
    Leiserson, Rivest, <I>Introduction to Algorithms</I>, MIT Press:
    Cambridge, MA, 1990. </P>

    <P> A property of this implementation is that it is designed to
    allow straightforward augmentation of the data structure. A valid
    augmentation is one in which a node contains auxiliary information
    that can be computed by examining only this node and its left and
    right children (see CLR, section 15.2). </P>

    <P> An RBTree is a structure of RBNodes, each of which contains a
    user data element. When the user inserts a piece of data into the
    tree, a new RBNode is constructed around it. </P>

    <P> An RBTree takes a Comparator as argument to its constructor
    which is used internally to order the nodes in the tree. The
    comparator's arguments are obtained by calling the routine
    "getNodeData" on two nodes; the default implementaion returns the
    node data. This Comparator is also used to perform the generic
    "find" operation, which returns the RBNode containing user data
    precisely equalling the query data. Different types of user data
    will typically require different types of traversals as well as
    additional comparison operations; these are left to the RBTree
    subclass. </P>

*/

import java.io.PrintStream;
import java.util.Comparator;
import java.util.Random;

public class RBTree {
  private RBNode root;
  private Comparator<Object> comparator;
  protected static final boolean DEBUGGING = true;
  protected static final boolean VERBOSE   = true;
  protected static final boolean REALLY_VERBOSE = false;

  public RBTree(Comparator<Object> comparator) {
    this.comparator = comparator;
  }

  public RBNode getRoot() {
    return root;
  }

  public void insertNode(RBNode x) {
    treeInsert(x);

    x.setColor(RBColor.RED);
    boolean shouldPropagate = x.update();

    if (DEBUGGING && REALLY_VERBOSE) {
      System.err.println("RBTree.insertNode");
    }

    RBNode propagateStart = x;

    // Loop invariant: x has been updated.
    while ((x != root) && (x.getParent().getColor() == RBColor.RED)) {
      if (x.getParent() == x.getParent().getParent().getLeft()) {
        RBNode y = x.getParent().getParent().getRight();
        if ((y != null) && (y.getColor() == RBColor.RED)) {
          // Case 1
          if (DEBUGGING && REALLY_VERBOSE) {
            System.err.println("  Case 1/1");
          }
          x.getParent().setColor(RBColor.BLACK);
          y.setColor(RBColor.BLACK);
          x.getParent().getParent().setColor(RBColor.RED);
          x.getParent().update();
          x = x.getParent().getParent();
          shouldPropagate = x.update();
          propagateStart = x;
        } else {
          if (x == x.getParent().getRight()) {
            // Case 2
            if (DEBUGGING && REALLY_VERBOSE) {
              System.err.println("  Case 1/2");
            }
            x = x.getParent();
            leftRotate(x);
          }
          // Case 3
          if (DEBUGGING && REALLY_VERBOSE) {
            System.err.println("  Case 1/3");
          }
          x.getParent().setColor(RBColor.BLACK);
          x.getParent().getParent().setColor(RBColor.RED);
          shouldPropagate = rightRotate(x.getParent().getParent());
          propagateStart = x.getParent();
        }
      } else {
        // Same as then clause with "right" and "left" exchanged
        RBNode y = x.getParent().getParent().getLeft();
        if ((y != null) && (y.getColor() == RBColor.RED)) {
          // Case 1
          if (DEBUGGING && REALLY_VERBOSE) {
            System.err.println("  Case 2/1");
          }
          x.getParent().setColor(RBColor.BLACK);
          y.setColor(RBColor.BLACK);
          x.getParent().getParent().setColor(RBColor.RED);
          x.getParent().update();
          x = x.getParent().getParent();
          shouldPropagate = x.update();
          propagateStart = x;
        } else {
          if (x == x.getParent().getLeft()) {
            // Case 2
            if (DEBUGGING && REALLY_VERBOSE) {
              System.err.println("  Case 2/2");
            }
            x = x.getParent();
            rightRotate(x);
          }
          // Case 3
          if (DEBUGGING && REALLY_VERBOSE) {
            System.err.println("  Case 2/3");
          }
          x.getParent().setColor(RBColor.BLACK);
          x.getParent().getParent().setColor(RBColor.RED);
          shouldPropagate = leftRotate(x.getParent().getParent());
          propagateStart = x.getParent();
        }
      }
    }

    while (shouldPropagate && (propagateStart != root)) {
      if (DEBUGGING && REALLY_VERBOSE) {
        System.err.println("  Propagating");
      }
      propagateStart = propagateStart.getParent();
      shouldPropagate = propagateStart.update();
    }

    root.setColor(RBColor.BLACK);

    if (DEBUGGING) {
      verify();
    }
  }

  /** FIXME: this does not work properly yet for augmented red-black
      trees since it doesn't update nodes. Need to figure out exactly
      from which points we need to propagate updates upwards. */
  public void deleteNode(RBNode z) {
    // This routine splices out a node. Note that we may not actually
    // delete the RBNode z from the tree, but may instead remove
    // another node from the tree, copying its contents into z.

    // y is the node to be unlinked from the tree
    RBNode y;
    if ((z.getLeft() == null) || (z.getRight() == null)) {
      y = z;
    } else {
      y = treeSuccessor(z);
    }
    // y is guaranteed to be non-null at this point
    RBNode x;
    if (y.getLeft() != null) {
      x = y.getLeft();
    } else {
      x = y.getRight();
    }
    // x is the potential child of y to replace it in the tree.
    // x may be null at this point
    RBNode xParent;
    if (x != null) {
      x.setParent(y.getParent());
      xParent = x.getParent();
    } else {
      xParent = y.getParent();
    }
    if (y.getParent() == null) {
      root = x;
    } else {
      if (y == y.getParent().getLeft()) {
        y.getParent().setLeft(x);
      } else {
        y.getParent().setRight(x);
      }
    }
    if (y != z) {
      z.copyFrom(y);
    }
    if (y.getColor() == RBColor.BLACK) {
      deleteFixup(x, xParent);
    }

    if (DEBUGGING) {
      verify();
    }
  }

  public void print() {
    printOn(System.out);
  }

  public void printOn(PrintStream tty) {
    printFromNode(root, tty, 0);
  }

  //----------------------------------------------------------------------
  // Functionality overridable by subclass
  //

  protected Object getNodeValue(RBNode node) {
    return node.getData();
  }

  /** Verify invariants are preserved */
  protected void verify() {
    verifyFromNode(root);
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  //
  // Vanilla binary search tree operations
  //

  private void treeInsert(RBNode z) {
    RBNode y = null;
    RBNode x = root;

    while (x != null) {
      y = x;
      if (comparator.compare(getNodeValue(z), getNodeValue(x)) < 0) {
        x = x.getLeft();
      } else {
        x = x.getRight();
      }
    }
    z.setParent(y);
    if (y == null) {
      root = z;
    } else {
      if (comparator.compare(getNodeValue(z), getNodeValue(y)) < 0) {
        y.setLeft(z);
      } else {
        y.setRight(z);
      }
    }
  }

  private RBNode treeSuccessor(RBNode x) {
    if (x.getRight() != null) {
      return treeMinimum(x.getRight());
    }
    RBNode y = x.getParent();
    while ((y != null) && (x == y.getRight())) {
      x = y;
      y = y.getParent();
    }
    return y;
  }

  private RBNode treeMinimum(RBNode x) {
    while (x.getLeft() != null) {
      x = x.getLeft();
    }
    return x;
  }

  //
  // Insertion and deletion helpers
  //

  /** Returns true if updates must continue propagating up the tree */
  private boolean leftRotate(RBNode x) {
    // Set y.
    RBNode y = x.getRight();
    // Turn y's left subtree into x's right subtree.
    x.setRight(y.getLeft());
    if (y.getLeft() != null) {
      y.getLeft().setParent(x);
    }
    // Link x's parent to y.
    y.setParent(x.getParent());
    if (x.getParent() == null) {
      root = y;
    } else {
      if (x == x.getParent().getLeft()) {
        x.getParent().setLeft(y);
      } else {
        x.getParent().setRight(y);
      }
    }
    // Put x on y's left.
    y.setLeft(x);
    x.setParent(y);
    // Update nodes in appropriate order (lowest to highest)
    boolean res = x.update();
    res = y.update() || res;
    return res;
  }

  /** Returns true if updates must continue propagating up the tree */
  private boolean rightRotate(RBNode y) {
    // Set x.
    RBNode x = y.getLeft();
    // Turn x's right subtree into y's left subtree.
    y.setLeft(x.getRight());
    if (x.getRight() != null) {
      x.getRight().setParent(y);
    }
    // Link y's parent into x.
    x.setParent(y.getParent());
    if (y.getParent() == null) {
      root = x;
    } else {
      if (y == y.getParent().getLeft()) {
        y.getParent().setLeft(x);
      } else {
        y.getParent().setRight(x);
      }
    }
    // Put y on x's right.
    x.setRight(y);
    y.setParent(x);
    // Update nodes in appropriate order (lowest to highest)
    boolean res = y.update();
    res = x.update() || res;
    return res;
  }

  /** Restores red-black property to tree after splicing out node
      during deletion. Note that x may be null, which is why xParent
      must be specified. */
  private void deleteFixup(RBNode x, RBNode xParent) {
    while ((x != root) && ((x == null) || (x.getColor() == RBColor.BLACK))) {
      if (x == xParent.getLeft()) {
        // NOTE: the text points out that w can not be null. The
        // reason is not obvious from simply examining the code; it
        // comes about because of properties of the red-black tree.
        RBNode w = xParent.getRight();
        if (DEBUGGING) {
          if (w == null) {
            throw new RuntimeException("x's sibling should not be null");
          }
        }
        if (w.getColor() == RBColor.RED) {
          // Case 1
          w.setColor(RBColor.BLACK);
          xParent.setColor(RBColor.RED);
          leftRotate(xParent);
          w = xParent.getRight();
        }
        if (((w.getLeft()  == null) || (w.getLeft().getColor()  == RBColor.BLACK)) &&
            ((w.getRight() == null) || (w.getRight().getColor() == RBColor.BLACK))) {
          // Case 2
          w.setColor(RBColor.RED);
          x = xParent;
          xParent = x.getParent();
        } else {
          if ((w.getRight() == null) || (w.getRight().getColor() == RBColor.BLACK)) {
            // Case 3
            w.getLeft().setColor(RBColor.BLACK);
            w.setColor(RBColor.RED);
            rightRotate(w);
            w = xParent.getRight();
          }
          // Case 4
          w.setColor(xParent.getColor());
          xParent.setColor(RBColor.BLACK);
          if (w.getRight() != null) {
            w.getRight().setColor(RBColor.BLACK);
          }
          leftRotate(xParent);
          x = root;
          xParent = x.getParent();
        }
      } else {
        // Same as clause above with "right" and "left"
        // exchanged

        // NOTE: the text points out that w can not be null. The
        // reason is not obvious from simply examining the code; it
        // comes about because of properties of the red-black tree.
        RBNode w = xParent.getLeft();
        if (DEBUGGING) {
          if (w == null) {
            throw new RuntimeException("x's sibling should not be null");
          }
        }
        if (w.getColor() == RBColor.RED) {
          // Case 1
          w.setColor(RBColor.BLACK);
          xParent.setColor(RBColor.RED);
          rightRotate(xParent);
          w = xParent.getLeft();
        }
        if (((w.getRight() == null) || (w.getRight().getColor() == RBColor.BLACK)) &&
            ((w.getLeft()  == null) || (w.getLeft().getColor()  == RBColor.BLACK))) {
          // Case 2
          w.setColor(RBColor.RED);
          x = xParent;
          xParent = x.getParent();
        } else {
          if ((w.getLeft() == null) || (w.getLeft().getColor() == RBColor.BLACK)) {
            // Case 3
            w.getRight().setColor(RBColor.BLACK);
            w.setColor(RBColor.RED);
            leftRotate(w);
            w = xParent.getLeft();
          }
          // Case 4
          w.setColor(xParent.getColor());
          xParent.setColor(RBColor.BLACK);
          if (w.getLeft() != null) {
            w.getLeft().setColor(RBColor.BLACK);
          }
          rightRotate(xParent);
          x = root;
          xParent = x.getParent();
        }
      }
    }
    if (x != null) {
      x.setColor(RBColor.BLACK);
    }
  }

  // Returns the number of black children along all paths to all
  // leaves of the given node
  private int verifyFromNode(RBNode node) {
    // Bottoms out at leaf nodes
    if (node == null) {
      return 1;
    }

    // Each node is either red or black
    if (!((node.getColor() == RBColor.RED) ||
          (node.getColor() == RBColor.BLACK))) {
      if (DEBUGGING) {
        System.err.println("Verify failed:");
        printOn(System.err);
      }
      throw new RuntimeException("Verify failed (1)");
    }

    // Every leaf (null) is black

    if (node.getColor() == RBColor.RED) {
      // Both its children are black
      if (node.getLeft() != null) {
        if (node.getLeft().getColor() != RBColor.BLACK) {
          if (DEBUGGING) {
            System.err.println("Verify failed:");
            printOn(System.err);
          }
          throw new RuntimeException("Verify failed (2)");
        }
      }
      if (node.getRight() != null) {
        if (node.getRight().getColor() != RBColor.BLACK) {
          if (DEBUGGING) {
            System.err.println("Verify failed:");
            printOn(System.err);
          }
          throw new RuntimeException("Verify failed (3)");
        }
      }
    }

    // Every simple path to a leaf contains the same number of black
    // nodes
    int i = verifyFromNode(node.getLeft());
    int j = verifyFromNode(node.getRight());
    if (i != j) {
      if (DEBUGGING) {
        System.err.println("Verify failed:");
        printOn(System.err);
      }
      throw new RuntimeException("Verify failed (4) (left black count = " +
                                 i + ", right black count = " + j + ")");
    }

    return i + ((node.getColor() == RBColor.RED) ? 0 : 1);
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

    tty.println(" " + getNodeValue(node) +
                ((node.getColor() == RBColor.RED) ? " (red)" : " (black)"));
    printFromNode(node.getLeft(), tty, indentDepth + 2);
    printFromNode(node.getRight(), tty, indentDepth + 2);
  }

  //----------------------------------------------------------------------
  // Test harness
  //

  public static void main(String[] args) {
    int treeSize = 10000;
    int maxVal = treeSize;
    System.err.println("Building tree...");
    RBTree tree = new RBTree(new Comparator<>() {
        public int compare(Object o1, Object o2) {
          Integer i1 = (Integer) o1;
          Integer i2 = (Integer) o2;
          if (i1.intValue() < i2.intValue()) {
            return -1;
          } else if (i1.intValue() == i2.intValue()) {
            return 0;
          }
          return 1;
        }
      });
    Random rand = new Random(System.currentTimeMillis());
    for (int i = 0; i < treeSize; i++) {
      Integer val = rand.nextInt(maxVal) + 1;
      try {
        tree.insertNode(new RBNode(val));
        if ((i > 0) && (i % 100 == 0)) {
          System.err.print(i + "...");
          System.err.flush();
        }
      }
      catch (Exception e) {
        e.printStackTrace();
        System.err.println("While inserting value " + val);
        tree.printOn(System.err);
        System.exit(1);
      }
    }
    // Now churn data in tree by deleting and inserting lots of nodes
    System.err.println();
    System.err.println("Churning tree...");
    for (int i = 0; i < treeSize; i++) {
      if (DEBUGGING && VERBOSE) {
        System.err.println("Iteration " + i + ":");
        tree.printOn(System.err);
      }
      // Pick a random value to remove (NOTE that this is not
      // uniformly distributed)
      RBNode xParent = null;
      RBNode x = tree.getRoot();
      int depth = 0;
      while (x != null) {
        // Traverse path down to leaf
        xParent = x;
        if (rand.nextBoolean()) {
          x = x.getLeft();
        } else {
          x = x.getRight();
        }
        ++depth;
      }
      // Pick a random height at which to remove value
      int height = rand.nextInt(depth);
      if (DEBUGGING) {
        if (height >= depth) {
          throw new RuntimeException("bug in java.util.Random");
        }
      }
      // Walk that far back up (FIXME: off by one?)
      while (height > 0) {
        xParent = xParent.getParent();
        --height;
      }
      // Tell the tree to remove this node
      if (DEBUGGING && VERBOSE) {
        System.err.println("(Removing value " + tree.getNodeValue(xParent) + ")");
      }
      tree.deleteNode(xParent);

      // Now create and insert a new value
      Integer newVal = rand.nextInt(maxVal) + 1;
      if (DEBUGGING && VERBOSE) {
        System.err.println("(Inserting value " + newVal + ")");
      }
      tree.insertNode(new RBNode(newVal));
    }
    System.err.println("All tests passed.");
  }
}
