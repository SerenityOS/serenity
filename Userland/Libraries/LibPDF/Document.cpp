/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/Document.h>
#include <LibPDF/Parser.h>

namespace PDF {

String OutlineItem::to_string(int indent) const
{
    auto indent_str = String::repeated("  ", indent + 1);

    StringBuilder child_builder;
    child_builder.append('[');
    for (auto& child : children)
        child_builder.appendff("{}\n", child.to_string(indent + 1));
    child_builder.appendff("{}]", indent_str);

    StringBuilder builder;
    builder.append("OutlineItem {{\n");
    builder.appendff("{}title={}\n", indent_str, title);
    builder.appendff("{}count={}\n", indent_str, count);
    builder.appendff("{}dest={}\n", indent_str, dest);
    builder.appendff("{}color={}\n", indent_str, color);
    builder.appendff("{}italic={}\n", indent_str, italic);
    builder.appendff("{}bold={}\n", indent_str, bold);
    builder.appendff("{}children={}\n", indent_str, child_builder.to_string());
    builder.appendff("{}}}", String::repeated("  ", indent));

    return builder.to_string();
}

Document::Document(const ReadonlyBytes& bytes)
    : m_parser(Parser({}, bytes))
{
    m_parser.set_document(this);

    VERIFY(m_parser.perform_validation());
    auto [xref_table, trailer] = m_parser.parse_last_xref_table_and_trailer();

    m_xref_table = xref_table;
    m_trailer = trailer;

    m_catalog = m_trailer->get_dict(this, "Root");
    build_page_tree();
    build_outline();
}

Value Document::get_or_load_value(u32 index)
{
    auto value = get_value(index);
    if (value)
        return value;

    VERIFY(m_xref_table.has_object(index));
    auto byte_offset = m_xref_table.byte_offset_for_object(index);
    auto indirect_value = m_parser.parse_indirect_value_at_offset(byte_offset);
    VERIFY(indirect_value->index() == index);
    value = indirect_value->value();
    m_values.set(index, value);
    return value;
}

u32 Document::get_first_page_index() const
{
    // FIXME: A PDF can have a different default first page, which
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
    auto raw_page_object = resolve_to<DictObject>(get_or_load_value(page_object_index));

    auto resources = raw_page_object->get_dict(this, "Resources");
    auto contents = raw_page_object->get_object(this, "Contents");

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

    Page page { move(resources), move(contents), media_box, crop_box, user_unit, rotate };
    m_pages.set(index, page);
    return page;
}

Value Document::resolve(const Value& value)
{
    if (value.is_ref()) {
        // FIXME: Surely indirect PDF objects can't contain another indirect PDF object,
        // right? Unsure from the spec, but if they can, these return values would have
        // to be wrapped with another resolve() call.
        return get_or_load_value(value.as_ref_index());
    }

    if (!value.is_object())
        return value;

    auto obj = value.as_object();

    if (obj->is_indirect_value())
        return static_cast<NonnullRefPtr<IndirectValue>>(obj)->value();

    return obj;
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
            auto reference_index = value.as_ref_index();
            auto byte_offset = m_xref_table.byte_offset_for_object(reference_index);
            auto maybe_page_tree_node = m_parser.conditionally_parse_page_tree_node_at_offset(byte_offset);
            if (maybe_page_tree_node) {
                add_page_tree_node_to_page_tree(maybe_page_tree_node.release_nonnull());
            } else {
                m_page_object_indices.append(reference_index);
            }
        }

        return;
    }

    // We know all of the kids are leaf nodes
    for (auto& value : *kids_array)
        m_page_object_indices.append(value.as_ref_index());
}

