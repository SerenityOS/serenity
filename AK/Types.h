#pragma once

#include <stdint.h>

typedef uint8_t byte;
typedef uint16_t word;
typedef uint32_t dword;
typedef uint64_t qword;

typedef int8_t signed_byte;
typedef int16_t signed_word;
typedef int32_t signed_dword;
typedef int64_t signed_qword;

constexpr unsigned KB = 1024;
constexpr unsigned MB = KB * KB;
constexpr unsigned GB = KB * KB * KB;
