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
#include <Kernel/KeyCode.h>
#include <LibGfx/Palette.h>
#include <LibGUI/DragOperation.h>
#include <LibGUI/ItemView.h>
#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>

namespace GUI {

ItemView::ItemView(Widget* parent)
    : AbstractView(parent)
{
    set_background_role(ColorRole::Base);
    set_foreground_role(ColorRole::BaseText);
    set_frame_shape(Gfx::FrameShape::Container);
    set_frame_shadow(Gfx::FrameShadow::Sunken);
    set_frame_thickness(2);
    horizontal_scrollbar().set_visible(false);
}

ItemView::~ItemView()
{
}

void ItemView::scroll_into_view(const ModelIndex& index, Orientation orientation)
{
    ScrollableWidget::scroll_into_view(item_rect(index.row()), orientation);
}

void ItemView::resize_event(ResizeEvent& event)
{
    AbstractView::resize_event(event);
    update_content_size();
}

void ItemView::did_update_model()
{
    AbstractView::did_update_model();
    update_content_size();
    update();
}

void ItemView::update_content_size()
{
    if (!model())
        return set_content_size({});

    m_visual_column_count = available_size().width() / effective_item_size().width();
    if (m_visual_column_count)
        m_visual_row_count = ceil_div(model()->row_count(), m_visual_column_count);
    else
        m_visual_row_count = 0;

    int content_width = available_size().width();
    int content_height = m_visual_row_count * effective_item_size().height();

    set_content_size({ content_width, content_height });
}

Gfx::Rect ItemView::item_rect(int item_index) const
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

Vector<int> ItemView::items_intersecting_rect(const Gfx::Rect& rect) const
{
    ASSERT(model());
    const auto& column_metadata = model()->column_metadata(model_column());
    const auto& font = column_metadata.font ? *column_metadata.font : this->font();
    Vector<int> item_indexes;
    for (int item_index = 0; item_index < item_count(); ++item_index) {
        Gfx::Rect item_rect;
        Gfx::Rect icon_rect;
        Gfx::Rect text_rect;
        auto item_text = model()->data(model()->index(item_index, model_column()));
        get_item_rects(item_index, font, item_text, item_rect, icon_rect, text_rect);
        if (icon_rect.intersects(rect) || text_rect.intersects(rect))
            item_indexes.append(item_index);
    }
    return item_indexes;
}

ModelIndex ItemView::index_at_event_position(const Gfx::Point& position) const
{
    ASSERT(model());
    // FIXME: Since all items are the same size, just compute the clicked item index
    //        instead of iterating over everything.
    auto adjusted_position = position.translated(0, vertical_scrollbar().value());
    const auto& column_metadata = model()->column_metadata(model_column());
    const auto& font = column_metadata.font ? *column_metadata.font : this->font();
    for (int item_index = 0; item_index < item_count(); ++item_index) {
        Gfx::Rect item_rect;
        Gfx::Rect icon_rect;
        Gfx::Rect text_rect;
        auto index = model()->index(item_index, model_column());
        auto item_text = model()->data(index);
        get_item_rects(item_index, font, item_text, item_rect, icon_rect, text_rect);
        if (icon_rect.contains(adjusted_position) || text_rect.contains(adjusted_position))
            return index;
    }
    return {};
}

void ItemView::mousedown_event(MouseEvent& event)
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

    ASSERT(m_rubber_band_remembered_selection.is_empty());

    if (event.modifiers() & Mod_Ctrl) {
        selection().for_each_index([&](auto& index) {
            m_rubber_band_remembered_selection.append(index);
        });
    } else {
        selection().clear();
    }

    m_might_drag = false;
    m_rubber_banding = true;
    m_rubber_band_origin = event.position();
    m_rubber_band_current = event.position();
}

void ItemView::mouseup_event(MouseEvent& event)
{
    if (m_rubber_banding && event.button() == MouseButton::Left) {
        m_rubber_banding = false;
        m_rubber_band_remembered_selection.clear();
        update();
    }
    AbstractView::mouseup_event(event);
}

void ItemView::mousemove_event(MouseEvent& event)
{
    if (!model())
        return AbstractView::mousemove_event(event);

    if (m_rubber_banding) {
        if (m_rubber_band_current != event.position()) {
            m_rubber_band_current = event.position();
            auto rubber_band_rect = Gfx::Rect::from_two_points(m_rubber_band_origin, m_rubber_band_current);
            selection().clear();
            for (auto item_index : items_intersecting_rect(rubber_band_rect)) {
                selection().add(model()->index(item_index, model_column()));
            }
            if (event.modifiers() & Mod_Ctrl) {
                for (auto stored_item : m_rubber_band_remembered_selection) {
                    selection().add(stored_item);
                }
            }
            update();
            return;
        }
    }

    AbstractView::mousemove_event(event);
}

