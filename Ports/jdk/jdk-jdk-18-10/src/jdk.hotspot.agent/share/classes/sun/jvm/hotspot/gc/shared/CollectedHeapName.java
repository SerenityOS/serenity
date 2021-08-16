/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.gc.shared;

/** Mimics the enums in the VM under CollectedHeap::Name */

public class CollectedHeapName {
  private String name;

  private CollectedHeapName(String name) { this.name = name; }

  public static final CollectedHeapName SERIAL = new CollectedHeapName("Serial");
  public static final CollectedHeapName PARALLEL = new CollectedHeapName("Parallel");
  public static final CollectedHeapName CMS = new CollectedHeapName("CMS");
  public static final CollectedHeapName G1 = new CollectedHeapName("G1");
  public static final CollectedHeapName EPSILON = new CollectedHeapName("Epsilon");
  public static final CollectedHeapName Z = new CollectedHeapName("Z");
  public static final CollectedHeapName SHENANDOAH = new CollectedHeapName("Shenandoah");

  public String toString() {
    return name;
  }
}
