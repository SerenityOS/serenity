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

/** Constants indicating debug information types in the
    DebugDirectory. (Some of the descriptions are taken directly from
    Microsoft's documentation and are copyrighted by Microsoft.) */

public interface DebugTypes {

  /** Unknown value, ignored by all tools. */
  public static final int IMAGE_DEBUG_TYPE_UNKNOWN = 0;

  /** COFF debug information (line numbers, symbol table, and string
      table). This type of debug information is also pointed to by
      fields in the file headers. */
  public static final int IMAGE_DEBUG_TYPE_COFF = 1;

  /** CodeView debug information. The format of the data block is
      described by the CV4 specification. */
  public static final int IMAGE_DEBUG_TYPE_CODEVIEW = 2;

  /** Frame Pointer Omission (FPO) information. This information tells
      the debugger how to interpret non-standard stack frames, which
      use the EBP register for a purpose other than as a frame
      pointer. */
  public static final int IMAGE_DEBUG_TYPE_FPO = 3;

  public static final int IMAGE_DEBUG_TYPE_MISC = 4;
  public static final int IMAGE_DEBUG_TYPE_EXCEPTION = 5;
  public static final int IMAGE_DEBUG_TYPE_FIXUP = 6;
  public static final int IMAGE_DEBUG_TYPE_OMAP_TO_SRC = 7;
  public static final int IMAGE_DEBUG_TYPE_OMAP_FROM_SRC = 8;
  public static final int IMAGE_DEBUG_TYPE_BORLAND = 9;
}
