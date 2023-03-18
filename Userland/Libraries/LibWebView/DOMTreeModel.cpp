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

namespace WebView {

DOMTreeModel::DOMTreeModel(JsonObject dom_tree, GUI::TreeView* tree_view)
    : m_tree_view(tree_view)
    , m_dom_tree(move(dom_tree))
{
    // FIXME: Get these from the outside somehow instead of hard-coding paths here.
#ifdef AK_OS_SERENITY
    m_document_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-html.png"sv).release_value_but_fixme_should_propagate_errors());
    m_element_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/inspector-object.png"sv).release_value_but_fixme_should_propagate_errors());
    m_text_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-unknown.png"sv).release_value_but_fixme_should_propagate_errors());
#endif

    map_dom_nodes_to_parent(nullptr, &m_dom_tree);
}

DOMTreeModel::~DOMTreeModel() = default;

GUI::ModelIndex DOMTreeModel::index(int row, int column, const GUI::ModelIndex& parent) const
{
    if (!parent.is_valid()) {
        return create_index(row, column, &m_dom_tree);
    }

    auto const& parent_node = *static_cast<JsonObject const*>(parent.internal_data());
    auto children = get_children(parent_node);
    if (!children.has_value())
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

    auto grandparent_children = get_children(*grandparent_node);
    if (!grandparent_children.has_value())
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
    auto children = get_children(node);
    return children.has_value() ? children->size() : 0;
}

int DOMTreeModel::column_count(const GUI::ModelIndex&) const
{
    return 1;
}

static DeprecatedString with_whitespace_collapsed(StringView string)
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
    return builder.to_deprecated_string();
}

GUI::Variant DOMTreeModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto const& node = *static_cast<JsonObject const*>(index.internal_data());
    auto node_name = node.get_deprecated_string("name"sv).value_or({});
    auto type = node.get_deprecated_string("type"sv).value_or("unknown");

    // FIXME: This FIXME can go away when we fix the one below.
#ifdef AK_OS_SERENITY
    if (role == GUI::ModelRole::ForegroundColor) {
        // FIXME: Allow models to return a foreground color *role*.
        //        Then we won't need to have a GUI::TreeView& member anymore.
        if (type == "comment"sv || type == "shadow-root"sv)
            return m_tree_view->palette().syntax_comment();
        if (type == "pseudo-element"sv)
            return m_tree_view->palette().syntax_type();
        if (!node.get_bool("visible"sv).value_or(true))
            return m_tree_view->palette().syntax_comment();
        return {};
    }
#endif

    // FIXME: This FIXME can go away when the icons are provided from the outside (see constructor).
#ifdef AK_OS_SERENITY
    if (role == GUI::ModelRole::Icon) {
        if (type == "document")
            return m_document_icon;
        if (type == "element")
            return m_element_icon;
        // FIXME: More node type icons?
        return m_text_icon;
    }
#endif

    if (role == GUI::ModelRole::Display) {
        if (type == "text")
            return with_whitespace_collapsed(node.get_deprecated_string("text"sv).value());
        if (type == "comment"sv)
            return DeprecatedString::formatted("<!--{}-->", node.get_deprecated_string("data"sv).value());
        if (type == "shadow-root"sv)
            return DeprecatedString::formatted("{} ({})", node_name, node.get_deprecated_string("mode"sv).value());
        if (type != "element")
            return node_name;

        StringBuilder builder;
        builder.append('<');
        builder.append(node_name.to_lowercase());
        if (node.has("attributes"sv)) {
            auto attributes = node.get_object("attributes"sv).value();
            attributes.for_each_member([&builder](auto& name, JsonValue const& value) {
                builder.append(' ');
                builder.append(name);
                builder.append('=');
                builder.append('"');
                builder.append(value.to_deprecated_string());
                builder.append('"');
            });
        }
        builder.append('>');
        return builder.to_deprecated_string();
    }
    return {};
}

void DOMTreeModel::map_dom_nodes_to_parent(JsonObject const* parent, JsonObject const* node)
{
    m_dom_node_to_parent_map.set(node, parent);
    m_node_id_to_dom_node_map.set(node->get_i32("id"sv).value_or(0), node);

    auto children = get_children(*node);
    if (!children.has_value())
        return;

    children->for_each([&](auto const& child) {
        auto const& child_node = child.as_object();
        map_dom_nodes_to_parent(node, &child_node);
    });
}

GUI::ModelIndex DOMTreeModel::index_for_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement> pseudo_element) const
{
    auto node = m_node_id_to_dom_node_map.get(node_id).value_or(nullptr);
    if (node) {
        if (pseudo_element.has_value()) {
            // Find pseudo-element child of the node.
            auto node_children = get_children(*node);
            for (size_t i = 0; i < node_children->size(); i++) {
                auto& child = node_children->at(i).as_object();
                if (!child.has("pseudo-element"sv))
                    continue;

                auto child_pseudo_element = child.get_i32("pseudo-element"sv);
                if (child_pseudo_element == to_underlying(pseudo_element.value()))
                    return create_index(i, 0, &child);
            }
        } else {
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
    }

    dbgln("Didn't find index for node {}, pseudo-element {}!", node_id, pseudo_element.has_value() ? Web::CSS::pseudo_element_name(pseudo_element.value()) : "NONE"sv);
    return {};
}

}
