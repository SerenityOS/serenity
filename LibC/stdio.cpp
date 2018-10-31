#include <stdio.h>
#include <stdarg.h>
#include <sys/types.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <assert.h>
#include <stdlib.h>
#include <Kernel/Syscall.h>
#include <AK/printf.cpp>

extern "C" {

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

char* fgets(char* buffer, int size, FILE* stream)
{
    assert(stream);
    ssize_t nread = 0;
    for (;;) {
        if (nread >= size)
            break;
        char ch = fgetc(stream);
        if (feof(stream))
            break;
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
    fread(&ch, sizeof(char), 1, stream);
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

int fputc(int ch, FILE* stream)
{
    assert(stream);
    write(stream->fd, &ch, 1);
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

void clearerr(FILE* stream)
{
    assert(stream);
    stream->eof = false;
}

size_t fread(void* ptr, size_t size, size_t nmemb, FILE* stream)
{
    assert(stream);
    ssize_t nread = read(stream->fd, ptr, nmemb * size);
    if (nread < 0)
        return 0;
    if (nread == 0)
        stream->eof = true;
    return nread;
}

size_t fwrite(const void* ptr, size_t size, size_t nmemb, FILE* stream)
{
    assert(stream);
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

static void sys_putch(char*&, char ch)
{
    putchar(ch);
}

static FILE* __current_stream = nullptr;
static void stream_putch(char*&, char ch)
{
    write(__current_stream->fd, &ch, 1);
}

int fprintf(FILE* fp, const char* fmt, ...)
{
    __current_stream = fp;
    va_list ap;
    va_start(ap, fmt);
    int ret = printfInternal(stream_putch, nullptr, fmt, ap);
    va_end(ap);
    return ret;
}

int printf(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = printfInternal(sys_putch, nullptr, fmt, ap);
    va_end(ap);
    return ret;
}

static void buffer_putch(char*& bufptr, char ch)
{
    *bufptr++ = ch;
}

int sprintf(char* buffer, const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = printfInternal(buffer_putch, buffer, fmt, ap);
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
    assert(!strcmp(mode, "r") || !strcmp(mode, "rb"));
    int fd = open(pathname, O_RDONLY);
    if (fd < 0)
        return nullptr;
    auto* fp = (FILE*)malloc(sizeof(FILE));
    fp->fd = fd;
    fp->eof = false;
    return fp;
}

int fclose(FILE* stream)
{
    int rc = close(stream->fd);
    free(stream);
    return rc;
}

}

