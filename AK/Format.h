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
#include <AK/GenericLexer.h>
#include <AK/StringView.h>

// FIXME: I would really love to merge the format_value and make_type_erased_parameters functions,
//        but the compiler creates weird error messages when I do that. Here is a small snippet that
//        reproduces the issue: https://godbolt.org/z/o55crs

namespace AK {

class TypeErasedFormatParams;
class FormatParser;
class FormatBuilder;

template<typename T, typename = void>
struct Formatter {
    using __no_formatter_defined = void;
};

constexpr size_t max_format_arguments = 256;

struct TypeErasedParameter {
    enum class Type {
        UInt8,
        UInt16,
        UInt32,
        UInt64,
        Int8,
        Int16,
        Int32,
        Int64,
        Custom
    };

    template<typename T>
    static Type get_type()
    {
        if (IsSame<T, u8>::value)
            return Type::UInt8;
        if (IsSame<T, u16>::value)
            return Type::UInt16;
        if (IsSame<T, u32>::value)
            return Type::UInt32;
        if (IsSame<T, u64>::value)
            return Type::UInt64;
        if (IsSame<T, i8>::value)
            return Type::Int8;
        if (IsSame<T, i16>::value)
            return Type::Int16;
        if (IsSame<T, i32>::value)
            return Type::Int32;
        if (IsSame<T, i64>::value)
            return Type::Int64;

        return Type::Custom;
    }

    const void* value;
    Type type;
    void (*formatter)(TypeErasedFormatParams&, FormatBuilder&, FormatParser&, const void* value);
};

class FormatParser : public GenericLexer {
public:
    struct FormatSpecifier {
        StringView flags;
        size_t index;
    };

    explicit FormatParser(StringView input);

    StringView consume_literal();
    bool consume_number(size_t& value);
    bool consume_specifier(FormatSpecifier& specifier);
    bool consume_replacement_field(size_t& index);
};

class FormatBuilder {
public:
    enum class Align {
        Default,
        Left,
        Center,
        Right,
    };
    enum class SignMode {
        OnlyIfNeeded,
        Always,
        Reserved,
        Default = OnlyIfNeeded,
    };

    explicit FormatBuilder(StringBuilder& builder)
        : m_builder(builder)
    {
    }

    void put_padding(char fill, size_t amount);

    void put_literal(StringView value);

    void put_string(
        StringView value,
        Align align = Align::Left,
        size_t min_width = 0,
        size_t max_width = NumericLimits<size_t>::max(),
        char fill = ' ');

    void put_u64(
        u64 value,
        u8 base = 10,
        bool prefix = false,
        bool upper_case = false,
        bool zero_pad = false,
        Align align = Align::Right,
        size_t min_width = 0,
        char fill = ' ',
        SignMode sign_mode = SignMode::OnlyIfNeeded,
        bool is_negative = false);

    void put_i64(
        i64 value,
        u8 base = 10,
        bool prefix = false,
        bool upper_case = false,
        bool zero_pad = false,
        Align align = Align::Right,
        size_t min_width = 0,
        char fill = ' ',
        SignMode sign_mode = SignMode::OnlyIfNeeded);

    const StringBuilder& builder() const { return m_builder; }
    StringBuilder& builder() { return m_builder; }

private:
    StringBuilder& m_builder;
};

class TypeErasedFormatParams {
public:
    Span<const TypeErasedParameter> parameters() const { return m_parameters; }

    void set_parameters(Span<const TypeErasedParameter> parameters) { m_parameters = parameters; }
    size_t take_next_index() { return m_next_index++; }

    size_t decode(size_t value, size_t default_value = 0);

private:
    Span<const TypeErasedParameter> m_parameters;
    size_t m_next_index { 0 };
};

template<typename T>
void __format_value(TypeErasedFormatParams& params, FormatBuilder& builder, FormatParser& parser, const void* value)
{
    Formatter<T> formatter;

    formatter.parse(params, parser);
    formatter.format(params, builder, *static_cast<const T*>(value));
}

template<typename... Parameters>
class VariadicFormatParams : public TypeErasedFormatParams {
public:
    static_assert(sizeof...(Parameters) <= max_format_arguments);

    explicit VariadicFormatParams(const Parameters&... parameters)
        : m_data({ TypeErasedParameter { &parameters, TypeErasedParameter::get_type<Parameters>(), __format_value<Parameters> }... })
    {
        this->set_parameters(m_data);
    }

private:
    Array<TypeErasedParameter, sizeof...(Parameters)> m_data;
};

// We use the same format for most types for consistency. This is taken directly from
// std::format. One difference is that we are not counting the width or sign towards the
// total width when calculating zero padding for numbers.
// https://en.cppreference.com/w/cpp/utility/format/formatter#Standard_format_specification
struct StandardFormatter {
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

    static constexpr size_t value_not_set = NumericLimits<size_t>::max();
    static constexpr size_t value_from_next_arg = NumericLimits<size_t>::max() - 1;
    static constexpr size_t value_from_arg = NumericLimits<size_t>::max() - max_format_arguments - 2;

    FormatBuilder::Align m_align = FormatBuilder::Align::Default;
    FormatBuilder::SignMode m_sign_mode = FormatBuilder::SignMode::OnlyIfNeeded;
    Mode m_mode = Mode::Default;
    bool m_alternative_form = false;
    char m_fill = ' ';
    bool m_zero_pad = false;
    size_t m_width = value_not_set;
    size_t m_precision = value_not_set;

