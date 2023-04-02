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
class ProcFS final : public FileSystem {
    friend class ProcFSInode;
    friend class Process;

public:
    virtual ~ProcFS() override;
    static ErrorOr<NonnullRefPtr<FileSystem>> try_create();

    virtual ErrorOr<void> initialize() override;
    virtual StringView class_name() const override { return "ProcFS"sv; }

    virtual Inode& root_inode() override;

private:
    ProcFS();

    ErrorOr<NonnullRefPtr<Inode>> get_inode(InodeIdentifier) const;

    RefPtr<ProcFSInode> m_root_inode;
};

}
