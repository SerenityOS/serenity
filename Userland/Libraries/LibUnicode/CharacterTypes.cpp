/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <AK/Utf8View.h>
#include <LibUnicode/CharacterTypes.h>

#if ENABLE_UNICODE_DATA
#    include <LibUnicode/UnicodeData.h>
#else
#    include <AK/CharacterTypes.h>
#endif

// For details on the algorithms used here, see Section 3.13 Default Case Algorithms
// https://www.unicode.org/versions/Unicode13.0.0/ch03.pdf

namespace Unicode {

#if ENABLE_UNICODE_DATA

static bool is_cased_letter(UnicodeData const& unicode_data)
{
    // A character C is defined to be cased if and only if C has the Lowercase or Uppercase property
    // or has a General_Category value of Titlecase_Letter.
    switch (unicode_data.general_category) {
    case GeneralCategory::Ll: // FIXME: Should be Ll + Other_Lowercase (PropList.txt).
    case GeneralCategory::Lu: // FIXME: Should be Lu + Other_Uppercase (PropList.txt).
    case GeneralCategory::Lt:
        return true;
    default:
        return false;
    }
}

static bool is_case_ignorable(UnicodeData const& unicode_data)
{
    // A character C is defined to be case-ignorable if C has the value MidLetter (ML),
    // MidNumLet (MB), or Single_Quote (SQ) for the Word_Break property or its General_Category is
    // one of Nonspacing_Mark (Mn), Enclosing_Mark (Me), Format (Cf), Modifier_Letter (Lm), or
    // Modifier_Symbol (Sk).
    switch (unicode_data.general_category) {
    case GeneralCategory::Mn:
    case GeneralCategory::Me:
    case GeneralCategory::Cf:
    case GeneralCategory::Lm:
    case GeneralCategory::Sk:
        return true;
    default:
        // FIXME: Handle word break properties (auxiliary/WordBreakProperty.txt).
        return false;
    }
}

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
        auto unicode_data = unicode_data_for_code_point(code_point);
        if (!unicode_data.has_value())
            return false;

        if (is_cased_letter(*unicode_data))
            ++cased_letter_count;
        else if (!is_case_ignorable(*unicode_data))
            cased_letter_count = 0;
    }

    if (cased_letter_count == 0)
        return false;

    for (auto code_point : following_view) {
        auto unicode_data = unicode_data_for_code_point(code_point);
        if (!unicode_data.has_value())
            return false;

        if (is_case_ignorable(*unicode_data))
            continue;
        if (is_cased_letter(*unicode_data))
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
    auto unicode_data = unicode_data_for_code_point(code_point);
    if (unicode_data.has_value())
        return unicode_data->simple_lowercase_mapping;
    return code_point;
#else
    return AK::to_ascii_lowercase(code_point);
#endif
}

u32 to_unicode_uppercase(u32 code_point)
{
#if ENABLE_UNICODE_DATA
    auto unicode_data = unicode_data_for_code_point(code_point);
    if (unicode_data.has_value())
        return unicode_data->simple_uppercase_mapping;
    return code_point;
#else
    return AK::to_ascii_uppercase(code_point);
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

        auto unicode_data = unicode_data_for_code_point(code_point);
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

        auto unicode_data = unicode_data_for_code_point(code_point);
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

}
