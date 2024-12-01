/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Devices/FUSEDevice.h>
#include <Kernel/FileSystem/FUSE/FUSEConnection.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/FileSystemSpecificOption.h>
#include <Kernel/FileSystem/Inode.h>

namespace Kernel {

class FUSEInode;

class FUSE final : public FileSystem {
    friend class FUSEInode;

public:
    virtual ~FUSE() override;
    static ErrorOr<NonnullRefPtr<FileSystem>> try_create(FileSystemSpecificOptions const&);

    static ErrorOr<void> validate_mount_unsigned_integer_flag(StringView key, u64);

    virtual ErrorOr<void> initialize() override;
    virtual StringView class_name() const override { return "FUSE"sv; }

    virtual Inode& root_inode() override;

    virtual ErrorOr<void> rename(Inode& old_parent_inode, StringView old_basename, Inode& new_parent_inode, StringView new_basename) override;

private:
    virtual u8 internal_file_type_to_directory_entry_type(DirectoryEntryView const& entry) const override;

    FUSE(NonnullRefPtr<FUSEConnection> connection, u64 rootmode, u64 gid, u64 uid);

    RefPtr<FUSEInode> m_root_inode;
    NonnullRefPtr<FUSEConnection> m_connection;
    u64 m_rootmode { 0 };
    u64 m_gid { 0 };
    u64 m_uid { 0 };
};

}
