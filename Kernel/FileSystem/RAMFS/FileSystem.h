/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/initramfs_definitions.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Forward.h>

namespace Kernel {

class RAMFS final : public FileSystem {
    friend class RAMFSInode;

public:
    virtual ~RAMFS() override;

    static ErrorOr<NonnullLockRefPtr<RAMFS>> try_create_as_populated_initramfs(Badge<StorageManagement>, PhysicalAddress initramfs_image_start, PhysicalAddress initramfs_image_end);
    static ErrorOr<NonnullLockRefPtr<FileSystem>> try_create();
    virtual ErrorOr<void> initialize() override;

    virtual StringView class_name() const override { return "RAMFS"sv; }

    virtual bool supports_watchers() const override { return true; }

    virtual Inode& root_inode() override;

private:
    RAMFS();

    ErrorOr<NonnullLockRefPtr<RAMFSInode>> try_create_ramfs_inode_for_initramfs(PhysicalAddress initramfs_image_inodes_data_blocks_section_start, initramfs_inode const& inode, RAMFSInode const& parent_directory_inode);
    ErrorOr<void> populate_initramfs(PhysicalAddress initramfs_image_start, PhysicalAddress initramfs_image_end);

    LockRefPtr<RAMFSInode> m_root_inode;

    // NOTE: We start by assigning InodeIndex of 2, because 0 is invalid and 1
    // is reserved for the root directory inode.
    unsigned m_next_inode_index { 2 };
    unsigned next_inode_index();
};

}
