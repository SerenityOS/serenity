/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
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

size_t LayerListWidget::to_gadget_index(size_t layer_index) const
{
    return m_image->layer_count() - layer_index - 1;
}

size_t LayerListWidget::to_layer_index(size_t gadget_index) const
{
    return m_image->layer_count() - gadget_index - 1;
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
        for (int layer_index = m_image->layer_count() - 1; layer_index >= 0; --layer_index) {
            m_gadgets.append({ static_cast<size_t>(layer_index), {}, false, {} });
        }
    }
    relayout_gadgets();
}

void LayerListWidget::resize_event(GUI::ResizeEvent& event)
{
    AbstractScrollableWidget::resize_event(event);
    relayout_gadgets();
}

void LayerListWidget::get_gadget_rects(Gadget const& gadget, bool is_masked, Gfx::IntRect& outer_rect, Gfx::IntRect& outer_thumbnail_rect, Gfx::IntRect& inner_thumbnail_rect, Gfx::IntRect& outer_mask_thumbnail_rect, Gfx::IntRect& inner_mask_thumbnail_rect, Gfx::IntRect& text_rect)
{
    outer_rect = gadget.rect;
    outer_rect.translate_by(0, -vertical_scrollbar().value());
    outer_rect.translate_by(frame_thickness(), frame_thickness());
    if (gadget.is_moving) {
        outer_rect.translate_by(0, gadget.movement_delta.y());
    }

    auto const& layer = m_image->layer(gadget.layer_index);

    outer_thumbnail_rect = { outer_rect.x(), outer_rect.y(), outer_rect.height(), outer_rect.height() };
    outer_thumbnail_rect.shrink(8, 8);

    Gfx::IntSize thumbnail_size;
    if (layer.size().width() > layer.size().height()) {
        float ratio = static_cast<float>(layer.size().height()) / static_cast<float>(layer.size().width());
        thumbnail_size.set_width(outer_thumbnail_rect.width());
        thumbnail_size.set_height(outer_thumbnail_rect.width() * ratio);
    } else {
        float ratio = static_cast<float>(layer.size().width()) / static_cast<float>(layer.size().height());
        thumbnail_size.set_height(outer_thumbnail_rect.height());
        thumbnail_size.set_width(outer_thumbnail_rect.height() * ratio);
    }

    inner_thumbnail_rect = { 0, 0, thumbnail_size.width(), thumbnail_size.height() };
    inner_thumbnail_rect.center_within(outer_thumbnail_rect);

    if (is_masked) {
        outer_mask_thumbnail_rect = { outer_thumbnail_rect.right() + 4, outer_thumbnail_rect.y(), outer_thumbnail_rect.width(), outer_thumbnail_rect.height() };
        inner_mask_thumbnail_rect = { 0, 0, thumbnail_size.width(), thumbnail_size.height() };
        inner_mask_thumbnail_rect.center_within(outer_mask_thumbnail_rect);
    } else {
        outer_mask_thumbnail_rect = outer_thumbnail_rect;
        inner_mask_thumbnail_rect = inner_thumbnail_rect;
    }

    text_rect = { outer_mask_thumbnail_rect.right() + 9, outer_rect.y(), outer_rect.width(), outer_rect.height() };
    text_rect.intersect(outer_rect);
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

        auto is_masked = layer.is_masked();

        Gfx::IntRect adjusted_rect;
        Gfx::IntRect outer_thumbnail_rect;
        Gfx::IntRect inner_thumbnail_rect;
        Gfx::IntRect outer_mask_thumbnail_rect;
        Gfx::IntRect inner_mask_thumbnail_rect;
        Gfx::IntRect text_rect;
        get_gadget_rects(gadget, is_masked, adjusted_rect, outer_thumbnail_rect, inner_thumbnail_rect, outer_mask_thumbnail_rect, inner_mask_thumbnail_rect, text_rect);

        if (gadget.is_moving) {
            painter.fill_rect(adjusted_rect, palette().selection().lightened(1.5f));
        } else if (layer.is_selected()) {
            painter.fill_rect(adjusted_rect, palette().selection());
        }

        painter.draw_rect(adjusted_rect, palette().color(ColorRole::BaseText));
        painter.draw_scaled_bitmap(inner_thumbnail_rect, layer.display_bitmap(), layer.display_bitmap().rect(), 1.f, Gfx::ScalingMode::BoxSampling);

        if (is_masked)
            painter.draw_scaled_bitmap(inner_mask_thumbnail_rect, *layer.mask_bitmap(), layer.mask_bitmap()->rect(), 1.f, Gfx::ScalingMode::BoxSampling);

        Color border_color = layer.is_visible() ? palette().color(ColorRole::BaseText) : palette().color(ColorRole::DisabledText);

        // FIXME: This needs cleaning up
        if (layer.is_visible()) {
            painter.draw_text(text_rect, layer.name(), Gfx::TextAlignment::CenterLeft, layer.is_selected() ? palette().selection_text() : palette().button_text());
            switch (layer.edit_mode()) {
            case Layer::EditMode::Content:
                if (is_masked) {
                    painter.draw_rect_with_thickness(inner_thumbnail_rect.inflated(4, 4), Color::Yellow, 2);
                    painter.draw_rect(inner_mask_thumbnail_rect, border_color);
                } else {
                    painter.draw_rect(inner_thumbnail_rect, border_color);
                }
                break;
            case Layer::EditMode::Mask:
                painter.draw_rect(inner_thumbnail_rect, border_color);
                if (is_masked)
                    painter.draw_rect_with_thickness(inner_mask_thumbnail_rect.inflated(4, 4), Color::Yellow, 2);
                break;
            }
        } else {
            painter.draw_text(text_rect, layer.name(), Gfx::TextAlignment::CenterLeft, palette().color(ColorRole::DisabledText));
            painter.draw_rect(inner_thumbnail_rect, border_color);
            if (is_masked)
                painter.draw_rect(inner_mask_thumbnail_rect, border_color);
        }
    };

    for (auto& gadget : m_gadgets) {
        if (!gadget.is_moving)
            paint_gadget(gadget);
    }

    if (m_moving_gadget_index.has_value())
        paint_gadget(m_gadgets[m_moving_gadget_index.value()]);

    Gfx::StylePainter::paint_frame(painter, rect(), palette(), Gfx::FrameStyle::SunkenBox);
}

