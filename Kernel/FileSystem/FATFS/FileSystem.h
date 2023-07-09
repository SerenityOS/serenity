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

class DOSBIOSParameterBlock final {
private:
    KBuffer const* const m_boot_record;
    const struct DOS4BIOSParameterBlock* m_dos4_block;
    const struct DOS7BIOSParameterBlock* m_dos7_block;
    const struct DOS3BIOSParameterBlock* m_common_block;

public:
    DOSBIOSParameterBlock(KBuffer const* const boot_record)
        : m_boot_record(boot_record)
        , m_dos4_block(reinterpret_cast<const struct DOS4BIOSParameterBlock*>(boot_record->bytes().offset_pointer(0x024)))
        , m_dos7_block(reinterpret_cast<const struct DOS7BIOSParameterBlock*>(boot_record->bytes().offset_pointer(0x024)))
        , m_common_block(reinterpret_cast<const struct DOS3BIOSParameterBlock*>(boot_record->bytes().data()))
    {
    }

    DOSBIOSParameterBlockVersion bpb_version() const;

    DOS3BIOSParameterBlock const* common_bpb() const;

    DOS4BIOSParameterBlock const* dos4_bpb() const;

    DOS7BIOSParameterBlock const* dos7_bpb() const;

    u16 sectors_per_fat() const;

    u32 sector_count() const;

    u8 signature() const;
};

// Represents a block of contiguous sectors to read. This typically represents a
// cluster, but is also used to define areas of the root directory region.
struct FatBlockSpan {
    BlockBasedFileSystem::BlockIndex start_block;
    size_t number_of_sectors;
};

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

    FatBlockSpan first_block_of_cluster(u32 cluster) const;

    OwnPtr<KBuffer> m_boot_record;
    OwnPtr<DOSBIOSParameterBlock> m_parameter_block;
    RefPtr<FATInode> m_root_inode;
    u32 m_first_data_sector { 0 };
    FATVersion m_fat_version;
};

}
