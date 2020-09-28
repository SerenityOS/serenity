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

struct TypeErasedParameter {
    const void* value;
    void (*formatter)(StringBuilder& builder, const void* value, StringView flags, Span<const TypeErasedParameter> parameters);
};

} // namespace AK

namespace AK::Detail::Format {

template<typename T>
void format_value(StringBuilder& builder, const void* value, StringView flags, AK::Span<const TypeErasedParameter> parameters)
{
    Formatter<T> formatter;

    formatter.parse(flags);
    formatter.format(builder, *static_cast<const T*>(value), parameters);
}

} // namespace AK::Detail::Format

namespace AK {

constexpr size_t max_format_arguments = 256;

// We use the same format for most types for consistency. This is taken directly from std::format.
// Not all valid options do anything yet.
// https://en.cppreference.com/w/cpp/utility/format/formatter#Standard_format_specification
struct StandardFormatter {
    enum class Align {
        Default,
        Left,
        Right,
        Center,
    };
    enum class Sign {
        NegativeOnly,
        PositiveAndNegative,
        ReserveSpace,
        Default = NegativeOnly
    };
    enum class Mode {
        Default,
        Binary,
        BinaryUppercase,
        Decimal,
        Octal,
        Hexadecimal,
        HexadecimalUppercase,
        Character,
        String,
        Pointer,
    };

    static constexpr size_t value_not_set = 0;
    static constexpr size_t value_from_arg = NumericLimits<size_t>::max() - max_format_arguments;

    Align m_align = Align::Default;
    Sign m_sign = Sign::NegativeOnly;
    Mode m_mode = Mode::Default;
    bool m_alternative_form = false;
    char m_fill = ' ';
    bool m_zero_pad = false;
    size_t m_width = value_not_set;
    size_t m_precision = value_not_set;

    void parse(StringView flags);
};

template<>
struct Formatter<StringView> : StandardFormatter {
    void format(StringBuilder& builder, StringView value, Span<const TypeErasedParameter>);
};
template<>
struct Formatter<const char*> : Formatter<StringView> {
};
template<>
struct Formatter<char*> : Formatter<StringView> {
};
template<size_t Size>
struct Formatter<char[Size]> : Formatter<StringView> {
};
template<>
struct Formatter<String> : Formatter<StringView> {
};

template<typename T>
struct Formatter<T, typename EnableIf<IsIntegral<T>::value>::Type> : StandardFormatter {
    void format(StringBuilder&, T value, Span<const TypeErasedParameter>);
};

template<typename... Parameters>
Array<TypeErasedParameter, sizeof...(Parameters)> make_type_erased_parameters(const Parameters&... parameters)
{
    static_assert(sizeof...(Parameters) <= max_format_arguments);
    return { TypeErasedParameter { &parameters, Detail::Format::format_value<Parameters> }... };
}

void vformat(StringBuilder& builder, StringView fmtstr, Span<const TypeErasedParameter>);
void vformat(const LogStream& stream, StringView fmtstr, Span<const TypeErasedParameter>);

} // namespace AK
