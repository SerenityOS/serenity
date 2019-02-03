#pragma once

#include <AK/Compiler.h>
#include <AK/Types.h>

#define PACKED __attribute__ ((packed))
#define PURE __attribute__ ((pure))

typedef dword __u32;
typedef word __u16;
typedef byte __u8;
typedef int __s32;
typedef short __s16;

typedef dword uid_t;
typedef dword gid_t;
typedef signed_word pid_t;
typedef dword time_t;
typedef dword useconds_t;
typedef dword suseconds_t;

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
typedef word mode_t;
typedef dword nlink_t;
typedef dword blksize_t;
typedef dword blkcnt_t;

struct FarPtr {
    dword offset { 0 };
    word selector { 0 };
} PACKED;

class PhysicalAddress {
public:
    PhysicalAddress() { }
    explicit PhysicalAddress(dword address) : m_address(address) { }

    PhysicalAddress offset(dword o) const { return PhysicalAddress(m_address + o); }
    dword get() const { return m_address; }
    void set(dword address) { m_address = address; }
    void mask(dword m) { m_address &= m; }

    bool is_null() const { return m_address == 0; }

    byte* as_ptr() { return reinterpret_cast<byte*>(m_address); }
    const byte* as_ptr() const { return reinterpret_cast<const byte*>(m_address); }

    dword page_base() const { return m_address & 0xfffff000; }

    bool operator==(const PhysicalAddress& other) const { return m_address == other.m_address; }

private:
    dword m_address { 0 };
};

class LinearAddress {
public:
    LinearAddress() { }
    explicit LinearAddress(dword address) : m_address(address) { }

    bool is_null() const { return m_address == 0; }

    LinearAddress offset(dword o) const { return LinearAddress(m_address + o); }
    dword get() const { return m_address; }
    void set(dword address) { m_address = address; }
    void mask(dword m) { m_address &= m; }

    bool operator<=(const LinearAddress& other) const { return m_address <= other.m_address; }
    bool operator>=(const LinearAddress& other) const { return m_address >= other.m_address; }
    bool operator>(const LinearAddress& other) const { return m_address > other.m_address; }
    bool operator<(const LinearAddress& other) const { return m_address < other.m_address; }
    bool operator==(const LinearAddress& other) const { return m_address == other.m_address; }
    bool operator!=(const LinearAddress& other) const { return m_address != other.m_address; }

    byte* as_ptr() { return reinterpret_cast<byte*>(m_address); }
    const byte* as_ptr() const { return reinterpret_cast<const byte*>(m_address); }

    dword page_base() const { return m_address & 0xfffff000; }

private:
    dword m_address { 0 };
};

inline LinearAddress operator-(const LinearAddress& a, const LinearAddress& b)
{
    return LinearAddress(a.get() - b.get());
}
