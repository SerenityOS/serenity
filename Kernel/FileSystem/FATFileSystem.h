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
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/KBuffer.h>

namespace Kernel {

struct [[gnu::packed]] FAT32BootRecord {
    u8 boot_jump[3];
    char oem_identifier[8];
    u16 bytes_per_sector;
    u8 sectors_per_cluster;
    u16 reserved_sector_count;
    u8 fat_count;
    u16 root_directory_entry_count;
    u16 unused1;
    u8 media_descriptor_type;
    u16 unused2;
    u16 sectors_per_track;
    u16 head_count;
    u32 hidden_sector_count;
    u32 sector_count;
    u32 sectors_per_fat;
    u16 flags;
    u16 fat_version;
    u32 root_directory_cluster;
    u16 fs_info_sector;
    u16 backup_boot_sector;
    u8 unused3[12];
    u8 drive_number;
    u8 unused4;
    u8 signature;
    u32 volume_id;
    char volume_label_string[11];
    char system_identifier_string[8];
};
static_assert(sizeof(FAT32BootRecord) == 90);

enum class FATAttributes : u8 {
    ReadOnly = 0x01,
    Hidden = 0x02,
    System = 0x04,
    VolumeID = 0x08,
    Directory = 0x10,
    Archive = 0x20,
    LongFileName = 0x0F
};

AK_ENUM_BITWISE_OPERATORS(FATAttributes);

union FATPackedTime {
    u16 value;
    struct {
        u16 second : 5;
        u16 minute : 6;
        u16 hour : 5;
    };
};
static_assert(sizeof(FATPackedTime) == 2);

union FATPackedDate {
    u16 value;
    struct {
        u16 day : 5;
        u16 month : 4;
        u16 year : 7;
    };
};
static_assert(sizeof(FATPackedDate) == 2);

struct [[gnu::packed]] FATEntry {
    char filename[8];
    char extension[3];
    FATAttributes attributes;
    u8 unused1;
    u8 creation_time_seconds;
    FATPackedTime creation_time;
    FATPackedDate creation_date;
    FATPackedDate last_accessed_date;
    u16 first_cluster_high;
    FATPackedTime modification_time;
    FATPackedDate modification_date;
    u16 first_cluster_low;
    u32 file_size;
};
static_assert(sizeof(FATEntry) == 32);

struct [[gnu::packed]] FATLongFileNameEntry {
    u8 entry_index;
    u16 characters1[5];
    FATAttributes attributes;
    u8 entry_type;
    u8 checksum;
    u16 characters2[6];
    u16 zero;
    u16 characters3[2];
};
static_assert(sizeof(FATLongFileNameEntry) == 32);

class FATInode;

class FATFS final : public BlockBasedFileSystem {
    friend FATInode;

public:
    static ErrorOr<NonnullLockRefPtr<FileSystem>> try_create(OpenFileDescription&);

    virtual ~FATFS() override = default;
    virtual ErrorOr<void> initialize() override;
    virtual StringView class_name() const override { return "FATFS"sv; }
    virtual Inode& root_inode() override;

private:
    FATFS(OpenFileDescription&);

    static constexpr u8 signature_1 = 0x28;
    static constexpr u8 signature_2 = 0x29;

    static constexpr u32 first_data_cluster = 2;

    FAT32BootRecord const* boot_record() const { return reinterpret_cast<FAT32BootRecord const*>(m_boot_record->data()); };

    BlockBasedFileSystem::BlockIndex first_block_of_cluster(u32 cluster) const;

    OwnPtr<KBuffer> m_boot_record;
    LockRefPtr<FATInode> m_root_inode;
    u32 m_first_data_sector;
};

class FATInode final : public Inode {
    friend FATFS;

public:
    virtual ~FATInode() override = default;

    static ErrorOr<NonnullLockRefPtr<FATInode>> create(FATFS&, FATEntry, Vector<FATLongFileNameEntry> const& = {});

    FATFS& fs() { return static_cast<FATFS&>(Inode::fs()); }
    FATFS const& fs() const { return static_cast<FATFS const&>(Inode::fs()); }

private:
    FATInode(FATFS&, FATEntry, NonnullOwnPtr<KString> filename);

    static constexpr u32 no_more_clusters = 0x0FFFFFF8;
    static constexpr u32 cluster_number_mask = 0x0FFFFFFF;

    static constexpr u8 end_entry_byte = 0x00;
    static constexpr u8 unused_entry_byte = 0xE5;

    static constexpr u8 lfn_entry_text_termination = 0xFF;

    static constexpr u16 first_fat_year = 1980;

    static constexpr u8 normal_filename_length = 8;
    static constexpr u8 normal_extension_length = 3;

    static ErrorOr<NonnullOwnPtr<KString>> compute_filename(FATEntry&, Vector<FATLongFileNameEntry> const& = {});
    static StringView byte_terminated_string(StringView, u8);
    static time_t fat_date_time(FATPackedDate date, FATPackedTime time);

    ErrorOr<Vector<BlockBasedFileSystem::BlockIndex>> compute_block_list();
    ErrorOr<NonnullOwnPtr<KBuffer>> read_block_list();
    ErrorOr<LockRefPtr<FATInode>> traverse(Function<ErrorOr<bool>(LockRefPtr<FATInode>)> callback);
    u32 first_cluster() const;

    // ^Inode
    virtual ErrorOr<size_t> write_bytes_locked(off_t, size_t, UserOrKernelBuffer const& data, OpenFileDescription*) override;
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;

    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> lookup(StringView name) override;
    virtual ErrorOr<NonnullLockRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual ErrorOr<void> add_child(Inode&, StringView name, mode_t) override;
    virtual ErrorOr<void> remove_child(StringView name) override;
    virtual ErrorOr<void> chmod(mode_t) override;
    virtual ErrorOr<void> chown(UserID, GroupID) override;
    virtual ErrorOr<void> flush_metadata() override;

    Vector<BlockBasedFileSystem::BlockIndex> m_block_list;
    FATEntry m_entry;
    NonnullOwnPtr<KString> m_filename;
    InodeMetadata m_metadata;
};

}
