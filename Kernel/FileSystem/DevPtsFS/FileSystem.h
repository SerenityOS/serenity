/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>

namespace Kernel {

class SlavePTY;
class DevPtsFSInode;

class DevPtsFS final : public FileSystem {
    friend class DevPtsFSInode;

public:
    virtual ~DevPtsFS() override;
    static ErrorOr<NonnullLockRefPtr<FileSystem>> try_create();

    virtual ErrorOr<void> initialize() override;
    virtual StringView class_name() const override { return "DevPtsFS"sv; }

    virtual Inode& root_inode() override;

private:
    DevPtsFS();
    ErrorOr<NonnullLockRefPtr<Inode>> get_inode(InodeIdentifier) const;

    LockRefPtr<DevPtsFSInode> m_root_inode;
};

}
