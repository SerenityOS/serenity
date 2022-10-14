/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/ProcFS/ProcessAssociatedInode.h>
#include <Kernel/Forward.h>

namespace Kernel {

class ProcFSProcessDirectoryInode final : public ProcFSProcessAssociatedInode {
    friend class ProcFS;

public:
    static ErrorOr<NonnullLockRefPtr<ProcFSProcessDirectoryInode>> try_create(ProcFS const&, ProcessID);

private:
    ProcFSProcessDirectoryInode(ProcFS const&, ProcessID);
    // ^Inode
    virtual ErrorOr<void> attach(OpenFileDescription& description) override;
    virtual void did_seek(OpenFileDescription&, off_t) override { }
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override final;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> lookup(StringView name) override;
};

}
