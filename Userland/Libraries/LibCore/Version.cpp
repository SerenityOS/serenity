/*
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/Version.h>

#ifdef AK_OS_SERENITY
#    include <sys/utsname.h>
#endif

namespace Core::Version {

ErrorOr<String> read_long_version_string()
{
#ifdef AK_OS_SERENITY
    struct utsname uts;
    int rc = uname(&uts);
    if ((rc) < 0) {
        return Error::from_syscall("uname"sv, rc);
    }
    auto const* version = uts.release;
    auto const* git_hash = uts.version;

    return String::formatted("Version {} revision {}", version, git_hash);
#else
    return "Version 1.0"_string;
#endif
}

}
