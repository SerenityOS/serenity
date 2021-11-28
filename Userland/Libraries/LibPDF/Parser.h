/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <AK/WeakPtr.h>
#include <LibPDF/Command.h>
#include <LibPDF/Object.h>
#include <LibPDF/Reader.h>
#include <LibPDF/XRefTable.h>

namespace PDF {

class Document;

class Parser final : public RefCounted<Parser> {
public:
    enum class LinearizationResult {
        Error,
        NotLinearized,
        Linearized,
    };

    static Vector<Command> parse_graphics_commands(ReadonlyBytes);

    Parser(Badge<Document>, ReadonlyBytes);

    [[nodiscard]] ALWAYS_INLINE RefPtr<DictObject> const& trailer() const { return m_trailer; }
    void set_document(WeakPtr<Document> const&);

    // Parses the header and initializes the xref table and trailer
    bool initialize();

    Value parse_object_with_index(u32 index);

    // Specialized version of parse_dict which aborts early if the dict being parsed
    // is not a page object. A null RefPtr return indicates that the dict at this index
    // is not a page tree node, whereas ok == false indicates a malformed PDF file and
    // should cause an abort of the current operation.
    RefPtr<DictObject> conditionally_parse_page_tree_node(u32 object_index, bool& ok);

private:
    struct LinearizationDictionary {
        u32 length_of_file { 0 };
        u32 primary_hint_stream_offset { 0 };
        u32 primary_hint_stream_length { 0 };
        u32 overflow_hint_stream_offset { 0 };
        u32 overflow_hint_stream_length { 0 };
        u32 first_page_object_number { 0 };
        u32 offset_of_first_page_end { 0 };
        u16 number_of_pages { 0 };
        u32 offset_of_main_xref_table { 0 };
        u32 first_page { 0 }; // The page to initially open (I think, the spec isn't all that clear here)
    };

    struct PageOffsetHintTable {
        u32 least_number_of_objects_in_a_page { 0 };
        u32 location_of_first_page_object { 0 };
        u16 bits_required_for_object_number { 0 };
        u32 least_length_of_a_page { 0 };
        u16 bits_required_for_page_length { 0 };
        u32 least_offset_of_any_content_stream { 0 };
        u16 bits_required_for_content_stream_offsets { 0 };
        u32 least_content_stream_length { 0 };
        u16 bits_required_for_content_stream_length { 0 };
        u16 bits_required_for_number_of_shared_obj_refs { 0 };
        u16 bits_required_for_greatest_shared_obj_identifier { 0 };
        u16 bits_required_for_fraction_numerator { 0 };
        u16 shared_object_reference_fraction_denominator { 0 };
    };

    struct PageOffsetHintTableEntry {
        u32 objects_in_page_number { 0 };
        u32 page_length_number { 0 };
        u32 number_of_shared_objects { 0 };
        Vector<u32> shared_object_identifiers {};
        Vector<u32> shared_object_location_numerators {};
        u32 page_content_stream_offset_number { 0 };
        u32 page_content_stream_length_number { 0 };
    };

    friend struct AK::Formatter<LinearizationDictionary>;
    friend struct AK::Formatter<PageOffsetHintTable>;
    friend struct AK::Formatter<PageOffsetHintTableEntry>;

    explicit Parser(ReadonlyBytes);

    bool parse_header();
    LinearizationResult initialize_linearization_dict();
    bool initialize_linearized_xref_table();
    bool initialize_non_linearized_xref_table();
    bool initialize_hint_tables();
    Optional<PageOffsetHintTable> parse_page_offset_hint_table(ReadonlyBytes hint_stream_bytes);
    Optional<Vector<PageOffsetHintTableEntry>> parse_all_page_offset_hint_table_entries(PageOffsetHintTable const&, ReadonlyBytes hint_stream_bytes);
    RefPtr<XRefTable> parse_xref_table();
    RefPtr<DictObject> parse_file_trailer();

    bool navigate_to_before_eof_marker();
    bool navigate_to_after_startxref();

    // If the PDF is linearized, the first object will be the linearization
    // parameter dictionary, and it will always occur within the first 1024 bytes.
    // We do a very sloppy and context-free search for this object. A return value
    // of true does not necessarily mean this PDF is linearized, but a return value
    // of false does mean this PDF is not linearized.
    // FIXME: false doesn't guarantee non-linearization, but we VERIFY the result!
    bool sloppy_is_linearized();

    String parse_comment();

    Value parse_value();
    Value parse_possible_indirect_value_or_ref();
    RefPtr<IndirectValue> parse_indirect_value(int index, int generation);
    RefPtr<IndirectValue> parse_indirect_value();
    Value parse_number();
    RefPtr<NameObject> parse_name();
    RefPtr<StringObject> parse_string();
    String parse_literal_string();
    String parse_hex_string();
    RefPtr<ArrayObject> parse_array();
    RefPtr<DictObject> parse_dict();
    RefPtr<StreamObject> parse_stream(NonnullRefPtr<DictObject> dict);

    Vector<Command> parse_graphics_commands();

    bool matches_eol() const;
    bool matches_whitespace() const;
    bool matches_number() const;
    bool matches_delimiter() const;
    bool matches_regular_character() const;

    bool consume_eol();
    bool consume_whitespace();
    char consume();
    void consume(int amount);
    bool consume(char);

    Reader m_reader;
    WeakPtr<Document> m_document;
    RefPtr<XRefTable> m_xref_table;
    RefPtr<DictObject> m_trailer;
    Optional<LinearizationDictionary> m_linearization_dictionary;
};

};
