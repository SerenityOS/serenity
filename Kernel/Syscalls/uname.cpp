/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$uname(Userspace<utsname*> user_buf)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    extern String* g_hostname;
    extern Mutex* g_hostname_lock;

    REQUIRE_PROMISE(stdio);

    MutexLocker locker(*g_hostname_lock, Mutex::Mode::Shared);
    if (g_hostname->length() + 1 > sizeof(utsname::nodename))
        return ENAMETOOLONG;

    utsname buf {};
    memcpy(buf.sysname, "SerenityOS", 11);
    memcpy(buf.release, "1.0-dev", 8);
    memcpy(buf.version, "FIXME", 6);
#if ARCH(I386)
    memcpy(buf.machine, "i686", 5);
#else
    memcpy(buf.machine, "x86_64", 7);
#endif

    memcpy(buf.nodename, g_hostname->characters(), g_hostname->length() + 1);

    if (!copy_to_user(user_buf, &buf))
        return EFAULT;
    return 0;
}

}
