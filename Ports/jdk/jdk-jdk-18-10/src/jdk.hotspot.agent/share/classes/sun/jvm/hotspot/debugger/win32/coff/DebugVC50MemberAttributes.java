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

/** Member attributes used to describe fields and methods, represented
    as a 16-bit bit field. */

public interface DebugVC50MemberAttributes {
  /** Access protection of the item */
  // FIXME: verify these are correct (properly aligned)
  public static final short MEMATTR_ACCESS_MASK          = (short) 0x0003;
  public static final short MEMATTR_ACCESS_NO_PROTECTION = (short) 0;
  public static final short MEMATTR_ACCESS_PRIVATE       = (short) 1;
  public static final short MEMATTR_ACCESS_PROTECTED     = (short) 2;
  public static final short MEMATTR_ACCESS_PUBLIC        = (short) 3;

  /** Method attribute bit field for various type records */
  // FIXME: verify these are correct (properly aligned)
  public static final short MEMATTR_MPROP_MASK                     = (short) 0x001C;
  // 00000
  public static final short MEMATTR_MPROP_VANILLA                  = (short) 0x0000;
  // 00100
  public static final short MEMATTR_MPROP_VIRTUAL                  = (short) 0x0004;
  // 01000
  public static final short MEMATTR_MPROP_STATIC                   = (short) 0x0008;
  // 01100
  public static final short MEMATTR_MPROP_FRIEND                   = (short) 0x000C;
  // 10000
  public static final short MEMATTR_MPROP_INTRODUCING_VIRTUAL      = (short) 0x0010;
  // 10100
  public static final short MEMATTR_MPROP_PURE_VIRTUAL             = (short) 0x0014;
  // 11000
  public static final short MEMATTR_MPROP_PURE_INTRODUCING_VIRTUAL = (short) 0x0018;

  /** Set if the method is never instantiated by the compiler */
  public static final short MEMATTR_PSEUDO_MASK      = (short) 0x0020;

  /** Set if the class cannot be inherited */
  public static final short MEMATTR_NOINHERIT_MASK   = (short) 0x0040;

  /** Set if the class cannot be constructed */
  public static final short MEMATTR_NOCONSTRUCT_MASK = (short) 0x0080;

  /** Set if the method is instantiated by the compiler */
  public static final short MEMATTR_COMPGENX_MASK    = (short) 0x0100;
}
