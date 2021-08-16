/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
import sun.jvm.hotspot.gc.epsilon.*;
import sun.jvm.hotspot.gc.g1.*;
import sun.jvm.hotspot.gc.parallel.*;
import sun.jvm.hotspot.gc.serial.*;
import sun.jvm.hotspot.gc.shenandoah.*;
import sun.jvm.hotspot.gc.shared.*;
import sun.jvm.hotspot.gc.z.*;
import sun.jvm.hotspot.debugger.JVMDebugger;
import sun.jvm.hotspot.memory.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;

public class HeapSummary extends Tool {

   public HeapSummary() {
      super();
   }

   public HeapSummary(JVMDebugger d) {
      super(d);
   }

   public static void main(String[] args) {
      HeapSummary hs = new HeapSummary();
      hs.execute(args);
   }

   @Override
   public String getName() {
      return "heapSummary";
   }

   public void run() {
      CollectedHeap heap = VM.getVM().getUniverse().heap();
      VM.Flag[] flags = VM.getVM().getCommandLineFlags();
      Map<String, VM.Flag> flagMap = new HashMap<>();
      if (flags == null) {
         System.out.println("WARNING: command line flags are not available");
      } else {
         for (int f = 0; f < flags.length; f++) {
            flagMap.put(flags[f].getName(), flags[f]);
         }
      }

      System.out.println();
      printGCAlgorithm(flagMap);
      System.out.println();
      System.out.println("Heap Configuration:");
      printValue("MinHeapFreeRatio         = ", getFlagValue("MinHeapFreeRatio", flagMap));
      printValue("MaxHeapFreeRatio         = ", getFlagValue("MaxHeapFreeRatio", flagMap));
      printValMB("MaxHeapSize              = ", getFlagValue("MaxHeapSize", flagMap));
      printValMB("NewSize                  = ", getFlagValue("NewSize", flagMap));
      printValMB("MaxNewSize               = ", getFlagValue("MaxNewSize", flagMap));
      printValMB("OldSize                  = ", getFlagValue("OldSize", flagMap));
      printValue("NewRatio                 = ", getFlagValue("NewRatio", flagMap));
      printValue("SurvivorRatio            = ", getFlagValue("SurvivorRatio", flagMap));
      printValMB("MetaspaceSize            = ", getFlagValue("MetaspaceSize", flagMap));
      printValMB("CompressedClassSpaceSize = ", getFlagValue("CompressedClassSpaceSize", flagMap));
      printValMB("MaxMetaspaceSize         = ", getFlagValue("MaxMetaspaceSize", flagMap));
      if (heap instanceof ShenandoahHeap) {
         printValMB("ShenandoahRegionSize     = ", ShenandoahHeapRegion.regionSizeBytes());
      } else {
         printValMB("G1HeapRegionSize         = ", HeapRegion.grainBytes());
      }

      System.out.println();
      System.out.println("Heap Usage:");

      if (heap instanceof GenCollectedHeap) {
         GenCollectedHeap genHeap = (GenCollectedHeap) heap;
         for (int n = 0; n < genHeap.nGens(); n++) {
            Generation gen = genHeap.getGen(n);
            if (gen instanceof DefNewGeneration) {
               System.out.println("New Generation (Eden + 1 Survivor Space):");
               printGen(gen);

               ContiguousSpace eden = ((DefNewGeneration)gen).eden();
               System.out.println("Eden Space:");
               printSpace(eden);

               ContiguousSpace from = ((DefNewGeneration)gen).from();
               System.out.println("From Space:");
               printSpace(from);

               ContiguousSpace to = ((DefNewGeneration)gen).to();
               System.out.println("To Space:");
               printSpace(to);
            } else {
               System.out.println(gen.name() + ":");
               printGen(gen);
            }
         }
      } else if (heap instanceof G1CollectedHeap) {
          printG1HeapSummary((G1CollectedHeap)heap);
      } else if (heap instanceof ParallelScavengeHeap) {
         ParallelScavengeHeap psh = (ParallelScavengeHeap) heap;
         PSYoungGen youngGen = psh.youngGen();
         printPSYoungGen(youngGen);

         PSOldGen oldGen = psh.oldGen();
         long oldFree = oldGen.capacity() - oldGen.used();
         System.out.println("PS Old Generation");
         printValMB("capacity = ", oldGen.capacity());
         printValMB("used     = ", oldGen.used());
         printValMB("free     = ", oldFree);
         System.out.println(alignment + (double)oldGen.used() * 100.0 / oldGen.capacity() + "% used");
      } else if (heap instanceof ShenandoahHeap) {
         ShenandoahHeap sh = (ShenandoahHeap) heap;
         long num_regions = sh.numOfRegions();
         System.out.println("Shenandoah Heap:");
         System.out.println("   regions   = " + num_regions);
         printValMB("capacity  = ", num_regions * ShenandoahHeapRegion.regionSizeBytes());
         printValMB("used      = ", sh.used());
         printValMB("committed = ", sh.committed());
      } else if (heap instanceof EpsilonHeap) {
         EpsilonHeap eh = (EpsilonHeap) heap;
         printSpace(eh.space());
      } else if (heap instanceof ZCollectedHeap) {
         ZCollectedHeap zheap = (ZCollectedHeap) heap;
         zheap.printOn(System.out);
      } else {
         throw new RuntimeException("unknown CollectedHeap type : " + heap.getClass());
      }

      System.out.println();
   }

   // Helper methods

