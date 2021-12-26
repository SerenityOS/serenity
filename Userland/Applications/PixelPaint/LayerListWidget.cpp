/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LayerListWidget.h"
#include "Image.h"
#include "ImageEditor.h"
#include "Layer.h"
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(PixelPaint, LayerListWidget);

namespace PixelPaint {

LayerListWidget::LayerListWidget()
{
    set_should_hide_unnecessary_scrollbars(false);
    horizontal_scrollbar().set_visible(false);
}

LayerListWidget::~LayerListWidget()
{
    if (m_image)
        m_image->remove_client(*this);
}

void LayerListWidget::set_image(Image* image)
{
    if (m_image == image)
        return;
    if (m_image)
        m_image->remove_client(*this);
    m_image = image;
    if (m_image)
        m_image->add_client(*this);

    rebuild_gadgets();
}

void LayerListWidget::rebuild_gadgets()
{
    m_gadgets.clear();
    if (m_image) {
        for (size_t layer_index = 0; layer_index < m_image->layer_count(); ++layer_index) {
            m_gadgets.append({ layer_index, {}, {}, false, {} });
        }
    }
    relayout_gadgets();
}

void LayerListWidget::resize_event(GUI::ResizeEvent& event)
{
    AbstractScrollableWidget::resize_event(event);
    relayout_gadgets();
}

void LayerListWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.fill_rect(event.rect(), palette().button());

    if (!m_image)
        return;

    painter.fill_rect(event.rect(), palette().button());

    auto paint_gadget = [&](auto& gadget) {
        auto& layer = m_image->layer(gadget.layer_index);

        auto adjusted_rect = gadget.rect;
        adjusted_rect.translate_by(0, -vertical_scrollbar().value());
        adjusted_rect.translate_by(frame_thickness(), frame_thickness());

        if (gadget.is_moving) {
            adjusted_rect.translate_by(0, gadget.movement_delta.y());
        }

        if (gadget.is_moving) {
            painter.fill_rect(adjusted_rect, palette().selection().lightened(1.5f));
        } else if (layer.is_selected()) {
            painter.fill_rect(adjusted_rect, palette().selection());
        }

        painter.draw_rect(adjusted_rect, palette().color(ColorRole::BaseText));

        Gfx::IntRect thumbnail_rect { adjusted_rect.x(), adjusted_rect.y(), adjusted_rect.height(), adjusted_rect.height() };
        thumbnail_rect.shrink(8, 8);
        painter.draw_scaled_bitmap(thumbnail_rect, layer.bitmap(), layer.bitmap().rect());

        Gfx::IntRect text_rect { thumbnail_rect.right() + 10, adjusted_rect.y(), adjusted_rect.width(), adjusted_rect.height() };
        text_rect.intersect(adjusted_rect);

        if (layer.is_visible()) {
            painter.draw_text(text_rect, layer.name(), Gfx::TextAlignment::CenterLeft, layer.is_selected() ? palette().selection_text() : palette().button_text());
            painter.draw_rect(thumbnail_rect, palette().color(ColorRole::BaseText));
        } else {
            painter.draw_text(text_rect, layer.name(), Gfx::TextAlignment::CenterLeft, palette().color(ColorRole::DisabledText));
            painter.draw_rect(thumbnail_rect, palette().color(ColorRole::DisabledText));
        }
    };

    for (auto& gadget : m_gadgets) {
        if (!gadget.is_moving)
            paint_gadget(gadget);
    }

    if (m_moving_gadget_index.has_value())
        paint_gadget(m_gadgets[m_moving_gadget_index.value()]);

    Gfx::StylePainter::paint_frame(painter, rect(), palette(), Gfx::FrameShape::Box, Gfx::FrameShadow::Sunken, 2);
}

Optional<size_t> LayerListWidget::gadget_at(Gfx::IntPoint const& position)
{
    for (size_t i = 0; i < m_gadgets.size(); ++i) {
        if (m_gadgets[i].rect.contains(position))
            return i;
    }
    return {};
}

void LayerListWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (!m_image)
        return;
    if (event.button() != GUI::MouseButton::Left)
        return;

    Gfx::IntPoint translated_event_point = { 0, vertical_scrollbar().value() + event.y() };

    auto gadget_index = gadget_at(translated_event_point);
    if (!gadget_index.has_value())
        return;

    m_moving_gadget_index = gadget_index;
    m_selected_layer_index = gadget_index.value();
    m_moving_event_origin = translated_event_point;
    auto& gadget = m_gadgets[m_moving_gadget_index.value()];
    auto& layer = m_image->layer(gadget_index.value());
    set_selected_layer(&layer);
    gadget.is_moving = true;
    gadget.movement_delta = {};
    update();
}

void LayerListWidget::mousemove_event(GUI::MouseEvent& event)
{
    if (!m_image)
        return;
    if (!m_moving_gadget_index.has_value())
        return;

    Gfx::IntPoint translated_event_point = { 0, vertical_scrollbar().value() + event.y() };

    auto delta = translated_event_point - m_moving_event_origin;
    auto& gadget = m_gadgets[m_moving_gadget_index.value()];
    VERIFY(gadget.is_moving);
    gadget.movement_delta = delta;

    auto adjusted_rect = gadget.rect;
    adjusted_rect.translate_by(gadget.movement_delta);
    scroll_into_view(adjusted_rect, false, true);

    relayout_gadgets();
}

