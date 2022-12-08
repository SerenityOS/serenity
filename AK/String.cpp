/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/Format.h>
#include <AK/Memory.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
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

private:
    explicit StringData(size_t byte_count);
    StringData(StringData const& superstring, size_t start, size_t byte_count);

    void compute_hash() const;

    u32 m_byte_count { 0 };
    mutable unsigned m_hash { 0 };
    mutable bool m_has_hash { false };
    bool m_substring { false };

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
    if (m_substring)
        substring_data().superstring->unref();
}

constexpr size_t allocation_size_for_string_data(size_t length)
{
    return sizeof(StringData) + (sizeof(char) * length) + sizeof(char);
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

String::String(ShortString short_string)
    : m_short_string(short_string)
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
}

String& String::operator=(String&& other)
{
    if (!is_short_string())
        m_data->unref();

    m_data = exchange(other.m_data, nullptr);
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

String::~String()
{
    if (!is_short_string() && m_data)
        m_data->unref();
}

String::String()
{
    // This is an empty string, it's always short and zero-length.
    m_short_string.byte_count_and_short_string_flag = SHORT_STRING_FLAG;
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

bool String::operator==(String const& other) const
{
    if (is_short_string())
        return m_data == other.m_data;
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

bool String::is_short_string() const
{
    return reinterpret_cast<uintptr_t>(m_data) & SHORT_STRING_FLAG;
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
