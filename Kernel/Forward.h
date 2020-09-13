/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

namespace Kernel {

class BlockDevice;
class CharacterDevice;
class Custody;
class Device;
class DiskCache;
class DoubleBuffer;
class File;
class FileDescription;
class IPv4Socket;
class Inode;
class InodeIdentifier;
class SharedInodeVMObject;
class InodeWatcher;
class KBuffer;
class KResult;
class LocalSocket;
class MappedROM;
class MasterPTY;
class PageDirectory;
class PerformanceEventBuffer;
class PhysicalPage;
class PhysicalRegion;
class Process;
class ThreadTracer;
class Range;
class RangeAllocator;
class Region;
class Scheduler;
class SchedulerPerProcessorData;
class SharedBuffer;
class Socket;
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
class VFS;
class VMObject;
class WaitQueue;

template<typename T>
class KResultOr;

}
