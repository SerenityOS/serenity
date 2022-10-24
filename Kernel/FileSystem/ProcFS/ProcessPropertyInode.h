/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/ProcFS/ProcessAssociatedInode.h>
#include <Kernel/Forward.h>
#include <Kernel/KBufferBuilder.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/ProcessExposed.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class ProcFSProcessPropertyInode final : public ProcFSProcessAssociatedInode {
    friend class ProcFS;

public:
    static ErrorOr<NonnullLockRefPtr<ProcFSProcessPropertyInode>> try_create_for_file_description_link(ProcFS const&, unsigned, ProcessID);
    static ErrorOr<NonnullLockRefPtr<ProcFSProcessPropertyInode>> try_create_for_thread_stack(ProcFS const&, ThreadID, ProcessID);
    static ErrorOr<NonnullLockRefPtr<ProcFSProcessPropertyInode>> try_create_for_pid_property(ProcFS const&, SegmentedProcFSIndex::MainProcessProperty, ProcessID);
    static ErrorOr<NonnullLockRefPtr<ProcFSProcessPropertyInode>> try_create_for_child_process_link(ProcFS const&, ProcessID, ProcessID);

private:
    ProcFSProcessPropertyInode(ProcFS const&, SegmentedProcFSIndex::MainProcessProperty, ProcessID);
    ProcFSProcessPropertyInode(ProcFS const&, ThreadID, ProcessID);
    ProcFSProcessPropertyInode(ProcFS const&, unsigned, ProcessID);
    ProcFSProcessPropertyInode(ProcFS const&, ProcessID, ProcessID);

    // ^Inode
    virtual ErrorOr<void> attach(OpenFileDescription& description) override;
    virtual void did_seek(OpenFileDescription&, off_t) override;
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override final;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> lookup(StringView name) override final;

    ErrorOr<void> refresh_data(OpenFileDescription& description);
    ErrorOr<void> try_to_acquire_data(Process& process, KBufferBuilder& builder) const;

    const SegmentedProcFSIndex::ProcessSubDirectory m_parent_sub_directory_type;
    union {
        SegmentedProcFSIndex::MainProcessProperty property_type;
        unsigned property_index;
    } m_possible_data;
    mutable Mutex m_refresh_lock;
};
}
