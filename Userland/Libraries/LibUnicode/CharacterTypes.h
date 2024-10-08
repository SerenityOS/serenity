/*
 * Copyright (c) 2021-2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Forward.h>
#include <AK/Optional.h>
#include <AK/Span.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibUnicode/Forward.h>

namespace Unicode {

struct CodePointRange {
    u32 first { 0 };
    u32 last { 0 };
};

struct CodePointRangeComparator {
    constexpr int operator()(u32 code_point, CodePointRange const& range)
    {
        return (code_point > range.last) - (code_point < range.first);
    }
};

struct BlockName {
    CodePointRange code_point_range {};
    StringView display_name;
};

Optional<ByteString> code_point_display_name(u32 code_point);
Optional<StringView> code_point_block_display_name(u32 code_point);
Optional<StringView> code_point_abbreviation(u32 code_point);

ReadonlySpan<BlockName> block_display_names();

u32 canonical_combining_class(u32 code_point);

// Note: The single code point case conversions only perform simple case folding.
// Use the full-string transformations for full case folding.
u32 to_unicode_lowercase(u32 code_point);
u32 to_unicode_uppercase(u32 code_point);
u32 to_unicode_titlecase(u32 code_point);

template<typename ViewType>
bool equals_ignoring_case(ViewType, ViewType);

template<typename ViewType>
Optional<size_t> find_ignoring_case(ViewType, ViewType);

Optional<GeneralCategory> general_category_from_string(StringView);
bool code_point_has_general_category(u32 code_point, GeneralCategory general_category);

bool code_point_has_control_general_category(u32 code_point);
bool code_point_has_letter_general_category(u32 code_point);
bool code_point_has_number_general_category(u32 code_point);
bool code_point_has_punctuation_general_category(u32 code_point);
bool code_point_has_separator_general_category(u32 code_point);
bool code_point_has_space_separator_general_category(u32 code_point);
bool code_point_has_symbol_general_category(u32 code_point);

Optional<Property> property_from_string(StringView);
bool code_point_has_property(u32 code_point, Property property);

bool code_point_has_emoji_property(u32 code_point);
bool code_point_has_emoji_modifier_base_property(u32 code_point);
bool code_point_has_emoji_presentation_property(u32 code_point);
bool code_point_has_identifier_start_property(u32 code_point);
bool code_point_has_identifier_continue_property(u32 code_point);
bool code_point_has_regional_indicator_property(u32 code_point);
bool code_point_has_variation_selector_property(u32 code_point);

bool is_ecma262_property(Property);

Optional<Script> script_from_string(StringView);
bool code_point_has_script(u32 code_point, Script script);
bool code_point_has_script_extension(u32 code_point, Script script);

bool code_point_has_grapheme_break_property(u32 code_point, GraphemeBreakProperty property);
bool code_point_has_word_break_property(u32 code_point, WordBreakProperty property);
bool code_point_has_sentence_break_property(u32 code_point, SentenceBreakProperty property);

enum class BidirectionalClassInternal : u8;
Optional<BidirectionalClassInternal> bidirectional_class_internal(u32 code_point);

enum class BidiClass {
    ArabicNumber,             // AN
    BlockSeparator,           // B
    BoundaryNeutral,          // BN
    CommonNumberSeparator,    // CS
    DirNonSpacingMark,        // NSM
    EuropeanNumber,           // EN
    EuropeanNumberSeparator,  // ES
    EuropeanNumberTerminator, // ET
    FirstStrongIsolate,       // FSI
    LeftToRight,              // L
    LeftToRightEmbedding,     // LRE
    LeftToRightIsolate,       // LRI
    LeftToRightOverride,      // LRO
    OtherNeutral,             // ON
    PopDirectionalFormat,     // PDF
    PopDirectionalIsolate,    // PDI
    RightToLeft,              // R
    RightToLeftArabic,        // AL
    RightToLeftEmbedding,     // RLE
    RightToLeftIsolate,       // RLI
    RightToLeftOverride,      // RLO
    SegmentSeparator,         // S
    WhiteSpaceNeutral,        // WS
};

BidiClass bidirectional_class(u32 code_point);

}
