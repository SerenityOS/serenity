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

/** Models the information stored in the COFF header of either a
    Portable Executable or object file. */

public interface COFFHeader {
  /** Returns one of the constants in {@link
      sun.jvm.hotspot.debugger.win32.coff.MachineTypes}. */
  public short getMachineType();

  /** Number of sections; indicates size of the Section Table, which
      immediately follows the headers. */
  public short getNumberOfSections();

  /** Time and date the file was created. */
  public int getTimeDateStamp();

  /** File offset of the COFF symbol table or 0 if none is present. */
  public int getPointerToSymbolTable();

  /** Number of entries in the symbol table. This data can be used in
      locating the string table, which immediately follows the symbol
      table. */
  public int getNumberOfSymbols();

  /** Size of the optional header, which is required for executable
      files but not for object files. An object file should have a
      value of 0 here. */
  public short getSizeOfOptionalHeader();

  /** Returns the optional header if one is present or null if not. */
  public OptionalHeader getOptionalHeader() throws COFFException;

  /** Gets the union of all characteristics set for this object or
      image file. See {@link
      sun.jvm.hotspot.debugger.win32.coff.Characteristics}. */
  public short getCharacteristics();

  /** Indicates whether this file has the given characteristic. The
      argument must be one of the constants specified in {@link
      sun.jvm.hotspot.debugger.win32.coff.Characteristics}. */
  public boolean hasCharacteristic(short characteristic);

  /** Retrieves the section header at the given index, between
      1 and getNumberOfSections(). <B>NOTE</B>: This index is one-based,
      so the first section is numbered one, not zero. */
  public SectionHeader getSectionHeader(int index);

  /** Retrieves the COFF symbol at the given index, between 0 and
      getNumberOfSymbols() - 1. This is distinct from CodeView
      information. */
  public COFFSymbol getCOFFSymbol(int index);

  /** Returns the number of strings in the String Table, which
      immediately follows the COFF Symbol Table. */
  public int getNumberOfStrings();

  /** Retrieves the <i>i</i>th string (0..{@link #getNumberOfStrings} - 1)
      from the string table. */
  public String getString(int i);
}
