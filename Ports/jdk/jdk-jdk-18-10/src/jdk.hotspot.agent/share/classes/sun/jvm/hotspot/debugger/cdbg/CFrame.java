/*
 * Copyright (c) 2001, 2012, Oracle and/or its affiliates. All rights reserved.
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

/** Models a "C" programming language frame on the stack -- really
    just an arbitrary frame with hooks to access C and C++ debug
    information if available. It is recommended that implementors of
    this interface derive from BasicCFrame, which provides some of the
    functionality. */

public interface CFrame {
  /** Returns null when no more frames on stack */
  public CFrame sender(ThreadProxy th);

  /** Get the program counter of this frame */
  public Address pc();

  /** Get the loadobject in which the PC lies. Returns null if the PC
      is not contained in any of the loadobjects in the target
      process. */
  public LoadObject loadObjectForPC();

  /** If debug information is available, retrieves the block in which
      the program counter lies. Returns null if there is no debug
      information for the current program counter or if the PC could
      not be located for other reasons. */
  public BlockSym blockForPC();

  /** For the loadobject in which the PC lies, fetch the name of the
      closest exported symbol and the distance of the PC to that
      symbol. Returns null if the PC was not within any of the
      loadobjects of the target process. FIXME: specify whether this
      is mangled/demangled. */
  public ClosestSymbol closestSymbolToPC();

  /** Gets the base pointer in this frame from which local variable
      offsets in the debug info are based. Typically this is the
      base-of-frame pointer (EBP on x86, FP/I6 on SPARC). */
  public Address localVariableBase();

  /** Visit all local variables in this frame if debug information is
      available. Automatically descends into compound types and arrays. */
  public void iterateLocals(ObjectVisitor v);
}
