/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, networkException <networkexception@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/ColumnsView.h>
#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Scrollbar.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Palette.h>

namespace GUI {

static constexpr Gfx::CharacterBitmap s_arrow_bitmap {
    "         "
    "   #     "
    "   ##    "
    "   ###   "
    "   ####  "
    "   ###   "
    "   ##    "
    "   #     "
    "         "sv,
    9, 9
};

ColumnsView::ColumnsView()
{
    set_fill_with_background_color(true);
    set_background_role(ColorRole::Base);
    set_foreground_role(ColorRole::BaseText);
    m_columns.append({ {}, 0 });
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
        VERIFY_NOT_REACHED();
    });

    for (Column& column : columns_for_selection) {
        int row_count = model()->row_count(column.parent_index);
        for (int row = 0; row < row_count; row++) {
            ModelIndex index = model()->index(row, m_model_column, column.parent_index);
            selection().add(index);
        }
    }
}

void ColumnsView::second_paint_event(PaintEvent& event)
{
    if (!m_rubber_banding)
        return;

    Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(widget_inner_rect());

    // Columns start rendering relative to the widget inner rect. We also account for horizontal scroll here.
    int column_x = widget_inner_rect().left() - horizontal_scrollbar().value();
    for (auto const& column : m_columns) {
        if (m_rubber_band_origin_column.parent_index == column.parent_index)
            break;
        column_x += column.width + 1;
    }

    // After walking all columns to the current one we get its bounds relative to the widget inner rect and scroll position.
    auto column_left = column_x;
    auto column_right = column_x + m_rubber_band_origin_column.width;

    // The rubber band rect always stays inside the widget inner rect, the vertical component is handled by mousemove
    auto rubber_band_left = clamp(column_left, widget_inner_rect().left(), widget_inner_rect().right());
    auto rubber_band_right = clamp(column_right, widget_inner_rect().left(), widget_inner_rect().right());

    auto rubber_band_rect = Gfx::IntRect::from_two_points({ rubber_band_left, m_rubber_band_origin }, { rubber_band_right, m_rubber_band_current });

    painter.fill_rect(rubber_band_rect, palette().rubber_band_fill());
    painter.draw_rect(rubber_band_rect, palette().rubber_band_border());
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

    auto selection_color = is_focused() ? palette().selection() : palette().inactive_selection();

    for (size_t i = 0; i < m_columns.size(); i++) {
        auto& column = m_columns[i];
        auto* next_column = i + 1 == m_columns.size() ? nullptr : &m_columns[i + 1];

        VERIFY(column.width > 0);

        int row_count = model()->row_count(column.parent_index);
        for (int row = 0; row < row_count; row++) {
            ModelIndex index = model()->index(row, m_model_column, column.parent_index);
            VERIFY(index.is_valid());

            bool is_selected_row = selection().contains(index);

            Color background_color = palette().color(background_role());
            Color text_color = palette().color(foreground_role());

            if (next_column != nullptr && next_column->parent_index == index) {
                background_color = palette().inactive_selection();
                text_color = palette().inactive_selection_text();
            }

            if (is_selected_row) {
                background_color = selection_color;
                text_color = is_focused() ? palette().selection_text() : palette().inactive_selection_text();
            }

            Gfx::IntRect row_rect { column_x, row * item_height(), column.width, item_height() };

            if (m_edit_index.row() != row)
                painter.fill_rect(row_rect, background_color);

            auto icon = index.data(ModelRole::Icon);
            Gfx::IntRect icon_rect = { column_x + icon_spacing(), 0, icon_size(), icon_size() };
            icon_rect.center_vertically_within(row_rect);
            if (icon.is_icon()) {
                if (auto* bitmap = icon.as_icon().bitmap_for_size(icon_size())) {
                    if (is_selected_row) {
                        auto tint = selection_color.with_alpha(100);
                        painter.blit_filtered(icon_rect.location(), *bitmap, bitmap->rect(), [&](auto src) { return src.blend(tint); });
                    } else if (m_hovered_index.is_valid() && m_hovered_index.parent() == index.parent() && m_hovered_index.row() == index.row()) {
                        painter.blit_brightened(icon_rect.location(), *bitmap, bitmap->rect());
                    } else {
                        auto opacity = index.data(ModelRole::IconOpacity).as_float_or(1.0f);
                        painter.blit(icon_rect.location(), *bitmap, bitmap->rect(), opacity);
                    }
                }
            }

            Gfx::IntRect text_rect = {
                icon_rect.right() + icon_spacing(), row * item_height(),
                column.width - icon_spacing() - icon_size() - icon_spacing() - icon_spacing() - static_cast<int>(s_arrow_bitmap.width()) - icon_spacing(), item_height()
            };
            draw_item_text(painter, index, is_selected_row, text_rect, index.data().to_byte_string(), font_for_index(index), Gfx::TextAlignment::CenterLeft, Gfx::TextElision::None);

            if (is_focused() && index == cursor_index()) {
                painter.draw_rect(row_rect, palette().color(background_role()));
                painter.draw_focus_rect(row_rect, palette().focus_outline());
            }

            if (has_pending_drop() && index == drop_candidate_index()) {
                painter.draw_rect(row_rect, palette().selection(), true);
            }

            bool expandable = model()->row_count(index) > 0;
            if (expandable) {
                Gfx::IntRect arrow_rect = {
                    text_rect.right() + icon_spacing(), 0,
                    s_arrow_bitmap.width(), s_arrow_bitmap.height()
                };
                arrow_rect.center_vertically_within(row_rect);
                painter.draw_bitmap(arrow_rect.location(), s_arrow_bitmap, text_color);
            }
        }

        int separator_height = content_size().height();
        if (height() > separator_height)
            separator_height = height();
        painter.draw_line({ column_x + column.width, 0 }, { column_x + column.width, separator_height }, palette().button());
        column_x += column.width + column_separator_width();
    }
}

