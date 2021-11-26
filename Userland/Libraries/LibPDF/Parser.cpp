/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BitStream.h>
#include <AK/MemoryStream.h>
#include <AK/ScopeGuard.h>
#include <AK/TypeCasts.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Document.h>
#include <LibPDF/Filter.h>
#include <LibPDF/Parser.h>
#include <LibTextCodec/Decoder.h>
#include <ctype.h>

namespace PDF {

template<typename T, typename... Args>
static NonnullRefPtr<T> make_object(Args... args) requires(IsBaseOf<Object, T>)
{
    return adopt_ref(*new T(forward<Args>(args)...));
}

Vector<Command> Parser::parse_graphics_commands(ReadonlyBytes bytes)
{
    auto parser = adopt_ref(*new Parser(bytes));
    return parser->parse_graphics_commands();
}

Parser::Parser(Badge<Document>, ReadonlyBytes bytes)
    : m_reader(bytes)
{
}

Parser::Parser(ReadonlyBytes bytes)
    : m_reader(bytes)
{
}

void Parser::set_document(RefPtr<Document> const& document)
{
    m_document = document;
}

bool Parser::initialize()
{
    if (!parse_header())
        return {};

    const auto result = initialize_linearization_dict();
    if (result == LinearizationResult::Error)
        return {};

    if (result == LinearizationResult::NotLinearized)
        return initialize_non_linearized_xref_table();

    bool is_linearized = m_linearization_dictionary.has_value();
    if (is_linearized) {
        // The file may have been linearized at one point, but could have been updated afterwards,
        // which means it is no longer a linearized PDF file.
        is_linearized = m_linearization_dictionary.value().length_of_file == m_reader.bytes().size();

        if (!is_linearized) {
            // FIXME: The file shouldn't be treated as linearized, yet the xref tables are still
            // split. This might take some tweaking to ensure correct behavior, which can be
            // implemented later.
            TODO();
        }
    }

    if (is_linearized)
        return initialize_linearized_xref_table();

    return initialize_non_linearized_xref_table();
}

Value Parser::parse_object_with_index(u32 index)
{
    VERIFY(m_xref_table->has_object(index));
    auto byte_offset = m_xref_table->byte_offset_for_object(index);
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
    if (m_reader.remaining() == 0)
        return false;
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
    if (minor_ver < '0' || minor_ver > '7')
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

Parser::LinearizationResult Parser::initialize_linearization_dict()
{
    // parse_header() is called immediately before this, so we are at the right location
    auto dict_value = m_document->resolve(parse_indirect_value());
    if (!dict_value.has<NonnullRefPtr<Object>>())
        return LinearizationResult::Error;

    auto dict_object = dict_value.get<NonnullRefPtr<Object>>();
    if (!dict_object->is_dict())
        return LinearizationResult::NotLinearized;

    auto dict = object_cast<DictObject>(dict_object);

    if (!dict->contains(CommonNames::Linearized))
        return LinearizationResult::NotLinearized;

    if (!dict->contains(CommonNames::L, CommonNames::H, CommonNames::O, CommonNames::E, CommonNames::N, CommonNames::T))
        return LinearizationResult::Error;

    auto length_of_file = dict->get_value(CommonNames::L);
    auto hint_table = dict->get_value(CommonNames::H);
    auto first_page_object_number = dict->get_value(CommonNames::O);
    auto offset_of_first_page_end = dict->get_value(CommonNames::E);
    auto number_of_pages = dict->get_value(CommonNames::N);
    auto offset_of_main_xref_table = dict->get_value(CommonNames::T);
    auto first_page = dict->get(CommonNames::P).value_or({});

    // Validation
    if (!length_of_file.has_u32()
        || !hint_table.has<NonnullRefPtr<Object>>()
        || !first_page_object_number.has_u32()
        || !number_of_pages.has_u16()
        || !offset_of_main_xref_table.has_u32()
        || (!first_page.has<Empty>() && !first_page.has_u32())) {
        return LinearizationResult::Error;
    }

    auto hint_table_object = hint_table.get<NonnullRefPtr<Object>>();
    if (!hint_table_object->is_array())
        return LinearizationResult::Error;

    auto hint_table_array = object_cast<ArrayObject>(hint_table_object);
    auto hint_table_size = hint_table_array->size();
    if (hint_table_size != 2 && hint_table_size != 4)
        return LinearizationResult::Error;

    auto primary_hint_stream_offset = hint_table_array->at(0);
    auto primary_hint_stream_length = hint_table_array->at(1);
    Value overflow_hint_stream_offset;
    Value overflow_hint_stream_length;

    if (hint_table_size == 4) {
        overflow_hint_stream_offset = hint_table_array->at(2);
        overflow_hint_stream_length = hint_table_array->at(3);
    }

    if (!primary_hint_stream_offset.has_u32()
        || !primary_hint_stream_length.has_u32()
        || (!overflow_hint_stream_offset.has<Empty>() && !overflow_hint_stream_offset.has_u32())
        || (!overflow_hint_stream_length.has<Empty>() && !overflow_hint_stream_length.has_u32())) {
        return LinearizationResult::Error;
    }

    m_linearization_dictionary = LinearizationDictionary {
        length_of_file.get_u32(),
        primary_hint_stream_offset.get_u32(),
        primary_hint_stream_length.get_u32(),
        overflow_hint_stream_offset.has<Empty>() ? NumericLimits<u32>::max() : overflow_hint_stream_offset.get_u32(),
        overflow_hint_stream_length.has<Empty>() ? NumericLimits<u32>::max() : overflow_hint_stream_length.get_u32(),
        first_page_object_number.get_u32(),
        offset_of_first_page_end.get_u32(),
        number_of_pages.get_u16(),
        offset_of_main_xref_table.get_u32(),
        first_page.has<Empty>() ? NumericLimits<u32>::max() : first_page.get_u32(),
    };

    return LinearizationResult::Linearized;
}

bool Parser::initialize_linearized_xref_table()
{
    // The linearization parameter dictionary has just been parsed, and the xref table
    // comes immediately after it. We are in the correct spot.
    if (!m_reader.matches("xref"))
        return false;

    m_xref_table = parse_xref_table();
    if (!m_xref_table)
        return false;

    m_trailer = parse_file_trailer();
    if (!m_trailer)
        return false;

    // Also parse the main xref table and merge into the first-page xref table. Note
    // that we don't use the main xref table offset from the linearization dict because
    // for some reason, it specified the offset of the whitespace after the object
    // index start and length? So it's much easier to do it this way.
    auto main_xref_table_offset = m_trailer->get_value(CommonNames::Prev).to_int();
    m_reader.move_to(main_xref_table_offset);
    auto main_xref_table = parse_xref_table();
    if (!main_xref_table)
        return false;

    return m_xref_table->merge(move(*main_xref_table));
}

bool Parser::initialize_hint_tables()
{
    auto linearization_dict = m_linearization_dictionary.value();
    auto primary_offset = linearization_dict.primary_hint_stream_offset;
    auto overflow_offset = linearization_dict.overflow_hint_stream_offset;

    auto parse_hint_table = [&](size_t offset) -> RefPtr<StreamObject> {
        m_reader.move_to(offset);
        auto stream_indirect_value = parse_indirect_value();
        if (!stream_indirect_value)
            return {};

        auto stream_value = stream_indirect_value->value();
        if (!stream_value.has<NonnullRefPtr<Object>>())
            return {};

        auto stream_object = stream_value.get<NonnullRefPtr<Object>>();
        if (!stream_object->is_stream())
            return {};

        return object_cast<StreamObject>(stream_object);
    };

    auto primary_hint_stream = parse_hint_table(primary_offset);
    if (!primary_hint_stream)
        return false;

    RefPtr<StreamObject> overflow_hint_stream;
    if (overflow_offset != NumericLimits<u32>::max())
        overflow_hint_stream = parse_hint_table(overflow_offset);

    ByteBuffer possible_merged_stream_buffer;
    ReadonlyBytes hint_stream_bytes;

    if (overflow_hint_stream) {
        auto primary_size = primary_hint_stream->bytes().size();
        auto overflow_size = overflow_hint_stream->bytes().size();
        auto total_size = primary_size + overflow_size;

        auto buffer_result = ByteBuffer::create_uninitialized(total_size);
        if (!buffer_result.has_value())
            return false;
        possible_merged_stream_buffer = buffer_result.release_value();
        auto ok = !possible_merged_stream_buffer.try_append(primary_hint_stream->bytes()).is_error();
        ok = ok && !possible_merged_stream_buffer.try_append(overflow_hint_stream->bytes()).is_error();
        if (!ok)
            return false;
        hint_stream_bytes = possible_merged_stream_buffer.bytes();
    } else {
        hint_stream_bytes = primary_hint_stream->bytes();
    }

    auto hint_table = parse_page_offset_hint_table(hint_stream_bytes);
    if (!hint_table.has_value())
        return false;

    dbgln("hint table: {}", hint_table.value());

    auto hint_table_entries = parse_all_page_offset_hint_table_entries(hint_table.value(), hint_stream_bytes);
    if (!hint_table_entries.has_value())
        return false;

    auto entries = hint_table_entries.value();
    dbgln("hint table entries size: {}", entries.size());
    for (auto& entry : entries)
        dbgln("{}", entry);

    return true;
}

bool Parser::initialize_non_linearized_xref_table()
{
    m_reader.move_to(m_reader.bytes().size() - 1);
    if (!navigate_to_before_eof_marker())
        return false;
    if (!navigate_to_after_startxref())
        return false;
    if (m_reader.done())
        return false;

    m_reader.set_reading_forwards();
    auto xref_offset_value = parse_number();
    if (!xref_offset_value.has<int>())
        return false;
    auto xref_offset = xref_offset_value.get<int>();

    m_reader.move_to(xref_offset);
    m_xref_table = parse_xref_table();
    if (!m_xref_table)
        return false;
    m_trailer = parse_file_trailer();
    return m_trailer;
}

RefPtr<XRefTable> Parser::parse_xref_table()
{
    if (!m_reader.matches("xref"))
        return {};
    m_reader.move_by(4);
    if (!consume_eol())
        return {};

    auto table = adopt_ref(*new XRefTable());

    while (true) {
        if (m_reader.matches("trailer"))
            return table;

        Vector<XRefEntry> entries;

        auto starting_index_value = parse_number();
        auto starting_index = starting_index_value.get<int>();
        auto object_count_value = parse_number();
        auto object_count = object_count_value.get<int>();

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

        table->add_section({ starting_index, object_count, entries });
    }
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

Optional<Parser::PageOffsetHintTable> Parser::parse_page_offset_hint_table(ReadonlyBytes hint_stream_bytes)
{
    if (hint_stream_bytes.size() < sizeof(PageOffsetHintTable))
        return {};

    size_t offset = 0;

    auto read_u32 = [&] {
        u32 data = reinterpret_cast<const u32*>(hint_stream_bytes.data() + offset)[0];
        offset += 4;
        return AK::convert_between_host_and_big_endian(data);
    };

    auto read_u16 = [&] {
        u16 data = reinterpret_cast<const u16*>(hint_stream_bytes.data() + offset)[0];
        offset += 2;
        return AK::convert_between_host_and_big_endian(data);
    };

    PageOffsetHintTable hint_table {
        read_u32(),
        read_u32(),
        read_u16(),
        read_u32(),
        read_u16(),
        read_u32(),
        read_u16(),
        read_u32(),
        read_u16(),
        read_u16(),
        read_u16(),
        read_u16(),
        read_u16(),
    };

    // Verify that all of the bits_required_for_xyz fields are <= 32, since all of the numeric
    // fields in PageOffsetHintTableEntry are u32
    VERIFY(hint_table.bits_required_for_object_number <= 32);
    VERIFY(hint_table.bits_required_for_page_length <= 32);
    VERIFY(hint_table.bits_required_for_content_stream_offsets <= 32);
    VERIFY(hint_table.bits_required_for_content_stream_length <= 32);
    VERIFY(hint_table.bits_required_for_number_of_shared_obj_refs <= 32);
    VERIFY(hint_table.bits_required_for_greatest_shared_obj_identifier <= 32);
    VERIFY(hint_table.bits_required_for_fraction_numerator <= 32);

    return hint_table;
}

Optional<Vector<Parser::PageOffsetHintTableEntry>> Parser::parse_all_page_offset_hint_table_entries(PageOffsetHintTable const& hint_table, ReadonlyBytes hint_stream_bytes)
{
    InputMemoryStream input_stream(hint_stream_bytes);
    input_stream.seek(sizeof(PageOffsetHintTable));
    if (input_stream.has_any_error())
        return {};

    InputBitStream bit_stream(input_stream);

    auto number_of_pages = m_linearization_dictionary.value().number_of_pages;
    Vector<PageOffsetHintTableEntry> entries;
    for (size_t i = 0; i < number_of_pages; i++)
        entries.append(PageOffsetHintTableEntry {});

    auto bits_required_for_object_number = hint_table.bits_required_for_object_number;
    auto bits_required_for_page_length = hint_table.bits_required_for_page_length;
    auto bits_required_for_content_stream_offsets = hint_table.bits_required_for_content_stream_offsets;
    auto bits_required_for_content_stream_length = hint_table.bits_required_for_content_stream_length;
    auto bits_required_for_number_of_shared_obj_refs = hint_table.bits_required_for_number_of_shared_obj_refs;
    auto bits_required_for_greatest_shared_obj_identifier = hint_table.bits_required_for_greatest_shared_obj_identifier;
    auto bits_required_for_fraction_numerator = hint_table.bits_required_for_fraction_numerator;

    auto parse_int_entry = [&](u32 PageOffsetHintTableEntry::*field, u32 bit_size) {
        if (bit_size <= 0)
            return;

        for (int i = 0; i < number_of_pages; i++) {
            auto& entry = entries[i];
            entry.*field = bit_stream.read_bits(bit_size);
        }
    };

    auto parse_vector_entry = [&](Vector<u32> PageOffsetHintTableEntry::*field, u32 bit_size) {
        if (bit_size <= 0)
            return;

        for (int page = 1; page < number_of_pages; page++) {
            auto number_of_shared_objects = entries[page].number_of_shared_objects;
            Vector<u32> items;
            items.ensure_capacity(number_of_shared_objects);

            for (size_t i = 0; i < number_of_shared_objects; i++)
                items.unchecked_append(bit_stream.read_bits(bit_size));

            entries[page].*field = move(items);
        }
    };

    parse_int_entry(&PageOffsetHintTableEntry::objects_in_page_number, bits_required_for_object_number);
    parse_int_entry(&PageOffsetHintTableEntry::page_length_number, bits_required_for_page_length);
    parse_int_entry(&PageOffsetHintTableEntry::number_of_shared_objects, bits_required_for_number_of_shared_obj_refs);
    parse_vector_entry(&PageOffsetHintTableEntry::shared_object_identifiers, bits_required_for_greatest_shared_obj_identifier);
    parse_vector_entry(&PageOffsetHintTableEntry::shared_object_location_numerators, bits_required_for_fraction_numerator);
    parse_int_entry(&PageOffsetHintTableEntry::page_content_stream_offset_number, bits_required_for_content_stream_offsets);
    parse_int_entry(&PageOffsetHintTableEntry::page_content_stream_length_number, bits_required_for_content_stream_length);

    return entries;
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
        return Value(nullptr);
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
        if (m_reader.matches("stream"))
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
    if (!first_number.has<int>() || !matches_number())
        return first_number;

    m_reader.save();
    auto second_number = parse_number();
    if (!second_number.has<int>()) {
        m_reader.load();
        return first_number;
    }

    if (m_reader.matches('R')) {
        m_reader.discard();
        consume();
        consume_whitespace();
        return Value(Reference(first_number.get<int>(), second_number.get<int>()));
    }

    if (m_reader.matches("obj")) {
        m_reader.discard();
        return parse_indirect_value(first_number.get<int>(), second_number.get<int>());
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

    consume(6);
    consume_whitespace();

    return make_object<IndirectValue>(index, generation, value);
}

RefPtr<IndirectValue> Parser::parse_indirect_value()
{
    auto first_number = parse_number();
    if (!first_number.has<int>())
        return {};
    auto second_number = parse_number();
    if (!second_number.has<int>())
        return {};
    return parse_indirect_value(first_number.get<int>(), second_number.get<int>());
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
        if (value.has<Empty>())
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
        if (value.has<Empty>())
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

    VERIFY(m_xref_table->has_object(object_index));
    auto byte_offset = m_xref_table->byte_offset_for_object(object_index);

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
        if (value.has<Empty>()) {
            ok = false;
            return {};
        }
        if (name_string == CommonNames::Type) {
            if (!value.has<NonnullRefPtr<Object>>())
                return {};
            auto type_object = value.get<NonnullRefPtr<Object>>();
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
    if (maybe_length.has_value() && (!maybe_length->has<Reference>() || m_xref_table)) {
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
    if (m_reader.done()) {
        return false;
    }
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

namespace AK {

template<>
struct Formatter<PDF::Parser::LinearizationDictionary> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& format_builder, PDF::Parser::LinearizationDictionary const& dict)
    {
        StringBuilder builder;
        builder.append("{\n");
        builder.appendff("  length_of_file={}\n", dict.length_of_file);
        builder.appendff("  primary_hint_stream_offset={}\n", dict.primary_hint_stream_offset);
        builder.appendff("  primary_hint_stream_length={}\n", dict.primary_hint_stream_length);
        builder.appendff("  overflow_hint_stream_offset={}\n", dict.overflow_hint_stream_offset);
        builder.appendff("  overflow_hint_stream_length={}\n", dict.overflow_hint_stream_length);
        builder.appendff("  first_page_object_number={}\n", dict.first_page_object_number);
        builder.appendff("  offset_of_first_page_end={}\n", dict.offset_of_first_page_end);
        builder.appendff("  number_of_pages={}\n", dict.number_of_pages);
        builder.appendff("  offset_of_main_xref_table={}\n", dict.offset_of_main_xref_table);
        builder.appendff("  first_page={}\n", dict.first_page);
        builder.append('}');
        return Formatter<StringView>::format(format_builder, builder.to_string());
    }
};

template<>
struct Formatter<PDF::Parser::PageOffsetHintTable> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& format_builder, PDF::Parser::PageOffsetHintTable const& table)
    {
        StringBuilder builder;
        builder.append("{\n");
        builder.appendff("  least_number_of_objects_in_a_page={}\n", table.least_number_of_objects_in_a_page);
        builder.appendff("  location_of_first_page_object={}\n", table.location_of_first_page_object);
        builder.appendff("  bits_required_for_object_number={}\n", table.bits_required_for_object_number);
        builder.appendff("  least_length_of_a_page={}\n", table.least_length_of_a_page);
        builder.appendff("  bits_required_for_page_length={}\n", table.bits_required_for_page_length);
        builder.appendff("  least_offset_of_any_content_stream={}\n", table.least_offset_of_any_content_stream);
        builder.appendff("  bits_required_for_content_stream_offsets={}\n", table.bits_required_for_content_stream_offsets);
        builder.appendff("  least_content_stream_length={}\n", table.least_content_stream_length);
        builder.appendff("  bits_required_for_content_stream_length={}\n", table.bits_required_for_content_stream_length);
        builder.appendff("  bits_required_for_number_of_shared_obj_refs={}\n", table.bits_required_for_number_of_shared_obj_refs);
        builder.appendff("  bits_required_for_greatest_shared_obj_identifier={}\n", table.bits_required_for_greatest_shared_obj_identifier);
        builder.appendff("  bits_required_for_fraction_numerator={}\n", table.bits_required_for_fraction_numerator);
        builder.appendff("  shared_object_reference_fraction_denominator={}\n", table.shared_object_reference_fraction_denominator);
        builder.append('}');
        return Formatter<StringView>::format(format_builder, builder.to_string());
    }
};

template<>
struct Formatter<PDF::Parser::PageOffsetHintTableEntry> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& format_builder, PDF::Parser::PageOffsetHintTableEntry const& entry)
    {
        StringBuilder builder;
        builder.append("{\n");
        builder.appendff("  objects_in_page_number={}\n", entry.objects_in_page_number);
        builder.appendff("  page_length_number={}\n", entry.page_length_number);
        builder.appendff("  number_of_shared_objects={}\n", entry.number_of_shared_objects);
        builder.append("  shared_object_identifiers=[");
        for (auto& identifier : entry.shared_object_identifiers)
            builder.appendff(" {}", identifier);
        builder.append(" ]\n");
        builder.append("  shared_object_location_numerators=[");
        for (auto& numerator : entry.shared_object_location_numerators)
            builder.appendff(" {}", numerator);
        builder.append(" ]\n");
        builder.appendff("  page_content_stream_offset_number={}\n", entry.page_content_stream_offset_number);
        builder.appendff("  page_content_stream_length_number={}\n", entry.page_content_stream_length_number);
        builder.append('}');
        return Formatter<StringView>::format(format_builder, builder.to_string());
    }
};

}
