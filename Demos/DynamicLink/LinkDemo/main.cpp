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

#include <AK/String.h>
#include <LibELF/AuxiliaryVector.h>

#include <dlfcn.h>
#include <stdio.h>

int main(int argc, char** argv, char** envp)
{
    for (int i = 0; i < argc; ++i)
        outln("argv[{}]: {}", i, argv[i]);

    char** env;
    for (env = envp; *env; ++env)
        outln("env: {}", *env);

    for (auxv_t* auxvp = (auxv_t*)++env; auxvp->a_type != AT_NULL; ++auxvp) {
        outln("AuxVal: Type={}, Val/Ptr={}", auxvp->a_type, auxvp->a_un.a_ptr);
        if (auxvp->a_type == AT_PLATFORM) {
            outln("    Platform: {}", (char*)auxvp->a_un.a_ptr);
        } else if (auxvp->a_type == AT_EXECFN) {
            outln("    Filename: {}", (char*)auxvp->a_un.a_ptr);
        } else if (auxvp->a_type == AT_RANDOM) {
            auto byte_ptr = (uint8_t*)auxvp->a_un.a_ptr;
            outln("    My Random bytes are: ");
            for (size_t i = 0; i < 16; ++i)
                new_out("{:#02x} ", byte_ptr[i]);
            outln();
        }
    }

    void* handle = dlopen("/usr/lib/libDynamicLib.so", RTLD_LAZY | RTLD_GLOBAL);

    if (!handle) {
        warnln("Failed to dlopen! {}", dlerror());
        return 1;
    }

    // Test getting an external variable from the library and read it out
    int* ptr_global = (int*)dlsym(handle, "global_lib_variable");

    if (!ptr_global) {
        warnln("Failed to dlsym for \"global_lib_variable\"! {}", dlerror());
        return 2;
    }

    outln("Found global lib variable address: {}", ptr_global);

    outln("Global lib variable is {}", *ptr_global);

    // Test getting a method from the library and calling it
    void (*lib_func)(void) = (void (*)(void))dlsym(handle, "global_lib_function");

    outln("Found global lib function address: {}", lib_func);

    if (!lib_func) {
        warnln("Failed to dlsym for \"global_lib_function\"! {}", dlerror());
        return 2;
    }

    lib_func();

    outln("I think I called my lib function!");

    // Test getting a method that takes and returns arguments now
    const char* (*other_func)(int) = (const char* (*)(int))dlsym(handle, "other_lib_function");

    outln("Found other lib function address {}", other_func);

    if (!other_func) {
        warnln("Failed to dlsym for \"other_lib_function\"! {}", dlerror());
        return 2;
    }

    // Call it twice with different arguments
    String formatted_result = other_func(10);

    outln("({} + {} = {}) {}", *ptr_global, 10, *ptr_global + 10, formatted_result);

    *ptr_global = 17;

    formatted_result = other_func(5);

    outln("({} + {} = {}) {}", *ptr_global, 5, *ptr_global + 5, formatted_result);

    int ret = dlclose(handle);

    if (ret < 0) {
        warnln("Failed to dlclose! {}", dlerror());
        return 3;
    }

    outln("Bye for now!");

    return 0;
}
