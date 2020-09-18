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

#include <Kernel/Process.h>
#include <Kernel/Random.h>
#include <Kernel/UserOrKernelBuffer.h>

namespace Kernel {

// We don't use the flag yet, but we could use it for distinguishing
// random source like Linux, unlike the OpenBSD equivalent. However, if we
// do, we should be able of the caveats that Linux has dealt with.
ssize_t Process::sys$getrandom(Userspace<void*> buffer, size_t buffer_size, [[maybe_unused]] unsigned flags)
{
    REQUIRE_PROMISE(stdio);
    if (buffer_size <= 0)
        return -EINVAL;

    SmapDisabler disabler;
    auto data_buffer = UserOrKernelBuffer::for_user_buffer(buffer, buffer_size);
    if (!data_buffer.has_value())
        return -EFAULT;
    ssize_t nwritten = data_buffer.value().write_buffered<1024>(buffer_size, [&](u8* buffer, size_t buffer_bytes) {
        get_good_random_bytes(buffer, buffer_bytes);
        return (ssize_t)buffer_bytes;
    });
    return nwritten;
}

}
