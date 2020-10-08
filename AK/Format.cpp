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
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <ctype.h>

#ifdef KERNEL
#    include <Kernel/Process.h>
#    include <Kernel/Thread.h>
#else
#    include <stdio.h>
#    include <unistd.h>
#endif

namespace AK {

namespace {

constexpr size_t use_next_index = NumericLimits<size_t>::max();

// The worst case is that we have the largest 64-bit value formatted as binary number, this would take
// 65 bytes. Choosing a larger power of two won't hurt and is a bit of mitigation against out-of-bounds accesses.
inline size_t convert_unsigned_to_string(u64 value, Array<u8, 128>& buffer, u8 base, bool upper_case)
{
    ASSERT(base >= 2 && base <= 16);

    static constexpr const char* lowercase_lookup = "0123456789abcdef";
    static constexpr const char* uppercase_lookup = "0123456789ABCDEF";

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

void vformat_impl(TypeErasedFormatParams& params, FormatBuilder& builder, FormatParser& parser)
{
    const auto literal = parser.consume_literal();
    builder.put_literal(literal);

    FormatParser::FormatSpecifier specifier;
    if (!parser.consume_specifier(specifier)) {
        ASSERT(parser.is_eof());
        return;
    }

    if (specifier.index == use_next_index)
        specifier.index = params.take_next_index();

    auto& parameter = params.parameters().at(specifier.index);

    FormatParser argparser { specifier.flags };
    parameter.formatter(params, builder, argparser, parameter.value);

    vformat_impl(params, builder, parser);
}

} // namespace AK::{anonymous}

size_t TypeErasedFormatParams::decode(size_t value, size_t default_value)
{
    if (value == StandardFormatter::value_not_set)
        return default_value;

    if (value == StandardFormatter::value_from_next_arg)
        value = StandardFormatter::value_from_arg + take_next_index();

    if (value >= StandardFormatter::value_from_arg) {
        const auto parameter = parameters().at(value - StandardFormatter::value_from_arg);

        Optional<i64> svalue;
        if (parameter.type == TypeErasedParameter::Type::UInt8)
            value = *reinterpret_cast<const u8*>(parameter.value);
        else if (parameter.type == TypeErasedParameter::Type::UInt16)
            value = *reinterpret_cast<const u16*>(parameter.value);
        else if (parameter.type == TypeErasedParameter::Type::UInt32)
            value = *reinterpret_cast<const u32*>(parameter.value);
        else if (parameter.type == TypeErasedParameter::Type::UInt64)
            value = *reinterpret_cast<const u64*>(parameter.value);
        else if (parameter.type == TypeErasedParameter::Type::Int8)
            svalue = *reinterpret_cast<const i8*>(parameter.value);
        else if (parameter.type == TypeErasedParameter::Type::Int16)
            svalue = *reinterpret_cast<const i16*>(parameter.value);
        else if (parameter.type == TypeErasedParameter::Type::Int32)
            svalue = *reinterpret_cast<const i32*>(parameter.value);
        else if (parameter.type == TypeErasedParameter::Type::Int64)
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

FormatParser::FormatParser(StringView input)
    : GenericLexer(input)
{
}
StringView FormatParser::consume_literal()
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
bool FormatParser::consume_number(size_t& value)
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
bool FormatParser::consume_specifier(FormatSpecifier& specifier)
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
bool FormatParser::consume_replacement_field(size_t& index)
{
    if (!consume_specific('{'))
        return false;

    if (!consume_number(index))
        index = use_next_index;

    if (!consume_specific('}'))
        ASSERT_NOT_REACHED();

    return true;
}

void FormatBuilder::put_padding(char fill, size_t amount)
{
    for (size_t i = 0; i < amount; ++i)
        m_builder.append(fill);
}
void FormatBuilder::put_literal(StringView value)
{
    for (size_t i = 0; i < value.length(); ++i) {
        m_builder.append(value[i]);
        if (value[i] == '{' || value[i] == '}')
            ++i;
    }
}
void FormatBuilder::put_string(
    StringView value,
    Align align,
    size_t min_width,
    size_t max_width,
    char fill)
{
    const auto used_by_string = min(max_width, value.length());
    const auto used_by_padding = max(min_width, used_by_string) - used_by_string;

    if (used_by_string < value.length())
        value = value.substring_view(0, used_by_string);

    if (align == Align::Left || align == Align::Default) {
        m_builder.append(value);
        put_padding(fill, used_by_padding);
    } else if (align == Align::Center) {
        const auto used_by_left_padding = used_by_padding / 2;
        const auto used_by_right_padding = ceil_div<size_t, size_t>(used_by_padding, 2);

        put_padding(fill, used_by_left_padding);
        m_builder.append(value);
        put_padding(fill, used_by_right_padding);
    } else if (align == Align::Right) {
        put_padding(fill, used_by_padding);
        m_builder.append(value);
    }
}
void FormatBuilder::put_u64(
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

    const auto used_by_digits = convert_unsigned_to_string(value, buffer, base, upper_case);

    size_t used_by_prefix = 0;
    if (align == Align::Right && zero_pad) {
        // We want String::formatted("{:#08x}", 32) to produce '0x00000020' instead of '0x000020'. This
        // behaviour differs from both fmtlib and printf, but is more intuitive.
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

    const auto used_by_field = used_by_prefix + used_by_digits;
    const auto used_by_padding = max(used_by_field, min_width) - used_by_field;

    const auto put_prefix = [&]() {
        if (is_negative)
            m_builder.append('-');
        else if (sign_mode == SignMode::Always)
            m_builder.append('+');
        else if (sign_mode == SignMode::Reserved)
            m_builder.append(' ');

        if (prefix) {
            if (base == 2) {
                if (upper_case)
                    m_builder.append("0B");
                else
                    m_builder.append("0b");
            } else if (base == 8) {
                m_builder.append("0");
            } else if (base == 16) {
                if (upper_case)
                    m_builder.append("0X");
                else
                    m_builder.append("0x");
            }
        }
    };
    const auto put_digits = [&]() {
        for (size_t i = 0; i < used_by_digits; ++i)
            m_builder.append(buffer[i]);
    };

    if (align == Align::Left) {
        const auto used_by_right_padding = used_by_padding;

        put_prefix();
        put_digits();
        put_padding(fill, used_by_right_padding);
    } else if (align == Align::Center) {
        const auto used_by_left_padding = used_by_padding / 2;
        const auto used_by_right_padding = ceil_div<size_t, size_t>(used_by_padding, 2);

        put_padding(fill, used_by_left_padding);
        put_prefix();
        put_digits();
        put_padding(fill, used_by_right_padding);
    } else if (align == Align::Right) {
        const auto used_by_left_padding = used_by_padding;

        if (zero_pad) {
            put_prefix();
            put_padding('0', used_by_left_padding);
            put_digits();
        } else {
            put_padding(fill, used_by_left_padding);
            put_prefix();
            put_digits();
        }
    }
}
void FormatBuilder::put_i64(
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
    const auto is_negative = value < 0;
    value = is_negative ? -value : value;

    put_u64(static_cast<size_t>(value), base, prefix, upper_case, zero_pad, align, min_width, fill, sign_mode, is_negative);
}

void vformat(StringBuilder& builder, StringView fmtstr, TypeErasedFormatParams params)
{
    FormatBuilder fmtbuilder { builder };
    FormatParser parser { fmtstr };

    vformat_impl(params, fmtbuilder, parser);
}
void vformat(const LogStream& stream, StringView fmtstr, TypeErasedFormatParams params)
{
    StringBuilder builder;
    vformat(builder, fmtstr, params);
    stream << builder.to_string();
}

void StandardFormatter::parse(TypeErasedFormatParams& params, FormatParser& parser)
{
    if (StringView { "<^>" }.contains(parser.peek(1))) {
        ASSERT(!parser.next_is(is_any_of("{}")));
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

        m_width = value_from_arg + index;
    } else if (size_t width = 0; parser.consume_number(width)) {
        m_width = width;
    }

    if (parser.consume_specific('.')) {
        if (size_t index = 0; parser.consume_replacement_field(index)) {
            if (index == use_next_index)
                index = params.take_next_index();

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
        dbgln("{} did not consume '{}'", __PRETTY_FUNCTION__, parser.remaining());

    ASSERT(parser.is_eof());
}

void Formatter<StringView>::format(TypeErasedFormatParams& params, FormatBuilder& builder, StringView value)
{
    if (m_sign_mode != FormatBuilder::SignMode::Default)
        ASSERT_NOT_REACHED();
    if (m_alternative_form)
        ASSERT_NOT_REACHED();
    if (m_zero_pad)
        ASSERT_NOT_REACHED();
    if (m_mode != Mode::Default && m_mode != Mode::String)
        ASSERT_NOT_REACHED();
    if (m_width != value_not_set && m_precision != value_not_set)
        ASSERT_NOT_REACHED();

    const auto width = params.decode(m_width);
    const auto precision = params.decode(m_precision, NumericLimits<size_t>::max());

    builder.put_string(value, m_align, width, precision, m_fill);
}

template<typename T>
void Formatter<T, typename EnableIf<IsIntegral<T>::value>::Type>::format(TypeErasedFormatParams& params, FormatBuilder& builder, T value)
{
    if (m_mode == Mode::Character) {
        // FIXME: We just support ASCII for now, in the future maybe unicode?
        ASSERT(value >= 0 && value <= 127);

        m_mode = Mode::String;

        Formatter<StringView> formatter { *this };
        return formatter.format(params, builder, StringView { reinterpret_cast<const char*>(&value), 1 });
    }

    if (m_precision != NumericLimits<size_t>::max())
        ASSERT_NOT_REACHED();

    if (m_mode == Mode::Pointer) {
        if (m_sign_mode != FormatBuilder::SignMode::Default)
            ASSERT_NOT_REACHED();
        if (m_align != FormatBuilder::Align::Default)
            ASSERT_NOT_REACHED();
        if (m_alternative_form)
            ASSERT_NOT_REACHED();
        if (m_width != value_not_set)
            ASSERT_NOT_REACHED();

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
    } else {
        ASSERT_NOT_REACHED();
    }

    const auto width = params.decode(m_width);

    if (IsSame<typename MakeUnsigned<T>::Type, T>::value)
        builder.put_u64(value, base, m_alternative_form, upper_case, m_zero_pad, m_align, width, m_fill, m_sign_mode);
    else
        builder.put_i64(value, base, m_alternative_form, upper_case, m_zero_pad, m_align, width, m_fill, m_sign_mode);
}

void Formatter<char>::format(TypeErasedFormatParams& params, FormatBuilder& builder, char value)
{
    if (m_mode == Mode::Binary || m_mode == Mode::BinaryUppercase || m_mode == Mode::Decimal || m_mode == Mode::Octal || m_mode == Mode::Hexadecimal || m_mode == Mode::HexadecimalUppercase) {
        // Trick: signed char != char. (Sometimes weird features are actually helpful.)
        Formatter<signed char> formatter { *this };
        return formatter.format(params, builder, static_cast<signed char>(value));
    } else {
        Formatter<StringView> formatter { *this };
        return formatter.format(params, builder, { &value, 1 });
    }
}
void Formatter<bool>::format(TypeErasedFormatParams& params, FormatBuilder& builder, bool value)
{
    if (m_mode == Mode::Binary || m_mode == Mode::BinaryUppercase || m_mode == Mode::Decimal || m_mode == Mode::Octal || m_mode == Mode::Hexadecimal || m_mode == Mode::HexadecimalUppercase) {
        Formatter<u8> formatter { *this };
        return formatter.format(params, builder, static_cast<u8>(value));
    } else {
        Formatter<StringView> formatter { *this };
        return formatter.format(params, builder, value ? "true" : "false");
    }
}

#ifndef KERNEL
void raw_out(StringView string)
{
    const auto retval = ::fwrite(string.characters_without_null_termination(), 1, string.length(), stdout);
    ASSERT(retval == string.length());
}
void vout(StringView fmtstr, TypeErasedFormatParams params, bool newline)
{
    StringBuilder builder;
    vformat(builder, fmtstr, params);

    if (newline && !builder.is_empty())
        builder.append('\n');

    raw_out(builder.to_string());
}

void raw_warn(StringView string)
{
    const auto retval = ::write(STDERR_FILENO, string.characters_without_null_termination(), string.length());
    ASSERT(static_cast<size_t>(retval) == string.length());
}
void vwarn(StringView fmtstr, TypeErasedFormatParams params, bool newline)
{
    StringBuilder builder;
    vformat(builder, fmtstr, params);

    if (newline && !builder.is_empty())
        builder.append('\n');

    raw_warn(builder.to_string());
}
#endif

void vdbgln(StringView fmtstr, TypeErasedFormatParams params)
{
    StringBuilder builder;

// FIXME: This logic is redundant with the stuff in LogStream.cpp.
#if defined(__serenity__)
#    ifdef KERNEL
    if (Kernel::Processor::is_initialized() && Kernel::Thread::current()) {
        auto& thread = *Kernel::Thread::current();
        builder.appendff("\033[34;1m[{}({}:{})]\033[0m: ", thread.process().name(), thread.pid().value(), thread.tid().value());
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
    if (got_process_name == TriState::True)
        builder.appendff("\033[33;1m{}({})\033[0m: ", process_name_buffer, getpid());
#    endif
#endif

    vformat(builder, fmtstr, params);
    builder.append('\n');

    const auto string = builder.build();

    const auto retval = dbgputstr(string.characters(), string.length());
    ASSERT(retval == 0);
}

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