Optional<size_t> LayerListWidget::gadget_at(Gfx::IntPoint position)
{
    for (size_t i = 0; i < m_gadgets.size(); ++i) {
        if (m_gadgets[i].rect.contains(position))
            return i;
    }
    return {};
}

void LayerListWidget::doubleclick_event(GUI::MouseEvent& event)
{
    if (!m_image)
        return;
    if (event.button() != GUI::MouseButton::Primary)
        return;

    Gfx::IntPoint translated_event_point = { 0, vertical_scrollbar().value() + event.y() };

    auto maybe_gadget_index = gadget_at(translated_event_point);
    if (!maybe_gadget_index.has_value())
        return;
    auto gadget_index = maybe_gadget_index.value();

    // FIXME: Allow for a double click to change the selected gadget
    if (m_selected_gadget_index != gadget_index)
        return;

    auto& gadget = m_gadgets[gadget_index];
    auto& layer = m_image->layer(to_layer_index(gadget_index));

    auto is_masked = layer.is_masked();

    if (!is_masked)
        return;

    Gfx::IntRect adjusted_rect;
    Gfx::IntRect outer_thumbnail_rect;
    Gfx::IntRect inner_thumbnail_rect;
    Gfx::IntRect outer_mask_thumbnail_rect;
    Gfx::IntRect inner_mask_thumbnail_rect;
    Gfx::IntRect text_rect;
    get_gadget_rects(gadget, is_masked, adjusted_rect, outer_thumbnail_rect, inner_thumbnail_rect, outer_mask_thumbnail_rect, inner_mask_thumbnail_rect, text_rect);

    if (outer_thumbnail_rect.contains(event.position()))
        layer.set_edit_mode(Layer::EditMode::Content);
    else if (outer_mask_thumbnail_rect.contains(event.position()))
        layer.set_edit_mode(Layer::EditMode::Mask);

    update();
}

void LayerListWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (!m_image)
        return;
    if (event.button() != GUI::MouseButton::Primary)
        return;

    Gfx::IntPoint translated_event_point = { 0, vertical_scrollbar().value() + event.y() };

    auto maybe_gadget_index = gadget_at(translated_event_point);
    if (!maybe_gadget_index.has_value())
        return;
    auto gadget_index = maybe_gadget_index.value();

    m_moving_gadget_index = gadget_index;
    m_selected_gadget_index = gadget_index;
    m_moving_event_origin = translated_event_point;
    auto& gadget = m_gadgets[m_moving_gadget_index.value()];
    auto& layer = m_image->layer(to_layer_index(gadget_index));
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

    if (delta.y() == 0)
        return;

    auto& gadget = m_gadgets[m_moving_gadget_index.value()];
    VERIFY(gadget.is_moving);

    gadget.movement_delta.set_y(delta.y());
    auto inner_rect_max_height = widget_inner_rect().height() - 1 + vertical_scrollbar().max();

    if (delta.y() < 0 && gadget.rect.y() < -delta.y())
        gadget.movement_delta.set_y(-gadget.rect.y());
    else if (delta.y() > 0 && gadget.rect.bottom() + delta.y() > inner_rect_max_height)
        gadget.movement_delta.set_y(inner_rect_max_height - gadget.rect.bottom());

    m_automatic_scroll_delta = automatic_scroll_delta_from_position(event.position());
    set_automatic_scrolling_timer_active(vertical_scrollbar().is_scrollable() && !m_automatic_scroll_delta.is_zero());

    relayout_gadgets();
}

void LayerListWidget::mouseup_event(GUI::MouseEvent& event)
{
    if (!m_image)
        return;
    if (event.button() != GUI::MouseButton::Primary)
        return;
    if (!m_moving_gadget_index.has_value())
        return;

    size_t old_index = m_moving_gadget_index.value();
    size_t new_index = hole_index_during_move();
    if (new_index >= m_image->layer_count())
        new_index = m_image->layer_count() - 1;

    m_moving_gadget_index = {};
    set_automatic_scrolling_timer_active(false);

    auto old_layer_index = to_layer_index(old_index);
    auto new_layer_index = to_layer_index(new_index);
    m_image->change_layer_index(old_layer_index, new_layer_index);
}

void LayerListWidget::context_menu_event(GUI::ContextMenuEvent& event)
{
    Gfx::IntPoint translated_event_point = { 0, vertical_scrollbar().value() + event.position().y() };

    auto gadget_index = gadget_at(translated_event_point);
    if (gadget_index.has_value()) {
        m_selected_gadget_index = gadget_index.value();
        auto& layer = m_image->layer(to_layer_index(m_selected_gadget_index));
        set_selected_layer(&layer);
    }

    if (on_context_menu_request)
        on_context_menu_request(event);
}

void LayerListWidget::automatic_scrolling_timer_did_fire()
{
    auto& gadget = m_gadgets[m_moving_gadget_index.value()];
    VERIFY(gadget.is_moving);

    if (m_automatic_scroll_delta.y() == 0)
        return;

    if (vertical_scrollbar().is_min() && m_automatic_scroll_delta.y() < 0)
        return;

    if (vertical_scrollbar().is_max() && m_automatic_scroll_delta.y() > 0)
        return;

    vertical_scrollbar().increase_slider_by(m_automatic_scroll_delta.y());
    gadget.movement_delta.set_y(gadget.movement_delta.y() + m_automatic_scroll_delta.y());

    auto inner_rect_max_height = widget_inner_rect().height() - 1 + vertical_scrollbar().max();
    auto gadget_absolute_position = gadget.rect.y() + gadget.movement_delta.y();

    if (gadget_absolute_position < 0)
        gadget.movement_delta.set_y(-gadget.rect.y());
    else if (gadget_absolute_position + gadget.rect.height() >= inner_rect_max_height - 1)
        gadget.movement_delta.set_y(inner_rect_max_height - gadget.rect.bottom());
    else
        relayout_gadgets();
}

