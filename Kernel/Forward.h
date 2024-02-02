/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>
#include <Kernel/API/POSIX/sys/types.h>

namespace Kernel {

enum class LockRank;

class BlockDevice;
class CharacterDevice;
class Coredump;
class Credentials;
class CustodyBase;
class Custody;
class Device;
class DeviceControlDevice;
class DiskCache;
class DoubleBuffer;
class File;
class FATInode;
class OpenFileDescription;
class DisplayConnector;
class FileSystem;
class FutexQueue;
class HostnameContext;
class IPv4Socket;
class Inode;
class InodeIdentifier;
class InodeWatcher;
class MountFile;
class KBuffer;
class KString;
class LocalSocket;
class LoopDevice;
class Mutex;
class MasterPTY;
class Mount;
class PerformanceEventBuffer;
class PowerStateSwitchTask;
class ProcFS;
class ProcFSInode;
class Process;
class ProcessGroup;
class RAMFS;
template<LockRank Rank>
class RecursiveSpinlock;
class Scheduler;
class ScopedProcessList;
class Socket;
class StorageManagement;
class SysFS;
class SysFSDirectory;
class SysFSRootDirectory;
class SysFSBusDirectory;
class SysFSDevicesDirectory;
class SysFSDirectoryInode;
class SysFSInode;
class TCPSocket;
class TTY;
class Thread;
class ThreadTracer;
class RAMFSInode;
class UDPSocket;
class UserOrKernelBuffer;
class VFSRootContext;
class WaitQueue;
class WorkQueue;

namespace Memory {
class AddressSpace;
class AnonymousVMObject;
class InodeVMObject;
class MappedROM;
class MemoryManager;
class PageDirectory;
class PhysicalRAMPage;
class PhysicalRegion;
class PrivateInodeVMObject;
class Region;
class SharedInodeVMObject;
class VMObject;
class VirtualRange;
}

template<LockRank Rank>
class Spinlock;
template<typename LockType>
class SpinlockLocker;

struct InodeMetadata;
struct TrapFrame;

AK_TYPEDEF_DISTINCT_ORDERED_ID(pid_t, ProcessID);
AK_TYPEDEF_DISTINCT_ORDERED_ID(pid_t, ThreadID);
AK_TYPEDEF_DISTINCT_ORDERED_ID(pid_t, SessionID);
AK_TYPEDEF_DISTINCT_ORDERED_ID(pid_t, ProcessGroupID);

AK_TYPEDEF_DISTINCT_ORDERED_ID(uid_t, UserID);
AK_TYPEDEF_DISTINCT_ORDERED_ID(gid_t, GroupID);

}
