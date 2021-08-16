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

/** Constants indicating attributes of the object or image file. (Some
    of the descriptions are taken directly from Microsoft's
    documentation and are copyrighted by Microsoft.) */

public interface Characteristics {
  /** Image only, Windows CE, Windows NT and above. Indicates that the
      file does not contain base relocations and must therefore be
      loaded at its preferred base address. If the base address is not
      available, the loader reports an error. Operating systems
      running on top of MS-DOS (Win32s) are generally not able to use
      the preferred base address and so cannot run these
      images. However, beginning with version 4.0, Windows will use an
      application's preferred base address. The default behavior of
      the linker is to strip base relocations from EXEs. */
  public static final short IMAGE_FILE_RELOCS_STRIPPED = (short) 0x0001;

  /** Image only. Indicates that the image file is valid and can be
      run. If this flag is not set, it generally indicates a linker
      error. */
  public static final short IMAGE_FILE_EXECUTABLE_IMAGE = (short) 0x0002;

  /** COFF line numbers have been removed. */
  public static final short IMAGE_FILE_LINE_NUMS_STRIPPED = (short) 0x0004;

  /** COFF symbol table entries for local symbols have been removed. */
  public static final short IMAGE_FILE_LOCAL_SYMS_STRIPPED = (short) 0x0008;

  /** Aggressively trim working set. */
  public static final short IMAGE_FILE_AGGRESSIVE_WS_TRIM = (short) 0x0010;

  /** App can handle > 2gb addresses. */
  public static final short IMAGE_FILE_LARGE_ADDRESS_AWARE = (short) 0x0020;

  /** Use of this flag is reserved for future use. */
  public static final short IMAGE_FILE_16BIT_MACHINE = (short) 0x0040;

  /** Little endian: LSB precedes MSB in memory. */
  public static final short IMAGE_FILE_BYTES_REVERSED_LO = (short) 0x0080;

  /** Machine based on 32-bit-word architecture. */
  public static final short IMAGE_FILE_32BIT_MACHINE = (short) 0x0100;

  /** Debugging information removed from image file. */
  public static final short IMAGE_FILE_DEBUG_STRIPPED = (short) 0x0200;

  /** If image is on removable media, copy and run from swap file. */
  public static final short IMAGE_FILE_REMOVABLE_RUN_FROM_SWAP = (short) 0x0400;

  /** The image file is a system file, not a user program. */
  public static final short IMAGE_FILE_SYSTEM = (short) 0x1000;

  /** The image file is a dynamic-link library (DLL). Such files are
      considered executable files for almost all purposes, although
      they cannot be directly run. */
  public static final short IMAGE_FILE_DLL = (short) 0x2000;

  /** File should be run only on a UP machine. */
  public static final short IMAGE_FILE_UP_SYSTEM_ONLY = (short) 0x4000;

  /** Big endian: MSB precedes LSB in memory. */
  public static final short IMAGE_FILE_BYTES_REVERSED_HI = (short) 0x8000;
}
