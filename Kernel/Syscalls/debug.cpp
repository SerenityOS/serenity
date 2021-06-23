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

KResultOr<int> Process::sys$dump_backtrace()
{
    dump_backtrace();
    return 0;
}

KResultOr<int> Process::sys$dbgputch(u8 ch)
{
    dbgputch(ch);
    return 0;
}

KResultOr<size_t> Process::sys$dbgputstr(Userspace<const u8*> characters, size_t size)
{
    if (size == 0)
        return 0;

    auto result = try_copy_kstring_from_user(reinterpret_cast<char const*>(characters.unsafe_userspace_ptr()), size);
    if (result.is_error())
        return result.error();
    dbgputstr(reinterpret_cast<const char*>(result.value()->characters()), size);
    return size;
}

}
