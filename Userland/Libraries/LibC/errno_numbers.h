/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

enum ErrnoCode {
    EPERM = 1,
#define EPERM EPERM
    ENOENT,
#define ENOENT ENOENT
    ESRCH,
#define ESRCH ESRCH
    EINTR,
#define EINTR EINTR
    EIO,
#define EIO EIO
    ENXIO,
#define ENXIO ENXIO
    E2BIG,
#define E2BIG E2BIG
    ENOEXEC,
#define ENOEXEC ENOEXEC
    EBADF,
#define EBADF EBADF
    ECHILD,
#define ECHILD ECHILD
    EAGAIN,
#define EAGAIN EAGAIN
    ENOMEM,
#define ENOMEM ENOMEM
    EACCES,
#define EACCES EACCES
    EFAULT,
#define EFAULT EFAULT
    ENOTBLK,
#define ENOTBLK ENOTBLK
    EBUSY,
#define EBUSY EBUSY
    EEXIST,
#define EEXIST EEXIST
    EXDEV,
#define EXDEV EXDEV
    ENODEV,
#define ENODEV ENODEV
    ENOTDIR,
#define ENOTDIR ENOTDIR
    EISDIR,
#define EISDIR EISDIR
    EINVAL,
#define EINVAL EINVAL
    ENFILE,
#define ENFILE ENFILE
    EMFILE,
#define EMFILE EMFILE
    ENOTTY,
#define ENOTTY ENOTTY
    ETXTBSY,
#define ETXTBSY ETXTBSY
    EFBIG,
#define EFBIG EFBIG
    ENOSPC,
#define ENOSPC ENOSPC
    ESPIPE,
#define ESPIPE ESPIPE
    EROFS,
#define EROFS EROFS
    EMLINK,
#define EMLINK EMLINK
    EPIPE,
#define EPIPE EPIPE
    ERANGE,
#define ERANGE ERANGE
    ENAMETOOLONG,
#define ENAMETOOLONG ENAMETOOLONG
    ELOOP,
#define ELOOP ELOOP
    EOVERFLOW,
#define EOVERFLOW EOVERFLOW
    EOPNOTSUPP,
#define EOPNOTSUPP EOPNOTSUPP
    ENOSYS,
#define ENOSYS ENOSYS
    ENOTIMPL,
#define ENOTIMPL ENOTIMPL
    EAFNOSUPPORT,
#define EAFNOSUPPORT EAFNOSUPPORT
    ENOTSOCK,
#define ENOTSOCK ENOTSOCK
    EADDRINUSE,
#define EADDRINUSE EADDRINUSE
    EWHYTHO,
#define EWHYTHO EWHYTHO
    ENOTEMPTY,
#define ENOTEMPTY ENOTEMPTY
    EDOM,
#define EDOM EDOM
    ECONNREFUSED,
#define ECONNREFUSED ECONNREFUSED
    EADDRNOTAVAIL,
#define EADDRNOTAVAIL EADDRNOTAVAIL
    EISCONN,
#define EISCONN EISCONN
    ECONNABORTED,
#define ECONNABORTED ECONNABORTED
    EALREADY,
#define EALREADY EALREADY
    ECONNRESET,
#define ECONNRESET ECONNRESET
    EDESTADDRREQ,
#define EDESTADDRREQ EDESTADDRREQ
    EHOSTUNREACH,
#define EHOSTUNREACH EHOSTUNREACH
    EILSEQ,
#define EILSEQ EILSEQ
    EMSGSIZE,
#define EMSGSIZE EMSGSIZE
    ENETDOWN,
#define ENETDOWN ENETDOWN
    ENETUNREACH,
#define ENETUNREACH ENETUNREACH
    ENETRESET,
#define ENETRESET ENETRESET
    ENOBUFS,
#define ENOBUFS ENOBUFS
    ENOLCK,
#define ENOLCK ENOLCK
    ENOMSG,
#define ENOMSG ENOMSG
    ENOPROTOOPT,
#define ENOPROTOOPT ENOPROTOOPT
    ENOTCONN,
#define ENOTCONN ENOTCONN
    EPROTONOSUPPORT,
#define EPROTONOSUPPORT EPROTONOSUPPORT
    EDEADLK,
#define EDEADLK EDEADLK
    ETIMEDOUT,
#define ETIMEDOUT ETIMEDOUT
    EPROTOTYPE,
#define EPROTOTYPE EPROTOTYPE
    EINPROGRESS,
#define EINPROGRESS EINPROGRESS
    ENOTHREAD,
#define ENOTHREAD ENOTHREAD
    EPROTO,
#define EPROTO EPROTO
    ENOTSUP,
#define ENOTSUP ENOTSUP
    EPFNOSUPPORT,
#define EPFNOSUPPORT EPFNOSUPPORT
    EDIRINTOSELF,
#define EDQUOT EDQUOT
    EDQUOT,
#define EDIRINTOSELF EDIRINTOSELF
    EMAXERRNO,
#define EMAXERRNO EMAXERRNO
};

#define EWOULDBLOCK EAGAIN
