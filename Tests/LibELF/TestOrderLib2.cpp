/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>

[[gnu::constructor]] static void init()
{
    outln("TestOrderLib2.cpp:init");
}

[[gnu::destructor]] static void fini()
{
    outln("TestOrderLib2.cpp:fini");
}

StringView f();
StringView f()
{
    return "TestOrderLib2.cpp"sv;
}
