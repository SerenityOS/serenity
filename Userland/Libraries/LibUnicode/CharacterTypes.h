/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Forward.h>
#include <YAK/String.h>
#include <YAK/Types.h>
#include <LibUnicode/Forward.h>

namespace Unicode {

// Note: The single code point case conversions only perform simple case folding.
// Use the full-string transformations for full case folding.
u32 to_unicode_lowercase(u32 code_point);
u32 to_unicode_uppercase(u32 code_point);

String to_unicode_lowercase_full(StringView const&);
String to_unicode_uppercase_full(StringView const&);

Optional<GeneralCategory> general_category_from_string(StringView const&);
bool code_point_has_general_category(u32 code_point, GeneralCategory general_category);

Optional<Property> property_from_string(StringView const&);
bool code_point_has_property(u32 code_point, Property property);
bool is_ecma262_property(Property);

Optional<Script> script_from_string(StringView const&);
bool code_point_has_script(u32 code_point, Script script);
bool code_point_has_script_extension(u32 code_point, Script script);

}
