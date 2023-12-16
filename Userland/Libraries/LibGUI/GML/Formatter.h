/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Forward.h>
#include <LibGUI/GML/AST.h>
#include <LibGUI/GML/Parser.h>

namespace GUI::GML {

inline ErrorOr<ByteString> format_gml(StringView string)
{
    return TRY(parse_gml(string))->to_byte_string();
}

}
