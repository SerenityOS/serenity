/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/String.h>
#include <LibGUI/GML/AST.h>
#include <LibGUI/GML/Parser.h>

namespace GUI::GML {

inline ErrorOr<String> format_gml(StringView string)
{
    return TRY(parse_gml(string))->to_string();
}

}
