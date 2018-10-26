#pragma once

#include <AK/Types.h>

#define PACKED __attribute__ ((packed))
#define NORETURN __attribute__ ((noreturn))
#define ALWAYS_INLINE __attribute__ ((always_inline))
#define PURE __attribute__ ((pure))
#define PUBLIC
#define PRIVATE static

typedef unsigned char BYTE;
typedef unsigned short WORD;
typedef unsigned int DWORD;

typedef DWORD __u32;
typedef WORD __u16;
typedef BYTE __u8;
typedef int __s32;
typedef short __s16;

typedef DWORD uid_t;
typedef DWORD gid_t;
typedef int pid_t;
typedef DWORD time_t;
typedef DWORD suseconds_t;
typedef DWORD size_t;

struct timeval {
    time_t tv_sec;
    suseconds_t tv_usec;
};

#define UTSNAME_ENTRY_LEN 65

struct utsname {
    char sysname[UTSNAME_ENTRY_LEN];
    char nodename[UTSNAME_ENTRY_LEN];
    char release[UTSNAME_ENTRY_LEN];
    char version[UTSNAME_ENTRY_LEN];
    char machine[UTSNAME_ENTRY_LEN];
};

typedef dword ino_t;
typedef signed_dword off_t;

typedef dword dev_t;
typedef dword mode_t;
typedef dword nlink_t;
typedef dword blksize_t;
typedef dword blkcnt_t;
typedef dword time_t;
typedef dword suseconds_t;

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

struct FarPtr {
    DWORD offset { 0 };
    WORD selector { 0 };
} PACKED;

class PhysicalAddress {
public:
    PhysicalAddress() { }
    explicit PhysicalAddress(dword address) : m_address(address) { }

    dword get() const { return m_address; }
    void set(dword address) { m_address = address; }
    void mask(dword m) { m_address &= m; }

    byte* asPtr() { return reinterpret_cast<byte*>(m_address); }
    const byte* asPtr() const { return reinterpret_cast<const byte*>(m_address); }

    dword pageBase() const { return m_address & 0xfffff000; }

private:
    dword m_address { 0 };
};

class LinearAddress {
public:
    LinearAddress() { }
    explicit LinearAddress(dword address) : m_address(address) { }

    LinearAddress offset(dword o) const { return LinearAddress(m_address + o); }
    dword get() const { return m_address; }
    void set(dword address) { m_address = address; }
    void mask(dword m) { m_address &= m; }

    bool operator<=(const LinearAddress& other) const { return m_address <= other.m_address; }
    bool operator>=(const LinearAddress& other) const { return m_address >= other.m_address; }
    bool operator>(const LinearAddress& other) const { return m_address > other.m_address; }
    bool operator<(const LinearAddress& other) const { return m_address < other.m_address; }
    bool operator==(const LinearAddress& other) const { return m_address == other.m_address; }

    byte* asPtr() { return reinterpret_cast<byte*>(m_address); }
    const byte* asPtr() const { return reinterpret_cast<const byte*>(m_address); }

    dword pageBase() const { return m_address & 0xfffff000; }

private:
    dword m_address { 0 };
};
