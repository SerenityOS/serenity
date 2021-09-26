/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/Vector.h>
#include <Kernel/FileSystem/BlockBasedFileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/KBuffer.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

enum FATType {
    FAT12,
    FAT16,
    FAT32,
};

class FATFS;
struct FATFSDirectoryEntry;

struct [[gnu::packed]] FATDiskGeometry {
    u8 unused_boot_code[11];  /* Unused boot code */
    u16 bytes_per_sector;     /* Bytes Per Sector */
    u8 sectors_per_cluster;   /* Sectors Per Cluster */
    u16 num_reserved_sectors; /* Number of reserved sectors */
    u8 num_fats;              /* Number of FATs */
    u16 max_root_dir_entries; /* Maximum number of root directory entries */
    u16 total_sector_count;   /* Total sector count */
    u8 ignore1[1];            /* Ignore */
    u16 sectors_per_fat;      /* Number of sectors for each FAT copy */
    u16 sectors_per_track;    /* Sectors per track */
    u16 number_of_heads;      /* Number of heads */
    u8 ignore2[4];            /* Ignore */
    u32 fat32_sector_count;   /* FAT32 Sector Count */
    u8 ignore3[2];            /* Ignore */
    u8 boot_signature;        /* Boot Signature */
    char volume_id[4];        /* Volume ID */
    char volume_label[11];    /* Volume Label */
    char file_system_type[8]; /* File System Type. Not for autodetection */
};
static_assert(sizeof(FATDiskGeometry) == 62);

struct [[gnu::packed]] FATDirectoryEntry {
    unsigned char filename[8];  /* Eight Character Space Padded Name */
    unsigned char extension[3]; /* Three Character Space Padded Extension */
    u8 attributes;              /* Attributes */
    u8 unused1[2];              /* Unused */
    u16 creation_time;          /* Creation Time */
    u16 creation_date;          /* Creation Date */
    u16 last_access_date;       /* Last Access Date */
    u8 unused2[2];              /* Unused in FAT12 */
    u16 last_write_time;        /* Last Write Time */
    u16 last_write_date;        /* Last Write Date */
    u16 first_logical_cluster;  /* First Logical Cluster */
    u32 size;                   /* File Size */
};
static_assert(sizeof(FATDirectoryEntry) == 32);

enum FATAttributeType : u8 {
    ReadOnly = 1,
    Hidden = 2,
    System = 4,
    VolumeLabel = 8,
    Subdirectory = 16,
    Archive = 32,
};

class FATFSInode final : public Inode {
    friend class FATFS;

public:
    virtual ~FATFSInode() override;

    u64 size() const;
    bool is_symlink() const { return false; }
    bool is_directory() const { return m_raw_entry.attributes & FATAttributeType::Subdirectory; }

    // ^Inode (RefCounted magic)
    virtual void one_ref_left() override;

    static RefPtr<FATFSInode> create(FATFS& fs, FATDirectoryEntry entry);

    static u16 fat_date_from_time(time_t);
    static u16 fat_time_from_time(time_t);
    static time_t time_from_fat_date_time(u16 d, u16 t);
    static StringView space_terminated_filename(const unsigned char*, size_t max_length);
    static String dos_filename_from_directory_entry(FATDirectoryEntry&);
    static u8 directory_entry_type_from_attributes(u8);

private:
    // ^Inode
    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual InodeMetadata metadata() const override;
    virtual KResult traverse_as_directory(Function<bool(FileSystem::DirectoryEntryView const&)>) const override;
    virtual KResultOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
    virtual void flush_metadata() override;
    virtual KResultOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer& data, OpenFileDescription*) override;
    virtual KResultOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual KResult add_child(Inode& child, const StringView& name, mode_t) override;
    virtual KResult remove_child(const StringView& name) override;
    virtual KResult set_atime(time_t) override;
    virtual KResult set_ctime(time_t) override;
    virtual KResult set_mtime(time_t) override;
    virtual KResult chmod(mode_t) override;
    virtual KResult chown(UserID, GroupID) override;
    virtual KResult truncate(u64) override;

    KResult write_directory(Vector<FATFSDirectoryEntry>&);
    KResult resize(u64);

    void recalculate_block_list();

    FATFS& fs();
    const FATFS& fs() const;
    FATFSInode(FATFS&, FATDirectoryEntry);

    Vector<BlockBasedFileSystem::BlockIndex> m_block_list;
    FATDirectoryEntry m_raw_entry;
};

class FATFS final : public BlockBasedFileSystem {
    friend class FATFSInode;

public:
    static KResultOr<NonnullRefPtr<FATFS>> try_create(OpenFileDescription&);

    virtual ~FATFS() override;
    virtual KResult initialize() override;

    virtual unsigned total_block_count() const override;
    virtual unsigned free_block_count() const override;

    virtual bool supports_watchers() const override { return false; }

    virtual u8 internal_file_type_to_directory_entry_type(const DirectoryEntryView& entry) const override;

    Vector<BlockBasedFileSystem::BlockIndex> block_chain_from(u16 from) const;
    u16 next_cluster_for(u16 first) const;
    bool is_end_cluster(u16 index) const;
    RefPtr<FATFSInode> create_root();

private:
    explicit FATFS(OpenFileDescription&);

    virtual StringView class_name() const override { return "FATFS"sv; }
    virtual FATFSInode& root_inode() override;
    RefPtr<Inode> get_inode(InodeIdentifier) const;
    KResultOr<NonnullRefPtr<Inode>> create_inode(FATFSInode& parent_inode, const String& name, mode_t, dev_t, uid_t, gid_t);
    KResult create_directory(FATFSInode& parent_inode, const String& name, mode_t, uid_t, gid_t);
    virtual void flush_writes() override;
    BlockBasedFileSystem::BlockIndex block_for_cluster(u16 cluster) const;
    const FATDiskGeometry* disk_geometry() const { return (const FATDiskGeometry*)m_disk_geometry->data(); };
    u8 allocation_table_byte(u16 index) const { return m_allocation_table->data()[index]; };

    FATType detect_fat_type() const;

    RefPtr<FATFSInode> m_root_inode;
    mutable OwnPtr<KBuffer> m_disk_geometry;

    mutable OwnPtr<KBuffer> m_allocation_table;
    u16 m_root_dir_start_sector;
    u16 m_root_dir_num_sectors;
    u16 m_allocation_root_sector;

    FATType m_fat_type;
};

inline FATFS& FATFSInode::fs()
{
    return static_cast<FATFS&>(Inode::fs());
}

inline const FATFS& FATFSInode::fs() const
{
    return static_cast<const FATFS&>(Inode::fs());
}

}
