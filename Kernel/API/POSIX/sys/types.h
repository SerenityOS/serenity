/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifdef __cplusplus
extern "C" {
#endif

typedef __INT64_TYPE__ __i64;
typedef __UINT8_TYPE__ __u8;
typedef __UINT16_TYPE__ __u16;
typedef __UINT32_TYPE__ __u32;
typedef __UINT64_TYPE__ __u64;

typedef __SIZE_TYPE__ size_t;

#if defined(__x86_64__)
typedef __i64 ssize_t;
#elif defined(__aarch64__)
typedef __i64 ssize_t;
#elif defined(__riscv) && __riscv_xlen == 64
typedef __i64 ssize_t;
#else
#    error "Don't know how what ssize_t is on this architecture."
#endif

typedef unsigned char u_char;
typedef unsigned short u_short;
typedef unsigned int u_int;
typedef unsigned long u_long;

typedef __u32 uid_t;
typedef __u32 gid_t;

typedef int __pid_t;
#define pid_t __pid_t

typedef char* caddr_t;

typedef int id_t;

typedef __u64 ino_t;
typedef __i64 off_t;

typedef __u64 blkcnt_t;
typedef __u64 blksize_t;
typedef __u64 dev_t;
typedef __u16 mode_t;
typedef __u64 nlink_t;

typedef __i64 time_t;
typedef __u32 useconds_t;
typedef __i64 suseconds_t;
typedef __u64 clock_t;

typedef __u64 fsblkcnt_t;
typedef __u64 fsfilcnt_t;

#define __socklen_t_defined
#define __socklen_t __u32
typedef __socklen_t socklen_t;

struct utimbuf {
    time_t actime;
    time_t modtime;
};

typedef int pthread_t;
typedef int pthread_key_t;
typedef __u32 pthread_once_t;

typedef struct __pthread_mutex_t {
    __u32 lock;
    pthread_t owner;
    int level;
    int type;
} pthread_mutex_t;

typedef void* pthread_attr_t;
typedef struct __pthread_mutexattr_t {
    int type;
} pthread_mutexattr_t;

typedef struct __pthread_cond_t {
    pthread_mutex_t* mutex;
    __u32 value;
    int clockid; // clockid_t
} pthread_cond_t;

typedef __u64 pthread_rwlock_t;
typedef void* pthread_rwlockattr_t;
typedef struct __pthread_spinlock_t {
    int m_lock;
} pthread_spinlock_t;
typedef struct __pthread_condattr_t {
    int clockid; // clockid_t
} pthread_condattr_t;

#ifdef __cplusplus
}
#endif
