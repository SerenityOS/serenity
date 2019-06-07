#pragma once

#include <AK/IterationDecision.h>

#ifdef __serenity__
typedef unsigned char byte;
typedef unsigned short word;
typedef unsigned int dword;
typedef unsigned long long int qword;

typedef signed char signed_byte;
typedef signed short signed_word;
typedef signed int signed_dword;
typedef signed long long int signed_qword;

typedef decltype(sizeof(void*)) size_t;
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
