/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Language.h"

namespace HackStudio {

Language language_from_file_extension(const String& extension)
{
    VERIFY(!extension.starts_with("."));
    if (extension == "cpp" || extension == "h")
        return Language::Cpp;
    else if (extension == "js")
        return Language::JavaScript;
    else if (extension == "gml")
        return Language::GML;
    else if (extension == "ini")
        return Language::Ini;
    else if (extension == "sh")
        return Language::Shell;

    return Language::Unknown;
}

Language language_from_name(const String& name)
{
    if (name == "Cpp")
        return Language::Cpp;
    if (name == "Javascript")
        return Language::JavaScript;
    if (name == "Shell")
        return Language::Shell;

    return Language::Unknown;
}

}
