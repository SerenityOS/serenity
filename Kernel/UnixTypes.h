/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>
#include <AK/Types.h>
#include <Kernel/API/POSIX/fcntl.h>
#include <Kernel/API/POSIX/sys/mman.h>
#include <Kernel/API/POSIX/sys/socket.h>
#include <Kernel/API/POSIX/sys/stat.h>
#include <Kernel/API/POSIX/sys/un.h>
#include <Kernel/API/POSIX/termios.h>
#include <Kernel/API/POSIX/time.h>

// Kernel internal options.
#define O_NOFOLLOW_NOERROR (1 << 29)
#define O_UNLINK_INTERNAL (1 << 30)

#define MS_NODEV (1 << 0)
#define MS_NOEXEC (1 << 1)
#define MS_NOSUID (1 << 2)
#define MS_BIND (1 << 3)
#define MS_RDONLY (1 << 4)
#define MS_REMOUNT (1 << 5)

enum {
    _SC_MONOTONIC_CLOCK,
    _SC_NPROCESSORS_CONF,
    _SC_NPROCESSORS_ONLN,
    _SC_OPEN_MAX,
    _SC_TTY_NAME_MAX,
    _SC_PAGESIZE,
    _SC_GETPW_R_SIZE_MAX,
    _SC_CLK_TCK,
};

enum {
    PERF_EVENT_SAMPLE = 1,
    PERF_EVENT_MALLOC = 2,
    PERF_EVENT_FREE = 4,
    PERF_EVENT_MMAP = 8,
    PERF_EVENT_MUNMAP = 16,
    PERF_EVENT_PROCESS_CREATE = 32,
    PERF_EVENT_PROCESS_EXEC = 64,
    PERF_EVENT_PROCESS_EXIT = 128,
    PERF_EVENT_THREAD_CREATE = 256,
    PERF_EVENT_THREAD_EXIT = 512,
    PERF_EVENT_CONTEXT_SWITCH = 1024,
    PERF_EVENT_KMALLOC = 2048,
    PERF_EVENT_KFREE = 4096,
    PERF_EVENT_PAGE_FAULT = 8192,
    PERF_EVENT_SYSCALL = 16384,
    PERF_EVENT_SIGNPOST = 32768,
};

#define WNOHANG 1
#define WUNTRACED 2
#define WSTOPPED WUNTRACED
#define WEXITED 4
#define WCONTINUED 8
#define WNOWAIT 0x1000000

#define R_OK 4
#define W_OK 2
#define X_OK 1
#define F_OK 0

#define SIG_DFL ((void*)0)
#define SIG_ERR ((void*)-1)
#define SIG_IGN ((void*)1)

#define SEEK_SET 0
#define SEEK_CUR 1
#define SEEK_END 2

#define MADV_SET_VOLATILE 0x100
#define MADV_SET_NONVOLATILE 0x200

#define F_DUPFD 0
#define F_GETFD 1
#define F_SETFD 2
#define F_GETFL 3
#define F_SETFL 4
#define F_ISTTY 5
#define F_GETLK 6
#define F_SETLK 7
#define F_SETLKW 8

#define FD_CLOEXEC 1

#define _FUTEX_OP_SHIFT_OP 28
#define _FUTEX_OP_MASK_OP 0xf
#define _FUTEX_OP_SHIFT_CMP 24
#define _FUTEX_OP_MASK_CMP 0xf
#define _FUTEX_OP_SHIFT_OP_ARG 12
#define _FUTEX_OP_MASK_OP_ARG 0xfff
#define _FUTEX_OP_SHIFT_CMP_ARG 0
#define _FUTEX_OP_MASK_CMP_ARG 0xfff

#define _FUTEX_OP(val3) (((val3) >> _FUTEX_OP_SHIFT_OP) & _FUTEX_OP_MASK_OP)
#define _FUTEX_CMP(val3) (((val3) >> _FUTEX_OP_SHIFT_CMP) & _FUTEX_OP_MASK_CMP)
#define _FUTEX_OP_ARG(val3) (((val3) >> _FUTEX_OP_SHIFT_OP_ARG) & _FUTEX_OP_MASK_OP_ARG)
#define _FUTEX_CMP_ARG(val3) (((val3) >> _FUTEX_OP_SHIFT_CMP_ARG) & _FUTEX_OP_MASK_CMP_ARG)

#define FUTEX_OP_SET 0
#define FUTEX_OP_ADD 1
#define FUTEX_OP_OR 2
#define FUTEX_OP_ANDN 3
#define FUTEX_OP_XOR 4
#define FUTEX_OP_ARG_SHIFT 8

#define FUTEX_OP_CMP_EQ 0
#define FUTEX_OP_CMP_NE 1
#define FUTEX_OP_CMP_LT 2
#define FUTEX_OP_CMP_LE 3
#define FUTEX_OP_CMP_GT 4
#define FUTEX_OP_CMP_GE 5

