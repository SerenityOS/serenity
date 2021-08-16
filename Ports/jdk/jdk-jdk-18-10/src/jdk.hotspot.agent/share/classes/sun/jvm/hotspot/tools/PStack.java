/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.*;
import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.interpreter.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.*;
import sun.jvm.hotspot.debugger.remote.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.utilities.PlatformInfo;

public class PStack extends Tool {
    // in non-verbose mode, Method*s are not printed in java frames
   public PStack(boolean v, boolean concurrentLocks, HotSpotAgent agent) {
      super(agent);
      this.verbose = v;
      this.concurrentLocks = concurrentLocks;
   }

   public PStack(boolean v, boolean concurrentLocks) {
      this(v, concurrentLocks, null);
   }

   public PStack() {
      this(true, true, null);
   }

   public PStack(JVMDebugger d) {
      super(d);
   }

   public void run() {
      run(System.out);
   }

   public void run(PrintStream out) {
      Debugger dbg = getAgent().getDebugger();
      run(out, dbg);
   }

   public void run(PrintStream out, Debugger dbg) {
      CDebugger cdbg = dbg.getCDebugger();
      if (cdbg != null) {
         ConcurrentLocksPrinter concLocksPrinter = null;
         // compute and cache java Vframes.
         initJFrameCache();
         if (concurrentLocks) {
            concLocksPrinter = new ConcurrentLocksPrinter();
         }
         // print Java level deadlocks
         try {
            DeadlockDetector.print(out);
         } catch (Exception exp) {
            out.println("can't print deadlock information: " + exp.getMessage());
         }

         List<ThreadProxy> l = cdbg.getThreadList();
         if (l.isEmpty() && PlatformInfo.getOS().equals("darwin")) {
           // If the list is empty, we assume we attached to a process, and on OSX we can only
           // get the native thread list for core files.
           out.println("Not available for Mac OS X processes");
           return;
        }
         final boolean cdbgCanDemangle = cdbg.canDemangle();
         String fillerForAddress = " ".repeat(2 + 2 * (int) VM.getVM().getAddressSize()) + "\t";
         for (Iterator<ThreadProxy> itr = l.iterator() ; itr.hasNext();) {
            ThreadProxy th = itr.next();
            try {
               CFrame f = cdbg.topFrameForThread(th);
               out.print("----------------- ");
               out.print(th);
               out.println(" -----------------");
               JavaThread jthread = (JavaThread) proxyToThread.get(th);
               if (jthread != null) {
                  jthread.printThreadInfoOn(out);
               }
               while (f != null) {
                  ClosestSymbol sym = f.closestSymbolToPC();
                  Address pc = f.pc();
                  out.print(pc + "\t");
                  if (sym != null) {
                     String name = sym.getName();
                     if (cdbgCanDemangle) {
                        name = cdbg.demangle(name);
                     }
                     out.print(name);
                     long diff = sym.getOffset();
                     if (diff != 0L) {
                        out.print(" + 0x" + Long.toHexString(diff));
                     }
                     out.println();
                  } else {
                      // look for one or more java frames
                      String[] names = null;
                      // check interpreter frame
                      Interpreter interp = VM.getVM().getInterpreter();
                      if (interp.contains(pc)) {
                         names = getJavaNames(th, f.localVariableBase());
                         // print codelet name if we can't determine method
                         if (names == null || names.length == 0) {
                            out.print("<interpreter> ");
                            InterpreterCodelet ic = interp.getCodeletContaining(pc);
                            if (ic != null) {
                               String desc = ic.getDescription();
                               if (desc != null) out.print(desc);
                            }
                            out.println();
                         }
                      } else {
                         // look for known code blobs
                         CodeCache c = VM.getVM().getCodeCache();
                         if (c.contains(pc)) {
                            CodeBlob cb = c.findBlobUnsafe(pc);
                            if (cb.isNMethod()) {
                               if (cb.isNativeMethod()) {
                                  out.print(((CompiledMethod)cb).getMethod().externalNameAndSignature());
                                  long diff = pc.minus(cb.codeBegin());
                                  if (diff != 0L) {
                                    out.print(" + 0x" + Long.toHexString(diff));
                                  }
                                  out.println(" (Native method)");
                               } else {
                                  names = getJavaNames(th, f.localVariableBase());
                                  // just print compiled code, if can't determine method
                                  if (names == null || names.length == 0) {
                                    out.println("<Unknown compiled code>");
                                  }
                               }
                            } else if (cb.isBufferBlob()) {
                               out.println("<StubRoutines>");
                            } else if (cb.isRuntimeStub()) {
                               out.println("<RuntimeStub>");
                            } else if (cb.isDeoptimizationStub()) {
                               out.println("<DeoptimizationStub>");
                            } else if (cb.isUncommonTrapStub()) {
                               out.println("<UncommonTrap>");
                            } else if (cb.isExceptionStub()) {
                               out.println("<ExceptionStub>");
                            } else if (cb.isSafepointStub()) {
                               out.println("<SafepointStub>");
                            } else {
                               out.println("<Unknown code blob>");
                            }
                         } else {
                            printUnknown(out);
                         }
                      }
                      // print java frames, if any
                      if (names != null && names.length != 0) {
                         // print java frame(s)
                         for (int i = 0; i < names.length; i++) {
                             if (i > 0) {
                                 out.print(fillerForAddress);
                             }
                             out.println(names[i]);
                         }
                      }
                  }
                  f = f.sender(th);
               }
            } catch (Exception exp) {
               exp.printStackTrace();
               // continue, may be we can do a better job for other threads
            }
            if (concurrentLocks) {
               JavaThread jthread = (JavaThread) proxyToThread.get(th);
               if (jthread != null) {
                   concLocksPrinter.print(jthread, out);
               }
            }
         } // for threads
      } else {
          if (getDebugeeType() == DEBUGEE_REMOTE) {
              out.print(((RemoteDebuggerClient)dbg).execCommandOnServer("pstack", Map.of("concurrentLocks", concurrentLocks)));
          } else {
              out.println("not yet implemented (debugger does not support CDebugger)!");
          }
      }
   }

