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
    auto page_object_index = m_page_object_indices[index];
    auto page_object = m_pages.get(page_object_index);
    if (page_object.has_value())
        return page_object.value();

    auto raw_page_object = object_cast<DictObject>(get_or_load_object(page_object_index));
    auto resources = raw_page_object->get_dict(this, "Resources");
    auto media_box_array = raw_page_object->get_array(this, "MediaBox");
    auto media_box = Rectangle {
        static_cast<float>(media_box_array->at(0).as_int()),
        static_cast<float>(media_box_array->at(1).as_int()),
        static_cast<float>(media_box_array->at(2).as_int()),
        static_cast<float>(media_box_array->at(3).as_int()),
    };
    auto contents = raw_page_object->get_object("Contents");

    Page page { resources, media_box, contents };
    m_pages.set(index, page);
    return page;
}

void Document::build_page_tree()
{
    auto page_tree = m_catalog->get_dict(this, "Pages");
    auto kids_array = page_tree->get_array(this, "Kids");

    auto page_count = page_tree->get("Count").value().as_int();
    if (static_cast<size_t>(page_count) != kids_array->elements().size()) {
        // FIXME: Support recursive PDF page tree structures
        VERIFY_NOT_REACHED();
    }

    for (auto& value : *kids_array) {
        auto reference = object_cast<IndirectObjectRef>(value.as_object());
        m_page_object_indices.append(reference->index());
    }
}

}
