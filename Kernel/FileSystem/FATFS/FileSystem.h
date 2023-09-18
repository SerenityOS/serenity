/*
 * Copyright (c) 2022-2023, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/BlockBasedFileSystem.h>
#include <Kernel/FileSystem/FATFS/Definitions.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Forward.h>
#include <Kernel/Library/KBuffer.h>

namespace Kernel {

class FATFS final : public BlockBasedFileSystem {
    friend FATInode;

public:
    static ErrorOr<NonnullRefPtr<FileSystem>> try_create(OpenFileDescription&, ReadonlyBytes);

    virtual ~FATFS() override = default;
    virtual StringView class_name() const override { return "FATFS"sv; }
    virtual Inode& root_inode() override;
    virtual u8 internal_file_type_to_directory_entry_type(DirectoryEntryView const& entry) const override;

private:
    virtual ErrorOr<void> initialize_while_locked() override;
    virtual bool is_initialized_while_locked() override;
    // FIXME: This is not a proper way to clear last mount of a FAT filesystem,
    // but for now we simply have no other way to properly do it.
    virtual ErrorOr<void> prepare_to_clear_last_mount(Inode&) override { return {}; }

    FATFS(OpenFileDescription&);

    static constexpr u8 signature_1 = 0x28;
    static constexpr u8 signature_2 = 0x29;

    static constexpr u32 fs_info_signature_1 = 0x41615252;
    static constexpr u32 fs_info_signature_2 = 0x61417272;
    static constexpr u32 fs_info_signature_3 = 0xAA550000;

    static constexpr u32 fs_info_data_unknown = 0xFFFFFFFF;

    static constexpr u32 first_data_cluster = 2;

    static constexpr u32 cluster_number_mask = 0x0FFFFFFF;

    FAT32BootRecord const* boot_record() const { return reinterpret_cast<FAT32BootRecord const*>(m_boot_record->data()); }
    FATFSInfo const* fs_info() const { return reinterpret_cast<FATFSInfo const*>(m_fs_info->data()); }

    BlockBasedFileSystem::BlockIndex first_block_of_cluster(u32 cluster) const;
    u32 block_to_cluster(BlockBasedFileSystem::BlockIndex block) const;
    ErrorOr<u32> allocate_cluster();

    ErrorOr<u32> fat_read(u32 cluster);
    ErrorOr<void> fat_write(u32 cluster, u32 value);

    OwnPtr<KBuffer> m_boot_record {};
    OwnPtr<KBuffer> m_fs_info {};
    RefPtr<FATInode> m_root_inode;
    u32 m_first_data_sector { 0 };
};

}
