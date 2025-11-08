/*
 * Copyright (c) 2025, Kusekushi <0kusekushi0@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$rfork(RegisterState& regs)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::proc));

    FlatPtr syscall_function = 0;
    FlatPtr arg1 = 0;
    FlatPtr arg2 = 0;
    FlatPtr arg3 = 0;
    FlatPtr arg4 = 0;
    regs.capture_syscall_params(syscall_function, arg1, arg2, arg3, arg4);
    FlatPtr rfork_flags = arg1;

    return do_fork_common(regs, rfork_flags);
}

} // namespace Kernel
