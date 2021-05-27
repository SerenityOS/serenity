/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <bits/stdint.h>
#include <stddef.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

/* There is no __SSIZE_TYPE__ but we can trick the preprocessor into defining it for us anyway! */
#define unsigned signed
typedef __SIZE_TYPE__ ssize_t;
#undef unsigned

typedef unsigned char u_char;
typedef unsigned short u_short;
typedef unsigned int u_int;
typedef unsigned long u_long;

typedef uint32_t uid_t;
typedef uint32_t gid_t;

typedef int __pid_t;
#define pid_t __pid_t

typedef char* caddr_t;

typedef int id_t;

typedef uint32_t ino_t;
typedef int64_t off_t;

typedef uint32_t blkcnt_t;
typedef uint32_t blksize_t;
typedef uint32_t dev_t;
typedef uint16_t mode_t;
typedef uint32_t nlink_t;

typedef int64_t time_t;
typedef uint32_t useconds_t;
typedef int32_t suseconds_t;
typedef uint32_t clock_t;

typedef uint64_t fsblkcnt_t;
typedef uint64_t fsfilcnt_t;

#define __socklen_t_defined
#define __socklen_t uint32_t
typedef __socklen_t socklen_t;

struct utimbuf {
    time_t actime;
    time_t modtime;
};

typedef int pthread_t;
typedef int pthread_key_t;
typedef uint32_t pthread_once_t;

typedef struct __pthread_mutex_t {
    uint32_t lock;
    pthread_t owner;
    int level;
    int type;
} pthread_mutex_t;

typedef void* pthread_attr_t;
typedef struct __pthread_mutexattr_t {
    int type;
} pthread_mutexattr_t;

typedef struct __pthread_cond_t {
    uint32_t value;
    uint32_t previous;
    int clockid; // clockid_t
} pthread_cond_t;

typedef uint64_t pthread_rwlock_t;
typedef void* pthread_rwlockattr_t;
typedef struct __pthread_spinlock_t {
    int m_lock;
} pthread_spinlock_t;
typedef struct __pthread_condattr_t {
    int clockid; // clockid_t
} pthread_condattr_t;

inline dev_t makedev(unsigned int major, unsigned int minor) { return (minor & 0xffu) | (major << 8u) | ((minor & ~0xffu) << 12u); }
inline unsigned int major(dev_t dev) { return (dev & 0xfff00u) >> 8u; }
inline unsigned int minor(dev_t dev) { return (dev & 0xffu) | ((dev >> 12u) & 0xfff00u); }

__END_DECLS
