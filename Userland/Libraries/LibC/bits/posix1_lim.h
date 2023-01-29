/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define _POSIX_AIO_LISTIO_MAX 2               /* The number of I/O operations that can be specified in a list I/O call. */
#define _POSIX_AIO_MAX 1                      /* The number of outstanding asynchronous I/O operations. */
#define _POSIX_ARG_MAX 4096                   /* Maximum length of argument to the exec functions including environment data. */
#define _POSIX_CHILD_MAX 25                   /* Maximum number of simultaneous processes per real user ID. */
#define _POSIX_DELAYTIMER_MAX 32              /* The number of timer expiration overruns. */
#define _POSIX_HOST_NAME_MAX 255              /* Maximum length of a host name (not including the terminating null) as returned from the gethostname() function. */
#define _POSIX_LINK_MAX 8                     /* Maximum number of links to a single file. */
#define _POSIX_LOGIN_NAME_MAX 9               /* The size of the storage required for a login name, in bytes, including the terminating null. */
#define _POSIX_MAX_CANON 255                  /* Maximum number of bytes in a terminal canonical input queue. */
#define _POSIX_MAX_INPUT 255                  /* Maximum number of bytes allowed in a terminal input queue. */
#define _POSIX_MQ_OPEN_MAX 8                  /* The number of message queues that can be open for a single process.) */
#define _POSIX_MQ_PRIO_MAX 32                 /* The maximum number of message priorities supported by the implementation. */
#define _POSIX_NAME_MAX 14                    /* Maximum number of bytes in a filename (not including terminating null). */
#define _POSIX_NGROUPS_MAX 8                  /* Maximum number of simultaneous supplementary group IDs per process. */
#define _POSIX_OPEN_MAX 20                    /* Maximum number of files that one process can have open at any one time. */
#define _POSIX_PATH_MAX 256                   /* Maximum number of bytes in a pathname. */
#define _POSIX_PIPE_BUF 512                   /* Maximum number of bytes that is guaranteed to be atomic when writing to a pipe. */
#define _POSIX_RE_DUP_MAX 255                 /* The number of repeated occurrences of a BRE permitted by the regexec() and regcomp() functions when using the interval notation #define \(m,n\}; see BREs Matching Multiple Characters. */
#define _POSIX_RTSIG_MAX 8                    /* The number of realtime signal numbers reserved for application use. */
#define _POSIX_SEM_NSEMS_MAX 256              /* The number of semaphores that a process may have. */
#define _POSIX_SEM_VALUE_MAX 32767            /* The maximum value a semaphore may have. */
#define _POSIX_SIGQUEUE_MAX 32                /* The number of queued signals that a process may send and have pending at the receiver(s) at any time. */
#define _POSIX_SSIZE_MAX 32767                /* The value that can be stored in an object of type ssize_t. */
#define _POSIX_SS_REPL_MAX 4                  /* The number of replenishment operations that may be simultaneously pending for a particular sporadic server scheduler. */
#define _POSIX_STREAM_MAX 8                   /* The number of streams that one process can have open at one time. */
#define _POSIX_SYMLINK_MAX 255                /* The number of bytes in a symbolic link. */
#define _POSIX_SYMLOOP_MAX 8                  /* The number of symbolic links that can be traversed in the resolution of a pathname in the absence of a loop. */
#define _POSIX_THREAD_DESTRUCTOR_ITERATIONS 4 /* The number of attempts made to destroy a thread's thread-specific data values on thread exit. */
#define _POSIX_THREAD_KEYS_MAX 128            /* The number of data keys per process. */
#define _POSIX_THREAD_THREADS_MAX 64          /* The number of threads per process. */
#define _POSIX_TIMER_MAX 32                   /* The per-process number of timers. */
#define _POSIX_TRACE_EVENT_NAME_MAX 30        /* The length in bytes of a trace event name. */
#define _POSIX_TRACE_NAME_MAX 8               /* The length in bytes of a trace generation version string or a trace stream name. */
#define _POSIX_TRACE_SYS_MAX 8                /* The number of trace streams that may simultaneously exist in the system. */
#define _POSIX_TRACE_USER_EVENT_MAX 32        /* The number of user trace event type identifiers that may simultaneously exist in a traced process, including the predefined user trace event POSIX_TRACE_UNNAMED_USER_EVENT. */
#define _POSIX_TTY_NAME_MAX 9                 /* The size of the storage required for a terminal device name, in bytes, including the terminating null. */
#define _POSIX_TZNAME_MAX 6                   /* Maximum number of bytes supported for the name of a timezone (not of the TZ variable). */
