/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/Forward.h>
#include <Kernel/Locking/MutexProtected.h>

namespace Kernel {

class SysFS final : public FileSystem {
    friend class SysFSInode;
    friend class SysFSDirectoryInode;

public:
    virtual ~SysFS() override;
    static ErrorOr<NonnullLockRefPtr<FileSystem>> try_create();

    virtual ErrorOr<void> initialize() override;
    virtual StringView class_name() const override { return "SysFS"sv; }

    virtual Inode& root_inode() override;

private:
    SysFS();

    RefPtr<SysFSInode> m_root_inode;
};

}
