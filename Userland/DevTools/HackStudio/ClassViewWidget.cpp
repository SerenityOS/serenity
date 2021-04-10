/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
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

#include "ClassViewWidget.h"
#include "HackStudio.h"
#include "ProjectDeclarations.h"
#include <LibGUI/BoxLayout.h>

namespace HackStudio {

ClassViewWidget::ClassViewWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    m_class_tree = add<GUI::TreeView>();

    m_class_tree->on_selection = [this](auto& index) {
        if (!index.is_valid())
            return;

        auto* node = static_cast<const ClassViewNode*>(index.internal_data());
        if (!node->declaration)
            return;

        open_file(node->declaration->position.file, node->declaration->position.line, node->declaration->position.column);
    };
}

RefPtr<ClassViewModel> ClassViewModel::create()
{
    return adopt(*new ClassViewModel());
}

int ClassViewModel::row_count(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return m_root_scope.size();
    auto* node = static_cast<ClassViewNode*>(index.internal_data());
    return node->children.size();
}

GUI::Variant ClassViewModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto* node = static_cast<const ClassViewNode*>(index.internal_data());
    switch (role) {
    case GUI::ModelRole::Display: {
        return node->name;
    }
    case GUI::ModelRole::Icon: {
        if (!node->declaration)
            return {};
        auto icon = ProjectDeclarations::get_icon_for(node->declaration->type);
        if (icon.has_value())
            return icon.value();
        return {};
    }
    default:
        return {};
    }
}

GUI::ModelIndex ClassViewModel::parent_index(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto* child = static_cast<const ClassViewNode*>(index.internal_data());
    auto* parent = child->parent;
    if (parent == nullptr)
        return {};

    if (parent->parent == nullptr) {
        for (size_t row = 0; row < m_root_scope.size(); row++) {
            if (m_root_scope.ptr_at(row).ptr() == parent)
                return create_index(row, 0, parent);
        }
        VERIFY_NOT_REACHED();
    }
    for (size_t row = 0; row < parent->parent->children.size(); row++) {
        ClassViewNode* child_at_row = parent->parent->children.ptr_at(row).ptr();
        if (child_at_row == parent)
            return create_index(row, 0, parent);
    }
    VERIFY_NOT_REACHED();
}

GUI::ModelIndex ClassViewModel::index(int row, int column, const GUI::ModelIndex& parent_index) const
{
    if (!parent_index.is_valid())
        return create_index(row, column, &m_root_scope[row]);
    auto* parent = static_cast<const ClassViewNode*>(parent_index.internal_data());
    auto* child = &parent->children[row];
    return create_index(row, column, child);
}

ClassViewModel::ClassViewModel()
{
    m_root_scope.clear();
    ProjectDeclarations::the().for_each_declared_symbol([this](auto& decl) {
        if (decl.type == GUI::AutocompleteProvider::DeclarationType::Class
            || decl.type == GUI::AutocompleteProvider::DeclarationType::Struct
            || decl.type == GUI::AutocompleteProvider::DeclarationType::Member
            || decl.type == GUI::AutocompleteProvider::DeclarationType::Namespace) {
            add_declaration(decl);
        }
    });
}

void ClassViewModel::add_declaration(const GUI::AutocompleteProvider::Declaration& decl)
{
    ClassViewNode* parent = nullptr;
    auto scope_parts = decl.scope.view().split_view("::");

    if (!scope_parts.is_empty()) {
        // Traverse declarations tree to the parent of 'decl'
        for (auto& node : m_root_scope) {
            if (node.name == scope_parts.first())
                parent = &node;
        }

        if (parent == nullptr) {
            m_root_scope.append(make<ClassViewNode>(scope_parts.first()));
            parent = &m_root_scope.last();
        }

        for (size_t i = 1; i < scope_parts.size(); ++i) {
            auto& scope = scope_parts[i];
            ClassViewNode* next { nullptr };
            for (auto& child : parent->children) {
                VERIFY(child.declaration);
                if (child.declaration->name == scope) {
                    next = &child;
                    break;
                }
            }

            if (next) {
                parent = next;
                continue;
            }

            parent->children.append(make<ClassViewNode>(scope));
            parent->children.last().parent = parent;
            parent = &parent->children.last();
        }
    }

    NonnullOwnPtrVector<ClassViewNode>* children_of_parent = nullptr;
    if (parent) {
        children_of_parent = &parent->children;
    } else {
        children_of_parent = &m_root_scope;
    }

    bool already_exists = false;
    for (auto& child : *children_of_parent) {
        if (child.name == decl.name) {
            already_exists = true;
            if (!child.declaration) {
                child.declaration = &decl;
            }
            break;
        }
    }
    if (!already_exists) {
        children_of_parent->append(make<ClassViewNode>(decl.name));
        children_of_parent->last().declaration = &decl;
        children_of_parent->last().parent = parent;
    }
}

void ClassViewWidget::refresh()
{
    m_class_tree->set_model(ClassViewModel::create());
}

}
