/*
 * Copyright (c) 2021-2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FilterModel.h"
#include <LibGUI/FileIconProvider.h>

namespace PixelPaint {
FilterModel::FilterModel()
{

    auto filter_bitmap = Gfx::Bitmap::try_load_from_file("/res/icons/pixelpaint/filter.png").release_value_but_fixme_should_propagate_errors();
    m_filter_icon = GUI::Icon(filter_bitmap);
}

GUI::ModelIndex FilterModel::index(int row, int column, const GUI::ModelIndex& parent_index) const
{
    if (!parent_index.is_valid()) {
        if (static_cast<size_t>(row) >= m_filters.size())
            return {};
        return create_index(row, column, &m_filters[row]);
    }
    auto* parent = static_cast<const FilterInfo*>(parent_index.internal_data());
    if (static_cast<size_t>(row) >= parent->children.size())
        return {};
    auto* child = &parent->children[row];
    return create_index(row, column, child);
}

GUI::ModelIndex FilterModel::parent_index(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return {};

    auto* child = static_cast<const FilterInfo*>(index.internal_data());
    auto* parent = child->parent;
    if (parent == nullptr)
        return {};

    if (parent->parent == nullptr) {
        for (size_t row = 0; row < m_filters.size(); row++)
            if (m_filters.ptr_at(row).ptr() == parent)
                return create_index(row, 0, parent);
        VERIFY_NOT_REACHED();
    }
    for (size_t row = 0; row < parent->parent->children.size(); row++) {
        FilterInfo* child_at_row = parent->parent->children.ptr_at(row).ptr();
        if (child_at_row == parent)
            return create_index(row, 0, parent);
    }
    VERIFY_NOT_REACHED();
}

int FilterModel::row_count(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return m_filters.size();
    auto* node = static_cast<const FilterInfo*>(index.internal_data());
    return node->children.size();
}

GUI::Variant FilterModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto* filter = static_cast<const FilterInfo*>(index.internal_data());
    switch (role) {
    case GUI::ModelRole::Display:
        return filter->text;
    case GUI::ModelRole::Icon:
        if (filter->type == FilterInfo::Type::Category)
            return GUI::FileIconProvider::directory_icon();
        return m_filter_icon;
    default:
        return {};
    }
}
}
