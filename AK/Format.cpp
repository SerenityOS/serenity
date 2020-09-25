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

namespace {

struct FormatSpecifier {
    StringView flags;
    size_t index { 0 };
};

static bool find_next_unescaped(size_t& index, StringView input, char ch)
{
    constexpr size_t unset = NumericLimits<size_t>::max();

    index = unset;
    for (size_t idx = 0; idx < input.length(); ++idx) {
        if (input[idx] == ch) {
            if (index == unset)
                index = idx;
            else
                index = unset;
        } else if (index != unset) {
            return true;
        }
    }

    return index != unset;
}
static bool find_next(size_t& index, StringView input, char ch)
{
    for (index = 0; index < input.length(); ++index) {
        if (input[index] == ch)
            return index;
    }

    return false;
}
static void write_escaped_literal(StringBuilder& builder, StringView literal)
{
    for (size_t idx = 0; idx < literal.length(); ++idx) {
        builder.append(literal[idx]);
        if (literal[idx] == '{' || literal[idx] == '}')
            ++idx;
    }
}

static bool parse_number(GenericLexer& lexer, size_t& value)
{
    value = 0;

    bool consumed_at_least_one = false;
    while (!lexer.is_eof()) {
        if (lexer.next_is(is_digit)) {
            value *= 10;
            value += lexer.consume() - '0';
            consumed_at_least_one = true;
        } else {
            break;
        }
    }

    return consumed_at_least_one;
}

constexpr size_t use_next_index = NumericLimits<size_t>::max();

static bool parse_format_specifier(StringView input, FormatSpecifier& specifier)
{
    GenericLexer lexer { input };

    if (!parse_number(lexer, specifier.index))
        specifier.index = use_next_index;

    if (!lexer.consume_specific(':'))
        return lexer.is_eof();

    specifier.flags = lexer.consume_all();
    return true;
}

static bool parse_nested_replacement_field(GenericLexer& lexer, size_t& index)
{
    if (!lexer.consume_specific('{'))
        return false;

    if (!parse_number(lexer, index))
        index = use_next_index;

    if (!lexer.consume_specific('}'))
        ASSERT_NOT_REACHED();

    return true;
}

} // namespace

