/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Forward.h>

namespace AK {

namespace Detail {
template<Concepts::AnyString T, Concepts::AnyString U>
inline constexpr bool IsHashCompatible<T, U> = true;
}

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

    bool operator==(const MaskSpan& other) const
    {
        return start == other.start && length == other.length;
    }
    bool operator!=(const MaskSpan& other) const
    {
        return !(*this == other);
    }
};

namespace StringUtils {

bool matches(StringView str, StringView mask, CaseSensitivity = CaseSensitivity::CaseInsensitive, Vector<MaskSpan>* match_spans = nullptr);
template<typename T = int>
Optional<T> convert_to_int(StringView, TrimWhitespace = TrimWhitespace::Yes);
template<typename T = unsigned>
Optional<T> convert_to_uint(StringView, TrimWhitespace = TrimWhitespace::Yes);
template<typename T = unsigned>
Optional<T> convert_to_uint_from_hex(StringView, TrimWhitespace = TrimWhitespace::Yes);
template<typename T = unsigned>
Optional<T> convert_to_uint_from_octal(StringView, TrimWhitespace = TrimWhitespace::Yes);
bool equals_ignoring_case(StringView, StringView);
bool ends_with(StringView a, StringView b, CaseSensitivity);
bool starts_with(StringView, StringView, CaseSensitivity);
bool contains(StringView, StringView, CaseSensitivity);
bool is_whitespace(StringView);
StringView trim(StringView string, StringView characters, TrimMode mode);
StringView trim_whitespace(StringView string, TrimMode mode);

Optional<size_t> find(StringView haystack, char needle, size_t start = 0);
Optional<size_t> find(StringView haystack, StringView needle, size_t start = 0);
Optional<size_t> find_last(StringView haystack, char needle);
Vector<size_t> find_all(StringView haystack, StringView needle);
enum class SearchDirection {
    Forward,
    Backward
};
Optional<size_t> find_any_of(StringView haystack, StringView needles, SearchDirection);

String to_snakecase(StringView);
String to_titlecase(StringView);

String replace(StringView, StringView needle, StringView replacement, bool all_occurrences = false);
size_t count(StringView, StringView needle);

}

}

using AK::CaseSensitivity;
using AK::TrimMode;
using AK::TrimWhitespace;
