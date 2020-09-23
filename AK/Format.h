/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/Array.h>
#include <AK/StringView.h>

// FIXME: I would really love to merge the format_value and make_type_erased_parameters functions,
//        but the compiler creates weird error messages when I do that. Here is a small snippet that
//        reproduces the issue: https://godbolt.org/z/o55crs

namespace AK {

template<typename T, typename = void>
struct Formatter;

} // namespace AK

namespace AK::Detail::Format {

template<typename T>
bool format_value(StringBuilder& builder, const void* value, StringView flags)
{
    Formatter<T> formatter;

    if (!formatter.parse(flags))
        return false;

    formatter.format(builder, *static_cast<const T*>(value));
    return true;
}

} // namespace AK::Detail::Format

namespace AK {

struct TypeErasedParameter {
    const void* value;
    bool (*formatter)(StringBuilder& builder, const void* value, StringView flags);
};

template<>
struct Formatter<StringView> {
    bool parse(StringView flags);
    void format(StringBuilder& builder, StringView value);
};
template<size_t Size>
struct Formatter<char[Size]> : Formatter<StringView> {
};
template<>
struct Formatter<String> : Formatter<StringView> {
};

template<typename T>
struct Formatter<T, typename EnableIf<IsIntegral<T>::value>::Type> {
    bool parse(StringView flags);
    void format(StringBuilder&, T value);

    bool zero_pad { false };
    bool hexadecimal { false };
    size_t field_width { 0 };
};

template<typename... Parameters>
Array<TypeErasedParameter, sizeof...(Parameters)> make_type_erased_parameters(const Parameters&... parameters)
{
    return { TypeErasedParameter { &parameters, Detail::Format::format_value<Parameters> }... };
}

void vformat(StringBuilder& builder, StringView fmtstr, Span<const TypeErasedParameter>, size_t argument_index = 0);

} // namespace AK
