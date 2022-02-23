/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(GUI, HorizontalSplitter)
REGISTER_WIDGET(GUI, VerticalSplitter)

namespace GUI {

Splitter::Splitter(Orientation orientation)
    : m_orientation(orientation)
{
    REGISTER_INT_PROPERTY("first_resizee_minimum_size", first_resizee_minimum_size, set_first_resizee_minimum_size);
    REGISTER_INT_PROPERTY("second_resizee_minimum_size", second_resizee_minimum_size, set_second_resizee_minimum_size);
    REGISTER_ENUM_PROPERTY("fixed_resizee", fixed_resizee, set_fixed_resizee, FixedResizee,
        { FixedResizee::First, "First" },
        { FixedResizee::Second, "Second" });

    set_background_role(ColorRole::Button);
    set_layout<BoxLayout>(orientation);
    set_fill_with_background_color(true);
    if (m_orientation == Gfx::Orientation::Horizontal)
        layout()->set_spacing(3);
    else
        layout()->set_spacing(4);
}

Splitter::~Splitter()
{
}

void Splitter::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    auto palette = this->palette();

    auto paint_knurl = [&](int x, int y) {
        painter.set_pixel(x, y, palette.threed_shadow1());
        painter.set_pixel(x + 1, y, palette.threed_shadow1());
        painter.set_pixel(x, y + 1, palette.threed_shadow1());
        painter.set_pixel(x + 1, y + 1, palette.threed_highlight());
    };

    constexpr size_t knurl_width = 2;
    constexpr size_t knurl_spacing = 1;
    constexpr size_t knurl_count = 10;
    constexpr size_t total_knurling_width = knurl_count * (knurl_width + knurl_spacing);

    if (m_hovered_index.has_value())
        painter.fill_rect(m_grabbables[m_hovered_index.value()].paint_rect, palette.hover_highlight());

    for (auto& grabbable : m_grabbables) {
        for (size_t i = 0; i < knurl_count; ++i) {
            auto& rect = grabbable.paint_rect;
            int primary = rect.center().primary_offset_for_orientation(m_orientation) - 1;
            int secondary = rect.center().secondary_offset_for_orientation(m_orientation) - (total_knurling_width / 2) + (i * (knurl_width + knurl_spacing));
            if (m_orientation == Gfx::Orientation::Vertical)
                paint_knurl(secondary, primary);
            else
                paint_knurl(primary, secondary);
        }
    }
}

void Splitter::resize_event(ResizeEvent& event)
{
    Widget::resize_event(event);
    set_hovered_grabbable(nullptr);
}

void Splitter::set_hovered_grabbable(Grabbable* grabbable)
{
    if (m_hovered_index.has_value()) {
        if (grabbable && grabbable->index == m_hovered_index.value())
            return;
        update(m_grabbables[m_hovered_index.value()].paint_rect);
    }

    if (grabbable) {
        m_hovered_index = grabbable->index;
        update(grabbable->paint_rect);
    } else {
        m_hovered_index = {};
    }
}

void Splitter::override_cursor(bool do_override)
{
    if (do_override) {
        if (!m_overriding_cursor) {
            set_override_cursor(m_orientation == Orientation::Horizontal ? Gfx::StandardCursor::ResizeColumn : Gfx::StandardCursor::ResizeRow);
            m_overriding_cursor = true;
        }
    } else {
        if (m_overriding_cursor) {
            set_override_cursor(Gfx::StandardCursor::None);
            m_overriding_cursor = false;
        }
    }
}

void Splitter::leave_event(Core::Event&)
{
    if (!m_resizing)
        override_cursor(false);
    set_hovered_grabbable(nullptr);
}

Splitter::Grabbable* Splitter::grabbable_at(Gfx::IntPoint const& position)
{
    for (auto& grabbable : m_grabbables) {
        if (grabbable.grabbable_rect.contains(position))
            return &grabbable;
    }
    return nullptr;
}

void Splitter::mousedown_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Primary)
        return;

    auto* grabbable = grabbable_at(event.position());
    if (!grabbable)
        return;

    m_resizing = true;

    m_first_resizee = *grabbable->first_widget;
    m_second_resizee = *grabbable->second_widget;
    m_first_resizee_start_size = m_first_resizee->size();
    m_second_resizee_start_size = m_second_resizee->size();
    m_resize_origin = event.position();
}

Gfx::IntRect Splitter::rect_between_widgets(GUI::Widget const& first_widget, GUI::Widget const& second_widget, bool honor_grabbable_margins) const
{
    auto first_widget_rect = honor_grabbable_margins ? first_widget.relative_non_grabbable_rect() : first_widget.relative_rect();
    auto second_widget_rect = honor_grabbable_margins ? second_widget.relative_non_grabbable_rect() : second_widget.relative_rect();

    auto first_edge = first_widget_rect.last_edge_for_orientation(m_orientation);
    auto second_edge = second_widget_rect.first_edge_for_orientation(m_orientation);
    Gfx::IntRect rect;
    rect.set_primary_offset_for_orientation(m_orientation, first_edge + 1);
    rect.set_primary_size_for_orientation(m_orientation, second_edge - first_edge - 1);
    rect.set_secondary_offset_for_orientation(m_orientation, 0);
    rect.set_secondary_size_for_orientation(m_orientation, relative_rect().secondary_size_for_orientation(m_orientation));
    return rect;
}

