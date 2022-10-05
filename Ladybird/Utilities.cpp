/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define AK_DONT_REPLACE_STD

#include "Utilities.h"
#include <AK/LexicalPath.h>
#include <AK/Platform.h>
#include <LibCore/File.h>
#include <QCoreApplication>

String s_serenity_resource_root;

AK::String akstring_from_qstring(QString const& qstring)
{
    return AK::String(qstring.toUtf8().data());
}

QString qstring_from_akstring(AK::String const& akstring)
{
    return QString::fromUtf8(akstring.characters(), akstring.length());
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
            return String::formatted("{}/Base", source_dir);
        }
        auto* home = getenv("XDG_CONFIG_HOME") ?: getenv("HOME");
        VERIFY(home);
        auto home_lagom = String::formatted("{}/.lagom", home);
        if (Core::File::is_directory(home_lagom))
            return home_lagom;
        auto app_dir = akstring_from_qstring(QCoreApplication::applicationDirPath());
        return LexicalPath(app_dir).parent().append("share"sv).string();
    }();
#endif
}
