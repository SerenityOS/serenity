/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/KSyms.h>
#include <Kernel/Library/UserOrKernelBuffer.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/kstdio.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$dump_backtrace()
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    dump_backtrace();
    return 0;
}

ErrorOr<FlatPtr> Process::sys$dbgputstr(Userspace<char const*> characters, size_t size)
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

    auto string = TRY(try_copy_kstring_from_user(characters, size));
    dbgputstr(string->view());
    return string->length();
}

}
