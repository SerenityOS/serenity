/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2022, Julian Offenhäuser <offenhaeuser@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BitStream.h>
#include <AK/Endian.h>
#include <AK/MemoryStream.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Document.h>
#include <LibPDF/DocumentParser.h>
#include <LibPDF/ObjectDerivatives.h>

namespace PDF {

DocumentParser::DocumentParser(Document* document, ReadonlyBytes bytes)
    : Parser(document, bytes)
{
}

PDFErrorOr<Version> DocumentParser::initialize()
{
    m_reader.set_reading_forwards();
    if (m_reader.remaining() == 0)
        return error("Empty PDF document");

    auto maybe_version = parse_header();
    if (maybe_version.is_error()) {
        warnln("{}", maybe_version.error().message());
        warnln("No valid PDF header detected, continuing anyway.");
        maybe_version = Version { 1, 6 }; // ¯\_(ツ)_/¯
    }

    auto const linearization_result = TRY(initialize_linearization_dict());

    if (linearization_result == LinearizationResult::NotLinearized) {
        TRY(initialize_non_linearized_xref_table());
        return maybe_version.value();
    }

    bool is_linearized = m_linearization_dictionary.has_value();
    if (is_linearized) {
        // If the length given in the linearization dictionary is not equal to the length
        // of the document, then this file has most likely been incrementally updated, and
        // should no longer be treated as linearized.
        // FIXME: This check requires knowing the full size of the file, while linearization
        //        is all about being able to render some of it without having to download all of it.
        //        PDF 2.0 Annex G.7 "Accessing an updated file" talks about this some,
        //        but mostly just throws its hand in the air.
        is_linearized = m_linearization_dictionary.value().length_of_file == m_reader.bytes().size();
    }

    if (is_linearized)
        TRY(initialize_linearized_xref_table());
    else
        TRY(initialize_non_linearized_xref_table());

    return maybe_version.value();
}

PDFErrorOr<Value> DocumentParser::parse_object_with_index(u32 index)
{
    VERIFY(m_xref_table->has_object(index));

    // PDF spec 1.7, Indirect Objects:
    // "An indirect reference to an undefined object is not an error; it is simply treated as a reference to the null object."
    // FIXME: Should this apply to the !has_object() case right above too?
    if (!m_xref_table->is_object_in_use(index))
        return nullptr;

    // If this is called to resolve an indirect object reference while parsing another object,
    // make sure to restore the current position after parsing the indirect object, so that the
    // parser can keep parsing the original object stream afterwards.
    // parse_compressed_object_with_index() also moves the reader's position, so this needs
    // to be before the potential call to parse_compressed_object_with_index().
    class SavePoint {
    public:
        SavePoint(Reader& reader)
            : m_reader(reader)
        {
            m_reader.save();
        }
        ~SavePoint() { m_reader.load(); }

    private:
        Reader& m_reader;
    };
    SavePoint restore_current_position { m_reader };

    if (m_xref_table->is_object_compressed(index))
        // The object can be found in a object stream
        return parse_compressed_object_with_index(index);

    auto byte_offset = m_xref_table->byte_offset_for_object(index);

    m_reader.move_to(byte_offset);
    auto indirect_value = TRY(parse_indirect_value());
    VERIFY(indirect_value->index() == index);
    return indirect_value->value();
}

PDFErrorOr<size_t> DocumentParser::scan_for_header_start(ReadonlyBytes bytes)
{
    // PDF 1.7 spec, APPENDIX H, 3.4.1 "File Header":
    // "13. Acrobat viewers require only that the header appear somewhere within the first 1024 bytes of the file."
    // ...which of course means files depend on it.
    // All offsets in the file are relative to the header start, not to the start of the file.
    StringView first_bytes { bytes.data(), min(bytes.size(), 1024 - "1.4"sv.length()) };
    Optional<size_t> start_offset = first_bytes.find("%PDF-"sv);
    if (!start_offset.has_value())
        return Error { Error::Type::Parse, "Failed to find PDF start" };
    return start_offset.value();
}

PDFErrorOr<Version> DocumentParser::parse_header()
{
    m_reader.move_to(0);
    if (m_reader.remaining() < 8 || !m_reader.matches("%PDF-"))
        return error("Not a PDF document");

    m_reader.move_by(5);

    char major_ver = m_reader.read();
    if (major_ver != '1' && major_ver != '2') {
        dbgln_if(PDF_DEBUG, "Unknown major version \"{}\"", major_ver);
        return error("Unknown major version");
    }

    if (m_reader.read() != '.')
        return error("Malformed PDF version");

    char minor_ver = m_reader.read();
    if (minor_ver < '0' || minor_ver > '7') {
        dbgln_if(PDF_DEBUG, "Unknown minor version \"{}\"", minor_ver);
        return error("Unknown minor version");
    }

    m_reader.consume_eol();
    m_reader.consume_whitespace();

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

    return Version { major_ver - '0', minor_ver - '0' };
}

PDFErrorOr<DocumentParser::LinearizationResult> DocumentParser::initialize_linearization_dict()
{
    // parse_header() is called immediately before this, so we are at the right location.
    // There may not actually be a linearization dict, or even a valid PDF object here.
    // If that is the case, this file may be completely valid but not linearized.

    // If there is indeed a linearization dict, there should be an object number here.
    if (!m_reader.matches_number())
        return LinearizationResult::NotLinearized;

    // At this point, we still don't know for sure if we are dealing with a valid object.

    // The linearization dict is read before decryption state is initialized.
    // A linearization dict only contains numbers, so the decryption dictionary is not been needed (only strings and streams get decrypted, and only streams get unfiltered).
    // But we don't know if the first object is a linearization dictionary until after parsing it, so the object might be a stream.
    // If that stream is encrypted and filtered, we'd try to unfilter it while it's still encrypted, handing encrypted data to the unfiltering algorithms.
    // This makes them assert, since they can't make sense of the encrypted data.
    // So read the first object without unfiltering.
    // If it is a linearization dict, there's no stream data and this has no effect.
    // If it is a stream, this isn't a linearized file and the object will be read on demand (and unfiltered) later, when the object is lazily read via an xref entry.
    set_filters_enabled(false);
    auto indirect_value_or_error = parse_indirect_value();
    set_filters_enabled(true);

    if (indirect_value_or_error.is_error())
        return LinearizationResult::NotLinearized;

    auto dict_value = indirect_value_or_error.value()->value();
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

PDFErrorOr<void> DocumentParser::initialize_linearized_xref_table()
{
    // The linearization parameter dictionary has just been parsed, and the xref table
    // comes immediately after it. We are in the correct spot.
    m_xref_table = TRY(parse_xref_table());

    // Also parse the main xref table and merge into the first-page xref table. Note
    // that we don't use the main xref table offset from the linearization dict because
    // for some reason, it specified the offset of the whitespace after the object
    // index start and length? So it's much easier to do it this way.
    auto main_xref_table_offset = m_xref_table->trailer()->get_value(CommonNames::Prev).to_int();
    m_reader.move_to(main_xref_table_offset);
    auto main_xref_table = TRY(parse_xref_table());
    TRY(m_xref_table->merge(move(*main_xref_table)));

    return validate_xref_table_and_fix_if_necessary();
}

PDFErrorOr<void> DocumentParser::initialize_hint_tables()
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
    auto hint_table_entries = TRY(parse_all_page_offset_hint_table_entries(hint_table, hint_stream_bytes));

    // FIXME: Do something with the hint tables
    return {};
}

PDFErrorOr<void> DocumentParser::initialize_non_linearized_xref_table()
{
    m_reader.move_to(m_reader.bytes().size() - 1);
    if (!navigate_to_before_eof_marker())
        return error("No EOF marker");
    if (!navigate_to_after_startxref())
        return error("No xref");

    m_reader.set_reading_forwards();
    auto xref_offset_value = TRY(parse_number());
    auto xref_offset = TRY(m_document->resolve_to<int>(xref_offset_value));
    m_reader.move_to(xref_offset);

    // As per 7.5.6 Incremental Updates:
    // When a conforming reader reads the file, it shall build its cross-reference
    // information in such a way that the most recent copy of each object shall be
    // the one accessed from the file.
    // NOTE: This means that we have to follow back the chain of XRef table sections
    //       and only add objects that were not already specified in a previous
    //       (and thus newer) XRef section.
    while (1) {
        auto xref_table = TRY(parse_xref_table());
        if (!m_xref_table)
            m_xref_table = xref_table;
        else
            TRY(m_xref_table->merge(move(*xref_table)));

        if (!xref_table->trailer() || !xref_table->trailer()->contains(CommonNames::Prev))
            break;

        auto offset = TRY(m_document->resolve_to<int>(xref_table->trailer()->get_value(CommonNames::Prev)));
        m_reader.move_to(offset);
    }

    return validate_xref_table_and_fix_if_necessary();
}

PDFErrorOr<void> DocumentParser::validate_xref_table_and_fix_if_necessary()
{
    /* While an xref table may start with an object number other than zero, this is
       very uncommon and likely a sign of a document with broken indices.
       Like most other PDF parsers seem to do, we still try to salvage the situation.
       NOTE: This is probably not spec-compliant behavior.*/
    size_t first_valid_index = 0;
    while (!m_xref_table->has_object(first_valid_index))
        first_valid_index++;

    if (first_valid_index) {
        auto& entries = m_xref_table->entries();

        bool need_to_rebuild_table = true;
        for (size_t i = first_valid_index; i < entries.size(); ++i) {
            if (!entries[i].in_use)
                continue;

            size_t actual_object_number = 0;
            if (entries[i].compressed) {
                auto object_stream_index = m_xref_table->object_stream_for_object(i);
                auto stream_offset = m_xref_table->byte_offset_for_object(object_stream_index);
                m_reader.move_to(stream_offset);
                auto first_number = TRY(parse_number());
                actual_object_number = first_number.get_u32();
            } else {
                auto byte_offset = m_xref_table->byte_offset_for_object(i);
                m_reader.move_to(byte_offset);
                auto indirect_value = TRY(parse_indirect_value());
                actual_object_number = indirect_value->index();
            }

            if (actual_object_number != i - first_valid_index) {
                /* Our suspicion was wrong, not all object numbers are shifted equally.
                   This could mean that the document is hopelessly broken, or it just
                   starts at a non-zero object index for some reason. */
                need_to_rebuild_table = false;
                break;
            }
        }

        if (need_to_rebuild_table) {
            warnln("Broken xref table detected, trying to fix it.");
            entries.remove(0, first_valid_index);
        }
    }

    return {};
}

static PDFErrorOr<NonnullRefPtr<StreamObject>> indirect_value_as_stream(NonnullRefPtr<IndirectValue> indirect_value)
{
    auto value = indirect_value->value();
    if (!value.has<NonnullRefPtr<Object>>())
        return Error { Error::Type::Parse, "Expected indirect value to be a stream" };
    auto value_object = value.get<NonnullRefPtr<Object>>();
    if (!value_object->is<StreamObject>())
        return Error { Error::Type::Parse, "Expected indirect value to be a stream" };
    return value_object->cast<StreamObject>();
}

PDFErrorOr<NonnullRefPtr<XRefTable>> DocumentParser::parse_xref_stream()
{
    // PDF 1.7 spec, 3.4.7 "Cross-Reference Streams"
    auto xref_stream = TRY(parse_indirect_value());
    auto stream = TRY(indirect_value_as_stream(xref_stream));

    auto dict = stream->dict();
    auto type = TRY(dict->get_name(m_document, CommonNames::Type))->name();
    if (type != "XRef")
        return error("Malformed xref dictionary");

    auto field_sizes = TRY(dict->get_array(m_document, CommonNames::W));
    if (field_sizes->size() != 3)
        return error("Malformed xref dictionary");
    if (field_sizes->at(1).get_u32() == 0)
        return error("Malformed xref dictionary");

    auto number_of_object_entries = dict->get_value("Size").get<int>();

    struct Subsection {
        int start;
        int count;
    };

    Vector<Subsection> subsections;
    if (dict->contains(CommonNames::Index)) {
        auto index_array = TRY(dict->get_array(m_document, CommonNames::Index));
        if (index_array->size() % 2 != 0)
            return error("Malformed xref dictionary");

        for (size_t i = 0; i < index_array->size(); i += 2)
            subsections.append({ index_array->at(i).get<int>(), index_array->at(i + 1).get<int>() });
    } else {
        subsections.append({ 0, number_of_object_entries });
    }
    auto table = adopt_ref(*new XRefTable());

    auto field_to_long = [](ReadonlyBytes field) -> long {
        long value = 0;
        u8 const max = (field.size() - 1) * 8;
        for (size_t i = 0; i < field.size(); ++i) {
            value |= static_cast<long>(field[i]) << (max - (i * 8));
        }
        return value;
    };

    size_t byte_index = 0;

    for (auto [start, count] : subsections) {
        Vector<XRefEntry> entries;

        for (int i = 0; i < count; i++) {
            Array<u64, 3> fields;
            for (size_t field_index = 0; field_index < 3; ++field_index) {
                if (!field_sizes->at(field_index).has_u32())
                    return error("Malformed xref stream");

                auto field_size = field_sizes->at(field_index).get_u32();
                if (field_size > 8)
                    return error("Malformed xref stream");

                if (byte_index + field_size > stream->bytes().size())
                    return error("The xref stream data cut off early");

                auto field = stream->bytes().slice(byte_index, field_size);
                fields[field_index] = field_to_long(field);
                byte_index += field_size;
            }

            u8 type = 1;
            if (field_sizes->at(0).get_u32() != 0)
                type = fields[0];

            entries.append({ fields[1], static_cast<u16>(fields[2]), type != 0, type == 2 });
        }

        table->add_section({ start, count, move(entries) });
    }

    table->set_trailer(dict);

    return table;
}

PDFErrorOr<NonnullRefPtr<XRefTable>> DocumentParser::parse_xref_table()
{
    if (!m_reader.matches("xref")) {
        // Since version 1.5, there may be a cross-reference stream instead
        return parse_xref_stream();
    }

    m_reader.move_by(4);
    m_reader.consume_non_eol_whitespace();
    if (!m_reader.consume_eol())
        return error("Expected newline after \"xref\"");

    auto table = adopt_ref(*new XRefTable());

    while (m_reader.matches_number()) {
        Vector<XRefEntry> entries;

        auto starting_index_value = TRY(parse_number());
        auto object_count_value = TRY(parse_number());
        if (!(starting_index_value.has_u32() && object_count_value.has_u32()))
            return error("Malformed xref entry");

        auto object_count = object_count_value.get<int>();
        auto starting_index = starting_index_value.get<int>();

        for (int i = 0; i < object_count; i++) {
            auto offset_string = ByteString(m_reader.bytes().slice(m_reader.offset(), 10));
            m_reader.move_by(10);
            if (!m_reader.consume(' '))
                return error("Malformed xref entry");

            auto generation_string = ByteString(m_reader.bytes().slice(m_reader.offset(), 5));
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

            u64 offset = strtoll(offset_string.characters(), nullptr, 10);
            auto generation = strtol(generation_string.characters(), nullptr, 10);

            entries.append({ offset, static_cast<u16>(generation), letter == 'n' });
        }

        table->add_section({ starting_index, object_count, entries });
    }

    m_reader.consume_whitespace();
    if (m_reader.matches("trailer"))
        table->set_trailer(TRY(parse_file_trailer()));

    return table;
}

PDFErrorOr<NonnullRefPtr<DictObject>> DocumentParser::parse_file_trailer()
{
    while (m_reader.matches_eol())
        m_reader.consume_eol();

    if (!m_reader.matches("trailer"))
        return error("Expected \"trailer\" keyword");
    m_reader.move_by(7);
    m_reader.consume_whitespace();
    return parse_dict();
}

PDFErrorOr<Value> DocumentParser::parse_compressed_object_with_index(u32 index)
{
    auto object_stream_index = m_xref_table->object_stream_for_object(index);
    auto stream_offset = m_xref_table->byte_offset_for_object(object_stream_index);

    m_reader.move_to(stream_offset);

    auto obj_stream = TRY(parse_indirect_value());
    auto stream = TRY(indirect_value_as_stream(obj_stream));

    if (obj_stream->index() != object_stream_index)
        return error("Mismatching object stream index");

    auto dict = stream->dict();

    auto type = TRY(dict->get_name(m_document, CommonNames::Type))->name();
    if (type != "ObjStm")
        return error("Invalid object stream type");

    auto object_count = dict->get_value("N").get_u32();
    auto first_object_offset = dict->get_value("First").get_u32();

    Parser stream_parser(m_document, stream->bytes());

    // The data was already decrypted when reading the outer compressed ObjStm.
    stream_parser.set_encryption_enabled(false);

    for (u32 i = 0; i < object_count; ++i) {
        auto object_number = TRY(stream_parser.parse_number());
        auto object_offset = TRY(stream_parser.parse_number());

        if (object_number.get_u32() == index) {
            stream_parser.move_to(first_object_offset + object_offset.get_u32());
            break;
        }
    }

    stream_parser.push_reference({ index, 0 });
    stream_parser.consume_whitespace();
    auto value = TRY(stream_parser.parse_value());
    stream_parser.pop_reference();

    return value;
}

PDFErrorOr<DocumentParser::PageOffsetHintTable> DocumentParser::parse_page_offset_hint_table(ReadonlyBytes hint_stream_bytes)
{
    if (hint_stream_bytes.size() < sizeof(PageOffsetHintTable))
        return error("Hint stream is too small");

    size_t offset = 0;

    auto read_u32 = [&] {
        u32 data = reinterpret_cast<u32 const*>(hint_stream_bytes.data() + offset)[0];
        offset += 4;
        return AK::convert_between_host_and_big_endian(data);
    };

    auto read_u16 = [&] {
        u16 data = reinterpret_cast<u16 const*>(hint_stream_bytes.data() + offset)[0];
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

PDFErrorOr<Vector<DocumentParser::PageOffsetHintTableEntry>> DocumentParser::parse_all_page_offset_hint_table_entries(PageOffsetHintTable const& hint_table, ReadonlyBytes hint_stream_bytes)
{
    auto input_stream = TRY(try_make<FixedMemoryStream>(hint_stream_bytes));
    TRY(input_stream->seek(sizeof(PageOffsetHintTable)));

    LittleEndianInputBitStream bit_stream { move(input_stream) };

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

    auto parse_int_entry = [&](u32 PageOffsetHintTableEntry::*field, u32 bit_size) -> ErrorOr<void> {
        if (bit_size <= 0)
            return {};

        for (int i = 0; i < number_of_pages; i++) {
            auto& entry = entries[i];
            entry.*field = TRY(bit_stream.read_bits(bit_size));
        }

        return {};
    };

    auto parse_vector_entry = [&](Vector<u32> PageOffsetHintTableEntry::*field, u32 bit_size) -> ErrorOr<void> {
        if (bit_size <= 0)
            return {};

        for (int page = 1; page < number_of_pages; page++) {
            auto number_of_shared_objects = entries[page].number_of_shared_objects;
            Vector<u32> items;
            items.ensure_capacity(number_of_shared_objects);

            for (size_t i = 0; i < number_of_shared_objects; i++)
                items.unchecked_append(TRY(bit_stream.read_bits(bit_size)));

            entries[page].*field = move(items);
        }

        return {};
    };

    TRY(parse_int_entry(&PageOffsetHintTableEntry::objects_in_page_number, bits_required_for_object_number));
    TRY(parse_int_entry(&PageOffsetHintTableEntry::page_length_number, bits_required_for_page_length));
    TRY(parse_int_entry(&PageOffsetHintTableEntry::number_of_shared_objects, bits_required_for_number_of_shared_obj_refs));
    TRY(parse_vector_entry(&PageOffsetHintTableEntry::shared_object_identifiers, bits_required_for_greatest_shared_obj_identifier));
    TRY(parse_vector_entry(&PageOffsetHintTableEntry::shared_object_location_numerators, bits_required_for_fraction_numerator));
    TRY(parse_int_entry(&PageOffsetHintTableEntry::page_content_stream_offset_number, bits_required_for_content_stream_offsets));
    TRY(parse_int_entry(&PageOffsetHintTableEntry::page_content_stream_length_number, bits_required_for_content_stream_length));

    return entries;
}

bool DocumentParser::navigate_to_before_eof_marker()
{
    m_reader.set_reading_backwards();

    while (!m_reader.done()) {
        m_reader.consume_eol();
        m_reader.consume_whitespace();
        if (m_reader.matches("%%EOF")) {
            m_reader.move_by(5);
            return true;
        }

        m_reader.move_until([&](auto) { return m_reader.matches_eol(); });
    }

    return false;
}

bool DocumentParser::navigate_to_after_startxref()
{
    m_reader.set_reading_backwards();

    while (!m_reader.done()) {
        m_reader.move_until([&](auto) { return m_reader.matches_eol(); });
        auto offset = m_reader.offset() + 1;

        m_reader.consume_eol();
        m_reader.consume_whitespace();

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

PDFErrorOr<RefPtr<DictObject>> DocumentParser::conditionally_parse_page_tree_node(u32 object_index)
{
    auto dict_value = TRY(parse_object_with_index(object_index));
    auto dict_object = dict_value.get<NonnullRefPtr<Object>>();
    if (!dict_object->is<DictObject>())
        return error(ByteString::formatted("Invalid page tree with xref index {}", object_index));

    auto dict = dict_object->cast<DictObject>();
    if (!dict->contains_any_of(CommonNames::Type, CommonNames::Parent, CommonNames::Kids, CommonNames::Count))
        // This is a page, not a page tree node
        return RefPtr<DictObject> {};

    if (!dict->contains(CommonNames::Type))
        return RefPtr<DictObject> {};
    auto type_object = TRY(dict->get_object(m_document, CommonNames::Type));
    if (!type_object->is<NameObject>())
        return RefPtr<DictObject> {};
    auto type_name = type_object->cast<NameObject>();
    if (type_name->name() != CommonNames::Pages)
        return RefPtr<DictObject> {};

    return dict;
}

}

namespace AK {

template<>
struct Formatter<PDF::DocumentParser::LinearizationDictionary> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& format_builder, PDF::DocumentParser::LinearizationDictionary const& dict)
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
        return Formatter<StringView>::format(format_builder, builder.to_byte_string());
    }
};

template<>
struct Formatter<PDF::DocumentParser::PageOffsetHintTable> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& format_builder, PDF::DocumentParser::PageOffsetHintTable const& table)
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
        return Formatter<StringView>::format(format_builder, builder.to_byte_string());
    }
};

template<>
struct Formatter<PDF::DocumentParser::PageOffsetHintTableEntry> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& format_builder, PDF::DocumentParser::PageOffsetHintTableEntry const& entry)
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
        return Formatter<StringView>::format(format_builder, builder.to_byte_string());
    }
};

}
