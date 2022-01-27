/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Concepts.h>
#include <AK/Error.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/OwnPtr.h>
#include <AK/Span.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>
#include <LibCore/Stream.h>

namespace Core::Stream {

/// A stream wrapper class that allows you to read arbitrary amounts of bits
/// in big-endian order from another stream.
/// Note that this stream does not own its underlying stream, it merely takes a reference.
class BigEndianInputBitStream : public Stream {
public:
    static ErrorOr<NonnullOwnPtr<BigEndianInputBitStream>> construct(Stream& stream)
    {
        return adopt_nonnull_own_or_enomem<BigEndianInputBitStream>(new BigEndianInputBitStream(stream));
    }

    // ^Stream
    virtual bool is_readable() const override { return m_stream.is_readable(); }
    virtual ErrorOr<size_t> read(Bytes bytes) override
    {
        if (m_current_byte.has_value() && is_aligned_to_byte_boundary()) {
            bytes[0] = m_current_byte.release_value();
            return m_stream.read(bytes.slice(1));
        }
        align_to_byte_boundary();
        return m_stream.read(bytes);
    }
    virtual bool is_writable() const override { return m_stream.is_writable(); }
    virtual ErrorOr<size_t> write(ReadonlyBytes bytes) override { return m_stream.write(bytes); }
    virtual bool write_or_error(ReadonlyBytes bytes) override { return m_stream.write_or_error(bytes); }
    virtual bool is_eof() const override { return m_stream.is_eof() && !m_current_byte.has_value(); }
    virtual bool is_open() const override { return m_stream.is_open(); }
    virtual void close() override
    {
        m_stream.close();
        align_to_byte_boundary();
    }

    ErrorOr<bool> read_bit()
    {
        return read_bits<bool>(1);
    }
    /// Depending on the number of bits to read, the return type can be chosen appropriately.
    /// This avoids a bunch of static_cast<>'s for the user.
    // TODO: Support u128, u256 etc. as well: The concepts would be quite complex.
    template<Unsigned T = u64>
    ErrorOr<T> read_bits(size_t count)
    {
        if constexpr (IsSame<bool, T>) {
            VERIFY(count == 1);
        }
        T result = 0;

        size_t nread = 0;
        while (nread < count) {
            if (m_current_byte.has_value()) {
                if constexpr (!IsSame<bool, T> && !IsSame<u8, T>) {
                    // read as many bytes as possible directly
                    if (((count - nread) >= 8) && is_aligned_to_byte_boundary()) {
                        // shift existing data over
                        result <<= 8;
                        result |= m_current_byte.value();
                        nread += 8;
                        m_current_byte.clear();
                    } else {
                        const auto bit = (m_current_byte.value() >> (7 - m_bit_offset)) & 1;
                        result <<= 1;
                        result |= bit;
                        ++nread;
                        if (m_bit_offset++ == 7)
                            m_current_byte.clear();
                    }
                } else {
                    // Always take this branch for booleans or u8: there's no purpose in reading more than a single bit
                    const auto bit = (m_current_byte.value() >> (7 - m_bit_offset)) & 1;
                    if constexpr (IsSame<bool, T>)
                        result = bit;
                    else {
                        result <<= 1;
                        result |= bit;
                    }
                    ++nread;
                    if (m_bit_offset++ == 7)
                        m_current_byte.clear();
                }
            } else {
                auto temp_buffer = TRY(ByteBuffer::create_uninitialized(1));
                TRY(m_stream.read(temp_buffer.bytes()));
                m_current_byte = temp_buffer[0];
                m_bit_offset = 0;
            }
        }

        return result;
    }

    /// Discards any sub-byte stream positioning the input stream may be keeping track of.
    /// Non-bitwise reads will implicitly call this.
    void align_to_byte_boundary()
    {
        m_current_byte.clear();
        m_bit_offset = 0;
    }

    /// Whether we are (accidentally or intentionally) at a byte boundary right now.
    ALWAYS_INLINE bool is_aligned_to_byte_boundary() const { return m_bit_offset == 0; }

private:
    BigEndianInputBitStream(Stream& stream)
        : m_stream(stream)
    {
    }

    Optional<u8> m_current_byte;
    size_t m_bit_offset { 0 };
    Stream& m_stream;
};

}
