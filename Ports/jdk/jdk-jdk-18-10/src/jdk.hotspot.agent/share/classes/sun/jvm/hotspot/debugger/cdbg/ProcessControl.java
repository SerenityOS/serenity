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

/** <P> A highly experimental interface for process control and debug
    events. May not be sufficiently portable; for this reason it has
    been factored out from the CDebugger interface and support for it
    is optional. </P>

    <P> The ProcessControl interface defines a process control and
    event model for debugging. When a process is attached to by the
    base Debugger, all threads in the target process are suspended.
    The ProcessControl interface allows resumption and re-suspension
    of the threads in the target process, setting of breakpoints, and
    reception of debugging events (breakpoint hit, signal received,
    etc.). </P>

    <P> Debugging events are generated one at a time by the target
    process. They must be queued up by the underlying debugging
    mechanism so that an attempt to send a second debugging event
    blocks until the first has been serviced with a call to
    debugEventResume. </P> */

public interface ProcessControl {
  /** Suspends all threads in the target process. A process is already
      suspended when attached to by {@link
      sun.jvm.hotspot.debugger.Debugger.attach(int)}. The application
      should check for the presence of a debug event via
      debugEventPoll() upon re-suspending the target process (if one
      is not yet known to be present.)

      @throw DebuggerException if the process is already suspended or
      if the suspension failed for some other reason. */
  public void suspend() throws DebuggerException;

  /** Resumes all threads in the target process.

      @throw DebuggerException if the process is not suspended or if
      the resumption failed for some other reason. */
  public void resume() throws DebuggerException;

  /** Indicates whether the target process is suspended. */
  public boolean isSuspended() throws DebuggerException;

  /** Sets a breakpoint at the given address. The target process must
      be suspended in order to set a breakpoint.

      @throw DebuggerException if the breakpoint could not be set for
      some reason, including that a breakpoint is already set at that
      address or that the underlying debugging mechanism does not
      support that many breakpoints. */
  public void setBreakpoint(Address addr)
    throws UnmappedAddressException, DebuggerException;

  /** Clears a breakpoint at the given address. The target process
      must be suspended in order to clear a breakpoint.

      @throw DebuggerException if the breakpoint could not be cleared
      for some reason, including that there was no breakpoint at that
      address. */
  public void clearBreakpoint(Address addr) throws DebuggerException;

  /** Indicates whether a breakpoint is set at the given address. */
  public boolean isBreakpointSet(Address addr) throws DebuggerException;

  /** Polls for the presence of a debug event. Does not wait for one
      to be generated; returns null if none was pending. The target
      process does not need to be suspended. Returns the same
      DebugEvent object until the debug event is handled via
      debugEventContinue. Typically the application will suspend the
      target process upon reception of a debug event but before
      handling it via a call to debugEventContinue. This ensures that
      the state of the thread which generated the debug event is
      precisely what it was when the event was generated.

      @return The pending debug event, or null if none pending. */
  public DebugEvent debugEventPoll() throws DebuggerException;

  /** Informs the target process to resume past this debug event. The
      target process does not need to be suspended. Breakpoint debug
      events must be handled transparently by the implementation to
      re-execute the instruction and replace the breakpoint. (Ideally
      they should be replaced in such a way that there is no race
      condition between the re-execution and the re-insertion of the
      breakpoint.) All other kinds of exceptions or signals are passed
      on to the target process.

      @throw DebuggerException if no debug event is pending. */
  public void debugEventContinue()
    throws DebuggerException;
}
