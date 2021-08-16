/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.runtime.amd64;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.amd64.*;
import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.interpreter.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.runtime.x86.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;

/** <P> Should be able to be used on all amd64 platforms we support
    (Linux/amd64) to implement JavaThread's
    "currentFrameGuess()" functionality. Input is an AMD64ThreadContext;
    output is SP, FP, and PC for an AMD64Frame. Instantiation of the
    AMD64Frame is left to the caller, since we may need to subclass
    AMD64Frame to support signal handler frames on Unix platforms. </P>

    <P> Algorithm is to walk up the stack within a given range (say,
    512K at most) looking for a plausible PC and SP for a Java frame,
    also considering those coming in from the context. If we find a PC
    that belongs to the VM (i.e., in generated code like the
    interpreter or CodeCache) then we try to find an associated EBP.
    We repeat this until we either find a complete frame or run out of
    stack to look at. </P> */

public class AMD64CurrentFrameGuess {
  private AMD64ThreadContext context;
  private JavaThread       thread;
  private Address          spFound;
  private Address          fpFound;
  private Address          pcFound;

  private static final boolean DEBUG = System.getProperty("sun.jvm.hotspot.runtime.amd64.AMD64Frame.DEBUG")
                                       != null;

  public AMD64CurrentFrameGuess(AMD64ThreadContext context,
                              JavaThread thread) {
    this.context = context;
    this.thread  = thread;
  }

  private boolean validateInterpreterFrame(Address sp, Address fp, Address pc) {
    VM vm = VM.getVM();
    X86Frame f = new X86Frame(sp, fp, pc);

    // First validate that frame->method is really a Method*
    Method method = null;
    try {
      method = f.getInterpreterFrameMethod();
    } catch (WrongTypeException | AddressException | NullPointerException e) {
      // This just means frame->method is not valid.
      if (DEBUG) {
        System.out.println("CurrentFrameGuess: frame->method is invalid");
      }
    }

    // Next make sure frame->bcp is really in the method's bytecodes
    if (method != null && f.getInterpreterFrameBCP() != null) {
      if (method.getConstMethod().isAddressInMethod(f.getInterpreterFrameBCP())) {
        // All's good. This is the normal path when the PC is in the interpreter.
        // The cases below are all exceptionally rare.
        setValues(sp, fp, pc);
        return true;
      } else {
        if (DEBUG) {
          System.out.println("CurrentFrameGuess: frame->bcp is invalid");
        }
      }
    }

    // Either frame->method is not a Method* or frame->bcp is not valid. That means either
    // we have pushed the new interpreter frame, but have not intialized it yet, or
    // we have yet to push the new interpreter frame, and the "current" frame is not an
    // interpreter frame. Figure out which is the case.

    // Try to find the return address in RAX or on the stack. If we can't
    // find what appears to be a valid codecache address in either of these
    // two locations, then we cannot determine the frame.
    Address returnAddress = context.getRegisterAsAddress(AMD64ThreadContext.RAX);
    CodeCache c = VM.getVM().getCodeCache();
    if (returnAddress == null || !c.contains(returnAddress)) {
      returnAddress = sp.getAddressAt(0);  // check top of stack
      if (returnAddress == null || !c.contains(returnAddress)) {
        if (DEBUG) {
          System.out.println("CurrentFrameGuess: Cannot find valid returnAddress");
        }
        setValues(sp, fp, pc);
        return false; // couldn't find a valid PC for frame.
      } else {
        if (DEBUG) {
          System.out.println("CurrentFrameGuess: returnAddress found on stack: " + returnAddress);
        }
      }
    } else {
      if (DEBUG) {
        System.out.println("CurrentFrameGuess: returnAddress found in RAX: " + returnAddress);
      }
    }

    // See what return address is stored in the frame. Most likely it is not valid, but
    // its validity will help us determine the state of the new frame push.
    Address returnAddress2 = null;
    try {
      returnAddress2 = f.getSenderPC();
    } catch (AddressException e) {
      // Just ignore. This is expected sometimes.
      if (DEBUG) {
        System.out.println("CurrentFrameGuess: senderPC is invalid");
      }
    }
    if (DEBUG) {
      System.out.println("CurrentFrameGuess: returnAddress2: " + returnAddress2);
    }

    if (returnAddress.equals(returnAddress2)) {
      // If these two ways of fetching the return address produce the same address,
      // then that means we have pushed the new frame, but have not finished
      // initializing it yet. Otherwise we would also have found a valid frame->method
      // and frame->bcp. Because this frame is incomplete, we instead use
      // the previous frame as the current frame.
      if (DEBUG) {
        System.out.println("CurrentFrameGuess: frame pushed but not initialized.");
      }
      sp = f.getSenderSP();
      fp = f.getLink();
      setValues(sp, fp, returnAddress);
      // If this previous frame is interpreted, then we are done and setValues() has been
      // called with a valid interpreter frame. Otherwise return false and the caller will
      // need to determine frame.
      if (vm.getInterpreter().contains(returnAddress)) {
        if (DEBUG) {
          System.out.println("CurrentFrameGuess: Interpreted: using previous frame.");
        }
        return true;
      } else {
        if (DEBUG) {
          System.out.println("CurrentFrameGuess: Not Interpreted: using previous frame.");
        }
        return false;
      }
    } else {
      // We haven't even pushed the new frame yet. sp and fp are for the previous
      // frame that is making the call to the interpreter. Since this frame is
      // not a valid interpreter frame (we know either frame->method or frame->bcp
      // are not valid), it must be something else. Assume compiled or native and
      // let the  caller figure it out.
      setValues(sp, fp, returnAddress);
      if (DEBUG) {
        System.out.println("CurrentFrameGuess: Frame not yet pushed. Previous frame not interpreted.");
      }
      return false;
    }
  }

