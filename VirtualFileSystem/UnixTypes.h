#pragma once

extern "C" {

namespace Unix {

#define SEEK_SET 0
#define SEEK_CUR 1
#define SEEK_END 2

typedef dword dev_t;
typedef dword ino_t;
typedef dword mode_t;
typedef dword nlink_t;
typedef dword uid_t;
typedef dword gid_t;

#ifdef SERENITY
typedef void (*__sighandler_t)(int);
typedef __sighandler_t sighandler_t;

typedef dword sigset_t;
typedef void siginfo_t;

struct sigaction {
    union {
        void (*sa_handler)(int);
        void (*sa_sigaction)(int, siginfo_t*, void*);
    };
    sigset_t sa_mask;
    int sa_flags;
    void (*sa_restorer)(void);
};

#define SA_NOCLDSTOP 1
#define SA_NOCLDWAIT 2
#define SA_SIGINFO 4

#define SIG_BLOCK 0
#define SIG_UNBLOCK 1
#define SIG_SETMASK 2

#endif

#ifdef SERENITY
// FIXME: Support 64-bit offsets!
typedef signed_dword off_t;
typedef unsigned int time_t;
#else
typedef signed_qword off_t;
typedef ::time_t time_t;
#endif

typedef dword blksize_t;
typedef dword blkcnt_t;
typedef dword size_t;
typedef signed_dword ssize_t;

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

}

}