void LayerListWidget::image_did_add_layer(size_t layer_index)
{
    if (m_moving_gadget_index.has_value()) {
        m_gadgets[m_moving_gadget_index.value()].is_moving = false;
        m_moving_gadget_index = {};
    }
    auto gadget_index = to_gadget_index(layer_index);
    Gadget gadget { gadget_index, {}, false, {} };
    m_gadgets.insert(gadget_index, gadget);
    relayout_gadgets();
}

void LayerListWidget::image_did_remove_layer(size_t layer_index)
{
    if (m_moving_gadget_index.has_value()) {
        m_gadgets[m_moving_gadget_index.value()].is_moving = false;
        m_moving_gadget_index = {};
    }
    // No -1 here since a layer has already been removed.
    auto gadget_index = m_image->layer_count() - layer_index;
    m_gadgets.remove(gadget_index);
    m_selected_gadget_index = to_gadget_index(0);
    relayout_gadgets();
}

void LayerListWidget::image_did_modify_layer_properties(size_t layer_index)
{
    update(m_gadgets[to_gadget_index(layer_index)].rect);
}

void LayerListWidget::image_did_modify_layer_bitmap(size_t layer_index)
{
    Gfx::IntRect adjusted_rect;
    Gfx::IntRect outer_thumbnail_rect;
    Gfx::IntRect inner_thumbnail_rect;
    Gfx::IntRect outer_mask_thumbnail_rect;
    Gfx::IntRect inner_mask_thumbnail_rect;
    Gfx::IntRect text_rect;

    auto is_masked = m_image->layer(layer_index).is_masked();

    get_gadget_rects(m_gadgets[to_gadget_index(layer_index)], is_masked, adjusted_rect, outer_thumbnail_rect, inner_thumbnail_rect, outer_mask_thumbnail_rect, inner_mask_thumbnail_rect, text_rect);
    update(outer_thumbnail_rect);
    if (is_masked)
        update(outer_mask_thumbnail_rect);
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
    m_selected_gadget_index = to_gadget_index(0);
    set_selected_layer(&m_image->layer(0));
}

void LayerListWidget::select_top_layer()
{
    if (!m_image || !m_image->layer_count())
        return;
    m_selected_gadget_index = 0;
    set_selected_layer(&m_image->layer(to_layer_index(0)));
}

void LayerListWidget::cycle_through_selection(int delta)
{
    if (!m_image || !m_image->layer_count())
        return;

    auto current_index = static_cast<int>(m_selected_gadget_index);
    current_index += delta;

    if (current_index < 0)
        current_index = m_gadgets.size() - 1;
    if (current_index > static_cast<int>(m_gadgets.size()) - 1)
        current_index = 0;

    m_selected_gadget_index = current_index;
    auto selected_layer_index = to_layer_index(m_selected_gadget_index);
    set_selected_layer(&m_image->layer(selected_layer_index));
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
        if (index == hole_index)
            y += vertical_step;
        gadget.rect = { 0, y, widget_inner_rect().width(), gadget_height };
        y += vertical_step;
        ++index;
    }

    auto total_gadget_height = static_cast<int>(m_gadgets.size()) * vertical_step + 6;
    set_content_size({ widget_inner_rect().width(), total_gadget_height });
    vertical_scrollbar().set_range(0, max(total_gadget_height - height(), 0));
    update();
}

void LayerListWidget::set_selected_layer(Layer* layer)
{
    if (!m_image)
        return;

    if (layer && layer->is_selected())
        return;

    for (size_t i = 0; i < m_image->layer_count(); ++i) {
        if (layer == &m_image->layer(i)) {
            m_image->layer(i).set_selected(true);
            m_selected_gadget_index = to_gadget_index(i);
            scroll_into_view(m_gadgets[m_selected_gadget_index].rect, false, true);
        } else {
            m_image->layer(i).set_selected(false);
        }
    }
    if (on_layer_select)
        on_layer_select(layer);

    update();
}

}
