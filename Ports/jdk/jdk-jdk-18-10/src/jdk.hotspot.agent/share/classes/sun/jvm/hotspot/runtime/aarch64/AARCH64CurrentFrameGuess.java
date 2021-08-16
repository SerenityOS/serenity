/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2015, 2019, Red Hat Inc.
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

package sun.jvm.hotspot.runtime.aarch64;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.aarch64.*;
import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.interpreter.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.runtime.aarch64.*;

/** <P> Should be able to be used on all aarch64 platforms we support
    (Linux/aarch64) to implement JavaThread's "currentFrameGuess()"
    functionality. Input is an AARCH64ThreadContext; output is SP, FP,
    and PC for an AARCH64Frame. Instantiation of the AARCH64Frame is
    left to the caller, since we may need to subclass AARCH64Frame to
    support signal handler frames on Unix platforms. </P>

    <P> Algorithm is to walk up the stack within a given range (say,
    512K at most) looking for a plausible PC and SP for a Java frame,
    also considering those coming in from the context. If we find a PC
    that belongs to the VM (i.e., in generated code like the
    interpreter or CodeCache) then we try to find an associated FP.
    We repeat this until we either find a complete frame or run out of
    stack to look at. </P> */

public class AARCH64CurrentFrameGuess {
  private AARCH64ThreadContext context;
  private JavaThread       thread;
  private Address          spFound;
  private Address          fpFound;
  private Address          pcFound;

  private static final boolean DEBUG = System.getProperty("sun.jvm.hotspot.runtime.aarch64.AARCH64Frame.DEBUG")
                                       != null;

  public AARCH64CurrentFrameGuess(AARCH64ThreadContext context,
                              JavaThread thread) {
    this.context = context;
    this.thread  = thread;
  }

  /** Returns false if not able to find a frame within a reasonable range. */
  public boolean run(long regionInBytesToSearch) {
    Address sp  = context.getRegisterAsAddress(AARCH64ThreadContext.SP);
    Address pc  = context.getRegisterAsAddress(AARCH64ThreadContext.PC);
    Address fp  = context.getRegisterAsAddress(AARCH64ThreadContext.FP);
    if (sp == null) {
      // Bail out if no last java frame either
      if (thread.getLastJavaSP() != null) {
        setValues(thread.getLastJavaSP(), thread.getLastJavaFP(), null);
        return true;
      }
      return false;
    }
    Address end = sp.addOffsetTo(regionInBytesToSearch);
    VM vm       = VM.getVM();

    setValues(null, null, null); // Assume we're not going to find anything

    if (vm.isJavaPCDbg(pc)) {
      if (vm.isClientCompiler()) {
        // If the topmost frame is a Java frame, we are (pretty much)
        // guaranteed to have a viable FP. We should be more robust
        // than this (we have the potential for losing entire threads'
        // stack traces) but need to see how much work we really have
        // to do here. Searching the stack for an (SP, FP) pair is
        // hard since it's easy to misinterpret inter-frame stack
        // pointers as base-of-frame pointers; we also don't know the
        // sizes of C1 frames (not registered in the nmethod) so can't
        // derive them from SP.

        setValues(sp, fp, pc);
        return true;
      } else {
        if (vm.getInterpreter().contains(pc)) {
          if (DEBUG) {
            System.out.println("CurrentFrameGuess: choosing interpreter frame: sp = " +
                               sp + ", fp = " + fp + ", pc = " + pc);
          }
          setValues(sp, fp, pc);
          return true;
        }

        // For the server compiler, FP is not guaranteed to be valid
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

        for (long offset = 0;
             offset < regionInBytesToSearch;
             offset += vm.getAddressSize()) {
          try {
            Address curSP = sp.addOffsetTo(offset);
            Frame frame = new AARCH64Frame(curSP, null, pc);
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

        // For the server compiler, FP is not guaranteed to be valid
        // for compiled code. We see whether the PC is in the
        // interpreter and take care of that, otherwise we run code
        // (unfortunately) duplicated from AARCH64Frame.senderForCompiledFrame.

        CodeCache cc = vm.getCodeCache();
        if (cc.contains(pc)) {
          CodeBlob cb = cc.findBlob(pc);

          // See if we can derive a frame pointer from SP and PC
          // NOTE: This is the code duplicated from AARCH64Frame
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
    } else {
      // If the current program counter was not known to us as a Java
      // PC, we currently assume that we are in the run-time system
      // and attempt to look to thread-local storage for saved SP and
      // FP. Note that if these are null (because we were, in fact,
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

      // The runtime has a nasty habit of not saving fp in the frame
      // anchor, leaving us to grovel about in the stack to find a
      // plausible address.  Fortunately, this only happens in
      // compiled code; there we always have a valid PC, and we always
      // push LR and FP onto the stack as a pair, with FP at the lower
      // address.
      pc = thread.getLastJavaPC();
      fp = thread.getLastJavaFP();
      sp = thread.getLastJavaSP();

      if (fp == null) {
        CodeCache cc = vm.getCodeCache();
        if (cc.contains(pc)) {
          CodeBlob cb = cc.findBlob(pc);
          if (DEBUG) {
            System.out.println("FP is null.  Found blob frame size " + cb.getFrameSize());
          }
          // See if we can derive a frame pointer from SP and PC
          long link_offset = cb.getFrameSize() - 2 * VM.getVM().getAddressSize();
          if (link_offset >= 0) {
            fp = sp.addOffsetTo(link_offset);
          }
        }
      }

      // We found a PC in the frame anchor. Check that it's plausible, and
      // if it is, use it.
      if (vm.isJavaPCDbg(pc)) {
        setValues(sp, fp, pc);
      } else {
        setValues(sp, fp, null);
      }

      return true;
    }
  }

  public Address getSP() { return spFound; }
  public Address getFP() { return fpFound; }
  /** May be null if getting values from thread-local storage; take
      care to call the correct AARCH64Frame constructor to recover this if
      necessary */
  public Address getPC() { return pcFound; }

  private void setValues(Address sp, Address fp, Address pc) {
    spFound = sp;
    fpFound = fp;
    pcFound = pc;
  }
}
