/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.gc.shared.*;
import sun.jvm.hotspot.memory.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;

/** Finds all paths from roots to the specified set of objects. NOTE:
    currently only a subset of the roots known to the VM is exposed to
    the SA: objects on the stack, static fields in classes, and JNI
    handles. These should be most of the user-level roots keeping
    objects alive. */

public class LivenessAnalysis {
  // Used for debugging this code
  private static final boolean DEBUG = false;

  private LivenessAnalysis() {}

  public static LivenessPathList computeAllLivenessPaths(Oop target) {
    LivenessPathList list = computeAllLivenessPaths(target, true);
    if ((list == null) || (list.size() == 0)) {
      // Dead object
      return null;
    }
    return list;
  }

  //---------------------------------------------------------------------------
  // Internals only below this point
  //

  // Returns true if a new path was completed, otherwise false
  // indicating there were no more paths to complete.
  //
  // The trimPathsThroughPopularObjects flag alters the behavior of
  // the returned results. If true, then if multiple paths to
  // different roots all go through a particular popular object, those
  // paths will be truncated and only one (arbitrary one) will be be
  // returned. On the other hand, if the target object itself is
  // popular and there are multiple distinct paths to it (indicating
  // that there are multiple objects pointing directly to it) then all
  // of those paths will be reported.
  private static LivenessPathList computeAllLivenessPaths(Oop target, boolean trimPathsThroughPopularObjects) {
    ReversePtrs rev = VM.getVM().getRevPtrs();
    if (rev == null) {
      throw new RuntimeException("LivenessAnalysis requires ReversePtrs to have been computed");
    }

    // Currently the reverse pointer analysis returns non-null results
    // only for live objects
    if (rev.get(target) == null) {
      // Object is dead
      return null;
    }

    // HashSet of Oops acting as a bit mask indicating which ones have
    // already been traversed
    Set<Oop> visitedOops = new HashSet<>();

      // IdentityHashMap of LivenessElements acting as a bit mask
      // indicating which roots have already been traversed
    Map<LivenessPathElement, LivenessPathElement> visitedRoots =
      new IdentityHashMap<>();

    visitedOops.add(target);

    // Construct the initial LivenessPath
    LivenessPathList list = new LivenessPathList();
    {
      LivenessPath path = new LivenessPath();
      path.push(new LivenessPathElement(target, null));
      list.add(path);
    }

    // Iterate until done
    while (true) {
      // See if there are any incomplete paths left
      LivenessPath path = null;

      for (int i = list.size() - 1; i >= 0; i--) {
        LivenessPath tmp = list.get(i);
        if (!tmp.isComplete()) {
          path = tmp;
          break;
        }
      }

      // If no incomplete paths left, return
      if (path == null) {
        return list;
      }

      // Try to complete this path

      // Remove the path from the list of reported ones in
      // anticipation of completing it
      list.remove(path);

      try {
        // Fetch next set of reverse pointers for the last object on
        // the list
        ArrayList/*<LivenessPathElement>*/ nextPtrs =
          rev.get(path.peek().getObj());

        // Depending on exactly what the reverse pointers analysis
        // yields, these results may be null, although currently they
        // won't be
        if (nextPtrs != null) {
          // Iterate these
          for (Iterator iter = nextPtrs.iterator(); iter.hasNext(); ) {
            LivenessPathElement nextElement = (LivenessPathElement) iter.next();
            // See whether we've visited this element yet
            if ((nextElement.isRoot() && (visitedRoots.get(nextElement) == null)) ||
                (!nextElement.isRoot() && !visitedOops.contains(nextElement.getObj()))) {
              // Show we've visited this one
              if (nextElement.isRoot()) {
                visitedRoots.put(nextElement, nextElement);
              } else {
                visitedOops.add(nextElement.getObj());
              }

              // Create a new LivenessPath for each one
              LivenessPath nextPath = path.copy();
              nextPath.push(nextElement);

              // Regardless of whether we found a root, put the
              // original path back on the list because we're going to
              // do depth-first searching rather than breadth-first
              list.add(path);
              list.add(nextPath);

              // See whether this path is complete (i.e., it
              // terminates in a root)
              if (trimPathsThroughPopularObjects && nextElement.isRoot()) {
                // Go back through the path list and remove all
                // incomplete paths ending in any of the intermediate
                // (non-root and non-terminal) nodes in this path.
                // This has the effect of removing multiple paths
                // going through popular objects.
                for (int i = 1; i < nextPath.size() - 1; i++) {
                  LivenessPathElement el = nextPath.get(i);
                  int j = 0;
                  while (j < list.size()) {
                    LivenessPath curPath = list.get(j);
                    // We can use an object identity since these
                    // intermediate nodes are canonicalized via the
                    // ReversePtrsAnalysis
                    if (curPath.peek() == el) {
                      list.remove(curPath);
                    } else {
                      j++;
                    }
                  }
                }
              }

              // Do depth-first searching, not breadth-first
              break;
            }
          }
        }
      } catch (Exception e) {
        System.err.println("LivenessAnalysis: WARNING: " + e +
                           " during traversal");
      }
    }
  }
}
