#include "string.h"
#include "errno.h"
#include "stdio.h"

extern "C" {

size_t strlen(const char* str)
{
    size_t len = 0;
    while (*(str++))
        ++len;
    return len;
}

const char* strerror(int errnum)
{
    switch (errnum) {
    case 0: return "No error";
    case EPERM: return "Operation not permitted";
    case ENOENT: return "No such file or directory";
    case ESRCH: return "No such process";
    case EINTR: return "Interrupted syscall";
    case EIO: return "I/O error";
    case ENXIO: return "No such device/address";
    case E2BIG: return "Argument list too long";
    case ENOEXEC: return "Exec format error";
    case EBADF: return "Bad fd number";
    case ECHILD: return "No child processes";
    case EAGAIN: return "Try again";
    case ENOMEM: return "Out of memory";
    case EACCES: return "Access denied";
    case EFAULT: return "Bad address";
    case ENOTBLK: return "Not a block device";
    case EBUSY: return "Resource busy";
    case EEXIST: return "File already exists";
    case EXDEV: return "Cross-device link";
    case ENODEV: return "No such device";
    case ENOTDIR: return "Not a directory";
    case EISDIR: return "Is a directory";
    case EINVAL: return "Invalid argument";
    case ENFILE: return "File table overflow";
    case EMFILE: return "Too many open files";
    case ENOTTY: return "Not a TTY";
    case ETXTBSY: return "Text file busy";
    case EFBIG: return "File too big";
    case ENOSPC: return "No space left";
    case ESPIPE: return "Illegal seek";
    case EROFS: return "File system is read-only";
    case EMLINK: return "Too many links";
    case EPIPE: return "Broken pipe";
    case EDOM: return "Math argument out of domain";
    case ERANGE: return "Math result not representable";
    case EOVERFLOW: return "Value too large for data type";
    }
    printf("strerror() missing string for errnum=%d\n", errnum);
    return "Unknown error";
}

}

