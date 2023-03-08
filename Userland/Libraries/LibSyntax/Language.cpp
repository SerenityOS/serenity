/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Language.h"
#include <LibGUI/TextEditor.h>
#include <LibSyntax/Highlighter.h>

namespace Syntax {

StringView language_to_string(Language language)
{
    switch (language) {
    case Language::CMake:
        return "CMake"sv;
    case Language::CMakeCache:
        return "CMakeCache"sv;
    case Language::Cpp:
        return "C++"sv;
    case Language::CSS:
        return "CSS"sv;
    case Language::GitCommit:
        return "Git"sv;
    case Language::GML:
        return "GML"sv;
    case Language::HTML:
        return "HTML"sv;
    case Language::INI:
        return "INI"sv;
    case Language::JavaScript:
        return "JavaScript"sv;
    case Language::PlainText:
        return "Plain Text"sv;
    case Language::Shell:
        return "Shell"sv;
    case Language::SQL:
        return "SQL"sv;
    }
    VERIFY_NOT_REACHED();
}

StringView common_language_extension(Language language)
{
    switch (language) {
    case Language::CMake:
        return "cmake"sv;
    case Language::CMakeCache:
        return {};
    case Language::Cpp:
        return "cpp"sv;
    case Language::CSS:
        return "css"sv;
    case Language::GitCommit:
        return {};
    case Language::GML:
        return "gml"sv;
    case Language::HTML:
        return "html"sv;
    case Language::INI:
        return "ini"sv;
    case Language::JavaScript:
        return "js"sv;
    case Language::PlainText:
        return "txt"sv;
    case Language::Shell:
        return "sh"sv;
    case Language::SQL:
        return "sql"sv;
    }
    VERIFY_NOT_REACHED();
}

}
