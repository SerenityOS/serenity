/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CheckedFormatString.h>

#include <AK/AllOf.h>
#include <AK/AnyOf.h>
#include <AK/Array.h>
#include <AK/Error.h>
#include <AK/Forward.h>
#include <AK/Optional.h>
#include <AK/StringView.h>

#ifndef KERNEL
#    include <stdio.h>
#    include <string.h>
#endif

namespace AK {

class TypeErasedFormatParams;
class FormatParser;
class FormatBuilder;

template<typename T, typename = void>
struct Formatter {
    using __no_formatter_defined = void;
};

enum AllowDebugOnlyFormatters {
    No,
    Yes
};

template<typename T, typename = void>
inline constexpr bool HasFormatter = true;

template<typename T>
inline constexpr bool HasFormatter<T, typename Formatter<T>::__no_formatter_defined> = false;

template<typename Formatter>
inline constexpr bool is_debug_only_formatter()
{
    constexpr bool has_is_debug_only = requires(Formatter const& formatter) { formatter.is_debug_only(); };
    if constexpr (has_is_debug_only)
        return Formatter::is_debug_only();
    return false;
}

template<typename T>
concept Formattable = HasFormatter<T>;

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

    template<size_t size, bool is_unsigned>
    static consteval Type get_type_from_size()
    {
        if constexpr (is_unsigned) {
            if constexpr (size == 1)
                return Type::UInt8;
            if constexpr (size == 2)
                return Type::UInt16;
            if constexpr (size == 4)
                return Type::UInt32;
            if constexpr (size == 8)
                return Type::UInt64;
        } else {
            if constexpr (size == 1)
                return Type::Int8;
            if constexpr (size == 2)
                return Type::Int16;
            if constexpr (size == 4)
                return Type::Int32;
            if constexpr (size == 8)
                return Type::Int64;
        }

        VERIFY_NOT_REACHED();
    }

    template<typename T>
    static consteval Type get_type()
    {
        if constexpr (IsIntegral<T>)
            return get_type_from_size<sizeof(T), IsUnsigned<T>>();
        else
            return Type::Custom;
    }

    template<typename Visitor>
    constexpr auto visit(Visitor&& visitor) const
    {
        switch (type) {
        case TypeErasedParameter::Type::UInt8:
            return visitor(*static_cast<u8 const*>(value));
        case TypeErasedParameter::Type::UInt16:
            return visitor(*static_cast<u16 const*>(value));
        case TypeErasedParameter::Type::UInt32:
            return visitor(*static_cast<u32 const*>(value));
        case TypeErasedParameter::Type::UInt64:
            return visitor(*static_cast<u64 const*>(value));
        case TypeErasedParameter::Type::Int8:
            return visitor(*static_cast<i8 const*>(value));
        case TypeErasedParameter::Type::Int16:
            return visitor(*static_cast<i16 const*>(value));
        case TypeErasedParameter::Type::Int32:
            return visitor(*static_cast<i32 const*>(value));
        case TypeErasedParameter::Type::Int64:
            return visitor(*static_cast<i64 const*>(value));
        default:
            TODO();
        }
    }

    constexpr size_t to_size() const
    {
        return visit([]<typename T>(T value) {
            if constexpr (sizeof(T) > sizeof(size_t))
                VERIFY(value < NumericLimits<size_t>::max());
            if constexpr (IsSigned<T>)
                VERIFY(value >= 0);
            return static_cast<size_t>(value);
        });
    }

    // FIXME: Getters and setters.

    void const* value;
    Type type;
    ErrorOr<void> (*formatter)(TypeErasedFormatParams&, FormatBuilder&, FormatParser&, void const* value);
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

    enum class RealNumberDisplayMode {
        FixedPoint,
        General,
        Default = General,
    };

    explicit FormatBuilder(StringBuilder& builder)
        : m_builder(builder)
    {
    }

    ErrorOr<void> put_padding(char fill, size_t amount);

    ErrorOr<void> put_literal(StringView value);

    ErrorOr<void> put_string(
        StringView value,
        Align align = Align::Left,
        size_t min_width = 0,
        size_t max_width = NumericLimits<size_t>::max(),
        char fill = ' ');

