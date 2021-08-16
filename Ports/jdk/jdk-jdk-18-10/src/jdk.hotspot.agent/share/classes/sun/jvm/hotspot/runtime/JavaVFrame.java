/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.runtime;

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.debugger.*;

public abstract class JavaVFrame extends VFrame {

  private static final String ADDRESS_FORMAT = VM.getVM().isLP64() ? "0x%016x"
                                                                   : "0x%08x";

  /** JVM state */
  public abstract Method getMethod();
  public abstract int    getBCI();
  public abstract StackValueCollection getLocals();
  public abstract StackValueCollection getExpressions();
  public abstract List<MonitorInfo> getMonitors();

  /** Test operation */
  public boolean isJavaFrame() { return true; }

  /** Package-internal constructor */
  JavaVFrame(Frame fr, RegisterMap regMap, JavaThread thread) {
    super(fr, regMap, thread);
  }

  /** Get monitor (if any) that this JavaVFrame is trying to enter */
  // FIXME: not yet implemented
  //  public Address getPendingMonitor(int frameCount);

  public void printLockedObjectClassName(PrintStream tty,
                                         OopHandle hobj, String lockState) {
    if (hobj.asLongValue() != 0L) {
      tty.format("\t- %s <" + ADDRESS_FORMAT + "> ",
                 lockState, hobj.asLongValue());

      Klass klass = Oop.getKlassForOopHandle(hobj);
      String klassName = klass.getName().asString();
      tty.print("(a ");
      if (klassName.equals("java/lang/Class")) {
        Oop obj = VM.getVM().getObjectHeap().newOop(hobj);
        klassName = java_lang_Class.asExternalName(obj);
        tty.print("java.lang.Class for ");
      }
      tty.println(klassName.replace('/', '.') + ")");
    }
  }

  private String identifyLockState(MonitorInfo monitor, String waitingState) {
    Mark mark = new Mark(monitor.owner());
    if (mark.hasMonitor() &&
        ( // we have marked ourself as pending on this monitor
          mark.monitor().equals(thread.getCurrentPendingMonitor()) ||
          // we are not the owner of this monitor
          !mark.monitor().isEntered(thread)
        )) {
      return waitingState;
    }
    return "locked";
  }

  /** Printing used during stack dumps */
  public void printLockInfo(PrintStream tty, int frameCount) {
    // If this is the first frame and it is java.lang.Object.wait(...)
    // then print out the receiver. Locals are not always available,
    // e.g., compiled native frames have no scope so there are no locals.
    if (frameCount == 0) {
      if (getMethod().getName().asString().equals("wait") &&
          getMethod().getMethodHolder().getName().asString().equals("java/lang/Object")) {
        String waitState = "waiting on"; // assume we are waiting
        // If earlier in the output we reported java.lang.Thread.State ==
        // "WAITING (on object monitor)" and now we report "waiting on", then
        // we are still waiting for notification or timeout. Otherwise if
        // we earlier reported java.lang.Thread.State == "BLOCKED (on object
        // monitor)", then we are actually waiting to re-lock the monitor.
        StackValueCollection locs = getLocals();
        if (!locs.isEmpty()) {
          StackValue sv = locs.get(0);
          if (sv.getType() == BasicType.getTObject()) {
            OopHandle o = sv.getObject();
            if (OopUtilities.threadOopGetThreadStatus(thread.getThreadObj()) == OopUtilities.THREAD_STATUS_BLOCKED_ON_MONITOR_ENTER) {
              waitState = "waiting to re-lock in wait()";
            }
            printLockedObjectClassName(tty, o, waitState);
          }
        } else {
          tty.println("\t- " + waitState + " <no object reference available>");
        }
      } else if (thread.getCurrentParkBlocker() != null) {
        Oop obj = thread.getCurrentParkBlocker();
        Klass k = obj.getKlass();
        tty.format("\t- parking to wait for <" + ADDRESS_FORMAT + "> (a %s)",
                   obj.getHandle().asLongValue(), k.getName().asString());
        tty.println();
      }
    }

    // Print out all monitors that we have locked, or are trying to lock,
    // including re-locking after being notified or timing out in a wait().
    List<MonitorInfo> mons = getMonitors();
    if (!mons.isEmpty()) {
      boolean foundFirstMonitor = false;
      for (int index = mons.size() - 1; index >= 0; index--) {
        MonitorInfo monitor = mons.get(index);
        if (monitor.eliminated() && isCompiledFrame()) { // Eliminated in compiled code
          if (monitor.ownerIsScalarReplaced()) {
            Klass k = Oop.getKlassForOopHandle(monitor.ownerKlass());
            tty.println("\t- eliminated <owner is scalar replaced> (a " + k.getName().asString() + ")");
          } else if (monitor.owner() != null) {
            printLockedObjectClassName(tty, monitor.owner(), "eliminated");
          }
          continue;
        }
        if (monitor.owner() != null) {
          // the monitor is associated with an object, i.e., it is locked
          String lockState = "locked";
          if (!foundFirstMonitor && frameCount == 0) {
            // If this is the first frame and we haven't found an owned
            // monitor before, then we need to see if we have completed
            // the lock or if we are blocked trying to acquire it. Only
            // an inflated monitor that is first on the monitor list in
            // the first frame can block us on a monitor enter.
            lockState = identifyLockState(monitor, "waiting to lock");
          }
          printLockedObjectClassName(tty, monitor.owner(), lockState);
          foundFirstMonitor = true;
        }
      }
    }
  }

