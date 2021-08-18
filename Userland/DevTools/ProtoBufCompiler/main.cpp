/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AnyOf.h>
#include <AK/CharacterTypes.h>
#include <AK/Debug.h>
#include <AK/FlyString.h>
#include <AK/Function.h>
#include <AK/GenericLexer.h>
#include <AK/HashMap.h>
#include <AK/HashTable.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
#include <LibCore/File.h>
#include <ctype.h>
#include <stdio.h>

// FIXME: Use a more intelligent Lexer and give more detailed error output

namespace WireTypes {
static FlyString VarInt = "VarInt";
static FlyString F32 = "F32";
static FlyString F64 = "F64";
static FlyString LengthDelimited = "LengthDelimited";
}

namespace FieldTypeNames {
static FlyString Bool = "bool";
static FlyString Int32 = "i32";
static FlyString Int64 = "i64";
static FlyString UInt32 = "u32";
static FlyString UInt64 = "u64";
static FlyString Float = "float";
static FlyString Double = "double";
static FlyString String = "String";
static FlyString Bytes = "ByteBuffer";
}

enum class FieldType : u8 {
    Bool = 0,
    Int32,
    Int64,
    SInt32,
    SInt64,
    UInt32,
    UInt64,
    Float,
    Double,
    Fixed32,
    Fixed64,
    String,
    Bytes,
    Custom
};

struct Field {
    StringView name;
    FieldType type;
    FlyString wire_type;
    FlyString type_name;
    StringView number;
    bool repeated { false };
    bool packed { false };
};

// Bites AK::Enum Concept
struct _Enum {
    StringView name;
    StringView content;
};

struct Message {
    String name;
    Vector<Field> fields;
    Vector<_Enum> enums;
    NonnullOwnPtrVector<Message> messages;
};

void consume_whitespace(GenericLexer& lexer)
{
    lexer.ignore_while([](char ch) { return isspace(ch); });
    if (lexer.peek() == '/' && lexer.peek(1) == '/')
        lexer.ignore_until([](char ch) { return ch == '\n'; });
}

