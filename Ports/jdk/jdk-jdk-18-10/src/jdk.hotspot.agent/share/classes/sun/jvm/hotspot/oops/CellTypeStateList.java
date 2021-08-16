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

package sun.jvm.hotspot.oops;

import java.util.*;

/** Auxiliary class for GenerateOopMap */
public class CellTypeStateList {
  public CellTypeStateList(int size) {
    list = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      list.add(i, CellTypeState.makeBottom());
    }
  }

  public int size() {
    return list.size();
  }

  public CellTypeState get(int i) {
    return list.get(i);
  }

  public CellTypeStateList subList(int fromIndex, int toIndex) {
    return new CellTypeStateList(list.subList(fromIndex, toIndex));
  }

  //----------------------------------------------------------------------
  private List<CellTypeState> list;
  private CellTypeStateList(List<CellTypeState> list) {
    this.list = list;
  }
}
