/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/LexicalPath.h>
#include <AK/Platform.h>
#include <AK/StringBuilder.h>
#include <LibCore/StandardPaths.h>
#include <pwd.h>
#include <stdlib.h>
#include <unistd.h>

namespace Core {

DeprecatedString StandardPaths::home_directory()
{
    if (auto* home_env = getenv("HOME"))
        return LexicalPath::canonicalized_path(home_env);

    auto* pwd = getpwuid(getuid());
    DeprecatedString path = pwd ? pwd->pw_dir : "/";
    endpwent();
    return LexicalPath::canonicalized_path(path);
}

DeprecatedString StandardPaths::desktop_directory()
{
    StringBuilder builder;
    builder.append(home_directory());
    builder.append("/Desktop"sv);
    return LexicalPath::canonicalized_path(builder.to_deprecated_string());
}

DeprecatedString StandardPaths::documents_directory()
{
    StringBuilder builder;
    builder.append(home_directory());
    builder.append("/Documents"sv);
    return LexicalPath::canonicalized_path(builder.to_deprecated_string());
}

DeprecatedString StandardPaths::downloads_directory()
{
    StringBuilder builder;
    builder.append(home_directory());
    builder.append("/Downloads"sv);
    return LexicalPath::canonicalized_path(builder.to_deprecated_string());
}

DeprecatedString StandardPaths::config_directory()
{
    if (auto* config_directory = getenv("XDG_CONFIG_HOME"))
        return LexicalPath::canonicalized_path(config_directory);

    StringBuilder builder;
    builder.append(home_directory());
#if defined(AK_OS_MACOS)
    builder.append("/Library/Preferences"sv);
#else
    builder.append("/.config"sv);
#endif
    return LexicalPath::canonicalized_path(builder.to_deprecated_string());
}

DeprecatedString StandardPaths::data_directory()
{
    if (auto* data_directory = getenv("XDG_DATA_HOME"))
        return LexicalPath::canonicalized_path(data_directory);

    StringBuilder builder;
    builder.append(home_directory());
#if defined(AK_OS_SERENITY)
    builder.append("/.data"sv);
#elif defined(AK_OS_MACOS)
    builder.append("/Library/Application Support"sv);
#else
    builder.append("/.local/share"sv);
#endif

    return LexicalPath::canonicalized_path(builder.to_deprecated_string());
}

DeprecatedString StandardPaths::tempfile_directory()
{
    return "/tmp";
}

}
