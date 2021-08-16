/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.compiler.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.interpreter.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

/** <P> A frame represents a physical stack frame (an activation).
    Frames can be C or Java frames, and the Java frames can be
    interpreted or compiled. In contrast, vframes represent
    source-level activations, so that one physical frame can
    correspond to multiple source level frames because of inlining.
    </P>

    <P> NOTE that this is not a VMObject and does not wrap an Address
    -- this is an actual port of the VM's Frame code to Java. </P>

    <P> NOTE also that this is incomplete -- just trying to get
    reading of interpreted frames working for now, so all non-core and
    setter methods are removed for now. (FIXME) </P> */

public abstract class Frame implements Cloneable {
  /** A raw stack pointer. The accessor getSP() will return a real (usable)
      stack pointer (e.g. from Thread::last_Java_sp) */
  protected Address raw_sp;

  /** Program counter (the next instruction after the call) */
  protected Address pc;
  protected boolean deoptimized;

  public Frame() {
    deoptimized = false;
  }

  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  /** Size of ConstMethod for computing BCI from BCP (FIXME: hack) */
  private static long    ConstMethodSize;

  private static int pcReturnOffset;

  public static int pcReturnOffset() {
    return pcReturnOffset;
  }

  private static synchronized void initialize(TypeDataBase db) {
    Type ConstMethodType = db.lookupType("ConstMethod");
    // FIXME: not sure whether alignment here is correct or how to
    // force it (round up to address size?)
    ConstMethodSize = ConstMethodType.getSize();

    pcReturnOffset = db.lookupIntConstant("frame::pc_return_offset").intValue();
  }

  protected int bcpToBci(Address bcp, ConstMethod cm) {
    // bcp will be null for interpreter native methods
    // in addition depending on where we catch the system the value can
    // be a bcp or a bci.
    if (bcp == null) return 0;
    long bci = bcp.minus(null);
    if (bci >= 0 && bci < cm.getCodeSize()) return (int) bci;
    return (int) (bcp.minus(cm.getAddress()) - ConstMethodSize);
  }

  protected int bcpToBci(Address bcp, Method m) {
    return bcpToBci(bcp, m.getConstMethod());
  }

  public abstract Object clone();

  // Accessors

  /** pc: Returns the pc at which this frame will continue normally.
      It must point at the beginning of the next instruction to
      execute. */
  public Address getPC()              { return pc; }
  public void    setPC(Address newpc) { pc = newpc; }
  public boolean isDeoptimized()      { return deoptimized; }

  public CodeBlob cb() {
    return VM.getVM().getCodeCache().findBlob(getPC());
  }

  public abstract Address getSP();
  public abstract Address getID();
  public abstract Address getFP();

  /** testers -- platform dependent */
  public abstract boolean equals(Object arg);

  /** type testers */
  public boolean isInterpretedFrame()           { return VM.getVM().getInterpreter().contains(getPC()); }
  public boolean isJavaFrame() {
    if (isInterpretedFrame()) return true;
    if (!VM.getVM().isCore()) {
      if (isCompiledFrame())    return true;
    }
    return false;
  }

  /** Java frame called from C? */
  public boolean isEntryFrame()                 { return VM.getVM().getStubRoutines().returnsToCallStub(getPC()); }
  public boolean isNativeFrame() {
    if (!VM.getVM().isCore()) {
      CodeBlob cb = VM.getVM().getCodeCache().findBlob(getPC());
      return (cb != null && cb.isNativeMethod());
    } else {
      return false;
    }
  }

