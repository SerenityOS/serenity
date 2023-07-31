/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HelperProcess.h"
#include "Utilities.h"
#include <QCoreApplication>

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
