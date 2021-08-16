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

/** Describes an Auxiliary Weak External record, which follows a
    weak-external symbol record. (Some of the descriptions are taken
    directly from Microsoft's documentation and are copyrighted by
    Microsoft.)  */

public interface AuxWeakExternalRecord extends AuxSymbolRecord {
  public static final int IMAGE_WEAK_EXTERN_SEARCH_NOLIBRARY = 1;
  public static final int IMAGE_WEAK_EXTERN_SEARCH_LIBRARY   = 2;
  public static final int IMAGE_WEAK_EXTERN_SEARCH_ALIAS     = 3;

  /** Symbol-table index of sym2, the symbol to be linked if sym1 is
      not found. */
  public int getTagIndex();

  /** <P> A value of {@link #IMAGE_WEAK_EXTERN_SEARCH_NOLIBRARY}
      indicates that no library search for sym1 should be
      performed. </P>

      <P> A value of {@link #IMAGE_WEAK_EXTERN_SEARCH_LIBRARY}
      indicates that a library search for sym1 should be
      performed. </P>

      <P> A value of {@link #IMAGE_WEAK_EXTERN_SEARCH_ALIAS} indicates
      that sym1 is an alias for sym2. </P> */
  public int getCharacteristics();
}