  public boolean isCompiledFrame() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(!VM.getVM().isCore(), "noncore builds only");
    }
    CodeBlob cb = VM.getVM().getCodeCache().findBlob(getPC());
    return (cb != null && cb.isJavaMethod());
  }

  public boolean isRuntimeFrame() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(!VM.getVM().isCore(), "noncore builds only");
    }
    CodeBlob cb = VM.getVM().getCodeCache().findBlob(getPC());
    if (cb == null) {
      return false;
    }
    if (cb.isRuntimeStub()) return true;
    else return false;
  }

  /** oldest frame? (has no sender) FIXME: this is modified from the
      C++ code to handle the debugging situation where we try to
      traverse the stack for, for example, the signal thread, and
      don't find any valid Java frames. Would really like to put the
      second half of the conditional in some sort of debugging-only if
      statement. */
  // *** FIXME: THE CALL TO isJavaFrame() IS WAY TOO EXPENSIVE!!!!! ***
  public boolean isFirstFrame()                 { return ((isEntryFrame() && entryFrameIsFirst()) ||
                                                          (!isJavaFrame() && !hasSenderPD()));       }
  /** same for Java frame */
  public boolean isFirstJavaFrame()             { throw new RuntimeException("not yet implemented"); }

  /** This is an addition for debugging purposes on platforms which
      have the notion of signals. */
  public abstract boolean isSignalHandlerFrameDbg();

  /** If this is a signal handler frame (again, on a platform with a
      notion of signals), get the signal number. */
  public abstract int getSignalNumberDbg();

  /** If this is a signal handler frame (again, on a platform with a
      notion of signals), get the name of the signal. */
  public abstract String getSignalNameDbg();

  /** performs sanity checks on interpreted frames. */
  public abstract boolean isInterpretedFrameValid();

  /** tells whether this frame is marked for deoptimization */
  public boolean shouldBeDeoptimized()          { throw new RuntimeException("not yet implemented"); }

  /** tells whether this frame can be deoptimized */
  public boolean canBeDeoptimized()             { throw new RuntimeException("not yet implemented"); }

  /** returns the sending frame */
  public abstract Frame sender(RegisterMap map, CodeBlob nm);

  /** equivalent to sender(map, null) */
  public Frame sender(RegisterMap map)          { return sender(map, null); }

  /** returns the sender, but skips conversion frames */
  public Frame realSender(RegisterMap map) {
    if (!VM.getVM().isCore()) {
      Frame result = sender(map);
      while (result.isRuntimeFrame()) {
        result = result.sender(map);
      }
      return result;
    } else {
      return sender(map);
    }
  }

  /** Platform-dependent query indicating whether this frame has a
      sender. Should return true if it is possible to call sender() at
      all on this frame. (This is currently only needed for the
      debugging system, if a stack trace is attempted for a Java
      thread which has no Java frames, i.e., the signal thread; we
      have to know to stop traversal at the bottom frame.) */
  protected abstract boolean hasSenderPD();

  //--------------------------------------------------------------------------------
  // All frames:
  // A low-level interface for vframes:

  /** Returns the address of the requested "slot" on the stack. Slots
      are as wide as addresses, so are 32 bits wide on a 32-bit
      machine and 64 bits wide on a 64-bit machine. */
  public Address   addressOfStackSlot(int slot)              { return getFP().addOffsetTo(slot * VM.getVM().getAddressSize()); }

  /** Fetches the OopHandle at the requested slot */
  public OopHandle getOopHandleAt(int slot)                  { return addressOfStackSlot(slot).getOopHandleAt(0);              }
  /** Fetches the OopHandle at the slot, adjusted for compiler frames */
  // FIXME: looks like this is only used for compiled frames
  //  public OopHandle getOopHandleAtAdjusted(MethodOop method, int slot) { return addressOfStackSlot(slot).getOopHandleAt(0); }
  // FIXME: Not yet implementable
  //  public void  setOopHandleAt(int slot, OopHandle value) { addressOfStackSlot(slot).setOopHandleAt(0, value);              }

  /** Fetches the (Java) int at the requested slot */
  public int       getIntAt(int slot)                        { return addressOfStackSlot(slot).getJIntAt(0);                   }
  // FIXME: Not yet implementable
  // public void setIntAt(int slot, int value)               { addressOfStackSlot(slot).setJIntAt(0, value);                   }

  /** returns the frame size in stack slots */
  public abstract long frameSize();

  /** Link (i.e., the pointer to the previous frame) */
  public abstract Address getLink();
  //  public abstract void    setLink(Address addr);

  /** Return address */
  public abstract Address getSenderPC();
  // FIXME: currently unimplementable
  //  public abstract void    setSenderPC(Address addr);

  /** The frame's original SP, before any extension by an interpreted
      callee; used for packing debug info into vframeArray objects and
      vframeArray lookup. */
  public abstract Address getUnextendedSP();

  /** Returns the stack pointer of the calling frame */
  public abstract Address getSenderSP();

  //--------------------------------------------------------------------------------
  // Interpreter frames:
  //

  public abstract Address addressOfInterpreterFrameLocals();

  public Address addressOfInterpreterFrameLocal(int slot) {
    return addressOfInterpreterFrameLocals().getAddressAt(0).addOffsetTo(-slot * VM.getVM().getAddressSize());
  }

  // FIXME: not yet implementable
  //  void interpreter_frame_set_locals(intptr_t* locs);

  // NOTE that the accessor "addressOfInterpreterFrameBCX" has
  // necessarily been eliminated. The byte code pointer is inherently
  // an interior pointer to a Method (the bytecodes follow the
  // Method data structure) and therefore acquisition of it in
  // this system can not be allowed. All accesses to interpreter frame
  // byte codes are via the byte code index (BCI).

  /** Byte code index. In the underlying frame, what is actually
      stored is a byte code pointer (BCP), which is converted to a BCI
      and back by the GC when methods are moved. In this system,
      interior pointers are not allowed, so we must make the access to
      the interpreter frame's BCI atomic with respect to GC. This may
      mean implementation with an underlying call through native code
      into the VM or a magic sequence in the compiler. (FIXME) */
  public abstract int     getInterpreterFrameBCI();
  // FIXME: not yet implementable
  // public abstract void setInterpreterFrameBCI(int bci);

  // FIXME: elided for now
  //  public abstract Address addressOfInterpreterCalleeReceiver(Symbol signature);

  /** Find receiver for an invoke when arguments are just pushed on
      stack (i.e., callee stack-frame is not setup) */
  // FIXME: elided for now
  //  public OopHandle getInterpreterCalleeReceiver(SymbolOop signature) { return addressOfInterpreterCalleeReceiver(signature).getOopHandleAt(0); }

  //--------------------------------------------------------------------------------
  // Expression stack (may go up or down, direction == 1 or -1)
  //

  public abstract Address addressOfInterpreterFrameExpressionStack();
  public abstract int     getInterpreterFrameExpressionStackDirection();
  public Address addressOfInterpreterFrameExpressionStackSlot(int slot) {
    return addressOfInterpreterFrameExpressionStack().addOffsetTo(-slot * VM.getVM().getAddressSize());
  }

  /** Top of expression stack */
  public abstract Address addressOfInterpreterFrameTOS();

  /** Expression stack from top down */
  public abstract Address addressOfInterpreterFrameTOSAt(int slot);

  /** FIXME: is this portable? */
  public int getInterpreterFrameExpressionStackSize() {
    return (int) (1 + (getInterpreterFrameExpressionStackDirection() *
                       (addressOfInterpreterFrameTOS().minus(addressOfInterpreterFrameExpressionStack()))));
  }

  public abstract Address getInterpreterFrameSenderSP();
  // FIXME: not yet implementable
  //  public abstract void    setInterpreterFrameSenderSP(Address senderSP);

  //--------------------------------------------------------------------------------
  // BasicObjectLocks:
  //

  public abstract BasicObjectLock interpreterFrameMonitorBegin();
  public abstract BasicObjectLock interpreterFrameMonitorEnd();
  /** NOTE: this returns a size in BYTES in this system! */
  public abstract int     interpreterFrameMonitorSize();
  public          BasicObjectLock nextMonitorInInterpreterFrame(BasicObjectLock cur) {
    return new BasicObjectLock(cur.address().addOffsetTo(interpreterFrameMonitorSize()));
  }
  public          BasicObjectLock previousMonitorInInterpreterFrame(BasicObjectLock cur) {
    return new BasicObjectLock(cur.address().addOffsetTo(-1 * interpreterFrameMonitorSize()));
  }

  // interpreter_frame_monitor_begin is higher in memory than interpreter_frame_monitor_end
  // Interpreter_frame_monitor_begin points to one element beyond the oldest one,
  // interpreter_frame_monitor_end   points to the youngest one, or if there are none,
  //                                 it points to one beyond where the first element will be.
  // interpreter_frame_monitor_size  reports the allocation size of a monitor in the interpreter stack.
  //                                 this value is >= BasicObjectLock::size(), and may be rounded up

  // FIXME: avoiding implementing this for now if possible
  //  public void interpreter_frame_set_monitor_end(BasicObjectLock* value);
  //  public void interpreter_frame_verify_monitor(BasicObjectLock* value) const;
  //--------------------------------------------------------------------------------
  // Method and constant pool cache:
  //

  /** Current method */
  public abstract Address  addressOfInterpreterFrameMethod();

  /** Current method */
  public Method            getInterpreterFrameMethod() {
    return (Method)Metadata.instantiateWrapperFor(addressOfInterpreterFrameMethod().getAddressAt(0));
  }

  /** Current method */
  // FIXME: not yet implementable
  //  public void          setInterpreterFrameMethod(Method method);

  /** Constant pool cache */
  public abstract Address  addressOfInterpreterFrameCPCache();
  /** Constant pool cache */
  public ConstantPoolCache getInterpreterFrameCPCache() {
    return (ConstantPoolCache) Metadata.instantiateWrapperFor(addressOfInterpreterFrameCPCache().getAddressAt(0));
  }

  //--------------------------------------------------------------------------------
  // Entry frames:
  //

  public abstract JavaCallWrapper getEntryFrameCallWrapper();

  // FIXME: add
  //  inline intptr_t* entry_frame_argument_at(int offset) const;


  /** Tells whether there is another chunk of Delta stack above */
  public boolean entryFrameIsFirst()            { return (getEntryFrameCallWrapper().getLastJavaSP() == null); }

  //--------------------------------------------------------------------------------
  // Safepoints:
  //

  protected abstract Address addressOfSavedOopResult();
  protected abstract Address addressOfSavedReceiver();

  public OopHandle getSavedOopResult() {
    return addressOfSavedOopResult().getOopHandleAt(0);
  }

  // FIXME: not yet implementable
  //  public void      setSavedOopResult(OopHandle obj);

  public OopHandle getSavedReceiver() {
    return addressOfSavedReceiver().getOopHandleAt(0);
  }

  // FIXME: not yet implementable
  //  public void      setSavedReceiver(OopHandle obj);

  //--------------------------------------------------------------------------------
  // Oop traversals:
  //

  public void oopsInterpretedArgumentsDo(Symbol signature, boolean isStatic, AddressVisitor f) {
    ArgumentOopFinder finder = new ArgumentOopFinder(signature, isStatic, this, f);
    finder.oopsDo();
  }

  /** Conversion from an VMReg::Name to physical stack location */
  public Address oopMapRegToLocation(VMReg reg, RegisterMap regMap) {
    VMReg stack0 = VM.getVM().getVMRegImplInfo().getStack0();
    if (reg.lessThan(stack0)) {
      // If it is passed in a register, it got spilled in the stub frame.
      return regMap.getLocation(reg);
    } else {
      long spOffset = reg.reg2Stack() * VM.getVM().getVMRegImplInfo().getStackSlotSize();
      return getUnextendedSP().addOffsetTo(spOffset);
    }
  }

  public void oopsDo(AddressVisitor oopVisitor, RegisterMap map) {
    if (isInterpretedFrame()) {
      oopsInterpretedDo(oopVisitor, map);
    } else if (isEntryFrame()) {
      oopsEntryDo(oopVisitor, map);
    } else if (VM.getVM().getCodeCache().contains(getPC())) {
      oopsCodeBlobDo(oopVisitor, map);
    } else {
      Assert.that(false, "should not reach here");
    }
  }

  //--------------------------------------------------------------------------------
  // Printing code
  //

  public void printValue() {
    printValueOn(System.out);
  }

  public void printValueOn(PrintStream tty) {
    //    FIXME;
  }

  public void print() {
    printOn(System.out);
  }

  public void printOn(PrintStream tty) {
    //    FIXME;
  }

  public void interpreterFramePrintOn(PrintStream tty) {
    //    FIXME;
  }

  //--------------------------------------------------------------------------------
  // Get/set typed locals from a frame.
  // Respects platform dependent word-ordering.
  //
  // FIXME: avoiding implementing this for now if possible
  //
  // Currently these work only for interpreted frames.
  // Todo: make these work for compiled frames.
  //
  //  oop     get_local_object(jint slot) const;
  //  jint    get_local_int   (jint slot) const;
  //  jlong   get_local_long  (jint slot) const;
  //  jfloat  get_local_float (jint slot) const;
  //  jdouble get_local_double(jint slot) const;
  //
  //  void set_local_object(jint slot, oop     obj);
  //  void set_local_int   (jint slot, jint    i);
  //  void set_local_long  (jint slot, jlong   l);
  //  void set_local_float (jint slot, jfloat  f);
  //  void set_local_double(jint slot, jdouble d);

  // FIXME: add safepoint code, oops_do, etc.
  // FIXME: NOT FINISHED





  //--------------------------------------------------------------------------------
  // Internals only below this point
  //

  //   /** Helper method for better factored code in frame::sender */
  //   private frame sender_for_entry_frame(RegisterMap* map)        { throw new RuntimeException("not yet implemented"); }
  //   private frame sender_for_interpreter_frame(RegisterMap* map)  { throw new RuntimeException("not yet implemented"); }

  //
  // Oop iteration (FIXME: NOT FINISHED)
  //


  private static class InterpVisitor implements OopMapVisitor {
    private AddressVisitor addressVisitor;

    public InterpVisitor(AddressVisitor oopVisitor) {
      setAddressVisitor(oopVisitor);
    }

    public void setAddressVisitor(AddressVisitor addressVisitor) {
      this.addressVisitor = addressVisitor;
    }

    public void visitOopLocation(Address oopAddr) {
      addressVisitor.visitAddress(oopAddr);
    }

    public void visitDerivedOopLocation(Address baseOopAddr, Address derivedOopAddr) {
      if (VM.getVM().isClientCompiler()) {
        Assert.that(false, "should not reach here");
      } else if (VM.getVM().isServerCompiler() &&
                 VM.getVM().useDerivedPointerTable()) {
        Assert.that(false, "FIXME: add derived pointer table");
      }
    }

    public void visitNarrowOopLocation(Address compOopAddr) {
      addressVisitor.visitCompOopAddress(compOopAddr);
    }
  }

  private void oopsInterpretedDo(AddressVisitor oopVisitor, RegisterMap map) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(map != null, "map must be set");
    }
    Method m = getInterpreterFrameMethod();
    int bci  = getInterpreterFrameBCI();

    // FIXME: Seeing this sometimes
    if (VM.getVM().isDebugging()) {
      if (bci < 0 || bci >= m.getCodeSize()) return;
    }

    if (Assert.ASSERTS_ENABLED) {
      //      Assert.that(VM.getVM().getUniverse().heap().isIn(m), "method must be valid oop");
      Assert.that((m.isNative() && (bci == 0)) || ((bci >= 0) && (bci < m.getCodeSize())), "invalid bci value");
    }

    // Handle the monitor elements in the activation
    // FIXME: monitor information not yet exposed
    //    for (
    //      BasicObjectLock* current = interpreter_frame_monitor_end();
    //      current < interpreter_frame_monitor_begin();
    //      current = next_monitor_in_interpreter_frame(current)
    //    ) {
    //#ifdef ASSERT
    //      interpreter_frame_verify_monitor(current);
    //#endif
    //      current->oops_do(f);
    //    }

    // process fixed part
    // FIXME: these are no longer oops, so should anything be visitied?
    // oopVisitor.visitAddress(addressOfInterpreterFrameMethod());
    // oopVisitor.visitAddress(addressOfInterpreterFrameCPCache());

    // FIXME: expose interpreterFrameMirrorOffset
    //    if (m.isNative() && m.isStatic()) {
    //      oopVisitor.visitAddress(getFP().addOffsetTo(interpreterFrameMirrorOffset));
    //    }

    int maxLocals = (int) (m.isNative() ? m.getSizeOfParameters() : m.getMaxLocals());
    InterpreterFrameClosure blk = new InterpreterFrameClosure(this, maxLocals, (int) m.getMaxStack(), oopVisitor);

    // process locals & expression stack
    OopMapCacheEntry mask = m.getMaskFor(bci);
    mask.iterateOop(blk);

    // process a callee's arguments if we are at a call site
    // (i.e., if we are at an invoke bytecode)
    if (map.getIncludeArgumentOops() && !m.isNative()) {
      BytecodeInvoke call = BytecodeInvoke.atCheck(m, bci);
      if (call != null && getInterpreterFrameExpressionStackSize() > 0) {
        // we are at a call site & the expression stack is not empty
        // => process callee's arguments
        //
        // Note: The expression stack can be empty if an exception
        //       occured during method resolution/execution. In all
        //       cases we empty the expression stack completely be-
        //       fore handling the exception (the exception handling
        //       code in the interpreter calls a blocking runtime
        //       routine which can cause this code to be executed).
        //       (was bug gri 7/27/98)
        oopsInterpretedArgumentsDo(call.signature(), call.isInvokestatic(), oopVisitor);
      }
    }
  }

  private void oopsEntryDo      (AddressVisitor oopVisitor, RegisterMap regMap) {}
  private void oopsCodeBlobDo   (AddressVisitor oopVisitor, RegisterMap regMap) {
    CodeBlob cb = VM.getVM().getCodeCache().findBlob(getPC());
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(cb != null, "sanity check");
    }
    if (cb.getOopMaps() != null) {
      ImmutableOopMapSet.oopsDo(this, cb, regMap, oopVisitor, VM.getVM().isDebugging());

      // FIXME: add in traversal of argument oops (skipping this for
      // now until we have the other stuff tested)

    }

    // FIXME: would add this in in non-debugging system

    // If we see an activation belonging to a non_entrant nmethod, we mark it.
    //    if (cb->is_nmethod() && ((nmethod *)cb)->is_not_entrant()) {
    //      ((nmethod*)cb)->mark_as_seen_on_stack();
    //    }
  }

  // FIXME: implement the above routines, plus add
  // oops_interpreted_arguments_do and oops_compiled_arguments_do
}