  /** Printing operations */

  //
  // FIXME: implement visitor pattern for traversing vframe contents?
  //

  public void print() {
    printOn(System.out);
  }

  public void printOn(PrintStream tty) {
    super.printOn(tty);

    tty.print("\t");
    getMethod().printValueOn(tty);
    tty.println();
    tty.println("\tbci:\t" + getBCI());

    printStackValuesOn(tty, "locals",      getLocals());
    printStackValuesOn(tty, "expressions", getExpressions());
  }

  public void printActivation(int index) {
    printActivationOn(System.out, index);
  }

  public void printActivationOn(PrintStream tty, int index) {
    // frame number and method
    tty.print(index + " - ");
    printValueOn(tty);
    tty.println();

    if (VM.getVM().wizardMode()) {
      printOn(tty);
      tty.println();
    }
  }

  /** Verification operations */
  public void verify() {
  }

  public boolean equals(Object o) {
      if (o == null || !(o instanceof JavaVFrame)) {
          return false;
      }

      JavaVFrame other = (JavaVFrame) o;

      // Check static part
      if (!getMethod().equals(other.getMethod())) {
          return false;
      }

      if (getBCI() != other.getBCI()) {
          return false;
      }

      // dynamic part - we just compare the frame pointer
      if (! getFrame().equals(other.getFrame())) {
          return false;
      }
      return true;
  }

  public int hashCode() {
      return getMethod().hashCode() ^ getBCI() ^ getFrame().hashCode();
  }

  /** Structural compare */
  public boolean structuralCompare(JavaVFrame other) {
    // Check static part
    if (!getMethod().equals(other.getMethod())) {
      return false;
    }

    if (getBCI() != other.getBCI()) {
      return false;
    }

    // Check locals
    StackValueCollection locs      = getLocals();
    StackValueCollection otherLocs = other.getLocals();
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(locs.size() == otherLocs.size(), "sanity check");
    }
    for (int i = 0; i < locs.size(); i++) {
      // it might happen the compiler reports a conflict and
      // the interpreter reports a bogus int.
      if (      isCompiledFrame() && (locs.get(i)).getType()      == BasicType.getTConflict()) continue;
      if (other.isCompiledFrame() && (otherLocs.get(i)).getType() == BasicType.getTConflict()) continue;

      if (!locs.get(i).equals(otherLocs.get(i))) {
        return false;
      }
    }

    // Check expressions
    StackValueCollection exprs      = getExpressions();
    StackValueCollection otherExprs = other.getExpressions();
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(exprs.size() == otherExprs.size(), "sanity check");
    }
    for (int i = 0; i < exprs.size(); i++) {
      if (!exprs.get(i).equals(otherExprs.get(i))) {
        return false;
      }
    }

    return true;
  }

  //--------------------------------------------------------------------------------
  // Internals only below this point
  //

  private void printStackValuesOn(PrintStream tty, String title, StackValueCollection values) {
    if (values.isEmpty()) {
      return;
    }
    tty.println("\t" + title + ":");
    for (int index = 0; index < values.size(); index++) {
      tty.print("\t" + index + "\t");
      values.get(index).printOn(tty);
      tty.println();
    }
  }
}