    ErrorOr<void> put_u64(
        u64 value,
        u8 base = 10,
        bool prefix = false,
        bool upper_case = false,
        bool zero_pad = false,
        bool use_separator = false,
        Align align = Align::Right,
        size_t min_width = 0,
        char fill = ' ',
        SignMode sign_mode = SignMode::OnlyIfNeeded,
        bool is_negative = false);

    ErrorOr<void> put_i64(
        i64 value,
        u8 base = 10,
        bool prefix = false,
        bool upper_case = false,
        bool zero_pad = false,
        bool use_separator = false,
        Align align = Align::Right,
        size_t min_width = 0,
        char fill = ' ',
        SignMode sign_mode = SignMode::OnlyIfNeeded);

    ErrorOr<void> put_fixed_point(
        bool is_negative,
        i64 integer_value,
        u64 fraction_value,
        u64 fraction_one,
        u8 base = 10,
        bool upper_case = false,
        bool zero_pad = false,
        bool use_separator = false,
        Align align = Align::Right,
        size_t min_width = 0,
        size_t precision = 6,
        char fill = ' ',
        SignMode sign_mode = SignMode::OnlyIfNeeded,
        RealNumberDisplayMode = RealNumberDisplayMode::Default);

#ifndef KERNEL
    ErrorOr<void> put_f80(
        long double value,
        u8 base = 10,
        bool upper_case = false,
        bool use_separator = false,
        Align align = Align::Right,
        size_t min_width = 0,
        size_t precision = 6,
        char fill = ' ',
        SignMode sign_mode = SignMode::OnlyIfNeeded,
        RealNumberDisplayMode = RealNumberDisplayMode::Default);

    ErrorOr<void> put_f64(
        double value,
        u8 base = 10,
        bool upper_case = false,
        bool zero_pad = false,
        bool use_separator = false,
        Align align = Align::Right,
        size_t min_width = 0,
        size_t precision = 6,
        char fill = ' ',
        SignMode sign_mode = SignMode::OnlyIfNeeded,
        RealNumberDisplayMode = RealNumberDisplayMode::Default);
#endif

    ErrorOr<void> put_hexdump(
        ReadonlyBytes,
        size_t width,
        char fill = ' ');

    StringBuilder const& builder() const
    {
        return m_builder;
    }
    StringBuilder& builder() { return m_builder; }

private:
    StringBuilder& m_builder;
};

class TypeErasedFormatParams {
public:
    ReadonlySpan<TypeErasedParameter> parameters() const { return m_parameters; }

    void set_parameters(ReadonlySpan<TypeErasedParameter> parameters) { m_parameters = parameters; }
    size_t take_next_index() { return m_next_index++; }

private:
    ReadonlySpan<TypeErasedParameter> m_parameters;
    size_t m_next_index { 0 };
};

template<typename T>
ErrorOr<void> __format_value(TypeErasedFormatParams& params, FormatBuilder& builder, FormatParser& parser, void const* value)
{
    Formatter<T> formatter;

    formatter.parse(params, parser);
    return formatter.format(builder, *static_cast<T const*>(value));
}

template<AllowDebugOnlyFormatters allow_debug_formatters, typename... Parameters>
class VariadicFormatParams : public TypeErasedFormatParams {
public:
    static_assert(sizeof...(Parameters) <= max_format_arguments);

    explicit VariadicFormatParams(Parameters const&... parameters)
        : m_data({ TypeErasedParameter { &parameters, TypeErasedParameter::get_type<Parameters>(), __format_value<Parameters> }... })
    {
        constexpr bool any_debug_formatters = (is_debug_only_formatter<Formatter<Parameters>>() || ...);
        static_assert(!any_debug_formatters || allow_debug_formatters == AllowDebugOnlyFormatters::Yes,
            "You are attempting to use a debug-only formatter outside of a debug log! Maybe one of your format values is an ErrorOr<T>?");
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
        FixedPoint,
        Hexfloat,
        HexfloatUppercase,
        HexDump,
    };

