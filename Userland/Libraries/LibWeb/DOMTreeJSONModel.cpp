/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2018-2020, Adam Hodgen <ant1441@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DOMTreeJSONModel.h"
#include <AK/JsonObject.h>
#include <AK/StringBuilder.h>
#include <ctype.h>

namespace Web {

DOMTreeJSONModel::DOMTreeJSONModel(JsonObject dom_tree)
    : m_dom_tree(move(dom_tree))
{
    m_document_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-html.png"));
    m_element_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/inspector-object.png"));
    m_text_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-unknown.png"));
}

DOMTreeJSONModel::~DOMTreeJSONModel()
{
}

GUI::ModelIndex DOMTreeJSONModel::index(int row, int column, const GUI::ModelIndex& parent) const
{
    if (!parent.is_valid()) {
        return create_index(row, column, (void*)get_internal_id(m_dom_tree));
    }

    auto const& parent_node = find_node(parent);
    auto const* children = get_children(parent_node);
    if (!children)
        return create_index(row, column, (void*)get_internal_id(m_dom_tree));

    auto const& child_node = children->at(row).as_object();
    auto child_internal_id = (void*)get_internal_id(child_node);
    return create_index(row, column, child_internal_id);
}

GUI::ModelIndex DOMTreeJSONModel::parent_index(const GUI::ModelIndex& index) const
{
    // FIXME: Handle the template element (child elements are not stored in it, all of its children are in its document fragment "content")
    //        Probably in the JSON generation in Node.cpp?
    if (!index.is_valid())
        return {};

    auto const& node = find_node(index);
    auto node_internal_id = get_internal_id(node);

    auto const* parent_node = find_parent_of_child_with_internal_id(node_internal_id);
    if (!parent_node)
        return {};

    // If the parent is the root document, we know it has index 0, 0
    auto parent_node_internal_id = get_internal_id(*parent_node);
    if (parent_node_internal_id == get_internal_id(m_dom_tree)) {
        return create_index(0, 0, (void*)parent_node_internal_id);
    }

    // Otherwise, we need to find the grandparent, to find the index of parent within that
    auto const* grandparent_node = find_parent_of_child_with_internal_id(parent_node_internal_id);
    VERIFY(grandparent_node);

    auto const* grandparent_children = get_children(*grandparent_node);
    if (!grandparent_children)
        return {};

    for (size_t grandparent_child_index = 0; grandparent_child_index < grandparent_children->size(); ++grandparent_child_index) {
        auto const& child = grandparent_children->at(grandparent_child_index).as_object();
        if (get_internal_id(child) == parent_node_internal_id)
            return create_index(grandparent_child_index, 0, (void*)(parent_node_internal_id));
    }

    return {};
}

int DOMTreeJSONModel::row_count(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return 1;

    auto const& node = find_node(index);
    auto const* children = get_children(node);
    return children ? children->size() : 0;
}

int DOMTreeJSONModel::column_count(const GUI::ModelIndex&) const
{
    return 1;
}

static String with_whitespace_collapsed(const StringView& string)
{
    StringBuilder builder;
    for (size_t i = 0; i < string.length(); ++i) {
        if (isspace(string[i])) {
            builder.append(' ');
            while (i < string.length()) {
                if (isspace(string[i])) {
                    ++i;
                    continue;
                }
                builder.append(string[i]);
                break;
            }
            continue;
        }
        builder.append(string[i]);
    }
    return builder.to_string();
}

GUI::Variant DOMTreeJSONModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto const& node = find_node(index);
    auto node_name = node.get("name").as_string();
    auto type = node.get("type").as_string_or("unknown");

    if (role == GUI::ModelRole::Icon) {
        if (type == "document")
            return m_document_icon;
        if (type == "element")
            return m_element_icon;
        // FIXME: More node type icons?
        return m_text_icon;
    }
    if (role == GUI::ModelRole::Display) {
        if (type == "text")
            return with_whitespace_collapsed(node.get("text").as_string());
        if (type != "element")
            return node_name;

        StringBuilder builder;
        builder.append('<');
        builder.append(node_name.to_lowercase());
        if (node.has("attributes")) {
            auto attributes = node.get("attributes").as_object();
            attributes.for_each_member([&builder](auto& name, JsonValue const& value) {
                builder.append(' ');
                builder.append(name);
                builder.append('=');
                builder.append('"');
                builder.append(value.to_string());
                builder.append('"');
            });
        }
        builder.append('>');
        return builder.to_string();
    }
    return {};
}

void DOMTreeJSONModel::update()
{
    did_update();
}

JsonObject const* DOMTreeJSONModel::find_parent_of_child_with_internal_id(size_t internal_id) const
{
    return find_parent_of_child_with_internal_id(m_dom_tree, internal_id);
}

JsonObject const* DOMTreeJSONModel::find_parent_of_child_with_internal_id(JsonObject const& node, size_t internal_id) const
{
    auto const* children = get_children(node);
    if (!children)
        return nullptr;

    for (size_t i = 0; i < children->size(); ++i) {
        auto const& child = children->at(i).as_object();

        auto child_internal_id = get_internal_id(child);
        if (child_internal_id == internal_id)
            return &node;

        if (auto const* maybe_node = find_parent_of_child_with_internal_id(child, internal_id); maybe_node)
            return maybe_node;
    }

    return nullptr;
}

JsonObject const* DOMTreeJSONModel::find_child_with_internal_id(size_t internal_id) const
{
    return find_child_with_internal_id(m_dom_tree, internal_id);
}

JsonObject const* DOMTreeJSONModel::find_child_with_internal_id(JsonObject const& node, size_t internal_id) const
{
    auto node_internal_id = get_internal_id(node);
    if (node_internal_id == internal_id) {
        return &node;
    }

    auto const* children = get_children(node);
    if (!children)
        return nullptr;

    for (size_t i = 0; i < children->size(); ++i) {
        auto const& child = children->at(i).as_object();

        if (auto const* maybe_node = find_child_with_internal_id(child, internal_id); maybe_node)
            return maybe_node;
    }

    return nullptr;
}

size_t DOMTreeJSONModel::get_internal_id(JsonObject const& o)
{
    return o.get("internal_id").as_u32();
}

JsonArray const* DOMTreeJSONModel::get_children(JsonObject const& o)
{
    if (auto const* maybe_children = o.get_ptr("children"); maybe_children)
        return &maybe_children->as_array();
    return nullptr;
}

JsonObject const& DOMTreeJSONModel::find_node(GUI::ModelIndex const& index) const
{
    auto internal_id = (size_t)(index.internal_data());

    if (auto const* maybe_node = find_child_with_internal_id(internal_id); maybe_node)
        return *maybe_node;

    dbgln("Failed to find node with internal_id={}", internal_id);
    VERIFY_NOT_REACHED();
}

}
