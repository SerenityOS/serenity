/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/HashMap.h>
#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <LibGfx/Color.h>
#include <LibPDF/DocumentParser.h>
#include <LibPDF/Encryption.h>
#include <LibPDF/Error.h>
#include <LibPDF/ObjectDerivatives.h>
#include <LibPDF/Page.h>

namespace PDF {

struct Destination {
    enum class Type {
        XYZ,
        Fit,
        FitH,
        FitV,
        FitR,
        FitB,
        FitBH,
        FitBV,
    };

    Type type;
    Optional<u32> page;
    Vector<Optional<float>> parameters;
};

struct OutlineItem final : public RefCounted<OutlineItem>
    , public Weakable<OutlineItem> {
    WeakPtr<OutlineItem> parent;
    Vector<NonnullRefPtr<OutlineItem>> children;
    String title;
    i32 count { 0 };
    Destination dest;
    Gfx::Color color { Color::NamedColor::Black }; // 'C' in the PDF spec
    bool italic { false };                         // bit 0 of 'F' in the PDF spec
    bool bold { false };                           // bit 0 of 'F' in the PDF spec

    OutlineItem() = default;

    ByteString to_byte_string(int indent) const;
};

struct OutlineDict final : public RefCounted<OutlineDict> {
    Vector<NonnullRefPtr<OutlineItem>> children;
    u32 count { 0 };

    OutlineDict() = default;
};

class InfoDict {
public:
    InfoDict(Document* document, NonnullRefPtr<DictObject> dict)
        : m_document(document)
        , m_info_dict(move(dict))
    {
    }

    // These all return strings that are already converted to UTF-8.

    PDFErrorOr<Optional<String>> title() const;
    PDFErrorOr<Optional<String>> author() const;
    PDFErrorOr<Optional<String>> subject() const;
    PDFErrorOr<Optional<String>> keywords() const;

    // Name of the program that created the original, non-PDF file.
    PDFErrorOr<Optional<String>> creator() const;

    // Name of the program that converted the file to PDF.
    PDFErrorOr<Optional<String>> producer() const;

    // FIXME: Provide some helper for parsing the date strings returned by these two methods.
    PDFErrorOr<Optional<ByteString>> creation_date() const;
    PDFErrorOr<Optional<ByteString>> modification_date() const;

private:
    PDFErrorOr<Optional<ByteString>> get(DeprecatedFlyString const& name) const
    {
        if (!m_info_dict->contains(name))
            return OptionalNone {};
        return TRY(m_info_dict->get_string(m_document, name))->string();
    }

    PDFErrorOr<Optional<String>> get_text(DeprecatedFlyString const& name) const;

