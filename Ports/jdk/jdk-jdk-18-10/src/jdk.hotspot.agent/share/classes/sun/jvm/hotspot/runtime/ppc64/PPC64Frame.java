/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.runtime.ppc64;

import java.util.*;
import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.compiler.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

/** Specialization of and implementation of abstract methods of the
    Frame class for the ppc64 family of CPUs. */

public class PPC64Frame extends Frame {
  private static final boolean DEBUG;
  static {
    DEBUG = System.getProperty("sun.jvm.hotspot.runtime.ppc64.PPC64Frame.DEBUG") != null;
  }

  // All frames
  private static final int SENDER_SP_OFFSET           =  0;

  // Interpreter frames
  private static final int INTERPRETER_FRAME_SENDER_SP_OFFSET = -4;
  private static final int INTERPRETER_FRAME_LAST_SP_OFFSET = INTERPRETER_FRAME_SENDER_SP_OFFSET - 1;
  private static final int INTERPRETER_FRAME_MDX_OFFSET = INTERPRETER_FRAME_LAST_SP_OFFSET -1;
  private static final int INTERPRETER_FRAME_ESP_OFFSET = INTERPRETER_FRAME_MDX_OFFSET - 1;
  private static final int INTERPRETER_FRAME_BCX_OFFSET = INTERPRETER_FRAME_ESP_OFFSET - 1;
  private static final int INTERPRETER_FRAME_CACHE_OFFSET = INTERPRETER_FRAME_BCX_OFFSET - 1;
  private static final int INTERPRETER_FRAME_MONITORS_OFFSET = INTERPRETER_FRAME_CACHE_OFFSET - 1;
  private static final int INTERPRETER_FRAME_LOCALS_OFFSET = INTERPRETER_FRAME_MONITORS_OFFSET - 1;
  private static final int INTERPRETER_FRAME_MIRROR_OFFSET = INTERPRETER_FRAME_LOCALS_OFFSET - 1;
  private static final int INTERPRETER_FRAME_METHOD_OFFSET = INTERPRETER_FRAME_MIRROR_OFFSET - 1;
  private static final int INTERPRETER_FRAME_MONITOR_BLOCK_BOTTOM_OFFSET = INTERPRETER_FRAME_METHOD_OFFSET - 1;

  // Entry frames
  private static int ENTRY_FRAME_CALL_WRAPPER_OFFSET;

  static {
    VM.registerVMInitializedObserver(new Observer() {
      public void update(Observable o, Object data) {
        initialize(VM.getVM().getTypeDataBase());
      }
    });
  }

  private static synchronized void initialize(TypeDataBase db) {
    int entry_frame_locals_size = db.lookupIntConstant("frame::entry_frame_locals_size").intValue();
    int wordLength = (int) VM.getVM().getAddressSize();
    ENTRY_FRAME_CALL_WRAPPER_OFFSET = -entry_frame_locals_size/wordLength;
  }


  // an additional field beyond sp and pc:
  Address raw_fp; // frame pointer
  private Address raw_unextendedSP;

  private PPC64Frame() {
  }

  private void adjustForDeopt() {
    if ( pc != null) {
      // Look for a deopt pc and if it is deopted convert to original pc
      CodeBlob cb = VM.getVM().getCodeCache().findBlob(pc);
      if (cb != null && cb.isJavaMethod()) {
        NMethod nm = (NMethod) cb;
        if (pc.equals(nm.deoptHandlerBegin())) {
          if (Assert.ASSERTS_ENABLED) {
            Assert.that(this.getUnextendedSP() != null, "null SP in Java frame");
          }
          // adjust pc if frame is deoptimized.
          pc = this.getUnextendedSP().getAddressAt(nm.origPCOffset());
          deoptimized = true;
        }
      }
    }
  }

  public PPC64Frame(Address raw_sp, Address raw_fp, Address pc) {
    this.raw_sp = raw_sp;
    this.raw_unextendedSP = raw_sp;
    if (raw_fp == null) {
      this.raw_fp = raw_sp.getAddressAt(0);
    } else {
      this.raw_fp = raw_fp;
    }
    if (pc == null) {
      this.pc = raw_sp.getAddressAt(2 * VM.getVM().getAddressSize());
    } else {
      this.pc = pc;
    }
    adjustUnextendedSP();

    // Frame must be fully constructed before this call
    adjustForDeopt();

    if (DEBUG) {
      System.out.println("PPC64Frame(sp, fp, pc): " + this);
      dumpStack();
    }
  }

