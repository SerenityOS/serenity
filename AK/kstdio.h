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

#pragma once

#ifdef __serenity__
#    ifdef KERNEL
#        include <Kernel/kstdio.h>
#    else
#        include <AK/Types.h>
#        include <stdarg.h>
extern "C" {
int vdbgprintf(const char* fmt, va_list);
int dbgprintf(const char* fmt, ...);
int dbgputstr(const char*, ssize_t);
int sprintf(char* buf, const char* fmt, ...);
int snprintf(char* buffer, size_t, const char* fmt, ...);
}
#    endif
#else
#    include <stdio.h>
#    define kprintf printf
#    define dbgprintf(...) fprintf(stderr, __VA_ARGS__)
inline int dbgputstr(const char* characters, ssize_t length)
{
    fwrite(characters, 1, length, stderr);
    return 0;
}
#endif
template<size_t N>
inline int dbgputstr(const char (&array)[N])
{
    return ::dbgputstr(array, N);
}
