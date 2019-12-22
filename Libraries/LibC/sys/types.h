#pragma once

#include <bits/stdint.h>
#include <stddef.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

typedef unsigned char u_char;
typedef unsigned short u_short;
typedef unsigned int u_int;
typedef unsigned long u_long;

typedef uint32_t uid_t;
typedef uint32_t gid_t;

typedef int __pid_t;
#define pid_t __pid_t

typedef int __ssize_t;
#define ssize_t __ssize_t

typedef __WINT_TYPE__ wint_t;

typedef uint32_t ino_t;
typedef int32_t off_t;

typedef uint32_t dev_t;
typedef uint16_t mode_t;
typedef uint32_t nlink_t;
typedef uint32_t blksize_t;
typedef uint32_t blkcnt_t;
typedef uint32_t time_t;
typedef uint32_t useconds_t;
typedef int32_t suseconds_t;
typedef uint32_t clock_t;

#define __socklen_t_defined
#define __socklen_t uint32_t
typedef __socklen_t socklen_t;

struct stat {
    dev_t st_dev;         /* ID of device containing file */
    ino_t st_ino;         /* inode number */
    mode_t st_mode;       /* protection */
    nlink_t st_nlink;     /* number of hard links */
    uid_t st_uid;         /* user ID of owner */
    gid_t st_gid;         /* group ID of owner */
    dev_t st_rdev;        /* device ID (if special file) */
    off_t st_size;        /* total size, in bytes */
    blksize_t st_blksize; /* blocksize for file system I/O */
    blkcnt_t st_blocks;   /* number of 512B blocks allocated */
    time_t st_atime;      /* time of last access */
    time_t st_mtime;      /* time of last modification */
    time_t st_ctime;      /* time of last status change */
};

struct utimbuf {
    time_t actime;
    time_t modtime;
};

typedef int pthread_t;
typedef int pthread_key_t;
typedef void* pthread_once_t;

typedef struct __pthread_mutex_t {
    uint32_t lock;
    pthread_t owner;
    int level;
    int type;
} pthread_mutex_t;

typedef void* pthread_attr_t;
typedef struct __pthread_mutexattr_t {
    int type;
} pthread_mutexattr_t;

typedef struct __pthread_cond_t {
    void* storage;
} pthread_cond_t;

typedef void* pthread_rwlock_t;
typedef void* pthread_rwlockatrr_t;
typedef void* pthread_spinlock_t;
typedef struct __pthread_condattr_t {
    int clockid; // clockid_t
} pthread_condattr_t;

__END_DECLS
