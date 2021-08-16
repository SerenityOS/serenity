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

package sun.jvm.hotspot.tools;

import java.io.*;
import java.util.*;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.classfile.*;
import sun.jvm.hotspot.memory.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.utilities.*;

/**
  A command line tool to print class loader statistics.
*/

public class ClassLoaderStats extends Tool {
   boolean verbose = true;

   public ClassLoaderStats() {
      super();
   }

   public ClassLoaderStats(JVMDebugger d) {
      super(d);
   }

   @Override
   public String getName() {
      return "classLoaderStats";
   }

   public static void main(String[] args) {
      ClassLoaderStats cls = new ClassLoaderStats();
      cls.execute(args);
   }

   private static class ClassData {
      Klass klass;
      long  size;

      ClassData(Klass klass, long size) {
         this.klass = klass; this.size = size;
      }
   }

   private static class LoaderData {
      long     numClasses;
      long     classSize;
      List<ClassData> classDetail = new ArrayList<>();
   }

   public void run() {
      printClassLoaderStatistics();
   }

   private void printClassLoaderStatistics() {
      final PrintStream out = System.out;
      final PrintStream err = System.err;
      final Map<Oop, LoaderData> loaderMap = new HashMap<>();
      // loader data for bootstrap class loader
      final LoaderData bootstrapLoaderData = new LoaderData();
      if (verbose) {
         err.print("finding class loader instances ..");
      }

      VM vm = VM.getVM();
      ObjectHeap heap = vm.getObjectHeap();
      Klass classLoaderKlass = vm.getSystemDictionary().getClassLoaderKlass();
      try {
         heap.iterateObjectsOfKlass(new DefaultHeapVisitor() {
                         public boolean doObj(Oop oop) {
                            loaderMap.put(oop, new LoaderData());
                                                        return false;
                         }
                      }, classLoaderKlass);
      } catch (Exception se) {
         se.printStackTrace();
      }

      if (verbose) {
         err.println("done.");
         err.print("computing per loader stat ..");
      }

      ClassLoaderDataGraph cldg = VM.getVM().getClassLoaderDataGraph();
      cldg.classesDo(new ClassLoaderDataGraph.ClassVisitor() {
                        public void visit(Klass k) {
                           if (! (k instanceof InstanceKlass)) {
                              return;
                           }
                           Oop loader = ((InstanceKlass) k).getClassLoader();
                           LoaderData ld = (loader != null) ? (LoaderData)loaderMap.get(loader)
                                                            : bootstrapLoaderData;
                           if (ld != null) {
                              ld.numClasses++;
                              long size = computeSize((InstanceKlass)k);
                              ld.classDetail.add(new ClassData(k, size));
                              ld.classSize += size;
                           }
                        }
                     });

      if (verbose) {
         err.println("done.");
         err.print("please wait.. computing liveness");
      }

      // compute reverse pointer analysis (takes long time for larger app)
      ReversePtrsAnalysis analysis = new ReversePtrsAnalysis();

      if (verbose) {
         analysis.setHeapProgressThunk(new HeapProgressThunk() {
            public void heapIterationFractionUpdate(double fractionOfHeapVisited) {
               err.print('.');
            }
            // This will be called after the iteration is complete
            public void heapIterationComplete() {
               err.println("done.");
            }
         });
      }

      try {
         analysis.run();
      } catch (Exception e) {
         // e.printStackTrace();
         if (verbose)
           err.println("liveness analysis may be inaccurate ...");
      }
      ReversePtrs liveness = VM.getVM().getRevPtrs();

      out.println("class_loader\tclasses\tbytes\tparent_loader\talive?\ttype");
      out.println();

      long numClassLoaders = 1L;
      long totalNumClasses = bootstrapLoaderData.numClasses;
      long totalClassSize  = bootstrapLoaderData.classSize;
      long numAliveLoaders = 1L;
      long numDeadLoaders  = 0L;

      // print bootstrap loader details
      out.print("<bootstrap>");
      out.print('\t');
      out.print(bootstrapLoaderData.numClasses);
      out.print('\t');
      out.print(bootstrapLoaderData.classSize);
      out.print('\t');
      out.print("  null  ");
      out.print('\t');
      // bootstrap loader is always alive
      out.print("live");
      out.print('\t');
      out.println("<internal>");

      for (Iterator keyItr = loaderMap.keySet().iterator(); keyItr.hasNext();) {
         Oop loader = (Oop) keyItr.next();
         LoaderData data = (LoaderData) loaderMap.get(loader);
         numClassLoaders ++;
         totalNumClasses += data.numClasses;
         totalClassSize  += data.classSize;

         out.print(loader.getHandle());
         out.print('\t');
         out.print(data.numClasses);
         out.print('\t');
         out.print(data.classSize);
         out.print('\t');

         class ParentFinder extends DefaultOopVisitor {
            public void doOop(OopField field, boolean isVMField) {
               if (field.getID().getName().equals("parent")) {
                  parent = field.getValue(getObj());
               }
            }
            private Oop parent = null;
            public Oop getParent() { return parent; }
         }

         ParentFinder parentFinder = new ParentFinder();
         loader.iterate(parentFinder, false);
         Oop parent = parentFinder.getParent();
         out.print((parent != null)? parent.getHandle().toString() : "  null  ");
         out.print('\t');
         boolean alive = (liveness != null) ? (liveness.get(loader) != null) : true;
         out.print(alive? "live" : "dead");
         if (alive) numAliveLoaders++; else numDeadLoaders++;
         out.print('\t');
         Klass loaderKlass = loader.getKlass();
         if (loaderKlass != null) {
            out.print(loaderKlass.getName().asString());
            out.print('@');
            out.print(loader.getKlass().getAddress());
         } else {
            out.print("    null!    ");
         }
         out.println();
      }

      out.println();
      // summary line
      out.print("total = ");
      out.print(numClassLoaders);
      out.print('\t');
      out.print(totalNumClasses);
      out.print('\t');
      out.print(totalClassSize);
      out.print('\t');
      out.print("    N/A    ");
      out.print('\t');
      out.print("alive=");
      out.print(numAliveLoaders);
      out.print(", dead=");
      out.print(numDeadLoaders);
      out.print('\t');
      out.print("    N/A    ");
      out.println();
   }

   private static long objectSize(Oop oop) {
      return oop == null ? 0L : oop.getObjectSize();
   }

   // Don't count the shared empty arrays
   private static long arraySize(GenericArray arr) {
     return arr.getLength() != 0L ? arr.getSize() : 0L;
   }

   private long computeSize(InstanceKlass k) {
      long size = 0L;
      // the InstanceKlass object itself
      size += k.getSize();

      // Constant pool
      ConstantPool cp = k.getConstants();
      size += cp.getSize();
      if (cp.getCache() != null) {
        size += cp.getCache().getSize();
      }
      size += arraySize(cp.getTags());

      // Interfaces
      size += arraySize(k.getLocalInterfaces());
      size += arraySize(k.getTransitiveInterfaces());

      // Inner classes
      size += arraySize(k.getInnerClasses());

      // Fields
      size += arraySize(k.getFields());

      // Methods
      MethodArray methods = k.getMethods();
      int nmethods = (int) methods.getLength();
      if (nmethods != 0L) {
         size += methods.getSize();
         for (int i = 0; i < nmethods; ++i) {
            Method m = methods.at(i);
            size += m.getSize();
            size += m.getConstMethod().getSize();
         }
      }

      return size;
   }
}
