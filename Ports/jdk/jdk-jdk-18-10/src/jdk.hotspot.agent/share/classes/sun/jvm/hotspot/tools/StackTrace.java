/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.tools;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.oops.*;

/** Traverses and prints the stack traces for all Java threads in the
 * remote VM */
public class StackTrace extends Tool {
    // in non-verbose mode pc, sp and Method* are not printed
    public StackTrace(boolean v, boolean concurrentLocks) {
        this.verbose = v;
        this.concurrentLocks = concurrentLocks;
    }

    public StackTrace() {
        this(true, true);
    }

    public void run() {
        run(System.out);
    }

    public StackTrace(JVMDebugger d) {
        super(d);
    }

    public StackTrace(JVMDebugger d, boolean v, boolean concurrentLocks) {
        super(d);
        this.verbose = v;
        this.concurrentLocks = concurrentLocks;
    }

    public void run(java.io.PrintStream tty) {
        // Ready to go with the database...
        try {
            // print deadlock information before stack trace
            DeadlockDetector.print(tty);
        } catch (Exception exp) {
            exp.printStackTrace();
            tty.println("Can't print deadlocks:" + exp.getMessage());
        }

        try {
            ConcurrentLocksPrinter concLocksPrinter = null;
            if (concurrentLocks) {
                concLocksPrinter = new ConcurrentLocksPrinter();
            }
            Threads threads = VM.getVM().getThreads();
            for (int i = 0; i < threads.getNumberOfThreads(); i++) {
                JavaThread cur = threads.getJavaThreadAt(i);
                if (cur.isJavaThread()) {
                    cur.printThreadInfoOn(tty);
                    try {
                        int count = 0;

                        for (JavaVFrame vf = cur.getLastJavaVFrameDbg(); vf != null; vf = vf.javaSender()) {
                            Method method = vf.getMethod();
                            tty.print(" - " + method.externalNameAndSignature() +
                            " @bci=" + vf.getBCI());

                            int lineNumber = method.getLineNumberFromBCI(vf.getBCI());
                            if (lineNumber != -1) {
                                tty.print(", line=" + lineNumber);
                            }

                            if (verbose) {
                                Address pc = vf.getFrame().getPC();
                                if (pc != null) {
                                    tty.print(", pc=" + pc);
                                }

                                tty.print(", Method*=" + method.getAddress());
                            }

                            if (vf.isCompiledFrame()) {
                                tty.print(" (Compiled frame");
                                if (vf.isDeoptimized()) {
                                  tty.print(" [deoptimized]");
                                }
                            }
                            if (vf.isInterpretedFrame()) {
                                tty.print(" (Interpreted frame");
                            }
                            if (vf.mayBeImpreciseDbg()) {
                                tty.print("; information may be imprecise");
                            }

                            tty.println(")");
                            vf.printLockInfo(tty, count++);
                        }
                    } catch (Exception e) {
                        tty.println("Error occurred during stack walking:");
                        e.printStackTrace();
                    }
                    tty.println();
                    if (concurrentLocks) {
                        concLocksPrinter.print(cur, tty);
                    }
                    tty.println();
              }
          }
      }
      catch (AddressException e) {
        System.err.println("Error accessing address 0x" + Long.toHexString(e.getAddress()));
        e.printStackTrace();
      }
   }

   public static void main(String[] args) {
      StackTrace st = new StackTrace();
      st.execute(args);
   }

   private boolean verbose;
   private boolean concurrentLocks;
}
