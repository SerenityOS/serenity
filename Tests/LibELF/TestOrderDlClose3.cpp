/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <dlfcn.h>

[[gnu::destructor]] static void fini()
{
    outln("TestOrderDlClose3.cpp:fini");
}

[[gnu::weak]] char const* f();

int main()
{
    outln("===== symbol dependencies =====");
    outln("main:1");
    void* lib2 = dlopen("libTestOrderLib2.so", RTLD_LAZY | RTLD_GLOBAL);
    VERIFY(lib2 != nullptr);
    outln("main:2");
    outln("f() = {}", f());
    dlclose(lib2);
    outln("main:3");
}
