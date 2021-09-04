/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace HackStudio {
enum class Language {
    Unknown,
    Cpp,
    JavaScript,
    HTML,
    GML,
    Ini,
    Shell,
    SQL,
};

Language language_from_file_extension(String const&);
Language language_from_name(String const&);
String language_name_from_file_extension(String const&);

}