    FormatBuilder::Align m_align = FormatBuilder::Align::Default;
    FormatBuilder::SignMode m_sign_mode = FormatBuilder::SignMode::OnlyIfNeeded;
    Mode m_mode = Mode::Default;
    bool m_alternative_form = false;
    bool m_use_separator = false;
    char m_fill = ' ';
    bool m_zero_pad = false;
    Optional<size_t> m_width;
    Optional<size_t> m_precision;

    void parse(TypeErasedFormatParams&, FormatParser&);
};

template<Integral T>
struct Formatter<T> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(move(formatter))
    {
    }

    ErrorOr<void> format(FormatBuilder&, T);
};

template<>
struct Formatter<StringView> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(move(formatter))
    {
    }

    ErrorOr<void> format(FormatBuilder&, StringView);
};

template<typename T>
requires(HasFormatter<T>)
struct Formatter<ReadonlySpan<T>> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(move(formatter))
    {
    }

    ErrorOr<void> format(FormatBuilder& builder, ReadonlySpan<T> value)
    {
        if (m_mode == Mode::Pointer) {
            Formatter<FlatPtr> formatter { *this };
            TRY(formatter.format(builder, reinterpret_cast<FlatPtr>(value.data())));
            return {};
        }

        if (m_sign_mode != FormatBuilder::SignMode::Default)
            VERIFY_NOT_REACHED();
        if (m_alternative_form)
            VERIFY_NOT_REACHED();
        if (m_zero_pad)
            VERIFY_NOT_REACHED();
        if (m_mode != Mode::Default)
            VERIFY_NOT_REACHED();
        if (m_width.has_value() && m_precision.has_value())
            VERIFY_NOT_REACHED();

        m_width = m_width.value_or(0);
        m_precision = m_precision.value_or(NumericLimits<size_t>::max());

        Formatter<T> content_fmt;
        TRY(builder.put_literal("[ "sv));
        bool first = true;
        for (auto& content : value) {
            if (!first) {
                TRY(builder.put_literal(", "sv));
                content_fmt = Formatter<T> {};
            }
            first = false;
            TRY(content_fmt.format(builder, content));
        }
        TRY(builder.put_literal(" ]"sv));
        return {};
    }
};

template<typename T>
requires(HasFormatter<T>)
struct Formatter<Span<T>> : Formatter<ReadonlySpan<T>> {
    ErrorOr<void> format(FormatBuilder& builder, Span<T> value)
    {
        return Formatter<ReadonlySpan<T>>::format(builder, value);
    }
};

template<typename T, size_t inline_capacity>
requires(HasFormatter<T>)
struct Formatter<Vector<T, inline_capacity>> : Formatter<ReadonlySpan<T>> {
    ErrorOr<void> format(FormatBuilder& builder, Vector<T, inline_capacity> const& value)
    {
        return Formatter<ReadonlySpan<T>>::format(builder, value.span());
    }
};

template<>
struct Formatter<ReadonlyBytes> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, ReadonlyBytes value)
    {
        if (m_mode == Mode::Pointer) {
            Formatter<FlatPtr> formatter { *this };
            return formatter.format(builder, reinterpret_cast<FlatPtr>(value.data()));
        }
        if (m_mode == Mode::Default || m_mode == Mode::HexDump) {
            m_mode = Mode::HexDump;
            return Formatter<StringView>::format(builder, value);
        }
        return Formatter<StringView>::format(builder, value);
    }
};

template<>
struct Formatter<Bytes> : Formatter<ReadonlyBytes> {
};

// FIXME: Printing raw char pointers is inherently dangerous. Remove this and
//        its users and prefer StringView over it.
template<>
struct Formatter<char const*> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, char const* value)
    {
        if (m_mode == Mode::Pointer) {
            Formatter<FlatPtr> formatter { *this };
            return formatter.format(builder, reinterpret_cast<FlatPtr>(value));
        }

        return Formatter<StringView>::format(builder, value != nullptr ? StringView { value, __builtin_strlen(value) } : "(null)"sv);
    }
};
template<>
struct Formatter<char*> : Formatter<char const*> {
};
template<size_t Size>
struct Formatter<char[Size]> : Formatter<char const*> {
};
template<size_t Size>
struct Formatter<unsigned char[Size]> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, unsigned char const* value)
    {
        if (m_mode == Mode::Pointer) {
            Formatter<FlatPtr> formatter { *this };
            return formatter.format(builder, reinterpret_cast<FlatPtr>(value));
        }
        return Formatter<StringView>::format(builder, { value, Size });
    }
};
template<>
struct Formatter<DeprecatedString> : Formatter<StringView> {
};
template<>
struct Formatter<DeprecatedFlyString> : Formatter<StringView> {
};

