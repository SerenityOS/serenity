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

#include <AK/Format.h>
#include <AK/GenericLexer.h>
#include <AK/PrintfImplementation.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <ctype.h>

namespace {

constexpr size_t use_next_index = NumericLimits<size_t>::max();

struct FormatSpecifier {
    StringView flags;
    size_t index;
};

class FormatStringParser : public GenericLexer {
public:
    explicit FormatStringParser(StringView input)
        : GenericLexer(input)
    {
    }

    StringView consume_literal()
    {
        const auto begin = tell();

        while (!is_eof()) {
            if (consume_specific("{{"))
                continue;

            if (consume_specific("}}"))
                continue;

            if (next_is(is_any_of("{}")))
                return m_input.substring_view(begin, tell() - begin);

            consume();
        }

        return m_input.substring_view(begin);
    }

    bool consume_number(size_t& value)
    {
        value = 0;

        bool consumed_at_least_one = false;
        while (next_is(isdigit)) {
            value *= 10;
            value += consume() - '0';
            consumed_at_least_one = true;
        }

        return consumed_at_least_one;
    }

    bool consume_specifier(FormatSpecifier& specifier)
    {
        ASSERT(!next_is('}'));

        if (!consume_specific('{'))
            return false;

        if (!consume_number(specifier.index))
            specifier.index = use_next_index;

        if (consume_specific(':')) {
            const auto begin = tell();

            size_t level = 1;
            while (level > 0) {
                ASSERT(!is_eof());

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
                ASSERT_NOT_REACHED();

            specifier.flags = "";
        }

        return true;
    }

    bool consume_replacement_field(size_t& index)
    {
        if (!consume_specific('{'))
            return false;

        if (!consume_number(index))
            index = use_next_index;

        if (!consume_specific('}'))
            ASSERT_NOT_REACHED();

        return true;
    }
};

void write_escaped_literal(StringBuilder& builder, StringView literal)
{
    for (size_t idx = 0; idx < literal.length(); ++idx) {
        builder.append(literal[idx]);
        if (literal[idx] == '{' || literal[idx] == '}')
            ++idx;
    }
}

void vformat_impl(StringBuilder& builder, FormatStringParser& parser, AK::FormatterContext& context)
{
    const auto literal = parser.consume_literal();
    write_escaped_literal(builder, literal);

    FormatSpecifier specifier;
    if (!parser.consume_specifier(specifier)) {
        ASSERT(parser.is_eof());
        return;
    }

    if (specifier.index == use_next_index)
        specifier.index = context.take_next_index();

    ASSERT(specifier.index < context.parameter_count());

    context.set_flags(specifier.flags);

    auto& parameter = context.parameter_at(specifier.index);
    parameter.formatter(builder, parameter.value, context);

    vformat_impl(builder, parser, context);
}

size_t decode_value(size_t value, AK::FormatterContext& context)
{
    if (value == AK::StandardFormatter::value_from_next_arg)
        value = AK::StandardFormatter::value_from_arg + context.take_next_index();

    if (value >= AK::StandardFormatter::value_from_arg) {
        const auto parameter = context.parameter_at(value - AK::StandardFormatter::value_from_arg);

        Optional<i64> svalue;
        if (parameter.type == AK::TypeErasedParameter::Type::UInt8)
            value = *reinterpret_cast<const u8*>(parameter.value);
        else if (parameter.type == AK::TypeErasedParameter::Type::UInt16)
            value = *reinterpret_cast<const u16*>(parameter.value);
        else if (parameter.type == AK::TypeErasedParameter::Type::UInt32)
            value = *reinterpret_cast<const u32*>(parameter.value);
        else if (parameter.type == AK::TypeErasedParameter::Type::UInt64)
            value = *reinterpret_cast<const u64*>(parameter.value);
        else if (parameter.type == AK::TypeErasedParameter::Type::Int8)
            svalue = *reinterpret_cast<const i8*>(parameter.value);
        else if (parameter.type == AK::TypeErasedParameter::Type::Int16)
            svalue = *reinterpret_cast<const i16*>(parameter.value);
        else if (parameter.type == AK::TypeErasedParameter::Type::Int32)
            svalue = *reinterpret_cast<const i32*>(parameter.value);
        else if (parameter.type == AK::TypeErasedParameter::Type::Int64)
            svalue = *reinterpret_cast<const i64*>(parameter.value);
        else
            ASSERT_NOT_REACHED();

        if (svalue.has_value()) {
            ASSERT(svalue.value() >= 0);
            value = static_cast<size_t>(svalue.value());
        }
    }

    return value;
}

} // namespace

namespace AK {

void vformat(StringBuilder& builder, StringView fmtstr, Span<const TypeErasedParameter> parameters)
{
    FormatStringParser parser { fmtstr };
    FormatterContext context { parameters };
    vformat_impl(builder, parser, context);
}
void vformat(const LogStream& stream, StringView fmtstr, Span<const TypeErasedParameter> parameters)
{
    StringBuilder builder;
    FormatStringParser parser { fmtstr };
    FormatterContext context { parameters };
    vformat_impl(builder, parser, context);
    stream << builder.to_string();
}

void StandardFormatter::parse(FormatterContext& context)
{
    FormatStringParser parser { context.flags() };

    if (StringView { "<^>" }.contains(parser.peek(1))) {
        ASSERT(!parser.next_is(is_any_of("{}")));
        m_fill = parser.consume();
    }

    if (parser.consume_specific('<'))
        m_align = Align::Left;
    else if (parser.consume_specific('^'))
        m_align = Align::Center;
    else if (parser.consume_specific('>'))
        m_align = Align::Right;

    if (parser.consume_specific('-'))
        m_sign = Sign::NegativeOnly;
    else if (parser.consume_specific('+'))
        m_sign = Sign::PositiveAndNegative;
    else if (parser.consume_specific(' '))
        m_sign = Sign::ReserveSpace;

    if (parser.consume_specific('#'))
        m_alternative_form = true;

    if (parser.consume_specific('0'))
        m_zero_pad = true;

    if (size_t index = 0; parser.consume_replacement_field(index)) {
        if (index == use_next_index)
            index = context.take_next_index();

        m_width = value_from_arg + index;
    } else if (size_t width = 0; parser.consume_number(width)) {
        m_width = width;
    }

    if (parser.consume_specific('.')) {
        if (size_t index = 0; parser.consume_replacement_field(index)) {
            if (index == use_next_index)
                index = context.take_next_index();

            m_precision = value_from_arg + index;
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

    if (!parser.is_eof())
        dbg() << __PRETTY_FUNCTION__ << " did not consume '" << parser.remaining() << "'";

    ASSERT(parser.is_eof());
}

void Formatter<StringView>::format(StringBuilder& builder, StringView value, FormatterContext& context)
{
    if (m_sign != Sign::Default)
        ASSERT_NOT_REACHED();
    if (m_alternative_form)
        ASSERT_NOT_REACHED();
    if (m_zero_pad)
        ASSERT_NOT_REACHED();
    if (m_mode != Mode::Default && m_mode != Mode::String)
        ASSERT_NOT_REACHED();
    if (m_width != value_not_set && m_precision != value_not_set)
        ASSERT_NOT_REACHED();

    if (m_align == Align::Default)
        m_align = Align::Left;

    const auto width = decode_value(m_width, context);
    const auto precision = decode_value(m_precision, context);

    const auto put_padding = [&](size_t amount, char fill) {
        for (size_t i = 0; i < amount; ++i)
            builder.append(fill);
    };
    const auto put_bytes = [&](ReadonlyBytes bytes) {
        for (size_t i = 0; i < bytes.size(); ++i)
            builder.append(static_cast<char>(bytes[i]));
    };

    auto used_by_string = value.length();
    if (precision != value_not_set)
        used_by_string = min(used_by_string, precision);

    const auto used_by_padding = width < used_by_string ? 0 : width - used_by_string;

    if (m_align == Align::Left) {
        const auto used_by_right_padding = used_by_padding;

        put_bytes(value.bytes().trim(used_by_string));
        put_padding(used_by_right_padding, m_fill);
        return;
    }
    if (m_align == Align::Center) {
        const auto used_by_left_padding = used_by_padding / 2;
        const auto used_by_right_padding = ceil_div<size_t, size_t>(used_by_padding, 2);

        put_padding(used_by_left_padding, m_fill);
        put_bytes(value.bytes().trim(used_by_string));
        put_padding(used_by_right_padding, m_fill);
        return;
    }
    if (m_align == Align::Right) {
        const auto used_by_left_padding = used_by_padding;

        put_padding(used_by_left_padding, m_fill);
        put_bytes(value.bytes().trim(used_by_string));
        return;
    }

    ASSERT_NOT_REACHED();
}

template<typename T>
void Formatter<T, typename EnableIf<IsIntegral<T>::value>::Type>::format(StringBuilder& builder, T value, FormatterContext& context)
{
    if (m_precision != value_not_set)
        ASSERT_NOT_REACHED();

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
    } else if (m_mode == Mode::Character) {
        // special case
    } else {
        ASSERT_NOT_REACHED();
    }

    auto width = decode_value(m_width, context);

    const auto put_padding = [&](size_t amount, char fill) {
        for (size_t i = 0; i < amount; ++i)
            builder.append(fill);
    };

    if (m_mode == Mode::Character) {
        // FIXME: We just support ASCII for now, in the future maybe unicode?
        ASSERT(value >= 0 && value <= 127);

        const size_t used_by_value = 1;
        const auto used_by_padding = width < used_by_value ? 0 : width - used_by_value;

        if (m_align == Align::Left || m_align == Align::Default) {
            const auto used_by_right_padding = used_by_padding;

            builder.append(static_cast<char>(value));
            put_padding(used_by_right_padding, m_fill);
            return;
        }
        if (m_align == Align::Center) {
            const auto used_by_left_padding = used_by_padding / 2;
            const auto used_by_right_padding = ceil_div<size_t, size_t>(used_by_padding, 2);

            put_padding(used_by_left_padding, m_fill);
            builder.append(static_cast<char>(value));
            put_padding(used_by_right_padding, m_fill);
            return;
        }
        if (m_align == Align::Right) {
            const auto used_by_left_padding = used_by_padding;

            put_padding(used_by_left_padding, m_fill);
            builder.append(static_cast<char>(value));
            return;
        }

        ASSERT_NOT_REACHED();
    }

    PrintfImplementation::Align align;
    if (m_align == Align::Left)
        align = PrintfImplementation::Align::Left;
    else if (m_align == Align::Right)
        align = PrintfImplementation::Align::Right;
    else if (m_align == Align::Center)
        align = PrintfImplementation::Align::Center;
    else if (m_align == Align::Default)
        align = PrintfImplementation::Align::Right;
    else
        ASSERT_NOT_REACHED();

    PrintfImplementation::SignMode sign_mode;
    if (m_sign == Sign::Default)
        sign_mode = PrintfImplementation::SignMode::OnlyIfNeeded;
    else if (m_sign == Sign::NegativeOnly)
        sign_mode = PrintfImplementation::SignMode::OnlyIfNeeded;
    else if (m_sign == Sign::PositiveAndNegative)
        sign_mode = PrintfImplementation::SignMode::Always;
    else if (m_sign == Sign::ReserveSpace)
        sign_mode = PrintfImplementation::SignMode::Reserved;
    else
        ASSERT_NOT_REACHED();

    if (IsSame<typename MakeUnsigned<T>::Type, T>::value)
        PrintfImplementation::convert_unsigned_to_string(value, builder, base, m_alternative_form, upper_case, m_zero_pad, align, width, m_fill, sign_mode);
    else
        PrintfImplementation::convert_signed_to_string(value, builder, base, m_alternative_form, upper_case, m_zero_pad, align, width, m_fill, sign_mode);
}

template struct Formatter<unsigned char, void>;
template struct Formatter<unsigned short, void>;
template struct Formatter<unsigned int, void>;
template struct Formatter<unsigned long, void>;
template struct Formatter<unsigned long long, void>;
template struct Formatter<char, void>;
template struct Formatter<short, void>;
template struct Formatter<int, void>;
template struct Formatter<long, void>;
template struct Formatter<long long, void>;
template struct Formatter<signed char, void>;

} // namespace AK
