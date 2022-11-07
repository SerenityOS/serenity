/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/ProcFS/ProcessDirectoryInode.h>
#include <Kernel/Process.h>
#include <Kernel/ProcessExposed.h>

namespace Kernel {

UserID Process::ProcessProcFSTraits::owner_user() const
{
    auto process = m_process.strong_ref();
    if (!process)
        return 0;

    auto credentials = process->credentials();
    return credentials->uid();
}

GroupID Process::ProcessProcFSTraits::owner_group() const
{
    auto process = m_process.strong_ref();
    if (!process)
        return 0;

    auto credentials = process->credentials();
    return credentials->gid();
}

InodeIndex Process::ProcessProcFSTraits::component_index() const
{
    auto process = m_process.strong_ref();
    if (!process)
        return {};

    return SegmentedProcFSIndex::build_segmented_index_for_pid_directory(process->pid());
}

ErrorOr<NonnullLockRefPtr<Inode>> Process::ProcessProcFSTraits::to_inode(ProcFS const& procfs_instance) const
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

    TRY(callback({ "."sv, { fsid, SegmentedProcFSIndex::build_segmented_index_for_pid_directory(process->pid()) }, DT_DIR }));
    TRY(callback({ ".."sv, { fsid, ProcFSComponentRegistry::the().root_directory().component_index() }, DT_DIR }));
    TRY(callback({ "fd"sv, { fsid, SegmentedProcFSIndex::build_segmented_index_for_sub_directory(process->pid(), SegmentedProcFSIndex::ProcessSubDirectory::OpenFileDescriptions) }, DT_DIR }));
    TRY(callback({ "stacks"sv, { fsid, SegmentedProcFSIndex::build_segmented_index_for_sub_directory(process->pid(), SegmentedProcFSIndex::ProcessSubDirectory::Stacks) }, DT_DIR }));
    TRY(callback({ "children"sv, { fsid, SegmentedProcFSIndex::build_segmented_index_for_sub_directory(process->pid(), SegmentedProcFSIndex::ProcessSubDirectory::Children) }, DT_DIR }));
    TRY(callback({ "unveil"sv, { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::Unveil) }, DT_REG }));
    TRY(callback({ "pledge"sv, { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::Pledge) }, DT_REG }));
    TRY(callback({ "fds"sv, { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::OpenFileDescriptions) }, DT_DIR }));
    TRY(callback({ "exe"sv, { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::BinaryLink) }, DT_LNK }));
    TRY(callback({ "cwd"sv, { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::CurrentWorkDirectoryLink) }, DT_LNK }));
    TRY(callback({ "perf_events"sv, { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::PerformanceEvents) }, DT_REG }));
    TRY(callback({ "vm"sv, { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::VirtualMemoryStats) }, DT_REG }));
    TRY(callback({ "cmdline"sv, { fsid, SegmentedProcFSIndex::build_segmented_index_for_main_property_in_pid_directory(process->pid(), SegmentedProcFSIndex::MainProcessProperty::CommandLine) }, DT_REG }));
    return {};
}

}
