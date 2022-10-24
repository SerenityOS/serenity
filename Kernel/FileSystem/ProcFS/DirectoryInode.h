/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/ProcFS/GlobalInode.h>

namespace Kernel {

class ProcFSDirectoryInode final : public ProcFSGlobalInode {
    friend class ProcFS;

public:
    static ErrorOr<NonnullLockRefPtr<ProcFSDirectoryInode>> try_create(ProcFS const&, ProcFSExposedComponent const&);
    virtual ~ProcFSDirectoryInode() override;

protected:
    ProcFSDirectoryInode(ProcFS const&, ProcFSExposedComponent const&);
    // ^Inode
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> lookup(StringView name) override;
};

}
