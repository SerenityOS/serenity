/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/Document.h>
#include <LibPDF/Parser.h>

namespace PDF {

NonnullRefPtr<Document> Document::from(const ReadonlyBytes& bytes)
{
    Parser parser({}, bytes);
    return adopt_ref(*new Document(move(parser)));
}

Document::Document(Parser&& parser)
    : m_parser(parser)
{
    VERIFY(m_parser.perform_validation());
    auto [xref_table, trailer] = m_parser.parse_last_xref_table_and_trailer();

    m_xref_table = xref_table;
    m_trailer = trailer;

    m_catalog = m_trailer->get_dict(this, "Root");
    build_page_tree();
}

NonnullRefPtr<Object> Document::get_or_load_object(u32 index)
{
    auto obj = get_object(index);
    if (obj)
        return obj.release_nonnull();

    VERIFY(m_xref_table.has_object(index));
    auto byte_offset = m_xref_table.byte_offset_for_object(index);
    auto indirect_object = m_parser.parse_indirect_obj_at_offset(byte_offset);
    VERIFY(indirect_object->index() == index);
    auto object = indirect_object->object();
    m_objects.set(index, object);
    return object;
}

u32 Document::get_first_page_index() const
{
    // TODO: A PDF can have a different default first page, which
    // should be fetched and returned here
    return 0;
}

u32 Document::get_page_count() const
{
    return m_page_object_indices.size();
}

Page Document::get_page(u32 index)
{
    VERIFY(index < m_page_object_indices.size());

    auto cached_page = m_pages.get(index);

    if (cached_page.has_value())
        return cached_page.value();

    auto page_object_index = m_page_object_indices[index];

    auto obj = get_or_load_object(page_object_index);
    auto raw_page_object = object_cast<DictObject>(obj);

    auto resources = raw_page_object->get_dict(this, "Resources");
    auto contents = raw_page_object->get_object("Contents");

    auto media_box_array = raw_page_object->get_array(this, "MediaBox");
    auto media_box = Rectangle {
        media_box_array->at(0).to_float(),
        media_box_array->at(1).to_float(),
        media_box_array->at(2).to_float(),
        media_box_array->at(3).to_float(),
    };

    auto crop_box = media_box;
    if (raw_page_object->contains("CropBox")) {
        auto crop_box_array = raw_page_object->get_array(this, "CropBox");
        crop_box = Rectangle {
            crop_box_array->at(0).to_float(),
            crop_box_array->at(1).to_float(),
            crop_box_array->at(2).to_float(),
            crop_box_array->at(3).to_float(),
        };
    }

    float user_unit = 1.0f;
    if (raw_page_object->contains("UserUnit"))
        user_unit = raw_page_object->get_value("UserUnit").to_float();

    int rotate = 0;
    if (raw_page_object->contains("Rotate")) {
        rotate = raw_page_object->get_value("Rotate").as_int();
        VERIFY(rotate % 90 == 0);
    }

    Page page { resources, contents, media_box, crop_box, user_unit, rotate };
    m_pages.set(index, page);
    return page;
}

void Document::build_page_tree()
{
    auto page_tree = m_catalog->get_dict(this, "Pages");
    add_page_tree_node_to_page_tree(page_tree);
}

void Document::add_page_tree_node_to_page_tree(NonnullRefPtr<DictObject> page_tree)
{
    auto kids_array = page_tree->get_array(this, "Kids");
    auto page_count = page_tree->get("Count").value().as_int();

    if (static_cast<size_t>(page_count) != kids_array->elements().size()) {
        // This page tree contains child page trees, so we recursively add
        // these pages to the overall page tree

        for (auto& value : *kids_array) {
            auto reference = object_cast<IndirectObjectRef>(value.as_object());
            auto byte_offset = m_xref_table.byte_offset_for_object(reference->index());
            auto maybe_page_tree_node = m_parser.conditionally_parse_page_tree_node_at_offset(byte_offset);
            if (maybe_page_tree_node) {
                add_page_tree_node_to_page_tree(maybe_page_tree_node.release_nonnull());
            } else {
                m_page_object_indices.append(reference->index());
            }
        }

        return;
    }

    // We know all of the kids are leaf nodes
    for (auto& value : *kids_array) {
        auto reference = object_cast<IndirectObjectRef>(value.as_object());
        m_page_object_indices.append(reference->index());
    }
}

}
