/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypedTransfer.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Version.h>

namespace Kernel {

#if ARCH(X86_64)
#    define UNAME_MACHINE "x86_64"
#elif ARCH(AARCH64)
#    define UNAME_MACHINE "AArch64"
#elif ARCH(RISCV64)
#    define UNAME_MACHINE "riscv64"
#else
#    error Unknown architecture
#endif

KString* g_version_string { nullptr };

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

    VERIFY(g_version_string);
    AK::TypedTransfer<u8>::copy(reinterpret_cast<u8*>(buf.release), g_version_string->bytes().data(), min(g_version_string->length(), UTSNAME_ENTRY_LEN - 1));

    AK::TypedTransfer<u8>::copy(reinterpret_cast<u8*>(buf.version), SERENITY_VERSION.bytes().data(), min(SERENITY_VERSION.length(), UTSNAME_ENTRY_LEN - 1));

    m_attached_hostname_context.with([&](auto const& hostname_context_ptr) {
        hostname_context_ptr->buffer().with([&](auto& name_buffer) {
            auto name_length = name_buffer.representable_view().length();
            VERIFY(name_length <= (UTSNAME_ENTRY_LEN - 1));
            AK::TypedTransfer<char>::copy(reinterpret_cast<char*>(buf.nodename), name_buffer.representable_view().characters_without_null_termination(), name_length);
            buf.nodename[name_length] = '\0';
        });
    });

    TRY(copy_to_user(user_buf, &buf));
    return 0;
}

}
