/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 */

package jdk.test.whitebox;

import java.lang.management.MemoryUsage;
import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.security.BasicPermission;
import java.util.Objects;

import jdk.test.whitebox.parser.DiagnosticCommand;

public class WhiteBox {
  @SuppressWarnings("serial")
  public static class WhiteBoxPermission extends BasicPermission {
    public WhiteBoxPermission(String s) {
      super(s);
    }
  }

  private WhiteBox() {}
  private static final WhiteBox instance = new WhiteBox();
  private static native void registerNatives();

  /**
   * Returns the singleton WhiteBox instance.
   *
   * The returned WhiteBox object should be carefully guarded
   * by the caller, since it can be used to read and write data
   * at arbitrary memory addresses. It must never be passed to
   * untrusted code.
   */
  public synchronized static WhiteBox getWhiteBox() {
    @SuppressWarnings("removal")
    SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      sm.checkPermission(new WhiteBoxPermission("getInstance"));
    }
    return instance;
  }

  static {
    registerNatives();
  }

  // Get the maximum heap size supporting COOPs
  public native long getCompressedOopsMaxHeapSize();
  // Arguments
  public native void printHeapSizes();

  // Memory
  private native long getObjectAddress0(Object o);
  public           long getObjectAddress(Object o) {
    Objects.requireNonNull(o);
    return getObjectAddress0(o);
  }

  public native int  getHeapOopSize();
  public native int  getVMPageSize();
  public native long getVMAllocationGranularity();
  public native long getVMLargePageSize();
  public native long getHeapSpaceAlignment();
  public native long getHeapAlignment();

  private native boolean isObjectInOldGen0(Object o);
  public         boolean isObjectInOldGen(Object o) {
    Objects.requireNonNull(o);
    return isObjectInOldGen0(o);
  }

  private native long getObjectSize0(Object o);
  public         long getObjectSize(Object o) {
    Objects.requireNonNull(o);
    return getObjectSize0(o);
  }

  // Runtime
  // Make sure class name is in the correct format
  public int countAliveClasses(String name) {
    return countAliveClasses0(name.replace('.', '/'));
  }
  private native int countAliveClasses0(String name);

  public boolean isClassAlive(String name) {
    return countAliveClasses(name) != 0;
  }

  public  native int getSymbolRefcount(String name);

  public native boolean deflateIdleMonitors();

  private native boolean isMonitorInflated0(Object obj);
  public         boolean isMonitorInflated(Object obj) {
    Objects.requireNonNull(obj);
    return isMonitorInflated0(obj);
  }

  public native void forceSafepoint();

  private native long getConstantPool0(Class<?> aClass);
  public         long getConstantPool(Class<?> aClass) {
    Objects.requireNonNull(aClass);
    return getConstantPool0(aClass);
  }

  private native int getConstantPoolCacheIndexTag0();
  public         int getConstantPoolCacheIndexTag() {
    return getConstantPoolCacheIndexTag0();
  }

  private native int getConstantPoolCacheLength0(Class<?> aClass);
  public         int getConstantPoolCacheLength(Class<?> aClass) {
    Objects.requireNonNull(aClass);
    return getConstantPoolCacheLength0(aClass);
  }

  private native int remapInstructionOperandFromCPCache0(Class<?> aClass, int index);
  public         int remapInstructionOperandFromCPCache(Class<?> aClass, int index) {
    Objects.requireNonNull(aClass);
    return remapInstructionOperandFromCPCache0(aClass, index);
  }

  private native int encodeConstantPoolIndyIndex0(int index);
  public         int encodeConstantPoolIndyIndex(int index) {
    return encodeConstantPoolIndyIndex0(index);
  }

  // JVMTI
  private native void addToBootstrapClassLoaderSearch0(String segment);
  public         void addToBootstrapClassLoaderSearch(String segment){
    Objects.requireNonNull(segment);
    addToBootstrapClassLoaderSearch0(segment);
  }

  private native void addToSystemClassLoaderSearch0(String segment);
  public         void addToSystemClassLoaderSearch(String segment) {
    Objects.requireNonNull(segment);
    addToSystemClassLoaderSearch0(segment);
  }

  // G1
  public native boolean g1InConcurrentMark();
  public native boolean g1HasRegionsToUncommit();
  private native boolean g1IsHumongous0(Object o);
  public         boolean g1IsHumongous(Object o) {
    Objects.requireNonNull(o);
    return g1IsHumongous0(o);
  }

  private native boolean g1BelongsToHumongousRegion0(long adr);
  public         boolean g1BelongsToHumongousRegion(long adr) {
    if (adr == 0) {
      throw new IllegalArgumentException("adr argument should not be null");
    }
    return g1BelongsToHumongousRegion0(adr);
  }


  private native boolean g1BelongsToFreeRegion0(long adr);
  public         boolean g1BelongsToFreeRegion(long adr) {
    if (adr == 0) {
      throw new IllegalArgumentException("adr argument should not be null");
    }
    return g1BelongsToFreeRegion0(adr);
  }

  public native long    g1NumMaxRegions();
  public native long    g1NumFreeRegions();
  public native int     g1RegionSize();
  public native MemoryUsage g1AuxiliaryMemoryUsage();
  private  native Object[]    parseCommandLine0(String commandline, char delim, DiagnosticCommand[] args);
  public          Object[]    parseCommandLine(String commandline, char delim, DiagnosticCommand[] args) {
    Objects.requireNonNull(args);
    return parseCommandLine0(commandline, delim, args);
  }

  public native int g1ActiveMemoryNodeCount();
  public native int[] g1MemoryNodeIds();

  // Parallel GC
  public native long psVirtualSpaceAlignment();
  public native long psHeapGenerationAlignment();

  /**
   * Enumerates old regions with liveness less than specified and produces some statistics
   * @param liveness percent of region's liveness (live_objects / total_region_size * 100).
   * @return long[3] array where long[0] - total count of old regions
   *                             long[1] - total memory of old regions
   *                             long[2] - lowest estimation of total memory of old regions to be freed (non-full
   *                             regions are not included)
   */
  public native long[] g1GetMixedGCInfo(int liveness);

  // NMT
  public native long NMTMalloc(long size);
  public native void NMTFree(long mem);
  public native long NMTReserveMemory(long size);
  public native long NMTAttemptReserveMemoryAt(long addr, long size);
  public native void NMTCommitMemory(long addr, long size);
  public native void NMTUncommitMemory(long addr, long size);
  public native void NMTReleaseMemory(long addr, long size);
  public native long NMTMallocWithPseudoStack(long size, int index);
  public native long NMTMallocWithPseudoStackAndType(long size, int index, int type);
  public native boolean NMTChangeTrackingLevel();
  public native int NMTGetHashSize();
  public native long NMTNewArena(long initSize);
  public native void NMTFreeArena(long arena);
  public native void NMTArenaMalloc(long arena, long size);

  // Compiler
  public native boolean isC2OrJVMCIIncluded();
  public native boolean isJVMCISupportedByGC();

  public native int     matchesMethod(Executable method, String pattern);
  public native int     matchesInline(Executable method, String pattern);
  public native boolean shouldPrintAssembly(Executable method, int comp_level);
  public native int     deoptimizeFrames(boolean makeNotEntrant);
  public native boolean isFrameDeoptimized(int depth);
  public native void    deoptimizeAll();

  public        boolean isMethodCompiled(Executable method) {
    return isMethodCompiled(method, false /*not osr*/);
  }
  private native boolean isMethodCompiled0(Executable method, boolean isOsr);
  public         boolean isMethodCompiled(Executable method, boolean isOsr){
    Objects.requireNonNull(method);
    return isMethodCompiled0(method, isOsr);
  }
  public        boolean isMethodCompilable(Executable method) {
    return isMethodCompilable(method, -1 /*any*/);
  }
  public        boolean isMethodCompilable(Executable method, int compLevel) {
    return isMethodCompilable(method, compLevel, false /*not osr*/);
  }
  private native boolean isMethodCompilable0(Executable method, int compLevel, boolean isOsr);
  public         boolean isMethodCompilable(Executable method, int compLevel, boolean isOsr) {
    Objects.requireNonNull(method);
    return isMethodCompilable0(method, compLevel, isOsr);
  }
  private native boolean isMethodQueuedForCompilation0(Executable method);
  public         boolean isMethodQueuedForCompilation(Executable method) {
    Objects.requireNonNull(method);
    return isMethodQueuedForCompilation0(method);
  }
  // Determine if the compiler corresponding to the compilation level 'compLevel'
  // and to the compilation context 'compilation_context' provides an intrinsic
  // for the method 'method'. An intrinsic is available for method 'method' if:
  //  - the intrinsic is enabled (by using the appropriate command-line flag) and
  //  - the platform on which the VM is running provides the instructions necessary
  //    for the compiler to generate the intrinsic code.
  //
  // The compilation context is related to using the DisableIntrinsic flag on a
  // per-method level, see hotspot/src/share/vm/compiler/abstractCompiler.hpp
  // for more details.
  public boolean isIntrinsicAvailable(Executable method,
                                      Executable compilationContext,
                                      int compLevel) {
      Objects.requireNonNull(method);
      return isIntrinsicAvailable0(method, compilationContext, compLevel);
  }
  // If usage of the DisableIntrinsic flag is not expected (or the usage can be ignored),
  // use the below method that does not require the compilation context as argument.
  public boolean isIntrinsicAvailable(Executable method, int compLevel) {
      return isIntrinsicAvailable(method, null, compLevel);
  }
  private native boolean isIntrinsicAvailable0(Executable method,
                                               Executable compilationContext,
                                               int compLevel);
  public        int     deoptimizeMethod(Executable method) {
    return deoptimizeMethod(method, false /*not osr*/);
  }
  private native int     deoptimizeMethod0(Executable method, boolean isOsr);
  public         int     deoptimizeMethod(Executable method, boolean isOsr) {
    Objects.requireNonNull(method);
    return deoptimizeMethod0(method, isOsr);
  }
  public        void    makeMethodNotCompilable(Executable method) {
    makeMethodNotCompilable(method, -1 /*any*/);
  }
  public        void    makeMethodNotCompilable(Executable method, int compLevel) {
    makeMethodNotCompilable(method, compLevel, false /*not osr*/);
  }
  private native void    makeMethodNotCompilable0(Executable method, int compLevel, boolean isOsr);
  public         void    makeMethodNotCompilable(Executable method, int compLevel, boolean isOsr) {
    Objects.requireNonNull(method);
    makeMethodNotCompilable0(method, compLevel, isOsr);
  }
  public        int     getMethodCompilationLevel(Executable method) {
    return getMethodCompilationLevel(method, false /*not ost*/);
  }
  private native int     getMethodCompilationLevel0(Executable method, boolean isOsr);
  public         int     getMethodCompilationLevel(Executable method, boolean isOsr) {
    Objects.requireNonNull(method);
    return getMethodCompilationLevel0(method, isOsr);
  }
  private native boolean testSetDontInlineMethod0(Executable method, boolean value);
  public         boolean testSetDontInlineMethod(Executable method, boolean value) {
    Objects.requireNonNull(method);
    return testSetDontInlineMethod0(method, value);
  }
  public        int     getCompileQueuesSize() {
    return getCompileQueueSize(-1 /*any*/);
  }
  public native int     getCompileQueueSize(int compLevel);
  private native boolean testSetForceInlineMethod0(Executable method, boolean value);
  public         boolean testSetForceInlineMethod(Executable method, boolean value) {
    Objects.requireNonNull(method);
    return testSetForceInlineMethod0(method, value);
  }
  public        boolean enqueueMethodForCompilation(Executable method, int compLevel) {
    return enqueueMethodForCompilation(method, compLevel, -1 /*InvocationEntryBci*/);
  }
  private native boolean enqueueMethodForCompilation0(Executable method, int compLevel, int entry_bci);
  public  boolean enqueueMethodForCompilation(Executable method, int compLevel, int entry_bci) {
    Objects.requireNonNull(method);
    return enqueueMethodForCompilation0(method, compLevel, entry_bci);
  }
  private native boolean enqueueInitializerForCompilation0(Class<?> aClass, int compLevel);
  public  boolean enqueueInitializerForCompilation(Class<?> aClass, int compLevel) {
    Objects.requireNonNull(aClass);
    return enqueueInitializerForCompilation0(aClass, compLevel);
  }
  private native void    clearMethodState0(Executable method);
  public  native void    markMethodProfiled(Executable method);
  public         void    clearMethodState(Executable method) {
    Objects.requireNonNull(method);
    clearMethodState0(method);
  }
  public native void    lockCompilation();
  public native void    unlockCompilation();
  private native int     getMethodEntryBci0(Executable method);
  public         int     getMethodEntryBci(Executable method) {
    Objects.requireNonNull(method);
    return getMethodEntryBci0(method);
  }
  private native Object[] getNMethod0(Executable method, boolean isOsr);
  public         Object[] getNMethod(Executable method, boolean isOsr) {
    Objects.requireNonNull(method);
    return getNMethod0(method, isOsr);
  }
  public native long    allocateCodeBlob(int size, int type);
  public        long    allocateCodeBlob(long size, int type) {
      int intSize = (int) size;
      if ((long) intSize != size || size < 0) {
          throw new IllegalArgumentException(
                "size argument has illegal value " + size);
      }
      return allocateCodeBlob( intSize, type);
  }
  public native void    freeCodeBlob(long addr);
  public native void    forceNMethodSweep();
  public native Object[] getCodeHeapEntries(int type);
  public native int     getCompilationActivityMode();
  private native long getMethodData0(Executable method);
  public         long getMethodData(Executable method) {
    Objects.requireNonNull(method);
    return getMethodData0(method);
  }
  public native Object[] getCodeBlob(long addr);

  private native void clearInlineCaches0(boolean preserve_static_stubs);
  public void clearInlineCaches() {
    clearInlineCaches0(false);
  }
  public void clearInlineCaches(boolean preserve_static_stubs) {
    clearInlineCaches0(preserve_static_stubs);
  }

  // Intered strings
  public native boolean isInStringTable(String str);

  // Memory
  public native void readReservedMemory();
  public native long allocateMetaspace(ClassLoader classLoader, long size);
  public native long incMetaspaceCapacityUntilGC(long increment);
  public native long metaspaceCapacityUntilGC();
  public native long metaspaceSharedRegionAlignment();

  // Metaspace Arena Tests
  public native long createMetaspaceTestContext(long commit_limit, long reserve_limit);
  public native void destroyMetaspaceTestContext(long context);
  public native void purgeMetaspaceTestContext(long context);
  public native void printMetaspaceTestContext(long context);
  public native long getTotalCommittedWordsInMetaspaceTestContext(long context);
  public native long getTotalUsedWordsInMetaspaceTestContext(long context);
  public native long createArenaInTestContext(long context, boolean is_micro);
  public native void destroyMetaspaceTestArena(long arena);
  public native long allocateFromMetaspaceTestArena(long arena, long word_size);
  public native void deallocateToMetaspaceTestArena(long arena, long p, long word_size);

  public native long maxMetaspaceAllocationSize();

  // Don't use these methods directly
  // Use jdk.test.whitebox.gc.GC class instead.
  public native boolean isGCSupported(int name);
  public native boolean isGCSupportedByJVMCICompiler(int name);
  public native boolean isGCSelected(int name);
  public native boolean isGCSelectedErgonomically();

  // Force Young GC
  public native void youngGC();

  // Force Full GC
  public native void fullGC();

  // Returns true if the current GC supports concurrent collection control.
  public native boolean supportsConcurrentGCBreakpoints();

  private void checkConcurrentGCBreakpointsSupported() {
    if (!supportsConcurrentGCBreakpoints()) {
      throw new UnsupportedOperationException("Concurrent GC breakpoints not supported");
    }
  }

  private native void concurrentGCAcquireControl0();
  private native void concurrentGCReleaseControl0();
  private native void concurrentGCRunToIdle0();
  private native boolean concurrentGCRunTo0(String breakpoint);

  private static boolean concurrentGCIsControlled = false;
  private void checkConcurrentGCIsControlled() {
    if (!concurrentGCIsControlled) {
      throw new IllegalStateException("Not controlling concurrent GC");
    }
  }

  // All collectors supporting concurrent GC breakpoints are expected
  // to provide at least the following breakpoints.
  public final String AFTER_MARKING_STARTED = "AFTER MARKING STARTED";
  public final String BEFORE_MARKING_COMPLETED = "BEFORE MARKING COMPLETED";

  // Collectors supporting concurrent GC breakpoints that do reference
  // processing concurrently should provide the following breakpoint.
  public final String AFTER_CONCURRENT_REFERENCE_PROCESSING_STARTED =
    "AFTER CONCURRENT REFERENCE PROCESSING STARTED";

  public void concurrentGCAcquireControl() {
    checkConcurrentGCBreakpointsSupported();
    if (concurrentGCIsControlled) {
      throw new IllegalStateException("Already controlling concurrent GC");
    }
    concurrentGCAcquireControl0();
    concurrentGCIsControlled = true;
  }

  public void concurrentGCReleaseControl() {
    checkConcurrentGCBreakpointsSupported();
    concurrentGCReleaseControl0();
    concurrentGCIsControlled = false;
  }

  // Keep concurrent GC idle.  Release from breakpoint.
  public void concurrentGCRunToIdle() {
    checkConcurrentGCBreakpointsSupported();
    checkConcurrentGCIsControlled();
    concurrentGCRunToIdle0();
  }

  // Allow concurrent GC to run to breakpoint.
  // Throws IllegalStateException if reached end of cycle first.
  public void concurrentGCRunTo(String breakpoint) {
    concurrentGCRunTo(breakpoint, true);
  }

  // Allow concurrent GC to run to breakpoint.
  // Returns true if reached breakpoint.  If reached end of cycle first,
  // then throws IllegalStateException if errorIfFail is true, returning
  // false otherwise.
  public boolean concurrentGCRunTo(String breakpoint, boolean errorIfFail) {
    checkConcurrentGCBreakpointsSupported();
    checkConcurrentGCIsControlled();
    if (breakpoint == null) {
      throw new NullPointerException("null breakpoint");
    } else if (concurrentGCRunTo0(breakpoint)) {
      return true;
    } else if (errorIfFail) {
      throw new IllegalStateException("Missed requested breakpoint \"" + breakpoint + "\"");
    } else {
      return false;
    }
  }

  // Method tries to start concurrent mark cycle.
  // It returns false if CM Thread is always in concurrent cycle.
  public native boolean g1StartConcMarkCycle();

  // Tests on ReservedSpace/VirtualSpace classes
  public native int stressVirtualSpaceResize(long reservedSpaceSize, long magnitude, long iterations);
  public native void readFromNoaccessArea();
  public native long getThreadStackSize();
  public native long getThreadRemainingStackSize();

  // CPU features
  public native String getCPUFeatures();

  // VM flags
  public native boolean isConstantVMFlag(String name);
  public native boolean isLockedVMFlag(String name);
  public native void    setBooleanVMFlag(String name, boolean value);
  public native void    setIntVMFlag(String name, long value);
  public native void    setUintVMFlag(String name, long value);
  public native void    setIntxVMFlag(String name, long value);
  public native void    setUintxVMFlag(String name, long value);
  public native void    setUint64VMFlag(String name, long value);
  public native void    setSizeTVMFlag(String name, long value);
  public native void    setStringVMFlag(String name, String value);
  public native void    setDoubleVMFlag(String name, double value);
  public native Boolean getBooleanVMFlag(String name);
  public native Long    getIntVMFlag(String name);
  public native Long    getUintVMFlag(String name);
  public native Long    getIntxVMFlag(String name);
  public native Long    getUintxVMFlag(String name);
  public native Long    getUint64VMFlag(String name);
  public native Long    getSizeTVMFlag(String name);
  public native String  getStringVMFlag(String name);
  public native Double  getDoubleVMFlag(String name);
  private final List<Function<String,Object>> flagsGetters = Arrays.asList(
    this::getBooleanVMFlag, this::getIntVMFlag, this::getUintVMFlag,
    this::getIntxVMFlag, this::getUintxVMFlag, this::getUint64VMFlag,
    this::getSizeTVMFlag, this::getStringVMFlag, this::getDoubleVMFlag);

  public Object getVMFlag(String name) {
    return flagsGetters.stream()
                       .map(f -> f.apply(name))
                       .filter(x -> x != null)
                       .findAny()
                       .orElse(null);
  }

  // Jigsaw
  public native void DefineModule(Object module, boolean is_open, String version,
                                  String location, Object[] packages);
  public native void AddModuleExports(Object from_module, String pkg, Object to_module);
  public native void AddReadsModule(Object from_module, Object source_module);
  public native void AddModuleExportsToAllUnnamed(Object module, String pkg);
  public native void AddModuleExportsToAll(Object module, String pkg);

  public native int getOffsetForName0(String name);
  public int getOffsetForName(String name) throws Exception {
    int offset = getOffsetForName0(name);
    if (offset == -1) {
      throw new RuntimeException(name + " not found");
    }
    return offset;
  }
  public native Boolean getMethodBooleanOption(Executable method, String name);
  public native Long    getMethodIntxOption(Executable method, String name);
  public native Long    getMethodUintxOption(Executable method, String name);
  public native Double  getMethodDoubleOption(Executable method, String name);
  public native String  getMethodStringOption(Executable method, String name);
  private final List<BiFunction<Executable,String,Object>> methodOptionGetters
      = Arrays.asList(this::getMethodBooleanOption, this::getMethodIntxOption,
          this::getMethodUintxOption, this::getMethodDoubleOption,
          this::getMethodStringOption);

  public Object getMethodOption(Executable method, String name) {
    return methodOptionGetters.stream()
                              .map(f -> f.apply(method, name))
                              .filter(x -> x != null)
                              .findAny()
                              .orElse(null);
  }

  // Sharing & archiving
  public native String  getDefaultArchivePath();
  public native boolean cdsMemoryMappingFailed();
  public native boolean isSharingEnabled();
  public native boolean isShared(Object o);
  public native boolean isSharedClass(Class<?> c);
  public native boolean areSharedStringsIgnored();
  public native boolean isCDSIncluded();
  public native boolean isJFRIncluded();
  public native boolean isJavaHeapArchiveSupported();
  public native Object  getResolvedReferences(Class<?> c);
  public native void    linkClass(Class<?> c);
  public native boolean areOpenArchiveHeapObjectsMapped();

  // Compiler Directive
  public native int addCompilerDirective(String compDirect);
  public native void removeCompilerDirective(int count);

  // Handshakes
  public native int handshakeWalkStack(Thread t, boolean all_threads);
  public native void asyncHandshakeWalkStack(Thread t);

  public native void lockAndBlock(boolean suspender);

  // Returns true on linux if library has the noexecstack flag set.
  public native boolean checkLibSpecifiesNoexecstack(String libfilename);

  // Container testing
  public native boolean isContainerized();
  public native int validateCgroup(String procCgroups,
                                   String procSelfCgroup,
                                   String procSelfMountinfo);
  public native void printOsInfo();

  // Decoder
  public native void disableElfSectionCache();

  // Resolved Method Table
  public native long resolvedMethodItemsCount();

  // Protection Domain Table
  public native int protectionDomainRemovedCount();

  public native int getKlassMetadataSize(Class<?> c);

  // ThreadSMR GC safety check for threadObj
  public native void checkThreadObjOfTerminatingThread(Thread target);

  // libc name
  public native String getLibcName();

  // Walk stack frames of current thread
  public native void verifyFrames(boolean log, boolean updateRegisterMap);

  public native boolean isJVMTIIncluded();

  public native void waitUnsafe(int time_ms);

  public native void lockCritical();

  public native void unlockCritical();
}
