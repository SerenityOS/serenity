/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BitStream.h>
#include <AK/MemoryStream.h>
#include <AK/ScopeGuard.h>
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

PDFErrorOr<Vector<Operator>> Parser::parse_operators(Document* document, ReadonlyBytes bytes)
{
    auto parser = adopt_ref(*new Parser(document, bytes));
    parser->m_disable_encryption = true;
    return parser->parse_operators();
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

PDFErrorOr<void> Parser::initialize()
{
    TRY(parse_header());

    auto const linearization_result = TRY(initialize_linearization_dict());

    if (linearization_result == LinearizationResult::NotLinearized)
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

PDFErrorOr<Value> Parser::parse_object_with_index(u32 index)
{
    VERIFY(m_xref_table->has_object(index));
    auto byte_offset = m_xref_table->byte_offset_for_object(index);
    m_reader.move_to(byte_offset);
    auto indirect_value = TRY(parse_indirect_value());
    VERIFY(indirect_value->index() == index);
    return indirect_value->value();
}

PDFErrorOr<void> Parser::parse_header()
{
    // FIXME: Do something with the version?
    m_reader.set_reading_forwards();
    if (m_reader.remaining() == 0)
        return error("Empty PDF document");

    m_reader.move_to(0);
    if (m_reader.remaining() < 8 || !m_reader.matches("%PDF-"))
        return error("Not a PDF document");

    m_reader.move_by(5);

    char major_ver = m_reader.read();
    if (major_ver != '1' && major_ver != '2')
        return error(String::formatted("Unknown major version \"{}\"", major_ver));

    if (m_reader.read() != '.')
        return error("Malformed PDF version");

    char minor_ver = m_reader.read();
    if (minor_ver < '0' || minor_ver > '7')
        return error(String::formatted("Unknown minor version \"{}\"", minor_ver));

    m_reader.consume_eol();

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

    return {};
}

PDFErrorOr<Parser::LinearizationResult> Parser::initialize_linearization_dict()
{
    // parse_header() is called immediately before this, so we are at the right location
    auto indirect_value = Value(*TRY(parse_indirect_value()));
    auto dict_value = TRY(m_document->resolve(indirect_value));
    if (!dict_value.has<NonnullRefPtr<Object>>())
        return error("Expected linearization object to be a dictionary");

    auto dict_object = dict_value.get<NonnullRefPtr<Object>>();
    if (!dict_object->is<DictObject>())
        return LinearizationResult::NotLinearized;

    auto dict = dict_object->cast<DictObject>();

    if (!dict->contains(CommonNames::Linearized))
        return LinearizationResult::NotLinearized;

    if (!dict->contains(CommonNames::L, CommonNames::H, CommonNames::O, CommonNames::E, CommonNames::N, CommonNames::T))
        return error("Malformed linearization dictionary");

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
        return error("Malformed linearization dictionary parameters");
    }

    auto hint_table_array = hint_table.get<NonnullRefPtr<Object>>()->cast<ArrayObject>();
    auto hint_table_size = hint_table_array->size();
    if (hint_table_size != 2 && hint_table_size != 4)
        return error("Expected hint table to be of length 2 or 4");

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
        return error("Malformed hint stream");
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

PDFErrorOr<void> Parser::initialize_linearized_xref_table()
{
    // The linearization parameter dictionary has just been parsed, and the xref table
    // comes immediately after it. We are in the correct spot.
    m_xref_table = TRY(parse_xref_table());
    m_trailer = TRY(parse_file_trailer());

    // Also parse the main xref table and merge into the first-page xref table. Note
    // that we don't use the main xref table offset from the linearization dict because
    // for some reason, it specified the offset of the whitespace after the object
    // index start and length? So it's much easier to do it this way.
    auto main_xref_table_offset = m_trailer->get_value(CommonNames::Prev).to_int();
    m_reader.move_to(main_xref_table_offset);
    auto main_xref_table = TRY(parse_xref_table());
    TRY(m_xref_table->merge(move(*main_xref_table)));
    return {};
}

PDFErrorOr<void> Parser::initialize_hint_tables()
{
    auto linearization_dict = m_linearization_dictionary.value();
    auto primary_offset = linearization_dict.primary_hint_stream_offset;
    auto overflow_offset = linearization_dict.overflow_hint_stream_offset;

    auto parse_hint_table = [&](size_t offset) -> RefPtr<StreamObject> {
        m_reader.move_to(offset);
        auto stream_indirect_value = parse_indirect_value();
        if (stream_indirect_value.is_error())
            return {};

        auto stream_value = stream_indirect_value.value()->value();
        if (!stream_value.has<NonnullRefPtr<Object>>())
            return {};

        auto stream_object = stream_value.get<NonnullRefPtr<Object>>();
        if (!stream_object->is<StreamObject>())
            return {};

        return stream_object->cast<StreamObject>();
    };

    auto primary_hint_stream = parse_hint_table(primary_offset);
    if (!primary_hint_stream)
        return error("Invalid primary hint stream");

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
        if (buffer_result.is_error())
            return Error { Error::Type::Internal, "Failed to allocate hint stream buffer" };
        possible_merged_stream_buffer = buffer_result.release_value();
        MUST(possible_merged_stream_buffer.try_append(primary_hint_stream->bytes()));
        MUST(possible_merged_stream_buffer.try_append(overflow_hint_stream->bytes()));
        hint_stream_bytes = possible_merged_stream_buffer.bytes();
    } else {
        hint_stream_bytes = primary_hint_stream->bytes();
    }

    auto hint_table = TRY(parse_page_offset_hint_table(hint_stream_bytes));
    auto hint_table_entries = parse_all_page_offset_hint_table_entries(hint_table, hint_stream_bytes);

    // FIXME: Do something with the hint tables
    return {};
}

PDFErrorOr<void> Parser::initialize_non_linearized_xref_table()
{
    m_reader.move_to(m_reader.bytes().size() - 1);
    if (!navigate_to_before_eof_marker())
        return error("No EOF marker");
    if (!navigate_to_after_startxref())
        return error("No xref");

    m_reader.set_reading_forwards();
    auto xref_offset_value = parse_number();
    if (xref_offset_value.is_error() || !xref_offset_value.value().has<int>())
        return error("Invalid xref offset");
    auto xref_offset = xref_offset_value.value().get<int>();

    m_reader.move_to(xref_offset);
    m_xref_table = TRY(parse_xref_table());
    m_trailer = TRY(parse_file_trailer());
    return {};
}

PDFErrorOr<NonnullRefPtr<XRefTable>> Parser::parse_xref_table()
{
    if (!m_reader.matches("xref"))
        return error("Expected \"xref\"");
    m_reader.move_by(4);
    if (!m_reader.consume_eol())
        return error("Expected newline after \"xref\"");

    auto table = adopt_ref(*new XRefTable());

    do {
        if (m_reader.matches("trailer"))
            return table;

        Vector<XRefEntry> entries;

        auto starting_index_value = TRY(parse_number());
        auto starting_index = starting_index_value.get<int>();
        auto object_count_value = TRY(parse_number());
        auto object_count = object_count_value.get<int>();

        for (int i = 0; i < object_count; i++) {
            auto offset_string = String(m_reader.bytes().slice(m_reader.offset(), 10));
            m_reader.move_by(10);
            if (!m_reader.consume(' '))
                return error("Malformed xref entry");

            auto generation_string = String(m_reader.bytes().slice(m_reader.offset(), 5));
            m_reader.move_by(5);
            if (!m_reader.consume(' '))
                return error("Malformed xref entry");

            auto letter = m_reader.read();
            if (letter != 'n' && letter != 'f')
                return error("Malformed xref entry");

            // The line ending sequence can be one of the following:
            // SP CR, SP LF, or CR LF
            if (m_reader.matches(' ')) {
                m_reader.consume();
                auto ch = m_reader.consume();
                if (ch != '\r' && ch != '\n')
                    return error("Malformed xref entry");
            } else {
                if (!m_reader.matches("\r\n"))
                    return error("Malformed xref entry");
                m_reader.move_by(2);
            }

            auto offset = strtol(offset_string.characters(), nullptr, 10);
            auto generation = strtol(generation_string.characters(), nullptr, 10);

            entries.append({ offset, static_cast<u16>(generation), letter == 'n' });
        }

        table->add_section({ starting_index, object_count, entries });
    } while (m_reader.matches_number());

    return table;
}

PDFErrorOr<NonnullRefPtr<DictObject>> Parser::parse_file_trailer()
{
    while (m_reader.matches_eol())
        m_reader.consume_eol();

    if (!m_reader.matches("trailer"))
        return error("Expected \"trailer\" keyword");
    m_reader.move_by(7);
    m_reader.consume_whitespace();
    auto dict = TRY(parse_dict());

    if (!m_reader.matches("startxref"))
        return error("Expected \"startxref\"");
    m_reader.move_by(9);
    m_reader.consume_whitespace();

    m_reader.move_until([&](auto) { return m_reader.matches_eol(); });
    VERIFY(m_reader.consume_eol());
    if (!m_reader.matches("%%EOF"))
        return error("Expected \"%%EOF\"");

    m_reader.move_by(5);
    m_reader.consume_whitespace();
    return dict;
}

PDFErrorOr<Parser::PageOffsetHintTable> Parser::parse_page_offset_hint_table(ReadonlyBytes hint_stream_bytes)
{
    if (hint_stream_bytes.size() < sizeof(PageOffsetHintTable))
        return error("Hint stream is too small");

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

Vector<Parser::PageOffsetHintTableEntry> Parser::parse_all_page_offset_hint_table_entries(PageOffsetHintTable const& hint_table, ReadonlyBytes hint_stream_bytes)
{
    InputMemoryStream input_stream(hint_stream_bytes);
    input_stream.seek(sizeof(PageOffsetHintTable));

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
        m_reader.move_until([&](auto) { return m_reader.matches_eol(); });
        if (m_reader.done())
            return false;

        m_reader.consume_eol();
        if (!m_reader.matches("%%EOF"))
            continue;

        m_reader.move_by(5);
        if (!m_reader.matches_eol())
            continue;
        m_reader.consume_eol();
        return true;
    }

    return false;
}

bool Parser::navigate_to_after_startxref()
{
    m_reader.set_reading_backwards();

    while (!m_reader.done()) {
        m_reader.move_until([&](auto) { return m_reader.matches_eol(); });
        auto offset = m_reader.offset() + 1;

        m_reader.consume_eol();
        if (!m_reader.matches("startxref"))
            continue;

        m_reader.move_by(9);
        if (!m_reader.matches_eol())
            continue;

        m_reader.move_to(offset);
        return true;
    }

    return false;
}

String Parser::parse_comment()
{
    if (!m_reader.matches('%'))
        return {};

    m_reader.consume();
    auto comment_start_offset = m_reader.offset();
    m_reader.move_until([&](auto) {
        return m_reader.matches_eol();
    });
    String str = StringView(m_reader.bytes().slice(comment_start_offset, m_reader.offset() - comment_start_offset));
    m_reader.consume_eol();
    m_reader.consume_whitespace();
    return str;
}

PDFErrorOr<Value> Parser::parse_value()
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

    if (m_reader.matches_number())
        return parse_possible_indirect_value_or_ref();

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

    return error(String::formatted("Unexpected char \"{}\"", m_reader.peek()));
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
    if (m_reader.matches_eol())
        m_reader.consume_eol();

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

    auto string = String(m_reader.bytes().slice(start_offset, m_reader.offset() - start_offset));
    float f = strtof(string.characters(), nullptr);
    if (is_float)
        return Value(f);

    VERIFY(floorf(f) == f);
    return Value(static_cast<int>(f));
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

    return make_object<NameObject>(builder.to_string());
}

