/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "OutlineModel.h"
#include <LibGfx/FontDatabase.h>

NonnullRefPtr<OutlineModel> OutlineModel::create(const NonnullRefPtr<PDF::OutlineDict>& outline)
{
    return adopt_ref(*new OutlineModel(outline));
}

OutlineModel::OutlineModel(const NonnullRefPtr<PDF::OutlineDict>& outline)
    : m_outline(outline)
{
    m_closed_item_icon.set_bitmap_for_size(16, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/book.png").release_value_but_fixme_should_propagate_errors());
    m_open_item_icon.set_bitmap_for_size(16, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/book-open.png").release_value_but_fixme_should_propagate_errors());
}

void OutlineModel::set_index_open_state(const GUI::ModelIndex& index, bool is_open)
{
    VERIFY(index.is_valid());
    auto* outline_item = static_cast<PDF::OutlineItem*>(index.internal_data());

    if (is_open) {
        m_open_outline_items.set(outline_item);
    } else {
        m_open_outline_items.remove(outline_item);
    }
}

int OutlineModel::row_count(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return m_outline->children.size();
    auto outline_item = static_cast<PDF::OutlineItem*>(index.internal_data());
    return static_cast<int>(outline_item->children.size());
}

int OutlineModel::column_count(const GUI::ModelIndex&) const
{
    return 1;
}

GUI::Variant OutlineModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    VERIFY(index.is_valid());
    auto outline_item = static_cast<PDF::OutlineItem*>(index.internal_data());

    switch (role) {
    case GUI::ModelRole::Display:
        return outline_item->title;
    case GUI::ModelRole::Icon:
        if (m_open_outline_items.contains(outline_item))
            return m_open_item_icon;
        return m_closed_item_icon;
    default:
        return {};
    }
}

GUI::ModelIndex OutlineModel::parent_index(const GUI::ModelIndex& index) const
{
    if (!index.is_valid())
        return {};

    auto* outline_item = static_cast<PDF::OutlineItem*>(index.internal_data());
    auto& parent = outline_item->parent;

    if (!parent)
        return {};

    if (parent->parent) {
        auto& grandparent = parent->parent;
        for (size_t i = 0; i < grandparent->children.size(); i++) {
            auto* sibling = &grandparent->children[i];
            if (sibling == index.internal_data())
                return create_index(static_cast<int>(i), 0, sibling);
        }
    } else {
        for (size_t i = 0; i < m_outline->children.size(); i++) {
            auto* sibling = &m_outline->children[i];
            if (sibling == index.internal_data())
                return create_index(static_cast<int>(i), 0, sibling);
        }
    }

    VERIFY_NOT_REACHED();
}

GUI::ModelIndex OutlineModel::index(int row, int column, const GUI::ModelIndex& parent) const
{
    if (!parent.is_valid())
        return create_index(row, column, &m_outline->children[row]);

    auto parent_outline_item = static_cast<PDF::OutlineItem*>(parent.internal_data());
    return create_index(row, column, &parent_outline_item->children[row]);
}
