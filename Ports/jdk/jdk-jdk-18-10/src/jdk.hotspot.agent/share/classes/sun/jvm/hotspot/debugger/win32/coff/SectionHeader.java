/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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

/** Describes the header of a section in a COFF file. The section
    headers are grouped together into the Section Table. (Some of the
    descriptions are taken directly from Microsoft's documentation and
    are copyrighted by Microsoft.) */

public interface SectionHeader {
  public String getName();

  /** Total size of the section when loaded into memory. If this value
      is greater than Size of Raw Data, the section is zero-padded.
      This field is valid only for executable images and should be set
      to 0 for object files. */
  public int getSize();

  /** For executable images this is the address of the first byte of
      the section, when loaded into memory, relative to the image
      base. For object files, this field is the address of the first
      byte before relocation is applied; for simplicity, compilers
      should set this to zero. Otherwise, it is an arbitrary value
      that is subtracted from offsets during relocation. */
  public int getVirtualAddress();

  /** Size of the section (object file) or size of the initialized
      data on disk (image files). For executable image, this must be a
      multiple of FileAlignment from the optional header. If this is
      less than VirtualSize the remainder of the section is zero
      filled. Because this field is rounded while the VirtualSize
      field is not it is possible for this to be greater than
      VirtualSize as well. When a section contains only uninitialized
      data, this field should be 0. */
  public int getSizeOfRawData();

  /** File pointer to section's first page within the COFF file. For
      executable images, this must be a multiple of FileAlignment from
      the optional header. For object files, the value should be
      aligned on a four-byte boundary for best performance. When a
      section contains only uninitialized data, this field should be
      0. */
  public int getPointerToRawData();

  /** File pointer to beginning of relocation entries for the section.
      Set to 0 for executable images or if there are no
      relocations. */
  public int getPointerToRelocations();

  /** File pointer to beginning of line-number entries for the
      section. Set to 0 if there are no COFF line numbers. */
  public int getPointerToLineNumbers();

  /** Number of relocation entries for the section. Set to 0 for
      executable images. */
  public short getNumberOfRelocations();

  /** Number of line-number entries for the section. */
  public short getNumberOfLineNumbers();

  /** Flags describing section's characteristics; see {@link
      sun.jvm.hotspot.debugger.win32.coff.SectionFlags}. */
  public int getSectionFlags();

  /** Returns true if the appropriate flag (from {@link
      sun.jvm.hotspot.debugger.win32.coff.SectionFlags}) is set. */
  public boolean hasSectionFlag(int flag);

  /** This is only present for object files. Retrieves the COFF
      relocation at the given index; valid indices are numbered
      0...getNumberOfRelocations() - 1. */
  public COFFRelocation getCOFFRelocation(int index);

  /** Retrieves the COFF line number at the given index; valid indices
      are numbered 0...getNumberOfLineNumbers() - 1. */
  public COFFLineNumber getCOFFLineNumber(int index);
}
