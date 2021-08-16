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

/** Models the "sstFileIndex" subsection in Visual C++ 5.0 debug
    information. This subsection contains a list of all of the source
    files that contribute code to any module (compiland) in the
    executable. File names are partially qualified relative to the
    compilation directory. */
public interface DebugVC50SSFileIndex extends DebugVC50Subsection {
  /** Number of file name references per module. */
  public short getNumModules();

  /** Count of the total number of file name references. */
  public short getNumReferences();

  /** Array of indices into the <i>NameOffset</i> table for each
      module. Each index is the start of the file name references for
      each module. */
  public short[] getModStart();

  /** Number of file name references per module. */
  public short[] getRefCount();

  /** Array of offsets into the Names table. For each module, the
      offset to first referenced file name is at NameRef[ModStart] and
      continues for cRefCnt entries. FIXME: this probably is useless
      and needs fixup to convert these offsets into indices into the
      following array. */
  public int[] getNameRef();

  /** List of zero terminated file names. Each file name is partially
      qualified relative to the compilation directory. */
  public String[] getNames();
}
