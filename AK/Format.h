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

#include <AK/AllOf.h>
#include <AK/AnyOf.h>
#include <AK/Array.h>
#include <AK/GenericLexer.h>
#include <AK/Optional.h>
#include <AK/StringView.h>

#ifndef KERNEL
#    include <stdio.h>
#endif

#ifndef DBGLN_NO_COMPILETIME_FORMAT_CHECK
// Note: Clang 12 adds support for CTAD, but still fails to build the dbgln() checks, so they're disabled altogether for now.
// See https://oss-fuzz-build-logs.storage.googleapis.com/log-79750138-f41e-4f39-8812-7c536f1d2e35.txt, for example.
#    if defined(__clang__)
#        define DBGLN_NO_COMPILETIME_FORMAT_CHECK
#    endif
#endif

#ifndef DBGLN_NO_COMPILETIME_FORMAT_CHECK
namespace {

template<size_t N>
consteval auto extract_used_argument_index(const char (&fmt)[N], size_t specifier_start_index, size_t specifier_end_index, size_t& next_implicit_argument_index)
{
    struct {
        size_t index_value { 0 };
        bool saw_explicit_index { false };
    } state;
    for (size_t i = specifier_start_index; i < specifier_end_index; ++i) {
        auto c = fmt[i];
        if (c > '9' || c < '0')
            break;

        state.index_value *= 10;
        state.index_value += c - '0';
        state.saw_explicit_index = true;
    }

    if (!state.saw_explicit_index)
        return next_implicit_argument_index++;

    return state.index_value;
}

// FIXME: We should rather parse these format strings at compile-time if possible.
template<size_t N>
consteval auto count_fmt_params(const char (&fmt)[N])
{
    struct {
        // FIXME: Switch to variable-sized storage whenever we can come up with one :)
        Array<size_t, 128> used_arguments { 0 };
        size_t total_used_argument_count { 0 };
        size_t next_implicit_argument_index { 0 };
        bool has_explicit_argument_references { false };

        size_t unclosed_braces { 0 };
        size_t extra_closed_braces { 0 };

        Array<size_t, 4> last_format_specifier_start { 0 };
        size_t total_used_last_format_specifier_start_count { 0 };

        StringLiteral<128> internal_error { { 0 } };
    } result;

    for (size_t i = 0; i < N; ++i) {
        auto ch = fmt[i];
        switch (ch) {
        case '{':
            if (i + 1 < N && fmt[i + 1] == '{') {
                ++i;
                continue;
            }

            // Note: There's no compile-time throw, so we have to abuse a compile-time string to store errors.
            if (result.total_used_last_format_specifier_start_count >= result.last_format_specifier_start.size() - 1)
                result.internal_error = "Format-String Checker internal error: Format specifier nested too deep";

            result.last_format_specifier_start[result.total_used_last_format_specifier_start_count++] = i + 1;

            ++result.unclosed_braces;
            break;
        case '}':
            if (i + 1 < N && fmt[i + 1] == '}') {
                ++i;
                continue;
            }
            if (result.unclosed_braces) {
                --result.unclosed_braces;

                if (result.total_used_last_format_specifier_start_count == 0)
                    result.internal_error = "Format-String Checker internal error: Expected location information";

                const auto specifier_start_index = result.last_format_specifier_start[--result.total_used_last_format_specifier_start_count];

                if (result.total_used_argument_count >= result.used_arguments.size())
                    result.internal_error = "Format-String Checker internal error: Too many format arguments in format string";

                auto used_argument_index = extract_used_argument_index<N>(fmt, specifier_start_index, i, result.next_implicit_argument_index);
                if (used_argument_index + 1 != result.next_implicit_argument_index)
                    result.has_explicit_argument_references = true;
                result.used_arguments[result.total_used_argument_count++] = used_argument_index;

            } else {
                ++result.extra_closed_braces;
            }
            break;
        default:
            continue;
        }
    }
    return result;
}
}

template<size_t N, StringLiteral<N> fmt, size_t param_count, auto check = count_fmt_params<N>(fmt.data)>
constexpr bool check_format_parameter_consistency()
{
    static_assert(check.internal_error.data[0] == 0, "Some internal error occured, try looking at the check function type for the error");
    static_assert(check.unclosed_braces == 0, "Extra unclosed braces in format string");
    static_assert(check.extra_closed_braces == 0, "Extra closing braces in format string");

    {
        constexpr auto begin = check.used_arguments.begin();
        constexpr auto end = check.used_arguments.begin() + check.total_used_argument_count;
        constexpr auto has_all_referenced_arguments = !AK::any_of(begin, end, [](auto& entry) { return entry >= param_count; });
        static_assert(has_all_referenced_arguments, "Format string references nonexistent parameter");
    }

    if constexpr (!check.has_explicit_argument_references)
        static_assert(check.total_used_argument_count == param_count, "Format string does not reference all passed parameters");

    // Ensure that no passed parameter is ignored or otherwise not referenced in the format
    // As this check is generally pretty expensive, try to avoid it where it cannot fail.
    // We will only do this check if the format string has explicit argument refs
    // otherwise, the check above covers this check too, as implicit refs
    // monotonically increase, and cannot have 'gaps'.
    if constexpr (check.has_explicit_argument_references) {
        constexpr auto all_parameters = iota_array<size_t, param_count>(0);
        auto contains = [](auto begin, auto end, auto entry) {
            for (; begin != end; begin++) {
                if (*begin == entry)
                    return true;
            }

            return false;
        };
        constexpr auto references_all_arguments = AK::all_of(
            all_parameters.begin(),
            all_parameters.end(),
            [&](auto& entry) {
                return contains(
                    check.used_arguments.begin(),
                    check.used_arguments.begin() + check.total_used_argument_count,
                    entry);
            });
        static_assert(references_all_arguments, "Format string does not reference all passed parameters");
    }

    return true;
}

