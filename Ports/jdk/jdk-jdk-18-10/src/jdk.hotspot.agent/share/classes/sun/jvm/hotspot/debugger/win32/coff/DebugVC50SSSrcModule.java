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

/** Models the "sstSrcModule" subsection in Visual C++ 5.0 debug
    information. This subsection contains line number to address
    mappings. (Some of the descriptions are taken directly from
    Microsoft's documentation and are copyrighted by Microsoft.) */

public interface DebugVC50SSSrcModule extends DebugVC50Subsection {
  /** The number of source files contributing code to segments. */
  public int getNumSourceFiles();

  /** The number of code segments receiving code from this module. */
  public int getNumCodeSegments();

  /** Get description of <i>i</i>th source file
      (0..getNumSourceFiles() - 1) contributing code to this
      module. */
  public DebugVC50SrcModFileDesc getSourceFileDesc(int i);

  /** Get <i>i</i>th (0..getNumCodeSegments() - 1) start offset,
      within a segment, of code contributed to that segment. If this
      and the end offset are both 0, the start/end offsets are not
      known and the file and line tables must be examined to find
      information about the program counter of interest. */
  public int getSegmentStartOffset(int i);

  /** Get <i>i</i>th (0..getNumCodeSegments() - 1) end offset, within
      a segment, of code contributed to that segment. If this and the
      start offset are both 0, the start/end offsets are not known and
      the file and line tables must be examined to find information
      about the program counter of interest. */
  public int getSegmentEndOffset(int i);

  /** Get <i>i</i>th (0..getNumCodeSegments() - 1) segment to which
      this module contributes code. See {@link
      sun.jvm.hotspot.debugger.win32.coff.COFFHeader.getSectionHeader}. */
  public int getSegment(int i);
}
