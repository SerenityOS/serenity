/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <stdio.h>

[[gnu::constructor]] static void init()
{
    printf("TestOrderLib1.cpp:init\n");
    fflush(stdout);
}

[[gnu::destructor]] static void fini()
{
    printf("TestOrderLib1.cpp:fini\n");
    fflush(stdout);
}

char const* f();
char const* f()
{
    return "TestOrderLib1.cpp";
}
