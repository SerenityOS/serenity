/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/TreeViewModel.h>

namespace GUI {

ModelIndex TreeViewModel::index(int row, int column, ModelIndex const& parent) const
{
    if (!parent.is_valid()) {
        if (static_cast<size_t>(row) >= m_nodes.size())
            return {};
        return create_index(row, column, m_nodes[row].ptr());
    }
    auto const& parent_node = *static_cast<Node const*>(parent.internal_data());
    if (static_cast<size_t>(row) >= parent_node.child_nodes().size())
        return {};
    auto const* child = parent_node.child_nodes()[row].ptr();
    return create_index(row, column, child);
}

ModelIndex TreeViewModel::parent_index(ModelIndex const& index) const
{
    if (!index.is_valid())
        return {};
    auto const& child_node = *static_cast<Node const*>(index.internal_data());
    auto const* parent_node = child_node.parent_node();
    if (parent_node == nullptr)
        return {};
    if (parent_node->parent_node() == nullptr) {
        for (size_t row = 0; row < m_nodes.size(); row++)
            if (m_nodes[row].ptr() == parent_node)
                return create_index(static_cast<int>(row), 0, parent_node);
        VERIFY_NOT_REACHED();
    }
    for (size_t row = 0; row < parent_node->parent_node()->child_nodes().size(); row++) {
        auto const* child_node_at_row = parent_node->parent_node()->child_nodes()[row].ptr();
        if (child_node_at_row == parent_node)
            return create_index(static_cast<int>(row), 0, parent_node);
    }
    VERIFY_NOT_REACHED();
}

int TreeViewModel::row_count(ModelIndex const& index) const
{
    if (!index.is_valid())
        return static_cast<int>(m_nodes.size());
    auto const& node = *static_cast<Node const*>(index.internal_data());
    return static_cast<int>(node.child_nodes().size());
}

Variant TreeViewModel::data(ModelIndex const& index, ModelRole role) const
{
    auto const& node = *static_cast<Node const*>(index.internal_data());
    switch (role) {
    case ModelRole::Display:
        return node.text();
    case ModelRole::Icon:
        if (node.icon().has_value())
            return *node.icon();
        return {};
    default:
        return {};
    }
}

Optional<ModelIndex> TreeViewModel::index_for_node(Node const& node, ModelIndex const& parent) const
{
    for (int row = 0; row < row_count(parent); ++row) {
        auto row_index = this->index(row, 0);
        auto const* row_node = static_cast<TreeViewModel::Node const*>(row_index.internal_data());
        if (&node == row_node)
            return row_index;
        if (auto index = index_for_node(node, row_index); index.has_value())
            return index;
    }
    return {};
}

}
