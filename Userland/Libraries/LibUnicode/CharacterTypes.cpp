/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Platform.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <AK/Utf8View.h>
#include <LibUnicode/CharacterTypes.h>

#if ENABLE_UNICODE_DATA
#    include <LibUnicode/UnicodeData.h>
#endif

// For details on the algorithms used here, see Section 3.13 Default Case Algorithms
// https://www.unicode.org/versions/Unicode13.0.0/ch03.pdf

namespace Unicode {

#if ENABLE_UNICODE_DATA

static bool has_property(UnicodeData const& unicode_data, Property property)
{
    return (unicode_data.properties & property) == property;
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
        auto unicode_data = Detail::unicode_data_for_code_point(code_point);
        if (!unicode_data.has_value())
            return false;

        bool is_cased = has_property(*unicode_data, Property::Cased);
        bool is_case_ignorable = has_property(*unicode_data, Property::Case_Ignorable);

        if (is_cased && !is_case_ignorable)
            ++cased_letter_count;
        else if (!is_case_ignorable)
            cased_letter_count = 0;
    }

    if (cased_letter_count == 0)
        return false;

    for (auto code_point : following_view) {
        auto unicode_data = Detail::unicode_data_for_code_point(code_point);
        if (!unicode_data.has_value())
            return false;

        bool is_cased = has_property(*unicode_data, Property::Cased);
        bool is_case_ignorable = has_property(*unicode_data, Property::Case_Ignorable);

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
    return AK::to_ascii_lowercase(code_point);
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
    if (property == Property::Any)
        return is_unicode(code_point);

    auto unicode_data = Detail::unicode_data_for_code_point(code_point);
    if (!unicode_data.has_value())
        return false;

    return has_property(*unicode_data, property);
#else
    return false;
#endif
}

}
