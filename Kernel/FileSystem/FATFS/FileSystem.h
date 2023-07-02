/*
 * Copyright (c) 2022, Undefine <undefine@undefine.pl>
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

    static constexpr u32 first_data_cluster = 2;

    FAT32BootRecord const* boot_record() const { return reinterpret_cast<FAT32BootRecord const*>(m_boot_record->data()); }

    BlockBasedFileSystem::BlockIndex first_block_of_cluster(u32 cluster) const;

    OwnPtr<KBuffer> m_boot_record {};
    RefPtr<FATInode> m_root_inode;
    u32 m_first_data_sector { 0 };
};

}
