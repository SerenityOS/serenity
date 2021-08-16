/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package jdk.test.lib.jfr;

import jdk.jfr.EventType;

/**
 * Contains id for events that are shipped with the JDK.
 *
 */
public class EventNames {

    public final static String PREFIX = "jdk.";
    private static final String GC_CATEGORY = "GC";

    // JVM Configuration
    public final static String JVMInformation = PREFIX + "JVMInformation";
    public final static String InitialSystemProperty = PREFIX + "InitialSystemProperty";
    public final static String IntFlag = PREFIX + "IntFlag";
    public final static String UnsignedIntFlag = PREFIX + "UnsignedIntFlag";
    public final static String LongFlag = PREFIX + "LongFlag";
    public final static String UnsignedLongFlag = PREFIX + "UnsignedLongFlag";
    public final static String DoubleFlag = PREFIX + "DoubleFlag";
    public final static String BooleanFlag = PREFIX + "BooleanFlag";
    public final static String StringFlag = PREFIX + "StringFlag";
    public final static String IntFlagChanged = PREFIX + "IntFlagChanged";
    public final static String UnsignedIntFlagChanged = PREFIX + "UnsignedIntFlagChanged";
    public final static String LongFlagChanged = PREFIX + "LongFlagChanged";
    public final static String UnsignedLongFlagChanged = PREFIX + "UnsignedLongFlagChanged";
    public final static String DoubleFlagChanged = PREFIX + "DoubleFlagChanged";
    public final static String BooleanFlagChanged = PREFIX + "BooleanFlagChanged";
    public final static String StringFlagChanged = PREFIX + "StringFlagChanged";

    // Runtime
    public final static String ThreadStart = PREFIX + "ThreadStart";
    public final static String ThreadEnd = PREFIX + "ThreadEnd";
    public final static String ThreadSleep = PREFIX + "ThreadSleep";
    public final static String ThreadPark = PREFIX + "ThreadPark";
    public final static String JavaMonitorEnter = PREFIX + "JavaMonitorEnter";
    public final static String JavaMonitorWait = PREFIX + "JavaMonitorWait";
    public final static String JavaMonitorInflate = PREFIX + "JavaMonitorInflate";
    public final static String SyncOnValueBasedClass = PREFIX + "SyncOnValueBasedClass";
    public final static String ClassLoad = PREFIX + "ClassLoad";
    public final static String ClassDefine = PREFIX + "ClassDefine";
    public final static String ClassUnload = PREFIX + "ClassUnload";
    public final static String SafepointBegin = PREFIX + "SafepointBegin";
    public final static String SafepointStateSynchronization = PREFIX + "SafepointStateSynchronization";
    public final static String SafepointCleanup = PREFIX + "SafepointCleanup";
    public final static String SafepointCleanupTask = PREFIX + "SafepointCleanupTask";
    public final static String SafepointEnd = PREFIX + "SafepointEnd";
    public final static String ExecuteVMOperation = PREFIX + "ExecuteVMOperation";
    public final static String Shutdown = PREFIX + "Shutdown";
    public final static String JavaThreadStatistics = PREFIX + "JavaThreadStatistics";
    public final static String ClassLoadingStatistics = PREFIX + "ClassLoadingStatistics";
    public final static String ClassLoaderStatistics = PREFIX + "ClassLoaderStatistics";
    public final static String ThreadAllocationStatistics = PREFIX + "ThreadAllocationStatistics";
    public final static String ExecutionSample = PREFIX + "ExecutionSample";
    public final static String NativeMethodSample = PREFIX + "NativeMethodSample";
    public final static String ThreadDump = PREFIX + "ThreadDump";
    public final static String OldObjectSample = PREFIX + "OldObjectSample";
    public final static String SymbolTableStatistics = PREFIX + "SymbolTableStatistics";
    public final static String StringTableStatistics = PREFIX + "StringTableStatistics";
    public final static String PlaceholderTableStatistics = PREFIX + "PlaceholderTableStatistics";
    public final static String LoaderConstraintsTableStatistics = PREFIX + "LoaderConstraintsTableStatistics";
    public final static String ProtectionDomainCacheTableStatistics = PREFIX + "ProtectionDomainCacheTableStatistics";
    public static final String RedefineClasses = PREFIX + "RedefineClasses";
    public static final String RetransformClasses = PREFIX + "RetransformClasses";
    public static final String ClassRedefinition = PREFIX + "ClassRedefinition";

    // This event is hard to test
    public final static String ReservedStackActivation = PREFIX + "ReservedStackActivation";

