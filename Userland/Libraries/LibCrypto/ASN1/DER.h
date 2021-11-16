/*
 * Copyright (c) 2020, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BitmapView.h>
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

    Optional<DecodeError> drop()
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

        auto length = length_or_error.value();

        auto bytes_result = read_bytes(length);
        if (bytes_result.is_error()) {
            m_stack = move(previous_position);
            return bytes_result.error();
        }

        m_current_tag.clear();
        return {};
    }

    template<typename ValueType>
    Result<ValueType, DecodeError> read(Optional<Class> class_override = {}, Optional<Kind> kind_override = {})
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

        auto value_or_error = read_value<ValueType>(class_override.value_or(tag.class_), kind_override.value_or(tag.kind), length);
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

        if constexpr (IsSame<ValueType, bool> && !IsSame<DecodedType, bool>) {
            return DecodeError::NonConformingType;
        } else {
            auto&& value = value_or_error.value();
            if constexpr (requires { ValueType { value }; })
                return ValueType { value };
        }

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
    static Result<const BitmapView, DecodeError> decode_bit_string(ReadonlyBytes);

    Vector<ReadonlyBytes> m_stack;
    Optional<Tag> m_current_tag;
};

void pretty_print(Decoder&, OutputStream&, int indent = 0);

}

template<>
struct AK::Formatter<Crypto::ASN1::DecodeError> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder&, Crypto::ASN1::DecodeError);
};
