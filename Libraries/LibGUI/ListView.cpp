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

#include <LibGUI/ListView.h>
#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGfx/Palette.h>

namespace GUI {

ListView::ListView()
{
    set_fill_with_background_color(true);
    set_background_role(ColorRole::Base);
    set_foreground_role(ColorRole::BaseText);
}

ListView::~ListView()
{
}

void ListView::select_all()
{
    selection().clear();
    for (int item_index = 0; item_index < item_count(); ++item_index) {
        auto index = model()->index(item_index, m_model_column);
        selection().add(index);
    }
}

void ListView::update_content_size()
{
    if (!model())
        return set_content_size({});

    int content_width = 0;
    for (int row = 0, row_count = model()->row_count(); row < row_count; ++row) {
        auto text = model()->index(row, m_model_column).data();
        content_width = max(content_width, font().width(text.to_string()));
    }

    content_width = max(content_width, widget_inner_rect().width());

    int content_height = item_count() * item_height();
    set_content_size({ content_width, content_height });
}

void ListView::resize_event(ResizeEvent& event)
{
    update_content_size();
    AbstractView::resize_event(event);
}

void ListView::did_update_model(unsigned flags)
{
    AbstractView::did_update_model(flags);
    update_content_size();
    update();
}

Gfx::IntRect ListView::content_rect(int row) const
{
    return { 0, row * item_height(), content_width(), item_height() };
}

Gfx::IntRect ListView::content_rect(const ModelIndex& index) const
{
    return content_rect(index.row());
}

ModelIndex ListView::index_at_event_position(const Gfx::IntPoint& point) const
{
    ASSERT(model());

    auto adjusted_position = this->adjusted_position(point);
    for (int row = 0, row_count = model()->row_count(); row < row_count; ++row) {
        if (!content_rect(row).contains(adjusted_position))
            continue;
        return model()->index(row, m_model_column);
    }
    return {};
}

Gfx::IntPoint ListView::adjusted_position(const Gfx::IntPoint& position) const
{
    return position.translated(horizontal_scrollbar().value() - frame_thickness(), vertical_scrollbar().value() - frame_thickness());
}

void ListView::paint_list_item(Painter& painter, int row_index, int painted_item_index)
{
    bool is_selected_row = selection().contains_row(row_index);

    int y = painted_item_index * item_height();

    Color background_color;
    if (is_selected_row) {
        background_color = is_focused() ? palette().selection() : palette().inactive_selection();
    } else {
        Color row_fill_color = palette().color(background_role());
        if (alternating_row_colors() && (painted_item_index % 2)) {
            background_color = row_fill_color.darkened(0.8f);
        } else {
            background_color = row_fill_color;
        }
    }

    Gfx::IntRect row_rect(0, y, content_width(), item_height());
    painter.fill_rect(row_rect, background_color);
    auto index = model()->index(row_index, m_model_column);
    auto data = index.data();
    auto font = font_for_index(index);
    if (data.is_bitmap()) {
        painter.blit(row_rect.location(), data.as_bitmap(), data.as_bitmap().rect());
    } else if (data.is_icon()) {
        if (auto bitmap = data.as_icon().bitmap_for_size(16))
            painter.blit(row_rect.location(), *bitmap, bitmap->rect());
    } else {
        Color text_color;
        if (is_selected_row)
            text_color = is_focused() ? palette().selection_text() : palette().inactive_selection_text();
        else
            text_color = index.data(ModelRole::ForegroundColor).to_color(palette().color(foreground_role()));
        auto text_rect = row_rect;
        text_rect.move_by(horizontal_padding(), 0);
        text_rect.set_width(text_rect.width() - horizontal_padding() * 2);
        auto text_alignment = index.data(ModelRole::TextAlignment).to_text_alignment(Gfx::TextAlignment::CenterLeft);
        painter.draw_text(text_rect, data.to_string(), font, text_alignment, text_color);
    }
}

void ListView::paint_event(PaintEvent& event)
{
    Frame::paint_event(event);

    if (!model())
        return;

    Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    int exposed_width = max(content_size().width(), width());
    int painted_item_index = 0;

    for (int row_index = 0; row_index < model()->row_count(); ++row_index) {
        paint_list_item(painter, row_index, painted_item_index);
        ++painted_item_index;
    };

    Gfx::IntRect unpainted_rect(0, painted_item_index * item_height(), exposed_width, height());
    if (fill_with_background_color())
        painter.fill_rect(unpainted_rect, palette().color(background_role()));
}

int ListView::item_count() const
{
    if (!model())
        return 0;
    return model()->row_count();
}

void ListView::mousemove_event(MouseEvent& event)
{
    auto previous_hovered_index = m_hovered_index;
    AbstractView::mousemove_event(event);
    if (hover_highlighting() && previous_hovered_index != m_hovered_index)
        set_cursor(m_hovered_index, SelectionUpdate::Set);
}

void ListView::keydown_event(KeyEvent& event)
{
    if (!model())
        return;

    if (event.key() == KeyCode::Key_Escape) {
        if (on_escape_pressed)
            on_escape_pressed();
        return;
    }
    AbstractView::keydown_event(event);
}

void ListView::move_cursor_relative(int steps, SelectionUpdate selection_update)
{
    if (!model())
        return;
    auto& model = *this->model();
    ModelIndex new_index;
    if (cursor_index().is_valid()) {
        new_index = model.index(cursor_index().row() + steps, cursor_index().column());
    } else {
        new_index = model.index(0, 0);
    }
    set_cursor(new_index, selection_update);
}

void ListView::move_cursor(CursorMovement movement, SelectionUpdate selection_update)
{
    if (!model())
        return;
    auto& model = *this->model();

    if (!cursor_index().is_valid()) {
        set_cursor(model.index(0, 0), SelectionUpdate::Set);
        return;
    }

    ModelIndex new_index;

    switch (movement) {
    case CursorMovement::Up:
        new_index = model.index(cursor_index().row() - 1, cursor_index().column());
        break;
    case CursorMovement::Down:
        new_index = model.index(cursor_index().row() + 1, cursor_index().column());
        break;
    case CursorMovement::Home:
        new_index = model.index(0, 0);
        break;
    case CursorMovement::End:
        new_index = model.index(model.row_count() - 1, 0);
        break;
    case CursorMovement::PageUp: {
        int items_per_page = visible_content_rect().height() / item_height();
        new_index = model.index(max(0, cursor_index().row() - items_per_page), cursor_index().column());
        break;
    }
    case CursorMovement::PageDown: {
        int items_per_page = visible_content_rect().height() / item_height();
        new_index = model.index(min(model.row_count() - 1, cursor_index().row() + items_per_page), cursor_index().column());
        break;
    }
    default:
        break;
    }

    if (new_index.is_valid())
        set_cursor(new_index, selection_update);
}

void ListView::scroll_into_view(const ModelIndex& index, bool scroll_horizontally, bool scroll_vertically)
{
    if (!model())
        return;
    ScrollableWidget::scroll_into_view(content_rect(index.row()), scroll_horizontally, scroll_vertically);
}

}
