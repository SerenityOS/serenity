/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <dlfcn.h>

[[gnu::destructor]] static void fini()
{
    outln("TestOrderDlClose2.cpp:fini");
}

int main()
{
    outln("===== not closed library destructors =====");
    outln("main:1");
    void* lib2 = dlopen("libTestOrderLib2.so", RTLD_LAZY | RTLD_GLOBAL);
    VERIFY(lib2 != nullptr);
    outln("main:2");
    dlclose(lib2);
    outln("main:3");
    void* lib2_again = dlopen("libTestOrderLib2.so", RTLD_LAZY | RTLD_GLOBAL);
    VERIFY(lib2_again != nullptr);
    outln("main:4");
    auto f = reinterpret_cast<char const* (*)()>(dlsym(lib2_again, "_Z1fv"));
    VERIFY(f != nullptr);
    outln("f() = {}", f());
    outln("main:5");
}
