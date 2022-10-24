/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/ProcFS/ProcessAssociatedInode.h>
#include <Kernel/Forward.h>
#include <Kernel/ProcessExposed.h>

namespace Kernel {

class ProcFSProcessSubDirectoryInode final : public ProcFSProcessAssociatedInode {
    friend class ProcFS;

public:
    static ErrorOr<NonnullLockRefPtr<ProcFSProcessSubDirectoryInode>> try_create(ProcFS const&, SegmentedProcFSIndex::ProcessSubDirectory, ProcessID);

private:
    ProcFSProcessSubDirectoryInode(ProcFS const&, SegmentedProcFSIndex::ProcessSubDirectory, ProcessID);
    // ^Inode
    virtual ErrorOr<void> attach(OpenFileDescription& description) override;
    virtual void did_seek(OpenFileDescription&, off_t) override;
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override final;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> lookup(StringView name) override;

    const SegmentedProcFSIndex::ProcessSubDirectory m_sub_directory_type;
};

}
