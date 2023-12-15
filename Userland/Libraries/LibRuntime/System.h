/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/API/POSIX/sys/types.h>
#include <LibRuntime/StringArgument.h>

namespace Runtime {

struct StackBounds {
    uintptr_t user_stack_base;
    size_t user_stack_size;
};

StackBounds get_stack_bounds();
void dbgputstr(StringArgument const& string);
pid_t gettid();

}
