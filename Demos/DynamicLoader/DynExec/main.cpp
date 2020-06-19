/*
 * Copyright (c) 2019-2020, Andrew Kaster <andrewdkaster@gmail.com>
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
#include <Kernel/Syscall.h>
#include <cstdio>
#include <fcntl.h>

extern int g_lib_var1;
extern int g_lib_var2;
__thread int g_tls_lib_var;

const char* g_string = "Hello, World!\n";
//
int libfunc();
// int libfunc_tls();
void local_dbgputstr(const char* str, int len);

int main(int, char**)
{
    g_tls_lib_var += 2;
    printf("hi\n");
    // printf("errno before = %d\n", errno);
    printf("DynExec: addr of errno: %p\n", &errno);
    printf("main: gettid(): %d\n", gettid());
    printf("main: gettid(): %d\n", gettid());
    printf("main: gettid(): %d\n", gettid());
    // printf("DynExec: addr of errno: %p\n", &errno);
    libfunc();
    printf("main: gettid(): %d\n", gettid());
    printf("main: gettid(): %d\n", gettid());
    // printf("errno after = %d\n", errno);
    // local_dbgputstr(g_string, 15);
    // printf("~~~ This should be printed by libc! ~~~\n");
    // dbg() << "This should be printed in the debug console";

    // int sum = 0;
    // sum += libfunc() + g_lib_var1 + g_lib_var2 + g_tls_lib_var;
    // // sum += libfunc() + g_lib_var1 + g_lib_var2;
    // local_dbgputstr("1\n", 2);
    // // sum += libfunc_tls();
    // local_dbgputstr("2\n", 2);
    // // sum += g_tls_lib_var + g_tls_lib_var2;
    // local_dbgputstr("3\n", 2);
    // g_tls_lib_var = 3 + libfunc();
    return g_tls_lib_var;
}
