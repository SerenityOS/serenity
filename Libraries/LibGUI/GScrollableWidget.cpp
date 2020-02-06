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

#include <LibGUI/GScrollBar.h>
#include <LibGUI/GScrollableWidget.h>

namespace GUI {

ScrollableWidget::ScrollableWidget(Widget* parent)
    : Frame(parent)
{
    m_vertical_scrollbar = ScrollBar::construct(Orientation::Vertical, this);
    m_vertical_scrollbar->set_step(4);
    m_vertical_scrollbar->on_change = [this](int) {
        did_scroll();
        update();
    };

    m_horizontal_scrollbar = ScrollBar::construct(Orientation::Horizontal, this);
    m_horizontal_scrollbar->set_step(4);
    m_horizontal_scrollbar->set_big_step(30);
    m_horizontal_scrollbar->on_change = [this](int) {
        did_scroll();
        update();
    };

    m_corner_widget = Widget::construct(this);
    m_corner_widget->set_fill_with_background_color(true);
}

ScrollableWidget::~ScrollableWidget()
{
}

void ScrollableWidget::mousewheel_event(MouseEvent& event)
{
    // FIXME: The wheel delta multiplier should probably come from... somewhere?
    vertical_scrollbar().set_value(vertical_scrollbar().value() + event.wheel_delta() * 20);
}

void ScrollableWidget::custom_layout()
{
    auto inner_rect = frame_inner_rect_for_size(size());
    int height_wanted_by_horizontal_scrollbar = m_horizontal_scrollbar->is_visible() ? m_horizontal_scrollbar->preferred_size().height() : 0;
    int width_wanted_by_vertical_scrollbar = m_vertical_scrollbar->is_visible() ? m_vertical_scrollbar->preferred_size().width() : 0;

    m_vertical_scrollbar->set_relative_rect(
        inner_rect.right() + 1 - m_vertical_scrollbar->preferred_size().width(),
        inner_rect.top(),
        m_vertical_scrollbar->preferred_size().width(),
        inner_rect.height() - height_wanted_by_horizontal_scrollbar);

    m_horizontal_scrollbar->set_relative_rect(
        inner_rect.left(),
        inner_rect.bottom() + 1 - m_horizontal_scrollbar->preferred_size().height(),
        inner_rect.width() - width_wanted_by_vertical_scrollbar,
        m_horizontal_scrollbar->preferred_size().height());

    m_corner_widget->set_visible(m_vertical_scrollbar->is_visible() && m_horizontal_scrollbar->is_visible());
    if (m_corner_widget->is_visible()) {
        Gfx::Rect corner_rect { m_horizontal_scrollbar->relative_rect().right() + 1, m_vertical_scrollbar->relative_rect().bottom() + 1, width_occupied_by_vertical_scrollbar(), height_occupied_by_horizontal_scrollbar() };
        m_corner_widget->set_relative_rect(corner_rect);
    }
}

void ScrollableWidget::resize_event(ResizeEvent& event)
{
    Frame::resize_event(event);
    update_scrollbar_ranges();
}

Size ScrollableWidget::available_size() const
{
    int available_width = frame_inner_rect().width() - m_size_occupied_by_fixed_elements.width() - width_occupied_by_vertical_scrollbar();
    int available_height = frame_inner_rect().height() - m_size_occupied_by_fixed_elements.height() - height_occupied_by_horizontal_scrollbar();
    return { available_width, available_height };
}

void ScrollableWidget::update_scrollbar_ranges()
{
    auto available_size = this->available_size();

    int excess_height = max(0, m_content_size.height() - available_size.height());
    m_vertical_scrollbar->set_range(0, excess_height);

    if (should_hide_unnecessary_scrollbars())
        m_vertical_scrollbar->set_visible(excess_height > 0);

    int excess_width = max(0, m_content_size.width() - available_size.width());
    m_horizontal_scrollbar->set_range(0, excess_width);

    if (should_hide_unnecessary_scrollbars())
        m_horizontal_scrollbar->set_visible(excess_width > 0);

    m_vertical_scrollbar->set_big_step(visible_content_rect().height() - m_vertical_scrollbar->step());
}

void ScrollableWidget::set_content_size(const Gfx::Size& size)
{
    if (m_content_size == size)
        return;
    m_content_size = size;
    update_scrollbar_ranges();
}

void ScrollableWidget::set_size_occupied_by_fixed_elements(const Gfx::Size& size)
{
    if (m_size_occupied_by_fixed_elements == size)
        return;
    m_size_occupied_by_fixed_elements = size;
    update_scrollbar_ranges();
}

int ScrollableWidget::height_occupied_by_horizontal_scrollbar() const
{
    return m_horizontal_scrollbar->is_visible() ? m_horizontal_scrollbar->height() : 0;
}

int ScrollableWidget::width_occupied_by_vertical_scrollbar() const
{
    return m_vertical_scrollbar->is_visible() ? m_vertical_scrollbar->width() : 0;
}

Gfx::Rect ScrollableWidget::visible_content_rect() const
{
    return {
        m_horizontal_scrollbar->value(),
        m_vertical_scrollbar->value(),
        min(m_content_size.width(), frame_inner_rect().width() - width_occupied_by_vertical_scrollbar() - m_size_occupied_by_fixed_elements.width()),
        min(m_content_size.height(), frame_inner_rect().height() - height_occupied_by_horizontal_scrollbar() - m_size_occupied_by_fixed_elements.height())
    };
}

void ScrollableWidget::scroll_into_view(const Gfx::Rect& rect, Orientation orientation)
{
    if (orientation == Orientation::Vertical)
        return scroll_into_view(rect, false, true);
    return scroll_into_view(rect, true, false);
}

void ScrollableWidget::scroll_into_view(const Gfx::Rect& rect, bool scroll_horizontally, bool scroll_vertically)
{
    auto visible_content_rect = this->visible_content_rect();
    if (visible_content_rect.contains(rect))
        return;

    if (scroll_vertically) {
        if (rect.top() < visible_content_rect.top())
            m_vertical_scrollbar->set_value(rect.top());
        else if (rect.bottom() > visible_content_rect.bottom())
            m_vertical_scrollbar->set_value(rect.bottom() - visible_content_rect.height());
    }
    if (scroll_horizontally) {
        if (rect.left() < visible_content_rect.left())
            m_horizontal_scrollbar->set_value(rect.left());
        else if (rect.right() > visible_content_rect.right())
            m_horizontal_scrollbar->set_value(rect.right() - visible_content_rect.width());
    }
}

void ScrollableWidget::set_scrollbars_enabled(bool scrollbars_enabled)
{
    if (m_scrollbars_enabled == scrollbars_enabled)
        return;
    m_scrollbars_enabled = scrollbars_enabled;
    m_vertical_scrollbar->set_visible(m_scrollbars_enabled);
    m_horizontal_scrollbar->set_visible(m_scrollbars_enabled);
    m_corner_widget->set_visible(m_scrollbars_enabled);
}

void ScrollableWidget::scroll_to_top()
{
    scroll_into_view({ 0, 0, 1, 1 }, Orientation::Vertical);
}

void ScrollableWidget::scroll_to_bottom()
{
    scroll_into_view({ 0, content_height(), 1, 1 }, Orientation::Vertical);
}

Gfx::Rect ScrollableWidget::widget_inner_rect() const
{
    auto rect = frame_inner_rect();
    rect.set_width(rect.width() - width_occupied_by_vertical_scrollbar());
    rect.set_height(rect.height() - height_occupied_by_horizontal_scrollbar());
    return rect;
}

Point ScrollableWidget::to_content_position(const Gfx::Point& widget_position) const
{
    auto content_position = widget_position;
    content_position.move_by(horizontal_scrollbar().value(), vertical_scrollbar().value());
    content_position.move_by(-frame_thickness(), -frame_thickness());
    return content_position;
}

Point ScrollableWidget::to_widget_position(const Gfx::Point& content_position) const
{
    auto widget_position = content_position;
    widget_position.move_by(-horizontal_scrollbar().value(), -vertical_scrollbar().value());
    widget_position.move_by(frame_thickness(), frame_thickness());
    return widget_position;
}

}
