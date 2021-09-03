/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/CharacterTypes.h>
#include <YAK/Platform.h>
#include <YAK/StringBuilder.h>
#include <YAK/Types.h>
#include <YAK/Utf8View.h>
#include <LibUnicode/CharacterTypes.h>

#if ENABLE_UNICODE_DATA
#    include <LibUnicode/UnicodeData.h>
#endif

// For details on the algorithms used here, see Section 3.13 Default Case Algorithms
// https://www.unicode.org/versions/Unicode13.0.0/ch03.pdf

namespace Unicode {

#if ENABLE_UNICODE_DATA

static bool is_final_code_point(Utf8View const& string, size_t index, size_t byte_length)
{
    // C is preceded by a sequence consisting of a cased letter and then zero or more case-ignorable
    // characters, and C is not followed by a sequence consisting of zero or more case-ignorable
    // characters and then a cased letter.
    auto preceding_view = string.substring_view(0, index);
    auto following_view = ((index + byte_length) < string.byte_length())
        ? string.substring_view(index + byte_length)
        : Utf8View {};

    size_t cased_letter_count = 0;

    for (auto code_point : preceding_view) {
        bool is_cased = code_point_has_property(code_point, Property::Cased);
        bool is_case_ignorable = code_point_has_property(code_point, Property::Case_Ignorable);

        if (is_cased && !is_case_ignorable)
            ++cased_letter_count;
        else if (!is_case_ignorable)
            cased_letter_count = 0;
    }

    if (cased_letter_count == 0)
        return false;

    for (auto code_point : following_view) {
        bool is_cased = code_point_has_property(code_point, Property::Cased);
        bool is_case_ignorable = code_point_has_property(code_point, Property::Case_Ignorable);

        if (is_case_ignorable)
            continue;
        if (is_cased)
            return false;

        break;
    }

    return true;
}

static SpecialCasing const* find_matching_special_case(Utf8View const& string, size_t index, size_t byte_length, UnicodeData const& unicode_data)
{
    for (size_t i = 0; i < unicode_data.special_casing_size; ++i) {
        auto const* special_casing = unicode_data.special_casing[i];

        if ((special_casing->locale == Locale::None) && (special_casing->condition == Condition::None))
            return special_casing;

        // FIXME: Handle locale.
        if (special_casing->locale != Locale::None)
            continue;

        switch (special_casing->condition) {
        case Condition::FinalSigma:
            if (is_final_code_point(string, index, byte_length))
                return special_casing;
            break;

        default:
            break;
        }
    }

    return nullptr;
}

#endif

u32 to_unicode_lowercase(u32 code_point)
{
#if ENABLE_UNICODE_DATA
    auto unicode_data = Detail::unicode_data_for_code_point(code_point);
    if (unicode_data.has_value())
        return unicode_data->simple_lowercase_mapping;
    return code_point;
#else
    return YAK::to_ascii_lowercase(code_point);
#endif
}

u32 to_unicode_uppercase(u32 code_point)
{
#if ENABLE_UNICODE_DATA
    auto unicode_data = Detail::unicode_data_for_code_point(code_point);
    if (unicode_data.has_value())
        return unicode_data->simple_uppercase_mapping;
    return code_point;
#else
    return YAK::to_ascii_uppercase(code_point);
#endif
}

String to_unicode_lowercase_full(StringView const& string)
{
#if ENABLE_UNICODE_DATA
    Utf8View view { string };
    StringBuilder builder;

    size_t index = 0;
    for (auto it = view.begin(); it != view.end(); ++it) {
        u32 code_point = *it;
        size_t byte_length = it.underlying_code_point_length_in_bytes();

        auto unicode_data = Detail::unicode_data_for_code_point(code_point);
        if (!unicode_data.has_value()) {
            builder.append_code_point(code_point);
            index += byte_length;
            continue;
        }

        auto const* special_casing = find_matching_special_case(view, index, byte_length, *unicode_data);
        if (!special_casing) {
            builder.append_code_point(unicode_data->simple_lowercase_mapping);
            index += byte_length;
            continue;
        }

        for (size_t i = 0; i < special_casing->lowercase_mapping_size; ++i)
            builder.append_code_point(special_casing->lowercase_mapping[i]);
    }

    return builder.build();
#else
    return string.to_lowercase_string();
#endif
}

String to_unicode_uppercase_full(StringView const& string)
{
#if ENABLE_UNICODE_DATA
    Utf8View view { string };
    StringBuilder builder;

    size_t index = 0;
    for (auto it = view.begin(); it != view.end(); ++it) {
        u32 code_point = *it;
        size_t byte_length = it.underlying_code_point_length_in_bytes();

        auto unicode_data = Detail::unicode_data_for_code_point(code_point);
        if (!unicode_data.has_value()) {
            builder.append_code_point(code_point);
            index += byte_length;
            continue;
        }

        auto const* special_casing = find_matching_special_case(view, index, byte_length, *unicode_data);
        if (!special_casing) {
            builder.append_code_point(unicode_data->simple_uppercase_mapping);
            index += byte_length;
            continue;
        }

        for (size_t i = 0; i < special_casing->uppercase_mapping_size; ++i)
            builder.append_code_point(special_casing->uppercase_mapping[i]);
    }

    return builder.build();
#else
    return string.to_uppercase_string();
#endif
}

Optional<GeneralCategory> general_category_from_string([[maybe_unused]] StringView const& general_category)
{
#if ENABLE_UNICODE_DATA
    return Detail::general_category_from_string(general_category);
#else
    return {};
#endif
}

bool code_point_has_general_category([[maybe_unused]] u32 code_point, [[maybe_unused]] GeneralCategory general_category)
{
#if ENABLE_UNICODE_DATA
    return Detail::code_point_has_general_category(code_point, general_category);
#else
    return {};
#endif
}

Optional<Property> property_from_string([[maybe_unused]] StringView const& property)
{
#if ENABLE_UNICODE_DATA
    return Detail::property_from_string(property);
#else
    return {};
#endif
}

bool code_point_has_property([[maybe_unused]] u32 code_point, [[maybe_unused]] Property property)
{
#if ENABLE_UNICODE_DATA
    return Detail::code_point_has_property(code_point, property);
#else
    return false;
#endif
}

bool is_ecma262_property([[maybe_unused]] Property property)
{
#if ENABLE_UNICODE_DATA
    // EMCA-262 only allows a subset of Unicode properties: https://tc39.es/ecma262/#table-binary-unicode-properties
    switch (property) {
    case Unicode::Property::ASCII:
    case Unicode::Property::ASCII_Hex_Digit:
    case Unicode::Property::Alphabetic:
    case Unicode::Property::Any:
    case Unicode::Property::Assigned:
    case Unicode::Property::Bidi_Control:
    case Unicode::Property::Bidi_Mirrored:
    case Unicode::Property::Case_Ignorable:
    case Unicode::Property::Cased:
    case Unicode::Property::Changes_When_Casefolded:
    case Unicode::Property::Changes_When_Casemapped:
    case Unicode::Property::Changes_When_Lowercased:
    case Unicode::Property::Changes_When_NFKC_Casefolded:
    case Unicode::Property::Changes_When_Titlecased:
    case Unicode::Property::Changes_When_Uppercased:
    case Unicode::Property::Dash:
    case Unicode::Property::Default_Ignorable_Code_Point:
    case Unicode::Property::Deprecated:
    case Unicode::Property::Diacritic:
    case Unicode::Property::Emoji:
    case Unicode::Property::Emoji_Component:
    case Unicode::Property::Emoji_Modifier:
    case Unicode::Property::Emoji_Modifier_Base:
    case Unicode::Property::Emoji_Presentation:
    case Unicode::Property::Extended_Pictographic:
    case Unicode::Property::Extender:
    case Unicode::Property::Grapheme_Base:
    case Unicode::Property::Grapheme_Extend:
    case Unicode::Property::Hex_Digit:
    case Unicode::Property::IDS_Binary_Operator:
    case Unicode::Property::IDS_Trinary_Operator:
    case Unicode::Property::ID_Continue:
    case Unicode::Property::ID_Start:
    case Unicode::Property::Ideographic:
    case Unicode::Property::Join_Control:
    case Unicode::Property::Logical_Order_Exception:
    case Unicode::Property::Lowercase:
    case Unicode::Property::Math:
    case Unicode::Property::Noncharacter_Code_Point:
    case Unicode::Property::Pattern_Syntax:
    case Unicode::Property::Pattern_White_Space:
    case Unicode::Property::Quotation_Mark:
    case Unicode::Property::Radical:
    case Unicode::Property::Regional_Indicator:
    case Unicode::Property::Sentence_Terminal:
    case Unicode::Property::Soft_Dotted:
    case Unicode::Property::Terminal_Punctuation:
    case Unicode::Property::Unified_Ideograph:
    case Unicode::Property::Uppercase:
    case Unicode::Property::Variation_Selector:
    case Unicode::Property::White_Space:
    case Unicode::Property::XID_Continue:
    case Unicode::Property::XID_Start:
        return true;
    default:
        return false;
    }
#else
    return false;
#endif
}

Optional<Script> script_from_string([[maybe_unused]] StringView const& script)
{
#if ENABLE_UNICODE_DATA
    return Detail::script_from_string(script);
#else
    return {};
#endif
}

bool code_point_has_script([[maybe_unused]] u32 code_point, [[maybe_unused]] Script script)
{
#if ENABLE_UNICODE_DATA
    return Detail::code_point_has_script(code_point, script);
#else
    return false;
#endif
}

bool code_point_has_script_extension([[maybe_unused]] u32 code_point, [[maybe_unused]] Script script)
{
#if ENABLE_UNICODE_DATA
    return Detail::code_point_has_script_extension(code_point, script);
#else
    return false;
#endif
}

}
