/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2018-2020, Adam Hodgen <ant1441@gmail.com>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/JsonValue.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWebView/ModelIndex.h>

namespace WebView {

class TreeModel {
public:
    enum class Type {
        AccessibilityTree,
        DOMTree,
    };

    TreeModel(Type, JsonValue tree);
    ~TreeModel();

    int row_count(ModelIndex const& parent) const;
    int column_count(ModelIndex const& parent) const;
    ModelIndex index(int row, int column, ModelIndex const& parent) const;
    ModelIndex parent(ModelIndex const& index) const;
    String text_for_display(ModelIndex const& index) const;

    ModelIndex index_for_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement> const& pseudo_element) const;

private:
    ALWAYS_INLINE JsonObject const* get_parent(JsonObject const& node) const
    {
        auto parent_node = m_node_to_parent_map.get(&node);
        VERIFY(parent_node.has_value());
        return *parent_node;
    }

    void prepare_node_maps(JsonObject const* node, JsonObject const* parent_node);

    Type m_type;
    JsonValue m_tree;

    HashMap<JsonObject const*, JsonObject const*> m_node_to_parent_map;
    HashMap<i32, JsonObject const*> m_node_id_to_node_map;
};

}
