/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/Process.h>

namespace Kernel {

uid_t Process::ProcessProcFSTraits::owner_user() const
{
    auto process = m_process.strong_ref();
    if (!process)
        return 0;

    return process->uid();
}

gid_t Process::ProcessProcFSTraits::owner_group() const
{
    auto process = m_process.strong_ref();
    if (!process)
        return 0;

    return process->gid();
}

InodeIndex Process::ProcessProcFSTraits::component_index() const
{
    auto process = m_process.strong_ref();
    if (!process)
        return {};

    return SegmentedProcFSIndex::build_segmented_index_for_pid_directory(process->pid());
}

KResultOr<NonnullRefPtr<Inode>> Process::ProcessProcFSTraits::to_inode(const ProcFS& procfs_instance) const
{
    auto process = m_process.strong_ref();
    if (!process)
        return ESRCH;

    auto maybe_inode = ProcFSProcessDirectoryInode::try_create(procfs_instance, process->pid());
    if (maybe_inode.is_error())
        return maybe_inode.error();
    return maybe_inode.release_value();
}

KResult Process::ProcessProcFSTraits::traverse_as_directory(unsigned fsid, Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    auto process = m_process.strong_ref();
    if (!process)
        return ESRCH;

    callback({ ".", { fsid, SegmentedProcFSIndex::build_segmented_index_for_pid_directory(process->pid()) }, 0 });
    callback({ "..", { fsid, ProcFSComponentRegistry::the().root_directory().component_index() }, 0 });
    callback({ "fd", { fsid, SegmentedProcFSIndex::build_segmented_index_for_sub_directory(process->pid(), SegmentedProcFSIndex::ProcessSubDirectory::FileDescriptions) }, 0 });
    callback({ "stacks", { fsid, SegmentedProcFSIndex::build_segmented_index_for_sub_directory(process->pid(), SegmentedProcFSIndex::ProcessSubDirectory::Stacks) }, 0 });
    callback({ "unveil", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::Unveil) }, 0 });
    callback({ "pledge", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::Pledge) }, 0 });
    callback({ "fds", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::FileDescriptions) }, 0 });
    callback({ "exe", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::BinaryLink) }, 0 });
    callback({ "cwd", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::CurrentWorkDirectoryLink) }, 0 });
    callback({ "perf_events", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::PerformanceEvents) }, 0 });
    callback({ "vm", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::VirtualMemoryStats) }, 0 });
    return KSuccess;
}

}