void parse_enum(GenericLexer& lexer, Vector<_Enum>& enums)
{
    if (lexer.consume_specific("enum"sv)) {
        consume_whitespace(lexer);
        enums.append(
            { lexer.consume_until(' '),
                lexer.consume_until('}') });
    }
}
void parse_field(GenericLexer& lexer, Message& message)
{
    bool found_repeated = false;
    Field field {};
    if (lexer.consume_specific("repeated"sv)) {
        field.repeated = true;
        found_repeated = true;
        consume_whitespace(lexer);
    }
    if (!lexer.next_is_any_of("option"sv, "enum"sv, "message"sv)) {
        StringView type_name = lexer.consume_until(' ');
        HashTable<StringView> used_numbers {};
        // FIXME: Make this nicer:
        {
            if (type_name == "bool"sv) {
                field.type = FieldType::Bool;
                field.type_name = FieldTypeNames::Bool;
                field.wire_type = WireTypes::VarInt;
            } else if (type_name == "int32"sv) {
                field.type = FieldType::Int32;
                field.type_name = FieldTypeNames::Int32;
                field.wire_type = WireTypes::VarInt;
            } else if (type_name == "int64"sv) {
                field.type = FieldType::Int64;
                field.type_name = FieldTypeNames::Int64;
                field.wire_type = WireTypes::VarInt;
            } else if (type_name == "sint32"sv) {
                field.type = FieldType::SInt32;
                field.type_name = FieldTypeNames::Int32;
                field.wire_type = WireTypes::VarInt;
            } else if (type_name == "sint64"sv) {
                field.type = FieldType::SInt64;
                field.type_name = FieldTypeNames::Int64;
                field.wire_type = WireTypes::VarInt;
            } else if (type_name == "uint32"sv) {
                field.type = FieldType::UInt32;
                field.type_name = FieldTypeNames::UInt32;
                field.wire_type = WireTypes::VarInt;
            } else if (type_name == "uint64"sv) {
                field.type = FieldType::UInt64;
                field.type_name = FieldTypeNames::UInt64;
                field.wire_type = WireTypes::VarInt;
            } else if (type_name == "float"sv) {
                field.type = FieldType::Float;
                field.type_name = FieldTypeNames::Float;
                field.wire_type = WireTypes::F32;
            } else if (type_name == "double"sv) {
                field.type = FieldType::Double;
                field.type_name = FieldTypeNames::Double;
                field.wire_type = WireTypes::F64;
            } else if (type_name == "fixed32"sv) {
                field.type = FieldType::Fixed32;
                field.type_name = FieldTypeNames::Int32;
                field.wire_type = WireTypes::F32;
            } else if (type_name == "fixed64"sv) {
                field.type = FieldType::Fixed64;
                field.type_name = FieldTypeNames::Int64;
                field.wire_type = WireTypes::F64;

            } else if (type_name == "string"sv) {
                field.type = FieldType::String;
                field.type_name = FieldTypeNames::String;
                field.wire_type = WireTypes::LengthDelimited;
            } else if (type_name == "bytes"sv) {
                field.type = FieldType::Bytes;
                field.type_name = FieldTypeNames::Bytes;
                field.wire_type = WireTypes::LengthDelimited;
            } else {
                field.type = FieldType::Custom;
                // FIXME: FlyString Construction is not optimal here
                // FIXME: Enum detection (they are VarInts)
                field.type_name = move(type_name);
                field.wire_type = WireTypes::LengthDelimited;
            }
        }
        consume_whitespace(lexer);
        field.name = lexer.consume_until(' ');

        consume_whitespace(lexer);
        if (!lexer.consume_specific("="sv)) {
            warnln("Missing field-number assignment:");
            warnln("    {}::{} of type {}", message.name, field.name, type_name);
            VERIFY_NOT_REACHED();
        }
        consume_whitespace(lexer);
        size_t number_length;
        for (number_length = 0; is_ascii_digit(lexer.peek(number_length)); ++number_length)
            ;
        if (!number_length) {
            warnln("No field-number provided");
            VERIFY_NOT_REACHED();
        }
        field.number = lexer.consume(number_length);
        if (used_numbers.contains(field.number)) {
            warnln("Reuse of number {}", field.number);
            VERIFY_NOT_REACHED();
        }
        used_numbers.set(field.number);

        consume_whitespace(lexer);

        // FIXME: This is naive, also maybe get rid of the double checking
        if (lexer.consume_specific("[packed = true]"sv)) {
            if (!field.repeated) {
                warnln("Cannot pack a non repeated field");
                VERIFY_NOT_REACHED();
            }
            if (!is_any_of(field.type, FieldType::Int32, FieldType::Int64, FieldType::SInt32, FieldType::SInt64, FieldType::UInt32, FieldType::UInt64, FieldType::Float, FieldType::Double, FieldType::Fixed32, FieldType::Fixed64)) {
                warnln("Only primitive numeric types can be declared packed");
                VERIFY_NOT_REACHED();
            } else {
                warnln("In Proto3 all primitive numeric types are packed by default");
            }
            consume_whitespace(lexer);
        }
        if (field.repeated && is_any_of(field.type, FieldType::Int32, FieldType::Int64, FieldType::SInt32, FieldType::SInt64, FieldType::UInt32, FieldType::UInt64, FieldType::Float, FieldType::Double, FieldType::Fixed32, FieldType::Fixed64)) {
            field.packed = true;
            field.wire_type = WireTypes::LengthDelimited;
        }
        if (!lexer.consume_specific(';')) {
            warnln("Invalid syntax: Missing Semicolon");
            VERIFY_NOT_REACHED();
        }
        message.fields.append(move(field));
    } else if (found_repeated) {
        warnln("Invalid syntax: Trailing 'repeated'");
        VERIFY_NOT_REACHED();
    }
}
void parse_message(GenericLexer& lexer, NonnullOwnPtrVector<Message>& messages)
{
    if (lexer.consume_specific("message"sv)) {
        Message& message = *new Message();
        consume_whitespace(lexer);
        message.name = lexer.consume_until(' ');
        lexer.consume_specific('{');
        consume_whitespace(lexer);
        while (!lexer.next_is('}')) {
            parse_field(lexer, message);
            parse_enum(lexer, message.enums);
            parse_message(lexer, message.messages);
            consume_whitespace(lexer);
        }
        messages.append(adopt_own(message));
        lexer.consume_specific('}');
    }
}

