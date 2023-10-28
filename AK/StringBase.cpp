/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBase.h>
#include <AK/StringInternals.h>

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

bool StringBase::is_short_string() const
{
    return has_short_string_bit(reinterpret_cast<uintptr_t>(m_data));
}

ReadonlyBytes StringBase::bytes() const
{
    if (is_short_string())
        return m_short_string.bytes();
    return m_data->bytes();
}

bool StringBase::operator==(StringBase const& other) const
{
    if (is_short_string())
        return m_data == other.m_data;
    return bytes() == other.bytes();
}

}
