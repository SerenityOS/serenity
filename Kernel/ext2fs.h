#ifndef __ext2fs_h__
#define __ext2fs_h__

#include "types.h"

#define EXT2_MAGIC                      0xef53
#define EXT2_NDIR_BLOCKS                12
#define EXT2_IND_BLOCK                  EXT2_NDIR_BLOCKS
#define EXT2_DIND_BLOCK                 (EXT2_IND_BLOCK + 1)
#define EXT2_TIND_BLOCK                 (EXT2_DIND_BLOCK + 1)
#define EXT2_N_BLOCKS                   (EXT2_TIND_BLOCK + 1)
#define EXT2_NAME_LEN 255

#define EXT2_FT_UNKNOWN                 0
#define EXT2_FT_REG_FILE                1
#define EXT2_FT_DIR                     2
#define EXT2_FT_CHRDEV                  3
#define EXT2_FT_BLKDEV                  4
#define EXT2_FT_FIFO                    5
#define EXT2_FT_SOCK                    6
#define EXT2_FT_SYMLINK                 7

typedef struct
{
    __u32 s_inodes_count;       /* Inodes count */
    __u32 s_blocks_count;       /* Blocks count */
    __u32 s_r_blocks_count;     /* Reserved blocks count */
    __u32 s_free_blocks_count;  /* Free blocks count */
    __u32 s_free_inodes_count;  /* Free inodes count */
    __u32 s_first_data_block;   /* First Data Block */
    __u32 s_log_block_size;     /* Block size */
    __s32 s_log_frag_size;      /* Fragment size */
    __u32 s_blocks_per_group;   /* # Blocks per group */
    __u32 s_frags_per_group;    /* # Fragments per group */
    __u32 s_inodes_per_group;   /* # Inodes per group */
    __u32 s_mtime;              /* Mount time */
    __u32 s_wtime;              /* Write time */
    __u16 s_mnt_count;          /* Mount count */
    __s16 s_max_mnt_count;      /* Maximal mount count */
    __u16 s_magic;              /* Magic signature */
    __u16 s_state;              /* File system state */
    __u16 s_errors;             /* Behaviour when detecting errors */
    __u16 s_pad;
    __u32 s_lastcheck;          /* time of last check */
    __u32 s_checkinterval;      /* max. time between checks */
    __u32 s_creator_os;         /* OS */
    __u32 s_rev_level;          /* Revision level */
    __u16 s_def_resuid;         /* Default uid for reserved blocks */
    __u16 s_def_resgid;         /* Default gid for reserved blocks */
    __u32 s_first_ino;          // First non-reserved inode
    __u16 s_inode_size;         // inode size
    __u16 s_block_group_nr;     // Index of block group hosting this superblock
    __u32 s_feature_compat;
    __u32 s_feature_incompat;
    __u32 s_feature_ro_compat;
    __u8 s_uuid[16];
    __u32 s_reserved[226];      /* Padding to the end of the block */
} PACKED ext2_super_block;

typedef struct
{
    __u32 bg_block_bitmap;
    __u32 bg_inode_bitmap;
    __u32 bg_inode_table;
    __u16 bg_free_blocks_count;
    __u16 bg_free_inodes_count;
    __u16 bg_used_dirs_count;
    __u16 bg_pad;
    __u32 bg_reserved;
} PACKED ext2_group_descriptor;

typedef struct
{
    __u16 i_mode;               /* File mode */
    __u16 i_uid;                /* Owner Uid */
    __u32 i_size;               /* 4: Size in bytes */
    __u32 i_atime;              /* Access time */
    __u32 i_ctime;              /* 12: Creation time */
    __u32 i_mtime;              /* Modification time */
    __u32 i_dtime;              /* 20: Deletion Time */
    __u16 i_gid;                /* Group Id */
    __u16 i_links_count;        /* 24: Links count */
    __u32 i_blocks;             /* Blocks count */
    __u32 i_flags;              /* 32: File flags */
    union
      {
        struct
          {
            __u32 l_i_reserved1;
          }
        linux1;
        struct
          {
            __u32 h_i_translator;
          }
        hurd1;
        struct
          {
            __u32 m_i_reserved1;
          }
        masix1;
      }
    osd1;                       /* OS dependent 1 */
    __u32 i_block[EXT2_N_BLOCKS];       /* 40: Pointers to blocks */
    __u32 i_version;            /* File version (for NFS) */
    __u32 i_file_acl;           /* File ACL */
    __u32 i_dir_acl;            /* Directory ACL */
    __u32 i_faddr;              /* Fragment address */
    union
      {
        struct
          {
            __u8 l_i_frag;      /* Fragment number */
            __u8 l_i_fsize;     /* Fragment size */
            __u16 i_pad1;
            __u32 l_i_reserved2[2];
          }
        linux2;
        struct
          {
            __u8 h_i_frag;      /* Fragment number */
            __u8 h_i_fsize;     /* Fragment size */
            __u16 h_i_mode_high;
            __u16 h_i_uid_high;
            __u16 h_i_gid_high;
            __u32 h_i_author;
          }
        hurd2;
        struct
          {
            __u8 m_i_frag;      /* Fragment number */
            __u8 m_i_fsize;     /* Fragment size */
            __u16 m_pad1;
            __u32 m_i_reserved2[2];
          }
        masix2;
      }
    osd2;                       /* OS dependent 2 */
} PACKED ext2_inode;

typedef struct
{
    __u32 d_inode;
    __u16 d_rec_len;
    __u8 d_name_len;
    __u8 d_file_type;
    char d_name[EXT2_NAME_LEN];
} PACKED ext2_dir_entry;

#endif
