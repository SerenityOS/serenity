/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2019 SAP SE and/or its affiliates. All rights reserved.
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

/*
 * This file contains implementations of NET_... functions. The NET_.. functions are
 * wrappers for common file- and socket functions plus provisions for non-blocking IO.
 *
 * (basically, the layers remember all  file descriptors waiting for a particular fd;
 *  all threads waiting on a certain fd can be woken up by sending them a signal; this
 *  is done e.g. when the fd is closed.)
 *
 * This was originally copied from the linux_close.c implementation.
 *
 * Side Note: This coding needs initialization. Under Linux this is done
 * automatically via __attribute((constructor)), on AIX this is done manually
 * (see aix_close_init).
 *
 */

/*
   AIX needs a workaround for I/O cancellation, see:
   http://publib.boulder.ibm.com/infocenter/pseries/v5r3/index.jsp?topic=/com.ibm.aix.basetechref/doc/basetrf1/close.htm
   ...
   The close subroutine is blocked until all subroutines which use the file
   descriptor return to usr space. For example, when a thread is calling close
   and another thread is calling select with the same file descriptor, the
   close subroutine does not return until the select call returns.
   ...
*/

#include <assert.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <pthread.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <sys/uio.h>
#include <unistd.h>
#include <errno.h>
#include <poll.h>
#include "jvm.h"
#include "net_util.h"

/*
 * Stack allocated by thread when doing blocking operation
 */
typedef struct threadEntry {
    pthread_t thr;                      /* this thread */
    struct threadEntry *next;           /* next thread */
    int intr;                           /* interrupted */
} threadEntry_t;

/*
 * Heap allocated during initialized - one entry per fd
 */
typedef struct {
    pthread_mutex_t lock;               /* fd lock */
    threadEntry_t *threads;             /* threads blocked on fd */
} fdEntry_t;

/*
 * Signal to unblock thread
 */
static int sigWakeup = (SIGRTMAX - 1);

/*
 * fdTable holds one entry per file descriptor, up to a certain
 * maximum.
 * Theoretically, the number of possible file descriptors can get
 * large, though usually it does not. Entries for small value file
 * descriptors are kept in a simple table, which covers most scenarios.
 * Entries for large value file descriptors are kept in an overflow
 * table, which is organized as a sparse two dimensional array whose
 * slabs are allocated on demand. This covers all corner cases while
 * keeping memory consumption reasonable.
 */

/* Base table for low value file descriptors */
static fdEntry_t* fdTable = NULL;
/* Maximum size of base table (in number of entries). */
static const int fdTableMaxSize = 0x1000; /* 4K */
/* Actual size of base table (in number of entries) */
static int fdTableLen = 0;
/* Max. theoretical number of file descriptors on system. */
static int fdLimit = 0;

/* Overflow table, should base table not be large enough. Organized as
 *   an array of n slabs, each holding 64k entries.
 */
static fdEntry_t** fdOverflowTable = NULL;
/* Number of slabs in the overflow table */
static int fdOverflowTableLen = 0;
/* Number of entries in one slab */
static const int fdOverflowTableSlabSize = 0x10000; /* 64k */
pthread_mutex_t fdOverflowTableLock = PTHREAD_MUTEX_INITIALIZER;

/*
 * Null signal handler
 */
static void sig_wakeup(int sig) {
}

/*
 * Initialization routine (executed when library is loaded)
 * Allocate fd tables and sets up signal handler.
 *
 * On AIX we don't have __attribute((constructor)) so we need to initialize
 * manually (from JNI_OnLoad() in 'src/share/native/java/net/net_util.c')
 */
