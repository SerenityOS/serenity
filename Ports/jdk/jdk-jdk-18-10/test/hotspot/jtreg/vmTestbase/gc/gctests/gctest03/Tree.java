/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
 */

package gc.gctests.gctest03;

class DataNodeException extends Exception
{
}


class DataNode  {
  int key;
  int buf[];
  static int dataNodeCount = 0;
  static int dataNodeLimit;

  static synchronized void incDataNodeCount()
  {
    dataNodeCount++;
  }

  static synchronized int getDataNodeCount()
  {
    return dataNodeCount;
  }

  static synchronized void setDataNodeLimit(int newLimit)
  {
    dataNodeLimit = newLimit;
  }

  static synchronized int getDataNodeLimit()
  {
    return dataNodeLimit;
  }

  static synchronized void clearDataNodeCount()
  {
        dataNodeCount = 0;
  }
  DataNode(int key) throws DataNodeException
  {

    incDataNodeCount();
    if (getDataNodeCount() > getDataNodeLimit())
    {
        throw (new DataNodeException());
    }

    this.key = key;
    try {
      buf = new int[key];
    }
    catch ( OutOfMemoryError e )
      {
        System.out.println(Thread.currentThread().getName() + " : outofmemory");
      }
  }

  public void print()
  {
    System.out.println(key);
  }

  public boolean equals(DataNode d)
  {
    int k = d.getkey();

    return ( key == k);
  }

  public boolean large(DataNode d)
  {
    int k = d.getkey();

    return ( key > k);
  }

 public boolean less(DataNode d)

  {
    int k = d.getkey();

    return ( key < k);
  }

  public int getkey()
  { return key; }
}


class TreeNode {
  DataNode  data;
  TreeNode  parent;
  TreeNode  left;
  TreeNode  right;

  TreeNode(DataNode key)
  {
    this.data = key;
    parent = null;
    left = null;
    right = null;
  }

  public void print()
  {
    data.print();
  }

  public TreeNode getleft()
  {
    return left;
  }

  public TreeNode getright()
  {
    return right;
  }

  public TreeNode getparent()
  {
    return parent;
  }

  public synchronized void setleft(TreeNode left)
  {
    this.left = left;
  }

  public synchronized void setright(TreeNode right)
  {
    this.right = right;
  }

  public synchronized void setparent(TreeNode parent)
  {
    this.parent = parent;
  }

  public DataNode getData()
  {
    return data;
  }

  // print it out in order of a large one first
  public void sprint()
  {
    //print itself
    if ( left != null )  left.sprint();
    System.out.println(data.getkey());
    if ( right != null )  right.sprint();
  }

   // print it out in order of a small one first
  public void lprint()
  {
    if (right != null ) right.lprint();
    System.out.println(data.getkey());
    if ( left != null ) left.lprint();
  }

  public synchronized TreeNode duplicate()
  {
    TreeNode tp = new TreeNode(data);

    if ( left != null )
      {
        tp.left = left.duplicate();
      }
    if ( right != null )
      {
        tp.right = right.duplicate();
      }
    return tp;
  }

  public TreeNode search(DataNode d)
  {
    TreeNode tp = this;
    DataNode k;

    while ( tp != null )
      {
        k = tp.getData();
        if ( k.equals(d) )
          {
            return tp;
          }
        else
          if ( d.large(k) )
            tp = tp.getright();
        else
          tp = tp.getleft();
      }
    return null;
  }

  public synchronized void insert(TreeNode t)
  {
    DataNode d = t.getData();

    TreeNode tp = this;
    TreeNode tp1 = tp;
    DataNode d0;

    while ( true )
      {
        d0 = tp.getData();

        if ( d.large(d0) )
          {
            tp1 = tp;
            tp = tp.getright();
            if ( tp == null )
              {
                tp1.setright(t);
                t.setparent(tp1);
                break;
              }
          }
        else
          {
            tp1 = tp;
            tp = tp.getleft();
            if (tp == null )
              {
                tp1.setleft(t);
                t.setparent(tp1);
                break;
              }
          }
      }

  }


}


class Tree {
  TreeNode root = null;

  Tree()
  {
    root = null;
  }

  Tree(TreeNode root)
  {
    this.root = root;
  }

  public synchronized void insert(TreeNode t)
  {
    if ( root == null )
      {
        root = t;
        return;
      }

    root.insert(t);
  }


  public void sort1()
  {
    root.sprint();
  }

  public void sort2()
  {
    root.lprint();
  }

  public TreeNode search(DataNode d)
  {
    if ( root == null ) return null;
    else return root.search(d);
  }

  public synchronized boolean remove(DataNode d)
  {
    if ( root == null ) return false;

    TreeNode t = root.search(d);

        // data is not in a heap
    if ( t == null ) return false;

/*
    if ( d.equals(t.getData()) == false )
      {
        System.out.println("failed");
        return false;
      }
 */

    TreeNode p = t.getparent();
    TreeNode l = t.getleft();
    TreeNode r = t.getright();

    // the removed node is a root
    if ( p == null )
      {
        if ( l == null && r != null )
          {
            r.setparent(null);
            root = r;
            return true;
          }
        if ( l != null && r == null )
          {
            l.setparent(null);
            root = l;
            return true;
          }
        if ( l == null && r == null )
          {
            root = null;
            return true;
          }

        if ( l != null && r != null )
          {
            r.setparent(null);
            r.insert(l);
            root = r;
            return true;
          }
      }

    // a leaf
    if ( r == null && l == null )
      {
        if ( p.getright() == t )
          {
            /* right child */
            p.setright(null);
          }
        else
          p.setleft(null);
        return true;
      }

    // a node without left child
    if ( r != null &&  l == null )
      {
        r.setparent(p);
        if ( t == p.getright() ) p.setright(r);
        if ( t == p.getleft() ) p.setleft(r);
        return true;
      }

    if ( r == null && l != null )
      {
        l.setparent(p);
        if ( t == p.getright() ) p.setright(l);
        if ( t == p.getleft() ) p.setleft(l);
        return true;
      }

    // a node with two children
    r.insert(l);
    r.setparent(p);
    if ( t == p.getright() ) p.setright(r);
    if ( t == p.getleft() ) p.setleft(r);
    return true;
   }

  public synchronized Tree copy()
  {
    if ( root == null ) return null;

    return(new Tree(root.duplicate()));
  }

  public synchronized boolean isempty()
  {
    return ( root == null );
  }


}
