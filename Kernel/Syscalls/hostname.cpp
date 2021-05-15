/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

extern String* g_hostname;
extern Lock* g_hostname_lock;

KResultOr<int> Process::sys$gethostname(Userspace<char*> buffer, ssize_t size)
{
    REQUIRE_PROMISE(stdio);
    if (size < 0)
        return EINVAL;
    Locker locker(*g_hostname_lock, Lock::Mode::Shared);
    if ((size_t)size < (g_hostname->length() + 1))
        return ENAMETOOLONG;
    if (!copy_to_user(buffer, g_hostname->characters(), g_hostname->length() + 1))
        return EFAULT;
    return 0;
}

KResultOr<int> Process::sys$sethostname(Userspace<const char*> hostname, ssize_t length)
{
    REQUIRE_NO_PROMISES;
    if (!is_superuser())
        return EPERM;
    if (length < 0)
        return EINVAL;
    Locker locker(*g_hostname_lock, Lock::Mode::Exclusive);
    if (length > 64)
        return ENAMETOOLONG;
    auto copied_hostname = copy_string_from_user(hostname, length);
    if (copied_hostname.is_null())
        return EFAULT;
    *g_hostname = move(copied_hostname);
    return 0;
}

}
