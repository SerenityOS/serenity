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

/** Describes an Auxiliary Section Definitions record, which follows a
    symbol that defines a section. Such a symbol has a name that is
    the name of a section (such as <B>.text</B> or <B>drectve</B> and
    has storage class STATIC. The auxiliary record provides
    information on the section referred to. Thus it duplicates some of
    the information in the section header. (Some of the descriptions
    are taken directly from Microsoft's documentation and are
    copyrighted by Microsoft.)  */

public interface AuxSectionDefinitionsRecord extends AuxSymbolRecord {
  /** Size of section data; same as Size of Raw Data in the section
      header. */
  public int getLength();

  /** Number of relocation entries for the section. */
  public short getNumberOfRelocations();

  /** Number of line-number entries for the section. */
  public short getNumberOfLineNumbers();

  /** Checksum for communal data. Applicable if the
      IMAGE_SCN_LNK_COMDAT flag is set in the section header. */
  public int getCheckSum();

  /** One-based index into the Section Table for the associated
      section; used when the COMDAT Selection setting is 5. */
  public short getNumber();

  /** COMDAT selection number. Applicable if the section is a COMDAT
      section. See {@link
      sun.jvm.hotspot.debugger.win32.coff.COMDATSelectionTypes}. */
  public byte getSelection();
}
