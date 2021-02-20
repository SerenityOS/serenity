/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Fei Wu <f.eiwu@yahoo.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

bool matches(const StringView& str, const StringView& mask, CaseSensitivity = CaseSensitivity::CaseInsensitive, Vector<MaskSpan>* match_spans = nullptr);
template<typename T = int>
Optional<T> convert_to_int(const StringView&);
template<typename T = unsigned>
Optional<T> convert_to_uint(const StringView&);
template<typename T = unsigned>
Optional<T> convert_to_uint_from_hex(const StringView&);
bool equals_ignoring_case(const StringView&, const StringView&);
bool ends_with(const StringView& a, const StringView& b, CaseSensitivity);
bool starts_with(const StringView&, const StringView&, CaseSensitivity);
bool contains(const StringView&, const StringView&, CaseSensitivity);
bool is_whitespace(const StringView&);
StringView trim_whitespace(const StringView&, TrimMode mode);
Optional<size_t> find(const StringView& haystack, const StringView& needle);
String to_snakecase(const StringView&);

}

}

using AK::CaseSensitivity;
using AK::TrimMode;
