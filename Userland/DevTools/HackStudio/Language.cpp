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
    if (extension == "c" || extension == "cc" || extension == "cxx" || extension == "cpp" || extension == "c++"
        || extension == "h" || extension == "cc" || extension == "hxx" || extension == "hpp" || extension == "h++")
        return Language::Cpp;
    if (extension == "js" || extension == "mjs" || extension == "json")
        return Language::JavaScript;
    if (extension == "html" || extension == "htm")
        return Language::HTML;
    if (extension == "gml")
        return Language::GML;
    if (extension == "ini")
        return Language::Ini;
    if (extension == "sh" || extension == "bash")
        return Language::Shell;
    if (extension == "sql")
        return Language::SQL;
    if (extension == "mk")
        return Language::Makefile;

    return Language::Unknown;
}

Language language_from_file_name(const String& name)
{
    if (name == "makefile" || name == "Makefile" || name == "GNUmakefile")
        return Language::Makefile;
    if (name == "CMakeLists.txt")
        return Language::CMake;

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
    if (extension == "c" || extension == "cc" || extension == "cxx" || extension == "cpp" || extension == "c++"
        || extension == "h" || extension == "hh" || extension == "hxx" || extension == "hpp" || extension == "h++")
        return "C++";
    if (extension == "js" || extension == "mjs" || extension == "json")
        return "JavaScript";
    if (extension == "gml")
        return "GML";
    if (extension == "ini")
        return "Ini";
    if (extension == "sh" || extension == "bash")
        return "Shell";
    if (extension == "md")
        return "Markdown";
    if (extension == "html" || extension == "htm")
        return "HTML";
    if (extension == "sql")
        return "SQL";
    if (extension == "txt")
        return "Plaintext";
    if (extension == "mk")
        return "Makefile";

    return "Unknown";
}

String language_name_from_file_name(const String& name)
{
    if (name == "makefile" || name == "Makefile" || name == "GNUmakefile")
        return "Makefile";
    if (name == "CMakeLists.txt")
        return "CMake";

    return "Unknown";
}

}
