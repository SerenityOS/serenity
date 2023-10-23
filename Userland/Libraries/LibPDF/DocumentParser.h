/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibPDF/Parser.h>

namespace PDF {

struct Version {
    int major { 0 };
    int minor { 0 };
};

class DocumentParser final : public RefCounted<DocumentParser>
    , public Parser {
public:
    static PDFErrorOr<size_t> scan_for_header_start(ReadonlyBytes);

    DocumentParser(Document*, ReadonlyBytes);

    enum class LinearizationResult {
        NotLinearized,
        Linearized,
    };

    [[nodiscard]] ALWAYS_INLINE RefPtr<DictObject> const& trailer() const { return m_xref_table->trailer(); }

    // Parses the header and initializes the xref table and trailer
    PDFErrorOr<Version> initialize();

    bool can_resolve_references() { return m_xref_table; }

    PDFErrorOr<Value> parse_object_with_index(u32 index);

    // Specialized version of parse_dict which aborts early if the dict being parsed
    // is not a page object
    PDFErrorOr<RefPtr<DictObject>> conditionally_parse_page_tree_node(u32 object_index);

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

    PDFErrorOr<Version> parse_header();
    PDFErrorOr<LinearizationResult> initialize_linearization_dict();
    PDFErrorOr<void> initialize_linearized_xref_table();
    PDFErrorOr<void> initialize_non_linearized_xref_table();
    PDFErrorOr<void> validate_xref_table_and_fix_if_necessary();
    PDFErrorOr<void> initialize_hint_tables();
    PDFErrorOr<PageOffsetHintTable> parse_page_offset_hint_table(ReadonlyBytes hint_stream_bytes);
    PDFErrorOr<Vector<PageOffsetHintTableEntry>> parse_all_page_offset_hint_table_entries(PageOffsetHintTable const&, ReadonlyBytes hint_stream_bytes);
    PDFErrorOr<NonnullRefPtr<XRefTable>> parse_xref_stream();
    PDFErrorOr<NonnullRefPtr<XRefTable>> parse_xref_table();
    PDFErrorOr<NonnullRefPtr<DictObject>> parse_file_trailer();
    PDFErrorOr<Value> parse_compressed_object_with_index(u32 index);

    bool navigate_to_before_eof_marker();
    bool navigate_to_after_startxref();

    RefPtr<XRefTable> m_xref_table;
    Optional<LinearizationDictionary> m_linearization_dictionary;
};

}
