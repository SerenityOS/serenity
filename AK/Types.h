#pragma once

#include <AK/IterationDecision.h>
#include <AK/Platform.h>

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

typedef __SIZE_TYPE__ size_t;
typedef i32 ssize_t;

static_assert(sizeof(size_t) == sizeof(u32));
static_assert(sizeof(ssize_t) == sizeof(i32));

typedef __PTRDIFF_TYPE__ ptrdiff_t;

typedef u8 uint8_t;
typedef u16 uint16_t;
typedef u32 uint32_t;
typedef u64 uint64_t;

typedef i8 int8_t;
typedef i16 int16_t;
typedef i32 int32_t;
typedef i64 int64_t;

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
#endif

constexpr unsigned KB = 1024;
constexpr unsigned MB = KB * KB;
constexpr unsigned GB = KB * KB * KB;

namespace std {
typedef decltype(nullptr) nullptr_t;
}

static constexpr u32 explode_byte(u8 b)
{
    return b << 24 | b << 16 | b << 8 | b;
}

static_assert(explode_byte(0xff) == 0xffffffff);
static_assert(explode_byte(0x80) == 0x80808080);
static_assert(explode_byte(0x7f) == 0x7f7f7f7f);
static_assert(explode_byte(0) == 0);
