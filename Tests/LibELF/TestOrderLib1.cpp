/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>

[[gnu::constructor]] static void init()
{
    outln("TestOrderLib1.cpp:init");
}

StringView f();
StringView f()
{
    return "TestOrderLib1.cpp"sv;
}
