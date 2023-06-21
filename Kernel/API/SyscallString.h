/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Patrick Meyer <git@the-space.agency>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <Kernel/API/Syscall.h>

namespace Kernel::Syscall {

// Separate header so syscall.h doesn't depend on malloc.
// https://github.com/SerenityOS/serenity/issues/13869
constexpr StringView to_string(Function function)
{
    switch (function) {
#undef __ENUMERATE_SYSCALL
#define __ENUMERATE_SYSCALL(sys_call, needs_lock) \
    case SC_##sys_call:                           \
        return #sys_call##sv;
        ENUMERATE_SYSCALLS(__ENUMERATE_SYSCALL)
#undef __ENUMERATE_SYSCALL
    default:
        break;
    }
    return "Unknown"sv;
}

}
