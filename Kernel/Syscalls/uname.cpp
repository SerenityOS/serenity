/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

KResultOr<int> Process::sys$uname(Userspace<utsname*> user_buf)
{
    extern String* g_hostname;
    extern Lock* g_hostname_lock;

    REQUIRE_PROMISE(stdio);

    Locker locker(*g_hostname_lock, Lock::Mode::Shared);
    if (g_hostname->length() + 1 > sizeof(utsname::nodename))
        return ENAMETOOLONG;

    utsname buf {};
    memcpy(buf.sysname, "SerenityOS", 11);
    memcpy(buf.release, "1.0-dev", 8);
    memcpy(buf.version, "FIXME", 6);
    memcpy(buf.machine, "i686", 5);
    memcpy(buf.nodename, g_hostname->characters(), g_hostname->length() + 1);

    if (!copy_to_user(user_buf, &buf))
        return EFAULT;
    return 0;
}

}
