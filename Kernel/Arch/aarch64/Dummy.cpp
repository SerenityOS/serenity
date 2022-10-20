/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/Types.h>

#include <Kernel/Coredump.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/KString.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/Memory/SharedInodeVMObject.h>
#include <Kernel/Panic.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/Process.h>
#include <Kernel/ProcessExposed.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Sections.h>
#include <Kernel/TTY/TTY.h>
#include <Kernel/ThreadTracer.h>
#include <Kernel/TimerQueue.h>
#include <Kernel/UserOrKernelBuffer.h>

// Process
char const* asm_signal_trampoline = nullptr;
char const* asm_signal_trampoline_end = nullptr;

namespace Kernel {

ProcessID g_init_pid { 0 };

bool Process::has_tracee_thread(ProcessID)
{
    TODO_AARCH64();
}

ErrorOr<void> Process::exec(NonnullOwnPtr<KString>, NonnullOwnPtrVector<KString>, NonnullOwnPtrVector<KString>, Thread*&, u32&, int)
{
    TODO_AARCH64();
}

}

// OpenFileDescription
namespace Kernel {

OpenFileDescription::~OpenFileDescription()
{
    TODO_AARCH64();
}

ErrorOr<size_t> OpenFileDescription::write(UserOrKernelBuffer const&, size_t)
{
    TODO_AARCH64();
}

}

// Custody
namespace Kernel {

SpinlockProtected<Custody::AllCustodiesList>& Custody::all_instances()
{
    TODO_AARCH64();
}

Custody::~Custody()
{
    TODO_AARCH64();
}

}

// VirtualFileSystem
namespace Kernel {

VirtualFileSystem& VirtualFileSystem::the()
{
    TODO_AARCH64();
}

NonnullRefPtr<Custody> VirtualFileSystem::root_custody()
{
    TODO_AARCH64();
}

ErrorOr<NonnullLockRefPtr<OpenFileDescription>> VirtualFileSystem::open(Credentials const&, StringView, int, mode_t, Custody&, Optional<UidAndGid>)
{
    TODO_AARCH64();
}

}

