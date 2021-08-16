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

//
// The ObjectHeap is an abstraction over all generations in the VM
// It gives access to all present objects and classes.
//

package sun.jvm.hotspot.oops;

import java.util.*;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.gc.shared.*;
import sun.jvm.hotspot.gc.epsilon.*;
import sun.jvm.hotspot.gc.g1.*;
import sun.jvm.hotspot.gc.shenandoah.*;
import sun.jvm.hotspot.gc.parallel.*;
import sun.jvm.hotspot.gc.z.*;
import sun.jvm.hotspot.memory.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;

public class ObjectHeap {

  private static final boolean DEBUG;

  static {
    DEBUG = System.getProperty("sun.jvm.hotspot.oops.ObjectHeap.DEBUG") != null;
  }

  public ObjectHeap(TypeDataBase db) throws WrongTypeException {
    // Get commonly used sizes of basic types
    oopSize     = VM.getVM().getOopSize();
    byteSize    = db.getJByteType().getSize();
    charSize    = db.getJCharType().getSize();
    booleanSize = db.getJBooleanType().getSize();
    intSize     = db.getJIntType().getSize();
    shortSize   = db.getJShortType().getSize();
    longSize    = db.getJLongType().getSize();
    floatSize   = db.getJFloatType().getSize();
    doubleSize  = db.getJDoubleType().getSize();
  }

  /** Comparison operation for oops, either or both of which may be null */
  public boolean equal(Oop o1, Oop o2) {
    if (o1 != null) return o1.equals(o2);
    return (o2 == null);
  }

  // Cached sizes of basic types
  private long oopSize;
  private long byteSize;
  private long charSize;
  private long booleanSize;
  private long intSize;
  private long shortSize;
  private long longSize;
  private long floatSize;
  private long doubleSize;

  public long getOopSize()     { return oopSize;     }
  public long getByteSize()    { return byteSize;    }
  public long getCharSize()    { return charSize;    }
  public long getBooleanSize() { return booleanSize; }
  public long getIntSize()     { return intSize;     }
  public long getShortSize()   { return shortSize;   }
  public long getLongSize()    { return longSize;    }
  public long getFloatSize()   { return floatSize;   }
  public long getDoubleSize()  { return doubleSize;  }

  /** an interface to filter objects while walking heap */
  public static interface ObjectFilter {
    public boolean canInclude(Oop obj);
  }

  /** The base heap iteration mechanism */
  public void iterate(HeapVisitor visitor) {
    iterateLiveRegions(collectLiveRegions(), visitor, null);
  }

  /** iterate objects satisfying a specified ObjectFilter */
  public void iterate(HeapVisitor visitor, ObjectFilter of) {
    iterateLiveRegions(collectLiveRegions(), visitor, of);
  }

  /** iterate objects of given Klass. param 'includeSubtypes' tells whether to
   *  include objects of subtypes or not */
  public void iterateObjectsOfKlass(HeapVisitor visitor, final Klass k, boolean includeSubtypes) {
    if (includeSubtypes) {
      if (k.isFinal()) {
        // do the simpler "exact" klass loop
        iterateExact(visitor, k);
      } else {
        iterateSubtypes(visitor, k);
      }
    } else {
      // there can no object of abstract classes and interfaces
      if (!k.isAbstract() && !k.isInterface()) {
        iterateExact(visitor, k);
      }
    }
  }

  /** iterate objects of given Klass (objects of subtypes included) */
  public void iterateObjectsOfKlass(HeapVisitor visitor, final Klass k) {
    iterateObjectsOfKlass(visitor, k, true);
  }