template<auto fmt, auto param_count>
concept ConsistentFormatParameters = check_format_parameter_consistency<fmt.size, fmt, param_count>();
#endif

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

    static Type get_type_from_size(size_t size, bool is_unsigned)
    {
        if (is_unsigned) {
            if (size == 1)
                return Type::UInt8;
            if (size == 2)
                return Type::UInt16;
            if (size == 4)
                return Type::UInt32;
            if (size == 8)
                return Type::UInt64;
        } else {
            if (size == 1)
                return Type::Int8;
            if (size == 2)
                return Type::Int16;
            if (size == 4)
                return Type::Int32;
            if (size == 8)
                return Type::Int64;
        }

        ASSERT_NOT_REACHED();
    }

    template<typename T>
    static Type get_type()
    {
        if (IsIntegral<T>::value)
            return get_type_from_size(sizeof(T), IsUnsigned<T>::value);

        return Type::Custom;
    }

    size_t to_size() const;

    // FIXME: Getters and setters.

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

#ifndef KERNEL
    void put_f64(
        double value,
        u8 base = 10,
        bool upper_case = false,
        Align align = Align::Right,
        size_t min_width = 0,
        size_t precision = 6,
        char fill = ' ',
        SignMode sign_mode = SignMode::OnlyIfNeeded);
#endif

    const StringBuilder& builder() const
    {
        return m_builder;
    }
    StringBuilder& builder() { return m_builder; }

private:
    StringBuilder& m_builder;
};

class TypeErasedFormatParams {
public:
    Span<const TypeErasedParameter> parameters() const { return m_parameters; }

    void set_parameters(Span<const TypeErasedParameter> parameters) { m_parameters = parameters; }
    size_t take_next_index() { return m_next_index++; }

private:
    Span<const TypeErasedParameter> m_parameters;
    size_t m_next_index { 0 };
};

template<typename T>
void __format_value(TypeErasedFormatParams& params, FormatBuilder& builder, FormatParser& parser, const void* value)
{
    Formatter<T> formatter;

    formatter.parse(params, parser);
    formatter.format(builder, *static_cast<const T*>(value));
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
        Float,
        Hexfloat,
        HexfloatUppercase,
    };

    FormatBuilder::Align m_align = FormatBuilder::Align::Default;
    FormatBuilder::SignMode m_sign_mode = FormatBuilder::SignMode::OnlyIfNeeded;
    Mode m_mode = Mode::Default;
    bool m_alternative_form = false;
    char m_fill = ' ';
    bool m_zero_pad = false;
    Optional<size_t> m_width;
    Optional<size_t> m_precision;

    void parse(TypeErasedFormatParams&, FormatParser&);
};

template<typename T>
struct Formatter<T, typename EnableIf<IsIntegral<T>::value>::Type> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }

    void format(FormatBuilder&, T value);
};

template<>
struct Formatter<StringView> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }

    void format(FormatBuilder&, StringView value);
};
template<>
struct Formatter<const char*> : Formatter<StringView> {
    void format(FormatBuilder& builder, const char* value)
    {
        if (m_mode == Mode::Pointer) {
            Formatter<FlatPtr> formatter { *this };
            formatter.format(builder, reinterpret_cast<FlatPtr>(value));
        } else {
            Formatter<StringView>::format(builder, value);
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
    void format(FormatBuilder& builder, T* value)
    {
        if (m_mode == Mode::Default)
            m_mode = Mode::Pointer;

        Formatter<FlatPtr> formatter { *this };
        formatter.format(builder, reinterpret_cast<FlatPtr>(value));
    }
};

template<>
struct Formatter<char> : StandardFormatter {
    void format(FormatBuilder&, char value);
};
template<>
struct Formatter<bool> : StandardFormatter {
    void format(FormatBuilder&, bool value);
};

#ifndef KERNEL
template<>
struct Formatter<float> : StandardFormatter {
    void format(FormatBuilder&, float value);
};
template<>
struct Formatter<double> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }

    void format(FormatBuilder&, double value);
};
#endif

template<>
struct Formatter<std::nullptr_t> : Formatter<FlatPtr> {
    void format(FormatBuilder& builder, std::nullptr_t)
    {
        if (m_mode == Mode::Default)
            m_mode = Mode::Pointer;

        return Formatter<FlatPtr>::format(builder, 0);
    }
};

