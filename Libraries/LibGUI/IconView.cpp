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

#include <AK/StringBuilder.h>
#include <LibCore/Timer.h>
#include <LibGUI/DragOperation.h>
#include <LibGUI/IconView.h>
#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGfx/Palette.h>

//#define DRAGDROP_DEBUG

namespace GUI {

IconView::IconView()
{
    set_fill_with_background_color(true);
    set_background_role(ColorRole::Base);
    set_foreground_role(ColorRole::BaseText);
    horizontal_scrollbar().set_visible(false);
}

IconView::~IconView()
{
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

void IconView::scroll_into_view(const ModelIndex& index, bool scroll_horizontally, bool scroll_vertically)
{
    if (!index.is_valid())
        return;
    ScrollableWidget::scroll_into_view(item_rect(index.row()), scroll_horizontally, scroll_vertically);
}

void IconView::resize_event(ResizeEvent& event)
{
    AbstractView::resize_event(event);
    update_content_size();
}

void IconView::reinit_item_cache() const
{
    auto prev_item_count = m_item_data_cache.size();
    size_t new_item_count = item_count();
    auto items_to_invalidate = min(prev_item_count, new_item_count);

    // if the new number of items is less, check if any of the
    // ones not in the list anymore was selected
    for (size_t i = new_item_count; i < m_item_data_cache.size(); i++) {
        auto& item_data = m_item_data_cache[i];
        if (item_data.selected) {
            ASSERT(m_selected_count_cache > 0);
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
        reinit_item_cache();

    auto& item_data = m_item_data_cache[item_index];
    if (item_data.is_valid())
        return item_data;

    item_data.index = model()->index(item_index, model_column());
    item_data.data = item_data.index.data();
    get_item_rects(item_index, item_data, font_for_index(item_data.index));
    item_data.valid = true;
    return item_data;
}

auto IconView::item_data_from_content_position(const Gfx::IntPoint& content_position) const -> ItemData*
{
    if (!m_visual_row_count || !m_visual_column_count)
        return nullptr;
    int row, column;
    column_row_from_content_position(content_position, row, column);
    int item_index = row * m_visual_column_count + column;
    if (item_index < 0 || item_index >= item_count())
        return nullptr;
    return &get_item_data(item_index);
}

void IconView::did_update_model(unsigned flags)
{
    AbstractView::did_update_model(flags);
    if (!model() || (flags & GUI::Model::InvalidateAllIndexes)) {
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

    m_visual_column_count = max(1, available_size().width() / effective_item_size().width());
    if (m_visual_column_count)
        m_visual_row_count = ceil_div(model()->row_count(), m_visual_column_count);
    else
        m_visual_row_count = 0;

    int content_width = available_size().width();
    int content_height = m_visual_row_count * effective_item_size().height();

    set_content_size({ content_width, content_height });

    if (!m_item_data_cache_valid)
        reinit_item_cache();

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
    int visual_row_index = item_index / m_visual_column_count;
    int visual_column_index = item_index % m_visual_column_count;
    return {
        visual_column_index * effective_item_size().width(),
        visual_row_index * effective_item_size().height(),
        effective_item_size().width(),
        effective_item_size().height()
    };
}

ModelIndex IconView::index_at_event_position(const Gfx::IntPoint& position) const
{
    ASSERT(model());
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

    if (event.button() != MouseButton::Left)
        return AbstractView::mousedown_event(event);

    auto index = index_at_event_position(event.position());
    if (index.is_valid()) {
        // We might start dragging this item, but not rubber-banding.
        return AbstractView::mousedown_event(event);
    }

    if (event.modifiers() & Mod_Ctrl) {
        m_rubber_banding_store_selection = true;
    } else {
        clear_selection();
        m_rubber_banding_store_selection = false;
    }

    auto adjusted_position = to_content_position(event.position());

    m_might_drag = false;
    if (is_multi_select()) {
        m_rubber_banding = true;
        m_rubber_band_origin = adjusted_position;
        m_rubber_band_current = adjusted_position;
    }
}

void IconView::mouseup_event(MouseEvent& event)
{
    if (m_rubber_banding && event.button() == MouseButton::Left) {
        m_rubber_banding = false;
        if (m_out_of_view_timer)
            m_out_of_view_timer->stop();
        update();
    }
    AbstractView::mouseup_event(event);
}

void IconView::drag_move_event(DragEvent& event)
{
    auto index = index_at_event_position(event.position());
    ModelIndex new_drop_candidate_index;
    if (index.is_valid()) {
        bool acceptable = model()->accepts_drag(index, event.data_type());
#ifdef DRAGDROP_DEBUG
        dbg() << "Drag of type '" << event.data_type() << "' moving over " << index << ", acceptable: " << acceptable;
#endif
        if (acceptable)
            new_drop_candidate_index = index;
    }
    if (m_drop_candidate_index != new_drop_candidate_index) {
        m_drop_candidate_index = new_drop_candidate_index;
        update();
    }
    event.accept();
}

bool IconView::update_rubber_banding(const Gfx::IntPoint& position)
{
    auto adjusted_position = to_content_position(position);
    if (m_rubber_band_current != adjusted_position) {
        auto prev_rect = Gfx::IntRect::from_two_points(m_rubber_band_origin, m_rubber_band_current);
        m_rubber_band_current = adjusted_position;
        auto rubber_band_rect = Gfx::IntRect::from_two_points(m_rubber_band_origin, m_rubber_band_current);

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

        // Now toggle all items that are no longer in the selected area, once only
        for_each_item_intersecting_rects(deselect_area, [&](ItemData& item_data) -> IterationDecision {
            if (!item_data.selection_toggled && item_data.is_intersecting(prev_rect) && !item_data.is_intersecting(rubber_band_rect)) {
                item_data.selection_toggled = true;
                toggle_selection(item_data);
            }
            return IterationDecision::Continue;
        });
        // Now toggle all items that are in the new selected area, once only
        for_each_item_intersecting_rects(select_area, [&](ItemData& item_data) -> IterationDecision {
            if (!item_data.selection_toggled && !item_data.is_intersecting(prev_rect) && item_data.is_intersecting(rubber_band_rect)) {
                item_data.selection_toggled = true;
                toggle_selection(item_data);
            }
            return IterationDecision::Continue;
        });

        update();
        return true;
    }
    return false;
}

#define SCROLL_OUT_OF_VIEW_HOT_MARGIN 20

void IconView::mousemove_event(MouseEvent& event)
{
    if (!model())
        return AbstractView::mousemove_event(event);

    if (m_rubber_banding) {
        auto in_view_rect = widget_inner_rect();
        in_view_rect.shrink(SCROLL_OUT_OF_VIEW_HOT_MARGIN, SCROLL_OUT_OF_VIEW_HOT_MARGIN);
        if (!in_view_rect.contains(event.position())) {
            if (!m_out_of_view_timer) {
                m_out_of_view_timer = add<Core::Timer>();
                m_out_of_view_timer->set_interval(100);
                m_out_of_view_timer->on_timeout = [this] {
                    scroll_out_of_view_timer_fired();
                };
            }

            m_out_of_view_position = event.position();
            if (!m_out_of_view_timer->is_active())
                m_out_of_view_timer->start();
        } else {
            if (m_out_of_view_timer)
                m_out_of_view_timer->stop();
        }
        if (update_rubber_banding(event.position()))
            return;
    }

    AbstractView::mousemove_event(event);
}

void IconView::scroll_out_of_view_timer_fired()
{
    auto scroll_to = to_content_position(m_out_of_view_position);
    // Adjust the scroll-to position by SCROLL_OUT_OF_VIEW_HOT_MARGIN / 2
    // depending on which direction we're scrolling. This allows us to
    // start scrolling before we actually leave the visible area, which
    // is important when there is no space to further move the mouse. The
    // speed of scrolling is determined by the distance between the mouse
    // pointer and the widget's inner rect shrunken by the hot margin
    auto in_view_rect = widget_inner_rect().shrunken(SCROLL_OUT_OF_VIEW_HOT_MARGIN, SCROLL_OUT_OF_VIEW_HOT_MARGIN);
    int adjust_x = 0, adjust_y = 0;
    if (m_out_of_view_position.y() > in_view_rect.bottom())
        adjust_y = (SCROLL_OUT_OF_VIEW_HOT_MARGIN / 2) + min(SCROLL_OUT_OF_VIEW_HOT_MARGIN, m_out_of_view_position.y() - in_view_rect.bottom());
    else if (m_out_of_view_position.y() < in_view_rect.top())
        adjust_y = -(SCROLL_OUT_OF_VIEW_HOT_MARGIN / 2) + max(-SCROLL_OUT_OF_VIEW_HOT_MARGIN, m_out_of_view_position.y() - in_view_rect.top());
    if (m_out_of_view_position.x() > in_view_rect.right())
        adjust_x = (SCROLL_OUT_OF_VIEW_HOT_MARGIN / 2) + min(SCROLL_OUT_OF_VIEW_HOT_MARGIN, m_out_of_view_position.x() - in_view_rect.right());
    else if (m_out_of_view_position.x() < in_view_rect.left())
        adjust_x = -(SCROLL_OUT_OF_VIEW_HOT_MARGIN / 2) + max(-SCROLL_OUT_OF_VIEW_HOT_MARGIN, m_out_of_view_position.x() - in_view_rect.left());

    ScrollableWidget::scroll_into_view({ scroll_to.translated(adjust_x, adjust_y), { 1, 1 } }, true, true);
    update_rubber_banding(m_out_of_view_position);
}

void IconView::update_item_rects(int item_index, ItemData& item_data) const
{
    auto item_rect = this->item_rect(item_index);
    item_data.icon_rect.center_within(item_rect);
    item_data.icon_rect.move_by(0, item_data.icon_offset_y);
    item_data.text_rect.center_horizontally_within(item_rect);
    item_data.text_rect.set_top(item_rect.y() + item_data.text_offset_y);
}

Gfx::IntRect IconView::content_rect(const ModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto& item_data = get_item_data(index.row());
    return item_data.text_rect;
}

void IconView::get_item_rects(int item_index, ItemData& item_data, const Gfx::Font& font) const
{
    auto item_rect = this->item_rect(item_index);
    item_data.icon_rect = { 0, 0, 32, 32 };
    item_data.icon_rect.center_within(item_rect);
    item_data.icon_offset_y = -font.glyph_height() - 6;
    item_data.icon_rect.move_by(0, item_data.icon_offset_y);
    item_data.text_rect = { 0, item_data.icon_rect.bottom() + 6 + 1, font.width(item_data.data.to_string()), font.glyph_height() };
    item_data.text_rect.center_horizontally_within(item_rect);
    item_data.text_rect.inflate(6, 4);
    item_data.text_rect.intersect(item_rect);
    item_data.text_offset_y = item_data.text_rect.y() - item_rect.y();
}

void IconView::second_paint_event(PaintEvent& event)
{
    if (!m_rubber_banding)
        return;

    Painter painter(*this);
    painter.add_clip_rect(event.rect());
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

    if (fill_with_background_color())
        painter.fill_rect(event.rect(), widget_background_color);
    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    auto translation = painter.translation().translated(-relative_position().x(), -relative_position().y());
    for_each_item_intersecting_rect(painter.clip_rect().translated(-translation.x(), -translation.y()), [&](auto& item_data) -> IterationDecision {
        Color background_color;
        if (item_data.selected) {
            background_color = is_focused() ? palette().selection() : palette().inactive_selection();
        } else {
            background_color = widget_background_color;
        }

        auto icon = item_data.index.data(ModelRole::Icon);
        auto item_text = item_data.index.data();

        if (icon.is_icon()) {
            if (auto bitmap = icon.as_icon().bitmap_for_size(item_data.icon_rect.width())) {
                Gfx::IntRect destination = bitmap->rect();
                destination.center_within(item_data.icon_rect);

                if (m_hovered_index.is_valid() && m_hovered_index == item_data.index) {
                    painter.blit_brightened(destination.location(), *bitmap, bitmap->rect());
                } else {
                    painter.blit(destination.location(), *bitmap, bitmap->rect());
                }
            }
        }

        Color text_color;
        if (item_data.selected)
            text_color = is_focused() ? palette().selection_text() : palette().inactive_selection_text();
        else
            text_color = item_data.index.data(ModelRole::ForegroundColor).to_color(palette().color(foreground_role()));
        painter.fill_rect(item_data.text_rect, background_color);
        painter.draw_text(item_data.text_rect, item_text.to_string(), font_for_index(item_data.index), Gfx::TextAlignment::Center, text_color, Gfx::TextElision::Right);

        if (item_data.index == m_drop_candidate_index) {
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
    selection().for_each_index([&](const ModelIndex& index) {
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
    ASSERT(m_selected_count_cache == 0);
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

void IconView::add_selection(const ModelIndex& new_index)
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
        remove_selection(item_data);
}

void IconView::toggle_selection(const ModelIndex& new_index)
{
    TemporaryChange change(m_changing_selection, true);
    auto item_index = model_index_to_item_index(new_index);
    toggle_selection(get_item_data(item_index));
}

void IconView::remove_selection(ItemData& item_data)
{
    if (!item_data.selected)
        return;

    TemporaryChange change(m_changing_selection, true);
    item_data.selected = false;
    ASSERT(m_selected_count_cache > 0);
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

void IconView::set_selection(const ModelIndex& new_index)
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

void IconView::move_cursor(CursorMovement movement, SelectionUpdate selection_update)
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
    case CursorMovement::Right:
        new_index = model.index(cursor_index().row() + 1, cursor_index().column());
        break;
    case CursorMovement::Left:
        new_index = model.index(cursor_index().row() - 1, cursor_index().column());
        break;
    case CursorMovement::Up:
        new_index = model.index(cursor_index().row() - m_visual_column_count, cursor_index().column());
        break;
    case CursorMovement::Down:
        new_index = model.index(cursor_index().row() + m_visual_column_count, cursor_index().column());
        break;
    case CursorMovement::PageUp: {
        int items_per_page = (visible_content_rect().height() / effective_item_size().height()) * m_visual_column_count;
        new_index = model.index(max(0, cursor_index().row() - items_per_page), cursor_index().column());
        break;
    }
    case CursorMovement::PageDown: {
        int items_per_page = (visible_content_rect().height() / effective_item_size().height()) * m_visual_column_count;
        new_index = model.index(min(model.row_count() - 1, cursor_index().row() + items_per_page), cursor_index().column());
        break;
    }
    case CursorMovement::Home:
        new_index = model.index(0, 0);
        break;
    case CursorMovement::End:
        new_index = model.index(model.row_count() - 1, 0);
        break;
    default:
        break;
    }
    if (new_index.is_valid())
        set_cursor(new_index, selection_update);
}

}
