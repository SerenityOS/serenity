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

#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <limits.h>

#include "childproc.h"

const char * const *parentPathv;

ssize_t
restartableWrite(int fd, const void *buf, size_t count)
{
    ssize_t result;
    RESTARTABLE(write(fd, buf, count), result);
    return result;
}

int
restartableDup2(int fd_from, int fd_to)
{
    int err;
    RESTARTABLE(dup2(fd_from, fd_to), err);
    return err;
}

int
closeSafely(int fd)
{
    return (fd == -1) ? 0 : close(fd);
}

int
isAsciiDigit(char c)
{
  return c >= '0' && c <= '9';
}

#if defined(_AIX)
  /* AIX does not understand '/proc/self' - it requires the real process ID */
  #define FD_DIR aix_fd_dir
  #define DIR DIR64
  #define dirent dirent64
  #define opendir opendir64
  #define readdir readdir64
  #define closedir closedir64
#elif defined(_ALLBSD_SOURCE)
  #define FD_DIR "/dev/fd"
#else
  #define FD_DIR "/proc/self/fd"
#endif

int
closeDescriptors(void)
{
    DIR *dp;
    struct dirent *dirp;
    int from_fd = FAIL_FILENO + 1;

    /* We're trying to close all file descriptors, but opendir() might
     * itself be implemented using a file descriptor, and we certainly
     * don't want to close that while it's in use.  We assume that if
     * opendir() is implemented using a file descriptor, then it uses
     * the lowest numbered file descriptor, just like open().  So we
     * close a couple explicitly.  */

    close(from_fd);          /* for possible use by opendir() */
    close(from_fd + 1);      /* another one for good luck */

#if defined(_AIX)
    /* AIX does not understand '/proc/self' - it requires the real process ID */
    char aix_fd_dir[32];     /* the pid has at most 19 digits */
    snprintf(aix_fd_dir, 32, "/proc/%d/fd", getpid());
#endif

    if ((dp = opendir(FD_DIR)) == NULL)
        return 0;

    while ((dirp = readdir(dp)) != NULL) {
        int fd;
        if (isAsciiDigit(dirp->d_name[0]) &&
            (fd = strtol(dirp->d_name, NULL, 10)) >= from_fd + 2)
            close(fd);
    }

    closedir(dp);

    return 1;
}

int
moveDescriptor(int fd_from, int fd_to)
{
    if (fd_from != fd_to) {
        if ((restartableDup2(fd_from, fd_to) == -1) ||
            (close(fd_from) == -1))
            return -1;
    }
    return 0;
}

int
magicNumber() {
    return 43110;
}

/*
 * Reads nbyte bytes from file descriptor fd into buf,
 * The read operation is retried in case of EINTR or partial reads.
 *
 * Returns number of bytes read (normally nbyte, but may be less in
 * case of EOF).  In case of read errors, returns -1 and sets errno.
 */
ssize_t
readFully(int fd, void *buf, size_t nbyte)
{
    ssize_t remaining = nbyte;
    for (;;) {
        ssize_t n = read(fd, buf, remaining);
        if (n == 0) {
            return nbyte - remaining;
        } else if (n > 0) {
            remaining -= n;
            if (remaining <= 0)
                return nbyte;
            /* We were interrupted in the middle of reading the bytes.
             * Unlikely, but possible. */
            buf = (void *) (((char *)buf) + n);
        } else if (errno == EINTR) {
            /* Strange signals like SIGJVM1 are possible at any time.
             * See http://www.dreamsongs.com/WorseIsBetter.html */
        } else {
            return -1;
        }
    }
}

void
initVectorFromBlock(const char**vector, const char* block, int count)
{
    int i;
    const char *p;
    for (i = 0, p = block; i < count; i++) {
        /* Invariant: p always points to the start of a C string. */
        vector[i] = p;
        while (*(p++));
    }
    vector[count] = NULL;
}

