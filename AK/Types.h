#pragma once

#include <AK/IterationDecision.h>

#ifdef __serenity__
typedef unsigned char u8;
typedef unsigned short u16;
typedef unsigned int u32;
typedef unsigned long long int u64;
static_assert(sizeof(u8) == 1);
static_assert(sizeof(u16) == 2);
static_assert(sizeof(u32) == 4);
static_assert(sizeof(u64) == 8);

typedef signed char i8;
typedef signed short i16;
typedef signed int i32;
typedef signed long long int i64;
static_assert(sizeof(i8) == 1);
static_assert(sizeof(i16) == 2);
static_assert(sizeof(i32) == 4);
static_assert(sizeof(i64) == 8);

typedef unsigned char byte;
typedef unsigned short word;
typedef unsigned int dword;
typedef unsigned long long int qword;

typedef signed char signed_byte;
typedef signed short signed_word;
typedef signed int signed_dword;
typedef signed long long int signed_qword;

typedef __SIZE_TYPE__ size_t;
typedef signed_dword ssize_t;

static_assert(sizeof(size_t) == sizeof(dword));
static_assert(sizeof(ssize_t) == sizeof(signed_dword));

typedef __PTRDIFF_TYPE__ ptrdiff_t;

typedef byte uint8_t;
typedef word uint16_t;
typedef dword uint32_t;
typedef qword uint64_t;

typedef signed_byte int8_t;
typedef signed_word int16_t;
typedef signed_dword int32_t;
typedef signed_qword int64_t;

#else
#    include <stdint.h>
#    include <sys/types.h>

typedef uint8_t u8;
typedef uint16_t u16;
typedef uint32_t u32;
typedef uint64_t u64;

typedef int8_t i8;
typedef int16_t i16;
typedef int32_t i32;
typedef int64_t i64;

typedef uint8_t byte;
typedef uint16_t word;
typedef uint32_t dword;
typedef uint64_t qword;

typedef int8_t signed_byte;
typedef int16_t signed_word;
typedef int32_t signed_dword;
typedef int64_t signed_qword;
#endif

constexpr unsigned KB = 1024;
constexpr unsigned MB = KB * KB;
constexpr unsigned GB = KB * KB * KB;

namespace std {
typedef decltype(nullptr) nullptr_t;
}

static constexpr dword explode_byte(byte b)
{
    return b << 24 | b << 16 | b << 8 | b;
}

static_assert(explode_byte(0xff) == 0xffffffff);
static_assert(explode_byte(0x80) == 0x80808080);
static_assert(explode_byte(0x7f) == 0x7f7f7f7f);
static_assert(explode_byte(0) == 0);
