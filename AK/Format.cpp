/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Format.h>
#include <AK/GenericLexer.h>
#include <AK/IntegralMath.h>
#include <AK/StringBuilder.h>
#include <AK/kstdio.h>

#if defined(AK_OS_SERENITY) && !defined(KERNEL)
#    include <serenity.h>
#endif

#ifdef KERNEL
#    include <Kernel/Process.h>
#    include <Kernel/Thread.h>
#else
#    include <math.h>
#    include <stdio.h>
#    include <string.h>
#endif

namespace AK {

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

namespace {

static constexpr size_t use_next_index = NumericLimits<size_t>::max();

// The worst case is that we have the largest 64-bit value formatted as binary number, this would take
// 65 bytes. Choosing a larger power of two won't hurt and is a bit of mitigation against out-of-bounds accesses.
static constexpr size_t convert_unsigned_to_string(u64 value, Array<u8, 128>& buffer, u8 base, bool upper_case)
{
    VERIFY(base >= 2 && base <= 16);

    constexpr char const* lowercase_lookup = "0123456789abcdef";
    constexpr char const* uppercase_lookup = "0123456789ABCDEF";

    if (value == 0) {
        buffer[0] = '0';
        return 1;
    }

    size_t used = 0;
    while (value > 0) {
        if (upper_case)
            buffer[used++] = uppercase_lookup[value % base];
        else
            buffer[used++] = lowercase_lookup[value % base];

        value /= base;
    }

    for (size_t i = 0; i < used / 2; ++i)
        swap(buffer[i], buffer[used - i - 1]);

    return used;
}

ErrorOr<void> vformat_impl(TypeErasedFormatParams& params, FormatBuilder& builder, FormatParser& parser)
{
    auto const literal = parser.consume_literal();
    TRY(builder.put_literal(literal));

    FormatParser::FormatSpecifier specifier;
    if (!parser.consume_specifier(specifier)) {
        VERIFY(parser.is_eof());
        return {};
    }

    if (specifier.index == use_next_index)
        specifier.index = params.take_next_index();

    auto& parameter = params.parameters().at(specifier.index);

    FormatParser argparser { specifier.flags };
    TRY(parameter.formatter(params, builder, argparser, parameter.value));
    TRY(vformat_impl(params, builder, parser));
    return {};
}

} // namespace AK::{anonymous}

FormatParser::FormatParser(StringView input)
    : GenericLexer(input)
{
}
StringView FormatParser::consume_literal()
{
    auto const begin = tell();

    while (!is_eof()) {
        if (consume_specific("{{"))
            continue;

        if (consume_specific("}}"))
            continue;

        if (next_is(is_any_of("{}"sv)))
            return m_input.substring_view(begin, tell() - begin);

        consume();
    }

    return m_input.substring_view(begin);
}
bool FormatParser::consume_number(size_t& value)
{
    value = 0;

    bool consumed_at_least_one = false;
    while (next_is(is_ascii_digit)) {
        value *= 10;
        value += parse_ascii_digit(consume());
        consumed_at_least_one = true;
    }

    return consumed_at_least_one;
}
bool FormatParser::consume_specifier(FormatSpecifier& specifier)
{
    VERIFY(!next_is('}'));

    if (!consume_specific('{'))
        return false;

    if (!consume_number(specifier.index))
        specifier.index = use_next_index;

    if (consume_specific(':')) {
        auto const begin = tell();

        size_t level = 1;
        while (level > 0) {
            VERIFY(!is_eof());

            if (consume_specific('{')) {
                ++level;
                continue;
            }

            if (consume_specific('}')) {
                --level;
                continue;
            }

            consume();
        }

        specifier.flags = m_input.substring_view(begin, tell() - begin - 1);
    } else {
        if (!consume_specific('}'))
            VERIFY_NOT_REACHED();

        specifier.flags = ""sv;
    }

    return true;
}
bool FormatParser::consume_replacement_field(size_t& index)
{
    if (!consume_specific('{'))
        return false;

    if (!consume_number(index))
        index = use_next_index;

    if (!consume_specific('}'))
        VERIFY_NOT_REACHED();

    return true;
}

ErrorOr<void> FormatBuilder::put_padding(char fill, size_t amount)
{
    for (size_t i = 0; i < amount; ++i)
        TRY(m_builder.try_append(fill));
    return {};
}
ErrorOr<void> FormatBuilder::put_literal(StringView value)
{
    for (size_t i = 0; i < value.length(); ++i) {
        TRY(m_builder.try_append(value[i]));
        if (value[i] == '{' || value[i] == '}')
            ++i;
    }
    return {};
}

ErrorOr<void> FormatBuilder::put_string(
    StringView value,
    Align align,
    size_t min_width,
    size_t max_width,
    char fill)
{
    auto const used_by_string = min(max_width, value.length());
    auto const used_by_padding = max(min_width, used_by_string) - used_by_string;

    if (used_by_string < value.length())
        value = value.substring_view(0, used_by_string);

    if (align == Align::Left || align == Align::Default) {
        TRY(m_builder.try_append(value));
        TRY(put_padding(fill, used_by_padding));
    } else if (align == Align::Center) {
        auto const used_by_left_padding = used_by_padding / 2;
        auto const used_by_right_padding = ceil_div<size_t, size_t>(used_by_padding, 2);

        TRY(put_padding(fill, used_by_left_padding));
        TRY(m_builder.try_append(value));
        TRY(put_padding(fill, used_by_right_padding));
    } else if (align == Align::Right) {
        TRY(put_padding(fill, used_by_padding));
        TRY(m_builder.try_append(value));
    }
    return {};
}

ErrorOr<void> FormatBuilder::put_u64(
    u64 value,
    u8 base,
    bool prefix,
    bool upper_case,
    bool zero_pad,
    Align align,
    size_t min_width,
    char fill,
    SignMode sign_mode,
    bool is_negative)
{
    if (align == Align::Default)
        align = Align::Right;

    Array<u8, 128> buffer;

    auto const used_by_digits = convert_unsigned_to_string(value, buffer, base, upper_case);

    size_t used_by_prefix = 0;
    if (align == Align::Right && zero_pad) {
        // We want String::formatted("{:#08x}", 32) to produce '0x00000020' instead of '0x000020'. This
        // behavior differs from both fmtlib and printf, but is more intuitive.
        used_by_prefix = 0;
    } else {
        if (is_negative || sign_mode != SignMode::OnlyIfNeeded)
            used_by_prefix += 1;

        if (prefix) {
            if (base == 8)
                used_by_prefix += 1;
            else if (base == 16)
                used_by_prefix += 2;
            else if (base == 2)
                used_by_prefix += 2;
        }
    }

    auto const used_by_field = used_by_prefix + used_by_digits;
    auto const used_by_padding = max(used_by_field, min_width) - used_by_field;

    auto const put_prefix = [&]() -> ErrorOr<void> {
        if (is_negative)
            TRY(m_builder.try_append('-'));
        else if (sign_mode == SignMode::Always)
            TRY(m_builder.try_append('+'));
        else if (sign_mode == SignMode::Reserved)
            TRY(m_builder.try_append(' '));

        if (prefix) {
            if (base == 2) {
                if (upper_case)
                    TRY(m_builder.try_append("0B"sv));
                else
                    TRY(m_builder.try_append("0b"sv));
            } else if (base == 8) {
                TRY(m_builder.try_append("0"sv));
            } else if (base == 16) {
                if (upper_case)
                    TRY(m_builder.try_append("0X"sv));
                else
                    TRY(m_builder.try_append("0x"sv));
            }
        }
        return {};
    };

    auto const put_digits = [&]() -> ErrorOr<void> {
        for (size_t i = 0; i < used_by_digits; ++i)
            TRY(m_builder.try_append(buffer[i]));
        return {};
    };

    if (align == Align::Left) {
        auto const used_by_right_padding = used_by_padding;

        TRY(put_prefix());
        TRY(put_digits());
        TRY(put_padding(fill, used_by_right_padding));
    } else if (align == Align::Center) {
        auto const used_by_left_padding = used_by_padding / 2;
        auto const used_by_right_padding = ceil_div<size_t, size_t>(used_by_padding, 2);

        TRY(put_padding(fill, used_by_left_padding));
        TRY(put_prefix());
        TRY(put_digits());
        TRY(put_padding(fill, used_by_right_padding));
    } else if (align == Align::Right) {
        auto const used_by_left_padding = used_by_padding;

        if (zero_pad) {
            TRY(put_prefix());
            TRY(put_padding('0', used_by_left_padding));
            TRY(put_digits());
        } else {
            TRY(put_padding(fill, used_by_left_padding));
            TRY(put_prefix());
            TRY(put_digits());
        }
    }
    return {};
}

ErrorOr<void> FormatBuilder::put_i64(
    i64 value,
    u8 base,
    bool prefix,
    bool upper_case,
    bool zero_pad,
    Align align,
    size_t min_width,
    char fill,
    SignMode sign_mode)
{
    auto const is_negative = value < 0;
    value = is_negative ? -value : value;

    TRY(put_u64(static_cast<u64>(value), base, prefix, upper_case, zero_pad, align, min_width, fill, sign_mode, is_negative));
    return {};
}

ErrorOr<void> FormatBuilder::put_fixed_point(
    i64 integer_value,
    u64 fraction_value,
    u64 fraction_one,
    u8 base,
    bool upper_case,
    bool zero_pad,
    Align align,
    size_t min_width,
    size_t precision,
    char fill,
    SignMode sign_mode)
{
    StringBuilder string_builder;
    FormatBuilder format_builder { string_builder };

    bool is_negative = integer_value < 0;
    if (is_negative)
        integer_value = -integer_value;

    TRY(format_builder.put_u64(static_cast<u64>(integer_value), base, false, upper_case, false, Align::Right, 0, ' ', sign_mode, is_negative));

    if (precision > 0) {
        // FIXME: This is a terrible approximation but doing it properly would be a lot of work. If someone is up for that, a good
        // place to start would be the following video from CppCon 2019:
        // https://youtu.be/4P_kbF0EbZM (Stephan T. Lavavej “Floating-Point <charconv>: Making Your Code 10x Faster With C++17's Final Boss”)

        u64 scale = pow<u64>(10, precision);

        auto fraction = (scale * fraction_value) / fraction_one; // TODO: overflows
        if (is_negative)
            fraction = scale - fraction;
        while (fraction != 0 && fraction % 10 == 0)
            fraction /= 10;

        size_t visible_precision = 0;
        {
            auto fraction_tmp = fraction;
            for (; visible_precision < precision; ++visible_precision) {
                if (fraction_tmp == 0)
                    break;
                fraction_tmp /= 10;
            }
        }

        if (zero_pad || visible_precision > 0)
            TRY(string_builder.try_append('.'));

        if (visible_precision > 0)
            TRY(format_builder.put_u64(fraction, base, false, upper_case, true, Align::Right, visible_precision));

        if (zero_pad && (precision - visible_precision) > 0)
            TRY(format_builder.put_u64(0, base, false, false, true, Align::Right, precision - visible_precision));
    }

    TRY(put_string(string_builder.string_view(), align, min_width, NumericLimits<size_t>::max(), fill));
    return {};
}

#ifndef KERNEL
ErrorOr<void> FormatBuilder::put_f64(
    double value,
    u8 base,
    bool upper_case,
    bool zero_pad,
    Align align,
    size_t min_width,
    size_t precision,
    char fill,
    SignMode sign_mode)
{
    StringBuilder string_builder;
    FormatBuilder format_builder { string_builder };

    if (isnan(value) || isinf(value)) [[unlikely]] {
        if (value < 0.0)
            TRY(string_builder.try_append('-'));
        else if (sign_mode == SignMode::Always)
            TRY(string_builder.try_append('+'));
        else if (sign_mode == SignMode::Reserved)
            TRY(string_builder.try_append(' '));

        if (isnan(value))
            TRY(string_builder.try_append(upper_case ? "NAN"sv : "nan"sv));
        else
            TRY(string_builder.try_append(upper_case ? "INF"sv : "inf"sv));
        TRY(put_string(string_builder.string_view(), align, min_width, NumericLimits<size_t>::max(), fill));
        return {};
    }

    bool is_negative = value < 0.0;
    if (is_negative)
        value = -value;

    TRY(format_builder.put_u64(static_cast<u64>(value), base, false, upper_case, false, Align::Right, 0, ' ', sign_mode, is_negative));

    if (precision > 0) {
        // FIXME: This is a terrible approximation but doing it properly would be a lot of work. If someone is up for that, a good
        // place to start would be the following video from CppCon 2019:
        // https://youtu.be/4P_kbF0EbZM (Stephan T. Lavavej “Floating-Point <charconv>: Making Your Code 10x Faster With C++17's Final Boss”)
        value -= static_cast<i64>(value);

        double epsilon = 0.5;
        for (size_t i = 0; i < precision; ++i)
            epsilon /= 10.0;

        size_t visible_precision = 0;
        for (; visible_precision < precision; ++visible_precision) {
            if (value - static_cast<i64>(value) < epsilon)
                break;
            value *= 10.0;
            epsilon *= 10.0;
        }

        if (zero_pad || visible_precision > 0)
            TRY(string_builder.try_append('.'));

        if (visible_precision > 0)
            TRY(format_builder.put_u64(static_cast<u64>(value), base, false, upper_case, true, Align::Right, visible_precision));

        if (zero_pad && (precision - visible_precision) > 0)
            TRY(format_builder.put_u64(0, base, false, false, true, Align::Right, precision - visible_precision));
    }

    TRY(put_string(string_builder.string_view(), align, min_width, NumericLimits<size_t>::max(), fill));
    return {};
}

ErrorOr<void> FormatBuilder::put_f80(
    long double value,
    u8 base,
    bool upper_case,
    Align align,
    size_t min_width,
    size_t precision,
    char fill,
    SignMode sign_mode)
{
    StringBuilder string_builder;
    FormatBuilder format_builder { string_builder };

    if (isnan(value) || isinf(value)) [[unlikely]] {
        if (value < 0.0l)
            TRY(string_builder.try_append('-'));
        else if (sign_mode == SignMode::Always)
            TRY(string_builder.try_append('+'));
        else if (sign_mode == SignMode::Reserved)
            TRY(string_builder.try_append(' '));

        if (isnan(value))
            TRY(string_builder.try_append(upper_case ? "NAN"sv : "nan"sv));
        else
            TRY(string_builder.try_append(upper_case ? "INF"sv : "inf"sv));
        TRY(put_string(string_builder.string_view(), align, min_width, NumericLimits<size_t>::max(), fill));
        return {};
    }

    bool is_negative = value < 0.0l;
    if (is_negative)
        value = -value;

    TRY(format_builder.put_u64(static_cast<u64>(value), base, false, upper_case, false, Align::Right, 0, ' ', sign_mode, is_negative));

    if (precision > 0) {
        // FIXME: This is a terrible approximation but doing it properly would be a lot of work. If someone is up for that, a good
        // place to start would be the following video from CppCon 2019:
        // https://youtu.be/4P_kbF0EbZM (Stephan T. Lavavej “Floating-Point <charconv>: Making Your Code 10x Faster With C++17's Final Boss”)
        value -= static_cast<i64>(value);

        long double epsilon = 0.5l;
        for (size_t i = 0; i < precision; ++i)
            epsilon /= 10.0l;

        size_t visible_precision = 0;
        for (; visible_precision < precision; ++visible_precision) {
            if (value - static_cast<i64>(value) < epsilon)
                break;
            value *= 10.0l;
            epsilon *= 10.0l;
        }

        if (visible_precision > 0) {
            string_builder.append('.');
            TRY(format_builder.put_u64(static_cast<u64>(value), base, false, upper_case, true, Align::Right, visible_precision));
        }
    }

    TRY(put_string(string_builder.string_view(), align, min_width, NumericLimits<size_t>::max(), fill));
    return {};
}

#endif

ErrorOr<void> FormatBuilder::put_hexdump(ReadonlyBytes bytes, size_t width, char fill)
{
    auto put_char_view = [&](auto i) -> ErrorOr<void> {
        TRY(put_padding(fill, 4));
        for (size_t j = i - width; j < i; ++j) {
            auto ch = bytes[j];
            TRY(m_builder.try_append(ch >= 32 && ch <= 127 ? ch : '.')); // silly hack
        }
        return {};
    };

    for (size_t i = 0; i < bytes.size(); ++i) {
        if (width > 0) {
            if (i % width == 0 && i) {
                TRY(put_char_view(i));
                TRY(put_literal("\n"sv));
            }
        }
        TRY(put_u64(bytes[i], 16, false, false, true, Align::Right, 2));
    }

    if (width > 0 && bytes.size() && bytes.size() % width == 0)
        TRY(put_char_view(bytes.size()));

    return {};
}

ErrorOr<void> vformat(StringBuilder& builder, StringView fmtstr, TypeErasedFormatParams& params)
{
    FormatBuilder fmtbuilder { builder };
    FormatParser parser { fmtstr };

    TRY(vformat_impl(params, fmtbuilder, parser));
    return {};
}

void StandardFormatter::parse(TypeErasedFormatParams& params, FormatParser& parser)
{
    if ("<^>"sv.contains(parser.peek(1))) {
        VERIFY(!parser.next_is(is_any_of("{}"sv)));
        m_fill = parser.consume();
    }

    if (parser.consume_specific('<'))
        m_align = FormatBuilder::Align::Left;
    else if (parser.consume_specific('^'))
        m_align = FormatBuilder::Align::Center;
    else if (parser.consume_specific('>'))
        m_align = FormatBuilder::Align::Right;

    if (parser.consume_specific('-'))
        m_sign_mode = FormatBuilder::SignMode::OnlyIfNeeded;
    else if (parser.consume_specific('+'))
        m_sign_mode = FormatBuilder::SignMode::Always;
    else if (parser.consume_specific(' '))
        m_sign_mode = FormatBuilder::SignMode::Reserved;

    if (parser.consume_specific('#'))
        m_alternative_form = true;

    if (parser.consume_specific('0'))
        m_zero_pad = true;

    if (size_t index = 0; parser.consume_replacement_field(index)) {
        if (index == use_next_index)
            index = params.take_next_index();

        m_width = params.parameters().at(index).to_size();
    } else if (size_t width = 0; parser.consume_number(width)) {
        m_width = width;
    }

    if (parser.consume_specific('.')) {
        if (size_t index = 0; parser.consume_replacement_field(index)) {
            if (index == use_next_index)
                index = params.take_next_index();

            m_precision = params.parameters().at(index).to_size();
        } else if (size_t precision = 0; parser.consume_number(precision)) {
            m_precision = precision;
        }
    }

    if (parser.consume_specific('b'))
        m_mode = Mode::Binary;
    else if (parser.consume_specific('B'))
        m_mode = Mode::BinaryUppercase;
    else if (parser.consume_specific('d'))
        m_mode = Mode::Decimal;
    else if (parser.consume_specific('o'))
        m_mode = Mode::Octal;
    else if (parser.consume_specific('x'))
        m_mode = Mode::Hexadecimal;
    else if (parser.consume_specific('X'))
        m_mode = Mode::HexadecimalUppercase;
    else if (parser.consume_specific('c'))
        m_mode = Mode::Character;
    else if (parser.consume_specific('s'))
        m_mode = Mode::String;
    else if (parser.consume_specific('p'))
        m_mode = Mode::Pointer;
    else if (parser.consume_specific('f'))
        m_mode = Mode::Float;
    else if (parser.consume_specific('a'))
        m_mode = Mode::Hexfloat;
    else if (parser.consume_specific('A'))
        m_mode = Mode::HexfloatUppercase;
    else if (parser.consume_specific("hex-dump"))
        m_mode = Mode::HexDump;

    if (!parser.is_eof())
        dbgln("{} did not consume '{}'", __PRETTY_FUNCTION__, parser.remaining());

    VERIFY(parser.is_eof());
}

ErrorOr<void> Formatter<StringView>::format(FormatBuilder& builder, StringView value)
{
    if (m_sign_mode != FormatBuilder::SignMode::Default)
        VERIFY_NOT_REACHED();
    if (m_alternative_form)
        VERIFY_NOT_REACHED();
    if (m_zero_pad)
        VERIFY_NOT_REACHED();
    if (m_mode != Mode::Default && m_mode != Mode::String && m_mode != Mode::Character && m_mode != Mode::HexDump)
        VERIFY_NOT_REACHED();

    m_width = m_width.value_or(0);
    m_precision = m_precision.value_or(NumericLimits<size_t>::max());

    if (m_mode == Mode::HexDump)
        return builder.put_hexdump(value.bytes(), m_width.value(), m_fill);
    return builder.put_string(value, m_align, m_width.value(), m_precision.value(), m_fill);
}

ErrorOr<void> Formatter<FormatString>::vformat(FormatBuilder& builder, StringView fmtstr, TypeErasedFormatParams& params)
{
    StringBuilder string_builder;
    TRY(AK::vformat(string_builder, fmtstr, params));
    TRY(Formatter<StringView>::format(builder, string_builder.string_view()));
    return {};
}

template<Integral T>
ErrorOr<void> Formatter<T>::format(FormatBuilder& builder, T value)
{
    if (m_mode == Mode::Character) {
        // FIXME: We just support ASCII for now, in the future maybe unicode?
        VERIFY(value >= 0 && value <= 127);

        m_mode = Mode::String;

        Formatter<StringView> formatter { *this };
        return formatter.format(builder, StringView { reinterpret_cast<char const*>(&value), 1 });
    }

    if (m_precision.has_value())
        VERIFY_NOT_REACHED();

    if (m_mode == Mode::Pointer) {
        if (m_sign_mode != FormatBuilder::SignMode::Default)
            VERIFY_NOT_REACHED();
        if (m_align != FormatBuilder::Align::Default)
            VERIFY_NOT_REACHED();
        if (m_alternative_form)
            VERIFY_NOT_REACHED();
        if (m_width.has_value())
            VERIFY_NOT_REACHED();

        m_mode = Mode::Hexadecimal;
        m_alternative_form = true;
        m_width = 2 * sizeof(void*);
        m_zero_pad = true;
    }

    u8 base = 0;
    bool upper_case = false;
    if (m_mode == Mode::Binary) {
        base = 2;
    } else if (m_mode == Mode::BinaryUppercase) {
        base = 2;
        upper_case = true;
    } else if (m_mode == Mode::Octal) {
        base = 8;
    } else if (m_mode == Mode::Decimal || m_mode == Mode::Default) {
        base = 10;
    } else if (m_mode == Mode::Hexadecimal) {
        base = 16;
    } else if (m_mode == Mode::HexadecimalUppercase) {
        base = 16;
        upper_case = true;
    } else if (m_mode == Mode::HexDump) {
        m_width = m_width.value_or(32);
        return builder.put_hexdump({ &value, sizeof(value) }, m_width.value(), m_fill);
    } else {
        VERIFY_NOT_REACHED();
    }

    m_width = m_width.value_or(0);

    if constexpr (IsSame<MakeUnsigned<T>, T>)
        return builder.put_u64(value, base, m_alternative_form, upper_case, m_zero_pad, m_align, m_width.value(), m_fill, m_sign_mode);
    else
        return builder.put_i64(value, base, m_alternative_form, upper_case, m_zero_pad, m_align, m_width.value(), m_fill, m_sign_mode);
}

ErrorOr<void> Formatter<char>::format(FormatBuilder& builder, char value)
{
    if (m_mode == Mode::Binary || m_mode == Mode::BinaryUppercase || m_mode == Mode::Decimal || m_mode == Mode::Octal || m_mode == Mode::Hexadecimal || m_mode == Mode::HexadecimalUppercase) {
        // Trick: signed char != char. (Sometimes weird features are actually helpful.)
        Formatter<signed char> formatter { *this };
        return formatter.format(builder, static_cast<signed char>(value));
    } else {
        Formatter<StringView> formatter { *this };
        return formatter.format(builder, { &value, 1 });
    }
}
ErrorOr<void> Formatter<wchar_t>::format(FormatBuilder& builder, wchar_t value)
{
    if (m_mode == Mode::Binary || m_mode == Mode::BinaryUppercase || m_mode == Mode::Decimal || m_mode == Mode::Octal || m_mode == Mode::Hexadecimal || m_mode == Mode::HexadecimalUppercase) {
        Formatter<u32> formatter { *this };
        return formatter.format(builder, static_cast<u32>(value));
    } else {
        StringBuilder codepoint;
        codepoint.append_code_point(value);

        Formatter<StringView> formatter { *this };
        return formatter.format(builder, codepoint.string_view());
    }
}
ErrorOr<void> Formatter<bool>::format(FormatBuilder& builder, bool value)
{
    if (m_mode == Mode::Binary || m_mode == Mode::BinaryUppercase || m_mode == Mode::Decimal || m_mode == Mode::Octal || m_mode == Mode::Hexadecimal || m_mode == Mode::HexadecimalUppercase) {
        Formatter<u8> formatter { *this };
        return formatter.format(builder, static_cast<u8>(value));
    } else if (m_mode == Mode::HexDump) {
        return builder.put_hexdump({ &value, sizeof(value) }, m_width.value_or(32), m_fill);
    } else {
        Formatter<StringView> formatter { *this };
        return formatter.format(builder, value ? "true"sv : "false"sv);
    }
}
#ifndef KERNEL
ErrorOr<void> Formatter<long double>::format(FormatBuilder& builder, long double value)
{
    u8 base;
    bool upper_case;
    if (m_mode == Mode::Default || m_mode == Mode::Float) {
        base = 10;
        upper_case = false;
    } else if (m_mode == Mode::Hexfloat) {
        base = 16;
        upper_case = false;
    } else if (m_mode == Mode::HexfloatUppercase) {
        base = 16;
        upper_case = true;
    } else {
        VERIFY_NOT_REACHED();
    }

    m_width = m_width.value_or(0);
    m_precision = m_precision.value_or(6);

    return builder.put_f80(value, base, upper_case, m_align, m_width.value(), m_precision.value(), m_fill, m_sign_mode);
}

ErrorOr<void> Formatter<double>::format(FormatBuilder& builder, double value)
{
    u8 base;
    bool upper_case;
    if (m_mode == Mode::Default || m_mode == Mode::Float) {
        base = 10;
        upper_case = false;
    } else if (m_mode == Mode::Hexfloat) {
        base = 16;
        upper_case = false;
    } else if (m_mode == Mode::HexfloatUppercase) {
        base = 16;
        upper_case = true;
    } else {
        VERIFY_NOT_REACHED();
    }

    m_width = m_width.value_or(0);
    m_precision = m_precision.value_or(6);

    return builder.put_f64(value, base, upper_case, m_zero_pad, m_align, m_width.value(), m_precision.value(), m_fill, m_sign_mode);
}

ErrorOr<void> Formatter<float>::format(FormatBuilder& builder, float value)
{
    Formatter<double> formatter { *this };
    return formatter.format(builder, value);
}
#endif

#ifndef KERNEL
void vout(FILE* file, StringView fmtstr, TypeErasedFormatParams& params, bool newline)
{
    StringBuilder builder;
    MUST(vformat(builder, fmtstr, params));

    if (newline)
        builder.append('\n');

    auto const string = builder.string_view();
    auto const retval = ::fwrite(string.characters_without_null_termination(), 1, string.length(), file);
    if (static_cast<size_t>(retval) != string.length()) {
        auto error = ferror(file);
        dbgln("vout() failed ({} written out of {}), error was {} ({})", retval, string.length(), error, strerror(error));
    }
}
#endif

static bool is_debug_enabled = true;

void set_debug_enabled(bool value)
{
    is_debug_enabled = value;
}

void vdbgln(StringView fmtstr, TypeErasedFormatParams& params)
{
    if (!is_debug_enabled)
        return;

    StringBuilder builder;

#ifdef AK_OS_SERENITY
#    ifdef KERNEL
    if (Kernel::Processor::is_initialized()) {
        struct timespec ts = {};
        if (TimeManagement::is_initialized())
            ts = TimeManagement::the().monotonic_time(TimePrecision::Coarse).to_timespec();
        if (Kernel::Thread::current()) {
            auto& thread = *Kernel::Thread::current();
            builder.appendff("{}.{:03} \033[34;1m[#{} {}({}:{})]\033[0m: ", ts.tv_sec, ts.tv_nsec / 1000000, Kernel::Processor::current_id(), thread.process().name(), thread.pid().value(), thread.tid().value());
        } else {
            builder.appendff("{}.{:03} \033[34;1m[#{} Kernel]\033[0m: ", ts.tv_sec, ts.tv_nsec / 1000000, Kernel::Processor::current_id());
        }
    } else {
        builder.appendff("\033[34;1m[Kernel]\033[0m: ");
    }
#    else
    static TriState got_process_name = TriState::Unknown;
    static char process_name_buffer[256];

    if (got_process_name == TriState::Unknown) {
        if (get_process_name(process_name_buffer, sizeof(process_name_buffer)) == 0)
            got_process_name = TriState::True;
        else
            got_process_name = TriState::False;
    }
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC_COARSE, &ts);
    if (got_process_name == TriState::True)
        builder.appendff("{}.{:03} \033[33;1m{}({}:{})\033[0m: ", ts.tv_sec, ts.tv_nsec / 1000000, process_name_buffer, getpid(), gettid());
#    endif
#endif