void Splitter::recompute_grabbables()
{
    auto old_grabbables_count = m_grabbables.size();
    m_grabbables.clear();
    auto old_hovered_index = m_hovered_index;
    m_hovered_index = {};

    auto child_widgets = this->child_widgets();
    child_widgets.remove_all_matching([&](auto& widget) { return !widget.is_visible(); });
    m_last_child_count = child_widgets.size();

    if (child_widgets.size() < 2)
        return;

    size_t start_index = 0;
    size_t end_index = 1;

    while (end_index < child_widgets.size()) {
        auto const& first_widget = child_widgets[start_index];
        auto const& second_widget = child_widgets[end_index];
        m_grabbables.append(Grabbable {
            .index = m_grabbables.size(),
            .grabbable_rect = rect_between_widgets(first_widget, second_widget, true),
            .paint_rect = rect_between_widgets(first_widget, second_widget, false),
            .first_widget = first_widget,
            .second_widget = second_widget,
        });
        ++start_index;
        ++end_index;
    }

    if (old_hovered_index.has_value() && old_grabbables_count == m_grabbables.size())
        set_hovered_grabbable(&m_grabbables[old_hovered_index.value()]);
}

void Splitter::mousemove_event(MouseEvent& event)
{
    auto* grabbable = grabbable_at(event.position());
    set_hovered_grabbable(grabbable);
    if (!m_resizing) {
        override_cursor(grabbable != nullptr);
        return;
    }
    auto delta = event.position() - m_resize_origin;
    if (!m_first_resizee || !m_second_resizee) {
        // One or both of the resizees were deleted during an ongoing resize, screw this.
        m_resizing = false;
        return;
    }
    auto new_first_resizee_size = m_first_resizee_start_size;
    auto new_second_resizee_size = m_second_resizee_start_size;

    new_first_resizee_size.set_primary_size_for_orientation(m_orientation, new_first_resizee_size.primary_size_for_orientation(m_orientation) + delta.primary_offset_for_orientation(m_orientation));
    new_second_resizee_size.set_primary_size_for_orientation(m_orientation, new_second_resizee_size.primary_size_for_orientation(m_orientation) - delta.primary_offset_for_orientation(m_orientation));

    if (new_first_resizee_size.primary_size_for_orientation(m_orientation) < m_first_resizee_minimum_size) {
        int correction = m_first_resizee_minimum_size - new_first_resizee_size.primary_size_for_orientation(m_orientation);
        new_first_resizee_size.set_primary_size_for_orientation(m_orientation, new_first_resizee_size.primary_size_for_orientation(m_orientation) + correction);
        new_second_resizee_size.set_primary_size_for_orientation(m_orientation, new_second_resizee_size.primary_size_for_orientation(m_orientation) - correction);
    }
    if (new_second_resizee_size.primary_size_for_orientation(m_orientation) < m_second_resizee_minimum_size) {
        int correction = m_second_resizee_minimum_size - new_second_resizee_size.primary_size_for_orientation(m_orientation);
        new_second_resizee_size.set_primary_size_for_orientation(m_orientation, new_second_resizee_size.primary_size_for_orientation(m_orientation) + correction);
        new_first_resizee_size.set_primary_size_for_orientation(m_orientation, new_first_resizee_size.primary_size_for_orientation(m_orientation) - correction);
    }

    if (m_orientation == Orientation::Horizontal) {
        m_first_resizee->set_fixed_width(fixed_resizee() == FixedResizee::First ? new_first_resizee_size.width() : -1);
        m_second_resizee->set_fixed_width(fixed_resizee() == FixedResizee::Second ? new_second_resizee_size.width() : -1);
    } else {
        m_first_resizee->set_fixed_height(fixed_resizee() == FixedResizee::First ? new_first_resizee_size.height() : -1);
        m_second_resizee->set_fixed_height(fixed_resizee() == FixedResizee::Second ? new_second_resizee_size.height() : -1);
    }

    invalidate_layout();
}

void Splitter::did_layout()
{
    recompute_grabbables();
}

void Splitter::custom_layout()
{
    auto child_widgets = this->child_widgets();
    child_widgets.remove_all_matching([&](auto& widget) { return !widget.is_visible(); });

    if (!child_widgets.size())
        return;

    if (m_last_child_count > child_widgets.size()) {
        bool has_child_to_fill_space = false;
        for (auto& child : child_widgets) {
            if (child.max_size() == Gfx::IntSize { -1, -1 }) {
                has_child_to_fill_space = true;
                break;
            }
        }
        if (!has_child_to_fill_space)
            child_widgets.last().set_fixed_size({ -1, -1 });
    }
}

void Splitter::mouseup_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Primary)
        return;
    m_resizing = false;
    m_first_resizee = nullptr;
    m_second_resizee = nullptr;
    if (!rect().contains(event.position()))
        set_override_cursor(Gfx::StandardCursor::None);
}

}