//
// Only used internally, to iterate through oop slots in interpreted
// frames
//
class InterpreterFrameClosure implements OffsetClosure {
  // Used for debugging this code
  private static final boolean DEBUG = false;

  private Frame fr;
  private AddressVisitor f;
  private int maxLocals;
  private int maxStack;

  InterpreterFrameClosure(Frame fr, int maxLocals, int maxStack, AddressVisitor f) {
    this.fr = fr;
    this.maxLocals = maxLocals;
    this.maxStack = maxStack;
    this.f = f;
  }

  public void offsetDo(int offset) {
    if (DEBUG) {
      System.err.println("Visiting offset " + offset + ", maxLocals = " + maxLocals +
                         " for frame " + fr + ", method " +
                         fr.getInterpreterFrameMethod().getMethodHolder().getName().asString() +
                         fr.getInterpreterFrameMethod().getName().asString());
    }
    Address addr;
    if (offset < maxLocals) {
      addr = fr.addressOfInterpreterFrameLocal(offset);
      if (Assert.ASSERTS_ENABLED) {
        Assert.that(AddressOps.gte(addr, fr.getSP()), "must be inside the frame");
      }
      if (DEBUG) {
        System.err.println("  Visiting local at addr " + addr);
      }
      f.visitAddress(addr);
    } else {
      addr = fr.addressOfInterpreterFrameExpressionStackSlot(offset - maxLocals);
      if (DEBUG) {
        System.err.println("  Address of expression stack slot: " + addr + ", TOS = " +
                           fr.addressOfInterpreterFrameTOS());
      }
      // In case of exceptions, the expression stack is invalid and the esp will be reset to express
      // this condition. Therefore, we call f only if addr is 'inside' the stack (i.e., addr >= esp for Intel).
      boolean inStack;
      if (fr.getInterpreterFrameExpressionStackDirection() > 0) {
        inStack = AddressOps.lte(addr, fr.addressOfInterpreterFrameTOS());
      } else {
        inStack = AddressOps.gte(addr, fr.addressOfInterpreterFrameTOS());
      }
      if (inStack) {
        if (DEBUG) {
          System.err.println("  In stack; visiting location.");
        }
        f.visitAddress(addr);
      } else if (DEBUG) {
        System.err.println("  *** WARNING: Address is out of bounds");
      }
    }
  }
}

// Only used internally, to find arguments in interpreted frames
class ArgumentOopFinder extends SignatureInfo {
  private AddressVisitor f;
  private int            offset;
  private boolean        isStatic;
  private Frame          fr;

  protected void set(int size, int type) {
    offset -= size;
    if (type == BasicType.getTObject() || type == BasicType.getTArray()) oopOffsetDo();
  }

  private void oopOffsetDo() {
    f.visitAddress(fr.addressOfInterpreterFrameTOSAt(offset));
  }

  public ArgumentOopFinder(Symbol signature, boolean isStatic, Frame fr, AddressVisitor f) {
    super(signature);

    // compute size of arguments
    int argsSize = new ArgumentSizeComputer(signature).size() + (isStatic ? 0 : 1);
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(!fr.isInterpretedFrame() ||
                  argsSize <= fr.getInterpreterFrameExpressionStackSize(), "args cannot be on stack anymore");
    }
    // initialize ArgumentOopFinder
    this.f        = f;
    this.fr       = fr;
    this.offset   = argsSize;
    this.isStatic = isStatic;
  }

  public void oopsDo() {
    if (!isStatic) {
      --offset;
      oopOffsetDo();
    }
    iterateParameters();
  }
}
