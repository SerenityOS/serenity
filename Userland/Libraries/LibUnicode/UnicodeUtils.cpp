/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <LibUnicode/CharacterTypes.h>
#include <LibUnicode/UnicodeUtils.h>

#if ENABLE_UNICODE_DATA
#    include <LibUnicode/UnicodeData.h>
#endif

// For details on the algorithms used here, see Section 3.13 Default Case Algorithms
// https://www.unicode.org/versions/Unicode13.0.0/ch03.pdf

namespace Unicode::Detail {

#if ENABLE_UNICODE_DATA

static bool is_after_uppercase_i(Utf8View const& string, size_t index)
{
    // There is an uppercase I before C, and there is no intervening combining character class 230 (Above) or 0.
    auto preceding_view = string.substring_view(0, index);
    bool found_uppercase_i = false;

    // FIXME: Would be better if Utf8View supported reverse iteration.
    for (auto code_point : preceding_view) {
        if (code_point == 'I') {
            found_uppercase_i = true;
            continue;
        }

        auto combining_class = canonical_combining_class(code_point);
        if (combining_class == 0 || combining_class == 230)
            found_uppercase_i = false;
    }

    return found_uppercase_i;
}

static bool is_after_soft_dotted_code_point(Utf8View const& string, size_t index)
{
    // There is a Soft_Dotted character before C, with no intervening character of combining class 0 or 230 (Above).
    auto preceding_view = string.substring_view(0, index);
    bool found_soft_dotted_code_point = false;

    // FIXME: Would be better if Utf8View supported reverse iteration.
    for (auto code_point : preceding_view) {
        if (code_point_has_property(code_point, Property::Soft_Dotted)) {
            found_soft_dotted_code_point = true;
            continue;
        }

        auto combining_class = canonical_combining_class(code_point);
        if (combining_class == 0 || combining_class == 230)
            found_soft_dotted_code_point = false;
    }

    return found_soft_dotted_code_point;
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

static bool is_followed_by_combining_class_above(Utf8View const& string, size_t index, size_t byte_length)
{
    // C is followed by a character of combining class 230 (Above) with no intervening character of combining class 0 or 230 (Above).
    auto following_view = ((index + byte_length) < string.byte_length())
        ? string.substring_view(index + byte_length)
        : Utf8View {};

    for (auto code_point : following_view) {
        u32 combining_class = canonical_combining_class(code_point);

        if (combining_class == 0)
            return false;
        if (combining_class == 230)
            return true;
    }

    return false;
}

static bool is_followed_by_combining_dot_above(Utf8View const& string, size_t index, size_t byte_length)
{
    // C is followed by combining dot above (U+0307). Any sequence of characters with a combining class that is neither 0 nor 230 may
    // intervene between the current character and the combining dot above.
    auto following_view = ((index + byte_length) < string.byte_length())
        ? string.substring_view(index + byte_length)
        : Utf8View {};

    for (auto code_point : following_view) {
        if (code_point == 0x307)
            return true;

        u32 combining_class = canonical_combining_class(code_point);

        if (combining_class == 0)
            return false;
        if (combining_class == 230)
            return false;
    }

    return false;
}

static SpecialCasing const* find_matching_special_case(u32 code_point, Utf8View const& string, Optional<StringView> locale, size_t index, size_t byte_length)
{
    auto requested_locale = Locale::None;

    if (locale.has_value()) {
        if (auto maybe_locale = locale_from_string(*locale); maybe_locale.has_value())
            requested_locale = *maybe_locale;
    }

    auto special_casings = special_case_mapping(code_point);

    for (auto const* special_casing : special_casings) {
        if (special_casing->locale != Locale::None && special_casing->locale != requested_locale)
            continue;

        switch (special_casing->condition) {
        case Condition::None:
            return special_casing;

        case Condition::AfterI:
            if (is_after_uppercase_i(string, index))
                return special_casing;
            break;

        case Condition::AfterSoftDotted:
            if (is_after_soft_dotted_code_point(string, index))
                return special_casing;
            break;

        case Condition::FinalSigma:
            if (is_final_code_point(string, index, byte_length))
                return special_casing;
            break;

        case Condition::MoreAbove:
            if (is_followed_by_combining_class_above(string, index, byte_length))
                return special_casing;
            break;

        case Condition::NotBeforeDot:
            if (!is_followed_by_combining_dot_above(string, index, byte_length))
                return special_casing;
            break;
        }
    }

    return nullptr;
}

#endif

ErrorOr<void> build_lowercase_string([[maybe_unused]] Utf8View code_points, [[maybe_unused]] StringBuilder& builder, [[maybe_unused]] Optional<StringView> const& locale)
{
#if ENABLE_UNICODE_DATA
    size_t index = 0;
    size_t byte_length = 0;

    for (auto it = code_points.begin(); it != code_points.end(); ++it, index += byte_length) {
        u32 code_point = *it;
        byte_length = it.underlying_code_point_length_in_bytes();

        auto const* special_casing = find_matching_special_case(code_point, code_points, locale, index, byte_length);
        if (!special_casing) {
            TRY(builder.try_append_code_point(to_unicode_lowercase(code_point)));
            continue;
        }

        for (size_t i = 0; i < special_casing->lowercase_mapping_size; ++i)
            TRY(builder.try_append_code_point(special_casing->lowercase_mapping[i]));
    }

    return {};
#else
    return Error::from_string_literal("Unicode data has been disabled");
#endif
}

ErrorOr<void> build_uppercase_string([[maybe_unused]] Utf8View code_points, [[maybe_unused]] StringBuilder& builder, [[maybe_unused]] Optional<StringView> const& locale)
{
#if ENABLE_UNICODE_DATA
    size_t index = 0;
    size_t byte_length = 0;

    for (auto it = code_points.begin(); it != code_points.end(); ++it, index += byte_length) {
        u32 code_point = *it;
        byte_length = it.underlying_code_point_length_in_bytes();

        auto const* special_casing = find_matching_special_case(code_point, code_points, locale, index, byte_length);
        if (!special_casing) {
            TRY(builder.try_append_code_point(to_unicode_uppercase(code_point)));
            continue;
        }

        for (size_t i = 0; i < special_casing->uppercase_mapping_size; ++i)
            TRY(builder.try_append_code_point(special_casing->uppercase_mapping[i]));
    }

    return {};
#else
    return Error::from_string_literal("Unicode data has been disabled");
#endif
}

}