  public PPC64Frame(Address raw_sp, Address raw_fp) {
    this.raw_sp = raw_sp;
    this.raw_unextendedSP = raw_sp;
    if (raw_fp == null) {
      this.raw_fp = raw_sp.getAddressAt(0);
    } else {
      this.raw_fp = raw_fp;
    }
    this.pc = raw_sp.getAddressAt(2 * VM.getVM().getAddressSize());
    adjustUnextendedSP();

    // Frame must be fully constructed before this call
    adjustForDeopt();

    if (DEBUG) {
      System.out.println("PPC64Frame(sp, fp): " + this);
      dumpStack();
    }
  }

  public PPC64Frame(Address raw_sp, Address raw_unextendedSp, Address raw_fp, Address pc) {
    this.raw_sp = raw_sp;
    this.raw_unextendedSP = raw_unextendedSp;
    if (raw_fp == null) {
      this.raw_fp = raw_sp.getAddressAt(0);
    } else {
      this.raw_fp = raw_fp;
    }
    if (pc == null) {
      this.pc = raw_sp.getAddressAt(2 * VM.getVM().getAddressSize());
    } else {
      this.pc = pc;
    }
    adjustUnextendedSP();

    // Frame must be fully constructed before this call
    adjustForDeopt();

    if (DEBUG) {
      System.out.println("PPC64Frame(sp, unextendedSP, fp, pc): " + this);
      dumpStack();
    }

  }

  public Object clone() {
    PPC64Frame frame = new PPC64Frame();
    frame.raw_sp = raw_sp;
    frame.raw_unextendedSP = raw_unextendedSP;
    frame.raw_fp = raw_fp;
    frame.pc = pc;
    frame.deoptimized = deoptimized;
    return frame;
  }

  public boolean equals(Object arg) {
    if (arg == null) {
      return false;
    }

    if (!(arg instanceof PPC64Frame)) {
      return false;
    }

    PPC64Frame other = (PPC64Frame) arg;

    return (AddressOps.equal(getSP(), other.getSP()) &&
        AddressOps.equal(getUnextendedSP(), other.getUnextendedSP()) &&
        AddressOps.equal(getFP(), other.getFP()) &&
        AddressOps.equal(getPC(), other.getPC()));
  }

  public int hashCode() {
    if (raw_sp == null) {
      return 0;
    }

    return raw_sp.hashCode();
  }

  public String toString() {
    return "sp: " + (getSP() == null ? "null" : getSP().toString()) +
        ", unextendedSP: " + (getUnextendedSP() == null ? "null" : getUnextendedSP().toString()) +
        ", fp: " + (getFP() == null ? "null" : getFP().toString()) +
        ", pc: " + (pc == null ? "null" : pc.toString());
  }

  // accessors for the instance variables
  public Address getFP() { return raw_fp; }
  public Address getSP() { return raw_sp; }
  public Address getID() { return raw_sp; }

  // FIXME: not implemented yet (should be done for Solaris/PPC64)
  public boolean isSignalHandlerFrameDbg() { return false; }
  public int     getSignalNumberDbg()      { return 0;     }
  public String  getSignalNameDbg()        { return null;  }

