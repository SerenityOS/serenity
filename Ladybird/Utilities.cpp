/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Utilities.h"
#include <AK/LexicalPath.h>
#include <AK/Platform.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>

DeprecatedString s_serenity_resource_root;

ErrorOr<String> application_directory()
{
    auto current_executable_path = TRY(Core::System::current_executable_path());
    auto dirname = LexicalPath::dirname(current_executable_path.to_deprecated_string());
    return String::from_deprecated_string(dirname);
}

void platform_init()
{
#ifdef AK_OS_ANDROID
    extern void android_platform_init();
    android_platform_init();
#else
    s_serenity_resource_root = [] {
        auto const* source_dir = getenv("SERENITY_SOURCE_DIR");
        if (source_dir) {
            return DeprecatedString::formatted("{}/Base", source_dir);
        }
        auto* home = getenv("XDG_CONFIG_HOME") ?: getenv("HOME");
        VERIFY(home);
        auto home_lagom = DeprecatedString::formatted("{}/.lagom", home);
        if (FileSystem::is_directory(home_lagom))
            return home_lagom;
        auto app_dir = application_directory().release_value_but_fixme_should_propagate_errors().to_deprecated_string();
#    ifdef AK_OS_MACOS
        return LexicalPath(app_dir).parent().append("Resources"sv).string();
#    else
        return LexicalPath(app_dir).parent().append("share"sv).string();
#    endif
    }();
#endif
}

ErrorOr<Vector<String>> get_paths_for_helper_process(StringView process_name)
{
    auto application_path = TRY(application_directory());
    Vector<String> paths;

    TRY(paths.try_append(TRY(String::formatted("{}/{}", application_path, process_name))));
    TRY(paths.try_append(TRY(String::formatted("./{}", process_name))));
    // NOTE: Add platform-specific paths here
    return paths;
}
