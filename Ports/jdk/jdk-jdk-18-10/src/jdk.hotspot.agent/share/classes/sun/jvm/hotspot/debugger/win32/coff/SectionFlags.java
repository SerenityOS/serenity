/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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

/** Constants indicating attributes of the section. (Some of the
    descriptions are taken directly from Microsoft's documentation and
    are copyrighted by Microsoft.) */

public interface SectionFlags {
  /** Reserved for future use. */
  public static final int IMAGE_SCN_TYPE_REG = 0x00000000;

  /** Reserved for future use. */
  public static final int IMAGE_SCN_TYPE_DSECT = 0x00000001;

  /** Reserved for future use. */
  public static final int IMAGE_SCN_TYPE_NOLOAD = 0x00000002;

  /** Reserved for future use. */
  public static final int IMAGE_SCN_TYPE_GROUP = 0x00000004;

  /** Section should not be padded to next boundary. This is obsolete
      and replaced by IMAGE_SCN_ALIGN_1BYTES. This is valid for object
      files only. */
  public static final int IMAGE_SCN_TYPE_NO_PAD = 0x00000008;

  /** Reserved for future use. */
  public static final int IMAGE_SCN_TYPE_COPY = 0x00000010;

  /** Section contains executable code. */
  public static final int IMAGE_SCN_CNT_CODE = 0x00000020;

  /** Section contains initialized data. */
  public static final int IMAGE_SCN_CNT_INITIALIZED_DATA = 0x00000040;

  /** Section contains uninitialized data. */
  public static final int IMAGE_SCN_CNT_UNINITIALIZED_DATA = 0x00000080;

  /** Reserved for future use. */
  public static final int IMAGE_SCN_LNK_OTHER = 0x00000100;

  /** Section contains comments or other information. The .drectve
      section has this type. This is valid for object files only. */
  public static final int IMAGE_SCN_LNK_INFO = 0x00000200;

  /** Reserved for future use. */
  public static final int IMAGE_SCN_TYPE_OVER = 0x00000400;

  /** Section will not become part of the image. This is valid for
      object files only. */
  public static final int IMAGE_SCN_LNK_REMOVE = 0x00000800;

  /** Section contains COMDAT data; see {@link
      sun.jvm.hotspot.debugger.win32.coff.COMDATSelectionTypes}. This is valid
      for object files only. */
  public static final int IMAGE_SCN_LNK_COMDAT = 0x00001000;

  /** Reserved for future use. */
  public static final int IMAGE_SCN_MEM_FARDATA = 0x00008000;

  /** Reserved for future use. */
  public static final int IMAGE_SCN_MEM_PURGEABLE = 0x00020000;

  /** Reserved for future use. */
  public static final int IMAGE_SCN_MEM_16BIT = 0x00020000;

  /** Reserved for future use. */
  public static final int IMAGE_SCN_MEM_LOCKED = 0x00040000;

  /** Reserved for future use. */
  public static final int IMAGE_SCN_MEM_PRELOAD = 0x00080000;

  /** Align data on a 1-byte boundary. This is valid for object files
      only. */
  public static final int IMAGE_SCN_ALIGN_1BYTES = 0x00100000;

  /** Align data on a 2-byte boundary. This is valid for object files
      only. */
  public static final int IMAGE_SCN_ALIGN_2BYTES = 0x00200000;

  /** Align data on a 4-byte boundary. This is valid for object files
      only. */
  public static final int IMAGE_SCN_ALIGN_4BYTES = 0x00300000;

  /** Align data on a 8-byte boundary. This is valid for object files
      only. */
  public static final int IMAGE_SCN_ALIGN_8BYTES = 0x00400000;

  /** Align data on a 16-byte boundary. This is valid for object files
      only. */
  public static final int IMAGE_SCN_ALIGN_16BYTES = 0x00500000;

  /** Align data on a 32-byte boundary. This is valid for object files
      only. */
  public static final int IMAGE_SCN_ALIGN_32BYTES = 0x00600000;

  /** Align data on a 64-byte boundary. This is valid for object files
      only. */
  public static final int IMAGE_SCN_ALIGN_64BYTES = 0x00700000;

  /** Align data on a 128-byte boundary. This is valid for object
      files only. */
  public static final int IMAGE_SCN_ALIGN_128BYTES = 0x00800000;

  /** Align data on a 256-byte boundary. This is valid for object
      files only. */
  public static final int IMAGE_SCN_ALIGN_256BYTES = 0x00900000;

  /** Align data on a 512-byte boundary. This is valid for object
      files only. */
  public static final int IMAGE_SCN_ALIGN_512BYTES = 0x00A00000;

  /** Align data on a 1024-byte boundary. This is valid for object
      files only. */
  public static final int IMAGE_SCN_ALIGN_1024BYTES = 0x00B00000;

  /** Align data on a 2048-byte boundary. This is valid for object
      files only. */
  public static final int IMAGE_SCN_ALIGN_2048BYTES = 0x00C00000;

  /** Align data on a 4096-byte boundary. This is valid for object
      files only. */
  public static final int IMAGE_SCN_ALIGN_4096BYTES = 0x00D00000;

  /** Align data on a 8192-byte boundary. This is valid for object
      files only. */
  public static final int IMAGE_SCN_ALIGN_8192BYTES = 0x00E00000;

  /** Section contains extended relocations. */
  public static final int IMAGE_SCN_LNK_NRELOC_OVFL = 0x01000000;

  /** Section can be discarded as needed. */
  public static final int IMAGE_SCN_MEM_DISCARDABLE = 0x02000000;

  /** Section cannot be cached. */
  public static final int IMAGE_SCN_MEM_NOT_CACHED = 0x04000000;

  /** Section is not pageable. */
  public static final int IMAGE_SCN_MEM_NOT_PAGED = 0x08000000;

  /** Section can be shared in memory. */
  public static final int IMAGE_SCN_MEM_SHARED = 0x10000000;

  /** Section can be executed as code. */
  public static final int IMAGE_SCN_MEM_EXECUTE = 0x20000000;

  /** Section can be read. */
  public static final int IMAGE_SCN_MEM_READ = 0x40000000;

  /** Section can be written to. */
  public static final int IMAGE_SCN_MEM_WRITE = 0x80000000;
}