template<typename T>
struct Formatter<T*> : StandardFormatter {
    ErrorOr<void> format(FormatBuilder& builder, T* value)
    {
        if (m_mode == Mode::Default)
            m_mode = Mode::Pointer;

        Formatter<FlatPtr> formatter { *this };
        return formatter.format(builder, reinterpret_cast<FlatPtr>(value));
    }
};

template<>
struct Formatter<char> : StandardFormatter {
    ErrorOr<void> format(FormatBuilder&, char);
};
template<>
struct Formatter<wchar_t> : StandardFormatter {
    ErrorOr<void> format(FormatBuilder& builder, wchar_t);
};
template<>
struct Formatter<bool> : StandardFormatter {
    ErrorOr<void> format(FormatBuilder&, bool);
};

#ifndef KERNEL
template<>
struct Formatter<float> : StandardFormatter {
    ErrorOr<void> format(FormatBuilder&, float value);
};
template<>
struct Formatter<double> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }

    ErrorOr<void> format(FormatBuilder&, double);
};

template<>
struct Formatter<long double> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }

    ErrorOr<void> format(FormatBuilder&, long double value);
};
#endif

template<>
struct Formatter<nullptr_t> : Formatter<FlatPtr> {
    ErrorOr<void> format(FormatBuilder& builder, nullptr_t)
    {
        if (m_mode == Mode::Default)
            m_mode = Mode::Pointer;

        return Formatter<FlatPtr>::format(builder, 0);
    }
};

ErrorOr<void> vformat(StringBuilder&, StringView fmtstr, TypeErasedFormatParams&);

#ifndef KERNEL
void vout(FILE*, StringView fmtstr, TypeErasedFormatParams&, bool newline = false);

