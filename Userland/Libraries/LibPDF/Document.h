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

namespace PDF {

struct Rectangle {
    float lower_left_x;
    float lower_left_y;
    float upper_right_x;
    float upper_right_y;

    float width() const { return upper_right_x - lower_left_x; }
    float height() const { return upper_right_y - lower_left_y; }
};

struct Page {
    NonnullRefPtr<DictObject> resources;
    NonnullRefPtr<Object> contents;
    Rectangle media_box;
    Rectangle crop_box;
    float user_unit;
    int rotate;
};

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
    Value page;
    Vector<float> parameters;
};

struct OutlineItem final : public RefCounted<OutlineItem> {
    RefPtr<OutlineItem> parent;
    NonnullRefPtrVector<OutlineItem> children;
    String title;
    i32 count { 0 };
    Destination dest;
    Gfx::Color color { Color::NamedColor::Black }; // 'C' in the PDF spec
    bool italic { false };                         // bit 0 of 'F' in the PDF spec
    bool bold { false };                           // bit 0 of 'F' in the PDF spec

    OutlineItem() = default;

    String to_string(int indent) const;
};

struct OutlineDict final : public RefCounted<OutlineDict> {
    NonnullRefPtrVector<OutlineItem> children;
    u32 count { 0 };

    OutlineDict() = default;
};

class Document final
    : public RefCounted<Document>
    , public Weakable<Document> {
public:
    static PDFErrorOr<NonnullRefPtr<Document>> create(ReadonlyBytes bytes);

    // If a security handler is present, it is the caller's responsibility to ensure
    // this document is unencrypted before calling this function. The user does not
    // need to handle the case where the user password is the empty string.
    PDFErrorOr<void> initialize();

    ALWAYS_INLINE RefPtr<SecurityHandler> const& security_handler() const { return m_security_handler; }

    ALWAYS_INLINE RefPtr<OutlineDict> const& outline() const { return m_outline; }

    ALWAYS_INLINE RefPtr<DictObject> const& trailer() const { return m_trailer; }

    [[nodiscard]] PDFErrorOr<Value> get_or_load_value(u32 index);

    [[nodiscard]] u32 get_first_page_index() const;

    [[nodiscard]] u32 get_page_count() const;

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
        auto resolved = TRY(resolve(value));

        if constexpr (IsSame<T, bool>)
            return resolved.get<bool>();
        else if constexpr (IsSame<T, int>)
            return resolved.get<int>();
        else if constexpr (IsSame<T, float>)
            return resolved.get<float>();
        else if constexpr (IsSame<T, Object>)
            return resolved.get<NonnullRefPtr<Object>>();
        else if constexpr (IsObject<T>)
            return resolved.get<NonnullRefPtr<Object>>()->cast<T>();

        VERIFY_NOT_REACHED();
    }

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
    PDFErrorOr<NonnullRefPtr<OutlineItem>> build_outline_item(NonnullRefPtr<DictObject> const& outline_item_dict);
    PDFErrorOr<NonnullRefPtrVector<OutlineItem>> build_outline_item_chain(Value const& first_ref, Value const& last_ref);

    PDFErrorOr<Destination> create_destination_from_parameters(NonnullRefPtr<ArrayObject>);

    PDFErrorOr<NonnullRefPtr<Object>> get_inheritable_object(FlyString const& name, NonnullRefPtr<DictObject>);

    NonnullRefPtr<DocumentParser> m_parser;
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
struct Formatter<PDF::Rectangle> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, PDF::Rectangle const& rectangle)
    {
        return Formatter<FormatString>::format(builder,
            "Rectangle {{ ll=({}, {}), ur=({}, {}) }}"sv,
            rectangle.lower_left_x,
            rectangle.lower_left_y,
            rectangle.upper_right_x,
            rectangle.upper_right_y);
    }
};

template<>
struct Formatter<PDF::Page> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, PDF::Page const& page)
    {
        return Formatter<FormatString>::format(builder,
            "Page {{\n  resources={}\n  contents={}\n  media_box={}\n  crop_box={}\n  user_unit={}\n  rotate={}\n}}"sv,
            page.resources->to_string(1),
            page.contents->to_string(1),
            page.media_box,
            page.crop_box,
            page.user_unit,
            page.rotate);
    }
};

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
        for (auto& param : destination.parameters)
            param_builder.appendff("{} ", param);

        return Formatter<FormatString>::format(builder, "{{ type={} page={} params={} }}"sv, type_str, destination.page, param_builder.to_string());
    }
};

template<>
struct Formatter<PDF::OutlineItem> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, PDF::OutlineItem const& item)
    {
        return builder.put_string(item.to_string(0));
    }
};

template<>
struct Formatter<PDF::OutlineDict> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, PDF::OutlineDict const& dict)
    {
        StringBuilder child_builder;
        child_builder.append('[');
        for (auto& child : dict.children)
            child_builder.appendff("{}\n", child.to_string(2));
        child_builder.append("  ]"sv);

        return Formatter<FormatString>::format(builder,
            "OutlineDict {{\n  count={}\n  children={}\n}}"sv, dict.count, child_builder.to_string());
    }
};

}
