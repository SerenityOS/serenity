/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/Checked.h>
#include <AK/FlyString.h>
#include <AK/Format.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <stdlib.h>

namespace AK {

namespace Detail {

class StringData final : public RefCounted<StringData> {
public:
    static ErrorOr<NonnullRefPtr<StringData>> create_uninitialized(size_t, u8*& buffer);
    static ErrorOr<NonnullRefPtr<StringData>> create_substring(StringData const& superstring, size_t start, size_t byte_count);
    static ErrorOr<NonnullRefPtr<StringData>> from_utf8(char const* utf8_bytes, size_t);

    struct SubstringData {
        StringData const* superstring { nullptr };
        u32 start_offset { 0 };
    };

    void operator delete(void* ptr);

    ~StringData();

    SubstringData const& substring_data() const
    {
        return *reinterpret_cast<SubstringData const*>(m_bytes_or_substring_data);
    }

    // NOTE: There is no guarantee about null-termination.
    ReadonlyBytes bytes() const
    {
        if (m_substring) {
            auto const& data = substring_data();
            return data.superstring->bytes().slice(data.start_offset, m_byte_count);
        }
        return { &m_bytes_or_substring_data[0], m_byte_count };
    }

    StringView bytes_as_string_view() const { return { bytes() }; }

    bool operator==(StringData const& other) const
    {
        return bytes_as_string_view() == other.bytes_as_string_view();
    }

    unsigned hash() const
    {
        if (!m_has_hash)
            compute_hash();
        return m_hash;
    }

    bool is_fly_string() const { return m_is_fly_string; }
    void set_fly_string(bool is_fly_string) { m_is_fly_string = is_fly_string; }

private:
    explicit StringData(size_t byte_count);
    StringData(StringData const& superstring, size_t start, size_t byte_count);

    void compute_hash() const;

    u32 m_byte_count { 0 };
    mutable unsigned m_hash { 0 };
    mutable bool m_has_hash { false };
    bool m_substring { false };
    bool m_is_fly_string { false };

