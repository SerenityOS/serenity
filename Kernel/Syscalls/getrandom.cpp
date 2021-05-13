/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>
#include <Kernel/Random.h>
#include <Kernel/UserOrKernelBuffer.h>

namespace Kernel {

// We don't use the flag yet, but we could use it for distinguishing
// random source like Linux, unlike the OpenBSD equivalent. However, if we
// do, we should be able of the caveats that Linux has dealt with.
KResultOr<size_t> Process::sys$getrandom(Userspace<void*> buffer, size_t buffer_size, [[maybe_unused]] unsigned flags)
{
    REQUIRE_PROMISE(stdio);
    if (buffer_size <= 0)
        return EINVAL;

    auto data_buffer = UserOrKernelBuffer::for_user_buffer(buffer, buffer_size);
    if (!data_buffer.has_value())
        return EFAULT;
    return data_buffer.value().write_buffered<1024>(buffer_size, [&](u8* buffer, size_t buffer_bytes) {
        get_good_random_bytes(buffer, buffer_bytes);
        return buffer_bytes;
    });
}

}
