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

/** Describes a COFF symbol. (Some of the descriptions are taken
    directly from Microsoft's documentation and are copyrighted by
    Microsoft.) */

public interface COFFSymbol {
  /** Offset within the file of this record. (FIXME: Now that we have
      the auxiliary records exposed, it may not be necessary to expose
      this.) */
  public int getOffset();

  public String getName();

  /** Value associated with the symbol. The interpretation of this
      field depends on Section Number and Storage Class. A typical
      meaning is the relocatable address. */
  public int getValue();

  /** Signed integer identifying the section, using a one-based index
      into the Section Table. Some values have special meaning defined
      in {@link sun.jvm.hotspot.debugger.win32.coff.COFFSymbolConstants}. */
  public short getSectionNumber();

  /** <P> The Type field of a symbol table entry contains two bytes,
      each byte representing type information. The least-significant
      byte represents simple (base) data type, and the
      most-significant byte represents complex type, if any: </P>

      <P> MSB: Complex type: none, pointer, function, array. </P>

      <P> LSB: Base type: integer, floating-point, etc. </P>

      <P> The possible base type values are listed in {@link
      sun.jvm.hotspot.debugger.win32.coff.COFFSymbolConstants} under the
      IMAGE_SYM_TYPE constants. </P>

      <P> The most significant byte specifies whether the symbol is a
      pointer to, function returning, or array of the base type
      specified in the least significant byte. Microsoft tools use
      this field only to indicate whether or not the symbol is a
      function, so that the only two resulting values are 0x0 and 0x20
      for the Type field. However, other tools can use this field to
      communicate more information. </P>

      <P> It is very important to specify the function attribute
      correctly. This information is required for incremental linking
      to work correctly. For some architectures the information may be
      required for other purposes. </P>

      <P> The possible function types are listed in {@link
      sun.jvm.hotspot.debugger.win32.coff.COFFSymbolConstants} under the
      IMAGE_SYM_DTYPE constants. </P> */
  public short getType();

  /** Enumerated value representing storage class. See {@link
      sun.jvm.hotspot.debugger.win32.coff.COFFSymbolConstants} under the
      IMAGE_SYM_CLASS constants. */
  public byte getStorageClass();

  /** Number of auxiliary symbol table entries that follow this
      record. (FIXME: the APIs below which fetch such an auxiliary
      symbol are only currently capable of fetching the first one.) */
  public byte getNumberOfAuxSymbols();

  /** Indicates whether this symbol is a function definition: storage
      class EXTERNAL (2), a Type value indicating it is a function
      (0x20), and a section number greater than zero. This indicates
      that the function is followed by an {@link
      sun.jvm.hotspot.debugger.win32.coff.AuxFunctionDefinitionRecord}.
      Note that a symbol table record that has a section number of
      UNDEFINED (0) does not define the function and does not have an
      auxiliary record. */
  public boolean isFunctionDefinition();

  /** This should only be called if {@link #isFunctionDefinition}
      returns true. */
  public AuxFunctionDefinitionRecord getAuxFunctionDefinitionRecord();

  /** Indicates whether this symbol is a .bf or .ef symbol record and
      is therefore followed by an {@link
      sun.jvm.hotspot.debugger.win32.coff.AuxBfEfRecord}. */
  public boolean isBfOrEfSymbol();

  /** This should only be called if {@link #isBfOrEfSymbol} returns
      true. */
  public AuxBfEfRecord getAuxBfEfRecord();

  /** Indicates whether this symbol is a weak external and is
      therefore followed by an {@link
      sun.jvm.hotspot.debugger.win32.coff.AuxWeakExternalRecord}. */
  public boolean isWeakExternal();

  /** This should only be called if {@link #isWeakExternal} returns
      true. */
  public AuxWeakExternalRecord getAuxWeakExternalRecord();

  /** Indicates whether this symbol has storage class FILE and is
      therefore followed by a {@link
      sun.jvm.hotspot.debugger.win32.coff.AuxFileRecord}. */
  public boolean isFile();

  /** This should only be called if {@link #isFile} returns
      true. */
  public AuxFileRecord getAuxFileRecord();

  /** Indicates whether this symbol defines a section and is therefore
      followed by an {@link
      sun.jvm.hotspot.debugger.win32.coff.AuxSectionDefinitionsRecord}. */
  public boolean isSectionDefinition();

  /** This should only be called if {@link #isSectionDefinition} returns
      true. */
  public AuxSectionDefinitionsRecord getAuxSectionDefinitionsRecord();
}
