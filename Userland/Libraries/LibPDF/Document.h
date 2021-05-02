/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/HashMap.h>
#include <AK/RefCounted.h>
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
};

class Document final : public RefCounted<Document> {
public:
    static NonnullRefPtr<Document> from(const ReadonlyBytes& bytes);

    ALWAYS_INLINE const XRefTable& xref_table() const { return m_xref_table; }

    ALWAYS_INLINE const DictObject& trailer() const { return *m_trailer; }

    RefPtr<Object> get_object(u32 index) const
    {
        if (!m_objects.contains(index))
            return {};
        return m_objects.get(index).value();
    }

    [[nodiscard]] NonnullRefPtr<Object> get_or_load_object(u32 index);

    [[nodiscard]] u32 get_first_page_index() const;

    [[nodiscard]] u32 get_page_count() const;

    [[nodiscard]] Page get_page(u32 index);

    ALWAYS_INLINE void set_object(u32 index, const NonnullRefPtr<Object>& object)
    {
        m_objects.ensure_capacity(index);
        m_objects.set(index, object);
    }

private:
    explicit Document(Parser&& parser);

    // FIXME: Currently, to improve performance, we don't load any pages at Document
    // construction, rather we just load the page structure and populate
    // m_page_object_indices. However, we can be even lazier and defer page tree node
    // parsing, as good PDF writers will layout the page tree in a balanced tree to
    // improve lookup time. This would reduce the initial overhead by not loading
    // every page tree node of, say, a 1000+ page PDF file.
    void build_page_tree();
    void add_page_tree_node_to_page_tree(NonnullRefPtr<DictObject> page_tree);

    Parser m_parser;
    XRefTable m_xref_table;
    RefPtr<DictObject> m_trailer;
    RefPtr<DictObject> m_catalog;
    Vector<u32> m_page_object_indices;
    HashMap<u32, Page> m_pages;
    HashMap<u32, NonnullRefPtr<Object>> m_objects;
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
        constexpr auto fmt_string = "Page {{\n  resources={}\n  contents={}\n  media_box={}\n  crop_box={}\n  user_unit={}\n}}";
        auto str = String::formatted(fmt_string,
            page.resources->to_string(1),
            page.contents->to_string(1),
            page.media_box,
            page.crop_box,
            page.user_unit);
        Formatter<StringView>::format(builder, str);
    }
};

}
