/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Format.h>
#include <AK/Forward.h>
#include <AK/Random.h>
#include <AK/Span.h>
#include <AK/Types.h>

namespace BitTorrent {

template<size_t size>
class FixedSizeByteString {
public:
    explicit FixedSizeByteString(ReadonlyBytes const& from_bytes)
    {
        VERIFY(from_bytes.size() == size);
        from_bytes.copy_to({ m_data, size });
    }

    FixedSizeByteString(FixedSizeByteString const& other)
    {
        memcpy(m_data, other.m_data, size);
    }

    [[nodiscard]] ReadonlyBytes bytes() const
    {
        return { m_data, size };
    }

    static FixedSizeByteString random()
    {
        auto string = FixedSizeByteString();
        fill_with_random({ string.m_data, size });
        return string;
    }

    constexpr FixedSizeByteString& operator=(FixedSizeByteString const& other) = default;

    constexpr FixedSizeByteString& operator=(FixedSizeByteString&& other) = default;

    constexpr bool operator==(FixedSizeByteString const& other) const
    {
        return bytes() == other.bytes();
    }

    constexpr bool operator==(ReadonlyBytes const& other) const
    {
        return bytes() == other;
    }

private:
    FixedSizeByteString() { memset(m_data, 0, size); }
    u8 m_data[size] {};
};

// FIXME: These would be better as Classes extending FixedSizeByteString<20> to make their usage type safe but it'd be good not to duplicate the Formatter and hash Trait for each of them.
using PeerId = FixedSizeByteString<20>;
using InfoHash = FixedSizeByteString<20>;

}

template<size_t size>
struct AK::Formatter<BitTorrent::FixedSizeByteString<size>> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, BitTorrent::FixedSizeByteString<size> const& value)
    {
        for (u8 c : value.bytes())
            TRY(Formatter<FormatString>::format(builder, "{:02X}"sv, c));
        return {};
    }
};

template<size_t size>
struct AK::Traits<BitTorrent::FixedSizeByteString<size>> : public GenericTraits<BitTorrent::FixedSizeByteString<size>> {
    static constexpr unsigned hash(BitTorrent::FixedSizeByteString<size> const& string)
    {
        return AK::Traits<Span<u8 const>>::hash(string.bytes());
    }
};
