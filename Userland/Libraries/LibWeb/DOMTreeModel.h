/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2018-2020, Adam Hodgen <ant1441@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/HashMap.h>
#include <YAK/JsonObject.h>
#include <LibGUI/Model.h>
#include <LibWeb/Forward.h>

namespace Web {

class DOMTreeModel final : public GUI::Model {
public:
    static NonnullRefPtr<DOMTreeModel> create(StringView dom_tree)
    {
        auto json_or_error = JsonValue::from_string(dom_tree);
        if (!json_or_error.has_value())
            VERIFY_NOT_REACHED();

        return adopt_ref(*new DOMTreeModel(json_or_error.value().as_object()));
    }

    virtual ~DOMTreeModel() override;

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;
    virtual GUI::ModelIndex index(int row, int column, const GUI::ModelIndex& parent = GUI::ModelIndex()) const override;
    virtual GUI::ModelIndex parent_index(const GUI::ModelIndex&) const override;

    GUI::ModelIndex index_for_node(i32 node_id) const;

private:
    explicit DOMTreeModel(JsonObject);

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

    GUI::Icon m_document_icon;
    GUI::Icon m_element_icon;
    GUI::Icon m_text_icon;
    JsonObject m_dom_tree;
    HashMap<JsonObject const*, JsonObject const*> m_dom_node_to_parent_map;
    HashMap<i32, JsonObject const*> m_node_id_to_dom_node_map;
};

}
