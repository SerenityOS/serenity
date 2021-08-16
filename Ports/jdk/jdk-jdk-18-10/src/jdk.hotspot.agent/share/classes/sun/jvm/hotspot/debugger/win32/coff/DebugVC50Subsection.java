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

/** Base class for subsections in Visual C++ 5.0 debug information. */

public interface DebugVC50Subsection {
  /** Returns type of this subsection; see {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50SubsectionTypes}. */
  public short getSubsectionType();

  /** Returns module index associated with this subsection. This
      number is 1 based and zero is never a valid index. The index
      0xffff is reserved for tables that are not associated with a
      specific module. These tables include sstLibraries,
      sstGlobalSym, sstGlobalPub and sstGlobalTypes. */
  public short getSubsectionModuleIndex();

  /** Number of bytes in subsection. */
  public int getSubsectionSize();
}
