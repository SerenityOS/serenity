/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/LogStream.h>
#include <AK/PrintfImplementation.h>
#include <AK/ScopedValueRollback.h>
#include <AK/StdLibExtras.h>
#include <Kernel/Syscall.h>
#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

extern "C" {

static FILE __default_streams[3];
FILE* stdin;
FILE* stdout;
FILE* stderr;

void init_FILE(FILE& fp, int fd, int mode)
{
    fp.fd = fd;
    fp.buffer = fp.default_buffer;
    fp.buffer_size = BUFSIZ;
    fp.mode = mode;
}

static FILE* make_FILE(int fd)
{
    auto* fp = (FILE*)malloc(sizeof(FILE));
    memset(fp, 0, sizeof(FILE));
    init_FILE(*fp, fd, isatty(fd));
    return fp;
}

void __stdio_init()
{
    stdin = &__default_streams[0];
    stdout = &__default_streams[1];
    stderr = &__default_streams[2];
    init_FILE(*stdin, 0, isatty(0) ? _IOLBF : _IOFBF);
    init_FILE(*stdout, 1, isatty(1) ? _IOLBF : _IOFBF);
    init_FILE(*stderr, 2, _IONBF);
}

int setvbuf(FILE* stream, char* buf, int mode, size_t size)
{
    if (mode != _IONBF && mode != _IOLBF && mode != _IOFBF) {
        errno = EINVAL;
        return -1;
    }
    stream->mode = mode;
    if (buf) {
        stream->buffer = buf;
        stream->buffer_size = size;
    } else {
        stream->buffer = stream->default_buffer;
        stream->buffer_size = BUFSIZ;
    }
    stream->buffer_index = 0;
    return 0;
}

void setbuf(FILE* stream, char* buf)
{
    setvbuf(stream, buf, buf ? _IOFBF : _IONBF, BUFSIZ);
}

void setlinebuf(FILE* stream)
{
    setvbuf(stream, nullptr, _IOLBF, 0);
}

int fileno(FILE* stream)
{
    assert(stream);
    return stream->fd;
}

int feof(FILE* stream)
{
    assert(stream);
    return stream->eof;
}

int fflush(FILE* stream)
{
    // FIXME: fflush(NULL) should flush all open output streams.
    ASSERT(stream);
    if (!stream->buffer_index)
        return 0;
    int rc = write(stream->fd, stream->buffer, stream->buffer_index);
    stream->buffer_index = 0;
    stream->error = 0;
    stream->eof = 0;
    stream->have_ungotten = false;
    stream->ungotten = 0;
    if (rc < 0) {
        stream->error = errno;
        return EOF;
    }
    return 0;
}

char* fgets(char* buffer, int size, FILE* stream)
{
    ASSERT(stream);
    ASSERT(size);
    ssize_t nread = 0;
    while (nread < (size - 1)) {
        int ch = fgetc(stream);
        if (ch == EOF)
            break;
        buffer[nread++] = ch;
        if (ch == '\n')
            break;
    }
    if (nread) {
        buffer[nread] = '\0';
        return buffer;
    }
    return nullptr;
}

int fgetc(FILE* stream)
{
    assert(stream);
    char ch;
    size_t nread = fread(&ch, sizeof(char), 1, stream);
    if (nread == 1)
        return ch;
    return EOF;
}

int getc(FILE* stream)
{
    return fgetc(stream);
}

int getc_unlocked(FILE* stream)
{
    return fgetc(stream);
}

int getchar()
{
    return getc(stdin);
}

ssize_t getdelim(char** lineptr, size_t* n, int delim, FILE* stream)
{
    char *ptr, *eptr;
    if (*lineptr == nullptr || *n == 0) {
        *n = BUFSIZ;
        if ((*lineptr = static_cast<char*>(malloc(*n))) == nullptr) {
            return -1;
        }
    }

    for (ptr = *lineptr, eptr = *lineptr + *n;;) {
        int c = fgetc(stream);
        if (c == -1) {
            if (feof(stream)) {
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

ssize_t getline(char** lineptr, size_t* n, FILE* stream)
{
    return getdelim(lineptr, n, '\n', stream);
}

int ungetc(int c, FILE* stream)
{
    ASSERT(stream);
    if (c == EOF)
        return EOF;
    if (stream->have_ungotten)
        return EOF;
    stream->have_ungotten = true;
    stream->ungotten = c;
    stream->eof = false;
    return c;
}

int fputc(int ch, FILE* stream)
{
    assert(stream);
    assert(stream->buffer_index < stream->buffer_size);
    stream->buffer[stream->buffer_index++] = ch;
    if (stream->buffer_index >= stream->buffer_size)
        fflush(stream);
    else if (stream->mode == _IONBF || (stream->mode == _IOLBF && ch == '\n'))
        fflush(stream);
    if (stream->eof || stream->error)
        return EOF;
    return (u8)ch;
}

int putc(int ch, FILE* stream)
{
    return fputc(ch, stream);
}

int putchar(int ch)
{
    return putc(ch, stdout);
}

int fputs(const char* s, FILE* stream)
{
    for (; *s; ++s) {
        int rc = putc(*s, stream);
        if (rc == EOF)
            return EOF;
    }
    return 1;
}

int puts(const char* s)
{
    int rc = fputs(s, stdout);
    if (rc == EOF)
        return EOF;
    return fputc('\n', stdout);
}

void clearerr(FILE* stream)
{
    assert(stream);
    stream->eof = false;
    stream->error = 0;
}

int ferror(FILE* stream)
{
    return stream->error;
}

size_t fread(void* ptr, size_t size, size_t nmemb, FILE* stream)
{
    assert(stream);
    if (!size)
        return 0;

    ssize_t nread = 0;

    if (stream->have_ungotten) {
        // FIXME: Support ungotten character even if size != 1.
        ASSERT(size == 1);
        ((char*)ptr)[0] = stream->ungotten;
        stream->have_ungotten = false;
        --nmemb;
        if (!nmemb)
            return 1;
        ptr = &((char*)ptr)[1];
        ++nread;
    }

    ssize_t rc = read(stream->fd, ptr, nmemb * size);
    if (rc < 0) {
        stream->error = errno;
        return 0;
    }
    if (rc == 0)
        stream->eof = true;
    nread += rc;
    return nread / size;
}

size_t fwrite(const void* ptr, size_t size, size_t nmemb, FILE* stream)
{
    assert(stream);
    auto* bytes = (const u8*)ptr;
    ssize_t nwritten = 0;
    for (size_t i = 0; i < (size * nmemb); ++i) {
        int rc = fputc(bytes[i], stream);
        if (rc == EOF)
            break;
        ++nwritten;
    }
    return nwritten / size;
}

int fseek(FILE* stream, long offset, int whence)
{
    assert(stream);
    fflush(stream);
    off_t off = lseek(stream->fd, offset, whence);
    if (off < 0)
        return off;
    stream->eof = false;
    stream->error = 0;
    stream->have_ungotten = false;
    stream->ungotten = 0;
    return 0;
}

long ftell(FILE* stream)
{
    assert(stream);
    fflush(stream);
    return lseek(stream->fd, 0, SEEK_CUR);
}

int fgetpos(FILE* stream, fpos_t* pos)
{
    assert(stream);
    assert(pos);

    long val = ftell(stream);
    if (val == -1L)
        return 1;

    *pos = val;
    return 0;
}

int fsetpos(FILE* stream, const fpos_t* pos)
{
    assert(stream);
    assert(pos);
    return fseek(stream, (long) *pos, SEEK_SET);
}

void rewind(FILE* stream)
{
    ASSERT(stream);
    int rc = fseek(stream, 0, SEEK_SET);
    ASSERT(rc == 0);
}

int dbgprintf(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = printf_internal([](char*&, char ch) { dbgputch(ch); }, nullptr, fmt, ap);
    va_end(ap);
    return ret;
}

static void stdout_putch(char*&, char ch)
{
    putchar(ch);
}

static FILE* __current_stream = nullptr;
static void stream_putch(char*&, char ch)
{
    fputc(ch, __current_stream);
}

int vfprintf(FILE* stream, const char* fmt, va_list ap)
{
    __current_stream = stream;
    return printf_internal(stream_putch, nullptr, fmt, ap);
}

int fprintf(FILE* stream, const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = vfprintf(stream, fmt, ap);
    va_end(ap);
    return ret;
}

int vprintf(const char* fmt, va_list ap)
{
    return printf_internal(stdout_putch, nullptr, fmt, ap);
}

int printf(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = vprintf(fmt, ap);
    va_end(ap);
    return ret;
}

static void buffer_putch(char*& bufptr, char ch)
{
    *bufptr++ = ch;
}

int vsprintf(char* buffer, const char* fmt, va_list ap)
{
    int ret = printf_internal(buffer_putch, buffer, fmt, ap);
    buffer[ret] = '\0';
    return ret;
}

int sprintf(char* buffer, const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = vsprintf(buffer, fmt, ap);
    va_end(ap);
    return ret;
}

static size_t __vsnprintf_space_remaining;
static void sized_buffer_putch(char*& bufptr, char ch)
{
    if (__vsnprintf_space_remaining) {
        *bufptr++ = ch;
        --__vsnprintf_space_remaining;
    }
}

int vsnprintf(char* buffer, size_t size, const char* fmt, va_list ap)
{
    __vsnprintf_space_remaining = size;
    int ret = printf_internal(sized_buffer_putch, buffer, fmt, ap);
    if (__vsnprintf_space_remaining) {
        buffer[ret] = '\0';
    }
    return ret;
}

int snprintf(char* buffer, size_t size, const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = vsnprintf(buffer, size, fmt, ap);
    va_end(ap);
    return ret;
}

void perror(const char* s)
{
    int saved_errno = errno;
    dbg() << "perror(): " << s << ": " << strerror(saved_errno);
    fprintf(stderr, "%s: %s\n", s, strerror(saved_errno));
}

FILE* fopen(const char* pathname, const char* mode)
{
    int flags = 0;
    // NOTE: rt is a non-standard mode which opens a file for read, explicitly
    // specifying that it's a text file
    if (!strcmp(mode, "r") || !strcmp(mode, "rb") || !strcmp(mode, "rt"))
        flags = O_RDONLY;
    else if (!strcmp(mode, "r+") || !strcmp(mode, "rb+"))
        flags = O_RDWR;
    else if (!strcmp(mode, "w") || !strcmp(mode, "wb"))
        flags = O_WRONLY | O_CREAT | O_TRUNC;
    else if (!strcmp(mode, "w+") || !strcmp(mode, "wb+"))
        flags = O_RDWR | O_CREAT | O_TRUNC;
    else if (!strcmp(mode, "a") || !strcmp(mode, "ab"))
        flags = O_WRONLY | O_APPEND | O_CREAT;
    else if (!strcmp(mode, "a+") || !strcmp(mode, "ab+"))
        flags = O_RDWR | O_APPEND | O_CREAT;
    else {
        fprintf(stderr, "FIXME(LibC): fopen('%s', '%s')\n", pathname, mode);
        ASSERT_NOT_REACHED();
    }
    int fd = open(pathname, flags, 0666);
    if (fd < 0)
        return nullptr;
    return make_FILE(fd);
}

FILE* freopen(const char* pathname, const char* mode, FILE* stream)
{
    (void)pathname;
    (void)mode;
    (void)stream;
    ASSERT_NOT_REACHED();
}

FILE* fdopen(int fd, const char* mode)
{
    UNUSED_PARAM(mode);
    // FIXME: Verify that the mode matches how fd is already open.
    if (fd < 0)
        return nullptr;
    return make_FILE(fd);
}

int fclose(FILE* stream)
{
    fflush(stream);
    int rc = close(stream->fd);
    if (stream != &__default_streams[0] && stream != &__default_streams[1] && stream != &__default_streams[2])
        free(stream);
    return rc;
}

int rename(const char* oldpath, const char* newpath)
{
    if (!oldpath || !newpath) {
        errno = EFAULT;
        return -1;
    }
    Syscall::SC_rename_params params { { oldpath, strlen(oldpath) }, { newpath, strlen(newpath) } };
    int rc = syscall(SC_rename, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

void dbgputch(char ch)
{
    syscall(SC_dbgputch, ch);
}

int dbgputstr(const char* characters, int length)
{
    int rc = syscall(SC_dbgputstr, characters, length);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

char* tmpnam(char*)
{
    ASSERT_NOT_REACHED();
}

FILE* popen(const char* command, const char* type)
{
    if (!type || (*type != 'r' && *type != 'w')) {
        errno = EINVAL;
        return nullptr;
    }

    int pipe_fds[2];

    int rc = pipe(pipe_fds);
    if (rc < 0) {
        ScopedValueRollback rollback(errno);
        perror("pipe");
        return nullptr;
    }

    pid_t child_pid = fork();
    if (!child_pid) {
        if (*type == 'r') {
            int rc = dup2(pipe_fds[1], STDOUT_FILENO);
            if (rc < 0) {
                perror("dup2");
                exit(1);
            }
            close(pipe_fds[0]);
            close(pipe_fds[1]);
        } else if (*type == 'w') {
            int rc = dup2(pipe_fds[0], STDIN_FILENO);
            if (rc < 0) {
                perror("dup2");
                exit(1);
            }
            close(pipe_fds[0]);
            close(pipe_fds[1]);
        }

        int rc = execl("/bin/sh", "sh", "-c", command, nullptr);
        if (rc < 0)
            perror("execl");
        exit(1);
    }

    FILE* fp = nullptr;
    if (*type == 'r') {
        fp = make_FILE(pipe_fds[0]);
        close(pipe_fds[1]);
    } else if (*type == 'w') {
        fp = make_FILE(pipe_fds[1]);
        close(pipe_fds[0]);
    }

    fp->popen_child = child_pid;
    return fp;
}

int pclose(FILE* fp)
{
    ASSERT(fp);
    ASSERT(fp->popen_child != 0);

    int wstatus = 0;
    int rc = waitpid(fp->popen_child, &wstatus, 0);
    if (rc < 0)
        return rc;

    return wstatus;
}

int remove(const char* pathname)
{
    int rc = unlink(pathname);
    if (rc < 0 && errno != EISDIR)
        return -1;
    return rmdir(pathname);
}

int scanf(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int count = vfscanf(stdin, fmt, ap);
    va_end(ap);
    return count;
}

int fscanf(FILE* stream, const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int count = vfscanf(stream, fmt, ap);
    va_end(ap);
    return count;
}

int sscanf(const char* buffer, const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int count = vsscanf(buffer, fmt, ap);
    va_end(ap);
    return count;
}

int vfscanf(FILE* stream, const char* fmt, va_list ap)
{
    char buffer[BUFSIZ];
    if (!fgets(buffer, sizeof(buffer) - 1, stream))
        return -1;
    return vsscanf(buffer, fmt, ap);
}

void flockfile(FILE* filehandle)
{
    (void)filehandle;
    dbgprintf("FIXME: Implement flockfile()\n");
}

void funlockfile(FILE* filehandle)
{
    (void)filehandle;
    dbgprintf("FIXME: Implement funlockfile()\n");
}

FILE* tmpfile()
{
    char tmp_path[] = "/tmp/XXXXXX";
    if (__generate_unique_filename(tmp_path) < 0)
        return nullptr;

    int fd = open(tmp_path, O_CREAT | O_EXCL | O_RDWR, S_IWUSR | S_IRUSR);
    if (fd < 0)
        return nullptr;

    // FIXME: instead of using this hack, implement with O_TMPFILE or similar
    unlink(tmp_path);

    return make_FILE(fd);
}
}
