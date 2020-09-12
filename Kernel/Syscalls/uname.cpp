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

int Process::sys$uname(Userspace<utsname*> buf)
{
    extern String* g_hostname;
    extern Lock* g_hostname_lock;

    REQUIRE_PROMISE(stdio);

    LOCKER(*g_hostname_lock, Lock::Mode::Shared);
    if (g_hostname->length() + 1 > sizeof(utsname::nodename))
        return -ENAMETOOLONG;

    // We have already validated the entire utsname struct at this
    // point, there is no need to re-validate every write to the struct.
    utsname* user_buf = buf.unsafe_userspace_ptr();
    if (!copy_to_user(user_buf->sysname, "SerenityOS", 11))
        return -EFAULT;
    if (!copy_to_user(user_buf->release, "1.0-dev", 8))
        return -EFAULT;
    if (!copy_to_user(user_buf->version, "FIXME", 6))
        return -EFAULT;
    if (!copy_to_user(user_buf->machine, "i686", 5))
        return -EFAULT;
    if (!copy_to_user(user_buf->nodename, g_hostname->characters(), g_hostname->length() + 1))
        return -EFAULT;
    return 0;
}

}
