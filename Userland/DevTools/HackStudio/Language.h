/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/LexicalPath.h>
#include <AK/String.h>

namespace HackStudio {
enum class Language {
    Unknown,
    Cpp,
    CSS,
    JavaScript,
    HTML,
    GitCommit,
    GML,
    Ini,
    Shell,
    SQL,
};

Language language_from_file(const LexicalPath&);
Language language_from_name(const String&);
String language_name_from_file(const LexicalPath&);

}
