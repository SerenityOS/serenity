/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
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

#include "ManualModel.h"
#include "ManualNode.h"
#include "ManualPageNode.h"
#include "ManualSectionNode.h"
#include <LibDraw/PNGLoader.h>

static ManualSectionNode s_sections[] = {
    { "1", "Command-line programs" },
    { "2", "System calls" },
    { "3", "Libraries" },
    { "4", "Special files" },
    { "5", "File formats" },
    { "6", "Games" },
    { "7", "Miscellanea" },
    { "8", "Sysadmin tools" }
};

ManualModel::ManualModel()
{
    // FIXME: need some help from the icon fairy ^)
    m_section_icon.set_bitmap_for_size(16, load_png("/res/icons/16x16/book.png"));
    m_page_icon.set_bitmap_for_size(16, load_png("/res/icons/16x16/filetype-unknown.png"));
}

String ManualModel::page_path(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto* node = static_cast<const ManualNode*>(index.internal_data());
    if (!node->is_page())
        return {};
    auto* page = static_cast<const ManualPageNode*>(node);
    return page->path();
}

String ManualModel::page_and_section(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto* node = static_cast<const ManualNode*>(index.internal_data());
    if (!node->is_page())
        return {};
    auto* page = static_cast<const ManualPageNode*>(node);
    auto* section = static_cast<const ManualSectionNode*>(page->parent());
    return String::format("%s(%s)", page->name().characters(), section->section_name().characters());
}

GUI::ModelIndex ManualModel::index(int row, int column, const GUI::ModelIndex& parent_index) const
{
    if (!parent_index.is_valid())
        return create_index(row, column, &s_sections[row]);
    auto* parent = static_cast<const ManualNode*>(parent_index.internal_data());
    auto* child = &parent->children()[row];
    return create_index(row, column, child);
}

GUI::ModelIndex ManualModel::parent_index(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto* child = static_cast<const ManualNode*>(index.internal_data());
    auto* parent = child->parent();
    if (parent == nullptr)
        return {};

    if (parent->parent() == nullptr) {
        for (size_t row = 0; row < sizeof(s_sections) / sizeof(s_sections[0]); row++)
            if (&s_sections[row] == parent)
                return create_index(row, 0, parent);
        ASSERT_NOT_REACHED();
    }
    for (int row = 0; row < parent->parent()->children().size(); row++) {
        ManualNode* child_at_row = &parent->parent()->children()[row];
        if (child_at_row == parent)
            return create_index(row, 0, parent);
    }
    ASSERT_NOT_REACHED();
}

int ManualModel::row_count(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return sizeof(s_sections) / sizeof(s_sections[0]);
    auto* node = static_cast<const ManualNode*>(index.internal_data());
    return node->children().size();
}

int ManualModel::column_count(const GUI::ModelIndex&) const
{
    return 1;
}

GUI::Variant ManualModel::data(const GUI::ModelIndex& index, Role role) const
{
    auto* node = static_cast<const ManualNode*>(index.internal_data());
    switch (role) {
    case Role::Display:
        return node->name();
    case Role::Icon:
        if (node->is_page())
            return m_page_icon;
        return m_section_icon;
    default:
        return {};
    }
}

void ManualModel::update()
{
    did_update();
}
