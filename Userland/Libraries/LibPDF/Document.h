/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/HashMap.h>
#include <AK/RefCounted.h>
#include <LibGfx/Color.h>
#include <LibPDF/Object.h>
#include <LibPDF/Parser.h>
#include <LibPDF/XRefTable.h>

namespace PDF {

struct Rectangle {
    float lower_left_x;
    float lower_left_y;
    float upper_right_x;
    float upper_right_y;
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

class Document final : public RefCounted<Document> {
public:
    explicit Document(const ReadonlyBytes& bytes);

    ALWAYS_INLINE const XRefTable& xref_table() const { return m_xref_table; }
    ALWAYS_INLINE const DictObject& trailer() const { return *m_trailer; }
    ALWAYS_INLINE const RefPtr<OutlineDict>& outline() const { return m_outline; }

    [[nodiscard]] Value get_or_load_value(u32 index);

    [[nodiscard]] u32 get_first_page_index() const;

    [[nodiscard]] u32 get_page_count() const;

    [[nodiscard]] Page get_page(u32 index);

    ALWAYS_INLINE Value get_value(u32 index) const
    {
        return m_values.get(index).value_or({});
    }

    ALWAYS_INLINE void set_value(u32 index, const Value& value)
    {
        m_values.ensure_capacity(index);
        m_values.set(index, value);
    }

    // Strips away the layer of indirection by turning indirect value
    // refs into the value they reference, and indirect values into
    // the value being wrapped.
    Value resolve(const Value& value);

    // Like resolve, but unwraps the Value into the given type. Accepts
    // any object type, and the three primitive Value types.
    template<IsValueType T>
    UnwrappedValueType<T> resolve_to(const Value& value)
    {
        auto resolved = resolve(value);

        if constexpr (IsSame<T, bool>)
            return resolved.as_bool();
        if constexpr (IsSame<T, int>)
            return resolved.as_int();
        if constexpr (IsSame<T, float>)
            return resolved.as_float();
        if constexpr (IsObject<T>)
            return object_cast<T>(resolved.as_object());

        VERIFY_NOT_REACHED();
    }

private:
    // FIXME: Currently, to improve performance, we don't load any pages at Document
    // construction, rather we just load the page structure and populate
    // m_page_object_indices. However, we can be even lazier and defer page tree node
    // parsing, as good PDF writers will layout the page tree in a balanced tree to
    // improve lookup time. This would reduce the initial overhead by not loading
    // every page tree node of, say, a 1000+ page PDF file.
    void build_page_tree();
    void add_page_tree_node_to_page_tree(NonnullRefPtr<DictObject> page_tree);

    void build_outline();
    NonnullRefPtr<OutlineItem> build_outline_item(NonnullRefPtr<DictObject> outline_item_dict);
    NonnullRefPtrVector<OutlineItem> build_outline_item_chain(const Value& first_ref, const Value& last_ref);

    Parser m_parser;
    XRefTable m_xref_table;
    RefPtr<DictObject> m_trailer;
    RefPtr<DictObject> m_catalog;
    Vector<u32> m_page_object_indices;
    HashMap<u32, Page> m_pages;
    HashMap<u32, Value> m_values;
    RefPtr<OutlineDict> m_outline;
};

}

namespace AK {

template<>
struct Formatter<PDF::Rectangle> : Formatter<StringView> {
    void format(FormatBuilder& builder, const PDF::Rectangle& rectangle)
    {
        Formatter<StringView>::format(builder,
            String::formatted("Rectangle {{ ll=({}, {}), ur=({}, {}) }}",
                rectangle.lower_left_x,
                rectangle.lower_left_y,
                rectangle.upper_right_x,
                rectangle.upper_right_y));
    }
};

template<>
struct Formatter<PDF::Page> : Formatter<StringView> {
    void format(FormatBuilder& builder, const PDF::Page& page)
    {
        constexpr auto fmt_string = "Page {{\n  resources={}\n  contents={}\n  media_box={}\n  crop_box={}\n  user_unit={}\n  rotate={}\n}}";
        auto str = String::formatted(fmt_string,
            page.resources->to_string(1),
            page.contents->to_string(1),
            page.media_box,
            page.crop_box,
            page.user_unit,
            page.rotate);
        Formatter<StringView>::format(builder, str);
    }
};

template<>
struct Formatter<PDF::Destination> : Formatter<StringView> {
    void format(FormatBuilder& builder, const PDF::Destination& destination)
    {
        String type_str;
        switch (destination.type) {
        case PDF::Destination::Type::XYZ:
            type_str = "XYZ";
            break;
        case PDF::Destination::Type::Fit:
            type_str = "Fit";
            break;
        case PDF::Destination::Type::FitH:
            type_str = "FitH";
            break;
        case PDF::Destination::Type::FitV:
            type_str = "FitV";
            break;
        case PDF::Destination::Type::FitR:
            type_str = "FitR";
            break;
        case PDF::Destination::Type::FitB:
            type_str = "FitB";
            break;
        case PDF::Destination::Type::FitBH:
            type_str = "FitBH";
            break;
        case PDF::Destination::Type::FitBV:
            type_str = "FitBV";
            break;
        }

        StringBuilder param_builder;
        for (auto& param : destination.parameters)
            param_builder.appendff("{} ", param);

        auto str = String::formatted("{{ type={} page={} params={} }}", type_str, destination.page, param_builder.to_string());
        Formatter<StringView>::format(builder, str);
    }
};

template<>
struct Formatter<PDF::OutlineItem> : Formatter<StringView> {
    void format(FormatBuilder& builder, const PDF::OutlineItem& item)
    {
        Formatter<StringView>::format(builder, item.to_string(0));
    }
};

template<>
struct Formatter<PDF::OutlineDict> : Formatter<StringView> {
    void format(FormatBuilder& builder, const PDF::OutlineDict& dict)
    {
        StringBuilder child_builder;
        child_builder.append('[');
        for (auto& child : dict.children)
            child_builder.appendff("{}\n", child.to_string(2));
        child_builder.append("  ]");

        Formatter<StringView>::format(builder,
            String::formatted("OutlineDict {{\n  count={}\n  children={}\n}}", dict.count, child_builder.to_string()));
    }
};

}
