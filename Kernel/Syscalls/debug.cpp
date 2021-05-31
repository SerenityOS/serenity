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
    IO::out8(IO::BOCHS_DEBUG_PORT, ch);
    return 0;
}

KResultOr<size_t> Process::sys$dbgputstr(Userspace<const u8*> characters, int length)
{
    if (length <= 0)
        return 0;

    auto buffer = UserOrKernelBuffer::for_user_buffer(characters, length);
    if (!buffer.has_value())
        return EFAULT;
    return buffer.value().read_buffered<1024>(length, [&](u8 const* buffer, size_t buffer_size) {
        for (size_t i = 0; i < buffer_size; ++i)
            IO::out8(IO::BOCHS_DEBUG_PORT, buffer[i]);
        return buffer_size;
    });
}

}
