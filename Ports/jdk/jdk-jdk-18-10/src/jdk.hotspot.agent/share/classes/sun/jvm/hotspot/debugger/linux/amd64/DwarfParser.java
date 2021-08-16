/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, NTT DATA.
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

package sun.jvm.hotspot.debugger.linux.amd64;

import java.lang.ref.Cleaner;
import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.debugger.DebuggerException;

public class DwarfParser {
  private final long p_dwarf_context; // native dwarf context handle

  private static native void init0();
  private static native long createDwarfContext(long lib);
  private static native void destroyDwarfContext(long context);
  private native boolean isIn0(long pc);

  static {
    init0();
  }

  public DwarfParser(Address lib) {
    p_dwarf_context = createDwarfContext(lib.asLongValue());

    if (p_dwarf_context == 0L) {
      throw new DebuggerException("Could not create DWARF context");
    }

    Cleaner.create()
           .register(this, () -> DwarfParser.destroyDwarfContext(p_dwarf_context));
  }

  public boolean isIn(Address pc) {
    return isIn0(pc.asLongValue());
  }

  private native void processDwarf0(long pc);

  public void processDwarf(Address pc) {
    processDwarf0(pc.asLongValue());
  }

  public native int getCFARegister();
  public native int getCFAOffset();
  public native int getReturnAddressOffsetFromCFA();
  public native int getBasePointerOffsetFromCFA();
  public native boolean isBPOffsetAvailable();
}
