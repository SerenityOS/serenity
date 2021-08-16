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

/** Models the information stored in the export directory table.
    Ostensibly this is supposed to lie in the .edata section.
    However, experience has shown that this data does not appear to be
    present there, instead (likely -- not yet tested) showing up in
    the Export Table portion of the OptionalHeaderDataDirectories.
    (Some of the descriptions are taken directly from Microsoft's
    documentation and are copyrighted by Microsoft.) */

public interface ExportDirectoryTable {
  /** A reserved field, set to zero for now. */
  public int getExportFlags();

  /** Time and date the export data was created. */
  public int getTimeDateStamp();

  /** Major version number. The major/minor version number can be set
      by the user. */
  public short getMajorVersion();

  /** Minor version number. */
  public short getMinorVersion();

  /** Address of the ASCII string containing the name of the
      DLL. Relative to image base. See {@link #getDLLName}. */
  public int getNameRVA();

  /** Convenience routine which returns the name of the DLL containing
      this export directory. */
  public String getDLLName();

  /** Starting ordinal number for exports in this image. This field
      specifies the starting ordinal number for the Export Address
      Table. Usually set to 1. */
  public int getOrdinalBase();

  /** Number of entries in the Export Address Table. */
  public int getNumberOfAddressTableEntries();

  /** Number of entries in the Name Pointer Table (also the number of
      entries in the Ordinal Table). */
  public int getNumberOfNamePointers();

  /** Address of the Export Address Table, relative to the image
      base. */
  public int getExportAddressTableRVA();

  /** Address of the Export Name Pointer Table, relative to the image
      base. The table size is given by Number of Name Pointers. */
  public int getNamePointerTableRVA();

  /** Address of the Ordinal Table, relative to the image base. */
  public int getOrdinalTableRVA();

  /** Returns the <I>i</I>th exported symbol (from 0..{@link
      #getNumberOfNamePointers} - 1). These are arranged in sorted
      order to allow binary searches. */
  public String getExportName(int i);

  /** Returns the <I>i</I>th entry (0..{@link
      #getNumberOfNamePointers} in the Export Ordinal Table.  This is
      used for looking up a given symbol's address in the Export
      Address Table; see {@link #getExportAddress}. */
  public short getExportOrdinal(int i);

  /** Indicates whether the specified export address is really a
      forwarder, in which case the value is not an address but a
      string. */
  public boolean isExportAddressForwarder(short ordinal);

  /** Get the forwarder name for the given ordinal. Must be called
      only if isExportAddressForwarder() returns true. */
  public String getExportAddressForwarder(short ordinal);

  /** <P> Takes in an ordinal from the Export Ordinal Table (see
      {@link #getExportOrdinal}). This ordinal is biased by {@link
      #getOrdinalBase}; however, the subtraction described in the
      documentation is done internally for convenience. Returns an
      address that is in one of two formats. If the address specified
      is not within the export section (as defined by the address and
      length indicated in the Optional Header), the field is an Export
      RVA: an actual address in code or data. Otherwise, the field is
      a Forwarder RVA, which names a symbol in another DLL. </P>

      <P> An Export RVA is the address of the exported symbol when
      loaded into memory, relative to the image base. For example, the
      address of an exported function. </P>

      <P> A Forwarder RVA is a pointer to a null-terminated ASCII
      string in the export section, giving the DLL name and the name
      of the export (for example, "MYDLL.expfunc") or the DLL
      name and an export (for example, "MYDLL.#27"). </P>

      <P> NOTE: the value returned has been transformed from an RVA to
      a file pointer which can be added to the image base to find an
      absolute address for the symbol. </P> */
  public int getExportAddress(short ordinal);
}
