/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// Includes essentially mandated by POSIX:
// https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/sys_resource.h.html
#include <sys/time.h>

#include <stdint.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

struct rusage {
    struct timeval ru_utime;
    struct timeval ru_stime;
    long ru_maxrss;
    long ru_ixrss;
    long ru_idrss;
    long ru_isrss;
    long ru_minflt;
    long ru_majflt;
    long ru_nswap;
    long ru_inblock;
    long ru_oublock;
    long ru_msgsnd;
    long ru_msgrcv;
    long ru_nsignals;
    long ru_nvcsw;
    long ru_nivcsw;
};

#define RUSAGE_SELF 1
#define RUSAGE_CHILDREN 2

int getrusage(int who, struct rusage* usage);

#define RLIMIT_CORE 1
#define RLIMIT_CPU 2
#define RLIMIT_DATA 3
#define RLIMIT_FSIZE 4
#define RLIMIT_NOFILE 5
#define RLIMIT_STACK 6
#define RLIMIT_AS 7

#define RLIM_NLIMITS 8

#define RLIM_INFINITY SIZE_MAX

typedef size_t rlim_t;

struct rlimit {
    rlim_t rlim_cur;
    rlim_t rlim_max;
};

int getrlimit(int, struct rlimit*);
int setrlimit(int, struct rlimit const*);

#define PRIO_PROCESS 0
#define PRIO_PGRP 1
#define PRIO_USER 2

int getpriority(int, id_t);
int setpriority(int, id_t, int);

__END_DECLS
