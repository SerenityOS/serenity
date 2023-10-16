/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/DeprecatedFlyString.h>
#include <AK/DeprecatedString.h>
#include <AK/Format.h>
#include <AK/Function.h>
#include <AK/StdLibExtras.h>
#include <AK/StringView.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>

namespace AK {

bool DeprecatedString::operator==(DeprecatedFlyString const& fly_string) const
{
    return m_impl == fly_string.impl() || view() == fly_string.view();
}

bool DeprecatedString::operator==(DeprecatedString const& other) const
{
    return m_impl == other.impl() || view() == other.view();
}

bool DeprecatedString::operator==(StringView other) const
{
    if (other.is_null())
        return is_empty();

    return view() == other;
}

bool DeprecatedString::operator<(DeprecatedString const& other) const
{
    return view() < other.view();
}

bool DeprecatedString::operator>(DeprecatedString const& other) const
{
    return view() > other.view();
}

bool DeprecatedString::copy_characters_to_buffer(char* buffer, size_t buffer_size) const
{
    // We must fit at least the NUL-terminator.
    VERIFY(buffer_size > 0);

    size_t characters_to_copy = min(length(), buffer_size - 1);
    __builtin_memcpy(buffer, characters(), characters_to_copy);
    buffer[characters_to_copy] = 0;

    return characters_to_copy == length();
}

DeprecatedString DeprecatedString::isolated_copy() const
{
    if (m_impl->length() == 0)
        return empty();
    char* buffer;
    auto impl = StringImpl::create_uninitialized(length(), buffer);
    memcpy(buffer, m_impl->characters(), m_impl->length());
    return DeprecatedString(move(*impl));
}

DeprecatedString DeprecatedString::substring(size_t start, size_t length) const
{
    if (!length)
        return DeprecatedString::empty();
    VERIFY(!Checked<size_t>::addition_would_overflow(start, length));
    VERIFY(start + length <= m_impl->length());
    return { characters() + start, length };
}

DeprecatedString DeprecatedString::substring(size_t start) const
{
    VERIFY(start <= length());
    return { characters() + start, length() - start };
}

StringView DeprecatedString::substring_view(size_t start, size_t length) const
{
    VERIFY(!Checked<size_t>::addition_would_overflow(start, length));
    VERIFY(start + length <= m_impl->length());
    return { characters() + start, length };
}

StringView DeprecatedString::substring_view(size_t start) const
{
    VERIFY(start <= length());
    return { characters() + start, length() - start };
}

Vector<DeprecatedString> DeprecatedString::split(char separator, SplitBehavior split_behavior) const
{
    return split_limit(separator, 0, split_behavior);
}

Vector<DeprecatedString> DeprecatedString::split_limit(char separator, size_t limit, SplitBehavior split_behavior) const
{
    if (is_empty())
        return {};

    Vector<DeprecatedString> v;
    size_t substart = 0;
    bool keep_empty = has_flag(split_behavior, SplitBehavior::KeepEmpty);
    bool keep_separator = has_flag(split_behavior, SplitBehavior::KeepTrailingSeparator);
    for (size_t i = 0; i < length() && (v.size() + 1) != limit; ++i) {
        char ch = characters()[i];
        if (ch == separator) {
            size_t sublen = i - substart;
            if (sublen != 0 || keep_empty)
                v.append(substring(substart, keep_separator ? sublen + 1 : sublen));
            substart = i + 1;
        }
    }
    size_t taillen = length() - substart;
    if (taillen != 0 || keep_empty)
        v.append(substring(substart, taillen));
    return v;
}

Vector<StringView> DeprecatedString::split_view(Function<bool(char)> separator, SplitBehavior split_behavior) const
{
    if (is_empty())
        return {};

    Vector<StringView> v;
    size_t substart = 0;
    bool keep_empty = has_flag(split_behavior, SplitBehavior::KeepEmpty);
    bool keep_separator = has_flag(split_behavior, SplitBehavior::KeepTrailingSeparator);
    for (size_t i = 0; i < length(); ++i) {
        char ch = characters()[i];
        if (separator(ch)) {
            size_t sublen = i - substart;
            if (sublen != 0 || keep_empty)
                v.append(substring_view(substart, keep_separator ? sublen + 1 : sublen));
            substart = i + 1;
        }
    }
    size_t taillen = length() - substart;
    if (taillen != 0 || keep_empty)
        v.append(substring_view(substart, taillen));
    return v;
}

Vector<StringView> DeprecatedString::split_view(char const separator, SplitBehavior split_behavior) const
{
    return split_view([separator](char ch) { return ch == separator; }, split_behavior);
}

ByteBuffer DeprecatedString::to_byte_buffer() const
{
    // FIXME: Handle OOM failure.
    return ByteBuffer::copy(bytes()).release_value_but_fixme_should_propagate_errors();
}

template<typename T>
Optional<T> DeprecatedString::to_int(TrimWhitespace trim_whitespace) const
{
    return StringUtils::convert_to_int<T>(view(), trim_whitespace);
}

template Optional<i8> DeprecatedString::to_int(TrimWhitespace) const;
template Optional<i16> DeprecatedString::to_int(TrimWhitespace) const;
template Optional<i32> DeprecatedString::to_int(TrimWhitespace) const;
template Optional<long> DeprecatedString::to_int(TrimWhitespace) const;
template Optional<long long> DeprecatedString::to_int(TrimWhitespace) const;

template<typename T>
Optional<T> DeprecatedString::to_uint(TrimWhitespace trim_whitespace) const
{
    return StringUtils::convert_to_uint<T>(view(), trim_whitespace);
}

template Optional<u8> DeprecatedString::to_uint(TrimWhitespace) const;
template Optional<u16> DeprecatedString::to_uint(TrimWhitespace) const;
template Optional<u32> DeprecatedString::to_uint(TrimWhitespace) const;
template Optional<unsigned long> DeprecatedString::to_uint(TrimWhitespace) const;
template Optional<unsigned long long> DeprecatedString::to_uint(TrimWhitespace) const;

#ifndef KERNEL
Optional<double> DeprecatedString::to_double(TrimWhitespace trim_whitespace) const
{
    return StringUtils::convert_to_floating_point<double>(*this, trim_whitespace);
}

Optional<float> DeprecatedString::to_float(TrimWhitespace trim_whitespace) const
{
    return StringUtils::convert_to_floating_point<float>(*this, trim_whitespace);
}
#endif

bool DeprecatedString::starts_with(StringView str, CaseSensitivity case_sensitivity) const
{
    return StringUtils::starts_with(*this, str, case_sensitivity);
}

bool DeprecatedString::starts_with(char ch) const
{
    if (is_empty())
        return false;
    return characters()[0] == ch;
}

bool DeprecatedString::ends_with(StringView str, CaseSensitivity case_sensitivity) const
{
    return StringUtils::ends_with(*this, str, case_sensitivity);
}

bool DeprecatedString::ends_with(char ch) const
{
    if (is_empty())
        return false;
    return characters()[length() - 1] == ch;
}

DeprecatedString DeprecatedString::repeated(char ch, size_t count)
{
    if (!count)
        return empty();
    char* buffer;
    auto impl = StringImpl::create_uninitialized(count, buffer);
    memset(buffer, ch, count);
    return *impl;
}

DeprecatedString DeprecatedString::repeated(StringView string, size_t count)
{
    if (!count || string.is_empty())
        return empty();
    char* buffer;
    auto impl = StringImpl::create_uninitialized(count * string.length(), buffer);
    for (size_t i = 0; i < count; i++)
        __builtin_memcpy(buffer + i * string.length(), string.characters_without_null_termination(), string.length());
    return *impl;
}

DeprecatedString DeprecatedString::bijective_base_from(size_t value, unsigned base, StringView map)
{
    value++;
    if (map.is_null())
        map = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"sv;

    VERIFY(base >= 2 && base <= map.length());

    // The '8 bits per byte' assumption may need to go?
    Array<char, round_up_to_power_of_two(sizeof(size_t) * 8 + 1, 2)> buffer;
    size_t i = 0;
    do {
        auto remainder = value % base;
        auto new_value = value / base;
        if (remainder == 0) {
            new_value--;
            remainder = map.length();
        }

        buffer[i++] = map[remainder - 1];
        value = new_value;
    } while (value > 0);

    for (size_t j = 0; j < i / 2; ++j)
        swap(buffer[j], buffer[i - j - 1]);

    return DeprecatedString { ReadonlyBytes(buffer.data(), i) };
}

DeprecatedString DeprecatedString::roman_number_from(size_t value)
{
    if (value > 3999)
        return DeprecatedString::number(value);

    StringBuilder builder;

    while (value > 0) {
        if (value >= 1000) {
            builder.append('M');
            value -= 1000;
        } else if (value >= 900) {
            builder.append("CM"sv);
            value -= 900;
        } else if (value >= 500) {
            builder.append('D');
            value -= 500;
        } else if (value >= 400) {
            builder.append("CD"sv);
            value -= 400;
        } else if (value >= 100) {
            builder.append('C');
            value -= 100;
        } else if (value >= 90) {
            builder.append("XC"sv);
            value -= 90;
        } else if (value >= 50) {
            builder.append('L');
            value -= 50;
        } else if (value >= 40) {
            builder.append("XL"sv);
            value -= 40;
        } else if (value >= 10) {
            builder.append('X');
            value -= 10;
        } else if (value == 9) {
            builder.append("IX"sv);
            value -= 9;
        } else if (value >= 5 && value <= 8) {
            builder.append('V');
            value -= 5;
        } else if (value == 4) {
            builder.append("IV"sv);
            value -= 4;
        } else if (value <= 3) {
            builder.append('I');
            value -= 1;
        }
    }

    return builder.to_deprecated_string();
}

bool DeprecatedString::matches(StringView mask, Vector<MaskSpan>& mask_spans, CaseSensitivity case_sensitivity) const
{
    return StringUtils::matches(*this, mask, case_sensitivity, &mask_spans);
}

bool DeprecatedString::matches(StringView mask, CaseSensitivity case_sensitivity) const
{
    return StringUtils::matches(*this, mask, case_sensitivity);
}

bool DeprecatedString::contains(StringView needle, CaseSensitivity case_sensitivity) const
{
    return StringUtils::contains(*this, needle, case_sensitivity);
}

bool DeprecatedString::contains(char needle, CaseSensitivity case_sensitivity) const
{
    return StringUtils::contains(*this, StringView(&needle, 1), case_sensitivity);
}

bool DeprecatedString::equals_ignoring_ascii_case(StringView other) const
{
    return StringUtils::equals_ignoring_ascii_case(view(), other);
}

DeprecatedString DeprecatedString::reverse() const
{
    StringBuilder reversed_string(length());
    for (size_t i = length(); i-- > 0;) {
        reversed_string.append(characters()[i]);
    }
    return reversed_string.to_deprecated_string();
}

DeprecatedString escape_html_entities(StringView html)
{
    StringBuilder builder;
    for (size_t i = 0; i < html.length(); ++i) {
        if (html[i] == '<')
            builder.append("&lt;"sv);
        else if (html[i] == '>')
            builder.append("&gt;"sv);
        else if (html[i] == '&')
            builder.append("&amp;"sv);
        else if (html[i] == '"')
            builder.append("&quot;"sv);
        else
            builder.append(html[i]);
    }
    return builder.to_deprecated_string();
}

DeprecatedString::DeprecatedString(DeprecatedFlyString const& string)
    : m_impl(*(string.impl() ?: &StringImpl::the_empty_stringimpl()))
{
}

DeprecatedString DeprecatedString::to_lowercase() const
{
    return m_impl->to_lowercase();
}

DeprecatedString DeprecatedString::to_uppercase() const
{
    return m_impl->to_uppercase();
}

DeprecatedString DeprecatedString::to_snakecase() const
{
    return StringUtils::to_snakecase(*this);
}

DeprecatedString DeprecatedString::to_titlecase() const
{
    return StringUtils::to_titlecase(*this);
}

DeprecatedString DeprecatedString::invert_case() const
{
    return StringUtils::invert_case(*this);
}

bool DeprecatedString::operator==(char const* cstring) const
{
    if (!cstring)
        return is_empty();

    return view() == cstring;
}

DeprecatedString DeprecatedString::vformatted(StringView fmtstr, TypeErasedFormatParams& params)
{
    StringBuilder builder;
    MUST(vformat(builder, fmtstr, params));
    return builder.to_deprecated_string();
}

Vector<size_t> DeprecatedString::find_all(StringView needle) const
{
    return StringUtils::find_all(*this, needle);
}

DeprecatedStringCodePointIterator DeprecatedString::code_points() const
{
    return DeprecatedStringCodePointIterator(*this);
}

ErrorOr<DeprecatedString> DeprecatedString::from_utf8(ReadonlyBytes bytes)
{
    if (!Utf8View(bytes).validate())
        return Error::from_string_literal("DeprecatedString::from_utf8: Input was not valid UTF-8");
    return DeprecatedString { *StringImpl::create(bytes) };
}

}
