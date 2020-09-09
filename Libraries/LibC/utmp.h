/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