template<typename... Parameters>
void out(FILE* file, CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
{
    VariadicFormatParams<AllowDebugOnlyFormatters::Yes, Parameters...> variadic_format_params { parameters... };
    vout(file, fmtstr.view(), variadic_format_params);
}

template<typename... Parameters>
void outln(FILE* file, CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
{
    VariadicFormatParams<AllowDebugOnlyFormatters::Yes, Parameters...> variadic_format_params { parameters... };
    vout(file, fmtstr.view(), variadic_format_params, true);
}

inline void outln(FILE* file) { fputc('\n', file); }

template<typename... Parameters>
void out(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters) { out(stdout, move(fmtstr), parameters...); }

template<typename... Parameters>
void outln(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters) { outln(stdout, move(fmtstr), parameters...); }

inline void outln() { outln(stdout); }

#    define outln_if(flag, fmt, ...)       \
        do {                               \
            if constexpr (flag)            \
                outln(fmt, ##__VA_ARGS__); \
        } while (0)

template<typename... Parameters>
void warn(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
{
    out(stderr, move(fmtstr), parameters...);
}

template<typename... Parameters>
void warnln(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters) { outln(stderr, move(fmtstr), parameters...); }

inline void warnln() { outln(stderr); }

#    define warnln_if(flag, fmt, ...)       \
        do {                                \
            if constexpr (flag)             \
                warnln(fmt, ##__VA_ARGS__); \
        } while (0)

#endif

void vdbgln(StringView fmtstr, TypeErasedFormatParams&);

template<typename... Parameters>
void dbgln(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
{
    VariadicFormatParams<AllowDebugOnlyFormatters::Yes, Parameters...> variadic_format_params { parameters... };
    vdbgln(fmtstr.view(), variadic_format_params);
}

inline void dbgln() { dbgln(""); }

void set_debug_enabled(bool);

#ifdef KERNEL
void vdmesgln(StringView fmtstr, TypeErasedFormatParams&);

template<typename... Parameters>
void dmesgln(CheckedFormatString<Parameters...>&& fmt, Parameters const&... parameters)
{
    VariadicFormatParams<AllowDebugOnlyFormatters::Yes, Parameters...> variadic_format_params { parameters... };
    vdmesgln(fmt.view(), variadic_format_params);
}

void v_critical_dmesgln(StringView fmtstr, TypeErasedFormatParams&);

// be very careful to not cause any allocations here, since we could be in
// a very unstable situation
template<typename... Parameters>
void critical_dmesgln(CheckedFormatString<Parameters...>&& fmt, Parameters const&... parameters)
{
    VariadicFormatParams<AllowDebugOnlyFormatters::Yes, Parameters...> variadic_format_params { parameters... };
    v_critical_dmesgln(fmt.view(), variadic_format_params);
}
#endif

template<typename T>
class FormatIfSupported {
public:
    explicit FormatIfSupported(T const& value)
        : m_value(value)
    {
    }

    T const& value() const { return m_value; }

private:
    T const& m_value;
};
template<typename T, bool Supported = false>
struct __FormatIfSupported : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, FormatIfSupported<T> const&)
    {
        return Formatter<StringView>::format(builder, "?"sv);
    }
};
template<typename T>
struct __FormatIfSupported<T, true> : Formatter<T> {
    ErrorOr<void> format(FormatBuilder& builder, FormatIfSupported<T> const& value)
    {
        return Formatter<T>::format(builder, value.value());
    }
};
template<typename T>
struct Formatter<FormatIfSupported<T>> : __FormatIfSupported<T, HasFormatter<T>> {
};

// This is a helper class, the idea is that if you want to implement a formatter you can inherit
// from this class to "break down" the formatting.
struct FormatString {
};
template<>
struct Formatter<FormatString> : Formatter<StringView> {
    template<typename... Parameters>
    ErrorOr<void> format(FormatBuilder& builder, StringView fmtstr, Parameters const&... parameters)
    {
        VariadicFormatParams<AllowDebugOnlyFormatters::No, Parameters...> variadic_format_params { parameters... };
        return vformat(builder, fmtstr, variadic_format_params);
    }
    ErrorOr<void> vformat(FormatBuilder& builder, StringView fmtstr, TypeErasedFormatParams& params);
};

template<>
struct Formatter<Error> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Error const& error)
    {
        if (error.is_custom_error())
            return Formatter<FormatString>::format(builder, "{}"sv, error.custom_error_as_string());

#if defined(AK_OS_SERENITY) && defined(KERNEL)
        return Formatter<FormatString>::format(builder, "Error(errno={})"sv, error.code());
#else
        if (error.is_syscall())
            return Formatter<FormatString>::format(builder, "{}: {} (errno={})"sv, error.string_literal(), strerror(error.code()), error.code());
        if (error.is_errno())
            return Formatter<FormatString>::format(builder, "{} (errno={})"sv, strerror(error.code()), error.code());

        return Formatter<FormatString>::format(builder, "{}"sv, error.string_literal());
#endif
    }
};

template<typename T, typename ErrorType>
struct Formatter<ErrorOr<T, ErrorType>> : Formatter<FormatString> {
    static constexpr bool is_debug_only() { return true; }

    ErrorOr<void> format(FormatBuilder& builder, ErrorOr<T, ErrorType> const& error_or)
    {
        if (error_or.is_error())
            return Formatter<FormatString>::format(builder, "{}"sv, error_or.error());
        return Formatter<FormatString>::format(builder, "{{{}}}"sv, error_or.value());
    }
};

template<typename T>
struct Formatter<Optional<T>> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Optional<T> const& optional)
    {
        if (optional.has_value())
            return Formatter<FormatString>::format(builder, "{}"sv, *optional);
        return builder.put_literal("None"sv);
    }
};

} // namespace AK

#if USING_AK_GLOBALLY
#    ifdef KERNEL
using AK::critical_dmesgln;
using AK::dmesgln;
#    else
using AK::out;
using AK::outln;

using AK::warn;
using AK::warnln;
#    endif

using AK::dbgln;

using AK::CheckedFormatString;
using AK::FormatIfSupported;
using AK::FormatString;

#    define dbgln_if(flag, fmt, ...)       \
        do {                               \
            if constexpr (flag)            \
                dbgln(fmt, ##__VA_ARGS__); \
        } while (0)

#endif