    // GC
    public final static String GCHeapSummary = PREFIX + "GCHeapSummary";
    public final static String MetaspaceSummary = PREFIX + "MetaspaceSummary";
    public final static String MetaspaceGCThreshold = PREFIX + "MetaspaceGCThreshold";
    public final static String MetaspaceAllocationFailure = PREFIX + "MetaspaceAllocationFailure";
    public final static String MetaspaceOOM = PREFIX + "MetaspaceOOM";
    public final static String MetaspaceChunkFreeListSummary = PREFIX + "MetaspaceChunkFreeListSummary";
    public final static String PSHeapSummary = PREFIX + "PSHeapSummary";
    public final static String G1HeapSummary = PREFIX + "G1HeapSummary";
    public final static String G1HeapRegionInformation = PREFIX + "G1HeapRegionInformation";
    public final static String G1HeapRegionTypeChange = PREFIX + "G1HeapRegionTypeChange";
    public final static String ShenandoahHeapRegionInformation = PREFIX + "ShenandoahHeapRegionInformation";
    public final static String ShenandoahHeapRegionStateChange = PREFIX + "ShenandoahHeapRegionStateChange";
    public final static String TenuringDistribution = PREFIX + "TenuringDistribution";
    public final static String GarbageCollection = PREFIX + "GarbageCollection";
    public final static String ParallelOldGarbageCollection = PREFIX + "ParallelOldGarbageCollection";
    public final static String ParallelOldCollection = ParallelOldGarbageCollection;
    public final static String YoungGarbageCollection = PREFIX + "YoungGarbageCollection";
    public final static String OldGarbageCollection = PREFIX + "OldGarbageCollection";
    public final static String G1GarbageCollection = PREFIX + "G1GarbageCollection";
    public final static String G1MMU = PREFIX + "G1MMU";
    public final static String EvacuationInformation = PREFIX + "EvacuationInformation";
    public final static String GCReferenceStatistics = PREFIX + "GCReferenceStatistics";
    public final static String ObjectCountAfterGC = PREFIX + "ObjectCountAfterGC";
    public final static String PromoteObjectInNewPLAB = PREFIX + "PromoteObjectInNewPLAB";
    public final static String PromoteObjectOutsidePLAB = PREFIX + "PromoteObjectOutsidePLAB";
    public final static String PromotionFailed = PREFIX + "PromotionFailed";
    public final static String EvacuationFailed = PREFIX + "EvacuationFailed";
    public final static String ConcurrentModeFailure = PREFIX + "ConcurrentModeFailure";
    public final static String GCPhasePause = PREFIX + "GCPhasePause";
    public final static String GCPhasePauseLevel1 = PREFIX + "GCPhasePauseLevel1";
    public final static String GCPhasePauseLevel2 = PREFIX + "GCPhasePauseLevel2";
    public final static String GCPhasePauseLevel3 = PREFIX + "GCPhasePauseLevel3";
    public final static String GCPhasePauseLevel4 = PREFIX + "GCPhasePauseLevel4";
    public final static String ObjectCount = PREFIX + "ObjectCount";
    public final static String GCConfiguration = PREFIX + "GCConfiguration";
    public final static String GCSurvivorConfiguration = PREFIX + "GCSurvivorConfiguration";
    public final static String GCTLABConfiguration = PREFIX + "GCTLABConfiguration";
    public final static String GCHeapConfiguration = PREFIX + "GCHeapConfiguration";
    public final static String YoungGenerationConfiguration = PREFIX + "YoungGenerationConfiguration";
    public final static String G1AdaptiveIHOP = PREFIX + "G1AdaptiveIHOP";
    public final static String G1EvacuationYoungStatistics = PREFIX + "G1EvacuationYoungStatistics";
    public final static String G1EvacuationOldStatistics = PREFIX + "G1EvacuationOldStatistics";
    public final static String G1BasicIHOP = PREFIX + "G1BasicIHOP";
    public final static String AllocationRequiringGC = PREFIX + "AllocationRequiringGC";
    public final static String GCPhaseParallel = PREFIX + "GCPhaseParallel";
    public final static String GCPhaseConcurrent = PREFIX + "GCPhaseConcurrent";
    public final static String GCPhaseConcurrentLevel1 = PREFIX + "GCPhaseConcurrentLevel1";
    public final static String ZAllocationStall = PREFIX + "ZAllocationStall";
    public final static String ZPageAllocation = PREFIX + "ZPageAllocation";
    public final static String ZRelocationSet = PREFIX + "ZRelocationSet";
    public final static String ZRelocationSetGroup = PREFIX + "ZRelocationSetGroup";
    public final static String ZUncommit = PREFIX + "ZUncommit";
    public final static String ZUnmap = PREFIX + "ZUnmap";
    public final static String GCLocker = PREFIX + "GCLocker";
    public static final String SystemGC = PREFIX + "SystemGC";

