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

import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.*;
import sun.jvm.hotspot.gc.shared.*;
import sun.jvm.hotspot.interpreter.*;
import sun.jvm.hotspot.memory.*;
import sun.jvm.hotspot.oops.Metadata;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.WrongTypeException;

/** This class, only intended for use in the debugging system,
    provides the functionality of find() in the VM. */

public class PointerFinder {
  public static PointerLocation find(Address a) {
    PointerLocation loc = new PointerLocation(a);
    Threads threads = VM.getVM().getThreads();

    // Check if address is a pointer to a Metadata object.
    try {
      loc.metadata = Metadata.instantiateWrapperFor(a);
      return loc;
    } catch (Exception e) {
      // Just ignore. This just means we aren't dealing with a Metadata pointer.
    }

    // Check if address is some other C++ type that we can deduce
    loc.ctype = VM.getVM().getTypeDataBase().guessTypeForAddress(a);
    if (loc.ctype == null && VM.getVM().isSharingEnabled()) {
      // Check if the value falls in the _md_region
      try {
        Address loc1 = a.getAddressAt(0);
        FileMapInfo cdsFileMapInfo = VM.getVM().getFileMapInfo();
        if (cdsFileMapInfo.inCopiedVtableSpace(loc1)) {
          loc.ctype = cdsFileMapInfo.getTypeForVptrAddress(loc1);
        }
      } catch (AddressException | WrongTypeException e) {
        // This can happen if "a" or "loc1" is a bad address. Just ignore.
      }
    }
    if (loc.ctype != null) {
      return loc;
    }

    // Check if address is in the stack of a JavaThread
    for (int i = 0; i < threads.getNumberOfThreads(); i++) {
        JavaThread t = threads.getJavaThreadAt(i);
        Address stackBase = t.getStackBase();
        if (stackBase != null) {
            Long stackSize = t.getStackSize();
            Address stackEnd = stackBase.addOffsetTo(-stackSize);
            if (a.lessThanOrEqual(stackBase) && a.greaterThan(stackEnd)) {
                loc.stackThread = t;
                return loc;
            }
        }
    }

    // Check if address is in the java heap.
    CollectedHeap heap = VM.getVM().getUniverse().heap();
    if (heap instanceof GenCollectedHeap) {
      GenCollectedHeap genheap = (GenCollectedHeap) heap;
      if (genheap.isIn(a)) {
        for (int i = 0; i < genheap.nGens(); i++) {
          Generation g = genheap.getGen(i);
          if (g.isIn(a)) {
            loc.gen = g;
            break;
          }
        }

        if (Assert.ASSERTS_ENABLED) {
          Assert.that(loc.gen != null, "Should have found this in a generation");
        }

        if (VM.getVM().getUseTLAB()) {
          // Try to find thread containing it
          for (int i = 0; i < threads.getNumberOfThreads(); i++) {
            JavaThread t = threads.getJavaThreadAt(i);
            ThreadLocalAllocBuffer tlab = t.tlab();
            if (tlab.contains(a)) {
              loc.inTLAB = true;
              loc.tlabThread = t;
              loc.tlab = tlab;
              break;
            }
          }
        }

        return loc;
      }
    } else {
      if (heap.isIn(a)) {
        loc.heap = heap;
        return loc;
      }
    }

    // Check if address is in the interpreter
    Interpreter interp = VM.getVM().getInterpreter();
    if (interp.contains(a)) {
      loc.inInterpreter = true;
      loc.interpreterCodelet = interp.getCodeletContaining(a);
      return loc;
    }

    // Check if address is in the code cache
    if (!VM.getVM().isCore()) {
      CodeCache c = VM.getVM().getCodeCache();
      if (c.contains(a)) {
        loc.inCodeCache = true;
        loc.blob = c.findBlobUnsafe(a);
        if (Assert.ASSERTS_ENABLED) {
          Assert.that(loc.blob != null, "Should have found CodeBlob");
        }
        loc.inBlobCode = loc.blob.codeContains(a);
        loc.inBlobData = loc.blob.dataContains(a);

        if (loc.blob.isNMethod()) {
            NMethod nm = (NMethod) loc.blob;
            loc.inBlobOops = nm.oopsContains(a);
        }

        loc.inBlobUnknownLocation = (!(loc.inBlobCode ||
                                       loc.inBlobData ||
                                       loc.inBlobOops));
        return loc;
      }
    }

    // Check JNIHandles; both local and global
    JNIHandles handles = VM.getVM().getJNIHandles();

    // --- looking in oopstorage should model OopStorage::allocation_status?
    // --- that is, if in a block but not allocated, then not valid.

    // Look in global handles
    OopStorage storage = handles.globalHandles();
    if ((storage != null) && storage.findOop(a)) {
      loc.inStrongGlobalJNIHandles = true;
      return loc;
    }
    // Look in weak global handles
    storage = handles.weakGlobalHandles();
    if ((storage != null) && storage.findOop(a)) {
      loc.inWeakGlobalJNIHandles = true;
      return loc;
    }
    // Look in thread-local handles
    for (int i = 0; i < threads.getNumberOfThreads(); i++) {
      JavaThread t = threads.getJavaThreadAt(i);
      JNIHandleBlock handleBlock = t.activeHandles();
      if (handleBlock != null) {
        handleBlock = handleBlock.blockContainingHandle(a);
        if (handleBlock != null) {
          loc.inLocalJNIHandleBlock = true;
          loc.handleBlock = handleBlock;
          loc.handleThread = t;
          return loc;
        }
      }
    }

    // Check if address is a native (C++) symbol. Do this last because we don't always
    // do a good job of computing if an address is actually within a native lib, and sometimes
    // an address outside of a lib will be found as inside.
    JVMDebugger dbg = VM.getVM().getDebugger();
    CDebugger cdbg = dbg.getCDebugger();
    if (cdbg != null) {
        loc.loadObject = cdbg.loadObjectContainingPC(a);
        if (loc.loadObject != null) {
            loc.nativeSymbol = loc.loadObject.closestSymbolToPC(a);
            return loc;
        }
    }

    // Fall through; have to return it anyway.
    return loc;
  }
}
