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

class Parser {
public:
    static Vector<Command> parse_graphics_commands(const ReadonlyBytes&);

    Parser(Badge<Document>, const ReadonlyBytes&);

    void set_document(RefPtr<Document> document) { m_document = document; }

    bool perform_validation();

    struct XRefTableAndTrailer {
        XRefTable xref_table;
        NonnullRefPtr<DictObject> trailer;
    };
    XRefTableAndTrailer parse_last_xref_table_and_trailer();

    NonnullRefPtr<IndirectValue> parse_indirect_value_at_offset(size_t offset);

    RefPtr<DictObject> conditionally_parse_page_tree_node_at_offset(size_t offset);

private:
    explicit Parser(const ReadonlyBytes&);

    bool parse_header();
    XRefTable parse_xref_table();
    NonnullRefPtr<DictObject> parse_file_trailer();

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
    NonnullRefPtr<IndirectValue> parse_indirect_value(int index, int generation);
    NonnullRefPtr<IndirectValue> parse_indirect_value();
    Value parse_number();
    NonnullRefPtr<NameObject> parse_name();
    NonnullRefPtr<StringObject> parse_string();
    String parse_literal_string();
    String parse_hex_string();
    NonnullRefPtr<ArrayObject> parse_array();
    NonnullRefPtr<DictObject> parse_dict();
    NonnullRefPtr<StreamObject> parse_stream(NonnullRefPtr<DictObject> dict);

    Vector<Command> parse_graphics_commands();

    bool matches_eol() const;
    bool matches_whitespace() const;
    bool matches_number() const;
    bool matches_delimiter() const;
    bool matches_regular_character() const;

    void consume_eol();
    bool consume_whitespace();
    char consume();
    void consume(char);

    Reader m_reader;
    RefPtr<Document> m_document;
};

}
