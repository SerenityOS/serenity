/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypedTransfer.h>
#include <Kernel/Process.h>
#include <Kernel/Version.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$uname(Userspace<utsname*> user_buf)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));

    utsname buf
    {
        "SerenityOS",
            {}, // Hostname, filled in below.
            {}, // "Release" (1.0-dev), filled in below.
            {}, // "Revision" (git commit hash), filled in below.
#if ARCH(I386)
            "i686",
#elif ARCH(X86_64)
            "x86_64",
#elif ARCH(AARCH64)
            "AArch64",
#else
#    error Unknown architecture
#endif
    };

    auto version_string = TRY(KString::formatted("{}.{}-dev", SERENITY_MAJOR_REVISION, SERENITY_MINOR_REVISION));
    AK::TypedTransfer<u8>::copy(reinterpret_cast<u8*>(buf.release), version_string->bytes().data(), min(version_string->length(), UTSNAME_ENTRY_LEN - 1));

    AK::TypedTransfer<u8>::copy(reinterpret_cast<u8*>(buf.version), SERENITY_VERSION.bytes().data(), min(SERENITY_VERSION.length(), UTSNAME_ENTRY_LEN - 1));

    hostname().with_shared([&](auto const& name) {
        auto length = min(name->length(), UTSNAME_ENTRY_LEN - 1);
        AK::TypedTransfer<char>::copy(reinterpret_cast<char*>(buf.nodename), name->characters(), length);
        buf.nodename[length] = '\0';
    });

    TRY(copy_to_user(user_buf, &buf));
    return 0;
}

}
