/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger.win32.coff;

/** Supplies line number/address mapping for a given source file. */

public interface DebugVC50SrcModLineNumberMap {
  /** Segment index for this table. */
  public int getSegment();

  /** Number of source line pairs. */
  public int getNumSourceLinePairs();

  /** Get the <i>i</i>th (i..getNumSourceLinePairs() - 1) offset
      within the code segment of the start of the line in the parallel
      line number array. */
  public int getCodeOffset(int i);

  /** Get the <i>i</i>th (i..getNumSourceLinePairs() - 1) line
      number. */
  public int getLineNumber(int i);
}
