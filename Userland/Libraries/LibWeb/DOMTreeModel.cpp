/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2018-2020, Adam Hodgen <ant1441@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DOMTreeModel.h"
#include <AK/JsonObject.h>
#include <AK/StringBuilder.h>
#include <LibGUI/TreeView.h>
#include <LibGfx/Palette.h>
#include <ctype.h>

namespace Web {

DOMTreeModel::DOMTreeModel(JsonObject dom_tree, GUI::TreeView& tree_view)
    : m_tree_view(tree_view)
    , m_dom_tree(move(dom_tree))
{
    m_document_icon.set_bitmap_for_size(16, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-html.png").release_value_but_fixme_should_propagate_errors());
    m_element_icon.set_bitmap_for_size(16, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/inspector-object.png").release_value_but_fixme_should_propagate_errors());
    m_text_icon.set_bitmap_for_size(16, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-unknown.png").release_value_but_fixme_should_propagate_errors());

    map_dom_nodes_to_parent(nullptr, &m_dom_tree);
}

DOMTreeModel::~DOMTreeModel()
{
}

GUI::ModelIndex DOMTreeModel::index(int row, int column, const GUI::ModelIndex& parent) const
{
    if (!parent.is_valid()) {
        return create_index(row, column, &m_dom_tree);
    }

    auto const& parent_node = *static_cast<JsonObject const*>(parent.internal_data());
    auto const* children = get_children(parent_node);
    if (!children)
        return create_index(row, column, &m_dom_tree);

    auto const& child_node = children->at(row).as_object();
    return create_index(row, column, &child_node);
}

GUI::ModelIndex DOMTreeModel::parent_index(const GUI::ModelIndex& index) const
{
    // FIXME: Handle the template element (child elements are not stored in it, all of its children are in its document fragment "content")
    //        Probably in the JSON generation in Node.cpp?
    if (!index.is_valid())
        return {};

    auto const& node = *static_cast<JsonObject const*>(index.internal_data());

    auto const* parent_node = get_parent(node);
    if (!parent_node)
        return {};

    // If the parent is the root document, we know it has index 0, 0
    if (parent_node == &m_dom_tree) {
        return create_index(0, 0, parent_node);
    }

    // Otherwise, we need to find the grandparent, to find the index of parent within that
    auto const* grandparent_node = get_parent(*parent_node);
    VERIFY(grandparent_node);

    auto const* grandparent_children = get_children(*grandparent_node);
    if (!grandparent_children)
        return {};

    for (size_t grandparent_child_index = 0; grandparent_child_index < grandparent_children->size(); ++grandparent_child_index) {
        auto const& child = grandparent_children->at(grandparent_child_index).as_object();
        if (&child == parent_node)
            return create_index(grandparent_child_index, 0, parent_node);
    }

    return {};
}

int DOMTreeModel::row_count(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return 1;

    auto const& node = *static_cast<JsonObject const*>(index.internal_data());
    auto const* children = get_children(node);
    return children ? children->size() : 0;
}

int DOMTreeModel::column_count(const GUI::ModelIndex&) const
{
    return 1;
}

static String with_whitespace_collapsed(StringView string)
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

GUI::Variant DOMTreeModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto const& node = *static_cast<JsonObject const*>(index.internal_data());
    auto node_name = node.get("name").as_string();
    auto type = node.get("type").as_string_or("unknown");

    if (role == GUI::ModelRole::ForegroundColor) {
        // FIXME: Allow models to return a foreground color *role*.
        //        Then we won't need to have a GUI::TreeView& member anymore.
        if (type == "comment"sv)
            return m_tree_view.palette().syntax_comment();
        return {};
    }

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
        if (type == "comment"sv)
            return String::formatted("<!--{}-->", node.get("data"sv).as_string());
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

void DOMTreeModel::map_dom_nodes_to_parent(JsonObject const* parent, JsonObject const* node)
{
    m_dom_node_to_parent_map.set(node, parent);
    m_node_id_to_dom_node_map.set(node->get("id").to_i32(), node);

    auto const* children = get_children(*node);
    if (!children)
        return;

    children->for_each([&](auto const& child) {
        auto const& child_node = child.as_object();
        map_dom_nodes_to_parent(node, &child_node);
    });
}

GUI::ModelIndex DOMTreeModel::index_for_node(i32 node_id) const
{
    auto node = m_node_id_to_dom_node_map.get(node_id).value_or(nullptr);
    if (node) {
        auto* parent = get_parent(*node);
        if (!parent)
            return {};
        auto parent_children = get_children(*parent);
        for (size_t i = 0; i < parent_children->size(); i++) {
            if (&parent_children->at(i).as_object() == node) {
                return create_index(i, 0, node);
            }
        }
    }

    dbgln("Didn't find index for node {}!", node_id);
    return {};
}

}