#define FUTEX_WAIT 1
#define FUTEX_WAKE 2
#define FUTEX_REQUEUE 3
#define FUTEX_CMP_REQUEUE 4
#define FUTEX_WAKE_OP 5
#define FUTEX_WAIT_BITSET 9
#define FUTEX_WAKE_BITSET 10

#define FUTEX_PRIVATE_FLAG (1 << 7)
#define FUTEX_CLOCK_REALTIME (1 << 8)
#define FUTEX_CMD_MASK ~(FUTEX_PRIVATE_FLAG | FUTEX_CLOCK_REALTIME)

#define FUTEX_BITSET_MATCH_ANY 0xffffffff

#define S_IFMT 0170000
#define S_IFDIR 0040000
#define S_IFCHR 0020000
#define S_IFBLK 0060000
#define S_IFREG 0100000
#define S_IFIFO 0010000
#define S_IFLNK 0120000
#define S_IFSOCK 0140000

#define S_ISUID 04000
#define S_ISGID 02000
#define S_ISVTX 01000
#define S_IRUSR 0400
#define S_IWUSR 0200
#define S_IXUSR 0100
#define S_IRGRP 0040
#define S_IWGRP 0020
#define S_IXGRP 0010
#define S_IROTH 0004
#define S_IWOTH 0002
#define S_IXOTH 0001

typedef u32 dev_t;
typedef u64 ino_t;
typedef u16 mode_t;
typedef u32 nlink_t;
typedef u32 uid_t;
typedef u32 gid_t;
typedef u32 clock_t;
typedef u32 socklen_t;
typedef int pid_t;
// Avoid interference with AK/Types.h and LibC/sys/types.h by defining *separate* names:
TYPEDEF_DISTINCT_ORDERED_ID(pid_t, ProcessID);
TYPEDEF_DISTINCT_ORDERED_ID(pid_t, ThreadID);
TYPEDEF_DISTINCT_ORDERED_ID(pid_t, SessionID);
TYPEDEF_DISTINCT_ORDERED_ID(pid_t, ProcessGroupID);

struct tms {
    clock_t tms_utime;
    clock_t tms_stime;
    clock_t tms_cutime;
    clock_t tms_cstime;
};

typedef void (*__sighandler_t)(int);
typedef __sighandler_t sighandler_t;

typedef u32 sigset_t;

union sigval {
    int sival_int;
    void* sival_ptr;
};

typedef struct siginfo {
    int si_signo;
    int si_code;
    pid_t si_pid;
    uid_t si_uid;
    void* si_addr;
    int si_status;
    union sigval si_value;
} siginfo_t;

struct sigaction {
    union {
        void (*sa_handler)(int);
        void (*sa_sigaction)(int, siginfo_t*, void*);
    };
    sigset_t sa_mask;
    int sa_flags;
};

#define SA_NOCLDSTOP 1
#define SA_NOCLDWAIT 2
#define SA_SIGINFO 4
#define SA_NODEFER 0x40000000

#define SIG_BLOCK 0
#define SIG_UNBLOCK 1
#define SIG_SETMASK 2

#define CLD_EXITED 0
#define CLD_KILLED 1
#define CLD_DUMPED 2
#define CLD_TRAPPED 3
#define CLD_STOPPED 4
#define CLD_CONTINUED 5

typedef i64 off_t;
typedef i64 time_t;

typedef u32 blksize_t;
typedef u32 blkcnt_t;

#define POLLIN (1u << 0)
#define POLLPRI (1u << 1)
#define POLLOUT (1u << 2)
#define POLLERR (1u << 3)
#define POLLHUP (1u << 4)
#define POLLNVAL (1u << 5)
#define POLLRDHUP (1u << 13)

struct pollfd {
    int fd;
    short events;
    short revents;
};

#define IPPROTO_IP 0
#define IPPROTO_ICMP 1
#define IPPROTO_TCP 6
#define IPPROTO_UDP 17

#define IP_TTL 2
#define IP_MULTICAST_LOOP 3
#define IP_ADD_MEMBERSHIP 4
#define IP_DROP_MEMBERSHIP 5

#define S_IFSOCK 0140000

struct in_addr {
    uint32_t s_addr;
};
typedef uint32_t in_addr_t;

struct sockaddr_in {
    int16_t sin_family;
    uint16_t sin_port;
    struct in_addr sin_addr;
    char sin_zero[8];
};

struct ip_mreq {
    struct in_addr imr_multiaddr;
    struct in_addr imr_interface;
};

#define INADDR_ANY ((in_addr_t)0)
#define INADDR_NONE ((in_addr_t)-1)
#define INADDR_LOOPBACK 0x7f000001

typedef u32 __u32;
typedef u16 __u16;
typedef u8 __u8;
typedef int __s32;
typedef short __s16;

typedef u32 useconds_t;
typedef i32 suseconds_t;

struct timeval {
    time_t tv_sec;
    suseconds_t tv_usec;
};

