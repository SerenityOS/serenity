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
    else if (extension == "js" || extension == "json")
        return Language::JavaScript;
    else if (extension == "html" || extension == "htm")
        return Language::HTML;
    else if (extension == "gml")
        return Language::GML;
    else if (extension == "ini")
        return Language::Ini;
    else if (extension == "sh" || extension == "bash")
        return Language::Shell;
    else if (extension == "sql")
        return Language::SQL;

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

String language_name_from_file_extension(const String& extension)
{
    VERIFY(!extension.starts_with("."));
    if (extension == "cpp" || extension == "h")
        return "C++";
    else if (extension == "js" || extension == "json")
        return "JavaScript";
    else if (extension == "gml")
        return "GML";
    else if (extension == "ini")
        return "Ini";
    else if (extension == "sh" || extension == "bash")
        return "Shell";
    else if (extension == "md")
        return "Markdown";
    else if (extension == "html" || extension == "htm")
        return "HTML";
    else if (extension == "sql")
        return "SQL";
    else if (extension == "txt")
        return "Plaintext";

    return "Unknown";
}

}
