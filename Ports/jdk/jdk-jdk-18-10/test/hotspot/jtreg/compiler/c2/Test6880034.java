/*
 * Copyright (c) 2009 SAP SE. All rights reserved.
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
 */

/**
 * @test
 * @bug 6880034
 * @summary SIGBUS during deoptimisation at a safepoint on 64bit-SPARC
 *
 * @run main/othervm -Xcomp -Xbatch
 *    -XX:+PrintCompilation
 *    -XX:CompileCommand=compileonly,compiler.c2.Test6880034::deopt_compiledframe_at_safepoint
 *    compiler.c2.Test6880034
 */

package compiler.c2;

// This test provokes a deoptimisation at a safepoint.
//
// It achieves this by compiling the method 'deopt_compiledframe_at_safepoint'
// before its first usage at a point in time when a call to the virtual method
// A::doSomething() from within 'deopt_compiledframe_at_safepoint' can be
// optimised to a static call because class A has no descendants.
//
// Later, when deopt_compiledframe_at_safepoint() is running, class B which
// extends A and overrides the virtual method "doSomething()", is loaded
// asynchronously in another thread.  This makes the compiled code of
// 'deopt_compiledframe_at_safepoint' invalid and triggers a deoptimisation of
// the frame where 'deopt_compiledframe_at_safepoint' is running in a
// loop.
//
// The deoptimisation leads to a SIGBUS on 64-bit server VMs on SPARC and to
// an incorrect result on 32-bit server VMs on SPARC due to a regression
// introduced by the change: "6420645: Create a vm that uses compressed oops
// for up to 32gb heapsizes"
// (http://hg.openjdk.java.net/jdk7/jdk7/hotspot/rev/ba764ed4b6f2).  Further
// investigation showed that change 6420645 is not really the root cause of
// this error but only reveals a problem with the float register encodings in
// sparc.ad which was hidden until now.
//
// Notice that for this test to fail in jtreg it is crucial that
// deopt_compiledframe_at_safepoint() runs in the main thread. Otherwise a
// crash in deopt_compiledframe_at_safepoint() will not be detected as a test
// failure by jtreg.
//
// Author: Volker H. Simonis

public class Test6880034 {
  static class A {
    public int doSomething() {
      return 0;
    }
  }

  static class B extends A {
    public B() {}
    // override 'A::doSomething()'
    public int doSomething() {
      return 1;
    }
  }

  static class G {
    public static volatile A a = new A();

    // Change 'a' to point to a 'B' object
    public static void setAtoB() {
      try {
        a =  (A) ClassLoader.
                getSystemClassLoader().
                loadClass("B").
                getConstructor(new Class[] {}).
                newInstance(new Object[] {});
      }
      catch (Exception e) {
        System.out.println(e);
      }
    }
  }

  public static volatile boolean is_in_loop = false;
  public static volatile boolean stop_while_loop = false;

  public static double deopt_compiledframe_at_safepoint() {
    // This will be an optimised static call to A::doSomething() until we load "B"
    int i = G.a.doSomething();

    // Need more than 16 'double' locals in this frame
    double local1 = 1;
    double local2 = 2;
    double local3 = 3;
    double local4 = 4;
    double local5 = 5;
    double local6 = 6;
    double local7 = 7;
    double local8 = 8;

    long k = 0;
    // Once we load "B", this method will be made 'not entrant' and deoptimised
    // at the safepoint which is at the end of this loop.
    while (!stop_while_loop) {
      if (k ==  1) local1 += i;
      if (k ==  2) local2 += i;
      if (k ==  3) local3 += i;
      if (k ==  4) local4 += i;
      if (k ==  5) local5 += i;
      if (k ==  6) local6 += i;
      if (k ==  7) local7 += i;
      if (k ==  8) local8 += i;

      // Tell the world that we're now running wild in the loop
      if (k++ == 20000) is_in_loop = true;
    }

    return
      local1 + local2 + local3 + local4 +
      local5 + local6 + local7 + local8 + i;
  }

  public static void main(String[] args) {

    // Just to resolve G before we compile deopt_compiledframe_at_safepoint()
    G g = new G();

    // Asynchronous thread which will eventually invalidate the code for
    // deopt_compiledframe_at_safepoint() and therefore triggering a
    // deoptimisation of that method.
    new Thread() {
      public void run() {
        while (!is_in_loop) {
          // Wait until the loop is running
        }
        // Load class 'B' asynchronously..
        G.setAtoB();
        // ..and stop the loop
        stop_while_loop = true;
      }
    }.start();

    // Run the loop in deopt_compiledframe_at_safepoint()
    double retVal = deopt_compiledframe_at_safepoint();

    System.out.println(retVal == 36 ? "OK" : "ERROR : " + retVal);
    if (retVal != 36) throw new RuntimeException();
  }
}
