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

#include <AK/kstdio.h>
#include <AK/String.h>
#include <assert.h>
#include <stdio.h>

char* __static_environ[] = { nullptr }; // We don't get the environment without some libc workarounds..

// FIXME: Because we need to call printf, and we don't have access to the stout file descriptor
//     from the main executable. We need to call __libc_init....
extern "C" void __libc_init();
extern "C" bool __environ_is_malloced;

class Global {
public:
    Global(int i)
        : m_i(i)
    {
        __environ_is_malloced = false;
        environ = __static_environ;
        __libc_init();
    }

    int get_i() const { return m_i; }

private:
    int m_i = 0;
};

// This object exists to call __stdio_init and __malloc_init. Also to show that global vars work
Global g_glob { 5 };

extern "C" {
int global_lib_variable = 1234;

void global_lib_function()
{
    printf("Hello from Dynamic Lib! g_glob::m_i == %d\n", g_glob.get_i());
}

const char* other_lib_function(int my_argument)
{
    dbgprintf("Hello from Dynamic Lib, now from the debug port! g_glob::m_i == %d\n", g_glob.get_i());

    int sum = my_argument + global_lib_variable;

    // FIXME: We can't just return AK::String::format across the lib boundary here.
    //     It will use malloc from our DSO's copy of LibC, and then probably be free'd into
    //     the malloc of the main program which would be what they call 'very crash'.
    //     Feels very Windows :)
    static String s_string;
    s_string = String::format("Here's your string! Sum of argument and global_lib_variable: %d", sum);
    return s_string.characters();
}
}
