#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

typedef unsigned long long int uint64_t;
typedef unsigned int uint32_t;
typedef unsigned short uint16_t;
typedef unsigned char uint8_t;

typedef signed long long int int64_t;
typedef signed int int32_t;
typedef signed short int16_t;
typedef signed char int8_t;

#define __int8_t_defined 1
#define __uint8_t_defined 1
#define __int16_t_defined 1
#define __uint16_t_defined 1
#define __int32_t_defined 1
#define __uint32_t_defined 1
#define __int64_t_defined 1
#define __uint64_t_defined 1

typedef __PTRDIFF_TYPE__ uintptr_t;
typedef __PTRDIFF_TYPE__ intptr_t;
#define INTPTR_MAX PTRDIFF_MAX
#define INTPTR_MIN PTRDIFF_MIN
#define UINTPTR_MAX __UINTPTR_MAX__

typedef __UINTMAX_TYPE__ uintmax_t;
#define UINTMAX_MAX __UINTMAX_MAX__
#define UINTMAX_MIN __UINTMAX_MIN__

typedef __INTMAX_TYPE__ intmax_t;
#define INTMAX_MAX __INTMAX_MAX__
#define INTMAX_MIN (-INTMAX_MAX - 1)

#define INT8_MIN (-128)
#define INT16_MIN (-32767-1)
#define INT32_MIN (-2147483647-1)
#define INT8_MAX (127)
#define INT16_MAX (32767)
#define INT32_MAX (2147483647)
#define UINT8_MAX (255)
#define UINT16_MAX (65535)
#define UINT32_MAX (4294967295U)

#define INT64_C(x)  x##LL
#define UINT64_C(x) x##ULL

__END_DECLS

