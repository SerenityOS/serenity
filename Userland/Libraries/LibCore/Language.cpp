/*
 * Copyright (c) 2021, Ricky Severino <rickyseverino@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Language.h"

namespace Core {

FlyString language_name_from_filename(const String& file_path)
{
#define __ENUMERATE_LANGUAGE_NAME(name, extension, language)              \
    if (file_path.ends_with(extension, CaseSensitivity::CaseInsensitive)) \
        return language;
    ENUMERATE_LANGUAGE_NAMES;
#undef __ENUMERATE_LANGUAGE_NAME

    if (file_path.ends_with(".h", CaseSensitivity::CaseInsensitive))
        return "Cpp";

    return "Unknown";
}

Language language_from_filename(const String& file_path)
{
#define __ENUMERATE_LANGUAGE_NAME(name, extension, language)              \
    if (file_path.ends_with(extension, CaseSensitivity::CaseInsensitive)) \
        return Language::name;
    ENUMERATE_LANGUAGE_NAMES;
#undef __ENUMERATE_LANGUAGE_NAME

    if (file_path.ends_with(".h", CaseSensitivity::CaseInsensitive))
        return Language::Cpp;

    return Language::Unknown;
}

}
