/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>

namespace AK {

enum class CaseSensitivity {
    CaseInsensitive,
    CaseSensitive,
};

enum class TrimMode {
    Left,
    Right,
    Both
};

enum class TrimWhitespace {
    Yes,
    No,
};

struct MaskSpan {
    size_t start;
    size_t length;

    bool operator==(MaskSpan const& other) const
    {
        return start == other.start && length == other.length;
    }
    bool operator!=(MaskSpan const& other) const
    {
        return !(*this == other);
    }
};

namespace StringUtils {

bool matches(StringView const& str, StringView const& mask, CaseSensitivity = CaseSensitivity::CaseInsensitive, Vector<MaskSpan>* match_spans = nullptr);
template<typename T = int>
Optional<T> convert_to_int(StringView const&, TrimWhitespace = TrimWhitespace::Yes);
template<typename T = unsigned>
Optional<T> convert_to_uint(StringView const&, TrimWhitespace = TrimWhitespace::Yes);
template<typename T = unsigned>
Optional<T> convert_to_uint_from_hex(StringView const&, TrimWhitespace = TrimWhitespace::Yes);
bool equals_ignoring_case(StringView const&, StringView const&);
bool ends_with(StringView const& a, StringView const& b, CaseSensitivity);
bool starts_with(StringView const&, StringView const&, CaseSensitivity);
bool contains(StringView const&, StringView const&, CaseSensitivity);
bool is_whitespace(StringView const&);
StringView trim(StringView const& string, StringView const& characters, TrimMode mode);
StringView trim_whitespace(StringView const& string, TrimMode mode);

Optional<size_t> find(StringView const& haystack, char needle, size_t start = 0);
Optional<size_t> find(StringView const& haystack, StringView const& needle, size_t start = 0);
Optional<size_t> find_last(StringView const& haystack, char needle);
Vector<size_t> find_all(StringView const& haystack, StringView const& needle);
enum class SearchDirection {
    Forward,
    Backward
};
Optional<size_t> find_any_of(StringView const& haystack, StringView const& needles, SearchDirection);

String to_snakecase(StringView const&);
String to_titlecase(StringView const&);

}

}

using AK::CaseSensitivity;
using AK::TrimMode;
using AK::TrimWhitespace;
