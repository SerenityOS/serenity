/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf8View.h>
#include <LibCore/Timer.h>
#include <LibGUI/IconView.h>
#include <LibGUI/Model.h>
#include <LibGUI/ModelEditingDelegate.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Scrollbar.h>

#include <LibGfx/Palette.h>

REGISTER_WIDGET(GUI, IconView);

namespace GUI {

IconView::IconView()
{
    set_fill_with_background_color(true);
    set_background_role(ColorRole::Base);
    set_foreground_role(ColorRole::BaseText);
    horizontal_scrollbar().set_visible(false);
}

void IconView::select_all()
{
    for (int item_index = 0; item_index < item_count(); ++item_index) {
        auto& item_data = m_item_data_cache[item_index];
        if (!item_data.selected) {
            if (item_data.is_valid())
                add_selection(item_data);
            else
                add_selection(model()->index(item_index, model_column()));
        }
    }
}

void IconView::scroll_into_view(ModelIndex const& index, bool scroll_horizontally, bool scroll_vertically)
{
    if (!index.is_valid())
        return;
    AbstractScrollableWidget::scroll_into_view(item_rect(index.row()), scroll_horizontally, scroll_vertically);
}

void IconView::resize_event(ResizeEvent& event)
{
    AbstractView::resize_event(event);
    update_content_size();

    if (!m_had_valid_size) {
        m_had_valid_size = true;
        if (!selection().is_empty())
            scroll_into_view(selection().first());
    }
}

void IconView::did_change_font()
{
    AbstractView::did_change_font();
    rebuild_item_cache();
}

void IconView::rebuild_item_cache() const
{
    auto prev_item_count = m_item_data_cache.size();
    size_t new_item_count = item_count();
    auto items_to_invalidate = min(prev_item_count, new_item_count);

    // if the new number of items is less, check if any of the
    // ones not in the list anymore was selected
    for (size_t i = new_item_count; i < m_item_data_cache.size(); i++) {
        auto& item_data = m_item_data_cache[i];
        if (item_data.selected) {
            VERIFY(m_selected_count_cache > 0);
            m_selected_count_cache--;
        }
    }
    if ((size_t)m_first_selected_hint >= new_item_count)
        m_first_selected_hint = 0;
    m_item_data_cache.resize(new_item_count);
    for (size_t i = 0; i < items_to_invalidate; i++) {
        auto& item_data = m_item_data_cache[i];
        // TODO: It's unfortunate that we have no way to know whether any
        // data actually changed, so we have to invalidate *everyone*
        if (item_data.is_valid() /* && !model()->is_valid(item_data.index)*/)
            item_data.invalidate();
        if (item_data.selected && i < (size_t)m_first_selected_hint)
            m_first_selected_hint = (int)i;
    }

    m_item_data_cache_valid = true;
}

auto IconView::get_item_data(int item_index) const -> ItemData&
{
    if (!m_item_data_cache_valid)
        rebuild_item_cache();

    auto& item_data = m_item_data_cache[item_index];
    if (item_data.is_valid())
        return item_data;

    item_data.index = model()->index(item_index, model_column());
    item_data.text = item_data.index.data().to_byte_string();
    get_item_rects(item_index, item_data, font_for_index(item_data.index));
    item_data.valid = true;
    return item_data;
}

auto IconView::item_data_from_content_position(Gfx::IntPoint content_position) const -> ItemData*
{
    if (!m_visual_row_count || !m_visual_column_count)
        return nullptr;
    int row, column;
    column_row_from_content_position(content_position, row, column);
    int item_index = (m_flow_direction == FlowDirection::LeftToRight)
        ? row * m_visual_column_count + column
        : column * m_visual_row_count + row;
    if (item_index < 0 || item_index >= item_count())
        return nullptr;
    return &get_item_data(item_index);
}

void IconView::model_did_update(unsigned flags)
{
    AbstractView::model_did_update(flags);
    if (!model() || (flags & GUI::Model::InvalidateAllIndices)) {
        m_item_data_cache.clear();
        AbstractView::clear_selection();
        m_selected_count_cache = 0;
        m_first_selected_hint = 0;
    }
    m_item_data_cache_valid = false;
    update_content_size();
    update();
}

void IconView::update_content_size()
{
    if (!model())
        return set_content_size({});

    int content_width;
    int content_height;

    if (m_flow_direction == FlowDirection::LeftToRight) {
        m_visual_column_count = max(1, available_size().width() / effective_item_size().width());
        if (m_visual_column_count)
            m_visual_row_count = ceil_div(model()->row_count(), m_visual_column_count);
        else
            m_visual_row_count = 0;
        content_width = m_visual_column_count * effective_item_size().width();
        content_height = m_visual_row_count * effective_item_size().height();
    } else {
        m_visual_row_count = max(1, available_size().height() / effective_item_size().height());
        if (m_visual_row_count)
            m_visual_column_count = ceil_div(model()->row_count(), m_visual_row_count);
        else
            m_visual_column_count = 0;
        content_width = m_visual_column_count * effective_item_size().width();
        content_height = available_size().height();
    }

    set_content_size({ content_width, content_height });

    if (!m_item_data_cache_valid)
        rebuild_item_cache();

    for (int item_index = 0; item_index < item_count(); item_index++) {
        auto& item_data = m_item_data_cache[item_index];
        if (item_data.is_valid())
            update_item_rects(item_index, item_data);
    }
}

Gfx::IntRect IconView::item_rect(int item_index) const
{
    if (!m_visual_row_count || !m_visual_column_count)
        return {};
    int visual_row_index;
    int visual_column_index;

    if (m_flow_direction == FlowDirection::LeftToRight) {
        visual_row_index = item_index / m_visual_column_count;
        visual_column_index = item_index % m_visual_column_count;
    } else {
        visual_row_index = item_index % m_visual_row_count;
        visual_column_index = item_index / m_visual_row_count;
    }

    return {
        visual_column_index * effective_item_size().width(),
        visual_row_index * effective_item_size().height(),
        effective_item_size().width(),
        effective_item_size().height()
    };
}

ModelIndex IconView::index_at_event_position(Gfx::IntPoint position) const
{
    VERIFY(model());
    auto adjusted_position = to_content_position(position);
    if (auto item_data = item_data_from_content_position(adjusted_position)) {
        if (item_data->is_containing(adjusted_position))
            return item_data->index;
    }
    return {};
}

void IconView::mousedown_event(MouseEvent& event)
{
    if (!model())
        return AbstractView::mousedown_event(event);

    if (event.button() != MouseButton::Primary)
        return AbstractView::mousedown_event(event);

    auto index = index_at_event_position(event.position());
    if (index.is_valid()) {
        // We might start dragging this item, but not rubber-banding.
        return AbstractView::mousedown_event(event);
    }

    if (!(event.modifiers() & Mod_Ctrl)) {
        clear_selection();
    }

    auto adjusted_position = to_content_position(event.position());

    m_might_drag = false;
    if (selection_mode() == SelectionMode::MultiSelection) {
        m_rubber_banding = true;
        m_rubber_band_origin = adjusted_position;
        m_rubber_band_current = adjusted_position;
    }
}

void IconView::mouseup_event(MouseEvent& event)
{
    if (m_rubber_banding && event.button() == MouseButton::Primary) {
        m_rubber_banding = false;
        set_automatic_scrolling_timer_active(false);
        update(to_widget_rect(Gfx::IntRect::from_two_points(m_rubber_band_origin, m_rubber_band_current)));
    }
    AbstractView::mouseup_event(event);
}

bool IconView::update_rubber_banding(Gfx::IntPoint input_position)
{
    auto adjusted_position = to_content_position(input_position.constrained(widget_inner_rect().inflated(1, 1)));
    if (m_rubber_band_current != adjusted_position) {
        auto prev_rect = Gfx::IntRect::from_two_points(m_rubber_band_origin, m_rubber_band_current);
        auto prev_rubber_band_fill_rect = prev_rect.shrunken(1, 1);
        m_rubber_band_current = adjusted_position;
        auto rubber_band_rect = Gfx::IntRect::from_two_points(m_rubber_band_origin, m_rubber_band_current);
        auto rubber_band_fill_rect = rubber_band_rect.shrunken(1, 1);

        for (auto& rect : prev_rubber_band_fill_rect.shatter(rubber_band_fill_rect))
            update(to_widget_rect(rect.inflated(1, 1)));
        for (auto& rect : rubber_band_fill_rect.shatter(prev_rubber_band_fill_rect))
            update(to_widget_rect(rect.inflated(1, 1)));

        // If the rectangle width or height is 0, we still want to be able
        // to match the items in the path. An easy work-around for this
        // is to simply set the width or height to 1
        auto ensure_rect = [](Gfx::IntRect& rect) {
            if (rect.width() <= 0)
                rect.set_width(1);
            if (rect.height() <= 0)
                rect.set_height(1);
        };
        ensure_rect(prev_rect);
        ensure_rect(rubber_band_rect);

        // Clearing the entire selection every time is very expensive,
        // determine what items may need to be deselected and what new
        // items may need to be selected. Avoid a ton of allocations.

        auto deselect_area = prev_rect.shatter(rubber_band_rect);
        auto select_area = rubber_band_rect.shatter(prev_rect);

        // Initialize all candidate's toggle flag. We need to know which
        // items we touched because the various rectangles likely will
        // contain the same item more than once
        for_each_item_intersecting_rects(deselect_area, [](ItemData& item_data) -> IterationDecision {
            item_data.selection_toggled = false;
            return IterationDecision::Continue;
        });
        for_each_item_intersecting_rects(select_area, [](ItemData& item_data) -> IterationDecision {
            item_data.selection_toggled = false;
            return IterationDecision::Continue;
        });

        // We're changing the selection and invalidating those items, so
        // no need to trigger a full re-render for each item
        set_suppress_update_on_selection_change(true);

        // Now toggle all items that are no longer in the selected area, once only
        for_each_item_intersecting_rects(deselect_area, [&](ItemData& item_data) -> IterationDecision {
            if (!item_data.selection_toggled && item_data.is_intersecting(prev_rect) && !item_data.is_intersecting(rubber_band_rect)) {
                item_data.selection_toggled = true;
                toggle_selection(item_data);
                update(to_widget_rect(item_data.rect()));
            }
            return IterationDecision::Continue;
        });
        // Now toggle all items that are in the new selected area, once only
        for_each_item_intersecting_rects(select_area, [&](ItemData& item_data) -> IterationDecision {
            if (!item_data.selection_toggled && !item_data.is_intersecting(prev_rect) && item_data.is_intersecting(rubber_band_rect)) {
                item_data.selection_toggled = true;
                toggle_selection(item_data);
                update(to_widget_rect(item_data.rect()));
            }
            return IterationDecision::Continue;
        });

        set_suppress_update_on_selection_change(false);

        return true;
    }
    return false;
}

void IconView::mousemove_event(MouseEvent& event)
{
    if (!model())
        return AbstractView::mousemove_event(event);

    m_rubber_band_scroll_delta = automatic_scroll_delta_from_position(event.position());

    if (m_rubber_banding) {
        m_out_of_view_position = event.position();
        set_automatic_scrolling_timer_active(!m_rubber_band_scroll_delta.is_zero());

        if (update_rubber_banding(event.position()))
            return;
    }

    AbstractView::mousemove_event(event);
}

void IconView::automatic_scrolling_timer_did_fire()
{
    AbstractView::automatic_scrolling_timer_did_fire();

    if (m_rubber_band_scroll_delta.is_zero())
        return;

    vertical_scrollbar().increase_slider_by(m_rubber_band_scroll_delta.y());
    horizontal_scrollbar().increase_slider_by(m_rubber_band_scroll_delta.x());
    update_rubber_banding(m_out_of_view_position);
}

void IconView::update_item_rects(int item_index, ItemData& item_data) const
{
    auto item_rect = this->item_rect(item_index);
    item_data.icon_rect.center_within(item_rect);
    item_data.icon_rect.translate_by(0, item_data.icon_offset_y);
    item_data.text_rect.center_horizontally_within(item_rect);
    item_data.text_rect.set_top(item_rect.y() + item_data.text_offset_y);
}

Gfx::IntRect IconView::content_rect(ModelIndex const& index) const
{
    if (!index.is_valid())
        return {};
    auto& item_data = get_item_data(index.row());
    return item_data.rect();
}

Gfx::IntRect IconView::editing_rect(ModelIndex const& index) const
{
    if (!index.is_valid())
        return {};
    auto& item_data = get_item_data(index.row());
    auto editing_rect = item_data.text_rect;
    editing_rect.set_height(font_for_index(index)->pixel_size_rounded_up() + 8);
    editing_rect.set_y(item_data.text_rect.y() - 2);
    return editing_rect;
}

void IconView::editing_widget_did_change(ModelIndex const& index)
{
    if (m_editing_delegate->value().is_string()) {
        auto text_width = font_for_index(index)->width(m_editing_delegate->value().as_string());
        m_edit_widget_content_rect.set_width(min(text_width + 8, effective_item_size().width()));
        m_edit_widget_content_rect.center_horizontally_within(editing_rect(index).translated(frame_thickness(), frame_thickness()));
        update_edit_widget_position();
    }
}

Gfx::IntRect
IconView::paint_invalidation_rect(ModelIndex const& index) const
{
    if (!index.is_valid())
        return {};
    auto& item_data = get_item_data(index.row());
    return item_data.rect(true);
}

void IconView::did_change_hovered_index(ModelIndex const& old_index, ModelIndex const& new_index)
{
    AbstractView::did_change_hovered_index(old_index, new_index);
    if (old_index.is_valid())
        get_item_rects(old_index.row(), get_item_data(old_index.row()), font_for_index(old_index));
    if (new_index.is_valid())
        get_item_rects(new_index.row(), get_item_data(new_index.row()), font_for_index(new_index));
}

void IconView::did_change_cursor_index(ModelIndex const& old_index, ModelIndex const& new_index)
{
    AbstractView::did_change_cursor_index(old_index, new_index);
    if (old_index.is_valid())
        get_item_rects(old_index.row(), get_item_data(old_index.row()), font_for_index(old_index));
    if (new_index.is_valid())
        get_item_rects(new_index.row(), get_item_data(new_index.row()), font_for_index(new_index));
}

void IconView::get_item_rects(int item_index, ItemData& item_data, Gfx::Font const& font) const
{
    auto item_rect = this->item_rect(item_index);
    item_data.icon_rect = Gfx::IntRect(0, 0, 32, 32).centered_within(item_rect);
    item_data.icon_offset_y = -font.pixel_size_rounded_up() - 6;
    item_data.icon_rect.translate_by(0, item_data.icon_offset_y);

    int unwrapped_text_width = font.width_rounded_up(item_data.text);
    int available_width = item_rect.width() - 6;

    item_data.text_rect = { 0, item_data.icon_rect.bottom() + 6, 0, font.pixel_size_rounded_up() };
    item_data.wrapped_text_lines.clear();

    if ((unwrapped_text_width > available_width) && (item_data.selected || m_hovered_index == item_data.index || cursor_index() == item_data.index || m_always_wrap_item_labels)) {
        int current_line_width = 0;
        int current_line_start = 0;
        int widest_line_width = 0;
        Utf8View utf8_view(item_data.text);
        auto it = utf8_view.begin();
        for (; it != utf8_view.end(); ++it) {
            auto code_point = *it;
            auto glyph_width = font.glyph_width(code_point);
            if ((current_line_width + glyph_width + font.glyph_spacing()) > available_width) {
                item_data.wrapped_text_lines.append(item_data.text.substring_view(current_line_start, utf8_view.byte_offset_of(it) - current_line_start));
                current_line_start = utf8_view.byte_offset_of(it);
                current_line_width = glyph_width;
            } else {
                current_line_width += glyph_width + font.glyph_spacing();
            }
            widest_line_width = max(widest_line_width, current_line_width);
        }
        if (current_line_width > 0) {
            item_data.wrapped_text_lines.append(item_data.text.substring_view(current_line_start, utf8_view.byte_offset_of(it) - current_line_start));
        }
        item_data.text_rect.set_width(widest_line_width);
        item_data.text_rect.center_horizontally_within(item_rect);
        item_data.text_rect.intersect(item_rect);
        item_data.text_rect.set_height(font.pixel_size_rounded_up() * item_data.wrapped_text_lines.size());
        item_data.text_rect.inflate(6, 6);
        item_data.text_rect_wrapped = item_data.text_rect;
    } else {
        item_data.text_rect.set_width(unwrapped_text_width);
        item_data.text_rect.inflate(6, 6);
        if (item_data.text_rect.width() > available_width)
            item_data.text_rect.set_width(available_width);
        item_data.text_rect.center_horizontally_within(item_rect);
    }
    item_data.text_rect.intersect(item_rect);
    item_data.text_offset_y = item_data.text_rect.y() - item_rect.y();
}

void IconView::second_paint_event(PaintEvent& event)
{
    if (!m_rubber_banding)
        return;

    Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(widget_inner_rect());
    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    auto rubber_band_rect = Gfx::IntRect::from_two_points(m_rubber_band_origin, m_rubber_band_current);
    painter.fill_rect(rubber_band_rect, palette().rubber_band_fill());
    painter.draw_rect(rubber_band_rect, palette().rubber_band_border());
}

void IconView::paint_event(PaintEvent& event)
{
    Color widget_background_color = palette().color(background_role());
    Frame::paint_event(event);

    Painter painter(*this);
    painter.add_clip_rect(widget_inner_rect());
    painter.add_clip_rect(event.rect());

    painter.fill_rect(event.rect(), fill_with_background_color() ? widget_background_color : Color::Transparent);

    if (!model())
        return;

    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    auto selection_color = is_focused() ? palette().selection() : palette().inactive_selection();

    for_each_item_intersecting_rect(to_content_rect(event.rect()), [&](auto& item_data) -> IterationDecision {
        Color background_color;
        if (item_data.selected) {
            background_color = selection_color;
        } else {
            if (fill_with_background_color())
                background_color = widget_background_color;
        }

        auto icon = item_data.index.data(ModelRole::Icon);

        if (icon.is_icon()) {
            if (auto bitmap = icon.as_icon().bitmap_for_size(item_data.icon_rect.width())) {
                Gfx::IntRect destination = bitmap->rect();
                destination.center_within(item_data.icon_rect);

                if (item_data.selected) {
                    auto tint = selection_color.with_alpha(100);
                    painter.blit_filtered(destination.location(), *bitmap, bitmap->rect(), [&](auto src) { return src.blend(tint); });
                } else if (m_hovered_index.is_valid() && m_hovered_index == item_data.index) {
                    painter.blit_brightened(destination.location(), *bitmap, bitmap->rect());
                } else {
                    auto opacity = item_data.index.data(ModelRole::IconOpacity).as_float_or(1.0f);
                    painter.blit(destination.location(), *bitmap, bitmap->rect(), opacity);
                }
            }
        }

        auto font = font_for_index(item_data.index);

        auto const& text_rect = item_data.text_rect;

        if (m_edit_index != item_data.index)
            painter.fill_rect(text_rect, background_color);

        if (is_focused() && item_data.index == cursor_index()) {
            painter.draw_rect(text_rect, widget_background_color);
            painter.draw_focus_rect(text_rect, palette().focus_outline());
        }

        if (!item_data.wrapped_text_lines.is_empty()) {
            // Item text would not fit in the item text rect, let's break it up into lines..

            auto const& lines = item_data.wrapped_text_lines;
            size_t number_of_text_lines = min((size_t)text_rect.height() / font->pixel_size_rounded_up(), lines.size());
            size_t previous_line_lengths = 0;
            for (size_t line_index = 0; line_index < number_of_text_lines; ++line_index) {
                Gfx::IntRect line_rect;
                line_rect.set_width(text_rect.width());
                line_rect.set_height(font->pixel_size_rounded_up());
                line_rect.center_horizontally_within(item_data.text_rect);
                line_rect.set_y(3 + item_data.text_rect.y() + line_index * font->pixel_size_rounded_up());
                line_rect.inflate(6, 0);

                // Shrink the line_rect on the last line to apply elision if there are more lines.
                if (number_of_text_lines - 1 == line_index && lines.size() > number_of_text_lines)
                    line_rect.inflate(-(6 + 2 * font->max_glyph_width()), 0);

                draw_item_text(painter, item_data.index, item_data.selected, line_rect, lines[line_index], font, Gfx::TextAlignment::Center, Gfx::TextElision::Right, previous_line_lengths);
                previous_line_lengths += lines[line_index].length();
            }
        } else {
            draw_item_text(painter, item_data.index, item_data.selected, item_data.text_rect, item_data.text, font, Gfx::TextAlignment::Center, Gfx::TextElision::Right);
        }

        if (has_pending_drop() && item_data.index == drop_candidate_index()) {
            // FIXME: This visualization is not great, as it's also possible to drop things on the text label..
            painter.draw_rect(item_data.icon_rect.inflated(8, 8), palette().selection(), true);
        }
        return IterationDecision::Continue;
    });
}

int IconView::item_count() const
{
    if (!model())
        return 0;
    return model()->row_count();
}

void IconView::did_update_selection()
{
    AbstractView::did_update_selection();
    if (m_changing_selection)
        return;

    // Selection was modified externally, we need to synchronize our cache
    do_clear_selection();
    selection().for_each_index([&](ModelIndex const& index) {
        if (index.is_valid()) {
            auto item_index = model_index_to_item_index(index);
            if ((size_t)item_index < m_item_data_cache.size())
                do_add_selection(get_item_data(item_index));
        }
    });
}

void IconView::do_clear_selection()
{
    for (size_t item_index = m_first_selected_hint; item_index < m_item_data_cache.size(); item_index++) {
        if (m_selected_count_cache == 0)
            break;
        auto& item_data = m_item_data_cache[item_index];
        if (!item_data.selected)
            continue;
        item_data.selected = false;
        m_selected_count_cache--;
    }
    m_first_selected_hint = 0;
    VERIFY(m_selected_count_cache == 0);
}

void IconView::clear_selection()
{
    TemporaryChange change(m_changing_selection, true);
    AbstractView::clear_selection();
    do_clear_selection();
}

bool IconView::do_add_selection(ItemData& item_data)
{
    if (!item_data.selected) {
        item_data.selected = true;
        m_selected_count_cache++;
        int item_index = &item_data - &m_item_data_cache[0];
        if (m_first_selected_hint > item_index)
            m_first_selected_hint = item_index;
        return true;
    }
    return false;
}

void IconView::add_selection(ItemData& item_data)
{
    if (do_add_selection(item_data))
        AbstractView::add_selection(item_data.index);
}

void IconView::add_selection(ModelIndex const& new_index)
{
    TemporaryChange change(m_changing_selection, true);
    auto item_index = model_index_to_item_index(new_index);
    add_selection(get_item_data(item_index));
}

void IconView::toggle_selection(ItemData& item_data)
{
    if (!item_data.selected)
        add_selection(item_data);
    else
        remove_item_selection(item_data);
}

void IconView::toggle_selection(ModelIndex const& new_index)
{
    TemporaryChange change(m_changing_selection, true);
    auto item_index = model_index_to_item_index(new_index);
    toggle_selection(get_item_data(item_index));
}

void IconView::remove_item_selection(ItemData& item_data)
{
    if (!item_data.selected)
        return;

    TemporaryChange change(m_changing_selection, true);
    item_data.selected = false;
    VERIFY(m_selected_count_cache > 0);
    m_selected_count_cache--;
    int item_index = &item_data - &m_item_data_cache[0];
    if (m_first_selected_hint == item_index) {
        m_first_selected_hint = 0;
        while ((size_t)item_index < m_item_data_cache.size()) {
            if (m_item_data_cache[item_index].selected) {
                m_first_selected_hint = item_index;
                break;
            }
            item_index++;
        }
    }
    AbstractView::remove_selection(item_data.index);
}

void IconView::set_selection(ModelIndex const& new_index)
{
    TemporaryChange change(m_changing_selection, true);
    do_clear_selection();
    auto item_index = model_index_to_item_index(new_index);
    auto& item_data = get_item_data(item_index);
    item_data.selected = true;
    m_selected_count_cache = 1;
    if (item_index < m_first_selected_hint)
        m_first_selected_hint = item_index;
    AbstractView::set_selection(new_index);
}

int IconView::items_per_page() const
{
    if (m_flow_direction == FlowDirection::LeftToRight)
        return (visible_content_rect().height() / effective_item_size().height()) * m_visual_column_count;
    return (visible_content_rect().width() / effective_item_size().width()) * m_visual_row_count;
}

void IconView::move_cursor(CursorMovement movement, SelectionUpdate selection_update)
{
    if (!model())
        return;
    auto& model = *this->model();

    if (!cursor_index().is_valid()) {
        set_cursor(model.index(0, model_column()), SelectionUpdate::Set);
        return;
    }

    auto new_row = cursor_index().row();

    switch (movement) {
    case CursorMovement::Right:
        if (m_flow_direction == FlowDirection::LeftToRight)
            new_row += 1;
        else
            new_row += m_visual_row_count;
        break;
    case CursorMovement::Left:
        if (m_flow_direction == FlowDirection::LeftToRight)
            new_row -= 1;
        else
            new_row -= m_visual_row_count;
        break;
    case CursorMovement::Up:
        if (m_flow_direction == FlowDirection::LeftToRight)
            new_row -= m_visual_column_count;
        else
            new_row -= 1;
        break;
    case CursorMovement::Down:
        if (m_flow_direction == FlowDirection::LeftToRight)
            new_row += m_visual_column_count;
        else
            new_row += 1;
        break;
    case CursorMovement::PageUp:
        new_row = max(0, cursor_index().row() - items_per_page());
        break;

    case CursorMovement::PageDown:
        new_row = min(model.row_count() - 1, cursor_index().row() + items_per_page());
        break;

    case CursorMovement::Home:
        new_row = 0;
        break;
    case CursorMovement::End:
        new_row = model.row_count() - 1;
        break;
    default:
        return;
    }
    auto new_index = model.index(new_row, cursor_index().column());
    if (new_index.is_valid())
        set_cursor(new_index, selection_update);
}

void IconView::set_flow_direction(FlowDirection flow_direction)
{
    if (m_flow_direction == flow_direction)
        return;
    m_flow_direction = flow_direction;
    m_item_data_cache.clear();
    m_item_data_cache_valid = false;
    update();
}

template<typename Function>
inline IterationDecision IconView::for_each_item_intersecting_rect(Gfx::IntRect const& rect, Function f) const
{
    VERIFY(model());
    if (rect.is_empty())
        return IterationDecision::Continue;
    int begin_row, begin_column;
    column_row_from_content_position(rect.top_left(), begin_row, begin_column);
    int end_row, end_column;
    column_row_from_content_position(rect.bottom_right().translated(-1), end_row, end_column);

    int items_per_flow_axis_step;
    int item_index;
    int last_index;
    if (m_flow_direction == FlowDirection::LeftToRight) {
        items_per_flow_axis_step = end_column - begin_column + 1;
        item_index = max(0, begin_row * m_visual_column_count + begin_column);
        last_index = min(item_count(), end_row * m_visual_column_count + end_column + 1);
    } else {
        items_per_flow_axis_step = end_row - begin_row + 1;
        item_index = max(0, begin_column * m_visual_row_count + begin_row);
        last_index = min(item_count(), end_column * m_visual_row_count + end_row + 1);
    }

    while (item_index < last_index) {
        for (int i = item_index; i < min(item_index + items_per_flow_axis_step, last_index); i++) {
            auto& item_data = get_item_data(i);
            if (item_data.is_intersecting(rect)) {
                auto decision = f(item_data);
                if (decision != IterationDecision::Continue)
                    return decision;
            }
        }
        item_index += (m_flow_direction == FlowDirection::LeftToRight) ? m_visual_column_count : m_visual_row_count;
    };

    return IterationDecision::Continue;
}

template<typename Function>
inline IterationDecision IconView::for_each_item_intersecting_rects(Vector<Gfx::IntRect> const& rects, Function f) const
{
    for (auto& rect : rects) {
        auto decision = for_each_item_intersecting_rect(rect, f);
        if (decision != IterationDecision::Continue)
            return decision;
    }
    return IterationDecision::Continue;
}
}
