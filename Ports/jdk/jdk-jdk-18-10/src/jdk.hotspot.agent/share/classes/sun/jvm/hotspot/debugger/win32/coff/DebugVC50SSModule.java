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

/** Models the "module" subsection in Visual C++ 5.0 debug
    information. (Some of the descriptions are taken directly from
    Microsoft's documentation and are copyrighted by Microsoft.) */

public interface DebugVC50SSModule extends DebugVC50Subsection {
  public short getOverlayNumber();
  /** Index into sstLibraries subsection if this module was linked
      from a library. */
  public short getLibrariesIndex();

  /** Count of the number of code segments this module contributes to.
      This is the length of the SegInfo array, below. */
  public short getNumCodeSegments();

  /** Debugging style for this module. Currently only "CV" is defined.
      A module can have only one debugging style. If a module contains
      debugging information in an unrecognized style, the information
      will be discarded. */
  public short getDebuggingStyle();

  /** Fetch description of segment to which this module contributes
      code (0..getNumCodeSegments - 1) */
  public DebugVC50SegInfo getSegInfo(int i);

  /** Name of the module */
  public String getName();
}