   public static void main(String[] args) throws Exception {
      PStack t = new PStack();
      t.execute(args);
   }

   // -- Internals only below this point
   private Map<ThreadProxy, JavaVFrame[]> jframeCache;
   private Map<ThreadProxy, JavaThread> proxyToThread;
   private PrintStream out;
   private boolean verbose;
   private boolean concurrentLocks;

   private void initJFrameCache() {
      // cache frames for subsequent reference
      jframeCache = new HashMap<>();
      proxyToThread = new HashMap<>();
      Threads threads = VM.getVM().getThreads();
      for (int i = 0; i < threads.getNumberOfThreads(); i++) {
         JavaThread cur = threads.getJavaThreadAt(i);
         List<JavaVFrame> tmp = new ArrayList<>(10);
         try {
            for (JavaVFrame vf = cur.getLastJavaVFrameDbg(); vf != null; vf = vf.javaSender()) {
               tmp.add(vf);
            }
         } catch (Exception exp) {
            // may be we may get frames for other threads, continue
            // after printing stack trace.
            exp.printStackTrace();
         }
         JavaVFrame[] jvframes = tmp.toArray(new JavaVFrame[0]);
         jframeCache.put(cur.getThreadProxy(), jvframes);
         proxyToThread.put(cur.getThreadProxy(), cur);
      }
   }

   private void printUnknown(PrintStream out) {
      out.println("\t????????");
   }

   private String[] getJavaNames(ThreadProxy th, Address fp) {
      if (fp == null) {
         return null;
      }
      JavaVFrame[] jvframes = (JavaVFrame[]) jframeCache.get(th);
      if (jvframes == null) return null; // not a java thread
      List<String> names = new ArrayList<>(10);
      for (int fCount = 0; fCount < jvframes.length; fCount++) {
         JavaVFrame vf = jvframes[fCount];
         Frame f = vf.getFrame();
         if (fp.equals(f.getFP())) {
            StringBuilder sb = new StringBuilder();
            Method method = vf.getMethod();
            // a special char to identify java frames in output
            sb.append("* ");
            sb.append(method.externalNameAndSignature());
            sb.append(" bci:").append(vf.getBCI());
            int lineNumber = method.getLineNumberFromBCI(vf.getBCI());
            if (lineNumber != -1) {
                sb.append(" line:").append(lineNumber);
            }

            if (verbose) {
               sb.append(" Method*:").append(method.getAddress());
            }

            if (vf.isCompiledFrame()) {
               sb.append(" (Compiled frame");
               if (vf.isDeoptimized()) {
                 sb.append(" [deoptimized]");
               }
            } else if (vf.isInterpretedFrame()) {
               sb.append(" (Interpreted frame");
            }
            if (vf.mayBeImpreciseDbg()) {
               sb.append("; information may be imprecise");
            }
            sb.append(")");
            names.add(sb.toString());
         }
      }
      String[] res = names.toArray(new String[0]);
      return res;
   }

   public void setVerbose(boolean verbose) {
       this.verbose = verbose;
   }

   public void setConcurrentLocks(boolean concurrentLocks) {
       this.concurrentLocks = concurrentLocks;
   }
}
