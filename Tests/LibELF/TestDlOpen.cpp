/*
 * Copyright (c) 2021, Rodrigo Tobar <rtobarc@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <dlfcn.h>

TEST_CASE(test_dlopen)
{
    auto liba = dlopen("libDynlibA.so", RTLD_LAZY | RTLD_GLOBAL);
    EXPECT_NE(liba, nullptr);
    auto libb = dlopen("libDynlibB.so", RTLD_LAZY | RTLD_GLOBAL);
    EXPECT_NE(libb, nullptr);

    typedef int (*dynlib_func_t)();
    dynlib_func_t func_a = (dynlib_func_t)dlsym(liba, "dynliba_function");
    EXPECT_NE(func_a, nullptr);
    EXPECT_EQ(0, func_a());

    dynlib_func_t func_b = (dynlib_func_t)dlsym(libb, "dynlibb_function");
    EXPECT_NE(func_b, nullptr);
    EXPECT_EQ(0, func_b());
}

TEST_CASE(test_dlsym_rtld_default)
{
    auto libd = dlopen("libDynlibD.so", RTLD_LAZY | RTLD_GLOBAL);
    EXPECT_NE(libd, nullptr);
    if (libd == nullptr) {
        warnln("can't open libDynlibD.so, {}", dlerror());
        return;
    }

    typedef int (*dynlib_func_t)();
    dynlib_func_t func_c = (dynlib_func_t)dlsym(RTLD_DEFAULT, "dynlibc_function");
    EXPECT_NE(func_c, nullptr);
    EXPECT_EQ(0, func_c());

    dynlib_func_t func_d = (dynlib_func_t)dlsym(RTLD_DEFAULT, "dynlibd_function");
    EXPECT_NE(func_d, nullptr);
    EXPECT_EQ(0, func_d());

    dlclose(libd);
}
