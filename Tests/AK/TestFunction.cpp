/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>

constexpr int const_call(Function<int(int)> f, int i)
{
    return f(i);
}

constinit int i = const_call([](int i) { return i; }, 4);
