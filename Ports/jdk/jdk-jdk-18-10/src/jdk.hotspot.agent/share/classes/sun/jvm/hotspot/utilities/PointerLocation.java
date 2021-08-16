/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.utilities;

import java.io.*;
import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.*;
import sun.jvm.hotspot.gc.shared.*;
import sun.jvm.hotspot.interpreter.*;
import sun.jvm.hotspot.memory.*;
import sun.jvm.hotspot.oops.Metadata;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.Type;

/** This class attempts to describe possible locations of pointers in
    the VM. */

public class PointerLocation {
  //////////////////////////////////////////////////////////////////
  //                                                              //
  // These are package private to simplify the implementation and //
  // interaction with PointerFinder                               //
  //                                                              //
  //////////////////////////////////////////////////////////////////

  Address addr;

  Metadata metadata;
  Type ctype;
  JavaThread stackThread;

  LoadObject loadObject;
  ClosestSymbol nativeSymbol;

  CollectedHeap heap;
  Generation gen;

  // If UseTLAB was enabled and the pointer was found in a
  // currently-active TLAB, these will be set
  boolean inTLAB;
  JavaThread tlabThread;
  ThreadLocalAllocBuffer tlab;

  // Generated code locations
  boolean inInterpreter;
  boolean inCodeCache;

  // FIXME: add other locations like VTableStubs, StubRoutines, maybe
  // even "on thread x's stack"

  InterpreterCodelet interpreterCodelet;
  CodeBlob blob;
  // FIXME: add more detail about CodeBlob
  boolean inBlobCode;
  boolean inBlobData;
  boolean inBlobOops;
  boolean inBlobUnknownLocation;

  boolean inStrongGlobalJNIHandles;
  boolean inWeakGlobalJNIHandles;

  boolean inLocalJNIHandleBlock;
  JNIHandleBlock handleBlock;
  sun.jvm.hotspot.runtime.Thread handleThread;

  public PointerLocation(Address addr) {
    this.addr = addr;
  }

  public boolean isMetadata() {
    return metadata != null;
  }

  public boolean isCtype() {
    return ctype != null;
  }

  public boolean isInJavaStack() {
    return stackThread != null;
  }

  public boolean isNativeSymbol() {
    return loadObject != null;
  }

  public boolean isInHeap() {
    return (heap != null || (gen != null));
  }

  public boolean isInNewGen() {
    return ((gen != null) && (gen == ((GenCollectedHeap)heap).getGen(0)));
  }

  public boolean isInOldGen() {
    return ((gen != null) && (gen == ((GenCollectedHeap)heap).getGen(1)));
  }

  public boolean inOtherGen() {
    return (!isInNewGen() && !isInOldGen());
  }

  /** Only valid if isInHeap() */
  public Generation getGeneration() {
      return gen;
  }

  /** This may be true if isInNewGen is also true */
  public boolean isInTLAB() {
    return inTLAB;
  }

  /** Only valid if isInTLAB() returns true */
  public JavaThread getTLABThread() {
    return tlabThread;
  }

  /** Only valid if isInTLAB() returns true */
  public ThreadLocalAllocBuffer getTLAB() {
    return tlab;
  }

  public boolean isInInterpreter() {
    return inInterpreter;
  }

  /** For now, only valid if isInInterpreter is true */
  public InterpreterCodelet getInterpreterCodelet() {
    return interpreterCodelet;
  }

  public boolean isInCodeCache() {
    return inCodeCache;
  }

  /** For now, only valid if isInCodeCache is true */
  public CodeBlob getCodeBlob() {
    return blob;
  }

  public boolean isInBlobCode() {
    return inBlobCode;
  }

  public boolean isInBlobData() {
    return inBlobData;
  }

  public boolean isInBlobOops() {
    return inBlobOops;
  }

  public boolean isInBlobUnknownLocation() {
    return inBlobUnknownLocation;
  }

  public boolean isInStrongGlobalJNIHandles() {
    return inStrongGlobalJNIHandles;
  }

  public boolean isInWeakGlobalJNIHandles() {
    return inWeakGlobalJNIHandles;
  }

  public boolean isInLocalJNIHandleBlock() {
    return inLocalJNIHandleBlock;
  }

  /** Only valid if isInLocalJNIHandleBlock is true */
  public JNIHandleBlock getJNIHandleBlock() {
    assert isInLocalJNIHandleBlock();
    return handleBlock;
  }

  /** Only valid if isInLocalJNIHandleBlock is true */
  public sun.jvm.hotspot.runtime.Thread getJNIHandleThread() {
    assert isInLocalJNIHandleBlock();
    return handleThread;
  }

