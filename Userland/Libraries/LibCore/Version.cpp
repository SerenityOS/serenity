/*
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/System.h>
#include <LibCore/Version.h>

namespace Core::Version {

String read_long_version_string()
{
    auto result = Core::System::uname();
    if (result.is_error())
        return {};

    auto version = result.value().release;
    auto git_hash = result.value().version;

    return String::formatted("Version {} revision {}", version, git_hash);
}

}
