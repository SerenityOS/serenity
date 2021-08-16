/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import sun.jvm.hotspot.debugger.*;

/** Interface describing how to debug C and C++ programs. Being kept
    very minimal and incomplete for now; can be extended later. */

public interface CDebugger {
  /** Fetch the thread list for the target process as a List of
      ThreadProxy objects. Do not mutate this list. Throws
      DebuggerException if the target process is not suspended (via
      ProcessControl) or if the fetch failed for some other reason. */
  public List<ThreadProxy> getThreadList() throws DebuggerException;

  /** Return a list of LoadObjects in the target process. Do not
      mutate this list. Throws DebuggerException if the target process
      is not suspended (via ProcessControl) or if the fetch failed for
      some other reason. */
  public List<LoadObject> getLoadObjectList() throws DebuggerException;

  /** Fetch the loadobject containing the current program counter.
      Returns null if the PC was outside the ranges of all loadobjects
      in the target process. Throws DebuggerException if the target
      process is not suspended (via ProcessControl) or if the fetch
      failed for some other reason. */
  public LoadObject loadObjectContainingPC(Address pc) throws DebuggerException;

  /** Create a CFrame object for the top frame of the given thread,
      specified as a ThreadProxy. Returns null if there are no frames
      on the stack or the frame can not be created for some other
      reason. Throws DebuggerException if the target process is not
      suspended (via ProcessControl). */
  public CFrame topFrameForThread(ThreadProxy t)
    throws DebuggerException, IllegalThreadStateException;

  /** Get the file name component for the given full path to a DLL.
      (The path separator characters and behavior of File.getName()
      are platform-specific.) */
  public String getNameOfFile(String fileName) throws DebuggerException;

  /** Fetch a ProcessControl object for the target process, enabling
      suspension, resumption and event handling. This method may
      return null for many reasons, including that the underlying
      implementation does not support process interaction, or that the
      target process is dead (i.e., a core file). */
  public ProcessControl getProcessControl() throws DebuggerException;

  /** is demangling of C++ symbols supported by this CDebugger? */
  public boolean canDemangle();

  /** Demangle C++ symbols into readable strings, if possible.
      otherwise returns the input symbol back. */
  public String demangle(String sym) throws UnsupportedOperationException;
}
