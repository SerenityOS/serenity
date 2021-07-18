/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/KSyms.h>
#include <Kernel/Process.h>
#include <Kernel/UserOrKernelBuffer.h>
#include <Kernel/kstdio.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$dump_backtrace()
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    dump_backtrace();
    return 0;
}

KResultOr<FlatPtr> Process::sys$dbgputch(u8 ch)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    dbgputch(ch);
    return 0;
}

KResultOr<FlatPtr> Process::sys$dbgputstr(Userspace<const u8*> characters, size_t size)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    if (size == 0)
        return 0;

    if (size <= 1024) {
        char buffer[1024];
        if (!copy_from_user(buffer, characters, size))
            return EFAULT;
        dbgputstr(buffer, size);
        return size;
    }

    auto result = try_copy_kstring_from_user(reinterpret_cast<char const*>(characters.unsafe_userspace_ptr()), size);
    if (result.is_error())
        return result.error();
    dbgputstr(result.value()->characters(), size);
    return size;
}

}
