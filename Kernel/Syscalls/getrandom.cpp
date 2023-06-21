/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Library/UserOrKernelBuffer.h>
#include <Kernel/Security/Random.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

// We don't use the flag yet, but we could use it for distinguishing
// random source like Linux, unlike the OpenBSD equivalent. However, if we
// do, we should be able of the caveats that Linux has dealt with.
ErrorOr<FlatPtr> Process::sys$getrandom(Userspace<void*> buffer, size_t buffer_size, [[maybe_unused]] unsigned flags)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    if (buffer_size > NumericLimits<ssize_t>::max())
        return EINVAL;

    auto data_buffer = TRY(UserOrKernelBuffer::for_user_buffer(buffer, buffer_size));

    return TRY(data_buffer.write_buffered<1024>(buffer_size, [&](Bytes bytes) {
        get_good_random_bytes(bytes);
        return bytes.size();
    }));
}

}