/**
 * Exec FILE as a traditional Bourne shell script (i.e. one without #!).
 * If we could do it over again, we would probably not support such an ancient
 * misfeature, but compatibility wins over sanity.  The original support for
 * this was imported accidentally from execvp().
 */
void
execve_as_traditional_shell_script(const char *file,
                                   const char *argv[],
                                   const char *const envp[])
{
    /* Use the extra word of space provided for us in argv by caller. */
    const char *argv0 = argv[0];
    const char *const *end = argv;
    while (*end != NULL)
        ++end;
    memmove(argv+2, argv+1, (end-argv) * sizeof(*end));
    argv[0] = "/bin/sh";
    argv[1] = file;
    execve(argv[0], (char **) argv, (char **) envp);
    /* Can't even exec /bin/sh?  Big trouble, but let's soldier on... */
    memmove(argv+1, argv+2, (end-argv) * sizeof(*end));
    argv[0] = argv0;
}

/**
 * Like execve(2), except that in case of ENOEXEC, FILE is assumed to
 * be a shell script and the system default shell is invoked to run it.
 */
void
execve_with_shell_fallback(int mode, const char *file,
                           const char *argv[],
                           const char *const envp[])
{
    if (mode == MODE_CLONE || mode == MODE_VFORK) {
        /* shared address space; be very careful. */
        execve(file, (char **) argv, (char **) envp);
        if (errno == ENOEXEC)
            execve_as_traditional_shell_script(file, argv, envp);
    } else {
        /* unshared address space; we can mutate environ. */
        environ = (char **) envp;
        execvp(file, (char **) argv);
    }
}

/**
 * 'execvpe' should have been included in the Unix standards,
 * and is a GNU extension in glibc 2.10.
 *
 * JDK_execvpe is identical to execvp, except that the child environment is
 * specified via the 3rd argument instead of being inherited from environ.
 */
void
JDK_execvpe(int mode, const char *file,
            const char *argv[],
            const char *const envp[])
{
    if (envp == NULL || (char **) envp == environ) {
        execvp(file, (char **) argv);
        return;
    }

    if (*file == '\0') {
        errno = ENOENT;
        return;
    }

    if (strchr(file, '/') != NULL) {
        execve_with_shell_fallback(mode, file, argv, envp);
    } else {
        /* We must search PATH (parent's, not child's) */
        char expanded_file[PATH_MAX];
        int filelen = strlen(file);
        int sticky_errno = 0;
        const char * const * dirs;
        for (dirs = parentPathv; *dirs; dirs++) {
            const char * dir = *dirs;
            int dirlen = strlen(dir);
            if (filelen + dirlen + 2 >= PATH_MAX) {
                errno = ENAMETOOLONG;
                continue;
            }
            memcpy(expanded_file, dir, dirlen);
            if (expanded_file[dirlen - 1] != '/')
                expanded_file[dirlen++] = '/';
            memcpy(expanded_file + dirlen, file, filelen);
            expanded_file[dirlen + filelen] = '\0';
            execve_with_shell_fallback(mode, expanded_file, argv, envp);
            /* There are 3 responses to various classes of errno:
             * return immediately, continue (especially for ENOENT),
             * or continue with "sticky" errno.
             *
             * From exec(3):
             *
             * If permission is denied for a file (the attempted
             * execve returned EACCES), these functions will continue
             * searching the rest of the search path.  If no other
             * file is found, however, they will return with the
             * global variable errno set to EACCES.
             */
            switch (errno) {
            case EACCES:
                sticky_errno = errno;
                /* FALLTHRU */
            case ENOENT:
            case ENOTDIR:
#ifdef ELOOP
            case ELOOP:
#endif
#ifdef ESTALE
            case ESTALE:
#endif
#ifdef ENODEV
            case ENODEV:
#endif
#ifdef ETIMEDOUT
            case ETIMEDOUT:
#endif
                break; /* Try other directories in PATH */
            default:
                return;
            }
        }
        if (sticky_errno != 0)
            errno = sticky_errno;
    }
}

