/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "BitField.h"
#include "FixedSizeByteString.h"
#include <AK/ByteBuffer.h>
#include <AK/DeprecatedString.h>
#include <AK/Endian.h>
#include <AK/MemoryStream.h>
#include <AK/TypeCasts.h>
#include <AK/Types.h>
#include <initializer_list>

namespace BitTorrent {

class Message {
public:
    enum class Type : u8 {
        Choke = 0x00,
        Unchoke = 0x01,
        Interested = 0x02,
        NotInterested = 0x03,
        Have = 0x04,
        Bitfield = 0x05,
        Request = 0x06,
        Piece = 0x07,
        Cancel = 0x08
    };
    virtual ~Message() = default;

    static DeprecatedString to_string(Type);
    virtual DeprecatedString to_string() const;

    u32 size() const { return serialized.size(); }

    ByteBuffer serialized;
    Type const type;

protected:
    class StreamWritable {
    public:
        StreamWritable(ReadonlyBytes bytes)
            : m_bytes(bytes)
        {
        }

        ErrorOr<void> write_to_stream(AK::Stream& stream) const
        {
            return stream.write_until_depleted(m_bytes);
        }

    private:
        ReadonlyBytes m_bytes;
    };

    template<typename... Payload>
    Message(Type type, Payload... payloads)
        : serialized(serialize(type, payloads...))
        , type(type)
    {
    }

    Message(SeekableStream& stream)
        : serialized(copy_already_serialized(stream))
        , type(MUST(stream.read_value<Type>()))
    {
    }

    // Null message used only for keepalives
    Message()
        : serialized(MUST(ByteBuffer::create_uninitialized(0)))
        , type(Type::Choke) // Bogus value. Should never be used
    {
    }

private:
    // TODO: make this variadic template argument optional so we don't have to use StreamWritable({}) for messages with no payload
    template<typename... Payload>
    static ByteBuffer serialize(Type message_type, Payload... payloads)
    {
        auto stream = AllocatingMemoryStream();

        MUST(stream.write_value(static_cast<u8>(message_type)));
        for (auto const& param : { payloads... }) {
            MUST(stream.write_value(param));
        }
        auto buffer = MUST(ByteBuffer::create_zeroed(stream.used_buffer_size()));
        MUST(stream.read_until_filled(buffer.bytes()));
        return buffer;
    }

    static ByteBuffer copy_already_serialized(SeekableStream& stream)
    {
        auto buffer = MUST(stream.read_until_eof());
        MUST(stream.seek(0, AK::SeekMode::SetPosition));
        return buffer;
    }
};

class BitFieldMessage : public Message {
public:
    BitFieldMessage(BitField bitfield)
        : Message(Type::Bitfield, bitfield)
        , bitfield(bitfield)
    {
    }

    BitFieldMessage(SeekableStream& stream, u64 size)
        : Message(stream)
        , bitfield(MUST(BitField::read_from_stream(stream, size)))
    {
    }
    BitField const bitfield;
    DeprecatedString to_string() const override;
};

class ChokeMessage : public Message {
public:
    ChokeMessage()
        : Message(Type::Choke, StreamWritable({}))
    {
    }
};

class HaveMessage : public Message {
public:
    HaveMessage(BigEndian<u32> piece_index)
        : Message(Type::Have, piece_index)
        , piece_index(piece_index)
    {
    }

    HaveMessage(SeekableStream& stream)
        : Message(stream)
        , piece_index(MUST(stream.read_value<BigEndian<u32>>()))
    {
    }

    BigEndian<u32> piece_index;
    DeprecatedString to_string() const override;
};

class InterestedMessage : public Message {
public:
    InterestedMessage()
        : Message(Type::Interested, StreamWritable({}))
    {
    }
};

class KeepAliveMessage : public Message {
};

class NotInterestedMessage : public Message {
public:
    NotInterestedMessage()
        : Message(Type::NotInterested, StreamWritable({}))
    {
    }
};

class PieceMessage : public Message {
public:
    PieceMessage(BigEndian<u32> piece_index, BigEndian<u32> begin_offset, ByteBuffer block)
        : Message(Type::Piece, piece_index, begin_offset)
        , piece_index(piece_index)
        , begin_offset(begin_offset)
        , block(block)
    {
        serialized.append(block);
    }

    PieceMessage(SeekableStream& stream)
        : Message(stream)
        , piece_index(MUST(stream.read_value<BigEndian<u32>>()))
        , begin_offset(MUST(stream.read_value<BigEndian<u32>>()))
        , block(MUST(stream.read_until_eof()))
    {
    }

    BigEndian<u32> const piece_index;
    BigEndian<u32> const begin_offset;
    ByteBuffer const block;
    DeprecatedString to_string() const override;
};

class RequestMessage : public Message {
public:
    RequestMessage(BigEndian<u32> piece_index, BigEndian<u32> piece_offset, BigEndian<u32> block_length)
        : Message(Type::Request, piece_index, piece_offset, block_length)
        , piece_index(piece_index)
        , piece_offset(piece_offset)
        , block_length(block_length)
    {
    }

    RequestMessage(SeekableStream& stream)
        : Message(stream)
        , piece_index(MUST(stream.read_value<BigEndian<u32>>()))
        , piece_offset(MUST(stream.read_value<BigEndian<u32>>()))
        , block_length(MUST(stream.read_value<BigEndian<u32>>()))
    {
    }

    BigEndian<u32> const piece_index;
    BigEndian<u32> const piece_offset;
    BigEndian<u32> const block_length;
    DeprecatedString to_string() const override;
};

class UnchokeMessage : public Message {
public:
    UnchokeMessage()
        : Message(Type::Unchoke, StreamWritable({}))
    {
    }
};

}

template<>
struct AK::Formatter<BitTorrent::Message> : AK::Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, BitTorrent::Message const& value)
    {
        return Formatter<StringView>::format(builder, value.to_string());
    }
};