    MUST(vformat(builder, fmtstr, params));
    builder.append('\n');

    auto const string = builder.string_view();

#ifdef AK_OS_SERENITY
#    ifdef KERNEL
    if (!Kernel::Processor::is_initialized()) {
        kernelearlyputstr(string.characters_without_null_termination(), string.length());
        return;
    }
#    endif
#endif
    dbgputstr(string.characters_without_null_termination(), string.length());
}

#ifdef KERNEL
void vdmesgln(StringView fmtstr, TypeErasedFormatParams& params)
{
    StringBuilder builder;

#    ifdef AK_OS_SERENITY
    struct timespec ts = {};

    if (TimeManagement::is_initialized())
        ts = TimeManagement::the().monotonic_time(TimePrecision::Coarse).to_timespec();

    if (Kernel::Processor::is_initialized() && Kernel::Thread::current()) {
        auto& thread = *Kernel::Thread::current();
        builder.appendff("{}.{:03} \033[34;1m[{}({}:{})]\033[0m: ", ts.tv_sec, ts.tv_nsec / 1000000, thread.process().name(), thread.pid().value(), thread.tid().value());
    } else {
        builder.appendff("{}.{:03} \033[34;1m[Kernel]\033[0m: ", ts.tv_sec, ts.tv_nsec / 1000000);
    }
#    endif

    MUST(vformat(builder, fmtstr, params));
    builder.append('\n');

    auto const string = builder.string_view();
    kernelputstr(string.characters_without_null_termination(), string.length());
}

void v_critical_dmesgln(StringView fmtstr, TypeErasedFormatParams& params)
{
    // FIXME: Try to avoid memory allocations further to prevent faulting
    // at OOM conditions.

    StringBuilder builder;
#    ifdef AK_OS_SERENITY
    if (Kernel::Processor::is_initialized() && Kernel::Thread::current()) {
        auto& thread = *Kernel::Thread::current();
        builder.appendff("[{}({}:{})]: ", thread.process().name(), thread.pid().value(), thread.tid().value());
    } else {
        builder.appendff("[Kernel]: ");
    }
#    endif

    MUST(vformat(builder, fmtstr, params));
    builder.append('\n');

    auto const string = builder.string_view();
    kernelcriticalputstr(string.characters_without_null_termination(), string.length());
}

#endif

template struct Formatter<unsigned char, void>;
template struct Formatter<unsigned short, void>;
template struct Formatter<unsigned int, void>;
template struct Formatter<unsigned long, void>;
template struct Formatter<unsigned long long, void>;
template struct Formatter<short, void>;
template struct Formatter<int, void>;
template struct Formatter<long, void>;
template struct Formatter<long long, void>;
template struct Formatter<signed char, void>;

} // namespace AK
