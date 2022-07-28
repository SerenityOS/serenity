/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/errno.h>

// NOTE: You can't define with a macro, so these have to be duplicated.
#define ESUCCESS          ESUCCESS
#define EPERM             EPERM
#define ENOENT            ENOENT
#define ESRCH             ESRCH
#define EINTR             EINTR
#define EIO               EIO
#define ENXIO             ENXIO
#define E2BIG             E2BIG
#define ENOEXEC           ENOEXEC
#define EBADF             EBADF
#define ECHILD            ECHILD
#define EAGAIN            EAGAIN
#define ENOMEM            ENOMEM
#define EACCES            EACCES
#define EFAULT            EFAULT
#define ENOTBLK           ENOTBLK
#define EBUSY             EBUSY
#define EEXIST            EEXIST
#define EXDEV             EXDEV
#define ENODEV            ENODEV
#define ENOTDIR           ENOTDIR
#define EISDIR            EISDIR
#define EINVAL            EINVAL
#define ENFILE            ENFILE
#define EMFILE            EMFILE
#define ENOTTY            ENOTTY
#define ETXTBSY           ETXTBSY
#define EFBIG             EFBIG
#define ENOSPC            ENOSPC
#define ESPIPE            ESPIPE
#define EROFS             EROFS
#define EMLINK            EMLINK
#define EPIPE             EPIPE
#define ERANGE            ERANGE
#define ENAMETOOLONG      ENAMETOOLONG
#define ELOOP             ELOOP
#define EOVERFLOW         EOVERFLOW
#define EOPNOTSUPP        EOPNOTSUPP
#define ENOSYS            ENOSYS
#define ENOTIMPL          ENOTIMPL
#define EAFNOSUPPORT      EAFNOSUPPORT
#define ENOTSOCK          ENOTSOCK
#define EADDRINUSE        EADDRINUSE
#define ENOTEMPTY         ENOTEMPTY
#define EDOM              EDOM
#define ECONNREFUSED      ECONNREFUSED
#define EHOSTDOWN         EHOSTDOWN
#define EADDRNOTAVAIL     EADDRNOTAVAIL
#define EISCONN           EISCONN
#define ECONNABORTED      ECONNABORTED
#define EALREADY          EALREADY
#define ECONNRESET        ECONNRESET
#define EDESTADDRREQ      EDESTADDRREQ
#define EHOSTUNREACH      EHOSTUNREACH
#define EILSEQ            EILSEQ
#define EMSGSIZE          EMSGSIZE
#define ENETDOWN          ENETDOWN
#define ENETUNREACH       ENETUNREACH
#define ENETRESET         ENETRESET
#define ENOBUFS           ENOBUFS
#define ENOLCK            ENOLCK
#define ENOMSG            ENOMSG
#define ENOPROTOOPT       ENOPROTOOPT
#define ENOTCONN          ENOTCONN
#define ESHUTDOWN         ESHUTDOWN
#define ETOOMANYREFS      ETOOMANYREFS
#define ESOCKTNOSUPPORT   ESOCKTNOSUPPORT
#define EPROTONOSUPPORT   EPROTONOSUPPORT
#define EDEADLK           EDEADLK
#define ETIMEDOUT         ETIMEDOUT
#define EPROTOTYPE        EPROTOTYPE
#define EINPROGRESS       EINPROGRESS
#define ENOTHREAD         ENOTHREAD
#define EPROTO            EPROTO
#define ENOTSUP           ENOTSUP
#define EPFNOSUPPORT      EPFNOSUPPORT
#define EDIRINTOSELF      EDIRINTOSELF
#define EDQUOT            EDQUOT
#define ENOTRECOVERABLE   ENOTRECOVERABLE
#define ECANCELED         ECANCELED
#define EPROMISEVIOLATION EPROMISEVIOLATION
#define ESTALE            ESTALE
#define EMAXERRNO         EMAXERRNO

#define EWOULDBLOCK EAGAIN
#define ELAST       EMAXERRNO
