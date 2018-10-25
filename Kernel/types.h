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

    bool operator==(const LinearAddress& other) const { return m_address == other.m_address; }

    byte* asPtr() { return reinterpret_cast<byte*>(m_address); }
    const byte* asPtr() const { return reinterpret_cast<const byte*>(m_address); }

    dword pageBase() const { return m_address & 0xfffff000; }

private:
    dword m_address { 0 };
};