/**
 * Child process after a successful fork().
 * This function must not return, and must be prepared for either all
 * of its address space to be shared with its parent, or to be a copy.
 * It must not modify global variables such as "environ".
 */
int
childProcess(void *arg)
{
    const ChildStuff* p = (const ChildStuff*) arg;
    int fail_pipe_fd = p->fail[1];

    if (p->sendAlivePing) {
        /* Child shall signal aliveness to parent at the very first
         * moment. */
        int code = CHILD_IS_ALIVE;
        restartableWrite(fail_pipe_fd, &code, sizeof(code));
    }

    /* Close the parent sides of the pipes.
       Closing pipe fds here is redundant, since closeDescriptors()
       would do it anyways, but a little paranoia is a good thing. */
    if ((closeSafely(p->in[1])   == -1) ||
        (closeSafely(p->out[0])  == -1) ||
        (closeSafely(p->err[0])  == -1) ||
        (closeSafely(p->childenv[0])  == -1) ||
        (closeSafely(p->childenv[1])  == -1) ||
        (closeSafely(p->fail[0]) == -1))
        goto WhyCantJohnnyExec;

    /* Give the child sides of the pipes the right fileno's. */
    /* Note: it is possible for in[0] == 0 */
    if ((moveDescriptor(p->in[0] != -1 ?  p->in[0] : p->fds[0],
                        STDIN_FILENO) == -1) ||
        (moveDescriptor(p->out[1]!= -1 ? p->out[1] : p->fds[1],
                        STDOUT_FILENO) == -1))
        goto WhyCantJohnnyExec;

    if (p->redirectErrorStream) {
        if ((closeSafely(p->err[1]) == -1) ||
            (restartableDup2(STDOUT_FILENO, STDERR_FILENO) == -1))
            goto WhyCantJohnnyExec;
    } else {
        if (moveDescriptor(p->err[1] != -1 ? p->err[1] : p->fds[2],
                           STDERR_FILENO) == -1)
            goto WhyCantJohnnyExec;
    }

    if (moveDescriptor(fail_pipe_fd, FAIL_FILENO) == -1)
        goto WhyCantJohnnyExec;

    /* We moved the fail pipe fd */
    fail_pipe_fd = FAIL_FILENO;

    /* close everything */
    if (closeDescriptors() == 0) { /* failed,  close the old way */
        int max_fd = (int)sysconf(_SC_OPEN_MAX);
        int fd;
        for (fd = FAIL_FILENO + 1; fd < max_fd; fd++)
            if (close(fd) == -1 && errno != EBADF)
                goto WhyCantJohnnyExec;
    }

    /* change to the new working directory */
    if (p->pdir != NULL && chdir(p->pdir) < 0)
        goto WhyCantJohnnyExec;

    if (fcntl(FAIL_FILENO, F_SETFD, FD_CLOEXEC) == -1)
        goto WhyCantJohnnyExec;

    JDK_execvpe(p->mode, p->argv[0], p->argv, p->envv);

 WhyCantJohnnyExec:
    /* We used to go to an awful lot of trouble to predict whether the
     * child would fail, but there is no reliable way to predict the
     * success of an operation without *trying* it, and there's no way
     * to try a chdir or exec in the parent.  Instead, all we need is a
     * way to communicate any failure back to the parent.  Easy; we just
     * send the errno back to the parent over a pipe in case of failure.
     * The tricky thing is, how do we communicate the *success* of exec?
     * We use FD_CLOEXEC together with the fact that a read() on a pipe
     * yields EOF when the write ends (we have two of them!) are closed.
     */
    {
        int errnum = errno;
        restartableWrite(fail_pipe_fd, &errnum, sizeof(errnum));
    }
    close(fail_pipe_fd);
    _exit(-1);
    return 0;  /* Suppress warning "no return value from function" */
}
