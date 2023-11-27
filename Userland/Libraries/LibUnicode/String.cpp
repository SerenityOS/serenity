/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibUnicode/CharacterTypes.h>
#include <LibUnicode/UnicodeUtils.h>

// This file contains definitions of AK::String methods which require UCD data.

namespace AK {

ErrorOr<String> String::to_lowercase(Optional<StringView> const& locale) const
{
    StringBuilder builder;
    TRY(Unicode::Detail::build_lowercase_string(code_points(), builder, locale));
    return builder.to_string();
}

ErrorOr<String> String::to_uppercase(Optional<StringView> const& locale) const
{
    StringBuilder builder;
    TRY(Unicode::Detail::build_uppercase_string(code_points(), builder, locale));
    return builder.to_string();
}

ErrorOr<String> String::to_titlecase(Optional<StringView> const& locale, TrailingCodePointTransformation trailing_code_point_transformation) const
{
    StringBuilder builder;
    TRY(Unicode::Detail::build_titlecase_string(code_points(), builder, locale, trailing_code_point_transformation));
    return builder.to_string();
}

ErrorOr<String> String::to_casefold() const
{
    StringBuilder builder;
    TRY(Unicode::Detail::build_casefold_string(code_points(), builder));
    return builder.to_string();
}

bool String::equals_ignoring_case(String const& other) const
{
    return Unicode::equals_ignoring_case(code_points(), other.code_points());
}

}
