/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <AK/FlyString.h>
#include <AK/StringBase.h>
#include <AK/StringData.h>

namespace AK::Detail {

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
