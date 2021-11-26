/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ManualModel.h"
#include "ManualNode.h"
#include "ManualPageNode.h"
#include "ManualSectionNode.h"
#include <AK/ByteBuffer.h>
#include <AK/Try.h>
#include <LibCore/File.h>
#include <LibGUI/FilteringProxyModel.h>

static ManualSectionNode s_sections[] = {
    { "1", "User programs" },
    { "2", "System calls" },
    { "3", "Library functions" },
    { "4", "Special files" },
    { "5", "File formats" },
    { "6", "Games" },
    { "7", "Miscellanea" },
    { "8", "Sysadmin tools" }
};

ManualModel::ManualModel()
{
    m_section_open_icon.set_bitmap_for_size(16, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/book-open.png").release_value_but_fixme_should_propagate_errors());
    m_section_icon.set_bitmap_for_size(16, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/book.png").release_value_but_fixme_should_propagate_errors());
    m_page_icon.set_bitmap_for_size(16, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-unknown.png").release_value_but_fixme_should_propagate_errors());
}

Optional<GUI::ModelIndex> ManualModel::index_from_path(StringView path) const
{
    for (int section = 0; section < row_count(); ++section) {
        auto parent_index = index(section, 0);
        for (int row = 0; row < row_count(parent_index); ++row) {
            auto child_index = index(row, 0, parent_index);
            auto* node = static_cast<const ManualNode*>(child_index.internal_data());
            if (!node->is_page())
                continue;
            auto* page = static_cast<const ManualPageNode*>(node);
            if (page->path() != path)
                continue;
            return child_index;
        }
    }
    return {};
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

ErrorOr<StringView> ManualModel::page_view(String const& path) const
{
    if (path.is_empty())
        return StringView {};

    {
        // Check if we've got it cached already.
        auto mapped_file = m_mapped_files.get(path);
        if (mapped_file.has_value())
            return StringView { mapped_file.value()->bytes() };
    }

    auto file = TRY(Core::MappedFile::map(path));

    StringView view { file->bytes() };
    m_mapped_files.set(path, move(file));
    return view;
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
    return String::formatted("{}({})", page->name(), section->section_name());
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
        VERIFY_NOT_REACHED();
    }
    for (size_t row = 0; row < parent->parent()->children().size(); row++) {
        ManualNode* child_at_row = &parent->parent()->children()[row];
        if (child_at_row == parent)
            return create_index(row, 0, parent);
    }
    VERIFY_NOT_REACHED();
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

GUI::Variant ManualModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto* node = static_cast<const ManualNode*>(index.internal_data());
    switch (role) {
    case GUI::ModelRole::Search:
        if (!node->is_page())
            return {};
        return String(page_view(page_path(index)).value());
    case GUI::ModelRole::Display:
        return node->name();
    case GUI::ModelRole::Icon:
        if (node->is_page())
            return m_page_icon;
        if (node->is_open())
            return m_section_open_icon;
        return m_section_icon;
    default:
        return {};
    }
}

void ManualModel::update_section_node_on_toggle(const GUI::ModelIndex& index, const bool open)
{
    auto* node = static_cast<ManualSectionNode*>(index.internal_data());
    node->set_open(open);
}

TriState ManualModel::data_matches(const GUI::ModelIndex& index, const GUI::Variant& term) const
{
    auto view_result = page_view(page_path(index));
    if (view_result.is_error() || view_result.value().is_empty())
        return TriState::False;

    return view_result.value().contains(term.as_string()) ? TriState::True : TriState::False;
}
