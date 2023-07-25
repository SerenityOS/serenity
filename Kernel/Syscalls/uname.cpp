/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Tasks/Process.h>
#include <Kernel/Version.h>

namespace Kernel {

#if ARCH(X86_64)
#    define UNAME_MACHINE "x86_64"
#elif ARCH(AARCH64)
#    define UNAME_MACHINE "AArch64"
#else
#    error Unknown architecture
#endif

ErrorOr<FlatPtr> Process::sys$uname(Userspace<utsname*> user_buf)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));

    utsname buf {
        "SerenityOS",
        {}, // Hostname, filled in below.
        {}, // "Release" (1.0-dev), filled in below.
        {}, // "Revision" (git commit hash), filled in below.
        UNAME_MACHINE
    };

    auto version_string = TRY(KString::formatted("{}.{}-dev", SERENITY_MAJOR_REVISION, SERENITY_MINOR_REVISION));
    version_string->view().copy_characters_to_buffer(buf.release, UTSNAME_ENTRY_LEN);
    SERENITY_VERSION.view().copy_characters_to_buffer(buf.version, UTSNAME_ENTRY_LEN);

    hostname().with_shared([&](auto const& name) {
        name->view().copy_characters_to_buffer(buf.nodename, UTSNAME_ENTRY_LEN);
    });

    TRY(copy_to_user(user_buf, &buf));
    return 0;
}

}
