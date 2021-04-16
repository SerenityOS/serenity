/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Kernel {

[[noreturn]] void __panic(const char* file, unsigned int line, const char* function);

#define PANIC(...)                                        \
    do {                                                  \
        critical_dmesgln("KERNEL PANIC! :^(");            \
        critical_dmesgln(__VA_ARGS__);                    \
        __panic(__FILE__, __LINE__, __PRETTY_FUNCTION__); \
    } while (0)

}
