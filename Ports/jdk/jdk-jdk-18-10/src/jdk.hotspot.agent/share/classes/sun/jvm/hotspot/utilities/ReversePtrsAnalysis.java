/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.classfile.*;
import sun.jvm.hotspot.gc.shared.*;
import sun.jvm.hotspot.memory.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.utilities.*;

/** For a set of known roots, descends recursively into the object
    graph, for each object recording those objects (and their fields)
    which point to it. NOTE: currently only a subset of the roots
    known to the VM is exposed to the SA: objects on the stack, static
    fields in classes, and JNI handles. These should be most of the
    user-level roots keeping objects alive. */

public class ReversePtrsAnalysis {
  // Used for debugging this code
  private static final boolean DEBUG = false;

  public ReversePtrsAnalysis() {
  }

  /** Sets an optional progress thunk */
  public void setHeapProgressThunk(HeapProgressThunk thunk) {
    progressThunk = thunk;
  }


  /** Runs the analysis algorithm */
  public void run() {
    if (VM.getVM().getRevPtrs() != null) {
      return; // Assume already done
    }

    VM vm = VM.getVM();
    rp = new ReversePtrs();
    vm.setRevPtrs(rp);
    Universe universe = vm.getUniverse();
    CollectedHeap collHeap = universe.heap();
    usedSize = collHeap.used();
    visitedSize = 0;

    // Note that an experiment to iterate the heap linearly rather
    // than in recursive-descent order has been done. It turns out
    // that the recursive-descent algorithm is nearly twice as fast
    // due to the fact that it scans only live objects and (currently)
    // only a fraction of the perm gen, namely the static fields
    // contained in instanceKlasses. (Iterating the heap linearly
    // would also change the semantics of the result so that
    // ReversePtrs.get() would return a non-null value even for dead
    // objects.) Nonetheless, the reverse pointer computation is still
    // quite slow and optimization in field iteration of objects
    // should be done.

    if (progressThunk != null) {
      // Get it started
      progressThunk.heapIterationFractionUpdate(0);
    }

    // Allocate mark bits for heap
    markBits = new MarkBits(collHeap);

    // Get a hold of the object heap
    heap = vm.getObjectHeap();

    // Do each thread's roots
    Threads threads = VM.getVM().getThreads();
    for (int i = 0; i < threads.getNumberOfThreads(); i++) {
      JavaThread thread = threads.getJavaThreadAt(i);
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      thread.printThreadIDOn(new PrintStream(bos));
      String threadDesc =
        " in thread \"" + thread.getThreadName() +
        "\" (id " + bos.toString() + ")";
      doStack(thread,
              new RootVisitor("Stack root" + threadDesc));
      doJNIHandleBlock(thread.activeHandles(),
                       new RootVisitor("JNI handle root" + threadDesc));
    }

    // Do global JNI handles
    JNIHandles handles = VM.getVM().getJNIHandles();
    doOopStorage(handles.globalHandles(),
                 new RootVisitor("Global JNI handle root"));
    doOopStorage(handles.weakGlobalHandles(),
                 new RootVisitor("Weak global JNI handle root"));

    // Do Java-level static fields
    ClassLoaderDataGraph cldg = VM.getVM().getClassLoaderDataGraph();
    cldg.classesDo(new ClassLoaderDataGraph.ClassVisitor() {

            public void visit(Klass k) {
                if (k instanceof InstanceKlass) {
                    final InstanceKlass ik = (InstanceKlass)k;
            ik.iterateStaticFields(
               new DefaultOopVisitor() {
                   public void doOop(OopField field, boolean isVMField) {
                     Oop next = field.getValue(getObj());
                                                   NamedFieldIdentifier nfi = new NamedFieldIdentifier("Static field \"" +
                                                field.getID().getName() +
                                                "\" in class \"" +
                                                                                                       ik.getName().asString() + "\"");
                                                   LivenessPathElement lp = new LivenessPathElement(null, nfi);
                     rp.put(lp, next);
                     try {
                       markAndTraverse(next);
                     } catch (AddressException e) {
                       System.err.print("RevPtrs analysis: WARNING: AddressException at 0x" +
                                        Long.toHexString(e.getAddress()) +
                                        " while traversing static fields of InstanceKlass ");
                       ik.printValueOn(System.err);
                       System.err.println();
                     } catch (UnknownOopException e) {
                       System.err.println("RevPtrs analysis: WARNING: UnknownOopException while " +
                                          "traversing static fields of InstanceKlass ");
                       ik.printValueOn(System.err);
                       System.err.println();
                     }
                   }
                 });
          }
        }
      });

    if (progressThunk != null) {
      progressThunk.heapIterationComplete();
    }

    // Clear out markBits
    markBits = null;
  }