  public boolean isUnknown() {
      return (!(isMetadata() || isCtype() || isInJavaStack() || isNativeSymbol() || isInHeap() ||
                isInInterpreter() || isInCodeCache() || isInStrongGlobalJNIHandles() ||
                isInWeakGlobalJNIHandles() || isInLocalJNIHandleBlock()));
  }

  public String toString() {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    printOn(new PrintStream(bos));
    return bos.toString();
  }

  public void print() {
      printOn(System.out, true, true);
  }

  public void print(boolean printAddress, boolean verbose) {
    printOn(System.out, printAddress, verbose);
  }

  public void printOn(PrintStream tty) {
    printOn(tty, true, true);
  }

  public void printOn(PrintStream tty, boolean printAddress, boolean verbose) {
    if (printAddress) {
      tty.print("Address ");
      if (addr == null) {
        tty.print("0x0");
      } else {
        tty.print(addr.toString());
      }
      tty.print(": ");
    }
    if (isMetadata()) {
      metadata.printValueOn(tty); // does not include "\n"
      tty.println();
    } else if (isCtype()) {
      tty.println("Is of type " + ctype.getName());
    } else if (isInJavaStack()) {
        if (verbose) {
            tty.format("In java stack [%s,%s,%s] for thread %s:\n   ",
                       stackThread.getStackBase(), stackThread.lastSPDbg(),
                       stackThread.getStackBase().addOffsetTo(-stackThread.getStackSize()),
                       stackThread);
            stackThread.printThreadInfoOn(tty); // includes "\n"
        } else {
            tty.format("In java stack for thread \"%s\" %s\n", stackThread.getThreadName(), stackThread);
        }
    } else if (isNativeSymbol()) {
        CDebugger cdbg = VM.getVM().getDebugger().getCDebugger();
        long diff;
        if (nativeSymbol != null) {
            String name = nativeSymbol.getName();
            if (cdbg.canDemangle()) {
                name = cdbg.demangle(name);
            }
            tty.print(name);
            diff = nativeSymbol.getOffset();
        } else {
            tty.print(loadObject.getName());
            diff = addr.minus(loadObject.getBase());
        }
        if (diff != 0L) {
            tty.print(" + 0x" + Long.toHexString(diff));
        }
        tty.println();
    } else if (isInHeap()) {
      if (isInTLAB()) {
        tty.print("In thread-local allocation buffer for thread (");
        getTLABThread().printThreadInfoOn(tty);
        tty.print(") ");
        getTLAB().printOn(tty); // includes "\n"
      } else {
        if (isInNewGen()) {
          tty.print("In new generation ");
        } else if (isInOldGen()) {
          tty.print("In old generation ");
        } else {
          tty.print("In unknown section of Java heap");
        }
        if (getGeneration() != null) {
          getGeneration().printOn(tty); // does not include "\n"
        }
        tty.println();
      }
    } else if (isInInterpreter()) {
      tty.print("In interpreter codelet: ");
      interpreterCodelet.printOn(tty); // includes "\n"
    } else if (isInCodeCache()) {
      // TODO: print the type of CodeBlob. See "look for known code blobs" comment
      // in PStack.java for example code.
      CodeBlob b = getCodeBlob();
      tty.print("In ");
      if (isInBlobCode()) {
        tty.print("code");
      } else if (isInBlobData()) {
        tty.print("data");
      } else if (isInBlobOops()) {
        tty.print("oops");
      } else {
        if (Assert.ASSERTS_ENABLED) {
          Assert.that(isInBlobUnknownLocation(), "Should have known location in CodeBlob");
        }
        tty.print("unknown location");
      }
      tty.print(" in ");
      if (verbose) {
          b.printOn(tty); // includes "\n"
      } else {
          tty.println(b.toString());
      }

      // FIXME: add more detail
    } else if (isInStrongGlobalJNIHandles()) {
      tty.println("In JNI strong global");
    } else if (isInWeakGlobalJNIHandles()) {
      tty.println("In JNI weak global");
    } else if (isInLocalJNIHandleBlock()) {
      tty.print("In thread-local");
      tty.print(" JNI handle block (" + handleBlock.top() + " handle slots present)");
      if (handleThread.isJavaThread()) {
        tty.print(" for JavaThread ");
        ((JavaThread) handleThread).printThreadIDOn(tty); // includes "\n"
      } else {
        tty.println(" for a non-Java Thread");
      }
    } else {
      // This must be last
      if (Assert.ASSERTS_ENABLED) {
        Assert.that(isUnknown(), "Should have unknown location");
      }
      tty.println("In unknown location");
    }
  }
}
