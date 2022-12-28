/*
 * Copyright (c) 2022, Jonah Shafran <jonahshafran@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/JsonObject.h>
#include <LibGUI/Model.h>
#include <LibWeb/CSS/Selector.h>

namespace WebView {

class AccessibilityTreeModel final : public GUI::Model {
public:
    static NonnullRefPtr<AccessibilityTreeModel> create(StringView accessibility_tree, GUI::TreeView& tree_view)
    {
        auto json_or_error = JsonValue::from_string(accessibility_tree).release_value_but_fixme_should_propagate_errors();
        return adopt_ref(*new AccessibilityTreeModel(json_or_error.as_object(), &tree_view));
    }

    static NonnullRefPtr<AccessibilityTreeModel> create(StringView accessibility_tree)
    {
        auto json_or_error = JsonValue::from_string(accessibility_tree).release_value_but_fixme_should_propagate_errors();
        return adopt_ref(*new AccessibilityTreeModel(json_or_error.as_object(), nullptr));
    }

    virtual ~AccessibilityTreeModel() override;

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override;
    virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole) const override;
    virtual GUI::ModelIndex index(int row, int column, GUI::ModelIndex const& parent = GUI::ModelIndex()) const override;
    virtual GUI::ModelIndex parent_index(GUI::ModelIndex const&) const override;

private:
    AccessibilityTreeModel(JsonObject, GUI::TreeView*);

    ALWAYS_INLINE JsonObject const* get_parent(JsonObject const& o) const
    {
        auto parent_node = m_accessibility_node_to_parent_map.get(&o);
        VERIFY(parent_node.has_value());
        return *parent_node;
    }

    ALWAYS_INLINE static JsonArray const* get_children(JsonObject const& o)
    {
        if (auto const* maybe_children = o.get_ptr("children"sv); maybe_children)
            return &maybe_children->as_array();
        return nullptr;
    }

    void map_accessibility_nodes_to_parent(JsonObject const* parent, JsonObject const* child);

    GUI::TreeView* m_tree_view { nullptr };
    JsonObject m_accessibility_tree;
    HashMap<JsonObject const*, JsonObject const*> m_accessibility_node_to_parent_map;
};

}
