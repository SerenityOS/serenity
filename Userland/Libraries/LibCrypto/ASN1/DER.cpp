/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf8View.h>
#include <LibCrypto/ASN1/DER.h>

namespace Crypto::ASN1 {

Result<Tag, DecodeError> Decoder::read_tag()
{
    auto byte_or_error = read_byte();
    if (byte_or_error.is_error())
        return byte_or_error.error();

    auto byte = byte_or_error.value();
    u8 class_ = byte & 0xc0;
    u8 type = byte & 0x20;
    u8 kind = byte & 0x1f;

    if (kind == 0x1f) {
        kind = 0;
        while (byte & 0x80) {
            auto byte_or_error = read_byte();
            if (byte_or_error.is_error())
                return byte_or_error.error();

            byte = byte_or_error.value();
            kind = (kind << 7) | (byte & 0x7f);
        }
    }

    return Tag { (Kind)kind, (Class)class_, (Type)type };
}

Result<size_t, DecodeError> Decoder::read_length()
{
    auto byte_or_error = read_byte();
    if (byte_or_error.is_error())
        return byte_or_error.error();

    auto byte = byte_or_error.value();
    size_t length = byte;

    if (byte & 0x80) {
        auto count = byte & 0x7f;
        if (count == 0x7f)
            return DecodeError::InvalidInputFormat;
        auto data_or_error = read_bytes(count);
        if (data_or_error.is_error())
            return data_or_error.error();

        auto data = data_or_error.value();
        length = 0;

        if (data.size() > sizeof(size_t))
            return DecodeError::Overflow;

        for (auto&& byte : data)
            length = (length << 8) | (size_t)byte;
    }

    return length;
}

Result<u8, DecodeError> Decoder::read_byte()
{
    if (m_stack.is_empty())
        return DecodeError::NoInput;

    auto& entry = m_stack.last();
    if (entry.is_empty())
        return DecodeError::NotEnoughData;

    auto byte = entry[0];
    entry = entry.slice(1);

    return byte;
}

Result<ReadonlyBytes, DecodeError> Decoder::read_bytes(size_t length)
{
    if (m_stack.is_empty())
        return DecodeError::NoInput;

    auto& entry = m_stack.last();
    if (entry.size() < length)
        return DecodeError::NotEnoughData;

    auto bytes = entry.slice(0, length);
    entry = entry.slice(length);

    return bytes;
}

Result<bool, DecodeError> Decoder::decode_boolean(ReadonlyBytes data)
{
    if (data.size() != 1)
        return DecodeError::InvalidInputFormat;

    return data[0] == 0;
}

Result<UnsignedBigInteger, DecodeError> Decoder::decode_arbitrary_sized_integer(ReadonlyBytes data)
{
    if (data.size() < 1)
        return DecodeError::NotEnoughData;

    if (data.size() > 1
        && ((data[0] == 0xff && data[1] & 0x80)
            || (data[0] == 0x00 && !(data[1] & 0x80)))) {
        return DecodeError::InvalidInputFormat;
    }

    bool is_negative = data[0] & 0x80;
    if (is_negative)
        return DecodeError::UnsupportedFormat;

    return UnsignedBigInteger::import_data(data.data(), data.size());
}

Result<StringView, DecodeError> Decoder::decode_octet_string(ReadonlyBytes bytes)
{
    return StringView { bytes.data(), bytes.size() };
}

Result<std::nullptr_t, DecodeError> Decoder::decode_null(ReadonlyBytes data)
{
    if (data.size() != 0)
        return DecodeError::InvalidInputFormat;

    return nullptr;
}

Result<Vector<int>, DecodeError> Decoder::decode_object_identifier(ReadonlyBytes data)
{
    Vector<int> result;
    result.append(0); // Reserved space.

    u32 value = 0;
    for (auto&& byte : data) {
        if (value == 0 && byte == 0x80)
            return DecodeError::InvalidInputFormat;

        value = (value << 7) | (byte & 0x7f);
        if (!(byte & 0x80)) {
            result.append(value);
            value = 0;
        }
    }

    if (result.size() == 1 || result[1] >= 1600)
        return DecodeError::InvalidInputFormat;

    result[0] = result[1] / 40;
    result[1] = result[1] % 40;

    return result;
}

Result<StringView, DecodeError> Decoder::decode_printable_string(ReadonlyBytes data)
{
    Utf8View view { data };
    if (!view.validate())
        return DecodeError::InvalidInputFormat;

    return StringView { data };
}

Result<const BitmapView, DecodeError> Decoder::decode_bit_string(ReadonlyBytes data)
{
    if (data.size() < 1)
        return DecodeError::InvalidInputFormat;

    auto unused_bits = data[0];
    auto total_size_in_bits = (data.size() - 1) * 8;

    if (unused_bits > total_size_in_bits)
        return DecodeError::Overflow;

    return BitmapView { const_cast<u8*>(data.offset_pointer(1)), total_size_in_bits - unused_bits };
}

Result<Tag, DecodeError> Decoder::peek()
{
    if (m_stack.is_empty())
        return DecodeError::NoInput;

    if (eof())
        return DecodeError::EndOfStream;

    if (m_current_tag.has_value())
        return m_current_tag.value();

    auto tag_or_error = read_tag();
    if (tag_or_error.is_error())
        return tag_or_error.error();

    m_current_tag = tag_or_error.value();

    return m_current_tag.value();
}

bool Decoder::eof() const
{
    return m_stack.is_empty() || m_stack.last().is_empty();
}

Optional<DecodeError> Decoder::enter()
{
    if (m_stack.is_empty())
        return DecodeError::NoInput;

    auto tag_or_error = peek();
    if (tag_or_error.is_error())
        return tag_or_error.error();

    auto tag = tag_or_error.value();
    if (tag.type != Type::Constructed)
        return DecodeError::EnteringNonConstructedTag;

    auto length_or_error = read_length();
    if (length_or_error.is_error())
        return length_or_error.error();

    auto length = length_or_error.value();

    auto data_or_error = read_bytes(length);
    if (data_or_error.is_error())
        return data_or_error.error();

    m_current_tag.clear();

    auto data = data_or_error.value();
    m_stack.append(data);
    return {};
}

Optional<DecodeError> Decoder::leave()
{
    if (m_stack.is_empty())
        return DecodeError::NoInput;

    if (m_stack.size() == 1)
        return DecodeError::LeavingMainContext;

    m_stack.take_last();
    m_current_tag.clear();

    return {};
}

void pretty_print(Decoder& decoder, OutputStream& stream, int indent)
{
    while (!decoder.eof()) {
        auto tag = decoder.peek();
        if (tag.is_error()) {
            dbgln("PrettyPrint error: {}", tag.error());
            return;
        }

        StringBuilder builder;
        for (int i = 0; i < indent; ++i)
            builder.append(' ');
        builder.appendff("<{}> ", class_name(tag.value().class_));
        if (tag.value().type == Type::Constructed) {
            builder.appendff("[{}] {} ({})", type_name(tag.value().type), static_cast<u8>(tag.value().kind), kind_name(tag.value().kind));
            if (auto error = decoder.enter(); error.has_value()) {
                dbgln("Constructed PrettyPrint error: {}", error.value());
                return;
            }

            builder.append('\n');
            stream.write(builder.string_view().bytes());

            pretty_print(decoder, stream, indent + 2);

            if (auto error = decoder.leave(); error.has_value()) {
                dbgln("Constructed PrettyPrint error: {}", error.value());
                return;
            }

            continue;
        } else {
            if (tag.value().class_ != Class::Universal)
                builder.appendff("[{}] {} {}", type_name(tag.value().type), static_cast<u8>(tag.value().kind), kind_name(tag.value().kind));
            else
                builder.appendff("[{}] {}", type_name(tag.value().type), kind_name(tag.value().kind));
            switch (tag.value().kind) {
            case Kind::Eol: {
                auto value = decoder.read<ReadonlyBytes>();
                if (value.is_error()) {
                    dbgln("EOL PrettyPrint error: {}", value.error());
                    return;
                }
                break;
            }
            case Kind::Boolean: {
                auto value = decoder.read<bool>();
                if (value.is_error()) {
                    dbgln("Bool PrettyPrint error: {}", value.error());
                    return;
                }
                builder.appendff(" {}", value.value());
                break;
            }
            case Kind::Integer: {
                auto value = decoder.read<ReadonlyBytes>();
                if (value.is_error()) {
                    dbgln("Integer PrettyPrint error: {}", value.error());
                    return;
                }
                builder.append(" 0x");
                for (auto ch : value.value())
                    builder.appendff("{:0>2x}", ch);
                break;
            }
            case Kind::BitString: {
                auto value = decoder.read<const BitmapView>();
                if (value.is_error()) {
                    dbgln("BitString PrettyPrint error: {}", value.error());
                    return;
                }
                builder.append(" 0b");
                for (size_t i = 0; i < value.value().size(); ++i)
                    builder.append(value.value().get(i) ? '1' : '0');
                break;
            }
            case Kind::OctetString: {
                auto value = decoder.read<StringView>();
                if (value.is_error()) {
                    dbgln("OctetString PrettyPrint error: {}", value.error());
                    return;
                }
                builder.append(" 0x");
                for (auto ch : value.value())
                    builder.appendff("{:0>2x}", ch);
                break;
            }
            case Kind::Null: {
                auto value = decoder.read<decltype(nullptr)>();
                if (value.is_error()) {
                    dbgln("Bool PrettyPrint error: {}", value.error());
                    return;
                }
                break;
            }
            case Kind::ObjectIdentifier: {
                auto value = decoder.read<Vector<int>>();
                if (value.is_error()) {
                    dbgln("Identifier PrettyPrint error: {}", value.error());
                    return;
                }
                for (auto& id : value.value())
                    builder.appendff(" {}", id);
                break;
            }
            case Kind::UTCTime:
            case Kind::GeneralizedTime:
            case Kind::IA5String:
            case Kind::PrintableString: {
                auto value = decoder.read<StringView>();
                if (value.is_error()) {
                    dbgln("String PrettyPrint error: {}", value.error());
                    return;
                }
                builder.append(' ');
                builder.append(value.value());
                break;
            }
            case Kind::Utf8String: {
                auto value = decoder.read<Utf8View>();
                if (value.is_error()) {
                    dbgln("UTF8 PrettyPrint error: {}", value.error());
                    return;
                }
                builder.append(' ');
                for (auto cp : value.value())
                    builder.append_code_point(cp);
                break;
            }
            case Kind::Sequence:
            case Kind::Set:
                dbgln("Seq/Sequence PrettyPrint error: Unexpected Primitive");
                return;
            }
        }

        builder.append('\n');
        stream.write(builder.string_view().bytes());
    }
}

}