    WeakPtr<Document> m_document;
    NonnullRefPtr<DictObject> m_info_dict;
};

class Document final
    : public RefCounted<Document>
    , public Weakable<Document> {
public:
    // Converts a text string (PDF 1.7 spec, 3.8.1. "String Types") to UTF-8.
    static ErrorOr<String> text_string_to_utf8(ByteString const&);

    static PDFErrorOr<NonnullRefPtr<Document>> create(ReadonlyBytes bytes);

    // If a security handler is present, it is the caller's responsibility to ensure
    // this document is unencrypted before calling this function. The user does not
    // need to handle the case where the user password is the empty string.
    PDFErrorOr<void> initialize();

    Version version() const { return m_version; }

    ALWAYS_INLINE RefPtr<SecurityHandler> const& security_handler() const { return m_security_handler; }

    ALWAYS_INLINE RefPtr<OutlineDict> const& outline() const { return m_outline; }

    ALWAYS_INLINE RefPtr<DictObject> const& trailer() const { return m_trailer; }

    [[nodiscard]] PDFErrorOr<Value> get_or_load_value(u32 index);

    [[nodiscard]] u32 get_first_page_index() const;

    [[nodiscard]] u32 get_page_count() const;

    PDFErrorOr<void> dump_page(u32 index);
    [[nodiscard]] PDFErrorOr<Page> get_page(u32 index);

    ALWAYS_INLINE Value get_value(u32 index) const
    {
        return m_values.get(index).value_or({});
    }

    // Strips away the layer of indirection by turning indirect value
    // refs into the value they reference, and indirect values into
    // the value being wrapped.
    PDFErrorOr<Value> resolve(Value const& value);

    // Like resolve, but unwraps the Value into the given type. Accepts
    // any object type, and the three primitive Value types.
    template<IsValueType T>
    PDFErrorOr<UnwrappedValueType<T>> resolve_to(Value const& value)
    {
        return cast_to<T>(TRY(resolve(value)));
    }

    /// Whether this Document is ready to resolve references, which is usually
    /// true, except just before the XRef table is parsed (and while the linearization
    /// dict is being read).
    bool can_resolve_references() { return m_parser->can_resolve_references(); }

    PDFErrorOr<Optional<InfoDict>> info_dict();

    PDFErrorOr<Vector<DeprecatedFlyString>> read_filters(NonnullRefPtr<DictObject>);

    PDFErrorOr<void> unfilter_stream(NonnullRefPtr<StreamObject> stream) { return m_parser->unfilter_stream(move(stream)); }

private:
    explicit Document(NonnullRefPtr<DocumentParser> const& parser);

    // FIXME: Currently, to improve performance, we don't load any pages at Document
    // construction, rather we just load the page structure and populate
    // m_page_object_indices. However, we can be even lazier and defer page tree node
    // parsing, as good PDF writers will layout the page tree in a balanced tree to
    // improve lookup time. This would reduce the initial overhead by not loading
    // every page tree node of, say, a 1000+ page PDF file.
    PDFErrorOr<void> build_page_tree();
    PDFErrorOr<void> add_page_tree_node_to_page_tree(NonnullRefPtr<DictObject> const& page_tree);

    PDFErrorOr<void> build_outline();
    PDFErrorOr<NonnullRefPtr<OutlineItem>> build_outline_item(NonnullRefPtr<DictObject> const& outline_item_dict, HashMap<u32, u32> const&);
    PDFErrorOr<Vector<NonnullRefPtr<OutlineItem>>> build_outline_item_chain(Value const& first_ref, HashMap<u32, u32> const&);

    PDFErrorOr<Destination> create_destination_from_parameters(NonnullRefPtr<ArrayObject>, HashMap<u32, u32> const&);
    PDFErrorOr<Destination> create_destination_from_dictionary_entry(NonnullRefPtr<Object> const& entry, HashMap<u32, u32> const& page_number_by_index_ref);
    PDFErrorOr<Destination> create_destination_from_object(NonnullRefPtr<Object> const& dest_obj, HashMap<u32, u32> const& page_number_by_index_ref);

    PDFErrorOr<Optional<NonnullRefPtr<Object>>> get_inheritable_object(DeprecatedFlyString const& name, NonnullRefPtr<DictObject>);
    PDFErrorOr<Optional<Value>> get_inheritable_value(DeprecatedFlyString const& name, NonnullRefPtr<DictObject>);

    PDFErrorOr<NonnullRefPtr<Object>> find_in_name_tree(NonnullRefPtr<DictObject> root, DeprecatedFlyString name);
    PDFErrorOr<NonnullRefPtr<Object>> find_in_name_tree_nodes(NonnullRefPtr<ArrayObject> siblings, DeprecatedFlyString name);
    PDFErrorOr<NonnullRefPtr<Object>> find_in_key_value_array(NonnullRefPtr<ArrayObject> key_value_array, DeprecatedFlyString name);

    NonnullRefPtr<DocumentParser> m_parser;
    Version m_version;
    RefPtr<DictObject> m_catalog;
    RefPtr<DictObject> m_trailer;
    Vector<u32> m_page_object_indices;
    HashMap<u32, Page> m_pages;
    HashMap<u32, Value> m_values;
    RefPtr<OutlineDict> m_outline;
    RefPtr<SecurityHandler> m_security_handler;
};

}

namespace AK {

template<>
struct Formatter<PDF::Destination> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, PDF::Destination const& destination)
    {
        StringView type_str;
        switch (destination.type) {
        case PDF::Destination::Type::XYZ:
            type_str = "XYZ"sv;
            break;
        case PDF::Destination::Type::Fit:
            type_str = "Fit"sv;
            break;
        case PDF::Destination::Type::FitH:
            type_str = "FitH"sv;
            break;
        case PDF::Destination::Type::FitV:
            type_str = "FitV"sv;
            break;
        case PDF::Destination::Type::FitR:
            type_str = "FitR"sv;
            break;
        case PDF::Destination::Type::FitB:
            type_str = "FitB"sv;
            break;
        case PDF::Destination::Type::FitBH:
            type_str = "FitBH"sv;
            break;
        case PDF::Destination::Type::FitBV:
            type_str = "FitBV"sv;
            break;
        }

        StringBuilder param_builder;
        builder.builder().appendff("{{ type={} page="sv, type_str);
        if (!destination.page.has_value())
            TRY(builder.put_literal("{{}}"sv));
        else
            TRY(builder.put_u64(destination.page.value()));
        if (!destination.parameters.is_empty()) {
            TRY(builder.put_literal(" parameters="sv));
            for (auto const& param : destination.parameters) {
                if (param.has_value())
                    TRY(builder.put_f32_or_f64(param.value()));
                else
                    TRY(builder.put_literal("{{}}"sv));
                TRY(builder.put_literal(" "sv));
            }
        }
        return builder.put_literal(" }}"sv);
    }
};

template<>
struct Formatter<PDF::OutlineItem> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, PDF::OutlineItem const& item)
    {
        return builder.put_string(item.to_byte_string(0));
    }
};

template<>
struct Formatter<PDF::OutlineDict> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, PDF::OutlineDict const& dict)
    {
        StringBuilder child_builder;
        child_builder.append('[');
        for (auto& child : dict.children)
            child_builder.appendff("{}\n", child->to_byte_string(2));
        child_builder.append("  ]"sv);

        return Formatter<FormatString>::format(builder,
            "OutlineDict {{\n  count={}\n  children={}\n}}"sv, dict.count, child_builder.to_byte_string());
    }
};

}