typedef enum {
    P_ALL = 1,
    P_PID,
    P_PGID
} idtype_t;

#define UTSNAME_ENTRY_LEN 65

struct utsname {
    char sysname[UTSNAME_ENTRY_LEN];
    char nodename[UTSNAME_ENTRY_LEN];
    char release[UTSNAME_ENTRY_LEN];
    char version[UTSNAME_ENTRY_LEN];
    char machine[UTSNAME_ENTRY_LEN];
};

struct iovec {
    void* iov_base;
    size_t iov_len;
};

struct sched_param {
    int sched_priority;
};

struct ifreq {
#define IFNAMSIZ 16
    char ifr_name[IFNAMSIZ];
    union {
        struct sockaddr ifru_addr;
        struct sockaddr ifru_dstaddr;
        struct sockaddr ifru_broadaddr;
        struct sockaddr ifru_netmask;
        struct sockaddr ifru_hwaddr;
        short ifru_flags;
        int ifru_metric;
        int64_t ifru_vnetid;
        uint64_t ifru_media;
        void* ifru_data;
        unsigned int ifru_index;
    } ifr_ifru;

#define ifr_addr ifr_ifru.ifru_addr           // address
#define ifr_dstaddr ifr_ifru.ifru_dstaddr     // other end of p-to-p link
#define ifr_broadaddr ifr_ifru.ifru_broadaddr // broadcast address
#define ifr_netmask ifr_ifru.ifru_netmask     // network mask
#define ifr_flags ifr_ifru.ifru_flags         // flags
#define ifr_metric ifr_ifru.ifru_metric       // metric
#define ifr_mtu ifr_ifru.ifru_metric          // mtu (overload)
#define ifr_hardmtu ifr_ifru.ifru_metric      // hardmtu (overload)
#define ifr_media ifr_ifru.ifru_media         // media options
#define ifr_rdomainid ifr_ifru.ifru_metric    // VRF instance (overload)
#define ifr_vnetid ifr_ifru.ifru_vnetid       // Virtual Net Id
#define ifr_ttl ifr_ifru.ifru_metric          // tunnel TTL (overload)
#define ifr_data ifr_ifru.ifru_data           // for use by interface
#define ifr_index ifr_ifru.ifru_index         // interface index
#define ifr_llprio ifr_ifru.ifru_metric       // link layer priority
#define ifr_hwaddr ifr_ifru.ifru_hwaddr       // MAC address
};

struct rtentry {
    struct sockaddr rt_gateway; /* the gateway address */
    struct sockaddr rt_genmask; /* the target network mask */
    unsigned short int rt_flags;
    char* rt_dev;
    /* FIXME: complete the struct */
};

#define RTF_UP 0x1      /* do not delete the route */
#define RTF_GATEWAY 0x2 /* the route is a gateway and not an end host */

#define AT_FDCWD -100
#define AT_SYMLINK_NOFOLLOW 0x100

struct arpreq {
    struct sockaddr arp_pa;      /* protocol address */
    struct sockaddr arp_ha;      /* hardware address */
    struct sockaddr arp_netmask; /* netmask of protocol address */
    int arp_flags;               /* flags */
    char arp_dev[16];
};

#define PURGE_ALL_VOLATILE 0x1
#define PURGE_ALL_CLEAN_INODE 0x2

#define PT_TRACE_ME 1
#define PT_ATTACH 2
#define PT_CONTINUE 3
#define PT_SYSCALL 4
#define PT_GETREGS 5
#define PT_DETACH 6
#define PT_PEEK 7
#define PT_POKE 8
#define PT_SETREGS 9
#define PT_POKEDEBUG 10
#define PT_PEEKDEBUG 11

// Used in struct dirent
enum {
    DT_UNKNOWN = 0,
#define DT_UNKNOWN DT_UNKNOWN
    DT_FIFO = 1,
#define DT_FIFO DT_FIFO
    DT_CHR = 2,
#define DT_CHR DT_CHR
    DT_DIR = 4,
#define DT_DIR DT_DIR
    DT_BLK = 6,
#define DT_BLK DT_BLK
    DT_REG = 8,
#define DT_REG DT_REG
    DT_LNK = 10,
#define DT_LNK DT_LNK
    DT_SOCK = 12,
#define DT_SOCK DT_SOCK
    DT_WHT = 14
#define DT_WHT DT_WHT
};

typedef uint64_t fsblkcnt_t;
typedef uint64_t fsfilcnt_t;

#define ST_RDONLY 0x1
#define ST_NOSUID 0x2

struct statvfs {
    unsigned long f_bsize;
    unsigned long f_frsize;
    fsblkcnt_t f_blocks;
    fsblkcnt_t f_bfree;
    fsblkcnt_t f_bavail;

    fsfilcnt_t f_files;
    fsfilcnt_t f_ffree;
    fsfilcnt_t f_favail;

    unsigned long f_fsid;
    unsigned long f_flag;
    unsigned long f_namemax;
};