void write_header(SourceGenerator& generator)
{
    generator.append("#pragma once\n"sv);
    generator.append(R"~~~(
#include <AK/Base64.h>
#include <AK/ByteBuffer.h>
#include <AK/JsonObject.h>
#include <AK/MemoryStream.h>
#include <AK/ProtoBufTypes.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>

)~~~"sv);
}

void write_enums(SourceGenerator& generator, Vector<_Enum> const& enums)
{
    for (auto const& _enum : enums) {
        generator.append("enum class "sv);
        generator.append(_enum.name);
        generator.append(" {\n"sv);
        generator.append(_enum.content);
        generator.append("};\n");
    }
}

void write_fields(SourceGenerator& generator, Vector<Field> const& fields)
{
    for (auto const& field : fields) {
        auto field_generator = generator.fork();
        field_generator.set("field.name"sv, field.name);
        field_generator.set("field.number"sv, field.number);
        field_generator.set("field.type_name"sv, field.type_name);
        field_generator.set("field.wire_type"sv, field.wire_type);
        if (field.repeated) {
            field_generator.append("    Vector<@field.type_name@> @field.name@ = {};\n"sv);
            continue;
        }
        field_generator.append("    @field.type_name@ @field.name@ = "sv);
        switch (field.type) {
        case FieldType::Bool:
            field_generator.append("false"sv);
            break;
        case FieldType::Int32:
        case FieldType::SInt32:
        case FieldType::UInt32:
        case FieldType::Int64:
        case FieldType::SInt64:
        case FieldType::UInt64:
        case FieldType::Fixed32:
        case FieldType::Fixed64:
        case FieldType::Float:
        case FieldType::Double:
            field_generator.append("0"sv);
            break;
        case FieldType::String:
            field_generator.append("String::empty()"sv);
            break;
        case FieldType::Bytes:
        case FieldType::Custom:
            field_generator.append("{}"sv);
            break;
        default:
            VERIFY_NOT_REACHED();
        }
        field_generator.append(";\n"sv);
    }
}

