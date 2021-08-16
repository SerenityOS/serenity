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

/** Describes a COFF line number. (Some of the descriptions are taken
    directly from Microsoft's documentation and are copyrighted by
    Microsoft.) */

public interface COFFLineNumber {
  /** <P> Union of two fields: Symbol Table Index and RVA. Whether
      Symbol Table Index or RVA is used depends on the value of
      getLineNumber(). </P>

      <P> SymbolTableIndex is used when getLineNumber() is 0: index to
      symbol table entry for a function. This format is used to
      indicate the function that a group of line-number records refer
      to. </P>

      <P> VirtualAddress is used when LineNumber is non-zero: relative
      virtual address of the executable code that corresponds to the
      source line indicated. In an object file, this contains the
      virtual address within the section. </P> */
  public int getType();

  /** When nonzero, this field specifies a one-based line number. When
      zero, the Type field is interpreted as a Symbol Table Index for
      a function. */
  public short getLineNumber();
}
