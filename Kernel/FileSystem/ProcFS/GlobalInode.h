/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/ProcFS/FileSystem.h>
#include <Kernel/FileSystem/ProcFS/Inode.h>
#include <Kernel/ProcessExposed.h>

namespace Kernel {

class ProcFSGlobalInode : public ProcFSInode {
    friend class ProcFS;

public:
    static ErrorOr<NonnullLockRefPtr<ProcFSGlobalInode>> try_create(ProcFS const&, ProcFSExposedComponent const&);
    virtual ~ProcFSGlobalInode() override {};
    StringView name() const;

protected:
    ProcFSGlobalInode(ProcFS const&, ProcFSExposedComponent const&);

    // ^Inode
    virtual ErrorOr<void> attach(OpenFileDescription& description) override final;
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override final;
    virtual ErrorOr<size_t> write_bytes_locked(off_t, size_t, UserOrKernelBuffer const& buffer, OpenFileDescription*) override final;
    virtual void did_seek(OpenFileDescription&, off_t) override final;
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> lookup(StringView) override;
    virtual ErrorOr<void> truncate(u64) override final;
    virtual ErrorOr<void> update_timestamps(Optional<Time> atime, Optional<Time> ctime, Optional<Time> mtime) override;

    NonnullLockRefPtr<ProcFSExposedComponent> m_associated_component;
};

}
