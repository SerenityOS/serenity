/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Kernel {

class BlockDevice;
class CharacterDevice;
class CoreDump;
class Custody;
class DevFSDeviceInode;
class DevFSDirectoryInode;
class DevFSInode;
class DevFSPtsDirectoryInode;
class DevFSRootDirectoryInode;
class Device;
class Device;
class DiskCache;
class DoubleBuffer;
class File;
class FileDescription;
class FileSystem;
class FutexQueue;
class IPv4Socket;
class Inode;
class Inode;
class InodeIdentifier;
class InodeWatcher;
class KBuffer;
class KResult;
class LocalSocket;
class Lock;
class MappedROM;
class MasterPTY;
class Mount;
class PageDirectory;
class PerformanceEventBuffer;
class PhysicalPage;
class PhysicalRegion;
class ProcFS;
class ProcFSBusDirectory;
class ProcFSDirectoryInode;
class ProcFSExposedComponent;
class ProcFSExposedDirectory;
class ProcFSInode;
class ProcFSProcessInformation;
class ProcFSRootDirectory;
class ProcFSSystemBoolean;
class ProcFSSystemDirectory;
class Process;
class ProcessGroup;
class Range;
class RangeAllocator;
class RecursiveSpinLock;
class Region;
class Scheduler;
class SchedulerPerProcessorData;
class SharedInodeVMObject;
class Socket;
class Space;
class SysFS;
class SysFSDirectoryInode;
class SysFSInode;
class TCPSocket;
class TTY;
class Thread;
class ThreadTracer;
class UDPSocket;
class UserOrKernelBuffer;
class VMObject;
class VirtualFileSystem;
class VirtualFileSystem;
class WaitQueue;
class WorkQueue;

template<typename BaseType>
class SpinLock;
template<typename LockType>
class ScopedSpinLock;
template<typename T>
class KResultOr;

struct InodeMetadata;
struct TrapFrame;

}
