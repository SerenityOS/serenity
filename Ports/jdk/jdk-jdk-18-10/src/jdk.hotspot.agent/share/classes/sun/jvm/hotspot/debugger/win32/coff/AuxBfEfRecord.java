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

/** Describes an Auxiliary .bf/.ef Record, which follows a .bf or .ef
    symbol. (Some of the descriptions are taken directly from
    Microsoft's documentation and are copyrighted by Microsoft.)  */

public interface AuxBfEfRecord extends AuxSymbolRecord {
  /** Actual ordinal line number (1, 2, 3, etc.) within source file,
      corresponding to the .bf or .ef record. */
  public short getLineNumber();

  /** Symbol-table index of the next .bf symbol record. If the
      function is the last in the symbol table, this field is set to
      zero. Not used for .ef records. */
  public int getPointerToNextFunction();
}
