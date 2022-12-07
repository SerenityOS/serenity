/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/LexicalPath.h>
#include <AK/Platform.h>
#include <AK/StringBuilder.h>
#include <LibCore/SessionManagement.h>
#include <LibCore/StandardPaths.h>
#include <pwd.h>
#include <stdlib.h>
#include <unistd.h>

namespace Core {

DeprecatedString StandardPaths::home_directory()
{
    if (auto* home_env = getenv("HOME"))
        return LexicalPath::canonicalized_path(StringView { home_env, strlen(home_env) }).release_value_but_fixme_should_propagate_errors().to_deprecated_string();

    auto* pwd = getpwuid(getuid());
    DeprecatedString path = pwd ? pwd->pw_dir : "/";
    endpwent();
    return LexicalPath::canonicalized_path(path.view()).release_value_but_fixme_should_propagate_errors().to_deprecated_string();
}

DeprecatedString StandardPaths::desktop_directory()
{
    StringBuilder builder;
    builder.append(home_directory());
    builder.append("/Desktop"sv);
    return LexicalPath::canonicalized_path(builder.string_view()).release_value_but_fixme_should_propagate_errors().to_deprecated_string();
}

DeprecatedString StandardPaths::documents_directory()
{
    StringBuilder builder;
    builder.append(home_directory());
    builder.append("/Documents"sv);
    return LexicalPath::canonicalized_path(builder.string_view()).release_value_but_fixme_should_propagate_errors().to_deprecated_string();
}

DeprecatedString StandardPaths::downloads_directory()
{
    StringBuilder builder;
    builder.append(home_directory());
    builder.append("/Downloads"sv);
    return LexicalPath::canonicalized_path(builder.string_view()).release_value_but_fixme_should_propagate_errors().to_deprecated_string();
}

DeprecatedString StandardPaths::config_directory()
{
    if (auto* config_directory = getenv("XDG_CONFIG_HOME"))
        return LexicalPath::canonicalized_path(StringView { config_directory, strlen(config_directory) }).release_value_but_fixme_should_propagate_errors().to_deprecated_string();

    StringBuilder builder;
    builder.append(home_directory());
#if defined(AK_OS_MACOS)
    builder.append("/Library/Preferences"sv);
#else
    builder.append("/.config"sv);
#endif
    return LexicalPath::canonicalized_path(builder.to_deprecated_string()).release_value_but_fixme_should_propagate_errors().to_deprecated_string();
}

DeprecatedString StandardPaths::data_directory()
{
    if (auto* data_directory = getenv("XDG_DATA_HOME"))
        return LexicalPath::canonicalized_path(StringView { data_directory, strlen(data_directory) }).release_value_but_fixme_should_propagate_errors().to_deprecated_string();

    StringBuilder builder;
    builder.append(home_directory());
#if defined(AK_OS_SERENITY)
    builder.append("/.data"sv);
#elif defined(AK_OS_MACOS)
    builder.append("/Library/Application Support"sv);
#else
    builder.append("/.local/share"sv);
#endif

    return LexicalPath::canonicalized_path(builder.string_view()).release_value_but_fixme_should_propagate_errors().to_deprecated_string();
}

ErrorOr<DeprecatedString> StandardPaths::runtime_directory()
{
    if (auto* data_directory = getenv("XDG_RUNTIME_DIR"))
        return TRY(LexicalPath::canonicalized_path({ data_directory, strlen(data_directory) })).to_deprecated_string();

    StringBuilder builder;

#if defined(AK_OS_SERENITY)
    auto sid = TRY(Core::SessionManagement::root_session_id());
    builder.appendff("/tmp/session/{}", sid);
#elif defined(AK_OS_MACOS)
    builder.append(home_directory());
    builder.append("/Library/Application Support"sv);
#else
    auto uid = getuid();
    builder.appendff("/run/user/{}", uid);
#endif

    return TRY(LexicalPath::canonicalized_path(builder.to_deprecated_string())).to_deprecated_string();
}

DeprecatedString StandardPaths::tempfile_directory()
{
    return "/tmp";
}

}