void LayerListWidget::mouseup_event(GUI::MouseEvent& event)
{
    if (!m_image)
        return;
    if (event.button() != GUI::MouseButton::Left)
        return;
    if (!m_moving_gadget_index.has_value())
        return;

    size_t old_index = m_moving_gadget_index.value();
    size_t new_index = hole_index_during_move();
    if (new_index >= m_image->layer_count())
        new_index = m_image->layer_count() - 1;

    m_moving_gadget_index = {};
    m_image->change_layer_index(old_index, new_index);
}

void LayerListWidget::context_menu_event(GUI::ContextMenuEvent& event)
{
    Gfx::IntPoint translated_event_point = { 0, vertical_scrollbar().value() + event.position().y() };

    auto gadget_index = gadget_at(translated_event_point);
    if (gadget_index.has_value()) {
        auto& layer = m_image->layer(gadget_index.value());
        m_selected_layer_index = gadget_index.value();
        set_selected_layer(&layer);
    }

    if (on_context_menu_request)
        on_context_menu_request(event);
}

void LayerListWidget::image_did_add_layer(size_t layer_index)
{
    if (m_moving_gadget_index.has_value()) {
        m_gadgets[m_moving_gadget_index.value()].is_moving = false;
        m_moving_gadget_index = {};
    }
    Gadget gadget { layer_index, {}, {}, false, {} };
    m_gadgets.insert(layer_index, move(gadget));
    relayout_gadgets();
}

void LayerListWidget::image_did_remove_layer(size_t layer_index)
{
    if (m_moving_gadget_index.has_value()) {
        m_gadgets[m_moving_gadget_index.value()].is_moving = false;
        m_moving_gadget_index = {};
    }
    m_gadgets.remove(layer_index);
    m_selected_layer_index = 0;
    relayout_gadgets();
}

void LayerListWidget::image_did_modify_layer(size_t layer_index)
{
    update(m_gadgets[layer_index].rect);
}

void LayerListWidget::image_did_modify_layer_stack()
{
    rebuild_gadgets();
}

static constexpr int gadget_height = 40;
static constexpr int gadget_spacing = -1;
static constexpr int vertical_step = gadget_height + gadget_spacing;

size_t LayerListWidget::hole_index_during_move() const
{
    VERIFY(is_moving_gadget());
    auto& moving_gadget = m_gadgets[m_moving_gadget_index.value()];
    int center_y_of_moving_gadget = moving_gadget.rect.translated(0, moving_gadget.movement_delta.y()).center().y();
    return center_y_of_moving_gadget / vertical_step;
}

void LayerListWidget::select_bottom_layer()
{
    if (!m_image || !m_image->layer_count())
        return;
    m_selected_layer_index = 0;
    set_selected_layer(&m_image->layer(0));
}

void LayerListWidget::select_top_layer()
{
    if (!m_image || !m_image->layer_count())
        return;
    m_selected_layer_index = m_image->layer_count() - 1;
    set_selected_layer(&m_image->layer(m_image->layer_count() - 1));
}

void LayerListWidget::cycle_through_selection(int delta)
{
    if (!m_image || !m_image->layer_count())
        return;

    int selected_layer_index = static_cast<int>(m_selected_layer_index);
    selected_layer_index += delta;

    if (selected_layer_index < 0)
        selected_layer_index = m_image->layer_count() - 1;
    if (selected_layer_index > static_cast<int>(m_image->layer_count()) - 1)
        selected_layer_index = 0;

    m_selected_layer_index = selected_layer_index;
    set_selected_layer(&m_image->layer(m_selected_layer_index));
}

void LayerListWidget::relayout_gadgets()
{
    int y = 0;

    Optional<size_t> hole_index;
    if (is_moving_gadget())
        hole_index = hole_index_during_move();

    size_t index = 0;
    for (auto& gadget : m_gadgets) {
        if (gadget.is_moving)
            continue;
        if (hole_index.has_value() && index == hole_index.value())
            y += vertical_step;
        gadget.rect = { 0, y, widget_inner_rect().width(), gadget_height };
        y += vertical_step;
        ++index;
    }

    auto total_gadget_height = static_cast<int>(m_gadgets.size()) * vertical_step;
    set_content_size({ widget_inner_rect().width(), total_gadget_height });
    vertical_scrollbar().set_range(0, max(total_gadget_height - height(), 0));
    update();
}

void LayerListWidget::set_selected_layer(Layer* layer)
{
    if (!m_image)
        return;
    for (size_t i = 0; i < m_image->layer_count(); ++i) {
        if (layer == &m_image->layer(i)) {
            m_image->layer(i).set_selected(true);
            scroll_into_view(m_gadgets[i].rect, false, true);
            m_selected_layer_index = i;
        } else {
            m_image->layer(i).set_selected(false);
        }
    }
    if (on_layer_select)
        on_layer_select(layer);

    update();
}

}
