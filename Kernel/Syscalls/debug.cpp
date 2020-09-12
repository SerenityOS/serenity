/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/IO.h>
#include <Kernel/KSyms.h>
#include <Kernel/Process.h>
#include <Kernel/UserOrKernelBuffer.h>

namespace Kernel {

int Process::sys$dump_backtrace()
{
    dump_backtrace();
    return 0;
}

int Process::sys$dbgputch(u8 ch)
{
    IO::out8(0xe9, ch);
    return 0;
}

int Process::sys$dbgputstr(Userspace<const u8*> characters, int length)
{
    if (length <= 0)
        return 0;

    SmapDisabler disabler;
    auto buffer = UserOrKernelBuffer::for_user_buffer(characters, length);
    if (!buffer.has_value())
        return -EFAULT;
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
