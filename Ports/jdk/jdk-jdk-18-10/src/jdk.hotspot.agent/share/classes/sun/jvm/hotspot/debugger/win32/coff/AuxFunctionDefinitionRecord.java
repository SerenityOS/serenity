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

/** Describes an Auxiliary Function Definition record, which follows a
    function definition symbol record. (Some of the descriptions are
    taken directly from Microsoft's documentation and are copyrighted
    by Microsoft.)  */

public interface AuxFunctionDefinitionRecord extends AuxSymbolRecord {
  /** Symbol-table index of the corresponding .bf (begin function)
      symbol record. */
  public int getTagIndex();

  /** Size of the executable code for the function itself. If the
      function is in its own section, the Size of Raw Data in the
      section header will be greater or equal to this field, depending
      on alignment considerations. */
  public int getTotalSize();

  /** Index of the first COFF line-number entry for the function in
      the global array of line numbers (see {@link
      sun.jvm.hotspot.debugger.win32.coff.SectionHeader#getCOFFLineNumber}),
      or -1 if none exists. */
  public int getPointerToLineNumber();

  /** Symbol-table index of the record for the next function. If the
      function is the last in the symbol table, this field is set to
      zero. */
  public int getPointerToNextFunction();
}
