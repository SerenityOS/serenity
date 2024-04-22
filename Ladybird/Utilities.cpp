/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Utilities.h"
#include <AK/LexicalPath.h>
#include <AK/Platform.h>
#include <LibCore/Environment.h>
#include <LibCore/ResourceImplementationFile.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>

#define TOKENCAT(x, y) x##y
#define STRINGIFY(x) TOKENCAT(x, sv)

// This is expected to be set from the build scripts, if a packager desires
#if defined(LADYBIRD_LIBEXECDIR)
constexpr auto libexec_path = STRINGIFY(LADYBIRD_LIBEXECDIR);
#else
constexpr auto libexec_path = "libexec"sv;
#endif

ByteString s_serenity_resource_root;

Optional<ByteString> s_mach_server_name;

Optional<ByteString const&> mach_server_name()
{
    if (s_mach_server_name.has_value())
        return *s_mach_server_name;
    return {};
}

void set_mach_server_name(ByteString name)
{
    s_mach_server_name = move(name);
}

ErrorOr<ByteString> application_directory()
{
    auto current_executable_path = TRY(Core::System::current_executable_path());
    return LexicalPath::dirname(current_executable_path);
}

[[gnu::used]] static LexicalPath find_prefix(LexicalPath const& application_directory);
static LexicalPath find_prefix(LexicalPath const& application_directory)
{
    if (application_directory.string().ends_with(libexec_path)) {
        // Strip libexec_path if it's there
        return LexicalPath(application_directory.string().substring_view(0, application_directory.string().length() - libexec_path.length()));
    }

    // Otherwise, we are in $prefix/bin
    return application_directory.parent();
}

void platform_init()
{
    s_serenity_resource_root = [] {
        auto home = Core::Environment::get("XDG_CONFIG_HOME"sv)
                        .value_or_lazy_evaluated_optional([]() { return Core::Environment::get("HOME"sv); });
        if (home.has_value()) {
            auto home_lagom = ByteString::formatted("{}/.lagom", home);
            if (FileSystem::is_directory(home_lagom))
                return home_lagom;
        }
        auto app_dir = MUST(application_directory());
#ifdef AK_OS_MACOS
        return LexicalPath(app_dir).parent().append("Resources"sv).string();
#else
        return find_prefix(LexicalPath(app_dir)).append("share/Lagom"sv).string();
#endif
    }();
    Core::ResourceImplementation::install(make<Core::ResourceImplementationFile>(MUST(String::from_byte_string(s_serenity_resource_root))));
}

ErrorOr<Vector<ByteString>> get_paths_for_helper_process(StringView process_name)
{
    auto application_path = TRY(application_directory());
    Vector<ByteString> paths;

#if !defined(AK_OS_MACOS)
    auto prefix = find_prefix(LexicalPath(application_path));
    TRY(paths.try_append(LexicalPath::join(prefix.string(), libexec_path, process_name).string()));
    TRY(paths.try_append(LexicalPath::join(prefix.string(), "bin"sv, process_name).string()));
#endif
    TRY(paths.try_append(ByteString::formatted("{}/{}", application_path, process_name)));
    TRY(paths.try_append(ByteString::formatted("./{}", process_name)));
    // NOTE: Add platform-specific paths here
    return paths;
}
