/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define ENUMERATE_ERRNO_CODES(E)                                      \
    E(ESUCCESS, "Success (not an error)")                             \
    E(EPERM, "Operation not permitted")                               \
    E(ENOENT, "No such file or directory")                            \
    E(ESRCH, "No such process")                                       \
    E(EINTR, "Interrupted syscall")                                   \
    E(EIO, "I/O error")                                               \
    E(ENXIO, "No such device or address")                             \
    E(E2BIG, "Argument list too long")                                \
    E(ENOEXEC, "Exec format error")                                   \
    E(EBADF, "Bad fd number")                                         \
    E(ECHILD, "No child processes")                                   \
    E(EAGAIN, "Try again")                                            \
    E(ENOMEM, "Out of memory")                                        \
    E(EACCES, "Permission denied")                                    \
    E(EFAULT, "Bad address")                                          \
    E(ENOTBLK, "Block device required")                               \
    E(EBUSY, "Device or resource busy")                               \
    E(EEXIST, "File already exists")                                  \
    E(EXDEV, "Cross-device link")                                     \
    E(ENODEV, "No such device")                                       \
    E(ENOTDIR, "Not a directory")                                     \
    E(EISDIR, "Is a directory")                                       \
    E(EINVAL, "Invalid argument")                                     \
    E(ENFILE, "File table overflow")                                  \
    E(EMFILE, "Too many open files")                                  \
    E(ENOTTY, "Not a TTY")                                            \
    E(ETXTBSY, "Text file busy")                                      \
    E(EFBIG, "File too large")                                        \
    E(ENOSPC, "No space left on device")                              \
    E(ESPIPE, "Illegal seek")                                         \
    E(EROFS, "Read-only filesystem")                                  \
    E(EMLINK, "Too many links")                                       \
    E(EPIPE, "Broken pipe")                                           \
    E(ERANGE, "Range error")                                          \
    E(ENAMETOOLONG, "Name too long")                                  \
    E(ELOOP, "Too many symlinks")                                     \
    E(EOVERFLOW, "Overflow")                                          \
    E(EOPNOTSUPP, "Operation not supported")                          \
    E(ENOSYS, "No such syscall")                                      \
    E(ENOTIMPL, "Not implemented")                                    \
    E(EAFNOSUPPORT, "Address family not supported")                   \
    E(ENOTSOCK, "Not a socket")                                       \
    E(EADDRINUSE, "Address in use")                                   \
    E(ENOTEMPTY, "Directory not empty")                               \
    E(EDOM, "Math argument out of domain")                            \
    E(ECONNREFUSED, "Connection refused")                             \
    E(EHOSTDOWN, "Host is down")                                      \
    E(EADDRNOTAVAIL, "Address not available")                         \
    E(EISCONN, "Already connected")                                   \
    E(ECONNABORTED, "Connection aborted")                             \
    E(EALREADY, "Connection already in progress")                     \
    E(ECONNRESET, "Connection reset")                                 \
    E(EDESTADDRREQ, "Destination address required")                   \
    E(EHOSTUNREACH, "Host unreachable")                               \
    E(EILSEQ, "Illegal byte sequence")                                \
    E(EMSGSIZE, "Message size")                                       \
    E(ENETDOWN, "Network down")                                       \
    E(ENETUNREACH, "Network unreachable")                             \
    E(ENETRESET, "Network reset")                                     \
    E(ENOBUFS, "No buffer space")                                     \
    E(ENOLCK, "No lock available")                                    \
    E(ENOMSG, "No message")                                           \
    E(ENOPROTOOPT, "No protocol option")                              \
    E(ENOTCONN, "Not connected")                                      \
    E(ESHUTDOWN, "Transport endpoint has shutdown")                   \
    E(ETOOMANYREFS, "Too many references")                            \
    E(ESOCKTNOSUPPORT, "Socket type not supported")                   \
    E(EPROTONOSUPPORT, "Protocol not supported")                      \
    E(EDEADLK, "Resource deadlock would occur")                       \
    E(ETIMEDOUT, "Timed out")                                         \
    E(EPROTOTYPE, "Wrong protocol type")                              \
    E(EINPROGRESS, "Operation in progress")                           \
    E(ENOTHREAD, "No such thread")                                    \
    E(EPROTO, "Protocol error")                                       \
    E(ENOTSUP, "Not supported")                                       \
    E(EPFNOSUPPORT, "Protocol family not supported")                  \
    E(EDIRINTOSELF, "Cannot make directory a subdirectory of itself") \
    E(EDQUOT, "Quota exceeded")                                       \
    E(ENOTRECOVERABLE, "State not recoverable")                       \
    E(ECANCELED, "Operation cancelled")                               \
    E(EPROMISEVIOLATION, "The process has a promise violation")       \
    E(ESTALE, "Stale network file handle")                            \
    E(ESRCNOTFOUND, "System resource not found")                      \
    E(EMAXERRNO, "The highest errno +1 :^)")

enum ErrnoCode {
#define __ENUMERATE_ERRNO_CODE(c, s) c,
    ENUMERATE_ERRNO_CODES(__ENUMERATE_ERRNO_CODE)
#undef __ENUMERATE_ERRNO_CODE
};
