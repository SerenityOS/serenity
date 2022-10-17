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
#include <Kernel/Thread.h>
#include <Kernel/UserOrKernelBuffer.h>

// Scheduler
namespace Kernel {

void Scheduler::timer_tick(RegisterState const&) {
    // NOTE: This function currently will be called by the TimeManagement code,
    //       so there is no TODO_AARCH64. Instead there will be a linker error when
    //       the Scheduler code is compiled for aarch64.
};

READONLY_AFTER_INIT Thread* g_finalizer;
RecursiveSpinlock g_scheduler_lock { LockRank::None };

void Scheduler::yield()
{
    TODO_AARCH64();
}

void Scheduler::notify_finalizer()
{
    TODO_AARCH64();
}

void Scheduler::add_time_scheduled(u64, bool)
{
    TODO_AARCH64();
}

void Scheduler::enqueue_runnable_thread(Kernel::Thread&)
{
    TODO_AARCH64();
}
}

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

}

// Thread
namespace Kernel {

bool Thread::JoinBlocker::unblock(void*, bool)
{
    TODO_AARCH64();
}

Thread::BlockTimeout::BlockTimeout(bool, const AK::Time*, const AK::Time*, clockid_t)
{
    TODO_AARCH64();
}

Thread::SleepBlocker::SleepBlocker(Kernel::Thread::BlockTimeout const&, AK::Time*)
{
    TODO_AARCH64();
}

Thread::BlockTimeout const& Thread::SleepBlocker::override_timeout(Kernel::Thread::BlockTimeout const&)
{
    TODO_AARCH64();
}

void Thread::SleepBlocker::will_unblock_immediately_without_blocking(UnblockImmediatelyReason)
{
}

void Thread::SleepBlocker::was_unblocked(bool)
{
    TODO_AARCH64();
}

Thread::BlockResult Thread::SleepBlocker::block_result()
{
    TODO_AARCH64();
}

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
