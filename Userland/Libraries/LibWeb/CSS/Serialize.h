/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringView.h>

namespace Web::CSS {

String escape_a_character(u32 character);
String escape_a_character_as_code_point(u32 character);

String serialize_an_identifier(StringView const& ident);

}
