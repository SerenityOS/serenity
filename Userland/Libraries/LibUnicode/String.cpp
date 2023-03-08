/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
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

ErrorOr<String> String::to_titlecase(Optional<StringView> const& locale) const
{
    StringBuilder builder;
    TRY(Unicode::Detail::build_titlecase_string(code_points(), builder, locale));
    return builder.to_string();
}

ErrorOr<String> String::to_casefold() const
{
    StringBuilder builder;
    TRY(Unicode::Detail::build_casefold_string(code_points(), builder));
    return builder.to_string();
}

class CasefoldStringComparator {
public:
    explicit CasefoldStringComparator(Utf8View string)
        : m_string(string)
        , m_it(m_string.begin())
    {
    }

    bool has_more_data() const
    {
        return !m_casefolded_code_points.is_empty() || (m_it != m_string.end());
    }

    u32 next_code_point()
    {
        VERIFY(has_more_data());

        if (m_casefolded_code_points.is_empty()) {
            m_current_code_point = *m_it;
            ++m_it;

            m_casefolded_code_points = Unicode::Detail::casefold_code_point(m_current_code_point);
            VERIFY(!m_casefolded_code_points.is_empty()); // Must at least contain the provided code point.
        }

        auto code_point = m_casefolded_code_points[0];
        m_casefolded_code_points = m_casefolded_code_points.substring_view(1);

        return code_point;
    }

private:
    Utf8View m_string;
    Utf8CodePointIterator m_it;

    u32 m_current_code_point { 0 };
    Utf32View m_casefolded_code_points;
};

// https://www.unicode.org/versions/Unicode15.0.0/ch03.pdf#G34145
bool String::equals_ignoring_case(String const& other) const
{
    // A string X is a caseless match for a string Y if and only if:
    //     toCasefold(X) = toCasefold(Y)

    CasefoldStringComparator lhs { code_points() };
    CasefoldStringComparator rhs { other.code_points() };

    while (lhs.has_more_data() && rhs.has_more_data()) {
        if (lhs.next_code_point() != rhs.next_code_point())
            return false;
    }

    return !lhs.has_more_data() && !rhs.has_more_data();
}

}
