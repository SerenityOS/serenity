/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>
#include <AK/Types.h>

#define O_RDONLY (1 << 0)
#define O_WRONLY (1 << 1)
#define O_RDWR (O_RDONLY | O_WRONLY)
#define O_ACCMODE (O_RDONLY | O_WRONLY)
#define O_EXEC (1 << 2)
#define O_CREAT (1 << 3)
#define O_EXCL (1 << 4)
#define O_NOCTTY (1 << 5)
#define O_TRUNC (1 << 6)
#define O_APPEND (1 << 7)
#define O_NONBLOCK (1 << 8)
#define O_DIRECTORY (1 << 9)
#define O_NOFOLLOW (1 << 10)
#define O_CLOEXEC (1 << 11)
#define O_DIRECT (1 << 12)

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

#define MAP_SHARED 0x01
#define MAP_PRIVATE 0x02
#define MAP_FIXED 0x10
#define MAP_ANONYMOUS 0x20
#define MAP_ANON MAP_ANONYMOUS
#define MAP_STACK 0x40
#define MAP_NORESERVE 0x80
#define MAP_RANDOMIZED 0x100

#define PROT_READ 0x1
#define PROT_WRITE 0x2
#define PROT_EXEC 0x4
#define PROT_NONE 0x0

#define MADV_SET_VOLATILE 0x100
#define MADV_SET_NONVOLATILE 0x200
#define MADV_GET_VOLATILE 0x400

#define F_DUPFD 0
#define F_GETFD 1
#define F_SETFD 2
#define F_GETFL 3
#define F_SETFL 4
#define F_ISTTY 5

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

/* c_cc characters */
#define VINTR 0
#define VQUIT 1
#define VERASE 2
#define VKILL 3
#define VEOF 4
#define VTIME 5
#define VMIN 6
#define VSWTC 7
#define VSTART 8
#define VSTOP 9
#define VSUSP 10
#define VEOL 11
#define VREPRINT 12
#define VDISCARD 13
#define VWERASE 14
#define VLNEXT 15
#define VEOL2 16
#define VINFO 17

/* c_iflag bits */
#define IGNBRK 0000001
#define BRKINT 0000002
#define IGNPAR 0000004
#define PARMRK 0000010
#define INPCK 0000020
#define ISTRIP 0000040
#define INLCR 0000100
#define IGNCR 0000200
#define ICRNL 0000400
#define IUCLC 0001000
#define IXON 0002000
#define IXANY 0004000
#define IXOFF 0010000
#define IMAXBEL 0020000
#define IUTF8 0040000

/* c_oflag bits */
#define OPOST 0000001
#define OLCUC 0000002
#define ONLCR 0000004
#define OCRNL 0000010
#define ONOCR 0000020
#define ONLRET 0000040
#define OFILL 0000100
#define OFDEL 0000200
#define NLDLY 0000400
#define NL0 0000000
#define NL1 0000400
#define CRDLY 0003000
#define CR0 0000000
#define CR1 0001000
#define CR2 0002000
#define CR3 0003000
#define TABDLY 0014000
#define TAB0 0000000
#define TAB1 0004000
#define TAB2 0010000
#define TAB3 0014000
#define BSDLY 0020000
#define BS0 0000000
#define BS1 0020000
#define FFDLY 0100000
#define FF0 0000000
#define FF1 0100000

#define VTDLY 0040000
#define VT0 0000000
#define VT1 0040000

#define XTABS 0014000

/* c_cflag bit meaning */
#define CBAUD 0010017
#define B0 0000000 /* hang up */
#define B50 0000001
#define B75 0000002
#define B110 0000003
#define B134 0000004
#define B150 0000005
#define B200 0000006
#define B300 0000007
#define B600 0000010
#define B1200 0000011
#define B1800 0000012
#define B2400 0000013
#define B4800 0000014
#define B9600 0000015
#define B19200 0000016
#define B38400 0000017
#define EXTA B19200
#define EXTB B38400
#define CSIZE 0000060
#define CS5 0000000
#define CS6 0000020
#define CS7 0000040
#define CS8 0000060
#define CSTOPB 0000100
#define CREAD 0000200
#define PARENB 0000400
#define PARODD 0001000
#define HUPCL 0002000
#define CLOCAL 0004000
#define CBAUDEX 0010000
#define B57600 0010001
#define B115200 0010002
#define B230400 0010003
#define B460800 0010004
#define B500000 0010005
#define B576000 0010006
#define B921600 0010007
#define B1000000 0010010
#define B1152000 0010011
#define B1500000 0010012
#define B2000000 0010013
#define B2500000 0010014
#define B3000000 0010015
#define B3500000 0010016
#define B4000000 0010017
#define __MAX_BAUD B4000000
#define CIBAUD 002003600000  /* input baud rate (not used) */
#define CMSPAR 010000000000  /* mark or space (stick) parity */
#define CRTSCTS 020000000000 /* flow control */