void aix_close_init() {
    struct rlimit nbr_files;
    sigset_t sigset;
    struct sigaction sa;
    int i = 0;

    /* Determine the maximum number of possible file descriptors. */
    if (-1 == getrlimit(RLIMIT_NOFILE, &nbr_files)) {
        fprintf(stderr, "library initialization failed - "
                "unable to get max # of allocated fds\n");
        abort();
    }
    if (nbr_files.rlim_max != RLIM_INFINITY) {
        fdLimit = nbr_files.rlim_max;
    } else {
        /* We just do not know. */
        fdLimit = INT_MAX;
    }

    /* Allocate table for low value file descriptors. */
    fdTableLen = fdLimit < fdTableMaxSize ? fdLimit : fdTableMaxSize;
    fdTable = (fdEntry_t*) calloc(fdTableLen, sizeof(fdEntry_t));
    if (fdTable == NULL) {
        fprintf(stderr, "library initialization failed - "
                "unable to allocate file descriptor table - out of memory");
        abort();
    } else {
        for (i = 0; i < fdTableLen; i ++) {
            pthread_mutex_init(&fdTable[i].lock, NULL);
        }
    }

    /* Allocate overflow table, if needed */
    if (fdLimit > fdTableMaxSize) {
        fdOverflowTableLen = ((fdLimit - fdTableMaxSize) / fdOverflowTableSlabSize) + 1;
        fdOverflowTable = (fdEntry_t**) calloc(fdOverflowTableLen, sizeof(fdEntry_t*));
        if (fdOverflowTable == NULL) {
            fprintf(stderr, "library initialization failed - "
                    "unable to allocate file descriptor overflow table - out of memory");
            abort();
        }
    }

    /*
     * Setup the signal handler
     */
    sa.sa_handler = sig_wakeup;
    sa.sa_flags   = 0;
    sigemptyset(&sa.sa_mask);
    sigaction(sigWakeup, &sa, NULL);

    sigemptyset(&sigset);
    sigaddset(&sigset, sigWakeup);
    sigprocmask(SIG_UNBLOCK, &sigset, NULL);
}

/*
 * Return the fd table for this fd.
 */
static inline fdEntry_t *getFdEntry(int fd)
{
    fdEntry_t* result = NULL;

    if (fd < 0) {
        return NULL;
    }

    /* This should not happen. If it does, our assumption about
     * max. fd value was wrong. */
    assert(fd < fdLimit);

    if (fd < fdTableMaxSize) {
        /* fd is in base table. */
        assert(fd < fdTableLen);
        result = &fdTable[fd];
    } else {
        /* fd is in overflow table. */
        const int indexInOverflowTable = fd - fdTableMaxSize;
        const int rootindex = indexInOverflowTable / fdOverflowTableSlabSize;
        const int slabindex = indexInOverflowTable % fdOverflowTableSlabSize;
        fdEntry_t* slab = NULL;
        assert(rootindex < fdOverflowTableLen);
        assert(slabindex < fdOverflowTableSlabSize);
        pthread_mutex_lock(&fdOverflowTableLock);
        /* Allocate new slab in overflow table if needed */
        if (fdOverflowTable[rootindex] == NULL) {
            fdEntry_t* const newSlab =
                (fdEntry_t*)calloc(fdOverflowTableSlabSize, sizeof(fdEntry_t));
            if (newSlab == NULL) {
                fprintf(stderr, "Unable to allocate file descriptor overflow"
                        " table slab - out of memory");
                pthread_mutex_unlock(&fdOverflowTableLock);
                abort();
            } else {
                int i;
                for (i = 0; i < fdOverflowTableSlabSize; i ++) {
                    pthread_mutex_init(&newSlab[i].lock, NULL);
                }
                fdOverflowTable[rootindex] = newSlab;
            }
        }
        pthread_mutex_unlock(&fdOverflowTableLock);
        slab = fdOverflowTable[rootindex];
        result = &slab[slabindex];
    }

    return result;

}


/*
 * Start a blocking operation :-
 *    Insert thread onto thread list for the fd.
 */
static inline void startOp(fdEntry_t *fdEntry, threadEntry_t *self)
{
    self->thr = pthread_self();
    self->intr = 0;

    pthread_mutex_lock(&(fdEntry->lock));
    {
        self->next = fdEntry->threads;
        fdEntry->threads = self;
    }
    pthread_mutex_unlock(&(fdEntry->lock));
}

/*
 * End a blocking operation :-
 *     Remove thread from thread list for the fd
 *     If fd has been interrupted then set errno to EBADF
 */
