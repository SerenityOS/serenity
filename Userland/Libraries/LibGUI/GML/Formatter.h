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

inline String format_gml(StringView string)
{
    auto ast = parse_gml(string);
    if (ast.is_error())
        return {};
    return ast.value()->to_string();
}

}
