/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Forward.h>

namespace Kernel {

class ProcFSInode;
class ProcFSProcessDirectoryInode;
class ProcFSGlobalInode;
class ProcFSAssociatedProcessInode;
class ProcFSProcessSubDirectoryInode;

class ProcFS final : public FileSystem {
    friend class ProcFSInode;
    friend class ProcFSDirectoryInode;
    friend class ProcFSProcessDirectoryInode;
    friend class ProcFSGlobalInode;
    friend class ProcFSAssociatedProcessInode;
    friend class ProcFSProcessSubDirectoryInode;

public:
    virtual ~ProcFS() override;
    static ErrorOr<NonnullLockRefPtr<FileSystem>> try_create();

    virtual ErrorOr<void> initialize() override;
    virtual StringView class_name() const override { return "ProcFS"sv; }

    virtual Inode& root_inode() override;

private:
    ProcFS();

    LockRefPtr<ProcFSDirectoryInode> m_root_inode;
};

}
