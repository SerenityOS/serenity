/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>

namespace Kernel {

class LoopDevice;
class DevLoopFSInode;

class DevLoopFS final : public FileSystem {
    friend class DevLoopFSInode;

public:
    virtual ~DevLoopFS() override;
    static ErrorOr<NonnullRefPtr<FileSystem>> try_create(ReadonlyBytes);

    virtual ErrorOr<void> initialize() override;
    virtual StringView class_name() const override { return "DevLoopFS"sv; }

    virtual Inode& root_inode() override;

private:
    virtual u8 internal_file_type_to_directory_entry_type(DirectoryEntryView const& entry) const override;

    DevLoopFS();
    ErrorOr<NonnullRefPtr<Inode>> get_inode(InodeIdentifier) const;

    RefPtr<DevLoopFSInode> m_root_inode;
};

}
