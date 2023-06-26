/*
 * Copyright (c) 2022, Jonah Shafran <jonahshafran@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWebView/AccessibilityTreeModel.h>

namespace WebView {

AccessibilityTreeModel::AccessibilityTreeModel(JsonObject accessibility_tree, GUI::TreeView* tree_view)
    : m_tree_view(tree_view)
    , m_accessibility_tree(move(accessibility_tree))
{
    map_accessibility_nodes_to_parent(nullptr, &m_accessibility_tree);
}

AccessibilityTreeModel::~AccessibilityTreeModel() = default;

GUI::ModelIndex AccessibilityTreeModel::index(int row, int column, GUI::ModelIndex const& parent) const
{
    if (!parent.is_valid()) {
        return create_index(row, column, &m_accessibility_tree);
    }

    auto const& parent_node = *static_cast<JsonObject const*>(parent.internal_data());
    auto children = get_children(parent_node);
    if (!children.has_value())
        return create_index(row, column, &m_accessibility_tree);

    auto const& child_node = children->at(row).as_object();
    return create_index(row, column, &child_node);
}

GUI::ModelIndex AccessibilityTreeModel::parent_index(GUI::ModelIndex const& index) const
{
    if (!index.is_valid())
        return {};

    auto const& node = *static_cast<JsonObject const*>(index.internal_data());

    auto const* parent_node = get_parent(node);
    if (!parent_node)
        return {};

    // If the parent is the root document, we know it has index 0, 0
    if (parent_node == &m_accessibility_tree) {
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

int AccessibilityTreeModel::row_count(GUI::ModelIndex const& index) const
{
    if (!index.is_valid())
        return 1;

    auto const& node = *static_cast<JsonObject const*>(index.internal_data());
    auto children = get_children(node);
    return children.has_value() ? children->size() : 0;
}

int AccessibilityTreeModel::column_count(const GUI::ModelIndex&) const
{
    return 1;
}

GUI::Variant AccessibilityTreeModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    auto const& node = *static_cast<JsonObject const*>(index.internal_data());
    auto type = node.get_deprecated_string("type"sv).value_or("unknown"sv);

    if (role == GUI::ModelRole::Display) {
        if (type == "text")
            return node.get_deprecated_string("text"sv).value();

        auto node_role = node.get_deprecated_string("role"sv).value();
        if (type != "element")
            return node_role;

        auto name = node.get_deprecated_string("name"sv).value_or("");
        auto description = node.get_deprecated_string("description"sv).value_or("");

        StringBuilder builder;
        builder.append(node_role.to_lowercase());
        builder.appendff(" name: \"{}\", description: \"{}\"", name, description);
        return builder.to_deprecated_string();
    }
    return {};
}

void AccessibilityTreeModel::map_accessibility_nodes_to_parent(JsonObject const* parent, JsonObject const* node)
{
    m_accessibility_node_to_parent_map.set(node, parent);

    auto children = get_children(*node);
    if (!children.has_value())
        return;

    children->for_each([&](auto const& child) {
        auto const& child_node = child.as_object();
        map_accessibility_nodes_to_parent(node, &child_node);
    });
}

}
