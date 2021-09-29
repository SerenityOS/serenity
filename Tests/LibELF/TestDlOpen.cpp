/*
 * Copyright (c) 2021, Rodrigo Tobar <rtobarc@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibDl/dlfcn.h>
#include <LibTest/TestCase.h>

TEST_CASE(test_dlopen)
{
    auto liba = dlopen("/usr/Tests/LibELF/libDynlibA.so", 0);
    EXPECT_NE(liba, nullptr);
    auto libb = dlopen("/usr/Tests/LibELF/libDynlibB.so", 0);
    EXPECT_NE(libb, nullptr);

    typedef int (*dynlib_func_t)();
    dynlib_func_t func_a = (dynlib_func_t)dlsym(liba, "dynliba_function");
    EXPECT_NE(func_a, nullptr);
    EXPECT_EQ(0, func_a());

    dynlib_func_t func_b = (dynlib_func_t)dlsym(libb, "dynlibb_function");
    EXPECT_NE(func_b, nullptr);
    EXPECT_EQ(0, func_b());
}
