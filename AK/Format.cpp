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
#include <AK/StringBuilder.h>

namespace AK::Detail::Format {

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
static size_t parse_number(StringView input)
{
    String null_terminated { input };
    char* endptr;
    return strtoull(null_terminated.characters(), &endptr, 10);
}
static bool parse_format_specifier(StringView input, FormatSpecifier& specifier)
{
    specifier.index = NumericLimits<size_t>::max();

    GenericLexer lexer { input };

    auto index = lexer.consume_while([](char ch) { return StringView { "0123456789" }.contains(ch); });

    if (index.length() > 0)
        specifier.index = parse_number(index);

    if (!lexer.consume_specific(':'))
        return lexer.is_eof();

    specifier.flags = lexer.consume_all();
    return true;
}

String format(StringView fmtstr, AK::Span<TypeErasedFormatter> formatters, size_t argument_index)
{
    StringBuilder builder;
    format(builder, fmtstr, formatters, argument_index);
    return builder.to_string();
}

void format(StringBuilder& builder, StringView fmtstr, AK::Span<TypeErasedFormatter> formatters, size_t argument_index)
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

    if (specifier.index >= formatters.size())
        ASSERT_NOT_REACHED();

    auto& formatter = formatters[specifier.index];
    if (!formatter.format(builder, formatter.parameter, specifier.flags))
        ASSERT_NOT_REACHED();

    format(builder, fmtstr.substring_view(closing + 1), formatters, argument_index);
}

} // namespace AK::Detail::Format

namespace AK {

template<typename T>
bool Formatter<T, typename EnableIf<IsIntegral<T>::value>::Type>::parse(StringView flags)
{
    GenericLexer lexer { flags };

    if (lexer.consume_specific('0'))
        zero_pad = true;

    auto field_width = lexer.consume_while([](char ch) { return StringView { "0123456789" }.contains(ch); });
    if (field_width.length() > 0)
        this->field_width = Detail::Format::parse_number(field_width);

    if (lexer.consume_specific('x'))
        hexadecimal = true;

    return lexer.is_eof();
}

template<typename T>
void Formatter<T, typename EnableIf<IsIntegral<T>::value>::Type>::format(StringBuilder& builder, T value)
{
    char* bufptr;

    if (hexadecimal)
        PrintfImplementation::print_hex([&](auto, char ch) { builder.append(ch); }, bufptr, value, false, false, false, zero_pad, field_width);
    else if (IsSame<typename MakeUnsigned<T>::Type, T>::value)
        PrintfImplementation::print_u64([&](auto, char ch) { builder.append(ch); }, bufptr, value, false, zero_pad, field_width);
    else
        PrintfImplementation::print_i64([&](auto, char ch) { builder.append(ch); }, bufptr, value, false, zero_pad, field_width);
}

template struct Formatter<StringView>;
template struct Formatter<String>;
template struct Formatter<u8, void>;
template struct Formatter<u16, void>;
template struct Formatter<u32, void>;
template struct Formatter<u64, void>;
template struct Formatter<i8, void>;
template struct Formatter<i16, void>;
template struct Formatter<i32, void>;
template struct Formatter<i64, void>;

} // namespace AK
