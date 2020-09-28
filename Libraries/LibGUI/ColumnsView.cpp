/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
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

#include <LibGUI/ColumnsView.h>
#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Palette.h>

namespace GUI {

static const char* s_arrow_bitmap_data = {
    "         "
    "   #     "
    "   ##    "
    "   ###   "
    "   ####  "
    "   ###   "
    "   ##    "
    "   #     "
    "         "
};
static const int s_arrow_bitmap_width = 9;
static const int s_arrow_bitmap_height = 9;

ColumnsView::ColumnsView()
{
    set_fill_with_background_color(true);
    set_background_role(ColorRole::Base);
    set_foreground_role(ColorRole::BaseText);
    m_columns.append({ {}, 0 });
}

ColumnsView::~ColumnsView()
{
}

void ColumnsView::select_all()
{
    Vector<Column> columns_for_selection;
    selection().for_each_index([&](auto& index) {
        for (auto& column : m_columns) {
            if (column.parent_index == index.parent()) {
                columns_for_selection.append(column);
                return;
            }
        }
        ASSERT_NOT_REACHED();
    });

    for (Column& column : columns_for_selection) {
        int row_count = model()->row_count(column.parent_index);
        for (int row = 0; row < row_count; row++) {
            ModelIndex index = model()->index(row, m_model_column, column.parent_index);
            selection().add(index);
        }
    }
}

void ColumnsView::paint_event(PaintEvent& event)
{
    AbstractView::paint_event(event);

    if (!model())
        return;

    Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    int column_x = 0;

    for (size_t i = 0; i < m_columns.size(); i++) {
        auto& column = m_columns[i];
        auto* next_column = i + 1 == m_columns.size() ? nullptr : &m_columns[i + 1];

        ASSERT(column.width > 0);

        int row_count = model()->row_count(column.parent_index);
        for (int row = 0; row < row_count; row++) {
            ModelIndex index = model()->index(row, m_model_column, column.parent_index);
            ASSERT(index.is_valid());

            bool is_selected_row = selection().contains(index);

            Color background_color = palette().color(background_role());
            Color text_color = palette().color(foreground_role());

            if (next_column != nullptr && next_column->parent_index == index) {
                background_color = palette().inactive_selection();
                text_color = palette().inactive_selection_text();
            }

            if (is_selected_row) {
                background_color = palette().selection();
                text_color = palette().selection_text();
            }

            Gfx::IntRect row_rect { column_x, row * item_height(), column.width, item_height() };
            painter.fill_rect(row_rect, background_color);

            auto icon = index.data(ModelRole::Icon);
            Gfx::IntRect icon_rect = { column_x + icon_spacing(), 0, icon_size(), icon_size() };
            icon_rect.center_vertically_within(row_rect);
            if (icon.is_icon()) {
                if (auto* bitmap = icon.as_icon().bitmap_for_size(icon_size())) {
                    if (m_hovered_index.is_valid() && m_hovered_index.parent() == index.parent() && m_hovered_index.row() == index.row())
                        painter.blit_brightened(icon_rect.location(), *bitmap, bitmap->rect());
                    else
                        painter.blit(icon_rect.location(), *bitmap, bitmap->rect());
                }
            }

            Gfx::IntRect text_rect = {
                icon_rect.right() + 1 + icon_spacing(), row * item_height(),
                column.width - icon_spacing() - icon_size() - icon_spacing() - icon_spacing() - s_arrow_bitmap_width - icon_spacing(), item_height()
            };
            auto text = index.data().to_string();
            painter.draw_text(text_rect, text, Gfx::TextAlignment::CenterLeft, text_color);

            bool expandable = model()->row_count(index) > 0;
            if (expandable) {
                Gfx::IntRect arrow_rect = {
                    text_rect.right() + 1 + icon_spacing(), 0,
                    s_arrow_bitmap_width, s_arrow_bitmap_height
                };
                arrow_rect.center_vertically_within(row_rect);
                static auto& arrow_bitmap = Gfx::CharacterBitmap::create_from_ascii(s_arrow_bitmap_data, s_arrow_bitmap_width, s_arrow_bitmap_height).leak_ref();
                painter.draw_bitmap(arrow_rect.location(), arrow_bitmap, text_color);
            }
        }

        int separator_height = content_size().height();
        if (height() > separator_height)
            separator_height = height();
        painter.draw_line({ column_x + column.width, 0 }, { column_x + column.width, separator_height }, palette().button());
        column_x += column.width + 1;
    }
}

void ColumnsView::push_column(const ModelIndex& parent_index)
{
    ASSERT(model());

    // Drop columns at the end.
    ModelIndex grandparent = model()->parent_index(parent_index);
    for (int i = m_columns.size() - 1; i > 0; i--) {
        if (m_columns[i].parent_index == grandparent)
            break;
        m_columns.shrink(i);
        dbg() << "Dropping column " << i;
    }

    // Add the new column.
    dbg() << "Adding a new column";
    m_columns.append({ parent_index, 0 });
    update_column_sizes();
    update();
}

void ColumnsView::update_column_sizes()
{
    if (!model())
        return;

    int total_width = 0;
    int total_height = 0;

    for (auto& column : m_columns) {
        int row_count = model()->row_count(column.parent_index);

        int column_height = row_count * item_height();
        if (column_height > total_height)
            total_height = column_height;

        column.width = 10;
        for (int row = 0; row < row_count; row++) {
            ModelIndex index = model()->index(row, m_model_column, column.parent_index);
            ASSERT(index.is_valid());
            auto text = index.data().to_string();
            int row_width = icon_spacing() + icon_size() + icon_spacing() + font().width(text) + icon_spacing() + s_arrow_bitmap_width + icon_spacing();
            if (row_width > column.width)
                column.width = row_width;
        }
        total_width += column.width + 1;
    }

    set_content_size({ total_width, total_height });
}

ModelIndex ColumnsView::index_at_event_position(const Gfx::IntPoint& a_position) const
{
    if (!model())
        return {};

    auto position = a_position.translated(horizontal_scrollbar().value() - frame_thickness(), vertical_scrollbar().value() - frame_thickness());

    int column_x = 0;

    for (auto& column : m_columns) {
        if (position.x() < column_x)
            break;
        if (position.x() > column_x + column.width) {
            column_x += column.width;
            continue;
        }

        int row = position.y() / item_height();
        int row_count = model()->row_count(column.parent_index);
        if (row >= row_count)
            return {};

        return model()->index(row, m_model_column, column.parent_index);
    }

    return {};
}

void ColumnsView::mousedown_event(MouseEvent& event)
{
    AbstractView::mousedown_event(event);

    if (!model())
        return;

    if (event.button() != MouseButton::Left)
        return;

    auto index = index_at_event_position(event.position());
    if (index.is_valid() && !(event.modifiers() & Mod_Ctrl)) {
        if (model()->row_count(index))
            push_column(index);
    }
}

void ColumnsView::did_update_model(unsigned flags)
{
    AbstractView::did_update_model(flags);

    // FIXME: Don't drop the columns on minor updates.
    dbg() << "Model was updated; dropping columns :(";
    m_columns.clear();
    m_columns.append({ {}, 0 });

    update_column_sizes();
    update();
}

void ColumnsView::move_cursor(CursorMovement movement, SelectionUpdate selection_update)
{
    if (!model())
        return;
    auto& model = *this->model();
    if (!cursor_index().is_valid()) {
        set_cursor(model.index(0, m_model_column, {}), SelectionUpdate::Set);
        return;
    }

    ModelIndex new_index;
    auto cursor_parent = model.parent_index(cursor_index());

    switch (movement) {
    case CursorMovement::Up: {
        int row = cursor_index().row() > 0 ? cursor_index().row() - 1 : 0;
        new_index = model.index(row, cursor_index().column(), cursor_parent);
        break;
    }
    case CursorMovement::Down: {
        int row = cursor_index().row() + 1;
        new_index = model.index(row, cursor_index().column(), cursor_parent);
        break;
    }
    case CursorMovement::Left:
        new_index = cursor_parent;
        break;
    case CursorMovement::Right:
        new_index = model.index(0, m_model_column, cursor_index());
        if (model.is_valid(new_index)) {
            if (model.is_valid(cursor_index()))
                push_column(cursor_index());
            update();
            break;
        }
    default:
        break;
    }

    if (new_index.is_valid())
        set_cursor(new_index, selection_update);
}

Gfx::IntRect ColumnsView::content_rect(const ModelIndex& index) const
{
    if (!index.is_valid())
        return {};

    int column_x = 0;
    for (auto& column : m_columns) {
        if (column.parent_index == index.parent())
            return { column_x + icon_size(), index.row() * item_height(), column.width - icon_size(), item_height() };
        column_x += column.width + 1;
    }

    return {};
}

}
