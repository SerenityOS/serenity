/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Utilities.h"
#include <AK/LexicalPath.h>
#include <AK/Platform.h>
#include <LibCore/ResourceImplementationFile.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>

ByteString s_serenity_resource_root;

ErrorOr<ByteString> application_directory()
{
    auto current_executable_path = TRY(Core::System::current_executable_path());
    return LexicalPath::dirname(current_executable_path);
}

void platform_init()
{
    s_serenity_resource_root = [] {
        auto* home = getenv("XDG_CONFIG_HOME") ?: getenv("HOME");
        VERIFY(home);
        auto home_lagom = ByteString::formatted("{}/.lagom", home);
        if (FileSystem::is_directory(home_lagom))
            return home_lagom;
        auto app_dir = MUST(application_directory());
#ifdef AK_OS_MACOS
        return LexicalPath(app_dir).parent().append("Resources"sv).string();
#else
        return LexicalPath(app_dir).parent().append("share/Lagom"sv).string();
#endif
    }();

    Core::ResourceImplementation::install(make<Core::ResourceImplementationFile>(MUST(String::from_byte_string(s_serenity_resource_root))));
}

ErrorOr<Vector<ByteString>> get_paths_for_helper_process(StringView process_name)
{
    auto application_path = TRY(application_directory());
    Vector<ByteString> paths;

    TRY(paths.try_append(ByteString::formatted("{}/{}", application_path, process_name)));
    TRY(paths.try_append(ByteString::formatted("./{}", process_name)));
    // NOTE: Add platform-specific paths here
    return paths;
}