    u8 m_bytes_or_substring_data[0];
};

void StringData::operator delete(void* ptr)
{
    free(ptr);
}

StringData::StringData(size_t byte_count)
    : m_byte_count(byte_count)
{
}

StringData::StringData(StringData const& superstring, size_t start, size_t byte_count)
    : m_byte_count(byte_count)
    , m_substring(true)
{
    auto& data = const_cast<SubstringData&>(substring_data());
    data.start_offset = start;
    data.superstring = &superstring;
    superstring.ref();
}

StringData::~StringData()
{
    if (m_is_fly_string)
        FlyString::did_destroy_fly_string_data({}, bytes_as_string_view());
    if (m_substring)
        substring_data().superstring->unref();
}

constexpr size_t allocation_size_for_string_data(size_t length)
{
    return sizeof(StringData) + (sizeof(char) * length);
}

ErrorOr<NonnullRefPtr<StringData>> StringData::create_uninitialized(size_t byte_count, u8*& buffer)
{
    VERIFY(byte_count);
    void* slot = malloc(allocation_size_for_string_data(byte_count));
    if (!slot) {
        return Error::from_errno(ENOMEM);
    }
    auto new_string_data = adopt_ref(*new (slot) StringData(byte_count));
    buffer = const_cast<u8*>(new_string_data->bytes().data());
    return new_string_data;
}

ErrorOr<NonnullRefPtr<StringData>> StringData::from_utf8(char const* utf8_data, size_t byte_count)
{
    // Strings of MAX_SHORT_STRING_BYTE_COUNT bytes or less should be handled by the String short string optimization.
    VERIFY(byte_count > String::MAX_SHORT_STRING_BYTE_COUNT);

    Utf8View view(StringView(utf8_data, byte_count));
    if (!view.validate())
        return Error::from_string_literal("StringData::from_utf8: Input was not valid UTF-8");

    VERIFY(utf8_data);
    u8* buffer = nullptr;
    auto new_string_data = TRY(create_uninitialized(byte_count, buffer));
    memcpy(buffer, utf8_data, byte_count * sizeof(char));
    return new_string_data;
}

ErrorOr<NonnullRefPtr<StringData>> StringData::create_substring(StringData const& superstring, size_t start, size_t byte_count)
{
    // Strings of MAX_SHORT_STRING_BYTE_COUNT bytes or less should be handled by the String short string optimization.
    VERIFY(byte_count > String::MAX_SHORT_STRING_BYTE_COUNT);

    void* slot = malloc(sizeof(StringData) + sizeof(StringData::SubstringData));
    if (!slot) {
        return Error::from_errno(ENOMEM);
    }
    return adopt_ref(*new (slot) StringData(superstring, start, byte_count));
}

void StringData::compute_hash() const
{
    auto bytes = this->bytes();
    if (bytes.size() == 0)
        m_hash = 0;
    else
        m_hash = string_hash(reinterpret_cast<char const*>(bytes.data()), bytes.size());
    m_has_hash = true;
}

}

String::String(NonnullRefPtr<Detail::StringData> data)
    : m_data(&data.leak_ref())
{
}

String::String(String const& other)
    : m_data(other.m_data)
{
    if (!is_short_string())
        m_data->ref();
}

String::String(String&& other)
    : m_data(exchange(other.m_data, nullptr))
{
    other.m_short_string.byte_count_and_short_string_flag = SHORT_STRING_FLAG;
}

String& String::operator=(String&& other)
{
    if (!is_short_string())
        m_data->unref();

    m_data = exchange(other.m_data, nullptr);
    other.m_short_string.byte_count_and_short_string_flag = SHORT_STRING_FLAG;
    return *this;
}

String& String::operator=(String const& other)
{
    if (&other != this) {
        m_data = other.m_data;
        if (!is_short_string())
            m_data->ref();
    }
    return *this;
}

void String::destroy_string()
{
    if (!is_short_string())
        m_data->unref();
}

ErrorOr<String> String::from_utf8(StringView view)
{
    if (view.length() <= MAX_SHORT_STRING_BYTE_COUNT) {
        ShortString short_string;
        if (!view.is_empty())
            memcpy(short_string.storage, view.characters_without_null_termination(), view.length());
        short_string.byte_count_and_short_string_flag = (view.length() << 1) | SHORT_STRING_FLAG;
        return String { short_string };
    }
    auto data = TRY(Detail::StringData::from_utf8(view.characters_without_null_termination(), view.length()));
    return String { move(data) };
}

ErrorOr<String> String::repeated(u32 code_point, size_t count)
{
    VERIFY(is_unicode(code_point));

    Array<u8, 4> code_point_as_utf8;
    size_t i = 0;

    size_t code_point_byte_length = UnicodeUtils::code_point_to_utf8(code_point, [&](auto byte) {
        code_point_as_utf8[i++] = static_cast<u8>(byte);
    });

    auto copy_to_buffer = [&](u8* buffer) {
        if (code_point_byte_length == 1) {
            memset(buffer, code_point_as_utf8[0], count);
            return;
        }

        for (i = 0; i < count; ++i)
            memcpy(buffer + (i * code_point_byte_length), code_point_as_utf8.data(), code_point_byte_length);
    };

    auto total_byte_count = code_point_byte_length * count;

    if (total_byte_count <= MAX_SHORT_STRING_BYTE_COUNT) {
        ShortString short_string;
        copy_to_buffer(short_string.storage);
        short_string.byte_count_and_short_string_flag = (total_byte_count << 1) | SHORT_STRING_FLAG;

        return String { short_string };
    }

    u8* buffer = nullptr;
    auto new_string_data = TRY(Detail::StringData::create_uninitialized(total_byte_count, buffer));
    copy_to_buffer(buffer);

    return String { move(new_string_data) };
}

StringView String::bytes_as_string_view() const
{
    return StringView(bytes());
}

ReadonlyBytes String::bytes() const
{
    if (is_short_string())
        return m_short_string.bytes();
    return m_data->bytes();
}

bool String::is_empty() const
{
    return bytes().size() == 0;
}

ErrorOr<String> String::vformatted(StringView fmtstr, TypeErasedFormatParams& params)
{
    StringBuilder builder;
    TRY(vformat(builder, fmtstr, params));
    return builder.to_string();
}

ErrorOr<Vector<String>> String::split(u32 separator, SplitBehavior split_behavior) const
{
    return split_limit(separator, 0, split_behavior);
}

ErrorOr<Vector<String>> String::split_limit(u32 separator, size_t limit, SplitBehavior split_behavior) const
{
    Vector<String> result;

    if (is_empty())
        return result;

    bool keep_empty = has_flag(split_behavior, SplitBehavior::KeepEmpty);

    size_t substring_start = 0;
    for (auto it = code_points().begin(); it != code_points().end() && (result.size() + 1) != limit; ++it) {
        u32 code_point = *it;
        if (code_point == separator) {
            size_t substring_length = code_points().iterator_offset(it) - substring_start;
            if (substring_length != 0 || keep_empty)
                TRY(result.try_append(TRY(substring_from_byte_offset_with_shared_superstring(substring_start, substring_length))));
            substring_start = code_points().iterator_offset(it) + it.underlying_code_point_length_in_bytes();
        }
    }
    size_t tail_length = code_points().byte_length() - substring_start;
    if (tail_length != 0 || keep_empty)
        TRY(result.try_append(TRY(substring_from_byte_offset_with_shared_superstring(substring_start, tail_length))));
    return result;
}

Optional<size_t> String::find_byte_offset(u32 code_point, size_t from_byte_offset) const
{
    auto code_points = this->code_points();
    if (from_byte_offset >= code_points.byte_length())
        return {};

    for (auto it = code_points.iterator_at_byte_offset(from_byte_offset); it != code_points.end(); ++it) {
        if (*it == code_point)
            return code_points.byte_offset_of(it);
    }

    return {};
}

bool String::operator==(String const& other) const
{
    if (is_short_string())
        return m_data == other.m_data;
    return bytes_as_string_view() == other.bytes_as_string_view();
}

bool String::operator==(FlyString const& other) const
{
    if (reinterpret_cast<uintptr_t>(m_data) == other.data({}))
        return true;
    return bytes_as_string_view() == other.bytes_as_string_view();
}

bool String::operator==(StringView other) const
{
    return bytes_as_string_view() == other;
}

ErrorOr<String> String::substring_from_byte_offset(size_t start, size_t byte_count) const
{
    if (!byte_count)
        return String {};
    return String::from_utf8(bytes_as_string_view().substring_view(start, byte_count));
}

ErrorOr<String> String::substring_from_byte_offset_with_shared_superstring(size_t start, size_t byte_count) const
{
    if (!byte_count)
        return String {};
    if (byte_count <= MAX_SHORT_STRING_BYTE_COUNT)
        return String::from_utf8(bytes_as_string_view().substring_view(start, byte_count));
    return String { TRY(Detail::StringData::create_substring(*m_data, start, byte_count)) };
}

bool String::operator==(char const* c_string) const
{
    return bytes_as_string_view() == c_string;
}

u32 String::hash() const
{
    if (is_short_string()) {
        auto bytes = this->bytes();
        return string_hash(reinterpret_cast<char const*>(bytes.data()), bytes.size());
    }
    return m_data->hash();
}

Utf8View String::code_points() const
{
    return Utf8View(bytes_as_string_view());
}

ErrorOr<void> Formatter<String>::format(FormatBuilder& builder, String const& utf8_string)
{
    return Formatter<StringView>::format(builder, utf8_string.bytes_as_string_view());
}

ErrorOr<String> String::replace(StringView needle, StringView replacement, ReplaceMode replace_mode) const
{
    return StringUtils::replace(*this, needle, replacement, replace_mode);
}

ErrorOr<String> String::reverse() const
{
    // FIXME: This handles multi-byte code points, but not e.g. grapheme clusters.
    // FIXME: We could avoid allocating a temporary vector if Utf8View supports reverse iteration.
    auto code_point_length = code_points().length();

    Vector<u32> code_points;
    TRY(code_points.try_ensure_capacity(code_point_length));

    for (auto code_point : this->code_points())
        code_points.unchecked_append(code_point);

    auto builder = TRY(StringBuilder::create(code_point_length * sizeof(u32)));
    while (!code_points.is_empty())
        TRY(builder.try_append_code_point(code_points.take_last()));

    return builder.to_string();
}

bool String::contains(StringView needle, CaseSensitivity case_sensitivity) const
{
    return StringUtils::contains(bytes_as_string_view(), needle, case_sensitivity);
}

bool String::contains(char needle, CaseSensitivity case_sensitivity) const
{
    return contains(StringView { &needle, 1 }, case_sensitivity);
}

bool String::is_short_string() const
{
    return has_short_string_bit(reinterpret_cast<uintptr_t>(m_data));
}

ReadonlyBytes String::ShortString::bytes() const
{
    return { storage, byte_count() };
}

size_t String::ShortString::byte_count() const
{
    return byte_count_and_short_string_flag >> 1;
}

unsigned Traits<String>::hash(String const& string)
{
    return string.hash();
}

String String::fly_string_data_to_string(Badge<FlyString>, uintptr_t const& data)
{
    if (has_short_string_bit(data))
        return String { *reinterpret_cast<ShortString const*>(&data) };

    auto const* string_data = reinterpret_cast<Detail::StringData const*>(data);
    return String { NonnullRefPtr<Detail::StringData>(*string_data) };
}

StringView String::fly_string_data_to_string_view(Badge<FlyString>, uintptr_t const& data)
{
    if (has_short_string_bit(data)) {
        auto const* short_string = reinterpret_cast<ShortString const*>(&data);
        return short_string->bytes();
    }

    auto const* string_data = reinterpret_cast<Detail::StringData const*>(data);
    return string_data->bytes_as_string_view();
}

uintptr_t String::to_fly_string_data(Badge<FlyString>) const
{
    return reinterpret_cast<uintptr_t>(m_data);
}

void String::ref_fly_string_data(Badge<FlyString>, uintptr_t data)
{
    if (has_short_string_bit(data))
        return;

    auto const* string_data = reinterpret_cast<Detail::StringData const*>(data);
    string_data->ref();
}

void String::unref_fly_string_data(Badge<FlyString>, uintptr_t data)
{
    if (has_short_string_bit(data))
        return;

    auto const* string_data = reinterpret_cast<Detail::StringData const*>(data);
    string_data->unref();
}

void String::did_create_fly_string(Badge<FlyString>) const
{
    VERIFY(!is_short_string());
    m_data->set_fly_string(true);
}

DeprecatedString String::to_deprecated_string() const
{
    return DeprecatedString(bytes_as_string_view());
}

ErrorOr<String> String::from_deprecated_string(DeprecatedString const& deprecated_string)
{
    Utf8View view(deprecated_string);
    if (!view.validate())
        return Error::from_string_literal("String::from_deprecated_string: Input was not valid UTF-8");
    return String::from_utf8(deprecated_string.view());
}

}
