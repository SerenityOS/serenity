/*
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/System.h>
#include <LibCore/Version.h>

namespace Core::Version {

ErrorOr<String> read_long_version_string()
{
#ifdef AK_OS_SERENITY
    auto uname = TRY(Core::System::uname());

    auto const* version = uname.release;
    auto const* git_hash = uname.version;

    return String::formatted("Version {} revision {}", version, git_hash);
#else
    return String::from_utf8("Version 1.0"sv);
#endif
}

}
