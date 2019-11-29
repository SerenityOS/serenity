#pragma once

#ifndef KERNEL

#include <sys/cdefs.h>
#include <sys/types.h>

#ifdef __cplusplus
#    define NULL nullptr
#else
#    define NULL ((void*)0)
#endif

typedef __PTRDIFF_TYPE__ ptrdiff_t;
typedef __SIZE_TYPE__ size_t;

#endif
