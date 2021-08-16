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

/** Models the information stored in the Windows-specific fields
    portion of the optional header of a Portable Executable file.
    (Some of the descriptions are taken directly from Microsoft's
    documentation and are copyrighted by Microsoft.) */

public interface OptionalHeaderWindowsSpecificFields {
  /** Preferred address of first byte of image when loaded into
      memory; must be a multiple of 64K. The default for DLLs is
      0x10000000. The default for Windows CE EXEs is 0x00010000. The
      default for Windows NT, Windows 95, and Windows 98 is
      0x00400000. */
  public long getImageBase();

  /** Alignment (in bytes) of sections when loaded into memory. Must
      be greater or equal to File Alignment. Default is the page size
      for the architecture. */
  public int getSectionAlignment();

  /** Alignment factor (in bytes) used to align the raw data of
      sections in the image file. The value should be a power of 2
      between 512 and 64K inclusive. The default is 512. If the
      SectionAlignment is less than the architecture's page size than
      this must match the SectionAlignment. */
  public int getFileAlignment();

  /** Major version number of required OS. */
  public short getMajorOperatingSystemVersion();

  /** Minor version number of required OS. */
  public short getMinorOperatingSystemVersion();

  /** Major version number of image. */
  public short getMajorImageVersion();

  /** Minor version number of image. */
  public short getMinorImageVersion();

  /** Major version number of subsystem. */
  public short getMajorSubsystemVersion();

  /** Minor version number of subsystem. */
  public short getMinorSubsystemVersion();

  /** Size, in bytes, of image, including all headers; must be a
      multiple of Section Alignment. */
  public int getSizeOfImage();

  /** Combined size of MS-DOS stub, PE Header, and section headers
      rounded up to a multiple of FileAlignment. */
  public int getSizeOfHeaders();

  /** Image file checksum. The algorithm for computing is incorporated
      into IMAGHELP.DLL. The following are checked for validation at
      load time: all drivers, any DLL loaded at boot time, and any DLL
      that ends up in the server. */
  public int getCheckSum();

  /** Subsystem required to run this image; returns one of the
      constants defined in {@link
      sun.jvm.hotspot.debugger.win32.coff.WindowsNTSubsystem}. */
  public short getSubsystem();

  /** Indicates characteristics of a DLL; see {@link
      sun.jvm.hotspot.debugger.win32.coff.DLLCharacteristics}. */
  public short getDLLCharacteristics();

  /** Size of stack to reserve. Only the Stack Commit Size is
      committed; the rest is made available one page at a time, until
      reserve size is reached. */
  public long getSizeOfStackReserve();

  /** Size of stack to commit. */
  public long getSizeOfStackCommit();

  /** Size of local heap space to reserve. Only the Heap Commit Size
      is committed; the rest is made available one page at a time,
      until reserve size is reached. */
  public long getSizeOfHeapReserve();

  /** Size of local heap space to commit. */
  public long getSizeOfHeapCommit();

  /** Obsolete. */
  public int getLoaderFlags();

  /** Number of data-dictionary entries in the remainder of the
      Optional Header. Each describes a location and size. */
  public int getNumberOfRvaAndSizes();
}
