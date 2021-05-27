/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <LibPDF/Command.h>
#include <LibPDF/Object.h>
#include <LibPDF/Reader.h>
#include <LibPDF/XRefTable.h>

namespace PDF {

class Document;

class Parser final : public RefCounted<Parser> {
public:
    static Vector<Command> parse_graphics_commands(const ReadonlyBytes&);

    Parser(Badge<Document>, const ReadonlyBytes&);

    [[nodiscard]] ALWAYS_INLINE const RefPtr<DictObject>& trailer() const { return m_trailer; }
    void set_document(const RefPtr<Document>& document) { m_document = document; }

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

    friend struct AK::Formatter<LinearizationDictionary>;

    explicit Parser(const ReadonlyBytes&);

    bool parse_header();
    bool initialize_linearization_dict();
    bool initialize_linearized_xref_table();
    bool initialize_non_linearized_xref_table();
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
    RefPtr<Document> m_document;
    RefPtr<XRefTable> m_xref_table;
    RefPtr<DictObject> m_trailer;
    Optional<LinearizationDictionary> m_linearization_dictionary;
};

};