// ProcFS
namespace Kernel {

ProcFSInode::~ProcFSInode()
{
    TODO_AARCH64();
}

ErrorOr<NonnullLockRefPtr<ProcFSProcessDirectoryInode>> ProcFSProcessDirectoryInode::try_create(ProcFS const&, ProcessID)
{
    TODO_AARCH64();
}

ProcFSComponentRegistry& ProcFSComponentRegistry::the()
{
    TODO_AARCH64();
}

ErrorOr<NonnullLockRefPtr<Inode>> ProcFSInode::create_child(StringView, mode_t, dev_t, UserID, GroupID)
{
    TODO_AARCH64();
}

ErrorOr<void> ProcFSInode::add_child(Inode&, StringView, mode_t)
{
    TODO_AARCH64();
}

ErrorOr<void> ProcFSInode::remove_child(StringView)
{
    TODO_AARCH64();
}

ErrorOr<void> ProcFSInode::chmod(mode_t)
{
    TODO_AARCH64();
}

ErrorOr<void> ProcFSInode::chown(UserID, GroupID)
{
    TODO_AARCH64();
}

ErrorOr<void> ProcFSInode::flush_metadata()
{
    TODO_AARCH64();
}

ErrorOr<void> ProcFSGlobalInode::attach(OpenFileDescription&)
{
    TODO_AARCH64();
}

ErrorOr<size_t> ProcFSGlobalInode::read_bytes_locked(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const
{
    TODO_AARCH64();
}

StringView ProcFSGlobalInode::name() const
{
    TODO_AARCH64();
}

ErrorOr<void> ProcFSGlobalInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const
{
    TODO_AARCH64();
}

ErrorOr<NonnullLockRefPtr<Inode>> ProcFSGlobalInode::lookup(StringView)
{
    TODO_AARCH64();
}

ErrorOr<void> ProcFSGlobalInode::truncate(u64)
{
    TODO_AARCH64();
}

ErrorOr<void> ProcFSGlobalInode::update_timestamps(Optional<time_t>, Optional<time_t>, Optional<time_t>)
{
    TODO_AARCH64();
}

InodeMetadata ProcFSGlobalInode::metadata() const
{
    TODO_AARCH64();
}

void ProcFSGlobalInode::did_seek(OpenFileDescription&, off_t)
{
    TODO_AARCH64();
}

ErrorOr<size_t> ProcFSGlobalInode::write_bytes_locked(off_t, size_t, UserOrKernelBuffer const&, OpenFileDescription*)
{
    TODO_AARCH64();
}

}

// ProcessGroup
namespace Kernel {

ProcessGroup::~ProcessGroup()
{
    TODO_AARCH64();
}

}

// ProcessExposed
namespace Kernel {

ErrorOr<NonnullLockRefPtr<Inode>> ProcFSExposedComponent::to_inode(ProcFS const&) const
{
    TODO_AARCH64();
}

InodeIndex SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(ProcessID, SegmentedProcFSIndex::MainProcessProperty)
{
    TODO_AARCH64();
}

InodeIndex SegmentedProcFSIndex::build_segmented_index_for_pid_directory(ProcessID)
{
    TODO_AARCH64();
}

InodeIndex SegmentedProcFSIndex::build_segmented_index_for_sub_directory(ProcessID, ProcessSubDirectory)
{
    TODO_AARCH64();
}

ProcFSExposedComponent::ProcFSExposedComponent()
{
    TODO_AARCH64();
}

}

// FileSystem
namespace Kernel {

FileSystem::DirectoryEntryView::DirectoryEntryView(StringView, InodeIdentifier, u8)
{
    TODO_AARCH64();
}

}

// Coredump
namespace Kernel {

ErrorOr<NonnullOwnPtr<Coredump>> Coredump::try_create(NonnullLockRefPtr<Process>, StringView)
{
    TODO_AARCH64();
}

ErrorOr<void> Coredump::write()
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

Thread::WaitQueueBlocker::WaitQueueBlocker(WaitQueue& wait_queue, StringView)
    : m_wait_queue(wait_queue)
{
    TODO_AARCH64();
}

bool Thread::WaitQueueBlocker::setup_blocker()
{
    TODO_AARCH64();
}

Thread::WaitQueueBlocker::~WaitQueueBlocker()
{
    TODO_AARCH64();
}

void Thread::WaitBlockerSet::finalize()
{
    TODO_AARCH64();
}

bool Thread::WaitBlockerSet::unblock(Process&, WaitBlocker::UnblockFlags, u8)
{
    TODO_AARCH64();
}

void Thread::WaitBlockerSet::disowned_by_waiter(Process&)
{
    TODO_AARCH64();
}

bool Thread::WaitBlockerSet::should_add_blocker(Blocker&, void*)
{
    TODO_AARCH64();
}

Thread::WaitBlockerSet::ProcessBlockInfo::~ProcessBlockInfo()
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

OwnPtr<PerformanceEventBuffer> PerformanceEventBuffer::try_create_with_size(size_t)
{
    TODO_AARCH64();
}

ErrorOr<void> PerformanceEventBuffer::add_process(Process const&, ProcessEventType)
{
    TODO_AARCH64();
}

ErrorOr<void> PerformanceEventBuffer::to_json(KBufferBuilder&) const
{
    TODO_AARCH64();
}

ErrorOr<void> PerformanceEventBuffer::append_with_ip_and_bp(ProcessID, ThreadID,
    FlatPtr, FlatPtr, int, u32, FlatPtr, FlatPtr, StringView, FlatPtr, u64, ErrorOr<FlatPtr>)
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

ErrorOr<void> Inode::update_timestamps([[maybe_unused]] Optional<time_t> atime, [[maybe_unused]] Optional<time_t> ctime, [[maybe_unused]] Optional<time_t> mtime)
{
    TODO_AARCH64();
}

ErrorOr<void> Inode::increment_link_count()
{
    TODO_AARCH64();
}

ErrorOr<void> Inode::decrement_link_count()
{
    TODO_AARCH64();
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
