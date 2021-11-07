/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$uname(Userspace<utsname*> user_buf)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this)
    REQUIRE_PROMISE(stdio);

    utsname buf {};
    memcpy(buf.sysname, "SerenityOS", 11);
    memcpy(buf.release, "1.0-dev", 8);
    memcpy(buf.version, "FIXME", 6);
#if ARCH(I386)
    memcpy(buf.machine, "i686", 5);
#else
    memcpy(buf.machine, "x86_64", 7);
#endif

    hostname().with_shared([&](const auto& name) {
        memcpy(buf.nodename, name.characters(), name.length() + 1);
    });

    TRY(copy_to_user(user_buf, &buf));
    return 0;
}

}
