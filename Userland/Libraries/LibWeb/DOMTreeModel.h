/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2018-2020, Adam Hodgen <ant1441@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/JsonObject.h>
#include <LibGUI/Model.h>
#include <LibWeb/Forward.h>

namespace Web {

class DOMTreeModel final : public GUI::Model {
public:
    static NonnullRefPtr<DOMTreeModel> create(StringView dom_tree, GUI::TreeView& tree_view)
    {
        auto json_or_error = JsonValue::from_string(dom_tree).release_value_but_fixme_should_propagate_errors();
        return adopt_ref(*new DOMTreeModel(json_or_error.as_object(), tree_view));
    }

    virtual ~DOMTreeModel() override;

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;
    virtual GUI::ModelIndex index(int row, int column, const GUI::ModelIndex& parent = GUI::ModelIndex()) const override;
    virtual GUI::ModelIndex parent_index(const GUI::ModelIndex&) const override;

    GUI::ModelIndex index_for_node(i32 node_id) const;

private:
    DOMTreeModel(JsonObject, GUI::TreeView&);

    ALWAYS_INLINE JsonObject const* get_parent(const JsonObject& o) const
    {
        auto parent_node = m_dom_node_to_parent_map.get(&o);
        VERIFY(parent_node.has_value());
        return *parent_node;
    }

    ALWAYS_INLINE static JsonArray const* get_children(const JsonObject& o)
    {
        if (auto const* maybe_children = o.get_ptr("children"); maybe_children)
            return &maybe_children->as_array();
        return nullptr;
    }

    void map_dom_nodes_to_parent(JsonObject const* parent, JsonObject const* child);

    GUI::TreeView& m_tree_view;
    GUI::Icon m_document_icon;
    GUI::Icon m_element_icon;
    GUI::Icon m_text_icon;
    JsonObject m_dom_tree;
    HashMap<JsonObject const*, JsonObject const*> m_dom_node_to_parent_map;
    HashMap<i32, JsonObject const*> m_node_id_to_dom_node_map;
};

}