void Document::build_outline()
{
    if (!m_catalog->contains("Outlines"))
        return;

    auto outline_dict = m_catalog->get_dict(this, "Outlines");
    if (!outline_dict->contains("First"))
        return;

    VERIFY(outline_dict->contains("Last"));

    auto first_ref = outline_dict->get_value("First");
    auto last_ref = outline_dict->get_value("Last");

    auto children = build_outline_item_chain(first_ref, last_ref);

    m_outline = adopt_ref(*new OutlineDict());
    m_outline->children = move(children);

    if (outline_dict->contains("Count"))
        m_outline->count = outline_dict->get_value("Count").as_int();
}

NonnullRefPtr<OutlineItem> Document::build_outline_item(NonnullRefPtr<DictObject> outline_item_dict)
{
    auto outline_item = adopt_ref(*new OutlineItem {});

    if (outline_item_dict->contains("First")) {
        VERIFY(outline_item_dict->contains("Last"));
        auto first_ref = outline_item_dict->get_value("First");
        auto last_ref = outline_item_dict->get_value("Last");

        auto children = build_outline_item_chain(first_ref, last_ref);
        outline_item->children = move(children);
    }

    outline_item->title = outline_item_dict->get_string(this, "Title")->string();

    if (outline_item_dict->contains("Count"))
        outline_item->count = outline_item_dict->get_value("Count").as_int();

    if (outline_item_dict->contains("Dest")) {
        auto dest_arr = outline_item_dict->get_array(this, "Dest");
        auto page_ref = dest_arr->at(0);
        auto type_name = dest_arr->get_name_at(this, 1)->name();

        Vector<float> parameters;
        for (size_t i = 2; i < dest_arr->size(); i++)
            parameters.append(dest_arr->at(i).to_float());

        Destination::Type type;
        if (type_name == "XYZ") {
            type = Destination::Type::XYZ;
        } else if (type_name == "Fit") {
            type = Destination::Type::Fit;
        } else if (type_name == "FitH") {
            type = Destination::Type::FitH;
        } else if (type_name == "FitV") {
            type = Destination::Type::FitV;
        } else if (type_name == "FitR") {
            type = Destination::Type::FitR;
        } else if (type_name == "FitB") {
            type = Destination::Type::FitB;
        } else if (type_name == "FitBH") {
            type = Destination::Type::FitBH;
        } else if (type_name == "FitBV") {
            type = Destination::Type::FitBV;
        } else {
            VERIFY_NOT_REACHED();
        }

        outline_item->dest = Destination { type, page_ref, parameters };
    }

    if (outline_item_dict->contains("C")) {
        auto color_array = outline_item_dict->get_array(this, "C");
        auto r = static_cast<int>(255.0f * color_array->at(0).as_float());
        auto g = static_cast<int>(255.0f * color_array->at(1).as_float());
        auto b = static_cast<int>(255.0f * color_array->at(2).as_float());
        outline_item->color = Color(r, g, b);
    }

    if (outline_item_dict->contains("F")) {
        auto bitfield = outline_item_dict->get_value("F").as_int();
        outline_item->italic = bitfield & 0x1;
        outline_item->bold = bitfield & 0x2;
    }

    return outline_item;
}

NonnullRefPtrVector<OutlineItem> Document::build_outline_item_chain(const Value& first_ref, const Value& last_ref)
{
    VERIFY(first_ref.is_ref());
    VERIFY(last_ref.is_ref());

    NonnullRefPtrVector<OutlineItem> children;

    auto first_dict = object_cast<DictObject>(get_or_load_value(first_ref.as_ref_index()).as_object());
    auto first = build_outline_item(first_dict);
    children.append(first);

    auto current_child_dict = first_dict;
    u32 current_child_index = first_ref.as_ref_index();

    while (current_child_dict->contains("Next")) {
        auto next_child_dict_ref = current_child_dict->get_value("Next");
        current_child_index = next_child_dict_ref.as_ref_index();
        auto next_child_dict = object_cast<DictObject>(get_or_load_value(current_child_index).as_object());
        auto next_child = build_outline_item(next_child_dict);
        children.append(next_child);

        current_child_dict = next_child_dict;
    }

    VERIFY(last_ref.as_ref_index() == current_child_index);

    return children;
}

}
