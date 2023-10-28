/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <AK/FlyString.h>
#include <AK/StringBase.h>

namespace AK::Detail {

class StringData final : public RefCounted<StringData> {
public:
    static ErrorOr<NonnullRefPtr<StringData>> create_uninitialized(size_t byte_count, u8*& buffer)
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

    static ErrorOr<NonnullRefPtr<StringData>> create_substring(StringData const& superstring, size_t start, size_t byte_count)
    {
        // Strings of MAX_SHORT_STRING_BYTE_COUNT bytes or less should be handled by the String short string optimization.
        VERIFY(byte_count > MAX_SHORT_STRING_BYTE_COUNT);

        void* slot = malloc(sizeof(StringData) + sizeof(StringData::SubstringData));
        if (!slot) {
            return Error::from_errno(ENOMEM);
        }
        return adopt_ref(*new (slot) StringData(superstring, start, byte_count));
    }

    struct SubstringData {
        StringData const* superstring { nullptr };
        u32 start_offset { 0 };
    };

    void operator delete(void* ptr)
    {
        free(ptr);
    }

    void unref() const
    {
        if (m_is_fly_string && m_ref_count == 2) {
            m_is_fly_string = false; // Otherwise unref from did_destory_fly_string_data will cause infinite recursion.
            FlyString::did_destroy_fly_string_data({}, bytes_as_string_view());
        }
        RefCounted::unref();
    }

    ~StringData()
    {
        if (m_substring)
            substring_data().superstring->unref();
    }

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
    void set_fly_string(bool is_fly_string) const { m_is_fly_string = is_fly_string; }

    size_t byte_count() const { return m_byte_count; }

private:
    static constexpr size_t allocation_size_for_string_data(size_t length)
    {
        return sizeof(StringData) + (sizeof(char) * length);
    }

    explicit StringData(size_t byte_count)
        : m_byte_count(byte_count)
    {
    }

    StringData(StringData const& superstring, size_t start, size_t byte_count)
        : m_byte_count(byte_count)
        , m_substring(true)
    {
        auto& data = const_cast<SubstringData&>(substring_data());
        data.start_offset = start;
        data.superstring = &superstring;
        superstring.ref();
    }

    void compute_hash() const
    {
        auto bytes = this->bytes();
        if (bytes.size() == 0)
            m_hash = 0;
        else
            m_hash = string_hash(reinterpret_cast<char const*>(bytes.data()), bytes.size());
        m_has_hash = true;
    }

    u32 m_byte_count { 0 };
    mutable unsigned m_hash { 0 };
    mutable bool m_has_hash { false };
    bool m_substring { false };
    mutable bool m_is_fly_string { false };

    alignas(SubstringData) u8 m_bytes_or_substring_data[0];
};

ReadonlyBytes ShortString::bytes() const
{
    return { storage, byte_count() };
}

size_t ShortString::byte_count() const
{
    return byte_count_and_short_string_flag >> 1;
}

StringBase::StringBase(NonnullRefPtr<Detail::StringData const> data)
    : m_data(&data.leak_ref())
{
}

StringBase::StringBase(StringBase const& other)
    : m_data(other.m_data)
{
    if (!is_short_string())
        m_data->ref();
}

StringBase& StringBase::operator=(StringBase&& other)
{
    if (!is_short_string())
        m_data->unref();

    m_data = exchange(other.m_data, nullptr);
    other.m_short_string.byte_count_and_short_string_flag = SHORT_STRING_FLAG;
    return *this;
}

StringBase& StringBase::operator=(StringBase const& other)
{
    if (&other != this) {
        if (!is_short_string())
            m_data->unref();

        m_data = other.m_data;
        if (!is_short_string())
            m_data->ref();
    }
    return *this;
}

ReadonlyBytes StringBase::bytes() const
{
    if (is_short_string())
        return m_short_string.bytes();
    return m_data->bytes();
}

u32 StringBase::hash() const
{
    if (is_short_string()) {
        auto bytes = this->bytes();
        return string_hash(reinterpret_cast<char const*>(bytes.data()), bytes.size());
    }
    return m_data->hash();
}

size_t StringBase::byte_count() const
{
    if (is_short_string())
        return m_short_string.byte_count_and_short_string_flag >> 1;
    return m_data->byte_count();
}

bool StringBase::operator==(StringBase const& other) const
{
    if (is_short_string())
        return m_data == other.m_data;
    if (other.is_short_string())
        return false;
    if (m_data->is_fly_string() && other.m_data->is_fly_string())
        return m_data == other.m_data;
    return bytes() == other.bytes();
}

void StringBase::did_create_fly_string(Badge<FlyString>) const
{
    VERIFY(!is_short_string());
    m_data->set_fly_string(true);
}

ErrorOr<Bytes> StringBase::replace_with_uninitialized_buffer(size_t byte_count)
{
    if (byte_count <= MAX_SHORT_STRING_BYTE_COUNT)
        return replace_with_uninitialized_short_string(byte_count);

    u8* buffer = nullptr;
    destroy_string();
    m_data = &TRY(StringData::create_uninitialized(byte_count, buffer)).leak_ref();
    return Bytes { buffer, byte_count };
}

ErrorOr<StringBase> StringBase::substring_from_byte_offset_with_shared_superstring(size_t start, size_t length) const
{
    VERIFY(start + length <= byte_count());

    if (length == 0)
        return StringBase {};
    if (length <= MAX_SHORT_STRING_BYTE_COUNT) {
        StringBase result;
        bytes().slice(start, length).copy_to(result.replace_with_uninitialized_short_string(length));
        return result;
    }
    return StringBase { TRY(Detail::StringData::create_substring(*m_data, start, length)) };
}

void StringBase::destroy_string()
{
    if (!is_short_string())
        m_data->unref();
}

}
