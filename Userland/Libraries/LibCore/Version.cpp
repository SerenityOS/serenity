/*
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <LibCore/System.h>
#include <LibCore/Version.h>

namespace Core::Version {

DeprecatedString read_long_version_string()
{
#if defined(AK_OS_WINDOWS)
    auto version = "1.0.0"sv;
    auto git_hash = "a"sv;
#else
    auto result = Core::System::uname();
    if (result.is_error())
        return {};

    auto version = result.value().release;
    auto git_hash = result.value().version;
#endif

    return DeprecatedString::formatted("Version {} revision {}", version, git_hash);
}

}
