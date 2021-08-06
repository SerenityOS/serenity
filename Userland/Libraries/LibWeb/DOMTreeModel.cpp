/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DOMTreeModel.h"
#include <AK/StringBuilder.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/Text.h>
#include <ctype.h>
#include <stdio.h>

namespace Web {

DOMTreeModel::DOMTreeModel(DOM::Document& document)
    : m_document(document)
{
    m_document_icon.set_bitmap_for_size(16, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-html.png"));
    m_element_icon.set_bitmap_for_size(16, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/inspector-object.png"));
    m_text_icon.set_bitmap_for_size(16, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-unknown.png"));
}

DOMTreeModel::~DOMTreeModel()
{
}

GUI::ModelIndex DOMTreeModel::index(int row, int column, const GUI::ModelIndex& parent) const
{
    if (!parent.is_valid()) {
        return create_index(row, column, m_document.ptr());
    }
    auto& parent_node = *static_cast<DOM::Node*>(parent.internal_data());
    return create_index(row, column, parent_node.child_at_index(row));
}

GUI::ModelIndex DOMTreeModel::parent_index(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto& node = *static_cast<DOM::Node*>(index.internal_data());
    if (!node.parent())
        return {};

    // FIXME: Handle the template element (child elements are not stored in it, all of its children are in its document fragment "content")

    // No grandparent? Parent is the document!
    if (!node.parent()->parent()) {
        return create_index(0, 0, m_document.ptr());
    }

    // Walk the grandparent's children to find the index of node's parent in its parent.
    // (This is needed to produce the row number of the GUI::ModelIndex corresponding to node's parent.)
    int grandparent_child_index = 0;
    for (auto* grandparent_child = node.parent()->parent()->first_child(); grandparent_child; grandparent_child = grandparent_child->next_sibling()) {
        if (grandparent_child == node.parent())
            return create_index(grandparent_child_index, 0, node.parent());
        ++grandparent_child_index;
    }

    VERIFY_NOT_REACHED();
    return {};
}

int DOMTreeModel::row_count(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return 1;
    auto& node = *static_cast<DOM::Node*>(index.internal_data());
    return node.child_count();
}

int DOMTreeModel::column_count(const GUI::ModelIndex&) const
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

GUI::Variant DOMTreeModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto& node = *static_cast<DOM::Node*>(index.internal_data());
    if (role == GUI::ModelRole::Icon) {
        if (node.is_document())
            return m_document_icon;
        if (node.is_element())
            return m_element_icon;
        // FIXME: More node type icons?
        return m_text_icon;
    }
    if (role == GUI::ModelRole::Display) {
        if (node.is_text())
            return with_whitespace_collapsed(verify_cast<DOM::Text>(node).data());
        if (!node.is_element())
            return node.node_name();
        auto& element = verify_cast<DOM::Element>(node);
        StringBuilder builder;
        builder.append('<');
        builder.append(element.local_name());
        element.for_each_attribute([&](auto& name, auto& value) {
            builder.append(' ');
            builder.append(name);
            builder.append('=');
            builder.append('"');
            builder.append(value);
            builder.append('"');
        });
        builder.append('>');
        return builder.to_string();
    }
    return {};
}

}
