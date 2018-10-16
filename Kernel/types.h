#pragma once

#define PACKED __attribute__ ((packed))
#define NORETURN __attribute__ ((noreturn))
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
typedef DWORD size_t;

struct FarPtr {
    DWORD offset { 0 };
    WORD selector { 0 };
} PACKED;
