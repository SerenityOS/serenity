/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/TypeCasts.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Document.h>
#include <LibPDF/Filter.h>
#include <LibPDF/Parser.h>
#include <LibTextCodec/Decoder.h>
#include <ctype.h>
#include <math.h>

namespace PDF {

template<typename T, typename... Args>
static NonnullRefPtr<T> make_object(Args... args) requires(IsBaseOf<Object, T>)
{
    return adopt_ref(*new T(forward<Args>(args)...));
}

Vector<Command> Parser::parse_graphics_commands(const ReadonlyBytes& bytes)
{
    auto parser = adopt_ref(*new Parser(bytes));
    return parser->parse_graphics_commands();
}

Parser::Parser(Badge<Document>, const ReadonlyBytes& bytes)
    : m_reader(bytes)
{
}

Parser::Parser(const ReadonlyBytes& bytes)
    : m_reader(bytes)
{
}

bool Parser::initialize()
{
    if (!parse_header())
        return {};

    m_reader.move_to(m_reader.bytes().size() - 1);
    if (!navigate_to_before_eof_marker())
        return false;
    if (!navigate_to_after_startxref())
        return false;
    if (m_reader.done())
        return false;

    m_reader.set_reading_forwards();
    auto xref_offset_value = parse_number();
    if (!xref_offset_value.is_int())
        return false;
    auto xref_offset = xref_offset_value.as_int();

    m_reader.move_to(xref_offset);
    auto xref_table = parse_xref_table();
    if (!xref_table.has_value())
        return false;
    auto trailer = parse_file_trailer();
    if (!trailer)
        return false;

    m_xref_table = xref_table.value();
    m_trailer = trailer;
    return true;
}

Value Parser::parse_object_with_index(u32 index)
{
    VERIFY(m_xref_table.has_object(index));
    auto byte_offset = m_xref_table.byte_offset_for_object(index);
    m_reader.move_to(byte_offset);
    auto indirect_value = parse_indirect_value();
    VERIFY(indirect_value);
    VERIFY(indirect_value->index() == index);
    return indirect_value->value();
}

bool Parser::parse_header()
{
    // FIXME: Do something with the version?
    m_reader.set_reading_forwards();
    m_reader.move_to(0);
    if (m_reader.remaining() < 8 || !m_reader.matches("%PDF-"))
        return false;
    m_reader.move_by(5);

    char major_ver = m_reader.read();
    if (major_ver != '1' && major_ver != '2')
        return false;
    if (m_reader.read() != '.')
        return false;

    char minor_ver = m_reader.read();
    if (minor_ver < '0' || major_ver > '7')
        return false;
    consume_eol();

    // Parse optional high-byte comment, which signifies a binary file
    // FIXME: Do something with this?
    auto comment = parse_comment();
    if (!comment.is_empty()) {
        auto binary = comment.length() >= 4;
        if (binary) {
            for (size_t i = 0; i < comment.length() && binary; i++)
                binary = static_cast<u8>(comment[i]) > 128;
        }
    }

    return true;
}

Optional<XRefTable> Parser::parse_xref_table()
{
    if (!m_reader.matches("xref"))
        return {};
    m_reader.move_by(4);
    if (!consume_eol())
        return {};

    XRefTable table;

    while (true) {
        if (m_reader.matches("trailer"))
            break;

        Vector<XRefEntry> entries;

        auto starting_index_value = parse_number();
        auto starting_index = starting_index_value.as_int();
        auto object_count_value = parse_number();
        auto object_count = object_count_value.as_int();

        for (int i = 0; i < object_count; i++) {
            auto offset_string = String(m_reader.bytes().slice(m_reader.offset(), 10));
            m_reader.move_by(10);
            if (!consume(' '))
                return {};

            auto generation_string = String(m_reader.bytes().slice(m_reader.offset(), 5));
            m_reader.move_by(5);
            if (!consume(' '))
                return {};

            auto letter = m_reader.read();
            if (letter != 'n' && letter != 'f')
                return {};

            // The line ending sequence can be one of the following:
            // SP CR, SP LF, or CR LF
            if (m_reader.matches(' ')) {
                consume();
                auto ch = consume();
                if (ch != '\r' && ch != '\n')
                    return {};
            } else {
                if (!m_reader.matches("\r\n"))
                    return {};
                m_reader.move_by(2);
            }

            auto offset = strtol(offset_string.characters(), nullptr, 10);
            auto generation = strtol(generation_string.characters(), nullptr, 10);

            entries.append({ offset, static_cast<u16>(generation), letter == 'n' });
        }

        table.add_section({ starting_index, object_count, entries });
    }

    return table;
}

RefPtr<DictObject> Parser::parse_file_trailer()
{
    if (!m_reader.matches("trailer"))
        return {};
    m_reader.move_by(7);
    consume_whitespace();
    auto dict = parse_dict();
    if (!dict)
        return {};

    if (!m_reader.matches("startxref"))
        return {};
    m_reader.move_by(9);
    consume_whitespace();

    m_reader.move_until([&](auto) { return matches_eol(); });
    VERIFY(consume_eol());
    if (!m_reader.matches("%%EOF"))
        return {};
    m_reader.move_by(5);
    consume_whitespace();

    return dict;
}

bool Parser::navigate_to_before_eof_marker()
{
    m_reader.set_reading_backwards();

    while (!m_reader.done()) {
        m_reader.move_until([&](auto) { return matches_eol(); });
        if (m_reader.done())
            return false;

        consume_eol();
        if (!m_reader.matches("%%EOF"))
            continue;

        m_reader.move_by(5);
        if (!matches_eol())
            continue;
        consume_eol();
        return true;
    }

    return false;
}

bool Parser::navigate_to_after_startxref()
{
    m_reader.set_reading_backwards();

    while (!m_reader.done()) {
        m_reader.move_until([&](auto) { return matches_eol(); });
        auto offset = m_reader.offset() + 1;

        consume_eol();
        if (!m_reader.matches("startxref"))
            continue;

        m_reader.move_by(9);
        if (!matches_eol())
            continue;

        m_reader.move_to(offset);
        return true;
    }

    return false;
}

bool Parser::sloppy_is_linearized()
{
    ScopeGuard guard([&] {
        m_reader.move_to(0);
        m_reader.set_reading_forwards();
    });

    auto limit = min(1024ul, m_reader.bytes().size() - 1);
    m_reader.move_to(limit);
    m_reader.set_reading_backwards();

    while (!m_reader.done()) {
        m_reader.move_until('/');
        if (m_reader.matches("/Linearized"))
            return true;
        m_reader.move_by(1);
    }

    return false;
}

String Parser::parse_comment()
{
    if (!m_reader.matches('%'))
        return {};

    consume();
    auto comment_start_offset = m_reader.offset();
    m_reader.move_until([&](auto) {
        return matches_eol();
    });
    String str = StringView(m_reader.bytes().slice(comment_start_offset, m_reader.offset() - comment_start_offset));
    consume_eol();
    consume_whitespace();
    return str;
}

Value Parser::parse_value()
{
    parse_comment();

    if (m_reader.matches("null")) {
        m_reader.move_by(4);
        consume_whitespace();
        return Value(Value::NullTag {});
    }

    if (m_reader.matches("true")) {
        m_reader.move_by(4);
        consume_whitespace();
        return Value(true);
    }

    if (m_reader.matches("false")) {
        m_reader.move_by(5);
        consume_whitespace();
        return Value(false);
    }

    if (matches_number())
        return parse_possible_indirect_value_or_ref();

    if (m_reader.matches('/'))
        return parse_name();

    if (m_reader.matches("<<")) {
        auto dict = parse_dict();
        if (!dict)
            return {};
        if (m_reader.matches("stream\n"))
            return parse_stream(dict.release_nonnull());
        return dict;
    }

    if (m_reader.matches_any('(', '<'))
        return parse_string();

    if (m_reader.matches('['))
        return parse_array();

    dbgln("tried to parse value, but found char {} ({}) at offset {}", m_reader.peek(), static_cast<u8>(m_reader.peek()), m_reader.offset());
    VERIFY_NOT_REACHED();
}

Value Parser::parse_possible_indirect_value_or_ref()
{
    auto first_number = parse_number();
    if (!first_number.is_int() || !matches_number())
        return first_number;

    m_reader.save();
    auto second_number = parse_number();
    if (!second_number.is_int()) {
        m_reader.load();
        return first_number;
    }

    if (m_reader.matches('R')) {
        m_reader.discard();
        consume();
        consume_whitespace();
        return Value(first_number.as_int(), second_number.as_int());
    }

    if (m_reader.matches("obj")) {
        m_reader.discard();
        return parse_indirect_value(first_number.as_int(), second_number.as_int());
    }

    m_reader.load();
    return first_number;
}

RefPtr<IndirectValue> Parser::parse_indirect_value(int index, int generation)
{
    if (!m_reader.matches("obj"))
        return {};
    m_reader.move_by(3);
    if (matches_eol())
        consume_eol();
    auto value = parse_value();
    if (!m_reader.matches("endobj"))
        return {};

    return make_object<IndirectValue>(index, generation, value);
}

RefPtr<IndirectValue> Parser::parse_indirect_value()
{
    auto first_number = parse_number();
    if (!first_number.is_int())
        return {};
    auto second_number = parse_number();
    if (!second_number.is_int())
        return {};
    return parse_indirect_value(first_number.as_int(), second_number.as_int());
}

Value Parser::parse_number()
{
    size_t start_offset = m_reader.offset();
    bool is_float = false;

    if (m_reader.matches('+') || m_reader.matches('-'))
        consume();

    while (!m_reader.done()) {
        if (m_reader.matches('.')) {
            if (is_float)
                break;
            is_float = true;
            consume();
        } else if (isdigit(m_reader.peek())) {
            consume();
        } else {
            break;
        }
    }

    consume_whitespace();

    auto string = String(m_reader.bytes().slice(start_offset, m_reader.offset() - start_offset));
    float f = strtof(string.characters(), nullptr);
    if (is_float)
        return Value(f);

    VERIFY(floorf(f) == f);
    return Value(static_cast<int>(f));
}

RefPtr<NameObject> Parser::parse_name()
{
    if (!consume('/'))
        return {};
    StringBuilder builder;

    while (true) {
        if (!matches_regular_character())
            break;

        if (m_reader.matches('#')) {
            int hex_value = 0;
            for (int i = 0; i < 2; i++) {
                auto ch = consume();
                if (!isxdigit(ch))
                    return {};
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

        builder.append(consume());
    }

    consume_whitespace();

    return make_object<NameObject>(builder.to_string());
}

RefPtr<StringObject> Parser::parse_string()
{
    ScopeGuard guard([&] { consume_whitespace(); });

    String string;
    bool is_binary_string;

    if (m_reader.matches('(')) {
        string = parse_literal_string();
        is_binary_string = false;
    } else {
        string = parse_hex_string();
        is_binary_string = true;
    }

    if (string.is_null())
        return {};

    if (string.bytes().starts_with(Array<u8, 2> { 0xfe, 0xff })) {
        // The string is encoded in UTF16-BE
        string = TextCodec::decoder_for("utf-16be")->to_utf8(string.substring(2));
    } else if (string.bytes().starts_with(Array<u8, 3> { 239, 187, 191 })) {
        // The string is encoded in UTF-8. This is the default anyways, but if these bytes
        // are explicitly included, we have to trim them
        string = string.substring(3);
    }

    return make_object<StringObject>(string, is_binary_string);
}

String Parser::parse_literal_string()
{
    if (!consume('('))
        return {};
    StringBuilder builder;
    auto opened_parens = 0;

    while (true) {
        if (m_reader.matches('(')) {
            opened_parens++;
            builder.append(consume());
        } else if (m_reader.matches(')')) {
            consume();
            if (opened_parens == 0)
                break;
            opened_parens--;
            builder.append(')');
        } else if (m_reader.matches('\\')) {
            consume();
            if (matches_eol()) {
                consume_eol();
                continue;
            }

            if (m_reader.done())
                return {};

            auto ch = consume();
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
                        auto octal_ch = consume();
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
        } else if (matches_eol()) {
            consume_eol();
            builder.append('\n');
        } else {
            builder.append(consume());
        }
    }

    if (opened_parens != 0)
        return {};

    return builder.to_string();
}

String Parser::parse_hex_string()
{
    if (!consume('<'))
        return {};
    StringBuilder builder;

    while (true) {
        if (m_reader.matches('>')) {
            consume();
            return builder.to_string();
        } else {
            int hex_value = 0;

            for (int i = 0; i < 2; i++) {
                auto ch = consume();
                if (ch == '>') {
                    // The hex string contains an odd number of characters, and the last character
                    // is assumed to be '0'
                    consume();
                    hex_value *= 16;
                    builder.append(static_cast<char>(hex_value));
                    return builder.to_string();
                }

                if (!isxdigit(ch))
                    return {};

                hex_value *= 16;
                if (ch <= '9') {
                    hex_value += ch - '0';
                } else {
                    hex_value += ch - 'A' + 10;
                }
            }

            builder.append(static_cast<char>(hex_value));
        }
    }
}

RefPtr<ArrayObject> Parser::parse_array()
{
    if (!consume('['))
        return {};
    consume_whitespace();
    Vector<Value> values;

    while (!m_reader.matches(']')) {
        auto value = parse_value();
        if (!value)
            return {};
        values.append(value);
    }

    if (!consume(']'))
        return {};
    consume_whitespace();

    return make_object<ArrayObject>(values);
}

RefPtr<DictObject> Parser::parse_dict()
{
    if (!consume('<') || !consume('<'))
        return {};
    consume_whitespace();
    HashMap<FlyString, Value> map;

    while (true) {
        if (m_reader.matches(">>"))
            break;
        auto name = parse_name();
        if (!name)
            return {};
        auto value = parse_value();
        if (!value)
            return {};
        map.set(name->name(), value);
    }

    if (!consume('>') || !consume('>'))
        return {};
    consume_whitespace();

    return make_object<DictObject>(map);
}

RefPtr<DictObject> Parser::conditionally_parse_page_tree_node(u32 object_index, bool& ok)
{
    ok = true;

    VERIFY(m_xref_table.has_object(object_index));
    auto byte_offset = m_xref_table.byte_offset_for_object(object_index);

    m_reader.move_to(byte_offset);
    parse_number();
    parse_number();
    if (!m_reader.matches("obj")) {
        ok = false;
        return {};
    }

    m_reader.move_by(3);
    consume_whitespace();

    if (!consume('<') || !consume('<'))
        return {};
    consume_whitespace();
    HashMap<FlyString, Value> map;

    while (true) {
        if (m_reader.matches(">>"))
            break;
        auto name = parse_name();
        if (!name) {
            ok = false;
            return {};
        }

        auto name_string = name->name();
        if (!name_string.is_one_of(CommonNames::Type, CommonNames::Parent, CommonNames::Kids, CommonNames::Count)) {
            // This is a page, not a page tree node
            return {};
        }
        auto value = parse_value();
        if (!value) {
            ok = false;
            return {};
        }
        if (name_string == CommonNames::Type) {
            if (!value.is_object())
                return {};
            auto type_object = value.as_object();
            if (!type_object->is_name())
                return {};
            auto type_name = object_cast<NameObject>(type_object);
            if (type_name->name() != CommonNames::Pages)
                return {};
        }
        map.set(name->name(), value);
    }

    if (!consume('>') || !consume('>'))
        return {};
    consume_whitespace();

    return make_object<DictObject>(map);
}

RefPtr<StreamObject> Parser::parse_stream(NonnullRefPtr<DictObject> dict)
{
    if (!m_reader.matches("stream"))
        return {};
    m_reader.move_by(6);
    if (!consume_eol())
        return {};

    ReadonlyBytes bytes;

    auto maybe_length = dict->get(CommonNames::Length);
    if (maybe_length.has_value()) {
        // The PDF writer has kindly provided us with the direct length of the stream
        m_reader.save();
        auto length = m_document->resolve_to<int>(maybe_length.value());
        m_reader.load();
        bytes = m_reader.bytes().slice(m_reader.offset(), length);
        m_reader.move_by(length);
        consume_whitespace();
    } else {
        // We have to look for the endstream keyword
        auto stream_start = m_reader.offset();

        while (true) {
            m_reader.move_until([&](auto) { return matches_eol(); });
            auto potential_stream_end = m_reader.offset();
            consume_eol();
            if (!m_reader.matches("endstream"))
                continue;

            bytes = m_reader.bytes().slice(stream_start, potential_stream_end - stream_start);
            break;
        }
    }

    m_reader.move_by(9);
    consume_whitespace();

    if (dict->contains(CommonNames::Filter)) {
        auto filter_type = dict->get_name(m_document, CommonNames::Filter)->name();
        auto maybe_bytes = Filter::decode(bytes, filter_type);
        if (!maybe_bytes.has_value())
            return {};
        return make_object<EncodedStreamObject>(dict, move(maybe_bytes.value()));
    }

    return make_object<PlainTextStreamObject>(dict, bytes);
}

Vector<Command> Parser::parse_graphics_commands()
{
    Vector<Command> commands;
    Vector<Value> command_args;

    constexpr static auto is_command_char = [](char ch) {
        return isalpha(ch) || ch == '*' || ch == '\'';
    };

    while (!m_reader.done()) {
        auto ch = m_reader.peek();
        if (is_command_char(ch)) {
            auto command_start = m_reader.offset();
            while (is_command_char(ch)) {
                consume();
                if (m_reader.done())
                    break;
                ch = m_reader.peek();
            }

            auto command_string = StringView(m_reader.bytes().slice(command_start, m_reader.offset() - command_start));
            auto command_type = Command::command_type_from_symbol(command_string);
            commands.append(Command(command_type, move(command_args)));
            command_args = Vector<Value>();
            consume_whitespace();

            continue;
        }

        command_args.append(parse_value());
    }

    return commands;
}

bool Parser::matches_eol() const
{
    return m_reader.matches_any(0xa, 0xd);
}

bool Parser::matches_whitespace() const
{
    return matches_eol() || m_reader.matches_any(0, 0x9, 0xc, ' ');
}

bool Parser::matches_number() const
{
    if (m_reader.done())
        return false;
    auto ch = m_reader.peek();
    return isdigit(ch) || ch == '-' || ch == '+';
}

bool Parser::matches_delimiter() const
{
    return m_reader.matches_any('(', ')', '<', '>', '[', ']', '{', '}', '/', '%');
}

bool Parser::matches_regular_character() const
{
    return !matches_delimiter() && !matches_whitespace();
}

bool Parser::consume_eol()
{
    if (m_reader.matches("\r\n")) {
        consume(2);
        return true;
    }

    auto consumed = consume();
    return consumed == 0xd || consumed == 0xa;
}

bool Parser::consume_whitespace()
{
    bool consumed = false;
    while (matches_whitespace()) {
        consumed = true;
        consume();
    }
    return consumed;
}

char Parser::consume()
{
    return m_reader.read();
}

void Parser::consume(int amount)
{
    for (size_t i = 0; i < static_cast<size_t>(amount); i++)
        consume();
}

bool Parser::consume(char ch)
{
    return consume() == ch;
}

}
