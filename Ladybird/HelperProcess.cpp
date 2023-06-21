/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HelperProcess.h"
#include "Utilities.h"
#include <AK/String.h>
#include <QCoreApplication>

ErrorOr<void> spawn_helper_process(StringView process_name, ReadonlySpan<StringView> arguments, Core::System::SearchInPath search_in_path, Optional<ReadonlySpan<StringView>> environment)
{
    auto paths = TRY(get_paths_for_helper_process(process_name));
    VERIFY(!paths.is_empty());
    ErrorOr<void> result;
    for (auto const& path : paths) {
        result = Core::System::exec(path, arguments, search_in_path, environment);
        if (!result.is_error())
            break;
    }

    return result;
}

ErrorOr<Vector<String>> get_paths_for_helper_process(StringView process_name)
{
    auto application_path = TRY(ak_string_from_qstring(QCoreApplication::applicationDirPath()));
    Vector<String> paths;

    TRY(paths.try_append(TRY(String::formatted("./{}/{}", process_name, process_name))));
    TRY(paths.try_append(TRY(String::formatted("{}/{}/{}", application_path, process_name, process_name))));
    TRY(paths.try_append(TRY(String::formatted("{}/{}", application_path, process_name))));
    TRY(paths.try_append(TRY(String::formatted("./{}", process_name))));
    // NOTE: Add platform-specific paths here
    return paths;
}
