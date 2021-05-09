/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>
#include <sys/time.h>

__BEGIN_DECLS

struct exit_status {         /* Type for ut_exit, below */
    short int e_termination; /* Process termination status */
    short int e_exit;        /* Process exit status */
};

#define USER_PROCESS 7
#define DEAD_PROCESS 8

#define UT_NAMESIZE 32
#define UT_LINESIZE 32
#define UT_HOSTSIZE 256

struct utmp {
    short ut_type;              /* Type of record */
    pid_t ut_pid;               /* PID of login process */
    char ut_line[UT_LINESIZE];  /* Device name of tty - "/dev/" */
    char ut_id[4];              /* Terminal name suffix,
                                     or inittab(5) ID */
    char ut_user[UT_NAMESIZE];  /* Username */
    char ut_host[UT_HOSTSIZE];  /* Hostname for remote login, or
                                     kernel version for run-level
                                     messages */
    struct exit_status ut_exit; /* Exit status of a process
                                     marked as DEAD_PROCESS; not
                                     used by Linux init (1 */

    long ut_session;      /* Session ID */
    struct timeval ut_tv; /* Time entry was made */

    int32_t ut_addr_v6[4]; /* Internet address of remote
                                     host; IPv4 address uses
                                     just ut_addr_v6[0] */

    char __unused[20]; /* Reserved for future use */
};

/* Backward compatibility hacks */
#define ut_name ut_user
#ifndef _NO_UT_TIME
#    define ut_time ut_tv.tv_sec
#endif
#define ut_xtime ut_tv.tv_sec
#define ut_addr ut_addr_v6[0]

__END_DECLS
