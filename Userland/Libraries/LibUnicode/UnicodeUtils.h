/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Forward.h>
#include <AK/Optional.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
#include <LibUnicode/Forward.h>

namespace Unicode::Detail {

ErrorOr<void> build_lowercase_string(Utf8View code_points, StringBuilder& builder, Optional<StringView> const& locale);
ErrorOr<void> build_uppercase_string(Utf8View code_points, StringBuilder& builder, Optional<StringView> const& locale);
ErrorOr<void> build_titlecase_string(Utf8View code_points, StringBuilder& builder, Optional<StringView> const& locale, TrailingCodePointTransformation trailing_code_point_transformation);
ErrorOr<void> build_casefold_string(Utf8View code_points, StringBuilder& builder);
Utf32View casefold_code_point(u32 const& code_point);

}
