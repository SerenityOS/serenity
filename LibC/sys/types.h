#pragma once

#include <sys/cdefs.h>
#include <stdint.h>

__BEGIN_DECLS

typedef uint32_t uid_t;
typedef uint32_t gid_t;
typedef int pid_t;

typedef uint32_t size_t;
typedef int32_t ssize_t;

typedef uint32_t ino_t;
typedef int32_t off_t;

typedef uint32_t dev_t;
typedef uint32_t mode_t;
typedef uint32_t nlink_t;
typedef uint32_t blksize_t;
typedef uint32_t blkcnt_t;
typedef uint32_t time_t;
typedef uint32_t suseconds_t;

typedef uint32_t clock_t;

struct timeval {
    time_t tv_sec;
    suseconds_t tv_usec;
};

struct stat {
    dev_t     st_dev;     /* ID of device containing file */
    ino_t     st_ino;     /* inode number */
    mode_t    st_mode;    /* protection */
    nlink_t   st_nlink;   /* number of hard links */
    uid_t     st_uid;     /* user ID of owner */
    gid_t     st_gid;     /* group ID of owner */
    dev_t     st_rdev;    /* device ID (if special file) */
    off_t     st_size;    /* total size, in bytes */
    blksize_t st_blksize; /* blocksize for file system I/O */
    blkcnt_t  st_blocks;  /* number of 512B blocks allocated */
    time_t    st_atime;   /* time of last access */
    time_t    st_mtime;   /* time of last modification */
    time_t    st_ctime;   /* time of last status change */
};

#ifdef __cplusplus
#define NULL nullptr
#else
#define NULL 0
#endif

__END_DECLS