  /** This routine can be used to iterate through the heap at an
      extremely low level (stepping word-by-word) to provide the
      ability to do very low-level debugging */
  public void iterateRaw(RawHeapVisitor visitor) {
    List<Address> liveRegions = collectLiveRegions();

    // Summarize size
    long totalSize = 0;
    for (int i = 0; i < liveRegions.size(); i += 2) {
      Address bottom = (Address) liveRegions.get(i);
      Address top    = (Address) liveRegions.get(i+1);
      totalSize += top.minus(bottom);
    }
    visitor.prologue(totalSize);

    for (int i = 0; i < liveRegions.size(); i += 2) {
      Address bottom = (Address) liveRegions.get(i);
      Address top    = (Address) liveRegions.get(i+1);

      // Traverses the space from bottom to top
      while (bottom.lessThan(top)) {
        visitor.visitAddress(bottom);
        bottom = bottom.addOffsetTo(VM.getVM().getAddressSize());
      }
    }

    visitor.epilogue();
  }

  public boolean isValidMethod(Address handle) {
    try {
      Method m = (Method)Metadata.instantiateWrapperFor(handle);
      return true;
    } catch (Exception e) {
      return false;
  }
  }

  // Creates an instance from the Oop hierarchy based based on the handle
  public Oop newOop(OopHandle handle) {
    // The only known way to detect the right type of an oop is
    // traversing the class chain until a well-known klass is recognized.
    // A more direct solution would require the klasses to expose
    // the C++ vtbl structure.

    // Handle the null reference
    if (handle == null) return null;

    // Then check if obj.klass() is one of the root objects
    Klass klass = Oop.getKlassForOopHandle(handle);
    if (klass != null) {
      if (klass instanceof TypeArrayKlass) return new TypeArray(handle, this);
      if (klass instanceof ObjArrayKlass) return new ObjArray(handle, this);
      if (klass instanceof InstanceKlass) return new Instance(handle, this);
    }

    if (DEBUG) {
      System.err.println("Unknown oop at " + handle);
      System.err.println("Oop's klass is " + klass);
    }

    throw new UnknownOopException();
  }

  // Print all objects in the object heap
  public void print() {
    HeapPrinter printer = new HeapPrinter(System.out);
    iterate(printer);
  }

  //---------------------------------------------------------------------------
  // Internals only below this point
  //

  private void iterateExact(HeapVisitor visitor, final Klass k) {
    iterateLiveRegions(collectLiveRegions(), visitor, new ObjectFilter() {
          public boolean canInclude(Oop obj) {
            Klass tk = obj.getKlass();
            // null Klass is seen sometimes!
            return (tk != null && tk.equals(k));
          }
        });
  }

  private void iterateSubtypes(HeapVisitor visitor, final Klass k) {
    iterateLiveRegions(collectLiveRegions(), visitor, new ObjectFilter() {
          public boolean canInclude(Oop obj) {
            Klass tk = obj.getKlass();
            // null Klass is seen sometimes!
            return (tk != null && tk.isSubtypeOf(k));
          }
        });
  }

  private void iterateLiveRegions(List<Address> liveRegions, HeapVisitor visitor, ObjectFilter of) {
    // Summarize size
    long totalSize = 0;
    for (int i = 0; i < liveRegions.size(); i += 2) {
      Address bottom = (Address) liveRegions.get(i);
      Address top    = (Address) liveRegions.get(i+1);
      totalSize += top.minus(bottom);
    }
    visitor.prologue(totalSize);

    for (int i = 0; i < liveRegions.size(); i += 2) {
      Address bottom = (Address) liveRegions.get(i);
      Address top    = (Address) liveRegions.get(i+1);

      try {
        // Traverses the space from bottom to top
        OopHandle handle = bottom.addOffsetToAsOopHandle(0);

        while (handle.lessThan(top)) {
          Oop obj = null;

          obj = newOop(handle);
          if (obj == null) {
              throw new UnknownOopException();
          }
          if (of == null || of.canInclude(obj)) {
                  if (visitor.doObj(obj)) {
                         // doObj() returns true to abort this loop.
                          break;
                  }
          }

          handle = handle.addOffsetToAsOopHandle(obj.getObjectSize());
        }
      } catch (AddressException | UnknownOopException | WrongTypeException e) {
        // This is okay at the top of these regions
      }
    }

    visitor.epilogue();
  }

