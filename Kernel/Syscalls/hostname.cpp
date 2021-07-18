/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

extern String* g_hostname;
extern Mutex* g_hostname_lock;

KResultOr<FlatPtr> Process::sys$gethostname(Userspace<char*> buffer, size_t size)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this)
    REQUIRE_PROMISE(stdio);
    if (size > NumericLimits<ssize_t>::max())
        return EINVAL;
    MutexLocker locker(*g_hostname_lock, Mutex::Mode::Shared);
    if (size < (g_hostname->length() + 1))
        return ENAMETOOLONG;
    if (!copy_to_user(buffer, g_hostname->characters(), g_hostname->length() + 1))
        return EFAULT;
    return 0;
}

KResultOr<FlatPtr> Process::sys$sethostname(Userspace<const char*> hostname, size_t length)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this)
    REQUIRE_NO_PROMISES;
    if (!is_superuser())
        return EPERM;
    MutexLocker locker(*g_hostname_lock, Mutex::Mode::Exclusive);
    if (length > 64)
        return ENAMETOOLONG;
    auto copied_hostname = copy_string_from_user(hostname, length);
    if (copied_hostname.is_null())
        return EFAULT;
    *g_hostname = move(copied_hostname);
    return 0;
}

}