ErrorOr<void> AK::Formatter<Crypto::ASN1::DecodeError>::format(FormatBuilder& fmtbuilder, Crypto::ASN1::DecodeError error)
{
    using Crypto::ASN1::DecodeError;

    switch (error) {
    case DecodeError::NoInput:
        return Formatter<StringView>::format(fmtbuilder, "DecodeError(No input provided)");
    case DecodeError::NonConformingType:
        return Formatter<StringView>::format(fmtbuilder, "DecodeError(Tried to read with a non-conforming type)");
    case DecodeError::EndOfStream:
        return Formatter<StringView>::format(fmtbuilder, "DecodeError(End of stream)");
    case DecodeError::NotEnoughData:
        return Formatter<StringView>::format(fmtbuilder, "DecodeError(Not enough data)");
    case DecodeError::EnteringNonConstructedTag:
        return Formatter<StringView>::format(fmtbuilder, "DecodeError(Tried to enter a primitive tag)");
    case DecodeError::LeavingMainContext:
        return Formatter<StringView>::format(fmtbuilder, "DecodeError(Tried to leave the main context)");
    case DecodeError::InvalidInputFormat:
        return Formatter<StringView>::format(fmtbuilder, "DecodeError(Input data contained invalid syntax/data)");
    case DecodeError::Overflow:
        return Formatter<StringView>::format(fmtbuilder, "DecodeError(Construction would overflow)");
    case DecodeError::UnsupportedFormat:
        return Formatter<StringView>::format(fmtbuilder, "DecodeError(Input data format not supported by this parser)");
    default:
        return Formatter<StringView>::format(fmtbuilder, "DecodeError(Unknown)");
    }
}