  private static class LiveRegionsCollector implements LiveRegionsClosure {
    LiveRegionsCollector(List<Address> l) {
      liveRegions = l;
    }

    @Override
    public void doLiveRegions(LiveRegionsProvider lrp) {
      for (MemRegion reg : lrp.getLiveRegions()) {
        Address top = reg.end();
        Address bottom = reg.start();
        if (Assert.ASSERTS_ENABLED) {
          Assert.that(top != null, "top address in a live region should not be null");
        }
        if (Assert.ASSERTS_ENABLED) {
          Assert.that(bottom != null, "bottom address in a live region should not be null");
        }
        liveRegions.add(top);
        liveRegions.add(bottom);
        if (DEBUG) {
          System.err.println("Live region: " + lrp + ": " + bottom + ", " + top);
      }
    }
  }

     private List<Address> liveRegions;
  }

  // Returns a List<Address> where the addresses come in pairs. These
  // designate the live regions of the heap.
  private List<Address> collectLiveRegions() {
    // We want to iterate through all live portions of the heap, but
    // do not want to abort the heap traversal prematurely if we find
    // a problem (like an allocated but uninitialized object at the
    // top of a generation). To do this we enumerate all generations'
    // bottom and top regions, and factor in TLABs if necessary.

    // Addresses come in pairs.
    List<Address> liveRegions = new ArrayList<>();
    LiveRegionsCollector lrc = new LiveRegionsCollector(liveRegions);

    CollectedHeap heap = VM.getVM().getUniverse().heap();
    heap.liveRegionsIterate(lrc);

    // If UseTLAB is enabled, snip out regions associated with TLABs'
    // dead regions. Note that TLABs can be present in any generation.

    // FIXME: consider adding fewer boundaries to live region list.
    // Theoretically only need to stop at TLAB's top and resume at its
    // end.

    if (VM.getVM().getUseTLAB()) {
      Threads threads = VM.getVM().getThreads();
      for (int i = 0; i < threads.getNumberOfThreads(); i++) {
        JavaThread thread = threads.getJavaThreadAt(i);
        ThreadLocalAllocBuffer tlab = thread.tlab();
        if (tlab.start() != null) {
          if ((tlab.top() == null) || (tlab.end() == null)) {
            System.err.print("Warning: skipping invalid TLAB for thread ");
            thread.printThreadIDOn(System.err);
            System.err.println();
          } else {
            if (DEBUG) {
              System.err.print("TLAB for " + thread.getThreadName() + ", #");
              thread.printThreadIDOn(System.err);
              System.err.print(": ");
              tlab.printOn(System.err);
            }
            // Go from:
            //  - below start() to start()
            //  - start() to top()
            //  - end() and above
            liveRegions.add(tlab.start());
            liveRegions.add(tlab.start());
            liveRegions.add(tlab.top());
            liveRegions.add(tlab.hardEnd());
          }
        }
      }
    }

    // Now sort live regions
    sortLiveRegions(liveRegions);

    if (Assert.ASSERTS_ENABLED) {
      Assert.that(liveRegions.size() % 2 == 0, "Must have even number of region boundaries");
    }

    if (DEBUG) {
      System.err.println("liveRegions:");
      for (int i = 0; i < liveRegions.size(); i += 2) {
          Address bottom = (Address) liveRegions.get(i);
          Address top    = (Address) liveRegions.get(i+1);
          System.err.println(" " + bottom + " - " + top);
      }
    }

    return liveRegions;
  }

  private void sortLiveRegions(List<Address> liveRegions) {
    Collections.sort(liveRegions, new Comparator<Address>() {
        public int compare(Address a1, Address a2) {
          if (AddressOps.lt(a1, a2)) {
            return -1;
          } else if (AddressOps.gt(a1, a2)) {
            return 1;
          }
          return 0;
        }
      });
  }
}
