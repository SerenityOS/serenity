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

/** Models a segment descriptor in {@link
    sun.jvm.hotspot.debugger.win32.coff.DebugVC50SSSegMap}. */

public interface DebugVC50SegDesc {
  /** Descriptor flags bit field. See {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50SegDescEnums}. */
  public short getFlags();

  /** The logical overlay number. */
  public short getOverlayNum();

  /** The group index into the descriptor array. The group index must
      either be 0 or cSegLog <= group < cSeg. */
  public short getGroup();

  /** <p> This value has different meanings depending upon the values
      of fAbs and fSel in the flags bit array and ovl: </p>

      <p>
      <table>
      <tr> <td> fAbs <td> fSel <td> ovl <td> Operation
      <tr> <td> 0    <td> 0    <td> 0   <td> Frame is added to PSP + 0x10 if not a .com file
      <tr> <td> 0    <td> 0    <td> 0   <td> Frame is added to PSP if it is a .com file
      <tr> <td> 0    <td> 0    <td> != 0 <td> Frame is added to current overlay base
      <tr> <td> 1    <td> 0    <td> x   <td> Frame is absolute address
      <tr> <td> 0    <td> 1    <td> x   <td> Frame contains a selector
      </table>
      </p>
  */
  public short getFrame();

  /** The byte index of the segment or group name in the sstSegName
      table. A value of 0xffff indicates there is no name. */
  public short getName();

  /** The byte index of the class name in the sstSegName table. A
      value of 0xffff indicates there is no name.  */
  public short getClassName();

  /** Byte offset of the logical segment within the specified physical
      segment. If fGroup is set in flags, offset is the offset of the
      group in the physical segment. Currently, all groups define
      physical segments so offset will be zero for groups. */
  public int   getOffset();

  /** Byte count of the logical segment or group. */
  public int   getSize();
}
