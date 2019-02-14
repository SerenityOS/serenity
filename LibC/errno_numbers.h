#pragma once

#define __ENUMERATE_ALL_ERRORS \
    __ERROR(ESUCCESS,       "Success (not an error)") \
    __ERROR(EPERM,          "Operation not permitted") \
    __ERROR(ENOENT,         "No such file or directory") \
    __ERROR(ESRCH,          "No such process") \
    __ERROR(EINTR,          "Interrupted syscall") \
    __ERROR(EIO,            "I/O error") \
    __ERROR(ENXIO,          "No such device or address") \
    __ERROR(E2BIG,          "Argument list too long") \
    __ERROR(ENOEXEC,        "Exec format error") \
    __ERROR(EBADF,          "Bad fd number") \
    __ERROR(ECHILD,         "No child processes") \
    __ERROR(EAGAIN,         "Try again") \
    __ERROR(ENOMEM,         "Out of memory") \
    __ERROR(EACCES,         "Permission denied") \
    __ERROR(EFAULT,         "Bad address") \
    __ERROR(ENOTBLK,        "Block device required") \
    __ERROR(EBUSY,          "Device or resource busy") \
    __ERROR(EEXIST,         "File already exists") \
    __ERROR(EXDEV,          "Cross-device link") \
    __ERROR(ENODEV,         "No such device") \
    __ERROR(ENOTDIR,        "Not a directory") \
    __ERROR(EISDIR,         "Is a directory") \
    __ERROR(EINVAL,         "Invalid argument") \
    __ERROR(ENFILE,         "File table overflow") \
    __ERROR(EMFILE,         "Too many open files") \
    __ERROR(ENOTTY,         "Not a TTY") \
    __ERROR(ETXTBSY,        "Text file busy") \
    __ERROR(EFBIG,          "File too large") \
    __ERROR(ENOSPC,         "No space left on device") \
    __ERROR(ESPIPE,         "Illegal seek") \
    __ERROR(EROFS,          "Read-only filesystem") \
    __ERROR(EMLINK,         "Too many links") \
    __ERROR(EPIPE,          "Broken pipe") \
    __ERROR(ERANGE,         "Range error") \
    __ERROR(ENAMETOOLONG,   "Name too long") \
    __ERROR(ELOOP,          "Too many symlinks") \
    __ERROR(EOVERFLOW,      "Overflow") \
    __ERROR(EOPNOTSUPP,     "Operation not supported") \
    __ERROR(ENOSYS,         "No such syscall") \
    __ERROR(ENOTIMPL,       "Not implemented") \
    __ERROR(EAFNOSUPPORT,   "Address family not supported") \
    __ERROR(ENOTSOCK,       "Not a socket") \
    __ERROR(EADDRINUSE,     "Address in use") \
    __ERROR(EWHYTHO,        "Failed without setting an error code (Bug!)") \
    __ERROR(ENOTEMPTY,      "Directory not empty") \
    __ERROR(ECONNREFUSED,   "Connection refused") \


enum __errno_values {
#undef __ERROR
#define __ERROR(a, b) a,
    __ENUMERATE_ALL_ERRORS
#undef __ERROR
    __errno_count
};
