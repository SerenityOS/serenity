/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "DOMTreeModel.h"
#include <AK/StringBuilder.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/DOM/Text.h>
#include <ctype.h>
#include <stdio.h>

DOMTreeModel::DOMTreeModel(Document& document)
    : m_document(document)
{
    m_document_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-html.png"));
    m_element_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/inspector-object.png"));
    m_text_icon.set_bitmap_for_size(16, Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-unknown.png"));
}

DOMTreeModel::~DOMTreeModel()
{
}

GUI::ModelIndex DOMTreeModel::index(int row, int column, const GUI::ModelIndex& parent) const
{
    if (!parent.is_valid()) {
        return create_index(row, column, m_document.ptr());
    }
    auto& parent_node = *static_cast<Node*>(parent.internal_data());
    return create_index(row, column, parent_node.child_at_index(row));
}

GUI::ModelIndex DOMTreeModel::parent_index(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto& node = *static_cast<Node*>(index.internal_data());
    if (!node.parent())
        return {};

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

    ASSERT_NOT_REACHED();
    return {};
}

int DOMTreeModel::row_count(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return 1;
    auto& node = *static_cast<Node*>(index.internal_data());
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

GUI::Variant DOMTreeModel::data(const GUI::ModelIndex& index, Role role) const
{
    auto& node = *static_cast<Node*>(index.internal_data());
    if (role == Role::Icon) {
        if (node.is_document())
            return m_document_icon;
        if (node.is_element())
            return m_element_icon;
        // FIXME: More node type icons?
        return m_text_icon;
    }
    if (role == Role::Display) {
        if (node.is_text())
            return String::format("%s", with_whitespace_collapsed(to<Text>(node).data()).characters());
        if (!node.is_element())
            return node.tag_name();
        auto& element = to<Element>(node);
        StringBuilder builder;
        builder.append('<');
        builder.append(element.tag_name());
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

void DOMTreeModel::update()
{
    did_update();
}
