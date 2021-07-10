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
class Device;
class DiskCache;
class DoubleBuffer;
class File;
class FileDescription;
class FutexQueue;
class IPv4Socket;
class Inode;
class InodeIdentifier;
class SharedInodeVMObject;
class InodeWatcher;
class KBuffer;
class KResult;
class LocalSocket;
class Lock;
class MappedROM;
class MasterPTY;
class PageDirectory;
class PerformanceEventBuffer;
class PhysicalPage;
class PhysicalRegion;
class Process;
class ProcessGroup;
class ThreadTracer;
class Range;
class RangeAllocator;
class Region;
class Scheduler;
class SchedulerPerProcessorData;
class Socket;
class Space;
template<typename BaseType>
class SpinLock;
class RecursiveSpinLock;
template<typename LockType>
class ScopedSpinLock;
class TCPSocket;
class TTY;
class Thread;
class UDPSocket;
class UserOrKernelBuffer;
class VirtualFileSystem;
class VMObject;
class WaitQueue;
class WorkQueue;

template<typename T>
class KResultOr;

}
