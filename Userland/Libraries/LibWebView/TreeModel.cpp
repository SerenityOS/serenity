/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2018-2020, Adam Hodgen <ant1441@gmail.com>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <AK/StringBuilder.h>
#include <LibWeb/Infra/Strings.h>
#include <LibWebView/TreeModel.h>

namespace WebView {

TreeModel::TreeModel(Type type, JsonValue tree)
    : m_type(type)
    , m_tree(move(tree))
{
    prepare_node_maps(&m_tree.as_object(), nullptr);
}

TreeModel::~TreeModel() = default;

void TreeModel::prepare_node_maps(JsonObject const* node, JsonObject const* parent_node)
{
    m_node_to_parent_map.set(node, parent_node);

    if (auto id = node->get_integer<i32>("id"sv); id.has_value()) {
        m_node_id_to_node_map.set(*id, node);
    }

    if (auto children = node->get_array("children"sv); children.has_value()) {
        children->for_each([&](auto const& child) {
            prepare_node_maps(&child.as_object(), node);
        });
    }
}

int TreeModel::row_count(ModelIndex const& parent) const
{
    if (!parent.is_valid())
        return 1;

    auto const& node = *static_cast<JsonObject const*>(parent.internal_data);
    auto children = node.get_array("children"sv);

    return children.has_value() ? static_cast<int>(children->size()) : 0;
}

int TreeModel::column_count(ModelIndex const&) const
{
    return 1;
}

ModelIndex TreeModel::index(int row, int column, ModelIndex const& parent) const
{
    if (!parent.is_valid())
        return { row, column, &m_tree.as_object() };

    auto const& parent_node = *static_cast<JsonObject const*>(parent.internal_data);

    auto children = parent_node.get_array("children"sv);
    if (!children.has_value())
        return { row, column, &m_tree.as_object() };

    auto const& child_node = children->at(row).as_object();
    return { row, column, &child_node };
}

ModelIndex TreeModel::parent(ModelIndex const& index) const
{
    // FIXME: Handle the template element (child elements are not stored in it, all of its children are in its document fragment "content")
    //        Probably in the JSON generation in Node.cpp?
    if (!index.is_valid())
        return {};

    auto const& node = *static_cast<JsonObject const*>(index.internal_data);

    auto const* parent_node = get_parent(node);
    if (!parent_node)
        return {};

    // If the parent is the root document, we know it has index 0, 0
    if (parent_node == &m_tree.as_object())
        return { 0, 0, parent_node };

    // Otherwise, we need to find the grandparent, to find the index of parent within that
    auto const* grandparent_node = get_parent(*parent_node);
    VERIFY(grandparent_node);

    auto grandparent_children = grandparent_node->get_array("children"sv);
    if (!grandparent_children.has_value())
        return {};

    for (size_t grandparent_child_index = 0; grandparent_child_index < grandparent_children->size(); ++grandparent_child_index) {
        auto const& child = grandparent_children->at(grandparent_child_index).as_object();

        if (&child == parent_node)
            return { static_cast<int>(grandparent_child_index), 0, parent_node };
    }

    return {};
}

static String accessibility_tree_text_for_display(JsonObject const& node, StringView type)
{
    auto role = node.get_deprecated_string("role"sv).value_or({});

    if (type == "text")
        return MUST(Web::Infra::strip_and_collapse_whitespace(node.get_deprecated_string("text"sv).value()));
    if (type != "element")
        return MUST(String::from_deprecated_string(role));

    auto name = node.get_deprecated_string("name"sv).value_or({});
    auto description = node.get_deprecated_string("description"sv).value_or({});

    StringBuilder builder;
    builder.append(role.to_lowercase());
    builder.appendff(" name: \"{}\", description: \"{}\"", name, description);

    return MUST(builder.to_string());
}

static String dom_tree_text_for_display(JsonObject const& node, StringView type)
{
    auto name = node.get_deprecated_string("name"sv).value_or({});

    if (type == "text"sv)
        return MUST(Web::Infra::strip_and_collapse_whitespace(node.get_deprecated_string("text"sv).value()));
    if (type == "comment"sv)
        return MUST(String::formatted("<!--{}-->", node.get_deprecated_string("data"sv).value()));
    if (type == "shadow-root"sv)
        return MUST(String::formatted("{} ({})", name, node.get_deprecated_string("mode"sv).value()));
    if (type != "element"sv)
        return MUST(String::from_deprecated_string(name));

    StringBuilder builder;

    builder.append('<');
    builder.append(name.to_lowercase());
    if (node.has("attributes"sv)) {
        auto attributes = node.get_object("attributes"sv).value();
        attributes.for_each_member([&builder](auto const& name, JsonValue const& value) {
            builder.append(' ');
            builder.append(name);
            builder.append('=');
            builder.append('"');
            builder.append(value.as_string());
            builder.append('"');
        });
    }
    builder.append('>');

    return MUST(builder.to_string());
}

String TreeModel::text_for_display(ModelIndex const& index) const
{
    auto const& node = *static_cast<JsonObject const*>(index.internal_data);
    auto type = node.get_deprecated_string("type"sv).value_or("unknown"sv);

    switch (m_type) {
    case Type::AccessibilityTree:
        return accessibility_tree_text_for_display(node, type);
    case Type::DOMTree:
        return dom_tree_text_for_display(node, type);
    }

    VERIFY_NOT_REACHED();
}

ModelIndex TreeModel::index_for_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement> const& pseudo_element) const
{
    if (auto const* node = m_node_id_to_node_map.get(node_id).value_or(nullptr)) {
        if (pseudo_element.has_value()) {
            // Find pseudo-element child of the node.
            auto node_children = node->get_array("children"sv);
            for (size_t i = 0; i < node_children->size(); i++) {
                auto const& child = node_children->at(i).as_object();
                if (!child.has("pseudo-element"sv))
                    continue;

                auto child_pseudo_element = child.get_i32("pseudo-element"sv);
                if (child_pseudo_element == to_underlying(pseudo_element.value()))
                    return { static_cast<int>(i), 0, &child };
            }
        } else {
            auto const* parent = get_parent(*node);
            if (!parent)
                return {};

            auto parent_children = parent->get_array("children"sv);
            for (size_t i = 0; i < parent_children->size(); i++) {
                if (&parent_children->at(i).as_object() == node)
                    return { static_cast<int>(i), 0, node };
            }
        }
    }

    dbgln("Didn't find index for node {}, pseudo-element {}!", node_id, pseudo_element.has_value() ? Web::CSS::pseudo_element_name(pseudo_element.value()) : "NONE"sv);
    return {};
}

}
