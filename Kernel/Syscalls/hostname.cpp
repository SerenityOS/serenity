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

namespace Kernel {

extern String* g_hostname;
extern Lock* g_hostname_lock;

int Process::sys$gethostname(Userspace<char*> buffer, ssize_t size)
{
    REQUIRE_PROMISE(stdio);
    if (size < 0)
        return -EINVAL;
    LOCKER(*g_hostname_lock, Lock::Mode::Shared);
    if ((size_t)size < (g_hostname->length() + 1))
        return -ENAMETOOLONG;
    if (!copy_to_user(buffer, g_hostname->characters(), g_hostname->length() + 1))
        return -EFAULT;
    return 0;
}

int Process::sys$sethostname(Userspace<const char*> hostname, ssize_t length)
{
    REQUIRE_NO_PROMISES;
    if (!is_superuser())
        return -EPERM;
    if (length < 0)
        return -EINVAL;
    LOCKER(*g_hostname_lock, Lock::Mode::Exclusive);
    if (length > 64)
        return -ENAMETOOLONG;
    auto copied_hostname = copy_string_from_user(hostname, length);
    if (copied_hostname.is_null())
        return -EFAULT;
    *g_hostname = move(copied_hostname);
    return 0;
}

}