static inline void endOp
    (fdEntry_t *fdEntry, threadEntry_t *self)
{
    int orig_errno = errno;
    pthread_mutex_lock(&(fdEntry->lock));
    {
        threadEntry_t *curr, *prev=NULL;
        curr = fdEntry->threads;
        while (curr != NULL) {
            if (curr == self) {
                if (curr->intr) {
                    orig_errno = EBADF;
                }
                if (prev == NULL) {
                    fdEntry->threads = curr->next;
                } else {
                    prev->next = curr->next;
                }
                break;
            }
            prev = curr;
            curr = curr->next;
        }
    }
    pthread_mutex_unlock(&(fdEntry->lock));
    errno = orig_errno;
}

/*
 * Close or dup2 a file descriptor ensuring that all threads blocked on
 * the file descriptor are notified via a wakeup signal.
 *
 *      fd1 < 0    => close(fd2)
 *      fd1 >= 0   => dup2(fd1, fd2)
 *
 * Returns -1 with errno set if operation fails.
 */
static int closefd(int fd1, int fd2) {
    int rv, orig_errno;
    fdEntry_t *fdEntry = getFdEntry(fd2);
    if (fdEntry == NULL) {
        errno = EBADF;
        return -1;
    }

    /*
     * Lock the fd to hold-off additional I/O on this fd.
     */
    pthread_mutex_lock(&(fdEntry->lock));

    {
        /* On fast machines we see that we enter dup2 before the
         * accepting thread had a chance to get and process the signal.
         * So in case we woke a thread up, give it some time to cope.
         * Also see https://bugs.openjdk.java.net/browse/JDK-8006395 */
        int num_woken = 0;

        /*
         * Send a wakeup signal to all threads blocked on this
         * file descriptor.
         */
        threadEntry_t *curr = fdEntry->threads;
        while (curr != NULL) {
            curr->intr = 1;
            pthread_kill( curr->thr, sigWakeup );
            num_woken ++;
            curr = curr->next;
        }

        if (num_woken > 0) {
          usleep(num_woken * 50);
        }

        /*
         * And close/dup the file descriptor
         * (restart if interrupted by signal)
         */
        do {
            if (fd1 < 0) {
                rv = close(fd2);
            } else {
                rv = dup2(fd1, fd2);
            }
        } while (rv == -1 && errno == EINTR);
    }

    /*
     * Unlock without destroying errno
     */
    orig_errno = errno;
    pthread_mutex_unlock(&(fdEntry->lock));
    errno = orig_errno;

    return rv;
}

/*
 * Wrapper for dup2 - same semantics as dup2 system call except
 * that any threads blocked in an I/O system call on fd2 will be
 * preempted and return -1/EBADF;
 */
int NET_Dup2(int fd, int fd2) {
    if (fd < 0) {
        errno = EBADF;
        return -1;
    }
    return closefd(fd, fd2);
}

/*
 * Wrapper for close - same semantics as close system call
 * except that any threads blocked in an I/O on fd will be
 * preempted and the I/O system call will return -1/EBADF.
 */
int NET_SocketClose(int fd) {
    return closefd(-1, fd);
}

/************** Basic I/O operations here ***************/

/*
 * Macro to perform a blocking IO operation. Restarts
 * automatically if interrupted by signal (other than
 * our wakeup signal)
 */
#define BLOCKING_IO_RETURN_INT(FD, FUNC) {      \
    int ret;                                    \
    threadEntry_t self;                         \
    fdEntry_t *fdEntry = getFdEntry(FD);        \
    if (fdEntry == NULL) {                      \
        errno = EBADF;                          \
        return -1;                              \
    }                                           \
    do {                                        \
        startOp(fdEntry, &self);                \
        ret = FUNC;                             \
        endOp(fdEntry, &self);                  \
    } while (ret == -1 && errno == EINTR);      \
    return ret;                                 \
}

int NET_Read(int s, void* buf, size_t len) {
    BLOCKING_IO_RETURN_INT( s, recv(s, buf, len, 0) );
}

int NET_NonBlockingRead(int s, void* buf, size_t len) {
    BLOCKING_IO_RETURN_INT(s, recv(s, buf, len, MSG_NONBLOCK));
}

