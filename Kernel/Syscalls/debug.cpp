/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/IO.h>
#include <Kernel/KSyms.h>
#include <Kernel/Process.h>
#include <Kernel/UserOrKernelBuffer.h>

namespace Kernel {

KResultOr<int> Process::sys$dump_backtrace()
{
    dump_backtrace();
    return 0;
}

KResultOr<int> Process::sys$dbgputch(u8 ch)
{
    IO::out8(0xe9, ch);
    return 0;
}

KResultOr<int> Process::sys$dbgputstr(Userspace<const u8*> characters, int length)
{
    if (length <= 0)
        return 0;

    auto buffer = UserOrKernelBuffer::for_user_buffer(characters, length);
    if (!buffer.has_value())
        return EFAULT;
    ssize_t nread = buffer.value().read_buffered<1024>(length, [&](const u8* buffer, size_t buffer_size) {
        for (size_t i = 0; i < buffer_size; ++i)
            IO::out8(0xe9, buffer[i]);
        return (ssize_t)buffer_size;
    });
    if (nread < 0)
        return (int)nread;
    return 0;
}

}
