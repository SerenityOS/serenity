/*
 * Copyright (c) 2020, Ali Mohammad Pur <ali.mpfard@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/Result.h>
#include <AK/Types.h>
#include <LibCrypto/ASN1/ASN1.h>
#include <LibCrypto/BigInt/UnsignedBigInteger.h>

namespace Crypto::ASN1 {

enum class DecodeError {
    NoInput,
    NonConformingType,
    EndOfStream,
    NotEnoughData,
    EnteringNonConstructedTag,
    LeavingMainContext,
    InvalidInputFormat,
    Overflow,
    UnsupportedFormat,
};

class Decoder {
public:
    Decoder(ReadonlyBytes data)
    {
        m_stack.append(data);
    }

    // Read a tag without consuming it (and its data).
    Result<Tag, DecodeError> peek();

    bool eof() const;

    template<typename ValueType>
    struct TaggedValue {
        Tag tag;
        ValueType value;
    };

    template<typename ValueType>
    Result<ValueType, DecodeError> read()
    {
        if (m_stack.is_empty())
            return DecodeError::NoInput;

        if (eof())
            return DecodeError::EndOfStream;

        auto previous_position = m_stack;

        auto tag_or_error = peek();
        if (tag_or_error.is_error()) {
            m_stack = move(previous_position);
            return tag_or_error.error();
        }

        auto length_or_error = read_length();
        if (length_or_error.is_error()) {
            m_stack = move(previous_position);
            return length_or_error.error();
        }

        auto tag = tag_or_error.value();
        auto length = length_or_error.value();

        auto value_or_error = read_value<ValueType>(tag.class_, tag.kind, length);
        if (value_or_error.is_error()) {
            m_stack = move(previous_position);
            return value_or_error.error();
        }

        m_current_tag.clear();

        return value_or_error.release_value();
    }

    Optional<DecodeError> enter();
    Optional<DecodeError> leave();

private:
    template<typename ValueType, typename DecodedType>
    Result<ValueType, DecodeError> with_type_check(DecodedType&& value)
    {
        if constexpr (requires { ValueType { value }; })
            return ValueType { value };

        return DecodeError::NonConformingType;
    }

    template<typename ValueType, typename DecodedType>
    Result<ValueType, DecodeError> with_type_check(Result<DecodedType, DecodeError>&& value_or_error)
    {
        if (value_or_error.is_error())
            return value_or_error.error();

        auto&& value = value_or_error.value();
        if constexpr (requires { ValueType { value }; })
            return ValueType { value };

        return DecodeError::NonConformingType;
    }

    template<typename ValueType>
    Result<ValueType, DecodeError> read_value(Class klass, Kind kind, size_t length)
    {
        auto data_or_error = read_bytes(length);
        if (data_or_error.is_error())
            return data_or_error.error();
        auto data = data_or_error.value();

        if (klass != Class::Universal)
            return with_type_check<ValueType>(data);

        if (kind == Kind::Boolean)
            return with_type_check<ValueType>(decode_boolean(data));

        if (kind == Kind::Integer)
            return with_type_check<ValueType>(decode_arbitrary_sized_integer(data));

        if (kind == Kind::OctetString)
            return with_type_check<ValueType>(decode_octet_string(data));

        if (kind == Kind::Null)
            return with_type_check<ValueType>(decode_null(data));

        if (kind == Kind::ObjectIdentifier)
            return with_type_check<ValueType>(decode_object_identifier(data));

        if (kind == Kind::PrintableString || kind == Kind::IA5String || kind == Kind::UTCTime)
            return with_type_check<ValueType>(decode_printable_string(data));

        if (kind == Kind::Utf8String)
            return with_type_check<ValueType>(StringView { data.data(), data.size() });

        if (kind == Kind::BitString)
            return with_type_check<ValueType>(decode_bit_string(data));

        return with_type_check<ValueType>(data);
    }

    Result<Tag, DecodeError> read_tag();
    Result<size_t, DecodeError> read_length();
    Result<u8, DecodeError> read_byte();
    Result<ReadonlyBytes, DecodeError> read_bytes(size_t length);

    static Result<bool, DecodeError> decode_boolean(ReadonlyBytes);
    static Result<UnsignedBigInteger, DecodeError> decode_arbitrary_sized_integer(ReadonlyBytes);
    static Result<StringView, DecodeError> decode_octet_string(ReadonlyBytes);
    static Result<std::nullptr_t, DecodeError> decode_null(ReadonlyBytes);
    static Result<Vector<int>, DecodeError> decode_object_identifier(ReadonlyBytes);
    static Result<StringView, DecodeError> decode_printable_string(ReadonlyBytes);
    static Result<Bitmap, DecodeError> decode_bit_string(ReadonlyBytes);

    Vector<ReadonlyBytes> m_stack;
    Optional<Tag> m_current_tag;
};

}

template<>
struct AK::Formatter<Crypto::ASN1::DecodeError> : Formatter<StringView> {
    void format(FormatBuilder&, Crypto::ASN1::DecodeError);
};