   private void printGCAlgorithm(Map flagMap) {
       long l = getFlagValue("UseTLAB", flagMap);
       if (l == 1L) {
          System.out.println("using thread-local object allocation.");
       }

       l = getFlagValue("UseParallelGC", flagMap);
       if (l == 1L) {
          System.out.print("Parallel GC ");
          l = getFlagValue("ParallelGCThreads", flagMap);
          System.out.println("with " + l + " thread(s)");
          return;
       }

       l = getFlagValue("UseG1GC", flagMap);
       if (l == 1L) {
           System.out.print("Garbage-First (G1) GC ");
           l = getFlagValue("ParallelGCThreads", flagMap);
           System.out.println("with " + l + " thread(s)");
           return;
       }

       l = getFlagValue("UseEpsilonGC", flagMap);
       if (l == 1L) {
           System.out.println("Epsilon (no-op) GC");
           return;
       }

       l = getFlagValue("UseZGC", flagMap);
       if (l == 1L) {
           System.out.print("ZGC ");
           l = getFlagValue("ParallelGCThreads", flagMap);
           System.out.println("with " + l + " thread(s)");
           return;
       }

       l = getFlagValue("UseShenandoahGC", flagMap);
       if (l == 1L) {
           System.out.print("Shenandoah GC ");
           l = getFlagValue("ParallelGCThreads", flagMap);
           System.out.println("with " + l + " thread(s)");
           return;
       }

       System.out.println("Mark Sweep Compact GC");
   }

   private void printPSYoungGen(PSYoungGen youngGen) {
      System.out.println("PS Young Generation");
      MutableSpace eden = youngGen.edenSpace();
      System.out.println("Eden Space:");
      printMutableSpace(eden);
      MutableSpace from = youngGen.fromSpace();
      System.out.println("From Space:");
      printMutableSpace(from);
      MutableSpace to = youngGen.toSpace();
      System.out.println("To Space:");
      printMutableSpace(to);
   }

   private void printMutableSpace(MutableSpace space) {
      printValMB("capacity = ", space.capacity());
      printValMB("used     = ", space.used());
      long free = space.capacity() - space.used();
      printValMB("free     = ", free);
      System.out.println(alignment + (double)space.used() * 100.0 / space.capacity() + "% used");
   }

   private static String alignment = "   ";

   private void printGen(Generation gen) {
      printValMB("capacity = ", gen.capacity());
      printValMB("used     = ", gen.used());
      printValMB("free     = ", gen.free());
      System.out.println(alignment + (double)gen.used() * 100.0 / gen.capacity() + "% used");
   }

   private void printSpace(ContiguousSpace space) {
      printValMB("capacity = ", space.capacity());
      printValMB("used     = ", space.used());
      printValMB("free     = ", space.free());
      System.out.println(alignment +  (double)space.used() * 100.0 / space.capacity() + "% used");
   }

   public void printG1HeapSummary(G1CollectedHeap g1h) {
      printG1HeapSummary(System.out, g1h);
   }

   public void printG1HeapSummary(PrintStream tty, G1CollectedHeap g1h) {
      G1MonitoringSupport monitoringSupport = g1h.monitoringSupport();
      long edenSpaceRegionNum = monitoringSupport.edenSpaceRegionNum();
      long survivorSpaceRegionNum = monitoringSupport.survivorSpaceRegionNum();
      HeapRegionSetBase oldSet = g1h.oldSet();
      HeapRegionSetBase archiveSet = g1h.archiveSet();
      HeapRegionSetBase humongousSet = g1h.humongousSet();
      long oldGenRegionNum = oldSet.length() + archiveSet.length() + humongousSet.length();
      printG1Space(tty, "G1 Heap:", g1h.n_regions(),
                   g1h.used(), g1h.capacity());
      tty.println("G1 Young Generation:");
      printG1Space(tty, "Eden Space:", edenSpaceRegionNum,
                   monitoringSupport.edenSpaceUsed(), monitoringSupport.edenSpaceCommitted());
      printG1Space(tty, "Survivor Space:", survivorSpaceRegionNum,
                   monitoringSupport.survivorSpaceUsed(), monitoringSupport.survivorSpaceCommitted());
      printG1Space(tty, "G1 Old Generation:", oldGenRegionNum,
                   monitoringSupport.oldGenUsed(), monitoringSupport.oldGenCommitted());
   }

   private void printG1Space(PrintStream tty, String spaceName, long regionNum,
                             long used, long capacity) {
      long free = capacity - used;
      tty.println(spaceName);
      printValue(tty, "regions  = ", regionNum);
      printValMB(tty, "capacity = ", capacity);
      printValMB(tty, "used     = ", used);
      printValMB(tty, "free     = ", free);
      double occPerc = (capacity > 0) ? (double) used * 100.0 / capacity : 0.0;
      tty.println(alignment + occPerc + "% used");
   }

   private void printValMB(String title, long value) {
      printValMB(System.out, title, value);
   }

   private static final double FACTOR = 1024*1024;
   private void printValMB(PrintStream tty, String title, long value) {
      if (value < 0) {
        tty.println(alignment + title +   (value >>> 20)  + " MB");
      } else {
        double mb = value/FACTOR;
        tty.println(alignment + title + value + " (" + mb + "MB)");
      }
   }

   private void printValue(String title, long value) {
      printValue(System.out, title, value);
   }

   private void printValue(PrintStream tty, String title, long value) {
      tty.println(alignment + title + value);
   }

   private long getFlagValue(String name, Map flagMap) {
      VM.Flag f = (VM.Flag) flagMap.get(name);
      if (f != null) {
         if (f.isBool()) {
            return f.getBool()? 1L : 0L;
         } else if (f.isUIntx() || f.isSizet() || f.isUint64t()) {
            return Long.parseUnsignedLong(f.getValue());
         } else {
            return Long.parseLong(f.getValue());
         }
      } else {
         return -1;
      }
   }
}
