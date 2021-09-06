/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/Process.h>

namespace Kernel {

UserID Process::ProcessProcFSTraits::owner_user() const
{
    auto process = m_process.strong_ref();
    if (!process)
        return 0;

    return process->uid();
}

GroupID Process::ProcessProcFSTraits::owner_group() const
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

    return TRY(ProcFSProcessDirectoryInode::try_create(procfs_instance, process->pid()));
}

KResult Process::ProcessProcFSTraits::traverse_as_directory(unsigned fsid, Function<bool(FileSystem::DirectoryEntryView const&)> callback) const
{
    auto process = m_process.strong_ref();
    if (!process)
        return ESRCH;

    callback({ ".", { fsid, SegmentedProcFSIndex::build_segmented_index_for_pid_directory(process->pid()) }, DT_DIR });
    callback({ "..", { fsid, ProcFSComponentRegistry::the().root_directory().component_index() }, DT_DIR });
    callback({ "fd", { fsid, SegmentedProcFSIndex::build_segmented_index_for_sub_directory(process->pid(), SegmentedProcFSIndex::ProcessSubDirectory::FileDescriptions) }, DT_DIR });
    callback({ "stacks", { fsid, SegmentedProcFSIndex::build_segmented_index_for_sub_directory(process->pid(), SegmentedProcFSIndex::ProcessSubDirectory::Stacks) }, DT_DIR });
    callback({ "unveil", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::Unveil) }, DT_REG });
    callback({ "pledge", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::Pledge) }, DT_REG });
    callback({ "fds", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::FileDescriptions) }, DT_DIR });
    callback({ "exe", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::BinaryLink) }, DT_LNK });
    callback({ "cwd", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::CurrentWorkDirectoryLink) }, DT_LNK });
    callback({ "perf_events", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::PerformanceEvents) }, DT_REG });
    callback({ "vm", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::VirtualMemoryStats) }, DT_REG });
    return KSuccess;
}

}