/* c_lflag bits */
#define ISIG 0000001
#define ICANON 0000002
#define XCASE 0000004
#define ECHO 0000010
#define ECHOE 0000020
#define ECHOK 0000040
#define ECHONL 0000100
#define NOFLSH 0000200
#define TOSTOP 0000400
#define ECHOCTL 0001000
#define ECHOPRT 0002000
#define ECHOKE 0004000
#define FLUSHO 0010000
#define PENDIN 0040000
#define IEXTEN 0100000
#define EXTPROC 0200000

/* tcflow() and TCXONC use these */
#define TCOOFF 0
#define TCOON 1
#define TCIOFF 2
#define TCION 3

/* tcflush() and TCFLSH use these */
#define TCIFLUSH 0
#define TCOFLUSH 1
#define TCIOFLUSH 2

/* tcsetattr uses these */
#define TCSANOW 0
#define TCSADRAIN 1
#define TCSAFLUSH 2

typedef u32 dev_t;
typedef u32 ino_t;
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

struct utimbuf {
    time_t actime;
    time_t modtime;
};

typedef u32 blksize_t;
typedef u32 blkcnt_t;

#define NCCS 32

typedef uint32_t tcflag_t;
typedef uint8_t cc_t;
typedef uint32_t speed_t;

struct termios {
    tcflag_t c_iflag;
    tcflag_t c_oflag;
    tcflag_t c_cflag;
    tcflag_t c_lflag;
    cc_t c_cc[NCCS];
    speed_t c_ispeed;
    speed_t c_ospeed;
};

struct timespec {
    time_t tv_sec; /* Seconds */
    long tv_nsec;  /* Nanoseconds */
};

struct stat {
    dev_t st_dev;            /* ID of device containing file */
    ino_t st_ino;            /* inode number */
    mode_t st_mode;          /* protection */
    nlink_t st_nlink;        /* number of hard links */
    uid_t st_uid;            /* user ID of owner */
    gid_t st_gid;            /* group ID of owner */
    dev_t st_rdev;           /* device ID (if special file) */
    off_t st_size;           /* total size, in bytes */
    blksize_t st_blksize;    /* blocksize for file system I/O */
    blkcnt_t st_blocks;      /* number of 512B blocks allocated */
    struct timespec st_atim; /* time of last access */
    struct timespec st_mtim; /* time of last modification */
    struct timespec st_ctim; /* time of last status change */
};

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

#define AF_MASK 0xff
#define AF_UNSPEC 0
#define AF_LOCAL 1
#define AF_INET 2
#define AF_MAX 3
#define PF_LOCAL AF_LOCAL
#define PF_INET AF_INET

#define SOCK_TYPE_MASK 0xff
#define SOCK_STREAM 1
#define SOCK_RAW 3
#define SOCK_DGRAM 2
#define SOCK_NONBLOCK 04000
#define SOCK_CLOEXEC 02000000

#define SHUT_RD 1
#define SHUT_WR 2
#define SHUT_RDWR 3

#define MSG_TRUNC 0x1
#define MSG_CTRUNC 0x2
#define MSG_PEEK 0x4
#define MSG_OOB 0x8
#define MSG_DONTWAIT 0x40

#define SOL_SOCKET 1

enum {
    SO_RCVTIMEO,
    SO_SNDTIMEO,
    SO_TYPE,
    SO_ERROR,
    SO_PEERCRED,
    SO_RCVBUF,
    SO_SNDBUF,
    SO_REUSEADDR,
    SO_BINDTODEVICE,
    SO_KEEPALIVE,
    SO_TIMESTAMP,
};

enum {
    SCM_TIMESTAMP,
};

#define IPPROTO_IP 0
#define IPPROTO_ICMP 1
#define IPPROTO_TCP 6
#define IPPROTO_UDP 17

#define IP_TTL 2
#define IP_MULTICAST_LOOP 3
#define IP_ADD_MEMBERSHIP 4
#define IP_DROP_MEMBERSHIP 5

struct ucred {
    pid_t pid;
    uid_t uid;
    gid_t gid;
};

struct sockaddr {
    u16 sa_family;
    char sa_data[14];
};

#define S_IFSOCK 0140000
#define UNIX_PATH_MAX 108

struct sockaddr_un {
    u16 sun_family;
    char sun_path[UNIX_PATH_MAX];
};

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

typedef int clockid_t;

#define CLOCK_REALTIME 0
#define CLOCK_MONOTONIC 1
#define CLOCK_MONOTONIC_RAW 4
#define CLOCK_REALTIME_COARSE 5
#define CLOCK_MONOTONIC_COARSE 6
#define TIMER_ABSTIME 99

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

struct cmsghdr {
    socklen_t cmsg_len;
    int cmsg_level;
    int cmsg_type;
};

struct msghdr {
    void* msg_name;
    socklen_t msg_namelen;
    struct iovec* msg_iov;
    int msg_iovlen;
    void* msg_control;
    socklen_t msg_controllen;
    int msg_flags;
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
