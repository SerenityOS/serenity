/*
 * Copyright (c) 2023, Pierre Delagrave <pierre.delagrave@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BDecoder.h"
#include <AK/CharacterTypes.h>

namespace BitTorrent {

ErrorOr<BEncodingType> BDecoder::parse_bencoded(Stream& stream, Optional<u8> byte_already_read)
{
    u8 const next_byte = TRY(byte_already_read.try_value_or_lazy_evaluated([&] { return stream.read_value<u8>(); }));
    if (next_byte == 'i') {
        return BEncodingType(TRY(parse_integer(stream)));
    } else if (next_byte == 'l') {
        return parse_list(stream);
    } else if (next_byte == 'd') {
        return parse_dictionary(stream);
    } else if (is_ascii_digit(next_byte)) {
        return parse_byte_array(stream, next_byte);
    }

    return Error::from_string_literal("Can't parse type");
}

ErrorOr<i64> BDecoder::parse_integer(Stream& stream)
{
    auto integer_str = StringBuilder();

    TRY(for_each_byte_until('e', stream, [&](u8 const byte) -> ErrorOr<void> {
        if (is_ascii_digit(byte) || byte == '-') {
            if (byte == '-' && integer_str.length() != 0)
                return Error::from_string_literal("Invalid integer: When used, minus sign must be the first character.");
            else if (integer_str.string_view() == "0")
                return Error::from_string_literal("Invalid integer: Leading 0s not allowed.");
            else if (integer_str.string_view() == '-' && byte == '0')
                return Error::from_string_literal("Invalid integer: Leading 0s and -0 not allowed.");

            integer_str.append(byte);
            return {};
        } else {
            return Error::from_string_literal("Invalid integer, valid characters are 0-9 and -");
        }
    }));

    // BEP3 says there's no limit to integer size but let's keep it to i64 here.
    auto ret = AK::StringUtils::convert_to_int<i64>(integer_str.string_view());
    if (ret.has_value())
        return ret.value();
    else
        return Error::from_string_literal("Invalid integer, likely out of bound");
}

ErrorOr<ByteBuffer> BDecoder::parse_byte_array(Stream& stream, u8 first_byte)
{
    auto array_size_str = StringBuilder();
    array_size_str.append(first_byte);

    TRY(for_each_byte_until(':', stream, [&](u8 const byte) -> ErrorOr<void> {
        if (is_ascii_digit(byte))
            array_size_str.append(byte);
        else
            return Error::from_string_literal("Invalid byte array size");

        return {};
    }));

    auto array_size = AK::StringUtils::convert_to_uint<u64>(array_size_str.string_view(), AK::TrimWhitespace::No).value();
    auto buffer = TRY(ByteBuffer::create_uninitialized(array_size));
    TRY(stream.read_until_filled(buffer));

    return buffer;
}

ErrorOr<Dict> BDecoder::parse_dictionary(Stream& stream)
{
    auto dict = Dict();
    auto previous_key = DeprecatedString::empty();

    TRY(for_each_byte_until('e', stream, [&](u8 const byte) -> ErrorOr<void> {
        // The key is always expected to be a byte array (string).
        auto buffer = TRY(parse_byte_array(stream, byte));
        auto key = TRY(DeprecatedString::from_utf8(buffer.bytes()));
        if (key < previous_key)
            warnln("Invalid dictionary: entries key must be sorted"); // but many trackers don't sort them.
        BEncodingType const& value = TRY(parse_bencoded(stream, {}));
        dict.set(key, value);
        previous_key = key;
        return {};
    }));

    return dict;
}

ErrorOr<List> BDecoder::parse_list(Stream& stream)
{
    auto list = List();
    TRY(for_each_byte_until('e', stream, [&](u8 const byte) -> ErrorOr<void> {
        list.append(TRY(parse_bencoded(stream, byte)));
        return {};
    }));

    return list;
}

}