  public boolean isInterpretedFrameValid() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(isInterpretedFrame(), "Not an interpreted frame");
    }

    // These are reasonable sanity checks
    if (getFP() == null || getFP().andWithMask(0x3) != null) {
      return false;
    }

    if (getSP() == null || getSP().andWithMask(0x3) != null) {
      return false;
    }

    // These are hacks to keep us out of trouble.
    // The problem with these is that they mask other problems
    if (getFP().lessThanOrEqual(getSP())) {
      // this attempts to deal with unsigned comparison above
      return false;
    }

    if (getFP().minus(getSP()) > 4096 * VM.getVM().getAddressSize()) {
      // stack frames shouldn't be large.
      return false;
    }

    return true;
  }

  // FIXME: not applicable in current system
  //  void    patch_pc(Thread* thread, address pc);

  public Frame sender(RegisterMap regMap, CodeBlob cb) {
    PPC64RegisterMap map = (PPC64RegisterMap) regMap;

    if (Assert.ASSERTS_ENABLED) {
      Assert.that(map != null, "map must be set");
    }

    // Default is we done have to follow them. The sender_for_xxx will
    // update it accordingly
    map.setIncludeArgumentOops(false);

    if (isEntryFrame()) return senderForEntryFrame(map);
    if (isInterpretedFrame()) return senderForInterpreterFrame(map);

    if(cb == null) {
      cb = VM.getVM().getCodeCache().findBlob(getPC());
    } else {
      if (Assert.ASSERTS_ENABLED) {
        Assert.that(cb.equals(VM.getVM().getCodeCache().findBlob(getPC())), "Must be the same");
      }
    }

    if (cb != null) {
      return senderForCompiledFrame(map, cb);
    }

    // Must be native-compiled frame, i.e. the marshaling code for native
    // methods that exists in the core system.
    return new PPC64Frame(getSenderSP(), getLink(), getSenderPC());
  }

  private Frame senderForEntryFrame(PPC64RegisterMap map) {
    if (DEBUG) {
      System.out.println("senderForEntryFrame");
    }
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(map != null, "map must be set");
    }
    // Java frame called from C; skip all C frames and return top C
    // frame of that chunk as the sender
    PPC64JavaCallWrapper jcw = (PPC64JavaCallWrapper) getEntryFrameCallWrapper();
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(!entryFrameIsFirst(), "next Java fp must be non zero");
      Assert.that(jcw.getLastJavaSP().greaterThan(getSP()), "must be above this frame on stack");
    }
    PPC64Frame fr;
    if (jcw.getLastJavaPC() != null) {
      fr = new PPC64Frame(jcw.getLastJavaSP(), jcw.getLastJavaFP(), jcw.getLastJavaPC());
    } else {
      fr = new PPC64Frame(jcw.getLastJavaSP(), jcw.getLastJavaFP());
    }
    map.clear();
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(map.getIncludeArgumentOops(), "should be set by clear");
    }
    return fr;
  }

  //------------------------------------------------------------------------------
  // frame::adjust_unextended_sp
  private void adjustUnextendedSP() {
    raw_unextendedSP = getFP();
  }
  private Frame senderForInterpreterFrame(PPC64RegisterMap map) {
    if (DEBUG) {
      System.out.println("senderForInterpreterFrame");
    }
    Address unextendedSP = addressOfStackSlot(INTERPRETER_FRAME_SENDER_SP_OFFSET).getAddressAt(0);
    Address sp = getSenderSP();

    return new PPC64Frame(sp, unextendedSP, getLink(), getSenderPC());
  }


  private Frame senderForCompiledFrame(PPC64RegisterMap map, CodeBlob cb) {
    if (DEBUG) {
      System.out.println("senderForCompiledFrame");
    }

    //
    // NOTE: some of this code is (unfortunately) duplicated in PPC64CurrentFrameGuess
    //

    if (Assert.ASSERTS_ENABLED) {
      Assert.that(map != null, "map must be set");
    }

    // frame owned by optimizing compiler
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(cb.getFrameSize() >= 0, "must have non-zero frame size");
    }
    Address senderSP = getSenderSP();

    Address senderPC = getSenderPC();

    if (map.getUpdateMap()) {
      // Tell GC to use argument oopmaps for some runtime stubs that need it.
      // For C1, the runtime stub might not have oop maps, so set this flag
      // outside of update_register_map.
      map.setIncludeArgumentOops(cb.callerMustGCArguments());

      if (cb.getOopMaps() != null) {
        ImmutableOopMapSet.updateRegisterMap(this, cb, map, true);
      }
    }

    return new PPC64Frame(senderSP, getLink(), senderPC);
  }

  protected boolean hasSenderPD() {
    // FIXME
    return true;
  }

  public long frameSize() {
    return (getSenderSP().minus(getSP()) / VM.getVM().getAddressSize());
  }

  public Address getLink() {
    return getSenderSP().getAddressAt(0);
  }

  public Address getUnextendedSP() { return raw_unextendedSP; }

  // Return address:
  public Address getSenderPC()     { return getSenderSP().getAddressAt(2 * VM.getVM().getAddressSize()); }

  public Address getSenderSP()     { return getFP(); }
  public Address addressOfInterpreterFrameLocals() {
    return addressOfStackSlot(INTERPRETER_FRAME_LOCALS_OFFSET);
  }

  private Address addressOfInterpreterFrameBCX() {
    return addressOfStackSlot(INTERPRETER_FRAME_BCX_OFFSET);
  }

  public int getInterpreterFrameBCI() {
    // FIXME: this is not atomic with respect to GC and is unsuitable
    // for use in a non-debugging, or reflective, system. Need to
    // figure out how to express this.
    Address bcp = addressOfInterpreterFrameBCX().getAddressAt(0);
    Address methodHandle = addressOfInterpreterFrameMethod().getAddressAt(0);
    Method method = (Method)Metadata.instantiateWrapperFor(methodHandle);
    return bcpToBci(bcp, method);
  }

  public Address addressOfInterpreterFrameMDX() {
    return addressOfStackSlot(INTERPRETER_FRAME_MDX_OFFSET);
  }

  // FIXME
  //inline int frame::interpreter_frame_monitor_size() {
  //  return BasicObjectLock::size();
  //}

  // expression stack
  // (the max_stack arguments are used by the GC; see class FrameClosure)

  public Address addressOfInterpreterFrameExpressionStack() {
    Address monitorEnd = interpreterFrameMonitorEnd().address();
    return monitorEnd.addOffsetTo(-1 * VM.getVM().getAddressSize());
  }

  public int getInterpreterFrameExpressionStackDirection() { return -1; }

  // top of expression stack
  public Address addressOfInterpreterFrameTOS() {
    return getSP();
  }

  /** Expression stack from top down */
  public Address addressOfInterpreterFrameTOSAt(int slot) {
    return addressOfInterpreterFrameTOS().addOffsetTo(slot * VM.getVM().getAddressSize());
  }

  public Address getInterpreterFrameSenderSP() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(isInterpretedFrame(), "interpreted frame expected");
    }
    return addressOfStackSlot(INTERPRETER_FRAME_SENDER_SP_OFFSET).getAddressAt(0);
  }

  // Monitors
  public BasicObjectLock interpreterFrameMonitorBegin() {
    return new BasicObjectLock(addressOfStackSlot(INTERPRETER_FRAME_MONITOR_BLOCK_BOTTOM_OFFSET));
  }

  public BasicObjectLock interpreterFrameMonitorEnd() {
    Address result = addressOfStackSlot(INTERPRETER_FRAME_MONITORS_OFFSET).getAddressAt(0);
    if (Assert.ASSERTS_ENABLED) {
      // make sure the pointer points inside the frame
      Assert.that(AddressOps.gt(getFP(), result), "result must <  than frame pointer");
      Assert.that(AddressOps.lte(getSP(), result), "result must >= than stack pointer");
    }
    return new BasicObjectLock(result);
  }

  public int interpreterFrameMonitorSize() {
    return BasicObjectLock.size();
  }

  // Method
  public Address addressOfInterpreterFrameMethod() {
    return addressOfStackSlot(INTERPRETER_FRAME_METHOD_OFFSET);
  }

  // Constant pool cache
  public Address addressOfInterpreterFrameCPCache() {
    return addressOfStackSlot(INTERPRETER_FRAME_CACHE_OFFSET);
  }

  // Entry frames
  public JavaCallWrapper getEntryFrameCallWrapper() {
    return new PPC64JavaCallWrapper(addressOfStackSlot(ENTRY_FRAME_CALL_WRAPPER_OFFSET).getAddressAt(0));
  }

  protected Address addressOfSavedOopResult() {
    // offset is 2 for compiler2 and 3 for compiler1
    return getSP().addOffsetTo((VM.getVM().isClientCompiler() ? 2 : 3) *
        VM.getVM().getAddressSize());
  }

  protected Address addressOfSavedReceiver() {
    return getSP().addOffsetTo(-4 * VM.getVM().getAddressSize());
  }

  private void dumpStack() {
    if (getFP() != null) {
      for (Address addr = getSP().addOffsetTo(-5 * VM.getVM().getAddressSize());
          AddressOps.lte(addr, getFP().addOffsetTo(5 * VM.getVM().getAddressSize()));
          addr = addr.addOffsetTo(VM.getVM().getAddressSize())) {
        System.out.println(addr + ": " + addr.getAddressAt(0));
      }
    } else {
      for (Address addr = getSP().addOffsetTo(-5 * VM.getVM().getAddressSize());
          AddressOps.lte(addr, getSP().addOffsetTo(20 * VM.getVM().getAddressSize()));
          addr = addr.addOffsetTo(VM.getVM().getAddressSize())) {
        System.out.println(addr + ": " + addr.getAddressAt(0));
      }
    }
  }
}
