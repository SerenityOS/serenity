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

package sun.jvm.hotspot.debugger.cdbg;

import sun.jvm.hotspot.debugger.*;

/** A BlockSym models a lexical scope in a block-structured
    language. It is (currently) the bottommost scope type. */

public interface BlockSym extends Sym {
  /** Get the lexically enclosing block, or null if none */
  public BlockSym getParent();

  /** Length in bytes of the machine code in this block */
  public long getLength();

  /** Address of the first machine instruction in this block */
  public Address getAddress();

  /** Name of this block, or null if none */
  public String getName();

  /** Number of local variable symbols associated with this block */
  public int getNumLocals();

  /** Return <i>i</i>th local (0..getNumLocals() - 1) */
  public LocalSym getLocal(int i);
}
