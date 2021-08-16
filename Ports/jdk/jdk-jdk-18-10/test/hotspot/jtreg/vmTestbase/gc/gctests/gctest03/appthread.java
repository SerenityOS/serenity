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

import nsk.share.test.*;
import nsk.share.gc.*;

//import Tree;
//import LocalRandom;

// remove the nodes whose key is the multiple of key from the tree
class Redthread extends Thread {
  Tree optree;
  int key;

  Redthread(Tree tr, int k)
  {
    optree = tr;
    key = k;
  }

  public void run() {
    int maxsz = (1024 * 64); // 64k
    int i = 1;
    int sz;

    sz = 1;

    while ( optree.isempty() == false)
      {
        DataNode d;
//      System.out.println(getName() + i);
//      sz = (key * i) % maxsz;
            try
            {
            d = new DataNode(sz);
        }
        catch (DataNodeException e)
        {
//System.out.println(getName() + " exiting");
            return;
        }
       if (optree.remove(d))
//        System.out.println(getName() + " removes " + sz);
       i++;
       try
       {
          sleep(3);
       }
       catch(InterruptedException e) {}

//      optree.sort1();
        sz++;
     }
  }
}

// add the nodes whose key is the multiple of key from the tree
class Bluethread extends Thread {
  Tree optree;
  int key;
  private int loopcount = 0;

  Bluethread(Tree tr, int k)
  {
    optree = tr;
    key = k;
  }

  public void setloop(int n)
  {
      loopcount = n;
  }

  public void run()
  {
    int i;
    int sz;
    int maxsz = (1024 * 64); // 64k

    i = 1; sz = 0;
    while ( (loopcount== 0) ? true : (i < loopcount) )
    {
            sz = (key * i) % maxsz;
       DataNode d;

       try
       {
            d= new DataNode(sz);
       }
       catch (DataNodeException e)
       {
//System.out.println(getName() + " exiting");
            return;
       }

       TreeNode t = new TreeNode(d);
 //             System.out.println(getName() + i);
       if ( optree.search(d) == null )
         {
           optree.insert(t);
//           System.out.println(getName() + " insert " + sz);
         }
       //optree.sort1();
       i++;
       try
       {
          sleep(5);
       }
       catch(InterruptedException e) {}
     }
  }
}

class Yellowthread extends Thread {
  Tree optree;
  int key;    // data to be moved from the tree

  Yellowthread(Tree tr, int k)
  {
    optree = tr;
    key = k;
  }

  // remove the nodes whose key is the multiple of key from the tree
  public void run()
  {
    int i = 1;
    while ( true )
      {
        DataNode d;
        try
        {
            d = new DataNode(key*i);
        }
        catch (DataNodeException e)
        {
//System.out.println(getName() + " exiting");
            return;
        }
       TreeNode t = optree.search(d);
/*       if ( t != null ) System.out.println(getName() + ": search = " +
 *                                         (t.getData()).getkey());
 */
       i++;
       if ( LocalRandom.random() < 0.668 )
       {
          try
          {
             sleep(5);
          }
          catch(InterruptedException e) {}
       }
     }
  }
}

public class appthread {
}
