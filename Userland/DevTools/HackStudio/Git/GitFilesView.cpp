/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GitFilesView.h"
#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Scrollbar.h>
#include <LibGfx/Palette.h>

namespace HackStudio {

void GitFilesView::paint_list_item(GUI::Painter& painter, int row_index, int painted_item_index)
{
    ListView::paint_list_item(painter, row_index, painted_item_index);

    painter.blit(action_icon_rect((size_t)painted_item_index).top_left(), *m_action_icon, m_action_icon->rect());
}

Gfx::IntRect GitFilesView::action_icon_rect(size_t painted_item_index)
{
    int y = painted_item_index * item_height();
    return { content_width() - 20, y, m_action_icon->rect().width(), m_action_icon->rect().height() };
}

GitFilesView::GitFilesView(GitFileActionCallback callback, NonnullRefPtr<Gfx::Bitmap> action_icon)
    : m_action_callback(move(callback))
    , m_action_icon(action_icon)
{
    set_alternating_row_colors(false);
}

void GitFilesView::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Primary) {
        ListView::mousedown_event(event);
        return;
    }

    if (event.x() < action_icon_rect(0).x() || event.x() >= action_icon_rect(0).right()) {
        ListView::mousedown_event(event);
        return;
    }

    size_t item_index = (event.y() + vertical_scrollbar().value()) / item_height();
    if (model()->row_count() == 0 || item_index > (size_t)model()->row_count()) {
        ListView::mousedown_event(event);
        return;
    }

    auto data = model()->index(item_index, model_column()).data();

    VERIFY(data.is_string());
    m_action_callback(data.to_byte_string());
}

};
