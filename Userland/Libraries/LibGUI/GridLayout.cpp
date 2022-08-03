/*
 * Copyright (c) 2022, Filiph Sandstrom <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/GridLayout.h>

namespace GUI {

GridLayout::GridLayout()
{
}

UISize GridLayout::preferred_size() const
{
    return min_size();
}

UISize GridLayout::min_size() const
{
    UIDimension columns { m_item_size * m_columns };

    if (!auto_layout()) {
        UIDimension rows { (m_item_size + margins().top()) * (static_cast<int>(m_entries.size()) / m_columns) + margins().top() };
        return { columns, rows };
    }

    // FIXME: Calculate width & height when using auto_layout.
    VERIFY_NOT_REACHED();
}

void GridLayout::run(Widget& widget)
{
    if (m_entries.is_empty())
        return;

    struct Item {
        Widget* widget { nullptr };
        int width { 0 };
        int height { 0 };
    };

    Vector<Item, 32> items;
    for (auto& entry : m_entries) {
        if (!entry.widget || !entry.widget->is_visible()) {
            items.append(Item { nullptr, item_size(), item_size() });
            continue;
        }

        auto preferred_width = clamp(
            entry.widget->effective_preferred_size().primary_size_for_orientation(Gfx::Orientation::Horizontal),
            entry.widget->effective_min_size().primary_size_for_orientation(Gfx::Orientation::Horizontal),
            entry.widget->max_size().primary_size_for_orientation(Gfx::Orientation::Horizontal));
        auto preferred_height = clamp(
            entry.widget->effective_preferred_size().primary_size_for_orientation(Gfx::Orientation::Vertical),
            entry.widget->effective_min_size().primary_size_for_orientation(Gfx::Orientation::Vertical),
            entry.widget->max_size().primary_size_for_orientation(Gfx::Orientation::Vertical));

        int width = item_size();
        int height = item_size();
        if (preferred_width.is_int()) {
            width = preferred_width.as_int();
        }
        if (preferred_height.is_int()) {
            height = preferred_height.as_int();
        }

        items.append(
            Item {
                entry.widget.ptr(),
                width,
                height });
    }

    if (items.is_empty())
        return;

    Gfx::IntRect content_rect = widget.content_rect();
    int current_row = 0;
    int current_column = 0;

    for (auto& item : items) {
        if (current_column + 1 > columns()) {
            current_column = 0;
            current_row += 1;
        }

        if (!item.widget) {
            current_column += 1;
            continue;
        }

        auto size_columns = item.width / item_size();
        auto size_rows = item.height / item_size();

        auto width_padding = 0;
        if (item.width != item_size()) {
            width_padding += (size_columns - 1) * margins().left();

            // Handle having to overflow to the next row
            if (auto_layout() && current_column + size_columns > columns()) {
                current_column = 0;
                current_row += 1;
            }
        }

        auto x = (content_rect.x() + margins().left() + (item_size() + margins().left()) * current_column);
        auto y = (content_rect.y() + margins().top() + (item_size() + margins().top()) * current_row);

        auto height_padding = 0;
        if (item.height != item_size()) {
            height_padding += (size_rows - 1) * margins().top();

            if (auto_layout()) {
                current_row += size_rows - 1;
            }
        }

        Gfx::IntRect rect {
            x,
            y,
            item.width + width_padding,
            item.height + height_padding
        };

        item.widget->set_relative_rect(rect);
        if (auto_layout()) {
            current_column += size_columns;
        } else {
            current_column += 1;
        }
    }
}

}
