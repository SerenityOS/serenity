/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/Types.h>

#include <Kernel/FileSystem/Inode.h>
#include <Kernel/KString.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/Memory/SharedInodeVMObject.h>
#include <Kernel/Panic.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/Process.h>
#include <Kernel/Random.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Sections.h>
#include <Kernel/TTY/TTY.h>
#include <Kernel/ThreadTracer.h>
#include <Kernel/TimerQueue.h>
#include <Kernel/UserOrKernelBuffer.h>

// Random
namespace Kernel {

void get_fast_random_bytes(Bytes)
{
    TODO_AARCH64();
}

}

// Process
namespace Kernel {

SpinlockProtected<Process::List>& Process::all_instances()
{
    TODO_AARCH64();
}

bool Process::remove_thread(Kernel::Thread&)
{
    TODO_AARCH64();
}

void Process::finalize()
{
    TODO_AARCH64();
}

LockRefPtr<Process> Process::from_pid(Kernel::ProcessID)
{
    TODO_AARCH64();
}

LockRefPtr<Thread> Process::create_kernel_thread(void (*)(void*), void*, u32, NonnullOwnPtr<KString>, u32, bool)
{
    TODO_AARCH64();
}

bool Process::add_thread(Thread&)
{
    TODO_AARCH64();
}

Process::~Process()
{
    TODO_AARCH64();
}

void Process::unblock_waiters(Thread::WaitBlocker::UnblockFlags, u8)
{
    TODO_AARCH64();
}

ErrorOr<void> Process::send_signal(u8, Process*)
{
    TODO_AARCH64();
}

void Process::terminate_due_to_signal(u8)
{
    TODO_AARCH64();
}

ProcessID Process::allocate_pid()
{
    TODO_AARCH64();
}

NonnullRefPtr<Credentials> Process::credentials() const
{
    TODO_AARCH64();
}

}

// ThreadBlockers
namespace Kernel {

bool Thread::Blocker::setup_blocker()
{
    TODO_AARCH64();
}

void Thread::Blocker::finalize()
{
}

Thread::Blocker::~Blocker()
{
    TODO_AARCH64();
}

void Thread::Blocker::begin_blocking(Badge<Thread>)
{
    TODO_AARCH64();
}

Thread::BlockTimeout::BlockTimeout(bool, Time const*, Time const*, clockid_t)
{
    TODO_AARCH64();
}

bool Thread::JoinBlocker::unblock(void*, bool)
{
    TODO_AARCH64();
}

bool Thread::SignalBlocker::check_pending_signals(bool)
{
    TODO_AARCH64();
}

Thread::SleepBlocker::SleepBlocker(BlockTimeout const&, Time*)
{
    TODO_AARCH64();
}

Thread::BlockResult Thread::SleepBlocker::block_result()
{
    TODO_AARCH64();
}

Thread::BlockTimeout const& Thread::SleepBlocker::override_timeout(BlockTimeout const&)
{
    TODO_AARCH64();
}

void Thread::SleepBlocker::will_unblock_immediately_without_blocking(UnblockImmediatelyReason)
{
    TODO_AARCH64();
}

void Thread::SleepBlocker::was_unblocked(bool)
{
    TODO_AARCH64();
}

bool Thread::WaitQueueBlocker::unblock()
{
    TODO_AARCH64();
}

}

// PerformanceEventBuffer
namespace Kernel {

bool g_profiling_all_threads = false;
PerformanceEventBuffer* g_global_perf_events = nullptr;

ErrorOr<void> PerformanceEventBuffer::append(int, unsigned long, unsigned long, AK::StringView, Kernel::Thread*, unsigned long, u64, ErrorOr<FlatPtr>)
{
    TODO_AARCH64();
}

}

// LockRank
namespace Kernel {

void track_lock_acquire(LockRank) { }
void track_lock_release(LockRank) { }

}

// Inode
namespace Kernel {

static Singleton<SpinlockProtected<Inode::AllInstancesList>> s_all_instances;

SpinlockProtected<Inode::AllInstancesList>& Inode::all_instances()
{
    TODO_AARCH64();
    return s_all_instances;
}

LockRefPtr<Memory::SharedInodeVMObject> Inode::shared_vmobject() const
{
    TODO_AARCH64();
    return LockRefPtr<Memory::SharedInodeVMObject>(nullptr);
}

void Inode::will_be_destroyed()
{
    TODO_AARCH64();
}

ErrorOr<void> Inode::set_shared_vmobject(Memory::SharedInodeVMObject&)
{
    TODO_AARCH64();
    return {};
}

ErrorOr<size_t> Inode::read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const
{
    TODO_AARCH64();
    return 0;
}

ErrorOr<size_t> Inode::write_bytes(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*)
{
    TODO_AARCH64();
    return 0;
}

}

// x86 init

multiboot_module_entry_t multiboot_copy_boot_modules_array[16];
size_t multiboot_copy_boot_modules_count;

extern "C" {
READONLY_AFTER_INIT PhysicalAddress start_of_prekernel_image;
READONLY_AFTER_INIT PhysicalAddress end_of_prekernel_image;
READONLY_AFTER_INIT size_t physical_to_virtual_offset;
// READONLY_AFTER_INIT FlatPtr kernel_mapping_base;
READONLY_AFTER_INIT FlatPtr kernel_load_base;
#if ARCH(X86_64)
READONLY_AFTER_INIT PhysicalAddress boot_pml4t;
#endif
READONLY_AFTER_INIT PhysicalAddress boot_pdpt;
READONLY_AFTER_INIT PhysicalAddress boot_pd0;
READONLY_AFTER_INIT PhysicalAddress boot_pd_kernel;
READONLY_AFTER_INIT Kernel::PageTableEntry* boot_pd_kernel_pt1023;
READONLY_AFTER_INIT char const* kernel_cmdline;
READONLY_AFTER_INIT u32 multiboot_flags;
READONLY_AFTER_INIT multiboot_memory_map_t* multiboot_memory_map;
READONLY_AFTER_INIT size_t multiboot_memory_map_count;
READONLY_AFTER_INIT multiboot_module_entry_t* multiboot_modules;
READONLY_AFTER_INIT size_t multiboot_modules_count;
READONLY_AFTER_INIT PhysicalAddress multiboot_framebuffer_addr;
READONLY_AFTER_INIT u32 multiboot_framebuffer_pitch;
READONLY_AFTER_INIT u32 multiboot_framebuffer_width;
READONLY_AFTER_INIT u32 multiboot_framebuffer_height;
READONLY_AFTER_INIT u8 multiboot_framebuffer_bpp;
READONLY_AFTER_INIT u8 multiboot_framebuffer_type;
}

extern "C" {
FlatPtr kernel_mapping_base;
}
