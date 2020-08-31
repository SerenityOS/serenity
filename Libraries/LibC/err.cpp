/*
 * Copyright (c) 2020, The SerenityOS developers.
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
#include <err.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static void __internal_warn(int code, const char *fmt, va_list args)
{
    int save_errno = errno;

    (void)fprintf(stderr, "%s: ", getprogname());
    if (fmt != nullptr) {
        (void)vfprintf(stderr, fmt, args);
    }
    if (code != -1) {
        (void)fprintf(stderr, ": %s", strerror(code));
    }
    fputc('\n', stderr);

    errno = save_errno;
}

extern "C" {

void err(int eval, const char *fmt, ...)
{
    va_list args;
    va_start(args, fmt);
    __internal_warn(errno, fmt, args);
    va_end(args);

    exit(eval);
}

void verr(int eval, const char *fmt, va_list args)
{
    __internal_warn(errno, fmt, args);

    exit(eval);
}

void errc(int eval, int code, const char *fmt, ...)
{
    va_list args;
    va_start(args, fmt);
    __internal_warn(code, fmt, args);
    va_end(args);

    exit(eval);
}

void verrc(int eval, int code, const char *fmt, va_list args)
{
    __internal_warn(code, fmt, args);

    exit(eval);
}

void errx(int eval, const char *fmt, ...)
{
    va_list args;
    va_start(args, fmt);
    __internal_warn(-1, fmt, args);
    va_end(args);

    exit(eval);
}

void verrx(int eval, const char *fmt, va_list args)
{
    __internal_warn(-1, fmt, args);

    exit(eval);
}

void warn(const char *fmt, ...)
{
    va_list args;
    va_start(args, fmt);
    __internal_warn(errno, fmt, args);
    va_end(args);
}

void vwarn(const char *fmt, va_list args)
{
    __internal_warn(errno, fmt, args);
}

void warnc(int code, const char *fmt, ...)
{
    va_list args;
    va_start(args, fmt);
    __internal_warn(code, fmt, args);
    va_end(args);
}

void vwarnc(int code, const char *fmt, va_list args)
{
    __internal_warn(code, fmt, args);
}

void warnx(const char *fmt, ...)
{
    va_list args;
    va_start(args, fmt);
    __internal_warn(-1, fmt, args);
    va_end(args);
}

void vwarnx(const char *fmt, va_list args)
{
    __internal_warn(-1, fmt, args);
}

}