void ItemView::get_item_rects(int item_index, const Gfx::Font& font, const Variant& item_text, Gfx::Rect& item_rect, Gfx::Rect& icon_rect, Gfx::Rect& text_rect) const
{
    item_rect = this->item_rect(item_index);
    icon_rect = { 0, 0, 32, 32 };
    icon_rect.center_within(item_rect);
    icon_rect.move_by(0, -font.glyph_height() - 6);
    text_rect = { 0, icon_rect.bottom() + 6 + 1, font.width(item_text.to_string()), font.glyph_height() };
    text_rect.center_horizontally_within(item_rect);
    text_rect.inflate(6, 4);
    text_rect.intersect(item_rect);
}

void ItemView::second_paint_event(PaintEvent& event)
{
    if (!m_rubber_banding)
        return;

    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    auto rubber_band_rect = Gfx::Rect::from_two_points(m_rubber_band_origin, m_rubber_band_current);
    painter.fill_rect(rubber_band_rect, parent_widget()->palette().rubber_band_fill());
    painter.draw_rect(rubber_band_rect, parent_widget()->palette().rubber_band_border());
}

void ItemView::paint_event(PaintEvent& event)
{
    Color widget_background_color = palette().color(background_role());
    Frame::paint_event(event);

    Painter painter(*this);
    painter.add_clip_rect(widget_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), widget_background_color);
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    auto column_metadata = model()->column_metadata(m_model_column);
    const Gfx::Font& font = column_metadata.font ? *column_metadata.font : this->font();

    for (int item_index = 0; item_index < model()->row_count(); ++item_index) {
        auto model_index = model()->index(item_index, m_model_column);
        bool is_selected_item = selection().contains(model_index);
        Color background_color;
        if (is_selected_item) {
            background_color = is_focused() ? palette().selection() : Color::from_rgb(0x606060);
        } else {
            background_color = widget_background_color;
        }

        auto icon = model()->data(model_index, Model::Role::Icon);
        auto item_text = model()->data(model_index, Model::Role::Display);

        Gfx::Rect item_rect;
        Gfx::Rect icon_rect;
        Gfx::Rect text_rect;
        get_item_rects(item_index, font, item_text, item_rect, icon_rect, text_rect);

        if (icon.is_icon()) {
            if (auto bitmap = icon.as_icon().bitmap_for_size(icon_rect.width()))
                painter.draw_scaled_bitmap(icon_rect, *bitmap, bitmap->rect());
        }

        Color text_color;
        if (is_selected_item)
            text_color = palette().selection_text();
        else
            text_color = model()->data(model_index, Model::Role::ForegroundColor).to_color(palette().color(foreground_role()));
        painter.fill_rect(text_rect, background_color);
        painter.draw_text(text_rect, item_text.to_string(), font, Gfx::TextAlignment::Center, text_color, Gfx::TextElision::Right);
    };
}

int ItemView::item_count() const
{
    if (!model())
        return 0;
    return model()->row_count();
}

void ItemView::keydown_event(KeyEvent& event)
{
    if (!model())
        return;
    if (!m_visual_row_count || !m_visual_column_count)
        return;

    auto& model = *this->model();
    if (event.key() == KeyCode::Key_Return) {
        activate_selected();
        return;
    }
    if (event.key() == KeyCode::Key_Home) {
        auto new_index = model.index(0, 0);
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_End) {
        auto new_index = model.index(model.row_count() - 1, 0);
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_Up) {
        ModelIndex new_index;
        if (!selection().is_empty()) {
            auto old_index = selection().first();
            new_index = model.index(old_index.row() - m_visual_column_count, old_index.column());
        } else {
            new_index = model.index(0, 0);
        }
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_Down) {
        ModelIndex new_index;
        if (!selection().is_empty()) {
            auto old_index = selection().first();
            new_index = model.index(old_index.row() + m_visual_column_count, old_index.column());
        } else {
            new_index = model.index(0, 0);
        }
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_Left) {
        ModelIndex new_index;
        if (!selection().is_empty()) {
            auto old_index = selection().first();
            new_index = model.index(old_index.row() - 1, old_index.column());
        } else {
            new_index = model.index(0, 0);
        }
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_Right) {
        ModelIndex new_index;
        if (!selection().is_empty()) {
            auto old_index = selection().first();
            new_index = model.index(old_index.row() + 1, old_index.column());
        } else {
            new_index = model.index(0, 0);
        }
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_PageUp) {
        int items_per_page = (visible_content_rect().height() / effective_item_size().height()) * m_visual_column_count;
        auto old_index = selection().first();
        auto new_index = model.index(max(0, old_index.row() - items_per_page), old_index.column());
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_PageDown) {
        int items_per_page = (visible_content_rect().height() / effective_item_size().height()) * m_visual_column_count;
        auto old_index = selection().first();
        auto new_index = model.index(min(model.row_count() - 1, old_index.row() + items_per_page), old_index.column());
        if (model.is_valid(new_index)) {
            selection().set(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    return Widget::keydown_event(event);
}

}
