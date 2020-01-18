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

#include <stdarg.h>

__BEGIN_DECLS

struct syslog_data {
    const char* ident;
    int logopt;
    int facility;
    int maskpri;
};

/* The severity of the message. This is ordered. */
#define LOG_EMERG   0
#define LOG_ALERT   1
#define LOG_CRIT    2
#define LOG_ERR     3
#define LOG_WARNING 4
#define LOG_NOTICE  5
#define LOG_INFO    6
#define LOG_DEBUG   7

/* Macros for masking out the priority of a combined priority */
#define LOG_PRIMASK (7)
#define LOG_PRI(priority) ((priority) & LOG_PRIMASK)

/*
 * Many of these facilities don't really make sense anymore, but we keep them
 * for compatability purposes.
 */
#define LOG_KERN     ( 0 << 3)
#define LOG_USER     ( 1 << 3)
#define LOG_MAIL     ( 2 << 3)
#define LOG_DAEMON   ( 3 << 3)
#define LOG_AUTH     ( 4 << 3)
#define LOG_SYSLOG   ( 5 << 3)
#define LOG_LPR      ( 6 << 3) 
#define LOG_NEWS     ( 7 << 3) 
#define LOG_UUCP     ( 8 << 3)
#define LOG_CRON     ( 9 << 3)
#define LOG_AUTHPRIV (10 << 3)
#define LOG_FTP      (11 << 3)
/* glibc and OpenBSD reserve 12..15 for future system usage, we will too */
#define LOG_LOCAL0   (16 << 3)
#define LOG_LOCAL1   (17 << 3)
#define LOG_LOCAL2   (18 << 3)
#define LOG_LOCAL3   (19 << 3)
#define LOG_LOCAL4   (20 << 3)
#define LOG_LOCAL5   (21 << 3)
#define LOG_LOCAL6   (22 << 3)
#define LOG_LOCAL7   (23 << 3)

#define LOG_NFACILITIES 24

/* Macros to get the facility from a combined priority. */
#define LOG_FACMASK (~7)
#define LOG_FAC(priority) (((priority) & LOG_FACMASK) >> 3)

/* For masking logs, we use these macros with just the priority. */
#define LOG_MASK(priority) (1 << (priority))
#define LOG_UPTO(priority) (LOG_MASK(priority) + (LOG_MASK(priority) - 1))

/* Macro to make a combined priority. */
#define LOG_MAKEPRI(facility, priority) ((facility) | (priority))

/* Include a PID with the message. */
#define LOG_PID    (1 << 0)
/* Log on the console. */
#define LOG_CONS   (1 << 1)
/* Open the syslogd connection at the first call. (not implemented, default) */
#define LOG_ODELAY (1 << 2)
/* Open the syslogd connection immediately. (not implemented) */
#define LOG_NDELAY (1 << 3)
/* Log to stderr as well. */
#define LOG_PERROR (1 << 4)

/* This is useful to have, but has to be stored weirdly for compatibility. */
#ifdef SYSLOG_NAMES
/* Used for marking the fallback; some applications check for these defines. */
#    define INTERNAL_NOPRI 0x10
#    define INTERNAL_MARK LOG_MAKEPRI(LOG_NFACILITIES << 3, 0)

typedef struct _code {
    /*
     * Most Unices define this as char*, but in C++, we have to define it as a
     * const char* if we want to use string constants.
     */
    const char* c_name;
    int c_val;
} CODE;

/*
 * The names we use are the same as what glibc and OpenBSD use. We omit
 * deprecated values in the hope that no one uses them. Sorted, as well.
 */

CODE prioritynames[] = {
    { "alert",   LOG_ALERT },
    { "crit",    LOG_CRIT },
    { "debug",   LOG_DEBUG },
    { "emerg",   LOG_EMERG },
    { "err",     LOG_ERR },
    { "info",    LOG_INFO },
    /* Fallback */
    { "none",    INTERNAL_NOPRI },
    { "notice",  LOG_NOTICE },
    { "warning", LOG_WARNING },
    { NULL, -1 },
};

CODE facilitynames[] = {
    { "auth",     LOG_AUTH },
    { "authpriv", LOG_AUTHPRIV },
    { "cron",     LOG_CRON },
    { "daemon",   LOG_DAEMON },
    { "ftp",      LOG_FTP },
    { "kern",     LOG_KERN },
    { "local0",   LOG_LOCAL0 },
    { "local1",   LOG_LOCAL1 },
    { "local2",   LOG_LOCAL2 },
    { "local3",   LOG_LOCAL3 },
    { "local4",   LOG_LOCAL4 },
    { "local5",   LOG_LOCAL5 },
    { "local6",   LOG_LOCAL6 },
    { "local7",   LOG_LOCAL7 },
    { "lpr",      LOG_LPR },
    { "mail",     LOG_MAIL },
    /* Fallback */
    { "mark",     INTERNAL_MARK },
    { "news",     LOG_NEWS },
    { "syslog",   LOG_SYSLOG },
    { "user",     LOG_USER },
    { "uucp",     LOG_UUCP },
    { NULL, -1 },
};
#endif

/* The re-entrant versions are an OpenBSD extension we also implement. */
void syslog(int, const char*, ...);
void syslog_r(int, struct syslog_data*, const char*, ...);
void vsyslog(int, const char* message, va_list);
void vsyslog_r(int, struct syslog_data* data, const char* message, va_list);
void openlog(const char*, int, int);
void openlog_r(const char*, int, int, struct syslog_data*);
void closelog(void);
void closelog_r(struct syslog_data*);
int setlogmask(int);
int setlogmask_r(int, struct syslog_data*);

__END_DECLS