void write_reader(SourceGenerator& generator, Message const& message)
{
    generator.append(R"~~~(
    static @message.name@ read_from_stream(InputStream& stream)
    {
        @message.name@ message {};
        while (!stream.unreliable_eof()) {
            size_t field_specifier = AK::VarInt<size_t>::read_from_stream(stream).value();
    	    size_t field_number = field_specifier >> 3;
    	    u8 field_type = field_specifier & 0b111;
            switch (field_number) {
            )~~~"sv);
    for (auto const& field : message.fields) {
        auto field_generator = generator.fork();
        field_generator.set("field.name"sv, field.name);
        field_generator.set("field.number"sv, field.number);
        field_generator.set("field.type_name"sv, field.type_name);
        field_generator.set("field.wire_type"sv, field.wire_type);

        field_generator.append(R"~~~(case @field.number@: {
                VERIFY(field_type == (u8)AK::WireType::@field.wire_type@);
)~~~"sv);
        switch (field.type) {
        case FieldType::Bool:
        case FieldType::Int32:
        case FieldType::UInt32:
        case FieldType::Int64:
        case FieldType::UInt64:
            if (field.repeated) {
                field_generator.append(R"~~~(                auto maybe_buffer = AK::LengthDelimited::from_stream(stream);
                if (!buffer.has_value()) {
                    warnln("Unexpected End of Buffer while reading Array of VarInt");
                    VERIFY_NOT_REACHED();
                }
                InputMemoryStream array_stream {maybe_buffer.value().span()};
                while (!array_stream.unreliable_eof()) {
                    auto temp = AK::VarInt<@field.type_name@>::read_from_stream(array_stream);
                    if (!temp.has_value()) {
                        warnln("Unexpected End of Buffer while reading VarInt from Array");
                        VERIFY_NOT_REACHED();
                    }
                    message.@field.name@.append(temp.release_value());
                }
                VERIFY(array_stream.unreliable_eof());
                break;
            }
            )~~~"sv);
            } else {
                field_generator.append(R"~~~(                auto result = AK::VarInt<@field.type_name@>::read_from_stream(stream);
                if (!result.has_value()) {
                    warnln("Unexpected End of Buffer while reading VarInt");
                    VERIFY_NOT_REACHED();
                }
                message.@field.name@ = result.release_value();
                break;
            }
            )~~~"sv);
            }
            break;
        case FieldType::SInt32:
        case FieldType::SInt64:
            if (field.repeated) {
                field_generator.append(R"~~~(                auto maybe_buffer = AK::LengthDelimited::from_stream(stream);
                if (!buffer.has_value()) {
                    warnln("Unexpected End of Buffer while reading Array of SignedVarInt");
                    VERIFY_NOT_REACHED();
                }
                InputMemoryStream array_stream {maybe_buffer.value().span()};
                while (!array_stream.unreliable_eof()) {
                    auto temp = AK::SignedVarInt<@field.type_name@>::read_from_stream(array_stream);
                    if (!temp.has_value()) {
                        warnln("Unexpected End of Buffer while reading SignedVarInt from Array");
                        VERIFY_NOT_REACHED();
                    }
                    message.@field.name@.append(temp.release_value());
                }
                VERIFY(array_stream.unreliable_eof());
                break;
            }
            )~~~"sv);
            } else {
                field_generator.append(R"~~~(                auto result = AK::SignedVarInt<@field.type_name@>::read_from_stream(stream);
                if (!result.has_value()) {
                    warnln("Unexpected End of Buffer while reading SignedVarInt");
                    VERIFY_NOT_REACHED();
                }
                message.@field.name@ = result.release_value();
                break;
            }
            )~~~"sv);
            }
            break;
        case FieldType::Fixed32:
        case FieldType::Fixed64:
        case FieldType::Float:
        case FieldType::Double:
            if (field.repeated) {
                // FIXME: The checking wether the buffer size is actually true could be simplified here
                field_generator.append(R"~~~(                auto maybe_buffer = AK::LengthDelimited::from_stream(stream);
                if (!buffer.has_value()) {
                    warnln("Unexpected End of Buffer while reading Array of @field.name@");
                    VERIFY_NOT_REACHED();
                }
                InputMemoryStream array_stream {maybe_buffer.value().span()};
                while (!array_stream.unreliable_eof()) {
                    auto temp = AK::FixedSizeValue<@field.type_name@>::read_from_stream(array_stream);
                    if (!temp.has_value()) {
                        warnln("Unexpected End of Buffer while reading SignedVarInt from Array");
                        VERIFY_NOT_REACHED();
                    }
                    message.@field.name@.append(temp.release_value());
                }
                VERIFY(array_stream.unreliable_eof());
                break;
            }
            )~~~"sv);
            } else {
                field_generator.append(R"~~~(                auto result = AK::FixedSizeType<@field.type_name@>::read_from_stream(stream);
                if (!result.has_value()) {
                    warnln("Unexpected End of Buffer while reading @field.type_name@");
                    VERIFY_NOT_REACHED();
                }
                message.@field.name@ = result.release_value();
                break;
            }
            )~~~"sv);
            }
            break;
        case FieldType::String:
            // FIXME: Find a nicer way to take the data from the ByteBuffer
            //        Ideally find a way to adopt the buffer
            field_generator.append(R"~~~(                auto result = AK::LengthDelimited::read_from_stream(stream);
                if (!result.has_value()) {
                    warnln("Unexpected End of Buffer while reading String");
                    VERIFY_NOT_REACHED();
                }
                message.@field.name@)~~~"sv);
            if (field.repeated)
                field_generator.append(".append(move( String { result.value() }));\n                break;\n            }\n            "sv);
            else
                field_generator.append(" = String { StringView { result.value().data(), result.value().size()}};\n                break;\n            }\n            "sv);

            break;
        case FieldType::Bytes:
            generator.append(R"~~~(                auto result = AK::LengthDelimited::read_from_stream(stream);
                if (!result.has_value()) {
                    warnln("Unexpected End of Buffer while reading Bytes");
                    VERIFY_NOT_REACHED();
                }
                message.@field.name@)~~~"sv);
            if (field.repeated)
                field_generator.append(".append(result.release_value());\n                break;\n            }\n            "sv);
            else
                field_generator.append(" = result.release_value();\n                break;\n            }\n            "sv);
            break;
        case FieldType::Custom:
            // This encompasses another Message
            field_generator.append(R"~~~(                auto result = AK::LengthDelimited::read_from_stream(stream);
                if (!result.has_value()) {
                    warnln("Unexpected End of Buffer while reading @field.type_name@");
                    VERIFY_NOT_REACHED();
                }
                InputMemoryStream message_stream {result.release_value().span()};
                message.@field.name@)~~~"sv);
            if (field.repeated) {
                field_generator.append(".append(@field.type_name@::read_from_stream(message_stream));\n                break;\n            }\n            "sv);
            } else {
                field_generator.append(" = @field.type_name@::read_from_stream(message_stream);\n                break;\n            }\n            "sv);
            }
            break;
        default:
            warnln("Unknown FieldType: {}", (u8)field.type);
            VERIFY_NOT_REACHED();
        }
    }
    generator.append(R"~~~(default:
                switch (field_type) {
                case (u8)AK::WireType::VarInt: {
                    auto result = AK::VarInt<size_t>::read_from_stream(stream);
                    if (!result.has_value()) {
                        warnln("Unexpected End of Buffer while reading unused VarInt");
                        VERIFY_NOT_REACHED();
                    }
                }
                case (u8)AK::WireType::LengthDelimited: {
                    auto result = AK::LengthDelimited::read_from_stream(stream);
                    if (!result.has_value()) {
                        warnln("Unexpected End of Buffer while reading unused LengthDelimited value");
                        VERIFY_NOT_REACHED();
                    }
                }
                case (u8)AK::WireType::F32: {
                    auto result = AK::FixedSizeType<i32>::read_from_stream(stream);
                    if (!result.has_value()) {
                        warnln("Unexpected End of Buffer while reading unused value of size 32");
                        VERIFY_NOT_REACHED();
                    }
                }
                case (u8)AK::WireType::F64: {
                    auto result = AK::FixedSizeType<i64>::read_from_stream(stream);
                    if (!result.has_value()) {
                        warnln("Unexpected End of Buffer while reading unused value of size 64");
                        VERIFY_NOT_REACHED();
                    }
                }
            }
            }
        }
        return message;
    }
)~~~"sv);
}