void ColumnsView::push_column(ModelIndex const& parent_index)
{
    VERIFY(model());

    // Drop columns at the end.
    ModelIndex grandparent = model()->parent_index(parent_index);
    for (int i = m_columns.size() - 1; i > 0; i--) {
        if (m_columns[i].parent_index == grandparent)
            break;
        m_columns.shrink(i);
        dbgln("Dropping column {}", i);
    }

    // Add the new column.
    dbgln("Adding a new column");
    m_columns.append({ parent_index, 0 });
    update_column_sizes();

    // FIXME: Find a way not to jump the view so much when changing folders within the same directory.
    scroll_to_right();

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
            VERIFY(index.is_valid());
            auto text = index.data().to_byte_string();
            int row_width = icon_spacing() + icon_size() + icon_spacing() + font().width(text) + icon_spacing() + s_arrow_bitmap.width() + icon_spacing();
            if (row_width > column.width)
                column.width = row_width;
        }
        total_width += column.width + column_separator_width();
    }

    // "Hide" last separator behind a window frame.
    total_width -= column_separator_width();

    set_content_size({ total_width, total_height });
}

Optional<ColumnsView::Column> ColumnsView::column_at_event_position(Gfx::IntPoint position) const
{
    if (!model())
        return {};

    int column_x = 0;

    for (auto const& column : m_columns) {
        if (position.x() < column_x)
            break;
        if (position.x() > column_x + column.width) {
            column_x += column.width + column_separator_width();
            continue;
        }

        return column;
    }

    return {};
}

void ColumnsView::select_range(ModelIndex const& index)
{
    auto min_row = min(selection_start_index().row(), index.row());
    auto max_row = max(selection_start_index().row(), index.row());
    auto parent = index.parent();

    clear_selection();
    for (auto row = min_row; row <= max_row; ++row) {
        auto new_index = model()->index(row, m_model_column, parent);
        if (new_index.is_valid())
            toggle_selection(new_index);
    }
}

ModelIndex ColumnsView::index_at_event_position_in_column(Gfx::IntPoint position, Column const& column) const
{
    int row = position.y() / item_height();
    int row_count = model()->row_count(column.parent_index);
    if (row >= row_count)
        return {};

    return model()->index(row, m_model_column, column.parent_index);
}

