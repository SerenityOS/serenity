#include <stdio.h>
#include <stdarg.h>
#include <sys/types.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <Kernel/Syscall.h>
#include <AK/printf.cpp>

extern "C" {

int putchar(int ch)
{
    write(0, &ch, 1);
    return (byte)ch;
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
    printf("%s: %s\n", s, strerror(errno));
}

}