void write_size_estimator(SourceGenerator& generator, Message const& message)
{
    generator.append(R"~~~(
    size_t estimate_size() const
    {
        size_t estimate = 0;
        size_t temp;
        )~~~"sv);
    for (auto const& field : message.fields) {
        auto field_generator = generator.fork();
        field_generator.set("field.name"sv, field.name);
        field_generator.set("field.number"sv, field.number);
        field_generator.set("field.type_name"sv, field.type_name);
        field_generator.set("field.wire_type"sv, field.wire_type);

        field_generator.append(R"~~~(// @field.name@
        estimate += ceil_div(@field.number@<<3,128);
        )~~~"sv);

        if (field.repeated) {
            // You can't/shouldn't shift booleans....
            if (field.packed && field.type != FieldType::Bool) {
                // these are only numeric types
                if (field.wire_type == WireTypes::VarInt) {
                    field_generator.append(R"~~~(estimate += ceil_div(@field.name@.size(), 128u);
        for (auto value : @field.name@) {
            estimate += ceil_div(@value@<<3u, 128u);
        }
        )~~~"sv);
                } else {
                    // run length:
                    // estimate += AK::VarInt<size_t>::size(@field.name@.size() * sizeof(@field.type_name@));
                    // ^= ceil_div(@field.name@.size() * sizeof(@field.type_name@), 128u);
                    // data:
                    // estimate += @field.name@.size() * sizeof(@field.type_name@);
                    // =>
                    // ceil_div(@field.name@.size() * sizeof(@field.type_name@), 128u) + @field.name@.size() * sizeof(@field.type_name@);
                    // ^= ceil_div(a, 128u) + a = ceil_div(2u * a, 128u)
                    // => estimate += AK::VarInt<size_t>::size(2 * @field.name@.size() * sizeof(@field.type_name@));
                    // same applies for other fields aswell
                    field_generator.append(R"~~~(estimate += AK::VarInt<size_t>::size(2u * @field.name@.size() * sizeof(@field.type_name@));
        )~~~");
                }
            } else {
                // Only String, Bytes and Custom (Message) should be here, all these
                // are of WireType LengthDelimited
                VERIFY(field.wire_type == WireTypes::LengthDelimited);
                switch (field.type) {
                case FieldType::String:
                    field_generator.append(R"~~~(temp = 0;
        for (auto const& value : @field.name@) {
            temp += value.length();
        }
        estimate += AK::VarInt<size_t>::size(2u * temp);
        )~~~");
                    break;
                case FieldType::Bytes:
                    field_generator.append(R"~~~(temp = 0;
        for (auto const& value : @field.name@) {
            temp += calculate_base64_encoded_length(value.span());
        }
        estimate += AK::VarInt<size_t>::size(2u * temp);
        )~~~");
                    break;
                case FieldType::Custom:
                    field_generator.append(R"~~~(temp = 0;
        for (auto const& value : @field.name@) {
            temp += value.estimate_size();
        }
        estimate += AK::VarInt<size_t>::size(2u * temp);
        )~~~");
                    break;
                default:
                    VERIFY_NOT_REACHED();
                }
            }
        } else {
            switch (field.type) {
            case FieldType::Bool:
            case FieldType::Int32:
            case FieldType::UInt32:
            case FieldType::Int64:
            case FieldType::UInt64:
                field_generator.append(R"~~~(estimate += AK::VarInt<@field.type_name@>::size(@field.name@);
        )~~~");
                break;
            case FieldType::SInt64:
            case FieldType::SInt32:
                field_generator.append(R"~~~(estimate += AK::SignedVarInt<@field.type_name@>::size_from_twos_complement(@field.name@);
        )~~~");
                break;
            case FieldType::Fixed32:
            case FieldType::Float:
                field_generator.append(R"~~~(estimate += 4u;
        )~~~");
                break;
            case FieldType::Fixed64:
            case FieldType::Double:
                field_generator.append(R"~~~(estimate += 8u;
        )~~~");
                break;
            case FieldType::String:
                field_generator.append(R"~~~(estimate += AK::VarInt<size_t>::size(2u * @field.name@.length());
        )~~~"sv);
                break;
            case FieldType::Bytes:
                field_generator.append(R"~~~(temp = calculate_base64_encoded_length(value.span());
        estimate += AK::VarInt<size_t>::size(2u * temp);
        )~~~"sv);
                break;
            case FieldType::Custom:
                field_generator.append(R"~~~(temp = @field.name@.estimate_size();
        estimate += AK::VarInt<size_t>::size(2u * temp);
        )~~~"sv);
                break;
            default:
                VERIFY_NOT_REACHED();
            }
        }
    }
    generator.append("}\n"sv);
}

void write_writer(SourceGenerator& generator, Message const& message)
{
    generator.append(R"~~~(
    size_t write_to_stream(OutputStream& stream) const
    {
        size_t bytes_written = 0;
        )~~~"sv);
    for (auto const& field : message.fields) {
        auto field_generator = generator.fork();
        field_generator.set("field.name"sv, field.name);
        field_generator.set("field.number"sv, field.number);
        field_generator.set("field.type_name"sv, field.type_name);
        field_generator.set("field.wire_type"sv, field.wire_type);
        if (field.repeated) {
            if (field.packed) {
                // these are only numeric types
                if (field.wire_type == WireTypes::VarInt) {
                    field_generator.append(R"~~~(// Writing @field.name@
        AK::write_VarInt_array(@field.number@, @field.name@, stream);
        )~~~"sv);
                } else {
                    VERIFY(is_any_of(field.wire_type, WireTypes::F32, WireTypes::F64));
                    field_generator.append(R"~~~(// Writing @field.name@
        bytes_written += AK::VarInt<size_t>::write_to_stream((@field.number@ << 3) | (u8) AK::WireType::LengthDelimited, stream);
        bytes_written += AK::VarInt<size_t>::write_to_stream(sizeof(@field.type@)*@field.name@.size(), stream);
        for (auto value : @field.name@)
            bytes_written += FixedSizeType<@field.type_name@>::write_to_stream(value, stream);
        )~~~"sv);
                }
            } else {
                // Only String, Bytes and Custom (Message) should be here, all these
                // are of WireType LengthDelimited
                VERIFY(field.wire_type == WireTypes::LengthDelimited);
                switch (field.type) {
                case FieldType::String:
                case FieldType::Bytes:
                    field_generator.append(R"~~~(// Writing @field.name@
        bytes_written += AK::write_bytes_array(@field.number@, @field.name@, stream);
        )~~~"sv);
                    break;
                case FieldType::Custom:
                    field_generator.append(R"~~~(// Writing @field.name@
        for (auto const& value : @field.name@) {
            bytes_written += AK::VarInt<size_t>::write_to_stream((@field.number@ << 3) | (u8)AK::WireType::LengthDelimited, stream);
            bytes_written += value.write_to_stream(stream);
        }
        )~~~"sv);
                    break;
                default:
                    VERIFY_NOT_REACHED();
                }
            }
        } else {
            switch (field.type) {
            case FieldType::Bool:
            case FieldType::Int32:
            case FieldType::UInt32:
            case FieldType::Int64:
            case FieldType::UInt64:
                field_generator.append(R"~~~(// Writing @field.name@
        bytes_written += AK::VarInt<size_t>::write_to_stream((@field.number@ << 3) | (u8)AK::WireType::VarInt, stream);
        bytes_written += AK::VarInt<@field.type_name@>::write_to_stream(@field.name@, stream);
        )~~~"sv);
                break;
            case FieldType::SInt64:
            case FieldType::SInt32:
                field_generator.append(R"~~~(// Writing @field.name@
        bytes_written += AK::VarInt<size_t>::write_to_stream((@field.number@ << 3) | (u8)AK::WireType::VarInt, stream);
        bytes_written += AK::SignedVarInt<@field.type_name@>::write_to_stream(@field.name@, stream);
        )~~~"sv);
                break;
            case FieldType::Fixed32:
            case FieldType::Float:
                field_generator.append(R"~~~(// Writing @field.name@
        bytes_written += AK::VarInt<size_t>::write_to_stream((@field.number@ << 3) | (u8)AK::WireType::F32, stream);
        bytes_written += AK::FixedSizeType<@field.type_name@>::write_to_stream(@field.name@, stream);
        )~~~"sv);
                break;
            case FieldType::Fixed64:
            case FieldType::Double:
                field_generator.append(R"~~~(// Writing @field.name@
        bytes_written += AK::VarInt<size_t>::write_to_stream((@field.number@ << 3) | (u8)AK::WireType::F64, stream);
        bytes_written += AK::FixedSizeType<@field.type_name@>::write_to_stream(@field.name@, stream);
        )~~~"sv);
                break;
            case FieldType::String:
                field_generator.append(R"~~~(// Writing @field.name@
        bytes_written += AK::VarInt<size_t>::write_to_stream((@field.number@ << 3) | (u8)AK::WireType::LengthDelimited, stream);
        bytes_written += AK::LengthDelimited::write_to_stream(@field.name@.bytes(), stream);
        )~~~"sv);
                break;
            case FieldType::Bytes:
                field_generator.append(R"~~~(// Writing @field.name@
        bytes_written += AK::VarInt<size_t>::write_to_stream((@field.number@ << 3) | (u8)AK::WireType::LengthDelimited, stream);
        bytes_written += AK::LengthDelimited::write_to_stream(@field.name@.span(), stream);
        )~~~"sv);
                break;
            case FieldType::Custom:
                field_generator.append(R"~~~(// Writing @field.name@
        bytes_written += AK::VarInt<size_t>::write_to_stream((@field.number@ << 3) | (u8)AK::WireType::LengthDelimited, stream);
        bytes_written += AK::VarInt<size_t>::write_to_stream(@field.name@.estimate_size());
        bytes_written += @field.name@.write_to_stream(stream);
        )~~~"sv);
                break;
            default:
                VERIFY_NOT_REACHED();
            }
        }
    }
    generator.append(R"~~~(
        return bytes_written;
    })~~~"sv);
}

void write_messages(SourceGenerator& generator, NonnullOwnPtrVector<Message> const& messages)
{
    for (auto const& message : messages) {
        auto message_generator = generator.fork();
        message_generator.set("message.name", message.name);
        message_generator.append("struct @message.name@ {\n"sv);
        write_enums(message_generator, message.enums);
        write_messages(message_generator, message.messages);
        write_fields(message_generator, message.fields);
        write_reader(message_generator, message);
        write_size_estimator(message_generator, message);
        write_writer(message_generator, message);
        generator.append("};\n"sv);
    }
}

int main(int argc, char** argv)
{
    if (argc != 2) {
        outln("usage: {} <IPC endpoint definition file>", argv[0]);
        return 0;
    }

    auto file = Core::File::construct(argv[1]);
    if (!file->open(Core::OpenMode::ReadOnly)) {
        warnln("Error: Cannot open {}: {}", argv[1], file->error_string());
        return 1;
    }

    auto file_contents = file->read_all();
    GenericLexer lexer(file_contents);

    Vector<_Enum> enums;
    NonnullOwnPtrVector<Message> messages;

    auto parse_any = [&]() {
        consume_whitespace(lexer);
        // FIXME: support options and syntax statements
        if (lexer.consume_specific("option"sv) || lexer.consume_specific("syntax"sv)) {
            lexer.consume_until(';');
            consume_whitespace(lexer);
            return;
        }

        parse_enum(lexer, enums);
        parse_message(lexer, messages);
    };

    size_t previous_offset;
    do {
        previous_offset = lexer.tell();
        parse_any();
    } while (lexer.tell() < file_contents.size() && previous_offset != lexer.tell());

    StringBuilder builder;
    SourceGenerator generator { builder };
    write_header(generator);
    write_enums(generator, enums);
    write_messages(generator, messages);
    outln("{}", generator.as_string_view());
}