    void parse(TypeErasedFormatParams&, FormatParser&);
};

template<typename T>
struct Formatter<T, typename EnableIf<IsIntegral<T>::value>::Type> : StandardFormatter {
    Formatter() { }
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }

    void format(TypeErasedFormatParams&, FormatBuilder&, T value);
};

template<>
struct Formatter<StringView> : StandardFormatter {
    Formatter() { }
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }

    void format(TypeErasedFormatParams&, FormatBuilder&, StringView value);
};
template<>
struct Formatter<const char*> : Formatter<StringView> {
    void format(TypeErasedFormatParams& params, FormatBuilder& builder, const char* value)
    {
        if (m_mode == Mode::Pointer) {
            Formatter<FlatPtr> formatter { *this };
            formatter.format(params, builder, reinterpret_cast<FlatPtr>(value));
        } else {
            Formatter<StringView>::format(params, builder, value);
        }
    }
};
template<>
struct Formatter<char*> : Formatter<const char*> {
};
template<size_t Size>
struct Formatter<char[Size]> : Formatter<const char*> {
};
template<>
struct Formatter<String> : Formatter<StringView> {
};
template<>
struct Formatter<FlyString> : Formatter<StringView> {
};

template<typename T>
struct Formatter<T*> : StandardFormatter {
    void format(TypeErasedFormatParams& params, FormatBuilder& builder, T* value)
    {
        if (m_mode == Mode::Default)
            m_mode = Mode::Pointer;

        Formatter<FlatPtr> formatter { *this };
        formatter.format(params, builder, reinterpret_cast<FlatPtr>(value));
    }
};

template<>
struct Formatter<char> : StandardFormatter {
    void format(TypeErasedFormatParams&, FormatBuilder&, char value);
};
template<>
struct Formatter<bool> : StandardFormatter {
    void format(TypeErasedFormatParams&, FormatBuilder&, bool value);
};

void vformat(StringBuilder& builder, StringView fmtstr, TypeErasedFormatParams);
void vformat(const LogStream& stream, StringView fmtstr, TypeErasedFormatParams);

#ifndef KERNEL
void vout(StringView fmtstr, TypeErasedFormatParams, bool newline = false);
void raw_out(StringView string);

// FIXME: Rename this function to 'out' when that name becomes avaliable.
template<typename... Parameters>
void new_out(StringView fmtstr, const Parameters&... parameters) { vout(fmtstr, VariadicFormatParams { parameters... }); }
template<typename... Parameters>
void outln(StringView fmtstr, const Parameters&... parameters) { vout(fmtstr, VariadicFormatParams { parameters... }, true); }
template<typename... Parameters>
void outln(const char* fmtstr, const Parameters&... parameters) { outln(StringView { fmtstr }, parameters...); }
inline void outln() { raw_out("\n"); }

void vwarn(StringView fmtstr, TypeErasedFormatParams, bool newline = false);
void raw_warn(StringView string);

// FIXME: Rename this function to 'warn' when that name becomes avaliable.
template<typename... Parameters>
void new_warn(StringView fmtstr, const Parameters&... parameters) { vwarn(fmtstr, VariadicFormatParams { parameters... }); }
template<typename... Parameters>
void warnln(StringView fmtstr, const Parameters&... parameters) { vwarn(fmtstr, VariadicFormatParams { parameters... }, true); }
template<typename... Parameters>
void warnln(const char* fmtstr, const Parameters&... parameters) { warnln(StringView { fmtstr }, parameters...); }
inline void warnln() { raw_out("\n"); }
#endif

void vdbgln(StringView fmtstr, TypeErasedFormatParams);

template<typename... Parameters>
void dbgln(StringView fmtstr, const Parameters&... parameters) { vdbgln(fmtstr, VariadicFormatParams { parameters... }); }
template<typename... Parameters>
void dbgln(const char* fmtstr, const Parameters&... parameters) { dbgln(StringView { fmtstr }, parameters...); }

template<typename T, typename = void>
struct HasFormatter : TrueType {
};
template<typename T>
struct HasFormatter<T, typename Formatter<T>::__no_formatter_defined> : FalseType {
};

template<typename T>
class FormatIfSupported {
public:
    explicit FormatIfSupported(const T& value)
        : m_value(value)
    {
    }

    const T& value() const { return m_value; }

private:
    const T& m_value;
};
template<typename T, bool Supported = false>
struct __FormatIfSupported : Formatter<StringView> {
    void format(TypeErasedFormatParams& params, FormatBuilder& builder, const FormatIfSupported<T>&)
    {
        Formatter<StringView>::format(params, builder, "?");
    }
};
template<typename T>
struct __FormatIfSupported<T, true> : Formatter<T> {
    void format(TypeErasedFormatParams& params, FormatBuilder& builder, const FormatIfSupported<T>& value)
    {
        Formatter<T>::format(params, builder, value.value());
    }
};
template<typename T>
struct Formatter<FormatIfSupported<T>> : __FormatIfSupported<T, HasFormatter<T>::value> {
};

} // namespace AK

#ifndef KERNEL
using AK::new_out;
using AK::outln;
using AK::raw_out;

using AK::new_warn;
using AK::raw_warn;
using AK::warnln;
#endif

using AK::dbgln;

using AK::FormatIfSupported;
