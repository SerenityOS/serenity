/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/SysFS/Inode.h>

namespace Kernel {

class SysFSDirectoryInode : public SysFSInode {
    friend class SysFS;

public:
    static ErrorOr<NonnullRefPtr<SysFSDirectoryInode>> try_create(SysFS const&, SysFSComponent const&);
    virtual ~SysFSDirectoryInode() override;

    SysFS& fs() { return static_cast<SysFS&>(Inode::fs()); }
    SysFS const& fs() const { return static_cast<SysFS const&>(Inode::fs()); }

protected:
    SysFSDirectoryInode(SysFS const&, SysFSComponent const&);
    // ^Inode
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
};

}
