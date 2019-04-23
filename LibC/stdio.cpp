#include <stdio.h>
#include <stdarg.h>
#include <sys/types.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <assert.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <AK/printf.cpp>
#include <AK/StdLibExtras.h>
#include <Kernel/Syscall.h>

extern "C" {

static FILE __default_streams[4];
FILE* stdin;
FILE* stdout;
FILE* stderr;
FILE* stddbg;

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
    stddbg = &__default_streams[3];
    init_FILE(*stdin, 0, isatty(0) ? _IOLBF : _IOFBF);
    init_FILE(*stdout, 1, isatty(1) ? _IOLBF : _IOFBF);
    init_FILE(*stderr, 2, _IONBF);
    int fd = open("/dev/debuglog", O_WRONLY | O_CLOEXEC);
    if (fd < 0) {
        perror("open /dev/debuglog");
        ASSERT_NOT_REACHED();
    }
    init_FILE(*stddbg, fd, _IOLBF);
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
    // FIXME: Implement buffered streams, duh.
    if (!stream)
        return -EBADF;
    if (!stream->buffer_index)
        return 0;
    int rc = write(stream->fd, stream->buffer, stream->buffer_index);
    stream->buffer_index = 0;
    return rc;
}

char* fgets(char* buffer, int size, FILE* stream)
{
    assert(stream);
    ssize_t nread = 0;
    for (;;) {
        if (nread >= size)
            break;
        int ch = fgetc(stream);
        if (ch == EOF) {
            if (nread == 0)
                return nullptr;
            break;
        }
        buffer[nread++] = ch;
        if (!ch || ch == '\n')
            break;
    }
    if (nread < size)
        buffer[nread] = '\0';
    return buffer;
}

int fgetc(FILE* stream)
{
    assert(stream);
    char ch;
    size_t nread = fread(&ch, sizeof(char), 1, stream);
    if (nread <= 0) {
        stream->eof = nread == 0;
        stream->error = -nread;
        return EOF;
    }
    return ch;
}

int getc(FILE* stream)
{
    return fgetc(stream);
}

int getchar()
{
    return getc(stdin);
}

int ungetc(int c, FILE* stream)
{
    ASSERT(stream);
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
    if (stream->eof)
        return EOF;
    return (byte)ch;
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
    return 0;
}

int puts(const char* s)
{
    int rc = fputs(s, stdout);
    if (rc < 0)
        return rc;
    return fputc('\n', stdout);
}

void clearerr(FILE* stream)
{
    assert(stream);
    stream->eof = false;
    stream->error = false;
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
    if (rc < 0)
        return 0;
    if (rc == 0)
        stream->eof = true;
    nread += rc;
    return nread / size;
}

size_t fwrite(const void* ptr, size_t size, size_t nmemb, FILE* stream)
{
    assert(stream);
    fflush(stream);
    ssize_t nwritten = write(stream->fd, ptr, nmemb * size);
    if (nwritten < 0)
        return 0;
    return nwritten;
}

int fseek(FILE* stream, long offset, int whence)
{
    assert(stream);
    off_t off = lseek(stream->fd, offset, whence);
    if (off < 0)
        return off;
    return 0;
}

long ftell(FILE* stream)
{
    assert(stream);
    return lseek(stream->fd, 0, SEEK_CUR);
}

void rewind(FILE* stream)
{
    fseek(stream, 0, SEEK_SET);
}

int dbgprintf(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = vfprintf(stddbg, fmt, ap);
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
    buffer[ret] = '\0';
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
    buffer[ret] = '\0';
    return ret;
}

int snprintf(char* buffer, size_t size, const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = vsnprintf(buffer, size, fmt, ap);
    buffer[ret] = '\0';
    va_end(ap);
    return ret;
}

void perror(const char* s)
{
    fprintf(stderr, "%s: %s\n", s, strerror(errno));
}

FILE* fopen(const char* pathname, const char* mode)
{
    int flags = 0;
    if (!strcmp(mode, "r") || !strcmp(mode, "rb"))
        flags = O_RDONLY;
    else if (!strcmp(mode, "r+") || !strcmp(mode, "rb+"))
        flags = O_RDWR;
    else if (!strcmp(mode, "w") || !strcmp(mode, "wb"))
        flags = O_WRONLY | O_CREAT | O_TRUNC;
    else if (!strcmp(mode, "w+") || !strcmp(mode, "wb+"))
        flags = O_RDWR | O_CREAT | O_TRUNC;
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
    free(stream);
    return rc;
}

int rename(const char* oldpath, const char* newpath)
{
    int rc = syscall(SC_rename, oldpath, newpath);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

char* tmpnam(char*)
{
    ASSERT_NOT_REACHED();
}

FILE* popen(const char* command, const char* type)
{
    (void)command;
    (void)type;
    ASSERT_NOT_REACHED();
}

int pclose(FILE*)
{
    ASSERT_NOT_REACHED();
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


}

