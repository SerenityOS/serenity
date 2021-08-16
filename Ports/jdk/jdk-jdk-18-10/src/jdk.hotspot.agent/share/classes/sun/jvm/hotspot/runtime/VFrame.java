/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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
import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.utilities.*;

public class VFrame {
  protected Frame       fr;
  protected RegisterMap regMap;
  protected JavaThread  thread;

  protected VFrame(Frame f, RegisterMap regMap, JavaThread thread) {
    this.regMap = (RegisterMap) regMap.clone();

    if (f != null) {
      // the frame is null if we create a deoptimizedVFrame from a vframeArray
      fr = (Frame) f.clone();
    }

    this.thread = thread;
  }

  /** Factory method for creating vframes. The "unsafe" flag turns off
      an assertion which the runtime system uses to ensure integrity,
      but which must not be applied in the debugging situation. The
      "mayBeImprecise" flag should be set to true for the case of the
      top frame in the debugging system (obtained via
      JavaThread.getCurrentFrameGuess()). */
  public static VFrame newVFrame(Frame f, RegisterMap regMap, JavaThread thread, boolean unsafe, boolean mayBeImprecise) {
    if (f.isInterpretedFrame()) {
      return new InterpretedVFrame(f, regMap, thread);
    }

    if (!VM.getVM().isCore()) {
      CodeBlob cb;
      if (unsafe) {
        cb = VM.getVM().getCodeCache().findBlobUnsafe(f.getPC());
      } else {
        cb = VM.getVM().getCodeCache().findBlob(f.getPC());
      }

      if (cb != null) {
        if (cb.isNMethod()) {
          NMethod nm = (NMethod) cb;
          // Compiled method (native stub or Java code)
          ScopeDesc scope = null;
          // FIXME: should revisit the check of isDebugging(); should not be necessary
          if (mayBeImprecise || VM.getVM().isDebugging()) {
            scope = nm.getScopeDescNearDbg(f.getPC());
          } else {
            scope = nm.getScopeDescAt(f.getPC());
          }
          return new CompiledVFrame(f, regMap, thread, scope, mayBeImprecise);
        }

        if (f.isRuntimeFrame()) {
          // This is a conversion frame or a Stub routine. Skip this frame and try again.
          RegisterMap tempMap = regMap.copy();
          Frame s = f.sender(tempMap);
          return newVFrame(s, tempMap, thread, unsafe, false);
        }
      }
    }

    // External frame
    return new ExternalVFrame(f, regMap, thread, mayBeImprecise);
  }

  /** Factory method for creating vframes. This is equivalent to
      calling the above version with the "unsafe" and "imprecise"
      flags set to false. */
  public static VFrame newVFrame(Frame f, RegisterMap regMap, JavaThread thread) {
    return newVFrame(f, regMap, thread, false, false);
  }

  /** Accessors */
  public Frame       getFrame()       { return fr;     }
  public RegisterMap getRegisterMap() { return regMap; }
  public JavaThread  getThread()      { return thread; }

  /** Returns the sender vframe */
  public VFrame sender() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(isTop(), "just checking");
    }
    return sender(false);
  }

  /** Returns the sender vframe; takes argument for debugging situation */
  public VFrame sender(boolean mayBeImprecise) {
    RegisterMap tempMap = (RegisterMap) getRegisterMap().clone();
    if (fr.isFirstFrame()) {
      return null;
    }
    Frame s = fr.realSender(tempMap);
    if (s == null) {
      return null;
    }
    if (s.isFirstFrame()) {
      return null;
    }
    return VFrame.newVFrame(s, tempMap, getThread(), VM.getVM().isDebugging(), mayBeImprecise);
  }

  /** Returns the next javaVFrame on the stack (skipping all other
      kinds of frames).  In the debugging situation, allows the
      "imprecise" flag to propagate up the stack. We must not assert
      that a ScopeDesc exists for the topmost compiled frame on the
      stack. */
  public JavaVFrame javaSender() {
    boolean imprecise = false;

    // Hack for debugging
    if (VM.getVM().isDebugging()) {
      if (!isJavaFrame()) {
        imprecise = mayBeImpreciseDbg();
      }
    }
    VFrame f = sender(imprecise);
    while (f != null) {
      if (f.isJavaFrame()) {
        return (JavaVFrame) f;
      }
      f = f.sender(imprecise);
    }
    return null;
  }

  /** Answers if the this is the top vframe in the frame, i.e., if the
      sender vframe is in the caller frame */
  public boolean isTop() {
    return true;
  }

  /** Returns top vframe within same frame (see isTop()) */
  public VFrame top() {
    VFrame vf = this;
    while (!vf.isTop()) {
      vf = vf.sender();
    }
    return vf;
  }

  /** Type testing operations */
  public boolean isEntryFrame()       { return false; }
  public boolean isJavaFrame()        { return false; }
  public boolean isInterpretedFrame() { return false; }
  public boolean isCompiledFrame()    { return false; }
  public boolean isDeoptimized()      { return false; }

  /** An indication of whether this VFrame is "precise" or a best
      guess. This is used in the debugging system to handle the top
      frame on the stack, which, since the system will in general not
      be at a safepoint, has to make some guesses about exactly where
      in the execution it is. Any debugger should indicate to the user
      that the information for this frame may not be 100% correct.
      FIXME: may need to move this up into VFrame instead of keeping
      it in CompiledVFrame. */
  public boolean mayBeImpreciseDbg()  { return false; }

  /** Printing operations */
  public void print() {
    printOn(System.out);
  }

  public void printOn(PrintStream tty) {
    if (VM.getVM().wizardMode()) {
      fr.printValueOn(tty);
    }
  }

  public void printValue() {
    printValueOn(System.out);
  }

  public void printValueOn(PrintStream tty) {
    printOn(tty);
  }
}