int NET_RecvFrom(int s, void *buf, int len, unsigned int flags,
       struct sockaddr *from, socklen_t *fromlen) {
    BLOCKING_IO_RETURN_INT( s, recvfrom(s, buf, len, flags, from, fromlen) );
}

int NET_Send(int s, void *msg, int len, unsigned int flags) {
    BLOCKING_IO_RETURN_INT( s, send(s, msg, len, flags) );
}

int NET_SendTo(int s, const void *msg, int len,  unsigned  int
       flags, const struct sockaddr *to, int tolen) {
    BLOCKING_IO_RETURN_INT( s, sendto(s, msg, len, flags, to, tolen) );
}

int NET_Accept(int s, struct sockaddr *addr, socklen_t *addrlen) {
    BLOCKING_IO_RETURN_INT( s, accept(s, addr, addrlen) );
}

int NET_Connect(int s, struct sockaddr *addr, int addrlen) {
    int crc = -1, prc = -1;
    threadEntry_t self;
    fdEntry_t* fdEntry = getFdEntry(s);

    if (fdEntry == NULL) {
        errno = EBADF;
        return -1;
    }

    /* On AIX, when the system call connect() is interrupted, the connection
     * is not aborted and it will be established asynchronously by the kernel.
     * Hence, no need to restart connect() when EINTR is received
     */
    startOp(fdEntry, &self);
    crc = connect(s, addr, addrlen);
    endOp(fdEntry, &self);

    if (crc == -1 && errno == EINTR) {
        struct pollfd s_pollfd;
        int sockopt_arg = 0;
        socklen_t len;

        s_pollfd.fd = s;
        s_pollfd.events = POLLOUT | POLLERR;

        /* poll the file descriptor */
        do {
            startOp(fdEntry, &self);
            prc = poll(&s_pollfd, 1, -1);
            endOp(fdEntry, &self);
        } while (prc == -1  && errno == EINTR);

        if (prc < 0)
            return prc;

        len = sizeof(sockopt_arg);

        /* Check whether the connection has been established */
        if (getsockopt(s, SOL_SOCKET, SO_ERROR, &sockopt_arg, &len) == -1)
            return -1;

        if (sockopt_arg != 0 ) {
            errno = sockopt_arg;
            return -1;
        }
    } else {
        return crc;
    }

    /* At this point, fd is connected. Set successful return code */
    return 0;
}

int NET_Poll(struct pollfd *ufds, unsigned int nfds, int timeout) {
    BLOCKING_IO_RETURN_INT( ufds[0].fd, poll(ufds, nfds, timeout) );
}

/*
 * Wrapper for poll(s, timeout).
 * Auto restarts with adjusted timeout if interrupted by
 * signal other than our wakeup signal.
 */
int NET_Timeout(JNIEnv *env, int s, long timeout, jlong nanoTimeStamp) {
    jlong prevNanoTime = nanoTimeStamp;
    jlong nanoTimeout = (jlong) timeout * NET_NSEC_PER_MSEC;
    fdEntry_t *fdEntry = getFdEntry(s);

    /*
     * Check that fd hasn't been closed.
     */
    if (fdEntry == NULL) {
        errno = EBADF;
        return -1;
    }

    for(;;) {
        struct pollfd pfd;
        int rv;
        threadEntry_t self;

        /*
         * Poll the fd. If interrupted by our wakeup signal
         * errno will be set to EBADF.
         */
        pfd.fd = s;
        pfd.events = POLLIN | POLLERR;

        startOp(fdEntry, &self);
        rv = poll(&pfd, 1, nanoTimeout / NET_NSEC_PER_MSEC);
        endOp(fdEntry, &self);

        /*
         * If interrupted then adjust timeout. If timeout
         * has expired return 0 (indicating timeout expired).
         */
        if (rv < 0 && errno == EINTR) {
            if (timeout > 0) {
                jlong newNanoTime = JVM_NanoTime(env, 0);
                nanoTimeout -= newNanoTime - prevNanoTime;
                if (nanoTimeout < NET_NSEC_PER_MSEC) {
                    return 0;
                }
                prevNanoTime = newNanoTime;
            } else {
                continue; // timeout is -1, so  loop again.
            }
        } else {
            return rv;
        }
    }
}
