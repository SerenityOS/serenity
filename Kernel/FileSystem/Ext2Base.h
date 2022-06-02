/*
 * Copyright (c) 2022, iProgramInCpp <iprogramincpp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Kernel::Ext2 {
// The second extended filesystem constants/structures

constexpr u32 ext4_feature_incompat_64bit = (1 << 7);
constexpr u32 max_name_len = 255;
constexpr u16 super_magic = 0xEF53;

constexpr u32 num_direct_blocks = 12;

// FIXME: Give these variables a better name.
constexpr u32 indirect_block = num_direct_blocks;
constexpr u32 dindirect_block = indirect_block + 1;
constexpr u32 tindirect_block = dindirect_block + 1;
constexpr u32 num_blocks = tindirect_block + 1;

// Group descriptor sizes
constexpr size_t min_group_descriptor_size = 32;
constexpr size_t min_group_descriptor_size_64bit = 64;
constexpr size_t max_group_descriptor_size = min_group_descriptor_size;

// Block sizes
constexpr size_t min_block_log_size = 10;
constexpr size_t max_block_log_size = 16;
constexpr size_t min_block_size = (1 << min_block_log_size);
constexpr size_t max_block_size = (1 << max_block_log_size);

// Fragment sizes
constexpr size_t min_fragment_size = min_block_size;
constexpr size_t max_fragment_size = max_block_size;
constexpr size_t min_fragment_log_size = min_block_log_size;

// The size of an inode
constexpr size_t good_old_inode_size = 128;

// This is how many hard links are allowed at maximum.
constexpr u32 link_max = 65000;

enum class FeatureFullCompat : u32 {
    DirPrealloc = (1 << 0),
    ImagicInodes = (1 << 1),
    HasJournal = (1 << 2),
    ExtendedAttrs = (1 << 3),
    ResizeInode = (1 << 4),
    DirIndex = (1 << 5),
};

AK_ENUM_BITWISE_OPERATORS(FeatureFullCompat);

// Special inode numbers
enum class InodeNumbers : u64 {
    Bad = 1,    // Bad blocks inode
    Root,       // Root inode
    ACLIndex,   // ACL inode
    ACLData,    // ACL inode
    BootLoader, // Boot loader inode
    Undelete,   // Undelete directory inode
    Resize,     // Reserved group descriptors inode
    Journal,    // Journal inode

    FirstInode = 11, // First usable inode
};

// File system states
enum class FileSystemState : u32 {
    Valid,  // Cleanly unmounted.
    Error,  // Errors detected
    Orphan, // EXT3 Orphans being recovered
};

// Structure of a block group descriptor
struct GroupDescriptor {
    u32 block_bitmap;
    u32 inode_bitmap;
    u32 inode_table;
    u16 free_blocks_count;
    u16 free_inodes_count;
    u16 used_dirs_count;
    u16 flags;
    u32 reserved[2];
    u16 itable_unused;
    u16 checksum; // crc16(volume_uuid+group_num+group_desc)
};

// Structure of an inode on the disk
struct Inode {
    u16 mode;
    u16 owner_uid; // Low 16 bits of Owner Uid
    u32 file_size;
    u32 access_time;
    u32 create_time;
    u32 modify_time;
    u32 delete_time;
    u16 group_id; // Low 16 bits of Group Id
    u16 num_links;
    u32 num_blocks;
    u32 file_flags;

    // This is Linux dependent
    u32 linux_version;

    u32 block_ptrs[Ext2::num_blocks];
    u32 file_generation;
    u32 file_acl;
    u32 directory_acl;
    u32 fragment_addr;

    // These fields below are also Linux dependent.
    u16 num_blocks_high;
    u16 file_acl_high;
    u16 owner_uid_high;
    u16 group_id_high;
    u32 reserved2;
};

// Revision levels
enum class Revision : u32 {
    Original = 0, // The good old (original) format
    Dynamic = 1,  // V2 format w/ dynamic inode sizes
};

// The structure of the super block
struct SuperBlock {
    u32 num_inodes;
    u32 num_blocks;
    u32 num_reserved_blocks;
    u32 num_free_blocks;
    u32 num_free_inodes;
    u32 first_data_block;
    u32 block_log_size;
    i32 fragment_log_size;
    u32 num_blocks_per_group;
    u32 num_fragments_per_group;
    u32 num_inodes_per_group;
    u32 mount_time;
    u32 write_time;
    u16 num_mounts;
    i16 num_max_mounts;
    u16 magic_number;
    u16 file_system_state;
    u16 error_behavior;
    u16 minor_rev_level;
    u32 last_check_time;
    u32 check_interval;
    u32 os_creator;
    u32 rev_level;
    u16 reserved_block_uid;
    u16 reserved_block_gid;

    // These fields are for EXT2_DYNAMIC_REV superblocks only.

    u32 first_inode_num;
    u16 inode_size_num;
    u16 num_block_group;
    u32 compatible_features;
    u32 incompatible_features;
    u32 ro_compatible_features;
    u8 volume_uuid[16];
    char volume_name[16];
    char last_mounted_dir[64];
    u32 algorithm_usage_bitmap;

    // Performance hints.  Directory preallocation should only
    // happen if the EXT2_FEATURE_COMPAT_DIR_PREALLOC flag is on.
    u8 prealloc_blocks;
    u8 prealloc_dir_blocks;
    u16 reserved_gdt_blocks;

    // Journaling support valid if EXT2_FEATURE_COMPAT_HAS_JOURNAL is set.
    u8 journal_sb_uuid[16];
    u32 journal_inode_num;
    u32 journal_device;
    u32 last_orphan;
    u32 htree_hash_seed[4];
    u8 default_hash_version;
    u8 journal_backup_type;
    u16 group_descriptor_size_num;
    u32 default_mount_options;
    u32 first_meta_block_group;
    u32 fs_create_time;
    u32 journal_inode_backup[17];
    u32 num_blocks_high;
    u32 num_reserved_blocks_high;
    u32 num_free_blocks_high;
    u16 min_extra_inode_size;
    u16 want_extra_inode_size;
    u32 misc_flags;
    u16 raid_stride;
    u16 mmp_interval;
    u64 mmp_block;
    u32 raid_stripe_width;
    u8 log_groups_per_flex;
    u8 reserved_char_pad;
    u16 reserved_pad;  // Padding to next 32bits
    u32 reserved[162]; // Padding to the end of the block

    InodeIndex first_inode() const
    {
        if (rev_level == to_underlying(Revision::Original))
            return to_underlying(Ext2::InodeNumbers::FirstInode);
        else
            return first_inode_num;
    }
    size_t inode_size() const
    {
        if (rev_level == to_underlying(Revision::Original))
            return good_old_inode_size;
        else
            return inode_size_num;
    }
    size_t block_size() const
    {
        return (1 << min_block_log_size) << block_log_size;
    }
    size_t fragment_size() const
    {
        return (1 << min_fragment_log_size) << fragment_log_size;
    }
    // Returns the exponent of the block's size.
    size_t block_size_bits() const
    {
        return block_log_size + min_block_log_size;
    }
    size_t inodes_per_block() const
    {
        return block_size() / inode_size();
    }
    size_t fragments_per_block() const
    {
        return block_size() / fragment_size();
    }
    size_t addresses_per_block() const
    {
        return block_size() / sizeof(u32);
    }
    size_t group_descriptor_size() const
    {
        if (incompatible_features & ext4_feature_incompat_64bit)
            return group_descriptor_size_num;
        else
            return min_group_descriptor_size;
    }
    size_t group_descriptors_per_block() const
    {
        return block_size() / group_descriptor_size();
    }
};

// Ensure the ext2 super block is 1024 bytes in size.
static_assert(sizeof(SuperBlock) == 1024);

// Ext2 file types. Only the low 3 bits are used, other bits are reserved.
enum class FileType : u8 {
    Unknown,     // Unknown file type
    File,        // Regular old file
    Directory,   // Directory
    CharDevice,  // Character device
    BlockDevice, // Block device
    FIFO,        // FIFO
    Socket,      // Socket
    SymLink,     // Symbolic link
    Max          // The maximum file type plus one.
};

struct DirectoryEntry {
    u32 inode;
    u16 entry_size;
    u8 file_name_length;
    FileType file_type;
    char file_name[max_name_len];
};

// Since the max_name_len is 255, and the stuff that comes before
// it is 8 bytes in size, we need to add 1 byte because of padding.
static_assert(sizeof(DirectoryEntry) == 9 + max_name_len);

// This pads a dir entry name's length to be a multiple of 4
constexpr size_t dir_round = (4 - 1);
static inline size_t pad_directory_name_length(size_t length)
{
    return (length + 8 + dir_round) & ~dir_round;
}

}
