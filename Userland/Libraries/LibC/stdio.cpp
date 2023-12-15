/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BuiltinWrappers.h>
#include <AK/ByteString.h>
#include <AK/Format.h>
#include <AK/PrintfImplementation.h>
#include <AK/ScopedValueRollback.h>
#include <AK/StdLibExtras.h>
#include <assert.h>
#include <bits/mutex_locker.h>
#include <bits/stdio_file_implementation.h>
#include <errno.h>
#include <fcntl.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdio_ext.h>
#include <stdlib.h>
#include <string.h>
#include <sys/internals.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <syscall.h>
#include <unistd.h>

extern "C" {

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/setvbuf.html
int setvbuf(FILE* stream, char* buf, int mode, size_t size)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    if (mode != _IONBF && mode != _IOLBF && mode != _IOFBF) {
        errno = EINVAL;
        return -1;
    }
    stream->setbuf(reinterpret_cast<u8*>(buf), mode, size);
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/setbuf.html
void setbuf(FILE* stream, char* buf)
{
    setvbuf(stream, buf, buf ? _IOFBF : _IONBF, BUFSIZ);
}

void setlinebuf(FILE* stream)
{
    setvbuf(stream, nullptr, _IOLBF, 0);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fileno.html
int fileno(FILE* stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    return stream->fileno();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/feof.html
int feof(FILE* stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    return stream->eof();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fflush.html
int fflush(FILE* stream)
{
    if (!stream)
        return FILE::flush_open_streams();
    ScopedFileLock lock(stream);
    return stream->flush() ? 0 : EOF;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fgets.html
char* fgets(char* buffer, int size, FILE* stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    bool ok = stream->gets(reinterpret_cast<u8*>(buffer), size);
    return ok ? buffer : nullptr;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fgetc.html
int fgetc(FILE* stream)
{
    VERIFY(stream);
    unsigned char ch;
    size_t nread = fread(&ch, sizeof(unsigned char), 1, stream);
    if (nread == 1) {
        return ch;
    }
    return EOF;
}

int fgetc_unlocked(FILE* stream)
{
    VERIFY(stream);
    unsigned char ch;
    size_t nread = fread_unlocked(&ch, sizeof(unsigned char), 1, stream);
    if (nread == 1)
        return ch;
    return EOF;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getc.html
int getc(FILE* stream)
{
    return fgetc(stream);
}

int getc_unlocked(FILE* stream)
{
    return fgetc_unlocked(stream);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getchar.html
int getchar()
{
    return getc(stdin);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getdelim.html
ssize_t getdelim(char** lineptr, size_t* n, int delim, FILE* stream)
{
    if (!lineptr || !n) {
        errno = EINVAL;
        return -1;
    }

    if (*lineptr == nullptr || *n == 0) {
        *n = BUFSIZ;
        if ((*lineptr = static_cast<char*>(malloc(*n))) == nullptr) {
            return -1;
        }
    }

    char* ptr;
    char* eptr;
    for (ptr = *lineptr, eptr = *lineptr + *n;;) {
        int c = fgetc(stream);
        if (c == -1) {
            if (feof(stream)) {
                *ptr = '\0';
                return ptr == *lineptr ? -1 : ptr - *lineptr;
            } else {
                return -1;
            }
        }
        *ptr++ = c;
        if (c == delim) {
            *ptr = '\0';
            return ptr - *lineptr;
        }
        if (ptr + 2 >= eptr) {
            char* nbuf;
            size_t nbuf_sz = *n * 2;
            ssize_t d = ptr - *lineptr;
            if ((nbuf = static_cast<char*>(realloc(*lineptr, nbuf_sz))) == nullptr) {
                return -1;
            }
            *lineptr = nbuf;
            *n = nbuf_sz;
            eptr = nbuf + nbuf_sz;
            ptr = nbuf + d;
        }
    }
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getline.html
ssize_t getline(char** lineptr, size_t* n, FILE* stream)
{
    return getdelim(lineptr, n, '\n', stream);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/ungetc.html
int ungetc(int c, FILE* stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    bool ok = stream->ungetc(c);
    return ok ? c : EOF;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/putc.html
int putc(int ch, FILE* stream)
{
    return fputc(ch, stream);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/putchar.html
int putchar(int ch)
{
    return putc(ch, stdout);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/puts.html
int puts(char const* s)
{
    int rc = fputs(s, stdout);
    if (rc == EOF)
        return EOF;
    return fputc('\n', stdout);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/clearerr.html
void clearerr(FILE* stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    stream->clear_err();
}

size_t fread_unlocked(void* ptr, size_t size, size_t nmemb, FILE* stream)
{
    VERIFY(stream);
    VERIFY(!Checked<size_t>::multiplication_would_overflow(size, nmemb));

    size_t nread = stream->read(reinterpret_cast<u8*>(ptr), size * nmemb);
    if (!nread)
        return 0;
    return nread / size;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fread.html
size_t fread(void* ptr, size_t size, size_t nmemb, FILE* stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    return fread_unlocked(ptr, size, nmemb, stream);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fseek.html
int fseek(FILE* stream, long offset, int whence)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    return stream->seek(offset, whence);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fseeko.html
int fseeko(FILE* stream, off_t offset, int whence)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    return stream->seek(offset, whence);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/ftell.html
long ftell(FILE* stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    return stream->tell();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/ftello.html
off_t ftello(FILE* stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    return stream->tell();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fgetpos.html
int fgetpos(FILE* stream, fpos_t* pos)
{
    VERIFY(stream);
    VERIFY(pos);

    ScopedFileLock lock(stream);
    off_t val = stream->tell();
    if (val == -1L)
        return 1;

    *pos = val;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fsetpos.html
int fsetpos(FILE* stream, fpos_t const* pos)
{
    VERIFY(stream);
    VERIFY(pos);

    ScopedFileLock lock(stream);
    return stream->seek(*pos, SEEK_SET);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/rewind.html
void rewind(FILE* stream)
{
    fseek(stream, 0, SEEK_SET);
    clearerr(stream);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/perror.html
void perror(char const* s)
{
    int saved_errno = errno;
    dbgln("perror(): {}: {}", s, strerror(saved_errno));
    warnln("{}: {}", s, strerror(saved_errno));
}

static int parse_mode(char const* mode)
{
    int flags = 0;

    // NOTE: rt is a non-standard mode which opens a file for read, explicitly
    // specifying that it's a text file
    for (auto* ptr = mode; *ptr; ++ptr) {
        switch (*ptr) {
        case 'r':
            flags |= O_RDONLY;
            break;
        case 'w':
            flags |= O_WRONLY | O_CREAT | O_TRUNC;
            break;
        case 'a':
            flags |= O_WRONLY | O_APPEND | O_CREAT;
            break;
        case '+':
            flags |= O_RDWR;
            break;
        case 'e':
            flags |= O_CLOEXEC;
            break;
        case 'b':
            // Ok...
            break;
        case 't':
            // Ok...
            break;
        default:
            dbgln("Potentially unsupported fopen mode _{}_ (because of '{}')", mode, *ptr);
        }
    }

    return flags;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fopen.html
FILE* fopen(char const* pathname, char const* mode)
{
    int flags = parse_mode(mode);
    int fd = open(pathname, flags, 0666);
    if (fd < 0)
        return nullptr;
    return FILE::create(fd, flags);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/freopen.html
FILE* freopen(char const* pathname, char const* mode, FILE* stream)
{
    VERIFY(stream);
    if (!pathname) {
        // FIXME: Someone should probably implement this path.
        TODO();
    }

    int flags = parse_mode(mode);
    int fd = open(pathname, flags, 0666);
    if (fd < 0)
        return nullptr;

    stream->reopen(fd, flags);
    return stream;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fdopen.html
FILE* fdopen(int fd, char const* mode)
{
    int flags = parse_mode(mode);
    // FIXME: Verify that the mode matches how fd is already open.
    if (fd < 0)
        return nullptr;
    return FILE::create(fd, flags);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fmemopen.html
FILE* fmemopen(void*, size_t, char const*)
{
    // FIXME: Implement me :^)
    TODO();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fclose.html
int fclose(FILE* stream)
{
    return FILE::close(stream);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/rename.html
int rename(char const* oldpath, char const* newpath)
{
    return renameat(AT_FDCWD, oldpath, AT_FDCWD, newpath);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/renameat.html
int renameat(int olddirfd, char const* oldpath, int newdirfd, char const* newpath)
{
    if (!oldpath || !newpath) {
        errno = EFAULT;
        return -1;
    }
    Syscall::SC_rename_params params { olddirfd, { oldpath, strlen(oldpath) }, newdirfd, { newpath, strlen(newpath) } };
    int rc = syscall(SC_rename, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

void dbgputstr(char const* characters, size_t length)
{
    syscall(SC_dbgputstr, characters, length);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/tmpnam.html
char* tmpnam(char*)
{
    dbgln("FIXME: Implement tmpnam()");
    TODO();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/popen.html
FILE* popen(char const* command, char const* type)
{
    if (!type || (*type != 'r' && *type != 'w')) {
        errno = EINVAL;
        return nullptr;
    }

    int pipe_fds[2];

    if (pipe(pipe_fds) < 0) {
        ScopedValueRollback rollback(errno);
        perror("pipe");
        return nullptr;
    }

    pid_t child_pid = fork();
    if (child_pid < 0) {
        ScopedValueRollback rollback(errno);
        perror("fork");
        close(pipe_fds[0]);
        close(pipe_fds[1]);
        return nullptr;
    } else if (child_pid == 0) {
        if (*type == 'r') {
            if (dup2(pipe_fds[1], STDOUT_FILENO) < 0) {
                perror("dup2");
                exit(1);
            }
            close(pipe_fds[0]);
            close(pipe_fds[1]);
        } else if (*type == 'w') {
            if (dup2(pipe_fds[0], STDIN_FILENO) < 0) {
                perror("dup2");
                exit(1);
            }
            close(pipe_fds[0]);
            close(pipe_fds[1]);
        }

        if (execl("/bin/sh", "sh", "-c", command, nullptr) < 0)
            perror("execl");
        exit(1);
    }

    FILE* file = nullptr;
    if (*type == 'r') {
        file = FILE::create(pipe_fds[0], O_RDONLY);
        close(pipe_fds[1]);
    } else if (*type == 'w') {
        file = FILE::create(pipe_fds[1], O_WRONLY);
        close(pipe_fds[0]);
    }

    file->set_popen_child(child_pid);
    return file;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pclose.html
int pclose(FILE* stream)
{
    VERIFY(stream);
    VERIFY(stream->popen_child() != 0);

    int wstatus = 0;
    if (waitpid(stream->popen_child(), &wstatus, 0) < 0)
        return -1;

    return wstatus;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/remove.html
int remove(char const* pathname)
{
    if (unlink(pathname) < 0) {
        if (errno == EISDIR)
            return rmdir(pathname);
        return -1;
    }
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/scanf.html
int scanf(char const* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int count = vfscanf(stdin, fmt, ap);
    va_end(ap);
    return count;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fscanf.html
int fscanf(FILE* stream, char const* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int count = vfscanf(stream, fmt, ap);
    va_end(ap);
    return count;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sscanf.html
int sscanf(char const* buffer, char const* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int count = vsscanf(buffer, fmt, ap);
    va_end(ap);
    return count;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/vfscanf.html
int vfscanf(FILE* stream, char const* fmt, va_list ap)
{
    char buffer[BUFSIZ];
    if (!fgets(buffer, sizeof(buffer) - 1, stream))
        return -1;
    return vsscanf(buffer, fmt, ap);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/vscanf.html
int vscanf(char const* fmt, va_list ap)
{
    return vfscanf(stdin, fmt, ap);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/flockfile.html
void flockfile(FILE* filehandle)
{
    VERIFY(filehandle);
    filehandle->lock();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/funlockfile.html
void funlockfile(FILE* filehandle)
{
    VERIFY(filehandle);
    filehandle->unlock();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/tmpfile.html
FILE* tmpfile()
{
    char tmp_path[] = "/tmp/XXXXXX";
    int fd = mkstemp(tmp_path);
    if (fd < 0)
        return nullptr;
    // FIXME: instead of using this hack, implement with O_TMPFILE or similar
    unlink(tmp_path);
    return fdopen(fd, "rw");
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/ctermid.html
char* ctermid(char* s)
{
    static char tty_path[L_ctermid] = "/dev/tty";
    if (s)
        return strcpy(s, tty_path);
    return tty_path;
}

size_t __fpending(FILE* stream)
{
    ScopedFileLock lock(stream);
    return stream->pending();
}

int __freading(FILE* stream)
{
    ScopedFileLock lock(stream);

    if ((stream->mode() & O_RDWR) == O_RDONLY) {
        return 1;
    }

    return (stream->flags() & FILE::Flags::LastRead);
}

int __fwriting(FILE* stream)
{
    ScopedFileLock lock(stream);

    if ((stream->mode() & O_RDWR) == O_WRONLY) {
        return 1;
    }

    return (stream->flags() & FILE::Flags::LastWrite);
}

void __fpurge(FILE* stream)
{
    ScopedFileLock lock(stream);
    stream->purge();
}

size_t __freadahead(FILE* stream)
{
    VERIFY(stream);

    ScopedFileLock lock(stream);

    size_t available_size;
    stream->readptr(available_size);
    return available_size;
}

char const* __freadptr(FILE* stream, size_t* sizep)
{
    VERIFY(stream);
    VERIFY(sizep);

    ScopedFileLock lock(stream);

    size_t available_size;
    u8 const* ptr = stream->readptr(available_size);

    if (available_size == 0)
        return nullptr;

    *sizep = available_size;
    return reinterpret_cast<char const*>(ptr);
}

void __freadptrinc(FILE* stream, size_t increment)
{
    VERIFY(stream);

    ScopedFileLock lock(stream);

    stream->readptr_increase(increment);
}

void __fseterr(FILE* stream)
{
    ScopedFileLock lock(stream);
    stream->set_err();
}
}
