/*
 * Copyright (c) 2000, 2003, Oracle and/or its affiliates. All rights reserved.
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

import sun.jvm.hotspot.*;
import sun.jvm.hotspot.types.*;
import java.io.*;

// An IndexableFieldIdentifier describes a field in an Oop accessed by an index

public class IndexableFieldIdentifier extends FieldIdentifier {

  public IndexableFieldIdentifier(int index) {
    this.index = index;
  }

  private int index;

  public int getIndex() { return index; }

  public String getName() { return Integer.toString(getIndex()); }

  public void printOn(PrintStream tty) {
    tty.print(" - " + getIndex() + ":\t");
  }

  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }

    if (!(obj instanceof IndexableFieldIdentifier)) {
      return false;
    }

    return (((IndexableFieldIdentifier) obj).getIndex() == index);
  }

  public int hashCode() {
    return index;
  }
};
