/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClassViewWidget.h"
#include "HackStudio.h"
#include "ProjectDeclarations.h"
#include <AK/StdLibExtras.h>
#include <LibGUI/BoxLayout.h>
#include <string.h>

namespace HackStudio {

ClassViewWidget::ClassViewWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    m_class_tree = add<GUI::TreeView>();

    m_class_tree->on_selection_change = [this] {
        auto const& index = m_class_tree->selection().first();
        if (!index.is_valid())
            return;

        auto* node = static_cast<ClassViewNode const*>(index.internal_data());
        if (!node->declaration)
            return;

        open_file(node->declaration->position.file, node->declaration->position.line, node->declaration->position.column);
    };
}

RefPtr<ClassViewModel> ClassViewModel::create()
{
    return adopt_ref(*new ClassViewModel());
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
    auto* node = static_cast<ClassViewNode const*>(index.internal_data());
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
    auto* child = static_cast<ClassViewNode const*>(index.internal_data());
    auto* parent = child->parent;
    if (parent == nullptr)
        return {};

    if (parent->parent == nullptr) {
        for (size_t row = 0; row < m_root_scope.size(); row++) {
            if (m_root_scope[row].ptr() == parent)
                return create_index(row, 0, parent);
        }
        VERIFY_NOT_REACHED();
    }
    for (size_t row = 0; row < parent->parent->children.size(); row++) {
        ClassViewNode* child_at_row = parent->parent->children[row].ptr();
        if (child_at_row == parent)
            return create_index(row, 0, parent);
    }
    VERIFY_NOT_REACHED();
}

GUI::ModelIndex ClassViewModel::index(int row, int column, const GUI::ModelIndex& parent_index) const
{
    if (!parent_index.is_valid())
        return create_index(row, column, m_root_scope[row].ptr());
    auto* parent = static_cast<ClassViewNode const*>(parent_index.internal_data());
    auto* child = parent->children[row].ptr();
    return create_index(row, column, child);
}

ClassViewModel::ClassViewModel()
{
    m_root_scope.clear();
    ProjectDeclarations::the().for_each_declared_symbol([this](auto& decl) {
        if (decl.type == CodeComprehension::DeclarationType::Class
            || decl.type == CodeComprehension::DeclarationType::Struct
            || decl.type == CodeComprehension::DeclarationType::Member
            || decl.type == CodeComprehension::DeclarationType::Namespace) {
            add_declaration(decl);
        }
    });
}

static ClassViewNode& add_child_node(Vector<NonnullOwnPtr<ClassViewNode>>& children, NonnullOwnPtr<ClassViewNode>&& node_ptr, ClassViewNode* parent, CodeComprehension::Declaration const* declaration)
{
    node_ptr->parent = parent;
    node_ptr->declaration = declaration;

    size_t inserted_index = 0;
    ClassViewNode& node = *node_ptr;
    // Insert into parent's children list, sorted lexicographically by name.
    children.insert_before_matching(
        move(node_ptr), [&node](auto& other_node) {
            return strncmp(node.name.characters_without_null_termination(), other_node->name.characters_without_null_termination(), min(node.name.length(), other_node->name.length())) < 0;
        },
        0, &inserted_index);

    return *children.at(inserted_index);
}

void ClassViewModel::add_declaration(CodeComprehension::Declaration const& decl)
{
    ClassViewNode* parent = nullptr;
    auto scope_parts = decl.scope.view().split_view("::"sv);

    if (!scope_parts.is_empty()) {
        // Traverse declarations tree to the parent of 'decl'
        for (auto& node : m_root_scope) {
            if (node->name == scope_parts.first())
                parent = node;
        }

        if (parent == nullptr) {
            m_root_scope.append(make<ClassViewNode>(scope_parts.first()));
            parent = m_root_scope.last();
        }

        for (size_t i = 1; i < scope_parts.size(); ++i) {
            auto& scope = scope_parts[i];
            ClassViewNode* next { nullptr };
            for (auto& child : parent->children) {
                if (child->name == scope) {
                    next = child;
                    break;
                }
            }

            if (next) {
                parent = next;
                continue;
            }

            parent = &add_child_node(parent->children, make<ClassViewNode>(scope), parent, nullptr);
        }
    }

    Vector<NonnullOwnPtr<ClassViewNode>>* children_of_parent = nullptr;
    if (parent) {
        children_of_parent = &parent->children;
    } else {
        children_of_parent = &m_root_scope;
    }

    bool already_exists = false;
    for (auto& child : *children_of_parent) {
        if (child->name == decl.name) {
            already_exists = true;
            if (!child->declaration) {
                child->declaration = &decl;
            }
            break;
        }
    }
    if (!already_exists) {
        add_child_node(*children_of_parent, make<ClassViewNode>(decl.name), parent, &decl);
    }
}

void ClassViewWidget::refresh()
{
    m_class_tree->set_model(ClassViewModel::create());
}

}
