/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Language.h"
#include <AK/LexicalPath.h>

namespace HackStudio {

Language language_from_file(const LexicalPath& file)
{
    if (file.title() == "COMMIT_EDITMSG")
        return Language::GitCommit;

    auto extension = file.extension();
    VERIFY(!extension.starts_with("."));
    if (extension == "c" || extension == "cc" || extension == "cxx" || extension == "cpp" || extension == "c++"
        || extension == "h" || extension == "hh" || extension == "hxx" || extension == "hpp" || extension == "h++")
        return Language::Cpp;
    if (extension == "js" || extension == "mjs" || extension == "json")
        return Language::JavaScript;
    if (extension == "html" || extension == "htm")
        return Language::HTML;
    if (extension == "css")
        return Language::CSS;
    if (extension == "gml")
        return Language::GML;
    if (extension == "ini" || extension == "af")
        return Language::Ini;
    if (extension == "sh" || extension == "bash")
        return Language::Shell;
    if (extension == "sql")
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
    if (name == "GitCommit")
        return Language::GitCommit;

    return Language::Unknown;
}

String language_name_from_file(const LexicalPath& file)
{
    if (file.title() == "COMMIT_EDITMSG")
        return "GitCommit";

    auto extension = file.extension();
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
    if (extension == "css")
        return "CSS";
    if (extension == "sql")
        return "SQL";
    if (extension == "txt")
        return "Plaintext";

    return "Unknown";
}

}
