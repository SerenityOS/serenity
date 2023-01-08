/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringBuilder.h>
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

}
