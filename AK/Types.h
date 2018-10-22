#pragma once

#if defined(SERENITY_KERNEL) || defined(SERENITY_LIBC)
typedef unsigned char byte;
typedef unsigned short word;
typedef unsigned int dword;
typedef unsigned long long int qword;

typedef signed char signed_byte;
typedef signed short signed_word;
typedef signed int signed_dword;
typedef signed long long int signed_qword;

typedef dword size_t;
typedef signed_dword ssize_t;

typedef signed_dword ptrdiff_t;
#else
#include <stdint.h>
#include <sys/types.h>

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