NonnullRefPtr<StringObject> Parser::parse_string()
{
    ScopeGuard guard([&] { m_reader.consume_whitespace(); });

    String string;
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

    if (m_document->security_handler() && !m_disable_encryption)
        m_document->security_handler()->decrypt(string_object, m_current_reference_stack.last());

    auto unencrypted_string = string_object->string();

    if (unencrypted_string.bytes().starts_with(Array<u8, 2> { 0xfe, 0xff })) {
        // The string is encoded in UTF16-BE
        string_object->set_string(TextCodec::decoder_for("utf-16be")->to_utf8(unencrypted_string));
    } else if (unencrypted_string.bytes().starts_with(Array<u8, 3> { 239, 187, 191 })) {
        // The string is encoded in UTF-8. This is the default anyways, but if these bytes
        // are explicitly included, we have to trim them
        string_object->set_string(unencrypted_string.substring(3));
    }

    return string_object;
}

String Parser::parse_literal_string()
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

    return builder.to_string();
}

String Parser::parse_hex_string()
{
    VERIFY(m_reader.consume('<'));

    StringBuilder builder;

    while (true) {
        if (m_reader.matches('>')) {
            m_reader.consume();
            return builder.to_string();
        } else {
            int hex_value = 0;

            for (int i = 0; i < 2; i++) {
                auto ch = m_reader.consume();
                if (ch == '>') {
                    // The hex string contains an odd number of characters, and the last character
                    // is assumed to be '0'
                    m_reader.consume();
                    hex_value *= 16;
                    builder.append(static_cast<char>(hex_value));
                    return builder.to_string();
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
    HashMap<FlyString, Value> map;

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

    return make_object<DictObject>(map);
}

PDFErrorOr<RefPtr<DictObject>> Parser::conditionally_parse_page_tree_node(u32 object_index)
{
    VERIFY(m_xref_table->has_object(object_index));
    auto byte_offset = m_xref_table->byte_offset_for_object(object_index);

    m_reader.move_to(byte_offset);
    TRY(parse_number());
    TRY(parse_number());
    if (!m_reader.matches("obj"))
        return error(String::formatted("Invalid page tree offset {}", object_index));

    m_reader.move_by(3);
    m_reader.consume_whitespace();

    VERIFY(m_reader.consume('<') && m_reader.consume('<'));

    m_reader.consume_whitespace();
    HashMap<FlyString, Value> map;

    while (true) {
        if (m_reader.matches(">>"))
            break;
        auto name = TRY(parse_name());
        auto name_string = name->name();
        if (!name_string.is_one_of(CommonNames::Type, CommonNames::Parent, CommonNames::Kids, CommonNames::Count)) {
            // This is a page, not a page tree node
            return RefPtr<DictObject> {};
        }

        auto value = TRY(parse_value());
        if (name_string == CommonNames::Type) {
            if (!value.has<NonnullRefPtr<Object>>())
                return RefPtr<DictObject> {};
            auto type_object = value.get<NonnullRefPtr<Object>>();
            if (!type_object->is<NameObject>())
                return RefPtr<DictObject> {};
            auto type_name = type_object->cast<NameObject>();
            if (type_name->name() != CommonNames::Pages)
                return RefPtr<DictObject> {};
        }
        map.set(name->name(), value);
    }

    VERIFY(m_reader.consume('>') && m_reader.consume('>'));
    m_reader.consume_whitespace();

    return make_object<DictObject>(map);
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
    if (maybe_length.has_value() && (!maybe_length->has<Reference>() || m_xref_table)) {
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

        while (true) {
            m_reader.move_until([&](auto) { return m_reader.matches_eol(); });
            auto potential_stream_end = m_reader.offset();
            m_reader.consume_eol();
            if (!m_reader.matches("endstream"))
                continue;

            bytes = m_reader.bytes().slice(stream_start, potential_stream_end - stream_start);
            break;
        }
    }

    m_reader.move_by(9);
    m_reader.consume_whitespace();

    auto stream_object = make_object<StreamObject>(dict, MUST(ByteBuffer::copy(bytes)));

    if (m_document->security_handler() && !m_disable_encryption)
        m_document->security_handler()->decrypt(stream_object, m_current_reference_stack.last());

    if (dict->contains(CommonNames::Filter)) {
        auto filter_type = MUST(dict->get_name(m_document, CommonNames::Filter))->name();
        auto maybe_bytes = Filter::decode(stream_object->bytes(), filter_type);
        if (maybe_bytes.is_error()) {
            warnln("Failed to decode filter: {}", maybe_bytes.error().string_literal());
            return error(String::formatted("Failed to decode filter {}", maybe_bytes.error().string_literal()));
        }
        stream_object->buffer() = maybe_bytes.release_value();
    }

    return stream_object;
}

PDFErrorOr<Vector<Operator>> Parser::parse_operators()
{
    Vector<Operator> operators;
    Vector<Value> operator_args;

    constexpr static auto is_operator_char = [](char ch) {
        return isalpha(ch) || ch == '*' || ch == '\'';
    };

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

        operator_args.append(TRY(parse_value()));
    }

    return operators;
}

Error Parser::error(
    String const& message
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

namespace AK {

template<>
struct Formatter<PDF::Parser::LinearizationDictionary> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& format_builder, PDF::Parser::LinearizationDictionary const& dict)
    {
        StringBuilder builder;
        builder.append("{\n"sv);
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
        builder.append("{\n"sv);
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
        builder.append("{\n"sv);
        builder.appendff("  objects_in_page_number={}\n", entry.objects_in_page_number);
        builder.appendff("  page_length_number={}\n", entry.page_length_number);
        builder.appendff("  number_of_shared_objects={}\n", entry.number_of_shared_objects);
        builder.append("  shared_object_identifiers=["sv);
        for (auto& identifier : entry.shared_object_identifiers)
            builder.appendff(" {}", identifier);
        builder.append(" ]\n"sv);
        builder.append("  shared_object_location_numerators=["sv);
        for (auto& numerator : entry.shared_object_location_numerators)
            builder.appendff(" {}", numerator);
        builder.append(" ]\n"sv);
        builder.appendff("  page_content_stream_offset_number={}\n", entry.page_content_stream_offset_number);
        builder.appendff("  page_content_stream_length_number={}\n", entry.page_content_stream_length_number);
        builder.append('}');
        return Formatter<StringView>::format(format_builder, builder.to_string());
    }
};

}
