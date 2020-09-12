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
#include <Kernel/SharedBuffer.h>

namespace Kernel {

pid_t Process::sys$getpid()
{
    REQUIRE_PROMISE(stdio);
    return m_pid.value();
}

pid_t Process::sys$getppid()
{
    REQUIRE_PROMISE(stdio);
    return m_ppid.value();
}

int Process::sys$set_process_icon(int icon_id)
{
    REQUIRE_PROMISE(shared_buffer);
    LOCKER(shared_buffers().lock());
    auto it = shared_buffers().resource().find(icon_id);
    if (it == shared_buffers().resource().end())
        return -EINVAL;
    auto& shared_buffer = *(*it).value;
    if (!shared_buffer.is_shared_with(m_pid))
        return -EPERM;
    m_icon_id = icon_id;
    return 0;
}

int Process::sys$get_process_name(Userspace<char*> buffer, size_t buffer_size)
{
    REQUIRE_PROMISE(stdio);
    if (m_name.length() + 1 > buffer_size)
        return -ENAMETOOLONG;

    if (!copy_to_user(buffer, m_name.characters(), m_name.length() + 1))
        return -EFAULT;
    return 0;
}

int Process::sys$set_process_name(Userspace<const char*> user_name, size_t user_name_length)
{
    REQUIRE_PROMISE(proc);
    if (user_name_length > 256)
        return -ENAMETOOLONG;
    auto name = copy_string_from_user(user_name, user_name_length);
    if (name.is_null())
        return -EFAULT;
    m_name = move(name);
    return 0;
}

}
