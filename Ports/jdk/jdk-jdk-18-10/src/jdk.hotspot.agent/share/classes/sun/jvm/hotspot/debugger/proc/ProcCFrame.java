/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger.proc;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.*;
import sun.jvm.hotspot.debugger.cdbg.basic.*;

final class ProcCFrame extends BasicCFrame {
   public Address pc() {
      return pc;
   }

   public Address localVariableBase() {
      return fp;
   }

   public CFrame sender(ThreadProxy t) {
      return sender;
   }

   public ClosestSymbol closestSymbolToPC() {
      // we don't use base class ELF parsing based
      // symbol lookup for pc for performance reasons.
      return procDbg.lookup(procDbg.getAddressValue(pc));
   }

   // package/class internals only

   ProcCFrame(ProcDebugger dbg, Address pc, Address fp) {
      super(dbg.getCDebugger());
      this.pc = pc;
      this.fp = fp;
      this.procDbg = dbg;
   }

   void setSender(ProcCFrame sender) {
      this.sender = sender;
   }

   private Address    pc;
   private Address    fp;
   private ProcCFrame sender;
   private ProcDebugger procDbg;
}
