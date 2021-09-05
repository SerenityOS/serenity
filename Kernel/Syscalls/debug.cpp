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
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    dump_backtrace();
    return 0;
}

KResultOr<FlatPtr> Process::sys$dbgputch(u8 ch)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    dbgputch(ch);
    return 0;
}

KResultOr<FlatPtr> Process::sys$dbgputstr(Userspace<const char*> characters, size_t size)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    if (size == 0)
        return 0;

    if (size <= 1024) {
        char buffer[1024];
        TRY(copy_from_user(buffer, characters, size));
        dbgputstr(buffer, size);
        return size;
    }

    auto result = try_copy_kstring_from_user(characters, size);
    if (result.is_error())
        return result.error();
    auto string = result.release_value();
    dbgputstr(string->view());
    return string->length();
}

}
