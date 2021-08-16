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

/** Models an entry in the Debug Directory, which is an entry in the
    image optional header. This directory indicates where in the image
    the CodeView debug information appears, if enabled using /Z7
    /PDB:NONE. (Some of the descriptions are taken directly from
    Microsoft's documentation and are copyrighted by Microsoft.) */

public interface DebugDirectoryEntry {
  /** A reserved field intended to be used for flags, set to zero for
      now. */
  public int getCharacteristics();

  /** Time and date the debug data was created. */
  public int getTimeDateStamp();

  /** Major version number of the debug data format. */
  public short getMajorVersion();

  /** Minor version number of the debug data format. */
  public short getMinorVersion();

  /** Format of debugging information: this field enables support of
      multiple debuggers. See
      @link{sun.jvm.hotspot.debugger.win32.coff.DebugTypes}. */
  public int getType();

  /** Size of the debug data (not including the debug directory itself). */
  public int getSizeOfData();

  /** Address of the debug data when loaded, relative to the image base. */
  public int getAddressOfRawData();

  /** File pointer to the debug data. */
  public int getPointerToRawData();

  /** If this debug directory entry is of type
      IMAGE_DEBUG_TYPE_CODEVIEW (see
      @link{sun.jvm.hotspot.debugger.win32.coff.DebugTypes}), returns
      the contents as a DebugVC50 object; otherwise, returns null. */
  public DebugVC50 getDebugVC50();

  /** Placeholder */
  public byte getRawDataByte(int i);
}
