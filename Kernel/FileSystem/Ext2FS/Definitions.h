/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Types.h>

// NOTE: Using https://www.nongnu.org/ext2-doc/ext2.html

namespace Kernel::Ext2 {
static constexpr size_t max_block_size = 4096;
static constexpr size_t super_block_offset_on_device = 1024;
static constexpr size_t max_name_length = 255;
static constexpr size_t number_of_direct_blocks = 12;
static constexpr size_t single_indirect_block = 12;
static constexpr size_t double_indirect_blocks = 13;
static constexpr size_t triple_indirect_blocks = 14;
static constexpr size_t number_of_blocks = 15;
static constexpr size_t min_group_size = 32;
static constexpr size_t min_group_size64 = 64;
static constexpr size_t min_block_size = 1024;
static constexpr u16 ext2_magic_number = 0xEF53;

enum InodeNumber {
    Reserved,
    EXT2_BAD_INO,
    EXT2_ROOT_INO,
    EXT2_ACL_IDX_INO,
    EXT2_ACL_DATA_INO,
    EXT2_BOOT_LOADER_INO,
    EXT2_UNDEL_DIR_INO,
    EXT2_RESIZE_INO,
    EXT2_JOURNAL_INO
};

enum FileType {
    EXT2_FT_UNKNOWN,
    EXT2_FT_REG_FILE,
    EXT2_FT_DIR,
    EXT2_FT_CHRDEV,
    EXT2_FT_BLKDEV,
    EXT2_FT_FIFO,
    EXT2_FT_SOCK,
    EXT2_FT_SYMLINK,
    EXT2_FT_MAX
};

enum FileSystemState : u8 {
    EXT2_VALID_FS = 0x0001, /* Unmounted cleanly */
    EXT2_ERROR_FS = 0x0002, /* Errors detected */
    EXT3_ORPHAN_FS = 0x0004 /* Orphans being recovered */
};

enum class FeaturesOptional : u32 {
    None = 0,
    ExtendedAttributes = 0x0008,
};

AK_ENUM_BITWISE_OPERATORS(FeaturesOptional);

enum class FeaturesReadOnly : u32 {
    None = 0,
    SparseSuperblock = 0x0001,
    FileSize64bits = 0x0002,
};

AK_ENUM_BITWISE_OPERATORS(FeaturesReadOnly);

enum class FeaturesIncompat : u32 {
    None = 0,
    EXT4_FEATURE_INCOMPAT_64BIT = 0x0080
};

AK_ENUM_BITWISE_OPERATORS(FeaturesIncompat);

struct GroupDescriptor {
    u32 bg_block_bitmap;      /* Blocks bitmap block */
    u32 bg_inode_bitmap;      /* Inodes bitmap block */
    u32 bg_inode_table;       /* Inodes table block */
    u16 bg_free_blocks_count; /* Free blocks count */
    u16 bg_free_inodes_count; /* Free inodes count */
    u16 bg_used_dirs_count;   /* Directories count */
    u16 bg_flags;
    u32 bg_reserved[2];
    u16 bg_itable_unused; /* Unused inodes count */
    u16 bg_checksum;      /* crc16(s_uuid+group_num+group_desc)*/
};

struct Ext2Inode {
    u16 i_mode;        /* File mode */
    u16 i_uid;         /* Low 16 bits of Owner Uid */
    u32 i_size;        /* Size in bytes */
    u32 i_atime;       /* Access time */
    u32 i_ctime;       /* Inode change time */
    u32 i_mtime;       /* Modification time */
    u32 i_dtime;       /* Deletion Time */
    u16 i_gid;         /* Low 16 bits of Group Id */
    u16 i_links_count; /* Links count */
    u32 i_blocks;      /* Blocks count */
    u32 i_flags;       /* File flags */
    union {
        struct {
            u32 l_i_version; /* was l_i_reserved1 */
        } linux1;
        struct {
            u32 h_i_translator;
        } hurd1;
    } osd1;                        /* OS dependent 1 */
    u32 i_block[number_of_blocks]; /* Pointers to blocks */
    u32 i_generation;              /* File version (for NFS) */
    u32 i_file_acl;                /* File ACL */
    u32 i_dir_acl;                 /* Directory ACL */
    u32 i_faddr;                   /* Fragment address */
    union {
        struct {
            u16 l_i_blocks_hi;
            u16 l_i_file_acl_high;
            u16 l_i_uid_high; /* these 2 fields    */
            u16 l_i_gid_high; /* were reserved2[0] */
            u32 l_i_reserved2;
        } linux2;
        struct {
            u8 h_i_frag;  /* Fragment number */
            u8 h_i_fsize; /* Fragment size */
            u16 h_i_mode_high;
            u16 h_i_uid_high;
            u16 h_i_gid_high;
            u32 h_i_author;
        } hurd2;
    } osd2; /* OS dependent 2 */
};

struct SuperBlock {
    u32 s_inodes_count;             /* Inodes count */
    u32 s_blocks_count;             /* Blocks count */
    u32 s_r_blocks_count;           /* Reserved blocks count */
    u32 s_free_blocks_count;        /* Free blocks count */
    u32 s_free_inodes_count;        /* Free inodes count */
    u32 s_first_data_block;         /* First Data Block */
    u32 s_log_block_size;           /* Block size */
    unsigned short s_log_frag_size; /* Fragment size */
    u32 s_blocks_per_group;         /* # Blocks per group */
    u32 s_frags_per_group;          /* # Fragments per group */
    u32 s_inodes_per_group;         /* # Inodes per group */
    u32 s_mtime;                    /* Mount time */
    u32 s_wtime;                    /* Write time */
    u16 s_mnt_count;                /* Mount count */
    unsigned short s_max_mnt_count; /* Maximal mount count */
    u16 s_magic;                    /* Magic signature */
    u16 s_state;                    /* File system state */
    u16 s_errors;                   /* Behavior when detecting errors */
    u16 s_minor_rev_level;          /* minor revision level */
    u32 s_lastcheck;                /* time of last check */
    u32 s_checkinterval;            /* max. time between checks */
    u32 s_creator_os;               /* OS */
    u32 s_rev_level;                /* Revision level */
    u16 s_def_resuid;               /* Default uid for reserved blocks */
    u16 s_def_resgid;               /* Default gid for reserved blocks */
    u32 s_first_ino;                /* First non-reserved inode */
    u16 s_inode_size;               /* size of inode structure */
    u16 s_block_group_nr;           /* block group # of this superblock */
    u32 s_feature_compat;           /* compatible feature set */
    u32 s_feature_incompat;         /* incompatible feature set */
    u32 s_feature_ro_compat;        /* readonly-compatible feature set */
    u8 s_uuid[16];                  /* 128-bit uuid for volume */
    char s_volume_name[16];         /* volume name */
    char s_last_mounted[64];        /* directory where last mounted */
    u32 s_algorithm_usage_bitmap;   /* For compression */
    u8 s_prealloc_blocks;           /* Nr of blocks to try to preallocate*/
    u8 s_prealloc_dir_blocks;       /* Nr to preallocate for dirs */
    u16 s_reserved_gdt_blocks;      /* Per group table for online growth */
    u8 s_journal_uuid[16];          /* uuid of journal superblock */
    u32 s_journal_inum;             /* inode number of journal file */
    u32 s_journal_dev;              /* device number of journal file */
    u32 s_last_orphan;              /* start of list of inodes to delete */
    u32 s_hash_seed[4];             /* HTREE hash seed */
    u8 s_def_hash_version;          /* Default hash version to use */
    u8 s_jnl_backup_type;           /* Default type of journal backup */
    u16 s_desc_size;                /* Group desc. size: INCOMPAT_64BIT */
    u32 s_default_mount_opts;
    u32 s_first_meta_bg;      /* First metablock group */
    u32 s_mkfs_time;          /* When the filesystem was created */
    u32 s_jnl_blocks[17];     /* Backup of the journal inode */
    u32 s_blocks_count_hi;    /* Blocks count high 32bits */
    u32 s_r_blocks_count_hi;  /* Reserved blocks count high 32 bits*/
    u32 s_free_blocks_hi;     /* Free blocks count */
    u16 s_min_extra_isize;    /* All inodes have at least # bytes */
    u16 s_want_extra_isize;   /* New inodes should reserve # bytes */
    u32 s_flags;              /* Miscellaneous flags */
    u16 s_raid_stride;        /* RAID stride */
    u16 s_mmp_interval;       /* # seconds to wait in MMP checking */
    u64 s_mmp_block;          /* Block for multi-mount protection */
    u32 s_raid_stripe_width;  /* blocks on all data disks (N*stride)*/
    u8 s_log_groups_per_flex; /* FLEX_BG group size */
    u8 s_reserved_char_pad;
    u16 s_reserved_pad;  /* Padding to next 32bits */
    u32 s_reserved[162]; /* Padding to the end of the block */
};

struct DirectoryEntry {
    u32 inode;
    u16 rec_len;
    u8 name_len;
    u8 file_type;
    char name[max_name_length];
};

inline size_t superblock_size(SuperBlock sb) { return min_block_size << sb.s_log_block_size; }
inline size_t superblock_size_bits(SuperBlock sb) { return sb.s_log_block_size + 10; }

inline size_t first_inode(SuperBlock sb) { return (sb.s_rev_level == 0) ? 11 : sb.s_first_ino; }
inline size_t address_per_block(SuperBlock sb) { return superblock_size(sb) / 4; }
inline size_t fragment_size(SuperBlock sb) { return super_block_offset_on_device << sb.s_log_frag_size; }
inline size_t descriptor_size(SuperBlock sb) { return (sb.s_feature_incompat & static_cast<u32>(FeaturesIncompat::EXT4_FEATURE_INCOMPAT_64BIT)) ? sb.s_desc_size : Ext2::min_group_size; }

inline size_t descriptor_per_block(SuperBlock sb) { return superblock_size(sb) / descriptor_size(sb); }
// ^ Inode operations

inline size_t inode_uid(Ext2Inode inode) { return (inode.i_uid | inode.osd2.linux2.l_i_uid_high) << 16; }
inline size_t inode_gid(Ext2Inode inode) { return (inode.i_gid | inode.osd2.linux2.l_i_gid_high) << 16; }
inline size_t set_i_uid_high(Ext2Inode inode, unsigned uid) { return inode.osd2.linux2.l_i_uid_high = uid; }
inline size_t set_i_gid_high(Ext2Inode inode, unsigned uid) { return inode.osd2.linux2.l_i_gid_high = uid; }
inline size_t directory_record_length(size_t name_len) { return ((name_len + 11) & ~3); }

}