namespace AK {

void vformat(StringBuilder& builder, StringView fmtstr, AK::Span<const TypeErasedParameter> parameters, size_t argument_index)
{
    size_t opening;
    if (!find_next_unescaped(opening, fmtstr, '{')) {
        size_t dummy;
        if (find_next_unescaped(dummy, fmtstr, '}'))
            ASSERT_NOT_REACHED();

        write_escaped_literal(builder, fmtstr);
        return;
    }

    write_escaped_literal(builder, fmtstr.substring_view(0, opening));

    size_t closing;
    if (!find_next(closing, fmtstr.substring_view(opening), '}'))
        ASSERT_NOT_REACHED();
    closing += opening;

    FormatSpecifier specifier;
    if (!parse_format_specifier(fmtstr.substring_view(opening + 1, closing - (opening + 1)), specifier))
        ASSERT_NOT_REACHED();

    if (specifier.index == NumericLimits<size_t>::max())
        specifier.index = argument_index++;

    if (specifier.index >= parameters.size())
        ASSERT_NOT_REACHED();

    auto& parameter = parameters[specifier.index];
    parameter.formatter(builder, parameter.value, specifier.flags, parameters);

    vformat(builder, fmtstr.substring_view(closing + 1), parameters, argument_index);
}
void vformat(const LogStream& stream, StringView fmtstr, Span<const TypeErasedParameter> parameters)
{
    StringBuilder builder;
    vformat(builder, fmtstr, parameters);
    stream << builder.to_string();
}

void StandardFormatter::parse(StringView specifier)
{
    GenericLexer lexer { specifier };

    if (StringView { "<^>" }.contains(lexer.peek(1))) {
        ASSERT(!lexer.next_is(is_any_of("{}")));
        m_fill = lexer.consume();
    }

    if (lexer.consume_specific('<'))
        m_align = Align::Left;
    else if (lexer.consume_specific('^'))
        m_align = Align::Center;
    else if (lexer.consume_specific('>'))
        m_align = Align::Right;

    if (lexer.consume_specific('-'))
        m_sign = Sign::NegativeOnly;
    else if (lexer.consume_specific('+'))
        m_sign = Sign::PositiveAndNegative;
    else if (lexer.consume_specific(' '))
        m_sign = Sign::ReserveSpace;

    if (lexer.consume_specific('#'))
        m_alternative_form = true;

    if (lexer.consume_specific('0'))
        m_zero_pad = true;

    if (size_t index = 0; parse_nested_replacement_field(lexer, index))
        m_width = value_from_arg + index;
    else if (size_t width = 0; parse_number(lexer, width))
        m_width = width;

    if (lexer.consume_specific('.')) {
        if (size_t index = 0; parse_nested_replacement_field(lexer, index))
            m_precision = value_from_arg + index;
        else if (size_t precision = 0; parse_number(lexer, precision))
            m_precision = precision;
    }

    if (lexer.consume_specific('b'))
        m_mode = Mode::Binary;
    else if (lexer.consume_specific('d'))
        m_mode = Mode::Decimal;
    else if (lexer.consume_specific('o'))
        m_mode = Mode::Octal;
    else if (lexer.consume_specific('x'))
        m_mode = Mode::Hexadecimal;
    else if (lexer.consume_specific('c'))
        m_mode = Mode::Character;
    else if (lexer.consume_specific('s'))
        m_mode = Mode::String;
    else if (lexer.consume_specific('p'))
        m_mode = Mode::Pointer;

    if (!lexer.is_eof())
        dbg() << __PRETTY_FUNCTION__ << " did not consume '" << lexer.remaining() << "'";

    ASSERT(lexer.is_eof());
}

void Formatter<StringView>::format(StringBuilder& builder, StringView value, Span<const TypeErasedParameter>)
{
    if (m_align != Align::Default)
        TODO();
    if (m_sign != Sign::Default)
        ASSERT_NOT_REACHED();
    if (m_alternative_form)
        ASSERT_NOT_REACHED();
    if (m_zero_pad)
        ASSERT_NOT_REACHED();
    if (m_width != value_not_set)
        TODO();
    if (m_precision != value_not_set)
        TODO();
    if (m_mode != Mode::Default && m_mode != Mode::String)
        ASSERT_NOT_REACHED();

    builder.append(value);
}

template<typename T>
void Formatter<T, typename EnableIf<IsIntegral<T>::value>::Type>::format(StringBuilder& builder, T value, Span<const TypeErasedParameter> parameters)
{
    if (m_align != Align::Default)
        TODO();
    if (m_sign != Sign::Default)
        TODO();
    if (m_alternative_form)
        TODO();
    if (m_precision != value_not_set)
        ASSERT_NOT_REACHED();
    if (m_mode == Mode::Character)
        TODO();

    int base;
    if (m_mode == Mode::Binary)
        TODO();
    else if (m_mode == Mode::Octal)
        TODO();
    else if (m_mode == Mode::Decimal || m_mode == Mode::Default)
        base = 10;
    else if (m_mode == Mode::Hexadecimal)
        base = 16;
    else
        ASSERT_NOT_REACHED();

    size_t width = m_width;
    if (m_width >= value_from_arg) {
        const auto parameter = parameters.at(m_width - value_from_arg);

        // FIXME: Totally unsave cast. We should store the type in TypeErasedParameter. For compactness it could be smart to
        //        find a few addresses that can not be valid function pointers and encode the type information there?
        width = *reinterpret_cast<const size_t*>(parameter.value);
    }

    // FIXME: We really need one canonical print implementation that just takes a base.
    (void)base;

    char* bufptr;
    if (m_mode == Mode::Hexadecimal)
        PrintfImplementation::print_hex([&](auto, char ch) { builder.append(ch); }, bufptr, value, false, false, false, m_zero_pad, width);
    else if (IsSame<typename MakeUnsigned<T>::Type, T>::value)
        PrintfImplementation::print_u64([&](auto, char ch) { builder.append(ch); }, bufptr, value, false, m_zero_pad, width);
    else
        PrintfImplementation::print_i64([&](auto, char ch) { builder.append(ch); }, bufptr, value, false, m_zero_pad, width);
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