  //---------------------------------------------------------------------------
  // Internals only below this point
  //
  private HeapProgressThunk   progressThunk;
  private long                usedSize;
  private long                visitedSize;
  private double              lastNotificationFraction;
  private static final double MINIMUM_NOTIFICATION_FRACTION = 0.01;
  private ObjectHeap          heap;
  private MarkBits            markBits;
  private int                 depth; // Debugging only
  private ReversePtrs         rp;

  private void markAndTraverse(OopHandle handle) {
    try {
      markAndTraverse(heap.newOop(handle));
    } catch (AddressException e) {
      System.err.println("RevPtrs analysis: WARNING: AddressException at 0x" +
                         Long.toHexString(e.getAddress()) +
                         " while traversing oop at " + handle);
    } catch (UnknownOopException e) {
      System.err.println("RevPtrs analysis: WARNING: UnknownOopException for " +
                         "oop at " + handle);
    }
  }

  private void printHeader() {
    for (int i = 0; i < depth; i++) {
      System.err.print(" ");
    }
  }

  private void markAndTraverse(final Oop obj) {

    // End of path
    if (obj == null) {
      return;
    }

    // Visited object
    if (!markBits.mark(obj)) {
      return;
    }

    // Root of work list for objects to be visited.  A simple
    // stack for saving new objects to be analyzed.

    final Stack<Oop> workList = new Stack<>();

    // Next object to be visited.
    Oop next = obj;

    try {
      // Node in the list currently being visited.

      while (true) {
        final Oop currObj = next;

        // For the progress meter
        if (progressThunk != null) {
          visitedSize += currObj.getObjectSize();
          double curFrac = (double) visitedSize / (double) usedSize;
          if (curFrac >
              lastNotificationFraction + MINIMUM_NOTIFICATION_FRACTION) {
            progressThunk.heapIterationFractionUpdate(curFrac);
            lastNotificationFraction = curFrac;
          }
        }

        if (DEBUG) {
          ++depth;
          printHeader();
          System.err.println("ReversePtrs.markAndTraverse(" +
              currObj.getHandle() + ")");
        }

        // Iterate over the references in the object.  Do the
        // reverse pointer analysis for each reference.
        // Add the reference to the work-list so that its
        // references will be visited.
        currObj.iterate(new DefaultOopVisitor() {
          public void doOop(OopField field, boolean isVMField) {
            // "field" refers to a reference in currObj
            Oop next = field.getValue(currObj);
            rp.put(new LivenessPathElement(currObj, field.getID()), next);
            if ((next != null) && markBits.mark(next)) {
              workList.push(next);
            }
          }
        }, false);

        if (DEBUG) {
          --depth;
        }

        // Get the next object to visit.
        next = (Oop) workList.pop();
      }
    } catch (EmptyStackException e) {
      // Done
    } catch (NullPointerException e) {
      System.err.println("ReversePtrs: WARNING: " + e +
        " during traversal");
    } catch (Exception e) {
      System.err.println("ReversePtrs: WARNING: " + e +
        " during traversal");
    }
  }


  class RootVisitor implements AddressVisitor {
    RootVisitor(String baseRootDescription) {
      this.baseRootDescription = baseRootDescription;
    }

    public void visitAddress(Address addr) {
      Oop next = heap.newOop(addr.getOopHandleAt(0));
      LivenessPathElement lp = new LivenessPathElement(null,
                                        new NamedFieldIdentifier(baseRootDescription +
                                                                 " @ " + addr));
      rp.put(lp, next);
      markAndTraverse(next);
    }

    public void visitCompOopAddress(Address addr) {
      Oop next = heap.newOop(addr.getCompOopHandleAt(0));
      LivenessPathElement lp = new LivenessPathElement(null,
                                        new NamedFieldIdentifier(baseRootDescription +
                                                                 " @ " + addr));
      rp.put(lp, next);
      markAndTraverse(next);
    }

    private String baseRootDescription;
  }

  // Traverse the roots on a given thread's stack
  private void doStack(JavaThread thread, AddressVisitor oopVisitor) {
    for (StackFrameStream fst = new StackFrameStream(thread); !fst.isDone(); fst.next()) {
      fst.getCurrent().oopsDo(oopVisitor, fst.getRegisterMap());
    }
  }

  // Traverse a JNIHandleBlock
  private void doJNIHandleBlock(JNIHandleBlock handles, AddressVisitor oopVisitor) {
    handles.oopsDo(oopVisitor);
  }

  // Traverse jobjects in global JNIHandles
  private void doOopStorage(OopStorage oopSet, AddressVisitor oopVisitor) {
    oopSet.oopsDo(oopVisitor);
  }
}
