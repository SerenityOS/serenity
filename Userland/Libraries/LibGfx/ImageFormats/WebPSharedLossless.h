/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCompress/Deflate.h>

namespace Gfx {

// WebP-lossless's CanonicalCodes are almost identical to deflate's.
// One difference is that codes with a single element in webp-lossless consume 0 bits to produce that single element,
// while they consume 1 bit in Compress::CanonicalCode. This class wraps Compress::CanonicalCode to handle the case
// where the codes contain just a single element, and dispatches to Compress::CanonicalCode else.
class CanonicalCode {
public:
    CanonicalCode()
        : m_code(0)
    {
    }

    static ErrorOr<CanonicalCode> from_bytes(ReadonlyBytes);
    ErrorOr<u32> read_symbol(LittleEndianInputBitStream&) const;

private:
    explicit CanonicalCode(u32 single_symbol)
        : m_code(single_symbol)
    {
    }

    explicit CanonicalCode(Compress::CanonicalCode code)
        : m_code(move(code))
    {
    }

    Variant<u32, Compress::CanonicalCode> m_code;
};

// https://developers.google.com/speed/webp/docs/webp_lossless_bitstream_specification#61_overview
// "From here on, we refer to this set as a prefix code group."
class PrefixCodeGroup {
public:
    PrefixCodeGroup() = default;
    PrefixCodeGroup(PrefixCodeGroup&&) = default;
    PrefixCodeGroup(PrefixCodeGroup const&) = delete;

    CanonicalCode& operator[](int i) { return m_codes[i]; }
    CanonicalCode const& operator[](int i) const { return m_codes[i]; }

private:
    Array<CanonicalCode, 5> m_codes;
};

}
