/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Span.h>
#include <AK/StringView.h>

namespace AK::Detail {

class StringData final : public RefCounted<StringData> {
public:
    static ErrorOr<NonnullRefPtr<StringData>> create_uninitialized(size_t, u8*& buffer);
    static ErrorOr<NonnullRefPtr<StringData>> create_substring(StringData const& superstring, size_t start, size_t byte_count);
    static ErrorOr<NonnullRefPtr<StringData>> from_stream(Stream&, size_t byte_count);

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
    void set_fly_string(bool is_fly_string) const { m_is_fly_string = is_fly_string; }

private:
    explicit StringData(size_t byte_count);
    StringData(StringData const& superstring, size_t start, size_t byte_count);

    void compute_hash() const;

    u32 m_byte_count { 0 };
    mutable unsigned m_hash { 0 };
    mutable bool m_has_hash { false };
    bool m_substring { false };
    mutable bool m_is_fly_string { false };

    alignas(SubstringData) u8 m_bytes_or_substring_data[0];
};

}