    // Compiler
    public final static String Compilation = PREFIX + "Compilation";
    public final static String CompilerPhase = PREFIX + "CompilerPhase";
    public final static String CompilationFailure = PREFIX + "CompilationFailure";
    public final static String CompilerInlining = PREFIX + "CompilerInlining";
    public final static String CompilerStatistics = PREFIX + "CompilerStatistics";
    public final static String CompilerConfiguration = PREFIX + "CompilerConfiguration";
    public final static String CodeCacheStatistics = PREFIX + "CodeCacheStatistics";
    public final static String CodeCacheConfiguration = PREFIX + "CodeCacheConfiguration";
    public final static String CodeSweeperStatistics = PREFIX + "CodeSweeperStatistics";
    public final static String CodeSweeperConfiguration = PREFIX + "CodeSweeperConfiguration";
    public final static String SweepCodeCache = PREFIX + "SweepCodeCache";
    public final static String CodeCacheFull = PREFIX + "CodeCacheFull";
    public final static String ObjectAllocationInNewTLAB = PREFIX + "ObjectAllocationInNewTLAB";
    public final static String ObjectAllocationOutsideTLAB = PREFIX + "ObjectAllocationOutsideTLAB";
    public final static String ObjectAllocationSample = PREFIX + "ObjectAllocationSample";
    public final static String Deoptimization = PREFIX + "Deoptimization";

    // OS
    public final static String OSInformation = PREFIX + "OSInformation";
    public final static String VirtualizationInformation = PREFIX + "VirtualizationInformation";
    public final static String CPUInformation = PREFIX + "CPUInformation";
    public final static String CPULoad = PREFIX + "CPULoad";
    public final static String ThreadCPULoad = PREFIX + "ThreadCPULoad";
    public final static String SystemProcess = PREFIX + "SystemProcess";
    public final static String ThreadContextSwitchRate = PREFIX + "ThreadContextSwitchRate";
    public final static String InitialEnvironmentVariable = PREFIX + "InitialEnvironmentVariable";
    public final static String NativeLibrary = PREFIX + "NativeLibrary";
    public final static String PhysicalMemory = PREFIX + "PhysicalMemory";
    public final static String NetworkUtilization = PREFIX + "NetworkUtilization";
    public static final String ProcessStart = PREFIX + "ProcessStart";

    // JDK
    public static final String FileForce  = PREFIX + "FileForce";
    public static final String FileRead = PREFIX + "FileRead";
    public static final String FileWrite = PREFIX + "FileWrite";
    public static final String SocketRead = PREFIX + "SocketRead";
    public static final String SocketWrite = PREFIX + "SocketWrite";
    public final static String ExceptionStatistics = PREFIX + "ExceptionStatistics";
    public final static String JavaExceptionThrow = PREFIX + "JavaExceptionThrow";
    public final static String JavaErrorThrow = PREFIX + "JavaErrorThrow";
    public final static String ModuleRequire = PREFIX + "ModuleRequire";
    public final static String ModuleExport = PREFIX + "ModuleExport";
    public final static String TLSHandshake = PREFIX + "TLSHandshake";
    public final static String X509Certificate = PREFIX + "X509Certificate";
    public final static String X509Validation = PREFIX + "X509Validation";
    public final static String SecurityProperty = PREFIX + "SecurityPropertyModification";
    public final static String DirectBufferStatistics = PREFIX + "DirectBufferStatistics";
    public final static String Deserialization = PREFIX + "Deserialization";

    // Containers
    public static final String ContainerConfiguration = PREFIX + "ContainerConfiguration";
    public static final String ContainerCPUUsage = PREFIX + "ContainerCPUUsage";
    public static final String ContainerCPUThrottling = PREFIX + "ContainerCPUThrottling";
    public static final String ContainerMemoryUsage = PREFIX + "ContainerMemoryUsage";
    public static final String ContainerIOUsage = PREFIX + "ContainerIOUsage";

    // Flight Recorder
    public final static String DumpReason = PREFIX + "DumpReason";
    public final static String DataLoss = PREFIX + "DataLoss";
    public final static String CPUTimeStampCounter = PREFIX + "CPUTimeStampCounter";
    public final static String ActiveRecording = PREFIX + "ActiveRecording";
    public final static String ActiveSetting = PREFIX + "ActiveSetting";
    public static final String Flush = PREFIX + "Flush";

    // Diagnostics
    public static final String HeapDump = PREFIX + "HeapDump";

    public static boolean isGcEvent(EventType et) {
        return et.getCategoryNames().contains(GC_CATEGORY);
    }

}
