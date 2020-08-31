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
#pragma once

#include <stdarg.h>

__BEGIN_DECLS

__attribute__((noreturn)) void err(int eval, const char *fmt, ...);
__attribute__((noreturn)) void verr(int eval, const char *fmt, va_list args);
__attribute__((noreturn)) void errc(int eval, int code, const char *fmt, ...);
__attribute__((noreturn)) void verrc(int eval, int code, const char *fmt, va_list args);
__attribute__((noreturn)) void errx(int eval, const char *fmt, ...);
__attribute__((noreturn)) void verrx(int eval, const char *fmt, va_list args);
void warn(const char *fmt, ...);
void vwarn(const char *fmt, va_list args);
void warnc(int code, const char *fmt, ...);
void vwarnc(int code, const char *fmt, va_list args);
void warnx(const char *fmt, ...);
void vwarnx(const char *fmt, va_list args);

__END_DECLS
