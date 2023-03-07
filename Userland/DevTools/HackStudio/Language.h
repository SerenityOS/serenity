/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/LexicalPath.h>

namespace HackStudio {
enum class Language {
    Unknown,
    CMake,
    CMakeCache,
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

Language language_from_file(LexicalPath const&);
Language language_from_name(DeprecatedString const&);
DeprecatedString language_name_from_file(LexicalPath const&);

}
