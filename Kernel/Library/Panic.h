/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>

namespace Kernel {

[[noreturn]] void __panic(char const* file, unsigned int line, char const* function);

#define PANIC(...)                                        \
    do {                                                  \
        critical_dmesgln("KERNEL PANIC! :^(");            \
        critical_dmesgln(__VA_ARGS__);                    \
        __panic(__FILE__, __LINE__, __PRETTY_FUNCTION__); \
    } while (0)

}
