/*
 * Copyright (c) 2021, Ricky Severino <rickyseverino@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>

namespace Core {

#define ENUMERATE_LANGUAGE_NAMES                                      \
    __ENUMERATE_LANGUAGE_NAME(CMake, ".cmake", "CMake")               \
    __ENUMERATE_LANGUAGE_NAME(Configuration, ".cfg", "Configuration") \
    __ENUMERATE_LANGUAGE_NAME(Cpp, ".cpp", "Cpp")                     \
    __ENUMERATE_LANGUAGE_NAME(GML, ".gml", "GML")                     \
    __ENUMERATE_LANGUAGE_NAME(HTML, ".html", "HTML")                  \
    __ENUMERATE_LANGUAGE_NAME(Ini, "ini", "Ini")                      \
    __ENUMERATE_LANGUAGE_NAME(JSON, ".json", "JSON")                  \
    __ENUMERATE_LANGUAGE_NAME(JavaScript, ".js", "JavaScript")        \
    __ENUMERATE_LANGUAGE_NAME(Markdown, ".md", "Markdown")            \
    __ENUMERATE_LANGUAGE_NAME(Plaintext, ".txt", "Plaintext")         \
    __ENUMERATE_LANGUAGE_NAME(Python, ".py", "Python")                \
    __ENUMERATE_LANGUAGE_NAME(Shell, ".sh", "Shell")                  \
    __ENUMERATE_LANGUAGE_NAME(XML, ".xml", "XML")

enum class Language {
#define __ENUMERATE_LANGUAGE_NAME(name, extension, language) name,
    ENUMERATE_LANGUAGE_NAMES
#undef __ENUMERATE_LANGUAGE_NAME
        Unknown
};

FlyString language_name_from_filename(const String&);
Language language_from_filename(const String&);

}
