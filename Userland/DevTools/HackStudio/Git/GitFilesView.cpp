/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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

#include "GitFilesView.h"
#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGfx/Palette.h>

namespace HackStudio {
GitFilesView::~GitFilesView()
{
}

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
    if (event.button() != GUI::MouseButton::Left) {
        ListView::mousedown_event(event);
        return;
    }

    if (event.x() < action_icon_rect(0).x() || event.x() > action_icon_rect(0).top_right().x()) {
        ListView::mousedown_event(event);
        return;
    }

    size_t item_index = (event.y() + vertical_scrollbar().value()) / item_height();
    if (model()->row_count() == 0 || item_index > (size_t)model()->row_count()) {
        ListView::mousedown_event(event);
        return;
    }

    auto data = model()->index(item_index, model_column()).data();

    ASSERT(data.is_string());
    m_action_callback(LexicalPath(data.to_string()));
}

};
