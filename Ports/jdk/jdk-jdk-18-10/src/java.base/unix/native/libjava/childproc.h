/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#ifndef CHILDPROC_MD_H
#define CHILDPROC_MD_H

#include <sys/types.h>

#ifdef __APPLE__
#include <crt_externs.h>
#define environ (*_NSGetEnviron())
#else
/* This is one of the rare times it's more portable to declare an
 * external symbol explicitly, rather than via a system header.
 * The declaration is standardized as part of UNIX98, but there is
 * no standard (not even de-facto) header file where the
 * declaration is to be found.  See:
 * http://www.opengroup.org/onlinepubs/009695399/functions/environ.html
 * http://www.opengroup.org/onlinepubs/009695399/functions/xsh_chap02_02.html
 *
 * "All identifiers in this volume of IEEE Std 1003.1-2001, except
 * environ, are defined in at least one of the headers" (!)
 */
extern char **environ;
#endif

#ifdef __linux__
#include <sched.h>
#endif

#ifndef STDIN_FILENO
#define STDIN_FILENO 0
#endif

#ifndef STDOUT_FILENO
#define STDOUT_FILENO 1
#endif

#ifndef STDERR_FILENO
#define STDERR_FILENO 2
#endif

#ifndef SA_NOCLDSTOP
#define SA_NOCLDSTOP 0
#endif

#ifndef SA_RESTART
#define SA_RESTART 0
#endif

#define FAIL_FILENO (STDERR_FILENO + 1)

/* TODO: Refactor. */
#define RESTARTABLE(_cmd, _result) do { \
  do { \
    _result = _cmd; \
  } while((_result == -1) && (errno == EINTR)); \
} while(0)

/* These numbers must be the same as the Enum in ProcessImpl.java
 * Must be a better way of doing this.
 */
#define MODE_FORK 1
#define MODE_POSIX_SPAWN 2
#define MODE_VFORK 3
#define MODE_CLONE 4

typedef struct _ChildStuff
{
    int in[2];
    int out[2];
    int err[2];
    int fail[2];
    int childenv[2];
    int fds[3];
    int mode;
    const char **argv;
    int argc;
    const char **envv;
    const char *pdir;
    int redirectErrorStream;
    int sendAlivePing;
} ChildStuff;

/* following used in addition when mode is SPAWN */
typedef struct _SpawnInfo {
    int nargv; /* number of argv array elements  */
    int argvBytes; /* total number of bytes in argv array */
    int nenvv; /* number of envv array elements  */
    int envvBytes; /* total number of bytes in envv array */
    int dirlen; /* length of home directory string */
    int nparentPathv; /* number of elements in parentPathv array */
    int parentPathvBytes; /* total number of bytes in parentPathv array */
} SpawnInfo;

/* If ChildStuff.sendAlivePing is true, child shall signal aliveness to
 * the parent the moment it gains consciousness, before any subsequent
 * pre-exec errors could happen.
 * This code must fit into an int and not be a valid errno value on any of
 * our platforms. */
#define CHILD_IS_ALIVE      65535

/**
 * The cached and split version of the JDK's effective PATH.
 * (We don't support putenv("PATH=...") in native code)
 */
extern const char * const *parentPathv;

ssize_t restartableWrite(int fd, const void *buf, size_t count);
int restartableDup2(int fd_from, int fd_to);
int closeSafely(int fd);
int isAsciiDigit(char c);
int closeDescriptors(void);
int moveDescriptor(int fd_from, int fd_to);

int magicNumber();
ssize_t readFully(int fd, void *buf, size_t nbyte);
void initVectorFromBlock(const char**vector, const char* block, int count);
void execve_as_traditional_shell_script(const char *file,
                                        const char *argv[],
                                        const char *const envp[]);
void execve_with_shell_fallback(int mode, const char *file,
                                const char *argv[],
                                const char *const envp[]);
void JDK_execvpe(int mode, const char *file,
                 const char *argv[],
                 const char *const envp[]);
int childProcess(void *arg);

#endif
