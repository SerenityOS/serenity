/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Optional.h>
#include <AK/Span.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <LibUnicode/Forward.h>

namespace Unicode {

Optional<String> code_point_display_name(u32 code_point);
Optional<StringView> code_point_abbreviation(u32 code_point);

u32 canonical_combining_class(u32 code_point);
Span<SpecialCasing const* const> special_case_mapping(u32 code_point);

// Note: The single code point case conversions only perform simple case folding.
// Use the full-string transformations for full case folding.
u32 to_unicode_lowercase(u32 code_point);
u32 to_unicode_uppercase(u32 code_point);

String to_unicode_lowercase_full(StringView, Optional<StringView> locale = {});
String to_unicode_uppercase_full(StringView, Optional<StringView> locale = {});

Optional<GeneralCategory> general_category_from_string(StringView);
bool code_point_has_general_category(u32 code_point, GeneralCategory general_category);

Optional<Property> property_from_string(StringView);
bool code_point_has_property(u32 code_point, Property property);
bool is_ecma262_property(Property);

Optional<Script> script_from_string(StringView);
bool code_point_has_script(u32 code_point, Script script);
bool code_point_has_script_extension(u32 code_point, Script script);

bool code_point_has_grapheme_break_property(u32 code_point, GraphemeBreakProperty property);
bool code_point_has_word_break_property(u32 code_point, WordBreakProperty property);
bool code_point_has_sentence_break_property(u32 code_point, SentenceBreakProperty property);

Vector<size_t> find_grapheme_segmentation_boundaries(Utf16View const&);
Vector<size_t> find_word_segmentation_boundaries(Utf16View const&);
Vector<size_t> find_sentence_segmentation_boundaries(Utf16View const&);

}
