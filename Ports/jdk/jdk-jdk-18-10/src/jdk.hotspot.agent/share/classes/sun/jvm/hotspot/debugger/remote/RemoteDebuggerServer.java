/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger.remote;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.tools.*;

/** The implementation of the RemoteDebugger interface. This
    delegates to a local debugger */

public class RemoteDebuggerServer extends UnicastRemoteObject
  implements RemoteDebugger {

  private transient JVMDebugger debugger;

  /** This is the required no-arg constructor */
  public RemoteDebuggerServer() throws RemoteException {
    super();
  }

  /** This is the constructor used on the machine where the debuggee
   process lies that accepts an RMI connector port */
  public RemoteDebuggerServer(JVMDebugger debugger, int port) throws RemoteException {
    super(port);
    this.debugger = debugger;
  }

  /** This is the constructor used on the machine where the debuggee
   process lies */
  public RemoteDebuggerServer(JVMDebugger debugger) throws RemoteException {
    this(debugger, 0);
  }

  public String getOS() throws RemoteException {
    return debugger.getOS();
  }

  public String getCPU() throws RemoteException {
    return debugger.getCPU();
  }

  public MachineDescription getMachineDescription() throws RemoteException {
    return debugger.getMachineDescription();
  }

  public long lookupInProcess(String objectName, String symbol) throws RemoteException {
    Address addr = debugger.lookup(objectName, symbol);
    return addr == null? 0L : debugger.getAddressValue(addr);
  }

  public ReadResult readBytesFromProcess(long address, long numBytes) throws RemoteException {
    return debugger.readBytesFromProcess(address, numBytes);
  }

  public boolean hasConsole() throws RemoteException {
    return debugger.hasConsole();
  }

  public String getConsolePrompt() throws RemoteException {
    return debugger.getConsolePrompt();
  }

  public String consoleExecuteCommand(String cmd) throws RemoteException {
    return debugger.consoleExecuteCommand(cmd);
  }

  public long getJBooleanSize() throws RemoteException {
    return debugger.getJBooleanSize();
  }

  public long getJByteSize() throws RemoteException {
    return debugger.getJByteSize();
  }

  public long getJCharSize() throws RemoteException {
    return debugger.getJCharSize();
  }

  public long getJDoubleSize() throws RemoteException {
    return debugger.getJDoubleSize();
  }

  public long getJFloatSize() throws RemoteException {
    return debugger.getJFloatSize();
  }

  public long getJIntSize() throws RemoteException {
    return debugger.getJIntSize();
  }

  public long getJLongSize() throws RemoteException {
    return debugger.getJLongSize();
  }

  public long getJShortSize() throws RemoteException {
    return debugger.getJShortSize();
  }

  public long getHeapOopSize() throws RemoteException {
    return debugger.getHeapOopSize();
  }

  public long getNarrowOopBase() throws RemoteException {
    return debugger.getNarrowOopBase();
  }

  public int  getNarrowOopShift() throws RemoteException {
    return debugger.getNarrowOopShift();
  }

  public long getKlassPtrSize() throws RemoteException {
    return debugger.getHeapOopSize();
  }

  public long getNarrowKlassBase() throws RemoteException {
    return debugger.getNarrowKlassBase();
  }

  public int  getNarrowKlassShift() throws RemoteException {
    return debugger.getNarrowKlassShift();
  }

  public boolean   areThreadsEqual(long addrOrId1, boolean isAddress1,
                                   long addrOrId2, boolean isAddress2) throws RemoteException {
    ThreadProxy t1 = getThreadProxy(addrOrId1, isAddress1);
    ThreadProxy t2 = getThreadProxy(addrOrId2, isAddress2);
    return t1.equals(t2);
  }


  public int       getThreadHashCode(long addrOrId, boolean isAddress) throws RemoteException {
    ThreadProxy t = getThreadProxy(addrOrId, isAddress);
    return t.hashCode();
  }

  public long[]    getThreadIntegerRegisterSet(long addrOrId, boolean isAddress) throws RemoteException {
    ThreadProxy t = getThreadProxy(addrOrId, isAddress);
    ThreadContext tc = t.getContext();
    long[] regs = new long[tc.getNumRegisters()];
    for (int r = 0; r < regs.length; r++) {
       regs[r] = tc.getRegister(r);
    }
    return regs;
  }

  private ThreadProxy getThreadProxy(long addrOrId, boolean isAddress) throws DebuggerException {
     if (isAddress) {
        Address addr = debugger.parseAddress("0x" + Long.toHexString(addrOrId));
        return debugger.getThreadForIdentifierAddress(addr);
     } else {
        return debugger.getThreadForThreadId(addrOrId);
     }
  }

  @Override
  public String execCommandOnServer(String command, Map<String, Object> options) throws RemoteException {
    if (command.equals("findsym")) {
      return debugger.findSymbol((String)options.get("symbol"));
    } else {
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      try (var out = new PrintStream(bout)) {
        if (command.equals("pmap")) {
          (new PMap(debugger)).run(out, debugger);
        } else if (command.equals("pstack")) {
          PStack pstack = new PStack(debugger);
          pstack.setVerbose(false);
          pstack.setConcurrentLocks((boolean)options.get("concurrentLocks"));
          pstack.run(out, debugger);
        } else {
          throw new DebuggerException(command + " is not supported in this debugger");
        }
      }
      return bout.toString();
    }
  }
}
