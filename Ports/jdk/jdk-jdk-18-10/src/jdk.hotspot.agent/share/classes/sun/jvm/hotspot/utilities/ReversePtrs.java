/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import sun.jvm.hotspot.oops.*;

/** ReversePtrs hashtable. */

public class ReversePtrs  {
  private HashMap<Oop, ArrayList<LivenessPathElement>> rp;

  public ReversePtrs() {
    rp = new HashMap<>();
  }

  public void put(LivenessPathElement from, Oop to) {
    if (to == null) return;
    ArrayList<LivenessPathElement> al = rp.get(to);
    if (al == null) al = new ArrayList<>();
    // Inserting at the beginning is a hack to change the reported
    // paths from LivenessAnalysis to look more like they used to;
    // otherwise paths through the Finalizer queue to popular objects
    // seem to be preferred
    al.add(0, from);
    rp.put(to, al);
  }

  /** Returns an ArrayList of the incoming references to this Oop if
      it is alive, and null if it is dead according to the
      ReversePtrsAnalysis. Currently not all roots are scanned so this
      result is frequently inaccurate for JVM-internal objects, but is
      usually correct for Java-level objects. */
  public ArrayList<LivenessPathElement> get(Oop obj) {
    return rp.get(obj);
  }
}
