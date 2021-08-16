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

// FIXME: NOT FINISHED

/** Models the information stored in the data directories portion of
    the optional header of a Portable Executable file. FIXME: the
    DataDirectory objects are less than useful; need to bring up more
    of the data structures defined in the documentation. (Some of the
    descriptions are taken directly from Microsoft's documentation and
    are copyrighted by Microsoft.) */

public interface OptionalHeaderDataDirectories {
  /** Export Table address and size. */
  public DataDirectory getExportTable() throws COFFException;

  /** Returns the Export Table, or null if none was present. */
  public ExportDirectoryTable getExportDirectoryTable() throws COFFException;

  /** Import Table address and size */
  public DataDirectory getImportTable() throws COFFException;

  /** Resource Table address and size. */
  public DataDirectory getResourceTable() throws COFFException;

  /** Exception Table address and size. */
  public DataDirectory getExceptionTable() throws COFFException;

  /** Attribute Certificate Table address and size. */
  public DataDirectory getCertificateTable() throws COFFException;

  /** Base Relocation Table address and size. */
  public DataDirectory getBaseRelocationTable() throws COFFException;

  /** Debug data starting address and size. */
  public DataDirectory getDebug() throws COFFException;

  /** Returns the Debug Directory, or null if none was present. */
  public DebugDirectory getDebugDirectory() throws COFFException;

  /** Architecture-specific data address and size. */
  public DataDirectory getArchitecture() throws COFFException;

  /** Relative virtual address of the value to be stored in the global
      pointer register. Size member of this structure must be set to
      0. */
  public DataDirectory getGlobalPtr() throws COFFException;

  /** Thread Local Storage (TLS) Table address and size. */
  public DataDirectory getTLSTable() throws COFFException;

  /** Load Configuration Table address and size. */
  public DataDirectory getLoadConfigTable() throws COFFException;

  /** Bound Import Table address and size. */
  public DataDirectory getBoundImportTable() throws COFFException;

  /** Import Address Table address and size. */
  public DataDirectory getImportAddressTable() throws COFFException;

  /** Address and size of the Delay Import Descriptor. */
  public DataDirectory getDelayImportDescriptor() throws COFFException;

  /** COM+ Runtime Header address and size */
  public DataDirectory getCOMPlusRuntimeHeader() throws COFFException;
}
