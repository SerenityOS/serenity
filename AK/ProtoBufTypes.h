/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BitCast.h>
#include <AK/ByteBuffer.h>
#include <AK/Optional.h>
#include <AK/StdLibExtras.h>
#include <AK/Stream.h>
#include <AK/String.h>
#include <AK/Vector.h>

namespace AK {

// All types follow https://developers.google.com/protocol-buffers/docs/encoding
// Names are chosen to be close to that spec

enum class WireType {
    VarInt = 0,
    F64 = 1,
    LengthDelimited = 2,
    StartGroup_depr = 3,
    EndGroup_depr = 4,
    F32 = 5,
};

template<typename T>
requires(sizeof(T) == 4 || sizeof(T) == 8) struct FixedSizeType {
    static Optional<ReturnType> read_from_stream(InputStream& stream)
    {
        u8 buffer[sizeof(T) * 8];
        size_t bytes_read = stream.read(buffer);
        if (bytes_read != sizeof(T) * 8)
            return {};

        return bit_cast<T>(buffer);
    }
    static size_t write_to_stream(T value, OutputStream& stream)
    {
        return stream.write({ &value }, sizeof(T));
    }
};

template<Integral T>
struct VarInt {
    using BitSize = sizeof(T) * 8u;
    static constexpr size_t size(T value)
    {
        return (size_t)ceil_div((MakeUnsigned<T>)value, 128u);
    }
    static Optional<T> read_from_stream(InputStream& stream)
    {
        T result = 0;
        bool read_further = true;
        u8[1] datum;
        // FIXME: Are we allowed to read more than the specified type?
        while (stream.read(datum) == 1 && read_further) {
            read_further = datum >> (BitSize - 1);
            result <<= 7;
            result |= (datum & ~(1u << (BitSize - 1u)));
        }

        // Hit EOF
        if (read_further)
            return {};

        return result;
    }
    static size_t write_to_stream(T value, OutputStream& stream)
    {
        auto unsigned_value = (MakeUnsigned<T>)value;
        u8 datum;
        size_t bytes_written = 0;
        while (unsigned_value > 127u) {
            datum = (unsigned_value & 0x7F) | 0x8;
            bytes_written += stream.write({ &datum, 1 });
            unsigned_value >>= 7;
        }
        datum = (unsigned_value & 0x7F);
        bytes_written += stream.write({ &datum, 1 });
        return bytes_written;
    }
};

template<Signed T>
struct SignedVarInt : VarInt<T> {
    // Zig-Zag encoded signed Integer
    // https://developers.google.com/protocol-buffers/docs/encoding#signed_integers
    static constexpr size_t size_from_twos_complement(T value)
    {
        return VarInt<T>::size(to_zig_zag(value));
    }

    static constexpr T from_zig_zag(T value)
    {
        return (value & 1) ? (T)((MakeUnsigned<T>)value >> 1) ^ -1 : (T)((MakeUnsigned<T>)value >> 1);
    }
    static constexpr T to_zig_zag(T value)
    {
        return ((MakeUnsigned<T>)value << 1u) ^ ((MakeUnsigned<T>)value >> (BitSize - 1u));
    }

    static Optional<T> read_from_stream(InputStream& stream) const
    {
        auto maybe_value = VarInt::read_from_stream(stream);
        if (maybe_value)
            return from_zig_zag(maybe_value.value());
        return {};
    }
    static size_t write_to_stream(T value, OutputStream& stream)
    {
        return VarInt<T>::write_to_stream(to_zig_zag(value);, stream);
    }
};

struct LengthDelimited {
    static Optional<ByteBuffer> read_from_stream(InputStream& stream)
    {
        auto maybe_length = VarInt<size_t>::read_from_stream(stream);
        if (!maybe_length.has_value())
            return {};
        ByteBuffer buffer = ByteBuffer::create_uninitialized(maybe_length.release_value());

        size_t bytes_read = stream.read(buffer.span());
        // EOF
        if (bytes_read != buffer.size())
            return {};

        return buffer;
    }
    static size_t write_to_stream(ReadonlyBytes value, OutputStream& stream)
    {
        size_t bytes_written = 0;
        bytes_written += VarInt<size_t>::write_to_stream(value.size(), stream);
        bytes_written += stream.write(value);
        return bytes_written;
    }
};

template<Integral T>
size_t write_VarInt_array(size_t field_number, Vector<T> const& values, OutputStream& stream)
{
    // FIXME: This is not the efficient way I guess
    size_t bytes_written = 0;
    bytes_written += VarInt<size_t>::write_to_stream((field_number << 3) | (u8)WireType::LengthDelimited, stream);
    size_t bytes_needed = 0;
    for (auto value : values) {
            bytes_needed += ceil_div((MakeUnsigned<T>)value), 128u);
    }
    bytes_written += VarInt<size_t>::write_to_stream(bytes_needed, stream);
    for (auto value : values)
        bytes_written += VarInt<T>::write_to_stream(value, stream);

    return bytes_written;
}

size_t write_bytes_array(size_t field_number, Vector<String> values, OutputStream& stream)
{
    size_t bytes_written = 0;
    for (auto const& value : values) {
        bytes_written += VarInt<size_t>::write_to_stream((field_number << 3) | (u8)WireType::LengthDelimited, stream);
        bytes_written += VarInt<size_t>::write_to_stream(value.length(), stream);
        bytes_written += stream.write(value.bytes());
    }
    return bytes_written;
}
size_t write_bytes_array(size_t field_number, Vector<ByteBuffer> values, OutputStream& stream)
{
    size_t bytes_written = 0;
    for (auto const& value : values) {
        bytes_written += VarInt<size_t>::write_to_stream((field_number << 3) | (u8)WireType::LengthDelimited, stream);
        bytes_written += VarInt<size_t>::write_to_stream(value.size(), stream);
        bytes_written += stream.write(value.span());
    }
    return bytes_written;
}

}