void vformat(StringBuilder&, StringView fmtstr, TypeErasedFormatParams);
void vformat(const LogStream& stream, StringView fmtstr, TypeErasedFormatParams);

#ifndef KERNEL
void vout(FILE*, StringView fmtstr, TypeErasedFormatParams, bool newline = false);

template<typename... Parameters>
void out(FILE* file, StringView fmtstr, const Parameters&... parameters) { vout(file, fmtstr, VariadicFormatParams { parameters... }); }
template<typename... Parameters>
void outln(FILE* file, StringView fmtstr, const Parameters&... parameters) { vout(file, fmtstr, VariadicFormatParams { parameters... }, true); }
template<typename... Parameters>
void outln(FILE* file, const char* fmtstr, const Parameters&... parameters) { vout(file, fmtstr, VariadicFormatParams { parameters... }, true); }
inline void outln(FILE* file) { fputc('\n', file); }

template<typename... Parameters>
void out(StringView fmtstr, const Parameters&... parameters) { out(stdout, fmtstr, parameters...); }
template<typename... Parameters>
void outln(StringView fmtstr, const Parameters&... parameters) { outln(stdout, fmtstr, parameters...); }
template<typename... Parameters>
void outln(const char* fmtstr, const Parameters&... parameters) { outln(stdout, fmtstr, parameters...); }
inline void outln() { outln(stdout); }

template<typename... Parameters>
void warn(StringView fmtstr, const Parameters&... parameters) { out(stderr, fmtstr, parameters...); }
template<typename... Parameters>
void warnln(StringView fmtstr, const Parameters&... parameters) { outln(stderr, fmtstr, parameters...); }
template<typename... Parameters>
void warnln(const char* fmtstr, const Parameters&... parameters) { outln(stderr, fmtstr, parameters...); }
inline void warnln() { outln(stderr); }
#endif

void vdbgln(StringView fmtstr, TypeErasedFormatParams);

#ifndef DBGLN_NO_COMPILETIME_FORMAT_CHECK
template<StringLiteral fmt, bool enabled = true, typename... Parameters>
void dbgln(const Parameters&... parameters) requires ConsistentFormatParameters<fmt, sizeof...(Parameters)>
{
    dbgln<enabled>(StringView { fmt.data }, parameters...);
}
#endif

template<bool enabled = true, typename... Parameters>
void dbgln(StringView fmtstr, const Parameters&... parameters)
{
    if constexpr (enabled)
        vdbgln(fmtstr, VariadicFormatParams { parameters... });
}
template<bool enabled = true, typename... Parameters>
void dbgln(const char* fmtstr, const Parameters&... parameters) { dbgln<enabled>(StringView { fmtstr }, parameters...); }

template<bool enabled = true>
void dbgln() { dbgln<enabled>(""); }

void set_debug_enabled(bool);

#ifdef KERNEL
void vdmesgln(StringView fmtstr, TypeErasedFormatParams);

template<typename... Parameters>
void dmesgln(StringView fmtstr, const Parameters&... parameters) { vdmesgln(fmtstr, VariadicFormatParams { parameters... }); }
template<typename... Parameters>
void dmesgln(const char* fmtstr, const Parameters&... parameters) { vdmesgln(fmtstr, VariadicFormatParams { parameters... }); }
inline void dmesgln() { dmesgln(""); }
#endif

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
    void format(FormatBuilder& builder, const FormatIfSupported<T>&)
    {
        Formatter<StringView>::format(builder, "?");
    }
};
template<typename T>
struct __FormatIfSupported<T, true> : Formatter<T> {
    void format(FormatBuilder& builder, const FormatIfSupported<T>& value)
    {
        Formatter<T>::format(builder, value.value());
    }
};
template<typename T>
struct Formatter<FormatIfSupported<T>> : __FormatIfSupported<T, HasFormatter<T>::value> {
};

// This is a helper class, the idea is that if you want to implement a formatter you can inherit
// from this class to "break down" the formatting.
struct FormatString {
};
template<>
struct Formatter<FormatString> : Formatter<String> {
    template<typename... Parameters>
    void format(FormatBuilder& builder, StringView fmtstr, const Parameters&... parameters)
    {
        vformat(builder, fmtstr, VariadicFormatParams { parameters... });
    }
    void vformat(FormatBuilder& builder, StringView fmtstr, TypeErasedFormatParams params);
};

} // namespace AK

#ifdef KERNEL
using AK::dmesgln;
#else
using AK::out;
using AK::outln;

using AK::warn;
using AK::warnln;
#endif

using AK::dbgln;

using AK::FormatIfSupported;
using AK::FormatString;

#ifdef DBGLN_NO_COMPILETIME_FORMAT_CHECK
#    define dbgln(fmt, ...) dbgln(fmt, ##__VA_ARGS__)
#    define dbgln_if(flag, fmt, ...) dbgln<flag>(fmt, ##__VA_ARGS__)
#else
#    define dbgln(fmt, ...) dbgln<fmt>(__VA_ARGS__)
#    define dbgln_if(flag, fmt, ...) dbgln<fmt, flag>(__VA_ARGS__)
#endif