ModelIndex ColumnsView::index_at_event_position(Gfx::IntPoint widget_position) const
{
    auto position = to_content_position(widget_position);
    auto const& column = column_at_event_position(position);
    if (!column.has_value())
        return {};

    return index_at_event_position_in_column(position, *column);
}

void ColumnsView::mousedown_event(MouseEvent& event)
{
    AbstractView::mousedown_event(event);

    if (!model())
        return;

    if (event.button() != MouseButton::Primary)
        return;

    auto position = to_content_position(event.position());
    auto column = column_at_event_position(position);
    if (!column.has_value())
        return;

    auto index = index_at_event_position_in_column(position, *column);
    if (index.is_valid() && !(event.modifiers() & Mod_Ctrl)) {
        if (model()->row_count(index)) {
            auto is_index_already_open = m_columns.first_matching([&](auto& column) { return column.parent_index == index; }).has_value();
            if (is_index_already_open) {
                set_cursor(index, SelectionUpdate::Set);
            } else {
                push_column(index);
            }
        }
        return;
    }

    if (selection_mode() == SelectionMode::MultiSelection) {
        m_rubber_banding = true;
        m_rubber_band_origin_column = *column;
        m_rubber_band_origin = position.y();
        m_rubber_band_current = position.y();
    }
}

void ColumnsView::mousemove_event(MouseEvent& event)
{
    if (m_rubber_banding) {
        m_rubber_band_current = clamp(event.position().y(), widget_inner_rect().top(), widget_inner_rect().bottom());

        auto parent = m_rubber_band_origin_column.parent_index;
        int row_count = model()->row_count(parent);

        clear_selection();

        set_suppress_update_on_selection_change(true);

        for (int row = 0; row < row_count; row++) {
            auto index = model()->index(row, m_model_column, parent);
            VERIFY(index.is_valid());

            int row_top = row * item_height();
            int row_bottom = row * item_height() + item_height();

            if ((m_rubber_band_origin > row_top && m_rubber_band_current < row_top) || (m_rubber_band_origin > row_bottom && m_rubber_band_current < row_bottom)) {
                add_selection(index);
            }
        }

        set_suppress_update_on_selection_change(false);

        update();
    }

    AbstractView::mousemove_event(event);
}

void ColumnsView::mouseup_event(MouseEvent& event)
{
    if (m_rubber_banding && event.button() == MouseButton::Primary) {
        m_rubber_banding = false;
        update();
    }
}

void ColumnsView::model_did_update(unsigned flags)
{
    AbstractView::model_did_update(flags);

    // FIXME: Don't drop the columns on minor updates.
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
    case CursorMovement::Right: {
        // Don't reset columns if one already exists.
        auto maybe_column = m_columns.first_matching([&](auto& column) { return model.parent_index(column.parent_index) == cursor_index(); });
        if (maybe_column.has_value()) {
            new_index = maybe_column->parent_index;
            break;
        }

        new_index = model.index(0, m_model_column, cursor_index());
        if (model.is_within_range(new_index)) {
            if (model.is_within_range(cursor_index()))
                push_column(cursor_index());
            update();
        }
        break;
    }
    default:
        break;
    }

    if (new_index.is_valid())
        set_cursor(new_index, selection_update);
}

Gfx::IntRect ColumnsView::index_content_rect(ModelIndex const& index)
{
    int column_x = 0;
    for (auto const& column : m_columns) {
        if (column.parent_index == index.parent())
            return { column_x, index.row() * item_height(), column.width, item_height() };

        column_x += column.width + column_separator_width();
    }
    return {};
}

void ColumnsView::scroll_into_view(ModelIndex const& index, bool scroll_horizontally, bool scroll_vertically)
{
    if (!model())
        return;
    AbstractScrollableWidget::scroll_into_view(index_content_rect(index), scroll_horizontally, scroll_vertically);
}

Gfx::IntRect ColumnsView::content_rect(ModelIndex const& index) const
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

Gfx::IntRect ColumnsView::paint_invalidation_rect(ModelIndex const& index) const
{
    auto rect = content_rect(index);
    rect.translate_by(-icon_size(), 0);
    rect.set_width(rect.width() + icon_size());
    return rect;
}

}
