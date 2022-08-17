/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$uname(Userspace<utsname*> user_buf)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));

    utsname buf {};
    memcpy(buf.sysname, "SerenityOS", 11);
    memcpy(buf.release, "1.0-dev", 8);
    memcpy(buf.version, "FIXME", 6);
#if ARCH(I386)
    memcpy(buf.machine, "i686", 5);
#elif ARCH(X86_64)
    memcpy(buf.machine, "x86_64", 7);
#elif ARCH(AARCH64)
    memcpy(buf.machine, "AArch64", 7);
#else
#    error Unknown architecture
#endif

    hostname().with_shared([&](auto const& name) {
        auto length = min(name->length(), UTSNAME_ENTRY_LEN - 1);
        memcpy(buf.nodename, name->characters(), length);
        buf.nodename[length] = '\0';
    });

    TRY(copy_to_user(user_buf, &buf));
    return 0;
}

}
