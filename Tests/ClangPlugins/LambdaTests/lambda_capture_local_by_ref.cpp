/*
 * Copyright (c) 2024, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// RUN: %clang++ -cc1 -verify %plugin_opts% %s 2>&1

#include <AK/Function.h>

// expected-note@+1 {{Annotate the parameter with NOESCAPE if the lambda will not outlive the function call}}
void take_fn(Function<void()>) { }

void test()
{
    // expected-note@+1 {{Annotate the variable declaration with IGNORE_USE_IN_ESCAPING_LAMBDA if it outlives the lambda}}
    int a = 0;

    // expected-warning@+1 {{Variable with local storage is captured by reference in a lambda that may be asynchronously executed}}
    take_fn([&a] {
        (void)a;
    });
}