  /** Returns false if not able to find a frame within a reasonable range. */
  public boolean run(long regionInBytesToSearch) {
    Address sp  = context.getRegisterAsAddress(AMD64ThreadContext.RSP);
    Address pc  = context.getRegisterAsAddress(AMD64ThreadContext.RIP);
    Address fp  = context.getRegisterAsAddress(AMD64ThreadContext.RBP);
    if (sp == null) {
      return checkLastJavaSP();
    }
    Address end = sp.addOffsetTo(regionInBytesToSearch);
    VM vm       = VM.getVM();

    setValues(null, null, null); // Assume we're not going to find anything

    if (!vm.isJavaPCDbg(pc)) {
      return checkLastJavaSP();
    } else {
      if (vm.isClientCompiler()) {
        // If the topmost frame is a Java frame, we are (pretty much)
        // guaranteed to have a viable EBP. We should be more robust
        // than this (we have the potential for losing entire threads'
        // stack traces) but need to see how much work we really have
        // to do here. Searching the stack for an (SP, FP) pair is
        // hard since it's easy to misinterpret inter-frame stack
        // pointers as base-of-frame pointers; we also don't know the
        // sizes of C1 frames (not registered in the nmethod) so can't
        // derive them from ESP.

        setValues(sp, fp, pc);
        return true;
      } else {
        if (vm.getInterpreter().contains(pc)) {
          // pc points into the interpreter, but that doesn't necessarily mean the current
          // frame is interpreted. We may be in interpreter method entry code before the frame
          // has been pushed, or possibly after it has been pushed but before it has been
          // initialized. See TemplateInterpreterGenerator::generate_normal_entry(). So we
          // need to do a few sanity checks here, and try to correct the situation if
          // we are in the middle of a frame push.
          if (validateInterpreterFrame(sp, fp, pc)) {
            if (DEBUG) {
              System.out.println("CurrentFrameGuess: choosing interpreter frame: sp = " +
                                 spFound + ", fp = " + fpFound + ", pc = " + pcFound);
            }
            return true; // We're done. setValues() has been called for valid interpreter frame.
          } else {
            // This does not appear to be a valid interpreter frame. Possibly we are in the
            // middle of pushing a new frame. Update the frame values to those suggested
            // by validateInterpreterFrame() and then fall through to check if it is compiled.
            sp = spFound;
            fp = fpFound;
            pc = pcFound;
            setValues(null, null, null);
            if (pcFound == null) {
              return false;
            }
            // pc may have changed, so we need to redo the isJavaPCDbg(pc) check before
            // falling into code below that assumes the frame is compiled.
            if (!vm.isJavaPCDbg(pc)) {
              return checkLastJavaSP();
            }
          }
        }

        // For the server compiler, EBP is not guaranteed to be valid
        // for compiled code. In addition, an earlier attempt at a
        // non-searching algorithm (see below) failed because the
        // stack pointer from the thread context was pointing
        // (considerably) beyond the ostensible end of the stack, into
        // garbage; walking from the topmost frame back caused a crash.
        //
        // This algorithm takes the current PC as a given and tries to
        // find the correct corresponding SP by walking up the stack
        // and repeatedly performing stackwalks (very inefficient).
        //
        // FIXME: there is something wrong with stackwalking across
        // adapter frames...this is likely to be the root cause of the
        // failure with the simpler algorithm below.

        if (DEBUG) {
          System.out.println("CurrentFrameGuess: sp = " + sp + ", pc = " + pc);
        }
        for (long offset = 0;
             offset < regionInBytesToSearch;
             offset += vm.getAddressSize()) {
          try {
            Address curSP = sp.addOffsetTo(offset);
            Frame frame = new X86Frame(curSP, null, pc);
            RegisterMap map = thread.newRegisterMap(false);
            while (frame != null) {
              if (frame.isEntryFrame() && frame.entryFrameIsFirst()) {
                // We were able to traverse all the way to the
                // bottommost Java frame.
                // This sp looks good. Keep it.
                if (DEBUG) {
                  System.out.println("CurrentFrameGuess: Choosing sp = " + curSP + ", pc = " + pc);
                }
                setValues(curSP, null, pc);
                return true;
              }
              Frame oldFrame = frame;
              frame = frame.sender(map);
              if (frame.getSP().lessThanOrEqual(oldFrame.getSP())) {
                  // Frame points to itself or to a location in the wrong direction.
                  // Break the loop and move on to next offset.
                  if (DEBUG) {
                    System.out.println("CurrentFrameGuess: frame <= oldFrame: " + frame);
                  }
                  break;
              }
            }
          } catch (Exception e) {
            if (DEBUG) {
              System.out.println("CurrentFrameGuess: Exception " + e + " at offset " + offset);
            }
            // Bad SP. Try another.
          }
        }

        // Were not able to find a plausible SP to go with this PC.
        // Bail out.
        return false;

        /*
        // Original algorithm which does not work because SP was
        // pointing beyond where it should have:

        // For the server compiler, EBP is not guaranteed to be valid
        // for compiled code. We see whether the PC is in the
        // interpreter and take care of that, otherwise we run code
        // (unfortunately) duplicated from AMD64Frame.senderForCompiledFrame.

        CodeCache cc = vm.getCodeCache();
        if (cc.contains(pc)) {
          CodeBlob cb = cc.findBlob(pc);

          // See if we can derive a frame pointer from SP and PC
          // NOTE: This is the code duplicated from AMD64Frame
          Address saved_fp = null;
          int llink_offset = cb.getLinkOffset();
          if (llink_offset >= 0) {
            // Restore base-pointer, since next frame might be an interpreter frame.
            Address fp_addr = sp.addOffsetTo(VM.getVM().getAddressSize() * llink_offset);
            saved_fp = fp_addr.getAddressAt(0);
          }

          setValues(sp, saved_fp, pc);
          return true;
        }
        */
      }
    }
  }

  private boolean checkLastJavaSP() {
    // If the current program counter was not known to us as a Java
    // PC, we currently assume that we are in the run-time system
    // and attempt to look to thread-local storage for saved ESP and
    // EBP. Note that if these are null (because we were, in fact,
    // in Java code, i.e., vtable stubs or similar, and the SA
    // didn't have enough insight into the target VM to understand
    // that) then we are going to lose the entire stack trace for
    // the thread, which is sub-optimal. FIXME.

    if (DEBUG) {
      System.out.println("CurrentFrameGuess: choosing last Java frame: sp = " +
                         thread.getLastJavaSP() + ", fp = " + thread.getLastJavaFP());
    }
    if (thread.getLastJavaSP() == null) {
      return false; // No known Java frames on stack
    }
    setValues(thread.getLastJavaSP(), thread.getLastJavaFP(), null);
    return true;
  }

  public Address getSP() { return spFound; }
  public Address getFP() { return fpFound; }
  /** May be null if getting values from thread-local storage; take
      care to call the correct AMD64Frame constructor to recover this if
      necessary */
  public Address getPC() { return pcFound; }

  private void setValues(Address sp, Address fp, Address pc) {
    spFound = sp;
    fpFound = fp;
    pcFound = pc;
  }
}
