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

ErrorOr<NonnullRefPtr<Inode>> Process::ProcessProcFSTraits::to_inode(const ProcFS& procfs_instance) const
{
    auto process = m_process.strong_ref();
    if (!process)
        return ESRCH;

    return TRY(ProcFSProcessDirectoryInode::try_create(procfs_instance, process->pid()));
}

ErrorOr<void> Process::ProcessProcFSTraits::traverse_as_directory(FileSystemID fsid, Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    auto process = m_process.strong_ref();
    if (!process)
        return ESRCH;

    TRY(callback({ ".", { fsid, SegmentedProcFSIndex::build_segmented_index_for_pid_directory(process->pid()) }, DT_DIR }));
    TRY(callback({ "..", { fsid, ProcFSComponentRegistry::the().root_directory().component_index() }, DT_DIR }));
    TRY(callback({ "fd", { fsid, SegmentedProcFSIndex::build_segmented_index_for_sub_directory(process->pid(), SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions) }, DT_DIR }));
    TRY(callback({ "stacks", { fsid, SegmentedProcFSIndex::build_segmented_index_for_sub_directory(process->pid(), SegmentedProcFSIndex::ProcessSubDirectory::Stacks) }, DT_DIR }));
    TRY(callback({ "unveil", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::Unveil) }, DT_REG }));
    TRY(callback({ "pledge", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::Pledge) }, DT_REG }));
    TRY(callback({ "fds", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::OpenFileDescriptions) }, DT_DIR }));
    TRY(callback({ "exe", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::BinaryLink) }, DT_LNK }));
    TRY(callback({ "cwd", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::CurrentWorkDirectoryLink) }, DT_LNK }));
    TRY(callback({ "perf_events", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::PerformanceEvents) }, DT_REG }));
    TRY(callback({ "vm", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::VirtualMemoryStats) }, DT_REG }));
    TRY(callback({ "tty", { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::TTYLink) }, DT_LNK }));
    return {};
}

}
