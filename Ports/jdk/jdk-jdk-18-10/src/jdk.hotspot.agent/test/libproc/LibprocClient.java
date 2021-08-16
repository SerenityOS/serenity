/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.tools.*;
import sun.jvm.hotspot.utilities.*;

/**
   We don't run any of the "standard" SA command line tools for sanity
   check. This is because the standard tools print addresses in hex
   which could change legally. Also, textual comparison of output may
   not match because of other reasons as well. This tool checks
   validity of threads and frames logically. This class has reference
   frame names from "known" threads. The debuggee is assumed to run
   "LibprocTest.java".
*/

public class LibprocClient extends Tool {

   public void run() {
      // try to get VM version and check
      String version = VM.getVM().getVMRelease();
      Assert.that(version.startsWith("1.5"), "1.5 expected");

      // try getting threads
      Threads threads = VM.getVM().getThreads();
      boolean mainTested = false;

      // check frames of each thread
      for (JavaThread cur = threads.first(); cur != null; cur = cur.next()) {
         if (cur.isJavaThread()) {
             String name = cur.getThreadName();
             // testing of basic frame walking for all threads
             for (JavaVFrame vf = getLastJavaVFrame(cur); vf != null; vf = vf.javaSender()) {
                checkFrame(vf);
             }

             // special testing for "known" threads. For now, only "main" thread.
             if (name.equals("main")) {
                checkMainThread(cur);
                mainTested = true;
             }
         }
      }
      Assert.that(mainTested, "main thread missing");
   }

   public static void main(String[] args) {
      try {
         LibprocClient lc = new LibprocClient();
         lc.start(args);
         lc.getAgent().detach();
         System.out.println("\nPASSED\n");
      } catch (Exception exp) {
         System.out.println("\nFAILED\n");
         exp.printStackTrace();
      }
   }

   // -- Internals only below this point
   private static JavaVFrame getLastJavaVFrame(JavaThread cur) {
      RegisterMap regMap = cur.newRegisterMap(true);
      Frame f = cur.getCurrentFrameGuess();
      if (f == null) {
         System.err.println(" (Unable to get a top most frame)");
         return null;
      }
      VFrame vf = VFrame.newVFrame(f, regMap, cur, true, true);
      if (vf == null) {
         System.err.println(" (Unable to create vframe for topmost frame guess)");
         return null;
      }
      if (vf.isJavaFrame()) {
         return (JavaVFrame) vf;
      }
      return (JavaVFrame) vf.javaSender();
   }

   private void checkMethodSignature(Symbol sig) {
      SignatureIterator itr = new SignatureIterator(sig) {
                                  public void doBool  () {}
                                  public void doChar  () {}
                                  public void doFloat () {}
                                  public void doDouble() {}
                                  public void doByte  () {}
                                  public void doShort () {}
                                  public void doInt   () {}
                                  public void doLong  () {}
                                  public void doVoid  () {}
                                  public void doObject(int begin, int end) {}
                                  public void doArray (int begin, int end) {}
                              };
      // this will throw RuntimeException for any invalid item in signature.
      itr.iterate();
   }

   private void checkBCI(Method m, int bci) {
      if (! m.isNative()) {
         byte[] buf = m.getByteCode();
         Assert.that(bci >= 0 && bci < buf.length, "invalid bci, not in code range");
         if (m.hasLineNumberTable()) {
           int lineNum = m.getLineNumberFromBCI(bci);
           Assert.that(lineNum >= 0, "expecting non-negative line number");
         }
      }
   }

   private void checkMethodHolder(Method method) {
      Klass klass = method.getMethodHolder();
      Assert.that(klass != null, "expecting non-null instance klass");
   }

   private void checkFrame(JavaVFrame vf) {
      Method method = vf.getMethod();
      Assert.that(method != null, "expecting a non-null method here");
      Assert.that(method.getName() != null, "expecting non-null method name");
      checkMethodHolder(method);
      checkMethodSignature(method.getSignature());
      checkBCI(method, vf.getBCI());
   }

   // from the test case LibprocTest.java - in the main thread we
   // should see frames as below
   private static String[] mainThreadMethods = new String[] {
                             "java.lang.Object.wait(long)",
                             "java.lang.Object.wait()",
                             "LibprocTest.main(java.lang.String[])"
                          };

   private void checkMainThread(JavaThread thread) {
      checkFrames(thread, mainThreadMethods);
   }

   private void checkFrames(JavaThread thread, String[] expectedMethodNames) {
      int i = 0;
      for (JavaVFrame vf = getLastJavaVFrame(thread); vf != null; vf = vf.javaSender(), i++) {
         Method m = vf.getMethod();
         Assert.that(m.externalNameAndSignature().equals(expectedMethodNames[i]),
                     "expected frame missing");
      }
   }
}
