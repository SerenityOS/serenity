/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Utilities.h"
#include <AK/LexicalPath.h>
#include <AK/Platform.h>
#include <LibFileSystem/FileSystem.h>
#include <QCoreApplication>

DeprecatedString s_serenity_resource_root;

AK::DeprecatedString ak_deprecated_string_from_qstring(QString const& qstring)
{
    return AK::DeprecatedString(qstring.toUtf8().data());
}

ErrorOr<String> ak_string_from_qstring(QString const& qstring)
{
    return String::from_utf8(StringView(qstring.toUtf8().data(), qstring.size()));
}

QString qstring_from_ak_deprecated_string(AK::DeprecatedString const& ak_deprecated_string)
{
    return QString::fromUtf8(ak_deprecated_string.characters(), ak_deprecated_string.length());
}

QString qstring_from_ak_string(String const& ak_string)
{
    auto view = ak_string.bytes_as_string_view();
    return QString::fromUtf8(view.characters_without_null_termination(), view.length());
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
        auto app_dir = ak_deprecated_string_from_qstring(QCoreApplication::applicationDirPath());
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
    auto application_path = TRY(ak_string_from_qstring(QCoreApplication::applicationDirPath()));
    Vector<String> paths;

    TRY(paths.try_append(TRY(String::formatted("./{}/{}", process_name, process_name))));
    TRY(paths.try_append(TRY(String::formatted("{}/{}/{}", application_path, process_name, process_name))));
    TRY(paths.try_append(TRY(String::formatted("{}/{}", application_path, process_name))));
    TRY(paths.try_append(TRY(String::formatted("./{}", process_name))));
    // NOTE: Add platform-specific paths here
    return paths;
}
