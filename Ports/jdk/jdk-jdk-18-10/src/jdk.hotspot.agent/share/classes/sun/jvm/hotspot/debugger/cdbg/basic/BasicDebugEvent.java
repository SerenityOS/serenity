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

package sun.jvm.hotspot.debugger.cdbg.basic;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.*;

public class BasicDebugEvent implements DebugEvent {
  private DebugEvent.Type type;
  private ThreadProxy thread;
  private Address     pc;
  private Address     address;
  private boolean     wasWrite;
  private String      detail;

  public BasicDebugEvent(DebugEvent.Type type, ThreadProxy thread) {
    this.type = type;
    this.thread = thread;
  }

  public DebugEvent.Type getType()               { return type;            }
  public ThreadProxy     getThread()             { return thread;          }
  public Address         getPC()                 { return pc;              }
  public boolean         getWasWrite()           { return wasWrite;        }
  public Address         getAddress()            { return address;         }
  public String          getUnknownEventDetail() { return detail;          }

  /** Mutators for convenience */
  public void setType(DebugEvent.Type type)      { this.type = type;       }
  public void setThread(ThreadProxy thread)      { this.thread = thread;   }
  public void setPC(Address pc)                  { this.pc = pc;           }
  public void setWasWrite(boolean val)           { wasWrite = val;         }
  public void setAddress(Address address)        { this.address = address; }
  public void setUnknownEventDetail(String msg)  { detail = msg;           }

  /** Factory methods for convenience */
  public static BasicDebugEvent newLoadObjectLoadEvent(ThreadProxy thread, Address base) {
    return newAddressEvent(DebugEvent.Type.LOADOBJECT_LOAD, thread, base);
  }

  public static BasicDebugEvent newLoadObjectUnloadEvent(ThreadProxy thread, Address base) {
    return newAddressEvent(DebugEvent.Type.LOADOBJECT_UNLOAD, thread, base);
  }

  public static BasicDebugEvent newBreakpointEvent(ThreadProxy thread, Address pc) {
    return newPCEvent(DebugEvent.Type.BREAKPOINT, thread, pc);
  }

  public static BasicDebugEvent newSingleStepEvent(ThreadProxy thread, Address pc) {
    return newPCEvent(DebugEvent.Type.BREAKPOINT, thread, pc);
  }

  public static BasicDebugEvent newAccessViolationEvent(ThreadProxy thread,
                                                        Address pc,
                                                        boolean wasWrite,
                                                        Address addr) {
    BasicDebugEvent ev = newPCEvent(DebugEvent.Type.ACCESS_VIOLATION, thread, pc);
    ev.setWasWrite(wasWrite);
    ev.setAddress(addr);
    return ev;
  }

  public static BasicDebugEvent newUnknownEvent(ThreadProxy thread, String detail) {
    BasicDebugEvent ev = new BasicDebugEvent(DebugEvent.Type.UNKNOWN, thread);
    ev.setUnknownEventDetail(detail);
    return ev;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private static BasicDebugEvent newAddressEvent(DebugEvent.Type type, ThreadProxy thread, Address addr) {
    BasicDebugEvent ev = new BasicDebugEvent(type, thread);
    ev.setAddress(addr);
    return ev;
  }

  private static BasicDebugEvent newPCEvent(DebugEvent.Type type, ThreadProxy thread, Address pc) {
    BasicDebugEvent ev = new BasicDebugEvent(type, thread);
    ev.setPC(pc);
    return ev;
  }
}
