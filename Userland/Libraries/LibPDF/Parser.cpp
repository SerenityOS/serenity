/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Document.h>
#include <LibPDF/Filter.h>
#include <LibPDF/Parser.h>
#include <LibTextCodec/Decoder.h>
#include <ctype.h>

namespace PDF {

PDFErrorOr<Vector<Operator>> Parser::parse_operators(Document* document, ReadonlyBytes bytes)
{
    Parser parser(document, bytes);
    parser.m_enable_encryption = false;
    return parser.parse_operators();
}

Parser::Parser(Document* document, ReadonlyBytes bytes)
    : m_reader(bytes)
    , m_document(document)
{
}

Parser::Parser(ReadonlyBytes bytes)
    : m_reader(bytes)
{
}

void Parser::set_document(WeakPtr<Document> const& document)
{
    m_document = document;
}

DeprecatedString Parser::parse_comment()
{
    StringBuilder comment;
    while (true) {
        if (!m_reader.matches('%'))
            break;

        m_reader.consume();
        auto comment_start_offset = m_reader.offset();
        m_reader.move_until([&](auto) {
            return m_reader.matches_eol();
        });
        comment.append(m_reader.bytes().slice(comment_start_offset, m_reader.offset() - comment_start_offset));
        m_reader.consume_eol();
        m_reader.consume_whitespace();
    }
    return comment.to_deprecated_string();
}

PDFErrorOr<Value> Parser::parse_value(CanBeIndirectValue can_be_indirect_value)
{
    parse_comment();

    if (m_reader.matches("null")) {
        m_reader.move_by(4);
        m_reader.consume_whitespace();
        return Value(nullptr);
    }

    if (m_reader.matches("true")) {
        m_reader.move_by(4);
        m_reader.consume_whitespace();
        return Value(true);
    }

    if (m_reader.matches("false")) {
        m_reader.move_by(5);
        m_reader.consume_whitespace();
        return Value(false);
    }

    if (m_reader.matches_number()) {
        if (can_be_indirect_value == CanBeIndirectValue::Yes)
            return parse_possible_indirect_value_or_ref();
        else
            return parse_number();
    }

    if (m_reader.matches('/'))
        return MUST(parse_name());

    if (m_reader.matches("<<")) {
        auto dict = TRY(parse_dict());
        if (m_reader.matches("stream"))
            return TRY(parse_stream(dict));
        return dict;
    }

    if (m_reader.matches_any('(', '<'))
        return parse_string();

    if (m_reader.matches('['))
        return TRY(parse_array());

    return error(DeprecatedString::formatted("Unexpected char \"{}\"", m_reader.peek()));
}

PDFErrorOr<Value> Parser::parse_possible_indirect_value_or_ref()
{
    auto first_number = TRY(parse_number());
    if (!m_reader.matches_number())
        return first_number;

    m_reader.save();
    auto second_number = parse_number();
    if (second_number.is_error()) {
        m_reader.load();
        return first_number;
    }

    if (m_reader.matches('R')) {
        m_reader.discard();
        m_reader.consume();
        m_reader.consume_whitespace();
        return Value(Reference(first_number.get<int>(), second_number.value().get<int>()));
    }

    if (m_reader.matches("obj")) {
        m_reader.discard();
        auto index = first_number.get<int>();
        auto generation = second_number.value().get<int>();
        VERIFY(index >= 0);
        VERIFY(generation >= 0);
        return TRY(parse_indirect_value(index, generation));
    }

    m_reader.load();
    return first_number;
}

PDFErrorOr<NonnullRefPtr<IndirectValue>> Parser::parse_indirect_value(u32 index, u32 generation)
{
    if (!m_reader.matches("obj"))
        return error("Expected \"obj\" at beginning of indirect value");
    m_reader.move_by(3);
    m_reader.consume_whitespace();

    push_reference({ index, generation });
    auto value = TRY(parse_value());
    if (!m_reader.matches("endobj"))
        return error("Expected \"endobj\" at end of indirect value");

    m_reader.consume(6);
    m_reader.consume_whitespace();

    pop_reference();

    return make_object<IndirectValue>(index, generation, value);
}

PDFErrorOr<NonnullRefPtr<IndirectValue>> Parser::parse_indirect_value()
{
    auto first_number = TRY(parse_number());
    auto second_number = TRY(parse_number());
    auto index = first_number.get<int>();
    auto generation = second_number.get<int>();
    VERIFY(index >= 0);
    VERIFY(generation >= 0);
    return parse_indirect_value(index, generation);
}

PDFErrorOr<Value> Parser::parse_number()
{
    m_reader.consume_whitespace();

    size_t start_offset = m_reader.offset();
    bool is_float = false;
    bool consumed_digit = false;

    if (m_reader.matches('+') || m_reader.matches('-'))
        m_reader.consume();

    while (!m_reader.done()) {
        if (m_reader.matches('.')) {
            if (is_float)
                break;
            is_float = true;
            m_reader.consume();
        } else if (isdigit(m_reader.peek())) {
            m_reader.consume();
            consumed_digit = true;
        } else {
            break;
        }
    }

    if (!consumed_digit)
        return error("Invalid number");

    m_reader.consume_whitespace();

    auto string = DeprecatedString(m_reader.bytes().slice(start_offset, m_reader.offset() - start_offset));
    if (is_float)
        return Value(strtof(string.characters(), nullptr));

    return Value(atoi(string.characters()));
}

PDFErrorOr<NonnullRefPtr<NameObject>> Parser::parse_name()
{
    if (!m_reader.consume('/'))
        return error("Expected Name object to start with \"/\"");

    StringBuilder builder;

    while (true) {
        if (!m_reader.matches_regular_character())
            break;

        if (m_reader.matches('#')) {
            m_reader.consume();
            int hex_value = 0;
            for (int i = 0; i < 2; i++) {
                auto ch = m_reader.consume();
                VERIFY(isxdigit(ch));
                hex_value *= 16;
                if (ch <= '9') {
                    hex_value += ch - '0';
                } else {
                    hex_value += ch - 'A' + 10;
                }
            }
            builder.append(static_cast<char>(hex_value));
            continue;
        }

        builder.append(m_reader.consume());
    }

    m_reader.consume_whitespace();

    return make_object<NameObject>(builder.to_deprecated_string());
}

NonnullRefPtr<StringObject> Parser::parse_string()
{
    ScopeGuard guard([&] { m_reader.consume_whitespace(); });

    DeprecatedString string;
    bool is_binary_string;

    if (m_reader.matches('(')) {
        string = parse_literal_string();
        is_binary_string = false;
    } else {
        string = parse_hex_string();
        is_binary_string = true;
    }

    VERIFY(!string.is_null());

    auto string_object = make_object<StringObject>(string, is_binary_string);

    if (m_document->security_handler() && m_enable_encryption)
        m_document->security_handler()->decrypt(string_object, m_current_reference_stack.last());

    auto unencrypted_string = string_object->string();

    if (unencrypted_string.bytes().starts_with(Array<u8, 2> { 0xfe, 0xff })) {
        // The string is encoded in UTF16-BE
        string_object->set_string(TextCodec::decoder_for("utf-16be"sv)->to_utf8(unencrypted_string).release_value_but_fixme_should_propagate_errors().to_deprecated_string());
    } else if (unencrypted_string.bytes().starts_with(Array<u8, 3> { 239, 187, 191 })) {
        // The string is encoded in UTF-8. This is the default anyways, but if these bytes
        // are explicitly included, we have to trim them
        string_object->set_string(unencrypted_string.substring(3));
    }

    return string_object;
}

DeprecatedString Parser::parse_literal_string()
{
    VERIFY(m_reader.consume('('));
    StringBuilder builder;
    auto opened_parens = 0;

    while (true) {
        if (m_reader.matches('(')) {
            opened_parens++;
            builder.append(m_reader.consume());
        } else if (m_reader.matches(')')) {
            m_reader.consume();
            if (opened_parens == 0)
                break;
            opened_parens--;
            builder.append(')');
        } else if (m_reader.matches('\\')) {
            m_reader.consume();
            if (m_reader.matches_eol()) {
                m_reader.consume_eol();
                continue;
            }

            if (m_reader.done())
                return {};

            auto ch = m_reader.consume();
            switch (ch) {
            case 'n':
                builder.append('\n');
                break;
            case 'r':
                builder.append('\r');
                break;
            case 't':
                builder.append('\t');
                break;
            case 'b':
                builder.append('\b');
                break;
            case 'f':
                builder.append('\f');
                break;
            case '(':
                builder.append('(');
                break;
            case ')':
                builder.append(')');
                break;
            case '\\':
                builder.append('\\');
                break;
            default: {
                if (ch >= '0' && ch <= '7') {
                    int octal_value = ch - '0';
                    for (int i = 0; i < 2; i++) {
                        auto octal_ch = m_reader.consume();
                        if (octal_ch < '0' || octal_ch > '7')
                            break;
                        octal_value = octal_value * 8 + (octal_ch - '0');
                    }
                    builder.append(static_cast<char>(octal_value));
                } else {
                    builder.append(ch);
                }
            }
            }
        } else if (m_reader.matches_eol()) {
            m_reader.consume_eol();
            builder.append('\n');
        } else {
            builder.append(m_reader.consume());
        }
    }

    return builder.to_deprecated_string();
}

DeprecatedString Parser::parse_hex_string()
{
    VERIFY(m_reader.consume('<'));

    StringBuilder builder;

    while (true) {
        if (m_reader.matches('>')) {
            m_reader.consume();
            return builder.to_deprecated_string();
        } else {
            int hex_value = 0;

            for (int i = 0; i < 2; i++) {
                m_reader.consume_whitespace();
                auto ch = m_reader.consume();
                if (ch == '>') {
                    // The hex string contains an odd number of characters, and the last character
                    // is assumed to be '0'
                    m_reader.consume();
                    hex_value *= 16;
                    builder.append(static_cast<char>(hex_value));
                    return builder.to_deprecated_string();
                }
                VERIFY(isxdigit(ch));

                hex_value *= 16;
                if (ch <= '9') {
                    hex_value += ch - '0';
                } else if (ch >= 'A' && ch <= 'F') {
                    hex_value += ch - 'A' + 10;
                } else {
                    hex_value += ch - 'a' + 10;
                }
            }

            builder.append(static_cast<char>(hex_value));
        }
    }
}

PDFErrorOr<NonnullRefPtr<ArrayObject>> Parser::parse_array()
{
    if (!m_reader.consume('['))
        return error("Expected array to start with \"[\"");
    m_reader.consume_whitespace();
    Vector<Value> values;

    while (!m_reader.matches(']'))
        values.append(TRY(parse_value()));

    VERIFY(m_reader.consume(']'));
    m_reader.consume_whitespace();

    return make_object<ArrayObject>(values);
}

PDFErrorOr<NonnullRefPtr<DictObject>> Parser::parse_dict()
{
    if (!m_reader.consume('<') || !m_reader.consume('<'))
        return error("Expected dict to start with \"<<\"");

    m_reader.consume_whitespace();
    HashMap<DeprecatedFlyString, Value> map;

    while (!m_reader.done()) {
        if (m_reader.matches(">>"))
            break;
        auto name = TRY(parse_name())->name();
        auto value = TRY(parse_value());
        map.set(name, value);
    }

    if (!m_reader.consume('>') || !m_reader.consume('>'))
        return error("Expected dict to end with \">>\"");
    m_reader.consume_whitespace();

    return make_object<DictObject>(move(map));
}

PDFErrorOr<NonnullRefPtr<StreamObject>> Parser::parse_stream(NonnullRefPtr<DictObject> dict)
{
    if (!m_reader.matches("stream"))
        return error("Expected stream to start with \"stream\"");
    m_reader.move_by(6);
    if (!m_reader.consume_eol())
        return error("Expected \"stream\" to be followed by a newline");

    ReadonlyBytes bytes;

    auto maybe_length = dict->get(CommonNames::Length);
    if (maybe_length.has_value() && m_document->can_resolve_references()) {
        // The PDF writer has kindly provided us with the direct length of the stream
        m_reader.save();
        auto length = TRY(m_document->resolve_to<int>(maybe_length.value()));
        m_reader.load();
        bytes = m_reader.bytes().slice(m_reader.offset(), length);
        m_reader.move_by(length);
        m_reader.consume_whitespace();
    } else {
        // We have to look for the endstream keyword
        auto stream_start = m_reader.offset();
        while (!m_reader.matches("endstream")) {
            m_reader.consume();
            m_reader.move_until('e');
        }
        auto stream_end = m_reader.offset();
        m_reader.consume_eol();
        bytes = m_reader.bytes().slice(stream_start, stream_end - stream_start);
    }

    m_reader.move_by(9);
    m_reader.consume_whitespace();

    auto stream_object = make_object<StreamObject>(dict, MUST(ByteBuffer::copy(bytes)));

    if (m_document->security_handler() && m_enable_encryption)
        m_document->security_handler()->decrypt(stream_object, m_current_reference_stack.last());

    if (dict->contains(CommonNames::Filter) && m_enable_filters) {
        Vector<DeprecatedFlyString> filters = TRY(m_document->read_filters(dict));

        // Every filter may get its own parameter dictionary
        Vector<RefPtr<DictObject>> decode_parms_vector;
        RefPtr<Object> decode_parms_object;
        if (dict->contains(CommonNames::DecodeParms)) {
            decode_parms_object = TRY(dict->get_object(m_document, CommonNames::DecodeParms));
            if (decode_parms_object->is<ArrayObject>()) {
                auto decode_parms_array = decode_parms_object->cast<ArrayObject>();
                for (size_t i = 0; i < decode_parms_array->size(); ++i) {
                    RefPtr<DictObject> decode_parms;
                    auto entry = decode_parms_array->at(i);
                    if (entry.has<NonnullRefPtr<Object>>())
                        decode_parms = entry.get<NonnullRefPtr<Object>>()->cast<DictObject>();
                    decode_parms_vector.append(decode_parms);
                }
            } else {
                decode_parms_vector.append(decode_parms_object->cast<DictObject>());
            }
        }

        VERIFY(decode_parms_vector.is_empty() || decode_parms_vector.size() == filters.size());

        for (size_t i = 0; i < filters.size(); ++i) {
            RefPtr<DictObject> decode_parms;
            if (!decode_parms_vector.is_empty())
                decode_parms = decode_parms_vector.at(i);

            stream_object->buffer() = TRY(Filter::decode(stream_object->bytes(), filters.at(i), decode_parms));
        }
    }

    return stream_object;
}

PDFErrorOr<Vector<Operator>> Parser::parse_operators()
{
    Vector<Operator> operators;
    Vector<Value> operator_args;

    constexpr static auto is_operator_char = [](char ch) {
        return isalpha(ch) || ch == '*' || ch == '\'' || ch == '"';
    };

    m_reader.consume_whitespace();

    while (!m_reader.done()) {
        auto ch = m_reader.peek();
        if (is_operator_char(ch)) {
            auto operator_start = m_reader.offset();
            while (is_operator_char(ch)) {
                m_reader.consume();
                if (m_reader.done())
                    break;
                ch = m_reader.peek();
            }

            auto operator_string = StringView(m_reader.bytes().slice(operator_start, m_reader.offset() - operator_start));
            auto operator_type = Operator::operator_type_from_symbol(operator_string);
            operators.append(Operator(operator_type, move(operator_args)));
            operator_args = Vector<Value>();
            m_reader.consume_whitespace();

            continue;
        }

        // Note: We disallow parsing indirect values here, since
        //       operations like 0 0 0 RG would confuse the parser
        auto v = TRY(parse_value(CanBeIndirectValue::No));
        operator_args.append(v);
    }

    return operators;
}

Error Parser::error(
    DeprecatedString const& message
#ifdef PDF_DEBUG
    ,
    SourceLocation loc
#endif
) const
{
#ifdef PDF_DEBUG
    dbgln("\033[31m{} Parser error at offset {}: {}\033[0m", loc, m_reader.offset(), message);
#endif

    return Error { Error::Type::Parse, message };
}

}
