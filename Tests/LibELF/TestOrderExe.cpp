/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>

[[gnu::constructor]] static void init()
{
    outln("TestOrderExe.cpp:init");
}

[[gnu::destructor]] static void fini()
{
    outln("TestOrderExe.cpp:fini");
}

StringView f();

int main()
{
    outln("TestOrderExe.cpp:main");
    outln("f() returns: {}", f());
}
